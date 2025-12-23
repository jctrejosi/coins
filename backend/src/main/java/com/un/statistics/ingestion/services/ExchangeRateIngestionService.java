package com.un.statistics.ingestion.services;

import com.un.statistics.ingestion.client.ExchangeRateClient;
import com.un.statistics.ingestion.response.ExchangeRateResponse;
import com.un.statistics.model.Coin;
import com.un.statistics.model.CurrencyRate;
import com.un.statistics.repository.CoinRepository;
import com.un.statistics.repository.CurrencyRateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExchangeRateIngestionService {

    private final ExchangeRateClient exchangeRateClient;
    private final CoinRepository coinRepository;
    private final CurrencyRateRepository currencyRateRepository;

    public ExchangeRateIngestionService(
            ExchangeRateClient exchangeRateClient,
            CoinRepository coinRepository,
            CurrencyRateRepository currencyRateRepository
    ) {
        this.exchangeRateClient = exchangeRateClient;
        this.coinRepository = coinRepository;
        this.currencyRateRepository = currencyRateRepository;
    }

    /**
     * Backfill usando timeframe en una sola llamada (por rango).
     * Calcula:
     *  - rate_to_usd = 1 / (USD->XXX)
     *  - rate_to_co  = rate_to_usd * (USD->COP)
     *
     * Solo procesa las monedas registradas en la BD.
     */
    @Transactional
    public void backfill(LocalDate startDate, LocalDate endDate) {

        List<Coin> coins = coinRepository.findAll();
        if (coins.isEmpty()) throw new IllegalStateException("No hay monedas registradas en la tabla coin");

        // map code -> Coin (asegúrate que coin.getCode() devuelve "COP", "EUR", etc)
        Map<String, Coin> coinMap = coins.stream()
                .collect(Collectors.toMap(c -> c.getCode().toUpperCase(), c -> c));

        // pasar la lista de symbols para reducir payload
        List<String> symbols = new ArrayList<>(coinMap.keySet());
        if (!symbols.contains("COP")) symbols.add("COP"); // necesario para calcular rate_to_co

        ExchangeRateResponse response = exchangeRateClient.getTimeframeRates(startDate, endDate, "USD", symbols);

        if (response == null || response.getQuotes() == null || response.getQuotes().isEmpty()) {
            return;
        }

        for (Map.Entry<String, Map<String, Double>> entry : response.getQuotes().entrySet()) {
            LocalDate rateDate = LocalDate.parse(entry.getKey());
            Map<String, Double> dailyRates = entry.getValue();

            Double usdToCop = dailyRates.get("USDCOP");

            List<CurrencyRate> toSave = new ArrayList<>();

            for (Map.Entry<String, Double> kv : dailyRates.entrySet()) {
                String apiCode = kv.getKey();
                if (apiCode == null || !apiCode.startsWith("USD")) continue;

                String code = apiCode.substring(3).toUpperCase();
                Coin coin = coinMap.get(code);
                if (coin == null) continue; // ignorar monedas que no estén en BD

                Double usdToX = kv.getValue();
                if (usdToX == null || usdToX == 0.0) continue;

                boolean exists = currencyRateRepository.existsByCoinAndRateDateAndOrigin(coin, rateDate, "exchangerate.host");
                if (exists) continue; // evitar duplicados

                BigDecimal rateToUsd = BigDecimal.ONE.divide(BigDecimal.valueOf(usdToX), 12, RoundingMode.HALF_UP);

                BigDecimal rateToCo;
                if (usdToCop != null && usdToCop > 0) {
                    rateToCo = rateToUsd.multiply(BigDecimal.valueOf(usdToCop)).setScale(6, RoundingMode.HALF_UP);
                } else if ("COP".equals(code)) {
                    rateToCo = BigDecimal.ONE;
                } else {
                    rateToCo = BigDecimal.ZERO; // si no se puede calcular
                }

                CurrencyRate cr = new CurrencyRate();
                cr.setCoin(coin);
                cr.setRateDate(rateDate);
                cr.setRateToUsd(rateToUsd.setScale(8, RoundingMode.HALF_UP));
                cr.setRateToCo(rateToCo);
                cr.setOrigin("exchangerate.host");

                toSave.add(cr);
            }

            if (!toSave.isEmpty()) {
                currencyRateRepository.saveAll(toSave);
            }
        }
    }
}
