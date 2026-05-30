# Prism — Document 1.8: The Invisible Grounding Signal

*How Prism knows whether the child actually learned — without her ever knowing it’s measuring, and without ever rewarding her for learning. This is the R_phys analog for the child domain, and it is built on a hard psychological constraint.*

> **The architect’s own contribution:** the “rule of three” — independent convergent confirmation, observed without intervention — is the seed of this entire mechanism. It is withhold-sampling (ArcShield §5.4) lived as a habit: a signal only counts when the observer did not cause it.

---

## 1. The governing constraint: the overjustification effect

Research on the **overjustification effect** (Lepper, Greene & Nisbett 1973, and the large literature since) establishes that rewarding a child for an activity they already find intrinsically interesting *reduces* their intrinsic motivation for it. The reward becomes the point; the activity becomes a means. Worse, children overfit their causal associations to the *reward* rather than the *outcome* — and later show weaker real-world problem-solving, because everyday life does not dispense rewards for solving everyday problems.

**This is the same pathology OGC prevents, one layer deeper.** OGC stops the *device* from grading itself on engagement. The overjustification constraint stops the *child* from ever perceiving a reward signal at all. If she detects that learning earns something — points, praise-as-currency, the companion’s approval-as-reward — she optimizes for the signal, and that simultaneously:
1. corrupts her intrinsic curiosity (the pedagogical failure), and
2. corrupts the measurement (the prompted instance is no longer independent).

The pedagogy and the measurement fail at the *same point* for the *same reason*. So:

> **Hard rule: the grounding signal is invisible to the child. There is no perceptible reward for learning. Ever.**

The only place visibility and control live is the **parent** side.

---

## 2. The measurement: continuous accumulation, not a threshold

We drop the hard “count to 3.” The rule of three is the architect’s adult-world calibration (many independent sources, long patience). For a 3–5-year-old it becomes a **continuously accumulating confidence per concept**, where each unprompted reappearance adds confidence weighted by its independence.

### 2.1 Context-distance weighting (this is mp4Real’s deviation vector, reused)
Not all reappearances are worth the same:

- She learned “spiral” on a **snail**. She later says “spiral” about a **pinecone** → moderate independence, moderate confidence gain.
- She says “spiral” about the **swirl in her bathwater** (a context the device never taught) → high independence, high confidence gain.
- She parrots “spiral” back in the *same* snail context, same session → near-zero independence, near-zero gain (it’s an echo, not evidence).

This is exactly the codebook deviation-vector logic from the ArcShield paper §4.6: a reproduction *close* to the taught context costs little and confirms little; a reproduction *far* from it is high-information. **The confidence curve for a concept is the divergence between the taught context and the reproduced contexts.** Same math as the drift detector, pointed at mastery instead of drift.

### 2.2 Age- and skill-appropriate reproduction
“Reproduction” is judged at her level. For a 3-year-old, pointing and naming counts. For a 5-year-old, using the concept in a sentence or applying it to a novel object counts. The bar rises automatically with the age-scaling spine (Doc 01 §2, principle 5). The hidden log tracks *exposure → reproduction* per concept at the right granularity for her.

### 2.3 The reflexivity guard (the toddler-scale trap)
The device taught the concept. If the device is *also* the only thing listening for it to return, instances 2 and 3 are echoes of instance 1 — the child parroting the device back to the device. That is not convergence; it is the reflexivity trap in miniature. **Independence must come from outside the device’s own teaching.** Two legitimate sources:
- **Context variety the device legitimately observes** (snail → pinecone → bathwater, all seen through Prism). Weak-to-moderate independence; usable.
- **The world outside the device** (she says it to a parent, the device nowhere in the loop). Strong independence — but the device must NOT listen for this (privacy line). It enters only if the parent volunteers it (§3).

---

## 3. The Parent Curriculum Suite (a core product surface — possibly *the* product)

The hidden log surfaces, to the parent only, as a real curriculum engine. This is what parents pay for and what makes the product defensible.

**What the parent sees:**
- A private map of what she’s been exposed to, what has reappeared, and how grounded each concept is (the accumulating confidence, in plain language: “exploring” → “getting it” → “owns it”).
- The *trajectory* — her curiosity moving across domains over weeks (the CIAER Result→Cause arc, Doc 1.7 §2).

**What the parent controls:**
- **What to expose and when** — topics, themes, values, vocabulary. The parent curates her world.
- **Pacing** — push more, hold back, let an interest breathe.
- **Boundaries** — what’s off-limits, what waits for a later age.

**What the parent can volunteer (the only outside-world channel):**
- A gentle, optional, *non-nagging* prompt: “Have you heard her use ‘spiral’ on her own?” Tap yes → the parent becomes the independent third source. The device never eavesdrops on family life; the parent contributes it deliberately. Default to minimal friction; a parent who never engages this still gets a working product from context-variety alone.

