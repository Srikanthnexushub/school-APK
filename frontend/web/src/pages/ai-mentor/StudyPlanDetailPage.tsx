import { useParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import { CheckCircle2, ArrowLeft, BookOpen, Lightbulb, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { cn } from '../../lib/utils';
import { Badge, PriorityBadge } from '../../components/ui/Badge';
import { ProgressBar } from '../../components/ui/ProgressBar';
import { Skeleton } from '../../components/ui/LoadingSkeleton';
import { useAuthStore } from '../../stores/authStore';

// ─── Types ──────────────────────────────────────────────────────────────────

interface StudyPlanItem {
  id: string;
  subject: string;
  topic: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  completed: boolean;
  estimatedMinutes?: number;
  dueDate?: string;
}

interface StudyPlan {
  id: string;
  targetExam?: string;
  status: 'ACTIVE' | 'COMPLETED' | 'PAUSED';
  items?: StudyPlanItem[];
  weakSubjects?: string[];
  createdAt?: string;
  daysUntilExam?: number;
}

// ─── Response Mapper ─────────────────────────────────────────────────────────

function mapPlan(raw: Record<string, unknown>): StudyPlan {
  const rawItems = (raw.items as Record<string, unknown>[] | undefined) ?? [];
  const targetExamDate = raw.targetExamDate as string | undefined;
  const daysUntilExam = targetExamDate
    ? Math.max(0, Math.ceil((new Date(targetExamDate).getTime() - Date.now()) / 86400000))
    : undefined;
  return {
    id: raw.id as string,
    targetExam: raw.title as string,
    status: (raw.active as boolean) ? 'ACTIVE' : 'COMPLETED',
    createdAt: raw.createdAt as string | undefined,
    daysUntilExam,
    items: rawItems.map((item) => ({
      id: item.id as string,
      subject: item.subjectArea as string,
      topic: item.topic as string,
      priority: item.priorityLevel as 'HIGH' | 'MEDIUM' | 'LOW',
      completed: item.quality !== null && item.quality !== undefined,
    })),
    weakSubjects: rawItems
      .filter((i) => i.priorityLevel === 'HIGH' && (i.quality === null || i.quality === undefined))
      .map((i) => i.subjectArea as string)
      .filter((v, idx, arr) => arr.indexOf(v) === idx),
  };
}

// ─── AI Suggestions Side Panel ───────────────────────────────────────────────

const FALLBACK_TIPS = [
  'Review yesterday\'s material before starting new topics',
  'Use active recall over passive re-reading',
];

function AiSuggestionsPanel({
  weakSubjects,
  planId,
}: {
  weakSubjects: string[];
  planId: string;
}) {
  const user = useAuthStore((s) => s.user);

  const tipsQuery = useQuery<string[]>({
    queryKey: ['ai-tips', planId, weakSubjects],
    queryFn: async () => {
      const subjects =
        weakSubjects.length > 0 ? weakSubjects : ['general study topics'];
      const res = await api.post('/api/v1/ai/completions', {
        requesterId: user?.id ?? '',
        systemPrompt:
          'You are a study advisor. Return exactly 5 actionable study tips as a JSON array of strings.',
        userMessage: `Generate 5 specific study tips for a student working on a plan focusing on: ${subjects.join(', ')}. Return JSON array: ["tip1", "tip2", "tip3", "tip4", "tip5"]`,
        maxTokens: 400,
        temperature: 0.7,
      });
      const content: string =
        (res.data as { content?: string })?.content ?? '';
      // Extract JSON array from content (may be wrapped in markdown code fences)
      const jsonMatch = content.match(/\[[\s\S]*\]/);
      if (!jsonMatch) return FALLBACK_TIPS;
      const parsed: unknown = JSON.parse(jsonMatch[0]);
      if (
        Array.isArray(parsed) &&
        parsed.every((t) => typeof t === 'string')
      ) {
        return (parsed as string[]).slice(0, 5);
      }
      return FALLBACK_TIPS;
    },
    enabled: !!planId && !!user?.id,
    staleTime: 10 * 60 * 1000,
    retry: false,
  });

  const tips = tipsQuery.isError
    ? FALLBACK_TIPS
    : (tipsQuery.data ?? []);

  return (
    <div className="glass rounded-2xl p-5 space-y-4">
      <div className="flex items-center gap-2">
        <div className="w-8 h-8 rounded-xl bg-amber-500/20 flex items-center justify-center">
          <Lightbulb className="w-4 h-4 text-amber-400" />
        </div>
        <h3 className="text-white font-semibold">AI Suggestions</h3>
      </div>

      {weakSubjects.length > 0 && (
        <div>
          <p className="text-xs text-white/40 mb-2 uppercase tracking-wider font-medium">Weak Areas</p>
          <div className="flex flex-wrap gap-1.5">
            {weakSubjects.map((s) => (
              <span
                key={s}
                className="px-2 py-0.5 rounded-full text-xs bg-red-500/15 text-red-400 border border-red-500/20"
              >
                {s}
              </span>
            ))}
          </div>
        </div>
      )}

      {tipsQuery.isLoading ? (
        <div className="flex flex-col items-center justify-center py-6 gap-3">
          <Loader2 className="w-6 h-6 text-indigo-400 animate-spin" />
          <p className="text-white/40 text-xs">Generating personalised tips…</p>
        </div>
      ) : (
        <div className="space-y-3">
          {tips.map((tip, i) => (
            <div key={i} className="flex gap-3">
              <span className="w-5 h-5 rounded-full bg-indigo-500/20 text-indigo-400 text-xs flex items-center justify-center shrink-0 mt-0.5 font-medium">
                {i + 1}
              </span>
              <p className="text-white/60 text-sm leading-relaxed">{tip}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ─── Subject Group ───────────────────────────────────────────────────────────

function SubjectGroup({
  subject,
  items,
  onToggle,
  isPending,
}: {
  subject: string;
  items: StudyPlanItem[];
  onToggle: (itemId: string, completed: boolean) => void;
  isPending: boolean;
}) {
  const done = items.filter((i) => i.completed).length;

  return (
    <div className="glass rounded-2xl overflow-hidden">
      {/* Header */}
      <div className="flex items-center justify-between px-5 py-3 border-b border-white/5">
        <div className="flex items-center gap-2">
          <BookOpen className="w-4 h-4 text-indigo-400" />
          <span className="text-white font-semibold">{subject}</span>
        </div>
        <span className="text-white/40 text-xs">
          {done}/{items.length} done
        </span>
      </div>

      {/* Items */}
      <div className="divide-y divide-white/5">
        {items.map((item) => (
          <motion.div
            key={item.id}
            layout
            className={cn(
              'flex items-start gap-3 px-5 py-3 transition-colors hover:bg-white/5',
              item.completed && 'opacity-50'
            )}
          >
            <button
              onClick={() => onToggle(item.id, !item.completed)}
              disabled={isPending}
              className="mt-0.5 shrink-0"
            >
              <CheckCircle2
                className={cn(
                  'w-4 h-4 transition-colors',
                  item.completed
                    ? 'text-emerald-400'
                    : 'text-white/20 hover:text-emerald-400'
                )}
              />
            </button>

            <div className="flex-1 min-w-0">
              <p
                className={cn(
                  'text-sm text-white/80',
                  item.completed && 'line-through'
                )}
              >
                {item.topic}
              </p>
              {item.estimatedMinutes && (
                <p className="text-xs text-white/30 mt-0.5">
                  {item.estimatedMinutes} min estimated
                </p>
              )}
            </div>

            <div className="shrink-0">
              <PriorityBadge priority={item.priority} />
            </div>
          </motion.div>
        ))}
      </div>
    </div>
  );
}

// ─── Main Page ───────────────────────────────────────────────────────────────

export default function StudyPlanDetailPage() {
  const { id: planId } = useParams<{ id: string }>();
  const queryClient = useQueryClient();

  const planQuery = useQuery<StudyPlan>({
    queryKey: ['study-plan', planId],
    queryFn: () =>
      api
        .get(`/api/v1/study-plans/${planId}`)
        .then((r) => mapPlan(r.data as Record<string, unknown>)),
    enabled: !!planId,
    staleTime: 3 * 60 * 1000,
  });

  const toggleMutation = useMutation({
    mutationFn: ({
      itemId,
      completed,
    }: {
      itemId: string;
      completed: boolean;
    }) =>
      api.put(
        `/api/v1/study-plans/items/${itemId}/review?quality=${completed ? 5 : 0}`
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['study-plan', planId] });
      queryClient.invalidateQueries({ queryKey: ['study-plans'] });
    },
    onError: () => toast.error('Failed to update item'),
  });

  const plan = planQuery.data;
  const items = plan?.items ?? [];
  const total = items.length;
  const done = items.filter((i) => i.completed).length;
  const pct = total > 0 ? Math.round((done / total) * 100) : 0;

  // Group items by subject
  const grouped = items.reduce<Record<string, StudyPlanItem[]>>((acc, item) => {
    if (!acc[item.subject]) acc[item.subject] = [];
    acc[item.subject].push(item);
    return acc;
  }, {});

  if (planQuery.isLoading) {
    return (
      <div className="p-6 space-y-6 max-w-7xl mx-auto">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-24 w-full" />
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 space-y-4">
            {[...Array(3)].map((_, i) => (
              <Skeleton key={i} className="h-40 w-full" />
            ))}
          </div>
          <Skeleton className="h-64 w-full" />
        </div>
      </div>
    );
  }

  if (planQuery.isError || !plan) {
    return (
      <div className="p-6 max-w-7xl mx-auto">
        <div className="glass rounded-2xl p-12 text-center space-y-4">
          <p className="text-white/50">Failed to load study plan.</p>
          <button
            onClick={() => planQuery.refetch()}
            className="btn-primary text-sm"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <motion.div
      className="p-6 space-y-6 max-w-7xl mx-auto"
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
    >
      {/* Back link */}
      <a
        href="/ai-mentor"
        className="inline-flex items-center gap-1.5 text-white/40 hover:text-white/70 transition-colors text-sm"
      >
        <ArrowLeft className="w-4 h-4" /> Back to AI Mentor
      </a>

      {/* Plan Header */}
      <div className="glass rounded-2xl p-6 space-y-4">
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-xl font-bold text-white">
              {plan.targetExam ?? 'Study Plan'}
            </h1>
            <p className="text-white/40 text-sm mt-1">
              Created{' '}
              {plan.createdAt
                ? new Date(plan.createdAt).toLocaleDateString()
                : 'recently'}
              {plan.daysUntilExam && ` · ${plan.daysUntilExam} days until exam`}
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

        <div className="space-y-2">
          <div className="flex justify-between text-sm">
            <span className="text-white/60">Overall Progress</span>
            <span className="text-white font-semibold">
              {pct}% — {done}/{total} items
            </span>
          </div>
          <ProgressBar value={done} max={total} color={pct >= 80 ? 'emerald' : 'brand'} />
        </div>
      </div>

      {/* Main content */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Items grouped by subject */}
        <div className="lg:col-span-2 space-y-4">
          {Object.keys(grouped).length === 0 ? (
            <div className="glass rounded-2xl p-10 text-center text-white/40">
              <p>No study items in this plan yet.</p>
            </div>
          ) : (
            Object.entries(grouped).map(([subject, subjectItems]) => (
              <SubjectGroup
                key={subject}
                subject={subject}
                items={subjectItems}
                onToggle={(itemId, completed) =>
                  toggleMutation.mutate({ itemId, completed })
                }
                isPending={toggleMutation.isPending}
              />
            ))
          )}
        </div>

        {/* Side panel */}
        <div>
          <AiSuggestionsPanel
            weakSubjects={plan.weakSubjects ?? []}
            planId={plan.id}
          />
        </div>
      </div>
    </motion.div>
  );
}
