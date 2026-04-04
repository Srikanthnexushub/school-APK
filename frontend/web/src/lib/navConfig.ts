import type { ElementType } from 'react';
import {
  LayoutDashboard, Bot, ClipboardList, BarChart3, Target, Brain,
  Users, Calendar, BookOpen, Library, Award,
  CreditCard, Beaker, UserCog, BookCheck, Building2, Upload, UserCheck,
  Megaphone, CalendarCheck, GraduationCap, Wallet, ShieldCheck, Layers, Bell, Sparkles,
} from 'lucide-react';

export interface NavItem {
  icon: ElementType;
  label: string;
  to: string;
  badge?: number;
  disabled?: boolean;
  sectionLabel?: string;
}

export const studentNav: NavItem[] = [
  { icon: LayoutDashboard, label: 'Dashboard',    to: '/dashboard' },
  { icon: GraduationCap,   label: 'Academic Plan',to: '/academic-plan' },
  { icon: Beaker,           label: 'AI Lab',       to: '/lab' },
  { icon: Bot,              label: 'AI Mentor',    to: '/ai-mentor' },
  { icon: Bell,             label: 'Announcements',to: '/announcements' },
  { icon: ClipboardList,    label: 'Assessments',  to: '/assessments' },
  { icon: BookCheck,        label: 'Assignments',  to: '/assignments' },
  { icon: CalendarCheck,    label: 'Attendance',   to: '/attendance' },
  { icon: Target,           label: 'Career Oracle',to: '/career' },
  { icon: Layers,           label: 'Curricular',   to: '/curricular' },
  { icon: Calendar,         label: 'Exam Tracker', to: '/exam-tracker' },
  { icon: CreditCard,       label: 'Fees',         to: '/fees' },
  { icon: Library,          label: 'Library',      to: '/library' },
  { icon: Users,            label: 'Mentors',      to: '/mentors' },
  { icon: BarChart3,        label: 'Performance',  to: '/performance' },
  { icon: Brain,            label: 'Psychometric', to: '/psychometric' },
  { icon: BookOpen,         label: 'Question Bank',to: '/question-bank' },
];

export function getAdminNav(role?: string): NavItem[] {
  return [
    { icon: LayoutDashboard, label: 'Overview',          to: '/admin?tab=overview' },
    { icon: Wallet,          label: 'Accounts',          to: '/admin?tab=accounts' },
    { icon: GraduationCap,   label: 'Academic Plan',     to: '/academic-plan' },
    { icon: Bell,            label: 'Announcements',     to: '/admin/announcements' },
    { icon: ClipboardList,   label: 'Assessments',       to: '/admin?tab=assessments' },
    { icon: BookCheck,       label: 'Assignments',       to: '/admin?tab=assignments' },
    { icon: CalendarCheck,   label: 'Attendance',        to: '/admin/attendance' },
    ...(role === 'SUPER_ADMIN' ? [
      { icon: Megaphone,   label: 'Banners',             to: '/admin?tab=banners' },
    ] as NavItem[] : []),
    { icon: Users,           label: 'Batches',           to: '/admin?tab=batches' },
    { icon: Upload,          label: 'Bulk Import',       to: '/admin?tab=teacher-import' },
    { icon: Building2,       label: 'Centers',           to: '/admin?tab=centers' },
    { icon: Layers,          label: 'Curricular',        to: '/admin/curricular' },
    { icon: Library,         label: 'Library',           to: '/admin?tab=library' },
    ...(role === 'SUPER_ADMIN' ? [
      { icon: ShieldCheck, label: 'NFR Audit',           to: '/admin/audit' },
    ] as NavItem[] : []),
    { icon: Brain,           label: 'Psychometric',      to: '/admin?tab=psychometric' },
    { icon: BookOpen,        label: 'Question Bank',     to: '/admin/question-bank' },
    { icon: UserCog,         label: 'Staff',             to: '/admin?tab=staff' },
    { icon: UserCheck,       label: 'Teacher Approvals', to: '/admin?tab=teacher-pending' },
  ];
}

