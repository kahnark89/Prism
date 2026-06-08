from __future__ import annotations
from prism.config import PrismConfig
from prism.hal.base import HALBundle
from prism.hal.camera import MockCameraHAL, PiCameraHAL
from prism.hal.audio_in import MockMicHAL, PiMicHAL
from prism.hal.audio_out import MockSpeakerHAL, PiSpeakerHAL
from prism.hal.led import MockLedHAL, PiNeoPixelLedHAL
from prism.hal.haptic import MockHapticHAL, PiHapticHAL
from prism.hal.gpio import MockGpioHAL, PiGpioHAL


class HALFactory:
    @staticmethod
    def create(config: PrismConfig) -> HALBundle:
        if config.use_mock_hal:
            return HALBundle(
                camera=MockCameraHAL(config),
                mic=MockMicHAL(config),
                speaker=MockSpeakerHAL(config),
                led=MockLedHAL(config),
                haptic=MockHapticHAL(config),
                gpio=MockGpioHAL(config),
            )
        return HALBundle(
            camera=PiCameraHAL(config),
            mic=PiMicHAL(config),
            speaker=PiSpeakerHAL(config),
            led=PiNeoPixelLedHAL(config),
            haptic=PiHapticHAL(config),
            gpio=PiGpioHAL(config),
        )
