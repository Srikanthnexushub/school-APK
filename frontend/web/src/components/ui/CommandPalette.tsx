import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  Search, X,
  LayoutDashboard, Bot, ClipboardList, BarChart3, Target, Brain,
  Users, Calendar, Settings, GraduationCap, BookOpen, Briefcase,
  UserCheck, CreditCard, Megaphone, HelpCircle, BookCheck, Library,
  Award, Building2, Upload, UserCog, Wallet, CalendarCheck,
  FlaskConical, ClipboardCheck, Beaker,
} from 'lucide-react';
import { cn } from '../../lib/utils';

interface CommandItem {
  label: string;
  description?: string;
  path: string;
  Icon: React.ElementType;
  keywords: string[];
}

// ─── Student ─────────────────────────────────────────────────────────────────
const studentCommands: CommandItem[] = [
  { label: 'Dashboard',      description: 'Your study overview',            path: '/dashboard',          Icon: LayoutDashboard, keywords: ['home', 'overview'] },
  { label: 'AI Mentor',      description: 'Chat with your AI tutor',        path: '/ai-mentor',          Icon: Bot,             keywords: ['chat', 'tutor', 'ai'] },
  { label: 'Doubts',         description: 'Ask doubts via AI',              path: '/ai-mentor/doubts',   Icon: HelpCircle,      keywords: ['question', 'help', 'doubt', 'ask'] },
  { label: 'Academic Plan',  description: 'AI curriculum & revision plans', path: '/academic-plan',      Icon: GraduationCap,   keywords: ['curriculum', 'revision', 'plan', 'study'] },
  { label: 'Assessments',    description: 'Take tests and exams',           path: '/assessments',        Icon: ClipboardList,   keywords: ['test', 'exam', 'quiz'] },
  { label: 'Assignments',    description: 'Your assignments & homework',    path: '/assignments',        Icon: ClipboardCheck,  keywords: ['homework', 'tasks', 'submission'] },
  { label: 'Performance',    description: 'View your analytics',            path: '/performance',        Icon: BarChart3,       keywords: ['analytics', 'scores', 'weak'] },
  { label: 'Career Oracle',  description: 'AI career guidance',             path: '/career',             Icon: Target,          keywords: ['career', 'guidance', 'future'] },
  { label: 'Psychometric',   description: 'Personality insights',           path: '/psychometric',       Icon: Brain,           keywords: ['personality', 'big five', 'riasec'] },
  { label: 'Mentors',        description: 'Connect with mentors',           path: '/mentors',            Icon: Users,           keywords: ['mentor', 'session', 'book'] },
  { label: 'Exam Tracker',   description: 'Track upcoming exams',           path: '/exam-tracker',       Icon: Calendar,        keywords: ['schedule', 'countdown', 'mock'] },
  { label: 'Question Bank',  description: 'Practice questions by topic',    path: '/question-bank',      Icon: BookOpen,        keywords: ['practice', 'mcq', 'questions', 'bank'] },
  { label: 'Library',        description: 'Study materials & resources',    path: '/library',            Icon: Library,         keywords: ['books', 'notes', 'resources'] },
  { label: 'AI Lab',         description: 'Hands-on AI projects',           path: '/lab',                Icon: Beaker,          keywords: ['project', 'lab', 'practical', 'experiment'] },
  { label: 'Attendance',     description: 'Your attendance record',         path: '/attendance',         Icon: CalendarCheck,   keywords: ['present', 'absent', 'record'] },
  { label: 'Fees',           description: 'Fee payments & history',         path: '/fees',               Icon: CreditCard,      keywords: ['payment', 'due', 'invoice'] },
  { label: 'Announcements',  description: 'Centre announcements',           path: '/announcements',      Icon: Megaphone,       keywords: ['notice', 'update', 'news'] },
  { label: 'Jobs',           description: 'Job & internship board',         path: '/jobs',               Icon: Briefcase,       keywords: ['internship', 'placement'] },
  { label: 'Settings',       description: 'Account preferences',            path: '/settings',           Icon: Settings,        keywords: ['profile', 'account', 'password'] },
];

// ─── Parent ──────────────────────────────────────────────────────────────────
const parentCommands: CommandItem[] = [
  { label: 'Overview',       description: 'Parent dashboard',               path: '/parent',                    Icon: LayoutDashboard, keywords: ['home', 'overview'] },
  { label: 'My Children',    description: 'Linked student profiles',        path: '/parent/children',           Icon: Users,           keywords: ['student', 'child', 'link'] },
  { label: 'Academic Plan',  description: 'AI curriculum & revision plans', path: '/academic-plan',             Icon: GraduationCap,   keywords: ['curriculum', 'revision', 'plan'] },
  { label: 'Fees',           description: 'Fee payments & history',         path: '/parent/fees',               Icon: CreditCard,      keywords: ['payment', 'due', 'invoice'] },
  { label: 'Psychometric',   description: 'Personality insights',           path: '/parent/psychometric',       Icon: Brain,           keywords: ['personality', 'big five', 'riasec'] },
  { label: 'Library',        description: 'Study materials & resources',    path: '/parent/library',            Icon: Library,         keywords: ['books', 'notes', 'resources'] },
  { label: 'Question Bank',  description: 'Practice questions by topic',    path: '/parent/question-bank',      Icon: BookOpen,        keywords: ['practice', 'mcq', 'questions', 'bank'] },
  { label: 'Copilot',        description: 'AI parent assistant',            path: '/parent/copilot',            Icon: Bot,             keywords: ['ai', 'chat', 'assistant'] },
  { label: 'Attendance',     description: "Child's attendance record",      path: '/parent/attendance',         Icon: CalendarCheck,   keywords: ['present', 'absent', 'record'] },
  { label: 'Announcements',  description: 'Centre announcements',           path: '/parent/announcements',      Icon: Megaphone,       keywords: ['notice', 'update', 'news'] },
  { label: 'Profile',        description: 'Your profile & settings',        path: '/parent/profile',            Icon: Settings,        keywords: ['account', 'details'] },
];

