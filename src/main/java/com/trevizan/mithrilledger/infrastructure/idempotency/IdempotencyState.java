package com.trevizan.mithrilledger.infrastructure.idempotency;

public record IdempotencyState(
    IdempotencyStatus status,
    Integer responseStatus,
    String responseBody
) { }
