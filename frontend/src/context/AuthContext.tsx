import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';
import { apiClient } from '../api/client';
import type { LoginResponse } from '../api/types';

interface AuthUser {
  email: string;
  fullName: string;
  organization: string;
  role: string;
}

interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function readStoredUser(): AuthUser | null {
  const raw = localStorage.getItem('sentinelap_user');
  return raw ? (JSON.parse(raw) as AuthUser) : null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(readStoredUser());

  const login = async (email: string, password: string) => {
    const { data } = await apiClient.post<LoginResponse>('/api/auth/login', { email, password });
    const authUser: AuthUser = {
      email: data.email,
      fullName: data.fullName,
      organization: data.organization,
      role: data.role,
    };
    localStorage.setItem('sentinelap_token', data.token);
    localStorage.setItem('sentinelap_user', JSON.stringify(authUser));
    setUser(authUser);
  };

  const logout = () => {
    localStorage.removeItem('sentinelap_token');
    localStorage.removeItem('sentinelap_user');
    setUser(null);
  };

  const value = useMemo(
    () => ({ user, isAuthenticated: user !== null, login, logout }),
    [user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
