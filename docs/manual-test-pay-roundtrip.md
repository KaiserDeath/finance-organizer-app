# Manual test — the pay round trip

Closes the "Still open" item in `context.md`: no instrumented test can prove that tapping
**Pagar** actually opens Yape/BCP/Interbank/BBVA on a real phone and that the app correctly
settles the charge when the user comes back. This is a device-only check — run it after any
change to `launchPaymentApp`, `SettleUpcomingPaymentUseCase`, `PaySheet`, `DashboardScreen`,
`UpcomingScreen`, or `AddEditScreen`.

## What "pass" means

Returning from the external surface (bank app, browser, or Play Store) must **settle the charge
and show an undo snackbar**, and this must be true from every entry point the same way:

| Entry point | Where the sheet opens | What settles |
|---|---|---|
| Inicio → "Por pagar" card → row **Pagar** | `DashboardScreen` inline `PaySheet` | existing pending row, or a materialized projection |
| Próximos → row **Pagar** | `UpcomingScreen`'s `PaySheet` | existing pending row, or a materialized projection |
| Nuevo movimiento → **Pagar con X** (only shown for a pending charge with a linked app) | `AddEditScreen`, no sheet — launches directly | the row `save()` just wrote |

If any of the three produces a different outcome on return (silently stays pending, double
snackbar, no undo, crash), that's the regression this checklist exists to catch — it's exactly
what was inconsistent before 2026-08-09.

## Prerequisites

- A real device, not an emulator — Play Store and the bank apps' real sign-in flows are what's
  under test.
- Install at least: **Yape**, and one of **BCP** / **BBVA** / **Interbank**. Test the "not
  installed" path (below) with whichever of those three you *don't* install.
- A debug build installed fresh, or `./gradlew installDebug`.
- In Ajustes, set at least one payment method for each app under test, with the account/card
  linked so `paymentMethodId` resolves.
- Seed at least: one **overdue** pending expense, one **due-soon** (within 7 days) pending
  expense, and one **recurring template** with an occurrence due later this month (to cover the
  projected-occurrence path added 2026-08-09).

## Core scenarios

Run each from **all three entry points** in the table above unless marked entry-point-specific.

### 1. Real pending row, app installed — settles on return

