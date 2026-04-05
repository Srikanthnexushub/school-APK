import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import {
  ClipboardList, ChevronRight, Plus, Trash2, CheckCircle2,
  BookOpen, Clock, BarChart3, FlaskConical, AlertCircle,
  Eye, ChevronDown, ChevronUp, Sparkles, Calendar, Loader2, X,
} from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { cn } from '../../lib/utils';

// ─── Types ────────────────────────────────────────────────────────────────────

interface CenterResponse {
  id: string;
  name: string;
  ownerId: string;
}

interface BatchResponse {
  id: string;
  name: string;
  centerId: string;
}

interface ExamResponse {
  id: string;
  title: string;
  subject: string;
  mode: string;
  durationMinutes: number;
  totalQuestions: number;
  status: 'DRAFT' | 'PUBLISHED' | 'AVAILABLE' | 'CLOSED' | 'CANCELLED';
  createdAt?: string;
  startAt?: string;
  endAt?: string;
}

interface AIGeneratedQuestion {
  questionText: string;
  options: [string, string, string, string];
  correctAnswer: number;
  explanation: string;
  difficulty: Difficulty;
}

type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';

interface QuestionDraft {
  questionText: string;
  options: [string, string, string, string];
  correctAnswer: number; // 0-based index
  explanation: string;
  marks: number;
  difficulty: Difficulty;       // preset label (EASY/MEDIUM/HARD)
  irtDifficulty: number;        // IRT b-param
  discrimination: number;       // IRT a-param
  guessingParam: number;        // IRT c-param
  showAdvanced: boolean;
}

interface CreateExamRequest {
  title: string;
  description: string;
  batchId: string;
  centerId: string;
  mode: 'ONLINE' | 'OFFLINE';
  durationMinutes: number;
  maxAttempts: number;
  startAt?: string;
  endAt?: string;
  totalMarks: number;
  passingMarks: number;
}

// ─── IRT Presets ──────────────────────────────────────────────────────────────

const IRT_PRESETS: Record<Difficulty, { irtDifficulty: number; discrimination: number; guessingParam: number }> = {
  EASY:   { irtDifficulty: -1.0, discrimination: 1.0,  guessingParam: 0.25 },
  MEDIUM: { irtDifficulty:  0.0, discrimination: 1.2,  guessingParam: 0.20 },
  HARD:   { irtDifficulty:  1.5, discrimination: 1.5,  guessingParam: 0.10 },
};

const DIFFICULTY_STYLES: Record<Difficulty, string> = {
  EASY:   'bg-emerald-500/15 text-emerald-400 border-emerald-500/30',
  MEDIUM: 'bg-amber-500/15 text-amber-400 border-amber-500/30',
  HARD:   'bg-red-500/15 text-red-400 border-red-500/30',
};

const STATUS_STYLES: Record<string, string> = {
  DRAFT:     'bg-white/10 text-white/50',
  PUBLISHED: 'bg-emerald-500/15 text-emerald-400',
  AVAILABLE: 'bg-brand-500/15 text-brand-400',
  CLOSED:    'bg-red-500/15 text-red-400',
  CANCELLED: 'bg-red-500/10 text-red-400/60',
};

// ─── Step Indicator ───────────────────────────────────────────────────────────

function StepIndicator({ step }: { step: number }) {
  const steps = [
    { label: 'Exam Details' },
    { label: 'Add Questions' },
    { label: 'Review & Publish' },
  ];
  return (
    <div className="flex items-center gap-0 mb-8">
      {steps.map((s, i) => (
        <div key={i} className="flex items-center flex-1 last:flex-none">
          <div className="flex items-center gap-2 flex-shrink-0">
            <div className={cn(
              'w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold border-2 transition-all',
              i < step  ? 'bg-brand-500 border-brand-500 text-white' :
              i === step ? 'bg-brand-500/20 border-brand-500 text-brand-400' :
                           'bg-white/5 border-white/10 text-white/30'
            )}>
              {i < step ? <CheckCircle2 className="w-4 h-4" /> : i + 1}
            </div>
            <span className={cn(
              'text-sm font-medium hidden sm:block',
              i === step ? 'text-white' : i < step ? 'text-brand-400' : 'text-white/30'
            )}>{s.label}</span>
          </div>
          {i < steps.length - 1 && (
            <div className={cn('flex-1 h-px mx-3', i < step ? 'bg-brand-500/50' : 'bg-white/10')} />
          )}
        </div>
      ))}
    </div>
  );
}

// ─── Step 1: Exam Details ─────────────────────────────────────────────────────

function blankDraft(): CreateExamRequest {
  return {
    title: '', description: '', batchId: '', centerId: '',
    mode: 'ONLINE', durationMinutes: 60, maxAttempts: 1,
    startAt: '', endAt: '', totalMarks: 100, passingMarks: 40,
  };
}

