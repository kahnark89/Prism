from __future__ import annotations
from fastapi import APIRouter, Query
from typing import Optional

router = APIRouter(prefix="/learning-log", tags=["learning-log"])

# Injected at app startup
_learning_log = None
_log_store = None


def set_deps(learning_log, log_store) -> None:
    global _learning_log, _log_store
    _learning_log = learning_log
    _log_store = log_store


@router.get("")
async def get_learning_log(
    limit: int = Query(100, ge=1, le=1000),
    concept: Optional[str] = None,
):
    if _learning_log is None:
        return []
    if concept:
        records = _learning_log.get_by_concept(concept)
    else:
        records = _learning_log.get_recent(limit)
    return records
