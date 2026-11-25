'use client'

import { useState } from 'react'
import {
  Users, Plus, Search, Shield, KeyRound,
  CheckCircle, XCircle, Lock, ChevronDown, X, Eye, EyeOff
} from 'lucide-react'

// ── Types ──────────────────────────────────────────────────────────────────
interface User {
  id: string
  username: string
  email: string
  status: 'ACTIVE' | 'DISABLED' | 'LOCKED'
  mfaEnabled: boolean
  roles: string[]
  branch: string
  lastLogin?: string
}

interface PasswordResetRequest {
  id: string
  userId: string
  username: string
  email: string
  requestedAt: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  approvedBy?: string
}

// ── Mock data (replace with real API calls) ────────────────────────────────
const MOCK_USERS: User[] = [
  { id: '1', username: 'sarah.chemist',  email: 'sarah@pharma.ug',   status: 'ACTIVE',    mfaEnabled: true,  roles: ['Pharmacist'],     branch: 'Kampala Central', lastLogin: '2 hours ago' },
  { id: '2', username: 'john.cashier',   email: 'john@pharma.ug',    status: 'ACTIVE',    mfaEnabled: false, roles: ['Cashier'],         branch: 'Kampala Central', lastLogin: '30 min ago'  },
  { id: '3', username: 'alice.manager',  email: 'alice@pharma.ug',   status: 'ACTIVE',    mfaEnabled: true,  roles: ['Branch Manager'],  branch: 'Entebbe Branch',  lastLogin: 'Yesterday'   },
  { id: '4', username: 'bob.store',      email: 'bob@pharma.ug',     status: 'DISABLED',  mfaEnabled: false, roles: ['Storekeeper'],     branch: 'Jinja Branch',    lastLogin: '5 days ago'  },
  { id: '5', username: 'grace.accountant', email: 'grace@pharma.ug', status: 'ACTIVE',    mfaEnabled: true,  roles: ['Accountant'],      branch: 'Head Office',     lastLogin: '1 hour ago'  },
  { id: '6', username: 'peter.pharm',    email: 'peter@pharma.ug',   status: 'LOCKED',    mfaEnabled: true,  roles: ['Pharmacist'],      branch: 'Mbarara Branch',  lastLogin: '2 days ago'  },
]

const MOCK_RESET_REQUESTS: PasswordResetRequest[] = [
  { id: 'r1', userId: '2', username: 'john.cashier',  email: 'john@pharma.ug',  requestedAt: '10 min ago', status: 'PENDING'  },
  { id: 'r2', userId: '4', username: 'bob.store',     email: 'bob@pharma.ug',   requestedAt: '2 hours ago', status: 'APPROVED', approvedBy: 'alice.manager' },
]

// ── Helpers ────────────────────────────────────────────────────────────────
const StatusBadge = ({ status }: { status: User['status'] }) => {
  const styles: Record<User['status'], string> = {
    ACTIVE:   'badge-active',
    DISABLED: 'badge-disabled',
    LOCKED:   'badge-locked',
  }
  return <span className={styles[status]}>{status}</span>
}

const ResetBadge = ({ status }: { status: PasswordResetRequest['status'] }) => {
  const styles = {
    PENDING:  'badge-pending',
    APPROVED: 'badge-active',
    REJECTED: 'badge-locked',
  }
  return <span className={styles[status]}>{status}</span>
}

