// src/pages/admin/AdminPortalPage.tsx
import { useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  LayoutDashboard, Building2, Users, ClipboardList, Clock, XCircle, Upload, UserCheck,
} from 'lucide-react';
import { cn } from '../../lib/utils';
import { useAuthStore } from '../../stores/authStore';
import { useQuery } from '@tanstack/react-query';
import api from '../../lib/api';
import AdminDashboardPage from './AdminDashboardPage';
import AdminCentersPage from './AdminCentersPage';
import AdminBatchesPage from './AdminBatchesPage';
import AdminAssessmentsPage from './AdminAssessmentsPage';
import AdminPendingRegistrationsPage from './AdminPendingRegistrationsPage';
import AdminBulkImportTeachersPage from './AdminBulkImportTeachersPage';
import AdminPendingTeachersPage from './AdminPendingTeachersPage';

// ─── Types ────────────────────────────────────────────────────────────────────

interface RegistrationStatus {
  centerId: string;
  name: string;
  status: string;
  rejectionReason?: string;
  createdAt: string;
}

// ─── Pending / Rejected States ────────────────────────────────────────────────

function PendingVerificationScreen({ name }: { name: string }) {
  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] p-8 text-center">
      <div className="w-20 h-20 rounded-full bg-amber-500/15 border border-amber-500/30 flex items-center justify-center mb-6">
        <Clock className="w-10 h-10 text-amber-400" />
      </div>
      <h2 className="text-2xl font-bold text-white mb-3">Registration Under Review</h2>
      <p className="text-white/50 text-sm leading-relaxed max-w-sm mb-2">
        <span className="text-white/80">{name}</span> is currently under review.
      </p>
      <p className="text-white/40 text-sm max-w-sm">
        Our team will verify your details within 1–2 business days.
        You'll receive an email once approved.
      </p>
    </div>
  );
}

function RejectedScreen({ name, reason }: { name: string; reason?: string }) {
  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] p-8 text-center">
      <div className="w-20 h-20 rounded-full bg-red-500/15 border border-red-500/30 flex items-center justify-center mb-6">
        <XCircle className="w-10 h-10 text-red-400" />
      </div>
      <h2 className="text-2xl font-bold text-white mb-3">Registration Rejected</h2>
      <p className="text-white/50 text-sm mb-4">
        The registration for <span className="text-white/80">{name}</span> was not approved.
      </p>
      {reason && (
        <div className="bg-red-500/10 border border-red-500/20 rounded-xl px-5 py-3 max-w-sm text-sm text-red-300">
          {reason}
        </div>
      )}
      <p className="text-white/30 text-sm mt-6">
        Please contact support or re-register with corrected details.
      </p>
    </div>
  );
}

// ─── Tab config ───────────────────────────────────────────────────────────────

type TabId = 'overview' | 'centers' | 'batches' | 'assessments' | 'pending' | 'teacher-import' | 'teacher-pending';

interface Tab {
  id: TabId;
  label: string;
  icon: React.ElementType;
  superAdminOnly?: boolean;
  centerAdminOnly?: boolean;
}

const TABS: Tab[] = [
  { id: 'overview',         label: 'Overview',          icon: LayoutDashboard },
  { id: 'centers',          label: 'Centers',            icon: Building2 },
  { id: 'batches',          label: 'Batches',            icon: Users },
  { id: 'assessments',      label: 'Assessments',        icon: ClipboardList },
  { id: 'teacher-import',   label: 'Bulk Import',        icon: Upload, centerAdminOnly: true },
  { id: 'teacher-pending',  label: 'Teacher Approvals',  icon: UserCheck, centerAdminOnly: true },
  { id: 'pending',          label: 'Pending',            icon: Clock, superAdminOnly: true },
];

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function AdminPortalPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab = (searchParams.get('tab') as TabId) ?? 'overview';
  const role = useAuthStore(s => s.user?.role);
  const isSuperAdmin = role === 'SUPER_ADMIN';
  const isCenterAdmin = role === 'CENTER_ADMIN';

  // For CENTER_ADMIN: check registration status
  const { data: regStatus, isLoading: regLoading } = useQuery<RegistrationStatus>({
    queryKey: ['my-center-registration'],
    queryFn: async () => {
      const res = await api.get('/api/v1/centers/my-registration');
      return res.data;
    },
    enabled: isCenterAdmin && !isSuperAdmin,
    retry: false,
  });

  const switchTab = (id: TabId) => {
    setSearchParams({ tab: id }, { replace: true });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const visibleTabs = TABS.filter(t =>
    (!t.superAdminOnly || isSuperAdmin) &&
    (!t.centerAdminOnly || isCenterAdmin || isSuperAdmin)
  );

  // CENTER_ADMIN with pending/rejected registration — show status screen
  if (isCenterAdmin && !isSuperAdmin) {
    if (regLoading) {
      return (
        <div className="flex items-center justify-center min-h-[60vh] text-white/40">
          <span className="w-5 h-5 border-2 border-white/20 border-t-white/60 rounded-full animate-spin mr-3" />
          Loading your registration status…
        </div>
      );
    }
    if (regStatus?.status === 'PENDING_VERIFICATION') {
      return <PendingVerificationScreen name={regStatus.name} />;
    }
    if (regStatus?.status === 'REJECTED') {
      return <RejectedScreen name={regStatus.name} reason={regStatus.rejectionReason} />;
    }
    // regStatus not found (404) or ACTIVE — show normal portal
  }

  return (
    <div className="flex flex-col min-h-full">
      {/* ── Tab bar ─────────────────────────────────────────────────────────── */}
      <div className="sticky top-0 z-20 bg-surface-50/95 backdrop-blur border-b border-white/5 px-4 lg:px-8 pt-4">
        <div className="flex items-end gap-1 overflow-x-auto scrollbar-none max-w-7xl mx-auto">
          {visibleTabs.map((tab) => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => switchTab(tab.id)}
                className={cn(
                  'group relative flex items-center gap-2 px-4 py-3 text-sm font-medium whitespace-nowrap transition-colors rounded-t-xl border-b-2',
                  isActive
                    ? 'text-white border-brand-500 bg-white/3'
                    : 'text-white/40 border-transparent hover:text-white/70 hover:bg-white/3'
                )}
              >
                <Icon className={cn('w-4 h-4 flex-shrink-0', isActive ? 'text-brand-400' : 'text-white/30 group-hover:text-white/50')} />
                {tab.label}
                {isActive && (
                  <motion.div
                    layoutId="admin-tab-indicator"
                    className="absolute bottom-0 left-0 right-0 h-0.5 bg-brand-500 rounded-t"
                    transition={{ type: 'spring', stiffness: 400, damping: 30 }}
                  />
                )}
              </button>
            );
          })}
        </div>
      </div>

      {/* ── Tab content ─────────────────────────────────────────────────────── */}
      <motion.div
        key={activeTab}
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.18 }}
        className="flex-1"
      >
        {activeTab === 'overview'        && <AdminDashboardPage />}
        {activeTab === 'centers'         && <AdminCentersPage />}
        {activeTab === 'batches'         && <AdminBatchesPage />}
        {activeTab === 'assessments'     && <AdminAssessmentsPage />}
        {activeTab === 'teacher-import'  && <AdminBulkImportTeachersPage />}
        {activeTab === 'teacher-pending' && <AdminPendingTeachersPage />}
        {activeTab === 'pending'         && isSuperAdmin && <AdminPendingRegistrationsPage />}
      </motion.div>
    </div>
  );
}
