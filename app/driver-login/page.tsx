'use client';

import { useState } from 'react';
import Link from 'next/link';
import { motion } from 'framer-motion';
import { Mail, Lock, Eye, EyeOff, ChevronLeft } from 'lucide-react';
import { useI18n } from '@/components/shared/LanguageProvider';
import { GlowButton } from '@/components/shared/GlowButton';

export default function DriverLogin() {
  const { t } = useI18n();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    // Mock login
    await new Promise(r => setTimeout(r, 1500));
    // Redirect to dashboard (mock)
    window.location.href = '/driver/dashboard';
    setLoading(false);
  };

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="min-h-screen flex items-center justify-center p-4 bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950"
    >
      <div className="w-full max-w-md">
        <motion.div
          initial={{ y: -20, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ duration: 0.5 }}
          className="card-glow p-8"
        >
          <Link href="/" className="flex items-center gap-2 mb-6 text-slate-400 hover:text-white transition">
            <ChevronLeft size={20} />
            {t('common.backHome') || 'Back to Home'}
          </Link>
          <h1 className="text-3xl font-black mb-2 gradient-text">{t('driverLogin.title') || 'Driver Portal'}</h1>
          <p className="text-slate-400 mb-8">{t('driverLogin.subtitle') || 'Access emergency dashboard'}</p>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-semibold mb-2">Email</label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 w-4 h-4" />
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="demo@driver.com"
                  className="w-full pl-10 pr-4 py-3 rounded-lg bg-white/10 border border-white/20 focus:border-orange-400 focus:bg-white/20"
                  required
                />
              </div>
            </div>
            <div>
              <label className="block text-sm font-semibold mb-2">Password</label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 w-4 h-4" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="test123"
                  className="w-full pl-10 pr-12 py-3 rounded-lg bg-white/10 border border-white/20 focus:border-orange-400 focus:bg-white/20"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-white"
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>
            <div className="flex items-center justify-between text-sm">
              <label className="flex items-center gap-2">
                <input type="checkbox" className="rounded" />
                <span>{t('driverLogin.remember') || 'Remember me'}</span>
              </label>
              <Link href="#" className="text-orange-400 hover:text-orange-300">{t('driverLogin.forgot') || 'Forgot Password?'}</Link>
            </div>
            <GlowButton
              type="submit"
              variant="primary"
              size="lg"
              disabled={loading}
              className="w-full"
            >
              {loading ? (t('driverLogin.signing') || 'Signing In...') : (t('driverLogin.signIn') || 'Sign In')}
            </GlowButton>
            <div className="text-center text-sm text-slate-400 pt-4">
              <span>{t('driverLogin.noAccount') || "Don't have an account? "}</span>
              <Link href="/driver-register" className="text-orange-400 hover:text-orange-300 font-semibold">
                {t('driverLogin.register') || 'Register as Driver'}
              </Link>
            </div>
          </form>
        </motion.div>
      </div>
    </motion.div>
  );
}

