package com.trevizan.mithrilledger.controller;

import com.trevizan.mithrilledger.controller.dto.TransactionResponse;
import com.trevizan.mithrilledger.controller.dto.TransferRequest;
import com.trevizan.mithrilledger.controller.dto.WalletAmountRequest;
import com.trevizan.mithrilledger.controller.dto.WalletRequest;
import com.trevizan.mithrilledger.controller.dto.WalletResponse;
import com.trevizan.mithrilledger.domain.model.Transaction;
import com.trevizan.mithrilledger.domain.model.Wallet;
import com.trevizan.mithrilledger.service.WalletService;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Currency;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public ResponseEntity<WalletResponse> create(@RequestBody WalletRequest request) {
        validateWalletRequest(request);

        Currency currency;
        try {
            currency = Currency.getInstance(request.currency());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid currency code: " + request.currency());
        }

        Wallet wallet = walletService.createWallet(
            request.ownerId(),
            currency
        );

        WalletResponse response = WalletResponse.from(wallet);

        URI location = URI.create("/api/v1/wallets/" + wallet.getId());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public WalletResponse getWallet(@PathVariable UUID id) {
        return WalletResponse.from(walletService.getWalletById(id));
    }

    @PostMapping("/credit")
    public ResponseEntity<WalletResponse> credit(@RequestBody WalletAmountRequest request) {
        validateWalletAmountRequest(request);

        Wallet wallet = walletService.credit(
            request.walletId(),
            request.amount()
        );

        WalletResponse response = WalletResponse.from(wallet);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/debit")
    public ResponseEntity<WalletResponse> debit(@RequestBody WalletAmountRequest request) {
        validateWalletAmountRequest(request);

        Wallet wallet = walletService.debit(
            request.walletId(),
            request.amount()
        );

        WalletResponse response = WalletResponse.from(wallet);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@RequestBody TransferRequest request) {
        validateTransferRequest(request);

        Transaction transaction = walletService.transfer(
            walletService.getWalletById(request.fromWalletId()),
            walletService.getWalletById(request.toWalletId()),
            request.amount()
        );

        TransactionResponse response = TransactionResponse.from(transaction);

        URI location = URI.create("/api/v1/wallets/transactions/" + transaction.getId());
        return ResponseEntity.created(location).body(response);
    }

    private void validateWalletRequest(WalletRequest request) {
        if (request.ownerId() == null || request.ownerId().isBlank()) {
            throw new IllegalArgumentException("OwnerId is required.");
        }
        if (request.currency() == null || request.currency().isBlank()) {
            throw new IllegalArgumentException("Currency is required.");
        }
    }

    private void validateWalletAmountRequest(WalletAmountRequest request) {
        if (request.walletId() == null) {
            throw new IllegalArgumentException("WalletId is required.");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0.");
        }
    }

    private void validateTransferRequest(TransferRequest request) {
        if (request.fromWalletId() == null) {
            throw new IllegalArgumentException("Origin Wallet ID is required.");
        }
        if (request.toWalletId() == null) {
            throw new IllegalArgumentException("Destination Wallet ID is required.");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0.");
        }
    }

}
