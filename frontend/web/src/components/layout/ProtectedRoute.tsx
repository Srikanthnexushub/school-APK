import { Navigate } from 'react-router-dom';
import { useAuthStore, isJwtExpired } from '../../stores/authStore';

interface Props {
  allowedRoles?: string[];
  children: React.ReactNode;
}

export default function ProtectedRoute({ allowedRoles, children }: Props) {
  const { isAuthenticated, user, token, refreshToken, logout } = useAuthStore();

  // Token expired with no refresh token → clear stale session and force login.
  // (If a refresh token exists the 401 interceptor will silently rotate it.)
  if (isAuthenticated && token && isJwtExpired(token) && !refreshToken) {
    logout();
    return <Navigate to="/login" replace />;
  }

  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (allowedRoles && user && !allowedRoles.includes(user.role))
    return <Navigate to="/dashboard" replace />;
  return <>{children}</>;
}
