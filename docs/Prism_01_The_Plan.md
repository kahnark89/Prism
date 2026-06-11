# Prism — Document 01: The Plan

> **⚠ SUPERSEDED (platform pivot, Epigenome 024 · 2026-06-08).** This document describes the original bespoke-hardware product (Pi5/CM5, custom carrier, enclosure, LED ring) — a form Prism no longer takes. Prism is now **two linked Android apps**; see `Prism_3.0_Platform_Architecture.md`. The design *ideas* here (two brains, glass box, safety layering, age-scaling) all carry forward; the hardware, BOM, and build-mission specifics do not. Preserved as design history (rejected path: `40_SHADOW.md` S09).

*A DIY smart learning camera that shows a child many points of view on the world, and teaches how AI itself works. The build is the first lesson.*

**Build tier:** Tier 2 (custom product) · **Primary users:** ages 3–5, designed to scale to 12 · **Builder:** adult with printer, soldering, and broad fabrication access

> **How to read this manual.** Every document has two tracks. The **Kid Track** (indented quotes like this) is the simple idea, the thing a child eventually learns. The **Builder Track** (normal text) is the real engineering for you. As the kids grow, more of the Builder Track becomes their track too.

---

## 1. What we are building, and why it is different from Dex

Dex names an object and translates the word. One discipline, one answer per snap, AI hidden in a black box.

Prism points at the same object and offers **several points of view** — and it is a **glass box**: the child can see that the machine is *guessing*, how sure it is, and when it is wrong. That single inversion is the whole point. A child who grows up knowing “the computer thinks it’s an apple, and it’s 84% sure” has an intuition about AI that most adults lack.

> **Kid Track:** “Prism is a magic magnifying glass. Point it at something and your friends inside tell you about it — and Prism tells you how sure it is, because even Prism doesn’t always know!”

---

## 2. Design principles (these govern every later decision)

1. **Glass box, not black box.** Always expose confidence and uncertainty in a child-friendly way. Uncertainty is a feature, not a bug to hide.
2. **Multiple points of view.** Knowledge is not one answer. Three friendly characters give different lenses on the same object.
3. **Two brains, one lesson.** A small model lives *on the device* (fast, offline, sometimes wrong). A big model lives *in the cloud* (smart, conversational, needs internet). The difference between them is the headline AI lesson.
4. **Audio- and image-first.** Users can’t read yet. Almost no text. Light, sound, voice, and one big photo carry the interaction.
5. **Age-scaling.** Same hardware for years. Software unlocks more characters, deeper content, and eventually a tinker/modify mode. The device grows with the child.
6. **Safety is the substrate, not a feature.** A pre-reader plus an LLM demands the tightest reasonable bounds. See §6.
7. **The build is documented as it happens.** Each phase produces a manual chapter. The finished guide becomes a mode on the device itself.

---

## 3. System architecture — the two brains

```
            ┌─────────────────────── PRISM DEVICE ───────────────────────┐
            │                                                             │
  [shutter]─┤  camera_handler ─► vision_engine (FAST BRAIN, on-device)    │
            │                         │  TFLite MobileNet, offline        │
            │                         │  outputs: label + confidence      │
            │                         ▼                                   │
            │                    safety (input gate)                      │
            │                         │                                   │
            │   ┌─── offline? ───► local response (“It’s an apple!”)      │
            │   │                     │ online?                           │
   [mic]────┤   │                     ▼                                   │
            │   │            perspective_engine ──► CLOUD LLM (SMART BRAIN)│
            │   │                     │   3 character personas             │
            │   │                     ▼                                   │
            │   │            safety (output gate)                         │
            │   │                     │                                   │
            │   └──► audio_feedback (TTS, per-character voice) ─► speaker  │
            │                         │                                   │
            │                    learning_log ──► parent dashboard         │
   [LED ring: glows while the smart brain is thinking]                    │
            └───────────────────────────────────────────────────────────────┘
```

**Why hybrid is mandatory, not a compromise.** A Compute Module cannot run a multimodal LLM locally. It *can* run a small image classifier. So we get both brains for free — and the contrast between “tiny model on the device” and “big model in the cloud” is exactly the concept we want a child to internalize. The architecture *is* the curriculum.

> **Kid Track:** “Prism has a fast little brain that lives inside and works even with no internet — but it can mix things up. And a big smart brain far away that knows lots of stories, but needs the internet to talk. The light glows while the big brain is thinking!”

---

## 4. Module contracts

Each module is independently testable. Build and verify one at a time (this is also how the Missions are sequenced).

| Module | Input | Output | Brain | Offline behavior |
|---|---|---|---|---|
| `camera_handler` | shutter press | RGB NumPy frame (640×480) | device | full |
| `vision_engine` | RGB frame | `{label, confidence, box}` | device (TFLite) | full |
| `perspective_engine` | label + image + (optional kid question) | 3 character responses (short) | **cloud LLM** | degrades: local name only |
| `conversation` | mic audio | transcribed question; dialogue state | cloud (STT+LLM) | disabled offline |
| `audio_feedback` | text + character id | spoken audio, per-character voice | device (TTS) | full |
| `safety` | any text in/out | pass / redirect / block | hybrid | full (local rules) |
| `learning_log` | session events | CIAER-style records → dashboard | device | full |
| `ui` / `led` | state | round display + LED ring + haptic | device | full |

**`vision_engine` exposes confidence and the bounding box on purpose** — that data feeds the glass-box UX, not just the logic.

**`learning_log` records each exploration as a chain** — saw it → guessed → snapped → heard → learned. (If the CIAER schema in your other work looks familiar here, that’s because this is the same capture instinct pointed at a child’s curiosity instead of an operator’s expertise.)

