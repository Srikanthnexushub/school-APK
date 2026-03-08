import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Video, X, MessageSquare, Calendar, Clock, Check,
} from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { cn } from '../../lib/utils';
import { Avatar } from '../../components/ui/Avatar';
import { StarRating } from '../../components/ui/StarRating';
import { Modal } from '../../components/ui/Modal';

type SessionStatus = 'UPCOMING' | 'COMPLETED' | 'CANCELLED';

interface MentorSession {
  id: string;
  mentorId: string;
  mentorName: string;
  scheduledAt: string;
  durationMinutes: number;
  status: SessionStatus;
  subject?: string;
  zoomLink?: string;
  feedback?: { rating: number; comment: string };
}

const MOCK_SESSIONS: MentorSession[] = [
  { id: 's1', mentorId: 'm1', mentorName: 'Dr. Priya Sharma', scheduledAt: new Date(Date.now() + 86400000 * 2).toISOString(), durationMinutes: 60, status: 'UPCOMING', subject: 'Differential Equations', zoomLink: 'https://zoom.us/j/123456789' },
  { id: 's2', mentorId: 'm3', mentorName: 'Anika Mehta', scheduledAt: new Date(Date.now() + 86400000 * 5).toISOString(), durationMinutes: 90, status: 'UPCOMING', subject: 'Data Structures', zoomLink: 'https://zoom.us/j/987654321' },
  { id: 's3', mentorId: 'm1', mentorName: 'Dr. Priya Sharma', scheduledAt: new Date(Date.now() - 86400000 * 3).toISOString(), durationMinutes: 60, status: 'COMPLETED', subject: 'Integration Techniques', feedback: { rating: 5, comment: 'Excellent session!' } },
  { id: 's4', mentorId: 'm2', mentorName: 'Rahul Verma', scheduledAt: new Date(Date.now() - 86400000 * 10).toISOString(), durationMinutes: 60, status: 'COMPLETED', subject: 'Organic Chemistry' },
  { id: 's5', mentorId: 'm4', mentorName: 'Suresh Iyer', scheduledAt: new Date(Date.now() - 86400000 * 7).toISOString(), durationMinutes: 30, status: 'CANCELLED', subject: 'Algebra' },
];

const STATUS_TABS: SessionStatus[] = ['UPCOMING', 'COMPLETED', 'CANCELLED'];
const STATUS_LABELS: Record<SessionStatus, string> = { UPCOMING: 'Upcoming', COMPLETED: 'Completed', CANCELLED: 'Cancelled' };
const STATUS_BADGE: Record<SessionStatus, string> = {
  UPCOMING: 'bg-brand-600/20 text-brand-300',
  COMPLETED: 'bg-emerald-600/20 text-emerald-300',
  CANCELLED: 'bg-red-600/20 text-red-300',
};

interface FeedbackModalProps {
  session: MentorSession | null;
  onClose: () => void;
}

function FeedbackModal({ session, onClose }: FeedbackModalProps) {
  const queryClient = useQueryClient();
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState('');

  const feedbackMutation = useMutation({
    mutationFn: () =>
      api.patch(`/api/v1/sessions/${session?.id}/complete`, { rating, feedback: comment }),
    onSuccess: () => {
      toast.success('Feedback submitted! Thank you.');
      queryClient.invalidateQueries({ queryKey: ['my-sessions'] });
      onClose();
    },
    onError: () => {
      toast.error('Failed to submit feedback.');
    },
  });

  if (!session) return null;

  return (
    <Modal isOpen={!!session} onClose={onClose} title="Leave Feedback">
      <div className="space-y-5">
        <div className="flex items-center gap-3 p-4 glass rounded-xl">
          <Avatar name={session.mentorName} size="md" />
          <div>
            <p className="font-semibold text-white">{session.mentorName}</p>
            <p className="text-white/50 text-sm">{session.subject}</p>
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-white/70 mb-3">Your rating</label>
          <StarRating value={rating} onChange={setRating} size="lg" />
        </div>

        <div>
          <label className="block text-sm font-medium text-white/70 mb-2">Comment (optional)</label>
          <textarea
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder="Share your experience with this mentor…"
            rows={3}
            className="input w-full resize-none"
          />
        </div>

        <button
          onClick={() => feedbackMutation.mutate()}
          disabled={rating === 0 || feedbackMutation.isPending}
          className="btn-primary w-full py-3 flex items-center justify-center gap-2"
        >
          {feedbackMutation.isPending ? (
            <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
          ) : (
            <Check className="w-4 h-4" />
          )}
          Submit Feedback
        </button>
      </div>
    </Modal>
  );
}

interface SessionCardProps {
  session: MentorSession;
  index: number;
  onFeedback: (session: MentorSession) => void;
  onCancel: (sessionId: string) => void;
}

