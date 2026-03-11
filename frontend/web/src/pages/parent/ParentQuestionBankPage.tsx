import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  BookOpen, Loader2, Eye, EyeOff, Download, Sparkles,
  FileText, ChevronDown, ChevronUp, CheckCircle2,
} from 'lucide-react';
import { cn } from '../../lib/utils';
import { toast } from 'sonner';
import api from '../../lib/api';

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
  difficulty: string;
  chapters: string[];
  questions: MCQQuestion[];
  generatedAt: string;
}

// ─── Chapter data ──────────────────────────────────────────────────────────────

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

// ─── Helpers ─────────────────────────────────────────────────────────────────

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleString('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });
  } catch {
    return iso;
  }
}

function extractJsonArray(text: string): MCQQuestion[] | null {
  // Try to parse as-is first
  try {
    const parsed = JSON.parse(text);
    if (Array.isArray(parsed)) return parsed as MCQQuestion[];
  } catch { /* continue */ }

  // Extract JSON array from markdown code block or prose
  const match = text.match(/\[[\s\S]*\]/);
  if (match) {
    try {
      const parsed = JSON.parse(match[0]);
      if (Array.isArray(parsed)) return parsed as MCQQuestion[];
    } catch { /* continue */ }
  }

  return null;
}

function generateDownloadText(paper: GeneratedPaper): string {
  const lines: string[] = [];
  lines.push(`QUESTION PAPER`);
  lines.push(`Subject: ${paper.subject} | Exam: ${paper.targetExam} | Difficulty: ${paper.difficulty}`);
  lines.push(`Chapters: ${paper.chapters.join(', ')}`);
  lines.push(`Total Questions: ${paper.questions.length} | Generated: ${formatDate(paper.generatedAt)}`);
  lines.push('');
  lines.push('=' .repeat(60));
  lines.push('');

  paper.questions.forEach((q, i) => {
    lines.push(`Q${i + 1}. ${q.q}`);
    q.options.forEach((opt) => lines.push(`   ${opt}`));
    lines.push('');
  });

  lines.push('=' .repeat(60));
  lines.push('ANSWER KEY');
  lines.push('');
  paper.questions.forEach((q, i) => {
    lines.push(`Q${i + 1}: ${q.answer} — ${q.explanation}`);
  });

  return lines.join('\n');
}

// ─── Question Card ────────────────────────────────────────────────────────────

function QuestionCard({
  question,
  index,
  showAnswers,
}: {
  question: MCQQuestion;
  index: number;
  showAnswers: boolean;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.03 }}
      className="card border border-white/5"
    >
      <div className="flex items-start gap-3">
        <span className="text-brand-400 font-bold text-sm flex-shrink-0 w-6 text-right">
          {index + 1}.
        </span>
        <div className="flex-1 min-w-0">
          <p className="text-sm text-white leading-relaxed mb-3">{question.q}</p>
          <div className="space-y-2">
            {(question.options ?? []).map((opt, oi) => {
              const optLetter = opt.trim()[0]?.toUpperCase();
              const isCorrect = showAnswers && optLetter === question.answer?.toUpperCase();
              return (
                <div
                  key={oi}
                  className={cn(
                    'flex items-start gap-2.5 px-3 py-2.5 rounded-xl border text-sm transition-colors',
                    isCorrect
                      ? 'border-emerald-500/30 bg-emerald-500/5 text-emerald-300'
                      : 'border-white/5 text-white/60'
                  )}
                >
                  {isCorrect && <CheckCircle2 className="w-4 h-4 text-emerald-400 flex-shrink-0 mt-0.5" />}
                  <span>{opt}</span>
                </div>
              );
            })}
          </div>
          {showAnswers && question.explanation && (
            <div className="mt-3 p-3 bg-brand-500/5 border border-brand-500/15 rounded-xl">
              <span className="text-xs font-semibold text-brand-400">Explanation: </span>
              <span className="text-xs text-white/60">{question.explanation}</span>
            </div>
          )}
          {question.chapter && (
            <div className="mt-2 text-xs text-white/25">Chapter: {question.chapter}</div>
          )}
        </div>
      </div>
    </motion.div>
  );
}

// ─── Paper display ────────────────────────────────────────────────────────────

