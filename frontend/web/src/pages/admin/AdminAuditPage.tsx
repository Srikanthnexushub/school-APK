// src/pages/admin/AdminAuditPage.tsx
import { useQuery } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import {
  Users, Building2, BookOpen, UserCheck, ShieldCheck,
  TrendingUp, BarChart3, Globe, Wifi, RefreshCw, Download,
  AlertTriangle, CheckCircle2, Activity, Lock,
} from 'lucide-react';
import api from '../../lib/api';
import { cn } from '../../lib/utils';
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell,
  PieChart, Pie, Legend,
} from 'recharts';

interface AuthAuditStats {
  totalUsers: number;
  activeUsers: number;
  deletedUsers: number;
  usersByRole: Record<string, number>;
  registeredLast7Days: number;
  registeredLast30Days: number;
  mfaEnabledUsers: number;
  socialAuthUsers: number;
  verifiedEmailUsers: number;
}

interface CenterAuditStats {
  totalCenters: number;
  activeCenters: number;
  totalBatches: number;
  activeBatches: number;
  upcomingBatches: number;
  completedBatches: number;
  totalEnrolled: number;
  totalTeachers: number;
  activeTeachers: number;
  avgBatchFillRate: number;
}

const ROLE_COLORS: Record<string, string> = {
  STUDENT:           '#6366f1',
  PARENT:            '#10b981',
  TEACHER:           '#f59e0b',
  CENTER_ADMIN:      '#ef4444',
  INSTITUTION_ADMIN: '#a855f7',
  SUPER_ADMIN:       '#ec4899',
};

function MetricCard({ label, value, sub, icon: Icon, color, delay }: {
  label: string; value: string | number; sub?: string;
  icon: React.ElementType; color: string; delay: number;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay }}
      className="card"
    >
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs font-medium text-white/40 uppercase tracking-wider mb-1">{label}</p>
          <p className={cn('text-2xl font-bold', color)}>{value}</p>
          {sub && <p className="text-xs text-white/30 mt-0.5">{sub}</p>}
        </div>
        <div className={cn('w-10 h-10 rounded-xl flex items-center justify-center bg-white/5')}>
          <Icon className={cn('w-5 h-5', color)} />
        </div>
      </div>
    </motion.div>
  );
}

function NfrRow({ label, value, status }: { label: string; value: string; status: 'pass' | 'warn' | 'info' }) {
  return (
    <div className="flex items-center justify-between py-2.5 border-b border-white/5 last:border-0">
      <span className="text-sm text-white/70">{label}</span>
      <div className="flex items-center gap-2">
        <span className="text-sm font-medium text-white">{value}</span>
        {status === 'pass' && <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />}
        {status === 'warn' && <AlertTriangle className="w-3.5 h-3.5 text-amber-400" />}
        {status === 'info' && <Activity className="w-3.5 h-3.5 text-sky-400" />}
      </div>
    </div>
  );
}

