from __future__ import annotations
import asyncio
import logging
import time
from datetime import datetime
from typing import Optional

from prism.config import PrismConfig
from prism.hal.base import HALBundle
from prism.engines.inner_life import InnerLifeEngine
from prism.engines.memory import MemoryEngine
from prism.engines.mood_line import MoodLineCompiler
from prism.engines.grounding import GroundingAccumulator
from prism.modules.camera_handler import CameraHandler
from prism.modules.vision_engine import VisionEngine
from prism.modules.recognition import RecognitionModule
from prism.modules.perspective_engine import PerspectiveEngine, PerspectiveRequest
from prism.modules.audio_feedback import AudioFeedback
from prism.modules.safety import SafetyModule
from prism.modules.learning_log import LearningLog, CIAERRecord
from prism.modules.ui_controller import UIController, UIMode
from prism.awakening import AwakeningMachine, AwakeningState
from prism.event_bus import EventBus, INNER_LIFE_TICKED, MEMORY_ENCODED, AWAKENING_TRIGGERED
from prism.personas import get_persona

log = logging.getLogger(__name__)


class Orchestrator:
    def __init__(
        self,
        config: PrismConfig,
        hal: HALBundle,
        inner_life: InnerLifeEngine,
        memory: MemoryEngine,
        mood_line: MoodLineCompiler,
        grounding: GroundingAccumulator,
        camera: CameraHandler,
        vision: VisionEngine,
        recognition: RecognitionModule,
        perspective: PerspectiveEngine,
        audio: AudioFeedback,
        safety: SafetyModule,
        learning_log: LearningLog,
        ui: UIController,
        awakening: AwakeningMachine,
        event_bus: EventBus,
    ) -> None:
        self._cfg = config
        self._hal = hal
        self._inner_life = inner_life
        self._memory = memory
        self._mood_line = mood_line
        self._grounding = grounding
        self._camera = camera
        self._vision = vision
        self._recognition = recognition
        self._perspective = perspective
        self._audio = audio
        self._safety = safety
        self._log = learning_log
        self._ui = ui
        self._awakening = awakening
        self._bus = event_bus

        self._running = False
        self._event_count = 0
        self._last_interaction = time.monotonic()
        self._is_online = True

        # Persistence stores — injected after DB init
        self._inner_life_store = None
        self._memory_store = None
        self._grounding_store = None
        self._session_store = None

        # Subscribe to events
        self._bus.subscribe(AWAKENING_TRIGGERED, self._on_awakening_triggered)

    def set_stores(self, inner_life_store, memory_store, grounding_store, session_store) -> None:
        self._inner_life_store = inner_life_store
        self._memory_store = memory_store
        self._grounding_store = grounding_store
        self._session_store = session_store

    async def run(self) -> None:
        self._running = True
        persona = get_persona(self._cfg.active_companion)
        log.info("Orchestrator starting — companion: %s, child: %s",
                 persona.name, self._cfg.child_name)

        if self._session_store:
            await self._session_store.start_session(persona.id)

        # Register shutter button
        self._hal.gpio.register_button_callback(
            "shutter", lambda: asyncio.create_task(self.handle_shutter_press())
        )

        await asyncio.gather(
            self._inner_life_tick_loop(),
            self._memory_decay_loop(),
            self._save_state_loop(),
            self._idle_watch_loop(),
        )

    async def shutdown(self) -> None:
        self._running = False
        log.info("Orchestrator shutting down — saving state…")
        await self._save_all()
        if self._session_store:
            await self._session_store.end_session(self._event_count)
        await self._hal.stop_all()

    # ─── Hot path ────────────────────────────────────────────────────

    async def handle_shutter_press(self) -> None:
        """
        Full pipeline on shutter press:
        camera → vision → (optional recognition) → inner life event →
        safety → mood_line → perspective → safety → audio →
        learning_log → memory → grounding → ui
        """
        self._last_interaction = time.monotonic()
        self._event_count += 1

        persona = get_persona(self._cfg.active_companion)
        now = time.time()
        dt_now = datetime.fromtimestamp(now)
        t_hours = dt_now.hour + dt_now.minute / 60.0

        # 1. Capture frame
        frame = await self._camera.capture()

        # 2. Vision inference
        vision_result = self._vision.infer(frame)
        log.info("[shutter] vision: %s (%.0f%%)", vision_result.label,
                 vision_result.confidence * 100)

        # 3. Recognition check (non-blocking to inner life)
        if not self._awakening.is_awakened and self._recognition.is_enrolled():
            rec_result = self._recognition.recognize(frame)
            if rec_result.is_enrolled_child:
                await self._awakening.check_and_trigger(
                    is_enrolled_child=True,
                    recognition_confidence=rec_result.confidence,
                )

        # 4. Inner life event
        if self._awakening.is_mechanical:
            mode = UIMode.MECHANICAL
        else:
            mode = UIMode.AWAKENED

        prev_concepts = [n.concept for n in self._memory.get_top_activated(k=5)]
        event = "repeat" if vision_result.label in prev_concepts else "novel"
        self._inner_life.fire_event(event)
        state = self._inner_life.tick(now)

        # 5. UI thinking cue (non-blocking)
        asyncio.create_task(self._ui.thinking_cue())

        # 6. Input safety
        safety_in = self._safety.check_input(vision_result.label)
        effective_label = safety_in.sanitized or "something interesting"

        # 7. Mood-line + memory
        top_mems = self._memory.get_top_activated(concept_hint=vision_result.label, k=3)
        mood_line = self._mood_line.compile(state, persona, self._cfg.child_name, top_mems, t_hours)
        mem_summary = self._mood_line._memory_sentence(top_mems)

        # 8. Perspective engine (async, awaited — timeout handled internally)
        req = PerspectiveRequest(
            vision_label=effective_label,
            vision_confidence=vision_result.confidence,
            mood_line=mood_line,
            memory_summary=mem_summary,
            child_question=None,
            persona=persona,
            is_online=self._is_online,
        )
        response = await self._perspective.generate(req)
        log.info("[%s%s] %s", persona.name,
                 " (fallback)" if response.is_fallback else "",
                 response.text)

        # 9. Audio (output safety already applied inside perspective engine)
        await self._audio.speak(response.text, persona, state)

        # 10. Memory encode
        salience = min(1.0, vision_result.confidence + (0.1 if event == "novel" else 0.0))
        self._memory.encode(
            concept=vision_result.label,
            episode=f"{vision_result.label} (session)",
            salience=salience,
        )
        await self._bus.publish_async(MEMORY_ENCODED, {"concept": vision_result.label})

        # 11. Grounding exposure
        self._grounding.record_exposure(vision_result.label)

        # 12. Learning log (CIAER record)
        record: CIAERRecord = {
            "timestamp": now,
            "cause": vision_result.label,
            "intuition": vision_result.label,
            "confidence_in_voice": "SKILL",
            "action": "snap",
            "effect": response.text,
            "reaction": "observed",
            "salience": salience,
            "result": "none",
            "shadow_actions": [],
            "pre_env_time_of_day": t_hours,
            "pre_env_companion_mood": mood_line[:80],
            "pre_env_child_recent_concepts": prev_concepts[:3],
        }
        self._log.record_event(record)
        if self._session_store:
            await self._session_store.increment_events()

        # 13. UI update
        await self._ui.update_from_state(state, persona, mode)

    # ─── Background loops ─────────────────────────────────────────────

    async def _inner_life_tick_loop(self) -> None:
        while self._running:
            try:
                now = time.time()
                state = self._inner_life.tick(now)
                persona = get_persona(self._cfg.active_companion)
                mode = UIMode.AWAKENED if self._awakening.is_awakened else UIMode.MECHANICAL
                await self._ui.update_from_state(state, persona, mode)
                await self._bus.publish_async(INNER_LIFE_TICKED, {"state": state})
            except Exception as e:
                log.error("Inner life tick error: %s", e)
            await asyncio.sleep(self._cfg.tick_interval_s)

    async def _memory_decay_loop(self) -> None:
        while self._running:
            await asyncio.sleep(self._cfg.decay_interval_s)
            try:
                day = time.time() / 86400.0
                pruned = self._memory.advance_time(day)
                if pruned:
                    log.debug("Memory pruned %d nodes", len(pruned))
                mass = self._memory.get_affection_mass()
                self._inner_life.apply_affection_boost(mass)
            except Exception as e:
                log.error("Memory decay error: %s", e)

    async def _save_state_loop(self) -> None:
        while self._running:
            await asyncio.sleep(self._cfg.save_interval_s)
            await self._save_all()

    async def _save_all(self) -> None:
        try:
            persona = get_persona(self._cfg.active_companion)
            if self._inner_life_store:
                await self._inner_life_store.save(persona.id, self._inner_life.get_state())
            if self._memory_store:
                nodes, cb = self._memory.get_snapshot()
                await self._memory_store.save(nodes, cb)
            if self._grounding_store:
                await self._grounding_store.save(self._grounding.get_snapshot())
            log.debug("State saved")
        except Exception as e:
            log.error("Save failed: %s", e)

    async def _idle_watch_loop(self) -> None:
        while self._running:
            await asyncio.sleep(30.0)
            idle_s = time.monotonic() - self._last_interaction
            if idle_s >= self._cfg.idle_timeout_s:
                self._inner_life.fire_event("idle")

    # ─── Event handlers ───────────────────────────────────────────────

    def _on_awakening_triggered(self, payload: dict) -> None:
        """Runs the 5-beat awakening sequence asynchronously."""
        asyncio.create_task(self._run_awakening_sequence())

    async def _run_awakening_sequence(self) -> None:
        persona = get_persona(self._cfg.active_companion)
        state = self._inner_life.get_state()
        await asyncio.gather(
            self._audio.play_awakening_sequence(self._cfg.child_name, persona),
            self._ui.awakening_bloom(),
            self._ui.heartbeat_pulse(),
        )
        self._awakening.complete_awakening()
        self._inner_life.fire_event("reunion")
