package com.trevizan.mithrilledger.exception.infrastructure;

public class ExchangeInvalidResponseException extends RuntimeException {

    public ExchangeInvalidResponseException(String fromCurrency, String toCurrency) {
        super("Invalid response from exchange service for " + fromCurrency + " -> " + toCurrency + ".");
    }

}
