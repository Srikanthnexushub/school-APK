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
import { suggestStates, getCitiesForState, WORLD_COUNTRIES } from '../../utils/indiaLocations';

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID ?? '';

// ─── Location Autocomplete Input ────────────────────────────────────────────

function LocationInput({ label, value, onChange, suggestions, placeholder }: {
  label: string; value: string; onChange: (v: string) => void;
  suggestions: string[]; placeholder?: string;
}) {
  const [show, setShow] = useState(false);
  const filtered = suggestions.filter(s => s.toLowerCase().includes(value.toLowerCase())).slice(0, 8);
  return (
    <div className="relative">
      <label className="block text-sm font-medium text-white/70 mb-1.5">{label}</label>
      <input
        value={value}
        onChange={e => { onChange(e.target.value); setShow(true); }}
        onFocus={() => setShow(true)}
        onBlur={() => setTimeout(() => setShow(false), 150)}
        placeholder={placeholder || label}
        className="input w-full"
      />
      {show && filtered.length > 0 && value.length > 0 && (
        <div className="absolute z-50 mt-1 w-full bg-surface-100 border border-white/10 rounded-xl shadow-xl max-h-40 overflow-y-auto">
          {filtered.map(s => (
            <button key={s} type="button"
              onMouseDown={() => { onChange(s); setShow(false); }}
              className="w-full text-left px-3 py-2 text-sm text-white/80 hover:bg-white/5 hover:text-white"
            >{s}</button>
          ))}
        </div>
      )}
    </div>
  );
}

// ─── Creatable Input (dropdown with add-new option) ─────────────────────────

function CreatableInput({ label, value, onChange, placeholder, required }: {
  label: string; value: string; onChange: (v: string) => void;
  placeholder?: string; required?: boolean;
}) {
  const [show, setShow] = useState(false);
  const trimmed = value.trim();
  return (
    <div className="relative">
      <label className="block text-sm font-medium text-white/70 mb-1.5">
        {label}{required && <span className="text-red-400"> *</span>}
      </label>
      <input
        value={value}
        onChange={e => { onChange(e.target.value); setShow(true); }}
        onFocus={() => setShow(true)}
        onBlur={() => setTimeout(() => setShow(false), 150)}
        placeholder={placeholder || label}
        className="input w-full"
      />
      {show && trimmed && (
        <div className="absolute z-50 left-0 right-0 mt-1 rounded-lg border border-white/10 bg-gray-900/95 backdrop-blur-sm shadow-xl overflow-hidden">
          <button
            type="button"
            onMouseDown={() => { onChange(trimmed); setShow(false); }}
            className="w-full text-left px-3 py-2.5 text-sm text-brand-300 hover:bg-white/5 transition-colors flex items-center gap-2"
          >
            <span className="text-brand-400 font-bold">+</span>
            <span>Create &ldquo;{trimmed}&rdquo;</span>
          </button>
        </div>
      )}
    </div>
  );
}

// ─── Searchable Select (click to open, search inside, "Can't find" free-text) ─

