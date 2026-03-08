import { motion } from 'framer-motion';
import {
  TrendingUp,
  Brain,
  Target,
  Zap,
  BookOpen,
  Clock,
  ChevronRight,
  AlertTriangle,
} from 'lucide-react';
import { useAuthStore } from '../../stores/authStore';
import { cn, getReadinessLabel, getRiskColor } from '../../lib/utils';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  RadarChart,
  PolarGrid,
  PolarAngleAxis,
  Radar,
} from 'recharts';

const performanceData = [
  { week: 'W1', score: 42 },
  { week: 'W2', score: 51 },
  { week: 'W3', score: 48 },
  { week: 'W4', score: 63 },
  { week: 'W5', score: 59 },
  { week: 'W6', score: 71 },
  { week: 'W7', score: 68 },
  { week: 'W8', score: 76 },
];

const subjectRadar = [
  { subject: 'Physics', score: 72 },
  { subject: 'Chemistry', score: 58 },
  { subject: 'Maths', score: 85 },
  { subject: 'Biology', score: 63 },
  { subject: 'English', score: 79 },
];

const weakAreas = [
  { topic: 'Organic Chemistry', subject: 'Chemistry', mastery: 34, risk: 'HIGH' },
  { topic: 'Wave Optics', subject: 'Physics', mastery: 41, risk: 'MEDIUM' },
  { topic: 'Coordinate Geometry', subject: 'Maths', mastery: 55, risk: 'MEDIUM' },
];

const upcomingExams = [
  { name: 'JEE Advanced Mock #4', date: '2026-03-15', daysLeft: 7 },
  { name: 'NEET Chapter Test', date: '2026-03-18', daysLeft: 10 },
  { name: 'Full Syllabus Test', date: '2026-03-25', daysLeft: 17 },
];

interface StatCardProps {
  label: string;
  value: string;
  sub?: string;
  Icon: React.ElementType;
  color: string;
  delay?: number;
}

function StatCard({ label, value, sub, Icon, color, delay = 0 }: StatCardProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay, duration: 0.4 }}
      className="card flex items-start gap-4"
    >
      <div className={cn('p-3 rounded-xl flex-shrink-0', color)}>
        <Icon className="w-5 h-5" />
      </div>
      <div>
        <div className="text-2xl font-bold text-white">{value}</div>
        <div className="text-white/50 text-sm mt-0.5">{label}</div>
        {sub && <div className="text-xs text-white/30 mt-1">{sub}</div>}
      </div>
    </motion.div>
  );
}

