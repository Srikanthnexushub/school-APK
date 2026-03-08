import { useState } from 'react';
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
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { cn } from '../../lib/utils';
import { toast } from 'sonner';

// ─── Mock data ─────────────────────────────────────────────────────────────

interface Center {
  id: string;
  name: string;
  city: string;
  address: string;
  phone: string;
  students: number;
  batches: number;
  status: 'Active' | 'Inactive' | 'Pending';
}

const initialCenters: Center[] = [
  { id: 'ct1', name: 'NexusEd Koramangala', city: 'Bengaluru', address: '5th Block, Koramangala', phone: '+91 98765 43210', students: 142, batches: 8, status: 'Active' },
  { id: 'ct2', name: 'NexusEd Indiranagar',  city: 'Bengaluru', address: '100 Feet Road, Indiranagar', phone: '+91 98765 43211', students: 98, batches: 5, status: 'Active' },
  { id: 'ct3', name: 'NexusEd Andheri',      city: 'Mumbai',    address: 'Veera Desai Rd, Andheri W', phone: '+91 98765 43212', students: 211, batches: 11, status: 'Active' },
  { id: 'ct4', name: 'NexusEd Powai',        city: 'Mumbai',    address: 'Hiranandani Gardens, Powai', phone: '+91 98765 43213', students: 67,  batches: 4, status: 'Pending' },
  { id: 'ct5', name: 'NexusEd Banjara Hills', city: 'Hyderabad', address: 'Road No. 12, Banjara Hills', phone: '+91 98765 43214', students: 0,   batches: 0, status: 'Inactive' },
];

const enrollmentByBatch = [
  { batch: 'JEE 26', students: 84 },
  { batch: 'NEET 26', students: 62 },
  { batch: 'JEE 27', students: 41 },
  { batch: 'Foundation', students: 28 },
  { batch: 'Board Prep', students: 55 },
  { batch: 'MHT-CET', students: 38 },
];

const streamDistribution = [
  { name: 'Engineering', value: 185, color: '#6366f1' },
  { name: 'Medical', value: 120, color: '#34d399' },
  { name: 'Foundation', value: 69,  color: '#fbbf24' },
  { name: 'Commerce',   value: 44,  color: '#f472b6' },
  { name: 'Other',      value: 30,  color: '#60a5fa' },
];

interface RecentEvent {
  id: string;
  type: 'enrollment' | 'payment' | 'schedule' | 'teacher';
  text: string;
  sub: string;
  time: string;
}

const recentEvents: RecentEvent[] = [
  { id: 'e1', type: 'enrollment', text: 'New enrollment', sub: 'Riya Kapoor joined JEE 2026 Batch at Koramangala', time: '10m ago' },
  { id: 'e2', type: 'payment',    text: 'Fee received',   sub: '₹4,500 from Arjun Mehta — Andheri Center', time: '25m ago' },
  { id: 'e3', type: 'schedule',   text: 'Schedule change', sub: 'Physics batch rescheduled to 5 PM — Indiranagar', time: '1h ago' },
  { id: 'e4', type: 'teacher',    text: 'Teacher assigned', sub: 'Dr. Priya Nair assigned to NEET 2026 — Andheri', time: '2h ago' },
  { id: 'e5', type: 'enrollment', text: 'New enrollment', sub: 'Varun Singh joined Foundation Batch at Powai', time: '3h ago' },
];

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

const statusColors: Record<Center['status'], string> = {
  Active:   'bg-emerald-500/15 text-emerald-400',
  Inactive: 'bg-white/5 text-white/30',
  Pending:  'bg-amber-500/15 text-amber-400',
};

const statusIcons: Record<Center['status'], React.ElementType> = {
  Active:   CheckCircle2,
  Inactive: X,
  Pending:  AlertTriangle,
};

// ─── Add center form schema ───────────────────────────────────────────────────

const addCenterSchema = z.object({
  name:    z.string().min(3, 'Name must be at least 3 characters'),
  city:    z.string().min(2, 'City is required'),
  address: z.string().min(5, 'Address is required'),
  phone:   z.string().regex(/^\+?[\d\s-]{10,}$/, 'Invalid phone number'),
});
type AddCenterForm = z.infer<typeof addCenterSchema>;

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

