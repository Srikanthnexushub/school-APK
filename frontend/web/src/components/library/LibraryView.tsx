// Shared read-only library view — student, parent, teacher.
// Uses GET /api/v1/library/search (FTS + filters, AVAILABLE items only).
// Download → GET /api/v1/centers/{centerId}/content/{id}/download → presigned URL.
import { useState, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useQuery } from '@tanstack/react-query';
import {
  FileText, Video, File, Music, BookOpen, Search, Download,
  Filter, Library, Loader2, ChevronLeft, ChevronRight,
  X, Flame, TrendingUp, Hash, Calendar, Star, ExternalLink,
} from 'lucide-react';
import { toast } from 'sonner';
import { cn } from '../../lib/utils';
import api from '../../lib/api';

// ─── Types ────────────────────────────────────────────────────────────────────

interface ContentItem {
  id: string;
  centerId: string;
  title: string;
  description?: string;
  type: string;
  subject?: string;
  board?: string;
  classGrade?: string;
  yearOfPaper?: number;
  examType?: string;
  difficulty?: string;
  language?: string;
  tags?: string[];
  downloadCount: number;
  aiSummary?: string;
  fileSizeBytes?: number;
  pageCount?: number;
  fileUrl?: string;       // backend field name (used as external URL for LINK/VIDEO types)
  externalUrl?: string;   // alias kept for compatibility
  isPlatformResource?: boolean;
  createdAt: string;
}

// ─── Constants ────────────────────────────────────────────────────────────────

const TYPE_COLOR: Record<string, string> = {
  PDF:                 'bg-red-500/15 text-red-400 border-red-500/20',
  VIDEO:               'bg-purple-500/15 text-purple-400 border-purple-500/20',
  DOCUMENT:            'bg-sky-500/15 text-sky-400 border-sky-500/20',
  AUDIO:               'bg-amber-500/15 text-amber-400 border-amber-500/20',
  PREVIOUS_YEAR_PAPER: 'bg-orange-500/15 text-orange-400 border-orange-500/20',
  NOTES:               'bg-cyan-500/15 text-cyan-400 border-cyan-500/20',
  FORMULA_SHEET:       'bg-violet-500/15 text-violet-400 border-violet-500/20',
  MIND_MAP:            'bg-green-500/15 text-green-400 border-green-500/20',
  QUIZ_REF:            'bg-teal-500/15 text-teal-400 border-teal-500/20',
  LINK:                'bg-blue-500/15 text-blue-400 border-blue-500/20',
};

const TYPE_ICON: Record<string, React.ElementType> = {
  PDF: FileText, VIDEO: Video, DOCUMENT: File, AUDIO: Music,
  PREVIOUS_YEAR_PAPER: BookOpen, NOTES: FileText, FORMULA_SHEET: FileText,
  MIND_MAP: FileText, QUIZ_REF: FileText, LINK: ExternalLink,
};

const TYPE_LABEL: Record<string, string> = {
  PDF: 'PDF', VIDEO: 'Video', DOCUMENT: 'Document', AUDIO: 'Audio',
  PREVIOUS_YEAR_PAPER: 'Past Paper', NOTES: 'Notes',
  FORMULA_SHEET: 'Formula Sheet', MIND_MAP: 'Mind Map', QUIZ_REF: 'Quiz',
  LINK: 'Link',
};

const DIFF_COLOR: Record<string, string> = {
  EASY:   'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
  MEDIUM: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
  HARD:   'bg-red-500/10 text-red-400 border-red-500/20',
};

const BOARDS   = ['CBSE', 'ICSE', 'STATE_BOARD', 'IB', 'CAMBRIDGE', 'OTHER'];
const EXAM_TYPES = [
  { value: 'BOARD_EXAM', label: 'Board Exam' },
  { value: 'JEE',        label: 'JEE' },
  { value: 'NEET',       label: 'NEET' },
  { value: 'UPSC',       label: 'UPSC' },
  { value: 'GATE',       label: 'GATE' },
  { value: 'UNIT_TEST',  label: 'Unit Test' },
  { value: 'MOCK',       label: 'Mock Test' },
  { value: 'PRACTICE',   label: 'Practice' },
];
const DIFFICULTIES = ['EASY', 'MEDIUM', 'HARD'];
const PAGE_SIZE = 12;

