'use client';

import { useState } from 'react';
import { Phone, AlertCircle, MapPin, Clock } from 'lucide-react';
import { GlowButton } from '@/components/shared';

export default function PublicSOSPage() {
  const [isEmergencyActive, setIsEmergencyActive] = useState(false);
  const [emergencyType, setEmergencyType] = useState('');
  const [location, setLocation] = useState<{
    lat: number;
    lng: number;
  } | null>(null);

  const emergencyTypes = [
    { id: 'medical', label: 'Medical Emergency', icon: '🏥' },
    { id: 'accident', label: 'Road Accident', icon: '🚗' },
    { id: 'fire', label: 'Fire', icon: '🔥' },
    { id: 'crime', label: 'Crime/Safety', icon: '🚨' },
  ];

  const handleEmergencySOS = (type: string) => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition((position) => {
        const emergencyLocation = {
          lat: position.coords.latitude,
          lng: position.coords.longitude,
        };
        setLocation(emergencyLocation);
        setEmergencyType(type);
        setIsEmergencyActive(true);
      });
    }
  };

  if (isEmergencyActive) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-red-950 via-red-900 to-orange-900 flex items-center justify-center p-4">
        <div className="w-full max-w-md">
          {/* Pulse Animation */}
          <div className="mb-8">
            <div className="relative w-32 h-32 mx-auto">
              <div className="absolute inset-0 bg-red-500 rounded-full animate-pulse"></div>
              <div className="absolute inset-4 bg-red-600 rounded-full animate-pulse" style={{ animationDelay: '0.2s' }}></div>
              <div className="absolute inset-8 bg-red-700 rounded-full flex items-center justify-center">
                <AlertCircle size={48} className="text-white" />
              </div>
            </div>
          </div>

          <div className="bg-white/10 backdrop-blur-md rounded-2xl border-2 border-red-500 p-8 text-center">
            <h1 className="text-3xl font-black text-white mb-2">EMERGENCY DISPATCHED</h1>
            <p className="text-orange-200 text-lg mb-6">{emergencyTypes.find(e => e.id === emergencyType)?.label}</p>

            <div className="space-y-4 mb-8">
              <div className="flex items-center justify-center gap-3 bg-red-600/30 p-4 rounded-lg">
                <MapPin size={20} className="text-orange-400" />
                <div className="text-left">
                  <p className="text-white/70 text-sm">Location</p>
                  <p className="text-white font-semibold">{location?.lat.toFixed(4)}, {location?.lng.toFixed(4)}</p>
                </div>
              </div>

              <div className="flex items-center justify-center gap-3 bg-blue-600/30 p-4 rounded-lg">
                <Clock size={20} className="text-blue-400" />
                <div className="text-left">
                  <p className="text-white/70 text-sm">ETA</p>
                  <p className="text-white font-semibold">07 mins away</p>
                </div>
              </div>
            </div>

            <GlowButton
              variant="danger"
              size="lg"
              onClick={() => setIsEmergencyActive(false)}
              className="w-full"
            >
              <Phone size={20} className="inline mr-2" />
              Stay Connected
            </GlowButton>

            <p className="text-white/60 text-xs mt-4">
              Keep your screen on. Driver will call you.
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-red-900 to-orange-900 p-4">
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <div className="text-center mb-12 pt-8">
          <h1 className="text-5xl font-black text-white mb-2">
            One-Tap Emergency
          </h1>
          <p className="text-orange-200 text-xl">
            Get immediate emergency response
          </p>
        </div>

        {/* Emergency Type Selection Grid */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-12">
          {emergencyTypes.map((type) => (
            <button
              key={type.id}
              onClick={() => handleEmergencySOS(type.id)}
              className="group relative overflow-hidden rounded-xl p-6 bg-white/10 border-2 border-white/20 hover:border-orange-400 transition-all hover:scale-105"
            >
              <div className="absolute inset-0 bg-gradient-to-br from-orange-500/0 to-red-600/0 group-hover:from-orange-500/20 group-hover:to-red-600/20 transition-all"></div>
              <div className="relative text-center">
                <div className="text-4xl mb-2">{type.icon}</div>
                <p className="text-white font-bold text-sm">{type.label}</p>
              </div>
            </button>
          ))}
        </div>

        {/* Features */}
        <div className="grid md:grid-cols-3 gap-6 mb-12">
          <div className="bg-white/10 backdrop-blur-md border border-white/20 rounded-xl p-6">
            <div className="text-3xl mb-3">⚡</div>
            <h3 className="text-white font-bold mb-2">Instant Dispatch</h3>
            <p className="text-white/70 text-sm">Nearest ambulance notified instantly</p>
          </div>

          <div className="bg-white/10 backdrop-blur-md border border-white/20 rounded-xl p-6">
            <div className="text-3xl mb-3">📍</div>
            <h3 className="text-white font-bold mb-2">Auto Location</h3>
            <p className="text-white/70 text-sm">Your location shared automatically</p>
          </div>

          <div className="bg-white/10 backdrop-blur-md border border-white/20 rounded-xl p-6">
            <div className="text-3xl mb-3">📞</div>
            <h3 className="text-white font-bold mb-2">Direct Contact</h3>
            <p className="text-white/70 text-sm">Driver calls you with ETA</p>
          </div>
        </div>

        {/* Emergency Numbers */}
        <div className="bg-gradient-to-r from-red-600 to-orange-600 rounded-xl p-6 text-center">
          <p className="text-white/90 text-sm mb-2">Emergency Hotline</p>
          <p className="text-4xl font-black text-white">112</p>
        </div>
      </div>
    </div>
  );
}
