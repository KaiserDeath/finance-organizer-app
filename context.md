# Context

Orientation for someone picking this repo up cold — human or agent. The README says what the app is
and how to build it; this file says what you need to know *before* changing it: what is already
settled, what has bitten people here, and what is genuinely still open.

Current as of **2026-08-04**, at `db806b8` plus the UX audit and Phase 1 UI polish recorded below.

---

## The app in one paragraph

MoneyFlow Peru is an offline-first personal-finance organizer for the Peru market: Kotlin, Jetpack
Compose, Material 3, Room, Hilt, multi-module clean architecture. Product phases 0–6 are implemented
and the *Propuesta C* redesign is applied — all six of its blocks have landed, and the five phases of
`docs/audit-2026-08-01.md` are closed. The UI copy is Spanish; the code and docs are English.

## Where things are

```
:app                  shell (NavigationSuiteScaffold), onboarding, app lock, widget,
                      Tu dinero (app/money/), Ajustes (app/settings/), backup, legal
core/                 model · common · domain · database · datastore · data ·
                      designsystem · ui · testing
feature/              dashboard · transactions · addedit · categories · paymentmethods ·
                      budgets · upcoming · recurring · analytics · accounts · savings · currency
build-logic/          convention plugins — the only place build config is written
gradle/libs.versions.toml   every version, no exceptions
.github/              ci.yml (two jobs) and scripts/check_test_counts.py
docs/                 the audit, the standing decisions, the design handoff as delivered
```

Dependencies flow one way: `:app` → `:feature:*` → `:core:*`, and within core
`:core:data` → `:core:domain` → `:core:model`.

## Build and test

```bash
./gradlew test                      # 171 unit tests, no device
./gradlew connectedDebugAndroidTest # 51 instrumented tests, needs a device
```

The wrapper (Gradle 8.13) is committed. Point `JAVA_HOME` at a JDK 17–21 — the JBR bundled with
Android Studio is the easiest one to reach for. A newer system JDK (25) will not work with this
Gradle/AGP pair.

## Recent UX audit and implementation

On 2026-08-04 the current debug build was audited on a Pixel 7 emulator using fresh screenshots.
The combined UX/accessibility review scored the product **7.1/10**: navigation and expense entry
are strong; Home has too many competing numbers; Analytics needs a clear next action; and Tu dinero
needed better value alignment, copy, and row affordances.

Evidence and the full report live in:

- `docs/audit-2026-08-04-evidence/` — six accepted emulator screenshots.
- `docs/design-audit-2026-08-04.md` — findings, accessibility limits, and the four-phase plan.
- `docs/moneyflow-ux-audit-board/index.html` — standalone interactive audit board.
- `output/pdf/moneyflow-ux-audit-2026-08-04.pdf` — verified PDF report.

Phase 1 clarity and trust changes are implemented:

- `feature/dashboard/HeroBalanceCard.kt` now labels the signed monthly figure **“Ingresos menos
  gastos”** instead of the ambiguous “Balance”. The validated budget denominator and projection
  remain unchanged.
- `app/money/MoneyScreen.kt` now uses exact Spanish singular/plural copy, shows numeric budget
  status (`0 en riesgo` rather than `Todo en orden`), right-aligns trailing values in a stable
  width, and gives every destination row a consistent chevron.

Verification completed after these changes:

- `:app:compileDebugKotlin` — passed.
- `:feature:dashboard:test` — passed.
- The complete unit suite passes with all 171 expected tests, and
  `compileDebugAndroidTestKotlin` passes. Run Gradle with JDK 17–21; JDK 25 fails before project
  compilation. On this workstation, `JAVA_HOME` points to Android Studio's bundled JDK 21.

Phase 2 is now partly implemented: the expense form keeps payment/date context in
`QuickEntrySummary`, Movimientos exposes a labeled `Filtros` action and low-data guidance, and a
description-matched category is moved to the first chip position in
`feature/addedit/AddEditScreen.kt`. This keeps the suggested choice immediately scannable without
inventing frequency data that the current state does not provide.

Remaining Phase 2 work: add true recent-frequency category data, preserve a sticky save/context strip
while the IME is open, and finish empty/low-data copy review across all destinations. The remaining
validation also requires a real install with a supported Peru bank app: confirm the launch-and-return
payment round trip and the package ids listed below.

---

## Settled — don't reopen these without a reason

Each of these was decided against a real alternative. The reasoning is in
[docs/design-decisions.md](docs/design-decisions.md); the summary is here so you can recognise one
before you "fix" it.

