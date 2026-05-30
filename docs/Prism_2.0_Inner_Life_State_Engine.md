# Prism — Document 2.0: The Inner Life State Engine

*The dynamical system that makes the companion feel alive. Fully specified: state variables, update equations, coupling, rhythm, homeostasis, growth, bounded unpredictability, and the mapping from internal state to behavior a four-year-old can read. All on-device, persistent, offline-capable. The LLM is conditioned by this engine; it does not contain the soul.*

> **Design thesis (restated, sharpened):** An inner life must feel *spontaneous but never random*, *recognizable but never predictable*, *responsive but never erratic*. These tensions resolve through a **layered dynamical system** — variables at different timescales, coupled into a single organism, pulled gently toward baseline, perturbed by events and rhythm. Aliveness is not a feature you add; it is a system you tune.

---

## 1. The four timescale layers

Aliveness reads as real when slow things and fast things coexist coherently — your circadian rhythm colors but never erases your personality. Four layers, slowest to fastest:

| Layer | Variable kind | Timescale | What it is | Changes via |
|---|---|---|---|---|
| **L0 — Traits** | who it *is* | months | personality constants + slow growth | growth dynamics (§6) only |
| **L1 — Mood** | how it *feels today* | hours–day | the medium-term affective state | drift + events + coupling + rhythm |
| **L2 — Reaction** | this *moment* | seconds | momentary spikes from what just happened | events; decays fast back into L1 |
| **R — Rhythm** | the *day* | 24h cycle | circadian energy/alertness envelope | clock (deterministic) |

L2 rides on L1 rides on L0, with R modulating throughout. The companion you meet at any instant is the **sum**: trait baseline + today's mood + this moment's reaction + where it is in its day.

---

## 2. The state variables (L1 — the core)

Five bounded continuous variables, each in [0, 1]. These are the medium-term mood state — the heart of the system.

| Var | Name | 0 ↔ 1 | Primary behavioral readout |
|---|---|---|---|
| `M` | **Mood** | grumpy ↔ joyful | tone, word warmth, LED hue |
| `E` | **Energy** | sleepy ↔ bouncy | speech pace, volume, utterance length |
| `C` | **Curiosity** | mellow ↔ fascinated | how many questions it asks, how it pursues a topic |
| `A` | **Affection** | warm ↔ devoted | use of her name, callbacks, "I missed you" |
| `S` | **Sociability** | quiet ↔ chatty | how much it initiates vs. waits |

Plus two scalars that aren't moods but shape them:
- `W` — **the daily whim**: a categorical "favorite of the day" (a color, a shape, a kind of thing), reseeded each morning. Biases `C` toward matching objects.
- `t` — **time of day**, feeding the rhythm `R(t)`.

**L0 traits** are the *baselines* each L1 variable is pulled toward: `M₀, E₀, C₀, A₀, S₀` — the companion's personality. The Curious One has high `C₀`; the Gentle One high `A₀`, lower `S₀`. Growth (§6) nudges these baselines over months.

---

## 3. The update equation (one variable, then the system)

Each L1 variable evolves in discrete ticks (a tick ≈ one interaction beat, or a periodic background step when idle). For a variable `x` with trait baseline `x₀`:

```
x(t+1) = clamp01(
    x(t)
  + λ · (x₀_eff − x(t))        # homeostasis: pull toward (rhythm-shifted) baseline
  + Σ_j  κ_xj · (x_j(t) − x_j*) # coupling: other variables tug on x
  + event_kick_x(t)            # L2 reactions injected here, then they decay
  + ε_x(t)                     # bounded unpredictability (§7), small
)
```

