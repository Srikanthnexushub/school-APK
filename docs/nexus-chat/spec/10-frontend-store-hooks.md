# NexusChat — Frontend Store & Hooks (Full TypeScript Code)

## File Locations

```
frontend/web/src/
  types/
    chat.ts                  # All TypeScript interfaces
  store/
    chatStore.ts             # Zustand store (sessionStorage-backed, matches authStore pattern)
  hooks/
    useChatStream.ts         # SSE streaming hook
    useNudgePoller.ts        # Proactive nudge polling hook
```

---

## 1. types/chat.ts

```typescript
export interface ChatSessionSummary {
  sessionId: string;
  title: string | null;
  pageContext: string | null;
  messageCount: number;
  lastActiveAt: string;
  status: 'ACTIVE' | 'ARCHIVED';
}

export interface ChatMessageDto {
  messageId: string;
  role: 'USER' | 'ASSISTANT' | 'SYSTEM';
  content: string;
  messageType: 'TEXT' | 'ACTION_RESULT' | 'CONTEXT_CARD';
  actionPayload: Record<string, unknown> | null;
  createdAt: string;
}

export interface StartSessionResponse {
  sessionId: string;
  title: string | null;
  greeting: string;
  createdAt: string;
}

export interface ProactiveNudgeDto {
  nudgeId: string;
  triggerType: string;
  message: string;
  actionUrl: string | null;
  delivered: boolean;
  opened: boolean;
  createdAt: string;
}

export interface ChatState {
  // Session state
  activeSessionId: string | null;
  sessions: ChatSessionSummary[];
  messages: ChatMessageDto[];

  // Streaming state
  isStreaming: boolean;
  streamingContent: string;
  isLoading: boolean;

  // UI state
  isOpen: boolean;
  inputText: string;

  // Nudges
  nudges: ProactiveNudgeDto[];
  unreadNudgeCount: number;

  // Actions
  openChat: () => void;
  closeChat: () => void;
  setInputText: (text: string) => void;
  startSession: (pageContext: string) => Promise<void>;
  sendMessage: (message: string, pageContext: string) => void;
  loadSessions: () => Promise<void>;
  loadMessages: (sessionId: string) => Promise<void>;
  loadNudges: () => Promise<void>;
  markNudgeOpened: (nudgeId: string) => Promise<void>;
  archiveSession: (sessionId: string) => Promise<void>;
  reset: () => void;
}
```

---

## 2. store/chatStore.ts

