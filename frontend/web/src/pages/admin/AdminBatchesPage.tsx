import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Plus, X, AlertTriangle, CheckCircle2, Clock,
  Users, BookOpen, Calendar, ChevronDown, ChevronUp,
} from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
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
  centerId: string;
  name: string;
  code: string;
  subject: string;
  teacherId: string;
  maxStudents: number;
  enrolledCount: number;
  startDate: string;
  endDate: string;
  status: 'ACTIVE' | 'INACTIVE' | 'COMPLETED' | 'UPCOMING';
  createdAt: string;
}

interface BatchHealthSummary {
  batchId: string;
  name: string;
  subject: string;
  enrolledCount: number;
  maxStudents: number;
  fillRate: number;
  healthStatus: 'CRITICAL' | 'WARNING' | 'HEALTHY';
}

interface CreateBatchRequest {
  name: string;
  code: string;
  subject: string;
  teacherId: string;
  maxStudents: number;
  startDate: string;
  endDate: string;
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

function formatDate(iso: string): string {
  if (!iso) return '—';
  try {
    return new Date(iso).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  } catch {
    return iso;
  }
}

// ─── Status badge ────────────────────────────────────────────────────────────

const batchStatusColors: Record<BatchResponse['status'], string> = {
  ACTIVE:    'bg-emerald-500/15 text-emerald-400',
  INACTIVE:  'bg-white/5 text-white/30',
  COMPLETED: 'bg-sky-500/15 text-sky-400',
  UPCOMING:  'bg-amber-500/15 text-amber-400',
};

const batchStatusIcons: Record<BatchResponse['status'], React.ElementType> = {
  ACTIVE:    CheckCircle2,
  INACTIVE:  X,
  COMPLETED: Clock,
  UPCOMING:  Clock,
};

const healthColors: Record<BatchHealthSummary['healthStatus'], string> = {
  CRITICAL: 'bg-red-500/15 text-red-400 border-red-500/20',
  WARNING:  'bg-amber-500/15 text-amber-400 border-amber-500/20',
  HEALTHY:  'bg-emerald-500/15 text-emerald-400 border-emerald-500/20',
};

const healthIcons: Record<BatchHealthSummary['healthStatus'], React.ElementType> = {
  CRITICAL: AlertTriangle,
  WARNING:  AlertTriangle,
  HEALTHY:  CheckCircle2,
};

// ─── Loading skeleton ─────────────────────────────────────────────────────────

function Skeleton({ className }: { className?: string }) {
  return <div className={cn('animate-pulse bg-white/5 rounded-lg', className)} />;
}

// ─── Health summary card ──────────────────────────────────────────────────────

function HealthCard({ summary, delay }: { summary: BatchHealthSummary; delay: number }) {
  const Icon = healthIcons[summary.healthStatus];
  const colorClass = healthColors[summary.healthStatus];
  const fillPct = Math.min(100, Math.round(summary.fillRate * 100));

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay }}
      className={cn('card border', colorClass)}
    >
      <div className="flex items-start justify-between mb-3">
        <div className="min-w-0 flex-1 mr-2">
          <p className="text-sm font-semibold text-white truncate">{summary.name}</p>
          <p className="text-xs text-white/40 mt-0.5">{summary.subject}</p>
        </div>
        <span className={cn('inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium flex-shrink-0', colorClass)}>
          <Icon className="w-3 h-3" />
          {summary.healthStatus}
        </span>
      </div>
      <div className="flex items-center gap-2 text-xs text-white/50 mb-2">
        <Users className="w-3 h-3" />
        {summary.enrolledCount} / {summary.maxStudents} enrolled
      </div>
      <div className="w-full bg-white/5 rounded-full h-1.5">
        <div
          className={cn(
            'h-1.5 rounded-full transition-all',
            summary.healthStatus === 'CRITICAL' ? 'bg-red-400' :
            summary.healthStatus === 'WARNING'  ? 'bg-amber-400' :
                                                   'bg-emerald-400'
          )}
          style={{ width: `${fillPct}%` }}
        />
      </div>
      <p className="text-xs text-white/30 mt-1">{fillPct}% fill rate</p>
    </motion.div>
  );
}

// ─── Add batch form ────────────────────────────────────────────────────────────

interface AddBatchFormState {
  name: string;
  code: string;
  subject: string;
  teacherId: string;
  maxStudents: string;
  startDate: string;
  endDate: string;
}

