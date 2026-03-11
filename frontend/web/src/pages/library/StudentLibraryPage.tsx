import { useQuery } from '@tanstack/react-query';
import { Library } from 'lucide-react';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { LibraryView } from '../../components/library/LibraryView';

interface GradeResponse { centerId: string; batchId: string; }

function resolveCenterId(grades: GradeResponse[]): string {
  if (grades.length === 0) return '';
  const freq: Record<string, number> = {};
  grades.forEach(g => { if (g.centerId) freq[g.centerId] = (freq[g.centerId] ?? 0) + 1; });
  return Object.entries(freq).sort((a, b) => b[1] - a[1])[0]?.[0] ?? '';
}

export default function StudentLibraryPage() {
  const { user } = useAuthStore();
  const studentId = user?.id ?? '';

  const { data: grades = [], isLoading: gradesLoading } = useQuery<GradeResponse[]>({
    queryKey: ['student-grades-lib', studentId],
    queryFn: async () => {
      const res = await api.get(`/api/v1/grades/student/${studentId}`);
      return Array.isArray(res.data) ? res.data : [];
    },
    enabled: !!studentId,
    retry: false,
  });

  const centerId = resolveCenterId(grades);

  return (
    <div className="p-6 max-w-6xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white flex items-center gap-2">
          <Library className="w-6 h-6 text-brand-400" /> Library
        </h1>
        <p className="text-sm text-white/40 mt-1">Study materials and resources from your center</p>
      </div>

      <LibraryView centerId={centerId} isResolvingCenter={gradesLoading} />
    </div>
  );
}