---

## 5. Hardware — Tier 2 Bill of Materials

Two stages: prototype on dev boards, then move to a custom carrier for the real product.

### 5a. Prototype rig (validate the system first)
| Part | Choice | Note |
|---|---|---|
| Compute | Raspberry Pi 5, 4GB | headroom for UI + camera + audio + network |
| Camera | Camera Module 3 (autofocus) | autofocus is essential — kids point at anything, any distance |
| Display | 4” round capacitive touch (DSI or HDMI) | echoes the magnifying-glass form; touch is secondary to audio |
| Audio out | MAX98357A I2S amp + small full-range speaker | clear voice is the primary channel |
| Audio in | I2S MEMS mic (ICS-43434 / SPH0645) | unlocks voice Q&A — the real “conversation” |
| Inputs | 1 large arcade-style shutter + 2 big character buttons | minimal controls for tiny hands; no fiddly encoder yet |
| Feedback | NeoPixel LED ring + small vibration motor | LED = “thinking” cue and AI-literacy signal |
| Power | protected LiPo + power HAT, USB-C charge | sealed, non-accessible cell |
| Storage | microSD (prototype only) | replace with eMMC in product — nothing a toddler can pull out |

### 5b. Product build (your fab access makes this real)
| Subsystem | Decision | Why it matters at ages 3–5 |
|---|---|---|
| Compute | **Compute Module 5 (eMMC)** on a **custom carrier board** | no loose SD card; integrate power, audio, mic, buttons on one PCB |
| Enclosure | molded **PC or PC/ABS** shell + **TPE overmold** grip | impact + grip; see material note below |
| Sealing | rounded edges, splash-resistant (IP54-ish), gasketed seams | they will drool, spill, and drop it |
| Thermal | passive heatsink + airflow; **enforce skin-contact surface temp limit** | Pi 5 / CM5 runs warm; a handheld toddler device must stay safe to touch |
| Battery | protected pack, fuel gauge, robust enclosure, no user access | LiPo safety is non-negotiable in a toddler’s hands |
| Fasteners/parts | nothing detachable that’s a choking hazard; captive screws | small-parts rule |

> **Material note for you specifically.** A 3–5-year-old *will* mouth this. Avoid soft PVC and heavily plasticized TPE on contact surfaces — you know better than most how plasticizers and processing lubricants migrate and degrade out of a polymer over time and heat. Favor a TPE/TPU overmold with no phthalate plasticizers (and confirm food-contact-grade for any surface a child can reach with their mouth). Surface temperature and additive migration are the two spec lines I’d put your name on.

---

## 6. Safety model (ages 3–5 + LLM)

Non-negotiable, layered:

1. **Input gate** — the only things that reach the cloud are: the recognized object label, the photo, and short bounded kid questions. No open web. No free text from anywhere else.
2. **System prompt constraints** — responses are 1–2 short, warm, concrete sentences. No scary, violent, sad, or instructional content. Gently redirect anything off-topic back to “the thing you’re looking at.”
3. **Output gate** — a safety classifier pass runs *before* text reaches TTS. Block-or-redirect on fail; never speak unreviewed model output to a small child.
4. **Topic bounding for the youngest tier** — Prism essentially only talks about the object in front of it and a small set of kid-safe expansions. This widens as the age-scaling unlocks.
5. **Graceful offline degrade** — with no internet, the fast brain still names objects. The device never goes dead silent.
6. **Parent dashboard + review** — every session is logged; content level and characters are parent-controlled.

> **Kid Track:** Children never see this layer. They just experience a friend who is always kind and only ever talks about fun, safe things.

---

## 7. The three characters (the “multiple points of view,” sized for a 4-year-old)

Seven expert lenses overwhelm a preschooler. Three recurring *friends* do not. Each is a persona with its own voice, and each is one lens:

- **Pip — the “How does it work?” friend** (science / making). *“It grew from a flower! There are tiny seeds inside that could each become a whole tree.”*
- **Lumi — the “Look and feel” friend** (art / senses). *“Look how the red turns into yellow. Want to draw that?”*
- **Tale — the “Story” friend** (world / story / words). *“People have grown these for a very, very long time. In Spanish it’s manzana!”*

Plus Prism’s own honest voice for the AI-literacy micro-moments: *“I thought this was an apple and I was pretty sure — but I almost said tomato!”*

The child swaps characters with a big button. As they age, the roster expands toward the full expert panel (Scientist, Engineer, Historian, Word-Explorer, and the AI explaining its own reasoning in real depth).

---

## 8. The roadmap & where this fits

**Idea ✓ → Plan ✓ (this document) → Implementation → Product → UX.**

Implementation is staged so each stage is a self-contained Mission *and* a lesson:

- **Mission 0 — First Light:** camera → fast brain → speech, offline, on the prototype rig. Proves the loop in a weekend.
- **Mission 1:** add the cloud smart brain + one character.
- **Mission 2:** full three-character panel + “Prism explains itself.”
- **Mission 3:** voice / conversation via the mic.
- **Mission 4:** round display + glass-box confidence visualization.
- **Mission 5:** safety layer hardening + parent dashboard.
- **Product phase:** custom carrier board + designed enclosure.
- **UX phase:** voices, swap interaction, onboarding, the build-story mode.

**Next deliverable:** *Document 02 — Mission 0: First Light* — the actual step-by-step build (wiring, OS, libraries, the offline classifier loop), in dual-track format, with the kid-facing concepts (what a pixel is, what “confidence” means) woven into each step.

---

*Prism · Document 01 of the build manual · The Plan*