// ─── Helpers ──────────────────────────────────────────────────────────────────

function formatBytes(b?: number): string {
  if (!b) return '';
  if (b < 1024 * 1024) return `${(b / 1024).toFixed(0)} KB`;
  return `${(b / (1024 * 1024)).toFixed(1)} MB`;
}

// ─── Content Card ─────────────────────────────────────────────────────────────

function ContentCard({ item }: { item: ContentItem }) {
  const [downloading, setDownloading] = useState(false);
  const Icon       = TYPE_ICON[item.type] ?? File;
  const typeColor  = TYPE_COLOR[item.type] ?? 'bg-black/5 dark:bg-white/5 text-white/50 border-black/10 dark:border-white/10';
  const typeLabel  = TYPE_LABEL[item.type] ?? item.type;

  async function handleDownload() {
    if (downloading) return;
    setDownloading(true);
    try {
      const res = await api.get(
        `/api/v1/centers/${item.centerId}/content/${item.id}/download`,
      );
      const url = res.data?.downloadUrl ?? res.data;
      if (!url || typeof url !== 'string') {
        toast.error('No file available for download.');
        return;
      }
      window.open(url, '_blank', 'noopener');
    } catch {
      toast.error('Download failed. Please try again.');
    } finally {
      setDownloading(false);
    }
  }

  function extractYouTubeId(url: string): string | null {
    const m = url.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/)([A-Za-z0-9_-]{11})/);
    return m ? m[1] : null;
  }

  const linkUrl     = item.fileUrl || item.externalUrl;
  const isLink      = item.type === 'LINK' || (item.type === 'VIDEO' && !!linkUrl);
  const hasValidUrl = isLink && !!linkUrl?.trim();
  const ytId        = linkUrl ? extractYouTubeId(linkUrl) : null;

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      whileHover={{ y: -2 }}
      className="bg-surface-100/40 border border-white/5 rounded-xl p-4 flex flex-col gap-3 hover:border-white/10 transition-all"
    >
      {/* Top row — icon + type badge */}
      <div className="flex items-start justify-between gap-2">
        <div className={cn('p-2.5 rounded-lg border flex-shrink-0', typeColor)}>
          <Icon className="w-4 h-4" />
        </div>
        <div className="flex flex-wrap gap-1 justify-end">
          <span className={cn(
            'text-[10px] font-semibold uppercase tracking-wider px-2 py-0.5 rounded-full border flex-shrink-0',
            typeColor,
          )}>
            {typeLabel}
          </span>
          {item.difficulty && (
            <span className={cn(
              'text-[10px] font-semibold uppercase tracking-wider px-2 py-0.5 rounded-full border flex-shrink-0',
              DIFF_COLOR[item.difficulty] ?? 'bg-black/5 dark:bg-white/5 text-white/30 border-black/10 dark:border-white/10',
            )}>
              {item.difficulty}
            </span>
          )}
        </div>
      </div>

      {/* YouTube thumbnail */}
      {ytId && (
        <div className="rounded-lg overflow-hidden -mx-1">
          <img
            src={`https://img.youtube.com/vi/${ytId}/hqdefault.jpg`}
            alt={item.title}
            className="w-full h-24 object-cover opacity-80"
            loading="lazy"
          />
        </div>
      )}

      {/* Title */}
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold text-white leading-tight line-clamp-2">
          {item.title}
        </p>

        {/* AI summary */}
        {item.aiSummary && (
          <p className="text-xs text-white/40 mt-1.5 line-clamp-2 leading-relaxed">
            {item.aiSummary}
          </p>
        )}
        {!item.aiSummary && item.description && (
          <p className="text-xs text-white/40 mt-1.5 line-clamp-2">
            {item.description}
          </p>
        )}
      </div>

      {/* Metadata badges */}
      <div className="flex flex-wrap gap-1">
        {item.subject && (
          <span className="text-[10px] px-1.5 py-0.5 bg-brand-500/10 border border-brand-500/15 rounded text-brand-300/80">
            {item.subject}
          </span>
        )}
        {item.board && (
          <span className="text-[10px] px-1.5 py-0.5 bg-black/5 dark:bg-white/5 border border-black/10 dark:border-white/10 rounded text-white/50">
            {item.board}
          </span>
        )}
        {item.yearOfPaper && (
          <span className="text-[10px] px-1.5 py-0.5 bg-black/5 dark:bg-white/5 border border-black/10 dark:border-white/10 rounded text-white/50 flex items-center gap-0.5">
            <Calendar className="w-2.5 h-2.5" />
            {item.yearOfPaper}
          </span>
        )}
        {item.examType && (
          <span className="text-[10px] px-1.5 py-0.5 bg-indigo-500/10 border border-indigo-500/15 rounded text-indigo-300/80">
            {EXAM_TYPES.find(e => e.value === item.examType)?.label ?? item.examType}
          </span>
        )}
      </div>

      {/* Footer */}
      <div className="flex items-center justify-between text-xs text-white/30">
        <div className="flex items-center gap-2">
          {formatBytes(item.fileSizeBytes) && (
            <span>{formatBytes(item.fileSizeBytes)}</span>
          )}
          {item.pageCount && (
            <span className="flex items-center gap-0.5">
              <Hash className="w-2.5 h-2.5" />{item.pageCount}pp
            </span>
          )}
          {item.downloadCount > 0 && (
            <span className="flex items-center gap-0.5">
              <Download className="w-2.5 h-2.5" />{item.downloadCount}
            </span>
          )}
        </div>
        <span>{new Date(item.createdAt).toLocaleDateString()}</span>
      </div>

      {/* Action button */}
      {isLink ? (
        hasValidUrl ? (
          <a
            href={linkUrl!}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center justify-center gap-1.5 text-xs font-medium text-blue-400 hover:text-blue-300 bg-blue-500/10 hover:bg-blue-500/15 border border-blue-500/20 rounded-lg py-2 transition-colors"
          >
            <ExternalLink className="w-3.5 h-3.5" />
            Open
          </a>
        ) : (
          <span className="flex items-center justify-center gap-1.5 text-xs font-medium text-white/20 bg-black/5 dark:bg-white/5 border border-black/10 dark:border-white/10 rounded-lg py-2 cursor-not-allowed">
            <ExternalLink className="w-3.5 h-3.5" />
            URL not set
          </span>
        )
      ) : (
        <button
          onClick={handleDownload}
          disabled={downloading}
          className="flex items-center justify-center gap-1.5 text-xs font-medium text-brand-400 hover:text-brand-300 bg-brand-500/10 hover:bg-brand-500/15 border border-brand-500/20 rounded-lg py-2 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {downloading
            ? <Loader2 className="w-3.5 h-3.5 animate-spin" />
            : <Download className="w-3.5 h-3.5" />}
          {downloading ? 'Preparing…' : 'Download'}
        </button>
      )}
    </motion.div>
  );
}

