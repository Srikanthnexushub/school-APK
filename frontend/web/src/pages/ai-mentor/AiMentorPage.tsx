import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import {
  MessageSquare, BarChart2, Bot,
  Clock, Search, Send,
  TrendingUp, TrendingDown, Minus, BookOpen,
} from 'lucide-react';
import { ExportMenu } from '../../components/ui/ExportMenu';
import { toast } from 'sonner';
import api from '../../lib/api';
import { cn } from '../../lib/utils';
import { Badge } from '../../components/ui/Badge';
import { Skeleton } from '../../components/ui/LoadingSkeleton';
import { useAuthStore } from '../../stores/authStore';

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

// ─── Gap Analysis Types ──────────────────────────────────────────────────────

interface ErsComponent {
  score: number;
  weight: number;
  gap: number;
}

interface ErsBreakdown {
  total: number;
  syllabusCoverage: ErsComponent;
  mockTestTrend: ErsComponent;
  masteryAverage: ErsComponent;
  timeManagement: ErsComponent;
  accuracy: ErsComponent;
}

interface DropoutRisk {
  level: 'HIGH' | 'MEDIUM' | 'LOW' | 'UNKNOWN';
  score: number;
  topFactor: string;
}

interface SubjectGap {
  subject: string;
  masteryPercent: number;
  gapPoints: number;
  priority: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'ON_TRACK';
  trend: 'IMPROVING' | 'DECLINING' | 'FLAT';
  topWeakTopics: string[];
}

interface GapAnalysisData {
  ersBreakdown: ErsBreakdown;
  subjectGaps: SubjectGap[];
  dropoutRisk: DropoutRisk;
}

interface SyllabusVelocity {
  totalModules: number;
  completedModules: number;
  completionPercent: number;
  onTrack: boolean;
  behindByDays: number;
}

interface MockTestStats {
  count: number;
  latestScorePercent: number;
  trend: 'IMPROVING' | 'DECLINING' | 'FLAT';
}

interface VelocityEnrollment {
  enrollmentId: string;
  examName: string;
  examCode: string;
  daysRemaining: number;
  syllabus: SyllabusVelocity;
  mockTests: MockTestStats;
  studyHoursThisWeek: number;
}

interface VelocityData {
  enrollments: VelocityEnrollment[];
}

