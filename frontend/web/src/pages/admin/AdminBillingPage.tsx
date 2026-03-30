// src/pages/admin/AdminBillingPage.tsx
import { useState } from 'react';
import { motion } from 'framer-motion';
import {
  Receipt, Users, CheckCircle2, AlertCircle, CircleDashed,
  Send, Loader2, AlertTriangle,
} from 'lucide-react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { toast } from 'sonner';
import { cn } from '../../lib/utils';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';

interface StudentFeeReportItem {
  studentId: string;
  studentName: string;
  totalPaid: number;
  paymentCount: number;
  paymentStatus: 'FULLY_PAID' | 'PARTIAL' | 'NO_PAYMENT';
}

const statusConfig = {
  FULLY_PAID:  { label: 'Fully Paid',   color: 'bg-emerald-500/15 text-emerald-400', icon: CheckCircle2 },
  PARTIAL:     { label: 'Installment',  color: 'bg-amber-500/15 text-amber-400',     icon: AlertCircle  },
  NO_PAYMENT:  { label: 'No Payment',   color: 'bg-red-500/15 text-red-400',         icon: CircleDashed },
} as const;

type Filter = 'ALL' | 'FULLY_PAID' | 'PARTIAL' | 'NO_PAYMENT';

export default function AdminBillingPage({ embedded = false }: { embedded?: boolean }) {
  const { user } = useAuthStore();
  const centerId = user?.centerId;
  const [filter, setFilter] = useState<Filter>('ALL');
  const [sending, setSending] = useState<string | null>(null);

  const { data: report = [], isLoading, isError } = useQuery<StudentFeeReportItem[]>({
    queryKey: ['billing-report', centerId],
    queryFn: () =>
      api.get(`/api/v1/parents/billing/report?centerId=${centerId}`).then((r) =>
        Array.isArray(r.data) ? r.data : []
      ),
    enabled: !!centerId,
  });

  const reminderMutation = useMutation({
    mutationFn: (studentId: string) =>
      api.post(`/api/v1/parents/billing/reminder/${studentId}?centerId=${centerId}`),
    onMutate: (studentId) => setSending(studentId),
    onSuccess: (_, studentId) => {
      const student = report.find((s) => s.studentId === studentId);
      toast.success(`Reminder sent to ${student?.studentName ?? 'parent'}'s parent.`);
      setSending(null);
    },
    onError: () => {
      toast.error('Failed to send reminder.');
      setSending(null);
    },
  });

  const totalStudents = report.length;
  const fullyPaid     = report.filter((s) => s.paymentStatus === 'FULLY_PAID').length;
  const partial       = report.filter((s) => s.paymentStatus === 'PARTIAL').length;
  const noPayment     = report.filter((s) => s.paymentStatus === 'NO_PAYMENT').length;

  const filtered = filter === 'ALL' ? report : report.filter((s) => s.paymentStatus === filter);

  const filterTabs: { key: Filter; label: string; count: number }[] = [
    { key: 'ALL',        label: 'All',         count: totalStudents },
    { key: 'FULLY_PAID', label: 'Fully Paid',  count: fullyPaid     },
    { key: 'PARTIAL',    label: 'Installment', count: partial       },
    { key: 'NO_PAYMENT', label: 'No Payment',  count: noPayment     },
  ];

  return (
    <div className={embedded ? 'space-y-6' : 'p-4 lg:p-8 space-y-6 max-w-7xl mx-auto'}>
      {!embedded && (
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-violet-500/10 border border-violet-500/20">
            <Receipt className="w-5 h-5 text-violet-400" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-white">Billing</h2>
            <p className="text-white/50 text-sm mt-0.5">Fee payment report and parent reminders.</p>
          </div>
        </div>
      )}

      {/* Summary cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {[
          { label: 'Total Students', value: totalStudents, icon: Users,        color: 'text-brand-400',   bg: 'bg-brand-500/10'   },
          { label: 'Fully Paid',     value: fullyPaid,     icon: CheckCircle2, color: 'text-emerald-400', bg: 'bg-emerald-500/10' },
          { label: 'Installment',    value: partial,       icon: AlertCircle,  color: 'text-amber-400',   bg: 'bg-amber-500/10'   },
          { label: 'No Payment',     value: noPayment,     icon: CircleDashed, color: 'text-red-400',     bg: 'bg-red-500/10'     },
        ].map(({ label, value, icon: Icon, color, bg }) => (
          <motion.div
            key={label}
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            className="card flex items-center gap-4"
          >
            <div className={cn('p-2.5 rounded-xl', bg)}>
              <Icon className={cn('w-5 h-5', color)} />
            </div>
            <div>
              <p className="text-2xl font-bold text-white">{isLoading ? '—' : value}</p>
              <p className="text-xs text-white/50">{label}</p>
            </div>
          </motion.div>
        ))}
      </div>

      {/* Filters */}
      <div className="flex items-center gap-2 flex-wrap">
        {filterTabs.map(({ key, label, count }) => (
          <button
            key={key}
            onClick={() => setFilter(key)}
            className={cn(
              'px-3 py-1.5 rounded-lg text-xs font-medium transition-colors',
              filter === key
                ? 'bg-brand-500/15 text-brand-400 border border-brand-500/30'
                : 'bg-white/5 text-white/50 hover:text-white hover:bg-white/10'
            )}
          >
            {label}
            <span className="ml-1.5 opacity-60">({count})</span>
          </button>
        ))}
      </div>

      {/* Loading */}
      {isLoading && (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="w-6 h-6 text-brand-400 animate-spin" />
        </div>
      )}

      {/* Error */}
      {isError && (
        <div className="card text-center py-12">
          <AlertTriangle className="w-10 h-10 text-red-400/50 mx-auto mb-3" />
          <p className="text-white/50 text-sm">Failed to load billing report.</p>
        </div>
      )}

      {/* Empty */}
      {!isLoading && !isError && filtered.length === 0 && (
        <div className="card text-center py-12">
          <Receipt className="w-10 h-10 text-white/20 mx-auto mb-3" />
          <p className="text-white/50 text-sm">
            {filter === 'ALL' ? 'No students linked to this center.' : `No students with status "${statusConfig[filter as Exclude<Filter,'ALL'>]?.label}".`}
          </p>
        </div>
      )}

      {/* Table */}
      {!isLoading && !isError && filtered.length > 0 && (
        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          className="card overflow-x-auto"
        >
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-xs text-white/30 uppercase tracking-wider border-b border-white/5">
                <th className="pb-2 pr-4">Student</th>
                <th className="pb-2 pr-4">Status</th>
                <th className="pb-2 pr-4">Total Paid</th>
                <th className="pb-2 pr-4">Payments</th>
                <th className="pb-2">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {filtered.map((item) => {
                const cfg = statusConfig[item.paymentStatus];
                const StatusIcon = cfg.icon;
                const isSending = sending === item.studentId;
                return (
                  <tr key={item.studentId} className="hover:bg-white/3 transition-colors">
                    <td className="py-3 pr-4">
                      <span className="font-medium text-white text-sm">{item.studentName}</span>
                    </td>
                    <td className="py-3 pr-4">
                      <span className={cn('badge text-xs flex items-center gap-1 w-fit', cfg.color)}>
                        <StatusIcon className="w-3 h-3" />
                        {cfg.label}
                      </span>
                    </td>
                    <td className="py-3 pr-4 text-white/70 text-sm">
                      {item.totalPaid > 0
                        ? `₹${item.totalPaid.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
                        : '—'}
                    </td>
                    <td className="py-3 pr-4 text-white/50 text-sm">{item.paymentCount}</td>
                    <td className="py-3">
                      {item.paymentStatus !== 'FULLY_PAID' && (
                        <button
                          onClick={() => reminderMutation.mutate(item.studentId)}
                          disabled={isSending || reminderMutation.isPending}
                          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-brand-500/10 text-brand-400 hover:bg-brand-500/20 text-xs font-medium transition-colors disabled:opacity-50"
                          title="Send reminder to parent"
                        >
                          {isSending ? (
                            <Loader2 className="w-3.5 h-3.5 animate-spin" />
                          ) : (
                            <Send className="w-3.5 h-3.5" />
                          )}
                          Send Reminder
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </motion.div>
      )}
    </div>
  );
}
