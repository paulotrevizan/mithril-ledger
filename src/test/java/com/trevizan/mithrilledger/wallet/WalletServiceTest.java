package com.trevizan.mithrilledger.wallet;

import com.trevizan.mithrilledger.domain.Wallet;
import com.trevizan.mithrilledger.exception.domain.WalletNotFoundException;
import com.trevizan.mithrilledger.repository.WalletRepository;
import com.trevizan.mithrilledger.service.WalletService;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WalletServiceTests {

    private WalletRepository walletRepository;
    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletRepository = Mockito.mock(WalletRepository.class);
        walletService = new WalletService(walletRepository);
    }

    @Test
    void shouldCreateWalletWithZeroBalance() {
        String ownerId = "1234";
        Currency currency = Currency.getInstance("EUR");
        Wallet wallet = Wallet.create(ownerId, currency);

        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

        Wallet created = walletService.createWallet(ownerId, currency);

        assertNotNull(created.getId());
        assertEquals(ownerId, created.getOwnerId());
        assertEquals(currency, created.getCurrency());
        assertEquals(0, created.getBalance().compareTo(BigDecimal.ZERO));
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    void shouldGetExistingWalletById() {
        Wallet wallet = Wallet.create("1234", Currency.getInstance("EUR"));
        UUID id = wallet.getId();

        when(walletRepository.findById(id)).thenReturn(Optional.of(wallet));

        Wallet walletFound = walletService.getWalletById(id);

        assertEquals(wallet, walletFound);
        verify(walletRepository, times(1)).findById(id);
    }

    @Test
    void shouldThrowExceptionWhenWalletNotFound() {
        UUID id = UUID.randomUUID();
        when(walletRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getWalletById(id))
            .isInstanceOf(WalletNotFoundException.class)
            .hasMessageContaining("not found");
        verify(walletRepository, times(1)).findById(id);
    }

}
