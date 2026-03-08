import { motion } from 'framer-motion';
import { TrendingUp, TrendingDown } from 'lucide-react';
import { cn } from '../../lib/utils';

interface StatCardProps {
  title: string;
  value: string | number;
  icon: React.ReactNode;
  iconBg?: string;
  trend?: { value: number; label: string };
  subtitle?: string;
  className?: string;
}

export function StatCard({ title, value, icon, iconBg = 'bg-indigo-500/20', trend, subtitle, className }: StatCardProps) {
  const isPositive = (trend?.value ?? 0) >= 0;

  return (
    <motion.div
      className={cn('glass rounded-2xl p-6', className)}
      whileHover={{ scale: 1.01 }}
      transition={{ type: 'spring', stiffness: 300, damping: 20 }}
    >
      <div className="flex items-start justify-between mb-4">
        <p className="text-white/50 text-sm font-medium">{title}</p>
        <div className={cn('w-10 h-10 rounded-xl flex items-center justify-center', iconBg)}>
          {icon}
        </div>
      </div>

      <p className="text-3xl font-bold text-white mb-1">{value}</p>

      {subtitle && (
        <p className="text-white/40 text-xs">{subtitle}</p>
      )}

      {trend && (
        <div className={cn('flex items-center gap-1 mt-2 text-xs font-medium', isPositive ? 'text-emerald-400' : 'text-red-400')}>
          {isPositive ? <TrendingUp className="w-3 h-3" /> : <TrendingDown className="w-3 h-3" />}
          <span>{isPositive ? '+' : ''}{trend.value}</span>
          <span className="text-white/40">{trend.label}</span>
        </div>
      )}
    </motion.div>
  );
}
