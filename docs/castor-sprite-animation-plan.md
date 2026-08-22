# Castor Sprite Animation Plan (Clippy-Style Frame Animation)

**Status:** Proposed alternative to the Rive rig path
**Target fidelity:** Microsoft Office Assistant (Clippy), 1997–2003
**Blocker removed:** No Rive editor session, no bones, no mesh deformation, no `.riv`

## 1. Why this path works

Clippy was never rigged. Microsoft Agent characters (`.acs`) were hand-drawn cel
animation stored as **sprite frame sequences**, played back at roughly 10–15 fps with
branching between named animations. There was no skeletal deformation and no runtime
blending — motion read well because of *timing and pose economy*, not because of a rig.

This matters because sprite frames are **images**, and image generation is the one thing
AI tools genuinely do well. The `castor.riv` dependency — a GUI editor, a login, a
compiled binary format — disappears entirely.

The existing [`castor-animation-states-v1.png`](pet-companion-assets/castor-animation-states-v1.png)
is already a nine-pose key sheet in exactly this tradition. This plan extends each of
those nine key poses into a short frame strip.

The renderer contract does not change. `PetAnimationIntent` in
[`PetRendererContract.kt`](../feature/pet/src/main/kotlin/pe/moneyflow/feature/pet/PetRendererContract.kt)
already exposes the nine intents plus `reducedMotion`, `speaking`, and lifecycle state.
A sprite renderer satisfies that contract, and a future Rive renderer can still replace it.

## 2. Fidelity target

| Property | Clippy | Castor target |
|---|---|---|
| Technique | 2D cel frames | 2D cel frames |
| Playback | ~10–15 fps | 4–20 fps, per state |
| Frames per animation | Few; poses held | 3–8 unique |
| Transitions | Cut, or a short bridging animation | Cut to idle |
| Deformation | None | None |

Pose economy is the point. Clippy holds a pose and sells the motion with timing. Do not
chase smooth interpolation — it is neither achievable from generated frames nor necessary.

## 3. Frame inventory and timing

Frame counts chosen to land inside the durations already specified in
[`castor-rive-handoff.md`](castor-rive-handoff.md).

| State | Unique frames | Playback | Duration | Loop |
|---|---:|---:|---:|---|
| `idle` | 6 | 8 fps ping-pong | ~1.5 s | Yes |
| `blink` | 3 | 20 fps fwd+rev | ~150 ms | No |
| `tap` | 5 | 12 fps | ~420 ms | No |
| `held` | 4 | 8 fps | ~500 ms | Yes |
| `settle` | 5 | 12 fps | ~420 ms | No |
| `speak` | 4 | 6 fps | ~670 ms | Yes |
| `dismiss` | 6 | 12 fps | ~500 ms | No |
| `sleep` | 6 | 4 fps ping-pong | ~3.0 s | Yes |
| `wake` | 8 | 12 fps | ~670 ms | No |

**Total: 47 unique frames.** Ping-pong looping doubles apparent length at no asset cost.

### Reduced motion

Do not generate a second frame set. `reducedMotion` renders **frame 0 of the target state
as a static hold**, with cross-state changes only. Pose and expression still communicate
the state; travel, overshoot, and cycling are removed. This satisfies the accessibility
requirement at zero additional asset weight.

## 4. Asset budget

At 320×320 px (covers 104 dp through xxhdpi; acceptable upscale at xxxhdpi):

| Encoding | Per frame | 47 frames |
|---|---:|---:|
| PNG-24 + alpha | ~70 KB | ~3.3 MB |
| **WebP lossy + alpha, q80** | **~20 KB** | **~0.95 MB** |

**Ship WebP.** Native Android support, no dependency, and roughly 1 MB total — cheaper
than most icon sets and far below a 3D runtime. Rive would be smaller (~200 KB plus a
1–2 MB runtime), so the two land in the same order of magnitude.

## 5. Generation strategy — the critical part

Character consistency is the entire risk. Three rules:

**Rule 1 — Generate a whole strip in one image, never frame by frame.**
One generation containing all frames of a state keeps proportions, palette, and shading
coherent by construction. This is why the existing nine-pose sheet holds together.

**Rule 2 — Always pass the approved reference as an image input.**
Use [`castor-animation-states-v1.png`](pet-companion-assets/castor-animation-states-v1.png)
or [`castor-rig-construction-v1.png`](pet-companion-assets/castor-rig-construction-v1.png)
as style/character reference (image-to-image, `--cref`, IP-Adapter, or equivalent). Never
generate from text alone.

**Rule 3 — Lock the seed per state and regenerate the full strip on any retry.**
Never patch a single frame from a different seed; it will pop.

### Background

Request a **flat uniform chroma background** (`#00FF00`) rather than white or the cream in
the current sheets. Castor contains no green, so keying is clean and the teal leaf badge
is far enough away in hue to survive.

### Prompt template

Substitute the per-state motion description; keep everything else byte-identical.

