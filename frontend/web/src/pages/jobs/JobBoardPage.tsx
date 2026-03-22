// src/pages/jobs/JobBoardPage.tsx
// Read-only public job board for STUDENT / PARENT / TEACHER roles.
// Calls GET /api/v1/jobs (any authenticated user — no role restriction).
import { useState, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Briefcase, Search, Loader2, MapPin, Clock, DollarSign, Calendar,
  ChevronLeft, ChevronRight, Globe, Building2, X, Filter, Users,
} from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import api from '../../lib/api';
import { cn } from '../../lib/utils';
import { STAFF_ROLE_TYPES, getStaffRoleConfig } from '../../constants/staffConstants';
import {
  JOB_TYPES,
  getJobTypeLabel,
  experienceLabel,
  salaryLabel,
  deadlineDaysLeft,
  relativeTime,
} from '../../constants/jobConstants';
import type { JobPosting } from '../admin/PostJobModal';

const PAGE_SIZE = 20;

// ─── Sub-components ───────────────────────────────────────────────────────────

function RoleBadge({ roleType }: { roleType: string }) {
  const cfg = getStaffRoleConfig(roleType);
  return (
    <span className={cn(
      'inline-flex px-2 py-0.5 rounded-lg text-xs font-medium border',
      cfg?.bg ?? 'bg-white/8 border-white/15',
      cfg?.color ?? 'text-white/40',
    )}>
      {cfg?.label ?? roleType}
    </span>
  );
}

function DeadlineBadge({ deadline }: { deadline: string | null }) {
  if (!deadline) return null;
  const days = deadlineDaysLeft(deadline);
  if (days == null) return null;
  const isPast   = days < 0;
  const isUrgent = days <= 7;
  return (
    <span className={cn(
      'inline-flex items-center gap-1 text-xs',
      isPast   ? 'text-red-400'   :
      isUrgent ? 'text-amber-400' :
                 'text-white/35',
    )}>
      <Calendar className="w-3 h-3" />
      {isPast ? 'Expired' : days === 0 ? 'Due today' : `${days}d left`}
    </span>
  );
}

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

function JobCard({ job }: { job: JobPosting }) {
  const subjectList = job.subjects?.split(',').map(s => s.trim()).filter(Boolean) ?? [];
  const salary = salaryLabel(job.salaryMin, job.salaryMax);
  const exp    = experienceLabel(job.experienceMinYears);

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-white/3 border border-white/8 rounded-xl p-4 hover:bg-white/5 hover:border-white/12 transition-colors"
    >
      {/* Institution + city */}
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
            <span
              key={s}
              className="text-xs px-1.5 py-0.5 bg-brand-500/10 border border-brand-500/15 rounded text-brand-300/80"
            >
              {s}
            </span>
          ))}
        </div>
      )}

      {/* Meta row */}
      <div className="flex flex-wrap items-center gap-3 text-xs text-white/35 mt-2">
        {exp    && <span className="flex items-center gap-1"><Clock     className="w-3 h-3" /> {exp}</span>}
        {salary && <span className="flex items-center gap-1"><DollarSign className="w-3 h-3" /> {salary}</span>}
        {job.deadline && <DeadlineBadge deadline={job.deadline} />}
        <span className="flex items-center gap-1">
          <Briefcase className="w-3 h-3" /> Posted {relativeTime(job.postedAt)}
        </span>
      </div>
    </motion.div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function JobBoardPage() {
  const [search,           setSearch]           = useState('');
  const [filterRole,       setFilterRole]       = useState('');
  const [filterType,       setFilterType]       = useState('');
  const [filterCity,       setFilterCity]       = useState('');
  const [page,             setPage]             = useState(0);
  const [debouncedSearch,  setDebouncedSearch]  = useState('');

  const debounceRef = useRef<ReturnType<typeof setTimeout>>();

  function handleSearchChange(v: string) {
    setSearch(v);
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setDebouncedSearch(v);
      setPage(0);
    }, 400);
  }

  const { data, isLoading, isFetching } = useQuery<{
    jobs: JobPosting[];
    total: number;
    totalPages: number;
  }>({
    queryKey: ['jobs-board-public', debouncedSearch, filterRole, filterType, filterCity, page],
    queryFn: async () => {
      const params = new URLSearchParams();
      if (debouncedSearch) params.set('keyword', debouncedSearch);
      if (filterRole)      params.set('roleType', filterRole);
      if (filterType)      params.set('jobType',  filterType);
      if (filterCity)      params.set('city',     filterCity);
      params.set('page', String(page));
      params.set('size', String(PAGE_SIZE));
      const res = await api.get(`/api/v1/jobs?${params.toString()}`);
      const d = res.data;
      if (Array.isArray(d)) return { jobs: d, total: d.length, totalPages: 1 };
      return {
        jobs:       d.content      ?? [],
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
    <div className="p-6 lg:p-8 max-w-4xl mx-auto space-y-5">

      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-white flex items-center gap-2">
          <Globe className="w-6 h-6 text-brand-400" /> Job Board
        </h1>
        <p className="text-sm text-white/40 mt-1">
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
          <button
            onClick={() => handleSearchChange('')}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-white/30 hover:text-white/60"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        )}
      </div>

      {/* Filter row */}
      <div className="flex flex-wrap items-center gap-2">
        <Filter className="w-3.5 h-3.5 text-white/30 flex-shrink-0" />

        {/* Role filter */}
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

        {/* Job type filter */}
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

        {/* City filter */}
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
            <button
              onClick={clearFilters}
              className="mt-3 text-xs text-brand-400 hover:text-brand-300 transition-colors"
            >
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
              {jobs.map(job => <JobCard key={job.id} job={job} />)}
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
                <ChevronLeft className="w-4 h-4" /> Previous
              </button>
              <span className="text-xs text-white/30">Page {page + 1} of {totalPages}</span>
              <button
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1 || isFetching}
                className="flex items-center gap-1.5 px-4 py-2 rounded-xl border border-white/10 text-sm text-white/50 hover:text-white hover:bg-white/5 transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
              >
                Next <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
