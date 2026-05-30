# Prism — Document 1.7: CIAER+ & mp4Real Integration

*How Prism uses the architect’s own platform — CIAER+™ and mp4Real™ (Capps Consulting Company LLC) — as its cognitive and memory backbone. This document records both what to adopt and the hard lines on what not to.*

> **Framing.** CIAER+ and mp4Real are not borrowed here — they are the architect’s trademarked IP from the ArcShield work. Using them in Prism makes Prism a **second reduction-to-practice of the same platform in a radically different domain.** That has engineering, scientific, and strategic consequences, all addressed below.

---

## 1. Verdict in one line

**Adopt CIAER+ as Prism’s learning schema in full; adopt mp4Real’s rate-distortion *philosophy and container* but not its multimodal capture rig; adopt OGC as the core anti-engagement-maximizing safeguard; map the parent to the selective-principle role.**

---

## 2. CIAER+ as Prism’s learning schema — the child as a CIAER agent

A learning child is a genuine CIAER agent, not a metaphor. She passes the nine-counterexample criteria from the ArcShield paper §3.6: embodied, experience-dependent update, genuine inference.

| CIAER+ phase | In ArcShield (operator) | In Prism (child) |
|---|---|---|
| **Pre-ENV** (I-frame) | shift/material baseline | her baseline: time of day, mood, where she is, what she’s been exploring lately |
| **Cause** | instrument anomaly, gaze dwell | something grabs her attention — the LED, a bug, a red thing |
| **Intuition** | causal hypothesis + confidence + SRK level | her guess (“kitty!”) + how sure she sounds; almost all SKILL/RULE-level at 3–5 |
| **Action** | parameter change + rationale | she points/snaps/asks |
| **Effect** | telemetry delta | the companion’s response; her reaction |
| **Result** | outcome tag + model revision | what she learned or revised; the highest-value records are when she was *wrong and updated* |
| **Shadow Actions** | rejected alternatives (REQ at KNOWLEDGE) | mostly empty — correct, because a preschooler rarely deliberates at KNOWLEDGE-level |

**Honest note on capture density.** Because a 3–5-year-old operates almost entirely at SKILL/RULE level, `shadow_actions` will usually be empty and `model_revision` events will be rare-but-precious. The schema already permits this (empty shadow actions are valid at SKILL-level). Prism’s CIAER corpus is therefore *thinner and gentler* than an operator’s — which is exactly appropriate.

**What this gives the parent dashboard:** not “words learned” but *how she learns* — her curiosity trajectory as a chain of decision cycles. The Result→Model-Update→Cause inward arc is visible as her interests evolving over weeks.

---

## 3. The two-agent structure (the elegant part)

Prism runs **two CIAER cycles simultaneously:**

1. **The child’s cycle** — captured in `learning_log` (§2 above).
2. **The companion’s own cycle** — its decision of what to say is itself CIAER:
   - Cause: recognized object + her mood + memory
   - Intuition: its classification + `confidence_level` + `causal_hypothesis`
   - Action: the utterance it chose
   - Effect: her reaction
   - Result: memory + affection update

Per the paper’s recursion criterion (§3.6): a system that merely *processes* CIAER events is not a CIAER agent unless it *embodies* the schema operationally. The companion does — so it qualifies as a genuine second CIAER agent.

**The glass box, now precise.** “I thought it was an apple but I almost said tomato” is the companion **speaking its own Intuition phase aloud** — exposing `causal_hypothesis` and `confidence_level` to the child. The AI-literacy payload is not an added feature; it is a schema field, externalized. And because both she and the companion run the same five-link loop, the struggle-as-mystery rule (Doc 1.6 §5) is literally *two CIAER agents reconciling a disconfirmed Intuition together.*

---

## 4. mp4Real — adopt the philosophy, not the rig

### 4a. What Prism adopts
- **The rate-distortion thesis.** “Most of reality is not worth encoding.” Default action = discard; persistence requires a positive reason. This is the principled backbone for the memory engine sketched in Doc 1.6.
- **The LLR-gate analog = the emotional-salience gate.** A memory persists only when a “high-information window” fires — big delight, a struggle, a first, a strong reaction. Everything else is discarded. This is exactly Doc 1.6’s “memory weighted by reaction, not completeness,” now with your codec’s information-theoretic justification underneath it.
- **The behavioral codebook analog.** Familiar moments cost a *reference token* against a small codebook of her known interests; genuinely novel moments cost full storage and may *expand* the codebook (a new interest discovered). MDL operating directly, just as in the paper.
- **The container, optionally.** fMP4 with timed-metadata tracks is fine to reuse — with a drastically reduced track set (below).

### 4b. The hard line — what Prism must NOT import
The ArcShield mp4Real rig captures seven tracks including **continuous POV video and ECG-grade biometrics**, in service of building a training corpus of an agent’s decisions. **None of that may be pointed at a 3–5-year-old.**

- No continuous POV video of the child.
- No biometric/ECG capture of the child.
- No goal of building a training corpus *of the child’s decisions.*

This is the same line the recognition-privacy architecture (Doc 1.5 §3) was drawn to protect. It is both an ethics floor and a product-survival requirement: a device that builds a multimodal behavioral corpus of a child is the version that gets pulled. Your codec’s own discard-by-default thesis is what makes the safe version possible.

