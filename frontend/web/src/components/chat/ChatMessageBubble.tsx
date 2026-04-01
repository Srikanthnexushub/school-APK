import React, { useState } from 'react';
import { ChatMessageDto, ActionCommand } from '../../types/chat';
import ActionResultCard from './ActionResultCard';

interface Props {
  message: ChatMessageDto;
}

function stripActionJson(content: string): string {
  return content.replace(/\{"action":[^}]*(?:\}[^}]*)?\}/g, '').trim();
}

function extractActionCommand(content: string): ActionCommand | null {
  const match = content.match(/(\{"action":.*?\})/s);
  if (!match) return null;
  try {
    return JSON.parse(match[1]) as ActionCommand;
  } catch {
    return null;
  }
}

const ChatMessageBubble: React.FC<Props> = ({ message }) => {
  const isUser = message.role === 'USER';
  const [actionHandled, setActionHandled] = useState(false);

  const displayContent = isUser ? message.content : stripActionJson(message.content);
  const actionCommand = !isUser ? extractActionCommand(message.content) : null;

  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div className={`max-w-[85%] ${isUser ? 'order-2' : 'order-1'}`}>
        {!isUser && (
          <div className="flex items-center gap-1 mb-1">
            <div className="w-5 h-5 rounded-full bg-indigo-600 flex items-center justify-center text-white text-[10px] font-bold">N</div>
            <span className="text-[10px] text-gray-400">Nexus</span>
          </div>
        )}

        <div className={`rounded-2xl px-3 py-2 text-sm leading-relaxed ${
          isUser
            ? 'bg-indigo-600 text-white rounded-br-sm'
            : 'bg-gray-100 text-gray-800 rounded-bl-sm'
        }`}>
          {isUser ? (
            <p>{message.content}</p>
          ) : (
            <p className="whitespace-pre-wrap break-words text-sm leading-relaxed">{displayContent}</p>
          )}
        </div>

        {actionCommand && !actionHandled && (
          <ActionResultCard
            action={actionCommand}
            onHandled={() => setActionHandled(true)}
          />
        )}

        <p className={`text-[10px] text-gray-300 mt-0.5 ${isUser ? 'text-right' : 'text-left'}`}>
          {new Date(message.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
        </p>
      </div>
    </div>
  );
};

export default ChatMessageBubble;
