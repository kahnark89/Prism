# Prism — Document 2.2: The Parent Suite — Converged UX

*How the grounding-signal view, the environment-shaping tool, and absence-visibility stop being three product surfaces and become one mechanism seen from three angles — and what that means for what the parent actually sees and touches.*

---

## 1. The convergence: three asks are one structure

Doc 1.8 §3 specified a curriculum view (exposure → reappearance → grounding). Genotype Principle 12 (seated by Epigenome 020) specified a curation/counter-balance tool that must be "fully inspectable — absence shown as clearly as presence." Phenotype §1 queued these as if they were two features plus a third constraint (absence-visibility) to bolt on.

They aren't three things. They're one structure read from three ends:

- **The grounding-signal view** is the *output* read of a concept's life in the device: what's happened to it since it was offered (exposure → reappearance → grounding curve, Doc 1.8 §2).
- **The environment-shaping tool** is the *input* write to the same concept: is it currently on offer, how often, alongside what.
- **Absence-visibility** is not a fourth feature layered on top of the shaping tool to make it auditable. It is the structural fact that *every concept Prism could offer renders somewhere*, in one of a small set of explicit states — and "not currently offered" is simply one of those states, with its own reason and a one-tap reversal. There is no code path that can draw "present" without being able to draw "absent" from the same record, because they're the same field.

**Core claim — one structure, call it the Map:** a per-concept record carrying, at once —
1. **menu status** — on-menu / dormant / off-menu (with reason), counter-balance-added
2. **exposure history** — when and how it's come up (feeds the confidence curve, Doc 1.8 §2.1)
3. **grounding level** — exploring → getting it → owns it (the accumulating, continuous confidence)
4. **provenance** — *why* it's in its current state: parent added it, parent removed it, age-gate, counter-balance, or simply "not reached yet"

Collapsing these into one record is the structural-coherence move heuristic #5 calls for — and it's what makes Principle 12's inspectability requirement *automatic* rather than policed: the same grid that shows the parent "she's getting 'spiral'" is the grid that shows "you've never been offered anything from the natural-science domain — here's what that would look like." One mechanism; the safety property falls out of its shape rather than being checked against it after the fact.

---

## 2. The Map — primary surface

A navigable grid/constellation of concepts and domains (animals, shapes, science ideas, social-emotional concepts, places, etc.). Each tile renders **one of four states**, visually distinct but equally legible — none hidden, none emphasized by omission of the others:

| State | What it means | What the parent sees |
|---|---|---|
| **Active** | On the menu, has appeared, grounding in progress | exposure count, current band (exploring/getting it/owns it), trajectory arrow |
| **Dormant** | On the menu, hasn't come up recently | "on offer, hasn't surfaced lately" + a pacing nudge ("bring this forward?") |
| **Absent — by your hand** | Parent removed or never added it | reason shown verbatim ("you set this aside on [date]" / "age-gated until ~6") + one-tap "add to the menu" |
| **Absent — not yet reached** | Exists in Prism's library, never surfaced | plainly marked as such — *not* indistinguishable from a tile that doesn't exist |

The fourth row is the load-bearing one: it's what stops "absence" from quietly collapsing into "doesn't exist." A parent scanning the Map sees the *whole library*, not a highlight reel of what made the cut — exactly the "absence shown as clearly as presence" Principle 12 requires, and exactly what would let a regulator or a skeptical co-parent audit the thing without taking the company's word for it.

---

## 3. The Trajectory — secondary surface (the story, not the dashboard)

A longitudinal, narrative view — closer to a memory book than an analytics panel, in keeping with "aliveness over consistency" (Principle 7) and the project's general allergy to dashboard-as-metaphor for a relationship. Shows:

- her curiosity *moving* across domains over weeks (the CIAER Result→Cause arc, Doc 1.7 §2) — "three weeks ago it was all dinosaurs; this week she keeps circling back to how things float"
- concepts crossing into "owns it" rendered as small milestones, in plain language, never as a score (Hard Line 6)
- the same Map data, replayed in time rather than laid out in space — one dataset, two lenses (spatial = Map, temporal = Trajectory), so nothing the parent sees in one view is invisible from the other

