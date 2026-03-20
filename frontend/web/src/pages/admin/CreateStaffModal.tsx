// src/pages/admin/CreateStaffModal.tsx
import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  X, ChevronRight, ChevronLeft, User, GraduationCap, Sparkles,
  Loader2, Check, Copy, RefreshCw, Mail, Phone, Hash,
  BookOpen, Briefcase, Star, Bot,
} from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { cn } from '../../lib/utils';
import { useStaffBioGenerator } from '../../hooks/useStaffAI';
import {
  STAFF_ROLE_TYPES,
  DESIGNATIONS_BY_ROLE,
  QUALIFICATION_OPTIONS,
  SUBJECT_OPTIONS,
  type StaffRoleTypeValue,
} from '../../constants/staffConstants';

// ─── Types ────────────────────────────────────────────────────────────────────

interface CreateStaffModalProps {
  centerId: string;
  onClose: () => void;
  onCreated: () => void;
}

interface FormState {
  // Step 1 — Identity
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  employeeId: string;
  roleType: StaffRoleTypeValue | '';
  designation: string;
  // Step 2 — Qualifications
  subjects: string[];
  qualification: string;
  yearsOfExperience: string;
  // Step 3 — AI Polish
  bio: string;
}

const INITIAL_FORM: FormState = {
  firstName: '', lastName: '', email: '', phoneNumber: '',
  employeeId: '', roleType: '', designation: '',
  subjects: [], qualification: '',
  yearsOfExperience: '', bio: '',
};

const STEPS = [
  { id: 1, label: 'Identity',       icon: User },
  { id: 2, label: 'Qualifications', icon: GraduationCap },
  { id: 3, label: 'AI Polish',      icon: Sparkles },
] as const;

// ─── Field component helpers ──────────────────────────────────────────────────

function FieldLabel({ children }: { children: React.ReactNode }) {
  return <label className="block text-xs font-medium text-white/50 mb-1.5">{children}</label>;
}

function FieldInput({
  value, onChange, placeholder, type = 'text', className,
}: {
  value: string; onChange: (v: string) => void; placeholder?: string;
  type?: string; className?: string;
}) {
  return (
    <input
      type={type}
      value={value}
      onChange={e => onChange(e.target.value)}
      placeholder={placeholder}
      className={cn(
        'w-full bg-white/5 border border-white/10 rounded-xl px-3.5 py-2.5 text-sm text-white placeholder:text-white/25',
        'focus:outline-none focus:border-brand-500/50 focus:bg-white/8 transition-colors',
        className
      )}
    />
  );
}

// ─── Modal ────────────────────────────────────────────────────────────────────

