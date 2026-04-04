import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Plus, X, AlertTriangle, CheckCircle2, Clock,
  BookOpen, Send, HelpCircle, ChevronDown, ChevronUp,
  FlaskConical, AlertCircle, Eye, Zap, BarChart3, ChevronRight, Trash2,
} from 'lucide-react';
import { useQuery, useQueries, useMutation, useQueryClient } from '@tanstack/react-query';
import { cn } from '../../lib/utils';
import { toast } from 'sonner';
import api from '../../lib/api';

// ─── API Types ──────────────────────────────────────────────────────────────

interface CenterResponse {
  id: string;
  name: string;
  code: string;
  status: string;
}

interface BatchResponse {
  id: string;
  name: string;
  code: string;
  subject: string;
  status: string;
}

type ExamMode   = 'ONLINE' | 'OFFLINE';
type ExamStatus = 'DRAFT' | 'PUBLISHED' | 'COMPLETED' | 'CANCELLED';

interface ExamResponse {
  id: string;
  title: string;
  description: string;
  batchId: string;
  centerId: string;
  mode: ExamMode;
  durationMinutes: number;
  maxAttempts: number;
  totalMarks: number;
  passingMarks: number;
  startAt: string;
  endAt: string;
  status: ExamStatus;
  createdAt: string;
}

interface CreateExamRequest {
  title: string;
  description: string;
  batchId: string;
  centerId: string;
  mode: ExamMode;
  durationMinutes: number;
  maxAttempts: number;
  totalMarks: number;
  passingMarks: number;
  startAt?: string;
  endAt?: string;
}

interface QuestionResponse {
  id: string;
  examId: string;
  questionText: string;
  options: string[];
  correctAnswer: number;
  marks: number;
}

