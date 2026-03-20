// src/pages/assignments/StudentAssignmentsPage.tsx
import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  BookCheck, Clock, CheckCircle2, AlertTriangle, Loader2,
  FileText, X, Send, Calendar,
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

type TabKey = 'all' | 'pending' | 'submitted' | 'graded';

// ─── Submit Modal ─────────────────────────────────────────────────────────────

function SubmitModal({
  assignment,
  onClose,
  onSubmit,
  isSubmitting,
}: {
  assignment: AssignmentResponse;
  onClose: () => void;
  onSubmit: (text: string) => void;
  isSubmitting: boolean;
}) {
  const [text, setText] = useState('');

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
        <div className="bg-surface-50 border border-white/10 rounded-2xl shadow-2xl overflow-hidden">
          <div className="flex items-center justify-between px-6 py-4 border-b border-white/5">
            <div>
              <h3 className="font-semibold text-white">Submit Assignment</h3>
              <p className="text-xs text-white/40 mt-0.5">{assignment.title}</p>
            </div>
            <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-white/5 text-white/30 hover:text-white/70 transition-colors">
              <X className="w-4 h-4" />
            </button>
          </div>
          <div className="p-6 space-y-4">
            {assignment.instructions && (
              <div className="bg-white/3 rounded-xl p-3 border border-white/5">
                <p className="text-xs text-white/50 font-medium mb-1">Instructions</p>
                <p className="text-sm text-white/70">{assignment.instructions}</p>
              </div>
            )}
            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Your Response</label>
              <textarea
                value={text}
                onChange={(e) => setText(e.target.value)}
                rows={6}
                placeholder="Write your answer here…"
                className="input w-full resize-none"
              />
            </div>
            <div className="flex gap-3 pt-2">
              <button type="button" onClick={onClose} className="flex-1 py-2.5 rounded-xl border border-white/10 text-sm font-medium text-white/60 hover:text-white hover:border-white/20 transition-colors">
                Cancel
              </button>
              <button
                onClick={() => onSubmit(text)}
                disabled={isSubmitting || !text.trim()}
                className="flex-1 btn-primary py-2.5 text-sm font-medium flex items-center justify-center gap-2 disabled:opacity-50"
              >
                {isSubmitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
                {isSubmitting ? 'Submitting…' : 'Submit'}
              </button>
            </div>
          </div>
        </div>
      </motion.div>
    </>
  );
}

// ─── Assignment Card ──────────────────────────────────────────────────────────

function AssignmentCard({
  assignment,
  submission,
  onSubmit,
}: {
  assignment: AssignmentResponse;
  submission?: AssignmentSubmissionResponse;
  onSubmit: (a: AssignmentResponse) => void;
}) {
  const dueDate = new Date(assignment.dueDate);
  const isPastDue = dueDate < new Date();
  const canSubmit = assignment.status === 'PUBLISHED' && (!submission || submission.status === 'PENDING');

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      className="card hover:border-white/10 transition-colors"
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
            {submission && (
              <span className={cn('badge text-xs', subStatusColors[submission.status] ?? 'bg-white/10 text-white/40')}>
                {submission.status}
              </span>
            )}
          </div>
          <h3 className="font-semibold text-white text-sm">{assignment.title}</h3>
          {assignment.description && (
            <p className="text-xs text-white/50 mt-1 line-clamp-2">{assignment.description}</p>
          )}
          <div className="flex items-center gap-4 mt-3 text-xs text-white/40">
            <span className={cn('flex items-center gap-1', isPastDue ? 'text-red-400' : '')}>
              <Calendar className="w-3 h-3" />
              Due {dueDate.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}
              {isPastDue && ' (overdue)'}
            </span>
            <span className="flex items-center gap-1">
              <FileText className="w-3 h-3" />
              {assignment.totalMarks} marks
            </span>
          </div>
          {submission?.score !== undefined && (
            <div className="mt-2 text-xs">
              <span className="text-emerald-400 font-semibold">Score: {submission.score}/{assignment.totalMarks}</span>
              {submission.feedback && <span className="text-white/40 ml-2">· {submission.feedback}</span>}
            </div>
          )}
        </div>
        {canSubmit && (
          <button
            onClick={() => onSubmit(assignment)}
            className="flex-shrink-0 btn-primary px-4 py-2 text-xs font-medium flex items-center gap-1.5"
          >
            <Send className="w-3.5 h-3.5" />
            Submit
          </button>
        )}
      </div>
    </motion.div>
  );
}

// ─── Main page ────────────────────────────────────────────────────────────────

