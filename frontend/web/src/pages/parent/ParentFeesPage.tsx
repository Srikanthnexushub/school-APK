import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import {
  IndianRupee, Plus, X, Loader2, CheckCircle2, AlertTriangle,
  CreditCard, Clock, TrendingUp, Calendar,
} from 'lucide-react';
import { ExportMenu } from '../../components/ui/ExportMenu';
import { cn } from '../../lib/utils';
import { toast } from 'sonner';
import api from '../../lib/api';

// ─── Types ────────────────────────────────────────────────────────────────────

interface ParentProfileResponse {
  id: string;
  name: string;
}

interface StudentLinkResponse {
  id: string;
  studentId: string;
  studentName: string;
  centerId: string;
  status: string;
}

interface FeePaymentResponse {
  id: string;
  parentId: string;
  studentId: string | null;
  centerId: string | null;
  batchId?: string;
  feeType?: string;
  amountPaid: number;
  currency: string;
  paymentDate: string;
  referenceNumber: string;
  paymentMethod?: string;
  remarks: string;
  status: string;
  createdAt: string;
}

interface FeeStructure {
  id: string; name: string; amount: number; currency: string;
  frequency: string; dueDay: number; lateFeeAmount?: number;
}
interface BatchFeeAssignmentResp {
  id: string; feeStructureId: string; effectiveFrom: string; effectiveTo?: string;
}

const FREQUENCY_LABELS: Record<string, string> = {
  MONTHLY: 'Monthly', QUARTERLY: 'Quarterly', ANNUAL: 'Annual', ONE_TIME: 'One-time',
};