```text
A horizontal strip of {N} sequential animation frames of the SAME cartoon beaver
character, evenly spaced, identical scale and identical vertical position in every frame.

Character: chubby cartoon beaver, warm brown fur, large round dark eyes with white
highlights, two prominent white front teeth, small rounded ears, cream muzzle with
freckles, thin light whiskers, broad cross-hatched flat brown tail, deep indigo hoodie
with a front pocket and a small teal leaf badge on the chest.

Motion across the strip: {MOTION}

Style: soft-shaded 2D cartoon illustration, clean readable silhouette, children's book
mascot, consistent lighting from upper left.

Requirements: flat solid #00FF00 background, no ground shadow, no text, no frame borders,
no numbering, full body visible in every frame, feet on the same baseline in every frame.
```

### Per-state `{MOTION}` values

| State | N | `{MOTION}` |
|---|---:|---|
| `idle` | 6 | standing still, breathing gently — chest and shoulders rise slightly, tail sways a few degrees, expression unchanged and friendly |
| `blink` | 3 | eyes fully open, then half-closed, then fully closed — head and body completely still |
| `tap` | 5 | reacting happily to being touched — a small delighted hop, arms lifting, mouth opening into a grin, then landing |
| `held` | 4 | being gently picked up and held in the air — body hanging relaxed, limbs dangling loosely, looking upward, slight sway |
| `settle` | 5 | landing softly from being released, absorbing the landing with a small squash, then straightening back to standing |
| `speak` | 4 | talking cheerfully — mouth opening and closing through a small talk cycle, one paw gesturing, eyes attentive |
| `dismiss` | 6 | turning away and walking off to the left, glancing back once, growing slightly smaller |
| `sleep` | 6 | curled up asleep resting on its own tail, breathing slowly and evenly, eyes closed peacefully |
| `wake` | 8 | waking from sleep — uncurling, a big yawn with arms stretched overhead, rubbing one eye, then settling into standing |

## 6. Post-processing pipeline

Generated strips are not shippable as-is. Four scripted steps, all of which I can implement:

1. **Slice** the strip into N frames on even boundaries.
2. **Key out** `#00FF00` to alpha, despill green fringing on edge pixels.
3. **Register** frames — this is the step that decides whether the animation reads as
   animation or as jitter. Detect the alpha bounding box per frame, then align every frame
   to a shared anchor: horizontal centroid, vertical **foot baseline** (bottom of the alpha
   box). This mirrors the artboard-origin convention already specified for Rive
   (bottom-center between the feet), so the two paths stay conceptually aligned.
4. **Pack** into one atlas per state plus a JSON manifest (frame rects, fps, loop mode,
   ping-pong flag), encoded as WebP into `feature/pet/src/main/res/drawable-nodpi/`.

Step 3 is where a human eye is still worth one pass. Expect to nudge a handful of frames.

## 7. Renderer implementation

New `PetSpriteRenderer` inside `:feature:pet`, consuming the existing `PetRendererInput`:

- Load the atlas + manifest once, cache bitmaps, recycle on disposal.
- Drive frame index from a single `withFrameNanos` loop; advance only while the state is
  animating and the app is foregrounded.
- Honor `reducedMotion` by pinning frame 0.
- Honor `speaking` by running the `speak` loop under the Compose bubble.
- Keep the existing `beaver_prototype.png` as the load-failure fallback.
- `lookX` / `lookY` are **not supported** — sprite frames cannot redirect gaze. Accept this
  as a known fidelity gap versus the Rive path (Clippy could not do it either).

Existing behavior, placement, safe areas, accessibility, and tests are untouched.

## 8. Acceptance criteria

- All nine states visually distinct in motion — the current prototype's core failure.
- No visible baseline jitter or horizontal drift within a state.
- No green fringing at any character edge.
- Total added APK weight under 1.5 MB.
- Stable frame pacing at 60 fps on the Pixel 7 emulator; no animation work while backgrounded.
- Reduced motion produces a readable static pose for every state.
- Existing 20 JVM and 7 instrumentation tests still pass.

## 9. Honest risks

| Risk | Severity | Mitigation |
|---|---|---|
| Frame-to-frame character drift | High | Strip-per-generation, seed lock, reference image, regenerate whole strips |
| `blink` needs a precise sub-pose delta that generators handle poorly | Medium | Only 3 frames; hand-composite from `castor-master.svg` lids if generation fails |
| Baseline jitter after keying | Medium | Programmatic foot-baseline registration, then one manual pass |
| `dismiss` scale change reads as zoom, not walking | Low | Accept, or drop the scale change and translate in Compose instead |
| Sprite fidelity below the approved 8–9/10 target | Medium | Explicitly a Clippy-fidelity decision; Rive remains the upgrade path |

## 10. Relationship to the Rive plan

This does **not** cancel [`castor-rive-handoff.md`](castor-rive-handoff.md). Because both
render behind `PetAnimationIntent`, sprites can ship now and Rive can replace them later
without touching behavior, placement, accessibility, or tests. The layered
[`castor-master.svg`](pet-companion-assets/castor-master.svg) stays valid as the Rive
source and as the fallback for hand-composited frames.
