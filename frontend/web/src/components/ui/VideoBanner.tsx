// src/components/ui/VideoBanner.tsx
// Advanced VIDEO banner: autoplay muted, 5-second loop, progress bar,
// Intersection Observer pause-on-scroll, poster fallback, animated overlay.
import { useState, useEffect, useRef, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useQuery } from '@tanstack/react-query';
import { ExternalLink, Play, Pause, Volume2, VolumeX } from 'lucide-react';
import { cn } from '../../lib/utils';
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

export interface VideoBannerProps {
  audience: 'PARENT' | 'CENTER_ADMIN' | 'ALL';
}

// ─── Single video player slide ────────────────────────────────────────────────

interface VideoSlideProps {
  banner: BannerResponse;
  isActive: boolean;
  onEnded: () => void;
}

function VideoSlide({ banner, isActive, onEnded }: VideoSlideProps) {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const [progress, setProgress] = useState(0);
  const [muted, setMuted] = useState(true);
  const [playing, setPlaying] = useState(false);
  const [showControls, setShowControls] = useState(false);

  // Intersection Observer — pause when scrolled out of view
  useEffect(() => {
    const el = videoRef.current;
    if (!el) return;
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (!entry.isIntersecting) {
          el.pause();
        } else if (isActive) {
          el.play().catch(() => {});
        }
      },
      { threshold: 0.3 }
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, [isActive]);

  // Play/pause based on isActive prop
  useEffect(() => {
    const el = videoRef.current;
    if (!el) return;
    if (isActive) {
      el.currentTime = 0;
      el.play().then(() => setPlaying(true)).catch(() => setPlaying(false));
    } else {
      el.pause();
      el.currentTime = 0;
      setProgress(0);
      setPlaying(false);
    }
  }, [isActive]);

  const handleTimeUpdate = useCallback(() => {
    const el = videoRef.current;
    if (!el || !el.duration) return;
    setProgress((el.currentTime / el.duration) * 100);
  }, []);

  const handlePlay = useCallback(() => setPlaying(true), []);
  const handlePause = useCallback(() => setPlaying(false), []);

  const togglePlay = () => {
    const el = videoRef.current;
    if (!el) return;
    if (el.paused) { el.play().catch(() => {}); } else { el.pause(); }
  };

  const toggleMute = () => {
    const el = videoRef.current;
    if (!el) return;
    el.muted = !el.muted;
    setMuted(el.muted);
  };

  const bg = banner.bgColor ?? '#0f0a1e';
  const isHex = bg.startsWith('#');
  const gradientStyle = isHex
    ? { background: `linear-gradient(160deg, ${bg}ee 0%, ${bg}99 100%)` }
    : { background: bg };

  return (
    <div
      className="relative w-full overflow-hidden rounded-2xl"
      style={{ minHeight: 220 }}
      onMouseEnter={() => setShowControls(true)}
      onMouseLeave={() => setShowControls(false)}
    >
      {/* Video element */}
      {banner.videoUrl ? (
        <video
          ref={videoRef}
          src={banner.videoUrl}
          poster={banner.imageUrl}
          muted
          playsInline
          loop
          preload="metadata"
          onTimeUpdate={handleTimeUpdate}
          onPlay={handlePlay}
          onPause={handlePause}
          onEnded={onEnded}
          className="absolute inset-0 w-full h-full object-cover"
          style={{ zIndex: 0 }}
        />
      ) : (
        // Fallback: image-only when no videoUrl
        banner.imageUrl && (
          <img
            src={banner.imageUrl}
            alt={banner.title}
            className="absolute inset-0 w-full h-full object-cover"
            style={{ zIndex: 0 }}
          />
        )
      )}

      {/* Dark gradient overlay for text legibility */}
      <div
        className="absolute inset-0"
        style={{
          background: 'linear-gradient(to top, rgba(0,0,0,0.75) 0%, rgba(0,0,0,0.2) 50%, rgba(0,0,0,0.05) 100%)',
          zIndex: 1,
        }}
      />

      {/* Colored tint overlay (branding) */}
      {!banner.videoUrl && (
        <div className="absolute inset-0" style={{ ...gradientStyle, zIndex: 1 }} />
      )}

      {/* Content overlay */}
      <div className="relative z-10 flex flex-col justify-end h-full min-h-[220px] p-5 pb-8">
        {/* Tag */}
        <span className="inline-flex items-center gap-1.5 text-[10px] font-bold text-white/60 uppercase tracking-widest mb-2 w-fit">
          <span className="w-1.5 h-1.5 rounded-full bg-red-500 animate-pulse" />
          Advertisement
        </span>

        <h2 className="text-lg font-bold text-white leading-snug mb-1 drop-shadow-lg">
          {banner.title}
        </h2>
        {banner.subtitle && (
          <p className="text-sm text-white/70 mb-3 max-w-md drop-shadow">{banner.subtitle}</p>
        )}
        {banner.linkUrl && banner.linkLabel && (
          <a
            href={banner.linkUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1.5 px-4 py-2 bg-white/15 hover:bg-white/25 rounded-xl text-sm font-semibold text-white transition-colors border border-white/20 w-fit backdrop-blur-sm"
          >
            {banner.linkLabel}
            <ExternalLink className="w-3.5 h-3.5" />
          </a>
        )}
      </div>

      {/* Controls overlay (visible on hover) */}
      <AnimatePresence>
        {banner.videoUrl && showControls && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.15 }}
            className="absolute top-3 right-3 z-20 flex items-center gap-2"
          >
            <button
              onClick={toggleMute}
              className="p-2 rounded-full bg-black/40 hover:bg-black/60 text-white/80 hover:text-white transition-colors backdrop-blur-sm"
              title={muted ? 'Unmute' : 'Mute'}
            >
              {muted ? <VolumeX className="w-3.5 h-3.5" /> : <Volume2 className="w-3.5 h-3.5" />}
            </button>
            <button
              onClick={togglePlay}
              className="p-2 rounded-full bg-black/40 hover:bg-black/60 text-white/80 hover:text-white transition-colors backdrop-blur-sm"
              title={playing ? 'Pause' : 'Play'}
            >
              {playing ? <Pause className="w-3.5 h-3.5" /> : <Play className="w-3.5 h-3.5" />}
            </button>
          </motion.div>
        )}
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
    </div>
  );
}

// ─── Main VideoBanner component ───────────────────────────────────────────────

export default function VideoBanner({ audience }: VideoBannerProps) {
  const [current, setCurrent] = useState(0);

  const { data: allBanners = [] } = useQuery<BannerResponse[]>({
    queryKey: ['banners', audience],
    queryFn: () =>
      api.get(`/api/v1/banners?audience=${audience}`).then((r) => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
    staleTime: 60 * 1000,
  });

  const banners = allBanners.filter((b) => b.bannerType === 'VIDEO');

  const handleEnded = useCallback(() => {
    setCurrent((c) => (c + 1) % banners.length);
  }, [banners.length]);

  if (banners.length === 0) return null;

  const banner = banners[current];

  return (
    <div className="relative mb-6">
      <AnimatePresence mode="wait">
        <motion.div
          key={banner.id}
          initial={{ opacity: 0, scale: 0.98 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.98 }}
          transition={{ duration: 0.3 }}
        >
          <VideoSlide
            banner={banner}
            isActive={true}
            onEnded={handleEnded}
          />
        </motion.div>
      </AnimatePresence>

      {/* Dot nav for multiple video banners */}
      {banners.length > 1 && (
        <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex items-center gap-1.5 z-20">
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
              aria-label={`Go to video ${i + 1}`}
            />
          ))}
        </div>
      )}
    </div>
  );
}
