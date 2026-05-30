import React, { useState, useEffect, useRef, useCallback } from 'react';

// ============================================================================
// PRISM — Inner Life State Engine · Live Simulator
// Implements Document 2.0: coupled damped system + circadian forcing +
// event perturbation + autocorrelated noise + slow growth.
// ============================================================================

const VARS = ['M', 'E', 'C', 'A', 'S'];
const VAR_META = {
  M: { name: 'Mood',        lo: 'grumpy',  hi: 'joyful',     color: '#e8b04b' },
  E: { name: 'Energy',      lo: 'sleepy',  hi: 'bouncy',     color: '#e86b5a' },
  C: { name: 'Curiosity',   lo: 'mellow',  hi: 'fascinated', color: '#4ba3e8' },
  A: { name: 'Affection',   lo: 'warm',    hi: 'devoted',    color: '#d579c4' },
  S: { name: 'Sociability', lo: 'quiet',   hi: 'chatty',     color: '#6bc88a' },
};

// Coupling matrix κ[x][j]: how column j pulls row x (Doc 2.0 §4)
const COUPLING = {
  M: { M: 0,    E: 0.10, C: 0.05, A: 0.04, S: 0    },
  E: { M: 0.06, E: 0,    C: 0.08, A: 0,    S: 0.03 },
  C: { M: 0.05, E: 0.10, C: 0,    A: 0,    S: 0.04 },
  A: { M: 0.03, E: 0,    C: 0.02, A: 0,    S: 0.03 },
  S: { M: 0.05, E: 0.12, C: 0.06, A: 0.04, S: 0    },
};

// Event library (Doc 2.0 §5): kick magnitudes per variable
const EVENTS = {
  novel:    { label: 'Shows something new', kicks: { M: .05, E: .08, C: .20, A: .03, S: .05 } },
  delight:  { label: 'She laughs / delights', kicks: { M: .15, E: .10, C: .05, A: .08, S: .08 } },
  whim:     { label: 'Matches today’s whim', kicks: { M: .06, E: .05, C: .18, A: 0,  S: .04 } },
  reunion:  { label: 'Returns after absence', kicks: { M: .10, E: .06, C: .05, A: .15, S: .10 } },
  idle:     { label: 'Walks away / idle', kicks: { M: -.03, E: -.05, C: -.06, A: 0, S: -.08 } },
  repeat:   { label: 'Same object again', kicks: { M: 0, E: -.04, C: -.10, A: 0, S: -.03 } },
};

const WHIMS = ['round things', 'the color blue', 'spirals', 'soft things', 'shiny things', 'tiny things'];

// Companion trait baselines (L0) — personality
const COMPANIONS = {
  Pip:  { name: 'Pip (The Curious One)',  base: { M: .62, E: .60, C: .80, A: .55, S: .62 } },
  Lumi: { name: 'Lumi (The Gentle One)',  base: { M: .66, E: .45, C: .55, A: .75, S: .42 } },
  Tale: { name: 'Tale (The Storyteller)', base: { M: .60, E: .50, C: .62, A: .60, S: .70 } },
};

const clamp01 = (v) => Math.max(0, Math.min(1, v));

// Circadian envelope R(t) ∈ [-1,1], t in hours [0,24) (Doc 2.0 §6)
// double-bump: morning rise, midday peak, afternoon dip, evening fall
function circadian(t) {
  const morning = Math.exp(-Math.pow((t - 10) / 3.0, 2));   // peak ~10am
  const afternoon = Math.exp(-Math.pow((t - 15.5) / 2.6, 2)) * 0.85; // ~3:30pm
  const nightFloor = -Math.exp(-Math.pow((t - 22) / 3.2, 2)) * 1.1
                     - Math.exp(-Math.pow((t - 2) / 4) ** 2) * 1.0;
  const base = morning + afternoon + nightFloor;
  return Math.max(-1, Math.min(1, base));
}

