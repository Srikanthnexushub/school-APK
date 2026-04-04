import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useQuery, useMutation } from '@tanstack/react-query';
import {
  BarChart, Bar, XAxis, YAxis, Cell, Tooltip, ResponsiveContainer,
  RadarChart, PolarGrid, PolarAngleAxis, Radar,
} from 'recharts';
import {
  Sparkles, Zap, Share2, ChevronDown, ChevronUp,
  Wrench, FlaskConical, Palette, Briefcase, Heart, BookOpen,
  TrendingUp, TrendingDown, Minus, ExternalLink, Brain,
  Scale, Users, BarChart2, AlertTriangle, Target,
} from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { cn } from '../../lib/utils';

// ─── Backend response shapes ────────────────────────────────────────────────

interface CareerProfile {
  id: string;
  studentId: string;
  enrollmentId: string;
  academicStream: string;
  currentGrade: number;
  ersScore: number | null;
  preferredCareerStream: string | null;
  createdAt: string;
  updatedAt: string;
}

interface BackendRecommendation {
  id: string;
  studentId: string;
  careerStream: string;
  fitScore: number;
  confidenceLevel: string;
  rationale: string;
  rankOrder: number;
  generatedAt: string;
  validUntil: string;
  isActive: boolean;
}

// ─── Gap Analysis types (subset for Career context) ─────────────────────────

