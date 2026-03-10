// src/pages/mentor-portal/MentorPortalLibraryPage.tsx
import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  FileText, Video, File, Image, Music, Upload, Search,
  Plus, ExternalLink, Loader2, X, Filter, Library,
} from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { cn } from '../../lib/utils';
import { useAuthStore } from '../../stores/authStore';

// ─── Types ────────────────────────────────────────────────────────────────────

type ContentType = 'PDF' | 'VIDEO' | 'DOCUMENT' | 'IMAGE' | 'AUDIO';
type ContentStatus = 'ACTIVE' | 'ARCHIVED';

interface CenterResponse { id: string; name: string; }
interface BatchResponse  { id: string; name: string; subject: string; }
interface ContentItem {
  id: string;
  centerId: string;
  batchId?: string;
  title: string;
  description?: string;
  type: ContentType;
  fileUrl: string;
  fileSizeBytes?: number;
  uploadedByUserId: string;
  status: ContentStatus;
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

const TYPE_OPTIONS: ContentType[] = ['PDF', 'VIDEO', 'DOCUMENT', 'IMAGE', 'AUDIO'];

function formatBytes(bytes?: number): string {
  if (!bytes) return '';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

// ─── Content Card ─────────────────────────────────────────────────────────────

function ContentCard({ item }: { item: ContentItem }) {
  const Icon = TYPE_ICON[item.type] ?? File;
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-surface-100/40 border border-white/5 rounded-xl p-4 flex flex-col gap-3 hover:border-white/10 transition-colors"
    >
      <div className="flex items-start justify-between gap-2">
        <div className={cn('p-2.5 rounded-lg border flex-shrink-0', TYPE_COLOR[item.type])}>
          <Icon className="w-4 h-4" />
        </div>
        <span className={cn('text-[10px] font-semibold uppercase tracking-wider px-2 py-0.5 rounded-full border', TYPE_COLOR[item.type])}>
          {item.type}
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
        className="flex items-center gap-1.5 text-xs text-brand-400 hover:text-brand-300 transition-colors font-medium"
      >
        <ExternalLink className="w-3.5 h-3.5" />
        Open resource
      </a>
    </motion.div>
  );
}

// ─── Upload Modal ─────────────────────────────────────────────────────────────

function UploadModal({
  centerId,
  batches,
  onClose,
}: { centerId: string; batches: BatchResponse[]; onClose: () => void }) {
  const queryClient = useQueryClient();
  const [form, setForm] = useState({
    title: '', description: '', type: 'PDF' as ContentType,
    fileUrl: '', fileSizeBytes: '', batchId: '',
  });

  const upload = useMutation({
    mutationFn: () => api.post(`/api/v1/centers/${centerId}/content`, {
      batchId: form.batchId || undefined,
      title: form.title,
      description: form.description || undefined,
      type: form.type,
      fileUrl: form.fileUrl,
      fileSizeBytes: form.fileSizeBytes ? parseInt(form.fileSizeBytes) : undefined,
    }),
    onSuccess: () => {
      toast.success('Resource uploaded successfully');
      queryClient.invalidateQueries({ queryKey: ['library', centerId] });
      onClose();
    },
    onError: () => toast.error('Failed to upload resource'),
  });

  function set(k: string, v: string) { setForm(f => ({ ...f, [k]: v })); }
  const disabled = !form.title.trim() || !form.fileUrl.trim();

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        className="w-full max-w-md bg-surface-50 border border-white/10 rounded-2xl shadow-2xl overflow-hidden"
      >
        <div className="flex items-center justify-between px-5 py-4 border-b border-white/5">
          <h2 className="text-sm font-semibold text-white flex items-center gap-2">
            <Upload className="w-4 h-4 text-brand-400" /> Add Resource
          </h2>
          <button onClick={onClose} className="p-1 rounded-lg hover:bg-white/5 text-white/40 hover:text-white transition-colors">
            <X className="w-4 h-4" />
          </button>
        </div>
        <div className="p-5 space-y-4">
          <div>
            <label className="block text-xs text-white/50 mb-1.5 font-medium">Title *</label>
            <input
              value={form.title} onChange={e => set('title', e.target.value)}
              placeholder="e.g. Chapter 5 – Algebra Notes"
              className="w-full bg-surface-100/50 border border-white/10 rounded-xl px-3 py-2.5 text-sm text-white placeholder-white/20 focus:outline-none focus:border-brand-500/50"
            />
          </div>
          <div>
            <label className="block text-xs text-white/50 mb-1.5 font-medium">Description</label>
            <textarea
              value={form.description} onChange={e => set('description', e.target.value)}
              rows={2} placeholder="Brief description of the resource…"
              className="w-full bg-surface-100/50 border border-white/10 rounded-xl px-3 py-2.5 text-sm text-white placeholder-white/20 focus:outline-none focus:border-brand-500/50 resize-none"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs text-white/50 mb-1.5 font-medium">Type *</label>
              <select
                value={form.type} onChange={e => set('type', e.target.value)}
                className="w-full bg-surface-100/50 border border-white/10 rounded-xl px-3 py-2.5 text-sm text-white focus:outline-none focus:border-brand-500/50"
              >
                {TYPE_OPTIONS.map(t => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-xs text-white/50 mb-1.5 font-medium">Batch (optional)</label>
              <select
                value={form.batchId} onChange={e => set('batchId', e.target.value)}
                className="w-full bg-surface-100/50 border border-white/10 rounded-xl px-3 py-2.5 text-sm text-white focus:outline-none focus:border-brand-500/50"
              >
                <option value="">All batches</option>
                {batches.map(b => <option key={b.id} value={b.id}>{b.name}</option>)}
              </select>
            </div>
          </div>
          <div>
            <label className="block text-xs text-white/50 mb-1.5 font-medium">File URL *</label>
            <input
              value={form.fileUrl} onChange={e => set('fileUrl', e.target.value)}
              placeholder="https://cdn.example.com/file.pdf"
              className="w-full bg-surface-100/50 border border-white/10 rounded-xl px-3 py-2.5 text-sm text-white placeholder-white/20 focus:outline-none focus:border-brand-500/50"
            />
          </div>
          <div>
            <label className="block text-xs text-white/50 mb-1.5 font-medium">File size (bytes)</label>
            <input
              value={form.fileSizeBytes} onChange={e => set('fileSizeBytes', e.target.value)}
              placeholder="e.g. 204800"
              type="number" min="0"
              className="w-full bg-surface-100/50 border border-white/10 rounded-xl px-3 py-2.5 text-sm text-white placeholder-white/20 focus:outline-none focus:border-brand-500/50"
            />
          </div>
        </div>
        <div className="px-5 pb-5 flex gap-3 justify-end">
          <button onClick={onClose} className="px-4 py-2 text-sm text-white/50 hover:text-white transition-colors">Cancel</button>
          <button
            onClick={() => upload.mutate()}
            disabled={disabled || upload.isPending}
            className="flex items-center gap-2 px-5 py-2 bg-brand-600 hover:bg-brand-500 disabled:opacity-50 text-white text-sm font-medium rounded-xl transition-colors"
          >
            {upload.isPending ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Upload className="w-3.5 h-3.5" />}
            Upload
          </button>
        </div>
      </motion.div>
    </div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function MentorPortalLibraryPage() {
  const { user } = useAuthStore();
  const [selectedCenter, setSelectedCenter] = useState<string>('');
  const [filterType, setFilterType] = useState<string>('ALL');
  const [search, setSearch] = useState('');
  const [showUpload, setShowUpload] = useState(false);

  const { data: centers = [], isLoading: centersLoading } = useQuery<CenterResponse[]>({
    queryKey: ['teacher-centers'],
    queryFn: async () => {
      const res = await api.get('/api/v1/centers');
      const d = res.data;
      return Array.isArray(d) ? d : (d.content ?? []);
    },
    enabled: !!user,
  });

  const centerId = selectedCenter || centers[0]?.id || '';

  const { data: batches = [] } = useQuery<BatchResponse[]>({
    queryKey: ['center-batches', centerId],
    queryFn: async () => {
      const res = await api.get(`/api/v1/centers/${centerId}/batches`);
      const d = res.data;
      return Array.isArray(d) ? d : (d.content ?? []);
    },
    enabled: !!centerId,
  });

  const { data: content = [], isLoading: contentLoading } = useQuery<ContentItem[]>({
    queryKey: ['library', centerId],
    queryFn: async () => {
      const res = await api.get(`/api/v1/centers/${centerId}/content`);
      const d = res.data;
      return Array.isArray(d) ? d : (d.content ?? []);
    },
    enabled: !!centerId,
  });

  const filtered = content.filter(item => {
    const matchType = filterType === 'ALL' || item.type === filterType;
    const matchSearch = !search || item.title.toLowerCase().includes(search.toLowerCase())
      || (item.description ?? '').toLowerCase().includes(search.toLowerCase());
    return matchType && matchSearch;
  });

  const isLoading = centersLoading || contentLoading;

  return (
    <div className="p-6 max-w-6xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <Library className="w-6 h-6 text-brand-400" /> Library
          </h1>
          <p className="text-sm text-white/40 mt-1">Manage study resources for your batches</p>
        </div>
        <button
          onClick={() => setShowUpload(true)}
          disabled={!centerId}
          className="flex items-center gap-2 px-4 py-2.5 bg-brand-600 hover:bg-brand-500 disabled:opacity-40 text-white text-sm font-medium rounded-xl transition-colors"
        >
          <Plus className="w-4 h-4" /> Add Resource
        </button>
      </div>

      {/* Filters bar */}
      <div className="flex flex-col sm:flex-row gap-3">
        {/* Center selector */}
        {centers.length > 1 && (
          <select
            value={centerId}
            onChange={e => setSelectedCenter(e.target.value)}
            className="bg-surface-100/50 border border-white/10 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-brand-500/50"
          >
            {centers.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        )}
        {/* Search */}
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
          <input
            value={search} onChange={e => setSearch(e.target.value)}
            placeholder="Search resources…"
            className="w-full bg-surface-100/50 border border-white/10 rounded-xl pl-9 pr-3 py-2 text-sm text-white placeholder-white/20 focus:outline-none focus:border-brand-500/50"
          />
        </div>
        {/* Type filter */}
        <div className="flex items-center gap-1.5 flex-wrap">
          <Filter className="w-3.5 h-3.5 text-white/30" />
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
              {t}
            </button>
          ))}
        </div>
      </div>

      {/* Content grid */}
      {isLoading ? (
        <div className="flex items-center gap-3 justify-center py-16 text-white/40">
          <Loader2 className="w-5 h-5 animate-spin" />
          <span className="text-sm">Loading library…</span>
        </div>
      ) : filtered.length === 0 ? (
        <div className="text-center py-16 text-white/30">
          <Library className="w-12 h-12 mx-auto mb-3 opacity-30" />
          <p className="font-medium">No resources found</p>
          <p className="text-sm mt-1">{content.length === 0 ? 'Upload your first resource to get started.' : 'Try adjusting the filters.'}</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {filtered.map(item => <ContentCard key={item.id} item={item} />)}
        </div>
      )}

      {/* Stats footer */}
      {content.length > 0 && (
        <p className="text-xs text-white/30 text-center">
          Showing {filtered.length} of {content.length} resources
        </p>
      )}

      {/* Upload modal */}
      <AnimatePresence>
        {showUpload && centerId && (
          <UploadModal centerId={centerId} batches={batches} onClose={() => setShowUpload(false)} />
        )}
      </AnimatePresence>
    </div>
  );
}
