import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import { CalendarCheck, Users, AlertTriangle, ChevronDown, BarChart3, ClipboardCheck, Calendar } from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { ExportMenu } from '../../components/ui/ExportMenu';

interface Batch { id: string; name: string; code: string; }
interface AttendanceSummary {
  studentId: string; studentName: string;
  totalDays: number; presentDays: number; absentDays: number;
  lateDays: number; excusedDays: number; attendancePct: number;
}
interface BatchMember { id: string; studentId: string; studentName: string; }
type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSED';

const STATUS_OPTIONS: { value: AttendanceStatus; label: string; color: string }[] = [
  { value: 'PRESENT', label: 'Present', color: 'bg-emerald-500/20 text-emerald-400 ring-emerald-500' },
  { value: 'ABSENT',  label: 'Absent',  color: 'bg-red-500/20 text-red-400 ring-red-500' },
  { value: 'LATE',    label: 'Late',    color: 'bg-amber-500/20 text-amber-400 ring-amber-500' },
  { value: 'EXCUSED', label: 'Excused', color: 'bg-brand-500/20 text-brand-400 ring-brand-500' },
];

export default function AdminAttendancePage() {
  const { user } = useAuthStore();
  const qc = useQueryClient();
  const [activeTab, setActiveTab] = useState<'summary' | 'mark'>('summary');
  const [selectedBatchId, setSelectedBatchId] = useState<string>('');
  const [lowOnly, setLowOnly] = useState(false);
  const [markBatchId, setMarkBatchId] = useState('');
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().slice(0, 10));
  const [attendance, setAttendance] = useState<Record<string, AttendanceStatus>>({});

  const { data: centers = [] } = useQuery<{ id: string }[]>({
    queryKey: ['centers'],
    queryFn: () => api.get('/api/v1/centers').then(r => Array.isArray(r.data) ? r.data : (r.data.content ?? [])),
    enabled: !!user,
  });
  const centerId = centers[0]?.id ?? '';

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

  const { data: members = [], isLoading: membersLoading } = useQuery<BatchMember[]>({
    queryKey: ['batch-members-admin', markBatchId],
    queryFn: () => api.get(`/api/v1/centers/${centerId}/batches/${markBatchId}/members`)
      .then(r => Array.isArray(r.data) ? r.data : (r.data.content ?? [])),
    enabled: !!centerId && !!markBatchId && activeTab === 'mark',
  });

  useEffect(() => {
    if (!members.length) return;
    const init: Record<string, AttendanceStatus> = {};
    members.forEach(m => { init[m.studentId] = 'PRESENT'; });
    setAttendance(init);
  }, [members]);

  const { mutate: submitAttendance, isPending: isSubmitting } = useMutation({
    mutationFn: () => api.post(`/api/v1/centers/${centerId}/batches/${markBatchId}/attendance`, {
      date: selectedDate,
      entries: members.map(m => ({
        studentId: m.studentId,
        status: attendance[m.studentId] ?? 'PRESENT',
        notes: null,
      })),
    }),
    onSuccess: () => toast.success('Attendance marked successfully'),
    onError: () => toast.error('Failed to mark attendance'),
  });

  function markAll(status: AttendanceStatus) {
    const updated: Record<string, AttendanceStatus> = {};
    members.forEach(m => { updated[m.studentId] = status; });
    setAttendance(updated);
  }

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
        {activeTab === 'summary' && displayed.length > 0 && (
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

      {/* Tab switcher */}
      <div className="flex gap-1 p-1 bg-white/5 rounded-xl w-fit">
        <button
          onClick={() => setActiveTab('summary')}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${
            activeTab === 'summary'
              ? 'bg-brand-500/20 text-brand-400'
              : 'text-white/40 hover:text-white'
          }`}
        >
          View Summary
        </button>
        <button
          onClick={() => setActiveTab('mark')}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${
            activeTab === 'mark'
              ? 'bg-brand-500/20 text-brand-400'
              : 'text-white/40 hover:text-white'
          }`}
        >
          Mark Attendance
        </button>
      </div>

      {/* Summary tab */}
      {activeTab === 'summary' && (
        <>
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
        </>
      )}

      {/* Mark Attendance tab */}
      {activeTab === 'mark' && (
        <div className="space-y-5">
          {/* Batch + Date row */}
          <div className="flex flex-wrap gap-4">
            <div className="relative">
              <select
                value={markBatchId}
                onChange={e => setMarkBatchId(e.target.value)}
                className="w-56 appearance-none bg-surface-100 border border-white/10 rounded-xl px-4 py-2.5 text-white text-sm focus:outline-none focus:border-brand-500/50 pr-10"
              >
                <option value="">Select batch…</option>
                {batches.map(b => <option key={b.id} value={b.id}>{b.name} ({b.code})</option>)}
              </select>
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/40 pointer-events-none" />
            </div>
            <div className="relative flex items-center gap-2">
              <Calendar className="w-4 h-4 text-white/40" />
              <input
                type="date"
                value={selectedDate}
                onChange={e => setSelectedDate(e.target.value)}
                className="bg-surface-100 border border-white/10 rounded-xl px-4 py-2.5 text-white text-sm focus:outline-none focus:border-brand-500/50"
              />
            </div>
          </div>

          {!markBatchId ? (
            <div className="flex flex-col items-center gap-3 py-16 text-white/30">
              <ClipboardCheck className="w-10 h-10" />
              <p>Select a batch and date to mark attendance</p>
            </div>
          ) : membersLoading ? (
            <div className="text-center py-16 text-white/40">Loading students…</div>
          ) : members.length === 0 ? (
            <div className="flex flex-col items-center gap-3 py-16 text-white/30">
              <Users className="w-10 h-10" />
              <p>No students in this batch yet.</p>
            </div>
          ) : (
            <div className="space-y-4">
              {/* Bulk actions */}
              <div className="flex items-center gap-2 flex-wrap">
                <span className="text-xs text-white/40">Mark all:</span>
                {STATUS_OPTIONS.map(s => (
                  <button
                    key={s.value}
                    onClick={() => markAll(s.value)}
                    className={`px-3 py-1 rounded-lg text-xs font-medium transition-all ${s.color}`}
                  >
                    {s.label}
                  </button>
                ))}
              </div>

              {/* Student list */}
              <div className="space-y-2">
                {members.map(m => (
                  <div key={m.studentId} className="bg-surface-50/40 border border-white/5 rounded-xl px-4 py-3 flex items-center justify-between gap-4">
                    <span className="text-sm font-medium text-white flex-1 min-w-0 truncate">{m.studentName}</span>
                    <div className="flex gap-1 flex-shrink-0">
                      {STATUS_OPTIONS.map(s => (
                        <button
                          key={s.value}
                          onClick={() => setAttendance(prev => ({ ...prev, [m.studentId]: s.value }))}
                          className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ring-inset ${
                            attendance[m.studentId] === s.value
                              ? `${s.color} ring-1`
                              : 'bg-white/5 text-white/40 hover:bg-white/10'
                          }`}
                        >
                          {s.label}
                        </button>
                      ))}
                    </div>
                  </div>
                ))}
              </div>

              {/* Submit */}
              <div className="flex justify-end pt-2">
                <button
                  onClick={() => submitAttendance()}
                  disabled={isSubmitting}
                  className="btn-primary flex items-center gap-2 disabled:opacity-50"
                >
                  {isSubmitting ? (
                    <><div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" /> Saving…</>
                  ) : (
                    <><ClipboardCheck className="w-4 h-4" /> Save Attendance ({members.length} students)</>
                  )}
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
