'use client';

import { useState, useEffect } from 'react';
import {
  ShoppingCart,
  Search,
  Calendar,
  Receipt,
  TrendingUp,
  RefreshCw,
} from 'lucide-react';
import { salesApi, Sale } from '@/lib/api';

const STATUS_STYLES: Record<string, string> = {
  DRAFT: 'bg-gray-100 text-gray-700',
  FINALISED: 'bg-green-100 text-green-800',
  VOIDED: 'bg-red-100 text-red-700',
  REFUNDED: 'bg-amber-100 text-amber-800',
};

const MOCK_SALES: Sale[] = [
  {
    id: '1',
    receiptNumber: 'RCP-00001',
    customerName: 'John Doe',
    total: 45.60,
    discount: 2.00,
    tax: 4.36,
    netTotal: 47.96,
    paymentMethod: 'CASH',
    status: 'FINALISED',
    cashierId: 'u1',
    cashierName: 'Alice M.',
    shiftId: 's1',
    createdAt: new Date(Date.now() - 3600000).toISOString(),
    items: [],
  },
  {
    id: '2',
    receiptNumber: 'RCP-00002',
    customerName: 'Jane Smith',
    total: 120.00,
    discount: 0,
    tax: 12.00,
    netTotal: 132.00,
    paymentMethod: 'CARD',
    status: 'FINALISED',
    cashierId: 'u1',
    cashierName: 'Alice M.',
    shiftId: 's1',
    createdAt: new Date(Date.now() - 7200000).toISOString(),
    items: [],
  },
  {
    id: '3',
    receiptNumber: 'RCP-00003',
    total: 15.75,
    discount: 0,
    tax: 1.58,
    netTotal: 17.33,
    paymentMethod: 'MPESA',
    status: 'VOIDED',
    cashierId: 'u2',
    cashierName: 'Bob K.',
    shiftId: 's1',
    createdAt: new Date(Date.now() - 10800000).toISOString(),
    items: [],
  },
];

export default function SalesPage() {
  const [sales, setSales] = useState<Sale[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');

  useEffect(() => {
    async function load() {
      setIsLoading(true);
      try {
        const res = await salesApi.list({ size: 50 });
        const data = res.data?.content;
        setSales(Array.isArray(data) ? data : MOCK_SALES);
      } catch {
        setSales(MOCK_SALES);
      } finally {
        setIsLoading(false);
      }
    }
    load();
  }, []);

  const filtered = sales.filter((s) => {
    const matchSearch =
      !search ||
      s.receiptNumber.toLowerCase().includes(search.toLowerCase()) ||
      (s.customerName ?? '').toLowerCase().includes(search.toLowerCase()) ||
      s.cashierName.toLowerCase().includes(search.toLowerCase());
    const matchStatus = statusFilter === 'ALL' || s.status === statusFilter;
    return matchSearch && matchStatus;
  });

  const totals = filtered.reduce(
    (acc, s) => ({
      gross: acc.gross + s.total,
      net: acc.net + s.netTotal,
      count: acc.count + 1,
    }),
    { gross: 0, net: 0, count: 0 }
  );

  return (
    <div className="p-6 bg-cream min-h-screen">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-espresso">Sales</h1>
        <p className="text-coffee text-sm">Transaction history and receipts</p>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        {[
          {
            label: 'Total Transactions',
            value: totals.count,
            icon: Receipt,
            format: (v: number) => v.toString(),
          },
          {
            label: 'Gross Sales',
            value: totals.gross,
            icon: ShoppingCart,
            format: (v: number) => `$${v.toFixed(2)}`,
          },
          {
            label: 'Net Revenue',
            value: totals.net,
            icon: TrendingUp,
            format: (v: number) => `$${v.toFixed(2)}`,
          },
        ].map(({ label, value, icon: Icon, format }) => (
          <div
            key={label}
            className="bg-parchment rounded-xl p-5 shadow-sm border border-mocha/10"
          >
            <div className="flex items-center gap-3">
              <div className="p-2 bg-caramel/20 rounded-lg">
                <Icon className="w-5 h-5 text-caramel" />
              </div>
              <div>
                <p className="text-xs text-coffee">{label}</p>
                <p className="text-2xl font-bold text-espresso">{format(value)}</p>
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
            placeholder="Search receipt, customer, cashier…"
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
          <option value="FINALISED">Finalised</option>
          <option value="DRAFT">Draft</option>
          <option value="VOIDED">Voided</option>
          <option value="REFUNDED">Refunded</option>
        </select>
        <div className="flex items-center gap-2">
          <Calendar className="w-4 h-4 text-mocha" />
          <input
            type="date"
            value={dateFrom}
            onChange={(e) => setDateFrom(e.target.value)}
            className="bg-cream border border-mocha/20 rounded-lg text-sm text-espresso px-3 py-2 focus:outline-none focus:ring-2 focus:ring-caramel"
          />
          <span className="text-coffee text-sm">–</span>
          <input
            type="date"
            value={dateTo}
            onChange={(e) => setDateTo(e.target.value)}
            className="bg-cream border border-mocha/20 rounded-lg text-sm text-espresso px-3 py-2 focus:outline-none focus:ring-2 focus:ring-caramel"
          />
        </div>
      </div>

      {/* Table */}
      <div className="bg-parchment rounded-xl shadow-sm border border-mocha/10 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-mocha/10 border-b border-mocha/20">
                <th className="text-left px-4 py-3 text-espresso font-semibold">Receipt #</th>
                <th className="text-left px-4 py-3 text-espresso font-semibold">Customer</th>
                <th className="text-left px-4 py-3 text-espresso font-semibold">Cashier</th>
                <th className="text-left px-4 py-3 text-espresso font-semibold">Date & Time</th>
                <th className="text-right px-4 py-3 text-espresso font-semibold">Gross</th>
                <th className="text-right px-4 py-3 text-espresso font-semibold">Net</th>
                <th className="text-left px-4 py-3 text-espresso font-semibold">Payment</th>
                <th className="text-left px-4 py-3 text-espresso font-semibold">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-mocha/10">
              {isLoading ? (
                <tr>
                  <td colSpan={8} className="text-center py-12 text-coffee">
                    <RefreshCw className="w-5 h-5 animate-spin inline mr-2" />
                    Loading sales…
                  </td>
                </tr>
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan={8} className="text-center py-12 text-coffee">
                    No sales found.
                  </td>
                </tr>
              ) : (
                filtered.map((sale) => (
                  <tr key={sale.id} className="hover:bg-cream/50">
                    <td className="px-4 py-3 font-mono text-xs font-medium text-espresso">
                      {sale.receiptNumber}
                    </td>
                    <td className="px-4 py-3 text-coffee">{sale.customerName ?? '—'}</td>
                    <td className="px-4 py-3 text-coffee">{sale.cashierName}</td>
                    <td className="px-4 py-3 text-coffee text-xs">
                      {new Date(sale.createdAt).toLocaleString()}
                    </td>
                    <td className="px-4 py-3 text-right font-mono text-espresso">
                      ${sale.total.toFixed(2)}
                    </td>
                    <td className="px-4 py-3 text-right font-mono text-espresso font-semibold">
                      ${sale.netTotal.toFixed(2)}
                    </td>
                    <td className="px-4 py-3 text-coffee text-xs">{sale.paymentMethod}</td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${
                          STATUS_STYLES[sale.status] ?? 'bg-gray-100 text-gray-700'
                        }`}
                      >
                        {sale.status}
                      </span>
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
