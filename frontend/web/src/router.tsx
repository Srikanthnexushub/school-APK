import { createBrowserRouter, Navigate } from 'react-router-dom';
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import OtpPage from './pages/auth/OtpPage';
import ForgotPasswordPage from './pages/auth/ForgotPasswordPage';
import ResetPasswordPage from './pages/auth/ResetPasswordPage';
import AppLayout from './components/layout/AppLayout';
import ProtectedRoute from './components/layout/ProtectedRoute';
import StudentDashboardPage from './pages/dashboard/StudentDashboardPage';
import AiMentorPage from './pages/ai-mentor/AiMentorPage';
import StudyPlanDetailPage from './pages/ai-mentor/StudyPlanDetailPage';
import DoubtsPage from './pages/ai-mentor/DoubtsPage';
import AcademicPlanPage from './pages/academic-plan/AcademicPlanPage';
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
import ParentProfilePage from './pages/parent/ParentProfilePage';
import ParentChildrenPage from './pages/parent/ParentChildrenPage';
import ParentFeesPage from './pages/parent/ParentFeesPage';
import ParentPsychometricPage from './pages/parent/ParentPsychometricPage';
import ParentLibraryPage from './pages/parent/ParentLibraryPage';
import QuestionBankPage from './pages/question-bank/QuestionBankPage';
import AdminPortalPage from './pages/admin/AdminPortalPage';
import AcceptInvitationPage from './pages/auth/AcceptInvitationPage';
import AdminDashboardPage from './pages/admin/AdminDashboardPage';
import AdminBatchesPage from './pages/admin/AdminBatchesPage';
import AdminAssessmentsPage from './pages/admin/AdminAssessmentsPage';
import AdminCentersPage from './pages/admin/AdminCentersPage';
import MentorPortalDashboardPage from './pages/mentor-portal/MentorPortalDashboardPage';
import MentorPortalSessionsPage from './pages/mentor-portal/MentorPortalSessionsPage';
import MentorPortalInsightsPage from './pages/mentor-portal/MentorPortalInsightsPage';
import MentorPortalExamsPage from './pages/mentor-portal/MentorPortalExamsPage';
import MentorPortalPerformancePage from './pages/mentor-portal/MentorPortalPerformancePage';
import MentorPortalLibraryPage from './pages/mentor-portal/MentorPortalLibraryPage';
import StudentLibraryPage from './pages/library/StudentLibraryPage';
import ProjectLabPage from './pages/lab/ProjectLabPage';
import StudentAssignmentsPage from './pages/assignments/StudentAssignmentsPage';
import MentorPortalAssignmentsPage from './pages/mentor-portal/MentorPortalAssignmentsPage';
import StudentAttendancePage from './pages/attendance/StudentAttendancePage';
import StudentFeesPage from './pages/fees/StudentFeesPage';
import StudentAnnouncementsPage from './pages/announcements/StudentAnnouncementsPage';
import ParentAttendancePage from './pages/parent/ParentAttendancePage';
import ParentAnnouncementsPage from './pages/parent/ParentAnnouncementsPage';
import MentorPortalAttendancePage from './pages/mentor-portal/MentorPortalAttendancePage';
import MentorPortalAnnouncementsPage from './pages/mentor-portal/MentorPortalAnnouncementsPage';
import AdminAnnouncementsPage from './pages/admin/AdminAnnouncementsPage';
import AdminFeesPage from './pages/admin/AdminFeesPage';
import AdminBillingPage from './pages/admin/AdminBillingPage';
import AdminAttendancePage from './pages/admin/AdminAttendancePage';
import AdminAuditPage from './pages/admin/AdminAuditPage';
import StudentCurricularPage from './pages/curricular/StudentCurricularPage';
import MentorPortalCurricularPage from './pages/mentor-portal/MentorPortalCurricularPage';
import ParentCurricularPage from './pages/parent/ParentCurricularPage';
import AdminCurricularPage from './pages/admin/AdminCurricularPage';
import TeacherApprovalGate from './components/teacher/TeacherApprovalGate';

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
    path: '/forgot-password',
    element: <ForgotPasswordPage />,
  },
  {
    path: '/reset-password',
    element: <ResetPasswordPage />,
  },
  {
    path: '/accept-invitation',
    element: <AcceptInvitationPage />,
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
      { path: 'lab', element: <ProjectLabPage /> },
      { path: 'assignments',   element: <StudentAssignmentsPage /> },
      { path: 'attendance',    element: <StudentAttendancePage /> },
      { path: 'fees',          element: <StudentFeesPage /> },
      { path: 'announcements', element: <StudentAnnouncementsPage /> },
      { path: 'academic-plan', element: <AcademicPlanPage /> },
      { path: 'academic-plan/:id', element: <StudyPlanDetailPage /> },
      { path: 'settings', element: <SettingsPage /> },
      { path: 'question-bank', element: <QuestionBankPage /> },
      { path: 'curricular', element: <StudentCurricularPage /> },
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
      { path: 'profile', element: <ParentProfilePage /> },
      { path: 'children', element: <ParentChildrenPage /> },
      { path: 'fees', element: <ParentFeesPage /> },
      { path: 'psychometric', element: <ParentPsychometricPage /> },
      { path: 'question-bank', element: <QuestionBankPage /> },
      { path: 'library',        element: <ParentLibraryPage /> },
      { path: 'attendance',     element: <ParentAttendancePage /> },
      { path: 'announcements',  element: <ParentAnnouncementsPage /> },
      { path: 'curricular',     element: <ParentCurricularPage /> },
    ],
  },
  {
    path: '/admin',
    element: (
      <ProtectedRoute allowedRoles={['CENTER_ADMIN', 'INSTITUTION_ADMIN', 'SUPER_ADMIN']}>
        <AppLayout />
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: <AdminPortalPage /> },
      { path: 'dashboard', element: <AdminPortalPage /> },
      { path: 'centers', element: <AdminPortalPage /> },
      { path: 'batches',        element: <AdminPortalPage /> },
      { path: 'assessments',    element: <AdminPortalPage /> },
      { path: 'attendance',     element: <AdminAttendancePage /> },
      { path: 'announcements',  element: <AdminAnnouncementsPage /> },
      { path: 'fees',           element: <Navigate to="/admin?tab=accounts" replace /> },
      { path: 'billing',        element: <Navigate to="/admin?tab=accounts" replace /> },
      { path: 'audit',          element: <AdminAuditPage /> },
      { path: 'question-bank',  element: <QuestionBankPage /> },
      { path: 'curricular',     element: <AdminCurricularPage /> },
    ],
  },
  {
    path: '/mentor-portal',
    element: (
      <ProtectedRoute allowedRoles={['TEACHER']}>
        <TeacherApprovalGate>
          <AppLayout />
        </TeacherApprovalGate>
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: <MentorPortalDashboardPage /> },
      { path: 'dashboard', element: <MentorPortalDashboardPage /> },
      { path: 'sessions',    element: <MentorPortalSessionsPage /> },
      { path: 'insights',    element: <MentorPortalInsightsPage /> },
      { path: 'exams',       element: <MentorPortalExamsPage /> },
      { path: 'assignments', element: <MentorPortalAssignmentsPage /> },
      { path: 'performance', element: <MentorPortalPerformancePage /> },
      { path: 'library',        element: <MentorPortalLibraryPage /> },
      { path: 'attendance',     element: <MentorPortalAttendancePage /> },
      { path: 'announcements',  element: <MentorPortalAnnouncementsPage /> },
      { path: 'question-bank',  element: <QuestionBankPage /> },
      { path: 'curricular',     element: <MentorPortalCurricularPage /> },
    ],
  },
]);
