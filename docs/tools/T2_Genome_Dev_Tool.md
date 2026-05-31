# Genome — Cross-Session Coherence for AI-Assisted Development
### Solve Amnesia, Drift, and Lost Reasoning — the Three Failure Modes of Long-Running AI Projects

---

## The Bottleneck

Any software project worked on with AI assistance — Claude Code, Cursor, GitHub Copilot, Gemini CLI — across multiple sessions accumulates three compounding failure modes that no existing tool addresses:

**1. Amnesia.** Every new AI session starts blind. You re-explain the architecture, re-establish context, re-brief the model on decisions already made. The cost scales linearly with project age. For a project spanning months, re-briefing eventually consumes most of the productive session window.

**2. Drift.** Different sessions make subtly incompatible assumptions. One session resolves a tradeoff one way; a later session, not knowing about the earlier resolution, resolves it differently. The architecture erodes without anyone deciding to change it. No single session is wrong. The problem is cumulative incoherence. Drift is invisible until it's catastrophic.

**3. Lost reasoning.** The *why* behind decisions evaporates. A year later, a new session encounters a design choice and proposes changing it — not knowing it was already tried, rejected, and the rejection carefully explained. Settled questions re-open. Months of reasoning are lost because the reasoning lived in conversations, not in durable artifacts.

**Current developer response to these problems:** Write a long README. Update a Confluence wiki. Add comments. Hope the next session reads them.

None of it works because the fix is structural: these failure modes come from a missing layer in the standard project-documentation stack, not from insufficient notes.

---

## The Pattern

Genome adds a five-file coherence layer to any git repository. The files map to the biological structure of genetic information — not for aesthetic reasons, but because the biological structure captures a real constraint:

> **The invariant must mutate far slower than the live state. Mutation of the invariant must require explicit authorization.**

| File | Role | Mutation rate | Authorized by |
|---|---|---|---|
| `00_GENOTYPE.md` | What the project IS — invariant | very low | architect sign-off only |
| `10_PHENOTYPE.md` | What the project is doing RIGHT NOW | high (every session) | any contributor, freely |
| `20_EPIGENOME.md` | Why decisions were made — append-only log | append-only | any contributor, append-only |
| `30_SELECTION.md` | How the architect decides between alternatives | low | architect sign-off only |
| `40_SHADOW.md` | What was tried and rejected — append-only | append-only | any contributor, append-only |

These five files sit in a `.genome/` directory at the repo root. They are plain markdown. They require no infrastructure. They work with every LLM coding assistant that reads `AGENTS.md` — which is all of them.

---

## What Each File Does

### `00_GENOTYPE.md` — The Invariant

What the project IS: the thesis, the architecture principles, the hard constraints (the non-negotiables), the schema commitments, the security model. Read frequently, changed rarely.

When it changes, it changes through a deliberate process: the change is proposed in the phenotype's open questions section, the architect authorizes, the genotype is updated, the authorization is logged in the epigenome. The genotype is never changed silently.

An AI session reads the genotype to understand what it's working on. It tells the session: here is the architecture, here are the constraints that cannot drift, here is the security model, here are the schema invariants.

### `10_PHENOTYPE.md` — The Live State

What the project is doing RIGHT NOW: the current sprint focus (one sentence), the acceptance criteria for this phase, open questions, next actions.

A cold AI session reads the genotype to understand the rules, then the phenotype to understand the moment. Together, these two files provide enough context to resume productively. The cold-start cost is near-constant regardless of project age because the phenotype stays small — it carries only the current state, not the history.

### `20_EPIGENOME.md` — The Decision Log

The layer that `AGENTS.md` doesn't have: an append-only log of every meaningful architectural decision. Format per entry:

```markdown
### Decision 007 — Use append-only writes for the audit log
**Cause:** Requirement to detect tampering without a full audit service
**Reasoning:** Append-only files make insertions detectable via line count + hash;
  overwrites would require controlling the file system
**Decision:** audit.log is append-only; periodic SHA256 checkpoints committed to git
**Effect:** Tamper detection is free; no audit database needed; git is the audit trail
```

"Append-only" is the load-bearing constraint. The history cannot be rewritten. Drift is therefore detectable: if behavior diverges from a logged decision, the divergence shows in `git diff`. Re-litigation is prevented: any session encountering a settled question finds the entry that settled it and reads the reasoning.

When the reasoning is inferred rather than stated, it is marked as a hypothesis: `**Deeper why (hypothesis — UNCONFIRMED)**`. Inferred reasoning is never asserted as stated fact. This is the honesty firewall.

### `30_SELECTION.md` — The Architect's Decision Rules

The heuristics by which the architect decides between alternatives. Example entries:

```markdown
1. The constraint that protects the product beats the feature that wows
2. Optimize the teaching, never the engagement
3. Minimal, targeted changes over rewrites
4. Ground claims in evidence; calibrate confidence
```

These heuristics make the architect's judgment inheritable. When a session makes a judgment call, it can ask: "which alternative would the architect choose?" The selection file answers. Without it, the session guesses — and the guess accumulates as drift.

### `40_SHADOW.md` — The Counterfactual Record

Rejected paths and why. Example entries:

