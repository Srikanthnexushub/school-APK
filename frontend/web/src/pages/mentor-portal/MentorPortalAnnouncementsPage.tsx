import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import { Megaphone, Plus, X, Send, Clock, Users } from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';

interface Batch { id: string; name: string; code: string; }
interface Center { id: string; }
interface Announcement {
  id: string; centerId: string; title: string; body: string;
  targetType: string; targetId?: string; sentAt?: string; createdAt: string;
}

type TargetType = 'BATCH' | 'CENTER' | 'ALL';

export default function MentorPortalAnnouncementsPage() {
  const { user } = useAuthStore();
  const qc = useQueryClient();

  const { data: centers = [] } = useQuery<Center[]>({
    queryKey: ['teacher-centers-announcements'],
    queryFn: () => api.get('/api/v1/centers').then(r => Array.isArray(r.data) ? r.data : (r.data.content ?? [])),
    enabled: !!user,
    staleTime: 10 * 60_000,
  });
  const centerId = centers[0]?.id;
  const [showCompose, setShowCompose] = useState(false);
  const [form, setForm] = useState({ title: '', body: '', targetType: 'BATCH' as TargetType, targetId: '' });

  const { data: batches = [] } = useQuery<Batch[]>({
    queryKey: ['batches', centerId],
    queryFn: () => api.get(`/api/v1/centers/${centerId}/batches`).then(r =>
      Array.isArray(r.data) ? r.data : (r.data.content ?? [])),
    enabled: !!centerId,
  });

  const { data: announcements = [], isLoading } = useQuery<Announcement[]>({
    queryKey: ['announcements', centerId],
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
      targetRole: null,
      scheduledAt: null,
      expiresAt: null,
    }),
    onSuccess: () => {
      toast.success('Announcement sent!');
      setShowCompose(false);
      setForm({ title: '', body: '', targetType: 'BATCH', targetId: '' });
      qc.invalidateQueries({ queryKey: ['announcements', centerId] });
    },
    onError: () => toast.error('Failed to send announcement'),
  });

  return (
    <div className="p-6 space-y-6 max-w-3xl mx-auto">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-cyan-500/10 border border-cyan-500/30" style={{ boxShadow: '0 0 10px rgba(34,211,238,0.15)' }}>
            <Megaphone className="w-5 h-5 text-cyan-400" />
          </div>
          <div>
            <h1 className="text-xl font-bold" style={{ background: 'linear-gradient(90deg,#22d3ee,#a855f7)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>Announcements</h1>
            <p className="text-sm text-white/50">Send messages to your batch students</p>
          </div>
        </div>
        <button
          onClick={() => setShowCompose(true)}
          className="flex items-center gap-2 px-4 py-2 bg-brand-600 hover:bg-brand-500 text-white rounded-xl text-sm font-semibold transition-colors"
        >
          <Plus className="w-4 h-4" /> New
        </button>
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
                  placeholder="Title"
                  value={form.title}
                  onChange={e => setForm(f => ({ ...f, title: e.target.value }))}
                  className="w-full bg-surface-100 border border-white/10 rounded-xl px-3 py-2.5 text-white text-sm focus:outline-none focus:border-brand-500/50"
                />
                <textarea
                  placeholder="Message body..."
                  rows={4}
                  value={form.body}
                  onChange={e => setForm(f => ({ ...f, body: e.target.value }))}
                  className="w-full bg-surface-100 border border-white/10 rounded-xl px-3 py-2.5 text-white text-sm focus:outline-none focus:border-brand-500/50 resize-none"
                />
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-xs text-white/40 mb-1 block">Target</label>
                    <select
                      value={form.targetType}
                      onChange={e => setForm(f => ({ ...f, targetType: e.target.value as TargetType }))}
                      className="w-full bg-surface-100 border border-white/10 rounded-xl px-3 py-2.5 text-white text-sm focus:outline-none"
                    >
                      <option value="BATCH">Specific Batch</option>
                      <option value="CENTER">Entire Center</option>
                      <option value="ALL">All</option>
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
                        <option value="">Select batch...</option>
                        {batches.map(b => <option key={b.id} value={b.id}>{b.name}</option>)}
                      </select>
                    </div>
                  )}
                </div>
              </div>
              <button
                onClick={() => send()}
                disabled={isPending || !form.title.trim() || !form.body.trim()}
                className="w-full flex items-center justify-center gap-2 py-2.5 bg-brand-600 hover:bg-brand-500 text-white rounded-xl text-sm font-semibold transition-colors disabled:opacity-50"
              >
                <Send className="w-4 h-4" />
                {isPending ? 'Sending...' : 'Send Announcement'}
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
          <p>No announcements yet. Send your first one!</p>
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
              <div className="flex items-start gap-3">
                <div className="flex-1 min-w-0">
                  <div className="text-sm font-semibold text-white">{a.title}</div>
                  <div className="text-xs text-white/50 mt-1 line-clamp-2">{a.body}</div>
                </div>
              </div>
              <div className="flex items-center gap-3 text-xs" style={{ color: 'rgba(34,211,238,0.6)' }}>
                <span className="flex items-center gap-1">
                  <Users className="w-3 h-3" /> {a.targetType}
                </span>
                <span className="flex items-center gap-1">
                  <Clock className="w-3 h-3" />
                  {new Date(a.sentAt ?? a.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}
                </span>
                {a.sentAt && <span className="text-emerald-400">Sent</span>}
              </div>
            </motion.div>
          ))}
        </div>
      )}
    </div>
  );
}
