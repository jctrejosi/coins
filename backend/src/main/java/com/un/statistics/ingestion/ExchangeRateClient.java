package com.un.statistics.ingestion;

import com.un.statistics.config.ExchangeRateProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;

@Component
public class ExchangeRateClient {

    private final WebClient webClient;

    public ExchangeRateClient(WebClient.Builder builder, ExchangeRateProperties properties) {
        this.webClient = builder
			.baseUrl(properties.getBaseUrl()) // ahora configurable
			.build();
    }

    /**
     * Obtiene tasas para una fecha específica.
     * base: moneda base (ej: USD)
     * symbols: monedas objetivo (ej: EUR, COP, JPY)
     */
    public ExchangeRateResponse getRates(
		LocalDate date,
		String base,
		List<String> symbols
    ) {
        return webClient.get()
			.uri(uriBuilder -> uriBuilder
				.pathSegment(date.toString())  // automáticamente maneja la codificación
				.queryParam("base", base)
				.queryParam("symbols", String.join(",", symbols))
				.build()
			)
			.retrieve()
			.bodyToMono(ExchangeRateResponse.class)
			.block();
    }
}
