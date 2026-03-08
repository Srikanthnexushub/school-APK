import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import {
  Search,
  BookOpen,
  Clock,
  ListChecks,
  ChevronRight,
  FlaskConical,
  Calculator,
  Globe,
  Microscope,
  Cpu,
  Pencil,
  FileText,
  AlertCircle,
} from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { cn } from '../../lib/utils';
import { Badge } from '../../components/ui/Badge';
import { CardSkeleton } from '../../components/ui/LoadingSkeleton';

// ─── Types ───────────────────────────────────────────────────────────────────

type ExamStatus = 'AVAILABLE' | 'ENROLLED' | 'IN_PROGRESS' | 'COMPLETED' | 'UPCOMING';
type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';
type FilterOption = 'ALL' | 'AVAILABLE' | 'ENROLLED' | 'COMPLETED';

interface Exam {
  id: string;
  name: string;
  description: string;
  subject: string;
  durationSeconds: number;
  totalQuestions: number;
  difficulty: Difficulty;
  status: ExamStatus;
  startDate?: string;
  enrollmentId?: string;
  score?: number;
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

const SUBJECT_ICONS: Record<string, React.ReactNode> = {
  Mathematics:    <Calculator className="w-5 h-5" />,
  Science:        <Microscope className="w-5 h-5" />,
  Chemistry:      <FlaskConical className="w-5 h-5" />,
  Physics:        <Cpu className="w-5 h-5" />,
  English:        <Pencil className="w-5 h-5" />,
  Geography:      <Globe className="w-5 h-5" />,
  History:        <BookOpen className="w-5 h-5" />,
  default:        <FileText className="w-5 h-5" />,
};

const SUBJECT_COLORS: Record<string, string> = {
  Mathematics: 'bg-blue-500/20 text-blue-400 border-blue-500/20',
  Science:     'bg-emerald-500/20 text-emerald-400 border-emerald-500/20',
  Chemistry:   'bg-purple-500/20 text-purple-400 border-purple-500/20',
  Physics:     'bg-cyan-500/20 text-cyan-400 border-cyan-500/20',
  English:     'bg-rose-500/20 text-rose-400 border-rose-500/20',
  Geography:   'bg-teal-500/20 text-teal-400 border-teal-500/20',
  History:     'bg-amber-500/20 text-amber-400 border-amber-500/20',
  default:     'bg-brand-500/20 text-brand-400 border-brand-500/20',
};

const DIFFICULTY_BADGE: Record<Difficulty, { label: string; className: string }> = {
  EASY:   { label: 'Easy',   className: 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/20' },
  MEDIUM: { label: 'Medium', className: 'bg-amber-500/15 text-amber-400 border border-amber-500/20' },
  HARD:   { label: 'Hard',   className: 'bg-red-500/15 text-red-400 border border-red-500/20' },
};

function formatDuration(seconds: number): string {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  if (h === 0) return `${m} min`;
  if (m === 0) return `${h} hr`;
  return `${h} hr ${m} min`;
}

// ─── Exam Card ────────────────────────────────────────────────────────────────

function ExamCard({ exam, onEnroll, isEnrolling }: { exam: Exam; onEnroll: (id: string) => void; isEnrolling: boolean }) {
  const navigate = useNavigate();
  const iconKey = Object.keys(SUBJECT_ICONS).find((k) => exam.subject.includes(k)) ?? 'default';
  const colorClass = SUBJECT_COLORS[iconKey] ?? SUBJECT_COLORS.default;
  const diff = DIFFICULTY_BADGE[exam.difficulty];

  function handleAction() {
    if (exam.status === 'AVAILABLE') {
      onEnroll(exam.id);
    } else if (exam.status === 'ENROLLED' || exam.status === 'IN_PROGRESS') {
      navigate(`/assessments/${exam.enrollmentId}/exam`);
    } else if (exam.status === 'COMPLETED') {
      navigate(`/assessments/${exam.enrollmentId}/results`);
    }
  }

  return (
    <motion.div
      className="glass rounded-2xl p-5 flex flex-col gap-4 hover:border-white/10 transition-all duration-200"
      whileHover={{ y: -2 }}
      transition={{ type: 'spring', stiffness: 300, damping: 20 }}
    >
      {/* Header */}
      <div className="flex items-start gap-3">
        <div className={cn('w-11 h-11 rounded-xl flex items-center justify-center border flex-shrink-0', colorClass)}>
          {SUBJECT_ICONS[iconKey] ?? SUBJECT_ICONS.default}
        </div>
        <div className="flex-1 min-w-0">
          <h3 className="font-semibold text-white text-sm leading-tight line-clamp-1">{exam.name}</h3>
          <p className="text-white/40 text-xs mt-0.5">{exam.subject}</p>
        </div>
        <span className={cn('badge flex-shrink-0', diff.className)}>{diff.label}</span>
      </div>

      {/* Description */}
      <p className="text-white/50 text-xs leading-relaxed line-clamp-2">{exam.description}</p>

      {/* Meta */}
      <div className="flex items-center gap-4 text-xs text-white/40">
        <span className="flex items-center gap-1.5">
          <Clock className="w-3.5 h-3.5" />
          {formatDuration(exam.durationSeconds)}
        </span>
        <span className="flex items-center gap-1.5">
          <ListChecks className="w-3.5 h-3.5" />
          {exam.totalQuestions} questions
        </span>
      </div>

      {/* Start date notice */}
      {exam.status === 'UPCOMING' && exam.startDate && (
        <p className="text-xs text-amber-400/80 flex items-center gap-1.5">
          <AlertCircle className="w-3.5 h-3.5" />
          Opens {new Date(exam.startDate).toLocaleDateString()}
        </p>
      )}

      {/* Score for completed */}
      {exam.status === 'COMPLETED' && exam.score !== undefined && (
        <div className="flex items-center gap-2">
          <div className="flex-1 h-1.5 bg-white/5 rounded-full overflow-hidden">
            <div
              className="h-full bg-brand-500 rounded-full"
              style={{ width: `${exam.score}%` }}
            />
          </div>
          <span className="text-xs font-medium text-white/60">{exam.score}%</span>
        </div>
      )}

      {/* Action */}
      {exam.status !== 'UPCOMING' && (
        <button
          onClick={handleAction}
          disabled={isEnrolling && exam.status === 'AVAILABLE'}
          className={cn(
            'w-full flex items-center justify-center gap-1.5 py-2 rounded-xl text-sm font-medium transition-all duration-200',
            exam.status === 'AVAILABLE'
              ? 'btn-primary'
              : exam.status === 'COMPLETED'
              ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/20 hover:bg-emerald-500/25'
              : 'bg-brand-500/15 text-brand-400 border border-brand-500/20 hover:bg-brand-500/25'
          )}
        >
          {isEnrolling && exam.status === 'AVAILABLE' ? (
            <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
          ) : (
            <>
              {exam.status === 'AVAILABLE' && 'Enroll'}
              {(exam.status === 'ENROLLED' || exam.status === 'IN_PROGRESS') && 'Continue'}
              {exam.status === 'COMPLETED' && 'View Results'}
              <ChevronRight className="w-3.5 h-3.5" />
            </>
          )}
        </button>
      )}
    </motion.div>
  );
}

// ─── Empty State ──────────────────────────────────────────────────────────────

function EmptyState({ filter }: { filter: FilterOption }) {
  const messages: Record<FilterOption, string> = {
    ALL:       'No exams are available right now. Check back soon.',
    AVAILABLE: 'No available exams to enroll in.',
    ENROLLED:  "You haven't enrolled in any exams yet.",
    COMPLETED: "You haven't completed any exams yet.",
  };
  return (
    <div className="col-span-full flex flex-col items-center justify-center py-24 text-center gap-4">
      <div className="w-16 h-16 rounded-2xl bg-brand-500/10 border border-brand-500/20 flex items-center justify-center">
        <BookOpen className="w-8 h-8 text-brand-400/60" />
      </div>
      <div>
        <p className="text-white/60 font-medium">No exams found</p>
        <p className="text-white/30 text-sm mt-1">{messages[filter]}</p>
      </div>
    </div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function AssessmentsPage() {
  const user = useAuthStore((s) => s.user);
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState<FilterOption>('ALL');
  const [enrollingId, setEnrollingId] = useState<string | null>(null);

  const { data: exams = [], isLoading, isError, refetch } = useQuery<Exam[]>({
    queryKey: ['exams', user?.id],
    queryFn: async () => {
      try {
        const res = await api.get('/api/v1/exams');
        return res.data;
      } catch (err: unknown) {
        const axiosErr = err as { response?: { status?: number } };
        if (axiosErr.response?.status === 404) return [];
        throw err;
      }
    },
    retry: 1,
  });

  const enrollMutation = useMutation({
    mutationFn: (examId: string) => api.post(`/api/v1/exams/${examId}/enrollments`, { studentId: user?.id }),
    onSuccess: () => {
      toast.success('Successfully enrolled in exam!');
      queryClient.invalidateQueries({ queryKey: ['exams'] });
    },
    onError: () => {
      toast.error('Failed to enroll. Please try again.');
    },
    onSettled: () => setEnrollingId(null),
  });

  function handleEnroll(examId: string) {
    setEnrollingId(examId);
    enrollMutation.mutate(examId);
  }

  const filtered = exams.filter((e) => {
    const matchSearch =
      e.name.toLowerCase().includes(search.toLowerCase()) ||
      e.subject.toLowerCase().includes(search.toLowerCase());
    const matchFilter =
      filter === 'ALL' ||
      (filter === 'AVAILABLE' && e.status === 'AVAILABLE') ||
      (filter === 'ENROLLED' && (e.status === 'ENROLLED' || e.status === 'IN_PROGRESS')) ||
      (filter === 'COMPLETED' && e.status === 'COMPLETED');
    return matchSearch && matchFilter;
  });

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
      className="p-6 space-y-6"
    >
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">Assessments</h1>
          <p className="text-white/40 text-sm mt-0.5">Browse and take adaptive exams</p>
        </div>
        <div className="flex items-center gap-3">
          {/* Search */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
            <input
              type="text"
              placeholder="Search exams..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="input pl-9 w-56 text-sm"
            />
          </div>
          {/* Filter */}
          <select
            value={filter}
            onChange={(e) => setFilter(e.target.value as FilterOption)}
            className="input text-sm cursor-pointer"
          >
            <option value="ALL">All Exams</option>
            <option value="AVAILABLE">Available</option>
            <option value="ENROLLED">Enrolled</option>
            <option value="COMPLETED">Completed</option>
          </select>
        </div>
      </div>

      {/* Error state */}
      {isError && (
        <div className="glass rounded-2xl p-6 flex items-center gap-4">
          <AlertCircle className="w-5 h-5 text-red-400 flex-shrink-0" />
          <div className="flex-1">
            <p className="text-white/80 text-sm">Failed to load exams.</p>
          </div>
          <button onClick={() => refetch()} className="btn-primary text-sm py-1.5 px-3">
            Retry
          </button>
        </div>
      )}

      {/* Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {isLoading
          ? Array.from({ length: 6 }).map((_, i) => <CardSkeleton key={i} />)
          : filtered.length === 0
          ? <EmptyState filter={filter} />
          : filtered.map((exam) => (
              <ExamCard
                key={exam.id}
                exam={exam}
                onEnroll={handleEnroll}
                isEnrolling={enrollingId === exam.id}
              />
            ))}
      </div>
    </motion.div>
  );
}
