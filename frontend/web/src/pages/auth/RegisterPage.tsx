import { useState, useCallback, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import axios from 'axios';
import {
  BookOpen,
  ArrowRight,
  ArrowLeft,
  Eye,
  EyeOff,
  ChevronDown,
} from 'lucide-react';
import { toast } from 'sonner';
import CaptchaWidget from '../../components/CaptchaWidget';
import GoogleSignInButton from '../../components/GoogleSignInButton';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { cn, randomUUID } from '../../lib/utils';
import { INDIA_STATES, getCitiesForState, WORLD_COUNTRIES, getDistricts, lookupPincode } from '../../utils/indiaLocations';

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID ?? '';

// ─── Creatable Input ──────────────────────────────────────────────────────────

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

// ─── Searchable Select ────────────────────────────────────────────────────────

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
    <div
      className="relative"
      onBlur={(e) => {
        if (!e.currentTarget.contains(e.relatedTarget as Node)) { setOpen(false); setSearch(''); }
      }}
    >
      <label className="block text-sm font-medium text-white/70 mb-1.5">
        {label}{required && <span className="text-red-400"> *</span>}{optional && <span className="text-white/30"> (optional)</span>}
      </label>
      <button type="button" onClick={() => { setOpen(o => !o); setSearch(''); }}
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

// ─── Autocomplete Input ────────────────────────────────────────────────────────

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

// ─── Multi-Select Dropdown ─────────────────────────────────────────────────────

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
    <div className="relative" onBlur={(e) => { if (!e.currentTarget.contains(e.relatedTarget as Node)) setOpen(false); }}>
      <label className="block text-sm font-medium text-white/70 mb-1.5">
        {label}{required && <span className="text-red-400"> *</span>}
      </label>
      <button type="button" onClick={() => setOpen(o => !o)}
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

// ─── Constants ────────────────────────────────────────────────────────────────

const BOARD_OPTIONS = [
  { value: 'CBSE',        label: 'CBSE' },
  { value: 'ICSE',        label: 'ICSE' },
  { value: 'STATE_BOARD', label: 'State Board' },
  { value: 'IB',          label: 'IB' },
  { value: 'IGCSE',       label: 'IGCSE / Cambridge' },
];

const TEACHER_SUBJECTS = [
  'Mathematics', 'Physics', 'Chemistry', 'Biology', 'English', 'Hindi',
  'History', 'Geography', 'Economics', 'Political Science', 'Computer Science',
  'Accountancy', 'Business Studies', 'Sanskrit', 'Science', 'Social Studies',
  'Physical Education', 'Art', 'Music',
];

// ─── Schema ───────────────────────────────────────────────────────────────────

const registerSchema = z
  .object({
    firstName: z.string().min(2, 'First name is required').max(100),
    lastName: z.string().min(2, 'Last name is required').max(100),
    email: z.string().email('Invalid email address'),
    password: z
      .string()
      .min(8, 'At least 8 characters')
      .regex(/[A-Z]/, 'At least 1 uppercase letter')
      .regex(/[0-9]/, 'At least 1 digit')
      .regex(/[^A-Za-z0-9]/, 'At least 1 special character'),
    confirmPassword: z.string(),
    phone: z.string().max(20).optional().or(z.literal('')),
    dateOfBirth: z.string().optional().or(z.literal('')),
  })
  .refine((d) => d.password === d.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

type RegisterData = z.infer<typeof registerSchema>;

function calculateAge(dob: string): number {
  const today = new Date();
  const birth = new Date(dob);
  let age = today.getFullYear() - birth.getFullYear();
  const m = today.getMonth() - birth.getMonth();
  if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--;
  return age;
}

type Role = 'STUDENT' | 'PARENT' | 'TEACHER' | 'INSTITUTION_ADMIN';

const roleOptions: { role: Role; label: string }[] = [
  { role: 'STUDENT',           label: 'Student' },
  { role: 'PARENT',            label: 'Parent / Guardian' },
  { role: 'TEACHER',           label: 'Teacher' },
  { role: 'INSTITUTION_ADMIN', label: 'Institution / Coaching Centre' },
];

// ─── RegisterPage ─────────────────────────────────────────────────────────────

export default function RegisterPage() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);

  const [selectedRole, setSelectedRole] = useState<Role | null>(null);
  const [showPw, setShowPw] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [isRegistering, setIsRegistering] = useState(false);
  const [isGoogleLoading, setIsGoogleLoading] = useState(false);
  const [captchaToken, setCaptchaToken] = useState<string | null>(null);
  const [isUnder13, setIsUnder13] = useState(false);

  // Institution code lookup (student)
  const [studentInstitutionCode, setStudentInstitutionCode] = useState('');
  const [centerId, setCenterId] = useState<string | null>(null);
  const [centerName, setCenterName] = useState<string | null>(null);

  // Student academic + optional parent-guardian email for under-13
  const [studentBoard, setStudentBoard] = useState('');
  const [studentGrade, setStudentGrade] = useState('10');
  const [parentGuardianEmail, setParentGuardianEmail] = useState('');
  const [studentCentersList, setStudentCentersList] = useState<{ id: string; name: string }[]>([]);
  const [selectedStudentInstitutionName, setSelectedStudentInstitutionName] = useState('');

  // Student location
  const [studentAddressLine1, setStudentAddressLine1] = useState('');
  const [studentAddressLine2, setStudentAddressLine2] = useState('');
  const [studentCountry, setStudentCountry] = useState('');
  const [studentStateVal, setStudentStateVal] = useState('');
  const [studentDistrict, setStudentDistrict] = useState('');
  const [studentCity, setStudentCity] = useState('');
  const [studentPincode, setStudentPincode] = useState('');
  const [manualInstitutionName, setManualInstitutionName] = useState('');

  // Parent-specific
  const [parentPhone, setParentPhone] = useState('');
  const [parentOccupation, setParentOccupation] = useState('');
  const [selectedGender, setSelectedGender] = useState('');
  const [parentAddress, setParentAddress] = useState('');
  const [parentAddressLine2, setParentAddressLine2] = useState('');
  const [parentCountry, setParentCountry] = useState('');
  const [parentState, setParentState] = useState('');
  const [parentDistrict, setParentDistrict] = useState('');
  const [parentCity, setParentCity] = useState('');
  const [parentPincode, setParentPincode] = useState('');

  // Teacher-specific
  const [teacherCenterId, setTeacherCenterId] = useState<string | null>(null);
  const [teacherCenterName, setTeacherCenterName] = useState<string | null>(null);
  const [teacherCentersList, setTeacherCentersList] = useState<{ id: string; name: string }[]>([]);
  const [teacherCentersLoading, setTeacherCentersLoading] = useState(false);
  const [teacherSubjectsArr, setTeacherSubjectsArr] = useState<string[]>([]);
  const [teacherSubjectsOpen, setTeacherSubjectsOpen] = useState(false);
  const [teacherAddress, setTeacherAddress] = useState('');
  const [teacherAddressLine2, setTeacherAddressLine2] = useState('');
  const [teacherCountry, setTeacherCountry] = useState('');
  const [teacherStateVal, setTeacherStateVal] = useState('');
  const [teacherDistrict, setTeacherDistrict] = useState('');
  const [teacherCity, setTeacherCity] = useState('');
  const [teacherPincode, setTeacherPincode] = useState('');

  // Institution-specific
  const [institutionName, setInstitutionName] = useState('');
  const [institutionCity, setInstitutionCity] = useState('');
  const [institutionPhone, setInstitutionPhone] = useState('');
  const [instBranch, setInstBranch] = useState('');
  const [instBoard, setInstBoard] = useState<string[]>([]);
  const [instCenterType, setInstCenterType] = useState<'COACHING_CENTER' | 'SCHOOL' | 'COLLEGE'>('COACHING_CENTER');
  const [instAddressLine1, setInstAddressLine1] = useState('');
  const [instAddressLine2, setInstAddressLine2] = useState('');
  const [instCountry, setInstCountry] = useState('');
  const [instStateVal, setInstStateVal] = useState('');
  const [instDistrict, setInstDistrict] = useState('');
  const [instPincode, setInstPincode] = useState('');

  const handleCaptchaVerify = useCallback((token: string | null) => setCaptchaToken(token), []);

  async function handlePincodeChange(
    value: string,
    setPincode: (v: string) => void,
    setState: (v: string) => void,
    setDistrict: (v: string) => void,
    setCity: (v: string) => void,
  ) {
    setPincode(value);
    if (value.length === 6 && /^\d{6}$/.test(value)) {
      const result = await lookupPincode(value);
      if (result) {
        if (result.state) setState(result.state);
        if (result.district) setDistrict(result.district);
        if (result.city) setCity(result.city);
        toast.success(`Auto-filled: ${result.city}, ${result.district}, ${result.state}`);
      }
    }
  }

  async function handleGoogleSuccess(accessToken: string) {
    setIsGoogleLoading(true);
    try {
      const deviceId = randomUUID();
      const res = await api.post('/api/v1/auth/google', { idToken: accessToken }, {
        headers: { 'X-Device-Id': deviceId },
      });
      const { accessToken: jwt, refreshToken } = res.data;
      const meRes = await api.get('/api/v1/auth/me', { headers: { Authorization: `Bearer ${jwt}` } });
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
    setValue,
    formState: { errors },
  } = useForm<RegisterData>({ resolver: zodResolver(registerSchema) });

  const watchedPassword = watch('password', '');

  // Fetch centers list for Teacher role AND Student institution selection
  useEffect(() => {
    if (selectedRole !== 'TEACHER' && selectedRole !== 'STUDENT') return;
    api.get('/api/v1/centers?size=200')
      .then((r) => {
        const data = r.data;
        const list = Array.isArray(data) ? data : (data.content ?? []);
        const mapped = list.map((c: { id: string; name: string }) => ({ id: c.id, name: c.name }));
        if (selectedRole === 'TEACHER') { setTeacherCentersLoading(false); setTeacherCentersList(mapped); }
        if (selectedRole === 'STUDENT') setStudentCentersList(mapped);
      })
      .catch(() => {
        setTeacherCentersList([]);
        setStudentCentersList([]);
      });
    if (selectedRole === 'TEACHER') setTeacherCentersLoading(true);
  }, [selectedRole]);

  // Auto-populate firstName/lastName from institutionName for INSTITUTION_ADMIN
  useEffect(() => {
    if (selectedRole !== 'INSTITUTION_ADMIN') return;
    const words = institutionName.trim().split(/\s+/);
    setValue('firstName', words[0] || institutionName.trim() || 'Institution', { shouldValidate: false });
    setValue('lastName', words.slice(1).join(' ') || words[0] || 'Admin', { shouldValidate: false });
  }, [selectedRole, institutionName, setValue]);

  // Live debounce lookup for student institution code
  useEffect(() => {
    const code = studentInstitutionCode.trim();
    if (!code || code.length < 3) { setCenterName(null); setCenterId(null); return; }
    const timer = setTimeout(async () => {
      try {
        const resp = await api.get(`/api/v1/centers/lookup?code=${encodeURIComponent(code)}`);
        setCenterId(resp.data.id);
        setCenterName(resp.data.name);
        setSelectedStudentInstitutionName(resp.data.name);
      } catch {
        setCenterName(null);
        setCenterId(null);
      }
    }, 600);
    return () => clearTimeout(timer);
  }, [studentInstitutionCode]);

  const pwChecks = {
    length: watchedPassword.length >= 8,
    upper:  /[A-Z]/.test(watchedPassword),
    digit:  /[0-9]/.test(watchedPassword),
    special: /[^A-Za-z0-9]/.test(watchedPassword),
  };

  async function onSubmit(data: RegisterData) {
    if (!selectedRole) { toast.error('Please select your role to continue'); return; }

    if (selectedRole === 'STUDENT') {
      if (!data.dateOfBirth) { toast.error('Date of birth is required'); return; }
      if (!data.phone?.trim()) { toast.error('Phone number is required'); return; }
      if (!studentAddressLine1.trim()) { toast.error('Address is required'); return; }
      if (!studentCountry.trim()) { toast.error('Country is required'); return; }
      if (!studentStateVal.trim()) { toast.error('State is required'); return; }
      if (!studentDistrict.trim()) { toast.error('District is required'); return; }
      if (!studentCity.trim()) { toast.error('City is required'); return; }
      if (!studentPincode.trim() || !/^\d{6}$/.test(studentPincode.trim())) { toast.error('Valid 6-digit pincode is required'); return; }
      const under13 = calculateAge(data.dateOfBirth) < 13;
      setIsUnder13(under13);
      if (under13 && !parentGuardianEmail.trim()) { toast.error('Parent/guardian email is required for users under 13'); return; }
    }

    if (selectedRole === 'PARENT') {
      if (!parentPhone.trim()) { toast.error('Phone number is required'); return; }
      if (!selectedGender) { toast.error('Gender is required'); return; }
      if (!parentAddress.trim()) { toast.error('Address is required'); return; }
      if (!parentCountry.trim()) { toast.error('Country is required'); return; }
      if (!parentState.trim()) { toast.error('State is required'); return; }
      if (!parentDistrict.trim()) { toast.error('District is required'); return; }
      if (!parentCity.trim()) { toast.error('City is required'); return; }
      if (!parentPincode.trim() || !/^\d{6}$/.test(parentPincode.trim())) { toast.error('Valid 6-digit pincode is required'); return; }
    }

    if (selectedRole === 'TEACHER') {
      if (!selectedGender) { toast.error('Gender is required'); return; }
      if (teacherSubjectsArr.length === 0) { toast.error('Select at least one subject'); return; }
      if (!teacherAddress.trim()) { toast.error('Address is required'); return; }
      if (!teacherCountry.trim()) { toast.error('Country is required'); return; }
      if (!teacherStateVal.trim()) { toast.error('State is required'); return; }
      if (!teacherDistrict.trim()) { toast.error('District is required'); return; }
      if (!teacherCity.trim()) { toast.error('City is required'); return; }
      if (!teacherPincode.trim() || !/^\d{6}$/.test(teacherPincode.trim())) { toast.error('Valid 6-digit pincode is required'); return; }
    }

    if (selectedRole === 'INSTITUTION_ADMIN') {
      if (!institutionName.trim()) { toast.error('Institution name is required'); return; }
      if (!institutionPhone.trim()) { toast.error('Institution phone is required'); return; }
      if (!instAddressLine1.trim()) { toast.error('Address is required'); return; }
      if (!instCountry.trim()) { toast.error('Country is required'); return; }
      if (!instStateVal.trim()) { toast.error('State is required'); return; }
      if (!instDistrict.trim()) { toast.error('District is required'); return; }
      if (!institutionCity.trim()) { toast.error('City is required'); return; }
      if (!instPincode.trim() || !/^\d{6}$/.test(instPincode.trim())) { toast.error('Valid 6-digit pincode is required'); return; }
    }

    setIsRegistering(true);
    try {
      const deviceId = randomUUID();
      const instWords = selectedRole === 'INSTITUTION_ADMIN' ? institutionName.trim().split(/\s+/) : [];
      const response = await api.post('/api/v1/auth/register', {
        firstName: selectedRole === 'INSTITUTION_ADMIN' ? (instWords[0] || institutionName.trim()) : data.firstName,
        lastName:  selectedRole === 'INSTITUTION_ADMIN' ? (instWords.slice(1).join(' ') || instWords[0] || '-') : data.lastName,
        email:    data.email,
        password: data.password,
        role:     selectedRole,
        centerId: selectedRole === 'TEACHER' ? (teacherCenterId ?? undefined) : undefined,
        captchaToken: captchaToken!,
        deviceFingerprint: { userAgent: navigator.userAgent, deviceId, ipSubnet: '127.0.0' },
      });
      const token    = response.data.accessToken;
      const refreshTok = response.data.refreshToken ?? null;

      // ── PARENT ──
      if (selectedRole === 'PARENT') {
        try {
          const meRes = await axios.get('/api/v1/auth/me', { headers: { Authorization: `Bearer ${token}` } });
          const u = meRes.data;
          const name = [u.firstName, u.lastName].filter(Boolean).join(' ') || u.email;
          setAuth(token, { id: u.id, email: u.email, role: u.role, name }, refreshTok ?? '', deviceId);
          await axios.post('/api/v1/parents', {
            name,
            phone:      parentPhone || undefined,
            email:      data.email,
            occupation: parentOccupation || undefined,
            gender:     selectedGender || undefined,
            address:    [parentAddress, parentAddressLine2].filter(Boolean).join(', ') || undefined,
            city:       parentCity || undefined,
            state:      parentState || undefined,
            district:   parentDistrict || undefined,
            pincode:    parentPincode || undefined,
            country:    parentCountry || undefined,
          }, { headers: { Authorization: `Bearer ${token}` } });
          toast.success('Account created! Welcome to NexusEd.');
        } catch (e) {
          console.error('Parent profile setup failed:', e);
          toast.success('Account created! Welcome.');
        }
        navigate('/parent');
        return;
      }

      // ── TEACHER ──
      if (selectedRole === 'TEACHER') {
        try {
          const meRes = await axios.get('/api/v1/auth/me', { headers: { Authorization: `Bearer ${token}` } });
          const userId = meRes.data?.id;
          if (userId) {
            await axios.post('/api/v1/mentors', {
              userId,
              fullName: `${data.firstName} ${data.lastName}`.trim(),
              email: data.email,
              yearsOfExperience: 0,
              hourlyRate: 0.01,
            }, { headers: { Authorization: `Bearer ${token}` } });
          }
        } catch { /* non-fatal */ }

        if (teacherCenterId) {
          try {
            await axios.post(
              `/api/v1/centers/${teacherCenterId}/teachers/self-register`,
              {
                firstName:   data.firstName,
                lastName:    data.lastName,
                email:       data.email,
                phoneNumber: data.phone || undefined,
                subjects:    teacherSubjectsArr.length > 0 ? teacherSubjectsArr.join(', ') : undefined,
                address:     teacherAddress ? [teacherAddress, teacherAddressLine2].filter(Boolean).join(', ') : undefined,
                city:        teacherCity || undefined,
                state:       teacherStateVal || undefined,
                district:    teacherDistrict || undefined,
                country:     teacherCountry || undefined,
                pincode:     teacherPincode || undefined,
              },
              { headers: { Authorization: `Bearer ${token}` } }
            );
            toast.success('Registration submitted! Awaiting approval from your institution coordinator.');
          } catch { /* non-fatal */ }
        } else {
          toast.success('Account created! You can now sign in.');
        }
        navigate('/login');
        return;
      }

      // ── STUDENT ──
      if (selectedRole === 'STUDENT') {
        try {
          const payload = JSON.parse(atob(token.split('.')[1]));
          const userId  = payload.sub as string;
          const under13 = data.dateOfBirth ? calculateAge(data.dateOfBirth) < 13 : false;
          await axios.post('/api/v1/students', {
            userId,
            firstName:   data.firstName,
            lastName:    data.lastName,
            email:       data.email,
            phone:       data.phone || undefined,
            gender:      selectedGender || undefined,
            dateOfBirth: data.dateOfBirth!,
            address:     [studentAddressLine1, studentAddressLine2].filter(Boolean).join(', ') || undefined,
            city:        studentCity || undefined,
            state:       studentStateVal || undefined,
            district:    studentDistrict || undefined,
            country:     studentCountry || undefined,
            pincode:     studentPincode || undefined,
            institutionName: centerName || manualInstitutionName || undefined,
            board:           studentBoard || undefined,
            currentClass:    parseInt(studentGrade) || undefined,
            subjects:        [],
            parentGuardianEmail: under13 ? parentGuardianEmail || undefined : undefined,
          }, { headers: { Authorization: `Bearer ${token}` } });
        } catch (e) {
          console.error('Student profile creation failed (non-fatal):', e);
        }
        toast.success('Account created! You can now sign in.');
        navigate('/login');
        return;
      }

      // ── INSTITUTION_ADMIN ──
      if (selectedRole === 'INSTITUTION_ADMIN') {
        try {
          await axios.post('/api/v1/centers/self-register', {
            name:       institutionName.trim(),
            city:       institutionCity.trim(),
            phone:      institutionPhone.trim(),
            state:      instStateVal || undefined,
            address:    [instAddressLine1, instAddressLine2].filter(Boolean).join(', ') || undefined,
            branch:     instBranch.trim() || undefined,
            board:      instBoard.length > 0 ? instBoard.join(',') : undefined,
            pincode:    instPincode || undefined,
            country:    instCountry.trim() || undefined,
            centerType: instCenterType,
          }, { headers: { Authorization: `Bearer ${token}` } });
          toast.success('Institution registered! You can now sign in.');
        } catch {
          toast.success('Account created! Institution details can be set up after sign-in.');
        }
        navigate('/login');
        return;
      }

      toast.success('Account created! You can now sign in.');
      navigate('/login');
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status?: number; data?: { detail?: string } } };
      if (axiosErr.response?.status === 409) {
        toast.error('An account with this email already exists. Please sign in instead.');
        navigate('/login');
      } else {
        toast.error(axiosErr.response?.data?.detail ?? 'Registration failed');
        setCaptchaToken(null);
      }
    } finally {
      setIsRegistering(false);
    }
  }

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

        <div className="glass rounded-2xl p-4">
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

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" autoComplete="off">

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

            {/* First + Last Name (all roles except INSTITUTION_ADMIN) */}
            {selectedRole && selectedRole !== 'INSTITUTION_ADMIN' && (
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-1.5">First Name <span className="text-red-400">*</span></label>
                  <input {...register('firstName')} type="text" placeholder="Jane"
                    className={cn('input w-full', errors.firstName && 'border-red-500/50')} />
                  {errors.firstName && <p className="text-red-400 text-xs mt-1">{errors.firstName.message}</p>}
                </div>
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-1.5">Last Name <span className="text-red-400">*</span></label>
                  <input {...register('lastName')} type="text" placeholder="Smith"
                    className={cn('input w-full', errors.lastName && 'border-red-500/50')} />
                  {errors.lastName && <p className="text-red-400 text-xs mt-1">{errors.lastName.message}</p>}
                </div>
              </div>
            )}

            {/* ── STUDENT fields ── */}
            {selectedRole === 'STUDENT' && (
              <div className="space-y-3 pt-1">
                <div className="h-px bg-white/5" />
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-sm font-medium text-white/70 mb-1.5">Phone <span className="text-red-400">*</span></label>
                    <input {...register('phone')} type="tel" placeholder="+91 98765 43210"
                      className={cn('input w-full', errors.phone && 'border-red-500/50')} />
                    {errors.phone && <p className="text-red-400 text-xs mt-1">{errors.phone.message}</p>}
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-white/70 mb-1.5">Date of Birth <span className="text-red-400">*</span></label>
                    <input
                      {...register('dateOfBirth')}
                      type="date"
                      onChange={(e) => {
                        register('dateOfBirth').onChange(e);
                        if (e.target.value) setIsUnder13(calculateAge(e.target.value) < 13);
                      }}
                      className={cn('input w-full', errors.dateOfBirth && 'border-red-500/50')}
                    />
                    {errors.dateOfBirth && <p className="text-red-400 text-xs mt-1">{errors.dateOfBirth.message}</p>}
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-1.5">Address Line 1 <span className="text-red-400">*</span></label>
                  <input type="text" value={studentAddressLine1} onChange={(e) => setStudentAddressLine1(e.target.value)} placeholder="e.g. 12 Park Avenue" className="input w-full" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-1.5">Address Line 2 <span className="text-white/30">(optional)</span></label>
                  <input type="text" value={studentAddressLine2} onChange={(e) => setStudentAddressLine2(e.target.value)} placeholder="e.g. Andheri West" className="input w-full" />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <SearchableSelect label="Country" value={studentCountry} onChange={setStudentCountry} options={WORLD_COUNTRIES} placeholder="Select country…" required allowCustom />
                  <SearchableSelect label="State" value={studentStateVal} onChange={(v) => { setStudentStateVal(v); setStudentCity(''); setStudentDistrict(''); }} options={INDIA_STATES} placeholder="Select state…" required allowCustom />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <SearchableSelect label="District" value={studentDistrict} onChange={setStudentDistrict} options={studentStateVal ? getDistricts(studentStateVal) : []} placeholder="Select district…" required allowCustom />
                  <AutocompleteInput label="City" value={studentCity} onChange={setStudentCity} options={studentStateVal ? getCitiesForState(studentStateVal) : []} placeholder="Type to search city…" required />
                </div>
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-1.5">Pincode <span className="text-red-400">*</span></label>
                  <input type="text" value={studentPincode} onChange={(e) => handlePincodeChange(e.target.value, setStudentPincode, setStudentStateVal, setStudentDistrict, setStudentCity)} placeholder="e.g. 400001" className="input w-full" maxLength={6} />
                </div>

                {/* Academic Details — inline */}
                <div className="h-px bg-white/5" />
                <div className="rounded-xl border border-brand-500/20 bg-brand-500/5 p-3">
                  <p className="text-xs text-brand-300 font-medium mb-0.5">Already enrolled in a coaching center or school?</p>
                  <p className="text-xs text-white/40">Enter the institution code to link your account (optional).</p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-1.5">Institution Code <span className="text-white/30">(optional)</span></label>
                  <input type="text" value={studentInstitutionCode} onChange={(e) => setStudentInstitutionCode(e.target.value)} placeholder="e.g. SCH-2024-ABC" className="input w-full" />
                  {centerName && <p className="text-green-400 text-xs mt-1">✓ Linked to: {centerName}</p>}
                  {!centerName && studentInstitutionCode.length >= 3 && (
                    <p className="text-yellow-400/70 text-xs mt-1">Code not found — you can still register without linking.</p>
                  )}
                </div>
                {!centerName && (
                  <SearchableSelect
                    label="Select Institution"
                    value={selectedStudentInstitutionName}
                    onChange={(v) => {
                      setSelectedStudentInstitutionName(v);
                      const found = studentCentersList.find(c => c.name === v);
                      if (found) {
                        setCenterId(found.id);
                        setCenterName(found.name);
                        setManualInstitutionName('');
                      } else {
                        setCenterId(null);
                        setCenterName(null);
                        setManualInstitutionName(v);
                      }
                    }}
                    options={studentCentersList.map(c => c.name)}
                    placeholder="Search or type institution name…"
                    allowCustom
                  />
                )}
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-sm font-medium text-white/70 mb-1.5">Board</label>
                    <select value={studentBoard} onChange={(e) => setStudentBoard(e.target.value)} className="input w-full">
                      <option value="">Select board</option>
                      <option value="CBSE">CBSE</option>
                      <option value="ICSE">ICSE</option>
                      <option value="STATE_BOARD">State Board</option>
                      <option value="IB">IB</option>
                      <option value="IGCSE">IGCSE</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-white/70 mb-1.5">Grade</label>
                    <select value={studentGrade} onChange={(e) => setStudentGrade(e.target.value)} className="input w-full">
                      <option value="10">Grade 10</option>
                      <option value="11">Grade 11</option>
                      <option value="12">Grade 12</option>
                    </select>
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
                {isUnder13 && (
                  <div>
                    <label className="block text-sm font-medium text-white/70 mb-1.5">Parent / Guardian Email <span className="text-red-400">*</span></label>
                    <input type="email" value={parentGuardianEmail} onChange={(e) => setParentGuardianEmail(e.target.value)} placeholder="parent@example.com" className="input w-full" />
                    <p className="text-amber-400/70 text-xs mt-1">Required — you appear to be under 13.</p>
                  </div>
                )}
              </div>
            )}

            {/* ── PARENT fields ── */}
            {selectedRole === 'PARENT' && (
              <div className="space-y-3 pt-1">
                <div className="h-px bg-white/5" />
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-sm font-medium text-white/70 mb-1.5">Phone Number <span className="text-red-400">*</span></label>
                    <input type="tel" value={parentPhone} onChange={(e) => setParentPhone(e.target.value)} placeholder="+91 87654 32100" className="input w-full" />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-white/70 mb-1.5">Occupation</label>
                    <input type="text" value={parentOccupation} onChange={(e) => setParentOccupation(e.target.value)} placeholder="e.g. Marketing Manager" className="input w-full" />
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-1.5">Gender <span className="text-red-400">*</span></label>
                  <select value={selectedGender} onChange={(e) => setSelectedGender(e.target.value)} className="input w-full">
                    <option value="">— Select —</option>
                    <option value="MALE">Male</option>
                    <option value="FEMALE">Female</option>
                    <option value="OTHER">Other</option>
                    <option value="PREFER_NOT_TO_SAY">Prefer not to say</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-1.5">Address Line 1 <span className="text-red-400">*</span></label>
                  <input type="text" value={parentAddress} onChange={(e) => setParentAddress(e.target.value)} placeholder="e.g. 12 Park Avenue" className="input w-full" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-1.5">Address Line 2 <span className="text-white/30">(optional)</span></label>
                  <input type="text" value={parentAddressLine2} onChange={(e) => setParentAddressLine2(e.target.value)} placeholder="e.g. Andheri West" className="input w-full" />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <SearchableSelect label="Country" value={parentCountry} onChange={setParentCountry} options={WORLD_COUNTRIES} placeholder="Select country…" required allowCustom />
                  <SearchableSelect label="State" value={parentState} onChange={(v) => { setParentState(v); setParentCity(''); setParentDistrict(''); }} options={INDIA_STATES} placeholder="Select state…" required allowCustom />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <SearchableSelect label="District" value={parentDistrict} onChange={setParentDistrict} options={parentState ? getDistricts(parentState) : []} placeholder="Select district…" required allowCustom />
                  <AutocompleteInput label="City" value={parentCity} onChange={setParentCity} options={parentState ? getCitiesForState(parentState) : []} placeholder="Type to search city…" required />
                </div>
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-1.5">Pincode <span className="text-red-400">*</span></label>
                  <input type="text" value={parentPincode} onChange={(e) => handlePincodeChange(e.target.value, setParentPincode, setParentState, setParentDistrict, setParentCity)} placeholder="e.g. 110001" className="input w-full" maxLength={6} />
                </div>
              </div>
            )}

            {/* ── TEACHER fields ── */}
            {selectedRole === 'TEACHER' && (
              <div className="space-y-3 pt-1">
                <div className="h-px bg-white/5" />
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-1.5">Gender <span className="text-red-400">*</span></label>
                  <select value={selectedGender} onChange={(e) => setSelectedGender(e.target.value)} className="input w-full">
                    <option value="">— Select —</option>
                    <option value="MALE">Male</option>
                    <option value="FEMALE">Female</option>
                    <option value="OTHER">Other</option>
                    <option value="PREFER_NOT_TO_SAY">Prefer not to say</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-1.5">
                    Apply to Institution <span className="text-white/30">(select to send join request)</span>
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
                      <option value="">{teacherCentersLoading ? 'Loading institutions…' : '— Select institution to join —'}</option>
                      {teacherCentersList.map((c) => (
                        <option key={c.id} value={c.id}>{c.name}</option>
                      ))}
                    </select>
                    <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30 pointer-events-none" />
                  </div>
                  {teacherCenterName && <p className="text-green-400 text-xs mt-1">✓ Application will be sent to: {teacherCenterName}</p>}
                  {!teacherCenterName && !teacherCentersLoading && <p className="text-white/30 text-xs mt-1">Your application will be reviewed by the institution coordinator.</p>}
                </div>
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-1.5">Subjects <span className="text-red-400">*</span></label>
                  <div className="relative">
                    <button type="button" onClick={() => setTeacherSubjectsOpen((o) => !o)}
                      className="input w-full text-left flex items-center justify-between">
                      <span className={teacherSubjectsArr.length === 0 ? 'text-white/30' : 'text-white/80 text-sm'}>
                        {teacherSubjectsArr.length === 0 ? 'Select subjects…' : teacherSubjectsArr.join(', ')}
                      </span>
                      <ChevronDown className={cn('w-4 h-4 text-white/30 transition-transform flex-shrink-0', teacherSubjectsOpen && 'rotate-180')} />
                    </button>
                    {teacherSubjectsOpen && (
                      <div className="absolute z-50 mt-1 w-full bg-surface-100 border border-white/10 rounded-xl shadow-xl max-h-48 overflow-y-auto divide-y divide-white/5">
                        {TEACHER_SUBJECTS.map((subj) => (
                          <label key={subj} className="flex items-center gap-3 px-3 py-2.5 hover:bg-white/5 cursor-pointer">
                            <input type="checkbox" checked={teacherSubjectsArr.includes(subj)}
                              onChange={() => setTeacherSubjectsArr((prev) =>
                                prev.includes(subj) ? prev.filter((s) => s !== subj) : [...prev, subj]
                              )}
                              className="w-4 h-4 accent-brand-500 cursor-pointer" />
                            <span className="text-sm text-white/80">{subj}</span>
                          </label>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-1.5">Address Line 1 <span className="text-red-400">*</span></label>
                  <input type="text" value={teacherAddress} onChange={(e) => setTeacherAddress(e.target.value)} placeholder="e.g. 45 MG Road, Apartment 3B" className="input w-full" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-1.5">Address Line 2 <span className="text-white/30">(optional)</span></label>
                  <input type="text" value={teacherAddressLine2} onChange={(e) => setTeacherAddressLine2(e.target.value)} placeholder="e.g. Andheri West" className="input w-full" />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <SearchableSelect label="Country" value={teacherCountry} onChange={setTeacherCountry} options={WORLD_COUNTRIES} placeholder="Select country…" required allowCustom />
                  <SearchableSelect label="State" value={teacherStateVal} onChange={(v) => { setTeacherStateVal(v); setTeacherCity(''); setTeacherDistrict(''); }} options={INDIA_STATES} placeholder="Select state…" required allowCustom />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <SearchableSelect label="District" value={teacherDistrict} onChange={setTeacherDistrict} options={teacherStateVal ? getDistricts(teacherStateVal) : []} placeholder="Select district…" required allowCustom />
                  <AutocompleteInput label="City" value={teacherCity} onChange={setTeacherCity} options={teacherStateVal ? getCitiesForState(teacherStateVal) : []} placeholder="Type to search city…" required />
                </div>
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-1.5">Pincode <span className="text-red-400">*</span></label>
                  <input type="text" value={teacherPincode} onChange={(e) => handlePincodeChange(e.target.value, setTeacherPincode, setTeacherStateVal, setTeacherDistrict, setTeacherCity)} placeholder="e.g. 400058" className="input w-full" maxLength={6} />
                </div>
              </div>
            )}

            {/* ── INSTITUTION_ADMIN fields ── */}
            {selectedRole === 'INSTITUTION_ADMIN' && (
              <div className="space-y-3 pt-1">
                <div className="h-px bg-white/5" />
                <CreatableInput label="Institution Name" value={institutionName} onChange={setInstitutionName} placeholder="e.g. Delhi Public Coaching Centre" required />
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-1.5">Branch <span className="text-red-400">*</span></label>
                  <input type="text" value={instBranch} onChange={(e) => setInstBranch(e.target.value)} placeholder="e.g. Andheri West Branch" className="input w-full" />
                </div>
                <MultiSelectDropdown label="Board" values={instBoard} onChange={setInstBoard} options={BOARD_OPTIONS} placeholder="Select board(s)…" required />
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-1">Institution Type</label>
                  <div className="grid grid-cols-3 gap-2">
                    {([
                      { value: 'COACHING_CENTER', label: 'Coaching Center' },
                      { value: 'SCHOOL',          label: 'School' },
                      { value: 'COLLEGE',         label: 'College' },
                    ] as const).map(opt => (
                      <button key={opt.value} type="button"
                        onClick={() => setInstCenterType(opt.value)}
                        className={`px-3 py-2 rounded-lg border text-sm font-medium transition-all ${
                          instCenterType === opt.value
                            ? 'border-violet-500 bg-violet-500/20 text-violet-300'
                            : 'border-white/10 bg-white/5 text-white/60 hover:bg-white/10'
                        }`}>
                        {opt.label}
                      </button>
                    ))}
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-1.5">Phone <span className="text-red-400">*</span></label>
                  <input type="text" value={institutionPhone} onChange={(e) => setInstitutionPhone(e.target.value)} placeholder="+91 98765 43210" className="input w-full" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-1.5">Address Line 1 <span className="text-red-400">*</span></label>
                  <input type="text" value={instAddressLine1} onChange={(e) => setInstAddressLine1(e.target.value)} placeholder="e.g. 123 Main Road" className="input w-full" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-1.5">Address Line 2 <span className="text-white/30">(optional)</span></label>
                  <input type="text" value={instAddressLine2} onChange={(e) => setInstAddressLine2(e.target.value)} placeholder="e.g. Andheri West" className="input w-full" />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <SearchableSelect label="Country" value={instCountry} onChange={setInstCountry} options={WORLD_COUNTRIES} placeholder="Select country…" required allowCustom />
                  <SearchableSelect label="State" value={instStateVal} onChange={(v) => { setInstStateVal(v); setInstitutionCity(''); setInstDistrict(''); }} options={INDIA_STATES} placeholder="Select state…" required allowCustom />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <SearchableSelect label="District" value={instDistrict} onChange={setInstDistrict} options={instStateVal ? getDistricts(instStateVal) : []} placeholder="Select district…" required allowCustom />
                  <AutocompleteInput label="City" value={institutionCity} onChange={setInstitutionCity} options={instStateVal ? getCitiesForState(instStateVal) : []} placeholder="Type to search city…" required />
                </div>
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-1.5">Pincode <span className="text-red-400">*</span></label>
                  <input type="text" value={instPincode} onChange={(e) => handlePincodeChange(e.target.value, setInstPincode, setInstStateVal, setInstDistrict, setInstitutionCity)} placeholder="e.g. 400058" className="input w-full" maxLength={6} />
                </div>
              </div>
            )}

            {/* ── Email / Password / Confirm ── */}
            {selectedRole && (
              <div className="space-y-4 pt-1">
                <div className="h-px bg-white/5" />
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-1.5">Email <span className="text-red-400">*</span></label>
                  <input {...register('email')} type="email" placeholder="you@example.com" autoComplete="new-password"
                    className={cn('input w-full', errors.email && 'border-red-500/50')} />
                  {errors.email && <p className="text-red-400 text-xs mt-1">{errors.email.message}</p>}
                </div>
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-1.5">Password <span className="text-red-400">*</span></label>
                  <div className="relative">
                    <input {...register('password')} type={showPw ? 'text' : 'password'} placeholder="••••••••" autoComplete="new-password"
                      className={cn('input w-full pr-10', errors.password && 'border-red-500/50')} />
                    <button type="button" onClick={() => setShowPw(!showPw)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-white/30 hover:text-white/70 transition-colors">
                      {showPw ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                  {errors.password && <p className="text-red-400 text-xs mt-1">{errors.password.message}</p>}
                  <div className="flex flex-wrap gap-2 mt-2">
                    {[
                      { key: 'length',  label: '≥ 8 chars' },
                      { key: 'upper',   label: '1 uppercase' },
                      { key: 'digit',   label: '1 digit' },
                      { key: 'special', label: '1 special char' },
                    ].map(({ key, label }) => (
                      <span key={key} className={cn(
                        'text-xs px-2 py-0.5 rounded-full border transition-colors',
                        pwChecks[key as keyof typeof pwChecks]
                          ? 'border-green-500/50 bg-green-500/10 text-green-400'
                          : 'border-white/10 text-white/30'
                      )}>{label}</span>
                    ))}
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-1.5">Confirm Password <span className="text-red-400">*</span></label>
                  <div className="relative">
                    <input {...register('confirmPassword')} type={showConfirm ? 'text' : 'password'} placeholder="••••••••" autoComplete="new-password"
                      className={cn('input w-full pr-10', errors.confirmPassword && 'border-red-500/50')} />
                    <button type="button" onClick={() => setShowConfirm(!showConfirm)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-white/30 hover:text-white/70 transition-colors">
                      {showConfirm ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                  {errors.confirmPassword && <p className="text-red-400 text-xs mt-1">{errors.confirmPassword.message}</p>}
                </div>
              </div>
            )}

            {/* Captcha */}
            {selectedRole && (
              <div className="flex justify-center pt-1">
                <CaptchaWidget onVerify={handleCaptchaVerify} />
              </div>
            )}

            <div className="flex gap-3 mt-2">
              <button type="button" onClick={() => navigate('/login')}
                className="btn-ghost flex items-center gap-2 py-3 px-4">
                <ArrowLeft className="w-4 h-4" /> Back
              </button>
              <button
                type="submit"
                disabled={!selectedRole || !captchaToken || isRegistering}
                className="btn-primary flex-1 flex items-center justify-center gap-2 py-3 disabled:opacity-50"
              >
                {isRegistering
                  ? <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  : <><span>Create Account</span><ArrowRight className="w-4 h-4" /></>
                }
              </button>
            </div>
          </form>
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
