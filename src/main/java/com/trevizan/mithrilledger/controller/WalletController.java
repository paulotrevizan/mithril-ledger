package com.trevizan.mithrilledger.controller;

import com.trevizan.mithrilledger.controller.dto.WalletRequest;
import com.trevizan.mithrilledger.controller.dto.WalletResponse;
import com.trevizan.mithrilledger.domain.Wallet;
import com.trevizan.mithrilledger.service.WalletService;

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
        if (request.ownerId() == null || request.ownerId().isBlank()) {
            throw new IllegalArgumentException("OwnerId is required.");
        }

        if (request.currency() == null || request.currency().isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }

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

}
