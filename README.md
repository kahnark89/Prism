# Prism

A DIY smart learning camera for a young child (ages 3–5, scaling to ~12). Point it at an object and friendly AI characters give multiple points of view; it teaches how AI works (glass box, not black box), awakens for the child it recognizes, and feels alive. The build is the child's first tech project.

Capps Family Enterprises · uses CIAER+™ and mp4Real™ (Capps Consulting Company LLC).

---

## This repo runs on the Genome Protocol

The project maintains coherent context across multiple LLM clients (Claude, Gemini, CLI), temporal gaps, and platforms (phone, laptop, desktop) using a five-layer "genome" under `.genome/`, wrapped in the AGENTS.md open standard.

```
AGENTS.md        ← every client reads this first (CLAUDE.md / GEMINI.md mirror it)
.genome/
  00_GENOTYPE.md   what the project IS + hard lines   (PROTECTED — sign-off to change)
  10_PHENOTYPE.md  what we're doing right now          (high-churn, edit freely)
  20_EPIGENOME.md  decision history                    (append-only)
  30_SELECTION.md  how the architect decides           (PROTECTED — sign-off to change)
  40_SHADOW.md     rejected paths + why                (append-only)
docs/            the actual design deliverables (master architecture + component docs)
```

## Starting a session with any LLM
Paste this prompt into any client:

> You're joining an ongoing project. Before responding: (1) read AGENTS.md, then `.genome/00_GENOTYPE.md`, then `.genome/10_PHENOTYPE.md`. (2) If your task touches WHY something is designed as it is, also read `.genome/20_EPIGENOME.md` and `.genome/40_SHADOW.md`. (3) You may edit `10_PHENOTYPE.md` and append to `20_EPIGENOME.md` / `40_SHADOW.md`. (4) You may NOT modify `00_GENOTYPE.md` or `30_SELECTION.md` — propose changes into `10_PHENOTYPE.md §3` for sign-off. (5) On finishing, give a one-line commit summary and list which genome files you touched. Confirm you've read the genome and state the current focus back to me before we start.

## Setup
1. `git init` (if not already a repo) and commit.
2. On Linux/macOS you can run `./setup_links.sh` to make `CLAUDE.md`/`GEMINI.md` real symlinks to `AGENTS.md`. On Windows without symlink support, leave the pointer files as-is.
3. Install a git client on your phone so `.genome/` is editable/commitable in the moment.

## Status
Design-complete, build-pending. Canonical reference: `docs/Prism_Master_Architecture_v1.md`.
