import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import {
  MessageSquare, Lightbulb, RefreshCw, Bot,
  Clock, ChevronRight, Search, Send,
} from 'lucide-react';
import { ExportMenu } from '../../components/ui/ExportMenu';
import { toast } from 'sonner';
import api from '../../lib/api';
import { cn } from '../../lib/utils';
import { Badge } from '../../components/ui/Badge';
import { Skeleton } from '../../components/ui/LoadingSkeleton';

// ─── Types ──────────────────────────────────────────────────────────────────

interface DoubtTicket {
  id: string;
  subject: string;
  questionText: string;
  status: 'PENDING' | 'RESOLVED' | 'IN_PROGRESS';
  answer?: string;
  resolvedAt?: string;
  createdAt?: string;
}

interface Recommendation {
  id: string;
  subject?: string;
  title?: string;
  contentPreview?: string;
  type: 'VIDEO' | 'ARTICLE' | 'PRACTICE' | 'MOCK_TEST';
  url?: string;
}

// ─── Response Mappers ────────────────────────────────────────────────────────

function mapDoubt(raw: Record<string, unknown>): DoubtTicket {
  return {
    id: raw.id as string,
    subject: raw.subjectArea as string,
    questionText: raw.question as string,
    status: raw.status === 'ESCALATED' ? 'IN_PROGRESS' : (raw.status as 'PENDING' | 'RESOLVED'),
    answer: (raw.aiAnswer as string) ?? undefined,
    resolvedAt: (raw.resolvedAt as string) ?? undefined,
    createdAt: raw.createdAt as string | undefined,
  };
}

function mapRec(raw: Record<string, unknown>): Recommendation {
  const priority = raw.priorityLevel as string;
  const type: Recommendation['type'] =
    priority === 'HIGH' ? 'PRACTICE' : priority === 'MEDIUM' ? 'ARTICLE' : 'VIDEO';
  return {
    id: raw.id as string,
    subject: raw.subjectArea as string,
    title: raw.topic as string,
    contentPreview: raw.recommendationText as string,
    type,
  };
}

const SUBJECTS = [
  'Mathematics', 'Physics', 'Chemistry', 'Biology',
  'English', 'History', 'Geography', 'Computer Science',
];

const SUBJECT_AREA_MAP: Record<string, string> = {
  'Mathematics': 'MATHEMATICS',
  'Physics': 'PHYSICS',
  'Chemistry': 'CHEMISTRY',
  'Biology': 'BIOLOGY',
  'English': 'ENGLISH',
  'History': 'HISTORY',
  'Geography': 'GEOGRAPHY',
  'Computer Science': 'COMPUTER_SCIENCE',
};

const TYPE_VARIANTS: Record<string, 'info' | 'success' | 'warning' | 'default'> = {
  VIDEO: 'info',
  ARTICLE: 'default',
  PRACTICE: 'success',
  MOCK_TEST: 'warning',
};

// ─── Doubt Resolver Tab ──────────────────────────────────────────────────────

