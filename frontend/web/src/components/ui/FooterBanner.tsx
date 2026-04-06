// src/components/ui/FooterBanner.tsx
import { useState, useEffect, useRef, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useQuery } from '@tanstack/react-query';
import { ExternalLink, Volume2, VolumeX, Play, Pause } from 'lucide-react';
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

// Default 4K event video shown when no DB FOOTER_VIDEO banners exist
const DEFAULT_BANNER: BannerResponse = {
  id: '__default__',
  title: 'Event Management Services',
  subtitle: 'World-class events, conferences & galas — powered by NexusED',
  videoUrl: 'https://assets.mixkit.co/videos/5227/5227-1080.mp4',
  linkUrl: '',
  linkLabel: '',
  audience: 'ALL',
  bannerType: 'FOOTER_VIDEO',
  displayOrder: 0,
  isActive: true,
  createdAt: '',
};

export interface FooterBannerProps {
  audience: 'PARENT' | 'CENTER_ADMIN' | 'ALL';
}

export default function FooterBanner({ audience }: FooterBannerProps) {
  const rotationMs = parseInt(import.meta.env.VITE_BANNER_ROTATION_MS ?? '8000', 10);
  const [current, setCurrent] = useState(0);
  const [muted, setMuted] = useState(true);
  const [paused, setPaused] = useState(false);
  const [playing, setPlaying] = useState(false);
  const [progress, setProgress] = useState(0);
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

  const dbBanners = allBanners.filter((b) => b.bannerType === 'FOOTER_VIDEO');
  const banners = dbBanners.length > 0 ? dbBanners : [DEFAULT_BANNER];

  // Auto-advance interval
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
  }, [banners.length, rotationMs, paused]);

  // Play/pause
  useEffect(() => {
    if (!videoRef.current) return;
    if (paused) { videoRef.current.pause(); } else { videoRef.current.play().catch(() => {}); }
  }, [paused]);

  // Reset on banner change
  useEffect(() => {
    if (videoRef.current) {
      videoRef.current.load();
      if (!paused) videoRef.current.play().catch(() => {});
    }
    setProgress(0);
  }, [current]); // eslint-disable-line

  const handleTimeUpdate = useCallback(() => {
    const el = videoRef.current;
    if (!el || !el.duration) return;
    setProgress((el.currentTime / el.duration) * 100);
  }, []);

  const banner = banners[current];

  return (
    <div
      className="relative overflow-hidden rounded-2xl"
      style={{
        minHeight: 220,
        background: 'linear-gradient(135deg, #6d28d9 0%, #db2777 55%, #f97316 100%)',
      }}
    >
      <AnimatePresence mode="wait">
        <motion.div
          key={banner.id}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.4 }}
          className="absolute inset-0"
        >
          {/* Video */}
          {banner.videoUrl && (
            <video
              ref={videoRef}
              src={banner.videoUrl}
              autoPlay
              muted={muted}
              playsInline
              loop
              preload="metadata"
              poster={banner.imageUrl ?? undefined}
              onTimeUpdate={handleTimeUpdate}
              onPlay={() => setPlaying(true)}
              onPause={() => setPlaying(false)}
              className="absolute inset-0 w-full h-full object-cover"
              style={{ zIndex: 0 }}
            />
          )}

          {/* Dark gradient overlay */}
          <div
            className="absolute inset-0"
            style={{
              background: 'linear-gradient(to top, rgba(0,0,0,0.75) 0%, rgba(0,0,0,0.2) 50%, rgba(0,0,0,0.05) 100%)',
              zIndex: 1,
            }}
          />

          {/* Content — text at bottom like header VideoBanner */}
          <div
            className="relative z-10 flex flex-col justify-end h-full p-5 pb-8"
            style={{ minHeight: 220 }}
          >
            <span
              className="inline-flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-widest mb-2 w-fit"
              style={{ color: 'rgba(255,255,255,0.6)' }}
            >
              <span className="w-1.5 h-1.5 rounded-full bg-red-500 animate-pulse" />
              Advertisement
            </span>

            <h2
              className="text-lg font-bold leading-snug mb-1 drop-shadow-lg"
              style={{ color: '#ffffff' }}
            >
              {banner.title}
            </h2>
            {banner.subtitle && (
              <p
                className="text-sm mb-3 max-w-md drop-shadow"
                style={{ color: 'rgba(255,255,255,0.85)' }}
              >
                {banner.subtitle}
              </p>
            )}
            {banner.linkUrl && banner.linkLabel && (
              <a
                href={banner.linkUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1.5 px-4 py-2 bg-white/15 hover:bg-white/25 rounded-xl text-sm font-semibold border border-white/20 w-fit backdrop-blur-sm transition-colors"
                style={{ color: '#ffffff' }}
              >
                {banner.linkLabel}
                <ExternalLink className="w-3.5 h-3.5" />
              </a>
            )}
          </div>

          {/* Controls — top right on hover */}
          <div className="absolute top-3 right-3 z-20 flex items-center gap-2">
            {banner.videoUrl && (
              <>
                <button
                  onClick={() => setMuted((m) => !m)}
                  className="p-2 rounded-full bg-black/40 hover:bg-black/60 transition-colors backdrop-blur-sm"
                  style={{ color: 'rgba(255,255,255,0.8)' }}
                  title={muted ? 'Unmute' : 'Mute'}
                >
                  {muted ? <VolumeX className="w-3.5 h-3.5" /> : <Volume2 className="w-3.5 h-3.5" />}
                </button>
                <button
                  onClick={() => setPaused((p) => !p)}
                  className="p-2 rounded-full bg-black/40 hover:bg-black/60 transition-colors backdrop-blur-sm"
                  style={{ color: 'rgba(255,255,255,0.8)' }}
                  title={playing ? 'Pause' : 'Play'}
                >
                  {paused ? <Play className="w-3.5 h-3.5" /> : <Pause className="w-3.5 h-3.5" />}
                </button>
              </>
            )}
          </div>
        </motion.div>
      </AnimatePresence>

      {/* Progress bar */}
      {banner.videoUrl && (
        <div className="absolute bottom-0 left-0 right-0 h-1 bg-white/10 z-20">
          <motion.div
            className="h-full bg-white/70 rounded-full"
            style={{ width: `${progress}%` }}
            transition={{ duration: 0.1 }}
          />
        </div>
      )}

      {/* Dot nav for multiple banners */}
      {banners.length > 1 && (
        <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex items-center gap-1.5 z-20">
          {banners.map((_, i) => (
            <button
              key={i}
              onClick={() => setCurrent(i)}
              className={`rounded-full transition-all ${
                i === current ? 'w-5 h-1.5 bg-white' : 'w-1.5 h-1.5 bg-white/40 hover:bg-white/70'
              }`}
              aria-label={`Go to banner ${i + 1}`}
            />
          ))}
        </div>
      )}
    </div>
  );
}
