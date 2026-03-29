// src/pages/academic-plan/AcademicPlanPage.tsx
import { useState } from 'react';
import { useAuthStore } from '../../stores/authStore';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import {
  GraduationCap, BookOpen, RefreshCw, FileText, Plus,
  ChevronRight, Bell, BellOff, Calendar, CheckCircle2,
  ClipboardList, AlertCircle, Trash2, ChevronDown, ChevronUp,
  Sparkles, Loader2, ArrowLeft,
} from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { cn } from '../../lib/utils';
import { Badge } from '../../components/ui/Badge';
import { Modal } from '../../components/ui/Modal';
import { CardSkeleton } from '../../components/ui/LoadingSkeleton';
import { ExportMenu } from '../../components/ui/ExportMenu';
import { ProgressBar } from '../../components/ui/ProgressBar';

// ─── Types ─────────────────────────────────────────────────────────────────

type PlanType = 'CURRICULUM_PLAN' | 'REVISION_PLAN' | 'MODEL_PAPERS';
type ModalPhase = 'configure' | 'generating' | 'review';

interface GeneratedItem {
  chapterNumber?: number;
  topic: string;
  subject?: string;
  startDate: string;
  endDate: string;
}

interface ChapterItem {
  id: string;
  chapterNumber?: number;
  topic: string;
  subjectArea: string;
  chapterStartDate?: string;
  chapterEndDate?: string;
  done: boolean;
}

interface AcademicPlan {
  id: string;
  title: string;
  planType: PlanType;
  board?: string;
  startDate?: string;
  endDate?: string;
  targetExamDate?: string;
  subjectAreas: string[];
  totalItems: number;
  doneItems: number;
  active: boolean;
  createdAt?: string;
  chapters: ChapterItem[];
}

interface Reminder {
  id: string;
  studyPlanId: string;
  title: string;
  message?: string;
  remindAt: string;
  acknowledged: boolean;
}

// ─── Constants ─────────────────────────────────────────────────────────────

const PLAN_TYPES: { id: PlanType; label: string; icon: React.ReactNode; desc: string }[] = [
  {
    id: 'CURRICULUM_PLAN',
    label: 'Curriculum Plan',
    icon: <BookOpen className="w-5 h-5" />,
    desc: 'AI schedules your full syllabus chapter by chapter',
  },
  {
    id: 'REVISION_PLAN',
    label: 'Revision Plan',
    icon: <RefreshCw className="w-5 h-5" />,
    desc: 'AI builds a topic-wise revision schedule before your exam',
  },
  {
    id: 'MODEL_PAPERS',
    label: 'Model Papers',
    icon: <FileText className="w-5 h-5" />,
    desc: 'AI creates a mock test schedule with progressive difficulty',
  },
];

const BOARDS = ['CBSE', 'ICSE', 'State Board', 'IB', 'IGCSE', 'Other'];

const SUBJECTS = [
  'Mathematics', 'Physics', 'Chemistry', 'Biology',
  'English', 'History', 'Geography', 'Computer Science',
  'Economics', 'Accountancy', 'Business Studies', 'Hindi',
];

const SUBJECT_AREA_MAP: Record<string, string> = {
  Mathematics: 'MATHEMATICS', Physics: 'PHYSICS', Chemistry: 'CHEMISTRY',
  Biology: 'BIOLOGY', English: 'ENGLISH', History: 'GENERAL',
  Geography: 'GENERAL', 'Computer Science': 'GENERAL', Economics: 'GENERAL',
  Accountancy: 'GENERAL', 'Business Studies': 'GENERAL', Hindi: 'GENERAL',
};

const PLAN_TYPE_COLORS: Record<PlanType, string> = {
  CURRICULUM_PLAN: 'text-indigo-400 bg-indigo-500/15 border-indigo-500/25',
  REVISION_PLAN: 'text-amber-400 bg-amber-500/15 border-amber-500/25',
  MODEL_PAPERS: 'text-emerald-400 bg-emerald-500/15 border-emerald-500/25',
};

const PLAN_TYPE_LABELS: Record<PlanType, string> = {
  CURRICULUM_PLAN: 'Curriculum',
  REVISION_PLAN: 'Revision',
  MODEL_PAPERS: 'Model Papers',
};

// ─── Helpers ───────────────────────────────────────────────────────────────

function daysBetween(a: string, b: string): number {
  if (!a || !b) return 90;
  return Math.max(1, Math.ceil((new Date(b).getTime() - new Date(a).getTime()) / 86400000));
}

// ─── Mappers ───────────────────────────────────────────────────────────────

