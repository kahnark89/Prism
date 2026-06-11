# 10_PHENOTYPE — Prism (live state)

> High-churn file. Any client updates this freely. This is "what we're doing right now."
> **Last touched:** 2026-06-11 by Claude Code (remote) · Cold-start hygiene pass: §1 design threads updated (Doc 2.3 done), §4 next actions rewritten to reflect completed replatform, §6 build status rewritten to describe the current reality (both stacks complete, tests passing, APKs building in CI).
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
2. ~~Awakening choreography spec~~ — **done.** Five-beat sequence redesigned for phone-native channels (screen + haptics + speaker) in `docs/Prism_2.3_Awakening_Choreography.md`; implemented as `AwakeningChoreographer` + `AwakeningMachine` in `:companion-app`.
3. **Mission 0 — First Light** — prove the core loop on a real device: sideload the APK, set the API key, enroll Naomi, run a real session. This is the lead candidate for the next hands-on step (architect may redirect).

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

- ~~**PROPOSED GENOTYPE CLARIFICATION — HL6 scope**~~ — **RESOLVED (2026-06-10, architect sign-off, Epigenome 029):** Hard Line 6 is **absolute** — no numeral anywhere, the child-facing `GlassBoxOverlay` included (the recommended scope-to-grounding option was declined; preserved in Shadow S10). Implemented same session: shared banded vocabulary `confidenceWords()` ("just guessing" / "pretty sure" / "very sure") in both the Kotlin engine and the Python reference spec; `GlassBoxOverlay` shows bar + words, no "%" text; LLM prompt sends banded words so a number can never be spoken aloud; retired Python dashboard brought to band-only (router + HTML). Genotype HL6 clause updated (authorization record: Epigenome 029).
- ~~**Review-session maintenance items (2026-06-10, no sign-off needed, queued)**~~ — **ALL DONE (2026-06-10, same day, architect-directed):** (a) ✅ em dashes in JVM test method names replaced with `--` (17 names across 11 files); 79/79 tests verified passing under the previously-failing POSIX locale. (b) ✅ AGENTS.md Tooling notes rewritten — "No code yet" replaced with the two-stack reality + build/test commands. (c) ✅ README status section rewritten for the Android platform; thesis line updated (two linked apps, setup ritual). (d) ✅ CI now runs `:engine:test :sync:test` before assembling APKs, plus a new `python-tests` job (`pytest tests/`); workflow also triggers on `prism/**`/`tests/**` changes. (e) ✅ Superseded-banners added to Docs 01, 1.5, 1.6 (hardware sections), and Master Architecture header. (f) ✅ `setup_links.sh` run — CLAUDE.md/GEMINI.md are now real symlinks. Also: .gitignore gained Python dev artifacts (`.pytest_cache/`, `*.egg-info/`, venvs). The stale-document flag (§1, Doc 3.0 §5 Q4) is now partially discharged: docs are *annotated* as superseded; a full content rework remains optional.

## §4 Next actions
1. **Mission 0 — First Light (device work, no code needed):** sideload both APKs (CI builds them at `.github/workflows/build-apks.yml` → Actions tab), enter Anthropic API key via long-press admin menu → "Set API key", run enrollment (admin menu → "Enroll child"), have a first real session.
2. **Seed tool repos** — `kahnark89/animus-sdk` and `kahnark89/cortex-dev` using the delivered zip files (if not already done).
3. **Architect directs the next design thread** — Mission 0 is the lead candidate; alternatively a new design thread (co-evolution curriculum, session-persistence schema, etc.).

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
**Both stacks are complete and green.**

**Python (`prism/`)** — algorithm reference spec. 34 tests passing (`pytest tests/`). This stack is never shipped as a runtime (Epigenome 025); it is the source of truth for equations, decay curves, and safety gates that the Kotlin port translates directly. HL6 banded-word fix applied (Epigenome 029); confidence numerals removed from grounding router + HTML dashboard.

**Android (`android/`)** — the shipping implementation. Four modules:
- `:engine` — pure-Kotlin port of the Python spec (inner life, memory, mood line, grounding, safety, learning log, perspective, personas). 79 JVM tests passing.
- `:sync` — ECDH pairing, `LinkedDeviceRegistry`, `EnvelopeCipher`, `SyncTransport`. 79 tests passing.
- `:companion-app` — child-facing app: CameraX HAL, ML Kit face recognition + enrollment UX, TFLite vision classifier (graceful fallback when model absent), Anthropic LLM client, awakening choreographer (Doc 2.3 five-beat sequence), full Compose UI with admin overlay + navigation. Builds to a 110 MB debug APK.
- `:parent-suite-app` — parent-facing app: Map / Trajectory / Pairing / Settings / PreviewMode screens. Builds to a 58 MB debug APK.

**CI** (`.github/workflows/build-apks.yml`) runs `:engine:test :sync:test` and `pytest tests/` before assembling both APKs; artifacts downloadable from the Actions tab for 90 days.

**What's needed before a real session (no code changes required):**
- Bundle `mobilenet_v1.tflite` + `labels.txt` in `companion-app/src/main/assets/` to activate real TFLite inference (falls back to `MockVisionClassifier` without it).
- Enter an Anthropic API key via the admin overlay → "Set API key" (falls back to offline phrases without it).
- Run enrollment (admin overlay → "Enroll child") on the real device with Naomi's face.
