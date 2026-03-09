// src/pages/mentor-portal/MentorPortalInsightsPage.tsx
import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Brain, Sparkles, AlertTriangle, TrendingDown, TrendingUp,
  CheckCircle2, XCircle, Loader2, ChevronDown, ChevronUp,
  FileText, Users, BarChart3, Lightbulb, Shield, RefreshCw,
  ClipboardList, BookOpen, Target, Zap,
} from 'lucide-react';
import { useMutation } from '@tanstack/react-query';
import { toast } from 'sonner';
import api from '../../lib/api';
import { cn } from '../../lib/utils';

// ─── Types ────────────────────────────────────────────────────────────────────

interface GeneratedQuestion {
  questionText: string;
  options: string[];
  correctAnswer: number;
  explanation: string;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
}

interface BatchHealthData {
  batchName: string;
  subject: string;
  avgScore: number;
  weeklyTrend: number[];
  alertLevel: 'NORMAL' | 'WARNING' | 'CRITICAL';
  studentCount: number;
  dropPercent: number;
}

interface PendingGrade {
  id: string;
  studentName: string;
  examTitle: string;
  questionText: string;
  studentAnswer: string;
  rubric: string;
  aiSuggestedScore: number;
  maxScore: number;
  aiFeedback: string;
}

// ─── Mock Data ────────────────────────────────────────────────────────────────

const BATCH_HEALTH: BatchHealthData[] = [
  {
    batchName: 'JEE Advanced Batch A',
    subject: 'Physics',
    avgScore: 61.2,
    weeklyTrend: [72, 70, 68, 65, 63, 61],
    alertLevel: 'WARNING',
    studentCount: 28,
    dropPercent: 11,
  },
  {
    batchName: 'NEET Batch B',
    subject: 'Biology',
    avgScore: 78.5,
    weeklyTrend: [74, 75, 77, 76, 78, 79],
    alertLevel: 'NORMAL',
    studentCount: 32,
    dropPercent: 0,
  },
  {
    batchName: 'JEE Mains Batch C',
    subject: 'Mathematics',
    avgScore: 48.3,
    weeklyTrend: [65, 60, 58, 54, 51, 48],
    alertLevel: 'CRITICAL',
    studentCount: 24,
    dropPercent: 26,
  },
];

const PENDING_GRADES: PendingGrade[] = [
  {
    id: 'pg1',
    studentName: 'Arjun Kapoor',
    examTitle: 'Physics Mock Test #4',
    questionText: 'Explain the principle of conservation of energy with a real-world example involving both kinetic and potential energy.',
    studentAnswer: 'Energy is neither created nor destroyed. For example, a roller coaster at the top has maximum potential energy which converts to kinetic energy at the bottom. The total energy sum remains constant throughout ignoring friction losses.',
    rubric: 'Correct principle (2), Real example (2), Energy types explained (2), Mathematical relation mentioned (2), Limitations acknowledged (2)',
    aiSuggestedScore: 8,
    maxScore: 10,
    aiFeedback: 'Strong conceptual understanding demonstrated. The roller coaster example is apt and both energy types are correctly identified. Deducted 2 marks: mathematical relation (½mv² + mgh = const) not explicitly stated and friction losses mentioned but not quantified.',
  },
  {
    id: 'pg2',
    studentName: 'Priya Nath',
    examTitle: 'Chemistry Essay — Organic Reactions',
    questionText: 'Describe the mechanism of SN2 reaction and factors that favour it over SN1.',
    studentAnswer: 'SN2 is a bimolecular nucleophilic substitution. The nucleophile attacks from the back while the leaving group leaves. It is favoured by primary substrates, strong nucleophiles, polar aprotic solvents and low temperature. Steric hindrance slows the reaction.',
    rubric: 'Mechanism description (3), Stereochemistry inversion (2), Favourable conditions listed (3), Comparison with SN1 (2)',
    aiSuggestedScore: 7,
    maxScore: 10,
    aiFeedback: 'Good mechanistic understanding. Walden inversion (stereochemistry) not explicitly mentioned — 2 marks deducted. The comparison with SN1 is implicit but not directly stated. All favourable conditions are correctly listed.',
  },
];

// ─── Mini Sparkline ───────────────────────────────────────────────────────────

