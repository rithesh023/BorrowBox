import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import api from '../services/api.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem('borrowbox_user') || 'null'); } catch { return null; }
  });
  const [adminSession, setAdminSession] = useState(() => localStorage.getItem('borrowbox_admin_session') === 'true');
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const token = localStorage.getItem('borrowbox_token');
    if (!token) { setReady(true); return; }
    api.get('/auth/me').then(({ data }) => {
      setUser(data);
      localStorage.setItem('borrowbox_user', JSON.stringify(data));
    }).catch(() => {
      localStorage.removeItem('borrowbox_token');
      localStorage.removeItem('borrowbox_user');
      localStorage.removeItem('borrowbox_admin_session');
      setUser(null);
      setAdminSession(false);
    }).finally(() => setReady(true));
  }, []);

  const value = useMemo(() => {
    async function login(email, password) {
      return storeSession(await api.post('/auth/login', { email, password }), false);
    }
    async function adminLogin(email, password) {
      return storeSession(await api.post('/auth/admin-login', { email, password }), true);
    }
    function storeSession(response, isAdminSession) {
      const { data } = response;
      localStorage.setItem('borrowbox_token', data.accessToken);
      localStorage.setItem('borrowbox_user', JSON.stringify(data.user));
      if (isAdminSession) localStorage.setItem('borrowbox_admin_session', 'true');
      else localStorage.removeItem('borrowbox_admin_session');
      setUser(data.user);
      setAdminSession(isAdminSession);
      return data.user;
    }
    return {
      user,
      ready,
      adminSession,
      login,
      adminLogin,
      async register(fields) {
        await api.post('/auth/register', fields);
        return login(fields.email, fields.password);
      },
      logout() {
      localStorage.removeItem('borrowbox_token');
      localStorage.removeItem('borrowbox_user');
      localStorage.removeItem('borrowbox_admin_session');
      setUser(null);
      setAdminSession(false);
      },
      updateUser(next) {
      setUser(next);
      localStorage.setItem('borrowbox_user', JSON.stringify(next));
      },
    };
  }, [user, ready, adminSession]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}
