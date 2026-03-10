// src/pages/library/StudentLibraryPage.tsx
import { useState } from 'react';
import { motion } from 'framer-motion';
import { useQuery } from '@tanstack/react-query';
import {
  FileText, Video, File, Image, Music,
  Search, ExternalLink, Loader2, Filter, Library, BookOpen,
} from 'lucide-react';
import api from '../../lib/api';
import { cn } from '../../lib/utils';
import { useAuthStore } from '../../stores/authStore';

// ─── Types ────────────────────────────────────────────────────────────────────

type ContentType = 'PDF' | 'VIDEO' | 'DOCUMENT' | 'IMAGE' | 'AUDIO';

interface GradeResponse { centerId: string; batchId: string; }
interface ContentItem {
  id: string;
  centerId: string;
  batchId?: string;
  title: string;
  description?: string;
  type: ContentType;
  fileUrl: string;
  fileSizeBytes?: number;
  status: string;
  createdAt: string;
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

const TYPE_ICON: Record<ContentType, React.ElementType> = {
  PDF: FileText, VIDEO: Video, DOCUMENT: File, IMAGE: Image, AUDIO: Music,
};

const TYPE_COLOR: Record<ContentType, string> = {
  PDF:      'bg-red-500/15 text-red-400 border-red-500/20',
  VIDEO:    'bg-purple-500/15 text-purple-400 border-purple-500/20',
  DOCUMENT: 'bg-sky-500/15 text-sky-400 border-sky-500/20',
  IMAGE:    'bg-emerald-500/15 text-emerald-400 border-emerald-500/20',
  AUDIO:    'bg-amber-500/15 text-amber-400 border-amber-500/20',
};

const TYPE_LABELS: Record<ContentType, string> = {
  PDF: 'PDF', VIDEO: 'Video', DOCUMENT: 'Document', IMAGE: 'Image', AUDIO: 'Audio',
};

const TYPE_OPTIONS: ContentType[] = ['PDF', 'VIDEO', 'DOCUMENT', 'IMAGE', 'AUDIO'];

function formatBytes(bytes?: number): string {
  if (!bytes) return '';
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

// ─── Content Card ─────────────────────────────────────────────────────────────

function ContentCard({ item }: { item: ContentItem }) {
  const Icon = TYPE_ICON[item.type] ?? File;
  const colorClass = TYPE_COLOR[item.type] ?? 'bg-white/5 text-white/50 border-white/10';

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      whileHover={{ y: -2 }}
      className="bg-surface-100/40 border border-white/5 rounded-xl p-4 flex flex-col gap-3 hover:border-white/10 transition-all"
    >
      <div className="flex items-start justify-between gap-2">
        <div className={cn('p-2.5 rounded-lg border flex-shrink-0', colorClass)}>
          <Icon className="w-4 h-4" />
        </div>
        <span className={cn('text-[10px] font-semibold uppercase tracking-wider px-2 py-0.5 rounded-full border flex-shrink-0', colorClass)}>
          {TYPE_LABELS[item.type] ?? item.type}
        </span>
      </div>

      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold text-white leading-tight line-clamp-2">{item.title}</p>
        {item.description && (
          <p className="text-xs text-white/40 mt-1 line-clamp-2">{item.description}</p>
        )}
      </div>

      <div className="flex items-center justify-between text-xs text-white/30">
        {item.fileSizeBytes ? <span>{formatBytes(item.fileSizeBytes)}</span> : <span />}
        <span>{new Date(item.createdAt).toLocaleDateString()}</span>
      </div>

      <a
        href={item.fileUrl}
        target="_blank"
        rel="noopener noreferrer"
        className="flex items-center justify-center gap-1.5 text-xs text-brand-400 hover:text-brand-300 transition-colors font-medium bg-brand-500/10 hover:bg-brand-500/15 border border-brand-500/20 rounded-lg py-2"
      >
        <ExternalLink className="w-3.5 h-3.5" />
        Open resource
      </a>
    </motion.div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function StudentLibraryPage() {
  const { user } = useAuthStore();
  const studentId = user?.id ?? '';
  const [filterType, setFilterType] = useState<string>('ALL');
  const [search, setSearch] = useState('');

  // Get student's grades to determine centerId (uses /api/v1/grades/** → api-gateway → assess-svc)
  const { data: grades = [], isLoading: gradesLoading } = useQuery<GradeResponse[]>({
    queryKey: ['student-grades-lib', studentId],
    queryFn: async () => {
      const res = await api.get(`/api/v1/grades/student/${studentId}`);
      return Array.isArray(res.data) ? res.data : [];
    },
    enabled: !!studentId,
    retry: false,
  });

  // Pick the most common centerId from grades
  const centerId = (() => {
    if (grades.length === 0) return '';
    const freq: Record<string, number> = {};
    grades.forEach(g => { if (g.centerId) freq[g.centerId] = (freq[g.centerId] ?? 0) + 1; });
    return Object.entries(freq).sort((a, b) => b[1] - a[1])[0]?.[0] ?? '';
  })();

  const { data: content = [], isLoading: contentLoading } = useQuery<ContentItem[]>({
    queryKey: ['student-library', centerId],
    queryFn: async () => {
      const res = await api.get(`/api/v1/centers/${centerId}/content`);
      const d = res.data;
      return Array.isArray(d) ? d : (d.content ?? []);
    },
    enabled: !!centerId,
  });

  const isLoading = gradesLoading || contentLoading;

  const filtered = content.filter(item => {
    const matchType = filterType === 'ALL' || item.type === filterType;
    const matchSearch = !search
      || item.title.toLowerCase().includes(search.toLowerCase())
      || (item.description ?? '').toLowerCase().includes(search.toLowerCase());
    return matchType && matchSearch;
  });

  // Group by type for visual interest
  const typeCounts = TYPE_OPTIONS.reduce((acc, t) => {
    acc[t] = content.filter(i => i.type === t).length;
    return acc;
  }, {} as Record<string, number>);

  return (
    <div className="p-6 max-w-6xl mx-auto space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-white flex items-center gap-2">
          <Library className="w-6 h-6 text-brand-400" /> Library
        </h1>
        <p className="text-sm text-white/40 mt-1">Study materials and resources from your center</p>
      </div>

      {/* Type summary pills */}
      {!isLoading && content.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {TYPE_OPTIONS.filter(t => typeCounts[t] > 0).map(t => {
            const Icon = TYPE_ICON[t];
            return (
              <button
                key={t}
                onClick={() => setFilterType(filterType === t ? 'ALL' : t)}
                className={cn(
                  'flex items-center gap-1.5 px-3 py-1.5 rounded-xl border text-xs font-medium transition-colors',
                  filterType === t
                    ? TYPE_COLOR[t]
                    : 'bg-surface-100/30 border-white/5 text-white/40 hover:text-white hover:border-white/10'
                )}
              >
                <Icon className="w-3.5 h-3.5" />
                {t} <span className="opacity-60">({typeCounts[t]})</span>
              </button>
            );
          })}
        </div>
      )}

      {/* Search & filter bar */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
          <input
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Search resources…"
            className="w-full bg-surface-100/50 border border-white/10 rounded-xl pl-9 pr-3 py-2 text-sm text-white placeholder-white/20 focus:outline-none focus:border-brand-500/50"
          />
        </div>
        <div className="flex items-center gap-1.5">
          <Filter className="w-3.5 h-3.5 text-white/30 flex-shrink-0" />
          {['ALL', ...TYPE_OPTIONS].map(t => (
            <button
              key={t}
              onClick={() => setFilterType(t)}
              className={cn(
                'px-2.5 py-1 rounded-lg text-xs font-medium transition-colors',
                filterType === t
                  ? 'bg-brand-600 text-white'
                  : 'bg-surface-100/40 text-white/40 hover:text-white border border-white/5'
              )}
            >
              {t === 'ALL' ? 'All' : t}
            </button>
          ))}
        </div>
      </div>

      {/* Main content */}
      {isLoading ? (
        <div className="flex items-center gap-3 justify-center py-20 text-white/40">
          <Loader2 className="w-5 h-5 animate-spin" />
          <span className="text-sm">Loading library…</span>
        </div>
      ) : !centerId ? (
        <div className="text-center py-20">
          <BookOpen className="w-12 h-12 mx-auto mb-4 text-white/20" />
          <p className="text-white/40 font-medium">No center linked yet</p>
          <p className="text-white/25 text-sm mt-1">Complete an assessment first to unlock your study library.</p>
        </div>
      ) : filtered.length === 0 ? (
        <div className="text-center py-20">
          <Library className="w-12 h-12 mx-auto mb-4 text-white/20" />
          <p className="text-white/40 font-medium">
            {content.length === 0 ? 'No resources available yet' : 'No resources match your search'}
          </p>
          <p className="text-white/25 text-sm mt-1">
            {content.length === 0
              ? "Your center hasn't uploaded any materials yet. Check back soon!"
              : 'Try a different search term or filter.'}
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {filtered.map(item => <ContentCard key={item.id} item={item} />)}
        </div>
      )}

      {content.length > 0 && (
        <p className="text-xs text-white/30 text-center">
          {filtered.length} of {content.length} resources shown
        </p>
      )}
    </div>
  );
}
