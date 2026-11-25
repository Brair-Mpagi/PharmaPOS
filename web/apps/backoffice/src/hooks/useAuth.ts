'use client';

import { useState, useEffect, useCallback } from 'react';
import { authApi, LoginRequest, LoginResponse } from '@/lib/api';

interface AuthUser {
  id: string;
  username: string;
  email: string;
  roles: string[];
  tenantId: string;
  branchId: string;
}

interface AuthState {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
}

export function useAuth() {
  const [state, setState] = useState<AuthState>({
    user: null,
    isAuthenticated: false,
    isLoading: true,
    error: null,
  });

  // Rehydrate from localStorage on mount
  useEffect(() => {
    if (typeof window === 'undefined') return;
    const token = localStorage.getItem('access_token');
    const userJson = localStorage.getItem('auth_user');
    if (token && userJson) {
      try {
        const user = JSON.parse(userJson) as AuthUser;
        setState({ user, isAuthenticated: true, isLoading: false, error: null });
      } catch {
        localStorage.clear();
        setState({ user: null, isAuthenticated: false, isLoading: false, error: null });
      }
    } else {
      setState((s) => ({ ...s, isLoading: false }));
    }
  }, []);

  const login = useCallback(async (credentials: LoginRequest) => {
    setState((s) => ({ ...s, isLoading: true, error: null }));
    try {
      const { data } = await authApi.login(credentials);
      localStorage.setItem('access_token', data.access_token);
      localStorage.setItem('refresh_token', data.refresh_token);
      localStorage.setItem('tenant_id', data.user.tenantId);
      localStorage.setItem('auth_user', JSON.stringify(data.user));
      setState({
        user: data.user,
        isAuthenticated: true,
        isLoading: false,
        error: null,
      });
      return data;
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Login failed. Please check your credentials.';
      setState((s) => ({ ...s, isLoading: false, error: msg }));
      throw err;
    }
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // best-effort
    } finally {
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
      localStorage.removeItem('tenant_id');
      localStorage.removeItem('auth_user');
      setState({ user: null, isAuthenticated: false, isLoading: false, error: null });
      window.location.href = '/login';
    }
  }, []);

  const hasRole = useCallback(
    (role: string): boolean => state.user?.roles?.includes(role) ?? false,
    [state.user]
  );

  const hasAnyRole = useCallback(
    (...roles: string[]): boolean =>
      roles.some((r) => state.user?.roles?.includes(r)) ?? false,
    [state.user]
  );

  const isManager = hasAnyRole('MANAGER', 'PHARMACIST', 'ADMIN', 'SUPER_ADMIN');
  const isAdmin = hasAnyRole('ADMIN', 'SUPER_ADMIN');

  return {
    ...state,
    login,
    logout,
    hasRole,
    hasAnyRole,
    isManager,
    isAdmin,
  };
}
