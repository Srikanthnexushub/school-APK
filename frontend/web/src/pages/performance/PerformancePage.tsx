import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import {
  TrendingUp,
  TrendingDown,
  AlertTriangle,
  Zap,
  BrainCircuit,
  Target,
  RefreshCw,
  ArrowRight,
  Minus,
} from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { cn, getReadinessLabel, getRiskColor } from '../../lib/utils';
import { SubjectRadarChart } from '../../components/charts/RadarChart';
import { AreaTrendChart } from '../../components/charts/AreaTrendChart';
import { RingGauge } from '../../components/charts/RingGauge';
import { Skeleton, ChartSkeleton } from '../../components/ui/LoadingSkeleton';

// ─── Types ────────────────────────────────────────────────────────────────────

interface ReadinessScore {
  studentId: string;
  score: number;
  computedAt: string;
}

interface SubjectMastery {
  subject: string;
  score: number;
  trend: 'UP' | 'DOWN' | 'STABLE';
  lastUpdated: string;
}

interface WeakArea {
  id: string;
  subject: string;
  topic: string;
  masteryScore: number;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
}

interface DropoutRisk {
  studentId: string;
  riskScore: number;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  causalFactors: { factor: string; impact: number; icon: string }[];
  recommendation: string;
}

interface TrendPoint {
  date: string;
  score: number;
}

type TabKey = 'overview' | 'trends' | 'weak-areas' | 'dropout-risk';
type TrendDays = 30 | 60 | 90;

// ─── Helpers ─────────────────────────────────────────────────────────────────

const SEVERITY_STYLES: Record<string, string> = {
  LOW:      'bg-emerald-500/15 text-emerald-400 border border-emerald-500/20',
  MEDIUM:   'bg-amber-500/15 text-amber-400 border border-amber-500/20',
  HIGH:     'bg-orange-500/15 text-orange-400 border border-orange-500/20',
  CRITICAL: 'bg-red-500/15 text-red-400 border border-red-500/20',
};

const RISK_RING_COLOR: Record<string, string> = {
  LOW:      '#10b981',
  MEDIUM:   '#f59e0b',
  HIGH:     '#f97316',
  CRITICAL: '#ef4444',
};

// ─── ERS Gauge Card ───────────────────────────────────────────────────────────

