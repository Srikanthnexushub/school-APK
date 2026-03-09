import { useState } from 'react';
import { motion } from 'framer-motion';
import { useQuery, useMutation } from '@tanstack/react-query';
import {
  Users, Star, Clock, TrendingUp, Video, Calendar,
  CheckCircle2, XCircle, AlertCircle, Check,
} from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { cn } from '../../lib/utils';
import { Avatar } from '../../components/ui/Avatar';

interface MentorStats {
  sessionsThisWeek: number;
  avgRating: number;
  pendingBookings: number;
  totalStudentsHelped: number;
}

interface UpcomingSession {
  id: string;
  studentName: string;
  scheduledAt: string;
  subject: string;
  durationMinutes: number;
  status: 'SCHEDULED' | 'IN_PROGRESS' | 'PENDING';
}

const MOCK_STATS: MentorStats = {
  sessionsThisWeek: 8,
  avgRating: 4.8,
  pendingBookings: 3,
  totalStudentsHelped: 47,
};

const MOCK_UPCOMING: UpcomingSession[] = [
  { id: 'us1', studentName: 'Arjun Kapoor', scheduledAt: new Date(Date.now() + 3600000).toISOString(), subject: 'Mechanics', durationMinutes: 60, status: 'IN_PROGRESS' },
  { id: 'us2', studentName: 'Sneha Reddy', scheduledAt: new Date(Date.now() + 86400000).toISOString(), subject: 'Calculus', durationMinutes: 60, status: 'SCHEDULED' },
  { id: 'us3', studentName: 'Vikram Das', scheduledAt: new Date(Date.now() + 86400000 * 2).toISOString(), subject: 'Thermodynamics', durationMinutes: 90, status: 'SCHEDULED' },
  { id: 'us4', studentName: 'Priya Nath', scheduledAt: new Date(Date.now() + 86400000 * 3).toISOString(), subject: 'Electrostatics', durationMinutes: 60, status: 'PENDING' },
  { id: 'us5', studentName: 'Rohit Verma', scheduledAt: new Date(Date.now() + 86400000 * 4).toISOString(), subject: 'Optics', durationMinutes: 60, status: 'SCHEDULED' },
];

const DAYS_OF_WEEK = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

const STATUS_CONFIG: Record<UpcomingSession['status'], { label: string; badge: string; icon: React.ReactNode }> = {
  IN_PROGRESS: { label: 'In Progress', badge: 'bg-emerald-600/20 text-emerald-300', icon: <CheckCircle2 className="w-4 h-4 text-emerald-400" /> },
  SCHEDULED: { label: 'Scheduled', badge: 'bg-brand-600/20 text-brand-300', icon: <Calendar className="w-4 h-4 text-brand-400" /> },
  PENDING: { label: 'Pending', badge: 'bg-amber-600/20 text-amber-300', icon: <AlertCircle className="w-4 h-4 text-amber-400" /> },
};

function StatCard({ icon, label, value, sub, color }: { icon: React.ReactNode; label: string; value: string | number; sub?: string; color: string }) {
  return (
    <motion.div
      className="card"
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
    >
      <div className={cn('w-10 h-10 rounded-xl flex items-center justify-center mb-3', color)}>
        {icon}
      </div>
      <p className="text-white/50 text-sm">{label}</p>
      <p className="text-3xl font-bold text-white mt-1">{value}</p>
      {sub && <p className="text-white/30 text-xs mt-1">{sub}</p>}
    </motion.div>
  );
}

