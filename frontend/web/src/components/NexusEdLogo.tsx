import { useId } from 'react';

interface Props {
  /** Height in px — width scales proportionally (40:32 ratio) */
  size?: number;
  className?: string;
}

/**
 * NexusEd brand mark — open book with gradient pages, spine, page lines, and star.
 * Uses React.useId() so gradient IDs are unique when multiple instances render.
 */
export default function NexusEdLogo({ size = 20, className }: Props) {
  const uid = useId().replace(/:/g, '');
  const lgL = `ne-l-${uid}`;
  const lgR = `ne-r-${uid}`;
  const lgS = `ne-s-${uid}`;
  const w = Math.round((size * 40) / 32);

  return (
    <svg
      width={w}
      height={size}
      viewBox="0 0 40 32"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      aria-label="NexusEd"
    >
      <defs>
        <linearGradient id={lgL} x1="2" y1="5" x2="20" y2="28" gradientUnits="userSpaceOnUse">
          <stop stopColor="#a5b4fc" />
          <stop offset="1" stopColor="#6366f1" />
        </linearGradient>
        <linearGradient id={lgR} x1="20" y1="5" x2="38" y2="28" gradientUnits="userSpaceOnUse">
          <stop stopColor="#c084fc" />
          <stop offset="1" stopColor="#7c3aed" />
        </linearGradient>
        <linearGradient id={lgS} x1="19" y1="3" x2="21" y2="30" gradientUnits="userSpaceOnUse">
          <stop stopColor="#e0e7ff" />
          <stop offset="1" stopColor="#a5b4fc" />
        </linearGradient>
      </defs>

      {/* ── Left page ── */}
      <path
        d="M19.5 5.5 C17 3.8 10 3.2 2.5 5.5 L2.5 26.5 C10 24.2 17 24.8 19.5 26.5 Z"
        fill={`url(#${lgL})`}
      />
      {/* Left page text lines */}
      <line x1="6.5" y1="11"   x2="16.5" y2="10.5" stroke="rgba(255,255,255,0.40)" strokeWidth="1.1" strokeLinecap="round"/>
      <line x1="6.5" y1="15"   x2="16.5" y2="14.6" stroke="rgba(255,255,255,0.28)" strokeWidth="0.9" strokeLinecap="round"/>
      <line x1="6.5" y1="19"   x2="14.5" y2="18.7" stroke="rgba(255,255,255,0.22)" strokeWidth="0.9" strokeLinecap="round"/>
      <line x1="6.5" y1="22.5" x2="11.5" y2="22.3" stroke="rgba(255,255,255,0.16)" strokeWidth="0.8" strokeLinecap="round"/>

      {/* ── Right page ── */}
      <path
        d="M20.5 5.5 C23 3.8 30 3.2 37.5 5.5 L37.5 26.5 C30 24.2 23 24.8 20.5 26.5 Z"
        fill={`url(#${lgR})`}
      />
      {/* Right page text lines */}
      <line x1="23.5" y1="10.5" x2="33.5" y2="11"   stroke="rgba(255,255,255,0.40)" strokeWidth="1.1" strokeLinecap="round"/>
      <line x1="23.5" y1="14.6" x2="33.5" y2="15"   stroke="rgba(255,255,255,0.28)" strokeWidth="0.9" strokeLinecap="round"/>
      <line x1="23.5" y1="18.7" x2="31.5" y2="19"   stroke="rgba(255,255,255,0.22)" strokeWidth="0.9" strokeLinecap="round"/>
      <line x1="23.5" y1="22.3" x2="28.5" y2="22.5" stroke="rgba(255,255,255,0.16)" strokeWidth="0.8" strokeLinecap="round"/>

      {/* ── Spine / binding strip ── */}
      <path
        d="M18.5 4.2 Q20 2.8 21.5 4.2 L21.5 28.8 Q20 30.2 18.5 28.8 Z"
        fill={`url(#${lgS})`}
        opacity="0.88"
      />

      {/* ── Amber sparkle — top-right of right page ── */}
      <path
        d="M33 6.8 L33.7 5 L34.4 6.8 L36.2 7.5 L34.4 8.2 L33.7 10 L33 8.2 L31.2 7.5 Z"
        fill="#fbbf24"
        opacity="0.92"
      />
    </svg>
  );
}
