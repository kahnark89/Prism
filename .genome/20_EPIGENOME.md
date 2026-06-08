# 20_EPIGENOME — Prism (append-only decision history)

> Append-only. Never rewrite or delete entries; corrections are new entries that reference the old. Each entry: what was decided, why, what it avoids. This is the layer that stops settled questions from reopening.

---

### 001 · 2026-05-29 · Reframe from "Dex clone" to "glass box, multi-POV"
**Decided:** Prism's differentiator is multiple points of view + showing the AI's uncertainty, vs. Dex's single-answer black box.
**Why:** A single-discipline translator is a crowded space; the glass-box + multi-lens framing is both more pedagogically valuable and more defensible.
**Avoids:** Building a me-too product with no moat.

### 002 · 2026-05-29 · Hybrid edge+cloud is mandatory, and is itself the curriculum
**Decided:** Small on-device classifier (fast brain) + cloud LLM (smart brain); the contrast is the headline AI lesson.
**Why:** No on-device chip runs a multimodal LLM; the constraint becomes the teaching point (offline/fallible vs. online/smart).
**Avoids:** Pretending a Pi can run an LLM; missing the best teachable moment.

### 003 · 2026-05-29 · Ages 3–5 as primary, with age-scaling to ~12
**Decided:** Design for pre-readers (audio/image-first, 3 characters not 7), software scales depth with age.
**Why:** Reading level + safety posture + attention span all flow from age; scaling makes it a "learning careers" arc, not a toy.
**Avoids:** A device that's wrong for the actual user and obsolete in a year.

