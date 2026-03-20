// src/pages/admin/AdminAssignmentsTab.tsx
import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  BookCheck, AlertTriangle, Loader2, CheckCircle2,
  XCircle, Plus, Calendar, FileText, Users,
} from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { cn } from '../../lib/utils';
import { useAuthStore } from '../../stores/authStore';
import api from '../../lib/api';
import { CreateAssignmentModal } from '../mentor-portal/MentorPortalAssignmentsPage';

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

// ─── Row ──────────────────────────────────────────────────────────────────────

function AssignmentRow({ assignment, centerId }: { assignment: AssignmentResponse; centerId: string }) {
  const qc = useQueryClient();

  const publishMutation = useMutation({
    mutationFn: () => api.patch(`/api/v1/assignments/${assignment.id}/publish`),
    onSuccess: () => {
      toast.success('Published!');
      qc.invalidateQueries({ queryKey: ['assignments-admin', centerId] });
    },
    onError: () => toast.error('Failed to publish.'),
  });

  const closeMutation = useMutation({
    mutationFn: () => api.patch(`/api/v1/assignments/${assignment.id}/close`),
    onSuccess: () => {
      toast.success('Closed!');
      qc.invalidateQueries({ queryKey: ['assignments-admin', centerId] });
    },
    onError: () => toast.error('Failed to close.'),
  });

  const dueDate = new Date(assignment.dueDate);

  return (
    <tr className="hover:bg-white/3 transition-colors group">
      <td className="py-3 pr-4">
        <div className="font-medium text-white text-sm">{assignment.title}</div>
        {assignment.description && (
          <div className="text-xs text-white/40 mt-0.5 line-clamp-1">{assignment.description}</div>
        )}
      </td>
      <td className="py-3 pr-4">
        <span className={cn('badge text-xs', typeColors[assignment.type] ?? 'bg-white/10 text-white/40')}>
          {assignment.type}
        </span>
      </td>
      <td className="py-3 pr-4 text-xs text-white/40 font-mono">{assignment.batchId.slice(0, 8)}…</td>
      <td className="py-3 pr-4">
        <span className={cn('badge text-xs', statusColors[assignment.status] ?? 'bg-white/10 text-white/40')}>
          {assignment.status}
        </span>
      </td>
      <td className="py-3 pr-4 text-xs text-white/40">
        {dueDate.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}
      </td>
      <td className="py-3 pr-4 text-xs text-white/60">
        {assignment.submissionCount ?? 0}
      </td>
      <td className="py-3">
        <div className="flex items-center gap-1.5">
          {assignment.status === 'DRAFT' && (
            <button
              onClick={() => publishMutation.mutate()}
              disabled={publishMutation.isPending}
              className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-emerald-500/15 text-emerald-400 hover:bg-emerald-500/25 transition-colors text-xs font-medium disabled:opacity-50"
            >
              {publishMutation.isPending ? <Loader2 className="w-3 h-3 animate-spin" /> : <CheckCircle2 className="w-3 h-3" />}
              Publish
            </button>
          )}
          {assignment.status === 'PUBLISHED' && (
            <button
              onClick={() => closeMutation.mutate()}
              disabled={closeMutation.isPending}
              className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-white/5 text-white/50 hover:bg-white/10 transition-colors text-xs font-medium disabled:opacity-50"
            >
              {closeMutation.isPending ? <Loader2 className="w-3 h-3 animate-spin" /> : <XCircle className="w-3 h-3" />}
              Close
            </button>
          )}
        </div>
      </td>
    </tr>
  );
}

// ─── Main tab ─────────────────────────────────────────────────────────────────

