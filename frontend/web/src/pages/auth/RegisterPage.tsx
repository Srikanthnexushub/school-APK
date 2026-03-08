import { useState, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { motion, AnimatePresence } from 'framer-motion';
import {
  BookOpen,
  ArrowRight,
  ArrowLeft,
  Eye,
  EyeOff,
  GraduationCap,
  Users,
  Briefcase,
  CheckCircle2,
} from 'lucide-react';
import { toast } from 'sonner';
import CaptchaWidget from '../../components/CaptchaWidget';
import api from '../../lib/api';
import { cn } from '../../lib/utils';


const step1Schema = z
  .object({
    name: z.string().min(2, 'Name must be at least 2 characters'),
    email: z.string().email('Invalid email address'),
    password: z.string().min(8, 'Password must be at least 8 characters'),
    confirmPassword: z.string(),
  })
  .refine((d) => d.password === d.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

type Step1Data = z.infer<typeof step1Schema>;

type Role = 'STUDENT' | 'PARENT' | 'MENTOR';

const roleCards: { role: Role; label: string; description: string; Icon: React.ElementType }[] = [
  {
    role: 'STUDENT',
    label: 'Student',
    description: 'Prepare for competitive exams with AI guidance and personalised study plans.',
    Icon: GraduationCap,
  },
  {
    role: 'PARENT',
    label: 'Parent',
    description: "Monitor your child's progress, fee payments, and receive real-time updates.",
    Icon: Users,
  },
  {
    role: 'MENTOR',
    label: 'Mentor',
    description: 'Coach students, conduct sessions, and track their performance analytics.',
    Icon: Briefcase,
  },
];

const slideVariants = {
  enter: (direction: number) => ({
    x: direction > 0 ? 60 : -60,
    opacity: 0,
  }),
  center: { x: 0, opacity: 1 },
  exit: (direction: number) => ({
    x: direction < 0 ? 60 : -60,
    opacity: 0,
  }),
};

export default function RegisterPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [direction, setDirection] = useState(1);
  const [selectedRole, setSelectedRole] = useState<Role | null>(null);
  const [showPw, setShowPw] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [step1Data, setStep1Data] = useState<Step1Data | null>(null);
  const [otpValues, setOtpValues] = useState<string[]>(Array(6).fill(''));
  const [isVerifying, setIsVerifying] = useState(false);
  const [isRegistering, setIsRegistering] = useState(false);
  const [isResending, setIsResending] = useState(false);
  const [captchaToken, setCaptchaToken] = useState<string | null>(null);
  const handleCaptchaVerify = useCallback((token: string | null) => setCaptchaToken(token), []);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<Step1Data>({ resolver: zodResolver(step1Schema) });

  function goNext() {
    setDirection(1);
    setStep((s) => s + 1);
  }

  function goBack() {
    setDirection(-1);
    setStep((s) => s - 1);
  }

  async function onStep1Submit(data: Step1Data) {
    setStep1Data(data);
    goNext();
  }

  async function onStep2Continue() {
    if (!selectedRole) {
      toast.error('Please select a role to continue');
      return;
    }
    if (!step1Data) return;
    setIsRegistering(true);
    try {
      const nameParts = step1Data.name.trim().split(/\s+/);
      const firstName = nameParts[0];
      const lastName = nameParts.slice(1).join(' ') || nameParts[0];
      await api.post('/api/v1/auth/register', {
        firstName,
        lastName,
        email: step1Data.email,
        password: step1Data.password,
        role: selectedRole,
        captchaToken: captchaToken!,
        deviceFingerprint: {
          userAgent: navigator.userAgent,
          deviceId: crypto.randomUUID(),
          ipSubnet: '127.0.0',
        },
      });
      toast.success('Account created! Check your email for OTP.');
      goNext();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status?: number; data?: { detail?: string } } };
      if (axiosErr.response?.status === 409) {
        // Account already exists but may not be verified — send a fresh OTP and proceed to verify
        toast.info('Account already exists. Sending a new verification code…');
        try {
          await api.post('/api/v1/otp/send', { email: step1Data.email, purpose: 'EMAIL_VERIFICATION', channel: 'email' });
        } catch {
          // OTP send error is non-fatal — user can retry from step 3
        }
        goNext();
      } else {
        toast.error(axiosErr.response?.data?.detail ?? 'Registration failed');
        setCaptchaToken(null);
      }
    } finally {
      setIsRegistering(false);
    }
  }

  async function onResendOtp() {
    if (!step1Data) return;
    setIsResending(true);
    try {
      await api.post('/api/v1/otp/send', { email: step1Data.email, purpose: 'EMAIL_VERIFICATION', channel: 'email' });
      toast.success('New code sent! Check your email (or auth-svc console in local dev).');
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { detail?: string } } };
      toast.error(axiosErr.response?.data?.detail ?? 'Failed to resend code');
    } finally {
      setIsResending(false);
    }
  }

  async function onVerifyOtp() {
    const otp = otpValues.join('');
    if (otp.length !== 6) {
      toast.error('Please enter the 6-digit code');
      return;
    }
    if (!step1Data) return;
    setIsVerifying(true);
    try {
      await api.post('/api/v1/otp/verify', {
        email: step1Data.email,
        otp,
        purpose: 'EMAIL_VERIFICATION',
      });
      toast.success('Email verified! You can now sign in.');
      navigate('/login');
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { detail?: string } } };
      toast.error(axiosErr.response?.data?.detail ?? 'OTP verification failed');
    } finally {
      setIsVerifying(false);
    }
  }

  function handleOtpChange(index: number, value: string) {
    if (!/^\d*$/.test(value)) return;
    const newOtp = [...otpValues];
    newOtp[index] = value.slice(-1);
    setOtpValues(newOtp);
    if (value && index < 5) {
      const next = document.getElementById(`otp-${index + 1}`);
      next?.focus();
    }
  }

  function handleOtpKeyDown(index: number, e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Backspace' && !otpValues[index] && index > 0) {
      const prev = document.getElementById(`otp-${index - 1}`);
      prev?.focus();
    }
  }

  const steps = ['Account Info', 'Choose Role', 'Verify Email'];

  return (
    <div className="min-h-screen bg-surface flex items-center justify-center p-6">
      <div className="w-full max-w-lg">
        {/* Logo */}
        <div className="flex items-center gap-3 mb-8 justify-center">
          <div className="p-2 rounded-xl bg-brand-600/20 border border-brand-500/30">
            <BookOpen className="w-5 h-5 text-brand-400" />
          </div>
          <span className="font-bold text-lg text-white">NexusEd</span>
        </div>

        {/* Progress bar */}
        <div className="mb-8">
          <div className="flex items-center justify-between mb-3">
            {steps.map((label, i) => (
              <div key={label} className="flex items-center gap-2">
                <div
                  className={cn(
                    'w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold border transition-all duration-300',
                    i + 1 < step
                      ? 'bg-brand-600 border-brand-600 text-white'
                      : i + 1 === step
                      ? 'border-brand-500 text-brand-400 bg-brand-500/10'
                      : 'border-white/10 text-white/30'
                  )}
                >
                  {i + 1 < step ? <CheckCircle2 className="w-4 h-4" /> : i + 1}
                </div>
                <span
                  className={cn(
                    'text-xs font-medium hidden sm:block',
                    i + 1 === step ? 'text-white' : 'text-white/30'
                  )}
                >
                  {label}
                </span>
                {i < steps.length - 1 && (
                  <div
                    className={cn(
                      'hidden sm:block h-px w-8 transition-all duration-300',
                      i + 1 < step ? 'bg-brand-600' : 'bg-white/10'
                    )}
                  />
                )}
              </div>
            ))}
          </div>
          <div className="h-1 bg-surface-100 rounded-full overflow-hidden">
            <motion.div
              className="h-full bg-gradient-to-r from-brand-600 to-violet-500 rounded-full"
              animate={{ width: `${((step - 1) / (steps.length - 1)) * 100}%` }}
              transition={{ duration: 0.4, ease: 'easeOut' }}
            />
          </div>
        </div>

        {/* Step content */}
        <div className="card overflow-hidden relative min-h-[420px]">
          <AnimatePresence custom={direction} mode="wait">
            {step === 1 && (
              <motion.div
                key="step1"
                custom={direction}
                variants={slideVariants}
                initial="enter"
                animate="center"
                exit="exit"
                transition={{ duration: 0.3, ease: 'easeInOut' }}
              >
                <h2 className="text-2xl font-bold text-white mb-1">Create your account</h2>
                <p className="text-white/40 mb-6 text-sm">Get started with NexusEd today.</p>

                <form onSubmit={handleSubmit(onStep1Submit)} className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-white/70 mb-1.5">Full Name</label>
                    <input
                      {...register('name')}
                      type="text"
                      placeholder="Jane Smith"
                      className={cn('input w-full', errors.name && 'border-red-500/50')}
                    />
                    {errors.name && <p className="text-red-400 text-xs mt-1">{errors.name.message}</p>}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-white/70 mb-1.5">Email</label>
                    <input
                      {...register('email')}
                      type="email"
                      placeholder="you@example.com"
                      className={cn('input w-full', errors.email && 'border-red-500/50')}
                    />
                    {errors.email && <p className="text-red-400 text-xs mt-1">{errors.email.message}</p>}
                  </div>

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
                        className="absolute right-3 top-1/2 -translate-y-1/2 text-white/30 hover:text-white/70 transition-colors"
                      >
                        {showPw ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>
                    </div>
                    {errors.password && <p className="text-red-400 text-xs mt-1">{errors.password.message}</p>}
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
                        className="absolute right-3 top-1/2 -translate-y-1/2 text-white/30 hover:text-white/70 transition-colors"
                      >
                        {showConfirm ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>
                    </div>
                    {errors.confirmPassword && (
                      <p className="text-red-400 text-xs mt-1">{errors.confirmPassword.message}</p>
                    )}
                  </div>

                  <button type="submit" className="btn-primary w-full flex items-center justify-center gap-2 py-3 mt-2">
                    Continue <ArrowRight className="w-4 h-4" />
                  </button>
                </form>
              </motion.div>
            )}

            {step === 2 && (
              <motion.div
                key="step2"
                custom={direction}
                variants={slideVariants}
                initial="enter"
                animate="center"
                exit="exit"
                transition={{ duration: 0.3, ease: 'easeInOut' }}
              >
                <h2 className="text-2xl font-bold text-white mb-1">Choose your role</h2>
                <p className="text-white/40 mb-6 text-sm">Select how you&apos;ll use NexusEd.</p>

                <div className="space-y-3 mb-6">
                  {roleCards.map(({ role, label, description, Icon }) => (
                    <button
                      key={role}
                      type="button"
                      onClick={() => setSelectedRole(role)}
                      className={cn(
                        'w-full text-left p-4 rounded-xl border transition-all duration-200 flex items-start gap-4',
                        selectedRole === role
                          ? 'border-brand-500 bg-brand-500/10'
                          : 'border-white/10 bg-surface-100/50 hover:border-white/20 hover:bg-surface-100'
                      )}
                    >
                      <div
                        className={cn(
                          'p-2.5 rounded-lg flex-shrink-0',
                          selectedRole === role ? 'bg-brand-600/30' : 'bg-surface-200/60'
                        )}
                      >
                        <Icon
                          className={cn(
                            'w-5 h-5',
                            selectedRole === role ? 'text-brand-400' : 'text-white/40'
                          )}
                        />
                      </div>
                      <div>
                        <div
                          className={cn(
                            'font-semibold text-sm',
                            selectedRole === role ? 'text-white' : 'text-white/70'
                          )}
                        >
                          {label}
                        </div>
                        <div className="text-xs text-white/40 mt-0.5 leading-relaxed">{description}</div>
                      </div>
                      {selectedRole === role && (
                        <CheckCircle2 className="w-4 h-4 text-brand-400 flex-shrink-0 ml-auto mt-0.5" />
                      )}
                    </button>
                  ))}
                </div>

                <div className="flex justify-center mb-4">
                  <CaptchaWidget onVerify={handleCaptchaVerify} />
                </div>

                <div className="flex gap-3">
                  <button type="button" onClick={goBack} className="btn-ghost flex items-center gap-2 py-3 px-4">
                    <ArrowLeft className="w-4 h-4" /> Back
                  </button>
                  <button
                    type="button"
                    onClick={onStep2Continue}
                    disabled={isRegistering || !captchaToken}
                    className="btn-primary flex-1 flex items-center justify-center gap-2 py-3"
                  >
                    {isRegistering ? (
                      <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    ) : (
                      <>
                        Create Account <ArrowRight className="w-4 h-4" />
                      </>
                    )}
                  </button>
                </div>
              </motion.div>
            )}

            {step === 3 && (
              <motion.div
                key="step3"
                custom={direction}
                variants={slideVariants}
                initial="enter"
                animate="center"
                exit="exit"
                transition={{ duration: 0.3, ease: 'easeInOut' }}
                className="text-center"
              >
                <div className="flex justify-center mb-4">
                  <div className="p-4 rounded-2xl bg-brand-600/20 border border-brand-500/30">
                    <CheckCircle2 className="w-8 h-8 text-brand-400" />
                  </div>
                </div>
                <h2 className="text-2xl font-bold text-white mb-1">Verify your email</h2>
                <p className="text-white/40 mb-2 text-sm">
                  We sent a 6-digit code to{' '}
                  <span className="text-white/60 font-medium">{step1Data?.email}</span>
                </p>
                <p className="text-white/30 text-xs mb-8">Check your inbox and spam folder.</p>

                <div className="flex justify-center gap-3 mb-8">
                  {otpValues.map((val, i) => (
                    <input
                      key={i}
                      id={`otp-${i}`}
                      type="text"
                      inputMode="numeric"
                      maxLength={1}
                      value={val}
                      onChange={(e) => handleOtpChange(i, e.target.value)}
                      onKeyDown={(e) => handleOtpKeyDown(i, e)}
                      className="w-11 h-14 text-center text-xl font-bold bg-surface-100 border border-white/10 rounded-xl text-white focus:outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/20 transition-all"
                    />
                  ))}
                </div>

                <button
                  type="button"
                  onClick={onVerifyOtp}
                  disabled={isVerifying || otpValues.some((v) => !v)}
                  className="btn-primary w-full flex items-center justify-center gap-2 py-3 mb-4"
                >
                  {isVerifying ? (
                    <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  ) : (
                    'Verify & Continue'
                  )}
                </button>

                <div className="flex items-center justify-between">
                  <button
                    type="button"
                    onClick={goBack}
                    className="text-xs text-white/30 hover:text-white/60 transition-colors"
                  >
                    <ArrowLeft className="w-3 h-3 inline mr-1" />
                    Go back
                  </button>
                  <button
                    type="button"
                    onClick={onResendOtp}
                    disabled={isResending}
                    className="text-xs text-brand-400 hover:text-brand-300 transition-colors disabled:opacity-50"
                  >
                    {isResending ? 'Sending…' : 'Resend code'}
                  </button>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        <p className="mt-6 text-center text-white/40 text-sm">
          Already have an account?{' '}
          <Link to="/login" className="text-brand-400 hover:text-brand-300 font-medium transition-colors">
            Sign in
          </Link>
        </p>

        <p className="mt-6 text-center text-white/15 text-xs">
          © {new Date().getFullYear()} Ai Nexus Innovation Hub Pvt Ltd. All rights reserved.
        </p>
      </div>
    </div>
  );
}
