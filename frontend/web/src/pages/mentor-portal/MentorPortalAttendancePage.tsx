import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import { ClipboardCheck, Check, X, Clock, BookOpen, AlertCircle } from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { ExportMenu } from '../../components/ui/ExportMenu';

interface Batch {
  id: string;
  name: string;
  code: string;
  centerId: string;
}

interface BatchMember {
  id: string;
  studentId: string;
  studentName: string;
}

type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSED';

const STATUS_OPTIONS: { value: AttendanceStatus; label: string; icon: React.ElementType; color: string }[] = [
  { value: 'PRESENT', label: 'Present', icon: Check,  color: 'bg-emerald-500/20 text-emerald-400 ring-emerald-500' },
  { value: 'ABSENT',  label: 'Absent',  icon: X,      color: 'bg-red-500/20 text-red-400 ring-red-500' },
  { value: 'LATE',    label: 'Late',    icon: Clock,   color: 'bg-amber-500/20 text-amber-400 ring-amber-500' },
  { value: 'EXCUSED', label: 'Excused', icon: BookOpen, color: 'bg-brand-500/20 text-brand-400 ring-brand-500' },
];

export default function MentorPortalAttendancePage() {
  const { user } = useAuthStore();
  const qc = useQueryClient();
  const [selectedBatchId, setSelectedBatchId] = useState<string>('');
  const [selectedDate, setSelectedDate] = useState<string>(new Date().toISOString().slice(0, 10));
  const [attendance, setAttendance] = useState<Record<string, AttendanceStatus>>({});

  const { data: mentorProfile } = useQuery({
    queryKey: ['mentor-profile-me'],
    queryFn: () => api.get('/api/v1/mentors/me').then(r => r.data),
    enabled: !!user,
    staleTime: 5 * 60 * 1000,
    retry: false,
  });

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

  const { data: members = [], isLoading: membersLoading } = useQuery<BatchMember[]>({
    queryKey: ['batch-members', selectedBatchId],
    queryFn: () => api.get(`/api/v1/centers/${centerId}/batches/${selectedBatchId}/members`).then(r =>
      Array.isArray(r.data) ? r.data : (r.data.content ?? [])),
    enabled: !!selectedBatchId && !!centerId,
  });

  // Pre-fill PRESENT for all members when roster loads
  useEffect(() => {
    if (members.length === 0) return;
    const init: Record<string, AttendanceStatus> = {};
    members.forEach((m: BatchMember) => { init[m.studentId] = 'PRESENT'; });
    setAttendance(init);
  }, [members]);

  const { mutate: submitAttendance, isPending } = useMutation({
    mutationFn: () => {
      const entries = members.map(m => ({
        studentId: m.studentId,
        status: attendance[m.studentId] ?? 'PRESENT',
        notes: null,
      }));
      return api.post(`/api/v1/centers/${centerId}/batches/${selectedBatchId}/attendance`, {
        date: selectedDate,
        entries,
      });
    },
    onSuccess: () => {
      toast.success('Attendance marked successfully');
      qc.invalidateQueries({ queryKey: ['attendance', selectedBatchId] });
    },
    onError: () => toast.error('Failed to mark attendance'),
  });

  function markAll(status: AttendanceStatus) {
    const updated: Record<string, AttendanceStatus> = {};
    members.forEach(m => { updated[m.studentId] = status; });
    setAttendance(updated);
  }

  const selectedBatch = batches.find(b => b.id === selectedBatchId);

  return (
    <div className="p-6 space-y-6 max-w-4xl mx-auto">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-brand-500/10 border border-brand-500/20">
            <ClipboardCheck className="w-5 h-5 text-brand-400" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-white">Mark Attendance</h1>
            <p className="text-sm text-white/50">Select a batch and date to record attendance</p>
          </div>
        </div>
        {members.length > 0 && (
          <ExportMenu
            csvData={members.map(m => ({
              Student: m.studentName,
              Date: selectedDate,
              Status: attendance[m.studentId] ?? 'NOT_MARKED',
            }))}
            csvFilename={`attendance-${selectedDate}`}
          />
        )}
      </div>

      {/* Selectors */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="space-y-1.5">
          <label className="text-xs font-medium text-white/50 uppercase tracking-wider">Batch</label>
          <select
            value={selectedBatchId}
            onChange={e => setSelectedBatchId(e.target.value)}
            className="w-full bg-surface-100 border border-white/10 rounded-xl px-3 py-2.5 text-white text-sm focus:outline-none focus:border-brand-500/50"
          >
            <option value="">Select batch...</option>
            {batches.map(b => (
              <option key={b.id} value={b.id}>{b.name} ({b.code})</option>
            ))}
          </select>
        </div>
        <div className="space-y-1.5">
          <label className="text-xs font-medium text-white/50 uppercase tracking-wider">Date</label>
          <input
            type="date"
            value={selectedDate}
            onChange={e => setSelectedDate(e.target.value)}
            max={new Date().toISOString().slice(0, 10)}
            className="w-full bg-surface-100 border border-white/10 rounded-xl px-3 py-2.5 text-white text-sm focus:outline-none focus:border-brand-500/50"
          />
        </div>
      </div>

      {selectedBatchId && (
        <>
          {/* Bulk actions */}
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-xs text-white/40 mr-2">Mark all as:</span>
            {STATUS_OPTIONS.map(opt => (
              <button
                key={opt.value}
                onClick={() => markAll(opt.value)}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium ${opt.color.split(' ').slice(0, 2).join(' ')} hover:ring-1 ${opt.color.split(' ')[2]} transition-all`}
              >
                {opt.label}
              </button>
            ))}
          </div>

          {/* Student roster */}
          {membersLoading ? (
            <div className="text-center py-12 text-white/40">Loading students...</div>
          ) : members.length === 0 ? (
            <div className="flex flex-col items-center gap-3 py-16 text-white/40">
              <AlertCircle className="w-10 h-10" />
              <p>No students in this batch yet. Ask admin to add members.</p>
            </div>
          ) : (
            <div className="space-y-2">
              {members.map((member, i) => {
                const current = attendance[member.studentId] ?? 'PRESENT';
                return (
                  <motion.div
                    key={member.studentId}
                    initial={{ opacity: 0, y: 8 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: i * 0.03 }}
                    className="flex items-center justify-between bg-surface-50/40 border border-white/5 rounded-xl px-4 py-3"
                  >
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-full bg-brand-500/10 border border-brand-500/20 flex items-center justify-center text-brand-400 text-sm font-bold">
                        {member.studentName?.[0]?.toUpperCase() ?? '?'}
                      </div>
                      <div>
                        <div className="text-sm font-medium text-white">{member.studentName}</div>
                        <div className="text-xs text-white/40">{member.studentId.slice(0, 8)}...</div>
                      </div>
                    </div>
                    <div className="flex items-center gap-1.5">
                      {STATUS_OPTIONS.map(opt => {
                        const Icon = opt.icon;
                        const active = current === opt.value;
                        return (
                          <button
                            key={opt.value}
                            onClick={() => setAttendance(prev => ({ ...prev, [member.studentId]: opt.value }))}
                            title={opt.label}
                            className={`p-2 rounded-lg transition-all ${active ? opt.color.split(' ').slice(0, 2).join(' ') + ' ring-1 ' + opt.color.split(' ')[2] : 'text-white/20 hover:text-white/50 hover:bg-white/5'}`}
                          >
                            <Icon className="w-4 h-4" />
                          </button>
                        );
                      })}
                    </div>
                  </motion.div>
                );
              })}
            </div>
          )}

          {members.length > 0 && (
            <div className="flex justify-end pt-2">
              <button
                onClick={() => submitAttendance()}
                disabled={isPending}
                className="px-6 py-2.5 bg-brand-600 hover:bg-brand-500 text-white rounded-xl text-sm font-semibold transition-colors disabled:opacity-50"
              >
                {isPending ? 'Saving...' : `Submit Attendance (${members.length} students)`}
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
