import { useState, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { motion } from 'framer-motion';
import { BookOpen, Sparkles, ArrowRight, Eye, EyeOff, ShieldCheck } from 'lucide-react';
import { toast } from 'sonner';
import CaptchaWidget from '../../components/CaptchaWidget';
import GoogleSignInButton from '../../components/GoogleSignInButton';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { cn } from '../../lib/utils';

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID ?? '';

const schema = z.object({
  email: z.string().email('Invalid email'),
  password: z.string().min(6, 'Password too short'),
});

type FormData = z.infer<typeof schema>;

export default function LoginPage() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);
  const [showPw, setShowPw] = useState(false);
  const [captchaToken, setCaptchaToken] = useState<string | null>(null);
  const [isGoogleLoading, setIsGoogleLoading] = useState(false);
  const handleCaptchaVerify = useCallback((token: string | null) => setCaptchaToken(token), []);

  // MFA step state — populated when server returns 202 mfaRequired
  const [mfaStep, setMfaStep] = useState<{
    pendingMfaToken: string;
    deviceId: string;
  } | null>(null);
  const [totpCode, setTotpCode] = useState('');
  const [isMfaSubmitting, setIsMfaSubmitting] = useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  async function handleGoogleSuccess(accessToken: string) {
    setIsGoogleLoading(true);
    try {
      const deviceId = crypto.randomUUID();
      const res = await api.post('/api/v1/auth/google', { idToken: accessToken }, {
        headers: { 'X-Device-Id': deviceId },
      });
      const { accessToken: jwt, refreshToken } = res.data;
      const meRes = await api.get('/api/v1/auth/me', {
        headers: { Authorization: `Bearer ${jwt}` },
      });
      const u = meRes.data;
      const name = [u.firstName, u.lastName].filter(Boolean).join(' ') || u.email;
      setAuth(jwt, { id: u.id, email: u.email, role: u.role, name, centerId: u.centerId ?? undefined }, refreshToken, deviceId);
      toast.success('Signed in with Google!');
      if (u.role === 'CENTER_ADMIN' || u.role === 'SUPER_ADMIN') navigate('/admin');
      else if (u.role === 'PARENT') navigate('/parent');
      else if (u.role === 'TEACHER') navigate('/mentor-portal');
      else navigate('/dashboard');
    } catch {
      toast.error('Google Sign-In failed. Please try again.');
    } finally {
      setIsGoogleLoading(false);
    }
  }

  async function finishLogin(accessToken: string, refreshToken: string, deviceId: string) {
    const meRes = await api.get('/api/v1/auth/me', {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    const u = meRes.data;
    const name = [u.firstName, u.lastName].filter(Boolean).join(' ') || u.email;
    setAuth(accessToken, { id: u.id, email: u.email, role: u.role, name, centerId: u.centerId ?? undefined }, refreshToken, deviceId);
    toast.success('Welcome back!');
    if (u.role === 'CENTER_ADMIN' || u.role === 'SUPER_ADMIN') navigate('/admin');
    else if (u.role === 'PARENT') navigate('/parent');
    else if (u.role === 'TEACHER') navigate('/mentor-portal');
    else navigate('/dashboard');
  }

  async function onSubmit(data: FormData) {
    try {
      const deviceId = crypto.randomUUID();
      const loginRes = await api.post('/api/v1/auth/login', {
        email: data.email,
        password: data.password,
        captchaToken: captchaToken!,
        deviceFingerprint: {
          userAgent: navigator.userAgent,
          deviceId,
          ipSubnet: '127.0.0',
        },
      });

      // 202 Accepted → MFA required
      if (loginRes.status === 202 && loginRes.data.mfaRequired) {
        setMfaStep({ pendingMfaToken: loginRes.data.pendingMfaToken, deviceId });
        return;
      }

      const { accessToken, refreshToken } = loginRes.data;
      await finishLogin(accessToken, refreshToken, deviceId);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { detail?: string; title?: string } } };
      toast.error(axiosErr.response?.data?.detail ?? axiosErr.response?.data?.title ?? 'Login failed');
      setCaptchaToken(null);
    }
  }

  async function handleMfaVerify(e: React.FormEvent) {
    e.preventDefault();
    if (!mfaStep || totpCode.length !== 6) return;
    setIsMfaSubmitting(true);
    try {
      const res = await api.post('/api/v1/auth/mfa/verify', {
        pendingMfaToken: mfaStep.pendingMfaToken,
        totpCode,
        deviceFingerprint: {
          userAgent: navigator.userAgent,
          deviceId: mfaStep.deviceId,
          ipSubnet: '127.0.0',
        },
      });
      const { accessToken, refreshToken } = res.data;
      await finishLogin(accessToken, refreshToken, mfaStep.deviceId);
    } catch {
      toast.error('Invalid authenticator code. Please try again.');
      setTotpCode('');
    } finally {
      setIsMfaSubmitting(false);
    }
  }

  return (
    <div className="min-h-screen bg-surface flex">
      {/* Left panel */}
      <div className="hidden lg:flex flex-1 relative overflow-hidden bg-gradient-to-br from-brand-950 via-surface to-surface items-center justify-center p-12">
        <div className="absolute top-1/4 left-1/4 w-64 h-64 bg-brand-600/20 rounded-full blur-3xl animate-pulse" />
        <div className="absolute bottom-1/3 right-1/4 w-48 h-48 bg-violet-600/20 rounded-full blur-3xl animate-pulse delay-700" />
        <div className="absolute top-1/2 right-1/3 w-32 h-32 bg-cyan-600/15 rounded-full blur-2xl animate-pulse delay-1000" />

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
          className="relative z-10 max-w-md text-center"
        >
          <div className="flex justify-center mb-6">
            <div className="p-4 rounded-2xl bg-brand-600/20 border border-brand-500/30 animate-pulse-glow">
              <Sparkles className="w-10 h-10 text-brand-400" />
            </div>
          </div>
          <h1 className="text-4xl font-bold text-white mb-4">
            Your AI-powered<br />
            <span className="gradient-text">exam companion</span>
          </h1>
          <p className="text-white/50 text-lg leading-relaxed">
            Personalised study plans, real-time career guidance, and adaptive
            assessments — all in one platform.
          </p>

          <div className="mt-12 grid grid-cols-3 gap-4">
            {[
              { label: 'Readiness Score', value: 'Live ERS', icon: '📊' },
              { label: 'AI Mentor', value: '24 / 7', icon: '🤖' },
              { label: 'Career Paths', value: '100+', icon: '🎯' },
            ].map((stat) => (
              <div key={stat.label} className="glass rounded-xl p-4 text-center">
                <div className="text-2xl mb-1">{stat.icon}</div>
                <div className="text-white font-semibold text-sm">{stat.value}</div>
                <div className="text-white/40 text-xs mt-0.5">{stat.label}</div>
              </div>
            ))}
          </div>
        </motion.div>
      </div>

      {/* Right panel — form */}
      <div className="flex-1 flex items-center justify-center p-8">
        <motion.div
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.5 }}
          className="w-full max-w-md"
        >
          <div className="flex items-center gap-3 mb-10">
            <div className="p-2 rounded-xl bg-brand-600/20 border border-brand-500/30">
              <BookOpen className="w-5 h-5 text-brand-400" />
            </div>
            <span className="font-bold text-lg text-white">NexusEd</span>
          </div>

          {/* MFA step — replaces normal form after credentials are verified */}
          {mfaStep ? (
            <>
              <div className="flex justify-center mb-5">
                <div className="p-4 rounded-2xl bg-brand-600/20 border border-brand-500/30">
                  <ShieldCheck className="w-8 h-8 text-brand-400" />
                </div>
              </div>
              <h2 className="text-2xl font-bold text-white mb-2">Two-factor verification</h2>
              <p className="text-white/50 mb-8">
                Enter the 6-digit code from your authenticator app.
              </p>
              <form onSubmit={handleMfaVerify} className="space-y-5">
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-1.5">
                    Authenticator code
                  </label>
                  <input
                    type="text"
                    inputMode="numeric"
                    maxLength={6}
                    value={totpCode}
                    onChange={(e) => setTotpCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                    placeholder="000000"
                    className="input w-full text-center text-2xl tracking-widest font-mono"
                    autoFocus
                    disabled={isMfaSubmitting}
                  />
                </div>
                <button
                  type="submit"
                  disabled={isMfaSubmitting || totpCode.length !== 6}
                  className="btn-primary w-full flex items-center justify-center gap-2 py-3 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {isMfaSubmitting ? (
                    <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  ) : (
                    'Verify & Sign in'
                  )}
                </button>
              </form>
              <button
                type="button"
                onClick={() => { setMfaStep(null); setTotpCode(''); setCaptchaToken(null); }}
                className="mt-4 w-full text-center text-white/40 hover:text-white/70 text-sm transition-colors"
              >
                ← Back to login
              </button>
            </>
          ) : (
            <>
          <h2 className="text-3xl font-bold text-white mb-2">Welcome back</h2>
          <p className="text-white/50 mb-8">Sign in to continue your learning journey.</p>

          {/* Google Sign-In — only mounted when VITE_GOOGLE_CLIENT_ID is configured */}
          {GOOGLE_CLIENT_ID && (
            <div className="mb-6">
              <GoogleSignInButton
                onSuccess={handleGoogleSuccess}
                onError={() => toast.error('Google Sign-In was cancelled or failed.')}
                loading={isGoogleLoading}
              />
              <div className="flex items-center gap-3 mt-4 mb-2">
                <div className="flex-1 h-px bg-white/10" />
                <span className="text-white/30 text-xs">or sign in with email</span>
                <div className="flex-1 h-px bg-white/10" />
              </div>
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5" autoComplete="off">
            <div>
              <label className="block text-sm font-medium text-white/70 mb-1.5">Email</label>
              <input
                {...register('email')}
                type="email"
                placeholder="you@example.com"
                autoComplete="email"
                className={cn('input w-full', errors.email && 'border-red-500/50')}
              />
              {errors.email && (
                <p className="text-red-400 text-xs mt-1">{errors.email.message}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-white/70 mb-1.5">Password</label>
              <div className="relative">
                <input
                  {...register('password')}
                  type={showPw ? 'text' : 'password'}
                  placeholder="••••••••"
                  autoComplete="new-password"
                  className={cn('input w-full pr-10', errors.password && 'border-red-500/50')}
                />
                <button
                  type="button"
                  onClick={() => setShowPw(!showPw)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-white/30 hover:text-white/70 transition-colors"
                >
                  {showPw ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {errors.password && (
                <p className="text-red-400 text-xs mt-1">{errors.password.message}</p>
              )}
            </div>

            <div className="flex items-center justify-between">
              <label className="flex items-center gap-2 text-sm text-white/50 cursor-pointer">
                <input type="checkbox" className="rounded" /> Remember me
              </label>
              <Link
                to="/forgot-password"
                className="text-sm text-brand-400 hover:text-brand-300 transition-colors"
              >
                Forgot password?
              </Link>
            </div>

            <CaptchaWidget onVerify={handleCaptchaVerify} />

            <button
              type="submit"
              disabled={isSubmitting || !captchaToken}
              className="btn-primary w-full flex items-center justify-center gap-2 py-3 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isSubmitting ? (
                <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <>
                  Sign in <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>


          <p className="mt-10 text-center text-white/15 text-xs">
            © {new Date().getFullYear()} Ai Nexus Innovation Hub Pvt Ltd. All rights reserved.
          </p>
          </>
          )}
        </motion.div>
      </div>
    </div>
  );
}
