'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'

export default function LoginPage() {
  const router = useRouter()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError('')

    try {
      // In production this calls the Keycloak OIDC flow.
      // For demo, simulate a login with mock token storage.
      await new Promise(r => setTimeout(r, 800))

      if (email && password) {
        // Store mock session (replace with real Keycloak auth in production)
        localStorage.setItem('pharmapos_user', JSON.stringify({
          email,
          name: email.split('@')[0],
          role: 'ADMIN',
          tenantId: 'demo-tenant',
        }))
        router.push('/dashboard')
      } else {
        setError('Please enter your email and password.')
      }
    } catch {
      setError('Login failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-coffee-gradient flex items-center justify-center p-4">
      {/* Background coffee beans decoration */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-20 left-10 w-32 h-32 rounded-full bg-coffee-800 opacity-10" />
        <div className="absolute bottom-20 right-20 w-48 h-48 rounded-full bg-coffee-600 opacity-10" />
        <div className="absolute top-1/2 right-10 w-24 h-24 rounded-full bg-coffee-400 opacity-10" />
      </div>

      <div className="w-full max-w-md relative">
        {/* Logo & Title */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-20 h-20 rounded-2xl bg-coffee-800 shadow-xl mb-4">
            <svg className="w-10 h-10 text-cream-200" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z" />
            </svg>
          </div>
          <h1 className="text-3xl font-bold text-cream-50">PharmaPOS</h1>
          <p className="text-cream-300 mt-1 text-sm">Back Office Management System</p>
        </div>

        {/* Login Card */}
        <div className="bg-parchment rounded-2xl shadow-2xl p-8 border border-coffee-200">
          <h2 className="text-xl font-semibold text-coffee-900 mb-6">Sign in to your account</h2>

          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
              {error}
            </div>
          )}

          <form onSubmit={handleLogin} className="space-y-4">
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-coffee-800 mb-1.5">
                Email address
              </label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                placeholder="you@pharmacy.com"
                required
                className="input-field"
                autoComplete="email"
              />
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-coffee-800 mb-1.5">
                Password
              </label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={e => setPassword(e.target.value)}
                placeholder="••••••••"
                required
                className="input-field"
                autoComplete="current-password"
              />
            </div>

            <div className="flex items-center justify-between">
              <label className="flex items-center gap-2 text-sm text-coffee-700 cursor-pointer">
                <input type="checkbox" className="rounded border-coffee-300 text-coffee-600" />
                Remember me
              </label>
              <a href="/forgot-password" className="text-sm text-coffee-600 hover:text-coffee-800 font-medium">
                Forgot password?
              </a>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full btn-primary py-3 text-base font-semibold mt-2"
            >
              {loading ? (
                <span className="flex items-center justify-center gap-2">
                  <svg className="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                  </svg>
                  Signing in…
                </span>
              ) : 'Sign in'}
            </button>
          </form>

          <div className="mt-6 pt-5 border-t border-coffee-200">
            <p className="text-xs text-coffee-500 text-center">
              Password resets require manager approval. Contact your system administrator.
            </p>
          </div>
        </div>

        <p className="text-center text-cream-400 text-xs mt-6">
          © 2026 PharmaPOS · All rights reserved
        </p>
      </div>
    </div>
  )
}
