package com.trevizan.mithrilledger.domain.model;

import com.trevizan.mithrilledger.exception.domain.InsufficientBalanceException;
import com.trevizan.mithrilledger.domain.model.converter.CurrencyAttributeConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String ownerId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false)
    @Convert(converter = CurrencyAttributeConverter.class)
    private Currency currency;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Wallet() {

    }

    private Wallet(UUID id, String ownerId, Currency currency) {
        this.id = Objects.requireNonNull(id);
        this.ownerId = Objects.requireNonNull(ownerId);
        this.currency = Objects.requireNonNull(currency);
        this.balance = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
        this.createdAt = Instant.now();
    }

    public static Wallet create(String ownerId, Currency currency) {
        return new Wallet(UUID.randomUUID(), ownerId, currency);
    }

    public void debit(BigDecimal amount) {
        amount = validateAmount(amount);

        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(this.id);
        }

        this.balance = this.balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        amount = validateAmount(amount);
        this.balance = this.balance.add(amount);
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null.");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0.");
        }

        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    public Currency getCurrency() {
        return this.currency;
    }

    public BigDecimal getBalance() {
        return this.balance;
    }

    public UUID getId() {
        return this.id;
    }

    public String getOwnerId() {
        return this.ownerId;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

}