function DoubtResolverTab() {
  const queryClient = useQueryClient();
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [newSubject, setNewSubject] = useState(SUBJECTS[0]);
  const [newQuestion, setNewQuestion] = useState('');
  const [immediateDoubt, setImmediateDoubt] = useState<DoubtTicket | null>(null);

  const doubtsQuery = useQuery<DoubtTicket[]>({
    queryKey: ['doubts'],
    queryFn: () => api.get('/api/v1/doubts').then((r) => { const d = r.data; const arr: Record<string, unknown>[] = Array.isArray(d) ? d : (d.content ?? []); return arr.map(mapDoubt); }),
    staleTime: 2 * 60 * 1000,
  });

  const submitMutation = useMutation({
    mutationFn: (payload: { subjectArea: string; question: string }) =>
      api.post('/api/v1/doubts', payload),
    onSuccess: (response) => {
      const newDoubt = mapDoubt(response.data as Record<string, unknown>);
      setImmediateDoubt(newDoubt);
      queryClient.invalidateQueries({ queryKey: ['doubts'] });
      setNewQuestion('');
      setSelectedId(response.data.id);
      toast.success('Doubt submitted!');
    },
    onError: () => toast.error('Failed to submit doubt'),
  });

  const doubts = doubtsQuery.data ?? [];
  // Use immediate response data if the list hasn't refreshed yet
  const selected = doubts.find((d) => d.id === selectedId)
    ?? (immediateDoubt?.id === selectedId ? immediateDoubt : undefined);

  const csvData = doubts.map(d => ({
    Subject: d.subject,
    Question: d.questionText,
    Status: d.status,
    Answer: d.answer ?? '',
    Date: d.createdAt ? new Date(d.createdAt).toLocaleDateString() : '',
  }));

  function handleSubmit() {
    if (!newQuestion.trim()) {
      toast.error('Enter your question');
      return;
    }
    submitMutation.mutate({ subjectArea: SUBJECT_AREA_MAP[newSubject] ?? 'GENERAL', question: newQuestion.trim() });
  }

  function statusDot(status: string) {
    if (status === 'RESOLVED')
      return <span className="w-2 h-2 rounded-full bg-emerald-400 shrink-0" />;
    if (status === 'IN_PROGRESS')
      return <span className="w-2 h-2 rounded-full bg-brand-400 shrink-0" />;
    return <span className="w-2 h-2 rounded-full bg-amber-400 shrink-0" />;
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-5 gap-4" style={{ minHeight: '520px' }}>
      {/* Left: list */}
      <div className="lg:col-span-2 glass rounded-2xl overflow-hidden flex flex-col">
        <div className="px-4 py-3 border-b border-white/5 flex items-center justify-between">
          <h3 className="text-white font-semibold text-sm">My Doubts</h3>
          <ExportMenu csvData={csvData} csvFilename="doubts" />
        </div>

        <div className="flex-1 overflow-y-auto">
          {doubtsQuery.isLoading ? (
            <div className="p-4 space-y-3">
              {[...Array(4)].map((_, i) => (
                <Skeleton key={i} className="h-16 w-full" />
              ))}
            </div>
          ) : doubts.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-full py-12 text-white/30 gap-2">
              <MessageSquare className="w-10 h-10 opacity-50" />
              <p className="text-sm">No doubts submitted yet</p>
            </div>
          ) : (
            doubts.map((d) => (
              <button
                key={d.id}
                onClick={() => setSelectedId(d.id)}
                className={cn(
                  'w-full text-left px-4 py-3 border-b border-white/5 hover:bg-white/5 transition-colors',
                  selectedId === d.id && 'bg-white/10',
                )}
              >
                <div className="flex items-center gap-2 mb-1">
                  {statusDot(d.status)}
                  <span className="text-xs text-white/50">{d.subject}</span>
                  <span className="ml-auto text-xs text-white/30">
                    {d.createdAt
                      ? new Date(d.createdAt).toLocaleDateString()
                      : ''}
                  </span>
                </div>
                <p className="text-sm text-white/70 line-clamp-2 pl-4">
                  {d.questionText}
                </p>
              </button>
            ))
          )}
        </div>

        {/* Submit area */}
        <div className="border-t border-white/10 p-4 space-y-3">
          <select
            className="input w-full text-sm"
            value={newSubject}
            onChange={(e) => setNewSubject(e.target.value)}
          >
            {SUBJECTS.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
          <div className="flex gap-2">
            <textarea
              className="input flex-1 text-sm resize-none"
              rows={2}
              placeholder="Type your doubt... (Ctrl+Enter to send)"
              value={newQuestion}
              onChange={(e) => setNewQuestion(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && e.ctrlKey) handleSubmit();
              }}
            />
            <button
              onClick={handleSubmit}
              disabled={submitMutation.isPending}
              className="btn-primary px-3 shrink-0 flex items-center justify-center"
            >
              <Send className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      {/* Right: detail */}
      <div className="lg:col-span-3 glass rounded-2xl overflow-hidden flex flex-col">
        {selected ? (
          <>
            <div className="px-5 py-3 border-b border-white/5 flex items-center gap-2 flex-wrap">
              <span className="text-xs px-2 py-0.5 rounded-full bg-brand-500/20 text-brand-400">
                {selected.subject}
              </span>
              <Badge
                variant={
                  selected.status === 'RESOLVED'
                    ? 'success'
                    : selected.status === 'IN_PROGRESS'
                      ? 'info'
                      : 'warning'
                }
              >
                {selected.status}
              </Badge>
              {selected.resolvedAt && (
                <span className="ml-auto text-xs text-white/30 flex items-center gap-1">
                  <Clock className="w-3 h-3" />
                  Resolved {new Date(selected.resolvedAt).toLocaleDateString()}
                </span>
              )}
            </div>

            <div className="flex-1 overflow-y-auto p-5 space-y-4">
              {/* User bubble */}
              <div className="flex justify-end">
                <div className="max-w-[80%] bg-brand-500/15 border border-brand-500/30 rounded-2xl rounded-tr-sm px-4 py-3" style={{ boxShadow: '0 0 12px rgba(0,245,255,0.06)' }}>
                  <p className="text-white/90 text-sm leading-relaxed">
                    {selected.questionText}
                  </p>
                </div>
              </div>

              {/* AI answer */}
              {selected.answer ? (
                <div className="flex gap-3">
                  <div className="w-8 h-8 rounded-full bg-brand-500/20 flex items-center justify-center shrink-0 mt-1">
                    <Bot className="w-4 h-4 text-brand-400" />
                  </div>
                  <div className="max-w-[80%] glass rounded-2xl rounded-tl-sm px-4 py-3 border border-white/10">
                    <p className="text-white/80 text-sm leading-relaxed">
                      {selected.answer}
                    </p>
                  </div>
                </div>
              ) : (
                <div className="flex gap-3">
                  <div className="w-8 h-8 rounded-full bg-brand-500/20 flex items-center justify-center shrink-0">
                    <Bot className="w-4 h-4 text-brand-400" />
                  </div>
                  <div className="glass rounded-2xl rounded-tl-sm px-4 py-3 border border-white/10 flex items-center gap-2">
                    <div className="flex gap-1">
                      {[...Array(3)].map((_, i) => (
                        <motion.span
                          key={i}
                          className="w-1.5 h-1.5 rounded-full bg-brand-400"
                          animate={{ opacity: [0.3, 1, 0.3] }}
                          transition={{
                            duration: 1.2,
                            repeat: Infinity,
                            delay: i * 0.2,
                          }}
                        />
                      ))}
                    </div>
                    <span className="text-white/40 text-xs">
                      Processing your doubt...
                    </span>
                  </div>
                </div>
              )}
            </div>
          </>
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center text-white/30 gap-4">
            <div className="w-20 h-20 rounded-full bg-white/5 flex items-center justify-center">
              <Search className="w-10 h-10 opacity-40" />
            </div>
            <div className="text-center">
              <p className="font-medium">Select a doubt to view details</p>
              <p className="text-sm mt-1">
                Or submit a new question from the left panel
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

// ─── Recommendations Tab ─────────────────────────────────────────────────────

function RecommendationsTab() {
  const recsQuery = useQuery<Recommendation[]>({
    queryKey: ['recommendations'],
    queryFn: () =>
      api.get('/api/v1/recommendations').then((r) => { const d = r.data; const arr: Record<string, unknown>[] = Array.isArray(d) ? d : (d.content ?? []); return arr.map(mapRec); }),
    staleTime: 5 * 60 * 1000,
  });

  const recs = recsQuery.data ?? [];

  function typeIcon(type: string) {
    if (type === 'VIDEO') return '🎬';
    if (type === 'ARTICLE') return '📄';
    if (type === 'PRACTICE') return '✏️';
    if (type === 'MOCK_TEST') return '📝';
    return '📚';
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-white">AI Recommendations</h2>
        <button
          onClick={() => recsQuery.refetch()}
          disabled={recsQuery.isFetching}
          className="flex items-center gap-2 px-3 py-1.5 rounded-xl border border-white/10 text-white/60 hover:text-white hover:bg-white/5 transition-all text-sm"
        >
          <RefreshCw
            className={cn('w-4 h-4', recsQuery.isFetching && 'animate-spin')}
          />
          Refresh
        </button>
      </div>

      {recsQuery.isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="glass rounded-2xl p-5 space-y-3">
              <Skeleton className="h-6 w-16" />
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-3/4" />
            </div>
          ))}
        </div>
      ) : recs.length === 0 ? (
        <div className="glass rounded-2xl p-12 flex flex-col items-center gap-4 text-white/40">
          <Lightbulb className="w-14 h-14 opacity-30" />
          <p className="text-base">No recommendations available yet</p>
          <p className="text-sm text-center max-w-xs">
            Complete some study sessions and submit doubts to get personalised
            recommendations.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {recs.map((rec) => (
            <motion.div
              key={rec.id}
              className="glass rounded-2xl p-5 flex flex-col gap-3"
              whileHover={{ scale: 1.02 }}
              transition={{ type: 'spring', stiffness: 300, damping: 20 }}
            >
              <div className="flex items-start justify-between">
                <span className="text-2xl">{typeIcon(rec.type)}</span>
                <Badge variant={TYPE_VARIANTS[rec.type] ?? 'default'}>
                  {rec.type}
                </Badge>
              </div>
              {rec.subject && (
                <span className="text-xs text-brand-400 font-medium">
                  {rec.subject}
                </span>
              )}
              <p className="text-white/80 text-sm font-medium line-clamp-2">
                {rec.title ?? 'Recommended Resource'}
              </p>
              {rec.contentPreview && (
                <p className="text-white/40 text-xs line-clamp-3">
                  {rec.contentPreview}
                </p>
              )}
              {rec.url && (
                <a
                  href={rec.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="mt-auto flex items-center gap-1 text-sm text-brand-400 hover:text-brand-300 transition-colors"
                >
                  Open Resource <ChevronRight className="w-3 h-3" />
                </a>
              )}
            </motion.div>
          ))}
        </div>
      )}
    </div>
  );
}

