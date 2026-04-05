import React, { useEffect, useRef } from 'react';
import { GripHorizontal, MessageCircle, X } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { useChatStore } from '../../store/chatStore';
import ChatPanel from './ChatPanel';
import { useLocation } from 'react-router-dom';

/**
 * NexusChatWidget — draggable anywhere on screen.
 *
 * Drag: grab the panel header grip or the chat button to reposition.
 * Panel stacks above the trigger button in a single draggable wrapper.
 * Supports mouse (desktop) and touch (mobile).
 */
const NexusChatWidget: React.FC = () => {
  const { isOpen, openChat, closeChat, unreadNudgeCount, startSession, activeSessionId } = useChatStore();
  const location = useLocation();
  const prevPathRef = useRef<string>(location.pathname);
  const constraintRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (isOpen && !activeSessionId) {
      const ctx = location.pathname.replace(/^\/dashboard\//, '').replace(/^\//, '') || 'dashboard';
      startSession(ctx);
    }
  }, [isOpen, activeSessionId, startSession, location.pathname]);

  useEffect(() => {
    prevPathRef.current = location.pathname;
  }, [location.pathname]);

  // Don't render floating widget on dedicated chat page
  if (location.pathname.startsWith('/chat')) return null;

  return (
    <>
      {/* Full-viewport drag constraint */}
      <div ref={constraintRef} className="fixed inset-0 z-40 pointer-events-none" />

      {/* Single draggable wrapper — panel + button move together */}
      <motion.div
        drag
        dragMomentum={false}
        dragElastic={0}
        dragConstraints={constraintRef}
        className="fixed bottom-6 right-6 z-50 flex flex-col items-end gap-2 select-none"
        style={{ touchAction: 'none' }}
      >
        {/* ── Chat panel ───────────────────────────────────────────────────── */}
        <AnimatePresence>
          {isOpen && (
            <motion.div
              key="chat-panel"
              initial={{ opacity: 0, y: 16, scale: 0.97 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: 16, scale: 0.97 }}
              transition={{ type: 'spring', stiffness: 380, damping: 30 }}
              className="flex flex-col bg-white rounded-2xl shadow-2xl border border-gray-200 overflow-hidden
                         w-[92vw] sm:w-[380px]
                         h-[70vh] sm:h-[560px] max-h-[600px]"
            >
              {/* Drag handle header */}
              <div className="flex items-center gap-2 px-3 py-2 border-b border-gray-100 cursor-grab active:cursor-grabbing bg-gray-50">
                <GripHorizontal className="w-4 h-4 text-gray-400 flex-shrink-0" />
                <span className="text-xs font-semibold text-gray-500 flex-1">Nexus Chat</span>
                <button
                  onClick={e => { e.stopPropagation(); closeChat(); }}
                  onPointerDown={e => e.stopPropagation()}
                  className="p-1 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
                  aria-label="Close Nexus Chat"
                >
                  <X size={14} />
                </button>
              </div>
              <div className="flex-1 overflow-hidden">
                <ChatPanel pageContext={location.pathname} />
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* ── Trigger button ────────────────────────────────────────────────── */}
        <motion.button
          className="w-14 h-14 rounded-full text-white shadow-xl flex items-center justify-center cursor-grab active:cursor-grabbing"
          style={{
            background: 'linear-gradient(135deg, #00f5ff 0%, #bf5af2 50%, #ff375f 100%)',
            boxShadow: '0 0 16px rgba(0,245,255,0.5), 0 0 32px rgba(191,90,242,0.35)',
          }}
          whileHover={{ scale: 1.08 }}
          whileTap={{ scale: 0.95 }}
          onClick={isOpen ? closeChat : openChat}
          aria-label={isOpen ? 'Close Nexus Chat' : 'Open Nexus Chat'}
        >
          <AnimatePresence mode="wait">
            {isOpen ? (
              <motion.span key="close" initial={{ rotate: -90, opacity: 0 }} animate={{ rotate: 0, opacity: 1 }} exit={{ rotate: 90, opacity: 0 }}>
                <X size={22} />
              </motion.span>
            ) : (
              <motion.span key="open" initial={{ scale: 0 }} animate={{ scale: 1 }} exit={{ scale: 0 }} className="relative">
                <MessageCircle size={22} />
                {unreadNudgeCount > 0 && (
                  <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full w-4 h-4 flex items-center justify-center font-bold">
                    {unreadNudgeCount > 9 ? '9+' : unreadNudgeCount}
                  </span>
                )}
              </motion.span>
            )}
          </AnimatePresence>
        </motion.button>
      </motion.div>
    </>
  );
};

export default NexusChatWidget;
