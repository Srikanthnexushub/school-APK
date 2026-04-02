import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import {
  GraduationCap, BookOpen, Award, Trophy, RefreshCw,
  CheckCircle2, Clock, Layers, ChevronRight, Star, Zap, Activity,
} from 'lucide-react';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { cn } from '../../lib/utils';

// ─── Types ──────────────────────────────────────────────────────────────────

interface BoardResponse    { id: string; boardCode: string; boardName: string; }
interface SubjectResponse  { id: string; subjectCode: string; subjectName: string; }
interface GradeLevelResponse { id: string; gradeCode: string; displayName: string; sortOrder: number; }

interface LearningPathItem {
  itemId: string; topicId: string; topicTitle: string; bloomsLevel: string;
  sequenceOrder: number; urgencyScore: number; status: string;
}
interface LearningPathResponse {
  pathId: string; subjectId: string; gradeId: string;
  computedAt: string; items: LearningPathItem[];
}

interface ActivityEnrollmentResponse {
  enrollmentId: string; activityId: string; activityName: string;
  category: string; status: string; enrolledAt: string;
}
interface AchievementResponse {
  id: string; activityId: string; activityName: string;
  achievementType: string; title: string; issuedAt: string;
}

// ─── Helpers ────────────────────────────────────────────────────────────────

const BLOOMS_COLORS: Record<string, string> = {
  REMEMBER:   'bg-slate-500/20 text-slate-300',
  UNDERSTAND: 'bg-blue-500/20 text-blue-300',
  APPLY:      'bg-green-500/20 text-green-300',
  ANALYZE:    'bg-yellow-500/20 text-yellow-300',
  EVALUATE:   'bg-orange-500/20 text-orange-300',
  CREATE:     'bg-purple-500/20 text-purple-300',
};

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

const STATUS_COLORS: Record<string, string> = {
  PENDING:    'bg-slate-500/20 text-slate-300',
  IN_PROGRESS:'bg-blue-500/20 text-blue-300',
  COMPLETED:  'bg-green-500/20 text-green-300',
  SKIPPED:    'bg-red-500/20 text-red-300',
  ENROLLED:   'bg-green-500/20 text-green-300',
  WAITLISTED: 'bg-yellow-500/20 text-yellow-300',
  WITHDRAWN:  'bg-red-500/20 text-red-300',
};

function urgencyColor(score: number): string {
  if (score >= 70) return 'text-red-400';
  if (score >= 40) return 'text-yellow-400';
  return 'text-green-400';
}

function urgencyBg(score: number): string {
  if (score >= 70) return 'bg-red-500/10 border-red-500/20';
  if (score >= 40) return 'bg-yellow-500/10 border-yellow-500/20';
  return 'bg-green-500/10 border-green-500/20';
}

type Tab = 'path' | 'activities' | 'achievements';

// ─── Component ──────────────────────────────────────────────────────────────

