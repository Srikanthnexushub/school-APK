import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import {
  BookOpen,
  Flag,
  ChevronLeft,
  ChevronRight,
  AlertTriangle,
  CheckCircle2,
} from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { cn } from '../../lib/utils';

// ─── Types ────────────────────────────────────────────────────────────────────

interface ExamSession {
  enrollmentId: string;
  examName: string;
  subject: string;
  totalQuestions: number;
  durationSeconds: number;
  currentQuestionIndex: number;
}

interface Question {
  id: string;
  questionNumber: number;
  text: string;
  options: { key: string; text: string }[];
  difficulty: number; // IRT 1–5
  subject: string;
}

type AnswerState = 'unanswered' | 'answered' | 'flagged';

// ─── Timer ────────────────────────────────────────────────────────────────────

function ExamTimer({
  durationSeconds,
  onExpire,
}: {
  durationSeconds: number;
  onExpire: () => void;
}) {
  const [remaining, setRemaining] = useState(durationSeconds);

  useEffect(() => {
    const interval = setInterval(() => {
      setRemaining((prev) => {
        if (prev <= 1) {
          onExpire();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [onExpire]);

  const h = Math.floor(remaining / 3600);
  const m = Math.floor((remaining % 3600) / 60);
  const s = remaining % 60;
  const isLow = remaining < 300;

  return (
    <div
      className={cn(
        'font-mono text-lg font-bold tabular-nums px-3 py-1.5 rounded-lg',
        isLow
          ? 'text-red-400 animate-pulse bg-red-500/10 border border-red-500/20'
          : 'text-white bg-white/5 border border-white/10'
      )}
    >
      {String(h).padStart(2, '0')}:{String(m).padStart(2, '0')}:{String(s).padStart(2, '0')}
    </div>
  );
}

// ─── Submit Confirm Modal ─────────────────────────────────────────────────────

function SubmitModal({
  isOpen,
  unansweredCount,
  onConfirm,
  onCancel,
  isSubmitting,
}: {
  isOpen: boolean;
  unansweredCount: number;
  onConfirm: () => void;
  onCancel: () => void;
  isSubmitting: boolean;
}) {
  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <motion.div
            className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onCancel}
          />
          <motion.div
            className="relative glass rounded-2xl shadow-2xl border border-white/10 w-full max-w-md p-6"
            initial={{ opacity: 0, scale: 0.95, y: 16 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 16 }}
            transition={{ type: 'spring', stiffness: 400, damping: 30 }}
          >
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 rounded-xl bg-amber-500/15 border border-amber-500/20 flex items-center justify-center">
                <AlertTriangle className="w-5 h-5 text-amber-400" />
              </div>
              <h2 className="text-lg font-semibold text-white">Submit Exam?</h2>
            </div>
            {unansweredCount > 0 ? (
              <p className="text-white/60 text-sm mb-6">
                You have{' '}
                <span className="text-amber-400 font-semibold">{unansweredCount} unanswered</span>{' '}
                {unansweredCount === 1 ? 'question' : 'questions'}. Once submitted, you cannot
                change your answers.
              </p>
            ) : (
              <p className="text-white/60 text-sm mb-6">
                All questions answered. Once submitted, you cannot change your answers.
              </p>
            )}
            <div className="flex gap-3">
              <button onClick={onCancel} className="flex-1 btn-ghost border border-white/10 py-2.5 text-sm">
                Continue exam
              </button>
              <button
                onClick={onConfirm}
                disabled={isSubmitting}
                className="flex-1 bg-brand-600 hover:bg-brand-500 text-white font-medium py-2.5 rounded-xl transition-all text-sm flex items-center justify-center gap-2"
              >
                {isSubmitting ? (
                  <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                ) : (
                  <>
                    <CheckCircle2 className="w-4 h-4" />
                    Submit
                  </>
                )}
              </button>
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function ExamPage() {
  const { enrollmentId } = useParams<{ enrollmentId: string }>();
  const navigate = useNavigate();

  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState<Record<string, string>>({});      // questionId → selectedOption
  const [flagged, setFlagged] = useState<Record<string, boolean>>({});     // questionId → bool
  const [questions, setQuestions] = useState<Question[]>([]);
  const [showSubmitModal, setShowSubmitModal] = useState(false);

  // ── Load session info ──────────────────────────────────────────────────────
  const { data: session, isLoading: sessionLoading } = useQuery<ExamSession>({
    queryKey: ['exam-session', enrollmentId],
    queryFn: async () => {
      const res = await api.get(`/api/v1/assess/enrollments/${enrollmentId}/session`);
      return res.data;
    },
    retry: 1,
  });

  // ── Load first question ────────────────────────────────────────────────────
  const { data: currentQuestion, isFetching: questionFetching } = useQuery<Question>({
    queryKey: ['exam-question', enrollmentId, currentIndex],
    queryFn: async () => {
      const res = await api.get(
        `/api/v1/assess/submissions/${enrollmentId}/questions/${currentIndex}`
      );
      const q: Question = res.data;
      // Accumulate questions list
      setQuestions((prev) => {
        if (prev.find((p) => p.id === q.id)) return prev;
        const updated = [...prev];
        updated[currentIndex] = q;
        return updated;
      });
      return q;
    },
    enabled: !!enrollmentId,
    retry: 1,
  });

  // ── Submit answer mutation ─────────────────────────────────────────────────
  const answerMutation = useMutation({
    mutationFn: (payload: { questionId: string; selectedOption: string }) =>
      api.post(`/api/v1/assess/submissions/${enrollmentId}/answers`, payload),
    onError: () => toast.error('Failed to save answer. Please try again.'),
  });

  // ── Submit exam mutation ───────────────────────────────────────────────────
  const submitMutation = useMutation({
    mutationFn: () => api.post(`/api/v1/assess/submissions/${enrollmentId}/submit`),
    onSuccess: () => {
      toast.success('Exam submitted successfully!');
      navigate(`/assessments/${enrollmentId}/results`, { replace: true });
    },
    onError: () => toast.error('Submission failed. Please try again.'),
  });

  // ── Handle timer expire ────────────────────────────────────────────────────
  const handleTimerExpire = useCallback(() => {
    toast.error('Time is up! Submitting exam...');
    submitMutation.mutate();
  }, [submitMutation]);

  // ── Select option ──────────────────────────────────────────────────────────
  function selectOption(option: string) {
    if (!currentQuestion) return;
    setAnswers((prev) => ({ ...prev, [currentQuestion.id]: option }));
    answerMutation.mutate({ questionId: currentQuestion.id, selectedOption: option });
  }

  // ── Navigate ───────────────────────────────────────────────────────────────
  function goToQuestion(index: number) {
    if (!session) return;
    if (index < 0 || index >= session.totalQuestions) return;
    setCurrentIndex(index);
  }

  // ── Navigator cell color ───────────────────────────────────────────────────
  function getCellStyle(index: number): string {
    const q = questions[index];
    if (!q) return 'bg-white/5 text-white/30 border border-white/10';
    if (flagged[q.id]) return 'bg-amber-500/20 text-amber-400 border border-amber-500/30';
    if (answers[q.id]) return 'bg-brand-500/25 text-brand-300 border border-brand-500/30';
    return 'bg-white/5 text-white/40 border border-white/10';
  }

  const totalQuestions = session?.totalQuestions ?? 30;
  const answeredCount = Object.keys(answers).length;
  const unansweredCount = totalQuestions - answeredCount;

  if (sessionLoading) {
    return (
      <div className="fixed inset-0 bg-surface flex items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <div className="w-10 h-10 border-2 border-brand-500/30 border-t-brand-500 rounded-full animate-spin" />
          <p className="text-white/50 text-sm">Loading exam session…</p>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 bg-surface flex flex-col overflow-hidden">
      {/* ── Top bar ──────────────────────────────────────────────────────── */}
      <div className="flex items-center justify-between px-6 py-3 border-b border-white/5 bg-surface-50/40 backdrop-blur-md flex-shrink-0">
        <div className="flex items-center gap-3">
          <div className="p-1.5 rounded-lg bg-brand-600/20 border border-brand-500/30">
            <BookOpen className="w-4 h-4 text-brand-400" />
          </div>
          <span className="font-bold text-white text-sm hidden sm:block">EduPath</span>
        </div>

        <div className="flex items-center gap-2 text-white/60 text-sm font-medium">
          Question
          <span className="text-white font-bold">{currentIndex + 1}</span>
          <span>/</span>
          <span>{totalQuestions}</span>
        </div>

        {session && (
          <ExamTimer
            durationSeconds={session.durationSeconds}
            onExpire={handleTimerExpire}
          />
        )}
      </div>

      {/* ── Main content ─────────────────────────────────────────────────── */}
      <div className="flex flex-1 overflow-hidden">
        {/* Question area */}
        <div className="flex-1 flex flex-col overflow-y-auto p-6 lg:p-10">
          <AnimatePresence mode="wait">
            {questionFetching ? (
              <motion.div
                key="loading"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="flex-1 flex items-center justify-center"
              >
                <div className="w-8 h-8 border-2 border-brand-500/30 border-t-brand-500 rounded-full animate-spin" />
              </motion.div>
            ) : currentQuestion ? (
              <motion.div
                key={currentQuestion.id}
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -20 }}
                transition={{ duration: 0.25 }}
                className="flex-1"
              >
                {/* Question text */}
                <div className="glass rounded-2xl p-6 mb-6">
                  <div className="flex items-start gap-3 mb-4">
                    <span className="text-brand-400 font-bold text-sm flex-shrink-0 mt-0.5">
                      Q{currentIndex + 1}.
                    </span>
                    <p className="text-white text-base leading-relaxed font-medium">
                      {currentQuestion.text}
                    </p>
                  </div>
                </div>

                {/* Options */}
                <div className="space-y-3">
                  {currentQuestion.options.map((opt) => {
                    const selected = answers[currentQuestion.id] === opt.key;
                    return (
                      <motion.button
                        key={opt.key}
                        onClick={() => selectOption(opt.key)}
                        whileHover={{ scale: 1.005 }}
                        whileTap={{ scale: 0.995 }}
                        className={cn(
                          'w-full flex items-center gap-4 p-4 rounded-xl border text-left transition-all duration-200',
                          selected
                            ? 'bg-brand-500/20 border-brand-500/50 shadow-lg shadow-brand-500/10'
                            : 'bg-surface-50/40 border-white/8 hover:bg-surface-50/80 hover:border-white/15 hover:shadow-md hover:shadow-brand-500/5'
                        )}
                      >
                        <div
                          className={cn(
                            'w-8 h-8 rounded-full border-2 flex items-center justify-center text-sm font-bold flex-shrink-0 transition-all',
                            selected
                              ? 'bg-brand-500 border-brand-500 text-white'
                              : 'border-white/20 text-white/50 bg-white/5'
                          )}
                        >
                          {opt.key}
                        </div>
                        <span
                          className={cn(
                            'text-sm leading-relaxed',
                            selected ? 'text-white font-medium' : 'text-white/70'
                          )}
                        >
                          {opt.text}
                        </span>
                      </motion.button>
                    );
                  })}
                </div>
              </motion.div>
            ) : (
              <motion.div
                key="empty"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                className="flex-1 flex items-center justify-center text-white/30 text-sm"
              >
                Question not available.
              </motion.div>
            )}
          </AnimatePresence>

          {/* Navigation buttons */}
          <div className="flex items-center justify-between mt-8 pt-4 border-t border-white/5">
            <button
              onClick={() => goToQuestion(currentIndex - 1)}
              disabled={currentIndex === 0}
              className={cn(
                'flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium transition-all',
                currentIndex === 0
                  ? 'text-white/20 cursor-not-allowed'
                  : 'btn-ghost border border-white/10'
              )}
            >
              <ChevronLeft className="w-4 h-4" />
              Previous
            </button>

            <span className="text-white/30 text-xs">
              {answeredCount}/{totalQuestions} answered
            </span>

            {currentIndex < totalQuestions - 1 ? (
              <button
                onClick={() => goToQuestion(currentIndex + 1)}
                className="flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium btn-primary"
              >
                Next
                <ChevronRight className="w-4 h-4" />
              </button>
            ) : (
              <button
                onClick={() => setShowSubmitModal(true)}
                className="flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium bg-emerald-600 hover:bg-emerald-500 text-white transition-all"
              >
                <CheckCircle2 className="w-4 h-4" />
                Finish
              </button>
            )}
          </div>
        </div>

        {/* ── Sidebar: Navigator ─────────────────────────────────────────── */}
        <div className="w-72 border-l border-white/5 bg-surface-50/30 flex flex-col p-5 gap-5 flex-shrink-0 overflow-y-auto hidden lg:flex">
          {/* Question grid */}
          <div>
            <p className="text-white/30 text-xs font-medium uppercase tracking-wider mb-3">
              Navigator
            </p>
            <div className="grid grid-cols-5 gap-1.5">
              {Array.from({ length: totalQuestions }).map((_, i) => (
                <button
                  key={i}
                  onClick={() => goToQuestion(i)}
                  className={cn(
                    'w-full aspect-square rounded-lg text-xs font-medium transition-all duration-150 hover:scale-105',
                    i === currentIndex
                      ? 'ring-2 ring-brand-400 ring-offset-1 ring-offset-surface-50'
                      : '',
                    getCellStyle(i)
                  )}
                >
                  {i + 1}
                </button>
              ))}
            </div>
            {/* Legend */}
            <div className="mt-3 flex flex-wrap gap-3 text-xs text-white/30">
              <span className="flex items-center gap-1.5">
                <span className="w-2.5 h-2.5 rounded-sm bg-brand-500/25 border border-brand-500/30 inline-block" />
                Answered
              </span>
              <span className="flex items-center gap-1.5">
                <span className="w-2.5 h-2.5 rounded-sm bg-amber-500/20 border border-amber-500/30 inline-block" />
                Flagged
              </span>
              <span className="flex items-center gap-1.5">
                <span className="w-2.5 h-2.5 rounded-sm bg-white/5 border border-white/10 inline-block" />
                Unanswered
              </span>
            </div>
          </div>

          {/* Subject & difficulty */}
          {currentQuestion && (
            <div className="glass rounded-xl p-4 space-y-3">
              <div>
                <p className="text-white/30 text-xs mb-1">Subject</p>
                <p className="text-white text-sm font-medium">{currentQuestion.subject}</p>
              </div>
              <div>
                <p className="text-white/30 text-xs mb-1.5">Difficulty</p>
                <div className="flex items-center gap-0.5">
                  {Array.from({ length: 5 }).map((_, i) => (
                    <div
                      key={i}
                      className={cn(
                        'w-4 h-2 rounded-sm',
                        i < (currentQuestion.difficulty ?? 3)
                          ? 'bg-brand-500'
                          : 'bg-white/10'
                      )}
                    />
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* Flag button */}
          {currentQuestion && (
            <button
              onClick={() =>
                setFlagged((prev) => ({
                  ...prev,
                  [currentQuestion.id]: !prev[currentQuestion.id],
                }))
              }
              className={cn(
                'flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium border transition-all',
                flagged[currentQuestion.id]
                  ? 'bg-amber-500/15 text-amber-400 border-amber-500/30'
                  : 'bg-white/5 text-white/50 border-white/10 hover:border-amber-500/20 hover:text-amber-400'
              )}
            >
              <Flag className="w-4 h-4" />
              {flagged[currentQuestion.id] ? 'Unflag question' : 'Flag for review'}
            </button>
          )}

          {/* Submit exam */}
          <div className="mt-auto">
            <button
              onClick={() => setShowSubmitModal(true)}
              className="w-full bg-emerald-600 hover:bg-emerald-500 text-white font-medium py-2.5 rounded-xl text-sm transition-all flex items-center justify-center gap-2"
            >
              <CheckCircle2 className="w-4 h-4" />
              Submit Exam
            </button>
          </div>
        </div>
      </div>

      {/* ── Submit modal ─────────────────────────────────────────────────── */}
      <SubmitModal
        isOpen={showSubmitModal}
        unansweredCount={unansweredCount}
        onConfirm={() => submitMutation.mutate()}
        onCancel={() => setShowSubmitModal(false)}
        isSubmitting={submitMutation.isPending}
      />
    </div>
  );
}
