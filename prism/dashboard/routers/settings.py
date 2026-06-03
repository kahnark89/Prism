from __future__ import annotations
from fastapi import APIRouter
from pydantic import BaseModel
from typing import Optional

router = APIRouter(prefix="/settings", tags=["settings"])

_config = None


def set_deps(config) -> None:
    global _config
    _config = config


class SettingsUpdate(BaseModel):
    child_name: Optional[str] = None
    active_companion: Optional[str] = None


@router.get("")
async def get_settings():
    if _config is None:
        return {}
    return {
        "child_name": _config.child_name,
        "active_companion": _config.active_companion,
        "dashboard_port": _config.dashboard_port,
        "llm_model": _config.llm_model,
        "use_mock_hal": _config.use_mock_hal,
    }


@router.put("")
async def update_settings(body: SettingsUpdate):
    if _config is None:
        return {"ok": False}
    if body.child_name:
        _config.child_name = body.child_name
    if body.active_companion:
        _config.active_companion = body.active_companion
    return {"ok": True}
