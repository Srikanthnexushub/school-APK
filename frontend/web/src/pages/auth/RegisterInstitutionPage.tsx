// src/pages/auth/RegisterInstitutionPage.tsx
import { useState, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  BookOpen, ArrowRight, ArrowLeft, Eye, EyeOff,
  Building2, MapPin, Phone, Mail, Globe, BookMarked,
  CheckCircle2, Clock, AlertCircle,
} from 'lucide-react';
import { toast } from 'sonner';
import axios from 'axios';
import api from '../../lib/api';
import CaptchaWidget from '../../components/CaptchaWidget';
import { cn } from '../../lib/utils';

// ─── Types ────────────────────────────────────────────────────────────────────

interface InstitutionFormData {
  name: string;
  institutionType: string;
  board: string;
  address: string;
  city: string;
  state: string;
  pincode: string;
  phone: string;
  email: string;
  website: string;
}

interface AdminFormData {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  confirmPassword: string;
  phone: string;
}

const INSTITUTION_TYPES = [
  'SCHOOL',
  'COACHING_CENTER',
  'COLLEGE',
  'UNIVERSITY',
  'TUTORIAL_CENTER',
  'OTHER',
];

const BOARDS = ['CBSE', 'ICSE', 'STATE_BOARD', 'IB', 'IGCSE', 'NOT_APPLICABLE'];

// ─── Step Indicator ───────────────────────────────────────────────────────────

function StepIndicator({ current, total }: { current: number; total: number }) {
  return (
    <div className="flex items-center gap-2 mb-8">
      {Array.from({ length: total }, (_, i) => (
        <div key={i} className="flex items-center gap-2">
          <div
            className={cn(
              'w-7 h-7 rounded-full flex items-center justify-center text-xs font-semibold transition-all',
              i < current
                ? 'bg-brand-500 text-white'
                : i === current
                  ? 'bg-brand-500/20 border-2 border-brand-500 text-brand-400'
                  : 'bg-white/5 border border-white/10 text-white/30'
            )}
          >
            {i < current ? <CheckCircle2 className="w-4 h-4" /> : i + 1}
          </div>
          {i < total - 1 && (
            <div className={cn('h-px w-8 transition-all', i < current ? 'bg-brand-500' : 'bg-white/10')} />
          )}
        </div>
      ))}
    </div>
  );
}

// ─── Field ────────────────────────────────────────────────────────────────────

function Field({
  label, error, children,
}: {
  label: string;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <div>
      <label className="block text-xs text-white/50 mb-1.5">{label}</label>
      {children}
      {error && <p className="text-xs text-red-400 mt-1">{error}</p>}
    </div>
  );
}

