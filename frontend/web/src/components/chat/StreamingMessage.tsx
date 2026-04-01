import React from 'react';

interface Props {
  content: string;
}

const StreamingMessage: React.FC<Props> = ({ content }) => (
  <div className="flex justify-start">
    <div className="max-w-[85%]">
      <div className="flex items-center gap-1 mb-1">
        <div className="w-5 h-5 rounded-full bg-indigo-600 flex items-center justify-center text-white text-[10px] font-bold">N</div>
        <span className="text-[10px] text-gray-400">Nexus</span>
      </div>
      <div className="bg-gray-100 text-gray-800 rounded-2xl rounded-bl-sm px-3 py-2 text-sm leading-relaxed">
        <p className="whitespace-pre-wrap break-words">{content}</p>
        <span className="inline-block w-0.5 h-4 bg-indigo-500 ml-0.5 animate-pulse align-middle" />
      </div>
    </div>
  </div>
);

export default StreamingMessage;
