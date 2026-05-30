# The Genome Protocol
### A Cross-Session Coherence System for Long-Running AI-Assisted Projects

---

## The Problem, Precisely Stated

Any long-running project worked on with AI assistance — across multiple clients, temporal gaps, and platforms — accumulates three failure modes that compound:

**1. Amnesia.** Every new client session starts blind. You re-explain the project, re-establish context, re-brief the model on decisions already made. The cost is linear with project age: the older the project, the more re-briefing each session requires. For a design project spanning months, the re-briefing eventually consumes most of the productive session.

**2. Drift.** Different client sessions make subtly incompatible assumptions. One session resolves a tradeoff one way; a later session, not knowing about the earlier resolution, resolves it differently. The architecture erodes without anyone deciding to change it. No single session is wrong; the problem is the cumulative incoherence across sessions. Drift is invisible until it's catastrophic.

**3. Lost reasoning.** The *why* behind decisions evaporates. A year later, a new session encounters a design choice and proposes changing it — not knowing it was already tried, rejected, and the rejection carefully explained. The old debate reopens. Settled questions get re-litigated. Months of reasoning are lost because the reasoning lived in conversations, not in durable artifacts.

Standard `AGENTS.md` solves (1) for stateless coding tasks. A well-written AGENTS.md means a new session can pick up and run tests, follow conventions, and understand the project structure. But it does **not** solve (2) drift or (3) lost reasoning — because it is a static document. It tells the model what the project is; it does not tell the model what decisions were made, why they were made, what was rejected, or how the architect decides.

The Genome Protocol combines AGENTS.md with a four-layer architecture specifically designed to solve all three failure modes.

---

## The Biological Analogy — and Why It Works

The protocol draws a structural parallel to the separation of genetic information in biology:

| File | Biological analog | Mutation rate | Controlled by |
|---|---|---|---|
| `00_GENOTYPE.md` | DNA — the invariant description | very low | architect only, explicit sign-off |
| `10_PHENOTYPE.md` | gene expression — the live state | high (every session) | any client, freely |
| `20_EPIGENOME.md` | epigenetic marks — history that shapes expression | append-only | any client, append-only |
| `30_SELECTION.md` | the selective principle — what survives | low | architect only |
| `40_SHADOW.md` | counterfactual record — what was tried and failed | append-only | any client, append-only |

This is not decoration. The analogy captures a real structural requirement: **the invariant must mutate far slower than the live state, and the mutation of the invariant must be controlled by an external authority** (the architect, not the AI client). Collapse any two layers — mix the invariant with the live state, or let the AI client modify the invariant — and you get drift.

The ArcShield paper calls this Invariant 5 (bounded mutation rate): the genotype changes far slower than the phenotype, by design. The Genome Protocol enforces this by file convention in a git repository.

---

## What Each File Actually Does

### `00_GENOTYPE.md` — The Invariant

The genotype carries what the project **IS**: the thesis, the design principles, the hard lines (the non-negotiables), the schema commitments, the safety model. This is the description that any AI client reads to understand what it's working on.

It is read frequently, changed rarely. When it changes, it changes through a deliberate sign-off process: a client proposes the change in the phenotype's open questions section; the architect authorizes; the genotype is updated; the authorization is logged in the epigenome. The genotype is never changed silently.

For Prism, the genotype carries: the co-evolution organizing principle, 12 design principles, 12 hard lines, the architecture invariants (aliveness is local, recognition is on-device, no biometric capture), and the safety model. These are the things that must not drift.

### `10_PHENOTYPE.md` — The Live State

The phenotype carries what the project is doing **right now**: the current focus (one sentence), the acceptance criteria for the current phase, open questions, next actions, and a pointer to the last session. It is the most-read file and the most frequently updated.

A cold client reads the genotype to understand the rules, then the phenotype to understand the moment. Together, these two files usually provide enough context to resume productively. The cold-start cost is near-constant regardless of project age because the phenotype stays small — it carries the *current* state, not the history.

The phenotype is the file that makes the coherence check possible: at the start of each session, the protocol asks the client to state the current focus back to the architect. If the client can do this correctly, it has loaded context. If it can't, the genome needs tightening.

### `20_EPIGENOME.md` — The Decision History

The epigenome is the layer that vanilla AGENTS.md lacks entirely. It is an append-only log of every meaningful decision: what was decided, why, what it depended on, and the trap it avoids.

"Append-only" is the critical constraint. The history cannot be rewritten. A session can add entries but cannot modify or delete existing ones. This means:
- Drift is detectable: if the system's behavior diverges from a logged decision, the divergence is visible in a `git diff`
- Re-litigation is prevented: a client encountering a settled question can find the entry that settled it and read the reasoning
- Tacit reasoning is preserved: the *why* behind decisions is durable, not session-local

