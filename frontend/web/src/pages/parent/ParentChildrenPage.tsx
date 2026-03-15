import { useState, useEffect, useCallback } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Users, Plus, Trash2, Edit2, Save, X, ChevronDown, ChevronUp,
  Loader2, School, Calendar, BookOpen, Hash, Mail, ArrowRight, ArrowLeft,
  CheckCircle2, UserPlus, Link2, Eye, EyeOff, User,
} from 'lucide-react';
import { cn } from '../../lib/utils';
import { Avatar } from '../../components/ui/Avatar';
import { toast } from 'sonner';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';

// ─── Types ────────────────────────────────────────────────────────────────────

interface ParentProfileResponse {
  id: string;
  name: string;
  verified: boolean;
  status: string;
  createdAt: string;
}

interface StudentLinkResponse {
  id: string;
  parentId: string;
  studentId: string;
  studentName: string;
  centerId: string;
  status: string;
  relationship?: string;
  dateOfBirth?: string;
  schoolName?: string;
  standard?: string;
  board?: string;
  rollNumber?: string;
  createdAt: string;
}

interface StudentLookupResponse {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  city?: string;
  board?: string;
  currentClass?: number;
}

interface EditSchoolForm {
  dateOfBirth: string;
  schoolName: string;
  standard: string;
  board: string;
  rollNumber: string;
}

type AddChildStep = 'email-check' | 'personal' | 'academic' | 'subjects' | 'done';

interface PersonalForm {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  confirmPassword: string;
  phone: string;
  gender: string;
  relationship: string;
}