// ─── Skeletons ────────────────────────────────────────────────────────────────

function CardSkeleton() {
  return (
    <div className="bg-surface-100/40 border border-white/5 rounded-xl p-4 animate-pulse space-y-3">
      <div className="flex justify-between">
        <div className="h-8 w-8 bg-white/8 rounded-lg" />
        <div className="h-4 w-20 bg-white/8 rounded-full" />
      </div>
      <div className="space-y-1.5">
        <div className="h-3.5 bg-white/8 rounded w-full" />
        <div className="h-3.5 bg-white/5 rounded w-3/4" />
      </div>
      <div className="flex gap-1">
        <div className="h-4 w-14 bg-white/5 rounded" />
        <div className="h-4 w-10 bg-white/5 rounded" />
      </div>
      <div className="h-8 bg-white/5 rounded-lg" />
    </div>
  );
}

// ─── Main Component ───────────────────────────────────────────────────────────

interface LibraryViewProps {
  centerId?: string;
  isResolvingCenter?: boolean;
  emptyHint?: string;
  /** When true, show a platform-wide search banner */
  globalMode?: boolean;
  /** Student profile stream — passed for platform resource discovery */
  stream?: string;
  /** When true, also fetch platform-wide resources (isPlatformResource=true) */
  showPlatformResources?: boolean;
}

