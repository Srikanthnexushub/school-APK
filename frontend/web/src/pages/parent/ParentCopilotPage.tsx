import { useState, useRef, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Bot, User, Send, Plus, Trash2, ChevronRight } from 'lucide-react';
import { cn } from '../../lib/utils';
import api from '../../lib/api';
import { toast } from 'sonner';

// ─── Types ────────────────────────────────────────────────────────────────────

type Role = 'user' | 'assistant';

interface Message {
  id: string;
  role: Role;
  content: string;
  displayedContent: string; // for typewriter animation
  complete: boolean;
  timestamp: Date;
}

interface Conversation {
  id: string;
  preview: string;
  timestamp: Date;
  messages: Message[];
}

// ─── Mock AI responses ────────────────────────────────────────────────────────

const mockResponses: Record<string, string> = {
  default:
    "I understand your concern about your child's academic progress. Based on recent data, Priya has been consistently improving her ERS score over the past two weeks, moving from 64 to 72. She shows particular strength in Physics and Mathematics, but needs more focused practice on Organic Chemistry and Electrochemistry. I recommend scheduling an extra mentoring session this week.",
  performance:
    "Priya's current overall performance is tracking well. Her ERS (Exam Readiness Score) stands at 72/100, placing her in the 'On Track' category for JEE 2026. Subject-wise breakdown: Physics 78%, Mathematics 74%, Chemistry 62%. The Chemistry score needs attention — particularly Organic Chemistry (Chapter 12-15). Her mock test scores have improved by 8% over the last month.",
  weak:
    "Priya's primary weak areas identified by our AI analysis are: 1) Electrochemistry — struggles with Nernst equation applications, 2) Organic Chemistry — specifically named reactions and mechanisms, 3) Integration techniques in Mathematics. I recommend targeting 45 minutes daily on these topics using our curated practice sets. Would you like me to create a focused study plan?",
  exam:
    "Upcoming exam schedule for Priya: JEE Main Mock Test #6 — March 15, 2026. Chemistry Unit Test — March 18, 2026. Physics Revision Test — March 22, 2026. The JEE Main registration deadline is April 1, 2026. Would you like a reminder set for these dates?",
  fees:
    "Current fee status for Priya Sharma: Monthly batch fee of ₹4,500 is due by March 15, 2026. Study material fee of ₹2,000 was paid on February 1, 2026. Total outstanding: ₹4,500. Payment can be made via UPI, net banking, or card through the parent portal. Shall I generate a payment link?",
};

function getMockResponse(message: string): string {
  const lower = message.toLowerCase();
  if (lower.includes('performance') || lower.includes('doing') || lower.includes('score')) {
    return mockResponses.performance;
  }
  if (lower.includes('weak') || lower.includes('struggle') || lower.includes('difficult')) {
    return mockResponses.weak;
  }
  if (lower.includes('exam') || lower.includes('test') || lower.includes('schedule')) {
    return mockResponses.exam;
  }
  if (lower.includes('fee') || lower.includes('payment') || lower.includes('due')) {
    return mockResponses.fees;
  }
  return mockResponses.default;
}

// ─── Quick chips ──────────────────────────────────────────────────────────────

const quickChips = [
  'How is my child doing?',
  'What are weak areas?',
  'When is the next exam?',
  'Outstanding fees?',
];

// ─── Typing indicator ─────────────────────────────────────────────────────────

function TypingIndicator() {
  return (
    <div className="flex items-end gap-2 justify-start">
      <div className="w-7 h-7 rounded-full bg-brand-600/20 border border-brand-500/30 flex items-center justify-center flex-shrink-0">
        <Bot className="w-3.5 h-3.5 text-brand-400" />
      </div>
      <div className="bg-surface-100 rounded-2xl rounded-bl-sm px-4 py-3 flex items-center gap-1">
        {[0, 1, 2].map((i) => (
          <motion.div
            key={i}
            animate={{ opacity: [0.3, 1, 0.3], y: [0, -4, 0] }}
            transition={{ duration: 0.8, delay: i * 0.15, repeat: Infinity }}
            className="w-1.5 h-1.5 bg-white/40 rounded-full"
          />
        ))}
      </div>
    </div>
  );
}

// ─── Message bubble ───────────────────────────────────────────────────────────

