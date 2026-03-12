import { useState, useEffect, useRef } from 'react';
import { Outlet, NavLink, useNavigate, useLocation } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  LayoutDashboard, Bot, ClipboardList, BarChart3, Target, Brain,
  Users, Calendar, Settings, LogOut, BookOpen, Menu, X, ChevronLeft,
  Bell, Search, ChevronRight, BookOpenCheck, Library, Award,
  CreditCard,
} from 'lucide-react';
import { toast } from 'sonner';
import { useAuthStore } from '../../stores/authStore';
import { Avatar } from '../ui/Avatar';
import { cn } from '../../lib/utils';
import CommandPalette from '../ui/CommandPalette';
import NotificationPanel from '../ui/NotificationPanel';
import { useNotifications } from '../../hooks/useNotifications';
import { useQuery } from '@tanstack/react-query';
import api from '../../lib/api';

interface NavItem {
  icon: React.ElementType;
  label: string;
  to: string;
  badge?: number;
}

const studentNav: NavItem[] = [
  { icon: LayoutDashboard, label: 'Dashboard',    to: '/dashboard' },
  { icon: Bot,             label: 'AI Mentor',    to: '/ai-mentor', badge: 3 },
  { icon: ClipboardList,   label: 'Assessments',  to: '/assessments' },
  { icon: BarChart3,       label: 'Performance',  to: '/performance' },
  { icon: Target,          label: 'Career Oracle',to: '/career' },
  { icon: Brain,           label: 'Psychometric', to: '/psychometric' },
  { icon: Users,           label: 'Mentors',      to: '/mentors' },
  { icon: Calendar,        label: 'Exam Tracker', to: '/exam-tracker' },
  { icon: Library,         label: 'Library',      to: '/library' },
];

const adminNav: NavItem[] = [
  { icon: LayoutDashboard,  label: 'Overview',    to: '/admin?tab=overview' },
  { icon: Users,            label: 'Centers',     to: '/admin?tab=centers' },
  { icon: BookOpenCheck,    label: 'Batches',     to: '/admin?tab=batches' },
  { icon: ClipboardList,    label: 'Assessments', to: '/admin?tab=assessments' },
];

const parentNav: NavItem[] = [
  { icon: LayoutDashboard, label: 'Overview',      to: '/parent' },
  { icon: Users,           label: 'My Children',   to: '/parent/children' },
  { icon: CreditCard,      label: 'Fees',          to: '/parent/fees' },
  { icon: Brain,           label: 'Psychometric',  to: '/parent/psychometric' },
  { icon: Library,         label: 'Library',       to: '/parent/library' },
  { icon: BookOpen,        label: 'Question Bank', to: '/parent/question-bank' },
  { icon: Bot,             label: 'Copilot',       to: '/parent/copilot' },
  { icon: Settings,        label: 'Profile',       to: '/parent/profile' },
];

const mentorNav: NavItem[] = [
  { icon: LayoutDashboard, label: 'Dashboard',    to: '/mentor-portal' },
  { icon: Calendar,        label: 'Sessions',     to: '/mentor-portal/sessions' },
  { icon: Brain,           label: 'AI Insights',  to: '/mentor-portal/insights' },
  { icon: ClipboardList,   label: 'Exams',        to: '/mentor-portal/exams' },
  { icon: Award,           label: 'My Performance',to: '/mentor-portal/performance' },
  { icon: Library,         label: 'Library',      to: '/mentor-portal/library' },
];

function getNavItems(role?: string): NavItem[] {
  if (role === 'CENTER_ADMIN' || role === 'SUPER_ADMIN') return adminNav;
  if (role === 'PARENT') return parentNav;
  if (role === 'TEACHER') return mentorNav;
  return studentNav;
}

const roleBadgeColors: Record<string, string> = {
  STUDENT:     'bg-brand-500/20 text-brand-400',
  CENTER_ADMIN:'bg-red-500/20 text-red-400',
  SUPER_ADMIN: 'bg-red-500/20 text-red-400',
  PARENT:      'bg-emerald-500/20 text-emerald-400',
  TEACHER:     'bg-amber-500/20 text-amber-400',
  GUEST:       'bg-white/10 text-white/40',
};

function RoleBadge({ role }: { role: string }) {
  return (
    <span className={cn('badge text-[10px]', roleBadgeColors[role] ?? 'bg-white/10 text-white/40')}>
      {role}
    </span>
  );
}

