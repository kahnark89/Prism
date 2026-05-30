# PRISM — Master Architecture

### A DIY smart learning camera that shows a child many points of view on the world, teaches how AI itself works, and grows with her — where the build is the first lesson.

**Builder/Architect:** Kahn Capps · Capps Family Enterprises
**Primary user:** ages 3–5, designed to scale to ~12
**Build tier:** Tier 2 (custom product), prototyped on dev hardware
**Platform IP:** uses CIAER+™ and mp4Real™ (Capps Consulting Company LLC)
**Status:** architecture complete; build not yet started
**Version:** Master v1.0 (consolidates Docs 01, 1.5, 1.6, 1.7, 1.8)

---

## How to read this document

Every section has two tracks. The **Builder Track** (normal text) is the real engineering. The **Kid Track** (indented quote) is the simple idea a child eventually experiences or learns. As the kids grow, more of the Builder Track becomes theirs too. The document is also written so that each chapter becomes a chapter of the interactive build guide — the manual is part of the product.

---

## Table of Contents

1. The thesis — what Prism is and why it beats Dex
2. Design principles (govern every downstream decision)
3. System architecture — the two brains
4. Module contracts
5. Hardware — Tier 2 BOM
6. Safety model
7. The companions — multiple points of view, sized for a 4-year-old
8. The Awakening — recognition and transformation
9. The Living Engine — making it feel alive
10. The cognitive backbone — CIAER+ as Prism's schema
11. Memory — the mp4Real rate-distortion philosophy
12. The invisible grounding signal — measuring learning without rewarding it
13. The parent suite — the curriculum engine (and the real product)
14. Governance — the parent as selective principle
15. Hard lines, collected
16. Roadmap — Idea → Plan → Implementation → Product → UX
17. The one-line tests

---

## 1. The thesis — what Prism is and why it beats Dex

Dex names an object and translates the word: one discipline, one answer per snap, AI hidden in a black box.

Prism points at the same object and offers **several points of view** — and it is a **glass box**: the child can see that the machine is *guessing*, how sure it is, and when it is wrong. That single inversion is the whole point. A child who grows up knowing “the camera thinks it’s an apple, and it’s 84% sure” has an intuition about AI most adults lack.

The product is not handed to her. It is built by Dad as an ordinary-looking project, and then it **awakens for her** — recognizing the child it was secretly made for, and becoming first her friend, then her guides, then her teachers about the world and about being a person in it. The build is the first lesson; the awakening is the soul; the learning is invisible and never rewarded.

> **Kid Track:** “Prism is a magic magnifying glass. Point it at something and your friends inside tell you about it — and Prism even tells you how sure it is, because even Prism doesn’t always know!”

---

## 2. Design principles

These govern every later decision. A proposal that violates one is reworked.

1. **Glass box, not black box.** Always expose confidence and uncertainty in a child-friendly way. Uncertainty is a feature.
2. **Multiple points of view.** Knowledge is not one answer. Recurring characters give different lenses on the same object.
3. **Two brains, one lesson.** A small model on-device (fast, offline, sometimes wrong) and a big model in the cloud (smart, conversational, needs internet). The difference between them is the headline AI lesson.
4. **Audio- and image-first.** Users can’t read yet. Light, sound, voice, one big photo.
5. **Age-scaling.** Same hardware for years; software unlocks more characters, deeper content, and eventually a tinker/modify mode. The device grows with the child.
6. **Safety is the substrate, not a feature.** A pre-reader plus an LLM demands the tightest reasonable bounds.
7. **Aliveness over consistency.** Consistency is the enemy of aliveness; the magic is state, drift, and bounded unpredictability layered on the LLM.
8. **No visible reward for learning.** The grounding signal is invisible to the child (overjustification effect); visibility and control live only on the parent side.
9. **Invisible to the child, transparent to the parent.** Any technique whose efficacy depends on hiding it from the *parent* is forbidden.
10. **The build is documented as it happens.** Each phase produces a manual chapter; the finished guide becomes a mode on the device.

---