interface AcademicForm {
  dateOfBirth: string;
  institutionCode: string;
  board: string;
  currentClass: string;
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

function formatDate(iso: string | undefined): string {
  if (!iso) return '—';
  try {
    return new Date(iso).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  } catch {
    return iso;
  }
}

const statusColors: Record<string, string> = {
  ACTIVE: 'bg-emerald-500/15 text-emerald-400',
  REVOKED: 'bg-red-500/15 text-red-400',
  PENDING: 'bg-amber-500/15 text-amber-400',
};

const RELATIONSHIP_OPTIONS = [
  { value: 'MOTHER', label: 'Mother' },
  { value: 'FATHER', label: 'Father' },
  { value: 'GUARDIAN', label: 'Guardian' },
  { value: 'GRANDPARENT', label: 'Grandparent' },
  { value: 'SIBLING', label: 'Sibling' },
  { value: 'OTHER', label: 'Other' },
];

const GENDER_OPTIONS = [
  { value: 'MALE', label: 'Male' },
  { value: 'FEMALE', label: 'Female' },
  { value: 'OTHER', label: 'Other' },
  { value: 'PREFER_NOT_TO_SAY', label: 'Prefer not to say' },
];

const BOARD_OPTIONS = [
  { value: 'CBSE', label: 'CBSE' },
  { value: 'ICSE', label: 'ICSE' },
  { value: 'STATE_BOARD', label: 'State Board' },
  { value: 'IB', label: 'IB' },
  { value: 'IGCSE', label: 'IGCSE' },
];

function validatePassword(pw: string): string | null {
  if (pw.length < 8) return 'At least 8 characters required';
  if (!/[A-Z]/.test(pw)) return 'At least 1 uppercase letter required';
  if (!/[0-9]/.test(pw)) return 'At least 1 digit required';
  if (!/[^A-Za-z0-9]/.test(pw)) return 'At least 1 special character required';
  return null;
}

// ─── Step indicator ───────────────────────────────────────────────────────────

const STEP_LABELS = ['Personal Details', 'Academic Info', 'Subjects'];

function StepBar({ current }: { current: 0 | 1 | 2 }) {
  return (
    <div className="flex items-center gap-2 mb-6">
      {STEP_LABELS.map((label, i) => (
        <div key={label} className="flex items-center gap-2 flex-1 last:flex-none">
          <div className={cn(
            'w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0',
            i < current ? 'bg-brand-500 text-white' : i === current ? 'bg-brand-500/20 border border-brand-500 text-brand-400' : 'bg-white/5 text-white/30'
          )}>
            {i < current ? <CheckCircle2 className="w-4 h-4" /> : i + 1}
          </div>
          <span className={cn('text-xs hidden sm:block', i === current ? 'text-white/70' : 'text-white/30')}>{label}</span>
          {i < STEP_LABELS.length - 1 && <div className={cn('flex-1 h-px hidden sm:block', i < current ? 'bg-brand-500/50' : 'bg-white/10')} />}
        </div>
      ))}
    </div>
  );
}

// ─── Add Child Modal ───────────────────────────────────────────────────────────

function AddChildModal({
  profileId,
  parentName,
  onClose,
  onAdded,
}: {
  profileId: string;
  parentName: string;
  onClose: () => void;
  onAdded: () => void;
}) {
  const [step, setStep] = useState<AddChildStep>('email-check');
  const [emailInput, setEmailInput] = useState('');
  const [looking, setLooking] = useState(false);
  const [existingStudent, setExistingStudent] = useState<StudentLookupResponse | null>(null);
  const [emailChecked, setEmailChecked] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [centerName, setCenterName] = useState<string | null>(null);
  const [centerId, setCenterId] = useState<string | null>(null);
  const [codeValidating, setCodeValidating] = useState(false);
  const [subjects, setSubjects] = useState<string[]>([]);
  const [selectedSubjects, setSelectedSubjects] = useState<string[]>([]);
  const [loadingSubjects, setLoadingSubjects] = useState(false);
  const [createdStudentId, setCreatedStudentId] = useState<string | null>(null);
  const [createdStudentName, setCreatedStudentName] = useState('');

  // Personal form state
  const [personal, setPersonal] = useState<PersonalForm>({
    firstName: '', lastName: '', email: '',
    password: '', confirmPassword: '',
    phone: '', gender: '', relationship: 'MOTHER',
  });
  const [personalErrors, setPersonalErrors] = useState<Partial<PersonalForm>>({});

  // Academic form state
  const [academic, setAcademic] = useState<AcademicForm>({
    dateOfBirth: '', institutionCode: '', board: '', currentClass: '10',
  });
  const [academicErrors, setAcademicErrors] = useState<Partial<AcademicForm>>({});

  // Debounced institution code lookup
  useEffect(() => {
    const code = academic.institutionCode.trim();
    if (code.length < 3) { setCenterName(null); setCenterId(null); return; }
    const timer = setTimeout(async () => {
      setCodeValidating(true);
      try {
        const res = await api.get(`/api/v1/centers/lookup?code=${encodeURIComponent(code)}`);
        setCenterId(res.data.id);
        setCenterName(res.data.name);
      } catch {
        setCenterId(null);
        setCenterName(null);
      } finally {
        setCodeValidating(false);
      }
    }, 600);
    return () => clearTimeout(timer);
  }, [academic.institutionCode]);

  // OTP-based link state (for "existing student" suggestion)
  const [otpStep, setOtpStep] = useState<'lookup' | 'otp-sent'>('lookup');
  const [generating, setGenerating] = useState(false);
  const [otp, setOtp] = useState('');
  const [otpError, setOtpError] = useState('');
  const [verifying, setVerifying] = useState(false);
  const [otpExpiresAt, setOtpExpiresAt] = useState<Date | null>(null);
  const [secondsLeft, setSecondsLeft] = useState(300);

  useEffect(() => {
    if (otpStep !== 'otp-sent' || !otpExpiresAt) return;
    const interval = setInterval(() => {
      const secs = Math.max(0, Math.round((otpExpiresAt.getTime() - Date.now()) / 1000));
      setSecondsLeft(secs);
      if (secs <= 0) clearInterval(interval);
    }, 1000);
    return () => clearInterval(interval);
  }, [otpStep, otpExpiresAt]);

  const fmtTime = (s: number) => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`;

  // ── Email check ─────────────────────────────────────────────────────────────

  async function handleEmailCheck() {
    const email = emailInput.trim().toLowerCase();
    if (!email) return;
    setLooking(true);
    setExistingStudent(null);
    setEmailChecked(false);
    try {
      const res = await api.get(`/api/v1/students/lookup?email=${encodeURIComponent(email)}`);
      setExistingStudent(res.data as StudentLookupResponse);
    } catch {
      setExistingStudent(null);
    } finally {
      setLooking(false);
      setEmailChecked(true);
    }
  }

  function proceedToCreate() {
    setPersonal((p) => ({ ...p, email: emailInput.trim().toLowerCase() }));
    setStep('personal');
  }

  // ── OTP-based link for existing student ─────────────────────────────────────

  async function handleGenerateOtp() {
    if (!existingStudent) return;
    setGenerating(true);
    try {
      const res = await api.post('/api/v1/students/link-otp/generate', {
        studentEmail: existingStudent.email,
        parentName,
      });
      setOtpExpiresAt(new Date(res.data.expiresAt));
      setSecondsLeft(300);
      setOtpStep('otp-sent');
    } catch (e: unknown) {
      const err = e as { response?: { data?: { detail?: string } } };
      toast.error(err?.response?.data?.detail ?? 'Failed to send OTP. Try again.');
    } finally {
      setGenerating(false);
    }
  }

  async function handleVerifyOtpAndLink() {
    if (!existingStudent || otp.length !== 6) return;
    setOtpError('');
    setVerifying(true);
    try {
      const verifyRes = await api.post('/api/v1/students/link-otp/verify', {
        studentEmail: existingStudent.email,
        otp,
      });
      const verifyData = verifyRes.data;
      await api.post(`/api/v1/parents/${profileId}/students`, {
        studentId: verifyData.studentId,
        studentName: verifyData.studentName,
        centerId: '00000000-0000-0000-0000-000000000000',
        relationship: personal.relationship || 'PARENT',
      });
      toast.success(`${verifyData.studentName} linked to your account!`);
      onAdded();
      onClose();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { detail?: string; message?: string } }; message?: string };
      const msg = err?.response?.data?.detail ?? err?.response?.data?.message ?? err?.message ?? '';
      if (msg.toLowerCase().includes('otp') || msg.toLowerCase().includes('invalid') || msg.toLowerCase().includes('expired')) {
        setOtpError('Incorrect or expired OTP. Try again.');
      } else {
        toast.error(msg || 'Failed to link child.');
      }
    } finally {
      setVerifying(false);
    }
  }

  // ── Personal details validation ──────────────────────────────────────────────

  function validatePersonal(): boolean {
    const errors: Partial<PersonalForm> = {};
    if (!personal.firstName.trim()) errors.firstName = 'Required';
    if (!personal.lastName.trim()) errors.lastName = 'Required';
    if (!personal.email.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(personal.email)) errors.email = 'Valid email required';
    const pwErr = validatePassword(personal.password);
    if (pwErr) errors.password = pwErr;
    if (personal.password !== personal.confirmPassword) errors.confirmPassword = 'Passwords do not match';
    if (!personal.relationship) errors.relationship = 'Required';
    setPersonalErrors(errors);
    return Object.keys(errors).length === 0;
  }

  // ── Academic details validation ──────────────────────────────────────────────

  function validateAcademic(): boolean {
    const errors: Partial<AcademicForm> = {};
    if (!academic.dateOfBirth) errors.dateOfBirth = 'Required';
    if (!academic.board) errors.board = 'Required';
    if (!academic.currentClass) errors.currentClass = 'Required';
    setAcademicErrors(errors);
    return Object.keys(errors).length === 0;
  }

  // ── Academic step submit → load subjects ─────────────────────────────────────

  async function handleAcademicNext() {
    if (!validateAcademic()) return;

    // Resolve center if code entered
    if (academic.institutionCode.trim() && !centerId) {
      setCodeValidating(true);
      try {
        const res = await api.get(`/api/v1/centers/lookup?code=${encodeURIComponent(academic.institutionCode.trim())}`);
        setCenterId(res.data.id);
        setCenterName(res.data.name);
      } catch {
        toast.error('Institution code not found. You can skip this field.');
        setAcademicErrors((e) => ({ ...e, institutionCode: 'Code not found' }));
        setCodeValidating(false);
        return;
      } finally {
        setCodeValidating(false);
      }
    }

    // Load subjects if we have a center
    if (centerId) {
      setLoadingSubjects(true);
      try {
        const { data } = await api.get(`/api/v1/centers/${centerId}/batches?size=100`);
        const batches = Array.isArray(data) ? data : (data.content ?? []);
        const found = [...new Set(batches.map((b: { subject?: string }) => b.subject as string).filter(Boolean))].sort() as string[];
        setSubjects(found);
      } catch {
        setSubjects([]);
      } finally {
        setLoadingSubjects(false);
      }
    }

    setStep('subjects');
  }

  // ── Final submit ──────────────────────────────────────────────────────────────

  async function handleFinalSubmit() {
    setSubmitting(true);
    try {
      // 1. Register child account (no captcha, no device fingerprint)
      const registerRes = await api.post('/api/v1/auth/register-child', {
        email: personal.email,
        password: personal.password,
        firstName: personal.firstName,
        lastName: personal.lastName,
        phoneNumber: personal.phone || undefined,
      });
      const { userId } = registerRes.data as { userId: string; email: string };

      // 2. Create student profile
      const profileRes = await api.post('/api/v1/students', {
        userId,
        firstName: personal.firstName,
        lastName: personal.lastName,
        email: personal.email,
        phone: personal.phone || undefined,
        gender: personal.gender || undefined,
        dateOfBirth: academic.dateOfBirth,
        board: academic.board || undefined,
        currentClass: academic.currentClass ? parseInt(academic.currentClass) : undefined,
        subjects: selectedSubjects,
      });
      const studentProfileId = (profileRes.data as { id: string }).id;

      // 3. Link student to parent
      const studentName = `${personal.firstName} ${personal.lastName}`;
      await api.post(`/api/v1/parents/${profileId}/students`, {
        studentId: studentProfileId,
        studentName,
        centerId: centerId ?? '00000000-0000-0000-0000-000000000000',
        relationship: personal.relationship,
        dateOfBirth: academic.dateOfBirth || undefined,
        board: academic.board || undefined,
        standard: academic.currentClass ? `Class ${academic.currentClass}` : undefined,
      });

      setCreatedStudentId(studentProfileId);
      setCreatedStudentName(studentName);
      setStep('done');
    } catch (e: unknown) {
      const err = e as { response?: { data?: { detail?: string; message?: string } }; message?: string };
      const msg = err?.response?.data?.detail ?? err?.response?.data?.message ?? err?.message ?? 'Failed to add child.';
      toast.error(msg);
    } finally {
      setSubmitting(false);
    }
  }

  function toggleSubject(s: string) {
    setSelectedSubjects((p) => p.includes(s) ? p.filter((x) => x !== s) : [...p, s]);
  }

  function pField(key: keyof PersonalForm) {
    return {
      value: personal[key],
      onChange: (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        setPersonal((p) => ({ ...p, [key]: e.target.value }));
        setPersonalErrors((p) => ({ ...p, [key]: undefined }));
      },
    };
  }

  function aField(key: keyof AcademicForm) {
    return {
      value: academic[key],
      onChange: (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        setAcademic((p) => ({ ...p, [key]: e.target.value }));
        setAcademicErrors((p) => ({ ...p, [key]: undefined }));
      },
    };
  }

  const pwChecks = {
    length: personal.password.length >= 8,
    upper: /[A-Z]/.test(personal.password),
    digit: /[0-9]/.test(personal.password),
    special: /[^A-Za-z0-9]/.test(personal.password),
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm overflow-y-auto">
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        exit={{ opacity: 0, scale: 0.95 }}
        className="bg-surface-100 border border-white/10 rounded-2xl w-full max-w-lg shadow-2xl my-4"
      >
        {/* Header */}
        <div className="flex items-center justify-between p-6 pb-4 border-b border-white/5">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-brand-500/15 flex items-center justify-center">
              <UserPlus className="w-5 h-5 text-brand-400" />
            </div>
            <div>
              <h2 className="text-base font-bold text-white">
                {step === 'done' ? 'Child Added!' : 'Add Your Child'}
              </h2>
              <p className="text-xs text-white/40">
                {step === 'email-check' ? 'Create an account and link your child' :
                 step === 'personal' ? 'Step 1 of 3 — Personal details' :
                 step === 'academic' ? 'Step 2 of 3 — Academic info' :
                 step === 'subjects' ? 'Step 3 of 3 — Subjects' :
                 'Successfully linked to your account'}
              </p>
            </div>
          </div>
          <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-white/5 text-white/40 hover:text-white/70 transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6">
          <AnimatePresence mode="wait">

            {/* ── Email check ──────────────────────────────────────────── */}
            {step === 'email-check' && (
              <motion.div key="email-check" initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -20 }} className="space-y-5">
                <p className="text-sm text-white/50">
                  Enter your child's email address. We'll check if they already have an account so you don't create a duplicate.
                </p>

                <div>
                  <label className="block text-xs font-semibold text-white/50 uppercase tracking-wider mb-2">
                    <Mail className="w-3.5 h-3.5 inline mr-1" />
                    Child's Email Address
                  </label>
                  <div className="flex gap-2">
                    <input
                      type="email"
                      value={emailInput}
                      onChange={(e) => { setEmailInput(e.target.value); setEmailChecked(false); setExistingStudent(null); }}
                      onKeyDown={(e) => e.key === 'Enter' && handleEmailCheck()}
                      placeholder="child@example.com"
                      className="input flex-1"
                    />
                    <button
                      onClick={handleEmailCheck}
                      disabled={looking || !emailInput.trim()}
                      className="btn-primary px-4 py-2.5 text-sm flex items-center gap-1.5 disabled:opacity-50 whitespace-nowrap"
                    >
                      {looking ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Check'}
                    </button>
                  </div>
                </div>

                {/* Existing student found */}
                {emailChecked && existingStudent && (
                  <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} className="space-y-4">
                    <div className="bg-amber-500/10 border border-amber-500/20 rounded-xl p-4">
                      <p className="text-sm text-amber-300 font-semibold mb-1">Account already exists</p>
                      <div className="flex items-center gap-3 mt-2">
                        <div className="w-9 h-9 rounded-full bg-brand-500/20 flex items-center justify-center flex-shrink-0">
                          <span className="text-brand-400 font-bold text-xs">{existingStudent.firstName?.[0]}{existingStudent.lastName?.[0]}</span>
                        </div>
                        <div>
                          <div className="font-semibold text-white text-sm">{existingStudent.firstName} {existingStudent.lastName}</div>
                          <div className="text-xs text-white/40">
                            {[existingStudent.currentClass && `Class ${existingStudent.currentClass}`, existingStudent.board, existingStudent.city].filter(Boolean).join(' · ')}
                          </div>
                        </div>
                      </div>
                      <p className="text-xs text-white/50 mt-3">
                        A student with this email is already registered. You can link them to your account using OTP verification, or enter a different email to create a new account.
                      </p>
                    </div>

                    {otpStep === 'lookup' ? (
                      <div className="space-y-3">
                        <div>
                          <label className="block text-xs font-semibold text-white/50 uppercase tracking-wider mb-1.5">Your relationship to {existingStudent.firstName}</label>
                          <select
                            value={personal.relationship}
                            onChange={(e) => setPersonal((p) => ({ ...p, relationship: e.target.value }))}
                            className="input w-full"
                          >
                            {RELATIONSHIP_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                          </select>
                        </div>
                        <button
                          onClick={handleGenerateOtp}
                          disabled={generating}
                          className="w-full btn-primary py-2.5 text-sm flex items-center justify-center gap-2 disabled:opacity-50"
                        >
                          {generating ? <Loader2 className="w-4 h-4 animate-spin" /> : <Link2 className="w-4 h-4" />}
                          {generating ? 'Sending OTP…' : 'Link Existing Child (OTP)'}
                        </button>
                        <button
                          onClick={() => { setEmailChecked(false); setExistingStudent(null); setEmailInput(''); }}
                          className="w-full px-4 py-2.5 rounded-xl border border-white/10 text-sm text-white/60 hover:text-white hover:border-white/20 transition-colors"
                        >
                          Use a Different Email
                        </button>
                      </div>
                    ) : (
                      <div className="space-y-4">
                        <div className="bg-brand-500/10 border border-brand-500/20 rounded-xl p-4 text-sm">
                          <p className="text-brand-300 font-medium mb-1">OTP sent to student's portal</p>
                          <p className="text-xs text-white/50">
                            Ask <span className="text-white">{existingStudent.firstName}</span> to check Settings → Parent Link Request and share the 6-digit code.
                          </p>
                          <div className="flex items-center gap-2 mt-2">
                            <span className="text-xs text-white/30">Expires in</span>
                            <span className={`text-xs font-mono font-bold ${secondsLeft < 60 ? 'text-red-400' : 'text-brand-400'}`}>{fmtTime(secondsLeft)}</span>
                          </div>
                        </div>
                        <input
                          type="text"
                          value={otp}
                          maxLength={6}
                          onChange={(e) => { setOtp(e.target.value.replace(/\D/g, '')); setOtpError(''); }}
                          placeholder="6-digit OTP"
                          className="input w-full text-center font-mono text-2xl tracking-[0.4em]"
                        />
                        {otpError && <p className="text-xs text-red-400">{otpError}</p>}
                        <div className="flex gap-3">
                          <button onClick={() => setOtpStep('lookup')} className="flex-1 px-4 py-2.5 rounded-xl border border-white/10 text-sm text-white/60 hover:text-white transition-colors">Back</button>
                          <button
                            onClick={handleVerifyOtpAndLink}
                            disabled={otp.length !== 6 || verifying || secondsLeft <= 0}
                            className="flex-1 btn-primary py-2.5 text-sm flex items-center justify-center gap-2 disabled:opacity-50"
                          >
                            {verifying ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Verify & Link'}
                          </button>
                        </div>
                      </div>
                    )}
                  </motion.div>
                )}

                {/* Email not found — show proceed button */}
                {emailChecked && !existingStudent && (
                  <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} className="space-y-3">
                    <div className="bg-emerald-500/10 border border-emerald-500/20 rounded-xl p-4 flex items-start gap-3">
                      <CheckCircle2 className="w-4 h-4 text-emerald-400 mt-0.5 flex-shrink-0" />
                      <p className="text-sm text-emerald-300">Email available. Let's create an account for your child.</p>
                    </div>
                    <button onClick={proceedToCreate} className="w-full btn-primary py-2.5 text-sm flex items-center justify-center gap-2">
                      Create Account <ArrowRight className="w-4 h-4" />
                    </button>
                  </motion.div>
                )}

                {!emailChecked && (
                  <div className="flex gap-3 pt-1">
                    <button onClick={onClose} className="flex-1 px-4 py-2.5 rounded-xl border border-white/10 text-sm text-white/60 hover:text-white hover:border-white/20 transition-colors">
                      Cancel
                    </button>
                  </div>
                )}
              </motion.div>
            )}

            {/* ── Step 1: Personal Details ────────────────────────────── */}
            {step === 'personal' && (
              <motion.div key="personal" initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -20 }} className="space-y-4">
                <StepBar current={0} />

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-medium text-white/60 mb-1.5">First Name <span className="text-red-400">*</span></label>
                    <input {...pField('firstName')} placeholder="Arjun" className={cn('input w-full', personalErrors.firstName && 'border-red-500/50')} />
                    {personalErrors.firstName && <p className="text-xs text-red-400 mt-1">{personalErrors.firstName}</p>}
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-white/60 mb-1.5">Last Name <span className="text-red-400">*</span></label>
                    <input {...pField('lastName')} placeholder="Sharma" className={cn('input w-full', personalErrors.lastName && 'border-red-500/50')} />
                    {personalErrors.lastName && <p className="text-xs text-red-400 mt-1">{personalErrors.lastName}</p>}
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-medium text-white/60 mb-1.5">Email Address <span className="text-red-400">*</span></label>
                  <input {...pField('email')} type="email" placeholder="child@example.com" className={cn('input w-full', personalErrors.email && 'border-red-500/50')} />
                  {personalErrors.email && <p className="text-xs text-red-400 mt-1">{personalErrors.email}</p>}
                </div>

                <div>
                  <label className="block text-xs font-medium text-white/60 mb-1.5">Password <span className="text-red-400">*</span></label>
                  <div className="relative">
                    <input
                      {...pField('password')}
                      type={showPassword ? 'text' : 'password'}
                      placeholder="Min 8 chars, 1 upper, 1 digit, 1 special"
                      className={cn('input w-full pr-10', personalErrors.password && 'border-red-500/50')}
                    />
                    <button type="button" onClick={() => setShowPassword((p) => !p)} className="absolute right-3 top-1/2 -translate-y-1/2 text-white/30 hover:text-white/60">
                      {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                  {personal.password && (
                    <div className="flex gap-3 mt-2 flex-wrap">
                      {[
                        { ok: pwChecks.length, label: '8+ chars' },
                        { ok: pwChecks.upper, label: 'Uppercase' },
                        { ok: pwChecks.digit, label: 'Digit' },
                        { ok: pwChecks.special, label: 'Special' },
                      ].map(({ ok, label }) => (
                        <span key={label} className={cn('text-[11px] flex items-center gap-1', ok ? 'text-emerald-400' : 'text-white/30')}>
                          <span className={cn('w-1.5 h-1.5 rounded-full', ok ? 'bg-emerald-400' : 'bg-white/20')} />{label}
                        </span>
                      ))}
                    </div>
                  )}
                  {personalErrors.password && <p className="text-xs text-red-400 mt-1">{personalErrors.password}</p>}
                </div>

                <div>
                  <label className="block text-xs font-medium text-white/60 mb-1.5">Confirm Password <span className="text-red-400">*</span></label>
                  <div className="relative">
                    <input
                      {...pField('confirmPassword')}
                      type={showConfirmPassword ? 'text' : 'password'}
                      placeholder="Re-enter password"
                      className={cn('input w-full pr-10', personalErrors.confirmPassword && 'border-red-500/50')}
                    />
                    <button type="button" onClick={() => setShowConfirmPassword((p) => !p)} className="absolute right-3 top-1/2 -translate-y-1/2 text-white/30 hover:text-white/60">
                      {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                  {personalErrors.confirmPassword && <p className="text-xs text-red-400 mt-1">{personalErrors.confirmPassword}</p>}
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-medium text-white/60 mb-1.5">Phone (optional)</label>
                    <input {...pField('phone')} placeholder="+91 98765 43210" className="input w-full" />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-white/60 mb-1.5">Gender</label>
                    <select {...pField('gender')} className="input w-full">
                      <option value="">— Select —</option>
                      {GENDER_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                    </select>
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-medium text-white/60 mb-1.5">Your Relationship <span className="text-red-400">*</span></label>
                  <select {...pField('relationship')} className={cn('input w-full', personalErrors.relationship && 'border-red-500/50')}>
                    {RELATIONSHIP_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                  </select>
                  {personalErrors.relationship && <p className="text-xs text-red-400 mt-1">{personalErrors.relationship}</p>}
                </div>

                <div className="flex gap-3 pt-2">
                  <button onClick={() => setStep('email-check')} className="flex items-center gap-1.5 px-4 py-2.5 rounded-xl border border-white/10 text-sm text-white/60 hover:text-white hover:border-white/20 transition-colors">
                    <ArrowLeft className="w-4 h-4" /> Back
                  </button>
                  <button
                    onClick={() => { if (validatePersonal()) setStep('academic'); }}
                    className="flex-1 btn-primary py-2.5 text-sm flex items-center justify-center gap-2"
                  >
                    Next: Academic Info <ArrowRight className="w-4 h-4" />
                  </button>
                </div>
              </motion.div>
            )}

            {/* ── Step 2: Academic Details ────────────────────────────── */}
            {step === 'academic' && (
              <motion.div key="academic" initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -20 }} className="space-y-4">
                <StepBar current={1} />

                <div>
                  <label className="block text-xs font-medium text-white/60 mb-1.5">Date of Birth <span className="text-red-400">*</span></label>
                  <input {...aField('dateOfBirth')} type="date" max={new Date().toISOString().split('T')[0]} className={cn('input w-full', academicErrors.dateOfBirth && 'border-red-500/50')} />
                  {academicErrors.dateOfBirth && <p className="text-xs text-red-400 mt-1">{academicErrors.dateOfBirth}</p>}
                </div>

                <div>
                  <label className="block text-xs font-medium text-white/60 mb-1.5">
                    Institution Code <span className="text-white/30 font-normal">(optional)</span>
                  </label>
                  <div className="relative">
                    <input
                      {...aField('institutionCode')}
                      placeholder="e.g. NEXED-2024"
                      className={cn('input w-full', academicErrors.institutionCode && 'border-red-500/50')}
                    />
                    {codeValidating && <Loader2 className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 animate-spin text-white/40" />}
                  </div>
                  {centerName && (
                    <div className="flex items-center gap-1.5 mt-1.5">
                      <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                      <span className="text-xs text-emerald-400">{centerName}</span>
                    </div>
                  )}
                  {academicErrors.institutionCode && <p className="text-xs text-red-400 mt-1">{academicErrors.institutionCode}</p>}
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-medium text-white/60 mb-1.5">Board <span className="text-red-400">*</span></label>
                    <select {...aField('board')} className={cn('input w-full', academicErrors.board && 'border-red-500/50')}>
                      <option value="">— Select —</option>
                      {BOARD_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                    </select>
                    {academicErrors.board && <p className="text-xs text-red-400 mt-1">{academicErrors.board}</p>}
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-white/60 mb-1.5">Class <span className="text-red-400">*</span></label>
                    <select {...aField('currentClass')} className={cn('input w-full', academicErrors.currentClass && 'border-red-500/50')}>
                      <option value="">— Select —</option>
                      {[10, 11, 12].map((c) => <option key={c} value={c}>Class {c}</option>)}
                    </select>
                    {academicErrors.currentClass && <p className="text-xs text-red-400 mt-1">{academicErrors.currentClass}</p>}
                  </div>
                </div>

                <div className="flex gap-3 pt-2">
                  <button onClick={() => setStep('personal')} className="flex items-center gap-1.5 px-4 py-2.5 rounded-xl border border-white/10 text-sm text-white/60 hover:text-white hover:border-white/20 transition-colors">
                    <ArrowLeft className="w-4 h-4" /> Back
                  </button>
                  <button
                    onClick={handleAcademicNext}
                    disabled={codeValidating}
                    className="flex-1 btn-primary py-2.5 text-sm flex items-center justify-center gap-2 disabled:opacity-50"
                  >
                    {codeValidating ? <Loader2 className="w-4 h-4 animate-spin" /> : null}
                    Next: Subjects <ArrowRight className="w-4 h-4" />
                  </button>
                </div>
              </motion.div>
            )}

            {/* ── Step 3: Subjects ────────────────────────────────────── */}
            {step === 'subjects' && (
              <motion.div key="subjects" initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -20 }} className="space-y-4">
                <StepBar current={2} />

                {loadingSubjects ? (
                  <div className="flex items-center justify-center py-12">
                    <Loader2 className="w-6 h-6 text-brand-400 animate-spin" />
                  </div>
                ) : subjects.length > 0 ? (
                  <>
                    <p className="text-sm text-white/50">Select the subjects your child will study at {centerName ?? 'the institution'}.</p>
                    <div className="flex flex-wrap gap-2">
                      {subjects.map((s) => (
                        <button
                          key={s}
                          onClick={() => toggleSubject(s)}
                          className={cn(
                            'px-3 py-1.5 rounded-lg text-sm font-medium border transition-all',
                            selectedSubjects.includes(s)
                              ? 'border-brand-500 bg-brand-500/20 text-brand-300'
                              : 'border-white/10 bg-white/5 text-white/50 hover:border-white/20 hover:text-white/70'
                          )}
                        >
                          {s}
                        </button>
                      ))}
                    </div>
                  </>
                ) : (
                  <div className="text-center py-8">
                    <BookOpen className="w-10 h-10 text-white/15 mx-auto mb-3" />
                    <p className="text-white/40 text-sm">No subjects found{centerId ? ' for this institution' : ' — no institution linked'}.</p>
                    <p className="text-white/30 text-xs mt-1">You can add subjects later from your child's profile.</p>
                  </div>
                )}

                {/* Summary card */}
                <div className="bg-white/3 border border-white/5 rounded-xl p-4 space-y-2">
                  <div className="text-xs font-semibold text-white/50 uppercase tracking-wider mb-3">Summary</div>
                  <div className="grid grid-cols-2 gap-x-4 gap-y-2 text-xs">
                    <div><span className="text-white/40">Name:</span> <span className="text-white ml-1">{personal.firstName} {personal.lastName}</span></div>
                    <div><span className="text-white/40">Class:</span> <span className="text-white ml-1">{academic.currentClass ? `Class ${academic.currentClass}` : '—'}</span></div>
                    <div><span className="text-white/40">Board:</span> <span className="text-white ml-1">{academic.board || '—'}</span></div>
                    <div><span className="text-white/40">Relationship:</span> <span className="text-white ml-1">{personal.relationship}</span></div>
                    {centerName && <div className="col-span-2"><span className="text-white/40">Institution:</span> <span className="text-white ml-1">{centerName}</span></div>}
                  </div>
                </div>

                <div className="flex gap-3 pt-2">
                  <button onClick={() => setStep('academic')} className="flex items-center gap-1.5 px-4 py-2.5 rounded-xl border border-white/10 text-sm text-white/60 hover:text-white hover:border-white/20 transition-colors">
                    <ArrowLeft className="w-4 h-4" /> Back
                  </button>
                  <button
                    onClick={handleFinalSubmit}
                    disabled={submitting || (subjects.length > 0 && selectedSubjects.length === 0)}
                    className="flex-1 btn-primary py-2.5 text-sm flex items-center justify-center gap-2 disabled:opacity-50"
                  >
                    {submitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <UserPlus className="w-4 h-4" />}
                    {submitting ? 'Creating account…' : 'Create & Link Child'}
                  </button>
                </div>
                {subjects.length > 0 && selectedSubjects.length === 0 && (
                  <p className="text-xs text-white/30 text-center">Select at least one subject to continue</p>
                )}
              </motion.div>
            )}

            {/* ── Done ─────────────────────────────────────────────────── */}
            {step === 'done' && (
              <motion.div key="done" initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} className="text-center py-4 space-y-5">
                <div className="w-16 h-16 rounded-full bg-emerald-500/15 flex items-center justify-center mx-auto">
                  <CheckCircle2 className="w-8 h-8 text-emerald-400" />
                </div>
                <div>
                  <h3 className="text-xl font-bold text-white mb-2">{createdStudentName}</h3>
                  <p className="text-white/50 text-sm">Account created and linked to your parent profile successfully.</p>
                  {createdStudentId && (
                    <p className="text-xs text-white/30 mt-2 font-mono">ID: {createdStudentId.slice(0, 16)}…</p>
                  )}
                </div>
                <div className="bg-brand-500/10 border border-brand-500/20 rounded-xl p-4 text-left space-y-2">
                  <p className="text-xs font-semibold text-brand-400 uppercase tracking-wider">Next steps</p>
                  <ul className="text-xs text-white/50 space-y-1.5">
                    <li>• Your child can now log in with the email and password you set</li>
                    <li>• They can update their profile and preferences in Settings</li>
                    <li>• You can monitor their progress from your parent dashboard</li>
                  </ul>
                </div>
                <button
                  onClick={() => { onAdded(); onClose(); }}
                  className="w-full btn-primary py-2.5 text-sm font-medium"
                >
                  Done
                </button>
              </motion.div>
            )}

          </AnimatePresence>
        </div>
      </motion.div>
    </div>
  );
}

// ─── Child Card ───────────────────────────────────────────────────────────────

function ChildCard({
  link,
  profileId,
  onRemove,
  onUpdated,
}: {
  link: StudentLinkResponse;
  profileId: string;
  onRemove: (linkId: string) => void;
  onUpdated: () => void;
}) {
  const [expanded, setExpanded] = useState(false);
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState<EditSchoolForm>({
    dateOfBirth: link.dateOfBirth ?? '',
    schoolName: link.schoolName ?? '',
    standard: link.standard ?? '',
    board: link.board ?? '',
    rollNumber: link.rollNumber ?? '',
  });

  const editMutation = useMutation({
    mutationFn: (data: EditSchoolForm) =>
      api.patch(`/api/v1/parents/${profileId}/students/${link.id}`, data),
    onSuccess: () => {
      toast.success('School details updated.');
      onUpdated();
      setEditing(false);
    },
    onError: (e: unknown) => {
      const err = e as { response?: { data?: { message?: string } }; message?: string };
      toast.error(err?.response?.data?.message ?? err?.message ?? 'Failed to update.');
    },
  });

  function field(key: keyof EditSchoolForm) {
    return {
      value: form[key],
      onChange: (ev: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
        setForm((p) => ({ ...p, [key]: ev.target.value })),
    };
  }

  const statusColor = statusColors[link.status?.toUpperCase()] ?? 'bg-white/5 text-white/40';

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      className="card border border-white/5"
    >
      <div className="flex items-center gap-4">
        <Avatar name={link.studentName} size="md" />
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2 flex-wrap">
            <h3 className="font-semibold text-white">{link.studentName}</h3>
            <span className={cn('inline-flex text-xs px-2 py-0.5 rounded-full font-medium', statusColor)}>
              {link.status}
            </span>
          </div>
          <p className="text-xs text-white/40 mt-0.5">
            {link.relationship ?? 'Parent'} · Linked {formatDate(link.createdAt)}
          </p>
        </div>
        <div className="flex items-center gap-2 flex-shrink-0">
          <button
            onClick={() => { setExpanded((p) => !p); if (editing) setEditing(false); }}
            className="p-2 rounded-lg hover:bg-white/5 text-white/30 hover:text-white/70 transition-colors"
          >
            {expanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
          </button>
          <button
            onClick={() => onRemove(link.id)}
            className="p-2 rounded-lg hover:bg-red-500/10 text-white/20 hover:text-red-400 transition-colors"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        </div>
      </div>

      <AnimatePresence initial={false}>
        {expanded && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: 0.2 }}
            className="overflow-hidden"
          >
            <div className="mt-4 pt-4 border-t border-white/5">
              {!editing ? (
                <>
                  <div className="flex items-center justify-between mb-3">
                    <h4 className="text-xs font-semibold text-white/50 uppercase tracking-wider">School Details</h4>
                    <button
                      onClick={() => setEditing(true)}
                      className="flex items-center gap-1.5 text-xs text-brand-400 hover:text-brand-300 transition-colors"
                    >
                      <Edit2 className="w-3.5 h-3.5" /> Edit
                    </button>
                  </div>
                  <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
                    {[
                      { icon: Calendar, label: 'Date of Birth', value: formatDate(link.dateOfBirth) },
                      { icon: School, label: 'School Name', value: link.schoolName || '—' },
                      { icon: BookOpen, label: 'Standard', value: link.standard || '—' },
                      { icon: BookOpen, label: 'Board', value: link.board || '—' },
                      { icon: Hash, label: 'Roll Number', value: link.rollNumber || '—' },
                    ].map(({ icon: Icon, label, value }) => (
                      <div key={label}>
                        <div className="flex items-center gap-1.5 text-xs text-white/40 mb-1">
                          <Icon className="w-3 h-3" /> {label}
                        </div>
                        <div className="text-sm text-white">{value}</div>
                      </div>
                    ))}
                  </div>
                </>
              ) : (
                <form onSubmit={(e) => { e.preventDefault(); editMutation.mutate(form); }} className="space-y-4">
                  <div className="flex items-center justify-between mb-1">
                    <h4 className="text-xs font-semibold text-white/50 uppercase tracking-wider">Edit School Details</h4>
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-medium text-white/60 mb-1.5">Date of Birth</label>
                      <input {...field('dateOfBirth')} type="date" className="input w-full" />
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-white/60 mb-1.5">School Name</label>
                      <input {...field('schoolName')} placeholder="Delhi Public School" className="input w-full" />
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-white/60 mb-1.5">Standard / Class</label>
                      <input {...field('standard')} placeholder="11th" className="input w-full" />
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-white/60 mb-1.5">Board</label>
                      <select {...field('board')} className="input w-full">
                        <option value="">— Select board —</option>
                        <option value="CBSE">CBSE</option>
                        <option value="ICSE">ICSE</option>
                        <option value="STATE">State Board</option>
                        <option value="IB">IB</option>
                        <option value="OTHER">Other</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-white/60 mb-1.5">Roll Number</label>
                      <input {...field('rollNumber')} placeholder="2024001" className="input w-full" />
                    </div>
                  </div>
                  <div className="flex gap-3">
                    <button type="button" onClick={() => setEditing(false)} className="flex items-center gap-1.5 px-4 py-2.5 rounded-xl border border-white/10 text-sm text-white/60 hover:text-white hover:border-white/20 transition-colors">
                      <X className="w-4 h-4" /> Cancel
                    </button>
                    <button type="submit" disabled={editMutation.isPending} className="btn-primary flex items-center gap-1.5 px-5 py-2.5 text-sm font-medium disabled:opacity-50">
                      {editMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
                      {editMutation.isPending ? 'Saving…' : 'Save'}
                    </button>
                  </div>
                </form>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────

export default function ParentChildrenPage() {
  const queryClient = useQueryClient();
  const [showAddModal, setShowAddModal] = useState(false);
  const authUser = useAuthStore((s) => s.user);
  const parentName: string = authUser?.name ?? 'Parent';

  const { data: profile, isLoading: profileLoading } = useQuery<ParentProfileResponse>({
    queryKey: ['parent-profile'],
    queryFn: () => api.get('/api/v1/parents/me').then((r) => r.data),
  });

  const { data: linkedStudents = [], isLoading: studentsLoading } = useQuery<StudentLinkResponse[]>({
    queryKey: ['linked-students', profile?.id],
    queryFn: () =>
      api.get(`/api/v1/parents/${profile!.id}/students?size=50`).then((r) => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
    enabled: !!profile?.id,
  });

  const removeMutation = useMutation({
    mutationFn: (linkId: string) => api.delete(`/api/v1/parents/${profile!.id}/students/${linkId}`),
    onSuccess: () => {
      toast.success('Child removed from your account.');
      queryClient.invalidateQueries({ queryKey: ['linked-students'] });
    },
    onError: () => toast.error('Failed to remove child link.'),
  });

  const isLoading = profileLoading || studentsLoading;

  if (isLoading) {
    return (
      <div className="p-4 lg:p-8 max-w-3xl mx-auto space-y-6">
        <div className="h-7 bg-white/10 rounded w-40 animate-pulse" />
        {[0, 1, 2].map((i) => (
          <div key={i} className="card animate-pulse">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 bg-white/10 rounded-full" />
              <div className="flex-1 space-y-2">
                <div className="h-4 bg-white/10 rounded w-32" />
                <div className="h-3 bg-white/10 rounded w-48" />
              </div>
            </div>
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="p-4 lg:p-8 max-w-3xl mx-auto space-y-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">My Children</h1>
          <p className="text-white/50 text-sm mt-0.5">
            Manage your children and their school details.
          </p>
        </div>
        {profile && (
          <button
            onClick={() => setShowAddModal(true)}
            className="btn-primary flex items-center gap-2 px-4 py-2.5 text-sm font-medium"
          >
            <Plus className="w-4 h-4" />
            Add Child
          </button>
        )}
      </div>

      {linkedStudents.length > 0 && (
        <div className="flex items-center gap-2 text-sm text-white/50">
          <Users className="w-4 h-4" />
          {linkedStudents.length} child{linkedStudents.length !== 1 ? 'ren' : ''} linked
        </div>
      )}

      {linkedStudents.length === 0 ? (
        <div className="card text-center py-16">
          <Users className="w-12 h-12 text-white/15 mx-auto mb-4" />
          <h3 className="text-white/50 font-medium mb-1">No children linked yet</h3>
          <p className="text-white/30 text-sm mb-6">
            Add your child's account to monitor their progress and manage school details.
          </p>
          {profile && (
            <button
              onClick={() => setShowAddModal(true)}
              className="btn-primary inline-flex items-center gap-2 px-5 py-2.5 text-sm"
            >
              <Plus className="w-4 h-4" />
              Add Your First Child
            </button>
          )}
        </div>
      ) : (
        <div className="space-y-4">
          {linkedStudents.map((link) => (
            <ChildCard
              key={link.id}
              link={link}
              profileId={profile!.id}
              onRemove={(linkId) => removeMutation.mutate(linkId)}
              onUpdated={() => queryClient.invalidateQueries({ queryKey: ['linked-students'] })}
            />
          ))}
        </div>
      )}

      <AnimatePresence>
        {showAddModal && profile && (
          <AddChildModal
            profileId={profile.id}
            parentName={parentName}
            onClose={() => setShowAddModal(false)}
            onAdded={() => queryClient.invalidateQueries({ queryKey: ['linked-students'] })}
          />
        )}
      </AnimatePresence>
    </div>
  );
}
