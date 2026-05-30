# The Soul/Mouth Separation
### An Architectural Pattern for AI Systems That Feel Alive

---

## The Core Claim

Every AI system designed to feel like more than a search engine faces the same failure mode: the model is consistent, always-available, and stateless — which is exactly why it feels dead. The standard response is to make the model smarter, more empathetic, better at small talk. This is the wrong lever.

Prism resolves this with one architectural decision:

> **The soul and the mouth are different systems, running on different substrates, connected by a narrow compiled interface.**

The **soul** is a deterministic + bounded-stochastic dynamical system running on-device, persistently, offline. It carries the creature's state: its mood, energy, curiosity, affection, the day's whim, its rhythm, its slow growth over months, its episodic memory of shared moments.

The **mouth** is the cloud LLM. It receives a compiled natural-language summary of the current soul state — the *mood-line* — and speaks. That's all it knows. The rest is the soul's business.

This separation is the invention. Not the toy. Not the characters. Not the curriculum. The separation.

---

## Why the Standard Approach Fails

The standard approach to AI personality is to put everything in the prompt: a long system prompt with a persona description, maybe some example exchanges, and then the LLM is expected to be consistent. This produces:

- **Inconsistency across sessions.** The model has no persistent state; it reads a static description and improvises. It reads "you are a curious character" the same way on day 1 and day 200.
- **Mood as assertion, not as state.** "You are curious" tells the LLM to assert curiosity in its text. It doesn't make it curious. The child can tell.
- **No organic change.** A system prompt doesn't drift, doesn't have a circadian rhythm, doesn't grow more affectionate over months of time together.
- **Dependence on the specific LLM.** The whole personality is inside one vendor's API. Swap the model, lose the persona.
- **No offline capability.** If the API is unreachable, the creature simply doesn't exist.

These aren't implementation bugs. They're structural consequences of fusing soul and mouth into a single stateless system.

---

## The Soul: A Layered Dynamical Organism

In Prism, the soul is a **coupled damped dynamical system with periodic forcing** — formally the same family as a network of leaky integrators. It is not a rules engine. It is not a decision tree. It is a physics.

**Four timescale layers:**

| Layer | Timescale | What it carries |
|---|---|---|
| L0 — Traits | months | personality constants; the slow growth of baselines |
| L1 — Mood | hours–day | the medium-term affective state; five coupled continuous variables |
| L2 — Reaction | seconds | event-driven spikes; fast decay back into L1 |
| R — Rhythm | 24h cycle | circadian energy/alertness envelope |

**The five L1 state variables**, each ∈ [0, 1]:
- **M** (Mood): grumpy ↔ joyful
- **E** (Energy): sleepy ↔ bouncy — the master variable; drags the others via coupling
- **C** (Curiosity): mellow ↔ fascinated
- **A** (Affection): warm ↔ devoted — fed by memory + time; grows over months
- **S** (Sociability): quiet ↔ chatty

**The update equation** (one variable per tick):
```
x(t+1) = clamp01(
    x(t)
  + λ · (x₀_eff − x(t))          # homeostasis: pull toward rhythm-shifted baseline
  + Σⱼ κₓⱼ · (xⱼ(t) − xⱼ*)       # coupling: other variables pull on x
  + event_kick_x(t)               # L2 spike from what just happened
  + ε_x(t)                        # bounded autocorrelated noise
)
```

The homeostasis term (λ) is critical: without it, the system random-walks to the walls and dies there. With it, moods linger the right amount and always return to a recognizable self. λ ≈ 0.08 is the master feel knob.

The **coupling matrix** is what turns five drifting variables into one legible organism. Energy → everything is the load-bearing link: low energy drags mood, curiosity, and sociability. A tired creature is quieter, less curious, a little flatter. A four-year-old reads "sleepy" without being told.

The **memory engine** (Doc 2.1) is the second pillar of the soul. Episodic nodes decay via `s(t) = s₀ · exp(−Δt/τ)`, where τ scales with salience. They consolidate on rehearsal: each time a concept reappears, τ lengthens. Forgotten specifics crystallize into gist in the codebook. The codebook feeds the daily whim. The consolidated memory mass lifts the Affection baseline. Memory and inner life are not two systems — memory feeds the soul.

**What the soul provides:**
- State that actually changes based on what happens (not assertion)
- Organic drift that produces recognizable but not predictable behavior
- A circadian rhythm so the creature has a day — the single cheapest and most powerful aliveness signal
- Persistent memory of shared moments — what makes it feel different on day 40 than day 1
- Slow growth that makes it measurably different in month three than at first meeting
- All of this offline, on-device, without a network call

---

## The Mouth: The LLM Conditioned by State

The cloud LLM knows nothing about the state engine. It receives, at each turn, a single compiled paragraph:

