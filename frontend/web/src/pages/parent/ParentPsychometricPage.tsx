import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import {
  Brain, ChevronRight, Star, TrendingUp, TrendingDown,
  BookOpen, Loader2, RefreshCw, Users,
} from 'lucide-react';
import {
  RadarChart, Radar, PolarGrid, PolarAngleAxis, PolarRadiusAxis,
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid, Cell,
} from 'recharts';
import { toast } from 'sonner';
import { cn } from '../../lib/utils';
import { Avatar } from '../../components/ui/Avatar';
import { Modal } from '../../components/ui/Modal';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { getAdaptiveQuestions, computeTraits } from '../../utils/psychometricUtils';

// ─── Types ────────────────────────────────────────────────────────────────────

interface ParentProfileResponse {
  id: string;
  name: string;
}

interface StudentLinkResponse {
  id: string;
  studentId: string;
  studentName: string;
  status: string;
}

interface BigFiveTrait {
  name: string;
  key: string;
  score: number; // 0–100
}

interface PsychProfile {
  id: string;
  studentId: string;
  bigFive: BigFiveTrait[];
  riasecCode: string;
  riasecScores: Record<string, number>;
  dominantLearningStyle: string;
  generatedAt: string;
  status: string;
}

// ─── Constants ────────────────────────────────────────────────────────────────

const RIASEC_META: Record<string, { label: string; color: string; careers: string[] }> = {
  R: { label: 'Realistic',     color: '#f87171', careers: ['Engineer', 'Technician', 'Mechanic'] },
  I: { label: 'Investigative', color: '#818cf8', careers: ['Research Scientist', 'Data Analyst', 'Physician'] },
  A: { label: 'Artistic',      color: '#fb923c', careers: ['Designer', 'Architect', 'Writer'] },
  S: { label: 'Social',        color: '#34d399', careers: ['Counselor', 'Teacher', 'Nurse'] },
  E: { label: 'Enterprising',  color: '#fbbf24', careers: ['Manager', 'Entrepreneur', 'Lawyer'] },
  C: { label: 'Conventional',  color: '#60a5fa', careers: ['Accountant', 'Analyst', 'Administrator'] },
};

const LEARNING_STYLE_LABELS: Record<string, string> = {
  visual: 'Visual',
  auditory: 'Auditory',
  kinesthetic: 'Kinesthetic',
  reading: 'Reading/Writing',
};

const LEARNING_STYLE_ICONS: Record<string, string> = {
  visual: '👁️', auditory: '👂', kinesthetic: '✋', reading: '📖',
};

function formatDate(iso: string): string {
  if (!iso) return '—';
  try {
    return new Date(iso).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  } catch {
    return iso;
  }
}

// ─── Transform raw API response → PsychProfile ───────────────────────────────

function transformProfile(raw: any): PsychProfile {
  const o = raw.openness ?? 0;
  const c = raw.conscientiousness ?? 0;
  const e = raw.extraversion ?? 0;
  const a = raw.agreeableness ?? 0;
  const n = raw.neuroticism ?? 0;

  const bigFive: BigFiveTrait[] = [
    { name: 'Openness',          key: 'openness',          score: Math.round(o * 100) },
    { name: 'Conscientiousness', key: 'conscientiousness', score: Math.round(c * 100) },
    { name: 'Extraversion',      key: 'extraversion',      score: Math.round(e * 100) },
    { name: 'Agreeableness',     key: 'agreeableness',     score: Math.round(a * 100) },
    { name: 'Neuroticism',       key: 'neuroticism',       score: Math.round(n * 100) },
  ].filter(t => t.score > 0);

  const riasecScores: Record<string, number> = {
    R: Math.round((c * 0.5 + (1 - o) * 0.5) * 100),
    I: Math.round((o * 0.7 + c * 0.3) * 100),
    A: Math.round((o * 0.8 + e * 0.2) * 100),
    S: Math.round((a * 0.6 + e * 0.4) * 100),
    E: Math.round((e * 0.5 + (1 - n) * 0.5) * 100),
    C: Math.round((c * 0.7 + (1 - o) * 0.3) * 100),
  };

  const learningStyleScores: Record<string, number> = {
    visual:      Math.round((o * 0.6 + c * 0.4) * 100),
    auditory:    Math.round((e * 0.5 + o * 0.5) * 100),
    kinesthetic: Math.round((e * 0.4 + c * 0.6) * 100),
    reading:     Math.round((c * 0.7 + o * 0.3) * 100),
  };
  const dominantLearningStyle = Object.entries(learningStyleScores)
    .sort((x, y) => y[1] - x[1])[0][0];

  return {
    id: raw.id,
    studentId: raw.studentId,
    bigFive,
    riasecCode: raw.riasecCode ?? '',
    riasecScores,
    dominantLearningStyle,
    generatedAt: raw.updatedAt ?? raw.createdAt ?? '',
    status: raw.status ?? '',
  };
}

