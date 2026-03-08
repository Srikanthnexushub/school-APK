import { useState } from 'react';
import { Star } from 'lucide-react';
import { cn } from '../../lib/utils';

const sizeClasses = {
  sm: 'w-3.5 h-3.5',
  md: 'w-5 h-5',
  lg: 'w-7 h-7',
};

interface StarRatingProps {
  value: number;
  onChange?: (v: number) => void;
  max?: number;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

export function StarRating({ value, onChange, max = 5, size = 'md', className }: StarRatingProps) {
  const [hovered, setHovered] = useState<number | null>(null);
  const interactive = !!onChange;

  const display = hovered ?? value;

  return (
    <div className={cn('flex items-center gap-0.5', className)}>
      {Array.from({ length: max }, (_, i) => {
        const starValue = i + 1;
        const filled = starValue <= display;
        return (
          <button
            key={i}
            type="button"
            disabled={!interactive}
            onClick={() => onChange?.(starValue)}
            onMouseEnter={() => interactive && setHovered(starValue)}
            onMouseLeave={() => interactive && setHovered(null)}
            className={cn(
              'transition-transform',
              interactive && 'cursor-pointer hover:scale-110',
              !interactive && 'cursor-default'
            )}
          >
            <Star
              className={cn(
                sizeClasses[size],
                'transition-colors',
                filled ? 'fill-amber-400 text-amber-400' : 'fill-transparent text-white/20'
              )}
            />
          </button>
        );
      })}
    </div>
  );
}
