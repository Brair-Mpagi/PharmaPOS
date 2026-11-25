'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Coffee, Mail, ArrowLeft, CheckCircle } from 'lucide-react';
import Link from 'next/link';
import { authApi } from '@/lib/api';

export default function ForgotPasswordPage() {
  const router = useRouter();
  const [step, setStep] = useState<'form' | 'submitted'>('form');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({ username: '', email: '', reason: '' });

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!form.username || !form.email || !form.reason) {
      setError('All fields are required.');
      return;
    }
    setIsLoading(true);
    setError('');
    try {
      await authApi.requestPasswordReset(form);
      setStep('submitted');
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Failed to submit request. Please try again.';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-espresso via-coffee to-mocha flex flex-col items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-caramel rounded-2xl shadow-lg mb-4">
            <Coffee className="w-9 h-9 text-cream" />
          </div>
          <h1 className="text-2xl font-bold text-cream">PharmaPOS</h1>
          <p className="text-latte text-sm mt-1">Password Reset Request</p>
        </div>

        <div className="bg-parchment rounded-2xl shadow-xl p-8">
          {step === 'form' ? (
            <>
              <div className="mb-6">
                <h2 className="text-xl font-semibold text-espresso">Forgot Password?</h2>
                <p className="text-sm text-coffee mt-1">
                  Submit a request and your manager will reset your password.
                </p>
              </div>

              {error && (
                <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-3 text-sm mb-4">
                  {error}
                </div>
              )}

              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-espresso mb-1">
                    Username
                  </label>
                  <input
                    type="text"
                    value={form.username}
                    onChange={(e) => setForm((f) => ({ ...f, username: e.target.value }))}
                    className="w-full px-3 py-2 bg-cream border border-mocha/30 rounded-lg text-espresso placeholder-mocha/50 focus:outline-none focus:ring-2 focus:ring-caramel focus:border-transparent"
                    placeholder="Your username"
                    autoComplete="username"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-espresso mb-1">
                    Email Address
                  </label>
                  <input
                    type="email"
                    value={form.email}
                    onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
                    className="w-full px-3 py-2 bg-cream border border-mocha/30 rounded-lg text-espresso placeholder-mocha/50 focus:outline-none focus:ring-2 focus:ring-caramel focus:border-transparent"
                    placeholder="you@example.com"
                    autoComplete="email"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-espresso mb-1">
                    Reason for Reset
                  </label>
                  <textarea
                    rows={3}
                    value={form.reason}
                    onChange={(e) => setForm((f) => ({ ...f, reason: e.target.value }))}
                    className="w-full px-3 py-2 bg-cream border border-mocha/30 rounded-lg text-espresso placeholder-mocha/50 focus:outline-none focus:ring-2 focus:ring-caramel focus:border-transparent resize-none"
                    placeholder="Briefly describe why you need a password reset..."
                  />
                </div>

                <button
                  type="submit"
                  disabled={isLoading}
                  className="w-full py-2.5 px-4 bg-caramel hover:bg-coffee text-cream font-semibold rounded-lg transition-colors disabled:opacity-60 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                >
                  {isLoading ? (
                    <>
                      <span className="inline-block w-4 h-4 border-2 border-cream/30 border-t-cream rounded-full animate-spin" />
                      Submitting…
                    </>
                  ) : (
                    <>
                      <Mail className="w-4 h-4" />
                      Submit Reset Request
                    </>
                  )}
                </button>
              </form>
            </>
          ) : (
            <div className="text-center py-4">
              <CheckCircle className="w-16 h-16 text-green-500 mx-auto mb-4" />
              <h2 className="text-xl font-semibold text-espresso mb-2">Request Submitted</h2>
              <p className="text-coffee text-sm mb-6">
                Your password reset request has been sent to your manager for approval. You will
                receive a notification once your password has been reset. Please be patient — your
                manager must review and approve the request before any change is made.
              </p>
              <div className="bg-amber-50 border border-amber-200 rounded-lg p-4 text-left mb-6">
                <p className="text-sm text-amber-800 font-medium">What happens next?</p>
                <ol className="text-sm text-amber-700 mt-2 space-y-1 list-decimal list-inside">
                  <li>Your manager receives your reset request</li>
                  <li>Manager reviews and approves it in the Users dashboard</li>
                  <li>Manager sets a temporary password for you</li>
                  <li>You can log in with the temporary password</li>
                  <li>You will be prompted to change it on first login</li>
                </ol>
              </div>
              <button
                onClick={() => router.push('/login')}
                className="w-full py-2.5 px-4 bg-caramel hover:bg-coffee text-cream font-semibold rounded-lg transition-colors"
              >
                Back to Login
              </button>
            </div>
          )}

          {step === 'form' && (
            <div className="mt-4 text-center">
              <Link
                href="/login"
                className="inline-flex items-center gap-1 text-sm text-coffee hover:text-espresso transition-colors"
              >
                <ArrowLeft className="w-3.5 h-3.5" />
                Back to login
              </Link>
            </div>
          )}
        </div>

        <p className="text-center text-latte/60 text-xs mt-6">
          © {new Date().getFullYear()} PharmaPOS Enterprise. All rights reserved.
        </p>
      </div>
    </div>
  );
}
