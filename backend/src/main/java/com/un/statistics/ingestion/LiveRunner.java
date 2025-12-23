package com.un.statistics.ingestion;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.un.statistics.ingestion.services.ExchangeRateLiveService;

import java.util.List;

@Component
@Profile("testlive") // se ejecuta solo con el perfil "testlive"
public class LiveRunner implements CommandLineRunner {

    private final ExchangeRateLiveService liveService;

    public LiveRunner(ExchangeRateLiveService liveService) {
        this.liveService = liveService;
    }

    @Override
    public void run(String... args) {

        List<String> symbols = List.of("COP", "EUR", "JPY"); // ejemplo con varias monedas

        System.out.println("Consultando tasas live...");
        liveService.fetchLiveRates(symbols);
        System.out.println("Consulta completada.");
    }
}