The epigenome uses a lightweight CIAER skeleton for each entry: **Cause / Reasoning / Decision / Effect**. When the reasoning is inferred rather than stated, it is marked explicitly as a hypothesis awaiting ratification by the architect. This is the honesty firewall: inferred reasoning is never asserted as stated fact.

For Prism, the epigenome now carries 20 entries spanning the full design history — from the initial "glass box, not black box" reframe through the co-evolution genotype change. A client joining the project today can read the full reasoning chain behind every major decision in one file.

### `30_SELECTION.md` — The Architect's Externalized Judgment

The selection file carries the architect's decision rules: the heuristics by which they decide which alternatives survive. For Prism, these are ten explicit principles:

1. The constraint that protects the product wins over the feature that wows
2. Invisible to the child, transparent to the parent
3. Optimize the teaching, never the engagement
4. Aliveness over consistency; ownable soul over rented intelligence
5. Surface the non-obvious cross-domain connection
6. Reuse the architect's own platform where it genuinely fits — and refuse the parts that don't
7. Honesty over polish
8. Minimal, targeted changes over rewrites
9. Direct, technical, no filler
10. Ground claims in evidence; calibrate confidence

These heuristics make the architect's judgment inheritable. Between sessions, a client making a judgment call can ask: "which alternative would the architect choose, and why?" The selection file answers that question. Without it, the client makes its best guess, which may or may not align with the architect's actual values.

### `40_SHADOW.md` — The Counterfactual Record

The shadow file records rejected alternatives and why. It is the counterfactual preservation layer: it prevents a future client from "rediscovering" a path that was already explored and found wanting.

For Prism, the shadow file has eight entries: Pi Zero 2 W rejected for compute, always-on mic rejected for regulatory risk, the full mp4Real capture rig rejected for child privacy, visible rewards rejected for overjustification, seven expert personas rejected for overwhelming preschoolers, explicit praise-for-recall rejected for reflexivity contamination, and so on.

Without this file, a new client, not knowing any of these rejections, might enthusiastically propose an always-on mic as an improvement to the conversation experience. With this file, it reads S02 and understands that this was a COPPA landmine rejected as a hard line. The shadow file converts rejected paths from conversation-local knowledge into durable institutional memory.

---

## The Reflexivity Firewall — The Load-Bearing Rule

The most important rule in the protocol:

> **No AI client may modify `00_GENOTYPE.md` or `30_SELECTION.md` without explicit architect sign-off.**

This is the reflexivity firewall. It prevents the failure mode called drift-with-authority: a well-meaning client, in the course of a session, "improves" the architecture — simplifies a hard line, loosens a constraint, adjusts a design principle — and commits the change. The next client inherits the mutation as if the architect blessed it. Over multiple sessions, the architecture erodes without anyone having made a deliberate decision to change it.

The firewall prevents this structurally. Clients propose genotype changes by writing them into the phenotype's open questions section and flagging them for architect sign-off. The architect reviews, authorizes or rejects, and only then does the genotype change. The authorization is logged in the epigenome as the record of sign-off.

The firewall is enforced two ways:
- **Socially:** the instruction is in AGENTS.md, visible to every client on every session
- **Verifiably:** `git diff` on the protected files is always reviewable; any unauthorized change is visible

It also means: if a client ever tells you it changed a hard line or core architecture decision on its own, that is a protocol violation and a signal to check the diff.

---

## The Tiered Loading Strategy

A cold client should not ingest the whole project. The protocol specifies four tiers:

| Tier | Files | When to load |
|---|---|---|
| 1 (always) | AGENTS.md + GENOTYPE + PHENOTYPE | every session; usually sufficient |
| 2 (on demand) | EPIGENOME + SHADOW | when task touches *why* something is designed as it is |
| 3 (rarely) | SELECTION | when making a judgment call the phenotype doesn't cover |
| 4 (deep) | docs/ and src/ | specific artifacts in play |

This is the genotype/phenotype split doing what the ArcShield paper describes: "a fresh agent reading these files in sequence inherits the same architectural genotype, sees the current phenotypic state, and reads the epigenetic marks without being retrained." The cold-start cost stays near-constant regardless of project age because the phenotype carries only the current moment, not the history.

---

## Multi-Platform Sync

