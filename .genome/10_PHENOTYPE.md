# 10_PHENOTYPE — Prism (live state)

> High-churn file. Any client updates this freely. This is "what we're doing right now."
> **Last touched:** 2026-06-10 by Claude Code (remote) · Review-session maintenance queue fully discharged (test-name locale fix, CI test jobs, AGENTS.md/README refresh, stale-doc banners, symlinks). HL6 scope clarification still awaiting architect sign-off (§3).
> **Pending ratification:** 2 unconfirmed deeper-why hypotheses in Epigenome 016, 017 (018 ratified into 019). Confirm/edit/drop when convenient.

---

## §1 Current focus
**PLATFORM PIVOT LANDED — Prism is now two linked Android apps (Companion + Parent Suite), not a bespoke physical product.** Architect-directed, "for ease of engineering" (Epigenome 024 — his words, recorded as-is). This is the single biggest architecture change in the project's history, and it is a *delivery-form* change, not a redesign of what Prism is or protects: the thesis, the co-evolution principle, every hard line, the two-brains split, the safety model, and the Parent-Suite spec (Doc 2.2, thread 1) **all carry forward unchanged.**

**What changed in the genotype (architect sign-off given live, same standing as 019/020/022):**
- `Hardware invariant` → **`Platform invariant — two linked Android apps`**: Companion app (child-facing, on-device fast brain + recognition + inner-life + awakened experience, locked via OS guided-access) and Parent Suite app (parent-facing, separate install on a separate device — the installation boundary *is* the transparency-asymmetry, Hard Line 9, made physical). Paired via an encrypted, parent-revocable link.
- **Thesis + Principle 10** ("the build is documented... a mode on the device") reframed: "Daddy's Project" becomes a **software-setup ritual** — Dad and child enroll her together (name, face, voice). Proposed and seated as arguably *sharper* than the hardware-build version: teaching an AI who you are, watching it guess and improve, *is* the glass-box thesis lived directly — not a tech-literacy detour through soldering.
- Principle 5 and the `ui/led` module reference updated to drop hardware-specific language (`ui` now: screen + haptics + speaker, phone-native).

**New deliverable:** `docs/Prism_3.0_Platform_Architecture.md` — module-to-app mapping, and a *proposed* pairing/sync mechanism (QR-code key exchange + local-first encrypted sync + zero-knowledge cloud relay fallback) for the "linked... with a security key or something" requirement the architect explicitly left open. Flagged for technical sign-off — it's a grounded proposal (Signal/WhatsApp-pattern), not a committed spec.

**Rejected path preserved, not deleted:** the full bespoke-hardware plan (Pi5/CM5, custom carrier, molded enclosure, LED ring, haptic ring, round display, material/thermal lines) lives on in `40_SHADOW.md` S09 — recoverable as a future product-phase starting spec if the app validates the concept and physical hardware becomes attractive later (this client's inference about reversibility, flagged as such, not the architect's stated reasoning).

**Stale-document flag:** Docs 01, 1.5, and the hardware-bearing portions of 1.6 / Master Architecture now describe a product-form that no longer exists (their *emotional/software* content — the awakening arc, inner life, magic — all survives the pivot intact and is NOT stale). Tracked in §3 below; not rewritten wholesale here per heuristic #8 (minimal, targeted changes — five detailed docs in one pass risks losing nuance worth keeping).

**Remaining queued design threads (priority order):**
1. ~~Parent-suite UX~~ — **done**, see Doc 2.2 (zero open questions remain; carries forward unaffected by the pivot)
2. **Awakening choreography spec — IN PROGRESS, redesigned for the new platform.** The original five-beat sequence (pause/spark/bloom/first breath/settle) was specified for a NeoPixel LED ring + dedicated haptic motor + round display (Doc 1.6 §6) — none of which exist on a phone. Architect directed: redesign now, against phone-native channels (screen + phone haptics + speaker), rather than waiting. → `docs/Prism_2.3_Awakening_Choreography.md`.
3. **Mission 0 — First Light** — camera → TFLite → speech, offline, one weekend build. *(Note: "offline, on the prototype rig" language needs a pass — the rig no longer exists; the spirit — prove the core loop fast, cheap, in a weekend — carries forward to "prove it on a single Android device.")*

## §2 Acceptance (how we know the current phase is done)
- Genome files committed to a git repo. ✅
- All 23 original files on `github.com/kahnark89/Prism` (master). ✅
- Three analysis docs on master: `docs/analysis/01–03`. ✅
- Tool specs separated: `animus-sdk` and `cortex-dev` repos initialized; zip files delivered. ✅
- Prism `docs/tools/README.md` points to both tool repos. ✅
- Cold-boot prompt in README.md still works from the remote. ✅

