'use client'

import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import {
  LayoutDashboard,
  Users,
  Package,
  ShoppingCart,
  FileText,
  TrendingUp,
  Settings,
  LogOut,
  ChevronRight,
  Pill,
} from 'lucide-react'

const navItems = [
  { href: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { href: '/users',     label: 'Users & Roles', icon: Users },
  { href: '/inventory', label: 'Inventory', icon: Package },
  { href: '/sales',     label: 'Sales', icon: ShoppingCart },
  { href: '/reports',   label: 'Reports', icon: TrendingUp },
  { href: '/procurement', label: 'Procurement', icon: FileText },
]

export default function Sidebar() {
  const pathname = usePathname()
  const router = useRouter()

  const handleLogout = () => {
    localStorage.removeItem('pharmapos_user')
    router.push('/login')
  }

  return (
    <aside className="w-64 min-h-screen bg-espresso flex flex-col shadow-xl">
      {/* Logo */}
      <div className="flex items-center gap-3 px-5 py-5 border-b border-coffee-800">
        <div className="flex items-center justify-center w-9 h-9 rounded-lg bg-coffee-600">
          <Pill className="w-5 h-5 text-cream-200" />
        </div>
        <div>
          <div className="text-cream-100 font-bold text-base leading-tight">PharmaPOS</div>
          <div className="text-coffee-400 text-xs">Back Office</div>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-3 py-4 space-y-1">
        {navItems.map(({ href, label, icon: Icon }) => {
          const active = pathname === href || (href !== '/dashboard' && pathname.startsWith(href))
          return (
            <Link
              key={href}
              href={href}
              className={active ? 'sidebar-link-active' : 'sidebar-link'}
            >
              <Icon className="w-4 h-4 flex-shrink-0" />
              <span className="flex-1">{label}</span>
              {active && <ChevronRight className="w-3.5 h-3.5 opacity-60" />}
            </Link>
          )
        })}
      </nav>

      {/* Bottom */}
      <div className="px-3 py-4 border-t border-coffee-800 space-y-1">
        <Link href="/settings" className="sidebar-link">
          <Settings className="w-4 h-4 flex-shrink-0" />
          Settings
        </Link>
        <button
          onClick={handleLogout}
          className="w-full sidebar-link text-left text-red-300 hover:bg-red-900/30 hover:text-red-200"
        >
          <LogOut className="w-4 h-4 flex-shrink-0" />
          Sign out
        </button>
      </div>
    </aside>
  )
}
