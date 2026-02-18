package com.trevizan.mithrilledger.domain.exchange;

import java.math.BigDecimal;

public interface ExchangeClient {

    BigDecimal getRate(String from, String to);

}
