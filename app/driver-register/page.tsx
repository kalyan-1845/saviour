'use client';

import { useState } from 'react';
import { motion } from 'framer-motion';
import Link from 'next/link';
import { ChevronLeft, User, Mail, Phone, IdCard, Car, Lock, Eye, EyeOff, ShieldCheck } from 'lucide-react';
import { GlowButton } from '@/components/shared/GlowButton';

export default function DriverRegister() {
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    phone: '',
    licenseNo: '',
    vehicleNo: '',
    password: '',
    confirmPassword: '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match');
      return;
    }
    
    setLoading(true);
    setError('');

    try {
      // In a real app, we'd hit /api/auth/driver-register
      // For now, let's simulate a successful registration
      await new Promise(r => setTimeout(r, 2000));
      setSuccess(true);
      setTimeout(() => {
        window.location.href = '/driver-login';
      }, 2000);
    } catch (err: any) {
      setError(err.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <div className="min-h-screen bg-slate-950 flex flex-col items-center justify-center p-6 text-center">
        <div className="w-20 h-20 rounded-full bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center mb-8">
          <ShieldCheck className="text-emerald-500 animate-pulse" size={40} />
        </div>
        <h1 className="text-3xl font-black text-white mb-4">Registration Successful!</h1>
        <p className="text-slate-400">Redirecting you to the login portal...</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col items-center justify-center p-6 relative overflow-hidden py-20">
      {/* Decorative Blur */}
      <div className="absolute top-0 right-0 w-96 h-96 bg-blue-600/10 blur-[120px] rounded-full"></div>
      <div className="absolute bottom-0 left-0 w-96 h-96 bg-red-600/10 blur-[120px] rounded-full"></div>

      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        className="w-full max-w-2xl z-10"
      >
        <div className="text-center mb-12">
          <Link href="/driver-login" className="inline-flex items-center gap-2 text-slate-500 hover:text-white transition-colors mb-6 group">
            <ChevronLeft size={20} className="group-hover:-translate-x-1 transition-transform" />
            Back to Login
          </Link>
          <h1 className="text-4xl font-black text-white mb-4 tracking-tight">Join the Network</h1>
          <p className="text-slate-500">Register as a responder and save lives</p>
        </div>

        <div className="bg-slate-900 border border-white/10 rounded-[40px] p-10 shadow-2xl">
          {error && (
            <div className="mb-8 p-4 rounded-2xl bg-red-500/10 border border-red-500/20 text-red-500 text-sm">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-8">
            <div className="grid md:grid-cols-2 gap-8">
              {/* Personal Info */}
              <div className="space-y-6">
                <h3 className="text-xs font-black uppercase tracking-[0.2em] text-blue-500 mb-2">Personal Details</h3>
                
                <div className="space-y-2">
                  <label className="text-[10px] font-black uppercase tracking-widest text-slate-500 ml-2">Full Name</label>
                  <div className="relative">
                    <User className="absolute left-6 top-1/2 -translate-y-1/2 text-slate-600" size={18} />
                    <input
                      name="fullName"
                      value={formData.fullName}
                      onChange={handleChange}
                      placeholder="Kalyan R"
                      className="w-full pl-14 pr-6 py-4 bg-slate-800 border-2 border-transparent rounded-2xl text-white focus:border-blue-500/50 outline-none transition-all"
                      required
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <label className="text-[10px] font-black uppercase tracking-widest text-slate-500 ml-2">Email Address</label>
                  <div className="relative">
                    <Mail className="absolute left-6 top-1/2 -translate-y-1/2 text-slate-600" size={18} />
                    <input
                      name="email"
                      type="email"
                      value={formData.email}
                      onChange={handleChange}
                      placeholder="kalyan@sarathi.in"
                      className="w-full pl-14 pr-6 py-4 bg-slate-800 border-2 border-transparent rounded-2xl text-white focus:border-blue-500/50 outline-none transition-all"
                      required
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <label className="text-[10px] font-black uppercase tracking-widest text-slate-500 ml-2">Phone Number</label>
                  <div className="relative">
                    <Phone className="absolute left-6 top-1/2 -translate-y-1/2 text-slate-600" size={18} />
                    <input
                      name="phone"
                      type="tel"
                      value={formData.phone}
                      onChange={handleChange}
                      placeholder="+91 98765 43210"
                      className="w-full pl-14 pr-6 py-4 bg-slate-800 border-2 border-transparent rounded-2xl text-white focus:border-blue-500/50 outline-none transition-all"
                      required
                    />
                  </div>
                </div>
              </div>

              {/* Vehicle Info */}
              <div className="space-y-6">
                <h3 className="text-xs font-black uppercase tracking-[0.2em] text-red-500 mb-2">Vehicle Details</h3>

                <div className="space-y-2">
                  <label className="text-[10px] font-black uppercase tracking-widest text-slate-500 ml-2">License Number</label>
                  <div className="relative">
                    <IdCard className="absolute left-6 top-1/2 -translate-y-1/2 text-slate-600" size={18} />
                    <input
                      name="licenseNo"
                      value={formData.licenseNo}
                      onChange={handleChange}
                      placeholder="TS-09-2024-XXXX"
                      className="w-full pl-14 pr-6 py-4 bg-slate-800 border-2 border-transparent rounded-2xl text-white focus:border-blue-500/50 outline-none transition-all"
                      required
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <label className="text-[10px] font-black uppercase tracking-widest text-slate-500 ml-2">Vehicle Number</label>
                  <div className="relative">
                    <Car className="absolute left-6 top-1/2 -translate-y-1/2 text-slate-600" size={18} />
                    <input
                      name="vehicleNo"
                      value={formData.vehicleNo}
                      onChange={handleChange}
                      placeholder="TS 09 AM 1234"
                      className="w-full pl-14 pr-6 py-4 bg-slate-800 border-2 border-transparent rounded-2xl text-white focus:border-blue-500/50 outline-none transition-all"
                      required
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <label className="text-[10px] font-black uppercase tracking-widest text-slate-500 ml-2">Password</label>
                  <div className="relative">
                    <Lock className="absolute left-6 top-1/2 -translate-y-1/2 text-slate-600" size={18} />
                    <input
                      name="password"
                      type={showPassword ? 'text' : 'password'}
                      value={formData.password}
                      onChange={handleChange}
                      placeholder="••••••••"
                      className="w-full pl-14 pr-14 py-4 bg-slate-800 border-2 border-transparent rounded-2xl text-white focus:border-blue-500/50 outline-none transition-all"
                      required
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-6 top-1/2 -translate-y-1/2 text-slate-600 hover:text-white transition-colors"
                    >
                      {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <div className="space-y-2 pt-4">
              <label className="text-[10px] font-black uppercase tracking-widest text-slate-500 ml-2">Confirm Password</label>
              <input
                name="confirmPassword"
                type="password"
                value={formData.confirmPassword}
                onChange={handleChange}
                placeholder="Confirm your password"
                className="w-full px-6 py-4 bg-slate-800 border-2 border-transparent rounded-2xl text-white focus:border-blue-500/50 outline-none transition-all"
                required
              />
            </div>

            <GlowButton
              type="submit"
              size="lg"
              disabled={loading}
              className="w-full py-8 rounded-[32px] text-xl font-black shadow-2xl shadow-blue-600/20"
            >
              {loading ? 'PROCESSING...' : 'COMPLETE REGISTRATION'}
            </GlowButton>
          </form>
        </div>
      </motion.div>
    </div>
  );
}
