# Prism Developer Tools

Two standalone tools extracted from Prism's architecture. Each lives in its own private repo.

| Tool | Repo | Purpose |
|---|---|---|
| Animus | kahnark89/animus-sdk | Persistent AI state layer — decouple your agent's soul from the LLM |
| Cortex | kahnark89/cortex-dev | AI collaboration governance — session coherence + comprehension depth |

## Applying a tool to any project

```bash
# Add Animus to any project (scaffolds state schema + AGENTS.md snippet)
npx animus-sdk init

# Add Cortex to any project (bootstraps .genome/ + .cortex/ + AGENTS.md)
npx cortex-dev init
```

## Relationship to Prism

Both tools were designed while building Prism but solve domain-general problems:

- **Animus** solves the "AI agents feel dead" problem for any agent/chatbot developer
- **Cortex** solves the "AI sessions lose context and drift" problem for any team using AI coding assistants on long-running projects

Prism uses both internally: Animus drives the companion inner life; Cortex governs this repo's cross-session coherence via `.genome/`.