function CollapseTooltip({ label, children }: { label: string; children: React.ReactNode }) {
  const [visible, setVisible] = useState(false);
  return (
    <div
      className="relative"
      onMouseEnter={() => setVisible(true)}
      onMouseLeave={() => setVisible(false)}
    >
      {children}
      <AnimatePresence>
        {visible && (
          <motion.div
            initial={{ opacity: 0, x: -4 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -4 }}
            transition={{ duration: 0.15 }}
            className="absolute left-full top-1/2 -translate-y-1/2 ml-3 z-50 bg-surface-100 border border-white/10 text-white text-xs font-medium px-2.5 py-1.5 rounded-lg whitespace-nowrap shadow-xl pointer-events-none"
          >
            {label}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

function AvatarDropdown({ name, avatarUrl }: { name: string; avatarUrl?: string }) {
  const [open, setOpen] = useState(false);
  const { logout } = useAuthStore();
  const navigate = useNavigate();
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handler(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  function handleLogout() {
    logout();
    toast.success('Signed out successfully');
    navigate('/login');
    setOpen(false);
  }

  return (
    <div className="relative" ref={ref}>
      <button
        onClick={() => setOpen(!open)}
        className="flex items-center gap-2 p-1.5 rounded-xl hover:bg-white/5 transition-colors"
      >
        <Avatar name={name} size="sm" imageUrl={avatarUrl} />
        <span className="hidden md:block text-sm font-medium text-white/70 hover:text-white transition-colors max-w-[120px] truncate">
          {name}
        </span>
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, y: -8, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -8, scale: 0.95 }}
            transition={{ duration: 0.15 }}
            className="absolute right-0 top-full mt-2 w-48 bg-surface-50 border border-white/10 rounded-xl shadow-2xl z-50 overflow-hidden"
          >
            <div className="p-1">
              <button
                onClick={() => { navigate('/settings'); setOpen(false); }}
                className="w-full text-left px-3 py-2 text-sm text-white/70 hover:text-white hover:bg-white/5 rounded-lg transition-colors flex items-center gap-2"
              >
                <Settings className="w-4 h-4" /> Profile
              </button>
              <button
                onClick={() => { navigate('/settings'); setOpen(false); }}
                className="w-full text-left px-3 py-2 text-sm text-white/70 hover:text-white hover:bg-white/5 rounded-lg transition-colors flex items-center gap-2"
              >
                <Settings className="w-4 h-4" /> Settings
              </button>
              <div className="border-t border-white/5 my-1" />
              <button
                onClick={handleLogout}
                className="w-full text-left px-3 py-2 text-sm text-red-400/70 hover:text-red-400 hover:bg-red-500/10 rounded-lg transition-colors flex items-center gap-2"
              >
                <LogOut className="w-4 h-4" /> Sign out
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

function SidebarContent({
  collapsed,
  setCollapsed,
  onNavigate,
}: {
  collapsed: boolean;
  setCollapsed: (v: boolean) => void;
  onNavigate?: () => void;
}) {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();
  const location = useLocation();
  const navItems = getNavItems(user?.role);

  function handleLogout() {
    logout();
    toast.success('Signed out successfully');
    navigate('/login');
    if (onNavigate) onNavigate();
  }

  // For items with query params (e.g. /admin?tab=centers), match both pathname + search
  function isTabActive(to: string): boolean | null {
    if (!to.includes('?')) return null; // let NavLink handle non-tab items
    const [path, qs] = to.split('?');
    const toParams = new URLSearchParams(qs);
    const locParams = new URLSearchParams(location.search);
    if (location.pathname !== path) return false;
    for (const [k, v] of toParams.entries()) {
      if (locParams.get(k) !== v) return false;
    }
    return true;
  }

  const endPaths = ['/dashboard', '/parent', '/mentor-portal'];

  return (
    <div className="flex flex-col h-full">
      {/* Logo + collapse */}
      <div className={cn('flex items-center p-4 border-b border-white/5 h-14 flex-shrink-0', collapsed ? 'justify-center' : 'justify-between')}>
        <div className="flex items-center gap-2.5 min-w-0">
          <div className="p-1.5 rounded-lg bg-brand-600/20 border border-brand-500/30 flex-shrink-0">
            <BookOpen className="w-4 h-4 text-brand-400" />
          </div>
          {!collapsed && (
            <span className="font-bold text-white truncate">NexusEd</span>
          )}
        </div>
        {!collapsed && (
          <button
            onClick={() => setCollapsed(true)}
            className="hidden lg:flex p-1.5 rounded-lg hover:bg-white/5 text-white/30 hover:text-white/70 transition-all flex-shrink-0"
            aria-label="Collapse sidebar"
          >
            <ChevronLeft className="w-4 h-4" />
          </button>
        )}
        {collapsed && (
          <button
            onClick={() => setCollapsed(false)}
            className="hidden lg:flex p-1.5 rounded-lg hover:bg-white/5 text-white/30 hover:text-white/70 transition-all absolute bottom-[76px] left-1/2 -translate-x-1/2"
            aria-label="Expand sidebar"
          >
            <ChevronRight className="w-3 h-3" />
          </button>
        )}
      </div>

      {/* User card */}
      {user && (
        <div className={cn('px-3 py-3 border-b border-white/5 flex-shrink-0', collapsed ? 'flex justify-center' : '')}>
          {collapsed ? (
            <CollapseTooltip label={user.name}>
              <Avatar name={user.name} size="sm" imageUrl={user.avatarUrl} />
            </CollapseTooltip>
          ) : (
            <div className="flex items-center gap-3">
              <Avatar name={user.name} size="sm" imageUrl={user.avatarUrl} />
              <div className="min-w-0 flex-1">
                <div className="text-sm font-semibold text-white truncate">{user.name}</div>
                <div className="mt-0.5">
                  <RoleBadge role={user.role} />
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Navigation */}
      <nav className="flex-1 p-3 space-y-0.5 overflow-y-auto">
        {navItems.map(({ icon: Icon, label, to, badge }) => {
          const tabActive = isTabActive(to);
          return collapsed ? (
            <CollapseTooltip key={to} label={label}>
              <NavLink
                to={to}
                end={endPaths.includes(to)}
                onClick={onNavigate}
                className={({ isActive }) => {
                  const active = tabActive ?? isActive;
                  return cn(
                    'flex items-center justify-center p-2.5 rounded-xl transition-all duration-200 relative',
                    active
                      ? 'bg-brand-500/10 text-white border-l-2 border-brand-500'
                      : 'text-white/50 hover:text-white hover:bg-white/5'
                  );
                }}
              >
                {({ isActive }) => {
                  const active = tabActive ?? isActive;
                  return (
                    <>
                      <Icon style={{ width: 18, height: 18 }} className={cn('flex-shrink-0', active ? 'text-brand-400' : '')} />
                      {badge !== undefined && badge > 0 && (
                        <span className="absolute top-1 right-1 w-2 h-2 bg-brand-500 rounded-full" />
                      )}
                    </>
                  );
                }}
              </NavLink>
            </CollapseTooltip>
          ) : (
            <NavLink
              key={to}
              to={to}
              end={endPaths.includes(to)}
              onClick={onNavigate}
              className={({ isActive }) => {
                const active = tabActive ?? isActive;
                return cn(
                  'flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 group relative',
                  active
                    ? 'bg-brand-500/10 text-white border-l-2 border-brand-500'
                    : 'text-white/50 hover:text-white hover:bg-white/5'
                );
              }}
            >
              {({ isActive }) => {
                const active = tabActive ?? isActive;
                return (
                  <>
                    <Icon
                      style={{ width: 18, height: 18 }}
                      className={cn('flex-shrink-0 transition-colors', active ? 'text-brand-400' : 'text-white/40 group-hover:text-white/70')}
                    />
                    <span className="flex-1">{label}</span>
                    {badge !== undefined && badge > 0 && (
                      <span className="bg-brand-600 text-white text-[10px] font-bold px-1.5 py-0.5 rounded-full min-w-[18px] text-center">
                        {badge}
                      </span>
                    )}
                  </>
                );
              }}
            </NavLink>
          );
        })}
      </nav>

      {/* Bottom actions */}
      <div className="p-3 border-t border-white/5 space-y-0.5 flex-shrink-0">
        {collapsed ? (
          <>
            <CollapseTooltip label="Settings">
              <NavLink
                to="/settings"
                className={({ isActive }) =>
                  cn('flex items-center justify-center p-2.5 rounded-xl transition-all duration-200', isActive ? 'bg-brand-500/10 text-brand-400' : 'text-white/50 hover:text-white hover:bg-white/5')
                }
              >
                <Settings style={{ width: 18, height: 18 }} />
              </NavLink>
            </CollapseTooltip>
            <CollapseTooltip label="Sign out">
              <button
                onClick={handleLogout}
                className="flex items-center justify-center p-2.5 rounded-xl w-full text-red-400/70 hover:text-red-400 hover:bg-red-500/10 transition-all duration-200"
              >
                <LogOut style={{ width: 18, height: 18 }} />
              </button>
            </CollapseTooltip>
          </>
        ) : (
          <>
            <NavLink
              to="/settings"
              className={({ isActive }) =>
                cn('flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-200', isActive ? 'bg-brand-500/10 text-brand-400' : 'text-white/50 hover:text-white hover:bg-white/5')
              }
            >
              <Settings style={{ width: 18, height: 18 }} className="flex-shrink-0" />
              Settings
            </NavLink>
            <button
              onClick={handleLogout}
              className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 w-full text-red-400/70 hover:text-red-400 hover:bg-red-500/10"
            >
              <LogOut style={{ width: 18, height: 18 }} className="flex-shrink-0" />
              Sign out
            </button>
          </>
        )}
      </div>
    </div>
  );
}

function ProfileRing({ pct, onClick }: { pct: number; onClick: () => void }) {
  const r = 13;
  const circ = 2 * Math.PI * r;
  const offset = circ * (1 - pct / 100);
  const color = pct === 100 ? '#10b981' : pct >= 67 ? '#f97316' : '#ef4444';
  return (
    <button
      onClick={onClick}
      title={`Profile ${pct}% complete`}
      className="relative flex items-center justify-center w-10 h-10 rounded-xl hover:bg-white/5 transition-colors"
    >
      <svg width="36" height="36" viewBox="0 0 36 36" className="-rotate-90">
        <circle cx="18" cy="18" r={r} fill="none" stroke="rgba(255,255,255,0.12)" strokeWidth="3" />
        <circle
          cx="18" cy="18" r={r}
          fill="none"
          stroke={color}
          strokeWidth="3"
          strokeLinecap="round"
          strokeDasharray={circ}
          strokeDashoffset={offset}
          style={{ transition: 'stroke-dashoffset 0.4s ease' }}
        />
      </svg>
      <span className="absolute text-[10px] font-extrabold leading-none text-white">{pct}</span>
    </button>
  );
}

export default function AppLayout() {
  const { user } = useAuthStore();
  const navigate = useNavigate();
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [commandOpen, setCommandOpen] = useState(false);
  const [notifOpen, setNotifOpen] = useState(false);
  const notifRef = useRef<HTMLDivElement>(null);
  const { notifications, unreadCount, isLoading, markRead, markAllRead } = useNotifications();

  const { data: studentProfile } = useQuery({
    queryKey: ['student-profile-me'],
    queryFn: () => api.get('/api/v1/students/me').then((r) => r.data),
    enabled: !!user && user.role === 'STUDENT',
    staleTime: 5 * 60 * 1000,
  });

  const { data: parentProfile } = useQuery({
    queryKey: ['parent-profile'],
    queryFn: () => api.get('/api/v1/parents/me').then((r) => r.data),
    enabled: !!user && user.role === 'PARENT',
    staleTime: 5 * 60 * 1000,
  });

  let profilePct = 0;
  if (user) {
    if (user.role === 'STUDENT' && studentProfile) {
      const f = [user.name, user.email, user.avatarUrl, studentProfile.phone, studentProfile.gender, studentProfile.dateOfBirth, studentProfile.city, studentProfile.stream];
      profilePct = Math.round(f.filter(Boolean).length / f.length * 100);
    } else if (user.role === 'PARENT' && parentProfile) {
      const f = [parentProfile.name, parentProfile.phone, parentProfile.email, parentProfile.relationshipType, parentProfile.address, parentProfile.city, parentProfile.state, parentProfile.pincode];
      profilePct = Math.round(f.filter(Boolean).length / f.length * 100);
    } else if (user.role === 'TEACHER' || user.role === 'CENTER_ADMIN' || user.role === 'SUPER_ADMIN') {
      const f = [!!user.name, !!user.email, !!user.avatarUrl];
      profilePct = Math.round(f.filter(Boolean).length / f.length * 100);
    }
  }
  const profilePath = user?.role === 'PARENT' ? '/parent/profile' : '/settings';

  // Close mobile on route change
  useEffect(() => { setMobileOpen(false); }, [location.pathname]);

  // ⌘K shortcut
  useEffect(() => {
    function handler(e: KeyboardEvent) {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        setCommandOpen(true);
      }
    }
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, []);

  // Close notif panel on outside click
  useEffect(() => {
    function handler(e: MouseEvent) {
      if (notifRef.current && !notifRef.current.contains(e.target as Node)) setNotifOpen(false);
    }
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  return (
    <div className="flex h-screen bg-surface overflow-hidden">
      {/* Desktop Sidebar */}
      <motion.aside
        animate={{ width: collapsed ? 64 : 240 }}
        transition={{ duration: 0.25, ease: 'easeInOut' }}
        className="hidden lg:flex flex-col bg-surface-50/40 border-r border-white/5 flex-shrink-0 overflow-hidden relative"
      >
        <SidebarContent collapsed={collapsed} setCollapsed={setCollapsed} />
      </motion.aside>

      {/* Mobile Sidebar overlay */}
      <AnimatePresence>
        {mobileOpen && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setMobileOpen(false)}
              className="lg:hidden fixed inset-0 z-40 bg-black/60 backdrop-blur-sm"
            />
            <motion.aside
              initial={{ x: -280 }}
              animate={{ x: 0 }}
              exit={{ x: -280 }}
              transition={{ duration: 0.25, ease: 'easeInOut' }}
              className="lg:hidden fixed left-0 top-0 bottom-0 z-50 w-64 bg-surface-50 border-r border-white/5 flex flex-col"
            >
              <button
                onClick={() => setMobileOpen(false)}
                className="absolute top-4 right-4 p-1.5 rounded-lg hover:bg-white/5 text-white/30 hover:text-white/70 z-10"
              >
                <X className="w-4 h-4" />
              </button>
              <SidebarContent
                collapsed={false}
                setCollapsed={() => {}}
                onNavigate={() => setMobileOpen(false)}
              />
            </motion.aside>
          </>
        )}
      </AnimatePresence>

      {/* Main content */}
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        {/* Topbar */}
        <header className="relative z-40 flex items-center justify-between px-4 lg:px-6 h-14 border-b border-white/5 bg-surface-50/20 backdrop-blur-sm flex-shrink-0">
          <div className="flex items-center gap-3">
            <button
              onClick={() => setMobileOpen(true)}
              className="lg:hidden p-2 rounded-lg hover:bg-white/5 text-white/50 hover:text-white transition-colors"
              aria-label="Open menu"
            >
              <Menu className="w-4 h-4" />
            </button>

            {/* Search bar — opens command palette */}
            <button
              onClick={() => setCommandOpen(true)}
              className="hidden sm:flex items-center gap-2 bg-surface-100/50 border border-white/10 rounded-xl px-3 py-1.5 w-56 text-left hover:border-white/20 transition-colors"
            >
              <Search className="w-3.5 h-3.5 text-white/50 flex-shrink-0" />
              <span className="text-sm text-white/50 flex-1">Search...</span>
              <kbd className="text-[10px] text-white/40 font-mono bg-white/10 px-1.5 py-0.5 rounded border border-white/10 hidden md:block">
                ⌘K
              </kbd>
            </button>
          </div>

          <div className="flex items-center gap-2">
            {/* Notifications */}
            <div className="relative" ref={notifRef}>
              <button
                onClick={() => setNotifOpen(!notifOpen)}
                className="relative p-2 rounded-xl hover:bg-white/5 text-white/40 hover:text-white transition-colors"
                aria-label="Notifications"
              >
                <Bell className="w-4 h-4" />
                {unreadCount > 0 && (
                  <span className="absolute top-1 right-1 min-w-[14px] h-3.5 bg-brand-500 rounded-full border border-surface text-[8px] flex items-center justify-center text-white font-bold px-0.5">
                    {unreadCount > 9 ? '9+' : unreadCount}
                  </span>
                )}
              </button>
              <AnimatePresence>
                {notifOpen && (
                  <NotificationPanel
                    onClose={() => setNotifOpen(false)}
                    notifications={notifications}
                    unreadCount={unreadCount}
                    isLoading={isLoading}
                    markRead={markRead}
                    markAllRead={markAllRead}
                  />
                )}
              </AnimatePresence>
            </div>

            {/* Profile completion ring */}
            {user && (
              <ProfileRing pct={profilePct} onClick={() => navigate(profilePath)} />
            )}

            {/* Avatar dropdown */}
            {user && (
              <AvatarDropdown name={user.name} avatarUrl={user.avatarUrl} />
            )}
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto flex flex-col">
          <div className="flex-1">
            <Outlet />
          </div>
          <footer className="flex-shrink-0 border-t border-white/5 px-6 py-3 flex items-center justify-between gap-4">
            <span className="text-xs text-white/50 font-medium">NexusEd</span>
            <span className="text-xs text-white/40 text-center">
              © {new Date().getFullYear()} Ai Nexus Innovation Hub Pvt Ltd. All rights reserved.
            </span>
            <span className="text-xs text-white/30 hidden sm:block">v1.0.0</span>
          </footer>
        </main>
      </div>

      {/* Command Palette */}
      <AnimatePresence>
        {commandOpen && (
          <CommandPalette onClose={() => setCommandOpen(false)} />
        )}
      </AnimatePresence>
    </div>
  );
}
