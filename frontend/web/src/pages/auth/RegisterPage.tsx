import { useState, useCallback, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { motion, AnimatePresence } from 'framer-motion';
import axios from 'axios';
import {
  BookOpen,
  ArrowRight,
  ArrowLeft,
  Eye,
  EyeOff,
  CheckCircle2,
  Loader2,
  ChevronDown,
} from 'lucide-react';
import { toast } from 'sonner';
import CaptchaWidget from '../../components/CaptchaWidget';
import GoogleSignInButton from '../../components/GoogleSignInButton';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { cn } from '../../lib/utils';

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID ?? '';

const step1Schema = z
  .object({
    firstName: z.string().min(1, 'First name is required').max(100),
    lastName: z.string().min(1, 'Last name is required').max(100),
    email: z.string().email('Invalid email address'),
    password: z
      .string()
      .min(8, 'At least 8 characters')
      .regex(/[A-Z]/, 'At least 1 uppercase letter')
      .regex(/[0-9]/, 'At least 1 digit')
      .regex(/[^A-Za-z0-9]/, 'At least 1 special character'),
    confirmPassword: z.string(),
    phone: z.string().max(20).optional().or(z.literal('')),
    occupation: z.string().max(100).optional().or(z.literal('')),
    dateOfBirth: z.string().optional().or(z.literal('')),
  })
  .refine((d) => d.password === d.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

type Step1Data = z.infer<typeof step1Schema>;

const step3Schema = z.object({
  institutionCode: z.string().min(1, 'Institution code is required'),
  board: z.enum(['CBSE', 'ICSE', 'STATE_BOARD', 'IB', 'IGCSE'], { required_error: 'Select a board' }),
  grade: z.coerce.number().min(10).max(12),
});

type Step3Data = z.infer<typeof step3Schema>;

const consentSchema = z.object({
  parentEmail: z.string().email('Enter a valid parent/guardian email'),
});

type ConsentData = z.infer<typeof consentSchema>;

function calculateAge(dob: string): number {
  const today = new Date();
  const birth = new Date(dob);
  let age = today.getFullYear() - birth.getFullYear();
  const m = today.getMonth() - birth.getMonth();
  if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--;
  return age;
}

type Role = 'STUDENT' | 'PARENT' | 'TEACHER' | 'CENTER_ADMIN';

const roleOptions: { role: Role; label: string; description: string }[] = [
  { role: 'STUDENT', label: 'Student', description: 'Prepare for competitive exams with AI guidance and personalised study plans.' },
  { role: 'PARENT', label: 'Parent', description: "Monitor your child's progress, fee payments, and receive real-time updates." },
  { role: 'TEACHER', label: 'Teacher', description: 'Coach students, conduct sessions, and track their performance analytics.' },
  { role: 'CENTER_ADMIN', label: 'Institution', description: 'Manage your coaching centre, batches, teachers, and student enrolments.' },
];

const slideVariants = {
  enter: (direction: number) => ({ x: direction > 0 ? 60 : -60, opacity: 0 }),
  center: { x: 0, opacity: 1 },
  exit: (direction: number) => ({ x: direction < 0 ? 60 : -60, opacity: 0 }),
};

export default function RegisterPage() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);
  const [step, setStep] = useState(1);
  const [direction, setDirection] = useState(1);
  const [showOtp, setShowOtp] = useState(false);
  const [selectedRole, setSelectedRole] = useState<Role | null>(null);
  const [showPw, setShowPw] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [step1Data, setStep1Data] = useState<Step1Data | null>(null);
  const [step3Data, setStep3Data] = useState<Step3Data | null>(null);
  const [otpValues, setOtpValues] = useState<string[]>(Array(6).fill(''));
  const [isVerifying, setIsVerifying] = useState(false);
  const [isRegistering, setIsRegistering] = useState(false);
  const [isResending, setIsResending] = useState(false);
  const [isGoogleLoading, setIsGoogleLoading] = useState(false);
  const [captchaToken, setCaptchaToken] = useState<string | null>(null);
  const [centerId, setCenterId] = useState<string | null>(null);
  const [centerName, setCenterName] = useState<string | null>(null);
  const [regToken, setRegToken] = useState<string | null>(null);
  const [regRefreshToken, setRegRefreshToken] = useState<string | null>(null);
  const [regDeviceId, setRegDeviceId] = useState<string | null>(null);
  const [selectedSubjects, setSelectedSubjects] = useState<string[]>([]);
  const [availableSubjects, setAvailableSubjects] = useState<string[]>([]);
  const [isLoadingSubjects, setIsLoadingSubjects] = useState(false);
  const [isValidatingCode, setIsValidatingCode] = useState(false);
  const [isUnder13, setIsUnder13] = useState(false);
  const [parentEmail, setParentEmail] = useState<string | null>(null);
  const [resendsRemaining, setResendsRemaining] = useState<number | null>(null);
  // Teacher-specific fields
  const [teacherInstitutionCode, setTeacherInstitutionCode] = useState('');
  const [teacherCenterId, setTeacherCenterId] = useState<string | null>(null);
  const [teacherCenterName, setTeacherCenterName] = useState<string | null>(null);
  const [teacherSubjectsInput, setTeacherSubjectsInput] = useState('');
  const [isValidatingTeacherCode, setIsValidatingTeacherCode] = useState(false);
  // Institution-specific fields
  const [institutionName, setInstitutionName] = useState('');
  const [institutionCity, setInstitutionCity] = useState('');
  const [institutionPhone, setInstitutionPhone] = useState('');
  // Parent-specific fields
  const [parentPhone, setParentPhone] = useState('');
  const [parentOccupation, setParentOccupation] = useState('');
  const [selectedGender, setSelectedGender] = useState('');

  const handleCaptchaVerify = useCallback((token: string | null) => setCaptchaToken(token), []);

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
      setAuth(jwt, { id: u.id, email: u.email, role: u.role, name }, refreshToken, deviceId);
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

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<Step1Data>({ resolver: zodResolver(step1Schema) });

  const {
    register: register3,
    handleSubmit: handleSubmit3,
    watch: watch3,
    formState: { errors: errors3 },
  } = useForm<Step3Data>({ resolver: zodResolver(step3Schema), defaultValues: { grade: 10 } });

  const {
    register: registerConsent,
    handleSubmit: handleSubmitConsent,
    formState: { errors: errorsConsent },
  } = useForm<ConsentData>({ resolver: zodResolver(consentSchema) });

  const watchedPassword = watch('password', '');
  const watchedInstitutionCode = watch3('institutionCode', '');

  // Live debounce lookup for institution code (student academic step)
  useEffect(() => {
    const code = watchedInstitutionCode?.trim();
    if (!code || code.length < 3) {
      setCenterName(null);
      setCenterId(null);
      return;
    }
    const timer = setTimeout(async () => {
      try {
        const resp = await api.get(`/api/v1/centers/lookup?code=${encodeURIComponent(code)}`);
        setCenterId(resp.data.id);
        setCenterName(resp.data.name);
      } catch {
        setCenterName(null);
        setCenterId(null);
      }
    }, 600);
    return () => clearTimeout(timer);
  }, [watchedInstitutionCode]);

  const pwChecks = {
    length: watchedPassword.length >= 8,
    upper: /[A-Z]/.test(watchedPassword),
    digit: /[0-9]/.test(watchedPassword),
    special: /[^A-Za-z0-9]/.test(watchedPassword),
  };

  // Steps: "Your Role" and "Verify Email" are merged into Personal Details (step 1)
  const steps =
    selectedRole === 'STUDENT'
      ? ['Personal Details', 'Academic Info', ...(isUnder13 ? ['Parental Consent'] : []), 'Subjects']
      : selectedRole === 'PARENT'
      ? ['Personal Details']
      : ['Personal Details'];

  function goNext() {
    setDirection(1);
    setStep((s) => s + 1);
  }

  function goBack() {
    setDirection(-1);
    setStep((s) => s - 1);
  }

  // Step 1: unified submit — registers account then shows OTP inline
  async function onStep1Submit(data: Step1Data) {
    if (!selectedRole) {
      toast.error('Please select your role to continue');
      return;
    }

    // STUDENT: DOB is required and drives the under-13 consent flow
    if (selectedRole === 'STUDENT') {
      if (!data.dateOfBirth) {
        toast.error('Date of birth is required');
        return;
      }
      const age = calculateAge(data.dateOfBirth);
      setIsUnder13(age < 13);
    }

    setStep1Data(data);

    // CENTER_ADMIN: validate institution fields
    if (selectedRole === 'CENTER_ADMIN') {
      if (!institutionName.trim()) { toast.error('Institution name is required'); return; }
      if (!institutionCity.trim()) { toast.error('City is required'); return; }
      if (!institutionPhone.trim()) { toast.error('Institution phone is required'); return; }
    }

    // TEACHER: validate institution code if provided and not yet resolved
    if (selectedRole === 'TEACHER' && teacherInstitutionCode.trim() && !teacherCenterId) {
      setIsValidatingTeacherCode(true);
      try {
        const resp = await api.get(`/api/v1/centers/lookup?code=${encodeURIComponent(teacherInstitutionCode.trim())}`);
        setTeacherCenterId(resp.data.id);
        setTeacherCenterName(resp.data.name);
      } catch {
        toast.error('Institution code not found. Check with your institution head.');
        setIsValidatingTeacherCode(false);
        return;
      } finally {
        setIsValidatingTeacherCode(false);
      }
    }

    setIsRegistering(true);
    try {
      const deviceId = crypto.randomUUID();
      setRegDeviceId(deviceId);
      const response = await api.post('/api/v1/auth/register', {
        firstName: data.firstName,
        lastName: data.lastName,
        email: data.email,
        password: data.password,
        role: selectedRole,
        centerId: selectedRole === 'TEACHER' ? (teacherCenterId ?? undefined) : undefined,
        captchaToken: captchaToken!,
        deviceFingerprint: {
          userAgent: navigator.userAgent,
          deviceId,
          ipSubnet: '127.0.0',
        },
      });
      setRegToken(response.data.accessToken);
      setRegRefreshToken(response.data.refreshToken ?? null);
      toast.success('Account created! Check your email for the 6-digit OTP.');
      setShowOtp(true);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status?: number; data?: { detail?: string } } };
      if (axiosErr.response?.status === 409) {
        toast.info('Account already exists. Sending a new verification code…');
        try {
          await api.post('/api/v1/otp/send', { email: data.email, purpose: 'EMAIL_VERIFICATION', channel: 'email' });
        } catch { /* non-fatal */ }
        setShowOtp(true);
      } else {
        toast.error(axiosErr.response?.data?.detail ?? 'Registration failed');
        setCaptchaToken(null);
      }
    } finally {
      setIsRegistering(false);
    }
  }

  // Academic Details submit (step 2 for STUDENT)
  async function onStep3Submit(data: Step3Data) {
    setIsValidatingCode(true);
    try {
      let resolvedCenterId = centerId;
      if (!centerName || !resolvedCenterId) {
        const resp = await api.get(`/api/v1/centers/lookup?code=${encodeURIComponent(data.institutionCode)}`);
        resolvedCenterId = resp.data.id;
        setCenterId(resolvedCenterId);
        setCenterName(resp.data.name);
      }
      setStep3Data(data);
      if (regToken && resolvedCenterId) await loadSubjects(regToken, resolvedCenterId);
      goNext();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status?: number } };
      if (axiosErr.response?.status === 404) {
        toast.error('Institution code not found. Please check with your school.');
      } else {
        toast.error('Failed to validate institution code. Please try again.');
      }
    } finally {
      setIsValidatingCode(false);
    }
  }

  function onConsentSubmit(data: ConsentData) {
    setParentEmail(data.parentEmail);
    goNext();
  }

  async function loadSubjects(token: string, cId?: string) {
    const resolvedId = cId ?? centerId;
    if (!resolvedId) return;
    setIsLoadingSubjects(true);
    try {
      const { data } = await axios.get(`/api/v1/centers/${resolvedId}/batches?size=100`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const batches = Array.isArray(data) ? data : (data.content ?? []);
      const subjects = [...new Set(batches.map((b: { subject?: string }) => b.subject as string).filter(Boolean))].sort() as string[];
      setAvailableSubjects(subjects);
    } catch {
      // non-fatal — user can skip
    } finally {
      setIsLoadingSubjects(false);
    }
  }

  function toggleSubject(subject: string) {
    setSelectedSubjects((prev) =>
      prev.includes(subject) ? prev.filter((s) => s !== subject) : [...prev, subject]
    );
  }

  async function onSubjectsContinue() {
    if (availableSubjects.length > 0 && selectedSubjects.length === 0) {
      toast.error('Please select at least one subject');
      return;
    }
    // Create student profile after subjects are selected
    if (step1Data && step3Data && regToken) {
      try {
        const payload = JSON.parse(atob(regToken.split('.')[1]));
        const userId = payload.sub as string;
        await axios.post(
          '/api/v1/students',
          {
            userId,
            firstName: step1Data.firstName,
            lastName: step1Data.lastName,
            email: step1Data.email,
            phone: step1Data.phone || undefined,
            gender: selectedGender || undefined,
            dateOfBirth: step1Data.dateOfBirth!,
            city: undefined,
            state: undefined,
            pincode: undefined,
            board: step3Data.board,
            currentClass: step3Data.grade,
            subjects: selectedSubjects,
          },
          { headers: { Authorization: `Bearer ${regToken}` } }
        );
      } catch (profileErr) {
        console.error('Student profile creation failed (non-fatal):', profileErr);
      }
    }
    toast.success('Profile complete! You can now sign in.');
    navigate('/login');
  }

  async function onResendOtp() {
    if (!step1Data) return;
    setIsResending(true);
    try {
      const res = await api.post('/api/v1/otp/send', { email: step1Data.email, purpose: 'EMAIL_VERIFICATION', channel: 'email' });
      const remaining = res.data?.resendsRemaining ?? null;
      setResendsRemaining(remaining);
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

      if (selectedRole === 'PARENT' && step1Data && regToken) {
        try {
          const meRes = await axios.get('/api/v1/auth/me', { headers: { Authorization: `Bearer ${regToken}` } });
          const u = meRes.data;
          const name = [u.firstName, u.lastName].filter(Boolean).join(' ') || u.email;
          setAuth(regToken, { id: u.id, email: u.email, role: u.role, name }, regRefreshToken ?? '', regDeviceId ?? crypto.randomUUID());
          await axios.post('/api/v1/parents', {
            name,
            phone: parentPhone || undefined,
            email: step1Data.email,
            occupation: parentOccupation || undefined,
            gender: selectedGender || undefined,
          }, { headers: { Authorization: `Bearer ${regToken}` } });
          toast.success('Account created! Welcome to NexusEd.');
        } catch (parentErr) {
          console.error('Parent profile setup failed:', parentErr);
          toast.success('Email verified!');
        }
        navigate('/parent');
        return;
      }

      // TEACHER: self-register pending approval
      if (selectedRole === 'TEACHER' && teacherCenterId && regToken && step1Data) {
        try {
          await axios.post(
            `/api/v1/centers/${teacherCenterId}/teachers/self-register`,
            {
              firstName: step1Data.firstName,
              lastName: step1Data.lastName,
              email: step1Data.email,
              phoneNumber: step1Data.phone || undefined,
              subjects: teacherSubjectsInput.trim() || undefined,
            },
            { headers: { Authorization: `Bearer ${regToken}` } }
          );
          toast.success('Registration submitted! Awaiting approval from your institution coordinator.');
        } catch {
          // Non-fatal
        }
        navigate('/login');
        return;
      }

      // STUDENT: move to Academic Details (step 2)
      if (selectedRole === 'STUDENT') {
        toast.success('Email verified! Now tell us about your studies.');
        setShowOtp(false);
        goNext();
        return;
      }

      // CENTER_ADMIN: create their institution in center-svc then redirect
      if (selectedRole === 'CENTER_ADMIN') {
        if (regToken) {
          try {
            await axios.post(
              '/api/v1/centers/self-register',
              {
                name: institutionName.trim(),
                city: institutionCity.trim(),
                phone: institutionPhone.trim(),
              },
              { headers: { Authorization: `Bearer ${regToken}` } }
            );
            toast.success('Institution registered! You can now sign in.');
          } catch {
            toast.success('Account created! Institution details can be set up after sign-in.');
          }
        } else {
          toast.success('Institution account created! You can now sign in.');
        }
        navigate('/login');
        return;
      }

      // TEACHER without centerId or other cases
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

  // Step indices (steps 1-based; OTP is now inline in step 1)
  const consentStep   = isUnder13 ? 3 : null;
  const subjectsStep  = isUnder13 ? 4 : 3;

  return (
    <div className="min-h-screen bg-surface flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="flex items-center gap-3 mb-5 justify-center">
          <div className="p-2 rounded-xl bg-brand-600/20 border border-brand-500/30">
            <BookOpen className="w-5 h-5 text-brand-400" />
          </div>
          <span className="font-bold text-lg text-white">NexusEd</span>
        </div>

        {/* Progress bar */}
        <div className="mb-5">
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
              animate={{ width: `${((step - 1) / Math.max(steps.length - 1, 1)) * 100}%` }}
              transition={{ duration: 0.4, ease: 'easeOut' }}
            />
          </div>
        </div>

        {/* Step content */}
        <div className="glass rounded-2xl p-4 overflow-hidden relative min-h-[380px]">
          <AnimatePresence custom={direction} mode="wait">

            {/* ── Step 1: Personal Details (merged: role + details + role-specific + captcha + OTP inline) ── */}
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
                <AnimatePresence mode="wait">

                  {/* Phase A: Registration Form */}
                  {!showOtp && (
                    <motion.div
                      key="step1-form"
                      initial={{ opacity: 0, x: 20 }}
                      animate={{ opacity: 1, x: 0 }}
                      exit={{ opacity: 0, x: -20 }}
                      transition={{ duration: 0.25 }}
                    >
                      <h2 className="text-2xl font-bold text-white mb-1">Create your account</h2>
                      <p className="text-white/40 mb-6 text-sm">Get started with NexusEd today.</p>

                      {GOOGLE_CLIENT_ID && (
                        <div className="mb-5">
                          <GoogleSignInButton
                            onSuccess={handleGoogleSuccess}
                            onError={() => toast.error('Google Sign-In was cancelled or failed.')}
                            loading={isGoogleLoading}
                            label="Continue with Google"
                          />
                          <div className="flex items-center gap-3 mt-4 mb-2">
                            <div className="flex-1 h-px bg-white/10" />
                            <span className="text-white/30 text-xs">or sign up with email</span>
                            <div className="flex-1 h-px bg-white/10" />
                          </div>
                        </div>
                      )}

                      <form onSubmit={handleSubmit(onStep1Submit)} className="space-y-4">

                        {/* Role dropdown — before first name */}
                        <div>
                          <label className="block text-sm font-medium text-white/70 mb-1.5">I am a</label>
                          <div className="relative">
                            <select
                              value={selectedRole ?? ''}
                              onChange={(e) => setSelectedRole((e.target.value as Role) || null)}
                              className={cn(
                                'input w-full appearance-none pr-10',
                                !selectedRole && 'text-white/30'
                              )}
                            >
                              <option value="" disabled>Select your role</option>
                              {roleOptions.map(({ role, label }) => (
                                <option key={role} value={role}>{label}</option>
                              ))}
                            </select>
                            <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30 pointer-events-none" />
                          </div>
                          {selectedRole && (
                            <p className="text-white/30 text-xs mt-1.5">
                              {roleOptions.find((r) => r.role === selectedRole)?.description}
                            </p>
                          )}
                        </div>

                        {/* First Name + Last Name */}
                        <div className="grid grid-cols-2 gap-3">
                          <div>
                            <label className="block text-sm font-medium text-white/70 mb-1.5">First Name</label>
                            <input
                              {...register('firstName')}
                              type="text"
                              placeholder="Jane"
                              className={cn('input w-full', errors.firstName && 'border-red-500/50')}
                            />
                            {errors.firstName && <p className="text-red-400 text-xs mt-1">{errors.firstName.message}</p>}
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-white/70 mb-1.5">Last Name</label>
                            <input
                              {...register('lastName')}
                              type="text"
                              placeholder="Smith"
                              className={cn('input w-full', errors.lastName && 'border-red-500/50')}
                            />
                            {errors.lastName && <p className="text-red-400 text-xs mt-1">{errors.lastName.message}</p>}
                          </div>
                        </div>

                        {/* Email */}
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

                        {/* Password */}
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

                        {/* Confirm Password */}
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

                        {/* Role-specific fields — STUDENT */}
                        {selectedRole === 'STUDENT' && (
                          <div className="space-y-3 pt-1">
                            <div className="h-px bg-white/5" />
                            <div className="grid grid-cols-2 gap-3">
                              <div>
                                <label className="block text-sm font-medium text-white/70 mb-1.5">Phone <span className="text-white/30">(optional)</span></label>
                                <input
                                  {...register('phone')}
                                  type="tel"
                                  placeholder="+91 98765 43210"
                                  className={cn('input w-full', errors.phone && 'border-red-500/50')}
                                />
                                {errors.phone && <p className="text-red-400 text-xs mt-1">{errors.phone.message}</p>}
                              </div>
                              <div>
                                <label className="block text-sm font-medium text-white/70 mb-1.5">Date of Birth <span className="text-red-400">*</span></label>
                                <input
                                  {...register('dateOfBirth')}
                                  type="date"
                                  className={cn('input w-full', errors.dateOfBirth && 'border-red-500/50')}
                                />
                                {errors.dateOfBirth && <p className="text-red-400 text-xs mt-1">{errors.dateOfBirth.message}</p>}
                              </div>
                            </div>
                          </div>
                        )}

                        {/* Role-specific fields — PARENT */}
                        {selectedRole === 'PARENT' && (
                          <div className="space-y-3 pt-1">
                            <div className="h-px bg-white/5" />
                            <div className="grid grid-cols-2 gap-3">
                              <div>
                                <label className="block text-sm font-medium text-white/70 mb-1.5">Phone Number</label>
                                <input
                                  type="tel"
                                  value={parentPhone}
                                  onChange={(e) => setParentPhone(e.target.value)}
                                  placeholder="+91 87654 32100"
                                  className="input w-full"
                                />
                              </div>
                              <div>
                                <label className="block text-sm font-medium text-white/70 mb-1.5">Occupation</label>
                                <input
                                  type="text"
                                  value={parentOccupation}
                                  onChange={(e) => setParentOccupation(e.target.value)}
                                  placeholder="e.g. Marketing Manager"
                                  className="input w-full"
                                />
                              </div>
                            </div>
                            <div>
                              <label className="block text-sm font-medium text-white/70 mb-1.5">Gender</label>
                              <select
                                value={selectedGender}
                                onChange={(e) => setSelectedGender(e.target.value)}
                                className="input w-full"
                              >
                                <option value="">— Select (optional) —</option>
                                <option value="MALE">Male</option>
                                <option value="FEMALE">Female</option>
                                <option value="OTHER">Other</option>
                                <option value="PREFER_NOT_TO_SAY">Prefer not to say</option>
                              </select>
                            </div>
                          </div>
                        )}

                        {/* Role-specific fields — TEACHER */}
                        {selectedRole === 'TEACHER' && (
                          <div className="space-y-3 pt-1">
                            <div className="h-px bg-white/5" />
                            <div>
                              <label className="block text-sm font-medium text-white/70 mb-1.5">Gender <span className="text-white/30">(optional)</span></label>
                              <select
                                value={selectedGender}
                                onChange={(e) => setSelectedGender(e.target.value)}
                                className="input w-full"
                              >
                                <option value="">— Select —</option>
                                <option value="MALE">Male</option>
                                <option value="FEMALE">Female</option>
                                <option value="OTHER">Other</option>
                                <option value="PREFER_NOT_TO_SAY">Prefer not to say</option>
                              </select>
                            </div>
                            <div>
                              <label className="block text-sm font-medium text-white/70 mb-1.5">
                                Institution Code <span className="text-white/30">(optional)</span>
                              </label>
                              <input
                                type="text"
                                value={teacherInstitutionCode}
                                onChange={async (e) => {
                                  const code = e.target.value;
                                  setTeacherInstitutionCode(code);
                                  setTeacherCenterId(null);
                                  setTeacherCenterName(null);
                                  if (code.trim().length >= 3) {
                                    try {
                                      const resp = await api.get(`/api/v1/centers/lookup?code=${encodeURIComponent(code.trim())}`);
                                      setTeacherCenterId(resp.data.id);
                                      setTeacherCenterName(resp.data.name);
                                    } catch { /* not found yet */ }
                                  }
                                }}
                                placeholder="e.g. NEXUS-DPS-001"
                                className="input w-full"
                              />
                              {teacherCenterName && (
                                <p className="text-green-400 text-xs mt-1">✓ {teacherCenterName}</p>
                              )}
                              {teacherInstitutionCode.trim().length >= 3 && !teacherCenterName && (
                                <p className="text-white/30 text-xs mt-1">Institution not found — you can leave this blank and add later.</p>
                              )}
                            </div>
                            <div>
                              <label className="block text-sm font-medium text-white/70 mb-1.5">
                                Subjects <span className="text-white/30">(comma-separated, optional)</span>
                              </label>
                              <input
                                type="text"
                                value={teacherSubjectsInput}
                                onChange={(e) => setTeacherSubjectsInput(e.target.value)}
                                placeholder="Mathematics, Physics"
                                className="input w-full"
                              />
                            </div>
                          </div>
                        )}

                        {/* Role-specific fields — INSTITUTION */}
                        {selectedRole === 'CENTER_ADMIN' && (
                          <div className="space-y-3 pt-1">
                            <div className="h-px bg-white/5" />
                            <div>
                              <label className="block text-sm font-medium text-white/70 mb-1.5">
                                Institution Name <span className="text-red-400">*</span>
                              </label>
                              <input
                                type="text"
                                value={institutionName}
                                onChange={(e) => setInstitutionName(e.target.value)}
                                placeholder="e.g. Delhi Public Coaching Centre"
                                className="input w-full"
                              />
                            </div>
                            <div className="grid grid-cols-2 gap-3">
                              <div>
                                <label className="block text-sm font-medium text-white/70 mb-1.5">
                                  City <span className="text-red-400">*</span>
                                </label>
                                <input
                                  type="text"
                                  value={institutionCity}
                                  onChange={(e) => setInstitutionCity(e.target.value)}
                                  placeholder="e.g. Mumbai"
                                  className="input w-full"
                                />
                              </div>
                              <div>
                                <label className="block text-sm font-medium text-white/70 mb-1.5">
                                  Phone <span className="text-red-400">*</span>
                                </label>
                                <input
                                  type="text"
                                  value={institutionPhone}
                                  onChange={(e) => setInstitutionPhone(e.target.value)}
                                  placeholder="+91 98765 43210"
                                  className="input w-full"
                                />
                              </div>
                            </div>
                          </div>
                        )}

                        {/* Captcha — shown when role is selected */}
                        {selectedRole && (
                          <div className="flex justify-center pt-1">
                            <CaptchaWidget onVerify={handleCaptchaVerify} />
                          </div>
                        )}

                        <button
                          type="submit"
                          disabled={!selectedRole || !captchaToken || isRegistering || isValidatingTeacherCode}
                          className="btn-primary w-full flex items-center justify-center gap-2 py-3 mt-2 disabled:opacity-50"
                        >
                          {isRegistering || isValidatingTeacherCode ? (
                            <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                          ) : (
                            <>Create Account <ArrowRight className="w-4 h-4" /></>
                          )}
                        </button>
                      </form>
                    </motion.div>
                  )}

                  {/* Phase B: Verify Email (inline OTP — same step 1) */}
                  {showOtp && (
                    <motion.div
                      key="step1-otp"
                      initial={{ opacity: 0, x: 20 }}
                      animate={{ opacity: 1, x: 0 }}
                      exit={{ opacity: 0, x: -20 }}
                      transition={{ duration: 0.25 }}
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
                          onClick={() => setShowOtp(false)}
                          className="text-xs text-white/30 hover:text-white/60 transition-colors"
                        >
                          <ArrowLeft className="w-3 h-3 inline mr-1" />
                          Go back
                        </button>
                        <div className="flex flex-col items-end gap-0.5">
                          <button
                            type="button"
                            onClick={onResendOtp}
                            disabled={isResending || resendsRemaining === 0}
                            className="text-xs text-brand-400 hover:text-brand-300 transition-colors disabled:opacity-50"
                          >
                            {isResending ? 'Sending…' : 'Resend code'}
                          </button>
                          {resendsRemaining !== null && (
                            <span className={cn(
                              'text-xs',
                              resendsRemaining === 0 ? 'text-red-400' : 'text-white/30'
                            )}>
                              {resendsRemaining === 0 ? 'Limit reached' : `${resendsRemaining} resend${resendsRemaining === 1 ? '' : 's'} left`}
                            </span>
                          )}
                        </div>
                      </div>
                    </motion.div>
                  )}

                </AnimatePresence>
              </motion.div>
            )}

            {/* ── Step 2 (STUDENT): Academic Details ── */}
            {step === 2 && selectedRole === 'STUDENT' && (
              <motion.div
                key="step2-academic"
                custom={direction}
                variants={slideVariants}
                initial="enter"
                animate="center"
                exit="exit"
                transition={{ duration: 0.3, ease: 'easeInOut' }}
              >
                <h2 className="text-2xl font-bold text-white mb-1">Academic Details</h2>
                <p className="text-white/40 mb-6 text-sm">Tell us about your school and grade.</p>

                <form onSubmit={handleSubmit3(onStep3Submit)} className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-white/70 mb-1.5">Institution Code</label>
                    <input
                      {...register3('institutionCode')}
                      type="text"
                      placeholder="e.g. SCH-2024-ABC"
                      className={cn('input w-full', errors3.institutionCode && 'border-red-500/50')}
                    />
                    {errors3.institutionCode && (
                      <p className="text-red-400 text-xs mt-1">{errors3.institutionCode.message}</p>
                    )}
                    {centerName && (
                      <p className="text-green-400 text-xs mt-1">✓ {centerName}</p>
                    )}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-white/70 mb-1.5">Board</label>
                    <select
                      {...register3('board')}
                      className={cn('input w-full', errors3.board && 'border-red-500/50')}
                    >
                      <option value="">Select board</option>
                      <option value="CBSE">CBSE</option>
                      <option value="ICSE">ICSE</option>
                      <option value="STATE_BOARD">State Board</option>
                      <option value="IB">IB</option>
                      <option value="IGCSE">IGCSE</option>
                    </select>
                    {errors3.board && <p className="text-red-400 text-xs mt-1">{errors3.board.message}</p>}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-white/70 mb-1.5">Grade</label>
                    <select
                      {...register3('grade')}
                      className={cn('input w-full', errors3.grade && 'border-red-500/50')}
                    >
                      <option value={10}>Grade 10</option>
                      <option value={11}>Grade 11</option>
                      <option value={12}>Grade 12</option>
                    </select>
                    {errors3.grade && <p className="text-red-400 text-xs mt-1">{errors3.grade.message}</p>}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-white/70 mb-1.5">Gender (optional)</label>
                    <select
                      value={selectedGender}
                      onChange={(e) => setSelectedGender(e.target.value)}
                      className="input w-full"
                    >
                      <option value="">— Select —</option>
                      <option value="MALE">Male</option>
                      <option value="FEMALE">Female</option>
                      <option value="OTHER">Other</option>
                      <option value="PREFER_NOT_TO_SAY">Prefer not to say</option>
                    </select>
                  </div>

                  <div className="flex gap-3 pt-2">
                    <button type="button" onClick={goBack} className="btn-ghost flex items-center gap-2 py-3 px-4">
                      <ArrowLeft className="w-4 h-4" /> Back
                    </button>
                    <button
                      type="submit"
                      disabled={isValidatingCode}
                      className="btn-primary flex-1 flex items-center justify-center gap-2 py-3"
                    >
                      {isValidatingCode ? (
                        <Loader2 className="w-5 h-5 animate-spin" />
                      ) : (
                        <>Continue <ArrowRight className="w-4 h-4" /></>
                      )}
                    </button>
                  </div>
                </form>
              </motion.div>
            )}

            {/* ── Parental Consent (STUDENT under-13) ── */}
            {step === consentStep && selectedRole === 'STUDENT' && isUnder13 && (
              <motion.div
                key="step-consent"
                custom={direction}
                variants={slideVariants}
                initial="enter"
                animate="center"
                exit="exit"
                transition={{ duration: 0.3, ease: 'easeInOut' }}
              >
                <h2 className="text-2xl font-bold text-white mb-1">Parental Consent Required</h2>
                <p className="text-white/40 mb-6 text-sm">
                  You appear to be under 13. A consent request will be sent to your parent or guardian.
                </p>

                <form onSubmit={handleSubmitConsent(onConsentSubmit)} className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-white/70 mb-1.5">Parent / Guardian Email</label>
                    <input
                      {...registerConsent('parentEmail')}
                      type="email"
                      placeholder="parent@example.com"
                      className={cn('input w-full', errorsConsent.parentEmail && 'border-red-500/50')}
                    />
                    {errorsConsent.parentEmail && (
                      <p className="text-red-400 text-xs mt-1">{errorsConsent.parentEmail.message}</p>
                    )}
                  </div>

                  <div className="flex gap-3 pt-2">
                    <button type="button" onClick={goBack} className="btn-ghost flex items-center gap-2 py-3 px-4">
                      <ArrowLeft className="w-4 h-4" /> Back
                    </button>
                    <button type="submit" className="btn-primary flex-1 flex items-center justify-center gap-2 py-3">
                      Continue <ArrowRight className="w-4 h-4" />
                    </button>
                  </div>
                </form>
              </motion.div>
            )}

            {/* ── Subjects (STUDENT) ── */}
            {step === subjectsStep && selectedRole === 'STUDENT' && (
              <motion.div
                key="step-subjects"
                custom={direction}
                variants={slideVariants}
                initial="enter"
                animate="center"
                exit="exit"
                transition={{ duration: 0.3, ease: 'easeInOut' }}
              >
                <h2 className="text-2xl font-bold text-white mb-1">Your Subjects</h2>
                <p className="text-white/40 mb-6 text-sm">Select the subjects you&apos;re studying.</p>

                {isLoadingSubjects ? (
                  <div className="flex items-center justify-center py-10">
                    <Loader2 className="w-8 h-8 text-brand-400 animate-spin" />
                  </div>
                ) : availableSubjects.length === 0 ? (
                  <p className="text-white/40 text-sm text-center py-8">
                    No subjects found for your institution. You can add them later in Settings.
                  </p>
                ) : (
                  <div className="flex flex-wrap gap-2 mb-6">
                    {availableSubjects.map((subject) => (
                      <button
                        key={subject}
                        type="button"
                        onClick={() => toggleSubject(subject)}
                        className={cn(
                          'px-4 py-2 rounded-full text-sm font-medium border transition-all duration-200',
                          selectedSubjects.includes(subject)
                            ? 'border-brand-500 bg-brand-500/20 text-brand-300'
                            : 'border-white/10 bg-surface-100/50 text-white/60 hover:border-white/20 hover:bg-surface-100'
                        )}
                      >
                        {subject}
                      </button>
                    ))}
                  </div>
                )}

                <div className="flex gap-3">
                  <button type="button" onClick={goBack} className="btn-ghost flex items-center gap-2 py-3 px-4">
                    <ArrowLeft className="w-4 h-4" /> Back
                  </button>
                  <button
                    type="button"
                    onClick={onSubjectsContinue}
                    className="btn-primary flex-1 flex items-center justify-center gap-2 py-3"
                  >
                    Finish <ArrowRight className="w-4 h-4" />
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
