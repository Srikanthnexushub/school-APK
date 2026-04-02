import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import {
  Layers, BookOpen, Activity, Plus, X, ClipboardCheck,
  Calendar, Trophy, ChevronDown, ChevronRight, Users,
} from 'lucide-react';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { cn } from '../../lib/utils';

// ─── Types ────────────────────────────────────────────────────────────────────

interface CoverageLogResponse  { id: string; topicId: string; topicTitle: string; taughtOn: string; deliveryMethod: string; durationMins: number; }
interface ActivityResponse     { id: string; name: string; description: string; category: string; maxParticipants: number; scheduleDescription: string; active: boolean; }
interface ActivitySessionResponse { id: string; activityId: string; sessionDate: string; venue: string; durationMins: number; notes: string; }

const DELIVERY_METHODS = ['LECTURE', 'LAB', 'ONLINE', 'FIELD_TRIP', 'WORKSHOP', 'SEMINAR'];
const ACHIEVEMENT_TYPES = ['PARTICIPATION', 'MERIT', 'EXCELLENCE', 'GOLD', 'SILVER', 'BRONZE', 'SPECIAL'];

const CATEGORY_COLORS: Record<string, string> = {
  SPORTS: 'bg-green-500/20 text-green-300 border-green-500/30',
  ARTS:   'bg-pink-500/20 text-pink-300 border-pink-500/30',
  MUSIC:  'bg-purple-500/20 text-purple-300 border-purple-500/30',
  DANCE:  'bg-fuchsia-500/20 text-fuchsia-300 border-fuchsia-500/30',
  DRAMA:  'bg-rose-500/20 text-rose-300 border-rose-500/30',
  CODING_CLUB: 'bg-cyan-500/20 text-cyan-300 border-cyan-500/30',
  DEBATE: 'bg-blue-500/20 text-blue-300 border-blue-500/30',
  SCIENCE_CLUB: 'bg-teal-500/20 text-teal-300 border-teal-500/30',
  COMMUNITY_SERVICE: 'bg-amber-500/20 text-amber-300 border-amber-500/30',
  LEADERSHIP: 'bg-indigo-500/20 text-indigo-300 border-indigo-500/30',
  CULTURAL_EVENTS: 'bg-orange-500/20 text-orange-300 border-orange-500/30',
  OTHER:  'bg-slate-500/20 text-slate-300 border-slate-500/30',
};
const CATEGORY_ICONS: Record<string, string> = {
  SPORTS: '⚽', ARTS: '🎨', MUSIC: '🎵', DANCE: '💃', DRAMA: '🎭',
  CODING_CLUB: '💻', DEBATE: '🗣️', SCIENCE_CLUB: '🔬',
  COMMUNITY_SERVICE: '🤝', LEADERSHIP: '🏆', CULTURAL_EVENTS: '🎪', OTHER: '✨',
};

type Tab = 'coverage' | 'activities';

// ─── Log Coverage Modal ───────────────────────────────────────────────────────

