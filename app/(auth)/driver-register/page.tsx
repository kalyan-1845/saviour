'use client';

import { useState } from 'react';
import { Eye, EyeOff, Lock, User, Mail, Phone, AlertCircle, CheckCircle } from 'lucide-react';
import Link from 'next/link';
import { GlowButton } from '@/components/shared';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_REGEX = /^\d{10}$/;
const VEHICLE_REGEX = /^(AP|TS)[ -]?\d{2}[ -]?[A-Z]{1,2}[ -]?\d{4}$/i;

export default function DriverRegisterPage() {
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    phone: '',
    licenseNumber: '',
    vehicleNumber: '',
    password: '',
    confirmPassword: '',
  });

  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    let nextValue = value;

    if (name === 'phone') {
      nextValue = value.replace(/[^\d]/g, '').slice(0, 10);
    }

    if (name === 'vehicleNumber') {
      nextValue = value.toUpperCase().replace(/[^A-Z0-9 -]/g, '');
    }

    if (name === 'email') {
      nextValue = value.trim();
    }

    setFormData((prev) => ({
      ...prev,
      [name]: nextValue,
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
      if (!formData.fullName || !formData.email || !formData.phone || !formData.licenseNumber || !formData.vehicleNumber || !formData.password) {
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

      const normalizedEmail = formData.email.trim().toLowerCase();
      const normalizedPhone = formData.phone.replace(/[^\d]/g, '');
      const normalizedVehicle = formData.vehicleNumber.trim().toUpperCase().replace(/\s+/g, ' ');

      if (!EMAIL_REGEX.test(normalizedEmail)) {
        setError('Enter a valid email address.');
        setIsLoading(false);
        return;
      }

      if (!PHONE_REGEX.test(normalizedPhone)) {
        setError('Phone number must be exactly 10 digits.');
        setIsLoading(false);
        return;
      }

      if (!VEHICLE_REGEX.test(normalizedVehicle)) {
        setError('Vehicle number must be like TS 09 AB 1234 or AP 28 X 1234.');
        setIsLoading(false);
        return;
      }

      // Call API to register
      const response = await fetch('/api/auth/driver-register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          fullName: formData.fullName,
          email: normalizedEmail,
          phone: normalizedPhone,
          licenseNumber: formData.licenseNumber,
          vehicleNumber: normalizedVehicle,
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
        licenseNumber: '',
        vehicleNumber: '',
        password: '',
        confirmPassword: '',
      });

      // Redirect to login after 2 seconds
      setTimeout(() => {
        window.location.href = '/driver-login';
      }, 2000);

    } catch (err: any) {
      setError(err.message || 'Registration failed. Please try again.');
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-green-900 to-teal-900 flex items-center justify-center p-4 py-12">
      <div className="w-full max-w-2xl">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-gradient-to-br from-green-500 to-teal-600 rounded-full mb-4">
            <span className="text-white text-3xl font-black">S</span>
          </div>
          <h1 className="text-3xl font-black text-white mb-2">SARATHI</h1>
          <p className="text-green-300">Driver Registration Portal</p>
        </div>

        {/* Form Card */}
        <div className="bg-white/10 backdrop-blur-md border-2 border-green-500/50 rounded-2xl p-8">
          {success && (
            <div className="mb-6 p-4 bg-green-500/20 border border-green-500 rounded-lg flex gap-3 animate-slide-down">
              <CheckCircle size={20} className="text-green-400 flex-shrink-0" />
              <div>
                <p className="text-green-300 text-sm font-bold">Registration Successful!</p>
                <p className="text-green-300/80 text-xs">Redirecting to login...</p>
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
              <div>
                <label className="block text-white font-semibold mb-2 text-sm">
                  Full Name *
                </label>
                <div className="relative">
                  <User
                    size={18}
                    className="absolute left-3 top-1/2 transform -translate-y-1/2 text-green-400"
                  />
                  <input
                    type="text"
                    name="fullName"
                    value={formData.fullName}
                    onChange={handleInputChange}
                    placeholder="John Doe"
                    className="w-full pl-10 pr-4 py-3 bg-green-950/50 border-2 border-green-500/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:border-green-500 focus:bg-green-950/80 transition"
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
                    className="absolute left-3 top-1/2 transform -translate-y-1/2 text-green-400"
                  />
                  <input
                    type="email"
                    name="email"
                    value={formData.email}
                    onChange={handleInputChange}
                    placeholder="driver@example.com"
                    className="w-full pl-10 pr-4 py-3 bg-green-950/50 border-2 border-green-500/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:border-green-500 focus:bg-green-950/80 transition"
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
                    className="absolute left-3 top-1/2 transform -translate-y-1/2 text-green-400"
                  />
                  <input
                    type="tel"
                    name="phone"
                    value={formData.phone}
                    onChange={handleInputChange}
                    placeholder="9876543210"
                    inputMode="numeric"
                    maxLength={10}
                    className="w-full pl-10 pr-4 py-3 bg-green-950/50 border-2 border-green-500/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:border-green-500 focus:bg-green-950/80 transition"
                  />
                </div>
              </div>

              {/* License Number */}
              <div>
                <label className="block text-white font-semibold mb-2 text-sm">
                  License Number *
                </label>
                <input
                  type="text"
                  name="licenseNumber"
                  value={formData.licenseNumber}
                  onChange={handleInputChange}
                  placeholder="DL12AB1234567"
                  className="w-full px-4 py-3 bg-green-950/50 border-2 border-green-500/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:border-green-500 focus:bg-green-950/80 transition"
                />
              </div>

              {/* Vehicle Number */}
              <div>
                <label className="block text-white font-semibold mb-2 text-sm">
                  Vehicle Number *
                </label>
                <input
                  type="text"
                  name="vehicleNumber"
                  value={formData.vehicleNumber}
                  onChange={handleInputChange}
                  placeholder="TS 09 AB 1234"
                  className="w-full px-4 py-3 bg-green-950/50 border-2 border-green-500/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:border-green-500 focus:bg-green-950/80 transition"
                />
              </div>

              {/* Password */}
              <div>
                <label className="block text-white font-semibold mb-2 text-sm">
                  Password *
                </label>
                <div className="relative">
                  <Lock
                    size={18}
                    className="absolute left-3 top-1/2 transform -translate-y-1/2 text-green-400"
                  />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    name="password"
                    value={formData.password}
                    onChange={handleInputChange}
                    placeholder="••••••••"
                    className="w-full pl-10 pr-10 py-3 bg-green-950/50 border-2 border-green-500/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:border-green-500 focus:bg-green-950/80 transition"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 transform -translate-y-1/2 text-green-400 hover:text-green-300"
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
                  className="w-full px-4 py-3 bg-green-950/50 border-2 border-green-500/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:border-green-500 focus:bg-green-950/80 transition"
                />
              </div>
            </div>

            {/* Terms */}
            <label className="flex items-center gap-2 cursor-pointer mt-4">
              <input
                type="checkbox"
                className="w-4 h-4 rounded bg-green-950/50 border-green-500 cursor-pointer"
              />
              <span className="text-green-300 text-sm">
                I agree to the terms and conditions
              </span>
            </label>

            {/* Register Button */}
            <GlowButton
              type="submit"
              variant="success"
              size="lg"
              className="w-full mt-6"
              disabled={isLoading}
            >
              {isLoading ? 'Registering...' : 'Complete Registration'}
            </GlowButton>
          </form>

          {/* Login Link */}
          <div className="text-center mt-6">
            <p className="text-white/70">Already a driver?</p>
            <Link
              href="/driver-login"
              className="text-green-400 hover:text-green-300 font-bold transition"
            >
              Login here
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