export function LibraryView({
  centerId,
  isResolvingCenter = false,
  emptyHint,
  globalMode = false,
  stream,
  showPlatformResources = false,
}: LibraryViewProps) {
  const [keyword,     setKeyword]     = useState('');
  const [debounced,   setDebounced]   = useState('');
  const [subject,     setSubject]     = useState('');
  const [board,       setBoard]       = useState('');
  const [examType,    setExamType]    = useState('');
  const [difficulty,  setDifficulty]  = useState('');
  const [yearOfPaper, setYearOfPaper] = useState('');
  const [page,        setPage]        = useState(0);
  const [showFilters, setShowFilters] = useState(false);

  const debounceRef = useRef<ReturnType<typeof setTimeout>>();

  function handleKeyword(v: string) {
    setKeyword(v);
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => { setDebounced(v); setPage(0); }, 400);
  }

  function clearFilters() {
    setKeyword(''); setDebounced(''); setSubject(''); setBoard('');
    setExamType(''); setDifficulty(''); setYearOfPaper(''); setPage(0);
  }

  const hasFilters = !!(debounced || subject || board || examType || difficulty || yearOfPaper);

  const { data, isLoading, isFetching } = useQuery({
    queryKey: [
      'library-search', centerId, debounced, subject, board, examType,
      difficulty, yearOfPaper, page, stream, showPlatformResources,
    ],
    queryFn: async () => {
      const p = new URLSearchParams();
      if (debounced)             p.set('q',            debounced);
      if (subject)               p.set('subject',      subject);
      if (board)                 p.set('board',        board);
      if (examType)              p.set('examType',     examType);
      if (difficulty)            p.set('difficulty',   difficulty);
      if (yearOfPaper)           p.set('yearOfPaper',  yearOfPaper);
      if (centerId)              p.set('centerId',     centerId);
      // Only filter by stream for platform-wide discovery. Center content is shown unfiltered
      // so items without a stream tag are visible (most center-uploaded content has no stream set).
      if (stream && (globalMode || showPlatformResources)) p.set('stream', stream);
      if (showPlatformResources) p.set('platformOnly', 'true');
      p.set('page', String(page));
      p.set('size', String(PAGE_SIZE));
      const res = await api.get(`/api/v1/library/search?${p.toString()}`);
      const d = res.data;
      if (Array.isArray(d)) return { items: d, total: d.length, totalPages: 1 };
      return {
        items:      d.content       ?? [],
        total:      d.totalElements ?? 0,
        totalPages: d.totalPages    ?? 1,
      };
    },
    enabled: !isResolvingCenter,
    staleTime: 30_000,
  });

  const items      = (data?.items ?? []) as ContentItem[];
  const total      = data?.total ?? 0;
  const totalPages = data?.totalPages ?? 1;

  // ── Loading state ────────────────────────────────────────────────────────

  if (isResolvingCenter) {
    return (
      <div className="flex items-center gap-3 justify-center py-20 text-white/40">
        <Loader2 className="w-5 h-5 animate-spin" />
        <span className="text-sm">Resolving library…</span>
      </div>
    );
  }

  return (
    <div className="space-y-5">

      {/* Global discovery banner */}
      {globalMode && !centerId && (
        <div className="flex items-center gap-2 px-4 py-3 bg-brand-500/10 border border-brand-500/20 rounded-xl text-xs text-brand-300">
          <TrendingUp className="w-4 h-4 flex-shrink-0" />
          Searching across <strong>all centers</strong> on the platform.
          {centerId && ' Filtered to your center.'}
        </div>
      )}

      {/* Search bar */}
      <div className="flex gap-3">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
          <input
            value={keyword}
            onChange={e => handleKeyword(e.target.value)}
            placeholder="Search by title, subject, keyword…"
            className="w-full bg-surface-100/50 border border-white/10 rounded-xl pl-9 pr-9 py-2.5 text-sm text-white placeholder:text-white/25 focus:outline-none focus:border-brand-500/50 transition-colors"
          />
          {keyword && (
            <button
              onClick={() => handleKeyword('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-white/30 hover:text-white/60"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
        </div>

        <button
          onClick={() => setShowFilters(f => !f)}
          className={cn(
            'flex items-center gap-1.5 px-3 py-2.5 rounded-xl border text-xs font-medium transition-colors',
            showFilters || hasFilters
              ? 'bg-brand-500/15 border-brand-500/30 text-brand-300'
              : 'bg-black/5 dark:bg-white/5 border-black/10 dark:border-white/10 text-white/40 hover:text-white',
          )}
        >
          <Filter className="w-3.5 h-3.5" />
          Filters
          {hasFilters && (
            <span className="bg-brand-500 text-white text-[10px] font-bold rounded-full w-4 h-4 flex items-center justify-center leading-none">
              {[board, examType, difficulty, yearOfPaper, subject].filter(Boolean).length}
            </span>
          )}
        </button>
      </div>

      {/* Filter panel */}
      <AnimatePresence>
        {showFilters && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            className="overflow-hidden"
          >
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3 p-4 bg-white/2 border border-white/8 rounded-xl">

              {/* Subject */}
              <div>
                <label className="block text-[10px] text-white/40 mb-1 font-medium uppercase tracking-wider">Subject</label>
                <input
                  value={subject}
                  onChange={e => { setSubject(e.target.value); setPage(0); }}
                  placeholder="e.g. Physics"
                  className="w-full bg-surface-100/50 border border-white/10 rounded-lg px-2.5 py-1.5 text-xs text-white placeholder:text-white/20 focus:outline-none focus:border-brand-500/50 transition-colors"
                />
              </div>

              {/* Board */}
              <div>
                <label className="block text-[10px] text-white/40 mb-1 font-medium uppercase tracking-wider">Board</label>
                <select
                  value={board}
                  onChange={e => { setBoard(e.target.value); setPage(0); }}
                  className="w-full bg-surface-100/50 border border-white/10 rounded-lg px-2.5 py-1.5 text-xs text-white focus:outline-none focus:border-brand-500/50 transition-colors"
                >
                  <option value="">All boards</option>
                  {BOARDS.map(b => <option key={b} value={b} className="bg-surface-100">{b}</option>)}
                </select>
              </div>

              {/* Exam Type */}
              <div>
                <label className="block text-[10px] text-white/40 mb-1 font-medium uppercase tracking-wider">Exam Type</label>
                <select
                  value={examType}
                  onChange={e => { setExamType(e.target.value); setPage(0); }}
                  className="w-full bg-surface-100/50 border border-white/10 rounded-lg px-2.5 py-1.5 text-xs text-white focus:outline-none focus:border-brand-500/50 transition-colors"
                >
                  <option value="">All types</option>
                  {EXAM_TYPES.map(e => <option key={e.value} value={e.value} className="bg-surface-100">{e.label}</option>)}
                </select>
              </div>

              {/* Difficulty */}
              <div>
                <label className="block text-[10px] text-white/40 mb-1 font-medium uppercase tracking-wider">Difficulty</label>
                <select
                  value={difficulty}
                  onChange={e => { setDifficulty(e.target.value); setPage(0); }}
                  className="w-full bg-surface-100/50 border border-white/10 rounded-lg px-2.5 py-1.5 text-xs text-white focus:outline-none focus:border-brand-500/50 transition-colors"
                >
                  <option value="">All levels</option>
                  {DIFFICULTIES.map(d => <option key={d} value={d} className="bg-surface-100">{d}</option>)}
                </select>
              </div>

              {/* Year */}
              <div>
                <label className="block text-[10px] text-white/40 mb-1 font-medium uppercase tracking-wider">Year</label>
                <input
                  type="number"
                  value={yearOfPaper}
                  onChange={e => { setYearOfPaper(e.target.value); setPage(0); }}
                  placeholder="e.g. 2023"
                  min="1990"
                  max={new Date().getFullYear()}
                  className="w-full bg-surface-100/50 border border-white/10 rounded-lg px-2.5 py-1.5 text-xs text-white placeholder:text-white/20 focus:outline-none focus:border-brand-500/50 transition-colors"
                />
              </div>
            </div>

            {hasFilters && (
              <button
                onClick={clearFilters}
                className="mt-2 flex items-center gap-1 text-xs text-red-400/70 hover:text-red-400 transition-colors"
              >
                <X className="w-3 h-3" /> Clear all filters
              </button>
            )}
          </motion.div>
        )}
      </AnimatePresence>

      {/* Results header */}
      {!isLoading && (
        <div className="flex items-center justify-between">
          <p className="text-xs text-white/30">
            {isFetching && <Loader2 className="w-3 h-3 animate-spin inline mr-1.5" />}
            {total > 0
              ? `${total} result${total !== 1 ? 's' : ''}${hasFilters ? ' matching filters' : ''}`
              : hasFilters ? 'No results for these filters' : 'No documents available'}
          </p>
          {total > 0 && totalPages > 1 && (
            <p className="text-xs text-white/30">Page {page + 1} of {totalPages}</p>
          )}
        </div>
      )}

      {/* Grid */}
      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {[...Array(8)].map((_, i) => <CardSkeleton key={i} />)}
        </div>
      ) : items.length === 0 ? (
        <div className="text-center py-20 border border-dashed border-white/8 rounded-xl">
          <Library className="w-12 h-12 text-white/10 mx-auto mb-4" />
          <p className="text-white/40 font-medium text-sm">
            {hasFilters ? 'No documents match your filters' : 'No documents available yet'}
          </p>
          <p className="text-white/25 text-xs mt-1">
            {hasFilters
              ? 'Try broadening your search'
              : (emptyHint ?? 'Check back soon — your center is building the library.')}
          </p>
          {hasFilters && (
            <button
              onClick={clearFilters}
              className="mt-3 text-xs text-brand-400 hover:text-brand-300 transition-colors"
            >
              Clear filters
            </button>
          )}
        </div>
      ) : (
        <AnimatePresence mode="popLayout">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {items.map(item => <ContentCard key={item.id} item={item} />)}
          </div>
        </AnimatePresence>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between pt-2">
          <button
            onClick={() => setPage(p => Math.max(0, p - 1))}
            disabled={page === 0 || isFetching}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl border border-black/10 dark:border-white/10 text-sm text-white/50 hover:text-white hover:bg-black/5 dark:hover:bg-white/5 transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
          >
            <ChevronLeft className="w-4 h-4" /> Previous
          </button>
          <span className="text-xs text-white/30">Page {page + 1} of {totalPages}</span>
          <button
            onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1 || isFetching}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl border border-black/10 dark:border-white/10 text-sm text-white/50 hover:text-white hover:bg-black/5 dark:hover:bg-white/5 transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
          >
            Next <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Trending strip (only when no filters and has results) */}
      {!hasFilters && !keyword && items.length > 0 && (
        <div className="flex items-center gap-2 text-xs text-white/25 border-t border-white/5 pt-4">
          <Flame className="w-3.5 h-3.5 text-orange-500/40" />
          <span>
            Most downloaded materials appear first. Use filters to narrow your search.
          </span>
          <Star className="w-3 h-3 text-amber-500/30 ml-auto" />
        </div>
      )}
    </div>
  );
}
