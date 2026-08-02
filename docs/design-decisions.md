# Design decisions

Standing rules that outlive any one audit. Each entry says what was decided and why, so a later
reader can tell a deliberate divergence from drift.

## Month-over-month comparisons — scope, not ban

Month-over-month comparisons are banned from ambient surfaces (Inicio, Análisis) and permitted in
explicitly requested reports (Reporte mensual, PDF/CSV export).

Phase 1 of the 2026-08-01 audit was written as "no month-over-month comparison survives anywhere in
the app", and read against the code that criterion looks half-applied: `DeltaChip`,
`CategoryDeltaRow` and the exported PDF still compare.

They stay. What testing rejected was a comparison occupying prime space on a screen the user did not
ask for it on. A retrospective report is the one place a comparison is the entire point, and it now
sits behind a deliberate tap.

## Navigation diverges from the prototype's eight screens, deliberately

Two structural changes landed that no phase asked for. Both were argued in code comments and
nowhere else, which is what the 2026-08-02 audit flagged: a divergence you chose is a decision, and
the same divergence undocumented is indistinguishable from drift. Recorded here as decisions.

> **Not verified against the prototype.** The audit asked for these to be confirmed against the
> prototype's navigation before recording. The Propuesta C spec and prototype still live outside
> version control (audit item D9), so that comparison could not be made. What follows documents
> what the code does and why. If the prototype disagrees, this section is the thing to revisit —
> it is not evidence that the prototype was consulted.

### Bottom nav carries four destinations, not five

`TopLevelDestination` is Inicio, Movimientos, Análisis, Tu dinero. Presupuestos was demoted to a
stacked destination.

The argument that promoted Presupuestos to a tab was that its headline number should be reachable
without entering the screen. That number now renders on the "Tu dinero" row directly, so the tab
was paying for something already delivered — and four tabs keep the bar legible on a 5-inch screen
where five crowd it.

Demotion is not a loss of reach. `BudgetsRoute` is entered from four places: the Tu dinero row,
Inicio's "Ver todo" on the budgets card, Inicio's hero budget bar (audit item U2), and Análisis's
primary action on both the overrun and no-overrun cards (U3). It gained traffic on the same change
that removed its tab, which reads as a contradiction and is not one — a tab is a permanent claim on
scarce space, while those four entries are offered where the number that leads to them is already
on screen.

### Ajustes sits two levels below Tu dinero

Tu dinero → Ajustes. The audit recorded this as three levels — Tu dinero → Ajustes → Apariencia —
and that third level is gone: U1 deleted the Apariencia destination and inlined its one segmented
control into the Ajustes list, so changing the theme is one tap rather than three.

The remaining rows under Ajustes (Categorías, Moneda, Atajos de un toque, Pagos recurrentes, Copia
de seguridad, Seguridad, Acerca de) do open their own screens, so those leaves are three levels
deep. That is deliberate and different in kind: each is a screen with its own content and its own
work to do, not a wrapper around a single control. The rule the U1 deletion establishes is the one
worth keeping — **a destination has to hold more than one control to deserve being a destination.**