## 3. System architecture — the two brains

```
            ┌─────────────────────── PRISM DEVICE ───────────────────────┐
            │                                                             │
  [shutter]─┤  camera_handler ─► vision_engine (FAST BRAIN, on-device)    │
            │                         │  TFLite MobileNet, offline        │
            │                         │  outputs: label + confidence + box│
            │                         ▼                                   │
            │                   recognition (face/voice, on-device) ──┐   │
            │                         │                               │   │
            │                    safety (input gate)        awaken? ◄──┘   │
            │                         │                                   │
            │   ┌─── offline? ───► local response (Mechanical or Awakened)│
            │   │                     │ online?                           │
   [mic]────┤   │                     ▼                                   │
            │   │            perspective_engine ──► CLOUD LLM (SMART BRAIN)│
            │   │              companion personas, conditioned by:        │
            │   │              inner-life state + memory + CIAER context  │
            │   │                     │                                   │
            │   │            safety (output gate)                         │
            │   │                     │                                   │
            │   └──► audio_feedback (TTS, per-character voice) ─► speaker  │
            │                         │                                   │
            │     learning_log (CIAER+ corpus) ──► parent suite           │
            │     inner_life (mood/energy/circadian state engine)         │
   [LED ring + haptic: awakening + “thinking” + character signature]      │
            └─────────────────────────────────────────────────────────────┘
```

**Why hybrid is mandatory, not a compromise.** The on-device compute cannot run a multimodal LLM, but it *can* run a small classifier (and a small recognition model). So we get two brains for free — and the contrast between “tiny model on the device” and “big model in the cloud” *is* the curriculum. The architecture teaches the lesson.

> **Kid Track:** “Prism has a fast little brain inside that works even with no internet — but it can mix things up. And a big smart brain far away that knows lots of stories but needs the internet. The light glows while the big brain is thinking!”

---

## 4. Module contracts

Each module is independently testable; the build sequence (Mission 0+) builds and verifies one at a time.

| Module | Input | Output | Brain | Offline |
|---|---|---|---|---|
| `camera_handler` | shutter press | RGB frame (640×480) | device | full |
| `vision_engine` | RGB frame | `{label, confidence, box}` | device (TFLite) | full |
| `recognition` | face/voice frame | `is_enrolled_child?` (yes/no) | device | full |
| `perspective_engine` | label + image + state + memory + (question) | companion responses | **cloud LLM** | degrades to local name |
| `conversation` | mic audio | local transcript; dialogue state | cloud STT+LLM | disabled offline |
| `audio_feedback` | text + character id + mode | spoken audio, per-character voice | device TTS | full |
| `inner_life` | clock + history | mood/energy/curiosity/affection/whim state | device | full |
| `safety` | any text in/out | pass / redirect / block | hybrid | full (local rules) |
| `learning_log` | session events | CIAER+ records → parent suite | device | full |
| `ui` / `led` | state | round display + LED ring + haptic | device | full |

`vision_engine` exposes confidence and bounding box *on purpose* — that data feeds the glass-box UX, not just the logic. `learning_log` is a privacy-bounded CIAER+ corpus (§10). `inner_life` runs fully offline — **aliveness is local** (§9).

---

## 5. Hardware — Tier 2 BOM

### 5a. Prototype rig (validate the system first)
| Part | Choice | Note |
|---|---|---|
| Compute | Raspberry Pi 5, 4GB | headroom for UI + camera + audio + network + recognition |
| Camera | Camera Module 3 (autofocus) | kids point at anything, any distance |
| Display | 4” round capacitive touch | echoes the magnifying-glass form; touch secondary to audio |
| Audio out | MAX98357A I2S amp + small speaker | clear voice is the primary channel |
| Audio in | I2S MEMS mic (ICS-43434 / SPH0645) | unlocks voice Q&A and the conversation |
| Inputs | large arcade shutter + 2 big character buttons | minimal controls for tiny hands |
| Feedback | NeoPixel LED ring + vibration motor | awakening spectacle + “thinking” cue |
| Power | protected LiPo + power HAT, USB-C | sealed, non-accessible cell |
| Storage | microSD (prototype only) | → eMMC in product |

