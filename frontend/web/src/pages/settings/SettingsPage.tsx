import { useState, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  User, Bell, Palette, Shield, Camera, Check, AlertTriangle,
  Smartphone, Monitor, Eye, EyeOff,
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

const profileSchema = z.object({
  name: z.string().min(2, 'Name must be at least 2 characters'),
  phone: z.string().optional(),
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

function ProfileTab() {
  const user = useAuthStore((s) => s.user);
  const avatarInputRef = useRef<HTMLInputElement>(null);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState('');

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<ProfileForm>({
    resolver: zodResolver(profileSchema),
    defaultValues: { name: user?.name ?? '', phone: '' },
  });

  const saveMutation = useMutation({
    mutationFn: (data: ProfileForm) =>
      api.patch(`/api/v1/students/${user?.id}`, { name: data.name, phone: data.phone }),
    onSuccess: () => toast.success('Profile updated successfully!'),
    onError: () => toast.error('Failed to save profile.'),
  });

  return (
    <div className="space-y-6">
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

      {/* Edit Info */}
      <div className="card">
        <h3 className="text-base font-semibold text-white mb-4">Personal Information</h3>
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
          <div>
            <label className="block text-sm font-medium text-white/70 mb-1.5">Phone Number</label>
            <input {...register('phone')} placeholder="+91 98765 43210" className="input w-full" />
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
  const [twoFAEnabled, setTwoFAEnabled] = useState(false);

  const { register, handleSubmit, formState: { errors }, reset } = useForm<PasswordForm>({
    resolver: zodResolver(passwordSchema),
  });

  const passwordMutation = useMutation({
    mutationFn: (data: PasswordForm) =>
      api.post('/api/v1/auth/change-password', {
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
      }),
    onSuccess: () => {
      toast.success('Password changed successfully!');
      reset();
    },
    onError: () => toast.error('Failed to change password. Check your current password.'),
  });

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
            <p className="text-white/40 text-sm mt-0.5">Add an extra layer of security to your account</p>
          </div>
          <Toggle
            checked={twoFAEnabled}
            onChange={(v) => { setTwoFAEnabled(v); toast.info('2FA setup coming soon!'); }}
          />
        </div>
        {twoFAEnabled && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            className="mt-4 p-3 bg-brand-600/10 border border-brand-600/20 rounded-xl"
          >
            <p className="text-brand-300 text-sm flex items-center gap-2">
              <Smartphone className="w-4 h-4" />
              2FA setup will be available in the next release.
            </p>
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
