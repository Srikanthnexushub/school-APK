import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { BookOpen, Mail, ArrowLeft } from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';

export default function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [emailError, setEmailError] = useState('');

  function validate(): boolean {
    if (!email.trim()) {
      setEmailError('Email is required');
      return false;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      setEmailError('Please enter a valid email address');
      return false;
    }
    setEmailError('');
    return true;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!validate()) return;

    setIsSubmitting(true);
    try {
      // Backend returns 204 regardless of whether email exists — prevents enumeration
      await api.post('/api/v1/auth/forgot-password', { email: email.trim() });
      toast.success('If this email is registered, you will receive a reset code shortly.');
      navigate('/verify-otp', {
        state: { email: email.trim(), purpose: 'PASSWORD_RESET' },
      });
    } catch {
      // Still navigate so the user flow continues — server logs the error
      toast.info('Check your email for the reset code.');
      navigate('/verify-otp', {
        state: { email: email.trim(), purpose: 'PASSWORD_RESET' },
      });
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="min-h-screen bg-surface flex items-center justify-center p-6">
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

        <div className="card">
          {/* Icon */}
          <div className="flex justify-center mb-5">
            <div className="p-4 rounded-2xl bg-brand-600/20 border border-brand-500/30">
              <Mail className="w-8 h-8 text-brand-400" />
            </div>
          </div>

          <h2 className="text-2xl font-bold text-white mb-2 text-center">Forgot your password?</h2>
          <p className="text-white/40 text-sm text-center mb-6">
            Enter your registered email address. We&apos;ll send a 6-digit reset code.
          </p>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-white/70 mb-1.5">
                Email address
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  if (emailError) setEmailError('');
                }}
                placeholder="you@example.com"
                className={`input w-full ${emailError ? 'border-red-500/50' : ''}`}
                disabled={isSubmitting}
                autoComplete="email"
                autoFocus
              />
              {emailError && (
                <p className="text-red-400 text-xs mt-1">{emailError}</p>
              )}
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="btn-primary w-full flex items-center justify-center gap-2 py-3"
            >
              {isSubmitting ? (
                <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                'Send Reset Code'
              )}
            </button>
          </form>

          <div className="mt-5 flex justify-center">
            <Link
              to="/login"
              className="flex items-center gap-1.5 text-white/40 hover:text-white/70 text-sm transition-colors"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              Back to login
            </Link>
          </div>
        </div>
      </motion.div>
    </div>
  );
}