function SessionCard({ session, index, onFeedback, onCancel }: SessionCardProps) {
  const scheduledDate = new Date(session.scheduledAt);
  const isToday = new Date().toDateString() === scheduledDate.toDateString();

  return (
    <motion.div
      className="card"
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.06 }}
    >
      <div className="flex items-start justify-between gap-4 mb-4">
        <div className="flex items-center gap-3">
          <Avatar name={session.mentorName} size="lg" />
          <div>
            <h3 className="font-semibold text-white">{session.mentorName}</h3>
            {session.subject && (
              <p className="text-white/50 text-sm">{session.subject}</p>
            )}
          </div>
        </div>
        <span className={cn('badge flex-shrink-0', STATUS_BADGE[session.status])}>
          {STATUS_LABELS[session.status]}
        </span>
      </div>

      <div className="flex flex-wrap gap-4 text-sm text-white/50 mb-4">
        <span className="flex items-center gap-1.5">
          <Calendar className="w-4 h-4" />
          {isToday ? 'Today' : scheduledDate.toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short' })}
        </span>
        <span className="flex items-center gap-1.5">
          <Clock className="w-4 h-4" />
          {scheduledDate.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })}
        </span>
        <span className="flex items-center gap-1.5">
          <Clock className="w-4 h-4" />
          {session.durationMinutes} min
        </span>
      </div>

      {session.status === 'COMPLETED' && session.feedback && (
        <div className="mb-4 p-3 glass rounded-xl">
          <div className="flex items-center gap-2 mb-1">
            <StarRating value={session.feedback.rating} size="sm" />
            <span className="text-white/40 text-xs">Your rating</span>
          </div>
          <p className="text-white/50 text-sm italic">"{session.feedback.comment}"</p>
        </div>
      )}

      {session.status === 'UPCOMING' && (
        <div className="flex gap-3">
          <a
            href={session.zoomLink ?? '#'}
            target="_blank"
            rel="noreferrer"
            className="btn-primary flex-1 flex items-center justify-center gap-2 text-sm"
          >
            <Video className="w-4 h-4" /> Join Session
          </a>
          <button
            onClick={() => onCancel(session.id)}
            className="flex items-center gap-2 px-4 py-2 rounded-xl glass border border-red-500/20 text-red-400 hover:bg-red-600/10 transition-all text-sm"
          >
            <X className="w-4 h-4" /> Cancel
          </button>
        </div>
      )}

      {session.status === 'COMPLETED' && !session.feedback && (
        <button
          onClick={() => onFeedback(session)}
          className="w-full flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl glass border border-white/10 text-white/70 hover:text-white hover:border-white/20 transition-all text-sm"
        >
          <MessageSquare className="w-4 h-4" /> Leave Feedback
        </button>
      )}
    </motion.div>
  );
}

export default function MySessionsPage() {
  const [activeTab, setActiveTab] = useState<SessionStatus>('UPCOMING');
  const [feedbackSession, setFeedbackSession] = useState<MentorSession | null>(null);
  const queryClient = useQueryClient();

  const { data: sessions } = useQuery<MentorSession[]>({
    queryKey: ['my-sessions'],
    queryFn: async () => {
      const res = await api.get('/api/v1/sessions/my');
      return res.data;
    },
    retry: false,
    placeholderData: MOCK_SESSIONS,
  });

  const cancelMutation = useMutation({
    mutationFn: (sessionId: string) =>
      api.patch(`/api/v1/sessions/${sessionId}/cancel`),
    onSuccess: () => {
      toast.success('Session cancelled.');
      queryClient.invalidateQueries({ queryKey: ['my-sessions'] });
    },
    onError: () => {
      toast.error('Failed to cancel session.');
    },
  });

  const all = sessions ?? MOCK_SESSIONS;
  const filtered = all.filter((s) => s.status === activeTab);

  const counts: Record<SessionStatus, number> = {
    UPCOMING: all.filter((s) => s.status === 'UPCOMING').length,
    COMPLETED: all.filter((s) => s.status === 'COMPLETED').length,
    CANCELLED: all.filter((s) => s.status === 'CANCELLED').length,
  };

  return (
    <div className="min-h-screen p-6 space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-white mb-1">My Sessions</h1>
        <p className="text-white/50">Track your mentoring sessions and leave feedback.</p>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 p-1 glass rounded-xl w-fit">
        {STATUS_TABS.map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={cn(
              'flex items-center gap-2 px-5 py-2 rounded-lg text-sm font-medium transition-all',
              activeTab === tab ? 'bg-brand-600 text-white' : 'text-white/50 hover:text-white'
            )}
          >
            {STATUS_LABELS[tab]}
            <span className={cn(
              'text-xs px-1.5 py-0.5 rounded-full font-bold',
              activeTab === tab ? 'bg-white/20' : 'bg-white/8'
            )}>
              {counts[tab]}
            </span>
          </button>
        ))}
      </div>

      {/* Session list */}
      <AnimatePresence mode="wait">
        <motion.div
          key={activeTab}
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -8 }}
          transition={{ duration: 0.2 }}
          className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5"
        >
          {filtered.map((session, i) => (
            <SessionCard
              key={session.id}
              session={session}
              index={i}
              onFeedback={setFeedbackSession}
              onCancel={(id) => cancelMutation.mutate(id)}
            />
          ))}
          {filtered.length === 0 && (
            <div className="col-span-full text-center py-16 text-white/30">
              <Calendar className="w-10 h-10 mx-auto mb-3 opacity-40" />
              <p className="text-lg">No {STATUS_LABELS[activeTab].toLowerCase()} sessions.</p>
              {activeTab === 'UPCOMING' && (
                <p className="text-sm mt-1">Browse mentors and book your first session.</p>
              )}
            </div>
          )}
        </motion.div>
      </AnimatePresence>

      <FeedbackModal session={feedbackSession} onClose={() => setFeedbackSession(null)} />
    </div>
  );
}