## §3 Open questions (incl. proposed genotype changes awaiting sign-off)
- **Parent-Suite UX — CLOSED (2026-06-08):** all four questions resolved by the architect; see §1 above and `docs/Prism_2.2_Parent_Suite_UX.md §8` for the full record. One forward-looking note carried into thread 2: the Parent Preview Mode (Doc 2.2 §6) will need to walk the awakening-choreography sequence once that spec exists — flag this when starting that thread so the two reconcile rather than diverge.
- **Next design thread** — Awakening choreography spec is next in the queue (architect may redirect).
- **Companion names** — Pip/Lumi/Tale are placeholders; replace with Naomi's real favorites during the build.
- ~~**Compute final call** — CM5-on-custom-carrier vs. Pi 5 in designed enclosure.~~ **OBSOLETE — superseded by the platform pivot (Epigenome 024).** Compute is now "whatever phone/tablet the parent provisions." No bespoke board, no enclosure, no thermal/material compliance. Question dissolves rather than resolves; preserved here only so a future reader doesn't wonder where it went. (Bespoke-hardware plan recoverable as a future product-phase spec — Shadow S09.)
- **Shadow Actions for youngest tier** — drop field at 3–5, light up with age?
- **mp4Real "+" / CIAER+ Pre-ENV mapping for a child** — sketched; not yet formalized into learning_log schema.
- **Replatform — ALL FOUR ITEMS COMPLETE (2026-06-09):** `prism/` (the Python reference stack) replatformed as two linked Android apps; runtime is Kotlin/Java, Python becomes the algorithm spec (Epigenome 025 + 026):
  1. ✅ `hal/` rebuilt natively for Android — `CompanionHal`, `CameraSource`, `MicrophoneSource`, `SpeakerOutput`, `HapticOutput` interfaces + Android implementations in `:companion-app`
  2. ✅ Awakening redesigned per Doc 2.3 — `AwakeningChoreographer`, `AwakeningMachine` (StateFlow-based), `PresentationCanvas`, `MechanicalPresentationScreen`, `AwakenedPresentationScreen`, `CompanionScreen` — all in `:companion-app`
  3. ✅ Orchestrator split + pairing/sync — `:sync` module (ECDH pairing, `LinkedDeviceRegistry`, `EnvelopeCipher`, `SyncTransport`, `SessionSummary`/`MenuState` payloads; 79 tests passing on JVM); `CompanionOrchestrator` in `:companion-app`
  4. ✅ `dashboard/` replaced by native Parent Suite app — `:parent-suite-app` complete: `ConceptRecord`/`ConceptTileState`/`Pacing`/`ChildProfile` data layer; `ParentSuiteViewModel`; `MapScreen` (2-col grid, all 4 tile states, counter-balance always visible); `TrajectoryScreen` (chronological, banded text only); `PairingScreen` (QR generation via BarcodeEncoder, TTL countdown, linked-devices list); `SettingsScreen`; `PreviewModeScreen` (honest "awaiting Companion" state — Companion pairing module not yet written); `ParentNavHost` (bottom-nav + nested destinations); `MainActivity`; `PrismParentApp`

  **All four replatform items are now complete** — including the Companion's Android HAL (`CameraXSource`, `AndroidMicrophone`, `AndroidSpeaker`, `AndroidHaptics`), `MlKitRecognitionEngine` + `RecognitionDatabase` (ML Kit face detection + pixel-similarity template matching; upgrade path to TFLite embedding model documented), `TfliteVisionClassifier` (TFLite Task Vision with graceful `MockVisionClassifier` fallback when model asset absent), `AnthropicLlmClient` (HTTP POST to `/v1/messages` via `HttpURLConnection`; API key from `ApiKeyStore`/SharedPreferences), enrollment UX (`CompanionEnrollmentScreen`), admin overlay + navigation (`CompanionNavHost`, long-press admin menu → enrollment / pairing / API key screens), `CompanionViewModel` (full orchestrator wiring with all real implementations), and `MainActivity` wired to `CompanionNavHost`.

  **What remains before a fully production-ready session:**
  - Bundle `mobilenet_v1.tflite` + `labels.txt` in `src/main/assets/` to activate real TFLite inference (without it, `TfliteVisionClassifier` falls back to `MockVisionClassifier`).
  - Enter an Anthropic API key via the admin overlay → "Set API key" screen to activate the smart-brain path (without it, `PerspectiveEngine` uses offline fallback phrases).
  - Run the enrollment flow (admin overlay → "Enroll child") on the actual device with the child's face to activate awakening.

