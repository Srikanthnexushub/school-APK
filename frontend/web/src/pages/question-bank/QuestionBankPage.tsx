// Centralized Question Bank — accessible to all roles (STUDENT, PARENT, TEACHER, CENTER_ADMIN, INSTITUTION_ADMIN, SUPER_ADMIN)
// Features: Online (AI) / Offline (static) mode · Print · PDF download · Reports tab

import { useState, useCallback, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  BookOpen, Loader2, Eye, EyeOff, Download, Sparkles, FileText,
  ChevronDown, ChevronUp, CheckCircle2, Printer, Wifi, WifiOff,
  BarChart2, Settings2, BookMarked, ClipboardList, TrendingUp, Trophy,
  RefreshCw,
} from 'lucide-react';
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend,
} from 'recharts';
import { cn } from '../../lib/utils';
import { toast } from 'sonner';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { getOfflineQuestions, type OfflineDifficulty, type OfflineQuestion } from '../../data/offlineQuestions';

// ─── Constants ────────────────────────────────────────────────────────────────

const CHAPTERS_BY_SUBJECT: Record<string, string[]> = {
  Physics: ['Mechanics', 'Thermodynamics', 'Electrostatics', 'Magnetism', 'Optics', 'Modern Physics'],
  Chemistry: ['Organic Chemistry', 'Inorganic Chemistry', 'Physical Chemistry', 'Electrochemistry'],
  Mathematics: ['Algebra', 'Calculus', 'Trigonometry', 'Coordinate Geometry', 'Probability', 'Statistics'],
  Biology: ['Cell Biology', 'Genetics', 'Ecology', 'Human Physiology', 'Plant Biology', 'Evolution'],
  English: ['Reading Comprehension', 'Grammar', 'Writing Skills', 'Vocabulary', 'Literature'],
  'Social Science': ['History', 'Geography', 'Civics', 'Economics'],
};

const SUBJECTS = Object.keys(CHAPTERS_BY_SUBJECT);
const TARGET_EXAMS = ['JEE Main', 'JEE Advanced', 'NEET', 'CBSE Board', 'ICSE Board'];
const DIFFICULTIES = ['Low', 'Medium', 'High'] as const;
type Difficulty = typeof DIFFICULTIES[number];
const QUESTION_COUNTS = [10, 20, 30] as const;
type QuestionCount = typeof QUESTION_COUNTS[number];
type Mode = 'online' | 'offline';
type Tab = 'generator' | 'reports';

const DIFFICULTY_COLORS: Record<string, string> = {
  Low: '#10b981',
  Medium: '#f59e0b',
  High: '#ef4444',
};

const SUBJECT_COLORS = ['#6366f1', '#22d3ee', '#f59e0b', '#10b981', '#f43f5e', '#8b5cf6'];

// ─── Role-aware description ───────────────────────────────────────────────────

function getRoleDescription(role?: string): string {
  switch (role) {
    case 'PARENT':           return 'Generate AI-powered practice papers for your child — by subject, chapter and difficulty.';
    case 'TEACHER':          return 'Generate practice question papers for your students by subject, chapter and difficulty.';
    case 'CENTER_ADMIN':
    case 'INSTITUTION_ADMIN':
    case 'SUPER_ADMIN':      return 'Generate curriculum-aligned question papers for your centre — by subject, chapter and difficulty.';
    default:                 return 'Generate AI-powered practice papers tailored to your target exam and weak areas.';
  }
}

// ─── Types ────────────────────────────────────────────────────────────────────

interface MCQQuestion {
  q: string;
  options: string[];
  answer: string;
  explanation: string;
  chapter: string;
}

interface GeneratedPaper {
  id: string;
  subject: string;
  targetExam: string;
  difficulty: Difficulty;
  chapters: string[];
  questions: MCQQuestion[];
  generatedAt: string;
  mode: Mode;
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleString('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });
  } catch { return iso; }
}

function extractJsonArray(text: string): MCQQuestion[] | null {
  try {
    const parsed = JSON.parse(text);
    if (Array.isArray(parsed)) return parsed as MCQQuestion[];
  } catch { /* continue */ }
  const match = text.match(/\[[\s\S]*\]/);
  if (match) {
    try {
      const parsed = JSON.parse(match[0]);
      if (Array.isArray(parsed)) return parsed as MCQQuestion[];
    } catch { /* continue */ }
  }
  return null;
}

