import { useState } from 'react';
import { motion } from 'framer-motion';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Users, Star, Clock, TrendingUp, Video, Calendar,
  CheckCircle2, XCircle, AlertCircle, Check, Loader2, RefreshCw,
} from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { cn } from '../../lib/utils';
import { Avatar } from '../../components/ui/Avatar';

interface MentorStats {
  sessionsThisWeek: number;
  avgRating: number | null;
  pendingBookings: number;
  totalStudentsHelped: number;
}

interface UpcomingSession {
  id: string;
  studentName: string;
  scheduledAt: string;
  subject: string;
  durationMinutes: number;
  status: 'SCHEDULED' | 'IN_PROGRESS' | 'PENDING' | 'COMPLETED' | 'CANCELLED';
  studentId?: string;
}

const DAYS_OF_WEEK = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

const STATUS_CONFIG: Record<string, { label: string; badge: string; icon: React.ReactNode }> = {
  IN_PROGRESS: { label: 'In Progress', badge: 'bg-emerald-600/20 text-emerald-300', icon: <CheckCircle2 className="w-4 h-4 text-emerald-400" /> },
  SCHEDULED: { label: 'Scheduled', badge: 'bg-brand-600/20 text-brand-300', icon: <Calendar className="w-4 h-4 text-brand-400" /> },
  PENDING: { label: 'Pending', badge: 'bg-amber-600/20 text-amber-300', icon: <AlertCircle className="w-4 h-4 text-amber-400" /> },
  COMPLETED: { label: 'Completed', badge: 'bg-white/10 text-white/50', icon: <CheckCircle2 className="w-4 h-4 text-white/40" /> },
  CANCELLED: { label: 'Cancelled', badge: 'bg-red-600/20 text-red-300', icon: <XCircle className="w-4 h-4 text-red-400" /> },
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
  const queryClient = useQueryClient();
  const [availability, setAvailability] = useState<Record<string, boolean>>({
    Mon: true, Tue: false, Wed: true, Thu: true, Fri: true, Sat: false, Sun: false,
  });

  // Fetch mentor profile by listing all and finding by userId
  const {
    data: mentorProfile,
    isLoading: profileLoading,
    isError: profileError,
    refetch: refetchProfile,
  } = useQuery<{ id: string; averageRating: number; totalSessions: number } | null>({
    queryKey: ['mentor-profile', user?.id],
    queryFn: async () => {
      const res = await api.get('/api/v1/mentors');
      const raw = res.data;
      const profiles: Array<{ id: string; userId: string; averageRating: number; totalSessions: number }> = Array.isArray(raw) ? raw : (raw.content ?? []);
      return profiles.find((p) => p.userId === user?.id) ?? null;
    },
    retry: false,
  });

  const profileId = mentorProfile?.id;

  const {
    data: allSessions,
    isLoading: sessionsLoading,
    isError: sessionsError,
    refetch: refetchSessions,
  } = useQuery<UpcomingSession[]>({
    queryKey: ['mentor-upcoming-sessions', profileId],
    queryFn: async () => {
      const res = await api.get(`/api/v1/mentor-sessions?mentorId=${profileId}`);
      const d = res.data;
      return Array.isArray(d) ? d : (d.content ?? []);
    },
    enabled: !!profileId,
    retry: false,
  });

  // Accept/Decline mutations for pending sessions in the upcoming table
  const updateStatusMutation = useMutation({
    mutationFn: async ({ sessionId, status }: { sessionId: string; status: string }) => {
      await api.patch(`/api/v1/mentor-sessions/${sessionId}/status`, { status });
    },
    onSuccess: (_data, variables) => {
      const label = variables.status === 'CONFIRMED' ? 'accepted' : 'declined';
      toast.success(`Booking ${label}!`);
      queryClient.invalidateQueries({ queryKey: ['mentor-upcoming-sessions', profileId] });
    },
    onError: () => {
      toast.error('Failed to update session status. Please try again.');
    },
  });

  const isLoading = profileLoading || (!!profileId && sessionsLoading);
  const hasError = profileError || sessionsError;

  const sessions = allSessions ?? [];

  // Compute stats from live data only
  const stats: MentorStats = {
    sessionsThisWeek: sessions.filter((s) => {
      const d = new Date(s.scheduledAt);
      const now = new Date();
      const startOfWeek = new Date(now);
      startOfWeek.setDate(now.getDate() - now.getDay());
      return d >= startOfWeek;
    }).length,
    avgRating: mentorProfile?.averageRating ?? null,
    pendingBookings: sessions.filter((s) => s.status === 'PENDING').length,
    totalStudentsHelped: (() => {
      const ids = sessions.map((s) => s.studentId).filter(Boolean);
      return ids.length > 0 ? new Set(ids).size : (mentorProfile?.totalSessions ?? 0);
    })(),
  };

  const upcomingSessions = sessions
    .filter((s) => new Date(s.scheduledAt) >= new Date() || s.status === 'IN_PROGRESS')
    .sort((a, b) => new Date(a.scheduledAt).getTime() - new Date(b.scheduledAt).getTime());

  // Save availability mutation
  const saveAvailabilityMutation = useMutation({
    mutationFn: async () => {
      if (!profileId) throw new Error('Mentor profile not loaded');
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

  // Loading state
  if (isLoading) {
    return (
      <div className="min-h-screen p-6 flex items-center justify-center">
        <div className="flex flex-col items-center gap-4 text-white/50">
          <Loader2 className="w-10 h-10 animate-spin" />
          <p>Loading mentor dashboard…</p>
        </div>
      </div>
    );
  }

  // Error state
  if (hasError) {
    return (
      <div className="min-h-screen p-6 flex items-center justify-center">
        <div className="glass rounded-2xl p-10 flex flex-col items-center gap-4 text-white/50">
          <AlertCircle className="w-12 h-12 text-red-400 opacity-70" />
          <p className="text-white/70 font-medium">Failed to load dashboard data</p>
          <button
            onClick={() => { refetchProfile(); refetchSessions(); }}
            className="flex items-center gap-2 px-4 py-2 bg-brand-600 hover:bg-brand-500 text-white rounded-xl text-sm font-medium transition-all"
          >
            <RefreshCw className="w-4 h-4" /> Retry
          </button>
        </div>
      </div>
    );
  }

  // No mentor profile configured
  if (mentorProfile === null) {
    return (
      <div className="min-h-screen p-6 flex items-center justify-center">
        <div className="glass rounded-2xl p-10 flex flex-col items-center gap-4 text-white/50 max-w-md text-center">
          <AlertCircle className="w-12 h-12 text-amber-400 opacity-70" />
          <p className="text-white font-semibold text-lg">Profile Not Configured</p>
          <p className="text-white/50 text-sm">
            No mentor profile was found for your account. Please contact an administrator to set up your mentor profile.
          </p>
        </div>
      </div>
    );
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
            <p className="text-white/50 mt-1">You have <span className="text-amber-400 font-semibold">{stats.pendingBookings} pending</span> booking requests.</p>
          </div>
        </div>
      </motion.div>

      {/* Stats */}
      <div className="grid grid-cols-2 xl:grid-cols-4 gap-5">
        <StatCard
          icon={<Clock className="w-5 h-5 text-brand-400" />}
          label="Sessions This Week"
          value={stats.sessionsThisWeek}
          color="bg-brand-600/20"
        />
        <StatCard
          icon={<Star className="w-5 h-5 text-amber-400" />}
          label="Average Rating"
          value={stats.avgRating !== null ? stats.avgRating : 'N/A'}
          sub="Based on all sessions"
          color="bg-amber-600/20"
        />
        <StatCard
          icon={<AlertCircle className="w-5 h-5 text-orange-400" />}
          label="Pending Bookings"
          value={stats.pendingBookings}
          sub="Awaiting confirmation"
          color="bg-orange-600/20"
        />
        <StatCard
          icon={<Users className="w-5 h-5 text-emerald-400" />}
          label="Students Helped"
          value={stats.totalStudentsHelped}
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

        {upcomingSessions.length === 0 ? (
          <div className="py-10 flex flex-col items-center gap-3 text-white/40">
            <Calendar className="w-10 h-10 opacity-30" />
            <p className="text-sm">No upcoming sessions</p>
          </div>
        ) : (
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
                {upcomingSessions.map((session, i) => {
                  const config = STATUS_CONFIG[session.status] ?? STATUS_CONFIG['SCHEDULED'];
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
                              onClick={() => updateStatusMutation.mutate({ sessionId: session.id, status: 'CONFIRMED' })}
                              disabled={updateStatusMutation.isPending}
                              className="p-1.5 rounded-lg bg-emerald-600/20 text-emerald-400 hover:bg-emerald-600/30 transition-colors disabled:opacity-50"
                            >
                              <CheckCircle2 className="w-4 h-4" />
                            </button>
                            <button
                              onClick={() => updateStatusMutation.mutate({ sessionId: session.id, status: 'CANCELLED' })}
                              disabled={updateStatusMutation.isPending}
                              className="p-1.5 rounded-lg bg-red-600/20 text-red-400 hover:bg-red-600/30 transition-colors disabled:opacity-50"
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
        )}
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
