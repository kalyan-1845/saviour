'use client';

import { useState } from 'react';
import { Eye, EyeOff, Lock, User, Mail, Phone, AlertCircle, CheckCircle } from 'lucide-react';
import Link from 'next/link';
import { GlowButton } from '@/components/shared';

export default function PublicRegisterPage() {
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    phone: '',
    password: '',
    confirmPassword: '',
  });

  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
    setError('');
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess(false);
    setIsLoading(true);

    try {
      // Validation
      if (!formData.fullName || !formData.email || !formData.phone || !formData.password) {
        setError('Please fill all required fields');
        setIsLoading(false);
        return;
      }

      if (formData.password !== formData.confirmPassword) {
        setError('Passwords do not match');
        setIsLoading(false);
        return;
      }

      if (formData.password.length < 6) {
        setError('Password must be at least 6 characters');
        setIsLoading(false);
        return;
      }

      // Call API to register
      const response = await fetch('/api/auth/user-register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          fullName: formData.fullName,
          email: formData.email,
          phone: formData.phone,
          password: formData.password,
          confirmPassword: formData.confirmPassword,
        }),
      });

      const data = await response.json();

      if (!response.ok) {
        setError(data.error || 'Registration failed. Please try again.');
        setIsLoading(false);
        return;
      }

      // Success!
      setSuccess(true);
      setFormData({
        fullName: '',
        email: '',
        phone: '',
        password: '',
        confirmPassword: '',
      });

      // Redirect to SOS page after 2 seconds
      setTimeout(() => {
        window.location.href = '/driver-login';
      }, 2000);

    } catch (err: any) {
      setError(err.message || 'Registration failed. Please try again.');
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-red-900 to-orange-900 flex items-center justify-center p-4 py-12">
      <div className="w-full max-w-2xl">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-gradient-to-br from-red-500 to-orange-600 rounded-full mb-4">
            <span className="text-white text-3xl font-black">S</span>
          </div>
          <h1 className="text-3xl font-black text-white mb-2">SARATHI</h1>
          <p className="text-red-300">Emergency Registration Portal</p>
        </div>

        {/* Form Card */}
        <div className="bg-white/10 backdrop-blur-md border-2 border-red-500/50 rounded-2xl p-8">
          {success && (
            <div className="mb-6 p-4 bg-green-500/20 border border-green-500 rounded-lg flex gap-3 animate-slide-down">
              <CheckCircle size={20} className="text-green-400 flex-shrink-0" />
              <div>
                <p className="text-green-300 text-sm font-bold">Registration Successful!</p>
                <p className="text-green-300/80 text-xs">Redirecting to SOS portal...</p>
              </div>
            </div>
          )}

          {error && (
            <div className="mb-6 p-4 bg-red-500/20 border border-red-500 rounded-lg flex gap-3">
              <AlertCircle size={20} className="text-red-400 flex-shrink-0" />
              <p className="text-red-300 text-sm">{error}</p>
            </div>
          )}

          <form onSubmit={handleRegister} className="space-y-4">
            <div className="grid md:grid-cols-2 gap-4">
              {/* Full Name */}
              <div className="md:col-span-2">
                <label className="block text-white font-semibold mb-2 text-sm">
                  Full Name *
                </label>
                <div className="relative">
                  <User
                    size={18}
                    className="absolute left-3 top-1/2 transform -translate-y-1/2 text-red-400"
                  />
                  <input
                    type="text"
                    name="fullName"
                    value={formData.fullName}
                    onChange={handleInputChange}
                    placeholder="John Doe"
                    className="w-full pl-10 pr-4 py-3 bg-red-950/50 border-2 border-red-500/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:border-red-500 focus:bg-red-950/80 transition"
                  />
                </div>
              </div>

              {/* Email */}
              <div>
                <label className="block text-white font-semibold mb-2 text-sm">
                  Email Address *
                </label>
                <div className="relative">
                  <Mail
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

              {/* Phone */}
              <div>
                <label className="block text-white font-semibold mb-2 text-sm">
                  Phone Number *
                </label>
                <div className="relative">
                  <Phone
                    size={18}
                    className="absolute left-3 top-1/2 transform -translate-y-1/2 text-red-400"
                  />
                  <input
                    type="tel"
                    name="phone"
                    value={formData.phone}
                    onChange={handleInputChange}
                    placeholder="+91 9876543210"
                    className="w-full pl-10 pr-4 py-3 bg-red-950/50 border-2 border-red-500/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:border-red-500 focus:bg-red-950/80 transition"
                  />
                </div>
              </div>

              {/* Password */}
              <div>
                <label className="block text-white font-semibold mb-2 text-sm">
                  Password *
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
                    {showPassword ? (
                      <EyeOff size={18} />
                    ) : (
                      <Eye size={18} />
                    )}
                  </button>
                </div>
              </div>

              {/* Confirm Password */}
              <div>
                <label className="block text-white font-semibold mb-2 text-sm">
                  Confirm Password *
                </label>
                <input
                  type="password"
                  name="confirmPassword"
                  value={formData.confirmPassword}
                  onChange={handleInputChange}
                  placeholder="••••••••"
                  className="w-full px-4 py-3 bg-red-950/50 border-2 border-red-500/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:border-red-500 focus:bg-red-950/80 transition"
                />
              </div>
            </div>

            {/* Terms */}
            <label className="flex items-center gap-2 cursor-pointer mt-4">
              <input
                type="checkbox"
                className="w-4 h-4 rounded bg-red-950/50 border-red-500 cursor-pointer"
              />
              <span className="text-red-300 text-sm">
                I agree to the terms and conditions
              </span>
            </label>

            {/* Register Button */}
            <GlowButton
              type="submit"
              variant="danger"
              size="lg"
              className="w-full mt-6"
              disabled={isLoading}
            >
              {isLoading ? 'Registering...' : 'Register for Emergency'}
            </GlowButton>
          </form>

          {/* Login Link */}
          <div className="text-center mt-6">
            <p className="text-white/70">Already registered?</p>
            <Link
              href="/driver-login"
              className="text-red-400 hover:text-red-300 font-bold transition"
            >
              Go to SOS
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
