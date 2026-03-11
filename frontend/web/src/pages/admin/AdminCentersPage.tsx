// src/pages/admin/AdminCentersPage.tsx
import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Plus, X, Building2, MapPin, Phone, Mail, Globe,
  CheckCircle2, AlertCircle, Clock, Pencil,
} from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { cn } from '../../lib/utils';
import { toast } from 'sonner';
import api from '../../lib/api';

// ─── Types ────────────────────────────────────────────────────────────────────

interface CenterResponse {
  id: string;
  name: string;
  code: string;
  address?: string;
  city?: string;
  state?: string;
  pincode?: string;
  phone?: string;
  email?: string;
  website?: string;
  logoUrl?: string;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  ownerId: string;
  createdAt: string;
  updatedAt: string;
}

interface CenterFormState {
  name: string;
  code: string;
  address: string;
  city: string;
  state: string;
  pincode: string;
  phone: string;
  email: string;
  website: string;
  logoUrl: string;
  ownerId: string;
}

const EMPTY_FORM: CenterFormState = {
  name: '', code: '', address: '', city: '', state: '',
  pincode: '', phone: '', email: '', website: '', logoUrl: '', ownerId: '',
};

// ─── Helpers ─────────────────────────────────────────────────────────────────

const STATUS_STYLES: Record<string, string> = {
  ACTIVE:    'bg-emerald-500/15 text-emerald-400 border-emerald-500/20',
  INACTIVE:  'bg-white/8 text-white/40 border-white/10',
  SUSPENDED: 'bg-red-500/15 text-red-400 border-red-500/20',
};

const STATUS_ICON: Record<string, React.ElementType> = {
  ACTIVE:    CheckCircle2,
  INACTIVE:  Clock,
  SUSPENDED: AlertCircle,
};

function formatDate(iso: string) {
  try { return new Date(iso).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }); }
  catch { return iso; }
}

// ─── Center Card ─────────────────────────────────────────────────────────────

function CenterCard({ center, onEdit }: { center: CenterResponse; onEdit: () => void }) {
  const StatusIcon = STATUS_ICON[center.status] ?? CheckCircle2;
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-surface-100/40 border border-white/5 rounded-xl p-5 flex flex-col gap-3 hover:border-white/10 transition-all"
    >
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-center gap-3 min-w-0">
          <div className="w-10 h-10 rounded-lg bg-brand-500/15 flex items-center justify-center flex-shrink-0">
            <Building2 className="w-5 h-5 text-brand-400" />
          </div>
          <div className="min-w-0">
            <p className="font-semibold text-white text-sm leading-tight truncate">{center.name}</p>
            <p className="text-xs text-white/40 font-mono mt-0.5">{center.code}</p>
          </div>
        </div>
        <div className="flex items-center gap-2 flex-shrink-0">
          <span className={cn('flex items-center gap-1 text-[10px] font-semibold uppercase tracking-wide px-2 py-0.5 rounded-full border', STATUS_STYLES[center.status])}>
            <StatusIcon className="w-3 h-3" />
            {center.status}
          </span>
          <button
            onClick={onEdit}
            className="p-1.5 rounded-lg text-white/30 hover:text-white hover:bg-white/5 transition-colors"
          >
            <Pencil className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      <div className="space-y-1.5">
        {center.city && (
          <div className="flex items-center gap-1.5 text-xs text-white/40">
            <MapPin className="w-3 h-3 flex-shrink-0" />
            <span className="truncate">{[center.city, center.state, center.pincode].filter(Boolean).join(', ')}</span>
          </div>
        )}
        {center.phone && (
          <div className="flex items-center gap-1.5 text-xs text-white/40">
            <Phone className="w-3 h-3 flex-shrink-0" />
            <span>{center.phone}</span>
          </div>
        )}
        {center.email && (
          <div className="flex items-center gap-1.5 text-xs text-white/40">
            <Mail className="w-3 h-3 flex-shrink-0" />
            <span className="truncate">{center.email}</span>
          </div>
        )}
        {center.website && (
          <div className="flex items-center gap-1.5 text-xs text-white/40">
            <Globe className="w-3 h-3 flex-shrink-0" />
            <a href={center.website} target="_blank" rel="noopener noreferrer"
               className="truncate hover:text-brand-400 transition-colors">{center.website}</a>
          </div>
        )}
      </div>

      <p className="text-[10px] text-white/20 mt-auto pt-2 border-t border-white/5">
        Created {formatDate(center.createdAt)}
      </p>
    </motion.div>
  );
}

// ─── Form Modal ───────────────────────────────────────────────────────────────

