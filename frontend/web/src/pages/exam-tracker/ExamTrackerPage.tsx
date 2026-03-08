import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import {
  CalendarDays,
  PlayCircle,
  Trophy,
  CheckCircle2,
  AlertCircle,
  Clock,
  BarChart3,
  ClipboardList,
  BookOpen,
  ChevronRight,
  Layers,
} from 'lucide-react';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { cn } from '../../lib/utils';
import { StatCard } from '../../components/ui/StatCard';
import { Skeleton, StatCardSkeleton, CardSkeleton } from '../../components/ui/LoadingSkeleton';

// ─── Types ────────────────────────────────────────────────────────────────────

type EnrollmentStatus = 'REGISTERED' | 'IN_PROGRESS' | 'COMPLETED' | 'MISSED';
type MockDifficulty = 'EASY' | 'MEDIUM' | 'HARD';

interface ExamEnrollment {
  id: string;
  examId: string;
  examName: string;
  subject: string;
  scheduledDate: string;
  status: EnrollmentStatus;
  score?: number;
  percentile?: number;
}

interface MockTestAttempt {
  id: string;
  mockTestId: string;
  score: number;
  attemptedAt: string;
}

interface MockTest {
  id: string;
  name: string;
  subject: string;
  difficulty: MockDifficulty;
  durationSeconds: number;
  questionCount: number;
  attempts: MockTestAttempt[];
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

const STATUS_BADGE: Record<
  EnrollmentStatus,
  { label: string; className: string; icon: React.ReactNode }
> = {
  REGISTERED:  {
    label: 'Registered',
    className: 'bg-brand-500/15 text-brand-400 border border-brand-500/20',
    icon: <CalendarDays className="w-3 h-3" />,
  },
  IN_PROGRESS: {
    label: 'In Progress',
    className: 'bg-amber-500/15 text-amber-400 border border-amber-500/20',
    icon: <PlayCircle className="w-3 h-3" />,
  },
  COMPLETED: {
    label: 'Completed',
    className: 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/20',
    icon: <CheckCircle2 className="w-3 h-3" />,
  },
  MISSED: {
    label: 'Missed',
    className: 'bg-red-500/15 text-red-400 border border-red-500/20',
    icon: <AlertCircle className="w-3 h-3" />,
  },
};

const DIFFICULTY_BADGE: Record<MockDifficulty, string> = {
  EASY:   'bg-emerald-500/15 text-emerald-400 border border-emerald-500/20',
  MEDIUM: 'bg-amber-500/15 text-amber-400 border border-amber-500/20',
  HARD:   'bg-red-500/15 text-red-400 border border-red-500/20',
};

function formatDuration(seconds: number): string {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  if (h === 0) return `${m}m`;
  if (m === 0) return `${h}h`;
  return `${h}h ${m}m`;
}

// ─── Enrolled Exams Section ───────────────────────────────────────────────────

function EnrolledExamsSection({
  enrollments,
  isLoading,
}: {
  enrollments: ExamEnrollment[];
  isLoading: boolean;
}) {
  const navigate = useNavigate();

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <ClipboardList className="w-5 h-5 text-brand-400" />
        <h2 className="font-semibold text-white">Enrolled Exams</h2>
      </div>

      <div className="glass rounded-2xl overflow-hidden">
        {isLoading ? (
          <div className="p-4 space-y-3">
            {Array.from({ length: 4 }).map((_, i) => (
              <Skeleton key={i} className="h-12 rounded-xl" />
            ))}
          </div>
        ) : enrollments.length === 0 ? (
          <div className="py-16 text-center">
            <BookOpen className="w-8 h-8 text-white/15 mx-auto mb-3" />
            <p className="text-white/30 text-sm">No exam enrollments yet.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-white/5">
                  <th className="text-left text-xs text-white/30 font-medium px-5 py-3 uppercase tracking-wider">
                    Exam
                  </th>
                  <th className="text-left text-xs text-white/30 font-medium px-5 py-3 uppercase tracking-wider hidden sm:table-cell">
                    Date
                  </th>
                  <th className="text-left text-xs text-white/30 font-medium px-5 py-3 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="text-left text-xs text-white/30 font-medium px-5 py-3 uppercase tracking-wider hidden md:table-cell">
                    Score
                  </th>
                  <th className="text-right text-xs text-white/30 font-medium px-5 py-3 uppercase tracking-wider">
                    Action
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/3">
                {enrollments.map((enr) => {
                  const statusInfo = STATUS_BADGE[enr.status];
                  return (
                    <tr key={enr.id} className="hover:bg-white/2 transition-colors">
                      <td className="px-5 py-3.5">
                        <div>
                          <p className="text-white text-sm font-medium">{enr.examName}</p>
                          <p className="text-white/40 text-xs mt-0.5">{enr.subject}</p>
                        </div>
                      </td>
                      <td className="px-5 py-3.5 hidden sm:table-cell">
                        <span className="text-white/50 text-sm">
                          {new Date(enr.scheduledDate).toLocaleDateString()}
                        </span>
                      </td>
                      <td className="px-5 py-3.5">
                        <span
                          className={cn(
                            'badge flex items-center gap-1.5 w-fit',
                            statusInfo.className
                          )}
                        >
                          {statusInfo.icon}
                          {statusInfo.label}
                        </span>
                      </td>
                      <td className="px-5 py-3.5 hidden md:table-cell">
                        {enr.score !== undefined ? (
                          <div className="flex items-center gap-2">
                            <span className="text-white font-medium text-sm">{enr.score}%</span>
                            {enr.percentile !== undefined && (
                              <span className="text-white/30 text-xs">P{enr.percentile}</span>
                            )}
                          </div>
                        ) : (
                          <span className="text-white/20 text-sm">—</span>
                        )}
                      </td>
                      <td className="px-5 py-3.5 text-right">
                        {enr.status === 'REGISTERED' || enr.status === 'IN_PROGRESS' ? (
                          <button
                            onClick={() => navigate(`/assessments/${enr.id}/exam`)}
                            className="text-xs btn-primary py-1.5 px-3 flex items-center gap-1 ml-auto"
                          >
                            {enr.status === 'IN_PROGRESS' ? 'Continue' : 'Start'}
                            <ChevronRight className="w-3 h-3" />
                          </button>
                        ) : enr.status === 'COMPLETED' ? (
                          <button
                            onClick={() => navigate(`/assessments/${enr.id}/results`)}
                            className="text-xs bg-emerald-500/15 text-emerald-400 border border-emerald-500/20 hover:bg-emerald-500/25 font-medium py-1.5 px-3 rounded-lg transition-all flex items-center gap-1 ml-auto"
                          >
                            Results
                            <ChevronRight className="w-3 h-3" />
                          </button>
                        ) : (
                          <span className="text-white/20 text-xs">—</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

// ─── Mock Tests Section ───────────────────────────────────────────────────────

function MockTestsSection({
  mockTests,
  isLoading,
}: {
  mockTests: MockTest[];
  isLoading: boolean;
}) {
  const navigate = useNavigate();

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <Layers className="w-5 h-5 text-brand-400" />
        <h2 className="font-semibold text-white">Mock Tests</h2>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {Array.from({ length: 3 }).map((_, i) => (
            <CardSkeleton key={i} />
          ))}
        </div>
      ) : mockTests.length === 0 ? (
        <div className="glass rounded-2xl py-12 text-center">
          <Layers className="w-8 h-8 text-white/15 mx-auto mb-3" />
          <p className="text-white/30 text-sm">No mock tests available.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {mockTests.map((mock) => {
            const bestScore =
              mock.attempts.length > 0
                ? Math.max(...mock.attempts.map((a) => a.score))
                : null;

            return (
              <motion.div
                key={mock.id}
                className="glass rounded-2xl p-5 flex flex-col gap-4"
                whileHover={{ y: -2 }}
                transition={{ type: 'spring', stiffness: 300, damping: 20 }}
              >
                {/* Header */}
                <div className="flex items-start justify-between gap-2">
                  <div className="flex-1 min-w-0">
                    <h3 className="text-white font-semibold text-sm line-clamp-1">{mock.name}</h3>
                    <p className="text-white/40 text-xs mt-0.5">{mock.subject}</p>
                  </div>
                  <span className={cn('badge flex-shrink-0', DIFFICULTY_BADGE[mock.difficulty])}>
                    {mock.difficulty}
                  </span>
                </div>

                {/* Meta */}
                <div className="flex items-center gap-4 text-xs text-white/40">
                  <span className="flex items-center gap-1.5">
                    <Clock className="w-3.5 h-3.5" />
                    {formatDuration(mock.durationSeconds)}
                  </span>
                  <span className="flex items-center gap-1.5">
                    <BookOpen className="w-3.5 h-3.5" />
                    {mock.questionCount} Qs
                  </span>
                </div>

                {/* Previous attempt scores */}
                {mock.attempts.length > 0 && (
                  <div className="flex flex-wrap gap-1.5">
                    {mock.attempts.slice(-4).map((attempt, i) => (
                      <span
                        key={attempt.id}
                        className={cn(
                          'badge text-xs',
                          attempt.score >= 80
                            ? 'bg-emerald-500/15 text-emerald-400'
                            : attempt.score >= 60
                            ? 'bg-brand-500/15 text-brand-400'
                            : 'bg-red-500/15 text-red-400'
                        )}
                      >
                        #{mock.attempts.length - (mock.attempts.slice(-4).length - 1 - i)}:{' '}
                        {attempt.score}%
                      </span>
                    ))}
                    {bestScore !== null && (
                      <span className="badge bg-white/5 text-white/30">Best: {bestScore}%</span>
                    )}
                  </div>
                )}

                {/* Start button */}
                <button
                  onClick={() => navigate(`/assessments/${mock.id}/exam`)}
                  className="w-full btn-primary py-2 text-sm flex items-center justify-center gap-1.5"
                >
                  <PlayCircle className="w-4 h-4" />
                  {mock.attempts.length > 0 ? 'Retake Mock' : 'Start Mock'}
                </button>
              </motion.div>
            );
          })}
        </div>
      )}
    </div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function ExamTrackerPage() {
  const user = useAuthStore((s) => s.user);
  const studentId = user?.id ?? '';

  const { data: enrollments = [], isLoading: enrollmentsLoading } = useQuery<ExamEnrollment[]>({
    queryKey: ['exam-enrollments', studentId],
    queryFn: async () => {
      try {
        const res = await api.get(
          `/api/v1/exam-tracker/students/${studentId}/enrollments`
        );
        return res.data;
      } catch {
        return [];
      }
    },
    enabled: !!studentId,
    retry: 1,
  });

  const { data: mockTests = [], isLoading: mocksLoading } = useQuery<MockTest[]>({
    queryKey: ['mock-tests', studentId],
    queryFn: async () => {
      try {
        const res = await api.get(
          `/api/v1/exam-tracker/students/${studentId}/mock-tests`
        );
        return res.data;
      } catch {
        return [];
      }
    },
    enabled: !!studentId,
    retry: 1,
  });

  // Compute quick stats
  const completed = enrollments.filter((e) => e.status === 'COMPLETED');
  const scores = completed.map((e) => e.score ?? 0).filter((s) => s > 0);
  const avgScore =
    scores.length > 0
      ? Math.round(scores.reduce((a, b) => a + b, 0) / scores.length)
      : 0;
  const bestScore = scores.length > 0 ? Math.max(...scores) : 0;

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
      className="p-6 space-y-8"
    >
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-white">Exam Tracker</h1>
        <p className="text-white/40 text-sm mt-0.5">
          Monitor enrollments and mock test performance
        </p>
      </div>

      {/* Quick stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {enrollmentsLoading ? (
          Array.from({ length: 4 }).map((_, i) => <StatCardSkeleton key={i} />)
        ) : (
          <>
            <StatCard
              title="Total Enrolled"
              value={enrollments.length}
              icon={<ClipboardList className="w-5 h-5 text-brand-400" />}
              iconBg="bg-brand-500/20"
            />
            <StatCard
              title="Completed"
              value={completed.length}
              icon={<CheckCircle2 className="w-5 h-5 text-emerald-400" />}
              iconBg="bg-emerald-500/20"
            />
            <StatCard
              title="Avg Score"
              value={avgScore > 0 ? `${avgScore}%` : '—'}
              icon={<BarChart3 className="w-5 h-5 text-amber-400" />}
              iconBg="bg-amber-500/20"
            />
            <StatCard
              title="Best Score"
              value={bestScore > 0 ? `${bestScore}%` : '—'}
              icon={<Trophy className="w-5 h-5 text-yellow-400" />}
              iconBg="bg-yellow-500/20"
            />
          </>
        )}
      </div>

      {/* Enrolled exams */}
      <EnrolledExamsSection
        enrollments={enrollments}
        isLoading={enrollmentsLoading}
      />

      {/* Mock tests */}
      <MockTestsSection mockTests={mockTests} isLoading={mocksLoading} />
    </motion.div>
  );
}
