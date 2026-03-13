// src/pages/admin/AdminPendingRegistrationsPage.tsx
import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Clock, Building2, MapPin, Phone, Mail, CheckCircle2, XCircle, X,
  AlertCircle,
} from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { cn } from '../../lib/utils';
import { toast } from 'sonner';
import api from '../../lib/api';

// ─── Types ────────────────────────────────────────────────────────────────────

interface CenterResponse {
  id: string;
  name: string;
  code: string | null;
  city?: string;
  state?: string;
  phone?: string;
  email?: string;
  status: string;
  createdAt: string;
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

function formatDate(iso: string) {
  try {
    return new Date(iso).toLocaleDateString('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric',
    });
  } catch { return iso; }
}

// ─── Reject Modal ─────────────────────────────────────────────────────────────

function RejectModal({
  center,
  onConfirm,
  onClose,
  isPending,
}: {
  center: CenterResponse;
  onConfirm: (reason: string) => void;
  onClose: () => void;
  isPending: boolean;
}) {
  const [reason, setReason] = useState('');

  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        className="bg-surface-200 border border-white/10 rounded-2xl w-full max-w-md"
      >
        <div className="flex items-center justify-between p-5 border-b border-white/8">
          <h2 className="text-base font-semibold text-white flex items-center gap-2">
            <XCircle className="w-4 h-4 text-red-400" /> Reject Registration
          </h2>
          <button onClick={onClose} className="p-1.5 rounded-lg text-white/30 hover:text-white hover:bg-white/5">
            <X className="w-4 h-4" />
          </button>
        </div>
        <div className="p-5 space-y-4">
          <p className="text-sm text-white/50">
            Rejecting: <span className="text-white/80">{center.name}</span>
          </p>
          <div>
            <label className="block text-xs text-white/50 mb-1.5">Rejection Reason *</label>
            <textarea
              value={reason}
              onChange={e => setReason(e.target.value)}
              placeholder="Explain why this registration is being rejected..."
              rows={3}
              className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-sm text-white placeholder-white/20 focus:outline-none focus:border-red-500/50 resize-none"
            />
          </div>
        </div>
        <div className="flex justify-end gap-3 p-5 border-t border-white/8">
          <button onClick={onClose}
            className="px-4 py-2 rounded-xl text-sm text-white/50 hover:text-white hover:bg-white/5 transition-colors">
            Cancel
          </button>
          <button
            disabled={!reason.trim() || isPending}
            onClick={() => onConfirm(reason.trim())}
            className="flex items-center gap-2 px-4 py-2 rounded-xl text-sm bg-red-600 text-white hover:bg-red-500 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
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

// ─── Registration Card ─────────────────────────────────────────────────────────

function RegistrationCard({
  center,
  onApprove,
  onReject,
  isApproving,
}: {
  center: CenterResponse;
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
      <div className="flex items-start justify-between gap-3 mb-4">
        <div className="flex items-center gap-3 min-w-0">
          <div className="w-10 h-10 rounded-lg bg-amber-500/15 flex items-center justify-center flex-shrink-0">
            <Building2 className="w-5 h-5 text-amber-400" />
          </div>
          <div className="min-w-0">
            <p className="font-semibold text-white text-sm leading-tight">{center.name}</p>
            <span className="flex items-center gap-1 text-[10px] font-semibold uppercase tracking-wide text-amber-400 mt-0.5">
              <Clock className="w-3 h-3" /> Pending Verification
            </span>
          </div>
        </div>
        <p className="text-[10px] text-white/30 flex-shrink-0 mt-1">
          {formatDate(center.createdAt)}
        </p>
      </div>

      <div className="space-y-1.5 mb-4">
        {center.city && (
          <div className="flex items-center gap-1.5 text-xs text-white/40">
            <MapPin className="w-3 h-3 flex-shrink-0" />
            <span>{[center.city, center.state].filter(Boolean).join(', ')}</span>
          </div>
        )}
        {center.phone && (
          <div className="flex items-center gap-1.5 text-xs text-white/40">
            <Phone className="w-3 h-3 flex-shrink-0" />
            <span>{center.phone}</span>
          </div>
        )}
        {center.email && (
          <div className="flex items-center gap-1.5 text-xs text-white/40">
            <Mail className="w-3 h-3 flex-shrink-0" />
            <span className="truncate">{center.email}</span>
          </div>
        )}
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

export default function AdminPendingRegistrationsPage() {
  const qc = useQueryClient();
  const [rejectTarget, setRejectTarget] = useState<CenterResponse | null>(null);
  const [approvingId, setApprovingId] = useState<string | null>(null);

  const { data: pending = [], isLoading } = useQuery<CenterResponse[]>({
    queryKey: ['admin-pending-registrations'],
    queryFn: async () => {
      const res = await api.get('/api/v1/centers/pending');
      const d = res.data;
      return Array.isArray(d) ? d : (d.content ?? []);
    },
  });

  const approveMutation = useMutation({
    mutationFn: (centerId: string) => api.post(`/api/v1/centers/${centerId}/approve`),
    onMutate: (centerId) => setApprovingId(centerId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-pending-registrations'] });
      qc.invalidateQueries({ queryKey: ['admin-centers'] });
      toast.success('Institution approved — code generated');
    },
    onError: () => toast.error('Failed to approve institution'),
    onSettled: () => setApprovingId(null),
  });

  const rejectMutation = useMutation({
    mutationFn: ({ centerId, reason }: { centerId: string; reason: string }) =>
      api.post(`/api/v1/centers/${centerId}/reject`, { reason }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-pending-registrations'] });
      setRejectTarget(null);
      toast.success('Institution registration rejected');
    },
    onError: () => toast.error('Failed to reject institution'),
  });

  return (
    <div className="p-6 max-w-6xl mx-auto space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-white flex items-center gap-2">
          <Clock className="w-6 h-6 text-amber-400" /> Pending Registrations
        </h1>
        <p className="text-sm text-white/40 mt-1">
          Review and approve institution registration requests
        </p>
      </div>

      {/* Content */}
      {isLoading ? (
        <div className="flex items-center justify-center py-20 text-white/40">
          <span className="w-5 h-5 border-2 border-white/20 border-t-white/60 rounded-full animate-spin mr-3" />
          Loading pending registrations…
        </div>
      ) : pending.length === 0 ? (
        <div className="text-center py-20">
          <CheckCircle2 className="w-12 h-12 mx-auto mb-4 text-emerald-400/40" />
          <p className="text-white/40 font-medium">No pending registrations</p>
          <p className="text-white/25 text-sm mt-1">All institution requests have been reviewed.</p>
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
            {pending.map(c => (
              <RegistrationCard
                key={c.id}
                center={c}
                onApprove={() => approveMutation.mutate(c.id)}
                onReject={() => setRejectTarget(c)}
                isApproving={approvingId === c.id}
              />
            ))}
          </div>
        </>
      )}

      {/* Reject modal */}
      <AnimatePresence>
        {rejectTarget && (
          <RejectModal
            center={rejectTarget}
            onConfirm={(reason) => rejectMutation.mutate({ centerId: rejectTarget.id, reason })}
            onClose={() => setRejectTarget(null)}
            isPending={rejectMutation.isPending}
          />
        )}
      </AnimatePresence>
    </div>
  );
}