function Step1({
  form, setForm, centers, batches, centersLoading, batchesLoading,
  onNext,
}: {
  form: CreateExamRequest;
  setForm: (f: CreateExamRequest) => void;
  centers: CenterResponse[];
  batches: BatchResponse[];
  centersLoading: boolean;
  batchesLoading: boolean;
  onNext: () => void;
}) {
  const [genDescLoading, setGenDescLoading] = useState(false);

  function set<K extends keyof CreateExamRequest>(key: K, val: CreateExamRequest[K]) {
    setForm({ ...form, [key]: val });
  }

  async function generateDescription() {
    if (!form.title.trim()) {
      toast.error('Enter an exam title first');
      return;
    }
    setGenDescLoading(true);
    try {
      const res = await api.post('/api/v1/ai/completions', {
        systemPrompt: 'You are an expert educator. Write a clear, concise 2–3 sentence exam description suitable for students. Be specific about the topics covered and what skills will be tested.',
        userMessage: `Write a description for an exam titled "${form.title}" (mode: ${form.mode}, duration: ${form.durationMinutes} minutes). Describe topics covered and student preparation needed.`,
        maxTokens: 150,
        temperature: 0.7,
      });
      const text: string = res.data?.content ?? res.data?.choices?.[0]?.message?.content ?? '';
      if (text) set('description', text.trim());
      else toast.error('No description generated');
    } catch {
      toast.error('AI description generation failed');
    } finally {
      setGenDescLoading(false);
    }
  }

  const valid = form.title.trim() && form.batchId && form.centerId && form.durationMinutes > 0;

  return (
    <div className="space-y-5">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        {/* Title */}
        <div className="md:col-span-2">
          <label className="block text-sm text-white/60 mb-1.5">Exam Title *</label>
          <input
            className="input w-full"
            placeholder="e.g. Mid-Term Physics — Chapter 5–8"
            value={form.title}
            onChange={e => set('title', e.target.value)}
          />
        </div>

        {/* Description */}
        <div className="md:col-span-2">
          <div className="flex items-center justify-between mb-1.5">
            <label className="text-sm text-white/60">Description</label>
            <button
              type="button"
              onClick={generateDescription}
              disabled={genDescLoading || !form.title.trim()}
              className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-purple-500/15 text-purple-400 border border-purple-500/25 hover:bg-purple-500/25 disabled:opacity-40 disabled:cursor-not-allowed transition-all text-xs font-medium"
            >
              {genDescLoading
                ? <><Loader2 className="w-3 h-3 animate-spin" /> Generating…</>
                : <><Sparkles className="w-3 h-3" /> AI Generate</>}
            </button>
          </div>
          <textarea
            className="input w-full resize-none"
            rows={3}
            placeholder="Brief description of what this exam covers… or click AI Generate"
            value={form.description}
            onChange={e => set('description', e.target.value)}
          />
        </div>

        {/* Center */}
        <div>
          <label className="block text-sm text-white/60 mb-1.5">Center *</label>
          <select
            className="input w-full"
            value={form.centerId}
            onChange={e => set('centerId', e.target.value)}
            disabled={centersLoading}
          >
            <option value="">{centersLoading ? 'Loading…' : 'Select center'}</option>
            {centers.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </div>

        {/* Batch */}
        <div>
          <label className="block text-sm text-white/60 mb-1.5">Batch *</label>
          <select
            className="input w-full"
            value={form.batchId}
            onChange={e => set('batchId', e.target.value)}
            disabled={!form.centerId || batchesLoading}
          >
            <option value="">{!form.centerId ? 'Select center first' : batchesLoading ? 'Loading…' : 'Select batch'}</option>
            {batches.map(b => <option key={b.id} value={b.id}>{b.name}</option>)}
          </select>
        </div>

        {/* Mode */}
        <div>
          <label className="block text-sm text-white/60 mb-1.5">Exam Mode</label>
          <div className="flex gap-2">
            {(['ONLINE', 'OFFLINE'] as const).map(m => (
              <button
                key={m}
                type="button"
                onClick={() => set('mode', m)}
                className={cn(
                  'flex-1 py-2.5 rounded-xl border text-sm font-medium transition-all',
                  form.mode === m
                    ? 'bg-brand-500/20 border-brand-500/50 text-brand-400'
                    : 'glass border-white/10 text-white/50 hover:border-white/20'
                )}
              >
                {m}
              </button>
            ))}
          </div>
        </div>

        {/* Duration */}
        <div>
          <label className="block text-sm text-white/60 mb-1.5">Duration (minutes) *</label>
          <input
            type="number"
            className="input w-full"
            min={5}
            max={360}
            value={form.durationMinutes}
            onChange={e => set('durationMinutes', Number(e.target.value))}
          />
        </div>

        {/* Max attempts */}
        <div>
          <label className="block text-sm text-white/60 mb-1.5">Max Attempts</label>
          <input
            type="number"
            className="input w-full"
            min={1}
            max={5}
            value={form.maxAttempts}
            onChange={e => set('maxAttempts', Number(e.target.value))}
          />
        </div>

        {/* Total marks */}
        <div>
          <label className="block text-sm text-white/60 mb-1.5">Total Marks</label>
          <input
            type="number"
            className="input w-full"
            min={1}
            value={form.totalMarks}
            onChange={e => set('totalMarks', Number(e.target.value))}
          />
        </div>

        {/* Passing marks */}
        <div>
          <label className="block text-sm text-white/60 mb-1.5">Passing Marks</label>
          <input
            type="number"
            className="input w-full"
            min={1}
            value={form.passingMarks}
            onChange={e => set('passingMarks', Number(e.target.value))}
          />
        </div>

        {/* Start at */}
        <div>
          <label className="block text-sm text-white/60 mb-1.5">Start Date/Time (optional)</label>
          <input
            type="datetime-local"
            className="input w-full"
            value={form.startAt ?? ''}
            onChange={e => set('startAt', e.target.value)}
          />
        </div>

        {/* End at */}
        <div>
          <label className="block text-sm text-white/60 mb-1.5">End Date/Time (optional)</label>
          <input
            type="datetime-local"
            className="input w-full"
            value={form.endAt ?? ''}
            onChange={e => set('endAt', e.target.value)}
          />
        </div>
      </div>

      <div className="flex justify-end pt-2">
        <button
          onClick={onNext}
          disabled={!valid}
          className="btn-primary flex items-center gap-2 disabled:opacity-40 disabled:cursor-not-allowed"
        >
          Next: Add Questions <ChevronRight className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
}

// ─── Question Form Card ───────────────────────────────────────────────────────

function blankQuestion(): QuestionDraft {
  return {
    questionText: '', options: ['', '', '', ''], correctAnswer: 0,
    explanation: '', marks: 1, difficulty: 'MEDIUM',
    ...IRT_PRESETS.MEDIUM, showAdvanced: false,
  } as QuestionDraft;
}

function QuestionCard({
  q, idx, onUpdate, onRemove,
}: {
  q: QuestionDraft;
  idx: number;
  onUpdate: (q: QuestionDraft) => void;
  onRemove: () => void;
}) {
  function setOpt(i: number, val: string) {
    const opts = [...q.options] as [string, string, string, string];
    opts[i] = val;
    onUpdate({ ...q, options: opts });
  }

  function setDifficulty(d: Difficulty) {
    onUpdate({ ...q, difficulty: d, ...IRT_PRESETS[d] });
  }

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -8 }}
      className="glass rounded-2xl p-5 space-y-4"
    >
      {/* Header */}
      <div className="flex items-center justify-between">
        <span className="text-sm font-semibold text-white/70">Question {idx + 1}</span>
        <button
          onClick={onRemove}
          className="p-1.5 rounded-lg text-red-400/50 hover:text-red-400 hover:bg-red-500/10 transition-colors"
        >
          <Trash2 className="w-4 h-4" />
        </button>
      </div>

      {/* Question text */}
      <div>
        <label className="block text-xs text-white/40 mb-1">Question *</label>
        <textarea
          className="input w-full resize-none text-sm"
          rows={2}
          placeholder="Type your question here…"
          value={q.questionText}
          onChange={e => onUpdate({ ...q, questionText: e.target.value })}
        />
      </div>

      {/* Options */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
        {q.options.map((opt, i) => (
          <div key={i} className="flex items-center gap-2">
            <input
              type="radio"
              name={`correct-${idx}`}
              checked={q.correctAnswer === i}
              onChange={() => onUpdate({ ...q, correctAnswer: i })}
              className="accent-brand-500 flex-shrink-0 w-4 h-4"
              title="Mark as correct answer"
            />
            <input
              className="input flex-1 text-sm py-2"
              placeholder={`Option ${String.fromCharCode(65 + i)}`}
              value={opt}
              onChange={e => setOpt(i, e.target.value)}
            />
          </div>
        ))}
      </div>
      <p className="text-xs text-white/30">Select the radio button next to the correct answer</p>

      {/* Bottom row: difficulty + marks */}
      <div className="flex items-center gap-4 flex-wrap">
        {/* Difficulty presets */}
        <div className="flex gap-1.5">
          {(['EASY', 'MEDIUM', 'HARD'] as Difficulty[]).map(d => (
            <button
              key={d}
              type="button"
              onClick={() => setDifficulty(d)}
              className={cn(
                'px-3 py-1 rounded-lg text-xs font-medium border transition-all',
                q.difficulty === d ? DIFFICULTY_STYLES[d] : 'glass border-white/10 text-white/40 hover:border-white/20'
              )}
            >
              {d}
            </button>
          ))}
        </div>

        {/* Marks */}
        <div className="flex items-center gap-2 ml-auto">
          <label className="text-xs text-white/40">Marks</label>
          <input
            type="number"
            min={1}
            max={20}
            value={q.marks}
            onChange={e => onUpdate({ ...q, marks: Number(e.target.value) })}
            className="input w-16 text-sm py-1.5 text-center"
          />
        </div>
      </div>

      {/* Explanation */}
      <div>
        <label className="block text-xs text-white/40 mb-1">Explanation (optional)</label>
        <input
          className="input w-full text-sm"
          placeholder="Why is this the correct answer?"
          value={q.explanation}
          onChange={e => onUpdate({ ...q, explanation: e.target.value })}
        />
      </div>

      {/* Advanced IRT (collapsible) */}
      <div>
        <button
          type="button"
          onClick={() => onUpdate({ ...q, showAdvanced: !q.showAdvanced })}
          className="text-xs text-white/30 hover:text-white/60 transition-colors flex items-center gap-1"
        >
          <BarChart3 className="w-3 h-3" />
          {q.showAdvanced ? 'Hide' : 'Show'} IRT Parameters
        </button>
        <AnimatePresence>
          {q.showAdvanced && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              className="overflow-hidden"
            >
              <div className="grid grid-cols-3 gap-3 mt-3 p-3 bg-white/3 rounded-xl border border-white/5">
                <div>
                  <label className="block text-xs text-white/40 mb-1">Difficulty (b)</label>
                  <input
                    type="number"
                    step={0.1}
                    className="input w-full text-sm py-1.5"
                    value={q.irtDifficulty}
                    onChange={e => onUpdate({ ...q, irtDifficulty: Number(e.target.value) })}
                  />
                </div>
                <div>
                  <label className="block text-xs text-white/40 mb-1">Discrimination (a)</label>
                  <input
                    type="number"
                    step={0.1}
                    min={0}
                    className="input w-full text-sm py-1.5"
                    value={q.discrimination}
                    onChange={e => onUpdate({ ...q, discrimination: Number(e.target.value) })}
                  />
                </div>
                <div>
                  <label className="block text-xs text-white/40 mb-1">Guessing (c)</label>
                  <input
                    type="number"
                    step={0.05}
                    min={0}
                    max={0.5}
                    className="input w-full text-sm py-1.5"
                    value={q.guessingParam}
                    onChange={e => onUpdate({ ...q, guessingParam: Number(e.target.value) })}
                  />
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </motion.div>
  );
}

// ─── Step 2: Add Questions ────────────────────────────────────────────────────

function Step2({
  examTitle, questions, setQuestions, onBack, onNext, isSaving,
}: {
  examTitle: string;
  questions: QuestionDraft[];
  setQuestions: (q: QuestionDraft[]) => void;
  onBack: () => void;
  onNext: () => void;
  isSaving: boolean;
}) {
  const [aiTopic, setAiTopic] = useState('');
  const [aiCount, setAiCount] = useState(5);
  const [aiDifficulty, setAiDifficulty] = useState<Difficulty | 'MIX'>('MIX');
  const [isGenerating, setIsGenerating] = useState(false);
  const [generatedPreview, setGeneratedPreview] = useState<AIGeneratedQuestion[]>([]);
  const [showAiPanel, setShowAiPanel] = useState(false);

  async function generateQuestions() {
    const topic = aiTopic.trim() || examTitle;
    setIsGenerating(true);
    try {
      const res = await api.post('/api/v1/ai/generate-questions', {
        topic,
        difficulty: aiDifficulty === 'MIX' ? 'MEDIUM' : aiDifficulty,
        count: aiCount,
      });
      const raw: Array<{
        questionText: string;
        options: string[];
        correctAnswer: string | number;
        explanation?: string;
        difficulty?: string;
      }> = Array.isArray(res.data) ? res.data : (res.data?.questions ?? []);

      const parsed: AIGeneratedQuestion[] = raw.map(q => {
        // correctAnswer may be index (0-3) or option text — normalise to index
        let correctIdx = 0;
        if (typeof q.correctAnswer === 'number') {
          correctIdx = q.correctAnswer;
        } else {
          const idx = q.options.findIndex(o => o === q.correctAnswer);
          correctIdx = idx >= 0 ? idx : 0;
        }
        const opts = (q.options ?? ['', '', '', '']).slice(0, 4) as [string, string, string, string];
        while (opts.length < 4) opts.push('');
        const diff = (['EASY', 'MEDIUM', 'HARD'].includes(q.difficulty ?? '') ? q.difficulty : 'MEDIUM') as Difficulty;
        return { questionText: q.questionText, options: opts, correctAnswer: correctIdx, explanation: q.explanation ?? '', difficulty: diff };
      });

      setGeneratedPreview(parsed);
      toast.success(`${parsed.length} questions generated — review and add`);
    } catch {
      toast.error('AI question generation failed');
    } finally {
      setIsGenerating(false);
    }
  }

  function addGeneratedQuestion(q: AIGeneratedQuestion) {
    const preset = IRT_PRESETS[q.difficulty];
    const draft: QuestionDraft = {
      questionText: q.questionText,
      options: q.options,
      correctAnswer: q.correctAnswer,
      explanation: q.explanation,
      marks: q.difficulty === 'HARD' ? 3 : q.difficulty === 'EASY' ? 1 : 2,
      difficulty: q.difficulty,
      ...preset,
      showAdvanced: false,
    };
    setQuestions([...questions, draft]);
    setGeneratedPreview(prev => prev.filter(p => p !== q));
    toast.success('Question added');
  }

  function addAllGenerated() {
    const newDrafts: QuestionDraft[] = generatedPreview.map(q => {
      const preset = IRT_PRESETS[q.difficulty];
      return {
        questionText: q.questionText,
        options: q.options,
        correctAnswer: q.correctAnswer,
        explanation: q.explanation,
        marks: q.difficulty === 'HARD' ? 3 : q.difficulty === 'EASY' ? 1 : 2,
        difficulty: q.difficulty,
        ...preset,
        showAdvanced: false,
      };
    });
    setQuestions([...questions, ...newDrafts]);
    setGeneratedPreview([]);
    toast.success(`${newDrafts.length} questions added`);
  }

  function addQuestion() {
    setQuestions([...questions, blankQuestion()]);
  }

  function updateQuestion(i: number, q: QuestionDraft) {
    const next = [...questions];
    next[i] = q;
    setQuestions(next);
  }

  function removeQuestion(i: number) {
    setQuestions(questions.filter((_, idx) => idx !== i));
  }

  const valid = questions.length > 0 && questions.every(q =>
    q.questionText.trim() && q.options.every(o => o.trim())
  );

  return (
    <div className="space-y-4">
      {/* AI Generator Panel */}
      <div className="glass rounded-2xl border border-purple-500/20 overflow-hidden">
        <button
          type="button"
          onClick={() => setShowAiPanel(!showAiPanel)}
          className="w-full flex items-center justify-between px-5 py-3.5 hover:bg-white/2 transition-colors"
        >
          <div className="flex items-center gap-2 text-purple-400">
            <Sparkles className="w-4 h-4" />
            <span className="text-sm font-semibold">AI Question Generator</span>
            <span className="text-xs text-purple-400/60">— auto-generate questions from topic</span>
          </div>
          {showAiPanel ? <ChevronUp className="w-4 h-4 text-white/30" /> : <ChevronDown className="w-4 h-4 text-white/30" />}
        </button>

        <AnimatePresence>
          {showAiPanel && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              className="overflow-hidden"
            >
              <div className="px-5 pb-5 space-y-4 border-t border-purple-500/10">
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-4">
                  <div className="sm:col-span-3">
                    <label className="block text-xs text-white/40 mb-1">Topic / Subject</label>
                    <input
                      className="input w-full text-sm"
                      placeholder={`e.g. ${examTitle || 'Newton\'s Laws of Motion'}`}
                      value={aiTopic}
                      onChange={e => setAiTopic(e.target.value)}
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-white/40 mb-1">No. of Questions</label>
                    <input
                      type="number"
                      min={1}
                      max={20}
                      className="input w-full text-sm"
                      value={aiCount}
                      onChange={e => setAiCount(Math.max(1, Math.min(20, Number(e.target.value))))}
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-white/40 mb-1">Difficulty</label>
                    <select
                      className="input w-full text-sm"
                      value={aiDifficulty}
                      onChange={e => setAiDifficulty(e.target.value as Difficulty | 'MIX')}
                    >
                      <option value="MIX">Mixed (E/M/H)</option>
                      <option value="EASY">Easy</option>
                      <option value="MEDIUM">Medium</option>
                      <option value="HARD">Hard</option>
                    </select>
                  </div>
                  <div className="flex items-end">
                    <button
                      type="button"
                      onClick={generateQuestions}
                      disabled={isGenerating}
                      className="w-full btn-primary flex items-center justify-center gap-2 disabled:opacity-50"
                    >
                      {isGenerating
                        ? <><Loader2 className="w-4 h-4 animate-spin" /> Generating…</>
                        : <><Sparkles className="w-4 h-4" /> Generate</>}
                    </button>
                  </div>
                </div>

                {/* Preview of generated questions */}
                <AnimatePresence>
                  {generatedPreview.length > 0 && (
                    <motion.div
                      initial={{ opacity: 0, y: 6 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0 }}
                      className="space-y-2"
                    >
                      <div className="flex items-center justify-between">
                        <p className="text-xs text-white/50">{generatedPreview.length} questions ready — review and add</p>
                        <button
                          type="button"
                          onClick={addAllGenerated}
                          className="text-xs px-3 py-1 rounded-lg bg-purple-500/15 text-purple-400 border border-purple-500/25 hover:bg-purple-500/25 transition-all font-medium"
                        >
                          Add All
                        </button>
                      </div>
                      {generatedPreview.map((q, i) => (
                        <div key={i} className="glass rounded-xl p-3 border border-white/5 flex items-start gap-3">
                          <div className="flex-1 min-w-0">
                            <p className="text-white/80 text-sm font-medium line-clamp-2">{q.questionText}</p>
                            <div className="flex items-center gap-2 mt-1">
                              <span className={cn('badge text-xs border', DIFFICULTY_STYLES[q.difficulty])}>{q.difficulty}</span>
                              <span className="text-white/30 text-xs">{q.options.filter(Boolean).length} options</span>
                            </div>
                          </div>
                          <button
                            type="button"
                            onClick={() => addGeneratedQuestion(q)}
                            className="flex-shrink-0 p-1.5 rounded-lg bg-brand-500/15 text-brand-400 border border-brand-500/20 hover:bg-brand-500/25 transition-colors"
                          >
                            <Plus className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      ))}
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      <div className="flex items-center justify-between">
        <p className="text-white/50 text-sm">{questions.length} question{questions.length !== 1 ? 's' : ''} added</p>
        <button
          type="button"
          onClick={addQuestion}
          className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-brand-500/15 text-brand-400 border border-brand-500/25 hover:bg-brand-500/25 transition-all text-sm font-medium"
        >
          <Plus className="w-4 h-4" /> Add Manually
        </button>
      </div>

      {questions.length === 0 ? (
        <div className="glass rounded-2xl p-12 flex flex-col items-center gap-4 text-white/30">
          <FlaskConical className="w-12 h-12 opacity-40" />
          <p>No questions yet. Click "Add Question" to start.</p>
        </div>
      ) : (
        <AnimatePresence mode="popLayout">
          {questions.map((q, i) => (
            <QuestionCard
              key={i}
              q={q}
              idx={i}
              onUpdate={(nq) => updateQuestion(i, nq)}
              onRemove={() => removeQuestion(i)}
            />
          ))}
        </AnimatePresence>
      )}

      <div className="flex justify-between pt-4">
        <button onClick={onBack} className="px-4 py-2 rounded-xl border border-white/10 text-white/50 hover:text-white hover:bg-white/5 transition-all text-sm">
          Back
        </button>
        <button
          onClick={onNext}
          disabled={!valid || isSaving}
          className="btn-primary flex items-center gap-2 disabled:opacity-40 disabled:cursor-not-allowed"
        >
          {isSaving ? (
            <><div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" /> Saving…</>
          ) : (
            <>Review & Publish <ChevronRight className="w-4 h-4" /></>
          )}
        </button>
      </div>
    </div>
  );
}

// ─── Step 3: Review & Publish ─────────────────────────────────────────────────

function Step3({
  examId, form, questions, onBack, onPublish, isPublishing,
}: {
  examId: string;
  form: CreateExamRequest;
  questions: QuestionDraft[];
  onBack: () => void;
  onPublish: () => void;
  isPublishing: boolean;
}) {
  return (
    <div className="space-y-6">
      {/* Exam summary */}
      <div className="glass rounded-2xl p-5 space-y-4">
        <div className="flex items-start gap-3">
          <div className="w-10 h-10 rounded-xl bg-brand-500/20 border border-brand-500/30 flex items-center justify-center flex-shrink-0">
            <BookOpen className="w-5 h-5 text-brand-400" />
          </div>
          <div>
            <h3 className="font-semibold text-white">{form.title}</h3>
            {form.description && <p className="text-white/40 text-sm mt-0.5">{form.description}</p>}
          </div>
          <span className="badge bg-amber-500/15 text-amber-400 ml-auto flex-shrink-0">DRAFT</span>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-sm">
          <div>
            <p className="text-white/40 text-xs">Mode</p>
            <p className="text-white font-medium mt-0.5">{form.mode}</p>
          </div>
          <div>
            <p className="text-white/40 text-xs">Duration</p>
            <p className="text-white font-medium mt-0.5 flex items-center gap-1"><Clock className="w-3.5 h-3.5" />{form.durationMinutes} min</p>
          </div>
          <div>
            <p className="text-white/40 text-xs">Questions</p>
            <p className="text-white font-medium mt-0.5">{questions.length}</p>
          </div>
          <div>
            <p className="text-white/40 text-xs">Total Marks</p>
            <p className="text-white font-medium mt-0.5">{questions.reduce((s, q) => s + q.marks, 0)}</p>
          </div>
        </div>

        {/* Difficulty distribution */}
        <div className="flex items-center gap-3">
          {(['EASY', 'MEDIUM', 'HARD'] as Difficulty[]).map(d => {
            const count = questions.filter(q => q.difficulty === d).length;
            return count > 0 ? (
              <span key={d} className={cn('badge border', DIFFICULTY_STYLES[d])}>
                {d}: {count}
              </span>
            ) : null;
          })}
        </div>
      </div>

      {/* Questions preview */}
      <div className="space-y-2">
        {questions.map((q, i) => (
          <div key={i} className="glass rounded-xl px-4 py-3 flex items-center gap-3">
            <span className="w-6 h-6 rounded-full bg-brand-500/20 text-brand-400 text-xs font-bold flex items-center justify-center flex-shrink-0">{i + 1}</span>
            <p className="text-white/70 text-sm flex-1 line-clamp-1">{q.questionText}</p>
            <span className={cn('badge text-xs border', DIFFICULTY_STYLES[q.difficulty])}>{q.difficulty}</span>
            <span className="text-white/30 text-xs">{q.marks}pt</span>
          </div>
        ))}
      </div>

      <div className="glass rounded-xl p-4 flex items-center gap-3 border border-amber-500/20">
        <AlertCircle className="w-5 h-5 text-amber-400 flex-shrink-0" />
        <p className="text-white/60 text-sm">
          Publishing will make this exam live for the selected batch. You can still add questions after publishing.
        </p>
      </div>

      <div className="flex justify-between pt-2">
        <button onClick={onBack} className="px-4 py-2 rounded-xl border border-white/10 text-white/50 hover:text-white hover:bg-white/5 transition-all text-sm">
          Back
        </button>
        <button
          onClick={onPublish}
          disabled={isPublishing}
          className="btn-primary flex items-center gap-2 disabled:opacity-40"
        >
          {isPublishing ? (
            <><div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" /> Publishing…</>
          ) : (
            <><Eye className="w-4 h-4" /> Publish Exam</>
          )}
        </button>
      </div>
    </div>
  );
}

// ─── My Exams Table ───────────────────────────────────────────────────────────

interface QuestionResponse {
  id: string;
  questionText: string;
  options: string[];
  correctAnswer: number;
  difficulty: number;
  marks: number;
}

// ─── Reschedule Modal ─────────────────────────────────────────────────────────

function RescheduleModal({
  exam, onClose, onSaved,
}: {
  exam: ExamResponse;
  onClose: () => void;
  onSaved: () => void;
}) {
  const toLocalDT = (iso?: string) => {
    if (!iso) return '';
    // Format ISO → datetime-local value (YYYY-MM-DDTHH:mm)
    return iso.slice(0, 16);
  };

  const [startAt, setStartAt] = useState(toLocalDT(exam.startAt));
  const [endAt,   setEndAt]   = useState(toLocalDT(exam.endAt));
  const [saving, setSaving]   = useState(false);

  async function save() {
    if (!startAt || !endAt) { toast.error('Both dates are required'); return; }
    setSaving(true);
    try {
      await api.patch(`/api/v1/exams/${exam.id}/reschedule`, {
        startAt: new Date(startAt).toISOString(),
        endAt:   new Date(endAt).toISOString(),
      });
      toast.success('Exam rescheduled');
      onSaved();
    } catch {
      toast.error('Failed to reschedule exam');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        exit={{ opacity: 0, scale: 0.95 }}
        className="glass rounded-2xl p-6 w-full max-w-md border border-white/10 space-y-5"
      >
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 text-white">
            <Calendar className="w-5 h-5 text-brand-400" />
            <h3 className="font-semibold">Reschedule Exam</h3>
          </div>
          <button onClick={onClose} className="p-1.5 rounded-lg text-white/30 hover:text-white hover:bg-white/5 transition-colors">
            <X className="w-4 h-4" />
          </button>
        </div>

        <p className="text-white/50 text-sm line-clamp-1">{exam.title}</p>

        <div className="space-y-3">
          <div>
            <label className="block text-sm text-white/60 mb-1.5">New Start Date/Time</label>
            <input
              type="datetime-local"
              className="input w-full"
              value={startAt}
              onChange={e => setStartAt(e.target.value)}
            />
          </div>
          <div>
            <label className="block text-sm text-white/60 mb-1.5">New End Date/Time</label>
            <input
              type="datetime-local"
              className="input w-full"
              value={endAt}
              onChange={e => setEndAt(e.target.value)}
            />
          </div>
        </div>

        <div className="flex gap-3 pt-1">
          <button
            onClick={onClose}
            className="flex-1 px-4 py-2 rounded-xl border border-white/10 text-white/50 hover:text-white hover:bg-white/5 transition-all text-sm"
          >
            Cancel
          </button>
          <button
            onClick={save}
            disabled={saving || !startAt || !endAt}
            className="flex-1 btn-primary flex items-center justify-center gap-2 disabled:opacity-40"
          >
            {saving ? <><Loader2 className="w-4 h-4 animate-spin" /> Saving…</> : 'Save Schedule'}
          </button>
        </div>
      </motion.div>
    </div>
  );
}

// ─── My Exams Table ───────────────────────────────────────────────────────────

function MyExamsTable() {
  const queryClient = useQueryClient();
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null);
  const [rescheduleExam, setRescheduleExam] = useState<ExamResponse | null>(null);
  const [publishingId, setPublishingId] = useState<string | null>(null);

  const { data: centers = [] } = useQuery<CenterResponse[]>({
    queryKey: ['teacher-centers'],
    queryFn: async () => {
      const res = await api.get('/api/v1/centers');
      const d = res.data;
      return Array.isArray(d) ? d : (d.content ?? []);
    },
    staleTime: 60_000,
  });

  const { data: exams = [], isLoading } = useQuery<ExamResponse[]>({
    queryKey: ['teacher-exams', centers.map(c => c.id)],
    queryFn: async () => {
      const all: ExamResponse[] = [];
      const seen = new Set<string>();
      for (const c of centers) {
        const res = await api.get(`/api/v1/exams?centerId=${c.id}`);
        const d = res.data;
        const items: ExamResponse[] = Array.isArray(d) ? d : (d.content ?? []);
        items.forEach(e => { if (!seen.has(e.id)) { seen.add(e.id); all.push(e); } });
      }
      return all;
    },
    enabled: centers.length > 0,
    retry: false,
  });

  const { data: questions = [], isFetching: questionsLoading } = useQuery<QuestionResponse[]>({
    queryKey: ['exam-questions', expandedId],
    queryFn: async () => {
      const res = await api.get(`/api/v1/exams/${expandedId}/questions`);
      const d = res.data;
      return Array.isArray(d) ? d : (d.content ?? []);
    },
    enabled: !!expandedId,
    retry: false,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => api.delete(`/api/v1/exams/${id}`),
    onSuccess: () => {
      toast.success('Exam cancelled');
      queryClient.invalidateQueries({ queryKey: ['teacher-exams'] });
      setConfirmDeleteId(null);
    },
    onError: () => toast.error('Failed to cancel exam'),
  });

  const publishDraftMutation = useMutation({
    mutationFn: (id: string) => api.put(`/api/v1/exams/${id}/publish`),
    onSuccess: () => {
      toast.success('Exam published — students can now enroll!');
      queryClient.invalidateQueries({ queryKey: ['teacher-exams'] });
      setPublishingId(null);
    },
    onError: () => {
      toast.error('Failed to publish exam');
      setPublishingId(null);
    },
  });

  if (isLoading) {
    return (
      <div className="space-y-2">
        {[...Array(3)].map((_, i) => (
          <div key={i} className="h-14 glass rounded-xl animate-pulse" />
        ))}
      </div>
    );
  }

  if (exams.length === 0) {
    return (
      <div className="glass rounded-2xl p-8 flex flex-col items-center gap-3 text-white/30">
        <ClipboardList className="w-10 h-10 opacity-40" />
        <p className="text-sm">No exams created yet</p>
      </div>
    );
  }

  const OPTION_LABELS = ['A', 'B', 'C', 'D'];

  return (
    <>
      {/* Reschedule modal */}
      <AnimatePresence>
        {rescheduleExam && (
          <RescheduleModal
            exam={rescheduleExam}
            onClose={() => setRescheduleExam(null)}
            onSaved={() => {
              setRescheduleExam(null);
              queryClient.invalidateQueries({ queryKey: ['teacher-exams'] });
            }}
          />
        )}
      </AnimatePresence>

      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-white/40 text-xs uppercase tracking-wider border-b border-white/5">
              <th className="text-left py-3 pr-4">Title</th>
              <th className="text-left py-3 pr-4">Mode</th>
              <th className="text-left py-3 pr-4">Duration</th>
              <th className="text-left py-3 pr-4">Questions</th>
              <th className="text-left py-3 pr-4">Status</th>
              <th className="py-3 pr-2 text-right">Actions</th>
              <th className="py-3 w-8" />
            </tr>
          </thead>
          <tbody>
            {exams.map((exam) => (
              <>
                <tr
                  key={exam.id}
                  onClick={() => setExpandedId(expandedId === exam.id ? null : exam.id)}
                  className="border-b border-white/5 hover:bg-white/2 transition-colors cursor-pointer"
                >
                  <td className="py-3 pr-4 text-white font-medium">{exam.title}</td>
                  <td className="py-3 pr-4 text-white/60">{exam.mode}</td>
                  <td className="py-3 pr-4 text-white/60">
                    <span className="flex items-center gap-1"><Clock className="w-3.5 h-3.5" />{exam.durationMinutes} min</span>
                  </td>
                  <td className="py-3 pr-4 text-white/60">{exam.totalQuestions ?? '—'}</td>
                  <td className="py-3 pr-4">
                    <span className={cn('badge', STATUS_STYLES[exam.status] ?? 'bg-white/10 text-white/50')}>
                      {exam.status}
                    </span>
                  </td>
                  <td className="py-3 pr-2 text-right" onClick={e => e.stopPropagation()}>
                    {exam.status !== 'CANCELLED' && exam.status !== 'CLOSED' && (
                      <div className="flex items-center justify-end gap-1.5">
                        {/* Publish (DRAFT only) */}
                        {exam.status === 'DRAFT' && (
                          <button
                            title="Publish exam — make visible to students"
                            onClick={() => { setPublishingId(exam.id); publishDraftMutation.mutate(exam.id); }}
                            disabled={publishingId === exam.id && publishDraftMutation.isPending}
                            className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 hover:bg-emerald-500/30 disabled:opacity-50 transition-all text-xs font-medium"
                          >
                            {publishingId === exam.id && publishDraftMutation.isPending
                              ? <Loader2 className="w-3 h-3 animate-spin" />
                              : <Eye className="w-3 h-3" />}
                            Publish
                          </button>
                        )}
                        {/* Reschedule */}
                        <button
                          title="Reschedule"
                          onClick={() => setRescheduleExam(exam)}
                          className="p-1.5 rounded-lg text-brand-400/60 hover:text-brand-400 hover:bg-brand-500/10 transition-colors"
                        >
                          <Calendar className="w-3.5 h-3.5" />
                        </button>
                        {/* Cancel / Delete */}
                        {confirmDeleteId === exam.id ? (
                          <div className="flex items-center gap-1">
                            <span className="text-xs text-red-400">Sure?</span>
                            <button
                              onClick={() => deleteMutation.mutate(exam.id)}
                              disabled={deleteMutation.isPending}
                              className="px-2 py-1 rounded-lg bg-red-500/20 text-red-400 text-xs hover:bg-red-500/30 transition-colors"
                            >
                              {deleteMutation.isPending ? <Loader2 className="w-3 h-3 animate-spin" /> : 'Yes'}
                            </button>
                            <button
                              onClick={() => setConfirmDeleteId(null)}
                              className="px-2 py-1 rounded-lg glass text-white/40 text-xs hover:text-white/60 transition-colors"
                            >
                              No
                            </button>
                          </div>
                        ) : (
                          <button
                            title="Cancel exam"
                            onClick={() => setConfirmDeleteId(exam.id)}
                            className="p-1.5 rounded-lg text-red-400/50 hover:text-red-400 hover:bg-red-500/10 transition-colors"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        )}
                      </div>
                    )}
                  </td>
                  <td className="py-3 text-white/30">
                    {expandedId === exam.id ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                  </td>
                </tr>
                {expandedId === exam.id && (
                  <tr key={`${exam.id}-questions`}>
                    <td colSpan={7} className="pb-4 pt-2 px-2">
                      {exam.status === 'DRAFT' && (
                        <div className="flex items-center gap-2 px-4 py-2.5 mb-3 rounded-xl bg-amber-500/10 border border-amber-500/25 text-amber-400 text-xs">
                          <AlertCircle className="w-3.5 h-3.5 flex-shrink-0" />
                          <span>This exam is in <strong>DRAFT</strong> — click <strong>Publish</strong> in the actions column to make it available to students.</span>
                        </div>
                      )}
                      {questionsLoading ? (
                        <div className="space-y-2">
                          {[...Array(3)].map((_, i) => <div key={i} className="h-10 glass rounded-lg animate-pulse" />)}
                        </div>
                      ) : questions.length === 0 ? (
                        <p className="text-white/30 text-xs py-3 text-center">No questions added yet.</p>
                      ) : (
                        <div className="space-y-2">
                          {questions.map((q, i) => (
                            <div key={q.id} className="glass rounded-xl p-4 border border-white/5 space-y-2">
                              <p className="text-white/80 text-sm font-medium">
                                <span className="text-brand-400 mr-2">Q{i + 1}.</span>{q.questionText}
                              </p>
                              <div className="grid grid-cols-1 sm:grid-cols-2 gap-1.5">
                                {q.options.map((opt, oi) => (
                                  <div
                                    key={oi}
                                    className={cn(
                                      'flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs',
                                      oi === q.correctAnswer
                                        ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30'
                                        : 'bg-white/3 text-white/50 border border-white/5'
                                    )}
                                  >
                                    <span className="font-bold flex-shrink-0">{OPTION_LABELS[oi]}.</span>
                                    {opt}
                                    {oi === q.correctAnswer && <CheckCircle2 className="w-3 h-3 ml-auto flex-shrink-0" />}
                                  </div>
                                ))}
                              </div>
                              <div className="flex items-center gap-3 text-xs text-white/30">
                                <span>{q.marks} mark{q.marks !== 1 ? 's' : ''}</span>
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </td>
                  </tr>
                )}
              </>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function MentorPortalExamsPage() {
  const user = useAuthStore((s) => s.user);
  const queryClient = useQueryClient();

  const [step, setStep] = useState(0);
  const [form, setForm] = useState<CreateExamRequest>(blankDraft);
  const [questions, setQuestions] = useState<QuestionDraft[]>([blankQuestion()]);
  const [createdExamId, setCreatedExamId] = useState<string | null>(null);
  const [showWizard, setShowWizard] = useState(false);

  // Fetch all centers (teachers work across centers, not just ones they own)
  const { data: centers = [], isLoading: centersLoading } = useQuery<CenterResponse[]>({
    queryKey: ['teacher-centers'],
    queryFn: async () => {
      const res = await api.get('/api/v1/centers');
      const d = res.data;
      return Array.isArray(d) ? d : (d.content ?? []);
    },
    enabled: !!user?.id,
    retry: false,
  });

  // Fetch batches for selected center
  const { data: batches = [], isLoading: batchesLoading } = useQuery<BatchResponse[]>({
    queryKey: ['teacher-batches', form.centerId],
    queryFn: async () => {
      const res = await api.get(`/api/v1/centers/${form.centerId}/batches`);
      const d = res.data;
      return Array.isArray(d) ? d : (d.content ?? []);
    },
    enabled: !!form.centerId,
    retry: false,
  });

  // Step 1 → Step 2: create exam (DRAFT)
  const createExamMutation = useMutation({
    mutationFn: (payload: CreateExamRequest) => api.post('/api/v1/exams', {
      ...payload,
      startAt: payload.startAt ? new Date(payload.startAt).toISOString() : undefined,
      endAt:   payload.endAt   ? new Date(payload.endAt).toISOString()   : undefined,
    }),
    onSuccess: (res) => {
      const id: string = res.data?.id ?? res.data;
      setCreatedExamId(id);
      setStep(1);
      toast.success('Exam created as draft!');
    },
    onError: () => toast.error('Failed to create exam. Please try again.'),
  });

  // Step 2 → Step 3: save all questions
  const saveQuestionsMutation = useMutation({
    mutationFn: async () => {
      if (!createdExamId) throw new Error('No exam id');
      for (const q of questions) {
        await api.post(`/api/v1/exams/${createdExamId}/questions`, {
          questionText: q.questionText,
          options: q.options,
          correctAnswer: q.correctAnswer,
          explanation: q.explanation || undefined,
          marks: q.marks,
          difficulty: q.irtDifficulty,   // backend expects IRT b-param float
          discrimination: q.discrimination,
          guessingParam: q.guessingParam,
        });
      }
    },
    onSuccess: () => {
      setStep(2);
      toast.success(`${questions.length} question${questions.length !== 1 ? 's' : ''} saved!`);
    },
    onError: () => toast.error('Failed to save questions. Please try again.'),
  });

  // Step 3: publish
  const publishMutation = useMutation({
    mutationFn: () => api.put(`/api/v1/exams/${createdExamId}/publish`),
    onSuccess: () => {
      toast.success('Exam published successfully!');
      queryClient.invalidateQueries({ queryKey: ['teacher-exams'] });
      // Reset wizard
      setStep(0);
      setForm(blankDraft());
      setQuestions([blankQuestion()]);
      setCreatedExamId(null);
      setShowWizard(false);
    },
    onError: () => toast.error('Failed to publish. Please try again.'),
  });

  function handleStep1Next() {
    createExamMutation.mutate(form);
  }

  function handleStep2Next() {
    saveQuestionsMutation.mutate();
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
      className="p-6 space-y-8 max-w-4xl mx-auto"
    >
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <ClipboardList className="w-6 h-6 text-brand-400" />
            Exam Management
          </h1>
          <p className="text-white/40 text-sm mt-0.5">Create, publish, and track your exams</p>
        </div>
        {!showWizard && (
          <button
            onClick={() => setShowWizard(true)}
            className="btn-primary flex items-center gap-2"
          >
            <Plus className="w-4 h-4" /> Create Exam
          </button>
        )}
      </div>

      {/* Wizard */}
      <AnimatePresence>
        {showWizard && (
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -12 }}
            className="glass rounded-2xl p-6 border border-white/10"
          >
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-lg font-semibold text-white">
                {step === 0 ? 'Exam Details' : step === 1 ? 'Add Questions' : 'Review & Publish'}
              </h2>
              <button
                onClick={() => { setShowWizard(false); setStep(0); setForm(blankDraft()); setQuestions([blankQuestion()]); setCreatedExamId(null); }}
                className="text-white/30 hover:text-white/60 text-sm transition-colors"
              >
                Cancel
              </button>
            </div>

            <StepIndicator step={step} />

            {step === 0 && (
              <Step1
                form={form}
                setForm={setForm}
                centers={centers}
                batches={batches}
                centersLoading={centersLoading}
                batchesLoading={batchesLoading}
                onNext={handleStep1Next}
              />
            )}
            {step === 1 && (
              <Step2
                examTitle={form.title}
                questions={questions}
                setQuestions={setQuestions}
                onBack={() => setStep(0)}
                onNext={handleStep2Next}
                isSaving={saveQuestionsMutation.isPending}
              />
            )}
            {step === 2 && createdExamId && (
              <Step3
                examId={createdExamId}
                form={form}
                questions={questions}
                onBack={() => setStep(1)}
                onPublish={() => publishMutation.mutate()}
                isPublishing={publishMutation.isPending}
              />
            )}
          </motion.div>
        )}
      </AnimatePresence>

      {/* My Exams */}
      <div className="card">
        <div className="flex items-center gap-2 mb-5">
          <BookOpen className="w-5 h-5 text-brand-400" />
          <h2 className="text-lg font-semibold text-white">My Exams</h2>
        </div>
        <MyExamsTable />
      </div>
    </motion.div>
  );
}
