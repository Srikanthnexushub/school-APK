// src/pages/mentor-portal/MentorPortalAssignmentsPage.tsx
import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  BookCheck, Plus, X, Loader2, AlertTriangle, ChevronRight,
  FileText, Calendar, Users, CheckCircle2, Star,
} from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { cn } from '../../lib/utils';
import { useAuthStore } from '../../stores/authStore';
import api from '../../lib/api';

// ─── Types ────────────────────────────────────────────────────────────────────

interface AssignmentResponse {
  id: string;
  batchId: string;
  centerId: string;
  createdByUserId: string;
  title: string;
  description?: string;
  type: 'HOMEWORK' | 'CLASSWORK' | 'PROJECT' | 'QUIZ' | 'PRACTICE';
  dueDate: string;
  totalMarks: number;
  passingMarks: number;
  instructions?: string;
  attachmentUrl?: string;
  status: 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'CANCELLED';
  createdAt: string;
  submissionCount?: number;
}

interface AssignmentSubmissionResponse {
  id: string;
  assignmentId: string;
  studentId: string;
  textResponse?: string;
  score?: number;
  feedback?: string;
  status: 'PENDING' | 'SUBMITTED' | 'LATE' | 'GRADED';
  submittedAt?: string;
  gradedAt?: string;
  createdAt: string;
}

interface CreateAssignmentRequest {
  batchId: string;
  centerId: string;
  title: string;
  description?: string;
  type: string;
  dueDate: string;
  totalMarks: number;
  passingMarks: number;
  instructions?: string;
  attachmentUrl?: string;
}

// ─── Constants ────────────────────────────────────────────────────────────────

const typeColors: Record<string, string> = {
  HOMEWORK:  'bg-blue-500/15 text-blue-400',
  CLASSWORK: 'bg-brand-500/15 text-brand-400',
  PROJECT:   'bg-violet-500/15 text-violet-400',
  QUIZ:      'bg-amber-500/15 text-amber-400',
  PRACTICE:  'bg-emerald-500/15 text-emerald-400',
};

const statusColors: Record<string, string> = {
  DRAFT:      'bg-white/10 text-white/40',
  PUBLISHED:  'bg-emerald-500/15 text-emerald-400',
  CLOSED:     'bg-white/10 text-white/30',
  CANCELLED:  'bg-red-500/15 text-red-400',
};

const subStatusColors: Record<string, string> = {
  PENDING:   'bg-amber-500/15 text-amber-400',
  SUBMITTED: 'bg-blue-500/15 text-blue-400',
  LATE:      'bg-red-500/15 text-red-400',
  GRADED:    'bg-emerald-500/15 text-emerald-400',
};

// ─── Create Assignment Modal ──────────────────────────────────────────────────

