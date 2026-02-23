package com.trevizan.mithrilledger.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_wallet_id")
    private Wallet fromWallet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_wallet_id")
    private Wallet toWallet;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amountDebited;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amountCredited;

    @Column(nullable = false)
    private String fromCurrency;

    @Column(nullable = false)
    private String toCurrency;

    @Column(precision = 19, scale = 6)
    private BigDecimal exchangeRate;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Transaction() {

    }

    public Transaction(
        Wallet fromWallet,
        Wallet toWallet,
        BigDecimal amountDebited,
        BigDecimal amountCredited,
        BigDecimal exchangeRate
    ) {
        if (amountDebited == null || amountDebited.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount debited must be positive.");
        }

        if (amountCredited == null || amountCredited.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount credited must be positive.");
        }

        if (fromWallet.equals(toWallet)) {
            throw new IllegalArgumentException("Origin and Destination Wallet must be different.");
        }

        this.fromWallet = fromWallet;
        this.toWallet = toWallet;
        this.amountDebited = amountDebited;
        this.amountCredited = amountCredited;
        this.fromCurrency = fromWallet.getCurrency().getCurrencyCode();
        this.toCurrency = toWallet.getCurrency().getCurrencyCode();
        this.exchangeRate = exchangeRate;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Wallet getFromWallet() {
        return fromWallet;
    }

    public Wallet getToWallet() {
        return toWallet;
    }

    public BigDecimal getAmountDebited() {
        return amountDebited;
    }

    public BigDecimal getAmountCredited() {
        return amountCredited;
    }

    public String getFromCurrency() {
        return fromCurrency;
    }

    public String getToCurrency() {
        return toCurrency;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

}
