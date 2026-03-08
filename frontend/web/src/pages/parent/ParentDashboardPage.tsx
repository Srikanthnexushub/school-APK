import { useState } from 'react';
import { motion } from 'framer-motion';
import {
  Calendar, CreditCard, BookOpen, MessageSquare,
  Download, Eye, CheckCircle2, AlertTriangle, Bot,
  ChevronRight, IndianRupee,
} from 'lucide-react';
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts';
import { cn } from '../../lib/utils';
import { Avatar } from '../../components/ui/Avatar';
import { toast } from 'sonner';

// ─── Types & mock data ────────────────────────────────────────────────────────

interface Child {
  id: string;
  name: string;
  grade: string;
  stream: string;
  ersScore: number;
  attendance: number;
  feeStatus: 'Paid' | 'Due';
  feeDue?: number;
  feeDueDate?: string;
  studyPlan: string;
  planCompletion: number;
}

const children: Child[] = [
  {
    id: 'c1', name: 'Priya Sharma', grade: 'Grade 11', stream: 'Science',
    ersScore: 72, attendance: 88, feeStatus: 'Due', feeDue: 4500,
    feeDueDate: '15 Mar 2026', studyPlan: 'JEE 2026 Advanced', planCompletion: 61,
  },
  {
    id: 'c2', name: 'Arjun Sharma', grade: 'Grade 9', stream: 'Science',
    ersScore: 85, attendance: 94, feeStatus: 'Paid',
    studyPlan: 'Foundation Course', planCompletion: 78,
  },
];

const ersHistory = [
  { day: 'Feb 23', score: 64 }, { day: 'Feb 25', score: 67 },
  { day: 'Feb 27', score: 65 }, { day: 'Mar 1',  score: 69 },
  { day: 'Mar 3',  score: 70 }, { day: 'Mar 5',  score: 68 },
  { day: 'Mar 7',  score: 72 },
];

interface Activity {
  id: string;
  icon: React.ElementType;
  iconColor: string;
  iconBg: string;
  text: string;
  sub: string;
  time: string;
}

const activities: Activity[] = [
  {
    id: 'a1', icon: CheckCircle2, iconColor: 'text-emerald-400', iconBg: 'bg-emerald-500/15',
    text: 'Completed Mock Test 3', sub: 'Score: 72% — Physics', time: '2h ago',
  },
  {
    id: 'a2', icon: Bot, iconColor: 'text-brand-400', iconBg: 'bg-brand-500/15',
    text: 'Doubt resolved by AI Mentor', sub: 'Integration by parts — Maths', time: '5h ago',
  },
  {
    id: 'a3', icon: Calendar, iconColor: 'text-amber-400', iconBg: 'bg-amber-500/15',
    text: 'Session booked with Dr. Sharma', sub: 'Organic Chemistry — Tomorrow 4 PM', time: '1d ago',
  },
  {
    id: 'a4', icon: BookOpen, iconColor: 'text-violet-400', iconBg: 'bg-violet-500/15',
    text: 'Study plan updated', sub: 'JEE 2026 Advanced — Chapter 14 added', time: '2d ago',
  },
  {
    id: 'a5', icon: AlertTriangle, iconColor: 'text-amber-400', iconBg: 'bg-amber-500/15',
    text: 'Weak area detected', sub: 'Electrochemistry needs more practice', time: '3d ago',
  },
];

interface FeeRecord {
  id: string;
  date: string;
  amount: number;
  description: string;
  status: 'Paid' | 'Pending' | 'Overdue';
}

const feeHistory: FeeRecord[] = [
  { id: 'f1', date: '01 Feb 2026', amount: 4500, description: 'February Batch Fee', status: 'Paid' },
  { id: 'f2', date: '01 Jan 2026', amount: 4500, description: 'January Batch Fee', status: 'Paid' },
  { id: 'f3', date: '01 Dec 2025', amount: 4500, description: 'December Batch Fee', status: 'Paid' },
  { id: 'f4', date: '01 Nov 2025', amount: 2000, description: 'Study Material Fee', status: 'Paid' },
];

