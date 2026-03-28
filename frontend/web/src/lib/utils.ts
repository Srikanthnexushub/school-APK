import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

/** crypto.randomUUID() requires a secure context (HTTPS). This wrapper falls
 *  back to a Math.random-based v4 UUID when running over plain HTTP so login
 *  works on EC2 before TLS is set up. */
export function randomUUID(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16);
  });
}

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
