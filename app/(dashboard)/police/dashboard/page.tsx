'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { AlertTriangle, ExternalLink, Siren, TrafficCone } from 'lucide-react';

type PoliceSession = {
  id: string;
  _id?: string;
  name: string;
  phone: string;
  jurisdiction: string;
  zone: string;
  city: string;
};

type PoliceAlert = {
  id: string;
  status: string;
  emergencyType: string;
  etaMinutes: number | null;
  policeAlertMessage: string | null;
  policeAlertMeta: {
    routeDistanceKm: number;
    congestionScore: number;
    congestionLevel: string;
    estimatedSignals: number;
    redSignalRisk: number;
    estimatedTimeToClearMins: number;
    generatedAt: string;
  } | null;
  hospitalName: string | null;
  user: { fullName: string; phone: string } | null;
  driver: { fullName: string; phone: string; vehicleNumber: string; liveMapUrl: string | null } | null;
  createdAt: string;
};

export default function PoliceDashboardPage() {
  const [station, setStation] = useState<PoliceSession | null>(null);
  const [alerts, setAlerts] = useState<PoliceAlert[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const raw = localStorage.getItem('currentPoliceStation');
    if (!raw) {
      window.location.href = '/police-login';
      return;
    }
    try {
      const parsed = JSON.parse(raw) as PoliceSession;
      const rawId = parsed.id ?? parsed._id ?? '';
      const normalized = {
        ...parsed,
        id:
          typeof rawId === 'string'
            ? rawId.trim()
            : rawId && typeof rawId === 'object' && '$oid' in (rawId as Record<string, unknown>)
              ? String((rawId as Record<string, unknown>).$oid ?? '').trim()
              : String(rawId).trim(),
      };
      if (!normalized.id) {
        throw new Error('Invalid station session');
      }
      localStorage.setItem('currentPoliceStation', JSON.stringify(normalized));
      setStation(normalized);
    } catch {
      localStorage.removeItem('currentPoliceStation');
      window.location.href = '/police-login';
    }
  }, []);

  const fetchAlerts = useCallback(async () => {
    if (!station?.id) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError('');
    try {
      const response = await fetch(`/api/police/alerts?stationId=${encodeURIComponent(station.id)}`, {
        cache: 'no-store',
      });
      const raw = await response.text();
      let data: { alerts?: PoliceAlert[]; error?: string } = {};
      try {
        data = raw ? (JSON.parse(raw) as { alerts?: PoliceAlert[]; error?: string }) : {};
      } catch {
        data = { error: raw || 'Unexpected server response.' };
      }
      if (!response.ok) {
        throw new Error(data.error || 'Failed to load police alerts.');
      }
      setAlerts(data.alerts ?? []);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Failed to load police alerts.');
    } finally {
      setLoading(false);
    }
  }, [station?.id]);

  useEffect(() => {
    if (!station?.id) return;
    fetchAlerts();
    const interval = setInterval(fetchAlerts, 6000);
    return () => clearInterval(interval);
  }, [fetchAlerts, station?.id]);

  const activeAlerts = useMemo(
    () => alerts.filter((item) => ['assigned', 'in-progress', 'accepted'].includes(item.status)),
    [alerts]
  );

  function logout() {
    localStorage.removeItem('currentPoliceStation');
    window.location.href = '/police-login';
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 text-white p-4 md:p-8">
      <div className="max-w-7xl mx-auto">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-8">
          <div>
            <h1 className="text-4xl font-black">Police Green Corridor Dashboard</h1>
            <p className="text-slate-300">
              {station?.name} | {station?.jurisdiction}
            </p>
          </div>
          <button onClick={logout} className="px-4 py-2 bg-red-600 hover:bg-red-700 rounded-lg font-semibold">
            Logout
          </button>
        </div>

        {error && <div className="mb-4 p-3 rounded-lg border border-red-700 bg-red-900/30 text-red-200">{error}</div>}

        <div className="mb-6 grid grid-cols-2 md:grid-cols-4 gap-3">
          <div className="bg-slate-800 border border-slate-700 rounded-lg p-3">
            <p className="text-slate-400 text-xs">Active Alerts</p>
            <p className="text-2xl font-bold">{activeAlerts.length}</p>
          </div>
          <div className="bg-slate-800 border border-slate-700 rounded-lg p-3">
            <p className="text-slate-400 text-xs">Total Alerts</p>
            <p className="text-2xl font-bold">{alerts.length}</p>
          </div>
          <div className="bg-slate-800 border border-slate-700 rounded-lg p-3">
            <p className="text-slate-400 text-xs">Zone</p>
            <p className="text-lg font-semibold">{station?.zone ?? 'N/A'}</p>
          </div>
          <div className="bg-slate-800 border border-slate-700 rounded-lg p-3">
            <p className="text-slate-400 text-xs">City</p>
            <p className="text-lg font-semibold">{station?.city ?? 'N/A'}</p>
          </div>
        </div>

        {loading && <p className="text-slate-300">Loading police route alerts...</p>}
        {!loading && alerts.length === 0 && (
          <div className="rounded-lg p-6 border border-slate-700 bg-slate-800/60 text-slate-300">
            No alerts received yet. Driver notifications will appear here.
          </div>
        )}

        <div className="space-y-4">
          {alerts.map((alert) => (
            <div key={alert.id} className="rounded-xl border border-slate-700 bg-slate-800/80 p-5 space-y-4">
              <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-2">
                <div>
                  <p className="text-xs text-slate-400">Alert ID</p>
                  <p className="font-semibold">{alert.id}</p>
                </div>
                <div className="flex items-center gap-2">
                  <span className="px-2 py-1 text-xs rounded bg-red-900/40 border border-red-700 text-red-200 uppercase">
                    {alert.emergencyType}
                  </span>
                  <span className="px-2 py-1 text-xs rounded bg-blue-900/40 border border-blue-700 text-blue-200 uppercase">
                    {alert.status}
                  </span>
                </div>
              </div>

              <div className="grid md:grid-cols-3 gap-3 text-sm">
                <div className="bg-slate-900/60 border border-slate-700 rounded-lg p-3">
                  <p className="text-slate-400 text-xs mb-1">Driver & Vehicle</p>
                  <p>{alert.driver?.fullName ?? 'N/A'}</p>
                  <p>{alert.driver?.vehicleNumber ?? 'N/A'}</p>
                  <p>{alert.driver?.phone ?? 'N/A'}</p>
                </div>
                <div className="bg-slate-900/60 border border-slate-700 rounded-lg p-3">
                  <p className="text-slate-400 text-xs mb-1">Patient</p>
                  <p>{alert.user?.fullName ?? 'N/A'}</p>
                  <p>{alert.user?.phone ?? 'N/A'}</p>
                  <p>ETA: {alert.etaMinutes ?? 'N/A'} min</p>
                </div>
                <div className="bg-slate-900/60 border border-slate-700 rounded-lg p-3">
                  <p className="text-slate-400 text-xs mb-1">Destination</p>
                  <p>{alert.hospitalName ?? 'N/A'}</p>
                  {alert.driver?.liveMapUrl && (
                    <a
                      href={alert.driver.liveMapUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="inline-flex items-center gap-1 text-blue-300 hover:text-blue-200 mt-1"
                    >
                      Live Ambulance
                      <ExternalLink size={12} />
                    </a>
                  )}
                </div>
              </div>

              {alert.policeAlertMeta && (
                <div className="grid md:grid-cols-5 gap-3 text-xs">
                  <div className="rounded-lg bg-slate-900/60 border border-slate-700 p-3">
                    <p className="text-slate-400">Route Distance</p>
                    <p className="font-semibold">{alert.policeAlertMeta.routeDistanceKm} km</p>
                  </div>
                  <div className="rounded-lg bg-slate-900/60 border border-slate-700 p-3">
                    <p className="text-slate-400">Congestion Score</p>
                    <p className="font-semibold">{alert.policeAlertMeta.congestionScore}/100</p>
                  </div>
                  <div className="rounded-lg bg-slate-900/60 border border-slate-700 p-3">
                    <p className="text-slate-400">Congestion Level</p>
                    <p className="font-semibold uppercase">{alert.policeAlertMeta.congestionLevel}</p>
                  </div>
                  <div className="rounded-lg bg-slate-900/60 border border-slate-700 p-3">
                    <p className="text-slate-400">Traffic Signals</p>
                    <p className="font-semibold">{alert.policeAlertMeta.estimatedSignals}</p>
                  </div>
                  <div className="rounded-lg bg-slate-900/60 border border-slate-700 p-3">
                    <p className="text-slate-400">Red Signal Risk</p>
                    <p className="font-semibold">{alert.policeAlertMeta.redSignalRisk}</p>
                  </div>
                </div>
              )}

              <div className="rounded-lg border border-orange-700 bg-orange-950/30 p-3">
                <p className="text-orange-200 text-sm font-semibold mb-2 flex items-center gap-2">
                  <TrafficCone size={15} />
                  Message Sent to Police Station
                </p>
                <pre className="text-xs text-orange-100 whitespace-pre-wrap font-mono">
                  {alert.policeAlertMessage ?? 'No message content available yet.'}
                </pre>
              </div>

              {alert.policeAlertMeta?.congestionLevel === 'severe' && (
                <div className="rounded-lg border border-red-700 bg-red-950/30 p-3 text-red-200 text-sm flex items-center gap-2">
                  <Siren size={16} />
                  High congestion route detected. Prioritize signal override and active corridor control.
                </div>
              )}

              {alert.policeAlertMeta?.congestionLevel === 'moderate' && (
                <div className="rounded-lg border border-amber-700 bg-amber-950/30 p-3 text-amber-200 text-sm flex items-center gap-2">
                  <AlertTriangle size={16} />
                  Moderate congestion. Deploy traffic unit at major intersections on route.
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
