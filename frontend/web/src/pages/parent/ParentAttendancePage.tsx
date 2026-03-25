import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import { CalendarCheck, AlertTriangle, Users } from 'lucide-react';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';

interface StudentLink {
  studentId: string;
  studentName?: string;
  batchId?: string;
  centerId?: string;
}

interface AttendanceSummary {
  studentId: string;
  studentName: string;
  totalDays: number;
  presentDays: number;
  absentDays: number;
  lateDays: number;
  excusedDays: number;
  attendancePct: number;
}

function getPctColor(pct: number) {
  if (pct >= 85) return '#10b981';
  if (pct >= 75) return '#f59e0b';
  return '#ef4444';
}

function AttendanceCard({ link }: { link: StudentLink }) {
  const { data: summary, isLoading } = useQuery<AttendanceSummary | undefined>({
    queryKey: ['attendance-summary', link.centerId, link.batchId, link.studentId],
    queryFn: () =>
      api.get(`/api/v1/centers/${link.centerId}/batches/${link.batchId}/attendance/summary`)
        .then(r => {
          const list: AttendanceSummary[] = Array.isArray(r.data) ? r.data : (r.data.content ?? []);
          return list.find(s => s.studentId === link.studentId);
        }),
    enabled: !!link.centerId && !!link.batchId,
  });

  if (isLoading) return <div className="bg-surface-50/40 border border-white/5 rounded-2xl p-6 text-white/40 text-sm">Loading...</div>;
  if (!summary) return (
    <div className="bg-surface-50/40 border border-white/5 rounded-2xl p-6">
      <div className="text-sm font-semibold text-white">{link.studentName ?? 'Student'}</div>
      <div className="text-xs text-white/40 mt-1">No attendance data yet</div>
    </div>
  );

  const pct = summary.attendancePct ?? 0;
  const circ = 2 * Math.PI * 30;
  const offset = circ * (1 - pct / 100);

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-surface-50/40 border border-white/5 rounded-2xl p-6 space-y-4"
    >
      <div className="flex items-center gap-3">
        <div className="w-9 h-9 rounded-full bg-brand-500/10 border border-brand-500/20 flex items-center justify-center text-brand-400 font-bold">
          {(summary.studentName ?? link.studentName ?? 'S')[0].toUpperCase()}
        </div>
        <div>
          <div className="text-sm font-semibold text-white">{summary.studentName ?? link.studentName}</div>
          <div className="text-xs text-white/40">Attendance overview</div>
        </div>
      </div>

      <div className="flex items-center gap-6">
        <svg width="80" height="80" viewBox="0 0 70 70" className="-rotate-90">
          <circle cx="35" cy="35" r="30" fill="none" stroke="rgba(255,255,255,0.08)" strokeWidth="6" />
          <circle
            cx="35" cy="35" r="30" fill="none"
            stroke={getPctColor(pct)}
            strokeWidth="6"
            strokeLinecap="round"
            strokeDasharray={circ}
            strokeDashoffset={offset}
            style={{ transition: 'stroke-dashoffset 0.6s ease' }}
          />
        </svg>
        <div>
          <div className="text-3xl font-extrabold text-white">{pct}%</div>
          <div className="text-xs text-white/40">Attendance rate</div>
          {pct < 75 && (
            <div className="flex items-center gap-1 mt-1 text-red-400 text-xs">
              <AlertTriangle className="w-3 h-3" />
              <span>Below 75%</span>
            </div>
          )}
        </div>
      </div>

      <div className="grid grid-cols-4 gap-2">
        {[
          { label: 'Present', value: summary.presentDays, color: 'text-emerald-400' },
          { label: 'Absent',  value: summary.absentDays,  color: 'text-red-400' },
          { label: 'Late',    value: summary.lateDays,    color: 'text-amber-400' },
          { label: 'Excused', value: summary.excusedDays, color: 'text-brand-400' },
        ].map(stat => (
          <div key={stat.label} className="text-center">
            <div className={`text-lg font-bold ${stat.color}`}>{stat.value}</div>
            <div className="text-[10px] text-white/40">{stat.label}</div>
          </div>
        ))}
      </div>
    </motion.div>
  );
}

export default function ParentAttendancePage() {
  const { user } = useAuthStore();

  const { data: parentProfile } = useQuery({
    queryKey: ['parent-profile'],
    queryFn: () => api.get('/api/v1/parents/me').then(r => r.data),
    enabled: !!user,
    staleTime: 5 * 60 * 1000,
  });

  const { data: children = [], isLoading } = useQuery<StudentLink[]>({
    queryKey: ['parent-children', parentProfile?.id],
    queryFn: () =>
      api.get(`/api/v1/parents/${parentProfile?.id}/students?size=50`).then(r =>
        Array.isArray(r.data) ? r.data : (r.data.content ?? [])),
    enabled: !!parentProfile?.id,
  });

  return (
    <div className="p-6 space-y-6 max-w-3xl mx-auto">
      <div className="flex items-center gap-3">
        <div className="p-2 rounded-xl bg-brand-500/10 border border-brand-500/20">
          <CalendarCheck className="w-5 h-5 text-brand-400" />
        </div>
        <div>
          <h1 className="text-xl font-bold text-white">Children's Attendance</h1>
          <p className="text-sm text-white/50">Track attendance rates across all your children</p>
        </div>
      </div>

      {isLoading ? (
        <div className="text-center py-16 text-white/40">Loading...</div>
      ) : children.length === 0 ? (
        <div className="flex flex-col items-center gap-3 py-16 text-white/40">
          <Users className="w-10 h-10" />
          <p>No children linked yet. Go to My Children to link.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {children.map(child => (
            <AttendanceCard key={child.studentId} link={child} />
          ))}
        </div>
      )}
    </div>
  );
}
