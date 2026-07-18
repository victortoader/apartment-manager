import React, { createContext, useContext, useState, useEffect } from 'react';

const API = process.env.REACT_APP_API_URL || '';
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [loading, setLoading] = useState(true);
  const [unreadTickets, setUnreadTickets] = useState(0);

  useEffect(() => {
    if (token) {
      fetch(`${API}/api/auth/me`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })
        .then(res => {
          if (res.ok) return res.json();
          throw new Error();
        })
        .then(data => setUser(data))
        .catch(() => {
          localStorage.removeItem('token');
          setToken(null);
        })
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, [token]);

  const login = async (username, password) => {
    const res = await fetch(`${API}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });

    if (!res.ok) {
      const data = await res.json();
      throw new Error(data.error || 'Login failed');
    }

    const data = await res.json();
    localStorage.setItem('token', data.token);
    setToken(data.token);
    setUser({ id: data.id, username: data.username, role: data.role, apartmentId: data.apartmentId });
    return data;
  };

  const logout = () => {
    localStorage.removeItem('token');
    setToken(null);
    setUser(null);
  };

  const authHeader = () => ({
    'Authorization': `Bearer ${token}`
  });

  const fetchUnreadCount = async () => {
    if (!token) return;
    const role = user?.role;
    if (role !== 'OWNER' && role !== 'ADMIN') return;
    try {
      const res = await fetch(`${API}/api/tickets/unread/count`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (res.ok) {
        const data = await res.json();
        setUnreadTickets(data.count);
      }
    } catch (e) {}
  };

  return (
    <AuthContext.Provider value={{ user, token, login, logout, authHeader, loading, isAuthenticated: !!token && !!user, unreadTickets, fetchUnreadCount }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