function PaperDisplay({ paper }: { paper: GeneratedPaper }) {
  const [showAnswers, setShowAnswers] = useState(false);

  function handleDownload() {
    const text = generateDownloadText(paper);
    const blob = new Blob([text], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${paper.subject}-${paper.difficulty}-${Date.now()}.txt`;
    a.click();
    URL.revokeObjectURL(url);
    toast.success('Question paper downloaded.');
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      className="space-y-4"
    >
      {/* Paper header */}
      <div className="card border border-brand-500/20">
        <div className="flex items-start justify-between gap-4 flex-wrap">
          <div>
            <h3 className="font-bold text-white text-base">{paper.subject} — {paper.targetExam}</h3>
            <div className="flex items-center gap-3 mt-1 flex-wrap">
              <span className={cn(
                'text-xs px-2 py-0.5 rounded-full font-medium',
                paper.difficulty === 'High' ? 'bg-red-500/15 text-red-400' :
                paper.difficulty === 'Medium' ? 'bg-amber-500/15 text-amber-400' :
                'bg-emerald-500/15 text-emerald-400'
              )}>
                {paper.difficulty} Difficulty
              </span>
              <span className="text-xs text-white/40">{paper.questions.length} Questions</span>
              <span className="text-xs text-white/30">{formatDate(paper.generatedAt)}</span>
            </div>
            <p className="text-xs text-white/30 mt-1">Chapters: {paper.chapters.join(', ')}</p>
          </div>
          <div className="flex items-center gap-2">
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
              onClick={handleDownload}
              className="flex items-center gap-2 px-3 py-2 rounded-xl text-sm border border-white/10 text-white/50 hover:text-white hover:border-white/20 transition-colors"
            >
              <Download className="w-4 h-4" />
              Download
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

// ─── Main Page ────────────────────────────────────────────────────────────────

export default function ParentQuestionBankPage() {
  const [subject, setSubject] = useState<string>('Physics');
  const [targetExam, setTargetExam] = useState<string>('JEE Main');
  const [selectedChapters, setSelectedChapters] = useState<string[]>([]);
  const [difficulty, setDifficulty] = useState<Difficulty>('Medium');
  const [numQuestions, setNumQuestions] = useState<QuestionCount>(10);
  const [generating, setGenerating] = useState(false);
  const [papers, setPapers] = useState<GeneratedPaper[]>([]);
  const [activePaperId, setActivePaperId] = useState<string | null>(null);
  const [showPrevious, setShowPrevious] = useState(false);

  const chapters = CHAPTERS_BY_SUBJECT[subject] ?? [];

  function toggleChapter(ch: string) {
    setSelectedChapters((prev) =>
      prev.includes(ch) ? prev.filter((c) => c !== ch) : [...prev, ch]
    );
  }

  function handleSubjectChange(newSubject: string) {
    setSubject(newSubject);
    setSelectedChapters([]);
  }

  async function handleGenerate() {
    const chaptersToUse = selectedChapters.length > 0 ? selectedChapters : chapters;

    setGenerating(true);
    try {
      const userMessage = `Generate ${numQuestions} ${difficulty} difficulty MCQ questions for ${subject} covering chapters: ${chaptersToUse.join(', ')}. Target exam: ${targetExam}. Format: [{"q":"question","options":["A)...","B)...","C)...","D)..."],"answer":"A","explanation":"...","chapter":"..."}]`;

      const response = await api.post('/api/v1/ai/completions', {
        requesterId: 'PARENT_PORTAL',
        systemPrompt: 'You are an expert exam paper generator for Indian competitive exams. Generate MCQ questions with options A, B, C, D and answers. Format as JSON array.',
        userMessage,
        maxTokens: 2000,
        temperature: 0.7,
      });

      const content: string = response.data?.content ?? response.data?.text ?? JSON.stringify(response.data);
      const questions = extractJsonArray(content);

      if (!questions || questions.length === 0) {
        toast.error('Could not parse questions from AI response. Please try again.');
        return;
      }

      const paper: GeneratedPaper = {
        id: `paper-${Date.now()}`,
        subject,
        targetExam,
        difficulty,
        chapters: chaptersToUse,
        questions,
        generatedAt: new Date().toISOString(),
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
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-white">Question Bank</h1>
        <p className="text-white/50 text-sm mt-0.5">
          Generate AI-powered practice papers for your child by subject and chapter.
        </p>
      </div>

      {/* Controls */}
      <div className="card border border-white/5 space-y-5">
        <div className="flex items-center gap-2 mb-1">
          <Sparkles className="w-4 h-4 text-brand-400" />
          <h3 className="font-semibold text-white text-sm">Configure Question Paper</h3>
        </div>

        {/* Subject + Exam row */}
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
            <label className="block text-xs font-medium text-white/60 mb-1.5">Target Exam</label>
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

        {/* Chapters multi-select */}
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
          <div className="flex gap-2">
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
          <div className="flex gap-2">
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
            ) : (
              <>
                <Sparkles className="w-4 h-4" />
                Generate Paper
              </>
            )}
          </button>
          {generating && (
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
                        <div className="text-xs text-white/30 mt-0.5">
                          {p.difficulty} · {p.questions.length}Q · {formatDate(p.generatedAt)}
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
        </div>
      )}
    </div>
  );
}
