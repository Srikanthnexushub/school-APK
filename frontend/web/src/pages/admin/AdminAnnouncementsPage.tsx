import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import { Megaphone, Plus, X, Send, Trash2, Clock, Users, CheckCircle } from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';

interface Batch { id: string; name: string; code: string; }
interface Center { id: string; name: string; }
interface Announcement {
  id: string; centerId: string; title: string; body: string;
  targetType: string; targetId?: string; targetRole?: string;
  sentAt?: string; createdAt: string; sentById: string;
}
type TargetType = 'BATCH' | 'CENTER' | 'ROLE' | 'ALL';
type TargetRole = 'STUDENT' | 'PARENT' | 'TEACHER' | 'ALL';

export default function AdminAnnouncementsPage() {
  const { user } = useAuthStore();
  const qc = useQueryClient();
  const centerId = user?.centerId;
  const [showCompose, setShowCompose] = useState(false);
  const [form, setForm] = useState({
    title: '', body: '',
    targetType: 'CENTER' as TargetType,
    targetId: '',
    targetRole: 'ALL' as TargetRole,
    scheduledAt: '',
  });

  const { data: batches = [] } = useQuery<Batch[]>({
    queryKey: ['batches', centerId],
    queryFn: () => api.get(`/api/v1/centers/${centerId}/batches`).then(r =>
      Array.isArray(r.data) ? r.data : (r.data.content ?? [])),
    enabled: !!centerId,
  });

  const { data: announcements = [], isLoading } = useQuery<Announcement[]>({
    queryKey: ['admin-announcements', centerId],
    queryFn: () => api.get(`/api/v1/centers/${centerId}/announcements`).then(r =>
      Array.isArray(r.data) ? r.data : (r.data.content ?? [])),
    enabled: !!centerId,
  });

  const { mutate: send, isPending } = useMutation({
    mutationFn: () => api.post(`/api/v1/centers/${centerId}/announcements`, {
      centerId,
      title: form.title,
      body: form.body,
      targetType: form.targetType,
      targetId: form.targetType === 'BATCH' ? form.targetId || null : null,
      targetRole: form.targetType === 'ROLE' ? form.targetRole : null,
      scheduledAt: form.scheduledAt ? new Date(form.scheduledAt).toISOString() : null,
      expiresAt: null,
    }),
    onSuccess: () => {
      toast.success('Announcement sent!');
      setShowCompose(false);
      setForm({ title: '', body: '', targetType: 'CENTER', targetId: '', targetRole: 'ALL', scheduledAt: '' });
      qc.invalidateQueries({ queryKey: ['admin-announcements', centerId] });
    },
    onError: () => toast.error('Failed to send announcement'),
  });

  const { mutate: expire } = useMutation({
    mutationFn: (id: string) => api.delete(`/api/v1/centers/${centerId}/announcements/${id}`),
    onSuccess: () => {
      toast.success('Announcement removed');
      qc.invalidateQueries({ queryKey: ['admin-announcements', centerId] });
    },
  });

  return (
    <div className="p-6 space-y-6 max-w-4xl mx-auto">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-brand-500/10 border border-brand-500/20">
            <Megaphone className="w-5 h-5 text-brand-400" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-white">Announcements</h1>
            <p className="text-sm text-white/50">Broadcast messages to students, parents, and staff</p>
          </div>
        </div>
        <button
          onClick={() => setShowCompose(true)}
          className="flex items-center gap-2 px-4 py-2 bg-brand-600 hover:bg-brand-500 text-white rounded-xl text-sm font-semibold transition-colors"
        >
          <Plus className="w-4 h-4" /> New Announcement
        </button>
      </div>

      {/* Stats row */}
      <div className="grid grid-cols-3 gap-4">
        {[
          { label: 'Total Sent', value: announcements.filter(a => a.sentAt).length, color: 'text-emerald-400' },
          { label: 'Scheduled', value: announcements.filter(a => !a.sentAt).length, color: 'text-amber-400' },
          { label: 'This Month', value: announcements.filter(a => {
            const d = new Date(a.createdAt);
            const now = new Date();
            return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear();
          }).length, color: 'text-brand-400' },
        ].map(s => (
          <div key={s.label} className="bg-surface-50/40 border border-white/5 rounded-xl p-4 text-center">
            <div className={`text-2xl font-bold ${s.color}`}>{s.value}</div>
            <div className="text-xs text-white/40 mt-1">{s.label}</div>
          </div>
        ))}
      </div>

      {/* Compose Modal */}
      <AnimatePresence>
        {showCompose && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4"
            onClick={e => e.target === e.currentTarget && setShowCompose(false)}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 16 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 16 }}
              className="bg-surface-50 border border-white/10 rounded-2xl p-6 w-full max-w-lg space-y-4"
            >
              <div className="flex items-center justify-between">
                <h2 className="text-lg font-bold text-white">Compose Announcement</h2>
                <button onClick={() => setShowCompose(false)} className="p-1.5 hover:bg-white/5 rounded-lg text-white/40">
                  <X className="w-4 h-4" />
                </button>
              </div>
              <div className="space-y-3">
                <input
                  placeholder="Title *"
                  value={form.title}
                  onChange={e => setForm(f => ({ ...f, title: e.target.value }))}
                  className="w-full bg-surface-100 border border-white/10 rounded-xl px-3 py-2.5 text-white text-sm focus:outline-none focus:border-brand-500/50"
                />
                <textarea
                  placeholder="Message body *"
                  rows={4}
                  value={form.body}
                  onChange={e => setForm(f => ({ ...f, body: e.target.value }))}
                  className="w-full bg-surface-100 border border-white/10 rounded-xl px-3 py-2.5 text-white text-sm focus:outline-none focus:border-brand-500/50 resize-none"
                />
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-xs text-white/40 mb-1 block">Audience Type</label>
                    <select
                      value={form.targetType}
                      onChange={e => setForm(f => ({ ...f, targetType: e.target.value as TargetType }))}
                      className="w-full bg-surface-100 border border-white/10 rounded-xl px-3 py-2.5 text-white text-sm focus:outline-none"
                    >
                      <option value="CENTER">Entire Center</option>
                      <option value="BATCH">Specific Batch</option>
                      <option value="ROLE">By Role</option>
                      <option value="ALL">All Users</option>
                    </select>
                  </div>
                  {form.targetType === 'BATCH' && (
                    <div>
                      <label className="text-xs text-white/40 mb-1 block">Batch</label>
                      <select
                        value={form.targetId}
                        onChange={e => setForm(f => ({ ...f, targetId: e.target.value }))}
                        className="w-full bg-surface-100 border border-white/10 rounded-xl px-3 py-2.5 text-white text-sm focus:outline-none"
                      >
                        <option value="">Select...</option>
                        {batches.map(b => <option key={b.id} value={b.id}>{b.name}</option>)}
                      </select>
                    </div>
                  )}
                  {form.targetType === 'ROLE' && (
                    <div>
                      <label className="text-xs text-white/40 mb-1 block">Role</label>
                      <select
                        value={form.targetRole}
                        onChange={e => setForm(f => ({ ...f, targetRole: e.target.value as TargetRole }))}
                        className="w-full bg-surface-100 border border-white/10 rounded-xl px-3 py-2.5 text-white text-sm focus:outline-none"
                      >
                        <option value="STUDENT">Students</option>
                        <option value="PARENT">Parents</option>
                        <option value="TEACHER">Teachers</option>
                        <option value="ALL">All Roles</option>
                      </select>
                    </div>
                  )}
                </div>
                <div>
                  <label className="text-xs text-white/40 mb-1 block">Schedule (optional — leave blank to send now)</label>
                  <input
                    type="datetime-local"
                    value={form.scheduledAt}
                    onChange={e => setForm(f => ({ ...f, scheduledAt: e.target.value }))}
                    className="w-full bg-surface-100 border border-white/10 rounded-xl px-3 py-2.5 text-white text-sm focus:outline-none focus:border-brand-500/50"
                  />
                </div>
              </div>
              <button
                onClick={() => send()}
                disabled={isPending || !form.title.trim() || !form.body.trim()}
                className="w-full flex items-center justify-center gap-2 py-2.5 bg-brand-600 hover:bg-brand-500 text-white rounded-xl text-sm font-semibold transition-colors disabled:opacity-50"
              >
                <Send className="w-4 h-4" />
                {isPending ? 'Sending...' : form.scheduledAt ? 'Schedule' : 'Send Now'}
              </button>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* History */}
      {isLoading ? (
        <div className="text-center py-12 text-white/40">Loading...</div>
      ) : announcements.length === 0 ? (
        <div className="flex flex-col items-center gap-3 py-16 text-white/40">
          <Megaphone className="w-10 h-10" />
          <p>No announcements yet. Create your first one above.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {announcements.map((a, i) => (
            <motion.div
              key={a.id}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.04 }}
              className="bg-surface-50/40 border border-white/5 rounded-xl p-4 space-y-2"
            >
              <div className="flex items-start justify-between gap-3">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-semibold text-white">{a.title}</span>
                    {a.sentAt && <CheckCircle className="w-3.5 h-3.5 text-emerald-400 flex-shrink-0" />}
                  </div>
                  <div className="text-xs text-white/50 mt-1 line-clamp-2">{a.body}</div>
                </div>
                <button
                  onClick={() => expire(a.id)}
                  className="p-1.5 hover:bg-red-500/10 rounded-lg text-white/20 hover:text-red-400 transition-colors flex-shrink-0"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
              <div className="flex items-center gap-4 text-xs text-white/30">
                <span className="flex items-center gap-1">
                  <Users className="w-3 h-3" /> {a.targetType}{a.targetRole ? ` / ${a.targetRole}` : ''}
                </span>
                <span className="flex items-center gap-1">
                  <Clock className="w-3 h-3" />
                  {new Date(a.sentAt ?? a.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}
                </span>
              </div>
            </motion.div>
          ))}
        </div>
      )}
    </div>
  );
}
