package com.trevizan.mithrilledger.wallet;

import com.trevizan.mithrilledger.domain.exchange.ExchangeClient;
import com.trevizan.mithrilledger.domain.model.Transaction;
import com.trevizan.mithrilledger.domain.model.Wallet;
import com.trevizan.mithrilledger.exception.domain.InsufficientBalanceException;
import com.trevizan.mithrilledger.exception.domain.WalletNotFoundException;
import com.trevizan.mithrilledger.repository.TransactionRepository;
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

class WalletServiceTest {

    private WalletRepository walletRepository;
    private WalletService walletService;
    private TransactionRepository transactionRepository;
    private ExchangeClient exchangeClient;

    @BeforeEach
    void setUp() {
        walletRepository = Mockito.mock(WalletRepository.class);
        transactionRepository = Mockito.mock(TransactionRepository.class);
        exchangeClient = Mockito.mock(ExchangeClient.class);
        walletService = new WalletService(walletRepository, transactionRepository, exchangeClient);
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

    @Test
    void shouldCreditWalletSuccessfully() {
        Wallet wallet = Wallet.create("1234", Currency.getInstance("EUR"));
        UUID id = wallet.getId();

        when(walletRepository.findById(id)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

        Wallet credited = walletService.credit(id, BigDecimal.valueOf(100.5));

        assertEquals(0, credited.getBalance().compareTo(BigDecimal.valueOf(100.5)));
        verify(walletRepository, times(1)).findById(id);
        verify(walletRepository, times(1)).save(wallet);
    }

    @Test
    void shouldDebitWalletSuccessfully() {
        Wallet wallet = Wallet.create("1234", Currency.getInstance("EUR"));
        wallet.credit(BigDecimal.valueOf(200));
        UUID id = wallet.getId();

        when(walletRepository.findById(id)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

        Wallet debited = walletService.debit(id, BigDecimal.valueOf(150));

        assertEquals(0, debited.getBalance().compareTo(BigDecimal.valueOf(50)));
        verify(walletRepository, times(1)).findById(id);
        verify(walletRepository, times(1)).save(wallet);
    }

    @Test
    void shouldThrowExceptionWhenDebitInsufficientBalance() {
        Wallet wallet = Wallet.create("1234", Currency.getInstance("EUR"));
        UUID id = wallet.getId();

        when(walletRepository.findById(id)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.debit(id, BigDecimal.valueOf(200)))
            .isInstanceOf(InsufficientBalanceException.class)
            .hasMessageContaining(id.toString());

        verify(walletRepository, times(1)).findById(id);
        verify(walletRepository, times(0)).save(wallet);
    }

    @Test
    void shouldThrowExceptionWhenWalletNotFoundOnCredit() {
        UUID id = UUID.randomUUID();
        when(walletRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.credit(id, BigDecimal.TEN))
            .isInstanceOf(WalletNotFoundException.class)
            .hasMessageContaining(id.toString());

        verify(walletRepository, times(1)).findById(id);
    }

    @Test
    void shouldThrowExceptionWhenWalletNotFoundOnDebit() {
        UUID id = UUID.randomUUID();
        when(walletRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.debit(id, BigDecimal.TEN))
            .isInstanceOf(WalletNotFoundException.class)
            .hasMessageContaining(id.toString());

        verify(walletRepository, times(1)).findById(id);
    }

    @Test
    void shouldTransferBetweenWalletsSuccessfully() {
        Wallet fromWallet = Wallet.create("1234", Currency.getInstance("EUR"));
        fromWallet.credit(BigDecimal.valueOf(200));
        Wallet toWallet = Wallet.create("1235", Currency.getInstance("EUR"));

        UUID fromId = fromWallet.getId();
        UUID toId = toWallet.getId();
        BigDecimal transferAmount = BigDecimal.valueOf(150);

        when(walletRepository.findById(fromId)).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findById(toId)).thenReturn(Optional.of(toWallet));
        when(walletRepository.save(fromWallet)).thenReturn(fromWallet);
        when(walletRepository.save(toWallet)).thenReturn(toWallet);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        Transaction transaction = walletService.transfer(fromWallet, toWallet, transferAmount);

        assertEquals(fromWallet, transaction.getFromWallet());
        assertEquals(toWallet, transaction.getToWallet());
        assertEquals(0, transaction.getAmount().compareTo(transferAmount));
        assertEquals(0, fromWallet.getBalance().compareTo(BigDecimal.valueOf(50)));
        assertEquals(0, toWallet.getBalance().compareTo(BigDecimal.valueOf(150)));

        verify(walletRepository, times(1)).save(fromWallet);
        verify(walletRepository, times(1)).save(toWallet);
        verify(transactionRepository, times(1)).save(transaction);
    }

    @Test
    void shouldThrowExceptionWhenTransferAmountIsZeroOrNegative() {
        Wallet fromWallet = Wallet.create("1234", Currency.getInstance("EUR"));
        Wallet toWallet = Wallet.create("1235", Currency.getInstance("EUR"));

        fromWallet.credit(BigDecimal.valueOf(100));

        assertThatThrownBy(() -> walletService.transfer(fromWallet, toWallet, BigDecimal.ZERO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Amount must be greater than 0.");

        assertThatThrownBy(() -> walletService.transfer(fromWallet, toWallet, BigDecimal.valueOf(-50)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Amount must be greater than 0.");
    }

    @Test
    void shouldThrowExceptionWhenFromAndToWalletAreSame() {
        Wallet wallet = Wallet.create("1234", Currency.getInstance("EUR"));
        wallet.credit(BigDecimal.valueOf(100));

        assertThatThrownBy(() -> walletService.transfer(wallet, wallet, BigDecimal.valueOf(50)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Origin and Destination Wallet must be different.");
    }

    @Test
    void shouldThrowExceptionWhenFromWalletHasInsufficientBalance() {
        Wallet fromWallet = Wallet.create("1234", Currency.getInstance("EUR"));
        Wallet toWallet = Wallet.create("1235", Currency.getInstance("EUR"));
        fromWallet.credit(BigDecimal.valueOf(50));

        BigDecimal transferAmount = BigDecimal.valueOf(100);

        assertThatThrownBy(() -> walletService.transfer(fromWallet, toWallet, transferAmount))
            .isInstanceOf(InsufficientBalanceException.class)
            .hasMessageContaining(fromWallet.getId().toString());
    }

}
