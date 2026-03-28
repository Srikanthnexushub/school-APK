import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import { CreditCard, Plus, X, IndianRupee, Archive, Link2 } from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { ExportMenu } from '../../components/ui/ExportMenu';

interface FeeStructure {
  id: string; name: string; description?: string;
  amount: number; currency: string; frequency: string;
  dueDay: number; lateFeeAmount?: number; status: string;
}
interface Batch { id: string; name: string; code: string; }
interface BatchFeeAssignment {
  id: string; batchId: string; feeStructureId: string;
  effectiveFrom: string; effectiveTo?: string;
}

const FREQUENCY_LABELS: Record<string, string> = {
  MONTHLY: 'Monthly', QUARTERLY: 'Quarterly',
  ANNUAL: 'Annual', ONE_TIME: 'One-time',
};

export default function AdminFeesPage() {
  const { user } = useAuthStore();
  const qc = useQueryClient();
  const centerId = user?.centerId;
  const [activeTab, setActiveTab] = useState<'structures' | 'assignments'>('structures');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showAssignModal, setShowAssignModal] = useState(false);
  const [createForm, setCreateForm] = useState({
    name: '', description: '', amount: '',
    frequency: 'MONTHLY', dueDay: '1', lateFeeAmount: '',
  });
  const [assignForm, setAssignForm] = useState({
    batchId: '', feeStructureId: '', effectiveFrom: new Date().toISOString().slice(0, 10),
  });

  const { data: fees = [], isLoading: feesLoading } = useQuery<FeeStructure[]>({
    queryKey: ['fee-structures', centerId],
    queryFn: () => api.get(`/api/v1/centers/${centerId}/fees?size=100`).then(r =>
      Array.isArray(r.data) ? r.data : (r.data.content ?? [])),
    enabled: !!centerId,
  });

  const { data: batches = [] } = useQuery<Batch[]>({
    queryKey: ['batches', centerId],
    queryFn: () => api.get(`/api/v1/centers/${centerId}/batches`).then(r =>
      Array.isArray(r.data) ? r.data : (r.data.content ?? [])),
    enabled: !!centerId,
  });

  const { mutate: createFee, isPending: creating } = useMutation({
    mutationFn: () => api.post(`/api/v1/centers/${centerId}/fees`, {
      name: createForm.name,
      description: createForm.description || null,
      amount: parseFloat(createForm.amount),
      currency: 'INR',
      frequency: createForm.frequency,
      dueDay: parseInt(createForm.dueDay),
      lateFeeAmount: createForm.lateFeeAmount ? parseFloat(createForm.lateFeeAmount) : null,
    }),
    onSuccess: () => {
      toast.success('Fee structure created');
      setShowCreateModal(false);
      setCreateForm({ name: '', description: '', amount: '', frequency: 'MONTHLY', dueDay: '1', lateFeeAmount: '' });
      qc.invalidateQueries({ queryKey: ['fee-structures', centerId] });
    },
    onError: () => toast.error('Failed to create fee structure'),
  });

  const { mutate: assignFee, isPending: assigning } = useMutation({
    mutationFn: () => api.post(`/api/v1/centers/${centerId}/batches/${assignForm.batchId}/fees`, {
      feeStructureId: assignForm.feeStructureId,
      effectiveFrom: assignForm.effectiveFrom,
      effectiveTo: null,
    }),
    onSuccess: () => {
      toast.success('Fee assigned to batch');
      setShowAssignModal(false);
      setAssignForm({ batchId: '', feeStructureId: '', effectiveFrom: new Date().toISOString().slice(0, 10) });
    },
    onError: (err: unknown) => {
      const status = (err as { response?: { status?: number } }).response?.status;
      toast.error(status === 409 ? 'This fee is already assigned to that batch' : 'Failed to assign fee');
    },
  });

  return (
    <div className="p-6 space-y-6 max-w-5xl mx-auto">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-brand-500/10 border border-brand-500/20">
            <CreditCard className="w-5 h-5 text-brand-400" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-white">Fee Management</h1>
            <p className="text-sm text-white/50">Create fee structures and assign them to batches</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <ExportMenu
            csvData={fees.map(f => ({
              Name: f.name,
              Amount: f.amount,
              Frequency: FREQUENCY_LABELS[f.frequency] ?? f.frequency,
              DueDay: f.dueDay,
              LateFee: f.lateFeeAmount ?? '',
              Status: f.status,
            }))}
            csvFilename="fee-structures"
          />
          <button
            onClick={() => setShowCreateModal(true)}
            className="flex items-center gap-2 px-4 py-2 bg-brand-600 hover:bg-brand-500 text-white rounded-xl text-sm font-semibold transition-colors"
          >
            <Plus className="w-4 h-4" /> New Structure
          </button>
          <button
            onClick={() => setShowAssignModal(true)}
            className="flex items-center gap-2 px-4 py-2 bg-surface-100 hover:bg-white/10 text-white rounded-xl text-sm font-semibold border border-white/10 transition-colors"
          >
            <Link2 className="w-4 h-4" /> Assign to Batch
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 bg-surface-100/50 p-1 rounded-xl w-fit">
        {(['structures', 'assignments'] as const).map(tab => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`px-4 py-1.5 rounded-lg text-sm font-medium transition-colors ${
              activeTab === tab ? 'bg-brand-600 text-white' : 'text-white/50 hover:text-white'
            }`}
          >
            {tab === 'structures' ? 'Fee Structures' : 'Batch Assignments'}
          </button>
        ))}
      </div>

      {activeTab === 'structures' && (
        feesLoading ? (
          <div className="text-center py-12 text-white/40">Loading...</div>
        ) : fees.length === 0 ? (
          <div className="flex flex-col items-center gap-3 py-16 text-white/40">
            <CreditCard className="w-10 h-10" />
            <p>No fee structures yet. Create your first one above.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {fees.map((fee, i) => (
              <motion.div
                key={fee.id}
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.05 }}
                className="bg-surface-50/40 border border-white/5 rounded-xl p-5 space-y-3"
              >
                <div className="flex items-start justify-between">
                  <div>
                    <div className="text-sm font-semibold text-white">{fee.name}</div>
                    {fee.description && <div className="text-xs text-white/40 mt-0.5 line-clamp-2">{fee.description}</div>}
                  </div>
                  <span className={`text-[10px] px-2 py-0.5 rounded-full font-medium ${
                    fee.status === 'ACTIVE' ? 'bg-emerald-500/20 text-emerald-400' : 'bg-white/10 text-white/40'
                  }`}>
                    {fee.status}
                  </span>
                </div>
                <div className="flex items-baseline gap-1">
                  <IndianRupee className="w-4 h-4 text-brand-400" />
                  <span className="text-2xl font-bold text-white">{fee.amount.toLocaleString('en-IN')}</span>
                  <span className="text-xs text-white/40">/ {FREQUENCY_LABELS[fee.frequency] ?? fee.frequency}</span>
                </div>
                <div className="text-xs text-white/40">
                  Due on day {fee.dueDay} of each period
                  {fee.lateFeeAmount ? ` · Late fee: ₹${fee.lateFeeAmount}` : ''}
                </div>
              </motion.div>
            ))}
          </div>
        )
      )}

      {activeTab === 'assignments' && (
        <div className="space-y-3">
          {batches.map(batch => (
            <BatchFeeRow key={batch.id} batch={batch} centerId={centerId!} fees={fees} />
          ))}
          {batches.length === 0 && (
            <div className="text-center py-12 text-white/40">No batches found.</div>
          )}
        </div>
      )}

      {/* Create Modal */}
      <AnimatePresence>
        {showCreateModal && (
          <motion.div
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4"
            onClick={e => e.target === e.currentTarget && setShowCreateModal(false)}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 16 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 16 }}
              className="bg-surface-50 border border-white/10 rounded-2xl p-6 w-full max-w-md space-y-4"
            >
              <div className="flex items-center justify-between">
                <h2 className="text-lg font-bold text-white">New Fee Structure</h2>
                <button onClick={() => setShowCreateModal(false)} className="p-1.5 hover:bg-white/5 rounded-lg text-white/40"><X className="w-4 h-4" /></button>
              </div>
              <div className="space-y-3">
                {[
                  { key: 'name', placeholder: 'Name (e.g. Monthly Tuition) *', type: 'text' },
                  { key: 'description', placeholder: 'Description (optional)', type: 'text' },
                  { key: 'amount', placeholder: 'Amount (₹) *', type: 'number' },
                  { key: 'dueDay', placeholder: 'Due day (1-31)', type: 'number' },
                  { key: 'lateFeeAmount', placeholder: 'Late fee amount (₹, optional)', type: 'number' },
                ].map(({ key, placeholder, type }) => (
                  <input
                    key={key}
                    type={type}
                    placeholder={placeholder}
                    value={createForm[key as keyof typeof createForm]}
                    onChange={e => setCreateForm(f => ({ ...f, [key]: e.target.value }))}
                    className="w-full bg-surface-100 border border-white/10 rounded-xl px-3 py-2.5 text-white text-sm focus:outline-none focus:border-brand-500/50"
                  />
                ))}
                <select
                  value={createForm.frequency}
                  onChange={e => setCreateForm(f => ({ ...f, frequency: e.target.value }))}
                  className="w-full bg-surface-100 border border-white/10 rounded-xl px-3 py-2.5 text-white text-sm focus:outline-none"
                >
                  {Object.entries(FREQUENCY_LABELS).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
                </select>
              </div>
              <button
                onClick={() => createFee()}
                disabled={creating || !createForm.name || !createForm.amount}
                className="w-full py-2.5 bg-brand-600 hover:bg-brand-500 text-white rounded-xl text-sm font-semibold transition-colors disabled:opacity-50"
              >
                {creating ? 'Creating...' : 'Create Fee Structure'}
              </button>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Assign Modal */}
      <AnimatePresence>
        {showAssignModal && (
          <motion.div
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4"
            onClick={e => e.target === e.currentTarget && setShowAssignModal(false)}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 16 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 16 }}
              className="bg-surface-50 border border-white/10 rounded-2xl p-6 w-full max-w-md space-y-4"
            >
              <div className="flex items-center justify-between">
                <h2 className="text-lg font-bold text-white">Assign Fee to Batch</h2>
                <button onClick={() => setShowAssignModal(false)} className="p-1.5 hover:bg-white/5 rounded-lg text-white/40"><X className="w-4 h-4" /></button>
              </div>
              <div className="space-y-3">
                <div>
                  <label className="text-xs text-white/40 mb-1 block">Batch</label>
                  <select value={assignForm.batchId} onChange={e => setAssignForm(f => ({ ...f, batchId: e.target.value }))}
                    className="w-full bg-surface-100 border border-white/10 rounded-xl px-3 py-2.5 text-white text-sm focus:outline-none">
                    <option value="">Select batch...</option>
                    {batches.map(b => <option key={b.id} value={b.id}>{b.name} ({b.code})</option>)}
                  </select>
                </div>
                <div>
                  <label className="text-xs text-white/40 mb-1 block">Fee Structure</label>
                  <select value={assignForm.feeStructureId} onChange={e => setAssignForm(f => ({ ...f, feeStructureId: e.target.value }))}
                    className="w-full bg-surface-100 border border-white/10 rounded-xl px-3 py-2.5 text-white text-sm focus:outline-none">
                    <option value="">Select fee...</option>
                    {fees.filter(f => f.status === 'ACTIVE').map(f => (
                      <option key={f.id} value={f.id}>{f.name} — ₹{f.amount} ({FREQUENCY_LABELS[f.frequency]})</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-xs text-white/40 mb-1 block">Effective From</label>
                  <input type="date" value={assignForm.effectiveFrom}
                    onChange={e => setAssignForm(f => ({ ...f, effectiveFrom: e.target.value }))}
                    className="w-full bg-surface-100 border border-white/10 rounded-xl px-3 py-2.5 text-white text-sm focus:outline-none" />
                </div>
              </div>
              <button
                onClick={() => assignFee()}
                disabled={assigning || !assignForm.batchId || !assignForm.feeStructureId}
                className="w-full py-2.5 bg-brand-600 hover:bg-brand-500 text-white rounded-xl text-sm font-semibold transition-colors disabled:opacity-50"
              >
                {assigning ? 'Assigning...' : 'Assign Fee'}
              </button>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

function BatchFeeRow({ batch, centerId, fees }: { batch: Batch; centerId: string; fees: FeeStructure[] }) {
  const { data: assignments = [] } = useQuery<BatchFeeAssignment[]>({
    queryKey: ['batch-fees', centerId, batch.id],
    queryFn: () => api.get(`/api/v1/centers/${centerId}/batches/${batch.id}/fees`).then(r =>
      Array.isArray(r.data) ? r.data : (r.data.content ?? [])),
    enabled: !!centerId,
  });
  const qc = useQueryClient();
  const { mutate: remove } = useMutation({
    mutationFn: (id: string) => api.delete(`/api/v1/centers/${centerId}/batches/${batch.id}/fees/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['batch-fees', centerId, batch.id] }),
  });

  return (
    <div className="bg-surface-50/40 border border-white/5 rounded-xl p-4">
      <div className="text-sm font-semibold text-white mb-2">{batch.name} <span className="text-white/30 text-xs">({batch.code})</span></div>
      {assignments.length === 0 ? (
        <div className="text-xs text-white/30">No fees assigned</div>
      ) : (
        <div className="space-y-1">
          {assignments.map(a => {
            const fee = fees.find(f => f.id === a.feeStructureId);
            return (
              <div key={a.id} className="flex items-center justify-between text-xs">
                <span className="text-white/70">{fee?.name ?? a.feeStructureId} — ₹{fee?.amount ?? '?'} ({fee ? FREQUENCY_LABELS[fee.frequency] : ''})</span>
                <button onClick={() => remove(a.id)} className="p-1 hover:text-red-400 text-white/20 rounded transition-colors">
                  <X className="w-3 h-3" />
                </button>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
