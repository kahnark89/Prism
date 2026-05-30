import React, { useState, useRef, useCallback, useEffect } from 'react';

// ============================================================================
// PRISM — Memory Engine · Live Simulator (Doc 2.1)
// Salience-gated encoding · salience-weighted forgetting · rehearsal/consolidation
// · spreading activation · codebook (interest profile) crystallization · pruning.
// Watch weeks pass: the right things survive, specifics fade, gist remains.
// ============================================================================

// A small world of things she might show the companion. Each maps to a concept.
const WORLD = [
  { episode: 'a snail', concept: 'spirals', emoji: '🐌' },
  { episode: 'a pinecone', concept: 'spirals', emoji: '🌲' },
  { episode: 'bathwater swirl', concept: 'spirals', emoji: '🌀' },
  { episode: 'a red leaf', concept: 'the color red', emoji: '🍁' },
  { episode: 'a fire truck', concept: 'the color red', emoji: '🚒' },
  { episode: 'a ladybug', concept: 'tiny creatures', emoji: '🐞' },
  { episode: 'an ant', concept: 'tiny creatures', emoji: '🐜' },
  { episode: 'a dog', concept: 'loud animals', emoji: '🐕' },
  { episode: 'a drum', concept: 'loud things', emoji: '🥁' },
  { episode: 'a soft blanket', concept: 'soft things', emoji: '🧸' },
  { episode: 'a cloud', concept: 'soft things', emoji: '☁️' },
  { episode: 'a shiny spoon', concept: 'shiny things', emoji: '🥄' },
];

const REACTIONS = [
  { label: 'delighted! 😄', salience: 0.9 },
  { label: 'curious 🤔', salience: 0.6 },
  { label: 'meh 😐', salience: 0.25 },
  { label: 'struggled then got it 💡', salience: 0.85 },
];

const clamp01 = v => Math.max(0, Math.min(1, v));
let NID = 1;

