import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useQuery } from '@tanstack/react-query';
import {
  RadarChart, PolarGrid, PolarAngleAxis, Radar, ResponsiveContainer, Legend,
} from 'recharts';
import {
  Brain, RefreshCw, Calendar, ChevronRight, Eye, Ear, BookText, Activity,
} from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { cn } from '../../lib/utils';
import { Modal } from '../../components/ui/Modal';

interface BigFiveTrait {
  name: string;
  score: number;
  benchmark: number;
  key: string;
}

interface SessionHistoryItem {
  id: string;
  completedAt: string;
  completenessPercent: number;
}

interface PsychProfile {
  id: string;
  studentId: string;
  bigFive: BigFiveTrait[];
  riasecCode: string;
  learningStyleScores: Record<string, number>;
  sessionHistories: SessionHistoryItem[];
  generatedAt: string;
}

const BIG_FIVE_META: Record<string, { shortDesc: string; icon: string }> = {
  Openness: { shortDesc: 'High curiosity. Explores new ideas and creative domains.', icon: '💡' },
  Conscientiousness: { shortDesc: 'Organised and disciplined. Excellent exam preparation habits.', icon: '📋' },
  Extraversion: { shortDesc: 'Energised by social settings. Thrives in group learning.', icon: '🌟' },
  Agreeableness: { shortDesc: 'Cooperative and empathetic. Works well in study groups.', icon: '🤝' },
  Neuroticism: { shortDesc: 'Moderate stress response. Resilience coaching recommended.', icon: '🧘' },
};

const RIASEC_META: Record<string, { label: string; description: string; color: string }> = {
  R: { label: 'Realistic', description: 'Practical, hands-on, prefers working with tools or machines', color: 'bg-amber-600/20 text-amber-300 border-amber-600/30' },
  I: { label: 'Investigative', description: 'Analytical, curious, loves research and problem solving', color: 'bg-brand-600/20 text-brand-300 border-brand-600/30' },
  A: { label: 'Artistic', description: 'Creative, expressive, values originality and aesthetics', color: 'bg-violet-600/20 text-violet-300 border-violet-600/30' },
  S: { label: 'Social', description: 'Helpful, empathetic, enjoys teaching and counselling', color: 'bg-emerald-600/20 text-emerald-300 border-emerald-600/30' },
  E: { label: 'Enterprising', description: 'Persuasive, leadership-oriented, loves challenges', color: 'bg-orange-600/20 text-orange-300 border-orange-600/30' },
  C: { label: 'Conventional', description: 'Organised, detail-oriented, follows procedures', color: 'bg-cyan-600/20 text-cyan-300 border-cyan-600/30' },
};

const RIASEC_CAREERS: Record<string, string[]> = {
  'I-A-C': ['Research Scientist', 'Data Analyst', 'Software Architect'],
  'I-R-C': ['Engineer', 'Physicist', 'Systems Analyst'],
  'S-A-I': ['Psychologist', 'Counsellor', 'Educator'],
  'E-S-C': ['Business Manager', 'HR Specialist', 'Entrepreneur'],
};

const ASSESSMENT_QUESTIONS = [
  { id: 0, trait: 'openness',          text: 'I enjoy exploring new ideas and learning about different subjects.' },
  { id: 1, trait: 'openness',          text: 'I often think about abstract concepts and creative possibilities.' },
  { id: 2, trait: 'conscientiousness', text: 'I complete my study tasks on time and stick to a plan.' },
  { id: 3, trait: 'conscientiousness', text: 'I keep my notes and study space well organised.' },
  { id: 4, trait: 'extraversion',      text: 'I feel energised after studying or working in groups.' },
  { id: 5, trait: 'extraversion',      text: 'I enjoy participating in class discussions and presentations.' },
  { id: 6, trait: 'agreeableness',     text: 'I enjoy helping classmates understand difficult topics.' },
  { id: 7, trait: 'agreeableness',     text: 'I try to be considerate and empathetic in group settings.' },
  { id: 8, trait: 'neuroticism',       text: 'I often feel anxious or stressed about upcoming exams.' },
  { id: 9, trait: 'neuroticism',       text: 'Small setbacks during study sessions can disrupt my focus.' },
];


