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

export interface ActionCommand {
  action: string;
  params: Record<string, unknown>;
}
