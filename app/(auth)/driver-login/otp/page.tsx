'use client';

import { useState } from 'react';
import Link from 'next/link';
import { AlertCircle, Smartphone, ShieldCheck } from 'lucide-react';
import { GlowButton } from '@/components/shared';

export default function DriverOtpLoginPage() {
  const [phone, setPhone] = useState('');
  const [otp, setOtp] = useState('');
  const [sentOtp, setSentOtp] = useState('');
  const [isSending, setIsSending] = useState(false);
  const [isVerifying, setIsVerifying] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [otpSent, setOtpSent] = useState(false);

  const normalizedPhone = phone.replace(/[^\d+]/g, '').trim();

  const sendOtp = async () => {
    setError('');
    setMessage('');
    if (normalizedPhone.length < 10) {
      setError('Enter valid mobile number.');
      return;
    }

    setIsSending(true);
    try {
      const response = await fetch('/api/auth/driver-otp/send', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phone: normalizedPhone }),
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error || 'Failed to send OTP');
      }
      setOtpSent(true);
      setSentOtp(data.otp || '');
      setMessage('OTP sent. Enter OTP to login.');
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Failed to send OTP');
    } finally {
      setIsSending(false);
    }
  };

  const verifyAndLogin = async () => {
    setError('');
    setMessage('');
    if (!otpSent) {
      setError('Please send OTP first.');
      return;
    }
    if (otp.trim().length !== 6) {
      setError('Enter 6-digit OTP.');
      return;
    }

    setIsVerifying(true);
    try {
      const response = await fetch('/api/auth/driver-otp/verify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phone: normalizedPhone, otp }),
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error || 'OTP verification failed');
      }

      localStorage.setItem(
        'currentUser',
        JSON.stringify({
          ...data.driver,
          type: 'driver',
        })
      );
      window.location.href = '/driver/dashboard';
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'OTP verification failed');
    } finally {
      setIsVerifying(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-blue-900 to-purple-900 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-gradient-to-br from-violet-500 to-fuchsia-600 rounded-full mb-4">
            <ShieldCheck className="text-white" size={30} />
          </div>
          <h1 className="text-3xl font-black text-white mb-2">Driver OTP Login</h1>
          <p className="text-blue-300">Sign in securely using mobile OTP</p>
        </div>

        <div className="bg-white/10 backdrop-blur-md border-2 border-violet-500/50 rounded-2xl p-8">
          {error && (
            <div className="mb-4 p-3 bg-red-500/20 border border-red-500 rounded-lg flex gap-2">
              <AlertCircle size={18} className="text-red-300 mt-0.5" />
              <p className="text-red-200 text-sm">{error}</p>
            </div>
          )}

          {message && (
            <div className="mb-4 p-3 bg-emerald-500/20 border border-emerald-500 rounded-lg text-emerald-200 text-sm">
              {message}
            </div>
          )}

          <div className="space-y-4">
            <div>
              <label className="block text-white font-semibold mb-2 text-sm">Mobile Number</label>
              <div className="relative">
                <Smartphone size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-violet-300" />
                <input
                  type="tel"
                  value={phone}
                  onChange={(event) => setPhone(event.target.value)}
                  placeholder="Enter registered mobile number"
                  className="w-full pl-10 pr-4 py-3 bg-blue-950/50 border-2 border-blue-500/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:border-violet-500"
                />
              </div>
            </div>

            <GlowButton
              variant="primary"
              size="lg"
              className="w-full"
              onClick={sendOtp}
              disabled={isSending}
            >
              {isSending ? 'Sending OTP...' : 'Send OTP'}
            </GlowButton>

            {otpSent && (
              <>
                <div>
                  <label className="block text-white font-semibold mb-2 text-sm">Enter OTP</label>
                  <input
                    type="text"
                    value={otp}
                    onChange={(event) => setOtp(event.target.value.replace(/[^\d]/g, '').slice(0, 6))}
                    placeholder="6-digit OTP"
                    className="w-full px-4 py-3 bg-blue-950/50 border-2 border-blue-500/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:border-violet-500 tracking-[0.3em]"
                  />
                </div>

                {sentOtp && (
                  <div className="text-xs text-yellow-200 bg-yellow-900/30 border border-yellow-700 rounded p-2">
                    Demo OTP: <span className="font-bold">{sentOtp}</span>
                  </div>
                )}

                <GlowButton
                  variant="danger"
                  size="lg"
                  className="w-full"
                  onClick={verifyAndLogin}
                  disabled={isVerifying}
                >
                  {isVerifying ? 'Verifying...' : 'Verify OTP & Login'}
                </GlowButton>
              </>
            )}
          </div>

          <div className="mt-6 text-center">
            <Link href="/driver-login" className="text-violet-300 hover:text-violet-200 font-semibold">
              Back to Password Login
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
