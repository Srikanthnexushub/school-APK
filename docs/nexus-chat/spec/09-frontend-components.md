# NexusChat — Frontend Components (Full TypeScript Code)

## File Structure

```
frontend/web/src/
  components/
    chat/
      NexusChatWidget.tsx      # Floating button + panel container
      ChatPanel.tsx            # Full chat panel (header, messages, input)
      ChatMessageBubble.tsx    # Single message bubble (user/assistant)
      StreamingMessage.tsx     # Live streaming token display with cursor
      ActionResultCard.tsx     # Rendered action result (weak areas, nav, etc.)
      ContextChips.tsx         # Page-context chips shown in input area
      TypingIndicator.tsx      # Three-dot pulse while waiting for first token
  pages/
    chat/
      ChatPage.tsx             # Full-page chat (for nudge deep-links)
```

---

## 1. NexusChatWidget.tsx

```tsx
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

  // Auto-start a session when widget opens (if none active)
  useEffect(() => {
    if (isOpen && !activeSessionId) {
      const pageContext = location.pathname.replace('/dashboard/', '').replace('/', '') || 'dashboard';
      startSession(pageContext);
    }
  }, [isOpen, activeSessionId, startSession, location.pathname]);

  // Refresh page context on navigation (don't start new session — just track context)
  useEffect(() => {
    if (prevPathRef.current !== location.pathname) {
      prevPathRef.current = location.pathname;
    }
  }, [location.pathname]);

  // Don't render in full-screen chat page (avoids duplicate)
  if (location.pathname === '/chat') return null;

  return (
    <>
      {/* Floating Chat Button */}
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

      {/* Chat Panel */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            key="chat-panel"
            initial={{ opacity: 0, y: 24, scale: 0.97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 24, scale: 0.97 }}
            transition={{ type: 'spring', stiffness: 380, damping: 30 }}
            className="fixed bottom-24 right-6 z-50 w-[380px] max-h-[600px] flex flex-col
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
```

---

## 2. ChatPanel.tsx

```tsx
import React, { useEffect, useRef, useCallback } from 'react';
import { Send, RotateCcw, Loader2 } from 'lucide-react';
import { useChatStore } from '../../store/chatStore';
import ChatMessageBubble from './ChatMessageBubble';
import StreamingMessage from './StreamingMessage';
import TypingIndicator from './TypingIndicator';
import ContextChips from './ContextChips';

interface ChatPanelProps {
  pageContext: string;
}

const ChatPanel: React.FC<ChatPanelProps> = ({ pageContext }) => {
  const {
    messages, streamingContent, isStreaming, isLoading,
    inputText, setInputText, sendMessage, activeSessionId,
    sessions, loadSessions,
  } = useChatStore();

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  // Auto-scroll to bottom on new message/token
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, streamingContent]);

  // Load sessions on mount
  useEffect(() => {
    loadSessions();
  }, [loadSessions]);

  const handleSend = useCallback(() => {
    const text = inputText.trim();
    if (!text || isStreaming || isLoading) return;
    sendMessage(text, pageContext);
    setInputText('');
  }, [inputText, isStreaming, isLoading, sendMessage, setInputText, pageContext]);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const activeSession = sessions.find(s => s.sessionId === activeSessionId);

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 bg-indigo-600 text-white rounded-t-2xl flex-shrink-0">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-full bg-indigo-400 flex items-center justify-center text-sm font-bold">N</div>
          <div>
            <p className="font-semibold text-sm leading-tight">Nexus AI</p>
            <p className="text-xs text-indigo-200 leading-tight">
              {isStreaming ? 'typing...' : activeSession?.title ?? 'Your study partner'}
            </p>
          </div>
        </div>
        <button
          onClick={() => useChatStore.getState().startSession(pageContext.replace('/dashboard/', '').replace('/', '') || 'dashboard')}
          title="New conversation"
          className="text-indigo-200 hover:text-white transition-colors"
        >
          <RotateCcw size={16} />
        </button>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-3 py-3 space-y-2 min-h-0">
        {isLoading && messages.length === 0 && (
          <div className="flex justify-center items-center h-20">
            <Loader2 size={20} className="animate-spin text-indigo-400" />
          </div>
        )}

        {messages.map((msg) => (
          <ChatMessageBubble key={msg.messageId} message={msg} />
        ))}

        {isStreaming && streamingContent === '' && <TypingIndicator />}
        {isStreaming && streamingContent !== '' && <StreamingMessage content={streamingContent} />}

        <div ref={messagesEndRef} />
      </div>

      {/* Context chips */}
      <ContextChips pageContext={pageContext} />

      {/* Input */}
      <div className="px-3 pb-3 pt-1 flex-shrink-0">
        <div className="flex items-end gap-2 bg-gray-50 rounded-xl border border-gray-200 px-3 py-2">
          <textarea
            ref={inputRef}
            rows={1}
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ask Nexus anything..."
            disabled={isStreaming || isLoading}
            className="flex-1 bg-transparent resize-none outline-none text-sm text-gray-800
                       placeholder-gray-400 max-h-24 min-h-[20px] overflow-y-auto disabled:opacity-50"
            style={{ height: 'auto' }}
            onInput={(e) => {
              const t = e.currentTarget;
              t.style.height = 'auto';
              t.style.height = Math.min(t.scrollHeight, 96) + 'px';
            }}
          />
          <button
            onClick={handleSend}
            disabled={!inputText.trim() || isStreaming || isLoading}
            className="w-8 h-8 rounded-lg bg-indigo-600 text-white flex items-center justify-center
                       hover:bg-indigo-700 disabled:opacity-40 disabled:cursor-not-allowed transition-colors flex-shrink-0"
          >
            {isStreaming ? <Loader2 size={14} className="animate-spin" /> : <Send size={14} />}
          </button>
        </div>
        <p className="text-center text-[10px] text-gray-300 mt-1">Nexus AI · Powered by NexusEd</p>
      </div>
    </div>
  );
};

export default ChatPanel;
```

