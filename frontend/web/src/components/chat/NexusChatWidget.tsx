import React, { useEffect, useRef } from 'react';
import { MessageCircle, X } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { useChatStore } from '../../store/chatStore';
import ChatPanel from './ChatPanel';
import { useLocation } from 'react-router-dom';

const NexusChatWidget: React.FC = () => {
  const { isOpen, openChat, closeChat, unreadNudgeCount, startSession, activeSessionId } = useChatStore();
  const location = useLocation();
  const prevPathRef = useRef<string>(location.pathname);

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
      {/* Floating button */}
      <motion.button
        className="fixed bottom-6 right-6 z-50 w-14 h-14 rounded-full bg-indigo-600 text-white shadow-xl
                   flex items-center justify-center hover:bg-indigo-700 transition-colors"
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

      {/* Chat panel */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            key="chat-panel"
            initial={{ opacity: 0, y: 24, scale: 0.97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 24, scale: 0.97 }}
            transition={{ type: 'spring', stiffness: 380, damping: 30 }}
            className="fixed bottom-24 right-6 z-50 w-[380px] h-[580px] flex flex-col
                       bg-white rounded-2xl shadow-2xl border border-gray-200 overflow-hidden"
          >
            <ChatPanel pageContext={location.pathname} />
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
};

export default NexusChatWidget;
