# GroundLine — AI Comprehension Depth Tracker
### Know What Your AI Assistant Actually Understands About Your Codebase

---

## The Bottleneck

AI coding assistants are equally confident whether they understand your code well or poorly.

A session that has seen your authentication module once in a training window gives the same confident suggestion tone as one that has traced it across ten independent contexts. There is no signal to tell you which parts of your codebase the AI has genuine depth on versus where it is pattern-matching on thin context. You cannot manage what you cannot measure.

**The consequence:** Developers apply uniform review scrutiny across all AI suggestions, which means:
- Over-reviewing confident suggestions in well-understood areas (wasted time)
- Under-reviewing confident suggestions in poorly-understood areas (where the bugs live)
- No way to know when to trust autonomous AI action vs. when to require human checkpoint

**Current workarounds that don't work:**
- Ask the AI to express confidence — it will, but the confidence is self-reported and unreliable
- Read the diff carefully — you're back to manual review; the AI provided no signal
- Run tests — catches bugs, doesn't catch architectural misunderstandings before they compound

The problem is structural: current tools have no mechanism for tracking comprehension depth. They track coverage, they track usage, they track errors — but not understanding.

---

## The Pattern

> **Genuine understanding shows up in independent reproductions. The same concept applied correctly in a structurally different context is evidence of comprehension, not recall.**

This is the grounding signal: instead of asking "did the AI see this code?" ask "has the AI applied this pattern correctly in multiple independent contexts?" The difference is the difference between exposure and comprehension.

The mechanism, formalized:

```
grounding_confidence(concept, t) =
    Σᵢ context_distance(encoding_context_i, application_context_i) × correctness(i)
```

Where:
- `concept` is an identified codebase pattern, API, convention, or module interface
- `context_distance` is the structural dissimilarity between where the concept was first seen and where it's being applied (high distance = genuinely new context; near-zero = same context = parroting)
- `correctness` is a binary signal from tests, linting, code review, or runtime

A concept seen once has low confidence regardless of how correctly it was applied. A concept applied correctly across multiple genuinely different contexts has high confidence. The signal accumulates naturally across working sessions — no benchmarks, no labeled datasets, no explicit testing.

---

## How It Works

### Step 1 — Concept Extraction

GroundLine instruments your git workflow and AI assistant sessions to extract concept applications. A concept is:
- A named module's public interface being used
- A design pattern being applied (factory, observer, repository)
- A project-specific convention being followed (error handling style, naming convention, schema field usage)
- A critical invariant being respected (auth check before data access, null check before dereference)

Extraction runs on diffs — the AI-assisted changes are parsed for concept applications.

### Step 2 — Context Distance

For each concept application, GroundLine computes the context distance between the current application site and the concept's prior exposure sites:

```python
distance(context_a, context_b) = JSD(
    embedding(context_a),   # call-site structural fingerprint
    embedding(context_b)    # prior exposure structural fingerprint
)
```

Jensen-Shannon divergence between structural fingerprints of the call sites. High divergence = genuinely new context. Near-zero divergence = the AI is applying the same pattern in essentially the same context (high risk of copy-paste reasoning, not deep understanding).

Structural fingerprints are derived from AST features — call depth, module neighborhood, data flow context — not from the raw text of the code.

### Step 3 — Confidence Accumulation

```
grounding(concept) += distance × correctness_signal
```

Correctness signal sources (in priority order):
1. Tests pass for the changed code — `+1.0`
2. Static analysis clean — `+0.5`
3. Human review approved — `+1.0`
4. Runtime error in changed code — `-1.5` (strong negative signal)
5. Revert of AI-assisted change — `-2.0` (strong negative signal)

Confidence decays slowly over time: if the AI hasn't applied a concept correctly in a new context for 30 days, the confidence drifts back toward baseline. This prevents stale confidence from old sessions masking current gaps.

### Step 4 — Risk Gradient Output

```bash
groundline status
```

```
Codebase Comprehension Map (as of 2026-05-31)
─────────────────────────────────────────────
HIGH CONFIDENCE (safe for autonomous AI action)
  auth/session.ts          ████████████  0.87  12 applications, 5 distinct contexts
  db/migrations/           ████████████  0.83   8 applications, 4 distinct contexts
  api/rate-limiter.ts      ██████████░░  0.74   6 applications, 3 distinct contexts

MEDIUM CONFIDENCE (spot review recommended)
  payment/stripe-webhook.ts ███████░░░░░  0.52   3 applications, 2 distinct contexts
  cache/redis-client.ts     ██████░░░░░░  0.44   4 applications, 2 distinct contexts

LOW CONFIDENCE (require human review before merge)
  auth/oauth-pkce.ts        ████░░░░░░░░  0.28   2 applications, 1 distinct context
  billing/proration.ts      ███░░░░░░░░░  0.19   1 application,  1 distinct context
  infra/k8s-secrets.ts      ██░░░░░░░░░░  0.11   1 application,  0 verified contexts

UNGROUNDED (AI has not applied these concepts)
  compliance/gdpr-audit.ts  (no AI applications detected)
  crypto/key-derivation.ts  (no AI applications detected)
```