---

## 3. ChatMessageBubble.tsx

```tsx
import React, { useState } from 'react';
import ReactMarkdown from 'react-markdown';
import { ChatMessageDto } from '../../types/chat';
import ActionResultCard from './ActionResultCard';

interface Props {
  message: ChatMessageDto;
}

// Strip action JSON blocks from display content
function stripActionJson(content: string): string {
  return content.replace(/\[\{[^}]*"action"[^}]*\}[^\]]*\]/g, '').trim();
}

function extractActionJson(content: string): Record<string, unknown> | null {
  const match = content.match(/\[\{[^}]*"action"[^}]*\}[^\]]*\]/);
  if (!match) return null;
  try {
    const parsed = JSON.parse(match[0]);
    return Array.isArray(parsed) ? parsed[0] : parsed;
  } catch {
    return null;
  }
}

const ChatMessageBubble: React.FC<Props> = ({ message }) => {
  const isUser = message.role === 'USER';
  const [actionHandled, setActionHandled] = useState(false);

  const displayContent = isUser ? message.content : stripActionJson(message.content);
  const actionCommand = !isUser ? extractActionJson(message.content) : null;

  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div className={`max-w-[85%] ${isUser ? 'order-2' : 'order-1'}`}>
        {!isUser && (
          <div className="flex items-center gap-1 mb-1">
            <div className="w-5 h-5 rounded-full bg-indigo-600 flex items-center justify-center text-white text-[10px] font-bold">N</div>
            <span className="text-[10px] text-gray-400">Nexus</span>
          </div>
        )}

        <div className={`rounded-2xl px-3 py-2 text-sm leading-relaxed
          ${isUser
            ? 'bg-indigo-600 text-white rounded-br-sm'
            : 'bg-gray-100 text-gray-800 rounded-bl-sm'
          }`}
        >
          {isUser ? (
            <p>{message.content}</p>
          ) : (
            <ReactMarkdown
              className="prose prose-sm max-w-none prose-p:my-1 prose-ul:my-1 prose-li:my-0 prose-strong:text-gray-900"
              components={{
                a: ({ ...props }) => <a {...props} target="_blank" rel="noopener noreferrer" className="text-indigo-600 underline" />,
              }}
            >
              {displayContent}
            </ReactMarkdown>
          )}
        </div>

        {/* Action card (rendered below the bubble) */}
        {actionCommand && !actionHandled && (
          <ActionResultCard
            action={actionCommand as { action: string; params: Record<string, unknown> }}
            onHandled={() => setActionHandled(true)}
          />
        )}

        <p className={`text-[10px] text-gray-300 mt-0.5 ${isUser ? 'text-right' : 'text-left'}`}>
          {new Date(message.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
        </p>
      </div>
    </div>
  );
};

export default ChatMessageBubble;
```

---

## 4. StreamingMessage.tsx

```tsx
import React from 'react';
import ReactMarkdown from 'react-markdown';

interface Props {
  content: string;
}

const StreamingMessage: React.FC<Props> = ({ content }) => {
  return (
    <div className="flex justify-start">
      <div className="max-w-[85%]">
        <div className="flex items-center gap-1 mb-1">
          <div className="w-5 h-5 rounded-full bg-indigo-600 flex items-center justify-center text-white text-[10px] font-bold">N</div>
          <span className="text-[10px] text-gray-400">Nexus</span>
        </div>
        <div className="bg-gray-100 text-gray-800 rounded-2xl rounded-bl-sm px-3 py-2 text-sm leading-relaxed">
          <ReactMarkdown className="prose prose-sm max-w-none prose-p:my-1 prose-ul:my-1 prose-li:my-0">
            {content}
          </ReactMarkdown>
          {/* Blinking cursor */}
          <span className="inline-block w-0.5 h-4 bg-indigo-500 ml-0.5 animate-pulse align-middle" />
        </div>
      </div>
    </div>
  );
};

export default StreamingMessage;
```

---

## 5. TypingIndicator.tsx

