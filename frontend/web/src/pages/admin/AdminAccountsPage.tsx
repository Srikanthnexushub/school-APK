// src/pages/admin/AdminAccountsPage.tsx
import { useState, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Wallet, LayoutDashboard, CreditCard, Users, FileText,
  ClipboardList, TrendingUp, TrendingDown, AlertTriangle,
  CheckCircle2, AlertCircle, CircleDashed, Send, Loader2,
  Sparkles, X, Printer, BellRing, Gift, ShieldAlert,
  ChevronDown, Download, RefreshCw, IndianRupee,
  BarChart3, Clock, Zap, Filter,
} from 'lucide-react';
import { toast } from 'sonner';
import { cn } from '../../lib/utils';
import api from '../../lib/api';
import AdminFeesPage from './AdminFeesPage';

// ─── Types ────────────────────────────────────────────────────────────────────

interface StudentFeeReportItem {
  studentId: string;
  studentName: string;
  totalPaid: number;
  paymentCount: number;
  paymentStatus: 'FULLY_PAID' | 'PARTIAL' | 'NO_PAYMENT';
}

interface FeeStructure {
  id: string; name: string; amount: number; currency: string;
  frequency: string; dueDay: number; lateFeeAmount?: number; status: string;
}

type Tab        = 'overview' | 'structures' | 'collections' | 'audit';
type ColFilter  = 'ALL' | 'FULLY_PAID' | 'PARTIAL' | 'NO_PAYMENT';
type CampaignType = 'reminder' | 'warning' | 'offer';

// ─── Constants ────────────────────────────────────────────────────────────────