function ERSCard({ data, isLoading }: { data?: ReadinessScore; isLoading: boolean }) {
  const readiness = data ? getReadinessLabel(data.score) : null;

  if (isLoading) {
    return (
      <div className="glass rounded-2xl p-6">
        <Skeleton className="h-5 w-40 mb-6" />
        <div className="flex items-center gap-8">
          <Skeleton className="w-32 h-32 rounded-full" />
          <div className="space-y-3 flex-1">
            <Skeleton className="h-6 w-28" />
            <Skeleton className="h-4 w-48" />
            <Skeleton className="h-3 w-36" />
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="glass rounded-2xl p-6">
      <div className="flex items-center gap-2 mb-6">
        <div className="w-7 h-7 rounded-lg bg-brand-500/20 border border-brand-500/30 flex items-center justify-center">
          <Target className="w-4 h-4 text-brand-400" />
        </div>
        <h2 className="font-semibold text-white">Exam Readiness Score</h2>
      </div>

      {data ? (
        <div className="flex flex-col sm:flex-row items-center gap-8">
          <RingGauge value={data.score} size="lg" label="ERS" />
          <div className="text-center sm:text-left">
            <div className={cn('text-2xl font-bold mb-1', readiness?.color)}>
              {readiness?.label}
            </div>
            <p className="text-white/40 text-sm">
              Your current exam readiness based on performance analytics
            </p>
            <p className="text-white/25 text-xs mt-2">
              Last computed: {new Date(data.computedAt).toLocaleString()}
            </p>
          </div>
        </div>
      ) : (
        <div className="text-center py-8 text-white/30 text-sm">
          No readiness data available yet.
        </div>
      )}
    </div>
  );
}

// ─── Overview Tab ─────────────────────────────────────────────────────────────

function OverviewTab({
  masteryData,
  isLoading,
}: {
  masteryData: SubjectMastery[];
  isLoading: boolean;
}) {
  const radarData = masteryData.map((m) => ({ subject: m.subject, score: m.score }));

  function TrendIcon({ trend }: { trend: string }) {
    if (trend === 'UP')   return <TrendingUp className="w-3.5 h-3.5 text-emerald-400" />;
    if (trend === 'DOWN') return <TrendingDown className="w-3.5 h-3.5 text-red-400" />;
    return <Minus className="w-3.5 h-3.5 text-white/30" />;
  }

  if (isLoading) {
    return (
      <div className="space-y-4">
        <ChartSkeleton height={280} />
        <Skeleton className="h-48 rounded-2xl" />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="glass rounded-2xl p-6">
        <h3 className="font-semibold text-white mb-4">Subject Mastery Overview</h3>
        {radarData.length > 0 ? (
          <SubjectRadarChart data={radarData} />
        ) : (
          <div className="h-40 flex items-center justify-center text-white/30 text-sm">
            No mastery data available.
          </div>
        )}
      </div>

      {masteryData.length > 0 && (
        <div className="glass rounded-2xl p-6">
          <h3 className="font-semibold text-white mb-4">Subject Breakdown</h3>
          <div className="space-y-3">
            {masteryData.map((m) => (
              <div key={m.subject} className="flex items-center gap-3">
                <span className="text-white/70 text-sm w-28 flex-shrink-0 truncate">{m.subject}</span>
                <div className="flex-1 h-2 bg-white/5 rounded-full overflow-hidden">
                  <motion.div
                    className="h-full bg-brand-500 rounded-full"
                    initial={{ width: 0 }}
                    animate={{ width: `${m.score}%` }}
                    transition={{ duration: 0.8, delay: 0.1 }}
                  />
                </div>
                <span className="text-white/60 text-xs w-10 text-right">{m.score}%</span>
                <TrendIcon trend={m.trend} />
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

// ─── Trends Tab ───────────────────────────────────────────────────────────────

function TrendsTab({ studentId }: { studentId: string }) {
  const [days, setDays] = useState<TrendDays>(30);

  const { data: trendData = [], isLoading } = useQuery<TrendPoint[]>({
    queryKey: ['performance-trend', studentId, days],
    queryFn: async () => {
      try {
        const res = await api.get(
          `/api/v1/performance/readiness/${studentId}/trend?days=${days}`
        );
        return res.data;
      } catch {
        return [];
      }
    },
    enabled: !!studentId,
    retry: 1,
  });

  const chartData = trendData.map((p) => ({
    date: new Date(p.date).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
    score: p.score,
  }));

  return (
    <div className="glass rounded-2xl p-6 space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="font-semibold text-white">ERS Trend</h3>
        <div className="flex items-center gap-1 bg-white/5 rounded-lg p-1">
          {([30, 60, 90] as TrendDays[]).map((d) => (
            <button
              key={d}
              onClick={() => setDays(d)}
              className={cn(
                'px-3 py-1 rounded-md text-xs font-medium transition-all',
                days === d ? 'bg-brand-600 text-white' : 'text-white/40 hover:text-white/70'
              )}
            >
              {d}d
            </button>
          ))}
        </div>
      </div>

      {isLoading ? (
        <Skeleton className="h-64 rounded-xl" />
      ) : chartData.length > 0 ? (
        <AreaTrendChart
          data={chartData}
          dataKey="score"
          color="#6366f1"
          height={260}
          yDomain={[0, 100]}
        />
      ) : (
        <div className="h-64 flex items-center justify-center text-white/30 text-sm">
          No trend data for this period.
        </div>
      )}
    </div>
  );
}

// ─── Weak Areas Tab ───────────────────────────────────────────────────────────

function WeakAreasTab({ studentId }: { studentId: string }) {
  const navigate = useNavigate();

  const { data: weakAreas = [], isLoading } = useQuery<WeakArea[]>({
    queryKey: ['weak-areas', studentId],
    queryFn: async () => {
      try {
        const res = await api.get(`/api/v1/performance/weak-areas/${studentId}`);
        return res.data;
      } catch {
        return [];
      }
    },
    enabled: !!studentId,
    retry: 1,
  });

  const studyPlanMutation = useMutation({
    mutationFn: () =>
      api.post(`/api/v1/ai-mentor/study-plan`, {
        studentId,
        weakAreaIds: weakAreas.map((w) => w.id),
      }),
    onSuccess: () => {
      toast.success('Study plan generated! Check AI Mentor.');
      navigate('/ai-mentor');
    },
    onError: () => toast.error('Failed to generate study plan.'),
  });

  const sorted = [...weakAreas].sort((a, b) => {
    const order: Record<string, number> = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 };
    return (order[a.severity] ?? 4) - (order[b.severity] ?? 4);
  });

  if (isLoading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 4 }).map((_, i) => (
          <Skeleton key={i} className="h-16 rounded-xl" />
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {sorted.length === 0 ? (
        <div className="glass rounded-2xl p-10 text-center text-white/30">
          No weak areas identified yet.
        </div>
      ) : (
        <>
          <div className="space-y-2">
            {sorted.map((area) => (
              <div key={area.id} className="glass rounded-xl p-4 flex items-center gap-4">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-white text-sm font-medium truncate">{area.subject}</span>
                    <span className="text-white/30 text-xs">·</span>
                    <span className="text-white/50 text-xs truncate">{area.topic}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="flex-1 h-1.5 bg-white/5 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-red-500 rounded-full"
                        style={{ width: `${area.masteryScore}%` }}
                      />
                    </div>
                    <span className="text-white/40 text-xs">{area.masteryScore}%</span>
                  </div>
                </div>
                <span className={cn('badge flex-shrink-0', SEVERITY_STYLES[area.severity])}>
                  {area.severity}
                </span>
              </div>
            ))}
          </div>

          <button
            onClick={() => studyPlanMutation.mutate()}
            disabled={studyPlanMutation.isPending}
            className="w-full btn-primary py-3 flex items-center justify-center gap-2 text-sm"
          >
            {studyPlanMutation.isPending ? (
              <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            ) : (
              <>
                <BrainCircuit className="w-4 h-4" />
                Generate Study Plan for Weak Areas
              </>
            )}
          </button>
        </>
      )}
    </div>
  );
}

// ─── Dropout Risk Tab ─────────────────────────────────────────────────────────

function DropoutRiskTab({ studentId }: { studentId: string }) {
  const navigate = useNavigate();

  const { data: risk, isLoading, isError, refetch } = useQuery<DropoutRisk>({
    queryKey: ['dropout-risk', studentId],
    queryFn: async () => {
      const res = await api.get(`/api/v1/performance/dropout-risk/${studentId}`);
      return res.data;
    },
    enabled: !!studentId,
    retry: 1,
  });

  if (isLoading) {
    return (
      <div className="glass rounded-2xl p-6 space-y-4">
        <div className="flex justify-center">
          <Skeleton className="w-32 h-32 rounded-full" />
        </div>
        <Skeleton className="h-4 w-40 mx-auto" />
        <Skeleton className="h-24 rounded-xl" />
      </div>
    );
  }

  if (isError || !risk) {
    return (
      <div className="glass rounded-2xl p-8 text-center">
        <AlertTriangle className="w-8 h-8 text-red-400 mx-auto mb-3" />
        <p className="text-white/60 text-sm mb-4">Failed to load risk data.</p>
        <button onClick={() => refetch()} className="btn-primary text-sm flex items-center gap-2 mx-auto">
          <RefreshCw className="w-3.5 h-3.5" />
          Retry
        </button>
      </div>
    );
  }

  const ringColor = RISK_RING_COLOR[risk.riskLevel] ?? '#6366f1';
  const riskTextColor = getRiskColor(risk.riskLevel);

  return (
    <div className="space-y-4">
      <div className="glass rounded-2xl p-6 text-center space-y-3">
        <div className="flex justify-center">
          <RingGauge value={risk.riskScore} color={ringColor} size="lg" label="Risk Score" />
        </div>
        <div className={cn('text-xl font-bold', riskTextColor)}>{risk.riskLevel} RISK</div>
        <p className="text-white/40 text-xs">
          Dropout probability index based on engagement and performance
        </p>
      </div>

      {risk.causalFactors?.length > 0 && (
        <div className="glass rounded-2xl p-6">
          <h4 className="text-white font-semibold text-sm mb-4">Contributing Factors</h4>
          <div className="space-y-3">
            {risk.causalFactors.map((f) => (
              <div key={f.factor} className="flex items-center gap-3">
                <span className="text-xl flex-shrink-0">{f.icon}</span>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-white/70 text-xs">{f.factor}</span>
                    <span className="text-white/40 text-xs">{f.impact}%</span>
                  </div>
                  <div className="h-1.5 bg-white/5 rounded-full overflow-hidden">
                    <motion.div
                      className="h-full rounded-full"
                      style={{ backgroundColor: ringColor }}
                      initial={{ width: 0 }}
                      animate={{ width: `${f.impact}%` }}
                      transition={{ duration: 0.8 }}
                    />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {risk.recommendation && (
        <div className="glass rounded-2xl p-5 border border-brand-500/15">
          <div className="flex items-start gap-3">
            <BrainCircuit className="w-4 h-4 text-brand-400 flex-shrink-0 mt-0.5" />
            <p className="text-white/60 text-sm leading-relaxed">{risk.recommendation}</p>
          </div>
        </div>
      )}

      <button
        onClick={() => navigate('/ai-mentor')}
        className="w-full btn-primary py-3 flex items-center justify-center gap-2 text-sm"
      >
        View Improvement Plan
        <ArrowRight className="w-4 h-4" />
      </button>
    </div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

const TABS: { key: TabKey; label: string; icon: React.ReactNode }[] = [
  { key: 'overview',     label: 'Overview',     icon: <Target className="w-4 h-4" /> },
  { key: 'trends',       label: 'Trends',       icon: <TrendingUp className="w-4 h-4" /> },
  { key: 'weak-areas',   label: 'Weak Areas',   icon: <Zap className="w-4 h-4" /> },
  { key: 'dropout-risk', label: 'Dropout Risk', icon: <AlertTriangle className="w-4 h-4" /> },
];

export default function PerformancePage() {
  const user = useAuthStore((s) => s.user);
  const studentId = user?.id ?? '';
  const [activeTab, setActiveTab] = useState<TabKey>('overview');

  const { data: readiness, isLoading: ersLoading } = useQuery<ReadinessScore>({
    queryKey: ['readiness', studentId],
    queryFn: async () => {
      const res = await api.get(`/api/v1/performance/readiness/${studentId}`);
      return res.data;
    },
    enabled: !!studentId,
    retry: 1,
  });

  const { data: masteryData = [], isLoading: masteryLoading } = useQuery<SubjectMastery[]>({
    queryKey: ['mastery', studentId],
    queryFn: async () => {
      try {
        const res = await api.get(`/api/v1/performance/mastery/${studentId}`);
        return res.data;
      } catch {
        return [];
      }
    },
    enabled: !!studentId,
    retry: 1,
  });

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
      className="p-6 space-y-6"
    >
      <div>
        <h1 className="text-2xl font-bold text-white">Performance Analytics</h1>
        <p className="text-white/40 text-sm mt-0.5">Track your learning progress and readiness</p>
      </div>

      <ERSCard data={readiness} isLoading={ersLoading} />

      {/* Tab bar */}
      <div className="flex items-center gap-1 bg-surface-50/60 border border-white/5 rounded-xl p-1">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={cn(
              'flex-1 flex items-center justify-center gap-1.5 py-2 px-3 rounded-lg text-sm font-medium transition-all duration-200',
              activeTab === tab.key
                ? 'bg-brand-600 text-white shadow-lg'
                : 'text-white/40 hover:text-white/70'
            )}
          >
            {tab.icon}
            <span className="hidden sm:inline">{tab.label}</span>
          </button>
        ))}
      </div>

      {/* Tab content */}
      <AnimatePresence mode="wait">
        <motion.div
          key={activeTab}
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -8 }}
          transition={{ duration: 0.2 }}
        >
          {activeTab === 'overview' && (
            <OverviewTab masteryData={masteryData} isLoading={masteryLoading} />
          )}
          {activeTab === 'trends' && <TrendsTab studentId={studentId} />}
          {activeTab === 'weak-areas' && <WeakAreasTab studentId={studentId} />}
          {activeTab === 'dropout-risk' && <DropoutRiskTab studentId={studentId} />}
        </motion.div>
      </AnimatePresence>
    </motion.div>
  );
}
