import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

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
  role: 'STUDENT' | 'CENTER_ADMIN' | 'INSTITUTION_ADMIN' | 'SUPER_ADMIN' | 'TEACHER' | 'PARENT' | 'GUEST';
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
      storage: createJSONStorage(() => sessionStorage),
      onRehydrateStorage: () => (state) => {
        if (!state?.token) return;
        if (isJwtExpired(state.token)) {
          // Access token is expired. If there is no refresh token either, wipe
          // everything immediately. If there IS a refresh token, at least null
          // out the expired access token so the request interceptor never
          // injects it and overwrites a fresh token obtained during login.
          if (!state.refreshToken) {
            state.logout();
          } else {
            state.setTokens('', state.refreshToken); // clear stale access token
          }
        }
      },
    }
  )
);
