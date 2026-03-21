// src/pages/admin/AdminBulkImportTeachersPage.tsx
import { useState, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Upload, Download, CheckCircle2, XCircle, AlertCircle,
  FileText, Users, ArrowRight, RotateCcw, Loader2,
} from 'lucide-react';
import { toast } from 'sonner';
import { useAuthStore } from '../../stores/authStore';
import api from '../../lib/api';
import { cn } from '../../lib/utils';

// ─── Types ────────────────────────────────────────────────────────────────────

interface BulkRowError {
  row: number;
  email: string;
  field: string;
  message: string;
  suggestion?: string;
}

interface BulkImportPreviewResponse {
  totalRows: number;
  validRows: number;
  errorRows: number;
  errors: BulkRowError[];
}

interface BulkImportConfirmResponse {
  imported: number;
  skipped: number;
  message: string;
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function AdminBulkImportTeachersPage({ centerId: centerIdProp }: { centerId?: string }) {
  const storeCenterId = useAuthStore(s => s.user?.centerId);
  const centerId = centerIdProp || storeCenterId;
  const fileRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<BulkImportPreviewResponse | null>(null);
  const [result, setResult] = useState<BulkImportConfirmResponse | null>(null);
  const [isPreviewing, setIsPreviewing] = useState(false);
  const [isImporting, setIsImporting] = useState(false);

  function reset() {
    setFile(null);
    setPreview(null);
    setResult(null);
    if (fileRef.current) fileRef.current.value = '';
  }

  async function handleDownloadTemplate() {
    if (!centerId) return;
    try {
      const res = await api.get(`/api/v1/centers/${centerId}/teachers/bulk-template`, {
        responseType: 'blob',
      });
      const url = URL.createObjectURL(res.data);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'teacher-import-template.csv';
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      toast.error('Failed to download template');
    }
  }

  async function handlePreview() {
    if (!file || !centerId) return;
    setIsPreviewing(true);
    try {
      const form = new FormData();
      form.append('file', file);
      const res = await api.post<BulkImportPreviewResponse>(
        `/api/v1/centers/${centerId}/teachers/bulk-preview`,
        form,
        { headers: { 'Content-Type': 'multipart/form-data' } }
      );
      setPreview(res.data);
    } catch (err: unknown) {
      const e = err as { response?: { data?: { detail?: string } } };
      toast.error(e.response?.data?.detail ?? 'Validation failed');
    } finally {
      setIsPreviewing(false);
    }
  }

  async function handleConfirm(skipErrors: boolean) {
    if (!file || !centerId) return;
    setIsImporting(true);
    try {
      const form = new FormData();
      form.append('file', file);
      const res = await api.post<BulkImportConfirmResponse>(
        `/api/v1/centers/${centerId}/teachers/bulk-confirm?skipErrors=${skipErrors}`,
        form,
        { headers: { 'Content-Type': 'multipart/form-data' } }
      );
      setResult(res.data);
      toast.success(`${res.data.imported} teachers imported — invitation emails sent!`);
    } catch (err: unknown) {
      const e = err as { response?: { data?: { detail?: string } } };
      toast.error(e.response?.data?.detail ?? 'Import failed');
    } finally {
      setIsImporting(false);
    }
  }

  // ─── Done screen ────────────────────────────────────────────────────────────

  if (result) {
    return (
      <div className="p-6 max-w-2xl mx-auto">
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-surface-100/40 border border-emerald-500/20 rounded-2xl p-8 text-center"
        >
          <div className="w-16 h-16 rounded-full bg-emerald-500/15 border border-emerald-500/30 flex items-center justify-center mx-auto mb-5">
            <CheckCircle2 className="w-8 h-8 text-emerald-400" />
          </div>
          <h2 className="text-xl font-bold text-white mb-2">Import Complete</h2>
          <p className="text-white/50 text-sm mb-5">{result.message}</p>
          <div className="flex justify-center gap-6 mb-6">
            <div className="text-center">
              <div className="text-2xl font-bold text-emerald-400">{result.imported}</div>
              <div className="text-xs text-white/40">Imported</div>
            </div>
            {result.skipped > 0 && (
              <div className="text-center">
                <div className="text-2xl font-bold text-white/50">{result.skipped}</div>
                <div className="text-xs text-white/40">Skipped</div>
              </div>
            )}
          </div>
          <button onClick={reset} className="flex items-center gap-2 px-4 py-2 rounded-xl text-sm bg-white/5 hover:bg-white/10 text-white/70 hover:text-white transition-colors mx-auto">
            <RotateCcw className="w-4 h-4" /> Import Another File
          </button>
        </motion.div>
      </div>
    );
  }

  return (
    <div className="p-6 max-w-3xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-xl font-bold text-white flex items-center gap-2">
            <Users className="w-5 h-5 text-brand-400" /> Bulk Import Teachers
          </h1>
          <p className="text-sm text-white/40 mt-1">Upload a CSV to add up to 500 teachers at once</p>
        </div>
        <button
          onClick={handleDownloadTemplate}
          className="flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-medium text-brand-400 border border-brand-500/20 bg-brand-500/10 hover:bg-brand-500/20 transition-colors"
        >
          <Download className="w-3.5 h-3.5" /> Download Template
        </button>
      </div>

      {/* Upload Zone */}
      {!preview && (
        <label
          className={cn(
            'border-2 border-dashed rounded-2xl p-10 text-center cursor-pointer transition-all block',
            file
              ? 'border-brand-500/40 bg-brand-500/5'
              : 'border-white/10 hover:border-white/20 hover:bg-white/3'
          )}
        >
          <input
            ref={fileRef}
            type="file"
            accept=".csv,text/csv"
            className="hidden"
            onChange={e => { setFile(e.target.files?.[0] ?? null); setPreview(null); }}
          />
          {file ? (
            <>
              <FileText className="w-10 h-10 text-brand-400 mx-auto mb-3" />
              <p className="text-white font-medium text-sm">{file.name}</p>
              <p className="text-white/40 text-xs mt-1">{(file.size / 1024).toFixed(1)} KB — click to replace</p>
            </>
          ) : (
            <>
              <Upload className="w-10 h-10 text-white/20 mx-auto mb-3" />
              <p className="text-white/60 text-sm font-medium">Click to upload CSV file</p>
              <p className="text-white/30 text-xs mt-1">Max 500 teachers per file</p>
            </>
          )}
        </label>
      )}

      {/* Validate Button */}
      {file && !preview && (
        <button
          onClick={handlePreview}
          disabled={isPreviewing}
          className="w-full btn-primary flex items-center justify-center gap-2 py-3"
        >
          {isPreviewing ? <Loader2 className="w-5 h-5 animate-spin" /> : <><ArrowRight className="w-4 h-4" /> Validate CSV</>}
        </button>
      )}

      {/* Preview Results */}
      <AnimatePresence>
        {preview && (
          <motion.div
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-4"
          >
            {/* Summary */}
            <div className="bg-surface-100/40 border border-white/8 rounded-xl p-5">
              <p className="text-white font-semibold mb-3">
                {preview.totalRows} teachers found —{' '}
                <span className="text-emerald-400">{preview.validRows} valid</span>
                {preview.errorRows > 0 && (
                  <>, <span className="text-red-400">{preview.errorRows} errors</span></>
                )}
              </p>
              <div className="flex gap-2">
                {preview.errorRows > 0 && (
                  <button
                    onClick={() => handleConfirm(true)}
                    disabled={isImporting || preview.validRows === 0}
                    className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-medium bg-brand-600 text-white hover:bg-brand-500 disabled:opacity-40 transition-colors"
                  >
                    {isImporting ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle2 className="w-4 h-4" />}
                    Proceed with {preview.validRows} valid
                  </button>
                )}
                {preview.errorRows === 0 && (
                  <button
                    onClick={() => handleConfirm(false)}
                    disabled={isImporting}
                    className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-medium bg-emerald-600 text-white hover:bg-emerald-500 disabled:opacity-40 transition-colors"
                  >
                    {isImporting ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle2 className="w-4 h-4" />}
                    Import All {preview.validRows} Teachers
                  </button>
                )}
                <button
                  onClick={reset}
                  className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm text-white/50 hover:text-white hover:bg-white/5 transition-colors"
                >
                  <RotateCcw className="w-4 h-4" /> Fix & Re-upload
                </button>
              </div>
            </div>

            {/* Error List */}
            {preview.errors.length > 0 && (
              <div className="space-y-2">
                <p className="text-xs font-semibold uppercase tracking-wide text-white/40 flex items-center gap-1.5">
                  <AlertCircle className="w-3.5 h-3.5" /> Errors
                </p>
                {preview.errors.map((err, i) => (
                  <div key={i} className="flex items-start gap-3 bg-red-500/5 border border-red-500/15 rounded-xl px-4 py-3">
                    <XCircle className="w-4 h-4 text-red-400 flex-shrink-0 mt-0.5" />
                    <div>
                      <p className="text-sm text-white/80">
                        <span className="text-white/40 text-xs">Row {err.row}</span>
                        {err.email && <span className="text-white/50 text-xs ml-2">{err.email}</span>}
                        <span className="ml-2">{err.message}</span>
                      </p>
                      {err.suggestion && (
                        <p className="text-xs text-brand-400 mt-0.5">{err.suggestion}</p>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
