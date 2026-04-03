import React, { useState, useCallback, useEffect } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Sparkles, ArrowRight, Eye, EyeOff, ShieldCheck,
  Brain, Bot, Compass, Zap, Users, BarChart3, FlaskConical, Bell,
  Sun, Moon,
} from 'lucide-react';
import NexusEdLogo from '../../components/NexusEdLogo';
import { toast } from 'sonner';
import CaptchaWidget from '../../components/CaptchaWidget';
import GoogleSignInButton from '../../components/GoogleSignInButton';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { useThemeStore } from '../../stores/themeStore';
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
  const { theme, toggleTheme } = useThemeStore();
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

  // Book page-turn — right page flips left (back face = next feature sweeps over left page, lingers), then new page opens from spine.
  // Exit:  0 → -180  slow-eased (lingers near -180 so back face is visible over left page briefly)
  // Enter: 90 → 0    small delay (gap between exit completing and new page opening)
  const pageVariants = {
    enter: { rotateY: 90 },
    center: { rotateY: 0, transition: { duration: 0.5, ease: [0.25, 0.1, 0.25, 1] } },
    exit:  { rotateY: -180, transition: { duration: 0.85, ease: [0.4, 0, 0.08, 1] } },
  };


  return (
    <div className="min-h-screen flex flex-col lg:flex-row items-stretch relative overflow-hidden" style={{ background: 'rgb(2,4,12)' }}>
      {/* Ambient aurora blobs — behind everything */}
      <div className="absolute inset-0 pointer-events-none" aria-hidden="true">
        <div className="absolute -top-60 -left-60 w-[700px] h-[700px] rounded-full" style={{ background: 'radial-gradient(circle, rgba(0,245,255,0.07) 0%, transparent 70%)', filter: 'blur(80px)' }} />
        <div className="absolute -bottom-60 -right-60 w-[600px] h-[600px] rounded-full" style={{ background: 'radial-gradient(circle, rgba(155,0,255,0.09) 0%, transparent 70%)', filter: 'blur(100px)' }} />
        <div className="absolute top-1/2 -translate-y-1/2 left-1/3 w-[400px] h-[400px] rounded-full" style={{ background: 'radial-gradient(circle, rgba(240,0,255,0.05) 0%, transparent 70%)', filter: 'blur(60px)' }} />
        {/* Tech grid */}
        <div className="absolute inset-0 opacity-[0.025]" style={{ backgroundImage: 'linear-gradient(rgba(0,245,255,1) 1px, transparent 1px), linear-gradient(90deg, rgba(0,245,255,1) 1px, transparent 1px)', backgroundSize: '60px 60px' }} />
      </div>

      {/* Theme toggle */}
      <motion.button
        onClick={toggleTheme}
        whileTap={{ scale: 0.88 }}
        className="absolute top-5 right-5 z-50 p-2.5 rounded-xl text-white/50 hover:text-white/90 transition-all"
        style={{ background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.10)', backdropFilter: 'blur(8px)' }}
        aria-label="Toggle theme"
      >
        <AnimatePresence mode="wait" initial={false}>
          {theme === 'dark' ? (
            <motion.span key="moon" initial={{ rotate: -30, opacity: 0, scale: 0.6 }} animate={{ rotate: 0, opacity: 1, scale: 1 }} exit={{ rotate: 30, opacity: 0, scale: 0.6 }} transition={{ duration: 0.22 }} className="flex">
              <Moon className="w-4 h-4" />
            </motion.span>
          ) : (
            <motion.span key="sun" initial={{ rotate: 30, opacity: 0, scale: 0.6 }} animate={{ rotate: 0, opacity: 1, scale: 1 }} exit={{ rotate: -30, opacity: 0, scale: 0.6 }} transition={{ duration: 0.22 }} className="flex">
              <Sun className="w-4 h-4 text-amber-400" />
            </motion.span>
          )}
        </AnimatePresence>
      </motion.button>

      {/* ── LEFT PANEL: Neural Command Center ─────────────────────────── */}
      <div className="flex flex-col items-center justify-center lg:flex-1 relative px-5 py-5 lg:px-12 lg:py-16 overflow-hidden">

        {/* Platform identity */}
        <motion.div
          initial={{ opacity: 0, y: -24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, delay: 0.2 }}
          className="text-center mb-3 lg:mb-8 relative z-10"
        >
          <div className="flex items-center justify-center gap-3 mb-3">
            <NexusEdLogo size={38} className="flex-shrink-0" />
            <h1 className="text-3xl lg:text-5xl font-black tracking-tight leading-none">
              <span className="neon-text-cyan">NEXUS</span>
              <span className="text-white">ED</span>
            </h1>
          </div>
          <p className="text-[11px] font-mono tracking-[0.35em] uppercase" style={{ color: 'rgba(0,245,255,0.55)' }}>
            Neural · AI · Platform
          </p>
        </motion.div>

        {/* ── Mobile-only: active feature badge + compact stats ── */}
        <div className="flex lg:hidden flex-col items-center gap-2 relative z-10 mb-1">
          <AnimatePresence mode="wait">
            <motion.div
              key={activeFeature}
              initial={{ opacity: 0, y: 5 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -5 }}
              transition={{ duration: 0.3 }}
              className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full"
              style={{ background: 'rgba(0,245,255,0.08)', border: '1px solid rgba(0,245,255,0.22)' }}
            >
              <FeatureIcon style={{ width: 12, height: 12, color: '#00f5ff' }} />
              <span className="text-[11px] font-semibold" style={{ color: 'rgba(0,245,255,0.85)' }}>{feature.label}</span>
            </motion.div>
          </AnimatePresence>
          <div className="flex items-center gap-5 mt-1">
            {[{ value: '22+', label: 'AI Modules' }, { value: '100+', label: 'Career Paths' }, { value: '24/7', label: 'Always On' }].map(({ value, label }) => (
              <div key={label} className="text-center">
                <div className="text-sm font-black neon-text-cyan">{value}</div>
                <div className="text-[9px] font-mono uppercase tracking-wider" style={{ color: 'rgba(255,255,255,0.35)' }}>{label}</div>
              </div>
            ))}
          </div>
        </div>

        {/* ── Orbital Feature Universe + Feature Card + Dots (desktop only) ── */}
        <div className="hidden lg:flex lg:flex-col lg:items-center">
        <div className="relative flex items-center justify-center mb-10" style={{ width: 380, height: 380 }}>
          {/* Orbital rings */}
          <div className="orbital-ring-1" />
          <div className="orbital-ring-2" />
          <div className="orbital-ring-3" />

          {/* Central glowing orb */}
          <div className="feature-orb">
            <AnimatePresence mode="wait">
              <motion.div
                key={activeFeature}
                initial={{ scale: 0.4, opacity: 0, rotate: -20 }}
                animate={{ scale: 1, opacity: 1, rotate: 0 }}
                exit={{ scale: 0.4, opacity: 0, rotate: 20 }}
                transition={{ duration: 0.45, ease: [0.23, 1, 0.32, 1] }}
              >
                <FeatureIcon style={{ width: 52, height: 52, color: '#00f5ff', filter: 'drop-shadow(0 0 12px #00f5ff)' }} />
              </motion.div>
            </AnimatePresence>
          </div>

          {/* Orbiting feature icons — 8 icons around orbit-ring-2 (r=130px) */}
          {FEATURES.map((f, i) => {
            const FIcon = f.icon as React.ElementType;
            const isActive = i === activeFeature;
            const orbitDelay = -((i / FEATURES.length) * 20);
            return (
              <div
                key={i}
                className="orbit-icon"
                style={{ animationDelay: `${orbitDelay}s`, animationDuration: '20s' } as React.CSSProperties}
              >
                <button
                  onClick={() => { setDirection(i > activeFeature ? 1 : -1); setActiveFeature(i); }}
                  className="transition-all duration-300"
                  style={{
                    width: 36, height: 36, borderRadius: '50%',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    background: isActive ? 'rgba(0,245,255,0.18)' : 'rgba(2,4,12,0.85)',
                    border: `1px solid ${isActive ? 'rgba(0,245,255,0.65)' : 'rgba(255,255,255,0.15)'}`,
                    boxShadow: isActive ? '0 0 10px rgba(0,245,255,0.5), 0 0 20px rgba(0,245,255,0.2)' : 'none',
                  }}
                >
                  <FIcon style={{ width: 15, height: 15, color: isActive ? '#00f5ff' : 'rgba(255,255,255,0.5)' }} />
                </button>
              </div>
            );
          })}
        </div>

        {/* Feature card */}
        <AnimatePresence mode="wait">
          <motion.div
            key={activeFeature}
            initial={{ opacity: 0, y: 16, scale: 0.96 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -16, scale: 0.96 }}
            transition={{ duration: 0.38, ease: [0.23, 1, 0.32, 1] }}
            className="holo-feature-card max-w-sm w-full text-center relative z-10"
          >
            <p className="text-[10px] font-mono tracking-[0.3em] uppercase mb-2" style={{ color: 'rgba(0,245,255,0.6)' }}>
              FEATURE {String(activeFeature + 1).padStart(2, '0')} / {String(FEATURES.length).padStart(2, '0')}
            </p>
            <h3 className="text-base font-bold text-white mb-2 leading-tight">{feature.label}</h3>
            <p className="text-sm leading-relaxed mb-3" style={{ color: 'rgba(255,255,255,0.58)' }}>{feature.brief}</p>
            <span className="inline-flex items-center gap-1.5 text-[11px] font-semibold px-3 py-1.5 rounded-full" style={{ background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.10)', color: 'rgba(255,255,255,0.65)' }}>
              {feature.stat}
            </span>
          </motion.div>
        </AnimatePresence>

        {/* Dot indicators */}
        <div className="flex items-center gap-2 mt-5 relative z-10">
          {FEATURES.map((_, i) => (
            <button
              key={i}
              onClick={() => { setDirection(i > activeFeature ? 1 : -1); setActiveFeature(i); }}
              className="transition-all duration-300 rounded-full"
              style={{
                width: i === activeFeature ? 24 : 6,
                height: 6,
                background: i === activeFeature ? '#00f5ff' : 'rgba(255,255,255,0.20)',
                boxShadow: i === activeFeature ? '0 0 8px #00f5ff, 0 0 16px rgba(0,245,255,0.4)' : 'none',
              }}
            />
          ))}
        </div>
        </div>{/* end hidden lg:block orbital wrapper */}

        {/* Platform stats — desktop only (absolute positioned within full-height panel) */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 1.2, duration: 0.6 }}
          className="hidden lg:flex absolute bottom-8 left-0 right-0 justify-center gap-8 z-10"
        >
          {[
            { value: '22+', label: 'AI Modules' },
            { value: '100+', label: 'Career Paths' },
            { value: '24/7', label: 'Always On' },
          ].map(({ value, label }) => (
            <div key={label} className="text-center">
              <div className="text-lg font-black neon-text-cyan">{value}</div>
              <div className="text-[10px] font-mono uppercase tracking-wider" style={{ color: 'rgba(255,255,255,0.35)' }}>{label}</div>
            </div>
          ))}
        </motion.div>
      </div>

      {/* ── RIGHT PANEL: Cyber Terminal ────────────────────────────────── */}
      <div className="flex items-center justify-center w-full lg:flex-shrink-0 lg:w-[480px] p-4 lg:p-10 relative z-10">
        <div className="cyber-terminal w-full max-w-md">
          {/* Scan sweep — animates once on mount */}
          <div className="scan-sweep" />
          {/* Corner brackets */}
          <div className="corner-tl" /><div className="corner-tr" />
          <div className="corner-bl" /><div className="corner-br" />

          <div className="p-7 lg:p-8">
            {/* Brand header */}
            <div className="flex items-center gap-3 mb-7">
              <div className="relative flex-shrink-0">
                <div className="absolute inset-0 rounded-full animate-ping opacity-20" style={{ background: 'radial-gradient(circle, rgba(0,245,255,0.6), transparent)' }} />
                <NexusEdLogo size={30} className="relative z-10" />
              </div>
              <div>
                <div className="font-black text-lg leading-none tracking-wide">
                  <span className="neon-text-cyan" style={{ fontSize: 18 }}>NEXUS</span>
                  <span className="text-white" style={{ fontSize: 18 }}>ED</span>
                </div>
                <div className="text-[9px] font-mono tracking-[0.25em] uppercase mt-0.5" style={{ color: 'rgba(0,245,255,0.45)' }}>
                  Identity Verification · v1.0
                </div>
              </div>
            </div>

            {/* ── MFA Step ── */}
            {mfaStep ? (
              <>
                <div className="flex justify-center mb-5">
                  <div className="p-4 rounded-2xl" style={{ background: 'rgba(0,245,255,0.08)', border: '1px solid rgba(0,245,255,0.25)' }}>
                    <ShieldCheck className="w-8 h-8" style={{ color: '#00f5ff' }} />
                  </div>
                </div>
                <h2 className="text-2xl font-bold text-white mb-2">Two-factor verification</h2>
                <p className="text-sm mb-8" style={{ color: 'rgba(255,255,255,0.48)' }}>
                  Enter the 6-digit code from your authenticator app.
                </p>
                <form onSubmit={handleMfaVerify} className="space-y-5">
                  <div>
                    <label className="block text-sm font-medium mb-1.5" style={{ color: 'rgba(255,255,255,0.65)' }}>
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
                    ) : 'Verify & Sign in'}
                  </button>
                </form>
                <button
                  type="button"
                  onClick={() => { setMfaStep(null); setTotpCode(''); setCaptchaToken(null); }}
                  className="mt-4 w-full text-center text-sm transition-colors"
                  style={{ color: 'rgba(255,255,255,0.38)' }}
                  onMouseEnter={e => (e.currentTarget.style.color = 'rgba(255,255,255,0.70)')}
                  onMouseLeave={e => (e.currentTarget.style.color = 'rgba(255,255,255,0.38)')}
                >
                  ← Back to login
                </button>
              </>
            ) : (
              <>
                <h2 className="text-2xl font-bold text-white mb-1">Welcome back</h2>
                <p className="text-sm mb-6" style={{ color: 'rgba(255,255,255,0.45)' }}>
                  Sign in to continue your learning journey.
                </p>

                {/* Social sign-in */}
                <div className="mb-5 space-y-3">
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
                        className="flex items-center justify-center gap-2.5 px-4 py-2.5 rounded-xl text-white text-sm font-medium transition-all"
                        style={{ border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)' }}
                        onMouseEnter={e => { e.currentTarget.style.background = 'rgba(255,255,255,0.08)'; }}
                        onMouseLeave={e => { e.currentTarget.style.background = 'rgba(255,255,255,0.04)'; }}
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
                      className="flex items-center justify-center gap-2.5 px-4 py-2.5 rounded-xl text-white text-sm font-medium transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                      style={{ border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)' }}
                      onMouseEnter={e => { e.currentTarget.style.background = 'rgba(255,255,255,0.08)'; }}
                      onMouseLeave={e => { e.currentTarget.style.background = 'rgba(255,255,255,0.04)'; }}
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
                    <div className="flex-1 h-px" style={{ background: 'rgba(255,255,255,0.08)' }} />
                    <span className="text-xs font-mono" style={{ color: 'rgba(255,255,255,0.28)' }}>or sign in with email</span>
                    <div className="flex-1 h-px" style={{ background: 'rgba(255,255,255,0.08)' }} />
                  </div>
                </div>

                <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" autoComplete="off">
                  <div>
                    <label className="block text-sm font-medium mb-1.5" style={{ color: 'rgba(255,255,255,0.65)' }}>Email</label>
                    <input
                      {...register('email')}
                      type="email"
                      placeholder="you@example.com"
                      autoComplete="email"
                      className={cn('input w-full', errors.email && 'border-red-500/50')}
                    />
                    {errors.email && <p className="text-red-400 text-xs mt-1">{errors.email.message}</p>}
                  </div>

                  <div>
                    <label className="block text-sm font-medium mb-1.5" style={{ color: 'rgba(255,255,255,0.65)' }}>Password</label>
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
                        className="absolute right-3 top-1/2 -translate-y-1/2 transition-colors"
                        style={{ color: 'rgba(255,255,255,0.30)' }}
                        onMouseEnter={e => (e.currentTarget.style.color = 'rgba(255,255,255,0.70)')}
                        onMouseLeave={e => (e.currentTarget.style.color = 'rgba(255,255,255,0.30)')}
                      >
                        {showPw ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>
                    </div>
                    {errors.password && <p className="text-red-400 text-xs mt-1">{errors.password.message}</p>}
                  </div>

                  <div className="flex items-center justify-between">
                    <label className="flex items-center gap-2 text-sm cursor-pointer" style={{ color: 'rgba(255,255,255,0.45)' }}>
                      <input type="checkbox" className="rounded" /> Remember me
                    </label>
                    <Link to="/forgot-password" className="text-sm transition-colors" style={{ color: '#00f5ff' }}
                      onMouseEnter={e => (e.currentTarget.style.color = '#7ffbff')}
                      onMouseLeave={e => (e.currentTarget.style.color = '#00f5ff')}
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
                      <>Sign in <ArrowRight className="w-4 h-4" /></>
                    )}
                  </button>
                </form>

                <p className="mt-5 text-center text-sm" style={{ color: 'rgba(255,255,255,0.38)' }}>
                  Don&apos;t have an account?{' '}
                  <Link to="/register" className="font-medium transition-colors" style={{ color: '#00f5ff' }}
                    onMouseEnter={e => (e.currentTarget.style.color = '#7ffbff')}
                    onMouseLeave={e => (e.currentTarget.style.color = '#00f5ff')}
                  >
                    Create one
                  </Link>
                </p>

                <p className="mt-8 text-center text-[10px]" style={{ color: 'rgba(255,255,255,0.13)' }}>
                  © {new Date().getFullYear()} Ai Nexus Innovation Hub Pvt Ltd. All rights reserved.
                </p>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