The protocol is designed for a specific hardware reality: a strong phone (always with you), a minimum-spec laptop (can't be assumed to have compute), a desktop (the main workstation), and multiple LLM clients (Claude, Gemini, CLI agents).

The substrate is git. Every platform reads and writes git. The `.genome/` files are plain markdown — tiny, fast to sync, editable from a phone in a git client app. The phone becomes a first-class node for capturing decisions in the moment (which is when most real decisions happen).

The workflow loop:
1. Open any client on any platform → it reads Tier 1 → coherent in seconds
2. Work → client updates phenotype freely, appends to epigenome for any real decision, appends to shadow for any rejected path
3. If genotype change implied → write proposal to phenotype §3, flag for sign-off; do NOT edit the genotype
4. Commit → one commit per session, message references the epigenome entry
5. Next client, next platform, next day → back to step 1, fully coherent

Merge conflicts are rare because sessions are usually sequential (one human, one project) and the append-only files never conflict destructively. The phenotype is small enough to reconcile by hand.

---

## The Autonomous Capture Addendum

The protocol includes a capture discipline for AI clients (Addendum A): throughout and at the end of a session, distill salient decisions from the conversation and log them — without being asked, without a permission step.

A decision is salient if a future cold client would otherwise have to re-derive it (a real choice was made, a path rejected, an open question resolved, a reason established).

The capture uses the CIAER skeleton (Cause / Reasoning / Decision / Effect-Commits) and a strict honesty rule: **stated reasoning is captured as fact; inferred reasoning is marked explicitly as "Deeper why (hypothesis — UNCONFIRMED)" until the architect ratifies.** A reconstruction that sounds like the architect but is actually a guess is the most dangerous fabrication. The hypothesis marker prevents this.

This discipline converts the AI client from a session assistant into a longitudinal collaborator — one that builds institutional memory across sessions rather than resetting to zero each time.

---

## The Genome Protocol as a Standalone Product

The protocol was built for Prism, but it solves a problem that is domain-general. Any development team using AI assistance on a project lasting more than a few sessions faces the same three failure modes.

The protocol is already built on two open standards: AGENTS.md (Linux Foundation) and git (universal). It adds no new infrastructure. It is five markdown files and a convention.

**Product form — three layers:**

**Layer 1: The standard document.** The Genome Protocol specification, versioned, open. Teams adopt it by reading the spec and creating the five files. Zero tooling required. Works today with any LLM client that reads AGENTS.md (which is all of them in 2026).

**Layer 2: The CLI.** `genome init` bootstraps the five files with sensible defaults in any repo. `genome status` shows the current phenotype focus and any unratified hypotheses. `genome log` displays the epigenome in a readable format. Installable via npm/pip/brew.

**Layer 3: The IDE extension.** The extension reads `10_PHENOTYPE.md §1` and surfaces the current focus in the status bar. Opens questions appear in a sidebar. Unratified hypotheses are flagged. A pre-commit hook validates that GENOTYPE and SELECTION weren't modified without a commit message referencing an epigenome entry.

**Target audience:** any team using Claude Code, Cursor, or similar AI coding assistants on a project lasting more than a few sessions. The problem is universal; the tooling is absent.

**IP position:** The Genome Protocol is a third-domain reduction-to-practice of the ArcShield genomic documentation architecture (after Prism children's learning and industrial PVC process monitoring). It is built on CIAER+ and mp4Real intellectual property. A standalone release strengthens both the invariance claim and the IP portfolio.

---

## Why This Is Better Than Existing Approaches

**vs. raw AGENTS.md:** gains decision memory (epigenome), counterfactual preservation (shadow), externalized judgment (selection), and the anti-drift firewall. None of these exist in the stateless standard.

**vs. a bespoke context system:** gains universal client compatibility for free. Every coding agent in 2026 reads AGENTS.md natively. The genome lives underneath it — no fighting tool defaults.

**vs. just CLAUDE.md:** gains cross-client portability. CLAUDE.md is a symlink to AGENTS.md; GEMINI.md is a symlink to AGENTS.md. Any client on any platform reads the same instructions. Vendor lock-in is structurally absent.

**vs. documentation wikis (Confluence, Notion, etc.):** the genome is in git — version-controlled, diffable, committable from a phone, accessible offline, and directly readable by AI clients without a web browsing step. History is preserved by append-only convention. The reflexivity firewall has no analog in a wiki.

**vs. conversation memory features (Claude's "memory," ChatGPT memory):** vendor-controlled, non-portable, non-inspectable, non-transferable to a different client. The genome is files you own, in a repo you control, readable by any client, auditable by `git diff`.

---

## The Coherence Check

The protocol ends each session with a coherence test: the client correctly states the current focus back to the architect before beginning work.

If the client states it correctly, the genome has done its job: the new session has loaded context accurately and is ready to continue coherently.

If the client cannot state it correctly — if it misses the current focus, describes the wrong phase, or demonstrates confusion about what the project is doing right now — the genome needs tightening. The failure is not in the client; it is in the genome's phenotype. §1 (Current focus) is probably too vague or too long.

This is the grounding check, applied to the protocol itself. The same principle that drives the learning grounding signal (external, independent confirmation of what's actually understood) applies to the coherence system: don't trust that the context was loaded correctly — verify it by testing reproduction.

---

*The Genome Protocol · Prism architectural analysis · built on CIAER+ / ArcShield, Capps Consulting Company LLC · wraps the AGENTS.md open standard (Linux Foundation)*
