// src/pages/admin/AdminJobsPage.tsx
import { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Briefcase, Search, RefreshCw, Loader2, Plus,
  Pencil, Trash2, MapPin, Clock, DollarSign, Calendar,
  ChevronDown, Globe, Building2, ArrowLeft, ArrowRight,
  X, Filter, Users, Check,
} from 'lucide-react';
import { toast } from 'sonner';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../lib/api';
import { cn } from '../../lib/utils';
import { useAuthStore } from '../../stores/authStore';
import {
  STAFF_ROLE_TYPES,
  getStaffRoleConfig,
} from '../../constants/staffConstants';
import {
  JOB_TYPES,
  JOB_STATUSES,
  getJobStatusConfig,
  getJobTypeLabel,
  SUBJECT_ROLES,
  experienceLabel,
  salaryLabel,
  deadlineDaysLeft,
  relativeTime,
} from '../../constants/jobConstants';
import { AnimatePresence as AP } from 'framer-motion';
import PostJobModal, { type JobPosting } from './PostJobModal';

// ─── Sub-tab type ─────────────────────────────────────────────────────────────

type SubTab = 'my-postings' | 'job-board';

// ─── Shared sub-components ────────────────────────────────────────────────────

function StatCard({ label, value, color }: { label: string; value: number; color?: string }) {
  return (
    <div className="bg-surface-100/50 border border-white/8 rounded-xl p-4 flex flex-col gap-1">
      <span className="text-xs font-medium text-white/40">{label}</span>
      <span className={cn('text-2xl font-bold', color ?? 'text-white')}>{value}</span>
    </div>
  );
}

function JobStatusBadge({ status }: { status: string }) {
  const cfg = getJobStatusConfig(status);
  return (
    <span className={cn('inline-flex items-center px-2 py-0.5 rounded-lg text-xs font-medium border', cfg.bg, cfg.color)}>
      {cfg.label}
    </span>
  );
}

function RoleBadge({ roleType }: { roleType: string }) {
  const cfg = getStaffRoleConfig(roleType);
  return (
    <span className={cn('inline-flex px-2 py-0.5 rounded-lg text-xs font-medium border', cfg?.bg ?? 'bg-white/8 border-white/15', cfg?.color ?? 'text-white/40')}>
      {cfg?.label ?? roleType}
    </span>
  );
}

function DeadlineBadge({ deadline }: { deadline: string | null }) {
  if (!deadline) return null;
  const days = deadlineDaysLeft(deadline);
  if (days == null) return null;
  const isUrgent = days <= 7;
  const isPast   = days < 0;

  return (
    <span className={cn(
      'inline-flex items-center gap-1 text-xs',
      isPast   ? 'text-red-400'    :
      isUrgent ? 'text-amber-400'  :
                 'text-white/35'
    )}>
      <Calendar className="w-3 h-3" />
      {isPast
        ? 'Expired'
        : days === 0
        ? 'Due today'
        : `${days}d left`}
    </span>
  );
}

// ─── Status Dropdown (for changing job status) ───────────────────────────────

