import { motion } from 'framer-motion';
import { Bell, CheckCircle2, AlertTriangle, Info, X } from 'lucide-react';
import { cn } from '../../lib/utils';

interface Notification {
  id: string;
  type: 'info' | 'success' | 'warning';
  title: string;
  message: string;
  time: string;
  read: boolean;
}

const mockNotifications: Notification[] = [
  { id: '1', type: 'warning', title: 'Weak area detected', message: 'Your Organic Chemistry score dropped to 34%. Practice now.', time: '5m ago', read: false },
  { id: '2', type: 'success', title: 'Study streak!', message: 'You have maintained a 14-day study streak. Keep it up!', time: '1h ago', read: false },
  { id: '3', type: 'info', title: 'New mock test available', message: 'JEE Advanced Mock #5 is now available. Attempt before Mar 20.', time: '3h ago', read: false },
  { id: '4', type: 'success', title: 'Assessment completed', message: 'You scored 78/100 on Physics Chapter Test.', time: '1d ago', read: true },
];

const typeConfig = {
  info: { Icon: Info, color: 'text-blue-400', bg: 'bg-blue-500/10' },
  success: { Icon: CheckCircle2, color: 'text-emerald-400', bg: 'bg-emerald-500/10' },
  warning: { Icon: AlertTriangle, color: 'text-amber-400', bg: 'bg-amber-500/10' },
};

interface Props {
  onClose: () => void;
}

export default function NotificationPanel({ onClose }: Props) {
  const unreadCount = mockNotifications.filter((n) => !n.read).length;

  return (
    <motion.div
      initial={{ opacity: 0, y: -8, scale: 0.95 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      exit={{ opacity: 0, y: -8, scale: 0.95 }}
      transition={{ duration: 0.15 }}
      className="absolute right-0 top-full mt-2 w-80 bg-surface-50 border border-white/10 rounded-2xl shadow-2xl z-50 overflow-hidden"
    >
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-white/5">
        <div className="flex items-center gap-2">
          <Bell className="w-4 h-4 text-white/50" />
          <span className="font-semibold text-white text-sm">Notifications</span>
          {unreadCount > 0 && (
            <span className="bg-brand-600 text-white text-[10px] font-bold px-1.5 py-0.5 rounded-full">
              {unreadCount}
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

      {/* Notifications list */}
      <div className="max-h-80 overflow-y-auto">
        {mockNotifications.map((notif) => {
          const { Icon, color, bg } = typeConfig[notif.type];
          return (
            <div
              key={notif.id}
              className={cn(
                'px-4 py-3 border-b border-white/5 last:border-0 hover:bg-white/5 transition-colors cursor-pointer',
                !notif.read && 'bg-brand-500/5'
              )}
            >
              <div className="flex gap-3">
                <div className={cn('p-1.5 rounded-lg flex-shrink-0 mt-0.5', bg)}>
                  <Icon className={cn('w-3.5 h-3.5', color)} />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex items-start justify-between gap-2">
                    <span className={cn('text-xs font-semibold', notif.read ? 'text-white/60' : 'text-white')}>
                      {notif.title}
                    </span>
                    {!notif.read && (
                      <span className="w-1.5 h-1.5 bg-brand-400 rounded-full flex-shrink-0 mt-1" />
                    )}
                  </div>
                  <p className="text-xs text-white/40 mt-0.5 leading-relaxed">{notif.message}</p>
                  <span className="text-[10px] text-white/20 mt-1 block">{notif.time}</span>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Footer */}
      <div className="px-4 py-2.5 border-t border-white/5">
        <button className="text-xs text-brand-400 hover:text-brand-300 transition-colors font-medium w-full text-center">
          Mark all as read
        </button>
      </div>
    </motion.div>
  );
}