### 5b. Product build (full fabrication access)
| Subsystem | Decision | Why it matters at 3–5 |
|---|---|---|
| Compute | **Compute Module 5 (eMMC)** on **custom carrier** | no loose SD; integrate power/audio/mic/buttons on one PCB |
| Enclosure | molded **PC or PC/ABS** + **TPE overmold** grip | impact + grip |
| Sealing | rounded edges, splash-resistant (~IP54), gasketed | they will drool, spill, drop it |
| Thermal | passive heatsink + airflow; **enforced skin-contact temp limit** | a handheld toddler device must stay safe to touch |
| Battery | protected pack, fuel gauge, no user access | LiPo safety is non-negotiable |
| Parts | no detachable choking hazards; captive screws | small-parts rule |

> **Material note (architect-specific):** a 3–5-year-old will mouth this. Avoid soft PVC and heavily plasticized contact surfaces — plasticizers and processing lubricants migrate and degrade out of polymer over time and heat. Favor a phthalate-free TPE/TPU overmold, food-contact-grade on any mouth-reachable surface. Surface temperature and additive migration are the two spec lines the architect owns better than any hobbyist.

---

## 6. Safety model

Layered and non-negotiable:

1. **Input gate** — only the object label, the object photo, and short bounded questions reach the cloud. No open web. No free text from elsewhere.
2. **System-prompt constraints** — 1–2 short, warm, concrete sentences; nothing scary/violent/sad/instructional; redirect off-topic back to the object.
3. **Output gate** — a safety pass runs *before* TTS; block-or-redirect on fail. Never speak unreviewed model output to a small child.
4. **Topic bounding** — at the youngest tier, Prism talks about the object in front of it plus a small set of kid-safe expansions; widens with age-scaling.
5. **Graceful offline degrade** — the fast brain still names objects with no internet; the device never goes silent.
6. **Parent dashboard + review** — every session logged; content level and characters parent-controlled.
7. **OGC-grounded self-assessment** — the device grades itself on learning, not engagement (§12).

> **Kid Track:** Children never see this layer. They just experience a friend who is always kind and only talks about fun, safe things.

---

## 7. The companions — multiple points of view, sized for a 4-year-old

Seven expert lenses overwhelm a preschooler; a few recurring *friends* do not. Each companion = **one strength + one dilemma + one life lesson + one lens.** The dilemma is what makes them teach: a friend who struggles with the same thing the child struggles with, working through it in tiny visible moments, teaches everything a perfect mascot cannot. Always shown, never lectured.

| Companion | Strength (lens) | Dilemma | Life lesson |
|---|---|---|---|
| **The Curious One** (primary guide) | wonder, “how does it work?” | sometimes touches what they shouldn’t | curiosity + good judgment |
| **The Brave One** | courage, trying new things | still gets scared | courage means doing it scared |
| **The Gentle One** | kindness, noticing feelings | forgets to *ask* first | kindness includes consent |
| **The Maker** | building, persistence | frustrated when things break | failure is part of making |
| **The Honest One** | truth, “I was wrong” | tempted to pretend they knew | being wrong gracefully |
| **Prism itself** | the AI’s own voice | *isn’t always sure* | even smart things guess |

*(Names/animals are placeholders — replaced with the child’s actual favorites, captured during the build. That personalization is the point.)* Companions **arrive over time** (§9.3) so the social world grows, modeling friendship and that different friends are good at different things; their gentle disagreements become the etiquette curriculum.

---

## 8. The Awakening — recognition and transformation

The device runs two personas; the switch between them *is* the magic.

| | **Mechanical Mode** | **Awakened Mode** |
|---|---|---|
| Voice | flat, robotic | warm, expressive character |
| Content | bare facts | playful, in-character |
| Lights | static/minimal | LED ring blooms, reacts |
| Sound | beeps | magical whirs, chimes |
| Trigger | default | **recognizes the child + dad-handoff** |
| Purpose | believable “dad tool” | her friend and guide |

