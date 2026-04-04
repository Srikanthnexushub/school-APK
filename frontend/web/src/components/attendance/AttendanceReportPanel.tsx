import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import {
  BarChart3, ChevronDown, ChevronUp, Download, Brain,
  AlertTriangle, Flame, Send, Loader2, X, TrendingUp, FileText,
} from 'lucide-react';
import { toast } from 'sonner';
import { LineChart, Line, ResponsiveContainer, Tooltip } from 'recharts';
import api from '../../lib/api';

// ─── Types ────────────────────────────────────────────────────────────────────

interface StudentBucketStat {
  studentId: string;
  studentName: string;
  totalSessions: number;
  present: number;
  absent: number;
  late: number;
  excused: number;
  attendancePercent: number;
  streak: number;
  atRisk: boolean;
}

interface AttendanceBucket {
  label: string;
  from: string;
  to: string;
  students: StudentBucketStat[];
  batchAveragePercent: number;
}

interface AttendancePeriodReport {
  batchId: string;
  batchName: string;
  period: string;
  from: string;
  to: string;
  buckets: AttendanceBucket[];
  atRiskStudentIds: string[];
}

interface AiAttendanceInsight {
  studentId: string;
  studentName: string;
  riskLevel: 'HIGH' | 'MEDIUM' | 'LOW' | 'NONE';
  insight: string;
  suggestedAction: string;
  predictedEomPercent: number;
  engagementScore: number;
  trend: 'IMPROVING' | 'DECLINING' | 'STABLE';
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function getFirstDayOfMonth(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`;
}

function getToday(): string {
  return new Date().toISOString().slice(0, 10);
}

function pctColor(pct: number): string {
  if (pct >= 85) return 'bg-emerald-500/20 text-emerald-400';
  if (pct >= 75) return 'bg-amber-500/20 text-amber-400';
  return 'bg-red-500/20 text-red-400';
}

const RISK_COLORS: Record<string, string> = {
  HIGH:   'bg-red-500/20 text-red-400 border-red-500/30',
  MEDIUM: 'bg-amber-500/20 text-amber-400 border-amber-500/30',
  LOW:    'bg-sky-500/20 text-sky-400 border-sky-500/30',
  NONE:   'bg-emerald-500/20 text-emerald-400 border-emerald-500/30',
};

// ─── Heatmap mini-bar ─────────────────────────────────────────────────────────

function HeatmapBar({ s }: { s: StudentBucketStat }) {
  const total = s.totalSessions || 1;
  const segments = [
    { pct: (s.present / total) * 100, color: 'bg-emerald-500' },
    { pct: (s.late    / total) * 100, color: 'bg-amber-400' },
    { pct: (s.excused / total) * 100, color: 'bg-sky-400' },
    { pct: (s.absent  / total) * 100, color: 'bg-red-500' },
  ];
  return (
    <div className="flex h-1.5 rounded-full overflow-hidden w-24 gap-px" title={`P:${s.present} L:${s.late} E:${s.excused} A:${s.absent}`}>
      {segments.map((seg, i) =>
        seg.pct > 0 ? (
          <div key={i} className={seg.color} style={{ width: `${seg.pct}%` }} />
        ) : null
      )}
    </div>
  );
}

// ─── Daily Heatmap Calendar (GitHub-style) ────────────────────────────────────

function DailyHeatmapCalendar({ buckets }: { buckets: AttendanceBucket[] }) {
  if (buckets.length === 0) return null;

  const dateMap = new Map<string, number>(
    buckets.map(b => [b.from, b.batchAveragePercent])
  );

  const firstDate = new Date(buckets[0].from);
  const lastDate  = new Date(buckets[buckets.length - 1].from);

  // Start from Monday of the week containing firstDate
  const start = new Date(firstDate);
  start.setDate(start.getDate() - ((start.getDay() + 6) % 7));

  // Build weeks array
  const weeks: Date[][] = [];
  const cur = new Date(start);
  while (true) {
    const week: Date[] = [];
    for (let d = 0; d < 7; d++) {
      week.push(new Date(cur));
      cur.setDate(cur.getDate() + 1);
    }
    weeks.push(week);
    if (cur > lastDate && cur.getDay() === 1) break;
  }

  // Month labels: track which weeks start a new month
  const monthLabels: { weekIndex: number; label: string }[] = [];
  weeks.forEach((week, i) => {
    if (i === 0 || week[0].getMonth() !== weeks[i - 1][0].getMonth()) {
      monthLabels.push({
        weekIndex: i,
        label: week[0].toLocaleString('default', { month: 'short' }) + ' ' + week[0].getFullYear(),
      });
    }
  });

  function cellColor(date: Date): string {
    const iso = date.toISOString().slice(0, 10);
    if (iso < buckets[0].from || iso > buckets[buckets.length - 1].from) return 'bg-black/5 dark:bg-white/5';
    if (!dateMap.has(iso)) return 'bg-black/5 dark:bg-white/5';
    const pct = dateMap.get(iso)!;
    if (pct === 0)  return 'bg-red-500/80';
    if (pct < 75)   return 'bg-amber-500/60';
    if (pct < 85)   return 'bg-emerald-400/60';
    return 'bg-emerald-500';
  }

  const DAY_LABELS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

  return (
    <div className="border border-cyan-500/20 rounded-2xl bg-surface-50/30 p-5 space-y-3">
      <div className="text-xs font-semibold text-white/60">Daily Attendance Heatmap</div>
      {/* Legend */}
      <div className="flex items-center gap-3 text-[10px] text-white/30 flex-wrap">
        <span>Less</span>
        {(['bg-black/5 dark:bg-white/5', 'bg-red-500/80', 'bg-amber-500/60', 'bg-emerald-400/60', 'bg-emerald-500'] as const).map((c, i) => (
          <span key={i} className={`w-3 h-3 rounded-sm ${c} inline-block`} />
        ))}
        <span>More</span>
        <span className="ml-2">· No data · 0% · &lt;75% · 75–84% · 85%+</span>
      </div>
      <div className="overflow-x-auto">
        <div className="inline-flex flex-col gap-1 min-w-max">
          {/* Month labels row */}
          <div className="flex gap-1 pl-8">
            {weeks.map((_, wi) => {
              const label = monthLabels.find(m => m.weekIndex === wi);
              return (
                <div key={wi} className="w-4 text-[9px] text-white/30 text-left truncate">
                  {label ? label.label : ''}
                </div>
              );
            })}
          </div>
          {/* Grid: 7 rows (days) × n columns (weeks) */}
          {DAY_LABELS.map((day, di) => (
            <div key={day} className="flex items-center gap-1">
              <span className="w-7 text-[9px] text-white/30 text-right">{day}</span>
              {weeks.map((week, wi) => {
                const date = week[di];
                const iso = date.toISOString().slice(0, 10);
                const pct = dateMap.get(iso);
                const label = date.toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' });
                return (
                  <div
                    key={wi}
                    className={`w-4 h-4 rounded-sm ${cellColor(date)} cursor-default transition-transform hover:scale-125`}
                    title={pct !== undefined ? `${label} — Avg ${pct.toFixed(1)}%` : `${label} — No data`}
                  />
                );
              })}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ─── Student Trends View ──────────────────────────────────────────────────────

function StudentTrendsView({ buckets }: { buckets: AttendanceBucket[] }) {
  if (buckets.length < 2) return (
    <div className="text-center py-8 text-white/30 text-sm">Need at least 2 periods to show trends.</div>
  );

  const studentIds = new Set<string>();
  const studentNames = new Map<string, string>();
  buckets.forEach(b => b.students.forEach(s => {
    studentIds.add(s.studentId);
    studentNames.set(s.studentId, s.studentName);
  }));

  const studentTrends = Array.from(studentIds).map(sid => {
    const data = buckets.map(b => {
      const stat = b.students.find(s => s.studentId === sid);
      return { label: b.label, pct: stat ? stat.attendancePercent : null };
    });
    const latest = data[data.length - 1]?.pct ?? 0;
    const first  = data[0]?.pct ?? 0;
    const trend = latest > first + 5 ? '↑' : latest < first - 5 ? '↓' : '→';
    const trendColor = trend === '↑' ? 'text-emerald-400' : trend === '↓' ? 'text-red-400' : 'text-white/40';
    return { studentId: sid, studentName: studentNames.get(sid) ?? sid, data, latest, trend, trendColor };
  });

  return (
    <div className="border border-indigo-500/20 rounded-2xl bg-surface-50/30 overflow-hidden">
      <div className="px-5 py-3.5 border-b border-white/5">
        <span className="text-sm font-semibold text-indigo-400">Student Attendance Trends</span>
        <span className="ml-2 text-xs text-white/30">across {buckets.length} periods</span>
      </div>
      <div className="divide-y divide-white/5">
        {studentTrends.map(st => (
          <div key={st.studentId} className="flex items-center gap-4 px-5 py-3">
            <div className="w-36 min-w-[9rem]">
              <div className="text-sm font-medium text-white truncate">{st.studentName}</div>
              <div className={`text-xs font-semibold ${st.trendColor}`}>
                {st.trend} {st.latest.toFixed(1)}%
              </div>
            </div>
            <div className="flex-1 h-8">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={st.data}>
                  <Line
                    type="monotone"
                    dataKey="pct"
                    stroke={st.latest >= 75 ? '#10b981' : '#ef4444'}
                    strokeWidth={1.5}
                    dot={false}
                    connectNulls
                  />
                  <Tooltip
                    contentStyle={{ background: '#1a1a2e', border: '1px solid rgba(34,211,238,0.2)', borderRadius: 8, fontSize: 10 }}
                    labelStyle={{ color: 'rgba(255,255,255,0.4)' }}
                    formatter={(v: number) => [v?.toFixed(1) + '%', 'Attendance']}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
            {/* Bucket values */}
            <div className="hidden lg:flex gap-2">
              {st.data.map((d, i) => (
                <div key={i} className="text-center">
                  <div className="text-[9px] text-white/20 truncate max-w-[40px]">{d.label.split(' ')[0]}</div>
                  <div className={`text-[10px] font-semibold ${d.pct !== null ? (d.pct >= 75 ? 'text-emerald-400' : 'text-red-400') : 'text-white/20'}`}>
                    {d.pct !== null ? d.pct.toFixed(0) + '%' : '—'}
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── Bucket card (collapsible) ────────────────────────────────────────────────

function BucketCard({ bucket, delay }: { bucket: AttendanceBucket; delay: number }) {
  const [open, setOpen] = useState(true);

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay }}
      className="border border-cyan-500/20 rounded-2xl overflow-hidden bg-surface-50/30"
    >
      {/* Bucket header */}
      <button
        onClick={() => setOpen(v => !v)}
        className="w-full flex items-center justify-between px-5 py-3.5 hover:bg-white/5 transition-colors"
      >
        <div className="flex items-center gap-3">
          <span className="text-sm font-semibold text-white">{bucket.label}</span>
          <span className="text-xs text-white/40">{bucket.from} → {bucket.to}</span>
          <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${pctColor(bucket.batchAveragePercent)}`}>
            Avg {bucket.batchAveragePercent.toFixed(1)}%
          </span>
        </div>
        {open
          ? <ChevronUp className="w-4 h-4 text-white/30" />
          : <ChevronDown className="w-4 h-4 text-white/30" />
        }
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="overflow-hidden"
          >
            <div className="border-t border-white/5 overflow-x-auto">
              {/* Table header */}
              <div className="grid grid-cols-[1fr_60px_60px_60px_60px_60px_90px_80px_60px] gap-x-2 px-5 py-2.5 text-[10px] text-white/30 font-medium uppercase tracking-wide border-b border-white/5 min-w-[700px]">
                <span>Student</span>
                <span className="text-right">Sessions</span>
                <span className="text-right">Present</span>
                <span className="text-right">Absent</span>
                <span className="text-right">Late</span>
                <span className="text-right">Excused</span>
                <span className="text-center">%</span>
                <span className="text-center">Heatmap</span>
                <span className="text-center">Streak</span>
              </div>
              <div className="divide-y divide-white/5 min-w-[700px]">
                {bucket.students.map((s, i) => (
                  <motion.div
                    key={s.studentId}
                    initial={{ opacity: 0, x: -4 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: i * 0.025 }}
                    className={`grid grid-cols-[1fr_60px_60px_60px_60px_60px_90px_80px_60px] gap-x-2 px-5 py-3 items-center ${
                      s.atRisk ? 'bg-red-500/5 border-l-2 border-red-500' : ''
                    }`}
                  >
                    {/* Name */}
                    <div className="min-w-0">
                      <div className="text-sm font-medium text-white truncate">{s.studentName}</div>
                      {s.atRisk && (
                        <div className="flex items-center gap-1 mt-0.5 text-[10px] text-red-400">
                          <AlertTriangle className="w-3 h-3" /> At risk
                        </div>
                      )}
                    </div>
                    {/* Sessions */}
                    <span className="text-right text-sm text-white/50">{s.totalSessions}</span>
                    {/* Present */}
                    <span className="text-right text-sm text-emerald-400">{s.present}</span>
                    {/* Absent */}
                    <span className="text-right text-sm text-red-400">{s.absent}</span>
                    {/* Late */}
                    <span className="text-right text-sm text-amber-400">{s.late}</span>
                    {/* Excused */}
                    <span className="text-right text-sm text-sky-400">{s.excused}</span>
                    {/* % pill */}
                    <div className="flex justify-center">
                      <span className={`text-xs px-2 py-0.5 rounded-full font-semibold ${pctColor(s.attendancePercent)}`}>
                        {s.attendancePercent.toFixed(1)}%
                      </span>
                    </div>
                    {/* Heatmap mini-bar */}
                    <div className="flex justify-center">
                      <HeatmapBar s={s} />
                    </div>
                    {/* Streak */}
                    <div className="flex justify-center">
                      {s.streak > 0 ? (
                        <span className="flex items-center gap-0.5 text-xs font-semibold text-orange-400 bg-orange-500/10 px-1.5 py-0.5 rounded-full">
                          <Flame className="w-3 h-3" /> {s.streak}
                        </span>
                      ) : (
                        <span className="text-xs text-white/20">—</span>
                      )}
                    </div>
                  </motion.div>
                ))}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}

