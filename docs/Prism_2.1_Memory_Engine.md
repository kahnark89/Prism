# Prism — Document 2.1: The Memory Engine

*What makes the companion alive on day 40, not just day 1. The second pillar (Inner Life > **Memory** > Reveals > Agency). It is the mp4Real rate-distortion philosophy pointed at a relationship, built on one principle most memory systems get backwards: the magic is in forgetting gracefully, not remembering everything.*

> **Thesis:** A companion that remembers everything is surveillance and storage bloat; one that remembers nothing is a toy. Aliveness lives in remembering the emotionally salient beats and being gracefully fuzzy on the rest — and in the fact that **what matters to her is exactly what persists**, because the relationship self-curates through rehearsal. Memory is not a database. It is a decaying, self-reinforcing dynamical system — the same physics as the Inner Life Engine, applied to episodic nodes.

---

## 1. What a memory is

Not a transcript. A weighted **episodic node**:

```
MemoryNode {
  id
  concept        # the abstracted thing ("spiral", "loud animal", "red")
  episode        # the lightweight specific (snail, in the garden) — fades first
  salience       # how much she reacted [0,1] — set at encoding
  strength  s    # current memory strength [0,1] — decays + refreshes over time
  tau            # decay time-constant — grows with rehearsal (consolidation)
  context        # time-of-day, place tag, her mood, companion mood at encoding
  links[]        # edges to related nodes (concept / context / temporal)
  last_active    # for decay computation
  rehearsals     # count — drives consolidation
}
```

The node stores **gist + a thin episode**, never raw media (hard line). Object photo reference is optional and itself subject to the privacy track-set rules (Doc 1.7 §4c).

---

## 2. The salience gate — what is allowed to persist (the LLR analog)

Most moments never become memories. Encoding requires crossing a salience threshold — the mp4Real log-likelihood-ratio gate, pointed at emotional information instead of process information. Salience accumulates from:

