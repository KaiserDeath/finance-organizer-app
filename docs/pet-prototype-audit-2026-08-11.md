# MoneyFlow Castor Prototype — New-Conversation Handoff

**Updated:** 2026-08-12  
**Stage:** Technical/product prototype complete; production animation and release hardening remain  
**Default character:** Castor the beaver  
**Product direction:** [pet-companion-plan.md](pet-companion-plan.md)  
**Rive production contract:** [castor-rive-handoff.md](castor-rive-handoff.md)  
**Approved visual reference:** [pet-family-v3-calico-samoyed-weather.png](pet-companion-assets/pet-family-v3-calico-samoyed-weather.png)

## Start here

Do not repeat the earlier drag, bubble, lifecycle, settings, or event-bus work. Those milestones are
implemented and tested.

The next conversation should start with the production gap: obtain or create genuinely layered
Castor source artwork and build `castor.riv` according to `docs/castor-rive-handoff.md`. The current
single flattened PNG cannot produce articulated 2D animation. Do not add the cat, Samoyed, or
weather companion before Castor's production renderer is integrated and stable.

If layered art is not available, continue release hardening instead:

1. add full activity rotation/process restoration and the remaining device-form-factor coverage;
2. profile frame time, recomposition, memory, and background battery behavior.

## Current implementation

### Architecture

- Dedicated `:feature:pet` Compose/Hilt module.
- One `PetOverlayHost` lives above app navigation and survives ordinary destination changes.
- The overlay is absent on add/edit, movement detail, and security surfaces.
- The root lock gate chooses a testable destination before `MoneyFlowApp` is composed, so an enabled
  pet cannot enter the lock-screen composition; unlocking is session-only and recreation relocks.
- An app-owned buffered `PetEventBus` keeps finance features independent of the pet module.
- `TransactionSaved` is the first and only product event. It contains no amount, title, account,
  category, method, or other financial detail.

### State and lifecycle

Visual states include idle, blink, tap, dragged/held, settle, speak, dismiss, sleep, and wake.
`PetAnimationIntent` maps every visual state to a runtime-neutral renderer contract.

- Dragging, dismissal, sleep, and wake are interruptible.
- Drag release and message dismissal restart the inactivity timer.
- Backgrounding cancels transition and sleep work.
- Foregrounding wakes a sleeping pet and starts a fresh timer.
- Introduction completion is persisted, so ViewModel recreation does not repeat the greeting.
- `PetAnchor` was removed; normalized X/Y is the only position model.

### Placement and safe areas

- Dragging is free across ordinary content and clamps only to physical/system boundaries.
- Safe release preserves exact X/Y; there is no edge snapping.
- Position is stored as normalized X/Y and restores across window-size changes.
- The shared safe-area registry accepts measured window rectangles without making ordinary features
  depend on the pet module.
- Measured snackbar hosts publish and remove live bounds, and the visible IME acts as a system
  placement boundary so Castor stays above the keyboard.
- Dialogs and bottom sheets that coexist with Castor publish their measured surfaces, including
  account, budget, category, savings, recurring, filter, exchange-rate, and payment workflows.
- Inline pay/payment-launch/status actions, destructive category/savings controls, and analytics
  chart/legend regions publish stable, item-specific exclusions.
- Top-level navigation items and the primary add action publish live bounds, supporting phone bottom
  bars and adaptive navigation rails without a hardcoded height.
- Releases that overlap registered controls settle at the nearest safe candidate.
- Bubble alignment uses measured width and available space, so left/center/right placement stays
  inside the window.

### Accessibility

- Character target exceeds 48 dp and exposes state-aware TalkBack descriptions and click actions.
- Message, close action, and character have explicit traversal order.
- Close action remains visible at 200% font scale.
- Disabled/hidden Castor is absent from composition and semantics.
- Speech grows upward and no longer changes the stored character coordinate.

### Product controls

- New installs default to disabled and require explicit opt-in.
- Existing prototype installs with `pet_enabled=true` remain enabled.
- Dedicated “Compañero Castor” settings destination.
- Controls: enabled, reduced motion, Normal/Low/Silent message frequency, reset position, and replay
  tap/drag tutorial.
- Normal contextual cooldown: 1 minute. Low: 5 minutes. Silent: no speech.
- Speech uses stable message IDs with explicit discreet-mode variants; visible speech updates if
  discreet mode changes while the bubble is open.
- Gesture tutorial completes only after a tap followed by a completed drag and persists that state.
- Sound is intentionally not exposed until the production rig supplies real audio.

### Current renderer

- Temporary transparent raster asset:
  `feature/pet/src/main/res/drawable-nodpi/beaver_prototype.png`.
