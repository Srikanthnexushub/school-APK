import { useState, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Users, BookOpen, GraduationCap, TrendingUp,
  Plus, Edit2, Eye, X, Building2, MapPin, Phone,
  CheckCircle2, AlertTriangle, Clock,
} from 'lucide-react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend,
} from 'recharts';
import { useQuery, useQueries, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { cn } from '../../lib/utils';
import { toast } from 'sonner';
import api from '../../lib/api';
import AdvertisementBanner from '../../components/ui/AdvertisementBanner';
import FooterBanner from '../../components/ui/FooterBanner';
import TickerBanner from '../../components/ui/TickerBanner';
import { useAuthStore } from '../../stores/authStore';

// ─── API Types ─────────────────────────────────────────────────────────────

interface CenterResponse {
  id: string;
  name: string;
  code: string;
  address: string;
  city: string;
  state: string;
  pincode: string;
  phone: string;
  email: string;
  website?: string;
  logoUrl?: string;
  status: string;
  ownerId?: string;
  createdAt: string;
  updatedAt: string;
}

interface BatchResponse {
  id: string;
  centerId: string;
  name: string;
  code: string;
  subject: string;
  teacherId?: string;
  maxStudents: number;
  enrolledCount: number;
  startDate?: string;
  endDate?: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

interface TeacherResponse {
  id: string;
  centerId: string;
  name: string;
  email: string;
  phone?: string;
  subject?: string;
  status?: string;
  createdAt?: string;
}

// ─── Derived UI types ────────────────────────────────────────────────────────

type CenterStatus = 'Active' | 'Inactive' | 'Pending';

interface CenterRow {
  id: string;
  name: string;
  city: string;
  address: string;
  phone: string;
  students: number;
  batches: number;
  status: CenterStatus;
  createdAt: string;
}

interface RecentEvent {
  id: string;
  type: 'enrollment' | 'payment' | 'schedule' | 'teacher';
  text: string;
  sub: string;
  time: string;
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

function mapStatus(raw: string): CenterStatus {
  const u = raw?.toUpperCase();
  if (u === 'ACTIVE') return 'Active';
  if (u === 'INACTIVE') return 'INACTIVE' === u ? 'Inactive' : 'Pending';
  if (u === 'PENDING') return 'Pending';
  return 'Inactive';
}

function timeAgo(iso: string): string {
  const ms = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(ms / 60000);
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  return `${Math.floor(hrs / 24)}d ago`;
}

const PIE_COLORS = ['#6366f1', '#34d399', '#fbbf24', '#f472b6', '#60a5fa', '#fb923c'];

// ─── Event colours/icons ──────────────────────────────────────────────────────

const eventColors: Record<RecentEvent['type'], { color: string; bg: string }> = {
  enrollment: { color: 'text-brand-400',   bg: 'bg-brand-500/15' },
  payment:    { color: 'text-emerald-400', bg: 'bg-emerald-500/15' },
  schedule:   { color: 'text-amber-400',   bg: 'bg-amber-500/15' },
  teacher:    { color: 'text-violet-400',  bg: 'bg-violet-500/15' },
};

const eventIcons: Record<RecentEvent['type'], React.ElementType> = {
  enrollment: Users,
  payment:    TrendingUp,
  schedule:   Clock,
  teacher:    GraduationCap,
};

// ─── Status badge ─────────────────────────────────────────────────────────────

const statusColors: Record<CenterStatus, string> = {
  Active:   'bg-emerald-500/15 text-emerald-400',
  Inactive: 'bg-white/5 text-white/30',
  Pending:  'bg-amber-500/15 text-amber-400',
};

const statusIcons: Record<CenterStatus, React.ElementType> = {
  Active:   CheckCircle2,
  Inactive: X,
  Pending:  AlertTriangle,
};

// ─── Add center form schema ───────────────────────────────────────────────────

const addCenterSchema = z.object({
  name:    z.string().min(3, 'Name must be at least 3 characters'),
  code:    z.string().min(2, 'Code is required').regex(/^[A-Z0-9]+$/, 'Code must be uppercase alphanumeric'),
  city:    z.string().min(2, 'City is required'),
  state:   z.string().min(2, 'State is required'),
  address: z.string().min(5, 'Address is required'),
  pincode: z.string().min(4, 'Pincode is required'),
  phone:   z.string().regex(/^\+?[\d\s-]{10,}$/, 'Invalid phone number'),
  email:   z.string().email('Invalid email'),
});
type AddCenterForm = z.infer<typeof addCenterSchema>;

// ─── Edit center form schema ──────────────────────────────────────────────────

const editCenterSchema = z.object({
  name:    z.string().min(3, 'Name must be at least 3 characters'),
  city:    z.string().min(2, 'City is required'),
  state:   z.string().min(2, 'State is required'),
  address: z.string().min(5, 'Address is required'),
  pincode: z.string().min(4, 'Pincode is required'),
  phone:   z.string().regex(/^\+?[\d\s-]{10,}$/, 'Invalid phone number'),
  email:   z.string().email('Invalid email'),
});
type EditCenterForm = z.infer<typeof editCenterSchema>;

// ─── KPI card ─────────────────────────────────────────────────────────────────

function KpiCard({ label, value, sub, icon: Icon, color, delay }: {
  label: string; value: string | number; sub: string;
  icon: React.ElementType; color: string; delay: number;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}
      transition={{ delay }}
      className="card"
    >
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs font-semibold text-white/40 uppercase tracking-wider">{label}</p>
          <p className="text-2xl font-bold text-white mt-1">{value}</p>
          <p className="text-xs text-white/30 mt-1">{sub}</p>
        </div>
        <div className={cn('p-2.5 rounded-xl', color)}>
          <Icon className="w-5 h-5" />
        </div>
      </div>
    </motion.div>
  );
}

// ─── Add center modal ─────────────────────────────────────────────────────────

function AddCenterModal({ onClose, onAdd, isSubmitting }: {
  onClose: () => void;
  onAdd: (data: AddCenterForm) => void;
  isSubmitting: boolean;
}) {
  const { register, handleSubmit, formState: { errors } } = useForm<AddCenterForm>({
    resolver: zodResolver(addCenterSchema),
  });

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
        className="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-[51] w-full max-w-md"
      >
        <div className="bg-surface-50 border border-white/10 rounded-2xl shadow-2xl overflow-hidden">
          <div className="flex items-center justify-between px-6 py-4 border-b border-white/5">
            <div>
              <h3 className="font-semibold text-white">Add New Center</h3>
              <p className="text-xs text-white/40 mt-0.5">Create a new coaching center</p>
            </div>
            <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-white/5 text-white/30 hover:text-white/70 transition-colors">
              <X className="w-4 h-4" />
            </button>
          </div>

          <form onSubmit={handleSubmit(onAdd)} className="p-6 space-y-4">
            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Center Name</label>
              <div className="relative">
                <Building2 className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
                <input {...register('name')} placeholder="NexusEd Whitefield" className="input pl-10 w-full" />
              </div>
              {errors.name && <p className="text-xs text-red-400 mt-1">{errors.name.message}</p>}
            </div>

            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Code (uppercase alphanumeric)</label>
              <input {...register('code')} placeholder="NEXWF001" className="input w-full" />
              {errors.code && <p className="text-xs text-red-400 mt-1">{errors.code.message}</p>}
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">City</label>
                <div className="relative">
                  <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
                  <input {...register('city')} placeholder="Bengaluru" className="input pl-10 w-full" />
                </div>
                {errors.city && <p className="text-xs text-red-400 mt-1">{errors.city.message}</p>}
              </div>
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">State</label>
                <input {...register('state')} placeholder="Karnataka" className="input w-full" />
                {errors.state && <p className="text-xs text-red-400 mt-1">{errors.state.message}</p>}
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Address</label>
              <input {...register('address')} placeholder="Full address" className="input w-full" />
              {errors.address && <p className="text-xs text-red-400 mt-1">{errors.address.message}</p>}
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">Pincode</label>
                <input {...register('pincode')} placeholder="560034" className="input w-full" />
                {errors.pincode && <p className="text-xs text-red-400 mt-1">{errors.pincode.message}</p>}
              </div>
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">Phone</label>
                <div className="relative">
                  <Phone className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
                  <input {...register('phone')} placeholder="+91 98765 43210" className="input pl-10 w-full" />
                </div>
                {errors.phone && <p className="text-xs text-red-400 mt-1">{errors.phone.message}</p>}
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Email</label>
              <input {...register('email')} type="email" placeholder="center@nexused.dev" className="input w-full" />
              {errors.email && <p className="text-xs text-red-400 mt-1">{errors.email.message}</p>}
            </div>

            <div className="flex gap-3 pt-2">
              <button type="button" onClick={onClose} className="flex-1 py-2.5 rounded-xl border border-white/10 text-sm font-medium text-white/60 hover:text-white hover:border-white/20 transition-colors">
                Cancel
              </button>
              <button type="submit" disabled={isSubmitting} className="flex-1 btn-primary py-2.5 text-sm font-medium disabled:opacity-50">
                {isSubmitting ? 'Adding…' : 'Add Center'}
              </button>
            </div>
          </form>
        </div>
      </motion.div>
    </>
  );
}

// ─── Edit center modal ────────────────────────────────────────────────────────

function EditCenterModal({ center, onClose, onSave, isSubmitting }: {
  center: CenterRow & { email?: string; state?: string; pincode?: string };
  onClose: () => void;
  onSave: (data: EditCenterForm) => void;
  isSubmitting: boolean;
}) {
  const { register, handleSubmit, formState: { errors } } = useForm<EditCenterForm>({
    resolver: zodResolver(editCenterSchema),
    defaultValues: {
      name:    center.name,
      city:    center.city,
      state:   center.state ?? '',
      address: center.address,
      pincode: center.pincode ?? '',
      phone:   center.phone,
      email:   center.email ?? '',
    },
  });

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
        className="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-[51] w-full max-w-md"
      >
        <div className="bg-surface-50 border border-white/10 rounded-2xl shadow-2xl overflow-hidden">
          <div className="flex items-center justify-between px-6 py-4 border-b border-white/5">
            <div>
              <h3 className="font-semibold text-white">Edit Center</h3>
              <p className="text-xs text-white/40 mt-0.5">{center.name}</p>
            </div>
            <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-white/5 text-white/30 hover:text-white/70 transition-colors">
              <X className="w-4 h-4" />
            </button>
          </div>

          <form onSubmit={handleSubmit(onSave)} className="p-6 space-y-4">
            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Center Name</label>
              <div className="relative">
                <Building2 className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
                <input {...register('name')} className="input pl-10 w-full" />
              </div>
              {errors.name && <p className="text-xs text-red-400 mt-1">{errors.name.message}</p>}
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">City</label>
                <div className="relative">
                  <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
                  <input {...register('city')} className="input pl-10 w-full" />
                </div>
                {errors.city && <p className="text-xs text-red-400 mt-1">{errors.city.message}</p>}
              </div>
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">State</label>
                <input {...register('state')} className="input w-full" />
                {errors.state && <p className="text-xs text-red-400 mt-1">{errors.state.message}</p>}
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Address</label>
              <input {...register('address')} className="input w-full" />
              {errors.address && <p className="text-xs text-red-400 mt-1">{errors.address.message}</p>}
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">Pincode</label>
                <input {...register('pincode')} className="input w-full" />
                {errors.pincode && <p className="text-xs text-red-400 mt-1">{errors.pincode.message}</p>}
              </div>
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">Phone</label>
                <div className="relative">
                  <Phone className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
                  <input {...register('phone')} className="input pl-10 w-full" />
                </div>
                {errors.phone && <p className="text-xs text-red-400 mt-1">{errors.phone.message}</p>}
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Email</label>
              <input {...register('email')} type="email" className="input w-full" />
              {errors.email && <p className="text-xs text-red-400 mt-1">{errors.email.message}</p>}
            </div>

            <div className="flex gap-3 pt-2">
              <button type="button" onClick={onClose} className="flex-1 py-2.5 rounded-xl border border-white/10 text-sm font-medium text-white/60 hover:text-white hover:border-white/20 transition-colors">
                Cancel
              </button>
              <button type="submit" disabled={isSubmitting} className="flex-1 btn-primary py-2.5 text-sm font-medium disabled:opacity-50">
                {isSubmitting ? 'Saving…' : 'Save Changes'}
              </button>
            </div>
          </form>
        </div>
      </motion.div>
    </>
  );
}

