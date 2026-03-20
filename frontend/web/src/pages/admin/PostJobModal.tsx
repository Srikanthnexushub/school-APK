// src/pages/admin/PostJobModal.tsx
import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  X, Briefcase, BookOpen, GraduationCap, DollarSign,
  Calendar, Loader2, Check, Star,
} from 'lucide-react';
import { toast } from 'sonner';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../lib/api';
import { cn } from '../../lib/utils';
import {
  STAFF_ROLE_TYPES,
  SUBJECT_OPTIONS,
  QUALIFICATION_OPTIONS,
  type StaffRoleTypeValue,
} from '../../constants/staffConstants';
import {
  JOB_TYPES,
  JOB_STATUSES,
  SUBJECT_ROLES,
  type JobTypeValue,
  type JobStatusValue,
} from '../../constants/jobConstants';

// ─── Types ────────────────────────────────────────────────────────────────────

export interface JobPosting {
  id: string;
  centerId: string;
  centerName: string;
  centerCity: string;
  title: string;
  description: string | null;
  roleType: string;
  subjects: string | null;
  qualifications: string | null;
  experienceMinYears: number | null;
  jobType: string;
  salaryMin: number | null;
  salaryMax: number | null;
  deadline: string | null;
  status: string;
  postedAt: string;
  updatedAt: string;
}

interface PostJobModalProps {
  centerId: string;
  editing?: JobPosting | null;
  onClose: () => void;
  onSaved: () => void;
}

interface FormState {
  title: string;
  description: string;
  roleType: StaffRoleTypeValue | '';
  subjects: string[];
  qualifications: string[];
  experienceMinYears: string;
  jobType: JobTypeValue | '';
  salaryMin: string;
  salaryMax: string;
  deadline: string;
  status: JobStatusValue;
}

const INITIAL_FORM: FormState = {
  title: '',
  description: '',
  roleType: '',
  subjects: [],
  qualifications: [],
  experienceMinYears: '',
  jobType: '',
  salaryMin: '',
  salaryMax: '',
  deadline: '',
  status: 'DRAFT',
};

// ─── Field helpers ────────────────────────────────────────────────────────────

function FieldLabel({ children, optional }: { children: React.ReactNode; optional?: boolean }) {
  return (
    <label className="flex items-center gap-1 text-xs font-medium text-white/50 mb-1.5">
      {children}
      {optional && <span className="text-white/25 font-normal">optional</span>}
    </label>
  );
}

function SectionHeading({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex items-center gap-2 pb-1 border-b border-white/5">
      <span className="text-xs font-semibold text-white/35 uppercase tracking-wider">{children}</span>
    </div>
  );
}

// ─── Modal ────────────────────────────────────────────────────────────────────

