# 10_PHENOTYPE — Prism (live state)

> High-churn file. Any client updates this freely. This is "what we're doing right now."
> **Last touched:** 2026-05-29 by Claude (web) · see Epigenome entry 020.
> **Pending ratification:** 2 unconfirmed deeper-why hypotheses in Epigenome 016, 017 (018 ratified into 019). Confirm/edit/drop when convenient.

---

## §1 Current focus
**HANDOFF TO CLAUDE CODE — push to remote.** Design session (Claude web) ended with the repo fully committed locally (6 commits, clean tree). The immediate job for the receiving client is operational, not design: initialize/connect a remote and push everything. After pushing, optionally run `./setup_links.sh` (Linux/macOS) to convert CLAUDE.md/GEMINI.md pointer files into real symlinks. Then await architect direction on the next design thread.

**Design state for context (not this session's task):** Both magic pillars are specified + simulated (Inner Life Doc 2.0, Memory Doc 2.1). Co-evolution is the seated organizing principle (genotype). The parent-suite UX — converging the grounding-signal view, the environment-shaping tool, and absence-visibility — is the most natural next *design* thread. Other queued threads: Awakening choreography spec; Mission 0 (First Light hardware build).

## §2 Acceptance (how we know the current phase is done)
- Genome files committed to a git repo. ✅ (bootstrapped this session)
- Architect can paste the cold-boot prompt into a fresh client (Claude or Gemini) and have it correctly state the current focus back. ← verify next session on a real repo.
- `00_GENOTYPE.md` accurately reflects the master architecture's hard lines. ✅

## §3 Open questions (incl. proposed genotype changes awaiting sign-off)
- **Build vs. magic-deepening order** — which Mission/spec to do next. (Architect decision.)
- **Companion names** — Pip/Lumi/Tale are placeholders; to be replaced with Naomi's real favorites, captured during the build via the Build-Session Companion. (Not yet built.)
- **Compute final call** — CM5-on-custom-carrier (chosen, master doc §5) vs. Pi 5 in a designed enclosure (simpler). Leaning carrier given fab access; not locked. *(Proposed genotype refinement — needs sign-off before §Hardware is tightened.)*
- **Shadow Actions for youngest tier** — drop the field at 3–5 (rarely fires) and let it light up with age? Clean age-scaling hook; undecided.
- **mp4Real "+" / CIAER+ Pre-ENV mapping for a child** — sketched (time/mood/place as I-frame); not yet formalized into the learning_log schema.

## §4 Next actions
1. **(Receiving client / Claude Code) Push to remote:** create the repo on the host (GitHub/GitLab/etc.), `git remote add origin <url>`, `git push -u origin main` (or `master` — this repo's default branch is whatever `git init` set; check with `git branch`). All 6 commits should transfer with full history.
2. Optionally `./setup_links.sh` on Linux/macOS to make CLAUDE.md/GEMINI.md real symlinks (on Windows without symlink support, leave the pointer files).
3. Confirm the push succeeded and report the remote URL back to the architect.
4. Then await architect direction on the next design thread (parent-suite UX is the lead candidate).

## §5 Deliverables on hand (in docs/)
- `Prism_Master_Architecture_v1.md` — canonical reference (consolidates Docs 01–1.8).
- Component docs 01, 1.5, 1.6, 1.7, 1.8 — the detailed source documents.
- `The_Genome_Protocol.md` — the protocol this folder implements.

## §6 Build status
Not started. No hardware acquired yet. No code written. Project is design-complete, build-pending.