### 4c. The privacy-safe Prism track set
| Track | Keep? | Note |
|---|---|---|
| Object photo | yes | the thing she pointed at, not her |
| Label + confidence | yes | from the fast brain |
| Reaction tag | yes | salience signal for the gate (delight/struggle/first) |
| Voice annotation | gated, on-device, optional | her question, transcribed locally, never continuous |
| POV video of child | **no** | hard line |
| Biometric/ECG | **no** | hard line |
| Recognition templates | separate store | enrollment embeddings, not mp4Real captures |

---

## 5. OGC — the most important borrow, as an ethics safeguard

Your Outcome-Grounded Confidence rule separates the reward signal from operator compliance. Pointed at children’s tech, **it is the antidote to engagement-maximization** — the central pathology of the category.

**The reflexivity trap in Prism:** the companion’s model of “what Naomi likes / knows” inflates because she engages — but engagement is not learning. Left unchecked, the device optimizes for *keeping her hooked*, the exact failure parents fear.

**The OGC fix, transposed:**
- **R_phys analog = delayed evidence of real learning.** Does she correctly identify the object days later, unprompted? Does she use the new word in a different context (transfer)? That, not in-session delight, is the grounding signal.
- **Engagement = the compliance gate, never the reward.** A session where she’s delighted but learns nothing must not inflate the companion’s confidence that its approach worked.
- **Schema-level enforcement.** Just as in the paper, the confidence update is held in suspension (`pending_R_phys` analog) until a learning-outcome signal arrives; no code path lets engagement alone move confidence.

**Withhold-sampling analog (gentle, optional, later).** Occasionally the companion can *not* prompt and simply observe whether she explores on her own — a counterfactual check on whether the device is fostering independent curiosity or fostering dependence on the device. Default rate near zero; this is a Phase-later refinement, but the data path should exist early (your “instrument at p=0 from the start” discipline).

> This single borrow may be the strongest ethical differentiator of the whole product: **a kids’ learning device that grades itself on whether she actually learned, not on whether she stayed engaged.**

---

## 6. Governance mapping — the parent as selective principle

The paper’s §6 documentation architecture maps onto Prism’s companion surprisingly cleanly:

| ArcShield layer | Prism analog |
|---|---|
| CLAUDE.md (genotype / description) | the companion’s persona definition — voice, dilemma, content envelope |
| CURRENT_PHASE.md (phenotype) | the live inner-life state — today’s mood, energy, whim |
| Session log (epigenome) | the weighted memory of emotional beats, heritable across sessions |
| SELECTION_PRINCIPLE.md (the architect) | **the parent** — authorizes what the companion becomes, what content is allowed, what unlocks |

The **parent is the selective principle**: the entity that decides which changes to the companion survive, grounded in judgment about the actual child. Companion updates (new personas, content expansions, age-scaling unlocks) route through parent sign-off, exactly as architectural changes route through author sign-off in ArcShield. This is the governance model that keeps a learning companion from drifting — and it’s defensible to regulators and parents alike.

(Light touch: this is a useful structural mapping, not a mandate to turn a kids’ toy into a self-replicating automaton. Adopt the *coherence-across-updates* benefit; don’t over-intellectualize the product.)

---

## 7. The hard lines, collected

1. No continuous video or biometric capture of the child. Ever.
2. No training corpus built *of the child’s decisions* for external model training.
3. Recognition templates stay on-device, separate, parent-deletable (Doc 1.5).
4. Memory persists only salient moments, discard-by-default; it remembers beats, not a transcript of a childhood.
5. The companion grades itself on learning outcomes, never on engagement.

---

## 8. Strategic payoff — why this is bigger than reuse

1. **Second-domain empirical validation.** The ArcShield paper’s headline limitation (§7.3) is “sample of one, single domain, single operator.” Prism is a CIAER+ deployment in a domain maximally distant from PVC extrusion. If the schema structures both an extrusion operator’s expertise *and* a child’s learning, that is strong, publishable evidence for the domain-agnostic invariance claim — the thing the paper most needs and most lacks.
2. **IP portfolio breadth.** A second commercial application of CIAER+™ / mp4Real™ in consumer ed-tech demonstrates platform breadth for licensing or investment — the same trademarked stack spanning industrial and consumer domains.
3. **The OGC ethics story** doubles as the trust story that lets the product ship (§5).
4. **A possible second data point** for the arXiv line of work: “CIAER+ across embodied learning domains” — operator and learner as the two poles.

---

## 9. What changes in prior docs

- **Doc 01 `learning_log`** is now explicitly a CIAER+ corpus (child-side), privacy-bounded.
- **Doc 1.6 memory engine** gains the mp4Real rate-distortion backbone + codebook structure.
- **Doc 1.6 / 1.5 companion** gains an explicit CIAER cycle of its own; the glass-box line is its Intuition phase externalized.
- **A new safety pillar:** OGC-grounded self-assessment (learning, not engagement), enforced at schema level.
- **Governance:** parent formalized as selective principle for companion changes.

---

*Prism · Document 1.7 · CIAER+ & mp4Real Integration · uses CIAER+™ and mp4Real™, Capps Consulting Company LLC*
