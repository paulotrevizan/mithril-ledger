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
- Wallet credit
- Wallet debit
- Wallet transfers
- Stable and explicit API contract
- Consistent error responses
- Controller-level validation
- Controller tests with mocked services
- Integration tests covering end-to-end HTTP flows

> ⚠️ Idempotency and concurrency handling are **not implemented yet**.  
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

## Credit Wallet

**POST** `/api/v1/wallets/credit`

Credits a wallet with a specified amount.

### Request Body

```json
{
  "walletId": "uuid",
  "amount": 100.50
}
```

### Validation Rules

- `walletId` must not be null
- `amount` must not be null and **greater than 0**

---

### Success Response

**200 OK**

**Body**

```json
{
  "id": "uuid",
  "ownerId": "string",
  "balance": 150.50,
  "currency": "EUR",
  "createdAt": "2026-01-28T13:56:51Z"
}
```
---

## Debit Wallet

**POST** `/api/v1/wallets/debit`

Debits a wallet with a specified amount.

### Request Body

```json
{
  "walletId": "uuid",
  "amount": 50.25
}
```

### Validation Rules

- `walletId` must not be null
- `amount` must not be null and **greater than 0**
- Wallet must have **sufficient balance** (cannot go negative)

### Success Response

**200 OK**

**Body**

```json
{
  "id": "uuid",
  "ownerId": "string",
  "balance": 100.25,
  "currency": "EUR",
  "createdAt": "2026-01-28T13:56:51Z"
}
```

## Transfer Between Wallets

**POST** `/api/v1/wallets/transfer`

Transfers an amount from one wallet to another.

### Request Body

```json
{
  "fromWalletId": "uuid",
  "toWalletId": "uuid",
  "amount": 50.00
}
```

### Validation Rules

- `fromWalletId` must not be null
- `toWalletId` must not be null
- `fromWalletId` and toWalletId must be different
- `amount` must be greater than 0
- Source wallet must have sufficient balance
- Both wallets must exist

---

### Success Response

**201 Created**

**Headers**

```
Location: /api/v1/wallets/transactions/{transactionId}
```

**Body**

```json
{
  "id": "uuid",
  "fromWalletId": "uuid",
  "toWalletId": "uuid",
  "amount": 50.00,
  "createdAt": "2026-02-12T14:03:22Z"
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

| Status | Description                              |
|--------|------------------------------------------|
| 400    | Invalid or missing input                 |
| 404    | Wallet not found                         |
| 409    | Insufficient balance for debit operation |

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

- Ledger based balance model
- Idempotency keys
- Concurrency handling
- External integrations
- Observability and metrics

---

## Author

**Paulo Trevizan**  
Software Engineer
