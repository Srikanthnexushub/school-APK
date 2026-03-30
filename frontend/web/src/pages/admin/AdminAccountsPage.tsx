// src/pages/admin/AdminAccountsPage.tsx
import { useState } from 'react';
import { Wallet, CreditCard, Receipt } from 'lucide-react';
import { cn } from '../../lib/utils';
import AdminFeesPage from './AdminFeesPage';
import AdminBillingPage from './AdminBillingPage';

type AccountsTab = 'fees' | 'billing';

const tabs: { id: AccountsTab; label: string; icon: React.ElementType }[] = [
  { id: 'fees',    label: 'Fee Structures', icon: CreditCard },
  { id: 'billing', label: 'Billing Report', icon: Receipt    },
];

export default function AdminAccountsPage() {
  const [active, setActive] = useState<AccountsTab>('fees');

  return (
    <div className="p-6 space-y-6">
      {/* Page header */}
      <div className="flex items-center gap-4">
        <div className="p-2.5 rounded-xl bg-brand-500/10">
          <Wallet className="w-5 h-5 text-brand-400" />
        </div>
        <div>
          <h1 className="text-xl font-bold text-white">Accounts</h1>
          <p className="text-sm text-white/50">Manage fee structures, batch assignments and student billing</p>
        </div>
      </div>

      {/* Sub-tabs */}
      <div className="flex gap-1 bg-surface-100/40 p-1 rounded-xl w-fit">
        {tabs.map(({ id, label, icon: Icon }) => (
          <button
            key={id}
            onClick={() => setActive(id)}
            className={cn(
              'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200',
              active === id
                ? 'bg-brand-600 text-white shadow-sm'
                : 'text-white/50 hover:text-white hover:bg-white/5'
            )}
          >
            <Icon className="w-4 h-4" />
            {label}
          </button>
        ))}
      </div>

      {/* Content */}
      {active === 'fees'    && <AdminFeesPage embedded />}
      {active === 'billing' && <AdminBillingPage embedded />}
    </div>
  );
}