// ─── Teacher (Mentor Portal) ──────────────────────────────────────────────────
const teacherCommands: CommandItem[] = [
  { label: 'Dashboard',      description: 'Mentor overview',                path: '/mentor-portal',                   Icon: LayoutDashboard, keywords: ['home', 'overview'] },
  { label: 'Sessions',       description: 'Your mentoring sessions',        path: '/mentor-portal/sessions',          Icon: Calendar,        keywords: ['booking', 'schedule', 'meet'] },
  { label: 'AI Insights',    description: 'AI-powered student insights',    path: '/mentor-portal/insights',          Icon: Brain,           keywords: ['analytics', 'ai', 'student'] },
  { label: 'Academic Plan',  description: 'AI curriculum & revision plans', path: '/academic-plan',                   Icon: GraduationCap,   keywords: ['curriculum', 'revision', 'plan'] },
  { label: 'Exams',          description: 'Manage exams',                   path: '/mentor-portal/exams',             Icon: ClipboardList,   keywords: ['test', 'assessment', 'create'] },
  { label: 'Assignments',    description: 'Manage assignments',             path: '/mentor-portal/assignments',       Icon: BookCheck,       keywords: ['homework', 'tasks'] },
  { label: 'My Performance', description: 'Your performance metrics',       path: '/mentor-portal/performance',       Icon: Award,           keywords: ['score', 'analytics', 'rating'] },
  { label: 'Library',        description: 'Study materials & resources',    path: '/mentor-portal/library',           Icon: Library,         keywords: ['books', 'notes', 'resources'] },
  { label: 'Question Bank',  description: 'Practice questions by topic',    path: '/mentor-portal/question-bank',     Icon: BookOpen,        keywords: ['practice', 'mcq', 'questions', 'bank'] },
  { label: 'Attendance',     description: 'Attendance records',             path: '/mentor-portal/attendance',        Icon: CalendarCheck,   keywords: ['present', 'absent', 'record'] },
  { label: 'Announcements',  description: 'Centre announcements',           path: '/mentor-portal/announcements',     Icon: Megaphone,       keywords: ['notice', 'update', 'news'] },
  { label: 'Settings',       description: 'Account preferences',            path: '/settings',                        Icon: Settings,        keywords: ['profile', 'account', 'password'] },
];

// ─── Admin (CENTER_ADMIN / INSTITUTION_ADMIN / SUPER_ADMIN) ──────────────────
const adminCommands: CommandItem[] = [
  { label: 'Overview',           description: 'Admin dashboard',              path: '/admin?tab=overview',        Icon: LayoutDashboard, keywords: ['home', 'summary'] },
  { label: 'Centers',            description: 'Manage centres',               path: '/admin?tab=centers',         Icon: Building2,       keywords: ['branch', 'centre', 'location'] },
  { label: 'Batches',            description: 'Manage student batches',       path: '/admin?tab=batches',         Icon: Users,           keywords: ['group', 'class', 'section'] },
  { label: 'Assessments',        description: 'Manage assessments & exams',   path: '/admin?tab=assessments',     Icon: ClipboardList,   keywords: ['test', 'exam', 'create'] },
  { label: 'Bulk Import',        description: 'Import teachers via CSV',      path: '/admin?tab=teacher-import',  Icon: Upload,          keywords: ['csv', 'upload', 'import'] },
  { label: 'Teacher Approvals',  description: 'Approve pending teachers',     path: '/admin?tab=teacher-pending', Icon: UserCheck,       keywords: ['approve', 'verify', 'pending'] },
  { label: 'Staff',              description: 'Manage staff members',         path: '/admin?tab=staff',           Icon: UserCog,         keywords: ['employee', 'team'] },
  { label: 'Jobs',               description: 'Job board management',         path: '/admin?tab=jobs',            Icon: Briefcase,       keywords: ['internship', 'placement'] },
  { label: 'Assignments',        description: 'Manage assignments',           path: '/admin?tab=assignments',     Icon: BookCheck,       keywords: ['homework', 'tasks'] },
  { label: 'Psychometric',       description: 'Psychometric tests',           path: '/admin?tab=psychometric',    Icon: Brain,           keywords: ['personality', 'test'] },
  { label: 'Library',            description: 'Study materials & resources',  path: '/admin?tab=library',         Icon: Library,         keywords: ['books', 'notes', 'resources'] },
  { label: 'Question Bank',      description: 'Practice questions by topic',  path: '/admin/question-bank',       Icon: BookOpen,        keywords: ['practice', 'mcq', 'questions', 'bank'] },
  { label: 'Attendance',         description: 'Attendance records',           path: '/admin/attendance',          Icon: CalendarCheck,   keywords: ['present', 'absent', 'record'] },
  { label: 'Announcements',      description: 'Publish announcements',        path: '/admin/announcements',       Icon: Megaphone,       keywords: ['notice', 'update', 'news'] },
  { label: 'Accounts',           description: 'Fees & billing',               path: '/admin?tab=accounts',        Icon: Wallet,          keywords: ['fees', 'payment', 'billing'] },
  { label: 'Settings',           description: 'Account preferences',          path: '/settings',                  Icon: Settings,        keywords: ['profile', 'account', 'password'] },
];

