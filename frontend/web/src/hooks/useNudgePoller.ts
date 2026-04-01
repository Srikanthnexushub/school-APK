import { useEffect } from 'react';
import { useChatStore } from '../store/chatStore';

const POLL_INTERVAL_MS = 30_000;

export function useNudgePoller(isAuthenticated: boolean) {
  const { loadNudges } = useChatStore();

  useEffect(() => {
    if (!isAuthenticated) return;

    loadNudges();
    const interval = setInterval(loadNudges, POLL_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [isAuthenticated, loadNudges]);
}