export default function CreateStaffModal({ centerId, onClose, onCreated }: CreateStaffModalProps) {
  const [step, setStep]       = useState(1);
  const [form, setForm]       = useState<FormState>(INITIAL_FORM);
  const [errors, setErrors]   = useState<Partial<Record<keyof FormState, string>>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { generateBio, isGenerating } = useStaffBioGenerator();

  function patch<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm(prev => ({ ...prev, [key]: value }));
    setErrors(prev => ({ ...prev, [key]: undefined }));
  }

  // ── Validation ──────────────────────────────────────────────────────────────

  function validateStep1(): boolean {
    const e: typeof errors = {};
    if (!form.firstName.trim()) e.firstName = 'Required';
    if (!form.lastName.trim())  e.lastName  = 'Required';
    if (!form.email.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email))
      e.email = 'Valid email required';
    if (!form.roleType) e.roleType = 'Select a role type';
    setErrors(e);
    return Object.keys(e).length === 0;
  }

  function validateStep2(): boolean {
    const e: typeof errors = {};
    const exp = parseInt(form.yearsOfExperience);
    if (form.yearsOfExperience !== '' && (isNaN(exp) || exp < 0 || exp > 60))
      e.yearsOfExperience = 'Must be between 0 and 60';
    setErrors(e);
    return Object.keys(e).length === 0;
  }

  function goNext() {
    if (step === 1 && !validateStep1()) return;
    if (step === 2 && !validateStep2()) return;
    setStep(s => s + 1);
  }

  // ── AI Bio ──────────────────────────────────────────────────────────────────

  async function handleGenerateBio() {
    const bio = await generateBio({
      firstName: form.firstName,
      lastName:  form.lastName,
      roleType:  form.roleType,
      designation: form.designation,
      subjects:  form.subjects.join(', '),
      yearsOfExperience: form.yearsOfExperience !== '' ? parseInt(form.yearsOfExperience) : null,
      qualification: form.qualification,
    });
    if (bio) patch('bio', bio);
  }

  // ── Submit ──────────────────────────────────────────────────────────────────

  async function handleSubmit() {
    setIsSubmitting(true);
    try {
      await api.post(`/api/v1/centers/${centerId}/staff`, {
        firstName:         form.firstName.trim(),
        lastName:          form.lastName.trim(),
        email:             form.email.trim().toLowerCase(),
        phoneNumber:       form.phoneNumber.trim() || null,
        employeeId:        form.employeeId.trim() || null,
        roleType:          form.roleType || null,
        designation:       form.designation.trim() || null,
        subjects:          showSubjects && form.subjects.length ? form.subjects.join(',') : null,
        qualification:     form.qualification.trim() || null,
        yearsOfExperience: form.yearsOfExperience !== '' ? parseInt(form.yearsOfExperience) : null,
        bio:               form.bio.trim() || null,
      });
      toast.success(`${form.firstName} ${form.lastName} added to staff`);
      onCreated();
    } catch (err: unknown) {
      const e = err as { response?: { data?: { detail?: string } } };
      toast.error(e.response?.data?.detail ?? 'Failed to create staff member');
    } finally {
      setIsSubmitting(false);
    }
  }

  const roleConfig = STAFF_ROLE_TYPES.find(r => r.value === form.roleType);
  const designationOptions = form.roleType
    ? DESIGNATIONS_BY_ROLE[form.roleType as StaffRoleTypeValue]
    : [];
  const showSubjects = ['TEACHER', 'HOD', 'COORDINATOR'].includes(form.roleType);

  // ─── Render ─────────────────────────────────────────────────────────────────

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={onClose} />

      <motion.div
        initial={{ opacity: 0, scale: 0.97, y: 12 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.97, y: 12 }}
        transition={{ duration: 0.2 }}
        className="relative w-full max-w-xl bg-surface-100 border border-white/10 rounded-2xl shadow-2xl overflow-hidden"
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-white/8">
          <div>
            <h2 className="text-base font-semibold text-white">Create Staff Member</h2>
            <p className="text-xs text-white/40 mt-0.5">Staff member will be activated immediately</p>
          </div>
          <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-white/8 text-white/40 hover:text-white transition-colors">
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Step indicator */}
        <div className="flex items-center gap-0 px-6 pt-4 pb-2">
          {STEPS.map((s, i) => {
            const Icon = s.icon;
            const isActive   = step === s.id;
            const isComplete = step > s.id;
            return (
              <div key={s.id} className="flex items-center gap-0 flex-1">
                <div className={cn(
                  'flex items-center gap-1.5 text-xs font-medium px-3 py-1.5 rounded-lg transition-colors',
                  isActive   ? 'text-white bg-brand-500/20 border border-brand-500/30' :
                  isComplete ? 'text-emerald-400'  : 'text-white/30'
                )}>
                  {isComplete
                    ? <Check className="w-3.5 h-3.5" />
                    : <Icon className="w-3.5 h-3.5" />
                  }
                  {s.label}
                </div>
                {i < STEPS.length - 1 && (
                  <div className={cn('flex-1 h-px mx-1', step > s.id ? 'bg-emerald-500/40' : 'bg-white/8')} />
                )}
              </div>
            );
          })}
        </div>

        {/* Body */}
        <div className="px-6 py-4 max-h-[60vh] overflow-y-auto space-y-3">
          <AnimatePresence mode="wait">
            <motion.div
              key={step}
              initial={{ opacity: 0, x: 16 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -16 }}
              transition={{ duration: 0.15 }}
              className="space-y-3"
            >

              {/* ── Step 1: Identity ──────────────────────────────────────── */}
              {step === 1 && (
                <>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <FieldLabel>First Name *</FieldLabel>
                      <FieldInput value={form.firstName} onChange={v => patch('firstName', v)} placeholder="Priya" />
                      {errors.firstName && <p className="text-xs text-red-400 mt-1">{errors.firstName}</p>}
                    </div>
                    <div>
                      <FieldLabel>Last Name *</FieldLabel>
                      <FieldInput value={form.lastName} onChange={v => patch('lastName', v)} placeholder="Sharma" />
                      {errors.lastName && <p className="text-xs text-red-400 mt-1">{errors.lastName}</p>}
                    </div>
                  </div>

                  <div>
                    <FieldLabel><span className="flex items-center gap-1"><Mail className="w-3 h-3" />Work Email *</span></FieldLabel>
                    <FieldInput value={form.email} onChange={v => patch('email', v)} placeholder="priya.sharma@school.edu.in" type="email" />
                    {errors.email && <p className="text-xs text-red-400 mt-1">{errors.email}</p>}
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <FieldLabel><span className="flex items-center gap-1"><Phone className="w-3 h-3" />Phone</span></FieldLabel>
                      <FieldInput value={form.phoneNumber} onChange={v => patch('phoneNumber', v)} placeholder="+91 98765 43210" />
                    </div>
                    <div>
                      <FieldLabel><span className="flex items-center gap-1"><Hash className="w-3 h-3" />Employee ID</span></FieldLabel>
                      <FieldInput value={form.employeeId} onChange={v => patch('employeeId', v)} placeholder="EMP-101" />
                    </div>
                  </div>

                  <div>
                    <FieldLabel><span className="flex items-center gap-1"><Briefcase className="w-3 h-3" />Role Type *</span></FieldLabel>
                    <div className="grid grid-cols-2 gap-2">
                      {STAFF_ROLE_TYPES.map(role => (
                        <button
                          key={role.value}
                          type="button"
                          onClick={() => { patch('roleType', role.value); patch('designation', ''); }}
                          className={cn(
                            'flex items-start gap-2 p-2.5 rounded-xl border text-left transition-all',
                            form.roleType === role.value
                              ? `${role.bg} border-opacity-60`
                              : 'border-white/8 hover:border-white/15 hover:bg-white/3'
                          )}
                        >
                          <div className={cn('mt-0.5 text-xs font-semibold', role.color)}>{role.label}</div>
                        </button>
                      ))}
                    </div>
                    {errors.roleType && <p className="text-xs text-red-400 mt-1">{errors.roleType}</p>}
                  </div>

                  {designationOptions.length > 0 && (
                    <div>
                      <FieldLabel>Designation</FieldLabel>
                      <select
                        value={form.designation}
                        onChange={e => patch('designation', e.target.value)}
                        className="w-full bg-white/5 border border-white/10 rounded-xl px-3.5 py-2.5 text-sm text-white focus:outline-none focus:border-brand-500/50 transition-colors"
                      >
                        <option value="">Select designation...</option>
                        {designationOptions.map(d => (
                          <option key={d} value={d} className="bg-surface-100">{d}</option>
                        ))}
                      </select>
                    </div>
                  )}
                </>
              )}

              {/* ── Step 2: Qualifications ───────────────────────────────── */}
              {step === 2 && (
                <>
                  {showSubjects && (
                    <div>
                      <FieldLabel><span className="flex items-center gap-1"><BookOpen className="w-3 h-3" />Subjects</span></FieldLabel>
                      <div className="flex flex-wrap gap-1.5 p-2.5 bg-white/3 border border-white/8 rounded-xl min-h-[44px]">
                        {SUBJECT_OPTIONS.map(subject => {
                          const selected = form.subjects.includes(subject);
                          return (
                            <button
                              key={subject}
                              type="button"
                              onClick={() => {
                                patch('subjects', selected
                                  ? form.subjects.filter(s => s !== subject)
                                  : [...form.subjects, subject]
                                );
                              }}
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
                    </div>
                  )}

                  <div>
                    <FieldLabel><span className="flex items-center gap-1"><Star className="w-3 h-3" />Experience (years)</span></FieldLabel>
                    <FieldInput
                      value={form.yearsOfExperience}
                      onChange={v => patch('yearsOfExperience', v)}
                      placeholder="0"
                      type="number"
                    />
                    {errors.yearsOfExperience && <p className="text-xs text-red-400 mt-1">{errors.yearsOfExperience}</p>}
                  </div>

                  <div>
                    <FieldLabel><span className="flex items-center gap-1"><GraduationCap className="w-3 h-3" />Qualifications</span></FieldLabel>
                    <div className="flex flex-wrap gap-1.5 p-2.5 bg-white/3 border border-white/8 rounded-xl min-h-[44px]">
                      {QUALIFICATION_OPTIONS.map(q => {
                        const selected = form.qualification.includes(q.value);
                        return (
                          <button
                            key={q.value}
                            type="button"
                            onClick={() => {
                              const current = form.qualification
                                .split(',').map(s => s.trim()).filter(Boolean);
                              const updated = selected
                                ? current.filter(c => c !== q.value)
                                : [...current, q.value];
                              patch('qualification', updated.join(', '));
                            }}
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
                </>
              )}

              {/* ── Step 3: AI Polish ────────────────────────────────────── */}
              {step === 3 && (
                <>
                  {/* Preview card */}
                  <div className="bg-white/3 border border-white/8 rounded-xl p-4">
                    <div className="flex items-center gap-3 mb-3">
                      <div className={cn(
                        'w-10 h-10 rounded-xl flex items-center justify-center text-sm font-bold',
                        roleConfig?.bg ?? 'bg-white/10 border border-white/15'
                      )}>
                        <span className={roleConfig?.color ?? 'text-white/60'}>
                          {form.firstName[0]}{form.lastName[0]}
                        </span>
                      </div>
                      <div>
                        <p className="text-sm font-semibold text-white">
                          {form.firstName} {form.lastName}
                        </p>
                        <p className={cn('text-xs', roleConfig?.color ?? 'text-white/40')}>
                          {form.designation || roleConfig?.label || '—'}
                        </p>
                      </div>
                    </div>
                    <div className="flex flex-wrap gap-1.5">
                      {form.subjects.map(s => (
                        <span key={s} className="text-xs px-2 py-0.5 bg-white/8 border border-white/10 rounded-lg text-white/60">{s}</span>
                      ))}
                      {form.qualification && form.qualification.split(',').map(q => (
                        <span key={q} className="text-xs px-2 py-0.5 bg-violet-500/10 border border-violet-500/20 rounded-lg text-violet-300">{q.trim()}</span>
                      ))}
                      {form.yearsOfExperience && (
                        <span className="text-xs px-2 py-0.5 bg-amber-500/10 border border-amber-500/20 rounded-lg text-amber-300">
                          {form.yearsOfExperience} yrs exp
                        </span>
                      )}
                    </div>
                  </div>

                  {/* AI Bio Generator */}
                  <div>
                    <div className="flex items-center justify-between mb-1.5">
                      <FieldLabel><span className="flex items-center gap-1"><Bot className="w-3 h-3 text-brand-400" />Professional Bio</span></FieldLabel>
                      <button
                        type="button"
                        onClick={handleGenerateBio}
                        disabled={isGenerating}
                        className="flex items-center gap-1 text-xs text-brand-400 hover:text-brand-300 disabled:opacity-50 transition-colors"
                      >
                        {isGenerating
                          ? <><Loader2 className="w-3 h-3 animate-spin" /> Generating...</>
                          : form.bio
                          ? <><RefreshCw className="w-3 h-3" /> Regenerate</>
                          : <><Sparkles className="w-3 h-3" /> AI Generate</>
                        }
                      </button>
                    </div>
                    <textarea
                      value={form.bio}
                      onChange={e => patch('bio', e.target.value)}
                      placeholder="Click 'AI Generate' for a professional bio, or write your own..."
                      rows={4}
                      className="w-full bg-white/5 border border-white/10 rounded-xl px-3.5 py-2.5 text-sm text-white placeholder:text-white/25 focus:outline-none focus:border-brand-500/50 resize-none transition-colors"
                    />
                    {form.bio && (
                      <button
                        type="button"
                        onClick={() => { navigator.clipboard.writeText(form.bio); toast.success('Copied'); }}
                        className="flex items-center gap-1 text-xs text-white/30 hover:text-white/60 mt-1 transition-colors"
                      >
                        <Copy className="w-3 h-3" /> Copy bio
                      </button>
                    )}
                  </div>

                  <div className="bg-emerald-500/5 border border-emerald-500/15 rounded-xl p-3">
                    <p className="text-xs text-emerald-300/80">
                      Staff member will be created as <strong className="text-emerald-300">Active</strong> and
                      linked to <span className="text-white/60">{form.email}</span>.
                    </p>
                  </div>
                </>
              )}

            </motion.div>
          </AnimatePresence>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between px-6 py-4 border-t border-white/8">
          <button
            type="button"
            onClick={step === 1 ? onClose : () => setStep(s => s - 1)}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm text-white/50 hover:text-white hover:bg-white/5 transition-colors"
          >
            {step === 1 ? <><X className="w-3.5 h-3.5" /> Cancel</> : <><ChevronLeft className="w-3.5 h-3.5" /> Back</>}
          </button>

          {step < 3 ? (
            <button
              type="button"
              onClick={goNext}
              className="flex items-center gap-1.5 px-5 py-2 rounded-xl text-sm font-medium bg-brand-600 hover:bg-brand-500 text-white transition-colors"
            >
              Next <ChevronRight className="w-3.5 h-3.5" />
            </button>
          ) : (
            <button
              type="button"
              onClick={handleSubmit}
              disabled={isSubmitting}
              className="flex items-center gap-1.5 px-5 py-2 rounded-xl text-sm font-medium bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white transition-colors"
            >
              {isSubmitting
                ? <><Loader2 className="w-3.5 h-3.5 animate-spin" /> Creating...</>
                : <><Check className="w-3.5 h-3.5" /> Create Staff</>
              }
            </button>
          )}
        </div>
      </motion.div>
    </div>
  );
}
