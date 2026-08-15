# Castor Rive Production Handoff

## Decision

Use the Rive Android Compose state-machine runtime for the production renderer. Keep the existing
Compose layer responsible for placement, gestures, speech, accessibility, privacy, and safe areas.
The Rive file owns only character deformation and animation transitions.

Why Rive:

- its Android Compose API directly supports state-machine playback;
- a single interactive state machine avoids stitching independent clips together;
- playback can pause when the app backgrounds or animations are disabled;
- the runtime can settle when no animation work remains, reducing idle processing.

Official references:

- https://rive.app/docs/runtimes/android/android
- https://rive.app/docs/runtimes/android/state-machines
- https://rive.app/docs/runtimes/android/data-binding

Do not add the runtime dependency until `castor.riv` is ready to integrate. This avoids shipping its
native binary cost while the flattened prototype PNG is still the only renderer asset.

The runtime-neutral Android boundary now supplies animation intent, clamped `lookX`/`lookY`,
reduced-motion state, speaking state, and lifecycle playback state to the renderer. The Compose PNG
fallback consumes the same input and stops its motion while backgrounded. A genuine `.riv` export
is still required before adding the Rive dependency; do not rename or package a raster/sprite asset
as `castor.riv`.

## Artwork source guide

- **Layered vector source (start here):** [`pet-companion-assets/castor-master.svg`](pet-companion-assets/castor-master.svg)
- Production construction sheet: [`pet-companion-assets/castor-rig-construction-v1.png`](pet-companion-assets/castor-rig-construction-v1.png)
- Animation key-pose sheet: [`pet-companion-assets/castor-animation-states-v1.png`](pet-companion-assets/castor-animation-states-v1.png)
- Approved family/style reference: [`pet-companion-assets/pet-family-v3-calico-samoyed-weather.png`](pet-companion-assets/pet-family-v3-calico-samoyed-weather.png)

The construction sheet supplies a neutral master pose, rotation views, complete limb shapes,
facial components, mouth poses, and segmented tail references for vector tracing. It is a raster
production guide, not an editable layered source and not a substitute for the required Rive file.
When tracing, keep every hidden overlap complete so deformation never reveals clipped artwork.

### Canonical model

The **neutral master pose on the construction sheet** is the single source of truth for
proportion, palette, and costume. The animation key-pose sheet and the family sheet drift from it
(head-to-body ratio, whisker count, tooth shape, sleeve treatment) and are **acting reference
only** — never proportion reference. Where the sheets disagree, the master pose wins.

Measured master-pose proportions, normalised to the 512 artboard:

| Feature | Value |
|---|---|
| Character height (tuft to sole) | 460 |
| Head | 240 wide x 181 tall — deliberately broader than tall |
| Body at widest | 286 |
| Eye | 40 x 47, centres 104 apart |
| Nose | 55 x 36 |
| Muzzle | 102 x 51 |
| Incisor block | 31 x 25 |
| Feet | centres at x 174 / 338, midpoint = artboard origin |

## Vector source and import

`castor-master.svg` is a hand-traced, fully separated vector build of the master pose. It is the
intended input to the Rive editor and already satisfies the hierarchy, naming, completeness, and
origin rules below.

1. Create a new Rive file with a 512 x 512 artboard named `Castor`.
2. Import `castor-master.svg`. Its viewBox is 512 x 512 and the character is positioned so the
   bottom-centre point between the feet lands at (256, 499) — set the artboard origin there.
3. Confirm each SVG group id survived import as a layer name. Ids match the hierarchy exactly,
   so no renaming should be needed.
4. Re-apply anything the importer drops: the three `clipTail*` clip paths (tail lattice) and the
   `opacity="0"` state on the two hidden mouth variants, which must become hidden Rive shapes.

Every part is drawn as a complete closed shape beneath its overlaps — full arms behind the torso,
full legs behind the hem, full eyeballs behind the lids, full tail root behind the body — so
deformation cannot reveal a cut edge.

### Deviations from the original hierarchy

These are deliberate corrections; keep them.

| Change | Reason |
|---|---|
| Added `whisker_left` / `whisker_right` | Whiskers baked into the face shear badly under head rotation and cannot carry follow-through. |
| `mouth` is a swap-group (`mouth_closed`, `mouth_open_smile`, `mouth_speak`) | The construction sheet supplies several mouth poses; one deforming shape cannot cover them. |
| `mouth` ordered **before** the incisors | A beaver's incisors must read in front of an open mouth. |
| `leg_*` / `foot_*` ordered **before** `body` | Complete leg tops must sit behind the garment hem. |
| Arms are bare fur, not indigo sleeves | The wrap is sleeveless in the master pose. The construction sheet's indigo arm parts belong to an alternate sleeved variant and are unused. |
| `hand_*` carries no outline of its own | Keeps the arm-to-paw mass reading as one silhouette; `arm_*` owns the outline. The hand is still a complete shape for rigging. |

The animation sheet is ordered left-to-right, top-to-bottom as `idle`, `blink`, `tap`, `held`,
`settle`, `speak`, `dismiss`, `sleep`, and `wake`. Treat these as key-pose targets rather than nine
independent clips: the Rive state machine must blend between them and preserve the interrupt rules
below. Reduced-motion variants should keep the face, eye line, silhouette, and pose change while
removing airborne travel, overshoot, and large tail follow-through.

