import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import {
  MessageSquare, CheckCircle2, Clock, Plus, Send, ChevronDown, ChevronUp,
} from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { cn } from '../../lib/utils';
import { Badge } from '../../components/ui/Badge';
import { Modal } from '../../components/ui/Modal';
import { StatCard } from '../../components/ui/StatCard';
import { Skeleton, StatCardSkeleton } from '../../components/ui/LoadingSkeleton';

// ─── Types ──────────────────────────────────────────────────────────────────

interface DoubtTicket {
  id: string;
  subject: string;
  questionText: string;
  status: 'PENDING' | 'RESOLVED' | 'IN_PROGRESS';
  answer?: string;
  resolvedAt?: string;
  createdAt?: string;
  resolutionTimeMinutes?: number;
}

function mapDoubt(raw: Record<string, unknown>): DoubtTicket {
  const createdAt = raw.createdAt as string | undefined;
  const resolvedAt = raw.resolvedAt as string | undefined;
  const resolutionTimeMinutes =
    createdAt && resolvedAt
      ? Math.round((new Date(resolvedAt).getTime() - new Date(createdAt).getTime()) / 60000)
      : undefined;
  return {
    id: raw.id as string,
    subject: raw.subjectArea as string,
    questionText: raw.question as string,
    status: raw.status === 'ESCALATED' ? 'IN_PROGRESS' : (raw.status as 'PENDING' | 'RESOLVED'),
    answer: (raw.aiAnswer as string) ?? undefined,
    resolvedAt,
    createdAt,
    resolutionTimeMinutes,
  };
}

type FilterTab = 'ALL' | 'PENDING' | 'RESOLVED';

const SUBJECTS = [
  'Mathematics', 'Physics', 'Chemistry', 'Biology',
  'English', 'History', 'Geography', 'Computer Science',
];

// ─── Doubt Card ──────────────────────────────────────────────────────────────