const inputCls =
  'w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2.5 text-sm text-white placeholder-white/20 focus:outline-none focus:border-brand-500/50 transition-colors';

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function RegisterInstitutionPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState(0); // 0=institution, 1=admin, 2=otp, 3=done

  // Institution form state
  const [instForm, setInstForm] = useState<InstitutionFormData>({
    name: '', institutionType: '', board: '', address: '',
    city: '', state: '', pincode: '', phone: '', email: '', website: '',
  });

  // Admin form state
  const [adminForm, setAdminForm] = useState<AdminFormData>({
    firstName: '', lastName: '', email: '', password: '', confirmPassword: '', phone: '',
  });
  const [showPw, setShowPw] = useState(false);
  const [showConfirmPw, setShowConfirmPw] = useState(false);

  // OTP state
  const [otpCode, setOtpCode] = useState('');
  const [captchaToken, setCaptchaToken] = useState<string | null>(null);
  const [registrationToken, setRegistrationToken] = useState<string | null>(null);

  // UI state
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [warningMessage, setWarningMessage] = useState<string | null>(null);

  const handleCaptchaVerify = useCallback((token: string | null) => setCaptchaToken(token), []);

  const setInst = (k: keyof InstitutionFormData, v: string) =>
    setInstForm(f => ({ ...f, [k]: v }));
  const setAdmin = (k: keyof AdminFormData, v: string) =>
    setAdminForm(f => ({ ...f, [k]: v }));

  // ── Step 0 validation ──────────────────────────────────────────────────────
  function validateInstitution(): boolean {
    const e: Record<string, string> = {};
    if (!instForm.name.trim()) e.name = 'Institution name is required';
    if (!instForm.institutionType) e.institutionType = 'Select institution type';
    if (!instForm.address.trim()) e.address = 'Address is required';
    if (!instForm.city.trim()) e.city = 'City is required';
    if (!instForm.state.trim()) e.state = 'State is required';
    if (!/^\d{6}$/.test(instForm.pincode)) e.pincode = 'Enter a valid 6-digit pincode';
    if (!instForm.phone.trim()) e.phone = 'Phone is required';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(instForm.email)) e.email = 'Enter a valid email';
    setErrors(e);
    return Object.keys(e).length === 0;
  }

  // ── Step 1 validation ──────────────────────────────────────────────────────
  function validateAdmin(): boolean {
    const e: Record<string, string> = {};
    if (!adminForm.firstName.trim()) e.firstName = 'First name is required';
    if (!adminForm.lastName.trim()) e.lastName = 'Last name is required';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(adminForm.email)) e.email = 'Enter a valid email';
    if (adminForm.password.length < 8) e.password = 'At least 8 characters';
    else if (!/[A-Z]/.test(adminForm.password)) e.password = 'At least 1 uppercase letter';
    else if (!/[0-9]/.test(adminForm.password)) e.password = 'At least 1 digit';
    else if (!/[^A-Za-z0-9]/.test(adminForm.password)) e.password = 'At least 1 special character';
    if (adminForm.password !== adminForm.confirmPassword) e.confirmPassword = 'Passwords do not match';
    if (!captchaToken) e.captcha = 'Please complete the captcha';
    setErrors(e);
    return Object.keys(e).length === 0;
  }

  // ── Step 1 → 2: Register admin user and trigger OTP ───────────────────────
  async function handleRegisterAndSendOtp() {
    if (!validateAdmin()) return;
    setIsSubmitting(true);
    try {
      const deviceId = crypto.randomUUID();
      await api.post('/api/v1/auth/register', {
        firstName: adminForm.firstName,
        lastName: adminForm.lastName,
        email: adminForm.email,
        password: adminForm.password,
        phone: adminForm.phone || null,
        role: 'CENTER_ADMIN',
        captchaToken,
      }, { headers: { 'X-Device-Id': deviceId } });
      setStep(2);
      toast.success('OTP sent to ' + adminForm.email);
    } catch (err: unknown) {
      const msg = axios.isAxiosError(err)
        ? (err.response?.data?.detail ?? 'Registration failed')
        : 'Registration failed';
      toast.error(msg);
    } finally {
      setIsSubmitting(false);
    }
  }

  // ── Step 2: Verify OTP → register institution ──────────────────────────────
  async function handleVerifyOtpAndRegister() {
    if (!otpCode || otpCode.length !== 6) {
      setErrors({ otp: 'Enter the 6-digit OTP' });
      return;
    }
    setIsSubmitting(true);
    setErrors({});
    try {
      const deviceId = crypto.randomUUID();
      // Verify OTP — activates account and returns JWT directly
      const verifyRes = await api.post('/api/v1/otp/verify', {
        email: adminForm.email,
        otp: otpCode,
        purpose: 'EMAIL_VERIFICATION',
      });
      const jwt: string = verifyRes.data.accessToken;
      setRegistrationToken(jwt);

      // Create pending institution registration
      const regRes = await api.post('/api/v1/centers/register', {
        name: instForm.name,
        institutionType: instForm.institutionType,
        board: instForm.board || null,
        address: instForm.address,
        city: instForm.city,
        state: instForm.state,
        pincode: instForm.pincode,
        phone: instForm.phone,
        email: instForm.email,
        website: instForm.website || null,
      }, {
        headers: { Authorization: `Bearer ${jwt}` },
      });

      if (regRes.data.warningMessage) {
        setWarningMessage(regRes.data.warningMessage);
      }
      setStep(3);
    } catch (err: unknown) {
      const msg = axios.isAxiosError(err)
        ? (err.response?.data?.detail ?? 'Verification failed')
        : 'Verification failed';
      toast.error(msg);
    } finally {
      setIsSubmitting(false);
    }
  }

  // ─────────────────────────────────────────────────────────────────────────

  return (
    <div className="min-h-screen bg-surface-50 flex flex-col lg:flex-row">
      {/* ── Left panel ──────────────────────────────────────────────────────── */}
      <div className="hidden lg:flex lg:w-2/5 xl:w-1/3 bg-gradient-to-br from-brand-900/60 to-surface-100 border-r border-white/5 flex-col p-10 justify-between">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-brand-500/20 border border-brand-500/30 flex items-center justify-center">
            <BookOpen className="w-5 h-5 text-brand-400" />
          </div>
          <span className="text-white font-semibold text-lg">NexusEd</span>
        </div>

        <div>
          <Building2 className="w-12 h-12 text-brand-400 mb-6" />
          <h1 className="text-3xl font-bold text-white leading-tight mb-4">
            Register your Institution
          </h1>
          <p className="text-white/50 text-sm leading-relaxed">
            Join the NexusEd platform. Your institution will go through a quick verification
            process before gaining full access to the admin dashboard.
          </p>

          <div className="mt-8 space-y-3">
            {[
              { icon: Building2, text: 'Submit your institution details' },
              { icon: Mail, text: 'Verify your admin email via OTP' },
              { icon: Clock, text: 'Await manual verification (1–2 business days)' },
              { icon: CheckCircle2, text: 'Receive your institution code & full access' },
            ].map(({ icon: Icon, text }, i) => (
              <div key={i} className="flex items-start gap-3">
                <div className="w-6 h-6 rounded-full bg-brand-500/15 flex items-center justify-center flex-shrink-0 mt-0.5">
                  <Icon className="w-3.5 h-3.5 text-brand-400" />
                </div>
                <span className="text-white/50 text-sm">{text}</span>
              </div>
            ))}
          </div>
        </div>

        <p className="text-white/20 text-xs">
          Already registered?{' '}
          <Link to="/login" className="text-brand-400 hover:text-brand-300">
            Sign in
          </Link>
        </p>
      </div>

      {/* ── Right panel ─────────────────────────────────────────────────────── */}
      <div className="flex-1 flex items-start lg:items-center justify-center p-6 lg:p-10">
        <div className="w-full max-w-xl">
          {/* Mobile logo */}
          <div className="flex items-center gap-2 mb-8 lg:hidden">
            <div className="w-8 h-8 rounded-xl bg-brand-500/20 border border-brand-500/30 flex items-center justify-center">
              <BookOpen className="w-4 h-4 text-brand-400" />
            </div>
            <span className="text-white font-semibold">NexusEd</span>
          </div>

          <StepIndicator current={step} total={4} />

          <AnimatePresence mode="wait">
            {/* ── Step 0: Institution Details ─────────────────────────────────── */}
            {step === 0 && (
              <motion.div
                key="step0"
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -20 }}
                transition={{ duration: 0.2 }}
              >
                <h2 className="text-2xl font-bold text-white mb-1">Institution Details</h2>
                <p className="text-white/40 text-sm mb-6">Tell us about your institution</p>

                <div className="space-y-4">
                  <Field label="Institution Name *" error={errors.name}>
                    <input
                      value={instForm.name}
                      onChange={e => setInst('name', e.target.value)}
                      placeholder="e.g. Delhi Public School"
                      className={cn(inputCls, errors.name && 'border-red-500/50')}
                    />
                  </Field>

                  <div className="grid grid-cols-2 gap-3">
                    <Field label="Institution Type *" error={errors.institutionType}>
                      <select
                        value={instForm.institutionType}
                        onChange={e => setInst('institutionType', e.target.value)}
                        className={cn(inputCls, 'appearance-none', errors.institutionType && 'border-red-500/50')}
                      >
                        <option value="">Select type</option>
                        {INSTITUTION_TYPES.map(t => (
                          <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>
                        ))}
                      </select>
                    </Field>

                    <Field label="Board (optional)">
                      <select
                        value={instForm.board}
                        onChange={e => setInst('board', e.target.value)}
                        className={cn(inputCls, 'appearance-none')}
                      >
                        <option value="">Select board</option>
                        {BOARDS.map(b => (
                          <option key={b} value={b}>{b.replace(/_/g, ' ')}</option>
                        ))}
                      </select>
                    </Field>
                  </div>

                  <Field label="Address *" error={errors.address}>
                    <input
                      value={instForm.address}
                      onChange={e => setInst('address', e.target.value)}
                      placeholder="Street address, building, landmark"
                      className={cn(inputCls, errors.address && 'border-red-500/50')}
                    />
                  </Field>

                  <div className="grid grid-cols-2 gap-3">
                    <Field label="City *" error={errors.city}>
                      <input
                        value={instForm.city}
                        onChange={e => setInst('city', e.target.value)}
                        placeholder="New Delhi"
                        className={cn(inputCls, errors.city && 'border-red-500/50')}
                      />
                    </Field>
                    <Field label="State *" error={errors.state}>
                      <input
                        value={instForm.state}
                        onChange={e => setInst('state', e.target.value)}
                        placeholder="Delhi"
                        className={cn(inputCls, errors.state && 'border-red-500/50')}
                      />
                    </Field>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <Field label="Pincode *" error={errors.pincode}>
                      <input
                        value={instForm.pincode}
                        onChange={e => setInst('pincode', e.target.value.replace(/\D/g, '').slice(0, 6))}
                        placeholder="110001"
                        className={cn(inputCls, errors.pincode && 'border-red-500/50')}
                        maxLength={6}
                      />
                    </Field>
                    <Field label="Phone *" error={errors.phone}>
                      <div className="relative">
                        <Phone className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
                        <input
                          value={instForm.phone}
                          onChange={e => setInst('phone', e.target.value)}
                          placeholder="+91 9876543210"
                          className={cn(inputCls, 'pl-9', errors.phone && 'border-red-500/50')}
                        />
                      </div>
                    </Field>
                  </div>

                  <Field label="Institution Email *" error={errors.email}>
                    <div className="relative">
                      <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
                      <input
                        type="email"
                        value={instForm.email}
                        onChange={e => setInst('email', e.target.value)}
                        placeholder="info@myschool.edu"
                        className={cn(inputCls, 'pl-9', errors.email && 'border-red-500/50')}
                      />
                    </div>
                  </Field>

                  <Field label="Website (optional)">
                    <div className="relative">
                      <Globe className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
                      <input
                        value={instForm.website}
                        onChange={e => setInst('website', e.target.value)}
                        placeholder="https://myschool.edu"
                        className={cn(inputCls, 'pl-9')}
                      />
                    </div>
                  </Field>
                </div>

                <button
                  onClick={() => { if (validateInstitution()) setStep(1); }}
                  className="mt-6 w-full flex items-center justify-center gap-2 px-6 py-3 rounded-xl bg-brand-600 hover:bg-brand-500 text-white font-medium text-sm transition-colors"
                >
                  Next: Admin Details <ArrowRight className="w-4 h-4" />
                </button>

                <p className="text-center text-white/30 text-sm mt-4">
                  Already registered?{' '}
                  <Link to="/login" className="text-brand-400 hover:text-brand-300">
                    Sign in
                  </Link>
                </p>
              </motion.div>
            )}

            {/* ── Step 1: Admin User Details ──────────────────────────────────── */}
            {step === 1 && (
              <motion.div
                key="step1"
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -20 }}
                transition={{ duration: 0.2 }}
              >
                <h2 className="text-2xl font-bold text-white mb-1">Admin Account</h2>
                <p className="text-white/40 text-sm mb-6">Create the admin login for your institution</p>

                <div className="space-y-4">
                  <div className="grid grid-cols-2 gap-3">
                    <Field label="First Name *" error={errors.firstName}>
                      <input
                        value={adminForm.firstName}
                        onChange={e => setAdmin('firstName', e.target.value)}
                        placeholder="Rajesh"
                        className={cn(inputCls, errors.firstName && 'border-red-500/50')}
                      />
                    </Field>
                    <Field label="Last Name *" error={errors.lastName}>
                      <input
                        value={adminForm.lastName}
                        onChange={e => setAdmin('lastName', e.target.value)}
                        placeholder="Kumar"
                        className={cn(inputCls, errors.lastName && 'border-red-500/50')}
                      />
                    </Field>
                  </div>

                  <Field label="Admin Email *" error={errors.email}>
                    <div className="relative">
                      <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
                      <input
                        type="email"
                        value={adminForm.email}
                        onChange={e => setAdmin('email', e.target.value)}
                        placeholder="principal@myschool.edu"
                        className={cn(inputCls, 'pl-9', errors.email && 'border-red-500/50')}
                      />
                    </div>
                  </Field>

                  <Field label="Phone (optional)">
                    <div className="relative">
                      <Phone className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
                      <input
                        value={adminForm.phone}
                        onChange={e => setAdmin('phone', e.target.value)}
                        placeholder="+91 9876543210"
                        className={cn(inputCls, 'pl-9')}
                      />
                    </div>
                  </Field>

                  <Field label="Password *" error={errors.password}>
                    <div className="relative">
                      <input
                        type={showPw ? 'text' : 'password'}
                        value={adminForm.password}
                        onChange={e => setAdmin('password', e.target.value)}
                        placeholder="Min 8 chars, uppercase, number, symbol"
                        className={cn(inputCls, 'pr-10', errors.password && 'border-red-500/50')}
                      />
                      <button
                        type="button"
                        onClick={() => setShowPw(p => !p)}
                        className="absolute right-3 top-1/2 -translate-y-1/2 text-white/30 hover:text-white/60"
                      >
                        {showPw ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>
                    </div>
                  </Field>

                  <Field label="Confirm Password *" error={errors.confirmPassword}>
                    <div className="relative">
                      <input
                        type={showConfirmPw ? 'text' : 'password'}
                        value={adminForm.confirmPassword}
                        onChange={e => setAdmin('confirmPassword', e.target.value)}
                        placeholder="Repeat password"
                        className={cn(inputCls, 'pr-10', errors.confirmPassword && 'border-red-500/50')}
                      />
                      <button
                        type="button"
                        onClick={() => setShowConfirmPw(p => !p)}
                        className="absolute right-3 top-1/2 -translate-y-1/2 text-white/30 hover:text-white/60"
                      >
                        {showConfirmPw ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>
                    </div>
                  </Field>

                  <div>
                    <CaptchaWidget onVerify={handleCaptchaVerify} />
                    {errors.captcha && <p className="text-xs text-red-400 mt-1">{errors.captcha}</p>}
                  </div>
                </div>

                <div className="flex gap-3 mt-6">
                  <button
                    onClick={() => { setErrors({}); setStep(0); }}
                    className="flex items-center gap-2 px-4 py-3 rounded-xl border border-white/10 text-white/50 hover:text-white hover:border-white/20 text-sm transition-colors"
                  >
                    <ArrowLeft className="w-4 h-4" /> Back
                  </button>
                  <button
                    onClick={handleRegisterAndSendOtp}
                    disabled={isSubmitting}
                    className="flex-1 flex items-center justify-center gap-2 px-6 py-3 rounded-xl bg-brand-600 hover:bg-brand-500 disabled:opacity-50 text-white font-medium text-sm transition-colors"
                  >
                    {isSubmitting ? (
                      <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    ) : (
                      <>Send OTP <ArrowRight className="w-4 h-4" /></>
                    )}
                  </button>
                </div>
              </motion.div>
            )}

            {/* ── Step 2: OTP Verification ─────────────────────────────────────── */}
            {step === 2 && (
              <motion.div
                key="step2"
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -20 }}
                transition={{ duration: 0.2 }}
              >
                <h2 className="text-2xl font-bold text-white mb-1">Verify Email</h2>
                <p className="text-white/40 text-sm mb-6">
                  Enter the 6-digit OTP sent to{' '}
                  <span className="text-white/70">{adminForm.email}</span>
                </p>

                <div className="space-y-4">
                  <Field label="OTP Code" error={errors.otp}>
                    <input
                      value={otpCode}
                      onChange={e => setOtpCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                      placeholder="6-digit code"
                      maxLength={6}
                      className={cn(inputCls, 'text-center text-xl tracking-[0.5em] font-mono', errors.otp && 'border-red-500/50')}
                    />
                  </Field>
                </div>

                <div className="flex gap-3 mt-6">
                  <button
                    onClick={() => { setErrors({}); setStep(1); }}
                    className="flex items-center gap-2 px-4 py-3 rounded-xl border border-white/10 text-white/50 hover:text-white hover:border-white/20 text-sm transition-colors"
                  >
                    <ArrowLeft className="w-4 h-4" /> Back
                  </button>
                  <button
                    onClick={handleVerifyOtpAndRegister}
                    disabled={isSubmitting || otpCode.length !== 6}
                    className="flex-1 flex items-center justify-center gap-2 px-6 py-3 rounded-xl bg-brand-600 hover:bg-brand-500 disabled:opacity-50 text-white font-medium text-sm transition-colors"
                  >
                    {isSubmitting ? (
                      <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    ) : (
                      <>Verify & Submit <ArrowRight className="w-4 h-4" /></>
                    )}
                  </button>
                </div>
              </motion.div>
            )}

            {/* ── Step 3: Under Review ─────────────────────────────────────────── */}
            {step === 3 && (
              <motion.div
                key="step3"
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ duration: 0.3 }}
                className="text-center"
              >
                <div className="w-20 h-20 rounded-full bg-amber-500/15 border border-amber-500/30 flex items-center justify-center mx-auto mb-6">
                  <Clock className="w-10 h-10 text-amber-400" />
                </div>

                <h2 className="text-2xl font-bold text-white mb-3">Under Review</h2>
                <p className="text-white/50 text-sm leading-relaxed max-w-sm mx-auto mb-6">
                  Your institution registration has been submitted successfully. Our team will review
                  your details within <span className="text-white/80">1–2 business days</span>.
                </p>

                {warningMessage && (
                  <div className="flex items-start gap-3 p-4 rounded-xl bg-amber-500/10 border border-amber-500/20 text-left mb-6">
                    <AlertCircle className="w-4 h-4 text-amber-400 flex-shrink-0 mt-0.5" />
                    <p className="text-sm text-amber-300">{warningMessage}</p>
                  </div>
                )}

                <div className="bg-surface-100/40 border border-white/5 rounded-xl p-5 text-left mb-6 space-y-2">
                  <div className="flex items-center gap-2 text-sm text-white/70">
                    <Building2 className="w-4 h-4 text-brand-400 flex-shrink-0" />
                    <span className="font-medium">{instForm.name}</span>
                  </div>
                  <div className="flex items-center gap-2 text-sm text-white/40">
                    <MapPin className="w-4 h-4 flex-shrink-0" />
                    <span>{[instForm.city, instForm.state, instForm.pincode].filter(Boolean).join(', ')}</span>
                  </div>
                  <div className="flex items-center gap-2 text-sm text-white/40">
                    <Mail className="w-4 h-4 flex-shrink-0" />
                    <span>{adminForm.email}</span>
                  </div>
                </div>

                <div className="space-y-3">
                  <p className="text-white/30 text-xs">
                    Once approved, you'll receive an email with your institution code.
                    Log in to check your registration status.
                  </p>
                  <button
                    onClick={() => navigate('/login')}
                    className="w-full flex items-center justify-center gap-2 px-6 py-3 rounded-xl bg-brand-600 hover:bg-brand-500 text-white font-medium text-sm transition-colors"
                  >
                    Go to Login <ArrowRight className="w-4 h-4" />
                  </button>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
    </div>
  );
}