function generatePrintableHTML(paper: GeneratedPaper): string {
  const questionsHTML = paper.questions.map((q, i) => `
    <div style="margin-bottom:20px;page-break-inside:avoid">
      <p style="font-weight:600;margin:0 0 8px 0;font-size:13px">Q${i + 1}. ${q.q}</p>
      <ul style="list-style:none;margin:0;padding:0">
        ${(q.options ?? []).map((o) => `<li style="margin:4px 0;font-size:12px;padding-left:8px">${o}</li>`).join('')}
      </ul>
      ${q.chapter ? `<p style="font-size:10px;color:#888;margin:4px 0 0 0">Chapter: ${q.chapter}</p>` : ''}
    </div>
  `).join('');

  const answerKeyHTML = paper.questions.map((q, i) =>
    `<p style="margin:4px 0;font-size:12px"><strong>Q${i + 1}:</strong> ${q.answer} — ${q.explanation}</p>`
  ).join('');

  return `<!DOCTYPE html><html><head><meta charset="utf-8">
    <title>${paper.subject} — ${paper.targetExam} Question Paper</title>
    <style>
      body { font-family: Arial, sans-serif; max-width: 800px; margin: 40px auto; color: #111; }
      h1 { font-size: 18px; margin-bottom: 4px; }
      .meta { font-size: 12px; color: #555; margin-bottom: 20px; }
      .divider { border: 1px solid #ccc; margin: 20px 0; }
      .answer-key { background: #f5f5f5; padding: 16px; border-radius: 4px; margin-top: 20px; }
      @media print { body { margin: 20px; } }
    </style>
    </head><body>
    <h1>${paper.subject} — ${paper.targetExam}</h1>
    <div class="meta">
      Difficulty: ${paper.difficulty} &nbsp;|&nbsp;
      Chapters: ${paper.chapters.join(', ')} &nbsp;|&nbsp;
      Questions: ${paper.questions.length} &nbsp;|&nbsp;
      Generated: ${formatDate(paper.generatedAt)}
      ${paper.mode === 'offline' ? ' &nbsp;|&nbsp; <em>Offline Mode</em>' : ''}
    </div>
    <hr class="divider">
    ${questionsHTML}
    <div class="answer-key">
      <h2 style="font-size:14px;margin:0 0 12px 0">ANSWER KEY</h2>
      ${answerKeyHTML}
    </div>
  </body></html>`;
}

function generateDownloadText(paper: GeneratedPaper): string {
  const lines: string[] = [
    'QUESTION PAPER',
    `Subject: ${paper.subject} | Exam: ${paper.targetExam} | Difficulty: ${paper.difficulty}`,
    `Chapters: ${paper.chapters.join(', ')}`,
    `Total Questions: ${paper.questions.length} | Generated: ${formatDate(paper.generatedAt)}`,
    paper.mode === 'offline' ? '[Offline Mode — pre-loaded question bank]' : '[Online Mode — AI-generated]',
    '',
    '='.repeat(60),
    '',
    ...paper.questions.flatMap((q, i) => [
      `Q${i + 1}. ${q.q}`,
      ...(q.options ?? []).map((o) => `   ${o}`),
      '',
    ]),
    '='.repeat(60),
    'ANSWER KEY',
    '',
    ...paper.questions.map((q, i) => `Q${i + 1}: ${q.answer} — ${q.explanation}`),
  ];
  return lines.join('\n');
}

// ─── Question Card ────────────────────────────────────────────────────────────

function QuestionCard({ question, index, showAnswers }: { question: MCQQuestion; index: number; showAnswers: boolean }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.025 }}
      className="card border border-white/5 print:border-gray-200 print:shadow-none print:bg-white print:text-black"
    >
      <div className="flex items-start gap-3">
        <span className="text-brand-400 font-bold text-sm flex-shrink-0 w-6 text-right print:text-black">
          {index + 1}.
        </span>
        <div className="flex-1 min-w-0">
          <p className="text-sm text-white leading-relaxed mb-3 print:text-black">{question.q}</p>
          <div className="space-y-2">
            {(question.options ?? []).map((opt, oi) => {
              const optLetter = opt.trim()[0]?.toUpperCase();
              const isCorrect = showAnswers && optLetter === question.answer?.toUpperCase();
              return (
                <div
                  key={oi}
                  className={cn(
                    'flex items-start gap-2.5 px-3 py-2.5 rounded-xl border text-sm transition-colors print:border-gray-300',
                    isCorrect
                      ? 'border-emerald-500/30 bg-emerald-500/5 text-emerald-300 print:border-emerald-600 print:text-emerald-800 print:bg-emerald-50'
                      : 'border-white/5 text-white/60 print:text-gray-700'
                  )}
                >
                  {isCorrect && <CheckCircle2 className="w-4 h-4 text-emerald-400 flex-shrink-0 mt-0.5 print:text-emerald-600" />}
                  <span>{opt}</span>
                </div>
              );
            })}
          </div>
          {showAnswers && question.explanation && (
            <div className="mt-3 p-3 bg-brand-500/5 border border-brand-500/15 rounded-xl print:bg-gray-50 print:border-gray-200">
              <span className="text-xs font-semibold text-brand-400 print:text-gray-700">Explanation: </span>
              <span className="text-xs text-white/60 print:text-gray-600">{question.explanation}</span>
            </div>
          )}
          {question.chapter && (
            <div className="mt-2 text-xs text-white/25 print:text-gray-400">Chapter: {question.chapter}</div>
          )}
        </div>
      </div>
    </motion.div>
  );
}