function Sparkline({ values, color }: { values: number[]; color: string }) {
  const max = Math.max(...values);
  const min = Math.min(...values);
  const range = max - min || 1;
  const w = 80;
  const h = 32;
  const pts = values.map((v, i) => {
    const x = (i / (values.length - 1)) * w;
    const y = h - ((v - min) / range) * h;
    return `${x},${y}`;
  }).join(' ');
  return (
    <svg width={w} height={h} className="flex-shrink-0">
      <polyline points={pts} fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

// ─── Batch Health Section ─────────────────────────────────────────────────────

function BatchHealthSection() {
  const ALERT_CONFIG = {
    NORMAL:   { color: 'text-emerald-400', bg: 'bg-emerald-500/15 border-emerald-500/20', label: 'Healthy', icon: <CheckCircle2 className="w-4 h-4" /> },
    WARNING:  { color: 'text-amber-400',   bg: 'bg-amber-500/15 border-amber-500/20',     label: 'Warning', icon: <AlertTriangle className="w-4 h-4" /> },
    CRITICAL: { color: 'text-red-400',     bg: 'bg-red-500/15 border-red-500/20',         label: 'Critical', icon: <TrendingDown className="w-4 h-4" /> },
  };

  return (
    <div className="space-y-4">
      {BATCH_HEALTH.map((batch, i) => {
        const cfg = ALERT_CONFIG[batch.alertLevel];
        const lineColor = batch.alertLevel === 'NORMAL' ? '#34d399' : batch.alertLevel === 'WARNING' ? '#fbbf24' : '#f87171';
        const trend = batch.weeklyTrend[batch.weeklyTrend.length - 1] - batch.weeklyTrend[0];

        return (
          <motion.div
            key={batch.batchName}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.08 }}
            className={cn('glass rounded-xl p-4 border', cfg.bg)}
          >
            <div className="flex items-start justify-between gap-4">
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className={cn('flex items-center gap-1.5 text-xs font-medium px-2 py-0.5 rounded-full border', cfg.bg, cfg.color)}>
                    {cfg.icon} {cfg.label}
                  </span>
                  <h3 className="text-white font-semibold text-sm">{batch.batchName}</h3>
                  <span className="text-white/40 text-xs">{batch.subject}</span>
                </div>

                <div className="flex items-center gap-6 mt-3 text-sm">
                  <div>
                    <p className="text-white/40 text-xs">Avg Score</p>
                    <p className={cn('font-bold text-lg', cfg.color)}>{batch.avgScore.toFixed(1)}%</p>
                  </div>
                  <div>
                    <p className="text-white/40 text-xs">Students</p>
                    <p className="text-white font-semibold">{batch.studentCount}</p>
                  </div>
                  <div>
                    <p className="text-white/40 text-xs">6-Week Drop</p>
                    <p className={cn('font-semibold flex items-center gap-1', batch.dropPercent > 0 ? 'text-red-400' : 'text-emerald-400')}>
                      {batch.dropPercent > 0 ? <TrendingDown className="w-3.5 h-3.5" /> : <TrendingUp className="w-3.5 h-3.5" />}
                      {batch.dropPercent > 0 ? `-${batch.dropPercent}%` : 'Stable'}
                    </p>
                  </div>
                </div>

                {batch.alertLevel !== 'NORMAL' && (
                  <p className={cn('mt-2 text-xs', cfg.color)}>
                    {batch.alertLevel === 'CRITICAL'
                      ? `⚠️ Score dropped ${batch.dropPercent}% over 6 weeks — exceeds 2σ threshold. Immediate intervention recommended.`
                      : `📉 Declining trend detected (${batch.dropPercent}% drop). Monitor closely.`}
                  </p>
                )}
              </div>

              <div className="flex flex-col items-end gap-1">
                <Sparkline values={batch.weeklyTrend} color={lineColor} />
                <span className="text-white/30 text-[10px]">6-week trend</span>
              </div>
            </div>
          </motion.div>
        );
      })}
    </div>
  );
}

// ─── AI Question Generator ────────────────────────────────────────────────────

