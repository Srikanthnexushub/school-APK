import { useState } from 'react';
import { useAuthStore } from '../../stores/authStore';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import {
  BookOpen, MessageSquare, Lightbulb, Plus, RefreshCw, Bot,
  Clock, ChevronRight, Search, Send,
} from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { cn } from '../../lib/utils';
import { Badge } from '../../components/ui/Badge';
import { ProgressBar } from '../../components/ui/ProgressBar';
import { Modal } from '../../components/ui/Modal';
import { CardSkeleton, Skeleton } from '../../components/ui/LoadingSkeleton';

// ─── Types ──────────────────────────────────────────────────────────────────

interface StudyPlanItem {
  id: string;
  subject: string;
  topic: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  completed: boolean;
}

interface StudyPlan {
  id: string;
  targetExam?: string;
  status: 'ACTIVE' | 'COMPLETED' | 'PAUSED';
  items?: StudyPlanItem[];
  subjectAreas?: string[];
  daysUntilExam?: number;
  createdAt?: string;
}

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

function mapPlan(raw: Record<string, unknown>): StudyPlan {
  const rawItems = (raw.items as Record<string, unknown>[] | undefined) ?? [];
  return {
    id: raw.id as string,
    targetExam: raw.title as string,
    status: (raw.active as boolean) ? 'ACTIVE' : 'COMPLETED',
    createdAt: raw.createdAt as string | undefined,
    items: rawItems.map((item) => ({
      id: item.id as string,
      subject: item.subjectArea as string,
      topic: item.topic as string,
      priority: item.priorityLevel as 'HIGH' | 'MEDIUM' | 'LOW',
      completed: item.quality !== null && item.quality !== undefined,
    })),
    subjectAreas: [...new Set(rawItems.map((i) => i.subjectArea as string))],
  };
}

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

const TYPE_VARIANTS: Record<string, 'info' | 'success' | 'warning' | 'default'> = {
  VIDEO: 'info',
  ARTICLE: 'default',
  PRACTICE: 'success',
  MOCK_TEST: 'warning',
};

// ─── Study Plans Tab ─────────────────────────────────────────────────────────

const SUBJECT_AREA_MAP: Record<string, string> = {
  Mathematics: 'MATHEMATICS',
  Physics: 'PHYSICS',
  Chemistry: 'CHEMISTRY',
  Biology: 'BIOLOGY',
  English: 'ENGLISH',
  History: 'GENERAL',
  Geography: 'GENERAL',
  'Computer Science': 'GENERAL',
};