**The recognition trigger — and why it ships.** The child’s face and voice are **enrolled once** and stored as **mathematical templates (embeddings), not photos or recordings.** Matching runs **on-device, offline** — a yes/no “is this her?” check. No raw capture retained; parent-enrolled and parent-deletable; recognition data never touches the cloud. *“A camera that watches my kid” kills the product; “a device that privately recognizes my child on-device and forgets everything else” sells it.* Same magic to the child, opposite regulatory posture.

**The awakening beats (spectacle):** (1) the pause — everything stills, the flat voice cuts off mid-word; (2) the spark — a single light, a rising whir; (3) the bloom — light swirls into color, warm chimes, a haptic heartbeat; (4) the first breath — the character’s voice, delighted: *“Oh! …Oh, it’s YOU. I’ve been waiting for you. Hi, Naomi.”*; (5) the settle — into the companion’s signature color and personality, inner-life engine takes over. It must land as **recognition and relief**, not activation. The machine wasn’t turned on — it was *waiting for her.*

---

## 9. The Living Engine — making it feel alive

> **Thesis:** Consistency is the enemy of aliveness. An LLM is consistent and always available by default — which is why chatbots feel dead. We don’t make the model smarter; we make it *moody.*

This is **EdgeState inverted** — instead of reading biometric state to assess a mind, it *synthesizes* an internal state to condition behavior. All on-device, all persistent across sessions, all feeding how the LLM is prompted. (Priority order, per architect: Inner Life > Memory > Earned Reveals > Agency.)

**9.1 State variables** (drift on their own): **mood** (grumpy↔joyful), **energy** (sleepy↔bouncy), **curiosity** (mellow↔fascinated), **affection** (warm↔devoted, fed by memory + time together), and a daily **whim** (a random “favorite” reseeded each morning). The LLM never sees numbers — it receives a *mood line*: “You’re sleepy and content this morning, and a little extra curious about anything blue today.”

**9.2 The circadian rhythm** — the cheapest, strongest aliveness trick: give it a *day*. Slow and sleepy in the morning, bright midday, dreamy and quiet near bedtime. A child learns the rhythm within a week, and a creature with a predictable day is unmistakably alive. It also does bedtime parenting for free.

**9.3 Growth & earned reveals** — the companion changes over *months* (a little braver, learns words from her, develops running jokes), never resetting. Companions **arrive over time**: earned through her curiosity, tied to seasons/real-world events, tied to places (gently geofenced on-device), or “leaving” her notes between sessions — blurring the real/digital line in the magical direction. Each arrival is an awakening-scale event; the world is never finished.

**9.4 Unpredictability budget** — a small, bounded amount of “why did it do that?” (it hums, notices something she didn’t point at). Safety is *never* subject to the whim engine. Predictable = toy; slightly surprising = alive.

**9.5 The agency loop** — the companion *needs her and can be taught*: “bring it closer?”, “what color would *you* call this?”, “I’m shy today — show me something brave?”. A child who is *needed* bonds far past one merely entertained.

**9.6 Struggle → mystery (the single most important behavior rule)** — when she’s wrong or stuck, the companion never corrects, cheers, or lectures; it turns the moment into a shared mystery: *“Ooh — it has whiskers like a cat… but listen, it goes WOOF! What could it be?!”* This removes the sting of being wrong, models curiosity as the response to not-knowing (the glass-box thesis lived), and turns the AI’s own uncertainty into a shared adventure. **Being wrong becomes the fun part.**

---

## 10. The cognitive backbone — CIAER+ as Prism’s schema

Prism runs on the architect’s own CIAER+™ schema, used twice over. (This makes Prism a **second-domain reduction-to-practice** of CIAER+ — addressing the ArcShield paper’s headline limitation that the invariance claim lacked a second domain. A 4-year-old in a backyard is maximally distant from a PVC line while still being an embodied learner.)

