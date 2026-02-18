package com.trevizan.mithrilledger.infrastructure.exchange;

import java.math.BigDecimal;

public record ExchangeResponse(
    BigDecimal rate
) { }
