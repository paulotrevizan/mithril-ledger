# DECISIONS — Mithril Ledger

## Wallet Domain

### Use of Currency
- **Decision:** use `java.util.Currency` for wallet currency representation.
- **Rationale:** standard class, enough for MVP balance operations.
- **Trade-off:** advanced rules may require a custom `Currency` class in future iterations.

### Domain encapsulation of balance
- **Decision:** all balance modifications handled by domain methods `credit` and `debit`, avoiding public setter for balance.
- **Rationale:** enforces business rules and prevents invalid states at the entity level.
- **Trade-off:** service/controller cannot bypass domain invariants, which is intentional to keep consistency.

### Debit / Credit operations
- **Decision:** domain provides `credit` and `debit` methods with **strict invariants**:
    - Amount must be positive
    - Debit cannot result in negative balance
- **Rationale:** domain methods enforce core business rules and invariants, making the domain the **source of truth** for wallet state.
- **API layer validation:** the controller performs minimal input validation (null checks, non-positive amounts) **before calling the domain**, to return meaningful HTTP errors (400 Bad Request).  
  This is not a duplication of business logic, but a **user friendly early check**.
- **Trade-off:** some simple validation rules appear in both controller and domain, but this separation ensures:
    - clear HTTP contract for API consumers
    - domain consistency even if the API is bypassed
    - safe and explicit error handling without exposing internal exceptions

### Transfer operation
- **Decision:** implement transfer in `WalletService` as debit and credit operation.
- **Rationale:** transfer between two wallets; domain invariants remain in `Wallet`.
- **Trade-off:** 
  - service coordinates multiple wallets 
  - domain still enforces balance rules
  - `@Transactional` ensures atomicity;
  - persist `Transaction` entity for traceability.

### Exception strategy
- **Decision:** use a domain-specific exception (`InsufficientBalanceException`) for debit violations.
- **Rationale:** clearly communicates business rules; improves maintainability and readability.
- **Trade-off:** could use generic exceptions, but explicit domain exceptions improve clarity and future scalability.

### API boundary validation
- **Decision:** validate HTTP input at the controller/service boundary, while keeping business invariants inside the domain model.
- **Rationale:** separates concerns between transport-level validation and domain rules, keeping the domain free of HTTP semantics.
- **Trade-off:** some validation logic is duplicated across layers, but responsibilities remain clear.

### Persistence setup
- **Decision:** prepare JPA mappings with `@Entity`, `@Version`, and `@Convert` for `Currency`.
- **Rationale:** sets up optimistic locking and proper persistence for future extensions.
- **Trade-off:** database constraints are minimal in early iterations.

### API model separation
- **Decision:** expose dedicated request/response DTOs instead of JPA entities.
- **Rationale:** protects the domain model, enables API evolution and avoids leaking internal persistence concerns.
- **Trade-off:** adds mapping code, but provides long-term flexibility and safety.

### Observability (audit logging)
- **Decision:** add minimal logging only for key business events:
    - Wallet creation (`createWallet`)
    - Transfers (`transfer`)
- **Rationale:** supports audit and traceability for important operations without polluting logs with high volume balance mutations.
- **Trade-off:** individual credit/debit operations are not logged; relies on domain invariants and tests for correctness.

### External Exchange Service Integration
- **Decision:** integrate with external currency exchange API via `HttpExchangeClient`.
- **Rationale:** enables automatic currency conversion during wallet transfers; keeps business logic simple and consistent.
- **Trade-off:** external dependency introduces risk of downtime or errors; mitigated by circuit breaker and retry strategy (using Resilience4j).

### Resilience and Fault Tolerance
- **Decision:** use Resilience4j circuit breaker for external API calls.
- **Rationale:** prevents external failures from cascading into the domain logic; ensures wallet operations remain responsive even if exchange service fails.
- **Trade-off:** temporary unavailability of exchange rates may result in partial functionality; fallback strategies right now is to block transfers between different currencies, as the exchange API doesn't exist.

### Retry Strategy
- **Decision:** implement a limited retry mechanism for transient network issues.
- **Rationale:** improves reliability for brief outages without overloading the external service.
- **Trade-off:** increases response time a little during retries; failed retries are surfaced as meaningful exceptions.

### Error Handling for External Calls
- **Decision:** propagate meaningful exceptions to the API layer for failed calls (e.g., `ResourceAccessException`).
- **Rationale:** allows API to return proper HTTP codes and messages; avoids silent failures.
- **Trade-off:** some failures will still bubble up as 5xx errors; future improvements may include proper fallback responses.

---

## Idempotency

### Idempotency strategy — current (DB unique constraint)
- **Decision:** use a `UNIQUE` constraint on `transactions.idempotency_key` as the hard guarantee against duplicate inserts. Before attempting the insert, a SELECT check runs within the same transaction as a soft early-exit for plain replays. The DB constraint is the real safety net for concurrent races that slip past the check.
- **Rationale:** the DB constraint is the only guarantee that holds under true concurrency; application-level checks alone cannot be made atomic without additional locking. This approach requires no external infrastructure and is the simplest correct foundation.
- **Trade-off:** every retry that reaches the service layer hits the DB. The standard fintech approach (Stripe, Adyen) goes further: a dedicated `idempotency_keys` table with a `SELECT FOR UPDATE` on the key record before any business logic, which serializes concurrent requests at the DB level and avoids exception-as-flow-control entirely. This is the planned evolution (see **Future Improvements**).

### Transaction boundary decomposition — WalletTransferExecutor
- **Decision:** introduce `WalletTransferExecutor` as a dedicated Spring component that owns the `@Transactional` boundary for the transfer operation. `WalletService.transfer(...)` is intentionally non-transactional and acts as the orchestrator.
- **Rationale:** in DDD terms the application service (`WalletService`) coordinates; the inner component (`WalletTransferExecutor`) owns the atomic unit of work. This is also necessary in Spring because `@Transactional` is proxy-based: a `@Transactional` method called from within the same bean bypasses the proxy and runs outside any transaction. Separating the TX boundary into its own bean makes the boundary explicit and testable.
- **Trade-off:** an extra class in the service layer. The alternative is programmatic `TransactionTemplate`, which is more verbose but possibly clearer in complex scenarios. For the current scope, a dedicated component is idiomatic Spring and sufficient.

### Concurrency recovery — optimistic approach (pragmatic shortcut)
- **Decision:** on `DataIntegrityViolationException` (duplicate key on concurrent insert), catch after the transaction has rolled back in `WalletService`, then re-fetch the already-committed transaction in a fresh `REQUIRES_NEW` read.
- **Rationale:** after a `DataIntegrityViolationException` the Spring transaction is irrecoverably marked rollback-only; any further query inside it triggers autoflush and cascades into `UnexpectedRollbackException`. Catching outside the TX boundary and opening a new read transaction (`REQUIRES_NEW`) is the cleanest way to recover within the current architecture.
- **Trade-off:** this is exception-as-flow-control, which is a code smell. It works correctly but is not the standard fintech pattern. The standard approach is pessimistic: `SELECT FOR UPDATE` on a dedicated idempotency key record before any side effects, so the "loser" thread blocks and reads the committed result without ever triggering a constraint violation. This is the planned next step.