```tsx
import React from 'react';

const TypingIndicator: React.FC = () => (
  <div className="flex justify-start">
    <div className="max-w-[85%]">
      <div className="flex items-center gap-1 mb-1">
        <div className="w-5 h-5 rounded-full bg-indigo-600 flex items-center justify-center text-white text-[10px] font-bold">N</div>
        <span className="text-[10px] text-gray-400">Nexus</span>
      </div>
      <div className="bg-gray-100 rounded-2xl rounded-bl-sm px-4 py-3 flex items-center gap-1">
        {[0, 1, 2].map((i) => (
          <span
            key={i}
            className="w-2 h-2 rounded-full bg-gray-400"
            style={{ animation: `bounce 1s ease-in-out ${i * 0.15}s infinite` }}
          />
        ))}
      </div>
    </div>
  </div>
);

export default TypingIndicator;
```

---

## 6. ActionResultCard.tsx

```tsx
import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, BookOpen, BarChart2, DollarSign, AlertTriangle } from 'lucide-react';
import { toast } from 'react-toastify';

interface ActionResultCardProps {
  action: { action: string; params: Record<string, unknown> };
  onHandled: () => void;
}

const ACTION_CONFIG: Record<string, { label: string; icon: React.ReactNode; color: string; route?: (p: Record<string, unknown>) => string }> = {
  NAVIGATE: {
    label: 'Take me there',
    icon: <ArrowRight size={14} />,
    color: 'bg-indigo-50 border-indigo-200 text-indigo-700',
    route: (p) => (p['path'] as string) || '/',
  },
  SHOW_WEAK_AREAS: {
    label: 'View Weak Areas',
    icon: <AlertTriangle size={14} />,
    color: 'bg-orange-50 border-orange-200 text-orange-700',
    route: () => '/performance',
  },
  CREATE_STUDY_PLAN: {
    label: 'Create Study Plan',
    icon: <BookOpen size={14} />,
    color: 'bg-green-50 border-green-200 text-green-700',
    route: () => '/study-plan',
  },
  SHOW_FEES: {
    label: 'View Fees',
    icon: <DollarSign size={14} />,
    color: 'bg-blue-50 border-blue-200 text-blue-700',
    route: () => '/fees',
  },
  ENROLL_EXAM: {
    label: 'Enroll in Exam',
    icon: <BarChart2 size={14} />,
    color: 'bg-purple-50 border-purple-200 text-purple-700',
    route: () => '/exams',
  },
};

const ActionResultCard: React.FC<ActionResultCardProps> = ({ action, onHandled }) => {
  const navigate = useNavigate();
  const config = ACTION_CONFIG[action.action];

  if (!config) return null;

  const handleClick = () => {
    if (config.route) {
      navigate(config.route(action.params));
    } else {
      toast.info(`Action: ${action.action}`);
    }
    onHandled();
  };

  return (
    <div className={`mt-1 px-3 py-2 rounded-xl border text-xs font-medium flex items-center justify-between cursor-pointer hover:opacity-80 transition-opacity ${config.color}`}
      onClick={handleClick}
    >
      <span>{config.label}</span>
      <span>{config.icon}</span>
    </div>
  );
};

export default ActionResultCard;
```

---

## 7. ContextChips.tsx

```tsx
import React from 'react';

const PAGE_CHIPS: Record<string, string[]> = {
  '/dashboard': ['My performance', 'Upcoming exams', 'Study plan'],
  '/performance': ['Weak areas', 'Subject mastery', 'Improve score'],
  '/exams': ['Upcoming exams', 'Past results', 'Enroll now'],
  '/study-plan': ['Today\'s tasks', 'Create plan', 'Progress'],
  '/fees': ['Fee dues', 'Payment history'],
  '/doubts': ['Solve a doubt', 'Pending doubts'],
};

interface Props { pageContext: string }

const ContextChips: React.FC<Props> = ({ pageContext }) => {
  const chips = PAGE_CHIPS[pageContext] ?? PAGE_CHIPS['/dashboard'];
  const { setInputText } = require('../../store/chatStore').useChatStore();

  if (!chips?.length) return null;

  return (
    <div className="px-3 pb-1 flex gap-1.5 overflow-x-auto scrollbar-hide flex-shrink-0">
      {chips.map((chip) => (
        <button
          key={chip}
          onClick={() => setInputText(chip)}
          className="text-xs bg-indigo-50 text-indigo-600 border border-indigo-100 rounded-full px-2.5 py-1
                     whitespace-nowrap hover:bg-indigo-100 transition-colors flex-shrink-0"
        >
          {chip}
        </button>
      ))}
    </div>
  );
};

export default ContextChips;
```

---

## 8. Integration into AppLayout.tsx

**Single line to add** at the bottom of `AppLayout.tsx`, just before the closing `</div>` of the root container:

```tsx
// Add import at top:
import NexusChatWidget from '../components/chat/NexusChatWidget';

// Add just before the last closing </div> in the return:
<NexusChatWidget />
```

This ensures the chat widget is available on all authenticated pages.
