// src/pages/admin/AdminBannersPage.tsx
import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Megaphone, Plus, X, Loader2, AlertTriangle,
  Edit2, Trash2, ToggleLeft, ToggleRight,
} from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { cn } from '../../lib/utils';
import api from '../../lib/api';

// ─── Types ────────────────────────────────────────────────────────────────────

interface BannerResponse {
  id: string;
  title: string;
  subtitle?: string;
  imageUrl?: string;
  linkUrl?: string;
  linkLabel?: string;
  audience: string;
  bannerType?: string;
  bgColor?: string;
  displayOrder: number;
  isActive: boolean;
  startDate?: string;
  endDate?: string;
  createdAt: string;
}

interface BannerFormState {
  title: string;
  subtitle: string;
  imageUrl: string;
  linkUrl: string;
  linkLabel: string;
  audience: string;
  bannerType: string;
  bgColor: string;
  displayOrder: number;
  isActive: boolean;
  startDate: string;
  endDate: string;
}

// ─── Audience badge ───────────────────────────────────────────────────────────

const audienceColors: Record<string, string> = {
  PARENT:       'bg-emerald-500/15 text-emerald-400',
  CENTER_ADMIN: 'bg-amber-500/15 text-amber-400',
  ALL:          'bg-brand-500/15 text-brand-400',
};

const bannerTypeColors: Record<string, string> = {
  HERO:   'bg-sky-500/15 text-sky-400',
  TICKER: 'bg-violet-500/15 text-violet-400',
};

// ─── Banner Form Modal ────────────────────────────────────────────────────────

function BannerFormModal({
  initial,
  onClose,
  onSave,
  isSubmitting,
}: {
  initial?: BannerResponse;
  onClose: () => void;
  onSave: (data: BannerFormState) => void;
  isSubmitting: boolean;
}) {
  const [form, setForm] = useState<BannerFormState>({
    title:        initial?.title ?? '',
    subtitle:     initial?.subtitle ?? '',
    imageUrl:     initial?.imageUrl ?? '',
    linkUrl:      initial?.linkUrl ?? '',
    linkLabel:    initial?.linkLabel ?? '',
    audience:     initial?.audience ?? 'ALL',
    bannerType:   initial?.bannerType ?? 'HERO',
    bgColor:      initial?.bgColor ?? '#1e1b4b',
    displayOrder: initial?.displayOrder ?? 0,
    isActive:     initial?.isActive ?? true,
    startDate:    initial?.startDate ? initial.startDate.slice(0, 10) : '',
    endDate:      initial?.endDate ? initial.endDate.slice(0, 10) : '',
  });

  function handleSubmit() {
    if (!form.title.trim()) {
      toast.error('Title is required.');
      return;
    }
    onSave(form);
  }

  return (
    <>
      <motion.div
        initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
        onClick={onClose}
        className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm"
      />
      <motion.div
        initial={{ opacity: 0, scale: 0.95, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: 20 }}
        transition={{ duration: 0.2 }}
        className="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-[51] w-full max-w-lg"
      >
        <div className="bg-surface-50 border border-white/10 rounded-2xl shadow-2xl overflow-hidden max-h-[90vh] overflow-y-auto">
          <div className="flex items-center justify-between px-6 py-4 border-b border-white/5 sticky top-0 bg-surface-50 z-10">
            <div>
              <h3 className="font-semibold text-white">{initial ? 'Edit Banner' : 'Create Banner'}</h3>
              <p className="text-xs text-white/40 mt-0.5">Fill in banner details</p>
            </div>
            <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-white/5 text-white/30 hover:text-white/70 transition-colors">
              <X className="w-4 h-4" />
            </button>
          </div>

          <div className="p-6 space-y-4">
            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Title *</label>
              <input
                className="input w-full"
                placeholder="Banner title"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Subtitle</label>
              <input
                className="input w-full"
                placeholder="Short description"
                value={form.subtitle}
                onChange={(e) => setForm({ ...form, subtitle: e.target.value })}
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Image URL</label>
              <input
                className="input w-full"
                placeholder="https://…"
                value={form.imageUrl}
                onChange={(e) => setForm({ ...form, imageUrl: e.target.value })}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">Link URL</label>
                <input
                  className="input w-full"
                  placeholder="https://…"
                  value={form.linkUrl}
                  onChange={(e) => setForm({ ...form, linkUrl: e.target.value })}
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">Link Label</label>
                <input
                  className="input w-full"
                  placeholder="Learn More"
                  value={form.linkLabel}
                  onChange={(e) => setForm({ ...form, linkLabel: e.target.value })}
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">Audience</label>
                <select
                  className="input w-full"
                  value={form.audience}
                  onChange={(e) => setForm({ ...form, audience: e.target.value })}
                >
                  <option value="ALL">All</option>
                  <option value="PARENT">Parent</option>
                  <option value="CENTER_ADMIN">Center Admin</option>
                </select>
              </div>
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">Banner Type</label>
                <select
                  className="input w-full"
                  value={form.bannerType}
                  onChange={(e) => setForm({ ...form, bannerType: e.target.value })}
                >
                  <option value="HERO">Hero Carousel</option>
                  <option value="TICKER">Running Ticker</option>
                </select>
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Display Order</label>
              <input
                type="number"
                className="input w-full"
                value={form.displayOrder}
                onChange={(e) => setForm({ ...form, displayOrder: Number(e.target.value) })}
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Background Color</label>
              <div className="flex gap-2 items-center">
                <input
                  type="color"
                  className="w-10 h-10 rounded-lg border border-white/10 bg-surface-100 cursor-pointer"
                  value={form.bgColor}
                  onChange={(e) => setForm({ ...form, bgColor: e.target.value })}
                />
                <input
                  className="input flex-1"
                  placeholder="#1e1b4b"
                  value={form.bgColor}
                  onChange={(e) => setForm({ ...form, bgColor: e.target.value })}
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">Start Date</label>
                <input
                  type="date"
                  className="input w-full"
                  value={form.startDate}
                  onChange={(e) => setForm({ ...form, startDate: e.target.value })}
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">End Date</label>
                <input
                  type="date"
                  className="input w-full"
                  value={form.endDate}
                  onChange={(e) => setForm({ ...form, endDate: e.target.value })}
                />
              </div>
            </div>

            <div className="flex items-center gap-3">
              <label className="text-sm font-medium text-white/60">Active</label>
              <button
                type="button"
                onClick={() => setForm({ ...form, isActive: !form.isActive })}
                className={cn('flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors', form.isActive ? 'bg-emerald-500/15 text-emerald-400' : 'bg-white/5 text-white/30')}
              >
                {form.isActive ? <ToggleRight className="w-4 h-4" /> : <ToggleLeft className="w-4 h-4" />}
                {form.isActive ? 'Active' : 'Inactive'}
              </button>
            </div>

            <div className="flex gap-3 pt-2">
              <button type="button" onClick={onClose} className="flex-1 py-2.5 rounded-xl border border-white/10 text-sm font-medium text-white/60 hover:text-white hover:border-white/20 transition-colors">
                Cancel
              </button>
              <button
                onClick={handleSubmit}
                disabled={isSubmitting}
                className="flex-1 btn-primary py-2.5 text-sm font-medium flex items-center justify-center gap-2 disabled:opacity-50"
              >
                {isSubmitting && <Loader2 className="w-4 h-4 animate-spin" />}
                {isSubmitting ? 'Saving…' : (initial ? 'Save Changes' : 'Create Banner')}
              </button>
            </div>
          </div>
        </div>
      </motion.div>
    </>
  );
}

