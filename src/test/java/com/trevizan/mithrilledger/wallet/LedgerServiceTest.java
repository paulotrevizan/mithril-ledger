package com.trevizan.mithrilledger.wallet;

import com.trevizan.mithrilledger.domain.model.Ledger;
import com.trevizan.mithrilledger.domain.model.LedgerType;
import com.trevizan.mithrilledger.repository.LedgerRepository;
import com.trevizan.mithrilledger.service.LedgerService;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class LedgerServiceTest {

    private LedgerRepository ledgerRepository;
    private LedgerService ledgerService;

    @BeforeEach
    void setUp() {
        ledgerRepository = Mockito.mock(LedgerRepository.class);
        ledgerService = new LedgerService(ledgerRepository);
    }

    @Test
    void shouldPersistWalletEntryForCreditOrDebit() {
        UUID walletId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(99.90);
        Currency currency = Currency.getInstance("EUR");

        ledgerService.persistWalletEntry(walletId, LedgerType.CREDIT, amount, currency);

        ArgumentCaptor<Ledger> captor = ArgumentCaptor.forClass(Ledger.class);
        verify(ledgerRepository, times(1)).save(captor.capture());

        Ledger persisted = captor.getValue();
        assertThat(ReflectionTestUtils.getField(persisted, "walletId")).isEqualTo(walletId);
        assertThat(ReflectionTestUtils.getField(persisted, "transactionId")).isNull();
        assertThat(ReflectionTestUtils.getField(persisted, "type")).isEqualTo(LedgerType.CREDIT);
        assertThat(ReflectionTestUtils.getField(persisted, "amount")).isEqualTo(amount);
        assertThat(ReflectionTestUtils.getField(persisted, "currency")).isEqualTo(currency);
        assertThat(ReflectionTestUtils.getField(persisted, "createdAt")).isNotNull();
    }

    @Test
    void shouldPersistTransferDebitAndCreditEntries() {
        UUID fromWalletId = UUID.randomUUID();
        UUID toWalletId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        BigDecimal amountDebited = BigDecimal.valueOf(100);
        BigDecimal amountCredited = BigDecimal.valueOf(109.25);

        ledgerService.persistTransferEntries(
            fromWalletId,
            toWalletId,
            transactionId,
            amountDebited,
            Currency.getInstance("USD"),
            amountCredited,
            Currency.getInstance("EUR")
        );

        ArgumentCaptor<Ledger> captor = ArgumentCaptor.forClass(Ledger.class);
        verify(ledgerRepository, times(2)).save(captor.capture());

        List<Ledger> entries = captor.getAllValues();
        Ledger debitEntry = entries.get(0);
        Ledger creditEntry = entries.get(1);

        assertThat(ReflectionTestUtils.getField(debitEntry, "walletId")).isEqualTo(fromWalletId);
        assertThat(ReflectionTestUtils.getField(debitEntry, "transactionId")).isEqualTo(transactionId);
        assertThat(ReflectionTestUtils.getField(debitEntry, "type")).isEqualTo(LedgerType.TRANSFER_DEBIT);
        assertThat(ReflectionTestUtils.getField(debitEntry, "amount")).isEqualTo(amountDebited);

        assertThat(ReflectionTestUtils.getField(creditEntry, "walletId")).isEqualTo(toWalletId);
        assertThat(ReflectionTestUtils.getField(creditEntry, "transactionId")).isEqualTo(transactionId);
        assertThat(ReflectionTestUtils.getField(creditEntry, "type")).isEqualTo(LedgerType.TRANSFER_CREDIT);
        assertThat(ReflectionTestUtils.getField(creditEntry, "amount")).isEqualTo(amountCredited);
    }

}
