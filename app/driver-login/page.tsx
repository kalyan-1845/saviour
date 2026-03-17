'use client';

import { useState } from 'react';
import Link from 'next/link';
import { motion } from 'framer-motion';
import { Mail, Lock, Eye, EyeOff, ChevronLeft, ShieldCheck } from 'lucide-react';
import { useI18n } from '@/components/shared/LanguageProvider';
import { GlowButton } from '@/components/shared/GlowButton';

export default function DriverLogin() {
  const { t } = useI18n();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const response = await fetch('/api/auth/driver-login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || 'Login failed');
      }

      // Success - In a real app we'd handle JWT here, but for now redirect
      window.location.href = '/driver/dashboard';
    } catch (err: any) {
      setError(err.message || 'Invalid credentials');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col items-center justify-center p-6 relative overflow-hidden">
      {/* Decorative Blur */}
      <div className="absolute top-0 right-0 w-96 h-96 bg-blue-600/10 blur-[120px] rounded-full"></div>
      <div className="absolute bottom-0 left-0 w-96 h-96 bg-red-600/10 blur-[120px] rounded-full"></div>

      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        className="w-full max-w-md z-10"
      >
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-blue-500/10 border border-blue-500/20 mb-6 shadow-xl">
            <ShieldCheck className="text-blue-500" size={32} />
          </div>
          <h1 className="text-3xl font-black text-white mb-2 tracking-tight">Driver Portal</h1>
          <p className="text-slate-500">Access your emergency dashboard</p>
        </div>

        <div className="bg-slate-900 border border-white/10 rounded-[40px] p-8 shadow-2xl">
          {error && (
            <div className="mb-6 p-4 rounded-2xl bg-red-500/10 border border-red-500/20 text-red-500 text-sm">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="space-y-2">
              <label className="text-xs font-black uppercase tracking-widest text-slate-500 ml-2">Email Address</label>
              <div className="relative">
                <Mail className="absolute left-6 top-1/2 -translate-y-1/2 text-slate-600" size={20} />
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@provider.com"
                  className="w-full pl-16 pr-6 py-5 bg-slate-800 border-2 border-transparent rounded-[24px] text-white focus:border-blue-500/50 outline-none transition-all placeholder:text-slate-600"
                  required
                />
              </div>
            </div>

            <div className="space-y-2">
              <label className="text-xs font-black uppercase tracking-widest text-slate-500 ml-2">Password</label>
              <div className="relative">
                <Lock className="absolute left-6 top-1/2 -translate-y-1/2 text-slate-600" size={20} />
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full pl-16 pr-16 py-5 bg-slate-800 border-2 border-transparent rounded-[24px] text-white focus:border-blue-500/50 outline-none transition-all placeholder:text-slate-600"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-6 top-1/2 -translate-y-1/2 text-slate-600 hover:text-white transition-colors"
                >
                  {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                </button>
              </div>
            </div>

            <div className="flex items-center justify-between text-xs font-bold px-2">
              <label className="flex items-center gap-2 cursor-pointer group">
                <div className="w-5 h-5 rounded-lg border-2 border-slate-700 bg-slate-800 flex items-center justify-center group-hover:border-blue-500 transition-colors">
                  <div className="w-2.5 h-2.5 bg-blue-500 rounded-sm opacity-0 checked:opacity-100"></div>
                </div>
                <span className="text-slate-500 group-hover:text-slate-300">Remember me</span>
              </label>
              <Link href="#" className="text-blue-500 hover:text-blue-400">Forgot Password?</Link>
            </div>

            <GlowButton
              type="submit"
              size="lg"
              disabled={loading}
              className="w-full py-6 rounded-[24px] text-lg font-black shadow-xl shadow-blue-500/10"
            >
              {loading ? 'AUTHENTICATING...' : 'SIGN IN'}
            </GlowButton>
          </form>

          <div className="mt-8 text-center">
            <p className="text-slate-500 text-sm">
              New to Sarathi?{' '}
              <Link href="/driver-register" className="text-white font-black hover:text-blue-400 transition-colors ml-1">
                Register as Driver
              </Link>
            </p>
          </div>
        </div>

        <Link href="/" className="flex items-center justify-center gap-2 mt-8 text-slate-600 hover:text-slate-400 transition-colors text-sm font-bold uppercase tracking-widest">
          <ChevronLeft size={16} />
          Back to Portal
        </Link>
      </motion.div>
    </div>
  );
}
