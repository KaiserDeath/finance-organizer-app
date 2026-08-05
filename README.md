# MoneyFlow Peru

A modern, offline-first personal‑finance organizer for the Peru market — track every expense,
know where money goes, what's due, and how much remains. Built with Kotlin, Jetpack Compose,
Material 3, and a clean multi‑module architecture designed to scale into a full product.

> **Status: Phases 0–6 implemented; the Propuesta C redesign is applied.** Expenses, budgets,
> upcoming payments and recurring templates, analytics, accounts and savings, multi-currency,
> app lock, onboarding, the home-screen widget and the rule-based insight engine all ship. The
> UI follows the Propuesta C redesign (`docs/design/`), whose six blocks are all landed, and the
> five phases of `docs/audit-2026-08-01.md` are closed out.
>
> What remains open is the on-device pay round trip (opening a real bank app and coming back with
> the payment recorded has never been run against a real install) and final real-device validation
> of the bank-app integration.

---

## Requirements

- **Android Studio** Ladybug (2024.2) or newer.
- **JDK 17–21** for the Gradle build. ⚠️ The build targets JDK 17 bytecode and Gradle 8.13 runs
  on JDK 17–21. A newer system JDK (e.g. 25) is **not** supported by this Gradle/AGP combo. In
  Android Studio this is automatic (it bundles a compatible JDK). From the command line, point
  Gradle at JDK 21, e.g. `JAVA_HOME=/path/to/jdk-21` or `-Dorg.gradle.java.home=...` — the JBR
  that ships with Android Studio works and is the easiest one to point at.
- **Android SDK** with API 35 installed (compileSdk 35, targetSdk 35, minSdk 26).

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
  :core:designsystem     theme, tokens, brandSurface/moneyColors/NoticeColors roles,
                         MoneyCard/StatTile/DonutChart/BarChart/EmptyState/…, LocalAmountsHidden
  :core:ui               AmountText, AmountKeypad, TransactionRow, PaymentStatusPill, InsightCard,
                         money() (the discreet-mode formatter), date format, bank-app launcher
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
- **One brand palette, no Material You.** There is no `dynamicColor` path: a wallpaper-derived
  scheme silently discarded the brand identity the redesign is about. Semantic colour lives in
  theme roles (`brandSurface`, `moneyColors`, `NoticeColors`), never in a component's local `when`.
- **Discreet mode is read from the composition.** Screens format money through `money()`
  (`core:ui/util/MoneyDisplay.kt`), which consults `LocalAmountsHidden`, so hiding amounts is the
  default and opting out is the deliberate act. The widget reads the flag directly.

Standing decisions — and the divergences from the spec that were taken on purpose — are recorded in
[docs/design-decisions.md](docs/design-decisions.md). Read that before "fixing" something that looks
inconsistent with the prototype.

### Peru bank package ids

Payment-method deep links (`core:database/SeedData.kt` and the `<queries>` in the app manifest)
match the Google Play listings verified on 2026-08-05 for Yape, BCP, Interbank, and BBVA. Migration
8→9 replaces the two obsolete BCP/Interbank ids on existing installs without touching user-created
methods. `launchPaymentApp` (`core:ui/util/AppLauncher.kt`) still falls back to a Play Store search
if an app is unavailable. "Plin" has no standalone app, so it has no deep link.

## Roadmap

> These phase numbers are the *product* roadmap. `docs/audit-2026-08-01.md` has its own,
> unrelated phase numbering for design-system work — the two collide on "Phase 4" and mean
> different things.

- **Phase 2** — Budgets, upcoming payments, recurring templates (WorkManager), reminders/notifications ✅.
- **Phase 3** — Analytics & monthly reports, CSV/PDF export ✅. Charts are drawn in Compose
  (`DonutChart`, `BarChart`, `MoneyProgressBar`); Vico is declared in `libs.versions.toml` but
  deliberately not wired, so nothing depends on it.
- **Phase 4** — Accounts, income, transfers, savings goals, net worth, multi-currency ✅.
- **Phase 5** — Biometric/PIN lock, backup/restore (JSON), onboarding, full search/filters,
  home-screen widget (Glance) ✅.
- **Phase 6** — Smart insights behind the `SmartInsights` interface (rule-based engine: cash-flow,
  spending spikes, top category, upcoming/overdue bills) ✅. LLM-backed variant can implement the
  same seam later.

## Testing

Two suites, both run by CI on every pull request.

```bash
./gradlew test                      # 171 unit tests, no device needed
./gradlew connectedDebugAndroidTest # 51 instrumented tests, needs a device or emulator
```

**Use `test`, not `testDebugUnitTest`.** The latter is an Android-only task, so it
silently skips `core:common` and `core:domain` — which between them hold 97 of the 171
unit tests.

Unit tests cover the use cases and domain models (`core:domain`, 90), money formatting
(`core:common`), backup serialization (`core:data`), the pure-Kotlin UI helpers
(`core:ui`) and the ViewModels, including focused filter/section/delete-and-undo coverage for
`TransactionsViewModel`. `core:testing` holds the shared
harness: `MainDispatcherRule` and the repository fakes, which are stateful, so a test
can observe a write rather than just watch it disappear.

Instrumented tests cover what a JVM test cannot see: Room migrations against real SQLite
(`core:database`), the settings file surviving a round trip (`core:datastore`), and
composition-level defects such as touch-target size, discreet mode and behaviour at 200%
font scale (`core:ui`, `feature:dashboard`, `feature:upcoming`).

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

The unit job also runs `compileDebugAndroidTestKotlin`. `compileDebugKotlin` does not build
`androidTest`, so a composable gaining a parameter leaves the main sources green while every
UI test that constructs it fails to compile — discovered only at the next device run, which
is the slow, skippable job. Compiling them in the fast job catches it in minutes.

## Documentation

| File | What it is |
|---|---|
| [context.md](context.md) | Orientation for someone (or some agent) picking this repo up cold: where things are, what is settled, what to check before changing anything. |
| [docs/design-decisions.md](docs/design-decisions.md) | Standing decisions that outlive any one audit, including the deliberate divergences from the spec. |
| [docs/audit-2026-08-01.md](docs/audit-2026-08-01.md) | The architecture and UX audit, its five-phase plan, and what each phase actually shipped. |
| [docs/design/](docs/design/) | The Propuesta C handoff **as delivered** — spec, navigable prototype, `TAREAS.md`. A historical artefact; it is not updated as work lands, so read implementation status from `context.md`, not from its checkboxes. |
