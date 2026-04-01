import React from 'react';

const TypingIndicator: React.FC = () => (
  <div className="flex justify-start">
    <div className="max-w-[85%]">
      <div className="flex items-center gap-1 mb-1">
        <div className="w-5 h-5 rounded-full bg-indigo-600 flex items-center justify-center text-white text-[10px] font-bold">N</div>
        <span className="text-[10px] text-gray-400">Nexus</span>
      </div>
      <div className="bg-gray-100 rounded-2xl rounded-bl-sm px-4 py-3 flex items-center gap-1.5">
        {[0, 1, 2].map((i) => (
          <span
            key={i}
            className="w-2 h-2 rounded-full bg-gray-400 inline-block"
            style={{ animation: `bounce 1.2s ease-in-out ${i * 0.2}s infinite` }}
          />
        ))}
      </div>
    </div>
  </div>
);

export default TypingIndicator;
