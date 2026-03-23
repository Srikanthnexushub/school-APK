// src/pages/mentor-portal/MentorPortalLibraryPage.tsx
// Read-only library view for teachers — shows all AVAILABLE documents
// from the teacher's center (resolved via GET /api/v1/centers).
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Library } from 'lucide-react';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { LibraryView } from '../../components/library/LibraryView';

interface CenterResponse { id: string; name: string; }

export default function MentorPortalLibraryPage() {
  const { user } = useAuthStore();
  const [selectedCenterId, setSelectedCenterId] = useState('');

  const { data: centers = [], isLoading: centersLoading } = useQuery<CenterResponse[]>({
    queryKey: ['teacher-centers'],
    queryFn: async () => {
      const res = await api.get('/api/v1/centers');
      const d = res.data;
      return Array.isArray(d) ? d : (d.content ?? []);
    },
    enabled: !!user,
  });

  const centerId = selectedCenterId || centers[0]?.id || '';

  return (
    <div className="p-6 max-w-6xl mx-auto space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <Library className="w-6 h-6 text-brand-400" /> Library
          </h1>
          <p className="text-sm text-white/40 mt-1">Browse available study materials and resources</p>
        </div>

        {/* Center selector (if teaching in multiple centers) */}
        {centers.length > 1 && (
          <select
            value={centerId}
            onChange={e => setSelectedCenterId(e.target.value)}
            className="bg-surface-100/50 border border-white/10 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-brand-500/50 transition-colors"
          >
            {centers.map(c => (
              <option key={c.id} value={c.id} className="bg-surface-100">{c.name}</option>
            ))}
          </select>
        )}
      </div>

      <LibraryView
        centerId={centerId}
        isResolvingCenter={centersLoading}
        emptyHint="Your center hasn't uploaded any materials yet. Check back soon!"
      />
    </div>
  );
}
