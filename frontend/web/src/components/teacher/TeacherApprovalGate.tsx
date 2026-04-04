// src/components/teacher/TeacherApprovalGate.tsx
import { ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Loader2, Clock, LogOut, ShieldAlert, MailOpen, UserX } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';

interface TeacherProfile {
  id: string;
  status: 'ACTIVE' | 'INACTIVE' | 'PENDING_APPROVAL' | 'INVITATION_SENT';
  firstName: string;
  lastName: string;
  centerId: string;
}

interface Center {
  id: string;
  name: string;
  city?: string;
}

// ─── Shared sign-out button ───────────────────────────────────────────────────
function SignOutButton() {
  const { logout } = useAuthStore();
  const navigate = useNavigate();
  return (
    <button
      onClick={() => { logout(); navigate('/login'); }}
      className="flex items-center gap-2 px-4 py-2 text-sm text-white/40 hover:text-white border border-white/10 hover:border-white/25 rounded-xl transition-colors"
    >
      <LogOut className="w-4 h-4" />
      Sign out
    </button>
  );
}

// ─── Holding pages ────────────────────────────────────────────────────────────
function HoldingPage({
  icon, iconColor, title, subtitle, children,
}: {
  icon: ReactNode;
  iconColor: string;
  title: string;
  subtitle: string;
  children?: ReactNode;
}) {
  return (
    <div className="min-h-screen bg-[#0a0a12] flex flex-col items-center justify-center gap-6 p-6">
      <div className={`p-4 rounded-2xl border ${iconColor}`}>{icon}</div>
      <div className="text-center space-y-2 max-w-sm">
        <h1 className="text-xl font-bold text-white">{title}</h1>
        <p className="text-sm text-white/50 leading-relaxed">{subtitle}</p>
      </div>
      {children}
      <SignOutButton />
    </div>
  );
}

// ─── Status-specific pages ────────────────────────────────────────────────────
function PendingApprovalPage({ firstName, centerName }: { firstName: string; centerName?: string }) {
  return (
    <HoldingPage
      icon={<Clock className="w-8 h-8 text-amber-400" />}
      iconColor="bg-amber-500/10 border-amber-500/25"
      title="Awaiting Approval"
      subtitle={`Hi ${firstName}, your registration${centerName ? ` at ${centerName}` : ''} is pending review by the institution coordinator. You'll get full access once approved.`}
    >
      <div className="flex items-center gap-2 px-4 py-2 bg-amber-500/10 border border-amber-500/20 rounded-xl text-xs text-amber-300">
        <Clock className="w-3.5 h-3.5 flex-shrink-0" />
        The coordinator will review your application shortly
      </div>
    </HoldingPage>
  );
}

function InactiveAccountPage({ firstName }: { firstName: string }) {
  return (
    <HoldingPage
      icon={<UserX className="w-8 h-8 text-red-400" />}
      iconColor="bg-red-500/10 border-red-500/25"
      title="Account Deactivated"
      subtitle={`Hi ${firstName}, your teacher account has been deactivated. Please contact your institution coordinator to restore access.`}
    />
  );
}

function InvitationPendingPage({ firstName }: { firstName: string }) {
  return (
    <HoldingPage
      icon={<MailOpen className="w-8 h-8 text-brand-400" />}
      iconColor="bg-brand-500/10 border-brand-500/25"
      title="Accept Your Invitation"
      subtitle={`Hi ${firstName}, your account was pre-created by your institution. Please check your email and click the invitation link to complete your setup.`}
    />
  );
}

function NoCenterPage() {
  return (
    <HoldingPage
      icon={<ShieldAlert className="w-8 h-8 text-white/40" />}
      iconColor="bg-white/5 border-white/10"
      title="No Institution Linked"
      subtitle="Your account isn't linked to any institution yet. Please contact your coordinator or re-register and select your institution."
    />
  );
}

// ─── Gate ─────────────────────────────────────────────────────────────────────
export default function TeacherApprovalGate({ children }: { children: ReactNode }) {
  const { user } = useAuthStore();

  // Step 1: resolve centerId — same pattern used across all teacher pages (Fix #170)
  const { data: centers = [], isLoading: centersLoading } = useQuery<Center[]>({
    queryKey: ['teacher-gate-centers'],
    queryFn: () =>
      api.get('/api/v1/centers').then(r => (Array.isArray(r.data) ? r.data : (r.data.content ?? []))),
    enabled: !!user,
    staleTime: 60_000,
  });

  const centerId = centers[0]?.id;
  const centerName = centers[0]?.name;

  // Step 2: fetch teacher's own center profile — contains approval status
  const {
    data: teacherProfile,
    isLoading: profileLoading,
    isError,
  } = useQuery<TeacherProfile>({
    queryKey: ['teacher-gate-profile', centerId],
    queryFn: () =>
      api.get(`/api/v1/centers/${centerId}/teachers/me`).then(r => r.data),
    enabled: !!centerId,
    staleTime: 30_000,   // short TTL — reflects approval quickly without re-login
    retry: false,        // don't retry 404 (orphaned teacher — no self-register record)
  });

  // ── Loading: hold render so no center data flashes through ──────────────────
  if (centersLoading || (!!centerId && profileLoading)) {
    return (
      <div className="min-h-screen bg-[#0a0a12] flex items-center justify-center">
        <Loader2 className="w-8 h-8 animate-spin text-brand-400" />
      </div>
    );
  }

  // ── No center in centers list (orphaned — no center selected at registration) ─
  if (!centerId) {
    return <NoCenterPage />;
  }

  // ── API error or 404 (self-register was never completed) ──────────────────
  if (isError || !teacherProfile) {
    return <NoCenterPage />;
  }

  // ── Blocked statuses ──────────────────────────────────────────────────────
  if (teacherProfile.status === 'PENDING_APPROVAL') {
    return <PendingApprovalPage firstName={teacherProfile.firstName} centerName={centerName} />;
  }
  if (teacherProfile.status === 'INACTIVE') {
    return <InactiveAccountPage firstName={teacherProfile.firstName} />;
  }
  if (teacherProfile.status === 'INVITATION_SENT') {
    return <InvitationPendingPage firstName={teacherProfile.firstName} />;
  }

  // ── ACTIVE — full access ───────────────────────────────────────────────────
  return <>{children}</>;
}
