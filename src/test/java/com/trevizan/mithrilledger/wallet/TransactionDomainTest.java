package com.trevizan.mithrilledger.wallet;

import com.trevizan.mithrilledger.domain.model.Transaction;
import com.trevizan.mithrilledger.domain.model.Wallet;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionDomainTest {

    @Test
    void shouldCreateTransactionSuccessfully() {
        Wallet fromWallet = Wallet.create("1234", Currency.getInstance("USD"));
        Wallet toWallet = Wallet.create("1235", Currency.getInstance("USD"));
        fromWallet.credit(BigDecimal.valueOf(100));

        Transaction transaction = new Transaction(
            fromWallet,
            toWallet,
            BigDecimal.valueOf(50),
            BigDecimal.valueOf(50),
            BigDecimal.ONE
        );

        assertThat(transaction.getFromWallet()).isEqualTo(fromWallet);
        assertThat(transaction.getToWallet()).isEqualTo(toWallet);
        assertThat(transaction.getAmountCredited()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(transaction.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldThrowExceptionWhenAmountIsZeroOrNegative() {
        Wallet fromWallet = Wallet.create("1234", Currency.getInstance("USD"));
        Wallet toWallet = Wallet.create("1235", Currency.getInstance("USD"));

        assertThatThrownBy(() -> new Transaction(fromWallet, toWallet, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Amount debited must be positive");

        assertThatThrownBy(() -> new Transaction(fromWallet, toWallet, BigDecimal.valueOf(-10), BigDecimal.ZERO, BigDecimal.ZERO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Amount debited must be positive");

        assertThatThrownBy(() -> new Transaction(fromWallet, toWallet, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Amount credited must be positive");

        assertThatThrownBy(() -> new Transaction(fromWallet, toWallet, BigDecimal.ONE, BigDecimal.valueOf(-1.0), BigDecimal.ZERO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Amount credited must be positive");
    }

    @Test
    void shouldThrowExceptionWhenFromAndToWalletAreSame() {
        Wallet wallet = Wallet.create("1234", Currency.getInstance("USD"));

        assertThatThrownBy(() -> new Transaction(wallet, wallet, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Origin and Destination Wallet must be different");
    }

}
