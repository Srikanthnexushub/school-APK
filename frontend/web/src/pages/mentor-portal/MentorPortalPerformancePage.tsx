// src/pages/mentor-portal/MentorPortalPerformancePage.tsx
import { useState } from 'react';
import { motion } from 'framer-motion';
import { useQuery } from '@tanstack/react-query';
import {
  RadarChart, PolarGrid, PolarAngleAxis, Radar,
  ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, Cell,
} from 'recharts';
import {
  Award, TrendingUp, Users, CheckCircle2, XCircle,
  BookOpenCheck, BarChart3, ChevronDown, ChevronUp, Loader2,
} from 'lucide-react';
import api from '../../lib/api';
import { cn } from '../../lib/utils';
import { useAuthStore } from '../../stores/authStore';

// ─── Types ────────────────────────────────────────────────────────────────────

interface CenterResponse { id: string; name: string; }
interface ExamResponse   { id: string; title: string; status: string; totalMarks: number; passingMarks: number; }
interface GradeResponse  {
  id: string; submissionId: string; examId: string; studentId: string;
  batchId: string; centerId: string;
  percentage: number; letterGrade: string; passed: boolean; createdAt: string;
}

interface ExamPerformance {
  exam: ExamResponse;
  grades: GradeResponse[];
  passRate: number;
  avgPercentage: number;
  distribution: Record<string, number>;
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function letterGradeFrom(avg: number): string {
  if (avg >= 90) return 'A+';
  if (avg >= 80) return 'A';
  if (avg >= 70) return 'B';
  if (avg >= 60) return 'C';
  if (avg >= 50) return 'D';
  return 'F';
}

function gradeColor(g: string) {
  if (g === 'A+' || g === 'A') return 'text-emerald-400';
  if (g === 'B') return 'text-sky-400';
  if (g === 'C') return 'text-amber-400';
  if (g === 'D') return 'text-orange-400';
  return 'text-red-400';
}

const GRADE_COLORS: Record<string, string> = {
  'A+': '#34d399', A: '#34d399', B: '#38bdf8', C: '#fbbf24', D: '#fb923c', F: '#f87171',
};

// ─── Exam Row ─────────────────────────────────────────────────────────────────

function ExamRow({ ep }: { ep: ExamPerformance }) {
  const [open, setOpen] = useState(false);
  const grade = letterGradeFrom(ep.avgPercentage);
  const distData = Object.entries(ep.distribution).map(([g, count]) => ({ grade: g, count }));

  return (
    <div className="bg-surface-100/40 border border-white/5 rounded-xl overflow-hidden">
      <button
        className="w-full flex items-center gap-4 px-5 py-4 hover:bg-white/5 transition-colors text-left"
        onClick={() => setOpen(!open)}
      >
        <div className="flex-1 min-w-0">
          <p className="text-sm font-semibold text-white truncate">{ep.exam.title}</p>
          <p className="text-xs text-white/40 mt-0.5">{ep.grades.length} student{ep.grades.length !== 1 ? 's' : ''} graded</p>
        </div>
        <div className="hidden sm:flex items-center gap-6 text-sm">
          <div className="text-center">
            <p className="text-white/40 text-xs">Pass Rate</p>
            <p className={cn('font-semibold', ep.passRate >= 70 ? 'text-emerald-400' : ep.passRate >= 50 ? 'text-amber-400' : 'text-red-400')}>
              {ep.passRate.toFixed(0)}%
            </p>
          </div>
          <div className="text-center">
            <p className="text-white/40 text-xs">Avg Score</p>
            <p className="text-white font-semibold">{ep.avgPercentage.toFixed(1)}%</p>
          </div>
          <div className={cn('text-2xl font-bold w-10 text-center', gradeColor(grade))}>{grade}</div>
        </div>
        {open ? <ChevronUp className="w-4 h-4 text-white/30 flex-shrink-0" /> : <ChevronDown className="w-4 h-4 text-white/30 flex-shrink-0" />}
      </button>

      {open && (
        <div className="border-t border-white/5 px-5 py-4 grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Grade distribution chart */}
          <div>
            <p className="text-xs text-white/40 mb-3 font-medium uppercase tracking-wider">Grade Distribution</p>
            <ResponsiveContainer width="100%" height={140}>
              <BarChart data={distData} barCategoryGap="30%">
                <XAxis dataKey="grade" tick={{ fill: '#ffffff60', fontSize: 11 }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fill: '#ffffff40', fontSize: 11 }} axisLine={false} tickLine={false} width={24} />
                <Tooltip
                  contentStyle={{ background: '#1a1d2e', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 8, color: '#fff', fontSize: 12 }}
                  cursor={{ fill: 'rgba(255,255,255,0.04)' }}
                />
                <Bar dataKey="count" radius={[4, 4, 0, 0]}>
                  {distData.map((d) => (
                    <Cell key={d.grade} fill={GRADE_COLORS[d.grade] ?? '#6366f1'} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
          {/* Student list */}
          <div>
            <p className="text-xs text-white/40 mb-3 font-medium uppercase tracking-wider">Student Results</p>
            <div className="space-y-1.5 max-h-[140px] overflow-y-auto pr-1 custom-scrollbar">
              {ep.grades.slice(0, 20).map((g) => (
                <div key={g.id} className="flex items-center justify-between text-xs">
                  <span className="text-white/60 truncate max-w-[140px]">{g.studentId.substring(0, 8)}…</span>
                  <div className="flex items-center gap-2 flex-shrink-0">
                    <span className="text-white/50">{g.percentage.toFixed(1)}%</span>
                    <span className={cn('font-semibold w-6 text-right', gradeColor(g.letterGrade))}>{g.letterGrade}</span>
                    {g.passed
                      ? <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                      : <XCircle className="w-3.5 h-3.5 text-red-400" />}
                  </div>
                </div>
              ))}
              {ep.grades.length === 0 && <p className="text-white/30 text-xs">No grades recorded yet.</p>}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function MentorPortalPerformancePage() {
  const { user } = useAuthStore();

  // 1. Fetch centers
  const { data: centers = [], isLoading: centersLoading } = useQuery<CenterResponse[]>({
    queryKey: ['teacher-centers'],
    queryFn: async () => {
      const res = await api.get('/api/v1/centers');
      const d = res.data;
      return Array.isArray(d) ? d : (d.content ?? []);
    },
    enabled: !!user,
  });

  // 2. Fetch exams for each center
  const { data: exams = [], isLoading: examsLoading } = useQuery<ExamResponse[]>({
    queryKey: ['teacher-exams', centers.map(c => c.id).join(',')],
    queryFn: async () => {
      const all: ExamResponse[] = [];
      await Promise.all(centers.map(async (c) => {
        const res = await api.get(`/api/v1/exams?centerId=${c.id}`);
        const d = res.data;
        const items: ExamResponse[] = Array.isArray(d) ? d : (d.content ?? []);
        all.push(...items);
      }));
      return all;
    },
    enabled: centers.length > 0,
  });

  // 3. Fetch grades for each exam
  const { data: examPerformances = [], isLoading: gradesLoading } = useQuery<ExamPerformance[]>({
    queryKey: ['teacher-grades', exams.map(e => e.id).join(',')],
    queryFn: async () => {
      const results: ExamPerformance[] = [];
      await Promise.all(exams.map(async (exam) => {
        try {
          const res = await api.get(`/api/v1/exams/${exam.id}/grades`);
          const grades: GradeResponse[] = Array.isArray(res.data) ? res.data : [];
          if (grades.length === 0) {
            results.push({ exam, grades: [], passRate: 0, avgPercentage: 0, distribution: {} });
            return;
          }
          const passed = grades.filter(g => g.passed).length;
          const avgPercentage = grades.reduce((s, g) => s + g.percentage, 0) / grades.length;
          const passRate = (passed / grades.length) * 100;
          const distribution: Record<string, number> = {};
          grades.forEach(g => { distribution[g.letterGrade] = (distribution[g.letterGrade] ?? 0) + 1; });
          results.push({ exam, grades, passRate, avgPercentage, distribution });
        } catch {
          results.push({ exam, grades: [], passRate: 0, avgPercentage: 0, distribution: {} });
        }
      }));
      return results.sort((a, b) => b.grades.length - a.grades.length);
    },
    enabled: exams.length > 0,
  });

  const isLoading = centersLoading || examsLoading || gradesLoading;

  // ── Overall metrics ──────────────────────────────────────────────────────────
  const examsWithGrades = examPerformances.filter(ep => ep.grades.length > 0);
  const totalStudents = examPerformances.reduce((s, ep) => s + ep.grades.length, 0);
  const overallPassRate = examsWithGrades.length
    ? examsWithGrades.reduce((s, ep) => s + ep.passRate, 0) / examsWithGrades.length : 0;
  const overallAvg = examsWithGrades.length
    ? examsWithGrades.reduce((s, ep) => s + ep.avgPercentage, 0) / examsWithGrades.length : 0;
  const overallGrade = letterGradeFrom(overallAvg);

  // ── Radar data ───────────────────────────────────────────────────────────────
  const radarData = [
    { metric: 'Pass Rate',   value: overallPassRate },
    { metric: 'Avg Score',   value: overallAvg },
    { metric: 'Engagement',  value: Math.min(100, totalStudents * 5) },
    { metric: 'Consistency', value: examsWithGrades.length > 1
        ? 100 - Math.max(0, Math.max(...examsWithGrades.map(e => e.avgPercentage)) - Math.min(...examsWithGrades.map(e => e.avgPercentage)))
        : overallAvg },
    { metric: 'Coverage',    value: Math.min(100, exams.length * 10) },
  ];

  // ── Trend bar data ───────────────────────────────────────────────────────────
  const trendData = examPerformances
    .filter(ep => ep.grades.length > 0)
    .slice(0, 8)
    .map(ep => ({ name: ep.exam.title.substring(0, 12) + (ep.exam.title.length > 12 ? '…' : ''), avg: parseFloat(ep.avgPercentage.toFixed(1)) }));

  return (
    <div className="p-6 max-w-5xl mx-auto space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-white">My Performance</h1>
        <p className="text-sm text-white/40 mt-1">
          Teacher effectiveness index based on student exam outcomes
        </p>
      </div>

      {isLoading && (
        <div className="flex items-center gap-3 text-white/50 py-10 justify-center">
          <Loader2 className="w-5 h-5 animate-spin" />
          <span className="text-sm">Loading performance data…</span>
        </div>
      )}

      {!isLoading && (
        <>
          {/* Stat cards */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            {[
              { label: 'Overall Grade', value: overallGrade, sub: `${overallAvg.toFixed(1)}% avg`, Icon: Award, color: 'text-brand-400', big: true },
              { label: 'Pass Rate',     value: `${overallPassRate.toFixed(0)}%`, sub: 'across all exams', Icon: TrendingUp, color: overallPassRate >= 70 ? 'text-emerald-400' : 'text-amber-400', big: false },
              { label: 'Students',      value: String(totalStudents), sub: 'total graded',    Icon: Users,         color: 'text-sky-400',     big: false },
              { label: 'Exams',         value: String(exams.length),  sub: `${examsWithGrades.length} with grades`, Icon: BookOpenCheck, color: 'text-purple-400', big: false },
            ].map(({ label, value, sub, Icon, color, big }) => (
              <motion.div
                key={label}
                initial={{ opacity: 0, y: 12 }}
                animate={{ opacity: 1, y: 0 }}
                className="bg-surface-100/40 border border-white/5 rounded-xl p-4"
              >
                <div className="flex items-center justify-between mb-2">
                  <p className="text-xs text-white/40 font-medium">{label}</p>
                  <Icon className={cn('w-4 h-4', color)} />
                </div>
                <p className={cn('font-bold', big ? 'text-4xl' : 'text-2xl', color)}>{value}</p>
                <p className="text-xs text-white/30 mt-1">{sub}</p>
              </motion.div>
            ))}
          </div>

          {/* Charts */}
          {examsWithGrades.length > 0 && (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
              {/* Radar */}
              <div className="bg-surface-100/40 border border-white/5 rounded-xl p-5">
                <div className="flex items-center gap-2 mb-4">
                  <BarChart3 className="w-4 h-4 text-brand-400" />
                  <h2 className="text-sm font-semibold text-white">Teaching Effectiveness Radar</h2>
                </div>
                <ResponsiveContainer width="100%" height={200}>
                  <RadarChart data={radarData}>
                    <PolarGrid stroke="rgba(255,255,255,0.06)" />
                    <PolarAngleAxis dataKey="metric" tick={{ fill: '#ffffff60', fontSize: 11 }} />
                    <Radar dataKey="value" stroke="#6366f1" fill="#6366f1" fillOpacity={0.25} strokeWidth={2} />
                  </RadarChart>
                </ResponsiveContainer>
              </div>

              {/* Avg score trend */}
              <div className="bg-surface-100/40 border border-white/5 rounded-xl p-5">
                <div className="flex items-center gap-2 mb-4">
                  <TrendingUp className="w-4 h-4 text-emerald-400" />
                  <h2 className="text-sm font-semibold text-white">Avg Score by Exam</h2>
                </div>
                <ResponsiveContainer width="100%" height={200}>
                  <BarChart data={trendData} barCategoryGap="35%">
                    <XAxis dataKey="name" tick={{ fill: '#ffffff50', fontSize: 10 }} axisLine={false} tickLine={false} />
                    <YAxis domain={[0, 100]} tick={{ fill: '#ffffff40', fontSize: 10 }} axisLine={false} tickLine={false} width={28} />
                    <Tooltip
                      contentStyle={{ background: '#1a1d2e', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 8, color: '#fff', fontSize: 12 }}
                      cursor={{ fill: 'rgba(255,255,255,0.04)' }}
                      formatter={(v: number) => [`${v}%`, 'Avg']}
                    />
                    <Bar dataKey="avg" radius={[4, 4, 0, 0]}>
                      {trendData.map((d) => (
                        <Cell key={d.name} fill={d.avg >= 70 ? '#34d399' : d.avg >= 50 ? '#fbbf24' : '#f87171'} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
          )}

          {/* Per-exam breakdown */}
          <div>
            <h2 className="text-sm font-semibold text-white/70 mb-3 uppercase tracking-wider">Exam Breakdown</h2>
            {examPerformances.length === 0 ? (
              <div className="text-center py-12 text-white/30">
                <BookOpenCheck className="w-10 h-10 mx-auto mb-3 opacity-40" />
                <p>No exams found for your centers yet.</p>
              </div>
            ) : (
              <div className="space-y-2">
                {examPerformances.map((ep) => (
                  <ExamRow key={ep.exam.id} ep={ep} />
                ))}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