export function CreateAssignmentModal({
  centerId,
  onClose,
  onCreated,
}: {
  centerId: string;
  onClose: () => void;
  onCreated?: (a: AssignmentResponse) => void;
}) {
  const qc = useQueryClient();
  const [form, setForm] = useState<Partial<CreateAssignmentRequest>>({
    centerId,
    type: 'HOMEWORK',
    totalMarks: 100,
    passingMarks: 40,
  });

  const createMutation = useMutation({
    mutationFn: (data: CreateAssignmentRequest) => api.post('/api/v1/assignments', data),
    onSuccess: (res) => {
      toast.success('Assignment created!');
      qc.invalidateQueries({ queryKey: ['assignments', centerId] });
      onCreated?.(res.data);
      onClose();
    },
    onError: () => toast.error('Failed to create assignment.'),
  });

  function submit() {
    if (!form.batchId?.trim() || !form.title?.trim() || !form.dueDate) {
      toast.error('Batch ID, title, and due date are required.');
      return;
    }
    createMutation.mutate(form as CreateAssignmentRequest);
  }

  return (
    <>
      <motion.div
        initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
        onClick={onClose}
        className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm"
      />
      <motion.div
        initial={{ opacity: 0, scale: 0.95, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: 20 }}
        transition={{ duration: 0.2 }}
        className="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-[51] w-full max-w-lg"
      >
        <div className="bg-surface-50 border border-white/10 rounded-2xl shadow-2xl overflow-hidden max-h-[90vh] overflow-y-auto">
          <div className="flex items-center justify-between px-6 py-4 border-b border-white/5 sticky top-0 bg-surface-50 z-10">
            <div>
              <h3 className="font-semibold text-white">Create Assignment</h3>
              <p className="text-xs text-white/40 mt-0.5">Fill in the details below</p>
            </div>
            <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-white/5 text-white/30 hover:text-white/70 transition-colors">
              <X className="w-4 h-4" />
            </button>
          </div>

          <div className="p-6 space-y-4">
            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Title *</label>
              <input
                className="input w-full"
                placeholder="e.g. Chapter 5 Homework"
                value={form.title ?? ''}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Description</label>
              <textarea
                className="input w-full resize-none"
                rows={3}
                placeholder="Optional description…"
                value={form.description ?? ''}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">Type</label>
                <select
                  className="input w-full"
                  value={form.type}
                  onChange={(e) => setForm({ ...form, type: e.target.value })}
                >
                  {['HOMEWORK', 'CLASSWORK', 'PROJECT', 'QUIZ', 'PRACTICE'].map((t) => (
                    <option key={t} value={t}>{t}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">Batch ID *</label>
                <input
                  className="input w-full"
                  placeholder="UUID"
                  value={form.batchId ?? ''}
                  onChange={(e) => setForm({ ...form, batchId: e.target.value })}
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Due Date *</label>
              <input
                type="datetime-local"
                className="input w-full"
                value={form.dueDate ?? ''}
                onChange={(e) => setForm({ ...form, dueDate: e.target.value })}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">Total Marks</label>
                <input
                  type="number"
                  className="input w-full"
                  value={form.totalMarks}
                  onChange={(e) => setForm({ ...form, totalMarks: Number(e.target.value) })}
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">Passing Marks</label>
                <input
                  type="number"
                  className="input w-full"
                  value={form.passingMarks}
                  onChange={(e) => setForm({ ...form, passingMarks: Number(e.target.value) })}
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Instructions</label>
              <textarea
                className="input w-full resize-none"
                rows={3}
                placeholder="Instructions for students…"
                value={form.instructions ?? ''}
                onChange={(e) => setForm({ ...form, instructions: e.target.value })}
              />
            </div>

            <div className="flex gap-3 pt-2">
              <button type="button" onClick={onClose} className="flex-1 py-2.5 rounded-xl border border-white/10 text-sm font-medium text-white/60 hover:text-white hover:border-white/20 transition-colors">
                Cancel
              </button>
              <button
                onClick={submit}
                disabled={createMutation.isPending}
                className="flex-1 btn-primary py-2.5 text-sm font-medium flex items-center justify-center gap-2 disabled:opacity-50"
              >
                {createMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                {createMutation.isPending ? 'Creating…' : 'Create Assignment'}
              </button>
            </div>
          </div>
        </div>
      </motion.div>
    </>
  );
}

// ─── Grade Modal ──────────────────────────────────────────────────────────────

function GradeModal({
  assignmentId,
  submission,
  totalMarks,
  onClose,
}: {
  assignmentId: string;
  submission: AssignmentSubmissionResponse;
  totalMarks: number;
  onClose: () => void;
}) {
  const qc = useQueryClient();
  const [score, setScore] = useState<number>(submission.score ?? 0);
  const [feedback, setFeedback] = useState(submission.feedback ?? '');

  const gradeMutation = useMutation({
    mutationFn: () =>
      api.patch(`/api/v1/assignments/${assignmentId}/submissions/${submission.id}/grade`, {
        score,
        feedback,
      }),
    onSuccess: () => {
      toast.success('Graded successfully!');
      qc.invalidateQueries({ queryKey: ['submissions', assignmentId] });
      onClose();
    },
    onError: () => toast.error('Failed to grade submission.'),
  });

  return (
    <>
      <motion.div
        initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
        onClick={onClose}
        className="fixed inset-0 z-[60] bg-black/60 backdrop-blur-sm"
      />
      <motion.div
        initial={{ opacity: 0, scale: 0.95, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: 20 }}
        transition={{ duration: 0.2 }}
        className="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-[61] w-full max-w-md"
      >
        <div className="bg-surface-50 border border-white/10 rounded-2xl shadow-2xl overflow-hidden">
          <div className="flex items-center justify-between px-6 py-4 border-b border-white/5">
            <div>
              <h3 className="font-semibold text-white">Grade Submission</h3>
              <p className="text-xs text-white/40 mt-0.5">Student: {submission.studentId.slice(0, 8)}…</p>
            </div>
            <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-white/5 text-white/30 hover:text-white/70 transition-colors">
              <X className="w-4 h-4" />
            </button>
          </div>
          <div className="p-6 space-y-4">
            {submission.textResponse && (
              <div className="bg-white/3 rounded-xl p-3 border border-white/5">
                <p className="text-xs text-white/50 font-medium mb-1">Student Response</p>
                <p className="text-sm text-white/70 whitespace-pre-wrap">{submission.textResponse}</p>
              </div>
            )}
            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Score (out of {totalMarks})</label>
              <input
                type="number"
                min={0}
                max={totalMarks}
                className="input w-full"
                value={score}
                onChange={(e) => setScore(Number(e.target.value))}
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Feedback</label>
              <textarea
                className="input w-full resize-none"
                rows={3}
                placeholder="Optional feedback for the student…"
                value={feedback}
                onChange={(e) => setFeedback(e.target.value)}
              />
            </div>
            <div className="flex gap-3 pt-2">
              <button type="button" onClick={onClose} className="flex-1 py-2.5 rounded-xl border border-white/10 text-sm font-medium text-white/60 hover:text-white hover:border-white/20 transition-colors">
                Cancel
              </button>
              <button
                onClick={() => gradeMutation.mutate()}
                disabled={gradeMutation.isPending}
                className="flex-1 btn-primary py-2.5 text-sm font-medium flex items-center justify-center gap-2 disabled:opacity-50"
              >
                {gradeMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                {gradeMutation.isPending ? 'Saving…' : 'Save Grade'}
              </button>
            </div>
          </div>
        </div>
      </motion.div>
    </>
  );
}

// ─── Submissions Panel ────────────────────────────────────────────────────────

function SubmissionsPanel({
  assignment,
  onClose,
}: {
  assignment: AssignmentResponse;
  onClose: () => void;
}) {
  const [grading, setGrading] = useState<AssignmentSubmissionResponse | null>(null);

  const { data: submissions = [], isLoading } = useQuery<AssignmentSubmissionResponse[]>({
    queryKey: ['submissions', assignment.id],
    queryFn: () =>
      api.get(`/api/v1/assignments/${assignment.id}/submissions`).then((r) => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
  });

  return (
    <>
      <motion.div
        initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
        onClick={onClose}
        className="fixed inset-0 z-40 bg-black/40"
      />
      <motion.div
        initial={{ x: '100%' }}
        animate={{ x: 0 }}
        exit={{ x: '100%' }}
        transition={{ duration: 0.25, ease: 'easeInOut' }}
        className="fixed right-0 top-0 bottom-0 z-50 w-full max-w-md bg-surface-50 border-l border-white/10 shadow-2xl flex flex-col"
      >
        <div className="flex items-center justify-between px-6 py-4 border-b border-white/5 flex-shrink-0">
          <div>
            <h3 className="font-semibold text-white">Submissions</h3>
            <p className="text-xs text-white/40 mt-0.5">{assignment.title}</p>
          </div>
          <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-white/5 text-white/30 hover:text-white/70 transition-colors">
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-4 space-y-3">
          {isLoading && (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="w-6 h-6 text-brand-400 animate-spin" />
            </div>
          )}
          {!isLoading && submissions.length === 0 && (
            <div className="text-center py-12">
              <FileText className="w-8 h-8 text-white/20 mx-auto mb-2" />
              <p className="text-white/40 text-sm">No submissions yet.</p>
            </div>
          )}
          {submissions.map((sub) => (
            <div key={sub.id} className="card flex items-center justify-between gap-4">
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium text-white">Student: {sub.studentId.slice(0, 12)}…</p>
                <div className="flex items-center gap-2 mt-1">
                  <span className={cn('badge text-xs', subStatusColors[sub.status] ?? 'bg-white/10 text-white/40')}>
                    {sub.status}
                  </span>
                  {sub.score !== undefined && (
                    <span className="text-xs text-emerald-400">{sub.score}/{assignment.totalMarks}</span>
                  )}
                </div>
              </div>
              {(sub.status === 'SUBMITTED' || sub.status === 'LATE') && (
                <button
                  onClick={() => setGrading(sub)}
                  className="flex-shrink-0 flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-amber-500/15 text-amber-400 hover:bg-amber-500/25 transition-colors text-xs font-medium"
                >
                  <Star className="w-3.5 h-3.5" />
                  Grade
                </button>
              )}
              {sub.status === 'GRADED' && (
                <button
                  onClick={() => setGrading(sub)}
                  className="flex-shrink-0 flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white/5 text-white/50 hover:bg-white/10 transition-colors text-xs font-medium"
                >
                  <Star className="w-3.5 h-3.5" />
                  Edit
                </button>
              )}
            </div>
          ))}
        </div>
      </motion.div>

      <AnimatePresence>
        {grading && (
          <GradeModal
            assignmentId={assignment.id}
            submission={grading}
            totalMarks={assignment.totalMarks}
            onClose={() => setGrading(null)}
          />
        )}
      </AnimatePresence>
    </>
  );
}

// ─── Assignment Card ──────────────────────────────────────────────────────────

function AssignmentCard({
  assignment,
  onGrade,
}: {
  assignment: AssignmentResponse;
  onGrade: (a: AssignmentResponse) => void;
}) {
  const qc = useQueryClient();
  const centerId = useAuthStore((s) => s.user?.centerId);

  const publishMutation = useMutation({
    mutationFn: () => api.patch(`/api/v1/assignments/${assignment.id}/publish`),
    onSuccess: () => {
      toast.success('Assignment published!');
      qc.invalidateQueries({ queryKey: ['assignments', centerId] });
    },
    onError: () => toast.error('Failed to publish.'),
  });

  const dueDate = new Date(assignment.dueDate);
  const isPastDue = dueDate < new Date();

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      className="card"
    >
      <div className="flex items-start justify-between gap-4">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap mb-2">
            <span className={cn('badge text-xs', typeColors[assignment.type] ?? 'bg-white/10 text-white/40')}>
              {assignment.type}
            </span>
            <span className={cn('badge text-xs', statusColors[assignment.status] ?? 'bg-white/10 text-white/40')}>
              {assignment.status}
            </span>
          </div>
          <h3 className="font-semibold text-white text-sm">{assignment.title}</h3>
          {assignment.description && (
            <p className="text-xs text-white/50 mt-1 line-clamp-2">{assignment.description}</p>
          )}
          <div className="flex items-center gap-4 mt-3 text-xs text-white/40 flex-wrap">
            <span className={cn('flex items-center gap-1', isPastDue ? 'text-red-400' : '')}>
              <Calendar className="w-3 h-3" />
              Due {dueDate.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}
            </span>
            <span className="flex items-center gap-1">
              <FileText className="w-3 h-3" />
              {assignment.totalMarks} marks
            </span>
            <span className="flex items-center gap-1">
              <Users className="w-3 h-3" />
              {assignment.submissionCount ?? 0} submissions
            </span>
            <span className="text-white/30 font-mono text-[10px]">
              Batch: {assignment.batchId.slice(0, 8)}…
            </span>
          </div>
        </div>

        <div className="flex items-center gap-2 flex-shrink-0">
          {assignment.status === 'DRAFT' && (
            <button
              onClick={() => publishMutation.mutate()}
              disabled={publishMutation.isPending}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-500/15 text-emerald-400 hover:bg-emerald-500/25 transition-colors text-xs font-medium disabled:opacity-50"
            >
              {publishMutation.isPending ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <CheckCircle2 className="w-3.5 h-3.5" />}
              Publish
            </button>
          )}
          {(assignment.status === 'PUBLISHED') && (
            <button
              onClick={() => onGrade(assignment)}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-brand-500/15 text-brand-400 hover:bg-brand-500/25 transition-colors text-xs font-medium"
            >
              <ChevronRight className="w-3.5 h-3.5" />
              Grade
            </button>
          )}
        </div>
      </div>
    </motion.div>
  );
}

// ─── Main page ────────────────────────────────────────────────────────────────

export default function MentorPortalAssignmentsPage() {
  const centerId = useAuthStore((s) => s.user?.centerId);
  const [showCreate, setShowCreate] = useState(false);
  const [gradingAssignment, setGradingAssignment] = useState<AssignmentResponse | null>(null);

  const { data: assignments = [], isLoading, isError } = useQuery<AssignmentResponse[]>({
    queryKey: ['assignments', centerId],
    queryFn: () =>
      api.get(`/api/v1/assignments?centerId=${centerId}`).then((r) => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
    enabled: !!centerId,
  });

  return (
    <div className="p-4 lg:p-8 space-y-6 max-w-4xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">Assignments</h1>
          <p className="text-white/50 text-sm mt-0.5">Create and manage assignments for your batches.</p>
        </div>
        {centerId && (
          <button
            onClick={() => setShowCreate(true)}
            className="btn-primary flex items-center gap-2 px-4 py-2.5 text-sm font-medium"
          >
            <Plus className="w-4 h-4" />
            Create Assignment
          </button>
        )}
      </div>

      {/* Content */}
      {!centerId && (
        <div className="card text-center py-12">
          <BookCheck className="w-10 h-10 text-white/20 mx-auto mb-3" />
          <p className="text-white/50 text-sm">No center linked to your account.</p>
        </div>
      )}

      {centerId && isLoading && (
        <div className="space-y-4">
          {[0, 1, 2].map((i) => (
            <div key={i} className="card animate-pulse">
              <div className="h-4 bg-white/10 rounded w-1/4 mb-3" />
              <div className="h-5 bg-white/10 rounded w-2/3 mb-2" />
              <div className="h-3 bg-white/10 rounded w-1/2" />
            </div>
          ))}
        </div>
      )}

      {centerId && isError && (
        <div className="card text-center py-12">
          <AlertTriangle className="w-10 h-10 text-red-400/50 mx-auto mb-3" />
          <p className="text-white/50 text-sm">Failed to load assignments.</p>
        </div>
      )}

      {centerId && !isLoading && !isError && assignments.length === 0 && (
        <div className="card text-center py-12">
          <BookCheck className="w-10 h-10 text-white/20 mx-auto mb-3" />
          <p className="text-white/50 text-sm">No assignments yet.</p>
          <button
            onClick={() => setShowCreate(true)}
            className="btn-primary inline-flex items-center gap-2 px-4 py-2 text-sm mt-4"
          >
            <Plus className="w-4 h-4" />
            Create First Assignment
          </button>
        </div>
      )}

      {centerId && !isLoading && !isError && (
        <div className="space-y-4">
          {assignments.map((a) => (
            <AssignmentCard
              key={a.id}
              assignment={a}
              onGrade={(a) => setGradingAssignment(a)}
            />
          ))}
        </div>
      )}

      {/* Modals */}
      <AnimatePresence>
        {showCreate && centerId && (
          <CreateAssignmentModal
            centerId={centerId}
            onClose={() => setShowCreate(false)}
          />
        )}
      </AnimatePresence>

      <AnimatePresence>
        {gradingAssignment && (
          <SubmissionsPanel
            assignment={gradingAssignment}
            onClose={() => setGradingAssignment(null)}
          />
        )}
      </AnimatePresence>
    </div>
  );
}
