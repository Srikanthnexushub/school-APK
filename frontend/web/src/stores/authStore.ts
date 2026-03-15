import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export function isJwtExpired(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.exp * 1000 < Date.now();
  } catch {
    return true;
  }
}

interface User {
  id: string;
  email: string;
  role: 'STUDENT' | 'CENTER_ADMIN' | 'SUPER_ADMIN' | 'TEACHER' | 'PARENT' | 'GUEST';
  name: string;
  centerId?: string;
  avatarUrl?: string;
}

interface AuthState {
  token: string | null;
  refreshToken: string | null;
  deviceId: string | null;
  user: User | null;
  isAuthenticated: boolean;
  setAuth: (token: string, user: User, refreshToken: string, deviceId: string) => void;
  setTokens: (token: string, refreshToken: string) => void;
  updateUser: (partial: Partial<User>) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      refreshToken: null,
      deviceId: null,
      user: null,
      isAuthenticated: false,
      setAuth: (token, user, refreshToken, deviceId) =>
        set({ token, refreshToken, deviceId, user, isAuthenticated: true }),
      setTokens: (token, refreshToken) => set({ token, refreshToken }),
      updateUser: (partial) => set((state) => ({ user: state.user ? { ...state.user, ...partial } : null })),
      logout: () => set({ token: null, refreshToken: null, deviceId: null, user: null, isAuthenticated: false }),
    }),
    {
      name: 'edupath-auth',
      onRehydrateStorage: () => (state) => {
        // On startup: if the access token is expired and there is no refresh
        // token to silently rotate it, wipe the persisted session immediately
        // so ProtectedRoute always starts from a clean unauthenticated state.
        if (state?.token && isJwtExpired(state.token) && !state.refreshToken) {
          state.logout();
        }
      },
    }
  )
);
