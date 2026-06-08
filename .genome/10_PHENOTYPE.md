# 10_PHENOTYPE — Prism (live state)

> High-churn file. Any client updates this freely. This is "what we're doing right now."
> **Last touched:** 2026-06-08 by Claude Code (remote) · Parent-Suite UX spec drafted (Doc 2.2).
> **Pending ratification:** 2 unconfirmed deeper-why hypotheses in Epigenome 016, 017 (018 ratified into 019). Confirm/edit/drop when convenient.

---

## §1 Current focus
**DESIGN THREAD 1 IN PROGRESS — Parent-Suite UX (Doc 2.2 drafted, awaiting architect review).**

Picked up the lead-candidate queued thread. `docs/Prism_2.2_Parent_Suite_UX.md` converges the grounding-signal view (Doc 1.8 §3) and the environment-shaping/counter-balance tool (Genotype Principle 12 / Epigenome 020) into **one structure** — a per-concept record (menu status + exposure history + grounding level + provenance) — and shows that absence-visibility isn't a fourth feature bolted on for inspectability, but a structural consequence of that record's shape: the same field that renders "present" renders "absent," so the tool cannot draw one without the other. Spec covers two surfaces (the Map / the Trajectory), the control set (incl. counter-balance as menu-only, never diner-facing — the Hard-Line-12 asymmetry made literal), the volunteer channel kept deliberately separate, and a pass against the one-line tests. Four open questions logged in the doc §7 for architect sign-off (naming, whether to expose numeric confidence to parents, counter-balance discoverability, build order).

**Remaining queued design threads (priority order):**
1. ~~Parent-suite UX~~ — **in progress**, see Doc 2.2
2. **Awakening choreography spec** — five-beat sequence (pause/spark/bloom/first breath/settle), audio + haptic + LED
3. **Mission 0 — First Light** — camera → TFLite → speech, offline, one weekend build

## §2 Acceptance (how we know the current phase is done)
- Genome files committed to a git repo. ✅
- All 23 original files on `github.com/kahnark89/Prism` (master). ✅
- Three analysis docs on master: `docs/analysis/01–03`. ✅
- Tool specs separated: `animus-sdk` and `cortex-dev` repos initialized; zip files delivered. ✅
- Prism `docs/tools/README.md` points to both tool repos. ✅
- Cold-boot prompt in README.md still works from the remote. ✅

## §3 Open questions (incl. proposed genotype changes awaiting sign-off)
- **Parent-Suite UX sign-off** — four questions logged in `docs/Prism_2.2_Parent_Suite_UX.md §7`: working names ("the Map"/"the Trajectory"), whether to expose numeric confidence to the parent at all (vs. banded-only, everywhere), how prominent the counter-balance control should be (permanent vs. contextually-surfaced — and whether "the suite notices a skew" is itself too close to analyzing the parent), and build order (clickable prototype now vs. wait for real `learning_log` data).
- **Companion names** — Pip/Lumi/Tale are placeholders; replace with Naomi's real favorites during the build.
- **Compute final call** — CM5-on-custom-carrier vs. Pi 5 in designed enclosure. *(Proposed genotype refinement — needs sign-off.)*
- **Shadow Actions for youngest tier** — drop field at 3–5, light up with age?
- **mp4Real "+" / CIAER+ Pre-ENV mapping for a child** — sketched; not yet formalized into learning_log schema.

## §4 Next actions
1. Architect directs the next design thread (parent-suite UX is the lead candidate).
2. Seed `kahnark89/animus-sdk` and `kahnark89/cortex-dev` repos using the delivered zip files.
3. Optionally `./setup_links.sh` on Linux/macOS to make CLAUDE.md/GEMINI.md real symlinks.

## §5 Deliverables on hand (in docs/)
- `Prism_Master_Architecture_v1.md` — canonical reference (consolidates Docs 01–1.8).
- Component docs 01, 1.5, 1.6, 1.7, 1.8, 2.0, 2.1, 2.2 — detailed source documents.
- `Prism_2.2_Parent_Suite_UX.md` — converged spec: grounding-signal view + environment-shaping tool + absence-visibility as one structure (the Map / the Trajectory).
- `The_Genome_Protocol.md` — the protocol this folder implements.
- `analysis/01_Soul_Mouth_Separation.md` — standalone architectural analysis.
- `analysis/02_Co_Evolution_Organizing_Principle.md` — standalone architectural analysis.
- `analysis/03_The_Genome_Protocol.md` — standalone architectural analysis.
- `tools/README.md` — pointer to `animus-sdk` and `cortex-dev` repos.

## §6 Build status
Not started. No hardware acquired yet. No code written. Project is design-complete, build-pending.
