import { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import {
  GraduationCap, BookOpen, Award, Trophy, RefreshCw,
  CheckCircle2, Layers, Star, Activity, UserPlus, UserMinus,
  Filter,
} from 'lucide-react';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { cn } from '../../lib/utils';

// ─── Types ───────────────────────────────────────────────────────────────────

interface GradeResponse        { centerId: string; }
interface BoardResponse        { id: string; boardCode: string; boardName: string; }
interface SubjectResponse      { id: string; subjectCode: string; subjectName: string; }
interface GradeLevelResponse   { id: string; gradeCode: string; displayName: string; sortOrder: number; }
interface LearningPathItem     { itemId: string; topicTitle: string; bloomsLevel: string; sequenceOrder: number; urgencyScore: number; status: string; }
interface LearningPathResponse { pathId: string; subjectId: string; gradeId: string; computedAt: string; items: LearningPathItem[]; }
interface ActivityResponse     { id: string; centerId: string; name: string; description: string; category: string; maxParticipants: number; scheduleDescription: string; active: boolean; }
interface ActivityEnrollmentResponse { enrollmentId: string; activityId: string; activityName: string; category: string; status: string; enrolledAt: string; }
interface AchievementResponse  { id: string; activityId: string; activityName: string; achievementType: string; title: string; issuedAt: string; }

// ─── Constants ────────────────────────────────────────────────────────────────

const CATEGORIES = ['ALL', 'SPORTS', 'ARTS', 'MUSIC', 'DANCE', 'DRAMA', 'CODING_CLUB', 'DEBATE', 'SCIENCE_CLUB', 'COMMUNITY_SERVICE', 'LEADERSHIP', 'CULTURAL_EVENTS', 'OTHER'];

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

const BLOOMS_COLORS: Record<string, string> = {
  REMEMBER: 'bg-slate-500/20 text-slate-300', UNDERSTAND: 'bg-blue-500/20 text-blue-300',
  APPLY: 'bg-green-500/20 text-green-300',    ANALYZE: 'bg-yellow-500/20 text-yellow-300',
  EVALUATE: 'bg-orange-500/20 text-orange-300', CREATE: 'bg-purple-500/20 text-purple-300',
};

const STATUS_COLORS: Record<string, string> = {
  PENDING: 'bg-slate-500/20 text-slate-300', IN_PROGRESS: 'bg-blue-500/20 text-blue-300',
  COMPLETED: 'bg-green-500/20 text-green-300', ENROLLED: 'bg-green-500/20 text-green-300',
  WAITLISTED: 'bg-yellow-500/20 text-yellow-300', WITHDRAWN: 'bg-red-500/20 text-red-300',
};

const ACHIEVEMENT_ICON: Record<string, string> = {
  GOLD: '🥇', SILVER: '🥈', BRONZE: '🥉', EXCELLENCE: '⭐',
  MERIT: '🏅', PARTICIPATION: '🎖️', SPECIAL: '🏆',
};

function urgencyColor(s: number) { return s >= 70 ? 'text-red-400' : s >= 40 ? 'text-yellow-400' : 'text-green-400'; }
function urgencyBg(s: number)    { return s >= 70 ? 'bg-red-500/10 border-red-500/20' : s >= 40 ? 'bg-yellow-500/10 border-yellow-500/20' : 'bg-green-500/10 border-green-500/20'; }

function resolveCenterId(grades: GradeResponse[]): string {
  if (!grades.length) return '';
  const freq: Record<string, number> = {};
  grades.forEach(g => { if (g.centerId) freq[g.centerId] = (freq[g.centerId] ?? 0) + 1; });
  return Object.entries(freq).sort((a, b) => b[1] - a[1])[0]?.[0] ?? '';
}

type Tab = 'path' | 'browse' | 'enrolled' | 'achievements';

// ─── Component ────────────────────────────────────────────────────────────────

export default function StudentCurricularPage() {
  const { user } = useAuthStore();
  const qc = useQueryClient();
  const studentId = user?.id ?? '';

  const [tab, setTab]                     = useState<Tab>('browse');
  const [selectedBoardId, setSelectedBoardId]     = useState('');
  const [selectedSubjectId, setSelectedSubjectId] = useState('');
  const [selectedGradeId, setSelectedGradeId]     = useState('');
  const [categoryFilter, setCategoryFilter]       = useState('ALL');

  // ── centerId from grades ─────────────────────────────────────────────────
  const { data: grades = [] } = useQuery<GradeResponse[]>({
    queryKey: ['student-grades-curricular', studentId],
    queryFn:  () => api.get(`/api/v1/grades/student/${studentId}`).then(r => Array.isArray(r.data) ? r.data : []),
    enabled:  !!studentId,
    staleTime: 5 * 60_000,
  });
  const centerId = resolveCenterId(grades);

  // ── Curriculum framework ─────────────────────────────────────────────────
  const { data: boards = [] }   = useQuery<BoardResponse[]>({
    queryKey: ['curricular-boards'],
    queryFn:  () => api.get('/api/v1/curricular/frameworks').then(r => Array.isArray(r.data) ? r.data : []),
    staleTime: 10 * 60_000,
    enabled: tab === 'path',
  });
  const { data: subjects = [] } = useQuery<SubjectResponse[]>({
    queryKey: ['curricular-subjects', selectedBoardId],
    queryFn:  () => api.get(`/api/v1/curricular/frameworks/${selectedBoardId}/subjects`).then(r => Array.isArray(r.data) ? r.data : []),
    enabled:  !!selectedBoardId && tab === 'path',
  });
  const { data: grades2 = [] }  = useQuery<GradeLevelResponse[]>({
    queryKey: ['curricular-grades', selectedBoardId],
    queryFn:  () => api.get(`/api/v1/curricular/frameworks/${selectedBoardId}/grades`).then(r => Array.isArray(r.data) ? r.data : []),
    enabled:  !!selectedBoardId && tab === 'path',
  });

  // ── Learning Path ────────────────────────────────────────────────────────
  const pathEnabled = !!selectedSubjectId && !!selectedGradeId;
  const { data: path, isLoading: pathLoading, isFetching: pathFetching } = useQuery<LearningPathResponse>({
    queryKey: ['curricular-my-path', selectedSubjectId, selectedGradeId],
    queryFn:  () => api.get(`/api/v1/curricular/my-path?subjectId=${selectedSubjectId}&gradeId=${selectedGradeId}`).then(r => r.data),
    enabled:  pathEnabled && tab === 'path',
    retry: false,
  });

  const refreshMutation = useMutation({
    mutationFn: () => api.post(`/api/v1/curricular/my-path/refresh?subjectId=${selectedSubjectId}&gradeId=${selectedGradeId}`),
    onSuccess: () => { toast.success('Path refreshed!'); qc.invalidateQueries({ queryKey: ['curricular-my-path', selectedSubjectId, selectedGradeId] }); },
    onError: () => toast.error('Refresh failed.'),
  });
  const markMutation = useMutation({
    mutationFn: ({ itemId, status }: { itemId: string; status: string }) =>
      api.patch(`/api/v1/curricular/my-path/items/${itemId}`, { status }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['curricular-my-path', selectedSubjectId, selectedGradeId] }); },
    onError: () => toast.error('Failed to update.'),
  });

  // ── All center activities (browse) ───────────────────────────────────────
  const categoryParam = categoryFilter !== 'ALL' ? `&category=${categoryFilter}` : '';
  const { data: allActivities = [], isLoading: browseLoading } = useQuery<ActivityResponse[]>({
    queryKey: ['curricular-activities-browse', centerId, categoryFilter],
    queryFn:  () => api.get(`/api/v1/curricular/activities?centerId=${centerId}${categoryParam}`).then(r => Array.isArray(r.data) ? r.data : []),
    enabled:  !!centerId && tab === 'browse',
    retry: false,
  });

  // ── My enrolled activities ───────────────────────────────────────────────
  const { data: enrolled = [], isLoading: enrolledLoading } = useQuery<ActivityEnrollmentResponse[]>({
    queryKey: ['curricular-my-activities', studentId],
    queryFn:  () => api.get('/api/v1/curricular/my-activities').then(r => Array.isArray(r.data) ? r.data : []),
    enabled:  tab === 'enrolled' || tab === 'browse',
    retry: false,
  });
  const enrolledActivityIds = useMemo(() => new Set(enrolled.map(e => e.activityId)), [enrolled]);

  // ── Enroll / Withdraw ────────────────────────────────────────────────────
  const enrollMutation = useMutation({
    mutationFn: (activityId: string) => api.post(`/api/v1/curricular/activities/${activityId}/enroll`),
    onSuccess: () => {
      toast.success('Enrolled successfully!');
      qc.invalidateQueries({ queryKey: ['curricular-my-activities'] });
      qc.invalidateQueries({ queryKey: ['curricular-activities-browse'] });
    },
    onError: (err: any) => toast.error(err?.response?.data?.message ?? 'Enrollment failed.'),
  });
  const withdrawMutation = useMutation({
    mutationFn: (activityId: string) => api.delete(`/api/v1/curricular/activities/${activityId}/enroll`),
    onSuccess: () => {
      toast.success('Withdrawn from activity.');
      qc.invalidateQueries({ queryKey: ['curricular-my-activities'] });
      qc.invalidateQueries({ queryKey: ['curricular-activities-browse'] });
    },
    onError: () => toast.error('Withdrawal failed.'),
  });

  // ── Achievements ─────────────────────────────────────────────────────────
  const { data: achievements = [], isLoading: achievementsLoading } = useQuery<AchievementResponse[]>({
    queryKey: ['curricular-my-achievements', studentId],
    queryFn:  () => api.get('/api/v1/curricular/my-achievements').then(r => Array.isArray(r.data) ? r.data : []),
    enabled:  tab === 'achievements',
    retry: false,
  });

  const handleBoardChange = (id: string) => { setSelectedBoardId(id); setSelectedSubjectId(''); setSelectedGradeId(''); };

  // ─── Activity card (reused in browse + enrolled) ──────────────────────────
  const ActivityCard = ({ a, isEnrolled }: { a: ActivityResponse; isEnrolled: boolean }) => (
    <div className={cn(
      'p-4 rounded-xl border transition-all',
      isEnrolled ? 'bg-brand-500/5 border-brand-500/20' : 'bg-surface-100/40 border-white/5 hover:border-white/10',
    )}>
      <div className="flex items-start gap-3">
        <div className={cn('w-11 h-11 rounded-xl flex items-center justify-center text-xl shrink-0',
          CATEGORY_COLORS[a.category]?.split(' ')[0] ?? 'bg-slate-500/20'
        )}>
          {CATEGORY_ICONS[a.category] ?? '✨'}
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <p className="text-sm font-semibold text-white">{a.name}</p>
            <span className={cn('text-[10px] px-2 py-0.5 rounded-full border', CATEGORY_COLORS[a.category] ?? 'bg-slate-500/20 text-slate-300 border-slate-500/30')}>
              {a.category.replace(/_/g, ' ')}
            </span>
            {isEnrolled && <span className="text-[10px] px-2 py-0.5 rounded-full bg-brand-500/20 text-brand-300 border border-brand-500/30">✓ Enrolled</span>}
          </div>
          {a.description && <p className="text-xs text-white/40 mt-1 line-clamp-2">{a.description}</p>}
          <div className="flex items-center gap-4 mt-2 text-xs text-white/30">
            <span>👥 {a.maxParticipants} max</span>
            {a.scheduleDescription && <span>📅 {a.scheduleDescription}</span>}
          </div>
        </div>
        <div className="shrink-0">
          {isEnrolled ? (
            <button
              onClick={() => withdrawMutation.mutate(a.id)}
              disabled={withdrawMutation.isPending}
              className="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400 hover:bg-red-500/20 text-xs transition-colors disabled:opacity-40"
            >
              <UserMinus className="w-3.5 h-3.5" /> Leave
            </button>
          ) : (
            <button
              onClick={() => enrollMutation.mutate(a.id)}
              disabled={enrollMutation.isPending}
              className="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-brand-600 hover:bg-brand-500 text-white text-xs transition-colors disabled:opacity-40"
            >
              <UserPlus className="w-3.5 h-3.5" /> Join
            </button>
          )}
        </div>
      </div>
    </div>
  );

  return (
    <div className="p-6 max-w-5xl mx-auto space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-white flex items-center gap-2">
          <Layers className="w-6 h-6 text-brand-400" /> Curricular Hub
        </h1>
        <p className="text-sm text-white/40 mt-1">Explore activities, track your learning path, and celebrate achievements</p>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 bg-surface-100/40 border border-white/5 rounded-xl p-1 w-fit flex-wrap">
        {([
          { id: 'browse',       label: 'Browse Activities', Icon: Activity },
          { id: 'enrolled',     label: `My Activities${enrolled.length ? ` (${enrolled.length})` : ''}`, Icon: CheckCircle2 },
          { id: 'path',         label: 'Learning Path',     Icon: BookOpen },
          { id: 'achievements', label: 'Achievements',      Icon: Trophy },
        ] as const).map(({ id, label, Icon }) => (
          <button key={id} onClick={() => setTab(id)}
            className={cn('flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium transition-colors',
              tab === id ? 'bg-brand-600 text-white' : 'text-white/50 hover:text-white/70 hover:bg-white/5'
            )}>
            <Icon className="w-3.5 h-3.5" /> {label}
          </button>
        ))}
      </div>

      {/* ── Browse Activities ── */}
      {tab === 'browse' && (
        <div className="space-y-4">
          {/* Category filter chips */}
          <div className="flex gap-2 flex-wrap">
            {CATEGORIES.map(cat => (
              <button key={cat}
                onClick={() => setCategoryFilter(cat)}
                className={cn(
                  'flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-medium border transition-colors',
                  categoryFilter === cat
                    ? 'bg-brand-600 text-white border-brand-500'
                    : cat === 'ALL'
                      ? 'bg-white/5 text-white/50 border-white/10 hover:bg-white/10'
                      : cn('border', CATEGORY_COLORS[cat] ?? 'bg-slate-500/20 text-slate-300 border-slate-500/30', 'opacity-70 hover:opacity-100'),
                )}>
                {cat !== 'ALL' && <span>{CATEGORY_ICONS[cat]}</span>}
                {cat === 'ALL' ? 'All' : cat.replace(/_/g, ' ')}
              </button>
            ))}
          </div>

          {!centerId && !browseLoading && (
            <div className="text-center py-12 text-white/30 text-sm">
              <Activity className="w-10 h-10 mx-auto mb-3 opacity-50" />
              No center found. Please contact your administrator.
            </div>
          )}
          {browseLoading && <div className="text-white/40 text-sm py-8 text-center">Loading activities…</div>}

          {!browseLoading && centerId && allActivities.filter(a => a.active).length === 0 && (
            <div className="flex flex-col items-center py-16 text-white/30">
              <Activity className="w-12 h-12 mb-3" />
              <p className="text-sm">No {categoryFilter !== 'ALL' ? categoryFilter.replace(/_/g, ' ').toLowerCase() : ''} activities at your center yet.</p>
            </div>
          )}

          <div className="space-y-3">
            {allActivities.filter(a => a.active).map(a => (
              <ActivityCard key={a.id} a={a} isEnrolled={enrolledActivityIds.has(a.id)} />
            ))}
          </div>
        </div>
      )}

      {/* ── My Enrolled Activities ── */}
      {tab === 'enrolled' && (
        <div className="space-y-3">
          {enrolledLoading && <div className="text-white/40 text-sm py-8 text-center">Loading…</div>}
          {!enrolledLoading && enrolled.length === 0 && (
            <div className="flex flex-col items-center py-16 text-white/30">
              <CheckCircle2 className="w-12 h-12 mb-3" />
              <p className="text-sm">Not enrolled in any activities yet.</p>
              <button onClick={() => setTab('browse')} className="mt-3 text-brand-400 text-sm hover:underline">
                Browse available activities →
              </button>
            </div>
          )}
          {enrolled.map(e => {
            const full = allActivities.find(a => a.id === e.activityId);
            return (
              <div key={e.enrollmentId} className="p-4 bg-brand-500/5 border border-brand-500/20 rounded-xl">
                <div className="flex items-start gap-3">
                  <div className={cn('w-11 h-11 rounded-xl flex items-center justify-center text-xl shrink-0',
                    CATEGORY_COLORS[e.category]?.split(' ')[0] ?? 'bg-slate-500/20'
                  )}>
                    {CATEGORY_ICONS[e.category] ?? '✨'}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-white">{e.activityName}</p>
                    <div className="flex items-center gap-2 mt-1 flex-wrap">
                      <span className={cn('text-[10px] px-2 py-0.5 rounded-full border', CATEGORY_COLORS[e.category] ?? 'bg-slate-500/20 text-slate-300 border-slate-500/30')}>
                        {e.category.replace(/_/g, ' ')}
                      </span>
                      <span className={cn('text-[10px] px-2 py-0.5 rounded-full', STATUS_COLORS[e.status] ?? 'bg-slate-500/20 text-slate-300')}>
                        {e.status}
                      </span>
                      <span className="text-xs text-white/30">Joined {new Date(e.enrolledAt).toLocaleDateString()}</span>
                    </div>
                    {full?.scheduleDescription && (
                      <p className="text-xs text-white/30 mt-1">📅 {full.scheduleDescription}</p>
                    )}
                  </div>
                  <button
                    onClick={() => withdrawMutation.mutate(e.activityId)}
                    disabled={withdrawMutation.isPending}
                    className="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400 hover:bg-red-500/20 text-xs transition-colors shrink-0"
                  >
                    <UserMinus className="w-3.5 h-3.5" /> Leave
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* ── Learning Path ── */}
      {tab === 'path' && (
        <div className="space-y-4">
          <div className="grid grid-cols-3 gap-3">
            {[
              { label: 'Board', value: selectedBoardId, setter: handleBoardChange, options: boards.map(b => ({ id: b.id, label: b.boardName })) },
              { label: 'Subject', value: selectedSubjectId, setter: setSelectedSubjectId, options: subjects.map(s => ({ id: s.id, label: s.subjectName })), disabled: !selectedBoardId },
              { label: 'Grade', value: selectedGradeId, setter: setSelectedGradeId, options: [...grades2].sort((a,b) => a.sortOrder-b.sortOrder).map(g => ({ id: g.id, label: g.displayName })), disabled: !selectedBoardId },
            ].map(({ label, value, setter, options, disabled }) => (
              <div key={label}>
                <label className="text-xs text-white/50 mb-1 block">{label}</label>
                <select value={value} onChange={e => setter(e.target.value)} disabled={disabled}
                  className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500 disabled:opacity-40">
                  <option value="">Select {label.toLowerCase()}…</option>
                  {options.map(o => <option key={o.id} value={o.id}>{o.label}</option>)}
                </select>
              </div>
            ))}
          </div>

          {!pathEnabled && <div className="flex flex-col items-center py-16 text-white/30"><GraduationCap className="w-12 h-12 mb-3" /><p className="text-sm">Select board, subject and grade to see your adaptive learning path</p></div>}
          {pathEnabled && pathLoading && <div className="text-white/40 text-sm py-8 text-center">Computing path…</div>}

          {pathEnabled && path && (
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex gap-4 text-xs">
                  <span className="text-green-400">{path.items.filter(i=>i.status==='COMPLETED').length} done</span>
                  <span className="text-blue-400">{path.items.filter(i=>i.status==='IN_PROGRESS').length} in progress</span>
                  <span className="text-white/30">{path.items.filter(i=>i.status==='PENDING').length} pending</span>
                </div>
                <button onClick={() => refreshMutation.mutate()} disabled={refreshMutation.isPending || pathFetching}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-brand-500/30 text-brand-400 hover:bg-brand-500/10 text-xs disabled:opacity-40">
                  <RefreshCw className={cn('w-3.5 h-3.5', (refreshMutation.isPending||pathFetching) && 'animate-spin')} /> Refresh
                </button>
              </div>
              {path.items.length > 0 && (
                <div className="w-full bg-white/5 rounded-full h-2">
                  <div className="bg-brand-500 rounded-full h-2 transition-all"
                    style={{ width: `${(path.items.filter(i=>i.status==='COMPLETED').length/path.items.length)*100}%` }} />
                </div>
              )}
              <div className="space-y-2">
                {path.items.map((item, idx) => (
                  <div key={item.itemId} className={cn('flex items-center gap-3 p-3 rounded-xl border',
                    item.status==='COMPLETED' ? 'bg-green-500/5 border-green-500/20 opacity-60' : urgencyBg(item.urgencyScore)
                  )}>
                    <span className="text-white/30 text-xs w-5 text-right shrink-0">{idx+1}</span>
                    <div className="flex-1 min-w-0">
                      <p className={cn('text-sm font-medium', item.status==='COMPLETED' ? 'text-white/40 line-through' : 'text-white')}>{item.topicTitle}</p>
                      <div className="flex gap-2 mt-1">
                        <span className={cn('text-[10px] px-2 py-0.5 rounded-full', BLOOMS_COLORS[item.bloomsLevel] ?? 'bg-slate-500/20 text-slate-300')}>{item.bloomsLevel}</span>
                      </div>
                    </div>
                    <span className={cn('text-xs font-bold shrink-0', urgencyColor(item.urgencyScore))}>{Math.round(item.urgencyScore)}</span>
                    {item.status !== 'COMPLETED' ? (
                      <button onClick={() => markMutation.mutate({ itemId: item.itemId, status: 'COMPLETED' })}
                        disabled={markMutation.isPending}
                        className="p-1.5 rounded-lg bg-green-500/10 text-green-400 hover:bg-green-500/20 shrink-0">
                        <CheckCircle2 className="w-4 h-4" />
                      </button>
                    ) : <CheckCircle2 className="w-4 h-4 text-green-500 shrink-0" />}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* ── Achievements ── */}
      {tab === 'achievements' && (
        <div className="space-y-3">
          {achievementsLoading && <div className="text-white/40 text-sm py-8 text-center">Loading…</div>}
          {!achievementsLoading && achievements.length === 0 && (
            <div className="flex flex-col items-center py-16 text-white/30"><Trophy className="w-12 h-12 mb-3" /><p className="text-sm">No achievements yet — keep going!</p></div>
          )}
          <div className="grid gap-3 sm:grid-cols-2">
            {achievements.map(a => (
              <div key={a.id} className="p-4 bg-gradient-to-br from-yellow-500/10 to-orange-500/5 border border-yellow-500/20 rounded-xl">
                <div className="flex items-start gap-3">
                  <span className="text-3xl shrink-0">{ACHIEVEMENT_ICON[a.achievementType] ?? '🏅'}</span>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-white">{a.title}</p>
                    <p className="text-xs text-white/50 mt-0.5">{a.activityName}</p>
                    <div className="flex items-center gap-2 mt-2">
                      <span className="text-[10px] px-2 py-0.5 rounded-full bg-yellow-500/20 text-yellow-300">{a.achievementType}</span>
                      <span className="text-[10px] text-white/30">{new Date(a.issuedAt).toLocaleDateString()}</span>
                    </div>
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