const emptyForm: AddBatchFormState = {
  name: '', code: '', subject: '', teacherId: '',
  maxStudents: '', startDate: '', endDate: '',
};

interface AddBatchFormProps {
  onSubmit: (data: CreateBatchRequest) => void;
  onCancel: () => void;
  isSubmitting: boolean;
}

function AddBatchForm({ onSubmit, onCancel, isSubmitting }: AddBatchFormProps) {
  const [form, setForm] = useState<AddBatchFormState>(emptyForm);
  const [errors, setErrors] = useState<Partial<AddBatchFormState>>({});

  function validate(): boolean {
    const e: Partial<AddBatchFormState> = {};
    if (!form.name.trim())      e.name       = 'Batch name is required';
    if (!form.code.trim())      e.code       = 'Batch code is required';
    if (!form.subject.trim())   e.subject    = 'Subject is required';
    if (!form.teacherId.trim()) e.teacherId  = 'Teacher ID is required';
    if (!form.maxStudents || isNaN(Number(form.maxStudents)) || Number(form.maxStudents) < 1)
      e.maxStudents = 'Max students must be a positive number';
    if (!form.startDate)        e.startDate  = 'Start date is required';
    if (!form.endDate)          e.endDate    = 'End date is required';
    setErrors(e);
    return Object.keys(e).length === 0;
  }

  function handleSubmit(ev: React.FormEvent) {
    ev.preventDefault();
    if (!validate()) return;
    onSubmit({
      name:        form.name.trim(),
      code:        form.code.trim(),
      subject:     form.subject.trim(),
      teacherId:   form.teacherId.trim(),
      maxStudents: Number(form.maxStudents),
      startDate:   form.startDate,
      endDate:     form.endDate,
    });
  }

  function field(key: keyof AddBatchFormState) {
    return {
      value: form[key],
      onChange: (ev: React.ChangeEvent<HTMLInputElement>) =>
        setForm((p) => ({ ...p, [key]: ev.target.value })),
    };
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
          <h3 className="font-semibold text-white">Add New Batch</h3>
          <p className="text-xs text-white/40 mt-0.5">Fill in the details to create a batch</p>
        </div>
        <button onClick={onCancel} className="p-1.5 rounded-lg hover:bg-white/5 text-white/30 hover:text-white/70 transition-colors">
          <X className="w-4 h-4" />
        </button>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-medium text-white/60 mb-1.5">Batch Name</label>
            <input {...field('name')} placeholder="JEE Advanced Batch A" className="input w-full" />
            {errors.name && <p className="text-xs text-red-400 mt-1">{errors.name}</p>}
          </div>
          <div>
            <label className="block text-xs font-medium text-white/60 mb-1.5">Batch Code</label>
            <input {...field('code')} placeholder="JEE-A-2026" className="input w-full" />
            {errors.code && <p className="text-xs text-red-400 mt-1">{errors.code}</p>}
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-medium text-white/60 mb-1.5">Subject</label>
            <input {...field('subject')} placeholder="Physics, Chemistry, Maths" className="input w-full" />
            {errors.subject && <p className="text-xs text-red-400 mt-1">{errors.subject}</p>}
          </div>
          <div>
            <label className="block text-xs font-medium text-white/60 mb-1.5">Teacher ID</label>
            <input {...field('teacherId')} placeholder="UUID of assigned teacher" className="input w-full" />
            {errors.teacherId && <p className="text-xs text-red-400 mt-1">{errors.teacherId}</p>}
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div>
            <label className="block text-xs font-medium text-white/60 mb-1.5">Max Students</label>
            <input {...field('maxStudents')} type="number" min={1} placeholder="60" className="input w-full" />
            {errors.maxStudents && <p className="text-xs text-red-400 mt-1">{errors.maxStudents}</p>}
          </div>
          <div>
            <label className="block text-xs font-medium text-white/60 mb-1.5">Start Date</label>
            <input {...field('startDate')} type="date" className="input w-full" />
            {errors.startDate && <p className="text-xs text-red-400 mt-1">{errors.startDate}</p>}
          </div>
          <div>
            <label className="block text-xs font-medium text-white/60 mb-1.5">End Date</label>
            <input {...field('endDate')} type="date" className="input w-full" />
            {errors.endDate && <p className="text-xs text-red-400 mt-1">{errors.endDate}</p>}
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
            {isSubmitting ? 'Creating…' : 'Create Batch'}
          </button>
        </div>
      </form>
    </motion.div>
  );
}

