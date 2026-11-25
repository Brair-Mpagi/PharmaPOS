import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

const api: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 30_000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Attach JWT from localStorage before every request
api.interceptors.request.use((config) => {
  if (typeof window !== 'undefined') {
    const token = localStorage.getItem('access_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    const tenantId = localStorage.getItem('tenant_id');
    if (tenantId) {
      config.headers['X-Tenant-Id'] = tenantId;
    }
  }
  return config;
});

// Handle 401 → redirect to login
api.interceptors.response.use(
  (res: AxiosResponse) => res,
  (err) => {
    if (err.response?.status === 401 && typeof window !== 'undefined') {
      localStorage.removeItem('access_token');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

// ─── Auth ──────────────────────────────────────────────────────────────────

export interface LoginRequest {
  username: string;
  password: string;
  tenantSubdomain: string;
}

export interface LoginResponse {
  access_token: string;
  refresh_token: string;
  expires_in: number;
  user: {
    id: string;
    username: string;
    email: string;
    roles: string[];
    tenantId: string;
    branchId: string;
  };
}

export const authApi = {
  login: (data: LoginRequest): Promise<AxiosResponse<LoginResponse>> =>
    api.post('/api/v1/auth/login', data),

  logout: (): Promise<AxiosResponse<void>> =>
    api.post('/api/v1/auth/logout'),

  refreshToken: (refreshToken: string): Promise<AxiosResponse<LoginResponse>> =>
    api.post('/api/v1/auth/refresh', { refresh_token: refreshToken }),

  requestPasswordReset: (data: {
    username: string;
    email: string;
    reason: string;
  }): Promise<AxiosResponse<{ requestId: string }>> =>
    api.post('/api/v1/auth/password-reset-request', data),

  approvePasswordReset: (data: {
    requestId: string;
    managerId: string;
    newPassword: string;
  }): Promise<AxiosResponse<void>> =>
    api.post('/api/v1/auth/password-reset-approve', data),

  listPendingPasswordResets: (): Promise<AxiosResponse<PasswordResetRequest[]>> =>
    api.get('/api/v1/auth/password-reset-requests'),
};

// ─── Users ─────────────────────────────────────────────────────────────────

export interface User {
  id: string;
  username: string;
  email: string;
  status: 'ACTIVE' | 'DISABLED' | 'LOCKED';
  mfaEnabled: boolean;
  roles: string[];
  createdAt: string;
  lastLoginAt?: string;
}

export interface PasswordResetRequest {
  id: string;
  userId: string;
  username: string;
  email: string;
  reason: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  requestedAt: string;
  approvedBy?: string;
  approvedAt?: string;
}

export const usersApi = {
  list: (params?: {
    page?: number;
    size?: number;
    search?: string;
  }): Promise<AxiosResponse<{ content: User[]; totalElements: number; totalPages: number }>> =>
    api.get('/api/v1/identity/users', { params }),

  get: (id: string): Promise<AxiosResponse<User>> =>
    api.get(`/api/v1/identity/users/${id}`),

  create: (data: Omit<User, 'id' | 'createdAt'>): Promise<AxiosResponse<User>> =>
    api.post('/api/v1/identity/users', data),

  update: (id: string, data: Partial<User>): Promise<AxiosResponse<User>> =>
    api.put(`/api/v1/identity/users/${id}`, data),

  disable: (id: string, reason: string): Promise<AxiosResponse<void>> =>
    api.post(`/api/v1/identity/users/${id}/disable`, { reason }),

  enable: (id: string): Promise<AxiosResponse<void>> =>
    api.post(`/api/v1/identity/users/${id}/enable`),
};

// ─── Inventory ─────────────────────────────────────────────────────────────

export interface StockBatch {
  id: string;
  productId: string;
  productName: string;
  batchNumber: string;
  expiresAt: string;
  quantityOnHand: number;
  quantityReserved: number;
  costPrice: number;
  status: 'AVAILABLE' | 'QUARANTINED' | 'RECALLED' | 'EXPIRED' | 'DEPLETED';
  branchId: string;
  supplierId?: string;
  receivedAt: string;
}

export const inventoryApi = {
  listBatches: (params?: {
    page?: number;
    size?: number;
    search?: string;
    status?: string;
    lowStock?: boolean;
  }): Promise<AxiosResponse<{ content: StockBatch[]; totalElements: number; totalPages: number }>> =>
    api.get('/api/v1/inventory/batches', { params }),

  getBatch: (id: string): Promise<AxiosResponse<StockBatch>> =>
    api.get(`/api/v1/inventory/batches/${id}`),

  getLowStockAlerts: (): Promise<AxiosResponse<StockBatch[]>> =>
    api.get('/api/v1/inventory/alerts/low-stock'),
};

// ─── Sales ─────────────────────────────────────────────────────────────────

export interface Sale {
  id: string;
  receiptNumber: string;
  customerId?: string;
  customerName?: string;
  total: number;
  discount: number;
  tax: number;
  netTotal: number;
  paymentMethod: string;
  status: 'DRAFT' | 'FINALISED' | 'VOIDED' | 'REFUNDED';
  cashierId: string;
  cashierName: string;
  shiftId: string;
  createdAt: string;
  items: SaleItem[];
}

export interface SaleItem {
  id: string;
  productId: string;
  productName: string;
  batchId: string;
  quantity: number;
  unitPrice: number;
  discount: number;
  tax: number;
  lineTotal: number;
}

export const salesApi = {
  list: (params?: {
    page?: number;
    size?: number;
    shiftId?: string;
    dateFrom?: string;
    dateTo?: string;
  }): Promise<AxiosResponse<{ content: Sale[]; totalElements: number; totalPages: number }>> =>
    api.get('/api/v1/sales', { params }),

  get: (id: string): Promise<AxiosResponse<Sale>> =>
    api.get(`/api/v1/sales/${id}`),

  void: (id: string, reason: string): Promise<AxiosResponse<void>> =>
    api.post(`/api/v1/sales/${id}/void`, { reason }),

  listShifts: (): Promise<
    AxiosResponse<
      Array<{
        id: string;
        openedAt: string;
        closedAt?: string;
        status: string;
        cashierId: string;
        cashierName: string;
      }>
    >
  > => api.get('/api/v1/sales/shifts'),
};

// ─── Procurement ───────────────────────────────────────────────────────────

export interface PurchaseOrder {
  id: string;
  poNumber: string;
  supplierId: string;
  supplierName: string;
  status: 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'ORDERED' | 'PARTIAL_RECEIVED' | 'RECEIVED' | 'CANCELLED';
  total: number;
  currency: string;
  orderDate: string;
  expectedDeliveryDate?: string;
  items: POItem[];
  createdBy: string;
  approvedBy?: string;
  approvedAt?: string;
}

export interface POItem {
  id: string;
  productId: string;
  productName: string;
  quantity: number;
  unitCost: number;
  lineTotal: number;
}

export const procurementApi = {
  listPOs: (params?: {
    page?: number;
    size?: number;
    status?: string;
  }): Promise<AxiosResponse<{ content: PurchaseOrder[]; totalElements: number; totalPages: number }>> =>
    api.get('/api/v1/procurement/purchase-orders', { params }),

  getPO: (id: string): Promise<AxiosResponse<PurchaseOrder>> =>
    api.get(`/api/v1/procurement/purchase-orders/${id}`),

  approvePO: (id: string): Promise<AxiosResponse<void>> =>
    api.post(`/api/v1/procurement/purchase-orders/${id}/approve`),

  cancelPO: (id: string, reason: string): Promise<AxiosResponse<void>> =>
    api.post(`/api/v1/procurement/purchase-orders/${id}/cancel`, { reason }),
};

// ─── Reports ───────────────────────────────────────────────────────────────

export const reportsApi = {
  getSalesSummary: (params: {
    dateFrom: string;
    dateTo: string;
    groupBy?: 'day' | 'week' | 'month';
  }): Promise<AxiosResponse<{ period: string; total: number; count: number }[]>> =>
    api.get('/api/v1/reports/sales-summary', { params }),

  getInventoryValuation: (): Promise<
    AxiosResponse<{ categoryId: string; categoryName: string; totalCost: number; totalBatches: number }[]>
  > => api.get('/api/v1/reports/inventory-valuation'),

  export: (reportType: string, params: Record<string, string>): Promise<AxiosResponse<Blob>> =>
    api.get(`/api/v1/reports/${reportType}/export`, {
      params,
      responseType: 'blob',
    }),
};

// ─── Settings / Tenant ─────────────────────────────────────────────────────

export const settingsApi = {
  getTenantSettings: (): Promise<AxiosResponse<Record<string, unknown>>> =>
    api.get('/api/v1/platform/settings'),

  updateTenantSettings: (data: Record<string, unknown>): Promise<AxiosResponse<void>> =>
    api.put('/api/v1/platform/settings', data),
};

export default api;