interface CreateQuestionRequest {
  questionText: string;
  options: string[];
  correctAnswer: number;
  explanation: string;
  marks: number;
  difficulty: number;
  discrimination: number;
  guessingParam: number;
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

function formatDateTime(iso: string): string {
  if (!iso) return '—';
  try {
    return new Date(iso).toLocaleString('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });
  } catch {
    return iso;
  }
}

// ─── Badge helpers ────────────────────────────────────────────────────────────

const examStatusColors: Record<ExamStatus, string> = {
  DRAFT:     'bg-white/5 text-white/50',
  PUBLISHED: 'bg-emerald-500/15 text-emerald-400',
  COMPLETED: 'bg-sky-500/15 text-sky-400',
  CANCELLED: 'bg-red-500/15 text-red-400',
};

const examStatusIcons: Record<ExamStatus, React.ElementType> = {
  DRAFT:     Clock,
  PUBLISHED: CheckCircle2,
  COMPLETED: BookOpen,
  CANCELLED: X,
};

const examModeColors: Record<ExamMode, string> = {
  ONLINE:  'bg-cyan-500/15 text-cyan-400',
  OFFLINE: 'bg-violet-500/15 text-violet-400',
};

// ─── Loading skeleton ─────────────────────────────────────────────────────────

function Skeleton({ className }: { className?: string }) {
  return <div className={cn('animate-pulse bg-white/5 rounded-lg', className)} />;
}

// ─── Wizard Types & Helpers ───────────────────────────────────────────────────

type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';

interface QuestionDraft {
  questionText: string;
  options: [string, string, string, string];
  correctAnswer: number;
  explanation: string;
  marks: number;
  difficulty: Difficulty;
  irtDifficulty: number;
  discrimination: number;
  guessingParam: number;
  showAdvanced: boolean;
}

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

function blankDraft(): CreateExamRequest {
  return {
    title: '', description: '', batchId: '', centerId: '',
    mode: 'ONLINE', durationMinutes: 60, maxAttempts: 1,
    startAt: '', endAt: '', totalMarks: 100, passingMarks: 40,
  };
}

function blankQuestion(): QuestionDraft {
  return {
    questionText: '', options: ['', '', '', ''], correctAnswer: 0,
    explanation: '', marks: 1, difficulty: 'MEDIUM',
    ...IRT_PRESETS.MEDIUM, showAdvanced: false,
  } as QuestionDraft;
}

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
  function set<K extends keyof CreateExamRequest>(key: K, val: CreateExamRequest[K]) {
    setForm({ ...form, [key]: val });
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
          <label className="block text-sm text-white/60 mb-1.5">Description</label>
          <textarea
            className="input w-full resize-none"
            rows={3}
            placeholder="Brief description of what this exam covers…"
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
  questions, setQuestions, onBack, onNext, isSaving,
}: {
  questions: QuestionDraft[];
  setQuestions: (q: QuestionDraft[]) => void;
  onBack: () => void;
  onNext: () => void;
  isSaving: boolean;
}) {
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
      <div className="flex items-center justify-between mb-2">
        <p className="text-white/50 text-sm">{questions.length} question{questions.length !== 1 ? 's' : ''} added</p>
        <button
          type="button"
          onClick={addQuestion}
          className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-brand-500/15 text-brand-400 border border-brand-500/25 hover:bg-brand-500/25 transition-all text-sm font-medium"
        >
          <Plus className="w-4 h-4" /> Add Question
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

// ─── Create Exam Form ─────────────────────────────────────────────────────────

interface CreateExamFormState {
  title: string;
  description: string;
  batchId: string;
  mode: ExamMode;
  durationMinutes: string;
  maxAttempts: string;
  totalMarks: string;
  passingMarks: string;
  startAt: string;
  endAt: string;
}

const emptyExamForm: CreateExamFormState = {
  title: '', description: '', batchId: '', mode: 'ONLINE',
  durationMinutes: '', maxAttempts: '1', totalMarks: '', passingMarks: '', startAt: '', endAt: '',
};

interface CreateExamFormProps {
  centerId: string;
  batches: BatchResponse[];
  onSubmit: (data: CreateExamRequest) => void;
  onCancel: () => void;
  isSubmitting: boolean;
}

function CreateExamForm({ centerId, batches, onSubmit, onCancel, isSubmitting }: CreateExamFormProps) {
  const [form, setForm] = useState<CreateExamFormState>(emptyExamForm);
  const [errors, setErrors] = useState<Partial<Record<keyof CreateExamFormState, string>>>({});

  function validate(): boolean {
    const e: Partial<Record<keyof CreateExamFormState, string>> = {};
    if (!form.title.trim())          e.title          = 'Title is required';
    if (!form.batchId)               e.batchId        = 'Batch is required';
    if (!form.startAt)               e.startAt        = 'Start date/time is required';
    if (!form.endAt)                 e.endAt          = 'End date/time is required';
    if (form.startAt && form.endAt && new Date(form.endAt) <= new Date(form.startAt))
      e.endAt = 'End must be after start';
    if (!form.durationMinutes || isNaN(Number(form.durationMinutes)) || Number(form.durationMinutes) < 1)
      e.durationMinutes = 'Duration must be a positive number';
    if (!form.totalMarks || isNaN(Number(form.totalMarks)) || Number(form.totalMarks) < 1)
      e.totalMarks = 'Total marks must be a positive number';
    if (!form.passingMarks || isNaN(Number(form.passingMarks)) || Number(form.passingMarks) < 1)
      e.passingMarks = 'Passing marks must be a positive number';
    if (Number(form.passingMarks) > Number(form.totalMarks))
      e.passingMarks = 'Passing marks cannot exceed total marks';
    setErrors(e);
    return Object.keys(e).length === 0;
  }

  function handleSubmit(ev: React.FormEvent) {
    ev.preventDefault();
    if (!validate()) return;
    onSubmit({
      title:           form.title.trim(),
      description:     form.description.trim(),
      batchId:         form.batchId,
      centerId,
      mode:            form.mode,
      durationMinutes: Number(form.durationMinutes),
      maxAttempts:     Number(form.maxAttempts) || 1,
      totalMarks:      Number(form.totalMarks),
      passingMarks:    Number(form.passingMarks),
      startAt:         new Date(form.startAt).toISOString(),
      endAt:           new Date(form.endAt).toISOString(),
    });
  }

  function setField<K extends keyof CreateExamFormState>(key: K, value: CreateExamFormState[K]) {
    setForm((p) => ({ ...p, [key]: value }));
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: -8 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -8 }}
      className="card border border-brand-500/30 mb-6"
    >
      <div className="flex items-center justify-between mb-4">
        <div>
          <h3 className="font-semibold text-white">Create New Exam</h3>
          <p className="text-xs text-white/40 mt-0.5">Fill in the exam details</p>
        </div>
        <button onClick={onCancel} className="p-1.5 rounded-lg hover:bg-white/5 text-white/30 hover:text-white/70 transition-colors">
          <X className="w-4 h-4" />
        </button>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Title + Mode */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="sm:col-span-2">
            <label className="block text-xs font-medium text-white/60 mb-1.5">Exam Title</label>
            <input
              value={form.title}
              onChange={(e) => setField('title', e.target.value)}
              placeholder="Mid-Term Physics Test"
              className="input w-full"
            />
            {errors.title && <p className="text-xs text-red-400 mt-1">{errors.title}</p>}
          </div>
          <div>
            <label className="block text-xs font-medium text-white/60 mb-1.5">Mode</label>
            <select
              value={form.mode}
              onChange={(e) => setField('mode', e.target.value as ExamMode)}
              className="input w-full"
            >
              <option value="ONLINE">Online</option>
              <option value="OFFLINE">Offline</option>
            </select>
          </div>
        </div>

        {/* Description */}
        <div>
          <label className="block text-xs font-medium text-white/60 mb-1.5">Description</label>
          <textarea
            value={form.description}
            onChange={(e) => setField('description', e.target.value)}
            placeholder="Brief description of the exam scope…"
            rows={2}
            className="input w-full resize-none"
          />
        </div>

        {/* Batch */}
        <div>
          <label className="block text-xs font-medium text-white/60 mb-1.5">Batch</label>
          <select
            value={form.batchId}
            onChange={(e) => setField('batchId', e.target.value)}
            className="input w-full"
          >
            <option value="">— Select batch —</option>
            {batches.map((b) => (
              <option key={b.id} value={b.id}>{b.name} ({b.subject})</option>
            ))}
          </select>
          {errors.batchId && <p className="text-xs text-red-400 mt-1">{errors.batchId}</p>}
        </div>

        {/* Start + End */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-medium text-white/60 mb-1.5">Start Date & Time</label>
            <input
              type="datetime-local"
              value={form.startAt}
              onChange={(e) => setField('startAt', e.target.value)}
              className="input w-full"
            />
            {errors.startAt && <p className="text-xs text-red-400 mt-1">{errors.startAt}</p>}
          </div>
          <div>
            <label className="block text-xs font-medium text-white/60 mb-1.5">End Date & Time</label>
            <input
              type="datetime-local"
              value={form.endAt}
              onChange={(e) => setField('endAt', e.target.value)}
              className="input w-full"
            />
            {errors.endAt && <p className="text-xs text-red-400 mt-1">{errors.endAt}</p>}
          </div>
        </div>

        {/* Duration + Attempts + Marks */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div>
            <label className="block text-xs font-medium text-white/60 mb-1.5">Duration (min)</label>
            <input
              type="number"
              min={1}
              value={form.durationMinutes}
              onChange={(e) => setField('durationMinutes', e.target.value)}
              placeholder="90"
              className="input w-full"
            />
            {errors.durationMinutes && <p className="text-xs text-red-400 mt-1">{errors.durationMinutes}</p>}
          </div>
          <div>
            <label className="block text-xs font-medium text-white/60 mb-1.5">Max Attempts</label>
            <input
              type="number"
              min={1}
              value={form.maxAttempts}
              onChange={(e) => setField('maxAttempts', e.target.value)}
              placeholder="1"
              className="input w-full"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-white/60 mb-1.5">Total Marks</label>
            <input
              type="number"
              min={1}
              value={form.totalMarks}
              onChange={(e) => setField('totalMarks', e.target.value)}
              placeholder="100"
              className="input w-full"
            />
            {errors.totalMarks && <p className="text-xs text-red-400 mt-1">{errors.totalMarks}</p>}
          </div>
          <div>
            <label className="block text-xs font-medium text-white/60 mb-1.5">Passing Marks</label>
            <input
              type="number"
              min={1}
              value={form.passingMarks}
              onChange={(e) => setField('passingMarks', e.target.value)}
              placeholder="40"
              className="input w-full"
            />
            {errors.passingMarks && <p className="text-xs text-red-400 mt-1">{errors.passingMarks}</p>}
          </div>
        </div>

        <div className="flex gap-3 pt-2">
          <button
            type="button"
            onClick={onCancel}
            className="py-2.5 px-5 rounded-xl border border-white/10 text-sm font-medium text-white/60 hover:text-white hover:border-white/20 transition-colors"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            className="btn-primary py-2.5 px-6 text-sm font-medium disabled:opacity-50"
          >
            {isSubmitting ? 'Creating…' : 'Create Exam'}
          </button>
        </div>
      </form>
    </motion.div>
  );
}

// ─── Add Question Form ────────────────────────────────────────────────────────

interface AddQuestionFormState {
  questionText: string;
  option0: string;
  option1: string;
  option2: string;
  option3: string;
  correctAnswer: string;
  marks: string;
}

const emptyQuestionForm: AddQuestionFormState = {
  questionText: '', option0: '', option1: '', option2: '', option3: '',
  correctAnswer: '', marks: '',
};

interface AddQuestionFormProps {
  examId: string;
  onSubmit: (examId: string, data: CreateQuestionRequest) => void;
  onCancel: () => void;
  isSubmitting: boolean;
}

function AddQuestionForm({ examId, onSubmit, onCancel, isSubmitting }: AddQuestionFormProps) {
  const [form, setForm] = useState<AddQuestionFormState>(emptyQuestionForm);
  const [errors, setErrors] = useState<Partial<Record<keyof AddQuestionFormState, string>>>({});

  const options = [form.option0, form.option1, form.option2, form.option3];
  const filledOptions = options.filter((o) => o.trim() !== '');

  function validate(): boolean {
    const e: Partial<Record<keyof AddQuestionFormState, string>> = {};
    if (!form.questionText.trim()) e.questionText = 'Question text is required';
    if (!form.option0.trim())      e.option0      = 'Option A is required';
    if (!form.option1.trim())      e.option1      = 'Option B is required';
    if (!form.option2.trim())      e.option2      = 'Option C is required';
    if (!form.option3.trim())      e.option3      = 'Option D is required';
    if (form.correctAnswer === '')  e.correctAnswer = 'Select the correct answer';
    if (!form.marks || isNaN(Number(form.marks)) || Number(form.marks) < 1)
      e.marks = 'Marks must be a positive number';
    setErrors(e);
    return Object.keys(e).length === 0;
  }

  function handleSubmit(ev: React.FormEvent) {
    ev.preventDefault();
    if (!validate()) return;
    onSubmit(examId, {
      questionText:   form.questionText.trim(),
      options:        options.map((o) => o.trim()),
      correctAnswer:  Number(form.correctAnswer),
      explanation:    '',
      marks:          Number(form.marks),
      difficulty:     0.5,
      discrimination: 1.0,
      guessingParam:  0.25,
    });
  }

  return (
    <motion.div
      initial={{ opacity: 0, height: 0 }}
      animate={{ opacity: 1, height: 'auto' }}
      exit={{ opacity: 0, height: 0 }}
      className="overflow-hidden"
    >
      <form onSubmit={handleSubmit} className="mt-4 pt-4 border-t border-white/5 space-y-3">
        <div>
          <label className="block text-xs font-medium text-white/60 mb-1">Question Text</label>
          <textarea
            value={form.questionText}
            onChange={(e) => setForm((p) => ({ ...p, questionText: e.target.value }))}
            placeholder="Enter your question here…"
            rows={2}
            className="input w-full resize-none text-sm"
          />
          {errors.questionText && <p className="text-xs text-red-400 mt-1">{errors.questionText}</p>}
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {(['option0', 'option1', 'option2', 'option3'] as const).map((key, i) => (
            <div key={key}>
              <label className="block text-xs font-medium text-white/60 mb-1">
                Option {String.fromCharCode(65 + i)}
              </label>
              <input
                value={form[key]}
                onChange={(e) => setForm((p) => ({ ...p, [key]: e.target.value }))}
                placeholder={`Option ${String.fromCharCode(65 + i)}`}
                className="input w-full text-sm"
              />
              {errors[key] && <p className="text-xs text-red-400 mt-1">{errors[key]}</p>}
            </div>
          ))}
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-medium text-white/60 mb-1">Correct Answer</label>
            <select
              value={form.correctAnswer}
              onChange={(e) => setForm((p) => ({ ...p, correctAnswer: e.target.value }))}
              className="input w-full text-sm"
            >
              <option value="">— Select —</option>
              {filledOptions.map((opt, i) => (
                <option key={i} value={i}>{String.fromCharCode(65 + i)}: {opt}</option>
              ))}
            </select>
            {errors.correctAnswer && <p className="text-xs text-red-400 mt-1">{errors.correctAnswer}</p>}
          </div>
          <div>
            <label className="block text-xs font-medium text-white/60 mb-1">Marks</label>
            <input
              type="number"
              min={1}
              value={form.marks}
              onChange={(e) => setForm((p) => ({ ...p, marks: e.target.value }))}
              placeholder="4"
              className="input w-full text-sm"
            />
            {errors.marks && <p className="text-xs text-red-400 mt-1">{errors.marks}</p>}
          </div>
        </div>

        <div className="flex gap-2">
          <button
            type="button"
            onClick={onCancel}
            className="py-2 px-4 rounded-xl border border-white/10 text-xs font-medium text-white/60 hover:text-white hover:border-white/20 transition-colors"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            className="btn-primary py-2 px-4 text-xs font-medium disabled:opacity-50 flex items-center gap-1.5"
          >
            <Send className="w-3 h-3" />
            {isSubmitting ? 'Adding…' : 'Add Question'}
          </button>
        </div>
      </form>
    </motion.div>
  );
}

// ─── Exam card ────────────────────────────────────────────────────────────────

interface ExamCardProps {
  exam: ExamResponse;
  questions: QuestionResponse[];
  questionsLoading: boolean;
  onPublish: (examId: string) => void;
  isPublishing: boolean;
  addingQuestionFor: string | null;
  setAddingQuestionFor: (id: string | null) => void;
  onAddQuestion: (examId: string, data: CreateQuestionRequest) => void;
  isAddingQuestion: boolean;
  batchName: string;
  delay: number;
}

function ExamCard({
  exam, questions, questionsLoading,
  onPublish, isPublishing,
  addingQuestionFor, setAddingQuestionFor,
  onAddQuestion, isAddingQuestion,
  batchName, delay,
}: ExamCardProps) {
  const [showQuestions, setShowQuestions] = useState(false);

  const StatusIcon = examStatusIcons[exam.status] ?? Clock;
  const statusColor = examStatusColors[exam.status] ?? 'bg-white/5 text-white/50';
  const modeColor   = examModeColors[exam.mode]   ?? 'bg-white/5 text-white/50';

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay }}
      className="card"
    >
      {/* Card header */}
      <div className="flex items-start justify-between gap-3 mb-3">
        <div className="min-w-0 flex-1">
          <h4 className="font-semibold text-white text-sm leading-snug">{exam.title}</h4>
          {exam.description && (
            <p className="text-xs text-white/40 mt-0.5 leading-relaxed line-clamp-2">{exam.description}</p>
          )}
        </div>
        <div className="flex items-center gap-2 flex-shrink-0">
          <span className={cn('inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium', statusColor)}>
            <StatusIcon className="w-3 h-3" />
            {exam.status}
          </span>
          <span className={cn('inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium', modeColor)}>
            {exam.mode}
          </span>
        </div>
      </div>

