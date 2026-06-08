from prism.personas.pip import PIP
from prism.personas.lumi import LUMI
from prism.personas.tale import TALE
from prism.personas.mechanical import MECHANICAL
from prism.personas.base import CompanionPersona

PERSONAS: dict[str, CompanionPersona] = {
    "pip": PIP,
    "lumi": LUMI,
    "tale": TALE,
    "mechanical": MECHANICAL,
}


def get_persona(name: str) -> CompanionPersona:
    p = PERSONAS.get(name.lower())
    if p is None:
        raise ValueError(f"Unknown persona '{name}'. Valid: {list(PERSONAS)}")
    return p
