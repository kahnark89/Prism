# The Genome Protocol
### A system for coherent context across multiple LLM clients, temporal gaps, and platforms

*Built on the ArcShield genomic documentation architecture (CIAER+ paper §6), wrapped in the AGENTS.md open standard for universal client compatibility. Designed for a workflow that spans Claude, Gemini, Claude Code, and CLI agents, across a Pixel 9 Pro, a minimum-spec Windows laptop, desktop, and terminal.*

---

## 0. The problem, stated precisely

A long-running project worked on by **different LLM clients** (Claude web, Gemini, Claude Code, CLI agents), across **temporal gaps** (days between sessions), on **different platforms** (phone, weak laptop, desktop, terminal), accumulates three failure modes:

1. **Amnesia** — each new client/session starts blind; you re-explain the project every time.
2. **Drift** — different clients make subtly incompatible assumptions; the architecture erodes without anyone deciding to change it.
3. **Lost reasoning** — *why* a decision was made (and what was rejected) evaporates, so old debates reopen and settled questions get re-litigated.

Standard AGENTS.md solves (1) for stateless coding tasks. It does **not** solve (2) drift-with-authority or (3) decision memory — which is exactly what the genomic architecture adds. This protocol combines both.

> **The core insight (from the ArcShield paper, now applied to its own development workflow):** any system that must persist information across discontinuous instances while resisting drift needs four separated layers — an invariant *description*, a live *phenotype*, an append-only *epigenetic* history, and an external *selective principle*. Collapse any two and you get drift. This protocol is that four-layer separation, made portable.

---

## 1. The file structure

