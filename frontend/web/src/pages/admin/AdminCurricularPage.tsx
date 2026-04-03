import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import {
  Layers, Activity, BarChart3, Calendar, Plus, X,
  Users, CheckCircle2, AlertCircle, ChevronDown, ChevronRight,
} from 'lucide-react';
import api from '../../lib/api';
import { cn } from '../../lib/utils';

// ─── Types ──────────────────────────────────────────────────────────────────

interface ActivityResponse {
  id: string; name: string; description: string; category: string;
  maxParticipants: number; scheduleDescription: string; active: boolean;
}
interface ActivitySessionResponse {
  id: string; activityId: string; sessionDate: string; venue: string; durationMins: number; notes: string;
}
interface AcademicYearResponse {
  id: string; yearLabel: string; startDate: string; endDate: string; active: boolean;
}
interface CenterCoverageReport {
  centerId: string; subjectId: string; gradeId: string;
  totalTopics: number; coveredTopics: number; coveragePercent: number;
  uncoveredTopicCodes: string[];
}
interface BoardResponse    { id: string; boardCode: string; boardName: string; }
interface SubjectResponse  { id: string; subjectCode: string; subjectName: string; }
interface GradeLevelResponse { id: string; gradeCode: string; displayName: string; sortOrder: number; }

const ACTIVITY_CATEGORIES = [
  'SPORTS','ARTS','MUSIC','DANCE','DRAMA','CODING_CLUB',
  'DEBATE','SCIENCE_CLUB','COMMUNITY_SERVICE','LEADERSHIP','CULTURAL_EVENTS','OTHER',
];

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

type Tab = 'activities' | 'coverage' | 'calendar';

// ─── Activity Row with expandable sessions ───────────────────────────────────

