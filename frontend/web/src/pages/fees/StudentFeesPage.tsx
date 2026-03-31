import { useQuery } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import { CreditCard, IndianRupee, Calendar, AlertCircle } from 'lucide-react';
import api from '../../lib/api';
import { ExportMenu } from '../../components/ui/ExportMenu';

interface Enrollment { centerId: string; batchId: string; }
interface FeeStructure {
  id: string; name: string; amount: number; currency: string;
  frequency: string; dueDay: number; lateFeeAmount?: number; status: string;
}
interface BatchFeeAssignment {
  id: string; feeStructureId: string; effectiveFrom: string; effectiveTo?: string;
}

const FREQUENCY_LABELS: Record<string, string> = {
  MONTHLY: 'Monthly', QUARTERLY: 'Quarterly',
  ANNUAL: 'Annual', ONE_TIME: 'One-time',
};

export default function StudentFeesPage() {
  // Fetch ALL active enrollments (student may be in multiple batches/centers)
  const { data: enrollments = [] } = useQuery<Enrollment[]>({
    queryKey: ['student-enrollment-all'],
    queryFn: () =>
      api.get('/api/v1/centers/student-enrollment/me/all')
        .then(r => Array.isArray(r.data) ? r.data : [])
        .catch(() => []),
    staleTime: 5 * 60 * 1000,
    retry: false,
  });

  // Fetch batch fee assignments for ALL enrollments in parallel
  const { data: allAssignments = [], isLoading } = useQuery<BatchFeeAssignment[]>({
    queryKey: ['batch-fees-student-all', enrollments.map(e => e.batchId).join(',')],
    queryFn: async () => {
      const results = await Promise.all(
        enrollments.map(e =>
          api.get(`/api/v1/centers/${e.centerId}/batches/${e.batchId}/fees`)
            .then(r => Array.isArray(r.data) ? r.data : (r.data.content ?? []))
            .catch(() => [])
        )
      );
      return results.flat();
    },
    enabled: enrollments.length > 0,
  });

  // Fetch fee structures for each unique centerId in parallel
  const uniqueCenterIds = [...new Set(enrollments.map(e => e.centerId))];
  const { data: allFees = [] } = useQuery<FeeStructure[]>({
    queryKey: ['fee-structures-all', uniqueCenterIds.join(',')],
    queryFn: async () => {
      const results = await Promise.all(
        uniqueCenterIds.map(cId =>
          api.get(`/api/v1/centers/${cId}/fees?size=100`)
            .then(r => Array.isArray(r.data) ? r.data : (r.data.content ?? []))
            .catch(() => [])
        )
      );
      return results.flat();
    },
    enabled: uniqueCenterIds.length > 0 && allAssignments.length > 0,
  });

  const feeMap = new Map(allFees.map(f => [f.id, f]));
  const activeAssignments = allAssignments.filter(a =>
    !a.effectiveTo || new Date(a.effectiveTo) >= new Date());

  return (
    <div className="p-6 space-y-6 max-w-2xl mx-auto">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-brand-500/10 border border-brand-500/20">
            <CreditCard className="w-5 h-5 text-brand-400" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-white">My Fees</h1>
            <p className="text-sm text-white/50">Fee structures applicable to your batch</p>
          </div>
        </div>
        <ExportMenu
          csvData={activeAssignments.map(a => {
            const fee = feeMap.get(a.feeStructureId);
            return {
              Name: fee?.name ?? '',
              Amount: fee?.amount ?? '',
              Frequency: fee ? (FREQUENCY_LABELS[fee.frequency] ?? fee.frequency) : '',
              DueDay: fee?.dueDay ?? '',
              LateFee: fee?.lateFeeAmount ?? '',
              EffectiveFrom: new Date(a.effectiveFrom).toLocaleDateString(),
              EffectiveTo: a.effectiveTo ? new Date(a.effectiveTo).toLocaleDateString() : 'Ongoing',
            };
          })}
          csvFilename="my-fees"
        />
      </div>

      {isLoading ? (
        <div className="text-center py-16 text-white/40">Loading fees...</div>
      ) : activeAssignments.length === 0 ? (
        <div className="flex flex-col items-center gap-3 py-16 text-white/40">
          <CreditCard className="w-10 h-10" />
          <p>No fee structures assigned to your batch yet.</p>
          <p className="text-xs">Contact your institute for fee details.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {activeAssignments.map((a, i) => {
            const fee = feeMap.get(a.feeStructureId);
            if (!fee) return null;
            const nextDue = getNextDueDate(fee.dueDay, fee.frequency);
            const isOverdue = nextDue < new Date();

            return (
              <motion.div
                key={a.id}
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.06 }}
                className="bg-surface-50/40 border border-white/5 rounded-2xl p-6 space-y-4"
              >
                <div className="flex items-start justify-between">
                  <div>
                    <div className="text-base font-semibold text-white">{fee.name}</div>
                    <div className="text-xs text-white/40 mt-0.5">{FREQUENCY_LABELS[fee.frequency] ?? fee.frequency}</div>
                  </div>
                  <div className="flex items-baseline gap-0.5">
                    <IndianRupee className="w-4 h-4 text-brand-400 mt-0.5" />
                    <span className="text-2xl font-bold text-white">{fee.amount.toLocaleString('en-IN')}</span>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div className="bg-surface-100/50 rounded-xl p-3">
                    <div className="text-xs text-white/40 mb-1">Due Date</div>
                    <div className="flex items-center gap-1.5 text-sm font-medium text-white">
                      <Calendar className="w-3.5 h-3.5 text-white/40" />
                      Day {fee.dueDay} of each {fee.frequency === 'MONTHLY' ? 'month' : 'period'}
                    </div>
                  </div>
                  {fee.lateFeeAmount && (
                    <div className="bg-surface-100/50 rounded-xl p-3">
                      <div className="text-xs text-white/40 mb-1">Late Fee</div>
                      <div className="flex items-center gap-1 text-sm font-medium text-amber-400">
                        <IndianRupee className="w-3.5 h-3.5" />
                        {fee.lateFeeAmount.toLocaleString('en-IN')}
                      </div>
                    </div>
                  )}
                </div>

                {isOverdue && (
                  <div className="flex items-center gap-2 px-3 py-2 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 text-xs">
                    <AlertCircle className="w-3.5 h-3.5 flex-shrink-0" />
                    Payment may be overdue. Contact your parent or the institute office.
                  </div>
                )}

                <div className="text-xs text-white/30">
                  Effective from {new Date(a.effectiveFrom).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}
                  {a.effectiveTo ? ` to ${new Date(a.effectiveTo).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}` : ''}
                </div>
              </motion.div>
            );
          })}
        </div>
      )}
    </div>
  );
}

function getNextDueDate(dueDay: number, frequency: string): Date {
  const now = new Date();
  const d = new Date(now.getFullYear(), now.getMonth(), dueDay);
  if (d < now && frequency === 'MONTHLY') d.setMonth(d.getMonth() + 1);
  return d;
}
