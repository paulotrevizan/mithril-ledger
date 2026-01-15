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

### Persistence setup
- **Decision:** prepare JPA mappings with `@Entity`, `@Version`, and `@Convert` for `Currency`.
- **Rationale:** sets up optimistic locking and proper persistence for future extensions.
- **Trade-off:** initial scope is still not using DB; DB constraints to be added later.
