import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: { alias: { '@': path.resolve(__dirname, './src') } },
  server: {
    host: true,
    port: 3000,
    proxy: {
      // Student-gateway routes (port 8089) — must be listed before the catch-all
      '/api/v1/students':             'http://localhost:8089',
      '/api/v1/exam-tracker':         'http://localhost:8089',
      '/api/v1/performance':          'http://localhost:8089',
      '/api/v1/doubts':               'http://localhost:8089',
      '/api/v1/recommendations':      'http://localhost:8089',
      '/api/v1/study-plans':          'http://localhost:8089',
      '/api/v1/career-profiles':      'http://localhost:8089',
      '/api/v1/career-recommendations': 'http://localhost:8089',
      '/api/v1/college-predictions':  'http://localhost:8089',
      '/api/v1/mentors':              'http://localhost:8089',
      '/api/v1/mentor-sessions':      'http://localhost:8089',
      // Api-gateway catch-all (port 8180) — auth, parent, center, psych, assess, ai
      '/api': 'http://localhost:8180',
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor-react':   ['react', 'react-dom', 'react-router-dom'],
          'vendor-query':   ['@tanstack/react-query'],
          'vendor-charts':  ['recharts'],
          'vendor-motion':  ['framer-motion'],
          'vendor-forms':   ['react-hook-form', 'zod', '@hookform/resolvers'],
          'vendor-ui':      ['lucide-react', 'sonner', 'clsx', 'tailwind-merge', 'class-variance-authority'],
          'vendor-state':   ['zustand', 'axios'],
        },
      },
    },
  },
});
