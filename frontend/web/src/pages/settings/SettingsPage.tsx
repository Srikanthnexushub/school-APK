import { useState, useRef, useEffect } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { motion, AnimatePresence } from 'framer-motion';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  User, Bell, Palette, Shield, Camera, Check, AlertTriangle,
  Smartphone, Monitor, Eye, EyeOff, GraduationCap, Calendar, MapPin, BookOpen,
  ShieldCheck, Clock,
} from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { cn } from '../../lib/utils';
import { Toggle } from '../../components/ui/Toggle';
import { Avatar } from '../../components/ui/Avatar';
import { Modal } from '../../components/ui/Modal';

type Tab = 'profile' | 'notifications' | 'appearance' | 'security';

const TABS: { key: Tab; label: string; icon: React.ReactNode }[] = [
  { key: 'profile', label: 'Profile', icon: <User className="w-4 h-4" /> },
  { key: 'notifications', label: 'Notifications', icon: <Bell className="w-4 h-4" /> },
  { key: 'appearance', label: 'Appearance', icon: <Palette className="w-4 h-4" /> },
  { key: 'security', label: 'Security', icon: <Shield className="w-4 h-4" /> },
];

const ACCENT_COLORS = [
  { name: 'indigo', hex: '#6366f1', label: 'Indigo' },
  { name: 'violet', hex: '#8b5cf6', label: 'Violet' },
  { name: 'cyan', hex: '#06b6d4', label: 'Cyan' },
  { name: 'emerald', hex: '#10b981', label: 'Emerald' },
  { name: 'amber', hex: '#f59e0b', label: 'Amber' },
  { name: 'rose', hex: '#f43f5e', label: 'Rose' },
];

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
    return localStorage.getItem(LS_ACCENT_KEY) ?? 'indigo';
  } catch {
    return 'indigo';
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
  board?: string;
  currentClass?: number;
  stream?: string;
  targetYear?: number;
  status: string;
  createdAt: string;
  parentLinkCode?: string;
}