```typescript
import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { ChatState, ChatMessageDto, StartSessionResponse } from '../types/chat';
import { useChatStream } from '../hooks/useChatStream';

const API_BASE = '/api/v1/chat';

// Helper: get auth token from authStore (same pattern as other stores)
function getToken(): string {
  const raw = sessionStorage.getItem('auth-storage');
  if (!raw) return '';
  try {
    return JSON.parse(raw)?.state?.token ?? '';
  } catch {
    return '';
  }
}

function authHeaders(): Record<string, string> {
  return { Authorization: `Bearer ${getToken()}`, 'Content-Type': 'application/json' };
}

const initialState = {
  activeSessionId: null as string | null,
  sessions: [] as ChatState['sessions'],
  messages: [] as ChatMessageDto[],
  isStreaming: false,
  streamingContent: '',
  isLoading: false,
  isOpen: false,
  inputText: '',
  nudges: [] as ChatState['nudges'],
  unreadNudgeCount: 0,
};

export const useChatStore = create<ChatState>()(
  persist(
    (set, get) => ({
      ...initialState,

      openChat: () => set({ isOpen: true }),
      closeChat: () => set({ isOpen: false }),
      setInputText: (text) => set({ inputText: text }),

      startSession: async (pageContext: string) => {
        set({ isLoading: true, messages: [], streamingContent: '', isStreaming: false });
        try {
          const res = await fetch(`${API_BASE}/sessions`, {
            method: 'POST',
            headers: authHeaders(),
            body: JSON.stringify({ pageContext }),
          });
          if (!res.ok) throw new Error(`HTTP ${res.status}`);
          const data: StartSessionResponse = await res.json();

          // Inject greeting as first assistant message
          const greetingMsg: ChatMessageDto = {
            messageId: `greeting-${data.sessionId}`,
            role: 'ASSISTANT',
            content: data.greeting,
            messageType: 'TEXT',
            actionPayload: null,
            createdAt: data.createdAt,
          };

          set({
            activeSessionId: data.sessionId,
            messages: [greetingMsg],
            isLoading: false,
          });

          // Refresh sessions list
          await get().loadSessions();
        } catch (err) {
          console.error('[chatStore] startSession error:', err);
          set({ isLoading: false });
        }
      },

      sendMessage: (message: string, pageContext: string) => {
        const { activeSessionId, messages, isStreaming } = get();
        if (!activeSessionId || isStreaming) return;

        // Optimistically add user message
        const userMsg: ChatMessageDto = {
          messageId: `temp-user-${Date.now()}`,
          role: 'USER',
          content: message,
          messageType: 'TEXT',
          actionPayload: null,
          createdAt: new Date().toISOString(),
        };

        set({ messages: [...messages, userMsg], isStreaming: true, streamingContent: '' });

        // Start SSE stream
        const token = getToken();
        streamChat(activeSessionId, message, pageContext, token, set, get);
      },

      loadSessions: async () => {
        try {
          const res = await fetch(`${API_BASE}/sessions`, { headers: authHeaders() });
          if (!res.ok) return;
          const data = await res.json();
          set({ sessions: Array.isArray(data) ? data : [] });
        } catch (err) {
          console.error('[chatStore] loadSessions error:', err);
        }
      },

      loadMessages: async (sessionId: string) => {
        set({ isLoading: true });
        try {
          const res = await fetch(`${API_BASE}/sessions/${sessionId}/messages`, { headers: authHeaders() });
          if (!res.ok) throw new Error(`HTTP ${res.status}`);
          const data = await res.json();
          set({ messages: Array.isArray(data) ? data : [], isLoading: false });
        } catch (err) {
          console.error('[chatStore] loadMessages error:', err);
          set({ isLoading: false });
        }
      },

      loadNudges: async () => {
        try {
          const res = await fetch(`${API_BASE}/nudges?limit=10`, { headers: authHeaders() });
          if (!res.ok) return;
          const data = await res.json();
          const nudges = Array.isArray(data) ? data : [];
          const unread = nudges.filter((n: ChatState['nudges'][0]) => !n.opened).length;
          set({ nudges, unreadNudgeCount: unread });
        } catch (err) {
          console.error('[chatStore] loadNudges error:', err);
        }
      },

      markNudgeOpened: async (nudgeId: string) => {
        try {
          await fetch(`${API_BASE}/nudges/${nudgeId}/opened`, {
            method: 'PATCH',
            headers: authHeaders(),
          });
          set((state) => ({
            nudges: state.nudges.map((n) => n.nudgeId === nudgeId ? { ...n, opened: true } : n),
            unreadNudgeCount: Math.max(0, state.unreadNudgeCount - 1),
          }));
        } catch (err) {
          console.error('[chatStore] markNudgeOpened error:', err);
        }
      },

      archiveSession: async (sessionId: string) => {
        try {
          await fetch(`${API_BASE}/sessions/${sessionId}`, {
            method: 'DELETE',
            headers: authHeaders(),
          });
          set((state) => ({
            sessions: state.sessions.filter((s) => s.sessionId !== sessionId),
            activeSessionId: state.activeSessionId === sessionId ? null : state.activeSessionId,
            messages: state.activeSessionId === sessionId ? [] : state.messages,
          }));
        } catch (err) {
          console.error('[chatStore] archiveSession error:', err);
        }
      },

      reset: () => set(initialState),
    }),
    {
      name: 'chat-storage',
      storage: createJSONStorage(() => sessionStorage),
      // Only persist UI state + active session — messages are re-loaded on demand
      partialize: (state) => ({
        activeSessionId: state.activeSessionId,
        isOpen: state.isOpen,
        unreadNudgeCount: state.unreadNudgeCount,
      }),
    }
  )
);

// ----- SSE Streaming logic (extracted to keep store clean) -----

function streamChat(
  sessionId: string,
  message: string,
  pageContext: string,
  token: string,
  set: (partial: Partial<ChatState> | ((s: ChatState) => Partial<ChatState>)) => void,
  get: () => ChatState
) {
  let accumulated = '';
  const startTime = Date.now();

  // Use fetch + ReadableStream for POST SSE (EventSource only supports GET)
  fetch(`/api/v1/chat/sessions/${sessionId}/stream`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
    },
    body: JSON.stringify({ message, pageContext }),
  })
    .then(async (response) => {
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      const reader = response.body!.getReader();
      const decoder = new TextDecoder();

      while (true) {
        const { value, done } = await reader.read();
        if (done) break;

        const chunk = decoder.decode(value, { stream: true });
        const lines = chunk.split('\n').filter((l) => l.trim());

        for (const line of lines) {
          if (!line.startsWith('data:')) continue;
          const data = line.slice(5).trim();

          if (data === '[DONE]') {
            // Stream complete — persist full message
            const finalContent = accumulated;
            const assistantMsg: ChatMessageDto = {
              messageId: `assistant-${Date.now()}`,
              role: 'ASSISTANT',
              content: finalContent,
              messageType: 'TEXT',
              actionPayload: null,
              createdAt: new Date().toISOString(),
            };
            set((state) => ({
              messages: [...state.messages, assistantMsg],
              isStreaming: false,
              streamingContent: '',
            }));
            // Refresh sessions to pick up updated title
            get().loadSessions();
            return;
          }

          // Parse OpenRouter delta format: {"choices":[{"delta":{"content":"token"}}]}
          try {
            const parsed = JSON.parse(data);
            const token = parsed?.choices?.[0]?.delta?.content;
            if (token) {
              accumulated += token;
              set({ streamingContent: accumulated });
            }
          } catch {
            // Ignore parse errors (can happen with partial chunks)
          }
        }
      }
    })
    .catch((err) => {
      console.error('[chatStore] stream error:', err);
      const errorMsg: ChatMessageDto = {
        messageId: `error-${Date.now()}`,
        role: 'ASSISTANT',
        content: 'Sorry, I ran into an issue. Please try again.',
        messageType: 'TEXT',
        actionPayload: null,
        createdAt: new Date().toISOString(),
      };
      set((state) => ({
        messages: [...state.messages, errorMsg],
        isStreaming: false,
        streamingContent: '',
      }));
    });
}
```

