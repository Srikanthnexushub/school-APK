import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import {
  User, Phone, Mail, MapPin, CheckCircle2, Loader2, Edit2, Save, X,
} from 'lucide-react';
import { cn } from '../../lib/utils';
import { Avatar } from '../../components/ui/Avatar';
import { toast } from 'sonner';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';

// ─── Types ────────────────────────────────────────────────────────────────────

interface ParentProfileResponse {
  id: string;
  userId: string;
  name: string;
  phone: string;
  email?: string;
  gender?: string;
  relationshipType?: string;
  address?: string;
  city?: string;
  state?: string;
  pincode?: string;
  verified: boolean;
  status: string;
  createdAt: string;
}

interface UpdateProfileRequest {
  name: string;
  phone: string;
  email: string;
  gender: string;
  relationshipType: string;
  address: string;
  city: string;
  state: string;
  pincode: string;
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

function formatDate(iso: string): string {
  if (!iso) return '—';
  try {
    return new Date(iso).toLocaleDateString('en-IN', {
      day: '2-digit', month: 'long', year: 'numeric',
    });
  } catch {
    return iso;
  }
}

// ─── Field row ────────────────────────────────────────────────────────────────

function InfoRow({ label, value, icon: Icon }: { label: string; value: string; icon: React.ElementType }) {
  return (
    <div className="flex items-start gap-3 py-3 border-b border-white/5 last:border-0">
      <div className="p-2 rounded-lg bg-brand-500/10 flex-shrink-0 mt-0.5">
        <Icon className="w-4 h-4 text-brand-400" />
      </div>
      <div className="min-w-0 flex-1">
        <div className="text-xs text-white/40 uppercase tracking-wider font-medium mb-0.5">{label}</div>
        <div className="text-sm text-white font-medium">{value || '—'}</div>
      </div>
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────

export default function ParentProfilePage() {
  const queryClient = useQueryClient();
  const updateUser = useAuthStore((s) => s.updateUser);
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState<UpdateProfileRequest>({
    name: '', phone: '', email: '', gender: '', relationshipType: 'PARENT',
    address: '', city: '', state: '', pincode: '',
  });

  const { data: profile, isLoading } = useQuery<ParentProfileResponse>({
    queryKey: ['parent-profile'],
    queryFn: () => api.get('/api/v1/parents/me').then((r) => r.data),
  });

  function startEdit() {
    if (!profile) return;
    setForm({
      name: profile.name ?? '',
      phone: profile.phone ?? '',
      email: profile.email ?? '',
      gender: profile.gender ?? '',
      relationshipType: profile.relationshipType ?? 'PARENT',
      address: profile.address ?? '',
      city: profile.city ?? '',
      state: profile.state ?? '',
      pincode: profile.pincode ?? '',
    });
    setEditing(true);
  }

  const updateMutation = useMutation({
    mutationFn: async (data: UpdateProfileRequest) => {
      const parts = data.name.trim().split(/\s+/);
      const firstName = parts[0] ?? '';
      const lastName = parts.slice(1).join(' ') || firstName;
      await Promise.all([
        api.put(`/api/v1/parents/${profile!.id}`, data),
        api.patch('/api/v1/auth/me', { firstName, lastName }),
      ]);
      return data.name.trim();
    },
    onSuccess: (name) => {
      updateUser({ name });
      toast.success('Profile updated successfully.');
      queryClient.invalidateQueries({ queryKey: ['parent-profile'] });
      setEditing(false);
    },
    onError: (e: unknown) => {
      const err = e as { response?: { data?: { message?: string } }; message?: string };
      toast.error(err?.response?.data?.message ?? err?.message ?? 'Failed to update profile.');
    },
  });

  function field(key: keyof UpdateProfileRequest) {
    return {
      value: form[key],
      onChange: (ev: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
        setForm((p) => ({ ...p, [key]: ev.target.value })),
    };
  }

  if (isLoading) {
    return (
      <div className="p-4 lg:p-8 max-w-3xl mx-auto space-y-6">
        <div className="h-7 bg-white/10 rounded w-40 animate-pulse" />
        <div className="card animate-pulse space-y-4">
          <div className="h-16 bg-white/10 rounded w-16 mx-auto rounded-full" />
          <div className="h-4 bg-white/10 rounded w-1/2 mx-auto" />
          <div className="h-3 bg-white/10 rounded w-1/3 mx-auto" />
        </div>
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="p-4 lg:p-8 max-w-3xl mx-auto">
        <div className="card text-center py-12">
          <User className="w-10 h-10 text-white/20 mx-auto mb-3" />
          <p className="text-white/40 text-sm">Could not load profile. Please try again.</p>
        </div>
      </div>
    );
  }

  const profileFields = [profile.name, profile.phone, profile.email, profile.gender, profile.relationshipType, profile.address, profile.city, profile.state, profile.pincode];
  const profilePct = Math.round((profileFields.filter(Boolean).length / profileFields.length) * 100);

  return (
    <div className="p-4 lg:p-8 max-w-3xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">My Profile</h1>
          <p className="text-white/50 text-sm mt-0.5">Manage your personal information.</p>
        </div>
        {!editing && (
          <button
            onClick={startEdit}
            className="btn-primary flex items-center gap-2 px-4 py-2.5 text-sm font-medium"
          >
            <Edit2 className="w-4 h-4" />
            Edit Profile
          </button>
        )}
      </div>

      {/* Profile completion */}
      <div className="card">
        <div className="flex items-center justify-between mb-2">
          <span className="text-sm text-white/70 font-medium">Profile Completion</span>
          <span className={cn('text-sm font-bold', profilePct === 100 ? 'text-emerald-400' : profilePct >= 50 ? 'text-amber-400' : 'text-red-400')}>
            {profilePct}%
          </span>
        </div>
        <div className="h-2 bg-surface-200 rounded-full overflow-hidden">
          <div
            className={cn('h-full rounded-full transition-all duration-500', profilePct === 100 ? 'bg-emerald-500' : profilePct >= 50 ? 'bg-amber-500' : 'bg-red-500')}
            style={{ width: `${profilePct}%` }}
          />
        </div>
        {profilePct < 100 && (
          <p className="text-xs text-white/30 mt-1.5">
            {profileFields.filter(Boolean).length}/{profileFields.length} fields filled — click Edit Profile to complete
          </p>
        )}
      </div>

      {/* Profile card */}
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        className="card"
      >
        {/* Avatar + name section */}
        <div className="flex flex-col sm:flex-row items-center sm:items-start gap-5 pb-6 mb-2 border-b border-white/5">
          <div className="relative flex-shrink-0">
            <Avatar name={profile.name} size="lg" />
            {profile.verified && (
              <div className="absolute -bottom-1 -right-1 w-6 h-6 rounded-full bg-emerald-500 border-2 border-surface flex items-center justify-center" title="Verified">
                <CheckCircle2 className="w-3.5 h-3.5 text-white" />
              </div>
            )}
          </div>
          <div className="text-center sm:text-left">
            <div className="flex items-center justify-center sm:justify-start gap-2 flex-wrap">
              <h2 className="text-xl font-bold text-white">{profile.name}</h2>
              {profile.verified && (
                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-emerald-500/15 text-emerald-400">
                  <CheckCircle2 className="w-3 h-3" />
                  Verified
                </span>
              )}
            </div>
            <p className="text-white/40 text-sm mt-0.5">
              {profile.relationshipType ?? 'Parent'} · Member since {formatDate(profile.createdAt)}
            </p>
            <div className={cn(
              'inline-flex mt-2 text-xs px-2.5 py-1 rounded-lg font-medium',
              profile.status === 'ACTIVE' ? 'bg-emerald-500/15 text-emerald-400' : 'bg-amber-500/15 text-amber-400'
            )}>
              {profile.status}
            </div>
          </div>
        </div>

        {/* View mode */}
        {!editing && (
          <div className="space-y-0">
            <InfoRow label="Full Name" value={profile.name} icon={User} />
            <InfoRow label="Phone Number" value={profile.phone} icon={Phone} />
            <InfoRow label="Email Address" value={profile.email ?? ''} icon={Mail} />
            <InfoRow label="Gender" value={profile.gender ?? ''} icon={User} />
            <InfoRow label="Relationship" value={profile.relationshipType ?? ''} icon={User} />
            <InfoRow label="Address" value={profile.address ?? ''} icon={MapPin} />
            <InfoRow label="City" value={profile.city ?? ''} icon={MapPin} />
            <InfoRow label="State" value={profile.state ?? ''} icon={MapPin} />
            <InfoRow label="Pincode" value={profile.pincode ?? ''} icon={MapPin} />
          </div>
        )}

        {/* Edit mode */}
        {editing && (
          <motion.form
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            onSubmit={(e) => { e.preventDefault(); updateMutation.mutate(form); }}
            className="space-y-4"
          >
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">Full Name</label>
                <input {...field('name')} placeholder="Your full name" className="input w-full" required />
              </div>
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">Phone Number</label>
                <input {...field('phone')} placeholder="+91 98765 43210" className="input w-full" />
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">Email Address</label>
                <input {...field('email')} type="email" placeholder="you@example.com" className="input w-full" />
              </div>
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">Gender</label>
                <select {...field('gender')} className="input w-full">
                  <option value="">— Select —</option>
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                  <option value="OTHER">Other</option>
                  <option value="PREFER_NOT_TO_SAY">Prefer not to say</option>
                </select>
              </div>
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">Relationship Type</label>
                <select {...field('relationshipType')} className="input w-full">
                  <option value="FATHER">Father</option>
                  <option value="MOTHER">Mother</option>
                  <option value="GUARDIAN">Guardian</option>
                  <option value="OTHER">Other</option>
                  <option value="PARENT">Parent</option>
                </select>
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Address</label>
              <input {...field('address')} placeholder="Street address" className="input w-full" />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">City</label>
                <input {...field('city')} placeholder="Mumbai" className="input w-full" />
              </div>
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">State</label>
                <input {...field('state')} placeholder="Maharashtra" className="input w-full" />
              </div>
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">Pincode</label>
                <input {...field('pincode')} placeholder="400001" className="input w-full" />
              </div>
            </div>

            <div className="flex gap-3 pt-2">
              <button
                type="button"
                onClick={() => setEditing(false)}
                className="flex items-center gap-2 px-4 py-2.5 rounded-xl border border-white/10 text-sm text-white/60 hover:text-white hover:border-white/20 transition-colors"
              >
                <X className="w-4 h-4" />
                Cancel
              </button>
              <button
                type="submit"
                disabled={updateMutation.isPending}
                className="btn-primary flex items-center gap-2 px-5 py-2.5 text-sm font-medium disabled:opacity-50"
              >
                {updateMutation.isPending
                  ? <Loader2 className="w-4 h-4 animate-spin" />
                  : <Save className="w-4 h-4" />}
                {updateMutation.isPending ? 'Saving…' : 'Save Changes'}
              </button>
            </div>
          </motion.form>
        )}
      </motion.div>
    </div>
  );
}