export default function AdminAuditPage() {
  const authStats = useQuery<AuthAuditStats>({
    queryKey: ['audit-auth-stats'],
    queryFn: () => api.get('/api/v1/auth/admin/audit/stats').then(r => r.data),
    staleTime: 60_000,
  });

  const centerStats = useQuery<CenterAuditStats>({
    queryKey: ['audit-center-stats'],
    queryFn: () => api.get('/api/v1/centers/admin/audit/stats').then(r => r.data),
    staleTime: 60_000,
  });

  const isLoading = authStats.isLoading || centerStats.isLoading;
  const hasError  = authStats.isError   || centerStats.isError;

  const auth   = authStats.data;
  const center = centerStats.data;

  // Role distribution for pie chart
  const roleData = auth ? Object.entries(auth.usersByRole).map(([role, count]) => ({
    name: role.replace('_', ' '),
    value: count,
    fill: ROLE_COLORS[role] ?? '#6366f1',
  })) : [];

  // Batch status for bar chart
  const batchData = center ? [
    { name: 'Active',    value: center.activeBatches,    fill: '#10b981' },
    { name: 'Upcoming',  value: center.upcomingBatches,  fill: '#f59e0b' },
    { name: 'Completed', value: center.completedBatches, fill: '#6366f1' },
  ] : [];

  const fillPct     = center ? Math.round(center.avgBatchFillRate * 100) : 0;
  const mfaPct      = auth   ? Math.round((auth.mfaEnabledUsers / Math.max(auth.totalUsers, 1)) * 100) : 0;
  const verifiedPct = auth   ? Math.round((auth.verifiedEmailUsers / Math.max(auth.totalUsers, 1)) * 100) : 0;

  function handleExport() {
    if (!auth || !center) return;
    const rows = [
      ['NFR Audit Report', new Date().toISOString()],
      [],
      ['=== USER METRICS ==='],
      ['Total Users', auth.totalUsers],
      ['Active Users', auth.activeUsers],
      ['Deleted Users', auth.deletedUsers],
      ['Registered (Last 7 days)', auth.registeredLast7Days],
      ['Registered (Last 30 days)', auth.registeredLast30Days],
      ['MFA Enabled', `${auth.mfaEnabledUsers} (${mfaPct}%)`],
      ['Social Auth Users', auth.socialAuthUsers],
      ['Email Verified', `${auth.verifiedEmailUsers} (${verifiedPct}%)`],
      [],
      ['=== USER ROLE DISTRIBUTION ==='],
      ...Object.entries(auth.usersByRole).map(([role, count]) => [role, count]),
      [],
      ['=== CENTER & BATCH METRICS ==='],
      ['Total Centers', center.totalCenters],
      ['Active Centers', center.activeCenters],
      ['Total Batches', center.totalBatches],
      ['Active Batches', center.activeBatches],
      ['Upcoming Batches', center.upcomingBatches],
      ['Completed Batches', center.completedBatches],
      ['Total Enrolled Students', center.totalEnrolled],
      ['Total Teachers', center.totalTeachers],
      ['Active Teachers', center.activeTeachers],
      ['Avg Batch Fill Rate', `${fillPct}%`],
    ];
    const csv = rows.map(r => r.join(',')).join('\n');
    const a = document.createElement('a');
    a.href = 'data:text/csv;charset=utf-8,' + encodeURIComponent(csv);
    a.download = `nfr-audit-${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
  }

  return (
    <div className="p-4 lg:p-8 space-y-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <Lock className="w-6 h-6 text-brand-400" />
            NFR Audit Report
          </h1>
          <p className="text-white/40 text-sm mt-0.5">
            Non-Functional Requirements — platform health, security & growth metrics
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => { authStats.refetch(); centerStats.refetch(); }}
            disabled={isLoading}
            className="flex items-center gap-1.5 px-3 py-2 rounded-xl border border-white/10 text-white/60 hover:text-white hover:border-white/20 transition-colors text-sm disabled:opacity-50"
          >
            <RefreshCw className={cn('w-3.5 h-3.5', isLoading && 'animate-spin')} />
            Refresh
          </button>
          <button
            onClick={handleExport}
            disabled={!auth || !center}
            className="btn-primary flex items-center gap-1.5 px-4 py-2 text-sm disabled:opacity-50"
          >
            <Download className="w-3.5 h-3.5" />
            Export CSV
          </button>
        </div>
      </div>

      {hasError && (
        <div className="card border border-red-500/20 bg-red-500/5">
          <p className="text-red-400 text-sm">Failed to load audit stats. Ensure you are SUPER_ADMIN and services are running.</p>
        </div>
      )}

      {/* User Metrics */}
      <div>
        <p className="text-xs font-medium text-white/40 uppercase tracking-wider mb-3">User Metrics</p>
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
          <MetricCard label="Total Users"    value={auth?.totalUsers ?? '—'}           icon={Users}        color="text-brand-400"   delay={0.00} />
          <MetricCard label="New (7 days)"   value={auth?.registeredLast7Days ?? '—'}  icon={TrendingUp}   color="text-emerald-400" delay={0.04} />
          <MetricCard label="New (30 days)"  value={auth?.registeredLast30Days ?? '—'} icon={BarChart3}    color="text-sky-400"     delay={0.08} />
          <MetricCard label="MFA Enabled"    value={auth ? `${mfaPct}%` : '—'}         icon={ShieldCheck}  color="text-amber-400"   delay={0.12}
            sub={auth ? `${auth.mfaEnabledUsers} users` : undefined} />
          <MetricCard label="Email Verified" value={auth ? `${verifiedPct}%` : '—'}    icon={CheckCircle2} color="text-emerald-400" delay={0.16}
            sub={auth ? `${auth.verifiedEmailUsers} verified` : undefined} />
          <MetricCard label="Social Auth"    value={auth?.socialAuthUsers ?? '—'}       icon={Globe}        color="text-purple-400"  delay={0.20} />
          <MetricCard label="Total Centers"  value={center?.totalCenters ?? '—'}        icon={Building2}    color="text-sky-400"     delay={0.24} />
          <MetricCard label="Total Enrolled" value={center?.totalEnrolled ?? '—'}       icon={UserCheck}    color="text-brand-400"   delay={0.28} />
        </div>
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Role Distribution */}
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.2 }} className="card">
          <h3 className="font-semibold text-white text-sm mb-4 flex items-center gap-2">
            <Users className="w-4 h-4 text-brand-400" /> User Role Distribution
          </h3>
          {roleData.length > 0 ? (
            <ResponsiveContainer width="100%" height={220}>
              <PieChart>
                <Pie data={roleData} cx="50%" cy="50%" outerRadius={80} dataKey="value"
                  label={({ name, value }) => `${name}: ${value}`} labelLine={false}>
                  {roleData.map((entry, i) => <Cell key={i} fill={entry.fill} />)}
                </Pie>
                <Legend formatter={(value) => <span className="text-xs text-white/60">{value}</span>} />
                <Tooltip contentStyle={{ background: '#1a1a2e', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 8 }} labelStyle={{ color: '#fff' }} />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-[220px] flex items-center justify-center text-white/20 text-sm">Loading…</div>
          )}
        </motion.div>

        {/* Batch Status Distribution */}
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.25 }} className="card">
          <h3 className="font-semibold text-white text-sm mb-4 flex items-center gap-2">
            <BookOpen className="w-4 h-4 text-emerald-400" /> Batch Status Distribution
          </h3>
          {batchData.length > 0 ? (
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={batchData} barSize={40}>
                <XAxis dataKey="name" tick={{ fill: 'rgba(255,255,255,0.4)', fontSize: 12 }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fill: 'rgba(255,255,255,0.4)', fontSize: 12 }} axisLine={false} tickLine={false} />
                <Tooltip contentStyle={{ background: '#1a1a2e', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 8 }} labelStyle={{ color: '#fff' }} />
                <Bar dataKey="value" radius={[6, 6, 0, 0]}>
                  {batchData.map((entry, i) => <Cell key={i} fill={entry.fill} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-[220px] flex items-center justify-center text-white/20 text-sm">Loading…</div>
          )}
        </motion.div>
      </div>

      {/* NFR Assessment Table */}
      <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.3 }} className="card">
        <h3 className="font-semibold text-white text-sm mb-4 flex items-center gap-2">
          <Activity className="w-4 h-4 text-sky-400" /> NFR Assessment
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-x-8">
          <div>
            <p className="text-[10px] font-medium text-white/30 uppercase tracking-wider mb-2">Security</p>
            <NfrRow label="MFA Adoption Rate"  value={auth ? `${mfaPct}%`      : '—'} status={mfaPct >= 50 ? 'pass' : 'warn'} />
            <NfrRow label="Email Verification" value={auth ? `${verifiedPct}%` : '—'} status={verifiedPct >= 80 ? 'pass' : 'warn'} />
            <NfrRow label="Social Auth Users"  value={auth ? `${auth.socialAuthUsers}` : '—'} status="info" />
            <NfrRow label="Deleted Accounts"   value={auth ? `${auth.deletedUsers}` : '—'} status="info" />
          </div>
          <div>
            <p className="text-[10px] font-medium text-white/30 uppercase tracking-wider mb-2">Scalability & Operations</p>
            <NfrRow label="Avg Batch Fill Rate" value={center ? `${fillPct}%` : '—'}              status={fillPct >= 70 ? 'pass' : 'warn'} />
            <NfrRow label="Active Batches"      value={center ? `${center.activeBatches}` : '—'}  status="info" />
            <NfrRow label="Active Teachers"     value={center ? `${center.activeTeachers}` : '—'} status={center && center.activeTeachers > 0 ? 'pass' : 'warn'} />
            <NfrRow label="Growth (30d)"        value={auth ? `+${auth.registeredLast30Days} users` : '—'} status={auth && auth.registeredLast30Days > 0 ? 'pass' : 'warn'} />
          </div>
        </div>
      </motion.div>

      {/* Role detail table */}
      <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.35 }} className="card">
        <h3 className="font-semibold text-white text-sm mb-4 flex items-center gap-2">
          <Wifi className="w-4 h-4 text-amber-400" /> Platform User Breakdown
        </h3>
        {auth && (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs text-white/30 uppercase tracking-wider border-b border-white/5">
                  <th className="pb-2 pr-6">Role</th>
                  <th className="pb-2 pr-6">Count</th>
                  <th className="pb-2">Share</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {Object.entries(auth.usersByRole).map(([role, count]) => (
                  <tr key={role}>
                    <td className="py-2.5 pr-6">
                      <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium"
                        style={{ background: `${ROLE_COLORS[role] ?? '#6366f1'}20`, color: ROLE_COLORS[role] ?? '#6366f1' }}>
                        {role.replace('_', ' ')}
                      </span>
                    </td>
                    <td className="py-2.5 pr-6 text-white font-medium">{count}</td>
                    <td className="py-2.5">
                      <div className="flex items-center gap-2">
                        <div className="w-24 bg-white/5 rounded-full h-1.5">
                          <div className="h-1.5 rounded-full" style={{
                            width: `${Math.round((count / auth.totalUsers) * 100)}%`,
                            background: ROLE_COLORS[role] ?? '#6366f1',
                          }} />
                        </div>
                        <span className="text-xs text-white/40">
                          {Math.round((count / auth.totalUsers) * 100)}%
                        </span>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {!auth && <p className="text-sm text-white/20">Loading…</p>}
      </motion.div>
    </div>
  );
}