      {/* Metadata row */}
      <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-white/40 mb-3">
        <span className="flex items-center gap-1">
          <Clock className="w-3 h-3" /> {exam.durationMinutes} min
        </span>
        <span>Total: <span className="text-white/60 font-medium">{exam.totalMarks}</span> marks</span>
        <span>Pass: <span className="text-white/60 font-medium">{exam.passingMarks}</span></span>
        {batchName && <span>Batch: <span className="text-white/60">{batchName}</span></span>}
        <span className="flex items-center gap-1">
          <BookOpen className="w-3 h-3" /> Starts: {formatDateTime(exam.startAt)}
        </span>
      </div>

      {/* Question count + actions */}
      <div className="flex items-center gap-2 flex-wrap">
        {questionsLoading ? (
          <Skeleton className="h-6 w-24" />
        ) : (
          <button
            onClick={() => setShowQuestions((p) => !p)}
            className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-white/5 hover:bg-white/8 text-xs text-white/50 hover:text-white/80 transition-colors"
          >
            <HelpCircle className="w-3 h-3" />
            {questions.length} question{questions.length !== 1 ? 's' : ''}
            {showQuestions ? <ChevronUp className="w-3 h-3" /> : <ChevronDown className="w-3 h-3" />}
          </button>
        )}

        {exam.status === 'DRAFT' && (
          <button
            onClick={() => onPublish(exam.id)}
            disabled={isPublishing}
            className="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg bg-emerald-500/15 hover:bg-emerald-500/25 text-emerald-400 text-xs font-medium transition-colors disabled:opacity-50"
          >
            <Send className="w-3 h-3" />
            {isPublishing ? 'Publishing…' : 'Publish'}
          </button>
        )}