const profileSchema = z.object({
  name: z.string().min(2, 'Name must be at least 2 characters'),
  phone: z.string().optional(),
  city: z.string().optional(),
  state: z.string().optional(),
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

  const { data: profile } = useQuery<StudentProfile>({
    queryKey: ['student-profile-me'],
    queryFn: () => api.get('/api/v1/students/me').then((r) => r.data),
    enabled: !!user,
  });

  interface PendingLink { otp: string; parentName: string; expiresAt: string; }
  const [pendingLink, setPendingLink] = useState<PendingLink | null>(null);
  const [pendingLinkLoading, setPendingLinkLoading] = useState(true);
  const [otpSecondsLeft, setOtpSecondsLeft] = useState(0);
  const pendingLinkInitialized = useRef(false);

  async function fetchPendingLink(showSpinner = false) {
    if (showSpinner) setPendingLinkLoading(true);
    try {
      const res = await api.get('/api/v1/students/me/pending-link');
      setPendingLink(res.data as PendingLink);
      setOtpSecondsLeft(Math.max(0, Math.round((new Date(res.data.expiresAt).getTime() - Date.now()) / 1000)));
    } catch {
      setPendingLink(null);
    } finally {
      if (showSpinner) setPendingLinkLoading(false);
      if (!pendingLinkInitialized.current) {
        setPendingLinkLoading(false);
        pendingLinkInitialized.current = true;
      }
    }
  }

  useEffect(() => { fetchPendingLink(true); }, []);

  useEffect(() => {
    const interval = setInterval(() => fetchPendingLink(false), 10000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (!pendingLink) return;
    const timer = setInterval(() => {
      setOtpSecondsLeft((s) => Math.max(0, s - 1));
    }, 1000);
    return () => clearInterval(timer);
  }, [pendingLink]);

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<ProfileForm>({
    resolver: zodResolver(profileSchema),
    defaultValues: { name: user?.name ?? '', phone: '', city: '', state: '', stream: '', targetYear: undefined },
    values: profile ? {
      name: user?.name ?? '',
      phone: profile.phone ?? '',
      city: profile.city ?? '',
      state: profile.state ?? '',
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
      await Promise.all([
        api.patch('/api/v1/students/me', {
          firstName,
          lastName,
          phone: data.phone || undefined,
          city: data.city || undefined,
          state: data.state || undefined,
          stream: data.stream || undefined,
          targetYear: data.targetYear || undefined,
        }),
        api.patch('/api/v1/auth/me', { firstName, lastName }),
      ]);
      return data.name.trim();
    },
    onSuccess: (name) => {
      updateUser({ name });
      queryClient.invalidateQueries({ queryKey: ['student-profile-me'] });
      toast.success('Profile updated successfully!');
    },
    onError: () => toast.error('Failed to save profile.'),
  });

  const allFields = profile
    ? [user?.name, user?.email, user?.avatarUrl, profile.phone, profile.gender, profile.dateOfBirth, profile.city, profile.stream]
    : null;
  const profilePct = allFields ? Math.round(allFields.filter(Boolean).length / allFields.length * 100) : null;

  return (
    <div className="space-y-6">
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
          <div className="relative group">
            <Avatar name={user?.name ?? 'User'} size="xl" imageUrl={user?.avatarUrl} />
            <button
              onClick={() => avatarInputRef.current?.click()}
              className="absolute inset-0 flex items-center justify-center rounded-full bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity"
            >
              <Camera className="w-5 h-5 text-white" />
            </button>
            <input ref={avatarInputRef} type="file" accept="image/*" className="hidden" onChange={() => toast.info('Avatar upload coming soon!')} />
          </div>
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
      {profile && (
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
          </div>
        </div>
      )}

      {/* Parent Link Request (OTP) */}
      <div className="card">
        <div className="flex items-center gap-2 mb-3">
          <ShieldCheck className="w-4 h-4 text-brand-400" />
          <h3 className="text-base font-semibold text-white">Parent Link Request</h3>
        </div>
        {pendingLinkLoading && !pendingLink ? (
          <div className="h-20 flex items-center justify-center">
            <div className="w-5 h-5 border-2 border-white/20 border-t-brand-400 rounded-full animate-spin" />
          </div>
        ) : pendingLink && otpSecondsLeft > 0 ? (
          <>
            <p className="text-sm text-white/50 mb-4">
              <span className="text-white font-medium">{pendingLink.parentName}</span> wants to link to your account. Share the code below with them.
            </p>
            <div className="bg-brand-500/10 border border-brand-500/20 rounded-xl px-5 py-4 text-center mb-3">
              <span className="font-mono text-4xl font-bold text-brand-400 tracking-[0.4em]">
                {pendingLink.otp}
              </span>
            </div>
            <div className="flex items-center justify-center gap-2 text-sm">
              <Clock className="w-4 h-4 text-amber-400" />
              <span className={`font-mono font-bold ${otpSecondsLeft < 60 ? 'text-red-400' : 'text-amber-400'}`}>
                {`${Math.floor(otpSecondsLeft / 60)}:${String(otpSecondsLeft % 60).padStart(2, '0')}`}
              </span>
              <span className="text-white/30 text-xs">remaining</span>
            </div>
            <p className="text-xs text-white/30 mt-3 text-center">
              This OTP expires automatically. If you don't recognize this request, ignore it.
            </p>
          </>
        ) : (
          <p className="text-sm text-white/40">
            No active link request. When your parent clicks "Link Child" and finds your account, a one-time verification code will appear here for you to share with them.
          </p>
        )}
      </div>

      {/* Edit Info */}
      <div className="card">
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
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-white/70 mb-1.5">Phone Number</label>
              <input {...register('phone')} placeholder="+91 98765 43210" className="input w-full" />
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
              <input {...register('state')} placeholder="Maharashtra" className="input w-full" />
            </div>
            <div>
              <label className="block text-sm font-medium text-white/70 mb-1.5">Target Year</label>
              <input {...register('targetYear', { valueAsNumber: true })} type="number" placeholder="2026" className="input w-full" />
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
      </div>

      {/* Danger Zone */}
      <div className="card border border-red-500/20">
        <div className="flex items-center gap-2 mb-3">
          <AlertTriangle className="w-4 h-4 text-red-400" />
          <h3 className="text-base font-semibold text-red-400">Danger Zone</h3>
        </div>
        <p className="text-white/50 text-sm mb-4">
          Permanently delete your account and all associated data. This action cannot be undone.
        </p>
        <button
          onClick={() => setShowDeleteModal(true)}
          className="px-4 py-2 rounded-xl border border-red-500/30 text-red-400 hover:bg-red-600/10 transition-all text-sm"
        >
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
  const [theme, setTheme] = useState<'dark' | 'light'>('dark');
  const [accent, setAccent] = useState(() => loadAccentColor());
  const [fontSize, setFontSize] = useState<'normal' | 'large'>('normal');

  return (
    <div className="space-y-6">
      <div className="card">
        <h3 className="text-base font-semibold text-white mb-4">Theme</h3>
        <div className="flex gap-3">
          {(['dark', 'light'] as const).map((t) => (
            <button
              key={t}
              onClick={() => { setTheme(t); if (t === 'light') toast.info('Light mode coming soon!'); }}
              className={cn(
                'flex-1 flex items-center justify-center gap-2 py-3 rounded-xl border transition-all text-sm font-medium',
                theme === t ? 'bg-brand-600 border-brand-500 text-white' : 'glass border-white/10 text-white/60 hover:border-white/20'
              )}
            >
              {t === 'dark' ? <Monitor className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              {t.charAt(0).toUpperCase() + t.slice(1)}
              {t === 'dark' && <span className="text-xs opacity-60">(Default)</span>}
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
