'use client';

import { useState, useEffect } from 'react';
import {
  FileText,
  Download,
  BarChart2,
  TrendingUp,
  Package,
  DollarSign,
  RefreshCw,
  Calendar,
} from 'lucide-react';
import { reportsApi } from '@/lib/api';

interface ReportCard {
  id: string;
  title: string;
  description: string;
  icon: React.ElementType;
  category: string;
  reportType: string;
}

const REPORTS: ReportCard[] = [
  {
    id: 'sales-summary',
    title: 'Sales Summary',
    description: 'Daily/weekly/monthly breakdown of sales totals, quantities, and payment methods.',
    icon: TrendingUp,
    category: 'Sales',
    reportType: 'sales-summary',
  },
  {
    id: 'shift-report',
    title: 'Shift Report',
    description: 'Per-cashier shift totals, discounts given, and voids.',
    icon: BarChart2,
    category: 'Sales',
    reportType: 'shift-report',
  },
  {
    id: 'inventory-valuation',
    title: 'Inventory Valuation',
    description: 'Current stock value by category, batch status, and expiry range.',
    icon: Package,
    category: 'Inventory',
    reportType: 'inventory-valuation',
  },
  {
    id: 'expiry-report',
    title: 'Expiry Report',
    description: 'Batches expiring within 30, 60, and 90 days with stock quantities.',
    icon: Calendar,
    category: 'Inventory',
    reportType: 'expiry-report',
  },
  {
    id: 'procurement-report',
    title: 'Procurement Report',
    description: 'Purchase orders by supplier, status, and cost for the selected period.',
    icon: FileText,
    category: 'Procurement',
    reportType: 'procurement-report',
  },
  {
    id: 'financial-summary',
    title: 'Financial Summary',
    description: 'Revenue, COGS, gross margin, and expenses for the period.',
    icon: DollarSign,
    category: 'Finance',
    reportType: 'financial-summary',
  },
];

const CATEGORIES = ['All', 'Sales', 'Inventory', 'Procurement', 'Finance'];

export default function ReportsPage() {
  const [category, setCategory] = useState('All');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [downloading, setDownloading] = useState<string | null>(null);
  const [message, setMessage] = useState('');

  // Default date range = last 30 days
  useEffect(() => {
    const now = new Date();
    const to = now.toISOString().split('T')[0];
    const from = new Date(now.setDate(now.getDate() - 30)).toISOString().split('T')[0];
    setDateFrom(from);
    setDateTo(to);
  }, []);

  async function handleDownload(report: ReportCard) {
    setDownloading(report.id);
    setMessage('');
    try {
      const res = await reportsApi.export(report.reportType, {
        dateFrom,
        dateTo,
        format: 'csv',
      });
      // Trigger browser download
      const url = URL.createObjectURL(res.data as Blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${report.reportType}-${dateFrom}-to-${dateTo}.csv`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
      setMessage(`${report.title} downloaded successfully.`);
    } catch {
      // If backend not available, show info
      setMessage(
        `${report.title} export is not available — the backend is not running. Start the API server and try again.`
      );
    } finally {
      setDownloading(null);
      setTimeout(() => setMessage(''), 5000);
    }
  }

  const filtered =
    category === 'All' ? REPORTS : REPORTS.filter((r) => r.category === category);

  return (
    <div className="p-6 bg-cream min-h-screen">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-espresso">Reports</h1>
        <p className="text-coffee text-sm">Generate and download business reports</p>
      </div>

      {/* Date Range + Category Filter */}
      <div className="bg-parchment rounded-xl shadow-sm border border-mocha/10 p-4 mb-6 flex flex-wrap gap-4 items-center">
        <div className="flex items-center gap-2">
          <Calendar className="w-4 h-4 text-mocha" />
          <label className="text-sm text-coffee">From:</label>
          <input
            type="date"
            value={dateFrom}
            onChange={(e) => setDateFrom(e.target.value)}
            className="bg-cream border border-mocha/20 rounded-lg text-sm text-espresso px-3 py-2 focus:outline-none focus:ring-2 focus:ring-caramel"
          />
          <label className="text-sm text-coffee">To:</label>
          <input
            type="date"
            value={dateTo}
            onChange={(e) => setDateTo(e.target.value)}
            className="bg-cream border border-mocha/20 rounded-lg text-sm text-espresso px-3 py-2 focus:outline-none focus:ring-2 focus:ring-caramel"
          />
        </div>

        <div className="flex gap-2 ml-auto flex-wrap">
          {CATEGORIES.map((cat) => (
            <button
              key={cat}
              onClick={() => setCategory(cat)}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                category === cat
                  ? 'bg-caramel text-cream'
                  : 'bg-cream text-coffee hover:bg-mocha/10'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>
      </div>

      {message && (
        <div
          className={`rounded-lg p-3 text-sm mb-4 ${
            message.includes('successfully')
              ? 'bg-green-50 border border-green-200 text-green-800'
              : 'bg-amber-50 border border-amber-200 text-amber-800'
          }`}
        >
          {message}
        </div>
      )}

      {/* Report Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {filtered.map((report) => {
          const Icon = report.icon;
          const isDownloading = downloading === report.id;
          return (
            <div
              key={report.id}
              className="bg-parchment rounded-xl shadow-sm border border-mocha/10 p-5 flex flex-col gap-4"
            >
              <div className="flex items-start gap-3">
                <div className="p-2.5 bg-caramel/15 rounded-lg flex-shrink-0">
                  <Icon className="w-5 h-5 text-caramel" />
                </div>
                <div>
                  <h3 className="font-semibold text-espresso">{report.title}</h3>
                  <span className="text-xs bg-mocha/10 text-mocha px-2 py-0.5 rounded-full">
                    {report.category}
                  </span>
                </div>
              </div>
              <p className="text-sm text-coffee flex-1">{report.description}</p>
              <button
                onClick={() => handleDownload(report)}
                disabled={isDownloading || !dateFrom || !dateTo}
                className="flex items-center justify-center gap-2 py-2 px-4 bg-caramel hover:bg-coffee text-cream text-sm font-medium rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isDownloading ? (
                  <>
                    <RefreshCw className="w-4 h-4 animate-spin" />
                    Generating…
                  </>
                ) : (
                  <>
                    <Download className="w-4 h-4" />
                    Download CSV
                  </>
                )}
              </button>
            </div>
          );
        })}
      </div>
    </div>
  );
}
