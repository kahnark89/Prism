# Prism — Document 3.0: Platform Architecture — Two Linked Android Apps

*Replaces the bespoke-hardware plan (Pi5/CM5, custom carrier, molded enclosure, LED ring, haptic ring, round display — preserved in `00_GENOTYPE.md` history and `40_SHADOW.md` S09) with a software-only platform: two separately-installed Android apps sharing one engine. Architect-directed pivot, "for ease of engineering" — Epigenome 024.*

---

## 1. Why two *separate installs*, not one app with a parent mode

The obvious-looking shortcut — one app, a PIN-gated "parent mode" — is exactly what Hard Line 9 (transparent to the parent / invisible to the child) and the genotype's installation-boundary language forbid in spirit, even if not in letter. A PIN screen is a door in the same house; a curious, persistent four-year-old eventually finds the door. A **separate install on a separate device** isn't a stronger lock on the same door — it's a different building. There is no code path, no UI state, no forgotten debug menu by which the child's experience could surface the parent's controls, because the parent's controls *do not exist as code on the child's device at all.*

This is the same structural-over-policed instinct that seated Hard Line 12 (020) and shaped the Map's absence-rendering (021): don't make the right outcome depend on the gate holding under pressure — make the wrong outcome impossible to construct. Two installs is that, applied to the oldest threat model in parenting software: *the kid finds the settings.*

---

## 2. What lives where — module-to-app mapping

Both apps are thin shells around **one shared engine** (a private library/SDK) that carries the platform-agnostic IP — the actual differentiating software named in Epigenome 024's reasoning (inner life, memory, CIAER+, recognition, safety logic, the sync protocol). Porting to Android changes *where* the UI renders, not *what* the engine does.

| Module (genotype `Architecture invariant`) | Lives in | Notes on the port |
|---|---|---|
| `camera_handler`, `vision_engine` | **Companion** | Android `CameraX` + on-device TFLite — direct port, no loss. Phone cameras outperform the Camera Module 3 prototype spec. |
| `recognition` | **Companion** | Face/voice embeddings via on-device ML Kit / TFLite, stored in Android Keystore-backed local storage. Hard Line 3 (on-device, parent-deletable, never cloud) is *easier* to guarantee on a platform with a mature secure-storage API than on a bespoke Linux image. |
| `perspective_engine`, `conversation` | **Companion** | Calls the cloud smart-brain through the existing Prism backend (the one that already enforces the input gate, Doc 01 §6). Unchanged by the pivot. |
| `audio_feedback` | **Companion** | Android TTS / bundled voice assets through the phone speaker. |
| `inner_life` | **Companion** | Runs on-device, exactly as specified (Doc 1.6 §1) — a handful of drifting local variables. Nothing about this module is hardware-specific; it ports verbatim. |
| `safety` | **Companion** (gate logic) + **shared engine** (rules) | The gate must run where the content is produced and spoken — on the child's device — but the rule-set is shared so both apps reason about safety identically (relevant for the Preview Mode, Doc 2.2 §6, which must run the *real* safety gate). |
| `learning_log` | **Companion** (writer) → **Parent Suite** (reader, via sync) | CIAER records are authored on-device where the session happens; summarized and synced to feed the Map/Trajectory. Raw logs never leave the Companion device — only the summaries the Parent Suite needs to render grounding state (§3). |
| `ui` | **Both**, independently | Companion: camera viewfinder, awakened-companion presentation (screen + phone haptics + speaker — see Doc 2.3). Parent Suite: the Map, the Trajectory, every control, the Preview Mode (Doc 2.2). Two different products, two different UIs, one shared design language. |
| *(new)* `pairing` / `sync` | **Both** (client) + **shared engine** (protocol) | See §3. |

**What this preserves exactly:** the two-brains split, on-device-first privacy posture, the safety layering, and the parent-suite spec drafted in Doc 2.2 — all of it was already platform-agnostic software design. The pivot subtracts an enclosure; it adds nothing to the privacy/safety surface and removes nothing from it.

---

## 3. Pairing & sync — a proposal for "linked... with a security key or something"

The architect named the requirement and explicitly left the mechanism open. This is a concrete proposal — **flagged for technical sign-off**, not a settled spec — built from patterns already proven at consumer scale (Signal's linked-devices, WhatsApp's QR pairing) and shaped to this project's specific constraint: *the cloud must never be able to read what crosses the link* (an extension of Hard Line 7's "no eavesdropping," applied to the project's own infrastructure, not just outside parties).

### 3.1 What actually needs to cross the link (keep this list short — it's the privacy surface)
- **Companion → Parent Suite:** session summaries / CIAER records — the data the Map and Trajectory render (exposure, reappearance, grounding band, never raw transcripts; Doc 2.2 §1).
- **Parent Suite → Companion:** the current menu state (on/off-menu, pacing, boundaries, counter-balance weights — Doc 2.2 §4), which feeds the suggested-topics signal to the LLM.
- Nothing else needs to cross. No raw audio, no raw video, no recognition templates (Hard Line 3 keeps those single-device, period — pairing must not become a backdoor around it).

