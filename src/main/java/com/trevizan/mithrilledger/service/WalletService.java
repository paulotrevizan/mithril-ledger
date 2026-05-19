package com.trevizan.mithrilledger.service;

import com.trevizan.mithrilledger.domain.model.LedgerType;
import com.trevizan.mithrilledger.domain.model.Transaction;
import com.trevizan.mithrilledger.domain.model.Wallet;
import com.trevizan.mithrilledger.exception.domain.WalletNotFoundException;
import com.trevizan.mithrilledger.repository.TransactionRepository;
import com.trevizan.mithrilledger.repository.WalletRepository;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerService ledgerService;
    private final WalletTransferExecutor walletTransferExecutor;

    public WalletService(
        WalletRepository walletRepository,
        TransactionRepository transactionRepository,
        LedgerService ledgerService,
        WalletTransferExecutor walletTransferExecutor
    ) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerService = ledgerService;
        this.walletTransferExecutor = walletTransferExecutor;
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

        ledgerService.persistWalletEntry(
            wallet.getId(),
            LedgerType.CREDIT,
            amount,
            wallet.getCurrency()
        );

        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet debit(UUID walletId, BigDecimal amount) {
        Wallet wallet = getWalletById(walletId);
        wallet.debit(amount);

        ledgerService.persistWalletEntry(
            wallet.getId(),
            LedgerType.DEBIT,
            amount,
            wallet.getCurrency()
        );

        return walletRepository.save(wallet);
    }

    public Transaction transfer(Wallet fromWallet, Wallet toWallet, BigDecimal amount, String idempotencyKey) {
        try {
            return walletTransferExecutor.execute(
                fromWallet,
                toWallet,
                amount,
                idempotencyKey
            );
        } catch (DataIntegrityViolationException e) {
            return transactionRepository
                .findByIdempotencyKeyInNewTransaction(idempotencyKey)
                .orElseThrow(() -> e);
        }
    }

}
