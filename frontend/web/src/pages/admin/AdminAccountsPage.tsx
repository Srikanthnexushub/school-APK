// src/pages/admin/AdminAccountsPage.tsx
import { useState, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Wallet, LayoutDashboard, CreditCard, Users, FileText,
  ClipboardList, TrendingUp, TrendingDown, AlertTriangle,
  CheckCircle2, AlertCircle, CircleDashed, Send, Loader2,
  Sparkles, X, Printer, BellRing, Gift, ShieldAlert,
  Download, RefreshCw, IndianRupee, BarChart3, Clock,
  Zap, Filter, PlusCircle, Bot, Search, ChevronRight,
  CalendarClock, ListChecks, Wand2, Play, ArrowRight,
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

type Tab        = 'overview' | 'billing' | 'autopilot' | 'structures' | 'audit';
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

const AI_ACTION = {
  NO_PAYMENT: { label: 'Immediate Follow-up', color: 'text-red-400',      bg: 'bg-red-500/10 border-red-500/20',         icon: ShieldAlert   },
  PARTIAL:    { label: 'Schedule a Plan',     color: 'text-amber-400',    bg: 'bg-amber-500/10 border-amber-500/20',     icon: Clock         },
  FULLY_PAID: { label: 'All Clear',           color: 'text-emerald-400',  bg: 'bg-emerald-500/10 border-emerald-500/20', icon: CheckCircle2  },
} as const;

const CAMPAIGN_CFG: Record<CampaignType, {
  label: string; icon: React.ElementType; color: string; bg: string;
  subject: string; body: string; target: ColFilter;
}> = {
  reminder: {
    label: 'Payment Reminder', icon: BellRing, color: 'text-brand-400', bg: 'bg-brand-500/10 border border-brand-500/20',
    subject: 'Friendly Reminder: Fee Payment Due',
    body: "Dear Parent,\n\nThis is a gentle reminder that your child's fee payment is pending. Please make the payment at your earliest convenience.\n\nThank you.",
    target: 'NO_PAYMENT',
  },
  warning: {
    label: 'Late Payment Warning', icon: ShieldAlert, color: 'text-red-400', bg: 'bg-red-500/10 border border-red-500/20',
    subject: '⚠️ Urgent: Fee Overdue — Action Required',
    body: "Dear Parent,\n\nYour child's fee payment is significantly overdue. Continued non-payment may affect your child's enrollment.\n\nPlease clear dues immediately or contact us.",
    target: 'NO_PAYMENT',
  },
  offer: {
    label: 'Installment Offer', icon: Gift, color: 'text-violet-400', bg: 'bg-violet-500/10 border border-violet-500/20',
    subject: '🎁 Special Offer: Flexible Installment Plan Available',
    body: "Dear Parent,\n\nWe understand full payment can be challenging. We're pleased to offer a flexible installment plan with no additional charges.\n\nPlease contact our accounts team.",
    target: 'PARTIAL',
  },
};

const TABS: { id: Tab; label: string; icon: React.ElementType }[] = [
  { id: 'overview',    label: 'Overview',        icon: LayoutDashboard },
  { id: 'billing',     label: 'Billing Hub',     icon: CreditCard      },
  { id: 'autopilot',   label: 'AI Autopilot',    icon: Bot             },
  { id: 'structures',  label: 'Fee Structures',  icon: Wallet          },
  { id: 'audit',       label: 'Audit & Reports', icon: ClipboardList   },
];

// ─── KPI Card ─────────────────────────────────────────────────────────────────

function KpiCard({ label, value, sub, icon: Icon, color, bg, trend }: {
  label: string; value: string | number; sub?: string;
  icon: React.ElementType; color: string; bg: string; trend?: 'up' | 'down' | 'neutral';
}) {
  return (
    <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}
      className="glass rounded-2xl p-5 flex flex-col gap-3">
      <div className="flex items-center justify-between">
        <div className={cn('p-2.5 rounded-xl', bg)}><Icon className={cn('w-5 h-5', color)} /></div>
        {trend && (
          <span className={cn('flex items-center gap-1 text-xs font-medium',
            trend === 'up' ? 'text-emerald-400' : trend === 'down' ? 'text-red-400' : 'text-white/30')}>
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
  const r = 40, circ = 2 * Math.PI * r, dash = (rate / 100) * circ;
  const color = rate >= 75 ? '#10b981' : rate >= 50 ? '#f59e0b' : '#ef4444';
  return (
    <div className="flex flex-col items-center gap-2">
      <div className="relative w-24 h-24">
        <svg width="96" height="96" viewBox="0 0 96 96" className="-rotate-90">
          <circle cx="48" cy="48" r={r} fill="none" stroke="rgba(255,255,255,0.05)" strokeWidth="8" />
          <motion.circle cx="48" cy="48" r={r} fill="none" stroke={color} strokeWidth="8"
            strokeLinecap="round" strokeDasharray={circ}
            initial={{ strokeDashoffset: circ }} animate={{ strokeDashoffset: circ - dash }}
            transition={{ duration: 1.2, ease: 'easeOut' }} />
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

function InvoiceModal({ student, fees, totalFeePerStu, onClose }: {
  student: StudentFeeReportItem; fees: FeeStructure[];
  totalFeePerStu: number; onClose: () => void;
}) {
  const totalFee    = totalFeePerStu > 0 ? totalFeePerStu : fees.reduce((s, f) => s + f.amount, 0);
  const outstanding = Math.max(0, totalFee - student.totalPaid);
  return (
    <AnimatePresence>
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
        className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4"
        onClick={onClose}>
        <motion.div initial={{ scale: 0.95, opacity: 0 }} animate={{ scale: 1, opacity: 1 }}
          className="bg-[#0f1117] border border-white/10 rounded-2xl w-full max-w-lg shadow-2xl"
          onClick={e => e.stopPropagation()}>
          <div className="flex items-center justify-between px-6 py-4 border-b border-white/5">
            <div className="flex items-center gap-2">
              <FileText className="w-4 h-4 text-brand-400" />
              <span className="text-sm font-semibold text-white">Fee Invoice</span>
            </div>
            <div className="flex items-center gap-2">
              <button onClick={() => window.print()}
                className="flex items-center gap-1.5 px-3 py-1.5 bg-brand-600 hover:bg-brand-500 text-white rounded-lg text-xs font-medium transition-colors">
                <Printer className="w-3.5 h-3.5" /> Print / PDF
              </button>
              <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-white/5 text-white/40 hover:text-white/70">
                <X className="w-4 h-4" />
              </button>
            </div>
          </div>
          <div className="p-6 space-y-5">
            <div className="flex items-start justify-between">
              <div>
                <p className="text-xs text-white/40 uppercase tracking-wider">Invoice To</p>
                <p className="text-white font-semibold mt-0.5">{student.studentName}</p>
                <p className="text-xs text-white/40 mt-0.5">ID: {student.studentId.slice(0, 8)}…</p>
              </div>
              <div className="text-right">
                <p className="text-xs text-white/40 uppercase tracking-wider">Date</p>
                <p className="text-white text-sm mt-0.5">{new Date().toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}</p>
                <span className={cn('inline-block mt-1 text-[10px] px-2 py-0.5 rounded-full font-medium', STATUS_CFG[student.paymentStatus].color)}>
                  {STATUS_CFG[student.paymentStatus].label}
                </span>
              </div>
            </div>
            <div className="border-t border-white/5 pt-4 space-y-2">
              {fees.slice(0, 4).map(f => (
                <div key={f.id} className="flex items-center justify-between text-sm">
                  <span className="text-white/70">{f.name} <span className="text-xs text-white/30">({f.frequency})</span></span>
                  <span className="text-white font-medium">₹{f.amount.toLocaleString('en-IN')}</span>
                </div>
              ))}
              {fees.length === 0 && <p className="text-sm text-white/40 italic">No fee structures assigned</p>}
            </div>
            <div className="border-t border-white/5 pt-4 space-y-1.5">
              <div className="flex justify-between text-sm"><span className="text-white/50">Total Fees</span><span className="text-white">₹{totalFee.toLocaleString('en-IN')}</span></div>
              <div className="flex justify-between text-sm"><span className="text-white/50">Amount Paid</span><span className="text-emerald-400">₹{student.totalPaid.toLocaleString('en-IN')}</span></div>
              <div className="flex justify-between text-sm font-semibold border-t border-white/5 pt-2 mt-2">
                <span className="text-white">Outstanding Balance</span>
                <span className={outstanding > 0 ? 'text-red-400' : 'text-emerald-400'}>₹{outstanding.toLocaleString('en-IN')}</span>
              </div>
            </div>
            <p className="text-[10px] text-white/20 text-center border-t border-white/5 pt-4">
              Computer-generated invoice. Contact accounts department for queries.
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
      try { await api.post(`/api/v1/parents/billing/reminder/${s.studentId}?centerId=${centerId}`); sent++; } catch { /* best-effort */ }
      setProgress(Math.round((sent / eligible.length) * 100));
    }
    setSending(false);
    onSent(sent);
  }

  return (
    <AnimatePresence>
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
        className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4"
        onClick={!sending ? onClose : undefined}>
        <motion.div initial={{ scale: 0.95, opacity: 0 }} animate={{ scale: 1, opacity: 1 }}
          className="bg-[#0f1117] border border-white/10 rounded-2xl w-full max-w-md shadow-2xl"
          onClick={e => e.stopPropagation()}>
          <div className="flex items-center justify-between px-6 py-4 border-b border-white/5">
            <div className="flex items-center gap-2.5">
              <div className={cn('p-2 rounded-lg', cfg.bg)}><cfg.icon className={cn('w-4 h-4', cfg.color)} /></div>
              <span className="text-sm font-semibold text-white">{cfg.label}</span>
            </div>
            {!sending && <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-white/5 text-white/40 hover:text-white/70"><X className="w-4 h-4" /></button>}
          </div>
          <div className="p-6 space-y-4">
            <div className="glass rounded-xl p-4 space-y-1">
              <p className="text-xs font-semibold text-white/40 uppercase tracking-wider">Subject</p>
              <p className="text-sm text-white">{cfg.subject}</p>
            </div>
            <div className="glass rounded-xl p-4 space-y-1">
              <p className="text-xs font-semibold text-white/40 uppercase tracking-wider">Message Preview</p>
              <p className="text-xs text-white/60 leading-relaxed whitespace-pre-line">{cfg.body}</p>
            </div>
            <div className="flex items-center justify-between text-sm">
              <span className="text-white/50">Recipients</span>
              <span className="font-semibold text-white">{eligible.length} parent{eligible.length !== 1 ? 's' : ''}</span>
            </div>
            {sending && (
              <div className="space-y-2">
                <div className="h-1.5 bg-white/5 rounded-full overflow-hidden">
                  <motion.div className={cn('h-full rounded-full', type === 'warning' ? 'bg-red-500' : type === 'offer' ? 'bg-violet-500' : 'bg-brand-500')}
                    animate={{ width: `${progress}%` }} transition={{ duration: 0.3 }} />
                </div>
                <p className="text-xs text-white/40 text-center">Sending… {progress}%</p>
              </div>
            )}
            <div className="flex gap-3">
              {!sending && <button onClick={onClose} className="flex-1 py-2.5 rounded-xl border border-white/10 text-white/50 hover:text-white hover:bg-white/5 text-sm transition-colors">Cancel</button>}
              <button onClick={runCampaign} disabled={sending || eligible.length === 0}
                className={cn('flex-1 py-2.5 rounded-xl text-sm font-semibold flex items-center justify-center gap-2 transition-all',
                  type === 'warning' ? 'bg-red-600 hover:bg-red-500 text-white' :
                  type === 'offer'   ? 'bg-violet-600 hover:bg-violet-500 text-white' :
                  'bg-brand-600 hover:bg-brand-500 text-white',
                  (sending || eligible.length === 0) && 'opacity-50 cursor-not-allowed')}>
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

// ─── Billing Wizard ───────────────────────────────────────────────────────────
// Multi-step modal: 1) Select Student → 2) Payment Details → 3) AI Review → 4) Confirm

function BillingWizard({ students, centerId, totalFeePerStu, fees, onClose, onSuccess }: {
  students: StudentFeeReportItem[];
  centerId: string;
  totalFeePerStu: number;
  fees: FeeStructure[];
  onClose: () => void;
  onSuccess: () => void;
}) {
  const [step, setStep]       = useState<1 | 2 | 3>(1);
  const [search, setSearch]   = useState('');
  const [student, setStudent] = useState<StudentFeeReportItem | null>(null);
  const [amount,  setAmount]  = useState('');
  const [method,  setMethod]  = useState('CASH');
  const [feeType, setFeeType] = useState('TUITION');
  const [refNo,   setRefNo]   = useState(`PAY-${Date.now()}`);
  const [date,    setDate]    = useState(new Date().toISOString().slice(0, 10));
  const [remarks, setRemarks] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const filtered = students.filter(s =>
    s.studentName.toLowerCase().includes(search.toLowerCase())
  );

  const balance = student ? Math.max(0, totalFeePerStu - student.totalPaid) : 0;
  const numAmount = parseFloat(amount) || 0;
  const afterPaid = student ? student.totalPaid + numAmount : 0;
  const newStatus = afterPaid >= totalFeePerStu ? 'Fully Paid' : afterPaid > 0 ? 'Partial' : 'No Payment';
  const newStatusColor = afterPaid >= totalFeePerStu ? 'text-emerald-400' : afterPaid > 0 ? 'text-amber-400' : 'text-red-400';

  async function handleSubmit() {
    if (!student || !amount || !refNo || !date) { toast.error('Please fill all required fields.'); return; }
    setSubmitting(true);
    try {
      await api.post('/api/v1/parents/billing/record-payment', {
        studentId: student.studentId,
        centerId,
        amountPaid: parseFloat(amount),
        referenceNumber: refNo,
        paymentDate: date,
        paymentMethod: method,
        feeType,
        remarks: remarks || null,
      });
      toast.success(`Payment of ₹${parseFloat(amount).toLocaleString('en-IN')} recorded for ${student.studentName}.`);
      onSuccess();
    } catch (e: any) {
      toast.error(e?.response?.data?.message ?? 'Failed to record payment.');
    } finally {
      setSubmitting(false);
    }
  }

  const STEPS = ['Select Student', 'Payment Details', 'Confirm'];

  return (
    <AnimatePresence>
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
        className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4"
        onClick={onClose}>
        <motion.div initial={{ scale: 0.95, opacity: 0 }} animate={{ scale: 1, opacity: 1 }}
          className="bg-[#0f1117] border border-white/10 rounded-2xl w-full max-w-lg shadow-2xl"
          onClick={e => e.stopPropagation()}>

          {/* Header */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-white/5">
            <div className="flex items-center gap-2">
              <PlusCircle className="w-4 h-4 text-brand-400" />
              <span className="text-sm font-semibold text-white">Create Bill</span>
            </div>
            <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-white/5 text-white/40 hover:text-white/70"><X className="w-4 h-4" /></button>
          </div>

          {/* Step indicator */}
          <div className="flex items-center gap-0 px-6 pt-5">
            {STEPS.map((label, i) => (
              <div key={i} className="flex items-center gap-0 flex-1 last:flex-none">
                <div className={cn('w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0 transition-colors',
                  step > i + 1 ? 'bg-brand-600 text-white' : step === i + 1 ? 'bg-brand-600 text-white ring-2 ring-brand-400/30' : 'bg-white/10 text-white/30')}>
                  {step > i + 1 ? <CheckCircle2 className="w-3.5 h-3.5" /> : i + 1}
                </div>
                <span className={cn('text-[10px] ml-1.5 hidden sm:inline font-medium', step === i + 1 ? 'text-white' : 'text-white/30')}>{label}</span>
                {i < STEPS.length - 1 && <ArrowRight className="w-3 h-3 text-white/15 mx-2 flex-shrink-0" />}
              </div>
            ))}
          </div>

          <div className="p-6">

            {/* ── Step 1: Select Student ── */}
            {step === 1 && (
              <div className="space-y-4">
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-white/30" />
                  <input type="text" placeholder="Search student…" value={search} onChange={e => setSearch(e.target.value)}
                    autoFocus
                    className="w-full pl-9 pr-4 py-2.5 bg-white/5 border border-white/10 rounded-xl text-sm text-white placeholder:text-white/25 focus:outline-none focus:border-brand-500/40" />
                </div>
                <div className="space-y-1 max-h-56 overflow-y-auto pr-1">
                  {filtered.length === 0 ? (
                    <p className="text-center text-white/30 text-sm py-8">No students found</p>
                  ) : filtered.map(s => {
                    const bal = Math.max(0, totalFeePerStu - s.totalPaid);
                    const cfg = STATUS_CFG[s.paymentStatus];
                    return (
                      <button key={s.studentId}
                        onClick={() => { setStudent(s); if (bal > 0) setAmount(String(bal)); setStep(2); }}
                        className={cn('w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-left transition-all hover:bg-white/5',
                          student?.studentId === s.studentId && 'bg-brand-500/10 border border-brand-500/20')}>
                        <div className="w-8 h-8 rounded-full bg-surface-100 flex items-center justify-center text-xs font-bold text-white/60 flex-shrink-0">
                          {s.studentName.charAt(0).toUpperCase()}
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium text-white truncate">{s.studentName}</p>
                          <p className="text-xs text-white/40">
                            Paid: ₹{s.totalPaid.toLocaleString('en-IN')}
                            {bal > 0 && <span className="text-red-400 ml-2">Balance: ₹{bal.toLocaleString('en-IN')}</span>}
                          </p>
                        </div>
                        <span className={cn('text-[10px] px-2 py-0.5 rounded-full font-medium flex-shrink-0', cfg.color)}>{cfg.label}</span>
                        <ChevronRight className="w-3.5 h-3.5 text-white/20 flex-shrink-0" />
                      </button>
                    );
                  })}
                </div>
              </div>
            )}

            {/* ── Step 2: Payment Details ── */}
            {step === 2 && student && (
              <div className="space-y-4">
                {/* Student summary */}
                <div className="glass rounded-xl p-3 flex items-center gap-3">
                  <div className="w-8 h-8 rounded-full bg-surface-100 flex items-center justify-center text-xs font-bold text-white/60 flex-shrink-0">
                    {student.studentName.charAt(0).toUpperCase()}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-white">{student.studentName}</p>
                    <p className="text-xs text-white/40">
                      Total Fee: ₹{totalFeePerStu.toLocaleString('en-IN')} · Paid: ₹{student.totalPaid.toLocaleString('en-IN')}
                      {balance > 0 && <span className="text-red-400"> · Balance: ₹{balance.toLocaleString('en-IN')}</span>}
                    </p>
                  </div>
                  <button onClick={() => setStep(1)} className="text-xs text-brand-400 hover:text-brand-300 flex-shrink-0">Change</button>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  {/* Amount */}
                  <div className="col-span-2">
                    <label className="text-xs text-white/40 mb-1 block">Amount Paid (₹) *</label>
                    <div className="relative">
                      <IndianRupee className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-white/30" />
                      <input type="number" placeholder={balance > 0 ? String(balance) : '0.00'}
                        value={amount} onChange={e => setAmount(e.target.value)}
                        className="w-full pl-9 pr-4 py-2.5 bg-white/5 border border-white/10 rounded-xl text-sm text-white focus:outline-none focus:border-brand-500/40" />
                    </div>
                  </div>
                  {/* Payment Method */}
                  <div>
                    <label className="text-xs text-white/40 mb-1 block">Method</label>
                    <select value={method} onChange={e => setMethod(e.target.value)}
                      className="w-full px-3 py-2.5 bg-white/5 border border-white/10 rounded-xl text-sm text-white focus:outline-none focus:border-brand-500/40">
                      {['CASH', 'ONLINE', 'UPI', 'CHEQUE', 'BANK_TRANSFER', 'CARD'].map(m => (
                        <option key={m} value={m} className="bg-surface-50">{m.replace('_', ' ')}</option>
                      ))}
                    </select>
                  </div>
                  {/* Fee Type */}
                  <div>
                    <label className="text-xs text-white/40 mb-1 block">Fee Type</label>
                    <select value={feeType} onChange={e => setFeeType(e.target.value)}
                      className="w-full px-3 py-2.5 bg-white/5 border border-white/10 rounded-xl text-sm text-white focus:outline-none focus:border-brand-500/40">
                      {['TUITION', 'EXAM_FEE', 'TRANSPORT', 'HOSTEL', 'LIBRARY', 'LAB', 'OTHER'].map(t => (
                        <option key={t} value={t} className="bg-surface-50">{t.replace('_', ' ')}</option>
                      ))}
                    </select>
                  </div>
                  {/* Reference Number */}
                  <div className="col-span-2">
                    <label className="text-xs text-white/40 mb-1 block">Reference Number *</label>
                    <input type="text" value={refNo} onChange={e => setRefNo(e.target.value)}
                      className="w-full px-3 py-2.5 bg-white/5 border border-white/10 rounded-xl text-sm text-white focus:outline-none focus:border-brand-500/40" />
                  </div>
                  {/* Date */}
                  <div>
                    <label className="text-xs text-white/40 mb-1 block">Payment Date *</label>
                    <input type="date" value={date} onChange={e => setDate(e.target.value)}
                      className="w-full px-3 py-2.5 bg-white/5 border border-white/10 rounded-xl text-sm text-white focus:outline-none focus:border-brand-500/40" />
                  </div>
                  {/* Remarks */}
                  <div>
                    <label className="text-xs text-white/40 mb-1 block">Remarks</label>
                    <input type="text" placeholder="Optional note…" value={remarks} onChange={e => setRemarks(e.target.value)}
                      className="w-full px-3 py-2.5 bg-white/5 border border-white/10 rounded-xl text-sm text-white focus:outline-none focus:border-brand-500/40 placeholder:text-white/20" />
                  </div>
                </div>

                {/* AI preview */}
                {numAmount > 0 && (
                  <div className="flex items-start gap-2.5 p-3 bg-brand-500/5 border border-brand-500/15 rounded-xl">
                    <Sparkles className="w-3.5 h-3.5 text-brand-400 mt-0.5 flex-shrink-0" />
                    <p className="text-xs text-white/60 leading-relaxed">
                      <span className="text-brand-400 font-medium">AI Preview: </span>
                      Recording ₹{numAmount.toLocaleString('en-IN')} for {student.studentName}.
                      After payment: Total paid = ₹{afterPaid.toLocaleString('en-IN')}.
                      Status will be <span className={cn('font-semibold', newStatusColor)}>{newStatus}</span>.
                      {afterPaid >= totalFeePerStu && totalFeePerStu > 0 && ' 🎉 Full fee cleared!'}
                      {afterPaid < totalFeePerStu && totalFeePerStu > 0 && ` Remaining balance: ₹${Math.max(0, totalFeePerStu - afterPaid).toLocaleString('en-IN')}.`}
                    </p>
                  </div>
                )}

                <div className="flex gap-3 pt-1">
                  <button onClick={() => setStep(1)} className="flex-1 py-2.5 rounded-xl border border-white/10 text-white/50 hover:text-white hover:bg-white/5 text-sm transition-colors">
                    Back
                  </button>
                  <button onClick={() => setStep(3)} disabled={!amount || !refNo || !date || numAmount <= 0}
                    className="flex-1 py-2.5 rounded-xl bg-brand-600 hover:bg-brand-500 text-white text-sm font-semibold transition-all disabled:opacity-40 flex items-center justify-center gap-2">
                    Review <ChevronRight className="w-4 h-4" />
                  </button>
                </div>
              </div>
            )}

            {/* ── Step 3: Confirm ── */}
            {step === 3 && student && (
              <div className="space-y-4">
                <div className="glass rounded-xl p-4 space-y-3">
                  <h3 className="text-sm font-semibold text-white">Payment Summary</h3>
                  {[
                    { label: 'Student',          value: student.studentName },
                    { label: 'Amount',            value: `₹${numAmount.toLocaleString('en-IN')}` },
                    { label: 'Method',            value: method.replace('_', ' ')   },
                    { label: 'Fee Type',          value: feeType.replace('_', ' ')  },
                    { label: 'Reference',         value: refNo                       },
                    { label: 'Date',              value: new Date(date).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) },
                    remarks ? { label: 'Remarks', value: remarks } : null,
                  ].filter(Boolean).map(row => (
                    <div key={row!.label} className="flex justify-between text-sm">
                      <span className="text-white/40">{row!.label}</span>
                      <span className="text-white font-medium">{row!.value}</span>
                    </div>
                  ))}
                </div>

                <div className="flex items-start gap-2.5 p-3 bg-emerald-500/5 border border-emerald-500/15 rounded-xl">
                  <Sparkles className="w-3.5 h-3.5 text-emerald-400 mt-0.5 flex-shrink-0" />
                  <p className="text-xs text-white/60 leading-relaxed">
                    <span className="text-emerald-400 font-medium">AI Confirmation: </span>
                    Payment will be auto-confirmed. Status → <span className={cn('font-semibold', newStatusColor)}>{newStatus}</span>.
                    Parent will receive an in-app notification immediately.
                  </p>
                </div>

                <div className="flex gap-3">
                  <button onClick={() => setStep(2)} className="flex-1 py-2.5 rounded-xl border border-white/10 text-white/50 hover:text-white hover:bg-white/5 text-sm transition-colors">Back</button>
                  <button onClick={handleSubmit} disabled={submitting}
                    className="flex-1 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-sm font-semibold flex items-center justify-center gap-2 transition-all disabled:opacity-50">
                    {submitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle2 className="w-4 h-4" />}
                    {submitting ? 'Recording…' : 'Confirm & Record'}
                  </button>
                </div>
              </div>
            )}
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
  const [showWizard, setShowWizard]       = useState(false);
  const [aiReport, setAiReport]           = useState('');
  const [auditSearch, setAuditSearch]     = useState('');

  // Autopilot state
  const [aiScanResult, setAiScanResult]   = useState('');
  const [aiScanLoading, setAiScanLoading] = useState(false);
  const [schedule, setSchedule]           = useState({ day0: true, day3: true, day7: true, day14: false, day30: false });
  const [executing, setExecuting]         = useState(false);

  // ── Queries ──────────────────────────────────────────────────────────────

  const { data: centers = [] } = useQuery<{ id: string; name: string }[]>({
    queryKey: ['centers'],
    queryFn: () => api.get('/api/v1/centers').then(r => Array.isArray(r.data) ? r.data : (r.data.content ?? [])),
    staleTime: 5 * 60 * 1000,
  });
  const centerId   = centers[0]?.id   ?? '';
  const centerName = centers[0]?.name ?? 'Center';

  const { data: report = [], isLoading: reportLoading, refetch: refetchReport } = useQuery<StudentFeeReportItem[]>({
    queryKey: ['billing-report', centerId],
    queryFn: () => api.get(`/api/v1/parents/billing/report?centerId=${centerId}`).then(r => Array.isArray(r.data) ? r.data : []),
    enabled: !!centerId,
  });

  const { data: fees = [] } = useQuery<FeeStructure[]>({
    queryKey: ['fee-structures', centerId],
    queryFn: () => api.get(`/api/v1/centers/${centerId}/fees?size=100`).then(r =>
      Array.isArray(r.data) ? r.data : (r.data.content ?? [])),
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

  // ── Derived financial values ──────────────────────────────────────────────

  const activeFees          = fees.filter(f => f.status === 'ACTIVE');
  const totalFeePerStu      = activeFees.reduce((s, f) => s + f.amount, 0);
  const expectedRevenue     = totalStudents * totalFeePerStu;
  const revenueGap          = Math.max(0, expectedRevenue - totalCollected);
  const collectionEfficiency = expectedRevenue > 0 ? Math.round((totalCollected / expectedRevenue) * 100) : 0;

  const nextDueDate = (() => {
    const f = activeFees.find(x => x.dueDay > 0);
    if (!f) return '—';
    const today = new Date();
    const d = new Date(today.getFullYear(), today.getMonth(), f.dueDay);
    if (d <= today) d.setMonth(d.getMonth() + 1);
    return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: '2-digit' });
  })();

  const getBalance = (item: StudentFeeReportItem) =>
    totalFeePerStu > 0 ? Math.max(0, totalFeePerStu - item.totalPaid) : 0;

  // ── Mutations ─────────────────────────────────────────────────────────────

  const [sending, setSending] = useState<string | null>(null);
  const singleReminder = useMutation({
    mutationFn: (studentId: string) =>
      api.post(`/api/v1/parents/billing/reminder/${studentId}?centerId=${centerId}`),
    onMutate: id => setSending(id),
    onSuccess: (_, id) => {
      toast.success(`Reminder sent to ${report.find(s => s.studentId === id)?.studentName ?? 'parent'}'s parent.`);
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
      const prompt = `You are a school finance AI analyst. Analyse fee collection for ${centerName}:
Total Students: ${totalStudents} | Fully Paid: ${fullyPaid} (${collectionRate}%) | Partial: ${partial} | No Payment: ${noPayment}
Total Collected: ₹${totalCollected.toLocaleString('en-IN')} | Expected Revenue: ₹${expectedRevenue.toLocaleString('en-IN')} | Gap: ₹${revenueGap.toLocaleString('en-IN')}
Provide a 3-paragraph executive summary: collection health, at-risk analysis, and 3 specific actions. Be concise and professional.`;
      const res = await api.post('/api/v1/ai/completions', { prompt, maxTokens: 400 });
      setAiReport(res.data?.content ?? res.data?.text ?? 'AI analysis unavailable.');
    } catch {
      setAiReport('AI analysis temporarily unavailable.');
    } finally {
      setAiLoading(false);
    }
  }

  // ── AI Autopilot ──────────────────────────────────────────────────────────

  async function runAiScan() {
    if (!totalStudents) { toast.info('No students to scan.'); return; }
    setAiScanLoading(true);
    try {
      const prompt = `You are a school billing AI. Scan this fee data for ${centerName} and output a structured action plan:

CRITICAL (No Payment): ${noPayment} students
AT RISK (Partial Payment): ${partial} students
HEALTHY (Fully Paid): ${fullyPaid} students
Revenue Gap: ₹${revenueGap.toLocaleString('en-IN')}
Next Due Date: ${nextDueDate}

For each group, provide:
1. Urgency level (🔴/🟡/🟢)
2. Specific recommended action
3. Expected outcome

Then provide one overall recommendation for the admin to act on TODAY. Format clearly with headers.`;
      const res = await api.post('/api/v1/ai/completions', { prompt, maxTokens: 500 });
      setAiScanResult(res.data?.content ?? res.data?.text ?? 'Scan unavailable.');
    } catch {
      setAiScanResult('AI scan temporarily unavailable.');
    } finally {
      setAiScanLoading(false);
    }
  }

  async function executeAllRecommendations() {
    const targets = report.filter(s => s.paymentStatus !== 'FULLY_PAID');
    if (targets.length === 0) { toast.info('No pending students to notify.'); return; }
    setExecuting(true);
    let sent = 0;
    for (const s of targets) {
      try { await api.post(`/api/v1/parents/billing/reminder/${s.studentId}?centerId=${centerId}`); sent++; } catch { /* best-effort */ }
    }
    setExecuting(false);
    toast.success(`AI Autopilot executed: ${sent} notifications sent to parents.`);
  }

  // ── Collections helpers ───────────────────────────────────────────────────

  const filteredReport = colFilter === 'ALL' ? report : report.filter(s => s.paymentStatus === colFilter);

  const toggleSelect = (id: string) => setSelected(prev => {
    const next = new Set(prev);
    next.has(id) ? next.delete(id) : next.add(id);
    return next;
  });
  const toggleAll = () => setSelected(prev =>
    prev.size === filteredReport.length ? new Set() : new Set(filteredReport.map(s => s.studentId))
  );

  // ── Audit helpers ─────────────────────────────────────────────────────────

  const auditRows = report
    .filter(s => auditSearch === '' || s.studentName.toLowerCase().includes(auditSearch.toLowerCase()))
    .sort((a, b) => ({ NO_PAYMENT: 0, PARTIAL: 1, FULLY_PAID: 2 }[a.paymentStatus] - { NO_PAYMENT: 0, PARTIAL: 1, FULLY_PAID: 2 }[b.paymentStatus]));

  function exportAuditCsv() {
    const header = 'Student Name,Status,Total Fee,Paid,Balance,Due Date,AI Recommendation,Payment Count\n';
    const rows   = auditRows.map(r =>
      `"${r.studentName}",${STATUS_CFG[r.paymentStatus].label},${totalFeePerStu},${r.totalPaid},${getBalance(r)},${nextDueDate},${AI_ACTION[r.paymentStatus].label},${r.paymentCount}`
    ).join('\n');
    const blob = new Blob([header + rows], { type: 'text/csv' });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a'); a.href = url;
    a.download = `accounts-audit-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click(); URL.revokeObjectURL(url);
  }

  // ─────────────────────────────────────────────────────────────────────────

  return (
    <div className="p-4 lg:p-6 space-y-6 max-w-7xl mx-auto">

      {/* ── Page Header ──────────────────────────────────────────────────── */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-brand-500/10 border border-brand-500/20">
            <Wallet className="w-5 h-5 text-brand-400" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-white flex items-center gap-2">
              Finance & Accounts
              <span className="text-[10px] font-medium px-2 py-0.5 rounded-full bg-brand-500/15 text-brand-400 border border-brand-500/25">AI-Powered</span>
            </h1>
            <p className="text-sm text-white/40">Billing · Collections · AI Autopilot · Reports</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={() => { refetchReport(); toast.info('Data refreshed.'); }}
            className="p-2 rounded-xl border border-white/10 hover:bg-white/5 text-white/40 hover:text-white/70 transition-colors">
            <RefreshCw className="w-4 h-4" />
          </button>
          <button onClick={() => { setActiveTab('billing'); setShowWizard(true); }}
            className="flex items-center gap-2 px-4 py-2 bg-brand-600 hover:bg-brand-500 text-white rounded-xl text-sm font-semibold transition-all">
            <PlusCircle className="w-4 h-4" /> Create Bill
          </button>
        </div>
      </div>

      {/* ── Tabs ─────────────────────────────────────────────────────────── */}
      <div className="flex flex-wrap gap-1 p-1 bg-white/5 rounded-xl border border-white/5 w-fit max-w-full">
        {TABS.map(({ id, label, icon: Icon }) => (
          <button key={id} onClick={() => setActiveTab(id)}
            className={cn('flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all',
              activeTab === id ? 'bg-brand-600 text-white shadow-lg shadow-brand-500/20' : 'text-white/50 hover:text-white hover:bg-white/5')}>
            <Icon className="w-4 h-4" />
            <span className="hidden sm:inline">{label}</span>
            {id === 'autopilot' && atRisk > 0 && (
              <span className="w-4 h-4 rounded-full bg-red-500 text-white text-[9px] font-bold flex items-center justify-center flex-shrink-0">{atRisk}</span>
            )}
          </button>
        ))}
      </div>

      {/* ══════════════════════════════════════════════════════════════════════ */}
      {/* TAB 1 — OVERVIEW                                                       */}
      {/* ══════════════════════════════════════════════════════════════════════ */}
      {activeTab === 'overview' && (
        <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">

          {/* KPI row */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            <KpiCard label="Total Collected" icon={IndianRupee} color="text-brand-400" bg="bg-brand-500/10"
              value={`₹${(totalCollected / 1000).toFixed(1)}k`} sub={`${totalStudents} students`} trend="neutral" />
            <KpiCard label="Fully Paid" icon={CheckCircle2} color="text-emerald-400" bg="bg-emerald-500/10"
              value={fullyPaid} sub={`${collectionRate}% of total`} trend={collectionRate >= 75 ? 'up' : 'down'} />
            <KpiCard label="Outstanding" icon={AlertCircle} color="text-amber-400" bg="bg-amber-500/10"
              value={partial} sub="partial payments" trend="neutral" />
            <KpiCard label="At Risk" icon={ShieldAlert} color="text-red-400" bg="bg-red-500/10"
              value={noPayment} sub="no payment received" trend={noPayment > 0 ? 'down' : 'up'} />
          </div>

          {/* Ring + AI Intelligence */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="glass rounded-2xl p-6 flex flex-col items-center justify-between gap-6">
              <CollectionRing rate={collectionRate} />
              <div className="w-full space-y-3">
                {[
                  { label: 'Fully Paid',  value: fullyPaid,  color: 'bg-emerald-500' },
                  { label: 'Installment', value: partial,    color: 'bg-amber-500'   },
                  { label: 'No Payment',  value: noPayment,  color: 'bg-red-500'     },
                ].map(({ label, value, color }) => (
                  <div key={label} className="flex items-center gap-3">
                    <div className={cn('w-2 h-2 rounded-full flex-shrink-0', color)} />
                    <div className="flex-1 flex justify-between items-center">
                      <span className="text-xs text-white/50">{label}</span>
                      <span className="text-xs font-semibold text-white">{value}</span>
                    </div>
                    <div className="w-16 h-1.5 bg-white/5 rounded-full overflow-hidden">
                      <div className={cn('h-full rounded-full', color)}
                        style={{ width: totalStudents > 0 ? `${(value / totalStudents) * 100}%` : '0%' }} />
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="lg:col-span-2 glass rounded-2xl p-6 flex flex-col gap-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-brand-400" />
                  <h3 className="text-sm font-semibold text-white">AI Revenue Intelligence</h3>
                </div>
                <button onClick={generateAiReport} disabled={aiLoading}
                  className="flex items-center gap-1.5 px-3 py-1.5 bg-brand-500/10 hover:bg-brand-500/20 text-brand-400 rounded-lg text-xs font-medium border border-brand-500/20 disabled:opacity-50 transition-colors">
                  {aiLoading ? <Loader2 className="w-3 h-3 animate-spin" /> : <Zap className="w-3 h-3" />}
                  {aiReport ? 'Regenerate' : 'Generate Report'}
                </button>
              </div>
              {aiLoading ? (
                <div className="flex-1 flex flex-col items-center justify-center gap-3 py-8 text-white/30">
                  <Loader2 className="w-6 h-6 animate-spin text-brand-400" />
                  <p className="text-sm">Analysing financial data…</p>
                </div>
              ) : !aiReport ? (
                <div className="flex-1 flex flex-col items-center justify-center gap-3 py-8 border-2 border-dashed border-white/5 rounded-xl text-white/20">
                  <BarChart3 className="w-8 h-8" />
                  <div className="text-center">
                    <p className="text-sm text-white/40">Click "Generate Report" for AI insights</p>
                    <p className="text-xs mt-1">Revenue trends · Risk analysis · Recommendations</p>
                  </div>
                </div>
              ) : (
                <div className="flex-1 bg-brand-500/5 border border-brand-500/10 rounded-xl p-4">
                  <p className="text-sm text-white/70 leading-relaxed whitespace-pre-wrap">{aiReport}</p>
                </div>
              )}
            </div>
          </div>

          {/* Gap Analysis */}
          {expectedRevenue > 0 && (
            <div className="glass rounded-2xl p-5">
              <div className="flex items-center gap-2 mb-4">
                <BarChart3 className="w-4 h-4 text-brand-400" />
                <h3 className="text-sm font-semibold text-white">Revenue Gap Analysis</h3>
                <span className={cn('ml-auto text-xs font-medium px-2 py-0.5 rounded-full',
                  collectionEfficiency >= 80 ? 'bg-emerald-500/10 text-emerald-400' :
                  collectionEfficiency >= 50 ? 'bg-amber-500/10 text-amber-400' :
                  'bg-red-500/10 text-red-400')}>{collectionEfficiency}% efficiency</span>
              </div>
              <div className="grid grid-cols-3 gap-4">
                {[
                  { label: 'Expected Revenue', value: `₹${(expectedRevenue / 1000).toFixed(1)}k`, sub: `${totalStudents} × ₹${(totalFeePerStu / 1000).toFixed(1)}k`, color: 'text-white/70', pct: 100 as number, bar: 'bg-white/20' },
                  { label: 'Collected',         value: `₹${(totalCollected / 1000).toFixed(1)}k`,  sub: `${collectionRate}% collected`,              color: 'text-emerald-400', pct: collectionEfficiency,   bar: 'bg-emerald-500' },
                  { label: 'Revenue Gap',       value: `₹${(revenueGap / 1000).toFixed(1)}k`,      sub: revenueGap === 0 ? 'Fully collected!' : `${atRisk} pending`, color: revenueGap === 0 ? 'text-emerald-400' : 'text-red-400', pct: Math.round((revenueGap / expectedRevenue) * 100), bar: 'bg-red-500' },
                ].map(({ label, value, sub, color, pct, bar }) => (
                  <div key={label} className="flex flex-col gap-2">
                    <p className="text-xs text-white/40 uppercase tracking-wide">{label}</p>
                    <p className={cn('text-xl font-bold', color)}>{value}</p>
                    <p className="text-xs text-white/30">{sub}</p>
                    <div className="h-1 bg-white/5 rounded-full overflow-hidden">
                      <motion.div className={cn('h-full rounded-full', bar)}
                        initial={{ width: '0%' }} animate={{ width: `${pct}%` }}
                        transition={{ duration: 0.8, ease: 'easeOut' }} />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Quick Campaign Actions */}
          <div className="glass rounded-2xl p-5">
            <div className="flex items-center gap-2 mb-4">
              <Zap className="w-4 h-4 text-brand-400" />
              <h3 className="text-sm font-semibold text-white">Quick Campaign Actions</h3>
              <span className="text-xs text-white/30 ml-auto">Reach parents instantly</span>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              {(Object.entries(CAMPAIGN_CFG) as [CampaignType, typeof CAMPAIGN_CFG[CampaignType]][]).map(([type, cfg]) => (
                <button key={type} onClick={() => setCampaign(type)} disabled={!centerId}
                  className={cn('flex items-center gap-3 p-4 rounded-xl text-left transition-all hover:scale-[1.01] active:scale-[0.99] disabled:opacity-40', cfg.bg)}>
                  <div className="p-2 rounded-lg bg-black/20"><cfg.icon className={cn('w-4 h-4', cfg.color)} /></div>
                  <div>
                    <p className={cn('text-sm font-semibold', cfg.color)}>{cfg.label}</p>
                    <p className="text-xs text-white/40 mt-0.5">
                      {type === 'reminder' ? `${noPayment} pending` : type === 'warning' ? `${noPayment} overdue` : `${partial} partial`}
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
                <button onClick={() => setActiveTab('billing')} className="text-xs text-brand-400 hover:text-brand-300">View all →</button>
              </div>
              <div className="divide-y divide-white/5">
                {report.filter(s => s.paymentStatus !== 'FULLY_PAID').slice(0, 5).map(s => {
                  const risk = RISK_CFG[STATUS_CFG[s.paymentStatus].risk];
                  return (
                    <div key={s.studentId} className="flex items-center gap-4 px-5 py-3">
                      <div className="w-8 h-8 rounded-full bg-surface-100 flex items-center justify-center text-xs font-bold text-white/70 flex-shrink-0">
                        {s.studentName.charAt(0).toUpperCase()}
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm text-white font-medium truncate">{s.studentName}</p>
                        <p className="text-xs text-white/40">{s.totalPaid > 0 ? `₹${s.totalPaid.toLocaleString('en-IN')} paid` : 'No payment received'}</p>
                      </div>
                      <div className="flex items-center gap-1.5 flex-shrink-0">
                        <div className={cn('w-1.5 h-1.5 rounded-full', risk.dot)} />
                        <span className={cn('text-xs font-medium', risk.color)}>{risk.label}</span>
                      </div>
                      <button onClick={() => singleReminder.mutate(s.studentId)} disabled={sending === s.studentId}
                        className="flex-shrink-0 p-1.5 rounded-lg bg-brand-500/10 text-brand-400 hover:bg-brand-500/20 transition-colors disabled:opacity-50" title="Send reminder">
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

      {/* ══════════════════════════════════════════════════════════════════════ */}
      {/* TAB 2 — BILLING HUB                                                    */}
      {/* ══════════════════════════════════════════════════════════════════════ */}
      {activeTab === 'billing' && (
        <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} className="space-y-5">

          {/* How-to guide + action */}
          <div className="glass rounded-2xl p-5">
            <div className="flex flex-col sm:flex-row sm:items-center gap-4 sm:gap-6">
              <div className="flex-1">
                <h3 className="text-sm font-semibold text-white flex items-center gap-2">
                  <PlusCircle className="w-4 h-4 text-brand-400" />
                  How to Create a Bill
                </h3>
                <div className="flex items-center gap-3 mt-3 flex-wrap">
                  {[
                    { step: '1', label: 'Click Create Bill' },
                    { step: '2', label: 'Search & select student' },
                    { step: '3', label: 'Enter payment details' },
                    { step: '4', label: 'AI previews status change' },
                    { step: '5', label: 'Confirm — parent notified' },
                  ].map(({ step, label }, i) => (
                    <div key={step} className="flex items-center gap-2">
                      <div className="w-5 h-5 rounded-full bg-brand-500/20 text-brand-400 flex items-center justify-center text-[10px] font-bold flex-shrink-0">{step}</div>
                      <span className="text-xs text-white/50">{label}</span>
                      {i < 4 && <ArrowRight className="w-3 h-3 text-white/15 flex-shrink-0" />}
                    </div>
                  ))}
                </div>
              </div>
              <button onClick={() => setShowWizard(true)}
                className="flex-shrink-0 flex items-center gap-2 px-5 py-2.5 bg-brand-600 hover:bg-brand-500 text-white rounded-xl text-sm font-semibold transition-all">
                <PlusCircle className="w-4 h-4" /> Create Bill
              </button>
            </div>
          </div>

          {/* Filter pills */}
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex flex-wrap gap-1.5">
              {(['ALL', 'FULLY_PAID', 'PARTIAL', 'NO_PAYMENT'] as ColFilter[]).map(f => {
                const count = f === 'ALL' ? totalStudents : f === 'FULLY_PAID' ? fullyPaid : f === 'PARTIAL' ? partial : noPayment;
                return (
                  <button key={f} onClick={() => setColFilter(f)}
                    className={cn('px-3 py-1.5 rounded-lg text-xs font-medium transition-colors',
                      colFilter === f ? 'bg-brand-500/15 text-brand-400 border border-brand-500/30' : 'bg-white/5 text-white/50 hover:text-white hover:bg-white/10')}>
                    {f === 'ALL' ? 'All Students' : STATUS_CFG[f].label}
                    <span className="ml-1 opacity-60">({count})</span>
                  </button>
                );
              })}
            </div>
            {selected.size > 0 && (
              <button onClick={() => setCampaign('reminder')}
                className="flex items-center gap-1.5 px-3 py-1.5 bg-brand-600 hover:bg-brand-500 text-white rounded-lg text-xs font-medium transition-colors">
                <Send className="w-3.5 h-3.5" /> Remind {selected.size} selected
              </button>
            )}
          </div>

          {/* Billing table */}
          {reportLoading ? (
            <div className="flex items-center justify-center py-16"><Loader2 className="w-6 h-6 text-brand-400 animate-spin" /></div>
          ) : filteredReport.length === 0 ? (
            <div className="glass rounded-2xl p-12 text-center">
              <Users className="w-10 h-10 text-white/20 mx-auto mb-3" />
              <p className="text-white/40 text-sm">No students in this category.</p>
              <button onClick={() => setShowWizard(true)} className="mt-4 flex items-center gap-2 px-4 py-2 bg-brand-600 text-white rounded-xl text-sm font-medium mx-auto">
                <PlusCircle className="w-4 h-4" /> Create First Bill
              </button>
            </div>
          ) : (
            <div className="glass rounded-2xl overflow-x-auto">
              <table className="w-full text-sm min-w-[720px]">
                <thead>
                  <tr className="border-b border-white/5">
                    <th className="px-4 py-3 w-8">
                      <input type="checkbox" checked={selected.size === filteredReport.length && filteredReport.length > 0}
                        onChange={toggleAll} className="rounded border-white/20 bg-white/5 text-brand-500" />
                    </th>
                    <th className="px-4 py-3 text-left text-xs text-white/30 uppercase tracking-wider">Student</th>
                    <th className="px-4 py-3 text-right text-xs text-white/30 uppercase tracking-wider">Total Fee</th>
                    <th className="px-4 py-3 text-right text-xs text-white/30 uppercase tracking-wider">Paid</th>
                    <th className="px-4 py-3 text-right text-xs text-white/30 uppercase tracking-wider">Balance</th>
                    <th className="px-4 py-3 text-center text-xs text-white/30 uppercase tracking-wider hidden lg:table-cell">Due Date</th>
                    <th className="px-4 py-3 text-left text-xs text-white/30 uppercase tracking-wider hidden md:table-cell">AI Recommendation</th>
                    <th className="px-4 py-3 text-right text-xs text-white/30 uppercase tracking-wider">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5">
                  {filteredReport.map((item, i) => {
                    const bal  = getBalance(item);
                    const ai   = AI_ACTION[item.paymentStatus];
                    const AiIcon = ai.icon;
                    return (
                      <motion.tr key={item.studentId}
                        initial={{ opacity: 0, y: 6 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.02 }}
                        className={cn('hover:bg-white/[0.02] transition-colors', selected.has(item.studentId) && 'bg-brand-500/5')}>
                        <td className="px-4 py-3">
                          <input type="checkbox" checked={selected.has(item.studentId)} onChange={() => toggleSelect(item.studentId)}
                            className="rounded border-white/20 bg-white/5 text-brand-500" />
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-2.5">
                            <div className="w-7 h-7 rounded-full bg-surface-100 flex items-center justify-center text-xs font-bold text-white/60 flex-shrink-0">
                              {item.studentName.charAt(0).toUpperCase()}
                            </div>
                            <span className="font-medium text-white truncate max-w-[140px]">{item.studentName}</span>
                          </div>
                        </td>
                        <td className="px-4 py-3 text-right text-white/50 text-sm">
                          {totalFeePerStu > 0 ? `₹${totalFeePerStu.toLocaleString('en-IN')}` : '—'}
                        </td>
                        <td className="px-4 py-3 text-right">
                          <span className={cn('font-semibold text-sm', item.totalPaid > 0 ? 'text-emerald-400' : 'text-white/30')}>
                            {item.totalPaid > 0 ? `₹${item.totalPaid.toLocaleString('en-IN')}` : '—'}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-right">
                          {bal === 0
                            ? <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-emerald-500/10 text-emerald-400"><CheckCircle2 className="w-3 h-3" /> Nil</span>
                            : <span className="font-bold text-red-400 text-sm">₹{bal.toLocaleString('en-IN')}</span>
                          }
                        </td>
                        <td className="px-4 py-3 text-center hidden lg:table-cell">
                          <span className="text-xs text-white/40">{nextDueDate}</span>
                        </td>
                        <td className="px-4 py-3 hidden md:table-cell">
                          <span className={cn('inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-medium border', ai.color, ai.bg)}>
                            <AiIcon className="w-3 h-3" /> {ai.label}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex items-center justify-end gap-1.5">
                            <button onClick={() => setInvoiceStudent(item)}
                              className="p-1.5 rounded-lg bg-white/5 text-white/40 hover:text-white hover:bg-white/10 transition-colors" title="View Invoice">
                              <FileText className="w-3.5 h-3.5" />
                            </button>
                            {item.paymentStatus !== 'FULLY_PAID' && (
                              <>
                                <button onClick={() => { setShowWizard(true); }}
                                  className="p-1.5 rounded-lg bg-emerald-500/10 text-emerald-400 hover:bg-emerald-500/20 transition-colors" title="Record Payment">
                                  <PlusCircle className="w-3.5 h-3.5" />
                                </button>
                                <button onClick={() => singleReminder.mutate(item.studentId)} disabled={sending === item.studentId}
                                  className="p-1.5 rounded-lg bg-brand-500/10 text-brand-400 hover:bg-brand-500/20 transition-colors disabled:opacity-50" title="Send Reminder">
                                  {sending === item.studentId ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Send className="w-3.5 h-3.5" />}
                                </button>
                              </>
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

      {/* ══════════════════════════════════════════════════════════════════════ */}
      {/* TAB 3 — AI AUTOPILOT                                                   */}
      {/* ══════════════════════════════════════════════════════════════════════ */}
      {activeTab === 'autopilot' && (
        <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">

          {/* Hero scan panel */}
          <div className="glass rounded-2xl p-6">
            <div className="flex flex-col sm:flex-row sm:items-center gap-5">
              <div className="flex items-center gap-3">
                <div className="p-3 rounded-xl bg-brand-500/10 border border-brand-500/20">
                  <Bot className="w-6 h-6 text-brand-400" />
                </div>
                <div>
                  <h2 className="text-base font-bold text-white">AI Billing Autopilot</h2>
                  <p className="text-xs text-white/40 mt-0.5">
                    AI scans all students · identifies unpaid balances · sends targeted notifications automatically
                  </p>
                </div>
              </div>
              <button onClick={runAiScan} disabled={aiScanLoading}
                className="flex-shrink-0 flex items-center gap-2 px-5 py-2.5 bg-brand-600 hover:bg-brand-500 text-white rounded-xl text-sm font-semibold transition-all disabled:opacity-60">
                {aiScanLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Wand2 className="w-4 h-4" />}
                {aiScanLoading ? 'Scanning…' : 'Run AI Scan'}
              </button>
            </div>
          </div>

          {/* Current status grid */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            {[
              { label: 'Critical',    count: noPayment, color: 'text-red-400',     bg: 'bg-red-500/10 border-red-500/20',         dot: 'bg-red-500',     desc: 'No payment received — urgent action needed' },
              { label: 'At Risk',     count: partial,   color: 'text-amber-400',   bg: 'bg-amber-500/10 border-amber-500/20',     dot: 'bg-amber-500',   desc: 'Partial payment — follow-up recommended' },
              { label: 'Healthy',     count: fullyPaid, color: 'text-emerald-400', bg: 'bg-emerald-500/10 border-emerald-500/20', dot: 'bg-emerald-500', desc: 'Fully paid — no action needed' },
            ].map(({ label, count, color, bg, dot, desc }) => (
              <div key={label} className={cn('glass rounded-2xl p-5 border', bg)}>
                <div className="flex items-center gap-2 mb-2">
                  <div className={cn('w-2 h-2 rounded-full', dot)} />
                  <span className={cn('text-sm font-semibold', color)}>{label}</span>
                </div>
                <p className={cn('text-3xl font-bold', color)}>{count}</p>
                <p className="text-xs text-white/40 mt-1">{desc}</p>
              </div>
            ))}
          </div>

          {/* AI scan result */}
          {(aiScanLoading || aiScanResult) && (
            <div className="glass rounded-2xl p-6">
              <div className="flex items-center gap-2 mb-4">
                <Sparkles className="w-4 h-4 text-brand-400" />
                <h3 className="text-sm font-semibold text-white">AI Scan Results</h3>
              </div>
              {aiScanLoading ? (
                <div className="flex items-center gap-3 py-6 justify-center text-white/40">
                  <Loader2 className="w-5 h-5 animate-spin text-brand-400" />
                  <p className="text-sm">AI is analysing {totalStudents} student records…</p>
                </div>
              ) : (
                <div className="bg-brand-500/5 border border-brand-500/10 rounded-xl p-4">
                  <p className="text-sm text-white/70 leading-relaxed whitespace-pre-wrap">{aiScanResult}</p>
                </div>
              )}
            </div>
          )}

          {/* Notification schedule */}
          <div className="glass rounded-2xl p-6">
            <div className="flex items-center gap-2 mb-5">
              <CalendarClock className="w-4 h-4 text-brand-400" />
              <h3 className="text-sm font-semibold text-white">Auto-Notification Schedule</h3>
              <span className="text-xs text-white/30 ml-auto">Toggles control which notifications auto-execute</span>
            </div>
            <div className="space-y-3">
              {[
                { key: 'day0'  as keyof typeof schedule, icon: CalendarClock, label: 'On Due Date',        desc: 'Send "Payment Due Today" reminder to all pending students',                          color: 'text-white/70'   },
                { key: 'day3'  as keyof typeof schedule, icon: BellRing,      label: 'Day +3 Follow-up',   desc: 'Gentle follow-up for students who missed the due date',                              color: 'text-brand-400'  },
                { key: 'day7'  as keyof typeof schedule, icon: AlertCircle,   label: 'Day +7 Warning',     desc: 'Warning notice for overdue payments — tone becomes urgent',                         color: 'text-amber-400'  },
                { key: 'day14' as keyof typeof schedule, icon: ShieldAlert,   label: 'Day +14 Escalation', desc: 'Escalation notice — enrollment impact advisory sent to parent',                     color: 'text-red-400'    },
                { key: 'day30' as keyof typeof schedule, icon: AlertTriangle, label: 'Day +30 Final',      desc: 'Final notice before fee account is flagged for management review',                  color: 'text-red-500'    },
              ].map(({ key, icon: Icon, label, desc, color }) => (
                <div key={key} className={cn('flex items-start gap-4 p-4 rounded-xl transition-colors', schedule[key] ? 'bg-brand-500/5 border border-brand-500/10' : 'bg-white/2 border border-white/5')}>
                  <div className={cn('p-2 rounded-lg flex-shrink-0 mt-0.5', schedule[key] ? 'bg-brand-500/10' : 'bg-white/5')}>
                    <Icon className={cn('w-4 h-4', schedule[key] ? color : 'text-white/20')} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className={cn('text-sm font-medium', schedule[key] ? 'text-white' : 'text-white/40')}>{label}</p>
                    <p className="text-xs text-white/30 mt-0.5">{desc}</p>
                  </div>
                  <button
                    onClick={() => setSchedule(prev => ({ ...prev, [key]: !prev[key] }))}
                    className={cn('w-10 h-5 rounded-full transition-all flex-shrink-0 relative mt-1',
                      schedule[key] ? 'bg-brand-500' : 'bg-white/10')}>
                    <span className={cn('absolute top-0.5 w-4 h-4 rounded-full bg-white shadow-sm transition-all',
                      schedule[key] ? 'left-5' : 'left-0.5')} />
                  </button>
                </div>
              ))}
            </div>
          </div>

          {/* Execute panel */}
          <div className="glass rounded-2xl p-6">
            <div className="flex items-center gap-2 mb-4">
              <Play className="w-4 h-4 text-brand-400" />
              <h3 className="text-sm font-semibold text-white">Execute AI Recommendations</h3>
            </div>
            <p className="text-sm text-white/50 mb-5">
              AI will immediately send notifications to all <span className="text-white font-medium">{atRisk} at-risk students</span>' parents based on their payment status and the schedule above.
              Healthy students ({fullyPaid}) are automatically excluded.
            </p>
            <div className="flex flex-col sm:flex-row gap-3">
              <button onClick={() => setCampaign('reminder')} disabled={noPayment === 0}
                className="flex-1 flex items-center justify-center gap-2 py-3 rounded-xl bg-brand-500/10 border border-brand-500/20 text-brand-400 hover:bg-brand-500/20 text-sm font-medium transition-colors disabled:opacity-30">
                <BellRing className="w-4 h-4" /> Send Reminders ({noPayment})
              </button>
              <button onClick={() => setCampaign('warning')} disabled={partial === 0}
                className="flex-1 flex items-center justify-center gap-2 py-3 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-400 hover:bg-amber-500/20 text-sm font-medium transition-colors disabled:opacity-30">
                <AlertCircle className="w-4 h-4" /> Send Warnings ({partial})
              </button>
              <button onClick={executeAllRecommendations} disabled={executing || atRisk === 0}
                className="flex-1 flex items-center justify-center gap-2 py-3 rounded-xl bg-brand-600 hover:bg-brand-500 text-white text-sm font-semibold transition-all disabled:opacity-40">
                {executing ? <Loader2 className="w-4 h-4 animate-spin" /> : <Zap className="w-4 h-4" />}
                {executing ? 'Executing…' : `Execute All (${atRisk})`}
              </button>
            </div>
          </div>
        </motion.div>
      )}

      {/* ══════════════════════════════════════════════════════════════════════ */}
      {/* TAB 4 — FEE STRUCTURES                                                 */}
      {/* ══════════════════════════════════════════════════════════════════════ */}
      {activeTab === 'structures' && (
        <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}>
          <AdminFeesPage embedded />
        </motion.div>
      )}

      {/* ══════════════════════════════════════════════════════════════════════ */}
      {/* TAB 5 — AUDIT & REPORTS                                                */}
      {/* ══════════════════════════════════════════════════════════════════════ */}
      {activeTab === 'audit' && (
        <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} className="space-y-5">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="relative">
              <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-white/30" />
              <input type="text" placeholder="Search students…" value={auditSearch} onChange={e => setAuditSearch(e.target.value)}
                className="pl-9 pr-4 py-2 bg-white/5 border border-white/8 rounded-xl text-sm text-white placeholder:text-white/25 focus:outline-none focus:border-brand-500/40 w-full sm:w-56" />
            </div>
            <button onClick={exportAuditCsv}
              className="flex items-center gap-2 px-4 py-2 bg-white/5 hover:bg-white/10 border border-white/8 text-white/70 hover:text-white rounded-xl text-sm font-medium transition-colors">
              <Download className="w-4 h-4" /> Export CSV
            </button>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {[
              { label: 'Total Records',    value: totalStudents,                              color: 'text-white'         },
              { label: 'Collection Rate',  value: `${collectionRate}%`,                       color: collectionRate >= 75 ? 'text-emerald-400' : 'text-amber-400' },
              { label: 'Total Collected',  value: `₹${totalCollected.toLocaleString('en-IN')}`, color: 'text-brand-400'  },
              { label: 'Action Required',  value: atRisk,                                     color: atRisk > 0 ? 'text-red-400' : 'text-emerald-400' },
            ].map(({ label, value, color }) => (
              <div key={label} className="glass rounded-xl p-4">
                <p className={cn('text-lg font-bold', color)}>{value}</p>
                <p className="text-xs text-white/40 mt-0.5">{label}</p>
              </div>
            ))}
          </div>

          <div className="glass rounded-2xl overflow-x-auto">
            <div className="flex items-center justify-between px-5 py-4 border-b border-white/5">
              <div className="flex items-center gap-2">
                <ListChecks className="w-4 h-4 text-brand-400" />
                <h3 className="text-sm font-semibold text-white">Financial Audit Log</h3>
              </div>
              <span className="text-xs text-white/30">{auditRows.length} records</span>
            </div>
            {reportLoading ? (
              <div className="flex items-center justify-center py-12"><Loader2 className="w-6 h-6 text-brand-400 animate-spin" /></div>
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
                    <th className="px-5 py-3 text-left text-xs text-white/30 uppercase tracking-wider">Status</th>
                    <th className="px-5 py-3 text-right text-xs text-white/30 uppercase tracking-wider hidden sm:table-cell">Total Fee</th>
                    <th className="px-5 py-3 text-right text-xs text-white/30 uppercase tracking-wider hidden md:table-cell">Paid</th>
                    <th className="px-5 py-3 text-right text-xs text-white/30 uppercase tracking-wider hidden md:table-cell">Balance</th>
                    <th className="px-5 py-3 text-right text-xs text-white/30 uppercase tracking-wider">Invoice</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5">
                  {auditRows.map((item, i) => {
                    const cfg = STATUS_CFG[item.paymentStatus];
                    const Icon = cfg.icon;
                    const bal = getBalance(item);
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
                            <Icon className="w-3 h-3" /> {cfg.label}
                          </span>
                        </td>
                        <td className="px-5 py-3 text-right text-white/50 hidden sm:table-cell">
                          {totalFeePerStu > 0 ? `₹${totalFeePerStu.toLocaleString('en-IN')}` : '—'}
                        </td>
                        <td className="px-5 py-3 text-right text-white/60 hidden md:table-cell">
                          {item.totalPaid > 0 ? `₹${item.totalPaid.toLocaleString('en-IN')}` : <span className="text-white/25">—</span>}
                        </td>
                        <td className="px-5 py-3 text-right hidden md:table-cell">
                          {bal === 0
                            ? <span className="text-xs text-emerald-400">Nil</span>
                            : <span className="text-xs font-semibold text-red-400">₹{bal.toLocaleString('en-IN')}</span>}
                        </td>
                        <td className="px-5 py-3 text-right">
                          <button onClick={() => setInvoiceStudent(item)}
                            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white/5 hover:bg-white/10 text-white/50 hover:text-white text-xs font-medium transition-colors">
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

      {/* ── Modals ─────────────────────────────────────────────────────────── */}

      {invoiceStudent && (
        <InvoiceModal student={invoiceStudent} fees={fees.filter(f => f.status === 'ACTIVE')}
          totalFeePerStu={totalFeePerStu} onClose={() => setInvoiceStudent(null)} />
      )}

      {campaign && (
        <CampaignModal type={campaign} targets={report} centerId={centerId}
          onClose={() => setCampaign(null)}
          onSent={count => { toast.success(`Campaign sent to ${count} parent${count !== 1 ? 's' : ''}.`); setCampaign(null); }} />
      )}

      {showWizard && (
        <BillingWizard students={report} centerId={centerId} totalFeePerStu={totalFeePerStu}
          fees={fees.filter(f => f.status === 'ACTIVE')}
          onClose={() => setShowWizard(false)}
          onSuccess={() => { setShowWizard(false); refetchReport(); qc.invalidateQueries({ queryKey: ['billing-report', centerId] }); }} />
      )}
    </div>
  );
}