// ─── Big Five Radar ────────────────────────────────────────────────────────────

function BigFiveRadar({ bigFive }: { bigFive: BigFiveTrait[] }) {
  const data = bigFive.map((t) => ({ subject: t.name.slice(0, 5), score: t.score, fullMark: 100 }));
  if (data.length === 0) return null;
  return (
    <div className="card">
      <h3 className="font-semibold text-white text-sm mb-4">Big Five Personality Traits</h3>
      <ResponsiveContainer width="100%" height={260}>
        <RadarChart data={data} margin={{ top: 8, right: 24, bottom: 8, left: 24 }}>
          <PolarGrid stroke="rgba(255,255,255,0.06)" />
          <PolarAngleAxis dataKey="subject" tick={{ fill: 'rgba(255,255,255,0.5)', fontSize: 11 }} />
          <PolarRadiusAxis angle={90} domain={[0, 100]} tick={{ fill: 'rgba(255,255,255,0.2)', fontSize: 9 }} />
          <Radar
            name="Score"
            dataKey="score"
            stroke="#6366f1"
            fill="#6366f1"
            fillOpacity={0.25}
            dot={{ fill: '#6366f1', r: 4 }}
          />
          <Tooltip
            contentStyle={{ background: '#252836', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12, fontSize: 12 }}
            labelStyle={{ color: 'rgba(255,255,255,0.5)' }}
            itemStyle={{ color: '#818cf8' }}
          />
        </RadarChart>
      </ResponsiveContainer>
    </div>
  );
}

// ─── RIASEC Chart ─────────────────────────────────────────────────────────────

