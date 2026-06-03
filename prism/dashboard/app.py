from __future__ import annotations
from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from pathlib import Path

from prism.dashboard.routers import (
    learning_log as ll_router,
    memory as mem_router,
    grounding as grd_router,
    recognition as rec_router,
    settings as set_router,
)

app = FastAPI(title="Prism Parent Dashboard", version="0.1.0")

app.include_router(ll_router.router, prefix="/api")
app.include_router(mem_router.router, prefix="/api")
app.include_router(grd_router.router, prefix="/api")
app.include_router(rec_router.router, prefix="/api")
app.include_router(set_router.router, prefix="/api")

_static_dir = Path(__file__).parent / "static"
if _static_dir.exists():
    app.mount("/static", StaticFiles(directory=str(_static_dir)), name="static")


@app.get("/")
async def index():
    idx = _static_dir / "index.html"
    if idx.exists():
        return FileResponse(str(idx))
    return {"message": "Prism Dashboard API. Visit /docs for API reference."}


def wire_dependencies(
    config,
    memory_engine,
    grounding_accumulator,
    learning_log,
    log_store,
    recognition,
    awakening,
) -> None:
    """Called from main.py after all modules are instantiated."""
    ll_router.set_deps(learning_log, log_store)
    mem_router.set_deps(memory_engine)
    grd_router.set_deps(grounding_accumulator)
    rec_router.set_deps(recognition, awakening)
    set_router.set_deps(config)
