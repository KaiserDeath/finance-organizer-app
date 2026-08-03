# Open items

What is actually still open, as of 2026-08-03. Everything else from the Propuesta C redesign and
the 2026-08-01 audit has shipped.

This file replaces `docs/design/TAREAS.md`, which was deleted because its six blocks are done and
a finished checklist with unticked boxes reads as a backlog. Anything decided rather than pending
belongs in [design-decisions.md](design-decisions.md), not here.

---

## 1. The pay sheet has never run on a real device

**Owner: not code.** No amount of test writing closes this one.

`PaySheetTest` covers all three routes — method with app, method without, "ya pagué por fuera",
including that it settles with the *selected* method — plus the 48 dp target. What it cannot cover
is the part that matters most: CI's emulator has neither Yape nor BCP installed, so
`launchPaymentApp()` takes the Play Store / web-banking fallback on **every** run. The criterion
the handoff wrote — *"pagar Netflix desde Próximos abre la app correcta y vuelve con el pago
registrado"* — has never been executed.

Also outside `PaySheetTest`'s scope: the 4 s undo, which lives in `UpcomingScreen`, not the sheet.

To close: install Yape, pay a real upcoming item from the nudge, confirm the right app opens, the
payment records on return, and undo works on all three routes.

## 2. The budget editor is the only amount field without the keypad

`AmountKeypad` (`core/ui/component/AmountKeypad.kt`) is what add/edit uses
(`AddEditScreen.kt:195`) and what onboarding uses. The budget editor still takes a plain text
field, and its category picker is a chip row rather than the prototype's icon grid.

This is not a fidelity gap so much as an internal inconsistency, and one we created ourselves: the
keypad shipped everywhere else *after* the budget editor was built. The same task — type an
amount — now has two different controls depending on which screen you are standing on.

## 3. Setting a default payment method requires entering edit mode

It is a `Switch` inside the add/edit sheet (`PaymentMethodsScreen.kt:537`), so changing your
default means opening an editor on a method you did not want to edit. The prototype makes it a
one-tap action with an affirmative resting state ("Ya es tu método predeterminado"), and also shows
per-method usage figures and the linked package id, none of which exist here.

Worth more than it looks: the default method is what the pay sheet suggests, so this feeds item 1.

## 4. `feature/transactions` has no tests

The only module with real UI logic reporting `NO-SOURCE`. Raised as §2.7 of the 2026-08-01 audit
and never closed. `TransactionsViewModel` owns the query, the type and category filters, and
delete-with-undo — all stateful, all uncovered.

## 5. The "Todas" chip mutates the collection it iterates

`TransactionsScreen.kt:173` does `state.filter.categoryIds.forEach(onToggleCategory)`. It works
only because the ViewModel emits a fresh state per toggle, leaving the iterated list stale — an
implementation detail nobody wrote down. A `clearCategories()` on the ViewModel makes it explicit.

---

## Not open — do not "fix" these toward the prototype

- **The two-number hero.** The prototype argues for it; the user sessions settled otherwise. See
  `HeroBalanceCard`'s KDoc.
- **The "Cifras del cierre" block on Análisis.** The prototype itself flagged it as a question to
  test. The sessions answered no.
- **The month-over-month delta**, anywhere ambient. Legitimate in Reporte mensual only.
- **The shortcut toast's "Abrir Yape"**, and the six-month window on the weekday chart — both
  decided, both recorded in [design-decisions.md](design-decisions.md).