        <button
          onClick={() =>
            setAddingQuestionFor(addingQuestionFor === exam.id ? null : exam.id)
          }
          className="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg bg-brand-500/15 hover:bg-brand-500/25 text-brand-400 text-xs font-medium transition-colors"
        >
          <Plus className="w-3 h-3" />
          Add Question
        </button>
      </div>

      {/* Questions list */}
      <AnimatePresence>
        {showQuestions && questions.length > 0 && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            className="overflow-hidden"
          >
            <div className="mt-3 pt-3 border-t border-white/5 space-y-2">
              {questions.map((q, idx) => (
                <div key={q.id} className="bg-white/3 rounded-xl p-3">
                  <div className="flex items-start gap-2">
                    <span className="text-xs text-white/30 font-mono flex-shrink-0 mt-0.5">Q{idx + 1}</span>
                    <div className="min-w-0">
                      <p className="text-xs text-white/70 leading-relaxed">{q.questionText}</p>
                      <div className="grid grid-cols-2 gap-1 mt-1.5">
                        {q.options.map((opt, i) => (
                          <span
                            key={i}
                            className={cn(
                              'text-xs px-2 py-0.5 rounded',
                              i === q.correctAnswer
                                ? 'bg-emerald-500/15 text-emerald-400'
                                : 'text-white/30'
                            )}
                          >
                            {String.fromCharCode(65 + i)}. {opt}
                          </span>
                        ))}
                      </div>
                      <p className="text-xs text-white/30 mt-1">{q.marks} mark{q.marks !== 1 ? 's' : ''}</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Add question form */}
      <AnimatePresence>
        {addingQuestionFor === exam.id && (
          <AddQuestionForm
            examId={exam.id}
            onSubmit={onAddQuestion}
            onCancel={() => setAddingQuestionFor(null)}
            isSubmitting={isAddingQuestion}
          />
        )}
      </AnimatePresence>
    </motion.div>
  );
}

// ─── Main page ────────────────────────────────────────────────────────────────

export default function AdminAssessmentsPage() {
  const qc = useQueryClient();
  const [showCreateForm, setShowCreateForm]           = useState(false);
  const [addingQuestionFor, setAddingQuestionFor]     = useState<string | null>(null);
  const [publishingId, setPublishingId]               = useState<string | null>(null);
  const [selectedCenterId, setSelectedCenterId]       = useState<string>('');

  // ── Exam creation wizard state ─────────────────────────────────────────────
  const [showWizard, setShowWizard]                   = useState(false);
  const [wizardStep, setWizardStep]                   = useState(0);
  const [wizardForm, setWizardForm]                   = useState<CreateExamRequest>(blankDraft);
  const [wizardQuestions, setWizardQuestions]         = useState<QuestionDraft[]>([blankQuestion()]);
  const [createdExamId, setCreatedExamId]             = useState<string | null>(null);

  // ── Fetch centers ──────────────────────────────────────────────────────────
  const { data: centers = [], isLoading: centersLoading } = useQuery<CenterResponse[]>({
    queryKey: ['centers'],
    queryFn: () =>
      api.get('/api/v1/centers').then((r) => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
  });

  // Auto-select the single centre when org has only one
  useEffect(() => {
    if (centers && centers.length === 1 && !selectedCenterId) {
      setSelectedCenterId(centers[0].id);
    }
  }, [centers, selectedCenterId]);

  const centerId = selectedCenterId || '';

  // ── Fetch batches for dropdown ─────────────────────────────────────────────
  const { data: batches = [] } = useQuery<BatchResponse[]>({
    queryKey: ['batches', centerId],
    queryFn: () =>
      api.get(`/api/v1/centers/${centerId}/batches`).then((r) => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
    enabled: !!centerId,
  });

  // ── Fetch exams ────────────────────────────────────────────────────────────
  const {
    data: exams = [],
    isLoading: examsLoading,
    error: examsError,
  } = useQuery<ExamResponse[]>({
    queryKey: ['exams', centerId],
    queryFn: () =>
      api.get(`/api/v1/exams?centerId=${centerId}`).then((r) => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
    enabled: !!centerId,
  });

  // ── Fetch questions for each exam (parallel) ───────────────────────────────
  const questionQueries = useQueries({
    queries: exams.map((exam) => ({
      queryKey: ['questions', exam.id],
      queryFn: () =>
        api.get(`/api/v1/exams/${exam.id}/questions`).then((r) => {
          const d = r.data;
          return Array.isArray(d) ? d : (d.content ?? []);
        }) as Promise<QuestionResponse[]>,
      enabled: !!exam.id,
    })),
  });

  // ── Create exam mutation ───────────────────────────────────────────────────
  const createExam = useMutation({
    mutationFn: (data: CreateExamRequest) => api.post('/api/v1/exams', data),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['exams'] });
      setShowCreateForm(false);
      toast.success(`Exam "${vars.title}" created as DRAFT`);
    },
    onError: (err: unknown) => {
      const msg =
        (err as { response?: { data?: { detail?: string; message?: string } } })?.response?.data?.detail ??
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Failed to create exam';
      toast.error(msg);
    },
  });

  // ── Publish exam mutation ──────────────────────────────────────────────────
  const publishExam = useMutation({
    mutationFn: (examId: string) => api.put(`/api/v1/exams/${examId}/publish`),
    onSuccess: (_, examId) => {
      qc.invalidateQueries({ queryKey: ['exams'] });
      setPublishingId(null);
      const exam = exams.find((e) => e.id === examId);
      toast.success(`Exam "${exam?.title ?? examId}" published`);
    },
    onError: (err: unknown) => {
      setPublishingId(null);
      const msg =
        (err as { response?: { data?: { detail?: string; message?: string } } })?.response?.data?.detail ??
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Failed to publish exam';
      toast.error(msg);
    },
  });

  // ── Add question mutation ──────────────────────────────────────────────────
  const addQuestion = useMutation({
    mutationFn: ({ examId, data }: { examId: string; data: CreateQuestionRequest }) =>
      api.post(`/api/v1/exams/${examId}/questions`, data),
    onSuccess: (_, { examId }) => {
      qc.invalidateQueries({ queryKey: ['questions', examId] });
      setAddingQuestionFor(null);
      toast.success('Question added successfully');
    },
    onError: (err: unknown) => {
      const msg =
        (err as { response?: { data?: { detail?: string; message?: string } } })?.response?.data?.detail ??
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Failed to add question';
      toast.error(msg);
    },
  });

  // ── Wizard mutations ───────────────────────────────────────────────────────
  const createExamWizard = useMutation({
    mutationFn: (payload: CreateExamRequest) => api.post('/api/v1/exams', {
      ...payload,
      startAt: payload.startAt || undefined,
      endAt:   payload.endAt   || undefined,
    }),
    onSuccess: (res) => {
      const id: string = res.data?.id ?? res.data;
      setCreatedExamId(id);
      setWizardStep(1);
      toast.success('Exam created as draft!');
    },
    onError: () => toast.error('Failed to create exam. Please try again.'),
  });

  const saveQuestionsMutation = useMutation({
    mutationFn: async () => {
      if (!createdExamId) throw new Error('No exam id');
      for (const q of wizardQuestions) {
        await api.post(`/api/v1/exams/${createdExamId}/questions`, {
          questionText: q.questionText,
          options: q.options,
          correctAnswer: q.correctAnswer,
          explanation: q.explanation || undefined,
          marks: q.marks,
          difficulty: q.irtDifficulty,
          discrimination: q.discrimination,
          guessingParam: q.guessingParam,
        });
      }
    },
    onSuccess: () => {
      setWizardStep(2);
      toast.success(`${wizardQuestions.length} question${wizardQuestions.length !== 1 ? 's' : ''} saved!`);
    },
    onError: () => toast.error('Failed to save questions.'),
  });

  const publishWizardMutation = useMutation({
    mutationFn: () => api.put(`/api/v1/exams/${createdExamId}/publish`),
    onSuccess: () => {
      toast.success('Exam published successfully!');
      qc.invalidateQueries({ queryKey: ['exams'] });
      setShowWizard(false);
      setWizardStep(0);
      setWizardForm(blankDraft());
      setWizardQuestions([blankQuestion()]);
      setCreatedExamId(null);
    },
    onError: () => toast.error('Failed to publish.'),
  });

  // Sync wizard centerId into selectedCenterId so batches refetch
  function setWizardFormAndSync(f: CreateExamRequest) {
    setWizardForm(f);
    if (f.centerId !== wizardForm.centerId) setSelectedCenterId(f.centerId);
  }

  function handlePublish(examId: string) {
    setPublishingId(examId);
    publishExam.mutate(examId);
  }

  function handleAddQuestion(examId: string, data: CreateQuestionRequest) {
    addQuestion.mutate({ examId, data });
  }

  // Build batch lookup map
  const batchMap = new Map(batches.map((b) => [b.id, b.name]));

  const isLoading = centersLoading || examsLoading;

  // KPI counts
  const draftCount     = exams.filter((e) => e.status === 'DRAFT').length;
  const publishedCount = exams.filter((e) => e.status === 'PUBLISHED').length;
  const completedCount = exams.filter((e) => e.status === 'COMPLETED').length;

  return (
    <div className="p-4 lg:p-8 space-y-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">Assessments</h1>
          <p className="text-white/50 text-sm mt-0.5">
            {centerId
              ? `Create and manage exams — ${centers.find(c => c.id === centerId)?.name ?? ''}`
              : 'Create and manage exams.'}
          </p>
        </div>
        {!showWizard && (
          <button
            onClick={() => setShowWizard(true)}
            className="btn-primary flex items-center gap-2 px-4 py-2.5 text-sm font-medium"
          >
            <Plus className="w-4 h-4" /> Create Exam
          </button>
        )}
      </div>

      {/* Summary row */}
      {!isLoading && exams.length > 0 && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          {[
            { label: 'Total Exams',  value: exams.length,     color: 'bg-brand-500/15 text-brand-400'     },
            { label: 'Draft',        value: draftCount,        color: 'bg-white/5 text-white/50'           },
            { label: 'Published',    value: publishedCount,    color: 'bg-emerald-500/15 text-emerald-400' },
            { label: 'Completed',    value: completedCount,    color: 'bg-sky-500/15 text-sky-400'         },
          ].map(({ label, value, color }, i) => (
            <motion.div
              key={label}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.05 }}
              className="card text-center"
            >
              <p className="text-2xl font-bold text-white">{value}</p>
              <span className={cn('inline-block mt-1 px-2 py-0.5 rounded-full text-xs font-medium', color)}>{label}</span>
            </motion.div>
          ))}
        </div>
      )}