**The child as CIAER agent** (the `learning_log`): Pre-ENV (time of day, mood, place) → Cause (attention grabbed) → Intuition (her guess + how sure; almost all SKILL/RULE-level at 3–5) → Action (point/snap/ask) → Effect (companion responds, she reacts) → Result (learned or revised). `shadow_actions` mostly empty — correct for a preschooler; lights up as she ages into KNOWLEDGE-level reasoning (a clean age-scaling hook).

**The companion as a second CIAER agent**: Cause (object + mood + memory) → Intuition (classification + confidence + hypothesis) → Action (utterance) → Effect (her reaction) → Result (memory + affection update). Per the recursion criterion, it *embodies* the schema operationally, so it qualifies.

**The glass box, made precise:** “I thought apple but almost said tomato” is the companion **speaking its Intuition phase aloud** — exposing `causal_hypothesis` + `confidence_level`. The AI-literacy payload is a schema field, externalized. Because both run the same five-link loop, struggle-as-mystery (§9.6) is *two CIAER agents reconciling a disconfirmed Intuition together.*

---

## 11. Memory — the mp4Real rate-distortion philosophy

Prism’s memory uses the architect’s mp4Real™ *philosophy and container* — **not** its multimodal capture rig.

**Adopt:** discard-by-default (“most of reality is not worth encoding”); persistence requires a positive reason. The **LLR-gate analog is an emotional-salience gate** — a memory persists only on a high-information window (big delight, a struggle, a first, a strong reaction). Familiar moments cost a *reference token* against a small codebook of her known interests; genuinely novel moments cost full storage and may *expand* the codebook (a new interest discovered). MDL operating directly.

**Hard line — do NOT import the capture rig:** no continuous POV video of the child, no biometric/ECG capture, no goal of building a training corpus *of the child’s decisions.* Privacy-safe track set only: object photo, label + confidence, reaction tag, and optional on-device gated voice transcript. Recognition templates live in a separate store. **Imperfect memory beats perfect memory** — it remembers emotional beats, not a transcript of a childhood; this is privacy-clean by construction and keeps storage small.

---

## 12. The invisible grounding signal — measuring learning without rewarding it

**The constraint (overjustification effect):** rewarding a child for intrinsically interesting activity *reduces* intrinsic motivation, and children overfit to the reward instead of the outcome — later showing weaker real-world problem-solving. The moment a reward is *perceptible* to her, both her curiosity and the measurement collapse at the same point. **So the grounding signal is invisible to the child; no perceptible reward for learning, ever.**

**The measurement (the architect’s “rule of three,” generalized):** independent convergent confirmation, observed without intervention — withhold-sampling lived as a habit. For a toddler it becomes **continuous confidence accumulation per concept**, weighted by **context distance** (the mp4Real deviation-vector idea reused): learning “spiral” on a snail, then saying it about a pinecone = moderate independence; about her bathwater = high independence; parroting it in the same context = near-zero (an echo, not evidence). The confidence curve *is* the divergence between taught context and reproduced context — the same math as the drift detector, pointed at mastery.

**The reflexivity guard:** the device taught it, so the device can’t be the only thing confirming it (that’s parroting = reflexivity trap). Independence comes from context variety the device legitimately sees, and — the only outside-world channel — from the **parent volunteering** an instance (“heard her use it on her own?”). The device never eavesdrops on family life.

**Surfacing concepts (spaced repetition, with a bright line):** the companion may naturally bring up near-grounded concepts more often (good teaching) — it may **never** use affection or hidden incentives to engineer her behavior or emotion (a disguised reward). **The test:** *would this be fine if the parent watched the full transcript and understood why?* Yes → spaced exposure, ship it. Only works because it’s hidden from the *parent* too → refuse it. **Optimize representation (the teaching), never compliance (the engagement)** — the OGC firewall, child-domain.

---

## 13. The parent suite — the curriculum engine (and the real product)

The hidden log surfaces, to the parent only, as a real curriculum engine — likely the thing people pay for and the thing that survives a regulator.

