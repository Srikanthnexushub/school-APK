import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Plus, X, AlertTriangle, CheckCircle2, Clock,
  Users, BookOpen, Calendar, ChevronDown, ChevronUp,
  UserMinus, UserPen, Loader2, UserPlus, Search,
  GraduationCap, Download, Printer,
} from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { cn } from '../../lib/utils';
import { toast } from 'sonner';
import api from '../../lib/api';
import { Modal } from '../../components/ui/Modal';

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
  mode?: 'ONLINE' | 'OFFLINE';
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

interface TeacherResponse {
  id: string;
  userId: string | null;
  firstName: string;
  lastName: string;
  email: string;
  subjects?: string;
  status?: string;
}

interface BatchMemberResponse {
  id: string;
  batchId: string;
  centerId: string;
  studentId: string;
  studentName: string;
  enrolledAt: string;
  active: boolean;
}

interface UserLookupResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

interface CreateBatchRequest {
  name: string;
  code: string;
  subject: string;
  teacherId: string | null;
  maxStudents: number;
  startDate: string;
  endDate: string;
  mode: 'ONLINE' | 'OFFLINE';
  centerId: string; // path param — used to determine URL; not sent in request body
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
  mode: string;
  centerId: string;
}

const emptyForm: AddBatchFormState = {
  name: '', code: '', subject: '', teacherId: '',
  maxStudents: '', startDate: '', endDate: '', mode: 'OFFLINE', centerId: '',
};

interface AddBatchFormProps {
  centers: CenterResponse[];
  teachers: TeacherResponse[];
  onSubmit: (data: CreateBatchRequest) => void;
  onCancel: () => void;
  isSubmitting: boolean;
}

