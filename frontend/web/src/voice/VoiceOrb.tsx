import React, { useCallback } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { Mic, Loader2, Volume2, AlertCircle, Radio } from 'lucide-react';
import type { VoiceState, UseVoiceAgentReturn } from './useVoiceAgent';
import type { VoicePersonaConfig } from './voicePersonas';
import { VoiceWaveform } from './VoiceWaveform';

interface VoiceOrbProps {
  agent: UseVoiceAgentReturn;
  /** Size in px (default 96) */
  size?: number;
}

/**
 * The floating voice orb — the primary UI for the enterprise voice agent.
 *
 * States:
 *   idle       → grey "Connect" button
 *   connecting → spinner
 *   ready      → persona orb, tap to speak
 *   listening  → pulsing red ring + live level bars
 *   thinking   → spinner + rotating ring
 *   speaking   → animated equalizer bars, green ring
 *   error      → red flash, error message
 */
export function VoiceOrb({ agent, size = 96 }: VoiceOrbProps) {
  const { state, persona, transcript, messages, error, inputLevel,
          startListening, stopListening, connect, disconnect } = agent;

  const lastAssistant = [...messages].reverse().find(m => m.role === 'assistant');

  const handleOrbClick = useCallback(() => {
    if (state === 'idle')      { connect(); return; }
    if (state === 'ready')     { startListening(); return; }
    if (state === 'listening') { stopListening(); return; }
    if (state === 'speaking')  { startListening(); return; } // barge-in
  }, [state, connect, startListening, stopListening]);

  return (
    <div className="flex flex-col items-center gap-4 select-none">
      {/* ── Orb button ─────────────────────────────────────────────────────── */}
      <div className="relative flex items-center justify-center" style={{ width: size + 40, height: size + 40 }}>
        {/* Glow ring — state-driven */}
        <AnimatePresence>
          {(state === 'listening' || state === 'speaking') && (
            <motion.div
              key="ring"
              className="absolute rounded-full"
              style={{
                width: size + 32,
                height: size + 32,
                boxShadow: `0 0 28px 8px ${persona?.glowColor ?? 'rgba(99,102,241,0.5)'}`,
                border: `2px solid ${persona?.accentHex ?? '#818cf8'}40`,
              }}
              initial={{ opacity: 0, scale: 0.85 }}
              animate={{ opacity: 1, scale: state === 'listening' ? [1, 1.05, 1] : 1 }}
              exit={{ opacity: 0, scale: 0.85 }}
              transition={{ duration: 0.5, repeat: state === 'listening' ? Infinity : 0, ease: 'easeInOut' }}
            />
          )}
          {state === 'thinking' && (
            <motion.div
              key="spin-ring"
              className="absolute rounded-full"
              style={{
                width: size + 32,
                height: size + 32,
                border: `2px solid transparent`,
                borderTopColor: persona?.accentHex ?? '#818cf8',
              }}
              animate={{ rotate: 360 }}
              transition={{ duration: 1.1, repeat: Infinity, ease: 'linear' }}
            />
          )}
        </AnimatePresence>

        {/* Orb itself */}
        <motion.button
          onClick={handleOrbClick}
          disabled={state === 'connecting' || state === 'thinking'}
          className="relative flex items-center justify-center rounded-full overflow-hidden cursor-pointer focus:outline-none"
          style={{
            width: size,
            height: size,
            background: persona
              ? persona.orbGradient
              : 'radial-gradient(circle at 35% 35%, #374151, #111827, #030712)',
          }}
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.95 }}
          aria-label={orbLabel(state)}
        >
          {/* Reflective gloss */}
          <div
            className="absolute top-2 left-3 rounded-full opacity-30"
            style={{ width: size * 0.4, height: size * 0.25,
                     background: 'radial-gradient(ellipse, white, transparent)' }}
          />

          {/* State icon */}
          <AnimatePresence mode="wait">
            <motion.div
              key={state}
              initial={{ opacity: 0, scale: 0.7 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.7 }}
              transition={{ duration: 0.2 }}
            >
              <OrbIcon state={state} size={size * 0.35} />
            </motion.div>
          </AnimatePresence>
        </motion.button>
      </div>

      {state === 'idle' && (
        <p className="text-xs text-gray-500">Tap to start voice agent</p>
      )}

      {/* ── Waveform ──────────────────────────────────────────────────────── */}
      <AnimatePresence>
        {(state === 'listening' || state === 'speaking') && persona && (
          <motion.div
            key="wave"
            initial={{ opacity: 0, y: 4 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 4 }}
          >
            <VoiceWaveform
              level={inputLevel}
              active={true}
              accentHex={persona.accentHex}
            />
          </motion.div>
        )}
      </AnimatePresence>

      {/* ── Status + text feedback ────────────────────────────────────────── */}
      <AnimatePresence>
        {state === 'ready' && !lastAssistant && (
          <motion.p key="welcome" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="text-xs text-gray-300 max-w-[220px] text-center leading-relaxed">
            Hi! I'm SRI. Tap the mic and ask me anything.
          </motion.p>
        )}
        {state === 'listening' && (
          <motion.p key="listening-hint" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="text-xs font-semibold"
            style={{ color: persona?.accentHex ?? '#818cf8' }}>
            Listening… speak now
          </motion.p>
        )}
        {state === 'thinking' && (
          <motion.p key="thinking" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="text-xs text-gray-400">Thinking…</motion.p>
        )}
        {transcript && (state === 'thinking' || state === 'speaking' || state === 'ready') && (
          <motion.p key="transcript" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="text-xs text-gray-400 italic max-w-[220px] text-center leading-snug">
            You: "{transcript}"
          </motion.p>
        )}
        {state === 'speaking' && (
          <motion.p key="speaking" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="text-xs font-semibold"
            style={{ color: persona?.accentHex ?? '#818cf8' }}>
            Speaking
          </motion.p>
        )}
        {lastAssistant && (state === 'speaking' || state === 'ready') && (
          <motion.p key="response" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="text-xs text-gray-200 max-w-[220px] text-center leading-snug">
            {lastAssistant.text}
          </motion.p>
        )}
        {error && (
          <motion.p key="error" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="text-xs text-red-400 max-w-[260px] text-center">
            {error}
          </motion.p>
        )}
      </AnimatePresence>

      {/* ── Disconnect ────────────────────────────────────────────────────── */}
      {state !== 'idle' && (
        <button
          onClick={disconnect}
          className="text-[10px] text-gray-600 hover:text-gray-400 transition-colors mt-1"
        >
          Disconnect
        </button>
      )}
    </div>
  );
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function OrbIcon({ state, size }: { state: VoiceState; size: number }) {
  const s = size;
  switch (state) {
    case 'idle':        return <Radio size={s} className="text-gray-400" />;
    case 'connecting':  return <Loader2 size={s} className="text-gray-300 animate-spin" />;
    case 'ready':       return <Mic size={s} className="text-white" />;
    case 'listening':   return <Mic size={s} className="text-white animate-pulse" />;
    case 'thinking':    return <Loader2 size={s} className="text-white animate-spin" />;
    case 'speaking':    return <Volume2 size={s} className="text-white" />;
    case 'error':       return <AlertCircle size={s} className="text-red-300" />;
  }
}

function orbLabel(state: VoiceState): string {
  switch (state) {
    case 'idle':       return 'Connect voice agent';
    case 'connecting': return 'Connecting…';
    case 'ready':      return 'Tap to speak';
    case 'listening':  return 'Tap to stop recording';
    case 'thinking':   return 'Processing…';
    case 'speaking':   return 'AI is speaking — tap to interrupt';
    case 'error':      return 'Error — tap to retry';
  }
}
