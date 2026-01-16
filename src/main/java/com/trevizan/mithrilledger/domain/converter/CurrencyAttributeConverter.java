package com.trevizan.mithrilledger.domain.converter;

import java.util.Currency;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CurrencyAttributeConverter implements AttributeConverter<Currency,String> {

    @Override
    public String convertToDatabaseColumn(Currency currency) {
        return currency.getCurrencyCode();
    }

    @Override
    public Currency convertToEntityAttribute(String code) {
        return Currency.getInstance(code);
    }

}
