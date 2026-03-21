// src/pages/admin/AdminStaffPage.tsx
import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Users, UserPlus, Search, Filter, RefreshCw, Loader2,
  Upload, ChevronDown, Sparkles, X, CheckCircle2,
  AlertCircle, Clock, UserX, RotateCcw, Bot,
  GraduationCap, Briefcase, MapPin, Star, Building2,
} from 'lucide-react';
import { toast } from 'sonner';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../lib/api';
import { cn } from '../../lib/utils';
import { useAuthStore } from '../../stores/authStore';
import { AnimatePresence as AP } from 'framer-motion';
import CreateStaffModal from './CreateStaffModal';
import AdminBulkImportTeachersPage from './AdminBulkImportTeachersPage';
import { useStaffGapAnalysis } from '../../hooks/useStaffAI';
import {
  STAFF_ROLE_TYPES,
  STAFF_STATUS_CONFIG,
  experienceLabel,
  getStaffRoleConfig,
  type StaffRoleTypeValue,
} from '../../constants/staffConstants';

// ─── Types ────────────────────────────────────────────────────────────────────

interface StaffMember {
  id: string;
  centerId: string;
  userId: string | null;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string | null;
  subjects: string | null;
  district: string | null;
  employeeId: string | null;
  status: string;
  joinedAt: string;
  roleType: string | null;
  qualification: string | null;
  yearsOfExperience: number | null;
  designation: string | null;
  bio: string | null;
}

type SubTab = 'directory' | 'import';

// ─── Sub-components ───────────────────────────────────────────────────────────

function StatCard({ label, value, sub, color }: {
  label: string; value: number | string; sub?: string; color?: string;
}) {
  return (
    <div className="bg-surface-100/50 border border-white/8 rounded-xl p-4 flex flex-col gap-1">
      <span className="text-xs font-medium text-white/40">{label}</span>
      <span className={cn('text-2xl font-bold', color ?? 'text-white')}>{value}</span>
      {sub && <span className="text-xs text-white/30">{sub}</span>}
    </div>
  );
}

function StatusBadge({ status }: { status: string }) {
  const cfg = STAFF_STATUS_CONFIG[status] ?? { label: status, color: 'text-white/40', bg: 'bg-white/8 border-white/15' };
  return (
    <span className={cn('inline-flex items-center gap-1 px-2 py-0.5 rounded-lg text-xs font-medium border', cfg.bg, cfg.color)}>
      {cfg.label}
    </span>
  );
}

function RoleBadge({ roleType }: { roleType: string | null }) {
  if (!roleType) return null;
  const cfg = getStaffRoleConfig(roleType);
  return (
    <span className={cn('inline-flex px-2 py-0.5 rounded-lg text-xs font-medium border', cfg?.bg ?? 'bg-white/8 border-white/15', cfg?.color ?? 'text-white/40')}>
      {cfg?.label ?? roleType}
    </span>
  );
}

function Avatar({ name, roleType }: { name: string; roleType: string | null }) {
  const cfg = getStaffRoleConfig(roleType);
  const initials = name.split(' ').map(w => w[0]).slice(0, 2).join('');
  return (
    <div className={cn(
      'w-10 h-10 rounded-xl flex items-center justify-center text-sm font-bold flex-shrink-0 border',
      cfg?.bg ?? 'bg-white/8 border-white/10'
    )}>
      <span className={cfg?.color ?? 'text-white/60'}>{initials}</span>
    </div>
  );
}

// ─── AI Insights Panel ────────────────────────────────────────────────────────

