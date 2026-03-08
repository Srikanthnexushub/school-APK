import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import {
  CheckCircle2,
  XCircle,
  ChevronDown,
  ChevronUp,
  RefreshCw,
  LayoutDashboard,
  BrainCircuit,
  Info,
  ArrowLeft,
} from 'lucide-react';
import api from '../../lib/api';
import { cn } from '../../lib/utils';
import { Skeleton } from '../../components/ui/LoadingSkeleton';

// ─── Types ────────────────────────────────────────────────────────────────────

type LetterGrade = 'A' | 'B' | 'C' | 'D' | 'F';

interface QuestionResult {
  questionId: string;
  questionText: string;
  selectedOption: string | null;
  correctOption: string;
  isCorrect: boolean;
  options: { key: string; text: string }[];
}

interface ExamResult {
  enrollmentId: string;
  examName: string;
  subject: string;
  score: number;           // 0–100
  thetaEstimate: number;   // IRT ability
  letterGrade: LetterGrade;
  correctCount: number;
  totalQuestions: number;
  timeTakenSeconds: number;
  questionResults: QuestionResult[];
  aiInsights: {
    weakTopics: string[];
    strongTopics: string[];
    recommendation: string;
  };
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

const GRADE_STYLES: Record<LetterGrade, { bg: string; text: string; border: string; label: string }> = {
  A: { bg: 'bg-emerald-500/15', text: 'text-emerald-400', border: 'border-emerald-500/30', label: 'Excellent' },
  B: { bg: 'bg-brand-500/15',   text: 'text-brand-400',   border: 'border-brand-500/30',   label: 'Good' },
  C: { bg: 'bg-amber-500/15',   text: 'text-amber-400',   border: 'border-amber-500/30',   label: 'Average' },
  D: { bg: 'bg-orange-500/15',  text: 'text-orange-400',  border: 'border-orange-500/30',  label: 'Below Average' },
  F: { bg: 'bg-red-500/15',     text: 'text-red-400',     border: 'border-red-500/30',     label: 'Failing' },
};

function formatTime(seconds: number): string {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  if (h > 0) return `${h}h ${m}m`;
  if (m > 0) return `${m}m ${s}s`;
  return `${s}s`;
}

// ─── CSS Confetti ─────────────────────────────────────────────────────────────

function Confetti({ score }: { score: number }) {
  if (score < 60) return null;

  const COLORS = ['#6366f1', '#10b981', '#f59e0b', '#ec4899', '#06b6d4', '#8b5cf6'];
  const dots = Array.from({ length: 28 }, (_, i) => ({
    id: i,
    color: COLORS[i % COLORS.length],
    left: `${(i * 3.7) % 100}%`,
    delay: `${(i * 0.07).toFixed(2)}s`,
    duration: `${0.8 + (i % 5) * 0.15}s`,
    size: 6 + (i % 4) * 2,
  }));

  return (
    <div className="absolute inset-x-0 top-0 h-40 overflow-hidden pointer-events-none">
      {dots.map((d) => (
        <div
          key={d.id}
          className="absolute rounded-full opacity-0"
          style={{
            left: d.left,
            top: '-10px',
            width: d.size,
            height: d.size,
            backgroundColor: d.color,
            animation: `confettiFall ${d.duration} ${d.delay} ease-in forwards`,
          }}
        />
      ))}
      <style>{`
        @keyframes confettiFall {
          0%   { transform: translateY(0) rotate(0deg);   opacity: 1; }
          100% { transform: translateY(160px) rotate(720deg); opacity: 0; }
        }
      `}</style>
    </div>
  );
}

// ─── Question Accordion Item ──────────────────────────────────────────────────

function QuestionItem({ result, index }: { result: QuestionResult; index: number }) {
  const [open, setOpen] = useState(false);

  return (
    <div className="glass rounded-xl overflow-hidden">
      <button
        onClick={() => setOpen((p) => !p)}
        className="w-full flex items-center gap-3 p-4 text-left hover:bg-white/3 transition-colors"
      >
        <div
          className={cn(
            'w-7 h-7 rounded-full flex items-center justify-center flex-shrink-0',
            result.isCorrect
              ? 'bg-emerald-500/15 text-emerald-400'
              : 'bg-red-500/15 text-red-400'
          )}
        >
          {result.isCorrect ? (
            <CheckCircle2 className="w-4 h-4" />
          ) : (
            <XCircle className="w-4 h-4" />
          )}
        </div>
        <span className="flex-1 text-sm text-white/80 line-clamp-1">
          <span className="text-white/40 mr-2">Q{index + 1}.</span>
          {result.questionText}
        </span>
        {open ? (
          <ChevronUp className="w-4 h-4 text-white/30 flex-shrink-0" />
        ) : (
          <ChevronDown className="w-4 h-4 text-white/30 flex-shrink-0" />
        )}
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="overflow-hidden"
          >
            <div className="px-4 pb-4 space-y-2 border-t border-white/5">
              {result.options.map((opt) => {
                const isSelected = opt.key === result.selectedOption;
                const isCorrect = opt.key === result.correctOption;
                return (
                  <div
                    key={opt.key}
                    className={cn(
                      'flex items-center gap-3 p-3 rounded-lg text-sm mt-2',
                      isCorrect
                        ? 'bg-emerald-500/10 border border-emerald-500/20'
                        : isSelected && !isCorrect
                        ? 'bg-red-500/10 border border-red-500/20'
                        : 'bg-white/3'
                    )}
                  >
                    <span
                      className={cn(
                        'w-6 h-6 rounded-full border flex items-center justify-center text-xs font-bold flex-shrink-0',
                        isCorrect
                          ? 'border-emerald-500 bg-emerald-500/20 text-emerald-400'
                          : isSelected && !isCorrect
                          ? 'border-red-500 bg-red-500/20 text-red-400'
                          : 'border-white/15 text-white/30'
                      )}
                    >
                      {opt.key}
                    </span>
                    <span
                      className={cn(
                        isCorrect
                          ? 'text-emerald-300'
                          : isSelected && !isCorrect
                          ? 'text-red-300'
                          : 'text-white/50'
                      )}
                    >
                      {opt.text}
                    </span>
                    {isCorrect && (
                      <span className="ml-auto text-emerald-400 text-xs font-medium">Correct</span>
                    )}
                    {isSelected && !isCorrect && (
                      <span className="ml-auto text-red-400 text-xs font-medium">Your answer</span>
                    )}
                  </div>
                );
              })}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

// ─── Theta Tooltip ────────────────────────────────────────────────────────────

function ThetaTooltip() {
  const [show, setShow] = useState(false);
  return (
    <div className="relative inline-block">
      <button
        onMouseEnter={() => setShow(true)}
        onMouseLeave={() => setShow(false)}
        className="text-white/30 hover:text-white/60 transition-colors"
      >
        <Info className="w-4 h-4" />
      </button>
      <AnimatePresence>
        {show && (
          <motion.div
            initial={{ opacity: 0, y: 4 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 4 }}
            className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 w-56 glass rounded-xl p-3 text-xs text-white/60 text-center shadow-xl z-10"
          >
            Your estimated ability level based on Item Response Theory. Higher θ means stronger
            subject mastery.
            <div className="absolute top-full left-1/2 -translate-x-1/2 border-4 border-transparent border-t-white/10" />
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function ExamResultsPage() {
  const { enrollmentId } = useParams<{ enrollmentId: string }>();
  const navigate = useNavigate();

  const { data: result, isLoading, isError, refetch } = useQuery<ExamResult>({
    queryKey: ['exam-results', enrollmentId],
    queryFn: async () => {
      const res = await api.get(`/api/v1/assess/submissions/${enrollmentId}/results`);
      return res.data;
    },
    retry: 1,
  });

  if (isLoading) {
    return (
      <div className="p-6 space-y-6 max-w-3xl mx-auto">
        <Skeleton className="h-48 rounded-2xl" />
        <Skeleton className="h-32 rounded-2xl" />
        <Skeleton className="h-64 rounded-2xl" />
      </div>
    );
  }

  if (isError || !result) {
    return (
      <div className="p-6 flex items-center justify-center min-h-64">
        <div className="glass rounded-2xl p-8 text-center max-w-sm">
          <XCircle className="w-10 h-10 text-red-400 mx-auto mb-4" />
          <p className="text-white/70 mb-4">Failed to load results.</p>
          <button onClick={() => refetch()} className="btn-primary text-sm">
            Retry
          </button>
        </div>
      </div>
    );
  }

  const grade = result.letterGrade as LetterGrade;
  const gradeStyle = GRADE_STYLES[grade] ?? GRADE_STYLES.F;

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
      className="p-6 space-y-6 max-w-3xl mx-auto"
    >
      {/* ── Score header ───────────────────────────────────────────────── */}
      <div className="relative glass rounded-2xl p-8 text-center overflow-hidden">
        <Confetti score={result.score} />

        <div className="relative z-10">
          <p className="text-white/40 text-sm mb-2">{result.examName}</p>

          {/* Score ring */}
          <div className="flex justify-center mb-4">
            <div className="relative w-32 h-32">
              <svg className="w-32 h-32 -rotate-90" viewBox="0 0 128 128">
                <circle cx="64" cy="64" r="52" fill="none" stroke="rgba(255,255,255,0.06)" strokeWidth="10" />
                <circle
                  cx="64"
                  cy="64"
                  r="52"
                  fill="none"
                  stroke={result.score >= 60 ? '#6366f1' : '#ef4444'}
                  strokeWidth="10"
                  strokeDasharray={2 * Math.PI * 52}
                  strokeDashoffset={2 * Math.PI * 52 * (1 - result.score / 100)}
                  strokeLinecap="round"
                  style={{ transition: 'stroke-dashoffset 1s ease' }}
                />
              </svg>
              <div className="absolute inset-0 flex flex-col items-center justify-center">
                <span className="text-3xl font-bold text-white">{result.score}</span>
                <span className="text-white/40 text-xs">%</span>
              </div>
            </div>
          </div>

          {/* Grade badge */}
          <div className="flex items-center justify-center gap-3 mb-4">
            <span
              className={cn(
                'badge text-2xl font-bold px-5 py-2 border',
                gradeStyle.bg,
                gradeStyle.text,
                gradeStyle.border
              )}
            >
              {grade}
            </span>
            <span className={cn('text-sm font-medium', gradeStyle.text)}>{gradeStyle.label}</span>
          </div>

          {/* IRT Theta */}
          <div className="flex items-center justify-center gap-2 text-white/50 text-sm">
            <span>Ability estimate: θ = {result.thetaEstimate?.toFixed(2) ?? '—'}</span>
            <ThetaTooltip />
          </div>

          {/* Quick stats */}
          <div className="grid grid-cols-3 gap-4 mt-6 pt-6 border-t border-white/5">
            {[
              { label: 'Correct',    value: `${result.correctCount}/${result.totalQuestions}` },
              { label: 'Time taken', value: formatTime(result.timeTakenSeconds) },
              { label: 'Subject',    value: result.subject },
            ].map((s) => (
              <div key={s.label} className="text-center">
                <p className="text-white font-bold text-base">{s.value}</p>
                <p className="text-white/30 text-xs mt-0.5">{s.label}</p>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* ── AI Insights ────────────────────────────────────────────────── */}
      {result.aiInsights && (
        <div className="glass rounded-2xl p-6 space-y-4">
          <div className="flex items-center gap-2 mb-2">
            <div className="w-7 h-7 rounded-lg bg-brand-500/20 border border-brand-500/30 flex items-center justify-center">
              <BrainCircuit className="w-4 h-4 text-brand-400" />
            </div>
            <h3 className="font-semibold text-white">AI Insights</h3>
          </div>

          <p className="text-white/60 text-sm leading-relaxed">{result.aiInsights.recommendation}</p>

          <div className="grid sm:grid-cols-2 gap-4">
            {result.aiInsights.weakTopics?.length > 0 && (
              <div className="bg-red-500/8 border border-red-500/15 rounded-xl p-4">
                <p className="text-red-400 text-xs font-semibold uppercase tracking-wider mb-2">
                  Focus Areas
                </p>
                <ul className="space-y-1">
                  {result.aiInsights.weakTopics.map((t) => (
                    <li key={t} className="text-white/60 text-xs flex items-center gap-1.5">
                      <span className="w-1 h-1 rounded-full bg-red-400 flex-shrink-0" />
                      {t}
                    </li>
                  ))}
                </ul>
              </div>
            )}
            {result.aiInsights.strongTopics?.length > 0 && (
              <div className="bg-emerald-500/8 border border-emerald-500/15 rounded-xl p-4">
                <p className="text-emerald-400 text-xs font-semibold uppercase tracking-wider mb-2">
                  Strong Areas
                </p>
                <ul className="space-y-1">
                  {result.aiInsights.strongTopics.map((t) => (
                    <li key={t} className="text-white/60 text-xs flex items-center gap-1.5">
                      <span className="w-1 h-1 rounded-full bg-emerald-400 flex-shrink-0" />
                      {t}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ── Question review ────────────────────────────────────────────── */}
      {result.questionResults?.length > 0 && (
        <div className="space-y-3">
          <h3 className="font-semibold text-white">Question Review</h3>
          {result.questionResults.map((qr, i) => (
            <QuestionItem key={qr.questionId} result={qr} index={i} />
          ))}
        </div>
      )}

      {/* ── Action buttons ─────────────────────────────────────────────── */}
      <div className="flex flex-col sm:flex-row gap-3 pt-2">
        <button
          onClick={() => navigate('/assessments')}
          className="flex-1 btn-ghost border border-white/10 py-2.5 text-sm flex items-center justify-center gap-2"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to Assessments
        </button>
        <button
          onClick={() => navigate('/performance/weak-areas')}
          className="flex-1 bg-amber-500/15 text-amber-400 border border-amber-500/25 hover:bg-amber-500/25 font-medium py-2.5 rounded-xl text-sm transition-all flex items-center justify-center gap-2"
        >
          <LayoutDashboard className="w-4 h-4" />
          Practice Weak Areas
        </button>
        <button
          onClick={() => navigate(`/assessments/${result.enrollmentId}/exam`)}
          className="flex-1 btn-primary py-2.5 text-sm flex items-center justify-center gap-2"
        >
          <RefreshCw className="w-4 h-4" />
          Retake Exam
        </button>
      </div>
    </motion.div>
  );
}
