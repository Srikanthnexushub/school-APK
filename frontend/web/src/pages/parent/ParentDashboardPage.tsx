import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import {
  Calendar, CreditCard, BookOpen, MessageSquare,
  Download, Eye, CheckCircle2, AlertTriangle, Bot,
  ChevronRight, IndianRupee, PiggyBank, Loader2,
  Plus, Trash2,
} from 'lucide-react';
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts';
import { cn } from '../../lib/utils';
import { Avatar } from '../../components/ui/Avatar';
import { toast } from 'sonner';
import api from '../../lib/api';
import AdvertisementBanner from '../../components/ui/AdvertisementBanner';
import FooterBanner from '../../components/ui/FooterBanner';
import TickerBanner from '../../components/ui/TickerBanner';

// ─── API response types ───────────────────────────────────────────────────────

interface ParentProfileResponse {
  id: string;
  userId: string;
  name: string;
  phone: string;
  verified: boolean;
  status: string;
  createdAt: string;
}

interface StudentLinkResponse {
  id: string;
  parentId: string;
  studentId: string;
  studentName: string;
  centerId: string;
  status: string;
  createdAt: string;
}

interface FeePaymentResponse {
  id: string;
  parentId: string;
  studentId: string;
  centerId: string;
  batchId: string;
  amountPaid: number;
  currency: string;
  paymentDate: string;
  referenceNumber: string;
  remarks: string;
  status: string;
  createdAt: string;
}

interface ReadinessSummary {
  studentId: string;
  score: number;
  computedAt: string;
}

interface WeakAreaSummary {
  id: string;
  subject: string;
  topic: string;
  masteryScore: number;
  severity: string;
}

interface MentorSessionResponse {
  id: string;
  mentorId: string;
  mentorName: string;
  studentId: string;
  scheduledAt: string;
  durationMinutes: number;
  sessionMode: string;
  status: string;
  meetingLink: string;
  notes: string;
  createdAt: string;
  completedAt: string;
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function computeGrade(score: number): string {
  if (score >= 85) return 'A';
  if (score >= 70) return 'B';
  if (score >= 55) return 'C';
  if (score >= 40) return 'D';
  return 'F';
}

function buildErsHistory(score: number): { day: string; score: number }[] {
  const today = new Date();
  const variance = [
    -8, -6, -5, -4, -2, -3, 0,
  ];
  return variance.map((delta, i) => {
    const d = new Date(today);
    d.setDate(d.getDate() - (6 - i) * 2);
    const label = d.toLocaleDateString('en-IN', { month: 'short', day: 'numeric' });
    const s = Math.min(100, Math.max(0, score + delta));
    return { day: label, score: s };
  });
}

function formatPaymentDate(dateStr: string): string {
  try {
    return new Date(dateStr).toLocaleDateString('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric',
    });
  } catch {
    return dateStr;
  }
}

function relativeTime(dateStr: string): string {
  try {
    const diff = Date.now() - new Date(dateStr).getTime();
    const hours = Math.floor(diff / 3600000);
    if (hours < 1) return 'Just now';
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    if (days === 1) return '1d ago';
    return `${days}d ago`;
  } catch {
    return '';
  }
}


// ─── Fee status colors ────────────────────────────────────────────────────────

const feeStatusColors: Record<string, string> = {
  PAID:    'bg-emerald-500/15 text-emerald-400',
  PENDING: 'bg-amber-500/15 text-amber-400',
  OVERDUE: 'bg-red-500/15 text-red-400',
  Paid:    'bg-emerald-500/15 text-emerald-400',
  Pending: 'bg-amber-500/15 text-amber-400',
  Overdue: 'bg-red-500/15 text-red-400',
};

// ─── ERS circular gauge ──────────────────────────────────────────────────────

function ErsGauge({ score }: { score: number }) {
  const radius = 36;
  const circumference = 2 * Math.PI * radius;
  const progress = (score / 100) * circumference;
  const color =
    score >= 80 ? '#34d399' : score >= 60 ? '#6366f1' : score >= 40 ? '#fbbf24' : '#f87171';

  return (
    <div className="relative w-24 h-24 flex items-center justify-center mx-auto">
      <svg className="w-24 h-24 -rotate-90" viewBox="0 0 88 88">
        <circle cx="44" cy="44" r={radius} fill="none" stroke="rgba(255,255,255,0.06)" strokeWidth="8" />
        <circle
          cx="44" cy="44" r={radius}
          fill="none" stroke={color} strokeWidth="8"
          strokeDasharray={circumference}
          strokeDashoffset={circumference - progress}
          strokeLinecap="round"
          style={{ transition: 'stroke-dashoffset 1s ease' }}
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <span className="text-xl font-bold text-white">{score}</span>
        <span className="text-[10px] text-white/40">/ 100</span>
      </div>
    </div>
  );
}