## File and artboard contract

- File: `feature/pet/src/main/res/raw/castor.riv`
- Artboard: `Castor`
- State machine: `CastorStateMachine`
- Artboard origin: bottom center between the feet
- Transparent background and no baked speech bubble or floor
- Character must remain inside a stable optical canvas across every state

## Data-binding contract

| Property | Type | Purpose |
|---|---|---|
| `animationIntent` | enum/string | `idle`, `blink`, `tap`, `held`, `settle`, `speak`, `dismiss`, `sleep`, `wake` |
| `lookX` | number, -1..1 | Horizontal eye/head attention |
| `lookY` | number, -1..1 | Vertical eye/head attention |
| `reducedMotion` | boolean | Removes bounce, overshoot, particles, and large secondary motion |
| `speaking` | boolean | Keeps a subtle mouth/listen loop while the Compose bubble is present |

`PetVisualState` maps through `PetAnimationIntent`; finance events and copy must never be encoded in
the `.riv` file.

## Required layer hierarchy

Listed in z-order, back to front — this is the order `castor-master.svg` already uses.

```text
Castor
  shadow
  tail
    tail_base / tail_mid / tail_tip
  leg_left / foot_left
  leg_right / foot_right
  body
    torso
    hoodie_back
    hoodie_front
      hoodie_panel
      hoodie_collar_under
      hoodie_collar_over
    pocket
    leaf_badge
  arm_left / hand_left
  arm_right / hand_right
  head
    ear_left / ear_right
    head_tuft
    face
    whisker_left / whisker_right
    eye_left / pupil_left / lid_left
    eye_right / pupil_right / lid_right
    brow_left / brow_right
    mouth
      mouth_closed / mouth_open_smile / mouth_speak
    muzzle
    nose / nostrils
    tooth_left / tooth_right
  highlights
```

All paired limbs and facial parts must be separate, complete shapes beneath overlaps—not cropped
fragments. Preserve the approved indigo hoodie, teal leaf badge, broad beaver tail, rounded ears,
and visible incisors from `docs/pet-companion-assets/pet-family-v3-calico-samoyed-weather.png`.

## Rigging plan

Bones:

- root/hips, torso, chest, neck, head
- upper arm, lower arm and hand, both sides
- upper leg, lower leg and foot, both sides
- three tail sections following `tail_base` → `tail_mid` → `tail_tip`

Constraints and deformation:

- Constrain hands, feet, and the tail tip so they stay stable through large rotations.
- Use mesh deformation sparingly, and only on the hoodie, belly, and muzzle.
- `pupil_left` / `pupil_right` need independent translation controls driven by `lookX` / `lookY`.
- Lids and brows stay separate from the eyes so a blink can combine with any other state.
- `highlights` rides the head bone rather than deforming with it.
- Whiskers are stroke shapes with a short follow-through delay off the head bone.

## Timeline specification

| Timeline | Duration | Loop |
|---|---:|---|
| `idle` | 2.5–4 s | yes |
| `blink` | 120–180 ms | no |
| `tap` | 350–500 ms | no |
| `held` | 400–800 ms | yes |
| `settle` | 300–450 ms | no |
| `speak` | 600–900 ms | yes |
| `dismiss` | 350–600 ms | no |
| `sleep` | 2.5–4 s | yes |
| `wake` | 500–800 ms | no |

Key poses come from `castor-animation-states-v1.png`, read left-to-right, top-to-bottom in the
order above. Treat them as targets to blend between, not as nine independent clips.

## State machine configuration

Name it `CastorStateMachine`; `idle` is the default state.

- Transitions are driven by `animationIntent`.
- `held`, `dismiss`, `sleep`, and `wake` interrupt immediately.
- `blink`, `tap`, `settle`, `dismiss`, and `wake` are one-shots that return to `idle`.
- `speaking = true` holds the subtle mouth loop for as long as the Compose bubble exists.
- `reducedMotion = true` removes airborne travel, bounce, overshoot, and particles, and damps tail
  follow-through — while preserving facial expression, eye direction, and silhouette change.

The Android runtime can pause playback, and the machine settles when no animation work remains.

## Export

Keep both the editable Rive project and the runtime export. From the editor use
**Export → For runtime**, and save to:

```text
feature/pet/src/main/res/raw/castor.riv
```

Create the `raw` directory if it does not exist.

## Minimum animation acceptance

Idle, blink, tap, held, settle, speak, dismiss, sleep, and wake must be visually distinct. Transitions
must be interruptible by held, dismiss, sleep, and wake. Reduced motion must retain state clarity
without bounce or particles. Deliver the editable Rive source plus exported `castor.riv`; a sprite
sheet or flattened PNG does not satisfy the rig requirement.

## Status

| Step | State |
|---|---|
| Canonical model chosen and measured | done |
| Layered vector source (`castor-master.svg`) | done — 41 named layers, all complete closed shapes |
| Rig, timelines, state machine, data binding | **not started — requires the Rive editor** |
| `castor.riv` export | not started |
| Android renderer integration | blocked on the export |

The remaining work cannot be done from source control: Rive files are authored in the visual
editor, so steps three and four need a person in Rive with this document and the SVG open.