```markdown
### Shadow S04 — Full client-side encryption rejected
**Proposed:** Encrypt all user data client-side before server storage
**Why rejected:** Key recovery is unsolvable without a trusted server component;
  losing the key = losing all data; tested with three enterprise customers who
  all required account recovery; blocked as customer-trust risk
**Constraint:** Do not re-propose without a key-recovery design
```

Without this file, a new session might enthusiastically propose client-side encryption as a security improvement. With this file, it reads S04 and understands the key-recovery impasse. The shadow file converts rejected paths from conversation-local knowledge into durable institutional memory.

---

## The Reflexivity Firewall

The load-bearing rule:

> **No AI session may modify `00_GENOTYPE.md` or `30_SELECTION.md` without explicit architect sign-off.**

This prevents drift-with-authority: a well-meaning session "improves" the architecture — simplifies a constraint, loosens a hard line — and commits it. The next session inherits the mutation as if the architect blessed it. The architecture erodes without anyone making a deliberate decision.

The firewall is enforced two ways:
- **Socially:** the rule is in `AGENTS.md`, visible to every session on every client
- **Verifiably:** `git diff .genome/00_GENOTYPE.md` is always reviewable; unauthorized changes are visible in PR diffs

Pre-commit hook option:
```bash
genome hook install
# Blocks commits that modify GENOTYPE or SELECTION
# without a commit message containing a GENOTYPE-CHANGE tag
```

---

## Developer Workflow Integration

### Tiered Loading — No Context Bloat

A session reads only what it needs:

| Tier | Files | When |
|---|---|---|
| 1 (always) | `AGENTS.md` + GENOTYPE + PHENOTYPE | Every session; usually sufficient |
| 2 (on demand) | EPIGENOME + SHADOW | Task touches *why* something is designed as it is |
| 3 (rarely) | SELECTION | Making a judgment call without explicit guidance |
| 4 (deep) | `src/`, `docs/` | Specific artifacts in play |

Tier 1 is ~3KB for most projects. Cold-start cost is near-constant regardless of project age.

### Git-Native — No New Infrastructure

```
.genome/
├── 00_GENOTYPE.md
├── 10_PHENOTYPE.md
├── 20_EPIGENOME.md
├── 30_SELECTION.md
└── 40_SHADOW.md
```

Version-controlled, diffable, committable from a phone, readable offline, readable by any LLM client. History is preserved by append-only convention. No database. No API. No vendor.

### Multi-Client, Multi-Platform

Every LLM coding assistant reads `AGENTS.md`. `CLAUDE.md`, `GEMINI.md`, and `.cursorrules` are symlinks to `AGENTS.md`. One source of truth, zero lock-in.

```bash
# AGENTS.md includes:
## Genome Protocol
Load .genome/ files in tier order before any session.
Never modify GENOTYPE or SELECTION without architect sign-off.
Append decisions to EPIGENOME; append rejections to SHADOW.
```

---

## CLI

```bash
npm install -g genome-cli
# or: pip install genome-cli  /  brew install genome

genome init                    # bootstrap .genome/ in any repo with sensible defaults
genome status                  # show current phenotype §1 focus + unratified hypotheses
genome log                     # epigenome in readable format
genome shadow                  # list rejected paths
genome diff --protected        # check if GENOTYPE or SELECTION changed since last commit
genome hook install            # install pre-commit guard on protected files
```

### `genome init` output:
```
.genome/
├── 00_GENOTYPE.md     (template: fill in project thesis, principles, hard lines)
├── 10_PHENOTYPE.md    (template: fill in current focus, acceptance criteria)
├── 20_EPIGENOME.md    (empty log, ready for first entry)
├── 30_SELECTION.md    (template: fill in architect decision heuristics)
└── 40_SHADOW.md       (empty, ready for first rejected path)

AGENTS.md              (updated with genome loading instructions)
```

Setup time: ~15 minutes for an existing project with one person who knows the architecture.

---

## IDE Extension

**VS Code / Cursor:**
- Status bar chip: current phenotype §1 focus (one sentence) — always visible
- Sidebar panel: open questions from phenotype §3 — actionable at any time
- Warning indicator: unratified hypotheses in epigenome awaiting architect review
- Pre-commit hook badge: GENOTYPE/SELECTION change detected without authorization tag

---

## What Developers Get

| Problem | Genome's fix |
|---|---|
| Re-briefing each new AI session | Tier 1 cold-start in seconds, project age irrelevant |
| Architectural drift across sessions | Protected files + reflexivity firewall |
| Lost reasoning behind decisions | Epigenome — append-only, searchable, durable |
| "We tried that already" — rediscovered rejections | Shadow file — rejected paths as institutional memory |
| AI sessions guessing what architect wants | Selection file — externalized judgment, inheritable |
| Vendor lock-in for AI workflow | AGENTS.md standard — every coding assistant reads it |

---

## Comparison

| Approach | Amnesia | Drift | Lost reasoning | Offline | Portable |
|---|---|---|---|---|---|
| Raw `AGENTS.md` | partial | ✗ | ✗ | ✓ | ✓ |
| Confluence / Notion wiki | partial | ✗ | partial | ✗ | ✗ |
| Claude/ChatGPT memory | partial | ✗ | ✗ | ✗ | ✗ (vendor) |
| **Genome** | **✓** | **✓** | **✓** | **✓** | **✓** |

---

*Genome · developer tool · implements the Genome Protocol (Capps Consulting Company LLC) · extends the AGENTS.md open standard (Linux Foundation)*
