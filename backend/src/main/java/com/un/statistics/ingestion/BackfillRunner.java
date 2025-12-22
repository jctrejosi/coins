package com.un.statistics.ingestion;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Profile("backfill") // solo corre con perfil 'backfill'
public class BackfillRunner implements CommandLineRunner {

    private final ExchangeRateIngestionService ingestionService;

    public BackfillRunner(ExchangeRateIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Override
    public void run(String... args) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(1); // últimos 1 año

        System.out.println("Iniciando backfill de " + startDate + " a " + endDate);

        try {
            ingestionService.backfill(startDate, endDate);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Backfill completado.");
    }
}
