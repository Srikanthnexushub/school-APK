// src/pages/admin/AdminPendingTeachersPage.tsx
import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Clock, User, CheckCircle2, XCircle, X, AlertCircle,
} from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { useAuthStore } from '../../stores/authStore';
import api from '../../lib/api';

// ─── Types ────────────────────────────────────────────────────────────────────

interface TeacherResponse {
  id: string;
  centerId: string;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber?: string;
  subjects?: string;
  status: string;
  joinedAt: string;
}

// ─── Reject Modal ─────────────────────────────────────────────────────────────

function RejectModal({
  teacher,
  onConfirm,
  onClose,
  isPending,
}: {
  teacher: TeacherResponse;
  onConfirm: () => void;
  onClose: () => void;
  isPending: boolean;
}) {
  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        className="bg-surface-200 border border-white/10 rounded-2xl w-full max-w-md"
      >
        <div className="flex items-center justify-between p-5 border-b border-white/8">
          <h2 className="text-base font-semibold text-white flex items-center gap-2">
            <XCircle className="w-4 h-4 text-red-400" /> Reject Teacher
          </h2>
          <button onClick={onClose} className="p-1.5 rounded-lg text-white/30 hover:text-white hover:bg-white/5">
            <X className="w-4 h-4" />
          </button>
        </div>
        <div className="p-5">
          <p className="text-sm text-white/50">
            Rejecting registration for <span className="text-white/80">{teacher.firstName} {teacher.lastName}</span>.
            They will not be able to access the platform.
          </p>
        </div>
        <div className="flex justify-end gap-3 p-5 border-t border-white/8">
          <button onClick={onClose} className="px-4 py-2 rounded-xl text-sm text-white/50 hover:text-white hover:bg-white/5 transition-colors">
            Cancel
          </button>
          <button
            disabled={isPending}
            onClick={onConfirm}
            className="flex items-center gap-2 px-4 py-2 rounded-xl text-sm bg-red-600 text-white hover:bg-red-500 disabled:opacity-40 transition-colors"
          >
            {isPending
              ? <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              : <XCircle className="w-4 h-4" />}
            Reject
          </button>
        </div>
      </motion.div>
    </div>
  );
}

// ─── Teacher Card ─────────────────────────────────────────────────────────────

function TeacherCard({
  teacher,
  onApprove,
  onReject,
  isApproving,
}: {
  teacher: TeacherResponse;
  onApprove: () => void;
  onReject: () => void;
  isApproving: boolean;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-surface-100/40 border border-white/5 rounded-xl p-5 hover:border-white/10 transition-all"
    >
      <div className="flex items-start gap-3 mb-4">
        <div className="w-10 h-10 rounded-lg bg-violet-500/15 flex items-center justify-center flex-shrink-0">
          <User className="w-5 h-5 text-violet-400" />
        </div>
        <div className="min-w-0 flex-1">
          <p className="font-semibold text-white text-sm">{teacher.firstName} {teacher.lastName}</p>
          <p className="text-xs text-white/40 truncate">{teacher.email}</p>
          {teacher.subjects && (
            <p className="text-xs text-brand-400 mt-0.5 truncate">{teacher.subjects}</p>
          )}
        </div>
        <span className="flex items-center gap-1 text-[10px] font-semibold uppercase tracking-wide text-amber-400 flex-shrink-0">
          <Clock className="w-3 h-3" /> Pending
        </span>
      </div>

      <div className="flex gap-2 pt-3 border-t border-white/5">
        <button
          onClick={onReject}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-red-400 border border-red-500/20 bg-red-500/10 hover:bg-red-500/20 transition-colors"
        >
          <XCircle className="w-3.5 h-3.5" /> Reject
        </button>
        <button
          onClick={onApprove}
          disabled={isApproving}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-emerald-400 border border-emerald-500/20 bg-emerald-500/10 hover:bg-emerald-500/20 disabled:opacity-50 transition-colors"
        >
          {isApproving
            ? <span className="w-3.5 h-3.5 border-2 border-emerald-400/30 border-t-emerald-400 rounded-full animate-spin" />
            : <CheckCircle2 className="w-3.5 h-3.5" />}
          Approve
        </button>
      </div>
    </motion.div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function AdminPendingTeachersPage() {
  const centerId = useAuthStore(s => s.user?.centerId);
  const qc = useQueryClient();
  const [rejectTarget, setRejectTarget] = useState<TeacherResponse | null>(null);
  const [approvingId, setApprovingId] = useState<string | null>(null);

  const { data: pending = [], isLoading } = useQuery<TeacherResponse[]>({
    queryKey: ['admin-pending-teachers', centerId],
    queryFn: async () => {
      const res = await api.get(`/api/v1/centers/${centerId}/teachers/pending`);
      const d = res.data;
      return Array.isArray(d) ? d : (d.content ?? []);
    },
    enabled: !!centerId,
  });

  const approveMutation = useMutation({
    mutationFn: (teacherId: string) =>
      api.post(`/api/v1/centers/${centerId}/teachers/${teacherId}/approve`),
    onMutate: (id) => setApprovingId(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-pending-teachers'] });
      toast.success('Teacher approved — they can now access the platform');
    },
    onError: () => toast.error('Failed to approve teacher'),
    onSettled: () => setApprovingId(null),
  });

  const rejectMutation = useMutation({
    mutationFn: (teacherId: string) =>
      api.post(`/api/v1/centers/${centerId}/teachers/${teacherId}/reject`, { reason: 'Rejected by coordinator' }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-pending-teachers'] });
      setRejectTarget(null);
      toast.success('Teacher registration rejected');
    },
    onError: () => toast.error('Failed to reject teacher'),
  });

  return (
    <div className="p-6 max-w-6xl mx-auto space-y-6">
      <div>
        <h1 className="text-xl font-bold text-white flex items-center gap-2">
          <Clock className="w-5 h-5 text-amber-400" /> Pending Teacher Approvals
        </h1>
        <p className="text-sm text-white/40 mt-1">Review teachers who self-registered and await your approval</p>
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center py-20 text-white/40">
          <span className="w-5 h-5 border-2 border-white/20 border-t-white/60 rounded-full animate-spin mr-3" />
          Loading pending teachers…
        </div>
      ) : pending.length === 0 ? (
        <div className="text-center py-20">
          <CheckCircle2 className="w-12 h-12 mx-auto mb-4 text-emerald-400/40" />
          <p className="text-white/40 font-medium">No pending teacher registrations</p>
          <p className="text-white/25 text-sm mt-1">All teacher requests have been reviewed.</p>
        </div>
      ) : (
        <>
          <div className="flex items-center gap-2">
            <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg border border-amber-500/20 bg-amber-500/10 text-xs font-medium text-amber-400">
              <AlertCircle className="w-3.5 h-3.5" />
              {pending.length} pending {pending.length === 1 ? 'request' : 'requests'}
            </div>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {pending.map(t => (
              <TeacherCard
                key={t.id}
                teacher={t}
                onApprove={() => approveMutation.mutate(t.id)}
                onReject={() => setRejectTarget(t)}
                isApproving={approvingId === t.id}
              />
            ))}
          </div>
        </>
      )}

      <AnimatePresence>
        {rejectTarget && (
          <RejectModal
            teacher={rejectTarget}
            onConfirm={() => rejectMutation.mutate(rejectTarget.id)}
            onClose={() => setRejectTarget(null)}
            isPending={rejectMutation.isPending}
          />
        )}
      </AnimatePresence>
    </div>
  );
}
