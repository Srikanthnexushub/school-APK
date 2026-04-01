import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { ChatMessageDto, ChatSessionSummary, ProactiveNudgeDto, StartSessionResponse } from '../types/chat';

const API_BASE = '/api/v1/chat';

function getToken(): string {
  const raw = sessionStorage.getItem('edupath-auth');
  if (!raw) return '';
  try {
    return (JSON.parse(raw) as { state?: { token?: string } })?.state?.token ?? '';
  } catch {
    return '';
  }
}

function authHeaders(): Record<string, string> {
  return { Authorization: `Bearer ${getToken()}`, 'Content-Type': 'application/json' };
}

interface ChatState {
  activeSessionId: string | null;
  sessions: ChatSessionSummary[];
  messages: ChatMessageDto[];
  isStreaming: boolean;
  streamingContent: string;
  isLoading: boolean;
  isOpen: boolean;
  inputText: string;
  nudges: ProactiveNudgeDto[];
  unreadNudgeCount: number;

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

const initialState = {
  activeSessionId: null as string | null,
  sessions: [] as ChatSessionSummary[],
  messages: [] as ChatMessageDto[],
  isStreaming: false,
  streamingContent: '',
  isLoading: false,
  isOpen: false,
  inputText: '',
  nudges: [] as ProactiveNudgeDto[],
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
          const data = await res.json() as { sessionId: string; greeting: string; createdAt: string };

          const greetingMsg: ChatMessageDto = {
            messageId: `greeting-${data.sessionId}`,
            role: 'ASSISTANT',
            content: data.greeting,
            messageType: 'TEXT',
            actionPayload: null,
            createdAt: data.createdAt,
          };

          set({ activeSessionId: data.sessionId, messages: [greetingMsg], isLoading: false });
          await get().loadSessions();
        } catch (err) {
          console.error('[chatStore] startSession error:', err);
          set({ isLoading: false });
        }
      },

      sendMessage: (message: string, pageContext: string) => {
        const { activeSessionId, messages, isStreaming } = get();
        if (!activeSessionId || isStreaming) return;

        const userMsg: ChatMessageDto = {
          messageId: `temp-user-${Date.now()}`,
          role: 'USER',
          content: message,
          messageType: 'TEXT',
          actionPayload: null,
          createdAt: new Date().toISOString(),
        };

        set({ messages: [...messages, userMsg], isStreaming: true, streamingContent: '' });
        streamChat(activeSessionId, message, pageContext, getToken(), set, get);
      },

      loadSessions: async () => {
        try {
          const res = await fetch(`${API_BASE}/sessions`, { headers: authHeaders() });
          if (!res.ok) return;
          const data = await res.json();
          // Backend returns {id, title, messageCount, lastActiveAt} — map to our shape
          const sessions: ChatSessionSummary[] = (Array.isArray(data) ? data : []).map((s: Record<string, unknown>) => ({
            sessionId: s['id'] as string,
            title: s['title'] as string | null,
            pageContext: null,
            messageCount: s['messageCount'] as number ?? 0,
            lastActiveAt: s['lastActiveAt'] as string ?? '',
            status: 'ACTIVE' as const,
          }));
          set({ sessions });
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
          const messages: ChatMessageDto[] = (Array.isArray(data) ? data : []).map((m: Record<string, unknown>) => ({
            messageId: m['id'] as string,
            role: m['role'] as ChatMessageDto['role'],
            content: m['content'] as string,
            messageType: m['messageType'] as ChatMessageDto['messageType'],
            actionPayload: m['actionPayload'] as Record<string, unknown> | null,
            createdAt: m['createdAt'] as string,
          }));
          set({ messages, isLoading: false });
        } catch (err) {
          console.error('[chatStore] loadMessages error:', err);
          set({ isLoading: false });
        }
      },

      loadNudges: async () => {
        try {
          const res = await fetch(`${API_BASE}/nudges/pending`, { headers: authHeaders() });
          if (!res.ok) return;
          const data = await res.json();
          const nudges: ProactiveNudgeDto[] = (Array.isArray(data) ? data : []).map((n: Record<string, unknown>) => ({
            nudgeId: n['id'] as string,
            triggerType: n['triggerType'] as string,
            message: n['message'] as string,
            actionUrl: n['actionUrl'] as string | null,
            delivered: true,
            opened: false,
            createdAt: n['createdAt'] as string,
          }));
          const unread = nudges.length;
          set({ nudges, unreadNudgeCount: unread });
        } catch (err) {
          console.error('[chatStore] loadNudges error:', err);
        }
      },

      markNudgeOpened: async (nudgeId: string) => {
        try {
          await fetch(`${API_BASE}/nudges/${nudgeId}/open`, { method: 'PUT', headers: authHeaders() });
          set((state) => ({
            nudges: state.nudges.filter((n) => n.nudgeId !== nudgeId),
            unreadNudgeCount: Math.max(0, state.unreadNudgeCount - 1),
          }));
        } catch (err) {
          console.error('[chatStore] markNudgeOpened error:', err);
        }
      },

      archiveSession: async (sessionId: string) => {
        try {
          await fetch(`${API_BASE}/sessions/${sessionId}`, { method: 'DELETE', headers: authHeaders() });
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
      partialize: (state) => ({
        activeSessionId: state.activeSessionId,
        isOpen: state.isOpen,
        unreadNudgeCount: state.unreadNudgeCount,
      }),
    }
  )
);

function streamChat(
  sessionId: string,
  message: string,
  pageContext: string,
  token: string,
  set: (partial: Partial<ChatState> | ((s: ChatState) => Partial<ChatState>)) => void,
  get: () => ChatState
) {
  let accumulated = '';

  fetch(`${API_BASE}/sessions/${sessionId}/stream`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
    },
    body: JSON.stringify({ message, pageContext }),
  })
    .then(async (response) => {
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
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
            const assistantMsg: ChatMessageDto = {
              messageId: `assistant-${Date.now()}`,
              role: 'ASSISTANT',
              content: accumulated,
              messageType: 'TEXT',
              actionPayload: null,
              createdAt: new Date().toISOString(),
            };
            set((state) => ({
              messages: [...state.messages, assistantMsg],
              isStreaming: false,
              streamingContent: '',
            }));
            get().loadSessions();
            return;
          }

          try {
            const parsed = JSON.parse(data) as { choices?: Array<{ delta?: { content?: string } }> };
            const tokenStr = parsed?.choices?.[0]?.delta?.content;
            if (tokenStr) {
              accumulated += tokenStr;
              set({ streamingContent: accumulated });
            }
          } catch {
            // Ignore partial chunk parse errors
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
