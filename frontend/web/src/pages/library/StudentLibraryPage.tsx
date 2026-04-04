import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Library, Globe } from 'lucide-react';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { LibraryView } from '../../components/library/LibraryView';
import { cn } from '../../lib/utils';

interface Enrollment { centerId: string; batchId: string; }
interface StudentProfile { board?: string; stream?: string; currentClass?: string; }

type Tab = 'center' | 'platform';

export default function StudentLibraryPage() {
  const { user } = useAuthStore();
  const studentId = user?.id ?? '';
  const [tab, setTab] = useState<Tab>('center');

  // Use enrollment endpoint (same as student fees page) — returns centerId reliably
  const { data: enrollments = [], isLoading: enrollmentsLoading } = useQuery<Enrollment[]>({
    queryKey: ['student-enrollment-lib'],
    queryFn: () =>
      api.get('/api/v1/centers/student-enrollment/me/all')
        .then(r => Array.isArray(r.data) ? r.data : [])
        .catch(() => []),
    enabled: !!studentId,
    staleTime: 5 * 60_000,
    retry: false,
  });

  const { data: profile } = useQuery<StudentProfile>({
    queryKey: ['student-profile-lib', studentId],
    queryFn: async () => {
      const res = await api.get('/api/v1/students/me');
      return res.data ?? {};
    },
    enabled: !!studentId,
    retry: false,
    staleTime: 5 * 60_000,
  });

  const centerId = enrollments[0]?.centerId || user?.centerId || '';

  return (
    <div className="p-6 max-w-6xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white flex items-center gap-2">
          <Library className="w-6 h-6 text-brand-400" /> Library
        </h1>
        <p className="text-sm text-white/40 mt-1">Study materials, resources, and platform-wide content</p>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 bg-surface-100/40 border border-white/5 rounded-xl p-1 w-fit">
        {([
          { id: 'center',   label: 'My Center',  Icon: Library },
          { id: 'platform', label: 'Platform',   Icon: Globe   },
        ] as const).map(({ id, label, Icon }) => (
          <button
            key={id}
            onClick={() => setTab(id)}
            className={cn(
              'flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium transition-colors',
              tab === id
                ? 'bg-brand-600 text-white'
                : 'text-white/50 hover:text-white/70 hover:bg-white/5',
            )}
          >
            <Icon className="w-3.5 h-3.5" />
            {label}
          </button>
        ))}
      </div>

      {tab === 'center' ? (
        <LibraryView
          centerId={centerId}
          isResolvingCenter={enrollmentsLoading}
          stream={profile?.stream}
        />
      ) : (
        <LibraryView
          showPlatformResources
          stream={profile?.stream}
          emptyHint="No platform-wide resources match your profile yet."
          globalMode
        />
      )}
    </div>
  );
}
