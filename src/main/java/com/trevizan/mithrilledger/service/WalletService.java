package com.trevizan.mithrilledger.service;

import com.trevizan.mithrilledger.domain.Transaction;
import com.trevizan.mithrilledger.domain.Wallet;
import com.trevizan.mithrilledger.exception.domain.WalletNotFoundException;
import com.trevizan.mithrilledger.repository.TransactionRepository;
import com.trevizan.mithrilledger.repository.WalletRepository;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public WalletService(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Wallet createWallet(String ownerId, Currency currency) {
        Wallet wallet = Wallet.create(ownerId, currency);
        return walletRepository.save(wallet);
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
        fromWallet.debit(amount);
        toWallet.credit(amount);

        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);

        Transaction transaction = new Transaction(fromWallet, toWallet, amount);
        transactionRepository.save(transaction);

        return transaction;
    }

}
