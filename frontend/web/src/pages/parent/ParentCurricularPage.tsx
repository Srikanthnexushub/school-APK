import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Layers, Activity, Trophy, Star, Users, ChevronDown } from 'lucide-react';
import api from '../../lib/api';
import { cn } from '../../lib/utils';

// ─── Types ──────────────────────────────────────────────────────────────────

interface ParentProfile      { id: string; fullName: string; }
interface StudentLink        { studentId: string; studentName: string; }

interface ActivityEnrollmentResponse {
  enrollmentId: string; activityId: string; activityName: string;
  category: string; status: string; enrolledAt: string;
}
interface AchievementResponse {
  id: string; activityId: string; activityName: string;
  achievementType: string; title: string; issuedAt: string;
}
interface LearningPathItem {
  itemId: string; topicTitle: string; bloomsLevel: string;
  sequenceOrder: number; urgencyScore: number; status: string;
}
interface LearningPathResponse {
  pathId: string; computedAt: string; items: LearningPathItem[];
}
interface ChildJourneyResponse {
  studentId: string;
  learningPaths: LearningPathResponse[];
  activities: ActivityEnrollmentResponse[];
  achievements: AchievementResponse[];
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
  ENROLLED:   'bg-green-500/20 text-green-300',
  WAITLISTED: 'bg-yellow-500/20 text-yellow-300',
  WITHDRAWN:  'bg-red-500/20 text-red-300',
};

type Tab = 'activities' | 'achievements' | 'learning';

// ─── Component ──────────────────────────────────────────────────────────────