const LEARNING_TABS = [
  { key: 'visual', label: 'Visual', icon: <Eye className="w-4 h-4" />, tips: ['Use diagrams and charts', 'Colour-code notes', 'Watch video lectures', 'Mind mapping'] },
  { key: 'auditory', label: 'Auditory', icon: <Ear className="w-4 h-4" />, tips: ['Listen to recorded lectures', 'Discuss topics aloud', 'Use mnemonics', 'Group study'] },
  { key: 'reading', label: 'Reading', icon: <BookText className="w-4 h-4" />, tips: ['Take detailed notes', 'Re-read and summarise', 'Make flashcards', 'Read textbooks'] },
  { key: 'kinesthetic', label: 'Kinesthetic', icon: <Activity className="w-4 h-4" />, tips: ['Practice problems', 'Lab experiments', 'Teach others', 'Study with breaks'] },
];

function getTraitColor(score: number): string {
  if (score >= 70) return 'text-emerald-400';
  if (score >= 40) return 'text-brand-400';
  return 'text-amber-400';
}

function getTraitBgClass(score: number): string {
  if (score >= 70) return 'bg-emerald-500';
  if (score >= 40) return 'bg-brand-500';
  return 'bg-amber-500';
}

function TraitCard({ trait, index }: { trait: BigFiveTrait; index: number }) {
  const meta = BIG_FIVE_META[trait.name];
  const colorClass = getTraitColor(trait.score);
  const bgClass = getTraitBgClass(trait.score);

  return (
    <motion.div
      className="card"
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.1 + index * 0.07 }}
    >
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <span className="text-xl">{meta?.icon}</span>
          <span className="font-semibold text-white text-sm">{trait.name}</span>
        </div>
        <span className={cn('text-2xl font-bold', colorClass)}>{trait.score}</span>
      </div>

      <div className="h-2 bg-white/8 rounded-full mb-3 overflow-hidden">
        <motion.div
          className={cn('h-full rounded-full', bgClass)}
          initial={{ width: 0 }}
          animate={{ width: `${trait.score}%` }}
          transition={{ duration: 1, ease: 'easeOut', delay: 0.2 + index * 0.07 }}
        />
      </div>

      <p className="text-white/50 text-xs leading-relaxed">{meta?.shortDesc}</p>
    </motion.div>
  );
}