This view is where the "private map of exposure → reappearance → grounding... and her curiosity trajectory" from Doc 1.8 §3 actually lives — it was always a *story* read of the Map, not a separate report.

---

## 4. Controls — what the parent actually touches

All controls write to the same per-concept record (§1), so every action immediately repaints both the Map and the Trajectory — there is no separate "curation mode" with its own state:

- **Add / remove from the menu** — direct toggle on any tile. Removing doesn't delete the concept from Prism's library (nothing the child has already encountered is erased from her own grounding record — Hard Line 4 governs *Prism's* memory of *her*, not the parent's curation choices); it moves the tile to "Absent — by your hand," with the reason auto-logged (so the parent can later remember *why* they set it aside).
- **Pacing** — a simple push-more / hold-back / let-it-breathe control per active concept, feeding the suggested-topics signal to the LLM (Doc 1.8 §3, "parent sets the *what*, LLM optimizes the *how*").
- **Boundaries** — two distinct controls, kept visually distinct so they're never confused: *off-limits* (hard exclude, parent-set, no auto-expiry) vs. *wait for later* (age-gate, carries an approximate unlock age, auto-promotes to "on offer" when she crosses it — with the parent notified, not surprised).
- **Counter-balance** — the TV-controls-extended capability seated in Epigenome 020: when an influence the parent didn't choose and couldn't block is shaping her world (a show, a relative's framing, a peer's enthusiasm), the parent can add *weight* to the menu — more of another perspective, another character "into" the counter-topic — without ever touching what's already landed in her. This is **adding to the menu, never editing the diner**: the tool has no control surface that targets something already internal to her (no "reduce her attachment to X," only "offer more of Y"). That asymmetry isn't a UI choice — it's the only form the control can take, because Hard Line 12 means the other form doesn't exist in the system at all. **Discoverability — settled (2026-06-08, Epigenome 023):** *permanent, always-visible control, full stop.* The architect's ruling: *"keep it a glass box. nothing hidden from the parent."* This also dissolves the recursive worry logged in Epigenome 021 (would the suite have to "notice a skew" in the parent's choices to surface this contextually, which is itself a form of modeling the parent?) — a permanent control needs no such inference. The tool is simply always there, like every other control on the Map; the suite never has to *decide* whether to show it to *this* parent.

---

## 5. The volunteer channel — kept separate, kept light

The one outside-world confirmation channel (Doc 1.8 §3) is deliberately **not** folded into the Map's controls — mixing "tell us what you saw in the world" with "configure what the device offers" would blur the line between *measurement* and *curation*, and the device must never feel like it's auditing the parent's parenting.

It surfaces instead as occasional, optional, non-nagging prompts at natural moments (end-of-day digest, not mid-task interruption): *"Have you heard her say 'spiral' on her own this week?"* — single tap, dismissible forever per-concept, defaulting to **off** for any parent who never engages it. A parent who taps nothing still gets a fully working product from context-variety alone (Doc 1.8 §2.3) — this channel is a bonus signal, never a requirement, and its absence from a household's usage is itself unremarkable and untracked-as-a-metric (tracking *engagement with the measurement tool* would be the compliance trap one layer up).

---

## 6. Pre-activation transparency — the parent test-drives the whole system first

**Settled (2026-06-08, Epigenome 023).** The architect's ruling on build order went further than "build a prototype" — it named a product requirement: *"yes for transparency — we don't want it to be a surprise later for the parent. They should be able to view and test the whole system before deciding to let the child play with it."*

This is the temporal half of Principle 11 ("never more influential than it is inspectable") that the doc hadn't yet named: inspectability can't only arrive *alongside* influence (the live parent suite, reviewed after sessions happen) — it has to be available **before the parent ever commits the child to a session.** A glass box you can only inspect after the fact is half a glass box.

**What this means concretely — a Parent Preview / Test-Drive Mode:**

