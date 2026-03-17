'use client';

import { useState } from 'react';
import { motion } from 'framer-motion';
import Link from 'next/link';
import { ChevronLeft, User, Mail, Phone, IdCard, Car, Lock, Eye, EyeOff } from 'lucide-react';
import { GlowButton } from '@/components/shared/GlowButton';

interface FormData {
  fullName: string;
  email: string;
  phone: string;
  licenseNo: string;
  vehicleNo: string;
  password: string;
  confirmPassword: string;
}

// Mock i18n
const t = (key: string): string => {
  const translations = {
    'common.backHome': 'Back to Home',
    'driverLogin.title': 'Driver Registration'
  };
  return translations[key as keyof typeof translations] || key;
};

export default function DriverRegister() {
  const [formData, setFormData] = useState<FormData>({
    fullName: '',
    email: '',
    phone: '',
    licenseNo: '',
    vehicleNo: '',
    password: '',
    confirmPassword: '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [termsAccepted, setTermsAccepted] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (formData.password !== formData.confirmPassword) {
      alert('Passwords do not match');
      return;
    }
    setLoading(true);
    await new Promise(r => setTimeout(r, 2000));
    // Mock register → login
    window.location.href = '/driver-login';
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
          className="bg-white/10 backdrop-blur-md border border-white/20 rounded-2xl shadow-2xl p-8"
        >
          <Link href="/" className="flex items-center gap-2 mb-6 text-slate-400 hover:text-white transition-all">
            <ChevronLeft size={20} />
            {t('common.backHome')}
          </Link>
          <h1 className="text-3xl font-black mb-2 bg-gradient-to-r from-orange-400 to-red-500 bg-clip-text text-transparent">
            {t('driverLogin.title')}
          </h1>
          <p className="text-slate-400 mb-8">Complete form to join emergency network</p>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="flex items-center gap-2 text-sm font-semibold mb-2">
                <User size={16} />
                Full Name
              </label>
              <input
                name="fullName"
                value={formData.fullName}
                onChange={handleChange}
                className="w-full px-4 py-3 rounded-lg bg-white/10 border border-white/20 focus:border-orange-400 focus:bg-white/20 transition-all"
                required
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="flex items-center gap-2 text-sm font-semibold mb-2">
                  <Mail size={16} />
                  Email
                </label>
                <input
                  name="email"
                  type="email"
                  value={formData.email}
                  onChange={handleChange}
                  className="w-full px-4 py-3 rounded-lg bg-white/10 border border-white/20 focus:border-orange-400"
                  required
                />
              </div>
              <div>
                <label className="flex items-center gap-2 text-sm font-semibold mb-2">
                  <Phone size={16} />
                  Phone
                </label>
                <input
                  name="phone"
                  type="tel"
                  value={formData.phone}
                  onChange={handleChange}
                  className="w-full px-4 py-3 rounded-lg bg-white/10 border border-white/20 focus:border-orange-400"
                  required
                />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="flex items-center gap-2 text-sm font-semibold mb-2">
                  <IdCard size={16} />
                  License No
                </label>
                <input
                  name="licenseNo"
                  value={formData.licenseNo}
                  onChange={handleChange}
                  className="w-full px-4 py-3 rounded-lg bg-white/10 border border-white/20 focus:border-orange-400"
                  required
                />
              </div>
              <div>
                <label className="flex items-center gap-2 text-sm font-semibold mb-2">
                  <Car size={16} />
                  Vehicle No
                </label>
                <input
                  name="vehicleNo"
                  value={formData.vehicleNo}
                  onChange={handleChange}
                  className="w-full px-4 py-3 rounded-lg bg-white/10 border border-white/20 focus:border-orange-400"
                  required
                />
              </div>
            </div>
            <div>
              <label className="flex items-center gap-2 text-sm font-semibold mb-2">
                <Lock size={16} />
                Password
              </label>
              <div className="relative">
                <input
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  value={formData.password}
                  onChange={handleChange}
                  className="w-full px-4 pr-12 py-3 rounded-lg bg-white/10 border border-white/20 focus:border-orange-400"
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
            <div>
              <label className="flex items-center gap-2 text-sm font-semibold mb-2">
                <Lock size={16} />
                Confirm Password
              </label>
              <div className="relative">
                <input
                  name="confirmPassword"
                  type={showConfirmPassword ? 'text' : 'password'}
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  className="w-full px-4 pr-12 py-3 rounded-lg bg-white/10 border border-white/20 focus:border-orange-400"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-white"
                >
                  {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <input
                id="terms"
                type="checkbox"
                checked={termsAccepted}
                onChange={(e) => setTermsAccepted(e.target.checked)}
                className="w-4 h-4 rounded text-orange-500 focus:ring-orange-500"
                required
              />
              <label htmlFor="terms" className="text-sm text-slate-400 cursor-pointer">
                I accept the terms and conditions
              </label>
            </div>
            <GlowButton
              type="submit"
              variant="primary"
              size="lg"
              disabled={loading || !termsAccepted}
              className="w-full"
            >
              {loading ? 'Registering...' : 'Register as Driver'}
            </GlowButton>
            <div className="text-center pt-4">
              <Link href="/driver-login" className="text-orange-400 hover:text-orange-300 transition-colors">
                Already registered? Login
              </Link>
            </div>
          </form>
        </motion.div>
      </div>
    </motion.div>
  );
}

