# Cortex — AI Collaboration Governance Layer
### Unified Session Coherence + Comprehension Depth for AI-Assisted Development

---

## The Problem, Precisely Stated

Any software project worked on with AI coding assistance across multiple sessions has two compounding failure modes. They look like different problems. They are two halves of the same problem.

**Failure mode A: The AI loses the plot.**
Every new session starts blind. You re-explain the architecture, re-establish context, re-brief the model on decisions already made. Across sessions, subtle incompatibilities accumulate. One session resolves a tradeoff one way; a later session, not knowing, resolves it differently. The architecture erodes. The reasoning behind decisions evaporates. No single session is wrong — the problem is cumulative incoherence.

**Failure mode B: The AI doesn't understand what it's saying it understands.**
AI coding assistants are equally confident whether they have deep familiarity with your codebase or have seen a module once. A session reads the architecture constraints, acknowledges them, and then writes a code path that violates one — not because it's malicious, but because it didn't genuinely understand the invariant in the new context. There's no signal to tell you where the AI has grounded comprehension vs. where it's pattern-matching on thin context.

**The question both failure modes answer:**
> *Can the AI work coherently and correctly on this codebase over time?*

Cortex answers it. Neither tool alone does.

---

## Why Two Tools Needed to Become One

**Genome** (the coherence layer) solves failure mode A:
- Five-file structure in `.genome/` — GENOTYPE, PHENOTYPE, EPIGENOME, SELECTION, SHADOW
- Solves amnesia (tiered cold-start), drift (reflexivity firewall), and lost reasoning (append-only epigenome)
- Tells every session: here are the rules, here is what we're doing now, here is why every major decision was made

**GroundLine** (the comprehension layer) solves failure mode B:
- Tracks concept applications across AI-assisted diffs; measures context-distance of correct reproductions
- Builds a risk gradient: high-confidence concepts are safe for autonomous AI action; low-confidence ones require human review
- Tells every PR gate: here is where the AI actually understands your code vs. where it is guessing

**The gap each has alone:**

*Genome without GroundLine:* Knows what was decided but cannot detect comprehension drift. The GENOTYPE says "auth checks must precede any data access." The AI reads it, acknowledges it, then writes a new module with a data fetch before the auth check. Genome has no signal — the genotype wasn't modified, the epigenome wasn't violated. The error is silent.

*GroundLine without Genome:* Knows what the AI understands but has no source of truth for what it *should* understand. It detects all concepts from diffs equally — a low-confidence logging convention is flagged at the same severity as a low-confidence auth invariant. A rejected path (the thing the architect explicitly ruled out) looks to GroundLine like a new concept exploration. It cannot distinguish architecturally critical from incidental.

**Together:** The architect's intent (GENOTYPE) flows into the comprehension measurement (concept criticality weights), the measurement results flow back into the institutional record (EPIGENOME auto-entries), the institutional record informs future sessions (cold-start), the architect's judgment (SELECTION) governs what the AI is trusted to do autonomously. The loop closes.

---

## The Five Integration Points

These are not thematic overlaps — they are mechanical connections where one system's data directly drives the other's logic.

### 1. GENOTYPE → Concept Criticality Weights

Without integration, GroundLine weights all concepts equally in confidence accumulation. With Cortex, concepts that appear in the GENOTYPE receive a **criticality multiplier**:

```
grounding(concept) += distance × correctness × criticality_weight(concept)
```

Where `criticality_weight` is derived from the concept's presence in GENOTYPE (critical), SELECTION (elevated), or neither (default). A low-confidence application of a GENOTYPE invariant blocks a merge. A low-confidence application of a logging convention flags for spot review.

The comprehension risk gradient is now calibrated to the architect's actual priorities, not an undifferentiated concept space.

### 2. SHADOW → Forbidden Zone Detection

The SHADOW file contains rejected paths — alternatives tried, found wanting, explicitly ruled out. Without integration, an AI re-exploring a shadow path looks to GroundLine like a new concept application and accumulates confidence normally.

With Cortex, concepts mapped to SHADOW entries become **forbidden zones**. When GroundLine detects the AI applying a concept that matches a rejected path, it:
- Flags the change as a shadow violation (distinct from a low-confidence warning)
- Surfaces the relevant SHADOW entry in the PR annotation
- Requires architect override, not just human review

The shadow file's institutional memory becomes an active gate, not a passive record. "We tried that already" stops happening silently.

### 3. PHENOTYPE → Measurement Priority Scoping

