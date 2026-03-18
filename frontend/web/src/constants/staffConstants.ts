// src/constants/staffConstants.ts
// Single source of truth for all staff-related constants.
// No inline hardcoded values in components — always import from here.

// ─── Role Types (mirrors backend StaffRoleType enum) ─────────────────────────

export const STAFF_ROLE_TYPES = [
  {
    value: 'TEACHER' as const,
    label: 'Teacher',
    description: 'Subject matter expert responsible for delivering curriculum content and student assessments',
    color: 'text-brand-400',
    bg: 'bg-brand-500/15 border-brand-500/30',
  },
  {
    value: 'HOD' as const,
    label: 'Head of Department',
    description: 'Departmental leadership, curriculum planning, and academic oversight of subject teachers',
    color: 'text-violet-400',
    bg: 'bg-violet-500/15 border-violet-500/30',
  },
  {
    value: 'COORDINATOR' as const,
    label: 'Academic Coordinator',
    description: 'Cross-departmental academic coordination, timetabling, and program scheduling',
    color: 'text-amber-400',
    bg: 'bg-amber-500/15 border-amber-500/30',
  },
  {
    value: 'COUNSELOR' as const,
    label: 'Student Counselor',
    description: 'Student welfare, career guidance, psychological support, and parent communication',
    color: 'text-emerald-400',
    bg: 'bg-emerald-500/15 border-emerald-500/30',
  },
  {
    value: 'LIBRARIAN' as const,
    label: 'Librarian',
    description: 'Library operations, resource management, digital catalog maintenance, and reading programs',
    color: 'text-cyan-400',
    bg: 'bg-cyan-500/15 border-cyan-500/30',
  },
  {
    value: 'LAB_ASSISTANT' as const,
    label: 'Lab Assistant',
    description: 'Laboratory setup, equipment maintenance, safety compliance, and practical session support',
    color: 'text-orange-400',
    bg: 'bg-orange-500/15 border-orange-500/30',
  },
  {
    value: 'SPORTS_COACH' as const,
    label: 'Sports Coach',
    description: 'Athletic training, physical development, inter-school event coordination, and fitness programs',
    color: 'text-red-400',
    bg: 'bg-red-500/15 border-red-500/30',
  },
  {
    value: 'ADMIN_STAFF' as const,
    label: 'Admin Staff',
    description: 'Administrative operations, records management, fee processing, and general institutional support',
    color: 'text-white/60',
    bg: 'bg-white/8 border-white/15',
  },
] as const;

export type StaffRoleTypeValue = typeof STAFF_ROLE_TYPES[number]['value'];

export function getStaffRoleConfig(value: string | null | undefined) {
  return STAFF_ROLE_TYPES.find(r => r.value === value) ?? null;
}

// ─── Designations per role ────────────────────────────────────────────────────

export const DESIGNATIONS_BY_ROLE: Record<StaffRoleTypeValue, string[]> = {
  TEACHER: [
    'Assistant Teacher',
    'Senior Teacher',
    'Lead Teacher',
    'Subject Expert',
    'Guest Lecturer',
  ],
  HOD: [
    'Head of Department',
    'Senior HOD',
    'Acting HOD',
    'Department Coordinator',
  ],
  COORDINATOR: [
    'Academic Coordinator',
    'Senior Academic Coordinator',
    'Program Coordinator',
    'Curriculum Coordinator',
    'Examination Coordinator',
  ],
  COUNSELOR: [
    'Student Counselor',
    'Senior Counselor',
    'Career Counselor',
    'Wellbeing Counselor',
    'Academic Advisor',
  ],
  LIBRARIAN: [
    'Librarian',
    'Senior Librarian',
    'Assistant Librarian',
    'Digital Resource Manager',
  ],
  LAB_ASSISTANT: [
    'Lab Assistant',
    'Senior Lab Assistant',
    'Lab Technician',
    'Lab Supervisor',
    'Technical Assistant',
  ],
  SPORTS_COACH: [
    'Sports Coach',
    'Head Coach',
    'Athletic Trainer',
    'Physical Education Instructor',
    'Fitness Coordinator',
  ],
  ADMIN_STAFF: [
    'Administrative Executive',
    'Senior Admin Executive',
    'Office Manager',
    'Front Office Executive',
    'Administrative Coordinator',
  ],
};

