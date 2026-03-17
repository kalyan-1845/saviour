'use client';

import { FormEvent, useEffect, useMemo, useState } from 'react';
import { AlertTriangle, Loader2, MapPin, Phone } from 'lucide-react';
import { useLocation } from '@/hooks/useLocation';
import { useI18n } from '@/components/shared/LanguageProvider';
import { MapComponent } from '@/components/map/MapComponent';

type SosResponse = {
  success: boolean;
  tripId: string;
  status: string;
  etaMinutes: number;
  message?: string;
  hospital?: {
    name: string;
    distanceKm: number;
    phone: string;
    latitude: number;
    longitude: number;
  } | null;
  policeStation?: {
    name: string;
    distanceKm: number;
    phone: string;
    latitude: number;
    longitude: number;
  } | null;
  driver?: {
    id?: string;
    fullName: string;
    phone: string;
    vehicleNumber: string;
  } | null;
  location?: {
    latitude: number;
    longitude: number;
  };
};

type TrackResponse = {
  success: boolean;
  message: string;
  trip: {
    id: string;
    status: string;
    emergencyType: string;
    estimatedTime?: number;
    hospitalName?: string;
    policeStationName?: string;
    pickupLocation?: { latitude: number; longitude: number };
    dropoffLocation?: { latitude: number; longitude: number };
  };
  driver?: {
    fullName: string;
    phone: string;
    vehicleNumber: string;
    currentLocation?: {
      latitude: number;
      longitude: number;
    } | null;
  } | null;
};

const DEFAULT_PHONE_KEY = 'sarathi_phone';
const AUTH_KEY = 'sarathi_auth_state';