- Rendered at 104 dp.
- Scale/rotation only provide minimal tap, held, and settle feedback.
- Most behavioral states are not visually distinct with this flattened asset.
- Rive Android Compose state machines are selected for production, but the dependency is deliberately
  deferred until a valid `castor.riv` exists, avoiding unused native APK weight.

## Completed validation

Latest successful coverage:

- 20 `feature:pet` JVM unit tests;
- 7 Pixel 7 Compose instrumentation tests;
- 12 Pixel 7 DataStore instrumentation tests;
- `:app:assembleDebug`;
- `git diff --check`.

Covered behavior includes:

- reducer transitions and renderer-intent mapping;
- free placement, system clamping, measured exclusion collision, and normalized restoration;
- center and all four corner releases on Pixel 7;
- drag callbacks and completion;
- sleep restart after drag, background pause, and foreground wake;
- message dismissal, 200% font scale, semantics, and traversal indices;
- Normal/Low/Silent persistence and cooldown policy;
- privacy-safe transaction reaction;
- normal/discreet structured message selection and live discreet-mode changes;
- tutorial requiring tap followed by drag;
- opt-in defaults, introduction persistence, and reset-position persistence.
- overlay state survival across sibling content navigation and enable-state persistence through
  ViewModel recreation.
- contextual cooldown persistence through ViewModel/process recreation.
- visible animation/message identity restoration through `SavedStateHandle`; interrupted drags
  restore as settle and transient restored states resume their normal return-to-idle timers.
- repeated-dismissal policy: three manual closes within ten minutes suppress automatic contextual
  speech for thirty minutes without disabling tap or tutorial interaction.
- dark-theme rendered-surface validation and normalized placement across portrait/landscape-sized
  windows.

Pixel 7 evidence is under `output/pet-qa/`, especially:

- `drag-top-left.png`, `drag-top-right.png`, `drag-bottom-left.png`, `drag-bottom-right.png`;
- `drag-center-final.png`;
- `speech-close-fixed.png`;
- `bubble-placement-final.png`;
- `pet-semantics.xml`.

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Remaining work

### P0 — layered Castor rig and production renderer

Create the layered artwork and Rive state machine defined in `docs/castor-rive-handoff.md`.
Required distinct animations: idle, blink, tap, held, settle, speak, dismiss, sleep, and wake.
Reduced motion must remain understandable without bounce or particles.

After receiving `castor.riv`:

1. add the current Rive Android Compose dependency;
2. initialize its worker/runtime at app scope;
3. implement a renderer mapping `PetAnimationIntent` and reduced-motion state to Rive bindings;
4. pause playback in background and when system/app motion is disabled;
5. retain the PNG renderer as a temporary fallback for load/error states;
6. add transition, loading failure, and lifecycle tests;
7. measure APK size, memory, CPU, and frame time before accepting the runtime.

### P1 — privacy and broader safe areas

- Decision: keep the conservative full 104 dp collision square for version 1. A smaller optical hull
  would place visible tail/hoodie pixels over controls and needs production-rig silhouette data to be
  safe; revisit only after `castor.riv` can provide stable per-state optical bounds.

### P1 — behavior policy

- Only `TransactionSaved` is integrated; do not add more events until privacy and frequency policy is
  stable.
- Transient visual/message state restores after process death without persisting rendered copy;
  discreet-mode text is recomputed from the stable message ID.

### P1 — QA and performance

- Full activity-level rotation instrumentation remains. Window-size restoration, pet
  `SavedStateHandle` process restoration, and session relocking on ViewModel recreation are covered.
- Tablet, landscape, foldable, and navigation-rail visual QA.
- TalkBack manual pass.
- Frame-time, recomposition, memory, CPU, and battery profiling.

## Working-tree warning

All pet implementation, tests, documentation, and visual assets are still local uncommitted changes.
Preserve unrelated user work. Before relying on another computer, intentionally review, commit, and
push the complete change set. Do not use destructive Git cleanup commands.

## Ready-to-paste opening prompt

```text
Continue the MoneyFlow Castor companion from docs/pet-prototype-audit-2026-08-11.md,
docs/pet-companion-plan.md, and docs/castor-rive-handoff.md. The technical/product prototype is
complete; do not redo drag, bubble, lifecycle, safe-area registry, TransactionSaved, settings, or
gesture-onboarding work. Preserve all existing uncommitted changes.

Start with the P0 production renderer. If genuine layered Castor artwork and castor.riv are available,
inspect them, integrate the Rive Android Compose state-machine renderer behind PetAnimationIntent,
retain a PNG load/error fallback, and validate lifecycle, reduced motion, frame time, memory, and APK
size. If layered art is not available, do not fake a rig from the flattened PNG; instead continue the
P1 release-hardening work: discreet-mode structured messages, broader measured safe areas, and the
missing navigation/accessibility/rotation/performance tests. Do not add other pets yet.
```
