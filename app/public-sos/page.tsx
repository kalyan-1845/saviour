'use client';

import { useState, useEffect } from 'react';
import { Phone, AlertCircle, MapPin, Clock, Shield, Activity, Flame, Car } from 'lucide-react';
import { useEmergencyStore } from '@/store/useEmergencyStore';
import { GlowButton } from '@/components/shared';

export default function PublicSOSPage() {
  const [phone, setPhone] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [emergencyType, setEmergencyType] = useState<'medical' | 'accident' | 'fire' | 'crime'>('medical');
  const { activeTrip, setActiveTrip } = useEmergencyStore();

  const handleSendSOS = async () => {
    if (!phone || phone.length < 10) {
      setError('Please enter a valid phone number');
      return;
    }

    setIsSubmitting(true);
    setError('');

    try {
      // Get location
      const position = await new Promise<GeolocationPosition>((resolve, reject) => {
        navigator.geolocation.getCurrentPosition(resolve, reject);
      });

      const { latitude, longitude } = position.coords;

      const response = await fetch('/api/emergency/sos', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          phone,
          latitude,
          longitude,
          emergencyType,
        }),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.error || 'Failed to send SOS');
      }

      setActiveTrip({
        id: data.tripId,
        phone,
        location: { latitude, longitude },
        emergencyType: data.emergencyType || emergencyType,
        timestamp: Date.now(),
        status: data.status,
        driverId: data.driver?.id,
        hospital: data.hospital,
        policeStation: data.policeStation,
        etaMinutes: data.etaMinutes,
      });

    } catch (err: any) {
      setError(err.message || 'Location permission denied or system error');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (activeTrip) {
    return (
      <div className="min-h-screen bg-slate-950 flex items-center justify-center p-4">
        <div className="w-full max-w-md space-y-6">
          <div className="relative w-40 h-40 mx-auto">
            <div className="absolute inset-0 bg-red-600/30 rounded-full animate-ping"></div>
            <div className="absolute inset-0 bg-red-600/20 rounded-full animate-pulse" style={{ animationDuration: '3s' }}></div>
            <div className="absolute inset-8 bg-red-600 rounded-full flex items-center justify-center shadow-[0_0_50px_rgba(220,38,38,0.5)]">
              <Shield size={48} className="text-white animate-bounce" />
            </div>
          </div>

          <div className="bg-slate-900/80 backdrop-blur-xl rounded-3xl border-2 border-red-500/50 p-8 shadow-2xl text-center">
            <div className="inline-block px-4 py-1 rounded-full bg-red-500/10 border border-red-500/30 text-red-500 text-xs font-bold tracking-widest uppercase mb-4">
              Emergency Active
            </div>
            <h1 className="text-3xl font-black text-white mb-2 leading-tight">HELP IS ON THE WAY</h1>
            <p className="text-slate-400 text-sm mb-8 italic">Your location is being tracked in real-time</p>

            <div className="grid grid-cols-2 gap-4 mb-8 text-left">
              <div className="bg-white/5 p-4 rounded-2xl border border-white/10">
                <Clock className="text-blue-400 mb-2" size={20} />
                <p className="text-slate-400 text-[10px] uppercase font-bold">ETA</p>
                <p className="text-white text-lg font-black">{activeTrip.etaMinutes || 'Calculating'}m</p>
              </div>
              <div className="bg-white/5 p-4 rounded-2xl border border-white/10">
                <Activity className="text-emerald-400 mb-2" size={20} />
                <p className="text-slate-400 text-[10px] uppercase font-bold">Status</p>
                <p className="text-white text-lg font-black capitalize">{activeTrip.status}</p>
              </div>
            </div>

            <div className="bg-blue-600/10 border border-blue-500/20 rounded-2xl p-4 mb-8 text-left">
              <p className="text-blue-400 text-[10px] uppercase font-bold mb-1">Assigned Unit</p>
              <p className="text-white font-bold">{activeTrip.hospital?.name || activeTrip.policeStation?.name || 'Emergency Responder'}</p>
              <p className="text-slate-400 text-xs">{activeTrip.hospital?.phone || activeTrip.policeStation?.phone || 'Contacting...'}</p>
            </div>

            <GlowButton
              variant="danger"
              size="lg"
              className="w-full py-6 text-xl rounded-2xl shadow-lg shadow-red-600/20"
              onClick={() => {
                if (activeTrip.hospital?.phone) window.location.href = `tel:${activeTrip.hospital.phone}`;
                else if (activeTrip.policeStation?.phone) window.location.href = `tel:${activeTrip.policeStation.phone}`;
              }}
            >
              <Phone size={24} className="mr-3" />
              Call Responder
            </GlowButton>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col items-center justify-center p-6 relative overflow-hidden">
      {/* Background Decorative Elements */}
      <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-red-600/10 blur-[120px] rounded-full"></div>
      <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-blue-600/10 blur-[120px] rounded-full"></div>

      <div className="w-full max-w-md relative z-10 text-center">
        <div className="mb-12">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-blue-500/10 border border-blue-500/20 text-blue-400 text-xs font-bold uppercase tracking-wider mb-6">
            <span className="w-2 h-2 bg-blue-500 rounded-full animate-pulse"></span>
            SARATHI Emergency System
          </div>
          <h1 className="text-5xl font-black text-white mb-4 tracking-tighter">
            ONE-TAP<br/><span className="text-red-600">EMERGENCY</span>
          </h1>
          <p className="text-slate-400 text-lg">
            No login. No forms.<br/>Just enter your phone and tap.
          </p>
        </div>

        <div className="bg-slate-900 border border-white/10 rounded-[40px] p-8 shadow-2xl">
          {error && (
            <div className="mb-6 p-4 rounded-2xl bg-red-500/10 border border-red-500/20 text-red-500 text-sm flex items-start gap-3 text-left">
              <AlertCircle size={18} className="shrink-0 mt-0.5" />
              {error}
            </div>
          )}

          <div className="grid grid-cols-4 gap-3 mb-8">
            {[
              { id: 'medical', icon: Activity, color: 'text-emerald-500', bg: 'bg-emerald-500/10' },
              { id: 'accident', icon: Car, color: 'text-orange-500', bg: 'bg-orange-500/10' },
              { id: 'fire', icon: Flame, color: 'text-red-500', bg: 'bg-red-500/10' },
              { id: 'crime', icon: Shield, color: 'text-blue-500', bg: 'bg-blue-500/10' },
            ].map((type) => (
              <button
                key={type.id}
                onClick={() => setEmergencyType(type.id as any)}
                className={`flex flex-col items-center gap-2 p-4 rounded-3xl transition-all ${
                  emergencyType === type.id
                    ? `${type.bg} border-2 border-white/20 scale-110 shadow-lg`
                    : 'bg-transparent border-2 border-transparent opacity-40 hover:opacity-100'
                }`}
              >
                <type.icon className={type.color} size={28} />
                <span className="text-[10px] uppercase font-black text-white tracking-widest">{type.id}</span>
              </button>
            ))}
          </div>

          <div className="relative mb-6">
            <div className="absolute left-6 top-1/2 -translate-y-1/2 text-slate-500">
              <Phone size={24} />
            </div>
            <input
              type="tel"
              placeholder="Enter Phone Number"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              className="w-full bg-slate-800 border-2 border-white/5 rounded-3xl py-6 pl-16 pr-6 text-xl text-white font-bold placeholder:text-slate-600 focus:border-blue-500/50 outline-none transition-all shadow-inner"
            />
          </div>

          <GlowButton
            variant="danger"
            size="lg"
            className={`w-full py-8 text-2xl font-black rounded-3xl transition-all active:scale-95 shadow-xl shadow-red-600/30 ${
              isSubmitting ? 'animate-pulse' : ''
            }`}
            onClick={handleSendSOS}
            disabled={isSubmitting}
          >
            {isSubmitting ? 'PROCESSING...' : 'SEND SOS'}
          </GlowButton>
        </div>

        <div className="mt-12 text-slate-500 text-sm font-medium uppercase tracking-[0.2em]">
          Emergency Hotline: <span className="text-white font-black">112</span>
        </div>
      </div>
    </div>
  );
}
