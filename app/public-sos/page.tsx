'use client';

import { FormEvent, useEffect, useMemo, useState } from 'react';
import { AlertTriangle, Loader2, MapPin, Phone } from 'lucide-react';
import { useLocation } from '@/hooks/useLocation';
import { useI18n } from '@/components/shared/LanguageProvider';

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
    mapUrl?: string;
  } | null;
  policeStation?: {
    name: string;
    distanceKm: number;
    phone: string;
    mapUrl?: string;
  } | null;
  driver?: {
    id?: string;
    fullName: string;
    phone: string;
    vehicleNumber: string;
  } | null;
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
    pickupMapUrl?: string;
    hospitalMapUrl?: string;
  };
  driver?: {
    fullName: string;
    phone: string;
    vehicleNumber: string;
    currentLocation?: {
      mapUrl: string;
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
      <div className="max-w-xl mx-auto">
        <div className="rounded-2xl border border-red-500/30 bg-slate-900/70 p-6 md:p-8">
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

          {result && (
            <div className="mt-4 rounded-lg border border-emerald-600/50 bg-emerald-950/20 p-4 text-sm space-y-2">
              <p className="font-semibold text-emerald-300">{t('sos.success')}</p>
              <p>Trip ID: {result.tripId}</p>
              <p>Status: {result.status}</p>
              <p>ETA: {result.etaMinutes} min</p>
              {result.driver && <p>Driver: {result.driver.fullName} ({result.driver.phone})</p>}
              {result.hospital && <p>Hospital: {result.hospital.name}</p>}
              {result.policeStation && <p>Police: {result.policeStation.name}</p>}
              {result.hospital?.mapUrl && (
                <a href={result.hospital.mapUrl} target="_blank" rel="noreferrer" className="inline-block text-emerald-300 underline">
                  Open Hospital Location
                </a>
              )}
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

            {trackError && (
              <div className="mt-3 rounded-lg border border-red-600/50 bg-red-950/30 px-3 py-2 text-sm text-red-200">
                {trackError}
              </div>
            )}

            {trackResult && (
              <div className="mt-3 rounded-lg border border-blue-600/50 bg-blue-950/20 p-4 text-sm space-y-2">
                <p className="font-semibold text-blue-300">Live SOS Tracking</p>
                <p>Status: {trackResult.trip.status}</p>
                <p>Update: {trackResult.message}</p>
                {trackResult.trip.estimatedTime && <p>ETA: {trackResult.trip.estimatedTime} min</p>}
                {trackResult.driver && <p>Driver: {trackResult.driver.fullName} ({trackResult.driver.phone})</p>}
                {trackResult.trip.hospitalName && <p>Hospital: {trackResult.trip.hospitalName}</p>}
                {trackResult.trip.pickupMapUrl && (
                  <a
                    href={trackResult.trip.pickupMapUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="inline-block text-blue-300 underline"
                  >
                    Open Pickup Location Map
                  </a>
                )}
                {trackResult.trip.hospitalMapUrl && (
                  <a
                    href={trackResult.trip.hospitalMapUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="inline-block text-blue-300 underline ml-4"
                  >
                    Open Hospital Location
                  </a>
                )}
                {trackResult.driver?.currentLocation?.mapUrl && (
                  <a
                    href={trackResult.driver.currentLocation.mapUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="inline-block text-blue-300 underline ml-4"
                  >
                    Open Driver Live Location
                  </a>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
