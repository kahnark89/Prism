from __future__ import annotations
from fastapi import APIRouter

router = APIRouter(prefix="/recognition", tags=["recognition"])

_recognition = None
_awakening = None


def set_deps(recognition, awakening) -> None:
    global _recognition, _awakening
    _recognition = recognition
    _awakening = awakening


@router.get("/status")
async def recognition_status():
    if _recognition is None:
        return {"enrolled": False}
    return {"enrolled": _recognition.is_enrolled()}


@router.delete("/templates")
async def delete_templates():
    if _recognition is None:
        return {"ok": False}
    _recognition.delete_templates()
    if _awakening:
        _awakening.force_mechanical()
    return {"ok": True, "message": "All recognition templates deleted. Device reset to mechanical mode."}