// ─── Paper Display ────────────────────────────────────────────────────────────

function PaperDisplay({ paper }: { paper: GeneratedPaper }) {
  const [showAnswers, setShowAnswers] = useState(false);
  const printRef = useRef<HTMLDivElement>(null);

  function handleDownloadTxt() {
    const text = generateDownloadText(paper);
    const blob = new Blob([text], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${paper.subject}-${paper.difficulty}-${Date.now()}.txt`;
    a.click();
    URL.revokeObjectURL(url);
    toast.success('Question paper downloaded as text.');
  }

  function handleDownloadPdf() {
    const html = generatePrintableHTML(paper);
    const blob = new Blob([html], { type: 'text/html' });
    const url = URL.createObjectURL(blob);
    const win = window.open(url, '_blank');
    if (win) {
      win.onload = () => {
        setTimeout(() => {
          win.print();
          URL.revokeObjectURL(url);
        }, 500);
      };
    } else {
      toast.error('Pop-ups blocked — please allow pop-ups for PDF download.');
    }
    toast.info('Print dialog opened — choose "Save as PDF" to download.');
  }

  function handlePrint() {
    if (printRef.current) {
      const originalBody = document.body.innerHTML;
      const content = printRef.current.innerHTML;
      document.body.innerHTML = `
        <style>
          body { font-family: Arial, sans-serif; color: #111; margin: 24px; }
          .qcard { margin-bottom: 20px; page-break-inside: avoid; border: 1px solid #ddd; padding: 12px; border-radius: 6px; }
          .opt { padding: 6px 10px; border: 1px solid #ccc; border-radius: 4px; margin: 4px 0; font-size: 12px; }
          .correct { background: #f0fdf4; border-color: #16a34a; }
          .exp { background: #f8fafc; border: 1px solid #e2e8f0; padding: 8px; border-radius: 4px; margin-top: 8px; font-size: 11px; }
          .no-print { display: none !important; }
        </style>
        ${content}
      `;
      window.print();
      document.body.innerHTML = originalBody;
      window.location.reload();
    }
  }

  return (
    <motion.div ref={printRef} initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} className="space-y-4">
      {/* Paper header */}
      <div className="card border border-brand-500/20 print:border-gray-300 print:bg-white">
        <div className="flex items-start justify-between gap-4 flex-wrap">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <h3 className="font-bold text-white text-base print:text-black">
                {paper.subject} — {paper.targetExam}
              </h3>
              <span className={cn(
                'text-[10px] px-2 py-0.5 rounded-full font-medium flex items-center gap-1',
                paper.mode === 'offline'
                  ? 'bg-violet-500/15 text-violet-400 print:bg-violet-100 print:text-violet-700'
                  : 'bg-sky-500/15 text-sky-400 print:bg-sky-100 print:text-sky-700'
              )}>
                {paper.mode === 'offline' ? <WifiOff className="w-2.5 h-2.5" /> : <Wifi className="w-2.5 h-2.5" />}
                {paper.mode === 'offline' ? 'Offline' : 'AI-Generated'}
              </span>
            </div>
            <div className="flex items-center gap-3 mt-1 flex-wrap">
              <span className={cn(
                'text-xs px-2 py-0.5 rounded-full font-medium',
                paper.difficulty === 'High' ? 'bg-red-500/15 text-red-400' :
                paper.difficulty === 'Medium' ? 'bg-amber-500/15 text-amber-400' :
                'bg-emerald-500/15 text-emerald-400'
              )}>
                {paper.difficulty} Difficulty
              </span>
              <span className="text-xs text-white/40 print:text-gray-500">{paper.questions.length} Questions</span>
              <span className="text-xs text-white/30 print:text-gray-400">{formatDate(paper.generatedAt)}</span>
            </div>
            <p className="text-xs text-white/30 mt-1 print:text-gray-400">Chapters: {paper.chapters.join(', ')}</p>
          </div>

          {/* Action buttons — hidden when printing via @media print */}
          <div className="flex items-center gap-2 flex-wrap no-print">
            <button
              onClick={() => setShowAnswers((p) => !p)}
              className={cn(
                'flex items-center gap-2 px-3 py-2 rounded-xl text-sm border transition-colors',
                showAnswers
                  ? 'border-brand-500/30 bg-brand-500/10 text-brand-400'
                  : 'border-white/10 text-white/50 hover:text-white hover:border-white/20'
              )}
            >
              {showAnswers ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              {showAnswers ? 'Hide Answers' : 'Show Answers'}
            </button>
            <button
              onClick={handlePrint}
              className="flex items-center gap-2 px-3 py-2 rounded-xl text-sm border border-white/10 text-white/50 hover:text-white hover:border-white/20 transition-colors"
              title="Print question paper"
            >
              <Printer className="w-4 h-4" />
              Print
            </button>
            <button
              onClick={handleDownloadPdf}
              className="flex items-center gap-2 px-3 py-2 rounded-xl text-sm border border-indigo-500/20 bg-indigo-500/5 text-indigo-400 hover:bg-indigo-500/10 transition-colors"
              title="Download as PDF"
            >
              <Download className="w-4 h-4" />
              PDF
            </button>
            <button
              onClick={handleDownloadTxt}
              className="flex items-center gap-2 px-3 py-2 rounded-xl text-sm border border-white/10 text-white/50 hover:text-white hover:border-white/20 transition-colors"
              title="Download as plain text"
            >
              <FileText className="w-4 h-4" />
              .txt
            </button>
          </div>
        </div>
      </div>

      {/* Questions */}
      <div className="space-y-3">
        {paper.questions.map((q, i) => (
          <QuestionCard key={i} question={q} index={i} showAnswers={showAnswers} />
        ))}
      </div>
    </motion.div>
  );
}

// ─── Reports Tab ──────────────────────────────────────────────────────────────

function ReportsTab({ papers }: { papers: GeneratedPaper[] }) {
  if (papers.length === 0) {
    return (
      <div className="card text-center py-16 border border-dashed border-white/5">
        <BarChart2 className="w-10 h-10 text-white/15 mx-auto mb-3" />
        <p className="text-white/40 text-sm">No papers generated yet.</p>
        <p className="text-white/25 text-xs mt-1">Switch to the Generator tab and create your first paper.</p>
      </div>
    );
  }

  const totalQuestions = papers.reduce((s, p) => s + p.questions.length, 0);
  const uniqueSubjects = [...new Set(papers.map((p) => p.subject))];
  const onlinePapers = papers.filter((p) => p.mode === 'online').length;
  const offlinePapers = papers.filter((p) => p.mode === 'offline').length;

  // Bar chart: questions per subject
  const subjectData = uniqueSubjects.map((subj) => ({
    name: subj.length > 8 ? subj.slice(0, 7) + '…' : subj,
    questions: papers.filter((p) => p.subject === subj).reduce((s, p) => s + p.questions.length, 0),
    papers: papers.filter((p) => p.subject === subj).length,
  }));

  // Pie chart: difficulty distribution
  const difficultyData = DIFFICULTIES.map((d) => ({
    name: d,
    value: papers.filter((p) => p.difficulty === d).reduce((s, p) => s + p.questions.length, 0),
  })).filter((d) => d.value > 0);

  // Mode distribution
  const modeData = [
    { name: 'AI Online', value: onlinePapers, color: '#38bdf8' },
    { name: 'Offline', value: offlinePapers, color: '#a78bfa' },
  ].filter((d) => d.value > 0);

  return (
    <div className="space-y-6">
      {/* KPI row */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        {[
          { icon: ClipboardList, label: 'Papers Generated', value: papers.length, color: 'bg-brand-500/15 text-brand-400' },
          { icon: BookMarked, label: 'Total Questions', value: totalQuestions, color: 'bg-emerald-500/15 text-emerald-400' },
          { icon: TrendingUp, label: 'Subjects Covered', value: uniqueSubjects.length, color: 'bg-amber-500/15 text-amber-400' },
          { icon: Trophy, label: 'Avg Q / Paper', value: papers.length > 0 ? Math.round(totalQuestions / papers.length) : 0, color: 'bg-violet-500/15 text-violet-400' },
        ].map(({ icon: Icon, label, value, color }, i) => (
          <motion.div
            key={label}
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.05 }}
            className="card border border-white/5"
          >
            <div className={cn('w-9 h-9 rounded-xl flex items-center justify-center mb-3', color)}>
              <Icon className="w-4 h-4" />
            </div>
            <div className="text-2xl font-bold text-white">{value}</div>
            <div className="text-xs text-white/40 mt-0.5">{label}</div>
          </motion.div>
        ))}
      </div>

      {/* Charts row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Questions by subject */}
        <div className="card border border-white/5">
          <h3 className="text-sm font-semibold text-white mb-4 flex items-center gap-2">
            <BarChart2 className="w-4 h-4 text-brand-400" />
            Questions by Subject
          </h3>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={subjectData} margin={{ top: 0, right: 8, left: -20, bottom: 0 }}>
              <XAxis dataKey="name" tick={{ fill: '#ffffff60', fontSize: 11 }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fill: '#ffffff40', fontSize: 10 }} axisLine={false} tickLine={false} />
              <Tooltip
                contentStyle={{ background: '#1a1a2e', border: '1px solid #ffffff15', borderRadius: 8, fontSize: 12 }}
                labelStyle={{ color: '#fff' }}
                itemStyle={{ color: '#a5b4fc' }}
              />
              <Bar dataKey="questions" fill="#6366f1" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Difficulty distribution */}
        <div className="card border border-white/5">
          <h3 className="text-sm font-semibold text-white mb-4 flex items-center gap-2">
            <TrendingUp className="w-4 h-4 text-amber-400" />
            Difficulty Distribution
          </h3>
          <ResponsiveContainer width="100%" height={200}>
            <PieChart>
              <Pie
                data={difficultyData}
                cx="50%"
                cy="50%"
                innerRadius={50}
                outerRadius={80}
                paddingAngle={3}
                dataKey="value"
              >
                {difficultyData.map((entry) => (
                  <Cell key={entry.name} fill={DIFFICULTY_COLORS[entry.name] ?? '#6366f1'} />
                ))}
              </Pie>
              <Tooltip
                contentStyle={{ background: '#1a1a2e', border: '1px solid #ffffff15', borderRadius: 8, fontSize: 12 }}
                itemStyle={{ color: '#fff' }}
              />
              <Legend
                iconType="circle"
                iconSize={8}
                formatter={(value) => <span style={{ color: '#ffffff80', fontSize: 12 }}>{value}</span>}
              />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Mode split + paper history */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Mode split */}
        <div className="card border border-white/5">
          <h3 className="text-sm font-semibold text-white mb-4 flex items-center gap-2">
            <Wifi className="w-4 h-4 text-sky-400" />
            Generation Mode
          </h3>
          <ResponsiveContainer width="100%" height={160}>
            <PieChart>
              <Pie data={modeData} cx="50%" cy="50%" outerRadius={60} dataKey="value">
                {modeData.map((entry) => (
                  <Cell key={entry.name} fill={entry.color} />
                ))}
              </Pie>
              <Tooltip
                contentStyle={{ background: '#1a1a2e', border: '1px solid #ffffff15', borderRadius: 8, fontSize: 12 }}
                itemStyle={{ color: '#fff' }}
              />
              <Legend
                iconType="circle"
                iconSize={8}
                formatter={(value) => <span style={{ color: '#ffffff80', fontSize: 11 }}>{value}</span>}
              />
            </PieChart>
          </ResponsiveContainer>
        </div>

        {/* Paper history table */}
        <div className="lg:col-span-2 card border border-white/5">
          <h3 className="text-sm font-semibold text-white mb-4 flex items-center gap-2">
            <FileText className="w-4 h-4 text-white/40" />
            Paper History ({papers.length})
          </h3>
          <div className="space-y-2">
            {papers.map((p, i) => (
              <div
                key={p.id}
                className="flex items-center gap-3 p-2.5 rounded-xl bg-white/[0.03] border border-white/5 text-sm"
              >
                <span className="text-white/30 text-xs w-5 text-right flex-shrink-0">{i + 1}</span>
                <div className="flex-1 min-w-0">
                  <div className="text-white/80 truncate">{p.subject} — {p.targetExam}</div>
                  <div className="text-white/30 text-xs flex items-center gap-2 mt-0.5">
                    <span className={cn(
                      'px-1.5 py-0.5 rounded text-[10px] font-medium',
                      p.difficulty === 'High' ? 'bg-red-500/15 text-red-400' :
                      p.difficulty === 'Medium' ? 'bg-amber-500/15 text-amber-400' :
                      'bg-emerald-500/15 text-emerald-400'
                    )}>{p.difficulty}</span>
                    <span>{p.questions.length}Q</span>
                    <span>{p.mode === 'offline' ? '· Offline' : '· AI'}</span>
                    <span>{formatDate(p.generatedAt)}</span>
                  </div>
                </div>
                {/* Per-paper actions */}
                <div className="flex items-center gap-1 flex-shrink-0">
                  <button
                    title="Print paper"
                    onClick={() => {
                      const html = generatePrintableHTML(p);
                      const win = window.open('', '_blank');
                      if (win) {
                        win.document.write(html);
                        win.document.close();
                        setTimeout(() => { win.print(); }, 400);
                      } else {
                        toast.error('Pop-ups blocked — allow pop-ups to print.');
                      }
                    }}
                    className="p-1.5 rounded-lg text-white/30 hover:text-white/70 hover:bg-white/5 transition-colors"
                  >
                    <Printer className="w-3.5 h-3.5" />
                  </button>
                  <button
                    title="Download PDF"
                    onClick={() => {
                      const html = generatePrintableHTML(p);
                      const blob = new Blob([html], { type: 'text/html' });
                      const url = URL.createObjectURL(blob);
                      const win = window.open(url, '_blank');
                      if (win) {
                        win.onload = () => { setTimeout(() => { win.print(); URL.revokeObjectURL(url); }, 500); };
                      } else {
                        toast.error('Pop-ups blocked — allow pop-ups for PDF download.');
                      }
                      toast.info('Choose "Save as PDF" in the print dialog.');
                    }}
                    className="p-1.5 rounded-lg text-indigo-400/50 hover:text-indigo-400 hover:bg-indigo-500/5 transition-colors"
                  >
                    <Download className="w-3.5 h-3.5" />
                  </button>
                  <button
                    title="Download as text"
                    onClick={() => {
                      const text = generateDownloadText(p);
                      const blob = new Blob([text], { type: 'text/plain' });
                      const url = URL.createObjectURL(blob);
                      const a = document.createElement('a');
                      a.href = url;
                      a.download = `${p.subject}-${p.difficulty}-${Date.now()}.txt`;
                      a.click();
                      URL.revokeObjectURL(url);
                      toast.success('Downloaded as text file.');
                    }}
                    className="p-1.5 rounded-lg text-white/30 hover:text-white/70 hover:bg-white/5 transition-colors"
                  >
                    <FileText className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────

export default function QuestionBankPage() {
  const user = useAuthStore((s) => s.user);
  const role = user?.role;

  // Form state
  const [subject, setSubject] = useState<string>('Physics');
  const [targetExam, setTargetExam] = useState<string>('JEE Main');
  const [selectedChapters, setSelectedChapters] = useState<string[]>([]);
  const [difficulty, setDifficulty] = useState<Difficulty>('Medium');
  const [numQuestions, setNumQuestions] = useState<QuestionCount>(10);
  const [mode, setMode] = useState<Mode>('online');

  // UI state
  const [generating, setGenerating] = useState(false);
  const [papers, setPapers] = useState<GeneratedPaper[]>([]);
  const [activePaperId, setActivePaperId] = useState<string | null>(null);
  const [showPrevious, setShowPrevious] = useState(false);
  const [activeTab, setActiveTab] = useState<Tab>('generator');

  const chapters = CHAPTERS_BY_SUBJECT[subject] ?? [];

  const toggleChapter = useCallback((ch: string) => {
    setSelectedChapters((prev) =>
      prev.includes(ch) ? prev.filter((c) => c !== ch) : [...prev, ch]
    );
  }, []);

  function handleSubjectChange(newSubject: string) {
    setSubject(newSubject);
    setSelectedChapters([]);
  }

  async function handleGenerate() {
    const chaptersToUse = selectedChapters.length > 0 ? selectedChapters : chapters;
    setGenerating(true);

    try {
      let questions: MCQQuestion[];

      if (mode === 'offline') {
        // Pull from static bundled question pool — no network call
        const raw = getOfflineQuestions(subject, chaptersToUse, difficulty as OfflineDifficulty, numQuestions);
        if (raw.length === 0) {
          toast.error('No offline questions available for this combination. Try a different subject or difficulty.');
          return;
        }
        questions = raw.map((q) => ({
          q: q.q,
          options: q.options,
          answer: q.answer,
          explanation: q.explanation,
          chapter: q.chapter,
        }));
        // Pad with available questions if fewer than requested
        if (questions.length < numQuestions) {
          toast.info(`${questions.length} questions available offline for this selection.`);
        }
      } else {
        // Online — call dedicated question generation endpoint (structured response + server-side fallback)
        const topic = `${subject}: ${chaptersToUse.join(', ')} (Target exam: ${targetExam})`;
        const response = await api.post('/api/v1/ai/generate-questions', {
          topic,
          difficulty: difficulty.toUpperCase(),
          count: numQuestions,
        });
        const generated: Array<{
          questionText: string;
          options: string[];
          correctAnswer: number;
          explanation: string;
          difficulty: string;
        }> = Array.isArray(response.data) ? response.data : [];
        if (generated.length === 0) {
          toast.error('No questions returned from AI. Please try again.');
          return;
        }
        const ANSWER_LETTERS = ['A', 'B', 'C', 'D'] as const;
        questions = generated.map((gq, idx) => ({
          q: gq.questionText,
          options: gq.options,
          answer: ANSWER_LETTERS[gq.correctAnswer] ?? 'A',
          explanation: gq.explanation,
          chapter: chaptersToUse[idx % chaptersToUse.length] ?? subject,
        }));
      }

      const paper: GeneratedPaper = {
        id: `paper-${Date.now()}`,
        subject,
        targetExam,
        difficulty,
        chapters: chaptersToUse,
        questions,
        generatedAt: new Date().toISOString(),
        mode,
      };

      setPapers((prev) => [paper, ...prev]);
      setActivePaperId(paper.id);
      toast.success(`Generated ${questions.length} questions successfully!`);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } }; message?: string };
      toast.error(err?.response?.data?.message ?? err?.message ?? 'Failed to generate questions.');
    } finally {
      setGenerating(false);
    }
  }

  const activePaper = papers.find((p) => p.id === activePaperId) ?? null;
  const previousPapers = papers.filter((p) => p.id !== activePaperId);

  return (
    <div className="p-4 lg:p-8 space-y-6 max-w-5xl mx-auto">
      {/* Page header */}
      <div className="flex items-start justify-between gap-4 flex-wrap">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <BookOpen className="w-6 h-6 text-brand-400" />
            Question Bank
          </h1>
          <p className="text-white/50 text-sm mt-0.5">{getRoleDescription(role)}</p>
        </div>
        {papers.length > 0 && (
          <div className="flex items-center gap-1.5 text-xs text-white/30">
            <RefreshCw className="w-3.5 h-3.5" />
            {papers.length} paper{papers.length !== 1 ? 's' : ''} this session
          </div>
        )}
      </div>

      {/* Tab selector */}
      <div className="flex items-center gap-1 bg-white/[0.03] rounded-xl p-1 border border-white/5 w-fit">
        {([
          { id: 'generator', label: 'Generator', icon: Settings2 },
          { id: 'reports',   label: 'Reports',   icon: BarChart2 },
        ] as const).map(({ id, label, icon: Icon }) => (
          <button
            key={id}
            onClick={() => setActiveTab(id)}
            className={cn(
              'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors',
              activeTab === id
                ? 'bg-brand-500 text-white shadow-sm'
                : 'text-white/40 hover:text-white/70'
            )}
          >
            <Icon className="w-4 h-4" />
            {label}
            {id === 'reports' && papers.length > 0 && (
              <span className="bg-brand-500/20 text-brand-400 text-[10px] px-1.5 py-0.5 rounded-full font-bold">
                {papers.length}
              </span>
            )}
          </button>
        ))}
      </div>

      <AnimatePresence mode="wait">
        {activeTab === 'generator' ? (
          <motion.div
            key="generator"
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            transition={{ duration: 0.2 }}
            className="space-y-6"
          >
            {/* ── Configuration card ── */}
            <div className="card border border-white/5 space-y-5">
              <div className="flex items-center justify-between gap-2 flex-wrap">
                <div className="flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-brand-400" />
                  <h3 className="font-semibold text-white text-sm">Configure Question Paper</h3>
                </div>

                {/* ── Mode toggle ── */}
                <div className="flex items-center gap-1 bg-white/[0.04] rounded-lg p-0.5 border border-white/5">
                  <button
                    onClick={() => setMode('online')}
                    className={cn(
                      'flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium transition-colors',
                      mode === 'online'
                        ? 'bg-sky-500/20 text-sky-300 border border-sky-500/30'
                        : 'text-white/40 hover:text-white/70'
                    )}
                  >
                    <Wifi className="w-3.5 h-3.5" />
                    Online (AI)
                  </button>
                  <button
                    onClick={() => setMode('offline')}
                    className={cn(
                      'flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium transition-colors',
                      mode === 'offline'
                        ? 'bg-violet-500/20 text-violet-300 border border-violet-500/30'
                        : 'text-white/40 hover:text-white/70'
                    )}
                  >
                    <WifiOff className="w-3.5 h-3.5" />
                    Offline
                  </button>
                </div>
              </div>

              {mode === 'offline' && (
                <div className="flex items-start gap-2 p-3 bg-violet-500/5 border border-violet-500/15 rounded-xl">
                  <WifiOff className="w-4 h-4 text-violet-400 flex-shrink-0 mt-0.5" />
                  <p className="text-xs text-violet-300/80 leading-relaxed">
                    <strong className="text-violet-300">Offline mode:</strong> Questions are drawn from a built-in question bank — no AI call or internet needed. Ideal for low-connectivity environments.
                  </p>
                </div>
              )}

              {/* Subject + Target Exam */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-white/60 mb-1.5">Subject</label>
                  <select
                    value={subject}
                    onChange={(e) => handleSubjectChange(e.target.value)}
                    className="input w-full"
                  >
                    {SUBJECTS.map((s) => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-medium text-white/60 mb-1.5">
                    Target Exam
                    {mode === 'offline' && <span className="text-white/30 ml-1">(used for labelling only)</span>}
                  </label>
                  <select
                    value={targetExam}
                    onChange={(e) => setTargetExam(e.target.value)}
                    className="input w-full"
                  >
                    {TARGET_EXAMS.map((ex) => (
                      <option key={ex} value={ex}>{ex}</option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Chapters */}
              <div>
                <label className="block text-xs font-medium text-white/60 mb-2">
                  Chapters
                  <span className="text-white/30 ml-1">
                    ({selectedChapters.length === 0 ? 'all' : selectedChapters.length} selected)
                  </span>
                </label>
                <div className="flex flex-wrap gap-2">
                  {chapters.map((ch) => (
                    <button
                      key={ch}
                      type="button"
                      onClick={() => toggleChapter(ch)}
                      className={cn(
                        'px-3 py-1.5 rounded-lg text-xs font-medium border transition-colors',
                        selectedChapters.includes(ch)
                          ? 'border-brand-500 bg-brand-500/15 text-brand-400'
                          : 'border-white/10 text-white/40 hover:border-white/20 hover:text-white/60'
                      )}
                    >
                      {ch}
                    </button>
                  ))}
                </div>
              </div>

              {/* Difficulty */}
              <div>
                <label className="block text-xs font-medium text-white/60 mb-2">Difficulty Level</label>
                <div className="flex gap-2 flex-wrap">
                  {DIFFICULTIES.map((d) => (
                    <button
                      key={d}
                      type="button"
                      onClick={() => setDifficulty(d)}
                      className={cn(
                        'px-4 py-2 rounded-xl text-sm font-medium border transition-colors',
                        difficulty === d
                          ? d === 'High'
                            ? 'border-red-500 bg-red-500/15 text-red-400'
                            : d === 'Medium'
                              ? 'border-amber-500 bg-amber-500/15 text-amber-400'
                              : 'border-emerald-500 bg-emerald-500/15 text-emerald-400'
                          : 'border-white/10 text-white/40 hover:border-white/20 hover:text-white/60'
                      )}
                    >
                      {d}
                    </button>
                  ))}
                </div>
              </div>

              {/* Number of questions */}
              <div>
                <label className="block text-xs font-medium text-white/60 mb-2">Number of Questions</label>
                <div className="flex gap-2 flex-wrap">
                  {QUESTION_COUNTS.map((n) => (
                    <button
                      key={n}
                      type="button"
                      onClick={() => setNumQuestions(n)}
                      className={cn(
                        'px-4 py-2 rounded-xl text-sm font-medium border transition-colors',
                        numQuestions === n
                          ? 'border-brand-500 bg-brand-500/15 text-brand-400'
                          : 'border-white/10 text-white/40 hover:border-white/20 hover:text-white/60'
                      )}
                    >
                      {n}
                    </button>
                  ))}
                </div>
              </div>

              {/* Generate button */}
              <div className="flex items-center gap-4">
                <button
                  onClick={handleGenerate}
                  disabled={generating}
                  className="btn-primary flex items-center gap-2 px-6 py-3 text-sm font-medium disabled:opacity-50"
                >
                  {generating ? (
                    <>
                      <Loader2 className="w-4 h-4 animate-spin" />
                      Generating…
                    </>
                  ) : mode === 'offline' ? (
                    <>
                      <WifiOff className="w-4 h-4" />
                      Generate Offline Paper
                    </>
                  ) : (
                    <>
                      <Sparkles className="w-4 h-4" />
                      Generate with AI
                    </>
                  )}
                </button>
                {generating && mode === 'online' && (
                  <p className="text-xs text-white/40 animate-pulse">
                    AI is writing your questions — this can take up to 45 seconds…
                  </p>
                )}
              </div>
            </div>

            {/* Active paper */}
            {activePaper && <PaperDisplay paper={activePaper} />}

            {/* Previous papers */}
            {previousPapers.length > 0 && (
              <div className="card border border-white/5">
                <button
                  onClick={() => setShowPrevious((p) => !p)}
                  className="flex items-center justify-between w-full"
                >
                  <div className="flex items-center gap-2">
                    <FileText className="w-4 h-4 text-white/40" />
                    <span className="text-sm font-semibold text-white">
                      Previous Papers ({previousPapers.length})
                    </span>
                  </div>
                  {showPrevious
                    ? <ChevronUp className="w-4 h-4 text-white/30" />
                    : <ChevronDown className="w-4 h-4 text-white/30" />}
                </button>

                <AnimatePresence initial={false}>
                  {showPrevious && (
                    <motion.div
                      initial={{ opacity: 0, height: 0 }}
                      animate={{ opacity: 1, height: 'auto' }}
                      exit={{ opacity: 0, height: 0 }}
                      transition={{ duration: 0.2 }}
                      className="overflow-hidden"
                    >
                      <div className="mt-4 space-y-2">
                        {previousPapers.map((p) => (
                          <button
                            key={p.id}
                            onClick={() => setActivePaperId(p.id)}
                            className="w-full flex items-center gap-3 p-3 rounded-xl bg-white/[0.03] border border-white/5 hover:border-white/10 transition-colors text-left"
                          >
                            <BookOpen className="w-4 h-4 text-white/30 flex-shrink-0" />
                            <div className="flex-1 min-w-0">
                              <div className="text-sm text-white">{p.subject} — {p.targetExam}</div>
                              <div className="text-xs text-white/30 mt-0.5 flex items-center gap-2">
                                <span>{p.difficulty}</span>
                                <span>·</span>
                                <span>{p.questions.length}Q</span>
                                <span>·</span>
                                <span>{p.mode === 'offline' ? 'Offline' : 'AI'}</span>
                                <span>·</span>
                                <span>{formatDate(p.generatedAt)}</span>
                              </div>
                            </div>
                          </button>
                        ))}
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            )}

            {/* Empty state */}
            {papers.length === 0 && !generating && (
              <div className="card text-center py-12 border border-dashed border-white/5">
                <Sparkles className="w-10 h-10 text-white/15 mx-auto mb-3" />
                <p className="text-white/40 text-sm">Configure and generate your first practice paper above.</p>
                <p className="text-white/25 text-xs mt-1">
                  {mode === 'offline' ? 'Offline mode — draws from built-in question bank.' : 'Online mode — powered by AI.'}
                </p>
              </div>
            )}
          </motion.div>
        ) : (
          <motion.div
            key="reports"
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            transition={{ duration: 0.2 }}
          >
            <ReportsTab papers={papers} />
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
