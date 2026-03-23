// src/components/ui/FooterBanner.tsx
import { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useQuery } from '@tanstack/react-query';
import { ExternalLink, Volume2, VolumeX, ChevronLeft, ChevronRight } from 'lucide-react';
import api from '../../lib/api';

interface BannerResponse {
  id: string;
  title: string;
  subtitle?: string;
  imageUrl?: string;
  videoUrl?: string;
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
  const rotationMs = parseInt(import.meta.env.VITE_BANNER_ROTATION_MS ?? '8000', 10);
  const [current, setCurrent] = useState(0);
  const [muted, setMuted] = useState(true);
  const videoRef = useRef<HTMLVideoElement | null>(null);
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

  // Show only FOOTER_VIDEO banners
  const banners = allBanners.filter((b) => b.bannerType === 'FOOTER_VIDEO');

  // Auto-advance interval
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

  // Reset video on banner change
  useEffect(() => {
    if (videoRef.current) {
      videoRef.current.load();
      videoRef.current.play().catch(() => {});
    }
  }, [current]);

  if (banners.length === 0) return null;

  const banner = banners[current];

  function prev() {
    setCurrent((c) => (c - 1 + banners.length) % banners.length);
  }
  function next() {
    setCurrent((c) => (c + 1) % banners.length);
  }

  return (
    <div className="relative overflow-hidden rounded-xl" style={{ height: 160 }}>
      <AnimatePresence mode="wait">
        <motion.div
          key={banner.id}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.4 }}
          className="absolute inset-0"
        >
          {/* Video background */}
          {banner.videoUrl ? (
            <video
              ref={videoRef}
              src={banner.videoUrl}
              autoPlay
              muted={muted}
              playsInline
              loop
              preload="metadata"
              poster={banner.imageUrl ?? undefined}
              className="absolute inset-0 w-full h-full object-cover"
            />
          ) : banner.imageUrl ? (
            <img
              src={banner.imageUrl}
              alt={banner.title}
              className="absolute inset-0 w-full h-full object-cover"
            />
          ) : (
            <div
              className="absolute inset-0"
              style={{ background: banner.bgColor ?? 'linear-gradient(90deg, #312e81dd, #312e8199)' }}
            />
          )}

          {/* Dark overlay for text legibility */}
          <div className="absolute inset-0 bg-gradient-to-r from-black/70 via-black/40 to-black/20" />

          {/* Content overlay */}
          <div className="absolute inset-0 flex items-center px-4 gap-3">
            <div className="flex-1 min-w-0">
              <p className="text-sm font-bold text-white truncate">{banner.title}</p>
              {banner.subtitle && (
                <p className="text-xs text-white/70 truncate mt-0.5">{banner.subtitle}</p>
              )}
            </div>

            <div className="flex items-center gap-2 flex-shrink-0">
              {banner.linkUrl && banner.linkLabel && (
                <a
                  href={banner.linkUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1 text-xs font-semibold text-white/90 hover:text-white border border-white/30 hover:border-white/60 px-2.5 py-1 rounded-lg transition-colors bg-black/20"
                >
                  {banner.linkLabel}
                  <ExternalLink className="w-3 h-3" />
                </a>
              )}

              {/* Mute toggle (only for video banners) */}
              {banner.videoUrl && (
                <button
                  onClick={() => setMuted((m) => !m)}
                  className="p-1.5 rounded-lg bg-black/30 border border-white/15 text-white/70 hover:text-white transition-colors"
                >
                  {muted ? <VolumeX className="w-3.5 h-3.5" /> : <Volume2 className="w-3.5 h-3.5" />}
                </button>
              )}
            </div>
          </div>

          {/* Prev / Next controls (only when multiple banners) */}
          {banners.length > 1 && (
            <>
              <button
                onClick={prev}
                className="absolute left-2 top-1/2 -translate-y-1/2 p-1 rounded-full bg-black/30 border border-white/10 text-white/60 hover:text-white transition-colors"
              >
                <ChevronLeft className="w-3.5 h-3.5" />
              </button>
              <button
                onClick={next}
                className="absolute right-2 top-1/2 -translate-y-1/2 p-1 rounded-full bg-black/30 border border-white/10 text-white/60 hover:text-white transition-colors"
              >
                <ChevronRight className="w-3.5 h-3.5" />
              </button>

              {/* Dot nav */}
              <div className="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-1">
                {banners.map((_, i) => (
                  <button
                    key={i}
                    onClick={() => setCurrent(i)}
                    className={`w-1.5 h-1.5 rounded-full transition-all ${
                      i === current ? 'bg-white scale-125' : 'bg-white/35'
                    }`}
                  />
                ))}
              </div>
            </>
          )}
        </motion.div>
      </AnimatePresence>
    </div>
  );
}