function StudyPlansTab() {
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.user);
  const [modalOpen, setModalOpen] = useState(false);
  const [targetExam, setTargetExam] = useState('');
  const [daysUntil, setDaysUntil] = useState('');
  const [weakSubjects, setWeakSubjects] = useState<string[]>([]);

  const plansQuery = useQuery<StudyPlan[]>({
    queryKey: ['study-plans'],
    queryFn: () => api.get('/api/v1/study-plans').then((r) => { const d = r.data; const arr: Record<string, unknown>[] = Array.isArray(d) ? d : (d.content ?? []); return arr.map(mapPlan); }),
    staleTime: 3 * 60 * 1000,
  });

  const createMutation = useMutation({
    mutationFn: (payload: {
      studentId: string;
      title: string;
      targetExamDate: string;
      items: { subjectArea: string; topic: string; priorityLevel: string }[];
    }) => api.post('/api/v1/study-plans', payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['study-plans'] });
      setModalOpen(false);
      setTargetExam('');
      setDaysUntil('');
      setWeakSubjects([]);
      toast.success('Study plan created!');
    },
    onError: () => toast.error('Failed to create study plan'),
  });

  function handleCreate() {
    if (!targetExam.trim()) {
      toast.error('Enter a target exam name');
      return;
    }
    const days = Number(daysUntil) || 90;
    const targetDate = new Date();
    targetDate.setDate(targetDate.getDate() + days);
    const targetExamDate = targetDate.toISOString().split('T')[0];
    const items = weakSubjects.map((s) => ({
      subjectArea: SUBJECT_AREA_MAP[s] ?? 'GENERAL',
      topic: s,
      priorityLevel: 'HIGH',
    }));
    createMutation.mutate({
      studentId: user?.id ?? '',
      title: targetExam.trim(),
      targetExamDate,
      items,
    });
  }

  function toggleSubject(s: string) {
    setWeakSubjects((prev) =>
      prev.includes(s) ? prev.filter((x) => x !== s) : [...prev, s],
    );
  }

  const plans = plansQuery.data ?? [];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-white">Your Study Plans</h2>
        <button
          onClick={() => setModalOpen(true)}
          className="btn-primary flex items-center gap-2 text-sm"
        >
          <Plus className="w-4 h-4" /> Create New Plan
        </button>
      </div>

      {plansQuery.isLoading ? (
        <div className="grid gap-4">
          {[...Array(3)].map((_, i) => (
            <CardSkeleton key={i} />
          ))}
        </div>
      ) : plans.length === 0 ? (
        <div className="glass rounded-2xl p-12 flex flex-col items-center gap-4 text-white/40">
          <BookOpen className="w-14 h-14 opacity-30" />
          <p className="text-base">No study plans yet</p>
          <button
            onClick={() => setModalOpen(true)}
            className="btn-primary flex items-center gap-2 text-sm"
          >
            <Plus className="w-4 h-4" /> Create Your First Plan
          </button>
        </div>
      ) : (
        <div className="grid gap-4">
          {plans.map((plan) => {
            const total = plan.items?.length ?? 0;
            const done = plan.items?.filter((i) => i.completed).length ?? 0;
            const pct = total > 0 ? (done / total) * 100 : 0;
            const areas =
              plan.subjectAreas ??
              [...new Set(plan.items?.map((i) => i.subject) ?? [])];

            return (
              <motion.div
                key={plan.id}
                className="glass rounded-2xl p-5"
                whileHover={{ scale: 1.005 }}
                transition={{ type: 'spring', stiffness: 300, damping: 20 }}
              >
                <div className="flex items-start justify-between mb-3">
                  <div>
                    <h3 className="text-white font-semibold">
                      {plan.targetExam ?? 'Study Plan'}
                    </h3>
                    <p className="text-white/40 text-xs mt-0.5">
                      Created{' '}
                      {plan.createdAt
                        ? new Date(plan.createdAt).toLocaleDateString()
                        : 'recently'}
                    </p>
                  </div>
                  <Badge
                    variant={
                      plan.status === 'ACTIVE'
                        ? 'info'
                        : plan.status === 'COMPLETED'
                          ? 'success'
                          : 'default'
                    }
                  >
                    {plan.status}
                  </Badge>
                </div>

                {areas.length > 0 && (
                  <div className="flex flex-wrap gap-1.5 mb-3">
                    {areas.slice(0, 6).map((area) => (
                      <span
                        key={area}
                        className="px-2 py-0.5 rounded-full text-xs bg-indigo-500/15 text-indigo-400 border border-indigo-500/20"
                      >
                        {area}
                      </span>
                    ))}
                    {areas.length > 6 && (
                      <span className="px-2 py-0.5 rounded-full text-xs bg-white/5 text-white/40">
                        +{areas.length - 6}
                      </span>
                    )}
                  </div>
                )}

                {total > 0 && (
                  <div className="mb-3 space-y-1">
                    <div className="flex justify-between text-xs text-white/40">
                      <span>Progress</span>
                      <span>
                        {done}/{total} items
                      </span>
                    </div>
                    <ProgressBar
                      value={done}
                      max={total}
                      color={pct >= 80 ? 'emerald' : 'brand'}
                    />
                  </div>
                )}

                <a
                  href={`/ai-mentor/study-plans/${plan.id}`}
                  className="flex items-center gap-1 text-sm text-indigo-400 hover:text-indigo-300 transition-colors"
                >
                  Open Plan <ChevronRight className="w-3 h-3" />
                </a>
              </motion.div>
            );
          })}
        </div>
      )}

      <Modal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title="Create Study Plan"
      >
        <div className="space-y-4">
          <div>
            <label className="block text-sm text-white/60 mb-1.5">
              Target Exam
            </label>
            <input
              className="input w-full"
              placeholder="e.g. JEE Advanced 2026"
              value={targetExam}
              onChange={(e) => setTargetExam(e.target.value)}
            />
          </div>
          <div>
            <label className="block text-sm text-white/60 mb-1.5">
              Days Until Exam
            </label>
            <input
              className="input w-full"
              type="number"
              placeholder="e.g. 90"
              value={daysUntil}
              onChange={(e) => setDaysUntil(e.target.value)}
            />
          </div>
          <div>
            <label className="block text-sm text-white/60 mb-2">
              Weak Subjects (select all that apply)
            </label>
            <div className="grid grid-cols-2 gap-2">
              {SUBJECTS.map((s) => (
                <label key={s} className="flex items-center gap-2 cursor-pointer group">
                  <input
                    type="checkbox"
                    checked={weakSubjects.includes(s)}
                    onChange={() => toggleSubject(s)}
                    className="w-4 h-4 rounded border-white/20 bg-white/5 text-indigo-500 focus:ring-indigo-500"
                  />
                  <span className="text-sm text-white/70 group-hover:text-white/90 transition-colors">
                    {s}
                  </span>
                </label>
              ))}
            </div>
          </div>
          <div className="flex gap-3 pt-2">
            <button
              onClick={() => setModalOpen(false)}
              className="flex-1 px-4 py-2 rounded-xl border border-white/10 text-white/60 hover:text-white hover:bg-white/5 transition-all text-sm"
            >
              Cancel
            </button>
            <button
              onClick={handleCreate}
              disabled={createMutation.isPending}
              className="flex-1 btn-primary text-sm"
            >
              {createMutation.isPending ? 'Creating...' : 'Create Plan'}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
}

