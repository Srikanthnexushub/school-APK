// src/components/ui/FooterBanner.tsx
import { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useQuery } from '@tanstack/react-query';
import { ExternalLink } from 'lucide-react';
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

export interface FooterBannerProps {
  audience: 'PARENT' | 'CENTER_ADMIN' | 'ALL';
}

export default function FooterBanner({ audience }: FooterBannerProps) {
  const rotationMs = parseInt(import.meta.env.VITE_BANNER_ROTATION_MS ?? '5000', 10) * 1.5;
  const [current, setCurrent] = useState(0);
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
    if (banners.length <= 1) {
      if (intervalRef.current) clearInterval(intervalRef.current);
      return;
    }
    intervalRef.current = setInterval(() => {
      setCurrent((c) => (c + 1) % banners.length);
    }, rotationMs);
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [banners.length, rotationMs]);

  if (banners.length === 0) return null;

  const banner = banners[current];
  const bg = banner.bgColor ?? '#312e81';
  const isHex = bg.startsWith('#');
  const gradientStyle = isHex
    ? { background: `linear-gradient(90deg, ${bg}dd 0%, ${bg}99 100%)` }
    : { background: bg };

  return (
    <div className="relative overflow-hidden rounded-xl" style={{ height: 48 }}>
      <AnimatePresence mode="wait">
        <motion.div
          key={banner.id}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.4 }}
          className="absolute inset-0 flex items-center px-4 gap-3"
          style={gradientStyle}
        >
          <span className="text-sm font-semibold text-white truncate flex-1">
            {banner.title}
          </span>
          {banner.linkUrl && banner.linkLabel && (
            <a
              href={banner.linkUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="flex-shrink-0 inline-flex items-center gap-1 text-xs font-semibold text-white/80 hover:text-white border border-white/20 hover:border-white/40 px-2.5 py-1 rounded-lg transition-colors"
            >
              {banner.linkLabel}
              <ExternalLink className="w-3 h-3" />
            </a>
          )}
        </motion.div>
      </AnimatePresence>
    </div>
  );
}
