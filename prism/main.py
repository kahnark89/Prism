from __future__ import annotations
import asyncio
import logging
import signal
import sys
import threading
import time
from pathlib import Path

log = logging.getLogger(__name__)


def _setup_logging() -> None:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s  %(levelname)-8s  %(name)s  %(message)s",
        datefmt="%H:%M:%S",
    )
    logging.getLogger("httpx").setLevel(logging.WARNING)
    logging.getLogger("anthropic").setLevel(logging.WARNING)
    logging.getLogger("uvicorn").setLevel(logging.WARNING)


async def _open_db(path: Path, init_fn):
    import aiosqlite
    path.parent.mkdir(parents=True, exist_ok=True)
    conn = await aiosqlite.connect(str(path))
    await init_fn(conn)
    return conn


async def _async_main(config_path: str = "config/default.yaml") -> None:
    from prism.config import load_config
    from prism.hal.factory import HALFactory
    from prism.engines.inner_life import InnerLifeEngine
    from prism.engines.memory import MemoryEngine
    from prism.engines.mood_line import MoodLineCompiler
    from prism.engines.grounding import GroundingAccumulator
    from prism.modules.camera_handler import CameraHandler
    from prism.modules.vision_engine import VisionEngine
    from prism.modules.recognition import RecognitionModule
    from prism.modules.perspective_engine import PerspectiveEngine
    from prism.modules.audio_feedback import AudioFeedback
    from prism.modules.safety import SafetyModule
    from prism.modules.learning_log import LearningLog
    from prism.modules.ui_controller import UIController
    from prism.awakening import AwakeningMachine
    from prism.event_bus import EventBus
    from prism.orchestrator import Orchestrator
    from prism.personas import get_persona
    from prism.persistence.schema import init_main_db, init_recognition_db
    from prism.persistence.inner_life_store import InnerLifeStore
    from prism.persistence.memory_store import MemoryStore
    from prism.persistence.learning_log_store import LearningLogStore
    from prism.persistence.grounding_store import GroundingStore
    from prism.persistence.session_store import SessionStore
    from prism.persistence.recognition_store import RecognitionStore

    # ── Config ──────────────────────────────────────────────────────
    cfg = load_config(config_path)
    log.info("Config loaded — companion=%s child=%s mock=%s",
             cfg.active_companion, cfg.child_name, cfg.use_mock_hal)

    # ── HAL ─────────────────────────────────────────────────────────
    hal = HALFactory.create(cfg)
    await hal.start_all()

    # ── Databases ───────────────────────────────────────────────────
    main_conn = await _open_db(cfg.db_path, init_main_db)
    rec_conn  = await _open_db(cfg.recognition_db_path, init_recognition_db)

    # ── Persistence stores ──────────────────────────────────────────
    il_store  = InnerLifeStore(cfg);  il_store.set_conn(main_conn)
    mem_store = MemoryStore(cfg);     mem_store.set_conn(main_conn)
    log_store = LearningLogStore(cfg); log_store.set_conn(main_conn)
    grd_store = GroundingStore(cfg);  grd_store.set_conn(main_conn)
    ses_store = SessionStore(cfg);    ses_store.set_conn(main_conn)
    rec_store = RecognitionStore(cfg); rec_store.set_conn(rec_conn)

    # ── Engines ─────────────────────────────────────────────────────
    persona = get_persona(cfg.active_companion)
    inner_life = InnerLifeEngine(
        cfg,
        base_M=persona.base_M, base_E=persona.base_E,
        base_C=persona.base_C, base_A=persona.base_A, base_S=persona.base_S,
    )
    memory   = MemoryEngine(cfg)
    mood_line_compiler = MoodLineCompiler(cfg)
    grounding = GroundingAccumulator(cfg)

    # ── Load persisted state ────────────────────────────────────────
    saved_il = await il_store.load(persona.id)
    if saved_il:
        inner_life.load_state(saved_il)
        log.info("Inner life state restored (days_together=%.1f)", saved_il.days_together)

    saved_nodes, saved_cb = await mem_store.load()
    if saved_nodes or saved_cb:
        memory.load_snapshot(saved_nodes, saved_cb)
        log.info("Memory restored (%d nodes, %d codebook entries)",
                 len(saved_nodes), len(saved_cb))

    saved_grounding = await grd_store.load()
    if saved_grounding:
        grounding.load_snapshot(saved_grounding)
        log.info("Grounding state restored (%d concepts)", len(saved_grounding))

    # Set memory clock to current day
    memory.set_day(time.time() / 86400.0)

    # ── Modules ─────────────────────────────────────────────────────
    safety    = SafetyModule(cfg)
    camera    = CameraHandler(cfg, hal.camera)
    vision    = VisionEngine(cfg)
    vision.load_model()
    recognition = RecognitionModule(cfg)
    perspective = PerspectiveEngine(cfg, safety)
    audio     = AudioFeedback(cfg, hal.speaker)
    ui        = UIController(cfg, hal.led, hal.haptic)
    event_bus = EventBus()
    awakening = AwakeningMachine(cfg, event_bus)

    # ── Learning log ────────────────────────────────────────────────
    learning_log = LearningLog(cfg, grounding)
    learning_log.set_store(log_store)

    # Load recent log into memory
    recent_records = await log_store.load_recent(200)
    for r in recent_records:
        learning_log._records.append(r)

    # ── Orchestrator ────────────────────────────────────────────────
    orchestrator = Orchestrator(
        config=cfg, hal=hal,
        inner_life=inner_life, memory=memory,
        mood_line=mood_line_compiler, grounding=grounding,
        camera=camera, vision=vision, recognition=recognition,
        perspective=perspective, audio=audio, safety=safety,
        learning_log=learning_log, ui=ui,
        awakening=awakening, event_bus=event_bus,
    )
    orchestrator.set_stores(il_store, mem_store, grd_store, ses_store)

    # ── Dashboard ───────────────────────────────────────────────────
    _start_dashboard(cfg, memory, grounding, learning_log, log_store, recognition, awakening)

    # ── Graceful shutdown ───────────────────────────────────────────
    loop = asyncio.get_running_loop()

    def _handle_signal():
        log.info("Shutdown signal received")
        asyncio.create_task(_shutdown(orchestrator, main_conn, rec_conn))

    for sig in (signal.SIGINT, signal.SIGTERM):
        try:
            loop.add_signal_handler(sig, _handle_signal)
        except NotImplementedError:
            pass  # Windows

    log.info("Prism running — press SPACE to simulate shutter press")
    await orchestrator.run()


async def _shutdown(orchestrator, main_conn, rec_conn) -> None:
    await orchestrator.shutdown()
    await main_conn.close()
    await rec_conn.close()
    log.info("Shutdown complete")
    sys.exit(0)


def _start_dashboard(cfg, memory, grounding, learning_log, log_store, recognition, awakening) -> None:
    try:
        import uvicorn
        from prism.dashboard.app import app, wire_dependencies
        wire_dependencies(cfg, memory, grounding, learning_log, log_store, recognition, awakening)

        def _run():
            uvicorn.run(app, host=cfg.dashboard_host, port=cfg.dashboard_port,
                        log_level="warning")

        t = threading.Thread(target=_run, daemon=True)
        t.start()
        log.info("Dashboard running at http://%s:%d", cfg.dashboard_host, cfg.dashboard_port)
    except ImportError:
        log.warning("uvicorn not installed — dashboard unavailable")


def main() -> None:
    _setup_logging()
    import argparse
    parser = argparse.ArgumentParser(description="Prism AI companion device")
    parser.add_argument("--config", default="config/default.yaml",
                        help="Path to config YAML file")
    args = parser.parse_args()
    asyncio.run(_async_main(args.config))


if __name__ == "__main__":
    main()