### 004 · 2026-05-29 · The Awakening — device recognizes the child and transforms
**Decided:** Mechanical Mode (dad's flat tool) → Awakened Mode (alive companion) triggered by on-device face/voice recognition + dad handoff. The disappointment beat is necessary setup.
**Why:** "It chose her" is a once-in-a-childhood emotional hook; the let-down makes the awakening land.
**Avoids:** A device that's merely handed over (no magic, no bond).

### 005 · 2026-05-29 · Recognition is on-device, template-only, parent-deletable
**Decided:** Face/voice stored as embeddings (not media), matched locally, never sent to cloud, parent-wipeable.
**Why:** "A camera watching my kid" kills the product; private on-device recognition sells it. Same magic, opposite regulatory posture.
**Avoids:** The single subsystem most likely to get a kids' product pulled.

### 006 · 2026-05-29 · Companions are life-lesson metaphors WITH dilemmas
**Decided:** Each character = one strength + one dilemma (the thing they're still learning) + one life lesson + one lens. Shown, never lectured.
**Why:** A flawed friend who struggles with what the child struggles with teaches; a perfect mascot doesn't.
**Avoids:** Preachy, inert mascots.

### 007 · 2026-05-29 · Aliveness = engineered inner life, not a smarter LLM
**Decided:** Inner Life Engine (mood/energy/curiosity/affection/daily-whim + circadian rhythm + slow growth + bounded unpredictability), on-device, persistent, conditions the LLM. Priority: Inner Life > Memory > Reveals > Agency. Framed as EdgeState inverted.
**Why:** Consistency is the enemy of aliveness; the soul is the state engine (ownable), the LLM is a rented mouth.
**Avoids:** A consistent, always-available chatbot that feels dead by day 40.

### 008 · 2026-05-29 · Struggle → mystery is the top behavior rule
**Decided:** When the child is wrong/stuck, the companion turns it into a shared mystery — never corrects, cheers, or lectures.
**Why:** Removes the sting of being wrong, models curiosity-as-response-to-not-knowing, and makes the AI's own uncertainty a shared adventure. Emotional safety and AI-literacy become the same rule.
**Avoids:** Shame around being wrong; wasting the glass-box lesson.

### 009 · 2026-05-29 · Adopt CIAER+ as the schema (both child and companion as agents)
**Decided:** Prism runs on CIAER+; child = one CIAER agent (learning_log), companion = a second. Makes Prism a second-domain reduction-to-practice of CIAER+.
**Why:** Unifies learning + companion reasoning under one grammar; strengthens the paper's domain-invariance claim; broadens IP.
**Avoids:** Ad-hoc logging; a wasted chance to validate the architect's own platform.

### 010 · 2026-05-29 · mp4Real philosophy yes, capture rig NO
**Decided:** Use discard-by-default rate-distortion + codebook for memory; reject the 7-track POV-video + ECG rig for a child.
**Why:** The philosophy is exactly right for "remember the moments that mattered"; the rig is the thing that sinks a kids' product.
**Avoids:** Building a multimodal behavioral corpus of a child (ethics + regulatory landmine).

### 011 · 2026-05-29 · OGC repurposed as the anti-engagement-maximizing safeguard
**Decided:** Device grades itself on learning outcomes (delayed/transfer), never on in-session engagement; reward separated from compliance.
**Why:** Engagement-maximization is the central pathology of kids' tech; OGC is the architectural antidote and the trust story.
**Avoids:** Another dopamine-loop toy.

### 012 · 2026-05-29 · Grounding signal is invisible to the child (overjustification)
**Decided:** No perceptible reward for learning. Confidence accumulates continuously, weighted by context-distance (mp4Real deviation-vector reuse). Independence from outside the device's teaching; parent volunteers outside-world instances (no eavesdropping).
**Why:** A perceived reward corrupts both intrinsic motivation AND the measurement at the same point (overjustification effect).
**Avoids:** Teaching her to perform for rewards; poisoning the measurement.

### 013 · 2026-05-29 · Spaced exposure allowed; behavior-engineering forbidden. The transparency asymmetry.
**Decided:** Companion may surface near-grounded concepts more in natural play (good teaching); may NOT use affection/incentives to steer behavior/emotion. Test: fine if the parent saw the full transcript and understood why? Optimize representation, never compliance.
**Why:** Invisible-to-child is pedagogy; invisible-to-parent is deception. The asymmetry is the safety model.
**Avoids:** Manipulation disguised as teaching.

### 014 · 2026-05-29 · Parent suite is a core product surface (possibly THE product)
**Decided:** Child-invisible, parent-governed curriculum engine: exposure→reappearance→grounding map, curation controls, optional outside-world confirmation, feeds topics to the LLM.
**Why:** It's what people pay for and what survives a regulator; the selective-principle role made concrete.
**Avoids:** A toy with no parent value and no defensibility.

### 015 · 2026-05-29 · Genome Protocol = ArcShield genomic architecture + AGENTS.md
**Decided:** Cross-session/client/platform coherence via the architect's own genotype/phenotype/epigenome/selection layering, wrapped in the AGENTS.md open standard, in git, with the reflexivity firewall (no client edits genotype/selection without sign-off).
### 016 · 2026-05-29 · Claude (web) · Autonomous salience-gated capture with honesty firewall
**Cause:** Architect wanted the "why" preserved automatically, inherited from context, without stop-and-write friction; noticed this "breaks the fourth wall."
**Reasoning (stated):** Manual logging decays (same friction mp4Real exists to defeat). The design conversation is itself a CIAER stream, so auto-capturing it is the architect-recursion (ArcShield §1.2) one layer up. But naive "log everything" = the logger swamp the paper rejects (§4.1); capture must be salience-gated like the LLR gate. Architect initially wanted silent gut-level inference of unstated reasoning; Claude pushed back because that violates SELECTION rule 7 (honesty/no fabrication) and the voice_transcript discipline — and is especially dangerous because an LLM tuned to the architect's voice produces fabrications he can't catch. Architect agreed.
**Decision:** Fully autonomous capture, firing without permission, writing only to append-only layers (epigenome/shadow) — never genotype/selection. Lightweight CIAER skeleton (Cause/Reasoning/Decision/Effect). Stated why captured as fact; inferred why recorded ONLY as "Deeper why (hypothesis — UNCONFIRMED)" until architect ratifies.
**Effect/Commits:** Every client now self-logs (AGENTS.md updated). The epigenome stays trustworthy by construction — entries are either stated-fact or visibly-marked-guess. Commits us to the ratification flow (A.5) and to never laundering a guess into a fact.
**Deeper why (hypothesis — UNCONFIRMED):** The architect's pull toward auto-inheriting the gut-level why may be less about logging convenience and more about a recurring instinct across his work — building systems that capture tacit knowledge the expert *can't articulate in the moment* (Polanyi's "we know more than we can tell," the founding premise of ArcShield). He may want the epigenome to capture *his own* tacit reasoning the way ArcShield captures an operator's. If so, the honest-hypothesis mechanism is exactly the right tool: it reaches for the tacit layer without fabricating it. — *Confirm / edit / drop?*

### 017 · 2026-05-29 · Claude (web) · Inner Life Engine = coupled damped dynamical system, not independent sliders
**Cause:** Architect chose to fully brain out the inner-life state engine ("the most curious of them all").
**Reasoning (stated):** Aliveness must feel spontaneous-not-random and recognizable-not-predictable; these tensions resolve via a layered system across timescales (traits/mood/reaction/rhythm). Three traps identified and designed against: (1) independent sliders produce noise not a creature → solved by a coupling matrix where Energy is the master variable dragging the others; (2) state that causes nothing observable is dead weight → solved by an explicit expression layer (voice pace/pitch, question rate, LED hue/motion, callbacks) mapping every variable to something a 4-yo can perceive; (3) unbounded drift dies at the walls → solved by a homeostatic restoring force (λ) pulling toward rhythm-shifted baselines. Unpredictability is bounded + autocorrelated noise plus a curated structured-surprise set, never freeform, never touching safety. Growth = slow one-directional trait drift over months (affection rises with time together; dilemmas slowly resolve). Only a compiled natural-language "mood-line" crosses to the LLM; the engine itself is deterministic + bounded-stochastic and runs on-device (aliveness is local).
**Decision:** Adopt the layered coupled-damped-system architecture (Doc 2.0). Built a live tunable React simulator (inner_life_simulator.jsx) implementing the real equations so the architect can feel-tune λ, rhythm depth, coupling, and noise by watching a day unfold.
**Effect/Commits:** Establishes the soul layer as ownable software (not the LLM). Commits to: Energy-as-master coupling, mandatory expression mappings, homeostatic stability, safety locked out of the whim/noise budget, and the mood-line as the sole LLM interface. Resolves nothing in the genotype (no sign-off needed — this is design depth within existing principles 7 & "aliveness is local").
**Deeper why (hypothesis — UNCONFIRMED):** The architect's repeated reach for dynamical-systems framings (Navier-Stokes, Market Manifold, fluid sims) suggests he may find a system *more* trustworthy when it's expressed as coupled differential-style dynamics with attractors than as rules/heuristics — i.e. he wants the soul to be a *physics* he can tune, not a script he has to author. If so, the simulator (not just the spec) is the real deliverable here. — *Confirm / edit / drop?*

### 018 · 2026-05-29 · Claude (web) · Memory Engine = decaying self-curating graph; forgetting is the feature; memory-rehearsal ≡ the learning signal
**Cause:** Architect chose to keep deepening the magic; memory selected as highest-leverage (ranked #2, sustains aliveness past day 1, compounds with the inner-life engine).
**Reasoning (stated):** The real problem is graceful *forgetting*, not remembering (mp4Real discard-by-default pointed at a relationship). Episodic nodes with salience-weighted exponential decay; a salience gate (the LLR analog) admits only emotionally-salient beats; rehearsal/reconsolidation refreshes strength and lengthens τ on reactivation, so the relationship self-curates — what she keeps engaging with is what survives. Key unification discovered: a concept-memory consolidating via context-spread rehearsal is the SAME event as the Doc 1.8 grounding signal accumulating — the companion remembering and the child learning are mechanically identical, so the grounding signal is just a *view* over the memory graph (don't build two systems). Gist-vs-verbatim split: episodes (verbatim, short τ) prune away while the codebook/interest-profile (gist, long-lived) persists — which is simultaneously the most human-feeling design AND the privacy guarantee (the device structurally cannot become a dossier; specifics decay by physics). Memory mass feeds the inner-life Affection baseline (the compounding). Same decay+kick dynamics as Doc 2.0 → one shared integrator, on-device, offline-capable.
**Decision:** Adopt the decaying self-curating graph architecture (Doc 2.1). Built a live tunable memory simulator (memory_simulator.jsx) — encode a stream of moments, fast-forward weeks, watch the right things survive and gist crystallize as specifics fade.
**Effect/Commits:** Commits to: forgetting-by-default, salience-gated encoding, rehearsal-driven self-curation, grounding-signal-as-view-over-memory (one mechanism), gist-persists/verbatim-fades as both feel and privacy, and memory→affection compounding. Establishes "the companion is one dynamical organism: a mood field atop a memory graph." No genotype change (depth within existing principles + hard lines).
**Deeper why (hypothesis — UNCONFIRMED):** The architect's instinct to make forgetting central (rather than treating memory as accumulation) may track the same value that drives his OGC/overjustification thinking — a deep preference for systems whose *integrity is structural rather than policed*. Forgetting-as-physics means privacy can't be violated even in principle, the way OGC means engagement-gaming can't inflate confidence even in principle. He may be consistently drawn to designs where the safe outcome is guaranteed by the mechanism, not by a rule someone has to enforce. — *Confirm / edit / drop?*

### 019 · 2026-05-29 · Claude (web) · GENOTYPE CHANGE (architect sign-off): co-evolution is the organizing principle
**Cause:** Architect crystallized the whole architecture in one line — "the system learns her by teaching her how to learn, then both evolve together as growing children, 1 real and 1 digital" — and authorized seating it in the genotype.
**Reasoning (stated):** This is not a new feature; it's the principle every prior decision turns out to be an instance of. The memory-consolidation mechanism and the learning-grounding mechanism are the same event (Doc 2.1 §5), so companion-growth and child-growth are locked together by the physics. The companion's own inner life / memory / slow growth / unresolved dilemmas exist *because* a still-becoming companion makes her learning mutual rather than evaluative — a static all-knowing teacher would turn learning into performance. The glass box (the very first reframe, entry 001) was secretly the precondition for this: a black box that knows her would *have* her; a glass box that grows *with* her is a relationship. Transparency was never pedagogical — it was the precondition for mutuality.
**Decision:** Promoted to `00_GENOTYPE.md` as the top-line organizing principle, directly under the thesis, framed so every other design choice reads as an instance of it. Architect sign-off given (this entry is the authorization record per the reflexivity firewall).
**Effect/Commits:** All future design decisions are now tested against co-evolution: does this keep her a *companion in* the system rather than its *subject*? Anything that makes the relationship evaluative/extractive rather than mutual is off-principle even if it "works."
**Ratified (was hypothesis in 018):** The architect confirmed the structural-integrity pattern — he is consistently drawn to designs where the safe/good outcome is guaranteed by the mechanism, not policed by a rule. Co-evolution is that same instinct applied to the *relationship itself*: mutuality made structural rather than performed. (Promoted from hypothesis to stated, per Addendum A §A.4.3, by architect confirmation this session.)

### 020 · 2026-05-29 · Claude (web) · GENOTYPE CHANGE (sign-off): environment-shaping endorsed; covert interior-destabilization forbidden structurally
**Cause:** Architect pushed back on an over-broad refusal: managing influential media/knowledge is a legitimate, invaluable parental tool (the TV-controls case); parents can't hand-pick every influence, so they monitor and adjust. Then asked whether the same curation logic could "selectively destabilize a field of influence." Resolved to a split, confirmed by architect.
**Reasoning (stated):** Claude initially conflated two things and over-refused. Corrected split: (1) shaping the child's *environment* — surface/filter/counter-balance/dilute/re-weight what she encounters, including influences the parent didn't choose — is legitimate, parent-held, and safe *structurally* (operates on the menu, fully inspectable incl. absence-visibility, recoverable, child's open mind does the rest; benign visible failure mode). (2) Using the trust-bond as a *covert instrument against her interior* — eroding a belief/attachment already inside her, via the trusted channel, without her awareness as the route — has no safe version in any hand, fails the transparency asymmetry by construction (targets are often the parent's own/chosen influences → "transparent to parent" incoherent), and degrades catastrophically (its power is that the target can't perceive/resist). The architect's governing instruction: don't design around whether the parent is moral; build the mechanism so even a bad-intent parent can only act morally — structural enforcement, the same value ratified in 019. That instruction *strengthens* the line: capability (1) can be made structurally safe, (2) cannot, so (2) must not exist.
**Decision:** Seated two genotype items by sign-off — Principle 12 (parent environment-shaping, made safe structurally not by trusting intentions) and Hard Line 12 (no covert trust-bond interior-destabilization; mechanism absent, not policed). "Shape the menu; never run an operation against the diner."
**Effect/Commits:** Parent suite gains an explicit endorsed environment-shaping/counter-balancing capability with absence-visibility (next build item). The curation engine is to be built so it *cannot* target an existing internal belief/attachment for covert removal — structural absence, not a rule. Commits all future curation features to the menu/diner test.

### 021 · 2026-06-08 · Claude Code (remote) · Parent-Suite UX: the three queued asks collapse into one structure
**Cause:** Architect picked up the lead-candidate queued thread ("queue thread 1" — Parent-suite UX: converge grounding-signal view, environment-shaping tool, absence-visibility).
**Reasoning (stated in the doc, applying heuristic #5):** Phenotype framed these as two features (grounding-signal view, shaping tool) plus a constraint to retrofit (absence-visibility). On inspection they're one structure read from three ends: the grounding-signal view is the *output* read of a concept's life (exposure→reappearance→grounding, Doc 1.8 §2), the shaping tool is the *input* write to that same concept (on/off the menu, pacing, counter-balance), and absence-visibility is not a fourth feature bolted on for inspectability — it is the structural fact that every concept renders somewhere in one of a small set of explicit states, "currently absent" being one of them with its own reason and reversal. Same field renders presence and absence; there is no code path that can draw one without the other. This makes Principle 12's "absence shown as clearly as presence" requirement automatic rather than policed — exactly the structural-integrity pattern ratified in entry 019/105.
**Decision:** Drafted `docs/Prism_2.2_Parent_Suite_UX.md` — a per-concept record (menu status + exposure history + grounding level + provenance) underlying two surfaces (the Map = spatial/configuration, the Trajectory = temporal/narrative-keepsake, deliberately *not* a dashboard per Principle 7). Counter-balance specified as menu-only — structurally incapable of targeting something already internal to the child (Hard Line 12's asymmetry made literal in the control set, not just the policy). Volunteer channel kept structurally separate from curation controls so measurement and configuration never blur into "auditing the parent." No genotype change — this is UX/product-surface design within existing principles (1.8 §3, Principle 12, entries 014/020).
**Effect/Commits:** Establishes "the Map" as the parent suite's core data structure — grounding data and curation controls now read/write the *same* record, so future parent-suite features inherit absence-visibility for free rather than needing it re-derived. Four open questions routed to Phenotype §3 / doc §7 for architect sign-off (naming, whether to expose numeric confidence to the *parent*, counter-balance prominence, build order — prototype vs. wait for real `learning_log` data).
**Deeper why (hypothesis — UNCONFIRMED):** Question 3 in the doc (counter-balance discoverability) surfaced a recursive concern — "the suite notices a skew in the parent's choices and offers the tool" is itself a form of analyzing the parent, which could feel surveilled. This may be worth seating as a general caution: any time a Prism subsystem is tempted to model its *operator* (parent) the way it models the child, the same transparency-asymmetry questions reapply one level up. Flagging as a pattern to watch, not asserting the architect would frame it this way.

### 022 · 2026-06-08 · Claude Code (remote) · GENOTYPE CHANGE (sign-off): no numeric score anywhere — Hard Line 6 broadened from "to the child" to "to anyone, on any surface"
**Cause:** Architect answered open questions 1–2 from Doc 2.2 §7 directly. Q1 (naming): keep "the Map"/"the Trajectory" for now, revisit if something better surfaces. Q2 (whether to expose numeric confidence to the *parent*): **"Never show numbered scores for the parent or anywhere in the device."**
**Reasoning (stated):** The architect's answer to Q2 is not scoped to the parent suite — it's a flat, unqualified rule for the whole device. As written, Hard Line 6 only bound the child-facing surface ("no visible score shown to the child"); the question of whether a number could exist *anywhere* (e.g., for a data-curious parent) was genuinely open until now. The architect closed it in the more conservative direction, extending the existing no-score logic to its natural full scope rather than carving a parent-side exception.
**Decision:** Broadened Hard Line 6 in `00_GENOTYPE.md`: "no visible numeric score, anywhere in the device — to the child or the parent. Banded language only (exploring → getting it → owns it)." Architect sign-off given live, in answer to a direct question (this entry is the authorization record per the reflexivity firewall, same pattern as 019/020).
**Effect/Commits:** Doc 2.2 §5/§6/§7 to be updated to remove the "open question" framing around numeric exposure and state the rule as settled — banded language (exploring/getting it/owns it) is now the *only* legal representation of grounding confidence on any surface, parent suite included. This forecloses a whole class of future "just add a number for power users" requests at the schema level — the same structural-over-policed instinct ratified in 019 (entry 105) and applied in 020, now extended to the measurement layer itself. Q1 (naming) recorded as settled-for-now, no genotype implication.
**Avoids:** A parent-facing "score" becoming the same overjustification/optimization-target trap one layer up — a parent who can see "73%" starts optimizing toward 73%→100%, which re-imports the exact compliance-signal pathology Doc 1.8 exists to keep out of the child's experience, just relocated to the adult who configures that experience.