const feeStatusColors: Record<string, string> = {
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

// ─── Main page ────────────────────────────────────────────────────────────────

export default function ParentDashboardPage() {
  const [activeChildId, setActiveChildId] = useState(children[0].id);
  const child = children.find((c) => c.id === activeChildId) ?? children[0];

  return (
    <div className="p-4 lg:p-8 space-y-6 max-w-7xl mx-auto">
      {/* Page header */}
      <div>
        <h1 className="text-2xl font-bold text-white">Parent Dashboard</h1>
        <p className="text-white/50 text-sm mt-0.5">Monitor your children's progress and manage fees.</p>
      </div>

      {/* Child selector */}
      {children.length > 1 && (
        <div className="flex gap-3 overflow-x-auto pb-1">
          {children.map((c) => (
            <button
              key={c.id}
              onClick={() => setActiveChildId(c.id)}
              className={cn(
                'flex items-center gap-3 px-4 py-3 rounded-2xl border transition-all flex-shrink-0',
                activeChildId === c.id
                  ? 'border-brand-500 bg-brand-500/10'
                  : 'border-white/5 bg-surface-50/40 hover:border-white/10'
              )}
            >
              <Avatar name={c.name} size="sm" />
              <div className="text-left">
                <div className="text-sm font-semibold text-white">{c.name}</div>
                <div className="text-xs text-white/40">{c.grade} · {c.stream}</div>
              </div>
            </button>
          ))}
        </div>
      )}

      {/* KPI overview cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <motion.div
          initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.05 }}
          className="card"
        >
          <div className="text-xs font-semibold text-white/40 uppercase tracking-wider mb-3">ERS Score</div>
          <ErsGauge score={child.ersScore} />
          <p className="text-center text-xs text-white/40 mt-2">
            {child.ersScore >= 80 ? 'Exam Ready' : child.ersScore >= 60 ? 'On Track' : 'Needs Work'}
          </p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="card"
        >
          <div className="text-xs font-semibold text-white/40 uppercase tracking-wider mb-3">Attendance</div>
          <div className="flex items-end gap-1">
            <span className="text-3xl font-bold text-white">{child.attendance}</span>
            <span className="text-white/40 mb-1 text-lg">%</span>
          </div>
          <div className="mt-3 h-2 bg-white/5 rounded-full overflow-hidden">
            <motion.div
              initial={{ width: 0 }}
              animate={{ width: `${child.attendance}%` }}
              transition={{ duration: 0.8, ease: 'easeOut' }}
              className={cn('h-full rounded-full', child.attendance >= 85 ? 'bg-emerald-500' : 'bg-amber-500')}
            />
          </div>
          <p className="text-xs text-white/30 mt-2 flex items-center gap-1">
            <Calendar className="w-3 h-3" /> Last 30 days
          </p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.15 }}
          className="card"
        >
          <div className="text-xs font-semibold text-white/40 uppercase tracking-wider mb-3">Fee Status</div>
          <div className={cn(
            'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-sm font-semibold mb-2',
            child.feeStatus === 'Paid' ? 'bg-emerald-500/15 text-emerald-400' : 'bg-red-500/15 text-red-400'
          )}>
            {child.feeStatus === 'Paid'
              ? <CheckCircle2 className="w-3.5 h-3.5" />
              : <AlertTriangle className="w-3.5 h-3.5" />}
            {child.feeStatus}
          </div>
          {child.feeStatus === 'Due' && (
            <>
              <div className="flex items-center gap-1 text-white/70 font-semibold text-lg">
                <IndianRupee className="w-4 h-4" />{child.feeDue?.toLocaleString()}
              </div>
              <p className="text-xs text-white/30 mt-1">Due {child.feeDueDate}</p>
            </>
          )}
          {child.feeStatus === 'Paid' && (
            <p className="text-xs text-white/30 mt-1">March fee paid</p>
          )}
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="card"
        >
          <div className="text-xs font-semibold text-white/40 uppercase tracking-wider mb-3">Study Plan</div>
          <div className="text-sm font-semibold text-white leading-snug mb-2">{child.studyPlan}</div>
          <div className="text-2xl font-bold text-white">{child.planCompletion}%</div>
          <div className="mt-2 h-1.5 bg-white/5 rounded-full overflow-hidden">
            <motion.div
              initial={{ width: 0 }}
              animate={{ width: `${child.planCompletion}%` }}
              transition={{ duration: 0.8, ease: 'easeOut' }}
              className="h-full bg-brand-500 rounded-full"
            />
          </div>
          <p className="text-xs text-white/30 mt-1.5">completed</p>
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
        </div>

        <div className="card lg:col-span-2">
          <h3 className="font-semibold text-white text-sm mb-4">Recent Activity</h3>
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
                    <div className="text-[11px] text-white/25 mt-1">{act.time}</div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* Fee section + Quick actions */}
      <div className="grid lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-4">
          {/* Outstanding fee alert */}
          {child.feeStatus === 'Due' && (
            <div className="card border border-red-500/20 bg-red-500/5">
              <div className="flex items-center justify-between">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <AlertTriangle className="w-4 h-4 text-red-400" />
                    <span className="font-semibold text-white text-sm">Outstanding Fee</span>
                  </div>
                  <div className="flex items-center gap-1 text-2xl font-bold text-white">
                    <IndianRupee className="w-5 h-5" />{child.feeDue?.toLocaleString()}
                  </div>
                  <p className="text-xs text-white/40 mt-1">Due by {child.feeDueDate}</p>
                </div>
                <button
                  onClick={() => toast.success('Redirecting to payment gateway…')}
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
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-xs text-white/30 uppercase tracking-wider border-b border-white/5">
                    <th className="pb-2 pr-4">Date</th>
                    <th className="pb-2 pr-4">Description</th>
                    <th className="pb-2 pr-4">Amount</th>
                    <th className="pb-2">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5">
                  {feeHistory.map((f) => (
                    <tr key={f.id} className="text-white/70 hover:text-white transition-colors">
                      <td className="py-3 pr-4 text-xs text-white/40 whitespace-nowrap">{f.date}</td>
                      <td className="py-3 pr-4 text-sm">{f.description}</td>
                      <td className="py-3 pr-4 text-sm font-medium">₹{f.amount.toLocaleString()}</td>
                      <td className="py-3">
                        <span className={cn('badge text-xs', feeStatusColors[f.status])}>
                          {f.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        {/* Quick actions */}
        <div className="card h-fit">
          <h3 className="font-semibold text-white text-sm mb-4">Quick Actions</h3>
          <div className="space-y-2">
            <QuickActionBtn
              icon={MessageSquare} label="Message Mentor" sub="Chat with Dr. Sharma"
              color="text-brand-400" bg="bg-brand-500/15"
              onClick={() => toast.success('Opening messenger…')}
            />
            <QuickActionBtn
              icon={Calendar} label="View Schedule" sub="Week of Mar 10"
              color="text-amber-400" bg="bg-amber-500/15"
              onClick={() => toast.success('Opening schedule…')}
            />
            <QuickActionBtn
              icon={Eye} label="View Performance" sub="Detailed analytics"
              color="text-emerald-400" bg="bg-emerald-500/15"
              onClick={() => toast.success('Opening performance report…')}
            />
            <QuickActionBtn
              icon={Download} label="Download Report" sub="PDF — Progress report"
              color="text-violet-400" bg="bg-violet-500/15"
              onClick={() => toast.success('Generating PDF report…')}
            />
            <QuickActionBtn
              icon={CreditCard} label="Fee Receipt" sub="Download last receipt"
              color="text-cyan-400" bg="bg-cyan-500/15"
              onClick={() => toast.success('Downloading receipt…')}
            />
          </div>
        </div>
      </div>
    </div>
  );
}