// rhythm shift per variable: how strongly R modulates each baseline
const RHYTHM_SHIFT = { M: 0.10, E: 0.45, C: 0.18, A: 0.05, S: 0.15 };

function band(v, loW, hiW, midW) {
  if (v < 0.34) return loW;
  if (v > 0.66) return hiW;
  return midW;
}

function makeMoodLine(name, st, t, whim) {
  const e = st.E, m = st.M, c = st.C, a = st.A, s = st.S;
  const energyW = band(e, 'sleepy and slow', 'bright and bouncy', 'easygoing');
  const moodW   = band(m, 'a little grumpy (but kind)', 'joyful', 'content');
  const curW    = band(c, 'mellow, savoring one thing', 'fascinated, full of questions', 'gently curious');
  const affW    = band(a, 'friendly', 'deeply fond of Naomi', 'warm toward Naomi');
  let timeW = '';
  if (t < 8) timeW = 'It’s early morning, so you’re just waking up. ';
  else if (t >= 19.5) timeW = 'It’s near bedtime, so you’re dreamy and soft-spoken. ';
  else if (t >= 13 && t < 15) timeW = 'It’s afternoon quiet-time, so you’re a bit mellow. ';
  return `You're ${name.split(' ')[0]}. ${timeW}Right now you feel ${energyW} and ${moodW}; you're ${curW}. ` +
         `You're especially charmed by ${whim} today. You feel ${affW}.`;
}

// LED hue from mood, dimmed by low energy (Doc 2.0 §9)
function ledStyle(st) {
  const m = st.M, e = st.E;
  // hue: grumpy(amber-dim) -> content(amber) -> joyful(gold); sleepy pulls toward blue
  let hue, sat, light;
  if (e < 0.35) { hue = 205; sat = 55; light = 45 + m * 12; }       // sleepy -> blue
  else { hue = 38 + m * 14; sat = 70 + m * 20; light = 42 + m * 20; } // awake -> amber/gold
  const pulse = 0.6 + e * 1.8; // motion speed
  return { hue, sat, light, pulse };
}

