import { cn } from '../../lib/utils';

type GaugeSize = 'sm' | 'md' | 'lg';

const sizeMap: Record<GaugeSize, { svgSize: number; r: number; strokeWidth: number; valueSize: string; labelSize: string }> = {
  sm: { svgSize: 80,  r: 28, strokeWidth: 6,  valueSize: 'text-lg',  labelSize: 'text-xs' },
  md: { svgSize: 120, r: 44, strokeWidth: 8,  valueSize: 'text-2xl', labelSize: 'text-sm' },
  lg: { svgSize: 160, r: 60, strokeWidth: 10, valueSize: 'text-3xl', labelSize: 'text-base' },
};

interface RingGaugeProps {
  value: number;       // 0–100
  color?: string;
  label?: string;
  size?: GaugeSize;
  className?: string;
}

function getColorForValue(value: number): string {
  if (value >= 80) return '#10b981'; // emerald
  if (value >= 60) return '#6366f1'; // brand
  if (value >= 40) return '#f59e0b'; // amber
  if (value >= 20) return '#f97316'; // orange
  return '#ef4444';                  // red
}

export function RingGauge({ value, color, label, size = 'md', className }: RingGaugeProps) {
  const { svgSize, r, strokeWidth, valueSize, labelSize } = sizeMap[size];
  const cx = svgSize / 2;
  const cy = svgSize / 2;
  const circumference = 2 * Math.PI * r;
  const clampedValue = Math.min(100, Math.max(0, value));
  const offset = circumference - (clampedValue / 100) * circumference;
  const activeColor = color ?? getColorForValue(clampedValue);

  return (
    <div className={cn('relative inline-flex flex-col items-center gap-1', className)}>
      <div className="relative" style={{ width: svgSize, height: svgSize }}>
        <svg
          width={svgSize}
          height={svgSize}
          className="rotate-[-90deg]"
          aria-label={`${clampedValue}% ${label ?? ''}`}
        >
          {/* Track */}
          <circle
            cx={cx}
            cy={cy}
            r={r}
            fill="none"
            stroke="rgba(255,255,255,0.06)"
            strokeWidth={strokeWidth}
          />
          {/* Progress */}
          <circle
            cx={cx}
            cy={cy}
            r={r}
            fill="none"
            stroke={activeColor}
            strokeWidth={strokeWidth}
            strokeDasharray={circumference}
            strokeDashoffset={offset}
            strokeLinecap="round"
            style={{ transition: 'stroke-dashoffset 0.8s ease, stroke 0.5s ease' }}
          />
        </svg>
        {/* Center label */}
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <span className={cn('font-bold text-white', valueSize)}>{clampedValue}</span>
        </div>
      </div>
      {label && (
        <span className={cn('text-white/50 text-center', labelSize)}>{label}</span>
      )}
    </div>
  );
}