// ─── Doubt Resolver Tab ──────────────────────────────────────────────────────

function DoubtResolverTab() {
  const queryClient = useQueryClient();
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [newSubject, setNewSubject] = useState(SUBJECTS[0]);
  const [newQuestion, setNewQuestion] = useState('');

  const doubtsQuery = useQuery<DoubtTicket[]>({
    queryKey: ['doubts'],
    queryFn: () => api.get('/api/v1/doubts').then((r) => { const d = r.data; const arr: Record<string, unknown>[] = Array.isArray(d) ? d : (d.content ?? []); return arr.map(mapDoubt); }),
    staleTime: 2 * 60 * 1000,
  });

  const submitMutation = useMutation({
    mutationFn: (payload: { subjectArea: string; question: string }) =>
      api.post('/api/v1/doubts', payload),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ['doubts'] });
      setNewQuestion('');
      setSelectedId(response.data.id);
      toast.success('Doubt submitted!');
    },
    onError: () => toast.error('Failed to submit doubt'),
  });

  const doubts = doubtsQuery.data ?? [];
  const selected = doubts.find((d) => d.id === selectedId);

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
      return <span className="w-2 h-2 rounded-full bg-indigo-400 shrink-0" />;
    return <span className="w-2 h-2 rounded-full bg-amber-400 shrink-0" />;
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-5 gap-4" style={{ minHeight: '520px' }}>
      {/* Left: list */}
      <div className="lg:col-span-2 glass rounded-2xl overflow-hidden flex flex-col">
        <div className="px-4 py-3 border-b border-white/5">
          <h3 className="text-white font-semibold text-sm">My Doubts</h3>
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
              <span className="text-xs px-2 py-0.5 rounded-full bg-indigo-500/20 text-indigo-400">
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
                <div className="max-w-[80%] bg-indigo-500/20 border border-indigo-500/20 rounded-2xl rounded-tr-sm px-4 py-3">
                  <p className="text-white/90 text-sm leading-relaxed">
                    {selected.questionText}
                  </p>
                </div>
              </div>

              {/* AI answer */}
              {selected.answer ? (
                <div className="flex gap-3">
                  <div className="w-8 h-8 rounded-full bg-indigo-500/30 flex items-center justify-center shrink-0 mt-1">
                    <Bot className="w-4 h-4 text-indigo-400" />
                  </div>
                  <div className="max-w-[80%] glass rounded-2xl rounded-tl-sm px-4 py-3 border border-white/10">
                    <p className="text-white/80 text-sm leading-relaxed">
                      {selected.answer}
                    </p>
                  </div>
                </div>
              ) : (
                <div className="flex gap-3">
                  <div className="w-8 h-8 rounded-full bg-indigo-500/30 flex items-center justify-center shrink-0">
                    <Bot className="w-4 h-4 text-indigo-400" />
                  </div>
                  <div className="glass rounded-2xl rounded-tl-sm px-4 py-3 border border-white/10 flex items-center gap-2">
                    <div className="flex gap-1">
                      {[...Array(3)].map((_, i) => (
                        <motion.span
                          key={i}
                          className="w-1.5 h-1.5 rounded-full bg-indigo-400"
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
            <CardSkeleton key={i} />
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
                <span className="text-xs text-indigo-400 font-medium">
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
                  className="mt-auto flex items-center gap-1 text-sm text-indigo-400 hover:text-indigo-300 transition-colors"
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

type Tab = 'plans' | 'doubts' | 'recommendations';

const TABS: { id: Tab; label: string; icon: React.ReactNode }[] = [
  {
    id: 'plans',
    label: 'Study Plans',
    icon: <BookOpen className="w-4 h-4" />,
  },
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
  const [activeTab, setActiveTab] = useState<Tab>('plans');

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
          Personalised study plans, instant doubt resolution, smart
          recommendations
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
                ? 'bg-indigo-500 text-white shadow-lg shadow-indigo-500/25'
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
          {activeTab === 'plans' && <StudyPlansTab />}
          {activeTab === 'doubts' && <DoubtResolverTab />}
          {activeTab === 'recommendations' && <RecommendationsTab />}
        </motion.div>
      </AnimatePresence>
    </motion.div>
  );
}
