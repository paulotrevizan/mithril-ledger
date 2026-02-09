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