function mapPlan(raw: Record<string, unknown>): AcademicPlan {
  const rawItems = (raw.items as Record<string, unknown>[] | undefined) ?? [];
  const subjectAreas: string[] = raw.subjectAreas
    ? (raw.subjectAreas as string[])
    : [...new Set(rawItems.map(i => i.subjectArea as string))];
  const chapters: ChapterItem[] = rawItems.map(i => ({
    id: i.id as string,
    chapterNumber: i.chapterNumber as number | undefined,
    topic: i.topic as string,
    subjectArea: i.subjectArea as string,
    chapterStartDate: i.chapterStartDate as string | undefined,
    chapterEndDate: i.chapterEndDate as string | undefined,
    done: i.quality !== null && i.quality !== undefined,
  }));
  return {
    id: raw.id as string,
    title: raw.title as string,
    planType: (raw.planType as PlanType) ?? 'CURRICULUM_PLAN',
    board: raw.board as string | undefined,
    startDate: raw.startDate as string | undefined,
    endDate: raw.endDate as string | undefined,
    targetExamDate: raw.targetExamDate as string | undefined,
    subjectAreas,
    totalItems: rawItems.length,
    doneItems: rawItems.filter(i => i.quality !== null && i.quality !== undefined).length,
    active: raw.active as boolean,
    createdAt: raw.createdAt as string | undefined,
    chapters,
  };
}

function mapReminder(raw: Record<string, unknown>): Reminder {
  return {
    id: raw.id as string,
    studyPlanId: raw.studyPlanId as string,
    title: raw.title as string,
    message: raw.message as string | undefined,
    remindAt: raw.remindAt as string,
    acknowledged: raw.acknowledged as boolean,
  };
}

// ─── Reminder Modal ────────────────────────────────────────────────────────