### 3.2 Proposed mechanism — pairing
1. **QR-code handshake.** The Parent Suite app displays a one-time QR code (a pairing token + its public key, short-lived). The Companion app — in a parent-authenticated setup flow (device PIN/biometric, the same gate that locks the rest of the OS into guided-access mode) — scans it.
2. **On-device key exchange (ECDH).** The two apps derive a shared secret without that secret ever existing outside the two devices — not on a server, not in a QR code that could be photographed and reused (the token is single-use and short-lived; the *derived* key is what persists).
3. **Stored in platform secure hardware.** Android Keystore / StrongBox on both ends — the same guarantee Hard Line 3 already requires for recognition templates, reused for the pairing key.
4. **Multiple parents, independently revocable.** Each parent device that pairs gets its *own* derived key. The Parent Suite shows a plain "linked devices" list (mirroring Doc 2.2's glass-box instinct — nothing about the *system's own connections* is hidden from the parent either); unlinking one revokes only that key, instantly, on both ends.

### 3.3 Proposed mechanism — ongoing sync
- **Local-first:** when both devices share a network (the common case — home), sync runs **peer-to-peer**, encrypted with the paired key, touching no server at all.
- **Relay fallback:** when apart (parent checking the suite from work), traffic routes through the Prism backend that *already exists* for smart-brain calls — but only as a **zero-knowledge relay**: it stores and forwards opaque, paired-key-encrypted blobs it cannot decrypt. The backend's trust boundary doesn't expand; it just also carries mail it can't open.
- **Parent-revocable, on-device-keyed, no new cloud dependency** — this is the existing Hard Line 3 / Doc 1.8 §7 privacy posture, extended to the one new data path the two-app split creates.

### 3.4 Why this passes the existing tests
- **Privacy one-line test** ("would a parent be glad, not alarmed, to learn exactly what's captured and where it goes?"): yes — the answer to "where does my data go between my two devices" is "nowhere it can be read except your two devices," and that claim is checkable (open protocol, no proprietary black box in the relay).
- **Hard Line 7** (no eavesdropping): the relay is structurally incapable of eavesdropping — it never holds a key. This isn't a promise about the relay operator's intentions; it's the same "structural, not policed" pattern the genotype favors throughout (Principle 12, Hard Line 12).

---

## 4. What changes, what doesn't — a translation table

| Hardware-era concept | Platform-era equivalent | Carries over unchanged? |
|---|---|---|
| Sealed enclosure, no detachable parts | OS-level guided-access/kiosk lock on the Companion device | **Intent: yes.** Mechanism: software lock replaces molded plastic. |
| Material/thermal compliance (skin-contact temp, plasticizer migration) | N/A — consumer phone hardware is already regulated for child proximity | Concern dissolves; doesn't need replacing. |
| LED ring + haptic motor + round display | Phone screen + phone haptic actuator + phone speaker | **Intent: yes, mechanism: redesigned** — see Doc 2.3 (awakening choreography, phone-native). |
| Protected LiPo, sealed battery | Whatever battery is in the parent-provisioned device | Out of scope — not Prism's hardware to engineer anymore. |
| "The build is the first lesson" (Principle 10) | The setup-ritual / enrollment-together moment | **Reframed, arguably strengthened** — see `00_GENOTYPE.md` Principle 10 and Epigenome 024 point 2: teaching an AI who you are *is* the glass-box thesis, lived. |
| Recognition templates on-device, parent-deletable (Hard Line 3) | Same, via Android Keystore | **Unchanged — and easier to guarantee** on a platform with mature secure-storage primitives than on a bespoke Linux image. |
| Two brains, safety layering, learning_log, parent suite | Same architecture, ported | **Entirely unchanged.** This was always software. |

---

## 5. Open questions for the architect (and eventually, engineering)

1. **Provisioning model** — does the family need *two* devices (a phone for the child, a phone/tablet for the parent), or can the Parent Suite run on the same device the parent already carries, alongside their own apps? (Likely the latter — but worth confirming it doesn't blur the installation-boundary principle in §1: the Parent Suite app itself must still be a separate, lockable, PIN-or-biometric-gated install even on a shared device.)
2. **Companion device sourcing** — is the assumption "repurpose an old phone/tablet" (cheapest, greenest, fits "Dad sets it up" ritual nicely — *handing down* a device has its own warmth) or "buy a dedicated low-cost Android device"? Changes the BOM story and possibly the setup-ritual script (Principle 10).
3. **Pairing mechanism sign-off** — §3 is a proposal grounded in proven consumer patterns, not a committed spec. Needs a real security review before build (out of this client's scope to finalize alone — flagging for whoever does the engineering).
4. **Stale-document cleanup** — Docs 01, 1.5, and the hardware-bearing portions of 1.6 / Master Architecture now describe a product-form that no longer exists. They're valuable as *design history* (the emotional arc, the inner-life spec, the magic itself all survive the pivot intact) but need a pass to either retire their hardware specifics or annotate them as superseded by this doc. Tracked in `10_PHENOTYPE.md §3`; not rewritten wholesale here (heuristic #8 — minimal, targeted changes; five detailed docs in one pass risks losing nuance worth keeping).

---

*Prism · Document 3.0 · Platform Architecture — Two Linked Android Apps · supersedes the hardware sections of Docs 01/1.5/Master Architecture · Capps Consulting Company LLC*
