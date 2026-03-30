// src/pages/admin/AdminPortalPage.tsx
import { useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import AdminDashboardPage from './AdminDashboardPage';
import AdminCentersPage from './AdminCentersPage';
import AdminBatchesPage from './AdminBatchesPage';
import AdminAssessmentsPage from './AdminAssessmentsPage';
import AdminBulkImportTeachersPage from './AdminBulkImportTeachersPage';
import AdminPendingTeachersPage from './AdminPendingTeachersPage';
import AdminStaffPage from './AdminStaffPage';
import AdminJobsPage from './AdminJobsPage';
import AdminAssignmentsTab from './AdminAssignmentsTab';
import AdminBannersPage from './AdminBannersPage';
import AdminPsychometricTab from './AdminPsychometricTab';
import AdminLibraryPage from './AdminLibraryPage';
import AdminAccountsPage from './AdminAccountsPage';
import { useAuthStore } from '../../stores/authStore';

type TabId = 'overview' | 'centers' | 'batches' | 'assessments' | 'teacher-import' | 'teacher-pending' | 'staff' | 'jobs' | 'assignments' | 'banners' | 'psychometric' | 'library' | 'accounts';

export default function AdminPortalPage() {
  const [searchParams] = useSearchParams();
  const activeTab = (searchParams.get('tab') as TabId) ?? 'overview';
  const { user } = useAuthStore();
  const isSuperAdmin = user?.role === 'SUPER_ADMIN';

  return (
    <motion.div
      key={activeTab}
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.18 }}
    >
      {activeTab === 'overview'        && <AdminDashboardPage />}
      {activeTab === 'centers'         && <AdminCentersPage />}
      {activeTab === 'batches'         && <AdminBatchesPage />}
      {activeTab === 'assessments'     && <AdminAssessmentsPage />}
      {activeTab === 'teacher-import'  && <AdminBulkImportTeachersPage />}
      {activeTab === 'teacher-pending' && <AdminPendingTeachersPage />}
      {activeTab === 'staff'           && <AdminStaffPage />}
      {activeTab === 'jobs'            && <AdminJobsPage />}
      {activeTab === 'assignments'     && <AdminAssignmentsTab />}
      {activeTab === 'psychometric'    && <AdminPsychometricTab />}
      {activeTab === 'library'         && <AdminLibraryPage />}
      {activeTab === 'accounts'        && <AdminAccountsPage />}
      {activeTab === 'banners'         && isSuperAdmin && <AdminBannersPage />}
    </motion.div>
  );
}
