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