// ─── Main page ────────────────────────────────────────────────────────────────

export default function AdminBatchesPage() {
  const qc = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [expandedHealth, setExpandedHealth] = useState(true);

  // ── Fetch centers to get centerId ──────────────────────────────────────────
  const { data: centers = [], isLoading: centersLoading } = useQuery<CenterResponse[]>({
    queryKey: ['centers'],
    queryFn: () =>
      api.get('/api/v1/centers').then((r) => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
  });

  const centerId = centers[0]?.id ?? '';

  // ── Fetch batches ──────────────────────────────────────────────────────────
  const {
    data: batches = [],
    isLoading: batchesLoading,
    error: batchesError,
  } = useQuery<BatchResponse[]>({
    queryKey: ['batches', centerId],
    queryFn: () =>
      api.get(`/api/v1/centers/${centerId}/batches`).then((r) => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
    enabled: !!centerId,
  });

  // ── Fetch health summary ───────────────────────────────────────────────────
  const {
    data: healthSummaries = [],
    isLoading: healthLoading,
  } = useQuery<BatchHealthSummary[]>({
    queryKey: ['batches-health', centerId],
    queryFn: () =>
      api.get(`/api/v1/centers/${centerId}/batches/health-summary`).then((r) => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
    enabled: !!centerId,
  });

  // ── Create batch mutation ──────────────────────────────────────────────────
  const createBatch = useMutation({
    mutationFn: (data: CreateBatchRequest) =>
      api.post(`/api/v1/centers/${centerId}/batches`, data),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['batches', centerId] });
      qc.invalidateQueries({ queryKey: ['batches-health', centerId] });
      setShowForm(false);
      toast.success(`Batch "${vars.name}" created successfully`);
    },
    onError: (err: unknown) => {
      const msg =
        (err as { response?: { data?: { detail?: string; message?: string } } })?.response?.data?.detail ??
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Failed to create batch';
      toast.error(msg);
    },
  });

  const isLoading = centersLoading || batchesLoading;

  return (
    <div className="p-4 lg:p-8 space-y-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">Batches</h1>
          <p className="text-white/50 text-sm mt-0.5">
            Manage and monitor all batches{centers[0] ? ` — ${centers[0].name}` : ''}.
          </p>
        </div>
        {!showForm && (
          <button
            onClick={() => setShowForm(true)}
            className="btn-primary flex items-center gap-2 px-4 py-2.5 text-sm font-medium"
          >
            <Plus className="w-4 h-4" /> Add Batch
          </button>
        )}
      </div>

      {/* Add batch inline form */}
      <AnimatePresence>
        {showForm && (
          <AddBatchForm
            onSubmit={(data) => createBatch.mutate(data)}
            onCancel={() => setShowForm(false)}
            isSubmitting={createBatch.isPending}
          />
        )}
      </AnimatePresence>

      {/* Health summary section */}
      <div className="card">
        <button
          onClick={() => setExpandedHealth((p) => !p)}
          className="flex items-center justify-between w-full"
        >
          <div>
            <h3 className="font-semibold text-white text-sm">Batch Health Overview</h3>
            <p className="text-xs text-white/40 mt-0.5">
              {healthSummaries.length} batches — fill rate & health status
            </p>
          </div>
          {expandedHealth ? (
            <ChevronUp className="w-4 h-4 text-white/30" />
          ) : (
            <ChevronDown className="w-4 h-4 text-white/30" />
          )}
        </button>

        <AnimatePresence initial={false}>
          {expandedHealth && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              transition={{ duration: 0.2 }}
              className="overflow-hidden"
            >
              <div className="mt-4">
                {healthLoading || centersLoading ? (
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
                    {[0, 1, 2, 3].map((i) => <Skeleton key={i} className="h-28" />)}
                  </div>
                ) : healthSummaries.length === 0 ? (
                  <div className="py-8 text-center text-white/30 text-sm">
                    No health data available yet.
                  </div>
                ) : (
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
                    {healthSummaries.map((s, i) => (
                      <HealthCard key={s.batchId} summary={s} delay={i * 0.05} />
                    ))}
                  </div>
                )}

                {/* Summary counters */}
                {healthSummaries.length > 0 && (
                  <div className="flex items-center gap-4 mt-4 pt-4 border-t border-white/5">
                    {(['CRITICAL', 'WARNING', 'HEALTHY'] as const).map((status) => {
                      const count = healthSummaries.filter((s) => s.healthStatus === status).length;
                      return (
                        <span key={status} className={cn('inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium border', healthColors[status])}>
                          {status === 'CRITICAL' ? <AlertTriangle className="w-3 h-3" /> :
                           status === 'WARNING'  ? <AlertTriangle className="w-3 h-3" /> :
                                                   <CheckCircle2  className="w-3 h-3" />}
                          {count} {status}
                        </span>
                      );
                    })}
                  </div>
                )}
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Batches table */}
      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold text-white text-sm">All Batches</h3>
          <span className="text-xs text-white/30">{batches.length} batches</span>
        </div>

        {isLoading ? (
          <div className="space-y-3">
            {[0, 1, 2, 3].map((i) => <Skeleton key={i} className="h-12" />)}
          </div>
        ) : batchesError ? (
          <div className="py-12 text-center">
            <AlertTriangle className="w-8 h-8 text-red-400 mx-auto mb-2" />
            <p className="text-red-400 text-sm">Failed to load batches. Please try again.</p>
          </div>
        ) : !centerId ? (
          <div className="py-12 text-center text-white/30 text-sm">
            No center found. Please ensure your account is linked to a center.
          </div>
        ) : batches.length === 0 ? (
          <div className="py-16 text-center">
            <BookOpen className="w-10 h-10 text-white/10 mx-auto mb-3" />
            <p className="text-white/30 text-sm">No batches yet.</p>
            <p className="text-white/20 text-xs mt-1">Click "Add Batch" to create the first batch.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs text-white/30 uppercase tracking-wider border-b border-white/5">
                  <th className="pb-2 pr-4">Name / Code</th>
                  <th className="pb-2 pr-4">Subject</th>
                  <th className="pb-2 pr-4">Teacher</th>
                  <th className="pb-2 pr-4">Enrolled</th>
                  <th className="pb-2 pr-4">Status</th>
                  <th className="pb-2 pr-4">Start</th>
                  <th className="pb-2">End</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {batches.map((batch) => {
                  const StatusIcon = batchStatusIcons[batch.status] ?? Clock;
                  const colorClass = batchStatusColors[batch.status] ?? 'bg-white/5 text-white/30';
                  const fillPct = batch.maxStudents > 0
                    ? Math.round((batch.enrolledCount / batch.maxStudents) * 100)
                    : 0;

                  return (
                    <motion.tr
                      key={batch.id}
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      className="hover:bg-white/3 transition-colors"
                    >
                      <td className="py-3 pr-4">
                        <div className="font-medium text-white">{batch.name}</div>
                        <div className="text-xs text-white/40 mt-0.5 font-mono">{batch.code}</div>
                      </td>
                      <td className="py-3 pr-4 text-white/70">{batch.subject}</td>
                      <td className="py-3 pr-4">
                        <span className="text-xs text-white/40 font-mono truncate block max-w-[120px]" title={batch.teacherId}>
                          {batch.teacherId ? `${batch.teacherId.substring(0, 8)}…` : '—'}
                        </span>
                      </td>
                      <td className="py-3 pr-4">
                        <div className="flex items-center gap-2">
                          <span className="text-white/70 font-medium">
                            {batch.enrolledCount}/{batch.maxStudents}
                          </span>
                          <div className="w-16 bg-white/5 rounded-full h-1.5 hidden sm:block">
                            <div
                              className="h-1.5 rounded-full bg-brand-400"
                              style={{ width: `${fillPct}%` }}
                            />
                          </div>
                        </div>
                      </td>
                      <td className="py-3 pr-4">
                        <span className={cn('inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium', colorClass)}>
                          <StatusIcon className="w-3 h-3" />
                          {batch.status}
                        </span>
                      </td>
                      <td className="py-3 pr-4">
                        <div className="flex items-center gap-1 text-xs text-white/50">
                          <Calendar className="w-3 h-3" />
                          {formatDate(batch.startDate)}
                        </div>
                      </td>
                      <td className="py-3">
                        <div className="flex items-center gap-1 text-xs text-white/50">
                          <Calendar className="w-3 h-3" />
                          {formatDate(batch.endDate)}
                        </div>
                      </td>
                    </motion.tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