1. Tap **Pagar** on a pending charge whose method has an installed app (e.g. Yape).
2. Tap **Abrir Yape y registrar**.
3. Confirm Yape opens (correct app, not a chooser, not a crash).
4. Press back / switch back to MoneyFlow (don't need to actually complete a Yape transfer).
5. **Expect:** a snackbar "Pagado con Yape" with **Deshacer**, and the row now shows PAID / no
   longer appears in Por pagar.
6. Tap **Deshacer**.
7. **Expect:** the row is back to PENDING with its original due date, and — check this
   specifically — its payment method reverts to whatever it was *before* this settle (not stuck
   on Yape if it wasn't Yape before).

### 2. "Ya pagué por fuera" — settles without leaving the app

1. Tap **Pagar** on a row with an app-having method.
2. Tap **Ya pagué por fuera** instead of the primary button.
3. **Expect:** settles immediately (no external app opens), snackbar with Deshacer, same undo
   behavior as scenario 1.

### 3. Projected recurring occurrence — materializes on pay

1. Find the seeded recurring occurrence (should now appear in Inicio's Por pagar card too, per
   the 2026-08-09 decision — confirm it's there, not just in Próximos).
2. Tap **Pagar**, complete the app round trip.
3. **Expect:** a new real transaction is created (check Movimientos — it wasn't there before),
   status PAID, correct amount/date/method.
4. Tap **Deshacer**.
5. **Expect:** the materialized row is *deleted* (not reverted to pending) — the occurrence goes
   back to being a projection only, still visible as upcoming.

### 4. AddEdit — "Pagar con X" (the flow this session's fix targeted)

1. Nuevo movimiento → fill amount/title → set "¿Ya pagaste esto?" to **Aún no** → pick a due date
   → pick a payment method with an installed app.
2. Confirm the **Pagar con `<method>`** button appears with its helper line.
3. Tap it.
4. **Expect:** the screen does **not** navigate away yet — it stays on AddEdit while the bank app
   launches. (This is the behavior that changed: previously it saved, launched, and left
   immediately, leaving the charge pending forever.)
5. Return to MoneyFlow (back / app switcher).
6. **Expect:** still on AddEdit briefly, snackbar "Pagado con `<method>`" with Deshacer appears,
   *then* the screen navigates back to wherever Nuevo movimiento was opened from.
7. Check Movimientos: the new charge is PAID, not pending.
8. Repeat and tap **Deshacer** on the snackbar this time.
9. **Expect:** the charge reverts to PENDING (same status it was saved with), and navigation still
   completes normally afterward.

### 5. Batch settle — must not offer the launch-and-return path

1. Get 2+ overdue payments with the same suggested method, open Próximos, tap **Pagar todos**.
2. **Expect:** no "abrir la app" button anywhere in this sheet — only **Registrar N pagos**,
   settling immediately. (Deliberate: one app launch can't prove N bills were paid.)
3. Confirm one **Deshacer** reverts all of them, not just the last.

## Bank-specific matrix

Run scenario 1 once per row. Package ids as verified 2026-08-05, recorded in `context.md`.

| Method | Package | Has web-banking fallback? | Expected when installed | Expected when **not** installed |
|---|---|---|---|---|
| Yape | `com.bcp.innovacxion.yapeapp` | No | Opens Yape directly | Falls straight to Play Store (see risk below — **no web page in between**) |
| BCP | `com.bcp.bank.bcp` | Yes (`bcpzonasegura.viabcp.com`) | Opens BCP app | Opens BCP's web banking in the external browser |
| BBVA | `com.bbva.nxt_peru` | Yes (`bbva.pe`) | Opens BBVA app | Opens BBVA's web banking in the external browser |
| Interbank | `pe.com.interbank.mobilebanking` | Yes (`bancaporinternet.interbank.pe`) | Opens Interbank app | Opens Interbank's web banking in the external browser |
| Plin | none (no deep link) | — | N/A — Plin never shows "Abrir…"; it always settles directly via "Registrar el pago" | N/A |

### ⚠ Known risk to specifically verify: false-positive settle on the Play Store fallback

`launchPaymentApp` returns `true` — which arms the settle-on-return handler — for **every**
fallback it opens, including the bare Play Store listing/search when a method has no
web-banking URL (this is Yape's case, and any bank if it somehow has neither the app nor a
banking URL configured). That means: if the tester **doesn't have Yape installed**, taps Pagar,
gets sent to the Play Store, and returns **without installing or paying anything**, the app will
still mark the charge as settled.

Specifically test this:

1. Uninstall Yape (or use a device that never had it).
2. Tap Pagar on a Yape-linked charge, confirm the Play Store listing for Yape opens.
3. Immediately press back without installing.
4. **Check:** does the charge still get marked PAID with an undo snackbar?

If yes, that confirms the risk above is real in practice, not just in the code — worth flagging
back rather than treating as a pass, since it means "returned from Play Store" and "returned
from actually paying" are indistinguishable today. Not a regression from this session's work
(the fallback chain in `AppLauncher.kt` predates it), but the session's fix made the settle side
of this more aggressive (three entry points now auto-settle instead of one), so it's worth
knowing whether this makes the false positive easier to hit in practice.

## Other things to eyeball while you're in there

- **Discreet mode on:** open the pay sheet with amounts hidden. Amount should still show masked
  consistently; paying/undoing should not leak the real figure anywhere (snackbar text, etc.).
- **Dark theme:** confirm the sheet's scrim (tokenized this session as `sheetScrimColor`) still
  reads correctly behind the sheet in both themes.
- **TalkBack:** turn it on, navigate to a Pagar button on the Inicio nudge card. It should
  announce "Pagar `<payee>`, `<amount>`" — not just "Pagar" — for each row, so multiple rows are
  distinguishable by ear.
- **Overdue text:** on the Inicio card, confirm the overdue count now shows as text ("N
  vencidos"), not only as a color change on the icon.
- **Truncation:** seed 4+ pending payments; confirm the card shows 3 rows plus a "y N más" link,
  and that it routes to Próximos.

## Sign-off

**Environment note:** this pass ran on the project's **Pixel 7 emulator** (`emulator-5554`),
against a freshly built debug APK containing the `f463439` recurring-duplicate fix, using the
repo's own `tmp/offline-stubs/` packages (`com.bcp.innovacxion.yapeapp`, `com.bcp.bank.bcp`,
`com.bbva.nxt_peru`, `pe.com.interbank.mobilebanking` — each a local "offline test stub" activity,
not the real bank app) as the installed targets for `launchPaymentApp`. That satisfies the intent
of the checklist (a real launch-and-return round trip through Android's activity stack, not a
mock) without touching any real bank session — driven via `adb`, screenshots read directly, and
`uiautomator` dumps for exact tap targets. It is **not** the real-device, real-app pass the
checklist's prerequisites ask for; that still stands as open. Existing "Hoy"-dated seed data on
this emulator (two duplicate `netflix` charges) served as live evidence of the bug the `f463439`
commit fixed — see notes below.

| Scenario | Inicio | Próximos | AddEdit | Notes |
|---|---|---|---|---|
| 1. Real row, app installed | not run | **PASS** | n/a | Paid the projected `netflix` (Sept 14, S/25) via Próximos → BBVA stub → back. Snackbar "Pagado con BBVA" + Deshacer shown; row disappeared from Próximos (total S/105→S/80). |
| 2. "Ya pagué por fuera" | not run | not run | n/a | PaySheet showed the button with correct copy when a no-app method (American Express) was selected ("Registrar el pago" / explanatory line); the actual tap-through wasn't exercised this pass. |
| 3. Projected occurrence | not run | **PASS** | n/a | Same `netflix` payment as #1 doubles as this scenario — materialized a real PAID transaction (confirmed in Movimientos, exactly one new row). Then **force-stopped and cold-relaunched the app** (re-triggers `RecurringGenerationWorker`'s catch-up) and re-checked Próximos: no duplicate reappeared, total stayed S/80. This is the direct regression check for the bug fixed in `f463439`. Did not test the Deshacer-deletes-the-materialized-row path this pass. |
| 4. AddEdit "Pagar con X" | n/a | n/a | **PASS** | Created a new S/99 pending "Prueba" expense with BBVA. Confirmed: screen stayed on AddEdit through the BBVA-stub launch-and-return (did not navigate immediately); "Pagado con BBVA" + Deshacer snackbar appeared on AddEdit itself; screen then navigated back to the origin (Movimientos) only after the snackbar's window; Movimientos showed the charge as PAID, not pending. Did not test the Deshacer branch of this scenario. |
| 5. Batch settle | n/a | not run | n/a | No 2+ overdue same-method payments were set up this pass. |
| Yape (installed) | not run | **PASS** | not run | Second live pass, after re-pushing to `origin/main` and cold-relaunching the app fresh. `mCurrentFocus` confirmed `com.bcp.innovacxion.yapeapp/offline.stub.StubActivity` opened from the `luz` (Sept 11, S/80) row in Próximos. Returned → "Pagado con Yape" + Deshacer snackbar shown → Próximos correctly emptied to its "Nada por pagar" state. Deshacer itself not exercised (snackbar window elapsed before it was tapped). |
| BCP / Interbank (installed) | not run | not run | not run | Packages confirmed present; launch not individually exercised. |
| BBVA (installed) | n/a | **PASS** | **PASS** | `mCurrentFocus` confirmed `com.bbva.nxt_peru/offline.stub.StubActivity` opened on both the Próximos and AddEdit launches — correct package targeted, real "offline test stub" screen shown, zero real-bank exposure. |
| Play Store false-positive check | not testable this pass | — | — | All four bank stub packages happen to be installed on this emulator, so there was no "app not installed" bank method available to force the fallback path. Still open — needs a method with no matching installed package. |
| Discreet mode | **PASS** | — | — | Toggled the eye icon on Inicio: hero figure, "gastado" line, "por día" line, shortcut amounts, and recent-movement amounts all masked to `•••••` consistently; toggled back off cleanly. |
| Dark theme (general) | **PASS** | — | — | `cmd uimode night yes`: background, text, and nav bar adapted; the indigo hero band correctly stayed on-brand rather than shifting to a Material You neutral. |
| Dark theme scrim (behind an open sheet) | not tested | — | — | Dark mode was toggled after the pay sheet was already closed; didn't reopen a sheet to check `sheetScrimColor` specifically against the dark background. |
| TalkBack row descriptions | not tested | not tested | — | No Inicio nudge card was on screen at any point during this pass (nothing pending remained after scenario 1/3), so there was no "Pagar `<payee>`, `<amount>`" row to inspect via `uiautomator` or TalkBack. |
| Truncation "y N más" | not tested | — | — | Didn't seed 4+ pending payments this pass. |

**Bonus finding, not in the original checklist:** the emulator's pre-existing seed data included
two identical `netflix` PAID transactions both dated "Hoy" — live, on-device evidence that the bug
`f463439` fixed had already produced a real duplicate charge before the fix was installed. The fix
does not retroactively clean up existing duplicates (confirmed: those two rows are still there);
it only prevents new ones.

Device: Pixel 7 emulator (`emulator-5554`)  Android version: (AVD default)  Date: 2026-08-10
Tester: Claude (via adb), pending a real-device pass by a human tester for the items above marked
"not run" / "not tested" / "not testable this pass".
