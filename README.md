# 🏦 Mithril Ledger

## Overview

Mithril Ledger is a **simplified financial service** built with **Java and Spring Boot**, developed incrementally to demonstrate **real world based engineering decisions** commonly found in data-sensitive systems such as wallets, balances and payments.

The project prioritizes:

- Clear and explicit business rules
- Data correctness and consistency
- Predictable and safe behavior
- Reliable automated tests
- Explicit architectural trade-offs

This repository is used for **learning and practice**, evolving step by step avoiding premature complexity.

---

## Current Scope

At its current stage, the system supports:

- Wallet creation
- Wallet retrieval by id
- Stable and explicit API contract
- Consistent error responses
- Controller-level validation
- Controller tests with mocked services

> ⚠️ Transfers, balance mutations, idempotency and concurrency handling are **not implemented yet**.  
> Planned work is tracked explicitly (see **Future Improvements**).

---

## Architecture

```
Controller
   ↓
Service (application logic)
   ↓
Domain (entities & invariants)
   ↓
Repository (persistence)
```

### Design Principles

- Clear separation of layers
- Explicit input validation in controllers
- Services do not depend on HTTP or DTO concerns
- Domain focused on invariants only
- No hidden or implicit behavior

---

## API Contract

Base path:

```
/api/v1/wallets
```

---

## Create Wallet

**POST** `/api/v1/wallets`

Creates a new wallet.

### Request Body

```json
{
  "ownerId": "string",
  "currency": "ISO-4217 currency code"
}
```

### Validation Rules

- `ownerId` must not be null or blank
- `currency` must be a valid ISO-4217 code (e.g.: `EUR`, `USD`, `GBP`)

---

### Success Response

**201 Created**

**Headers**

```
Location: /api/v1/wallets/{walletId}
```

**Body**

```json
{
  "id": "uuid",
  "ownerId": "string",
  "balance": 0,
  "currency": "EUR",
  "createdAt": "2026-01-28T13:56:51Z"
}
```

---

## Get Wallet by Id

**GET** `/api/v1/wallets/{id}`

Returns wallet details.

### Path Parameters

| Name | Type | Description       |
|------|------|-------------------|
| id   | UUID | Wallet identifier |

---

### Success Response

**200 OK**

```json
{
  "id": "uuid",
  "ownerId": "string",
  "balance": 0,
  "currency": "EUR",
  "createdAt": "2026-01-28T13:56:51Z"
}
```

---

## Error Handling

All errors are returned in a **consistent JSON format**.

### Error Response Schema

```json
{
  "timestamp": "2026-01-28T13:56:51.910481Z",
  "status": 400,
  "error": "Bad Request",
  "message": "OwnerId is required.",
  "path": "/api/v1/wallets"
}
```

---

### Common Error Scenarios

| Status | Description              |
|--------|--------------------------|
| 400    | Invalid or missing input |
| 404    | Wallet not found         |

---

## Testing

- Controller tests using `MockMvc` (services mocked)
- Services tested in isolation with unit tests
- **Integration tests added for Wallet API (POST / GET)**  
  Uses Spring Boot Test + H2 in-memory DB, validates HTTP + JSON contract end-to-end  
  No business logic is executed yet

---

## Running the Application

```bash
./mvnw spring-boot:run
```

---

## Architectural Decisions

All relevant architectural decisions are documented in:

```
DECISIONS.md
```

---

## Future Improvements

> 🚫 Scope is intentionally controlled.  
> Any new idea must be written here and **not implemented immediately**.

- Wallet transfers
- Balance mutations
- Ledger based balance model
- Idempotency keys
- Concurrency handling
- External integrations
- Observability and metrics

---

## Author

**Paulo Trevizan**  
Software Engineer