// ─── Main page ────────────────────────────────────────────────────────────────

export default function AdminBannersPage() {
  const qc = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [editing, setEditing] = useState<BannerResponse | null>(null);

  const { data: banners = [], isLoading, isError } = useQuery<BannerResponse[]>({
    queryKey: ['banners-all'],
    queryFn: () =>
      api.get('/api/v1/banners/all').then((r) => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
  });

  const createMutation = useMutation({
    mutationFn: (data: BannerFormState) => api.post('/api/v1/banners', data),
    onSuccess: () => {
      toast.success('Banner created!');
      qc.invalidateQueries({ queryKey: ['banners-all'] });
      qc.invalidateQueries({ queryKey: ['banners'] });
      setShowCreate(false);
    },
    onError: () => toast.error('Failed to create banner.'),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: BannerFormState }) =>
      api.put(`/api/v1/banners/${id}`, data),
    onSuccess: () => {
      toast.success('Banner updated!');
      qc.invalidateQueries({ queryKey: ['banners-all'] });
      qc.invalidateQueries({ queryKey: ['banners'] });
      setEditing(null);
    },
    onError: () => toast.error('Failed to update banner.'),
  });

  const toggleMutation = useMutation({
    mutationFn: (id: string) => api.patch(`/api/v1/banners/${id}/toggle`),
    onSuccess: () => {
      toast.success('Banner toggled!');
      qc.invalidateQueries({ queryKey: ['banners-all'] });
      qc.invalidateQueries({ queryKey: ['banners'] });
    },
    onError: () => toast.error('Failed to toggle banner.'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => api.delete(`/api/v1/banners/${id}`),
    onSuccess: () => {
      toast.success('Banner deleted.');
      qc.invalidateQueries({ queryKey: ['banners-all'] });
      qc.invalidateQueries({ queryKey: ['banners'] });
    },
    onError: () => toast.error('Failed to delete banner.'),
  });

  return (
    <div className="p-4 lg:p-8 space-y-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-white">Banners</h2>
          <p className="text-white/50 text-sm mt-0.5">Manage advertisement banners shown to users.</p>
        </div>
        <button
          onClick={() => setShowCreate(true)}
          className="btn-primary flex items-center gap-2 px-4 py-2.5 text-sm font-medium"
        >
          <Plus className="w-4 h-4" />
          Create Banner
        </button>
      </div>

      {/* Loading */}
      {isLoading && (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="w-6 h-6 text-brand-400 animate-spin" />
        </div>
      )}

      {/* Error */}
      {isError && (
        <div className="card text-center py-12">
          <AlertTriangle className="w-10 h-10 text-red-400/50 mx-auto mb-3" />
          <p className="text-white/50 text-sm">Failed to load banners.</p>
        </div>
      )}

      {/* Empty */}
      {!isLoading && !isError && banners.length === 0 && (
        <div className="card text-center py-12">
          <Megaphone className="w-10 h-10 text-white/20 mx-auto mb-3" />
          <p className="text-white/50 text-sm">No banners yet.</p>
          <button
            onClick={() => setShowCreate(true)}
            className="btn-primary inline-flex items-center gap-2 px-4 py-2 text-sm mt-4"
          >
            <Plus className="w-4 h-4" />
            Create First Banner
          </button>
        </div>
      )}

      {/* Table */}
      {!isLoading && !isError && banners.length > 0 && (
        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          className="card overflow-x-auto"
        >
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-xs text-white/30 uppercase tracking-wider border-b border-white/5">
                <th className="pb-2 pr-4">Title</th>
                <th className="pb-2 pr-4">Audience</th>
                <th className="pb-2 pr-4">Type</th>
                <th className="pb-2 pr-4">Active</th>
                <th className="pb-2 pr-4">Order</th>
                <th className="pb-2 pr-4">Start</th>
                <th className="pb-2 pr-4">End</th>
                <th className="pb-2">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {banners.map((banner) => (
                <tr key={banner.id} className="hover:bg-white/3 transition-colors group">
                  <td className="py-3 pr-4">
                    <div className="flex items-center gap-2">
                      {banner.bgColor && (
                        <div
                          className="w-3 h-3 rounded-full flex-shrink-0"
                          style={{ background: banner.bgColor }}
                        />
                      )}
                      <div>
                        <div className="font-medium text-white text-sm">{banner.title}</div>
                        {banner.subtitle && (
                          <div className="text-xs text-white/40 mt-0.5 line-clamp-1">{banner.subtitle}</div>
                        )}
                      </div>
                    </div>
                  </td>
                  <td className="py-3 pr-4">
                    <span className={cn('badge text-xs', audienceColors[banner.audience] ?? 'bg-white/10 text-white/40')}>
                      {banner.audience}
                    </span>
                  </td>
                  <td className="py-3 pr-4">
                    <span className={cn('badge text-xs', bannerTypeColors[banner.bannerType ?? 'HERO'] ?? 'bg-white/10 text-white/40')}>
                      {banner.bannerType === 'TICKER' ? 'Ticker' : 'Hero'}
                    </span>
                  </td>
                  <td className="py-3 pr-4">
                    <button
                      onClick={() => toggleMutation.mutate(banner.id)}
                      disabled={toggleMutation.isPending}
                      className={cn(
                        'flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-medium transition-colors',
                        banner.isActive
                          ? 'bg-emerald-500/15 text-emerald-400 hover:bg-emerald-500/25'
                          : 'bg-white/5 text-white/30 hover:bg-white/10'
                      )}
                    >
                      {banner.isActive ? <ToggleRight className="w-3.5 h-3.5" /> : <ToggleLeft className="w-3.5 h-3.5" />}
                      {banner.isActive ? 'Active' : 'Off'}
                    </button>
                  </td>
                  <td className="py-3 pr-4 text-white/50 text-xs">{banner.displayOrder}</td>
                  <td className="py-3 pr-4 text-white/40 text-xs">
                    {banner.startDate ? new Date(banner.startDate).toLocaleDateString('en-IN', { day: '2-digit', month: 'short' }) : '—'}
                  </td>
                  <td className="py-3 pr-4 text-white/40 text-xs">
                    {banner.endDate ? new Date(banner.endDate).toLocaleDateString('en-IN', { day: '2-digit', month: 'short' }) : '—'}
                  </td>
                  <td className="py-3">
                    <div className="flex items-center gap-1.5">
                      <button
                        onClick={() => setEditing(banner)}
                        className="p-1.5 rounded-lg hover:bg-white/5 text-white/30 hover:text-white/70 transition-colors"
                        title="Edit"
                      >
                        <Edit2 className="w-3.5 h-3.5" />
                      </button>
                      <button
                        onClick={() => {
                          if (window.confirm('Delete this banner?')) {
                            deleteMutation.mutate(banner.id);
                          }
                        }}
                        disabled={deleteMutation.isPending}
                        className="p-1.5 rounded-lg hover:bg-red-500/10 text-white/30 hover:text-red-400 transition-colors disabled:opacity-50"
                        title="Delete"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </motion.div>
      )}

      {/* Create modal */}
      <AnimatePresence>
        {showCreate && (
          <BannerFormModal
            onClose={() => setShowCreate(false)}
            onSave={(data) => createMutation.mutate(data)}
            isSubmitting={createMutation.isPending}
          />
        )}
      </AnimatePresence>

      {/* Edit modal */}
      <AnimatePresence>
        {editing && (
          <BannerFormModal
            initial={editing}
            onClose={() => setEditing(null)}
            onSave={(data) => updateMutation.mutate({ id: editing.id, data })}
            isSubmitting={updateMutation.isPending}
          />
        )}
      </AnimatePresence>
    </div>
  );
}
