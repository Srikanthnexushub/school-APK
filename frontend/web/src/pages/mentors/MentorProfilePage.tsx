import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import { useQuery, useMutation } from '@tanstack/react-query';
import { Star, Clock, Users, Check, Calendar } from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { cn } from '../../lib/utils';
import { Avatar } from '../../components/ui/Avatar';
import { StarRating } from '../../components/ui/StarRating';
import { Modal } from '../../components/ui/Modal';

interface MentorProfileResponse {
  id: string;
  userId: string;
  fullName: string;
  email: string;
  bio: string;
  specializations: string;
  yearsOfExperience: number;
  hourlyRate: number;
  isAvailable: boolean;
  averageRating: number;
  totalSessions: number;
  createdAt: string;
  updatedAt: string;
}

interface MentorSession {
  id: string;
  mentorId: string;
  mentorName: string;
  studentId: string;
  scheduledAt: string;
  durationMinutes: number;
  sessionMode: string;
  status: string;
  meetingLink?: string;
  notes?: string;
  createdAt: string;
  completedAt?: string;
}

function getNextSevenDays(): Date[] {
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date();
    d.setDate(d.getDate() + i + 1);
    return d;
  });
}

const TIME_SLOTS = ['10:00', '10:30', '11:00', '11:30', '14:00', '14:30', '15:00', '16:00', '16:30', '17:00'];
const DURATIONS = [30, 60, 90];
const SESSION_MODES = ['VIDEO', 'AUDIO', 'CHAT'];