export default function PsychometricPage() {
  const user = useAuthStore((s) => s.user);
  const [activeTab, setActiveTab] = useState('visual');
  const [showAssessModal, setShowAssessModal] = useState(false);
  const [assessStep, setAssessStep] = useState<'info' | 'quiz' | 'submitting'>('info');
  const [quizAnswers, setQuizAnswers] = useState<Record<number, number>>({});

  const openModal = () => {
    setAssessStep('info');
    setQuizAnswers({});
    setShowAssessModal(true);
  };

  const handleStartQuiz = () => setAssessStep('quiz');

  const handleSubmitAssessment = async () => {
    if (!profile?.id) return;
    const allAnswered = ASSESSMENT_QUESTIONS.every((q) => quizAnswers[q.id] !== undefined);
    if (!allAnswered) { toast.error('Please answer all questions before submitting.'); return; }

    setAssessStep('submitting');
    try {
      const sessRes = await api.post(`/api/v1/psych/profiles/${profile.id}/sessions`, {
        sessionType: 'INITIAL',
        scheduledAt: new Date().toISOString(),
      });
      const sessionId = sessRes.data.id;

      const traitAvg = (traitKey: string) => {
        const qs = ASSESSMENT_QUESTIONS.filter((q) => q.trait === traitKey);
        const sum = qs.reduce((acc, q) => acc + (quizAnswers[q.id] ?? 3), 0);
        return parseFloat((sum / qs.length / 5.0).toFixed(4));
      };
      const openness = traitAvg('openness');
      const conscientiousness = traitAvg('conscientiousness');
      const extraversion = traitAvg('extraversion');
      const agreeableness = traitAvg('agreeableness');
      const neuroticism = traitAvg('neuroticism');

      const riasecScores: Record<string, number> = {
        R: conscientiousness * 0.5 + (1 - openness) * 0.5,
        I: openness * 0.7 + conscientiousness * 0.3,
        A: openness * 0.8 + extraversion * 0.2,
        S: agreeableness * 0.6 + extraversion * 0.4,
        E: extraversion * 0.5 + (1 - neuroticism) * 0.5,
        C: conscientiousness * 0.7 + (1 - openness) * 0.3,
      };
      const riasecCode = Object.entries(riasecScores)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 3)
        .map(([k]) => k)
        .join('-');

      await api.post(`/api/v1/psych/profiles/${profile.id}/sessions/${sessionId}/complete`, {
        openness, conscientiousness, extraversion, agreeableness, neuroticism,
        riasecCode,
        notes: 'Self-assessment completed via student portal',
      });

      toast.success('Assessment complete! Your personality profile has been updated.');
      setShowAssessModal(false);
      refetch();
    } catch (err: any) {
      toast.error(err?.response?.data?.message ?? 'Submission failed. Please try again.');
      setAssessStep('quiz');
    }
  };

  const { data: profile, isLoading, isError, refetch } = useQuery<PsychProfile | null>({
    queryKey: ['psych-profile', user?.id],
    queryFn: async () => {
      try {
        const res = await api.get(`/api/v1/psych/profiles`, { params: { studentId: user?.id } });
        const d = res.data;
        const list = Array.isArray(d) ? d : (d?.content ?? []);
        const raw = list[0];
        if (!raw) return null;

        // Transform flat backend fields → frontend PsychProfile shape
        const BENCHMARKS: Record<string, number> = { Openness: 72, Conscientiousness: 78, Extraversion: 65, Agreeableness: 74, Neuroticism: 40 };
        const bigFive: BigFiveTrait[] = [
          { name: 'Openness',          score: Math.round((raw.openness          ?? 0) * 100), benchmark: BENCHMARKS.Openness,          key: 'openness' },
          { name: 'Conscientiousness', score: Math.round((raw.conscientiousness ?? 0) * 100), benchmark: BENCHMARKS.Conscientiousness, key: 'conscientiousness' },
          { name: 'Extraversion',      score: Math.round((raw.extraversion      ?? 0) * 100), benchmark: BENCHMARKS.Extraversion,      key: 'extraversion' },
          { name: 'Agreeableness',     score: Math.round((raw.agreeableness     ?? 0) * 100), benchmark: BENCHMARKS.Agreeableness,     key: 'agreeableness' },
          { name: 'Neuroticism',       score: Math.round((raw.neuroticism       ?? 0) * 100), benchmark: BENCHMARKS.Neuroticism,       key: 'neuroticism' },
        ].filter(t => t.score > 0);

        // Derive learning style scores from Big Five (approximate heuristic)
        const o = raw.openness ?? 0, c = raw.conscientiousness ?? 0, e = raw.extraversion ?? 0;
        const learningStyleScores = {
          visual:     Math.round((o * 0.6 + c * 0.4) * 100),
          auditory:   Math.round((e * 0.5 + o * 0.5) * 100),
          kinesthetic:Math.round((e * 0.4 + c * 0.6) * 100),
          reading:    Math.round((c * 0.7 + o * 0.3) * 100),
        };

        // Fetch session histories
        let sessionHistories: { id: string; completedAt: string; completenessPercent: number }[] = [];
        try {
          const sessRes = await api.get(`/api/v1/psych/profiles/${raw.id}/sessions`);
          const sessions = Array.isArray(sessRes.data) ? sessRes.data : (sessRes.data?.content ?? []);
          sessionHistories = sessions
            .filter((s: any) => s.status === 'COMPLETED')
            .map((s: any) => ({ id: s.id, completedAt: s.completedAt ?? s.createdAt, completenessPercent: 100 }));
        } catch { /* ignore */ }

        return {
          id: raw.id,
          studentId: raw.studentId,
          bigFive,
          riasecCode: raw.riasecCode ?? '',
          learningStyleScores,
          sessionHistories,
          generatedAt: raw.updatedAt ?? raw.createdAt,
        };
      } catch (err: any) {
        if (err?.response?.status === 404) return null;
        throw err;
      }
    },
    enabled: !!user?.id,
    retry: false,
  });

  const bigFive = profile?.bigFive ?? [];
  const riasecCode = profile?.riasecCode ?? '';
  const learningStyleScores = profile?.learningStyleScores ?? {};
  const sessionHistories = profile?.sessionHistories ?? [];

  const radarData = bigFive.map((t) => ({
    trait: t.name.slice(0, 5),
    student: t.score,
    benchmark: t.benchmark,
    fullMark: 100,
  }));

  const riasecLetters = riasecCode
    ? riasecCode.split('-').map((letter) => ({
        letter,
        label: RIASEC_META[letter]?.label ?? letter,
        description: RIASEC_META[letter]?.description ?? '',
        color: RIASEC_META[letter]?.color ?? 'bg-white/10 text-white border-white/20',
      }))
    : [];

  const matchedCareers = riasecCode ? (RIASEC_CAREERS[riasecCode] ?? []) : [];
  const activeTabData = LEARNING_TABS.find((t) => t.key === activeTab)!;
  const learningScore = learningStyleScores[activeTab] ?? 0;

  if (isLoading) {
    return (
      <div className="min-h-screen p-6 flex items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <div className="w-10 h-10 border-2 border-violet-400/30 border-t-violet-400 rounded-full animate-spin" />
          <p className="text-white/50 text-sm">Loading your psychometric profile…</p>
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="min-h-screen p-6 flex items-center justify-center">
        <div className="text-center space-y-4">
          <p className="text-white/50">Failed to load psychometric profile. Please try again.</p>
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
          initial={{ opacity: 0, y: -16 }}
          animate={{ opacity: 1, y: 0 }}
          className="relative overflow-hidden glass rounded-3xl p-8 border border-white/5"
        >
          <div className="absolute inset-0 bg-gradient-to-br from-violet-900/20 via-transparent to-brand-900/20 pointer-events-none" />
          <div className="relative z-10 flex items-center gap-2 mb-3">
            <Brain className="w-5 h-5 text-violet-400" />
            <span className="text-violet-400 text-sm font-medium uppercase tracking-wider">Psychometric Analysis</span>
          </div>
          <h1 className="relative z-10 text-4xl font-bold text-white mb-2">
            Personality <span className="gradient-text">DNA</span>
          </h1>
          <p className="relative z-10 text-white/50 max-w-lg">
            Your Big Five personality profile, RIASEC code, and learning style — science-backed insights to optimise how you study.
          </p>
        </motion.div>

        {/* Empty state */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="card text-center py-16 space-y-6"
        >
          <div className="w-20 h-20 mx-auto rounded-full bg-gradient-to-br from-violet-600/30 to-brand-600/30 flex items-center justify-center">
            <Brain className="w-10 h-10 text-violet-400" />
          </div>
          <div>
            <h2 className="text-2xl font-bold text-white mb-2">No Assessment Taken Yet</h2>
            <p className="text-white/50 max-w-md mx-auto text-sm">
              Take a science-backed psychometric assessment to unlock your Big Five personality profile, RIASEC career code, and personalised learning style recommendations.
            </p>
          </div>
          <button
            onClick={openModal}
            className="btn-primary flex items-center gap-2 mx-auto"
          >
            <RefreshCw className="w-4 h-4" /> Take Assessment Now
          </button>
        </motion.div>

        {/* Assessment Modal — no profile */}
        <AssessmentModal
          isOpen={showAssessModal}
          onClose={() => setShowAssessModal(false)}
          hasProfile={false}
          assessStep={assessStep}
          quizAnswers={quizAnswers}
          setQuizAnswers={setQuizAnswers}
          onStartQuiz={handleStartQuiz}
          onSubmit={handleSubmitAssessment}
        />
      </div>
    );
  }

  return (
    <div className="min-h-screen p-6 space-y-8">
      {/* Hero */}
      <motion.div
        initial={{ opacity: 0, y: -16 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative overflow-hidden glass rounded-3xl p-8 border border-white/5"
      >
        <div className="absolute inset-0 bg-gradient-to-br from-violet-900/20 via-transparent to-brand-900/20 pointer-events-none" />
        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div>
            <div className="flex items-center gap-2 mb-3">
              <Brain className="w-5 h-5 text-violet-400" />
              <span className="text-violet-400 text-sm font-medium uppercase tracking-wider">Psychometric Analysis</span>
            </div>
            <h1 className="text-4xl font-bold text-white mb-2">
              Personality <span className="gradient-text">DNA</span>
            </h1>
            <p className="text-white/50 max-w-lg">
              Your Big Five personality profile, RIASEC code, and learning style — science-backed insights to optimise how you study.
            </p>
            {profile?.generatedAt && (
              <p className="text-white/30 text-xs mt-2">
                Last updated: {new Date(profile.generatedAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}
              </p>
            )}
          </div>
          <button
            onClick={openModal}
            className="btn-primary flex items-center gap-2 flex-shrink-0"
          >
            <RefreshCw className="w-4 h-4" /> Retake Assessment
          </button>
        </div>
      </motion.div>

      {/* Big Five Radar + Trait Cards */}
      {bigFive.length > 0 && (
        <div className="grid grid-cols-1 xl:grid-cols-5 gap-6">
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.1 }}
            className="xl:col-span-2 card"
          >
            <h2 className="text-lg font-semibold text-white mb-1">Big Five Profile</h2>
            <p className="text-white/40 text-sm mb-4">vs. ideal exam candidate benchmark</p>
            <ResponsiveContainer width="100%" height={280}>
              <RadarChart data={radarData} cx="50%" cy="50%" outerRadius="70%">
                <PolarGrid stroke="rgba(255,255,255,0.06)" />
                <PolarAngleAxis dataKey="trait" tick={{ fill: 'rgba(255,255,255,0.5)', fontSize: 11 }} />
                <Radar name="Benchmark" dataKey="benchmark" stroke="rgba(255,255,255,0.3)" fill="rgba(255,255,255,0.06)" strokeWidth={1.5} />
                <Radar name="Your Profile" dataKey="student" stroke="#6366f1" fill="#6366f1" fillOpacity={0.3} strokeWidth={2} />
                <Legend
                  iconType="circle"
                  iconSize={8}
                  formatter={(value) => <span style={{ color: 'rgba(255,255,255,0.5)', fontSize: 11 }}>{value}</span>}
                />
              </RadarChart>
            </ResponsiveContainer>
          </motion.div>

          <div className="xl:col-span-3 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-2 2xl:grid-cols-3 gap-4 auto-rows-min">
            {bigFive.map((trait, i) => (
              <TraitCard key={trait.name} trait={trait} index={i} />
            ))}
          </div>
        </div>
      )}

      {/* RIASEC */}
      {riasecLetters.length > 0 && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="card"
        >
          <h2 className="text-lg font-semibold text-white mb-1">Your RIASEC Profile</h2>
          <p className="text-white/40 text-sm mb-6">Holland's career interest codes — derived from your personality and interests</p>

          <div className="flex flex-wrap gap-4 mb-6">
            {riasecLetters.map((r, i) => (
              <motion.div
                key={r.letter}
                initial={{ opacity: 0, scale: 0.8 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ delay: 0.25 + i * 0.08, type: 'spring' }}
                className={cn('flex flex-col items-center gap-2 p-4 rounded-2xl border flex-1 min-w-36', r.color)}
              >
                <span className="text-4xl font-black">{r.letter}</span>
                <span className="font-semibold text-sm">{r.label}</span>
                <p className="text-center text-xs opacity-70">{r.description}</p>
              </motion.div>
            ))}
          </div>

          {matchedCareers.length > 0 && (
            <div>
              <p className="text-white/50 text-sm mb-3 flex items-center gap-2">
                <ChevronRight className="w-4 h-4 text-brand-400" />
                Careers matching your RIASEC code
              </p>
              <div className="flex flex-wrap gap-2">
                {matchedCareers.map((career) => (
                  <span key={career} className="badge bg-brand-600/20 text-brand-300 border border-brand-600/30 text-sm px-3 py-1.5">
                    {career}
                  </span>
                ))}
              </div>
            </div>
          )}
        </motion.div>
      )}

      {/* Learning Style */}
      {Object.keys(learningStyleScores).length > 0 && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="card"
        >
          <h2 className="text-lg font-semibold text-white mb-1">Learning Style</h2>
          <p className="text-white/40 text-sm mb-5">How you absorb information best</p>

          <div className="flex gap-1 mb-6 p-1 glass rounded-xl w-fit">
            {LEARNING_TABS.map((tab) => (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                className={cn(
                  'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all',
                  activeTab === tab.key ? 'bg-brand-600 text-white' : 'text-white/50 hover:text-white'
                )}
              >
                {tab.icon} {tab.label}
              </button>
            ))}
          </div>

          <AnimatePresence mode="wait">
            <motion.div
              key={activeTab}
              initial={{ opacity: 0, x: 12 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -12 }}
              transition={{ duration: 0.2 }}
              className="grid grid-cols-1 md:grid-cols-2 gap-6"
            >
              <div>
                <div className="flex justify-between text-sm mb-2">
                  <span className="text-white/60">{activeTabData.label} Learning Score</span>
                  <span className="font-bold text-brand-400">{learningScore > 0 ? `${learningScore}%` : 'N/A'}</span>
                </div>
                <div className="h-3 bg-white/8 rounded-full overflow-hidden mb-4">
                  <motion.div
                    className="h-full bg-gradient-to-r from-brand-600 to-violet-500 rounded-full"
                    initial={{ width: 0 }}
                    animate={{ width: `${learningScore}%` }}
                    transition={{ duration: 0.8 }}
                  />
                </div>
                <p className="text-white/50 text-sm">
                  {learningScore >= 70
                    ? `You are a strong ${activeTabData.label.toLowerCase()} learner. Maximise this style in your study routine.`
                    : learningScore > 0
                    ? `${activeTabData.label} is not your primary style. You can still use it as a supplementary tool.`
                    : `No data available for ${activeTabData.label.toLowerCase()} learning style.`}
                </p>
              </div>

              <div>
                <p className="text-white/60 text-sm mb-3">Recommended study techniques:</p>
                <ul className="space-y-2">
                  {activeTabData.tips.map((tip, i) => (
                    <motion.li
                      key={tip}
                      initial={{ opacity: 0, x: 10 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: i * 0.06 }}
                      className="flex items-center gap-2 text-sm text-white/70"
                    >
                      <span className="w-1.5 h-1.5 rounded-full bg-brand-400 flex-shrink-0" />
                      {tip}
                    </motion.li>
                  ))}
                </ul>
              </div>
            </motion.div>
          </AnimatePresence>
        </motion.div>
      )}

      {/* Session History */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.35 }}
        className="card"
      >
        <div className="flex items-center justify-between mb-5">
          <div>
            <h2 className="text-lg font-semibold text-white">Assessment History</h2>
            <p className="text-white/40 text-sm">Past psychometric sessions</p>
          </div>
          <button onClick={openModal} className="btn-primary text-sm">
            Take New Assessment
          </button>
        </div>

        {sessionHistories.length === 0 ? (
          <p className="text-white/40 text-sm text-center py-8">No sessions recorded yet.</p>
        ) : (
          <div className="space-y-3">
            {sessionHistories.map((session, i) => (
              <motion.div
                key={session.id}
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.06 }}
                className="flex items-center justify-between p-4 glass rounded-xl"
              >
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-brand-600/20">
                    <Calendar className="w-4 h-4 text-brand-400" />
                  </div>
                  <div>
                    <p className="text-white font-medium text-sm">
                      {new Date(session.completedAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'long', year: 'numeric' })}
                    </p>
                    <p className="text-white/40 text-xs">Session #{sessionHistories.length - i}</p>
                  </div>
                </div>
                <div className="flex items-center gap-4">
                  <div className="text-right">
                    <p className="text-sm font-bold text-emerald-400">{session.completenessPercent ?? 0}%</p>
                    <p className="text-white/30 text-xs">Completeness</p>
                  </div>
                  <button
                    onClick={openModal}
                    className="text-brand-400 hover:text-brand-300 text-xs font-medium transition-colors"
                  >
                    Retake
                  </button>
                </div>
              </motion.div>
            ))}
          </div>
        )}
      </motion.div>

      {/* Assessment Modal — has profile */}
      <AssessmentModal
        isOpen={showAssessModal}
        onClose={() => setShowAssessModal(false)}
        hasProfile={true}
        assessStep={assessStep}
        quizAnswers={quizAnswers}
        setQuizAnswers={setQuizAnswers}
        onStartQuiz={handleStartQuiz}
        onSubmit={handleSubmitAssessment}
      />
    </div>
  );
}