function DoubtCard({ doubt }: { doubt: DoubtTicket }) {
  const [expanded, setExpanded] = useState(false);

  const statusVariant = doubt.status === 'RESOLVED' ? 'success'
    : doubt.status === 'IN_PROGRESS' ? 'info'
    : 'warning';

  return (
    <motion.div
      layout
      className="glass rounded-2xl overflow-hidden"
    >
      {/* Header */}
      <button
        className="w-full text-left p-5 flex items-start gap-3"
        onClick={() => setExpanded(x => !x)}
      >
        <div className={cn(
          'w-2 h-2 rounded-full mt-2 shrink-0',
          doubt.status === 'RESOLVED' ? 'bg-emerald-400' :
          doubt.status === 'IN_PROGRESS' ? 'bg-indigo-400' : 'bg-amber-400'
        )} />

        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1 flex-wrap">
            <span className="text-xs px-2 py-0.5 rounded-full bg-indigo-500/20 text-indigo-400">
              {doubt.subject}
            </span>
            <Badge variant={statusVariant}>{doubt.status}</Badge>
            <span className="ml-auto text-xs text-white/30 flex items-center gap-1">
              <Clock className="w-3 h-3" />
              {doubt.createdAt ? new Date(doubt.createdAt).toLocaleDateString() : 'Recent'}
            </span>
          </div>
          <p className="text-white/80 text-sm line-clamp-2 leading-relaxed">
            {doubt.questionText}
          </p>
        </div>

        <div className="shrink-0 mt-1 text-white/30">
          {expanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
        </div>
      </button>

      {/* Expanded content */}
      <AnimatePresence>
        {expanded && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="overflow-hidden"
          >
            <div className="border-t border-white/5 p-5 space-y-4">
              {/* Full question */}
              <div>
                <p className="text-xs text-white/40 uppercase tracking-wider font-medium mb-2">Question</p>
                <p className="text-white/70 text-sm leading-relaxed">{doubt.questionText}</p>
              </div>

              {/* Answer */}
              {doubt.answer ? (
                <div>
                  <p className="text-xs text-white/40 uppercase tracking-wider font-medium mb-2">Answer</p>
                  <div className="bg-emerald-500/5 border border-emerald-500/15 rounded-xl p-4">
                    <p className="text-white/80 text-sm leading-relaxed">{doubt.answer}</p>
                  </div>
                  {doubt.resolvedAt && (
                    <p className="text-xs text-white/30 mt-2 flex items-center gap-1">
                      <CheckCircle2 className="w-3 h-3 text-emerald-400" />
                      Resolved on {new Date(doubt.resolvedAt).toLocaleDateString()}
                      {doubt.resolutionTimeMinutes > 0 && ` · ${doubt.resolutionTimeMinutes} min response time`}
                    </p>
                  )}
                </div>
              ) : (
                <div className="bg-amber-500/5 border border-amber-500/15 rounded-xl p-4 flex items-center gap-3">
                  <div className="flex gap-1">
                    {[...Array(3)].map((_, i) => (
                      <motion.span
                        key={i}
                        className="w-1.5 h-1.5 rounded-full bg-amber-400"
                        animate={{ opacity: [0.3, 1, 0.3] }}
                        transition={{ duration: 1.2, repeat: Infinity, delay: i * 0.2 }}
                      />
                    ))}
                  </div>
                  <p className="text-amber-400/70 text-sm">Waiting for resolution...</p>
                </div>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}

// ─── Main Page ───────────────────────────────────────────────────────────────

export default function DoubtsPage() {
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState<FilterTab>('ALL');
  const [modalOpen, setModalOpen] = useState(false);
  const [subject, setSubject] = useState(SUBJECTS[0]);
  const [questionText, setQuestionText] = useState('');

  const SUBJECT_AREA_MAP: Record<string, string> = {
    Mathematics: 'MATHEMATICS', Physics: 'PHYSICS', Chemistry: 'CHEMISTRY',
    Biology: 'BIOLOGY', English: 'ENGLISH', History: 'GENERAL',
    Geography: 'GENERAL', 'Computer Science': 'GENERAL',
  };

  const doubtsQuery = useQuery<DoubtTicket[]>({
    queryKey: ['doubts'],
    queryFn: () => api.get('/api/v1/doubts').then(r => { const d = r.data; const arr: Record<string, unknown>[] = Array.isArray(d) ? d : (d.content ?? []); return arr.map(mapDoubt); }),
    staleTime: 2 * 60 * 1000,
  });

  const submitMutation = useMutation({
    mutationFn: (payload: { subjectArea: string; question: string }) =>
      api.post('/api/v1/doubts', payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['doubts'] });
      setModalOpen(false);
      setQuestionText('');
      toast.success('Doubt submitted successfully!');
    },
    onError: () => toast.error('Failed to submit doubt'),
  });

  function handleSubmit() {
    if (!questionText.trim()) { toast.error('Enter your question'); return; }
    submitMutation.mutate({ subjectArea: SUBJECT_AREA_MAP[subject] ?? 'GENERAL', question: questionText.trim() });
  }

  const all = doubtsQuery.data ?? [];
  const pending = all.filter(d => d.status === 'PENDING' || d.status === 'IN_PROGRESS');
  const resolved = all.filter(d => d.status === 'RESOLVED');

  const avgResTime = resolved.length > 0
    ? Math.round(resolved.reduce((s, d) => s + (d.resolutionTimeMinutes ?? 0), 0) / resolved.length)
    : null;

  const filtered = filter === 'PENDING' ? pending : filter === 'RESOLVED' ? resolved : all;

  const FILTER_TABS: { id: FilterTab; label: string; count: number }[] = [
    { id: 'ALL', label: 'All', count: all.length },
    { id: 'PENDING', label: 'Pending', count: pending.length },
    { id: 'RESOLVED', label: 'Resolved', count: resolved.length },
  ];

  return (
    <motion.div
      className="p-6 space-y-6 max-w-4xl mx-auto"
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
    >
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-white">
          Doubt <span className="gradient-text">Tracker</span>
        </h1>
        <p className="text-white/40 text-sm mt-1">Submit, track, and review your academic doubts</p>
      </div>

      {/* Stats Row */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {doubtsQuery.isLoading ? (
          [...Array(4)].map((_, i) => <StatCardSkeleton key={i} />)
        ) : (
          <>
            <StatCard
              title="Total Submitted"
              value={all.length}
              icon={<MessageSquare className="w-4 h-4 text-indigo-400" />}
              iconBg="bg-indigo-500/20"
            />
            <StatCard
              title="Resolved"
              value={resolved.length}
              icon={<CheckCircle2 className="w-4 h-4 text-emerald-400" />}
              iconBg="bg-emerald-500/20"
            />
            <StatCard
              title="Pending"
              value={pending.length}
              icon={<Clock className="w-4 h-4 text-amber-400" />}
              iconBg="bg-amber-500/20"
            />
            <StatCard
              title="Avg Resolution"
              value={avgResTime !== null ? `${avgResTime}m` : '—'}
              icon={<Clock className="w-4 h-4 text-white/50" />}
              iconBg="bg-white/10"
              subtitle="Response time"
            />
          </>
        )}
      </div>

      {/* Filter Tabs */}
      <div className="flex gap-1 p-1 glass rounded-xl w-fit">
        {FILTER_TABS.map(tab => (
          <button
            key={tab.id}
            onClick={() => setFilter(tab.id)}
            className={cn(
              'flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium transition-all',
              filter === tab.id
                ? 'bg-indigo-500 text-white shadow-lg shadow-indigo-500/25'
                : 'text-white/50 hover:text-white/80 hover:bg-white/5'
            )}
          >
            {tab.label}
            <span className={cn(
              'px-1.5 py-0.5 rounded-full text-xs',
              filter === tab.id ? 'bg-white/20' : 'bg-white/10'
            )}>
              {tab.count}
            </span>
          </button>
        ))}
      </div>

      {/* Doubts List */}
      {doubtsQuery.isLoading ? (
        <div className="space-y-3">
          {[...Array(4)].map((_, i) => <Skeleton key={i} className="h-20 w-full" />)}
        </div>
      ) : filtered.length === 0 ? (
        <div className="glass rounded-2xl p-12 flex flex-col items-center gap-4 text-white/40">
          <MessageSquare className="w-14 h-14 opacity-30" />
          <p className="text-base">No {filter !== 'ALL' ? filter.toLowerCase() : ''} doubts found</p>
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map(doubt => (
            <DoubtCard key={doubt.id} doubt={doubt} />
          ))}
        </div>
      )}

      {/* FAB — Submit New Doubt */}
      <button
        onClick={() => setModalOpen(true)}
        className="fixed bottom-8 right-8 w-14 h-14 bg-indigo-500 hover:bg-indigo-600 text-white rounded-full shadow-xl shadow-indigo-500/40 flex items-center justify-center transition-all hover:scale-110 active:scale-95 z-40"
        title="Submit New Doubt"
      >
        <Plus className="w-6 h-6" />
      </button>

      {/* Submit Modal */}
      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title="Submit New Doubt">
        <div className="space-y-4">
          <div>
            <label className="block text-sm text-white/60 mb-1.5">Subject</label>
            <select
              className="input w-full"
              value={subject}
              onChange={e => setSubject(e.target.value)}
            >
              {SUBJECTS.map(s => <option key={s} value={s}>{s}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm text-white/60 mb-1.5">Your Question</label>
            <textarea
              className="input w-full resize-none"
              rows={5}
              placeholder="Describe your doubt in detail. The more context you provide, the better the answer."
              value={questionText}
              onChange={e => setQuestionText(e.target.value)}
            />
            <p className="text-xs text-white/30 mt-1">{questionText.length} characters</p>
          </div>
          <div className="flex gap-3 pt-2">
            <button
              onClick={() => setModalOpen(false)}
              className="flex-1 px-4 py-2 rounded-xl border border-white/10 text-white/60 hover:text-white hover:bg-white/5 transition-all text-sm"
            >
              Cancel
            </button>
            <button
              onClick={handleSubmit}
              disabled={submitMutation.isPending || !questionText.trim()}
              className="flex-1 btn-primary text-sm flex items-center justify-center gap-2"
            >
              {submitMutation.isPending ? (
                'Submitting...'
              ) : (
                <><Send className="w-4 h-4" /> Submit Doubt</>
              )}
            </button>
          </div>
        </div>
      </Modal>
    </motion.div>
  );
}
