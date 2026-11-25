'use client';

import { useState } from 'react';
import {
  Settings,
  User,
  Building2,
  Bell,
  Shield,
  Save,
  Eye,
  EyeOff,
  CheckCircle,
} from 'lucide-react';

type TabKey = 'profile' | 'tenant' | 'notifications' | 'security';

const TABS: { key: TabKey; label: string; icon: React.ElementType }[] = [
  { key: 'profile', label: 'My Profile', icon: User },
  { key: 'tenant', label: 'Organisation', icon: Building2 },
  { key: 'notifications', label: 'Notifications', icon: Bell },
  { key: 'security', label: 'Security', icon: Shield },
];

function ProfileTab() {
  const [form, setForm] = useState({
    fullName: 'Alice Mwangi',
    email: 'alice@pharmapos.local',
    phone: '+254 712 345 678',
    language: 'en',
  });
  const [saved, setSaved] = useState(false);

  function handleSave() {
    setSaved(true);
    setTimeout(() => setSaved(false), 3000);
  }

  return (
    <div className="max-w-lg space-y-4">
      <div>
        <label className="block text-sm font-medium text-espresso mb-1">Full Name</label>
        <input
          type="text"
          value={form.fullName}
          onChange={(e) => setForm((f) => ({ ...f, fullName: e.target.value }))}
          className="w-full px-3 py-2 bg-cream border border-mocha/20 rounded-lg text-sm text-espresso focus:outline-none focus:ring-2 focus:ring-caramel"
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-espresso mb-1">Email Address</label>
        <input
          type="email"
          value={form.email}
          onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
          className="w-full px-3 py-2 bg-cream border border-mocha/20 rounded-lg text-sm text-espresso focus:outline-none focus:ring-2 focus:ring-caramel"
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-espresso mb-1">Phone Number</label>
        <input
          type="tel"
          value={form.phone}
          onChange={(e) => setForm((f) => ({ ...f, phone: e.target.value }))}
          className="w-full px-3 py-2 bg-cream border border-mocha/20 rounded-lg text-sm text-espresso focus:outline-none focus:ring-2 focus:ring-caramel"
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-espresso mb-1">Language</label>
        <select
          value={form.language}
          onChange={(e) => setForm((f) => ({ ...f, language: e.target.value }))}
          className="w-full px-3 py-2 bg-cream border border-mocha/20 rounded-lg text-sm text-espresso focus:outline-none focus:ring-2 focus:ring-caramel"
        >
          <option value="en">English</option>
          <option value="sw">Swahili</option>
          <option value="fr">French</option>
        </select>
      </div>
      <button
        onClick={handleSave}
        className="flex items-center gap-2 px-4 py-2 bg-caramel hover:bg-coffee text-cream text-sm font-medium rounded-lg transition-colors"
      >
        {saved ? (
          <>
            <CheckCircle className="w-4 h-4" />
            Saved!
          </>
        ) : (
          <>
            <Save className="w-4 h-4" />
            Save Changes
          </>
        )}
      </button>
    </div>
  );
}

