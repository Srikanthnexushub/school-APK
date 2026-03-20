// src/components/ui/TickerBanner.tsx
import { useQuery } from '@tanstack/react-query';
import { ExternalLink } from 'lucide-react';
import api from '../../lib/api';

interface BannerResponse {
  id: string;
  title: string;
  subtitle?: string;
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

export interface TickerBannerProps {
  audience: 'PARENT' | 'CENTER_ADMIN' | 'ALL';
}

const KEYFRAME_ID = 'edutech-ticker-keyframe';

function ensureKeyframe() {
  if (document.getElementById(KEYFRAME_ID)) return;
  const style = document.createElement('style');
  style.id = KEYFRAME_ID;
  style.textContent = `
    @keyframes edutech-ticker {
      0%   { transform: translateX(0); }
      100% { transform: translateX(-50%); }
    }
  `;
  document.head.appendChild(style);
}

export default function TickerBanner({ audience }: TickerBannerProps) {
  const speedS = parseInt(import.meta.env.VITE_TICKER_SPEED_S ?? '30', 10);

  const { data: allBanners = [] } = useQuery<BannerResponse[]>({
    queryKey: ['banners', audience],
    queryFn: () =>
      api.get(`/api/v1/banners?audience=${audience}`).then((r) => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
    staleTime: 60 * 1000,
  });

  // Only render TICKER-type banners
  const banners = allBanners.filter((b) => b.bannerType === 'TICKER');

  if (banners.length === 0) return null;

  ensureKeyframe();

  // Duplicate items so the marquee loops seamlessly
  const items = [...banners, ...banners];

  return (
    <div
      className="relative overflow-hidden rounded-xl mb-4"
      style={{ height: 44 }}
      aria-label="Ticker announcements"
    >
      {/* Background strip */}
      <div className="absolute inset-0 bg-white/5 border border-white/8 rounded-xl" />

      {/* Label */}
      <div className="absolute left-0 top-0 h-full flex items-center z-10 pl-3 pr-3 border-r border-white/10">
        <span className="text-[10px] font-bold text-white/40 uppercase tracking-widest whitespace-nowrap">
          Ads
        </span>
      </div>

      {/* Scrolling track */}
      <div className="absolute inset-0 left-[52px] overflow-hidden flex items-center">
        <div
          className="flex items-center gap-0 whitespace-nowrap"
          style={{
            animation: `edutech-ticker ${speedS}s linear infinite`,
            width: 'max-content',
          }}
        >
          {items.map((banner, idx) => {
            const dot = banner.bgColor ?? '#6366f1';
            return (
              <span
                key={`${banner.id}-${idx}`}
                className="inline-flex items-center gap-2 px-5"
              >
                {/* Color dot */}
                <span
                  className="w-2 h-2 rounded-full flex-shrink-0 opacity-80"
                  style={{ background: dot }}
                />
                {/* Title */}
                <span className="text-xs font-semibold text-white/80">
                  {banner.title}
                </span>
                {/* Subtitle */}
                {banner.subtitle && (
                  <span className="text-xs text-white/45">
                    — {banner.subtitle}
                  </span>
                )}
                {/* CTA link */}
                {banner.linkUrl && banner.linkLabel && (
                  <a
                    href={banner.linkUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    onClick={(e) => e.stopPropagation()}
                    className="inline-flex items-center gap-1 text-[11px] font-semibold text-brand-400 hover:text-brand-300 transition-colors"
                  >
                    {banner.linkLabel}
                    <ExternalLink className="w-2.5 h-2.5" />
                  </a>
                )}
                {/* Separator */}
                <span className="text-white/15 ml-3">·</span>
              </span>
            );
          })}
        </div>
      </div>
    </div>
  );
}