| Decision | The short version |
|---|---|
| **No Material You** | There is no `dynamicColor` path. A wallpaper-derived scheme silently replaced the brand identity the redesign exists to build. Semantic colour lives in theme roles (`brandSurface`, `moneyColors`, `NoticeColors`) — when a component starts choosing a colour, add a role rather than a local `when`. |
| **Discreet mode** | Persisted in settings, read at the theme via `LocalAmountsHidden`, applied by `money()` in `core:ui`. Hiding is the default and opting out is deliberate. Exports, the amount field, the shortcuts picker and onboarding are deliberately not masked. |
| **Insight amounts are data** | `Insight.message` is a `List<MessagePart>`, not a finished sentence; `InsightEngine` never calls `Money.format`. Don't inline an amount into prose — `InsightEngineTest` fails the build if you do. |
| **The hero keeps its two references** | Denominator and projection stay. The month-over-month delta, the prototype's "Cifras del cierre" block, and the normal-state cash-flow line were removed after review. Their absence is a result; the prototype predates it. |
| **Month-over-month = ambient vs requested** | Banned on Inicio and Análisis; kept in the monthly report and the exports, where the comparison is the point of a screen you asked for. |
| **Four bottom-nav tabs** | Inicio · Movimientos · Análisis · Tu dinero. Presupuestos is a stacked destination reached from four places. This is the spec, not drift. |
| **Apariencia was deleted** | The one deliberate divergence from the spec. The rule it establishes: a destination has to hold more than one control to deserve being a destination. |
| **Recurring is presentation-only** | The prototype draws no Próximos screen and no recurring-templates screen, and says so. `GetUpcomingPaymentsUseCase` projects occurrences in memory and never persists them — deliberately. Don't let a UI change reach into how recurrences are stored or browsed. |

## Traps that have actually cost time here

Not hypotheticals. Each of these shipped or nearly shipped.

- **A Room version bump wipes the seed data.** `DatabaseModule` still carries
  `fallbackToDestructiveMigration()` behind the real migrations, so a missing or throwing migration
  drops the ledger and recreates it empty — and the app comes up looking merely *new* rather than
  broken. Seeding runs from `onOpen` (`SeedData.seedIfEmpty`) precisely so that path reseeds. If you
  bump the version, write the migration *and* add it to `ALL_MIGRATIONS`, and extend
  `MigrationTest.kt` (instrumented — a unit test never opens a database).
- **`BUILD SUCCESSFUL` does not mean tests ran.** A Gradle test task reports success when it executes
  nothing. `core:ui`'s accessibility suite ran zero tests for months while looking green, because
  only `:app` declared a runner. Check the count:
  `python3 .github/scripts/check_test_counts.py unit` (or `instrumented`). CI runs this and fails on
  a module that has test sources and no results.
- **Use `test`, not `testDebugUnitTest`.** The latter is Android-only, so it silently skips
  `core:common` and `core:domain` — 97 of the 171 unit tests.
- **`compileDebugKotlin` does not build `androidTest`.** Add a parameter to a composable and the main
  sources stay green while every UI test constructing it fails to compile — discovered at the next
  device run, which is the slow job. That happened twice on 2026-08-02; CI now compiles instrumented
  sources in the fast job.
- **Espresso is pinned to 3.7.0 on purpose.** It resolves transitively to 3.5.0 (2022), which calls
  a removed `InputManager` API, and every Compose UI test dies on the first `onIdle`.
- **Vico is declared but not wired.** Charts are drawn in Compose (`DonutChart`, `BarChart`,
  `MoneyProgressBar`). The version entry in `libs.versions.toml` is not evidence of a dependency.
- **The Peru bank package ids were verified on 2026-08-05.** Yape, BCP, Interbank, and BBVA now
  match their Google Play listings. Migration 8→9 updates the obsolete BCP/Interbank ids on existing
  installs while leaving user-created methods alone. `launchPaymentApp` still falls back to a Play
  Store search when an app is unavailable. Plin has no standalone app and so has no deep link.

## Testing conventions

`core:testing` holds `MainDispatcherRule` and the shared repository fakes. There is **no mocking
library and no `kotlin-reflect`** — a test that needs a behaviour writes it into a fake, and the fakes
are stateful with live flows. That is not a style preference: `FakeBudgetRepository` was a no-op stub
whose `upsert` did nothing, so the undo test it backed passed while observing nothing.

Instrumented tests exist for what a JVM test cannot see — real SQLite, a real settings file, touch
targets, font scale, discreet mode. Several were verified by *mutation* rather than by a green run
(revert one row to `Money.format`, confirm exactly one test fails). If you add a test to guard a rule,
break the rule once and check it actually fails.

`feature:transactions` now has focused ViewModel coverage for category-route prefiltering, query and
type filters, section totals/order, and delete/undo persistence. The ViewModel reads its single
optional `categoryId` argument directly from `SavedStateHandle`, so the Analytics-to-Movimientos
handoff is covered by the JVM suite without depending on Navigation's Android `Bundle` decoder.
Its search/filter header now uses a wrapping `FlowRow`, allowing the field and filter action to move
onto separate lines instead of clipping at narrow widths or large font scales.

On 2026-08-05, `./gradlew test` passed with 171 unit tests, the count checker reported 51
instrumented tests, and `./gradlew compileDebugAndroidTestKotlin` passed. No emulator was attached to
this session, so a fresh `connectedDebugAndroidTest` run remains a device check rather than a claim
made from compilation alone.

## UI rules that apply everywhere

