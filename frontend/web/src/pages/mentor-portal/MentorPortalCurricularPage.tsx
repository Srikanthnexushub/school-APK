import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import {
  Layers, BookOpen, Activity, Plus, X, BarChart3,
  ClipboardCheck, Users, Calendar,
} from 'lucide-react';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { cn } from '../../lib/utils';

// ─── Types ──────────────────────────────────────────────────────────────────

interface CoverageLogResponse {
  id: string; topicId: string; topicTitle: string;
  taughtOn: string; deliveryMethod: string; durationMins: number;
}
interface ActivityResponse {
  id: string; centerId: string; name: string; description: string;
  category: string; maxParticipants: number; scheduleDescription: string; active: boolean;
}

const DELIVERY_METHODS = ['LECTURE', 'LAB', 'ONLINE', 'FIELD_TRIP', 'WORKSHOP', 'SEMINAR'];

const CATEGORY_COLORS: Record<string, string> = {
  SPORTS: 'bg-green-500/20 text-green-300',
  ARTS:   'bg-pink-500/20 text-pink-300',
  MUSIC:  'bg-purple-500/20 text-purple-300',
  DANCE:  'bg-fuchsia-500/20 text-fuchsia-300',
  DRAMA:  'bg-rose-500/20 text-rose-300',
  CODING_CLUB: 'bg-cyan-500/20 text-cyan-300',
  DEBATE: 'bg-blue-500/20 text-blue-300',
  SCIENCE_CLUB: 'bg-teal-500/20 text-teal-300',
  COMMUNITY_SERVICE: 'bg-amber-500/20 text-amber-300',
  LEADERSHIP: 'bg-indigo-500/20 text-indigo-300',
  CULTURAL_EVENTS: 'bg-orange-500/20 text-orange-300',
  OTHER:  'bg-slate-500/20 text-slate-300',
};

type Tab = 'coverage' | 'activities';

// ─── Log Coverage Modal ──────────────────────────────────────────────────────