// ─── AI Insights panel ────────────────────────────────────────────────────────

function AiInsightsPanel({
  insights,
  isLoading,
  onClose,
}: {
  insights?: AiAttendanceInsight[];
  isLoading: boolean;
  onClose: () => void;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: 12 }}
      className="border border-purple-500/30 rounded-2xl bg-surface-50/30 overflow-hidden"
    >
      {/* Header */}
      <div className="flex items-center justify-between px-5 py-3.5 border-b border-white/5">
        <div className="flex items-center gap-2">
          <div className="p-1.5 rounded-lg bg-purple-500/10">
            <Brain className="w-4 h-4 text-purple-400" />
          </div>
          <span className="text-sm font-semibold bg-gradient-to-r from-purple-400 to-cyan-400 bg-clip-text text-transparent">
            AI Attendance Insights
          </span>
        </div>
        <button
          onClick={onClose}
          className="p-1 rounded-lg hover:bg-white/10 text-white/30 hover:text-white/70 transition-colors"
        >
          <X className="w-3.5 h-3.5" />
        </button>
      </div>

      {/* Content */}
      <div className="p-5">
        {isLoading ? (
          <div className="flex flex-col items-center gap-3 py-8">
            <Loader2 className="w-6 h-6 text-purple-400 animate-spin" />
            <p className="text-sm text-white/40">Analysing attendance patterns…</p>
          </div>
        ) : !insights || insights.length === 0 ? (
          <div className="text-center py-8 text-white/30 text-sm">No insights available for this period.</div>
        ) : (
          <div className="space-y-3">
            {insights.map(ins => (
              <div
                key={ins.studentId}
                className={`rounded-xl border p-4 space-y-2 ${RISK_COLORS[ins.riskLevel] ?? RISK_COLORS.NONE}`}
              >
                <div className="flex items-center justify-between">
                  <span className="text-sm font-semibold text-white">{ins.studentName}</span>
                  <span className={`text-[10px] px-2 py-0.5 rounded-full border font-semibold ${RISK_COLORS[ins.riskLevel]}`}>
                    {ins.riskLevel} RISK
                  </span>
                </div>
                <p className="text-xs text-white/60 leading-relaxed">{ins.insight}</p>
                <div className="flex items-start gap-2 text-xs">
                  <span className="text-white/30 flex-shrink-0">Action:</span>
                  <span className="text-white/60">{ins.suggestedAction}</span>
                </div>
                <div className="flex items-center gap-1.5 text-xs text-white/40">
                  <BarChart3 className="w-3 h-3" />
                  Predicted EOM: <span className={`font-semibold ${pctColor(ins.predictedEomPercent).split(' ')[1]}`}>{ins.predictedEomPercent.toFixed(1)}%</span>
                </div>
                {/* Engagement score bar */}
                {ins.engagementScore !== undefined && (
                  <div className="space-y-1">
                    <div className="flex items-center justify-between text-[10px]">
                      <span className="text-white/40">Engagement Score</span>
                      <span className={`font-semibold ${
                        ins.engagementScore >= 70 ? 'text-emerald-400' :
                        ins.engagementScore >= 50 ? 'text-amber-400' : 'text-red-400'
                      }`}>{ins.engagementScore}/100</span>
                    </div>
                    <div className="h-1 bg-white/10 rounded-full overflow-hidden">
                      <div
                        className={`h-full rounded-full transition-all ${
                          ins.engagementScore >= 70 ? 'bg-emerald-500' :
                          ins.engagementScore >= 50 ? 'bg-amber-500' : 'bg-red-500'
                        }`}
                        style={{ width: `${ins.engagementScore}%` }}
                      />
                    </div>
                  </div>
                )}
                {/* Trend badge */}
                {ins.trend && (
                  <div className="flex items-center gap-1.5 text-xs">
                    <span className="text-white/30">Trend:</span>
                    <span className={`font-semibold ${
                      ins.trend === 'IMPROVING' ? 'text-emerald-400' :
                      ins.trend === 'DECLINING' ? 'text-red-400' : 'text-white/50'
                    }`}>
                      {ins.trend === 'IMPROVING' ? '↑ Improving' :
                       ins.trend === 'DECLINING' ? '↓ Declining' : '→ Stable'}
                    </span>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </motion.div>
  );
}

// ─── Main component ───────────────────────────────────────────────────────────

interface Props {
  centerId: string;
  batchId: string;
  batchName?: string;
}

type Period = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'CUSTOM';

export default function AttendanceReportPanel({ centerId, batchId, batchName }: Props) {
  const [period, setPeriod] = useState<Period>('MONTHLY');
  const [from, setFrom] = useState<string>(getFirstDayOfMonth());
  const [to, setTo] = useState<string>(getToday());
  const [showAiInsights, setShowAiInsights] = useState(false);
  const [showTrends, setShowTrends] = useState(false);
  const [sendingToParents, setSendingToParents] = useState(false);

  const { data: report, isLoading } = useQuery<AttendancePeriodReport>({
    queryKey: ['attendance-report', centerId, batchId, period, from, to],
    queryFn: () =>
      api.get(`/api/v1/centers/${centerId}/batches/${batchId}/attendance/report`, {
        params: { period, from, to },
      }).then(r => r.data),
    enabled: !!centerId && !!batchId,
    staleTime: 30_000,
  });

  const { data: aiInsightsRaw, isLoading: aiLoading } = useQuery<AiAttendanceInsight[]>({
    queryKey: ['attendance-ai-insights', centerId, batchId, from, to],
    queryFn: () =>
      api.get(`/api/v1/centers/${centerId}/batches/${batchId}/attendance/ai-insights`, {
        params: { from, to },
      }).then(r => r.data),
    enabled: showAiInsights && !!centerId && !!batchId,
    staleTime: 120_000,
    retry: false,
  });

  function handleExportCsv() {
    const url = `/api/v1/centers/${centerId}/batches/${batchId}/attendance/report/export?period=${period}&from=${from}&to=${to}`;
    api.get(url, { responseType: 'blob' })
      .then(response => {
        const blob = new Blob([response.data], { type: 'text/csv' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `attendance-report-${batchId}-${to}.csv`;
        link.click();
        URL.revokeObjectURL(link.href);
      })
      .catch(() => toast.error('Export failed'));
  }

  function handleExportPdf() {
    const url = `/api/v1/centers/${centerId}/batches/${batchId}/attendance/report/export?period=${period}&from=${from}&to=${to}&format=PDF`;
    api.get(url, { responseType: 'blob' })
      .then(response => {
        const blob = new Blob([response.data], { type: 'application/pdf' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `attendance-report-${batchId}-${to}.pdf`;
        link.click();
        URL.revokeObjectURL(link.href);
      })
      .catch(() => toast.error('PDF export failed'));
  }

  async function handleSendToParents() {
    if (sendingToParents) return;
    setSendingToParents(true);
    try {
      await api.post(
        `/api/v1/centers/${centerId}/batches/${batchId}/attendance/report/notify-parents`,
        null,
        { params: { from, to } }
      );
      toast.success('Report sent to parents via notification');
    } catch {
      toast.error('Failed to send report to parents');
    } finally {
      setSendingToParents(false);
    }
  }

  const buckets = report?.buckets ?? [];

  return (
    <div className="space-y-5">
      {/* Panel header */}
      <div className="flex items-center gap-3">
        <div className="p-2 rounded-xl bg-cyan-500/10 border border-cyan-500/20">
          <BarChart3 className="w-5 h-5 text-cyan-400" />
        </div>
        <div>
          <h2 className="text-base font-bold bg-gradient-to-r from-cyan-400 to-purple-400 bg-clip-text text-transparent">
            Attendance Reports
          </h2>
          {batchName && <p className="text-xs text-white/40">{batchName}</p>}
        </div>
      </div>

      {/* Filters row */}
      <div className="flex flex-wrap items-end gap-4">
        {/* Period dropdown */}
        <div className="space-y-1.5">
          <label className="text-[10px] font-medium text-white/40 uppercase tracking-wider">Period</label>
          <div className="relative">
            <select
              value={period}
              onChange={e => setPeriod(e.target.value as Period)}
              className="appearance-none bg-surface-100 border border-cyan-500/20 rounded-xl px-4 py-2.5 text-white text-sm focus:outline-none focus:border-cyan-500/50 pr-8 min-w-[140px]"
            >
              <option value="DAILY">Daily</option>
              <option value="WEEKLY">Weekly</option>
              <option value="MONTHLY">Monthly</option>
              <option value="QUARTERLY">Quarterly</option>
              <option value="CUSTOM">Custom</option>
            </select>
            <ChevronDown className="absolute right-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-white/40 pointer-events-none" />
          </div>
        </div>

        {/* From date */}
        <div className="space-y-1.5">
          <label className="text-[10px] font-medium text-white/40 uppercase tracking-wider">From</label>
          <input
            type="date"
            value={from}
            onChange={e => setFrom(e.target.value)}
            className="bg-surface-100 border border-cyan-500/20 rounded-xl px-4 py-2.5 text-white text-sm focus:outline-none focus:border-cyan-500/50"
          />
        </div>

        {/* To date */}
        <div className="space-y-1.5">
          <label className="text-[10px] font-medium text-white/40 uppercase tracking-wider">To</label>
          <input
            type="date"
            value={to}
            max={getToday()}
            onChange={e => setTo(e.target.value)}
            className="bg-surface-100 border border-cyan-500/20 rounded-xl px-4 py-2.5 text-white text-sm focus:outline-none focus:border-cyan-500/50"
          />
        </div>

        {/* Action buttons — pushed right */}
        <div className="flex items-center gap-2 ml-auto flex-wrap">
          <button
            onClick={handleExportCsv}
            className="flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-medium bg-cyan-500/10 border border-cyan-500/20 text-cyan-400 hover:bg-cyan-500/20 transition-colors"
          >
            <Download className="w-3.5 h-3.5" />
            Export CSV
          </button>
          <button
            onClick={handleExportPdf}
            className="flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-medium bg-rose-500/10 border border-rose-500/20 text-rose-400 hover:bg-rose-500/20 transition-colors"
          >
            <FileText className="w-3.5 h-3.5" />
            Export PDF
          </button>
          <button
            onClick={() => setShowAiInsights(v => !v)}
            className={`flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-medium border transition-colors ${
              showAiInsights
                ? 'bg-purple-500/20 border-purple-500/30 text-purple-300'
                : 'bg-purple-500/10 border-purple-500/20 text-purple-400 hover:bg-purple-500/20'
            }`}
          >
            <Brain className="w-3.5 h-3.5" />
            AI Insights
          </button>
          <button
            onClick={() => setShowTrends(v => !v)}
            className={`flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-medium border transition-colors ${
              showTrends
                ? 'bg-indigo-500/20 border-indigo-500/30 text-indigo-300'
                : 'bg-indigo-500/10 border-indigo-500/20 text-indigo-400 hover:bg-indigo-500/20'
            }`}
          >
            <TrendingUp className="w-3.5 h-3.5" />
            Trends
          </button>
          <button
            onClick={handleSendToParents}
            disabled={sendingToParents}
            className="flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-medium bg-teal-500/10 border border-teal-500/20 text-teal-400 hover:bg-teal-500/20 transition-colors disabled:opacity-50"
          >
            {sendingToParents ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Send className="w-3.5 h-3.5" />}
            {sendingToParents ? 'Sending…' : 'Send to Parents'}
          </button>
        </div>
      </div>

      {/* AI Insights panel */}
      <AnimatePresence>
        {showAiInsights && (
          <AiInsightsPanel
            insights={aiInsightsRaw}
            isLoading={aiLoading}
            onClose={() => setShowAiInsights(false)}
          />
        )}
      </AnimatePresence>

      {/* Report content */}
      {isLoading ? (
        <div className="flex flex-col items-center gap-3 py-16 text-white/40">
          <Loader2 className="w-8 h-8 animate-spin text-cyan-400" />
          <p className="text-sm">Loading report…</p>
        </div>
      ) : buckets.length === 0 ? (
        <div className="flex flex-col items-center gap-3 py-16 text-white/30">
          <BarChart3 className="w-10 h-10" />
          <p className="text-sm">No report data found for the selected period.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {/* Daily heatmap calendar — shown only for DAILY period */}
          {period === 'DAILY' && (
            <DailyHeatmapCalendar buckets={buckets} />
          )}

          {/* Student trends view */}
          {showTrends && buckets.length >= 2 && (
            <StudentTrendsView buckets={buckets} />
          )}

          {/* Heatmap legend */}
          <div className="flex items-center gap-4 text-[10px] text-white/30 flex-wrap">
            <span className="font-medium text-white/40">Heatmap:</span>
            {[
              { color: 'bg-emerald-500', label: 'Present' },
              { color: 'bg-amber-400',   label: 'Late' },
              { color: 'bg-sky-400',     label: 'Excused' },
              { color: 'bg-red-500',     label: 'Absent' },
            ].map(l => (
              <span key={l.label} className="flex items-center gap-1">
                <span className={`w-2.5 h-2.5 rounded-sm ${l.color}`} />
                {l.label}
              </span>
            ))}
          </div>

          {/* Buckets */}
          {buckets.map((bucket, i) => (
            <BucketCard key={bucket.label + bucket.from} bucket={bucket} delay={i * 0.05} />
          ))}
        </div>
      )}
    </div>
  );
}