function MessageBubble({ message }: { message: Message }) {
  const isUser = message.role === 'user';
  const timeStr = message.timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

  if (isUser) {
    return (
      <motion.div
        initial={{ opacity: 0, x: 20 }}
        animate={{ opacity: 1, x: 0 }}
        transition={{ duration: 0.2 }}
        className="flex items-end gap-2 justify-end"
      >
        <div className="max-w-[75%]">
          <div className="bg-brand-500/20 border border-brand-500/20 rounded-2xl rounded-br-sm px-4 py-3">
            <p className="text-sm text-white leading-relaxed whitespace-pre-wrap">{message.content}</p>
          </div>
          <p className="text-[10px] text-white/25 mt-1 text-right">{timeStr}</p>
        </div>
        <div className="w-7 h-7 rounded-full bg-brand-600/30 flex items-center justify-center flex-shrink-0 mb-4">
          <User className="w-3.5 h-3.5 text-brand-300" />
        </div>
      </motion.div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, x: -20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.2 }}
      className="flex items-end gap-2 justify-start"
    >
      <div className="w-7 h-7 rounded-full bg-brand-600/20 border border-brand-500/30 flex items-center justify-center flex-shrink-0 mb-4">
        <Bot className="w-3.5 h-3.5 text-brand-400" />
      </div>
      <div className="max-w-[75%]">
        <div className="bg-surface-100 border border-white/5 rounded-2xl rounded-bl-sm px-4 py-3">
          <p className="text-sm text-white/90 leading-relaxed whitespace-pre-wrap">
            {message.displayedContent}
            {!message.complete && (
              <span className="inline-block w-0.5 h-3.5 bg-brand-400 ml-0.5 animate-pulse align-middle" />
            )}
          </p>
        </div>
        <p className="text-[10px] text-white/25 mt-1">{timeStr}</p>
      </div>
    </motion.div>
  );
}

// ─── Conversation list item ───────────────────────────────────────────────────

function ConvItem({
  conv, active, onClick, onDelete,
}: {
  conv: Conversation;
  active: boolean;
  onClick: () => void;
  onDelete: () => void;
}) {
  const timeStr = conv.timestamp.toLocaleDateString([], { month: 'short', day: 'numeric' });
  return (
    <div
      onClick={onClick}
      className={cn(
        'group flex items-start gap-2 p-3 rounded-xl cursor-pointer transition-colors relative',
        active ? 'bg-brand-500/10 border border-brand-500/20' : 'hover:bg-white/5'
      )}
    >
      <div className="min-w-0 flex-1">
        <p className="text-xs font-medium text-white/80 truncate">{conv.preview}</p>
        <p className="text-[10px] text-white/30 mt-0.5">{timeStr}</p>
      </div>
      <button
        onClick={(e) => { e.stopPropagation(); onDelete(); }}
        className="opacity-0 group-hover:opacity-100 p-1 rounded-lg hover:bg-red-500/20 text-white/20 hover:text-red-400 transition-all flex-shrink-0"
      >
        <Trash2 className="w-3 h-3" />
      </button>
    </div>
  );
}

// ─── Greeting message ─────────────────────────────────────────────────────────

const greetingMessage = (id: string): Message => ({
  id,
  role: 'assistant',
  content: "Hello! I'm your Parent Copilot — powered by EduPath AI.\n\nI can help you with:\n• Your child's performance and progress\n• Fee and attendance queries\n• Study recommendations and weak areas\n• Exam schedules and upcoming tests\n• Booking mentor sessions\n\nHow can I help you today?",
  displayedContent: "Hello! I'm your Parent Copilot — powered by EduPath AI.\n\nI can help you with:\n• Your child's performance and progress\n• Fee and attendance queries\n• Study recommendations and weak areas\n• Exam schedules and upcoming tests\n• Booking mentor sessions\n\nHow can I help you today?",
  complete: true,
  timestamp: new Date(),
});

function newConv(): Conversation {
  const id = crypto.randomUUID();
  return {
    id,
    preview: 'New conversation',
    timestamp: new Date(),
    messages: [greetingMessage(crypto.randomUUID())],
  };
}

// ─── Main page ────────────────────────────────────────────────────────────────

