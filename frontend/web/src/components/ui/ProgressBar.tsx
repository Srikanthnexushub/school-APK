import { motion } from 'framer-motion';
import { cn } from '../../lib/utils';

type ProgressColor = 'brand' | 'emerald' | 'amber' | 'red';

const colorMap: Record<ProgressColor, string> = {
  brand:   'bg-indigo-500',
  emerald: 'bg-emerald-500',
  amber:   'bg-amber-500',
  red:     'bg-red-500',
};

interface ProgressBarProps {
  value: number;
  max?: number;
  color?: ProgressColor;
  className?: string;
  showLabel?: boolean;
}

export function ProgressBar({ value, max = 100, color = 'brand', className, showLabel = false }: ProgressBarProps) {
  const pct = Math.min(100, Math.max(0, (value / max) * 100));

  return (
    <div className={cn('space-y-1', className)}>
      {showLabel && (
        <div className="flex justify-between text-xs text-white/50">
          <span>{value}</span>
          <span>{max}</span>
        </div>
      )}
      <div className="h-1.5 bg-white/5 rounded-full overflow-hidden">
        <motion.div
          className={cn('h-full rounded-full', colorMap[color])}
          initial={{ width: 0 }}
          animate={{ width: `${pct}%` }}
          transition={{ duration: 0.8, ease: 'easeOut' }}
        />
      </div>
    </div>
  );
}