function TenantTab() {
  const [form, setForm] = useState({
    name: 'City Pharmacy Ltd.',
    registrationNo: 'CPH-2020-001',
    address: '123 Main Street, Nairobi',
    phone: '+254 20 123 4567',
    email: 'info@citypharmacy.co.ke',
    currency: 'KES',
    timezone: 'Africa/Nairobi',
  });
  const [saved, setSaved] = useState(false);

  return (
    <div className="max-w-lg space-y-4">
      <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 text-sm text-amber-800 mb-2">
        Organisation details are managed centrally. Some fields require platform admin access to change.
      </div>
      {[
        { key: 'name', label: 'Organisation Name', type: 'text' },
        { key: 'registrationNo', label: 'Registration Number', type: 'text' },
        { key: 'address', label: 'Address', type: 'text' },
        { key: 'phone', label: 'Phone', type: 'tel' },
        { key: 'email', label: 'Email', type: 'email' },
      ].map(({ key, label, type }) => (
        <div key={key}>
          <label className="block text-sm font-medium text-espresso mb-1">{label}</label>
          <input
            type={type}
            value={(form as Record<string, string>)[key]}
            onChange={(e) =>
              setForm((f) => ({ ...f, [key]: e.target.value }))
            }
            className="w-full px-3 py-2 bg-cream border border-mocha/20 rounded-lg text-sm text-espresso focus:outline-none focus:ring-2 focus:ring-caramel"
          />
        </div>
      ))}
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-espresso mb-1">Currency</label>
          <select
            value={form.currency}
            onChange={(e) => setForm((f) => ({ ...f, currency: e.target.value }))}
            className="w-full px-3 py-2 bg-cream border border-mocha/20 rounded-lg text-sm text-espresso focus:outline-none focus:ring-2 focus:ring-caramel"
          >
            <option value="KES">KES — Kenyan Shilling</option>
            <option value="USD">USD — US Dollar</option>
            <option value="UGX">UGX — Ugandan Shilling</option>
            <option value="TZS">TZS — Tanzanian Shilling</option>
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium text-espresso mb-1">Time Zone</label>
          <select
            value={form.timezone}
            onChange={(e) => setForm((f) => ({ ...f, timezone: e.target.value }))}
            className="w-full px-3 py-2 bg-cream border border-mocha/20 rounded-lg text-sm text-espresso focus:outline-none focus:ring-2 focus:ring-caramel"
          >
            <option value="Africa/Nairobi">Africa/Nairobi (EAT +3)</option>
            <option value="Africa/Lagos">Africa/Lagos (WAT +1)</option>
            <option value="Africa/Johannesburg">Africa/Johannesburg (SAST +2)</option>
            <option value="UTC">UTC</option>
          </select>
        </div>
      </div>
      <button
        onClick={() => setSaved(true)}
        className="flex items-center gap-2 px-4 py-2 bg-caramel hover:bg-coffee text-cream text-sm font-medium rounded-lg transition-colors"
      >
        {saved ? <><CheckCircle className="w-4 h-4" /> Saved!</> : <><Save className="w-4 h-4" /> Save Changes</>}
      </button>
    </div>
  );
}

