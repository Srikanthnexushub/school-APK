import { useQuery } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import { CalendarCheck, TrendingUp, AlertTriangle } from 'lucide-react';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { ExportMenu } from '../../components/ui/ExportMenu';

interface AttendanceSummary {
  batchId: string;
  studentId: string;
  studentName: string;
  totalDays: number;
  presentDays: number;
  absentDays: number;
  lateDays: number;
  excusedDays: number;
  attendancePct: number;
}

interface StudentProfile {
  centerId?: string;
  batchName?: string;
}

interface Enrollment { centerId: string; batchId: string; }

const STATUS_COLORS: Record<string, string> = {
  good:    'text-emerald-400',
  warning: 'text-amber-400',
  danger:  'text-red-400',
};

function getPctStatus(pct: number): 'good' | 'warning' | 'danger' {
  if (pct >= 85) return 'good';
  if (pct >= 75) return 'warning';
  return 'danger';
}

export default function StudentAttendancePage() {
  const { user } = useAuthStore();

  const { data: profile } = useQuery<StudentProfile>({
    queryKey: ['student-profile-me'],
    queryFn: () => api.get('/api/v1/students/me').then(r => r.data),
    enabled: !!user,
    staleTime: 5 * 60 * 1000,
    retry: false,
  });

  const { data: enrollment } = useQuery<Enrollment | null>({
    queryKey: ['student-enrollment-me'],
    queryFn: () =>
      api.get('/api/v1/centers/student-enrollment/me')
        .then(r => r.data)
        .catch(() => null),
    enabled: !!user,
    staleTime: 5 * 60 * 1000,
  });

  const centerId = enrollment?.centerId;
  const batchId = enrollment?.batchId;

  const { data: summary, isLoading } = useQuery<AttendanceSummary>({
    queryKey: ['my-attendance', centerId, batchId],
    queryFn: () => api.get(`/api/v1/centers/${centerId}/batches/${batchId}/attendance/my-summary`).then(r => r.data),
    enabled: !!centerId && !!batchId,
  });

  const pct = summary?.attendancePct ?? 0;
  const status = getPctStatus(pct);
  const circ = 2 * Math.PI * 40;
  const offset = circ * (1 - pct / 100);

  return (
    <div className="p-6 space-y-6 max-w-2xl mx-auto">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-brand-500/10 border border-brand-500/20">
            <CalendarCheck className="w-5 h-5 text-brand-400" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-white">My Attendance</h1>
            <p className="text-sm text-white/50">{profile?.batchName ?? 'Current batch'}</p>
          </div>
        </div>
        {summary && (
          <ExportMenu
            csvData={[{
              Attendance: `${summary.attendancePct}%`,
              Present: summary.presentDays,
              Absent: summary.absentDays,
              Late: summary.lateDays,
              Excused: summary.excusedDays,
              Total: summary.totalDays,
            }]}
            csvFilename="attendance-summary"
          />
        )}
      </div>

      {isLoading ? (
        <div className="text-center py-16 text-white/40">Loading attendance...</div>
      ) : !summary ? (
        <div className="text-center py-16 text-white/40">
          <CalendarCheck className="w-12 h-12 mx-auto mb-3 opacity-30" />
          <p>No attendance data available yet.</p>
        </div>
      ) : (
        <>
          {/* Attendance ring */}
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-surface-50/40 border border-white/5 rounded-2xl p-8 flex flex-col items-center gap-4"
          >
            <svg width="120" height="120" viewBox="0 0 100 100" className="-rotate-90">
              <circle cx="50" cy="50" r="40" fill="none" stroke="rgba(255,255,255,0.08)" strokeWidth="8" />
              <circle
                cx="50" cy="50" r="40" fill="none"
                stroke={status === 'good' ? '#10b981' : status === 'warning' ? '#f59e0b' : '#ef4444'}
                strokeWidth="8"
                strokeLinecap="round"
                strokeDasharray={circ}
                strokeDashoffset={offset}
                style={{ transition: 'stroke-dashoffset 0.6s ease' }}
              />
            </svg>
            <div className="text-center -mt-2">
              <div className={`text-4xl font-extrabold ${STATUS_COLORS[status]}`}>{pct}%</div>
              <div className="text-sm text-white/50 mt-1">Overall Attendance</div>
            </div>

            {pct < 75 && (
              <div className="flex items-center gap-2 px-4 py-2 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 text-sm">
                <AlertTriangle className="w-4 h-4 flex-shrink-0" />
                <span>Attendance below 75% — at risk of academic penalty</span>
              </div>
            )}
          </motion.div>

          {/* Stat cards */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {[
              { label: 'Present', value: summary.presentDays, color: 'text-emerald-400' },
              { label: 'Absent',  value: summary.absentDays,  color: 'text-red-400' },
              { label: 'Late',    value: summary.lateDays,    color: 'text-amber-400' },
              { label: 'Excused', value: summary.excusedDays, color: 'text-brand-400' },
            ].map(stat => (
              <motion.div
                key={stat.label}
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                className="bg-surface-50/40 border border-white/5 rounded-xl p-4 text-center"
              >
                <div className={`text-2xl font-bold ${stat.color}`}>{stat.value}</div>
                <div className="text-xs text-white/40 mt-1">{stat.label}</div>
              </motion.div>
            ))}
          </div>

          <div className="bg-surface-50/40 border border-white/5 rounded-xl p-4 flex items-center gap-3">
            <TrendingUp className="w-4 h-4 text-white/40" />
            <span className="text-sm text-white/60">
              Total class days recorded: <span className="text-white font-semibold">{summary.totalDays}</span>
            </span>
          </div>
        </>
      )}
    </div>
  );
}
