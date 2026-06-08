"""Run once to generate test fixture files."""
import numpy as np
import pathlib

out = pathlib.Path(__file__).parent

# 640×480 mock frame with a red square (simulates an object)
frame = np.zeros((480, 640, 3), dtype=np.uint8)
frame[100:300, 150:400] = [180, 60, 60]
np.save(out / "mock_frame.npy", frame)
print("Fixtures written.")
