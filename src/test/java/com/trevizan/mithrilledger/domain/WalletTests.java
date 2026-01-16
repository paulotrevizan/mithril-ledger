package com.trevizan.mithrilledger.domain;

import com.trevizan.mithrilledger.exception.InsufficientBalanceException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WalletTests {

    @Test
    void shouldInitializeWalletWithZeroBalance() {
        Wallet wallet = Wallet.create("owner-z", Currency.getInstance("BRL"));
        assertThat(wallet.getBalance())
            .isEqualByComparingTo(BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY));
        assertThat(wallet.getCurrency().getCurrencyCode()).isEqualTo("BRL");
    }

    @Test
    void shouldIncreaseWalletBalance() {
        Wallet wallet = Wallet.create("owner-n", Currency.getInstance("USD"));
        wallet.credit(BigDecimal.valueOf(100));
        assertThat(wallet.getBalance())
            .isEqualByComparingTo(BigDecimal.valueOf(100).setScale(2, RoundingMode.UNNECESSARY));
    }

    @Test
    void shouldDecreaseWalletBalance() {
        Wallet wallet = Wallet.create("owner-e", Currency.getInstance("GBP"));
        wallet.credit(BigDecimal.valueOf(100));
        wallet.debit(BigDecimal.valueOf(40));
        assertThat(wallet.getBalance())
            .isEqualByComparingTo(BigDecimal.valueOf(60).setScale(2, RoundingMode.UNNECESSARY));
    }

    @Test
    void shouldThrowExceptionWhenDebitGreaterThanBalance() {
        Wallet wallet = Wallet.create("owner-g", Currency.getInstance("EUR"));
        wallet.credit(BigDecimal.valueOf(50));

        assertThatThrownBy(() -> wallet.debit(BigDecimal.valueOf(100)))
            .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    void shouldThrowExceptionWhenCreditInvalidAmount() {
        Wallet wallet = Wallet.create("owner-w", Currency.getInstance("USD"));

        assertThatThrownBy(() -> wallet.credit(BigDecimal.ZERO))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> wallet.credit(BigDecimal.valueOf(-10)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowExceptionWhenDebitInvalidAmount() {
        Wallet wallet = Wallet.create("owner-s", Currency.getInstance("USD"));

        assertThatThrownBy(() -> wallet.debit(BigDecimal.ZERO))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> wallet.debit(BigDecimal.valueOf(-10)))
            .isInstanceOf(IllegalArgumentException.class);
    }

}