export default function MentorPortalDashboardPage() {
  const user = useAuthStore((s) => s.user);
  const [availability, setAvailability] = useState<Record<string, boolean>>({
    Mon: true, Tue: false, Wed: true, Thu: true, Fri: true, Sat: false, Sun: false,
  });

  // Fetch mentor profile by listing all and finding by userId
  const { data: mentorProfile } = useQuery<{ id: string; averageRating: number; totalSessions: number } | null>({
    queryKey: ['mentor-profile', user?.id],
    queryFn: async () => {
      const res = await api.get('/api/v1/mentors');
      const raw = res.data;
      const profiles: Array<{ id: string; userId: string; averageRating: number; totalSessions: number }> = Array.isArray(raw) ? raw : (raw.content ?? []);
      return profiles.find((p) => p.userId === user?.id) ?? null;
    },
    retry: false,
    placeholderData: null,
  });

  const profileId = mentorProfile?.id;

  const { data: upcomingSessions } = useQuery<UpcomingSession[]>({
    queryKey: ['mentor-upcoming-sessions', profileId],
    queryFn: async () => {
      const res = await api.get(`/api/v1/mentor-sessions?mentorId=${profileId}`);
      const d = res.data;
      return Array.isArray(d) ? d : (d.content ?? []);
    },
    enabled: !!profileId,
    retry: false,
    placeholderData: MOCK_UPCOMING,
  });

  const sessions = upcomingSessions ?? MOCK_UPCOMING;

  const stats: MentorStats = mentorProfile
    ? {
        sessionsThisWeek: sessions.filter((s) => {
          const d = new Date(s.scheduledAt);
          const now = new Date();
          const startOfWeek = new Date(now);
          startOfWeek.setDate(now.getDate() - now.getDay());
          return d >= startOfWeek;
        }).length,
        avgRating: mentorProfile.averageRating ?? 0,
        pendingBookings: sessions.filter((s) => s.status === 'PENDING').length,
        totalStudentsHelped: mentorProfile.totalSessions ?? 0,
      }
    : MOCK_STATS;

  const s = stats;

  // Save availability mutation
  const saveAvailabilityMutation = useMutation({
    mutationFn: async () => {
      if (!profileId) throw new Error('Mentor profile not loaded');
      // The UpdateMentorAvailabilityRequest DTO takes a single boolean `available`.
      // We derive it as true if any day is selected, false if none are selected.
      const isAvailable = Object.values(availability).some(Boolean);
      await api.patch(`/api/v1/mentors/${profileId}/availability`, { available: isAvailable });
    },
    onSuccess: () => {
      toast.success('Availability saved!');
    },
    onError: () => {
      toast.error('Failed to save availability. Please try again.');
    },
  });

  function toggleDay(day: string) {
    setAvailability((prev) => {
      const next = { ...prev, [day]: !prev[day] };
      toast.success(`${day} ${next[day] ? 'available' : 'unavailable'}`);
      return next;
    });
  }

  return (
    <div className="min-h-screen p-6 space-y-8">
      {/* Hero */}
      <motion.div
        initial={{ opacity: 0, y: -16 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative overflow-hidden glass rounded-3xl p-8 border border-white/5"
      >
        <div className="absolute top-0 right-0 w-64 h-64 bg-brand-600/10 rounded-full blur-3xl translate-x-1/2 -translate-y-1/2" />
        <div className="relative z-10 flex items-center gap-5">
          <Avatar name={user?.name ?? 'Mentor'} size="xl" />
          <div>
            <p className="text-brand-400 text-sm font-medium mb-1">Mentor Portal</p>
            <h1 className="text-3xl font-bold text-white">Welcome, {user?.name?.split(' ')[0] ?? 'Mentor'}</h1>
            <p className="text-white/50 mt-1">You have <span className="text-amber-400 font-semibold">{s.pendingBookings} pending</span> booking requests.</p>
          </div>
        </div>
      </motion.div>

      {/* Stats */}
      <div className="grid grid-cols-2 xl:grid-cols-4 gap-5">
        <StatCard
          icon={<Clock className="w-5 h-5 text-brand-400" />}
          label="Sessions This Week"
          value={s.sessionsThisWeek}
          color="bg-brand-600/20"
        />
        <StatCard
          icon={<Star className="w-5 h-5 text-amber-400" />}
          label="Average Rating"
          value={s.avgRating}
          sub="Based on all sessions"
          color="bg-amber-600/20"
        />
        <StatCard
          icon={<AlertCircle className="w-5 h-5 text-orange-400" />}
          label="Pending Bookings"
          value={s.pendingBookings}
          sub="Awaiting confirmation"
          color="bg-orange-600/20"
        />
        <StatCard
          icon={<Users className="w-5 h-5 text-emerald-400" />}
          label="Students Helped"
          value={s.totalStudentsHelped}
          sub="All time"
          color="bg-emerald-600/20"
        />
      </div>

      {/* Upcoming Sessions */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className="card"
      >
        <div className="flex items-center gap-2 mb-5">
          <TrendingUp className="w-5 h-5 text-brand-400" />
          <h2 className="text-lg font-semibold text-white">Upcoming Sessions</h2>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-white/40 text-xs uppercase tracking-wider">
                <th className="text-left pb-3 pr-4">Student</th>
                <th className="text-left pb-3 pr-4">Date & Time</th>
                <th className="text-left pb-3 pr-4">Subject</th>
                <th className="text-left pb-3 pr-4">Duration</th>
                <th className="text-left pb-3 pr-4">Status</th>
                <th className="text-left pb-3">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {sessions.map((session, i) => {
                const config = STATUS_CONFIG[session.status];
                const date = new Date(session.scheduledAt);
                return (
                  <motion.tr
                    key={session.id}
                    initial={{ opacity: 0, x: -12 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.15 + i * 0.05 }}
                    className="group"
                  >
                    <td className="py-3 pr-4">
                      <div className="flex items-center gap-2.5">
                        <Avatar name={session.studentName} size="sm" />
                        <span className="text-white font-medium">{session.studentName}</span>
                      </div>
                    </td>
                    <td className="py-3 pr-4 text-white/60">
                      <p>{date.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}</p>
                      <p className="text-xs text-white/30">{date.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })}</p>
                    </td>
                    <td className="py-3 pr-4 text-white/70">{session.subject}</td>
                    <td className="py-3 pr-4 text-white/60">{session.durationMinutes} min</td>
                    <td className="py-3 pr-4">
                      <span className={cn('badge', config.badge)}>
                        {config.label}
                      </span>
                    </td>
                    <td className="py-3">
                      {session.status === 'IN_PROGRESS' ? (
                        <button
                          onClick={() => toast.info('Launching session room…')}
                          className="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-medium transition-all"
                        >
                          <Video className="w-3.5 h-3.5" /> Start
                        </button>
                      ) : session.status === 'PENDING' ? (
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => toast.success('Booking accepted!')}
                            className="p-1.5 rounded-lg bg-emerald-600/20 text-emerald-400 hover:bg-emerald-600/30 transition-colors"
                          >
                            <CheckCircle2 className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => toast.info('Booking declined.')}
                            className="p-1.5 rounded-lg bg-red-600/20 text-red-400 hover:bg-red-600/30 transition-colors"
                          >
                            <XCircle className="w-4 h-4" />
                          </button>
                        </div>
                      ) : (
                        <span className="text-white/30 text-xs">—</span>
                      )}
                    </td>
                  </motion.tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </motion.div>

      {/* Availability */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.2 }}
        className="card"
      >
        <div className="flex items-center gap-2 mb-5">
          <Calendar className="w-5 h-5 text-brand-400" />
          <div>
            <h2 className="text-lg font-semibold text-white">Availability Settings</h2>
            <p className="text-white/40 text-sm">Toggle the days you are available for new bookings</p>
          </div>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-7 gap-3">
          {DAYS_OF_WEEK.map((day) => (
            <motion.button
              key={day}
              onClick={() => toggleDay(day)}
              whileTap={{ scale: 0.95 }}
              className={cn(
                'flex flex-col items-center py-3 rounded-xl border transition-all',
                availability[day]
                  ? 'bg-brand-600 border-brand-500 text-white'
                  : 'glass border-white/10 text-white/40 hover:border-white/20 hover:text-white/60'
              )}
            >
              <span className="font-semibold text-sm">{day}</span>
              <span className="text-xs mt-1 opacity-70">{availability[day] ? 'Available' : 'Off'}</span>
            </motion.button>
          ))}
        </div>

        <div className="mt-4 flex items-center justify-between">
          <p className="text-white/40 text-sm">
            Available on: {DAYS_OF_WEEK.filter((d) => availability[d]).join(', ') || 'No days selected'}
          </p>
          <button
            onClick={() => saveAvailabilityMutation.mutate()}
            disabled={!profileId || saveAvailabilityMutation.isPending}
            className="btn-primary text-sm flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Check className="w-4 h-4" /> Save Availability
          </button>
        </div>
      </motion.div>
    </div>
  );
}
