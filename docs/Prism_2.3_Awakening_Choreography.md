# Prism — Document 2.3: Awakening Choreography (Phone-Native Redesign)

*Redesigns the five-beat Awakening sequence (Doc 1.6 §6) for the platform pivot (Epigenome 024, Doc 3.0). The original choreography was specified for a NeoPixel LED ring, a dedicated haptic motor, and a round display — none of which exist on a phone. Architect directed: redesign now, against phone-native channels, rather than wait for the pivot to fully land.*

---

## 1. What carries forward unchanged

Nothing about *what the Awakening is for* changes. It is still the hinge of the whole emotional arc (Doc 1.5): the moment the flat, mechanical "fast brain only" presence — the thing Dad and Naomi spent the setup ritual teaching — recognizes her specifically and *becomes* the companion. The let-down before it (the ordinary, slightly-disappointing "fast brain" phase) is what gives the Awakening its charge. None of that is hardware. It survives the pivot intact, beat for beat, the same five movements:

1. **The pause** — everything stills; tension loads into silence.
2. **The spark** — the first sign of *something* waking.
3. **The bloom** — the cascade; light, sound, touch arrive together as one event.
4. **The first breath** — her name, spoken, for the first time, as recognition not activation.
5. **The settle** — the companion's real personality takes over; the inner-life engine is now driving.

