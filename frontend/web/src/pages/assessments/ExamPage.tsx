import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import {
  BookOpen, Flag, ChevronLeft, ChevronRight, AlertTriangle, CheckCircle2,
} from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { cn } from '../../lib/utils';

// ─── Types ────────────────────────────────────────────────────────────────────

interface ExamInfo {
  id: string;
  title?: string;
  name?: string;
  description?: string;
  durationMinutes?: number;
  totalMarks?: number;
  passingMarks?: number;
  status?: string;
}

interface Question {
  id: string;
  questionText: string;
  options: string[];      // plain strings from backend
  correctAnswer?: number; // 0-indexed (hidden during exam)
  marks?: number;
  difficulty?: number;
}

// ─── Timer ────────────────────────────────────────────────────────────────────

function ExamTimer({ durationSeconds, onExpire }: { durationSeconds: number; onExpire: () => void }) {
  const [remaining, setRemaining] = useState(durationSeconds);

  useEffect(() => {
    const interval = setInterval(() => {
      setRemaining((prev) => {
        if (prev <= 1) { onExpire(); return 0; }
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
    <div className={cn(
      'font-mono text-lg font-bold tabular-nums px-3 py-1.5 rounded-lg',
      isLow ? 'text-red-400 animate-pulse bg-red-500/10 border border-red-500/20'
            : 'text-white bg-white/5 border border-white/10'
    )}>
      {String(h).padStart(2, '0')}:{String(m).padStart(2, '0')}:{String(s).padStart(2, '0')}
    </div>
  );
}

// ─── Submit Modal ─────────────────────────────────────────────────────────────

function SubmitModal({
  isOpen, unansweredCount, onConfirm, onCancel, isSubmitting,
}: { isOpen: boolean; unansweredCount: number; onConfirm: () => void; onCancel: () => void; isSubmitting: boolean }) {
  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <motion.div className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onCancel} />
          <motion.div className="relative glass rounded-2xl shadow-2xl border border-white/10 w-full max-w-md p-6"
            initial={{ opacity: 0, scale: 0.95, y: 16 }} animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 16 }} transition={{ type: 'spring', stiffness: 400, damping: 30 }}>
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 rounded-xl bg-amber-500/15 border border-amber-500/20 flex items-center justify-center">
                <AlertTriangle className="w-5 h-5 text-amber-400" />
              </div>
              <h2 className="text-lg font-semibold text-white">Submit Exam?</h2>
            </div>
            <p className="text-white/60 text-sm mb-6">
              {unansweredCount > 0
                ? <><span className="text-amber-400 font-semibold">{unansweredCount} question{unansweredCount > 1 ? 's' : ''}</span> unanswered. Once submitted, you cannot change answers.</>
                : 'All questions answered. Once submitted, you cannot change your answers.'}
            </p>
            <div className="flex gap-3">
              <button onClick={onCancel} className="flex-1 btn-ghost border border-white/10 py-2.5 text-sm">
                Continue exam
              </button>
              <button onClick={onConfirm} disabled={isSubmitting}
                className="flex-1 bg-brand-600 hover:bg-brand-500 text-white font-medium py-2.5 rounded-xl transition-all text-sm flex items-center justify-center gap-2">
                {isSubmitting
                  ? <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  : <><CheckCircle2 className="w-4 h-4" /> Submit</>}
              </button>
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}

// ─── Results View ─────────────────────────────────────────────────────────────

function ResultsView({ examId, examInfo }: { examId: string; examInfo: ExamInfo | undefined }) {
  const navigate = useNavigate();
  const { data: submission } = useQuery({
    queryKey: ['submission-result', examId],
    queryFn: () => api.get(`/api/v1/exams/${examId}/submissions`).then(r => {
      const list = Array.isArray(r.data) ? r.data : [];
      return list.sort((a: { startedAt: string }, b: { startedAt: string }) =>
        new Date(b.startedAt).getTime() - new Date(a.startedAt).getTime())[0];
    }),
    retry: 2,
  });

  const pct = submission?.percentage ?? 0;
  const passed = submission && examInfo?.passingMarks != null
    ? submission.scoredMarks >= (examInfo.passingMarks ?? 0)
    : pct >= 60;

  return (
    <div className="min-h-screen bg-surface flex items-center justify-center p-6">
      <motion.div className="glass rounded-3xl p-8 max-w-md w-full text-center space-y-6"
        initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }}>
        <div className={cn(
          'w-20 h-20 rounded-full flex items-center justify-center mx-auto border-4',
          passed ? 'bg-emerald-500/20 border-emerald-500/40' : 'bg-red-500/20 border-red-500/40'
        )}>
          {passed
            ? <CheckCircle2 className="w-10 h-10 text-emerald-400" />
            : <AlertTriangle className="w-10 h-10 text-red-400" />}
        </div>

        <div>
          <h1 className="text-2xl font-bold text-white">{passed ? 'Congratulations!' : 'Keep Practising'}</h1>
          <p className="text-white/50 text-sm mt-1">{examInfo?.title ?? examInfo?.name ?? 'Exam'} completed</p>
        </div>

        {submission ? (
          <div className="grid grid-cols-2 gap-4">
            <div className="glass rounded-xl p-4">
              <p className="text-white/40 text-xs">Score</p>
              <p className="text-2xl font-bold text-white mt-1">
                {submission.scoredMarks?.toFixed(1) ?? '—'}<span className="text-sm text-white/40">/{submission.totalMarks?.toFixed(1) ?? '—'}</span>
              </p>
            </div>
            <div className="glass rounded-xl p-4">
              <p className="text-white/40 text-xs">Percentage</p>
              <p className={cn('text-2xl font-bold mt-1', passed ? 'text-emerald-400' : 'text-red-400')}>
                {pct.toFixed(1)}%
              </p>
            </div>
          </div>
        ) : (
          <div className="text-white/40 text-sm">Loading results…</div>
        )}

        <button onClick={() => navigate('/assessments')} className="btn-primary w-full">
          Back to Assessments
        </button>
      </motion.div>
    </div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function ExamPage() {
  const { examId } = useParams<{ examId: string }>();
  const navigate = useNavigate();
  const isResultsPage = window.location.pathname.includes('/results');

  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState<Record<string, number>>({});   // questionId → 0-indexed option
  const [flagged, setFlagged] = useState<Record<string, boolean>>({});
  const [showSubmitModal, setShowSubmitModal] = useState(false);
  const [submissionId, setSubmissionId] = useState<string | null>(null);

  // ── Load exam info ─────────────────────────────────────────────────────────
  const { data: examInfo, isLoading: examLoading } = useQuery<ExamInfo>({
    queryKey: ['exam-info', examId],
    queryFn: () => api.get(`/api/v1/exams/${examId}`).then(r => r.data),
    enabled: !!examId,
    retry: 1,
  });

  // ── Load all questions ─────────────────────────────────────────────────────
  const { data: questions = [], isLoading: questionsLoading } = useQuery<Question[]>({
    queryKey: ['exam-questions', examId],
    queryFn: () => api.get(`/api/v1/exams/${examId}/questions`).then(r => r.data),
    enabled: !!examId && !isResultsPage,
    retry: 1,
  });

  // ── Start submission on mount ──────────────────────────────────────────────
  const startMutation = useMutation({
    mutationFn: () => api.post(`/api/v1/exams/${examId}/submissions`),
    onSuccess: (res) => setSubmissionId(res.data?.id ?? null),
    onError: () => { /* Submission may already exist — proceed anyway */ },
  });

  useEffect(() => {
    if (examId && !isResultsPage && !submissionId) {
      startMutation.mutate();
    }
  }, [examId, isResultsPage]);

  // ── Submit exam ────────────────────────────────────────────────────────────
  const submitMutation = useMutation({
    mutationFn: async () => {
      const sid = submissionId;
      if (!sid) throw new Error('No submission started');
      // Build answers array: all questions, answered ones with selectedOption
      const answerEntries = questions.map(q => ({
        questionId: q.id,
        selectedOption: answers[q.id] ?? 0,
      }));
      return api.post(`/api/v1/exams/${examId}/submissions/${sid}/answers`, { answers: answerEntries });
    },
    onSuccess: () => {
      toast.success('Exam submitted successfully!');
      navigate(`/assessments/${examId}/results`, { replace: true });
    },
    onError: () => toast.error('Submission failed. Please try again.'),
  });

  const handleTimerExpire = useCallback(() => {
    toast.error('Time is up! Submitting exam…');
    submitMutation.mutate();
  }, [submitMutation]);

  // ── Results page ───────────────────────────────────────────────────────────
  if (isResultsPage) {
    return <ResultsView examId={examId!} examInfo={examInfo} />;
  }

  // ── Loading ────────────────────────────────────────────────────────────────
  if (examLoading || questionsLoading) {
    return (
      <div className="fixed inset-0 bg-surface flex items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <div className="w-10 h-10 border-2 border-brand-500/30 border-t-brand-500 rounded-full animate-spin" />
          <p className="text-white/50 text-sm">Loading exam…</p>
        </div>
      </div>
    );
  }

  const totalQuestions = questions.length;
  const currentQuestion = questions[currentIndex];
  const answeredCount = Object.keys(answers).length;
  const unansweredCount = totalQuestions - answeredCount;
  const durationSeconds = (examInfo?.durationMinutes ?? 30) * 60;

  function selectOption(optionIndex: number) {
    if (!currentQuestion) return;
    setAnswers(prev => ({ ...prev, [currentQuestion.id]: optionIndex }));
  }

  function goToQuestion(index: number) {
    if (index < 0 || index >= totalQuestions) return;
    setCurrentIndex(index);
  }

  function getCellStyle(index: number): string {
    const q = questions[index];
    if (!q) return 'bg-white/5 text-white/30 border border-white/10';
    if (flagged[q.id]) return 'bg-amber-500/20 text-amber-400 border border-amber-500/30';
    if (answers[q.id] !== undefined) return 'bg-brand-500/25 text-brand-300 border border-brand-500/30';
    return 'bg-white/5 text-white/40 border border-white/10';
  }

  const OPTION_LABELS = ['A', 'B', 'C', 'D', 'E'];

  return (
    <div className="fixed inset-0 bg-surface flex flex-col overflow-hidden">
      {/* ── Top bar ── */}
      <div className="flex items-center justify-between px-6 py-3 border-b border-white/5 bg-surface-50/40 backdrop-blur-md flex-shrink-0">
        <div className="flex items-center gap-3">
          <div className="p-1.5 rounded-lg bg-brand-600/20 border border-brand-500/30">
            <BookOpen className="w-4 h-4 text-brand-400" />
          </div>
          <span className="font-bold text-white text-sm hidden sm:block truncate max-w-[200px]">
            {examInfo?.title ?? examInfo?.name ?? 'Exam'}
          </span>
        </div>

        <div className="flex items-center gap-2 text-white/60 text-sm font-medium">
          Question <span className="text-white font-bold">{currentIndex + 1}</span>
          <span>/</span>
          <span>{totalQuestions}</span>
        </div>

        <ExamTimer durationSeconds={durationSeconds} onExpire={handleTimerExpire} />
      </div>

      {/* ── Main content ── */}
      <div className="flex flex-1 overflow-hidden">
        {/* Question area */}
        <div className="flex-1 flex flex-col overflow-y-auto p-6 lg:p-10">
          <AnimatePresence mode="wait">
            {currentQuestion ? (
              <motion.div key={currentQuestion.id}
                initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -20 }} transition={{ duration: 0.25 }}
                className="flex-1">
                {/* Question text */}
                <div className="glass rounded-2xl p-6 mb-6">
                  <div className="flex items-start gap-3 mb-4">
                    <span className="text-brand-400 font-bold text-sm flex-shrink-0 mt-0.5">Q{currentIndex + 1}.</span>
                    <p className="text-white text-base leading-relaxed font-medium">{currentQuestion.questionText}</p>
                  </div>
                </div>

                {/* Options */}
                <div className="space-y-3">
                  {currentQuestion.options.map((optText, idx) => {
                    const selected = answers[currentQuestion.id] === idx;
                    return (
                      <motion.button key={idx} onClick={() => selectOption(idx)}
                        whileHover={{ scale: 1.005 }} whileTap={{ scale: 0.995 }}
                        className={cn(
                          'w-full flex items-center gap-4 p-4 rounded-xl border text-left transition-all duration-200',
                          selected
                            ? 'bg-brand-500/20 border-brand-500/50 shadow-lg shadow-brand-500/10'
                            : 'bg-surface-50/40 border-white/8 hover:bg-surface-50/80 hover:border-white/15'
                        )}>
                        <div className={cn(
                          'w-8 h-8 rounded-full border-2 flex items-center justify-center text-sm font-bold flex-shrink-0 transition-all',
                          selected ? 'bg-brand-500 border-brand-500 text-white' : 'border-white/20 text-white/50 bg-white/5'
                        )}>
                          {OPTION_LABELS[idx] ?? String(idx + 1)}
                        </div>
                        <span className={cn('text-sm leading-relaxed', selected ? 'text-white font-medium' : 'text-white/70')}>
                          {optText}
                        </span>
                      </motion.button>
                    );
                  })}
                </div>
              </motion.div>
            ) : (
              <motion.div key="empty" initial={{ opacity: 0 }} animate={{ opacity: 1 }}
                className="flex-1 flex items-center justify-center text-white/30 text-sm">
                No questions found.
              </motion.div>
            )}
          </AnimatePresence>

          {/* Navigation */}
          <div className="flex items-center justify-between mt-8 pt-4 border-t border-white/5">
            <button onClick={() => goToQuestion(currentIndex - 1)} disabled={currentIndex === 0}
              className={cn('flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium transition-all',
                currentIndex === 0 ? 'text-white/20 cursor-not-allowed' : 'btn-ghost border border-white/10')}>
              <ChevronLeft className="w-4 h-4" /> Previous
            </button>

            <span className="text-white/30 text-xs">{answeredCount}/{totalQuestions} answered</span>

            {currentIndex < totalQuestions - 1 ? (
              <button onClick={() => goToQuestion(currentIndex + 1)}
                className="flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium btn-primary">
                Next <ChevronRight className="w-4 h-4" />
              </button>
            ) : (
              <button onClick={() => setShowSubmitModal(true)}
                className="flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium bg-emerald-600 hover:bg-emerald-500 text-white transition-all">
                <CheckCircle2 className="w-4 h-4" /> Finish
              </button>
            )}
          </div>
        </div>

        {/* ── Sidebar: Navigator ── */}
        <div className="w-72 border-l border-white/5 bg-surface-50/30 flex flex-col p-5 gap-5 flex-shrink-0 overflow-y-auto hidden lg:flex">
          <div>
            <p className="text-white/30 text-xs font-medium uppercase tracking-wider mb-3">Navigator</p>
            <div className="grid grid-cols-5 gap-1.5">
              {questions.map((_, i) => (
                <button key={i} onClick={() => goToQuestion(i)}
                  className={cn(
                    'w-full aspect-square rounded-lg text-xs font-medium transition-all duration-150 hover:scale-105',
                    i === currentIndex ? 'ring-2 ring-brand-400 ring-offset-1 ring-offset-surface-50' : '',
                    getCellStyle(i)
                  )}>
                  {i + 1}
                </button>
              ))}
            </div>
            <div className="mt-3 flex flex-wrap gap-3 text-xs text-white/30">
              <span className="flex items-center gap-1.5">
                <span className="w-2.5 h-2.5 rounded-sm bg-brand-500/25 border border-brand-500/30 inline-block" /> Answered
              </span>
              <span className="flex items-center gap-1.5">
                <span className="w-2.5 h-2.5 rounded-sm bg-amber-500/20 border border-amber-500/30 inline-block" /> Flagged
              </span>
              <span className="flex items-center gap-1.5">
                <span className="w-2.5 h-2.5 rounded-sm bg-white/5 border border-white/10 inline-block" /> Unanswered
              </span>
            </div>
          </div>

          {currentQuestion && (
            <div className="glass rounded-xl p-4 space-y-3">
              <div>
                <p className="text-white/30 text-xs mb-1.5">Difficulty</p>
                <div className="flex items-center gap-0.5">
                  {Array.from({ length: 5 }).map((_, i) => (
                    <div key={i} className={cn('w-4 h-2 rounded-sm',
                      i < Math.round((currentQuestion.difficulty ?? 0.5) * 5) ? 'bg-brand-500' : 'bg-white/10'
                    )} />
                  ))}
                </div>
              </div>
              {currentQuestion.marks != null && (
                <div>
                  <p className="text-white/30 text-xs mb-1">Marks</p>
                  <p className="text-white text-sm font-medium">{currentQuestion.marks}</p>
                </div>
              )}
            </div>
          )}

          {currentQuestion && (
            <button onClick={() => setFlagged(prev => ({ ...prev, [currentQuestion.id]: !prev[currentQuestion.id] }))}
              className={cn(
                'flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium border transition-all',
                flagged[currentQuestion.id]
                  ? 'bg-amber-500/15 text-amber-400 border-amber-500/30'
                  : 'bg-white/5 text-white/50 border-white/10 hover:border-amber-500/20 hover:text-amber-400'
              )}>
              <Flag className="w-4 h-4" />
              {flagged[currentQuestion.id] ? 'Unflag question' : 'Flag for review'}
            </button>
          )}

          <div className="mt-auto">
            <button onClick={() => setShowSubmitModal(true)}
              className="w-full bg-emerald-600 hover:bg-emerald-500 text-white font-medium py-2.5 rounded-xl text-sm transition-all flex items-center justify-center gap-2">
              <CheckCircle2 className="w-4 h-4" /> Submit Exam
            </button>
          </div>
        </div>
      </div>

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
