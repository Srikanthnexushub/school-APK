/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        /* ── BRAND: Electric Cyan → Neon Violet ─────────────────────────── */
        brand: {
          50:  '#e0fffe',
          100: '#b3fffe',
          200: '#7dfffe',
          300: '#3dfcff',
          400: '#00f5ff',   /* electric cyan — primary neon */
          500: '#00c8d4',
          600: '#008b94',
          700: '#006570',
          800: '#004650',
          900: '#002830',
          950: '#001018',
        },
        /* ── NEON EXTENDED PALETTE ───────────────────────────────────────── */
        neon: {
          cyan:    '#00f5ff',
          magenta: '#f000ff',
          violet:  '#9b00ff',
          lime:    '#00ff88',
          pink:    '#ff006e',
          orange:  '#ff6b00',
          gold:    '#ffd700',
          blue:    '#0066ff',
        },
        surface: {
          DEFAULT: 'rgb(var(--color-surface) / <alpha-value>)',
          50:  'rgb(var(--color-surface-50) / <alpha-value>)',
          100: 'rgb(var(--color-surface-100) / <alpha-value>)',
          200: 'rgb(var(--color-surface-200) / <alpha-value>)',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['Geist Mono', 'JetBrains Mono', 'monospace'],
      },
      animation: {
        /* Existing */
        'fade-in':   'fadeIn 0.3s ease-out',
        'slide-up':  'slideUp 0.4s ease-out',
        /* Neon glow pulse */
        'neon-pulse':   'neonPulse 2.5s ease-in-out infinite',
        'neon-pulse-mg': 'neonPulseMg 2.5s ease-in-out infinite',
        /* Animated gradient border */
        'border-spin':  'borderSpin 4s linear infinite',
        /* Floating levitate */
        'float':        'float 4s ease-in-out infinite',
        /* Aurora background */
        'aurora':       'aurora 18s ease-in-out infinite alternate',
        /* Scanline sweep */
        'scanline':     'scanline 8s linear infinite',
        /* Holographic shimmer */
        'holo':         'holo 5s ease-in-out infinite',
        /* Text flicker */
        'flicker':      'flicker 0.15s linear 2',
      },
      keyframes: {
        fadeIn:  { from: { opacity: '0' },                     to: { opacity: '1' } },
        slideUp: { from: { opacity: '0', transform: 'translateY(16px)' }, to: { opacity: '1', transform: 'translateY(0)' } },
        neonPulse: {
          '0%, 100%': { boxShadow: '0 0 4px #00f5ff40, 0 0 12px #00f5ff20, 0 0 0px #00f5ff00' },
          '50%':      { boxShadow: '0 0 12px #00f5ffcc, 0 0 32px #00f5ff66, 0 0 60px #00f5ff22' },
        },
        neonPulseMg: {
          '0%, 100%': { boxShadow: '0 0 4px #f000ff40, 0 0 12px #f000ff20' },
          '50%':      { boxShadow: '0 0 12px #f000ffcc, 0 0 32px #f000ff66, 0 0 60px #f000ff22' },
        },
        borderSpin: {
          '0%':   { backgroundPosition: '0% 50%' },
          '100%': { backgroundPosition: '400% 50%' },
        },
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%':      { transform: 'translateY(-6px)' },
        },
        aurora: {
          '0%':   { backgroundPosition: '0% 50%,   100% 50%,   50% 0%' },
          '33%':  { backgroundPosition: '100% 0%,  0%   100%, 100% 50%' },
          '66%':  { backgroundPosition: '50% 100%, 50%  0%,   0%  100%' },
          '100%': { backgroundPosition: '100% 50%, 0%   50%,  50% 100%' },
        },
        scanline: {
          '0%':   { transform: 'translateY(-100%)' },
          '100%': { transform: 'translateY(100vh)' },
        },
        holo: {
          '0%, 100%': { backgroundPosition: '0% 50%' },
          '50%':      { backgroundPosition: '100% 50%' },
        },
        flicker: {
          '0%, 19%, 21%, 23%, 25%, 54%, 56%, 100%': { opacity: '1' },
          '20%, 24%, 55%':                           { opacity: '0.6' },
        },
      },
      backdropBlur: { xs: '2px' },
      boxShadow: {
        'neon-cyan':    '0 0 8px #00f5ff, 0 0 20px #00f5ff80, 0 0 40px #00f5ff33',
        'neon-mg':      '0 0 8px #f000ff, 0 0 20px #f000ff80, 0 0 40px #f000ff33',
        'neon-violet':  '0 0 8px #9b00ff, 0 0 20px #9b00ff80, 0 0 40px #9b00ff33',
        'neon-lime':    '0 0 8px #00ff88, 0 0 20px #00ff8880, 0 0 40px #00ff8833',
        'neon-sm-cyan': '0 0 4px #00f5ff, 0 0 10px #00f5ff60',
        'neon-sm-mg':   '0 0 4px #f000ff, 0 0 10px #f000ff60',
        'card-3d':      '0 20px 60px rgba(0,0,0,0.8), 0 4px 16px rgba(0,0,0,0.6)',
      },
    },
  },
  plugins: [],
};
