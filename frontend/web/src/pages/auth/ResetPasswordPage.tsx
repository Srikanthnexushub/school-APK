import { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { BookOpen, Lock, Eye, EyeOff, ArrowLeft } from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';

export default function ResetPasswordPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const state = location.state as { email?: string; resetToken?: string } | null;

  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errors, setErrors] = useState<{ newPassword?: string; confirmPassword?: string }>({});

  // Guard: if accessed without the proper state, redirect to forgot-password
  if (!state?.email || !state?.resetToken) {
    return (
      <div className="min-h-screen bg-surface flex items-center justify-center p-6">
        <div className="text-center">
          <p className="text-white/50 mb-4">Invalid reset link. Please request a new one.</p>
          <Link to="/forgot-password" className="btn-primary">
            Request reset
          </Link>
        </div>
      </div>
    );
  }

  function validate(): boolean {
    const e: typeof errors = {};
    if (newPassword.length < 8) e.newPassword = 'Password must be at least 8 characters';
    if (newPassword !== confirmPassword) e.confirmPassword = 'Passwords do not match';
    setErrors(e);
    return Object.keys(e).length === 0;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!validate()) return;

    setIsSubmitting(true);
    try {
      await api.post('/api/v1/auth/reset-password', {
        email: state!.email,
        resetToken: state!.resetToken,
        newPassword,
      });
      toast.success('Password reset successfully! Please log in with your new password.');
      navigate('/login', { replace: true });
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { detail?: string } } };
      const detail = axiosErr.response?.data?.detail;
      if (detail?.includes('invalid') || detail?.includes('expired')) {
        toast.error('Reset link has expired. Please request a new one.');
        navigate('/forgot-password', { replace: true });
      } else {
        toast.error(detail ?? 'Failed to reset password. Please try again.');
      }
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
              <Lock className="w-8 h-8 text-brand-400" />
            </div>
          </div>

          <h2 className="text-2xl font-bold text-white mb-2 text-center">Set new password</h2>
          <p className="text-white/40 text-sm text-center mb-6">
            Choose a strong password for{' '}
            <span className="text-white/70">{state.email}</span>
          </p>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-white/70 mb-1.5">
                New password
              </label>
              <div className="relative">
                <input
                  type={showNew ? 'text' : 'password'}
                  value={newPassword}
                  onChange={(e) => {
                    setNewPassword(e.target.value);
                    if (errors.newPassword) setErrors((p) => ({ ...p, newPassword: undefined }));
                  }}
                  className={`input w-full pr-10 ${errors.newPassword ? 'border-red-500/50' : ''}`}
                  disabled={isSubmitting}
                  autoFocus
                  placeholder="At least 8 characters"
                />
                <button
                  type="button"
                  onClick={() => setShowNew(!showNew)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-white/30 hover:text-white/70"
                >
                  {showNew ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {errors.newPassword && (
                <p className="text-red-400 text-xs mt-1">{errors.newPassword}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-white/70 mb-1.5">
                Confirm new password
              </label>
              <div className="relative">
                <input
                  type={showConfirm ? 'text' : 'password'}
                  value={confirmPassword}
                  onChange={(e) => {
                    setConfirmPassword(e.target.value);
                    if (errors.confirmPassword) setErrors((p) => ({ ...p, confirmPassword: undefined }));
                  }}
                  className={`input w-full pr-10 ${errors.confirmPassword ? 'border-red-500/50' : ''}`}
                  disabled={isSubmitting}
                  placeholder="Re-enter password"
                />
                <button
                  type="button"
                  onClick={() => setShowConfirm(!showConfirm)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-white/30 hover:text-white/70"
                >
                  {showConfirm ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {errors.confirmPassword && (
                <p className="text-red-400 text-xs mt-1">{errors.confirmPassword}</p>
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
                'Reset Password'
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
