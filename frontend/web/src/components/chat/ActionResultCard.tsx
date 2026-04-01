import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, BookOpen, BarChart2, DollarSign, AlertTriangle } from 'lucide-react';
import type { ActionCommand } from '../../types/chat';

interface Props {
  action: ActionCommand;
  onHandled: () => void;
}

const ACTION_CONFIG: Record<string, {
  label: string;
  icon: React.ReactNode;
  color: string;
  route?: (p: Record<string, unknown>) => string;
}> = {
  NAVIGATE: {
    label: 'Take me there',
    icon: <ArrowRight size={14} />,
    color: 'bg-indigo-50 border-indigo-200 text-indigo-700',
    route: (p) => (p['path'] as string) || '/',
  },
  SHOW_WEAK_AREAS: {
    label: 'View Weak Areas',
    icon: <AlertTriangle size={14} />,
    color: 'bg-orange-50 border-orange-200 text-orange-700',
    route: () => '/performance',
  },
  CREATE_STUDY_PLAN: {
    label: 'Create Study Plan',
    icon: <BookOpen size={14} />,
    color: 'bg-green-50 border-green-200 text-green-700',
    route: () => '/study-plan',
  },
  SHOW_FEES: {
    label: 'View Fees',
    icon: <DollarSign size={14} />,
    color: 'bg-blue-50 border-blue-200 text-blue-700',
    route: () => '/fees',
  },
  ENROLL_EXAM: {
    label: 'Enroll in Exam',
    icon: <BarChart2 size={14} />,
    color: 'bg-purple-50 border-purple-200 text-purple-700',
    route: () => '/exams',
  },
};

const ActionResultCard: React.FC<Props> = ({ action, onHandled }) => {
  const navigate = useNavigate();
  const config = ACTION_CONFIG[action.action];

  if (!config) return null;

  const handleClick = () => {
    if (config.route) navigate(config.route(action.params));
    onHandled();
  };

  return (
    <div
      className={`mt-1 px-3 py-2 rounded-xl border text-xs font-medium flex items-center justify-between cursor-pointer hover:opacity-80 transition-opacity ${config.color}`}
      onClick={handleClick}
    >
      <span>{config.label}</span>
      <span>{config.icon}</span>
    </div>
  );
};

export default ActionResultCard;
