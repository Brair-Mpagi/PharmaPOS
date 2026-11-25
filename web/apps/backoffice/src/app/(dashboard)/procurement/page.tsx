'use client';

import { useState, useEffect } from 'react';
import {
  Truck,
  Search,
  Plus,
  CheckCircle,
  Clock,
  XCircle,
  RefreshCw,
  Eye,
} from 'lucide-react';
import { procurementApi, PurchaseOrder } from '@/lib/api';

const STATUS_STYLES: Record<string, string> = {
  DRAFT: 'bg-gray-100 text-gray-700',
  PENDING_APPROVAL: 'bg-amber-100 text-amber-800',
  APPROVED: 'bg-blue-100 text-blue-800',
  ORDERED: 'bg-indigo-100 text-indigo-800',
  PARTIAL_RECEIVED: 'bg-purple-100 text-purple-800',
  RECEIVED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-red-100 text-red-700',
};

const MOCK_POS: PurchaseOrder[] = [
  {
    id: '1',
    poNumber: 'PO-2024-001',
    supplierId: 's1',
    supplierName: 'MedSupply Ltd.',
    status: 'PENDING_APPROVAL',
    total: 15200.0,
    currency: 'USD',
    orderDate: '2024-05-01',
    expectedDeliveryDate: '2024-05-15',
    items: [],
    createdBy: 'Alice M.',
  },
  {
    id: '2',
    poNumber: 'PO-2024-002',
    supplierId: 's2',
    supplierName: 'PharmaCo Africa',
    status: 'RECEIVED',
    total: 8750.5,
    currency: 'USD',
    orderDate: '2024-04-15',
    expectedDeliveryDate: '2024-04-28',
    items: [],
    createdBy: 'Bob K.',
    approvedBy: 'Dr. Jane',
    approvedAt: '2024-04-16',
  },
  {
    id: '3',
    poNumber: 'PO-2024-003',
    supplierId: 's1',
    supplierName: 'MedSupply Ltd.',
    status: 'ORDERED',
    total: 22400.0,
    currency: 'USD',
    orderDate: '2024-05-05',
    expectedDeliveryDate: '2024-05-20',
    items: [],
    createdBy: 'Alice M.',
    approvedBy: 'Dr. Jane',
    approvedAt: '2024-05-06',
  },
  {
    id: '4',
    poNumber: 'PO-2024-004',
    supplierId: 's3',
    supplierName: 'Generic Pharma SA',
    status: 'CANCELLED',
    total: 5100.0,
    currency: 'USD',
    orderDate: '2024-04-20',
    items: [],
    createdBy: 'Bob K.',
  },
];

