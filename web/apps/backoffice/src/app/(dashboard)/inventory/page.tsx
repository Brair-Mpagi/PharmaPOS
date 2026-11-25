'use client';

import { useState, useEffect } from 'react';
import {
  Package,
  Search,
  AlertTriangle,
  Clock,
  CheckCircle,
  XCircle,
  Filter,
  RefreshCw,
} from 'lucide-react';
import { inventoryApi, StockBatch } from '@/lib/api';

const STATUS_STYLES: Record<string, string> = {
  AVAILABLE: 'bg-green-100 text-green-800',
  QUARANTINED: 'bg-yellow-100 text-yellow-800',
  RECALLED: 'bg-red-100 text-red-800',
  EXPIRED: 'bg-gray-200 text-gray-600',
  DEPLETED: 'bg-mocha/10 text-mocha',
};

const MOCK_BATCHES: StockBatch[] = [
  {
    id: '1',
    productId: 'p1',
    productName: 'Paracetamol 500mg Tabs',
    batchNumber: 'BT-2024-001',
    expiresAt: '2026-06-30',
    quantityOnHand: 1200,
    quantityReserved: 50,
    costPrice: 0.05,
    status: 'AVAILABLE',
    branchId: 'b1',
    receivedAt: '2024-01-10',
  },
  {
    id: '2',
    productId: 'p2',
    productName: 'Amoxicillin 250mg Caps',
    batchNumber: 'BT-2024-002',
    expiresAt: '2025-03-15',
    quantityOnHand: 48,
    quantityReserved: 10,
    costPrice: 0.18,
    status: 'AVAILABLE',
    branchId: 'b1',
    receivedAt: '2024-02-05',
  },
  {
    id: '3',
    productId: 'p3',
    productName: 'Metformin 850mg Tabs',
    batchNumber: 'BT-2024-003',
    expiresAt: '2024-11-01',
    quantityOnHand: 300,
    quantityReserved: 0,
    costPrice: 0.12,
    status: 'EXPIRED',
    branchId: 'b1',
    receivedAt: '2023-05-20',
  },
  {
    id: '4',
    productId: 'p4',
    productName: 'Ibuprofen 400mg Tabs',
    batchNumber: 'BT-2024-004',
    expiresAt: '2026-12-31',
    quantityOnHand: 15,
    quantityReserved: 5,
    costPrice: 0.08,
    status: 'AVAILABLE',
    branchId: 'b1',
    receivedAt: '2024-03-01',
  },
  {
    id: '5',
    productId: 'p5',
    productName: 'Omeprazole 20mg Caps',
    batchNumber: 'BT-2024-005',
    expiresAt: '2025-09-30',
    quantityOnHand: 500,
    quantityReserved: 20,
    costPrice: 0.22,
    status: 'QUARANTINED',
    branchId: 'b1',
    receivedAt: '2024-04-15',
  },
];

