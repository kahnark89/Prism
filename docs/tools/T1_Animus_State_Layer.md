# Animus — Persistent AI State Layer
### Decouple Your Agent's Soul from the LLM

---

## The Bottleneck

Every AI agent or chatbot built on an LLM shares the same structural defect: the model is stateless, consistent, and always-available — which is exactly why it feels dead.

The standard fix is more prompting: longer persona descriptions, injected conversation history, memory APIs. None of it works because the fix is aimed at the wrong layer. The LLM is not broken. The architecture is wrong.

**Current developer workflow:**
```
User input → [system prompt + history] → LLM → response
                     ↑
            persona lives here
            personality lives here
            state lives here
            everything lives here
```

This produces agents that are:
- **Vendor-locked** — personality is baked into a specific model's system prompt; swap providers, lose the persona
- **Inconsistent across sessions** — stateless model reads a static description and improvises from scratch
- **Flat** — no rhythm, no organic change, no difference between day 2 and day 200
- **Offline-incapable** — if the API is unreachable, the agent ceases to exist

---

## The Pattern

> **The state engine (soul) and the LLM (mouth) are different systems, connected by one narrow compiled interface.**

The state engine runs locally, persistently, and offline. It carries the agent's current state — mood, energy, affection, history, growth. The LLM is a commodity voice layer. It receives a compiled natural-language paragraph describing the current state and speaks from it.

```
User input ─────────────────────────────────┐
                                            ↓
State Engine (local, persistent, offline)   LLM call
├── continuous state variables             ├── receives: mood-line paragraph
├── decay / homeostasis equations          ├── knows: nothing else about state
├── event kick system                      └── returns: response
├── circadian rhythm
└── episodic memory

         ↓ compile ↓
      mood-line paragraph
"You're feeling bright and curious right now.
 It's midday, your most engaged time.
 You've been thinking about the auth module lately."
```

The LLM conditions on the mood-line and speaks. The response is parsed for event tags (delight, confusion, surprise) that feed back into the state engine — not as raw text, but as typed events. The interface is narrow by design.

---

## How It Works

### State Schema

Define state as a set of continuous variables with physical dynamics:

```json
{
  "variables": ["mood", "energy", "curiosity", "affection", "focus"],
  "homeostasis_rate": 0.08,
  "coupling": {
    "energy": { "mood": 0.3, "curiosity": 0.25, "focus": 0.2 }
  },
  "circadian": {
    "peaks": ["09:00", "14:00"],
    "floor": 0.15
  }
}
```

### Update Equation

```
x(t+1) = clamp(
    x(t)
  + λ · (x₀_eff − x(t))        # homeostasis: return toward baseline
  + Σ κ_xj · (xj(t) − xj*)     # coupling: other variables pull on x
  + event_kick(t)               # event spike: what just happened
  + ε(t)                        # bounded autocorrelated noise
)
```

State ticks independently — on a background thread, on a schedule, or event-driven. The LLM is never in the loop for state updates.

### Mood-Line Compiler

Before each LLM call, the compiler converts live state into a natural-language paragraph:

```typescript
compile(state: AgentState, memory: MemoryGraph): string
```

Output is injected as the first block of the system prompt. The LLM never receives raw variable values — only the compiled description. This is the only interface.

### Event Feedback

After each LLM turn, the response is parsed for event markers:

```typescript
const events = parseEvents(response);
// e.g. [{ type: "delight", intensity: 0.7 }, { type: "confusion", intensity: 0.4 }]
stateEngine.apply(events);
```

Events kick the relevant state variables. The state engine integrates them. The LLM never touches state directly.

---

## Developer Workflow Integration

### Drop-in for any LLM SDK

```typescript
import { Animus } from 'animus-sdk';

const agent = new Animus({
  schema: './agent.schema.json',
  memory: './agent.memory.db'
});

// Before your existing LLM call — inject mood-line
const moodLine = agent.compile();
const messages = [
  { role: 'system', content: `${baseSystemPrompt}\n\n${moodLine}` },
  ...conversationHistory
];

// Your existing LLM call — unchanged
const response = await anthropic.messages.create({ messages, ... });

// After — feed events back
agent.apply(parseEvents(response.content));
```

Works with Anthropic SDK, OpenAI SDK, Google Gemini, Ollama, or any HTTP LLM endpoint. The state engine has no opinion about the LLM provider.

### LangChain / LlamaIndex

```python
from animus import AnimusMemory

# Drop into any LangChain chain as a custom memory class
memory = AnimusMemory(schema="agent.schema.json")
chain = ConversationChain(llm=llm, memory=memory)
```

### Offline Fallback

When the LLM is unreachable, the state engine keeps ticking. A local fallback layer reads the current mood-line and returns a templated response. The agent is still in a mood, still has a rhythm, still feels present — even with no network.

---

## What Developers Get

| Without Animus | With Animus |
|---|---|
| Persona in system prompt | Persona in state engine — truly persistent |
| Consistent across sessions (dead) | State-driven across sessions (alive) |
| Vendor-locked | Swap LLMs freely — state is yours |
| Falls silent offline | Degrades gracefully offline |
| Tuned by prompting | Tuned by physical parameters |
| Personality is rented | Personality is owned |

**Tuning is physical, not linguistic.** Dial `λ` to change how quickly moods recover. Adjust coupling to make energy dominate curiosity. Change event kick magnitudes per event type. No prompt rewriting. No model fine-tuning. Parameter inspection via a built-in simulator.

**The differentiating asset is yours.** The state definition — the baselines, coupling matrix, growth curve, memory salience function — is a JSON file you own. It runs on your infrastructure. It is not inside any vendor's API.

---

## Target Use Cases

- **AI companions / assistants** that need to feel consistent and growing over time (customer service agents, tutors, coaches, onboarding assistants)
- **Game NPCs** with genuine emotional arcs rather than scripted state machines
- **Developer tools** (like AI pair programmers) that should feel more engaged when you're on a roll and more subdued when you're debugging at midnight
- **Any agent** where "it feels dead" is a blocking complaint and "better prompting" hasn't fixed it

---

## Technical Surface

```
animus/
├── core/         state engine, update equations, event system
├── compiler/     mood-line assembly, variable binning, memory injection
├── memory/       episodic graph, salience decay, codebook
├── simulator/    browser-based inspector (React) — watch a day unfold
├── adapters/     LangChain, LlamaIndex, direct SDK wrappers
└── schema/       JSON Schema for agent definitions
```

**Install:**
```bash
npm install animus-sdk
pip install animus-sdk
```

**Try the simulator before writing code:** `npx animus simulate` opens a browser inspector showing state evolution across a simulated day. Dial parameters by hand and watch the agent's inner life respond in real time.

---

*Animus · developer tool · derived from Prism soul/mouth separation architecture · Capps Consulting Company LLC*