export const parentNav: NavItem[] = [
  { icon: LayoutDashboard, label: 'Overview',      to: '/parent' },
  { icon: GraduationCap,   label: 'Academic Plan', to: '/academic-plan' },
  { icon: Bell,            label: 'Announcements', to: '/parent/announcements' },
  { icon: CalendarCheck,   label: 'Attendance',    to: '/parent/attendance' },
  { icon: Users,           label: 'My Children',   to: '/parent/children' },
  { icon: Bot,             label: 'Copilot',       to: '/parent/copilot' },
  { icon: Layers,          label: 'Curricular',    to: '/parent/curricular' },
  { icon: CreditCard,      label: 'Fees',          to: '/parent/fees' },
  { icon: Library,         label: 'Library',       to: '/parent/library' },
  { icon: Brain,           label: 'Psychometric',  to: '/parent/psychometric' },
  { icon: BookOpen,        label: 'Question Bank', to: '/parent/question-bank' },
];

export const mentorNav: NavItem[] = [
  { icon: LayoutDashboard, label: 'Dashboard',     to: '/mentor-portal' },
  { icon: GraduationCap,   label: 'Academic Plan', to: '/academic-plan' },
  { icon: Sparkles,        label: 'AI Insights',   to: '/mentor-portal/insights' },
  { icon: Megaphone,       label: 'Announcements', to: '/mentor-portal/announcements' },
  { icon: BookCheck,       label: 'Assignments',   to: '/mentor-portal/assignments' },
  { icon: CalendarCheck,   label: 'Attendance',    to: '/mentor-portal/attendance' },
  { icon: Layers,          label: 'Curricular',    to: '/mentor-portal/curricular' },
  { icon: ClipboardList,   label: 'Exams',         to: '/mentor-portal/exams' },
  { icon: Library,         label: 'Library',       to: '/mentor-portal/library' },
  { icon: Award,           label: 'My Performance',to: '/mentor-portal/performance' },
  { icon: Brain,           label: 'Psychometric',  to: '/psychometric' },
  { icon: BookOpen,        label: 'Question Bank', to: '/mentor-portal/question-bank' },
  { icon: Calendar,        label: 'Sessions',      to: '/mentor-portal/sessions' },
];

export function getNavItems(role?: string): NavItem[] {
  if (role === 'CENTER_ADMIN' || role === 'INSTITUTION_ADMIN' || role === 'SUPER_ADMIN') return getAdminNav(role);
  if (role === 'PARENT') return parentNav;
  if (role === 'TEACHER') return mentorNav;
  return studentNav;
}

export const ROLE_COLORS: Record<string, { neon: string; bg: string; border: string; badgeBg: string; badgeText: string; ring: string }> = {
  STUDENT:           { neon: '#00f5ff', bg: 'rgba(0,245,255,0.08)',   border: '#00f5ff',   badgeBg: 'rgba(0,245,255,0.15)',   badgeText: '#00f5ff',   ring: 'rgba(0,245,255,0.30)' },
  PARENT:            { neon: '#f000ff', bg: 'rgba(240,0,255,0.08)',   border: '#f000ff',   badgeBg: 'rgba(240,0,255,0.15)',   badgeText: '#f000ff',   ring: 'rgba(240,0,255,0.30)' },
  TEACHER:           { neon: '#00ff88', bg: 'rgba(0,255,136,0.08)',   border: '#00ff88',   badgeBg: 'rgba(0,255,136,0.15)',   badgeText: '#00ff88',   ring: 'rgba(0,255,136,0.30)' },
  CENTER_ADMIN:      { neon: '#ffd700', bg: 'rgba(255,215,0,0.08)',   border: '#ffd700',   badgeBg: 'rgba(255,215,0,0.15)',   badgeText: '#ffd700',   ring: 'rgba(255,215,0,0.30)' },
  INSTITUTION_ADMIN: { neon: '#9b00ff', bg: 'rgba(155,0,255,0.08)',   border: '#9b00ff',   badgeBg: 'rgba(155,0,255,0.15)',   badgeText: '#9b00ff',   ring: 'rgba(155,0,255,0.30)' },
  SUPER_ADMIN:       { neon: '#ff6b00', bg: 'rgba(255,107,0,0.08)',   border: '#ff6b00',   badgeBg: 'rgba(255,107,0,0.15)',   badgeText: '#ff6b00',   ring: 'rgba(255,107,0,0.30)' },
  GUEST:             { neon: 'rgba(255,255,255,0.5)', bg: 'rgba(255,255,255,0.05)', border: 'rgba(255,255,255,0.2)', badgeBg: 'rgba(255,255,255,0.08)', badgeText: 'rgba(255,255,255,0.5)', ring: 'rgba(255,255,255,0.15)' },
};