- `λ` (**homeostasis rate**, ~0.05–0.15): how fast it returns to baseline. Small = moods linger; large = snaps back fast. This is the restoring force that prevents random-walk drift to the extremes — the damping term. **Without it the system dies at a wall.**
- `x₀_eff = x₀ + rhythm_shift_x · R(t)` — the effective baseline is the trait baseline *shifted by the daily rhythm* (energy's baseline sags at night, peaks midday).
- `κ_xj` (**coupling coefficients**): how variable `j` pulls variable `x` (§4). `x_j*` is `j`'s neutral point (~0.5).
- `event_kick`: discrete perturbations from what just happened (§5).
- `ε`: small bounded noise (§7).

This is a **coupled damped system with periodic forcing and discrete perturbation** — formally the same family as a network of leaky integrators / damped oscillators. Tuning it is exactly the dynamical-systems feel-work you do.

---

## 4. The coupling matrix (this is what makes it a creature, not sliders)

The single most important design decision: the variables are **not independent.** Coupling is what turns five drifting numbers into one legible organism. The matrix `κ_xj` (how column j affects row x):

|        | ←M | ←E | ←C | ←A | ←S |
|--------|----|----|----|----|----||
| **M**  | —  | +0.10 | +0.05 | +0.04 | 0 |
| **E**  | +0.06 | — | +0.08 | 0 | +0.03 |
| **C**  | +0.05 | +0.10 | — | 0 | +0.04 |
| **A**  | +0.03 | 0 | +0.02 | — | +0.03 |
| **S**  | +0.05 | +0.12 | +0.06 | +0.04 | — |

Read the load-bearing couplings:
- **Energy → everything.** Low energy drags mood, curiosity, and sociability down. This is the big one: a tired creature is quieter, less curious, a little flatter — and a kid reads "sleepy" instantly. `E` is the master variable.
- **Curiosity ↔ Energy** are mutually reinforcing: getting fascinated wakes it up; being awake makes it curious. This pair can create gentle positive feedback (a "spark" of engagement) — bounded by homeostasis so it never runs away.
- **Mood → Sociability:** a good mood makes it chattier; a grumpy morning makes it withdrawn-but-present.
- **Affection** is weakly coupled *in* (it's mostly fed by memory/time, §6) but it gently lifts mood and sociability — being devoted to her makes it warmer overall.

Couplings are deliberately small (≤0.12) so the system stays stable; the *interaction* of many small pulls is what produces organic-feeling state, not any single strong link.

---

## 5. Events — how the world perturbs the mood (L2 → L1)

Events inject `event_kick` spikes that decay fast (L2), leaving a residue in L1. Examples (kick magnitudes on a [0,1] scale, applied then decayed at rate ~0.4/tick):

| Event | M | E | C | A | S | note |
|---|---|---|---|---|---|---|
| She shows it something novel | +.05 | +.08 | +.20 | +.03 | +.05 | curiosity spike — the core loop |
| She delights / laughs | +.15 | +.10 | +.05 | +.08 | +.08 | the best signal |
| Object matches today's whim `W` | +.06 | +.05 | +.18 | 0 | +.04 | "ooh, my favorite!" |
| She returns after absence | +.10 | +.06 | +.05 | +.15 | +.10 | "I missed you" |
| Long idle / she walks away | −.03 | −.05 | −.06 | 0 | −.08 | settles, quiets |
| Repeated same object | 0 | −.04 | −.10 | 0 | −.03 | mild "we've seen this" |
| Bedtime approaches (via R) | +.04 | −.15 | −.08 | +.06 | −.06 | dreamy, soft |

The asymmetry matters: positive social events (delight, reunion) hit `A` and `M` hardest (relationship-building); novelty hits `C` hardest (learning loop). The companion's "personality" is partly *which events move it most* — a trait-level event-sensitivity vector can differentiate companions further.

---

## 6. Circadian rhythm `R(t)` — the cheapest aliveness, formalized

`R(t)` is a smooth periodic envelope over the day, ∈ [−1, 1], that shifts effective baselines (mainly `E`, and through coupling, everything). A clean form:

```
R(t) = base_curve(t)        # double-bump: morning rise, midday peak, afternoon dip, evening fall
```

A practical implementation is a sum of two gaussians (morning + afternoon peaks) minus an evening decay, or a simple piecewise smooth curve keyed to the child's actual schedule (parent-set wake/nap/bed times). Key features that must be present:
- **Morning:** low and *rising* — sleepy, warming up. ("Good morning… *yawn*… oh, hi.")
- **Midday:** peak energy/curiosity — bounciest, most playful.
- **Afternoon:** a dip (the nap-time sag) — mellower.
- **Evening:** descending — dreamy, quiet, affectionate. Energy floor pulls everything toward calm.

The rhythm does real parenting work: the companion *cannot* be hyper at bedtime because `R(t)` has dragged `E`'s baseline to the floor and coupling has pulled `C` and `S` down with it. Calm is structural, not scripted.

> A child learns this rhythm within days. A creature with a predictable *day* is unmistakably alive — and the predictability of the rhythm is the *good* kind (recognizable), distinct from the predictability of behavior (mechanical) that we avoid elsewhere.

---

## 7. The unpredictability budget — bounded, honest, not random

"Slightly surprising = alive; predictable = toy" — but **random ≠ alive; random = broken.** The noise term `ε_x` is tightly bounded and structured:

- **Magnitude:** small (|ε| ≤ ~0.03 per tick) — it nudges, never lurches.
- **Autocorrelated, not white:** `ε(t+1) = 0.8·ε(t) + small_random` — so "moods" of noise drift gently rather than jittering. This is the difference between a creature that's "in a bit of a funny mood today" and one that twitches.
- **Occasional structured surprise (the *whim* and rare *spark*):** beyond the noise floor, a low-probability event (~once per session) can fire a small structured behavior — humming, noticing something unprompted, a tiny opinion. These are sampled from a *curated* set, never freeform, and **never touch safety** (the budget is locked out of any safety-relevant path — hard line from the genotype).

The honesty: unpredictability is a *bounded perturbation on a stable system*, not a license for the LLM to do anything. The system's attractor structure (homeostasis + coupling) guarantees it always settles back into a recognizable self.

---

## 8. Growth — slow trait drift over months (L0)

Separate from daily mood: the trait baselines `x₀` themselves migrate, slowly and one-directionally, so the companion is measurably different in month three than at first meeting.

```
x₀(month+1) = x₀(month) + γ · growth_signal_x
```
- `γ` is tiny (months-scale).
- `growth_signal` is driven by accumulated relationship history: total time together lifts `A₀` (it grows more devoted); repeated brave moments lift the Brave One's effective courage (its dilemma slowly resolves); words she teaches it enter its vocabulary permanently.
- Growth is **one-directional and never resets** — this is what lets her feel, looking back, "it's different now than when we met." The deepest aliveness signal, and nearly free (just persistence + a slow integrator).

Growth also slowly *resolves each companion's dilemma* (master doc §7) — the Brave One gets a little braver over a year — which models that people grow, and gives the relationship a narrative arc.

---

## 9. Expression — state → behavior a 4-year-old can read

State that doesn't *cause perceivable change* is dead weight. Every variable maps to observable output. This is the layer that makes the inner life *visible*.

| Channel | Driven by | Mapping |
|---|---|---|
| **Voice pace** | `E` | low E → slow, soft, longer pauses; high E → quick, lively |
| **Voice pitch/energy** | `E`, `M` | bright & lifted when high; gentle & low when sleepy |
| **Word warmth** | `M`, `A` | grumpy → terser, still kind; joyful+devoted → effusive, uses her name |
| **Question rate** | `C` | mellow → mostly states; fascinated → asks, follows up, wonders aloud |
| **Initiation** | `S` | quiet → waits for her; chatty → notices things first |
| **LED hue** | `M` (+ companion signature) | warm amber (content) → bright gold (joyful) → soft blue (sleepy/dreamy) → dim (grumpy) |
| **LED motion** | `E` | slow breathing pulse (low E) → lively shimmer (high E) |
| **Topic bias** | `W` | gently over-attends to today's whim category |
| **Callbacks** | `A` + memory | high A → more references to shared history |

The LED is doing heavy lifting for a pre-reader: **color and motion communicate mood pre-verbally.** A blue slow-breathing Prism at night *reads as sleepy* without a word.

---

## 10. The mood-line — how state conditions the LLM

The LLM never sees the numbers. At each turn, the engine compiles current state into a short natural-language **mood-line** prepended to the companion's system prompt. The state→language compiler bins each variable into 2–3 descriptive bands and assembles:

> *"You're [Pip]. Right now you're feeling sleepy and content — it's getting near bedtime, so you're dreamy and soft-spoken, savoring one thing at a time rather than bouncing around. You're especially charmed by anything **round** today. You've spent a lot of time with Naomi lately and feel very fond of her."*

That single paragraph, regenerated each turn from the live state, is what makes the same companion speak differently at 8am and 8pm, on day 2 and day 200. The dynamical system is the soul; the mood-line is how the soul reaches the mouth.

**Determinism note:** the state engine is fully deterministic + bounded-stochastic and runs on-device; only the mood-line crosses to the cloud LLM. So aliveness persists offline (the engine still runs, expression still works, only the rich language degrades) — *aliveness is local*, per the genotype.

---

## 11. Tuning philosophy (for the architect)

This is a feel-tuned system, like dialing in a process. The knobs, in order of impact:
1. **`λ` (homeostasis):** the master feel knob. Too high = no moods (snaps to baseline); too low = drifts to walls and sticks. Start ~0.08.
2. **Energy's rhythm depth:** how much the day swings. Too flat = no daily character; too deep = manic/comatose. 
3. **Coupling strengths:** raise Energy→others to make it more "of a piece"; lower to make variables more independent (riskier).
4. **Event kicks:** how reactive it is to her. Big delight-kicks = expressive; small = stoic.
5. **Noise autocorrelation + magnitude:** the texture of spontaneity. 
6. **Growth rate `γ`:** how fast it becomes a different creature. Months, not days.

Tune by *watching a simulated day* and asking the one-line test: *would a 4-year-old believe it's alive, and be sad it might be lonely while she's at school?*

---

*Prism · Document 2.0 · The Inner Life State Engine · on-device, persistent, offline-capable · the soul is the engine, the LLM is the mouth.*