export default function StudentCurricularPage() {
  const { user } = useAuthStore();
  const qc = useQueryClient();
  const [tab, setTab] = useState<Tab>('path');
  const [selectedBoardId, setSelectedBoardId] = useState('');
  const [selectedSubjectId, setSelectedSubjectId] = useState('');
  const [selectedGradeId, setSelectedGradeId] = useState('');

  // ── Frameworks ────────────────────────────────────────────────────────────
  const { data: boards = [] } = useQuery<BoardResponse[]>({
    queryKey: ['curricular-boards'],
    queryFn: () => api.get('/api/v1/curricular/frameworks').then(r => Array.isArray(r.data) ? r.data : []),
    staleTime: 10 * 60_000,
  });

  const { data: subjects = [] } = useQuery<SubjectResponse[]>({
    queryKey: ['curricular-subjects', selectedBoardId],
    queryFn: () => api.get(`/api/v1/curricular/frameworks/${selectedBoardId}/subjects`).then(r => Array.isArray(r.data) ? r.data : []),
    enabled: !!selectedBoardId,
  });

  const { data: grades = [] } = useQuery<GradeLevelResponse[]>({
    queryKey: ['curricular-grades', selectedBoardId],
    queryFn: () => api.get(`/api/v1/curricular/frameworks/${selectedBoardId}/grades`).then(r => Array.isArray(r.data) ? r.data : []),
    enabled: !!selectedBoardId,
  });

  // ── Learning Path ─────────────────────────────────────────────────────────
  const pathEnabled = !!selectedSubjectId && !!selectedGradeId;
  const { data: path, isLoading: pathLoading, isFetching: pathFetching } = useQuery<LearningPathResponse>({
    queryKey: ['curricular-my-path', selectedSubjectId, selectedGradeId],
    queryFn: () =>
      api.get(`/api/v1/curricular/my-path?subjectId=${selectedSubjectId}&gradeId=${selectedGradeId}`).then(r => r.data),
    enabled: pathEnabled && tab === 'path',
    retry: false,
  });

  const refreshMutation = useMutation({
    mutationFn: () =>
      api.post(`/api/v1/curricular/my-path/refresh?subjectId=${selectedSubjectId}&gradeId=${selectedGradeId}`).then(r => r.data),
    onSuccess: () => {
      toast.success('Learning path refreshed!');
      qc.invalidateQueries({ queryKey: ['curricular-my-path', selectedSubjectId, selectedGradeId] });
    },
    onError: () => toast.error('Refresh failed.'),
  });

  const markMutation = useMutation({
    mutationFn: ({ itemId, status }: { itemId: string; status: string }) =>
      api.patch(`/api/v1/curricular/my-path/items/${itemId}`, { status }),
    onSuccess: () => {
      toast.success('Progress updated!');
      qc.invalidateQueries({ queryKey: ['curricular-my-path', selectedSubjectId, selectedGradeId] });
    },
    onError: () => toast.error('Failed to update.'),
  });

  // ── Activities ────────────────────────────────────────────────────────────
  const { data: activities = [], isLoading: activitiesLoading } = useQuery<ActivityEnrollmentResponse[]>({
    queryKey: ['curricular-my-activities', user?.id],
    queryFn: () => api.get('/api/v1/curricular/my-activities').then(r => Array.isArray(r.data) ? r.data : []),
    enabled: tab === 'activities',
    retry: false,
  });

  // ── Achievements ──────────────────────────────────────────────────────────
  const { data: achievements = [], isLoading: achievementsLoading } = useQuery<AchievementResponse[]>({
    queryKey: ['curricular-my-achievements', user?.id],
    queryFn: () => api.get('/api/v1/curricular/my-achievements').then(r => Array.isArray(r.data) ? r.data : []),
    enabled: tab === 'achievements',
    retry: false,
  });

  const handleBoardChange = (boardId: string) => {
    setSelectedBoardId(boardId);
    setSelectedSubjectId('');
    setSelectedGradeId('');
  };

  return (
    <div className="p-6 max-w-5xl mx-auto space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-white flex items-center gap-2">
          <Layers className="w-6 h-6 text-brand-400" /> Curricular Hub
        </h1>
        <p className="text-sm text-white/40 mt-1">Your adaptive learning path, extracurricular activities, and achievements</p>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 bg-surface-100/40 border border-white/5 rounded-xl p-1 w-fit">
        {([
          { id: 'path',         label: 'Learning Path',  Icon: BookOpen },
          { id: 'activities',   label: 'My Activities',  Icon: Activity },
          { id: 'achievements', label: 'Achievements',   Icon: Trophy },
        ] as const).map(({ id, label, Icon }) => (
          <button key={id} onClick={() => setTab(id)}
            className={cn('flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium transition-colors',
              tab === id ? 'bg-brand-600 text-white' : 'text-white/50 hover:text-white/70 hover:bg-white/5'
            )}>
            <Icon className="w-3.5 h-3.5" /> {label}
          </button>
        ))}
      </div>

      {/* ── Learning Path Tab ── */}
      {tab === 'path' && (
        <div className="space-y-4">
          {/* Framework selector */}
          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="text-xs text-white/50 mb-1 block">Board</label>
              <select
                value={selectedBoardId}
                onChange={e => handleBoardChange(e.target.value)}
                className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500"
              >
                <option value="">Select board…</option>
                {boards.map(b => <option key={b.id} value={b.id}>{b.boardName}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs text-white/50 mb-1 block">Subject</label>
              <select
                value={selectedSubjectId}
                onChange={e => setSelectedSubjectId(e.target.value)}
                disabled={!selectedBoardId}
                className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500 disabled:opacity-40"
              >
                <option value="">Select subject…</option>
                {subjects.map(s => <option key={s.id} value={s.id}>{s.subjectName}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs text-white/50 mb-1 block">Grade</label>
              <select
                value={selectedGradeId}
                onChange={e => setSelectedGradeId(e.target.value)}
                disabled={!selectedBoardId}
                className="w-full bg-surface-100/40 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500 disabled:opacity-40"
              >
                <option value="">Select grade…</option>
                {[...grades].sort((a, b) => a.sortOrder - b.sortOrder).map(g => (
                  <option key={g.id} value={g.id}>{g.displayName}</option>
                ))}
              </select>
            </div>
          </div>

          {!pathEnabled && (
            <div className="flex flex-col items-center justify-center py-16 text-white/30">
              <GraduationCap className="w-12 h-12 mb-3" />
              <p className="text-sm">Select board, subject, and grade to generate your adaptive learning path</p>
            </div>
          )}

          {pathEnabled && pathLoading && (
            <div className="flex items-center justify-center py-12 text-white/40 text-sm">Computing path…</div>
          )}

          {pathEnabled && path && (
            <div className="space-y-3">
              {/* Path header */}
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-xs text-white/40">
                    {path.items.length} topics • computed {new Date(path.computedAt).toLocaleDateString()}
                  </p>
                  <div className="flex gap-3 mt-1 text-xs text-white/50">
                    <span className="text-green-400">{path.items.filter(i => i.status === 'COMPLETED').length} completed</span>
                    <span className="text-blue-400">{path.items.filter(i => i.status === 'IN_PROGRESS').length} in progress</span>
                    <span className="text-white/30">{path.items.filter(i => i.status === 'PENDING').length} pending</span>
                  </div>
                </div>
                <button
                  onClick={() => refreshMutation.mutate()}
                  disabled={refreshMutation.isPending || pathFetching}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-brand-500/30 text-brand-400 hover:bg-brand-500/10 text-xs transition-colors disabled:opacity-40"
                >
                  <RefreshCw className={cn('w-3.5 h-3.5', (refreshMutation.isPending || pathFetching) && 'animate-spin')} />
                  Refresh Path
                </button>
              </div>

              {/* Progress bar */}
              {path.items.length > 0 && (
                <div className="w-full bg-white/5 rounded-full h-2">
                  <div
                    className="bg-brand-500 rounded-full h-2 transition-all"
                    style={{ width: `${(path.items.filter(i => i.status === 'COMPLETED').length / path.items.length) * 100}%` }}
                  />
                </div>
              )}

              {/* Topic list */}
              <div className="space-y-2">
                {path.items.map((item, idx) => (
                  <div key={item.itemId}
                    className={cn('flex items-center gap-3 p-3 rounded-xl border transition-all',
                      item.status === 'COMPLETED'
                        ? 'bg-green-500/5 border-green-500/20 opacity-70'
                        : urgencyBg(item.urgencyScore)
                    )}>
                    <span className="text-white/30 text-xs w-5 text-right shrink-0">{idx + 1}</span>
                    <div className="flex-1 min-w-0">
                      <p className={cn('text-sm font-medium', item.status === 'COMPLETED' ? 'text-white/50 line-through' : 'text-white')}>
                        {item.topicTitle}
                      </p>
                      <div className="flex items-center gap-2 mt-1">
                        <span className={cn('text-[10px] px-2 py-0.5 rounded-full font-medium', BLOOMS_COLORS[item.bloomsLevel] ?? 'bg-slate-500/20 text-slate-300')}>
                          {item.bloomsLevel}
                        </span>
                        <span className={cn('text-[10px] px-2 py-0.5 rounded-full font-medium', STATUS_COLORS[item.status] ?? 'bg-slate-500/20 text-slate-300')}>
                          {item.status.replace('_', ' ')}
                        </span>
                      </div>
                    </div>
                    <div className="flex items-center gap-3 shrink-0">
                      <div className="text-center">
                        <p className={cn('text-xs font-bold', urgencyColor(item.urgencyScore))}>
                          {Math.round(item.urgencyScore)}
                        </p>
                        <p className="text-[9px] text-white/30">urgency</p>
                      </div>
                      {item.status !== 'COMPLETED' ? (
                        <button
                          onClick={() => markMutation.mutate({ itemId: item.itemId, status: 'COMPLETED' })}
                          disabled={markMutation.isPending}
                          className="p-1.5 rounded-lg bg-green-500/10 text-green-400 hover:bg-green-500/20 transition-colors"
                          title="Mark complete"
                        >
                          <CheckCircle2 className="w-4 h-4" />
                        </button>
                      ) : (
                        <CheckCircle2 className="w-4 h-4 text-green-500" />
                      )}
                    </div>
                  </div>
                ))}
              </div>

              {path.items.length === 0 && (
                <div className="text-center py-10 text-white/30 text-sm">
                  No topics found for this subject and grade.
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* ── Activities Tab ── */}
      {tab === 'activities' && (
        <div className="space-y-3">
          {activitiesLoading && <div className="text-white/40 text-sm py-8 text-center">Loading…</div>}
          {!activitiesLoading && activities.length === 0 && (
            <div className="flex flex-col items-center py-16 text-white/30">
              <Activity className="w-12 h-12 mb-3" />
              <p className="text-sm">No extracurricular activities enrolled yet.</p>
              <p className="text-xs mt-1">Ask your center admin to list available activities.</p>
            </div>
          )}
          {activities.map(a => (
            <div key={a.enrollmentId} className="flex items-center gap-4 p-4 bg-surface-100/40 border border-white/5 rounded-xl">
              <div className="w-10 h-10 rounded-xl bg-brand-500/10 flex items-center justify-center shrink-0">
                <Activity className="w-5 h-5 text-brand-400" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-white">{a.activityName}</p>
                <div className="flex items-center gap-2 mt-1">
                  <span className={cn('text-[10px] px-2 py-0.5 rounded-full', CATEGORY_COLORS[a.category] ?? 'bg-slate-500/20 text-slate-300')}>
                    {a.category.replace('_', ' ')}
                  </span>
                  <span className={cn('text-[10px] px-2 py-0.5 rounded-full', STATUS_COLORS[a.status] ?? 'bg-slate-500/20 text-slate-300')}>
                    {a.status}
                  </span>
                </div>
              </div>
              <p className="text-xs text-white/30 shrink-0">
                {new Date(a.enrolledAt).toLocaleDateString()}
              </p>
            </div>
          ))}
        </div>
      )}

      {/* ── Achievements Tab ── */}
      {tab === 'achievements' && (
        <div className="space-y-3">
          {achievementsLoading && <div className="text-white/40 text-sm py-8 text-center">Loading…</div>}
          {!achievementsLoading && achievements.length === 0 && (
            <div className="flex flex-col items-center py-16 text-white/30">
              <Trophy className="w-12 h-12 mb-3" />
              <p className="text-sm">No achievements yet — keep going!</p>
            </div>
          )}
          <div className="grid gap-3 sm:grid-cols-2">
            {achievements.map(a => (
              <div key={a.id} className="p-4 bg-gradient-to-br from-yellow-500/10 to-orange-500/5 border border-yellow-500/20 rounded-xl">
                <div className="flex items-start gap-3">
                  <div className="w-9 h-9 rounded-full bg-yellow-500/20 flex items-center justify-center shrink-0">
                    <Star className="w-4 h-4 text-yellow-400" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-white">{a.title}</p>
                    <p className="text-xs text-white/50 mt-0.5">{a.activityName}</p>
                    <div className="flex items-center gap-2 mt-2">
                      <span className="text-[10px] px-2 py-0.5 rounded-full bg-yellow-500/20 text-yellow-300">
                        {a.achievementType.replace('_', ' ')}
                      </span>
                      <span className="text-[10px] text-white/30">
                        {new Date(a.issuedAt).toLocaleDateString()}
                      </span>
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
