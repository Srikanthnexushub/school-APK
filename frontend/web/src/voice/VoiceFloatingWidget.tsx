import { useState, useCallback } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { Mic, X } from 'lucide-react';
import { useAuthStore } from '../stores/authStore';
import { useVoiceAgent } from './useVoiceAgent';
import { VoiceOrb } from './VoiceOrb';
import { personaForRole, type UserRole } from './voicePersonas';

/**
 * Floating voice agent widget — fixed bottom-right, sits above NexusChatWidget.
 *
 * Trigger: small mic button at bottom-28 right-6
 * Panel: compact card above the trigger button, contains the VoiceOrb
 */
export default function VoiceFloatingWidget() {
  const { user, token } = useAuthStore();
  const [open, setOpen] = useState(false);

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
      {/* ── Floating panel ─────────────────────────────────────────────────── */}
      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, scale: 0.92, y: 16 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.92, y: 16 }}
            transition={{ duration: 0.22, ease: 'easeOut' }}
            className="fixed bottom-[7.5rem] right-6 z-50 w-[260px] rounded-2xl border shadow-2xl overflow-hidden"
            style={{
              background: 'linear-gradient(135deg, #0a0d1a 0%, #0f1629 100%)',
              borderColor: `${persona.accentHex}30`,
              boxShadow: `0 0 40px ${persona.glowColor}, 0 20px 60px rgba(0,0,0,0.6)`,
            }}
          >
            {/* Header */}
            <div
              className="flex items-center justify-between px-4 py-3 border-b"
              style={{ borderColor: `${persona.accentHex}20` }}
            >
              <div className="flex items-center gap-2">
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
                onClick={handleToggle}
                className="p-1 rounded-lg transition-colors text-gray-500 hover:text-gray-300 hover:bg-white/5"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>

            {/* Orb */}
            <div className="flex items-center justify-center py-6">
              <VoiceOrb agent={agent} size={80} />
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* ── Trigger button ─────────────────────────────────────────────────── */}
      <motion.button
        onClick={handleToggle}
        whileHover={{ scale: 1.08 }}
        whileTap={{ scale: 0.94 }}
        className="fixed bottom-28 right-6 z-50 w-12 h-12 rounded-full flex items-center justify-center shadow-xl"
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
    </>
  );
}
