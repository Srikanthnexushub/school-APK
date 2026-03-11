import axios from 'axios';
import { useAuthStore } from '../stores/authStore';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 90000,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// ── Silent token refresh ───────────────────────────────────────────────────
// On 401, try once to rotate the refresh token. All concurrent requests that
// 401 while a refresh is in-flight are queued and retried after the new token
// arrives. If the refresh itself fails the user is logged out.

let isRefreshing = false;
let waitQueue: Array<(token: string) => void> = [];

function flushQueue(newToken: string) {
  waitQueue.forEach((resolve) => resolve(newToken));
  waitQueue = [];
}

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;

    // Only intercept 401s that haven't been retried and are not the refresh
    // endpoint itself (avoids infinite loop).
    if (
      error.response?.status !== 401 ||
      original._retried ||
      original.url?.includes('/api/v1/auth/refresh')
    ) {
      return Promise.reject(error);
    }

    if (isRefreshing) {
      // Queue this request until the in-flight refresh completes.
      return new Promise<string>((resolve) => {
        waitQueue.push(resolve);
      }).then((newToken) => {
        original.headers.Authorization = `Bearer ${newToken}`;
        return api(original);
      });
    }

    original._retried = true;
    isRefreshing = true;

    const { refreshToken, deviceId, setTokens, logout } = useAuthStore.getState();

    if (!refreshToken) {
      isRefreshing = false;
      logout();
      window.location.href = '/login';
      return Promise.reject(error);
    }

    try {
      const { data } = await axios.post(
        `${import.meta.env.VITE_API_BASE_URL ?? ''}/api/v1/auth/refresh`,
        {
          refreshToken,
          deviceFingerprint: {
            userAgent: navigator.userAgent,
            deviceId: deviceId ?? crypto.randomUUID(),
            ipSubnet: '127.0.0',
          },
        },
        { headers: { 'Content-Type': 'application/json' } }
      );

      const newAccess: string = data.accessToken;
      const newRefresh: string = data.refreshToken;

      setTokens(newAccess, newRefresh);
      flushQueue(newAccess);

      original.headers.Authorization = `Bearer ${newAccess}`;
      return api(original);
    } catch {
      waitQueue = [];
      logout();
      window.location.href = '/login';
      return Promise.reject(error);
    } finally {
      isRefreshing = false;
    }
  }
);

export default api;
