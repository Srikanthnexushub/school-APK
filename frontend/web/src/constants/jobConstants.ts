// src/constants/jobConstants.ts
// Single source of truth for all job-posting constants.
// No inline hardcoded values in components — always import from here.

// ─── Job Types ────────────────────────────────────────────────────────────────

export const JOB_TYPES = [
  { value: 'FULL_TIME', label: 'Full Time' },
  { value: 'PART_TIME', label: 'Part Time' },
  { value: 'CONTRACT',  label: 'Contract'  },
] as const;

export type JobTypeValue = typeof JOB_TYPES[number]['value'];

// ─── Job Statuses ─────────────────────────────────────────────────────────────

export const JOB_STATUSES = [
  {
    value: 'DRAFT',
    label: 'Draft',
    color: 'text-white/40',
    bg:    'bg-white/8 border-white/15',
  },
  {
    value: 'OPEN',
    label: 'Open',
    color: 'text-emerald-400',
    bg:    'bg-emerald-500/15 border-emerald-500/30',
  },
  {
    value: 'CLOSED',
    label: 'Closed',
    color: 'text-red-400',
    bg:    'bg-red-500/15 border-red-500/30',
  },
  {
    value: 'FILLED',
    label: 'Filled',
    color: 'text-violet-400',
    bg:    'bg-violet-500/15 border-violet-500/30',
  },
] as const;

export type JobStatusValue = typeof JOB_STATUSES[number]['value'];

export function getJobStatusConfig(value: string | null | undefined) {
  return JOB_STATUSES.find(s => s.value === value) ?? {
    value:  value ?? '',
    label:  value ?? '—',
    color:  'text-white/40',
    bg:     'bg-white/8 border-white/15',
  };
}

export function getJobTypeLabel(value: string | null | undefined): string {
  return JOB_TYPES.find(t => t.value === value)?.label ?? value ?? '—';
}

// ─── Roles that require subject selection ─────────────────────────────────────
// Mirrors the backend showSubjects logic

export const SUBJECT_ROLES: string[] = ['TEACHER', 'HOD', 'COORDINATOR'];

// ─── Experience display helper ────────────────────────────────────────────────

export function experienceLabel(years: number | null | undefined): string {
  if (years == null) return '';
  if (years === 0) return 'Fresher welcome';
  if (years === 1) return 'Min. 1 year';
  return `Min. ${years} years`;
}

// ─── Salary display helper ────────────────────────────────────────────────────

export function salaryLabel(
  min: number | null | undefined,
  max: number | null | undefined,
): string {
  const fmt = (n: number) =>
    n >= 100_000
      ? `₹${(n / 100_000).toFixed(n % 100_000 === 0 ? 0 : 1)}L`
      : `₹${n.toLocaleString('en-IN')}`;

  if (min != null && max != null && min !== max) return `${fmt(min)} – ${fmt(max)} / yr`;
  if (min != null) return `From ${fmt(min)} / yr`;
  if (max != null) return `Up to ${fmt(max)} / yr`;
  return '';
}

// ─── Deadline urgency helper ──────────────────────────────────────────────────

export function deadlineDaysLeft(deadline: string | null | undefined): number | null {
  if (!deadline) return null;
  const diff = new Date(deadline).getTime() - Date.now();
  return Math.ceil(diff / (1000 * 60 * 60 * 24));
}

// ─── Relative time helper ─────────────────────────────────────────────────────

export function relativeTime(isoDate: string): string {
  const diff = Date.now() - new Date(isoDate).getTime();
  const mins  = Math.floor(diff / 60_000);
  const hours = Math.floor(diff / 3_600_000);
  const days  = Math.floor(diff / 86_400_000);
  if (mins  < 1)  return 'just now';
  if (mins  < 60) return `${mins}m ago`;
  if (hours < 24) return `${hours}h ago`;
  if (days  < 30) return `${days}d ago`;
  return new Date(isoDate).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}
