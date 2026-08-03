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

## Discreet mode — persisted, everywhere, one tap

Inicio opens with a 32sp figure on a saturated band: the most readable-over-a-shoulder element in
the product, on a phone that gets handed around. The eye toggle in the band's own corner masks
every amount. Three questions were open before it was built; these are the answers it implements.

**It persists.** Stored in `SettingsRepository` as `amountsHidden`, read at the theme so it is
restored before the first frame. A mask that reset each session would be off exactly when the phone
is picked up cold, which is when it is most likely to be handed over.

**It covers everything, including outside the app.** The flag lives on `LocalAmountsHidden`,
provided by `MoneyFlowTheme`, and `money()` in `core/ui/util/MoneyDisplay.kt` is what screens call
instead of `Money.format`. Reading it from the composition means a screen has to opt *out*
deliberately — the opposite default from a boolean threaded screen by screen, where the one place
you forget is the place that defeats the feature. The home-screen widget cannot see the composition,
so it reads the flag directly; leaving it out would have been the worst hole of the lot.

**One tap, no PIN.** The lock screen's PIN and biometrics are untouched and unrelated. Asking for a
credential to reveal a number the owner already knows is friction dressed as security, and it would
make the toggle too slow to use in the moment it is for.

### What stays visible, and why

Percentages, day counts and the streak survive the mask. They do not identify what you have, and
they are what keeps the screen useful while hidden — a fully blanked Inicio would just be turned
off. Where a figure was the whole content of a line, the line becomes qualitative rather than a row
of dots: "Quedan S/ 819.50" reads "Vas al ritmo justo", and the budget denominator becomes "de tu
presupuesto del mes".

The mask is fixed-width (`S/ ••••••`) on purpose. A mask that grew with the number would still tell
a reader across the table whether they are looking at hundreds or tens of thousands.

### Deliberately not masked

- **PDF and CSV exports.** A masked export is a broken file, and an export is explicitly requested —
  the same ambient-versus-requested line the month-over-month rule draws above.
- **The add/edit amount field and the shortcuts picker.** You are entering or choosing those values;
  masking them makes the control unusable rather than private.
- **Onboarding.** It runs before the setting can have been turned on.

### Insight messages carry amounts as data

`Insight.message` is a `List<MessagePart>` — literal prose interleaved with
`MessagePart.Amount(amountMinor, currencyCode)` — rather than a finished sentence. `InsightEngine`
does not call `Money.format` at all; `core:ui`'s `insightMessage(...)` formats each amount through
`money()`, so the mask applies like anywhere else.

This replaced a regular expression that matched formatted amounts back out of the rendered text.
That worked, but it tied the mask to the exact output of `Money.format`: changing the separator or
the symbol spacing would have stopped the mask matching, leaking the figures it exists to hide,
with nothing failing to say so.

`plainMessage` renders with amounts always visible, for tests and exports — the same
ambient-versus-requested line drawn above. UI code should not use it.

`InsightEngineTest` pins the rule directly: every insight the engine can emit is checked for a
currency-shaped string in its text parts, so inlining an amount fails the build rather than
quietly reopening the hole.

## Navigation: what follows the spec, and the one thing that does not

The 2026-08-02 audit listed two navigation changes as "undocumented structural changes to confirm",
describing both as moving away from the prototype. Checked against the spec — now committed at
`docs/design/` — that is right about one and wrong about the other.

### Four bottom-nav destinations — this *is* the spec, not a divergence

`TopLevelDestination` is Inicio, Movimientos, Análisis, Tu dinero, with Presupuestos as a stacked
destination. Spec §2 ("Navegación — de doce destinos a seis") specifies exactly this: *"La barra
inferior pasa de cinco pestañas a cuatro"*, *"quitar BUDGETS del enum TopLevelDestination"*, and
*"BudgetsRoute deja de ser top-level y pasa a destino apilado con onBack obligatorio"*.

It also gives the same reason the code comment gives, independently: Presupuestos comes off the bar
because its figure is now visible without entering, which was the argument that put it there, and
because four tabs stay legible where five crowd.

So there is nothing to confirm and nothing to justify — the implementation followed the handoff.
Recorded here only because the audit raised it, and a reader who finds that entry should not go
looking for a decision that was never made.

Demotion did not cost reach. `BudgetsRoute` is entered from four places: the Tu dinero row, Inicio's
"Ver todo", Inicio's hero budget bar (audit item U2) and Análisis's cards (U3).

### Deleting the Apariencia destination — this *is* the divergence

Spec §7 puts Apariencia in the Ajustes list as one of seven configuration screens: *"La app
(Categorías, Moneda, Apariencia) y Tus datos (Recurrentes, Copia de seguridad, Seguridad, Legal)"*.
That is a destination, and it makes Tu dinero → Ajustes → Apariencia three levels — as specified.

Audit item U1 deleted it. The screen existed to host one `SingleChoiceSegmentedButtonRow` of three
options behind an app bar, a back arrow and a navigation transition; the control now sits in the
Ajustes list as its own card, and changing the theme costs one tap instead of three.

**This is the change that diverges from the spec, and it is deliberate.** The spec's own rule is
that a decision whose premise breaks is a different decision, to be reopened rather than
half-implemented. The premise here was that Apariencia is a configuration *screen* like the other
six. It is not: the other six each hold their own content and their own work, while this one held a
single control. The rule worth carrying forward — **a destination has to hold more than one control
to deserve being a destination** — is what the other six still satisfy and what this one never did.

The remaining Ajustes rows (Categorías, Moneda, Atajos de un toque, Pagos recurrentes, Copia de
seguridad, Seguridad, Acerca de) stay as their own screens, three levels below Tu dinero, per spec.
Atajos de un toque is new (audit item U5) and follows the same rule: it is a picker with real
content, not a wrapper.