interface ErsComponent { score: number; weight: number; gap: number; }
interface ErsBreakdown {
  total: number;
  syllabusCoverage: ErsComponent;
  mockTestTrend: ErsComponent;
  masteryAverage: ErsComponent;
  timeManagement: ErsComponent;
  accuracy: ErsComponent;
}
interface SubjectGap {
  subject: string;
  gapPoints: number;
  priority: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'ON_TRACK';
  topWeakTopics: string[];
}
interface RiskSummary { level: 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL'; score: number; topFactor: string; }
interface GapAnalysis { ersBreakdown: ErsBreakdown; subjectGaps: SubjectGap[]; riskSummary: RiskSummary; }

// ─── UI shape (mapped from backend) ─────────────────────────────────────────

interface CareerRecommendation {
  stream: string;        // CareerStream enum key
  displayName: string;   // human-readable label
  score: number;
  description: string;
  requiredSkills: string[];
  salaryRange: string;
}

// ─── Static maps keyed by CareerStream enum values ──────────────────────────

const STREAM_DISPLAY: Record<string, string> = {
  ENGINEERING:    'Engineering',
  MEDICAL:        'Healthcare',
  COMMERCE:       'Commerce',
  ARTS:           'Arts & Humanities',
  LAW:            'Law',
  CIVIL_SERVICES: 'Civil Services',
  MANAGEMENT:     'Management',
  RESEARCH:       'Research',
};

const SALARY_MAP: Record<string, string> = {
  ENGINEERING:    '₹8L – ₹40L / yr',
  MEDICAL:        '₹6L – ₹35L / yr',
  COMMERCE:       '₹5L – ₹25L / yr',
  ARTS:           '₹4L – ₹20L / yr',
  LAW:            '₹6L – ₹30L / yr',
  CIVIL_SERVICES: '₹7L – ₹18L / yr',
  MANAGEMENT:     '₹7L – ₹35L / yr',
  RESEARCH:       '₹5L – ₹25L / yr',
};

const SKILLS_MAP: Record<string, string[]> = {
  ENGINEERING:    ['Mathematics', 'Physics', 'Problem Solving', 'Programming'],
  MEDICAL:        ['Biology', 'Chemistry', 'Empathy', 'Attention to Detail'],
  COMMERCE:       ['Economics', 'Accounting', 'Finance', 'Mathematics'],
  ARTS:           ['Creativity', 'Communication', 'Design', 'Storytelling'],
  LAW:            ['Critical Thinking', 'Research', 'Communication', 'Ethics'],
  CIVIL_SERVICES: ['General Knowledge', 'Ethics', 'Leadership', 'Analysis'],
  MANAGEMENT:     ['Leadership', 'Strategy', 'Finance', 'Marketing'],
  RESEARCH:       ['Scientific Method', 'Data Analysis', 'Critical Thinking', 'Statistics'],
};

const CAREER_ROLES: Record<string, string[]> = {
  ENGINEERING:    ['Software Engineer', 'Civil Engineer', 'Mechanical Engineer'],
  MEDICAL:        ['Doctor', 'Pharmacist', 'Clinical Researcher'],
  COMMERCE:       ['Chartered Accountant', 'Financial Analyst', 'Investment Banker'],
  ARTS:           ['Graphic Designer', 'Content Creator', 'UX Designer'],
  LAW:            ['Advocate', 'Legal Consultant', 'Judge'],
  CIVIL_SERVICES: ['IAS Officer', 'IPS Officer', 'IFS Officer'],
  MANAGEMENT:     ['Product Manager', 'Marketing Lead', 'Business Consultant'],
  RESEARCH:       ['Research Scientist', 'Data Scientist', 'Biochemist'],
};

const DOMAIN_ICONS: Record<string, React.ReactNode> = {
  ENGINEERING:    <Wrench className="w-5 h-5" />,
  MEDICAL:        <Heart className="w-5 h-5" />,
  COMMERCE:       <Briefcase className="w-5 h-5" />,
  ARTS:           <Palette className="w-5 h-5" />,
  LAW:            <Scale className="w-5 h-5" />,
  CIVIL_SERVICES: <Users className="w-5 h-5" />,
  MANAGEMENT:     <TrendingUp className="w-5 h-5" />,
  RESEARCH:       <FlaskConical className="w-5 h-5" />,
};

const DOMAIN_COLORS = [
  '#6366f1', '#8b5cf6', '#06b6d4', '#10b981',
  '#f59e0b', '#ec4899', '#f97316', '#14b8a6',
];

const STREAM_KEYS = Object.keys(STREAM_DISPLAY);

// ─── Sub-components ──────────────────────────────────────────────────────────

interface DomainTileProps {
  stream: string;
  score: number;
  color: string;
  index: number;
}

function DomainTile({ stream, score, color, index }: DomainTileProps) {
  const [expanded, setExpanded] = useState(false);
  const roles = CAREER_ROLES[stream] ?? [];
  const displayName = STREAM_DISPLAY[stream] ?? stream;

  return (
    <motion.div
      className="glass rounded-2xl p-5 cursor-pointer glass-hover"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.08 }}
      onClick={() => setExpanded(!expanded)}
    >
      <div className="flex items-center gap-3 mb-3">
        <div className="p-2 rounded-xl" style={{ background: `${color}25` }}>
          <span style={{ color }}>{DOMAIN_ICONS[stream]}</span>
        </div>
        <span className="font-semibold text-white text-sm">{displayName}</span>
      </div>

      <div className="flex items-center justify-between">
        <div className="flex-1">
          <div className="flex justify-between text-xs mb-1.5">
            <span className="text-white/50">Match</span>
            <span className="font-bold" style={{ color }}>{score}%</span>
          </div>
          <div className="h-2 bg-white/8 rounded-full overflow-hidden">
            <motion.div
              className="h-full rounded-full"
              style={{ background: color }}
              initial={{ width: 0 }}
              animate={{ width: `${score}%` }}
              transition={{ duration: 1, ease: 'easeOut', delay: index * 0.1 }}
            />
          </div>
        </div>
      </div>