function AddCenterModal({ onClose, onAdd }: { onClose: () => void; onAdd: (data: AddCenterForm) => void }) {
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
              <label className="block text-xs font-medium text-white/60 mb-1.5">City</label>
              <div className="relative">
                <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
                <input {...register('city')} placeholder="Bengaluru" className="input pl-10 w-full" />
              </div>
              {errors.city && <p className="text-xs text-red-400 mt-1">{errors.city.message}</p>}
            </div>

            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Address</label>
              <input {...register('address')} placeholder="Full address" className="input w-full" />
              {errors.address && <p className="text-xs text-red-400 mt-1">{errors.address.message}</p>}
            </div>

            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Phone</label>
              <div className="relative">
                <Phone className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
                <input {...register('phone')} placeholder="+91 98765 43210" className="input pl-10 w-full" />
              </div>
              {errors.phone && <p className="text-xs text-red-400 mt-1">{errors.phone.message}</p>}
            </div>

            <div className="flex gap-3 pt-2">
              <button type="button" onClick={onClose} className="flex-1 py-2.5 rounded-xl border border-white/10 text-sm font-medium text-white/60 hover:text-white hover:border-white/20 transition-colors">
                Cancel
              </button>
              <button type="submit" className="flex-1 btn-primary py-2.5 text-sm font-medium">
                Add Center
              </button>
            </div>
          </form>
        </div>
      </motion.div>
    </>
  );
}

// ─── Main page ────────────────────────────────────────────────────────────────

export default function AdminDashboardPage() {
  const [centers, setCenters] = useState<Center[]>(initialCenters);
  const [showAddModal, setShowAddModal] = useState(false);

  const totalStudents = centers.reduce((s, c) => s + c.students, 0);
  const totalBatches  = centers.reduce((s, c) => s + c.batches, 0);
  const activeCenters = centers.filter((c) => c.status === 'Active').length;

  function handleAddCenter(data: AddCenterForm) {
    const newCenter: Center = {
      id: `ct${Date.now()}`,
      name: data.name,
      city: data.city,
      address: data.address,
      phone: data.phone,
      students: 0,
      batches: 0,
      status: 'Pending',
    };
    setCenters((prev) => [newCenter, ...prev]);
    setShowAddModal(false);
    toast.success(`Center "${data.name}" added successfully`);
  }

  return (
    <div className="p-4 lg:p-8 space-y-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">Admin Overview</h1>
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
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <KpiCard label="Active Students"  value={totalStudents.toLocaleString()} sub="+12 this week"        icon={Users}         color="bg-brand-500/15 text-brand-400"   delay={0.05} />
        <KpiCard label="Enrolled Batches" value={totalBatches}                   sub={`${activeCenters} centers active`} icon={BookOpen}       color="bg-emerald-500/15 text-emerald-400" delay={0.1} />
        <KpiCard label="Teachers"         value={48}                             sub="across all centers"   icon={GraduationCap} color="bg-violet-500/15 text-violet-400"  delay={0.15} />
        <KpiCard label="Monthly Revenue"  value="₹18.4L"                        sub="+8% vs last month"    icon={TrendingUp}    color="bg-amber-500/15 text-amber-400"   delay={0.2} />
      </div>

      {/* Charts row */}
      <div className="grid lg:grid-cols-5 gap-6">
        {/* Bar chart */}
        <div className="card lg:col-span-3">
          <h3 className="font-semibold text-white text-sm mb-1">Enrollment by Batch</h3>
          <p className="text-xs text-white/40 mb-4">Active students per batch stream</p>
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
        </div>

        {/* Pie chart */}
        <div className="card lg:col-span-2">
          <h3 className="font-semibold text-white text-sm mb-1">Student Distribution</h3>
          <p className="text-xs text-white/40 mb-4">By subject stream</p>
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
                            onClick={() => toast.success(`Editing ${center.name}`)}
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
        </div>

        {/* Recent activity */}
        <div className="card">
          <h3 className="font-semibold text-white text-sm mb-4">Recent Activity</h3>
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
        </div>
      </div>

      {/* Add center modal */}
      <AnimatePresence>
        {showAddModal && (
          <AddCenterModal onClose={() => setShowAddModal(false)} onAdd={handleAddCenter} />
        )}
      </AnimatePresence>
    </div>
  );
}
