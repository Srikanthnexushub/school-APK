import { useState, useCallback, useRef } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { GripHorizontal, Mic, X } from 'lucide-react';
import { useAuthStore } from '../stores/authStore';
import { useVoiceAgent } from './useVoiceAgent';
import { VoiceOrb } from './VoiceOrb';
import { personaForRole, type UserRole } from './voicePersonas';

/**
 * Floating voice agent widget — draggable anywhere on screen.
 *
 * Drag: grab the panel header grip or the mic button to reposition.
 * Panel stacks above the trigger button in a single draggable wrapper.
 * Supports mouse (desktop) and touch (mobile).
 */
export default function VoiceFloatingWidget() {
  const { user, token } = useAuthStore();
  const [open, setOpen] = useState(false);
  const constraintRef = useRef<HTMLDivElement>(null);

  const role = (user?.role ?? 'STUDENT') as UserRole;
  const persona = personaForRole(role);
  const agent = useVoiceAgent(role, token ?? '');

  const handleToggle = useCallback(() => {
    if (open) {
      agent.disconnect();
      setOpen(false);
    } else {
      setOpen(true);
      agent.connect();
    }
  }, [open, agent]);

  if (!user || !token) return null;

  return (
    <>
      {/* Full-viewport drag constraint */}
      <div ref={constraintRef} className="fixed inset-0 z-40 pointer-events-none" />

      {/* Single draggable wrapper — panel + trigger button move together */}
      <motion.div
        drag
        dragMomentum={false}
        dragElastic={0}
        dragConstraints={constraintRef}
        className="fixed bottom-24 right-6 z-50 flex flex-col items-end gap-2 select-none"
        style={{ touchAction: 'none' }}
      >
        {/* ── Voice panel ──────────────────────────────────────────────────── */}
        <AnimatePresence>
          {open && (
            <motion.div
              initial={{ opacity: 0, scale: 0.92, y: 8 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.92, y: 8 }}
              transition={{ duration: 0.22, ease: 'easeOut' }}
              className="w-[300px] rounded-2xl border shadow-2xl overflow-hidden"
              style={{
                background: 'linear-gradient(135deg, #0a0d1a 0%, #0f1629 100%)',
                borderColor: `${persona.accentHex}30`,
                boxShadow: `0 0 40px ${persona.glowColor}, 0 20px 60px rgba(0,0,0,0.6)`,
              }}
            >
              {/* Header — drag handle */}
              <div
                className="flex items-center justify-between px-4 py-3 border-b cursor-grab active:cursor-grabbing"
                style={{ borderColor: `${persona.accentHex}20` }}
              >
                <div className="flex items-center gap-2">
                  <GripHorizontal className="w-3.5 h-3.5 text-gray-600" />
                  <div
                    className="w-2 h-2 rounded-full animate-pulse"
                    style={{ background: persona.accentHex, boxShadow: `0 0 6px ${persona.accentHex}` }}
                  />
                  <span
                    className="text-xs font-bold tracking-widest uppercase"
                    style={{ color: persona.accentHex }}
                  >
                    {persona.name}
                  </span>
                </div>
                <button
                  onClick={e => { e.stopPropagation(); handleToggle(); }}
                  onPointerDown={e => e.stopPropagation()}
                  className="p-1 rounded-lg transition-colors text-gray-500 hover:text-gray-300 hover:bg-white/5"
                  aria-label="Close voice agent"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              </div>

              {/* Orb content */}
              <div className="flex items-center justify-center py-4 px-3 overflow-y-auto max-h-[420px]">
                <VoiceOrb agent={agent} size={80} />
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* ── Trigger button ────────────────────────────────────────────────── */}
        <motion.button
          onClick={handleToggle}
          whileHover={{ scale: 1.08 }}
          whileTap={{ scale: 0.94 }}
          className="w-12 h-12 rounded-full flex items-center justify-center shadow-xl cursor-grab active:cursor-grabbing"
          style={{
            background: open
              ? `linear-gradient(135deg, ${persona.accentHex}cc, ${persona.accentHex}66)`
              : 'linear-gradient(135deg, #1e2235, #0f1117)',
            border: `1.5px solid ${open ? persona.accentHex : persona.accentHex + '50'}`,
            boxShadow: open
              ? `0 0 20px ${persona.glowColor}, 0 4px 16px rgba(0,0,0,0.4)`
              : `0 4px 16px rgba(0,0,0,0.3)`,
          }}
          title={open ? 'Close voice agent' : `Talk to ${persona.name}`}
          aria-label={open ? 'Close voice agent' : `Talk to ${persona.name}`}
        >
          <AnimatePresence mode="wait">
            {open ? (
              <motion.div key="close" initial={{ rotate: -90, opacity: 0 }} animate={{ rotate: 0, opacity: 1 }} exit={{ rotate: 90, opacity: 0 }} transition={{ duration: 0.15 }}>
                <X className="w-4 h-4 text-white" />
              </motion.div>
            ) : (
              <motion.div key="mic" initial={{ scale: 0.7, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} exit={{ scale: 0.7, opacity: 0 }} transition={{ duration: 0.15 }}>
                <Mic className="w-4 h-4" style={{ color: persona.accentHex }} />
              </motion.div>
            )}
          </AnimatePresence>
        </motion.button>
      </motion.div>
    </>
  );
}
