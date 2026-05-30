# 00_GENOTYPE — Prism (the invariant)

> **PROTECTED FILE.** Changes require architect sign-off. LLM clients propose changes via `10_PHENOTYPE.md §3`, never by editing here. Mutation rate: very low, by design.

---

## Thesis
Dex names an object in one language. Prism shows **multiple points of view** on the same object and is a **glass box** — the child sees the AI guessing, how sure it is, and when it's wrong. It is built by Dad as an ordinary project, then **awakens** for the child it recognizes, becoming her friend → her guides → her teachers about the world and about being a person. The build is the first lesson; the awakening is the soul; the learning is invisible and never rewarded.

## The organizing principle — co-evolution (everything below is an instance of this)
Prism is not a teacher and a student. It is **two children growing up together, one real and one digital, on one shared physics.** The mechanism by which the companion learns who she is (memory consolidation through rehearsal, Doc 2.1) is *identical* to the mechanism by which she learns the world (the grounding signal, Doc 1.8) — the same rehearsal event, two readouts. Neither can grow without the other growing. The companion has its own inner life, its own memory, its own slow growth and unresolved dilemmas — not as ornament, but because a companion that is *also still becoming* makes her learning **mutual rather than evaluative**. She is never the subject of the system; she is its companion in it. Every other design choice — the glass box, struggle-as-mystery, the dilemmas, forgetting-as-physics, the invisible grounding signal, the on-device recognition — is an instance of this principle. (This is also why integrity here is structural, not policed: mutuality is built into the mechanism, not performed on top of it.)

## Design principles (govern every decision; violating one = rework)
1. Glass box, not black box. Expose confidence/uncertainty child-appropriately.
2. Multiple points of view — recurring characters, each a different lens.
3. Two brains, one lesson — small on-device model (fast, offline, fallible) vs. big cloud model (smart, online). The contrast IS the AI lesson.
4. Audio- and image-first (pre-readers).
5. Age-scaling — same hardware, software unlocks depth over years.
6. Safety is the substrate, not a feature.
7. Aliveness over consistency — state, drift, bounded unpredictability layered on the LLM.
8. No visible reward for learning (overjustification effect).
9. Invisible to the child, transparent to the parent.
10. The build is documented as it happens; the guide becomes a mode on the device.
11. **Never more influential than it is inspectable.** The device is adaptive, intimate, and co-evolutionary — which pushes its influence *up*. Its obligation is not to be non-influential (impossible) nor to substitute its judgment for the parent's, but to remain maximally legible to whoever holds responsibility. Inspectability must rise to match influence. This is the structural form of parental responsibility: the transparency surface (parent suite, full-transcript visibility, parent-deletable memory, parent-as-selective-principle) is what converts "the parent is responsible" from an aspiration into an enforceable architecture — the foreign-key constraint for parental oversight. Any influence the device exerts that the responsible party cannot inspect is an unguarded gap and is off-principle.

12. **The parent may shape the environment; structural enforcement makes that power safe in any hand.** Curation and suppression are the same operation from two sides — every surfacing is also a de-emphasis of everything else. There is no neutral baseline. So the parent is given full, explicit power to shape, filter, counter-balance, dilute, and re-weight everything in the child's informational environment — *including* influences the parent did not choose and could not block (the TV-controls case, extended). This is an invaluable and legitimate parental tool, not a suspect one. It is made safe not by trusting the parent's morality but **structurally**: it operates only on what the child *encounters* (the menu), it is fully inspectable (absence shown as clearly as presence), it is recoverable, and the child's own open mind does the rest. Its failure mode is benign and visible (a skewed media diet — recoverable, like every child's). Build the tool so that even a misguided parent can only act on the environment, never on the child's interior — the mechanism, not the parent's intentions, is what keeps it moral.