// ── Password Reset Modal ────────────────────────────────────────────────────
function ResetPasswordModal({
  user,
  onClose,
  onSubmit,
}: {
  user: User
  onClose: () => void
  onSubmit: (userId: string, reason: string) => void
}) {
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!reason.trim()) return
    onSubmit(user.id, reason)
    setSubmitted(true)
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-espresso/60 backdrop-blur-sm">
      <div className="bg-parchment rounded-2xl shadow-2xl w-full max-w-md border border-coffee-200">
        <div className="flex items-center justify-between px-6 py-4 border-b border-coffee-200">
          <div className="flex items-center gap-2">
            <KeyRound className="w-5 h-5 text-coffee-700" />
            <h2 className="font-semibold text-coffee-900">Request Password Reset</h2>
          </div>
          <button onClick={onClose} className="btn-ghost p-1.5 rounded-lg">
            <X className="w-4 h-4" />
          </button>
        </div>

        {submitted ? (
          <div className="p-6 text-center">
            <CheckCircle className="w-12 h-12 text-green-600 mx-auto mb-3" />
            <h3 className="font-semibold text-coffee-900 mb-1">Reset request submitted</h3>
            <p className="text-coffee-500 text-sm">
              A manager must approve this request before the password is reset.
              The user will be notified once approved.
            </p>
            <button onClick={onClose} className="btn-primary mt-5 w-full">Close</button>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="p-6 space-y-4">
            <div className="bg-coffee-50 rounded-lg p-3 border border-coffee-200">
              <div className="text-sm font-medium text-coffee-800">{user.username}</div>
              <div className="text-xs text-coffee-500">{user.email}</div>
            </div>

            <div className="bg-amber-50 border border-amber-200 rounded-lg p-3">
              <div className="flex items-start gap-2">
                <Shield className="w-4 h-4 text-amber-600 mt-0.5 flex-shrink-0" />
                <p className="text-xs text-amber-800">
                  <strong>Manager approval required.</strong> This request will be sent to the branch manager
                  or admin for approval before the password is reset.
                </p>
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-coffee-800 mb-1.5">
                Reason for reset <span className="text-red-500">*</span>
              </label>
              <textarea
                value={reason}
                onChange={e => setReason(e.target.value)}
                placeholder="e.g. User forgot password, locked out after too many attempts…"
                required
                rows={3}
                className="input-field resize-none"
              />
            </div>

            <div className="flex gap-3 pt-2">
              <button type="button" onClick={onClose} className="btn-secondary flex-1">Cancel</button>
              <button
                type="submit"
                disabled={!reason.trim()}
                className="btn-primary flex-1"
              >
                Submit Request
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}

// ── Approve / Reject modal ─────────────────────────────────────────────────
function ApproveResetModal({
  request,
  onClose,
  onDecision,
}: {
  request: PasswordResetRequest
  onClose: () => void
  onDecision: (id: string, approve: boolean) => void
}) {
  const [newPassword, setNewPassword] = useState('')
  const [showPw, setShowPw] = useState(false)
  const [approved, setApproved] = useState(false)

  const handleApprove = (e: React.FormEvent) => {
    e.preventDefault()
    if (!newPassword || newPassword.length < 8) return
    onDecision(request.id, true)
    setApproved(true)
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-espresso/60 backdrop-blur-sm">
      <div className="bg-parchment rounded-2xl shadow-2xl w-full max-w-md border border-coffee-200">
        <div className="flex items-center justify-between px-6 py-4 border-b border-coffee-200">
          <div className="flex items-center gap-2">
            <Shield className="w-5 h-5 text-coffee-700" />
            <h2 className="font-semibold text-coffee-900">Approve Password Reset</h2>
          </div>
          <button onClick={onClose} className="btn-ghost p-1.5 rounded-lg">
            <X className="w-4 h-4" />
          </button>
        </div>

        {approved ? (
          <div className="p-6 text-center">
            <CheckCircle className="w-12 h-12 text-green-600 mx-auto mb-3" />
            <h3 className="font-semibold text-coffee-900 mb-1">Password reset approved</h3>
            <p className="text-coffee-500 text-sm">The user&apos;s password has been updated and they have been notified.</p>
            <button onClick={onClose} className="btn-primary mt-5 w-full">Done</button>
          </div>
        ) : (
          <div className="p-6 space-y-4">
            <div className="bg-coffee-50 rounded-lg p-3 border border-coffee-200">
              <div className="text-sm font-medium text-coffee-800">{request.username}</div>
              <div className="text-xs text-coffee-500">{request.email}</div>
              <div className="text-xs text-coffee-400 mt-1">Requested: {request.requestedAt}</div>
            </div>

            <form onSubmit={handleApprove} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-coffee-800 mb-1.5">
                  New temporary password <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <input
                    type={showPw ? 'text' : 'password'}
                    value={newPassword}
                    onChange={e => setNewPassword(e.target.value)}
                    placeholder="Min. 8 characters"
                    minLength={8}
                    required
                    className="input-field pr-10"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPw(v => !v)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-coffee-400 hover:text-coffee-700"
                  >
                    {showPw ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                <p className="text-xs text-coffee-400 mt-1">The user must change this on next login.</p>
              </div>

              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={() => { onDecision(request.id, false); onClose() }}
                  className="btn-danger flex-1"
                >
                  Reject
                </button>
                <button
                  type="submit"
                  disabled={newPassword.length < 8}
                  className="btn-primary flex-1"
                >
                  Approve & Reset
                </button>
              </div>
            </form>
          </div>
        )}
      </div>
    </div>
  )
}

// ── Main Page ──────────────────────────────────────────────────────────────
export default function UsersPage() {
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('ALL')
  const [resetModal, setResetModal] = useState<User | null>(null)
  const [approveModal, setApproveModal] = useState<PasswordResetRequest | null>(null)
  const [activeTab, setActiveTab] = useState<'users' | 'resets'>('users')
  const [users, setUsers] = useState<User[]>(MOCK_USERS)
  const [resetRequests, setResetRequests] = useState<PasswordResetRequest[]>(MOCK_RESET_REQUESTS)

  // ── FIXED: safe array guard ── users is always User[], never an object with .data
  const userList: User[] = Array.isArray(users) ? users : []

  const filtered = userList.filter(u => {
    const matchSearch = !search ||
      u.username.toLowerCase().includes(search.toLowerCase()) ||
      u.email.toLowerCase().includes(search.toLowerCase()) ||
      u.branch.toLowerCase().includes(search.toLowerCase())
    const matchStatus = statusFilter === 'ALL' || u.status === statusFilter
    return matchSearch && matchStatus
  })

  const pendingResets = resetRequests.filter(r => r.status === 'PENDING').length

  const handleStatusToggle = (userId: string) => {
    setUsers(prev => prev.map(u =>
      u.id === userId
        ? { ...u, status: u.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE' }
        : u
    ))
  }

  const handleResetRequest = (userId: string, _reason: string) => {
    const user = userList.find(u => u.id === userId)
    if (!user) return
    const newReq: PasswordResetRequest = {
      id: `r${Date.now()}`,
      userId,
      username: user.username,
      email: user.email,
      requestedAt: 'Just now',
      status: 'PENDING',
    }
    setResetRequests(prev => [newReq, ...prev])
  }

  const handleResetDecision = (requestId: string, approve: boolean) => {
    setResetRequests(prev => prev.map(r =>
      r.id === requestId
        ? { ...r, status: approve ? 'APPROVED' : 'REJECTED', approvedBy: 'current.manager' }
        : r
    ))
  }

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-coffee-900 flex items-center gap-2">
            <Users className="w-6 h-6" />
            Users &amp; Access
          </h1>
          <p className="text-coffee-500 text-sm mt-1">Manage users, roles, and password resets</p>
        </div>
        <button className="btn-primary flex items-center gap-2">
          <Plus className="w-4 h-4" />
          Add User
        </button>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 p-1 bg-coffee-100 rounded-xl w-fit">
        <button
          onClick={() => setActiveTab('users')}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
            activeTab === 'users'
              ? 'bg-parchment text-coffee-900 shadow-sm'
              : 'text-coffee-600 hover:text-coffee-800'
          }`}
        >
          Users ({userList.length})
        </button>
        <button
          onClick={() => setActiveTab('resets')}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center gap-2 ${
            activeTab === 'resets'
              ? 'bg-parchment text-coffee-900 shadow-sm'
              : 'text-coffee-600 hover:text-coffee-800'
          }`}
        >
          Password Resets
          {pendingResets > 0 && (
            <span className="badge-pending text-xs">{pendingResets}</span>
          )}
        </button>
      </div>

      {activeTab === 'users' && (
        <>
          {/* Filters */}
          <div className="flex flex-col sm:flex-row gap-3">
            <div className="relative flex-1 max-w-sm">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-coffee-400" />
              <input
                type="text"
                value={search}
                onChange={e => setSearch(e.target.value)}
                placeholder="Search by name, email, branch…"
                className="input-field pl-9"
              />
            </div>
            <div className="relative">
              <select
                value={statusFilter}
                onChange={e => setStatusFilter(e.target.value)}
                className="input-field pr-8 appearance-none cursor-pointer"
              >
                <option value="ALL">All Statuses</option>
                <option value="ACTIVE">Active</option>
                <option value="DISABLED">Disabled</option>
                <option value="LOCKED">Locked</option>
              </select>
              <ChevronDown className="absolute right-2.5 top-1/2 -translate-y-1/2 w-4 h-4 text-coffee-400 pointer-events-none" />
            </div>
          </div>

          {/* Users Table */}
          <div className="card overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="table-header">
                    <th className="px-5 py-3 text-left">User</th>
                    <th className="px-5 py-3 text-left">Branch</th>
                    <th className="px-5 py-3 text-left">Role</th>
                    <th className="px-5 py-3 text-left">Status</th>
                    <th className="px-5 py-3 text-left">MFA</th>
                    <th className="px-5 py-3 text-left">Last Login</th>
                    <th className="px-5 py-3 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-coffee-100">
                  {/* FIXED: filtered is always User[], .map is safe */}
                  {filtered.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="px-5 py-10 text-center text-coffee-400">
                        No users found matching your search.
                      </td>
                    </tr>
                  ) : filtered.map((user: User) => (
                    <tr key={user.id} className="table-row">
                      <td className="px-5 py-3">
                        <div className="flex items-center gap-3">
                          <div className="w-8 h-8 rounded-full bg-coffee-200 flex items-center justify-center text-coffee-800 font-semibold text-sm">
                            {user.username[0].toUpperCase()}
                          </div>
                          <div>
                            <div className="font-medium text-coffee-900">{user.username}</div>
                            <div className="text-xs text-coffee-400">{user.email}</div>
                          </div>
                        </div>
                      </td>
                      <td className="px-5 py-3 text-coffee-700">{user.branch}</td>
                      <td className="px-5 py-3">
                        {user.roles.map(r => (
                          <span key={r} className="badge-disabled text-xs mr-1">{r}</span>
                        ))}
                      </td>
                      <td className="px-5 py-3">
                        <StatusBadge status={user.status} />
                      </td>
                      <td className="px-5 py-3">
                        {user.mfaEnabled
                          ? <span className="badge-active text-xs">Enabled</span>
                          : <span className="badge-disabled text-xs">Off</span>
                        }
                      </td>
                      <td className="px-5 py-3 text-coffee-400 text-xs">{user.lastLogin ?? '—'}</td>
                      <td className="px-5 py-3">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => setResetModal(user)}
                            title="Request password reset"
                            className="p-1.5 rounded-lg text-coffee-500 hover:bg-coffee-100 hover:text-coffee-800 transition-colors"
                          >
                            <KeyRound className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => handleStatusToggle(user.id)}
                            title={user.status === 'ACTIVE' ? 'Disable user' : 'Enable user'}
                            className={`p-1.5 rounded-lg transition-colors ${
                              user.status === 'ACTIVE'
                                ? 'text-red-500 hover:bg-red-50 hover:text-red-700'
                                : 'text-green-600 hover:bg-green-50 hover:text-green-800'
                            }`}
                          >
                            {user.status === 'ACTIVE'
                              ? <XCircle className="w-4 h-4" />
                              : <CheckCircle className="w-4 h-4" />
                            }
                          </button>
                          <button
                            title="Lock account"
                            className="p-1.5 rounded-lg text-amber-500 hover:bg-amber-50 hover:text-amber-700 transition-colors"
                          >
                            <Lock className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      {activeTab === 'resets' && (
        <div className="card overflow-hidden">
          <div className="px-5 py-4 border-b border-coffee-200">
            <h2 className="font-semibold text-coffee-900 flex items-center gap-2">
              <Shield className="w-4 h-4" />
              Password Reset Requests
            </h2>
            <p className="text-xs text-coffee-400 mt-1">
              All reset requests require manager approval before the password is changed.
            </p>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="table-header">
                  <th className="px-5 py-3 text-left">User</th>
                  <th className="px-5 py-3 text-left">Requested</th>
                  <th className="px-5 py-3 text-left">Status</th>
                  <th className="px-5 py-3 text-left">Approved By</th>
                  <th className="px-5 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-coffee-100">
                {resetRequests.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="px-5 py-10 text-center text-coffee-400">
                      No password reset requests.
                    </td>
                  </tr>
                ) : resetRequests.map(req => (
                  <tr key={req.id} className="table-row">
                    <td className="px-5 py-3">
                      <div className="font-medium text-coffee-900">{req.username}</div>
                      <div className="text-xs text-coffee-400">{req.email}</div>
                    </td>
                    <td className="px-5 py-3 text-coffee-500">{req.requestedAt}</td>
                    <td className="px-5 py-3"><ResetBadge status={req.status} /></td>
                    <td className="px-5 py-3 text-coffee-500">{req.approvedBy ?? '—'}</td>
                    <td className="px-5 py-3 text-right">
                      {req.status === 'PENDING' && (
                        <button
                          onClick={() => setApproveModal(req)}
                          className="btn-primary py-1.5 px-3 text-xs"
                        >
                          Review
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Modals */}
      {resetModal && (
        <ResetPasswordModal
          user={resetModal}
          onClose={() => setResetModal(null)}
          onSubmit={(userId, reason) => {
            handleResetRequest(userId, reason)
            setActiveTab('resets')
          }}
        />
      )}

      {approveModal && (
        <ApproveResetModal
          request={approveModal}
          onClose={() => setApproveModal(null)}
          onDecision={handleResetDecision}
        />
      )}
    </div>
  )
}