function SearchableSelect({ label, value, onChange, options, placeholder, optional, required, allowCustom }: {
  label: string; value: string; onChange: (v: string) => void;
  options: string[]; placeholder?: string; optional?: boolean; required?: boolean; allowCustom?: boolean;
}) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [customMode, setCustomMode] = useState(false);
  const filtered = options.filter(o => o.toLowerCase().includes(search.toLowerCase())).slice(0, 12);

  if (customMode) {
    return (
      <div>
        <label className="block text-sm font-medium text-white/70 mb-1.5">
          {label}{required && <span className="text-red-400"> *</span>}{optional && <span className="text-white/30"> (optional)</span>}
        </label>
        <div className="flex gap-2">
          <input type="text" value={value} onChange={e => onChange(e.target.value)}
            placeholder={`Enter ${label.toLowerCase()}…`} className="input flex-1" autoFocus />
          <button type="button" onClick={() => { setCustomMode(false); onChange(''); }}
            className="px-3 py-2 text-xs text-white/40 hover:text-white/70 border border-white/10 rounded-lg transition-colors">
            ↩ Back
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="relative">
      <label className="block text-sm font-medium text-white/70 mb-1.5">
        {label}{required && <span className="text-red-400"> *</span>}{optional && <span className="text-white/30"> (optional)</span>}
      </label>
      <button type="button" onClick={() => { setOpen(o => !o); setSearch(''); }}
        onBlur={() => setTimeout(() => setOpen(false), 150)}
        className="input w-full text-left flex items-center justify-between">
        <span className={value ? 'text-white/90' : 'text-white/30'}>{value || placeholder || `Select ${label}`}</span>
        <ChevronDown className={cn('w-4 h-4 text-white/30 flex-shrink-0 transition-transform', open && 'rotate-180')} />
      </button>
      {open && (
        <div className="absolute z-50 mt-1 w-full bg-surface-100 border border-white/10 rounded-xl shadow-xl overflow-hidden">
          <div className="p-2 border-b border-white/5">
            <input type="text" value={search} onChange={e => setSearch(e.target.value)}
              placeholder={`Search ${label.toLowerCase()}…`} className="input w-full text-sm py-1.5" autoFocus />
          </div>
          <div className="max-h-44 overflow-y-auto">
            {filtered.length > 0 ? filtered.map(o => (
              <button key={o} type="button"
                onMouseDown={() => { onChange(o); setOpen(false); setSearch(''); }}
                className={cn('w-full text-left px-3 py-2 text-sm hover:bg-white/5 transition-colors',
                  o === value ? 'text-brand-300 font-medium' : 'text-white/80')}>
                {o}
              </button>
            )) : <p className="px-3 py-2 text-sm text-white/30">No matches</p>}
            {allowCustom && (
              <button type="button" onMouseDown={() => { setCustomMode(true); setOpen(false); setSearch(''); }}
                className="w-full text-left px-3 py-2.5 text-xs text-white/40 hover:bg-white/5 border-t border-white/10 transition-colors">
                Can't find? Enter manually
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

// ─── Autocomplete Input (type → live suggestions + "Can't find" fallback) ─────

function AutocompleteInput({ label, value, onChange, options, placeholder, optional, required }: {
  label: string; value: string; onChange: (v: string) => void;
  options: string[]; placeholder?: string; optional?: boolean; required?: boolean;
}) {
  const [show, setShow] = useState(false);
  const filtered = value
    ? options.filter(o => o.toLowerCase().includes(value.toLowerCase())).slice(0, 12)
    : options.slice(0, 12);

  return (
    <div className="relative">
      <label className="block text-sm font-medium text-white/70 mb-1.5">
        {label}{required && <span className="text-red-400"> *</span>}{optional && <span className="text-white/30"> (optional)</span>}
      </label>
      <input type="text" value={value}
        onChange={e => { onChange(e.target.value); setShow(true); }}
        onFocus={() => setShow(true)}
        onBlur={() => setTimeout(() => setShow(false), 150)}
        placeholder={placeholder || `Type to search…`}
        className="input w-full" />
      {show && (
        <div className="absolute z-50 mt-1 w-full bg-surface-100 border border-white/10 rounded-xl shadow-xl overflow-hidden">
          <div className="max-h-44 overflow-y-auto">
            {filtered.length > 0 ? filtered.map(o => (
              <button key={o} type="button"
                onMouseDown={() => { onChange(o); setShow(false); }}
                className={cn('w-full text-left px-3 py-2 text-sm hover:bg-white/5 transition-colors',
                  o === value ? 'text-brand-300 font-medium' : 'text-white/80')}>
                {o}
              </button>
            )) : <p className="px-3 py-2 text-sm text-white/30">No matches — your input will be used</p>}
            <button type="button" onMouseDown={() => setShow(false)}
              className="w-full text-left px-3 py-2.5 text-xs text-white/40 hover:bg-white/5 border-t border-white/10 transition-colors">
              Can't find? Keep what you typed
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

// ─── Multi-Select Dropdown (checkboxes, comma-separated display) ──────────────

function MultiSelectDropdown({ label, values, onChange, options, placeholder, required }: {
  label: string; values: string[]; onChange: (v: string[]) => void;
  options: { value: string; label: string }[]; placeholder?: string; required?: boolean;
}) {
  const [open, setOpen] = useState(false);
  const toggle = (val: string) =>
    onChange(values.includes(val) ? values.filter(v => v !== val) : [...values, val]);
  const display = values.length === 0
    ? (placeholder || `Select ${label}`)
    : options.filter(o => values.includes(o.value)).map(o => o.label).join(', ');

  return (
    <div className="relative">
      <label className="block text-sm font-medium text-white/70 mb-1.5">
        {label}{required && <span className="text-red-400"> *</span>}
      </label>
      <button type="button" onClick={() => setOpen(o => !o)}
        onBlur={() => setTimeout(() => setOpen(false), 150)}
        className="input w-full text-left flex items-center justify-between">
        <span className={cn('truncate text-sm', values.length === 0 ? 'text-white/30' : 'text-white/90')}>{display}</span>
        <ChevronDown className={cn('w-4 h-4 text-white/30 flex-shrink-0 ml-2 transition-transform', open && 'rotate-180')} />
      </button>
      {open && (
        <div className="absolute z-50 mt-1 w-full bg-surface-100 border border-white/10 rounded-xl shadow-xl overflow-hidden">
          <div className="max-h-48 overflow-y-auto divide-y divide-white/5">
            {options.map(({ value, label: optLabel }) => (
              <label key={value} className="flex items-center gap-3 px-3 py-2.5 hover:bg-white/5 cursor-pointer">
                <input type="checkbox" checked={values.includes(value)} onChange={() => toggle(value)}
                  className="w-4 h-4 accent-brand-500 cursor-pointer flex-shrink-0" />
                <span className="text-sm text-white/80">{optLabel}</span>
              </label>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

const BOARD_OPTIONS = [
  { value: 'CBSE',        label: 'CBSE' },
  { value: 'ICSE',        label: 'ICSE' },
  { value: 'STATE_BOARD', label: 'State Board' },
  { value: 'IB',          label: 'IB' },
  { value: 'IGCSE',       label: 'IGCSE / Cambridge' },
];

const step1Schema = z
  .object({
    firstName: z.string().max(100).optional().or(z.literal('')),
    lastName: z.string().max(100).optional().or(z.literal('')),
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
  institutionCode: z.string().optional().or(z.literal('')),
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

type Role = 'STUDENT' | 'PARENT' | 'TEACHER' | 'INSTITUTION_ADMIN';

const TEACHER_SUBJECTS = [
  'Mathematics', 'Physics', 'Chemistry', 'Biology', 'English', 'Hindi',
  'History', 'Geography', 'Economics', 'Political Science', 'Computer Science',
  'Accountancy', 'Business Studies', 'Sanskrit', 'Science', 'Social Studies',
  'Physical Education', 'Art', 'Music',
];

const roleOptions: { role: Role; label: string }[] = [
  { role: 'STUDENT',           label: 'Student' },
  { role: 'PARENT',            label: 'Parent / Guardian' },
  { role: 'TEACHER',           label: 'Teacher' },
  { role: 'INSTITUTION_ADMIN', label: 'Institution / Coaching Centre' },
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
  const [teacherCenterId, setTeacherCenterId] = useState<string | null>(null);
  const [teacherCenterName, setTeacherCenterName] = useState<string | null>(null);
  const [teacherCentersList, setTeacherCentersList] = useState<{ id: string; name: string }[]>([]);
  const [teacherCentersLoading, setTeacherCentersLoading] = useState(false);
  const [teacherSubjectsArr, setTeacherSubjectsArr] = useState<string[]>([]);
  const [teacherSubjectsOpen, setTeacherSubjectsOpen] = useState(false);
  const [teacherAddress, setTeacherAddress] = useState('');
  const [teacherCity, setTeacherCity] = useState('');
  const [teacherStateVal, setTeacherStateVal] = useState('');
  const [teacherDistrict, setTeacherDistrict] = useState('');
  // Institution-specific fields
  const [institutionName, setInstitutionName] = useState('');
  const [institutionCity, setInstitutionCity] = useState('');
  const [institutionPhone, setInstitutionPhone] = useState('');
  // Parent-specific fields
  const [parentPhone, setParentPhone] = useState('');
  const [parentOccupation, setParentOccupation] = useState('');
  const [selectedGender, setSelectedGender] = useState('');
  // Student location fields
  const [studentCity, setStudentCity] = useState('');
  const [studentStateVal, setStudentStateVal] = useState('');
  // Manual institution name (when no code resolved)
  const [manualInstitutionName, setManualInstitutionName] = useState('');
  // Institution (CENTER_ADMIN) state field
  const [instStateVal, setInstStateVal] = useState('');
  const [instBranch, setInstBranch] = useState('');
  const [instBoard, setInstBoard] = useState<string[]>([]);
  const [instAddressLine1, setInstAddressLine1] = useState('');
  const [instAddressLine2, setInstAddressLine2] = useState('');
  const [instPincode, setInstPincode] = useState('');
  const [instCountry, setInstCountry] = useState('');
  // Country fields for other roles
  const [studentCountry, setStudentCountry] = useState('');
  const [parentCountry, setParentCountry] = useState('');
  const [teacherCountry, setTeacherCountry] = useState('');
  // Parent location fields
  const [parentState, setParentState] = useState('');
  const [parentCity, setParentCity] = useState('');
  const [parentAddress, setParentAddress] = useState('');
  const [parentDistrict, setParentDistrict] = useState('');
  const [parentPincode, setParentPincode] = useState('');
  // Student pincode
  const [studentPincode, setStudentPincode] = useState('');

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

  // Fetch centers list when Teacher role is selected
  useEffect(() => {
    if (selectedRole !== 'TEACHER') return;
    setTeacherCentersLoading(true);
    api.get('/api/v1/centers?size=200')
      .then((r) => {
        const data = r.data;
        const list = Array.isArray(data) ? data : (data.content ?? []);
        setTeacherCentersList(list.map((c: { id: string; name: string }) => ({ id: c.id, name: c.name })));
      })
      .catch(() => setTeacherCentersList([]))
      .finally(() => setTeacherCentersLoading(false));
  }, [selectedRole]);

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

    // Validate firstName/lastName for non-CENTER_ADMIN roles (schema allows optional)
    if (selectedRole !== 'INSTITUTION_ADMIN') {
      if (!data.firstName?.trim()) { toast.error('First name is required'); return; }
      if (!data.lastName?.trim()) { toast.error('Last name is required'); return; }
    }

    // CENTER_ADMIN: validate institution fields
    if (selectedRole === 'INSTITUTION_ADMIN') {
      if (!institutionName.trim()) { toast.error('Institution name is required'); return; }
      if (!institutionCity.trim()) { toast.error('City is required'); return; }
      if (!institutionPhone.trim()) { toast.error('Institution phone is required'); return; }
    }

    setIsRegistering(true);
    try {
      const deviceId = crypto.randomUUID();
      setRegDeviceId(deviceId);
      const instWords = selectedRole === 'INSTITUTION_ADMIN' ? institutionName.trim().split(/\s+/) : [];
      const response = await api.post('/api/v1/auth/register', {
        firstName: selectedRole === 'INSTITUTION_ADMIN' ? (instWords[0] || institutionName.trim()) : data.firstName,
        lastName: selectedRole === 'INSTITUTION_ADMIN' ? (instWords.slice(1).join(' ') || instWords[0] || '-') : data.lastName,
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
    const code = data.institutionCode?.trim();
    if (code && code.length >= 3) {
      setIsValidatingCode(true);
      try {
        let resolvedCenterId = centerId;
        if (!centerName || !resolvedCenterId) {
          const resp = await api.get(`/api/v1/centers/lookup?code=${encodeURIComponent(code)}`);
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
    } else {
      // No code provided — use manual institution name, skip lookup
      setStep3Data(data);
      goNext();
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
            city: studentCity || undefined,
            state: studentStateVal || undefined,
            country: studentCountry || undefined,
            pincode: studentPincode || undefined,
            institutionName: centerName || manualInstitutionName || undefined,
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
            address: parentAddress || undefined,
            city: parentCity || undefined,
            state: parentState || undefined,
            district: parentDistrict || undefined,
            pincode: parentPincode || undefined,
            country: parentCountry || undefined,
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
              subjects: teacherSubjectsArr.length > 0 ? teacherSubjectsArr.join(', ') : undefined,
              district: teacherDistrict || undefined,
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

      // INSTITUTION_ADMIN: create their institution in center-svc then redirect
      if (selectedRole === 'INSTITUTION_ADMIN') {
        if (regToken) {
          try {
            await axios.post(
              '/api/v1/centers/self-register',
              {
                name: institutionName.trim(),
                city: institutionCity.trim(),
                phone: institutionPhone.trim(),
                state: instStateVal || undefined,
                address: [instAddressLine1, instAddressLine2].filter(Boolean).join(', ') || undefined,
                branch: instBranch.trim() || undefined,
                board: instBoard.length > 0 ? instBoard.join(',') : undefined,
                pincode: instPincode || undefined,
                country: instCountry.trim() || undefined,
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

                      <form onSubmit={handleSubmit(onStep1Submit)} className="space-y-4" autoComplete="off">

                        {/* Role selector */}
                        <div>
                          <label className="block text-sm font-medium text-white/70 mb-1.5">I am a <span className="text-red-400">*</span></label>
                          <div className="relative">
                            <select
                              value={selectedRole ?? ''}
                              onChange={(e) => setSelectedRole((e.target.value as Role) || null)}
                              className="input w-full appearance-none pr-10"
                            >
                              <option value="">— Select your role —</option>
                              {roleOptions.map(({ role, label }) => (
                                <option key={role} value={role}>{label}</option>
                              ))}
                            </select>
                            <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30 pointer-events-none" />
                          </div>
                        </div>

                        {/* First Name + Last Name — hidden for CENTER_ADMIN (derived from institution name) */}
                        {selectedRole !== 'INSTITUTION_ADMIN' && (
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
                        )}

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
                            <SearchableSelect
                              label="Country"
                              value={studentCountry}
                              onChange={setStudentCountry}
                              options={WORLD_COUNTRIES}
                              placeholder="Select country…"
                              optional
                              allowCustom
                            />
                            <SearchableSelect
                              label="State"
                              value={studentStateVal}
                              onChange={(v) => { setStudentStateVal(v); setStudentCity(''); }}
                              options={suggestStates('')}
                              placeholder="Select state…"
                              optional
                              allowCustom
                            />
                            <AutocompleteInput
                              label="City"
                              value={studentCity}
                              onChange={setStudentCity}
                              options={studentStateVal ? getCitiesForState(studentStateVal) : []}
                              placeholder="Type to search city…"
                              optional
                            />
                            <div>
                              <label className="block text-sm font-medium text-white/70 mb-1.5">Pincode <span className="text-white/30">(optional)</span></label>
                              <input
                                type="text"
                                value={studentPincode}
                                onChange={(e) => setStudentPincode(e.target.value)}
                                placeholder="e.g. 400001"
                                className="input w-full"
                              />
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
                                <input type="tel" value={parentPhone} onChange={(e) => setParentPhone(e.target.value)} placeholder="+91 87654 32100" className="input w-full" />
                              </div>
                              <div>
                                <label className="block text-sm font-medium text-white/70 mb-1.5">Occupation</label>
                                <input type="text" value={parentOccupation} onChange={(e) => setParentOccupation(e.target.value)} placeholder="e.g. Marketing Manager" className="input w-full" />
                              </div>
                            </div>
                            <div>
                              <label className="block text-sm font-medium text-white/70 mb-1.5">Gender</label>
                              <select value={selectedGender} onChange={(e) => setSelectedGender(e.target.value)} className="input w-full">
                                <option value="">— Select (optional) —</option>
                                <option value="MALE">Male</option>
                                <option value="FEMALE">Female</option>
                                <option value="OTHER">Other</option>
                                <option value="PREFER_NOT_TO_SAY">Prefer not to say</option>
                              </select>
                            </div>
                            <div>
                              <label className="block text-sm font-medium text-white/70 mb-1.5">Address <span className="text-white/30">(optional)</span></label>
                              <input type="text" value={parentAddress} onChange={(e) => setParentAddress(e.target.value)} placeholder="e.g. 12 Park Avenue" className="input w-full" />
                            </div>
                            <SearchableSelect label="Country" value={parentCountry} onChange={setParentCountry} options={WORLD_COUNTRIES} placeholder="Select country…" optional allowCustom />
                            <SearchableSelect label="State" value={parentState} onChange={(v) => { setParentState(v); setParentCity(''); }} options={suggestStates('')} placeholder="Select state…" optional allowCustom />
                            <AutocompleteInput label="City" value={parentCity} onChange={setParentCity} options={parentState ? getCitiesForState(parentState) : []} placeholder="Type to search city…" optional />
                            <div className="grid grid-cols-2 gap-3">
                              <div>
                                <label className="block text-sm font-medium text-white/70 mb-1.5">District <span className="text-white/30">(optional)</span></label>
                                <input type="text" value={parentDistrict} onChange={(e) => setParentDistrict(e.target.value)} placeholder="e.g. South Delhi" className="input w-full" />
                              </div>
                              <div>
                                <label className="block text-sm font-medium text-white/70 mb-1.5">Pincode <span className="text-white/30">(optional)</span></label>
                                <input type="text" value={parentPincode} onChange={(e) => setParentPincode(e.target.value)} placeholder="e.g. 110001" className="input w-full" />
                              </div>
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
                            {/* Institution dropdown */}
                            <div>
                              <label className="block text-sm font-medium text-white/70 mb-1.5">
                                Institution <span className="text-white/30">(optional)</span>
                              </label>
                              <div className="relative">
                                <select
                                  value={teacherCenterId ?? ''}
                                  onChange={(e) => {
                                    const selected = teacherCentersList.find(c => c.id === e.target.value);
                                    setTeacherCenterId(selected?.id ?? null);
                                    setTeacherCenterName(selected?.name ?? null);
                                  }}
                                  className="input w-full appearance-none pr-10"
                                  disabled={teacherCentersLoading}
                                >
                                  <option value="">{teacherCentersLoading ? 'Loading institutions…' : '— Select institution —'}</option>
                                  {teacherCentersList.map((c) => (
                                    <option key={c.id} value={c.id}>{c.name}</option>
                                  ))}
                                </select>
                                <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30 pointer-events-none" />
                              </div>
                              {teacherCenterName && (
                                <p className="text-green-400 text-xs mt-1">✓ {teacherCenterName}</p>
                              )}
                            </div>
                            {/* Subjects collapsed dropdown */}
                            <div>
                              <label className="block text-sm font-medium text-white/70 mb-1.5">
                                Subjects <span className="text-white/30">(optional)</span>
                              </label>
                              <div className="relative">
                                <button
                                  type="button"
                                  onClick={() => setTeacherSubjectsOpen((o) => !o)}
                                  className="input w-full text-left flex items-center justify-between"
                                >
                                  <span className={teacherSubjectsArr.length === 0 ? 'text-white/30' : 'text-white/80 text-sm'}>
                                    {teacherSubjectsArr.length === 0
                                      ? 'Select subjects…'
                                      : teacherSubjectsArr.join(', ')}
                                  </span>
                                  <ChevronDown className={cn('w-4 h-4 text-white/30 transition-transform flex-shrink-0', teacherSubjectsOpen && 'rotate-180')} />
                                </button>
                                {teacherSubjectsOpen && (
                                  <div className="absolute z-50 mt-1 w-full bg-surface-100 border border-white/10 rounded-xl shadow-xl max-h-48 overflow-y-auto divide-y divide-white/5">
                                    {TEACHER_SUBJECTS.map((subj) => (
                                      <label key={subj} className="flex items-center gap-3 px-3 py-2.5 hover:bg-white/5 cursor-pointer">
                                        <input
                                          type="checkbox"
                                          checked={teacherSubjectsArr.includes(subj)}
                                          onChange={() => setTeacherSubjectsArr((prev) =>
                                            prev.includes(subj) ? prev.filter((s) => s !== subj) : [...prev, subj]
                                          )}
                                          className="w-4 h-4 accent-brand-500 cursor-pointer"
                                        />
                                        <span className="text-sm text-white/80">{subj}</span>
                                      </label>
                                    ))}
                                  </div>
                                )}
                              </div>
                            </div>
                            <div>
                              <label className="block text-sm font-medium text-white/70 mb-1.5">Address <span className="text-white/30">(optional)</span></label>
                              <input type="text" value={teacherAddress} onChange={(e) => setTeacherAddress(e.target.value)} placeholder="e.g. 45 MG Road, Apartment 3B" className="input w-full" />
                            </div>
                            <SearchableSelect label="Country" value={teacherCountry} onChange={setTeacherCountry} options={WORLD_COUNTRIES} placeholder="Select country…" optional allowCustom />
                            <SearchableSelect label="State" value={teacherStateVal} onChange={(v) => { setTeacherStateVal(v); setTeacherCity(''); }} options={suggestStates('')} placeholder="Select state…" optional allowCustom />
                            <AutocompleteInput label="City" value={teacherCity} onChange={setTeacherCity} options={teacherStateVal ? getCitiesForState(teacherStateVal) : []} placeholder="Type to search city…" optional />
                            <div>
                              <label className="block text-sm font-medium text-white/70 mb-1.5">District <span className="text-white/30">(optional)</span></label>
                              <input type="text" value={teacherDistrict} onChange={(e) => setTeacherDistrict(e.target.value)} placeholder="e.g. Andheri" className="input w-full" />
                            </div>
                          </div>
                        )}

                        {/* Role-specific fields — INSTITUTION */}
                        {selectedRole === 'INSTITUTION_ADMIN' && (
                          <div className="space-y-3 pt-1">
                            <div className="h-px bg-white/5" />
                            <CreatableInput
                              label="Institution Name"
                              value={institutionName}
                              onChange={setInstitutionName}
                              placeholder="e.g. Delhi Public Coaching Centre"
                              required
                            />
                            <div>
                              <label className="block text-sm font-medium text-white/70 mb-1.5">
                                Branch <span className="text-red-400">*</span>
                              </label>
                              <input
                                type="text"
                                value={instBranch}
                                onChange={(e) => setInstBranch(e.target.value)}
                                placeholder="e.g. Andheri West Branch"
                                className="input w-full"
                              />
                            </div>
                            <MultiSelectDropdown label="Board" values={instBoard} onChange={setInstBoard} options={BOARD_OPTIONS} placeholder="Select board(s)…" required />
                            <div>
                              <label className="block text-sm font-medium text-white/70 mb-1.5">Phone <span className="text-red-400">*</span></label>
                              <input type="text" value={institutionPhone} onChange={(e) => setInstitutionPhone(e.target.value)} placeholder="+91 98765 43210" className="input w-full" />
                            </div>
                            <div>
                              <label className="block text-sm font-medium text-white/70 mb-1.5">Address Line 1 <span className="text-white/30">(optional)</span></label>
                              <input type="text" value={instAddressLine1} onChange={(e) => setInstAddressLine1(e.target.value)} placeholder="e.g. 123 Main Road" className="input w-full" />
                            </div>
                            <div>
                              <label className="block text-sm font-medium text-white/70 mb-1.5">Address Line 2 <span className="text-white/30">(optional)</span></label>
                              <input type="text" value={instAddressLine2} onChange={(e) => setInstAddressLine2(e.target.value)} placeholder="e.g. Andheri West" className="input w-full" />
                            </div>
                            <SearchableSelect label="Country" value={instCountry} onChange={setInstCountry} options={WORLD_COUNTRIES} placeholder="Select country…" optional allowCustom />
                            <SearchableSelect label="State" value={instStateVal} onChange={(v) => { setInstStateVal(v); setInstitutionCity(''); }} options={suggestStates('')} placeholder="Select state…" optional allowCustom />
                            <AutocompleteInput label="City" value={institutionCity} onChange={setInstitutionCity} options={instStateVal ? getCitiesForState(instStateVal) : []} placeholder="Type to search city…" optional />
                            <div>
                              <label className="block text-sm font-medium text-white/70 mb-1.5">Pincode <span className="text-white/30">(optional)</span></label>
                              <input type="text" value={instPincode} onChange={(e) => setInstPincode(e.target.value)} placeholder="e.g. 400058" className="input w-full" />
                            </div>
                          </div>
                        )}

                        {/* Email / Password / Confirm — bottom of form */}
                        {selectedRole && (
                          <div className="space-y-4 pt-1">
                            <div className="h-px bg-white/5" />
                            <div>
                              <label className="block text-sm font-medium text-white/70 mb-1.5">Email</label>
                              <input {...register('email')} type="email" placeholder="you@example.com" autoComplete="new-password" className={cn('input w-full', errors.email && 'border-red-500/50')} />
                              {errors.email && <p className="text-red-400 text-xs mt-1">{errors.email.message}</p>}
                            </div>
                            <div>
                              <label className="block text-sm font-medium text-white/70 mb-1.5">Password</label>
                              <div className="relative">
                                <input {...register('password')} type={showPw ? 'text' : 'password'} placeholder="••••••••" autoComplete="new-password" className={cn('input w-full pr-10', errors.password && 'border-red-500/50')} />
                                <button type="button" onClick={() => setShowPw(!showPw)} className="absolute right-3 top-1/2 -translate-y-1/2 text-white/30 hover:text-white/70 transition-colors">
                                  {showPw ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                                </button>
                              </div>
                              {errors.password && <p className="text-red-400 text-xs mt-1">{errors.password.message}</p>}
                              <div className="flex flex-wrap gap-2 mt-2">
                                {[{ key: 'length', label: '≥ 8 chars' }, { key: 'upper', label: '1 uppercase' }, { key: 'digit', label: '1 digit' }, { key: 'special', label: '1 special char' }].map(({ key, label }) => (
                                  <span key={key} className={cn('text-xs px-2 py-0.5 rounded-full border transition-colors', pwChecks[key as keyof typeof pwChecks] ? 'border-green-500/50 bg-green-500/10 text-green-400' : 'border-white/10 text-white/30')}>{label}</span>
                                ))}
                              </div>
                            </div>
                            <div>
                              <label className="block text-sm font-medium text-white/70 mb-1.5">Confirm Password</label>
                              <div className="relative">
                                <input {...register('confirmPassword')} type={showConfirm ? 'text' : 'password'} placeholder="••••••••" autoComplete="new-password" className={cn('input w-full pr-10', errors.confirmPassword && 'border-red-500/50')} />
                                <button type="button" onClick={() => setShowConfirm(!showConfirm)} className="absolute right-3 top-1/2 -translate-y-1/2 text-white/30 hover:text-white/70 transition-colors">
                                  {showConfirm ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                                </button>
                              </div>
                              {errors.confirmPassword && <p className="text-red-400 text-xs mt-1">{errors.confirmPassword.message}</p>}
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
                          disabled={!selectedRole || !captchaToken || isRegistering}
                          className="btn-primary w-full flex items-center justify-center gap-2 py-3 mt-2 disabled:opacity-50"
                        >
                          {isRegistering ? (
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
                    <label className="block text-sm font-medium text-white/70 mb-1.5">Institution Code <span className="text-white/30">(optional)</span></label>
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

                  {!centerName && (
                    <div>
                      <label className="block text-sm font-medium text-white/70 mb-1.5">
                        Institution Name <span className="text-white/30">(if you don't have a code)</span>
                      </label>
                      <input
                        type="text"
                        value={manualInstitutionName}
                        onChange={(e) => setManualInstitutionName(e.target.value)}
                        placeholder="e.g. DPS School, St. Xavier's"
                        className="input w-full"
                      />
                    </div>
                  )}

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