function AddBatchForm({ centers, teachers, onSubmit, onCancel, isSubmitting }: AddBatchFormProps) {
  const [form, setForm] = useState<AddBatchFormState>({ ...emptyForm, centerId: centers[0]?.id ?? '' });
  const [errors, setErrors] = useState<Partial<AddBatchFormState>>({});

  function validate(): boolean {
    const e: Partial<AddBatchFormState> = {};
    if (!form.centerId)         e.centerId   = 'Center is required';
    if (!form.name.trim())      e.name       = 'Batch name is required';
    if (!form.code.trim())      e.code       = 'Batch code is required';
    if (!form.subject.trim())   e.subject    = 'Subject is required';
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
      teacherId:   form.teacherId.trim() || null,
      maxStudents: Number(form.maxStudents),
      startDate:   form.startDate,
      endDate:     form.endDate,
      mode:        (form.mode || 'OFFLINE') as 'ONLINE' | 'OFFLINE',
      centerId:    form.centerId,
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
        {/* Center selector — always visible so admin knows which center the batch belongs to */}
        <div>
          <label className="block text-xs font-medium text-white/60 mb-1.5">Center</label>
          <select
            value={form.centerId}
            onChange={(ev) => setForm((p) => ({ ...p, centerId: ev.target.value }))}
            className="input w-full"
            disabled={centers.length === 1}
          >
            <option value="">— Select center —</option>
            {centers.map((c) => (
              <option key={c.id} value={c.id}>{c.name} ({c.code})</option>
            ))}
          </select>
          {errors.centerId && <p className="text-xs text-red-400 mt-1">{errors.centerId}</p>}
        </div>

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
            <label className="block text-xs font-medium text-white/60 mb-1.5">Teacher</label>
            <select
              value={form.teacherId}
              onChange={(ev) => setForm((p) => ({ ...p, teacherId: ev.target.value }))}
              className="input w-full"
            >
              <option value="">— Select teacher —</option>
              {teachers.filter((t) => !!t.userId && t.status === 'ACTIVE').map((t) => (
                <option key={t.id} value={t.userId!}>
                  {t.firstName} {t.lastName}{t.subjects ? ` (${t.subjects})` : ''}
                </option>
              ))}
              {teachers.some((t) => t.status === 'PENDING_APPROVAL') && (
                <option disabled value="">— {teachers.filter(t => t.status === 'PENDING_APPROVAL').length} pending approval (approve in Staff tab first) —</option>
              )}
            </select>
            {errors.teacherId && <p className="text-xs text-red-400 mt-1">{errors.teacherId}</p>}
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-medium text-white/60 mb-1.5">
              Mode <span className="text-red-400">*</span>
            </label>
            <select
              value={form.mode}
              onChange={(ev) => setForm((p) => ({ ...p, mode: ev.target.value }))}
              className="input w-full"
            >
              <option value="OFFLINE">Offline</option>
              <option value="ONLINE">Online</option>
            </select>
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

// ─── Students modal ───────────────────────────────────────────────────────────

interface StudentsModalProps {
  batch: BatchResponse | null;
  centerId: string;
  onClose: () => void;
}

const TEMP_PASSWORD = 'Nexus@12345';

function StudentsModal({ batch, centerId, onClose }: StudentsModalProps) {
  const qc = useQueryClient();
  const [email, setEmail] = useState('');
  const [foundUser, setFoundUser] = useState<UserLookupResponse | null>(null);
  const [lookupError, setLookupError] = useState('');
  const [isLooking, setIsLooking] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [createForm, setCreateForm] = useState({ firstName: '', lastName: '' });

  const { data: members = [], isLoading: membersLoading } = useQuery<BatchMemberResponse[]>({
    queryKey: ['batch-members', batch?.id],
    queryFn: () =>
      api
        .get(`/api/v1/centers/${centerId}/batches/${batch!.id}/members`)
        .then((r) => (Array.isArray(r.data) ? r.data : (r.data.content ?? []))),
    enabled: !!batch && !!centerId,
  });

  const addMember = useMutation({
    mutationFn: async (user: UserLookupResponse) => {
      await api.post(`/api/v1/centers/${centerId}/batches/${batch!.id}/members`, {
        studentId: user.id,
        studentName: `${user.firstName} ${user.lastName}`,
      });
      await api.patch(`/api/v1/auth/admin/users/${user.id}/center`, { centerId });
    },
    onSuccess: (_, user) => {
      qc.invalidateQueries({ queryKey: ['batch-members', batch?.id] });
      qc.invalidateQueries({ queryKey: ['batches', centerId] });
      qc.invalidateQueries({ queryKey: ['batches-health', centerId] });
      toast.success(`${user.firstName} ${user.lastName} added to batch`);
      setFoundUser(null);
      setEmail('');
    },
    onError: (err: unknown) => {
      const msg =
        (err as { response?: { data?: { detail?: string; message?: string } } })?.response?.data?.detail ??
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Failed to add student';
      toast.error(msg);
    },
  });

  const withdrawMember = useMutation({
    mutationFn: (studentId: string) =>
      api.delete(`/api/v1/centers/${centerId}/batches/${batch!.id}/members/${studentId}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['batch-members', batch?.id] });
      qc.invalidateQueries({ queryKey: ['batches', centerId] });
      qc.invalidateQueries({ queryKey: ['batches-health', centerId] });
      toast.success('Student withdrawn from batch');
    },
    onError: () => toast.error('Failed to withdraw student'),
  });

  const createAndAdd = useMutation({
    mutationFn: async () => {
      if (!createForm.firstName.trim() || !createForm.lastName.trim() || !email.trim()) {
        throw new Error('First name, last name, and email are required');
      }
      // register-child: authenticated, no captcha, creates STUDENT role account
      const reg = await api.post<{ userId: string; email: string }>('/api/v1/auth/register-child', {
        email: email.trim(),
        password: TEMP_PASSWORD,
        firstName: createForm.firstName.trim(),
        lastName: createForm.lastName.trim(),
      });
      const userId = reg.data.userId;
      const fullName = `${createForm.firstName.trim()} ${createForm.lastName.trim()}`;
      await api.post(`/api/v1/centers/${centerId}/batches/${batch!.id}/members`, {
        studentId: userId,
        studentName: fullName,
      });
      await api.patch(`/api/v1/auth/admin/users/${userId}/center`, { centerId });
      return fullName;
    },
    onSuccess: (fullName) => {
      qc.invalidateQueries({ queryKey: ['batch-members', batch?.id] });
      qc.invalidateQueries({ queryKey: ['batches', centerId] });
      qc.invalidateQueries({ queryKey: ['batches-health', centerId] });
      toast.success(`${fullName} created and added to batch. Temp password: ${TEMP_PASSWORD}`);
      setEmail('');
      setCreateForm({ firstName: '', lastName: '' });
      setLookupError('');
      setShowCreate(false);
    },
    onError: (err: unknown) => {
      const msg =
        (err as { response?: { data?: { detail?: string; message?: string } } })?.response?.data?.detail ??
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        (err as Error).message ??
        'Failed to create student';
      toast.error(msg);
    },
  });

  async function handleLookup() {
    if (!email.trim()) return;
    setIsLooking(true);
    setLookupError('');
    setFoundUser(null);
    setShowCreate(false);
    try {
      const r = await api.get<UserLookupResponse>(
        `/api/v1/auth/users/lookup?email=${encodeURIComponent(email.trim())}`
      );
      setFoundUser(r.data);
    } catch {
      setLookupError('No user found with that email');
    } finally {
      setIsLooking(false);
    }
  }

  return (
    <Modal
      isOpen={!!batch}
      onClose={onClose}
      title={batch ? `Students — ${batch.name}` : ''}
      maxWidth="max-w-xl"
    >
      {/* Add / Create student */}
      <div className="mb-5">
        <p className="text-xs font-medium text-white/50 uppercase tracking-wider mb-2">Add Student</p>
        <div className="flex gap-2">
          <input
            type="email"
            value={email}
            onChange={(e) => { setEmail(e.target.value); setFoundUser(null); setLookupError(''); setShowCreate(false); }}
            onKeyDown={(e) => e.key === 'Enter' && handleLookup()}
            placeholder="student@email.com"
            className="input flex-1 text-sm"
          />
          <button
            onClick={handleLookup}
            disabled={isLooking || !email.trim()}
            className="btn-primary px-3 py-2 text-sm disabled:opacity-50 flex items-center gap-1.5"
          >
            {isLooking ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Search className="w-3.5 h-3.5" />}
            Find
          </button>
        </div>

        {/* Not found — offer to create */}
        {lookupError && !showCreate && (
          <div className="mt-2 flex items-center justify-between p-3 rounded-xl bg-amber-500/10 border border-amber-500/20">
            <p className="text-xs text-amber-400">{lookupError}</p>
            <button
              onClick={() => setShowCreate(true)}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-brand-500 hover:bg-brand-400 text-white text-xs font-medium transition-colors"
            >
              <UserPen className="w-3 h-3" />
              Create New
            </button>
          </div>
        )}

        {/* Inline create form */}
        <AnimatePresence>
          {showCreate && (
            <motion.div
              initial={{ opacity: 0, y: -4 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -4 }}
              className="mt-2 p-3 rounded-xl bg-brand-500/8 border border-brand-500/20 space-y-2"
            >
              <p className="text-xs font-medium text-brand-300 flex items-center gap-1.5">
                <GraduationCap className="w-3.5 h-3.5" />
                Create new student account for <span className="text-white">{email}</span>
              </p>
              <div className="grid grid-cols-2 gap-2">
                <input
                  placeholder="First name *"
                  value={createForm.firstName}
                  onChange={(e) => setCreateForm((p) => ({ ...p, firstName: e.target.value }))}
                  className="input text-sm py-2"
                />
                <input
                  placeholder="Last name *"
                  value={createForm.lastName}
                  onChange={(e) => setCreateForm((p) => ({ ...p, lastName: e.target.value }))}
                  className="input text-sm py-2"
                />
              </div>
              <p className="text-[10px] text-white/30">
                Temp password: <span className="font-mono text-white/50">{TEMP_PASSWORD}</span> — share with student to change on first login.
              </p>
              <div className="flex gap-2 pt-1">
                <button
                  onClick={() => { setShowCreate(false); setLookupError(''); }}
                  className="text-xs text-white/40 hover:text-white/70 transition-colors px-2"
                >Cancel</button>
                <button
                  onClick={() => createAndAdd.mutate()}
                  disabled={createAndAdd.isPending || !createForm.firstName.trim() || !createForm.lastName.trim()}
                  className="btn-primary text-xs py-1.5 px-4 disabled:opacity-50 flex items-center gap-1.5"
                >
                  {createAndAdd.isPending ? <Loader2 className="w-3 h-3 animate-spin" /> : <UserPlus className="w-3 h-3" />}
                  Create & Add
                </button>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {foundUser && (
          <motion.div
            initial={{ opacity: 0, y: -4 }}
            animate={{ opacity: 1, y: 0 }}
            className="mt-2 flex items-center justify-between p-3 rounded-xl bg-brand-500/10 border border-brand-500/20"
          >
            <div>
              <p className="text-sm font-medium text-white">
                {foundUser.firstName} {foundUser.lastName}
              </p>
              <p className="text-xs text-white/40">{foundUser.email} · {foundUser.role}</p>
            </div>
            <button
              onClick={() => addMember.mutate(foundUser)}
              disabled={addMember.isPending}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-brand-500 hover:bg-brand-400 text-white text-xs font-medium disabled:opacity-50 transition-colors"
            >
              {addMember.isPending ? <Loader2 className="w-3 h-3 animate-spin" /> : <UserPlus className="w-3 h-3" />}
              Add
            </button>
          </motion.div>
        )}
      </div>

      {/* Members list */}
      <div>
        <p className="text-xs font-medium text-white/50 uppercase tracking-wider mb-2">
          Enrolled Students ({members.length})
        </p>

        {membersLoading ? (
          <div className="space-y-2">
            {[0, 1, 2].map((i) => <Skeleton key={i} className="h-10" />)}
          </div>
        ) : members.length === 0 ? (
          <div className="py-8 text-center">
            <Users className="w-8 h-8 text-white/10 mx-auto mb-2" />
            <p className="text-sm text-white/30">No students enrolled yet.</p>
          </div>
        ) : (
          <div className="space-y-1.5">
            {members.map((m) => (
              <div
                key={m.id}
                className="flex items-center justify-between px-3 py-2.5 rounded-xl bg-white/3 hover:bg-white/5 transition-colors"
              >
                <div>
                  <p className="text-sm text-white font-medium">{m.studentName}</p>
                  <p className="text-xs text-white/30 font-mono">{m.studentId.substring(0, 8)}…</p>
                </div>
                <button
                  onClick={() => withdrawMember.mutate(m.studentId)}
                  disabled={withdrawMember.isPending}
                  className="p-1.5 rounded-lg text-white/30 hover:text-red-400 hover:bg-red-400/10 transition-colors disabled:opacity-50"
                  title="Withdraw from batch"
                >
                  <UserMinus className="w-3.5 h-3.5" />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </Modal>
  );
}
// ─── Main page ────────────────────────────────────────────────────────────────

export default function AdminBatchesPage() {
  const qc = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [expandedHealth, setExpandedHealth] = useState(true);
  const [studentsBatch, setStudentsBatch] = useState<BatchResponse | null>(null);
  const [selectedCenterId, setSelectedCenterId] = useState('');

  // ── Fetch centers to get centerId ──────────────────────────────────────────
  const { data: centers = [], isLoading: centersLoading } = useQuery<CenterResponse[]>({
    queryKey: ['centers'],
    queryFn: () =>
      api.get('/api/v1/centers').then((r) => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
  });

  const centerId = selectedCenterId || centers[0]?.id || '';
  const selectedCenter = centers.find((c) => c.id === centerId) ?? centers[0];

  // ── Fetch teachers ─────────────────────────────────────────────────────────
  const { data: teachers = [] } = useQuery<TeacherResponse[]>({
    queryKey: ['teachers', centerId],
    queryFn: () =>
      api.get(`/api/v1/centers/${centerId}/teachers`).then((r) => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
    enabled: !!centerId,
  });

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
    mutationFn: (data: CreateBatchRequest) => {
      const { centerId: reqCenterId, ...body } = data;
      return api.post(`/api/v1/centers/${reqCenterId || centerId}/batches`, body);
    },
    onSuccess: (_, vars) => {
      const createdUnder = vars.centerId || centerId;
      qc.invalidateQueries({ queryKey: ['batches', createdUnder] });
      qc.invalidateQueries({ queryKey: ['batches-health', createdUnder] });
      setSelectedCenterId(createdUnder);
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

  const teacherMap = new Map(teachers.map((t) => [t.userId, `${t.firstName} ${t.lastName}`]));

  const isLoading = centersLoading || batchesLoading;

  return (
    <div className="p-4 lg:p-8 space-y-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">Batches</h1>
          <p className="text-white/50 text-sm mt-0.5">
            Manage and monitor all batches{selectedCenter ? ` — ${selectedCenter.name}` : ''}.
          </p>
        </div>
        <div className="flex items-center gap-2">
          {centers.length > 1 && (
            <select
              value={centerId}
              onChange={(e) => setSelectedCenterId(e.target.value)}
              className="bg-white/5 border border-white/10 text-white text-sm rounded-lg px-3 py-2 focus:outline-none focus:ring-1 focus:ring-cyan-500/50"
            >
              {centers.map((c) => (
                <option key={c.id} value={c.id} className="bg-gray-900">
                  {c.name}
                </option>
              ))}
            </select>
          )}
          {!showForm && (
            <button
              onClick={() => setShowForm(true)}
              className="btn-primary flex items-center gap-2 px-4 py-2.5 text-sm font-medium"
            >
              <Plus className="w-4 h-4" /> Add Batch
            </button>
          )}
        </div>
      </div>

      {/* Add batch inline form */}
      <AnimatePresence>
        {showForm && (
          <AddBatchForm
            centers={centers}
            teachers={teachers}
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

      {/* Students modal */}
      <StudentsModal
        batch={studentsBatch}
        centerId={centerId}
        onClose={() => setStudentsBatch(null)}
      />

      {/* Batches table */}
      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold text-white text-sm">All Batches</h3>
          <div className="flex items-center gap-2">
            <span className="text-xs text-white/30">{batches.length} batches</span>
            <button
              onClick={() => {
                const csv = [
                  ['Name','Code','Subject','Mode','Teacher','Enrolled','Max','Status','Start','End'],
                  ...batches.map(b => [
                    b.name, b.code, b.subject, b.mode ?? 'OFFLINE',
                    teacherMap.get(b.teacherId) ?? '—',
                    b.enrolledCount, b.maxStudents, b.status,
                    b.startDate, b.endDate,
                  ])
                ].map(r => r.join(',')).join('\n');
                const a = document.createElement('a');
                a.href = 'data:text/csv;charset=utf-8,' + encodeURIComponent(csv);
                a.download = 'batches.csv';
                a.click();
              }}
              className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs font-medium text-white/50 hover:text-white hover:bg-white/8 border border-white/8 hover:border-white/15 transition-colors"
            >
              <Download className="w-3 h-3" />
              CSV
            </button>
            <button
              onClick={() => window.print()}
              className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs font-medium text-white/50 hover:text-white hover:bg-white/8 border border-white/8 hover:border-white/15 transition-colors"
            >
              <Printer className="w-3 h-3" />
              Print
            </button>
          </div>
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
                  <th className="pb-2 pr-4">Mode</th>
                  <th className="pb-2 pr-4">Teacher</th>
                  <th className="pb-2 pr-4">Enrolled</th>
                  <th className="pb-2 pr-4">Status</th>
                  <th className="pb-2 pr-4">Start</th>
                  <th className="pb-2 pr-4">End</th>
                  <th className="pb-2"></th>
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
                      className="hover:bg-black/5 dark:hover:bg-white/5 transition-colors"
                    >
                      <td className="py-3 pr-4">
                        <div className="font-medium text-white">{batch.name}</div>
                        <div className="text-xs text-white/40 mt-0.5 font-mono">{batch.code}</div>
                      </td>
                      <td className="py-3 pr-4 text-white/70">{batch.subject}</td>
                      <td className="py-3 pr-4">
                        <span className={cn(
                          'inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium',
                          (batch.mode ?? 'OFFLINE') === 'ONLINE'
                            ? 'bg-sky-500/15 text-sky-400'
                            : 'bg-amber-500/15 text-amber-400'
                        )}>
                          {(batch.mode ?? 'OFFLINE') === 'ONLINE' ? '🌐 Online' : '📍 Offline'}
                        </span>
                      </td>
                      <td className="py-3 pr-4">
                        <span className="text-xs text-white/60 truncate block max-w-[140px]" title={batch.teacherId}>
                          {teacherMap.get(batch.teacherId) ?? (batch.teacherId ? `${batch.teacherId.substring(0, 8)}…` : '—')}
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
                      <td className="py-3 pr-4">
                        <div className="flex items-center gap-1 text-xs text-white/50">
                          <Calendar className="w-3 h-3" />
                          {formatDate(batch.endDate)}
                        </div>
                      </td>
                      <td className="py-3">
                        <button
                          onClick={() => setStudentsBatch(batch)}
                          className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs font-medium text-white/50 hover:text-white hover:bg-white/8 border border-white/8 hover:border-white/15 transition-colors"
                        >
                          <Users className="w-3 h-3" />
                          Students
                        </button>
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
