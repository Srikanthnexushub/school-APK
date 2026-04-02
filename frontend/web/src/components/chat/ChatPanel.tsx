import React, { useEffect, useRef, useCallback } from 'react';
import { Send, RotateCcw, Loader2 } from 'lucide-react';
import { useChatStore } from '../../store/chatStore';
import ChatMessageBubble from './ChatMessageBubble';
import StreamingMessage from './StreamingMessage';
import TypingIndicator from './TypingIndicator';
import ContextChips from './ContextChips';

interface Props {
  pageContext: string;
}

const ChatPanel: React.FC<Props> = ({ pageContext }) => {
  const {
    messages, streamingContent, isStreaming, isLoading,
    inputText, setInputText, sendMessage,
    loadSessions, startSession,
  } = useChatStore();

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, streamingContent]);

  useEffect(() => {
    loadSessions();
  }, [loadSessions]);

  const handleSend = useCallback(() => {
    const text = inputText.trim();
    if (!text || isStreaming || isLoading) return;
    sendMessage(text, pageContext);
    setInputText('');
  }, [inputText, isStreaming, isLoading, sendMessage, setInputText, pageContext]);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleNewConversation = () => {
    const ctx = pageContext.replace('/dashboard/', '').replace(/^\//, '') || 'dashboard';
    startSession(ctx);
  };

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 rounded-t-2xl flex-shrink-0"
           style={{ background: 'linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%)' }}>
        <div className="flex items-center gap-2">
          <div className="w-9 h-9 rounded-full flex items-center justify-center text-sm font-extrabold shadow-lg"
               style={{ background: 'linear-gradient(135deg, #00f5ff 0%, #bf5af2 50%, #ff375f 100%)',
                        boxShadow: '0 0 12px rgba(0,245,255,0.6), 0 0 24px rgba(191,90,242,0.4)' }}>
            <span style={{ color: '#fff', textShadow: '0 0 6px rgba(255,255,255,0.9)' }}>N</span>
          </div>
          <div>
            <p className="font-bold text-sm leading-tight" style={{ color: '#ffffff', textShadow: '0 0 8px rgba(255,255,255,0.3)' }}>Nexus AI</p>
            <p className="text-xs leading-tight" style={{ color: '#c4b5fd' }}>
              {isStreaming ? '● typing...' : 'AI Study Partner'}
            </p>
          </div>
        </div>
        <button
          onClick={handleNewConversation}
          title="New conversation"
          className="text-indigo-200 hover:text-white transition-colors"
          aria-label="New conversation"
        >
          <RotateCcw size={16} />
        </button>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-3 py-3 space-y-2 min-h-0">
        {isLoading && messages.length === 0 && (
          <div className="flex justify-center items-center h-20">
            <Loader2 size={20} className="animate-spin text-indigo-400" />
          </div>
        )}
        {messages.map((msg) => (
          <ChatMessageBubble key={msg.messageId} message={msg} />
        ))}
        {isStreaming && streamingContent === '' && <TypingIndicator />}
        {isStreaming && streamingContent !== '' && <StreamingMessage content={streamingContent} />}
        <div ref={messagesEndRef} />
      </div>

      {/* Context chips */}
      <ContextChips pageContext={pageContext} />

      {/* Input */}
      <div className="px-3 pb-3 pt-1 flex-shrink-0">
        <div className="flex items-end gap-2 bg-gray-50 rounded-xl border border-gray-200 px-3 py-2">
          <textarea
            ref={inputRef}
            rows={1}
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ask Nexus anything..."
            disabled={isStreaming || isLoading}
            className="flex-1 bg-transparent resize-none outline-none text-sm text-gray-800
                       placeholder-gray-400 max-h-24 min-h-[20px] overflow-y-auto disabled:opacity-50"
            onInput={(e) => {
              const t = e.currentTarget;
              t.style.height = 'auto';
              t.style.height = Math.min(t.scrollHeight, 96) + 'px';
            }}
          />
          <button
            onClick={handleSend}
            disabled={!inputText.trim() || isStreaming || isLoading}
            className="w-8 h-8 rounded-lg bg-indigo-600 text-white flex items-center justify-center
                       hover:bg-indigo-700 disabled:opacity-40 disabled:cursor-not-allowed transition-colors flex-shrink-0"
            aria-label="Send message"
          >
            {isStreaming ? <Loader2 size={14} className="animate-spin" /> : <Send size={14} />}
          </button>
        </div>
        <p className="text-center text-[10px] text-gray-300 mt-1">Nexus AI · Powered by NexusEd</p>
      </div>
    </div>
  );
};

export default ChatPanel;