const STATUS_CFG = {
  FULLY_PAID: { label: 'Fully Paid',   color: 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/20', icon: CheckCircle2, risk: 'LOW'  },
  PARTIAL:    { label: 'Installment',  color: 'bg-amber-500/15  text-amber-400  border border-amber-500/20',    icon: AlertCircle,  risk: 'MED'  },
  NO_PAYMENT: { label: 'No Payment',   color: 'bg-red-500/15    text-red-400    border border-red-500/20',       icon: CircleDashed, risk: 'HIGH' },
} as const;

const RISK_CFG = {
  HIGH: { label: 'High Risk',   color: 'text-red-400',     dot: 'bg-red-500'     },
  MED:  { label: 'At Risk',     color: 'text-amber-400',   dot: 'bg-amber-500'   },
  LOW:  { label: 'Healthy',     color: 'text-emerald-400', dot: 'bg-emerald-500' },
};

const CAMPAIGN_CFG: Record<CampaignType, {
  label: string; icon: React.ElementType; color: string; bg: string;
  subject: string; body: string; target: ColFilter;
}> = {
  reminder: {
    label: 'Payment Reminder', icon: BellRing,
    color: 'text-brand-400', bg: 'bg-brand-500/10 border border-brand-500/20',
    subject: 'Friendly Reminder: Fee Payment Due',
    body: 'Dear Parent,\n\nThis is a gentle reminder that your child\'s fee payment is pending. Please make the payment at your earliest convenience to avoid any late fees.\n\nThank you for your support.',
    target: 'NO_PAYMENT',
  },
  warning: {
    label: 'Late Payment Warning', icon: ShieldAlert,
    color: 'text-red-400', bg: 'bg-red-500/10 border border-red-500/20',
    subject: '⚠️ Urgent: Fee Overdue — Action Required',
    body: 'Dear Parent,\n\nWe notice your child\'s fee payment is significantly overdue. Please note that continued non-payment may affect your child\'s enrollment status.\n\nKindly clear the dues immediately or contact us to discuss a payment plan.',
    target: 'NO_PAYMENT',
  },
  offer: {
    label: 'Installment Offer', icon: Gift,
    color: 'text-violet-400', bg: 'bg-violet-500/10 border border-violet-500/20',
    subject: '🎁 Special Offer: Flexible Installment Plan Available',
    body: 'Dear Parent,\n\nWe understand that full fee payment can sometimes be challenging. We\'re pleased to offer you a flexible installment plan with no additional charges.\n\nPlease contact our accounts team to set up a convenient payment schedule.',
    target: 'PARTIAL',
  },
};

const TABS: { id: Tab; label: string; icon: React.ElementType }[] = [
  { id: 'overview',    label: 'Overview',         icon: LayoutDashboard },
  { id: 'structures',  label: 'Fee Structures',   icon: CreditCard      },
  { id: 'collections', label: 'Collections',      icon: Users           },
  { id: 'audit',       label: 'Audit & Reports',  icon: ClipboardList   },
];

// ─── KPI Card ─────────────────────────────────────────────────────────────────

function KpiCard({ label, value, sub, icon: Icon, color, bg, trend }: {
  label: string; value: string | number; sub?: string;
  icon: React.ElementType; color: string; bg: string; trend?: 'up' | 'down' | 'neutral';
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      className="glass rounded-2xl p-5 flex flex-col gap-3"
    >
      <div className="flex items-center justify-between">
        <div className={cn('p-2.5 rounded-xl', bg)}>
          <Icon className={cn('w-5 h-5', color)} />
        </div>
        {trend && (
          <span className={cn('flex items-center gap-1 text-xs font-medium',
            trend === 'up' ? 'text-emerald-400' : trend === 'down' ? 'text-red-400' : 'text-white/30'
          )}>
            {trend === 'up' ? <TrendingUp className="w-3.5 h-3.5" /> : trend === 'down' ? <TrendingDown className="w-3.5 h-3.5" /> : null}
          </span>
        )}
      </div>
      <div>
        <p className="text-2xl font-bold text-white tracking-tight">{value}</p>
        <p className="text-xs text-white/50 mt-0.5">{label}</p>
        {sub && <p className="text-[10px] text-white/30 mt-0.5">{sub}</p>}
      </div>
    </motion.div>
  );
}

// ─── Collection Ring ──────────────────────────────────────────────────────────

function CollectionRing({ rate }: { rate: number }) {
  const r = 40, circ = 2 * Math.PI * r;
  const dash = (rate / 100) * circ;
  const color = rate >= 75 ? '#10b981' : rate >= 50 ? '#f59e0b' : '#ef4444';
  return (
    <div className="flex flex-col items-center gap-2">
      <div className="relative w-24 h-24">
        <svg width="96" height="96" viewBox="0 0 96 96" className="-rotate-90">
          <circle cx="48" cy="48" r={r} fill="none" stroke="rgba(255,255,255,0.05)" strokeWidth="8" />
          <motion.circle
            cx="48" cy="48" r={r} fill="none" stroke={color} strokeWidth="8"
            strokeLinecap="round"
            strokeDasharray={circ}
            initial={{ strokeDashoffset: circ }}
            animate={{ strokeDashoffset: circ - dash }}
            transition={{ duration: 1.2, ease: 'easeOut' }}
          />
        </svg>
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-xl font-bold text-white">{rate}%</span>
          <span className="text-[9px] text-white/40 uppercase tracking-wide">collected</span>
        </div>
      </div>
      <p className="text-xs text-white/50">Collection Rate</p>
    </div>
  );
}

// ─── Invoice Modal ────────────────────────────────────────────────────────────

function InvoiceModal({ student, centerId, fees, onClose }: {
  student: StudentFeeReportItem; centerId: string;
  fees: FeeStructure[]; onClose: () => void;
}) {
  const printRef = useRef<HTMLDivElement>(null);
  const handlePrint = () => window.print();
  const totalFee = fees.reduce((s, f) => s + f.amount, 0);
  const outstanding = Math.max(0, totalFee - student.totalPaid);

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
        className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4"
        onClick={onClose}
      >
        <motion.div
          initial={{ scale: 0.95, opacity: 0 }} animate={{ scale: 1, opacity: 1 }}
          className="bg-[#0f1117] border border-white/10 rounded-2xl w-full max-w-lg shadow-2xl"
          onClick={e => e.stopPropagation()}
        >
          {/* Modal header */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-white/5">
            <div className="flex items-center gap-2">
              <FileText className="w-4 h-4 text-brand-400" />
              <span className="text-sm font-semibold text-white">Fee Invoice</span>
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={handlePrint}
                className="flex items-center gap-1.5 px-3 py-1.5 bg-brand-600 hover:bg-brand-500 text-white rounded-lg text-xs font-medium transition-colors"
              >
                <Printer className="w-3.5 h-3.5" /> Print / PDF
              </button>
              <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-white/5 text-white/40 hover:text-white/70">
                <X className="w-4 h-4" />
              </button>
            </div>
          </div>

          {/* Invoice body */}
          <div ref={printRef} className="p-6 space-y-5">
            <div className="flex items-start justify-between">
              <div>
                <p className="text-xs text-white/40 uppercase tracking-wider">Invoice To</p>
                <p className="text-white font-semibold mt-0.5">{student.studentName}</p>
                <p className="text-xs text-white/40 mt-0.5">Student ID: {student.studentId.slice(0, 8)}…</p>
              </div>
              <div className="text-right">
                <p className="text-xs text-white/40 uppercase tracking-wider">Date</p>
                <p className="text-white text-sm mt-0.5">{new Date().toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}</p>
                <span className={cn('inline-block mt-1 text-[10px] px-2 py-0.5 rounded-full font-medium',
                  STATUS_CFG[student.paymentStatus].color
                )}>{STATUS_CFG[student.paymentStatus].label}</span>
              </div>
            </div>

            <div className="border-t border-white/5 pt-4 space-y-2">
              {fees.slice(0, 3).map(f => (
                <div key={f.id} className="flex items-center justify-between text-sm">
                  <span className="text-white/70">{f.name} <span className="text-xs text-white/30">({f.frequency})</span></span>
                  <span className="text-white font-medium">₹{f.amount.toLocaleString('en-IN')}</span>
                </div>
              ))}
              {fees.length === 0 && (
                <p className="text-sm text-white/40 italic">No fee structures assigned</p>
              )}
            </div>

            <div className="border-t border-white/5 pt-4 space-y-1.5">
              <div className="flex justify-between text-sm">
                <span className="text-white/50">Total Fees</span>
                <span className="text-white">₹{totalFee.toLocaleString('en-IN')}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-white/50">Amount Paid</span>
                <span className="text-emerald-400">₹{student.totalPaid.toLocaleString('en-IN')}</span>
              </div>
              <div className="flex justify-between text-sm font-semibold border-t border-white/5 pt-2 mt-2">
                <span className="text-white">Outstanding</span>
                <span className={outstanding > 0 ? 'text-red-400' : 'text-emerald-400'}>
                  ₹{outstanding.toLocaleString('en-IN')}
                </span>
              </div>
            </div>

            <p className="text-[10px] text-white/20 text-center border-t border-white/5 pt-4">
              This is a computer-generated invoice. Please contact the accounts department for queries.
            </p>
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  );
}

// ─── Campaign Modal ───────────────────────────────────────────────────────────

function CampaignModal({ type, targets, centerId, onClose, onSent }: {
  type: CampaignType; targets: StudentFeeReportItem[];
  centerId: string; onClose: () => void; onSent: (count: number) => void;
}) {
  const cfg = CAMPAIGN_CFG[type];
  const eligible = targets.filter(s => s.paymentStatus === cfg.target || type === 'warning');
  const [sending, setSending] = useState(false);
  const [progress, setProgress] = useState(0);

  async function runCampaign() {
    setSending(true);
    let sent = 0;
    for (const s of eligible) {
      try {
        await api.post(`/api/v1/parents/billing/reminder/${s.studentId}?centerId=${centerId}`);
        sent++;
      } catch { /* best-effort */ }
      setProgress(Math.round(((sent) / eligible.length) * 100));
    }
    setSending(false);
    onSent(sent);
  }

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
        className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4"
        onClick={!sending ? onClose : undefined}
      >
        <motion.div
          initial={{ scale: 0.95, opacity: 0 }} animate={{ scale: 1, opacity: 1 }}
          className="bg-[#0f1117] border border-white/10 rounded-2xl w-full max-w-md shadow-2xl"
          onClick={e => e.stopPropagation()}
        >
          <div className="flex items-center justify-between px-6 py-4 border-b border-white/5">
            <div className="flex items-center gap-2.5">
              <div className={cn('p-2 rounded-lg', cfg.bg)}>
                <cfg.icon className={cn('w-4 h-4', cfg.color)} />
              </div>
              <span className="text-sm font-semibold text-white">{cfg.label}</span>
            </div>
            {!sending && (
              <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-white/5 text-white/40 hover:text-white/70">
                <X className="w-4 h-4" />
              </button>
            )}
          </div>

          <div className="p-6 space-y-4">
            <div className="glass rounded-xl p-4 space-y-2">
              <p className="text-xs font-semibold text-white/50 uppercase tracking-wider">Subject</p>
              <p className="text-sm text-white">{cfg.subject}</p>
            </div>

            <div className="glass rounded-xl p-4 space-y-2">
              <p className="text-xs font-semibold text-white/50 uppercase tracking-wider">Message Preview</p>
              <p className="text-xs text-white/60 leading-relaxed whitespace-pre-line">{cfg.body}</p>
            </div>

            <div className="flex items-center justify-between text-sm">
              <span className="text-white/50">Recipients</span>
              <span className="font-semibold text-white">{eligible.length} parent{eligible.length !== 1 ? 's' : ''}</span>
            </div>

            {sending && (
              <div className="space-y-2">
                <div className="h-1.5 bg-white/5 rounded-full overflow-hidden">
                  <motion.div
                    className={cn('h-full rounded-full', type === 'warning' ? 'bg-red-500' : type === 'offer' ? 'bg-violet-500' : 'bg-brand-500')}
                    animate={{ width: `${progress}%` }}
                    transition={{ duration: 0.3 }}
                  />
                </div>
                <p className="text-xs text-white/40 text-center">Sending… {progress}%</p>
              </div>
            )}

            <div className="flex gap-3">
              {!sending && (
                <button
                  onClick={onClose}
                  className="flex-1 py-2.5 rounded-xl border border-white/10 text-white/50 hover:text-white hover:bg-white/5 text-sm transition-colors"
                >
                  Cancel
                </button>
              )}
              <button
                onClick={runCampaign}
                disabled={sending || eligible.length === 0}
                className={cn(
                  'flex-1 py-2.5 rounded-xl text-sm font-semibold transition-all flex items-center justify-center gap-2',
                  type === 'warning' ? 'bg-red-600 hover:bg-red-500 text-white' :
                  type === 'offer'   ? 'bg-violet-600 hover:bg-violet-500 text-white' :
                  'bg-brand-600 hover:bg-brand-500 text-white',
                  (sending || eligible.length === 0) && 'opacity-50 cursor-not-allowed'
                )}
              >
                {sending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
                {sending ? 'Sending…' : `Send to ${eligible.length}`}
              </button>
            </div>
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────

export default function AdminAccountsPage() {
  const qc = useQueryClient();
  const [activeTab, setActiveTab]         = useState<Tab>('overview');
  const [colFilter, setColFilter]         = useState<ColFilter>('ALL');
  const [selected, setSelected]           = useState<Set<string>>(new Set());
  const [invoiceStudent, setInvoiceStudent] = useState<StudentFeeReportItem | null>(null);
  const [campaign, setCampaign]           = useState<CampaignType | null>(null);
  const [aiReport, setAiReport]           = useState('');
  const [auditSearch, setAuditSearch]     = useState('');

  // ── Queries ──────────────────────────────────────────────────────────────

  const { data: centers = [] } = useQuery<{ id: string; name: string }[]>({
    queryKey: ['centers'],
    queryFn: () => api.get('/api/v1/centers').then(r => Array.isArray(r.data) ? r.data : (r.data.content ?? [])),
    staleTime: 5 * 60 * 1000,
  });
  const centerId = centers[0]?.id ?? '';
  const centerName = centers[0]?.name ?? 'Center';

  const { data: report = [], isLoading: reportLoading, refetch: refetchReport } = useQuery<StudentFeeReportItem[]>({
    queryKey: ['billing-report', centerId],
    queryFn: () => api.get(`/api/v1/parents/billing/report?centerId=${centerId}`).then(r =>
      Array.isArray(r.data) ? r.data : []
    ),
    enabled: !!centerId,
  });

  const { data: fees = [] } = useQuery<FeeStructure[]>({
    queryKey: ['fee-structures', centerId],
    queryFn: () => api.get(`/api/v1/centers/${centerId}/fees?size=100`).then(r =>
      Array.isArray(r.data) ? r.data : (r.data.content ?? [])
    ),
    enabled: !!centerId,
  });

  // ── KPIs ──────────────────────────────────────────────────────────────────

  const totalStudents  = report.length;
  const fullyPaid      = report.filter(s => s.paymentStatus === 'FULLY_PAID').length;
  const partial        = report.filter(s => s.paymentStatus === 'PARTIAL').length;
  const noPayment      = report.filter(s => s.paymentStatus === 'NO_PAYMENT').length;
  const totalCollected = report.reduce((s, r) => s + r.totalPaid, 0);
  const collectionRate = totalStudents > 0 ? Math.round((fullyPaid / totalStudents) * 100) : 0;
  const atRisk         = noPayment + partial;

  // ── Mutations ─────────────────────────────────────────────────────────────

  const [sending, setSending] = useState<string | null>(null);
  const singleReminder = useMutation({
    mutationFn: (studentId: string) =>
      api.post(`/api/v1/parents/billing/reminder/${studentId}?centerId=${centerId}`),
    onMutate: id => setSending(id),
    onSuccess: (_, id) => {
      const name = report.find(s => s.studentId === id)?.studentName ?? 'parent';
      toast.success(`Reminder sent to ${name}'s parent.`);
      setSending(null);
    },
    onError: () => { toast.error('Failed to send reminder.'); setSending(null); },
  });

  // ── AI Report ─────────────────────────────────────────────────────────────

  const [aiLoading, setAiLoading] = useState(false);
  async function generateAiReport() {
    if (!totalStudents) { toast.info('No billing data to analyse.'); return; }
    setAiLoading(true);
    try {
      const prompt = `You are a school finance AI analyst. Analyse the following fee collection data for ${centerName} and provide a concise 3-paragraph executive summary with key insights, risk areas, and recommended actions:

Center: ${centerName}
Total Students: ${totalStudents}
Fully Paid: ${fullyPaid} (${collectionRate}%)
Partial Payment: ${partial}
No Payment: ${noPayment}
Total Collected: ₹${totalCollected.toLocaleString('en-IN')}
Fee Structures: ${fees.length} active

Focus on: collection health, at-risk students, actionable recommendations. Be professional and concise.`;
      const res = await api.post('/api/v1/ai/completions', { prompt, maxTokens: 400 });
      setAiReport(res.data?.content ?? res.data?.text ?? 'AI analysis unavailable.');
    } catch {
      setAiReport('AI analysis is temporarily unavailable. Please try again later.');
    } finally {
      setAiLoading(false);
    }
  }

  // ── Collections helpers ───────────────────────────────────────────────────

  const filteredReport = colFilter === 'ALL' ? report : report.filter(s => s.paymentStatus === colFilter);

  const toggleSelect = (id: string) => {
    setSelected(prev => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };
  const toggleAll = () => {
    setSelected(prev =>
      prev.size === filteredReport.length ? new Set() : new Set(filteredReport.map(s => s.studentId))
    );
  };

  // ── Audit helpers ─────────────────────────────────────────────────────────

  const auditRows = report
    .filter(s => auditSearch === '' || s.studentName.toLowerCase().includes(auditSearch.toLowerCase()))
    .sort((a, b) => {
      const order = { NO_PAYMENT: 0, PARTIAL: 1, FULLY_PAID: 2 };
      return order[a.paymentStatus] - order[b.paymentStatus];
    });

  function exportAuditCsv() {
    const header = 'Student Name,Status,Total Paid,Payment Count,Risk Level\n';
    const rows = auditRows.map(r =>
      `"${r.studentName}",${STATUS_CFG[r.paymentStatus].label},${r.totalPaid},${r.paymentCount},${STATUS_CFG[r.paymentStatus].risk}`
    ).join('\n');
    const blob = new Blob([header + rows], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a'); a.href = url;
    a.download = `accounts-audit-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click(); URL.revokeObjectURL(url);
  }

  // ─────────────────────────────────────────────────────────────────────────

  return (
    <div className="p-4 lg:p-6 space-y-6 max-w-7xl mx-auto">

      {/* ── Page Header ─────────────────────────────────────────────────── */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-brand-500/10 border border-brand-500/20">
            <Wallet className="w-5 h-5 text-brand-400" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-white flex items-center gap-2">
              Finance & Accounts
              <span className="text-[10px] font-medium px-2 py-0.5 rounded-full bg-brand-500/15 text-brand-400 border border-brand-500/25">
                AI-Powered
              </span>
            </h1>
            <p className="text-sm text-white/40">Revenue intelligence · Collections · Audit · Automation</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => { refetchReport(); toast.info('Data refreshed.'); }}
            className="p-2 rounded-xl border border-white/10 hover:bg-white/5 text-white/40 hover:text-white/70 transition-colors"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
          <button
            onClick={generateAiReport}
            disabled={aiLoading}
            className="flex items-center gap-2 px-4 py-2 bg-brand-600 hover:bg-brand-500 text-white rounded-xl text-sm font-medium transition-all disabled:opacity-60"
          >
            {aiLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Sparkles className="w-4 h-4" />}
            AI Audit
          </button>
        </div>
      </div>

      {/* ── Tabs ────────────────────────────────────────────────────────── */}
      <div className="flex flex-wrap gap-1 p-1 bg-white/5 rounded-xl border border-white/5 w-fit max-w-full">
        {TABS.map(({ id, label, icon: Icon }) => (
          <button
            key={id}
            onClick={() => setActiveTab(id)}
            className={cn(
              'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all',
              activeTab === id
                ? 'bg-brand-600 text-white shadow-lg shadow-brand-500/20'
                : 'text-white/50 hover:text-white hover:bg-white/5'
            )}
          >
            <Icon className="w-4 h-4" />
            {label}
          </button>
        ))}
      </div>

      {/* ═══════════════════════════════════════════════════════════════════ */}
      {/* TAB 1 — OVERVIEW                                                    */}
      {/* ═══════════════════════════════════════════════════════════════════ */}
      {activeTab === 'overview' && (
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="space-y-6"
        >
          {/* KPI Row */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            <KpiCard
              label="Total Collected" icon={IndianRupee} color="text-brand-400" bg="bg-brand-500/10"
              value={`₹${(totalCollected / 1000).toFixed(1)}k`}
              sub={`${totalStudents} students`} trend="neutral"
            />
            <KpiCard
              label="Fully Paid" icon={CheckCircle2} color="text-emerald-400" bg="bg-emerald-500/10"
              value={fullyPaid} sub={`${collectionRate}% of total`}
              trend={collectionRate >= 75 ? 'up' : 'down'}
            />
            <KpiCard
              label="Outstanding" icon={AlertCircle} color="text-amber-400" bg="bg-amber-500/10"
              value={partial} sub="partial payments" trend="neutral"
            />
            <KpiCard
              label="At Risk" icon={ShieldAlert} color="text-red-400" bg="bg-red-500/10"
              value={noPayment} sub="no payment received" trend={noPayment > 0 ? 'down' : 'up'}
            />
          </div>

          {/* Main content grid */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

            {/* Collection rate ring + quick stats */}
            <div className="glass rounded-2xl p-6 flex flex-col items-center justify-between gap-6">
              <CollectionRing rate={collectionRate} />
              <div className="w-full space-y-3">
                {[
                  { label: 'Fully Paid',   value: fullyPaid,   color: 'bg-emerald-500' },
                  { label: 'Installment',  value: partial,     color: 'bg-amber-500'   },
                  { label: 'No Payment',   value: noPayment,   color: 'bg-red-500'     },
                ].map(({ label, value, color }) => (
                  <div key={label} className="flex items-center gap-3">
                    <div className={cn('w-2 h-2 rounded-full flex-shrink-0', color)} />
                    <div className="flex-1 flex justify-between items-center">
                      <span className="text-xs text-white/50">{label}</span>
                      <span className="text-xs font-semibold text-white">{value}</span>
                    </div>
                    <div className="w-16 h-1.5 bg-white/5 rounded-full overflow-hidden">
                      <div
                        className={cn('h-full rounded-full', color)}
                        style={{ width: totalStudents > 0 ? `${(value / totalStudents) * 100}%` : '0%' }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* AI Revenue Intelligence */}
            <div className="lg:col-span-2 glass rounded-2xl p-6 flex flex-col gap-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-brand-400" />
                  <h3 className="text-sm font-semibold text-white">AI Revenue Intelligence</h3>
                </div>
                <button
                  onClick={generateAiReport}
                  disabled={aiLoading}
                  className="flex items-center gap-1.5 px-3 py-1.5 bg-brand-500/10 hover:bg-brand-500/20 text-brand-400 rounded-lg text-xs font-medium transition-colors border border-brand-500/20 disabled:opacity-50"
                >
                  {aiLoading ? <Loader2 className="w-3 h-3 animate-spin" /> : <Zap className="w-3 h-3" />}
                  {aiReport ? 'Regenerate' : 'Generate Report'}
                </button>
              </div>

              {aiLoading && (
                <div className="flex-1 flex flex-col items-center justify-center gap-3 py-8 text-white/30">
                  <Loader2 className="w-6 h-6 animate-spin text-brand-400" />
                  <p className="text-sm">Analysing financial data…</p>
                </div>
              )}

              {!aiLoading && !aiReport && (
                <div className="flex-1 flex flex-col items-center justify-center gap-3 py-8 text-white/20 border-2 border-dashed border-white/5 rounded-xl">
                  <BarChart3 className="w-8 h-8" />
                  <div className="text-center">
                    <p className="text-sm text-white/40">Click "Generate Report" for AI insights</p>
                    <p className="text-xs mt-1">Revenue trends · Risk analysis · Recommendations</p>
                  </div>
                </div>
              )}

              {!aiLoading && aiReport && (
                <div className="flex-1 bg-brand-500/5 border border-brand-500/10 rounded-xl p-4">
                  <p className="text-sm text-white/70 leading-relaxed whitespace-pre-wrap">{aiReport}</p>
                </div>
              )}
            </div>
          </div>

          {/* Quick Campaign Actions */}
          <div className="glass rounded-2xl p-5">
            <div className="flex items-center gap-2 mb-4">
              <Zap className="w-4 h-4 text-brand-400" />
              <h3 className="text-sm font-semibold text-white">Quick Campaign Actions</h3>
              <span className="text-xs text-white/30 ml-auto">Reach parents instantly</span>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              {(Object.entries(CAMPAIGN_CFG) as [CampaignType, typeof CAMPAIGN_CFG[CampaignType]][]).map(([type, cfg]) => (
                <button
                  key={type}
                  onClick={() => setCampaign(type)}
                  disabled={!centerId}
                  className={cn(
                    'flex items-center gap-3 p-4 rounded-xl text-left transition-all hover:scale-[1.01] active:scale-[0.99] disabled:opacity-40',
                    cfg.bg
                  )}
                >
                  <div className={cn('p-2 rounded-lg bg-black/20')}>
                    <cfg.icon className={cn('w-4 h-4', cfg.color)} />
                  </div>
                  <div>
                    <p className={cn('text-sm font-semibold', cfg.color)}>{cfg.label}</p>
                    <p className="text-xs text-white/40 mt-0.5">
                      {type === 'reminder' ? `${noPayment} pending` :
                       type === 'warning'  ? `${noPayment} overdue` :
                                            `${partial} partial`}
                    </p>
                  </div>
                </button>
              ))}
            </div>
          </div>

          {/* At-Risk Students */}
          {atRisk > 0 && (
            <div className="glass rounded-2xl overflow-hidden">
              <div className="flex items-center justify-between px-5 py-4 border-b border-white/5">
                <div className="flex items-center gap-2">
                  <AlertTriangle className="w-4 h-4 text-amber-400" />
                  <h3 className="text-sm font-semibold text-white">At-Risk Students</h3>
                  <span className="text-xs px-2 py-0.5 rounded-full bg-amber-500/10 text-amber-400 border border-amber-500/20">{atRisk}</span>
                </div>
                <button onClick={() => setActiveTab('collections')} className="text-xs text-brand-400 hover:text-brand-300">
                  View all →
                </button>
              </div>
              <div className="divide-y divide-white/5">
                {report
                  .filter(s => s.paymentStatus !== 'FULLY_PAID')
                  .slice(0, 5)
                  .map(s => {
                    const risk = RISK_CFG[STATUS_CFG[s.paymentStatus].risk];
                    return (
                      <div key={s.studentId} className="flex items-center gap-4 px-5 py-3">
                        <div className="w-8 h-8 rounded-full bg-surface-100 flex items-center justify-center text-xs font-bold text-white/70 flex-shrink-0">
                          {s.studentName.charAt(0).toUpperCase()}
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="text-sm text-white font-medium truncate">{s.studentName}</p>
                          <p className="text-xs text-white/40">
                            {s.totalPaid > 0 ? `₹${s.totalPaid.toLocaleString('en-IN')} paid` : 'No payment received'}
                          </p>
                        </div>
                        <div className="flex items-center gap-1.5 flex-shrink-0">
                          <div className={cn('w-1.5 h-1.5 rounded-full', risk.dot)} />
                          <span className={cn('text-xs font-medium', risk.color)}>{risk.label}</span>
                        </div>
                        <button
                          onClick={() => singleReminder.mutate(s.studentId)}
                          disabled={sending === s.studentId}
                          className="flex-shrink-0 p-1.5 rounded-lg bg-brand-500/10 text-brand-400 hover:bg-brand-500/20 transition-colors disabled:opacity-50"
                          title="Send reminder"
                        >
                          {sending === s.studentId ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Send className="w-3.5 h-3.5" />}
                        </button>
                      </div>
                    );
                  })}
              </div>
            </div>
          )}
        </motion.div>
      )}

      {/* ═══════════════════════════════════════════════════════════════════ */}
      {/* TAB 2 — FEE STRUCTURES                                              */}
      {/* ═══════════════════════════════════════════════════════════════════ */}
      {activeTab === 'structures' && (
        <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}>
          <AdminFeesPage embedded />
        </motion.div>
      )}

      {/* ═══════════════════════════════════════════════════════════════════ */}
      {/* TAB 3 — COLLECTIONS                                                 */}
      {/* ═══════════════════════════════════════════════════════════════════ */}
      {activeTab === 'collections' && (
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="space-y-5"
        >
          {/* Controls bar */}
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex flex-wrap gap-1.5">
              {(['ALL', 'FULLY_PAID', 'PARTIAL', 'NO_PAYMENT'] as ColFilter[]).map(f => {
                const count = f === 'ALL' ? totalStudents :
                              f === 'FULLY_PAID' ? fullyPaid :
                              f === 'PARTIAL' ? partial : noPayment;
                return (
                  <button
                    key={f}
                    onClick={() => setColFilter(f)}
                    className={cn(
                      'px-3 py-1.5 rounded-lg text-xs font-medium transition-colors',
                      colFilter === f
                        ? 'bg-brand-500/15 text-brand-400 border border-brand-500/30'
                        : 'bg-white/5 text-white/50 hover:text-white hover:bg-white/10'
                    )}
                  >
                    {f === 'ALL' ? 'All' : STATUS_CFG[f].label}
                    <span className="ml-1 opacity-60">({count})</span>
                  </button>
                );
              })}
            </div>
            <div className="flex items-center gap-2">
              {selected.size > 0 && (
                <button
                  onClick={() => setCampaign('reminder')}
                  className="flex items-center gap-1.5 px-3 py-1.5 bg-brand-600 hover:bg-brand-500 text-white rounded-lg text-xs font-medium transition-colors"
                >
                  <Send className="w-3.5 h-3.5" />
                  Remind {selected.size} selected
                </button>
              )}
            </div>
          </div>

          {/* Table */}
          {reportLoading ? (
            <div className="flex items-center justify-center py-16">
              <Loader2 className="w-6 h-6 text-brand-400 animate-spin" />
            </div>
          ) : filteredReport.length === 0 ? (
            <div className="glass rounded-2xl p-12 text-center">
              <Users className="w-10 h-10 text-white/20 mx-auto mb-3" />
              <p className="text-white/40 text-sm">No students in this category.</p>
            </div>
          ) : (
            <div className="glass rounded-2xl overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-white/5">
                    <th className="px-4 py-3 text-left">
                      <input
                        type="checkbox"
                        checked={selected.size === filteredReport.length && filteredReport.length > 0}
                        onChange={toggleAll}
                        className="rounded border-white/20 bg-white/5 text-brand-500"
                      />
                    </th>
                    <th className="px-4 py-3 text-left text-xs text-white/30 uppercase tracking-wider font-medium">Student</th>
                    <th className="px-4 py-3 text-left text-xs text-white/30 uppercase tracking-wider font-medium">Status</th>
                    <th className="px-4 py-3 text-left text-xs text-white/30 uppercase tracking-wider font-medium hidden sm:table-cell">Risk</th>
                    <th className="px-4 py-3 text-left text-xs text-white/30 uppercase tracking-wider font-medium hidden md:table-cell">Paid</th>
                    <th className="px-4 py-3 text-left text-xs text-white/30 uppercase tracking-wider font-medium hidden md:table-cell">Count</th>
                    <th className="px-4 py-3 text-right text-xs text-white/30 uppercase tracking-wider font-medium">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5">
                  {filteredReport.map((item, i) => {
                    const cfg  = STATUS_CFG[item.paymentStatus];
                    const risk = RISK_CFG[cfg.risk];
                    const Icon = cfg.icon;
                    return (
                      <motion.tr
                        key={item.studentId}
                        initial={{ opacity: 0, y: 6 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: i * 0.02 }}
                        className={cn(
                          'hover:bg-white/3 transition-colors',
                          selected.has(item.studentId) && 'bg-brand-500/5'
                        )}
                      >
                        <td className="px-4 py-3">
                          <input
                            type="checkbox"
                            checked={selected.has(item.studentId)}
                            onChange={() => toggleSelect(item.studentId)}
                            className="rounded border-white/20 bg-white/5 text-brand-500"
                          />
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-2.5">
                            <div className="w-7 h-7 rounded-full bg-surface-100 flex items-center justify-center text-xs font-bold text-white/60 flex-shrink-0">
                              {item.studentName.charAt(0).toUpperCase()}
                            </div>
                            <span className="font-medium text-white">{item.studentName}</span>
                          </div>
                        </td>
                        <td className="px-4 py-3">
                          <span className={cn('inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium', cfg.color)}>
                            <Icon className="w-3 h-3" />
                            {cfg.label}
                          </span>
                        </td>
                        <td className="px-4 py-3 hidden sm:table-cell">
                          <div className="flex items-center gap-1.5">
                            <div className={cn('w-1.5 h-1.5 rounded-full', risk.dot)} />
                            <span className={cn('text-xs font-medium', risk.color)}>{risk.label}</span>
                          </div>
                        </td>
                        <td className="px-4 py-3 text-white/60 text-sm hidden md:table-cell">
                          {item.totalPaid > 0 ? `₹${item.totalPaid.toLocaleString('en-IN')}` : '—'}
                        </td>
                        <td className="px-4 py-3 text-white/40 text-sm hidden md:table-cell">{item.paymentCount}</td>
                        <td className="px-4 py-3">
                          <div className="flex items-center justify-end gap-1.5">
                            <button
                              onClick={() => setInvoiceStudent(item)}
                              className="p-1.5 rounded-lg bg-white/5 text-white/40 hover:text-white hover:bg-white/10 transition-colors"
                              title="View Invoice"
                            >
                              <FileText className="w-3.5 h-3.5" />
                            </button>
                            {item.paymentStatus !== 'FULLY_PAID' && (
                              <button
                                onClick={() => singleReminder.mutate(item.studentId)}
                                disabled={sending === item.studentId}
                                className="p-1.5 rounded-lg bg-brand-500/10 text-brand-400 hover:bg-brand-500/20 transition-colors disabled:opacity-50"
                                title="Send reminder"
                              >
                                {sending === item.studentId ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Send className="w-3.5 h-3.5" />}
                              </button>
                            )}
                          </div>
                        </td>
                      </motion.tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </motion.div>
      )}

      {/* ═══════════════════════════════════════════════════════════════════ */}
      {/* TAB 4 — AUDIT & REPORTS                                             */}
      {/* ═══════════════════════════════════════════════════════════════════ */}
      {activeTab === 'audit' && (
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="space-y-5"
        >
          {/* Controls */}
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="relative">
              <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-white/30" />
              <input
                type="text"
                placeholder="Search students…"
                value={auditSearch}
                onChange={e => setAuditSearch(e.target.value)}
                className="pl-9 pr-4 py-2 bg-white/5 border border-white/8 rounded-xl text-sm text-white placeholder:text-white/25 focus:outline-none focus:border-brand-500/40 w-full sm:w-56"
              />
            </div>
            <button
              onClick={exportAuditCsv}
              className="flex items-center gap-2 px-4 py-2 bg-white/5 hover:bg-white/10 border border-white/8 text-white/70 hover:text-white rounded-xl text-sm font-medium transition-colors"
            >
              <Download className="w-4 h-4" /> Export CSV
            </button>
          </div>

          {/* Summary stats row */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {[
              { label: 'Total Records', value: totalStudents, color: 'text-white' },
              { label: 'Collection Rate', value: `${collectionRate}%`, color: collectionRate >= 75 ? 'text-emerald-400' : 'text-amber-400' },
              { label: 'Total Collected', value: `₹${totalCollected.toLocaleString('en-IN')}`, color: 'text-brand-400' },
              { label: 'Action Required', value: atRisk, color: atRisk > 0 ? 'text-red-400' : 'text-emerald-400' },
            ].map(({ label, value, color }) => (
              <div key={label} className="glass rounded-xl p-4">
                <p className={cn('text-lg font-bold', color)}>{value}</p>
                <p className="text-xs text-white/40 mt-0.5">{label}</p>
              </div>
            ))}
          </div>

          {/* Audit log table */}
          <div className="glass rounded-2xl overflow-x-auto">
            <div className="flex items-center justify-between px-5 py-4 border-b border-white/5">
              <div className="flex items-center gap-2">
                <Clock className="w-4 h-4 text-brand-400" />
                <h3 className="text-sm font-semibold text-white">Financial Audit Log</h3>
              </div>
              <span className="text-xs text-white/30">{auditRows.length} records</span>
            </div>
            {reportLoading ? (
              <div className="flex items-center justify-center py-12">
                <Loader2 className="w-6 h-6 text-brand-400 animate-spin" />
              </div>
            ) : auditRows.length === 0 ? (
              <div className="py-12 text-center">
                <ClipboardList className="w-10 h-10 text-white/20 mx-auto mb-3" />
                <p className="text-white/40 text-sm">No records found.</p>
              </div>
            ) : (
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-white/5">
                    <th className="px-5 py-3 text-left text-xs text-white/30 uppercase tracking-wider">#</th>
                    <th className="px-5 py-3 text-left text-xs text-white/30 uppercase tracking-wider">Student</th>
                    <th className="px-5 py-3 text-left text-xs text-white/30 uppercase tracking-wider">Payment Status</th>
                    <th className="px-5 py-3 text-left text-xs text-white/30 uppercase tracking-wider hidden sm:table-cell">Risk Level</th>
                    <th className="px-5 py-3 text-left text-xs text-white/30 uppercase tracking-wider hidden md:table-cell">Amount Paid</th>
                    <th className="px-5 py-3 text-left text-xs text-white/30 uppercase tracking-wider hidden md:table-cell">Transactions</th>
                    <th className="px-5 py-3 text-right text-xs text-white/30 uppercase tracking-wider">Invoice</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5">
                  {auditRows.map((item, i) => {
                    const cfg  = STATUS_CFG[item.paymentStatus];
                    const risk = RISK_CFG[cfg.risk];
                    const Icon = cfg.icon;
                    return (
                      <tr key={item.studentId} className="hover:bg-white/3 transition-colors">
                        <td className="px-5 py-3 text-white/30 text-xs">{i + 1}</td>
                        <td className="px-5 py-3">
                          <div className="flex items-center gap-2.5">
                            <div className="w-7 h-7 rounded-full bg-surface-100 flex items-center justify-center text-xs font-bold text-white/60 flex-shrink-0">
                              {item.studentName.charAt(0).toUpperCase()}
                            </div>
                            <span className="font-medium text-white">{item.studentName}</span>
                          </div>
                        </td>
                        <td className="px-5 py-3">
                          <span className={cn('inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium', cfg.color)}>
                            <Icon className="w-3 h-3" />
                            {cfg.label}
                          </span>
                        </td>
                        <td className="px-5 py-3 hidden sm:table-cell">
                          <div className="flex items-center gap-1.5">
                            <div className={cn('w-1.5 h-1.5 rounded-full animate-pulse', risk.dot)} />
                            <span className={cn('text-xs font-medium', risk.color)}>{risk.label}</span>
                          </div>
                        </td>
                        <td className="px-5 py-3 text-white/60 hidden md:table-cell">
                          {item.totalPaid > 0 ? `₹${item.totalPaid.toLocaleString('en-IN')}` : <span className="text-white/25">—</span>}
                        </td>
                        <td className="px-5 py-3 text-white/40 hidden md:table-cell">{item.paymentCount}</td>
                        <td className="px-5 py-3 text-right">
                          <button
                            onClick={() => setInvoiceStudent(item)}
                            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white/5 hover:bg-white/10 text-white/50 hover:text-white text-xs font-medium transition-colors"
                          >
                            <Printer className="w-3 h-3" /> Invoice
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </div>
        </motion.div>
      )}

      {/* ── Modals ─────────────────────────────────────────────────────── */}

      {invoiceStudent && (
        <InvoiceModal
          student={invoiceStudent}
          centerId={centerId}
          fees={fees.filter(f => f.status === 'ACTIVE')}
          onClose={() => setInvoiceStudent(null)}
        />
      )}

      {campaign && (
        <CampaignModal
          type={campaign}
          targets={report}
          centerId={centerId}
          onClose={() => setCampaign(null)}
          onSent={(count) => {
            toast.success(`Campaign sent to ${count} parent${count !== 1 ? 's' : ''}.`);
            setCampaign(null);
          }}
        />
      )}
    </div>
  );
}
