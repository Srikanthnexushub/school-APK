import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import { useQuery, useMutation } from '@tanstack/react-query';
import { Star, Clock, Users, MessageSquare, Check, Calendar } from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { cn } from '../../lib/utils';
import { Avatar } from '../../components/ui/Avatar';
import { StarRating } from '../../components/ui/StarRating';
import { Modal } from '../../components/ui/Modal';

interface MentorDetail {
  id: string;
  name: string;
  expertise: string[];
  rating: number;
  totalSessions: number;
  bio: string;
  availability: string[];
  avatarUrl?: string;
  responseRate: number;
}

interface Review {
  id: string;
  studentName: string;
  rating: number;
  comment: string;
  date: string;
}

interface PastSession {
  id: string;
  date: string;
  subject: string;
  duration: number;
}

const MOCK_MENTOR: MentorDetail = {
  id: 'm1',
  name: 'Dr. Priya Sharma',
  expertise: ['Mathematics', 'Physics', 'JEE Advanced', 'Calculus'],
  rating: 4.9,
  totalSessions: 127,
  bio: "I hold a PhD in Applied Mathematics from IIT Delhi and have been teaching for over 8 years. My passion is making complex topics like calculus, mechanics, and probability feel intuitive and approachable. I specialise in JEE Advanced and NEET preparation.\n\nI believe every student can excel with the right approach — and I tailor each session to the individual's current understanding and learning style.",
  availability: ['Mon', 'Wed', 'Fri'],
  responseRate: 98,
};

const MOCK_REVIEWS: Review[] = [
  { id: 'r1', studentName: 'A. Kumar', rating: 5, comment: "Priya ma'am explained integration by parts in a way that finally made sense. Sessions are very organised and efficient.", date: '2026-02-15' },
  { id: 'r2', studentName: 'R. Singh', rating: 5, comment: 'Best tutor for JEE Maths. Very patient, always available on WhatsApp between sessions too.', date: '2026-01-28' },
  { id: 'r3', studentName: 'S. Mehta', rating: 4, comment: 'Great mentor. Covers a lot of ground in each session. Could slow down a bit on the harder problems.', date: '2025-12-10' },
  { id: 'r4', studentName: 'K. Iyer', rating: 5, comment: "Got into IIT Bombay! Dr. Sharma's sessions on mechanics were the turning point. Highly recommended.", date: '2025-11-22' },
];

const MOCK_PAST_SESSIONS: PastSession[] = [
  { id: 'ps1', date: '2026-02-10', subject: 'Differential Equations', duration: 60 },
  { id: 'ps2', date: '2026-01-27', subject: 'Mechanics — Rotational Motion', duration: 90 },
  { id: 'ps3', date: '2026-01-15', subject: 'Integration Techniques', duration: 60 },
];

function getNextSevenDays(): Date[] {
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date();
    d.setDate(d.getDate() + i + 1);
    return d;
  });
}

const TIME_SLOTS = ['10:00', '10:30', '11:00', '11:30', '14:00', '14:30', '15:00', '16:00', '16:30', '17:00'];
const DURATIONS = [30, 60, 90];

