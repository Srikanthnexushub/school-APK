// src/pages/admin/AdminPortalPage.tsx
import { useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  LayoutDashboard, Building2, Users, ClipboardList,
} from 'lucide-react';
import { cn } from '../../lib/utils';
import AdminDashboardPage from './AdminDashboardPage';
import AdminCentersPage from './AdminCentersPage';
import AdminBatchesPage from './AdminBatchesPage';
import AdminAssessmentsPage from './AdminAssessmentsPage';

// ─── Tab config ───────────────────────────────────────────────────────────────

type TabId = 'overview' | 'centers' | 'batches' | 'assessments';

interface Tab {
  id: TabId;
  label: string;
  icon: React.ElementType;
  description: string;
}

const TABS: Tab[] = [
  { id: 'overview',     label: 'Overview',     icon: LayoutDashboard, description: 'Platform stats & charts' },
  { id: 'centers',      label: 'Centers',      icon: Building2,       description: 'Coaching center management' },
  { id: 'batches',      label: 'Batches',      icon: Users,           description: 'Batch & teacher management' },
  { id: 'assessments',  label: 'Assessments',  icon: ClipboardList,   description: 'Exams & question banks' },
];

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function AdminPortalPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab = (searchParams.get('tab') as TabId) ?? 'overview';

  const switchTab = (id: TabId) => {
    setSearchParams({ tab: id }, { replace: true });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div className="flex flex-col min-h-full">
      {/* ── Tab bar ─────────────────────────────────────────────────────────── */}
      <div className="sticky top-0 z-20 bg-surface-50/95 backdrop-blur border-b border-white/5 px-4 lg:px-8 pt-4">
        <div className="flex items-end gap-1 overflow-x-auto scrollbar-none max-w-7xl mx-auto">
          {TABS.map((tab) => {
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
        {activeTab === 'overview'    && <AdminDashboardPage />}
        {activeTab === 'centers'     && <AdminCentersPage />}
        {activeTab === 'batches'     && <AdminBatchesPage />}
        {activeTab === 'assessments' && <AdminAssessmentsPage />}
      </motion.div>
    </div>
  );
}