export default function PostJobModal({ centerId, editing, onClose, onSaved }: PostJobModalProps) {
  const queryClient = useQueryClient();
  const isEditing   = !!editing;

  const [form, setForm]     = useState<FormState>(INITIAL_FORM);
  const [errors, setErrors] = useState<Partial<Record<keyof FormState, string>>>({});

  // Populate form when editing
  useEffect(() => {
    if (editing) {
      setForm({
        title:              editing.title,
        description:        editing.description ?? '',
        roleType:           (editing.roleType as StaffRoleTypeValue) ?? '',
        subjects:           editing.subjects ? editing.subjects.split(',').map(s => s.trim()).filter(Boolean) : [],
        qualifications:     editing.qualifications ? editing.qualifications.split(',').map(s => s.trim()).filter(Boolean) : [],
        experienceMinYears: editing.experienceMinYears != null ? String(editing.experienceMinYears) : '',
        jobType:            (editing.jobType as JobTypeValue) ?? '',
        salaryMin:          editing.salaryMin != null ? String(editing.salaryMin) : '',
        salaryMax:          editing.salaryMax != null ? String(editing.salaryMax) : '',
        deadline:           editing.deadline ?? '',
        status:             (editing.status as JobStatusValue) ?? 'DRAFT',
      });
    }
  }, [editing]);

  function patch<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm(prev => ({ ...prev, [key]: value }));
    setErrors(prev => ({ ...prev, [key]: undefined }));
  }

  function toggleSubject(subject: string) {
    patch('subjects', form.subjects.includes(subject)
      ? form.subjects.filter(s => s !== subject)
      : [...form.subjects, subject]
    );
  }

  function toggleQual(qual: string) {
    patch('qualifications', form.qualifications.includes(qual)
      ? form.qualifications.filter(q => q !== qual)
      : [...form.qualifications, qual]
    );
  }

  // ── Validation ──────────────────────────────────────────────────────────────

  function validate(): boolean {
    const e: typeof errors = {};
    if (!form.title.trim())  e.title    = 'Job title is required';
    if (!form.roleType)      e.roleType  = 'Select a role type';
    if (!form.jobType)       e.jobType   = 'Select a job type';

    const expVal = form.experienceMinYears !== '' ? parseInt(form.experienceMinYears) : null;
    if (form.experienceMinYears !== '' && (isNaN(expVal!) || expVal! < 0 || expVal! > 60))
      e.experienceMinYears = 'Must be 0–60';

    const salMin = form.salaryMin !== '' ? parseInt(form.salaryMin) : null;
    const salMax = form.salaryMax !== '' ? parseInt(form.salaryMax) : null;
    if (salMin != null && salMax != null && salMin > salMax)
      e.salaryMin = 'Min salary cannot exceed max';

    setErrors(e);
    return Object.keys(e).length === 0;
  }

  // ── Build payload ────────────────────────────────────────────────────────────

  function buildPayload() {
    const showSubjects = SUBJECT_ROLES.includes(form.roleType);
    return {
      title:              form.title.trim(),
      description:        form.description.trim() || undefined,
      roleType:           form.roleType,
      subjects:           showSubjects && form.subjects.length ? form.subjects.join(',') : undefined,
      qualifications:     form.qualifications.length ? form.qualifications.join(',') : undefined,
      experienceMinYears: form.experienceMinYears !== '' ? parseInt(form.experienceMinYears) : undefined,
      jobType:            form.jobType,
      salaryMin:          form.salaryMin !== '' ? parseInt(form.salaryMin) : undefined,
      salaryMax:          form.salaryMax !== '' ? parseInt(form.salaryMax) : undefined,
      deadline:           form.deadline || undefined,
      status:             form.status,
    };
  }

  // ── Mutations ────────────────────────────────────────────────────────────────

  const createMutation = useMutation({
    mutationFn: (payload: ReturnType<typeof buildPayload>) =>
      api.post(`/api/v1/centers/${centerId}/jobs`, payload),
    onSuccess: () => {
      toast.success('Job posted successfully');
      queryClient.invalidateQueries({ queryKey: ['jobs', centerId] });
      onSaved();
    },
    onError: (err: { response?: { data?: { detail?: string } } }) => {
      toast.error(err.response?.data?.detail ?? 'Failed to post job');
    },
  });

  const updateMutation = useMutation({
    mutationFn: (payload: ReturnType<typeof buildPayload>) =>
      api.put(`/api/v1/centers/${centerId}/jobs/${editing!.id}`, payload),
    onSuccess: () => {
      toast.success('Job updated');
      queryClient.invalidateQueries({ queryKey: ['jobs', centerId] });
      onSaved();
    },
    onError: (err: { response?: { data?: { detail?: string } } }) => {
      toast.error(err.response?.data?.detail ?? 'Failed to update job');
    },
  });

  const isSubmitting = createMutation.isPending || updateMutation.isPending;

  function handleSubmit() {
    if (!validate()) return;
    const payload = buildPayload();
    if (isEditing) {
      updateMutation.mutate(payload);
    } else {
      createMutation.mutate(payload);
    }
  }

  const showSubjects = SUBJECT_ROLES.includes(form.roleType);
  const isDraft      = form.status === 'DRAFT';

  // ─── Render ──────────────────────────────────────────────────────────────────

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={onClose} />

      <motion.div
        initial={{ opacity: 0, scale: 0.97, y: 12 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.97, y: 12 }}
        transition={{ duration: 0.2 }}
        className="relative w-full max-w-2xl bg-surface-100 border border-white/10 rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]"
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-white/8 flex-shrink-0">
          <div>
            <h2 className="text-base font-semibold text-white flex items-center gap-2">
              <Briefcase className="w-4 h-4 text-brand-400" />
              {isEditing ? 'Edit Job Posting' : 'Post a Job'}
            </h2>
            <p className="text-xs text-white/40 mt-0.5">
              {isEditing
                ? 'Update the details of this job posting'
                : 'Fill in the role details — you can save as draft or publish immediately'}
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg hover:bg-white/8 text-white/40 hover:text-white transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Scrollable body */}
        <div className="flex-1 overflow-y-auto px-6 py-5 space-y-6">

          {/* ── Section 1: Role & Type ──────────────────────────────────── */}
          <div className="space-y-4">
            <SectionHeading>Role &amp; Type</SectionHeading>

            {/* Role Type */}
            <div>
              <FieldLabel>
                <Briefcase className="w-3 h-3" /> Role Type *
              </FieldLabel>
              <div className="grid grid-cols-2 gap-2">
                {STAFF_ROLE_TYPES.map(role => (
                  <button
                    key={role.value}
                    type="button"
                    onClick={() => patch('roleType', role.value)}
                    className={cn(
                      'flex items-start gap-2 p-2.5 rounded-xl border text-left transition-all',
                      form.roleType === role.value
                        ? `${role.bg} border-opacity-60`
                        : 'border-white/8 hover:border-white/15 hover:bg-white/3'
                    )}
                  >
                    <div className="flex-1 min-w-0">
                      <div className={cn('text-xs font-semibold', role.color)}>{role.label}</div>
                    </div>
                    {form.roleType === role.value && (
                      <Check className={cn('w-3.5 h-3.5 flex-shrink-0 mt-0.5', role.color)} />
                    )}
                  </button>
                ))}
              </div>
              {errors.roleType && <p className="text-xs text-red-400 mt-1">{errors.roleType}</p>}
            </div>

            {/* Job Type */}
            <div>
              <FieldLabel>
                <Briefcase className="w-3 h-3" /> Job Type *
              </FieldLabel>
              <div className="flex gap-2">
                {JOB_TYPES.map(jt => (
                  <button
                    key={jt.value}
                    type="button"
                    onClick={() => patch('jobType', jt.value)}
                    className={cn(
                      'flex-1 py-2 px-3 rounded-xl border text-xs font-medium transition-all',
                      form.jobType === jt.value
                        ? 'bg-brand-500/20 border-brand-500/40 text-brand-300'
                        : 'border-white/8 text-white/50 hover:border-white/20 hover:text-white/80 hover:bg-white/3'
                    )}
                  >
                    {jt.label}
                  </button>
                ))}
              </div>
              {errors.jobType && <p className="text-xs text-red-400 mt-1">{errors.jobType}</p>}
            </div>

            {/* Status (create only) */}
            {!isEditing && (
              <div>
                <FieldLabel>
                  Publish Status
                </FieldLabel>
                <div className="flex gap-2">
                  {JOB_STATUSES.filter(s => s.value === 'DRAFT' || s.value === 'OPEN').map(s => (
                    <button
                      key={s.value}
                      type="button"
                      onClick={() => patch('status', s.value as JobStatusValue)}
                      className={cn(
                        'flex-1 py-2 px-3 rounded-xl border text-xs font-medium transition-all',
                        form.status === s.value
                          ? `${s.bg} ${s.color}`
                          : 'border-white/8 text-white/50 hover:border-white/20 hover:text-white/80 hover:bg-white/3'
                      )}
                    >
                      {s.value === 'DRAFT' ? 'Save as Draft' : 'Post Now (Open)'}
                    </button>
                  ))}
                </div>
                <p className="text-xs text-white/25 mt-1.5">
                  {isDraft
                    ? 'Draft — only visible to you. Publish anytime from the job card.'
                    : 'Open — immediately visible on the public job board.'}
                </p>
              </div>
            )}
          </div>

          {/* ── Section 2: Job Details ──────────────────────────────────── */}
          <div className="space-y-4">
            <SectionHeading>Job Details</SectionHeading>

            {/* Title */}
            <div>
              <FieldLabel>Job Title *</FieldLabel>
              <input
                type="text"
                value={form.title}
                onChange={e => patch('title', e.target.value)}
                placeholder="e.g. Senior Mathematics Teacher"
                className={cn(
                  'w-full bg-white/5 border rounded-xl px-3.5 py-2.5 text-sm text-white placeholder:text-white/25',
                  'focus:outline-none focus:bg-white/8 transition-colors',
                  errors.title ? 'border-red-500/50 focus:border-red-500/70' : 'border-white/10 focus:border-brand-500/50'
                )}
              />
              {errors.title && <p className="text-xs text-red-400 mt-1">{errors.title}</p>}
            </div>

            {/* Description */}
            <div>
              <FieldLabel optional>
                Description
              </FieldLabel>
              <textarea
                value={form.description}
                onChange={e => patch('description', e.target.value)}
                placeholder="Describe responsibilities, expectations, culture fit…"
                rows={4}
                className="w-full bg-white/5 border border-white/10 rounded-xl px-3.5 py-2.5 text-sm text-white placeholder:text-white/25 focus:outline-none focus:border-brand-500/50 focus:bg-white/8 resize-none transition-colors"
              />
            </div>

            {/* Subjects (conditional) */}
            <AnimatePresence>
              {showSubjects && (
                <motion.div
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  transition={{ duration: 0.15 }}
                >
                  <FieldLabel optional>
                    <BookOpen className="w-3 h-3" /> Subjects
                  </FieldLabel>
                  <div className="flex flex-wrap gap-1.5 p-2.5 bg-white/3 border border-white/8 rounded-xl min-h-[44px]">
                    {SUBJECT_OPTIONS.map(subject => {
                      const selected = form.subjects.includes(subject);
                      return (
                        <button
                          key={subject}
                          type="button"
                          onClick={() => toggleSubject(subject)}
                          className={cn(
                            'px-2 py-1 rounded-lg text-xs font-medium transition-colors border',
                            selected
                              ? 'bg-brand-500/20 border-brand-500/40 text-brand-300'
                              : 'bg-white/5 border-white/8 text-white/50 hover:text-white/80 hover:bg-white/8'
                          )}
                        >
                          {subject}
                        </button>
                      );
                    })}
                  </div>
                </motion.div>
              )}
            </AnimatePresence>

            {/* Qualifications */}
            <div>
              <FieldLabel optional>
                <GraduationCap className="w-3 h-3" /> Required Qualifications
              </FieldLabel>
              <div className="flex flex-wrap gap-1.5 p-2.5 bg-white/3 border border-white/8 rounded-xl min-h-[44px]">
                {QUALIFICATION_OPTIONS.map(q => {
                  const selected = form.qualifications.includes(q.value);
                  return (
                    <button
                      key={q.value}
                      type="button"
                      onClick={() => toggleQual(q.value)}
                      className={cn(
                        'px-2 py-1 rounded-lg text-xs font-medium transition-colors border',
                        selected
                          ? 'bg-violet-500/20 border-violet-500/40 text-violet-300'
                          : 'bg-white/5 border-white/8 text-white/50 hover:text-white/80 hover:bg-white/8'
                      )}
                    >
                      {q.value}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Experience */}
            <div className="w-1/3">
              <FieldLabel optional>
                <Star className="w-3 h-3" /> Min. Experience (years)
              </FieldLabel>
              <input
                type="number"
                min={0}
                max={60}
                value={form.experienceMinYears}
                onChange={e => patch('experienceMinYears', e.target.value)}
                placeholder="0"
                className={cn(
                  'w-full bg-white/5 border rounded-xl px-3.5 py-2.5 text-sm text-white placeholder:text-white/25',
                  'focus:outline-none focus:bg-white/8 transition-colors',
                  errors.experienceMinYears ? 'border-red-500/50' : 'border-white/10 focus:border-brand-500/50'
                )}
              />
              {errors.experienceMinYears && (
                <p className="text-xs text-red-400 mt-1">{errors.experienceMinYears}</p>
              )}
            </div>
          </div>

          {/* ── Section 3: Compensation & Deadline ─────────────────────── */}
          <div className="space-y-4">
            <SectionHeading>Compensation &amp; Deadline</SectionHeading>

            {/* Salary range */}
            <div>
              <FieldLabel optional>
                <DollarSign className="w-3 h-3" /> Annual Salary Range (₹)
              </FieldLabel>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <input
                    type="number"
                    min={0}
                    value={form.salaryMin}
                    onChange={e => patch('salaryMin', e.target.value)}
                    placeholder="Min e.g. 400000"
                    className={cn(
                      'w-full bg-white/5 border rounded-xl px-3.5 py-2.5 text-sm text-white placeholder:text-white/25',
                      'focus:outline-none focus:bg-white/8 transition-colors',
                      errors.salaryMin ? 'border-red-500/50' : 'border-white/10 focus:border-brand-500/50'
                    )}
                  />
                  {errors.salaryMin && <p className="text-xs text-red-400 mt-1">{errors.salaryMin}</p>}
                </div>
                <div>
                  <input
                    type="number"
                    min={0}
                    value={form.salaryMax}
                    onChange={e => patch('salaryMax', e.target.value)}
                    placeholder="Max e.g. 700000"
                    className={cn(
                      'w-full bg-white/5 border border-white/10 rounded-xl px-3.5 py-2.5 text-sm text-white placeholder:text-white/25',
                      'focus:outline-none focus:border-brand-500/50 focus:bg-white/8 transition-colors'
                    )}
                  />
                </div>
              </div>
            </div>

            {/* Deadline */}
            <div className="w-1/2">
              <FieldLabel optional>
                <Calendar className="w-3 h-3" /> Application Deadline
              </FieldLabel>
              <input
                type="date"
                value={form.deadline}
                min={new Date().toISOString().split('T')[0]}
                onChange={e => patch('deadline', e.target.value)}
                className="w-full bg-white/5 border border-white/10 rounded-xl px-3.5 py-2.5 text-sm text-white focus:outline-none focus:border-brand-500/50 focus:bg-white/8 transition-colors [color-scheme:dark]"
              />
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between px-6 py-4 border-t border-white/8 flex-shrink-0">
          <button
            type="button"
            onClick={onClose}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm text-white/50 hover:text-white hover:bg-white/5 transition-colors"
          >
            <X className="w-3.5 h-3.5" /> Cancel
          </button>

          <button
            type="button"
            onClick={handleSubmit}
            disabled={isSubmitting}
            className={cn(
              'flex items-center gap-1.5 px-5 py-2 rounded-xl text-sm font-medium transition-colors disabled:opacity-50',
              isEditing || form.status === 'OPEN'
                ? 'bg-brand-600 hover:bg-brand-500 text-white'
                : 'bg-white/10 hover:bg-white/15 text-white/80'
            )}
          >
            {isSubmitting ? (
              <><Loader2 className="w-3.5 h-3.5 animate-spin" /> {isEditing ? 'Saving…' : 'Posting…'}</>
            ) : isEditing ? (
              <><Check className="w-3.5 h-3.5" /> Save Changes</>
            ) : form.status === 'OPEN' ? (
              <><Briefcase className="w-3.5 h-3.5" /> Post Job</>
            ) : (
              <><Check className="w-3.5 h-3.5" /> Save Draft</>
            )}
          </button>
        </div>
      </motion.div>
    </div>
  );
}
