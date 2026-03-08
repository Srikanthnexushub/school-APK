import { useState, useEffect, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';
import { BookOpen, Mail, RotateCcw } from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { cn } from '../../lib/utils';

export default function OtpPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const email = (location.state as { email?: string })?.email ?? '';
  const purpose = (location.state as { purpose?: string })?.purpose ?? 'REGISTRATION';

  const [otpValues, setOtpValues] = useState<string[]>(Array(6).fill(''));
  const [isVerifying, setIsVerifying] = useState(false);
  const [isResending, setIsResending] = useState(false);
  const [countdown, setCountdown] = useState(60);
  const [canResend, setCanResend] = useState(false);
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  useEffect(() => {
    inputRefs.current[0]?.focus();
  }, []);

  useEffect(() => {
    if (countdown <= 0) {
      setCanResend(true);
      return;
    }
    const timer = setInterval(() => setCountdown((c) => c - 1), 1000);
    return () => clearInterval(timer);
  }, [countdown]);

  function handleChange(index: number, value: string) {
    if (!/^\d*$/.test(value)) return;
    const newOtp = [...otpValues];
    newOtp[index] = value.slice(-1);
    setOtpValues(newOtp);
    if (value && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
    // Auto-verify when all digits entered
    if (newOtp.every((v) => v) && newOtp.join('').length === 6) {
      verifyOtp(newOtp.join(''));
    }
  }

  function handleKeyDown(index: number, e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Backspace' && !otpValues[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  }

  function handlePaste(e: React.ClipboardEvent) {
    e.preventDefault();
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    if (!pasted) return;
    const newOtp = Array(6).fill('');
    pasted.split('').forEach((ch, i) => {
      newOtp[i] = ch;
    });
    setOtpValues(newOtp);
    const nextEmpty = pasted.length < 6 ? pasted.length : 5;
    inputRefs.current[nextEmpty]?.focus();
    if (pasted.length === 6) {
      verifyOtp(pasted);
    }
  }

  async function verifyOtp(otp: string) {
    if (!email) {
      toast.error('Email not found. Please restart the flow.');
      return;
    }
    setIsVerifying(true);
    try {
      await api.post('/api/v1/auth/verify-otp', { email, otp, purpose });
      toast.success('Email verified successfully!');
      navigate('/login', { replace: true });
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { detail?: string } } };
      toast.error(axiosErr.response?.data?.detail ?? 'Invalid or expired OTP');
      setOtpValues(Array(6).fill(''));
      inputRefs.current[0]?.focus();
    } finally {
      setIsVerifying(false);
    }
  }

  async function handleResend() {
    if (!canResend || !email) return;
    setIsResending(true);
    try {
      await api.post('/api/v1/auth/resend-otp', { email, purpose });
      toast.success('New OTP sent to your email!');
      setCountdown(60);
      setCanResend(false);
      setOtpValues(Array(6).fill(''));
      inputRefs.current[0]?.focus();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { detail?: string } } };
      toast.error(axiosErr.response?.data?.detail ?? 'Failed to resend OTP');
    } finally {
      setIsResending(false);
    }
  }

  const isComplete = otpValues.every((v) => v !== '');

  return (
    <div className="min-h-screen bg-surface flex items-center justify-center p-6">
      {/* Background orbs */}
      <div className="fixed top-1/4 left-1/4 w-64 h-64 bg-brand-600/10 rounded-full blur-3xl pointer-events-none" />
      <div className="fixed bottom-1/4 right-1/4 w-48 h-48 bg-violet-600/10 rounded-full blur-3xl pointer-events-none" />

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="w-full max-w-md"
      >
        {/* Logo */}
        <div className="flex items-center gap-3 mb-10 justify-center">
          <div className="p-2 rounded-xl bg-brand-600/20 border border-brand-500/30">
            <BookOpen className="w-5 h-5 text-brand-400" />
          </div>
          <span className="font-bold text-lg text-white">NexusEd</span>
        </div>

        <div className="card text-center">
          {/* Icon */}
          <div className="flex justify-center mb-5">
            <div className="relative">
              <div className="p-4 rounded-2xl bg-brand-600/20 border border-brand-500/30">
                <Mail className="w-8 h-8 text-brand-400" />
              </div>
              <div className="absolute -top-1 -right-1 w-4 h-4 bg-emerald-500 rounded-full border-2 border-surface" />
            </div>
          </div>

          <h2 className="text-2xl font-bold text-white mb-2">Check your email</h2>
          {email ? (
            <p className="text-white/40 text-sm mb-1">
              We sent a 6-digit verification code to
            </p>
          ) : null}
          {email ? (
            <p className="text-white/70 text-sm font-medium mb-6">{email}</p>
          ) : (
            <p className="text-white/40 text-sm mb-6">Enter the 6-digit code from your email.</p>
          )}

          {/* OTP inputs */}
          <div className="flex justify-center gap-2.5 mb-6" onPaste={handlePaste}>
            {otpValues.map((val, i) => (
              <input
                key={i}
                ref={(el) => { inputRefs.current[i] = el; }}
                type="text"
                inputMode="numeric"
                maxLength={1}
                value={val}
                onChange={(e) => handleChange(i, e.target.value)}
                onKeyDown={(e) => handleKeyDown(i, e)}
                className={cn(
                  'w-11 h-14 text-center text-xl font-bold rounded-xl border transition-all duration-200',
                  'bg-surface-100 text-white',
                  'focus:outline-none focus:ring-2 focus:ring-brand-500/20',
                  val
                    ? 'border-brand-500 bg-brand-500/10'
                    : 'border-white/10 focus:border-brand-500'
                )}
                disabled={isVerifying}
              />
            ))}
          </div>

          {/* Verify button */}
          <button
            type="button"
            onClick={() => verifyOtp(otpValues.join(''))}
            disabled={!isComplete || isVerifying}
            className={cn(
              'btn-primary w-full flex items-center justify-center gap-2 py-3 mb-4',
              (!isComplete || isVerifying) && 'opacity-50 cursor-not-allowed'
            )}
          >
            {isVerifying ? (
              <>
                <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                Verifying...
              </>
            ) : (
              'Verify Email'
            )}
          </button>

          {/* Resend */}
          <div className="flex items-center justify-center gap-2">
            <span className="text-white/30 text-sm">Didn&apos;t receive it?</span>
            {canResend ? (
              <button
                type="button"
                onClick={handleResend}
                disabled={isResending}
                className="text-brand-400 hover:text-brand-300 text-sm font-medium transition-colors flex items-center gap-1"
              >
                <RotateCcw className={cn('w-3.5 h-3.5', isResending && 'animate-spin')} />
                Resend OTP
              </button>
            ) : (
              <span className="text-white/40 text-sm font-mono">
                Resend in{' '}
                <span className="text-brand-400">{String(Math.floor(countdown / 60)).padStart(2, '0')}:{String(countdown % 60).padStart(2, '0')}</span>
              </span>
            )}
          </div>
        </div>
      </motion.div>
    </div>
  );
}
