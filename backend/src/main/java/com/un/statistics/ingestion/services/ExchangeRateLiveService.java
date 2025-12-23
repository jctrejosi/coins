package com.un.statistics.ingestion.services;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class ExchangeRateLiveService {

    private final WebClient webClient;

    public ExchangeRateLiveService(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("https://api.exchangerate.host")
                .build();
    }

    public void fetchLiveRates(List<String> symbols) {

        LiveResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/live")
                    .queryParam("base", "USD")
                    .queryParam("symbols", String.join(",", symbols))
                    .queryParam("access_key", "3c45fda8c10d9d1c287d00f2887e7bb5")
                    .build()
                )
                .retrieve()
                .bodyToMono(LiveResponse.class)
                .block();

        System.out.println(response);
        if (response != null && response.getQuotes() != null) {
            response.getQuotes().forEach((k, v) ->
                    System.out.println("Moneda: " + k + " | Tasa: " + v)
            );
        }
    }

    // Clase interna para mapear la respuesta de la API
    public static class LiveResponse {
        private boolean success;
        private String base;
        private String date;
        private Map<String, Double> quotes;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getBase() { return base; }
        public void setBase(String base) { this.base = base; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public Map<String, Double> getQuotes() { return quotes; }
        public void setQuotes(Map<String, Double> quotes) { this.quotes = quotes; }

        @Override
        public String toString() {
            return "LiveResponse{" +
                    "success=" + success +
                    ", base='" + base + '\'' +
                    ", date='" + date + '\'' +
                    ", quotes=" + quotes +
                    '}';
        }
    }
}
