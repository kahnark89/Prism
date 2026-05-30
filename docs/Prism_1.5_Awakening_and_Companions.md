# Prism — Document 1.5: The Awakening & The Companions

*The emotional core of the product. This is the document that turns a smart camera into a thing a child believes in. It comes after the Plan and before the first build Mission, because every technical choice from here on serves this story.*

> **Read me first.** Everything in Document 01 (architecture, BOM, safety, age-scaling) still holds. This document doesn’t replace it — it gives it a soul, and adds one capability to the fast brain: **recognition.**

---

## 1. The arc — what actually happens to the child

The product is not handed over. It **awakens for her.** Four beats:

**Beat 1 — Daddy’s Project.**
For days/weeks she watches Dad tinker, the way he always does. The thing is real but *flat*: a plain, slightly mechanical voice that just recites facts about the objects she mentioned while “helping.” It is intentionally a little boring. It is a **tool.** (Meanwhile, every preference she let slip — purple, foxes, Grandma’s porch — was quietly captured. See the Build-Session Companion, Doc 02-pre.)

**Beat 2 — The Charge for Independence.**
Dad steps back. “Okay — you try holding it.” He hands it over. This is the hinge. The let-down of beat 1 was *necessary*: it’s the low note that makes the next beat soar.

**Beat 3 — The Awakening.**
The device recognizes her face and voice (pre-enrolled, see §3). The instant it’s sure it’s *her* and Dad has stepped back, it **transforms**: the LED ring blooms and swirls, gentle magical whirs and chimes, the flat voice falls away — and a **character comes to life.** The machine has become a friend. The story it tells, implicitly: *I was waiting for you. I know you.*

**Beat 4 — The Guided World.**
Her primary companion becomes her learning guide for everything curious in her world. Over days and weeks the other companions arrive — each with a personality, a strength, and a **dilemma** — each a living metaphor for one high-value life lesson. They model curiosity, but also friendship, patience, honesty, courage, and how to be wrong gracefully.

> **Kid Track:** She is never told any of this. She *discovers* it. The magic only works if it’s real to her.

---

## 2. Two modes — Mechanical Mode → Awakened Mode

The single device runs two distinct personas. The switch between them *is* the magic.

| | **Mechanical Mode** (Beat 1) | **Awakened Mode** (Beat 3+) |
|---|---|---|
| Voice | flat, neutral, robotic TTS | warm character voice, expressive |
| Content | bare facts: “Object: fox. Color: orange.” | playful, curious, in-character |
| Lights | minimal / static | LED ring blooms, swirls, reacts |
| Sound | none / beeps | magical whirs, chimes, character stings |
| Trigger | default state | **recognizes Naomi’s face + voice, dad-handoff sensed** |
| Purpose | be a believable “dad tool” | be her friend and guide |

Technically this is a persona/state switch in the existing `perspective_engine` + `audio_feedback`, gated by a new recognition signal. No new architecture — a new *state*. The flat mode is genuinely useful as the offline/degraded fallback too, so it’s not throwaway.

---

## 3. The recognition trigger — and why it’s safe enough to ship

**What it does:** Naomi’s face and voice are **enrolled once** (a short setup you run). The device stores them as **mathematical templates** — embeddings, not photos or recordings. In use, the fast brain checks, *on the device, offline*: “Is the face/voice I’m seeing a match for the enrolled child?” Yes → awaken. That’s it.

**The privacy architecture — this is what makes the magic legal and trusted:**

1. **On-device only.** Face/voice matching runs locally on the CM5/Pi. No video or audio of the child is sent anywhere for recognition.
2. **Templates, not media.** What’s stored is a vector (a list of numbers), not an image or clip. It can’t be played back or reconstructed into a photo/recording.
3. **No raw capture retained.** Recognition is a yes/no check in the moment; frames aren’t logged.
4. **Parent-enrolled, parent-deletable.** You enroll the child; you can wipe the template anytime from the parent dashboard.
5. **Recognition ≠ the cloud.** The *only* thing that ever touches the cloud is what Doc 01 §6 already allows (object label, photo of the object, short bounded questions) — never the recognition data.

