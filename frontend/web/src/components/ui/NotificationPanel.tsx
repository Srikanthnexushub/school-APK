import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Bell, X, CheckCircle2, AlertTriangle, Info, Calendar,
  BarChart3, Map, Megaphone, ChevronDown, ChevronUp, CheckCheck,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { cn } from '../../lib/utils';
import type { AppNotification } from '../../hooks/useNotifications';

// ── Type config ──────────────────────────────────────────────────────────────

const typeConfig: Record<string, { Icon: React.ElementType; color: string; bg: string; label: string }> = {
  EXAM_ANNOUNCED:  { Icon: Calendar,     color: 'text-blue-400',    bg: 'bg-blue-500/10',    label: 'Exam' },
  RESULT_PUBLISHED:{ Icon: BarChart3,    color: 'text-emerald-400', bg: 'bg-emerald-500/10', label: 'Result' },
  STUDY_ROUTE:     { Icon: Map,          color: 'text-violet-400',  bg: 'bg-violet-500/10',  label: 'Study Route' },
  ANNOUNCEMENT:    { Icon: Megaphone,    color: 'text-amber-400',   bg: 'bg-amber-500/10',   label: 'Announcement' },
  info:            { Icon: Info,         color: 'text-blue-400',    bg: 'bg-blue-500/10',    label: 'Info' },
  success:         { Icon: CheckCircle2, color: 'text-emerald-400', bg: 'bg-emerald-500/10', label: 'Success' },
  warning:         { Icon: AlertTriangle,color: 'text-amber-400',   bg: 'bg-amber-500/10',   label: 'Warning' },
};

function getTypeConfig(n: AppNotification) {
  const key = n.notificationType ?? 'info';
  return typeConfig[key] ?? typeConfig['info'];
}

// ── Study Route expander ─────────────────────────────────────────────────────

function StudyRouteExpander({ body }: { body: string }) {
  const phases = body.split(/\n---\n/).map((block) => {
    const lines = block.trim().split('\n');
    const title = lines[0] ?? '';
    const tasks = lines.slice(1).filter((l) => l.startsWith('•')).map((l) => l.slice(2).trim());
    return { title, tasks };
  });

  return (
    <div className="mt-2 space-y-2">
      {phases.map((phase, idx) => (
        <div key={idx} className="flex gap-2">
          {/* Timeline spine */}
          <div className="flex flex-col items-center">
            <div className="w-2 h-2 rounded-full bg-violet-400 flex-shrink-0 mt-1" />
            {idx < phases.length - 1 && (
              <div className="w-px flex-1 bg-violet-400/30 mt-1" />
            )}
          </div>
          <div className="pb-2 flex-1">
            <p className="text-[11px] font-semibold text-violet-300">{phase.title}</p>
            <ul className="mt-0.5 space-y-0.5">
              {phase.tasks.map((task, ti) => (
                <li key={ti} className="text-[10px] text-white/50 leading-relaxed">
                  {task}
                </li>
              ))}
            </ul>
          </div>
        </div>
      ))}
    </div>
  );
}

// ── Single notification item ─────────────────────────────────────────────────