From the handoff, and enforced in review:

- No secondary text in opacity. Use `onSurfaceVariant`, never `.copy(alpha = …)` on `onSurface`.
- Everything destructive or automatic offers undo — 4 s, with an action.
- A disabled button must not look tappable.
- Minimum touch target 48 dp.
- No new navigation transitions. `NavTransitions.kt` defines the three that exist.

## Latest UX audit progress — 2026-08-04

Phase 1 of `docs/design-audit-2026-08-04.md` is implemented. The dashboard hero now has one primary
answer (monthly spend), keeps only the budget denominator and month-end projection as references, and
shows explanatory copy instead of another zero-valued figure on a first run. The normal-state
"Ingresos menos gastos" line is intentionally absent; it competed with the answer without changing
the next action.

`Tu dinero` rows now use stable right-aligned value columns, natural Spanish pluralization, useful
zero/one summaries, and consistent chevrons. The Movimientos filter and Análisis monthly-report
actions have visible labels in addition to their accessibility names. These changes are in
`HeroBalanceCard.kt`, `MoneyScreen.kt`, `TransactionsScreen.kt`, and `MoneyFlowApp.kt`.

The Phase 4 accessibility suite now also guards discreet mode at the shared `TransactionRow`
boundary: a row renders the fixed currency mask and never exposes the formatted amount when the
theme has amounts hidden. Analytics' donut chart semantics now include the visible
category labels, amounts, and shares in legend order, so TalkBack users can understand the breakdown
without visually decoding the chart.

The relevant Android modules compile and the repository-wide `./gradlew clean test` passes (171 unit
tests) when Gradle runs with the Android Studio JBR (JDK 17–21). An incremental run briefly surfaced a
stale Hilt class-file error; a clean build resolved it without a source change. Phase 2 is complete:
`CategorySuggester` results are now surfaced in
`AddEditScreen` as "Categoría sugerida", so an inferred chip is explained and remains easy to change.
`QuickEntrySummary` keeps the selected payment method and relative date above the keyboard and opens
the existing details section when tapped. Movimientos now labels its category chips as "Categorías
rápidas", names the sheet's type filter "Tipo de movimiento", and shows a low-data hint for one or
two unfiltered movements. The first Phase 3 analytics refinement is also in place: a one-category
dataset gets a direct concentration summary instead of a 100% donut; multi-category datasets keep
the donut and legend. Category-specific suggestions now carry their category as structured data and
open Movimientos with that category pre-filtered; other suggestions remain non-clickable instead of
implying an action they cannot complete. Together with the existing overrun actions and chart
semantics, this closes the Phase 3 implementation.

After these changes, the repository-wide `./gradlew test` passes (171 unit tests). The complete
instrumented suite also passes on the Android 17 Pixel 7 emulator (51 tests): Room migrations,
settings persistence, touch targets, 200% font/narrow layouts, discreet mode, dashboard behavior,
pay-sheet behavior, and undo/recovery paths. The emulator shows Android's expected older-target
warning because it runs API 37 while the project targets API 35; dismissing that system dialog is
required before the test APKs can start.

And the meta-rule the spec states about itself: **a decision whose premise breaks is a different
decision.** Stop and ask rather than half-implementing it. That is how the Apariencia divergence was
taken, and why it is written down instead of being drift.

## Still open

- **The pay round trip has never been run on a real install.** Ten instrumented tests cover the pay
  sheet, including that the primary action fires the launch intent and does not settle the payment
  behind the user's back. No test can prove Yape actually opens and the user comes back to a recorded
  payment. One manual pass on a device would close it.
- **The 2026-08-02 audit is not in the repo.** Its eighteen items (D1–D9, U1–U9) live in pull request
  descriptions, which is why `design-decisions.md` cites U-numbers you cannot look up in `docs/`.
- **Watch `DashboardViewModel`.** It combines six sources and derives nudge, insight, pace, budgets,
  shortcuts and streak. Still readable; the file most likely to stop being so.

## Reading the docs

| File | What it is | Is it maintained? |
|---|---|---|
| [README.md](README.md) | What the app is, how to build and test it, the module map | Yes |
| this file | Orientation, settled decisions, traps, open work | Yes |
| [docs/design-decisions.md](docs/design-decisions.md) | Standing decisions and the reasoning behind each | Yes — add an entry when you make one |
| [docs/audit-2026-08-01.md](docs/audit-2026-08-01.md) | The audit, its five-phase plan, and what each phase shipped | Closed; kept as a record |
| [docs/design/](docs/design/) | The Propuesta C handoff **as delivered**: spec, navigable prototype, `TAREAS.md` | **No** — a historical artefact |

`docs/design/` is deliberately frozen. Its `TAREAS.md` checkboxes are all unchecked and block 6 still
reads *BLOQUEADO* even though every block has landed and block 6 was resolved by the user sessions.
Editing it would destroy the record of what was specified versus what happened; read status from this
file instead. The prototype HTML is a self-contained offline bundle — open it in a browser rather
than reading the source, which is compressed.