interface RecordPaymentRequest {
  studentId: string;
  centerId?: string;
  feeType: string;
  amountPaid: number;
  currency: string;
  paymentMethod: string;
  paymentDate: string;
  referenceNumber: string;
  remarks: string;
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

const feeStatusColors: Record<string, string> = {
  CONFIRMED: 'bg-emerald-500/15 text-emerald-400',
  PAID:      'bg-emerald-500/15 text-emerald-400',
  PENDING:   'bg-amber-500/15 text-amber-400',
  DISPUTED:  'bg-red-500/15 text-red-400',
  REFUNDED:  'bg-white/5 text-white/40',
};

// ─── Summary card ─────────────────────────────────────────────────────────────

function SummaryCard({
  label, value, sub, icon: Icon, color,
}: {
  label: string;
  value: string | number;
  sub: string;
  icon: React.ElementType;
  color: string;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      className="card"
    >
      <div className="flex items-start justify-between mb-3">
        <div className={cn('p-2 rounded-lg', color)}>
          <Icon className="w-4 h-4" />
        </div>
      </div>
      <div className="text-xs font-semibold text-white/40 uppercase tracking-wider mb-1">{label}</div>
      <div className="text-2xl font-bold text-white">{value}</div>
      <div className="text-xs text-white/30 mt-1">{sub}</div>
    </motion.div>
  );
}

// ─── Record Payment Modal ─────────────────────────────────────────────────────

interface RecordPaymentModalProps {
  profileId: string;
  students: StudentLinkResponse[];
  onClose: () => void;
  onRecorded: () => void;
}

function RecordPaymentModal({ profileId, students, onClose, onRecorded }: RecordPaymentModalProps) {
  const [form, setForm] = useState<RecordPaymentRequest>({
    studentId: students[0]?.studentId ?? '',
    centerId: students[0]?.centerId,
    feeType: 'TUITION',
    amountPaid: 0,
    currency: 'INR',
    paymentMethod: 'CASH',
    paymentDate: new Date().toISOString().split('T')[0],
    referenceNumber: '',
    remarks: '',
  });

  const mutation = useMutation({
    mutationFn: (data: RecordPaymentRequest) =>
      api.post(`/api/v1/parents/${profileId}/payments`, data),
    onSuccess: () => {
      toast.success('Payment recorded successfully.');
      onRecorded();
      onClose();
    },
    onError: (e: unknown) => {
      const err = e as { response?: { data?: { message?: string } }; message?: string };
      toast.error(err?.response?.data?.message ?? err?.message ?? 'Failed to record payment.');
    },
  });

  function setField<K extends keyof RecordPaymentRequest>(key: K, value: RecordPaymentRequest[K]) {
    setForm((p) => ({ ...p, [key]: value }));
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        exit={{ opacity: 0, scale: 0.95 }}
        className="bg-surface-100 border border-white/10 rounded-2xl p-6 w-full max-w-lg shadow-2xl max-h-[90vh] overflow-y-auto"
      >
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-lg font-bold text-white">Record Payment</h2>
          <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-white/5 text-white/40 hover:text-white/70 transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={(e) => { e.preventDefault(); mutation.mutate(form); }} className="space-y-4">
          {/* Child selector */}
          <div>
            <label className="block text-xs font-medium text-white/60 mb-1.5">Child</label>
            <select
              value={form.studentId}
              onChange={(e) => {
                const selected = students.find((s) => s.studentId === e.target.value);
                setForm((p) => ({ ...p, studentId: e.target.value, centerId: selected?.centerId }));
              }}
              className="input w-full"
              required
            >
              <option value="">— Select child —</option>
              {students.map((s) => (
                <option key={s.studentId} value={s.studentId}>{s.studentName}</option>
              ))}
            </select>
          </div>

          {/* Fee type */}
          <div>
            <label className="block text-xs font-medium text-white/60 mb-2">Fee Type</label>
            <div className="flex gap-2 flex-wrap">
              {(['TUITION', 'VAN', 'MISCELLANEOUS'] as const).map((ft) => (
                <button
                  key={ft}
                  type="button"
                  onClick={() => setField('feeType', ft)}
                  className={cn(
                    'px-3 py-1.5 rounded-lg text-sm font-medium border transition-colors',
                    form.feeType === ft
                      ? 'border-brand-500 bg-brand-500/15 text-brand-400'
                      : 'border-white/10 text-white/40 hover:border-white/20 hover:text-white/60'
                  )}
                >
                  {ft === 'TUITION' ? 'School Fees' : ft === 'VAN' ? 'Van / Transport' : 'Miscellaneous'}
                </button>
              ))}
            </div>
          </div>

          {/* Amount */}
          <div>
            <label className="block text-xs font-medium text-white/60 mb-1.5">Amount (INR)</label>
            <div className="relative">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-white/40 text-sm">₹</span>
              <input
                type="number"
                min={1}
                step={1}
                value={form.amountPaid || ''}
                onChange={(e) => setField('amountPaid', Number(e.target.value))}
                placeholder="5000"
                className="input w-full pl-7"
                required
              />
            </div>
          </div>

          {/* Payment method */}
          <div>
            <label className="block text-xs font-medium text-white/60 mb-2">Payment Method</label>
            <div className="flex gap-2 flex-wrap">
              {(['CASH', 'ONLINE', 'CHEQUE', 'UPI'] as const).map((pm) => (
                <button
                  key={pm}
                  type="button"
                  onClick={() => setField('paymentMethod', pm)}
                  className={cn(
                    'px-3 py-1.5 rounded-lg text-sm font-medium border transition-colors',
                    form.paymentMethod === pm
                      ? 'border-brand-500 bg-brand-500/15 text-brand-400'
                      : 'border-white/10 text-white/40 hover:border-white/20 hover:text-white/60'
                  )}
                >
                  {pm}
                </button>
              ))}
            </div>
          </div>

          {/* Payment date */}
          <div>
            <label className="block text-xs font-medium text-white/60 mb-1.5">Payment Date</label>
            <input
              type="date"
              value={form.paymentDate}
              onChange={(e) => setField('paymentDate', e.target.value)}
              className="input w-full"
              required
            />
          </div>

          {/* Reference number */}
          <div>
            <label className="block text-xs font-medium text-white/60 mb-1.5">Reference Number</label>
            <input
              value={form.referenceNumber}
              onChange={(e) => setField('referenceNumber', e.target.value)}
              placeholder="TXN123456"
              className="input w-full"
            />
          </div>

          {/* Remarks */}
          <div>
            <label className="block text-xs font-medium text-white/60 mb-1.5">Remarks (optional)</label>
            <input
              value={form.remarks}
              onChange={(e) => setField('remarks', e.target.value)}
              placeholder="e.g. March term fees"
              className="input w-full"
            />
          </div>

          <div className="flex gap-3 pt-2">
            <button type="button" onClick={onClose} className="flex-1 px-4 py-2.5 rounded-xl border border-white/10 text-sm text-white/60 hover:text-white hover:border-white/20 transition-colors">
              Cancel
            </button>
            <button
              type="submit"
              disabled={mutation.isPending || !form.studentId || !form.amountPaid}
              className="flex-1 btn-primary py-2.5 text-sm flex items-center justify-center gap-2 disabled:opacity-50"
            >
              {mutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
              {mutation.isPending ? 'Recording…' : 'Record Payment'}
            </button>
          </div>
        </form>
      </motion.div>
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────

export default function ParentFeesPage() {
  const queryClient = useQueryClient();
  const [selectedStudentId, setSelectedStudentId] = useState<string | 'all'>('all');
  const [showModal, setShowModal] = useState(false);

  const { data: profile } = useQuery<ParentProfileResponse>({
    queryKey: ['parent-profile'],
    queryFn: () => api.get('/api/v1/parents/me').then((r) => r.data),
  });

  const { data: linkedStudents = [] } = useQuery<StudentLinkResponse[]>({
    queryKey: ['linked-students', profile?.id],
    queryFn: () =>
      api.get(`/api/v1/parents/${profile!.id}/students?size=500`).then((r) => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
    enabled: !!profile?.id,
  });

  const { data: allPayments = [], isLoading: paymentsLoading } = useQuery<FeePaymentResponse[]>({
    queryKey: ['fee-payments', profile?.id],
    queryFn: () =>
      api.get(`/api/v1/parents/${profile!.id}/payments?size=500`).then((r) => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
    enabled: !!profile?.id,
  });

  // Filter payments by selected student
  const payments = selectedStudentId === 'all'
    ? allPayments
    : allPayments.filter((p) => p.studentId === selectedStudentId);

  // Summary stats
  const totalPaid = payments
    .filter((p) => ['CONFIRMED', 'PAID'].includes(p.status?.toUpperCase()))
    .reduce((sum, p) => sum + p.amountPaid, 0);

  const pendingCount = payments.filter((p) => p.status?.toUpperCase() === 'PENDING').length;

  const sorted = [...payments].sort(
    (a, b) => new Date(b.paymentDate || b.createdAt).getTime() - new Date(a.paymentDate || a.createdAt).getTime()
  );
  const lastPayment = sorted[0];

  const tuitionTotal = payments
    .filter((p) => p.feeType === 'TUITION' && ['CONFIRMED', 'PAID'].includes(p.status?.toUpperCase()))
    .reduce((sum, p) => sum + p.amountPaid, 0);

  const vanTotal = payments
    .filter((p) => p.feeType === 'VAN' && ['CONFIRMED', 'PAID'].includes(p.status?.toUpperCase()))
    .reduce((sum, p) => sum + p.amountPaid, 0);

  const activeStudents = linkedStudents.filter((s) => s.status === 'ACTIVE' || s.status === 'active');

  // ── Fee structures for each child ──────────────────────────────────────────
  const uniqueCenterIds = [...new Set(
    linkedStudents
      .filter(s => s.centerId && s.centerId !== '00000000-0000-0000-0000-000000000000')
      .map(s => s.centerId),
  )];

  const { data: centerBatches = [] } = useQuery<{ id: string; centerId: string }[]>({
    queryKey: ['parent-center-batches-fees', uniqueCenterIds.join(',')],
    queryFn: async () => {
      const results = await Promise.all(
        uniqueCenterIds.map(cId =>
          api.get(`/api/v1/centers/${cId}/batches`).then(r => {
            const list = Array.isArray(r.data) ? r.data : (r.data.content ?? []);
            return list.map((b: { id: string }) => ({ id: b.id, centerId: cId }));
          }).catch(() => [])
        )
      );
      return results.flat();
    },
    enabled: uniqueCenterIds.length > 0,
    staleTime: 5 * 60_000,
  });

  const { data: batchFeeAssignments = [] } = useQuery<(BatchFeeAssignmentResp & { centerId: string })[]>({
    queryKey: ['parent-batch-fee-assignments', centerBatches.map(b => b.id).join(',')],
    queryFn: async () => {
      const results = await Promise.all(
        centerBatches.map(b =>
          api.get(`/api/v1/centers/${b.centerId}/batches/${b.id}/fees`).then(r => {
            const list = Array.isArray(r.data) ? r.data : (r.data.content ?? []);
            return list.map((a: BatchFeeAssignmentResp) => ({ ...a, centerId: b.centerId }));
          }).catch(() => [])
        )
      );
      return results.flat();
    },
    enabled: centerBatches.length > 0,
  });

  const { data: feeStructures = [] } = useQuery<FeeStructure[]>({
    queryKey: ['parent-fee-structures', uniqueCenterIds.join(',')],
    queryFn: async () => {
      const results = await Promise.all(
        uniqueCenterIds.map(cId =>
          api.get(`/api/v1/centers/${cId}/fees?size=100`).then(r =>
            Array.isArray(r.data) ? r.data : (r.data.content ?? [])
          ).catch(() => [])
        )
      );
      return results.flat();
    },
    enabled: uniqueCenterIds.length > 0,
  });

  const feeMap = new Map(feeStructures.map(f => [f.id, f]));
  const activeFeeAssignments = [...new Map(
    batchFeeAssignments
      .filter(a => !a.effectiveTo || new Date(a.effectiveTo) >= new Date())
      .map(a => [a.feeStructureId, a]),
  ).values()];

  return (
    <div className="p-4 lg:p-8 max-w-6xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">Fee Management</h1>
          <p className="text-white/50 text-sm mt-0.5">Track school and transport fees for all children.</p>
        </div>
        <div className="flex items-center gap-2">
          <ExportMenu
            csvData={sorted.map(p => ({
              Student: linkedStudents.find(s => s.studentId === p.studentId)?.studentName ?? p.studentId,
              FeeType: p.feeType ?? '',
              Amount: p.amountPaid,
              Currency: p.currency,
              Status: p.status,
              Method: p.paymentMethod ?? '',
              Date: formatDate(p.paymentDate || p.createdAt),
              Reference: p.referenceNumber,
            }))}
            csvFilename="fee-payments"
          />
          {profile && activeStudents.length > 0 && (
            <button
              onClick={() => setShowModal(true)}
              className="btn-primary flex items-center gap-2 px-4 py-2.5 text-sm font-medium"
            >
              <Plus className="w-4 h-4" />
              Record Payment
            </button>
          )}
        </div>
      </div>

      {/* Child selector tabs */}
      {linkedStudents.length > 0 && (
        <div className="flex gap-2 overflow-x-auto pb-1">
          <button
            onClick={() => setSelectedStudentId('all')}
            className={cn(
              'px-4 py-2 rounded-xl text-sm font-medium border transition-all flex-shrink-0',
              selectedStudentId === 'all'
                ? 'border-brand-500 bg-brand-500/10 text-white'
                : 'border-white/5 text-white/40 hover:border-white/10 hover:text-white/60'
            )}
          >
            All Children
          </button>
          {linkedStudents.map((s) => (
            <button
              key={s.studentId}
              onClick={() => setSelectedStudentId(s.studentId)}
              className={cn(
                'px-4 py-2 rounded-xl text-sm font-medium border transition-all flex-shrink-0',
                selectedStudentId === s.studentId
                  ? 'border-brand-500 bg-brand-500/10 text-white'
                  : 'border-white/5 text-white/40 hover:border-white/10 hover:text-white/60'
              )}
            >
              {s.studentName}
            </button>
          ))}
        </div>
      )}

      {/* Summary cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <SummaryCard
          label="Total Paid"
          value={`₹${totalPaid.toLocaleString('en-IN')}`}
          sub="Confirmed payments"
          icon={CheckCircle2}
          color="bg-emerald-500/10 text-emerald-400"
        />
        <SummaryCard
          label="Pending"
          value={pendingCount}
          sub="Awaiting confirmation"
          icon={Clock}
          color="bg-amber-500/10 text-amber-400"
        />
        <SummaryCard
          label="School Fees"
          value={`₹${tuitionTotal.toLocaleString('en-IN')}`}
          sub="TUITION type total"
          icon={IndianRupee}
          color="bg-brand-500/10 text-brand-400"
        />
        <SummaryCard
          label="Van / Transport"
          value={`₹${vanTotal.toLocaleString('en-IN')}`}
          sub="VAN type total"
          icon={TrendingUp}
          color="bg-violet-500/10 text-violet-400"
        />
      </div>

      {/* Fee structures */}
      {activeFeeAssignments.length > 0 && (
        <div className="space-y-3">
          <h3 className="text-sm font-semibold text-white/60 uppercase tracking-wider">Fee Structures</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {activeFeeAssignments.map(a => {
              const fee = feeMap.get(a.feeStructureId);
              if (!fee) return null;
              return (
                <motion.div
                  key={a.id}
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="bg-surface-50/40 border border-white/5 rounded-2xl p-5 space-y-3"
                >
                  <div className="flex items-start justify-between">
                    <div>
                      <div className="text-sm font-semibold text-white">{fee.name}</div>
                      <div className="text-xs text-white/40 mt-0.5">{FREQUENCY_LABELS[fee.frequency] ?? fee.frequency}</div>
                    </div>
                    <div className="flex items-baseline gap-0.5">
                      <IndianRupee className="w-3.5 h-3.5 text-brand-400 mt-0.5" />
                      <span className="text-xl font-bold text-white">{fee.amount.toLocaleString('en-IN')}</span>
                    </div>
                  </div>
                  <div className="flex items-center gap-1.5 text-xs text-white/50">
                    <Calendar className="w-3 h-3" />
                    Day {fee.dueDay} of each {fee.frequency === 'MONTHLY' ? 'month' : 'period'}
                  </div>
                  <div className="text-xs text-white/30">
                    Effective from {new Date(a.effectiveFrom).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}
                    {a.effectiveTo ? ` to ${new Date(a.effectiveTo).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}` : ''}
                  </div>
                </motion.div>
              );
            })}
          </div>
        </div>
      )}

      {/* Last payment info */}
      {lastPayment && (
        <div className="card border border-white/5 flex items-center gap-4 py-3">
          <div className="p-2 rounded-lg bg-brand-500/10 flex-shrink-0">
            <CreditCard className="w-4 h-4 text-brand-400" />
          </div>
          <div>
            <span className="text-xs text-white/40">Last payment</span>
            <span className="text-sm text-white font-medium ml-2">
              ₹{lastPayment.amountPaid.toLocaleString('en-IN')} on {formatDate(lastPayment.paymentDate || lastPayment.createdAt)}
            </span>
            {lastPayment.referenceNumber && (
              <span className="text-xs text-white/30 ml-2">Ref: {lastPayment.referenceNumber}</span>
            )}
          </div>
          <span className={cn('ml-auto text-xs px-2 py-0.5 rounded-full font-medium', feeStatusColors[lastPayment.status?.toUpperCase()] ?? 'bg-white/5 text-white/40')}>
            {lastPayment.status}
          </span>
        </div>
      )}

      {/* Payments table */}
      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold text-white text-sm">Payment History</h3>
          <span className="text-xs text-white/30">{payments.length} records</span>
        </div>

        {paymentsLoading ? (
          <div className="space-y-3">
            {[0, 1, 2, 3].map((i) => (
              <div key={i} className="h-10 bg-white/5 rounded-lg animate-pulse" />
            ))}
          </div>
        ) : payments.length === 0 ? (
          <div className="text-center py-12">
            <IndianRupee className="w-10 h-10 text-white/15 mx-auto mb-3" />
            <p className="text-white/30 text-sm">No payment records found.</p>
            {linkedStudents.length === 0 && (
              <p className="text-white/20 text-xs mt-1">Link a child first to track fees.</p>
            )}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs text-white/30 uppercase tracking-wider border-b border-white/5">
                  <th className="pb-2 pr-4">Date</th>
                  <th className="pb-2 pr-4">Child</th>
                  <th className="pb-2 pr-4">Fee Type</th>
                  <th className="pb-2 pr-4">Amount</th>
                  <th className="pb-2 pr-4">Reference</th>
                  <th className="pb-2 pr-4">Method</th>
                  <th className="pb-2">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {sorted.map((p) => {
                  const student = linkedStudents.find((s) => s.studentId === p.studentId);
                  return (
                    <tr key={p.id} className="hover:bg-white/[0.02] transition-colors">
                      <td className="py-3 pr-4 text-xs text-white/40 whitespace-nowrap">
                        {formatDate(p.paymentDate || p.createdAt)}
                      </td>
                      <td className="py-3 pr-4 text-white/70 whitespace-nowrap">
                        {student?.studentName ?? (p.studentId ? p.studentId.slice(0, 8) + '…' : '—')}
                      </td>
                      <td className="py-3 pr-4">
                        <span className="text-xs bg-white/5 text-white/50 px-2 py-0.5 rounded-lg">
                          {p.feeType ?? '—'}
                        </span>
                      </td>
                      <td className="py-3 pr-4 font-medium text-white whitespace-nowrap">
                        ₹{p.amountPaid.toLocaleString('en-IN')}
                      </td>
                      <td className="py-3 pr-4 text-white/40 text-xs font-mono">
                        {p.referenceNumber || '—'}
                      </td>
                      <td className="py-3 pr-4 text-white/40 text-xs">
                        {p.paymentMethod ?? '—'}
                      </td>
                      <td className="py-3">
                        <span className={cn(
                          'inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium',
                          feeStatusColors[p.status?.toUpperCase()] ?? 'bg-white/5 text-white/40'
                        )}>
                          {p.status?.toUpperCase() === 'CONFIRMED' || p.status?.toUpperCase() === 'PAID'
                            ? <CheckCircle2 className="w-3 h-3" />
                            : p.status?.toUpperCase() === 'PENDING'
                              ? <Clock className="w-3 h-3" />
                              : <AlertTriangle className="w-3 h-3" />}
                          {p.status}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Record Payment Modal */}
      <AnimatePresence>
        {showModal && profile && (
          <RecordPaymentModal
            profileId={profile.id}
            students={activeStudents}
            onClose={() => setShowModal(false)}
            onRecorded={() => queryClient.invalidateQueries({ queryKey: ['fee-payments'] })}
          />
        )}
      </AnimatePresence>
    </div>
  );
}