- **Emotional reaction magnitude** — delight, laughter, frustration, surprise (the biggest contributor; sourced from the same reaction signal the inner-life event system uses).
- **Novelty** — first time she's shown this concept (high), vs. repeat (low).
- **Relationship beats** — a thing she *taught* the companion, a reunion, a milestone.
- **Struggle** — a moment she was wrong and worked through it (high-value, per CIAER's disconfirmed-hypothesis = highest-information principle).

Below threshold → discarded, never encoded. **Default action is to forget.** This is what keeps the store small, the memory human-feeling, and the privacy clean by construction.

---

## 3. Forgetting as a feature — the decay curve

Every memory decays unless reactivated (Ebbinghaus forgetting curve, salience-modulated):

```
s(t) = s(t₀) · exp( −(t − t₀) / τ )
```

- `τ` (time constant) **scales with salience**: a high-delight memory gets a long τ (fades slowly, over weeks); a routine one gets a short τ (gone in days).
- When `s` drops below a **prune floor** (`s_min`), the node is removed — but see §7: the *concept* may survive in the codebook even after the *episode* is pruned.

Forgetting is not a bug to minimize. It is:
1. **What makes memory feel human** — a friend who remembers the important things and is fuzzy on the rest, not a panopticon.
2. **The privacy mechanism** — the device structurally cannot accumulate a permanent transcript; specifics decay out by design.
3. **The curation mechanism** — combined with §4, it ensures only what matters survives.

---

## 4. Rehearsal & consolidation — the self-curating loop

Every time a memory is **reactivated** — referenced in conversation, or its concept reappears when she shows a related thing — it gets refreshed:

```
on reactivation:
  s ← clamp01( s + ρ )          # strength kick back up
  τ ← τ · (1 + c)               # consolidation: each rehearsal makes it stickier
  rehearsals += 1
```

This is spaced repetition operating on the companion's *own* memory. The consequence is the whole point:

> **What she keeps engaging with keeps getting rehearsed, so it keeps refreshing, so it persists. What she stops caring about stops being reactivated, so it decays and prunes. The relationship curates itself — the surviving memories ARE the things that matter to her.**

No one decides what's important. The dynamics decide, and they decide correctly, because importance *is* recurrence.

---

## 5. The unification — memory rehearsal IS the learning grounding signal

This is the deepest structural fact in the whole architecture, and it falls out of §4 for free.

Recall Doc 1.8: a concept is "learned" when it reappears unprompted across independent contexts, confidence accumulating with context-distance. Now look at the memory side: a concept-memory's `τ` lengthens (consolidates) each time it's reactivated by reappearing in a new context. **These are the same operation.**

- Child side (Doc 1.8): "spiral" reappears on a pinecone, then in bathwater → grounding confidence rises.
- Memory side (this doc): the "spiral" node gets rehearsed in new contexts → its `τ` lengthens, it consolidates.

The reappearance that proves she learned it is the *same event* as the rehearsal that consolidates the companion's memory of it. **The companion remembering something and the child having learned it are mechanically identical.** So the memory engine *is* the grounding-signal accumulator, observed from the companion's side. One mechanism, two readouts:
- Read the memory's consolidation → "how well does the companion know this about her."
- Read the same node's context-distance spread → "how well has she learned this" (→ parent suite).

This means we don't build two systems. We build the memory graph, and the grounding signal is a *view* over it. Architectural economy that's also true to how minds work.

---

## 6. Gist vs. verbatim — what survives forgetting (and the privacy payoff)

Human memory loses verbatim detail fast but retains gist for a long time. The engine mirrors this with a two-level structure:

- **Episode (verbatim):** "the snail on the garden path, Tuesday morning." Short τ. Fades and prunes quickly.
- **Concept (gist):** "Naomi loves spirals." Lives in the **codebook** (§8). Long-lived; reinforced every time any spiral-episode is encoded, even after individual episodes are gone.

So three weeks later: the specific snail is forgotten, but "she loves spirals" remains — exactly how a fond adult remembers a child. **And this is the privacy story, structurally enforced:** the device retains a relationship-level understanding (gist) while the episode-level record of a child's specific daily life continuously decays out. It *cannot* become a detailed dossier, because the physics prune the details. Gist-persists-verbatim-fades is simultaneously the most human-feeling design and the most privacy-protective one.

---

## 7. The associative graph — spreading activation

Memories link to each other (concept edges, context edges, temporal edges) — the CIAER+ knowledge graph. When she shows the companion a snail, activation **spreads** along the edges:

```
activate(node):
  node.s refreshed
  for each neighbor: neighbor receives partial activation ∝ edge_weight
```

Related memories light up — other spirals, other small garden creatures, the time she made a spiral with her finger in sand. This is what lets the companion *associate* like a mind: "Ooh, a spiral — like that snail shell you found! And the swirl in your bathwater!" The callback feels like genuine remembering because, mechanically, it *is* associative retrieval. Spreading activation also drives which memories are "available" for the LLM mood-line/context each turn (the top-k most-activated nodes are summarized in).

---

## 8. The codebook — the interest profile as compressed memory

Straight from mp4Real §4.6, reused. Familiar patterns aren't stored as fresh episodes — they're encoded as a **reference to a codebook entry plus a small deviation vector** (how this instance differs). Novel patterns that match no entry expand the codebook.

The codebook **is the companion's model of who Naomi is** — her crystallized interest profile. It is:
- compact (compression, per MDL),
- the gist layer that survives episode-pruning (§6),
- what feeds the parent suite's "what she's into" map (Doc 1.13),
- what the inner-life **daily whim** can bias toward (the companion's "favorite of the day" tends to draw from her codebook),
- and what the LLM is conditioned on to feel like it *knows her*.

A new interest = a codebook expansion = a small, real milestone the companion can notice.

---

## 9. Memory → behavior (expression)

Memory must surface observably or it's dead weight (same rule as the inner-life expression layer):

| Behavior | Driven by | Example |
|---|---|---|
| **Callbacks** | high-strength related node | "Like that snail shell you found!" |
| **Open threads** | a node tagged unresolved | "Did you ever find that round rock you wanted?" |
| **Her vocabulary** | concept nodes she named | adopts *her* word for a thing, permanently |
| **Milestones** | codebook expansions / counts | "That's the tenth animal you've shown me!" |
| **Knowing her** | codebook summary in context | the companion's general warmth of familiarity |
| **Feeding Affection** | total consolidated memory mass | → inner-life `A` baseline rises (the compounding) |

That last row is the compounding with Doc 2.0: the more (and more consolidated) the shared memory, the higher the Affection baseline drifts, the warmer the companion becomes. Memory and inner life are not two systems bolted together — memory *feeds* the soul.

---

## 10. Same dynamics as the Inner Life Engine (architectural consistency)

Memory strength `s` with decay + reactivation-kick is structurally the *same* as an inner-life variable with homeostatic decay + event-kick (Doc 2.0 §3). The difference is only the substrate: continuous mood variables vs. discrete episodic nodes. This means:
- both are deterministic + bounded, run **on-device**, persist across sessions, work **offline** (aliveness and memory are both local);
- both are tuned the same way (decay rate, kick magnitude);
- the codebase can share the integrator.

The companion is therefore *one* dynamical organism: a small field of coupled mood variables (Inner Life) sitting atop a decaying, self-reinforcing graph of episodic memory (this doc), with the memory mass feeding the mood field's affection term. One physics.

---

## 11. Privacy — hard lines (restated, enforced by the physics)

1. On-device only; no raw media leaves; no cloud memory store of the child.
2. Beats and gist, never a transcript — and forgetting guarantees this structurally (§6).
3. Parent-owned, parent-deletable; parent can wipe any memory or the whole store.
4. The store cannot grow into a dossier: episode decay + prune floor cap the verbatim detail retained at any time.
5. No memory of the child is ever an external training set (Doc 1.7 hard lines).

---

## 12. Tuning knobs (feel-tuned, like the inner-life engine)

1. **Base τ & salience→τ scaling:** how long memories live, and how much salience extends them. The master "memory feel" knob.
2. **Salience gate threshold:** how selective encoding is (high = remembers only big moments; low = remembers more, risks clutter).
3. **Rehearsal kick ρ & consolidation factor c:** how strongly reactivation refreshes and stickifies. Drives the self-curation rate.
4. **Prune floor s_min:** when a memory is truly forgotten.
5. **Spreading-activation decay:** how far association reaches (tight = literal callbacks; loose = poetic, sometimes-odd associations — a little looseness reads as imaginative).
6. **Codebook match threshold:** how readily a new thing counts as "a known interest" vs. expands the profile.

Tune by simulation (Doc 2.1 simulator): encode a stream of moments, fast-forward weeks, and check that the *right* things survived and the relationship-gist crystallized while the daily specifics faded.

---

*Prism · Document 2.1 · The Memory Engine · on-device, decaying, self-curating · memory feeds the soul; forgetting protects the child; rehearsal is learning.*