function RiasecChart({ riasecCode, riasecScores }: { riasecCode: string; riasecScores: Record<string, number> }) {
  const data = Object.keys(RIASEC_META).map((code) => ({
    name: RIASEC_META[code].label,
    code,
    score: riasecScores[code] ?? 0,
    fill: RIASEC_META[code].color,
  }));

  const topCode = riasecCode || data.sort((a, b) => b.score - a.score).slice(0, 3).map(d => d.code).join('');
  const topLetters = topCode.split('-').filter(Boolean);
  const topNames = topLetters.map(l => RIASEC_META[l]?.label ?? l).join(' · ');

  return (
    <div className="card">
      <div className="flex items-start justify-between mb-4">
        <div>
          <h3 className="font-semibold text-white text-sm">RIASEC Career Code</h3>
          <p className="text-xs text-white/40 mt-0.5">Holland Interest Code Profile</p>
        </div>
        <div className="text-right">
          <div className="text-lg font-bold text-brand-400">{topCode || '—'}</div>
          <div className="text-xs text-white/30">{topNames}</div>
        </div>
      </div>
      <ResponsiveContainer width="100%" height={180}>
        <BarChart data={data} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.04)" vertical={false} />
          <XAxis dataKey="name" tick={{ fill: 'rgba(255,255,255,0.4)', fontSize: 10 }} axisLine={false} tickLine={false} />
          <YAxis domain={[0, 100]} tick={{ fill: 'rgba(255,255,255,0.2)', fontSize: 9 }} axisLine={false} tickLine={false} />
          <Tooltip
            contentStyle={{ background: '#252836', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12, fontSize: 12 }}
            labelStyle={{ color: 'rgba(255,255,255,0.5)' }}
          />
          <Bar dataKey="score" radius={[4, 4, 0, 0]}>
            {data.map((entry) => (
              <Cell key={entry.code} fill={entry.fill} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

// ─── Career Suggestions ───────────────────────────────────────────────────────

function CareerSuggestions({ riasecCode }: { riasecCode: string }) {
  const letters = riasecCode.split('-').filter(Boolean);
  const careers = letters.flatMap(l => RIASEC_META[l]?.careers ?? []).filter(Boolean).slice(0, 5);
  if (careers.length === 0) return null;

  return (
    <div className="card">
      <h3 className="font-semibold text-white text-sm mb-4">Top Career Recommendations</h3>
      <div className="space-y-2">
        {careers.map((career, i) => (
          <div key={i} className="flex items-center gap-3 p-3 rounded-xl bg-white/[0.03] border border-white/5">
            <div className="w-6 h-6 rounded-full bg-brand-500/20 flex items-center justify-center flex-shrink-0">
              <span className="text-brand-400 text-xs font-bold">{i + 1}</span>
            </div>
            <span className="text-sm text-white/80">{career}</span>
            <ChevronRight className="w-4 h-4 text-white/20 ml-auto flex-shrink-0" />
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── Strengths & Growth ────────────────────────────────────────────────────────

function StrengthsCard({ bigFive }: { bigFive: BigFiveTrait[] }) {
  if (bigFive.length === 0) return null;
  const sorted = [...bigFive].sort((a, b) => b.score - a.score);
  const strengths = sorted.slice(0, 3);
  const growth = sorted[sorted.length - 1];

  return (
    <div className="card">
      <h3 className="font-semibold text-white text-sm mb-4">Strengths & Growth Areas</h3>
      <div className="space-y-4">
        <div>
          <div className="flex items-center gap-2 mb-2">
            <TrendingUp className="w-4 h-4 text-emerald-400" />
            <span className="text-xs font-semibold text-emerald-400 uppercase tracking-wider">Top Strengths</span>
          </div>
          <div className="space-y-2">
            {strengths.map((t) => (
              <div key={t.key} className="flex items-center gap-3">
                <Star className="w-3.5 h-3.5 text-emerald-400 flex-shrink-0" />
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-sm text-white/80">{t.name}</span>
                    <span className="text-xs text-white/40">{t.score}%</span>
                  </div>
                  <div className="w-full bg-white/5 rounded-full h-1">
                    <div className="h-1 rounded-full bg-emerald-400 transition-all" style={{ width: `${t.score}%` }} />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {growth && (
          <div>
            <div className="flex items-center gap-2 mb-2">
              <TrendingDown className="w-4 h-4 text-amber-400" />
              <span className="text-xs font-semibold text-amber-400 uppercase tracking-wider">Growth Area</span>
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center justify-between mb-1">
                <span className="text-sm text-white/80">{growth.name}</span>
                <span className="text-xs text-white/40">{growth.score}%</span>
              </div>
              <div className="w-full bg-white/5 rounded-full h-1">
                <div className="h-1 rounded-full bg-amber-400 transition-all" style={{ width: `${growth.score}%` }} />
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

// ─── Inline Quiz Modal ────────────────────────────────────────────────────────

interface QuizModalProps {
  isOpen: boolean;
  onClose: () => void;
  assessStep: 'info' | 'quiz' | 'submitting';
  quizAnswers: Record<number, number>;
  setQuizAnswers: React.Dispatch<React.SetStateAction<Record<number, number>>>;
  onStartQuiz: () => void;
  onSubmit: () => void;
  questions: { id: number; trait: string; text: string }[];
}

function QuizModal({ isOpen, onClose, assessStep, quizAnswers, setQuizAnswers, onStartQuiz, onSubmit, questions }: QuizModalProps) {
  const answeredCount = Object.keys(quizAnswers).length;
  const allAnswered = answeredCount === questions.length;
  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Your Personality Assessment">
      {assessStep === 'info' ? (
        <div className="text-center space-y-4">
          <div className="w-16 h-16 mx-auto rounded-full bg-gradient-to-br from-brand-600 to-violet-600 flex items-center justify-center">
            <Brain className="w-8 h-8 text-white" />
          </div>
          <h3 className="text-xl font-bold text-white">Big Five Assessment</h3>
          <p className="text-white/50 text-sm leading-relaxed">
            10 science-backed questions measuring your personality across five dimensions. Takes about 5 minutes. Results are private to you.
          </p>
          <div className="grid grid-cols-3 gap-3 text-center">
            {[{ value: '5 min', label: 'Duration' }, { value: '10', label: 'Questions' }, { value: 'Big Five', label: 'Framework' }].map((stat) => (
              <div key={stat.label} className="glass rounded-xl p-3">
                <p className="text-white font-bold">{stat.value}</p>
                <p className="text-white/40 text-xs">{stat.label}</p>
              </div>
            ))}
          </div>
          <button className="btn-primary w-full py-3" onClick={onStartQuiz}>Start Assessment</button>
        </div>
      ) : assessStep === 'submitting' ? (
        <div className="text-center py-12 space-y-4">
          <div className="w-10 h-10 border-2 border-brand-400/30 border-t-brand-400 rounded-full animate-spin mx-auto" />
          <p className="text-white/60 text-sm">Calculating your personality profile…</p>
        </div>
      ) : (
        <div className="space-y-6 max-h-[70vh] overflow-y-auto pr-1">
          <div className="flex items-center justify-between sticky top-0 bg-surface-800 pb-2 border-b border-white/5">
            <p className="text-white/60 text-sm">{answeredCount} / {questions.length} answered</p>
            <div className="h-1.5 w-32 bg-white/10 rounded-full overflow-hidden">
              <div className="h-full bg-brand-500 rounded-full transition-all" style={{ width: `${(answeredCount / questions.length) * 100}%` }} />
            </div>
          </div>
          {questions.map((q) => (
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
            {allAnswered ? 'Submit Assessment' : `Answer all questions (${questions.length - answeredCount} remaining)`}
          </button>
        </div>
      )}
    </Modal>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────

export default function ParentPsychometricPage() {
  const user = useAuthStore((s) => s.user);
  const [activeView, setActiveView] = useState<'children' | 'mine'>('children');
  const [activeStudentId, setActiveStudentId] = useState<string | null>(null);

  // Parent's own quiz state
  const [showAssessModal, setShowAssessModal] = useState(false);
  const [assessStep, setAssessStep] = useState<'info' | 'quiz' | 'submitting'>('info');
  const [quizAnswers, setQuizAnswers] = useState<Record<number, number>>({});
  const [isCreatingProfile, setIsCreatingProfile] = useState(false);
  const parentQuestions = getAdaptiveQuestions('adult');

  const { data: profile } = useQuery<ParentProfileResponse>({
    queryKey: ['parent-profile'],
    queryFn: () => api.get('/api/v1/parents/me').then((r) => r.data),
  });

  const { data: linkedStudents = [], isLoading: studentsLoading } = useQuery<StudentLinkResponse[]>({
    queryKey: ['linked-students', profile?.id],
    queryFn: () =>
      api.get(`/api/v1/parents/${profile!.id}/students?size=50`).then((r) => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
    enabled: !!profile?.id,
  });

  const resolvedStudentId = activeStudentId ?? linkedStudents[0]?.studentId ?? null;
  const selectedLink = linkedStudents.find((s) => s.studentId === resolvedStudentId) ?? linkedStudents[0] ?? null;

  const { data: psychProfile = null, isLoading: psychLoading } = useQuery<PsychProfile | null>({
    queryKey: ['psych-profiles-parent', resolvedStudentId],
    queryFn: async () => {
      const res = await api.get(`/api/v1/psych/profiles?studentId=${resolvedStudentId}`);
      const d = res.data;
      const list = Array.isArray(d) ? d : (d.content ?? []);
      const raw = list[0];
      return raw ? transformProfile(raw) : null;
    },
    enabled: !!resolvedStudentId,
  });

  const isLoading = studentsLoading || psychLoading;

  // Parent's own psychometric profile (adult level, uses parent's userId as studentId)
  const { data: myPsychProfile = null, isLoading: myProfileLoading, refetch: refetchMyProfile } = useQuery<PsychProfile | null>({
    queryKey: ['psych-profile-parent-self', user?.id],
    queryFn: async () => {
      try {
        const res = await api.get(`/api/v1/psych/profiles`, { params: { studentId: user?.id } });
        const d = res.data;
        const list = Array.isArray(d) ? d : (d.content ?? []);
        const raw = list[0];
        return raw ? transformProfile(raw) : null;
      } catch (err: any) {
        if (err?.response?.status === 404) return null;
        throw err;
      }
    },
    enabled: !!user?.id && activeView === 'mine',
  });

  const openModal = () => {
    setAssessStep('info');
    setQuizAnswers({});
    setShowAssessModal(true);
  };

  const handleInitAndStart = async () => {
    if (isCreatingProfile) return;
    if (myPsychProfile) { openModal(); return; }
    setIsCreatingProfile(true);
    try {
      await api.post('/api/v1/psych/profiles', { studentId: user!.id });
      await refetchMyProfile();
      openModal();
    } catch (err: any) {
      if (err?.response?.status === 409 || err?.response?.status === 422) {
        await refetchMyProfile();
        openModal();
      } else {
        toast.error('Could not initialise your profile. Please try again.');
      }
    } finally {
      setIsCreatingProfile(false);
    }
  };

  const handleSubmitAssessment = async () => {
    if (!myPsychProfile?.id) return;
    const allAnswered = parentQuestions.every((q) => quizAnswers[q.id] !== undefined);
    if (!allAnswered) { toast.error('Please answer all questions before submitting.'); return; }
    setAssessStep('submitting');
    try {
      const sessRes = await api.post(`/api/v1/psych/profiles/${myPsychProfile.id}/sessions`, {
        sessionType: 'INITIAL',
        scheduledAt: new Date().toISOString(),
      });
      const { openness, conscientiousness, extraversion, agreeableness, neuroticism, riasecCode } =
        computeTraits(parentQuestions, quizAnswers);
      await api.post(`/api/v1/psych/profiles/${myPsychProfile.id}/sessions/${sessRes.data.id}/complete`, {
        openness, conscientiousness, extraversion, agreeableness, neuroticism,
        riasecCode,
        notes: 'Self-assessment completed via parent portal',
      });
      toast.success('Assessment complete! Your personality profile has been updated.');
      setShowAssessModal(false);
      refetchMyProfile();
    } catch (err: any) {
      toast.error(err?.response?.data?.message ?? 'Submission failed. Please try again.');
      setAssessStep('quiz');
    }
  };

  return (
    <div className="p-4 lg:p-8 space-y-6 max-w-5xl mx-auto">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-white">Psychometric Profile</h1>
        <p className="text-white/50 text-sm mt-0.5">
          Personality traits, career aptitude, and learning style — for you and your children.
        </p>
      </div>

      {/* Tab switcher */}
      <div className="flex gap-1 p-1 glass rounded-xl w-fit">
        <button
          onClick={() => setActiveView('children')}
          className={cn(
            'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all',
            activeView === 'children' ? 'bg-brand-600 text-white' : 'text-white/50 hover:text-white'
          )}
        >
          <Users className="w-4 h-4" /> My Children
        </button>
        <button
          onClick={() => setActiveView('mine')}
          className={cn(
            'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all',
            activeView === 'mine' ? 'bg-brand-600 text-white' : 'text-white/50 hover:text-white'
          )}
        >
          <Brain className="w-4 h-4" /> My Profile
        </button>
      </div>

      {/* ── My Profile Tab ── */}
      {activeView === 'mine' && (
        <>
          {myProfileLoading && (
            <div className="flex items-center justify-center py-16">
              <Loader2 className="w-8 h-8 text-brand-400 animate-spin" />
            </div>
          )}

          {!myProfileLoading && !myPsychProfile && (
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              className="card text-center py-16 space-y-6"
            >
              <div className="w-20 h-20 mx-auto rounded-full bg-gradient-to-br from-brand-600 to-violet-600 flex items-center justify-center">
                <Brain className="w-10 h-10 text-white" />
              </div>
              <div>
                <h2 className="text-2xl font-bold text-white mb-2">Discover Your Personality Profile</h2>
                <p className="text-white/50 max-w-md mx-auto text-sm">
                  Take the Big Five personality assessment to understand your own traits, career aptitude (RIASEC), and learning style. No admin required.
                </p>
              </div>
              <div className="flex justify-center gap-8 text-center">
                {[{ value: '5 min', label: 'Duration' }, { value: '10', label: 'Questions' }, { value: 'Big Five', label: 'Framework' }].map((stat) => (
                  <div key={stat.label}>
                    <p className="text-xl font-bold text-brand-400">{stat.value}</p>
                    <p className="text-white/40 text-xs">{stat.label}</p>
                  </div>
                ))}
              </div>
              <button
                onClick={handleInitAndStart}
                disabled={isCreatingProfile}
                className="btn-primary px-8 py-3 flex items-center gap-2 mx-auto"
              >
                {isCreatingProfile ? <Loader2 className="w-4 h-4 animate-spin" /> : <Brain className="w-4 h-4" />}
                {isCreatingProfile ? 'Setting up…' : 'Start Assessment'}
              </button>
            </motion.div>
          )}

          {!myProfileLoading && myPsychProfile && (
            <>
              <div className="flex items-center gap-3 flex-wrap">
                <span className="text-xs text-white/40">
                  Last updated: <span className="text-white/60">{formatDate(myPsychProfile.generatedAt)}</span>
                </span>
                <span className={cn(
                  'text-xs px-2 py-0.5 rounded-full font-medium',
                  myPsychProfile.status === 'ACTIVE' ? 'bg-emerald-500/15 text-emerald-400' : 'bg-amber-500/15 text-amber-400'
                )}>
                  {myPsychProfile.status}
                </span>
                <button onClick={handleInitAndStart} className="ml-auto btn-primary text-xs flex items-center gap-1 py-1.5 px-3">
                  <RefreshCw className="w-3 h-3" /> Retake
                </button>
              </div>

              {myPsychProfile.bigFive.length > 0 && (
                <>
                  <div className="card border border-brand-500/15">
                    <div className="flex items-center gap-3">
                      <div className="p-2.5 rounded-xl bg-brand-500/15 flex-shrink-0">
                        <BookOpen className="w-5 h-5 text-brand-400" />
                      </div>
                      <div>
                        <div className="text-xs text-white/40 mb-0.5">Dominant Learning Style</div>
                        <div className="text-lg font-bold text-white flex items-center gap-2">
                          <span>{LEARNING_STYLE_ICONS[myPsychProfile.dominantLearningStyle] ?? '🧠'}</span>
                          <span>{LEARNING_STYLE_LABELS[myPsychProfile.dominantLearningStyle] ?? myPsychProfile.dominantLearningStyle}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div className="grid lg:grid-cols-2 gap-6">
                    <BigFiveRadar bigFive={myPsychProfile.bigFive} />
                    <StrengthsCard bigFive={myPsychProfile.bigFive} />
                  </div>
                </>
              )}
              {myPsychProfile.riasecCode && (
                <div className="grid lg:grid-cols-2 gap-6">
                  <RiasecChart riasecCode={myPsychProfile.riasecCode} riasecScores={myPsychProfile.riasecScores} />
                  <CareerSuggestions riasecCode={myPsychProfile.riasecCode} />
                </div>
              )}
            </>
          )}

          <QuizModal
            isOpen={showAssessModal}
            onClose={() => setShowAssessModal(false)}
            assessStep={assessStep}
            quizAnswers={quizAnswers}
            setQuizAnswers={setQuizAnswers}
            onStartQuiz={() => setAssessStep('quiz')}
            onSubmit={handleSubmitAssessment}
            questions={parentQuestions}
          />
        </>
      )}

      {/* ── My Children Tab ── */}
      {activeView === 'children' && <>

      {/* Child selector */}
      {linkedStudents.length > 1 && (
        <div className="flex gap-3 overflow-x-auto pb-1">
          {linkedStudents.map((s) => (
            <button
              key={s.studentId}
              onClick={() => setActiveStudentId(s.studentId)}
              className={cn(
                'flex items-center gap-3 px-4 py-3 rounded-2xl border transition-all flex-shrink-0',
                resolvedStudentId === s.studentId
                  ? 'border-brand-500 bg-brand-500/10'
                  : 'border-white/5 bg-surface-50/40 hover:border-white/10'
              )}
            >
              <Avatar name={s.studentName} size="sm" />
              <div className="text-left">
                <div className="text-sm font-semibold text-white">{s.studentName}</div>
              </div>
            </button>
          ))}
        </div>
      )}

      {/* No children state */}
      {!studentsLoading && linkedStudents.length === 0 && (
        <div className="card text-center py-12">
          <Brain className="w-10 h-10 text-white/20 mx-auto mb-3" />
          <p className="text-white/50 text-sm">No children linked yet.</p>
          <p className="text-white/30 text-xs mt-1">Link a child to view their psychometric profile.</p>
        </div>
      )}

      {/* Loading */}
      {isLoading && resolvedStudentId && (
        <div className="flex items-center justify-center py-16">
          <Loader2 className="w-8 h-8 text-brand-400 animate-spin" />
        </div>
      )}

      {/* No profile state */}
      {!isLoading && selectedLink && !psychProfile && (
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="card text-center py-16"
        >
          <Brain className="w-12 h-12 text-white/15 mx-auto mb-4" />
          <h3 className="text-white/60 font-semibold mb-2">No Psychometric Assessment Yet</h3>
          <p className="text-white/30 text-sm max-w-md mx-auto">
            {selectedLink.studentName} hasn't completed the psychometric assessment yet.
            They can take it from their student portal under the Psychometric section.
          </p>
          <div className="mt-6 p-4 bg-brand-500/5 border border-brand-500/15 rounded-xl max-w-md mx-auto">
            <div className="flex items-start gap-3 text-left">
              <Brain className="w-5 h-5 text-brand-400 flex-shrink-0 mt-0.5" />
              <div>
                <div className="text-sm font-medium text-white mb-1">About the Assessment</div>
                <p className="text-xs text-white/40">
                  The Big Five personality assessment takes about 10 minutes and provides insights
                  into personality traits, career aptitude (RIASEC), and learning style.
                </p>
              </div>
            </div>
          </div>
        </motion.div>
      )}

      {/* Profile data */}
      {!isLoading && psychProfile && (
        <>
          {/* Status bar */}
          <div className="flex items-center gap-3 flex-wrap">
            <span className="text-xs text-white/40">
              Last updated: <span className="text-white/60">{formatDate(psychProfile.generatedAt)}</span>
            </span>
            <span className={cn(
              'text-xs px-2 py-0.5 rounded-full font-medium',
              psychProfile.status === 'ACTIVE' ? 'bg-emerald-500/15 text-emerald-400' : 'bg-amber-500/15 text-amber-400'
            )}>
              {psychProfile.status}
            </span>
          </div>

          {/* Dominant learning style */}
          <div className="card border border-brand-500/15">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-xl bg-brand-500/15 flex-shrink-0">
                <BookOpen className="w-5 h-5 text-brand-400" />
              </div>
              <div>
                <div className="text-xs text-white/40 mb-0.5">Dominant Learning Style</div>
                <div className="text-lg font-bold text-white flex items-center gap-2">
                  <span>{LEARNING_STYLE_ICONS[psychProfile.dominantLearningStyle] ?? '🧠'}</span>
                  <span>{LEARNING_STYLE_LABELS[psychProfile.dominantLearningStyle] ?? psychProfile.dominantLearningStyle}</span>
                </div>
              </div>
            </div>
          </div>

          {/* Radar + Strengths grid */}
          {psychProfile.bigFive.length > 0 && (
            <div className="grid lg:grid-cols-2 gap-6">
              <BigFiveRadar bigFive={psychProfile.bigFive} />
              <StrengthsCard bigFive={psychProfile.bigFive} />
            </div>
          )}

          {/* RIASEC + Career */}
          {psychProfile.riasecCode && (
            <div className="grid lg:grid-cols-2 gap-6">
              <RiasecChart riasecCode={psychProfile.riasecCode} riasecScores={psychProfile.riasecScores} />
              <CareerSuggestions riasecCode={psychProfile.riasecCode} />
            </div>
          )}
        </>
      )}

      </>}
    </div>
  );
}
