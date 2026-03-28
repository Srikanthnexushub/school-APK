import { useState, useRef, useEffect } from 'react';
import { Download, Printer, FileText, ChevronDown } from 'lucide-react';
import { cn } from '../../lib/utils';
import { downloadCSV, printPage } from '../../lib/export';

interface ExportMenuProps {
  /** Label on the button. Defaults to "Export" */
  label?: string;
  /** Rows to export as CSV. If not provided, CSV option is hidden. */
  csvData?: Record<string, unknown>[];
  /** Filename for the CSV download (without .csv) */
  csvFilename?: string;
  /** Extra className on the trigger button */
  className?: string;
}

export function ExportMenu({ label = 'Export', csvData, csvFilename = 'export', className }: ExportMenuProps) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handler(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  return (
    <div ref={ref} className="relative inline-block">
      <button
        onClick={() => setOpen(o => !o)}
        className={cn(
          'flex items-center gap-1.5 px-3 py-2 rounded-xl border border-white/10 text-white/60 hover:text-white hover:bg-white/5 transition-all text-sm',
          className
        )}
      >
        <Download className="w-4 h-4" />
        {label}
        <ChevronDown className={cn('w-3 h-3 transition-transform', open && 'rotate-180')} />
      </button>

      {open && (
        <div className="absolute right-0 mt-1 w-44 glass rounded-xl border border-white/10 shadow-2xl z-50 overflow-hidden">
          {csvData && csvData.length > 0 && (
            <button
              onClick={() => { downloadCSV(csvData, csvFilename); setOpen(false); }}
              className="w-full flex items-center gap-2.5 px-4 py-3 text-sm text-white/70 hover:text-white hover:bg-white/5 transition-colors"
            >
              <FileText className="w-4 h-4 text-emerald-400" />
              Download CSV
            </button>
          )}
          <button
            onClick={() => { printPage(); setOpen(false); }}
            className="w-full flex items-center gap-2.5 px-4 py-3 text-sm text-white/70 hover:text-white hover:bg-white/5 transition-colors"
          >
            <Printer className="w-4 h-4 text-indigo-400" />
            Print
          </button>
        </div>
      )}
    </div>
  );
}
