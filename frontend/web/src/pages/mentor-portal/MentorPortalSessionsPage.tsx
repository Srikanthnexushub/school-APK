import { useState } from 'react';
import { motion } from 'framer-motion';
import { useQuery } from '@tanstack/react-query';
import {
  Calendar, Video, CheckCircle2, XCircle, Clock, Filter,
  AlertCircle, BookOpen,
} from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { cn } from '../../lib/utils';
import { Avatar } from '../../components/ui/Avatar';
import { Badge } from '../../components/ui/Badge';

interface MentorSession {
  id: string;
  studentName: string;
  scheduledAt: string;
  subject: string;
  durationMinutes: number;
  status: 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'PENDING' | 'CANCELLED';
  notes?: string;
}

const MOCK_SESSIONS: MentorSession[] = [
  { id: 's1', studentName: 'Arjun Kapoor',  scheduledAt: new Date(Date.now() + 3600000).toISOString(),          subject: 'Mechanics',       durationMinutes: 60,  status: 'IN_PROGRESS' },
  { id: 's2', studentName: 'Sneha Reddy',   scheduledAt: new Date(Date.now() + 86400000).toISOString(),          subject: 'Calculus',         durationMinutes: 60,  status: 'SCHEDULED' },
  { id: 's3', studentName: 'Vikram Das',    scheduledAt: new Date(Date.now() + 86400000 * 2).toISOString(),      subject: 'Thermodynamics',   durationMinutes: 90,  status: 'SCHEDULED' },
  { id: 's4', studentName: 'Priya Nath',    scheduledAt: new Date(Date.now() + 86400000 * 3).toISOString(),      subject: 'Electrostatics',   durationMinutes: 60,  status: 'PENDING' },
  { id: 's5', studentName: 'Rohit Verma',   scheduledAt: new Date(Date.now() + 86400000 * 4).toISOString(),      subject: 'Optics',           durationMinutes: 60,  status: 'SCHEDULED' },
  { id: 's6', studentName: 'Kavya Sharma',  scheduledAt: new Date(Date.now() - 86400000).toISOString(),          subject: 'Algebra',          durationMinutes: 60,  status: 'COMPLETED', notes: 'Good progress on quadratic equations.' },
  { id: 's7', studentName: 'Rahul Menon',   scheduledAt: new Date(Date.now() - 86400000 * 2).toISOString(),      subject: 'Organic Chemistry', durationMinutes: 90, status: 'COMPLETED', notes: 'Covered IUPAC naming, alkyl halides.' },
  { id: 's8', studentName: 'Divya Iyer',    scheduledAt: new Date(Date.now() - 86400000 * 3).toISOString(),      subject: 'Kinematics',       durationMinutes: 60,  status: 'COMPLETED' },
  { id: 's9', studentName: 'Aditya Singh',  scheduledAt: new Date(Date.now() - 86400000 * 5).toISOString(),      subject: 'Wave Optics',      durationMinutes: 60,  status: 'CANCELLED' },
];

type FilterTab = 'ALL' | 'UPCOMING' | 'COMPLETED' | 'PENDING';

const STATUS_BADGE: Record<MentorSession['status'], { variant: 'success' | 'info' | 'warning' | 'default' | 'error'; label: string }> = {
  IN_PROGRESS: { variant: 'success',  label: 'In Progress' },
  SCHEDULED:   { variant: 'info',     label: 'Scheduled'   },
  PENDING:     { variant: 'warning',  label: 'Pending'     },
  COMPLETED:   { variant: 'default',  label: 'Completed'   },
  CANCELLED:   { variant: 'error',    label: 'Cancelled'   },
};

