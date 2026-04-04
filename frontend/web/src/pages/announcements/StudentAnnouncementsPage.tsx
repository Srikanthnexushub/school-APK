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
  metadata?: Record<string, string>;
}

interface CenterAnnouncement {
  id: string;
  title: string;
  body: string;
  sentAt?: string;
  createdAt: string;
  targetType: string;
}

export default function StudentAnnouncementsPage() {
  const { user } = useAuthStore();
  const qc = useQueryClient();
  const centerId = user?.centerId;

  // In-app notifications (for read/unread state)
  // Notifications are user-scoped (by userId from JWT), not center-scoped.
  const { data: notifications = [], isError } = useQuery<Notification[]>({
    queryKey: ['notifications-inapp'],
    queryFn: () =>
      api.get('/api/v1/notifications/inapp?size=500').then(r =>
        Array.isArray(r.data) ? r.data : (r.data.content ?? [])),
    enabled: !!user,
    refetchInterval: 30_000,
  });

  // Center announcements — source of truth, always complete
  const { data: centerAnnouncements = [], isLoading } = useQuery<CenterAnnouncement[]>({
    queryKey: ['center-announcements-student', centerId],
    queryFn: () =>
      api.get(`/api/v1/centers/${centerId}/announcements`).then(r =>
        Array.isArray(r.data) ? r.data : (r.data.content ?? [])),
    enabled: !!centerId,
    staleTime: 60_000,
  });

  const { mutate: markRead } = useMutation({
    mutationFn: (id: string) => api.put(`/api/v1/notifications/${id}/read`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications-inapp'] }),
  });

  const { mutate: markAll } = useMutation({
    mutationFn: () => api.put('/api/v1/notifications/mark-all-read'),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications-inapp'] }),
  });

  // Build a map of announcementId → notification for read-state lookup
  const notifByAnnouncementId = new Map(
    notifications
      .filter(n => n.metadata?.announcementId)
      .map(n => [n.metadata!.announcementId, n])
  );
  const unreadNotifIds = new Set(notifications.filter(n => !n.readAt).map(n => n.id));
  const unread = unreadNotifIds.size;

  // Sorted newest-first
  const sorted = [...centerAnnouncements].sort(
    (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  );

  return (
    <div className="p-6 space-y-6 max-w-2xl mx-auto">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-cyan-500/10 border border-cyan-500/30" style={{ boxShadow: '0 0 10px rgba(34,211,238,0.15)' }}>
            <Bell className="w-5 h-5 text-cyan-400" />
          </div>
          <div>
            <h1 className="text-xl font-bold" style={{ background: 'linear-gradient(90deg,#22d3ee,#a855f7)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>Announcements</h1>
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

      {isError ? (
        <div className="flex flex-col items-center gap-3 py-16 text-red-400/60">
          <Bell className="w-10 h-10" />
          <p>Failed to load notifications. Please try again.</p>
        </div>
      ) : isLoading ? (
        <div className="text-center py-16 text-white/40">Loading...</div>
      ) : sorted.length === 0 ? (
        <div className="flex flex-col items-center gap-3 py-16 text-white/40">
          <Bell className="w-10 h-10" />
          <p>No announcements yet.</p>
        </div>
      ) : (
        <div className="space-y-2">
          {sorted.map((a, i) => {
            const notif = notifByAnnouncementId.get(a.id);
            const isUnread = notif ? !notif.readAt : false;
            return (
              <motion.div
                key={a.id}
                initial={{ opacity: 0, y: 6 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.03 }}
                onClick={() => notif && isUnread && markRead(notif.id)}
                className={`relative bg-surface-50/40 border rounded-xl p-4 transition-colors hover:bg-surface-50/60 ${
                  isUnread ? 'border-brand-500/20 cursor-pointer' : 'border-white/5'
                }`}
              >
                {isUnread && (
                  <span className="absolute top-3 right-3 w-2 h-2 rounded-full bg-brand-500" />
                )}
                <div className="text-sm font-semibold text-white pr-4">{a.title}</div>
                <div className="text-xs text-white/50 mt-1 line-clamp-3">{a.body}</div>
                <div className="flex items-center gap-1 mt-2 text-xs" style={{ color: 'rgba(34,211,238,0.6)' }}>
                  <Clock className="w-3 h-3" />
                  {new Date(a.sentAt ?? a.createdAt).toLocaleDateString('en-IN', {
                    day: 'numeric', month: 'short', year: 'numeric',
                  })}
                </div>
              </motion.div>
            );
          })}
        </div>
      )}
    </div>
  );
}
