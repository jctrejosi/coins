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
import java.util.Map;

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
     * Carga histórica de tasas de cambio usando la API de timeframe.
     * Se asume base USD y frecuencia diaria.
     */
    @Transactional
    public void backfill(LocalDate startDate, LocalDate endDate) {

        // Obtener todos los coins de la BD
        List<Coin> coins = coinRepository.findAll();
        if (coins.isEmpty()) {
            throw new IllegalStateException("No hay monedas registradas en la tabla coin");
        }

        // Map para buscar coins por código
        Map<String, Coin> coinMap = coins.stream()
                .collect(java.util.stream.Collectors.toMap(Coin::getCode, c -> c));

        System.out.println("Símbolos registrados en la BD: " + coinMap.keySet());

        // Llamar a la API con rango de fechas
        ExchangeRateResponse response = exchangeRateClient.getTimeframeRates(startDate, endDate, "USD");

        if (response == null || response.getRates() == null || response.getRates().isEmpty()) {
            System.out.println("No hay datos de la API para el rango solicitado");
            return;
        }

        // Iterar por cada fecha
        for (Map.Entry<String, Map<String, Double>> entry : response.getRates().entrySet()) {
            LocalDate rateDate = LocalDate.parse(entry.getKey());
            Map<String, Double> dailyRates = entry.getValue();

            System.out.println("Procesando fecha: " + rateDate);

            // Iterar por cada par USDXXX -> valor
            dailyRates.forEach((apiCode, rateValue) -> {

                // Extraer los últimos 3 dígitos para compararlo con los coins
                String code = apiCode.substring(3); // USDXXX -> XXX

                Coin coin = coinMap.get(code);
                if (coin == null) {
                    System.out.println("Moneda no encontrada en BD: " + code);
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

        System.out.println("Backfill completado para el rango: " + startDate + " a " + endDate);
    }
}
