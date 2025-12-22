package com.un.statistics.repository;

import com.un.statistics.model.CurrencyRate;
import com.un.statistics.model.Coin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CurrencyRateRepository extends JpaRepository<CurrencyRate, Long> {

    List<CurrencyRate> findByCoin_IdInOrderByRateDateAsc(List<Long> coinIds);
    boolean existsByCoinAndRateDateAndOrigin(
            Coin coin,
            LocalDate rateDate,
            String origin
    );
}