function CenterFormModal({
  title,
  initial,
  onSubmit,
  onClose,
  isPending,
}: {
  title: string;
  initial: CenterFormState;
  onSubmit: (data: CenterFormState) => void;
  onClose: () => void;
  isPending: boolean;
}) {
  const [form, setForm] = useState<CenterFormState>(initial);
  const set = (k: keyof CenterFormState, v: string) => setForm(f => ({ ...f, [k]: v }));
  const valid = form.name.trim() && form.code.trim();

  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        className="bg-surface-200 border border-white/10 rounded-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto"
      >
        <div className="flex items-center justify-between p-5 border-b border-white/8">
          <h2 className="text-base font-semibold text-white flex items-center gap-2">
            <Building2 className="w-4 h-4 text-brand-400" /> {title}
          </h2>
          <button onClick={onClose} className="p-1.5 rounded-lg text-white/30 hover:text-white hover:bg-white/5 transition-colors">
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="p-5 space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div className="col-span-2">
              <label className="block text-xs text-white/50 mb-1.5">Center Name *</label>
              <input value={form.name} onChange={e => set('name', e.target.value)}
                placeholder="e.g. NexusEd Coaching Center"
                className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-sm text-white placeholder-white/20 focus:outline-none focus:border-brand-500/50" />
            </div>
            <div>
              <label className="block text-xs text-white/50 mb-1.5">Code *</label>
              <input value={form.code} onChange={e => set('code', e.target.value.toUpperCase())}
                placeholder="NEC-001"
                className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-sm text-white font-mono placeholder-white/20 focus:outline-none focus:border-brand-500/50" />
            </div>
            <div>
              <label className="block text-xs text-white/50 mb-1.5">Owner User ID</label>
              <input value={form.ownerId} onChange={e => set('ownerId', e.target.value)}
                placeholder="UUID"
                className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-sm text-white font-mono placeholder-white/20 focus:outline-none focus:border-brand-500/50" />
            </div>
            <div className="col-span-2">
              <label className="block text-xs text-white/50 mb-1.5">Address</label>
              <input value={form.address} onChange={e => set('address', e.target.value)}
                placeholder="Street address"
                className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-sm text-white placeholder-white/20 focus:outline-none focus:border-brand-500/50" />
            </div>
            <div>
              <label className="block text-xs text-white/50 mb-1.5">City</label>
              <input value={form.city} onChange={e => set('city', e.target.value)}
                placeholder="Mumbai"
                className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-sm text-white placeholder-white/20 focus:outline-none focus:border-brand-500/50" />
            </div>
            <div>
              <label className="block text-xs text-white/50 mb-1.5">State</label>
              <input value={form.state} onChange={e => set('state', e.target.value)}
                placeholder="Maharashtra"
                className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-sm text-white placeholder-white/20 focus:outline-none focus:border-brand-500/50" />
            </div>
            <div>
              <label className="block text-xs text-white/50 mb-1.5">Pincode</label>
              <input value={form.pincode} onChange={e => set('pincode', e.target.value)}
                placeholder="400001"
                className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-sm text-white placeholder-white/20 focus:outline-none focus:border-brand-500/50" />
            </div>
            <div>
              <label className="block text-xs text-white/50 mb-1.5">Phone</label>
              <input value={form.phone} onChange={e => set('phone', e.target.value)}
                placeholder="+91 9876543210"
                className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-sm text-white placeholder-white/20 focus:outline-none focus:border-brand-500/50" />
            </div>
            <div>
              <label className="block text-xs text-white/50 mb-1.5">Email</label>
              <input value={form.email} onChange={e => set('email', e.target.value)}
                placeholder="contact@center.com" type="email"
                className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-sm text-white placeholder-white/20 focus:outline-none focus:border-brand-500/50" />
            </div>
            <div>
              <label className="block text-xs text-white/50 mb-1.5">Website</label>
              <input value={form.website} onChange={e => set('website', e.target.value)}
                placeholder="https://center.com"
                className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-sm text-white placeholder-white/20 focus:outline-none focus:border-brand-500/50" />
            </div>
            <div>
              <label className="block text-xs text-white/50 mb-1.5">Logo URL</label>
              <input value={form.logoUrl} onChange={e => set('logoUrl', e.target.value)}
                placeholder="https://cdn.../logo.png"
                className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-sm text-white placeholder-white/20 focus:outline-none focus:border-brand-500/50" />
            </div>
          </div>
        </div>

        <div className="flex justify-end gap-3 p-5 border-t border-white/8">
          <button onClick={onClose}
            className="px-4 py-2 rounded-xl text-sm text-white/50 hover:text-white hover:bg-white/5 transition-colors">
            Cancel
          </button>
          <button
            disabled={!valid || isPending}
            onClick={() => onSubmit(form)}
            className="flex items-center gap-2 px-4 py-2 rounded-xl text-sm bg-brand-600 text-white hover:bg-brand-500 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
          >
            {isPending ? <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" /> : <Plus className="w-4 h-4" />}
            {title}
          </button>
        </div>
      </motion.div>
    </div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function AdminCentersPage() {
  const qc = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [editCenter, setEditCenter] = useState<CenterResponse | null>(null);

  const { data: centers = [], isLoading } = useQuery<CenterResponse[]>({
    queryKey: ['admin-centers'],
    queryFn: async () => {
      const res = await api.get('/api/v1/centers');
      const d = res.data;
      return Array.isArray(d) ? d : (d.content ?? []);
    },
  });

  const createMutation = useMutation({
    mutationFn: (data: CenterFormState) => api.post('/api/v1/centers', {
      name: data.name, code: data.code, address: data.address || null,
      city: data.city || null, state: data.state || null, pincode: data.pincode || null,
      phone: data.phone || null, email: data.email || null, website: data.website || null,
      logoUrl: data.logoUrl || null, ownerId: data.ownerId || null,
    }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin-centers'] }); setShowCreate(false); toast.success('Center created'); },
    onError: () => toast.error('Failed to create center'),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: CenterFormState }) =>
      api.put(`/api/v1/centers/${id}`, {
        name: data.name, address: data.address || null,
        city: data.city || null, state: data.state || null, pincode: data.pincode || null,
        phone: data.phone || null, email: data.email || null, website: data.website || null,
        logoUrl: data.logoUrl || null,
      }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin-centers'] }); setEditCenter(null); toast.success('Center updated'); },
    onError: () => toast.error('Failed to update center'),
  });

  const editFormInitial = editCenter ? {
    name: editCenter.name, code: editCenter.code,
    address: editCenter.address ?? '', city: editCenter.city ?? '',
    state: editCenter.state ?? '', pincode: editCenter.pincode ?? '',
    phone: editCenter.phone ?? '', email: editCenter.email ?? '',
    website: editCenter.website ?? '', logoUrl: editCenter.logoUrl ?? '',
    ownerId: editCenter.ownerId,
  } : EMPTY_FORM;

  return (
    <div className="p-6 max-w-6xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <Building2 className="w-6 h-6 text-brand-400" /> Centers
          </h1>
          <p className="text-sm text-white/40 mt-1">Manage coaching centers on the platform</p>
        </div>
        <button
          onClick={() => setShowCreate(true)}
          className="flex items-center gap-2 px-4 py-2 rounded-xl text-sm bg-brand-600 text-white hover:bg-brand-500 transition-colors"
        >
          <Plus className="w-4 h-4" /> Add Center
        </button>
      </div>

      {/* Stats bar */}
      {!isLoading && (
        <div className="flex gap-4">
          {(['ACTIVE', 'INACTIVE', 'SUSPENDED'] as const).map(s => {
            const count = centers.filter(c => c.status === s).length;
            return (
              <div key={s} className={cn('flex items-center gap-2 px-3 py-1.5 rounded-lg border text-xs font-medium', STATUS_STYLES[s])}>
                {s} <span className="opacity-60">({count})</span>
              </div>
            );
          })}
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg border border-white/10 text-xs font-medium text-white/40">
            Total <span className="opacity-60">({centers.length})</span>
          </div>
        </div>
      )}

      {/* Content */}
      {isLoading ? (
        <div className="flex items-center justify-center py-20 text-white/40">
          <span className="w-5 h-5 border-2 border-white/20 border-t-white/60 rounded-full animate-spin mr-3" />
          Loading centers…
        </div>
      ) : centers.length === 0 ? (
        <div className="text-center py-20">
          <Building2 className="w-12 h-12 mx-auto mb-4 text-white/20" />
          <p className="text-white/40 font-medium">No centers yet</p>
          <p className="text-white/25 text-sm mt-1">Create the first coaching center to get started.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {centers.map(c => (
            <CenterCard key={c.id} center={c} onEdit={() => setEditCenter(c)} />
          ))}
        </div>
      )}

      {/* Modals */}
      <AnimatePresence>
        {showCreate && (
          <CenterFormModal
            title="Add Center"
            initial={EMPTY_FORM}
            onSubmit={data => createMutation.mutate(data)}
            onClose={() => setShowCreate(false)}
            isPending={createMutation.isPending}
          />
        )}
        {editCenter && (
          <CenterFormModal
            title="Edit Center"
            initial={editFormInitial}
            onSubmit={data => updateMutation.mutate({ id: editCenter.id, data })}
            onClose={() => setEditCenter(null)}
            isPending={updateMutation.isPending}
          />
        )}
      </AnimatePresence>
    </div>
  );
}