function AdminActivityRow({ a, onDeactivate, deactivating }: {
  a: ActivityResponse;
  onDeactivate: (id: string) => void;
  deactivating: boolean;
}) {
  const [expanded, setExpanded] = useState(false);

  const { data: sessions = [], isLoading: sessionsLoading } = useQuery<ActivitySessionResponse[]>({
    queryKey: ['admin-activity-sessions', a.id],
    queryFn:  () => api.get(`/api/v1/curricular/activities/${a.id}/sessions`).then(r => Array.isArray(r.data) ? r.data : []),
    enabled: expanded,
  });

  return (
    <div className={cn('border rounded-xl overflow-hidden', a.active ? 'bg-surface-100/40 border-white/5' : 'bg-red-500/5 border-red-500/10 opacity-60')}>
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
            {!a.active && <span className="text-[10px] px-2 py-0.5 rounded-full bg-red-500/20 text-red-300 border border-red-500/30">Inactive</span>}
          </div>
          {a.description && <p className="text-xs text-white/40 mt-1 line-clamp-2">{a.description}</p>}
          <div className="flex items-center gap-3 mt-2 text-xs text-white/30">
            <span><Users className="w-3 h-3 inline mr-1" />{a.maxParticipants} max</span>
            {a.scheduleDescription && <span>📅 {a.scheduleDescription}</span>}
          </div>
        </div>
        <div className="flex items-center gap-2 shrink-0">
          {a.active && (
            <button onClick={() => onDeactivate(a.id)} disabled={deactivating}
              className="px-3 py-1.5 rounded-lg border border-red-500/30 text-red-400 hover:bg-red-500/10 text-xs transition-colors">
              Deactivate
            </button>
          )}
          <button onClick={() => setExpanded(v => !v)}
            className="p-1.5 rounded-lg text-white/40 hover:text-white hover:bg-white/5 transition-colors">
            {expanded ? <ChevronDown className="w-4 h-4" /> : <ChevronRight className="w-4 h-4" />}
          </button>
        </div>
      </div>

      {expanded && (
        <div className="border-t border-white/5 p-4 space-y-2 bg-white/[0.02]">
          <p className="text-xs text-white/40 font-semibold uppercase tracking-wide">
            Sessions {sessions.length > 0 ? `(${sessions.length})` : ''}
          </p>
          {sessionsLoading && <p className="text-xs text-white/30">Loading sessions…</p>}
          {!sessionsLoading && sessions.length === 0 && (
            <p className="text-xs text-white/30">No sessions recorded yet. Teachers can add sessions from their portal.</p>
          )}
          {sessions.map(s => (
            <div key={s.id} className="flex items-center gap-3 p-2.5 bg-white/[0.03] rounded-lg">
              <Calendar className="w-4 h-4 text-brand-400 shrink-0" />
              <div className="flex-1 min-w-0">
                <p className="text-xs text-white">{new Date(s.sessionDate).toLocaleDateString()}{s.venue ? ` — ${s.venue}` : ''}</p>
                {s.notes && <p className="text-[10px] text-white/30 truncate">{s.notes}</p>}
              </div>
              <span className="text-xs text-white/30 shrink-0">{s.durationMins} mins</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ─── Create Activity Modal ───────────────────────────────────────────────────

function CreateActivityModal({ centerId, onClose, onCreated }: { centerId: string; onClose: () => void; onCreated: () => void }) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('SPORTS');
  const [maxParticipants, setMaxParticipants] = useState(30);
  const [scheduleDescription, setScheduleDescription] = useState('');

  const mutation = useMutation({
    mutationFn: () => api.post(`/api/v1/curricular/activities?centerId=${centerId}`, {
      name, description, category, maxParticipants, scheduleDescription,
    }),
    onSuccess: () => { toast.success('Activity created!'); onCreated(); onClose(); },
    onError: () => toast.error('Failed to create activity.'),
  });

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60">
      <div className="glass rounded-2xl p-6 w-full max-w-md border border-white/10 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-white font-semibold">New Extracurricular Activity</h2>
          <button onClick={onClose} className="text-white/40 hover:text-white"><X className="w-5 h-5" /></button>
        </div>
        <div className="space-y-3">
          <div>
            <label className="text-xs text-white/50 mb-1 block">Activity Name</label>
            <input value={name} onChange={e => setName(e.target.value)} placeholder="e.g. School Football Team"
              className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-white/50 mb-1 block">Category</label>
              <select value={category} onChange={e => setCategory(e.target.value)}
                className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500"
              >
                {ACTIVITY_CATEGORIES.map(c => <option key={c} value={c}>{c.replace('_', ' ')}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs text-white/50 mb-1 block">Max Participants</label>
              <input type="number" value={maxParticipants} onChange={e => setMaxParticipants(Number(e.target.value))} min={1} max={500}
                className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500"
              />
            </div>
          </div>
          <div>
            <label className="text-xs text-white/50 mb-1 block">Schedule</label>
            <input value={scheduleDescription} onChange={e => setScheduleDescription(e.target.value)}
              placeholder="e.g. Every Saturday 9am–11am"
              className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500"
            />
          </div>
          <div>
            <label className="text-xs text-white/50 mb-1 block">Description (optional)</label>
            <textarea value={description} onChange={e => setDescription(e.target.value)} rows={2}
              className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500 resize-none"
            />
          </div>
        </div>
        <div className="flex gap-2 pt-1">
          <button onClick={onClose} className="flex-1 py-2 rounded-xl border border-white/10 text-white/50 hover:text-white hover:bg-white/5 text-sm transition-colors">
            Cancel
          </button>
          <button onClick={() => mutation.mutate()} disabled={!name || mutation.isPending}
            className="flex-1 py-2 rounded-xl bg-brand-600 hover:bg-brand-500 text-white text-sm transition-colors disabled:opacity-40"
          >
            {mutation.isPending ? 'Creating…' : 'Create Activity'}
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Component ──────────────────────────────────────────────────────────────

export default function AdminCurricularPage() {
  const qc = useQueryClient();
  const [tab, setTab] = useState<Tab>('activities');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedBoardId, setSelectedBoardId] = useState('');
  const [selectedSubjectId, setSelectedSubjectId] = useState('');
  const [selectedGradeId, setSelectedGradeId] = useState('');

  // ── Fetch centers to get centerId (same pattern as AdminBatchesPage Fix #176) ─
  const { data: centers = [] } = useQuery<{ id: string }[]>({
    queryKey: ['centers'],
    queryFn: () =>
      api.get('/api/v1/centers').then((r) => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
  });

  const centerId = centers[0]?.id ?? '';

  // ── Activities ────────────────────────────────────────────────────────────
  const { data: activities = [], isLoading: activitiesLoading } = useQuery<ActivityResponse[]>({
    queryKey: ['admin-curricular-activities', centerId],
    queryFn: () => api.get(`/api/v1/curricular/activities?centerId=${centerId}`).then(r => Array.isArray(r.data) ? r.data : []),
    enabled: tab === 'activities' && !!centerId,
    retry: false,
  });

  const deactivateMutation = useMutation({
    mutationFn: (id: string) => api.delete(`/api/v1/curricular/activities/${id}`),
    onSuccess: () => { toast.success('Activity deactivated.'); qc.invalidateQueries({ queryKey: ['admin-curricular-activities'] }); },
    onError: () => toast.error('Failed to deactivate.'),
  });

  // ── Coverage Report ───────────────────────────────────────────────────────
  const { data: boards = [] } = useQuery<BoardResponse[]>({
    queryKey: ['curricular-boards'],
    queryFn: () => api.get('/api/v1/curricular/frameworks').then(r => Array.isArray(r.data) ? r.data : []),
    enabled: tab === 'coverage',
    staleTime: 10 * 60_000,
  });

  const { data: subjects = [] } = useQuery<SubjectResponse[]>({
    queryKey: ['curricular-subjects', selectedBoardId],
    queryFn: () => api.get(`/api/v1/curricular/frameworks/${selectedBoardId}/subjects`).then(r => Array.isArray(r.data) ? r.data : []),
    enabled: !!selectedBoardId && tab === 'coverage',
  });

  const { data: grades = [] } = useQuery<GradeLevelResponse[]>({
    queryKey: ['curricular-grades', selectedBoardId],
    queryFn: () => api.get(`/api/v1/curricular/frameworks/${selectedBoardId}/grades`).then(r => Array.isArray(r.data) ? r.data : []),
    enabled: !!selectedBoardId && tab === 'coverage',
  });

  const reportEnabled = !!centerId && !!selectedSubjectId && !!selectedGradeId;
  const { data: report, isLoading: reportLoading } = useQuery<CenterCoverageReport>({
    queryKey: ['center-coverage-report', centerId, selectedSubjectId, selectedGradeId],
    queryFn: () =>
      api.get(`/api/v1/curricular/center/${centerId}/report?subjectId=${selectedSubjectId}&gradeId=${selectedGradeId}`).then(r => r.data),
    enabled: reportEnabled && tab === 'coverage',
    retry: false,
  });

  // ── Calendar ──────────────────────────────────────────────────────────────
  const { data: calendar = [], isLoading: calendarLoading } = useQuery<AcademicYearResponse[]>({
    queryKey: ['admin-curricular-calendar', centerId],
    queryFn: () => api.get(`/api/v1/curricular/center/${centerId}/calendar`).then(r => Array.isArray(r.data) ? r.data : []),
    enabled: tab === 'calendar' && !!centerId,
    retry: false,
  });

  const handleBoardChange = (boardId: string) => {
    setSelectedBoardId(boardId);
    setSelectedSubjectId('');
    setSelectedGradeId('');
  };

  const activeCount = activities.filter(a => a.active).length;

  return (
    <div className="p-6 max-w-5xl mx-auto space-y-6">
      {showCreateModal && centerId && (
        <CreateActivityModal
          centerId={centerId}
          onClose={() => setShowCreateModal(false)}
          onCreated={() => qc.invalidateQueries({ queryKey: ['admin-curricular-activities'] })}
        />
      )}

      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <Layers className="w-6 h-6 text-brand-400" /> Curricular Management
          </h1>
          <p className="text-sm text-white/40 mt-1">Manage activities, track coverage, and configure academic calendar</p>
        </div>
        {tab === 'activities' && (
          <button
            onClick={() => setShowCreateModal(true)}
            disabled={!centerId}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-brand-600 hover:bg-brand-500 text-white text-sm transition-colors disabled:opacity-40"
          >
            <Plus className="w-4 h-4" /> New Activity
          </button>
        )}
      </div>

      {/* Tabs */}
      <div className="flex gap-1 bg-surface-100/40 border border-white/5 rounded-xl p-1 w-fit">
        {([
          { id: 'activities', label: 'Activities',  Icon: Activity },
          { id: 'coverage',   label: 'Coverage',    Icon: BarChart3 },
          { id: 'calendar',   label: 'Calendar',    Icon: Calendar },
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
        <div className="space-y-4">
          {/* Stats */}
          {activities.length > 0 && (
            <div className="grid grid-cols-2 gap-3">
              <div className="p-4 bg-surface-100/40 border border-white/5 rounded-xl text-center">
                <p className="text-2xl font-bold text-white">{activeCount}</p>
                <p className="text-xs text-white/40 mt-1">Active activities</p>
              </div>
              <div className="p-4 bg-surface-100/40 border border-white/5 rounded-xl text-center">
                <p className="text-2xl font-bold text-white">{activities.length}</p>
                <p className="text-xs text-white/40 mt-1">Total created</p>
              </div>
            </div>
          )}

          {activitiesLoading && <div className="text-white/40 text-sm py-8 text-center">Loading…</div>}
          {!activitiesLoading && !centerId && (
            <div className="text-center py-10 text-white/30 text-sm">No center assigned to your account.</div>
          )}
          {!activitiesLoading && centerId && activities.length === 0 && (
            <div className="flex flex-col items-center py-16 text-white/30">
              <Activity className="w-12 h-12 mb-3" />
              <p className="text-sm">No activities yet. Create one to get started.</p>
            </div>
          )}

          <div className="space-y-3">
            {activities.map(a => (
              <AdminActivityRow
                key={a.id}
                a={a}
                onDeactivate={id => deactivateMutation.mutate(id)}
                deactivating={deactivateMutation.isPending}
              />
            ))}
          </div>
        </div>
      )}

      {/* ── Coverage Tab ── */}
      {tab === 'coverage' && (
        <div className="space-y-4">
          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="text-xs text-white/50 mb-1 block">Board</label>
              <select value={selectedBoardId} onChange={e => handleBoardChange(e.target.value)}
                className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500"
              >
                <option value="">Select board…</option>
                {boards.map(b => <option key={b.id} value={b.id}>{b.boardName}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs text-white/50 mb-1 block">Subject</label>
              <select value={selectedSubjectId} onChange={e => setSelectedSubjectId(e.target.value)} disabled={!selectedBoardId}
                className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500 disabled:opacity-40"
              >
                <option value="">Select subject…</option>
                {subjects.map(s => <option key={s.id} value={s.id}>{s.subjectName}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs text-white/50 mb-1 block">Grade</label>
              <select value={selectedGradeId} onChange={e => setSelectedGradeId(e.target.value)} disabled={!selectedBoardId}
                className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500 disabled:opacity-40"
              >
                <option value="">Select grade…</option>
                {[...grades].sort((a, b) => a.sortOrder - b.sortOrder).map(g => (
                  <option key={g.id} value={g.id}>{g.displayName}</option>
                ))}
              </select>
            </div>
          </div>

          {!reportEnabled && (
            <div className="flex flex-col items-center py-12 text-white/30">
              <BarChart3 className="w-12 h-12 mb-3" />
              <p className="text-sm">Select board, subject, and grade to view coverage report</p>
            </div>
          )}

          {reportEnabled && reportLoading && (
            <div className="text-white/40 text-sm py-8 text-center">Loading report…</div>
          )}

          {reportEnabled && report && (
            <div className="space-y-4">
              {/* Coverage circle */}
              <div className="p-6 bg-surface-100/40 border border-white/5 rounded-xl">
                <div className="flex items-center gap-6">
                  <div className="relative w-24 h-24 shrink-0">
                    <svg className="w-24 h-24 -rotate-90" viewBox="0 0 36 36">
                      <path d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                        fill="none" stroke="rgba(255,255,255,0.05)" strokeWidth="3" />
                      <path d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                        fill="none"
                        stroke={report.coveragePercent >= 70 ? '#22c55e' : report.coveragePercent >= 40 ? '#eab308' : '#ef4444'}
                        strokeWidth="3"
                        strokeDasharray={`${report.coveragePercent}, 100`}
                      />
                    </svg>
                    <div className="absolute inset-0 flex items-center justify-center">
                      <span className="text-lg font-bold text-white">{Math.round(report.coveragePercent)}%</span>
                    </div>
                  </div>
                  <div className="flex-1">
                    <p className="text-white font-semibold text-lg">Coverage Report</p>
                    <div className="mt-2 space-y-1">
                      <div className="flex items-center gap-2 text-sm">
                        <CheckCircle2 className="w-4 h-4 text-green-400" />
                        <span className="text-white/70">{report.coveredTopics} of {report.totalTopics} topics covered</span>
                      </div>
                      {report.uncoveredTopicCodes.length > 0 && (
                        <div className="flex items-start gap-2 text-sm">
                          <AlertCircle className="w-4 h-4 text-red-400 mt-0.5 shrink-0" />
                          <span className="text-white/50">{report.totalTopics - report.coveredTopics} topics pending</span>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </div>

              {/* Uncovered topics */}
              {report.uncoveredTopicCodes.length > 0 && (
                <div className="p-4 bg-red-500/5 border border-red-500/10 rounded-xl">
                  <p className="text-xs font-semibold text-red-400 mb-2">Uncovered Topics</p>
                  <div className="flex flex-wrap gap-2">
                    {report.uncoveredTopicCodes.map(code => (
                      <span key={code} className="text-xs px-2 py-0.5 rounded-full bg-red-500/20 text-red-300">{code}</span>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* ── Calendar Tab ── */}
      {tab === 'calendar' && (
        <div className="space-y-3">
          {calendarLoading && <div className="text-white/40 text-sm py-8 text-center">Loading…</div>}
          {!calendarLoading && calendar.length === 0 && (
            <div className="flex flex-col items-center py-16 text-white/30">
              <Calendar className="w-12 h-12 mb-3" />
              <p className="text-sm">No academic years configured yet.</p>
              <p className="text-xs mt-1 text-white/20">Use the API to create: POST /api/v1/curricular/center/{'{centerId}'}/calendar</p>
            </div>
          )}
          {calendar.map(y => (
            <div key={y.id} className={cn('p-4 border rounded-xl', y.active ? 'bg-green-500/5 border-green-500/20' : 'bg-surface-100/40 border-white/5')}>
              <div className="flex items-center justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <p className="text-sm font-semibold text-white">{y.yearLabel}</p>
                    {y.active && <span className="text-[10px] px-2 py-0.5 rounded-full bg-green-500/20 text-green-300">Active</span>}
                  </div>
                  <p className="text-xs text-white/40 mt-1">
                    {new Date(y.startDate).toLocaleDateString()} — {new Date(y.endDate).toLocaleDateString()}
                  </p>
                </div>
                <Calendar className="w-5 h-5 text-white/20" />
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