      {/* Center picker — only shown for multi-centre orgs when no centre is selected yet */}
      {!selectedCenterId && centers && centers.length > 1 && (
        <div className="mb-6">
          <label className="block text-sm font-medium text-white/70 mb-2">Select Centre</label>
          <select
            className="input w-full max-w-sm"
            value={selectedCenterId}
            onChange={e => setSelectedCenterId(e.target.value)}
          >
            <option value="">— Select a centre —</option>
            {centers.map(c => (
              <option key={c.id} value={c.id}>{c.name} ({c.code})</option>
            ))}
          </select>
        </div>
      )}

      {/* 3-Step Wizard */}
      <AnimatePresence>
        {showWizard && (
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -12 }}
            className="glass rounded-2xl p-6 border border-white/10 mb-6"
          >
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-lg font-semibold text-white">
                {wizardStep === 0 ? 'Exam Details' : wizardStep === 1 ? 'Add Questions' : 'Review & Publish'}
              </h2>
              <button
                onClick={() => {
                  setShowWizard(false);
                  setWizardStep(0);
                  setWizardForm(blankDraft());
                  setWizardQuestions([blankQuestion()]);
                  setCreatedExamId(null);
                }}
                className="text-white/30 hover:text-white/60 text-sm transition-colors"
              >
                Cancel
              </button>
            </div>

