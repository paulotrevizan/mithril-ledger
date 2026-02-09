package com.trevizan.mithrilledger.controller.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
    UUID fromWalletId,
    UUID toWalletId,
    BigDecimal amount
) { }