> *"You're Pip. Right now you feel bright and bouncy and joyful; you're fascinated, full of questions. It's midday, your most energetic time. You're especially charmed by spirals today. You've spent a lot of time with Naomi lately and feel very fond of her."*

This paragraph — regenerated from live state at each turn — is the **mood-line**. It is the sole interface between soul and mouth.

The mood-line compiler bins each variable into 2–3 descriptive bands, assembles companion-specific language, injects the top-3 spreading-activated memory nodes as a sentence, and adds time-of-day context. The LLM then produces an utterance that is consistent with that state, in character, on topic.

The same companion speaks differently at 8am and 8pm, on day 2 and day 200, after a reunion and after a long idle. Not because the LLM is different — because the soul that conditions it is different.

---

## What the Separation Buys

**LLM portability.** The soul is pure software owned by the architect. The LLM is a commodity service. Swap Claude for Gemini for GPT-4 for a locally-run model: the soul persists, the mood-line works the same way, the creature remains continuous. No personality dies in a model upgrade.

**Offline aliveness.** The soul runs on-device with no network. When the API is unreachable, the inner life keeps ticking — the circadian rhythm continues, event kicks still fire, the creature is still in a mood. The rich language degrades to a fallback ("I'm feeling curious today…"), but the *aliveness* doesn't disappear. This is the genotype's "aliveness is local" invariant.

**Regulatory surface reduction.** The only thing that crosses to the cloud is the mood-line and the object label/photo. The child's face, voice, memory, and learning data never leave the device. The soul's state is local. The mouth sends one paragraph and receives one response.

**Ownership.** The soul is the differentiating asset. It is the thing the architect owns. The LLM is a rented mouth. An architecture that puts the soul in the prompt has outsourced the differentiating value to an API provider.

**Tuning without retraining.** The soul's feel is dialed in by adjusting physical parameters: λ (homeostasis), coupling strengths, rhythm depth, noise magnitude, event kick sizes, growth rate. No fine-tuning. No labeled data. No model training. A React simulator (inner_life_simulator.jsx) lets the architect watch a day unfold and dial in the feel by inspection.

---

## The Mood-Line as a Design Surface

The mood-line is undervalued in the current spec. It is the highest-leverage surface in the system because improvements multiply across every LLM interaction.

Current implementation: 3-band binning per variable, template assembly, no memory injection, no companion-specific vocabulary.

High-leverage improvements:
1. **Memory sentence injection** — top-3 spreading-activated nodes summarized in one sentence. "You've been thinking about spirals lately — the snail she found, and the one in the pinecone." This makes the LLM feel like it *knows* her.
2. **Companion-specific vocabulary in the compiler** — Pip's bands use "ooh, fascinating"; Lumi's use "gently"; the Brave One's use "a little nervous but". The mood-line reads in character before the LLM even starts.
3. **Open thread surfacing** — if a memory node is tagged `unresolved`, surface it once per session: "You've been wondering whatever happened to that round rock she wanted to find." This makes the companion feel like it maintains a relationship, not just a conversation.

---

## Where This Pattern Generalizes

The soul/mouth separation solves the right problem for any AI system meant to feel like more than a search engine. The domains where this directly applies:

**Elder care companions** — the soul provides circadian alignment, memory of family history, and affection that grows over years of shared life. The mouth (LLM) speaks warmly and in the moment. Swap the LLM if a better model ships — the 5-year relationship doesn't restart.

**Therapeutic companions** — the soul tracks the user's emotional arc over weeks, maintains memory of disclosed difficulties (high salience), and has its own calibrated state (not performatively empathetic, but actually in a receptive mood). The mouth delivers the response. The soul is the therapy substrate.

**Language learning companions** — the soul tracks vocabulary acquisition (the codebook IS the competency map), maintains a circadian that matches the learner's study schedule, and grows genuinely more familiar with the learner's domain over time. The mouth conducts the conversation in the target language.

**Professional companions** — the soul holds the growing competency model of a new employee, tracks their grounding confidence per concept, and grows more "collegial" as trust accumulates. The mouth handles the dialogue. Swap models as better ones arrive — the onboarding continuity persists.

In every case, the value proposition is the same: the soul is ownable, persistent, offline-capable, and portable. The mouth is a commodity. Never fuse them.

---

## The Failure Mode to Avoid

The separation is only as strong as the narrowness of the interface. If the mouth starts receiving raw state variables, or if the soul starts being influenced by the mouth's output beyond the event system, the separation degrades.

The mood-line must remain the *only* interface: a compiled, natural-language summary that the LLM conditions on but does not modify. The soul updates based on events (what she showed, her reaction, time of day) — not based on what the LLM said. The LLM is stateless with respect to the soul. It reads the mood-line and speaks. That is all.

Keep the interface narrow. Keep the soul on-device. Keep the mouth in the cloud. The creature lives at the interface.

---

*Soul/Mouth Separation · Prism architectural analysis · Capps Family Enterprises*
