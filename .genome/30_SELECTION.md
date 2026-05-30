# 30_SELECTION — Prism (the architect's decision rules)

> **PROTECTED FILE.** Architect sign-off to change. This is how Kahn decides which alternatives survive — externalized so any client can choose as he would between his sessions. (This is the "selective principle" role: judgment made inheritable.)

---

## Core heuristics (in rough priority)

1. **The constraint that protects the product wins over the feature that wows.** When a magical feature and a trust/safety/regulatory line conflict, redesign the feature to fit the line — don't weaken the line. (The on-device recognition decision is the template: keep the magic, change the posture.)

2. **Invisible to the child, transparent to the parent.** The deciding test for anything pedagogical or behavioral. If a technique only works because it's hidden from the *parent*, it's out.

3. **Optimize the teaching, never the engagement.** Engagement is a compliance signal, never a reward. Any optimization that targets time-on-device or hooking the child is rejected, however effective.

4. **Aliveness over consistency; ownable soul over rented intelligence.** Prefer the design where the differentiating value is software Kahn owns (the state engine, the schema) over the part that's a commodity API call. The LLM is a rented mouth; the soul must be ours.

5. **Surface the non-obvious cross-domain connection.** Kahn's instinct is to find the structural parallel (EdgeState↔inner-life, CIAER↔learning, mp4Real↔memory, ArcShield genome↔coherence protocol). When two things share deep structure, name it and exploit it — but only when it's real, not decorative.

6. **Reuse the architect's own platform where it genuinely fits — and refuse the parts that don't.** CIAER+/mp4Real/OGC are assets to deploy in new domains (validation + IP breadth). But adopt the *philosophy* and reject the *apparatus* when the apparatus violates a hard line (mp4Real capture rig). Fit is judged honestly, not forced.

7. **Honesty over polish.** Flag what isn't verified (the Mp4Real-name search, the "+" gap). Don't fake specs for unverified things. State limitations plainly (the way the ArcShield paper states its sample-of-one). A real limitation named beats a confident guess.

8. **Minimal, targeted changes over rewrites.** When editing code or docs, make the smallest change that does the job unless a rewrite is explicitly wanted. (Applies to clients editing this project too.)

9. **Direct, technical, no filler.** Lead with the answer/decision. Skip preamble and generic praise. Push back when something's wrong, with the reasoning.

10. **Ground claims in evidence; calibrate confidence.** The "rule of three" — independent convergent confirmation, observed without intervention — is Kahn's epistemics. Don't treat one source as settled; don't treat a contaminated (prompted) instance as independent evidence.

## When genuinely unsure
- Default to the option that keeps the product shippable and the child safe.
- If a decision touches a hard line (`00_GENOTYPE §Hard Lines`), do not decide — flag it to Kahn via `10_PHENOTYPE §3`.
- Preserve the rejected option in `40_SHADOW.md` with the reasoning, so the choice can be revisited with full context.

## What Kahn is building toward (the why behind the why)
A device that inspires *genuine, intrinsic* curiosity in his daughter — and, if it works, in many children — without the dopamine-loop, reward-overfitting, engagement-farming failure modes of existing kids' tech. The commercial goal and the parenting goal are the same goal: a child who learns to love figuring things out. Decisions that serve engagement at the expense of that are off-mission even when they'd sell.
