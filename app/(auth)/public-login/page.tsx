'use client';

import { useState } from 'react';
import { Eye, EyeOff, Lock, User, AlertCircle } from 'lucide-react';
import Link from 'next/link';
import { GlowButton } from '@/components/shared';

export default function PublicLoginPage() {
  const [formData, setFormData] = useState({
    email: '',
    password: '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
    setError('');
  };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      if (!formData.email || !formData.password) {
        setError('Please fill all fields');
        setIsLoading(false);
        return;
      }

      // Call API to login
      const response = await fetch('/api/auth/user-login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          email: formData.email,
          password: formData.password,
        }),
      });

      const data = await response.json();

      if (!response.ok) {
        setError(data.error || 'Login failed. Please try again.');
        setIsLoading(false);
        return;
      }

      // Success! Save user to localStorage
      localStorage.setItem('currentUser', JSON.stringify({
        ...data.user,
        type: 'user'
      }));
      setFormData({ email: '', password: '' });
      window.location.href = '/public-sos';
    } catch (err: any) {
      setError(err.message || 'Login failed. Please try again.');
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-red-900 to-orange-900 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-gradient-to-br from-red-500 to-orange-600 rounded-full mb-4">
            <span className="text-white text-3xl font-black">S</span>
          </div>
          <h1 className="text-3xl font-black text-white mb-2">SARATHI</h1>
          <p className="text-red-300">Emergency SOS Login</p>
        </div>

        {/* Form Card */}
        <div className="bg-white/10 backdrop-blur-md border-2 border-red-500/50 rounded-2xl p-8 mb-6">
          {error && (
            <div className="mb-6 p-4 bg-red-500/20 border border-red-500 rounded-lg flex gap-3">
              <AlertCircle size={20} className="text-red-400 flex-shrink-0" />
              <p className="text-red-300 text-sm">{error}</p>
            </div>
          )}

          <form onSubmit={handleLogin} className="space-y-5">
            {/* Email Field */}
            <div>
              <label className="block text-white font-semibold mb-2 text-sm">
                Email Address
              </label>
              <div className="relative">
                <User
                  size={18}
                  className="absolute left-3 top-1/2 transform -translate-y-1/2 text-red-400"
                />
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleInputChange}
                  placeholder="user@example.com"
                  className="w-full pl-10 pr-4 py-3 bg-red-950/50 border-2 border-red-500/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:border-red-500 focus:bg-red-950/80 transition"
                />
              </div>
            </div>

            {/* Password Field */}
            <div>
              <label className="block text-white font-semibold mb-2 text-sm">
                Password
              </label>
              <div className="relative">
                <Lock
                  size={18}
                  className="absolute left-3 top-1/2 transform -translate-y-1/2 text-red-400"
                />
                <input
                  type={showPassword ? 'text' : 'password'}
                  name="password"
                  value={formData.password}
                  onChange={handleInputChange}
                  placeholder="••••••••"
                  className="w-full pl-10 pr-10 py-3 bg-red-950/50 border-2 border-red-500/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:border-red-500 focus:bg-red-950/80 transition"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 transform -translate-y-1/2 text-red-400 hover:text-red-300"
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            {/* Remember & Forgot */}
            <div className="flex justify-between items-center text-sm">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  className="w-4 h-4 rounded bg-red-950/50 border-red-500 cursor-pointer"
                />
                <span className="text-red-300">Remember me</span>
              </label>
              <Link
                href="/forgot-password"
                className="text-red-400 hover:text-red-300 transition"
              >
                Forgot Password?
              </Link>
            </div>

            {/* Login Button */}
            <GlowButton
              type="submit"
              variant="danger"
              size="lg"
              className="w-full mt-6"
              disabled={isLoading}
            >
              {isLoading ? 'Signing In...' : 'Sign In'}
            </GlowButton>
          </form>

          {/* Divider */}
          <div className="relative my-6">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-white/10"></div>
            </div>
            <div className="relative flex justify-center text-sm">
              <span className="px-2 bg-white/10 text-white/60">OR</span>
            </div>
          </div>

          {/* Quick SOS */}
          <Link
            href="/public-sos"
            className="block w-full py-3 px-4 text-center bg-orange-600/50 hover:bg-orange-600 border-2 border-orange-500/50 text-white font-bold rounded-lg transition"
          >
            Quick SOS
          </Link>
        </div>

        {/* Register Link */}
        <div className="text-center">
          <p className="text-white/70">Don't have an account?</p>
          <Link
            href="/public-register"
            className="text-red-400 hover:text-red-300 font-bold transition"
          >
            Register for Emergency
          </Link>
        </div>

        {/* Emergency Contact */}
        <div className="mt-8 p-4 bg-yellow-600/20 border border-yellow-600/50 rounded-lg text-center">
          <p className="text-yellow-300 text-xs font-semibold">EMERGENCY HELPLINE</p>
          <p className="text-white font-bold text-lg">+91-XXX-XXX-XXXX</p>
        </div>
      </div>
    </div>
  );
}