The PHENOTYPE carries the current sprint focus in one sentence. Without integration, GroundLine monitors the entire codebase uniformly. With Cortex, GroundLine's measurement priority **scopes to the active phenotype focus** — concepts in modules the phenotype marks as active get elevated monitoring; stable modules not in the current focus are measured at baseline.

When the phenotype says "current focus: payment flow refactor," Cortex intensifies comprehension monitoring on payment concepts and relaxes it elsewhere. Signal-to-noise improves because measurement effort follows the work.

### 4. GroundLine Events → EPIGENOME Auto-Entries

Currently, the EPIGENOME is manually appended. With Cortex, GroundLine becomes an **autonomous epigenome contributor** for comprehension-quality events:

```markdown
### Decision E-GL-047 — auth/oauth-pkce.ts flagged low-confidence (auto)
**Cause:** GroundLine detected AI applications of oauth-pkce in 1 context, 0 verified test passes
**Reasoning (auto):** Concept not grounded across independent contexts; GENOTYPE invariant
**Decision:** Blocked from autonomous merge; required human review
**Effect:** Two bugs caught in review (2026-05-31); concept now in active monitoring
```

The epigenome now carries not just architectural decisions but the longitudinal history of AI comprehension on each concept. Future sessions reading the epigenome know both what was decided and where the AI previously struggled — the two most important things to know before touching a module.

### 5. SELECTION → Autonomous Action Policy

The SELECTION file carries the architect's decision heuristics. GroundLine's autonomous action thresholds were previously flat numbers disconnected from the architect's actual values. With Cortex, the SELECTION file includes a `groundline:` block that Cortex reads directly:

```markdown
## Cortex Autonomous Action Policy (in SELECTION.md)
- GENOTYPE concepts: block if confidence < 0.60; never auto-merge without human sign-off
- SELECTION concepts: require review if confidence < 0.50
- SHADOW concepts: block always; architect override required
- Neutral concepts: auto-merge if confidence > 0.75 and tests pass
```

The autonomous action policy is governed by the same file that governs all the architect's other decisions. One source of truth.

---

## System Architecture

```
.genome/                          ← the five-file coherence backbone
├── 00_GENOTYPE.md                → concept criticality weights
├── 10_PHENOTYPE.md               → measurement priority scoping
├── 20_EPIGENOME.md               ← receives GroundLine auto-entries
├── 30_SELECTION.md               → autonomous action policy
└── 40_SHADOW.md                  → forbidden zone definitions

.cortex/                          ← comprehension measurement state
├── confidence.db                 ← concept confidence accumulator
├── concept-map.json              ← AST-extracted concept registry (reads GENOTYPE)
└── policy.yml                    ← compiled from SELECTION.md (do not edit directly)
```

The `.genome/` directory is the substrate — architect-controlled, git-native, human-readable. The `.cortex/` directory is the measurement state — auto-maintained, derived from AI coding activity. The `policy.yml` is always a compiled artifact from `SELECTION.md`; you never edit it directly.

---

## The Combined Cold-Start

When an AI session opens on a Cortex-enabled project:

1. Reads `AGENTS.md` → loads GENOTYPE + PHENOTYPE (Tier 1) — architectural context established
2. `cortex status` output injected → current sprint focus + comprehension map for active modules
3. Session proceeds knowing both: *here are the rules* (genome) and *here is where I've struggled before* (groundline)

The cold-start now answers two questions in one pass that were previously answered by separate tools — or not answered at all.

---

## CLI

```bash
npm install -g cortex-dev
# or: pip install cortex-dev  /  brew install cortex

# Setup
cortex init                       # bootstrap .genome/ + .cortex/ together; update AGENTS.md
cortex hook install               # pre-commit guard (genotype protection + confidence gate)

# Status
cortex status                     # unified view: phenotype focus + comprehension map + open questions
cortex status --risk              # only low-confidence + shadow-flagged + unratified hypotheses
cortex map                        # full comprehension map, weighted by GENOTYPE criticality
cortex map --critical             # GENOTYPE-class concepts only

# History
cortex log                        # epigenome (architectural decisions + GroundLine auto-entries)
cortex shadow                     # rejected paths + active forbidden zone alerts
cortex concept auth/oauth         # drill: grounding history + epigenome entries for one concept
cortex since 30d                  # what changed in comprehension map in last 30 days

# PR gate (used in CI)
cortex check --pr 42              # unified check: genome violations + confidence + shadow violations
```

