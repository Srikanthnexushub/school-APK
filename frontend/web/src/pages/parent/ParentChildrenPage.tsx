import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Users, Plus, Trash2, Edit2, Save, X, ChevronDown, ChevronUp,
  Loader2, School, Calendar, BookOpen, Hash, Key,
} from 'lucide-react';
import { cn } from '../../lib/utils';
import { Avatar } from '../../components/ui/Avatar';
import { toast } from 'sonner';
import api from '../../lib/api';

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

interface StudentLookupByCodeResponse {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  city?: string;
  state?: string;
  board?: string;
  currentClass?: number;
  parentLinkCode?: string;
}

interface CenterOption { id: string; name: string; }

interface EditSchoolForm {
  dateOfBirth: string;
  schoolName: string;
  standard: string;
  board: string;
  rollNumber: string;
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

// ─── Link Child Modal ─────────────────────────────────────────────────────────

function LinkChildModal({
  profileId,
  onClose,
  onLinked,
}: {
  profileId: string;
  onClose: () => void;
  onLinked: () => void;
}) {
  const [code, setCode] = useState('');
  const [foundStudent, setFoundStudent] = useState<StudentLookupByCodeResponse | null>(null);
  const [lookupError, setLookupError] = useState('');
  const [looking, setLooking] = useState(false);
  const [linking, setLinking] = useState(false);
  const [relationship, setRelationship] = useState('MOTHER');

  const { data: centers = [] } = useQuery<CenterOption[]>({
    queryKey: ['all-centers-parent'],
    queryFn: () => api.get('/api/v1/centers').then((r) => { const d = r.data; return Array.isArray(d) ? d : (d.content ?? []); }),
  });

  async function handleLookup() {
    const trimmed = code.trim();
    if (!trimmed) return;
    setLookupError('');
    setFoundStudent(null);
    setLooking(true);
    try {
      const res = await api.get(`/api/v1/students/link-code/${encodeURIComponent(trimmed)}`);
      setFoundStudent(res.data as StudentLookupByCodeResponse);
    } catch {
      setLookupError('Invalid verification code. Please ask your child or school for the correct code.');
    } finally {
      setLooking(false);
    }
  }

  async function handleLink() {
    if (!foundStudent) return;
    const centerId = centers[0]?.id ?? null;
    if (!centerId) {
      toast.error('No center found. Please contact support.');
      return;
    }
    setLinking(true);
    try {
      await api.post(`/api/v1/parents/${profileId}/students`, {
        studentId: foundStudent.id,
        studentName: `${foundStudent.firstName} ${foundStudent.lastName}`,
        centerId,
        relationship,
      });
      toast.success(`${foundStudent.firstName} linked to your account!`);
      onLinked();
      onClose();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { detail?: string; message?: string } }; message?: string };
      toast.error(err?.response?.data?.detail ?? err?.response?.data?.message ?? err?.message ?? 'Failed to link student.');
    } finally {
      setLinking(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        exit={{ opacity: 0, scale: 0.95 }}
        className="bg-surface-100 border border-white/10 rounded-2xl p-6 w-full max-w-md shadow-2xl"
      >
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-lg font-bold text-white">Link Your Child</h2>
          <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-white/5 text-white/40 hover:text-white/70 transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        <p className="text-sm text-white/50 mb-5">
          Enter the 6-digit verification code shown on your child's Student Profile page.
        </p>

        <div className="space-y-4">
          <div>
            <label className="text-xs font-semibold text-white/50 uppercase tracking-wider mb-2 block">
              <Key className="w-3.5 h-3.5 inline mr-1" />
              Child's Verification Code
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                value={code}
                maxLength={6}
                onChange={(e) => { setCode(e.target.value.replace(/\D/g, '')); setFoundStudent(null); setLookupError(''); }}
                onKeyDown={(e) => e.key === 'Enter' && handleLookup()}
                placeholder="e.g. 739251"
                className="flex-1 bg-white/5 border border-white/10 rounded-xl px-3 py-2.5 text-sm text-white placeholder-white/30 focus:outline-none focus:border-brand-500/50 tracking-widest text-center font-mono text-lg"
              />
              <button
                onClick={handleLookup}
                disabled={looking || code.length !== 6}
                className="btn-primary px-4 py-2.5 text-sm flex items-center gap-1.5 disabled:opacity-50"
              >
                {looking ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Find'}
              </button>
            </div>
            {lookupError && <p className="text-xs text-red-400 mt-2">{lookupError}</p>}
          </div>

          {foundStudent && (
            <motion.div
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              className="bg-brand-500/10 border border-brand-500/20 rounded-xl p-4 space-y-3"
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-brand-500/20 flex items-center justify-center flex-shrink-0">
                  <span className="text-brand-400 font-bold text-sm">
                    {foundStudent.firstName?.[0]}{foundStudent.lastName?.[0]}
                  </span>
                </div>
                <div>
                  <div className="font-semibold text-white">{foundStudent.firstName} {foundStudent.lastName}</div>
                  <div className="text-xs text-white/40">
                    {[foundStudent.currentClass && `Class ${foundStudent.currentClass}`, foundStudent.board, foundStudent.city].filter(Boolean).join(' · ')}
                  </div>
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-white/50 uppercase tracking-wider mb-1.5 block">Your relationship</label>
                <select
                  value={relationship}
                  onChange={(e) => setRelationship(e.target.value)}
                  className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2.5 text-sm text-white focus:outline-none focus:border-brand-500/50"
                >
                  {RELATIONSHIP_OPTIONS.map((o) => (
                    <option key={o.value} value={o.value}>{o.label}</option>
                  ))}
                </select>
              </div>
            </motion.div>
          )}

          <div className="flex gap-3 pt-2">
            <button onClick={onClose} className="flex-1 px-4 py-2.5 rounded-xl border border-white/10 text-sm text-white/60 hover:text-white hover:border-white/20 transition-colors">
              Cancel
            </button>
            <button
              onClick={handleLink}
              disabled={!foundStudent || linking}
              className="flex-1 btn-primary py-2.5 text-sm flex items-center justify-center gap-2 disabled:opacity-50"
            >
              {linking ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
              Link Child
            </button>
          </div>
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
      {/* Card header */}
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
            title={expanded ? 'Collapse' : 'Expand'}
          >
            {expanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
          </button>
          <button
            onClick={() => onRemove(link.id)}
            className="p-2 rounded-lg hover:bg-red-500/10 text-white/20 hover:text-red-400 transition-colors"
            title="Remove child"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Expanded school details */}
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
                      <Edit2 className="w-3.5 h-3.5" />
                      Edit
                    </button>
                  </div>
                  <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
                    <div>
                      <div className="flex items-center gap-1.5 text-xs text-white/40 mb-1">
                        <Calendar className="w-3 h-3" /> Date of Birth
                      </div>
                      <div className="text-sm text-white">{formatDate(link.dateOfBirth)}</div>
                    </div>
                    <div>
                      <div className="flex items-center gap-1.5 text-xs text-white/40 mb-1">
                        <School className="w-3 h-3" /> School Name
                      </div>
                      <div className="text-sm text-white">{link.schoolName || '—'}</div>
                    </div>
                    <div>
                      <div className="flex items-center gap-1.5 text-xs text-white/40 mb-1">
                        <BookOpen className="w-3 h-3" /> Standard / Class
                      </div>
                      <div className="text-sm text-white">{link.standard || '—'}</div>
                    </div>
                    <div>
                      <div className="flex items-center gap-1.5 text-xs text-white/40 mb-1">
                        <BookOpen className="w-3 h-3" /> Board
                      </div>
                      <div className="text-sm text-white">{link.board || '—'}</div>
                    </div>
                    <div>
                      <div className="flex items-center gap-1.5 text-xs text-white/40 mb-1">
                        <Hash className="w-3 h-3" /> Roll Number
                      </div>
                      <div className="text-sm text-white">{link.rollNumber || '—'}</div>
                    </div>
                  </div>
                </>
              ) : (
                <form
                  onSubmit={(e) => { e.preventDefault(); editMutation.mutate(form); }}
                  className="space-y-4"
                >
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
                    <button
                      type="button"
                      onClick={() => setEditing(false)}
                      className="flex items-center gap-1.5 px-4 py-2.5 rounded-xl border border-white/10 text-sm text-white/60 hover:text-white hover:border-white/20 transition-colors"
                    >
                      <X className="w-4 h-4" /> Cancel
                    </button>
                    <button
                      type="submit"
                      disabled={editMutation.isPending}
                      className="btn-primary flex items-center gap-1.5 px-5 py-2.5 text-sm font-medium disabled:opacity-50"
                    >
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
  const [showLinkModal, setShowLinkModal] = useState(false);

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
    mutationFn: (linkId: string) =>
      api.delete(`/api/v1/parents/${profile!.id}/students/${linkId}`),
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
      {/* Header */}
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">My Children</h1>
          <p className="text-white/50 text-sm mt-0.5">
            Manage your linked children and their school details.
          </p>
        </div>
        {profile && (
          <button
            onClick={() => setShowLinkModal(true)}
            className="btn-primary flex items-center gap-2 px-4 py-2.5 text-sm font-medium"
          >
            <Plus className="w-4 h-4" />
            Link Child
          </button>
        )}
      </div>

      {/* Summary badge */}
      {linkedStudents.length > 0 && (
        <div className="flex items-center gap-2 text-sm text-white/50">
          <Users className="w-4 h-4" />
          {linkedStudents.length} child{linkedStudents.length !== 1 ? 'ren' : ''} linked
        </div>
      )}

      {/* Children list */}
      {linkedStudents.length === 0 ? (
        <div className="card text-center py-16">
          <Users className="w-12 h-12 text-white/15 mx-auto mb-4" />
          <h3 className="text-white/50 font-medium mb-1">No children linked yet</h3>
          <p className="text-white/30 text-sm mb-6">
            Link your child's account to monitor their progress and manage school details.
          </p>
          {profile && (
            <button
              onClick={() => setShowLinkModal(true)}
              className="btn-primary inline-flex items-center gap-2 px-5 py-2.5 text-sm"
            >
              <Plus className="w-4 h-4" />
              Link Your First Child
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

      {/* Link Child Modal */}
      <AnimatePresence>
        {showLinkModal && profile && (
          <LinkChildModal
            profileId={profile.id}
            onClose={() => setShowLinkModal(false)}
            onLinked={() => queryClient.invalidateQueries({ queryKey: ['linked-students'] })}
          />
        )}
      </AnimatePresence>
    </div>
  );
}
