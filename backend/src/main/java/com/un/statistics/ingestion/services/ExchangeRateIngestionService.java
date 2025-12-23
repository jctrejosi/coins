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
import java.time.LocalDate;
import java.util.List;

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
     * Carga histórica de tasas de cambio.
     * Se asume base USD y frecuencia diaria.
     */
    @Transactional
    public void backfill(LocalDate startDate, LocalDate endDate) {

        List<Coin> coins = coinRepository.findAll();
        if (coins.isEmpty()) {
            throw new IllegalStateException("No hay monedas registradas en la tabla coin");
        }

        List<String> symbols = coins.stream().map(Coin::getCode).toList();
        System.out.println("Símbolos a procesar: " + symbols);

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {

            final LocalDate rateDate = date; // <-- esto es lo que captura la fecha para la lambda
            System.out.println("Procesando fecha: " + rateDate);

            ExchangeRateResponse response = exchangeRateClient.getRates(rateDate, "USD", symbols);

            if (response == null || response.getRates() == null || response.getRates().isEmpty()) {
                System.out.println("No hay datos para la fecha: " + rateDate);
                continue;
            }

            response.getRates().forEach((code, rateValue) -> {
                Coin coin = coinRepository.findByCode(code).orElse(null);
                if (coin == null) {
                    System.out.println("Moneda no encontrada: " + code);
                    return;
                }

                boolean exists = currencyRateRepository.existsByCoinAndRateDateAndOrigin(coin, rateDate, "USD");
                if (exists) {
                    System.out.println("Registro ya existe: " + code + " - " + rateDate);
                    return;
                }

                CurrencyRate rate = new CurrencyRate();
                rate.setCoin(coin);
                rate.setRateDate(rateDate);
                rate.setRateToUsd(BigDecimal.valueOf(rateValue));
                rate.setOrigin("exchangerate.host");

                currencyRateRepository.save(rate);
                System.out.println("Guardado: " + code + " -> " + rateValue + " en " + rateDate);
            });
        }

    }

}