**What this feeds:** the parent’s curation + the confidence log feed **suggested topics** to the LLM agent, which then coordinates *how, when, and through which character* to surface them — based on its memory and personal history with her. Parent sets the *what*; the LLM optimizes the *how*; the child experiences only organic play.

All of this lives in a corpus **purpose-built around toddler privacy and safety** — on-device, parent-owned, parent-deletable, never a child-behavior training set for external use (Doc 1.7 §7 hard lines hold).

---

## 4. Surfacing concepts: the injection mechanism and its bright line

The companion may surface near-grounded concepts more often in natural play to boost the learning curve — *without the child detecting intent*. This is legitimate and powerful **when it stays on the right side of one line.**

### 4.1 The good version: spaced repetition through natural exposure
This is what every skilled early-childhood teacher does: notice a child is *close* on a concept, then naturally bring it up more over the following days — in stories, in what gets pointed out, in established dialogue — without announcing “we’re practicing.” Responsive instruction, not manipulation. Encoding this so the companion organically increases exposure to concepts in the “getting it” band is core pedagogy. **A noticeable, gentle drift in what the characters talk about is fine.**

### 4.2 The bright line: shape the offering, never engineer the child
- **Allowed:** change *what the world offers her* — mention spirals more this week, weave the target concept into a story, have a character be “into” it today (ties to the daily-whim engine, Doc 1.6 §1.3). The content shifts; her autonomy doesn’t.
- **Refused:** use the companion’s affection, emotional cues, or any hidden incentive to *steer her behavior or feelings* toward something she wouldn’t choose. That is a hidden reward in disguise — the exact thing the overjustification research warns against. It corrupts motivation and measurement together.

### 4.3 The test (the architect’s “represent without her picking up the reasoning,” made operational)
> **Transparency asymmetry: invisible to the child, fully transparent to the parent.**
> Would this be fine if the parent watched the full transcript and understood exactly why the companion did it?
> - **Yes** → it’s spaced exposure. Ship it.
> - **It only works *because* it’s also hidden from the parent** → refuse it. Any technique whose efficacy depends on the *parent* not understanding it is manipulation, not teaching.

The child not seeing the mechanism is pedagogy. The parent not seeing it would be deception. The first is required; the second is forbidden.

---

## 5. The representation-vs-influence firewall (OGC, one more time)

The “lightweight informal test” of how to present information has a clean, safe form and a forbidden form — and it’s the same firewall as the whole platform:

- **Allowed — optimize representation (the teaching):** A/B whether “spiral” lands better via a story vs. a snap vs. a song. The target (her learning) is fixed; only the *medium* varies. This is pedagogy optimization.
- **Forbidden — optimize influence (the compliance):** A/B *how hard to push* her, how to maximize her engagement or time-on-device. That optimizes the compliance signal, which OGC forbids reading as reward (Doc 1.7 §5).

Same column-separation as R_phys vs. compliance in the ArcShield paper: **optimize the teaching, never the compliance.** Enforced as a design invariant, not a guideline.

---

## 6. How it all runs, end to end

1. Companion teaches “spiral” (snail). Logged: *exposed, ungrounded.* Confidence in suspension.
2. Over days, she reappears with it in new contexts the device sees. Confidence accumulates, weighted by context distance (§2.1). No acknowledgment to her — ever (§1).
3. Parent optionally confirms an outside-world use → strong independent signal (§3).
4. Confidence crosses into “owns it” (continuous, no visible threshold). Concept becomes a foundation the companion can build upward from — and only now (mastery-gated progression).
5. Parent suite shows the trajectory; parent curates what’s next; LLM coordinates organic surfacing within the bright lines (§4).
6. The child experiences only: a friend who plays, wonders, and happens to be curious about the same things she’s becoming curious about. No tests. No rewards. No visible machinery.

---

## 7. Hard rules, collected

1. The grounding signal is **invisible to the child**; no perceptible reward for learning (overjustification).
2. Confidence **accumulates continuously**, weighted by context-distance independence; no visible threshold or score shown to the child.
3. Independence must come from **outside the device’s own teaching**; the device never eavesdrops on family life — the parent volunteers outside-world instances.
4. The companion may **surface concepts via natural exposure** (spaced repetition); it may **never** use hidden incentives to engineer behavior or emotion.
5. **Transparency asymmetry:** invisible to the child, fully transparent to the parent. Any technique that depends on hiding from the *parent* is forbidden.
6. **Optimize representation, never compliance.** (OGC, child-domain.)
7. The exposure/reproduction corpus is **on-device, parent-owned, parent-deletable**, purpose-built for toddler privacy — never an external training set.

---

*Prism · Document 1.8 · The Invisible Grounding Signal · builds on CIAER+™ / OGC / withhold-sampling, Capps Consulting Company LLC*