function LogCoverageModal({ onClose, onSaved }: { onClose: () => void; onSaved: () => void }) {
  const [topicId, setTopicId] = useState('');
  const [taughtOn, setTaughtOn] = useState(new Date().toISOString().slice(0, 10));
  const [deliveryMethod, setDeliveryMethod] = useState('LECTURE');
  const [durationMins, setDurationMins] = useState(45);
  const [notes, setNotes] = useState('');
  const mutation = useMutation({
    mutationFn: () => api.post('/api/v1/curricular/coverage/log', { topicId, taughtOn, deliveryMethod, durationMins, notes: notes || undefined }),
    onSuccess: () => { toast.success('Coverage logged!'); onSaved(); onClose(); },
    onError: () => toast.error('Failed to log coverage.'),
  });
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60">
      <div className="glass rounded-2xl p-6 w-full max-w-md border border-white/10 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-white font-semibold">Log Coverage</h2>
          <button onClick={onClose} className="text-white/40 hover:text-white"><X className="w-5 h-5" /></button>
        </div>
        <div className="space-y-3">
          <div>
            <label className="text-xs text-white/50 mb-1 block">Topic ID (UUID)</label>
            <input value={topicId} onChange={e => setTopicId(e.target.value)} placeholder="Get from /api/v1/curricular/frameworks/topics"
              className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-white/50 mb-1 block">Taught On</label>
              <input type="date" value={taughtOn} onChange={e => setTaughtOn(e.target.value)}
                className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500" />
            </div>
            <div>
              <label className="text-xs text-white/50 mb-1 block">Duration (mins)</label>
              <input type="number" value={durationMins} onChange={e => setDurationMins(Number(e.target.value))} min={10}
                className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500" />
            </div>
          </div>
          <div>
            <label className="text-xs text-white/50 mb-1 block">Delivery Method</label>
            <select value={deliveryMethod} onChange={e => setDeliveryMethod(e.target.value)}
              className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500">
              {DELIVERY_METHODS.map(m => <option key={m} value={m}>{m}</option>)}
            </select>
          </div>
          <div>
            <label className="text-xs text-white/50 mb-1 block">Notes (optional)</label>
            <textarea value={notes} onChange={e => setNotes(e.target.value)} rows={2}
              className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500 resize-none" />
          </div>
        </div>
        <div className="flex gap-2">
          <button onClick={onClose} className="flex-1 py-2 rounded-xl border border-white/10 text-white/50 hover:text-white text-sm">Cancel</button>
          <button onClick={() => mutation.mutate()} disabled={!topicId || mutation.isPending}
            className="flex-1 py-2 rounded-xl bg-brand-600 hover:bg-brand-500 text-white text-sm disabled:opacity-40">
            {mutation.isPending ? 'Saving…' : 'Log Coverage'}
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Add Session Modal ────────────────────────────────────────────────────────

function AddSessionModal({ activityId, activityName, onClose, onSaved }: { activityId: string; activityName: string; onClose: () => void; onSaved: () => void }) {
  const [sessionDate, setSessionDate] = useState(new Date().toISOString().slice(0, 10));
  const [venue, setVenue] = useState('');
  const [durationMins, setDurationMins] = useState(60);
  const [notes, setNotes] = useState('');
  const mutation = useMutation({
    mutationFn: () => api.post(`/api/v1/curricular/activities/${activityId}/sessions`, { sessionDate, venue, durationMins, notes: notes || undefined }),
    onSuccess: () => { toast.success('Session added!'); onSaved(); onClose(); },
    onError: () => toast.error('Failed to add session.'),
  });
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60">
      <div className="glass rounded-2xl p-6 w-full max-w-md border border-white/10 space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-white font-semibold">Add Session</h2>
            <p className="text-xs text-white/40 mt-0.5">{activityName}</p>
          </div>
          <button onClick={onClose} className="text-white/40 hover:text-white"><X className="w-5 h-5" /></button>
        </div>
        <div className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-white/50 mb-1 block">Session Date</label>
              <input type="date" value={sessionDate} onChange={e => setSessionDate(e.target.value)}
                className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500" />
            </div>
            <div>
              <label className="text-xs text-white/50 mb-1 block">Duration (mins)</label>
              <input type="number" value={durationMins} onChange={e => setDurationMins(Number(e.target.value))} min={15}
                className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500" />
            </div>
          </div>
          <div>
            <label className="text-xs text-white/50 mb-1 block">Venue</label>
            <input value={venue} onChange={e => setVenue(e.target.value)} placeholder="e.g. School Ground, Auditorium"
              className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500" />
          </div>
          <div>
            <label className="text-xs text-white/50 mb-1 block">Notes (optional)</label>
            <textarea value={notes} onChange={e => setNotes(e.target.value)} rows={2} placeholder="What was practiced, drills, outcomes…"
              className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500 resize-none" />
          </div>
        </div>
        <div className="flex gap-2">
          <button onClick={onClose} className="flex-1 py-2 rounded-xl border border-white/10 text-white/50 hover:text-white text-sm">Cancel</button>
          <button onClick={() => mutation.mutate()} disabled={mutation.isPending}
            className="flex-1 py-2 rounded-xl bg-brand-600 hover:bg-brand-500 text-white text-sm disabled:opacity-40">
            {mutation.isPending ? 'Saving…' : 'Add Session'}
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Award Achievement Modal ──────────────────────────────────────────────────

function AwardAchievementModal({ activityId, activityName, onClose, onSaved }: { activityId: string; activityName: string; onClose: () => void; onSaved: () => void }) {
  const [studentId, setStudentId] = useState('');
  const [achievementType, setAchievementType] = useState('PARTICIPATION');
  const [title, setTitle] = useState('');
  const [issuedAt, setIssuedAt] = useState(new Date().toISOString().slice(0, 10));
  const mutation = useMutation({
    mutationFn: () => api.post(`/api/v1/curricular/activities/${activityId}/achievements?studentId=${studentId}`, {
      achievementType, title, issuedAt,
    }),
    onSuccess: () => { toast.success('Achievement awarded!'); onSaved(); onClose(); },
    onError: () => toast.error('Failed to award achievement.'),
  });
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60">
      <div className="glass rounded-2xl p-6 w-full max-w-md border border-white/10 space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-white font-semibold">Award Achievement</h2>
            <p className="text-xs text-white/40 mt-0.5">{activityName}</p>
          </div>
          <button onClick={onClose} className="text-white/40 hover:text-white"><X className="w-5 h-5" /></button>
        </div>
        <div className="space-y-3">
          <div>
            <label className="text-xs text-white/50 mb-1 block">Student ID (UUID)</label>
            <input value={studentId} onChange={e => setStudentId(e.target.value)} placeholder="e.g. 50d63f9c-9b9c-4737-…"
              className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-white/50 mb-1 block">Achievement Type</label>
              <select value={achievementType} onChange={e => setAchievementType(e.target.value)}
                className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500">
                {ACHIEVEMENT_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs text-white/50 mb-1 block">Issued Date</label>
              <input type="date" value={issuedAt} onChange={e => setIssuedAt(e.target.value)}
                className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500" />
            </div>
          </div>
          <div>
            <label className="text-xs text-white/50 mb-1 block">Achievement Title</label>
            <input value={title} onChange={e => setTitle(e.target.value)} placeholder="e.g. Best Performer — Inter-School Sports 2026"
              className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500" />
          </div>
        </div>
        <div className="flex gap-2">
          <button onClick={onClose} className="flex-1 py-2 rounded-xl border border-white/10 text-white/50 hover:text-white text-sm">Cancel</button>
          <button onClick={() => mutation.mutate()} disabled={!studentId || !title || mutation.isPending}
            className="flex-1 py-2 rounded-xl bg-brand-600 hover:bg-brand-500 text-white text-sm disabled:opacity-40">
            {mutation.isPending ? 'Awarding…' : 'Award'}
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Activity Row (with sessions expand + modals) ─────────────────────────────

function ActivityRow({ a, qc }: { a: ActivityResponse; qc: ReturnType<typeof useQueryClient> }) {
  const [expanded, setExpanded]           = useState(false);
  const [showSession, setShowSession]     = useState(false);
  const [showAchieve, setShowAchieve]     = useState(false);

  const { data: sessions = [], isLoading: sessionsLoading } = useQuery<ActivitySessionResponse[]>({
    queryKey: ['activity-sessions', a.id],
    queryFn:  () => api.get(`/api/v1/curricular/activities/${a.id}/sessions`).then(r => Array.isArray(r.data) ? r.data : []),
    enabled: expanded,
  });

  return (
    <>
      {showSession && <AddSessionModal activityId={a.id} activityName={a.name} onClose={() => setShowSession(false)} onSaved={() => qc.invalidateQueries({ queryKey: ['activity-sessions', a.id] })} />}
      {showAchieve && <AwardAchievementModal activityId={a.id} activityName={a.name} onClose={() => setShowAchieve(false)} onSaved={() => {}} />}

      <div className="bg-surface-100/40 border border-white/5 rounded-xl overflow-hidden">
        <div className="flex items-start gap-3 p-4">
          <div className={cn('w-11 h-11 rounded-xl flex items-center justify-center text-xl shrink-0',
            CATEGORY_COLORS[a.category]?.split(' ')[0] ?? 'bg-slate-500/20')}>
            {CATEGORY_ICONS[a.category] ?? '✨'}
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 flex-wrap">
              <p className="text-sm font-semibold text-white">{a.name}</p>
              <span className={cn('text-[10px] px-2 py-0.5 rounded-full border', CATEGORY_COLORS[a.category] ?? 'bg-slate-500/20 text-slate-300 border-slate-500/30')}>
                {a.category.replace(/_/g, ' ')}
              </span>
            </div>
            {a.description && <p className="text-xs text-white/40 mt-1 line-clamp-1">{a.description}</p>}
            <div className="flex items-center gap-3 mt-2 text-xs text-white/30">
              <span><Users className="w-3 h-3 inline mr-1" />{a.maxParticipants} max</span>
              {a.scheduleDescription && <span>📅 {a.scheduleDescription}</span>}
            </div>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <button onClick={() => setShowAchieve(true)}
              className="flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-yellow-500/10 border border-yellow-500/20 text-yellow-400 hover:bg-yellow-500/20 text-xs transition-colors">
              <Trophy className="w-3.5 h-3.5" /> Award
            </button>
            <button onClick={() => setShowSession(true)}
              className="flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-brand-500/10 border border-brand-500/20 text-brand-400 hover:bg-brand-500/20 text-xs transition-colors">
              <Plus className="w-3.5 h-3.5" /> Session
            </button>
            <button onClick={() => setExpanded(v => !v)}
              className="p-1.5 rounded-lg text-white/40 hover:text-white hover:bg-white/5 transition-colors">
              {expanded ? <ChevronDown className="w-4 h-4" /> : <ChevronRight className="w-4 h-4" />}
            </button>
          </div>
        </div>

        {/* Sessions panel */}
        {expanded && (
          <div className="border-t border-white/5 p-4 space-y-2 bg-white/[0.02]">
            <p className="text-xs text-white/40 font-semibold uppercase tracking-wide">Sessions ({sessions.length})</p>
            {sessionsLoading && <p className="text-xs text-white/30">Loading…</p>}
            {!sessionsLoading && sessions.length === 0 && (
              <p className="text-xs text-white/30">No sessions recorded yet. Click "+ Session" to add one.</p>
            )}
            {sessions.map(s => (
              <div key={s.id} className="flex items-center gap-3 p-2.5 bg-white/[0.03] rounded-lg">
                <Calendar className="w-4 h-4 text-brand-400 shrink-0" />
                <div className="flex-1 min-w-0">
                  <p className="text-xs text-white">{new Date(s.sessionDate).toLocaleDateString()} {s.venue && `— ${s.venue}`}</p>
                  {s.notes && <p className="text-[10px] text-white/30 truncate">{s.notes}</p>}
                </div>
                <span className="text-xs text-white/30 shrink-0">{s.durationMins} mins</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </>
  );
}

// ─── Main Component ───────────────────────────────────────────────────────────

export default function MentorPortalCurricularPage() {
  const { user } = useAuthStore();
  const qc = useQueryClient();
  const [tab, setTab] = useState<Tab>('activities');
  const [showLogModal, setShowLogModal] = useState(false);
  const centerId = user?.centerId ?? '';

  const { data: coverage = [], isLoading: coverageLoading } = useQuery<CoverageLogResponse[]>({
    queryKey: ['curricular-coverage', user?.id],
    queryFn:  () => api.get('/api/v1/curricular/coverage').then(r => Array.isArray(r.data) ? r.data : []),
    enabled: tab === 'coverage',
    retry: false,
  });

  const { data: activities = [], isLoading: activitiesLoading } = useQuery<ActivityResponse[]>({
    queryKey: ['curricular-activities', centerId],
    queryFn:  () => api.get(`/api/v1/curricular/activities?centerId=${centerId}`).then(r => Array.isArray(r.data) ? r.data : []),
    enabled: tab === 'activities' && !!centerId,
    retry: false,
  });

  const totalMins = coverage.reduce((s, c) => s + c.durationMins, 0);

  return (
    <div className="p-6 max-w-5xl mx-auto space-y-6">
      {showLogModal && (
        <LogCoverageModal onClose={() => setShowLogModal(false)} onSaved={() => qc.invalidateQueries({ queryKey: ['curricular-coverage'] })} />
      )}

      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <Layers className="w-6 h-6 text-brand-400" /> Curricular
          </h1>
          <p className="text-sm text-white/40 mt-1">Manage activities, log sessions, award achievements</p>
        </div>
        {tab === 'coverage' && (
          <button onClick={() => setShowLogModal(true)}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-brand-600 hover:bg-brand-500 text-white text-sm transition-colors">
            <Plus className="w-4 h-4" /> Log Coverage
          </button>
        )}
      </div>

      {/* Tabs */}
      <div className="flex gap-1 bg-surface-100/40 border border-white/5 rounded-xl p-1 w-fit">
        {([
          { id: 'activities', label: 'Activities', Icon: Activity },
          { id: 'coverage',   label: 'My Coverage', Icon: ClipboardCheck },
        ] as const).map(({ id, label, Icon }) => (
          <button key={id} onClick={() => setTab(id)}
            className={cn('flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium transition-colors',
              tab === id ? 'bg-brand-600 text-white' : 'text-white/50 hover:text-white/70 hover:bg-white/5'
            )}>
            <Icon className="w-3.5 h-3.5" /> {label}
          </button>
        ))}
      </div>

      {/* ── Activities Tab ── */}
      {tab === 'activities' && (
        <div className="space-y-3">
          {!centerId && <div className="text-center py-10 text-white/30 text-sm">No center assigned.</div>}
          {activitiesLoading && <div className="text-white/40 text-sm py-8 text-center">Loading…</div>}
          {!activitiesLoading && centerId && activities.filter(a => a.active).length === 0 && (
            <div className="flex flex-col items-center py-16 text-white/30">
              <Activity className="w-12 h-12 mb-3" />
              <p className="text-sm">No active activities at your center.</p>
            </div>
          )}
          {activities.filter(a => a.active).map(a => (
            <ActivityRow key={a.id} a={a} qc={qc} />
          ))}
        </div>
      )}

      {/* ── Coverage Tab ── */}
      {tab === 'coverage' && (
        <div className="space-y-4">
          {coverage.length > 0 && (
            <div className="grid grid-cols-3 gap-3">
              <div className="p-4 bg-surface-100/40 border border-white/5 rounded-xl text-center">
                <p className="text-2xl font-bold text-white">{coverage.length}</p>
                <p className="text-xs text-white/40 mt-1">Topics logged</p>
              </div>
              <div className="p-4 bg-surface-100/40 border border-white/5 rounded-xl text-center">
                <p className="text-2xl font-bold text-white">{Math.round(totalMins / 60)}h</p>
                <p className="text-xs text-white/40 mt-1">Total taught</p>
              </div>
              <div className="p-4 bg-surface-100/40 border border-white/5 rounded-xl text-center">
                <p className="text-2xl font-bold text-white">{Math.round(totalMins / Math.max(coverage.length, 1))}</p>
                <p className="text-xs text-white/40 mt-1">Avg mins/topic</p>
              </div>
            </div>
          )}
          {coverageLoading && <div className="text-white/40 text-sm py-8 text-center">Loading…</div>}
          {!coverageLoading && coverage.length === 0 && (
            <div className="flex flex-col items-center py-16 text-white/30">
              <BookOpen className="w-12 h-12 mb-3" />
              <p className="text-sm">No coverage logged yet.</p>
            </div>
          )}
          <div className="space-y-2">
            {coverage.map(c => (
              <div key={c.id} className="flex items-center gap-4 p-4 bg-surface-100/40 border border-white/5 rounded-xl">
                <div className="w-9 h-9 rounded-xl bg-brand-500/10 flex items-center justify-center shrink-0">
                  <BookOpen className="w-4 h-4 text-brand-400" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-white truncate">{c.topicTitle}</p>
                  <div className="flex items-center gap-2 mt-1">
                    <span className="text-[10px] px-2 py-0.5 rounded-full bg-blue-500/20 text-blue-300">{c.deliveryMethod}</span>
                    <span className="text-xs text-white/30">{c.durationMins} mins · {new Date(c.taughtOn).toLocaleDateString()}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
