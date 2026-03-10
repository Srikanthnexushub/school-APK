import { useState } from 'react';
import { motion } from 'framer-motion';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Calendar, Video, CheckCircle2, XCircle, Clock, Filter,
  AlertCircle, BookOpen, Loader2, RefreshCw,
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

type FilterTab = 'ALL' | 'UPCOMING' | 'COMPLETED' | 'PENDING';

const STATUS_BADGE: Record<MentorSession['status'], { variant: 'success' | 'info' | 'warning' | 'default' | 'danger'; label: string }> = {
  IN_PROGRESS: { variant: 'success',  label: 'In Progress' },
  SCHEDULED:   { variant: 'info',     label: 'Scheduled'   },
  PENDING:     { variant: 'warning',  label: 'Pending'     },
  COMPLETED:   { variant: 'default',  label: 'Completed'   },
  CANCELLED:   { variant: 'danger',   label: 'Cancelled'   },
};

export default function MentorPortalSessionsPage() {
  const user = useAuthStore((s) => s.user);
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState<FilterTab>('ALL');

  const { data: mentorProfile, isLoading: profileLoading, isError: profileError, refetch: refetchProfile } = useQuery<{ id: string } | null>({
    queryKey: ['mentor-profile', user?.id],
    queryFn: async () => {
      const res = await api.get('/api/v1/mentors');
      const raw = res.data;
      const profiles: Array<{ id: string; userId: string }> = Array.isArray(raw) ? raw : (raw.content ?? []);
      return profiles.find((p) => p.userId === user?.id) ?? null;
    },
    retry: false,
  });

  const profileId = mentorProfile?.id;

  const {
    data: apiSessions,
    isLoading: sessionsLoading,
    isError: sessionsError,
    refetch: refetchSessions,
  } = useQuery<MentorSession[]>({
    queryKey: ['mentor-sessions-all', profileId],
    queryFn: async () => {
      const res = await api.get(`/api/v1/mentor-sessions?mentorId=${profileId}`);
      const d = res.data;
      return Array.isArray(d) ? d : (d.content ?? []);
    },
    enabled: !!profileId,
    retry: false,
  });

  const updateStatusMutation = useMutation({
    mutationFn: async ({ sessionId, status }: { sessionId: string; status: string }) => {
      await api.patch(`/api/v1/mentor-sessions/${sessionId}/status`, { status });
    },
    onSuccess: (_data, variables) => {
      const label =
        variables.status === 'CONFIRMED' ? 'accepted' :
        variables.status === 'CANCELLED' ? 'declined' :
        'completed';
      toast.success(`Session ${label}!`);
      queryClient.invalidateQueries({ queryKey: ['mentor-sessions-all', profileId] });
    },
    onError: () => {
      toast.error('Failed to update session status. Please try again.');
    },
  });

  const isLoading = profileLoading || (!!profileId && sessionsLoading);
  const hasError = profileError || sessionsError;

  const sessions: MentorSession[] = apiSessions ?? [];

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

  // Loading state
  if (isLoading) {
    return (
      <div className="p-6 flex items-center justify-center min-h-[400px]">
        <div className="flex flex-col items-center gap-4 text-white/50">
          <Loader2 className="w-10 h-10 animate-spin" />
          <p>Loading sessions…</p>
        </div>
      </div>
    );
  }

  // Error state
  if (hasError) {
    return (
      <div className="p-6 flex items-center justify-center min-h-[400px]">
        <div className="glass rounded-2xl p-10 flex flex-col items-center gap-4 text-white/50">
          <AlertCircle className="w-12 h-12 text-red-400 opacity-70" />
          <p className="text-white/70 font-medium">Failed to load sessions</p>
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
      <div className="p-6 flex items-center justify-center min-h-[400px]">
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
          <p>No sessions found</p>
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
                        onClick={() => updateStatusMutation.mutate({ sessionId: session.id, status: 'CONFIRMED' })}
                        disabled={updateStatusMutation.isPending}
                        className="p-2 rounded-xl bg-emerald-600/20 text-emerald-400 hover:bg-emerald-600/30 transition-colors disabled:opacity-50"
                        title="Accept"
                      >
                        <CheckCircle2 className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => updateStatusMutation.mutate({ sessionId: session.id, status: 'CANCELLED' })}
                        disabled={updateStatusMutation.isPending}
                        className="p-2 rounded-xl bg-red-600/20 text-red-400 hover:bg-red-600/30 transition-colors disabled:opacity-50"
                        title="Decline"
                      >
                        <XCircle className="w-4 h-4" />
                      </button>
                    </>
                  )}
                  {session.status === 'SCHEDULED' && !isPast && (
                    <button
                      onClick={() => updateStatusMutation.mutate({ sessionId: session.id, status: 'COMPLETED' })}
                      disabled={updateStatusMutation.isPending}
                      className="p-2 rounded-xl bg-brand-600/20 text-brand-400 hover:bg-brand-600/30 transition-colors disabled:opacity-50 text-xs"
                      title="Mark Complete"
                    >
                      <CheckCircle2 className="w-4 h-4" />
                    </button>
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
                  {session.status === 'SCHEDULED' && isPast && (
                    <span className="text-xs text-white/30 flex items-center gap-1">
                      <AlertCircle className="w-3.5 h-3.5" /> Past
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
