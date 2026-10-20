# DERMIFY — Project Context

A handoff document. Read this before touching the codebase.

**What it is:** an Android app that screens photographs of skin for four
conditions, runs entirely on-device, explains its prediction with a heatmap,
and refers uncertain cases to a clinician over WhatsApp.

**Why it exists:** final-year BSc Telecommunication Engineering capstone,
KNUST, Kumasi, Ghana. Two-person team — Caleb (Android/Kotlin), Hannah
(Python/ML).

**Status:** working end-to-end. Model trained and exported, app builds and
runs on two physical devices. One known bug (Supabase uploads, see
[Known issues](#known-issues)).

---

## 1. The four classes

Chosen from Ghanaian hospital prevalence data, not convenience:

| Condition | Prevalence in Ghana | Model F1 (5-fold) |
|---|---|---|
| Eczema / dermatitis | 24.6% | 69.4% |
| Fungal infection | 15.2% | 71.4% |
| Acne vulgaris | 5.3% | 92.7% |
| Scabies | 5.1% | 79.3% |

Sources: KBTH Dermatology Clinic review (Rosenbaum et al., 2017); KATH study
(Asiedu-Gyekye et al., 2022, n=460); community study (Amoako et al., BMC
Infect Dis 2024, n=1,327).

**Critical:** the label order the app uses is the order Keras'
`flow_from_directory` produces, which is alphabetical-ish and **not** the order
in the `CLASS_NAMES` list:

```
["Acne", "Eczema", "Scabies", "fungal infection"]
```

Getting this wrong silently swaps predictions. It has bitten this project once
already. `class_labels.txt` must be written from `target_names`, never from a
hardcoded list.

---

## 2. Android app

### Stack

| | |
|---|---|
| Language | Kotlin |
| Package | `com.dermify.dermify` |
| Project dir | `Dermify2` |
| Build | AGP 9.1, `android.uniquePackageNames=false` |
| Min SDK | 24 |
| UI | Material 3, DayNight theme, teal primary `#0097A7` |
| Nav | Single nav graph, ViewPager2 for onboarding |
| ML runtime | `org.tensorflow:tensorflow-lite:2.14.0` (single dependency — adding more causes namespace conflicts under AGP 9.1) |
| Local DB | Room (`android.builtInKotlin=false`, `android.newDsl=false` — required for the annotation processor) |
| Auth | Firebase Auth + Firestore |
| Cloud storage | Supabase (bucket `scans`, West Europe) |

### Test devices

- Samsung SM-A320FL, Android 8 — the low-end target; latency budget is set by
  this device
- OPPO CPH2631, Android 16

### Package layout

```
com.dermify.dermify.ml/
├── CaptureGate.kt        three-layer "is this skin?" chain
├── SkinHeuristic.kt      layer 1 — YCbCr colour ratio, no model
├── NotSkinGate.kt        layer 2 — MobileNet v1, ImageNet objects
├── SkinBinaryGate.kt     layer 3 — MobileNetV2 sigmoid, domain-trained
├── HeatmapGenerator.kt   disease model + CAM overlay
└── ModelCache.kt         loads each model once, frees on memory pressure
```

### Assets (`app/src/main/assets/`)

```
mobilenet_v1.tflite                  layer 2 gate
skin_gate.tflite                     layer 3 gate (2.6 MB)
dermify_model_with_heatmap.tflite    dual-output disease model
cam_class_weights.json               class-averaged CAM weights
class_labels.txt                     MUST match target_names order
```

---

## 3. The capture gate (three layers, AND)

An image must pass **all three** to reach the disease model. Ordered cheapest
first. Each layer returns `true` = reject.

**Layer 1 — `SkinHeuristic`**
Colour-ratio maths in YCbCr, threshold 0.35. ~50 ms, no model.
Catches: rooms, sky, grass, grey electronics, documents.
Misses: wood, cardboard, beige/tan surfaces (they read as skin-coloured).

**Layer 2 — `NotSkinGate`**
MobileNet v1 (1001-class ImageNet), threshold 0.40 on max softmax.
Logic: ImageNet has no skin-condition classes, so a *confident* ImageNet
prediction means it's a recognisable object, not skin.
Catches: desks, mugs, chargers, keyboards.
Misses: arbitrary scenes and featureless surfaces.

**Layer 3 — `SkinBinaryGate`**
MobileNetV2, input `[1,224,224,3]` float32, output `[1,1]` sigmoid, not
quantised. Exists to catch what layers 1 and 2 both miss.

Behaviour was determined **empirically**, not from documentation — the verbal
description of the model turned out to be inverted:

- preprocessing: `pixel / 255f` → `[0,1]`. Raw 0–255 fails outright;
  `[-1,1]` separates but worse.
- **output HIGH = skin** (dermatology refs scored 0.87–1.00), LOW = not-skin
  (wood scored 0.07–0.21)
- threshold 0.50, `isLikelyNotSkin = score < 0.50`

If this model is ever retrained, re-probe those three properties. Do not
assume.

### Composition

`CaptureGate.check()` runs the layers in order and returns:

```kotlin
sealed class Verdict {
    object Accepted : Verdict()
    data class Rejected(val reason: String, val userMessage: String) : Verdict()
}
```

Each layer is wrapped in try/catch — if a model fails to load, that layer is
skipped rather than blocking the user, since earlier layers already passed.
Models come from `ModelCache`, never constructed per-scan (that was a
significant latency cost before it was fixed).

---

## 4. Disease model

**Architecture** (Keras functional):

```
EfficientNetB0 (ImageNet weights, include_top=False)
  → GlobalAveragePooling2D
  → BatchNormalization
  → Dense(256, relu, L2 1e-4) → Dropout(0.50)
  → Dense(128, relu, L2 1e-4) → Dropout(0.40)
  → Dense(4, softmax)
```

Input 224×224. Loss: categorical crossentropy, `label_smoothing=0.05`.

**Two-phase training:**
1. Base frozen, Adam 1e-3, 30 epochs
2. Unfreeze from layer 165 (BatchNorm layers stay frozen — unfreezing them with
   a small batch destabilises running stats), Adam 2e-5 with cosine decay,
   30 epochs

**Export:** a dual-output TFLite model returning both `[1,4]` class logits and
`[1,7,7,1280]` conv feature maps, so the app can compute a heatmap without a
second forward pass.

---

## 5. Explainability — CAM, not Grad-CAM

This distinction matters and is easy to get wrong.

**In the notebook:** true Grad-CAM. Per-image gradients via `GradientTape`.

**In the app:** CAM. TFLite cannot compute gradients at inference, so the
notebook exports *class-averaged* pooled gradients (60 training images per
class) to `cam_class_weights.json`. The app multiplies the exported feature
maps by those fixed per-class weights.

Consequence: the app's heatmap reflects "what generally matters for this class"
rather than "what mattered for this image." It is coarser and can miss
atypically-located lesions.

**Keras 3 gotcha:** rebuilding a graph from a nested model's `.output` raises
`KeyError` inside `Functional.call()`. The working pattern is to call `base`
directly and chain the head layers manually inside the tape:

```python
with tf.GradientTape() as tape:
    conv_out = base(img, training=False)
    tape.watch(conv_out)          # not a Variable — must watch explicitly
    x = conv_out
    for layer in head_layers:
        x = layer(x, training=False)
```

For TFLite export the fix is different — build a **fresh** graph from a new
`Input`, since conversion needs a traced Model rather than eager execution.

---

## 6. Data pipeline and the leakage incident

**Read this section before changing anything about splitting.**

### What happened

Initial results showed test accuracy (83.5%) *above* validation (79.4%). That's
backwards and it was correctly flagged as suspicious.

A perceptual-hash audit found **30 cross-split near-duplicate pairs** — the same
clinical photographs appearing in multiple public datasets under different
filenames (`Picture36.jpg` ↔ `t-tinea-groin-69.jpg`, and so on). MD5 caught only
4 of them because the files had been recompressed.

### The fix

Cluster by perceptual hash (DCT-based, Hamming ≤ 5) with union-find, then split
by **group** so every copy of an image lands in the same split. `CELL 4` asserts
zero cross-split duplicates and halts if violated.

Measured separation on real images: true duplicates hash to distance 0 even
after resize + JPEG recompression; distinct images sit at ≥ 14. Threshold 5 is
comfortably clear of both.

### What it turned out to be

Removing the duplicates did **not** close the gap. Further investigation found
the real cause was comparing two different measurements — training-time
epoch-end `val_accuracy` against best-checkpoint inference-mode test accuracy.
Evaluating both through an identical code path resolved it.

The notebook now has an "apples-to-apples" cell that scores train, val and test
identically and prints both gaps with interpretation thresholds.

### Rules that follow from this

- Augmentation applies to the **training generator only**. Val and test use a
  preprocessing-only generator.
- Never compare `history.history['val_accuracy']` to a test number computed
  after `load_model`.
- Report cross-validated figures, not single splits.

---

## 7. Results

**Headline: 78.14% ± 0.83** (group-aware 5-fold CV with test-time augmentation,
95% CI 77.4–78.9%). Without TTA: 76.83% ± 0.82. Mean AUC-ROC 0.9571.

Dataset: 1,976 images → 1,915 unique groups after deduplication (61 redundant
copies).

A single held-out split gave 81.14%, but single-split numbers varied by 1.35
points across runs, so the CV figure is the one reported.

### What does NOT improve it

Tested and found not to help:

- EfficientNetB1 at 240px — **worse** (75.20% ± 1.68)
- Stronger regularisation — improved the train/val gap but not accuracy
- Test-time augmentation — inconsistent; ranged from −0.67 to +3.03 across
  runs, averaging ~+1.3

Three quite different configurations all landed in 74–78%. The constraint is
dataset size and label quality (~500 images/class, no dermatologist
verification), not model capacity.

### TTA in deployment

The app ships **single-view** inference. TTA would multiply latency by four,
which the Samsung A320 cannot absorb. Reported TTA figures represent model
capability, not deployed behaviour.

---

## 8. App features beyond classification

**Confidence gate.** Predictions below 50% display a caution note. Ties
directly to the telemedicine path.

**Telemedicine referral.** `TelemedicineActivity`, Pending/Sent tabs. Builds a
de-identified composite image (photo + prediction + heatmap, no name or patient
ID) and sends it via WhatsApp's native share using FileProvider.
Store-and-forward, not live consultation — works over intermittent
connectivity.

**Consent flow.** Users may opt in to contribute images for future training.
Consent is explicit and separate from the diagnosis flow.

**Firebase Auth + Firestore**, three-tier security rules: Tier 1 Clinician,
Tier 2 Specialist, Tier 3 Admin.

**Room** for local scan history.

**Performance:** `ModelCache` singleton holds each interpreter; `DermifyApp`
releases them on `onTrimMemory`/`onLowMemory`; inference runs on a background
coroutine.

---

## 9. Known issues

**Supabase uploads failing.** Images are not reaching the `scans` bucket.
Postgres logs show `58P01` (`undefined_file`) errors. Not yet diagnosed. Most
likely candidates, in order: missing INSERT policy under Storage RLS (Supabase
denies by default); bucket name mismatch (case-sensitive); auth state not
established before the upload call. Next step is to read the upload code and
Logcat output rather than guess.

**Unverified on device:** the confidence-gate caution note and the heatmap
overlay ghosting fix are both code-complete but not confirmed on hardware.

**`SkinBinaryGate` threshold** is set to 0.50 from probe data on clean
dermatology reference images. Real captures through the app's dashed guide will
score differently. Add score logging and calibrate against real captures before
trusting it.

---

## 10. Gotchas

- **Label order** — from `target_names`, not `CLASS_NAMES`. Silent failure mode.
- **Double normalisation** — `ImagePreprocessor.java` once scaled an already-
  scaled tensor. Wrong preprocessing does not throw; it returns plausible
  garbage near 0.5. Verify empirically.
- **TFLite dependencies** — exactly one (`tensorflow-lite:2.14.0`). More causes
  AGP 9.1 namespace conflicts.
- **Room + Kotlin** — needs `android.builtInKotlin=false` and
  `android.newDsl=false`.
- **Poppins font** has no `→` glyph. Arrows in diagrams render as boxes.
- **Kaggle dataset paths** contain spaces and inconsistent casing. Two datasets
  exist (1,976 and 2,246 images); the 1,976 `wonn` set is the one the reported
  results and slides use.

---

## 11. Working conventions

Things this project has learned the hard way:

**Verify empirically rather than trusting descriptions.** The `skin_gate` output
direction was described one way and measured the opposite. Probe the model.

**Read the actual file before writing code that touches it.** Guessing at a
`ModelCache` accessor signature or a return type wastes more time than asking.

**Commit to an experimental configuration in advance.** Running several variants
and reporting the best turns cross-validation into a selection metric and
invalidates the error bars.

**Report the number you can defend.** 78.14% with a methodology behind it beats
81.14% that varies run to run.
