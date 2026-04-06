// src/pages/admin/AdminLibraryPage.tsx
// CENTER_ADMIN / INSTITUTION_ADMIN document library management.
// Upload wizard: Step 1 file pick → POST multipart to backend /upload-direct (Fix #231 — no CORS)
//                Step 2 metadata form → Step 3 confirm POST to center-svc.
// Manage table: list all content items + archive action.
import { useState, useRef, useCallback, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Upload, Library, Plus, X, Loader2, FileText, Trash2,
  CheckCircle, AlertCircle, ChevronRight, ChevronLeft,
  Search, Filter, BookOpen, Building2, Link, Globe, RefreshCw,
} from 'lucide-react';
import { toast } from 'sonner';
import api from '../../lib/api';
import { cn } from '../../lib/utils';
import { useAuthStore } from '../../stores/authStore';

// ─── Types ────────────────────────────────────────────────────────────────────

interface ContentItemResponse {
  id: string;
  centerId: string;
  title: string;
  description?: string;
  type: string;
  status: string;
  subject?: string;
  board?: string;
  classGrade?: string;
  yearOfPaper?: number;
  examType?: string;
  difficulty?: string;
  language?: string;
  downloadCount: number;
  aiSummary?: string;
  fileSizeBytes?: number;
  pageCount?: number;
  createdAt: string;
}

interface CenterResponse { id: string; name: string; city?: string; centerType?: string; }

interface PresignedUploadResponse {
  uploadUrl: string;
  objectKey: string;
  expirySeconds: number;
}

// ─── Constants ────────────────────────────────────────────────────────────────

const CONTENT_TYPES = [
  { value: 'PREVIOUS_YEAR_PAPER', label: 'Previous Year Paper' },
  { value: 'NOTES',               label: 'Notes' },
  { value: 'FORMULA_SHEET',       label: 'Formula Sheet' },
  { value: 'MIND_MAP',            label: 'Mind Map' },
  { value: 'QUIZ_REF',            label: 'Quiz Reference' },
  { value: 'PDF',                 label: 'PDF' },
  { value: 'VIDEO',               label: 'Video' },
  { value: 'DOCUMENT',            label: 'Document' },
];

const BOARDS = ['CBSE', 'ICSE', 'STATE_BOARD', 'IB', 'CAMBRIDGE', 'OTHER'];

const EXAM_TYPES = [
  { value: 'BOARD_EXAM', label: 'Board Exam' },
  { value: 'JEE',        label: 'JEE' },
  { value: 'NEET',       label: 'NEET' },
  { value: 'UPSC',       label: 'UPSC' },
  { value: 'GATE',       label: 'GATE' },
  { value: 'UNIT_TEST',  label: 'Unit Test' },
  { value: 'MOCK',       label: 'Mock Test' },
  { value: 'PRACTICE',   label: 'Practice' },
];

const DIFFICULTIES = ['EASY', 'MEDIUM', 'HARD'];

const STATUS_COLOR: Record<string, string> = {
  AVAILABLE:  'bg-emerald-500/15 text-emerald-400',
  PROCESSING: 'bg-amber-500/15 text-amber-400',
  ARCHIVED:   'bg-black/[.08] dark:bg-white/10 text-white/30',
};

// ─── Add Link Wizard Modal ────────────────────────────────────────────────────