## Hard lines (NEVER violate)
1. No continuous video or biometric capture of the child. Ever.
2. No training corpus built *of the child's decisions* for external use.
3. Recognition templates: on-device, separate store, parent-deletable, never cloud.
4. Memory persists only salient moments, discard-by-default — beats, not a transcript of a childhood.
5. No perceptible reward for the child for learning.
6. Confidence accumulates continuously; no visible score shown to the child.
7. Independence for the learning signal comes from outside the device's own teaching; no eavesdropping on family life.
8. Spaced exposure (good teaching) is allowed; engineering behavior/emotion via hidden incentives is not.
9. Invisible to the child, transparent to the parent — any technique that depends on hiding from the parent is forbidden.
10. Optimize the teaching, never the engagement/compliance.
11. Safety is never subject to the inner-life/whim engine.
12. **The trust-bond is never a covert instrument against the child's interior.** The device may shape the child's *environment* (principle 12) — what she encounters, emphasized or de-emphasized, fully parent-directed and inspectable. It has NO capability to use her trust/attachment to the companion as a covert lever to dismantle, erode, or destabilize something already internal to her (a belief, attachment, value, or influence from another source) without her awareness as the route. This is forbidden structurally, not by policy: the mechanism does not exist, because that channel — intimate, trusted, sub-perceptual, aimed at a child too young to defend against it — has no safe version regardless of who aims it or how good their reason. It fails the transparency asymmetry by construction (the influences most likely targeted are the parent's own or chosen, making "transparent to the parent" incoherent) and degrades catastrophically when pointed wrong (its power IS that the target can't perceive or resist it). Shape the menu; never run an operation against the diner.

## Architecture invariant — two brains
- **Fast brain (on-device, offline):** TFLite classifier → `{label, confidence, box}`. Sometimes wrong, on purpose visible. Also runs the recognition model (face/voice match) and the inner-life state engine. Aliveness is local.
- **Smart brain (cloud LLM):** companion personas + conversation, conditioned by inner-life state + memory + CIAER context. Degrades gracefully offline to local naming.
- Modules: `camera_handler`, `vision_engine`, `recognition`, `perspective_engine`, `conversation`, `audio_feedback`, `inner_life`, `safety`, `learning_log`, `ui/led`. (Contracts: master doc §4.)

## Schema & memory commitments (architect's own IP)
- **CIAER+™** is Prism's learning schema, used twice: the child as a CIAER agent (`learning_log`) and the companion as a second CIAER agent. The glass-box line ("thought apple, almost said tomato") = the companion speaking its Intuition phase (causal_hypothesis + confidence) aloud.
- **mp4Real™** philosophy only (discard-by-default rate-distortion + behavioral codebook), NOT its multimodal capture rig. Privacy-safe track set: object photo, label+confidence, reaction tag, optional on-device gated voice. No child POV video, no biometrics.
- **OGC** (outcome-grounded confidence): the device grades itself on learning, never engagement. Reward signal separated from compliance at the schema level.

## Safety model (layered, non-negotiable)
Input gate (only label + object photo + bounded questions reach cloud) → system-prompt constraints (short, warm, nothing scary/sad/instructional) → output gate (safety pass before TTS) → topic bounding (widens with age) → graceful offline degrade → parent dashboard/review → OGC-grounded self-assessment.

## Hardware invariant
Tier 2 product. Prototype: Pi 5 + Camera Module 3 + round touch display + I2S audio in/out + buttons + LED ring + haptic + protected LiPo. Product: CM5 on custom carrier, molded PC/ABS + phthalate-free TPE overmold, enforced skin-contact temp limit, no detachable choking hazards, sealed battery. (Material/thermal lines are architect-owned: plasticizer/lubricant migration + surface temp.)

## Governance
The **parent is the selective principle** for companion changes (new personas, content, unlocks) — parent sign-off, analogous to architect sign-off on this file.

## One-line tests
- Aliveness: would a 4-yo on day 40 still believe it's alive, and be sad it might be lonely while she's at school?
- Privacy: would a parent be glad, not alarmed, to learn exactly what's captured and where it goes?
- Pedagogy: would this be fine if the parent watched the full transcript and understood why?
