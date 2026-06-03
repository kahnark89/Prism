from __future__ import annotations
import asyncio
from prism.hal.base import MicHAL
from prism.config import PrismConfig


class MockMicHAL(MicHAL):
    def __init__(self, config: PrismConfig) -> None:
        self._responses: list[bytes] = []
        self._idx = 0

    async def start(self) -> None:
        pass

    async def stop(self) -> None:
        pass

    async def record_utterance(self, max_seconds: float = 10.0) -> bytes:
        await asyncio.sleep(0.1)
        if self._responses:
            r = self._responses[self._idx % len(self._responses)]
            self._idx += 1
            return r
        return b""

    def queue_response(self, audio: bytes) -> None:
        self._responses.append(audio)


class PiMicHAL(MicHAL):
    def __init__(self, config: PrismConfig) -> None:
        self._config = config

    async def start(self) -> None:
        pass

    async def stop(self) -> None:
        pass

    async def record_utterance(self, max_seconds: float = 10.0) -> bytes:
        # Real implementation uses sounddevice or pyaudio
        try:
            import sounddevice as sd  # type: ignore
            import numpy as np
            loop = asyncio.get_event_loop()
            sample_rate = 16000
            frames = int(max_seconds * sample_rate)
            audio = await loop.run_in_executor(
                None, lambda: sd.rec(frames, samplerate=sample_rate, channels=1, dtype="int16")
            )
            sd.wait()
            return audio.tobytes()
        except ImportError:
            return b""
