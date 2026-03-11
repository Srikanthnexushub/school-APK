import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Library } from 'lucide-react';
import { cn } from '../../lib/utils';
import { Avatar } from '../../components/ui/Avatar';
import { LibraryView } from '../../components/library/LibraryView';
import api from '../../lib/api';

interface ParentProfileResponse { id: string; name: string; }
interface StudentLinkResponse { id: string; studentId: string; studentName: string; status: string; }
interface GradeResponse { centerId: string; batchId: string; }

function resolveCenterId(grades: GradeResponse[]): string {
  if (grades.length === 0) return '';
  const freq: Record<string, number> = {};
  grades.forEach(g => { if (g.centerId) freq[g.centerId] = (freq[g.centerId] ?? 0) + 1; });
  return Object.entries(freq).sort((a, b) => b[1] - a[1])[0]?.[0] ?? '';
}

export default function ParentLibraryPage() {
  const [activeStudentId, setActiveStudentId] = useState<string | null>(null);

  const { data: profile } = useQuery<ParentProfileResponse>({
    queryKey: ['parent-profile'],
    queryFn: () => api.get('/api/v1/parents/me').then(r => r.data),
  });

  const { data: linkedStudents = [], isLoading: studentsLoading } = useQuery<StudentLinkResponse[]>({
    queryKey: ['linked-students', profile?.id],
    queryFn: () =>
      api.get(`/api/v1/parents/${profile!.id}/students?size=50`).then(r => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
    enabled: !!profile?.id,
  });

  const resolvedStudentId = activeStudentId ?? linkedStudents[0]?.studentId ?? null;

  const { data: grades = [], isLoading: gradesLoading } = useQuery<GradeResponse[]>({
    queryKey: ['student-grades-lib', resolvedStudentId],
    queryFn: async () => {
      const res = await api.get(`/api/v1/grades/student/${resolvedStudentId}`);
      return Array.isArray(res.data) ? res.data : [];
    },
    enabled: !!resolvedStudentId,
    retry: false,
  });

  const centerId = resolveCenterId(grades);
  const isResolvingCenter = studentsLoading || gradesLoading;

  return (
    <div className="p-6 max-w-6xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white flex items-center gap-2">
          <Library className="w-6 h-6 text-brand-400" /> Library
        </h1>
        <p className="text-sm text-white/40 mt-1">Study materials from your child's center</p>
      </div>

      {/* Child selector */}
      {linkedStudents.length > 1 && (
        <div className="flex gap-3 overflow-x-auto pb-1">
          {linkedStudents.map(s => (
            <button
              key={s.studentId}
              onClick={() => setActiveStudentId(s.studentId)}
              className={cn(
                'flex items-center gap-3 px-4 py-2.5 rounded-2xl border transition-all flex-shrink-0',
                resolvedStudentId === s.studentId
                  ? 'border-brand-500 bg-brand-500/10'
                  : 'border-white/5 bg-surface-50/40 hover:border-white/10'
              )}
            >
              <Avatar name={s.studentName} size="sm" />
              <span className="text-sm font-semibold text-white">{s.studentName}</span>
            </button>
          ))}
        </div>
      )}

      {!studentsLoading && linkedStudents.length === 0 ? (
        <div className="card text-center py-12">
          <Library className="w-10 h-10 text-white/20 mx-auto mb-3" />
          <p className="text-white/50 text-sm">No children linked yet.</p>
          <p className="text-white/30 text-xs mt-1">Link a child to view their study library.</p>
        </div>
      ) : (
        <LibraryView
          centerId={centerId}
          isResolvingCenter={isResolvingCenter}
          emptyHint="Your child's center hasn't uploaded materials yet. Check back soon!"
        />
      )}
    </div>
  );
}
