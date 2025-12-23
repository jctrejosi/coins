package com.un.statistics.ingestion.client;

import com.un.statistics.config.ExchangeRateProperties;
import com.un.statistics.ingestion.response.ExchangeRateResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;

@Component
public class ExchangeRateClient {

    private final WebClient webClient;

    public ExchangeRateClient(WebClient.Builder builder, ExchangeRateProperties properties) {
        this.webClient = builder
                .baseUrl(properties.getBaseUrl()) // configurable, ej: https://api.exchangerate.host
                .build();
    }

    /**
     * Obtiene tasas para una fecha específica (historical).
     * @param date fecha a consultar
     * @param base moneda base
     * @param symbols lista de códigos de monedas
     */
    public ExchangeRateResponse getRates(LocalDate date, String base, java.util.List<String> symbols) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("historical")
                        .queryParam("date", date.toString())
                        .queryParam("base", base)
                        .queryParam("symbols", String.join(",", symbols))
                        .build()
                )
                .retrieve()
                .bodyToMono(ExchangeRateResponse.class)
                .block();
    }

    /**
     * Obtiene tasas para un rango de fechas (timeframe).
     * Devuelve un mapa con fecha -> {USDXXX -> valor}.
     * @param startDate fecha de inicio
     * @param endDate fecha final
     * @param base moneda base
     */
    public ExchangeRateResponse getTimeframeRates(LocalDate startDate, LocalDate endDate, String base) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("timeframe")
                        .queryParam("start_date", startDate.toString())
                        .queryParam("end_date", endDate.toString())
                        .queryParam("base", base)
                        .build()
                )
                .retrieve()
                .bodyToMono(ExchangeRateResponse.class)
                .block();
    }

    /**
     * Convierte un monto de una moneda a otra (opcional, útil para otras operaciones).
     */
    public ExchangeRateResponse convert(String from, String to, double amount) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("convert")
                        .queryParam("from", from)
                        .queryParam("to", to)
                        .queryParam("amount", amount)
                        .build()
                )
                .retrieve()
                .bodyToMono(ExchangeRateResponse.class)
                .block();
    }
}
