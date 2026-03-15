// src/pages/auth/AcceptInvitationPage.tsx
import { useState, useEffect, useCallback } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { BookOpen, Eye, EyeOff, CheckCircle2, AlertCircle, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { cn } from '../../lib/utils';
import CaptchaWidget from '../../components/CaptchaWidget';

// ─── Types ────────────────────────────────────────────────────────────────────

interface InvitationInfo {
  teacherId: string;
  centerId: string;
  centerName: string;
  email: string;
  firstName: string;
  lastName: string;
}

const schema = z
  .object({
    password: z
      .string()
      .min(8, 'At least 8 characters')
      .regex(/[A-Z]/, 'At least 1 uppercase letter')
      .regex(/[0-9]/, 'At least 1 digit')
      .regex(/[^A-Za-z0-9]/, 'At least 1 special character'),
    confirmPassword: z.string(),
  })
  .refine(d => d.password === d.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

type FormData = z.infer<typeof schema>;

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function AcceptInvitationPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token') ?? '';

  const [info, setInfo] = useState<InvitationInfo | null>(null);
  const [isLoadingInfo, setIsLoadingInfo] = useState(true);
  const [isInvalidToken, setIsInvalidToken] = useState(false);
  const [showPw, setShowPw] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [captchaToken, setCaptchaToken] = useState<string | null>(null);
  const handleCaptcha = useCallback((t: string | null) => setCaptchaToken(t), []);

  const { register, handleSubmit, watch, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  const watchedPassword = watch('password', '');
  const pwChecks = {
    length: watchedPassword.length >= 8,
    upper: /[A-Z]/.test(watchedPassword),
    digit: /[0-9]/.test(watchedPassword),
    special: /[^A-Za-z0-9]/.test(watchedPassword),
  };

  useEffect(() => {
    if (!token) { setIsInvalidToken(true); setIsLoadingInfo(false); return; }
    api.get<InvitationInfo>(`/api/v1/teachers/invitation/${token}`)
      .then(res => setInfo(res.data))
      .catch(() => setIsInvalidToken(true))
      .finally(() => setIsLoadingInfo(false));
  }, [token]);

  async function onSubmit(data: FormData) {
    if (!info) return;
    setIsSubmitting(true);
    try {
      if (!captchaToken) { toast.error('Please complete the captcha'); return; }

      // 1. Register the teacher's account via standard auth flow
      const deviceId = crypto.randomUUID();
      const regRes = await api.post('/api/v1/auth/register', {
        firstName: info.firstName,
        lastName: info.lastName,
        email: info.email,
        password: data.password,
        role: 'TEACHER',
        centerId: info.centerId,
        captchaToken,
        deviceFingerprint: { userAgent: navigator.userAgent, deviceId, ipSubnet: '127.0.0' },
      });

      const accessToken: string = regRes.data.accessToken;

      // 2. Link the teacher stub to the newly created userId
      const payload = JSON.parse(atob(accessToken.split('.')[1]));
      const userId: string = payload.sub;

      await api.post(
        `/api/v1/centers/${info.centerId}/teachers/accept-invitation`,
        { token, userId },
        { headers: { Authorization: `Bearer ${accessToken}` } }
      );

      toast.success(`Welcome to ${info.centerName}! Please verify your email then log in.`);
      navigate('/login');
    } catch (err: unknown) {
      const e = err as { response?: { data?: { detail?: string } } };
      toast.error(e.response?.data?.detail ?? 'Registration failed. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  }

  if (isLoadingInfo) {
    return (
      <div className="min-h-screen bg-surface flex items-center justify-center">
        <Loader2 className="w-8 h-8 animate-spin text-white/40" />
      </div>
    );
  }

  if (isInvalidToken || !info) {
    return (
      <div className="min-h-screen bg-surface flex items-center justify-center p-6">
        <div className="text-center max-w-sm">
          <div className="w-16 h-16 rounded-full bg-red-500/15 border border-red-500/30 flex items-center justify-center mx-auto mb-5">
            <AlertCircle className="w-8 h-8 text-red-400" />
          </div>
          <h2 className="text-xl font-bold text-white mb-2">Invalid or Expired Invitation</h2>
          <p className="text-white/40 text-sm mb-6">
            This invitation link is no longer valid. It may have expired (7 days) or already been used.
            Contact your institution coordinator for a new invitation.
          </p>
          <Link to="/login" className="btn-primary px-6 py-2.5 text-sm">Go to Login</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-surface flex items-center justify-center p-6">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="flex items-center gap-3 mb-8 justify-center">
          <div className="p-2 rounded-xl bg-brand-600/20 border border-brand-500/30">
            <BookOpen className="w-5 h-5 text-brand-400" />
          </div>
          <span className="font-bold text-lg text-white">NexusEd</span>
        </div>

        <div className="card space-y-5">
          {/* Invitation banner */}
          <div className="bg-brand-500/10 border border-brand-500/20 rounded-xl px-4 py-3">
            <p className="text-xs text-brand-400 font-semibold mb-0.5">You've been invited</p>
            <p className="text-sm text-white">
              {info.firstName} {info.lastName} — <span className="text-white/60">{info.centerName}</span>
            </p>
            <p className="text-xs text-white/40 mt-0.5">{info.email}</p>
          </div>

          <div>
            <h2 className="text-xl font-bold text-white mb-1">Set your password</h2>
            <p className="text-white/40 text-sm">Create a password to complete your account setup.</p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-white/70 mb-1.5">Password</label>
              <div className="relative">
                <input
                  {...register('password')}
                  type={showPw ? 'text' : 'password'}
                  placeholder="••••••••"
                  className={cn('input w-full pr-10', errors.password && 'border-red-500/50')}
                />
                <button
                  type="button"
                  onClick={() => setShowPw(!showPw)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-white/30 hover:text-white/70"
                >
                  {showPw ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {errors.password && <p className="text-red-400 text-xs mt-1">{errors.password.message}</p>}
              <div className="flex flex-wrap gap-2 mt-2">
                {[
                  { key: 'length', label: '≥ 8 chars' },
                  { key: 'upper', label: '1 uppercase' },
                  { key: 'digit', label: '1 digit' },
                  { key: 'special', label: '1 special char' },
                ].map(({ key, label }) => (
                  <span
                    key={key}
                    className={cn(
                      'text-xs px-2 py-0.5 rounded-full border transition-colors',
                      pwChecks[key as keyof typeof pwChecks]
                        ? 'border-green-500/50 bg-green-500/10 text-green-400'
                        : 'border-white/10 text-white/30'
                    )}
                  >
                    {label}
                  </span>
                ))}
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-white/70 mb-1.5">Confirm Password</label>
              <div className="relative">
                <input
                  {...register('confirmPassword')}
                  type={showConfirm ? 'text' : 'password'}
                  placeholder="••••••••"
                  className={cn('input w-full pr-10', errors.confirmPassword && 'border-red-500/50')}
                />
                <button
                  type="button"
                  onClick={() => setShowConfirm(!showConfirm)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-white/30 hover:text-white/70"
                >
                  {showConfirm ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {errors.confirmPassword && <p className="text-red-400 text-xs mt-1">{errors.confirmPassword.message}</p>}
            </div>

            <div className="flex justify-center">
              <CaptchaWidget onVerify={handleCaptcha} />
            </div>

            <button
              type="submit"
              disabled={isSubmitting || !captchaToken}
              className="btn-primary w-full flex items-center justify-center gap-2 py-3"
            >
              {isSubmitting
                ? <Loader2 className="w-5 h-5 animate-spin" />
                : <><CheckCircle2 className="w-4 h-4" /> Complete Setup</>}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
