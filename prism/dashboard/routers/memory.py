from __future__ import annotations
from fastapi import APIRouter

router = APIRouter(prefix="/memory", tags=["memory"])

_memory_engine = None


def set_deps(memory_engine) -> None:
    global _memory_engine
    _memory_engine = memory_engine


@router.get("/graph")
async def get_memory_graph():
    if _memory_engine is None:
        return {"nodes": [], "edges": []}
    nodes = _memory_engine.get_all_nodes()
    node_list = []
    for n in nodes:
        node_list.append({
            "id": n.id,
            "concept": n.concept,
            "episode": n.episode,
            "strength": round(n.s, 3),
            "tau_days": round(n.tau, 1),
            "rehearsals": n.rehearsals,
            "salience": round(n.salience, 2),
            "contexts": n.contexts,
        })
    # Edges: connect nodes that share a concept
    edges = []
    by_concept: dict[str, list[str]] = {}
    for n in nodes:
        by_concept.setdefault(n.concept, []).append(n.id)
    for concept, ids in by_concept.items():
        for i in range(len(ids) - 1):
            edges.append({"source": ids[i], "target": ids[i + 1], "concept": concept})
    return {"nodes": node_list, "edges": edges}


@router.get("/codebook")
async def get_codebook():
    if _memory_engine is None:
        return []
    cb = _memory_engine.get_codebook()
    return [
        {"concept": k, "strength": round(v.strength, 3), "count": v.count}
        for k, v in sorted(cb.items(), key=lambda x: -x[1].strength)
    ]