export default function MemorySimulator() {
  // knobs (Doc 2.1 §12)
  const [tauBase, setTauBase] = useState(2.0);      // days, base decay constant
  const [tauScale, setTauScale] = useState(14.0);   // salience extends tau (days)
  const [gate, setGate] = useState(0.35);           // salience gate threshold
  const [rho, setRho] = useState(0.5);              // rehearsal kick
  const [consol, setConsol] = useState(0.35);       // consolidation factor c
  const [pruneFloor, setPruneFloor] = useState(0.05);
  const [spread, setSpread] = useState(0.4);        // spreading activation strength

  const [nodes, setNodes] = useState([]);           // episodic memory nodes
  const [codebook, setCodebook] = useState({});     // concept -> {strength, count}
  const [day, setDay] = useState(0);
  const [log, setLog] = useState([]);
  const [lastCallback, setLastCallback] = useState('');

  const nodesRef = useRef(nodes); nodesRef.current = nodes;
  const cbRef = useRef(codebook); cbRef.current = codebook;

  const pushLog = (msg) => setLog(l => [{ day: dayRef.current, msg }, ...l].slice(0, 40));
  const dayRef = useRef(day); dayRef.current = day;

  // Encode an experience
  const experience = useCallback((item, reaction) => {
    const sal = reaction.salience;
    if (sal < gate) {
      pushLog(`saw ${item.emoji} ${item.episode} — ${reaction.label} — below gate, forgotten instantly`);
      return;
    }
    const tau = tauBase + tauScale * sal;

    setNodes(prev => {
      // spreading activation: reactivate existing same-concept nodes (this is rehearsal + the learning signal)
      let rehearsed = 0;
      const bumped = prev.map(n => {
        if (n.concept === item.concept) {
          rehearsed++;
          return { ...n, s: clamp01(n.s + rho), tau: n.tau * (1 + consol), rehearsals: n.rehearsals + 1, lastDay: dayRef.current,
                   contexts: n.contexts.includes(item.episode) ? n.contexts : [...n.contexts, item.episode] };
        }
        // partial spread to linked concepts (loose association)
        return n;
      });
      // new episodic node
      const node = {
        id: NID++, concept: item.concept, episode: item.episode, emoji: item.emoji,
        salience: sal, s: clamp01(0.55 + sal * 0.45), tau, rehearsals: 0, lastDay: dayRef.current,
        contexts: [item.episode],
      };
      if (rehearsed > 0) {
        setLastCallback(`"Ooh, ${item.episode} — like the other ${item.concept} you showed me!"`);
        pushLog(`saw ${item.emoji} ${item.episode} — ${reaction.label} — encoded + rehearsed ${rehearsed} related (concept "${item.concept}" consolidating ↑)`);
      } else {
        pushLog(`saw ${item.emoji} ${item.episode} — ${reaction.label} — encoded as NEW concept "${item.concept}"`);
        setLastCallback(`"A ${item.episode}! I don't think I've seen ${item.concept} before."`);
      }
      return [...bumped, node];
    });

    // codebook: gist layer — strengthen concept, count it
    setCodebook(prev => {
      const e = prev[item.concept] || { strength: 0, count: 0 };
      return { ...prev, [item.concept]: { strength: clamp01(e.strength + 0.25 + sal * 0.2), count: e.count + 1 } };
    });
  }, [gate, tauBase, tauScale, rho, consol]);

  // Advance time by dt days: decay all episodic nodes, prune, gently decay codebook slower
  const advance = useCallback((dt) => {
    setDay(d => d + dt);
    setNodes(prev => {
      const decayed = prev.map(n => {
        const age = dt; // days since last step
        const s = n.s * Math.exp(-age / n.tau);
        return { ...n, s };
      }).filter(n => n.s >= pruneFloor);
      const prunedCount = prev.length - decayed.length;
      if (prunedCount > 0) pushLog(`⏳ ${dt}d passed — ${prunedCount} specific memor${prunedCount===1?'y':'ies'} faded & pruned (gist remains in profile)`);
      else pushLog(`⏳ ${dt}d passed — memories decayed`);
      return decayed;
    });
    // codebook (gist) decays much slower — concepts persist after episodes prune
    setCodebook(prev => {
      const next = {};
      Object.entries(prev).forEach(([k, v]) => {
        const s = v.strength * Math.exp(-dt / (tauBase * 12)); // ~12x slower
        if (s >= 0.02) next[k] = { ...v, strength: s };
      });
      return next;
    });
  }, [pruneFloor, tauBase]);

  // a "typical day" — she shows 2-3 things with random reactions, biased toward her real interests
  const liveADay = useCallback(() => {
    const k = 2 + Math.floor(Math.random() * 2);
    for (let i = 0; i < k; i++) {
      // bias: things whose concept is already strong in codebook are more likely to recur (she revisits interests)
      const cb = cbRef.current;
      let pool = WORLD;
      if (Math.random() < 0.6 && Object.keys(cb).length) {
        const favs = Object.entries(cb).sort((a,b)=>b[1].strength-a[1].strength).slice(0,3).map(e=>e[0]);
        const biased = WORLD.filter(w => favs.includes(w.concept));
        if (biased.length) pool = biased;
      }
      const item = pool[Math.floor(Math.random() * pool.length)];
      const reaction = REACTIONS[Math.floor(Math.random() * REACTIONS.length)];
      setTimeout(() => experience(item, reaction), i * 60);
    }
    setTimeout(() => advance(1), k * 60 + 40);
  }, [experience, advance]);

  const fastForward = useCallback((days) => {
    for (let d = 0; d < days; d++) setTimeout(() => liveADay(), d * 30);
  }, [liveADay]);

  const reset = () => { setNodes([]); setCodebook({}); setDay(0); setLog([]); setLastCallback(''); };

  const sortedCb = Object.entries(codebook).sort((a, b) => b[1].strength - a[1].strength);
  const maxNodes = 14;
  const shownNodes = [...nodes].sort((a, b) => b.s - a.s).slice(0, maxNodes);

  return (
    <div style={{
      fontFamily: '"Iowan Old Style", "Palatino Linotype", Georgia, serif',
      background: 'radial-gradient(ellipse at 50% -10%, #20283a 0%, #11151f 55%, #0a0d14 100%)',
      color: '#e9e5d9', minHeight: '100vh', padding: '24px 18px', boxSizing: 'border-box',
    }}>
      <style>{`
        .mbtn { font-family:inherit; cursor:pointer; border:1px solid #3a4860; background:#141d2e;
          color:#e9e5d9; padding:9px 12px; border-radius:9px; font-size:13px; transition:all .15s; }
        .mbtn:hover { background:#1f2c44; border-color:#d8a24a; }
        .mpanel { background:rgba(20,27,42,.62); border:1px solid #283449; border-radius:14px; padding:16px; }
        .knob { width:100%; accent-color:#d8a24a; }
        @keyframes fadein { from{opacity:0;transform:translateY(4px)} to{opacity:1;transform:none} }
      `}</style>

      <div style={{ maxWidth: 1060, margin: '0 auto' }}>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 14, flexWrap: 'wrap' }}>
          <h1 style={{ fontSize: 26, margin: 0, fontWeight: 600 }}>The Memory Engine</h1>
          <span style={{ fontSize: 13, color: '#7f8aa3', fontStyle: 'italic' }}>
            forgetting is the feature · what she keeps caring about is what survives
          </span>
          <span style={{ marginLeft: 'auto', fontSize: 15, color: '#d8a24a', fontVariantNumeric: 'tabular-nums' }}>
            day {Math.round(day)}
          </span>
        </div>

        {/* controls */}
        <div className="mpanel" style={{ marginTop: 14, display: 'flex', gap: 10, flexWrap: 'wrap', alignItems: 'center' }}>
          <button className="mbtn" onClick={liveADay}>▶ live one day</button>
          <button className="mbtn" onClick={() => fastForward(7)}>⏩ a week</button>
          <button className="mbtn" onClick={() => fastForward(30)}>⏩ a month</button>
          <button className="mbtn" onClick={() => advance(7)}>💤 a week away (decay only)</button>
          <button className="mbtn" onClick={reset} style={{ marginLeft: 'auto' }}>reset</button>
        </div>

        {lastCallback && (
          <div style={{ marginTop: 12, fontSize: 14.5, fontStyle: 'italic', color: '#e9d9b8',
            borderLeft: '3px solid #d8a24a', paddingLeft: 12 }}>{lastCallback}</div>
        )}

        <div style={{ display: 'grid', gridTemplateColumns: '1.25fr 1fr', gap: 16, marginTop: 16 }}>

          {/* episodic memory */}
          <div className="mpanel">
            <div style={{ fontSize: 12, color: '#9aa6bf', marginBottom: 4, textTransform: 'uppercase', letterSpacing: '1px' }}>
              Episodic memory (verbatim — fades fast)
            </div>
            <div style={{ fontSize: 11, color: '#6b7488', marginBottom: 12 }}>
              bar = strength · these are the specific moments; low-salience ones prune away
            </div>
            {shownNodes.length === 0 && <div style={{ color: '#5b6478', fontSize: 13, padding: '20px 0' }}>no memories yet — live a day</div>}
            {shownNodes.map(n => (
              <div key={n.id} style={{ marginBottom: 8, animation: 'fadein .3s' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12.5, marginBottom: 2 }}>
                  <span>{n.emoji} {n.episode} <span style={{ color: '#6b7488' }}>· {n.concept}{n.rehearsals > 0 ? ` · rehearsed ×${n.rehearsals}` : ''}</span></span>
                  <span style={{ color: '#6b7488', fontVariantNumeric: 'tabular-nums' }}>{n.s.toFixed(2)}</span>
                </div>
                <div style={{ height: 6, background: '#0d1320', borderRadius: 3, overflow: 'hidden' }}>
                  <div style={{ height: '100%', width: `${n.s * 100}%`,
                    background: n.rehearsals > 1 ? '#6bc88a' : '#d8a24a', borderRadius: 3, transition: 'width .3s' }} />
                </div>
              </div>
            ))}
            {nodes.length > maxNodes && <div style={{ fontSize: 11, color: '#5b6478', marginTop: 6 }}>+{nodes.length - maxNodes} weaker memories…</div>}
          </div>

          {/* codebook / interest profile */}
          <div className="mpanel">
            <div style={{ fontSize: 12, color: '#9aa6bf', marginBottom: 4, textTransform: 'uppercase', letterSpacing: '1px' }}>
              Interest profile (gist — persists)
            </div>
            <div style={{ fontSize: 11, color: '#6b7488', marginBottom: 12 }}>
              "who Naomi is" · survives after the specific episodes are forgotten · feeds the parent suite & daily whim
            </div>
            {sortedCb.length === 0 && <div style={{ color: '#5b6478', fontSize: 13, padding: '20px 0' }}>profile forms as she explores</div>}
            {sortedCb.map(([concept, v]) => (
              <div key={concept} style={{ marginBottom: 10, animation: 'fadein .3s' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, marginBottom: 2 }}>
                  <span style={{ color: '#e9d9b8' }}>{concept}</span>
                  <span style={{ color: '#6b7488', fontSize: 11.5 }}>seen ×{v.count}</span>
                </div>
                <div style={{ height: 8, background: '#0d1320', borderRadius: 4, overflow: 'hidden' }}>
                  <div style={{ height: '100%', width: `${v.strength * 100}%`, background: 'linear-gradient(90deg,#d8a24a,#e9c878)', borderRadius: 4, transition: 'width .3s' }} />
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* knobs + log */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginTop: 16 }}>
          <div className="mpanel">
            <div style={{ fontSize: 12, color: '#9aa6bf', marginBottom: 12, textTransform: 'uppercase', letterSpacing: '1px' }}>Tuning</div>
            {[
              ['Base τ (days)', tauBase, setTauBase, 0.5, 6, 0.25],
              ['Salience→τ (days)', tauScale, setTauScale, 0, 40, 1],
              ['Salience gate', gate, setGate, 0, 0.9, 0.05],
              ['Rehearsal kick ρ', rho, setRho, 0, 1, 0.05],
              ['Consolidation c', consol, setConsol, 0, 1, 0.05],
              ['Prune floor', pruneFloor, setPruneFloor, 0.01, 0.3, 0.01],
            ].map(([label, val, set, min, max, step]) => (
              <div key={label} style={{ marginBottom: 9 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11.5, color: '#9aa6bf', marginBottom: 3 }}>
                  <span>{label}</span><span style={{ color: '#d8a24a', fontVariantNumeric: 'tabular-nums' }}>{Number(val).toFixed(2)}</span>
                </div>
                <input className="knob" type="range" min={min} max={max} step={step} value={val} onChange={e => set(parseFloat(e.target.value))} />
              </div>
            ))}
          </div>

          <div className="mpanel" style={{ display: 'flex', flexDirection: 'column' }}>
            <div style={{ fontSize: 12, color: '#9aa6bf', marginBottom: 10, textTransform: 'uppercase', letterSpacing: '1px' }}>What's happening</div>
            <div style={{ overflowY: 'auto', maxHeight: 230, fontSize: 12, lineHeight: 1.5 }}>
              {log.length === 0 && <div style={{ color: '#5b6478' }}>start living days to see encoding, rehearsal, and forgetting…</div>}
              {log.map((e, i) => (
                <div key={i} style={{ marginBottom: 6, color: e.msg.includes('rehearsed') ? '#9ad6ad' : e.msg.includes('faded') ? '#c98b8b' : e.msg.includes('NEW concept') ? '#e9c878' : '#aab4c8' }}>
                  <span style={{ color: '#5b6478' }}>d{e.day}·</span> {e.msg}
                </div>
              ))}
            </div>
          </div>
        </div>

        <div style={{ fontSize: 11.5, color: '#5b6478', marginTop: 14, lineHeight: 1.6, textAlign: 'center' }}>
          Run <b>a month</b>, then hit <b>a week away</b> a couple times. Watch the specific episodes (amber) prune out while the interest profile (gist) survives — that's verbatim-fades / gist-persists, which is both the human feel and the privacy guarantee. Green bars are memories rehearsed across contexts: those are <i>exactly</i> the concepts she's learning (Doc 1.8) — the memory consolidating and the learning grounding signal are the same event.
        </div>
      </div>
    </div>
  );
}
