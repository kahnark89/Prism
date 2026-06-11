# AGENTS.md — Prism

Prism is a DIY smart learning camera for a young child (ages 3–5, scaling to ~12): point it at an object and friendly AI characters offer multiple points of view (science, art, story, the AI's own reasoning). It teaches how AI works (a "glass box," not a black box), it awakens for the child it recognizes, and it feels alive. The build itself is the child's first tech project. Built by Kahn Capps / Capps Family Enterprises. Uses CIAER+™ and mp4Real™ (Capps Consulting Company LLC).

## BOOT — read before doing anything
1. Read `.genome/00_GENOTYPE.md` — what this project IS and its hard lines (the invariant).
2. Read `.genome/10_PHENOTYPE.md` — what we're working on RIGHT NOW.
3. If your task touches WHY something is designed as it is, or proposes changing it, also read `.genome/20_EPIGENOME.md` (decision history) and `.genome/40_SHADOW.md` (rejected options) before proposing anything.
4. For a judgment call the phenotype doesn't cover, read `.genome/30_SELECTION.md` (how the architect decides).

Then state the current focus back to the architect before starting. If you can't, the genome needs tightening — say so.

## What you MAY change
- `10_PHENOTYPE.md` — freely (it's the live working state).
- `20_EPIGENOME.md` — append-only (log every real decision: what, why, what it avoids).
- `40_SHADOW.md` — append-only (log every rejected path and why).

## Autonomous capture (do this without being asked)
Throughout and at the end of a session, distill salient decisions from our conversation and log them yourself — no permission step. A decision is salient if a future cold client would otherwise have to re-derive it (a real choice was made, a path rejected, an open question resolved, a reason established). Skip chatter and mechanical steps.
- Write decisions to `20_EPIGENOME.md` and rejected paths to `40_SHADOW.md` (append-only). Update `10_PHENOTYPE.md` freely. NEVER autonomously write `00_GENOTYPE.md` / `30_SELECTION.md`.
- Use the lightweight CIAER skeleton: **Cause / Reasoning / Decision / Effect-Commits** (+ optional Deeper-why).
- **Honesty rule (non-negotiable):** capture the *stated* reasoning as fact. If you infer an unstated "gut-level" why, record it under **"Deeper why (hypothesis — UNCONFIRMED)"** — never assert it as fact. The architect ratifies hypotheses later (removing the marker). Never launder a guess into a fact; a reconstruction that sounds like the architect is the most dangerous fabrication.
- Full spec: `docs/Genome_Protocol_Addendum_A_Autonomous_Capture.md`.

## What you MAY NOT change (the reflexivity firewall)
- `00_GENOTYPE.md` and `30_SELECTION.md` — **architect sign-off required.** If your work implies a change to either, write the proposal into `10_PHENOTYPE.md §3 Open Questions` and flag it. Do not edit these files yourself.

## Boundaries (project hard lines — never violate)
- No continuous video or biometric capture of the child. Ever.
- No training corpus built *of the child's decisions* for external use.
- Recognition templates stay on-device, separate, parent-deletable.
- No perceptible reward for the child for learning (overjustification effect).
- Invisible to the child, transparent to the parent — any technique whose efficacy depends on hiding it from the *parent* is forbidden.
- Optimize the teaching, never the engagement/compliance.
- Safety is never subject to the inner-life / "whim" engine.
(Full list: `00_GENOTYPE.md §Hard Lines`.)

## On finishing
Summarize what changed in one line for the commit message, and list which genome files you touched.

## Tooling notes
- **Code exists in two stacks.** `prism/` (Python) is the *reference spec* — the algorithm source of truth (equations, decay curves, safety gates), never shipping as a runtime (Epigenome 025). `android/` (Kotlin) is the shipping implementation: `:engine` (shared pure-logic port), `:sync` (pairing/crypto), `:companion-app` (child-facing), `:parent-suite-app` (parent-facing).
- **Python tests:** `pip install -e ".[dev]" && pytest tests/` (34 tests).
- **Android JVM tests:** `cd android && ./gradlew :engine:test :sync:test` (79 tests).
- **Android builds:** `cd android && ./gradlew :companion-app:assembleDebug :parent-suite-app:assembleDebug` (needs the Android SDK; CI does this on every push — `.github/workflows/build-apks.yml`).
- Clients: Claude (web/Code), Gemini, CLI agents. `CLAUDE.md` and `GEMINI.md` are mirrors of this file.
- Canonical design references: `docs/Prism_3.0_Platform_Architecture.md` (current two-app platform) and `docs/Prism_Master_Architecture_v1.md` (consolidated design — hardware sections superseded by Doc 3.0).
