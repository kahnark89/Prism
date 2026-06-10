# 40_SHADOW — Prism (rejected alternatives)

> Append-only counterfactual record. Each entry: what was rejected, why, and what would change the decision. Prevents re-litigating closed paths. If a rejection is ever reversed, add a NEW entry referencing the old — don't delete.

---

### S01 · Pi Zero 2 W as the primary compute
**Rejected because:** chokes running touchscreen UI + camera + audio + networking + recognition simultaneously.
**Kept as:** the "Tier 0 / First Light" entry-level build only (offline classifier + audio, cardboard body) — a real first weekend project, not the product.
**Would reverse if:** the product scope shrank to offline-only naming with no UI/recognition.

### S02 · Always-on ambient microphone (continuous recording of the child)
**Rejected because:** a device that continuously records a young child and transcribes to an LLM is a COPPA/trust landmine — the single feature most likely to get the product pulled. Also pedagogically unnecessary.
**Replaced by:** push-to-capture parent tooling (Build-Session Companion) + on-device, gated, event-driven voice only. Preference capture happens at moments the parent controls.
**Would reverse if:** never, for the child-facing device. (Hard line.)

### S03 · The full mp4Real 7-track capture rig pointed at the child
**Rejected because:** continuous POV video + ECG-grade biometrics of a child to build a decision corpus is the same landmine as S02, at higher resolution.
**Replaced by:** mp4Real *philosophy* (discard-by-default, codebook) over a privacy-safe track set (object photo, label+confidence, reaction tag, optional gated voice). No child video, no biometrics.
**Would reverse if:** never, for the child. (Hard line.) The full rig remains valid for the industrial ArcShield domain it was designed for.

### S04 · Visible rewards / points / gamified praise for learning
**Rejected because:** overjustification effect — perceived rewards collapse intrinsic motivation and cause reward-overfitting, AND contaminate the learning measurement at the same point.
**Replaced by:** invisible continuous grounding signal; the child experiences only organic play.
**Would reverse if:** never. (Hard line + core mission.)

### S05 · Seven expert-persona panel for the 3–5 tier
**Rejected because:** seven lenses overwhelm a preschooler.
**Replaced by:** ~3 recurring character-friends at the youngest tier; the fuller expert panel unlocks with age-scaling.
**Would reverse if:** primary user age were older (the panel is the right call at ~9+).

### S06 · Acknowledging the child when she reuses a learned concept ("good job, you remembered!")
**Rejected because:** converts an independent observation into a prompted instance (poisons the measurement) and introduces a perceived reward (overjustification).
**Replaced by:** silent logging; at most, the companion naturally uses the concept more in play (spaced exposure), never as acknowledgment.
**Would reverse if:** never for explicit praise-as-reward; natural reuse is the allowed form.

### S07 · A bespoke cross-session memory format (instead of AGENTS.md-wrapped genome)
**Rejected because:** would fight every tool's defaults and lock to one vendor; the industry converged on AGENTS.md (Linux Foundation) and Kahn uses multiple clients (Claude + Gemini + CLI).
**Replaced by:** the ArcShield genomic architecture wrapped in AGENTS.md, in git.
**Would reverse if:** the open standard fragmented or a single-client lock-in became acceptable.

### S08 · Storing architecture overview in AGENTS.md root (per generic 2026 advice)
**Rejected because:** generic advice says architecture sections waste tokens for *stateless coding* — but Prism is a long-running *design* project where reasoning is the asset.
**Replaced by:** thin AGENTS.md (commands/boundaries/boot) + rich genome underneath (genotype carries architecture; epigenome carries reasoning).
**Would reverse if:** the project became a pure stateless codebase with no design-reasoning to preserve.

### S09 · 2026-06-08 · The standalone hardware product (Pi5/CM5 custom carrier, molded enclosure, LED ring, haptic ring, round display)
**Rejected because:** architect's stated reason — "ease of engineering" (Epigenome 024). This client's inference, offered as reasoning *for the record*, not as the architect's words: a bespoke physical product is an enclosure-design, materials-compliance, supply-chain, and manufacturing project layered on top of the software — each its own slow, expensive, regulatorily-loaded effort — before the actual differentiating IP (inner life, memory, CIAER+, the parent suite, all pure software) can even be tested with a real child. Two Android apps remove that entire layer and put the same software in front of Naomi sooner.
**Replaced by:** Two linked Android apps — Companion (child-facing, on a parent-provisioned phone/tablet, locked via guided-access/kiosk mode) + Parent Suite (parent-facing, separate install, separate device). Same engine underneath: two brains, on-device recognition, inner-life, learning_log, the parent suite — ported to a platform instead of bespoke-built. See `00_GENOTYPE.md` "Platform invariant," Epigenome 024, and `Prism_3.0_Platform_Architecture.md`.
**What carries forward unchanged:** the entire software architecture and every hard line — recognition stays on-device/parent-deletable, no continuous capture, the two-brains split, the safety model. This was a *delivery-form* decision, not a re-litigation of what Prism does or protects.
**Would reverse if** *(this client's inference — not stated by the architect; flag if it becomes relevant)*: the app validates the concept with a real child and a physical-product phase becomes commercially or pedagogically attractive later — nothing this pivot builds is wasted in that case; the bespoke-hardware plan documented in the prior "Hardware invariant" (Pi5/CM5, carrier, enclosure, LED ring, haptic, material/thermal lines — preserved here, not deleted) would simply become the *next* phase's starting spec rather than the first one.

### S10 · 2026-06-10 · Scoping Hard Line 6 to the grounding measurement (keeping the numeric child-facing glass box)
**Rejected because:** Architect chose "Keep HL6 absolute" when the contradiction between HL6's text and the `GlassBoxOverlay`'s percentage display was posed directly (Epigenome 029). This was the *recommended* option — it matched what was already built and entry 026's reasoning (vision confidence measures the machine, not the child; no overjustification risk) — and the architect declined it without further elaboration (recorded as-is, honesty rule).
**Replaced by:** Banded words ("just guessing" / "pretty sure" / "very sure") + non-numeric visuals everywhere, child-facing glass box and LLM prompt included; one shared vocabulary (`confidenceWords`) across the shown and spoken surfaces.
**Would reverse if:** the architect later judges that the explicit "the machine is 60% sure" beat is pedagogically worth more at an older age tier than the absolute rule costs — age-scaling (Principle 5) is the natural seam: numerals could unlock at an age where percentages are themselves curriculum. That reversal would need its own sign-off against Epigenome 029.