// ─── Qualifications ───────────────────────────────────────────────────────────

export const QUALIFICATION_OPTIONS = [
  { value: 'B.Ed',    label: 'B.Ed — Bachelor of Education' },
  { value: 'M.Ed',    label: 'M.Ed — Master of Education' },
  { value: 'D.Ed',    label: 'D.Ed — Diploma in Education' },
  { value: 'B.Sc',    label: 'B.Sc — Bachelor of Science' },
  { value: 'M.Sc',    label: 'M.Sc — Master of Science' },
  { value: 'B.A',     label: 'B.A — Bachelor of Arts' },
  { value: 'M.A',     label: 'M.A — Master of Arts' },
  { value: 'B.Com',   label: 'B.Com — Bachelor of Commerce' },
  { value: 'M.Com',   label: 'M.Com — Master of Commerce' },
  { value: 'B.Tech',  label: 'B.Tech — Bachelor of Technology' },
  { value: 'M.Tech',  label: 'M.Tech — Master of Technology' },
  { value: 'MBA',     label: 'MBA — Master of Business Administration' },
  { value: 'MCA',     label: 'MCA — Master of Computer Applications' },
  { value: 'BCA',     label: 'BCA — Bachelor of Computer Applications' },
  { value: 'Ph.D',    label: 'Ph.D — Doctor of Philosophy' },
  { value: 'M.Lib',   label: 'M.Lib — Master of Library Science' },
  { value: 'B.P.Ed',  label: 'B.P.Ed — Bachelor of Physical Education' },
  { value: 'M.P.Ed',  label: 'M.P.Ed — Master of Physical Education' },
  { value: 'CTET',    label: 'CTET — Central Teacher Eligibility Test' },
  { value: 'STET',    label: 'STET — State Teacher Eligibility Test' },
  { value: 'NET',     label: 'NET — National Eligibility Test' },
  { value: 'Diploma', label: 'Diploma' },
];

// ─── Experience buckets (display labels) ─────────────────────────────────────

export const EXPERIENCE_RANGES = [
  { min: 0,  max: 0,  label: 'Fresher (< 1 year)' },
  { min: 1,  max: 2,  label: '1–2 years' },
  { min: 3,  max: 4,  label: '3–4 years' },
  { min: 5,  max: 9,  label: '5–9 years' },
  { min: 10, max: 14, label: '10–14 years' },
  { min: 15, max: 19, label: '15–19 years' },
  { min: 20, max: 60, label: '20+ years' },
];

export function experienceLabel(years: number | null | undefined): string {
  if (years == null) return '—';
  const range = EXPERIENCE_RANGES.find(r => years >= r.min && years <= r.max);
  return range?.label ?? `${years} yrs`;
}

// ─── Status display config ────────────────────────────────────────────────────

export const STAFF_STATUS_CONFIG: Record<string, { label: string; color: string; bg: string }> = {
  ACTIVE:           { label: 'Active',          color: 'text-emerald-400', bg: 'bg-emerald-500/15 border-emerald-500/30' },
  INACTIVE:         { label: 'Inactive',         color: 'text-white/40',    bg: 'bg-white/8 border-white/15' },
  INVITATION_SENT:  { label: 'Invited',          color: 'text-amber-400',   bg: 'bg-amber-500/15 border-amber-500/30' },
  PENDING_APPROVAL: { label: 'Pending Approval', color: 'text-orange-400',  bg: 'bg-orange-500/15 border-orange-500/30' },
};

// ─── Subjects (mirrors SubjectCatalog.SUBJECTS in backend) ────────────────────

export const SUBJECT_OPTIONS = [
  'Mathematics', 'Physics', 'Chemistry', 'Biology',
  'English', 'Hindi', 'History', 'Geography',
  'Computer Science', 'Social Science', 'Economics',
  'Political Science', 'Accountancy', 'Business Studies',
  'Physical Education', 'Fine Arts', 'Music',
];
