import { useState, useCallback, useEffect } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { motion, AnimatePresence } from 'framer-motion';
import {
  BookOpen, Sparkles, ArrowRight, Eye, EyeOff, ShieldCheck,
  Brain, Bot, Compass, Zap, Users, BarChart3, FlaskConical, Bell,
  ChevronLeft, ChevronRight,
} from 'lucide-react';
import { toast } from 'sonner';
import CaptchaWidget from '../../components/CaptchaWidget';
import GoogleSignInButton from '../../components/GoogleSignInButton';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { cn } from '../../lib/utils';

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID ?? '';
const GITHUB_CLIENT_ID = import.meta.env.VITE_GITHUB_CLIENT_ID ?? '';

const FEATURES = [
  {
    icon: Brain,
    label: 'Psychometric Intelligence',
    from: 'from-violet-500',
    to: 'to-purple-600',
    text: 'text-violet-400',
    ring: 'bg-violet-500/20 border-violet-500/30',
    brief: 'Big Five personality profiling, RIASEC career codes, and learning style analysis — one deep assessment that shapes your entire academic journey.',
    stat: '3 assessments · 1 platform',
  },
  {
    icon: Bot,
    label: 'AI Study Mentor',
    from: 'from-cyan-500',
    to: 'to-blue-600',
    text: 'text-cyan-400',
    ring: 'bg-cyan-500/20 border-cyan-500/30',
    brief: '24/7 adaptive AI that generates personalised study plans, resolves doubts instantly, and recalibrates your focus around real performance gaps.',
    stat: '24 / 7 availability',
  },
  {
    icon: Compass,
    label: 'Career Oracle',
    from: 'from-amber-500',
    to: 'to-orange-600',
    text: 'text-amber-400',
    ring: 'bg-amber-500/20 border-amber-500/30',
    brief: 'Match your aptitude profile to 100+ career paths with college predictions, entrance exam alignment, and step-by-step career roadmaps.',
    stat: '100+ career paths mapped',
  },
  {
    icon: Zap,
    label: 'Adaptive Assessments',
    from: 'from-green-500',
    to: 'to-emerald-600',
    text: 'text-green-400',
    ring: 'bg-green-500/20 border-green-500/30',
    brief: 'CAT-mode exams that adjust difficulty after every answer — maximum accuracy in minimum questions to measure your true mastery level.',
    stat: 'CAT-mode precision',
  },
  {
    icon: Users,
    label: 'Parent Copilot',
    from: 'from-rose-500',
    to: 'to-pink-600',
    text: 'text-rose-400',
    ring: 'bg-rose-500/20 border-rose-500/30',
    brief: 'AI assistant that keeps parents in the loop — child performance summaries, weak area alerts, fee status, and next exam countdown, conversationally.',
    stat: 'Real-time parent insights',
  },
  {
    icon: BarChart3,
    label: 'Live Readiness Score',
    from: 'from-indigo-500',
    to: 'to-blue-700',
    text: 'text-indigo-400',
    ring: 'bg-indigo-500/20 border-indigo-500/30',
    brief: 'Exam Readiness Score (ERS) computed live across study hours, mock performance, doubt resolution, and assignment grades — always accurate.',
    stat: 'Live ERS · always current',
  },
  {
    icon: FlaskConical,
    label: 'AI Project Lab',
    from: 'from-teal-500',
    to: 'to-cyan-600',
    text: 'text-teal-400',
    ring: 'bg-teal-500/20 border-teal-500/30',
    brief: 'Conversational AI for science, technology, and research project ideation — from concept to full execution plan, guided step by step.',
    stat: 'Unlimited project scope',
  },
  {
    icon: Bell,
    label: 'Real-time Intelligence',
    from: 'from-yellow-500',
    to: 'to-amber-600',
    text: 'text-yellow-400',
    ring: 'bg-yellow-500/20 border-yellow-500/30',
    brief: 'Instant SSE + SMS alerts for assignments, exam results, grades, and milestones — so you and your parents never miss a critical academic moment.',
    stat: 'SSE + SMS · zero latency',
  },
] as const;

const schema = z.object({
  email: z.string().email('Invalid email'),
  password: z.string().min(6, 'Password too short'),
});

