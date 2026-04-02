import React, { useEffect } from 'react';
import ReactDOM from 'react-dom/client';
import { RouterProvider } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'sonner';
import { GoogleOAuthProvider } from '@react-oauth/google';
import { router } from './router';
import { useThemeStore } from './stores/themeStore';
import './index.css';

const queryClient = new QueryClient({
  defaultOptions: { queries: { staleTime: 30_000, retry: 1 } },
});

const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID ?? '';

/** Keeps `dark` class + accent attribute on <html> in sync with persisted preferences. */
function ThemeSync() {
  const theme = useThemeStore((s) => s.theme);

  useEffect(() => {
    const html = document.documentElement;
    if (theme === 'dark') {
      html.classList.add('dark');
    } else {
      html.classList.remove('dark');
    }
  }, [theme]);

  // Apply saved accent colour once on mount
  useEffect(() => {
    try {
      const accent = localStorage.getItem('edutech:accent_color') ?? 'cyan';
      document.documentElement.setAttribute('data-accent', accent);
    } catch { /* ignore */ }
  }, []);

  return (
    <Toaster
      position="top-right"
      theme={theme}
      richColors
    />
  );
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <GoogleOAuthProvider clientId={googleClientId}>
      <QueryClientProvider client={queryClient}>
        <ThemeSync />
        <RouterProvider router={router} />
      </QueryClientProvider>
    </GoogleOAuthProvider>
  </React.StrictMode>
);
