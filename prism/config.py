from __future__ import annotations
from dataclasses import dataclass, field
from pathlib import Path
import yaml


@dataclass
class PrismConfig:
    # Hardware
    use_mock_hal: bool = True
    camera_resolution: tuple[int, int] = (640, 480)

    # Inner Life Engine
    lambda_homeostasis: float = 0.08
    noise_magnitude: float = 0.02
    noise_autocorr: float = 0.8
    rhythm_depth: float = 1.0
    coupling_scale: float = 1.0
    event_kick_decay: float = 0.6
    growth_rate_per_day: float = 0.012
    growth_cap: float = 0.18

    # Memory Engine
    tau_base: float = 2.0
    tau_salience_scale: float = 14.0
    salience_gate: float = 0.35
    rehearsal_kick: float = 0.5
    consolidation_factor: float = 0.35
    prune_floor: float = 0.05
    spreading_activation: float = 0.4
    codebook_strength_per_encode: float = 0.25
    codebook_salience_bonus: float = 0.2

    # Perspective / LLM
    llm_provider: str = "anthropic"
    llm_model: str = "claude-haiku-4-5-20251001"
    llm_timeout_s: float = 8.0
    offline_fallback_enabled: bool = True

    # Safety
    safety_blocked_patterns: list[str] = field(default_factory=list)

    # Circadian
    wake_hour: float = 7.0
    nap_hour: float = 13.0
    bed_hour: float = 19.5

    # Persistence
    data_dir: str = "~/.prism/data"

    # Dashboard
    dashboard_host: str = "0.0.0.0"
    dashboard_port: int = 8080

    # Companion / child
    active_companion: str = "pip"
    child_name: str = "Naomi"

    # Orchestrator timing
    tick_interval_s: float = 5.0
    decay_interval_s: float = 1800.0
    save_interval_s: float = 120.0
    idle_timeout_s: float = 180.0

    @property
    def data_path(self) -> Path:
        return Path(self.data_dir).expanduser()

    @property
    def db_path(self) -> Path:
        return self.data_path / "prism.db"

    @property
    def recognition_db_path(self) -> Path:
        return self.data_path / "recognition.db"


def load_config(path: str | Path = "config/default.yaml") -> PrismConfig:
    p = Path(path)
    if not p.exists():
        return PrismConfig()
    with open(p) as f:
        raw = yaml.safe_load(f) or {}
    # camera_resolution comes in as a list from YAML
    if "camera_resolution" in raw:
        raw["camera_resolution"] = tuple(raw["camera_resolution"])
    known = {k: v for k, v in raw.items() if hasattr(PrismConfig, k) or k in PrismConfig.__dataclass_fields__}
    return PrismConfig(**known)