// ─── Loading skeleton ─────────────────────────────────────────────────────────

function Skeleton({ className }: { className?: string }) {
  return <div className={cn('animate-pulse bg-white/5 rounded-lg', className)} />;
}

// ─── Main page ────────────────────────────────────────────────────────────────

export default function AdminDashboardPage() {
  const user = useAuthStore(s => s.user);
  const qc = useQueryClient();
  const [showAddModal, setShowAddModal]   = useState(false);
  const [editingCenter, setEditingCenter] = useState<(CenterRow & { email?: string; state?: string; pincode?: string }) | null>(null);

  // ── Fetch centers ──────────────────────────────────────────────────────────
  const {
    data: rawCenters = [],
    isLoading: centersLoading,
  } = useQuery<CenterResponse[]>({
    queryKey: ['centers'],
    queryFn: () => api.get('/api/v1/centers').then((r) => { const d = r.data; return Array.isArray(d) ? d : (d.content ?? []); }),
  });

  // ── Fetch batches for every center (parallel) ──────────────────────────────
  const batchQueries = useQueries({
    queries: rawCenters.map((c) => ({
      queryKey: ['batches', c.id],
      queryFn: () => api.get(`/api/v1/centers/${c.id}/batches`).then((r) => { const d = r.data; return Array.isArray(d) ? d : (d.content ?? []); }) as Promise<BatchResponse[]>,
      enabled: !!c.id,
    })),
  });

  // ── Fetch teachers for every center (parallel) ─────────────────────────────
  const teacherQueries = useQueries({
    queries: rawCenters.map((c) => ({
      queryKey: ['teachers', c.id],
      queryFn: () => api.get(`/api/v1/centers/${c.id}/teachers`).then((r) => { const d = r.data; return Array.isArray(d) ? d : (d.content ?? []); }) as Promise<TeacherResponse[]>,
      enabled: !!c.id,
    })),
  });

  // ── Aggregate data ─────────────────────────────────────────────────────────
  const allBatches: BatchResponse[] = useMemo(
    () => batchQueries.flatMap((q) => q.data ?? []),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [batchQueries.map((q) => q.data).join(',')]
  );

  const totalTeachers = useMemo(
    () => teacherQueries.reduce((sum, q) => sum + (q.data?.length ?? 0), 0),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [teacherQueries.map((q) => q.data?.length).join(',')]
  );

  const totalStudents = allBatches.reduce((s, b) => s + b.enrolledCount, 0);
  const totalBatches  = allBatches.length;

  // Build center rows with aggregated batch data
  const centers: CenterRow[] = rawCenters.map((c, idx) => {
    const batches = batchQueries[idx]?.data ?? [];
    return {
      id:       c.id,
      name:     c.name,
      city:     c.city,
      address:  c.address,
      phone:    c.phone,
      students: batches.reduce((s, b) => s + b.enrolledCount, 0),
      batches:  batches.length,
      status:   mapStatus(c.status),
      createdAt: c.createdAt,
      // extra fields for edit modal
      email:   c.email,
      state:   c.state,
      pincode: c.pincode,
    } as CenterRow & { email?: string; state?: string; pincode?: string };
  });

  const activeCenters = centers.filter((c) => c.status === 'Active').length;

  // ── Charts data ────────────────────────────────────────────────────────────
  const enrollmentByBatch = useMemo(
    () =>
      [...allBatches]
        .sort((a, b) => b.enrolledCount - a.enrolledCount)
        .slice(0, 6)
        .map((b) => ({ batch: b.name, students: b.enrolledCount })),
    [allBatches]
  );

  const streamDistribution = useMemo(() => {
    const map = new Map<string, number>();
    for (const b of allBatches) {
      const subj = b.subject || 'Other';
      map.set(subj, (map.get(subj) ?? 0) + b.enrolledCount);
    }
    return Array.from(map.entries())
      .sort((a, b) => b[1] - a[1])
      .map(([name, value], i) => ({ name, value, color: PIE_COLORS[i % PIE_COLORS.length] }));
  }, [allBatches]);

  // ── Recent activity ────────────────────────────────────────────────────────
  const recentEvents: RecentEvent[] = useMemo(() => {
    const events: { ts: number; ev: RecentEvent }[] = [];

    rawCenters.forEach((c) => {
      events.push({
        ts: new Date(c.createdAt).getTime(),
        ev: {
          id:   `center-${c.id}`,
          type: 'enrollment',
          text: 'New center added',
          sub:  `${c.name} — ${c.city}`,
          time: timeAgo(c.createdAt),
        },
      });
    });

    allBatches.forEach((b) => {
      events.push({
        ts: new Date(b.createdAt).getTime(),
        ev: {
          id:   `batch-${b.id}`,
          type: 'schedule',
          text: 'Batch created',
          sub:  `${b.name} (${b.subject}) — ${b.enrolledCount} enrolled`,
          time: timeAgo(b.createdAt),
        },
      });
    });

    return events
      .sort((a, b) => b.ts - a.ts)
      .slice(0, 5)
      .map((e) => e.ev);
  }, [rawCenters, allBatches]);

  // ── Mutations ──────────────────────────────────────────────────────────────
  const createCenter = useMutation({
    mutationFn: (data: AddCenterForm) =>
      api.post('/api/v1/centers', {
        name:    data.name,
        code:    data.code,
        city:    data.city,
        state:   data.state,
        address: data.address,
        pincode: data.pincode,
        phone:   data.phone,
        email:   data.email,
      }),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['centers'] });
      setShowAddModal(false);
      toast.success(`Center "${vars.name}" added successfully`);
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { detail?: string } } })?.response?.data?.detail ?? 'Failed to add center';
      toast.error(msg);
    },
  });

  const updateCenter = useMutation({
    mutationFn: ({ id, data }: { id: string; data: EditCenterForm }) =>
      api.put(`/api/v1/centers/${id}`, data),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['centers'] });
      setEditingCenter(null);
      toast.success(`Center "${vars.data.name}" updated successfully`);
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { detail?: string } } })?.response?.data?.detail ?? 'Failed to update center';
      toast.error(msg);
    },
  });

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <div className="p-4 lg:p-8 space-y-6 max-w-7xl mx-auto">
      <AdvertisementBanner audience="CENTER_ADMIN" />
      <TickerBanner audience="CENTER_ADMIN" />
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">Welcome, {user?.name?.split(' ')[0] ?? 'Admin'}</h1>
          <p className="text-white/50 text-sm mt-0.5">Manage centers, batches and monitor platform health.</p>
        </div>
        <button
          onClick={() => setShowAddModal(true)}
          className="btn-primary flex items-center gap-2 px-4 py-2.5 text-sm font-medium"
        >
          <Plus className="w-4 h-4" /> Add Center
        </button>
      </div>

      {/* KPI row */}
      {centersLoading ? (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {[0, 1, 2, 3].map((i) => <Skeleton key={i} className="h-24" />)}
        </div>
      ) : (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <KpiCard
            label="Active Students"
            value={totalStudents.toLocaleString()}
            sub={`${activeCenters} centers active`}
            icon={Users}
            color="bg-brand-500/15 text-brand-400"
            delay={0.05}
          />
          <KpiCard
            label="Enrolled Batches"
            value={totalBatches}
            sub={`${centers.length} centers total`}
            icon={BookOpen}
            color="bg-emerald-500/15 text-emerald-400"
            delay={0.1}
          />
          <KpiCard
            label="Teachers"
            value={totalTeachers}
            sub="across all centers"
            icon={GraduationCap}
            color="bg-violet-500/15 text-violet-400"
            delay={0.15}
          />
          <KpiCard
            label="Enrolled Students"
            value={totalStudents.toLocaleString()}
            sub="Revenue data not available"
            icon={TrendingUp}
            color="bg-amber-500/15 text-amber-400"
            delay={0.2}
          />
        </div>
      )}

      {/* Charts row */}
      <div className="grid lg:grid-cols-5 gap-6">
        {/* Bar chart */}
        <div className="card lg:col-span-3">
          <h3 className="font-semibold text-white text-sm mb-1">Enrollment by Batch</h3>
          <p className="text-xs text-white/40 mb-4">Active students per batch stream</p>
          {centersLoading || batchQueries.some((q) => q.isLoading) ? (
            <Skeleton className="h-[180px]" />
          ) : enrollmentByBatch.length === 0 ? (
            <div className="h-[180px] flex items-center justify-center text-white/30 text-sm">No batch data yet</div>
          ) : (
            <ResponsiveContainer width="100%" height={180}>
              <BarChart data={enrollmentByBatch} margin={{ top: 0, right: 0, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.04)" vertical={false} />
                <XAxis dataKey="batch" tick={{ fill: 'rgba(255,255,255,0.3)', fontSize: 11 }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fill: 'rgba(255,255,255,0.3)', fontSize: 11 }} axisLine={false} tickLine={false} />
                <Tooltip
                  contentStyle={{ background: '#252836', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12, fontSize: 12 }}
                  cursor={{ fill: 'rgba(255,255,255,0.04)' }}
                  labelStyle={{ color: 'rgba(255,255,255,0.5)' }}
                  itemStyle={{ color: '#818cf8' }}
                />
                <Bar dataKey="students" fill="#6366f1" radius={[4, 4, 0, 0]} maxBarSize={40} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* Pie chart */}
        <div className="card lg:col-span-2">
          <h3 className="font-semibold text-white text-sm mb-1">Student Distribution</h3>
          <p className="text-xs text-white/40 mb-4">By subject stream</p>
          {centersLoading || batchQueries.some((q) => q.isLoading) ? (
            <Skeleton className="h-[180px]" />
          ) : streamDistribution.length === 0 ? (
            <div className="h-[180px] flex items-center justify-center text-white/30 text-sm">No data yet</div>
          ) : (
            <ResponsiveContainer width="100%" height={180}>
              <PieChart>
                <Pie
                  data={streamDistribution}
                  cx="50%"
                  cy="50%"
                  innerRadius={45}
                  outerRadius={70}
                  paddingAngle={3}
                  dataKey="value"
                >
                  {streamDistribution.map((entry, i) => (
                    <Cell key={i} fill={entry.color} stroke="transparent" />
                  ))}
                </Pie>
                <Tooltip
                  contentStyle={{ background: '#252836', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12, fontSize: 12 }}
                  itemStyle={{ color: 'rgba(255,255,255,0.7)' }}
                />
                <Legend
                  iconType="circle"
                  iconSize={8}
                  formatter={(value) => <span style={{ color: 'rgba(255,255,255,0.5)', fontSize: 11 }}>{value}</span>}
                />
              </PieChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>

      {/* Centers table + Recent activity */}
      <div className="grid lg:grid-cols-3 gap-6">
        {/* Centers table */}
        <div className="card lg:col-span-2">
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-semibold text-white text-sm">Coaching Centers</h3>
            <span className="text-xs text-white/30">{centers.length} centers</span>
          </div>

          {centersLoading ? (
            <div className="space-y-3">
              {[0, 1, 2].map((i) => <Skeleton key={i} className="h-12" />)}
            </div>
          ) : centers.length === 0 ? (
            <div className="py-12 text-center text-white/30 text-sm">No centers found. Add your first center.</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-xs text-white/30 uppercase tracking-wider border-b border-white/5">
                    <th className="pb-2 pr-4">Center</th>
                    <th className="pb-2 pr-4">Students</th>
                    <th className="pb-2 pr-4">Batches</th>
                    <th className="pb-2 pr-4">Status</th>
                    <th className="pb-2">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5">
                  {centers.map((center) => {
                    const StatusIcon = statusIcons[center.status];
                    return (
                      <tr key={center.id} className="hover:bg-white/3 transition-colors group">
                        <td className="py-3 pr-4">
                          <div className="font-medium text-white text-sm">{center.name}</div>
                          <div className="text-xs text-white/40 mt-0.5 flex items-center gap-1">
                            <MapPin className="w-3 h-3" /> {center.city}
                          </div>
                        </td>
                        <td className="py-3 pr-4 text-white/70 font-medium">{center.students}</td>
                        <td className="py-3 pr-4 text-white/70">{center.batches}</td>
                        <td className="py-3 pr-4">
                          <span className={cn('inline-flex items-center gap-1 badge text-xs', statusColors[center.status])}>
                            <StatusIcon className="w-2.5 h-2.5" />
                            {center.status}
                          </span>
                        </td>
                        <td className="py-3">
                          <div className="flex items-center gap-1">
                            <button
                              onClick={() => toast.success(`Viewing ${center.name}`)}
                              className="p-1.5 rounded-lg hover:bg-white/5 text-white/30 hover:text-white/70 transition-colors"
                              title="View"
                            >
                              <Eye className="w-3.5 h-3.5" />
                            </button>
                            <button
                              onClick={() => setEditingCenter(center as CenterRow & { email?: string; state?: string; pincode?: string })}
                              className="p-1.5 rounded-lg hover:bg-white/5 text-white/30 hover:text-white/70 transition-colors"
                              title="Edit"
                            >
                              <Edit2 className="w-3.5 h-3.5" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Recent activity */}
        <div className="card">
          <h3 className="font-semibold text-white text-sm mb-4">Recent Activity</h3>
          {centersLoading ? (
            <div className="space-y-3">
              {[0, 1, 2, 3, 4].map((i) => <Skeleton key={i} className="h-14" />)}
            </div>
          ) : recentEvents.length === 0 ? (
            <div className="text-white/30 text-sm text-center py-8">No recent activity</div>
          ) : (
            <div className="space-y-3">
              {recentEvents.map((ev) => {
                const { color, bg } = eventColors[ev.type];
                const Icon = eventIcons[ev.type];
                return (
                  <div key={ev.id} className="flex items-start gap-3">
                    <div className={cn('p-1.5 rounded-lg flex-shrink-0 mt-0.5', bg)}>
                      <Icon className={cn('w-3.5 h-3.5', color)} />
                    </div>
                    <div className="min-w-0 flex-1">
                      <div className="text-xs font-semibold text-white/80">{ev.text}</div>
                      <div className="text-xs text-white/40 mt-0.5 leading-relaxed">{ev.sub}</div>
                      <div className="text-[10px] text-white/25 mt-1">{ev.time}</div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {/* Add center modal */}
      <AnimatePresence>
        {showAddModal && (
          <AddCenterModal
            onClose={() => setShowAddModal(false)}
            onAdd={(data) => createCenter.mutate(data)}
            isSubmitting={createCenter.isPending}
          />
        )}
      </AnimatePresence>

      {/* Edit center modal */}
      <AnimatePresence>
        {editingCenter && (
          <EditCenterModal
            center={editingCenter}
            onClose={() => setEditingCenter(null)}
            onSave={(data) => updateCenter.mutate({ id: editingCenter.id, data })}
            isSubmitting={updateCenter.isPending}
          />
        )}
      </AnimatePresence>

      <FooterBanner audience="CENTER_ADMIN" />
    </div>
  );
}
