package com.trevizan.mithrilledger.exception.infrastructure;

public class ExchangeServiceUnavailableException extends RuntimeException {

    private final String fromCurrency;
    private final String toCurrency;

    public ExchangeServiceUnavailableException(
        String fromCurrency,
        String toCurrency,
        String message,
        Throwable cause
    ) {
        super(message, cause);
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
    }

    public String getFromCurrency() {
        return fromCurrency;
    }

    public String getToCurrency() {
        return toCurrency;
    }

}
