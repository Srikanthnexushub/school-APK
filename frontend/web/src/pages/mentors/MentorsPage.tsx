import { useState, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useQuery, useMutation } from '@tanstack/react-query';
import { Search, Star, Clock, Calendar, ChevronDown, Check } from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { cn } from '../../lib/utils';
import { Avatar } from '../../components/ui/Avatar';
import { StarRating } from '../../components/ui/StarRating';
import { Modal } from '../../components/ui/Modal';

interface Mentor {
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

const SUBJECTS = ['Math', 'Physics', 'Chemistry', 'Biology', 'English', 'CS'];
const SORT_OPTIONS = ['Highest Rated', 'Most Sessions', 'Available Now'] as const;
type SortOption = typeof SORT_OPTIONS[number];

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

interface BookingModalProps {
  mentor: Mentor | null;
  onClose: () => void;
}

function BookingModal({ mentor, onClose }: BookingModalProps) {
  const user = useAuthStore((s) => s.user);
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);
  const [selectedTime, setSelectedTime] = useState<string | null>(null);
  const [duration, setDuration] = useState(60);
  const [sessionMode, setSessionMode] = useState('VIDEO');
  const [notes, setNotes] = useState('');
  const days = getNextSevenDays();

  const bookMutation = useMutation({
    mutationFn: () => {
      if (!selectedDate || !selectedTime || !mentor) throw new Error('Incomplete');
      const [h, m] = selectedTime.split(':').map(Number);
      const scheduledAt = new Date(selectedDate);
      scheduledAt.setHours(h, m, 0, 0);
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
      toast.success('Session booked! Check My Sessions for details.');
      onClose();
    },
    onError: () => {
      toast.error('Booking failed. Please try again.');
    },
  });

  if (!mentor) return null;

  const specializationList = mentor.specializations
    ? mentor.specializations.split(',').map((s) => s.trim()).filter(Boolean)
    : [];

  return (
    <Modal isOpen={!!mentor} onClose={onClose} title="Book a Session" maxWidth="max-w-xl">
      <div className="space-y-5">
        <div className="flex items-center gap-3 p-4 glass rounded-xl">
          <Avatar name={mentor.fullName} size="lg" />
          <div>
            <p className="font-semibold text-white">{mentor.fullName}</p>
            <div className="flex items-center gap-1 mt-0.5">
              <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
              <span className="text-sm text-white/60">{mentor.averageRating?.toFixed(1)} ({mentor.totalSessions} sessions)</span>
            </div>
            <div className="flex flex-wrap gap-1.5 mt-1.5">
              {specializationList.map((e) => (
                <span key={e} className="badge bg-brand-600/20 text-brand-300 text-xs">{e}</span>
              ))}
            </div>
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
  );
}

interface MentorCardProps {
  mentor: Mentor;
  index: number;
  onBook: (mentor: Mentor) => void;
}

function MentorCard({ mentor, index, onBook }: MentorCardProps) {
  const specializationList = mentor.specializations
    ? mentor.specializations.split(',').map((s) => s.trim()).filter(Boolean)
    : [];

  return (
    <motion.div
      className="card flex flex-col gap-4 glass-hover"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.06 }}
    >
      <div className="flex items-start gap-4">
        <Avatar name={mentor.fullName} size="xl" />
        <div className="flex-1 min-w-0">
          <h3 className="font-bold text-white">{mentor.fullName}</h3>
          <div className="flex items-center gap-1.5 mt-1">
            <StarRating value={mentor.averageRating ?? 0} size="sm" />
            <span className="text-white/50 text-xs">{mentor.averageRating?.toFixed(1)} ({mentor.totalSessions} sessions)</span>
          </div>
          <div className="flex flex-wrap gap-1.5 mt-2">
            {specializationList.map((e) => (
              <span key={e} className="badge bg-brand-600/20 text-brand-300 text-xs">{e}</span>
            ))}
          </div>
        </div>
      </div>

      <div className="border-t border-white/8 pt-3">
        <p className="text-white/50 text-sm italic leading-relaxed line-clamp-2">"{mentor.bio}"</p>
      </div>

      <div className="border-t border-white/8 pt-3 flex items-center justify-between">
        <div className="flex items-center gap-1.5 text-white/40 text-xs">
          <Clock className="w-3.5 h-3.5" />
          <span>{mentor.yearsOfExperience} yrs exp</span>
        </div>
        <div className="flex items-center gap-1.5 text-white/40 text-xs">
          <Calendar className="w-3.5 h-3.5" />
          {mentor.isAvailable ? (
            <span className="text-emerald-400">Available</span>
          ) : (
            <span>Unavailable</span>
          )}
        </div>
      </div>

      <button onClick={() => onBook(mentor)} className="btn-primary w-full text-sm">
        Book Session
      </button>
    </motion.div>
  );
}

