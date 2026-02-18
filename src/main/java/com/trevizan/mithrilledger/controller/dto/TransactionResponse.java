package com.trevizan.mithrilledger.controller.dto;

import com.trevizan.mithrilledger.domain.model.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
    UUID id,
    UUID fromWalletId,
    UUID toWalletId,
    BigDecimal amount,
    Instant createdAt
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
            transaction.getId(),
            transaction.getFromWallet().getId(),
            transaction.getToWallet().getId(),
            transaction.getAmount(),
            transaction.getCreatedAt()
        );
    }
}
