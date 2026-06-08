from __future__ import annotations
import asyncio
import logging
import time
from dataclasses import dataclass
from typing import Optional
from prism.config import PrismConfig
from prism.modules.safety import SafetyModule, SafetyVerdict
from prism.personas.base import CompanionPersona

log = logging.getLogger(__name__)

_OFFLINE_TEMPLATES = {
    "lo": "Hmm... I'm feeling a bit slow right now. That looks like {label}!",
    "mid": "Oh, that looks like {label}! I wonder about it.",
    "hi": "Oh wow! That's a {label}! How exciting!",
}


@dataclass
class PerspectiveRequest:
    vision_label: str
    vision_confidence: float
    mood_line: str
    memory_summary: str
    child_question: Optional[str]
    persona: CompanionPersona
    is_online: bool


@dataclass
class PerspectiveResponse:
    text: str
    is_fallback: bool
    persona_name: str
    latency_ms: float


class PerspectiveEngine:
    def __init__(self, config: PrismConfig, safety: SafetyModule) -> None:
        self._cfg = config
        self._safety = safety
        self._client = None
        self._init_client()

    def _init_client(self) -> None:
        try:
            import anthropic  # type: ignore
            self._client = anthropic.AsyncAnthropic()
        except ImportError:
            log.warning("anthropic SDK not installed; LLM calls will fall back to mock")

    async def generate(self, req: PerspectiveRequest) -> PerspectiveResponse:
        t0 = time.monotonic()

        # Input safety gate (synchronous, always runs)
        input_text = f"{req.vision_label} {req.child_question or ''}"
        safety_in = self._safety.check_input(input_text)
        if safety_in.verdict == SafetyVerdict.BLOCK:
            return PerspectiveResponse(
                text=_OFFLINE_TEMPLATES["mid"].format(label="something interesting"),
                is_fallback=True,
                persona_name=req.persona.name,
                latency_ms=(time.monotonic() - t0) * 1000,
            )

        if not req.is_online or self._client is None:
            return self._fallback(req, t0)

        try:
            text = await asyncio.wait_for(
                self._call_llm(req),
                timeout=self._cfg.llm_timeout_s,
            )
        except (asyncio.TimeoutError, Exception) as e:
            log.warning("LLM call failed: %s; using fallback", e)
            return self._fallback(req, t0)

        # Output safety gate (synchronous, always runs)
        safety_out = self._safety.check_output(text)
        if safety_out.verdict == SafetyVerdict.BLOCK:
            return self._fallback(req, t0)
        final_text = safety_out.sanitized

        return PerspectiveResponse(
            text=final_text,
            is_fallback=False,
            persona_name=req.persona.name,
            latency_ms=(time.monotonic() - t0) * 1000,
        )

    async def _call_llm(self, req: PerspectiveRequest) -> str:
        system = self._build_system_prompt(req)
        user_msg = self._build_user_message(req)

        response = await self._client.messages.create(
            model=self._cfg.llm_model,
            max_tokens=120,
            system=system,
            messages=[{"role": "user", "content": user_msg}],
        )
        return response.content[0].text.strip()

    @staticmethod
    def _build_system_prompt(req: PerspectiveRequest) -> str:
        return (
            f"{req.persona.system_preamble}\n\n"
            f"MOOD: {req.mood_line}\n\n"
            f"MEMORY: {req.memory_summary or 'No specific memories yet.'}\n\n"
            "Rules: respond in 1–2 short warm sentences max. "
            "Never say anything scary, violent, or sad. "
            "End with a question or invitation to explore."
        )

    @staticmethod
    def _build_user_message(req: PerspectiveRequest) -> str:
        confidence_pct = int(req.vision_confidence * 100)
        msg = f"I see: {req.vision_label} ({confidence_pct}% sure)."
        if req.child_question:
            msg += f" Child asks: {req.child_question}"
        return msg

    def _fallback(self, req: PerspectiveRequest, t0: float) -> PerspectiveResponse:
        if req.persona.fallback_phrases:
            import random
            band = "hi" if req.vision_confidence > 0.66 else ("mid" if req.vision_confidence > 0.34 else "lo")
            phrases = req.persona.fallback_phrases.get(band, ["How interesting!"])
            text = random.choice(phrases)
        else:
            band = "hi" if req.vision_confidence > 0.66 else "mid"
            text = _OFFLINE_TEMPLATES[band].format(label=req.vision_label)

        return PerspectiveResponse(
            text=text,
            is_fallback=True,
            persona_name=req.persona.name,
            latency_ms=(time.monotonic() - t0) * 1000,
        )
