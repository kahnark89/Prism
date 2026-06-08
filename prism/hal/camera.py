from __future__ import annotations
import asyncio
import numpy as np
from prism.hal.base import CameraHAL
from prism.config import PrismConfig


class MockCameraHAL(CameraHAL):
    def __init__(self, config: PrismConfig) -> None:
        h, w = config.camera_resolution[1], config.camera_resolution[0]
        self._frame = np.zeros((h, w, 3), dtype=np.uint8)
        # Paint a simple pattern so vision mock has something to work with
        self._frame[100:200, 100:200] = [180, 80, 80]

    async def start(self) -> None:
        pass

    async def stop(self) -> None:
        pass

    async def capture_frame(self) -> np.ndarray:
        await asyncio.sleep(0.05)
        return self._frame.copy()

    def set_test_frame(self, frame: np.ndarray) -> None:
        self._frame = frame.copy()


class PiCameraHAL(CameraHAL):
    def __init__(self, config: PrismConfig) -> None:
        self._config = config
        self._cam = None

    async def start(self) -> None:
        try:
            from picamera2 import Picamera2  # type: ignore
            self._cam = Picamera2()
            w, h = self._config.camera_resolution
            cfg = self._cam.create_still_configuration(main={"size": (w, h), "format": "RGB888"})
            self._cam.configure(cfg)
            self._cam.start()
        except ImportError:
            pass

    async def stop(self) -> None:
        if self._cam:
            self._cam.stop()

    async def capture_frame(self) -> np.ndarray:
        if self._cam is None:
            return np.zeros((480, 640, 3), dtype=np.uint8)
        loop = asyncio.get_event_loop()
        return await loop.run_in_executor(None, self._cam.capture_array)
