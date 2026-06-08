from __future__ import annotations
import asyncio
import pytest
import numpy as np
from pathlib import Path
from prism.config import PrismConfig
from prism.hal.factory import HALFactory
from prism.hal.base import HALBundle
from prism.engines.inner_life import InnerLifeEngine, InnerLifeState
from prism.engines.memory import MemoryEngine
from prism.engines.mood_line import MoodLineCompiler
from prism.engines.grounding import GroundingAccumulator
from prism.modules.safety import SafetyModule
from prism.personas import get_persona


@pytest.fixture
def config(tmp_path) -> PrismConfig:
    return PrismConfig(
        use_mock_hal=True,
        data_dir=str(tmp_path),
        child_name="Naomi",
        active_companion="pip",
    )


@pytest.fixture
def hal(config) -> HALBundle:
    return HALFactory.create(config)


@pytest.fixture
def pip_persona():
    return get_persona("pip")


@pytest.fixture
def inner_life_engine(config, pip_persona) -> InnerLifeEngine:
    return InnerLifeEngine(
        config,
        base_M=pip_persona.base_M,
        base_E=pip_persona.base_E,
        base_C=pip_persona.base_C,
        base_A=pip_persona.base_A,
        base_S=pip_persona.base_S,
    )


@pytest.fixture
def memory_engine(config) -> MemoryEngine:
    return MemoryEngine(config)


@pytest.fixture
def mood_compiler(config) -> MoodLineCompiler:
    return MoodLineCompiler(config)


@pytest.fixture
def grounding(config) -> GroundingAccumulator:
    return GroundingAccumulator(config)


@pytest.fixture
def safety(config) -> SafetyModule:
    return SafetyModule(config)


@pytest.fixture
def mock_frame() -> np.ndarray:
    frame = np.zeros((480, 640, 3), dtype=np.uint8)
    frame[100:300, 150:400] = [180, 60, 60]
    return frame
