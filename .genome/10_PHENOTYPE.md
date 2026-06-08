# 10_PHENOTYPE — Prism (live state)

> High-churn file. Any client updates this freely. This is "what we're doing right now."
> **Last touched:** 2026-06-08 by Claude Code (remote) · Parent-Suite UX spec drafted (Doc 2.2).
> **Pending ratification:** 2 unconfirmed deeper-why hypotheses in Epigenome 016, 017 (018 ratified into 019). Confirm/edit/drop when convenient.

---

## §1 Current focus
**DESIGN THREAD 1 SETTLED — Parent-Suite UX (Doc 2.2 complete, all open questions resolved by architect 2026-06-08). Ready to pick up thread 2.**

`docs/Prism_2.2_Parent_Suite_UX.md` converges the grounding-signal view (Doc 1.8 §3) and the environment-shaping/counter-balance tool (Genotype Principle 12 / Epigenome 020) into **one structure** — a per-concept record (menu status + exposure history + grounding level + provenance) — where absence-visibility falls out of the record's shape rather than being a feature bolted on: the same field renders "present" and "absent," so the tool cannot draw one without the other. Spec covers two surfaces (the Map / the Trajectory), the full control set, the volunteer channel kept deliberately separate, a pass against the one-line tests, and — added this round — a **Parent Preview / Test-Drive Mode** (§6): the parent runs the *real* system end-to-end on their own test inputs and configures everything *before* the child's first session, so nothing about the live product is ever a surprise. All four open questions are now closed:
1. Naming → keep "the Map" / "the Trajectory."
2. Numeric confidence → **genotype change** (Hard Line 6 broadened, Epigenome 022): no visible numeric score anywhere, to anyone — banded language only, full stop.
3. Counter-balance discoverability → permanent, always-visible, glass-box control (Epigenome 023).
4. Build order → reframed by the architect into a requirement: the whole system must be parent-previewable before child activation (Epigenome 023) — this is now a committed build requirement, not just a sequencing call.

**Remaining queued design threads (priority order):**
1. ~~Parent-suite UX~~ — **done**, see Doc 2.2 (zero open questions remain)
2. **Awakening choreography spec** — five-beat sequence (pause/spark/bloom/first breath/settle), audio + haptic + LED. *(Note: Doc 2.2 §8 flags that the Preview Mode will need to walk this sequence too once it's specified — the two threads will need to reconcile.)*
3. **Mission 0 — First Light** — camera → TFLite → speech, offline, one weekend build

## §2 Acceptance (how we know the current phase is done)
- Genome files committed to a git repo. ✅
- All 23 original files on `github.com/kahnark89/Prism` (master). ✅
- Three analysis docs on master: `docs/analysis/01–03`. ✅
- Tool specs separated: `animus-sdk` and `cortex-dev` repos initialized; zip files delivered. ✅
- Prism `docs/tools/README.md` points to both tool repos. ✅
- Cold-boot prompt in README.md still works from the remote. ✅

## §3 Open questions (incl. proposed genotype changes awaiting sign-off)
- **Parent-Suite UX — CLOSED (2026-06-08):** all four questions resolved by the architect; see §1 above and `docs/Prism_2.2_Parent_Suite_UX.md §8` for the full record. One forward-looking note carried into thread 2: the Parent Preview Mode (Doc 2.2 §6) will need to walk the awakening-choreography sequence once that spec exists — flag this when starting that thread so the two reconcile rather than diverge.
- **Next design thread** — Awakening choreography spec is next in the queue (architect may redirect).
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
