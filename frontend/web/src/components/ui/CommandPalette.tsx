import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Search, X, HelpCircle, Settings } from 'lucide-react';
import { cn } from '../../lib/utils';
import { getNavItems, NavItem } from '../../lib/navConfig';

interface CommandItem {
  label: string;
  description?: string;
  path: string;
  Icon: React.ElementType;
  keywords: string[];
}

// ─── Extras per role (items not in the main sidebar nav) ─────────────────────

const STUDENT_EXTRAS: CommandItem[] = [
  { label: 'Doubts',   description: 'Ask doubts via AI',    path: '/ai-mentor/doubts', Icon: HelpCircle, keywords: ['question', 'help', 'doubt', 'ask'] },
  { label: 'Settings', description: 'Account preferences', path: '/settings',         Icon: Settings,   keywords: ['profile', 'account', 'password', 'theme'] },
];

const TEACHER_EXTRAS: CommandItem[] = [
  { label: 'Settings', description: 'Account preferences',   path: '/settings',         Icon: Settings,   keywords: ['profile', 'account', 'password', 'theme'] },
];

const ADMIN_EXTRAS: CommandItem[] = [
  { label: 'Settings', description: 'Account preferences', path: '/settings', Icon: Settings, keywords: ['profile', 'account', 'password', 'theme'] },
];

// ─── Auto-derive CommandItem from NavItem ─────────────────────────────────────

function navItemToCommand(item: NavItem): CommandItem {
  return {
    label: item.label,
    path: item.to,
    Icon: item.icon,
    keywords: item.label.toLowerCase().split(/[\s\/\-]+/).filter(Boolean),
  };
}

function getCommands(role?: string): CommandItem[] {
  const navItems = getNavItems(role);
  const fromNav = navItems.map(navItemToCommand);
  if (role === 'PARENT')   return fromNav;
  if (role === 'TEACHER')  return [...fromNav, ...TEACHER_EXTRAS];
  if (role === 'CENTER_ADMIN' || role === 'INSTITUTION_ADMIN' || role === 'SUPER_ADMIN') return [...fromNav, ...ADMIN_EXTRAS];
  return [...fromNav, ...STUDENT_EXTRAS];
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
