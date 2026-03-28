import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import { CalendarCheck, Users, AlertTriangle, ChevronDown, BarChart3 } from 'lucide-react';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { ExportMenu } from '../../components/ui/ExportMenu';

interface Batch { id: string; name: string; code: string; }
interface AttendanceSummary {
  studentId: string; studentName: string;
  totalDays: number; presentDays: number; absentDays: number;
  lateDays: number; excusedDays: number; attendancePct: number;
}

export default function AdminAttendancePage() {
  const { user } = useAuthStore();
  const centerId = user?.centerId;
  const [selectedBatchId, setSelectedBatchId] = useState<string>('');
  const [lowOnly, setLowOnly] = useState(false);

  const { data: batches = [] } = useQuery<Batch[]>({
    queryKey: ['batches', centerId],
    queryFn: () => api.get(`/api/v1/centers/${centerId}/batches`).then(r =>
      Array.isArray(r.data) ? r.data : (r.data.content ?? [])),
    enabled: !!centerId,
  });

  const { data: summary = [], isLoading } = useQuery<AttendanceSummary[]>({
    queryKey: ['attendance-summary-admin', centerId, selectedBatchId],
    queryFn: () =>
      api.get(`/api/v1/centers/${centerId}/batches/${selectedBatchId}/attendance/summary`)
        .then(r => Array.isArray(r.data) ? r.data : (r.data.content ?? [])),
    enabled: !!centerId && !!selectedBatchId,
  });

  const displayed = lowOnly ? summary.filter(s => s.attendancePct < 75) : summary;

  const avg = summary.length
    ? summary.reduce((acc, s) => acc + s.attendancePct, 0) / summary.length
    : 0;
  const lowCount = summary.filter(s => s.attendancePct < 75).length;
  const fullCount = summary.filter(s => s.attendancePct >= 90).length;

  return (
    <div className="p-6 space-y-6 max-w-5xl mx-auto">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-brand-500/10 border border-brand-500/20">
            <CalendarCheck className="w-5 h-5 text-brand-400" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-white">Attendance Reports</h1>
            <p className="text-sm text-white/50">View student attendance across batches</p>
          </div>
        </div>
        {displayed.length > 0 && (
          <ExportMenu
            csvData={displayed.map(s => ({
              Student: s.studentName,
              Attendance: `${s.attendancePct}%`,
              Present: s.presentDays,
              Absent: s.absentDays,
              Late: s.lateDays,
              Excused: s.excusedDays,
              Total: s.totalDays,
            }))}
            csvFilename="attendance-report"
          />
        )}
      </div>

      {/* Batch selector */}
      <div className="relative max-w-sm">
        <select
          value={selectedBatchId}
          onChange={e => setSelectedBatchId(e.target.value)}
          className="w-full appearance-none bg-surface-100 border border-white/10 rounded-xl px-4 py-2.5 text-white text-sm focus:outline-none focus:border-brand-500/50 pr-10"
        >
          <option value="">Select a batch…</option>
          {batches.map(b => (
            <option key={b.id} value={b.id}>{b.name} ({b.code})</option>
          ))}
        </select>
        <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/40 pointer-events-none" />
      </div>

      {!selectedBatchId ? (
        <div className="flex flex-col items-center gap-3 py-16 text-white/30">
          <BarChart3 className="w-10 h-10" />
          <p>Select a batch to view attendance reports</p>
        </div>
      ) : isLoading ? (
        <div className="text-center py-16 text-white/40">Loading attendance data…</div>
      ) : summary.length === 0 ? (
        <div className="flex flex-col items-center gap-3 py-16 text-white/30">
          <Users className="w-10 h-10" />
          <p>No attendance data found for this batch.</p>
          <p className="text-xs">Add students and mark attendance first.</p>
        </div>
      ) : (
        <>
          {/* Stats row */}
          <div className="grid grid-cols-3 gap-4">
            {[
              { label: 'Avg Attendance', value: `${avg.toFixed(1)}%`, color: avg >= 75 ? 'text-emerald-400' : 'text-red-400' },
              { label: 'At Risk (<75%)',  value: lowCount,            color: lowCount > 0 ? 'text-amber-400' : 'text-emerald-400' },
              { label: 'Excellent (≥90%)',value: fullCount,           color: 'text-brand-400' },
            ].map(s => (
              <div key={s.label} className="bg-surface-50/40 border border-white/5 rounded-xl p-4 text-center">
                <div className={`text-2xl font-bold ${s.color}`}>{s.value}</div>
                <div className="text-xs text-white/40 mt-1">{s.label}</div>
              </div>
            ))}
          </div>

          {/* Filter toggle */}
          <div className="flex items-center gap-3">
            <button
              onClick={() => setLowOnly(false)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${!lowOnly ? 'bg-brand-600 text-white' : 'text-white/40 hover:text-white hover:bg-white/5'}`}
            >
              All Students ({summary.length})
            </button>
            <button
              onClick={() => setLowOnly(true)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${lowOnly ? 'bg-amber-500/20 text-amber-400 border border-amber-500/20' : 'text-white/40 hover:text-white hover:bg-white/5'}`}
            >
              <AlertTriangle className="w-3.5 h-3.5" />
              At Risk ({lowCount})
            </button>
          </div>

          {/* Student table */}
          <div className="bg-surface-50/40 border border-white/5 rounded-2xl overflow-hidden">
            <div className="grid grid-cols-[1fr_auto_auto_auto_auto_auto] gap-x-4 px-5 py-3 border-b border-white/5 text-xs text-white/30 font-medium uppercase tracking-wide">
              <span>Student</span>
              <span className="text-right w-16">Total</span>
              <span className="text-right w-16">Present</span>
              <span className="text-right w-16">Absent</span>
              <span className="text-right w-16">Late</span>
              <span className="text-right w-20">Attendance</span>
            </div>
            <div className="divide-y divide-white/5">
              {displayed.map((s, i) => {
                const pct = Number(s.attendancePct);
                const pctColor = pct >= 90 ? 'text-emerald-400' : pct >= 75 ? 'text-amber-400' : 'text-red-400';
                const barColor = pct >= 90 ? 'bg-emerald-500' : pct >= 75 ? 'bg-amber-500' : 'bg-red-500';
                return (
                  <motion.div
                    key={s.studentId}
                    initial={{ opacity: 0, x: -4 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: i * 0.03 }}
                    className="grid grid-cols-[1fr_auto_auto_auto_auto_auto] gap-x-4 px-5 py-3.5 items-center"
                  >
                    <div>
                      <div className="text-sm font-medium text-white">{s.studentName}</div>
                      {pct < 75 && (
                        <div className="flex items-center gap-1 mt-0.5 text-xs text-amber-400">
                          <AlertTriangle className="w-3 h-3" /> Below threshold
                        </div>
                      )}
                    </div>
                    <span className="w-16 text-right text-sm text-white/50">{s.totalDays}</span>
                    <span className="w-16 text-right text-sm text-emerald-400">{s.presentDays}</span>
                    <span className="w-16 text-right text-sm text-red-400">{s.absentDays}</span>
                    <span className="w-16 text-right text-sm text-amber-400">{s.lateDays}</span>
                    <div className="w-20">
                      <div className={`text-sm font-semibold ${pctColor} text-right`}>{pct.toFixed(1)}%</div>
                      <div className="mt-1 h-1 bg-white/10 rounded-full overflow-hidden">
                        <div className={`h-full rounded-full ${barColor}`} style={{ width: `${Math.min(pct, 100)}%` }} />
                      </div>
                    </div>
                  </motion.div>
                );
              })}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
