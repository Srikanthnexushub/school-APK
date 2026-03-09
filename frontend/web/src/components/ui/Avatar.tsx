import { cn } from '../../lib/utils';

const gradients = [
  'from-brand-600 to-violet-600',
  'from-cyan-600 to-brand-600',
  'from-emerald-600 to-teal-600',
  'from-amber-600 to-orange-600',
  'from-pink-600 to-rose-600',
  'from-indigo-600 to-purple-600',
  'from-sky-600 to-cyan-600',
  'from-violet-600 to-fuchsia-600',
];

function getGradient(name: string): string {
  const safe = name || '?';
  const hash = safe.split('').reduce((acc, c) => acc + c.charCodeAt(0), 0);
  return gradients[hash % gradients.length];
}

function getInitials(name: string): string {
  const safe = name || '?';
  return safe
    .split(' ')
    .map((w) => w[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();
}

const sizeClasses = {
  sm: 'w-8 h-8 text-xs',
  md: 'w-10 h-10 text-sm',
  lg: 'w-14 h-14 text-lg',
  xl: 'w-20 h-20 text-2xl',
};

interface AvatarProps {
  name: string;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  imageUrl?: string;
  className?: string;
}

export function Avatar({ name, size = 'md', imageUrl, className }: AvatarProps) {
  const sizeClass = sizeClasses[size];
  const gradient = getGradient(name);

  if (imageUrl) {
    return (
      <img
        src={imageUrl}
        alt={name}
        className={cn('rounded-full object-cover', sizeClass, className)}
      />
    );
  }

  return (
    <div
      className={cn(
        'rounded-full bg-gradient-to-br flex items-center justify-center font-bold text-white flex-shrink-0',
        gradient,
        sizeClass,
        className
      )}
    >
      {getInitials(name)}
    </div>
  );
}
