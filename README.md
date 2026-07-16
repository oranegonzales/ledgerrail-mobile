# LedgerRail Mobile

[![CI](https://github.com/oranegonzales/ledgerrail-mobile/actions/workflows/ci.yml/badge.svg)](https://github.com/oranegonzales/ledgerrail-mobile/actions/workflows/ci.yml)
[![Security](https://github.com/oranegonzales/ledgerrail-mobile/actions/workflows/security.yml/badge.svg)](https://github.com/oranegonzales/ledgerrail-mobile/actions/workflows/security.yml)

LedgerRail Mobile is a native Kotlin and Jetpack Compose client for the [LedgerRail Core](https://github.com/oranegonzales/ledgerrail-core) payment-reliability sandbox. It demonstrates an Android client consuming a real Java/PostgreSQL API while preserving exact decimal values and safe retry semantics.

This is a portfolio sandbox. It never moves real money and must only use synthetic data.

## What the app demonstrates

- Kotlin with a fully Compose and Material 3 interface
- Phone and tablet-responsive layouts plus light and dark themes
- Unidirectional data flow with `ViewModel`, `StateFlow`, and immutable UI state
- A repository boundary around Retrofit, OkHttp, and Moshi
- Exact `BigDecimal` money serialization
- Public-demo access with idempotency headers and HTTP 429 handling
- Creation and retrieval of simulated pay-ins and pay-outs
- Inspection of the matching debit and credit ledger entries
- An explicit replay control proving that retries do not duplicate a transfer
- Zero-secret recruiter flow against the rate-limited synthetic API
- Automatic cold-start connection to one fixed HTTPS backend with a bounded retry state
- Cleartext and backup protection plus bounded client inputs and server error text
- JVM repository/ViewModel tests, a Compose UI test, lint, release assembly, CodeQL, and dependency review

## Live system

- Backend dashboard: [ledgerrail-core.onrender.com](https://ledgerrail-core.onrender.com/)
- Backend source: [github.com/oranegonzales/ledgerrail-core](https://github.com/oranegonzales/ledgerrail-core)

The backend uses Render Free and Neon PostgreSQL. With the uptime monitor paused, the first request after inactivity can take about one minute while Render wakes the service.

## Run on Windows

1. Install the current stable [Android Studio](https://developer.android.com/studio).
2. Open Android Studio, choose **Open**, and select the `ledgerrail-mobile` folder—not the `app` folder.
3. Allow the Gradle sync to finish. If prompted, install Android SDK 37 and accept the licenses.
4. Open **Tools → Device Manager**, create a recent Pixel virtual device, and start it. A physical Android phone with USB debugging also works.
5. Select the `app` run configuration and click the green **Run** triangle.
6. The app immediately connects to `https://ledgerrail-core.onrender.com/`; there is no server or API-key setup screen.
7. If Render is asleep, leave the app open while the first request wakes it. A **Retry** action appears only if that connection fails.

## Exercise the reliability flow

1. Tap **New ID** to create a synthetic account.
2. Choose **Pay in** or **Pay out**, then enter an amount and three-letter currency.
3. Tap **Create transfer**.
4. Select the transfer to inspect its balanced debit and credit.
5. Change the amount, then tap **Replay exact request**. The app sends the original payload and idempotency key; the API returns the original transfer rather than creating a duplicate.

## Verify from the terminal

From PowerShell in this repository:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease assembleDebugAndroidTest
```

The debug APK is generated at `app\build\outputs\apk\debug\app-debug.apk`. GitHub Actions runs the same checks on every pull request and also analyzes Java/Kotlin with CodeQL.

## Design notes

See [Architecture](docs/ARCHITECTURE.md) for the data flow, correctness decisions, public-demo security boundary, and free-hosting topology.

## Technology

| Area | Choice |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| State | Lifecycle ViewModel + StateFlow |
| Network | Retrofit + OkHttp + Moshi |
| Concurrency | Kotlin coroutines |
| Tests | JUnit, coroutines-test, MockWebServer, Compose UI test |
| Build | Gradle version catalog + GitHub Actions |

## Current limitations

- The anonymous API is only for synthetic portfolio data; it is rate-limited rather than user-authenticated.
- The app is intentionally online-only; PostgreSQL remains the sole source of truth.
- The Render Free service can sleep or restart.
- A signed release APK and store distribution are later release steps; no signing secret belongs in this repository.