export default function PublicSosPage() {
  const { t } = useI18n();
  const { location, error: locationError, isLoading: locationLoading } = useLocation();
  const [phone, setPhone] = useState('');
  const [loading, setLoading] = useState(false);
  const [trackLoading, setTrackLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [trackError, setTrackError] = useState<string | null>(null);
  const [result, setResult] = useState<SosResponse | null>(null);
  const [trackPhone, setTrackPhone] = useState('');
  const [trackResult, setTrackResult] = useState<TrackResponse | null>(null);

  useEffect(() => {
    const savedPhone = localStorage.getItem(DEFAULT_PHONE_KEY);
    if (savedPhone) {
      setPhone(savedPhone);
      setTrackPhone(savedPhone);
    }
  }, []);

  useEffect(() => {
    const activeTripId = result?.tripId ?? trackResult?.trip.id;
    if (!activeTripId) return;

    const interval = setInterval(async () => {
      try {
        const response = await fetch(`/api/emergency/track?tripId=${activeTripId}`);
        const data = (await response.json()) as TrackResponse | { error?: string };
        if (response.ok && 'success' in data && data.success) {
          setTrackResult(data);
        }
      } catch {
        // Silent polling failure for demo stability.
      }
    }, 8000);

    return () => clearInterval(interval);
  }, [result?.tripId, trackResult?.trip.id]);

  const canSend = useMemo(
    () => phone.trim().length >= 10 && Boolean(location) && !loading,
    [phone, location, loading]
  );

  const markers = useMemo(() => {
    const m = [];

    // User current Location
    if (location) {
      m.push({
        position: { lat: location.latitude, lng: location.longitude },
        title: 'Your Location (Current)',
      });
    }

    // Pickup location from trip data
    const tripData = trackResult?.trip || result;
    if (tripData && 'pickupLocation' in tripData && tripData.pickupLocation) {
      m.push({
        position: { lat: tripData.pickupLocation.latitude, lng: tripData.pickupLocation.longitude },
        title: 'SOS Pickup Point',
      });
    } else if (result?.location) {
      m.push({
        position: { lat: result.location.latitude, lng: result.location.longitude },
        title: 'SOS Pickup Point',
      });
    }

    // Dropoff location
    if (trackResult?.trip.dropoffLocation) {
      m.push({
        position: { lat: trackResult.trip.dropoffLocation.latitude, lng: trackResult.trip.dropoffLocation.longitude },
        title: trackResult.trip.hospitalName || 'Destination',
      });
    } else if (result?.hospital?.latitude) {
      m.push({
        position: { lat: result.hospital.latitude, lng: result.hospital.longitude },
        title: result.hospital.name,
      });
    }

    // Driver location
    if (trackResult?.driver?.currentLocation) {
      m.push({
        position: { lat: trackResult.driver.currentLocation.latitude, lng: trackResult.driver.currentLocation.longitude },
        title: `Ambulance: ${trackResult.driver.fullName}`,
      });
    }

    return m;
  }, [location, result, trackResult]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setResult(null);

    if (!location) {
      setError('Location is required. Please enable GPS.');
      return;
    }

    const normalizedPhone = phone.replace(/[^\d+]/g, '').trim();
    if (normalizedPhone.length < 10) {
      setError('Enter a valid phone number.');
      return;
    }

    setLoading(true);
    localStorage.setItem(DEFAULT_PHONE_KEY, normalizedPhone);

    try {
      const response = await fetch('/api/emergency/sos', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          phone: normalizedPhone,
          latitude: location.latitude,
          longitude: location.longitude,
          emergencyType: 'medical',
        }),
      });

      const data = (await response.json()) as SosResponse | { error?: string };
      if (!response.ok || !('success' in data && data.success)) {
        const message = 'error' in data && data.error ? data.error : 'Failed to send SOS.';
        throw new Error(message);
      }

      localStorage.setItem(
        AUTH_KEY,
        JSON.stringify({
          phone: normalizedPhone,
          isLoggedIn: true,
          lastTripId: data.tripId,
          loggedInAt: Date.now(),
        })
      );

      setResult(data);
      setTrackResult(null);
    } catch (requestError) {
      const message = requestError instanceof Error ? requestError.message : 'Failed to send SOS.';
      setError(message);
    } finally {
      setLoading(false);
    }
  }

  async function handleTrackExistingSos(event: FormEvent) {
    event.preventDefault();
    setTrackError(null);
    setTrackLoading(true);
    try {
      const normalizedPhone = trackPhone.replace(/[^\d+]/g, '').trim();
      if (normalizedPhone.length < 10) {
        throw new Error('Enter a valid phone number to track.');
      }

      const response = await fetch(`/api/emergency/track?phone=${encodeURIComponent(normalizedPhone)}`);
      const data = (await response.json()) as TrackResponse | { error?: string };
      if (!response.ok || !('success' in data && data.success)) {
        const message = 'error' in data && data.error ? data.error : 'Failed to track SOS.';
        throw new Error(message);
      }

      setTrackResult(data);
    } catch (trackingError) {
      const message = trackingError instanceof Error ? trackingError.message : 'Failed to track SOS.';
      setTrackError(message);
    } finally {
      setTrackLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-slate-950 text-white px-4 py-10">
      <div className="max-w-4xl mx-auto grid lg:grid-cols-2 gap-6">
        <div className="rounded-2xl border border-red-500/30 bg-slate-900/70 p-6 md:p-8 flex flex-col h-fit">
          <div className="flex items-center gap-3 mb-6">
            <div className="h-12 w-12 rounded-xl bg-red-600 flex items-center justify-center">
              <AlertTriangle className="h-6 w-6" />
            </div>
            <div>
              <h1 className="text-2xl font-black">{t('sos.title')}</h1>
              <p className="text-sm text-slate-300">{t('sos.subtitle')}</p>
            </div>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <label className="block">
              <span className="text-sm text-slate-300">{t('sos.phone')}</span>
              <div className="mt-2 flex items-center gap-2 rounded-lg border border-slate-700 bg-slate-950 px-3 py-3">
                <Phone className="h-4 w-4 text-slate-400" />
                <input
                  type="tel"
                  inputMode="tel"
                  autoComplete="tel"
                  placeholder="+91 98765 43210"
                  value={phone}
                  onChange={(event) => setPhone(event.target.value)}
                  className="w-full bg-transparent outline-none text-base"
                />
              </div>
            </label>

            <div className="rounded-lg border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-slate-300 flex items-center gap-2">
              <MapPin className="h-4 w-4" />
              {locationLoading && 'Detecting live location...'}
              {!locationLoading && location && `Live location active (${location.latitude.toFixed(4)}, ${location.longitude.toFixed(4)})`}
              {!locationLoading && !location && 'Location unavailable'}
            </div>

            <button
              type="submit"
              disabled={!canSend}
              className="w-full rounded-xl bg-red-600 hover:bg-red-500 disabled:bg-slate-700 disabled:text-slate-300 transition-colors py-4 text-lg font-bold"
            >
              {loading ? (
                <span className="inline-flex items-center gap-2">
                  <Loader2 className="h-5 w-5 animate-spin" />
                  {t('sos.sending')}
                </span>
              ) : (
                t('sos.send')
              )}
            </button>
          </form>

          {(error || locationError) && (
            <div className="mt-4 rounded-lg border border-red-600/50 bg-red-950/30 px-3 py-2 text-sm text-red-200">
              {error ?? locationError}
            </div>
          )}

          <div className="mt-6 border-t border-slate-700 pt-6">
            <h2 className="text-lg font-bold mb-2">{t('sos.trackTitle')}</h2>
            <form onSubmit={handleTrackExistingSos} className="space-y-3">
              <div className="flex items-center gap-2 rounded-lg border border-slate-700 bg-slate-950 px-3 py-3">
                <Phone className="h-4 w-4 text-slate-400" />
                <input
                  type="tel"
                  inputMode="tel"
                  placeholder="Enter same phone number"
                  value={trackPhone}
                  onChange={(event) => setTrackPhone(event.target.value)}
                  className="w-full bg-transparent outline-none text-base"
                />
              </div>
              <button
                type="submit"
                disabled={trackLoading}
                className="w-full rounded-xl bg-blue-600 hover:bg-blue-500 disabled:bg-slate-700 transition-colors py-3 text-base font-bold"
              >
                {trackLoading ? t('sos.tracking') : t('sos.track')}
              </button>
            </form>
            {trackError && <p className="mt-2 text-red-400 text-sm">{trackError}</p>}
          </div>
        </div>

        <div className="flex flex-col gap-6">
          <div className="rounded-2xl border border-slate-700 bg-black/50 overflow-hidden min-h-[400px] relative">
            <MapComponent
              center={location ? { lat: location.latitude, lng: location.longitude } : { lat: 17.3850, lng: 78.4867 }}
              markers={markers}
              onLocationChange={() => { }}
            />
            <div className="absolute top-4 left-4 bg-slate-900/80 backdrop-blur-md border border-slate-700 rounded-lg px-3 py-2 text-xs font-bold text-white z-10">
              Live SOS Tracking Map
            </div>
          </div>

          {(result || trackResult) && (
            <div className="rounded-2xl border border-emerald-500/30 bg-emerald-950/10 p-6 space-y-4 animate-in fade-in slide-in-from-bottom-4 duration-500">
              <div className="flex justify-between items-center">
                <h3 className="text-xl font-bold text-emerald-300">
                  {trackResult ? 'Active SOS Update' : 'Emergency Dispatched'}
                </h3>
                <div className="px-3 py-1 bg-emerald-500/20 rounded-full text-emerald-400 text-xs font-black uppercase tracking-widest">
                  {trackResult?.trip.status || result?.status}
                </div>
              </div>

              <div className="grid md:grid-cols-2 gap-4">
                <div className="p-3 bg-white/5 rounded-lg border border-white/10">
                  <p className="text-xs text-slate-400 mb-1">Estimated Arrival</p>
                  <p className="text-2xl font-black text-white">{trackResult?.trip.estimatedTime ?? result?.etaMinutes ?? '-'} <span className="text-sm font-normal text-slate-400">mins</span></p>
                </div>
                <div className="p-3 bg-white/5 rounded-lg border border-white/10">
                  <p className="text-xs text-slate-400 mb-1">Ambulance Unit</p>
                  <p className="text-lg font-bold text-white truncate">{trackResult?.driver?.fullName ?? result?.driver?.fullName ?? 'Locating...'}</p>
                  <p className="text-xs text-slate-500">{trackResult?.driver?.vehicleNumber ?? result?.driver?.vehicleNumber ?? ''}</p>
                </div>
              </div>

              <div className="space-y-2 border-t border-white/10 pt-4">
                {trackResult?.message && <p className="text-emerald-200 text-sm italic">"{trackResult.message}"</p>}
                <p className="text-sm">Assigned Hospital: <span className="font-bold">{trackResult?.trip.hospitalName ?? result?.hospital?.name ?? 'Assessing...'}</span></p>
                {trackResult?.trip.policeStationName && <p className="text-sm text-blue-300">Police Support: <span className="font-bold">{trackResult.trip.policeStationName}</span></p>}
              </div>

              <div className="flex gap-2">
                {(result?.driver?.phone || trackResult?.driver?.phone) && (
                  <a
                    href={`tel:${trackResult?.driver?.phone || result?.driver?.phone}`}
                    className="flex-1 flex items-center justify-center gap-2 bg-emerald-600 hover:bg-emerald-500 py-3 rounded-xl font-bold transition-transform active:scale-95"
                  >
                    <Phone size={18} />
                    Call Ambulance
                  </a>
                )}
                {(result?.hospital?.phone) && (
                  <a
                    href={`tel:${result.hospital.phone}`}
                    className="flex-1 border border-emerald-600 text-emerald-400 py-3 rounded-xl font-bold text-center"
                  >
                    Hospital Contact
                  </a>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