// ─── Shared Assessment Modal ────────────────────────────────────────────────
interface AssessmentModalProps {
  isOpen: boolean;
  onClose: () => void;
  hasProfile: boolean;
  assessStep: 'info' | 'quiz' | 'submitting';
  quizAnswers: Record<number, number>;
  setQuizAnswers: React.Dispatch<React.SetStateAction<Record<number, number>>>;
  onStartQuiz: () => void;
  onSubmit: () => void;
}

function AssessmentModal({ isOpen, onClose, hasProfile, assessStep, quizAnswers, setQuizAnswers, onStartQuiz, onSubmit }: AssessmentModalProps) {
  const answeredCount = Object.keys(quizAnswers).length;
  const allAnswered = answeredCount === ASSESSMENT_QUESTIONS.length;

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Discover Your Learning DNA">
      {!hasProfile ? (
        <div className="text-center space-y-4">
          <div className="w-16 h-16 mx-auto rounded-full bg-amber-600/20 flex items-center justify-center">
            <Brain className="w-8 h-8 text-amber-400" />
          </div>
          <h3 className="text-xl font-bold text-white">Profile Not Set Up</h3>
          <p className="text-white/50 text-sm leading-relaxed">
            Your psychometric profile hasn't been activated yet. Please contact your center administrator or counselor to get started.
          </p>
          <button className="btn-primary w-full" onClick={onClose}>Got It</button>
        </div>
      ) : assessStep === 'info' ? (
        <div className="text-center space-y-4">
          <div className="w-16 h-16 mx-auto rounded-full bg-gradient-to-br from-brand-600 to-violet-600 flex items-center justify-center">
            <Brain className="w-8 h-8 text-white" />
          </div>
          <h3 className="text-xl font-bold text-white">Big Five Assessment</h3>
          <p className="text-white/50 text-sm leading-relaxed">
            10 science-backed questions measuring your personality across five dimensions. Takes about 5 minutes. Results update your profile and learning recommendations.
          </p>
          <div className="grid grid-cols-3 gap-3 text-center">
            {[{ value: '5 min', label: 'Duration' }, { value: '10', label: 'Questions' }, { value: 'Big Five', label: 'Framework' }].map((stat) => (
              <div key={stat.label} className="glass rounded-xl p-3">
                <p className="text-white font-bold">{stat.value}</p>
                <p className="text-white/40 text-xs">{stat.label}</p>
              </div>
            ))}
          </div>
          <button className="btn-primary w-full py-3" onClick={onStartQuiz}>
            Start Assessment
          </button>
        </div>
      ) : assessStep === 'submitting' ? (
        <div className="text-center py-12 space-y-4">
          <div className="w-10 h-10 border-2 border-brand-400/30 border-t-brand-400 rounded-full animate-spin mx-auto" />
          <p className="text-white/60 text-sm">Calculating your personality profile…</p>
        </div>
      ) : (
        <div className="space-y-6 max-h-[70vh] overflow-y-auto pr-1">
          <div className="flex items-center justify-between sticky top-0 bg-surface-800 pb-2 border-b border-white/5">
            <p className="text-white/60 text-sm">{answeredCount} / {ASSESSMENT_QUESTIONS.length} answered</p>
            <div className="h-1.5 w-32 bg-white/10 rounded-full overflow-hidden">
              <div className="h-full bg-brand-500 rounded-full transition-all" style={{ width: `${(answeredCount / ASSESSMENT_QUESTIONS.length) * 100}%` }} />
            </div>
          </div>

          {ASSESSMENT_QUESTIONS.map((q) => (
            <div key={q.id} className="space-y-3">
              <p className="text-white text-sm font-medium leading-relaxed">
                <span className="text-white/30 mr-2">{q.id + 1}.</span>{q.text}
              </p>
              <div className="grid grid-cols-5 gap-1.5">
                {[1, 2, 3, 4, 5].map((val) => (
                  <button
                    key={val}
                    onClick={() => setQuizAnswers((prev) => ({ ...prev, [q.id]: val }))}
                    className={cn(
                      'py-2 rounded-lg text-xs font-medium transition-all border',
                      quizAnswers[q.id] === val
                        ? 'bg-brand-600 border-brand-500 text-white'
                        : 'glass border-white/10 text-white/50 hover:border-brand-500/50 hover:text-white/80'
                    )}
                  >
                    {val}
                  </button>
                ))}
              </div>
              <div className="flex justify-between text-white/20 text-xs px-0.5">
                <span>Strongly Disagree</span>
                <span>Strongly Agree</span>
              </div>
            </div>
          ))}

          <button
            className={cn('btn-primary w-full py-3 mt-4', !allAnswered && 'opacity-50 cursor-not-allowed')}
            onClick={onSubmit}
            disabled={!allAnswered}
          >
            {allAnswered ? 'Submit Assessment' : `Answer all questions (${ASSESSMENT_QUESTIONS.length - answeredCount} remaining)`}
          </button>
        </div>
      )}
    </Modal>
  );
}