export default function MentorProfilePage() {
  const { mentorId } = useParams<{ mentorId: string }>();
  const user = useAuthStore((s) => s.user);
  const [bookingOpen, setBookingOpen] = useState(false);
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);
  const [selectedTime, setSelectedTime] = useState<string | null>(null);
  const [duration, setDuration] = useState(60);
  const [sessionMode, setSessionMode] = useState('VIDEO');
  const [notes, setNotes] = useState('');
  const days = getNextSevenDays();

  const { data: mentor, isLoading: mentorLoading } = useQuery<MentorProfileResponse>({
    queryKey: ['mentor', mentorId],
    queryFn: async () => {
      const res = await api.get(`/api/v1/mentors/${mentorId}`);
      return res.data;
    },
    retry: false,
  });

  const { data: allSessions } = useQuery<MentorSession[]>({
    queryKey: ['student-sessions', user?.id, mentorId],
    queryFn: async () => {
      const res = await api.get(`/api/v1/mentor-sessions?studentId=${user?.id}`);
      return res.data;
    },
    enabled: !!user?.id,
    retry: false,
    placeholderData: [],
  });

  const pastSessions = (allSessions ?? []).filter(
    (s) => s.mentorId === mentorId
  );

  const bookMutation = useMutation({
    mutationFn: () => {
      if (!selectedDate || !selectedTime || !mentor) throw new Error('Incomplete');
      const [h, min] = selectedTime.split(':').map(Number);
      const scheduledAt = new Date(selectedDate);
      scheduledAt.setHours(h, min, 0, 0);
      return api.post('/api/v1/mentor-sessions', {
        mentorId: mentor.id,
        studentId: user?.id,
        scheduledAt: scheduledAt.toISOString(),
        durationMinutes: duration,
        sessionMode,
        notes,
      });
    },
    onSuccess: () => {
      toast.success('Session booked successfully!');
      setBookingOpen(false);
    },
    onError: () => {
      toast.error('Booking failed. Please try again.');
    },
  });

  if (mentorLoading) {
    return (
      <div className="min-h-screen p-6 max-w-4xl mx-auto flex items-center justify-center">
        <div className="w-8 h-8 border-2 border-white/20 border-t-white/70 rounded-full animate-spin" />
      </div>
    );
  }

  if (!mentor) {
    return (
      <div className="min-h-screen p-6 max-w-4xl mx-auto flex items-center justify-center">
        <p className="text-white/40">Mentor not found.</p>
      </div>
    );
  }

  const specializationList = mentor.specializations
    ? mentor.specializations.split(',').map((s) => s.trim()).filter(Boolean)
    : [];

  return (
    <div className="min-h-screen p-6 max-w-4xl mx-auto space-y-6">
      {/* Hero */}
      <motion.div
        initial={{ opacity: 0, y: -16 }}
        animate={{ opacity: 1, y: 0 }}
        className="card"
      >
        <div className="flex flex-col md:flex-row items-start gap-6">
          <Avatar name={mentor.fullName} size="xl" />
          <div className="flex-1 min-w-0">
            <h1 className="text-2xl font-bold text-white mb-1">{mentor.fullName}</h1>
            <div className="flex flex-wrap items-center gap-4 text-sm text-white/50 mb-3">
              <span className="flex items-center gap-1.5">
                <Star className="w-4 h-4 fill-amber-400 text-amber-400" />
                <span className="text-white font-semibold">{mentor.averageRating?.toFixed(1) ?? '—'}</span>
              </span>
              <span className="flex items-center gap-1.5">
                <Users className="w-4 h-4" />
                {mentor.totalSessions} sessions
              </span>
              <span className="flex items-center gap-1.5">
                <Clock className="w-4 h-4" />
                {mentor.yearsOfExperience} yrs experience
              </span>
              {mentor.hourlyRate > 0 && (
                <span className="flex items-center gap-1.5">
                  ₹{mentor.hourlyRate}/hr
                </span>
              )}
              {mentor.isAvailable && (
                <span className="flex items-center gap-1.5 text-emerald-400">
                  <span className="w-2 h-2 rounded-full bg-emerald-400 inline-block" />
                  Available
                </span>
              )}
            </div>
            <div className="flex flex-wrap gap-2">
              {specializationList.map((e) => (
                <span key={e} className="badge bg-brand-600/20 text-brand-300 border border-brand-600/30">{e}</span>
              ))}
            </div>
          </div>
          <button
            onClick={() => setBookingOpen(true)}
            className="btn-primary flex items-center gap-2 flex-shrink-0"
          >
            <Calendar className="w-4 h-4" /> Book New Session
          </button>
        </div>
      </motion.div>

      {/* Bio */}
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className="card"
      >
        <h2 className="text-lg font-semibold text-white mb-3">About</h2>
        <p className="text-white/60 leading-relaxed whitespace-pre-line text-sm">{mentor.bio}</p>
      </motion.div>

      {/* Past sessions */}
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.15 }}
        className="card"
      >
        <h2 className="text-lg font-semibold text-white mb-4">Session History</h2>
        {pastSessions.length > 0 ? (
          <div className="space-y-3">
            {pastSessions.map((session, i) => (
              <motion.div
                key={session.id}
                initial={{ opacity: 0, x: -12 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.2 + i * 0.06 }}
                className="flex items-center justify-between p-4 glass rounded-xl"
              >
                <div>
                  <p className="text-white font-medium text-sm">
                    {session.sessionMode}{session.notes ? ` — ${session.notes}` : ''}
                  </p>
                  <p className="text-white/40 text-xs mt-0.5">
                    {new Date(session.scheduledAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}
                    {' · '}
                    <span className="capitalize">{session.status.toLowerCase()}</span>
                  </p>
                </div>
                <span className="badge bg-white/8 text-white/50">{session.durationMinutes} min</span>
              </motion.div>
            ))}
          </div>
        ) : (
          <p className="text-white/30 text-sm">No past sessions with this mentor.</p>
        )}
      </motion.div>

      {/* Reviews */}
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.2 }}
        className="card"
      >
        <h2 className="text-lg font-semibold text-white mb-1">Student Reviews</h2>
        <p className="text-white/40 text-sm mb-5">0 reviews</p>
        <p className="text-white/30 text-sm">No reviews yet.</p>
      </motion.div>

      {/* Booking Modal */}
      <Modal isOpen={bookingOpen} onClose={() => setBookingOpen(false)} title="Book a Session" maxWidth="max-w-xl">
        <div className="space-y-5">
          <div className="flex items-center gap-3 p-4 glass rounded-xl">
            <Avatar name={mentor.fullName} size="md" />
            <div>
              <p className="font-semibold text-white">{mentor.fullName}</p>
              <StarRating value={mentor.averageRating ?? 0} size="sm" />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-white/70 mb-2">Select Date</label>
            <div className="flex gap-2 overflow-x-auto pb-1">
              {days.map((day) => {
                const isSelected = selectedDate?.toDateString() === day.toDateString();
                return (
                  <button
                    key={day.toISOString()}
                    onClick={() => setSelectedDate(day)}
                    className={cn(
                      'flex-shrink-0 flex flex-col items-center px-3 py-2 rounded-xl border transition-all text-xs',
                      isSelected ? 'bg-brand-600 border-brand-500 text-white' : 'glass border-white/10 text-white/60 hover:border-white/20'
                    )}
                  >
                    <span>{day.toLocaleDateString('en-IN', { weekday: 'short' })}</span>
                    <span className="font-bold text-base">{day.getDate()}</span>
                    <span>{day.toLocaleDateString('en-IN', { month: 'short' })}</span>
                  </button>
                );
              })}
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-white/70 mb-2">Select Time</label>
            <div className="grid grid-cols-5 gap-2">
              {TIME_SLOTS.map((slot) => (
                <button
                  key={slot}
                  onClick={() => setSelectedTime(slot)}
                  className={cn(
                    'py-1.5 rounded-lg border text-xs font-medium transition-all',
                    selectedTime === slot ? 'bg-brand-600 border-brand-500 text-white' : 'glass border-white/10 text-white/60 hover:border-white/20'
                  )}
                >
                  {slot}
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-white/70 mb-2">Duration</label>
            <div className="flex gap-2">
              {DURATIONS.map((d) => (
                <button
                  key={d}
                  onClick={() => setDuration(d)}
                  className={cn(
                    'flex-1 py-2 rounded-xl border text-sm font-medium transition-all',
                    duration === d ? 'bg-brand-600 border-brand-500 text-white' : 'glass border-white/10 text-white/60 hover:border-white/20'
                  )}
                >
                  {d} min
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-white/70 mb-2">Session Mode</label>
            <div className="flex gap-2">
              {SESSION_MODES.map((mode) => (
                <button
                  key={mode}
                  onClick={() => setSessionMode(mode)}
                  className={cn(
                    'flex-1 py-2 rounded-xl border text-sm font-medium transition-all',
                    sessionMode === mode ? 'bg-brand-600 border-brand-500 text-white' : 'glass border-white/10 text-white/60 hover:border-white/20'
                  )}
                >
                  {mode.charAt(0) + mode.slice(1).toLowerCase()}
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-white/70 mb-2">Notes (optional)</label>
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Topics to cover, questions to ask…"
              rows={2}
              className="input w-full resize-none"
            />
          </div>

          <button
            onClick={() => bookMutation.mutate()}
            disabled={!selectedDate || !selectedTime || bookMutation.isPending}
            className="btn-primary w-full py-3 flex items-center justify-center gap-2"
          >
            {bookMutation.isPending ? (
              <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            ) : (
              <Check className="w-4 h-4" />
            )}
            Confirm Booking
          </button>
        </div>
      </Modal>
    </div>
  );
}
