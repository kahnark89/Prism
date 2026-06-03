from __future__ import annotations
from abc import ABC, abstractmethod
from dataclasses import dataclass
import numpy as np


@dataclass
class LedState:
    hue: float          # 0–360
    saturation: float   # 0–100
    lightness: float    # 0–100
    pulse_speed: float  # seconds per breath cycle
    pattern: str = "breathe"  # breathe | solid | sparkle | off


class CameraHAL(ABC):
    @abstractmethod
    async def capture_frame(self) -> np.ndarray:
        """Return 640×480 RGB uint8 NumPy array."""

    @abstractmethod
    async def start(self) -> None: ...

    @abstractmethod
    async def stop(self) -> None: ...


class MicHAL(ABC):
    @abstractmethod
    async def record_utterance(self, max_seconds: float = 10.0) -> bytes:
        """Return raw PCM bytes."""

    @abstractmethod
    async def start(self) -> None: ...

    @abstractmethod
    async def stop(self) -> None: ...


class SpeakerHAL(ABC):
    @abstractmethod
    async def speak(self, text: str, rate: int = 150, volume: float = 1.0) -> None: ...

    @abstractmethod
    async def play_tone(self, frequency: float, duration_s: float, volume: float = 0.5) -> None: ...

    @abstractmethod
    async def play_sound(self, sound_id: str) -> None: ...


class LedHAL(ABC):
    @abstractmethod
    async def set_state(self, state: LedState) -> None: ...

    @abstractmethod
    async def off(self) -> None: ...


class HapticHAL(ABC):
    @abstractmethod
    async def pulse(self, pattern: str = "single", intensity: float = 0.8) -> None: ...

    @abstractmethod
    async def off(self) -> None: ...


class GpioHAL(ABC):
    @abstractmethod
    def read_button(self, button_id: str) -> bool: ...

    @abstractmethod
    def register_button_callback(self, button_id: str, cb) -> None: ...

    @abstractmethod
    def cleanup(self) -> None: ...


@dataclass
class HALBundle:
    camera: CameraHAL
    mic: MicHAL
    speaker: SpeakerHAL
    led: LedHAL
    haptic: HapticHAL
    gpio: GpioHAL

    async def start_all(self) -> None:
        await self.camera.start()
        await self.mic.start()

    async def stop_all(self) -> None:
        await self.camera.stop()
        await self.mic.stop()
        await self.led.off()
        await self.haptic.off()
        self.gpio.cleanup()