export default function AdminAssignmentsTab() {
  const role = useAuthStore((s) => s.user?.role);
  const centerId = useAuthStore((s) => s.user?.centerId);
  const isSuperAdmin = role === 'SUPER_ADMIN';

  const [manualCenterId, setManualCenterId] = useState('');
  const [showCreate, setShowCreate] = useState(false);

  const effectiveCenterId = isSuperAdmin ? manualCenterId : (centerId ?? '');

  const { data: assignments = [], isLoading, isError } = useQuery<AssignmentResponse[]>({
    queryKey: ['assignments-admin', effectiveCenterId],
    queryFn: () =>
      api.get(`/api/v1/assignments?centerId=${effectiveCenterId}`).then((r) => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
    enabled: !!effectiveCenterId,
  });

  return (
    <div className="p-4 lg:p-8 space-y-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-white">Assignments</h2>
          <p className="text-white/50 text-sm mt-0.5">
            {isSuperAdmin ? 'View assignments by center.' : 'Manage assignments for your center.'}
          </p>
        </div>
        {effectiveCenterId && (
          <button
            onClick={() => setShowCreate(true)}
            className="btn-primary flex items-center gap-2 px-4 py-2.5 text-sm font-medium"
          >
            <Plus className="w-4 h-4" />
            Create Assignment
          </button>
        )}
      </div>

      {/* Super admin center selector */}
      {isSuperAdmin && (
        <div className="card">
          <label className="block text-xs font-medium text-white/60 mb-1.5">Enter Center ID to view assignments</label>
          <div className="flex gap-3">
            <input
              className="input flex-1"
              placeholder="Center UUID…"
              value={manualCenterId}
              onChange={(e) => setManualCenterId(e.target.value)}
            />
          </div>
          {!manualCenterId && (
            <p className="text-xs text-white/30 mt-2">Enter a center ID above to load assignments.</p>
          )}
        </div>
      )}

      {/* No center */}
      {!effectiveCenterId && !isSuperAdmin && (
        <div className="card text-center py-12">
          <BookCheck className="w-10 h-10 text-white/20 mx-auto mb-3" />
          <p className="text-white/50 text-sm">No center linked to your account.</p>
        </div>
      )}

      {/* Loading */}
      {effectiveCenterId && isLoading && (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="w-6 h-6 text-brand-400 animate-spin" />
        </div>
      )}

      {/* Error */}
      {effectiveCenterId && isError && (
        <div className="card text-center py-12">
          <AlertTriangle className="w-10 h-10 text-red-400/50 mx-auto mb-3" />
          <p className="text-white/50 text-sm">Failed to load assignments.</p>
        </div>
      )}

      {/* Table */}
      {effectiveCenterId && !isLoading && !isError && (
        <>
          {assignments.length === 0 ? (
            <div className="card text-center py-12">
              <BookCheck className="w-10 h-10 text-white/20 mx-auto mb-3" />
              <p className="text-white/50 text-sm">No assignments found.</p>
            </div>
          ) : (
            <motion.div
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              className="card overflow-x-auto"
            >
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-xs text-white/30 uppercase tracking-wider border-b border-white/5">
                    <th className="pb-2 pr-4">Title</th>
                    <th className="pb-2 pr-4">Type</th>
                    <th className="pb-2 pr-4">
                      <span className="flex items-center gap-1"><Users className="w-3 h-3" /> Batch</span>
                    </th>
                    <th className="pb-2 pr-4">Status</th>
                    <th className="pb-2 pr-4">
                      <span className="flex items-center gap-1"><Calendar className="w-3 h-3" /> Due</span>
                    </th>
                    <th className="pb-2 pr-4">
                      <span className="flex items-center gap-1"><FileText className="w-3 h-3" /> Subs</span>
                    </th>
                    <th className="pb-2">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5">
                  {assignments.map((a) => (
                    <AssignmentRow key={a.id} assignment={a} centerId={effectiveCenterId} />
                  ))}
                </tbody>
              </table>
            </motion.div>
          )}
        </>
      )}

      {/* Create modal */}
      <AnimatePresence>
        {showCreate && effectiveCenterId && (
          <CreateAssignmentModal
            centerId={effectiveCenterId}
            onClose={() => setShowCreate(false)}
          />
        )}
      </AnimatePresence>
    </div>
  );
}
