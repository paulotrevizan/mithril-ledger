package com.trevizan.mithrilledger.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ledger_entries")
public class Ledger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID walletId;

    private UUID transactionId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LedgerType type;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private Currency currency;

    @Column(nullable = false)
    private Instant createdAt;

    protected Ledger() {

    }

    public Ledger(
        UUID walletId,
        UUID transactionId,
        LedgerType type,
        BigDecimal amount,
        Currency currency
    ) {
        this.walletId = walletId;
        this.transactionId = transactionId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = Instant.now();
    }

}
