import { useCallback, useEffect, useRef, useState } from 'react';
import { useAuthStore } from '../stores/authStore';
import api from '../lib/api';

export interface AppNotification {
  id: string;
  subject: string;
  body: string;
  status: string;
  channel: string;
  notificationType: string | null;
  actionUrl: string | null;
  createdAt: string;
  readAt: string | null;
}

interface UseNotificationsReturn {
  notifications: AppNotification[];
  unreadCount: number;
  isLoading: boolean;
  markRead: (id: string) => void;
  markAllRead: () => void;
}

export function useNotifications(): UseNotificationsReturn {
  const { token } = useAuthStore();
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const abortRef = useRef<AbortController | null>(null);

  // Initial fetch of IN_APP notifications
  const fetchNotifications = useCallback(async () => {
    if (!token) return;
    try {
      const res = await api.get<{ content: AppNotification[] }>('/api/v1/notifications/inapp?size=50&sort=createdAt,desc');
      const items: AppNotification[] = Array.isArray(res.data)
        ? res.data
        : (res.data.content ?? []);
      setNotifications(items);
      setUnreadCount(items.filter((n) => !n.readAt).length);
    } catch {
      // Silently ignore — service may be starting up
    } finally {
      setIsLoading(false);
    }
  }, [token]);

  useEffect(() => {
    fetchNotifications();
  }, [fetchNotifications]);

  // SSE real-time subscription
  useEffect(() => {
    if (!token) return;

    const controller = new AbortController();
    abortRef.current = controller;

    let retryDelay = 2000;
    let retryTimeout: ReturnType<typeof setTimeout>;

    async function connect() {
      try {
        const response = await fetch('/api/v1/notifications/stream', {
          headers: { Authorization: `Bearer ${token}` },
          signal: controller.signal,
        });

        if (!response.ok || !response.body) return;

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        while (true) {
          const { value, done } = await reader.read();
          if (done) break;
          buffer += decoder.decode(value, { stream: true });

          // SSE messages are separated by double newlines
          const parts = buffer.split('\n\n');
          buffer = parts.pop() ?? '';

          for (const part of parts) {
            const dataLine = part.split('\n').find((l) => l.startsWith('data:'));
            if (!dataLine) continue;
            const json = dataLine.slice(5).trim();
            if (!json) continue;
            try {
              const incoming: AppNotification = JSON.parse(json);
              setNotifications((prev) => {
                if (prev.some((n) => n.id === incoming.id)) return prev;
                if (!incoming.readAt) setUnreadCount((c) => c + 1);
                return [incoming, ...prev];
              });
            } catch {
              // Ignore malformed SSE frames
            }
          }
        }
        // Stream ended — reconnect after short delay
        retryDelay = 2000;
        retryTimeout = setTimeout(connect, retryDelay);
      } catch (err: unknown) {
        if ((err as { name?: string })?.name === 'AbortError') return;
        // Exponential backoff up to 30 s
        retryDelay = Math.min(retryDelay * 2, 30000);
        retryTimeout = setTimeout(connect, retryDelay);
      }
    }

    connect();
    return () => {
      controller.abort();
      clearTimeout(retryTimeout);
    };
  }, [token]);

  const markRead = useCallback((id: string) => {
    api.put(`/api/v1/notifications/${id}/read`).catch(() => {});
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, readAt: new Date().toISOString() } : n))
    );
    setUnreadCount((c) => Math.max(0, c - 1));
  }, []);

  const markAllRead = useCallback(() => {
    api.put('/api/v1/notifications/mark-all-read').catch(() => {});
    const now = new Date().toISOString();
    setNotifications((prev) => prev.map((n) => ({ ...n, readAt: n.readAt ?? now })));
    setUnreadCount(0);
  }, []);

  return { notifications, unreadCount, isLoading, markRead, markAllRead };
}