- **PROPOSED GENOTYPE CLARIFICATION (2026-06-10, from full-codebase review — awaiting sign-off):** Hard Line 6 as written ("no visible numeric score, anywhere in the device — to the child or the parent") textually contradicts the implemented child-facing `GlassBoxOverlay`, which deliberately shows the fast brain's numeric vision confidence as the glass-box lesson (Epigenome 026 even cites this contrast approvingly). Both implementations treat HL6 as scoped to the *grounding/learning* confidence (the measurement layer), not the vision model's per-guess confidence (the teaching content) — but the genotype text doesn't say that. Proposal: add a scoping clause to HL6, e.g. "…applies to the grounding/learning measurement; the fast brain's per-guess vision confidence shown to the child in mechanical mode is the glass-box lesson and is exempt." Architect sign-off required (reflexivity firewall). Related, lower-stakes: the retired Python dashboard (`prism/dashboard/routers/grounding.py:27,45`) returns numeric grounding confidence — it predates the HL6 broadening (Epigenome 022) and is superseded by the compliant native Parent Suite, but as "reference spec" it now contradicts the genome; needs either a one-line band-only fix or an explicit superseded-annotation. Same question for `perspective_engine.py:117` / the Kotlin `PerspectiveEngine.buildUserMessage` sending "(NN% sure)" into the LLM prompt — the LLM could speak the number aloud; consider banded words in the prompt instead.
- ~~**Review-session maintenance items (2026-06-10, no sign-off needed, queued)**~~ — **ALL DONE (2026-06-10, same day, architect-directed):** (a) ✅ em dashes in JVM test method names replaced with `--` (17 names across 11 files); 79/79 tests verified passing under the previously-failing POSIX locale. (b) ✅ AGENTS.md Tooling notes rewritten — "No code yet" replaced with the two-stack reality + build/test commands. (c) ✅ README status section rewritten for the Android platform; thesis line updated (two linked apps, setup ritual). (d) ✅ CI now runs `:engine:test :sync:test` before assembling APKs, plus a new `python-tests` job (`pytest tests/`); workflow also triggers on `prism/**`/`tests/**` changes. (e) ✅ Superseded-banners added to Docs 01, 1.5, 1.6 (hardware sections), and Master Architecture header. (f) ✅ `setup_links.sh` run — CLAUDE.md/GEMINI.md are now real symlinks. Also: .gitignore gained Python dev artifacts (`.pytest_cache/`, `*.egg-info/`, venvs). The stale-document flag (§1, Doc 3.0 §5 Q4) is now partially discharged: docs are *annotated* as superseded; a full content rework remains optional.

## §4 Next actions
1. Architect directs the next design thread (parent-suite UX is the lead candidate).
2. Seed `kahnark89/animus-sdk` and `kahnark89/cortex-dev` repos using the delivered zip files.
3. Optionally `./setup_links.sh` on Linux/macOS to make CLAUDE.md/GEMINI.md real symlinks.

## §5 Deliverables on hand (in docs/)
- `Prism_Master_Architecture_v1.md` — canonical reference (consolidates Docs 01–1.8). *(Hardware-bearing portions now superseded by Doc 3.0 — see stale-document flag, §1/§3.)*
- Component docs 01, 1.5, 1.6, 1.7, 1.8, 2.0, 2.1, 2.2 — detailed source documents. *(01, 1.5, and hardware portions of 1.6 likewise flagged stale — emotional/software content survives the pivot intact.)*
- `Prism_2.2_Parent_Suite_UX.md` — converged spec: grounding-signal view + environment-shaping tool + absence-visibility as one structure (the Map / the Trajectory).
- `Prism_3.0_Platform_Architecture.md` — **NEW.** The platform pivot's deliverable: module-to-app mapping (Companion / Parent Suite / shared engine), and a proposed pairing/sync mechanism (QR handshake + ECDH + zero-knowledge relay) flagged for technical sign-off.
- `The_Genome_Protocol.md` — the protocol this folder implements.
- `analysis/01_Soul_Mouth_Separation.md` — standalone architectural analysis.
- `analysis/02_Co_Evolution_Organizing_Principle.md` — standalone architectural analysis.
- `analysis/03_The_Genome_Protocol.md` — standalone architectural analysis.
- `tools/README.md` — pointer to `animus-sdk` and `cortex-dev` repos.

## §6 Build status
**Code exists, but targets the retired design — replatforming, not a from-scratch build.** A full single-process Python reference stack landed on `main` June 3 (`prism/` — HAL, engines, modules, personas, persistence, orchestrator, FastAPI dashboard; 34 tests passing), merged the same morning the platform pivot (Epigenome 024) landed. It was built entirely for the bespoke-hardware design (Pi target, NeoPixel LED ring, dedicated haptic motor, GPIO shutter button, single-device dashboard) — i.e., the design Epigenome 024 retired hours later. Reviewed 2026-06-08; verdict: the pure-logic core (`inner_life`, `memory`, `mood_line`, `grounding`, `personas`, `safety`, `learning_log` — the actual differentiating IP) **survives as the algorithm spec**; the hardware layer (`hal/` Pi targets, `ui_controller`'s LED compiler, the awakening sequence's LED/haptic-motor calls, the single-process orchestrator, the FastAPI dashboard) needs full replacement, redesign-per-Doc-2.3, or replatforming respectively — see Epigenome entry for the full breakdown.
**Runtime decision (architect, this session):** the shared engine on Android will be a **native Kotlin/Java rewrite** — this Python stack becomes the reference *spec* (equations, rules, schemas translate directly), not an embedded runtime (Chaquopy/Kivy considered and declined — bridge overhead nobody had signed off on). This resolves the open question Doc 3.0 §2 left implicit ("one shared engine," runtime unnamed).
**No physical hardware to acquire** — the BOM is now "a parent-provisioned phone/tablet" (Doc 3.0 §5 Q2: repurpose-old-device vs. buy-dedicated still open). Net: the pivot *shortens* the remaining road — it converts "build a device" into "replatform an already-proven reference implementation," removing the enclosure/supply-chain layer entirely.
