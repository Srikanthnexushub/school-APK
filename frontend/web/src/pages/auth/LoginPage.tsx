import React, { useState, useCallback, useEffect } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Sparkles, ArrowRight, Eye, EyeOff, ShieldCheck,
  Brain, Bot, Compass, Zap, Users, BarChart3, FlaskConical, Bell,
} from 'lucide-react';
import NexusEdLogo from '../../components/NexusEdLogo';
import { toast } from 'sonner';
import CaptchaWidget from '../../components/CaptchaWidget';
import GoogleSignInButton from '../../components/GoogleSignInButton';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { cn, randomUUID } from '../../lib/utils';

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID ?? '';
const GITHUB_CLIENT_ID = import.meta.env.VITE_GITHUB_CLIENT_ID ?? '';

const FEATURES = [
  {
    icon: Brain,
    label: 'Psychometric Intelligence',
    pageBg: 'linear-gradient(148deg, #818cf8 0%, #6366f1 50%, #4f46e5 100%)',
    brief: 'Big Five personality profiling, RIASEC career codes, and learning style analysis — one deep assessment that shapes your entire academic journey.',
    stat: '3 assessments · 1 platform',
  },
  {
    icon: Bot,
    label: 'AI Study Mentor',
    pageBg: 'linear-gradient(148deg, #38bdf8 0%, #0ea5e9 50%, #2563eb 100%)',
    brief: '24/7 adaptive AI that generates personalised study plans, resolves doubts instantly, and recalibrates your focus around real performance gaps.',
    stat: '24 / 7 availability',
  },
  {
    icon: Compass,
    label: 'Career Oracle',
    pageBg: 'linear-gradient(148deg, #fb923c 0%, #f97316 50%, #ea580c 100%)',
    brief: 'Match your aptitude profile to 100+ career paths with college predictions, entrance exam alignment, and step-by-step career roadmaps.',
    stat: '100+ career paths mapped',
  },
  {
    icon: Zap,
    label: 'Adaptive Assessments',
    pageBg: 'linear-gradient(148deg, #4ade80 0%, #22c55e 50%, #16a34a 100%)',
    brief: 'CAT-mode exams that adjust difficulty after every answer — maximum accuracy in minimum questions to measure your true mastery level.',
    stat: 'CAT-mode precision',
  },
  {
    icon: Users,
    label: 'Parent Copilot',
    pageBg: 'linear-gradient(148deg, #f472b6 0%, #ec4899 50%, #db2777 100%)',
    brief: 'AI assistant that keeps parents in the loop — child performance summaries, weak area alerts, fee status, and next exam countdown, conversationally.',
    stat: 'Real-time parent insights',
  },
  {
    icon: BarChart3,
    label: 'Live Readiness Score',
    pageBg: 'linear-gradient(148deg, #60a5fa 0%, #3b82f6 50%, #1d4ed8 100%)',
    brief: 'Exam Readiness Score (ERS) computed live across study hours, mock performance, doubt resolution, and assignment grades — always accurate.',
    stat: 'Live ERS · always current',
  },
  {
    icon: FlaskConical,
    label: 'AI Project Lab',
    pageBg: 'linear-gradient(148deg, #2dd4bf 0%, #14b8a6 50%, #0d9488 100%)',
    brief: 'Conversational AI for science, technology, and research project ideation — from concept to full execution plan, guided step by step.',
    stat: 'Unlimited project scope',
  },
  {
    icon: Bell,
    label: 'Real-time Intelligence',
    pageBg: 'linear-gradient(148deg, #fbbf24 0%, #f59e0b 50%, #d97706 100%)',
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
  const [captchaKey, setCaptchaKey] = useState(0);
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

  async function handleGoogleSuccess(accessToken: string) {
    setIsGoogleLoading(true);
    try {
      const deviceId = randomUUID();
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
    const state = randomUUID();
    sessionStorage.setItem('github_oauth_state', state);
    window.location.href = `https://github.com/login/oauth/authorize?client_id=${GITHUB_CLIENT_ID}&scope=user:email&state=${state}`;
  }

  async function handleGithubCallback(code: string) {
    setIsGithubLoading(true);
    try {
      const deviceId = randomUUID();
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
      const deviceId = randomUUID();
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
      setCaptchaKey((k) => k + 1); // force CaptchaWidget remount → fresh challenge
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
  const FeatureIcon = feature.icon as React.ElementType;

  // Physical page-turn — FORWARD direction (like lifting and turning a real page).
  // Both enter and exit use the SAME rotational direction so the page arcs TOWARD the viewer.
  // Exit:  0 → -180  (right edge lifts toward viewer, folds left — forward turn)
  // Enter: -180 → 0  (new page opens from the same forward direction)
  // backfaceVisibility:hidden hides each page when it passes 90° (showing its back face).
  const pageVariants = {
    enter: (dir: number) => ({ rotateY: dir > 0 ? -180 : 180 }),
    center: { rotateY: 0 },
    exit: (dir: number) => ({ rotateY: dir > 0 ? -180 : 180 }),
  };


  return (
    <div className="min-h-screen bg-surface flex flex-col lg:flex-row">
      {/* Left panel */}
      <div className="login-panel-left flex lg:flex-1 relative overflow-hidden bg-gradient-to-br from-brand-950 via-slate-950 to-indigo-950 items-center justify-center px-8 py-10 lg:p-12">
        {/* Ambient blobs */}
        <div className="absolute top-1/4 left-1/4 w-72 h-72 bg-brand-600/20 rounded-full blur-3xl animate-pulse" />
        <div className="absolute bottom-1/3 right-1/4 w-56 h-56 bg-violet-600/20 rounded-full blur-3xl animate-pulse delay-700" />
        <div className="absolute top-1/2 right-1/3 w-40 h-40 bg-cyan-600/15 rounded-full blur-2xl animate-pulse delay-1000" />

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
          className="relative z-10 max-w-md w-full text-center"
        >
          {/* Header */}
          <div className="flex justify-center mb-4 lg:mb-6">
            <div className="p-3 lg:p-4 rounded-2xl bg-brand-600/20 border border-brand-500/30 animate-pulse-glow">
              <Sparkles className="w-7 h-7 lg:w-10 lg:h-10 text-brand-400" />
            </div>
          </div>
          <h1 className="text-2xl lg:text-4xl font-bold text-white mb-3 lg:mb-4">
            Your AI-powered<br />
            <span className="gradient-text">study companion</span>
          </h1>
          <p className="text-white/50 text-sm lg:text-base leading-relaxed mb-6 lg:mb-10">
            Personalised study plans, real-time career guidance, and adaptive
            assessments — all in one platform.
          </p>

          {/* ── Open Book — straight, no tilt — desktop only ── */}
          <div className="mt-2 hidden lg:flex justify-center">
            <div
              style={{
                display: 'inline-block',
                filter: 'drop-shadow(0 20px 40px rgba(0,0,0,0.45)) drop-shadow(0 4px 12px rgba(99,102,241,0.25))',
              }}
            >
              {/* Book pages — left + spine + right */}
              <div style={{ display: 'flex', position: 'relative' }}>

                {/* ── Left page (static, plain glossy) ── */}
                <div
                  style={{
                    width: 178, height: 260,
                    background: 'linear-gradient(148deg, #f472b6 0%, #ec4899 50%, #be185d 100%)',
                    borderRadius: '14px 0 0 14px',
                    position: 'relative',
                    overflow: 'hidden',
                    flexShrink: 0,
                  }}
                >
                  {/* Main gloss — large top-left shine */}
                  <div style={{
                    position: 'absolute', inset: 0,
                    background: 'linear-gradient(142deg, rgba(255,255,255,0.55) 0%, rgba(255,255,255,0.20) 30%, transparent 60%)',
                    borderRadius: 'inherit', zIndex: 2, pointerEvents: 'none',
                  }} />
                  {/* Subtle bottom reflection */}
                  <div style={{
                    position: 'absolute', bottom: 0, left: 0, right: 0, height: '35%',
                    background: 'linear-gradient(0deg, rgba(255,255,255,0.10) 0%, transparent 100%)',
                    zIndex: 1, pointerEvents: 'none',
                  }} />
                  {/* Inner rim shadow near spine */}
                  <div style={{
                    position: 'absolute', top: 0, right: 0, bottom: 0, width: 22,
                    background: 'linear-gradient(90deg, transparent, rgba(0,0,0,0.16))',
                    zIndex: 1,
                  }} />
                </div>

                {/* ── Spine (book binding) ── */}
                <div
                  style={{
                    width: 20, height: 260, flexShrink: 0,
                    background: 'linear-gradient(180deg, #f9a8d4 0%, #ec4899 40%, #9d174d 100%)',
                    boxShadow: 'inset -3px 0 6px rgba(0,0,0,0.30), inset 2px 0 4px rgba(255,255,255,0.25)',
                    position: 'relative',
                  }}
                >
                  {[20, 40, 60, 80].map(pct => (
                    <div key={pct} style={{
                      position: 'absolute', left: 3, right: 3, top: `${pct}%`,
                      height: 1.5, background: 'rgba(255,255,255,0.30)', borderRadius: 1,
                    }} />
                  ))}
                </div>

                {/* ── Right page (physical page-turn) ── */}
                {/* Container is just a size/position frame — no background, no overflow hidden */}
                {/* perspective + perspectiveOrigin here so rotateY looks like a page flipping around the spine */}
                <div
                  style={{
                    width: 178, height: 260,
                    position: 'relative',
                    flexShrink: 0,
                    perspective: '600px',
                    perspectiveOrigin: '0% 50%',
                  }}
                >
                  {/* The ENTIRE page (background + gloss + star + content) is the rotating element */}
                  <AnimatePresence mode="sync" initial={false} custom={direction}>
                    <motion.div
                      key={activeFeature}
                      custom={direction}
                      variants={pageVariants}
                      initial="enter"
                      animate="center"
                      exit="exit"
                      transition={{ duration: 1.1, ease: [0.25, 0.1, 0.25, 1] }}
                      style={{
                        position: 'absolute', inset: 0,
                        background: feature.pageBg,
                        borderRadius: '0 14px 14px 0',
                        transformOrigin: '0% 50%',
                        backfaceVisibility: 'hidden',
                        overflow: 'hidden',
                      }}
                    >
                      {/* Specular gloss */}
                      <div style={{
                        position: 'absolute', inset: 0,
                        background: 'linear-gradient(142deg, rgba(255,255,255,0.45) 0%, rgba(255,255,255,0.12) 32%, transparent 60%)',
                        borderRadius: 'inherit', zIndex: 2, pointerEvents: 'none',
                      }} />
                      {/* Inner rim shadow near spine */}
                      <div style={{
                        position: 'absolute', top: 0, left: 0, bottom: 0, width: 16,
                        background: 'linear-gradient(90deg, rgba(0,0,0,0.18), transparent)',
                        zIndex: 2, pointerEvents: 'none',
                      }} />
                      {/* Amber star */}
                      <div style={{ position: 'absolute', top: 11, right: 13, zIndex: 3, pointerEvents: 'none' }}>
                        <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
                          <path
                            d="M9 1.5l2.09 4.24 4.68.68-3.39 3.3.8 4.66L9 12.27l-4.18 2.2.8-4.66L2.23 6.42l4.68-.68L9 1.5z"
                            fill="#fef08a" stroke="rgba(254,240,138,0.4)" strokeWidth="0.5"
                          />
                        </svg>
                      </div>
                      {/* Feature content — white text on all vibrant backgrounds */}
                      <div className="absolute inset-0 z-10 flex flex-col items-center text-center px-4 pt-5 pb-4">
                        <div style={{ padding: '10px', borderRadius: '12px', border: '1px solid rgba(255,255,255,0.35)', background: 'rgba(255,255,255,0.20)', marginBottom: '10px' }}>
                          <FeatureIcon style={{ width: 24, height: 24, color: '#fff' }} />
                        </div>
                        <h3 style={{ fontSize: '13px', fontWeight: 700, color: '#fff', marginBottom: '8px', lineHeight: 1.3 }}>
                          {feature.label}
                        </h3>
                        <p style={{ color: 'rgba(255,255,255,0.85)', fontSize: '11px', lineHeight: 1.6, marginBottom: '12px', display: '-webkit-box', WebkitLineClamp: 4, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                          {feature.brief}
                        </p>
                        <span style={{ fontSize: '10px', fontWeight: 600, padding: '4px 10px', borderRadius: '999px', border: '1px solid rgba(255,255,255,0.35)', background: 'rgba(255,255,255,0.20)', color: '#fff' }}>
                          {feature.stat}
                        </span>
                      </div>
                    </motion.div>
                  </AnimatePresence>
                </div>
              </div>

              {/* ── Bottom edge — shows page stack depth ── */}
              <div style={{
                position: 'absolute', bottom: -8, left: 14, right: 12, height: 8,
                background: 'linear-gradient(180deg, rgba(255,255,255,0.14) 0%, rgba(255,255,255,0.04) 100%)',
                borderRadius: '0 0 8px 8px',
              }} />

              {/* ── Right edge strips — stacked pages illusion ── */}
              {[0,1,2,3,4,5,6].map(i => (
                <div key={i} style={{
                  position: 'absolute',
                  right: -(i+1) * 2.4,
                  top: 5 + i,
                  bottom: 5 + i,
                  width: 2,
                  background: `rgba(99,102,241,${0.18 - i * 0.02})`,
                  borderRadius: 1,
                }} />
              ))}

              {/* ── Left edge strips — left page thickness ── */}
              {[0,1,2].map(i => (
                <div key={i} style={{
                  position: 'absolute',
                  left: -(i+1) * 2,
                  top: 6 + i,
                  bottom: 6 + i,
                  width: 2,
                  background: `rgba(236,72,153,${0.28 - i * 0.08})`,
                  borderRadius: 1,
                }} />
              ))}
            </div>
          </div>
        </motion.div>
      </div>

      {/* Right panel — form */}
      <div className="login-panel-left flex-1 flex items-center justify-center p-8 bg-gradient-to-br from-brand-950 via-slate-950 to-indigo-950">
        <motion.div
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.5 }}
          className="w-full max-w-md"
        >
          <div className="flex items-center gap-3 mb-10">
            <NexusEdLogo size={28} />
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

                <CaptchaWidget key={captchaKey} onVerify={handleCaptchaVerify} />

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
