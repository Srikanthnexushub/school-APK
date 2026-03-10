import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface User {
  id: string;
  email: string;
  role: 'STUDENT' | 'CENTER_ADMIN' | 'SUPER_ADMIN' | 'TEACHER' | 'PARENT' | 'GUEST';
  name: string;
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
      logout: () => set({ token: null, refreshToken: null, deviceId: null, user: null, isAuthenticated: false }),
    }),
    { name: 'edupath-auth' }
  )
);
