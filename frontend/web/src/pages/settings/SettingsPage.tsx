import { useState, useRef, useEffect, useCallback } from 'react';
import axios from 'axios';
import { QRCodeSVG } from 'qrcode.react';
import { motion, AnimatePresence } from 'framer-motion';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  User, Bell, Palette, Shield, Camera, Check, AlertTriangle,
  Smartphone, Monitor, Eye, EyeOff, GraduationCap, Calendar, MapPin, BookOpen,
  Plus, Loader2, Search, UserPlus, X,
} from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { useThemeStore } from '../../stores/themeStore';
import { cn } from '../../lib/utils';
import { Toggle } from '../../components/ui/Toggle';
import { Avatar } from '../../components/ui/Avatar';
import { Modal } from '../../components/ui/Modal';
import { suggestStates, suggestDistricts } from '../../utils/indiaLocations';
import CaptchaWidget from '../../components/CaptchaWidget';
import { ROLE_COLORS } from '../../lib/navConfig';

type Tab = 'profile' | 'notifications' | 'appearance' | 'security';

const TABS: { key: Tab; label: string; icon: React.ReactNode }[] = [
  { key: 'profile', label: 'Profile', icon: <User className="w-4 h-4" /> },
  { key: 'notifications', label: 'Notifications', icon: <Bell className="w-4 h-4" /> },
  { key: 'appearance', label: 'Appearance', icon: <Palette className="w-4 h-4" /> },
  { key: 'security', label: 'Security', icon: <Shield className="w-4 h-4" /> },
];

const ACCENT_COLORS = [
  { name: 'cyan',    hex: '#00f5ff', label: 'Neon Cyan'    },
  { name: 'magenta', hex: '#f000ff', label: 'Neon Magenta' },
  { name: 'lime',    hex: '#00ff88', label: 'Neon Lime'    },
  { name: 'gold',    hex: '#ffd700', label: 'Neon Gold'    },
  { name: 'violet',  hex: '#9b00ff', label: 'Neon Violet'  },
  { name: 'rose',    hex: '#ff006e', label: 'Neon Rose'    },
];

function applyAccent(name: string) {
  document.documentElement.setAttribute('data-accent', name);
}

const LS_NOTIF_KEY = 'edutech:notif_prefs';
const LS_ACCENT_KEY = 'edutech:accent_color';

function loadNotifPrefs() {
  try {
    const raw = localStorage.getItem(LS_NOTIF_KEY);
    if (raw) return JSON.parse(raw);
  } catch {
    // ignore
  }
  return {
    emailNotifications: true,
    pushNotifications: true,
    weeklyReport: true,
    newMentorAvailable: false,
    examReminders: true,
    aiRecommendations: true,
  };
}

function loadAccentColor(): string {
  try {
    const saved = localStorage.getItem(LS_ACCENT_KEY) ?? 'cyan';
    // Migrate old values to new neon palette
    if (!ACCENT_COLORS.some(c => c.name === saved)) return 'cyan';
    return saved;
  } catch {
    return 'cyan';
  }
}

interface StudentProfile {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  gender?: string;
  dateOfBirth?: string;
  city?: string;
  state?: string;
  district?: string;
  country?: string;
  board?: string;
  currentClass?: number;
  stream?: string;
  targetYear?: number;
  status: string;
  createdAt: string;
  parentLinkCode?: string;
}

interface ParentProfileMin {
  name?: string;
  phone?: string;
  email?: string;
  gender?: string;
  relationshipType?: string;
  address?: string;
  city?: string;
  state?: string;
  district?: string;
  country?: string;
  pincode?: string;
}

interface MentorProfileMin {
  id?: string;
  fullName?: string;
  email?: string;
  bio?: string;
  specializations?: string | string[];
  yearsOfExperience?: number;
  hourlyRate?: number;
  gender?: string;
  district?: string;
}

const profileSchema = z.object({
  name: z.string().min(2, 'Name must be at least 2 characters'),
  phone: z.string().optional(),
  gender: z.string().optional(),
  city: z.string().optional(),
  state: z.string().optional(),
  district: z.string().optional(),
  country: z.string().optional(),
  stream: z.string().optional(),
  targetYear: z.number().int().min(2024).max(2035).optional().or(z.nan()).transform(v => isNaN(v as number) ? undefined : v),
});

const passwordSchema = z.object({
  currentPassword: z.string().min(6, 'Required'),
  newPassword: z.string().min(8, 'At least 8 characters'),
  confirmPassword: z.string(),
}).refine((d) => d.newPassword === d.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
});

type ProfileForm = z.infer<typeof profileSchema>;
type PasswordForm = z.infer<typeof passwordSchema>;