export default function MentorsPage() {
  const [search, setSearch] = useState('');
  const [activeSubjects, setActiveSubjects] = useState<Set<string>>(new Set());
  const [sort, setSort] = useState<SortOption>('Highest Rated');
  const [bookingMentor, setBookingMentor] = useState<Mentor | null>(null);
  const [showSort, setShowSort] = useState(false);

  const { data: mentors } = useQuery<Mentor[]>({
    queryKey: ['mentors'],
    queryFn: async () => {
      const res = await api.get('/api/v1/mentors');
      return res.data;
    },
    retry: false,
    placeholderData: [],
  });

  const filtered = useMemo(() => {
    let list = mentors ?? [];

    if (search) {
      const q = search.toLowerCase();
      list = list.filter(
        (m) =>
          m.fullName.toLowerCase().includes(q) ||
          (m.specializations ?? '').toLowerCase().includes(q)
      );
    }

    if (activeSubjects.size > 0) {
      list = list.filter((m) => {
        const specs = (m.specializations ?? '').split(',').map((s) => s.trim());
        return specs.some((e) => activeSubjects.has(e));
      });
    }

    if (sort === 'Highest Rated') list = [...list].sort((a, b) => (b.averageRating ?? 0) - (a.averageRating ?? 0));
    else if (sort === 'Most Sessions') list = [...list].sort((a, b) => b.totalSessions - a.totalSessions);
    else if (sort === 'Available Now') list = [...list].filter((m) => m.isAvailable);

    return list;
  }, [mentors, search, activeSubjects, sort]);

  function toggleSubject(subject: string) {
    setActiveSubjects((prev) => {
      const next = new Set(prev);
      if (next.has(subject)) next.delete(subject);
      else next.add(subject);
      return next;
    });
  }

  return (
    <div className="min-h-screen p-6 space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-white mb-1">Find a Mentor</h1>
        <p className="text-white/50">Human experts to guide your exam preparation and career journey.</p>
      </div>

      <div className="card space-y-4">
        <div className="flex gap-3">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search by name or subject…"
              className="input w-full pl-9"
            />
          </div>
          <div className="relative">
            <button
              onClick={() => setShowSort(!showSort)}
              className="input flex items-center gap-2 cursor-pointer whitespace-nowrap"
            >
              {sort} <ChevronDown className="w-4 h-4" />
            </button>
            <AnimatePresence>
              {showSort && (
                <motion.div
                  initial={{ opacity: 0, y: -8 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -8 }}
                  className="absolute right-0 top-full mt-1 z-10 glass border border-white/10 rounded-xl overflow-hidden min-w-40"
                >
                  {SORT_OPTIONS.map((opt) => (
                    <button
                      key={opt}
                      onClick={() => { setSort(opt); setShowSort(false); }}
                      className={cn(
                        'w-full text-left px-4 py-2.5 text-sm transition-colors',
                        sort === opt ? 'text-brand-400 bg-brand-600/10' : 'text-white/60 hover:text-white hover:bg-white/5'
                      )}
                    >
                      {opt}
                    </button>
                  ))}
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </div>

        <div className="flex flex-wrap gap-2">
          {SUBJECTS.map((subject) => (
            <button
              key={subject}
              onClick={() => toggleSubject(subject)}
              className={cn(
                'px-3 py-1.5 rounded-full text-xs font-medium border transition-all',
                activeSubjects.has(subject)
                  ? 'bg-brand-600 border-brand-500 text-white'
                  : 'glass border-white/10 text-white/50 hover:border-white/20 hover:text-white'
              )}
            >
              {subject}
            </button>
          ))}
          {activeSubjects.size > 0 && (
            <button
              onClick={() => setActiveSubjects(new Set())}
              className="px-3 py-1.5 rounded-full text-xs font-medium text-white/30 hover:text-white transition-colors"
            >
              Clear filters
            </button>
          )}
        </div>
      </div>

      <p className="text-white/40 text-sm">{filtered.length} mentor{filtered.length !== 1 ? 's' : ''} found</p>

      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
        {filtered.map((mentor, i) => (
          <MentorCard key={mentor.id} mentor={mentor} index={i} onBook={setBookingMentor} />
        ))}
        {filtered.length === 0 && (
          <div className="col-span-full text-center py-16 text-white/30">
            <Search className="w-10 h-10 mx-auto mb-3 opacity-40" />
            <p className="text-lg">No mentors found.</p>
            <p className="text-sm mt-1">Try adjusting your search or filters.</p>
          </div>
        )}
      </div>

      <BookingModal mentor={bookingMentor} onClose={() => setBookingMentor(null)} />
    </div>
  );
}
