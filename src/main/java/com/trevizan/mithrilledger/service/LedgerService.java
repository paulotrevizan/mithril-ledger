package com.trevizan.mithrilledger.service;

import com.trevizan.mithrilledger.domain.model.Ledger;
import com.trevizan.mithrilledger.domain.model.LedgerType;
import com.trevizan.mithrilledger.repository.LedgerRepository;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class LedgerService {

    private final LedgerRepository ledgerRepository;

    public LedgerService(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    public void persistWalletEntry(UUID walletId, LedgerType type, BigDecimal amount, Currency currency) {
        Ledger entry = new Ledger(
            walletId,
            null,
            type,
            amount,
            currency
        );

        ledgerRepository.save(entry);
    }

    public void persistTransferEntries(
        UUID fromWalletId,
        UUID toWalletId,
        UUID transactionId,
        BigDecimal amountDebited,
        Currency fromCurrency,
        BigDecimal amountCredited,
        Currency toCurrency
    ) {
        Ledger debit = new Ledger(
            fromWalletId,
            transactionId,
            LedgerType.TRANSFER_DEBIT,
            amountDebited,
            fromCurrency
        );

        Ledger credit = new Ledger(
            toWalletId,
            transactionId,
            LedgerType.TRANSFER_CREDIT,
            amountCredited,
            toCurrency
        );

        ledgerRepository.save(debit);
        ledgerRepository.save(credit);
    }

}
