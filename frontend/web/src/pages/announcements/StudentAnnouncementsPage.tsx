import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import { Bell, Clock, CheckCheck } from 'lucide-react';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';

interface Notification {
  id: string;
  subject: string;
  body: string;
  channel: string;
  readAt?: string;
  createdAt?: string;
  notificationType?: string;
}

export default function StudentAnnouncementsPage() {
  const { user } = useAuthStore();
  const qc = useQueryClient();

  const { data: notifications = [], isLoading } = useQuery<Notification[]>({
    queryKey: ['notifications-inapp'],
    queryFn: () =>
      api.get('/api/v1/notifications/inapp?size=50').then(r =>
        Array.isArray(r.data) ? r.data : (r.data.content ?? [])),
    enabled: !!user,
    refetchInterval: 30_000,
  });

  const { mutate: markRead } = useMutation({
    mutationFn: (id: string) => api.put(`/api/v1/notifications/${id}/read`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications-inapp'] }),
  });

  const { mutate: markAll } = useMutation({
    mutationFn: () => api.put('/api/v1/notifications/mark-all-read'),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications-inapp'] }),
  });

  const unread = notifications.filter(n => !n.readAt).length;

  return (
    <div className="p-6 space-y-6 max-w-2xl mx-auto">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-brand-500/10 border border-brand-500/20">
            <Bell className="w-5 h-5 text-brand-400" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-white">Announcements</h1>
            <p className="text-sm text-white/50">
              {unread > 0 ? `${unread} unread` : 'All caught up!'}
            </p>
          </div>
        </div>
        {unread > 0 && (
          <button
            onClick={() => markAll()}
            className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-white/50 hover:text-white border border-white/10 rounded-lg hover:bg-white/5 transition-colors"
          >
            <CheckCheck className="w-3.5 h-3.5" /> Mark all read
          </button>
        )}
      </div>

      {isLoading ? (
        <div className="text-center py-16 text-white/40">Loading...</div>
      ) : notifications.length === 0 ? (
        <div className="flex flex-col items-center gap-3 py-16 text-white/40">
          <Bell className="w-10 h-10" />
          <p>No announcements yet.</p>
        </div>
      ) : (
        <div className="space-y-2">
          {notifications.map((n, i) => (
            <motion.div
              key={n.id}
              initial={{ opacity: 0, y: 6 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.03 }}
              onClick={() => !n.readAt && markRead(n.id)}
              className={`relative bg-surface-50/40 border rounded-xl p-4 cursor-pointer transition-colors hover:bg-surface-50/60 ${
                n.readAt ? 'border-white/5 opacity-60' : 'border-brand-500/20'
              }`}
            >
              {!n.readAt && (
                <span className="absolute top-3 right-3 w-2 h-2 rounded-full bg-brand-500" />
              )}
              <div className="text-sm font-semibold text-white pr-4">{n.subject}</div>
              <div className="text-xs text-white/50 mt-1 line-clamp-3">{n.body}</div>
              <div className="flex items-center gap-1 mt-2 text-xs text-white/30">
                <Clock className="w-3 h-3" />
                {n.createdAt
                  ? new Date(n.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
                  : 'Recently'}
              </div>
            </motion.div>
          ))}
        </div>
      )}
    </div>
  );
}
