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
