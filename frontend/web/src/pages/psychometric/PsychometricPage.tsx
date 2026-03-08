import { useState } from 'react';
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

const LEARNING_TABS = [
  { key: 'visual', label: 'Visual', icon: <Eye className="w-4 h-4" />, tips: ['Use diagrams and charts', 'Colour-code notes', 'Watch video lectures', 'Mind mapping'] },
  { key: 'auditory', label: 'Auditory', icon: <Ear className="w-4 h-4" />, tips: ['Listen to recorded lectures', 'Discuss topics aloud', 'Use mnemonics', 'Group study'] },
  { key: 'reading', label: 'Reading', icon: <BookText className="w-4 h-4" />, tips: ['Take detailed notes', 'Re-read and summarise', 'Make flashcards', 'Read textbooks'] },
  { key: 'kinesthetic', label: 'Kinesthetic', icon: <Activity className="w-4 h-4" />, tips: ['Practice problems', 'Lab experiments', 'Teach others', 'Study with breaks'] },
];

const MOCK_PROFILE: PsychProfile = {
  id: 'mock-psych-1',
  studentId: 'student-1',
  bigFive: [
    { name: 'Openness', score: 78, benchmark: 65, key: 'Openness' },
    { name: 'Conscientiousness', score: 84, benchmark: 72, key: 'Conscientiousness' },
    { name: 'Extraversion', score: 52, benchmark: 58, key: 'Extraversion' },
    { name: 'Agreeableness', score: 70, benchmark: 68, key: 'Agreeableness' },
    { name: 'Neuroticism', score: 41, benchmark: 45, key: 'Neuroticism' },
  ],
  riasecCode: 'I-A-C',
  learningStyleScores: { visual: 82, auditory: 55, reading: 74, kinesthetic: 61 },
  sessionHistories: [
    { id: 's1', completedAt: new Date(Date.now() - 86400000 * 30).toISOString(), completenessPercent: 100 },
    { id: 's2', completedAt: new Date(Date.now() - 86400000 * 90).toISOString(), completenessPercent: 85 },
    { id: 's3', completedAt: new Date(Date.now() - 86400000 * 180).toISOString(), completenessPercent: 72 },
  ],
  generatedAt: new Date(Date.now() - 86400000 * 30).toISOString(),
};

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
  const meta = BIG_FIVE_META[trait.key];
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

  const { data: profile } = useQuery<PsychProfile>({
    queryKey: ['psych-profile', user?.id],
    queryFn: async () => {
      const res = await api.get(`/api/v1/psych/profiles/${user?.id}`);
      return res.data;
    },
    retry: false,
    placeholderData: MOCK_PROFILE,
  });

  const data = profile ?? MOCK_PROFILE;

  const radarData = data.bigFive.map((t) => ({
    trait: t.name.slice(0, 5),
    student: t.score,
    benchmark: t.benchmark,
    fullMark: 100,
  }));

  const riasecLetters = data.riasecCode.split('-').map((letter) => ({
    letter,
    label: RIASEC_META[letter]?.label ?? letter,
    description: RIASEC_META[letter]?.description ?? '',
    color: RIASEC_META[letter]?.color ?? 'bg-white/10 text-white border-white/20',
  }));

  const matchedCareers = RIASEC_CAREERS[data.riasecCode] ?? ['Researcher', 'Data Scientist', 'Analyst'];
  const activeTabData = LEARNING_TABS.find((t) => t.key === activeTab)!;
  const learningScore = data.learningStyleScores[activeTab] ?? 50;

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
          </div>
          <button
            onClick={() => setShowAssessModal(true)}
            className="btn-primary flex items-center gap-2 flex-shrink-0"
          >
            <RefreshCw className="w-4 h-4" /> Retake Assessment
          </button>
        </div>
      </motion.div>

      {/* Big Five Radar + Trait Cards */}
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
          {data.bigFive.map((trait, i) => (
            <TraitCard key={trait.name} trait={trait} index={i} />
          ))}
        </div>
      </div>

      {/* RIASEC */}
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
      </motion.div>

      {/* Learning Style */}
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
                <span className="font-bold text-brand-400">{learningScore}%</span>
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
                  : `${activeTabData.label} is not your primary style. You can still use it as a supplementary tool.`}
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
          <button onClick={() => setShowAssessModal(true)} className="btn-primary text-sm">
            Take New Assessment
          </button>
        </div>

        <div className="space-y-3">
          {data.sessionHistories.map((session, i) => (
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
                  <p className="text-white/40 text-xs">Session #{data.sessionHistories.length - i}</p>
                </div>
              </div>
              <div className="flex items-center gap-4">
                <div className="text-right">
                  <p className="text-sm font-bold text-emerald-400">{session.completenessPercent}%</p>
                  <p className="text-white/30 text-xs">Completeness</p>
                </div>
                <button
                  onClick={() => setShowAssessModal(true)}
                  className="text-brand-400 hover:text-brand-300 text-xs font-medium transition-colors"
                >
                  Retake
                </button>
              </div>
            </motion.div>
          ))}
        </div>
      </motion.div>

      {/* Assessment Modal */}
      <Modal isOpen={showAssessModal} onClose={() => setShowAssessModal(false)} title="Discover Your Learning DNA">
        <div className="text-center space-y-4">
          <div className="w-16 h-16 mx-auto rounded-full bg-gradient-to-br from-brand-600 to-violet-600 flex items-center justify-center">
            <Brain className="w-8 h-8 text-white" />
          </div>
          <h3 className="text-xl font-bold text-white">Psychometric Assessment</h3>
          <p className="text-white/50 text-sm leading-relaxed">
            A science-backed 10-minute assessment covering personality traits, learning styles, and career interests. Your results personalise your entire NexusEd experience.
          </p>
          <div className="grid grid-cols-3 gap-3 text-center">
            {[{ value: '10 min', label: 'Duration' }, { value: '50+', label: 'Questions' }, { value: 'Big Five', label: 'Framework' }].map((stat) => (
              <div key={stat.label} className="glass rounded-xl p-3">
                <p className="text-white font-bold">{stat.value}</p>
                <p className="text-white/40 text-xs">{stat.label}</p>
              </div>
            ))}
          </div>
          <button
            className="btn-primary w-full py-3"
            onClick={() => {
              toast.success('Assessment started! Redirecting…');
              setShowAssessModal(false);
            }}
          >
            Start 10-Minute Assessment
          </button>
        </div>
      </Modal>
    </div>
  );
}