function StatusDropdown({
  current,
  jobId,
  centerId,
  onChanged,
}: {
  current: string;
  jobId: string;
  centerId: string;
  onChanged: () => void;
}) {
  const [open, setOpen]         = useState(false);
  const [loading, setLoading]   = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  async function changeStatus(newStatus: string) {
    if (newStatus === current) { setOpen(false); return; }
    setLoading(true);
    setOpen(false);
    try {
      await api.patch(`/api/v1/centers/${centerId}/jobs/${jobId}/status`, { status: newStatus });
      toast.success(`Status updated to ${getJobStatusConfig(newStatus).label}`);
      onChanged();
    } catch {
      toast.error('Failed to update status');
    } finally {
      setLoading(false);
    }
  }

  const cfg = getJobStatusConfig(current);

  return (
    <div ref={ref} className="relative">
      <button
        onClick={() => setOpen(v => !v)}
        disabled={loading}
        className={cn(
          'flex items-center gap-1 px-2 py-1 rounded-lg border text-xs font-medium transition-colors',
          cfg.bg, cfg.color,
          'hover:opacity-80 disabled:opacity-50'
        )}
      >
        {loading ? <Loader2 className="w-3 h-3 animate-spin" /> : cfg.label}
        <ChevronDown className={cn('w-3 h-3 transition-transform', open && 'rotate-180')} />
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, y: -4, scale: 0.97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -4, scale: 0.97 }}
            transition={{ duration: 0.1 }}
            className="absolute right-0 top-full mt-1 z-30 bg-surface-100 border border-white/10 rounded-xl shadow-xl overflow-hidden min-w-[120px]"
          >
            {JOB_STATUSES.map(s => (
              <button
                key={s.value}
                onClick={() => changeStatus(s.value)}
                className={cn(
                  'flex items-center gap-2 w-full px-3 py-2 text-xs text-left hover:bg-white/5 transition-colors',
                  s.color
                )}
              >
                {current === s.value && <Check className="w-3 h-3 flex-shrink-0" />}
                {current !== s.value && <span className="w-3" />}
                {s.label}
              </button>
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

// ─── Job Card (own postings) ──────────────────────────────────────────────────

function OwnJobCard({
  job,
  centerId,
  onEdit,
  onDeleted,
  onStatusChanged,
}: {
  job: JobPosting;
  centerId: string;
  onEdit: (job: JobPosting) => void;
  onDeleted: () => void;
  onStatusChanged: () => void;
}) {
  const [deleting, setDeleting] = useState(false);
  const subjectList = job.subjects?.split(',').map(s => s.trim()).filter(Boolean) ?? [];
  const salary = salaryLabel(job.salaryMin, job.salaryMax);
  const exp    = experienceLabel(job.experienceMinYears);

  async function handleDelete() {
    if (!confirm(`Delete "${job.title}"? This cannot be undone.`)) return;
    setDeleting(true);
    try {
      await api.delete(`/api/v1/centers/${centerId}/jobs/${job.id}`);
      toast.success('Job deleted');
      onDeleted();
    } catch {
      toast.error('Failed to delete job');
    } finally {
      setDeleting(false);
    }
  }

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-white/3 border border-white/8 rounded-xl p-4 hover:bg-white/5 hover:border-white/12 transition-colors"
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex-1 min-w-0">
          {/* Title + status */}
          <div className="flex flex-wrap items-center gap-2 mb-1">
            <h3 className="text-sm font-semibold text-white truncate">{job.title}</h3>
            <JobStatusBadge status={job.status} />
          </div>

          {/* Role + job type */}
          <div className="flex flex-wrap items-center gap-1.5 mb-2">
            <RoleBadge roleType={job.roleType} />
            <span className="text-xs text-white/35 border border-white/10 px-1.5 py-0.5 rounded-lg">
              {getJobTypeLabel(job.jobType)}
            </span>
          </div>

          {/* Subjects */}
          {subjectList.length > 0 && (
            <div className="flex flex-wrap gap-1 mb-2">
              {subjectList.map(s => (
                <span key={s} className="text-xs px-1.5 py-0.5 bg-brand-500/10 border border-brand-500/15 rounded text-brand-300/80">{s}</span>
              ))}
            </div>
          )}

          {/* Meta row */}
          <div className="flex flex-wrap items-center gap-3 text-xs text-white/35">
            {exp && (
              <span className="flex items-center gap-1">
                <Clock className="w-3 h-3" /> {exp}
              </span>
            )}
            {salary && (
              <span className="flex items-center gap-1">
                <DollarSign className="w-3 h-3" /> {salary}
              </span>
            )}
            {job.deadline && <DeadlineBadge deadline={job.deadline} />}
            <span className="flex items-center gap-1">
              <Briefcase className="w-3 h-3" /> Posted {relativeTime(job.postedAt)}
            </span>
          </div>
        </div>

        {/* Actions */}
        <div className="flex items-center gap-1.5 flex-shrink-0">
          <StatusDropdown
            current={job.status}
            jobId={job.id}
            centerId={centerId}
            onChanged={onStatusChanged}
          />
          <button
            onClick={() => onEdit(job)}
            className="p-1.5 rounded-lg border border-white/8 text-white/40 hover:text-white hover:border-white/20 hover:bg-white/5 transition-colors"
            title="Edit"
          >
            <Pencil className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={handleDelete}
            disabled={deleting}
            className="p-1.5 rounded-lg border border-white/8 text-white/40 hover:text-red-400 hover:border-red-500/30 hover:bg-red-500/5 transition-colors disabled:opacity-50"
            title="Delete"
          >
            {deleting ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Trash2 className="w-3.5 h-3.5" />}
          </button>
        </div>
      </div>
    </motion.div>
  );
}

// ─── Job Board Card (read-only) ───────────────────────────────────────────────

function BoardJobCard({ job }: { job: JobPosting }) {
  const subjectList = job.subjects?.split(',').map(s => s.trim()).filter(Boolean) ?? [];
  const salary = salaryLabel(job.salaryMin, job.salaryMax);
  const exp    = experienceLabel(job.experienceMinYears);

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-white/3 border border-white/8 rounded-xl p-4 hover:bg-white/5 hover:border-white/12 transition-colors"
    >
      {/* Center info */}
      <div className="flex items-center gap-1.5 mb-2 text-xs text-white/35">
        <Building2 className="w-3 h-3" />
        <span className="font-medium text-white/50">{job.centerName}</span>
        {job.centerCity && (
          <>
            <span className="text-white/20">·</span>
            <MapPin className="w-3 h-3" />
            <span>{job.centerCity}</span>
          </>
        )}
      </div>

      {/* Title */}
      <h3 className="text-sm font-semibold text-white mb-1.5">{job.title}</h3>

      {/* Role + job type */}
      <div className="flex flex-wrap items-center gap-1.5 mb-2">
        <RoleBadge roleType={job.roleType} />
        <span className="text-xs text-white/35 border border-white/10 px-1.5 py-0.5 rounded-lg">
          {getJobTypeLabel(job.jobType)}
        </span>
      </div>

      {/* Subjects */}
      {subjectList.length > 0 && (
        <div className="flex flex-wrap gap-1 mb-2">
          {subjectList.map(s => (
            <span key={s} className="text-xs px-1.5 py-0.5 bg-brand-500/10 border border-brand-500/15 rounded text-brand-300/80">{s}</span>
          ))}
        </div>
      )}

      {/* Meta */}
      <div className="flex flex-wrap items-center gap-3 text-xs text-white/35 mt-2">
        {exp && (
          <span className="flex items-center gap-1">
            <Clock className="w-3 h-3" /> {exp}
          </span>
        )}
        {salary && (
          <span className="flex items-center gap-1">
            <DollarSign className="w-3 h-3" /> {salary}
          </span>
        )}
        {job.deadline && <DeadlineBadge deadline={job.deadline} />}
        <span className="flex items-center gap-1">
          <Briefcase className="w-3 h-3" /> Posted {relativeTime(job.postedAt)}
        </span>
      </div>
    </motion.div>
  );
}

// ─── Loading Skeleton ─────────────────────────────────────────────────────────

function JobCardSkeleton() {
  return (
    <div className="bg-white/3 border border-white/8 rounded-xl p-4 animate-pulse space-y-2.5">
      <div className="flex items-center gap-2">
        <div className="h-4 w-48 bg-white/8 rounded" />
        <div className="h-4 w-16 bg-white/8 rounded-lg" />
      </div>
      <div className="flex gap-2">
        <div className="h-4 w-20 bg-white/5 rounded-lg" />
        <div className="h-4 w-16 bg-white/5 rounded-lg" />
      </div>
      <div className="flex gap-3">
        <div className="h-3 w-24 bg-white/5 rounded" />
        <div className="h-3 w-20 bg-white/5 rounded" />
      </div>
    </div>
  );
}

// ─── My Postings Sub-tab ──────────────────────────────────────────────────────

function MyPostingsTab({ centerId }: { centerId: string }) {
  const queryClient   = useQueryClient();
  const [showModal, setShowModal]     = useState(false);
  const [editingJob, setEditingJob]   = useState<JobPosting | null>(null);
  const [search, setSearch]           = useState('');
  const [filterStatus, setFilterStatus] = useState('');
  const [filterRole, setFilterRole]   = useState('');

  const { data: jobs = [], isLoading, refetch } = useQuery<JobPosting[]>({
    queryKey: ['jobs', centerId],
    queryFn: async () => {
      const res = await api.get(`/api/v1/centers/${centerId}/jobs`);
      const d = res.data;
      return Array.isArray(d) ? d : (d.content ?? []);
    },
    enabled: !!centerId,
  });

  // Stats
  const totalCount  = jobs.length;
  const openCount   = jobs.filter(j => j.status === 'OPEN').length;
  const draftCount  = jobs.filter(j => j.status === 'DRAFT').length;
  const closedCount = jobs.filter(j => j.status === 'CLOSED' || j.status === 'FILLED').length;

  // Filter
  const filtered = jobs.filter(j => {
    const q = search.toLowerCase();
    const matchSearch = !q || j.title.toLowerCase().includes(q) ||
      (getStaffRoleConfig(j.roleType)?.label ?? j.roleType).toLowerCase().includes(q) ||
      j.subjects?.toLowerCase().includes(q) || false;
    const matchStatus = !filterStatus || j.status === filterStatus;
    const matchRole   = !filterRole   || j.roleType === filterRole;
    return matchSearch && matchStatus && matchRole;
  });

  const hasFilters = !!filterStatus || !!filterRole;

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: ['jobs', centerId] });
  }

  function openCreate() {
    if (!centerId) {
      toast.error('Centre ID not found. Please sign out and sign in again.');
      return;
    }
    setEditingJob(null);
    setShowModal(true);
  }

  function openEdit(job: JobPosting) {
    setEditingJob(job);
    setShowModal(true);
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            <Briefcase className="w-5 h-5 text-brand-400" /> Job Postings
          </h2>
          <p className="text-sm text-white/40 mt-0.5">
            Manage open positions and hiring campaigns for your institution
          </p>
        </div>
        <button
          onClick={openCreate}
          className="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium bg-brand-600 hover:bg-brand-500 text-white transition-colors flex-shrink-0"
        >
          <Plus className="w-4 h-4" /> Post a Job
        </button>
      </div>

      {/* Stats */}
      {!isLoading && (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
          <StatCard label="Total Postings" value={totalCount} />
          <StatCard label="Open"           value={openCount}  color="text-emerald-400" />
          <StatCard label="Draft"          value={draftCount} color="text-white/50" />
          <StatCard label="Closed / Filled" value={closedCount} color="text-white/35" />
        </div>
      )}

      {/* Search + filters */}
      <div className="space-y-2">
        <div className="flex items-center gap-2">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
            <input
              value={search}
              onChange={e => setSearch(e.target.value)}
              placeholder="Search by title, role, subject…"
              className="w-full bg-white/5 border border-white/10 rounded-xl pl-9 pr-4 py-2.5 text-sm text-white placeholder:text-white/25 focus:outline-none focus:border-brand-500/50 transition-colors"
            />
            {search && (
              <button onClick={() => setSearch('')} className="absolute right-3 top-1/2 -translate-y-1/2 text-white/30 hover:text-white/60">
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>
          <button
            onClick={() => refetch()}
            className="p-2.5 rounded-xl border border-white/10 text-white/40 hover:text-white/70 hover:bg-white/3 transition-colors"
            title="Refresh"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
        </div>

        {/* Status filter pills */}
        <div className="flex flex-wrap items-center gap-1.5">
          <span className="text-xs text-white/30 mr-1 flex items-center gap-1">
            <Filter className="w-3 h-3" /> Status:
          </span>
          {[{ value: '', label: 'All' }, ...JOB_STATUSES].map(s => (
            <button
              key={s.value}
              onClick={() => setFilterStatus(s.value)}
              className={cn(
                'px-2.5 py-1 rounded-lg text-xs font-medium transition-colors border',
                filterStatus === s.value
                  ? 'bg-white/12 border-white/20 text-white'
                  : 'border-white/8 text-white/40 hover:border-white/15 hover:text-white/70'
              )}
            >
              {s.label}
            </button>
          ))}
          <span className="text-xs text-white/30 ml-2 mr-1">Role:</span>
          {[{ value: '', label: 'All' }, ...STAFF_ROLE_TYPES.map(r => ({ value: r.value, label: r.label }))].map(r => (
            <button
              key={r.value}
              onClick={() => setFilterRole(r.value)}
              className={cn(
                'px-2.5 py-1 rounded-lg text-xs font-medium transition-colors border',
                filterRole === r.value
                  ? 'bg-white/12 border-white/20 text-white'
                  : 'border-white/8 text-white/40 hover:border-white/15 hover:text-white/70'
              )}
            >
              {r.label}
            </button>
          ))}
          {hasFilters && (
            <button
              onClick={() => { setFilterStatus(''); setFilterRole(''); }}
              className="flex items-center gap-1 text-xs text-red-400/70 hover:text-red-400 transition-colors ml-1"
            >
              <X className="w-3 h-3" /> Clear
            </button>
          )}
        </div>
      </div>

      {/* Content */}
      {isLoading ? (
        <div className="space-y-3">
          {[...Array(3)].map((_, i) => <JobCardSkeleton key={i} />)}
        </div>
      ) : filtered.length === 0 ? (
        <div className="text-center py-20 border border-dashed border-white/8 rounded-xl">
          <Briefcase className="w-12 h-12 text-white/10 mx-auto mb-3" />
          <p className="text-white/40 text-sm font-medium">
            {jobs.length === 0
              ? 'No job postings yet'
              : 'No postings match your filters'}
          </p>
          <p className="text-white/25 text-xs mt-1 mb-4">
            {jobs.length === 0
              ? 'Create your first job posting to start hiring'
              : 'Try adjusting your search or filters'}
          </p>
          {jobs.length === 0 && (
            <button
              onClick={openCreate}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium bg-brand-600 hover:bg-brand-500 text-white transition-colors"
            >
              <Plus className="w-4 h-4" /> Post a Job
            </button>
          )}
          {hasFilters && (
            <button
              onClick={() => { setFilterStatus(''); setFilterRole(''); setSearch(''); }}
              className="text-xs text-brand-400 hover:text-brand-300 transition-colors"
            >
              Clear all filters
            </button>
          )}
        </div>
      ) : (
        <div className="space-y-3">
          <p className="text-xs text-white/30">
            {filtered.length} of {totalCount} posting{totalCount !== 1 ? 's' : ''}
            {(search || hasFilters) ? ' matching filters' : ''}
          </p>
          <AnimatePresence mode="popLayout">
            {filtered.map(job => (
              <OwnJobCard
                key={job.id}
                job={job}
                centerId={centerId}
                onEdit={openEdit}
                onDeleted={invalidate}
                onStatusChanged={invalidate}
              />
            ))}
          </AnimatePresence>
        </div>
      )}

      {/* Modal */}
      <AnimatePresence>
        {showModal && (
          <PostJobModal
            centerId={centerId}
            editing={editingJob}
            onClose={() => setShowModal(false)}
            onSaved={() => {
              setShowModal(false);
              invalidate();
            }}
          />
        )}
      </AnimatePresence>
    </div>
  );
}

// ─── Job Board Sub-tab ────────────────────────────────────────────────────────

const JOB_BOARD_PAGE_SIZE = 20;

function JobBoardTab() {
  const [search,      setSearch]      = useState('');
  const [filterRole,  setFilterRole]  = useState('');
  const [filterType,  setFilterType]  = useState('');
  const [filterCity,  setFilterCity]  = useState('');
  const [page,        setPage]        = useState(0);

  // Debounced search trigger
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const debounceRef = useRef<ReturnType<typeof setTimeout>>();
  function handleSearchChange(v: string) {
    setSearch(v);
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setDebouncedSearch(v);
      setPage(0);
    }, 400);
  }

  const queryKey = ['jobs-board', debouncedSearch, filterRole, filterType, filterCity, page];

  const { data, isLoading, isFetching } = useQuery<{ jobs: JobPosting[]; total: number; totalPages: number }>({
    queryKey,
    queryFn: async () => {
      const params = new URLSearchParams();
      if (debouncedSearch) params.set('keyword', debouncedSearch);
      if (filterRole)      params.set('roleType', filterRole);
      if (filterType)      params.set('jobType', filterType);
      if (filterCity)      params.set('city', filterCity);
      params.set('page', String(page));
      params.set('size', String(JOB_BOARD_PAGE_SIZE));
      const res = await api.get(`/api/v1/jobs?${params.toString()}`);
      const d = res.data;
      if (Array.isArray(d)) {
        return { jobs: d, total: d.length, totalPages: 1 };
      }
      return {
        jobs:       d.content ?? [],
        total:      d.totalElements ?? 0,
        totalPages: d.totalPages    ?? 1,
      };
    },
    staleTime: 30_000,
  });

  const jobs       = data?.jobs       ?? [];
  const totalPages = data?.totalPages ?? 1;
  const total      = data?.total      ?? 0;

  const hasFilters = !!filterRole || !!filterType || !!filterCity || !!debouncedSearch;

  function clearFilters() {
    setFilterRole('');
    setFilterType('');
    setFilterCity('');
    setSearch('');
    setDebouncedSearch('');
    setPage(0);
  }

  return (
    <div className="space-y-5">
      {/* Header */}
      <div>
        <h2 className="text-xl font-bold text-white flex items-center gap-2">
          <Globe className="w-5 h-5 text-brand-400" /> Job Board
        </h2>
        <p className="text-sm text-white/40 mt-0.5">
          Browse open positions from all institutions on the platform
        </p>
      </div>

      {/* Search */}
      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
        <input
          value={search}
          onChange={e => handleSearchChange(e.target.value)}
          placeholder="Search job title, keyword…"
          className="w-full bg-white/5 border border-white/10 rounded-xl pl-9 pr-4 py-2.5 text-sm text-white placeholder:text-white/25 focus:outline-none focus:border-brand-500/50 transition-colors"
        />
        {search && (
          <button onClick={() => handleSearchChange('')} className="absolute right-3 top-1/2 -translate-y-1/2 text-white/30 hover:text-white/60">
            <X className="w-3.5 h-3.5" />
          </button>
        )}
      </div>

      {/* Filter row */}
      <div className="flex flex-wrap items-center gap-2">
        <Filter className="w-3.5 h-3.5 text-white/30 flex-shrink-0" />

        {/* Role */}
        <select
          value={filterRole}
          onChange={e => { setFilterRole(e.target.value); setPage(0); }}
          className="bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500/50 transition-colors"
        >
          <option value="">All Roles</option>
          {STAFF_ROLE_TYPES.map(r => (
            <option key={r.value} value={r.value} className="bg-surface-100">{r.label}</option>
          ))}
        </select>

        {/* Job type */}
        <select
          value={filterType}
          onChange={e => { setFilterType(e.target.value); setPage(0); }}
          className="bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500/50 transition-colors"
        >
          <option value="">All Job Types</option>
          {JOB_TYPES.map(t => (
            <option key={t.value} value={t.value} className="bg-surface-100">{t.label}</option>
          ))}
        </select>

        {/* City */}
        <div className="relative">
          <MapPin className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3 h-3 text-white/30" />
          <input
            value={filterCity}
            onChange={e => { setFilterCity(e.target.value); setPage(0); }}
            placeholder="City…"
            className="bg-white/5 border border-white/10 rounded-xl pl-7 pr-3 py-2 text-xs text-white placeholder:text-white/25 focus:outline-none focus:border-brand-500/50 transition-colors w-28"
          />
        </div>

        {hasFilters && (
          <button
            onClick={clearFilters}
            className="flex items-center gap-1 text-xs text-red-400/70 hover:text-red-400 transition-colors"
          >
            <X className="w-3 h-3" /> Clear
          </button>
        )}

        {isFetching && !isLoading && (
          <Loader2 className="w-3.5 h-3.5 animate-spin text-white/30" />
        )}
      </div>

      {/* Results */}
      {isLoading ? (
        <div className="space-y-3">
          {[...Array(4)].map((_, i) => <JobCardSkeleton key={i} />)}
        </div>
      ) : jobs.length === 0 ? (
        <div className="text-center py-20 border border-dashed border-white/8 rounded-xl">
          <Users className="w-12 h-12 text-white/10 mx-auto mb-3" />
          <p className="text-white/40 text-sm font-medium">No open positions found</p>
          <p className="text-white/25 text-xs mt-1">
            {hasFilters ? 'Try adjusting your filters' : 'Check back soon for new opportunities'}
          </p>
          {hasFilters && (
            <button onClick={clearFilters} className="mt-3 text-xs text-brand-400 hover:text-brand-300 transition-colors">
              Clear filters
            </button>
          )}
        </div>
      ) : (
        <>
          <p className="text-xs text-white/30">
            {total} open position{total !== 1 ? 's' : ''}
            {hasFilters ? ' matching filters' : ' across all institutions'}
          </p>
          <div className="space-y-3">
            <AnimatePresence mode="popLayout">
              {jobs.map(job => <BoardJobCard key={job.id} job={job} />)}
            </AnimatePresence>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between pt-2">
              <button
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0 || isFetching}
                className="flex items-center gap-1.5 px-4 py-2 rounded-xl border border-white/10 text-sm text-white/50 hover:text-white hover:bg-white/5 transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
              >
                <ArrowLeft className="w-4 h-4" /> Previous
              </button>
              <span className="text-xs text-white/30">
                Page {page + 1} of {totalPages}
              </span>
              <button
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1 || isFetching}
                className="flex items-center gap-1.5 px-4 py-2 rounded-xl border border-white/10 text-sm text-white/50 hover:text-white hover:bg-white/5 transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
              >
                Next <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────

export default function AdminJobsPage() {
  const centerId  = useAuthStore(s => s.user?.centerId);
  const [subTab, setSubTab] = useState<SubTab>('my-postings');

  if (!centerId) {
    return (
      <div className="flex items-center justify-center min-h-[400px] p-8">
        <div className="text-center">
          <Briefcase className="w-10 h-10 text-white/15 mx-auto mb-3" />
          <p className="text-white/40 text-sm">Centre ID not found.</p>
          <p className="text-white/25 text-xs mt-1">Please sign out and sign in again.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col min-h-full">
      {/* Sub-tab bar */}
      <div className="flex items-center gap-1 px-6 lg:px-8 py-3 border-b border-white/5">
        {([
          { id: 'my-postings' as SubTab, label: 'My Postings',  icon: Briefcase },
          { id: 'job-board'   as SubTab, label: 'Job Board',    icon: Globe    },
        ]).map(tab => {
          const Icon = tab.icon;
          return (
            <button
              key={tab.id}
              onClick={() => setSubTab(tab.id)}
              className={cn(
                'flex items-center gap-1.5 px-4 py-1.5 rounded-lg text-sm font-medium transition-colors',
                subTab === tab.id
                  ? 'bg-white/8 text-white'
                  : 'text-white/40 hover:text-white/70 hover:bg-white/3'
              )}
            >
              <Icon className="w-3.5 h-3.5" />
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* Content */}
      <motion.div
        key={subTab}
        initial={{ opacity: 0, y: 6 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.15 }}
        className="flex-1 p-6 lg:p-8 max-w-5xl mx-auto w-full"
      >
        {subTab === 'my-postings' && <MyPostingsTab centerId={centerId} />}
        {subTab === 'job-board'   && <JobBoardTab />}
      </motion.div>
    </div>
  );
}