type FormData = z.infer<typeof schema>;

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const setAuth = useAuthStore((s) => s.setAuth);
  const [showPw, setShowPw] = useState(false);
  const [captchaToken, setCaptchaToken] = useState<string | null>(null);
  const [isGoogleLoading, setIsGoogleLoading] = useState(false);
  const [isGithubLoading, setIsGithubLoading] = useState(false);
  const [activeFeature, setActiveFeature] = useState(0);
  const [direction, setDirection] = useState(1);
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

  // Auto-advance carousel every 4 seconds
  useEffect(() => {
    const timer = setInterval(() => {
      setDirection(1);
      setActiveFeature((prev) => (prev + 1) % FEATURES.length);
    }, 4000);
    return () => clearInterval(timer);
  }, []);

  // GitHub OAuth callback — detect ?code=&state= on return from GitHub
  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const code = params.get('code');
    const state = params.get('state');
    const saved = sessionStorage.getItem('github_oauth_state');
    if (code && state && saved && state === saved) {
      sessionStorage.removeItem('github_oauth_state');
      handleGithubCallback(code);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function goToFeature(idx: number) {
    setDirection(idx > activeFeature ? 1 : -1);
    setActiveFeature(idx);
  }

  function prevFeature() {
    setDirection(-1);
    setActiveFeature((prev) => (prev - 1 + FEATURES.length) % FEATURES.length);
  }

  function nextFeature() {
    setDirection(1);
    setActiveFeature((prev) => (prev + 1) % FEATURES.length);
  }

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
      if (u.role === 'CENTER_ADMIN' || u.role === 'INSTITUTION_ADMIN' || u.role === 'SUPER_ADMIN') navigate('/admin');
      else if (u.role === 'PARENT') navigate('/parent');
      else if (u.role === 'TEACHER') navigate('/mentor-portal');
      else navigate('/dashboard');
    } catch {
      toast.error('Google Sign-In failed. Please try again.');
    } finally {
      setIsGoogleLoading(false);
    }
  }

  function initiateGithubLogin() {
    if (!GITHUB_CLIENT_ID) {
      toast.info('GitHub Sign-In is not configured on this server.');
      return;
    }
    const state = crypto.randomUUID();
    sessionStorage.setItem('github_oauth_state', state);
    window.location.href = `https://github.com/login/oauth/authorize?client_id=${GITHUB_CLIENT_ID}&scope=user:email&state=${state}`;
  }

  async function handleGithubCallback(code: string) {
    setIsGithubLoading(true);
    try {
      const deviceId = crypto.randomUUID();
      const res = await api.post('/api/v1/auth/github', { code }, {
        headers: { 'X-Device-Id': deviceId },
      });
      const { accessToken: jwt, refreshToken } = res.data;
      const meRes = await api.get('/api/v1/auth/me', {
        headers: { Authorization: `Bearer ${jwt}` },
      });
      const u = meRes.data;
      const name = [u.firstName, u.lastName].filter(Boolean).join(' ') || u.email;
      setAuth(jwt, { id: u.id, email: u.email, role: u.role, name, centerId: u.centerId ?? undefined }, refreshToken, deviceId);
      toast.success('Signed in with GitHub!');
      if (u.role === 'CENTER_ADMIN' || u.role === 'INSTITUTION_ADMIN' || u.role === 'SUPER_ADMIN') navigate('/admin');
      else if (u.role === 'PARENT') navigate('/parent');
      else if (u.role === 'TEACHER') navigate('/mentor-portal');
      else navigate('/dashboard');
    } catch {
      toast.error('GitHub Sign-In failed. Please try again.');
    } finally {
      setIsGithubLoading(false);
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
    if (u.role === 'CENTER_ADMIN' || u.role === 'INSTITUTION_ADMIN' || u.role === 'SUPER_ADMIN') navigate('/admin');
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

  const feature = FEATURES[activeFeature];
  const FeatureIcon = feature.icon;

  const slideVariants = {
    enter: (dir: number) => ({ opacity: 0, x: dir * 40, scale: 0.97 }),
    center: { opacity: 1, x: 0, scale: 1 },
    exit: (dir: number) => ({ opacity: 0, x: dir * -40, scale: 0.97 }),
  };


  return (
    <div className="min-h-screen bg-surface flex">
      {/* Left panel */}
      <div className="hidden lg:flex flex-1 relative overflow-hidden bg-gradient-to-br from-brand-950 via-surface to-surface items-center justify-center p-12">
        {/* Ambient blobs */}
        <div className="absolute top-1/4 left-1/4 w-64 h-64 bg-brand-600/20 rounded-full blur-3xl animate-pulse" />
        <div className="absolute bottom-1/3 right-1/4 w-48 h-48 bg-violet-600/20 rounded-full blur-3xl animate-pulse delay-700" />
        <div className="absolute top-1/2 right-1/3 w-32 h-32 bg-cyan-600/15 rounded-full blur-2xl animate-pulse delay-1000" />

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
          className="relative z-10 max-w-md w-full text-center"
        >
          {/* Static header */}
          <div className="flex justify-center mb-6">
            <div className="p-4 rounded-2xl bg-brand-600/20 border border-brand-500/30 animate-pulse-glow">
              <Sparkles className="w-10 h-10 text-brand-400" />
            </div>
          </div>
          <h1 className="text-4xl font-bold text-white mb-4">
            Your AI-powered<br />
            <span className="gradient-text">study companion</span>
          </h1>
          <p className="text-white/50 text-base leading-relaxed mb-10">
            Personalised study plans, real-time career guidance, and adaptive
            assessments — all in one platform.
          </p>

          {/* Feature showcase carousel */}
          <div className="relative">
            <div className="glass rounded-2xl p-6 min-h-[220px] overflow-hidden">
              <AnimatePresence mode="wait" custom={direction}>
                <motion.div
                  key={activeFeature}
                  custom={direction}
                  variants={slideVariants}
                  initial="enter"
                  animate="center"
                  exit="exit"
                  transition={{ duration: 0.4, ease: [0.25, 0.46, 0.45, 0.94] }}
                  className="flex flex-col items-center text-center"
                >
                  {/* Icon ring */}
                  <div className={cn('p-3 rounded-2xl border mb-4', feature.ring)}>
                    <FeatureIcon className={cn('w-8 h-8', feature.text)} />
                  </div>

                  {/* Feature title — gradient text */}
                  <h3 className={cn(
                    'text-xl font-bold mb-3 bg-gradient-to-r bg-clip-text text-transparent',
                    feature.from,
                    feature.to,
                  )}>
                    {feature.label}
                  </h3>

                  {/* Brief description */}
                  <p className="text-white/60 text-sm leading-relaxed mb-4">
                    {feature.brief}
                  </p>

                  {/* Stat badge */}
                  <span className={cn(
                    'text-xs font-semibold px-3 py-1 rounded-full border',
                    feature.ring,
                    feature.text,
                  )}>
                    {feature.stat}
                  </span>
                </motion.div>
              </AnimatePresence>
            </div>

            {/* Prev arrow */}
            <button
              type="button"
              onClick={prevFeature}
              className="absolute left-0 top-1/2 -translate-y-1/2 -translate-x-4 p-1.5 rounded-full bg-white/5 hover:bg-white/15 border border-white/10 text-white/40 hover:text-white/80 transition-all"
              aria-label="Previous feature"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>

            {/* Next arrow */}
            <button
              type="button"
              onClick={nextFeature}
              className="absolute right-0 top-1/2 -translate-y-1/2 translate-x-4 p-1.5 rounded-full bg-white/5 hover:bg-white/15 border border-white/10 text-white/40 hover:text-white/80 transition-all"
              aria-label="Next feature"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>

          {/* Dot navigation */}
          <div className="flex justify-center gap-1.5 mt-5">
            {FEATURES.map((_, i) => (
              <button
                key={i}
                type="button"
                onClick={() => goToFeature(i)}
                aria-label={`Feature ${i + 1}`}
                className={cn(
                  'rounded-full transition-all duration-300',
                  i === activeFeature
                    ? 'w-6 h-2 bg-brand-400'
                    : 'w-2 h-2 bg-white/20 hover:bg-white/40',
                )}
              />
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

              {/* Social sign-in — always visible; shows toast when provider not configured */}
              <div className="mb-6 space-y-3">
                <div className="grid grid-cols-2 gap-3">
                  {GOOGLE_CLIENT_ID ? (
                    <GoogleSignInButton
                      onSuccess={handleGoogleSuccess}
                      onError={() => toast.error('Google Sign-In was cancelled or failed.')}
                      loading={isGoogleLoading}
                      label="Google"
                    />
                  ) : (
                    <button
                      type="button"
                      onClick={() => toast.info('Google Sign-In is not configured on this server.')}
                      className="flex items-center justify-center gap-2.5 px-4 py-3 rounded-xl border border-white/20 bg-white/5 hover:bg-white/10 text-white text-sm font-medium transition-all"
                    >
                      <svg className="w-4 h-4 shrink-0" viewBox="0 0 24 24" aria-hidden="true">
                        <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                        <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                        <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                        <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                      </svg>
                      Google
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={initiateGithubLogin}
                    disabled={isGithubLoading}
                    className="flex items-center justify-center gap-2.5 px-4 py-3 rounded-xl border border-white/20 bg-white/5 hover:bg-white/10 text-white text-sm font-medium transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {isGithubLoading ? (
                      <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    ) : (
                      <svg className="w-4 h-4 shrink-0" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                        <path fillRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" clipRule="evenodd" />
                      </svg>
                    )}
                    GitHub
                  </button>
                </div>
                <div className="flex items-center gap-3">
                  <div className="flex-1 h-px bg-white/10" />
                  <span className="text-white/30 text-xs">or sign in with email</span>
                  <div className="flex-1 h-px bg-white/10" />
                </div>
              </div>

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

              <p className="mt-6 text-center text-white/40 text-sm">
                Don&apos;t have an account?{' '}
                <Link
                  to="/register"
                  className="text-brand-400 hover:text-brand-300 font-medium transition-colors"
                >
                  Create one
                </Link>
              </p>

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
