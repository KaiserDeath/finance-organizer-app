# MoneyFlow Peru

A modern, offline-first personal‑finance organizer for the Peru market — track every expense,
know where money goes, what's due, and how much remains. Built with Kotlin, Jetpack Compose,
Material 3, and a clean multi‑module architecture designed to scale into a full product.

> **Status: Phase 0 (foundation) + Phase 1 MVP slice.** The app launches to a themed dashboard,
> seeds Peru categories & payment methods, and lets you add an expense and see it in the list and
> month total. Later phases (budgets, analytics, accounts, security, widgets, AI) are architected
> for but not yet implemented — see the roadmap.

---

## Requirements

- **Android Studio** Ladybug (2024.2) or newer.
- **JDK 17–21** for the Gradle build. ⚠️ The build targets JDK 17 bytecode and Gradle 8.11 runs
  on JDK 17–21. A newer system JDK (e.g. 25) is **not** supported by this Gradle/AGP combo. In
  Android Studio this is automatic (it bundles a compatible JDK). From the command line, point
  Gradle at JDK 21, e.g. `JAVA_HOME=/path/to/jdk-21` or `-Dorg.gradle.java.home=...`.
- **Android SDK** with API 35 installed (compileSdk 35, minSdk 26).

## Getting started

1. Open the `Finance organizer` folder in **Android Studio** ("Open", select the folder with
   `settings.gradle.kts`).
2. Let Gradle sync. Android Studio will generate the Gradle wrapper JAR if it's missing.
   - CLI alternative (needs a local Gradle 8.11): `gradle wrapper` then `./gradlew assembleDebug`.
3. Run the `app` configuration on an emulator or device (API 26+).

Build from the command line:

```bash
./gradlew assembleDebug        # build the debug APK
./gradlew test                 # run JVM unit tests (Money, GetDashboardUseCase)
./gradlew :app:installDebug    # install on a connected device
```

## Architecture

Clean Architecture, multi‑module, one‑way dependencies
(`:app` → `:feature:*` → `:core:*`; within core `:core:data` → `:core:domain` → `:core:model`).
Shared Gradle config lives in `build-logic/` convention plugins; all versions in
`gradle/libs.versions.toml`.

```
:app                     Application, MainActivity, theme wiring, NavHost + bottom bar
core/
  :core:model            pure-Kotlin domain models (Transaction, Account, Category, …)
  :core:common           Money (minor-unit) utils, dispatchers qualifiers
  :core:domain           repository interfaces + use cases (pure Kotlin, unit-tested)
  :core:database         Room: entities, DAOs, converters, Peru seed data, DI
  :core:datastore        DataStore-backed SettingsRepository
  :core:data             repository impls, entity⇄domain mappers, DI (dispatchers, clock)
  :core:designsystem     theme, tokens, MoneyCard/StatTile/DonutChart/EmptyState/…
  :core:ui               AmountText, TransactionRow, date format, bank-app launcher
feature/
  :feature:dashboard        month spend, today, income, category donut, recent list
  :feature:transactions     date-grouped list, swipe-to-delete + undo
  :feature:addedit          add/edit expense (amount, category, method, date, notes)
  :feature:categories       category CRUD with color/icon picker
  :feature:paymentmethods   payment methods + tap-to-launch bank apps
```

### Key design decisions

- **Money is stored as `Long` minor units (céntimos)** — never floating point. Formatting to
  `S/ 1,234.56` (locale es-PE) happens only at the UI edge in `core:common/Money`.
- **Unified `Transaction`** (EXPENSE / INCOME / TRANSFER) rather than a separate expense table,
  so income tracking and net worth fall out naturally later.
- **Accounts** are first-class so "how much remains" and net worth are possible.
- The **full Room schema** (budgets, recurring, installments, reminders, savings, summaries,
  exchange rates, attachments) is defined now to avoid disruptive migrations; DAOs/repositories
  are added per phase.
- **Reactive**: DAOs expose `Flow`, repositories map to domain models, ViewModels expose a single
  immutable `StateFlow<UiState>`, everything constructor-injected via Hilt (testable with fakes).

### ⚠️ Peru bank package ids

Payment-method deep links (`core:database/SeedData.kt` and the `<queries>` in the app manifest)
use best-known package ids for Yape/BCP/Interbank/BBVA. **Verify these before release** — if an
id is wrong, `launchPaymentApp` (`core:ui/util/AppLauncher.kt`) safely falls back to a Play Store
search. "Plin" has no standalone app (it lives inside partner bank apps), so it has no deep link.

## Roadmap

- **Phase 2** — Budgets, upcoming payments, recurring templates (WorkManager), reminders/notifications.
- **Phase 3** — Analytics & monthly reports (Vico charts, CSV/PDF export).
- **Phase 4** — Accounts, income, transfers, savings goals, net worth, multi-currency.
- **Phase 5** — Biometric/PIN lock, backup/restore, home-screen widgets (Glance), onboarding, full search/filters.
- **Phase 6** — Smart/AI insights behind the `SmartInsights` interface (rule-based first, LLM later).

## Testing

- `core:common` — `MoneyTest` (format/parse/rounding).
- `core:domain` — `GetDashboardUseCaseTest` (month/today/income aggregation with fakes).

Run with `./gradlew test`.