function LogCoverageModal({ onClose, onSaved }: { onClose: () => void; onSaved: () => void }) {
  const [topicId, setTopicId] = useState('');
  const [taughtOn, setTaughtOn] = useState(new Date().toISOString().slice(0, 10));
  const [deliveryMethod, setDeliveryMethod] = useState('LECTURE');
  const [durationMins, setDurationMins] = useState(45);
  const [notes, setNotes] = useState('');

  const mutation = useMutation({
    mutationFn: () => api.post('/api/v1/curricular/coverage/log', {
      topicId, taughtOn, deliveryMethod, durationMins, notes: notes || undefined,
    }),
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
            <input
              value={topicId} onChange={e => setTopicId(e.target.value)} placeholder="e.g. 3fa85f64-..."
              className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500"
            />
            <p className="text-[10px] text-white/30 mt-1">Get topic IDs from GET /api/v1/curricular/frameworks/topics</p>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-white/50 mb-1 block">Taught On</label>
              <input type="date" value={taughtOn} onChange={e => setTaughtOn(e.target.value)}
                className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500"
              />
            </div>
            <div>
              <label className="text-xs text-white/50 mb-1 block">Duration (mins)</label>
              <input type="number" value={durationMins} onChange={e => setDurationMins(Number(e.target.value))} min={10} max={240}
                className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500"
              />
            </div>
          </div>
          <div>
            <label className="text-xs text-white/50 mb-1 block">Delivery Method</label>
            <select value={deliveryMethod} onChange={e => setDeliveryMethod(e.target.value)}
              className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500"
            >
              {DELIVERY_METHODS.map(m => <option key={m} value={m}>{m}</option>)}
            </select>
          </div>
          <div>
            <label className="text-xs text-white/50 mb-1 block">Notes (optional)</label>
            <textarea value={notes} onChange={e => setNotes(e.target.value)} rows={2} placeholder="Class notes, resources used…"
              className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500 resize-none"
            />
          </div>
        </div>

        <div className="flex gap-2 pt-1">
          <button onClick={onClose} className="flex-1 py-2 rounded-xl border border-white/10 text-white/50 hover:text-white hover:bg-white/5 text-sm transition-colors">
            Cancel
          </button>
          <button
            onClick={() => mutation.mutate()}
            disabled={!topicId || mutation.isPending}
            className="flex-1 py-2 rounded-xl bg-brand-600 hover:bg-brand-500 text-white text-sm transition-colors disabled:opacity-40"
          >
            {mutation.isPending ? 'Saving…' : 'Log Coverage'}
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Component ──────────────────────────────────────────────────────────────

export default function MentorPortalCurricularPage() {
  const { user } = useAuthStore();
  const qc = useQueryClient();
  const [tab, setTab] = useState<Tab>('coverage');
  const [showLogModal, setShowLogModal] = useState(false);
  const centerId = user?.centerId ?? '';

  // ── Coverage ──────────────────────────────────────────────────────────────
  const { data: coverage = [], isLoading: coverageLoading } = useQuery<CoverageLogResponse[]>({
    queryKey: ['curricular-coverage', user?.id],
    queryFn: () => api.get('/api/v1/curricular/coverage').then(r => Array.isArray(r.data) ? r.data : []),
    enabled: tab === 'coverage',
    retry: false,
  });

  // ── Activities ────────────────────────────────────────────────────────────
  const { data: activities = [], isLoading: activitiesLoading } = useQuery<ActivityResponse[]>({
    queryKey: ['curricular-activities', centerId],
    queryFn: () => api.get(`/api/v1/curricular/activities?centerId=${centerId}`).then(r => Array.isArray(r.data) ? r.data : []),
    enabled: tab === 'activities' && !!centerId,
    retry: false,
  });

  // Coverage stats
  const totalMins = coverage.reduce((s, c) => s + c.durationMins, 0);
  const deliveryBreakdown = coverage.reduce<Record<string, number>>((acc, c) => {
    acc[c.deliveryMethod] = (acc[c.deliveryMethod] ?? 0) + 1;
    return acc;
  }, {});

  return (
    <div className="p-6 max-w-5xl mx-auto space-y-6">
      {showLogModal && (
        <LogCoverageModal
          onClose={() => setShowLogModal(false)}
          onSaved={() => qc.invalidateQueries({ queryKey: ['curricular-coverage'] })}
        />
      )}

      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <Layers className="w-6 h-6 text-brand-400" /> Curricular
          </h1>
          <p className="text-sm text-white/40 mt-1">Track curriculum coverage and manage extracurricular activities</p>
        </div>
        {tab === 'coverage' && (
          <button
            onClick={() => setShowLogModal(true)}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-brand-600 hover:bg-brand-500 text-white text-sm transition-colors"
          >
            <Plus className="w-4 h-4" /> Log Coverage
          </button>
        )}
      </div>

      {/* Tabs */}
      <div className="flex gap-1 bg-surface-100/40 border border-white/5 rounded-xl p-1 w-fit">
        {([
          { id: 'coverage',   label: 'My Coverage',  Icon: ClipboardCheck },
          { id: 'activities', label: 'Activities',   Icon: Activity },
        ] as const).map(({ id, label, Icon }) => (
          <button key={id} onClick={() => setTab(id)}
            className={cn('flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium transition-colors',
              tab === id ? 'bg-brand-600 text-white' : 'text-white/50 hover:text-white/70 hover:bg-white/5'
            )}>
            <Icon className="w-3.5 h-3.5" /> {label}
          </button>
        ))}
      </div>

      {/* ── Coverage Tab ── */}
      {tab === 'coverage' && (
        <div className="space-y-4">
          {/* Stats row */}
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
                <p className="text-2xl font-bold text-white">{Object.keys(deliveryBreakdown).length}</p>
                <p className="text-xs text-white/40 mt-1">Methods used</p>
              </div>
            </div>
          )}

          {coverageLoading && <div className="text-white/40 text-sm py-8 text-center">Loading…</div>}

          {!coverageLoading && coverage.length === 0 && (
            <div className="flex flex-col items-center py-16 text-white/30">
              <BookOpen className="w-12 h-12 mb-3" />
              <p className="text-sm">No coverage logged yet. Click "Log Coverage" to start.</p>
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
                    <span className="text-xs text-white/30">{c.durationMins} mins</span>
                    <span className="text-xs text-white/30">{new Date(c.taughtOn).toLocaleDateString()}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ── Activities Tab ── */}
      {tab === 'activities' && (
        <div className="space-y-3">
          {!centerId && (
            <div className="text-center py-10 text-white/30 text-sm">
              No center assigned. Please contact your administrator.
            </div>
          )}
          {activitiesLoading && <div className="text-white/40 text-sm py-8 text-center">Loading…</div>}
          {!activitiesLoading && centerId && activities.length === 0 && (
            <div className="flex flex-col items-center py-16 text-white/30">
              <Activity className="w-12 h-12 mb-3" />
              <p className="text-sm">No extracurricular activities at your center yet.</p>
            </div>
          )}
          {activities.filter(a => a.active).map(a => (
            <div key={a.id} className="p-4 bg-surface-100/40 border border-white/5 rounded-xl">
              <div className="flex items-start gap-3">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <p className="text-sm font-semibold text-white">{a.name}</p>
                    <span className={cn('text-[10px] px-2 py-0.5 rounded-full', CATEGORY_COLORS[a.category] ?? 'bg-slate-500/20 text-slate-300')}>
                      {a.category.replace('_', ' ')}
                    </span>
                  </div>
                  {a.description && <p className="text-xs text-white/40 mt-1 line-clamp-2">{a.description}</p>}
                  <div className="flex items-center gap-3 mt-2 text-xs text-white/30">
                    <span className="flex items-center gap-1"><Users className="w-3 h-3" /> Max {a.maxParticipants}</span>
                    {a.scheduleDescription && <span className="flex items-center gap-1"><Calendar className="w-3 h-3" /> {a.scheduleDescription}</span>}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