---

## Developer Workflow Integration

### PR Gate

```yaml
# .github/workflows/groundline.yml
- name: GroundLine risk check
  run: groundline check --pr ${{ github.event.pull_request.number }}
```

GroundLine annotates the PR diff with confidence scores per changed concept. Low-confidence changes get a required human review flag. High-confidence changes are auto-approved for merge (if your policy allows).

Example PR annotation:
```
⚠ GroundLine: auth/oauth-pkce.ts — confidence 0.28 (LOW)
  This concept has only been applied in 1 context with 0 verified test passes.
  AI-assisted changes here require human review before merge.
```

### IDE Integration (VS Code / Cursor)

- Gutter indicators: green (high confidence), yellow (medium), red (low), grey (ungrounded)
- Hover tooltip: confidence score, number of independent contexts, last verification event
- Inline warning when AI suggestion touches a low-confidence concept: `⚠ GroundLine: low confidence on this pattern — review carefully`

### CLI

```bash
npm install -g groundline
# or: pip install groundline

groundline init              # configure for repo (language, test runner, AI tool integration)
groundline status            # full comprehension map
groundline status --risk     # only low + ungrounded concepts
groundline concept auth/oauth # drill into one concept's grounding history
groundline since 30d         # changes in confidence map in last 30 days

groundline watch             # daemon mode: update confidence in real time during sessions
```

### Invisible by Default

GroundLine runs entirely in the background. Developers do not change their workflow — they continue to write code, run tests, and merge PRs. The grounding signal accumulates from those existing events. The comprehension map updates automatically. No prompt changes, no special modes, no labeled datasets.

The signal is structurally invisible to the AI — the AI cannot optimize for a higher GroundLine score, because the score is derived from independent reproductions, not from self-report or performance on a known benchmark. The measurement cannot be gamed.

---

## What Developers Get

| Without GroundLine | With GroundLine |
|---|---|
| Uniform review — AI is always equally confident | Risk gradient — know where to focus review effort |
| No signal for autonomous action boundaries | Clear thresholds for when AI can act autonomously |
| Bug postmortems reveal misunderstood patterns | Proactive: low confidence flags the gap before the bug |
| Onboarding is opaque — no measure of ramp-up | Comprehension map shows new team member's grounding progress |
| "The AI got this wrong" (surprise) | "This concept was low-confidence" (expected, caught) |

### Onboarding Tracker

GroundLine works equally well for human contributors. A new engineer's grounding confidence per concept is tracked the same way: concept applications in new independent contexts, verified by test passes and review approval. The comprehension map becomes a concrete onboarding progress tracker — not time-in-seat, but demonstrated understanding.

### Autonomous Action Boundaries

For teams running AI agents with write access to the codebase (agentic coding, automated refactors, AI-generated PRs), GroundLine provides a principled policy surface:

```yaml
# groundline.policy.yml
autonomous_merge_threshold: 0.75     # AI can auto-merge if all changed concepts > 0.75
review_required_threshold: 0.50      # human review required for any concept < 0.50
block_threshold: 0.25                # block AI changes to concepts < 0.25
```

This is the first tool that lets teams set data-driven boundaries on AI autonomy — not "this file is sensitive" (static, unmaintained) but "this concept is not yet grounded" (dynamic, evidence-based).

---

## Technical Design Principles

**Language-agnostic via AST.** Concept extraction is AST-based; language parsers are pluggable (TypeScript, Python, Go, Rust, Java first).

**No training data required.** Confidence accumulates from existing development events: test runs, linter output, PR reviews, git reverts. No labeled dataset. No fine-tuning. Works from day one of installation.

**Privacy-safe.** Structural fingerprints are hashed AST features — no source code leaves the local environment. The confidence database is local by default; cloud sync is opt-in.

**Zero workflow change.** Runs as git hooks + CI step. Developers don't change how they work; the measurement is derived from events they're already producing.

---

*GroundLine · developer tool · derived from Prism invisible grounding signal architecture (Doc 1.8) and co-evolution organizing principle · Capps Consulting Company LLC*
