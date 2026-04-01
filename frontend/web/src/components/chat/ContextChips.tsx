import React from 'react';
import { useChatStore } from '../../store/chatStore';

const PAGE_CHIPS: Record<string, string[]> = {
  '/dashboard': ['My performance', 'Upcoming exams', 'Study plan'],
  '/performance': ['Weak areas', 'Subject mastery', 'Improve score'],
  '/exams': ['Upcoming exams', 'Past results', 'Enroll now'],
  '/study-plan': ["Today's tasks", 'Create plan', 'Progress'],
  '/fees': ['Fee dues', 'Payment history'],
  '/doubts': ['Solve a doubt', 'Pending doubts'],
};

interface Props {
  pageContext: string;
}

const ContextChips: React.FC<Props> = ({ pageContext }) => {
  const { setInputText, isStreaming } = useChatStore();
  const chips = PAGE_CHIPS[pageContext] ?? PAGE_CHIPS['/dashboard'];

  if (!chips?.length) return null;

  return (
    <div className="px-3 pb-1 flex gap-1.5 overflow-x-auto scrollbar-hide flex-shrink-0">
      {chips.map((chip) => (
        <button
          key={chip}
          onClick={() => setInputText(chip)}
          disabled={isStreaming}
          className="text-xs bg-indigo-50 text-indigo-600 border border-indigo-100 rounded-full px-2.5 py-1
                     whitespace-nowrap hover:bg-indigo-100 transition-colors flex-shrink-0 disabled:opacity-40"
        >
          {chip}
        </button>
      ))}
    </div>
  );
};

export default ContextChips;
