package com.trevizan.mithrilledger.exception.domain;

import java.util.UUID;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(UUID walletId) {
        super("Wallet " + walletId + " has insufficient balance.");
    }

}
