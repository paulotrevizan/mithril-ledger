package com.trevizan.mithrilledger.service;

import com.trevizan.mithrilledger.domain.Wallet;
import com.trevizan.mithrilledger.exception.domain.WalletNotFoundException;
import com.trevizan.mithrilledger.repository.WalletRepository;

import java.util.Currency;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
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

}
