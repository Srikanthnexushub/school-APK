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
  TrendingUp, ExternalLink, Brain,
} from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { cn } from '../../lib/utils';

interface CareerRecommendation {
  domain: string;
  score: number;
  description: string;
  requiredSkills: string[];
  salaryRange: string;
}

interface CareerProfile {
  id: string;
  studentId: string;
  recommendations: CareerRecommendation[];
  riasecCode: string;
  generatedAt: string;
  insights: string[];
}

const SALARY_MAP: Record<string, string> = {
  Engineering: '₹8L – ₹40L / yr',
  Sciences: '₹5L – ₹25L / yr',
  Arts: '₹4L – ₹20L / yr',
  Business: '₹7L – ₹35L / yr',
  Healthcare: '₹6L – ₹30L / yr',
  Education: '₹4L – ₹18L / yr',
};

const SKILLS_MAP: Record<string, string[]> = {
  Engineering: ['Mathematics', 'Physics', 'Problem Solving', 'Programming'],
  Sciences: ['Research', 'Critical Thinking', 'Biology', 'Data Analysis'],
  Arts: ['Creativity', 'Communication', 'Design', 'Storytelling'],
  Business: ['Leadership', 'Finance', 'Marketing', 'Strategy'],
  Healthcare: ['Biology', 'Empathy', 'Attention to Detail', 'Medicine'],
  Education: ['Communication', 'Patience', 'Subject Mastery', 'Mentoring'],
};

const CAREER_ROLES: Record<string, string[]> = {
  Engineering: ['Software Engineer', 'Civil Engineer', 'Mechanical Engineer'],
  Sciences: ['Research Scientist', 'Data Scientist', 'Biochemist'],
  Arts: ['Graphic Designer', 'Content Creator', 'UX Designer'],
  Business: ['Product Manager', 'Financial Analyst', 'Marketing Lead'],
  Healthcare: ['Doctor', 'Pharmacist', 'Clinical Researcher'],
  Education: ['Teacher', 'Curriculum Designer', 'Academic Counsellor'],
};

const DOMAIN_ICONS: Record<string, React.ReactNode> = {
  Engineering: <Wrench className="w-5 h-5" />,
  Sciences: <FlaskConical className="w-5 h-5" />,
  Arts: <Palette className="w-5 h-5" />,
  Business: <Briefcase className="w-5 h-5" />,
  Healthcare: <Heart className="w-5 h-5" />,
  Education: <BookOpen className="w-5 h-5" />,
};

const DOMAIN_COLORS = [
  '#6366f1',
  '#8b5cf6',
  '#06b6d4',
  '#10b981',
  '#f59e0b',
  '#ec4899',
];

function CircularProgress({ score, color, size = 72 }: { score: number; color: string; size?: number }) {
  const r = (size - 10) / 2;
  const circ = 2 * Math.PI * r;
  const dash = (score / 100) * circ;

  return (
    <svg width={size} height={size} className="-rotate-90">
      <circle cx={size / 2} cy={size / 2} r={r} strokeWidth={6} stroke="rgba(255,255,255,0.08)" fill="none" />
      <motion.circle
        cx={size / 2}
        cy={size / 2}
        r={r}
        strokeWidth={6}
        stroke={color}
        fill="none"
        strokeLinecap="round"
        strokeDasharray={circ}
        initial={{ strokeDashoffset: circ }}
        animate={{ strokeDashoffset: circ - dash }}
        transition={{ duration: 1.2, ease: 'easeOut' }}
      />
    </svg>
  );
}

interface DomainTileProps {
  domain: string;
  score: number;
  color: string;
  index: number;
}

function DomainTile({ domain, score, color, index }: DomainTileProps) {
  const [expanded, setExpanded] = useState(false);
  const roles = CAREER_ROLES[domain] ?? [];

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
          <span style={{ color }}>{DOMAIN_ICONS[domain]}</span>
        </div>
        <span className="font-semibold text-white">{domain}</span>
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
            <p className="text-white/50 text-xs mb-2">Top roles in this domain:</p>
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
  x?: number;
  y?: number;
  width?: number;
  height?: number;
  fill?: string;
  index?: number;
}

