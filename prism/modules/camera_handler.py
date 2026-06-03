from __future__ import annotations
import numpy as np
from prism.config import PrismConfig
from prism.hal.base import CameraHAL


class CameraHandler:
    def __init__(self, config: PrismConfig, camera: CameraHAL) -> None:
        self._cfg = config
        self._camera = camera

    async def capture(self) -> np.ndarray:
        """Return 640×480 RGB uint8 frame."""
        return await self._camera.capture_frame()