function NotificationItem({
  notif,
  onRead,
}: {
  notif: AppNotification;
  onRead: (id: string) => void;
}) {
  const navigate = useNavigate();
  const [expanded, setExpanded] = useState(false);
  const { Icon, color, bg, label } = getTypeConfig(notif);
  const isRoute = notif.notificationType === 'STUDY_ROUTE';
  const isUnread = !notif.readAt;

  function handleClick() {
    if (!notif.readAt) onRead(notif.id);
    if (isRoute) {
      setExpanded((e) => !e);
      return;
    }
    if (notif.actionUrl) navigate(notif.actionUrl);
  }

  function formatTime(iso: string) {
    const diff = Date.now() - new Date(iso).getTime();
    const m = Math.floor(diff / 60000);
    if (m < 1) return 'Just now';
    if (m < 60) return `${m}m ago`;
    const h = Math.floor(m / 60);
    if (h < 24) return `${h}h ago`;
    return `${Math.floor(h / 24)}d ago`;
  }

  return (
    <div
      onClick={handleClick}
      className={cn(
        'px-4 py-3 border-b border-white/5 last:border-0 hover:bg-white/5 transition-colors cursor-pointer',
        isUnread && 'bg-brand-500/5'
      )}
    >
      <div className="flex gap-3">
        <div className={cn('p-1.5 rounded-lg flex-shrink-0 mt-0.5', bg)}>
          <Icon className={cn('w-3.5 h-3.5', color)} />
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex items-start justify-between gap-2">
            <div className="flex items-center gap-1.5 min-w-0">
              <span className={cn('text-xs font-semibold truncate', isUnread ? 'text-white' : 'text-white/60')}>
                {notif.subject}
              </span>
              <span className={cn('text-[9px] px-1 py-0.5 rounded font-medium flex-shrink-0', bg, color)}>
                {label}
              </span>
            </div>
            <div className="flex items-center gap-1 flex-shrink-0">
              {isUnread && <span className="w-1.5 h-1.5 bg-brand-400 rounded-full" />}
              {isRoute && (
                expanded
                  ? <ChevronUp className="w-3 h-3 text-white/30" />
                  : <ChevronDown className="w-3 h-3 text-white/30" />
              )}
            </div>
          </div>

          {/* Body — route map gets special rendering; others show plain text */}
          {isRoute ? (
            <AnimatePresence>
              {expanded && (
                <motion.div
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: 'auto', opacity: 1 }}
                  exit={{ height: 0, opacity: 0 }}
                  transition={{ duration: 0.2 }}
                  className="overflow-hidden"
                >
                  <StudyRouteExpander body={notif.body} />
                </motion.div>
              )}
              {!expanded && (
                <p className="text-[10px] text-white/40 mt-0.5 leading-relaxed line-clamp-2">
                  Tap to view your personalised study roadmap
                </p>
              )}
            </AnimatePresence>
          ) : (
            <p className="text-xs text-white/40 mt-0.5 leading-relaxed line-clamp-3">{notif.body}</p>
          )}

          <span className="text-[10px] text-white/20 mt-1 block">{formatTime(notif.createdAt)}</span>
        </div>
      </div>
    </div>
  );
}

// ── Main panel ───────────────────────────────────────────────────────────────

interface Props {
  onClose: () => void;
  notifications: AppNotification[];
  unreadCount: number;
  isLoading: boolean;
  markRead: (id: string) => void;
  markAllRead: () => void;
}

export default function NotificationPanel({
  onClose,
  notifications,
  unreadCount,
  isLoading,
  markRead,
  markAllRead,
}: Props) {
  return (
    <motion.div
      initial={{ opacity: 0, y: -8, scale: 0.95 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      exit={{ opacity: 0, y: -8, scale: 0.95 }}
      transition={{ duration: 0.15 }}
      className="absolute right-0 top-full mt-2 w-96 bg-surface-50 border border-white/10 rounded-2xl shadow-2xl z-50 overflow-hidden"
    >
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-white/5">
        <div className="flex items-center gap-2">
          <Bell className="w-4 h-4 text-white/50" />
          <span className="font-semibold text-white text-sm">Notifications</span>
          {unreadCount > 0 && (
            <span className="bg-brand-600 text-white text-[10px] font-bold px-1.5 py-0.5 rounded-full">
              {unreadCount > 99 ? '99+' : unreadCount}
            </span>
          )}
        </div>
        <button
          onClick={onClose}
          className="p-1 rounded-lg hover:bg-white/10 text-white/30 hover:text-white/70 transition-colors"
        >
          <X className="w-3.5 h-3.5" />
        </button>
      </div>

      {/* List */}
      <div className="max-h-[420px] overflow-y-auto">
        {isLoading ? (
          <div className="px-4 py-8 text-center text-white/30 text-xs">Loading…</div>
        ) : notifications.length === 0 ? (
          <div className="px-4 py-8 text-center">
            <Bell className="w-8 h-8 text-white/10 mx-auto mb-2" />
            <p className="text-white/30 text-xs">You're all caught up!</p>
          </div>
        ) : (
          notifications.map((n) => (
            <NotificationItem key={n.id} notif={n} onRead={markRead} />
          ))
        )}
      </div>

      {/* Footer */}
      {notifications.length > 0 && (
        <div className="px-4 py-2.5 border-t border-white/5 flex items-center justify-between">
          <span className="text-[10px] text-white/20">{notifications.length} notifications</span>
          {unreadCount > 0 && (
            <button
              onClick={markAllRead}
              className="flex items-center gap-1 text-xs text-brand-400 hover:text-brand-300 transition-colors font-medium"
            >
              <CheckCheck className="w-3 h-3" />
              Mark all read
            </button>
          )}
        </div>
      )}
    </motion.div>
  );
}
