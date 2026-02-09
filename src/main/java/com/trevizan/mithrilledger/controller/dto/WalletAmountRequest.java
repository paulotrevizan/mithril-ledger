package com.trevizan.mithrilledger.controller.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletAmountRequest(
    UUID walletId,
    BigDecimal amount
) { }