function AddLinkWizard({ centerId, onClose, onSuccess, isSuperAdmin }: {
  centerId: string; onClose: () => void; onSuccess: () => void; isSuperAdmin: boolean;
}) {
  const queryClient = useQueryClient();
  const [url, setUrl]                 = useState('');
  const [title, setTitle]             = useState('');
  const [description, setDescription] = useState('');
  const [contentType, setContentType] = useState('VIDEO');
  const [subject, setSubject]         = useState('');
  const [board, setBoard]             = useState('');
  const [classGrade, setClassGrade]   = useState('');
  const [stream, setStream]           = useState('');
  const [difficulty, setDifficulty]   = useState('');
  const [isPlatform, setIsPlatform]   = useState(false);
  const [tags, setTags]               = useState('');

  // Auto-detect YouTube
  useEffect(() => {
    if (!url) return;
    const ytMatch = url.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/)([A-Za-z0-9_-]{11})/);
    if (ytMatch) {
      setContentType('VIDEO');
      // Try to auto-fill title from URL if blank
      if (!title) setTitle('YouTube Video');
    } else if (url.match(/\.(pdf)$/i)) {
      setContentType('PDF');
    }
  }, [url]); // eslint-disable-line

  const ytId = url.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/)([A-Za-z0-9_-]{11})/)?.[1] ?? null;

  const submitMutation = useMutation({
    mutationFn: async () => {
      const tagArr = tags.split(',').map(t => t.trim()).filter(Boolean);
      return api.post(`/api/v1/centers/${centerId}/content/link`, {
        externalUrl:        url,
        title:              title.trim(),
        description:        description || undefined,
        type:               contentType,
        subject:            subject || undefined,
        board:              board || undefined,
        classGrade:         classGrade || undefined,
        stream:             stream || undefined,
        difficulty:         difficulty || undefined,
        tags:               tagArr.length > 0 ? tagArr : undefined,
        isPlatformResource: isPlatform,
      });
    },
    onSuccess: () => {
      toast.success('Link added to library!');
      queryClient.invalidateQueries({ queryKey: ['admin-library', centerId] });
      onSuccess();
      onClose();
    },
    onError: () => toast.error('Failed to register link.'),
  });

  const canSubmit = url.trim().length > 0 && title.trim().length > 0 && !submitMutation.isPending;

  const modal = (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
      <motion.div
        initial={{ opacity: 0, scale: 0.96 }}
        animate={{ opacity: 1, scale: 1 }}
        className="w-full max-w-lg bg-surface-50 border border-white/10 rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]"
      >
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-white/5 flex-shrink-0">
          <h2 className="text-sm font-semibold text-white flex items-center gap-2">
            <Globe className="w-4 h-4 text-blue-400" />
            Add External Link
          </h2>
          <button onClick={onClose} className="p-1 rounded-lg hover:bg-white/5 text-white/40 hover:text-white transition-colors">
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="overflow-y-auto flex-1 p-5 space-y-4">
          {/* URL */}
          <div>
            <label className="block text-xs font-medium text-white/50 mb-1.5">URL *</label>
            <input
              type="url"
              value={url}
              onChange={e => setUrl(e.target.value)}
              placeholder="https://youtube.com/watch?v=... or any URL"
              className="input w-full text-sm"
            />
          </div>

          {/* YouTube preview */}
          {ytId && (
            <div className="rounded-xl overflow-hidden border border-white/10">
              <img
                src={`https://img.youtube.com/vi/${ytId}/hqdefault.jpg`}
                alt="YouTube thumbnail"
                className="w-full h-32 object-cover"
              />
              <div className="px-3 py-1.5 bg-surface-100/60 text-[10px] text-white/40 flex items-center gap-1">
                <Globe className="w-3 h-3" /> YouTube video detected
              </div>
            </div>
          )}

          {/* Title */}
          <div>
            <label className="block text-xs font-medium text-white/50 mb-1.5">Title *</label>
            <input
              type="text"
              value={title}
              onChange={e => setTitle(e.target.value)}
              placeholder="Descriptive title"
              className="input w-full text-sm"
            />
          </div>

          {/* Description */}
          <div>
            <label className="block text-xs font-medium text-white/50 mb-1.5">Description</label>
            <textarea
              value={description}
              onChange={e => setDescription(e.target.value)}
              rows={2}
              className="input w-full text-sm resize-none"
            />
          </div>

          {/* Content Type + Difficulty */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-white/50 mb-1.5">Type</label>
              <select value={contentType} onChange={e => setContentType(e.target.value)} className="input w-full text-sm">
                {CONTENT_TYPES.map(ct => <option key={ct.value} value={ct.value}>{ct.label}</option>)}
                <option value="LINK">Link</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-white/50 mb-1.5">Difficulty</label>
              <select value={difficulty} onChange={e => setDifficulty(e.target.value)} className="input w-full text-sm">
                <option value="">Any</option>
                {DIFFICULTIES.map(d => <option key={d} value={d}>{d}</option>)}
              </select>
            </div>
          </div>

          {/* Subject + Board */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-white/50 mb-1.5">Subject</label>
              <input type="text" value={subject} onChange={e => setSubject(e.target.value)} placeholder="e.g. Physics" className="input w-full text-sm" />
            </div>
            <div>
              <label className="block text-xs font-medium text-white/50 mb-1.5">Board</label>
              <select value={board} onChange={e => setBoard(e.target.value)} className="input w-full text-sm">
                <option value="">Any</option>
                {BOARDS.map(b => <option key={b} value={b}>{b}</option>)}
              </select>
            </div>
          </div>

          {/* Class + Stream */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-white/50 mb-1.5">Class Grade</label>
              <input type="text" value={classGrade} onChange={e => setClassGrade(e.target.value)} placeholder="e.g. 12" className="input w-full text-sm" />
            </div>
            <div>
              <label className="block text-xs font-medium text-white/50 mb-1.5">Stream</label>
              <input type="text" value={stream} onChange={e => setStream(e.target.value)} placeholder="e.g. SCIENCE" className="input w-full text-sm" />
            </div>
          </div>

          {/* Tags */}
          <div>
            <label className="block text-xs font-medium text-white/50 mb-1.5">Tags (comma-separated)</label>
            <input type="text" value={tags} onChange={e => setTags(e.target.value)} placeholder="physics, motion, newton" className="input w-full text-sm" />
          </div>

          {/* Platform resource toggle — only for SUPER_ADMIN / INSTITUTION_ADMIN */}
          {isSuperAdmin && (
            <button
              type="button"
              onClick={() => setIsPlatform(v => !v)}
              className={cn(
                'w-full flex items-center justify-between px-4 py-3 rounded-xl border transition-colors',
                isPlatform
                  ? 'bg-brand-600/15 border-brand-500/30 text-brand-300'
                  : 'bg-white/3 border-white/8 text-white/50 hover:border-white/15',
              )}
            >
              <span className="text-xs font-medium">Platform-wide resource</span>
              <div className={cn(
                'w-8 h-4 rounded-full transition-colors relative',
                isPlatform ? 'bg-brand-500' : 'bg-white/15',
              )}>
                <div className={cn(
                  'absolute top-0.5 w-3 h-3 rounded-full bg-white transition-all',
                  isPlatform ? 'left-4.5' : 'left-0.5',
                )} />
              </div>
            </button>
          )}
        </div>

        {/* Footer */}
        <div className="px-5 py-4 border-t border-white/5 flex gap-3 flex-shrink-0">
          <button onClick={onClose} className="btn-ghost flex-1 py-2.5 text-sm">Cancel</button>
          <button
            onClick={() => submitMutation.mutate()}
            disabled={!canSubmit}
            className="btn-primary flex-1 py-2.5 text-sm flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {submitMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Link className="w-4 h-4" />}
            Add Link
          </button>
        </div>
      </motion.div>
    </div>
  );
  return createPortal(modal, document.body);
}

// ─── Upload Wizard Modal ──────────────────────────────────────────────────────

interface UploadWizardProps {
  centerId: string;
  onClose: () => void;
  onSuccess: () => void;
}

function UploadWizard({ centerId, onClose, onSuccess }: UploadWizardProps) {
  const [step,       setStep]       = useState<1 | 2 | 3>(1);
  const [file,       setFile]       = useState<File | null>(null);
  const [progress,   setProgress]   = useState(0);
  const [uploading,  setUploading]  = useState(false);
  const [uploadDone, setUploadDone] = useState(false);
  const [objectKey,  setObjectKey]  = useState('');
  const [dragOver,   setDragOver]   = useState(false);

  // Metadata form
  const [meta, setMeta] = useState({
    title:       '',
    description: '',
    contentType: 'PREVIOUS_YEAR_PAPER',
    subject:     '',
    board:       '',
    classGrade:  '',
    yearOfPaper: '',
    examType:    '',
    difficulty:  '',
    language:    'ENGLISH',
    pageCount:   '',
    tags:        '',
  });

  const inputRef = useRef<HTMLInputElement>(null);
  const queryClient = useQueryClient();

  function setM(k: string, v: string) { setMeta(m => ({ ...m, [k]: v })); }

  // ── Step 1: pick file → POST multipart to backend → backend uploads to MinIO/S3 ──
  // Fix #231: replaced direct XHR PUT to S3/MinIO (CORS blocked) with server-side upload.
  // Backend endpoint: POST /api/v1/centers/{centerId}/content/upload-direct (MultipartFile)
  // Returns { objectKey } used in /confirm step — same confirm flow as before.

  async function startUpload(f: File) {
    setFile(f);
    setUploading(true);
    setProgress(0);
    try {
      const formData = new FormData();
      formData.append('file', f);

      const res = await api.post<PresignedUploadResponse>(
        `/api/v1/centers/${centerId}/content/upload-direct`,
        formData,
        {
          headers: { 'Content-Type': 'multipart/form-data' },
          onUploadProgress: (e) => {
            if (e.total) setProgress(Math.round((e.loaded / e.total) * 100));
          },
        },
      );

      setObjectKey(res.data.objectKey);
      setProgress(100);
      setUploadDone(true);
      // Pre-fill title from filename
      const nameWithoutExt = f.name.replace(/\.[^.]+$/, '').replace(/[-_]/g, ' ');
      if (!meta.title) setM('title', nameWithoutExt);
      setStep(2);
    } catch (err: unknown) {
      toast.error((err as Error).message ?? 'Upload failed');
    } finally {
      setUploading(false);
    }
  }

  function onFileSelected(f: File) { startUpload(f); }

  function handleDrop(e: React.DragEvent) {
    e.preventDefault();
    setDragOver(false);
    const f = e.dataTransfer.files[0];
    if (f) onFileSelected(f);
  }

  // ── Step 3: confirm POST ──

  const confirmMutation = useMutation({
    mutationFn: async () => {
      const tags = meta.tags.split(',').map(t => t.trim()).filter(Boolean);
      return api.post(`/api/v1/centers/${centerId}/content/confirm`, {
        objectKey,
        title:       meta.title,
        description: meta.description || undefined,
        type:        meta.contentType,
        fileSizeBytes: file?.size,
        mimeType:    file?.type || 'application/octet-stream',
        pageCount:   meta.pageCount ? parseInt(meta.pageCount) : undefined,
        subject:     meta.subject   || undefined,
        board:       meta.board     || undefined,
        classGrade:  meta.classGrade || undefined,
        yearOfPaper: meta.yearOfPaper ? parseInt(meta.yearOfPaper) : undefined,
        examType:    meta.examType   || undefined,
        difficulty:  meta.difficulty || undefined,
        language:    meta.language,
        tags:        tags.length > 0 ? tags : undefined,
      });
    },
    onSuccess: () => {
      toast.success('Document uploaded! AI tagging will complete shortly.');
      queryClient.invalidateQueries({ queryKey: ['admin-library', centerId] });
      onSuccess();
      onClose();
    },
    onError: () => toast.error('Failed to save document metadata.'),
  });

  const canConfirm = !!meta.title.trim() && uploadDone && !confirmMutation.isPending;

  // ── Render ──

  const modal = (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
      <motion.div
        initial={{ opacity: 0, scale: 0.96 }}
        animate={{ opacity: 1, scale: 1 }}
        className="w-full max-w-lg bg-surface-50 border border-white/10 rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]"
      >
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-white/5 flex-shrink-0">
          <h2 className="text-sm font-semibold text-white flex items-center gap-2">
            <Upload className="w-4 h-4 text-brand-400" />
            Upload Document
            <span className="text-white/30 text-xs font-normal">— Step {step} of 3</span>
          </h2>
          <button
            onClick={onClose}
            className="p-1 rounded-lg hover:bg-white/5 text-white/40 hover:text-white transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Step indicator */}
        <div className="flex border-b border-white/5 flex-shrink-0">
          {(['Upload', 'Metadata', 'Confirm'] as const).map((label, i) => (
            <div
              key={label}
              className={cn(
                'flex-1 py-2.5 text-center text-xs font-medium transition-colors',
                step === i + 1
                  ? 'text-brand-400 border-b-2 border-brand-500'
                  : step > i + 1
                  ? 'text-white/40'
                  : 'text-white/20',
              )}
            >
              {step > i + 1 ? <CheckCircle className="w-3.5 h-3.5 inline mr-1 text-emerald-400" /> : null}
              {label}
            </div>
          ))}
        </div>

        <div className="overflow-y-auto flex-1">

          {/* Step 1 — File drop zone */}
          {step === 1 && (
            <div className="p-5">
              {uploading ? (
                <div className="space-y-4 py-8">
                  <div className="flex items-center gap-3">
                    <Loader2 className="w-5 h-5 animate-spin text-brand-400 flex-shrink-0" />
                    <div className="flex-1">
                      <p className="text-sm text-white font-medium">Uploading {file?.name}</p>
                      <p className="text-xs text-white/40 mt-0.5">Sending directly to secure storage…</p>
                    </div>
                    <span className="text-sm font-bold text-brand-400">{progress}%</span>
                  </div>
                  <div className="w-full bg-white/5 rounded-full h-2">
                    <div
                      className="bg-brand-500 h-2 rounded-full transition-all duration-300"
                      style={{ width: `${progress}%` }}
                    />
                  </div>
                </div>
              ) : (
                <>
                  <label
                    onDragOver={e => { e.preventDefault(); setDragOver(true); }}
                    onDragLeave={() => setDragOver(false)}
                    onDrop={handleDrop}
                    className={cn(
                      'flex flex-col items-center justify-center gap-3 py-14 border-2 border-dashed rounded-xl cursor-pointer transition-colors',
                      dragOver
                        ? 'border-brand-500 bg-brand-500/10'
                        : 'border-white/10 bg-white/2 hover:border-white/20 hover:bg-white/5',
                    )}
                  >
                    <Upload className="w-10 h-10 text-white/20" />
                    <div className="text-center">
                      <p className="text-sm font-medium text-white/60">
                        Drop a file here or <span className="text-brand-400">browse</span>
                      </p>
                      <p className="text-xs text-white/30 mt-1">
                        PDF, DOCX, MP4, images — any file up to 500 MB
                      </p>
                    </div>
                    <input
                      ref={inputRef}
                      type="file"
                      className="hidden"
                      onChange={e => { const f = e.target.files?.[0]; if (f) onFileSelected(f); }}
                    />
                  </label>
                  <p className="text-xs text-white/25 text-center mt-3">
                    Files upload directly to encrypted MinIO storage. No data passes through the app server.
                  </p>
                </>
              )}
            </div>
          )}

          {/* Step 2 — Metadata */}
          {step === 2 && (
            <div className="p-5 space-y-4">
              <div className="flex items-center gap-2 px-3 py-2 bg-emerald-500/10 border border-emerald-500/20 rounded-lg text-xs text-emerald-400">
                <CheckCircle className="w-4 h-4 flex-shrink-0" />
                File uploaded successfully — now add metadata.
              </div>

              {/* Title */}
              <div>
                <label className="block text-xs text-white/50 mb-1.5 font-medium">Title *</label>
                <input
                  value={meta.title}
                  onChange={e => setM('title', e.target.value)}
                  placeholder="e.g. JEE 2023 Physics Previous Year Paper"
                  className="w-full bg-surface-100/50 border border-white/10 rounded-xl px-3 py-2.5 text-sm text-white placeholder-white/20 focus:outline-none focus:border-brand-500/50 transition-colors"
                />
              </div>

              {/* Description */}
              <div>
                <label className="block text-xs text-white/50 mb-1.5 font-medium">Description</label>
                <textarea
                  value={meta.description}
                  onChange={e => setM('description', e.target.value)}
                  rows={2}
                  placeholder="Brief description…"
                  className="w-full bg-surface-100/50 border border-white/10 rounded-xl px-3 py-2.5 text-sm text-white placeholder-white/20 focus:outline-none focus:border-brand-500/50 transition-colors resize-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                {/* Content Type */}
                <div>
                  <label className="block text-xs text-white/50 mb-1.5 font-medium">Type *</label>
                  <select
                    value={meta.contentType}
                    onChange={e => setM('contentType', e.target.value)}
                    className="w-full bg-surface-100/50 border border-white/10 rounded-xl px-3 py-2.5 text-sm text-white focus:outline-none focus:border-brand-500/50 transition-colors"
                  >
                    {CONTENT_TYPES.map(t => (
                      <option key={t.value} value={t.value} className="bg-surface-100">{t.label}</option>
                    ))}
                  </select>
                </div>

                {/* Language */}
                <div>
                  <label className="block text-xs text-white/50 mb-1.5 font-medium">Language</label>
                  <input
                    value={meta.language}
                    onChange={e => setM('language', e.target.value)}
                    placeholder="ENGLISH"
                    className="w-full bg-surface-100/50 border border-white/10 rounded-xl px-3 py-2.5 text-sm text-white placeholder-white/20 focus:outline-none focus:border-brand-500/50 transition-colors"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                {/* Subject */}
                <div>
                  <label className="block text-xs text-white/50 mb-1.5 font-medium">Subject</label>
                  <input
                    value={meta.subject}
                    onChange={e => setM('subject', e.target.value)}
                    placeholder="e.g. Physics"
                    className="w-full bg-surface-100/50 border border-white/10 rounded-xl px-3 py-2.5 text-sm text-white placeholder-white/20 focus:outline-none focus:border-brand-500/50 transition-colors"
                  />
                </div>

                {/* Board */}
                <div>
                  <label className="block text-xs text-white/50 mb-1.5 font-medium">Board</label>
                  <select
                    value={meta.board}
                    onChange={e => setM('board', e.target.value)}
                    className="w-full bg-surface-100/50 border border-white/10 rounded-xl px-3 py-2.5 text-sm text-white focus:outline-none focus:border-brand-500/50 transition-colors"
                  >
                    <option value="">Select board</option>
                    {BOARDS.map(b => <option key={b} value={b} className="bg-surface-100">{b}</option>)}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-3 gap-3">
                {/* Exam Type */}
                <div>
                  <label className="block text-xs text-white/50 mb-1.5 font-medium">Exam Type</label>
                  <select
                    value={meta.examType}
                    onChange={e => setM('examType', e.target.value)}
                    className="w-full bg-surface-100/50 border border-white/10 rounded-xl px-3 py-2.5 text-sm text-white focus:outline-none focus:border-brand-500/50 transition-colors"
                  >
                    <option value="">None</option>
                    {EXAM_TYPES.map(e => <option key={e.value} value={e.value} className="bg-surface-100">{e.label}</option>)}
                  </select>
                </div>

                {/* Difficulty */}
                <div>
                  <label className="block text-xs text-white/50 mb-1.5 font-medium">Difficulty</label>
                  <select
                    value={meta.difficulty}
                    onChange={e => setM('difficulty', e.target.value)}
                    className="w-full bg-surface-100/50 border border-white/10 rounded-xl px-3 py-2.5 text-sm text-white focus:outline-none focus:border-brand-500/50 transition-colors"
                  >
                    <option value="">None</option>
                    {DIFFICULTIES.map(d => <option key={d} value={d} className="bg-surface-100">{d}</option>)}
                  </select>
                </div>

                {/* Year */}
                <div>
                  <label className="block text-xs text-white/50 mb-1.5 font-medium">Year</label>
                  <input
                    type="number"
                    value={meta.yearOfPaper}
                    onChange={e => setM('yearOfPaper', e.target.value)}
                    placeholder="2023"
                    min="1990"
                    max={new Date().getFullYear()}
                    className="w-full bg-surface-100/50 border border-white/10 rounded-xl px-3 py-2.5 text-sm text-white placeholder-white/20 focus:outline-none focus:border-brand-500/50 transition-colors"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                {/* Grade */}
                <div>
                  <label className="block text-xs text-white/50 mb-1.5 font-medium">Class / Grade</label>
                  <input
                    value={meta.classGrade}
                    onChange={e => setM('classGrade', e.target.value)}
                    placeholder="e.g. 12"
                    className="w-full bg-surface-100/50 border border-white/10 rounded-xl px-3 py-2.5 text-sm text-white placeholder-white/20 focus:outline-none focus:border-brand-500/50 transition-colors"
                  />
                </div>

                {/* Page count */}
                <div>
                  <label className="block text-xs text-white/50 mb-1.5 font-medium">Pages</label>
                  <input
                    type="number"
                    value={meta.pageCount}
                    onChange={e => setM('pageCount', e.target.value)}
                    placeholder="e.g. 20"
                    min="1"
                    className="w-full bg-surface-100/50 border border-white/10 rounded-xl px-3 py-2.5 text-sm text-white placeholder-white/20 focus:outline-none focus:border-brand-500/50 transition-colors"
                  />
                </div>
              </div>

              {/* Tags */}
              <div>
                <label className="block text-xs text-white/50 mb-1.5 font-medium">Tags (comma-separated)</label>
                <input
                  value={meta.tags}
                  onChange={e => setM('tags', e.target.value)}
                  placeholder="e.g. electrostatics, optics, jee"
                  className="w-full bg-surface-100/50 border border-white/10 rounded-xl px-3 py-2.5 text-sm text-white placeholder-white/20 focus:outline-none focus:border-brand-500/50 transition-colors"
                />
                <p className="text-[10px] text-white/25 mt-1">
                  AI will also auto-suggest tags after upload.
                </p>
              </div>
            </div>
          )}

          {/* Step 3 — Confirm */}
          {step === 3 && (
            <div className="p-5 space-y-4">
              <div className="bg-white/3 border border-white/8 rounded-xl divide-y divide-white/5">
                {[
                  { label: 'Title',    value: meta.title },
                  { label: 'Type',     value: CONTENT_TYPES.find(t => t.value === meta.contentType)?.label },
                  { label: 'Subject',  value: meta.subject  || '—' },
                  { label: 'Board',    value: meta.board    || '—' },
                  { label: 'Exam',     value: meta.examType || '—' },
                  { label: 'Year',     value: meta.yearOfPaper || '—' },
                  { label: 'File',     value: file ? `${file.name} (${(file.size / 1024 / 1024).toFixed(1)} MB)` : '' },
                ].map(row => (
                  <div key={row.label} className="flex px-4 py-2.5 gap-3">
                    <span className="text-xs text-white/40 w-20 flex-shrink-0">{row.label}</span>
                    <span className="text-xs text-white font-medium truncate">{row.value}</span>
                  </div>
                ))}
              </div>
              <div className="flex items-start gap-2 px-3 py-2.5 bg-brand-500/10 border border-brand-500/15 rounded-xl text-xs text-brand-300">
                <BookOpen className="w-4 h-4 flex-shrink-0 mt-0.5" />
                <span>AI will auto-analyze this document and add suggested tags + summary. The document becomes available once AI tagging completes (usually under 30 seconds).</span>
              </div>
              {confirmMutation.isError && (
                <div className="flex items-center gap-2 text-xs text-red-400 bg-red-500/10 border border-red-500/15 px-3 py-2 rounded-lg">
                  <AlertCircle className="w-4 h-4 flex-shrink-0" /> Failed to save metadata. Please retry.
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer buttons */}
        <div className="px-5 py-4 border-t border-white/5 flex justify-between flex-shrink-0">
          <button
            onClick={() => step > 1 ? setStep(s => (s - 1) as 1 | 2 | 3) : onClose()}
            className="flex items-center gap-1.5 px-4 py-2 text-sm text-white/50 hover:text-white transition-colors"
          >
            <ChevronLeft className="w-4 h-4" />
            {step === 1 ? 'Cancel' : 'Back'}
          </button>

          {step < 3 ? (
            <button
              onClick={() => setStep(s => (s + 1) as 1 | 2 | 3)}
              disabled={(step === 1 && !uploadDone) || (step === 2 && !meta.title.trim())}
              className="flex items-center gap-1.5 px-5 py-2 bg-brand-600 hover:bg-brand-500 disabled:opacity-40 text-white text-sm font-medium rounded-xl transition-colors"
            >
              Continue <ChevronRight className="w-4 h-4" />
            </button>
          ) : (
            <button
              onClick={() => confirmMutation.mutate()}
              disabled={!canConfirm}
              className="flex items-center gap-2 px-5 py-2 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-40 text-white text-sm font-medium rounded-xl transition-colors"
            >
              {confirmMutation.isPending
                ? <Loader2 className="w-4 h-4 animate-spin" />
                : <CheckCircle className="w-4 h-4" />}
              Publish Document
            </button>
          )}
        </div>
      </motion.div>
    </div>
  );

  return createPortal(modal, document.body);
}

// ─── Manage Table Row ─────────────────────────────────────────────────────────

function ContentRow({ item, centerId, onArchived, canModify }: {
  item: ContentItemResponse;
  centerId: string;
  onArchived: () => void;
  canModify: boolean;
}) {
  const [confirming, setConfirming] = useState(false);

  const archiveMutation = useMutation({
    mutationFn: () => api.delete(`/api/v1/centers/${centerId}/content/${item.id}`),
    onSuccess: () => { toast.success('Document archived.'); onArchived(); },
    onError:   () => toast.error('Failed to archive document.'),
  });

  const retagMutation = useMutation({
    mutationFn: () => api.post(`/api/v1/centers/${centerId}/content/${item.id}/ai-tag`),
    onSuccess: () => { toast.success('Document processed — now Available.'); onArchived(); /* refresh list */ },
    onError:   () => toast.error('Re-processing failed. Please try again.'),
  });

  return (
    <div className="flex items-center gap-3 px-4 py-3 hover:bg-white/2 transition-colors border-b border-white/5 last:border-0">
      <FileText className="w-4 h-4 text-white/25 flex-shrink-0" />

      <div className="flex-1 min-w-0">
        <p className="text-sm text-white font-medium truncate">{item.title}</p>
        <div className="flex items-center gap-2 mt-0.5 flex-wrap">
          {item.subject && <span className="text-[10px] text-white/35">{item.subject}</span>}
          {item.board   && <span className="text-[10px] text-white/25">· {item.board}</span>}
          {item.yearOfPaper && <span className="text-[10px] text-white/25">· {item.yearOfPaper}</span>}
        </div>
      </div>

      <span className="text-[10px] font-medium px-2 py-0.5 rounded-full flex-shrink-0 bg-black/5 dark:bg-white/5 text-white/40">
        {item.type}
      </span>

      <span className={cn(
        'text-[10px] font-medium px-2 py-0.5 rounded-full flex-shrink-0',
        STATUS_COLOR[item.status] ?? 'bg-black/5 dark:bg-white/5 text-white/30',
      )}>
        {item.status}
      </span>

      <span className="text-xs text-white/25 flex-shrink-0 hidden sm:block">
        {item.downloadCount} dl
      </span>

      {canModify && item.status === 'PROCESSING' && (
        <button
          onClick={() => retagMutation.mutate()}
          disabled={retagMutation.isPending}
          className="flex items-center gap-1 px-2 py-1 rounded-lg text-xs text-amber-400 hover:text-white hover:bg-amber-500/15 border border-amber-500/20 transition-colors flex-shrink-0 disabled:opacity-40"
          title="Manually re-process to make Available"
        >
          {retagMutation.isPending
            ? <Loader2 className="w-3 h-3 animate-spin" />
            : <RefreshCw className="w-3 h-3" />}
          Re-process
        </button>
      )}

      {canModify && (
        !confirming ? (
          <button
            onClick={() => setConfirming(true)}
            className="p-1.5 rounded-lg text-white/25 hover:text-red-400 hover:bg-red-500/10 transition-colors flex-shrink-0"
            title="Archive document"
          >
            <Trash2 className="w-3.5 h-3.5" />
          </button>
        ) : (
          <div className="flex items-center gap-1 flex-shrink-0">
            <button
              onClick={() => archiveMutation.mutate()}
              disabled={archiveMutation.isPending}
              className="text-xs px-2 py-1 bg-red-600 hover:bg-red-500 text-white rounded-lg transition-colors disabled:opacity-50"
            >
              {archiveMutation.isPending ? '…' : 'Archive'}
            </button>
            <button
              onClick={() => setConfirming(false)}
              className="text-xs px-2 py-1 text-white/40 hover:text-white transition-colors"
            >
              Cancel
            </button>
          </div>
        )
      )}
    </div>
  );
}

// ─── Center Picker ────────────────────────────────────────────────────────────

function CenterPicker({ centers, onSelect }: {
  centers: CenterResponse[];
  onSelect: (id: string) => void;
}) {
  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2 text-sm text-white/50">
        <Building2 className="w-4 h-4" />
        <span>Select a center to manage its library:</span>
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        {centers.map(c => (
          <button
            key={c.id}
            onClick={() => onSelect(c.id)}
            className="flex items-center gap-3 p-4 bg-white/3 border border-white/8 rounded-xl hover:border-white/15 hover:bg-white/5 transition-colors text-left"
          >
            <div className="w-8 h-8 rounded-lg bg-brand-500/15 border border-brand-500/20 flex items-center justify-center flex-shrink-0">
              <Library className="w-4 h-4 text-brand-400" />
            </div>
            <div className="min-w-0">
              <p className="text-sm font-semibold text-white truncate">{c.name}</p>
              {c.city && <p className="text-xs text-white/35">{c.city}</p>}
            </div>
          </button>
        ))}
      </div>
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────

export default function AdminLibraryPage() {
  const { user } = useAuthStore();
  const centerId  = user?.centerId ?? '';
  const isInst    = user?.role === 'INSTITUTION_ADMIN';

  const [selectedCenterId, setSelectedCenterId] = useState('');
  const [showUpload, setShowUpload] = useState(false);
  const [showAddLink, setShowAddLink] = useState(false);
  const [search,    setSearch]    = useState('');
  const [filterStatus, setFilterStatus] = useState('');

  const effectiveCenterId = centerId || selectedCenterId;
  const isSuperAdmin = user?.role === 'SUPER_ADMIN' || user?.role === 'INSTITUTION_ADMIN';
  // Only SUPER_ADMIN, INSTITUTION_ADMIN, and CENTER_ADMIN may modify existing content (archive, re-tag).
  // Teachers have upload-only access — they cannot delete or alter existing library items.
  const canModify = ['SUPER_ADMIN', 'INSTITUTION_ADMIN', 'CENTER_ADMIN'].includes(user?.role ?? '');

  // Fetch center info for center-type-aware auto-open
  const { data: centerInfo } = useQuery({
    queryKey: ['center-info', effectiveCenterId],
    queryFn: () => api.get(`/api/v1/centers/${effectiveCenterId}`).then(r => r.data),
    enabled: !!effectiveCenterId,
    staleTime: 5 * 60 * 1000,
    retry: false,
  });
  const centerType: string = centerInfo?.centerType ?? 'COACHING_CENTER';
  const isSchoolOrCollege = centerType === 'SCHOOL' || centerType === 'COLLEGE';

  const queryClient = useQueryClient();

  // Centers for INSTITUTION_ADMIN picker
  const { data: centers = [] } = useQuery<CenterResponse[]>({
    queryKey: ['admin-centers-picker'],
    queryFn:  async () => {
      const res = await api.get('/api/v1/centers');
      const d = res.data;
      return Array.isArray(d) ? d : (d.content ?? []);
    },
    enabled: isInst && !centerId,
  });

  // Auto-select for SCHOOL/COLLEGE, or when there is only one center of any type
  useEffect(() => {
    if (!centerId && centers.length > 0 && (
      centers.every(c => c.centerType !== 'COACHING_CENTER') || centers.length === 1
    )) {
      setSelectedCenterId(centers[0].id);
    }
  }, [centers, centerId]); // eslint-disable-line

  // Library content list
  const { data: pageData, isLoading } = useQuery({
    queryKey: ['admin-library', effectiveCenterId],
    queryFn: async () => {
      const res = await api.get(`/api/v1/centers/${effectiveCenterId}/content`, {
        params: { page: 0, size: 100 },
      });
      const d = res.data;
      return Array.isArray(d) ? d : (d.content ?? []);
    },
    enabled: !!effectiveCenterId,
  });

  const allItems = (pageData ?? []) as ContentItemResponse[];

  const filtered = allItems.filter(item => {
    const matchSearch = !search
      || item.title.toLowerCase().includes(search.toLowerCase())
      || (item.subject ?? '').toLowerCase().includes(search.toLowerCase());
    const matchStatus = !filterStatus || item.status === filterStatus;
    return matchSearch && matchStatus;
  });

  const onArchived = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['admin-library', effectiveCenterId] });
  }, [queryClient, effectiveCenterId]);

  // ── No center (INSTITUTION_ADMIN without JWT centerId) ──
  if (isInst && !centerId) {
    if (centers.length === 0 && !isLoading) {
      return (
        <div className="p-6 max-w-4xl mx-auto">
          <h1 className="text-xl font-bold text-white flex items-center gap-2 mb-4">
            <Library className="w-5 h-5 text-brand-400" /> Document Library
          </h1>
          <div className="text-center py-20 text-white/30">
            <Library className="w-12 h-12 mx-auto mb-3 opacity-30" />
            <p>No centers found. Create a center first.</p>
          </div>
        </div>
      );
    }
    if (!selectedCenterId) {
      return (
        <div className="p-6 max-w-4xl mx-auto space-y-6">
          <h1 className="text-xl font-bold text-white flex items-center gap-2">
            <Library className="w-5 h-5 text-brand-400" /> Document Library
          </h1>
          <CenterPicker centers={centers} onSelect={setSelectedCenterId} />
        </div>
      );
    }
  }

  return (
    <div className="p-6 max-w-5xl mx-auto space-y-6">

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-white flex items-center gap-2">
            <Library className="w-5 h-5 text-brand-400" /> Document Library
          </h1>
          <p className="text-sm text-white/40 mt-0.5">
            Upload and manage previous year papers, notes, formula sheets and more.
          </p>
        </div>
        <div className="flex items-center gap-2 flex-shrink-0">
          <button
            onClick={() => setShowAddLink(true)}
            disabled={!effectiveCenterId}
            className="flex items-center gap-2 px-3 py-2 bg-blue-600/15 hover:bg-blue-600/20 border border-blue-500/25 text-blue-300 rounded-xl text-sm font-medium transition-colors disabled:opacity-40"
          >
            <Globe className="w-4 h-4" />
            Add Link
          </button>
          <button
            onClick={() => setShowUpload(true)}
            disabled={!effectiveCenterId}
            className="flex items-center gap-2 px-4 py-2.5 bg-brand-600 hover:bg-brand-500 disabled:opacity-40 text-white text-sm font-medium rounded-xl transition-colors"
          >
            <Plus className="w-4 h-4" /> Upload Document
          </button>
        </div>
      </div>

      {/* Stats strip */}
      {allItems.length > 0 && (
        <div className="grid grid-cols-3 gap-3">
          {[
            { label: 'Total',      val: allItems.length },
            { label: 'Available',  val: allItems.filter(i => i.status === 'AVAILABLE').length },
            { label: 'Processing', val: allItems.filter(i => i.status === 'PROCESSING').length },
          ].map(s => (
            <div key={s.label} className="bg-white/3 border border-white/8 rounded-xl px-4 py-3 text-center">
              <p className="text-2xl font-bold text-white">{s.val}</p>
              <p className="text-xs text-white/35 mt-0.5">{s.label}</p>
            </div>
          ))}
        </div>
      )}

      {/* Search + filter */}
      <div className="flex gap-3">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
          <input
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Search title or subject…"
            className="w-full bg-surface-100/50 border border-white/10 rounded-xl pl-9 pr-3 py-2 text-sm text-white placeholder-white/20 focus:outline-none focus:border-brand-500/50 transition-colors"
          />
        </div>
        <div className="flex items-center gap-2">
          <Filter className="w-3.5 h-3.5 text-white/30" />
          <select
            value={filterStatus}
            onChange={e => setFilterStatus(e.target.value)}
            className="bg-surface-100/50 border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500/50 transition-colors"
          >
            <option value="">All statuses</option>
            <option value="AVAILABLE"  className="bg-surface-100">Available</option>
            <option value="PROCESSING" className="bg-surface-100">Processing</option>
            <option value="ARCHIVED"   className="bg-surface-100">Archived</option>
          </select>
        </div>
      </div>

      {/* Table */}
      {isLoading ? (
        <div className="flex items-center gap-3 justify-center py-20 text-white/40">
          <Loader2 className="w-5 h-5 animate-spin" />
          <span className="text-sm">Loading library…</span>
        </div>
      ) : filtered.length === 0 ? (
        <div className="text-center py-20 border border-dashed border-white/8 rounded-xl">
          <Library className="w-12 h-12 text-white/10 mx-auto mb-3" />
          <p className="text-white/40 text-sm font-medium">
            {allItems.length === 0 ? 'No documents uploaded yet' : 'No documents match your search'}
          </p>
          <p className="text-white/25 text-xs mt-1">
            {allItems.length === 0
              ? 'Click "Upload Document" to get started.'
              : 'Try adjusting the search or filter.'}
          </p>
        </div>
      ) : (
        <div className="bg-white/2 border border-white/8 rounded-xl overflow-hidden">
          <div className="px-4 py-2 border-b border-white/5 flex items-center gap-2 text-[10px] text-white/30 font-medium uppercase tracking-wider">
            <span className="flex-1">Document</span>
            <span className="w-24 text-center hidden sm:block">Type</span>
            <span className="w-24 text-center">Status</span>
            <span className="w-16 text-center hidden sm:block">Downloads</span>
            <span className="w-16"></span>
          </div>
          <AnimatePresence>
            {filtered.map(item => (
              <motion.div
                key={item.id}
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
              >
                <ContentRow
                  item={item}
                  centerId={effectiveCenterId}
                  onArchived={onArchived}
                  canModify={canModify}
                />
              </motion.div>
            ))}
          </AnimatePresence>
        </div>
      )}

      {/* Add Link wizard portal */}
      {showAddLink && effectiveCenterId && (
        <AddLinkWizard
          centerId={effectiveCenterId}
          onClose={() => setShowAddLink(false)}
          onSuccess={() => queryClient.invalidateQueries({ queryKey: ['admin-library', effectiveCenterId] })}
          isSuperAdmin={isSuperAdmin}
        />
      )}

      {/* Upload wizard portal */}
      <AnimatePresence>
        {showUpload && effectiveCenterId && (
          <UploadWizard
            centerId={effectiveCenterId}
            onClose={() => setShowUpload(false)}
            onSuccess={() => queryClient.invalidateQueries({ queryKey: ['admin-library', effectiveCenterId] })}
          />
        )}
      </AnimatePresence>
    </div>
  );
}