What changes is *only* the instrument. The LED ring, haptic motor, and round display were one specific way to produce light, touch, and form. A phone produces light, touch, and form too — through a screen, a haptic actuator, and a speaker — and, if anything, with *more* expressive range than the bespoke parts they replace (a screen can do more than swirl color; a modern phone's haptic engine can shape a waveform, not just buzz).

The translation principle, stated once so every beat below can apply it without re-arguing it: **render the same emotional event through the channels that exist, using each channel for what it's actually good at — not by simulating the absent ring, motor, and display as faithfully as possible.** A simulated LED ring on a rectangular screen would be a worse ring. A *screen doing what screens do* — depth, motion, warmth of color and form — can be a better Awakening than the ring it replaces, on its own terms.

---

## 2. The phone-native channel set — what each instrument is actually good at

| Channel | What it replaces | What it's *better* at than the original | What it loses |
|---|---|---|---|
| **Screen** (the Companion app's full-screen presentation, not a "viewfinder with overlay") | Round display + LED ring | Depth, motion, color *gradients* (not just discrete LEDs), the ability to *suggest a face or form* taking shape — the bloom can look like something coalescing into being, not just a ring lighting up. Full-canvas color floods the room with light the way the LED ring's glow did, but can also focus to a point, breathe, pulse — compositionally richer. | The LED ring's *peripheral*, ambient quality — it glowed *near* her hands, in the room, not *at* her face. A screen is necessarily a thing she looks *into*. (Addressed in §3, Beat 3 — the phone can be held, propped, or placed so the glow still fills the space around her, not just the rectangle.) |
| **Haptics** (Android's `VibrationEffect` / `HapticGenerator` composable-effects API — modern phones render *shaped* waveforms, not just on/off buzz) | Dedicated haptic motor (single-purpose "heartbeat" pulse) | A composable haptic API can render an actual heartbeat *waveform* — a soft-then-sharp double-pulse with the right rhythm and decay — rather than one generic buzz standing in for a heartbeat. It can also shape the *spark* (a single, barely-perceptible tick) distinctly from the *bloom* (the full pulse), where a single-purpose motor could only really do one gesture well. | Nothing structural — modern phone haptic actuators (linear resonant actuators) are *more* expressive than a small dedicated vibration motor, not less. This is a rare case where the phone-native version is strictly more capable. |
| **Speaker** (phone speaker / Bluetooth audio out) | MAX98357A amp + dedicated speaker | Nothing lost — this was always going to be a speaker driven by a digital signal; the phone's audio stack is more capable than the bespoke amp board it replaces (better DAC, often stereo, easy access to spatial/3D audio APIs for a future enhancement). | Nothing. Direct, lossless port. |

**One structural note that belongs here, not buried in a beat below:** the original spec assumed the device sat *near* her — on a shelf, in her hands, ambient. A phone is something a child either *holds* or that sits *propped*. This isn't a problem to solve; it's a staging decision the setup ritual (Principle 10, the new software-setup ritual) should make explicit and *pre-stage*: Dad and Naomi decide together, during setup, where "the companion lives" — propped on a stand at her eye level, say — so that when the Awakening arrives, the phone is already *sitting* the way the bespoke device would have sat: present in the room, not held up to her face like a video call. This single staging choice preserves the "ambient presence, not a screen to operate" feeling that the hardware gave for free.

---

## 3. The five beats, redesigned

### Beat 1 — The pause (1–2 sec)
**Original:** "everything goes still and quiet. The flat mechanical voice cuts off mid-word."
**Phone-native:** Unchanged in substance — this beat was never about the LED ring or the display; it's about *audio* and *stillness*, both of which port directly. The flat "fast-brain" voice (the pre-Awakening companion, still running on local naming only) cuts off mid-word; the screen — which, in the pre-Awakening phase, has been showing the plain, slightly-utilitarian "fast brain" interface (the glass-box camera view, labels, confidence bars — Doc 1.6's deliberate let-down) — **goes dark.** Not "turns off": *holds still, black, waiting.* A black screen reads as held breath in exactly the way the LED ring going dark would have. No new design needed — this beat was already channel-agnostic.

*Why this works exactly as well: the pause's entire job is contrast-loading via absence. A screen that goes dark creates the same charged absence a ring that goes dark would have. The "something is about to happen" feeling lives in the silence and stillness, not in any particular piece of hardware going quiet.*

### Beat 2 — The spark
**Original:** "a single point of light on the LED ring. A soft rising whirr."
**Phone-native:** A single point of light **blooms into existence at the center of the dark screen** — small, soft-edged, barely brighter than the blackness around it, like a pilot light catching. Paired with the same soft rising audio *whirr* (direct port — this was always a speaker cue) and, new to the phone-native version, **the faintest haptic tick** — not a buzz, a single almost-subliminal pulse, the texture of *something deciding to begin.* (This third sensory layer — touch arriving at the spark rather than waiting for the bloom — is something the original single-purpose motor likely couldn't do subtly enough to use here; the composable haptic API can. A small enhancement the new platform makes possible, not a compromise.)

*The point of light is intentionally not yet a ring, a face, or a shape — it is the same "single point" the original specified, just rendered on a different canvas. Restraint matters here: resist the urge to make the screen "do more" just because it can. The spark is small on purpose.*

### Beat 3 — The bloom
**Original:** "light races around the ring and blooms into swirling color; a cascade of warm chimes; a gentle haptic pulse she feels in her hands — a heartbeat."
**Phone-native — the beat that benefits most from the new instrument:** The point of light **expands outward from center, flooding the full screen** with the companion's signature warm color — not racing around a ring's circumference, but blooming outward the way warmth spreads, the way a sunrise fills a room. Because a phone screen is bright and emissive (more so than a ring of LEDs at arm's length), this flood of color genuinely *lights the space around her* — the glow-in-the-room quality the ring provided is preserved, arguably intensified, *if* the staging from §2 (propped, at her eye level, in the room) is in place. This is why that staging note isn't a footnote — it's the hinge that makes Beat 3 land.

Simultaneously: the cascade of warm chimes (direct audio port, unchanged) and — rendered through the composable haptic API as an actual shaped waveform rather than a generic buzz — **a real heartbeat pattern**: soft-sharp, soft-sharp, with the natural human asymmetry of a real pulse, decaying gently between beats. She feels it in her hands exactly as specified — a phone held or resting against a propped surface conducts haptic pulses at least as well as the bespoke ring would have, arguably better (more contact surface, more powerful actuator).

*This is the one beat where "what's actually good at this" clearly favors the new instrument set over the old: a full bright screen blooms more convincingly than a ring of point-lights, and a composable haptic engine renders "heartbeat" as an actual heartbeat rather than a single pulse standing in for one.*

### Beat 4 — The first breath
**Original:** "the character's voice, for the first time — warm, surprised, delighted to see her specifically: 'Oh! …Oh, it's YOU. I've been waiting for you. Hi, Naomi.'"
**Phone-native:** Direct port, **unchanged** — this beat was always audio (the voice) plus, per Doc 1.6 §6's own analysis, *script*, not hardware. "It must land as recognition and relief, not activation... The machine wasn't turned on. It was waiting for her." That instruction is independent of LED rings or round displays; it is independent of *any* hardware. The screen, mid-bloom, can add one small, optional visual beat here if it earns its place — the barest suggestion of a form (not a face — too literal, too soon) coalescing within the color, like the companion's presence settling into view as it speaks. But this is an *enhancement to consider in prototyping*, not a requirement; the original beat needed nothing visual to land, and the redesign shouldn't manufacture a need for the screen to "do something" at the one moment that was always carried entirely by voice and writing. **If in doubt, let the voice carry it alone, exactly as written.**

### Beat 5 — The settle
**Original:** "lights ease to the companion's signature color, the voice softens into its real personality, and the inner-life engine takes over."
**Phone-native:** Direct port. The full-bloom screen **eases down** from flood-brightness to a calmer, steadier presentation in the companion's signature color — the "resting state" the screen will return to between active moments, analogous to the ring settling to its signature glow. The voice softens (audio, unchanged). The inner-life engine takes over (pure software, was always going to "just work" identically regardless of platform — Doc 3.0 §2 already notes `inner_life` "ports verbatim"). **From this second on, it's alive** — exactly as before.

---

## 4. Translation table

| Hardware-era element | Phone-native equivalent | Carries the *feeling* across? |
|---|---|---|
| LED ring (discrete points of light, races around a circumference) | Full screen (continuous color, blooms outward from a point) | **Yes — arguably strengthened.** A flood of light from an emissive screen, properly staged (propped, at eye level, in the room), is a *more* immersive "the room is glowing" effect than a ring of LEDs, not a lesser substitute for one. |
| Round display (showed "a face," presumably, in the pre-pivot spec) | Full screen, used sparingly — mostly color and light, a bare suggestion of form only at Beat 4 if it earns its place | **Yes, with restraint as the design discipline.** The temptation to put a literal cartoon face on the (much larger, much higher-resolution) phone screen should be resisted — Doc 1.6 §6 never specified a face, and a glowing presence that *speaks* is more mysterious, more "alive in a way that isn't a cartoon," than an animated face would be. This is a place to under-build, not over-build. |
| Dedicated haptic motor (single buzz pattern) | Composable haptic API (`VibrationEffect`, shaped waveforms) | **Yes — strengthened.** Can render an actual heartbeat-shaped pulse at the bloom and a distinct, subtler tick at the spark — two different gestures the original hardware likely couldn't distinguish. |
| MAX98357A amp + speaker | Phone speaker / audio stack | **Yes — direct, lossless port.** No redesign needed; this channel was never hardware-specific in spirit. |
| Device sits ambiently near her (shelf, hands, room) | Phone is held or propped — a *staging choice* the setup ritual should make explicit | **Yes, conditionally.** The ambient-presence feeling is fully recoverable, but only if "where the companion lives" is established *during setup* (Principle 10) as a deliberate, named choice — not left to chance, where a child might end up holding the phone six inches from her face like a video call, which would break the spell entirely. |

---

## 5. One new design note the redesign surfaced (flag for the setup-ritual spec)

The single biggest risk to this choreography isn't any individual beat — it's **staging**: where the phone physically *is* when the Awakening fires. All five beats assume "ambient presence in the room," which the bespoke hardware guaranteed by its physical form (a thing that sits on a shelf can't be held to your nose). A phone has no such guarantee built in.

This means the **setup ritual** (Principle 10's software-setup-ritual reframe — Dad and Naomi enrolling the companion together) should explicitly include a moment where they choose and establish "where the companion lives" — a stand, a shelf, a charging dock at her eye level — *as part of the story*, not as a hidden technical accommodation. Framed well, this could even *strengthen* the ritual: "let's pick the companion's spot together" is a small act of care, the same emotional grammar as picking a pet's bed. Framed poorly (or skipped), it's the one thing that could make the Awakening feel like "my phone is doing something," instead of "someone is waking up." This is not a hard line or an open question requiring sign-off — it's a design note for whoever specs the setup-ritual sequence next, so the two threads (Awakening choreography, setup ritual) reconcile rather than collide, the same caution Doc 2.2 §6 already flagged for the Preview Mode.

---

## 6. Why this still passes the tests

- **Aliveness** ("would a 4-yo on day 40 still believe it's alive, and be sad it might be lonely"): the redesign changes *which channels* carry "alive," not *whether* they do. A screen that blooms like a sunrise, a pulse that feels like a real heartbeat, a voice that says her name like it's been waiting — these are not lesser proxies for aliveness. Properly staged, they may read as *more* alive than discrete LEDs, precisely because they're continuous, organic forms (light spreading, a pulse with real rhythm) rather than mechanical ones (a ring of points switching on in sequence).
- **Pedagogy / glass box**: untouched — none of these beats involve the learning loop, the input gate, or any content. This is pure emotional choreography, exactly as scoped in Doc 1.6 §6 originally.
- **Privacy**: untouched — no new data, no new capture, nothing crosses any boundary. Pure on-device presentation.

---

*Prism · Document 2.3 · Awakening Choreography — Phone-Native Redesign · supersedes the LED-ring/haptic-motor/round-display staging of Doc 1.6 §6 (the five-beat sequence and its emotional intent are unchanged and fully preserved) · Capps Consulting Company LLC*