export default function MentorProfilePage() {
  const { mentorId } = useParams<{ mentorId: string }>();
  const user = useAuthStore((s) => s.user);
  const [bookingOpen, setBookingOpen] = useState(false);
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);
  const [selectedTime, setSelectedTime] = useState<string | null>(null);
  const [duration, setDuration] = useState(60);
  const [notes, setNotes] = useState('');
  const days = getNextSevenDays();

  const { data: mentor } = useQuery<MentorDetail>({
    queryKey: ['mentor', mentorId],
    queryFn: async () => {
      const res = await api.get(`/api/v1/mentors/${mentorId}`);
      return res.data;
    },
    retry: false,
    placeholderData: MOCK_MENTOR,
  });

  const { data: reviews } = useQuery<Review[]>({
    queryKey: ['mentor-reviews', mentorId],
    queryFn: async () => {
      const res = await api.get(`/api/v1/mentors/${mentorId}/reviews`);
      return res.data;
    },
    retry: false,
    placeholderData: MOCK_REVIEWS,
  });

  const bookMutation = useMutation({
    mutationFn: () => {
      if (!selectedDate || !selectedTime || !mentor) throw new Error('Incomplete');
      const [h, min] = selectedTime.split(':').map(Number);
      const scheduledAt = new Date(selectedDate);
      scheduledAt.setHours(h, min, 0, 0);
      return api.post('/api/v1/sessions', {
        mentorId: mentor.id,
        studentId: user?.id,
        scheduledAt: scheduledAt.toISOString(),
        durationMinutes: duration,
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

  const m = mentor ?? MOCK_MENTOR;

  return (
    <div className="min-h-screen p-6 max-w-4xl mx-auto space-y-6">
      {/* Hero */}
      <motion.div
        initial={{ opacity: 0, y: -16 }}
        animate={{ opacity: 1, y: 0 }}
        className="card"
      >
        <div className="flex flex-col md:flex-row items-start gap-6">
          <Avatar name={m.name} size="xl" imageUrl={m.avatarUrl} />
          <div className="flex-1 min-w-0">
            <h1 className="text-2xl font-bold text-white mb-1">{m.name}</h1>
            <div className="flex flex-wrap items-center gap-4 text-sm text-white/50 mb-3">
              <span className="flex items-center gap-1.5">
                <Star className="w-4 h-4 fill-amber-400 text-amber-400" />
                <span className="text-white font-semibold">{m.rating}</span>
              </span>
              <span className="flex items-center gap-1.5">
                <Users className="w-4 h-4" />
                {m.totalSessions} sessions
              </span>
              <span className="flex items-center gap-1.5">
                <MessageSquare className="w-4 h-4" />
                {m.responseRate}% response rate
              </span>
              <span className="flex items-center gap-1.5">
                <Clock className="w-4 h-4" />
                {m.availability.join(', ')}
              </span>
            </div>
            <div className="flex flex-wrap gap-2">
              {m.expertise.map((e) => (
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
        <p className="text-white/60 leading-relaxed whitespace-pre-line text-sm">{m.bio}</p>
      </motion.div>

      {/* Past sessions */}
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.15 }}
        className="card"
      >
        <h2 className="text-lg font-semibold text-white mb-4">Session History</h2>
        {MOCK_PAST_SESSIONS.length > 0 ? (
          <div className="space-y-3">
            {MOCK_PAST_SESSIONS.map((session, i) => (
              <motion.div
                key={session.id}
                initial={{ opacity: 0, x: -12 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.2 + i * 0.06 }}
                className="flex items-center justify-between p-4 glass rounded-xl"
              >
                <div>
                  <p className="text-white font-medium text-sm">{session.subject}</p>
                  <p className="text-white/40 text-xs mt-0.5">
                    {new Date(session.date).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}
                  </p>
                </div>
                <span className="badge bg-white/8 text-white/50">{session.duration} min</span>
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
        <p className="text-white/40 text-sm mb-5">{(reviews ?? MOCK_REVIEWS).length} reviews</p>
        <div className="space-y-4">
          {(reviews ?? MOCK_REVIEWS).map((review, i) => (
            <motion.div
              key={review.id}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.25 + i * 0.07 }}
              className="p-4 glass rounded-xl"
            >
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2">
                  <Avatar name={review.studentName} size="sm" />
                  <span className="text-white/70 text-sm font-medium">{review.studentName}</span>
                </div>
                <div className="flex items-center gap-2">
                  <StarRating value={review.rating} size="sm" />
                  <span className="text-white/30 text-xs">
                    {new Date(review.date).toLocaleDateString('en-IN', { month: 'short', year: 'numeric' })}
                  </span>
                </div>
              </div>
              <p className="text-white/60 text-sm leading-relaxed">"{review.comment}"</p>
            </motion.div>
          ))}
        </div>
      </motion.div>

      {/* Booking Modal */}
      <Modal isOpen={bookingOpen} onClose={() => setBookingOpen(false)} title="Book a Session" maxWidth="max-w-xl">
        <div className="space-y-5">
          <div className="flex items-center gap-3 p-4 glass rounded-xl">
            <Avatar name={m.name} size="md" />
            <div>
              <p className="font-semibold text-white">{m.name}</p>
              <StarRating value={m.rating} size="sm" />
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