// ─── Main Page ───────────────────────────────────────────────────────────────

type Tab = 'doubts' | 'recommendations';

const TABS: { id: Tab; label: string; icon: React.ReactNode }[] = [
  {
    id: 'doubts',
    label: 'Doubt Resolver',
    icon: <MessageSquare className="w-4 h-4" />,
  },
  {
    id: 'recommendations',
    label: 'Recommendations',
    icon: <Lightbulb className="w-4 h-4" />,
  },
];

export default function AiMentorPage() {
  const [activeTab, setActiveTab] = useState<Tab>('doubts');

  return (
    <motion.div
      className="p-6 space-y-6 max-w-7xl mx-auto"
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
    >
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-white">
          AI <span className="gradient-text">Mentor</span>
        </h1>
        <p className="text-white/40 text-sm mt-1">
          Instant doubt resolution and smart learning recommendations
        </p>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 p-1 glass rounded-xl w-fit">
        {TABS.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={cn(
              'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all',
              activeTab === tab.id
                ? 'bg-brand-500 text-white shadow-lg shadow-neon-sm-cyan'
                : 'text-white/50 hover:text-white/80 hover:bg-white/5',
            )}
          >
            {tab.icon}
            {tab.label}
          </button>
        ))}
      </div>

      {/* Tab Content */}
      <AnimatePresence mode="wait">
        <motion.div
          key={activeTab}
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -8 }}
          transition={{ duration: 0.2 }}
        >
          {activeTab === 'doubts' && <DoubtResolverTab />}
          {activeTab === 'recommendations' && <RecommendationsTab />}
        </motion.div>
      </AnimatePresence>
    </motion.div>
  );
}