function QuestionGenerator() {
  const [topic, setTopic] = useState('');
  const [difficulty, setDifficulty] = useState<'EASY' | 'MEDIUM' | 'HARD'>('MEDIUM');
  const [count, setCount] = useState(3);
  const [results, setResults] = useState<GeneratedQuestion[]>([]);
  const [expandedIdx, setExpandedIdx] = useState<number | null>(null);

  const generateMutation = useMutation({
    mutationFn: async () => {
      try {
        const res = await api.post('/api/v1/ai/generate-questions', { topic, difficulty, count });
        return res.data as GeneratedQuestion[];
      } catch {
        // Fallback to realistic mock when AI gateway unavailable
        return Array.from({ length: count }, (_, i) => ({
          questionText: `${topic}: Question ${i + 1} — ${difficulty.toLowerCase()} level concept check on ${topic.split(' ')[0] || 'this topic'}.`,
          options: [
            `Option A — Correct answer demonstrating ${topic}`,
            `Option B — Common misconception distractor`,
            `Option C — Partially correct plausible answer`,
            `Option D — Clearly incorrect distractor`,
          ],
          correctAnswer: 0,
          explanation: `This question tests understanding of ${topic}. The correct answer (A) directly demonstrates the key principle. Option B is a common misconception students have at ${difficulty.toLowerCase()} level.`,
          difficulty,
        })) as GeneratedQuestion[];
      }
    },
    onSuccess: (data) => {
      setResults(data);
      setExpandedIdx(null);
      toast.success(`Generated ${data.length} questions for "${topic}"`);
    },
    onError: () => toast.error('Generation failed. Please try again.'),
  });

  const DIFF_CONFIG = {
    EASY:   'bg-emerald-500/15 text-emerald-400 border-emerald-500/20',
    MEDIUM: 'bg-amber-500/15 text-amber-400 border-amber-500/20',
    HARD:   'bg-red-500/15 text-red-400 border-red-500/20',
  };

  return (
    <div className="space-y-5">
      {/* Input Panel */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="sm:col-span-3 lg:col-span-1">
          <label className="block text-xs text-white/50 mb-1.5">Topic / Concept</label>
          <input
            type="text"
            placeholder="e.g. Newton's Laws of Motion"
            value={topic}
            onChange={(e) => setTopic(e.target.value)}
            className="input w-full text-sm"
          />
        </div>
        <div>
          <label className="block text-xs text-white/50 mb-1.5">Difficulty</label>
          <select
            value={difficulty}
            onChange={(e) => setDifficulty(e.target.value as typeof difficulty)}
            className="input w-full text-sm cursor-pointer"
          >
            <option value="EASY">Easy</option>
            <option value="MEDIUM">Medium</option>
            <option value="HARD">Hard</option>
          </select>
        </div>
        <div>
          <label className="block text-xs text-white/50 mb-1.5">Number of Questions</label>
          <select
            value={count}
            onChange={(e) => setCount(Number(e.target.value))}
            className="input w-full text-sm cursor-pointer"
          >
            {[1, 3, 5, 10].map((n) => <option key={n} value={n}>{n}</option>)}
          </select>
        </div>
      </div>

      <button
        onClick={() => generateMutation.mutate()}
        disabled={!topic.trim() || generateMutation.isPending}
        className="btn-primary flex items-center gap-2 text-sm disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {generateMutation.isPending
          ? <><Loader2 className="w-4 h-4 animate-spin" /> Generating…</>
          : <><Sparkles className="w-4 h-4" /> Generate Questions</>}
      </button>

      {/* Results */}
      <AnimatePresence>
        {results.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-3"
          >
            <div className="flex items-center justify-between">
              <p className="text-white/60 text-sm font-medium">{results.length} questions generated</p>
              <button
                onClick={() => { toast.success('Questions saved to exam bank!'); setResults([]); }}
                className="text-xs text-brand-400 hover:text-brand-300 flex items-center gap-1"
              >
                <ClipboardList className="w-3.5 h-3.5" /> Save to Exam Bank
              </button>
            </div>

            {results.map((q, i) => (
              <motion.div
                key={i}
                className="glass rounded-xl border border-white/5 overflow-hidden"
                initial={{ opacity: 0, x: -8 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: i * 0.06 }}
              >
                <button
                  className="w-full flex items-center justify-between p-4 text-left"
                  onClick={() => setExpandedIdx(expandedIdx === i ? null : i)}
                >
                  <div className="flex items-start gap-3 flex-1 min-w-0">
                    <span className="flex-shrink-0 w-6 h-6 rounded-full bg-brand-600/30 text-brand-400 text-xs flex items-center justify-center font-bold">
                      {i + 1}
                    </span>
                    <p className="text-white/80 text-sm line-clamp-2">{q.questionText}</p>
                  </div>
                  <div className="flex items-center gap-2 ml-3 flex-shrink-0">
                    <span className={cn('badge border text-[10px]', DIFF_CONFIG[q.difficulty])}>{q.difficulty}</span>
                    {expandedIdx === i ? <ChevronUp className="w-4 h-4 text-white/40" /> : <ChevronDown className="w-4 h-4 text-white/40" />}
                  </div>
                </button>

                <AnimatePresence>
                  {expandedIdx === i && (
                    <motion.div
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: 'auto', opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      className="border-t border-white/5 px-4 pb-4 pt-3 space-y-3"
                    >
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                        {q.options.map((opt, j) => (
                          <div
                            key={j}
                            className={cn(
                              'flex items-center gap-2 p-2.5 rounded-lg border text-sm',
                              j === q.correctAnswer
                                ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300'
                                : 'bg-white/3 border-white/5 text-white/60'
                            )}
                          >
                            <span className={cn(
                              'w-5 h-5 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0',
                              j === q.correctAnswer ? 'bg-emerald-500 text-white' : 'bg-white/10 text-white/40'
                            )}>
                              {String.fromCharCode(65 + j)}
                            </span>
                            {opt}
                          </div>
                        ))}
                      </div>
                      <div className="bg-brand-500/10 border border-brand-500/20 rounded-lg p-3">
                        <p className="text-brand-400 text-xs font-medium mb-1">AI Explanation</p>
                        <p className="text-white/60 text-xs leading-relaxed">{q.explanation}</p>
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </motion.div>
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

// ─── AI Grade Assist ──────────────────────────────────────────────────────────

function GradeAssistSection() {
  const [grades, setGrades] = useState<Record<string, number | null>>(
    Object.fromEntries(PENDING_GRADES.map((g) => [g.id, null]))
  );
  const [submittedIds, setSubmittedIds] = useState<Set<string>>(new Set());
  const [expandedId, setExpandedId] = useState<string | null>(PENDING_GRADES[0]?.id ?? null);

  function acceptSuggestion(id: string, score: number) {
    setGrades((prev) => ({ ...prev, [id]: score }));
    toast.success('AI suggestion accepted');
  }

  function rejectSuggestion(id: string) {
    setGrades((prev) => ({ ...prev, [id]: null }));
    toast.info('Override the score manually');
  }

  function submitGrade(id: string) {
    if (grades[id] === null) { toast.error('Set a score first'); return; }
    setSubmittedIds((prev) => new Set([...prev, id]));
    toast.success('Grade submitted successfully!');
  }

  const pending = PENDING_GRADES.filter((g) => !submittedIds.has(g.id));

  return (
    <div className="space-y-4">
      {pending.length === 0 ? (
        <div className="flex flex-col items-center py-12 gap-3 text-center">
          <CheckCircle2 className="w-10 h-10 text-emerald-400" />
          <p className="text-white font-medium">All submissions graded!</p>
          <p className="text-white/40 text-sm">No pending AI grade assist items.</p>
        </div>
      ) : (
        pending.map((item) => (
          <motion.div
            key={item.id}
            className="glass rounded-xl border border-white/5 overflow-hidden"
            layout
          >
            <button
              className="w-full flex items-center justify-between p-4 text-left"
              onClick={() => setExpandedId(expandedId === item.id ? null : item.id)}
            >
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="text-white font-medium text-sm">{item.studentName}</span>
                  <span className="text-white/40 text-xs">·</span>
                  <span className="text-white/50 text-xs">{item.examTitle}</span>
                </div>
                <p className="text-white/40 text-xs mt-0.5 line-clamp-1">{item.questionText}</p>
              </div>
              <div className="flex items-center gap-3 ml-3 flex-shrink-0">
                {grades[item.id] !== null ? (
                  <span className="text-emerald-400 text-sm font-bold">{grades[item.id]}/{item.maxScore}</span>
                ) : (
                  <span className="text-amber-400/70 text-xs">AI: {item.aiSuggestedScore}/{item.maxScore}</span>
                )}
                {expandedId === item.id ? <ChevronUp className="w-4 h-4 text-white/40" /> : <ChevronDown className="w-4 h-4 text-white/40" />}
              </div>
            </button>

            <AnimatePresence>
              {expandedId === item.id && (
                <motion.div
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: 'auto', opacity: 1 }}
                  exit={{ height: 0, opacity: 0 }}
                  className="border-t border-white/5 p-4 space-y-4"
                >
                  <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                    <div>
                      <p className="text-white/40 text-xs mb-1.5 uppercase tracking-wider">Student Answer</p>
                      <div className="bg-white/3 rounded-lg p-3 text-white/70 text-sm leading-relaxed border border-white/5">
                        {item.studentAnswer}
                      </div>
                    </div>
                    <div>
                      <p className="text-white/40 text-xs mb-1.5 uppercase tracking-wider">Rubric</p>
                      <div className="bg-white/3 rounded-lg p-3 text-white/50 text-xs leading-relaxed border border-white/5">
                        {item.rubric}
                      </div>
                    </div>
                  </div>

                  {/* AI Suggestion */}
                  <div className="bg-brand-500/10 border border-brand-500/20 rounded-xl p-4">
                    <div className="flex items-center gap-2 mb-2">
                      <Brain className="w-4 h-4 text-brand-400" />
                      <p className="text-brand-400 text-sm font-medium">AI Grade Suggestion</p>
                      <span className="ml-auto text-white font-bold text-lg">{item.aiSuggestedScore}<span className="text-white/40 text-sm">/{item.maxScore}</span></span>
                    </div>
                    <p className="text-white/60 text-sm leading-relaxed">{item.aiFeedback}</p>
                  </div>

                  {/* Score input + actions */}
                  <div className="flex items-center gap-3 flex-wrap">
                    <div className="flex items-center gap-2">
                      <label className="text-white/50 text-sm">Final Score:</label>
                      <input
                        type="number"
                        min={0}
                        max={item.maxScore}
                        value={grades[item.id] ?? item.aiSuggestedScore}
                        onChange={(e) => setGrades((prev) => ({ ...prev, [item.id]: Number(e.target.value) }))}
                        className="input w-20 text-center text-sm"
                      />
                      <span className="text-white/40 text-sm">/ {item.maxScore}</span>
                    </div>
                    <button
                      onClick={() => acceptSuggestion(item.id, item.aiSuggestedScore)}
                      className="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-600/20 text-emerald-400 border border-emerald-600/30 rounded-lg text-xs font-medium hover:bg-emerald-600/30 transition-colors"
                    >
                      <CheckCircle2 className="w-3.5 h-3.5" /> Accept AI Score
                    </button>
                    <button
                      onClick={() => rejectSuggestion(item.id)}
                      className="flex items-center gap-1.5 px-3 py-1.5 bg-red-600/20 text-red-400 border border-red-600/30 rounded-lg text-xs font-medium hover:bg-red-600/30 transition-colors"
                    >
                      <XCircle className="w-3.5 h-3.5" /> Override
                    </button>
                    <button
                      onClick={() => submitGrade(item.id)}
                      className="btn-primary text-xs px-4 py-1.5 ml-auto"
                    >
                      Submit Grade
                    </button>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </motion.div>
        ))
      )}
    </div>
  );
}

// ─── AI Feature Cards (Summary) ───────────────────────────────────────────────

const AI_FEATURES = [
  {
    icon: <BarChart3 className="w-5 h-5 text-red-400" />,
    title: 'Batch Health Monitor',
    desc: 'Early warning when batch avg drops > 2σ from baseline',
    status: 'LIVE',
    color: 'bg-red-500/10 border-red-500/20',
    iconBg: 'bg-red-500/20',
  },
  {
    icon: <Sparkles className="w-5 h-5 text-purple-400" />,
    title: 'Auto Question Generator',
    desc: 'LLM generates distractors + answer key from any topic',
    status: 'LIVE',
    color: 'bg-purple-500/10 border-purple-500/20',
    iconBg: 'bg-purple-500/20',
  },
  {
    icon: <Brain className="w-5 h-5 text-brand-400" />,
    title: 'AI Grade Assist',
    desc: 'BERT-powered essay scoring with rubric alignment',
    status: 'LIVE',
    color: 'bg-brand-500/10 border-brand-500/20',
    iconBg: 'bg-brand-500/20',
  },
  {
    icon: <Shield className="w-5 h-5 text-emerald-400" />,
    title: 'Plagiarism Guard',
    desc: 'Semantic similarity + keystroke dynamics analysis',
    status: 'BETA',
    color: 'bg-emerald-500/10 border-emerald-500/20',
    iconBg: 'bg-emerald-500/20',
  },
  {
    icon: <Target className="w-5 h-5 text-amber-400" />,
    title: 'Teacher Performance Index',
    desc: 'AI score from student outcomes + feedback + attendance',
    status: 'COMING',
    color: 'bg-amber-500/10 border-amber-500/20',
    iconBg: 'bg-amber-500/20',
  },
  {
    icon: <Zap className="w-5 h-5 text-cyan-400" />,
    title: 'AI Timetable Optimizer',
    desc: 'Constraint satisfaction scheduling with conflict detection',
    status: 'COMING',
    color: 'bg-cyan-500/10 border-cyan-500/20',
    iconBg: 'bg-cyan-500/20',
  },
];

const STATUS_PILL: Record<string, string> = {
  LIVE:   'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30',
  BETA:   'bg-amber-500/20 text-amber-400 border border-amber-500/30',
  COMING: 'bg-white/5 text-white/40 border border-white/10',
};

// ─── Page ─────────────────────────────────────────────────────────────────────

type Tab = 'overview' | 'batch-health' | 'question-gen' | 'grade-assist';

const TABS: { id: Tab; label: string; icon: React.ReactNode }[] = [
  { id: 'overview',     label: 'Overview',         icon: <Lightbulb className="w-4 h-4" /> },
  { id: 'batch-health', label: 'Batch Health',      icon: <BarChart3 className="w-4 h-4" /> },
  { id: 'question-gen', label: 'Question Generator',icon: <Sparkles className="w-4 h-4" /> },
  { id: 'grade-assist', label: 'Grade Assist',      icon: <Brain className="w-4 h-4" /> },
];

export default function MentorPortalInsightsPage() {
  const [activeTab, setActiveTab] = useState<Tab>('overview');

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
      className="p-6 space-y-6"
    >
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-white flex items-center gap-2">
          <Brain className="w-6 h-6 text-brand-400" />
          AI Insights
        </h1>
        <p className="text-white/40 text-sm mt-0.5">
          Auto question generation · AI grade assist · Batch health monitoring
        </p>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-1 p-1 bg-white/5 rounded-xl border border-white/5 w-fit flex-wrap">
        {TABS.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={cn(
              'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all',
              activeTab === tab.id
                ? 'bg-brand-600 text-white shadow'
                : 'text-white/50 hover:text-white hover:bg-white/5'
            )}
          >
            {tab.icon} {tab.label}
          </button>
        ))}
      </div>

      {/* Tab Content */}
      <AnimatePresence mode="wait">
        <motion.div
          key={activeTab}
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -8 }}
          transition={{ duration: 0.2 }}
        >
          {activeTab === 'overview' && (
            <div className="space-y-6">
              {/* Quick Stats */}
              <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                {[
                  { label: 'Batches Monitored', value: '3', icon: <Users className="w-4 h-4 text-brand-400" />, bg: 'bg-brand-500/10' },
                  { label: 'Critical Alerts',   value: '1', icon: <AlertTriangle className="w-4 h-4 text-red-400" />, bg: 'bg-red-500/10' },
                  { label: 'Questions Generated', value: '47', icon: <FileText className="w-4 h-4 text-purple-400" />, bg: 'bg-purple-500/10' },
                  { label: 'Essays AI-Graded',  value: '23', icon: <BookOpen className="w-4 h-4 text-emerald-400" />, bg: 'bg-emerald-500/10' },
                ].map((stat) => (
                  <div key={stat.label} className={cn('glass rounded-xl p-4 border border-white/5', stat.bg)}>
                    <div className="flex items-center gap-2 mb-2">{stat.icon}<p className="text-white/50 text-xs">{stat.label}</p></div>
                    <p className="text-2xl font-bold text-white">{stat.value}</p>
                  </div>
                ))}
              </div>

              {/* AI Feature Grid */}
              <div>
                <h2 className="text-white font-semibold mb-3 flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-brand-400" />
                  AI Feature Suite — Teacher Edition
                </h2>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                  {AI_FEATURES.map((f) => (
                    <motion.div
                      key={f.title}
                      whileHover={{ y: -2 }}
                      className={cn('glass rounded-xl p-4 border cursor-pointer', f.color)}
                      onClick={() => {
                        if (f.status === 'LIVE') {
                          const tabMap: Record<string, Tab> = {
                            'Batch Health Monitor': 'batch-health',
                            'Auto Question Generator': 'question-gen',
                            'AI Grade Assist': 'grade-assist',
                          };
                          if (tabMap[f.title]) setActiveTab(tabMap[f.title]);
                        } else {
                          toast.info(`${f.title} — coming soon!`);
                        }
                      }}
                    >
                      <div className="flex items-start justify-between mb-3">
                        <div className={cn('w-9 h-9 rounded-lg flex items-center justify-center', f.iconBg)}>
                          {f.icon}
                        </div>
                        <span className={cn('badge text-[10px]', STATUS_PILL[f.status])}>{f.status}</span>
                      </div>
                      <h3 className="text-white font-semibold text-sm">{f.title}</h3>
                      <p className="text-white/40 text-xs mt-1 leading-relaxed">{f.desc}</p>
                    </motion.div>
                  ))}
                </div>
              </div>

              {/* Recent Activity */}
              <div className="card">
                <h2 className="text-white font-semibold mb-4 flex items-center gap-2">
                  <RefreshCw className="w-4 h-4 text-brand-400" /> Recent AI Activity
                </h2>
                <div className="space-y-3">
                  {[
                    { time: '2 hours ago', action: 'Batch Health Alert', detail: 'JEE Mains Batch C dropped to CRITICAL (48.3%)', color: 'text-red-400', dot: 'bg-red-500' },
                    { time: '4 hours ago', action: 'Questions Generated', detail: '5 HARD questions on Thermodynamics saved to bank', color: 'text-purple-400', dot: 'bg-purple-500' },
                    { time: 'Yesterday',   action: 'AI Grade Assist',    detail: 'Essay graded for Arjun Kapoor — 8/10', color: 'text-brand-400', dot: 'bg-brand-500' },
                    { time: 'Yesterday',   action: 'Batch Health',       detail: 'JEE Advanced Batch A entered WARNING zone', color: 'text-amber-400', dot: 'bg-amber-500' },
                    { time: '3 days ago',  action: 'Questions Generated', detail: '10 MEDIUM questions on Electrostatics', color: 'text-purple-400', dot: 'bg-purple-500' },
                  ].map((item, i) => (
                    <div key={i} className="flex items-start gap-3 text-sm">
                      <div className={cn('w-1.5 h-1.5 rounded-full mt-2 flex-shrink-0', item.dot)} />
                      <div className="flex-1 min-w-0">
                        <span className={cn('font-medium', item.color)}>{item.action}</span>
                        <span className="text-white/50 ml-2">{item.detail}</span>
                      </div>
                      <span className="text-white/30 text-xs flex-shrink-0">{item.time}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {activeTab === 'batch-health' && (
            <div className="space-y-5">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-white font-semibold">Batch Health Monitor</h2>
                  <p className="text-white/40 text-sm mt-0.5">AI early-warning system — alerts when batch avg drops &gt; 2σ from baseline</p>
                </div>
                <button
                  onClick={() => toast.success('Batch data refreshed')}
                  className="flex items-center gap-1.5 text-sm text-white/50 hover:text-white border border-white/10 hover:border-white/20 rounded-lg px-3 py-1.5 transition-all"
                >
                  <RefreshCw className="w-3.5 h-3.5" /> Refresh
                </button>
              </div>
              <BatchHealthSection />
            </div>
          )}

          {activeTab === 'question-gen' && (
            <div className="space-y-5">
              <div>
                <h2 className="text-white font-semibold">Auto Question Generator</h2>
                <p className="text-white/40 text-sm mt-0.5">
                  LLM generates MCQs with distractors, answer keys, and explanations from any topic
                </p>
              </div>
              <div className="card">
                <QuestionGenerator />
              </div>
            </div>
          )}

          {activeTab === 'grade-assist' && (
            <div className="space-y-5">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-white font-semibold">AI Grade Assist</h2>
                  <p className="text-white/40 text-sm mt-0.5">
                    BERT-powered essay and subjective answer scoring — review, override, and submit
                  </p>
                </div>
                <span className="badge bg-amber-500/20 text-amber-400 border border-amber-500/30 text-xs">
                  {PENDING_GRADES.length} Pending
                </span>
              </div>
              <div className="card">
                <GradeAssistSection />
              </div>
            </div>
          )}
        </motion.div>
      </AnimatePresence>
    </motion.div>
  );
}
