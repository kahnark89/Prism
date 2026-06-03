from __future__ import annotations
import logging
from dataclasses import dataclass
from typing import Optional
import numpy as np
from prism.config import PrismConfig

log = logging.getLogger(__name__)

# Mock label set (used when TFLite model is unavailable)
_MOCK_LABELS = [
    "apple", "banana", "orange", "cat", "dog", "flower",
    "car", "book", "cup", "chair", "tree", "bird",
]


@dataclass
class VisionResult:
    label: str
    confidence: float           # [0,1]
    box: tuple[int, int, int, int]  # x, y, w, h pixels
    raw_frame: np.ndarray


class VisionEngine:
    def __init__(self, config: PrismConfig) -> None:
        self._cfg = config
        self._interpreter = None
        self._labels: list[str] = _MOCK_LABELS
        self._input_details = None
        self._output_details = None
        self._mock_idx: int = 0

    def load_model(self, model_path: str = "models/mobilenet_v1.tflite",
                   labels_path: str = "models/labels.txt") -> None:
        try:
            import tflite_runtime.interpreter as tflite  # type: ignore
            self._interpreter = tflite.Interpreter(model_path=model_path)
            self._interpreter.allocate_tensors()
            self._input_details = self._interpreter.get_input_details()
            self._output_details = self._interpreter.get_output_details()
            try:
                with open(labels_path) as f:
                    self._labels = [l.strip() for l in f if l.strip()]
            except FileNotFoundError:
                pass
            log.info("TFLite model loaded from %s", model_path)
        except (ImportError, Exception) as e:
            log.info("TFLite unavailable (%s); using mock inference", e)

    def infer(self, frame: np.ndarray) -> VisionResult:
        if self._interpreter is None:
            return self._mock_infer(frame)
        return self._tflite_infer(frame)

    def _mock_infer(self, frame: np.ndarray) -> VisionResult:
        label = _MOCK_LABELS[self._mock_idx % len(_MOCK_LABELS)]
        confidence = 0.75 + (self._mock_idx % 4) * 0.05
        self._mock_idx += 1
        return VisionResult(label=label, confidence=round(confidence, 2),
                            box=(100, 100, 200, 200), raw_frame=frame)

    def _tflite_infer(self, frame: np.ndarray) -> VisionResult:
        import numpy as np
        input_shape = self._input_details[0]['shape']
        h, w = input_shape[1], input_shape[2]
        from PIL import Image  # type: ignore
        img = Image.fromarray(frame).resize((w, h))
        input_data = np.expand_dims(np.array(img, dtype=np.uint8), axis=0)
        self._interpreter.set_tensor(self._input_details[0]['index'], input_data)
        self._interpreter.invoke()
        output_data = self._interpreter.get_tensor(self._output_details[0]['index'])[0]
        top_idx = int(np.argmax(output_data))
        confidence = float(output_data[top_idx]) / 255.0
        label = self._labels[top_idx] if top_idx < len(self._labels) else "unknown"
        return VisionResult(label=label, confidence=round(confidence, 2),
                            box=(0, 0, frame.shape[1], frame.shape[0]),
                            raw_frame=frame)
