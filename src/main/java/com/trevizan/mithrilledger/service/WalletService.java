package com.trevizan.mithrilledger.service;

import com.trevizan.mithrilledger.domain.exchange.ExchangeClient;
import com.trevizan.mithrilledger.domain.model.Transaction;
import com.trevizan.mithrilledger.domain.model.Wallet;
import com.trevizan.mithrilledger.exception.domain.WalletNotFoundException;
import com.trevizan.mithrilledger.repository.TransactionRepository;
import com.trevizan.mithrilledger.repository.WalletRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final ExchangeClient exchangeClient;

    public WalletService(
        WalletRepository walletRepository,
        TransactionRepository transactionRepository,
        ExchangeClient exchangeClient
    ) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.exchangeClient = exchangeClient;
    }

    @Transactional
    public Wallet createWallet(String ownerId, Currency currency) {
        Wallet wallet = Wallet.create(ownerId, currency);
        walletRepository.save(wallet);

        log.info("Wallet created: walletId={}, ownerId={}, currency={}",
            wallet.getId(),
            wallet.getOwnerId(),
            wallet.getCurrency()
        );

        return wallet;
    }

    @Transactional(readOnly = true)
    public Wallet getWalletById(UUID id) {
        return walletRepository.findById(id).orElseThrow(
            () -> new WalletNotFoundException(id)
        );
    }

    @Transactional
    public Wallet credit(UUID walletId, BigDecimal amount) {
        Wallet wallet = getWalletById(walletId);
        wallet.credit(amount);
        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet debit(UUID walletId, BigDecimal amount) {
        Wallet wallet = getWalletById(walletId);
        wallet.debit(amount);
        return walletRepository.save(wallet);
    }

    @Transactional
    public Transaction transfer(Wallet fromWallet, Wallet toWallet, BigDecimal amount) {
        BigDecimal exchangeRate = getExchangeRate(fromWallet.getCurrency(), toWallet.getCurrency());
        BigDecimal amountToCredit = amount.multiply(exchangeRate);

        fromWallet.debit(amount);
        toWallet.credit(amountToCredit);

        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);

        Transaction transaction = new Transaction(
            fromWallet,
            toWallet,
            amount,
            amountToCredit,
            exchangeRate
        );
        transactionRepository.save(transaction);

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

    private BigDecimal getExchangeRate(Currency fromCurrency, Currency toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }

        return exchangeClient.getRate(fromCurrency.getCurrencyCode(), toCurrency.getCurrencyCode());
    }

}
