from __future__ import annotations
import asyncio
import logging
import threading
from typing import Callable
from prism.hal.base import GpioHAL
from prism.config import PrismConfig

log = logging.getLogger(__name__)


class MockGpioHAL(GpioHAL):
    """Mock GPIO that uses keyboard events (space=shutter) on non-Pi hardware."""

    def __init__(self, config: PrismConfig) -> None:
        self._callbacks: dict[str, list[Callable]] = {}
        self._button_states: dict[str, bool] = {}
        self._listener_thread: threading.Thread | None = None
        self._running = False

    def read_button(self, button_id: str) -> bool:
        return self._button_states.get(button_id, False)

    def register_button_callback(self, button_id: str, cb: Callable) -> None:
        self._callbacks.setdefault(button_id, []).append(cb)
        if not self._running:
            self._start_keyboard_listener()

    def trigger_button(self, button_id: str) -> None:
        """Programmatically trigger a button (for tests)."""
        for cb in self._callbacks.get(button_id, []):
            cb()

    def _start_keyboard_listener(self) -> None:
        self._running = True
        try:
            from pynput import keyboard  # type: ignore

            KEY_MAP = {keyboard.Key.space: "shutter"}

            def on_press(key):
                btn = KEY_MAP.get(key)
                if btn:
                    log.info("[GPIO] key pressed → button '%s'", btn)
                    for cb in self._callbacks.get(btn, []):
                        cb()

            listener = keyboard.Listener(on_press=on_press)
            self._listener_thread = threading.Thread(target=listener.start, daemon=True)
            self._listener_thread.start()
        except ImportError:
            log.debug("pynput not available; keyboard GPIO mock disabled")

    def cleanup(self) -> None:
        self._running = False


class PiGpioHAL(GpioHAL):
    BUTTON_PINS = {"shutter": 23, "char_a": 24, "char_b": 25}

    def __init__(self, config: PrismConfig) -> None:
        self._gpio = None
        try:
            import RPi.GPIO as GPIO  # type: ignore
            self._gpio = GPIO
            GPIO.setmode(GPIO.BCM)
            for pin in self.BUTTON_PINS.values():
                GPIO.setup(pin, GPIO.IN, pull_up_down=GPIO.PUD_UP)
        except Exception:
            pass

    def read_button(self, button_id: str) -> bool:
        if not self._gpio:
            return False
        pin = self.BUTTON_PINS.get(button_id)
        if pin is None:
            return False
        return not self._gpio.input(pin)

    def register_button_callback(self, button_id: str, cb: Callable) -> None:
        if not self._gpio:
            return
        pin = self.BUTTON_PINS.get(button_id)
        if pin is None:
            return
        import RPi.GPIO as GPIO  # type: ignore
        GPIO.add_event_detect(pin, GPIO.FALLING, callback=lambda _: cb(), bouncetime=300)

    def cleanup(self) -> None:
        if self._gpio:
            self._gpio.cleanup()