function LocationInput({
  label, value, onChange, suggestions,
}: {
  label: string; value: string;
  onChange: (v: string) => void;
  suggestions: string[];
}) {
  const [show, setShow] = useState(false);
  const filtered = suggestions.filter(s => s.toLowerCase().includes(value.toLowerCase())).slice(0, 8);
  return (
    <div className="relative">
      <label className="block text-sm font-medium text-white/70 mb-1.5">{label}</label>
      <input
        value={value}
        onChange={e => { onChange(e.target.value); setShow(true); }}
        onFocus={() => setShow(true)}
        onBlur={() => setTimeout(() => setShow(false), 150)}
        placeholder={label}
        className="input w-full"
      />
      {show && filtered.length > 0 && value.length > 0 && (
        <div className="absolute z-50 mt-1 w-full bg-surface-100 border border-white/10 rounded-xl shadow-xl max-h-48 overflow-y-auto">
          {filtered.map(s => (
            <button
              key={s}
              type="button"
              onMouseDown={() => { onChange(s); setShow(false); }}
              className="w-full text-left px-3 py-2 text-sm text-white/80 hover:bg-white/5 hover:text-white transition-colors"
            >
              {s}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function InfoBadge({ label, value, icon: Icon }: { label: string; value?: string | number | null; icon: React.ElementType }) {
  return (
    <div className="flex items-start gap-3 py-3 border-b border-white/5 last:border-0">
      <div className="p-2 rounded-lg bg-brand-500/10 flex-shrink-0 mt-0.5">
        <Icon className="w-4 h-4 text-brand-400" />
      </div>
      <div>
        <div className="text-xs text-white/40 uppercase tracking-wider font-medium mb-0.5">{label}</div>
        <div className="text-sm text-white font-medium">{value || '—'}</div>
      </div>
    </div>
  );
}

function formatDob(dob?: string): string {
  if (!dob) return '—';
  try {
    return new Date(dob).toLocaleDateString('en-IN', { day: '2-digit', month: 'long', year: 'numeric' });
  } catch { return dob; }
}

function ProfileTab() {
  const user = useAuthStore((s) => s.user);
  const updateUser = useAuthStore((s) => s.updateUser);
  const queryClient = useQueryClient();
  const avatarInputRef = useRef<HTMLInputElement>(null);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState('');
  const [showAddParentModal, setShowAddParentModal] = useState(false);
  const [addParentEmail, setAddParentEmail] = useState('');
  const [addParentSending, setAddParentSending] = useState(false);
  const [addParentPhase, setAddParentPhase] = useState<'choose' | 'search' | 'register'>('choose');
  const [addParentRegForm, setAddParentRegForm] = useState({
    firstName: '', lastName: '', phone: '', gender: '', relationship: 'MOTHER', password: '', confirmPassword: '',
  });
  const [addParentCaptcha, setAddParentCaptcha] = useState<string | null>(null);
  const [addParentRegistering, setAddParentRegistering] = useState(false);
  const [addParentShowPw, setAddParentShowPw] = useState(false);
  const handleAddParentCaptcha = useCallback((token: string | null) => setAddParentCaptcha(token), []);

  function resetAddParentModal() {
    setAddParentEmail('');
    setAddParentPhase('choose');
    setAddParentRegForm({ firstName: '', lastName: '', phone: '', gender: '', relationship: 'MOTHER', password: '', confirmPassword: '' });
    setAddParentCaptcha(null);
    setAddParentShowPw(false);
  }

  async function handleAddParent() {
    if (!addParentEmail.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(addParentEmail)) {
      toast.error('Enter a valid parent email');
      return;
    }
    setAddParentSending(true);
    try {
      await api.post('/api/v1/parents/request-link', { parentEmail: addParentEmail.trim() });
      toast.success('Linked! Your parent will be notified.');
      setShowAddParentModal(false);
      resetAddParentModal();
    } catch (e: unknown) {
      const err = e as { response?: { status?: number; data?: { detail?: string } } };
      if (err?.response?.status === 404) {
        // Parent not registered — switch to registration form
        setAddParentPhase('register');
      } else {
        toast.error(err?.response?.data?.detail ?? 'Failed to find parent. Please try again.');
      }
    } finally {
      setAddParentSending(false);
    }
  }

  async function handleRegisterParent() {
    const { firstName, lastName, phone, gender, relationship, password, confirmPassword } = addParentRegForm;
    if (!firstName.trim()) { toast.error('First name is required'); return; }
    if (!lastName.trim()) { toast.error('Last name is required'); return; }
    if (!relationship) { toast.error('Relationship is required'); return; }
    if (password.length < 8 || !/[A-Z]/.test(password) || !/[0-9]/.test(password) || !/[^A-Za-z0-9]/.test(password)) {
      toast.error('Password must be 8+ chars with uppercase, digit, and special character'); return;
    }
    if (password !== confirmPassword) { toast.error('Passwords do not match'); return; }
    if (!addParentCaptcha) { toast.error('Please complete the captcha'); return; }

    setAddParentRegistering(true);
    try {
      // 1. Create parent account
      const regRes = await api.post('/api/v1/auth/register', {
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: addParentEmail.trim(),
        password,
        role: 'PARENT',
        captchaToken: addParentCaptcha,
        deviceFingerprint: { userAgent: navigator.userAgent, deviceId: crypto.randomUUID(), ipSubnet: '127.0.0' },
      });
      const parentJwt = regRes.data.accessToken;

      // 2. Create parent profile
      await axios.post('/api/v1/parents', {
        name: `${firstName.trim()} ${lastName.trim()}`,
        phone: phone || undefined,
        email: addParentEmail.trim(),
        gender: gender || undefined,
      }, { headers: { Authorization: `Bearer ${parentJwt}` } });

      // 3. Link from student side
      await api.post('/api/v1/parents/request-link', { parentEmail: addParentEmail.trim() });

      toast.success(`Parent account created and linked!`);
      setShowAddParentModal(false);
      resetAddParentModal();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { detail?: string } } };
      toast.error(err?.response?.data?.detail ?? 'Failed to create parent account');
    } finally {
      setAddParentRegistering(false);
    }
  }

  const { data: profile } = useQuery<StudentProfile>({
    queryKey: ['student-profile-me'],
    queryFn: () => api.get('/api/v1/students/me').then((r) => r.data),
    enabled: !!user && user.role === 'STUDENT',
    retry: false,
    throwOnError: false,
  });

  const { data: parentProfile } = useQuery<ParentProfileMin>({
    queryKey: ['parent-profile'],
    queryFn: () => api.get('/api/v1/parents/me').then((r) => r.data),
    enabled: !!user && user.role === 'PARENT',
    retry: false,
    throwOnError: false,
  });

  const { data: mentorProfile } = useQuery<MentorProfileMin>({
    queryKey: ['mentor-profile-me'],
    queryFn: () => api.get('/api/v1/mentors/me').then((r) => r.data),
    enabled: !!user && user.role === 'TEACHER',
    retry: false,
    throwOnError: false,
  });

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<ProfileForm>({
    resolver: zodResolver(profileSchema),
    defaultValues: { name: user?.name ?? '', phone: '', gender: '', city: '', state: '', district: '', country: '', stream: '', targetYear: undefined },
    values: profile ? {
      name: user?.name ?? '',
      phone: profile.phone ?? '',
      gender: profile.gender ?? '',
      city: profile.city ?? '',
      state: profile.state ?? '',
      district: profile.district ?? '',
      country: profile.country ?? '',
      stream: profile.stream ?? '',
      targetYear: profile.targetYear,
    } : undefined,
    resetOptions: { keepDirtyValues: true },
  });

  const saveMutation = useMutation({
    mutationFn: async (data: ProfileForm) => {
      const parts = data.name.trim().split(/\s+/);
      const firstName = parts[0] ?? '';
      const lastName = parts.slice(1).join(' ') || firstName;
      if (!profile) {
        // No profile exists yet — create it (POST) instead of update (PATCH)
        await api.post('/api/v1/students', {
          userId: user!.id,
          email: user!.email,
          firstName,
          lastName,
          dateOfBirth: '2000-01-01', // placeholder — student can update later
          phone: data.phone || undefined,
          gender: data.gender || undefined,
          city: data.city || undefined,
          state: data.state || undefined,
          district: data.district || undefined,
          country: data.country || undefined,
          stream: data.stream || undefined,
          targetYear: data.targetYear || undefined,
        });
      } else {
        await api.patch('/api/v1/students/me', {
          firstName,
          lastName,
          phone: data.phone || undefined,
          gender: data.gender || undefined,
          city: data.city || undefined,
          state: data.state || undefined,
          district: data.district || undefined,
          country: data.country || undefined,
          stream: data.stream || undefined,
          targetYear: data.targetYear || undefined,
        });
      }
      await api.patch('/api/v1/auth/me', { firstName, lastName });
      return data.name.trim();
    },
    onSuccess: (name) => {
      updateUser({ name });
      queryClient.invalidateQueries({ queryKey: ['student-profile-me'] });
      toast.success('Profile updated successfully!');
    },
    onError: () => toast.error('Failed to save profile.'),
  });

  // Parent form state
  const [parentForm, setParentForm] = useState({
    name: '', phone: '', gender: '', address: '', city: '', state: '', district: '', country: '', pincode: '',
  });
  const parentFormInitialized = useRef(false);
  useEffect(() => {
    if (parentProfile && !parentFormInitialized.current) {
      setParentForm({
        name: parentProfile.name ?? '',
        phone: parentProfile.phone ?? '',
        gender: parentProfile.gender ?? '',
        address: parentProfile.address ?? '',
        city: parentProfile.city ?? '',
        state: parentProfile.state ?? '',
        district: parentProfile.district ?? '',
        country: parentProfile.country ?? '',
        pincode: parentProfile.pincode ?? '',
      });
      parentFormInitialized.current = true;
    }
  }, [parentProfile]);

  const parentSaveMutation = useMutation({
    mutationFn: async (form: typeof parentForm) => {
      const payload = {
        name: form.name || undefined,
        phone: form.phone || undefined,
        gender: form.gender || undefined,
        address: form.address || undefined,
        city: form.city || undefined,
        state: form.state || undefined,
        district: form.district || undefined,
        country: form.country || undefined,
        pincode: form.pincode || undefined,
      };
      if (!parentProfile) {
        await api.post('/api/v1/parents', { ...payload, email: user!.email });
      } else {
        await api.patch('/api/v1/parents/me', payload);
      }
      if (form.name) {
        const parts = form.name.trim().split(/\s+/);
        await api.patch('/api/v1/auth/me', { firstName: parts[0], lastName: parts.slice(1).join(' ') || parts[0] });
      }
    },
    onSuccess: () => {
      if (parentForm.name) updateUser({ name: parentForm.name });
      queryClient.invalidateQueries({ queryKey: ['parent-profile'] });
      toast.success('Profile updated successfully!');
    },
    onError: () => toast.error('Failed to save profile.'),
  });

  // Admin/Institution form state
  const [adminForm, setAdminForm] = useState({ name: '' });
  const adminFormInitialized = useRef(false);
  useEffect(() => {
    if (user && !adminFormInitialized.current && (user.role === 'CENTER_ADMIN' || user.role === 'INSTITUTION_ADMIN' || user.role === 'SUPER_ADMIN')) {
      setAdminForm({ name: user.name ?? '' });
      adminFormInitialized.current = true;
    }
  }, [user]);

  const adminSaveMutation = useMutation({
    mutationFn: (form: typeof adminForm) => {
      const parts = form.name.trim().split(/\s+/);
      return api.patch('/api/v1/auth/me', {
        firstName: parts[0] ?? '',
        lastName: parts.slice(1).join(' ') || (parts[0] ?? ''),
      });
    },
    onSuccess: () => {
      updateUser({ name: adminForm.name });
      toast.success('Profile updated successfully!');
    },
    onError: () => toast.error('Failed to save profile.'),
  });

  const [teacherForm, setTeacherForm] = useState({
    fullName: '', bio: '', specializations: '', yearsOfExperience: 0,
    hourlyRate: '', gender: '', district: '',
  });
  const teacherFormInitialized = useRef(false);
  useEffect(() => {
    if (mentorProfile && !teacherFormInitialized.current) {
      setTeacherForm({
        fullName: mentorProfile.fullName ?? '',
        bio: mentorProfile.bio ?? '',
        specializations: Array.isArray(mentorProfile.specializations)
          ? mentorProfile.specializations.join(', ')
          : (mentorProfile.specializations ?? ''),
        yearsOfExperience: mentorProfile.yearsOfExperience ?? 0,
        hourlyRate: mentorProfile.hourlyRate != null ? String(mentorProfile.hourlyRate) : '',
        gender: mentorProfile.gender ?? '',
        district: mentorProfile.district ?? '',
      });
      teacherFormInitialized.current = true;
    }
  }, [mentorProfile]);

  const teacherSaveMutation = useMutation({
    mutationFn: async (form: typeof teacherForm) => {
      if (!mentorProfile) {
        await api.post('/api/v1/mentors', {
          userId: user!.id,
          fullName: form.fullName || user!.name || 'Teacher',
          email: user!.email,
          bio: form.bio || undefined,
          specializations: form.specializations || undefined,
          yearsOfExperience: form.yearsOfExperience || 0,
          hourlyRate: form.hourlyRate ? parseFloat(form.hourlyRate) : 0.01,
          gender: form.gender || undefined,
          district: form.district || undefined,
        });
      } else {
        await api.patch('/api/v1/mentors/me', {
          fullName: form.fullName || undefined,
          bio: form.bio || undefined,
          specializations: form.specializations
            ? form.specializations.split(',').map((s: string) => s.trim()).filter(Boolean).join(',')
            : undefined,
          yearsOfExperience: form.yearsOfExperience || undefined,
          hourlyRate: form.hourlyRate ? parseFloat(form.hourlyRate) : undefined,
          gender: form.gender || undefined,
          district: form.district || undefined,
        });
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mentor-profile-me'] });
      toast.success('Profile updated successfully!');
    },
    onError: () => toast.error('Failed to save profile.'),
  });

  const allFields = (() => {
    if (!user) return null;
    if (user.role === 'STUDENT')
      return [user.name, user.email, user.avatarUrl, profile?.phone, profile?.gender, profile?.dateOfBirth, profile?.city, profile?.stream];
    if (user.role === 'PARENT')
      return [parentProfile?.name, parentProfile?.phone, parentProfile?.email, parentProfile?.gender, parentProfile?.relationshipType, parentProfile?.address, parentProfile?.city, parentProfile?.state, parentProfile?.pincode];
    if (user.role === 'TEACHER')
      return [mentorProfile?.fullName, mentorProfile?.email, user.avatarUrl, mentorProfile?.bio, mentorProfile?.specializations, mentorProfile?.yearsOfExperience, mentorProfile?.hourlyRate, mentorProfile?.gender];
    if (user.role === 'CENTER_ADMIN' || user.role === 'INSTITUTION_ADMIN' || user.role === 'SUPER_ADMIN')
      return [user.name, user.email];
    return null;
  })();
  const profilePct = allFields ? Math.round(allFields.filter(Boolean).length / allFields.length * 100) : null;

  return (
    <div className="space-y-6">
      {/* Header row — Add Parent button for students */}
      {user?.role === 'STUDENT' && (
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-medium text-white/40 uppercase tracking-wider">Profile</h2>
          <button
            onClick={() => setShowAddParentModal(true)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-brand-500/15 border border-brand-500/30 text-brand-400 hover:bg-brand-500/25 text-xs font-medium transition-colors"
          >
            <Plus className="w-3.5 h-3.5" /> Add Parent
          </button>
        </div>
      )}

      {/* Profile completion */}
      <div className="card">
        <div className="flex items-center justify-between mb-2">
          <span className="text-sm text-white/70 font-medium">Profile Completion</span>
          {profilePct !== null
            ? <span className={cn('text-sm font-bold', profilePct === 100 ? 'text-emerald-400' : profilePct >= 50 ? 'text-amber-400' : 'text-red-400')}>{profilePct}%</span>
            : <span className="h-4 w-8 bg-white/10 rounded animate-pulse" />}
        </div>
        <div className="h-2 bg-surface-200 rounded-full overflow-hidden">
          {profilePct !== null
            ? <div className={cn('h-full rounded-full transition-all duration-500', profilePct === 100 ? 'bg-emerald-500' : profilePct >= 50 ? 'bg-amber-500' : 'bg-red-500')} style={{ width: `${profilePct}%` }} />
            : <div className="h-full w-1/3 bg-white/10 rounded-full animate-pulse" />}
        </div>
        {profilePct !== null && profilePct < 100 && allFields && (
          <p className="text-xs text-white/30 mt-1.5">{allFields.filter(Boolean).length}/{allFields.length} fields complete — fill in details below to improve</p>
        )}
      </div>

      {/* Avatar */}
      <div className="card">
        <h3 className="text-base font-semibold text-white mb-4">Profile Photo</h3>
        <div className="flex items-center gap-6">
          {(() => {
            const roleColor = ROLE_COLORS[user?.role ?? ''] ?? ROLE_COLORS['GUEST'];
            return (
              <div className="relative group flex-shrink-0">
                {/* Neon role-color ring — same style as sidebar user card */}
                <div className="absolute -inset-1.5 rounded-full pointer-events-none"
                  style={{ background: `radial-gradient(circle, ${roleColor.neon}22, transparent 70%)`, boxShadow: `0 0 18px ${roleColor.ring}` }} />
                <Avatar name={user?.name ?? 'User'} size="xl" imageUrl={user?.avatarUrl} />
                <button
                  onClick={() => avatarInputRef.current?.click()}
                  className="absolute inset-0 flex items-center justify-center rounded-full bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity"
                >
                  <Camera className="w-5 h-5 text-white" />
                </button>
                <input ref={avatarInputRef} type="file" accept="image/*" className="hidden" onChange={() => toast.info('Avatar upload coming soon!')} />
              </div>
            );
          })()}
          <div>
            <p className="text-white font-medium">{user?.name}</p>
            <p className="text-white/40 text-sm">{user?.email}</p>
            <button
              onClick={() => avatarInputRef.current?.click()}
              className="text-brand-400 hover:text-brand-300 text-sm mt-1 transition-colors"
            >
              Change photo
            </button>
          </div>
        </div>
      </div>

      {/* Academic & personal details — read only */}
      {user?.role === 'STUDENT' && profile && (
        <div className="card">
          <h3 className="text-base font-semibold text-white mb-2">Academic Details</h3>
          <div>
            <InfoBadge label="Gender" value={profile.gender} icon={User} />
            <InfoBadge label="Date of Birth" value={formatDob(profile.dateOfBirth)} icon={Calendar} />
            <InfoBadge label="Board" value={profile.board} icon={BookOpen} />
            <InfoBadge label="Current Class" value={profile.currentClass != null ? `Class ${profile.currentClass}` : undefined} icon={GraduationCap} />
            <InfoBadge label="Stream" value={profile.stream} icon={BookOpen} />
            <InfoBadge label="Target Year" value={profile.targetYear} icon={Calendar} />
            <InfoBadge label="City" value={profile.city} icon={MapPin} />
            <InfoBadge label="State" value={profile.state} icon={MapPin} />
            {profile.district && <InfoBadge label="District" value={profile.district} icon={MapPin} />}
            {profile.country && <InfoBadge label="Country" value={profile.country} icon={MapPin} />}
          </div>
        </div>
      )}

      {/* Edit Info — students only */}
      {user?.role === 'STUDENT' && <div className="card">
        <h3 className="text-base font-semibold text-white mb-4">Edit Information</h3>
        <form onSubmit={handleSubmit((d) => saveMutation.mutate(d))} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-white/70 mb-1.5">Full Name</label>
            <input {...register('name')} className={cn('input w-full', errors.name && 'border-red-500/50')} />
            {errors.name && <p className="text-red-400 text-xs mt-1">{errors.name.message}</p>}
          </div>
          <div>
            <label className="block text-sm font-medium text-white/70 mb-1.5">Email</label>
            <input value={user?.email ?? ''} readOnly className="input w-full opacity-50 cursor-not-allowed" />
            <p className="text-white/30 text-xs mt-1">Email cannot be changed.</p>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-white/70 mb-1.5">Phone Number</label>
              <input {...register('phone')} placeholder="+91 98765 43210" className="input w-full" />
            </div>
            <div>
              <label className="block text-sm font-medium text-white/70 mb-1.5">Gender</label>
              <select {...register('gender')} className="input w-full">
                <option value="">— Select —</option>
                <option value="MALE">Male</option>
                <option value="FEMALE">Female</option>
                <option value="OTHER">Other</option>
                <option value="PREFER_NOT_TO_SAY">Prefer not to say</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-white/70 mb-1.5">Stream</label>
              <select {...register('stream')} className="input w-full">
                <option value="">— Select —</option>
                <option value="PCM">PCM</option>
                <option value="PCB">PCB</option>
                <option value="COMMERCE">Commerce</option>
                <option value="ARTS">Arts</option>
                <option value="VOCATIONAL">Vocational</option>
              </select>
            </div>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-white/70 mb-1.5">City</label>
              <input {...register('city')} placeholder="Mumbai" className="input w-full" />
            </div>
            <div>
              <label className="block text-sm font-medium text-white/70 mb-1.5">State</label>
              <input {...register('state')} list="india-states-list" placeholder="Maharashtra" className="input w-full" />
              <datalist id="india-states-list">
                {suggestStates('').map(s => <option key={s} value={s} />)}
              </datalist>
            </div>
            <div>
              <label className="block text-sm font-medium text-white/70 mb-1.5">Target Year</label>
              <input {...register('targetYear', { valueAsNumber: true })} type="number" placeholder="2026" className="input w-full" />
            </div>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-white/70 mb-1.5">District</label>
              <input {...register('district')} placeholder="District" className="input w-full" />
            </div>
            <div>
              <label className="block text-sm font-medium text-white/70 mb-1.5">Country</label>
              <input {...register('country')} placeholder="India" className="input w-full" />
            </div>
          </div>
          <button
            type="submit"
            disabled={isSubmitting || saveMutation.isPending}
            className="btn-primary flex items-center gap-2"
          >
            {saveMutation.isPending ? (
              <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            ) : (
              <Check className="w-4 h-4" />
            )}
            Save Changes
          </button>
        </form>
      </div>}

      {/* Edit Info — parents only */}
      {user?.role === 'PARENT' && (
        <div className="card">
          <h3 className="text-base font-semibold text-white mb-4">Edit Profile</h3>
          <form
            onSubmit={(e) => { e.preventDefault(); parentSaveMutation.mutate(parentForm); }}
            className="space-y-4"
          >
            <div>
              <label className="block text-sm font-medium text-white/70 mb-1.5">Full Name</label>
              <input
                value={parentForm.name}
                onChange={(e) => setParentForm((p) => ({ ...p, name: e.target.value }))}
                placeholder="Your full name"
                className="input w-full"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-white/70 mb-1.5">Email</label>
              <input value={user?.email ?? ''} readOnly className="input w-full opacity-50 cursor-not-allowed" />
              <p className="text-white/30 text-xs mt-1">Email cannot be changed.</p>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-white/70 mb-1.5">Phone Number</label>
                <input
                  value={parentForm.phone}
                  onChange={(e) => setParentForm((p) => ({ ...p, phone: e.target.value }))}
                  placeholder="+91 98765 43210"
                  className="input w-full"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-white/70 mb-1.5">Gender</label>
                <select
                  value={parentForm.gender}
                  onChange={(e) => setParentForm((p) => ({ ...p, gender: e.target.value }))}
                  className="input w-full"
                >
                  <option value="">— Select —</option>
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                  <option value="OTHER">Other</option>
                  <option value="PREFER_NOT_TO_SAY">Prefer not to say</option>
                </select>
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-white/70 mb-1.5">Address</label>
              <input
                value={parentForm.address}
                onChange={(e) => setParentForm((p) => ({ ...p, address: e.target.value }))}
                placeholder="Street address"
                className="input w-full"
              />
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-white/70 mb-1.5">City</label>
                <input
                  value={parentForm.city}
                  onChange={(e) => setParentForm((p) => ({ ...p, city: e.target.value }))}
                  placeholder="City"
                  className="input w-full"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-white/70 mb-1.5">State</label>
                <input
                  value={parentForm.state}
                  onChange={(e) => setParentForm((p) => ({ ...p, state: e.target.value }))}
                  list="parent-states-list"
                  placeholder="State"
                  className="input w-full"
                />
                <datalist id="parent-states-list">
                  {suggestStates('').map(s => <option key={s} value={s} />)}
                </datalist>
              </div>
              <div>
                <label className="block text-sm font-medium text-white/70 mb-1.5">Pincode</label>
                <input
                  value={parentForm.pincode}
                  onChange={(e) => setParentForm((p) => ({ ...p, pincode: e.target.value }))}
                  placeholder="400001"
                  className="input w-full"
                />
              </div>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-white/70 mb-1.5">District</label>
                <input
                  value={parentForm.district}
                  onChange={(e) => setParentForm((p) => ({ ...p, district: e.target.value }))}
                  placeholder="District"
                  className="input w-full"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-white/70 mb-1.5">Country</label>
                <input
                  value={parentForm.country}
                  onChange={(e) => setParentForm((p) => ({ ...p, country: e.target.value }))}
                  placeholder="India"
                  className="input w-full"
                />
              </div>
            </div>
            <button
              type="submit"
              disabled={parentSaveMutation.isPending}
              className="btn-primary flex items-center gap-2"
            >
              {parentSaveMutation.isPending ? (
                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <Check className="w-4 h-4" />
              )}
              Save Changes
            </button>
          </form>
        </div>
      )}

      {/* Edit Info — teachers only */}
      {user?.role === 'TEACHER' && mentorProfile && (
        <div className="card">
          <h3 className="text-base font-semibold text-white mb-4">Edit Profile</h3>
          <form
            onSubmit={(e) => { e.preventDefault(); teacherSaveMutation.mutate(teacherForm); }}
            className="space-y-4"
          >
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-white/50 uppercase tracking-wider">Full Name</label>
              <input
                type="text"
                value={teacherForm.fullName}
                onChange={e => setTeacherForm(f => ({ ...f, fullName: e.target.value }))}
                className="input w-full"
                placeholder="Your display name"
              />
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-white/70 mb-1.5">Gender</label>
                <select
                  value={teacherForm.gender}
                  onChange={(e) => setTeacherForm((p) => ({ ...p, gender: e.target.value }))}
                  className="input w-full"
                >
                  <option value="">— Select —</option>
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                  <option value="OTHER">Other</option>
                  <option value="PREFER_NOT_TO_SAY">Prefer not to say</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-white/70 mb-1.5">Years of Experience</label>
                <input
                  type="number"
                  min={0}
                  value={teacherForm.yearsOfExperience}
                  onChange={(e) => setTeacherForm((p) => ({ ...p, yearsOfExperience: parseInt(e.target.value) || 0 }))}
                  className="input w-full"
                />
              </div>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <label className="text-xs font-medium text-white/50 uppercase tracking-wider">District</label>
                <input
                  type="text"
                  value={teacherForm.district}
                  onChange={e => setTeacherForm(f => ({ ...f, district: e.target.value }))}
                  className="input w-full"
                  placeholder="e.g. Chennai"
                />
              </div>
              <div className="space-y-1.5">
                <label className="text-xs font-medium text-white/50 uppercase tracking-wider">Hourly Rate (₹)</label>
                <input
                  type="number"
                  min="0"
                  value={teacherForm.hourlyRate}
                  onChange={e => setTeacherForm(f => ({ ...f, hourlyRate: e.target.value }))}
                  className="input w-full"
                  placeholder="e.g. 500"
                />
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-white/70 mb-1.5">Specializations</label>
              <input
                type="text"
                value={teacherForm.specializations}
                onChange={(e) => setTeacherForm((p) => ({ ...p, specializations: e.target.value }))}
                placeholder="e.g. Mathematics, Physics"
                className="input w-full"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-white/70 mb-1.5">Bio</label>
              <textarea
                value={teacherForm.bio}
                onChange={(e) => setTeacherForm((p) => ({ ...p, bio: e.target.value }))}
                placeholder="Tell students about yourself..."
                rows={3}
                className="input w-full resize-none"
              />
            </div>
            <button
              type="submit"
              disabled={teacherSaveMutation.isPending}
              className="btn-primary flex items-center gap-2"
            >
              {teacherSaveMutation.isPending ? (
                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <Check className="w-4 h-4" />
              )}
              Save Changes
            </button>
          </form>
        </div>
      )}

      {/* Edit Info — CENTER_ADMIN / INSTITUTION_ADMIN / SUPER_ADMIN */}
      {(user?.role === 'CENTER_ADMIN' || user?.role === 'INSTITUTION_ADMIN' || user?.role === 'SUPER_ADMIN') && (
        <div className="card">
          <h3 className="text-base font-semibold text-white mb-4">Edit Profile</h3>
          <form
            onSubmit={(e) => { e.preventDefault(); adminSaveMutation.mutate(adminForm); }}
            className="space-y-4"
          >
            <div>
              <label className="block text-sm font-medium text-white/70 mb-1.5">Full Name</label>
              <input
                value={adminForm.name}
                onChange={(e) => setAdminForm((p) => ({ ...p, name: e.target.value }))}
                placeholder="Your full name"
                className="input w-full"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-white/70 mb-1.5">Email</label>
              <input value={user?.email ?? ''} readOnly className="input w-full opacity-50 cursor-not-allowed" />
              <p className="text-white/30 text-xs mt-1">Email cannot be changed.</p>
            </div>
            <button
              type="submit"
              disabled={adminSaveMutation.isPending || !adminForm.name.trim()}
              className="btn-primary flex items-center gap-2"
            >
              {adminSaveMutation.isPending ? (
                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <Check className="w-4 h-4" />
              )}
              Save Changes
            </button>
          </form>
        </div>
      )}

      {/* Danger Zone */}
      <div className="pt-1">
        <button
          onClick={() => setShowDeleteModal(true)}
          className="flex items-center gap-2 text-red-400/50 hover:text-red-400 text-xs transition-colors"
        >
          <AlertTriangle className="w-3.5 h-3.5" />
          Delete Account
        </button>
      </div>

      <Modal isOpen={showDeleteModal} onClose={() => setShowDeleteModal(false)} title="Delete Account">
        <div className="space-y-4">
          <div className="p-4 bg-red-600/10 border border-red-500/20 rounded-xl">
            <p className="text-red-300 text-sm">
              This will permanently delete your account, all your data, sessions, and progress. This cannot be undone.
            </p>
          </div>
          <div>
            <label className="block text-sm font-medium text-white/70 mb-1.5">
              Type <span className="text-red-400 font-mono">DELETE</span> to confirm
            </label>
            <input
              value={deleteConfirm}
              onChange={(e) => setDeleteConfirm(e.target.value)}
              placeholder="DELETE"
              className="input w-full"
            />
          </div>
          <div className="flex gap-3">
            <button
              onClick={() => setShowDeleteModal(false)}
              className="flex-1 btn-ghost border border-white/10"
            >
              Cancel
            </button>
            <button
              disabled={deleteConfirm !== 'DELETE'}
              onClick={() => toast.error('Account deletion is disabled in demo mode.')}
              className="flex-1 py-2 rounded-xl bg-red-600 hover:bg-red-500 disabled:opacity-40 disabled:cursor-not-allowed text-white font-medium text-sm transition-all"
            >
              Delete Account
            </button>
          </div>
        </div>
      </Modal>

      {/* Add Parent modal */}
      {showAddParentModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm overflow-y-auto">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-surface-100 border border-white/10 rounded-2xl w-full max-w-md shadow-2xl my-4"
          >
            {/* Header */}
            <div className="flex items-center justify-between p-5 pb-4 border-b border-white/5">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-brand-500/15 flex items-center justify-center">
                  <UserPlus className="w-5 h-5 text-brand-400" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-white">Add Parent / Guardian</h3>
                  <p className="text-xs text-white/40">
                    {addParentPhase === 'choose' ? 'Choose how to add your parent' :
                     addParentPhase === 'search' ? 'Search by email — link if found, register if not' :
                     `Creating account for ${addParentEmail}`}
                  </p>
                </div>
              </div>
              <button onClick={() => { setShowAddParentModal(false); resetAddParentModal(); }} className="p-1.5 rounded-lg hover:bg-white/5 text-white/40 hover:text-white/70 transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-5 space-y-4">
              {/* Phase: Choose */}
              {addParentPhase === 'choose' && (
                <div className="space-y-4">
                  <p className="text-sm text-white/50 text-center">How would you like to add your parent?</p>
                  <div className="grid grid-cols-2 gap-3">
                    <button
                      type="button"
                      onClick={() => setAddParentPhase('search')}
                      className="flex flex-col items-center gap-3 p-5 rounded-2xl border border-violet-500/30 bg-violet-500/5 hover:bg-violet-500/10 hover:border-violet-500/50 transition-all text-left group"
                    >
                      <div className="w-12 h-12 rounded-xl bg-violet-500/15 flex items-center justify-center group-hover:bg-violet-500/25 transition-colors">
                        <Search className="w-6 h-6 text-violet-400" />
                      </div>
                      <div>
                        <p className="text-sm font-semibold text-white mb-1">Already on NexusEd</p>
                        <p className="text-xs text-white/40 leading-relaxed">Parent has an account. Enter their email to link instantly.</p>
                      </div>
                    </button>
                    <button
                      type="button"
                      onClick={() => setAddParentPhase('register')}
                      className="flex flex-col items-center gap-3 p-5 rounded-2xl border border-brand-500/30 bg-brand-500/5 hover:bg-brand-500/10 hover:border-brand-500/50 transition-all text-left group"
                    >
                      <div className="w-12 h-12 rounded-xl bg-brand-500/15 flex items-center justify-center group-hover:bg-brand-500/25 transition-colors">
                        <UserPlus className="w-6 h-6 text-brand-400" />
                      </div>
                      <div>
                        <p className="text-sm font-semibold text-white mb-1">New to NexusEd</p>
                        <p className="text-xs text-white/40 leading-relaxed">Parent doesn't have an account. Register them right here.</p>
                      </div>
                    </button>
                  </div>
                  <button
                    onClick={() => { setShowAddParentModal(false); resetAddParentModal(); }}
                    className="w-full px-4 py-2.5 rounded-xl border border-white/10 text-sm text-white/40 hover:text-white/70 hover:border-white/20 transition-colors"
                  >
                    Cancel
                  </button>
                </div>
              )}

              {/* Phase: Search */}
              {addParentPhase === 'search' && (
                <>
                  <div className="flex items-start gap-3 p-3 rounded-xl bg-gradient-to-r from-brand-500/10 to-violet-500/10 border border-brand-500/20">
                    <Search className="w-4 h-4 text-brand-400 flex-shrink-0 mt-0.5" />
                    <p className="text-xs text-white/60 leading-relaxed">
                      Enter your parent's email. If they already have a NexusEd account we'll link instantly.
                      If not, you can register them right here.
                    </p>
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-white/60 mb-1.5">Parent's Email <span className="text-red-400">*</span></label>
                    <input
                      type="email"
                      value={addParentEmail}
                      onChange={(e) => setAddParentEmail(e.target.value)}
                      placeholder="parent@example.com"
                      className="input w-full"
                      onKeyDown={(e) => { if (e.key === 'Enter') handleAddParent(); }}
                      autoFocus
                    />
                  </div>
                  <div className="flex gap-3">
                    <button
                      onClick={() => setAddParentPhase('choose')}
                      className="px-4 py-2.5 rounded-xl border border-white/10 text-sm text-white/60 hover:text-white hover:border-white/20 transition-colors"
                    >
                      ← Back
                    </button>
                    <button
                      onClick={handleAddParent}
                      disabled={addParentSending || !addParentEmail.trim()}
                      className="flex-1 btn-primary py-2.5 text-sm disabled:opacity-50 flex items-center justify-center gap-2"
                    >
                      {addParentSending ? <Loader2 className="w-4 h-4 animate-spin" /> : <><Search className="w-4 h-4" /> Find Parent</>}
                    </button>
                  </div>
                </>
              )}

              {/* Phase: Register */}
              {addParentPhase === 'register' && (
                <>
                  <div className="flex items-center gap-2 p-3 rounded-xl bg-amber-500/10 border border-amber-500/20">
                    <span className="text-amber-400">✗</span>
                    <div>
                      <p className="text-xs text-amber-300 font-medium">Not found — fill in their details to create an account</p>
                      <p className="text-xs text-white/40 mt-0.5">{addParentEmail}</p>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-xs font-medium text-white/60 mb-1.5">First Name <span className="text-red-400">*</span></label>
                      <input type="text" value={addParentRegForm.firstName} onChange={(e) => setAddParentRegForm(f => ({ ...f, firstName: e.target.value }))} placeholder="Ravi" className="input w-full" />
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-white/60 mb-1.5">Last Name <span className="text-red-400">*</span></label>
                      <input type="text" value={addParentRegForm.lastName} onChange={(e) => setAddParentRegForm(f => ({ ...f, lastName: e.target.value }))} placeholder="Sharma" className="input w-full" />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-xs font-medium text-white/60 mb-1.5">Phone</label>
                      <input type="tel" value={addParentRegForm.phone} onChange={(e) => setAddParentRegForm(f => ({ ...f, phone: e.target.value }))} placeholder="+91 98765 43210" className="input w-full" />
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-white/60 mb-1.5">Gender</label>
                      <select value={addParentRegForm.gender} onChange={(e) => setAddParentRegForm(f => ({ ...f, gender: e.target.value }))} className="input w-full">
                        <option value="">— Select —</option>
                        <option value="MALE">Male</option>
                        <option value="FEMALE">Female</option>
                        <option value="OTHER">Other</option>
                        <option value="PREFER_NOT_TO_SAY">Prefer not to say</option>
                      </select>
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-white/60 mb-1.5">Relationship <span className="text-red-400">*</span></label>
                    <select value={addParentRegForm.relationship} onChange={(e) => setAddParentRegForm(f => ({ ...f, relationship: e.target.value }))} className="input w-full">
                      <option value="MOTHER">Mother</option>
                      <option value="FATHER">Father</option>
                      <option value="GUARDIAN">Guardian</option>
                      <option value="GRANDPARENT">Grandparent</option>
                      <option value="SIBLING">Sibling</option>
                      <option value="OTHER">Other</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-white/60 mb-1.5">Password for parent account <span className="text-red-400">*</span></label>
                    <div className="relative">
                      <input
                        type={addParentShowPw ? 'text' : 'password'}
                        value={addParentRegForm.password}
                        onChange={(e) => setAddParentRegForm(f => ({ ...f, password: e.target.value }))}
                        placeholder="••••••••"
                        className="input w-full pr-10"
                      />
                      <button type="button" onClick={() => setAddParentShowPw(p => !p)} className="absolute right-3 top-1/2 -translate-y-1/2 text-white/30 hover:text-white/70">
                        {addParentShowPw ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>
                    </div>
                    <div className="flex flex-wrap gap-1.5 mt-1.5">
                      {[['length', '8+ chars', addParentRegForm.password.length >= 8], ['upper', 'Uppercase', /[A-Z]/.test(addParentRegForm.password)], ['digit', 'Digit', /[0-9]/.test(addParentRegForm.password)], ['special', 'Special char', /[^A-Za-z0-9]/.test(addParentRegForm.password)]].map(([k, label, ok]) => (
                        <span key={String(k)} className={cn('text-xs px-2 py-0.5 rounded-full border transition-colors', ok ? 'border-green-500/50 bg-green-500/10 text-green-400' : 'border-white/10 text-white/30')}>{String(label)}</span>
                      ))}
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-white/60 mb-1.5">Confirm Password <span className="text-red-400">*</span></label>
                    <input
                      type="password"
                      value={addParentRegForm.confirmPassword}
                      onChange={(e) => setAddParentRegForm(f => ({ ...f, confirmPassword: e.target.value }))}
                      placeholder="••••••••"
                      className="input w-full"
                    />
                  </div>

                  <div className="flex justify-center">
                    <CaptchaWidget onVerify={handleAddParentCaptcha} />
                  </div>

                  <div className="flex gap-3">
                    <button
                      onClick={() => setAddParentPhase('choose')}
                      className="px-4 py-2.5 rounded-xl border border-white/10 text-sm text-white/60 hover:text-white hover:border-white/20 transition-colors flex items-center gap-1.5"
                    >
                      ← Back
                    </button>
                    <button
                      onClick={handleRegisterParent}
                      disabled={addParentRegistering || !addParentCaptcha}
                      className="flex-1 btn-primary py-2.5 text-sm disabled:opacity-50 flex items-center justify-center gap-2"
                    >
                      {addParentRegistering ? <Loader2 className="w-4 h-4 animate-spin" /> : <><UserPlus className="w-4 h-4" /> Create & Link Parent</>}
                    </button>
                  </div>
                </>
              )}
            </div>
          </motion.div>
        </div>
      )}
    </div>
  );
}

type NotifKey = 'emailNotifications' | 'pushNotifications' | 'weeklyReport' | 'newMentorAvailable' | 'examReminders' | 'aiRecommendations';

interface NotifPrefs {
  emailNotifications: boolean;
  pushNotifications: boolean;
  weeklyReport: boolean;
  newMentorAvailable: boolean;
  examReminders: boolean;
  aiRecommendations: boolean;
}

function NotificationsTab() {
  const [prefs, setPrefs] = useState<NotifPrefs>(() => loadNotifPrefs());

  const togglePref = (key: NotifKey) => {
    setPrefs((p) => {
      const next = { ...p, [key]: !p[key] };
      try {
        localStorage.setItem(LS_NOTIF_KEY, JSON.stringify(next));
      } catch {
        // ignore
      }
      return next;
    });
    toast.success('Preference saved.');
  };

  const notifItems: { key: NotifKey; label: string; description: string }[] = [
    { key: 'emailNotifications', label: 'Email Notifications', description: 'Receive updates and alerts via email' },
    { key: 'pushNotifications', label: 'Push Notifications', description: 'In-app and browser push notifications' },
    { key: 'weeklyReport', label: 'Weekly Progress Report', description: 'Summary of your study progress every Monday' },
    { key: 'newMentorAvailable', label: 'New Mentor Available', description: 'Alerts when a mentor matching your subjects joins' },
    { key: 'examReminders', label: 'Exam Reminders', description: 'Countdown reminders for your enrolled exams' },
    { key: 'aiRecommendations', label: 'AI Recommendations', description: 'Personalised study tips and career suggestions' },
  ];

  return (
    <div className="card space-y-5">
      <h3 className="text-base font-semibold text-white mb-2">Notification Preferences</h3>
      {notifItems.map((item, i) => (
        <motion.div
          key={item.key}
          initial={{ opacity: 0, x: -12 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ delay: i * 0.05 }}
          className={cn(i < notifItems.length - 1 && 'pb-5 border-b border-white/8')}
        >
          <Toggle
            checked={prefs[item.key]}
            onChange={() => togglePref(item.key)}
            label={item.label}
            description={item.description}
          />
        </motion.div>
      ))}
    </div>
  );
}

function AppearanceTab() {
  const { theme, setTheme } = useThemeStore();
  const [accent, setAccent] = useState(() => loadAccentColor());
  const [fontSize, setFontSize] = useState<'normal' | 'large'>('normal');

  // Apply saved accent on mount
  useEffect(() => {
    applyAccent(accent);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="space-y-6">
      <div className="card">
        <h3 className="text-base font-semibold text-white mb-4">Theme</h3>
        <div className="flex gap-3">
          {(['dark', 'light'] as const).map((t) => (
            <button
              key={t}
              onClick={() => setTheme(t)}
              className={cn(
                'flex-1 flex items-center justify-center gap-2 py-3 rounded-xl border transition-all text-sm font-medium',
                theme === t ? 'bg-brand-600 border-brand-500 text-white' : 'glass border-white/10 text-white/60 hover:border-white/20'
              )}
            >
              {t === 'dark' ? <Monitor className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              {t.charAt(0).toUpperCase() + t.slice(1)}
            </button>
          ))}
        </div>
      </div>

      <div className="card">
        <h3 className="text-base font-semibold text-white mb-4">Accent Colour</h3>
        <div className="flex gap-3 flex-wrap">
          {ACCENT_COLORS.map((c) => (
            <button
              key={c.name}
              onClick={() => {
                setAccent(c.name);
                applyAccent(c.name);
                try { localStorage.setItem(LS_ACCENT_KEY, c.name); } catch { /* ignore */ }
                toast.success(`Accent changed to ${c.label}`);
              }}
              title={c.label}
              className={cn(
                'w-10 h-10 rounded-full transition-all border-2',
                accent === c.name ? 'border-white scale-110' : 'border-transparent hover:scale-105'
              )}
              style={{ background: c.hex }}
            >
              {accent === c.name && <Check className="w-4 h-4 text-white mx-auto" />}
            </button>
          ))}
        </div>
        <p className="text-white/30 text-xs mt-3">
          Selected: {ACCENT_COLORS.find((c) => c.name === accent)?.label}
        </p>
      </div>

      <div className="card">
        <h3 className="text-base font-semibold text-white mb-4">Font Size</h3>
        <div className="flex gap-3">
          {(['normal', 'large'] as const).map((f) => (
            <button
              key={f}
              onClick={() => { setFontSize(f); toast.success(`Font size set to ${f}`); }}
              className={cn(
                'flex-1 py-3 rounded-xl border transition-all text-sm font-medium',
                fontSize === f ? 'bg-brand-600 border-brand-500 text-white' : 'glass border-white/10 text-white/60 hover:border-white/20'
              )}
            >
              {f.charAt(0).toUpperCase() + f.slice(1)}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}

function SecurityTab() {
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);

  // MFA state
  const [mfaEnabled, setMfaEnabled] = useState<boolean | null>(null);
  const [mfaSetup, setMfaSetup] = useState<{ secret: string; qrCodeUri: string } | null>(null);
  const [mfaConfirmCode, setMfaConfirmCode] = useState('');
  const [mfaDisableCode, setMfaDisableCode] = useState('');
  const [showDisableInput, setShowDisableInput] = useState(false);
  const [isMfaLoading, setIsMfaLoading] = useState(false);

  const { register, handleSubmit, formState: { errors }, reset } = useForm<PasswordForm>({
    resolver: zodResolver(passwordSchema),
  });

  // Load MFA status on mount
  useEffect(() => {
    api.get('/api/v1/auth/mfa/status')
      .then((r) => setMfaEnabled((r.data as { enabled: boolean }).enabled))
      .catch(() => setMfaEnabled(false));
  }, []);

  const passwordMutation = useMutation({
    mutationFn: (data: PasswordForm) =>
      api.post('/api/v1/auth/change-password', {
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
      }),
    onSuccess: () => {
      toast.success('Password changed successfully! Please log in again on other devices.');
      reset();
    },
    onError: (err: unknown) => {
      const axiosErr = err as { response?: { data?: { detail?: string } } };
      const detail = axiosErr.response?.data?.detail;
      toast.error(detail ?? 'Failed to change password. Check your current password.');
    },
  });

  async function handleMfaToggle(enable: boolean) {
    if (enable) {
      setIsMfaLoading(true);
      try {
        const r = await api.post('/api/v1/auth/mfa/setup');
        setMfaSetup(r.data as { secret: string; qrCodeUri: string });
      } catch {
        toast.error('Failed to initiate 2FA setup.');
      } finally {
        setIsMfaLoading(false);
      }
    } else {
      setShowDisableInput(true);
    }
  }

  async function handleMfaConfirm() {
    if (!mfaSetup || mfaConfirmCode.length !== 6) return;
    setIsMfaLoading(true);
    try {
      await api.post('/api/v1/auth/mfa/setup/confirm', { totpCode: mfaConfirmCode });
      setMfaEnabled(true);
      setMfaSetup(null);
      setMfaConfirmCode('');
      toast.success('Two-factor authentication enabled!');
    } catch {
      toast.error('Invalid code. Please try again.');
      setMfaConfirmCode('');
    } finally {
      setIsMfaLoading(false);
    }
  }

  async function handleMfaDisable() {
    if (mfaDisableCode.length !== 6) return;
    setIsMfaLoading(true);
    try {
      await api.delete('/api/v1/auth/mfa/setup', { data: { totpCode: mfaDisableCode } });
      setMfaEnabled(false);
      setShowDisableInput(false);
      setMfaDisableCode('');
      toast.success('Two-factor authentication disabled.');
    } catch {
      toast.error('Invalid code. Please try again.');
      setMfaDisableCode('');
    } finally {
      setIsMfaLoading(false);
    }
  }

  return (
    <div className="space-y-6">
      {/* Change Password */}
      <div className="card">
        <h3 className="text-base font-semibold text-white mb-4">Change Password</h3>
        <form onSubmit={handleSubmit((d) => passwordMutation.mutate(d))} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-white/70 mb-1.5">Current Password</label>
            <div className="relative">
              <input
                {...register('currentPassword')}
                type={showCurrent ? 'text' : 'password'}
                className={cn('input w-full pr-10', errors.currentPassword && 'border-red-500/50')}
              />
              <button type="button" onClick={() => setShowCurrent(!showCurrent)} className="absolute right-3 top-1/2 -translate-y-1/2 text-white/30 hover:text-white/70">
                {showCurrent ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
            {errors.currentPassword && <p className="text-red-400 text-xs mt-1">{errors.currentPassword.message}</p>}
          </div>

          <div>
            <label className="block text-sm font-medium text-white/70 mb-1.5">New Password</label>
            <div className="relative">
              <input
                {...register('newPassword')}
                type={showNew ? 'text' : 'password'}
                className={cn('input w-full pr-10', errors.newPassword && 'border-red-500/50')}
              />
              <button type="button" onClick={() => setShowNew(!showNew)} className="absolute right-3 top-1/2 -translate-y-1/2 text-white/30 hover:text-white/70">
                {showNew ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
            {errors.newPassword && <p className="text-red-400 text-xs mt-1">{errors.newPassword.message}</p>}
          </div>

          <div>
            <label className="block text-sm font-medium text-white/70 mb-1.5">Confirm New Password</label>
            <input
              {...register('confirmPassword')}
              type="password"
              className={cn('input w-full', errors.confirmPassword && 'border-red-500/50')}
            />
            {errors.confirmPassword && <p className="text-red-400 text-xs mt-1">{errors.confirmPassword.message}</p>}
          </div>

          <button
            type="submit"
            disabled={passwordMutation.isPending}
            className="btn-primary flex items-center gap-2"
          >
            {passwordMutation.isPending ? (
              <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            ) : (
              <Shield className="w-4 h-4" />
            )}
            Change Password
          </button>
        </form>
      </div>

      {/* 2FA */}
      <div className="card">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-base font-semibold text-white">Two-Factor Authentication</h3>
            <p className="text-white/40 text-sm mt-0.5">
              {mfaEnabled ? 'Your account is protected with an authenticator app.' : 'Add an extra layer of security to your account'}
            </p>
          </div>
          {mfaEnabled !== null && (
            <Toggle
              checked={mfaEnabled}
              onChange={handleMfaToggle}
            />
          )}
        </div>

        {/* MFA Setup Flow */}
        {mfaSetup && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            className="mt-5 space-y-4"
          >
            <div className="p-4 bg-surface-100 rounded-xl border border-white/10">
              <p className="text-white/60 text-sm mb-3">
                1. Scan this QR code with your authenticator app (Google Authenticator, Authy, etc.)
              </p>
              {/* QR code rendered client-side — secret never leaves the browser */}
              <div className="flex justify-center mb-3 p-3 bg-white rounded-xl">
                <QRCodeSVG value={mfaSetup.qrCodeUri} size={180} />
              </div>
              <p className="text-white/40 text-xs text-center mb-1">Or enter this code manually:</p>
              <p className="text-brand-300 font-mono text-sm text-center break-all select-all">
                {mfaSetup.secret}
              </p>
            </div>

            <div>
              <p className="text-white/60 text-sm mb-2">
                2. Enter the 6-digit code from your app to confirm setup
              </p>
              <input
                type="text"
                inputMode="numeric"
                maxLength={6}
                value={mfaConfirmCode}
                onChange={(e) => setMfaConfirmCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                placeholder="000000"
                className="input w-full text-center text-xl tracking-widest font-mono mb-3"
              />
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={handleMfaConfirm}
                  disabled={mfaConfirmCode.length !== 6 || isMfaLoading}
                  className="btn-primary flex-1 flex items-center justify-center gap-2"
                >
                  {isMfaLoading ? (
                    <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  ) : (
                    <Smartphone className="w-4 h-4" />
                  )}
                  Enable 2FA
                </button>
                <button
                  type="button"
                  onClick={() => { setMfaSetup(null); setMfaConfirmCode(''); }}
                  className="px-4 py-2 rounded-xl border border-white/10 text-white/50 hover:text-white/70 text-sm"
                >
                  Cancel
                </button>
              </div>
            </div>
          </motion.div>
        )}

        {/* MFA Disable Flow */}
        {showDisableInput && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            className="mt-4 space-y-3"
          >
            <p className="text-white/60 text-sm">
              Enter your authenticator code to disable 2FA:
            </p>
            <input
              type="text"
              inputMode="numeric"
              maxLength={6}
              value={mfaDisableCode}
              onChange={(e) => setMfaDisableCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
              placeholder="000000"
              className="input w-full text-center text-xl tracking-widest font-mono"
            />
            <div className="flex gap-2">
              <button
                type="button"
                onClick={handleMfaDisable}
                disabled={mfaDisableCode.length !== 6 || isMfaLoading}
                className="px-4 py-2 rounded-xl bg-red-500/20 border border-red-500/30 text-red-400 hover:bg-red-500/30 text-sm disabled:opacity-50"
              >
                Disable 2FA
              </button>
              <button
                type="button"
                onClick={() => { setShowDisableInput(false); setMfaDisableCode(''); }}
                className="px-4 py-2 rounded-xl border border-white/10 text-white/50 hover:text-white/70 text-sm"
              >
                Cancel
              </button>
            </div>
          </motion.div>
        )}
      </div>

      {/* Active Sessions */}
      <div className="card">
        <h3 className="text-base font-semibold text-white mb-4">Active Sessions</h3>
        <p className="text-white/30 text-sm">No active sessions found.</p>
      </div>
    </div>
  );
}

export default function SettingsPage() {
  const [activeTab, setActiveTab] = useState<Tab>('profile');

  return (
    <div className="min-h-screen p-6 max-w-4xl mx-auto">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-white mb-1">Settings</h1>
        <p className="text-white/50">Manage your account, preferences, and security.</p>
      </div>

      <div className="flex flex-col md:flex-row gap-6">
        {/* Sidebar */}
        <div className="md:w-52 flex-shrink-0">
          <nav className="space-y-1">
            {TABS.map((tab) => (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                className={cn(
                  'w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all text-left',
                  activeTab === tab.key
                    ? 'bg-brand-600 text-white'
                    : 'text-white/50 hover:text-white hover:bg-white/5'
                )}
              >
                {tab.icon}
                {tab.label}
              </button>
            ))}
          </nav>
        </div>

        {/* Content */}
        <div className="flex-1 min-w-0">
          <AnimatePresence mode="wait">
            <motion.div
              key={activeTab}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.2 }}
            >
              {activeTab === 'profile' && <ProfileTab />}
              {activeTab === 'notifications' && <NotificationsTab />}
              {activeTab === 'appearance' && <AppearanceTab />}
              {activeTab === 'security' && <SecurityTab />}
            </motion.div>
          </AnimatePresence>
        </div>
      </div>
    </div>
  );
}