      <AnimatePresence>
        {expanded && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            className="overflow-hidden mt-3 pt-3 border-t border-white/8"
          >
            <p className="text-white/50 text-xs mb-2">Top roles:</p>
            <ul className="space-y-1">
              {roles.map((role) => (
                <li key={role} className="text-sm text-white/80 flex items-center gap-2">
                  <span className="w-1.5 h-1.5 rounded-full bg-brand-400 flex-shrink-0" />
                  {role}
                </li>
              ))}
            </ul>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}

interface CustomBarProps {
  x?: number; y?: number; width?: number; height?: number; fill?: string;
}

function CustomBar({ x = 0, y = 0, width = 0, height = 0, fill = '#6366f1' }: CustomBarProps) {
  return <rect x={x} y={y} width={width} height={height} fill={fill} rx={4} ry={4} />;
}

const RISK_COLORS: Record<string, string> = {
  LOW: 'text-emerald-400', MODERATE: 'text-amber-400',
  HIGH: 'text-orange-400', CRITICAL: 'text-red-400',
};

const ERS_KEYS: Array<{ key: keyof Omit<ErsBreakdown, 'total'>; label: string }> = [
  { key: 'syllabusCoverage', label: 'Syllabus' },
  { key: 'mockTestTrend',    label: 'Mock Trend' },
  { key: 'masteryAverage',   label: 'Mastery' },
  { key: 'timeManagement',   label: 'Time Mgmt' },
  { key: 'accuracy',         label: 'Accuracy' },
];

// ─── Main Page ───────────────────────────────────────────────────────────────

export default function CareerOraclePage() {
  const user = useAuthStore((s) => s.user);
  const [selectedRec, setSelectedRec] = useState<CareerRecommendation | null>(null);
  const [insightsExpanded, setInsightsExpanded] = useState(false);

  // Career profile (tells us academic stream + ERS)
  const { data: profile, isLoading, isError, refetch: refetchProfile } = useQuery<CareerProfile | null>({
    queryKey: ['career-profile', user?.id],
    queryFn: async () => {
      try {
        const res = await api.get(`/api/v1/career-profiles/by-student/${user?.id}`);
        return res.data ?? null;
      } catch (err: any) {
        if (err?.response?.status === 404) return null;
        throw err;
      }
    },
    enabled: !!user?.id,
    retry: false,
  });

  // Career recommendations (separate resource)
  const { data: backendRecs = [], refetch: refetchRecs } = useQuery<BackendRecommendation[]>({
    queryKey: ['career-recommendations', user?.id],
    queryFn: async () => {
      const res = await api.get(`/api/v1/career-recommendations/students/${user?.id}`);
      const d = res.data;
      return Array.isArray(d) ? d : (d.content ?? []);
    },
    enabled: !!user?.id,
    retry: false,
  });

  // Gap analysis for performance context panel
  const { data: gap } = useQuery<GapAnalysis>({
    queryKey: ['gap-analysis', user?.id],
    queryFn: async () => {
      const res = await api.get(`/api/v1/performance/gap-analysis/${user?.id}`);
      return res.data;
    },
    enabled: !!user?.id,
    retry: false,
  });

  // Map backend recs → UI shape, sorted by rank
  const recommendations: CareerRecommendation[] = backendRecs
    .filter(r => r.isActive)
    .sort((a, b) => a.rankOrder - b.rankOrder)
    .map(r => ({
      stream: r.careerStream,
      displayName: STREAM_DISPLAY[r.careerStream] ?? r.careerStream,
      score: Math.round(Number(r.fitScore) || 0),
      description: r.rationale,
      requiredSkills: SKILLS_MAP[r.careerStream] ?? [],
      salaryRange: SALARY_MAP[r.careerStream] ?? '',
    }));

  const radarData = recommendations.map(r => ({
    domain: r.displayName,
    score: r.score,
    fullMark: 100,
  }));

  const generateMutation = useMutation({
    mutationFn: () =>
      api.post(`/api/v1/career-recommendations/students/${user?.id}/generate`),
    onSuccess: () => {
      toast.success('Career DNA generated!');
      refetchRecs();
      refetchProfile();
    },
    onError: () => {
      toast.error('Could not generate recommendations. Please try again later.');
    },
  });

  function handleShare() {
    if (profile?.id) {
      navigator.clipboard.writeText(`${window.location.origin}/career/shared/${profile.id}`);
      toast.success('Shareable link copied to clipboard!');
    }
  }

  // ── Loading / Error ──────────────────────────────────────────────────────

  if (isLoading) {
    return (
      <div className="min-h-screen p-6 flex items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <div className="w-10 h-10 border-2 border-brand-400/30 border-t-brand-400 rounded-full animate-spin" />
          <p className="text-white/50 text-sm">Loading your career profile…</p>
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="min-h-screen p-6 flex items-center justify-center">
        <div className="text-center space-y-4">
          <p className="text-white/50">Failed to load career profile. Please try again.</p>
          <button onClick={() => refetchProfile()} className="btn-primary">Retry</button>
        </div>
      </div>
    );
  }

  // ── Hero (shared across all states) ─────────────────────────────────────

  const Hero = (
    <motion.div
      initial={{ opacity: 0, y: -20 }}
      animate={{ opacity: 1, y: 0 }}
      className="relative overflow-hidden glass rounded-3xl p-8 border border-white/5"
    >
      <div className="absolute top-0 left-0 w-80 h-80 bg-brand-600/15 rounded-full blur-3xl -translate-x-1/2 -translate-y-1/2" />
      <div className="absolute bottom-0 right-0 w-64 h-64 bg-violet-600/15 rounded-full blur-3xl translate-x-1/2 translate-y-1/2" />
      <div className="relative z-10 flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
        <div>
          <div className="flex items-center gap-2 mb-3">
            <Sparkles className="w-5 h-5 text-brand-400" />
            <span className="text-brand-400 text-sm font-medium uppercase tracking-wider">AI Career Analysis</span>
          </div>
          <h1 className="text-4xl font-bold text-white mb-2">
            Your <span className="gradient-text">Career DNA</span>
          </h1>
          <p className="text-white/50 max-w-lg">
            Powered by AI, your personality profile, and academic performance.
          </p>
          {backendRecs[0]?.generatedAt && (
            <p className="text-white/30 text-xs mt-2">
              Last generated: {new Date(backendRecs[0].generatedAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}
            </p>
          )}
        </div>
        {recommendations.length > 0 && (
          <div className="flex items-center gap-3 flex-shrink-0">
            <button
              onClick={handleShare}
              className="flex items-center gap-2 px-4 py-2.5 rounded-xl glass border border-white/10 text-white/70 hover:text-white hover:border-white/20 transition-all text-sm"
            >
              <Share2 className="w-4 h-4" /> Share Report
            </button>
            <button
              onClick={() => generateMutation.mutate()}
              disabled={generateMutation.isPending}
              className="btn-primary flex items-center gap-2"
            >
              {generateMutation.isPending
                ? <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                : <Zap className="w-4 h-4" />}
              Regenerate
            </button>
          </div>
        )}
      </div>
    </motion.div>
  );

  // ── No-profile empty state (psychometric first) ──────────────────────────

  if (!profile && recommendations.length === 0) {
    return (
      <div className="min-h-screen p-6 space-y-8">
        {Hero}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="card text-center py-16 space-y-6"
        >
          <div className="w-20 h-20 mx-auto rounded-full bg-gradient-to-br from-brand-600/30 to-violet-600/30 flex items-center justify-center">
            <Brain className="w-10 h-10 text-brand-400" />
          </div>
          <div>
            <h2 className="text-2xl font-bold text-white mb-2">No Career Profile Yet</h2>
            <p className="text-white/50 max-w-md mx-auto text-sm">
              Complete your psychometric assessment to unlock AI-powered career recommendations tailored to your personality and aptitude. Or generate a starter profile instantly.
            </p>
          </div>
          <div className="flex flex-col sm:flex-row gap-3 justify-center">
            <a
              href="/psychometric"
              className="btn-primary flex items-center gap-2 justify-center"
            >
              <Brain className="w-4 h-4" /> Take Psychometric Assessment
            </a>
            <button
              onClick={() => generateMutation.mutate()}
              disabled={generateMutation.isPending}
              className="flex items-center gap-2 px-4 py-2.5 rounded-xl glass border border-white/10 text-white/70 hover:text-white hover:border-white/20 transition-all text-sm justify-center"
            >
              {generateMutation.isPending
                ? <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                : <Zap className="w-4 h-4" />}
              Generate Starter Profile
            </button>
          </div>
          <p className="text-white/25 text-xs">
            Starter profile uses default parameters. Take the psychometric assessment for personalised results.
          </p>
        </motion.div>
      </div>
    );
  }

  // ── Profile exists but no recommendations yet ────────────────────────────

  if (recommendations.length === 0) {
    return (
      <div className="min-h-screen p-6 space-y-8">
        {Hero}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="card text-center py-14 space-y-5"
        >
          <div className="w-16 h-16 mx-auto rounded-full bg-brand-600/20 flex items-center justify-center">
            <Target className="w-8 h-8 text-brand-400" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-white mb-1">Profile Ready — Generate Your Career DNA</h2>
            <p className="text-white/50 text-sm">
              Academic Stream: <span className="text-white/80 font-medium">{profile?.academicStream ?? 'SCIENCE'}</span>
              {profile?.ersScore != null && (
                <> &nbsp;·&nbsp; ERS Score: <span className="text-brand-300 font-medium">{Number(profile.ersScore).toFixed(1)}/100</span></>
              )}
            </p>
          </div>
          <button
            onClick={() => generateMutation.mutate()}
            disabled={generateMutation.isPending}
            className="btn-primary inline-flex items-center gap-2 mx-auto"
          >
            {generateMutation.isPending
              ? <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              : <Zap className="w-4 h-4" />}
            Generate Career Recommendations
          </button>
          <a href="/psychometric" className="block text-brand-400 hover:text-brand-300 text-sm">
            Take Psychometric Assessment first for better accuracy →
          </a>
        </motion.div>
      </div>
    );
  }

  // ── Full dashboard ───────────────────────────────────────────────────────

  return (
    <div className="min-h-screen p-6 space-y-8">
      {Hero}

      {/* Charts Row */}
      <div className="grid grid-cols-1 xl:grid-cols-5 gap-6">
        {/* Bar chart */}
        <motion.div
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ delay: 0.1 }}
          className="xl:col-span-3 card"
        >
          <h2 className="text-lg font-semibold text-white mb-1">Top Career Matches</h2>
          <p className="text-white/40 text-sm mb-6">Click a bar to explore details</p>

          <ResponsiveContainer width="100%" height={280}>
            <BarChart
              layout="vertical"
              data={recommendations.slice(0, 5).map(r => ({ ...r, domain: r.displayName }))}
              margin={{ top: 0, right: 24, left: 0, bottom: 0 }}
            >
              <XAxis type="number" domain={[0, 100]} tick={{ fill: 'rgba(255,255,255,0.3)', fontSize: 11 }} axisLine={false} tickLine={false} />
              <YAxis type="category" dataKey="domain" width={110} tick={{ fill: 'rgba(255,255,255,0.6)', fontSize: 12 }} axisLine={false} tickLine={false} />
              <Tooltip
                cursor={{ fill: 'rgba(255,255,255,0.04)' }}
                contentStyle={{ background: '#1e2130', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 12, color: '#fff' }}
                formatter={(value: number) => [`${value}%`, 'Match Score']}
              />
              <Bar
                dataKey="score"
                shape={<CustomBar />}
                onClick={(d) => setSelectedRec(d as CareerRecommendation)}
                cursor="pointer"
              >
                {recommendations.slice(0, 5).map((_, i) => (
                  <Cell key={i} fill={DOMAIN_COLORS[i % DOMAIN_COLORS.length]} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>

          <AnimatePresence>
            {selectedRec && (
              <motion.div
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: 'auto' }}
                exit={{ opacity: 0, height: 0 }}
                className="overflow-hidden mt-4 pt-4 border-t border-white/8"
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1">
                    <h3 className="font-bold text-white text-lg mb-1">{selectedRec.displayName}</h3>
                    <p className="text-white/50 text-sm mb-3">{selectedRec.description}</p>
                    <div className="flex flex-wrap gap-2 mb-3">
                      {selectedRec.requiredSkills.map((skill) => (
                        <span key={skill} className="badge bg-brand-600/20 text-brand-300">{skill}</span>
                      ))}
                    </div>
                    <p className="text-emerald-400 text-sm font-medium">
                      Avg. Salary: {selectedRec.salaryRange}
                    </p>
                  </div>
                  <a
                    href={`https://www.google.com/search?q=${encodeURIComponent(selectedRec.displayName + ' career courses India')}`}
                    target="_blank"
                    rel="noreferrer"
                    className="flex items-center gap-1.5 text-brand-400 hover:text-brand-300 text-sm font-medium flex-shrink-0"
                  >
                    Explore Courses <ExternalLink className="w-3.5 h-3.5" />
                  </a>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </motion.div>

        {/* Radar */}
        <motion.div
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ delay: 0.15 }}
          className="xl:col-span-2 card flex flex-col"
        >
          <h2 className="text-lg font-semibold text-white mb-1">Domain Radar</h2>
          <p className="text-white/40 text-sm mb-4">Your spread across all streams</p>
          <div className="flex-1 flex items-center justify-center">
            <ResponsiveContainer width="100%" height={260}>
              <RadarChart data={radarData} cx="50%" cy="50%" outerRadius="75%">
                <PolarGrid stroke="rgba(255,255,255,0.06)" />
                <PolarAngleAxis dataKey="domain" tick={{ fill: 'rgba(255,255,255,0.5)', fontSize: 10 }} />
                <Radar name="Score" dataKey="score" stroke="#6366f1" fill="#6366f1" fillOpacity={0.25} strokeWidth={2} />
              </RadarChart>
            </ResponsiveContainer>
          </div>
        </motion.div>
      </div>

      {/* Gap Analysis Performance Context */}
      {gap && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="card"
        >
          <div className="flex items-center gap-3 mb-5">
            <div className="p-2 rounded-xl bg-violet-600/20">
              <BarChart2 className="w-4 h-4 text-violet-400" />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-white">Performance Context</h2>
              <p className="text-white/40 text-sm">Your ERS score powers the career fit calculations above</p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            {/* ERS total */}
            <div className="glass rounded-2xl p-4 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-white/60 text-sm">ERS Score</span>
                <span className="text-2xl font-bold text-brand-400">{gap.ersBreakdown.total.toFixed(1)}<span className="text-white/30 text-sm font-normal">/10</span></span>
              </div>
              <div className="space-y-2">
                {ERS_KEYS.map(({ key, label }) => {
                  const comp = gap.ersBreakdown[key] ?? { score: 0, weight: 0, gap: 0 };
                  const pct = Math.min(100, ((comp.score ?? 0) / 10) * 100);
                  return (
                    <div key={key}>
                      <div className="flex justify-between text-xs text-white/50 mb-1">
                        <span>{label}</span>
                        <span>{comp.score.toFixed(1)}</span>
                      </div>
                      <div className="h-1.5 bg-white/8 rounded-full overflow-hidden">
                        <div className="h-full bg-brand-500 rounded-full" style={{ width: `${pct}%` }} />
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Weak subjects */}
            <div className="glass rounded-2xl p-4">
              <p className="text-white/60 text-sm mb-3">Subjects to Strengthen</p>
              {gap.subjectGaps.length === 0 ? (
                <p className="text-white/30 text-xs">All subjects on track</p>
              ) : (
                <ul className="space-y-2">
                  {gap.subjectGaps.slice(0, 4).map(sg => (
                    <li key={sg.subject} className="flex items-start gap-2">
                      <span className={cn('text-xs font-bold px-1.5 py-0.5 rounded flex-shrink-0 mt-0.5', {
                        'bg-red-500/20 text-red-400': sg.priority === 'CRITICAL',
                        'bg-orange-500/20 text-orange-400': sg.priority === 'HIGH',
                        'bg-amber-500/20 text-amber-400': sg.priority === 'MEDIUM',
                        'bg-emerald-500/20 text-emerald-400': sg.priority === 'ON_TRACK',
                      })}>
                        {sg.priority === 'ON_TRACK' ? '✓' : sg.priority.charAt(0)}
                      </span>
                      <div>
                        <p className="text-white/80 text-sm font-medium">{sg.subject}</p>
                        {sg.topWeakTopics.length > 0 && (
                          <p className="text-white/40 text-xs">{sg.topWeakTopics.slice(0, 2).join(', ')}</p>
                        )}
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>

            {/* Risk + connection note */}
            <div className="glass rounded-2xl p-4 space-y-4">
              <div>
                <p className="text-white/60 text-sm mb-2">Dropout Risk</p>
                <div className="flex items-center gap-2">
                  <AlertTriangle className={cn('w-5 h-5', RISK_COLORS[gap.riskSummary.level] ?? 'text-white/40')} />
                  <span className={cn('font-bold text-lg', RISK_COLORS[gap.riskSummary.level] ?? 'text-white/40')}>
                    {gap.riskSummary.level}
                  </span>
                </div>
                <p className="text-white/40 text-xs mt-1">{gap.riskSummary.topFactor}</p>
              </div>
              <div className="border-t border-white/8 pt-3">
                <p className="text-white/40 text-xs leading-relaxed">
                  Improving your ERS score directly raises your career fit percentages. Focus on weak subjects above to unlock higher-ranked streams.
                </p>
              </div>
            </div>
          </div>
        </motion.div>
      )}

      {/* AI Rationale / Why these recommendations */}
      {recommendations.length > 0 && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.25 }}
          className="card"
        >
          <button
            className="w-full flex items-center justify-between text-left"
            onClick={() => setInsightsExpanded(!insightsExpanded)}
          >
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-xl bg-brand-600/20">
                <TrendingUp className="w-4 h-4 text-brand-400" />
              </div>
              <div>
                <h2 className="text-lg font-semibold text-white">Why these recommendations?</h2>
                <p className="text-white/40 text-sm">AI-computed rationale based on your profile</p>
              </div>
            </div>
            {insightsExpanded
              ? <ChevronUp className="w-5 h-5 text-white/40" />
              : <ChevronDown className="w-5 h-5 text-white/40" />}
          </button>

          <AnimatePresence>
            {insightsExpanded && (
              <motion.div
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: 'auto' }}
                exit={{ opacity: 0, height: 0 }}
                className="overflow-hidden"
              >
                <div className="mt-5 pt-5 border-t border-white/8 space-y-3">
                  {recommendations.slice(0, 4).map((rec, i) => (
                    <motion.div
                      key={rec.stream}
                      initial={{ opacity: 0, x: -12 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: i * 0.06 }}
                      className="flex items-start gap-3"
                    >
                      <span className="w-5 h-5 rounded-full bg-brand-600/25 flex items-center justify-center flex-shrink-0 mt-0.5">
                        <span className="w-1.5 h-1.5 rounded-full bg-brand-400" />
                      </span>
                      <div>
                        <span className="text-brand-300 text-xs font-bold uppercase tracking-wide">{rec.displayName}</span>
                        <p className="text-white/70 text-sm">{rec.description}</p>
                      </div>
                    </motion.div>
                  ))}
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </motion.div>
      )}

      {/* Domain Exploration Tiles */}
      <div>
        <h2 className="text-xl font-bold text-white mb-4">Explore Career Streams</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 xl:grid-cols-8 gap-4">
          {STREAM_KEYS.map((stream, i) => {
            const rec = recommendations.find(r => r.stream === stream);
            return (
              <DomainTile
                key={stream}
                stream={stream}
                score={rec?.score ?? 0}
                color={DOMAIN_COLORS[i % DOMAIN_COLORS.length]}
                index={i}
              />
            );
          })}
        </div>
      </div>
    </div>
  );
}
