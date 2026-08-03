# MoneyFlow Peru

A modern, offline-first personal‑finance organizer for the Peru market — track every expense,
know where money goes, what's due, and how much remains. Built with Kotlin, Jetpack Compose,
Material 3, and a clean multi‑module architecture designed to scale into a full product.

> **Status: Phases 0–6 implemented.** Expenses, budgets, upcoming payments and recurring templates,
> analytics, accounts and savings, multi-currency, app lock, onboarding, the home-screen widget and
> the rule-based insight engine all ship. The UI follows the Propuesta C redesign (`docs/design/`).
> What is *not* done is listed in the roadmap and in `docs/audit-2026-08-01.md`.

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
2. Let Gradle sync. The wrapper (Gradle 8.13) is committed, so no local Gradle install
   is needed — `./gradlew` works from a fresh clone.
3. Run the `app` configuration on an emulator or device (API 26+).

Build from the command line:

```bash
./gradlew assembleDebug        # build the debug APK
./gradlew test                 # run every unit test (see Testing below)
./gradlew :app:installDebug    # install on a connected device
```

## Architecture

Clean Architecture, multi‑module, one‑way dependencies
(`:app` → `:feature:*` → `:core:*`; within core `:core:data` → `:core:domain` → `:core:model`).
Shared Gradle config lives in `build-logic/` convention plugins; all versions in
`gradle/libs.versions.toml`.

```
:app                     Application, MainActivity, NavigationSuiteScaffold shell, onboarding,
                         app lock, Tu dinero and Ajustes
core/
  :core:model            pure-Kotlin domain models (Transaction, Account, Category, …)
  :core:common           Money (minor-unit) utils, dispatchers qualifiers
  :core:domain           repository interfaces + use cases (pure Kotlin, unit-tested)
  :core:database         Room: entities, DAOs, converters, Peru seed data, DI
  :core:datastore        DataStore-backed SettingsRepository
  :core:data             repository impls, entity⇄domain mappers, DI (dispatchers, clock)
  :core:designsystem     theme, tokens, MoneyCard/StatTile/DonutChart/EmptyState/…
  :core:ui               AmountText, TransactionRow, date format, bank-app launcher
  :core:testing          MainDispatcherRule and the shared repository fakes
feature/
  :feature:dashboard        hero band, pace, shortcuts, insights, streak
  :feature:transactions     date-grouped list, swipe-to-delete + undo
  :feature:addedit          add/edit expense (amount, category, method, date, notes)
  :feature:categories       category CRUD with color/icon picker
  :feature:paymentmethods   payment methods + tap-to-launch bank apps
  :feature:budgets          month budget allocation + per-category limits
  :feature:upcoming         what's due, the pay sheet, "ya pagué por fuera"
  :feature:recurring        recurring expense templates
  :feature:analytics        trends and the monthly report
  :feature:accounts         accounts and balances
  :feature:savings          savings goals
  :feature:currency         exchange rates and multi-currency
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

> These phase numbers are the *product* roadmap. `docs/audit-2026-08-01.md` has its own,
> unrelated phase numbering for design-system work — the two collide on "Phase 4" and mean
> different things.

- **Phase 2** — Budgets, upcoming payments, recurring templates (WorkManager), reminders/notifications ✅.
- **Phase 3** — Analytics & monthly reports (Vico charts, CSV/PDF export) ✅.
- **Phase 4** — Accounts, income, transfers, savings goals, net worth, multi-currency ✅.
- **Phase 5** — Biometric/PIN lock, backup/restore (JSON), onboarding, full search/filters,
  home-screen widget (Glance) ✅.
- **Phase 6** — Smart insights behind the `SmartInsights` interface (rule-based engine: cash-flow,
  spending spikes, top category, upcoming/overdue bills) ✅. LLM-backed variant can implement the
  same seam later.

## Testing

Two suites, both run by CI on every pull request.

```bash
./gradlew test                      # 166 unit tests, no device needed
./gradlew connectedDebugAndroidTest # 49 instrumented tests, needs a device or emulator
```

**Use `test`, not `testDebugUnitTest`.** The latter is an Android-only task, so it
silently skips `core:common` and `core:domain` — which between them hold 96 of the 164
unit tests.

Unit tests cover the ViewModels, use cases and money formatting. Instrumented tests
cover the things a JVM test cannot see: Room migrations against real SQLite
(`core:database`), the settings file surviving a round trip (`core:datastore`), and
composition-level defects such as touch-target size and behaviour at 200% font scale
(`core:ui`, `feature:dashboard`, `feature:upcoming`).

### Read the count, not the build result

A Gradle test task reports `BUILD SUCCESSFUL` when it executes nothing. That is not
hypothetical here: `core:ui`'s accessibility suite ran zero tests for months while
looking green, because only `:app` declared a `testInstrumentationRunner`. CI therefore
runs `.github/scripts/check_test_counts.py`, which fails when any module with test
sources produced no results. If you run a suite by hand, check the count the same way:

```bash
python3 .github/scripts/check_test_counts.py unit
python3 .github/scripts/check_test_counts.py instrumented
```

### CI

`.github/workflows/ci.yml` runs both suites on every pull request, and both are required
checks on `main`. The emulator job is skipped only when a pull request changes nothing
but documentation — never on the basis of which test files changed, since instrumented
tests exercise the main sources.
