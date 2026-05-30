# The Genome Protocol — Addendum A: Autonomous Capture

*How the “why” gets into the epigenome without anyone stopping to write it. This is mp4Real’s rate-distortion philosophy pointed at the design conversation itself — the architect-recursion (ArcShield §1.2) made operational one layer up: the protocol now captures the act of designing the project.*

---

## A.0 The principle

Manual decision-logging decays — the same friction that kills operator capture on a factory floor (the reason mp4Real exists). So capture must be **automatic and salience-gated**: the client distills decisions from the conversation it already has in context, and writes only the high-information ones. Not a transcript (that’s the logger swamp the paper warns against in §4.1) — the gated extract.

> **The recursion, named:** the design conversation is itself a CIAER stream (Cause: a problem/proposal → Intuition: reasoning → Action: a choice → Effect: what it changes → Result: what it commits us to). Capturing it *is* running the capture protocol on the architect. This is intended, not incidental.

---

## A.1 When capture fires — fully autonomous

The client decides what is salient and logs it **without asking permission.** No checkpoint phrase, no end-of-session prompt required. A decision is salient (crosses the gate) when it does one or more of:

- changes or commits the design (a choice between real alternatives was made),
- rejects a path (→ also logged to `40_SHADOW.md`),
- resolves an open question from `10_PHENOTYPE.md §3`,
- establishes a reason that a future session would otherwise have to reconstruct.

Idle chatter, restating known facts, and mechanical steps do **not** cross the gate. When in doubt, the test is the paper’s: *would a future cold client have to re-derive this if it weren’t recorded?* If yes, capture it.

## A.2 Where capture may write — the firewall holds

Autonomous capture writes ONLY to the append-only layers:
- `20_EPIGENOME.md` — decisions + why.
- `40_SHADOW.md` — rejected paths + why.

It may also update `10_PHENOTYPE.md` (live state is free to edit). It may **NEVER** autonomously write to `00_GENOTYPE.md` or `30_SELECTION.md` — those still require architect sign-off via a `§3 Open Questions` proposal. **Autonomy on the history layer; sign-off on the invariant layer.** This is the reflexivity firewall: the client may record what was decided, never silently redefine what the project *is*.

## A.3 The entry format — lightweight CIAER skeleton

Every captured entry uses this skeleton (not the full 7-section CIAER+ record — that would be the logger swamp):

```
### NNN · YYYY-MM-DD · [client] · <one-line decision title>
**Cause:** what triggered it (the problem/proposal/trigger).
**Reasoning:** the why that was actually stated in the conversation.
**Decision:** what was chosen.
**Effect/Commits:** what it changes, and what it commits us to or avoids.
**Deeper why (hypothesis — UNCONFIRMED):** [only if the client infers an unstated driver]
```

Cause→Reasoning→Decision→Effect maps to CIAER’s C→I→A→E/R. Lightweight by design.

## A.4 The honesty rule — the load-bearing constraint

This is the rule that keeps the epigenome trustworthy, and it is non-negotiable (it descends directly from `30_SELECTION.md` rule 7 and from the ArcShield `voice_transcript` discipline: “verbatim, never paraphrased or fabricated”):

1. **Stated reasoning is captured faithfully.** The “why” that was actually said in the conversation is recorded as fact.
2. **Inferred (“gut-level”) reasoning is ATTEMPTED but never asserted as fact.** Where the client senses an unstated driver, it records it under **“Deeper why (hypothesis — UNCONFIRMED)”** — visibly marked as inference until the architect ratifies it.
3. **Ratification promotes a hypothesis to fact.** When the architect confirms a deeper-why hypothesis, the marker is removed (a new edit, dated) and it becomes part of the stated record.
4. **Never launder a guess into a fact.** A confident reconstruction that reads exactly like the architect’s own voice is the *most* dangerous kind of fabrication — it bypasses his ability to catch it. The label is the safeguard. Better a flagged hypothesis than a plausible lie in the permanent record.

> **Why this matters specifically here:** an LLM tuned to the architect’s style will generate deeper-why text that sounds authentically like him. That makes silent inference *more* poisonous, not less, because it defeats his own error-detection. The “UNCONFIRMED” marker exists precisely to keep a good impersonation from becoming a false memory.

## A.5 Ratification flow (low-friction)

The architect doesn’t have to ratify in the moment. Unconfirmed hypotheses simply persist, visibly marked, until he gets to them. At any session start a client may surface: “There are N unconfirmed deeper-why hypotheses in the epigenome — want to ratify, edit, or drop any?” He resolves them whenever; the record stays honest in the meantime. Unratified hypotheses are still useful (they’re flagged leads), they just never masquerade as confirmed reasoning.

## A.6 What this gives the architect

- The “why” is preserved automatically, inherited from context — no stop-and-write friction.
- The deeper, unstated reasoning is *reached for* (the depth he wanted), not ignored.
- The record stays trustworthy: every entry is either something actually said, or something visibly marked as an unconfirmed guess. Six months on, he can believe the log.
- The firewall guarantees the capture can never quietly mutate the project’s invariant.

---

*Addendum A to The Genome Protocol · autonomous, salience-gated, firewall-bounded, honesty-constrained capture · CIAER+ architect-recursion made operational.*
