package com.trevizan.mithrilledger.service;

import com.trevizan.mithrilledger.domain.exchange.ExchangeClient;
import com.trevizan.mithrilledger.domain.model.Transaction;
import com.trevizan.mithrilledger.domain.model.Wallet;
import com.trevizan.mithrilledger.repository.TransactionRepository;
import com.trevizan.mithrilledger.repository.WalletRepository;

import java.math.BigDecimal;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WalletTransferExecutor {

    private static final Logger log = LoggerFactory.getLogger(WalletTransferExecutor.class);

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerService ledgerService;
    private final ExchangeClient exchangeClient;

    public WalletTransferExecutor(
        WalletRepository walletRepository,
        TransactionRepository transactionRepository,
        LedgerService ledgerService,
        ExchangeClient exchangeClient
    ) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerService = ledgerService;
        this.exchangeClient = exchangeClient;
    }

    @Transactional
    public Transaction execute(Wallet fromWallet, Wallet toWallet, BigDecimal amount, String idempotencyKey) {
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        BigDecimal exchangeRate = getExchangeRate(fromWallet, toWallet);
        BigDecimal amountToCredit = amount.multiply(exchangeRate);

        Transaction transaction = new Transaction(
            fromWallet,
            toWallet,
            amount,
            amountToCredit,
            exchangeRate,
            idempotencyKey
        );

        transactionRepository.saveAndFlush(transaction);

        ledgerService.persistTransferEntries(
            fromWallet.getId(),
            toWallet.getId(),
            transaction.getId(),
            amount,
            fromWallet.getCurrency(),
            amountToCredit,
            toWallet.getCurrency()
        );

        fromWallet.debit(amount);
        toWallet.credit(amountToCredit);

        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);

        log.info(
            "Transfer executed: transactionId={}, fromWalletId={}, toWalletId={}, amountDebited={}, amountCredited={}, fromCurrency={}, toCurrency={}",
            transaction.getId(),
            fromWallet.getId(),
            toWallet.getId(),
            amount,
            amountToCredit,
            fromWallet.getCurrency(),
            toWallet.getCurrency()
        );

        return transaction;
    }

    private BigDecimal getExchangeRate(Wallet fromWallet, Wallet toWallet) {
        if (fromWallet.getCurrency().equals(toWallet.getCurrency())) {
            return BigDecimal.ONE;
        }

        return exchangeClient.getRate(
            fromWallet.getCurrency().getCurrencyCode(),
            toWallet.getCurrency().getCurrencyCode()
        );
    }

}