export default function StudentDashboardPage() {
  const user = useAuthStore((s) => s.user);
  const readinessScore = 68;
  const { label: readinessLabel, color: readinessColor } = getReadinessLabel(readinessScore);

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4 }}>
        <h1 className="text-2xl font-bold text-white">
          Good morning, {user?.name?.split(' ')[0] ?? 'Student'} 👋
        </h1>
        <p className="text-white/40 text-sm mt-1">Here&apos;s your study snapshot for today.</p>
      </motion.div>

      {/* Stat cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        <StatCard
          label="Exam Readiness"
          value={`${readinessScore}%`}
          sub={readinessLabel}
          Icon={Target}
          color="bg-brand-600/20 text-brand-400"
          delay={0.05}
        />
        <StatCard
          label="Streak"
          value="14 days"
          sub="Personal best: 21"
          Icon={Zap}
          color="bg-amber-500/20 text-amber-400"
          delay={0.1}
        />
        <StatCard
          label="Hours Studied"
          value="4.5 hrs"
          sub="Today's target: 6 hrs"
          Icon={Clock}
          color="bg-violet-500/20 text-violet-400"
          delay={0.15}
        />
        <StatCard
          label="Tests Completed"
          value="23"
          sub="This month"
          Icon={BookOpen}
          color="bg-emerald-500/20 text-emerald-400"
          delay={0.2}
        />
      </div>

      {/* Charts row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Performance trend */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.25, duration: 0.4 }}
          className="card lg:col-span-2"
        >
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="font-semibold text-white">Performance Trend</h3>
              <p className="text-white/40 text-xs mt-0.5">Weekly average score over 8 weeks</p>
            </div>
            <div className="flex items-center gap-1.5 text-emerald-400 text-sm font-medium">
              <TrendingUp className="w-4 h-4" />
              +34% growth
            </div>
          </div>
          <ResponsiveContainer width="100%" height={180}>
            <AreaChart data={performanceData}>
              <defs>
                <linearGradient id="scoreGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#6366f1" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#6366f1" stopOpacity={0} />
                </linearGradient>
              </defs>
              <XAxis dataKey="week" tick={{ fill: 'rgba(255,255,255,0.3)', fontSize: 11 }} axisLine={false} tickLine={false} />
              <YAxis domain={[0, 100]} tick={{ fill: 'rgba(255,255,255,0.3)', fontSize: 11 }} axisLine={false} tickLine={false} />
              <Tooltip
                contentStyle={{ background: '#1a1d27', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 8, color: '#fff', fontSize: 12 }}
                cursor={{ stroke: 'rgba(99,102,241,0.3)' }}
              />
              <Area type="monotone" dataKey="score" stroke="#6366f1" strokeWidth={2} fill="url(#scoreGrad)" dot={false} activeDot={{ r: 4, fill: '#6366f1' }} />
            </AreaChart>
          </ResponsiveContainer>
        </motion.div>

        {/* Subject radar */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3, duration: 0.4 }}
          className="card"
        >
          <h3 className="font-semibold text-white mb-1">Subject Mastery</h3>
          <p className="text-white/40 text-xs mb-3">Current proficiency levels</p>
          <ResponsiveContainer width="100%" height={180}>
            <RadarChart data={subjectRadar}>
              <PolarGrid stroke="rgba(255,255,255,0.06)" />
              <PolarAngleAxis dataKey="subject" tick={{ fill: 'rgba(255,255,255,0.4)', fontSize: 10 }} />
              <Radar dataKey="score" stroke="#6366f1" fill="#6366f1" fillOpacity={0.2} strokeWidth={2} />
            </RadarChart>
          </ResponsiveContainer>
        </motion.div>
      </div>

      {/* Bottom row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Weak areas */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.35, duration: 0.4 }}
          className="card"
        >
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-semibold text-white flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 text-amber-400" />
              Weak Areas
            </h3>
            <button className="text-brand-400 hover:text-brand-300 text-xs font-medium flex items-center gap-1 transition-colors">
              View all <ChevronRight className="w-3 h-3" />
            </button>
          </div>
          <div className="space-y-3">
            {weakAreas.map((area) => (
              <div key={area.topic} className="flex items-center gap-3">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-sm text-white truncate">{area.topic}</span>
                    <span className={cn('text-xs font-medium ml-2 flex-shrink-0', getRiskColor(area.risk))}>
                      {area.risk}
                    </span>
                  </div>
                  <div className="h-1.5 bg-surface-200 rounded-full overflow-hidden">
                    <motion.div
                      initial={{ width: 0 }}
                      animate={{ width: `${area.mastery}%` }}
                      transition={{ delay: 0.5, duration: 0.8 }}
                      className={cn(
                        'h-full rounded-full',
                        area.mastery < 40 ? 'bg-red-500' : area.mastery < 60 ? 'bg-amber-500' : 'bg-emerald-500'
                      )}
                    />
                  </div>
                  <span className="text-xs text-white/30 mt-0.5">{area.subject}</span>
                </div>
                <span className="text-sm font-mono font-semibold text-white/60 flex-shrink-0">{area.mastery}%</span>
              </div>
            ))}
          </div>
        </motion.div>

        {/* Upcoming exams */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4, duration: 0.4 }}
          className="card"
        >
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-semibold text-white flex items-center gap-2">
              <Brain className="w-4 h-4 text-violet-400" />
              Upcoming Exams
            </h3>
            <button className="text-brand-400 hover:text-brand-300 text-xs font-medium flex items-center gap-1 transition-colors">
              Tracker <ChevronRight className="w-3 h-3" />
            </button>
          </div>
          <div className="space-y-3">
            {upcomingExams.map((exam) => (
              <div key={exam.name} className="flex items-center gap-4 p-3 rounded-xl bg-surface-100/50 hover:bg-surface-100 transition-colors">
                <div className="text-center flex-shrink-0">
                  <div className="text-xl font-bold font-mono text-white">{exam.daysLeft}</div>
                  <div className="text-[10px] text-white/30 uppercase tracking-wide">days</div>
                </div>
                <div className="w-px h-8 bg-white/10 flex-shrink-0" />
                <div className="min-w-0 flex-1">
                  <div className="text-sm font-medium text-white truncate">{exam.name}</div>
                  <div className="text-xs text-white/30 mt-0.5">{exam.date}</div>
                </div>
                <div
                  className={cn(
                    'w-2 h-2 rounded-full flex-shrink-0',
                    exam.daysLeft <= 7 ? 'bg-red-400' : exam.daysLeft <= 14 ? 'bg-amber-400' : 'bg-emerald-400'
                  )}
                />
              </div>
            ))}
          </div>
        </motion.div>
      </div>

      {/* Readiness banner */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.45, duration: 0.4 }}
        className="card bg-gradient-to-r from-brand-950/80 to-violet-950/50 border border-brand-500/20"
      >
        <div className="flex items-center justify-between flex-wrap gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className="text-white/60 text-sm">Exam Readiness Score</span>
              <span className={cn('font-semibold text-sm', readinessColor)}>{readinessLabel}</span>
            </div>
            <div className="text-4xl font-bold font-mono text-white">
              {readinessScore}
              <span className="text-lg text-white/30">/100</span>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <button className="btn-primary px-5 py-2.5 text-sm flex items-center gap-2">
              AI Study Plan <ChevronRight className="w-4 h-4" />
            </button>
            <button className="btn-ghost text-sm">View Details</button>
          </div>
        </div>
      </motion.div>
    </div>
  );
}