export default function ParentCurricularPage() {
  const [selectedStudentId, setSelectedStudentId] = useState<string | null>(null);
  const [tab, setTab] = useState<Tab>('activities');

  // ── Fetch parent profile + linked students ────────────────────────────────
  const { data: profile } = useQuery<ParentProfile>({
    queryKey: ['parent-profile-curricular'],
    queryFn: () => api.get('/api/v1/parents/me').then(r => r.data),
  });

  const { data: linkedStudents = [] } = useQuery<StudentLink[]>({
    queryKey: ['parent-linked-students-curricular', profile?.id],
    queryFn: () => api.get(`/api/v1/parents/${profile!.id}/students`).then(r => {
      const d = r.data; return Array.isArray(d) ? d : (d.content ?? []);
    }),
    enabled: !!profile?.id,
  });

  const activeStudentId = selectedStudentId ?? linkedStudents[0]?.studentId ?? null;
  const selectedStudent = linkedStudents.find(s => s.studentId === activeStudentId);

  // ── Child journey ─────────────────────────────────────────────────────────
  const { data: journey, isLoading: journeyLoading } = useQuery<ChildJourneyResponse>({
    queryKey: ['child-journey', activeStudentId],
    queryFn: () => api.get(`/api/v1/curricular/child/${activeStudentId}/journey`).then(r => r.data),
    enabled: !!activeStudentId,
    retry: false,
  });

  const activities    = journey?.activities    ?? [];
  const achievements  = journey?.achievements  ?? [];
  const learningPaths = journey?.learningPaths ?? [];

  return (
    <div className="p-6 max-w-5xl mx-auto space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-white flex items-center gap-2">
          <Layers className="w-6 h-6 text-brand-400" /> Child Curricular Journey
        </h1>
        <p className="text-sm text-white/40 mt-1">Track your child's academic and extracurricular progress</p>
      </div>

      {/* Child selector */}
      {linkedStudents.length > 1 && (
        <div className="relative w-fit">
          <select
            value={activeStudentId ?? ''}
            onChange={e => setSelectedStudentId(e.target.value)}
            className="appearance-none bg-surface-100/40 border border-white/10 rounded-xl pl-4 pr-8 py-2 text-white text-sm focus:outline-none focus:border-brand-500"
          >
            {linkedStudents.map(s => (
              <option key={s.studentId} value={s.studentId}>{s.studentName}</option>
            ))}
          </select>
          <ChevronDown className="absolute right-2.5 top-1/2 -translate-y-1/2 w-4 h-4 text-white/40 pointer-events-none" />
        </div>
      )}

      {linkedStudents.length === 0 && (
        <div className="flex flex-col items-center py-16 text-white/30">
          <Users className="w-12 h-12 mb-3" />
          <p className="text-sm">No linked children found.</p>
        </div>
      )}

      {activeStudentId && (
        <>
          {/* Student name chip */}
          {selectedStudent && linkedStudents.length === 1 && (
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-full bg-brand-500/20 flex items-center justify-center">
                <span className="text-brand-400 text-xs font-bold">
                  {selectedStudent.studentName.charAt(0).toUpperCase()}
                </span>
              </div>
              <span className="text-white font-medium text-sm">{selectedStudent.studentName}</span>
            </div>
          )}

          {/* Tabs */}
          <div className="flex gap-1 bg-surface-100/40 border border-white/5 rounded-xl p-1 w-fit">
            {([
              { id: 'activities',   label: 'Activities',   Icon: Activity },
              { id: 'achievements', label: 'Achievements', Icon: Trophy },
              { id: 'learning',     label: 'Learning Path', Icon: Layers },
            ] as const).map(({ id, label, Icon }) => (
              <button key={id} onClick={() => setTab(id)}
                className={cn('flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium transition-colors',
                  tab === id ? 'bg-brand-600 text-white' : 'text-white/50 hover:text-white/70 hover:bg-white/5'
                )}>
                <Icon className="w-3.5 h-3.5" /> {label}
              </button>
            ))}
          </div>

          {journeyLoading && <div className="text-white/40 text-sm py-8 text-center">Loading journey…</div>}

          {/* ── Activities ── */}
          {!journeyLoading && tab === 'activities' && (
            <div className="space-y-3">
              {activities.length === 0 && (
                <div className="flex flex-col items-center py-12 text-white/30">
                  <Activity className="w-10 h-10 mb-3" />
                  <p className="text-sm">No extracurricular activities enrolled yet.</p>
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
                  <p className="text-xs text-white/30">{new Date(a.enrolledAt).toLocaleDateString()}</p>
                </div>
              ))}
            </div>
          )}

          {/* ── Achievements ── */}
          {!journeyLoading && tab === 'achievements' && (
            <div className="space-y-3">
              {achievements.length === 0 && (
                <div className="flex flex-col items-center py-12 text-white/30">
                  <Trophy className="w-10 h-10 mb-3" />
                  <p className="text-sm">No achievements yet.</p>
                </div>
              )}
              <div className="grid gap-3 sm:grid-cols-2">
                {achievements.map(a => (
                  <div key={a.id} className="p-4 bg-gradient-to-br from-yellow-500/10 to-orange-500/5 border border-yellow-500/20 rounded-xl">
                    <div className="flex items-start gap-3">
                      <div className="w-9 h-9 rounded-full bg-yellow-500/20 flex items-center justify-center shrink-0">
                        <Star className="w-4 h-4 text-yellow-400" />
                      </div>
                      <div className="flex-1">
                        <p className="text-sm font-semibold text-white">{a.title}</p>
                        <p className="text-xs text-white/50 mt-0.5">{a.activityName}</p>
                        <div className="flex items-center gap-2 mt-2">
                          <span className="text-[10px] px-2 py-0.5 rounded-full bg-yellow-500/20 text-yellow-300">
                            {a.achievementType.replace('_', ' ')}
                          </span>
                          <span className="text-[10px] text-white/30">{new Date(a.issuedAt).toLocaleDateString()}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* ── Learning Path ── */}
          {!journeyLoading && tab === 'learning' && (
            <div className="space-y-4">
              {learningPaths.length === 0 && (
                <div className="flex flex-col items-center py-12 text-white/30">
                  <Layers className="w-10 h-10 mb-3" />
                  <p className="text-sm">No learning paths generated yet.</p>
                </div>
              )}
              {learningPaths.map(lp => {
                const completed = lp.items.filter(i => i.status === 'COMPLETED').length;
                const pct = lp.items.length > 0 ? Math.round((completed / lp.items.length) * 100) : 0;
                return (
                  <div key={lp.pathId} className="p-4 bg-surface-100/40 border border-white/5 rounded-xl space-y-3">
                    <div className="flex items-center justify-between">
                      <p className="text-sm font-medium text-white">{lp.items.length} topics</p>
                      <span className="text-xs text-white/40">{pct}% complete</span>
                    </div>
                    <div className="w-full bg-white/5 rounded-full h-1.5">
                      <div className="bg-brand-500 rounded-full h-1.5 transition-all" style={{ width: `${pct}%` }} />
                    </div>
                    <div className="space-y-1.5 max-h-48 overflow-y-auto pr-1">
                      {lp.items.slice(0, 10).map(item => (
                        <div key={item.itemId} className="flex items-center gap-2">
                          <div className={cn('w-1.5 h-1.5 rounded-full shrink-0',
                            item.status === 'COMPLETED' ? 'bg-green-400' :
                            item.status === 'IN_PROGRESS' ? 'bg-blue-400' : 'bg-white/20'
                          )} />
                          <p className={cn('text-xs flex-1 truncate', item.status === 'COMPLETED' ? 'text-white/30 line-through' : 'text-white/70')}>
                            {item.topicTitle}
                          </p>
                          <span className={cn('text-[9px] px-1.5 py-0.5 rounded-full shrink-0', BLOOMS_COLORS[item.bloomsLevel] ?? '')}>
                            {item.bloomsLevel}
                          </span>
                        </div>
                      ))}
                      {lp.items.length > 10 && (
                        <p className="text-xs text-white/30 text-center">+{lp.items.length - 10} more topics</p>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </>
      )}
    </div>
  );
}
