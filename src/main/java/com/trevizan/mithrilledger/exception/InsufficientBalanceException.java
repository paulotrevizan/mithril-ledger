package com.trevizan.mithrilledger.exception;

import java.util.UUID;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(UUID walletId) {
        super("Wallet " + walletId + " has insufficient balance.");
    }

}
