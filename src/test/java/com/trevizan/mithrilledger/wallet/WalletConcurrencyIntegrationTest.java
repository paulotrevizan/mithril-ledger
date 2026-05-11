package com.trevizan.mithrilledger.wallet;

import com.trevizan.mithrilledger.domain.exchange.ExchangeClient;
import com.trevizan.mithrilledger.domain.model.Transaction;
import com.trevizan.mithrilledger.domain.model.Wallet;
import com.trevizan.mithrilledger.repository.TransactionRepository;
import com.trevizan.mithrilledger.repository.WalletRepository;
import com.trevizan.mithrilledger.service.WalletService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class WalletConcurrencyIntegrationTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @MockitoBean
    private ExchangeClient exchangeClient;

    private ExecutorService executorService;

    @BeforeEach
    void setup() {
        executorService = Executors.newFixedThreadPool(5);
        when(exchangeClient.getRate(anyString(), anyString())).thenReturn(BigDecimal.ONE);
    }

    @AfterEach
    void cleanup() {
        executorService.shutdown();
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
    }

    @Test
    void shouldProcessOnlyOneTransferForSameIdempotencyKey() throws Exception {
        Wallet fromWallet = walletService.createWallet(
            "Amuro Ray",
            Currency.getInstance("EUR")
        );

        Wallet toWallet = walletService.createWallet(
            "Hathaway Noa",
            Currency.getInstance("EUR")
        );

        walletService.credit(
            fromWallet.getId(),
            new BigDecimal("1000.00")
        );

        String idempotencyKey = UUID.randomUUID().toString();
        int concurrentRequests = 5;

        CountDownLatch readyLatch = new CountDownLatch(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Callable<Transaction>> tasks = new ArrayList<>();
        for (int i = 0; i < concurrentRequests; i++) {
            tasks.add(() -> {
                readyLatch.countDown();
                boolean started = startLatch.await(5, TimeUnit.SECONDS);
                Assertions.assertTrue(started);

                Wallet source = walletService.getWalletById(fromWallet.getId());
                Wallet destination = walletService.getWalletById(toWallet.getId());

                return walletService.transfer(
                    source,
                    destination,
                    new BigDecimal("100.00"),
                    idempotencyKey
                );
            });
        }

        List<Future<Transaction>> futures = new ArrayList<>();
        for (Callable<Transaction> task : tasks) {
            futures.add(executorService.submit(task));
        }

        boolean allThreadsReady = readyLatch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(allThreadsReady);

        startLatch.countDown();

        List<Transaction> transactions = new ArrayList<>();
        for (Future<Transaction> future : futures) {
            try {
                Transaction transaction = future.get(10, TimeUnit.SECONDS);
                transactions.add(transaction);
            } catch (ExecutionException e) {
                Throwable rootCause = e.getCause();
                if (rootCause != null) {
                    rootCause.printStackTrace();
                }
                throw e;
            }
        }

        Wallet updatedFromWallet = walletRepository.findById(fromWallet.getId()).orElseThrow();
        Wallet updatedToWallet = walletRepository.findById(toWallet.getId()).orElseThrow();
        List<Transaction> persistedTransactions = transactionRepository.findAll();

        Assertions.assertEquals(
            1,
            persistedTransactions.size()
        );
        Assertions.assertEquals(
            new BigDecimal("900.00"),
            updatedFromWallet.getBalance()
        );
        Assertions.assertEquals(
            new BigDecimal("100.00"),
            updatedToWallet.getBalance()
        );

        UUID expectedTransactionId = persistedTransactions.get(0).getId();

        for (Transaction transaction : transactions) {
            Assertions.assertEquals(expectedTransactionId, transaction.getId());
        }
    }

}
