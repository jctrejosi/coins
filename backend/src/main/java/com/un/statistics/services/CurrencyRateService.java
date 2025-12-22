package com.un.statistics.services;

import com.un.statistics.model.CurrencyRate;
import com.un.statistics.repository.CurrencyRateRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CurrencyRateService {

    private final CurrencyRateRepository currencyRateRepository;

    public CurrencyRateService(CurrencyRateRepository currencyRateRepository) {
        this.currencyRateRepository = currencyRateRepository;
    }

    public Map<Long, List<CurrencyRate>> getHistoryByCoinIds(List<Long> coinIds) {
        return currencyRateRepository
            .findByCoin_IdInOrderByRateDateAsc(coinIds)
            .stream()
            .collect(Collectors.groupingBy(rate -> rate.getCoin().getId()));
    }
}