export default function MentorPortalSessionsPage() {
  const user = useAuthStore((s) => s.user);
  const [filter, setFilter] = useState<FilterTab>('ALL');

  const { data: mentorProfile } = useQuery<{ id: string } | null>({
    queryKey: ['mentor-profile', user?.id],
    queryFn: async () => {
      const res = await api.get('/api/v1/mentors');
      const profiles: Array<{ id: string; userId: string }> = res.data;
      return profiles.find((p) => p.userId === user?.id) ?? null;
    },
    retry: false,
    placeholderData: null,
  });

  const { data: apiSessions } = useQuery<MentorSession[]>({
    queryKey: ['mentor-sessions-all', mentorProfile?.id],
    queryFn: async () => {
      const res = await api.get(`/api/v1/mentor-sessions?mentorId=${mentorProfile!.id}`);
      return res.data;
    },
    enabled: !!mentorProfile?.id,
    retry: false,
    placeholderData: MOCK_SESSIONS,
  });

  const sessions = apiSessions ?? MOCK_SESSIONS;

  const filtered = sessions.filter((s) => {
    if (filter === 'UPCOMING')  return s.status === 'SCHEDULED' || s.status === 'IN_PROGRESS';
    if (filter === 'PENDING')   return s.status === 'PENDING';
    if (filter === 'COMPLETED') return s.status === 'COMPLETED' || s.status === 'CANCELLED';
    return true;
  });

  const counts = {
    ALL:       sessions.length,
    UPCOMING:  sessions.filter((s) => s.status === 'SCHEDULED' || s.status === 'IN_PROGRESS').length,
    PENDING:   sessions.filter((s) => s.status === 'PENDING').length,
    COMPLETED: sessions.filter((s) => s.status === 'COMPLETED' || s.status === 'CANCELLED').length,
  };

  const TABS: { id: FilterTab; label: string }[] = [
    { id: 'ALL',       label: 'All Sessions' },
    { id: 'UPCOMING',  label: 'Upcoming'     },
    { id: 'PENDING',   label: 'Pending'      },
    { id: 'COMPLETED', label: 'History'      },
  ];

  return (
    <motion.div
      className="p-6 space-y-6 max-w-6xl mx-auto"
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
    >
      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">
            My <span className="gradient-text">Sessions</span>
          </h1>
          <p className="text-white/40 text-sm mt-1">
            Manage upcoming bookings and review your session history
          </p>
        </div>
        <div className="flex items-center gap-2 text-white/40">
          <Filter className="w-4 h-4" />
          <span className="text-sm">{counts.ALL} total</span>
        </div>
      </div>

      {/* Filter tabs */}
      <div className="flex gap-1 p-1 glass rounded-xl w-fit">
        {TABS.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setFilter(tab.id)}
            className={cn(
              'flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium transition-all',
              filter === tab.id
                ? 'bg-indigo-500 text-white shadow-lg shadow-indigo-500/25'
                : 'text-white/50 hover:text-white/80 hover:bg-white/5'
            )}
          >
            {tab.label}
            <span className={cn(
              'px-1.5 py-0.5 rounded-full text-xs',
              filter === tab.id ? 'bg-white/20' : 'bg-white/10'
            )}>
              {counts[tab.id]}
            </span>
          </button>
        ))}
      </div>

      {/* Sessions list */}
      {filtered.length === 0 ? (
        <div className="glass rounded-2xl p-12 flex flex-col items-center gap-4 text-white/40">
          <BookOpen className="w-14 h-14 opacity-30" />
          <p>No sessions in this category</p>
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map((session, i) => {
            const date = new Date(session.scheduledAt);
            const isPast = date < new Date();
            const cfg = STATUS_BADGE[session.status];

            return (
              <motion.div
                key={session.id}
                initial={{ opacity: 0, y: 12 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.04 }}
                className="glass rounded-2xl p-5 flex items-center gap-4"
              >
                <Avatar name={session.studentName} size="md" />

                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="font-semibold text-white">{session.studentName}</span>
                    <Badge variant={cfg.variant}>{cfg.label}</Badge>
                  </div>
                  <div className="flex items-center gap-3 mt-1 text-xs text-white/40 flex-wrap">
                    <span className="flex items-center gap-1">
                      <BookOpen className="w-3 h-3" /> {session.subject}
                    </span>
                    <span className="flex items-center gap-1">
                      <Clock className="w-3 h-3" /> {session.durationMinutes} min
                    </span>
                    <span className="flex items-center gap-1">
                      <Calendar className="w-3 h-3" />
                      {date.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}
                      {' '}
                      {date.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })}
                    </span>
                  </div>
                  {session.notes && (
                    <p className="text-xs text-white/30 mt-1.5 italic">"{session.notes}"</p>
                  )}
                </div>

                {/* Actions */}
                <div className="shrink-0 flex items-center gap-2">
                  {session.status === 'IN_PROGRESS' && (
                    <button
                      onClick={() => toast.info('Launching session room…')}
                      className="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-medium transition-all"
                    >
                      <Video className="w-3.5 h-3.5" /> Join
                    </button>
                  )}
                  {session.status === 'PENDING' && (
                    <>
                      <button
                        onClick={() => toast.success('Booking accepted!')}
                        className="p-2 rounded-xl bg-emerald-600/20 text-emerald-400 hover:bg-emerald-600/30 transition-colors"
                        title="Accept"
                      >
                        <CheckCircle2 className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => toast.info('Booking declined.')}
                        className="p-2 rounded-xl bg-red-600/20 text-red-400 hover:bg-red-600/30 transition-colors"
                        title="Decline"
                      >
                        <XCircle className="w-4 h-4" />
                      </button>
                    </>
                  )}
                  {session.status === 'COMPLETED' && (
                    <span className="text-xs text-emerald-400/60 flex items-center gap-1">
                      <CheckCircle2 className="w-3.5 h-3.5" /> Done
                    </span>
                  )}
                  {session.status === 'CANCELLED' && (
                    <span className="text-xs text-red-400/60 flex items-center gap-1">
                      <XCircle className="w-3.5 h-3.5" /> Cancelled
                    </span>
                  )}
                  {session.status === 'SCHEDULED' && !isPast && (
                    <span className="text-xs text-white/30 flex items-center gap-1">
                      <AlertCircle className="w-3.5 h-3.5" /> Upcoming
                    </span>
                  )}
                </div>
              </motion.div>
            );
          })}
        </div>
      )}
    </motion.div>
  );
}