export default function InnerLifeSimulator() {
  const [companionKey, setCompanionKey] = useState('Pip');
  const companion = COMPANIONS[companionKey];

  const [lambda, setLambda] = useState(0.08);      // homeostasis
  const [rhythmDepth, setRhythmDepth] = useState(1.0);
  const [couplingScale, setCouplingScale] = useState(1.0);
  const [noiseMag, setNoiseMag] = useState(0.02);
  const [speed, setSpeed] = useState(1.0);
  const [running, setRunning] = useState(true);

  const [clock, setClock] = useState(8.0); // hours
  const [state, setState] = useState({ ...companion.base });
  const [whim, setWhim] = useState(WHIMS[0]);
  const [history, setHistory] = useState([]);
  const [growthDays, setGrowthDays] = useState(0);

  const noiseRef = useRef({ M: 0, E: 0, C: 0, A: 0, S: 0 });
  const kickRef = useRef({ M: 0, E: 0, C: 0, A: 0, S: 0 });
  const baseRef = useRef({ ...companion.base });
  const lastWhimDay = useRef(0);

  // reset on companion change
  useEffect(() => {
    baseRef.current = { ...companion.base };
    setState({ ...companion.base });
    setHistory([]);
    setGrowthDays(0);
    setClock(8.0);
  }, [companionKey]);

  const fireEvent = useCallback((key) => {
    const ev = EVENTS[key];
    if (!ev) return;
    VARS.forEach(v => { kickRef.current[v] += ev.kicks[v] || 0; });
  }, []);

  // main integration tick
  useEffect(() => {
    if (!running) return;
    const id = setInterval(() => {
      setClock(prevClock => {
        const dtHours = 0.12 * speed;
        let nc = prevClock + dtHours;
        if (nc >= 24) {
          nc -= 24;
          setGrowthDays(d => d + 1);
        }
        return nc;
      });
    }, 90);
    return () => clearInterval(id);
  }, [running, speed]);

  // state update keyed to clock
  useEffect(() => {
    const R = circadian(clock) * rhythmDepth;

    // new whim each morning
    const day = growthDays;
    if (clock >= 7 && clock < 7.5 && lastWhimDay.current !== day) {
      lastWhimDay.current = day;
      setWhim(WHIMS[Math.floor(Math.random() * WHIMS.length)]);
    }

    setState(prev => {
      const next = {};
      // autocorrelated noise update
      VARS.forEach(v => {
        noiseRef.current[v] = 0.8 * noiseRef.current[v] + (Math.random() - 0.5) * noiseMag;
      });
      VARS.forEach(x => {
        const x0 = baseRef.current[x];
        const x0eff = clamp01(x0 + RHYTHM_SHIFT[x] * R);
        // homeostasis
        let dx = lambda * (x0eff - prev[x]);
        // coupling
        VARS.forEach(j => {
          if (j !== x) dx += couplingScale * COUPLING[x][j] * (prev[j] - 0.5);
        });
        // event kick (decays)
        dx += kickRef.current[x];
        // noise
        dx += noiseRef.current[x];
        next[x] = clamp01(prev[x] + dx);
      });
      // decay kicks
      VARS.forEach(v => { kickRef.current[v] *= 0.6; if (Math.abs(kickRef.current[v]) < 0.001) kickRef.current[v] = 0; });
      return next;
    });

    // slow growth: affection baseline creeps up with days together
    baseRef.current.A = clamp01(companion.base.A + Math.min(0.18, growthDays * 0.012));

    setHistory(h => {
      const nh = [...h, { t: clock + growthDays * 24, ...state }];
      return nh.length > 240 ? nh.slice(-240) : nh;
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clock]);

  const led = ledStyle(state);
  const moodLine = makeMoodLine(companion.name, state, clock, whim);
  const hh = Math.floor(clock);
  const mm = Math.floor((clock - hh) * 60);
  const timeStr = `${String(hh).padStart(2, '0')}:${String(mm).padStart(2, '0')}`;
  const R = circadian(clock) * rhythmDepth;

  return (
    <div style={{
      fontFamily: '"Iowan Old Style", "Palatino Linotype", Georgia, serif',
      background: 'radial-gradient(ellipse at 50% 0%, #1a2438 0%, #0d1320 60%, #080b14 100%)',
      color: '#e8e4d8', minHeight: '100vh', padding: '24px 18px', boxSizing: 'border-box',
    }}>
      <style>{`
        @keyframes breathe { 0%,100%{transform:scale(1);opacity:.85} 50%{transform:scale(1.08);opacity:1} }
        .knob { width:100%; accent-color:#e8b04b; }
        .ev-btn { font-family:inherit; cursor:pointer; border:1px solid #3a4860; background:#141d2e;
          color:#e8e4d8; padding:8px 10px; border-radius:8px; font-size:12.5px; transition:all .15s; }
        .ev-btn:hover { background:#1f2c44; border-color:#e8b04b; }
        .panel { background:rgba(20,29,46,.6); border:1px solid #283449; border-radius:14px; padding:16px; }
      `}</style>

      <div style={{ maxWidth: 1040, margin: '0 auto' }}>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 14, flexWrap: 'wrap', marginBottom: 4 }}>
          <h1 style={{ fontSize: 26, margin: 0, letterSpacing: '.5px', fontWeight: 600 }}>The Inner Life Engine</h1>
          <span style={{ fontSize: 13, color: '#7f8aa3', fontStyle: 'italic' }}>
            a coupled damped system, pulled toward baseline, perturbed by the world and the day
          </span>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'minmax(280px,1fr) minmax(300px,1.3fr)', gap: 16, marginTop: 16 }}>

          {/* LEFT: the creature */}
          <div className="panel" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', width: '100%', fontSize: 12.5, color: '#9aa6bf' }}>
              <span>day {growthDays + 1}</span>
              <span style={{ fontVariantNumeric: 'tabular-nums', fontSize: 15, color: '#e8e4d8' }}>{timeStr}</span>
              <span>R(t) {R >= 0 ? '+' : ''}{R.toFixed(2)}</span>
            </div>

            {/* LED ring */}
            <div style={{ position: 'relative', width: 200, height: 200, margin: '22px 0' }}>
              <div style={{
                position: 'absolute', inset: 0, borderRadius: '50%',
                background: `radial-gradient(circle, hsl(${led.hue} ${led.sat}% ${led.light}%) 0%, hsl(${led.hue} ${led.sat}% ${led.light * 0.5}%) 55%, transparent 72%)`,
                boxShadow: `0 0 ${30 + state.E * 60}px hsl(${led.hue} ${led.sat}% ${led.light}% / .7)`,
                animation: `breathe ${led.pulse.toFixed(2)}s ease-in-out infinite`,
              }} />
              <div style={{
                position: 'absolute', inset: 58, borderRadius: '50%',
                background: '#0a0e18', display: 'flex', alignItems: 'center', justifyContent: 'center',
                flexDirection: 'column', border: '1px solid #283449',
              }}>
                <div style={{ fontSize: 11, color: '#7f8aa3' }}>charmed by</div>
                <div style={{ fontSize: 13, color: `hsl(${led.hue} ${led.sat}% ${Math.min(75, led.light + 25)}%)`, textAlign: 'center', padding: '0 8px', fontStyle: 'italic' }}>{whim}</div>
              </div>
            </div>

            <select value={companionKey} onChange={e => setCompanionKey(e.target.value)}
              style={{ fontFamily: 'inherit', background: '#141d2e', color: '#e8e4d8', border: '1px solid #3a4860', borderRadius: 8, padding: '7px 10px', fontSize: 13, width: '100%' }}>
              {Object.entries(COMPANIONS).map(([k, c]) => <option key={k} value={k}>{c.name}</option>)}
            </select>

            {/* mood line */}
            <div style={{ marginTop: 16, fontSize: 13.5, lineHeight: 1.5, color: '#cdd4e4', fontStyle: 'italic',
              borderLeft: `3px solid hsl(${led.hue} ${led.sat}% ${led.light}%)`, paddingLeft: 12, alignSelf: 'stretch' }}>
              "{moodLine}"
            </div>
            <div style={{ fontSize: 10.5, color: '#5b6478', marginTop: 8, alignSelf: 'stretch' }}>
              ↑ this is the only thing sent to the LLM — regenerated every turn from the live state
            </div>
          </div>

          {/* RIGHT: state + controls */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>

            {/* state bars */}
            <div className="panel">
              <div style={{ fontSize: 12, color: '#9aa6bf', marginBottom: 10, textTransform: 'uppercase', letterSpacing: '1px' }}>Live state</div>
              {VARS.map(v => {
                const meta = VAR_META[v];
                return (
                  <div key={v} style={{ marginBottom: 9 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11.5, color: '#9aa6bf', marginBottom: 3 }}>
                      <span style={{ color: meta.color, fontWeight: 600 }}>{meta.name}</span>
                      <span>{meta.lo} · <b style={{ color: '#e8e4d8' }}>{state[v].toFixed(2)}</b> · {meta.hi}</span>
                    </div>
                    <div style={{ height: 8, background: '#0d1320', borderRadius: 4, overflow: 'hidden' }}>
                      <div style={{ height: '100%', width: `${state[v] * 100}%`, background: meta.color, borderRadius: 4, transition: 'width .15s linear' }} />
                    </div>
                  </div>
                );
              })}
            </div>

            {/* events */}
            <div className="panel">
              <div style={{ fontSize: 12, color: '#9aa6bf', marginBottom: 10, textTransform: 'uppercase', letterSpacing: '1px' }}>Perturb the world</div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
                {Object.entries(EVENTS).map(([k, ev]) => (
                  <button key={k} className="ev-btn" onClick={() => fireEvent(k)}>{ev.label}</button>
                ))}
              </div>
            </div>

            {/* knobs */}
            <div className="panel">
              <div style={{ fontSize: 12, color: '#9aa6bf', marginBottom: 12, textTransform: 'uppercase', letterSpacing: '1px' }}>Tuning knobs</div>
              {[
                ['Homeostasis λ (master feel)', lambda, setLambda, 0.01, 0.3, 0.005],
                ['Rhythm depth (day swing)', rhythmDepth, setRhythmDepth, 0, 2, 0.05],
                ['Coupling scale', couplingScale, setCouplingScale, 0, 2.5, 0.05],
                ['Noise magnitude', noiseMag, setNoiseMag, 0, 0.08, 0.002],
                ['Sim speed', speed, setSpeed, 0.2, 6, 0.2],
              ].map(([label, val, set, min, max, step]) => (
                <div key={label} style={{ marginBottom: 10 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11.5, color: '#9aa6bf', marginBottom: 3 }}>
                    <span>{label}</span><span style={{ color: '#e8b04b', fontVariantNumeric: 'tabular-nums' }}>{Number(val).toFixed(3)}</span>
                  </div>
                  <input className="knob" type="range" min={min} max={max} step={step} value={val}
                    onChange={e => set(parseFloat(e.target.value))} />
                </div>
              ))}
              <div style={{ display: 'flex', gap: 8, marginTop: 6 }}>
                <button className="ev-btn" style={{ flex: 1 }} onClick={() => setRunning(r => !r)}>{running ? '⏸ pause' : '▶ run'}</button>
                <button className="ev-btn" style={{ flex: 1 }} onClick={() => { setState({ ...baseRef.current }); setHistory([]); }}>reset state</button>
              </div>
            </div>
          </div>
        </div>

        {/* trace */}
        <div className="panel" style={{ marginTop: 16 }}>
          <div style={{ fontSize: 12, color: '#9aa6bf', marginBottom: 8, textTransform: 'uppercase', letterSpacing: '1px' }}>
            State trace (watch a day unfold — look for coherence, not noise)
          </div>
          <Trace history={history} />
          <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', marginTop: 8, fontSize: 11.5 }}>
            {VARS.map(v => (
              <span key={v} style={{ color: VAR_META[v].color }}>{VAR_META[v].name}</span>
            ))}
          </div>
        </div>

        <div style={{ fontSize: 11.5, color: '#5b6478', marginTop: 14, lineHeight: 1.6, textAlign: 'center' }}>
          Fire <b>Shows something new</b> repeatedly and watch Curiosity spike then settle (homeostasis). Drop λ near 0.01 and watch moods drift to the walls and stick — that's the dead-at-the-wall failure. Push Rhythm depth up and let the clock run to bedtime: Energy sags, and coupling drags Curiosity and Sociability down with it — calm becomes structural, not scripted.
        </div>
      </div>
    </div>
  );
}

function Trace({ history }) {
  const W = 1000, H = 150, pad = 4;
  if (history.length < 2) return <div style={{ height: H, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#5b6478', fontSize: 13 }}>letting it live…</div>;
  const n = history.length;
  const x = (i) => pad + (i / (n - 1)) * (W - pad * 2);
  const y = (v) => pad + (1 - v) * (H - pad * 2);
  return (
    <svg viewBox={`0 0 ${W} ${H}`} style={{ width: '100%', height: H, display: 'block' }}>
      {[0.25, 0.5, 0.75].map(g => (
        <line key={g} x1={pad} x2={W - pad} y1={y(g)} y2={y(g)} stroke="#1c2638" strokeWidth="1" />
      ))}
      {VARS.map(v => {
        const d = history.map((pt, i) => `${i === 0 ? 'M' : 'L'} ${x(i).toFixed(1)} ${y(pt[v]).toFixed(1)}`).join(' ');
        return <path key={v} d={d} fill="none" stroke={VAR_META[v].color} strokeWidth="1.8" opacity="0.9" />;
      })}
    </svg>
  );
}
