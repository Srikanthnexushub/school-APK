import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatScore(score: number): string {
  return score.toFixed(1);
}

export function getRiskColor(level: string): string {
  const map: Record<string, string> = {
    LOW: 'text-emerald-400',
    MEDIUM: 'text-amber-400',
    HIGH: 'text-orange-400',
    CRITICAL: 'text-red-400',
  };
  return map[level] ?? 'text-white/60';
}

export function getReadinessLabel(score: number): { label: string; color: string } {
  if (score >= 80) return { label: 'Exam Ready', color: 'text-emerald-400' };
  if (score >= 60) return { label: 'On Track', color: 'text-brand-400' };
  if (score >= 40) return { label: 'Needs Work', color: 'text-amber-400' };
  return { label: 'At Risk', color: 'text-red-400' };
}
