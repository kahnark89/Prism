from __future__ import annotations
from fastapi import APIRouter
from pydantic import BaseModel

router = APIRouter(prefix="/grounding", tags=["grounding"])

_grounding = None


def set_deps(grounding) -> None:
    global _grounding
    _grounding = grounding


class ParentConfirmBody(BaseModel):
    concept: str


# Hard Line 6 (ratified absolute, Epigenome 029): no numeric confidence on any
# surface, parent-facing included — banded status only. The numeric value stays
# internal to the accumulator (it may still order the list; it may not leave it).


@router.get("/trajectories")
async def get_grounding_trajectories():
    if _grounding is None:
        return []
    records = _grounding.get_all_records()
    return [
        {
            "concept": r.concept,
            "status": r.status,
            "parent_confirmations": r.parent_confirmations,
            "reappearances": len(r.context_distances),
        }
        for r in sorted(records, key=lambda r: -r.confidence)
    ]


@router.post("/parent-confirm")
async def parent_confirm(body: ParentConfirmBody):
    if _grounding is None:
        return {"ok": False, "error": "grounding not initialized"}
    _grounding.parent_confirm(body.concept)
    rec = _grounding.get_record(body.concept)
    return {
        "ok": True,
        "concept": body.concept,
        "status": rec.status,
    }