function getCommands(role?: string): CommandItem[] {
  if (role === 'PARENT') return parentCommands;
  if (role === 'TEACHER') return teacherCommands;
  if (role === 'CENTER_ADMIN' || role === 'INSTITUTION_ADMIN' || role === 'SUPER_ADMIN') return adminCommands;
  return studentCommands;
}

interface Props {
  onClose: () => void;
  role?: string;
}

export default function CommandPalette({ onClose, role }: Props) {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);

  const commands = getCommands(role);

  const filtered = query.trim()
    ? commands.filter((c) => {
        const q = query.toLowerCase();
        return c.label.toLowerCase().includes(q) || c.keywords.some((k) => k.includes(q));
      })
    : commands;

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  useEffect(() => {
    setSelectedIndex(0);
  }, [query]);

  function handleSelect(item: CommandItem) {
    navigate(item.path);
    onClose();
  }

  function handleKeyDown(e: React.KeyboardEvent) {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setSelectedIndex((i) => Math.min(i + 1, filtered.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setSelectedIndex((i) => Math.max(i - 1, 0));
    } else if (e.key === 'Enter' && filtered[selectedIndex]) {
      handleSelect(filtered[selectedIndex]);
    } else if (e.key === 'Escape') {
      onClose();
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center pt-[15vh] px-4">
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        onClick={onClose}
        className="absolute inset-0 bg-black/60 backdrop-blur-sm"
      />
      <motion.div
        initial={{ opacity: 0, y: -20, scale: 0.96 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        exit={{ opacity: 0, y: -20, scale: 0.96 }}
        transition={{ duration: 0.2 }}
        className="relative w-full max-w-xl bg-surface-50 border border-white/10 rounded-2xl shadow-2xl overflow-hidden"
        onKeyDown={handleKeyDown}
      >
        {/* Search input */}
        <div className="flex items-center gap-3 px-4 py-3.5 border-b border-white/5">
          <Search className="w-4 h-4 text-white/30 flex-shrink-0" />
          <input
            ref={inputRef}
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search pages, actions..."
            className="flex-1 bg-transparent text-white placeholder:text-white/20 text-sm focus:outline-none"
          />
          <button onClick={onClose} className="p-1 rounded-lg hover:bg-white/10 text-white/30 hover:text-white/70 transition-colors">
            <X className="w-3.5 h-3.5" />
          </button>
        </div>

        {/* Results */}
        <div className="py-2 max-h-80 overflow-y-auto">
          {filtered.length === 0 ? (
            <div className="px-4 py-8 text-center text-white/30 text-sm">No results found</div>
          ) : (
            filtered.map((item, i) => (
              <button
                key={item.path}
                onClick={() => handleSelect(item)}
                onMouseEnter={() => setSelectedIndex(i)}
                className={cn(
                  'w-full flex items-center gap-3 px-4 py-2.5 text-left transition-colors',
                  i === selectedIndex ? 'bg-brand-500/10' : 'hover:bg-white/5'
                )}
              >
                <div className={cn('p-1.5 rounded-lg flex-shrink-0', i === selectedIndex ? 'bg-brand-600/30' : 'bg-surface-200/60')}>
                  <item.Icon className={cn('w-4 h-4', i === selectedIndex ? 'text-brand-400' : 'text-white/40')} />
                </div>
                <div>
                  <div className={cn('text-sm font-medium', i === selectedIndex ? 'text-white' : 'text-white/70')}>{item.label}</div>
                  {item.description && <div className="text-xs text-white/30">{item.description}</div>}
                </div>
              </button>
            ))
          )}
        </div>

        {/* Footer */}
        <div className="px-4 py-2 border-t border-white/5 flex items-center gap-4 text-[10px] text-white/20">
          <span><kbd className="font-mono">↑↓</kbd> navigate</span>
          <span><kbd className="font-mono">↵</kbd> select</span>
          <span><kbd className="font-mono">esc</kbd> close</span>
        </div>
      </motion.div>
    </div>
  );
}
