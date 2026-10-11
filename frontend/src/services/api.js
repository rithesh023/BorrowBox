import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8900/api',
  // Cloud database wake-ups and cold backend requests can take longer than 10 seconds.
  timeout: 30000,
});

export function resolveImageUrl(value) {
  const source = typeof value === 'string' ? value.trim() : '';
  if (!source) return '';
  if (/^(https?:|data:|blob:)/i.test(source)) return source;
  try {
    const apiOrigin = new URL(api.defaults.baseURL, window.location.origin).origin;
    return new URL(source.startsWith('/') ? source : `/${source}`, apiOrigin).toString();
  } catch { return ''; }
}

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('borrowbox_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export default api;
