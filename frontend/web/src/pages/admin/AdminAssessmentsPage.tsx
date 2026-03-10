import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Plus, X, AlertTriangle, CheckCircle2, Clock,
  BookOpen, Send, HelpCircle, ChevronDown, ChevronUp,
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

type ExamMode   = 'STANDARD' | 'CAT';
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
  startAt: string;
  endAt: string;
}

interface QuestionResponse {
  id: string;
  examId: string;
  questionText: string;
  options: string[];
  correctAnswer: string;
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
  STANDARD: 'bg-cyan-500/15 text-cyan-400',
  CAT:      'bg-violet-500/15 text-violet-400',
};

// ─── Loading skeleton ─────────────────────────────────────────────────────────

function Skeleton({ className }: { className?: string }) {
  return <div className={cn('animate-pulse bg-white/5 rounded-lg', className)} />;
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
  title: '', description: '', batchId: '', mode: 'STANDARD',
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
              <option value="STANDARD">Standard</option>
              <option value="CAT">CAT (Adaptive)</option>
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
                              opt === q.correctAnswer
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

  // ── Fetch centers ──────────────────────────────────────────────────────────
  const { data: centers = [], isLoading: centersLoading } = useQuery<CenterResponse[]>({
    queryKey: ['centers'],
    queryFn: () =>
      api.get('/api/v1/centers').then((r) => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
  });

  const centerId = centers[0]?.id ?? '';

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
            Create and manage exams{centers[0] ? ` — ${centers[0].name}` : ''}.
          </p>
        </div>
        {!showCreateForm && (
          <button
            onClick={() => setShowCreateForm(true)}
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

      {/* Create exam inline form */}
      <AnimatePresence>
        {showCreateForm && (
          <CreateExamForm
            centerId={centerId}
            batches={batches}
            onSubmit={(data) => createExam.mutate(data)}
            onCancel={() => setShowCreateForm(false)}
            isSubmitting={createExam.isPending}
          />
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
