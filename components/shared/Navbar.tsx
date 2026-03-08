'use client';

import Link from 'next/link';
import { Menu, X, LogOut } from 'lucide-react';
import { useState } from 'react';
import { AppLanguage, useI18n } from './LanguageProvider';

interface User {
  email: string;
  fullName: string;
  type: 'driver' | 'user';
}

export const Navbar = () => {
  const { language, setLanguage, t } = useI18n();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [user, setUser] = useState<User | null>(() => {
    if (typeof window === 'undefined') return null;
    const storedUser = localStorage.getItem('currentUser');
    if (!storedUser) return null;
    try {
      return JSON.parse(storedUser) as User;
    } catch (err) {
      console.error('Error parsing user data:', err);
      return null;
    }
  });

  const handleLogout = () => {
    localStorage.removeItem('currentUser');
    setUser(null);
    window.location.href = '/';
  };

  const languageOptions: { value: AppLanguage; label: string }[] = [
    { value: 'en', label: 'English' },
    { value: 'te', label: 'తెలుగు' },
    { value: 'hi', label: 'हिंदी' },
    { value: 'mr', label: 'मराठी' },
  ];

  return (
    <nav className="sticky top-0 z-50 bg-gradient-to-r from-gray-950 to-black border-b border-white/10 backdrop-blur-lg">
      <div className="w-full px-4 sm:px-6 lg:px-8 xl:px-10 2xl:px-14">
        <div className="grid grid-cols-[auto_1fr_auto] items-center h-20 gap-4">
          <Link
            href="/"
            className="flex items-center gap-3 font-black text-white hover:text-orange-400 transition shrink-0 min-w-fit"
          >
            <div className="w-11 h-11 bg-white rounded-full flex items-center justify-center">
              <span className="text-black text-base font-black">S</span>
            </div>
            <span className="hidden sm:inline text-xl whitespace-nowrap">SARATHI</span>
          </Link>

          <div className="hidden 2xl:flex items-center justify-start gap-6 min-w-0 pl-2">
            <Link href="/" className="text-gray-300 hover:text-white transition font-semibold">{t('nav.home')}</Link>
            <Link href="/driver-login" className="text-gray-300 hover:text-white transition font-semibold">{t('nav.driver')}</Link>
            <Link href="/public-sos" className="text-gray-300 hover:text-white transition font-semibold">{t('nav.emergency')}</Link>
            <Link href="/hospital-login" className="text-gray-300 hover:text-white transition font-semibold">{t('nav.hospital')}</Link>
            <Link href="/police-login" className="text-gray-300 hover:text-white transition font-semibold">{t('nav.police')}</Link>
          </div>

          <div className="hidden 2xl:flex items-center gap-3 shrink-0">
            <div className="flex items-center gap-2 px-3 py-2 bg-white/10 rounded-lg border border-white/20">
              <span className="text-xs text-gray-300">{t('lang.label')}</span>
              <select
                value={language}
                onChange={(event) => setLanguage(event.target.value as AppLanguage)}
                className="bg-transparent text-white text-sm outline-none"
              >
                {languageOptions.map((option) => (
                  <option key={option.value} value={option.value} className="bg-slate-900">
                    {option.label}
                  </option>
                ))}
              </select>
            </div>
            {user ? (
              <>
                <div className="flex items-center gap-3 px-4 py-2 bg-white/10 rounded-lg border border-white/20">
                  <div className="flex flex-col">
                    <span className="text-xs text-gray-400">{t('nav.loggedIn')}</span>
                    <span className="text-white font-semibold">{user.fullName}</span>
                  </div>
                </div>
                <button
                  onClick={handleLogout}
                  className="px-6 py-2 bg-gradient-to-r from-red-600 to-red-700 hover:from-red-700 hover:to-red-800 text-white font-bold rounded-lg transition shadow-lg hover:shadow-red-500/50 flex items-center gap-2"
                >
                  <LogOut size={18} />
                  {t('nav.logout')}
                </button>
              </>
            ) : (
              <>
                <Link href="/driver-login" className="px-5 py-2 bg-gradient-to-r from-orange-500 to-orange-600 hover:from-orange-600 hover:to-orange-700 text-white font-bold rounded-lg transition shadow-lg hover:shadow-orange-500/50 whitespace-nowrap">
                  {t('nav.driverLogin')}
                </Link>
                <Link href="/public-sos" className="px-5 py-2 bg-gradient-to-r from-red-600 to-red-700 hover:from-red-700 hover:to-red-800 text-white font-bold rounded-lg transition shadow-lg hover:shadow-red-500/50 whitespace-nowrap">
                  {t('nav.sos')}
                </Link>
                <Link href="/hospital-login" className="hidden xl:inline-block px-5 py-2 bg-gradient-to-r from-emerald-600 to-emerald-700 hover:from-emerald-700 hover:to-emerald-800 text-white font-bold rounded-lg transition shadow-lg hover:shadow-emerald-500/50 whitespace-nowrap">
                  {t('nav.hospitalLogin')}
                </Link>
                <Link href="/police-login" className="hidden xl:inline-block px-5 py-2 bg-gradient-to-r from-blue-600 to-blue-700 hover:from-blue-700 hover:to-blue-800 text-white font-bold rounded-lg transition shadow-lg hover:shadow-blue-500/50 whitespace-nowrap">
                  {t('nav.policeLogin')}
                </Link>
              </>
            )}
          </div>

          <button onClick={() => setMobileMenuOpen(!mobileMenuOpen)} className="2xl:hidden ml-auto justify-self-end text-white hover:text-orange-400 transition" aria-label="Toggle menu">
            {mobileMenuOpen ? <X size={28} /> : <Menu size={28} />}
          </button>
        </div>

        {mobileMenuOpen && (
          <div className="2xl:hidden pb-4 border-t border-white/10 animate-slide-down">
            <div className="px-6 py-3">
              <label className="text-xs text-gray-400">{t('lang.label')}</label>
              <select
                value={language}
                onChange={(event) => setLanguage(event.target.value as AppLanguage)}
                className="mt-2 w-full bg-slate-900 text-white rounded-lg px-3 py-2 border border-slate-700"
              >
                {languageOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>
            <Link href="/" className="block px-6 py-3 text-gray-300 hover:text-white hover:bg-white/5 rounded-lg font-semibold transition">{t('nav.home')}</Link>
            <Link href="/driver-login" className="block px-6 py-3 text-gray-300 hover:text-white hover:bg-white/5 rounded-lg font-semibold transition">{t('nav.driver')}</Link>
            <Link href="/public-sos" className="block px-6 py-3 text-gray-300 hover:text-white hover:bg-white/5 rounded-lg font-semibold transition">{t('nav.emergency')}</Link>
            <Link href="/hospital-login" className="block px-6 py-3 text-gray-300 hover:text-white hover:bg-white/5 rounded-lg font-semibold transition">{t('nav.hospital')}</Link>
            <Link href="/police-login" className="block px-6 py-3 text-gray-300 hover:text-white hover:bg-white/5 rounded-lg font-semibold transition">{t('nav.police')}</Link>
            <div className="px-6 py-3 space-y-2 border-t border-white/10 mt-3 pt-3">
              {user ? (
                <>
                  <div className="px-6 py-3 bg-white/10 rounded-lg border border-white/20">
                    <span className="text-xs text-gray-400">{t('nav.loggedIn')}</span>
                    <p className="text-white font-semibold">{user.fullName}</p>
                  </div>
                  <button
                    onClick={handleLogout}
                    className="w-full px-6 py-3 bg-red-600 hover:bg-red-700 rounded-lg font-bold text-white transition flex items-center justify-center gap-2"
                  >
                    <LogOut size={18} />
                    {t('nav.logout')}
                  </button>
                </>
              ) : (
                <>
                  <Link href="/driver-login" className="block px-6 py-3 bg-orange-600 hover:bg-orange-700 rounded-lg font-bold text-center text-white transition">{t('nav.driverLogin')}</Link>
                  <Link href="/public-sos" className="block px-6 py-3 bg-red-600 hover:bg-red-700 rounded-lg font-bold text-center text-white transition">{t('nav.sos')}</Link>
                  <Link href="/hospital-login" className="block px-6 py-3 bg-emerald-600 hover:bg-emerald-700 rounded-lg font-bold text-center text-white transition">{t('nav.hospitalLogin')}</Link>
                  <Link href="/police-login" className="block px-6 py-3 bg-blue-600 hover:bg-blue-700 rounded-lg font-bold text-center text-white transition">{t('nav.policeLogin')}</Link>
                </>
              )}
            </div>
          </div>
        )}
      </div>
    </nav>
  );
}
