package com.un.statistics.ingestion.client;

import com.un.statistics.config.ExchangeRateProperties;
import com.un.statistics.ingestion.response.ExchangeRateResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import java.time.LocalDate;
import java.util.List;

@Component
public class ExchangeRateClient {

    private final WebClient webClient;

    public ExchangeRateClient(WebClient.Builder builder, ExchangeRateProperties properties) {
        this.webClient = builder
                .baseUrl(properties.getBaseUrl()) // por ejemplo https://api.exchangerate.host
                .build();
    }

    /**
     * historical: un día
     */
    public ExchangeRateResponse getRates(LocalDate date, String base, List<String> symbols) {
        return webClient.get()
                .uri(uriBuilder -> buildHistoricalUri(uriBuilder, date, base, symbols))
                .retrieve()
                .bodyToMono(ExchangeRateResponse.class)
                .block();
    }

    private java.net.URI buildHistoricalUri(UriBuilder uriBuilder, LocalDate date, String base, List<String> symbols) {
        UriBuilder b = uriBuilder.path("historical")
                .queryParam("date", date.toString())
                .queryParam("base", base);
        if (symbols != null && !symbols.isEmpty()) b.queryParam("symbols", String.join(",", symbols));
        return b.build();
    }

    /**
     * timeframe: rango de fechas. pasar los symbols reduce el payload.
     */
    public ExchangeRateResponse getTimeframeRates(LocalDate startDate, LocalDate endDate, String base, List<String> symbols) {
        return webClient.get()
                .uri(uriBuilder -> {
                    UriBuilder b = uriBuilder.path("timeframe")
                            .queryParam("start_date", startDate.toString())
                            .queryParam("end_date", endDate.toString())
                            .queryParam("base", base)
                            .queryParam("access_key", "3c45fda8c10d9d1c287d00f2887e7bb5");
                    if (symbols != null && !symbols.isEmpty()) b.queryParam("symbols", String.join(",", symbols));
                    return b.build();
                })
                .retrieve()
                .bodyToMono(ExchangeRateResponse.class)
                .block();
    }

    /**
     * convert (opcional)
     */
    public ExchangeRateResponse convert(String from, String to, double amount) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("convert")
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