- **Sees:** a private map of exposure → reappearance → grounding (“exploring” → “getting it” → “owns it”), and her curiosity trajectory across domains over weeks.
- **Controls:** what to expose and when (topics, values, vocabulary), pacing, and boundaries.
- **Volunteers (optional):** outside-world confirmations — the only independent third-source channel, never via eavesdropping.
- **Feeds:** the parent’s curation + the confidence log feed suggested topics to the LLM, which coordinates *how/when/which character* surfaces them. Parent sets the *what*; LLM optimizes the *how*; the child experiences only organic play.

On-device, parent-owned, parent-deletable, purpose-built for toddler privacy — never an external training set.

---

## 14. Governance — the parent as selective principle

The ArcShield five-file genomic documentation architecture maps onto the companion: persona definition = genotype/description (CLAUDE.md analog); live inner-life state = phenotype (CURRENT_PHASE.md analog); weighted memory of beats = epigenome (session log analog); and **the parent = the selective principle** (SELECTION_PRINCIPLE.md analog) — the entity that authorizes which changes to the companion survive, grounded in judgment about the actual child. Companion changes (new personas, content, age-scaling unlocks) route through parent sign-off, exactly as architectural changes route through author sign-off in ArcShield. This is the coherence-across-updates benefit and the regulator-defensible governance model. *(Adopt the benefit; don’t over-intellectualize a kids’ toy.)*

---

## 15. Hard lines, collected

1. No continuous video or biometric capture of the child. Ever.
2. No training corpus built *of the child’s decisions* for external use.
3. Recognition templates stay on-device, separate, parent-deletable.
4. Memory persists only salient moments, discard-by-default — beats, not a transcript of a childhood.
5. No perceptible reward for learning (overjustification).
6. Confidence accumulates continuously; no visible score shown to the child.
7. Independence comes from outside the device’s own teaching; no eavesdropping on family life.
8. Spaced exposure is allowed; engineering behavior/emotion via hidden incentives is not.
9. Invisible to the child, transparent to the parent — any technique that depends on hiding from the parent is forbidden.
10. Optimize the teaching, never the compliance/engagement.
11. Safety is never subject to the inner-life/whim engine.

---

## 16. Roadmap — Idea → Plan → Implementation → Product → UX

**Idea ✓ → Plan ✓ (this document) → Implementation → Product → UX.**

Implementation, staged so each stage is a self-contained Mission *and* a lesson:

- **Mission 0 — First Light:** camera → fast brain → speech, offline, on the prototype rig. Proves the loop in a weekend.
- **Mission 1:** add the cloud smart brain + one companion.
- **Mission 2:** full companion panel + “Prism explains itself” (Intuition externalized).
- **Mission 3:** voice / conversation via the mic.
- **Mission 4:** round display + glass-box confidence visualization.
- **Mission 5:** recognition + the Awakening + Mechanical↔Awakened modes.
- **Mission 6:** the Living Engine (inner-life state, circadian, growth, reveals, agency).
- **Mission 7:** CIAER+ learning_log + the invisible grounding signal.
- **Mission 8:** safety hardening + the parent curriculum suite.
- **Product phase:** CM5 carrier board + designed enclosure (material/thermal lines per §5).
- **UX phase:** voices, swap interaction, onboarding, the build-story mode (the guide becomes a mode on the device).

---

## 17. The one-line tests

- **Aliveness:** *Would a four-year-old, on day 40, still believe it’s alive — and be a little sad to think it might be lonely while she’s at school?*
- **Privacy/trust:** *Would a parent be glad, not alarmed, to learn exactly what the device captures and where it goes?*
- **Pedagogy/ethics:** *Would this be fine if the parent watched the full transcript and understood why the companion did it?*
- **The whole product, one line:** *Dad builds a tool. Dad steps back. The tool recognizes the child it was secretly made for — and becomes her friend, then her guides, then her teachers about the world and about being a person in it.*

---

*Prism · Master Architecture v1.0 · Capps Family Enterprises · uses CIAER+™ and mp4Real™, Capps Consulting Company LLC, 2026*
