package com.trevizan.mithrilledger.infrastructure.idempotency;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {

    private final Map<String, IdempotencyState> store = new ConcurrentHashMap<>();

    public IdempotencyState tryStart(String key) {
        IdempotencyState newState = new IdempotencyState(
            IdempotencyStatus.IN_PROGRESS,
            null,
            null
        );

        IdempotencyState existing = store.putIfAbsent(key, newState);

        return existing != null ? existing : newState;
    }

    public void markCompleted(String key, int status, String body) {
        store.put(key, new IdempotencyState(IdempotencyStatus.COMPLETED, status, body));
    }

}