---

## 3. hooks/useNudgePoller.ts

```typescript
import { useEffect } from 'react';
import { useChatStore } from '../store/chatStore';
import { useAuthStore } from '../store/authStore';

const POLL_INTERVAL_MS = 30_000; // 30 seconds

/**
 * Polls for new proactive nudges every 30 seconds.
 * Should be mounted once in AppLayout.tsx.
 */
export function useNudgePoller() {
  const { loadNudges } = useChatStore();
  const { isAuthenticated } = useAuthStore();

  useEffect(() => {
    if (!isAuthenticated) return;

    // Initial load
    loadNudges();

    const interval = setInterval(() => {
      loadNudges();
    }, POLL_INTERVAL_MS);

    return () => clearInterval(interval);
  }, [isAuthenticated, loadNudges]);
}
```

---

## 4. Integration into AppLayout.tsx (complete changes)

```tsx
// Add these imports at the top of AppLayout.tsx:
import NexusChatWidget from '../components/chat/NexusChatWidget';
import { useNudgePoller } from '../hooks/useNudgePoller';

// Inside AppLayout component, add the hook call:
const AppLayout: React.FC = () => {
  // ... existing hooks ...
  useNudgePoller();   // ADD THIS LINE

  return (
    // ... existing JSX ...
    <>
      {/* existing layout JSX unchanged */}
      <NexusChatWidget />   {/* ADD THIS LINE — just before closing root div */}
    </>
  );
};
```

**That's all** — 2 imports + 2 lines added to AppLayout.tsx. Everything else is zero-change.
