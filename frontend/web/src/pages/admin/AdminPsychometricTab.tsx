// src/pages/admin/AdminPsychometricTab.tsx
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Search, Brain, CheckCircle2, Loader2, UserCheck, AlertCircle } from 'lucide-react';
import { toast } from 'sonner';
import { useAuthStore } from '../../stores/authStore';
import api from '../../lib/api';
import { cn } from '../../lib/utils';

interface StudentLookup {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  board?: string;
  currentClass?: number;
}

interface PsychProfile {
  id: string;
  studentId: string;
  centerId: string;
  batchId: string;
  status: string;
  openness: number;
  conscientiousness: number;
  extraversion: number;
  agreeableness: number;
  neuroticism: number;
  riasecCode: string | null;
  createdAt: string;
}

interface Batch {
  id: string;
  name: string;
  code: string;
  status: string;
}

export default function AdminPsychometricTab({ centerId: centerIdProp }: { centerId?: string }) {
  const storeCenterId = useAuthStore(s => s.user?.centerId);
  const centerId = centerIdProp || storeCenterId;
  const token = useAuthStore(s => s.token);
  const qc = useQueryClient();

  const [email, setEmail] = useState('');
  const [foundStudent, setFoundStudent] = useState<(StudentLookup & { userId: string }) | null>(null);
  const [lookupError, setLookupError] = useState('');
  const [isLooking, setIsLooking] = useState(false);
  const [selectedBatchId, setSelectedBatchId] = useState('');

  // Existing profiles for this center
  const { data: profiles = [], isLoading: loadingProfiles } = useQuery<PsychProfile[]>({
    queryKey: ['psych-profiles', centerId],
    queryFn: () =>
      api.get(`/api/v1/psych/profiles?centerId=${centerId}`).then(r => r.data),
    enabled: !!centerId,
  });

  // Batches for this center
  const { data: batches = [] } = useQuery<Batch[]>({
    queryKey: ['batches', centerId],
    queryFn: () =>
      api.get(`/api/v1/centers/${centerId}/batches`).then(r => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
    enabled: !!centerId,
  });

  async function handleLookup() {
    if (!email.trim()) return;
    setIsLooking(true);
    setFoundStudent(null);
    setLookupError('');
    try {
      // Step 1: lookup by email → get profile ID
      const lookupRes = await api.get(`/api/v1/students/lookup?email=${encodeURIComponent(email.trim())}`);
      const lookup: StudentLookup = lookupRes.data;

      // Step 2: get full profile to get userId (needed as studentId for psych)
      const profileRes = await api.get(`/api/v1/students/${lookup.id}`);
      const userId: string = profileRes.data.userId;

      setFoundStudent({ ...lookup, userId });
    } catch {
      setLookupError('No student found with that email. Make sure they have registered and their profile exists.');
    } finally {
      setIsLooking(false);
    }
  }

  const activateMutation = useMutation({
    mutationFn: () =>
      api.post('/api/v1/psych/profiles', {
        studentId: foundStudent!.userId,
        centerId,
        batchId: selectedBatchId,
      }),
    onSuccess: () => {
      toast.success(`Profile activated for ${foundStudent!.firstName} ${foundStudent!.lastName}`);
      setEmail('');
      setFoundStudent(null);
      setSelectedBatchId('');
      qc.invalidateQueries({ queryKey: ['psych-profiles', centerId] });
    },
    onError: (err: unknown) => {
      const e = err as { response?: { data?: { detail?: string; message?: string } } };
      toast.error(e.response?.data?.detail ?? e.response?.data?.message ?? 'Activation failed');
    },
  });

  const alreadyActivated = foundStudent
    ? profiles.some(p => p.studentId === foundStudent.userId)
    : false;

  const activated = profiles.filter(p => p.status === 'ACTIVE').length;
  const assessed = profiles.filter(p => p.riasecCode !== null).length;

  return (
    <div className="p-6 max-w-4xl mx-auto space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-xl font-bold text-white flex items-center gap-2">
          <Brain className="w-5 h-5 text-brand-400" /> Psychometric Profiles
        </h1>
        <p className="text-sm text-white/40 mt-1">
          Activate psychometric assessments for students in your centre.
        </p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-4">
        {[
          { label: 'Total Activated', value: loadingProfiles ? '…' : activated, color: 'text-emerald-400' },
          { label: 'Assessed', value: loadingProfiles ? '…' : assessed, color: 'text-brand-400' },
          { label: 'Pending Assessment', value: loadingProfiles ? '…' : activated - assessed, color: 'text-amber-400' },
        ].map(s => (
          <div key={s.label} className="bg-surface-100/40 border border-white/8 rounded-xl p-4">
            <div className={cn('text-2xl font-bold', s.color)}>{s.value}</div>
            <div className="text-xs text-white/40 mt-1">{s.label}</div>
          </div>
        ))}
      </div>

      {/* Activate new profile */}
      <div className="bg-surface-100/40 border border-white/8 rounded-2xl p-6 space-y-4">
        <h2 className="text-sm font-semibold text-white/80">Activate Profile for a Student</h2>

        {/* Email lookup */}
        <div className="flex gap-2">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
            <input
              type="email"
              value={email}
              onChange={e => { setEmail(e.target.value); setFoundStudent(null); setLookupError(''); }}
              onKeyDown={e => e.key === 'Enter' && handleLookup()}
              placeholder="Student email address…"
              className="w-full pl-9 pr-4 py-2.5 bg-white/5 border border-white/10 rounded-xl text-sm text-white placeholder-white/30 focus:outline-none focus:border-brand-500/50"
            />
          </div>
          <button
            onClick={handleLookup}
            disabled={isLooking || !email.trim()}
            className="flex items-center gap-1.5 px-4 py-2.5 rounded-xl text-sm font-medium bg-brand-600 hover:bg-brand-500 text-white disabled:opacity-40 transition-colors"
          >
            {isLooking ? <Loader2 className="w-4 h-4 animate-spin" /> : <Search className="w-4 h-4" />}
            Find
          </button>
        </div>

        {/* Lookup error */}
        {lookupError && (
          <div className="flex items-start gap-2 text-sm text-red-400 bg-red-500/8 border border-red-500/15 rounded-xl px-4 py-3">
            <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
            {lookupError}
          </div>
        )}

        {/* Found student */}
        {foundStudent && (
          <div className="border border-white/8 rounded-xl p-4 space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-full bg-brand-500/20 border border-brand-500/30 flex items-center justify-center text-sm font-bold text-brand-300">
                {foundStudent.firstName[0]}{foundStudent.lastName[0]}
              </div>
              <div>
                <p className="text-sm font-semibold text-white">{foundStudent.firstName} {foundStudent.lastName}</p>
                <p className="text-xs text-white/40">{foundStudent.email}</p>
                {foundStudent.board && (
                  <p className="text-xs text-white/30">{foundStudent.board} · Class {foundStudent.currentClass}</p>
                )}
              </div>
              {alreadyActivated && (
                <span className="ml-auto flex items-center gap-1 text-xs text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 px-2.5 py-1 rounded-full">
                  <CheckCircle2 className="w-3.5 h-3.5" /> Already Activated
                </span>
              )}
            </div>

            {!alreadyActivated && (
              <>
                {/* Batch select */}
                <div>
                  <label className="text-xs text-white/50 mb-1.5 block">Select Batch</label>
                  <select
                    value={selectedBatchId}
                    onChange={e => setSelectedBatchId(e.target.value)}
                    className="w-full px-3 py-2.5 bg-white/5 border border-white/10 rounded-xl text-sm text-white focus:outline-none focus:border-brand-500/50"
                  >
                    <option value="">— choose a batch —</option>
                    {batches.map(b => (
                      <option key={b.id} value={b.id}>{b.name} ({b.code})</option>
                    ))}
                  </select>
                </div>

                <button
                  onClick={() => activateMutation.mutate()}
                  disabled={!selectedBatchId || activateMutation.isPending}
                  className="flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-semibold bg-emerald-600 hover:bg-emerald-500 text-white disabled:opacity-40 transition-colors"
                >
                  {activateMutation.isPending
                    ? <Loader2 className="w-4 h-4 animate-spin" />
                    : <UserCheck className="w-4 h-4" />}
                  Activate Psychometric Profile
                </button>
              </>
            )}
          </div>
        )}
      </div>

      {/* Existing profiles list */}
      {profiles.length > 0 && (
        <div className="bg-surface-100/40 border border-white/8 rounded-2xl overflow-hidden">
          <div className="px-5 py-3 border-b border-white/8">
            <p className="text-sm font-semibold text-white/70">Activated Profiles</p>
          </div>
          <div className="divide-y divide-white/5">
            {profiles.map(p => (
              <div key={p.id} className="px-5 py-3 flex items-center gap-3">
                <div className="w-2 h-2 rounded-full bg-emerald-400 flex-shrink-0" />
                <div className="flex-1 min-w-0">
                  <p className="text-xs text-white/50 font-mono truncate">{p.studentId}</p>
                  <p className="text-xs text-white/30 mt-0.5">
                    {p.riasecCode ? `RIASEC: ${p.riasecCode}` : 'Not yet assessed'}
                    {' · '}Activated {new Date(p.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}
                  </p>
                </div>
                <span className={cn(
                  'text-xs px-2 py-0.5 rounded-full border',
                  p.riasecCode
                    ? 'text-brand-400 bg-brand-500/10 border-brand-500/20'
                    : 'text-white/40 bg-white/5 border-white/10'
                )}>
                  {p.riasecCode ? 'Assessed' : 'Pending'}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