export default function ParentCopilotPage() {
  const [conversations, setConversations] = useState<Conversation[]>(() => [newConv()]);
  const [activeConvId, setActiveConvId] = useState<string>(() => conversations[0].id);
  const [input, setInput] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const bottomRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  const activeConv = conversations.find((c) => c.id === activeConvId)!;

  // Auto-scroll to bottom
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [activeConv?.messages, isTyping]);

  // Typewriter effect
  const runTypewriter = useCallback((convId: string, msgId: string, fullText: string) => {
    let idx = 0;
    const interval = setInterval(() => {
      idx += Math.floor(Math.random() * 3) + 1; // 1-3 chars per tick for natural feel
      const displayed = fullText.slice(0, idx);
      const complete = idx >= fullText.length;

      setConversations((prev) =>
        prev.map((c) =>
          c.id !== convId
            ? c
            : {
                ...c,
                messages: c.messages.map((m) =>
                  m.id !== msgId
                    ? m
                    : { ...m, displayedContent: displayed, complete }
                ),
              }
        )
      );

      if (complete) clearInterval(interval);
    }, 18);
  }, []);

  async function sendMessage(text: string) {
    const trimmed = text.trim();
    if (!trimmed || isTyping) return;

    const userMsg: Message = {
      id: crypto.randomUUID(),
      role: 'user',
      content: trimmed,
      displayedContent: trimmed,
      complete: true,
      timestamp: new Date(),
    };

    // Add user message + update preview
    setConversations((prev) =>
      prev.map((c) =>
        c.id !== activeConvId
          ? c
          : {
              ...c,
              preview: trimmed.slice(0, 50) + (trimmed.length > 50 ? '…' : ''),
              messages: [...c.messages, userMsg],
            }
      )
    );
    setInput('');
    setIsTyping(true);

    // Simulate API call
    try {
      // In real app: POST /api/v1/copilot/conversations/{id}/messages
      await new Promise((r) => setTimeout(r, 900 + Math.random() * 600));
      const responseText = getMockResponse(trimmed);

      const assistantMsg: Message = {
        id: crypto.randomUUID(),
        role: 'assistant',
        content: responseText,
        displayedContent: '',
        complete: false,
        timestamp: new Date(),
      };

      setIsTyping(false);
      const msgId = assistantMsg.id;
      const convId = activeConvId;

      setConversations((prev) =>
        prev.map((c) =>
          c.id !== convId
            ? c
            : { ...c, messages: [...c.messages, assistantMsg] }
        )
      );

      runTypewriter(convId, msgId, responseText);
    } catch {
      setIsTyping(false);
      toast.error('Failed to get response. Please try again.');
    }
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage(input);
    }
  }

  function startNewConv() {
    const c = newConv();
    setConversations((prev) => [c, ...prev]);
    setActiveConvId(c.id);
  }

  function deleteConv(id: string) {
    if (conversations.length === 1) {
      // Replace with fresh conversation
      const fresh = newConv();
      setConversations([fresh]);
      setActiveConvId(fresh.id);
      return;
    }
    const remaining = conversations.filter((c) => c.id !== id);
    setConversations(remaining);
    if (activeConvId === id) setActiveConvId(remaining[0].id);
  }

  // Group conversations by recency
  const today = new Date();
  const todayConvs = conversations.filter(
    (c) => c.timestamp.toDateString() === today.toDateString()
  );
  const yesterdayConvs = conversations.filter((c) => {
    const d = new Date(today);
    d.setDate(d.getDate() - 1);
    return c.timestamp.toDateString() === d.toDateString();
  });
  const olderConvs = conversations.filter(
    (c) =>
      c.timestamp.toDateString() !== today.toDateString() &&
      yesterdayConvs.indexOf(c) === -1
  );

  return (
    <div className="flex h-[calc(100vh-56px)] bg-surface overflow-hidden">
      {/* Left sidebar — conversation history */}
      <AnimatePresence initial={false}>
        {sidebarOpen && (
          <motion.div
            initial={{ width: 0, opacity: 0 }}
            animate={{ width: 260, opacity: 1 }}
            exit={{ width: 0, opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="flex-shrink-0 border-r border-white/5 bg-surface-50/40 flex flex-col overflow-hidden"
          >
            <div className="p-3 border-b border-white/5 flex items-center justify-between">
              <span className="text-sm font-semibold text-white">History</span>
              <button
                onClick={startNewConv}
                className="flex items-center gap-1 text-xs text-brand-400 hover:text-brand-300 font-medium transition-colors px-2 py-1 rounded-lg hover:bg-brand-500/10"
              >
                <Plus className="w-3 h-3" /> New
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-2 space-y-4">
              {todayConvs.length > 0 && (
                <div>
                  <div className="px-2 py-1 text-[10px] font-semibold text-white/25 uppercase tracking-wider">Today</div>
                  {todayConvs.map((c) => (
                    <ConvItem key={c.id} conv={c} active={activeConvId === c.id}
                      onClick={() => setActiveConvId(c.id)}
                      onDelete={() => deleteConv(c.id)}
                    />
                  ))}
                </div>
              )}
              {yesterdayConvs.length > 0 && (
                <div>
                  <div className="px-2 py-1 text-[10px] font-semibold text-white/25 uppercase tracking-wider">Yesterday</div>
                  {yesterdayConvs.map((c) => (
                    <ConvItem key={c.id} conv={c} active={activeConvId === c.id}
                      onClick={() => setActiveConvId(c.id)}
                      onDelete={() => deleteConv(c.id)}
                    />
                  ))}
                </div>
              )}
              {olderConvs.length > 0 && (
                <div>
                  <div className="px-2 py-1 text-[10px] font-semibold text-white/25 uppercase tracking-wider">Earlier</div>
                  {olderConvs.map((c) => (
                    <ConvItem key={c.id} conv={c} active={activeConvId === c.id}
                      onClick={() => setActiveConvId(c.id)}
                      onDelete={() => deleteConv(c.id)}
                    />
                  ))}
                </div>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Right — chat interface */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Chat header */}
        <div className="flex items-center justify-between px-4 py-3 border-b border-white/5 flex-shrink-0">
          <div className="flex items-center gap-3">
            <button
              onClick={() => setSidebarOpen(!sidebarOpen)}
              className="p-1.5 rounded-lg hover:bg-white/5 text-white/30 hover:text-white/70 transition-colors"
            >
              <ChevronRight className={cn('w-4 h-4 transition-transform', sidebarOpen ? 'rotate-180' : '')} />
            </button>
            <div className="flex items-center gap-2">
              <div className="p-1.5 rounded-lg bg-brand-600/20 border border-brand-500/30">
                <Bot className="w-4 h-4 text-brand-400" />
              </div>
              <div>
                <div className="text-sm font-semibold text-white">Parent Copilot</div>
                <div className="text-[10px] text-emerald-400 flex items-center gap-1">
                  <span className="w-1.5 h-1.5 bg-emerald-400 rounded-full" />
                  AI powered · always available
                </div>
              </div>
            </div>
          </div>

          <button
            onClick={startNewConv}
            className="flex items-center gap-1.5 text-xs font-medium text-white/50 hover:text-white px-3 py-1.5 rounded-lg border border-white/5 hover:border-white/10 hover:bg-white/5 transition-all"
          >
            <Plus className="w-3.5 h-3.5" /> New Conversation
          </button>
        </div>

        {/* Messages area */}
        <div className="flex-1 overflow-y-auto px-4 py-6 space-y-4">
          {activeConv?.messages.map((msg) => (
            <MessageBubble key={msg.id} message={msg} />
          ))}
          {isTyping && <TypingIndicator />}
          <div ref={bottomRef} />
        </div>

        {/* Quick chips */}
        <div className="px-4 pb-2 flex gap-2 overflow-x-auto flex-shrink-0">
          {quickChips.map((chip) => (
            <button
              key={chip}
              onClick={() => sendMessage(chip)}
              disabled={isTyping}
              className="flex-shrink-0 text-xs font-medium text-white/50 hover:text-white px-3 py-1.5 rounded-full border border-white/8 hover:border-brand-500/40 hover:bg-brand-500/10 transition-all disabled:opacity-40 disabled:cursor-not-allowed"
            >
              {chip}
            </button>
          ))}
        </div>

        {/* Input area */}
        <div className="px-4 pb-4 flex-shrink-0">
          <div className="flex items-end gap-3 bg-surface-100 border border-white/8 rounded-2xl px-4 py-3 focus-within:border-brand-500/40 transition-colors">
            <textarea
              ref={inputRef}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask about your child…"
              rows={1}
              disabled={isTyping}
              className="flex-1 bg-transparent text-sm text-white placeholder:text-white/25 focus:outline-none resize-none max-h-32 leading-relaxed disabled:opacity-50"
              style={{ minHeight: '24px' }}
              onInput={(e) => {
                const t = e.currentTarget;
                t.style.height = 'auto';
                t.style.height = Math.min(t.scrollHeight, 128) + 'px';
              }}
            />
            <button
              onClick={() => sendMessage(input)}
              disabled={!input.trim() || isTyping}
              className={cn(
                'p-2 rounded-xl transition-all flex-shrink-0',
                input.trim() && !isTyping
                  ? 'bg-brand-600 hover:bg-brand-500 text-white'
                  : 'bg-white/5 text-white/20 cursor-not-allowed'
              )}
              aria-label="Send"
            >
              <Send className="w-4 h-4" />
            </button>
          </div>
          <p className="text-[10px] text-white/20 text-center mt-2">
            AI responses are informational only. Always verify with your child's teacher.
          </p>
        </div>
      </div>
    </div>
  );
}