- **It runs the real system, not a demo of it.** Same companion logic, same safety gates, same input/output pipeline, same logging — fed by the parent's own test inputs (point the camera at a mug, ask it something off-topic, see what gets through and what gets redirected) instead of the child's. **No gap between the demo and the product** — the single most trust-corroding pattern in consumer tech, and the opposite of "glass box."
- **It walks the whole loop, not just the suite.** Hear each companion persona's voice and tone. See a sample object-recognition pass, including the fast brain being visibly, admittedly wrong sometimes (Design Principle 1 — that's a feature to preview, not a bug to hide). Trip the safety gate on purpose and see exactly what happens. Open the parent suite itself with seed/sample data so the Map and Trajectory aren't a cold, unfamiliar surface on day one.
- **It's where first configuration happens — before, not during, the child's first session.** Character selection, topic boundaries, age tier, all the controls in §4 — set once, calmly, with full information, rather than improvised mid-session under a curious toddler's gaze. The child's *first* real session is then already inside bounds the parent chose with full knowledge, not a cold start the parent has to react to.
- **Nothing the parent discovers in preview mode is new information once the child starts using the device.** That's the test: would anything the parent sees *after* a week of real use be a surprise, given what preview mode showed them on day one? If yes, preview mode is incomplete — extend it until the answer is no.

This also resolves the latent tension in §5 (the volunteer channel must stay low-friction and undemanding for parents who never engage it): preview mode is where a parent *forms* their trust in the system, once, up front — which is exactly what makes it safe for the ongoing relationship to ask so little of them afterward.

---

## 7. Why this passes the tests

- **Menu vs. diner (Hard Line 12 / Epigenome 020):** every control surface in §4 writes to "what's offered," none to "what's already hers." The structural absence of the second kind of control is what makes this safe in any parent's hands, not a promise about any parent's intentions.
- **Absence shown as clearly as presence (Principle 12):** not a feature — a consequence of the Map's four-state design (§2). There is no way to render the grid without rendering all four states from the same record.
- **Invisible to the child, transparent to the parent (Hard Line 9 / one-line test "Privacy"):** the entire surface lives on the parent's device/login; nothing here has a child-facing analog, by design — the suite *is* the transparency mechanism for everything else Prism does.
- **No visible score (Hard Line 6 — broadened, Epigenome 022):** grounding is rendered in the three plain-language bands, in narrative (Trajectory) or status-tile (Map) form — **never** a number, percentage, or progress bar, anywhere, for anyone. The architect closed this outright: *"Never show numbered scores for the parent or anywhere in the device."* No drill-down, no power-user view, no exception for the parent suite — banded language is now the only legal representation of grounding confidence on any surface. This forecloses the overjustification trap from re-emerging one layer up: a parent who can see "73%" starts optimizing toward "73% → 100%," which re-imports the exact compliance-signal pathology Doc 1.8 exists to keep out of the child's experience.
- **Aliveness over dashboard (Principle 7):** the Trajectory view exists specifically so the *primary emotional read* of the suite is "watching her grow," not "monitoring a system." The Map is the configuration surface; the Trajectory is the keepsake.

---

## 8. Questions raised, and how they closed

All four questions this doc originally raised are now settled by the architect (2026-06-08):

1. ~~**Naming**~~ — keep "the Map" / "the Trajectory"; revisit only if something better surfaces during the build.
2. ~~**Numeric confidence, exposed or not**~~ — **genotype-level:** *"Never show numbered scores for the parent or anywhere in the device."* Hard Line 6 broadened (Epigenome 022) — banded language only, every surface, no exceptions, no drill-down. See §7.
3. ~~**Counter-balance discoverability**~~ — *"keep it a glass box. nothing hidden from the parent. permanent control."* Settled in §4: always-visible, no contextual surfacing, no inference about the parent required (Epigenome 023).
4. ~~**Build order**~~ — *"yes for transparency... they should be able to view and test the whole system before deciding to let the child play with it."* This named a real product requirement beyond "build a prototype": a **Parent Preview / Test-Drive Mode**, specified in §6 (Epigenome 023).

No open questions remain on this thread. Future questions belong in `10_PHENOTYPE.md §3` as they arise (e.g., during prototype build or when the awakening-choreography thread surfaces overlap with this one — the preview mode in §6 will need to walk the awakening sequence too, once that spec exists).

---

*Prism · Document 2.2 · The Parent Suite — Converged UX · builds on Doc 1.8 §3, Genotype Principle 12, Epigenome 014/020 · Capps Consulting Company LLC*