> **Why this matters for “huge market presence”:** “A camera that watches my kid” kills the product. “A device that privately recognizes *my* child, on the device, and forgets everything else” is the trust story that sells it. Same magic to the child; opposite regulatory posture. Build it this way from day one — retrofitting privacy is how products die.

> **Kid Track:** “Prism *knows* it’s you. That’s why it wakes up for you and not for everybody.”

---

## 4. The Companions — characters as life lessons with dilemmas

The design rule that makes these teach instead of preach: **every companion has a dilemma — the very thing they’re still learning.** A perfect mascot teaches nothing. A friend who *struggles with the same thing the child struggles with*, and works through it in tiny visible moments, teaches everything. At ages 3–5 this is shown in 1–2 sentence beats, never lectured.

Each companion = **one strength + one dilemma + one life lesson + one lens on the world.**

*(Names/animals are placeholders — these get replaced with Naomi’s actual favorites, captured during the build. That personalization is the point.)*

| Companion | Strength (lens) | Dilemma (what they’re learning) | Life lesson it models |
|---|---|---|---|
| **The Curious One** (her primary guide) | wonder, “how does it work?” | sometimes touches things they shouldn’t; learning *when* to be careful | curiosity + good judgment together |
| **The Brave One** | courage, trying new things | still gets scared — bravery isn’t *not* being afraid | courage means doing it scared |
| **The Gentle One** | kindness, noticing feelings | so eager to help they forget to *ask* first | kindness includes consent and listening |
| **The Maker** | building, fixing, persistence | gets frustrated when things break; learning to try again | failure is part of making |
| **The Honest One** | truth, “I was wrong” | tempted to pretend they knew; learning admitting a mistake is strong | honesty, and being wrong gracefully |
| **Prism itself** | the AI’s own voice | *isn’t always sure* — “I thought it was X but almost said Y” | even smart things guess; uncertainty is okay |

**How they arrive:** the primary companion awakens first (Beat 3). The others enter over time — earned, seasonal, or unlocked by exploration — so the social world *grows*, modeling making friends and that different friends are good at different things. Their interactions with each other (and small disagreements they resolve kindly) become the etiquette/social-behavior curriculum you described.

**The writing discipline:** each companion is a metaphor, never a sermon. The Brave One never says “be brave” — it says *“ooh, that’s a little scary… let’s do it anyway, together,”* with a wobble in its voice. The lesson lives in the behavior, not the words.

---

## 5. What this changes in the build (forward pointers)

- **Fast brain gains a recognition model** — face embedding + speaker verification, on-device. Added as a Mission after the core loop works (doesn’t block First Light).
- **`audio_feedback` gains per-character voices** and a Mechanical↔Awakened mode flag.
- **`perspective_engine` gains the companion persona system** — each character a defined voice, strength, dilemma, and safe-content envelope.
- **Enrollment flow** — a parent setup step to register the child’s face/voice templates.
- **`learning_log`** now also tracks which companions she’s met and which lessons have surfaced — the parent can see the social/emotional arc, not just vocabulary.
- **Safety** extends to the companions: dilemmas are always resolved positively, never leave a small child on a scary or sad note.

---

## 6. The full arc, one line

*Dad builds a tool. Dad steps back. The tool recognizes the child it was secretly made for — and becomes her friend, and then her guides, and then her teachers about the world and about being a person in it.*

That’s the product. Everything else is engineering in service of that sentence.

---

**Next deliverable:** *Document 02-pre — The Build-Session Companion* (your private Pixel app: push-to-capture preferences + live, silent suggestions for sneaky elicitation and staging the beats), then *Document 02 — Mission 0: First Light* (the actual hands-on build).

---

*Prism · Document 1.5 of the build manual · The Awakening & The Companions*