function ReminderModal({ reminders, onAcknowledge }: { reminders: Reminder[]; onAcknowledge: (id: string) => void }) {
  const [current, setCurrent] = useState(0);
  if (reminders.length === 0) return null;
  const r = reminders[current];
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-6 bg-black/60">
      <motion.div
        initial={{ scale: 0.9, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        className="glass rounded-2xl p-6 max-w-sm w-full border border-indigo-500/30 shadow-2xl shadow-indigo-500/20"
      >
        <div className="flex items-center gap-3 mb-4">
          <div className="w-10 h-10 rounded-full bg-indigo-500/20 flex items-center justify-center">
            <Bell className="w-5 h-5 text-indigo-400" />
          </div>
          <div>
            <p className="text-xs text-indigo-400 font-semibold uppercase tracking-wider">Reminder</p>
            <h3 className="text-white font-semibold text-sm">{r.title}</h3>
          </div>
          {reminders.length > 1 && <span className="ml-auto text-xs text-white/40">{current + 1}/{reminders.length}</span>}
        </div>
        {r.message && <p className="text-white/60 text-sm mb-4 leading-relaxed">{r.message}</p>}
        <p className="text-white/30 text-xs mb-5">
          <Calendar className="w-3 h-3 inline mr-1" />
          {new Date(r.remindAt).toLocaleString()}
        </p>
        <div className="flex gap-2">
          <button onClick={() => onAcknowledge(r.id)} className="flex-1 btn-primary text-sm flex items-center justify-center gap-2">
            <CheckCircle2 className="w-4 h-4" /> Got it
          </button>
          {current < reminders.length - 1 && (
            <button onClick={() => setCurrent(c => c + 1)} className="px-3 py-2 rounded-xl border border-white/10 text-white/50 hover:text-white hover:bg-white/5 transition-all text-sm">
              Next
            </button>
          )}
        </div>
      </motion.div>
    </div>
  );
}

// ─── Schedule Table (plan card expand) ─────────────────────────────────────

function ScheduleTable({ chapters, planType }: { chapters: ChapterItem[]; planType: PlanType }) {
  const sorted = [...chapters].sort((a, b) => (a.chapterNumber ?? 0) - (b.chapterNumber ?? 0));
  if (sorted.length === 0) return null;
  const isCurriculum = planType === 'CURRICULUM_PLAN';
  return (
    <div className="overflow-x-auto rounded-xl border border-white/10 mt-2">
      <table className="w-full text-xs">
        <thead>
          <tr className="border-b border-white/10">
            {isCurriculum && <th className="text-left px-3 py-2 text-white/40 font-medium w-10">Ch#</th>}
            {!isCurriculum && <th className="text-left px-3 py-2 text-white/40 font-medium w-24">Subject</th>}
            <th className="text-left px-3 py-2 text-white/40 font-medium">Topic</th>
            <th className="text-left px-3 py-2 text-white/40 font-medium w-24">From</th>
            <th className="text-left px-3 py-2 text-white/40 font-medium w-24">To</th>
          </tr>
        </thead>
        <tbody>
          {sorted.map((ch, i) => (
            <tr key={ch.id} className={cn('border-b border-white/5 last:border-0', ch.done && 'opacity-50')}>
              {isCurriculum && <td className="px-3 py-2 text-white/50">{ch.chapterNumber ?? i + 1}</td>}
              {!isCurriculum && <td className="px-3 py-2 text-white/50 truncate max-w-[80px]">{ch.subjectArea}</td>}
              <td className="px-3 py-2 text-white/80"><span className={ch.done ? 'line-through' : ''}>{ch.topic}</span></td>
              <td className="px-3 py-2 text-white/50">{ch.chapterStartDate ? new Date(ch.chapterStartDate).toLocaleDateString() : '—'}</td>
              <td className="px-3 py-2 text-white/50">{ch.chapterEndDate ? new Date(ch.chapterEndDate).toLocaleDateString() : '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

// ─── Main Page ─────────────────────────────────────────────────────────────

export default function AcademicPlanPage() {
  const queryClient = useQueryClient();
  const user = useAuthStore(s => s.user);

  // ── Common form state ──
  const [modalOpen, setModalOpen] = useState(false);
  const [planType, setPlanType] = useState<PlanType>('CURRICULUM_PLAN');
  const [title, setTitle] = useState('');
  const [board, setBoard] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [targetExamDate, setTargetExamDate] = useState('');

  // ── Curriculum-specific ──
  const [curriculumSubject, setCurriculumSubject] = useState(SUBJECTS[0]);

  // ── Revision / Model Papers ──
  const [subjects, setSubjects] = useState<string[]>([]);

  // ── AI generation phase ──
  const [phase, setPhase] = useState<ModalPhase>('configure');
  const [generatedItems, setGeneratedItems] = useState<GeneratedItem[]>([]);

  // ── Card expand ──
  const [expandedPlanId, setExpandedPlanId] = useState<string | null>(null);

  // ── Reminder state ──
  const [reminderModalOpen, setReminderModalOpen] = useState(false);
  const [createdPlanId, setCreatedPlanId] = useState<string | null>(null);
  const [reminderDate, setReminderDate] = useState('');

  // ── Queries ──
  const plansQuery = useQuery<AcademicPlan[]>({
    queryKey: ['academic-plans'],
    queryFn: () =>
      api.get('/api/v1/study-plans').then(r => {
        const d = r.data;
        const arr: Record<string, unknown>[] = Array.isArray(d) ? d : (d.content ?? []);
        return arr.map(mapPlan);
      }),
    staleTime: 3 * 60 * 1000,
  });

  const remindersQuery = useQuery<Reminder[]>({
    queryKey: ['pending-reminders'],
    queryFn: () =>
      api.get('/api/v1/reminders/pending').then(r => {
        const d = r.data;
        const arr: Record<string, unknown>[] = Array.isArray(d) ? d : (d.content ?? []);
        return arr.map(mapReminder);
      }),
    staleTime: 60 * 1000,
    refetchOnWindowFocus: true,
  });

  const pendingReminders = remindersQuery.data ?? [];

  // ── Mutations ──
  const createMutation = useMutation({
    mutationFn: (payload: Record<string, unknown>) => api.post('/api/v1/study-plans', payload),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['academic-plans'] });
      setCreatedPlanId(res.data.id);
      setModalOpen(false);
      resetForm();
      setReminderModalOpen(true);
      toast.success('Academic plan created!');
    },
    onError: () => toast.error('Failed to create plan'),
  });

  const saveReminderMutation = useMutation({
    mutationFn: (payload: Record<string, unknown>) => api.post('/api/v1/reminders', payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pending-reminders'] });
      setReminderModalOpen(false);
      setReminderDate('');
      toast.success('Reminder set!');
    },
    onError: () => toast.error('Failed to save reminder'),
  });

  const ackMutation = useMutation({
    mutationFn: (id: string) => api.put(`/api/v1/reminders/${id}/acknowledge`, {}),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['pending-reminders'] }),
  });

  function resetForm() {
    setTitle(''); setBoard(''); setSubjects([]);
    setStartDate(''); setEndDate(''); setTargetExamDate('');
    setCurriculumSubject(SUBJECTS[0]);
    setPhase('configure');
    setGeneratedItems([]);
  }

  function toggleSubject(s: string) {
    setSubjects(prev => prev.includes(s) ? prev.filter(x => x !== s) : [...prev, s]);
  }

  // ── AI generation ──
  async function handleGenerate() {
    if (!title.trim()) { toast.error('Enter a plan title'); return; }
    if (!startDate || !endDate) { toast.error('Select From and To dates'); return; }
    if (planType !== 'CURRICULUM_PLAN' && subjects.length === 0) { toast.error('Select at least one subject'); return; }

    const days = daysBetween(startDate, endDate);
    let userMessage = '';

    if (planType === 'CURRICULUM_PLAN') {
      userMessage = `Generate a complete chapter-by-chapter academic year plan.

Subject: ${curriculumSubject}
Board: ${board || 'CBSE'}
From: ${startDate}
To: ${endDate}
Total days: ${days}

Return ONLY a valid JSON array (no markdown, no explanation, no code fences):
[{"chapterNumber":1,"topic":"Chapter Name","startDate":"YYYY-MM-DD","endDate":"YYYY-MM-DD"},...]

Include ALL standard chapters for ${curriculumSubject} ${board || 'CBSE'} curriculum. Distribute chapters proportionally across the full period. All dates must be within ${startDate} to ${endDate}.`;

    } else if (planType === 'REVISION_PLAN') {
      userMessage = `Generate a topic-wise revision schedule.

Subjects: ${subjects.join(', ')}
Board: ${board || 'CBSE'}
Revision Period: ${startDate} to ${endDate} (${days} days)

Return ONLY a valid JSON array (no markdown, no explanation, no code fences):
[{"topic":"Topic description","subject":"Subject Name","startDate":"YYYY-MM-DD","endDate":"YYYY-MM-DD"},...]

Cover all key topics for each subject. Use subject names exactly from this list: ${subjects.join(', ')}. Spread revision sessions evenly across the period. All dates must be within ${startDate} to ${endDate}.`;

    } else {
      userMessage = `Generate a mock test and model paper practice schedule.

Subjects: ${subjects.join(', ')}
Board: ${board || 'CBSE'}
Practice Period: ${startDate} to ${endDate} (${days} days)

Return ONLY a valid JSON array (no markdown, no explanation, no code fences):
[{"topic":"Mock Test description","subject":"Subject Name","startDate":"YYYY-MM-DD","endDate":"YYYY-MM-DD"},...]

Start with topic-wise tests, then subject-wise tests, then full syllabus mock tests. Cover all subjects. Use subject names exactly from: ${subjects.join(', ')}. All dates must be within ${startDate} to ${endDate}.`;
    }

    setPhase('generating');

    try {
      const res = await api.post('/api/v1/ai/completions', {
        requesterId: user?.id ?? '',
        systemPrompt: 'You are an academic planning expert. Always return ONLY valid JSON arrays. Never include markdown code fences, explanations, or any text outside the JSON array.',
        userMessage,
        maxTokens: 2000,
        temperature: 0.3,
      });

      const content: string = (res.data as { content?: string })?.content ?? '';
      const jsonMatch = content.match(/\[[\s\S]*\]/);
      if (!jsonMatch) throw new Error('No JSON array in response');
      const parsed: unknown = JSON.parse(jsonMatch[0]);
      if (!Array.isArray(parsed) || parsed.length === 0) throw new Error('Empty or invalid response');

      setGeneratedItems(parsed as GeneratedItem[]);
      setPhase('review');
    } catch {
      toast.error('AI generation failed. Please try again.');
      setPhase('configure');
    }
  }

  // ── Create plan from reviewed items ──
  function handleCreateFromReview() {
    const validItems = generatedItems.filter(item => item.topic?.trim());
    if (validItems.length === 0) { toast.error('No valid items to save'); return; }

    let items: Record<string, unknown>[];

    if (planType === 'CURRICULUM_PLAN') {
      items = validItems.map((item, i) => ({
        subjectArea: SUBJECT_AREA_MAP[curriculumSubject] ?? 'GENERAL',
        topic: item.topic.trim(),
        priorityLevel: 'MEDIUM',
        chapterNumber: item.chapterNumber ?? i + 1,
        chapterStartDate: item.startDate || null,
        chapterEndDate: item.endDate || null,
      }));
    } else {
      items = validItems.map(item => ({
        subjectArea: SUBJECT_AREA_MAP[item.subject ?? subjects[0] ?? ''] ?? 'GENERAL',
        topic: item.topic.trim(),
        priorityLevel: planType === 'REVISION_PLAN' ? 'HIGH' : 'MEDIUM',
        chapterStartDate: item.startDate || null,
        chapterEndDate: item.endDate || null,
      }));
    }

    createMutation.mutate({
      studentId: user?.id ?? '',
      title: title.trim(),
      planType,
      board: board || null,
      startDate,
      endDate,
      targetExamDate: targetExamDate || endDate,
      items,
    });
  }

  function updateGeneratedItem(index: number, field: keyof GeneratedItem, value: string | number) {
    setGeneratedItems(prev => prev.map((item, i) => i === index ? { ...item, [field]: value } : item));
  }

  function removeGeneratedItem(index: number) {
    setGeneratedItems(prev => prev.filter((_, i) => i !== index));
  }

  function addGeneratedItem() {
    const lastItem = generatedItems[generatedItems.length - 1];
    setGeneratedItems(prev => [
      ...prev,
      {
        chapterNumber: planType === 'CURRICULUM_PLAN' ? (generatedItems.length + 1) : undefined,
        topic: '',
        subject: planType !== 'CURRICULUM_PLAN' ? (subjects[0] ?? '') : undefined,
        startDate: lastItem?.endDate ?? endDate,
        endDate: lastItem?.endDate ?? endDate,
      },
    ]);
  }

  function handleSaveReminder() {
    if (!reminderDate || !createdPlanId) return;
    const plan = plansQuery.data?.find(p => p.id === createdPlanId);
    saveReminderMutation.mutate({
      studyPlanId: createdPlanId,
      reminderType: 'REVIEW_REMINDER',
      remindAt: new Date(reminderDate).toISOString(),
      title: `Review: ${plan?.title ?? 'Academic Plan'}`,
      message: `Time to review your ${PLAN_TYPE_LABELS[plan?.planType ?? 'CURRICULUM_PLAN']} — ${plan?.subjectAreas.join(', ')}`,
    });
  }

  const plans = plansQuery.data ?? [];

  const csvData = plans.map(p => ({
    Title: p.title,
    Type: PLAN_TYPE_LABELS[p.planType],
    Board: p.board ?? '',
    Subjects: p.subjectAreas.join('; '),
    'Start Date': p.startDate ?? '',
    'End Date': p.endDate ?? '',
    'Target Exam': p.targetExamDate ?? '',
    Progress: `${p.doneItems}/${p.totalItems}`,
    Status: p.active ? 'Active' : 'Inactive',
    Created: p.createdAt ? new Date(p.createdAt).toLocaleDateString() : '',
  }));

  return (
    <motion.div
      className="p-6 space-y-6 max-w-7xl mx-auto"
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
    >
      {pendingReminders.length > 0 && (
        <ReminderModal reminders={pendingReminders} onAcknowledge={id => ackMutation.mutate(id)} />
      )}

      {/* Header */}
      <div className="flex items-start justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <GraduationCap className="w-7 h-7 text-indigo-400" />
            Academic <span className="gradient-text">Plan</span>
          </h1>
          <p className="text-white/40 text-sm mt-1">
            AI-generated curriculum, revision schedules, and mock test plans
          </p>
        </div>
        <div className="flex items-center gap-2">
          <ExportMenu csvData={csvData} csvFilename="academic-plans" />
          <button onClick={() => setModalOpen(true)} className="btn-primary flex items-center gap-2 text-sm">
            <Plus className="w-4 h-4" /> New Plan
          </button>
        </div>
      </div>

      {/* Plan type cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        {PLAN_TYPES.map(pt => (
          <div key={pt.id} className="glass rounded-xl p-4 flex items-start gap-3">
            <div className={cn('p-2 rounded-lg border', PLAN_TYPE_COLORS[pt.id])}>
              {pt.icon}
            </div>
            <div>
              <p className="text-white font-medium text-sm">{pt.label}</p>
              <p className="text-white/40 text-xs mt-0.5">{pt.desc}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Plans list */}
      {plansQuery.isLoading ? (
        <div className="grid gap-4 md:grid-cols-2">
          {[...Array(4)].map((_, i) => <CardSkeleton key={i} />)}
        </div>
      ) : plans.length === 0 ? (
        <div className="glass rounded-2xl p-12 flex flex-col items-center gap-4 text-white/40">
          <GraduationCap className="w-14 h-14 opacity-30" />
          <p className="text-base">No academic plans yet</p>
          <button onClick={() => setModalOpen(true)} className="btn-primary flex items-center gap-2 text-sm">
            <Plus className="w-4 h-4" /> Create Your First Plan
          </button>
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {plans.map(plan => {
            const pct = plan.totalItems > 0 ? (plan.doneItems / plan.totalItems) * 100 : 0;
            const isExpanded = expandedPlanId === plan.id;
            const hasSchedule = plan.chapters.some(ch => ch.chapterStartDate || ch.chapterEndDate);
            return (
              <motion.div
                key={plan.id}
                className="glass rounded-2xl p-5 space-y-3"
                whileHover={{ scale: 1.005 }}
                transition={{ type: 'spring', stiffness: 300, damping: 20 }}
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="flex-1 min-w-0">
                    <h3 className="text-white font-semibold truncate">{plan.title}</h3>
                    <p className="text-white/40 text-xs mt-0.5">
                      Created {plan.createdAt ? new Date(plan.createdAt).toLocaleDateString() : 'recently'}
                    </p>
                  </div>
                  <span className={cn('text-xs px-2 py-0.5 rounded-full border font-medium shrink-0', PLAN_TYPE_COLORS[plan.planType])}>
                    {PLAN_TYPE_LABELS[plan.planType]}
                  </span>
                </div>

                <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-white/50">
                  {plan.board && <span className="flex items-center gap-1"><ClipboardList className="w-3 h-3" /> {plan.board}</span>}
                  {plan.startDate && plan.endDate && (
                    <span className="flex items-center gap-1">
                      <Calendar className="w-3 h-3" />
                      {new Date(plan.startDate).toLocaleDateString()} – {new Date(plan.endDate).toLocaleDateString()}
                    </span>
                  )}
                  {plan.targetExamDate && (
                    <span className="flex items-center gap-1 text-amber-400/70">
                      <AlertCircle className="w-3 h-3" />
                      Exam: {new Date(plan.targetExamDate).toLocaleDateString()}
                    </span>
                  )}
                </div>

                {plan.subjectAreas.length > 0 && (
                  <div className="flex flex-wrap gap-1.5">
                    {plan.subjectAreas.slice(0, 5).map(s => (
                      <span key={s} className="px-2 py-0.5 rounded-full text-xs bg-indigo-500/15 text-indigo-400 border border-indigo-500/20">{s}</span>
                    ))}
                    {plan.subjectAreas.length > 5 && (
                      <span className="px-2 py-0.5 rounded-full text-xs bg-white/5 text-white/40">+{plan.subjectAreas.length - 5}</span>
                    )}
                  </div>
                )}

                {plan.totalItems > 0 && (
                  <div className="space-y-1">
                    <div className="flex justify-between text-xs text-white/40">
                      <span>Progress</span>
                      <span>{plan.doneItems}/{plan.totalItems} {plan.planType === 'CURRICULUM_PLAN' ? 'chapters' : 'topics'}</span>
                    </div>
                    <ProgressBar value={plan.doneItems} max={plan.totalItems} color={pct >= 80 ? 'emerald' : 'brand'} />
                  </div>
                )}

                {/* Schedule toggle */}
                {plan.chapters.length > 0 && (
                  <div>
                    <button
                      onClick={() => setExpandedPlanId(isExpanded ? null : plan.id)}
                      className="flex items-center gap-1.5 text-xs text-indigo-400 hover:text-indigo-300 transition-colors"
                    >
                      {isExpanded ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
                      {isExpanded ? 'Hide' : 'View'} {plan.planType === 'CURRICULUM_PLAN' ? 'Chapters' : 'Schedule'} ({plan.chapters.length})
                      {!hasSchedule && <span className="text-white/30 ml-1">(no dates)</span>}
                    </button>
                    <AnimatePresence>
                      {isExpanded && (
                        <motion.div
                          initial={{ opacity: 0, height: 0 }}
                          animate={{ opacity: 1, height: 'auto' }}
                          exit={{ opacity: 0, height: 0 }}
                          transition={{ duration: 0.2 }}
                          className="overflow-hidden"
                        >
                          <ScheduleTable chapters={plan.chapters} planType={plan.planType} />
                        </motion.div>
                      )}
                    </AnimatePresence>
                  </div>
                )}

                <div className="flex items-center justify-between pt-1">
                  <a
                    href={`/academic-plan/${plan.id}`}
                    className="flex items-center gap-1 text-sm text-indigo-400 hover:text-indigo-300 transition-colors"
                  >
                    Open Plan <ChevronRight className="w-3 h-3" />
                  </a>
                  <Badge variant={plan.active ? 'info' : 'default'}>
                    {plan.active ? 'ACTIVE' : 'INACTIVE'}
                  </Badge>
                </div>
              </motion.div>
            );
          })}
        </div>
      )}

      {/* ── Create Plan Modal ── */}
      <Modal
        isOpen={modalOpen}
        onClose={() => { if (phase !== 'generating') { setModalOpen(false); resetForm(); } }}
        maxWidth="max-w-2xl"
        title={
          phase === 'configure' ? 'Create Academic Plan' :
          phase === 'generating' ? 'Generating Your Plan...' :
          'Review AI-Generated Plan'
        }
      >
        <AnimatePresence mode="wait">

          {/* ── Phase 1: Configure ── */}
          {phase === 'configure' && (
            <motion.div
              key="configure"
              initial={{ opacity: 0, x: -12 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -12 }}
              className="space-y-5"
            >
              {/* Plan type */}
              <div>
                <label className="block text-sm text-white/60 mb-2">Plan Type</label>
                <div className="grid grid-cols-1 gap-2">
                  {PLAN_TYPES.map(pt => (
                    <button
                      key={pt.id}
                      type="button"
                      onClick={() => setPlanType(pt.id)}
                      className={cn(
                        'flex items-center gap-3 p-3 rounded-xl border text-left transition-all',
                        planType === pt.id ? 'border-indigo-500/60 bg-indigo-500/10' : 'border-white/10 hover:bg-white/5'
                      )}
                    >
                      <span className={cn('p-1.5 rounded-lg border', PLAN_TYPE_COLORS[pt.id])}>{pt.icon}</span>
                      <div>
                        <p className="text-white text-sm font-medium">{pt.label}</p>
                        <p className="text-white/40 text-xs">{pt.desc}</p>
                      </div>
                      {planType === pt.id && <CheckCircle2 className="w-4 h-4 text-indigo-400 ml-auto shrink-0" />}
                    </button>
                  ))}
                </div>
              </div>

              {/* Title */}
              <div>
                <label className="block text-sm text-white/60 mb-1.5">Plan Title</label>
                <input
                  className="input w-full"
                  placeholder={planType === 'CURRICULUM_PLAN' ? 'e.g. Class 10 Maths 2026-27' : planType === 'REVISION_PLAN' ? 'e.g. JEE 2027 Final Revision' : 'e.g. NEET 2027 Mock Tests'}
                  value={title}
                  onChange={e => setTitle(e.target.value)}
                />
              </div>

              {/* Board */}
              <div>
                <label className="block text-sm text-white/60 mb-1.5">Board / Curriculum</label>
                <select className="input w-full" value={board} onChange={e => setBoard(e.target.value)}>
                  <option value="">Select board (optional)</option>
                  {BOARDS.map(b => <option key={b} value={b}>{b}</option>)}
                </select>
              </div>

              {/* Dates */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm text-white/60 mb-1.5">From Date</label>
                  <input type="date" className="input w-full" value={startDate} onChange={e => setStartDate(e.target.value)} />
                </div>
                <div>
                  <label className="block text-sm text-white/60 mb-1.5">To Date</label>
                  <input type="date" className="input w-full" value={endDate} onChange={e => setEndDate(e.target.value)} />
                </div>
              </div>

              {/* Target exam (optional) */}
              <div>
                <label className="block text-sm text-white/60 mb-1.5">
                  Target Exam Date <span className="text-white/30">(optional)</span>
                </label>
                <input type="date" className="input w-full" value={targetExamDate} onChange={e => setTargetExamDate(e.target.value)} />
              </div>

              {/* Subject — single for Curriculum, multi for others */}
              {planType === 'CURRICULUM_PLAN' ? (
                <div>
                  <label className="block text-sm text-white/60 mb-1.5">Subject</label>
                  <select className="input w-full" value={curriculumSubject} onChange={e => setCurriculumSubject(e.target.value)}>
                    {SUBJECTS.map(s => <option key={s} value={s}>{s}</option>)}
                  </select>
                  <p className="text-white/30 text-xs mt-1.5">
                    AI will generate all chapters with schedule for the selected subject
                  </p>
                </div>
              ) : (
                <div>
                  <label className="block text-sm text-white/60 mb-2">Subjects</label>
                  <div className="grid grid-cols-2 gap-2 max-h-40 overflow-y-auto pr-1">
                    {SUBJECTS.map(s => (
                      <label key={s} className="flex items-center gap-2 cursor-pointer group">
                        <input
                          type="checkbox"
                          checked={subjects.includes(s)}
                          onChange={() => toggleSubject(s)}
                          className="w-4 h-4 rounded border-white/20 bg-white/5 text-indigo-500 focus:ring-indigo-500"
                        />
                        <span className="text-sm text-white/70 group-hover:text-white/90 transition-colors">{s}</span>
                      </label>
                    ))}
                  </div>
                  <p className="text-white/30 text-xs mt-1.5">
                    AI will generate a {planType === 'REVISION_PLAN' ? 'topic-wise revision schedule' : 'mock test schedule'} for selected subjects
                  </p>
                </div>
              )}

              {/* AI notice */}
              <div className="flex items-start gap-2.5 p-3 bg-indigo-500/10 border border-indigo-500/20 rounded-xl">
                <Sparkles className="w-4 h-4 text-indigo-400 shrink-0 mt-0.5" />
                <p className="text-indigo-300/80 text-xs leading-relaxed">
                  AI will generate a complete{' '}
                  {planType === 'CURRICULUM_PLAN' ? 'chapter-by-chapter curriculum' :
                   planType === 'REVISION_PLAN' ? 'topic-wise revision schedule' : 'mock test schedule'}{' '}
                  based on your subject, board, and date range. You can review and edit before saving.
                </p>
              </div>

              {/* Buttons */}
              <div className="flex gap-3 pt-1">
                <button
                  onClick={() => { setModalOpen(false); resetForm(); }}
                  className="flex-1 px-4 py-2 rounded-xl border border-white/10 text-white/60 hover:text-white hover:bg-white/5 transition-all text-sm"
                >
                  Cancel
                </button>
                <button
                  onClick={handleGenerate}
                  className="flex-1 btn-primary text-sm flex items-center justify-center gap-2"
                >
                  <Sparkles className="w-4 h-4" /> Generate with AI
                </button>
              </div>
            </motion.div>
          )}

          {/* ── Phase 2: Generating ── */}
          {phase === 'generating' && (
            <motion.div
              key="generating"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="flex flex-col items-center justify-center py-16 gap-5"
            >
              <div className="relative">
                <div className="w-16 h-16 rounded-full bg-indigo-500/20 flex items-center justify-center">
                  <Sparkles className="w-8 h-8 text-indigo-400" />
                </div>
                <Loader2 className="w-20 h-20 text-indigo-500/30 animate-spin absolute -top-2 -left-2" />
              </div>
              <div className="text-center space-y-1.5">
                <p className="text-white font-semibold">AI is building your plan...</p>
                <p className="text-white/40 text-sm">
                  Generating {planType === 'CURRICULUM_PLAN' ? `${curriculumSubject} chapters` : planType === 'REVISION_PLAN' ? 'revision schedule' : 'mock test schedule'} for {daysBetween(startDate, endDate)} days
                </p>
              </div>
            </motion.div>
          )}

          {/* ── Phase 3: Review ── */}
          {phase === 'review' && (
            <motion.div
              key="review"
              initial={{ opacity: 0, x: 12 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 12 }}
              className="space-y-4"
            >
              <div className="flex items-center gap-2 p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl">
                <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                <p className="text-emerald-300/80 text-xs">
                  AI generated {generatedItems.length} {planType === 'CURRICULUM_PLAN' ? 'chapters' : 'items'}. Review and edit before saving.
                </p>
              </div>

              {/* Editable table */}
              <div className="overflow-x-auto rounded-xl border border-white/10 max-h-72 overflow-y-auto">
                <table className="w-full text-xs">
                  <thead className="sticky top-0 bg-surface-50/90 backdrop-blur-sm z-10">
                    <tr className="border-b border-white/10">
                      {planType === 'CURRICULUM_PLAN' && <th className="text-left px-3 py-2 text-white/40 font-medium w-10">Ch#</th>}
                      {planType !== 'CURRICULUM_PLAN' && <th className="text-left px-3 py-2 text-white/40 font-medium w-28">Subject</th>}
                      <th className="text-left px-3 py-2 text-white/40 font-medium">Topic / Name</th>
                      <th className="text-left px-3 py-2 text-white/40 font-medium w-32">From</th>
                      <th className="text-left px-3 py-2 text-white/40 font-medium w-32">To</th>
                      <th className="w-8" />
                    </tr>
                  </thead>
                  <tbody>
                    {generatedItems.map((item, i) => (
                      <tr key={i} className="border-b border-white/5 last:border-0">
                        {planType === 'CURRICULUM_PLAN' && (
                          <td className="px-3 py-1.5 text-white/50">{item.chapterNumber ?? i + 1}</td>
                        )}
                        {planType !== 'CURRICULUM_PLAN' && (
                          <td className="px-2 py-1.5">
                            <select
                              className="input w-full text-xs py-1"
                              value={item.subject ?? subjects[0] ?? ''}
                              onChange={e => updateGeneratedItem(i, 'subject', e.target.value)}
                            >
                              {subjects.map(s => <option key={s} value={s}>{s}</option>)}
                            </select>
                          </td>
                        )}
                        <td className="px-2 py-1.5">
                          <input
                            className="input w-full text-xs py-1"
                            value={item.topic}
                            onChange={e => updateGeneratedItem(i, 'topic', e.target.value)}
                          />
                        </td>
                        <td className="px-2 py-1.5">
                          <input
                            type="date"
                            className="input w-full text-xs py-1"
                            value={item.startDate ?? ''}
                            onChange={e => updateGeneratedItem(i, 'startDate', e.target.value)}
                          />
                        </td>
                        <td className="px-2 py-1.5">
                          <input
                            type="date"
                            className="input w-full text-xs py-1"
                            value={item.endDate ?? ''}
                            onChange={e => updateGeneratedItem(i, 'endDate', e.target.value)}
                          />
                        </td>
                        <td className="px-2 py-1.5">
                          <button
                            type="button"
                            onClick={() => removeGeneratedItem(i)}
                            className="p-1 rounded-lg hover:bg-red-500/20 text-white/30 hover:text-red-400 transition-colors"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <button
                type="button"
                onClick={addGeneratedItem}
                className="flex items-center gap-1.5 text-xs text-indigo-400 hover:text-indigo-300 transition-colors px-2 py-1"
              >
                <Plus className="w-3.5 h-3.5" /> Add Row
              </button>

              {/* Buttons */}
              <div className="flex gap-3 pt-1">
                <button
                  onClick={() => setPhase('configure')}
                  className="flex items-center gap-1.5 px-4 py-2 rounded-xl border border-white/10 text-white/60 hover:text-white hover:bg-white/5 transition-all text-sm"
                >
                  <ArrowLeft className="w-3.5 h-3.5" /> Back
                </button>
                <button
                  onClick={handleGenerate}
                  className="px-4 py-2 rounded-xl border border-indigo-500/30 text-indigo-400 hover:bg-indigo-500/10 transition-all text-sm flex items-center gap-1.5"
                >
                  <RefreshCw className="w-3.5 h-3.5" /> Regenerate
                </button>
                <button
                  onClick={handleCreateFromReview}
                  disabled={createMutation.isPending}
                  className="flex-1 btn-primary text-sm"
                >
                  {createMutation.isPending ? 'Creating...' : 'Create Plan'}
                </button>
              </div>
            </motion.div>
          )}

        </AnimatePresence>
      </Modal>

      {/* ── Set Reminder Modal ── */}
      <Modal
        isOpen={reminderModalOpen}
        onClose={() => { setReminderModalOpen(false); setReminderDate(''); }}
        title="Set a Reminder"
      >
        <div className="space-y-4">
          <div className="flex items-center gap-3 p-3 bg-indigo-500/10 border border-indigo-500/20 rounded-xl">
            <Bell className="w-5 h-5 text-indigo-400 shrink-0" />
            <p className="text-white/70 text-sm">Your plan was created! Would you like to set a reminder to review it?</p>
          </div>
          <div>
            <label className="block text-sm text-white/60 mb-1.5">Remind me on</label>
            <input type="datetime-local" className="input w-full" value={reminderDate} onChange={e => setReminderDate(e.target.value)} />
          </div>
          <div className="flex gap-3">
            <button
              onClick={() => { setReminderModalOpen(false); setReminderDate(''); }}
              className="flex-1 px-4 py-2 rounded-xl border border-white/10 text-white/60 hover:text-white hover:bg-white/5 transition-all text-sm flex items-center justify-center gap-2"
            >
              <BellOff className="w-4 h-4" /> Skip
            </button>
            <button
              onClick={handleSaveReminder}
              disabled={!reminderDate || saveReminderMutation.isPending}
              className="flex-1 btn-primary text-sm flex items-center justify-center gap-2"
            >
              <Bell className="w-4 h-4" />
              {saveReminderMutation.isPending ? 'Saving...' : 'Set Reminder'}
            </button>
          </div>
        </div>
      </Modal>
    </motion.div>
  );
}