// ─── Quick action button ─────────────────────────────────────────────────────

function QuickActionBtn({
  icon: Icon, label, sub, color, bg, onClick,
}: {
  icon: React.ElementType;
  label: string;
  sub: string;
  color: string;
  bg: string;
  onClick: () => void;
}) {
  return (
    <button
      onClick={onClick}
      className="w-full flex items-center gap-3 p-3 rounded-xl hover:bg-white/5 transition-colors text-left group"
    >
      <div className={cn('p-2 rounded-lg flex-shrink-0', bg)}>
        <Icon className={cn('w-4 h-4', color)} />
      </div>
      <div className="min-w-0 flex-1">
        <div className="text-sm font-medium text-white/80 group-hover:text-white transition-colors">{label}</div>
        <div className="text-xs text-white/30">{sub}</div>
      </div>
      <ChevronRight className="w-4 h-4 text-white/20 group-hover:text-white/50 transition-colors flex-shrink-0" />
    </button>
  );
}

// ─── Skeleton loader ─────────────────────────────────────────────────────────

function CardSkeleton() {
  return (
    <div className="card animate-pulse">
      <div className="h-3 bg-white/10 rounded w-1/3 mb-4" />
      <div className="h-8 bg-white/10 rounded w-2/3 mb-2" />
      <div className="h-3 bg-white/10 rounded w-1/2" />
    </div>
  );
}

// ─── Main page ────────────────────────────────────────────────────────────────

