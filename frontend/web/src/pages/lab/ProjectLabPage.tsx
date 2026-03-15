import { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Beaker, Send, Bot, User, Loader2, Plus, Sparkles } from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { cn } from '../../lib/utils';

interface Message {
  role: 'user' | 'assistant';
  content: string;
}

const PROJECT_STARTERS = [
  { icon: '🌦️', label: 'Weather App', prompt: 'Help me build a weather forecasting app that uses an API to show current and 7-day forecast.' },
  { icon: '🎮', label: 'Game with AI', prompt: 'I want to create a simple quiz game where AI generates questions based on my chosen topic.' },
  { icon: '📊', label: 'Data Dashboard', prompt: 'I want to build a data visualisation dashboard. Help me plan the architecture and which charts to use.' },
  { icon: '🤖', label: 'Chatbot', prompt: 'Help me design a chatbot for a specific domain — maybe a school FAQ bot or a recipe helper.' },
  { icon: '📱', label: 'Mobile App Idea', prompt: 'I have an idea for a mobile app to solve a real student problem. Help me refine the idea and plan it.' },
  { icon: '🔬', label: 'Science Experiment', prompt: 'I want to design a science experiment and document my hypothesis, procedure, and expected results.' },
];

const SYSTEM_PROMPT = `You are an expert project mentor for school and college students. You help students design, plan, and build experimental projects — both technology projects (apps, websites, AI tools) and science/research projects.

When a student describes their project idea:
1. Ask clarifying questions to understand their skill level, resources, and goal
2. Suggest a clear project structure with phases (ideation → planning → building → testing → presenting)
3. Recommend specific tools, frameworks, or methods appropriate for their level
4. Break down the first step into small actionable tasks
5. Be encouraging and practical — focus on what they CAN do, not limitations

Always ask about: their experience level, available time, tools they have access to, and what problem they want to solve.

Keep responses concise and structured with bullet points or numbered steps when listing actions.`;

