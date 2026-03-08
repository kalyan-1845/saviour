'use client';

import { useEffect, useMemo, useState } from 'react';
import { AlertCircle, ExternalLink, MapIcon, Phone, Send, ShieldAlert, Siren, Volume2 } from 'lucide-react';
import { GlowButton } from '@/components/shared';
import { useLocation } from '@/hooks/useLocation';

type AssignedTrip = {
  id: string;
  status: string;
  emergencyType: string;
  hospitalCaseStatus?: 'pending' | 'registered' | 'ready';
  estimatedTime?: number | null;
  user?: {
    fullName?: string;
    phone?: string;
  };
  pickupLocation?: {
    latitude?: number;
    longitude?: number;
  };
  dropoffLocation?: {
    latitude?: number;
    longitude?: number;
  };
  hospitalName?: string;
};

type NotifyResponse = {
  success: boolean;
  hospital: { name: string; message: string };
  police: { stations: { name: string; phone: string; jurisdiction: string }[]; message: string };
  live: { driverLocationMap: string; etaMinutes: number | null };
};

export default function ActiveNavigationPage() {
  const [trip, setTrip] = useState<AssignedTrip | null>(null);
  const [loadingTrip, setLoadingTrip] = useState(true);
  const [error, setError] = useState('');
  const [isVoiceActive, setIsVoiceActive] = useState(false);
  const [eta, setEta] = useState(0);
  const [distanceKm, setDistanceKm] = useState(0);
  const [currentSpeed] = useState(0);
  const [sendingNotify, setSendingNotify] = useState(false);
  const [hospitalMessage, setHospitalMessage] = useState('');
  const [policeMessage, setPoliceMessage] = useState('');
  const [policeStations, setPoliceStations] = useState<{ name: string; phone: string; jurisdiction: string }[]>([]);
  const { location } = useLocation();

  async function loadAssignedTrip() {
    setLoadingTrip(true);
    setError('');
    try {
      const rawUser = localStorage.getItem('currentUser');
      const currentUser = rawUser ? (JSON.parse(rawUser) as { email?: string }) : null;
      if (!currentUser?.email) {
        throw new Error('Driver session missing. Please login again.');
      }

      const response = await fetch(`/api/driver/assigned?email=${encodeURIComponent(currentUser.email)}`);
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error || 'Failed to load assigned trip.');
      }
      setTrip(data.trip ?? null);
      if (typeof data.trip?.estimatedTime === 'number' && Number.isFinite(data.trip.estimatedTime)) {
        setEta(Math.max(1, Math.round(data.trip.estimatedTime)));
      }
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Failed to load route data.');
    } finally {
      setLoadingTrip(false);
    }
  }

  useEffect(() => {
    loadAssignedTrip();
    const interval = setInterval(loadAssignedTrip, 5000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (!location || !trip?.dropoffLocation?.latitude || !trip?.dropoffLocation?.longitude) return;
    const latDiff = Math.abs(location.latitude - trip.dropoffLocation.latitude);
    const lngDiff = Math.abs(location.longitude - trip.dropoffLocation.longitude);
    const approxDistance = Math.sqrt(latDiff * latDiff + lngDiff * lngDiff) * 111;
    const roundedDistance = Number(approxDistance.toFixed(1));
    setDistanceKm(roundedDistance);
    const avgSpeedKmph = 35;
    const calculatedEta = Math.max(1, Math.ceil((roundedDistance / avgSpeedKmph) * 60));
    setEta(calculatedEta);
  }, [location, trip?.dropoffLocation?.latitude, trip?.dropoffLocation?.longitude]);

  const navigationUrl = useMemo(() => {
    if (!trip?.dropoffLocation?.latitude || !trip?.dropoffLocation?.longitude) return null;
    if (!location) {
      return `https://www.google.com/maps/dir/?api=1&destination=${trip.dropoffLocation.latitude},${trip.dropoffLocation.longitude}&travelmode=driving`;
    }
    return `https://www.google.com/maps/dir/?api=1&origin=${location.latitude},${location.longitude}&destination=${trip.dropoffLocation.latitude},${trip.dropoffLocation.longitude}&travelmode=driving`;
  }, [location, trip?.dropoffLocation?.latitude, trip?.dropoffLocation?.longitude]);

  const pickupUrl = useMemo(() => {
    if (!trip?.pickupLocation?.latitude || !trip?.pickupLocation?.longitude) return null;
    return `https://www.google.com/maps?q=${trip.pickupLocation.latitude},${trip.pickupLocation.longitude}`;
  }, [trip?.pickupLocation?.latitude, trip?.pickupLocation?.longitude]);

  const hospitalUrl = useMemo(() => {
    if (!trip?.dropoffLocation?.latitude || !trip?.dropoffLocation?.longitude) return null;
    return `https://www.google.com/maps?q=${trip.dropoffLocation.latitude},${trip.dropoffLocation.longitude}`;
  }, [trip?.dropoffLocation?.latitude, trip?.dropoffLocation?.longitude]);

  const handleVoiceCommand = () => {
    setIsVoiceActive((prev) => !prev);
    if ('speechSynthesis' in window) {
      const text = trip?.hospitalName
        ? `Proceed carefully. Destination is ${trip.hospitalName}.`
        : 'Proceed carefully. Emergency routing is active.';
      window.speechSynthesis.speak(new SpeechSynthesisUtterance(text));
    }
  };

  async function sendNotifications() {
    if (!trip?.id) {
      setError('No active trip to notify.');
      return;
    }
    if (!location) {
      setError('Live location not available yet. Enable GPS and retry.');
      return;
    }

    setSendingNotify(true);
    setError('');
    try {
      const rawUser = localStorage.getItem('currentUser');
      const currentUser = rawUser ? (JSON.parse(rawUser) as { email?: string }) : null;
      if (!currentUser?.email) {
        throw new Error('Driver session missing. Please login again.');
      }

      const response = await fetch('/api/driver/notify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tripId: trip.id,
          driverEmail: currentUser.email,
          location: { latitude: location.latitude, longitude: location.longitude },
          etaMinutes: eta,
        }),
      });
      const data = (await response.json()) as NotifyResponse | { error?: string };
      if (!response.ok || !('success' in data && data.success)) {
        const msg = 'error' in data && data.error ? data.error : 'Failed to send notifications.';
        throw new Error(msg);
      }

      setHospitalMessage(data.hospital.message);
      setPoliceMessage(data.police.message);
      setPoliceStations(data.police.stations);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Failed to send notifications.');
    } finally {
      setSendingNotify(false);
    }
  }

  if (loadingTrip) {
    return (
      <div className="min-h-screen bg-gray-900 text-white flex items-center justify-center">
        Loading driver route interface...
      </div>
    );
  }

  return (
    <div className="h-screen w-full bg-gray-900 flex flex-col">
      <div className="flex-1 overflow-hidden bg-gradient-to-br from-slate-950 via-blue-950 to-slate-900 p-4 md:p-6">
        <div className="h-full grid lg:grid-cols-5 gap-4">
          <div className="lg:col-span-2 bg-red-600/90 backdrop-blur-md border border-red-300/50 rounded-xl p-4 text-white">
            <div className="flex items-center gap-2 mb-3">
              <AlertCircle size={20} className="text-orange-200" />
              <h3 className="font-bold text-xl">Emergency Route Active</h3>
            </div>
            <p className="text-sm leading-relaxed">
              {trip
                ? `Case ${trip.id} | ${String(trip.emergencyType).toUpperCase()} | Destination: ${trip.hospitalName ?? 'Not assigned'}`
                : 'No active trip assigned.'}
            </p>
            {trip?.hospitalCaseStatus && (
              <p className="mt-2 text-sm">
                Hospital Case Status:{' '}
                <span
                  className={`font-semibold ${
                    trip.hospitalCaseStatus === 'ready'
                      ? 'text-emerald-200'
                      : trip.hospitalCaseStatus === 'registered'
                        ? 'text-cyan-200'
                        : 'text-amber-100'
                  }`}
                >
                  {trip.hospitalCaseStatus.toUpperCase()}
                </span>
              </p>
            )}
            <div className="mt-4 pt-4 border-t border-red-300/40">
              <p className="text-xs uppercase tracking-wide text-red-100">Driver Actions</p>
              <p className="text-sm text-red-50 mt-1">Keep this tab open and send notifications while navigating.</p>
            </div>
          </div>

          <div className="lg:col-span-3 bg-black/30 border border-slate-700 rounded-xl p-4 md:p-5 space-y-4 relative">
            <div className="pr-16">
              <h3 className="text-xl font-bold text-white">Navigation Mode: External Google Maps</h3>
              <p className="text-slate-300 text-sm mt-1">
                Embedded map disabled for reliability. Use Google Maps directly for fastest live navigation.
              </p>
            </div>

            <div className="grid sm:grid-cols-2 xl:grid-cols-3 gap-3">
              {navigationUrl && (
                <a
                  href={navigationUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="px-4 py-3 bg-blue-600 hover:bg-blue-700 rounded-lg font-semibold text-white inline-flex items-center justify-between gap-2"
                >
                  Turn-by-Turn
                  <ExternalLink size={16} />
                </a>
              )}
              {pickupUrl && (
                <a
                  href={pickupUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="px-4 py-3 bg-cyan-700 hover:bg-cyan-800 rounded-lg font-semibold text-white inline-flex items-center justify-between gap-2"
                >
                  Pickup Location
                  <ExternalLink size={16} />
                </a>
              )}
              {hospitalUrl && (
                <a
                  href={hospitalUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="px-4 py-3 bg-violet-700 hover:bg-violet-800 rounded-lg font-semibold text-white inline-flex items-center justify-between gap-2"
                >
                  Hospital Location
                  <ExternalLink size={16} />
                </a>
              )}
            </div>

            <button
              onClick={handleVoiceCommand}
              className={`absolute top-4 right-4 w-12 h-12 rounded-full flex items-center justify-center transition-all ${
                isVoiceActive ? 'bg-blue-600 shadow-lg shadow-blue-500/50' : 'bg-white/15 hover:bg-white/25'
              }`}
              title="Voice Navigation"
            >
              <Volume2 size={22} className="text-white" />
            </button>
          </div>
        </div>
      </div>

      <div className="bg-gradient-to-t from-gray-950 to-gray-900 border-t-2 border-orange-500 p-4 md:p-6 space-y-4 max-h-[48vh] overflow-y-auto">
        <div className="grid grid-cols-3 gap-3">
          <div className="bg-white/10 rounded-lg p-3 text-center border border-white/20">
            <p className="text-white/70 text-xs font-semibold mb-1">ETA</p>
            <p className="text-white text-2xl font-black">{eta}</p>
            <p className="text-white/60 text-xs">mins</p>
          </div>
          <div className="bg-white/10 rounded-lg p-3 text-center border border-white/20">
            <p className="text-white/70 text-xs font-semibold mb-1">Distance</p>
            <p className="text-white text-2xl font-black">{distanceKm || '-'}</p>
            <p className="text-white/60 text-xs">km</p>
          </div>
          <div className="bg-white/10 rounded-lg p-3 text-center border border-white/20">
            <p className="text-white/70 text-xs font-semibold mb-1">Speed</p>
            <p className="text-white text-2xl font-black">{currentSpeed}</p>
            <p className="text-white/60 text-xs">km/h</p>
          </div>
        </div>

        <div className="bg-gradient-to-r from-orange-600/20 to-red-600/20 border-2 border-orange-500 rounded-lg p-4">
          <div className="flex items-start gap-3 mb-3">
            <MapIcon className="text-orange-400 flex-shrink-0 mt-1" size={20} />
            <div>
              <h3 className="text-white font-bold text-lg">{trip?.hospitalName ?? 'Hospital Not Assigned'}</h3>
              <p className="text-orange-200 text-sm">Emergency Type: {String(trip?.emergencyType ?? 'N/A').toUpperCase()}</p>
            </div>
          </div>
          <p className="text-white/80 text-sm pl-7">
            Patient: {trip?.user?.fullName ?? 'N/A'} ({trip?.user?.phone ?? 'N/A'})
          </p>
        </div>

        <div className="bg-slate-800/70 border border-slate-600 rounded-lg p-4 space-y-3">
          <h3 className="text-lg font-bold text-white flex items-center gap-2">
            <Siren size={18} className="text-red-400" />
            Traffic & Service Notification System
          </h3>
          <p className="text-slate-300 text-sm">
            Generate and send live emergency alerts to hospital and nearby police stations from this driver interface.
          </p>
          <div className="grid md:grid-cols-2 gap-3">
            <div className="bg-slate-900/70 border border-slate-700 rounded-lg p-3">
              <p className="text-xs text-emerald-300 mb-2 font-semibold">Hospital Message</p>
              <pre className="text-xs text-slate-200 whitespace-pre-wrap font-mono">
                {hospitalMessage || 'No message generated yet. Click "Send Notifications Now".'}
              </pre>
            </div>
            <div className="bg-slate-900/70 border border-slate-700 rounded-lg p-3">
              <p className="text-xs text-blue-300 mb-2 font-semibold">Police Message</p>
              <pre className="text-xs text-slate-200 whitespace-pre-wrap font-mono">
                {policeMessage || 'No message generated yet. Click "Send Notifications Now".'}
              </pre>
            </div>
          </div>

          {policeStations.length > 0 && (
            <div className="bg-blue-950/40 border border-blue-700 rounded-lg p-3">
              <p className="text-sm font-semibold text-blue-200 mb-2 flex items-center gap-2">
                <ShieldAlert size={15} />
                Notified Police Stations
              </p>
              <div className="grid md:grid-cols-3 gap-2 text-xs text-blue-100">
                {policeStations.map((station) => (
                  <div key={`${station.name}-${station.phone}`} className="bg-blue-900/30 rounded p-2 border border-blue-800">
                    <p className="font-semibold">{station.name}</p>
                    <p>{station.phone}</p>
                    <p>{station.jurisdiction}</p>
                  </div>
                ))}
              </div>
            </div>
          )}

          <div className="flex gap-3 flex-wrap">
            <button className="px-4 py-3 bg-white/10 hover:bg-white/20 border-2 border-white/20 text-white font-bold rounded-lg transition flex items-center justify-center gap-2">
              <Phone size={18} />
              <span>Call Hospital</span>
            </button>
            <GlowButton
              variant="danger"
              className="flex items-center justify-center gap-2"
              onClick={sendNotifications}
              disabled={sendingNotify || !trip}
            >
              <Send size={16} />
              <span>{sendingNotify ? 'Sending...' : 'Send Notifications Now'}</span>
            </GlowButton>
          </div>
          {error && <p className="text-red-300 text-sm">{error}</p>}
        </div>
      </div>
    </div>
  );
}
