'use client'

import { Users, Package, ShoppingCart, TrendingUp, AlertTriangle, Clock } from 'lucide-react'

const stats = [
  { label: 'Total Sales Today',    value: 'UGX 2,847,500', icon: ShoppingCart, change: '+12%',  up: true  },
  { label: 'Low Stock Items',      value: '14',             icon: AlertTriangle,change: '-3',    up: false },
  { label: 'Active Users',         value: '28',             icon: Users,        change: '+2',    up: true  },
  { label: 'Pending Orders',       value: '6',              icon: Package,      change: '+1',    up: false },
]

const recentSales = [
  { id: 'S-00891', customer: 'John Mukasa',    items: 3, total: 'UGX 45,000',  time: '2 min ago',  status: 'COMPLETED' },
  { id: 'S-00890', customer: 'Walk-in',        items: 1, total: 'UGX 12,500',  time: '8 min ago',  status: 'COMPLETED' },
  { id: 'S-00889', customer: 'Aisha Namukasa', items: 5, total: 'UGX 128,000', time: '15 min ago', status: 'COMPLETED' },
  { id: 'S-00888', customer: 'Dr. Pharmacy',   items: 2, total: 'UGX 67,000',  time: '22 min ago', status: 'REFUNDED'  },
]

const lowStock = [
  { name: 'Amoxicillin 500mg',   qty: 12, min: 50 },
  { name: 'Metformin 500mg',     qty: 8,  min: 30 },
  { name: 'Paracetamol 1000mg',  qty: 25, min: 100 },
  { name: 'Omeprazole 20mg',     qty: 5,  min: 40 },
]

export default function DashboardPage() {
  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-coffee-900">Dashboard</h1>
        <p className="text-coffee-500 text-sm mt-1">Welcome back! Here&apos;s what&apos;s happening today.</p>
      </div>

      {/* Stat Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        {stats.map(({ label, value, icon: Icon, change, up }) => (
          <div key={label} className="card p-5">
            <div className="flex items-center justify-between mb-3">
              <div className="p-2 bg-coffee-100 rounded-lg">
                <Icon className="w-5 h-5 text-coffee-700" />
              </div>
              <span className={`text-xs font-semibold px-2 py-1 rounded-full ${
                up ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
              }`}>{change}</span>
            </div>
            <div className="text-2xl font-bold text-coffee-900">{value}</div>
            <div className="text-coffee-500 text-xs mt-1">{label}</div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        {/* Recent Sales */}
        <div className="xl:col-span-2 card overflow-hidden">
          <div className="px-5 py-4 border-b border-coffee-200 flex items-center justify-between">
            <h2 className="font-semibold text-coffee-900">Recent Sales</h2>
            <a href="/sales" className="text-xs text-coffee-600 hover:text-coffee-800 font-medium">View all →</a>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="table-header">
                  <th className="px-5 py-3 text-left">Receipt</th>
                  <th className="px-5 py-3 text-left">Customer</th>
                  <th className="px-5 py-3 text-right">Items</th>
                  <th className="px-5 py-3 text-right">Total</th>
                  <th className="px-5 py-3 text-left">Time</th>
                  <th className="px-5 py-3 text-left">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-coffee-100">
                {recentSales.map(s => (
                  <tr key={s.id} className="table-row">
                    <td className="px-5 py-3 font-mono text-coffee-700 font-medium">{s.id}</td>
                    <td className="px-5 py-3 text-coffee-800">{s.customer}</td>
                    <td className="px-5 py-3 text-right text-coffee-600">{s.items}</td>
                    <td className="px-5 py-3 text-right font-semibold text-coffee-900">{s.total}</td>
                    <td className="px-5 py-3 text-coffee-400 flex items-center gap-1">
                      <Clock className="w-3 h-3" />{s.time}
                    </td>
                    <td className="px-5 py-3">
                      <span className={s.status === 'COMPLETED' ? 'badge-active' : 'badge-locked'}>
                        {s.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Low Stock Alerts */}
        <div className="card overflow-hidden">
          <div className="px-5 py-4 border-b border-coffee-200 flex items-center justify-between">
            <h2 className="font-semibold text-coffee-900 flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 text-amber-600" />
              Low Stock Alerts
            </h2>
            <a href="/inventory" className="text-xs text-coffee-600 hover:text-coffee-800 font-medium">View all →</a>
          </div>
          <div className="p-4 space-y-3">
            {lowStock.map(item => (
              <div key={item.name} className="flex items-center justify-between">
                <div className="flex-1 min-w-0 mr-3">
                  <div className="text-sm font-medium text-coffee-800 truncate">{item.name}</div>
                  <div className="text-xs text-coffee-400 mt-0.5">Min stock: {item.min}</div>
                </div>
                <span className="badge-locked text-xs font-bold">{item.qty} left</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