export default function ProjectLabPage() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [showStarters, setShowStarters] = useState(true);
  const bottomRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  async function sendMessage(text: string) {
    if (!text.trim() || loading) return;

    const userMsg: Message = { role: 'user', content: text.trim() };
    const newMessages = [...messages, userMsg];
    setMessages(newMessages);
    setInput('');
    setShowStarters(false);
    setLoading(true);

    try {
      // Build conversation history for context
      const history = messages.map(m => `${m.role === 'user' ? 'Student' : 'Mentor'}: ${m.content}`).join('\n');
      const systemWithHistory = history
        ? `${SYSTEM_PROMPT}\n\nConversation so far:\n${history}`
        : SYSTEM_PROMPT;

      const res = await api.post('/api/v1/ai/completions', {
        requesterId: 'student-project-lab',
        systemPrompt: systemWithHistory,
        userMessage: text.trim(),
        maxTokens: 600,
        temperature: 0.7,
      });

      const reply = res.data?.content ?? 'I couldn\'t generate a response. Please try again.';
      setMessages(prev => [...prev, { role: 'assistant', content: reply }]);
    } catch {
      // Fallback: try doubts API which is accessible via student-gateway
      try {
        const res = await api.post('/api/v1/doubts', {
          question: text.trim(),
          context: 'experimental-project-lab',
          subject: 'Project Lab',
        });
        const reply = res.data?.answer ?? res.data?.response ?? 'Unable to get a response. Please try again.';
        setMessages(prev => [...prev, { role: 'assistant', content: reply }]);
      } catch {
        toast.error('Unable to connect to AI. Please check your connection.');
        setMessages(prev => prev.slice(0, -1)); // remove the user message
      }
    } finally {
      setLoading(false);
    }
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage(input);
    }
  }

  function clearChat() {
    setMessages([]);
    setShowStarters(true);
    setInput('');
  }

  return (
    <div className="flex flex-col h-[calc(100vh-5rem)] max-w-4xl mx-auto p-4 lg:p-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-4 flex-shrink-0">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-violet-500/15 border border-violet-500/30">
            <Beaker className="w-5 h-5 text-violet-400" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-white">AI Project Lab</h1>
            <p className="text-white/40 text-xs">Build, experiment, create — with AI guidance</p>
          </div>
        </div>
        {messages.length > 0 && (
          <button
            onClick={clearChat}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-white/10 text-xs text-white/50 hover:text-white hover:border-white/20 transition-colors"
          >
            <Plus className="w-3.5 h-3.5" /> New Project
          </button>
        )}
      </div>

      {/* Chat area */}
      <div className="flex-1 overflow-y-auto space-y-4 mb-4 min-h-0">
        {messages.length === 0 && showStarters && (
          <div className="space-y-5">
            <div className="text-center py-4">
              <div className="inline-flex p-4 rounded-2xl bg-violet-500/10 border border-violet-500/20 mb-4">
                <Sparkles className="w-8 h-8 text-violet-400" />
              </div>
              <h2 className="text-lg font-bold text-white mb-1">What will you build today?</h2>
              <p className="text-white/40 text-sm max-w-md mx-auto">
                Describe your project idea and your AI mentor will help you plan, design, and build it step by step.
              </p>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              {PROJECT_STARTERS.map((s) => (
                <button
                  key={s.label}
                  onClick={() => sendMessage(s.prompt)}
                  className="p-3 rounded-xl bg-surface-100 border border-white/8 hover:border-violet-500/40 hover:bg-violet-500/5 text-left transition-all group"
                >
                  <span className="text-xl mb-2 block">{s.icon}</span>
                  <span className="text-sm font-medium text-white/80 group-hover:text-white">{s.label}</span>
                </button>
              ))}
            </div>
          </div>
        )}

        <AnimatePresence initial={false}>
          {messages.map((msg, i) => (
            <motion.div
              key={i}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              className={cn('flex gap-3', msg.role === 'user' ? 'flex-row-reverse' : 'flex-row')}
            >
              <div className={cn(
                'w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0',
                msg.role === 'user' ? 'bg-brand-500/20' : 'bg-violet-500/20'
              )}>
                {msg.role === 'user'
                  ? <User className="w-4 h-4 text-brand-400" />
                  : <Bot className="w-4 h-4 text-violet-400" />}
              </div>
              <div className={cn(
                'max-w-[80%] rounded-2xl px-4 py-3 text-sm leading-relaxed whitespace-pre-wrap',
                msg.role === 'user'
                  ? 'bg-brand-600/20 text-white rounded-tr-md'
                  : 'bg-surface-100 text-white/90 border border-white/5 rounded-tl-md'
              )}>
                {msg.content}
              </div>
            </motion.div>
          ))}
        </AnimatePresence>

        {loading && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex gap-3">
            <div className="w-8 h-8 rounded-full bg-violet-500/20 flex items-center justify-center flex-shrink-0">
              <Bot className="w-4 h-4 text-violet-400" />
            </div>
            <div className="bg-surface-100 border border-white/5 rounded-2xl rounded-tl-md px-4 py-3">
              <div className="flex gap-1">
                {[0, 1, 2].map(i => (
                  <div key={i} className="w-2 h-2 rounded-full bg-violet-400/60 animate-bounce"
                    style={{ animationDelay: `${i * 0.15}s` }} />
                ))}
              </div>
            </div>
          </motion.div>
        )}

        <div ref={bottomRef} />
      </div>

      {/* Input area */}
      <div className="flex-shrink-0">
        <div className="flex gap-3 items-end bg-surface-100 border border-white/10 rounded-2xl p-3 focus-within:border-violet-500/40 transition-colors">
          <textarea
            ref={inputRef}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Describe your project idea, ask for help, or pick a starter above…"
            rows={1}
            className="flex-1 bg-transparent text-sm text-white placeholder-white/25 resize-none focus:outline-none leading-relaxed max-h-32 overflow-y-auto"
            style={{ minHeight: '24px' }}
            onInput={(e) => {
              const target = e.target as HTMLTextAreaElement;
              target.style.height = 'auto';
              target.style.height = Math.min(target.scrollHeight, 128) + 'px';
            }}
          />
          <button
            onClick={() => sendMessage(input)}
            disabled={!input.trim() || loading}
            className="w-9 h-9 rounded-xl bg-violet-600 hover:bg-violet-500 disabled:opacity-30 disabled:cursor-not-allowed flex items-center justify-center transition-colors flex-shrink-0"
          >
            {loading ? <Loader2 className="w-4 h-4 text-white animate-spin" /> : <Send className="w-4 h-4 text-white" />}
          </button>
        </div>
        <p className="text-center text-xs text-white/20 mt-2">
          Press Enter to send · Shift+Enter for new line
        </p>
      </div>
    </div>
  );
}