export default function StudentAssignmentsPage() {
  const userId = useAuthStore((s) => s.user?.id);
  const centerId = useAuthStore((s) => s.user?.centerId);
  const qc = useQueryClient();
  const [activeTab, setActiveTab] = useState<TabKey>('all');
  const [submitting, setSubmitting] = useState<AssignmentResponse | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Fetch assignments for this center
  const { data: assignments = [], isLoading, isError } = useQuery<AssignmentResponse[]>({
    queryKey: ['assignments', centerId],
    queryFn: () =>
      api.get(`/api/v1/assignments?centerId=${centerId}`).then((r) => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
    enabled: !!centerId,
  });

  // Fetch student submissions
  const { data: submissions = [] } = useQuery<AssignmentSubmissionResponse[]>({
    queryKey: ['student-submissions', userId],
    queryFn: () =>
      api.get(`/api/v1/students/${userId}/assignments`).then((r) => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
    enabled: !!userId,
  });

  const submissionMap = new Map(submissions.map((s) => [s.assignmentId, s]));

  const submitMutation = useMutation({
    mutationFn: ({ id, text }: { id: string; text: string }) =>
      api.post(`/api/v1/assignments/${id}/submissions`, { textResponse: text }),
    onSuccess: () => {
      toast.success('Assignment submitted!');
      qc.invalidateQueries({ queryKey: ['student-submissions', userId] });
      setSubmitting(null);
      setIsSubmitting(false);
    },
    onError: () => {
      toast.error('Failed to submit assignment.');
      setIsSubmitting(false);
    },
  });

  function handleSubmit(text: string) {
    if (!submitting) return;
    setIsSubmitting(true);
    submitMutation.mutate({ id: submitting.id, text });
  }

  // Tab filtering — only PUBLISHED for students
  const published = assignments.filter((a) => a.status === 'PUBLISHED');

  const filtered = (() => {
    switch (activeTab) {
      case 'pending':
        return published.filter((a) => {
          const sub = submissionMap.get(a.id);
          return !sub || sub.status === 'PENDING';
        });
      case 'submitted':
        return published.filter((a) => {
          const sub = submissionMap.get(a.id);
          return sub && (sub.status === 'SUBMITTED' || sub.status === 'LATE');
        });
      case 'graded':
        return published.filter((a) => {
          const sub = submissionMap.get(a.id);
          return sub && sub.status === 'GRADED';
        });
      default:
        return published;
    }
  })();

  const tabs: { key: TabKey; label: string; icon: React.ElementType }[] = [
    { key: 'all',       label: 'All',       icon: BookCheck },
    { key: 'pending',   label: 'Pending',   icon: Clock },
    { key: 'submitted', label: 'Submitted', icon: CheckCircle2 },
    { key: 'graded',    label: 'Graded',    icon: AlertTriangle },
  ];

  return (
    <div className="p-4 lg:p-8 space-y-6 max-w-4xl mx-auto">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-white">Assignments</h1>
        <p className="text-white/50 text-sm mt-0.5">View and submit your assignments.</p>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 bg-white/3 rounded-xl p-1 border border-white/5 overflow-x-auto">
        {tabs.map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            onClick={() => setActiveTab(key)}
            className={cn(
              'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors whitespace-nowrap flex-shrink-0',
              activeTab === key
                ? 'bg-brand-500/20 text-white border border-brand-500/30'
                : 'text-white/40 hover:text-white/70'
            )}
          >
            <Icon className="w-4 h-4" />
            {label}
          </button>
        ))}
      </div>

      {/* Content */}
      {!centerId && (
        <div className="card text-center py-12">
          <BookCheck className="w-10 h-10 text-white/20 mx-auto mb-3" />
          <p className="text-white/50 text-sm">No center linked to your account.</p>
          <p className="text-white/30 text-xs mt-1">Contact your administrator.</p>
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

      {centerId && !isLoading && !isError && filtered.length === 0 && (
        <div className="card text-center py-12">
          <BookCheck className="w-10 h-10 text-white/20 mx-auto mb-3" />
          <p className="text-white/50 text-sm">No assignments found.</p>
        </div>
      )}

      {centerId && !isLoading && !isError && (
        <div className="space-y-4">
          {filtered.map((a) => (
            <AssignmentCard
              key={a.id}
              assignment={a}
              submission={submissionMap.get(a.id)}
              onSubmit={(a) => setSubmitting(a)}
            />
          ))}
        </div>
      )}

      {/* Submit modal */}
      <AnimatePresence>
        {submitting && (
          <SubmitModal
            assignment={submitting}
            onClose={() => setSubmitting(null)}
            onSubmit={handleSubmit}
            isSubmitting={isSubmitting}
          />
        )}
      </AnimatePresence>
    </div>
  );
}
