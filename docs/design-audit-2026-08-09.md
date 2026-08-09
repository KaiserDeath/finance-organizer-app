# Design audit — 2026-08-09 (uncommitted "pay from Inicio" work)

Scope: the working-tree diff on top of `54d379c` — the dashboard "Por pagar" card with inline
PaySheet, the AddEdit "Pagar con X" action, the UpcomingRoute arguments, and the sheet scrim change.
Audited against `context.md`'s settled decisions and the 2026-08-04 audit trajectory.

Direction verdict: **right feature, two blockers before commit.** Surfacing pending charges above
recent movements with an in-place pay action is the "clear next action" the previous audit asked
for. The save-before-launch pattern in AddEdit is correct. Nothing below removes functionality.

## P0 — fix before shipping

1. **Inconsistent round-trip semantics.** Dashboard and Próximos auto-settle (with undo) when the
   user returns from the bank app; the new AddEdit "Pagar con X" flow leaves the expense *pendiente*
   after the identical gesture, with no prompt. Same action, opposite outcomes. Align: either apply
   the `awaitingReturn` confirm-with-undo after `onDone()`, or state in the AddEdit helper copy that
   the charge stays pending.
2. **`feature:dashboard → feature:upcoming` violates the module rule** (`:app → :feature:* →
   :core:*`). `PaySheet`/`UpcomingViewModel` were made public to allow it, and DashboardScreen now
   hosts a second ViewModel plus a duplicated copy of the resume/settle/undo machinery from
   UpcomingScreen. Move the sheet to `core:ui` or hoist the wiring to `:app`; extract the shared
   resume-settle logic once.

## P1 — UX regressions in the card

3. **Urgency information lost.** The title flattened to "Por pagar"; overdue state is now carried
   only by accent color (fails the app's own color-alone bar) and the overdue *count* is gone.
   Restore a text line ("2 vencidos") when `overdueCount > 0`.
4. **`take(3)` with no "+N más".** The total sums all pending charges but at most three rows render;
   the numbers visibly disagree. Add "y N más" routing to Próximos.
5. **Disclaimer copy too heavy.** "Esto no está sumado arriba. Es lo que debes, no lo que gastaste."
   → one calm line: "Pendiente de pago — no incluido en el gasto del mes." Also verify the new
   Text/row list (outside the clickable header Row, no padding of their own, broken indentation in
   the forEach block) actually align inside MoneyCard — this looks visually unverified.
6. **TalkBack ambiguity.** Each row's bare "Pagar" TextButton announces identically. Add a content
   description with payee and amount ("Pagar Netflix, S/ 44.90"), matching the Phase 4 semantics
   work.

## P2 — consistency

7. **Dead route surface.** `UpcomingRoute(paymentId, recurringId, dueDate)` and the auto-open logic
   in UpcomingScreen have no caller — the dashboard opens PaySheet inline and `MoneyFlowApp` still
   navigates `UpcomingRoute()` bare. Wire the card's row tap to route with the id, or drop the
   parameters.
8. **`scrim.copy(alpha = 0.52f)` is a local magic value** in both sheets. Per the settled rule, a
   component choosing its own color means a theme role is missing — tokenize it once.
9. **The projected-occurrence reversal needs a decision entry and tests.** The old nudge deliberately
   excluded projected occurrences; the new one includes current-month projections. Defensible
   (Inicio now agrees with Próximos) but it reverses a written decision — add a line to
   `docs/design-decisions.md`, and cover the new filter (projected-in-month inclusion, dueSoon vs
   pending split) in `DashboardViewModelTest`; the only new test checks `isFirstRun`.

## Keep as-is

Save-before-launch in AddEdit; suggested category promoted to first chip; `isFirstRun` no longer
treats a pending-only ledger as an empty app (tested); undo on every settle; 48 dp pay button.
