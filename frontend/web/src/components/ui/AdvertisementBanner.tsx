// src/components/ui/AdvertisementBanner.tsx
import { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useQuery } from '@tanstack/react-query';
import { ExternalLink } from 'lucide-react';
import { cn } from '../../lib/utils';
import api from '../../lib/api';

interface BannerResponse {
  id: string;
  title: string;
  subtitle?: string;
  imageUrl?: string;
  linkUrl?: string;
  linkLabel?: string;
  audience: string;
  bannerType?: string;
  bgColor?: string;
  displayOrder: number;
  isActive: boolean;
  startDate?: string;
  endDate?: string;
  createdAt: string;
}

export interface AdvertisementBannerProps {
  audience: 'PARENT' | 'CENTER_ADMIN' | 'ALL';
}

export default function AdvertisementBanner({ audience }: AdvertisementBannerProps) {
  const rotationMs = parseInt(import.meta.env.VITE_BANNER_ROTATION_MS ?? '5000', 10);
  const [current, setCurrent] = useState(0);
  const [paused, setPaused] = useState(false);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const { data: allBanners = [] } = useQuery<BannerResponse[]>({
    queryKey: ['banners', audience],
    queryFn: () =>
      api.get(`/api/v1/banners?audience=${audience}`).then((r) => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
    staleTime: 60 * 1000,
  });

  // Show only HERO-type banners (null/undefined treated as HERO for backward compat)
  const banners = allBanners.filter((b) => !b.bannerType || b.bannerType === 'HERO');

  useEffect(() => {
    if (banners.length <= 1 || paused) {
      if (intervalRef.current) clearInterval(intervalRef.current);
      return;
    }
    intervalRef.current = setInterval(() => {
      setCurrent((c) => (c + 1) % banners.length);
    }, rotationMs);
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [banners.length, paused, rotationMs]);

  if (banners.length === 0) return null;

  const banner = banners[current];

  // Parse bgColor into a gradient
  const bg = banner.bgColor ?? '#1e1b4b';
  const isHex = bg.startsWith('#');
  const gradientStyle = isHex
    ? { background: `linear-gradient(135deg, ${bg}cc 0%, ${bg}88 100%)` }
    : { background: bg };

  return (
    <div
      className="relative overflow-hidden rounded-2xl mb-6"
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
    >
      <AnimatePresence mode="wait">
        <motion.div
          key={banner.id}
          initial={{ opacity: 0, x: 32 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: -32 }}
          transition={{ duration: 0.35, ease: 'easeInOut' }}
          className="relative min-h-[180px] flex items-center overflow-hidden rounded-2xl"
          style={gradientStyle}
        >
          {/* Content */}
          <div className={cn('flex-1 px-6 py-6 z-10', banner.imageUrl ? 'pr-4' : '')}>
            <p className="text-xs font-semibold text-white/50 uppercase tracking-widest mb-1">
              Announcement
            </p>
            <h2 className="text-xl font-bold text-white leading-snug mb-1">
              {banner.title}
            </h2>
            {banner.subtitle && (
              <p className="text-sm text-white/70 mb-4 max-w-lg">{banner.subtitle}</p>
            )}
            {banner.linkUrl && banner.linkLabel && (
              <a
                href={banner.linkUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1.5 px-4 py-2 bg-white/15 hover:bg-white/25 rounded-xl text-sm font-semibold text-white transition-colors border border-white/20"
              >
                {banner.linkLabel}
                <ExternalLink className="w-3.5 h-3.5" />
              </a>
            )}
          </div>

          {/* Right image */}
          {banner.imageUrl && (
            <div className="hidden sm:flex w-48 h-full flex-shrink-0 items-center justify-center overflow-hidden">
              <img
                src={banner.imageUrl}
                alt={banner.title}
                className="h-full w-full object-cover opacity-90"
              />
            </div>
          )}

          {/* Decorative circles */}
          <div className="absolute -right-10 -top-10 w-40 h-40 rounded-full bg-white/5 pointer-events-none" />
          <div className="absolute -right-4 -bottom-6 w-24 h-24 rounded-full bg-white/5 pointer-events-none" />
        </motion.div>
      </AnimatePresence>

      {/* Dots */}
      {banners.length > 1 && (
        <div className="absolute bottom-3 left-1/2 -translate-x-1/2 flex items-center gap-1.5 z-20">
          {banners.map((_, i) => (
            <button
              key={i}
              onClick={() => setCurrent(i)}
              className={cn(
                'rounded-full transition-all',
                i === current
                  ? 'w-5 h-1.5 bg-white'
                  : 'w-1.5 h-1.5 bg-white/40 hover:bg-white/70'
              )}
              aria-label={`Go to banner ${i + 1}`}
            />
          ))}
        </div>
      )}
    </div>
  );
}