function NotificationsTab() {
  const [prefs, setPrefs] = useState({
    lowStockAlerts: true,
    expiryAlerts: true,
    poApprovals: true,
    passwordResets: true,
    shiftSummary: false,
    emailNotifications: true,
    smsNotifications: false,
  });

  const togglePref = (key: keyof typeof prefs) =>
    setPrefs((p) => ({ ...p, [key]: !p[key] }));

  return (
    <div className="max-w-lg space-y-4">
      <p className="text-sm text-coffee">Choose which events trigger notifications for you.</p>
      {[
        { key: 'lowStockAlerts', label: 'Low Stock Alerts', desc: 'When a batch drops below reorder level' },
        { key: 'expiryAlerts', label: 'Expiry Alerts', desc: 'Batches expiring within 90 days' },
        { key: 'poApprovals', label: 'PO Approval Requests', desc: 'When a purchase order needs your approval' },
        { key: 'passwordResets', label: 'Password Reset Requests', desc: 'When a user requests a password reset (managers)' },
        { key: 'shiftSummary', label: 'Daily Shift Summary', desc: 'End-of-day shift totals emailed to you' },
      ].map(({ key, label, desc }) => (
        <div
          key={key}
          className="flex items-center justify-between p-3 bg-cream rounded-lg border border-mocha/10"
        >
          <div>
            <p className="text-sm font-medium text-espresso">{label}</p>
            <p className="text-xs text-coffee">{desc}</p>
          </div>
          <button
            onClick={() => togglePref(key as keyof typeof prefs)}
            className={`relative inline-flex h-6 w-11 rounded-full transition-colors ${
              prefs[key as keyof typeof prefs] ? 'bg-caramel' : 'bg-mocha/20'
            }`}
          >
            <span
              className={`inline-block h-4 w-4 rounded-full bg-cream shadow transform transition-transform mt-1 ${
                prefs[key as keyof typeof prefs] ? 'translate-x-6' : 'translate-x-1'
              }`}
            />
          </button>
        </div>
      ))}

      <div className="border-t border-mocha/10 pt-4">
        <p className="text-sm font-medium text-espresso mb-3">Delivery Channels</p>
        {[
          { key: 'emailNotifications', label: 'Email Notifications' },
          { key: 'smsNotifications', label: 'SMS Notifications' },
        ].map(({ key, label }) => (
          <div
            key={key}
            className="flex items-center justify-between p-3 bg-cream rounded-lg border border-mocha/10 mb-2"
          >
            <p className="text-sm font-medium text-espresso">{label}</p>
            <button
              onClick={() => togglePref(key as keyof typeof prefs)}
              className={`relative inline-flex h-6 w-11 rounded-full transition-colors ${
                prefs[key as keyof typeof prefs] ? 'bg-caramel' : 'bg-mocha/20'
              }`}
            >
              <span
                className={`inline-block h-4 w-4 rounded-full bg-cream shadow transform transition-transform mt-1 ${
                  prefs[key as keyof typeof prefs] ? 'translate-x-6' : 'translate-x-1'
                }`}
              />
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}

function SecurityTab() {
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [passwords, setPasswords] = useState({ current: '', newPw: '', confirm: '' });
  const [mfaEnabled, setMfaEnabled] = useState(false);
  const [message, setMessage] = useState('');

  function handleChangePassword() {
    if (!passwords.current || !passwords.newPw || !passwords.confirm) {
      setMessage('All fields are required.');
      return;
    }
    if (passwords.newPw !== passwords.confirm) {
      setMessage('New passwords do not match.');
      return;
    }
    if (passwords.newPw.length < 8) {
      setMessage('Password must be at least 8 characters.');
      return;
    }
    setMessage('Password changed successfully.');
    setPasswords({ current: '', newPw: '', confirm: '' });
    setTimeout(() => setMessage(''), 4000);
  }

  return (
    <div className="max-w-lg space-y-6">
      {/* Change Password */}
      <div className="bg-cream rounded-xl border border-mocha/10 p-5">
        <h3 className="font-semibold text-espresso mb-4">Change Password</h3>
        {message && (
          <div
            className={`rounded-lg p-3 text-sm mb-4 ${
              message.includes('successfully')
                ? 'bg-green-50 border border-green-200 text-green-800'
                : 'bg-red-50 border border-red-200 text-red-700'
            }`}
          >
            {message}
          </div>
        )}
        <div className="space-y-3">
          {[
            { key: 'current', label: 'Current Password', show: showCurrent, toggle: () => setShowCurrent((v) => !v) },
            { key: 'newPw', label: 'New Password', show: showNew, toggle: () => setShowNew((v) => !v) },
            { key: 'confirm', label: 'Confirm New Password', show: showConfirm, toggle: () => setShowConfirm((v) => !v) },
          ].map(({ key, label, show, toggle }) => (
            <div key={key}>
              <label className="block text-sm font-medium text-espresso mb-1">{label}</label>
              <div className="relative">
                <input
                  type={show ? 'text' : 'password'}
                  value={(passwords as Record<string, string>)[key]}
                  onChange={(e) => setPasswords((p) => ({ ...p, [key]: e.target.value }))}
                  className="w-full px-3 py-2 pr-10 bg-parchment border border-mocha/20 rounded-lg text-sm text-espresso focus:outline-none focus:ring-2 focus:ring-caramel"
                />
                <button
                  type="button"
                  onClick={toggle}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-mocha hover:text-espresso"
                >
                  {show ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>
          ))}
        </div>
        <button
          onClick={handleChangePassword}
          className="mt-4 flex items-center gap-2 px-4 py-2 bg-caramel hover:bg-coffee text-cream text-sm font-medium rounded-lg transition-colors"
        >
          <Shield className="w-4 h-4" />
          Update Password
        </button>
      </div>

      {/* MFA */}
      <div className="bg-cream rounded-xl border border-mocha/10 p-5">
        <div className="flex items-center justify-between mb-2">
          <div>
            <h3 className="font-semibold text-espresso">Two-Factor Authentication</h3>
            <p className="text-xs text-coffee mt-0.5">
              {mfaEnabled ? 'MFA is enabled for your account.' : 'Recommended: add extra security to your account.'}
            </p>
          </div>
          <button
            onClick={() => setMfaEnabled((v) => !v)}
            className={`relative inline-flex h-6 w-11 rounded-full transition-colors ${
              mfaEnabled ? 'bg-caramel' : 'bg-mocha/20'
            }`}
          >
            <span
              className={`inline-block h-4 w-4 rounded-full bg-cream shadow transform transition-transform mt-1 ${
                mfaEnabled ? 'translate-x-6' : 'translate-x-1'
              }`}
            />
          </button>
        </div>
        {mfaEnabled && (
          <p className="text-sm text-green-700 bg-green-50 border border-green-200 rounded-lg p-3 mt-2">
            MFA is active. Use your authenticator app to verify logins. To configure your TOTP device, scan the QR code the next time you log in.
          </p>
        )}
      </div>

      {/* Active Sessions */}
      <div className="bg-cream rounded-xl border border-mocha/10 p-5">
        <h3 className="font-semibold text-espresso mb-3">Active Sessions</h3>
        <div className="space-y-2">
          {[
            { device: 'Chrome on Windows 11', location: 'Nairobi, KE', current: true },
            { device: 'Safari on iPhone 15', location: 'Nairobi, KE', current: false },
          ].map((session, i) => (
            <div
              key={i}
              className="flex items-center justify-between p-3 bg-parchment rounded-lg border border-mocha/10"
            >
              <div>
                <p className="text-sm font-medium text-espresso">
                  {session.device}
                  {session.current && (
                    <span className="ml-2 text-xs bg-green-100 text-green-800 px-1.5 py-0.5 rounded-full">
                      Current
                    </span>
                  )}
                </p>
                <p className="text-xs text-coffee">{session.location}</p>
              </div>
              {!session.current && (
                <button className="text-xs text-red-600 hover:text-red-800 transition-colors">
                  Revoke
                </button>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default function SettingsPage() {
  const [activeTab, setActiveTab] = useState<TabKey>('profile');

  const tabComponents: Record<TabKey, React.ReactNode> = {
    profile: <ProfileTab />,
    tenant: <TenantTab />,
    notifications: <NotificationsTab />,
    security: <SecurityTab />,
  };

  return (
    <div className="p-6 bg-cream min-h-screen">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-espresso">Settings</h1>
        <p className="text-coffee text-sm">Manage your profile, organisation, and preferences</p>
      </div>

      <div className="flex flex-col md:flex-row gap-6">
        {/* Tab nav */}
        <div className="md:w-48 flex-shrink-0">
          <nav className="bg-parchment rounded-xl shadow-sm border border-mocha/10 overflow-hidden">
            {TABS.map(({ key, label, icon: Icon }) => (
              <button
                key={key}
                onClick={() => setActiveTab(key)}
                className={`w-full flex items-center gap-3 px-4 py-3 text-sm font-medium transition-colors border-l-4 ${
                  activeTab === key
                    ? 'bg-caramel/10 border-caramel text-espresso'
                    : 'border-transparent text-coffee hover:bg-cream hover:text-espresso'
                }`}
              >
                <Icon className="w-4 h-4 flex-shrink-0" />
                {label}
              </button>
            ))}
          </nav>
        </div>

        {/* Tab content */}
        <div className="flex-1 bg-parchment rounded-xl shadow-sm border border-mocha/10 p-6">
          <div className="flex items-center gap-2 mb-6 pb-4 border-b border-mocha/10">
            {(() => {
              const tab = TABS.find((t) => t.key === activeTab)!;
              const Icon = tab.icon;
              return (
                <>
                  <Icon className="w-5 h-5 text-caramel" />
                  <h2 className="text-lg font-semibold text-espresso">{tab.label}</h2>
                </>
              );
            })()}
          </div>
          {tabComponents[activeTab]}
        </div>
      </div>
    </div>
  );
}