### `cortex init` output:
```
.genome/
├── 00_GENOTYPE.md     (template: fill in project thesis, invariants, hard lines)
├── 10_PHENOTYPE.md    (template: fill in current focus, acceptance criteria)
├── 20_EPIGENOME.md    (empty log, ready for first entry)
├── 30_SELECTION.md    (template: decision heuristics + Cortex autonomous action policy)
└── 40_SHADOW.md       (empty, ready for first rejected path)

.cortex/               (auto-maintained; commit confidence.db)
AGENTS.md              (updated with Cortex loading instructions)
```

Setup time: ~20 minutes for an existing project with one person who knows the architecture.

---

## IDE Extension (VS Code / Cursor)

**Status bar:** `[Auth refactor] Comprehension: 3 critical concepts LOW` — phenotype focus + risk summary always visible.

**Sidebar panel:**
- Open questions from PHENOTYPE §3
- Low-confidence GENOTYPE concepts in active modules (click to see grounding history)
- Unratified epigenome hypotheses awaiting architect review
- Active shadow violations in current diff

**Gutter indicators:** Per-file confidence bands — green (high), yellow (medium), red (low), orange (shadow forbidden zone).

**Pre-commit badge:** Fires if GENOTYPE or SELECTION is modified without authorization tag, or if diff touches a blocked concept.

---

## PR Gate

```yaml
# .github/workflows/cortex.yml
- name: Cortex check
  run: cortex check --pr ${{ github.event.pull_request.number }}
```

Example PR annotation:
```
🔴 Cortex [SHADOW VIOLATION]: auth/oauth-pkce.ts
   This approach matches Shadow S03 (PKCE without CSRF token — rejected 2025-11).
   Architect override required. See .genome/40_SHADOW.md#S03.

⚠️  Cortex [LOW CONFIDENCE]: billing/proration.ts — 0.19 (GENOTYPE concept)
   This is an architectural invariant. 1 application, 0 verified test passes.
   Human review required before merge.

✅ Cortex [OK]: auth/session.ts — 0.87 (12 applications, 5 distinct contexts)
   Auto-merge eligible per SELECTION policy.
```

---

## What Developers Get

| Problem | Cortex's fix |
|---|---|
| Re-briefing each new AI session | Tier 1 cold-start in seconds; comprehension map loaded in same pass |
| Architectural drift across sessions | Reflexivity firewall; GENOTYPE/SELECTION protected |
| Lost reasoning behind decisions | Epigenome — architectural decisions + comprehension history, unified |
| "We tried that already" — rediscovered rejections | Shadow forbidden zones: flagged in PR, not just recorded in a file |
| AI confident on code it doesn't understand | Comprehension map — risk gradient from evidence, not self-report |
| Uniform review scrutiny — can't prioritize | GENOTYPE-weighted confidence: focus review effort where architecture is at risk |
| No data-driven AI autonomy policy | SELECTION-derived policy: architect's judgment operationalized as merge gates |
| Onboarding is opaque | Comprehension map tracks new team member grounding progress same as AI |

---

## Comparison

| Approach | Amnesia | Drift | Lost reasoning | Comprehension gaps | Autonomy policy | Offline | Portable |
|---|---|---|---|---|---|---|---|
| Raw `AGENTS.md` | partial | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ |
| Genome alone | ✓ | ✓ | ✓ | ✗ | ✗ | ✓ | ✓ |
| GroundLine alone | ✗ | ✗ | ✗ | partial | partial | ✓ | ✓ |
| **Cortex** | **✓** | **✓** | **✓** | **✓** | **✓** | **✓** | **✓** |

---

## The Closed Loop

```
Architect intent (GENOTYPE / SELECTION)
    ↓
Weighted comprehension measurement
(GroundLine tracks concept applications;
 GENOTYPE concepts weighted critical;
 SHADOW concepts trigger forbidden zone alerts)
    ↓
Gated AI action
(PR gate: architecture check + confidence check + shadow check;
 autonomous merge policy from SELECTION)
    ↓
Institutional record
(EPIGENOME: architectural decisions + GroundLine auto-entries;
 comprehension history alongside design history)
    ↓
Future session coherence
(cold-start: genome Tier 1 + comprehension map scoped to phenotype focus)
    ↓
Architect intent (closed)
```

This is not a documentation system. It is not a measurement system. It is an **AI collaboration governance layer** — the first tool that closes the loop between what the architect intends, what the AI understands, and what the AI is trusted to do.

---

*Cortex · developer tool · unifies the Genome Protocol and GroundLine comprehension tracking · Capps Consulting Company LLC · extends the AGENTS.md open standard (Linux Foundation)*