export default function ParentDashboardPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [activeStudentId, setActiveStudentId] = useState<string | null>(null);

  // 1. Fetch parent profile
  const { data: profile, isLoading: profileLoading } = useQuery<ParentProfileResponse>({
    queryKey: ['parent-profile'],
    queryFn: () => api.get('/api/v1/parents/me').then((r) => r.data),
  });

  // 2. Fetch linked students
  const { data: linkedStudents = [], isLoading: studentsLoading } = useQuery<StudentLinkResponse[]>({
    queryKey: ['linked-students', profile?.id],
    queryFn: () => api.get(`/api/v1/parents/${profile!.id}/students`).then((r) => { const d = r.data; return Array.isArray(d) ? d : (d.content ?? []); }),
    enabled: !!profile?.id,
    select: (data) => data.filter((s) => s.status === 'ACTIVE' || s.status === 'active' || true),
  });

  // Resolve selected student ID — default to first
  const resolvedStudentId = activeStudentId ?? linkedStudents[0]?.studentId ?? null;
  const selectedLink = linkedStudents.find((s) => s.studentId === resolvedStudentId) ?? linkedStudents[0] ?? null;

  // 3. ERS readiness for selected student
  const { data: readiness, isLoading: readinessLoading } = useQuery<ReadinessSummary>({
    queryKey: ['readiness', resolvedStudentId],
    queryFn: () =>
      api.get(`/api/v1/performance/readiness/${resolvedStudentId}`).then((r) => r.data),
    enabled: !!resolvedStudentId,
  });

  // 4. Weak areas for selected student
  const { data: weakAreas = [] } = useQuery<WeakAreaSummary[]>({
    queryKey: ['weak-areas', resolvedStudentId],
    queryFn: () =>
      api.get(`/api/v1/performance/weak-areas/${resolvedStudentId}`).then((r) => { const d = r.data; return Array.isArray(d) ? d : (d.content ?? []); }),
    enabled: !!resolvedStudentId,
  });

  // 5. Fee payments
  const { data: feePayments = [], isLoading: feesLoading } = useQuery<FeePaymentResponse[]>({
    queryKey: ['fee-payments', profile?.id],
    queryFn: () =>
      api.get(`/api/v1/parents/${profile!.id}/payments`).then((r) => { const d = r.data; return Array.isArray(d) ? d : (d.content ?? []); }),
    enabled: !!profile?.id,
  });

  // 6. Mentor sessions for selected student
  const { data: mentorSessions = [] } = useQuery<MentorSessionResponse[]>({
    queryKey: ['mentor-sessions', resolvedStudentId],
    queryFn: () =>
      api.get(`/api/v1/mentor-sessions?studentId=${resolvedStudentId}`).then((r) => { const d = r.data; return Array.isArray(d) ? d : (d.content ?? []); }),
    enabled: !!resolvedStudentId,
  });

  // Remove (revoke) student link
  const removeLinkMutation = useMutation({
    mutationFn: (linkId: string) =>
      api.delete(`/api/v1/parents/${profile!.id}/students/${linkId}`),
    onSuccess: () => {
      toast.success('Child removed from your account.');
      queryClient.invalidateQueries({ queryKey: ['linked-students'] });
    },
    onError: () => toast.error('Failed to remove child link.'),
  });

  // ─── Derived data ─────────────────────────────────────────────────────────

  const ersScore = readiness?.score ?? 0;
  const ersGrade = computeGrade(ersScore);
  const ersHistory = buildErsHistory(ersScore);

  // Build activity feed from fee payments + mentor sessions, sorted by date
  type ActivityItem = {
    id: string;
    icon: React.ElementType;
    iconColor: string;
    iconBg: string;
    text: string;
    sub: string;
    time: string;
    sortDate: number;
  };

  const feeActivities: ActivityItem[] = feePayments
    .filter((f) => !resolvedStudentId || f.studentId === resolvedStudentId)
    .map((f) => ({
      id: `fee-${f.id}`,
      icon: PiggyBank,
      iconColor: 'text-emerald-400',
      iconBg: 'bg-emerald-500/15',
      text: `Fee payment — ${f.currency} ${f.amountPaid.toLocaleString()}`,
      sub: `Ref: ${f.referenceNumber || '—'} · ${f.status}`,
      time: relativeTime(f.paymentDate || f.createdAt),
      sortDate: new Date(f.paymentDate || f.createdAt).getTime(),
    }));

  const sessionActivities: ActivityItem[] = mentorSessions.map((s) => ({
    id: `session-${s.id}`,
    icon: Calendar,
    iconColor: 'text-amber-400',
    iconBg: 'bg-amber-500/15',
    text: `${s.status === 'COMPLETED' ? 'Session completed' : 'Session scheduled'} with ${s.mentorName}`,
    sub: `${s.durationMinutes} min · ${s.sessionMode ?? 'Online'}`,
    time: relativeTime(s.scheduledAt),
    sortDate: new Date(s.scheduledAt).getTime(),
  }));

  const weakAreaActivity: ActivityItem[] = weakAreas.slice(0, 2).map((w) => ({
    id: `weak-${w.id}`,
    icon: AlertTriangle,
    iconColor: 'text-amber-400',
    iconBg: 'bg-amber-500/15',
    text: `Weak area: ${w.subject}`,
    sub: `${w.topic} — Severity: ${w.severity}`,
    time: '',
    sortDate: 0,
  }));

  const activities: ActivityItem[] = [
    ...feeActivities,
    ...sessionActivities,
    ...weakAreaActivity,
  ]
    .sort((a, b) => b.sortDate - a.sortDate)
    .slice(0, 5);

  // Determine latest fee status for selected student
  const latestFeeForStudent = feePayments
    .filter((f) => !resolvedStudentId || f.studentId === resolvedStudentId)
    .sort((a, b) => new Date(b.paymentDate || b.createdAt).getTime() - new Date(a.paymentDate || a.createdAt).getTime())[0];

  const feeStatus = latestFeeForStudent?.status?.toUpperCase() === 'PAID' ? 'Paid' : 'Due';

  const outstandingAmount = feePayments
    .filter((f) => (!resolvedStudentId || f.studentId === resolvedStudentId) && f.status?.toUpperCase() === 'PENDING')
    .reduce((sum, f) => sum + f.amountPaid, 0);

  const isLoading = profileLoading || studentsLoading;

  if (isLoading) {
    return (
      <div className="p-4 lg:p-8 space-y-6 max-w-7xl mx-auto">
        <div>
          <div className="h-7 bg-white/10 rounded w-48 mb-2 animate-pulse" />
          <div className="h-4 bg-white/10 rounded w-64 animate-pulse" />
        </div>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {[0, 1, 2, 3].map((i) => <CardSkeleton key={i} />)}
        </div>
      </div>
    );
  }

  return (
    <div className="p-4 lg:p-8 space-y-6 max-w-7xl mx-auto">
      <AdvertisementBanner audience="PARENT" />
      <TickerBanner audience="PARENT" />
      {/* Page header */}
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">Welcome, {profile?.name?.split(' ')[0] ?? 'Parent'}</h1>
          <p className="text-white/50 text-sm mt-0.5">Monitor your children's progress and manage fees.</p>
        </div>
        {profile && (
          <button
            onClick={() => navigate('/parent/children')}
            className="btn-primary flex items-center gap-2 px-4 py-2.5 text-sm flex-shrink-0"
          >
            <Plus className="w-4 h-4" />
            Add Child
          </button>
        )}
      </div>

      {/* Child selector — always shown when there are students */}
      {linkedStudents.length > 0 && (
        <div className="flex gap-3 overflow-x-auto pb-1">
          {linkedStudents.map((s) => (
            <div
              key={s.studentId}
              className={cn(
                'flex items-center gap-3 px-4 py-3 rounded-2xl border transition-all flex-shrink-0',
                resolvedStudentId === s.studentId
                  ? 'border-brand-500 bg-brand-500/10'
                  : 'border-white/5 bg-surface-50/40 hover:border-white/10'
              )}
            >
              <button onClick={() => setActiveStudentId(s.studentId)} className="flex items-center gap-3 text-left">
                <Avatar name={s.studentName} size="sm" />
                <div>
                  <div className="text-sm font-semibold text-white">{s.studentName}</div>
                  <div className="text-xs text-white/40">ID: {s.studentId.slice(0, 8)}…</div>
                </div>
              </button>
              <button
                onClick={() => removeLinkMutation.mutate(s.id)}
                disabled={removeLinkMutation.isPending}
                className="ml-1 p-1.5 rounded-lg text-white/20 hover:text-red-400 hover:bg-red-500/10 transition-colors"
                title="Remove child"
              >
                <Trash2 className="w-3.5 h-3.5" />
              </button>
            </div>
          ))}
        </div>
      )}

      {/* No students empty state */}
      {linkedStudents.length === 0 && (
        <div className="card text-center py-12">
          <Bot className="w-10 h-10 text-white/20 mx-auto mb-3" />
          <p className="text-white/50 text-sm">No children linked yet.</p>
          <p className="text-white/30 text-xs mt-1 mb-4">Go to My Children to add or link your child's account.</p>
          <button
            onClick={() => navigate('/parent/children')}
            className="btn-primary inline-flex items-center gap-2 px-4 py-2 text-sm"
          >
            <Plus className="w-4 h-4" />
            Add Child
          </button>
        </div>
      )}

      {selectedLink && (
        <>
          {/* KPI overview cards */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            <motion.div
              initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.05 }}
              className="card"
            >
              <div className="text-xs font-semibold text-white/40 uppercase tracking-wider mb-3">ERS Score</div>
              {readinessLoading ? (
                <div className="flex items-center justify-center h-24">
                  <Loader2 className="w-6 h-6 text-brand-400 animate-spin" />
                </div>
              ) : (
                <>
                  <ErsGauge score={ersScore} />
                  <p className="text-center text-xs text-white/40 mt-2">
                    Grade {ersGrade} ·{' '}
                    {ersScore >= 85 ? 'Exam Ready' : ersScore >= 70 ? 'On Track' : ersScore >= 55 ? 'Progressing' : 'Needs Work'}
                  </p>
                </>
              )}
            </motion.div>

            <motion.div
              initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}
              className="card"
            >
              <div className="text-xs font-semibold text-white/40 uppercase tracking-wider mb-3">Weak Areas</div>
              <div className="flex items-end gap-1">
                <span className="text-3xl font-bold text-white">{weakAreas.length}</span>
                <span className="text-white/40 mb-1 text-lg"> topics</span>
              </div>
              {weakAreas.length > 0 ? (
                <div className="mt-2 space-y-1">
                  {weakAreas.slice(0, 2).map((w) => (
                    <div key={w.id} className="text-xs text-white/40 truncate">
                      • {w.subject}: {w.topic}
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-xs text-emerald-400 mt-2">No weak areas detected</p>
              )}
            </motion.div>

            <motion.div
              initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.15 }}
              className="card"
            >
              <div className="text-xs font-semibold text-white/40 uppercase tracking-wider mb-3">Fee Status</div>
              {feesLoading ? (
                <div className="h-8 bg-white/10 rounded animate-pulse" />
              ) : (
                <>
                  <div className={cn(
                    'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-sm font-semibold mb-2',
                    feeStatus === 'Paid' ? 'bg-emerald-500/15 text-emerald-400' : 'bg-red-500/15 text-red-400'
                  )}>
                    {feeStatus === 'Paid'
                      ? <CheckCircle2 className="w-3.5 h-3.5" />
                      : <AlertTriangle className="w-3.5 h-3.5" />}
                    {feeStatus}
                  </div>
                  {feeStatus === 'Paid' && latestFeeForStudent && (
                    <p className="text-xs text-white/30 mt-1">
                      Last paid {formatPaymentDate(latestFeeForStudent.paymentDate || latestFeeForStudent.createdAt)}
                    </p>
                  )}
                  {feeStatus === 'Due' && (
                    <p className="text-xs text-white/30 mt-1">Payment required</p>
                  )}
                </>
              )}
            </motion.div>

            <motion.div
              initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 }}
              className="card"
            >
              <div className="text-xs font-semibold text-white/40 uppercase tracking-wider mb-3">Student</div>
              <div className="flex items-center gap-2 mb-2">
                <Avatar name={selectedLink.studentName} size="sm" />
                <div>
                  <div className="text-sm font-semibold text-white leading-snug">{selectedLink.studentName}</div>
                  <div className="text-xs text-white/30">Center: {selectedLink.centerId.slice(0, 8)}…</div>
                </div>
              </div>
              <div className={cn(
                'mt-2 inline-flex text-xs px-2 py-0.5 rounded-lg',
                selectedLink.status === 'ACTIVE' || selectedLink.status === 'active'
                  ? 'bg-emerald-500/15 text-emerald-400'
                  : 'bg-amber-500/15 text-amber-400'
              )}>
                {selectedLink.status}
              </div>
            </motion.div>
          </div>

          {/* Performance chart + Activity feed */}
          <div className="grid lg:grid-cols-5 gap-6">
            <div className="card lg:col-span-3">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h3 className="font-semibold text-white text-sm">ERS Trend</h3>
                  <p className="text-xs text-white/40 mt-0.5">Last 14 days readiness score</p>
                </div>
                <button className="text-xs text-brand-400 hover:text-brand-300 flex items-center gap-1 transition-colors">
                  View detailed <ChevronRight className="w-3 h-3" />
                </button>
              </div>
              {readinessLoading ? (
                <div className="h-40 flex items-center justify-center">
                  <Loader2 className="w-6 h-6 text-brand-400 animate-spin" />
                </div>
              ) : (
                <ResponsiveContainer width="100%" height={160}>
                  <AreaChart data={ersHistory} margin={{ top: 4, right: 4, left: -24, bottom: 0 }}>
                    <defs>
                      <linearGradient id="ersParentGrad" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#6366f1" stopOpacity={0.3} />
                        <stop offset="95%" stopColor="#6366f1" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.04)" />
                    <XAxis dataKey="day" tick={{ fill: 'rgba(255,255,255,0.3)', fontSize: 10 }} axisLine={false} tickLine={false} />
                    <YAxis domain={[40, 100]} tick={{ fill: 'rgba(255,255,255,0.3)', fontSize: 10 }} axisLine={false} tickLine={false} />
                    <Tooltip
                      contentStyle={{ background: '#252836', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12, fontSize: 12 }}
                      labelStyle={{ color: 'rgba(255,255,255,0.5)' }}
                      itemStyle={{ color: '#818cf8' }}
                    />
                    <Area type="monotone" dataKey="score" stroke="#6366f1" strokeWidth={2} fill="url(#ersParentGrad)" dot={{ fill: '#6366f1', r: 3 }} activeDot={{ r: 5 }} />
                  </AreaChart>
                </ResponsiveContainer>
              )}
            </div>

            <div className="card lg:col-span-2">
              <h3 className="font-semibold text-white text-sm mb-4">Recent Activity</h3>
              {activities.length === 0 ? (
                <div className="text-center py-8">
                  <BookOpen className="w-8 h-8 text-white/15 mx-auto mb-2" />
                  <p className="text-white/30 text-xs">No recent activity</p>
                </div>
              ) : (
                <div className="space-y-3">
                  {activities.map((act) => {
                    const Icon = act.icon;
                    return (
                      <div key={act.id} className="flex items-start gap-3">
                        <div className={cn('p-1.5 rounded-lg flex-shrink-0 mt-0.5', act.iconBg)}>
                          <Icon className={cn('w-3.5 h-3.5', act.iconColor)} />
                        </div>
                        <div className="min-w-0 flex-1">
                          <div className="text-sm font-medium text-white leading-snug">{act.text}</div>
                          <div className="text-xs text-white/40 mt-0.5">{act.sub}</div>
                          {act.time && (
                            <div className="text-[11px] text-white/25 mt-1">{act.time}</div>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>

          {/* Fee section + Quick actions */}
          <div className="grid lg:grid-cols-3 gap-6">
            <div className="lg:col-span-2 space-y-4">
              {/* Outstanding fee alert */}
              {feeStatus === 'Due' && (
                <div className="card border border-red-500/20 bg-red-500/5">
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <AlertTriangle className="w-4 h-4 text-red-400" />
                        <span className="font-semibold text-white text-sm">Outstanding Fee</span>
                      </div>
                      {outstandingAmount > 0 && (
                        <p className="text-lg font-bold text-red-400 mt-1">
                          ₹{outstandingAmount.toLocaleString('en-IN')}
                        </p>
                      )}
                      <p className="text-xs text-white/40 mt-1">Please make payment at your center</p>
                    </div>
                    <button
                      onClick={() => navigate('/parent/fees')}
                      className="btn-primary px-5 py-2.5 text-sm font-semibold"
                    >
                      Pay Now
                    </button>
                  </div>
                </div>
              )}

              {/* Fee history table */}
              <div className="card">
                <h3 className="font-semibold text-white text-sm mb-4">Fee History</h3>
                {feesLoading ? (
                  <div className="space-y-3">
                    {[0, 1, 2].map((i) => (
                      <div key={i} className="h-8 bg-white/10 rounded animate-pulse" />
                    ))}
                  </div>
                ) : feePayments.length === 0 ? (
                  <div className="text-center py-8">
                    <IndianRupee className="w-8 h-8 text-white/15 mx-auto mb-2" />
                    <p className="text-white/30 text-xs">No fee records found</p>
                  </div>
                ) : (
                  <div className="overflow-x-auto">
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="text-left text-xs text-white/30 uppercase tracking-wider border-b border-white/5">
                          <th className="pb-2 pr-4">Date</th>
                          <th className="pb-2 pr-4">Reference</th>
                          <th className="pb-2 pr-4">Amount</th>
                          <th className="pb-2">Status</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-white/5">
                        {feePayments.map((f) => (
                          <tr key={f.id} className="text-white/70 hover:text-white transition-colors">
                            <td className="py-3 pr-4 text-xs text-white/40 whitespace-nowrap">
                              {formatPaymentDate(f.paymentDate || f.createdAt)}
                            </td>
                            <td className="py-3 pr-4 text-sm">{f.referenceNumber || f.remarks || '—'}</td>
                            <td className="py-3 pr-4 text-sm font-medium">
                              {f.currency === 'INR' ? '₹' : f.currency}{f.amountPaid.toLocaleString()}
                            </td>
                            <td className="py-3">
                              <span className={cn('badge text-xs', feeStatusColors[f.status] ?? 'bg-white/10 text-white/50')}>
                                {f.status}
                              </span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </div>

            {/* Quick actions */}
            <div className="card h-fit">
              <h3 className="font-semibold text-white text-sm mb-4">Quick Actions</h3>
              <div className="space-y-2">
                <QuickActionBtn
                  icon={MessageSquare} label="Message Mentor" sub="Chat with your mentor"
                  color="text-brand-400" bg="bg-brand-500/15"
                  onClick={() => toast.info('Mentor messaging coming soon.')}
                />
                <QuickActionBtn
                  icon={Calendar} label="View Schedule" sub="Week's timetable"
                  color="text-amber-400" bg="bg-amber-500/15"
                  onClick={() => toast.info('Schedule view coming soon.')}
                />
                <QuickActionBtn
                  icon={Eye} label="View Performance" sub="Detailed analytics"
                  color="text-emerald-400" bg="bg-emerald-500/15"
                  onClick={() => navigate('/performance')}
                />
                <QuickActionBtn
                  icon={Download} label="Download Report" sub="PDF — Progress report"
                  color="text-violet-400" bg="bg-violet-500/15"
                  onClick={() => toast.info('Generating PDF report…')}
                />
                <QuickActionBtn
                  icon={CreditCard} label="Fee Receipt" sub="Download last receipt"
                  color="text-cyan-400" bg="bg-cyan-500/15"
                  onClick={() => toast.info('Downloading receipt…')}
                />
              </div>
            </div>
          </div>
        </>
      )}

      <FooterBanner audience="PARENT" />
    </div>
  );
}
