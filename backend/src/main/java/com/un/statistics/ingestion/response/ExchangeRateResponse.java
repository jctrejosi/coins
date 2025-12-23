package com.un.statistics.ingestion.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExchangeRateResponse {

    private boolean success;
    private String base;
    private String startDate;
    private String endDate;

    // timeframe usa "quotes": { "YYYY-MM-DD": { "USDXXX": value, ... }, ... }
    @JsonProperty("quotes")
    private Map<String, Map<String, Double>> quotes;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getBase() { return base; }
    public void setBase(String base) { this.base = base; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public Map<String, Map<String, Double>> getQuotes() { return quotes; }
    public void setQuotes(Map<String, Map<String, Double>> quotes) { this.quotes = quotes; }

    // helper para compatibilidad si en algún sitio llamabas getRates()
    public Map<String, Map<String, Double>> getRates() { return quotes; }

    /**
     * Devuelve todos los quotes planos para una fecha específica
     */
    public Map<String, Double> getQuotes(String date) {
        if (quotes == null || !quotes.containsKey(date)) return Map.of();
        return quotes.get(date);
    }

    @Override
    public String toString() {
        return "ExchangeRateResponse{success=" + success + ", base=" + base +
                ", start=" + startDate + ", end=" + endDate + ", quotes=" + (quotes != null ? quotes.size() : 0) + "}";
    }
}