            <StepIndicator step={wizardStep} />

            {wizardStep === 0 && (
              <Step1
                form={wizardForm}
                setForm={setWizardFormAndSync}
                centers={centers}
                batches={batches}
                centersLoading={centersLoading}
                batchesLoading={false}
                onNext={() => createExamWizard.mutate(wizardForm)}
              />
            )}
            {wizardStep === 1 && (
              <Step2
                questions={wizardQuestions}
                setQuestions={setWizardQuestions}
                onBack={() => setWizardStep(0)}
                onNext={() => saveQuestionsMutation.mutate()}
                isSaving={saveQuestionsMutation.isPending}
              />
            )}
            {wizardStep === 2 && createdExamId && (
              <Step3
                examId={createdExamId}
                form={wizardForm}
                questions={wizardQuestions}
                onBack={() => setWizardStep(1)}
                onPublish={() => publishWizardMutation.mutate()}
                isPublishing={publishWizardMutation.isPending}
              />
            )}
          </motion.div>
        )}
      </AnimatePresence>

      {/* Exam cards */}
      {isLoading ? (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {[0, 1, 2, 3].map((i) => <Skeleton key={i} className="h-40" />)}
        </div>
      ) : examsError ? (
        <div className="card py-12 text-center">
          <AlertTriangle className="w-8 h-8 text-red-400 mx-auto mb-2" />
          <p className="text-red-400 text-sm">Failed to load exams. Please try again.</p>
        </div>
      ) : !centerId ? (
        <div className="card py-12 text-center text-white/30 text-sm">
          No center found. Please ensure your account is linked to a center.
        </div>
      ) : exams.length === 0 ? (
        <div className="card py-16 text-center">
          <BookOpen className="w-10 h-10 text-white/10 mx-auto mb-3" />
          <p className="text-white/30 text-sm">No exams yet.</p>
          <p className="text-white/20 text-xs mt-1">Click "Create Exam" to add the first assessment.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {exams.map((exam, idx) => (
            <ExamCard
              key={exam.id}
              exam={exam}
              questions={questionQueries[idx]?.data ?? []}
              questionsLoading={questionQueries[idx]?.isLoading ?? false}
              onPublish={handlePublish}
              isPublishing={publishingId === exam.id && publishExam.isPending}
              addingQuestionFor={addingQuestionFor}
              setAddingQuestionFor={setAddingQuestionFor}
              onAddQuestion={handleAddQuestion}
              isAddingQuestion={addQuestion.isPending && addingQuestionFor === exam.id}
              batchName={batchMap.get(exam.batchId) ?? ''}
              delay={idx * 0.05}
            />
          ))}
        </div>
      )}
    </div>
  );
}