function AIInsightsPanel({ staff }: { staff: StaffMember[] }) {
  const { analyzeGaps, isAnalyzing, result } = useStaffGapAnalysis();
  const [ran, setRan] = useState(false);

  async function run() {
    setRan(true);
    await analyzeGaps(staff.map(s => ({
      status:   s.status,
      subjects: s.subjects,
      roleType: s.roleType,
    })));
  }

  const topUncovered = result?.understaffedSubjects.slice(0, 4) ?? [];

  return (
    <div className="bg-surface-100/50 border border-brand-500/15 rounded-xl p-4 space-y-3">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Bot className="w-4 h-4 text-brand-400" />
          <span className="text-sm font-semibold text-white">AI Staffing Insights</span>
        </div>
        <button
          onClick={run}
          disabled={isAnalyzing}
          className="flex items-center gap-1 text-xs text-brand-400 hover:text-brand-300 disabled:opacity-50 transition-colors"
        >
          {isAnalyzing
            ? <><Loader2 className="w-3 h-3 animate-spin" /> Analysing...</>
            : ran
            ? <><RefreshCw className="w-3 h-3" /> Refresh</>
            : <><Sparkles className="w-3 h-3" /> Analyse Gaps</>
          }
        </button>
      </div>

      {!ran && !isAnalyzing && (
        <p className="text-xs text-white/30">
          Click "Analyse Gaps" to get AI-powered hiring recommendations based on your current staff coverage.
        </p>
      )}

      {isAnalyzing && (
        <div className="flex items-center gap-2 text-xs text-white/40">
          <Loader2 className="w-3 h-3 animate-spin" />
          Analysing {staff.filter(s => s.status === 'ACTIVE').length} active staff members…
        </div>
      )}

      {ran && result && !isAnalyzing && (
        <>
          {topUncovered.length > 0 && (
            <div>
              <p className="text-xs font-medium text-white/50 mb-1.5">Subjects with no coverage</p>
              <div className="flex flex-wrap gap-1.5">
                {topUncovered.map(s => (
                  <span key={s} className="text-xs px-2 py-0.5 bg-red-500/10 border border-red-500/20 rounded-lg text-red-300">{s}</span>
                ))}
              </div>
            </div>
          )}

          {result.recommendation && (
            <div>
              <p className="text-xs font-medium text-white/50 mb-1.5">Hiring Recommendations</p>
              <div className="text-xs text-white/70 leading-relaxed whitespace-pre-line bg-white/3 rounded-lg p-3 border border-white/5">
                {result.recommendation}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}

// ─── Staff Card ───────────────────────────────────────────────────────────────

function StaffCard({ member, centerId, onRefresh }: {
  member: StaffMember; centerId: string; onRefresh: () => void;
}) {
  const [showBio, setShowBio] = useState(false);
  const [deactivating, setDeactivating] = useState(false);
  const [reactivating, setReactivating] = useState(false);

  const fullName    = `${member.firstName} ${member.lastName}`;
  const subjectList = member.subjects?.split(',').map(s => s.trim()) ?? [];

  async function handleDeactivate() {
    if (!confirm(`Deactivate ${fullName}? They will lose access.`)) return;
    setDeactivating(true);
    try {
      await api.delete(`/api/v1/centers/${centerId}/staff/${member.id}`);
      toast.success(`${fullName} deactivated`);
      onRefresh();
    } catch {
      toast.error('Deactivation failed');
    } finally {
      setDeactivating(false);
    }
  }

  async function handleReactivate() {
    setReactivating(true);
    try {
      await api.post(`/api/v1/centers/${centerId}/staff/${member.id}/reactivate`);
      toast.success(`${fullName} reactivated`);
      onRefresh();
    } catch {
      toast.error('Reactivation failed');
    } finally {
      setReactivating(false);
    }
  }

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-surface-100/50 border border-white/8 rounded-xl p-4 hover:border-white/15 transition-colors"
    >
      <div className="flex items-start gap-3">
        <Avatar name={fullName} roleType={member.roleType} />

        <div className="flex-1 min-w-0">
          <div className="flex items-start justify-between gap-2">
            <div>
              <p className="text-sm font-semibold text-white">{fullName}</p>
              <p className="text-xs text-white/40 truncate">{member.email}</p>
            </div>
            <div className="flex items-center gap-1.5 flex-shrink-0">
              <StatusBadge status={member.status} />
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-1.5 mt-2">
            {member.roleType && <RoleBadge roleType={member.roleType} />}
            {member.designation && (
              <span className="text-xs text-white/40">{member.designation}</span>
            )}
          </div>

          <div className="flex flex-wrap items-center gap-3 mt-2 text-xs text-white/35">
            {member.yearsOfExperience != null && (
              <span className="flex items-center gap-1">
                <Star className="w-3 h-3" />
                {experienceLabel(member.yearsOfExperience)}
              </span>
            )}
            {member.qualification && (
              <span className="flex items-center gap-1">
                <GraduationCap className="w-3 h-3" />
                {member.qualification.split(',')[0].trim()}
              </span>
            )}
            {member.district && (
              <span className="flex items-center gap-1">
                <MapPin className="w-3 h-3" />
                {member.district}
              </span>
            )}
            {member.employeeId && (
              <span className="text-white/25"># {member.employeeId}</span>
            )}
          </div>

          {subjectList.length > 0 && (
            <div className="flex flex-wrap gap-1 mt-2">
              {subjectList.map(s => (
                <span key={s} className="text-xs px-1.5 py-0.5 bg-brand-500/10 border border-brand-500/15 rounded text-brand-300/80">{s}</span>
              ))}
            </div>
          )}

          {member.bio && (
            <div className="mt-2">
              <button
                onClick={() => setShowBio(v => !v)}
                className="flex items-center gap-1 text-xs text-white/30 hover:text-white/60 transition-colors"
              >
                <ChevronDown className={cn('w-3 h-3 transition-transform', showBio && 'rotate-180')} />
                {showBio ? 'Hide bio' : 'View bio'}
              </button>
              <AnimatePresence>
                {showBio && (
                  <motion.p
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: 'auto' }}
                    exit={{ opacity: 0, height: 0 }}
                    className="text-xs text-white/50 leading-relaxed mt-1 italic"
                  >
                    {member.bio}
                  </motion.p>
                )}
              </AnimatePresence>
            </div>
          )}
        </div>
      </div>

      {/* Actions */}
      <div className="flex items-center gap-2 mt-3 pt-3 border-t border-white/5">
        {member.status === 'ACTIVE' && (
          <button
            onClick={handleDeactivate}
            disabled={deactivating}
            className="flex items-center gap-1 text-xs text-red-400/70 hover:text-red-400 transition-colors disabled:opacity-50"
          >
            {deactivating ? <Loader2 className="w-3 h-3 animate-spin" /> : <UserX className="w-3 h-3" />}
            Deactivate
          </button>
        )}
        {member.status === 'INACTIVE' && (
          <button
            onClick={handleReactivate}
            disabled={reactivating}
            className="flex items-center gap-1 text-xs text-emerald-400/70 hover:text-emerald-400 transition-colors disabled:opacity-50"
          >
            {reactivating ? <Loader2 className="w-3 h-3 animate-spin" /> : <RotateCcw className="w-3 h-3" />}
            Reactivate
          </button>
        )}
        {member.status === 'INVITATION_SENT' && (
          <span className="flex items-center gap-1 text-xs text-amber-400/60">
            <Clock className="w-3 h-3" /> Awaiting acceptance
          </span>
        )}
        {member.status === 'PENDING_APPROVAL' && (
          <span className="flex items-center gap-1 text-xs text-orange-400/60">
            <AlertCircle className="w-3 h-3" /> Pending approval
          </span>
        )}
      </div>
    </motion.div>
  );
}

// ─── Center Picker (for INSTITUTION_ADMIN with no centerId in JWT) ────────────

interface CenterOption { id: string; name: string; code?: string; }

function CenterPicker({ onSelect }: { onSelect: (id: string) => void }) {
  const { data: centers = [], isLoading } = useQuery<CenterOption[]>({
    queryKey: ['centers-picker-staff'],
    queryFn: async () => {
      const r = await api.get('/api/v1/centers?size=100');
      const d = r.data;
      return Array.isArray(d) ? d : (d.content ?? []);
    },
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <Loader2 className="w-6 h-6 text-brand-400 animate-spin" />
      </div>
    );
  }

  if (centers.length === 0) {
    return (
      <div className="flex items-center justify-center min-h-[400px] p-8">
        <div className="text-center">
          <Building2 className="w-10 h-10 text-white/15 mx-auto mb-3" />
          <p className="text-white/40 text-sm">No centres found.</p>
          <p className="text-white/25 text-xs mt-1">Create a centre first before managing staff.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 lg:p-8 max-w-lg mx-auto">
      <div className="mb-6">
        <h2 className="text-xl font-bold text-white flex items-center gap-2">
          <Users className="w-5 h-5 text-brand-400" /> Staff
        </h2>
        <p className="text-sm text-white/40 mt-1">Select a centre to manage its staff.</p>
      </div>
      <div className="space-y-2">
        {centers.map(c => (
          <button
            key={c.id}
            onClick={() => onSelect(c.id)}
            className="w-full flex items-center justify-between gap-3 p-4 rounded-xl bg-white/3 border border-white/8 hover:bg-white/6 hover:border-white/15 transition-colors text-left"
          >
            <div className="flex items-center gap-3">
              <Building2 className="w-4 h-4 text-brand-400 flex-shrink-0" />
              <div>
                <p className="text-sm font-medium text-white">{c.name}</p>
                {c.code && <p className="text-xs text-white/35">{c.code}</p>}
              </div>
            </div>
            <Users className="w-4 h-4 text-white/25 flex-shrink-0" />
          </button>
        ))}
      </div>
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────

export default function AdminStaffPage() {
  const centerId   = useAuthStore(s => s.user?.centerId);
  const queryClient = useQueryClient();

  const [selectedCenterId, setSelectedCenterId] = useState('');
  const effectiveCenterId = centerId || selectedCenterId;

  const [subTab,      setSubTab]      = useState<SubTab>('directory');
  const [showCreate,  setShowCreate]  = useState(false);
  const [search,      setSearch]      = useState('');
  const [filterRole,  setFilterRole]  = useState<StaffRoleTypeValue | ''>('');
  const [filterStatus,setFilterStatus]= useState('');
  const [showFilters, setShowFilters] = useState(false);

  const { data: staff = [], isLoading, refetch } = useQuery<StaffMember[]>({
    queryKey: ['staff', effectiveCenterId],
    queryFn: async () => {
      if (!effectiveCenterId) return [];
      const res = await api.get(`/api/v1/centers/${effectiveCenterId}/staff`);
      const d = res.data;
      return Array.isArray(d) ? d : (d.content ?? []);
    },
    enabled: !!effectiveCenterId,
  });

  if (!effectiveCenterId) {
    return <CenterPicker onSelect={setSelectedCenterId} />;
  }

  // ── Stats ────────────────────────────────────────────────────────────────────
  const total           = staff.length;
  const activeCount     = staff.filter(s => s.status === 'ACTIVE').length;
  const invitedCount    = staff.filter(s => s.status === 'INVITATION_SENT').length;
  const pendingCount    = staff.filter(s => s.status === 'PENDING_APPROVAL').length;

  const roleCounts = STAFF_ROLE_TYPES.reduce((acc, r) => {
    acc[r.value] = staff.filter(s => s.roleType === r.value && s.status === 'ACTIVE').length;
    return acc;
  }, {} as Record<string, number>);

  // ── Filter + Search ──────────────────────────────────────────────────────────
  const filtered = staff.filter(s => {
    const q = search.toLowerCase();
    const matchesSearch = !q || [
      s.firstName, s.lastName, s.email,
      s.designation, s.subjects, s.roleType,
    ].some(v => v?.toLowerCase().includes(q));

    const matchesRole   = !filterRole   || s.roleType === filterRole;
    const matchesStatus = !filterStatus || s.status   === filterStatus;
    return matchesSearch && matchesRole && matchesStatus;
  });

  const hasActiveFilters = !!filterRole || !!filterStatus;

  function clearFilters() {
    setFilterRole('');
    setFilterStatus('');
  }

  // ─── Render ──────────────────────────────────────────────────────────────────

  return (
    <div className="flex flex-col min-h-full">
      {/* Sub-tab bar */}
      <div className="flex items-center gap-1 px-6 lg:px-8 py-3 border-b border-white/5">
        {(['directory', 'import'] as const).map(tab => (
          <button
            key={tab}
            onClick={() => setSubTab(tab)}
            className={cn(
              'flex items-center gap-1.5 px-4 py-1.5 rounded-lg text-sm font-medium transition-colors',
              subTab === tab
                ? 'bg-white/8 text-white'
                : 'text-white/40 hover:text-white/70 hover:bg-white/3'
            )}
          >
            {tab === 'directory' ? <Users className="w-3.5 h-3.5" /> : <Upload className="w-3.5 h-3.5" />}
            {tab === 'directory' ? 'Staff Directory' : 'Bulk Import'}
          </button>
        ))}
      </div>

      {subTab === 'import' && (
        <div className="flex-1">
          <AdminBulkImportTeachersPage />
        </div>
      )}

      {subTab === 'directory' && (
        <div className="flex-1 p-6 lg:p-8 max-w-7xl mx-auto w-full space-y-6">

          {/* Header */}
          <div className="flex items-start justify-between gap-4">
            <div>
              <h1 className="text-xl font-bold text-white flex items-center gap-2">
                <Users className="w-5 h-5 text-brand-400" /> Staff Directory
              </h1>
              <p className="text-sm text-white/40 mt-0.5">
                Manage all staff members, roles, qualifications, and invitations
              </p>
            </div>
            <button
              onClick={() => setShowCreate(true)}
              className="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium bg-brand-600 hover:bg-brand-500 text-white transition-colors flex-shrink-0"
            >
              <UserPlus className="w-4 h-4" /> + Create Staff
            </button>
          </div>

          {/* Stats */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
            <StatCard label="Total Staff"    value={total}        sub="all statuses" />
            <StatCard label="Active"         value={activeCount}  color="text-emerald-400" />
            <StatCard label="Invitations Sent" value={invitedCount} color="text-amber-400" />
            <StatCard label="Pending Approval" value={pendingCount} color="text-orange-400" />
          </div>

          {/* Role breakdown */}
          {activeCount > 0 && (
            <div className="grid grid-cols-4 lg:grid-cols-8 gap-2">
              {STAFF_ROLE_TYPES.map(role => (
                <button
                  key={role.value}
                  onClick={() => setFilterRole(filterRole === role.value ? '' : role.value)}
                  className={cn(
                    'flex flex-col items-center gap-1 p-2 rounded-xl border text-center transition-all text-xs',
                    filterRole === role.value
                      ? `${role.bg} border-opacity-60`
                      : 'border-white/8 hover:border-white/15 hover:bg-white/3'
                  )}
                >
                  <span className={cn('text-base font-bold', role.color)}>
                    {roleCounts[role.value] ?? 0}
                  </span>
                  <span className="text-white/35 leading-tight">{role.label.split(' ')[0]}</span>
                </button>
              ))}
            </div>
          )}

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="lg:col-span-2 space-y-4">
              {/* Search + Filters */}
              <div className="flex items-center gap-2">
                <div className="relative flex-1">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
                  <input
                    value={search}
                    onChange={e => setSearch(e.target.value)}
                    placeholder="Search by name, email, subject, role…"
                    className="w-full bg-white/5 border border-white/10 rounded-xl pl-9 pr-4 py-2.5 text-sm text-white placeholder:text-white/25 focus:outline-none focus:border-brand-500/50 transition-colors"
                  />
                  {search && (
                    <button onClick={() => setSearch('')} className="absolute right-3 top-1/2 -translate-y-1/2 text-white/30 hover:text-white/60">
                      <X className="w-3.5 h-3.5" />
                    </button>
                  )}
                </div>
                <button
                  onClick={() => setShowFilters(v => !v)}
                  className={cn(
                    'flex items-center gap-1.5 px-3.5 py-2.5 rounded-xl border text-sm transition-colors',
                    hasActiveFilters
                      ? 'border-brand-500/40 bg-brand-500/10 text-brand-400'
                      : 'border-white/10 text-white/40 hover:text-white/70 hover:bg-white/3'
                  )}
                >
                  <Filter className="w-3.5 h-3.5" />
                  {hasActiveFilters ? 'Filtered' : 'Filter'}
                </button>
                <button
                  onClick={() => refetch()}
                  className="p-2.5 rounded-xl border border-white/10 text-white/40 hover:text-white/70 hover:bg-white/3 transition-colors"
                >
                  <RefreshCw className="w-4 h-4" />
                </button>
              </div>

              <AnimatePresence>
                {showFilters && (
                  <motion.div
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: 'auto' }}
                    exit={{ opacity: 0, height: 0 }}
                    className="flex flex-wrap items-center gap-2 p-3 bg-white/3 border border-white/8 rounded-xl"
                  >
                    <div className="flex items-center gap-1.5">
                      <span className="text-xs text-white/40">Role:</span>
                      <select
                        value={filterRole}
                        onChange={e => setFilterRole(e.target.value as StaffRoleTypeValue | '')}
                        className="bg-white/5 border border-white/10 rounded-lg px-2 py-1 text-xs text-white focus:outline-none"
                      >
                        <option value="">All</option>
                        {STAFF_ROLE_TYPES.map(r => (
                          <option key={r.value} value={r.value} className="bg-surface-100">{r.label}</option>
                        ))}
                      </select>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <span className="text-xs text-white/40">Status:</span>
                      <select
                        value={filterStatus}
                        onChange={e => setFilterStatus(e.target.value)}
                        className="bg-white/5 border border-white/10 rounded-lg px-2 py-1 text-xs text-white focus:outline-none"
                      >
                        <option value="">All</option>
                        {Object.entries(STAFF_STATUS_CONFIG).map(([k, v]) => (
                          <option key={k} value={k} className="bg-surface-100">{v.label}</option>
                        ))}
                      </select>
                    </div>
                    {hasActiveFilters && (
                      <button
                        onClick={clearFilters}
                        className="flex items-center gap-1 text-xs text-red-400/70 hover:text-red-400 transition-colors"
                      >
                        <X className="w-3 h-3" /> Clear
                      </button>
                    )}
                  </motion.div>
                )}
              </AnimatePresence>

              {/* Staff list */}
              {isLoading ? (
                <div className="flex items-center justify-center py-20">
                  <Loader2 className="w-6 h-6 animate-spin text-white/30" />
                </div>
              ) : filtered.length === 0 ? (
                <div className="text-center py-16 border border-dashed border-white/10 rounded-xl">
                  <Users className="w-10 h-10 text-white/15 mx-auto mb-3" />
                  <p className="text-white/40 text-sm">
                    {staff.length === 0
                      ? 'No staff members yet — click "+ Create Staff" to send the first invitation.'
                      : 'No staff match your filters.'}
                  </p>
                  {hasActiveFilters && (
                    <button onClick={clearFilters} className="mt-2 text-xs text-brand-400 hover:text-brand-300">
                      Clear filters
                    </button>
                  )}
                </div>
              ) : (
                <div className="space-y-3">
                  <p className="text-xs text-white/30">
                    {filtered.length} of {total} staff member{total !== 1 ? 's' : ''}
                    {(search || hasActiveFilters) ? ' matching filters' : ''}
                  </p>
                  {filtered.map(member => (
                    <StaffCard
                      key={member.id}
                      member={member}
                      centerId={effectiveCenterId}
                      onRefresh={() => queryClient.invalidateQueries({ queryKey: ['staff', effectiveCenterId] })}
                    />
                  ))}
                </div>
              )}
            </div>

            {/* AI Insights sidebar */}
            <div className="space-y-4">
              {!isLoading && staff.length > 0 && (
                <AIInsightsPanel staff={staff} />
              )}

              {/* Quick role stats */}
              {activeCount > 0 && (
                <div className="bg-surface-100/50 border border-white/8 rounded-xl p-4">
                  <p className="text-xs font-semibold text-white/50 mb-3 flex items-center gap-1.5">
                    <Briefcase className="w-3.5 h-3.5" /> Role Breakdown
                  </p>
                  <div className="space-y-2">
                    {STAFF_ROLE_TYPES.filter(r => (roleCounts[r.value] ?? 0) > 0).map(role => (
                      <div key={role.value} className="flex items-center justify-between">
                        <span className={cn('text-xs', role.color)}>{role.label}</span>
                        <span className="text-xs font-semibold text-white/60">{roleCounts[role.value]}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Create Staff Modal */}
      <AnimatePresence>
        {showCreate && (
          <CreateStaffModal
            centerId={effectiveCenterId}
            onClose={() => setShowCreate(false)}
            onCreated={() => {
              setShowCreate(false);
              queryClient.invalidateQueries({ queryKey: ['staff', effectiveCenterId] });
            }}
          />
        )}
      </AnimatePresence>
    </div>
  );
}