function daysUntilExpiry(dateStr: string): number {
  const exp = new Date(dateStr);
  const now = new Date();
  return Math.floor((exp.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
}

export default function InventoryPage() {
  const [batches, setBatches] = useState<StockBatch[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [lowStockOnly, setLowStockOnly] = useState(false);

  useEffect(() => {
    async function load() {
      setIsLoading(true);
      try {
        const res = await inventoryApi.listBatches({ size: 50 });
        const data = res.data?.content;
        setBatches(Array.isArray(data) ? data : MOCK_BATCHES);
      } catch {
        setBatches(MOCK_BATCHES);
      } finally {
        setIsLoading(false);
      }
    }
    load();
  }, []);

  const filtered = batches.filter((b) => {
    const matchSearch =
      !search ||
      b.productName.toLowerCase().includes(search.toLowerCase()) ||
      b.batchNumber.toLowerCase().includes(search.toLowerCase());
    const matchStatus = statusFilter === 'ALL' || b.status === statusFilter;
    const matchLow = !lowStockOnly || b.quantityOnHand < 50;
    return matchSearch && matchStatus && matchLow;
  });

  const stats = {
    total: batches.length,
    available: batches.filter((b) => b.status === 'AVAILABLE').length,
    expiringSoon: batches.filter((b) => daysUntilExpiry(b.expiresAt) <= 90 && b.status !== 'EXPIRED').length,
    lowStock: batches.filter((b) => b.quantityOnHand < 50 && b.status === 'AVAILABLE').length,
  };

  return (
    <div className="p-6 bg-cream min-h-screen">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-espresso">Inventory</h1>
        <p className="text-coffee text-sm">Stock batches and expiry management</p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        {[
          { label: 'Total Batches', value: stats.total, icon: Package, color: 'text-coffee' },
          { label: 'Available', value: stats.available, icon: CheckCircle, color: 'text-green-600' },
          {
            label: 'Expiring Soon',
            value: stats.expiringSoon,
            icon: Clock,
            color: 'text-amber-600',
          },
          {
            label: 'Low Stock',
            value: stats.lowStock,
            icon: AlertTriangle,
            color: 'text-red-500',
          },
        ].map(({ label, value, icon: Icon, color }) => (
          <div
            key={label}
            className="bg-parchment rounded-xl p-4 shadow-sm border border-mocha/10"
          >
            <div className="flex items-center gap-3">
              <Icon className={`w-5 h-5 ${color}`} />
              <div>
                <p className="text-xs text-coffee">{label}</p>
                <p className="text-xl font-bold text-espresso">{value}</p>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Filters */}
      <div className="bg-parchment rounded-xl shadow-sm border border-mocha/10 p-4 mb-4 flex flex-wrap gap-3 items-center">
        <div className="relative flex-1 min-w-48">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-mocha" />
          <input
            type="text"
            placeholder="Search product or batch…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-9 pr-3 py-2 bg-cream border border-mocha/20 rounded-lg text-sm text-espresso focus:outline-none focus:ring-2 focus:ring-caramel"
          />
        </div>
        <div className="flex items-center gap-2">
          <Filter className="w-4 h-4 text-mocha" />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="bg-cream border border-mocha/20 rounded-lg text-sm text-espresso px-3 py-2 focus:outline-none focus:ring-2 focus:ring-caramel"
          >
            <option value="ALL">All Statuses</option>
            <option value="AVAILABLE">Available</option>
            <option value="QUARANTINED">Quarantined</option>
            <option value="RECALLED">Recalled</option>
            <option value="EXPIRED">Expired</option>
            <option value="DEPLETED">Depleted</option>
          </select>
        </div>
        <label className="flex items-center gap-2 text-sm text-espresso cursor-pointer">
          <input
            type="checkbox"
            checked={lowStockOnly}
            onChange={(e) => setLowStockOnly(e.target.checked)}
            className="accent-caramel w-4 h-4"
          />
          Low Stock Only
        </label>
        <button
          onClick={() => { setSearch(''); setStatusFilter('ALL'); setLowStockOnly(false); }}
          className="flex items-center gap-1 text-sm text-coffee hover:text-espresso transition-colors"
        >
          <RefreshCw className="w-3.5 h-3.5" /> Reset
        </button>
      </div>

      {/* Table */}
      <div className="bg-parchment rounded-xl shadow-sm border border-mocha/10 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-mocha/10 border-b border-mocha/20">
                <th className="text-left px-4 py-3 text-espresso font-semibold">Product</th>
                <th className="text-left px-4 py-3 text-espresso font-semibold">Batch #</th>
                <th className="text-left px-4 py-3 text-espresso font-semibold">Expires</th>
                <th className="text-right px-4 py-3 text-espresso font-semibold">On Hand</th>
                <th className="text-right px-4 py-3 text-espresso font-semibold">Reserved</th>
                <th className="text-right px-4 py-3 text-espresso font-semibold">Unit Cost</th>
                <th className="text-left px-4 py-3 text-espresso font-semibold">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-mocha/10">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="text-center py-12 text-coffee">
                    <RefreshCw className="w-5 h-5 animate-spin inline mr-2" />
                    Loading inventory…
                  </td>
                </tr>
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan={7} className="text-center py-12 text-coffee">
                    No batches found matching your filters.
                  </td>
                </tr>
              ) : (
                filtered.map((batch) => {
                  const days = daysUntilExpiry(batch.expiresAt);
                  const isLow = batch.quantityOnHand < 50 && batch.status === 'AVAILABLE';
                  return (
                    <tr key={batch.id} className={`hover:bg-cream/50 ${isLow ? 'bg-red-50/30' : ''}`}>
                      <td className="px-4 py-3">
                        <div className="font-medium text-espresso">{batch.productName}</div>
                        {isLow && (
                          <span className="text-xs text-red-600 flex items-center gap-0.5 mt-0.5">
                            <AlertTriangle className="w-3 h-3" /> Low stock
                          </span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-coffee font-mono text-xs">{batch.batchNumber}</td>
                      <td className="px-4 py-3">
                        <div className={days < 0 ? 'text-red-600' : days < 90 ? 'text-amber-600' : 'text-coffee'}>
                          {new Date(batch.expiresAt).toLocaleDateString()}
                        </div>
                        {days >= 0 && days < 90 && (
                          <span className="text-xs text-amber-600">{days}d left</span>
                        )}
                        {days < 0 && <span className="text-xs text-red-600">Expired</span>}
                      </td>
                      <td className="px-4 py-3 text-right font-mono text-espresso">
                        {batch.quantityOnHand.toLocaleString()}
                      </td>
                      <td className="px-4 py-3 text-right font-mono text-coffee">
                        {batch.quantityReserved}
                      </td>
                      <td className="px-4 py-3 text-right text-coffee">
                        ${batch.costPrice.toFixed(2)}
                      </td>
                      <td className="px-4 py-3">
                        <span
                          className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${
                            STATUS_STYLES[batch.status] ?? 'bg-gray-100 text-gray-700'
                          }`}
                        >
                          {batch.status === 'AVAILABLE' && <CheckCircle className="w-3 h-3 mr-1" />}
                          {batch.status === 'EXPIRED' && <XCircle className="w-3 h-3 mr-1" />}
                          {batch.status === 'QUARANTINED' && <AlertTriangle className="w-3 h-3 mr-1" />}
                          {batch.status}
                        </span>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