interface MentorCoverageData {
  totalCompletedSessions: number;
  totalStudyMinutes: number;
  daysSinceLastSession: number;
  coveredTopics: string[];
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

// ─── Gap Analysis Dashboard ──────────────────────────────────────────────────

type ErsComponentKey = 'syllabusCoverage' | 'mockTestTrend' | 'masteryAverage' | 'timeManagement' | 'accuracy';

const ERS_COMPONENT_KEYS: ErsComponentKey[] = [
  'syllabusCoverage', 'mockTestTrend', 'masteryAverage', 'timeManagement', 'accuracy',
];

const ERS_COMPONENT_LABELS: Record<ErsComponentKey, string> = {
  syllabusCoverage: 'Syllabus Coverage',
  mockTestTrend: 'Mock Test Trend',
  masteryAverage: 'Mastery Average',
  timeManagement: 'Time Management',
  accuracy: 'Accuracy',
};

function gapColor(gap: number): string {
  if (gap === 0) return 'text-emerald-400';
  if (gap < 20) return 'text-amber-400';
  return 'text-red-400';
}

function gapBarColor(gap: number): string {
  if (gap === 0) return 'bg-emerald-500';
  if (gap < 20) return 'bg-amber-500';
  return 'bg-red-500';
}

function priorityVariant(priority: SubjectGap['priority']): 'default' | 'info' | 'success' | 'warning' {
  if (priority === 'CRITICAL') return 'default';
  if (priority === 'HIGH') return 'warning';
  if (priority === 'MEDIUM') return 'info';
  return 'success';
}

function TrendArrow({ trend }: { trend: 'IMPROVING' | 'DECLINING' | 'FLAT' }) {
  if (trend === 'IMPROVING') return <TrendingUp className="w-4 h-4 text-emerald-400" />;
  if (trend === 'DECLINING') return <TrendingDown className="w-4 h-4 text-red-400" />;
  return <Minus className="w-4 h-4 text-white/40" />;
}

function GapAnalysisDashboard() {
  const user = useAuthStore((s) => s.user);
  const studentId = user?.id;

  const gapQuery = useQuery<GapAnalysisData>({
    queryKey: ['gap-analysis', studentId],
    queryFn: () => api.get(`/api/v1/performance/gap-analysis/${studentId}`).then((r) => r.data),
    enabled: !!studentId,
    staleTime: 5 * 60 * 1000,
  });

  const velocityQuery = useQuery<VelocityData>({
    queryKey: ['exam-velocity', studentId],
    queryFn: () => api.get(`/api/v1/exam-tracker/students/${studentId}/velocity`).then((r) => r.data),
    enabled: !!studentId,
    staleTime: 5 * 60 * 1000,
  });

  const mentorQuery = useQuery<MentorCoverageData>({
    queryKey: ['mentor-coverage', studentId],
    queryFn: () => api.get(`/api/v1/mentor-sessions/gap-coverage?studentId=${studentId}`).then((r) => r.data),
    enabled: !!studentId,
    staleTime: 5 * 60 * 1000,
  });

  const gap = gapQuery.data;
  const velocity = velocityQuery.data;
  const mentor = mentorQuery.data;

  return (
    <div className="space-y-6">

      {/* Panel 1: ERS Breakdown — full width */}
      <motion.div
        className="glass rounded-2xl p-5"
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
      >
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-white font-semibold">Exam Readiness Score (ERS)</h3>
          {gapQuery.isLoading ? (
            <Skeleton className="h-8 w-16" />
          ) : gap ? (
            <div className="flex items-center gap-3">
              <span className="text-3xl font-bold text-brand-400">{gap.ersBreakdown.total.toFixed(1)}</span>
              <Badge
                variant={
                  gap.dropoutRisk?.level === 'HIGH' ? 'default' : gap.dropoutRisk?.level === 'MEDIUM' ? 'warning' : 'success'
                }
              >
                {gap.dropoutRisk?.level ?? 'UNKNOWN'} RISK
              </Badge>
            </div>
          ) : null}
        </div>

        {gapQuery.isLoading ? (
          <div className="space-y-3">
            {[...Array(5)].map((_, i) => <Skeleton key={i} className="h-8 w-full" />)}
          </div>
        ) : !gap ? (
          <div className="flex flex-col items-center py-8 text-white/30 gap-2">
            <BarChart2 className="w-10 h-10 opacity-30" />
            <p className="text-sm">No ERS data available yet</p>
          </div>
        ) : (
          <div className="space-y-3">
            {ERS_COMPONENT_KEYS.map((key) => {
              const comp = gap.ersBreakdown[key];
              if (!comp) return null;
              const score = Math.round(comp.score);
              const gapVal = Math.round(comp.gap);
              return (
                <div key={key} className="flex items-center gap-3">
                  <span className="text-white/60 text-sm w-40 shrink-0">{ERS_COMPONENT_LABELS[key]}</span>
                  <div className="flex-1 h-2 rounded-full bg-white/10 overflow-hidden">
                    <div
                      className={cn('h-full rounded-full transition-all', gapBarColor(gapVal))}
                      style={{ width: `${score}%` }}
                    />
                  </div>
                  <span className="text-white/80 text-sm w-10 text-right shrink-0">{score}%</span>
                  {gapVal > 0 && (
                    <span className={cn('text-xs font-medium w-16 text-right shrink-0', gapColor(gapVal))}>
                      -{gapVal}% gap
                    </span>
                  )}
                  {gapVal === 0 && (
                    <span className="text-xs font-medium w-16 text-right shrink-0 text-emerald-400">
                      On track
                    </span>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </motion.div>

      {/* Panels 2 & 3: Subject Gaps + Exam Velocity — side by side on lg */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

        {/* Panel 2: Subject Gaps */}
        <motion.div
          className="glass rounded-2xl p-5"
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3, delay: 0.05 }}
        >
          <h3 className="text-white font-semibold mb-4">Subject Gaps</h3>

          {gapQuery.isLoading ? (
            <div className="space-y-4">
              {[...Array(4)].map((_, i) => <Skeleton key={i} className="h-20 w-full" />)}
            </div>
          ) : !gap || gap.subjectGaps.length === 0 ? (
            <div className="flex flex-col items-center py-8 text-white/30 gap-2">
              <BookOpen className="w-10 h-10 opacity-30" />
              <p className="text-sm">No subject gaps to display</p>
            </div>
          ) : (
            <div className="space-y-4">
              {gap.subjectGaps.map((sg) => (
                <div key={sg.subject} className="space-y-2">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-white/80 text-sm font-medium">{sg.subject}</span>
                    <Badge variant={priorityVariant(sg.priority)}>{sg.priority}</Badge>
                    <TrendArrow trend={sg.trend} />
                    <span className="ml-auto text-white/50 text-xs">{sg.masteryPercent}% mastery</span>
                  </div>
                  <div className="h-1.5 rounded-full bg-white/10 overflow-hidden">
                    <div
                      className={cn('h-full rounded-full', gapBarColor(sg.gapPoints))}
                      style={{ width: `${sg.masteryPercent}%` }}
                    />
                  </div>
                  {sg.topWeakTopics.length > 0 && (
                    <div className="flex flex-wrap gap-1 pt-0.5">
                      {sg.topWeakTopics.map((t) => (
                        <span
                          key={t}
                          className="text-xs px-2 py-0.5 rounded-full bg-white/5 text-white/40"
                        >
                          {t}
                        </span>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </motion.div>

        {/* Panel 3: Exam Velocity */}
        <motion.div
          className="glass rounded-2xl p-5"
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3, delay: 0.1 }}
        >
          <h3 className="text-white font-semibold mb-4">Exam Velocity</h3>

          {velocityQuery.isLoading ? (
            <div className="space-y-4">
              {[...Array(3)].map((_, i) => <Skeleton key={i} className="h-24 w-full" />)}
            </div>
          ) : !velocity || velocity.enrollments.length === 0 ? (
            <div className="flex flex-col items-center py-8 text-white/30 gap-2">
              <Clock className="w-10 h-10 opacity-30" />
              <p className="text-sm">No active exam enrollments</p>
            </div>
          ) : (
            <div className="space-y-4">
              {velocity.enrollments.map((en) => (
                <div key={en.examCode} className="space-y-2 pb-4 border-b border-white/5 last:border-0 last:pb-0">
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <p className="text-white/80 text-sm font-medium">{en.examName}</p>
                      <p className="text-white/40 text-xs">{en.examCode}</p>
                    </div>
                    <div className="flex flex-col items-end gap-1">
                      <Badge variant={en.syllabus?.onTrack ? 'success' : 'warning'}>
                        {en.syllabus?.onTrack ? 'On Track' : 'Behind'}
                      </Badge>
                      <span className="text-white/40 text-xs">{en.daysRemaining}d left</span>
                    </div>
                  </div>
                  <div className="space-y-1">
                    <div className="flex justify-between text-xs text-white/40">
                      <span>Syllabus</span>
                      <span>{en.syllabus?.completionPercent?.toFixed(0) ?? 0}%</span>
                    </div>
                    <div className="h-1.5 rounded-full bg-white/10 overflow-hidden">
                      <div
                        className="h-full rounded-full bg-brand-500 transition-all"
                        style={{ width: `${en.syllabus?.completionPercent ?? 0}%` }}
                      />
                    </div>
                  </div>
                  <div className="flex items-center gap-3 text-xs text-white/50">
                    <span>{en.mockTests?.count ?? 0} mocks</span>
                    {(en.mockTests?.latestScorePercent ?? 0) > 0 && (
                      <span className="flex items-center gap-1">
                        Latest: {en.mockTests.latestScorePercent.toFixed(0)}%
                        <TrendArrow trend={en.mockTests.trend} />
                      </span>
                    )}
                    {!en.syllabus?.onTrack && (en.syllabus?.behindByDays ?? 0) > 0 && (
                      <span className="text-amber-400 ml-auto">Behind by {en.syllabus.behindByDays} days</span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </motion.div>
      </div>

      {/* Panel 4: Mentor Coverage — full width */}
      <motion.div
        className="glass rounded-2xl p-5"
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, delay: 0.15 }}
      >
        <h3 className="text-white font-semibold mb-4">Mentor Coverage</h3>

        {mentorQuery.isLoading ? (
          <div className="space-y-3">
            <div className="flex gap-6">
              <Skeleton className="h-12 w-28" />
              <Skeleton className="h-12 w-28" />
              <Skeleton className="h-12 w-28" />
            </div>
            <Skeleton className="h-8 w-full" />
          </div>
        ) : !mentor ? (
          <div className="flex flex-col items-center py-8 text-white/30 gap-2">
            <Bot className="w-10 h-10 opacity-30" />
            <p className="text-sm">Mentor coverage data unavailable</p>
          </div>
        ) : mentor.totalCompletedSessions === 0 ? (
          <div className="flex flex-col items-center py-8 text-white/30 gap-2">
            <Bot className="w-10 h-10 opacity-30" />
            <p className="text-sm">No mentor sessions yet — consider booking one</p>
          </div>
        ) : (
          <div className="space-y-4">
            <div className="flex flex-wrap gap-6">
              <div>
                <p className="text-white/40 text-xs mb-1">Sessions Completed</p>
                <p className="text-2xl font-bold text-brand-400">{mentor.totalCompletedSessions}</p>
              </div>
              <div>
                <p className="text-white/40 text-xs mb-1">Study Minutes</p>
                <p className="text-2xl font-bold text-white/80">{mentor.totalStudyMinutes}</p>
              </div>
              <div>
                <p className="text-white/40 text-xs mb-1">Days Since Last Session</p>
                <p className={cn('text-2xl font-bold', mentor.daysSinceLastSession > 7 ? 'text-amber-400' : 'text-emerald-400')}>
                  {mentor.daysSinceLastSession}
                </p>
              </div>
            </div>

            {mentor.coveredTopics.length > 0 && (
              <div>
                <p className="text-white/40 text-xs mb-2">Covered Topics</p>
                <div className="flex flex-wrap gap-2">
                  {mentor.coveredTopics.map((topic) => (
                    <span
                      key={topic}
                      className="text-xs px-2.5 py-1 rounded-full bg-brand-500/15 text-brand-400 border border-brand-500/20"
                    >
                      {topic}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </motion.div>
    </div>
  );
}

// ─── Main Page ───────────────────────────────────────────────────────────────

type Tab = 'doubts' | 'gap-analysis';

const TABS: { id: Tab; label: string; icon: React.ReactNode }[] = [
  {
    id: 'doubts',
    label: 'Doubt Resolver',
    icon: <MessageSquare className="w-4 h-4" />,
  },
  {
    id: 'gap-analysis',
    label: 'Gap Analysis',
    icon: <BarChart2 className="w-4 h-4" />,
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
          {activeTab === 'gap-analysis' && <GapAnalysisDashboard />}
        </motion.div>
      </AnimatePresence>
    </motion.div>
  );
}
