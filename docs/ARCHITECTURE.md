# Architecture

LedgerRail Mobile is a thin, native portfolio client for the LedgerRail Core sandbox. The backend remains the source of truth; the app never attempts to reproduce ledger rules locally.

```mermaid
flowchart TD
    Screen[Compose screen] -->|Events| ViewModel[StateFlow ViewModel]
    ViewModel -->|Domain operations| Repository[Repository contract]
    Repository --> Client[Retrofit + OkHttp + Moshi]
    Client -->|Anonymous HTTPS| Core[LedgerRail Core on Render]
    Core --> Database[(Neon PostgreSQL)]
```

## State and data flow

The UI follows unidirectional data flow:

1. `LedgerRailViewModel` starts the fixed-backend health check as soon as it is created.
2. `LedgerRailApp` renders one immutable `LedgerRailUiState`.
3. User actions call methods on `LedgerRailViewModel`.
4. The ViewModel validates bounded domain input and calls `LedgerRailRepository`.
5. The repository maps API DTOs to domain models and normalizes network failures.
6. The ViewModel publishes a new state through `StateFlow`.

The repository boundary makes ViewModel tests deterministic and leaves room for a future offline implementation without coupling Compose to Retrofit.

## Correctness choices

- Monetary values use `BigDecimal`; neither the API layer nor UI converts them to floating point.
- A fresh idempotency key is created for each new submission.
- “Replay exact request” intentionally reuses both the prior key and prior payload, demonstrating safe retry behavior.
- Changing the synthetic account invalidates the replay context.
- Starting a new operation cancels the previous UI job to prevent stale results from overwriting newer state.
- The release client is constructed once with the fixed Render HTTPS endpoint; localhost HTTP is accepted only by JVM test construction.

## Security boundary

The app contains and requests no secret. It calls only LedgerRail Core's fixed anonymous synthetic-transfer endpoint. Cleartext traffic and Android backups are disabled, only system trust anchors are accepted, inputs and displayed server errors are bounded, and only the Internet permission is requested. The backend enforces a per-client minute limit and a PostgreSQL-backed daily write quota; private metrics, reconciliation, and failed-event replay endpoints are not exposed by the app.

This is appropriate only for a portfolio sandbox containing no real money or personal data. A real payment application would place user authentication and authorization in front of the API, use short-lived tokens, bind access to accounts, protect tokens with platform-backed storage, and apply device and risk controls.

## Free-hosting topology

The Android app does not require a web host: it runs on the phone or emulator and calls the existing Render HTTPS service. Source and CI live on GitHub; a later signed demo APK can be attached to a GitHub Release at no hosting cost. Render may sleep while the monitor is paused, so the first connection can take about a minute.