export default function ProcurementPage() {
  const [orders, setOrders] = useState<PurchaseOrder[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [approvingId, setApprovingId] = useState<string | null>(null);
  const [message, setMessage] = useState('');

  useEffect(() => {
    async function load() {
      setIsLoading(true);
      try {
        const res = await procurementApi.listPOs({ size: 50 });
        const data = res.data?.content;
        setOrders(Array.isArray(data) ? data : MOCK_POS);
      } catch {
        setOrders(MOCK_POS);
      } finally {
        setIsLoading(false);
      }
    }
    load();
  }, []);

  async function handleApprove(po: PurchaseOrder) {
    setApprovingId(po.id);
    try {
      await procurementApi.approvePO(po.id);
      setOrders((prev) =>
        prev.map((o) => (o.id === po.id ? { ...o, status: 'APPROVED' } : o))
      );
      setMessage(`PO ${po.poNumber} approved successfully.`);
    } catch {
      // Fallback: update local state for demo
      setOrders((prev) =>
        prev.map((o) => (o.id === po.id ? { ...o, status: 'APPROVED' } : o))
      );
      setMessage(`PO ${po.poNumber} approved (demo mode — backend not connected).`);
    } finally {
      setApprovingId(null);
      setTimeout(() => setMessage(''), 4000);
    }
  }

  const filtered = orders.filter((o) => {
    const matchSearch =
      !search ||
      o.poNumber.toLowerCase().includes(search.toLowerCase()) ||
      o.supplierName.toLowerCase().includes(search.toLowerCase());
    const matchStatus = statusFilter === 'ALL' || o.status === statusFilter;
    return matchSearch && matchStatus;
  });

  const stats = {
    total: orders.length,
    pending: orders.filter((o) => o.status === 'PENDING_APPROVAL').length,
    value: orders.reduce((s, o) => s + o.total, 0),
  };

  return (
    <div className="p-6 bg-cream min-h-screen">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-espresso">Procurement</h1>
          <p className="text-coffee text-sm">Purchase orders and supplier management</p>
        </div>
        <button className="flex items-center gap-2 px-4 py-2 bg-caramel hover:bg-coffee text-cream text-sm font-medium rounded-lg transition-colors">
          <Plus className="w-4 h-4" />
          New PO
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        {[
          { label: 'Total Orders', value: stats.total, icon: Truck, format: (v: number) => String(v) },
          {
            label: 'Pending Approval',
            value: stats.pending,
            icon: Clock,
            format: (v: number) => String(v),
          },
          {
            label: 'Total Value',
            value: stats.value,
            icon: CheckCircle,
            format: (v: number) => `$${v.toLocaleString('en', { minimumFractionDigits: 2 })}`,
          },
        ].map(({ label, value, icon: Icon, format }) => (
          <div
            key={label}
            className="bg-parchment rounded-xl p-5 shadow-sm border border-mocha/10"
          >
            <div className="flex items-center gap-3">
              <div className="p-2 bg-caramel/15 rounded-lg">
                <Icon className="w-5 h-5 text-caramel" />
              </div>
              <div>
                <p className="text-xs text-coffee">{label}</p>
                <p className="text-xl font-bold text-espresso">{format(value)}</p>
              </div>
            </div>
          </div>
        ))}
      </div>

      {message && (
        <div className="bg-green-50 border border-green-200 text-green-800 rounded-lg p-3 text-sm mb-4">
          {message}
        </div>
      )}

      {/* Filters */}
      <div className="bg-parchment rounded-xl shadow-sm border border-mocha/10 p-4 mb-4 flex flex-wrap gap-3 items-center">
        <div className="relative flex-1 min-w-48">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-mocha" />
          <input
            type="text"
            placeholder="Search PO number or supplier…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-9 pr-3 py-2 bg-cream border border-mocha/20 rounded-lg text-sm text-espresso focus:outline-none focus:ring-2 focus:ring-caramel"
          />
        </div>
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="bg-cream border border-mocha/20 rounded-lg text-sm text-espresso px-3 py-2 focus:outline-none focus:ring-2 focus:ring-caramel"
        >
          <option value="ALL">All Statuses</option>
          <option value="DRAFT">Draft</option>
          <option value="PENDING_APPROVAL">Pending Approval</option>
          <option value="APPROVED">Approved</option>
          <option value="ORDERED">Ordered</option>
          <option value="RECEIVED">Received</option>
          <option value="CANCELLED">Cancelled</option>
        </select>
      </div>

      {/* Table */}
      <div className="bg-parchment rounded-xl shadow-sm border border-mocha/10 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-mocha/10 border-b border-mocha/20">
                <th className="text-left px-4 py-3 text-espresso font-semibold">PO #</th>
                <th className="text-left px-4 py-3 text-espresso font-semibold">Supplier</th>
                <th className="text-left px-4 py-3 text-espresso font-semibold">Order Date</th>
                <th className="text-left px-4 py-3 text-espresso font-semibold">Expected</th>
                <th className="text-right px-4 py-3 text-espresso font-semibold">Total</th>
                <th className="text-left px-4 py-3 text-espresso font-semibold">Status</th>
                <th className="text-left px-4 py-3 text-espresso font-semibold">Created By</th>
                <th className="text-left px-4 py-3 text-espresso font-semibold">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-mocha/10">
              {isLoading ? (
                <tr>
                  <td colSpan={8} className="text-center py-12 text-coffee">
                    <RefreshCw className="w-5 h-5 animate-spin inline mr-2" />
                    Loading orders…
                  </td>
                </tr>
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan={8} className="text-center py-12 text-coffee">
                    No purchase orders found.
                  </td>
                </tr>
              ) : (
                filtered.map((po) => (
                  <tr key={po.id} className="hover:bg-cream/50">
                    <td className="px-4 py-3 font-mono text-xs font-semibold text-espresso">
                      {po.poNumber}
                    </td>
                    <td className="px-4 py-3 text-espresso">{po.supplierName}</td>
                    <td className="px-4 py-3 text-coffee text-xs">{po.orderDate}</td>
                    <td className="px-4 py-3 text-coffee text-xs">
                      {po.expectedDeliveryDate ?? '—'}
                    </td>
                    <td className="px-4 py-3 text-right font-mono text-espresso font-semibold">
                      ${po.total.toLocaleString('en', { minimumFractionDigits: 2 })}
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${
                          STATUS_STYLES[po.status] ?? 'bg-gray-100 text-gray-700'
                        }`}
                      >
                        {po.status.replace('_', ' ')}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-coffee text-xs">{po.createdBy}</td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <button className="text-coffee hover:text-espresso transition-colors" title="View">
                          <Eye className="w-4 h-4" />
                        </button>
                        {po.status === 'PENDING_APPROVAL' && (
                          <button
                            onClick={() => handleApprove(po)}
                            disabled={approvingId === po.id}
                            className="flex items-center gap-1 px-2 py-1 bg-green-100 hover:bg-green-200 text-green-800 text-xs rounded-lg transition-colors disabled:opacity-60"
                          >
                            {approvingId === po.id ? (
                              <RefreshCw className="w-3 h-3 animate-spin" />
                            ) : (
                              <CheckCircle className="w-3 h-3" />
                            )}
                            Approve
                          </button>
                        )}
                        {['DRAFT', 'PENDING_APPROVAL'].includes(po.status) && (
                          <button className="flex items-center gap-1 px-2 py-1 bg-red-50 hover:bg-red-100 text-red-700 text-xs rounded-lg transition-colors">
                            <XCircle className="w-3 h-3" />
                            Cancel
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
