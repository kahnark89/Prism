from __future__ import annotations

SCHEMA_VERSION = 1

MAIN_TABLES = """
CREATE TABLE IF NOT EXISTS schema_version (
    version INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS inner_life_state (
    id INTEGER PRIMARY KEY,
    companion_id TEXT NOT NULL,
    state_json TEXT NOT NULL,
    updated_at REAL NOT NULL
);

CREATE TABLE IF NOT EXISTS memory_nodes (
    id TEXT PRIMARY KEY,
    node_json TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS codebook (
    concept TEXT PRIMARY KEY,
    entry_json TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS ciaer_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp REAL NOT NULL,
    concept TEXT,
    salience REAL,
    record_json TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS grounding (
    concept TEXT PRIMARY KEY,
    record_json TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS sessions (
    id TEXT PRIMARY KEY,
    started_at REAL NOT NULL,
    ended_at REAL,
    companion_id TEXT,
    event_count INTEGER DEFAULT 0
);
"""

RECOGNITION_TABLES = """
CREATE TABLE IF NOT EXISTS recognition_templates (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT NOT NULL,
    embedding_blob BLOB NOT NULL,
    enrolled_at REAL NOT NULL,
    label TEXT
);
"""


async def init_main_db(conn) -> None:
    await conn.executescript(MAIN_TABLES)
    row = await conn.execute("SELECT COUNT(*) FROM schema_version")
    count = (await row.fetchone())[0]
    if count == 0:
        await conn.execute("INSERT INTO schema_version VALUES (?)", (SCHEMA_VERSION,))
    await conn.commit()


async def init_recognition_db(conn) -> None:
    await conn.executescript(RECOGNITION_TABLES)
    await conn.commit()
