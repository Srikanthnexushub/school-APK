import { createBrowserRouter, Navigate } from 'react-router-dom';
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import OtpPage from './pages/auth/OtpPage';
import AppLayout from './components/layout/AppLayout';
import ProtectedRoute from './components/layout/ProtectedRoute';
import StudentDashboardPage from './pages/dashboard/StudentDashboardPage';
import AiMentorPage from './pages/ai-mentor/AiMentorPage';
import StudyPlanDetailPage from './pages/ai-mentor/StudyPlanDetailPage';
import DoubtsPage from './pages/ai-mentor/DoubtsPage';
import AssessmentsPage from './pages/assessments/AssessmentsPage';
import ExamPage from './pages/assessments/ExamPage';
import PerformancePage from './pages/performance/PerformancePage';
import CareerOraclePage from './pages/career/CareerOraclePage';
import PsychometricPage from './pages/psychometric/PsychometricPage';
import MentorsPage from './pages/mentors/MentorsPage';
import MentorProfilePage from './pages/mentors/MentorProfilePage';
import ExamTrackerPage from './pages/exam-tracker/ExamTrackerPage';
import SettingsPage from './pages/settings/SettingsPage';
import ParentDashboardPage from './pages/parent/ParentDashboardPage';
import ParentCopilotPage from './pages/parent/ParentCopilotPage';
import AdminDashboardPage from './pages/admin/AdminDashboardPage';
import AdminBatchesPage from './pages/admin/AdminBatchesPage';
import AdminAssessmentsPage from './pages/admin/AdminAssessmentsPage';
import MentorPortalDashboardPage from './pages/mentor-portal/MentorPortalDashboardPage';
import MentorPortalSessionsPage from './pages/mentor-portal/MentorPortalSessionsPage';
import MentorPortalInsightsPage from './pages/mentor-portal/MentorPortalInsightsPage';
import MentorPortalExamsPage from './pages/mentor-portal/MentorPortalExamsPage';
import MentorPortalPerformancePage from './pages/mentor-portal/MentorPortalPerformancePage';
import MentorPortalLibraryPage from './pages/mentor-portal/MentorPortalLibraryPage';
import StudentLibraryPage from './pages/library/StudentLibraryPage';

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/register',
    element: <RegisterPage />,
  },
  {
    path: '/verify-otp',
    element: <OtpPage />,
  },
  {
    path: '/',
    element: (
      <ProtectedRoute>
        <AppLayout />
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: <StudentDashboardPage /> },
      { path: 'dashboard', element: <StudentDashboardPage /> },
      { path: 'ai-mentor', element: <AiMentorPage /> },
      { path: 'ai-mentor/study-plans/:id', element: <StudyPlanDetailPage /> },
      { path: 'ai-mentor/doubts', element: <DoubtsPage /> },
      { path: 'assessments', element: <AssessmentsPage /> },
      { path: 'assessments/:examId/exam', element: <ExamPage /> },
      { path: 'assessments/:examId/results', element: <ExamPage /> },
      { path: 'performance', element: <PerformancePage /> },
      { path: 'career', element: <CareerOraclePage /> },
      { path: 'psychometric', element: <PsychometricPage /> },
      { path: 'mentors', element: <MentorsPage /> },
      { path: 'mentors/:id', element: <MentorProfilePage /> },
      { path: 'exam-tracker', element: <ExamTrackerPage /> },
      { path: 'library', element: <StudentLibraryPage /> },
      { path: 'settings', element: <SettingsPage /> },
      { path: 'study-plan', element: <Navigate to="/ai-mentor" replace /> },
      { path: 'doubts', element: <Navigate to="/ai-mentor" replace /> },
    ],
  },
  {
    path: '/parent',
    element: (
      <ProtectedRoute allowedRoles={['PARENT']}>
        <AppLayout />
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: <ParentDashboardPage /> },
      { path: 'dashboard', element: <ParentDashboardPage /> },
      { path: 'copilot', element: <ParentCopilotPage /> },
    ],
  },
  {
    path: '/admin',
    element: (
      <ProtectedRoute allowedRoles={['CENTER_ADMIN', 'SUPER_ADMIN']}>
        <AppLayout />
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: <AdminDashboardPage /> },
      { path: 'dashboard', element: <AdminDashboardPage /> },
      { path: 'centers', element: <AdminDashboardPage /> },
      { path: 'batches', element: <AdminBatchesPage /> },
      { path: 'assessments', element: <AdminAssessmentsPage /> },
    ],
  },
  {
    path: '/mentor-portal',
    element: (
      <ProtectedRoute allowedRoles={['TEACHER']}>
        <AppLayout />
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: <MentorPortalDashboardPage /> },
      { path: 'dashboard', element: <MentorPortalDashboardPage /> },
      { path: 'sessions',    element: <MentorPortalSessionsPage /> },
      { path: 'insights',    element: <MentorPortalInsightsPage /> },
      { path: 'exams',       element: <MentorPortalExamsPage /> },
      { path: 'performance', element: <MentorPortalPerformancePage /> },
      { path: 'library',     element: <MentorPortalLibraryPage /> },
    ],
  },
]);