A single git repository is the substrate (it's the universal copier — every client, every platform reads git). At the root:

```
project-root/
├── AGENTS.md                  ← COMPATIBILITY SURFACE. Every client reads this first.
│                                Thin. Points into the genome below.
├── CLAUDE.md  → symlink to AGENTS.md   (Claude Code reads this)
├── GEMINI.md  → symlink to AGENTS.md   (Gemini reads this)
│
├── .genome/
│   ├── 00_GENOTYPE.md         ← INVARIANT. What the project IS. Rarely changes.
│   │                            Architecture, hard lines, schema, commitments.
│   ├── 10_PHENOTYPE.md        ← LIVE STATE. What THIS phase is doing. Changes every session.
│   ├── 20_EPIGENOME.md        ← APPEND-ONLY history. Decisions, traps hit, why.
│   ├── 30_SELECTION.md        ← The architect's externalized decision rules.
│   └── 40_SHADOW.md           ← Rejected alternatives + why (counterfactual preservation).
│
├── docs/                      ← the actual deliverables (e.g. the Prism master architecture)
└── src/                       ← code, when the build starts (nested AGENTS.md per module)
```

**Why this layering maps to the genome (ArcShield §6.2–6.3):**

| File | Biological analog | Mutation rate | Who may change it |
|---|---|---|---|
| `00_GENOTYPE.md` | DNA | very low | architect only, with explicit sign-off |
| `10_PHENOTYPE.md` | gene expression | high (every session) | any client, freely |
| `20_EPIGENOME.md` | epigenetic marks | append-only | any client, append-only |
| `30_SELECTION.md` | the selective principle | low | architect only |
| `40_SHADOW.md` | counterfactual record | append-only | any client, append-only |

This is **Invariant 5 (bounded mutation rate)** from your paper, enforced by file convention: the genotype changes far slower than the phenotype, by design.

---

## 2. What goes in each file

### `AGENTS.md` (the compatibility surface — keep it thin)
Per the 2026 best practice (architecture overviews waste tokens here; commands and boundaries earn their place). Contents:
- One-paragraph project description.
- **The boot instruction:** "Before doing anything, read `.genome/00_GENOTYPE.md` (what this project is), then `.genome/10_PHENOTYPE.md` (what we're doing right now). For decision history see `20_EPIGENOME.md`. Do not modify `00_GENOTYPE.md` or `30_SELECTION.md` without explicit architect sign-off."
- Build/test/lint commands (once code exists).
- Hard boundaries (the "hard lines" — never do X).
- Tooling/version constraints.

### `00_GENOTYPE.md` (the invariant)
The things that must not drift. For Prism: the thesis, the design principles, the hard lines (§15 of the master doc), the CIAER+ schema commitment, the safety model. This is the description an LLM constructor reads but **cannot rewrite without authorization** — the reflexivity firewall (§5 below).

### `10_PHENOTYPE.md` (the live state — the most-read file)
What *this* working phase is actually doing. Structured:
- **§1 Current focus** — the one thing in flight right now.
- **§2 Acceptance** — how we'll know this phase is done (the external grounding signal — Invariant 2).
- **§3 Open questions** — what's undecided *right now*.
- **§4 Next actions** — concrete, so a cold client knows where to pick up.
- **§5 Session log pointer** — "last touched by [client] on [date], see epigenome entry N."

This is what makes resumption instant: a cold LLM reads the genotype once to know the rules, then the phenotype to know the moment.

### `20_EPIGENOME.md` (append-only decision history)
Every meaningful decision, dated, with: what was decided, *why*, what it depended on, and the trap it avoids. **Append-only** (Invariant 4 — history is the substrate of learning; silent rewriting loses drift detection). This is the layer vanilla AGENTS.md lacks entirely, and the one that prevents re-litigating settled questions.

### `30_SELECTION.md` (the architect's externalized judgment)
Your decision rules — the heuristics by which *you* decide which alternatives survive. For Prism, things like: "invisible to the child, transparent to the parent," "optimize the teaching never the compliance," "aliveness over consistency." This lets any client make choices *the way you would* between your sessions. (This is the "rule of three" / OGC discipline, externalized — your selective principle made inheritable.)

### `40_SHADOW.md` (counterfactual preservation)
Rejected alternatives and why (Invariant 3 — Shadow Actions at the project scale). Prevents a future client from "rediscovering" a path you already rejected. E.g., "Rejected: Pi Zero 2 W as primary — chokes on UI+camera+audio+network. Rejected: always-on ambient mic — privacy/regulatory landmine."

---

## 3. The tiered context-loading strategy (context-window economics)

A cold client should **not** ingest the whole project to resume. Load in tiers, stopping when the task is covered:

- **Tier 1 (always):** `AGENTS.md` + `00_GENOTYPE.md` + `10_PHENOTYPE.md`. ~the rules + the moment. Usually enough to resume.
- **Tier 2 (on demand):** `20_EPIGENOME.md` + `40_SHADOW.md` — when the task touches *why* something is the way it is, or proposes changing it.
- **Tier 3 (rarely):** `30_SELECTION.md` — when making a judgment call the phenotype doesn't cover, or when you're absent and the client must decide as you would.
- **Tier 4 (deep):** `docs/` and `src/` — the actual artifacts, loaded by the specific files in play.

This is your genotype/phenotype split doing exactly what your paper said it does: "a fresh agent reading these files in sequence inherits the same architectural genotype, sees the current phenotypic state, and reads the epigenetic marks without being retrained." Tiering keeps the cold-start cost near-constant regardless of project age.

---

## 4. The multi-platform sync workflow (tuned for your hardware reality)

The constraint: a **strong phone (Pixel 9 Pro)** that's always with you, and a **minimum-spec Windows laptop** that can't be assumed to have heavy compute or constant connectivity. Git is the backbone, but the workflow must treat the phone as a first-class node.

**The substrate: one git repo, synced everywhere.**
- **Desktop / laptop:** standard git. Claude Code and CLI agents commit directly.
- **Phone (primary, always-available node):** the `.genome/` files are plain markdown — editable and commitable from the phone via a git client app, or via a synced folder (the files are tiny; sync is trivial even on cellular). The phone is where you capture decisions *in the moment* — which is most of them.
- **Weak laptop:** never needs to build or run heavy compute to participate in the *coherence* layer — editing markdown and committing is near-zero cost. Heavy lifting (actual builds) happens on the desktop or in the cloud; the laptop stays a thin client for the design conversation.

**The workflow loop:**
1. **Open** any client on any platform → it reads Tier 1 → it's coherent in seconds.
2. **Work** → the client updates `10_PHENOTYPE.md` freely, appends to `20_EPIGENOME.md` for any real decision, appends to `40_SHADOW.md` for any rejected path.
3. **Sign-off gate:** if the work implies a *genotype* change (an architecture or hard-line change), the client does NOT edit `00_GENOTYPE.md` — it writes a proposal into `10_PHENOTYPE.md §3` and flags it for you. You authorize, then the genotype changes. (§5.)
4. **Commit** → one commit per session, message references the epigenome entry. Even from the phone.
5. **Next client, next platform, next day** → back to step 1, fully coherent.

**Conflict handling:** because the phenotype is the only high-churn file and sessions are usually sequential (one human, one project), merge conflicts are rare. When they happen, the append-only files never conflict destructively (appends merge), and the phenotype is small enough to reconcile by hand. Last-write-wins is acceptable for the phenotype; never for the append-only layers.

---

## 5. The reflexivity firewall (the load-bearing rule)

Straight from your paper, and the single most important rule in this protocol:

> **No LLM client may modify `00_GENOTYPE.md` or `30_SELECTION.md` without explicit architect sign-off.** The constructor cannot rewrite the description.

This is what prevents drift-with-authority — the failure where a well-meaning client "improves" your architecture mid-session and the next client inherits the mutation as if you'd blessed it. Clients propose genotype changes into the phenotype's open-questions section; only you promote them. Phenotype and epigenome churn freely; the genome mutates only through you. This keeps **selection exogenous** — the architect remains the selective principle, exactly as natural selection stays external to the organism.

It also means: if a client ever tells you it changed a hard line or core architecture decision on its own, that's a protocol violation and a signal to check the diff. The firewall is enforced socially (the instruction) and verifiably (git diff on protected files is always reviewable).

---

## 6. The cold-boot prompt (paste this into any new client)

A single reusable prompt that cold-starts any LLM client — Claude, Gemini, whatever — into full coherence:

```
You're joining an ongoing project. Before responding:
1. Read AGENTS.md, then .genome/00_GENOTYPE.md (what this project is and its hard
   lines), then .genome/10_PHENOTYPE.md (what we're doing right now).
2. If the task touches WHY something is designed as it is, or proposes changing it,
   also read .genome/20_EPIGENOME.md and .genome/40_SHADOW.md before proposing anything.
3. You may freely update 10_PHENOTYPE.md and append to 20_EPIGENOME.md (decisions)
   and 40_SHADOW.md (rejected options).
4. You may NOT modify 00_GENOTYPE.md or 30_SELECTION.md. If your work implies a change
   to either, write the proposal into 10_PHENOTYPE.md §3 (Open Questions) and flag it
   for my sign-off instead.
5. When we finish, summarize what changed in one line for the commit message and tell
   me which genome files you touched.
Confirm you've read the genome and state the current focus back to me before we start.
```

That last line is the coherence check: if the client correctly states the current focus back, it's truly loaded the context. If it can't, the genome files need tightening.

---

## 7. Why this is better than either approach alone

- **vs. raw AGENTS.md:** gains decision memory (epigenome), counterfactual preservation (shadow), externalized judgment (selection), and the anti-drift firewall — none of which the stateless standard provides.
- **vs. a bespoke system:** gains universal client compatibility for free. Every coding agent in 2026 reads AGENTS.md natively; the genome lives underneath it, so you never fight a tool's defaults.
- **vs. just CLAUDE.md:** gains cross-client portability (symlinks mirror AGENTS.md into CLAUDE.md/GEMINI.md), so you're not locked to one vendor — which matters because you actively use both Claude and Gemini.

---

## 8. Bootstrapping checklist

1. `git init` a repo (or use the existing project repo).
2. Create `.genome/` with the five files. Seed `00_GENOTYPE.md` from the project's master doc; leave `10_PHENOTYPE.md` pointing at the current focus.
3. Write the thin `AGENTS.md` at root with the boot instruction (§2).
4. `ln -s AGENTS.md CLAUDE.md && ln -s AGENTS.md GEMINI.md` (or copies on Windows if symlinks are awkward — the weak-laptop accommodation).
5. Install a git client on the Pixel; confirm you can edit `.genome/` and commit from the phone.
6. Save the cold-boot prompt (§6) somewhere one tap away.
7. From now on: every session starts with the boot prompt, ends with a commit. The project becomes immortal across clients, gaps, and platforms.

---

## 9. The one-line summary

*Your ArcShield genome architecture is a cross-session coherence protocol. Wrap it in AGENTS.md for universal client compatibility, put it in git for universal platform sync, keep the genotype mutation behind your sign-off, and any LLM on any device on any day resumes the project as if it never left.*

---

*The Genome Protocol · built on CIAER+ / ArcShield genomic documentation architecture, Capps Consulting Company LLC · wraps the AGENTS.md open standard (Linux Foundation)*