function CustomBar(props: CustomBarProps) {
  const { x = 0, y = 0, width = 0, height = 0, fill = '#6366f1' } = props;
  return (
    <rect x={x} y={y} width={width} height={height} fill={fill} rx={4} ry={4} />
  );
}

export default function CareerOraclePage() {
  const user = useAuthStore((s) => s.user);
  const [selectedCareer, setSelectedCareer] = useState<CareerRecommendation | null>(null);
  const [insightsExpanded, setInsightsExpanded] = useState(false);

  const { data: profile, isLoading, isError, refetch } = useQuery<CareerProfile | null>({
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

  const generateMutation = useMutation({
    mutationFn: () =>
      api.post(`/api/v1/career-recommendations/students/${user?.id}/generate`),
    onSuccess: () => {
      toast.success('Career DNA generated!');
      refetch();
    },
    onError: () => {
      toast.error('Could not generate recommendations. Please try again later.');
    },
  });

  const recommendations = profile?.recommendations ?? [];
  const radarData = recommendations.map((r) => ({
    domain: r.domain,
    score: r.score,
    fullMark: 100,
  }));

  function handleShare() {
    if (profile?.id) {
      navigator.clipboard.writeText(`${window.location.origin}/career/shared/${profile.id}`);
      toast.success('Shareable link copied to clipboard!');
    }
  }

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
          <button onClick={() => refetch()} className="btn-primary">Retry</button>
        </div>
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="min-h-screen p-6 space-y-8">
        {/* Hero */}
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className="relative overflow-hidden glass rounded-3xl p-8 border border-white/5"
        >
          <div className="absolute top-0 left-0 w-80 h-80 bg-brand-600/15 rounded-full blur-3xl -translate-x-1/2 -translate-y-1/2" />
          <div className="absolute bottom-0 right-0 w-64 h-64 bg-violet-600/15 rounded-full blur-3xl translate-x-1/2 translate-y-1/2" />
          <div className="relative z-10 flex items-center gap-2 mb-3">
            <Sparkles className="w-5 h-5 text-brand-400" />
            <span className="text-brand-400 text-sm font-medium uppercase tracking-wider">AI Career Analysis</span>
          </div>
          <h1 className="relative z-10 text-4xl font-bold text-white mb-2">
            Your <span className="gradient-text">Career DNA</span>
          </h1>
          <p className="relative z-10 text-white/50 max-w-lg">
            Powered by AI, your personality profile, and academic performance.
          </p>
        </motion.div>

        {/* Empty state */}
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
              Complete your psychometric assessment first to unlock AI-powered career recommendations tailored to your personality and aptitude.
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
              {generateMutation.isPending ? (
                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <Zap className="w-4 h-4" />
              )}
              Generate Recommendations
            </button>
          </div>
        </motion.div>
      </div>
    );
  }

  return (
    <div className="min-h-screen p-6 space-y-8">
      {/* Hero */}
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
              Powered by AI, your personality profile, and academic performance. Discover paths uniquely matched to you.
            </p>
            {profile?.generatedAt && (
              <p className="text-white/30 text-xs mt-2">
                Last generated: {new Date(profile.generatedAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}
              </p>
            )}
          </div>

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
              {generateMutation.isPending ? (
                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <Zap className="w-4 h-4" />
              )}
              Regenerate
            </button>
          </div>
        </div>
      </motion.div>

      {/* Charts Row */}
      {recommendations.length > 0 && (
        <div className="grid grid-cols-1 xl:grid-cols-5 gap-6">
          {/* Bar Chart */}
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
                data={recommendations.slice(0, 5)}
                margin={{ top: 0, right: 24, left: 0, bottom: 0 }}
              >
                <XAxis type="number" domain={[0, 100]} tick={{ fill: 'rgba(255,255,255,0.3)', fontSize: 11 }} axisLine={false} tickLine={false} />
                <YAxis type="category" dataKey="domain" width={90} tick={{ fill: 'rgba(255,255,255,0.6)', fontSize: 12 }} axisLine={false} tickLine={false} />
                <Tooltip
                  cursor={{ fill: 'rgba(255,255,255,0.04)' }}
                  contentStyle={{ background: '#1e2130', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 12, color: '#fff' }}
                  formatter={(value: number) => [`${value}%`, 'Match Score']}
                />
                <Bar
                  dataKey="score"
                  shape={<CustomBar />}
                  onClick={(d) => setSelectedCareer(d as CareerRecommendation)}
                  cursor="pointer"
                >
                  {recommendations.slice(0, 5).map((_, index) => (
                    <Cell key={index} fill={DOMAIN_COLORS[index % DOMAIN_COLORS.length]} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>

            {/* Detail Panel */}
            <AnimatePresence>
              {selectedCareer && (
                <motion.div
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  className="overflow-hidden mt-4 pt-4 border-t border-white/8"
                >
                  <div className="flex items-start justify-between gap-4">
                    <div className="flex-1">
                      <h3 className="font-bold text-white text-lg mb-1">{selectedCareer.domain}</h3>
                      <p className="text-white/50 text-sm mb-3">{selectedCareer.description}</p>
                      <div className="flex flex-wrap gap-2 mb-3">
                        {(selectedCareer.requiredSkills?.length
                          ? selectedCareer.requiredSkills
                          : SKILLS_MAP[selectedCareer.domain] ?? []
                        ).map((skill) => (
                          <span key={skill} className="badge bg-brand-600/20 text-brand-300">{skill}</span>
                        ))}
                      </div>
                      <p className="text-emerald-400 text-sm font-medium">
                        Avg. Salary: {selectedCareer.salaryRange ?? SALARY_MAP[selectedCareer.domain] ?? 'N/A'}
                      </p>
                    </div>
                    <a
                      href={`https://www.google.com/search?q=${encodeURIComponent(selectedCareer.domain + ' career courses India')}`}
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

          {/* Radar Chart */}
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.15 }}
            className="xl:col-span-2 card flex flex-col"
          >
            <h2 className="text-lg font-semibold text-white mb-1">Domain Radar</h2>
            <p className="text-white/40 text-sm mb-4">Your spread across all domains</p>
            <div className="flex-1 flex items-center justify-center">
              <ResponsiveContainer width="100%" height={260}>
                <RadarChart data={radarData} cx="50%" cy="50%" outerRadius="75%">
                  <PolarGrid stroke="rgba(255,255,255,0.06)" />
                  <PolarAngleAxis dataKey="domain" tick={{ fill: 'rgba(255,255,255,0.5)', fontSize: 11 }} />
                  <Radar
                    name="Score"
                    dataKey="score"
                    stroke="#6366f1"
                    fill="#6366f1"
                    fillOpacity={0.25}
                    strokeWidth={2}
                  />
                </RadarChart>
              </ResponsiveContainer>
            </div>
          </motion.div>
        </div>
      )}

      {/* AI Insights */}
      {(profile?.insights?.length ?? 0) > 0 && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
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
                <p className="text-white/40 text-sm">AI-generated insights based on your profile</p>
              </div>
            </div>
            {insightsExpanded ? (
              <ChevronUp className="w-5 h-5 text-white/40" />
            ) : (
              <ChevronDown className="w-5 h-5 text-white/40" />
            )}
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
                  {(profile?.insights ?? []).map((insight, i) => (
                    <motion.div
                      key={i}
                      initial={{ opacity: 0, x: -12 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: i * 0.06 }}
                      className="flex items-start gap-3"
                    >
                      <span className="w-5 h-5 rounded-full bg-brand-600/25 flex items-center justify-center flex-shrink-0 mt-0.5">
                        <span className="w-1.5 h-1.5 rounded-full bg-brand-400" />
                      </span>
                      <p className="text-white/70 text-sm">{insight}</p>
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
        <h2 className="text-xl font-bold text-white mb-4">Explore Career Domains</h2>
        <div className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-6 gap-4">
          {Object.keys(DOMAIN_ICONS).map((domain, i) => {
            const rec = recommendations.find((r) => r.domain === domain);
            const score = rec?.score ?? 0;
            return (
              <DomainTile
                key={domain}
                domain={domain}
                score={score}
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
