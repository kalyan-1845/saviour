'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { AlertTriangle, Siren, TrafficCone, X, MapPinned, Ambulance, ShieldAlert, Phone } from 'lucide-react';
import { MapComponent } from '@/components/map/MapComponent';

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
  driver: {
    fullName: string;
    phone: string;
    vehicleNumber: string;
    currentLocation?: {
      coordinates: [number, number];
    };
  } | null;
  pickupLocation?: { latitude: number; longitude: number };
  dropoffLocation?: { latitude: number; longitude: number };
  createdAt: string;
};

export default function PoliceDashboardPage() {
  const [station, setStation] = useState<PoliceSession | null>(null);
  const [alerts, setAlerts] = useState<PoliceAlert[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedAlertId, setSelectedAlertId] = useState<string | null>(null);

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
        id: String(rawId).trim(),
      };
      setStation(normalized);
    } catch {
      localStorage.removeItem('currentPoliceStation');
      window.location.href = '/police-login';
    }
  }, []);

  const fetchAlerts = useCallback(async () => {
    if (!station?.id) return;
    try {
      const response = await fetch(`/api/police/alerts?stationId=${encodeURIComponent(station.id)}`);
      const data = await response.json();
      if (!response.ok) throw new Error(data.error || 'Failed to load alerts.');
      setAlerts(data.alerts ?? []);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Failed to load alerts.');
    } finally {
      setLoading(false);
    }
  }, [station?.id]);

  useEffect(() => {
    if (!station?.id) return;
    fetchAlerts();
    const interval = setInterval(fetchAlerts, 5000);
    return () => clearInterval(interval);
  }, [fetchAlerts, station?.id]);

  const selectedAlert = useMemo(
    () => alerts.find(a => a.id === selectedAlertId),
    [alerts, selectedAlertId]
  );

  const markers = useMemo(() => {
    if (!selectedAlert) return [];
    const m = [];
    if (selectedAlert.pickupLocation) {
      m.push({
        position: { lat: selectedAlert.pickupLocation.latitude, lng: selectedAlert.pickupLocation.longitude },
        title: 'Emergency Origin',
      });
    }
    if (selectedAlert.dropoffLocation) {
      m.push({
        position: { lat: selectedAlert.dropoffLocation.latitude, lng: selectedAlert.dropoffLocation.longitude },
        title: selectedAlert.hospitalName || 'Destination Hospital',
      });
    }
    if (selectedAlert.driver?.currentLocation?.coordinates) {
      m.push({
        position: {
          lat: selectedAlert.driver.currentLocation.coordinates[1],
          lng: selectedAlert.driver.currentLocation.coordinates[0]
        },
        title: `Emergency Unit: ${selectedAlert.driver.fullName}`,
      });
    }
    return m;
  }, [selectedAlert]);

  function logout() {
    localStorage.removeItem('currentPoliceStation');
    window.location.href = '/police-login';
  }

  return (
    <div className="min-h-screen bg-slate-950 text-white p-4 md:p-8">
      <div className="max-w-7xl mx-auto">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-8">
          <div>
            <h1 className="text-4xl font-black text-blue-500">Green Corridor Command</h1>
            <p className="text-slate-400 font-bold uppercase tracking-widest text-sm">
              {station?.name} Dispatch | {station?.jurisdiction}
            </p>
          </div>
          <button onClick={logout} className="px-5 py-2.5 bg-blue-600/10 hover:bg-blue-600 border border-blue-500/50 rounded-xl font-bold transition-all">
            Logout Console
          </button>
        </div>

        {error && <div className="mb-6 p-4 rounded-xl border border-red-500/50 bg-red-950/30 text-red-200">{error}</div>}

        <div className="grid lg:grid-cols-3 gap-8">
          {/* Alerts Sidebar */}
          <div className="lg:col-span-1 space-y-4">
            <div className="flex items-center justify-between px-2">
              <h2 className="text-xl font-bold flex items-center gap-2">
                <Siren className="text-blue-400" size={20} />
                Active Alerts
              </h2>
              <span className="px-2 py-0.5 bg-blue-500/20 text-blue-400 text-xs font-black rounded-lg">
                {alerts.filter(a => ['assigned', 'in-progress', 'accepted'].includes(a.status)).length} LIVE
              </span>
            </div>

            <div className="space-y-3 overflow-y-auto max-h-[75vh] pr-1">
              {loading && alerts.length === 0 && <p className="text-slate-500 text-center py-10 italic">Initializing traffic sync...</p>}
              {!loading && alerts.length === 0 && (
                <div className="bg-slate-900/50 border border-slate-800 rounded-2xl p-8 text-center text-slate-500">
                  No active corridor alerts.
                </div>
              )}

              {alerts.map((alert) => (
                <div
                  key={alert.id}
                  onClick={() => setSelectedAlertId(alert.id)}
                  className={`group rounded-2xl border transition-all cursor-pointer p-5 space-y-3 ${selectedAlertId === alert.id
                    ? 'bg-blue-500/10 border-blue-500 shadow-lg shadow-blue-500/10'
                    : 'bg-slate-900/50 border-slate-800 hover:border-slate-700 shadow-md'
                    }`}
                >
                  <div className="flex justify-between items-start">
                    <div>
                      <div className="flex gap-2 items-center mb-1">
                        <span className={`w-2 h-2 rounded-full animate-pulse ${alert.policeAlertMeta?.congestionLevel === 'severe' ? 'bg-red-500' : 'bg-blue-500'}`} />
                        <p className="text-[10px] text-slate-500 uppercase font-black tracking-widest">UNIT: {alert.id.slice(-6)}</p>
                      </div>
                      <p className="font-bold text-lg text-white group-hover:text-blue-300 transition-colors uppercase">{alert.emergencyType.replace('_', ' ')}</p>
                    </div>
                    <div className="text-right">
                      <p className="text-xl font-black text-white">{alert.etaMinutes ?? '-'}</p>
                      <p className="text-[10px] text-slate-500 uppercase font-bold">ETA MIN</p>
                    </div>
                  </div>
                  <div className="flex justify-between items-center pt-2">
                    <span className="text-xs text-slate-400">{alert.hospitalName}</span>
                    <div className={`px-2 py-0.5 rounded text-[9px] font-black uppercase tracking-tighter ${alert.policeAlertMeta?.congestionLevel === 'severe' ? 'bg-red-500/20 text-red-400' : 'bg-emerald-500/20 text-emerald-400'
                      }`}>
                      {alert.policeAlertMeta?.congestionLevel || 'Light'} Traffic
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Main Content Area */}
          <div className="lg:col-span-2 space-y-6">
            {!selectedAlert ? (
              <div className="h-full min-h-[500px] flex flex-col items-center justify-center bg-slate-900/30 border border-slate-800 border-dashed rounded-3xl text-slate-500">
                <ShieldAlert size={48} className="mb-4 opacity-20" />
                <p className="font-medium">Direct signal control enabled. Select unit to track live.</p>
              </div>
            ) : (
              <div className="space-y-6 animate-in fade-in zoom-in-95 duration-300">
                {/* Map */}
                <div className="rounded-3xl border border-slate-800 bg-black/50 overflow-hidden h-[450px] relative shadow-2xl">
                  <MapComponent
                    center={selectedAlert.driver?.currentLocation?.coordinates
                      ? { lat: selectedAlert.driver.currentLocation.coordinates[1], lng: selectedAlert.driver.currentLocation.coordinates[0] }
                      : { lat: 17.3850, lng: 78.4867 }}
                    markers={markers}
                    onLocationChange={() => { }}
                  />
                  <div className="absolute top-5 left-5 bg-slate-950/90 backdrop-blur-xl border border-blue-500/30 rounded-2xl px-5 py-4 flex items-center gap-4 shadow-2xl z-10">
                    <div className="w-12 h-12 rounded-xl bg-blue-600 flex items-center justify-center shadow-lg shadow-blue-500/40">
                      <Siren size={28} className="text-white" />
                    </div>
                    <div>
                      <p className="text-white font-black text-lg uppercase leading-none mb-1">{selectedAlert.driver?.vehicleNumber ?? 'NAV-UNIT'}</p>
                      <p className="text-[10px] text-blue-400 font-bold uppercase tracking-widest">Live corridor tracking active</p>
                    </div>
                  </div>
                  <button
                    onClick={() => setSelectedAlertId(null)}
                    className="absolute top-5 right-5 w-10 h-10 bg-slate-950/90 backdrop-blur-md border border-slate-800 rounded-full flex items-center justify-center text-slate-400 hover:text-white z-10 transition-colors"
                  >
                    <X size={20} />
                  </button>
                </div>

                {/* Analysis Details */}
                <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-4">
                  {[
                    { label: 'Congestion', value: `${selectedAlert.policeAlertMeta?.congestionScore}%`, sub: selectedAlert.policeAlertMeta?.congestionLevel, color: 'text-red-400' },
                    { label: 'Distance', value: `${selectedAlert.policeAlertMeta?.routeDistanceKm} km`, sub: 'Remaining', color: 'text-blue-400' },
                    { label: 'Signals', value: selectedAlert.policeAlertMeta?.estimatedSignals, sub: 'on route', color: 'text-amber-400' },
                    { label: 'Risk', value: selectedAlert.policeAlertMeta?.redSignalRisk, sub: 'high latency', color: 'text-orange-400' },
                  ].map((stat, i) => (
                    <div key={i} className="bg-slate-900 border border-slate-800 rounded-2xl p-4 text-center">
                      <p className="text-[10px] font-black text-slate-500 uppercase tracking-widest mb-1">{stat.label}</p>
                      <p className={`text-2xl font-black ${stat.color}`}>{stat.value}</p>
                      <p className="text-[10px] font-bold text-slate-600 uppercase pt-1">{stat.sub}</p>
                    </div>
                  ))}
                </div>

                <div className="rounded-2xl border border-orange-500/30 bg-orange-500/5 p-6 space-y-4">
                  <div className="flex items-center gap-3">
                    <TrafficCone className="text-orange-500" />
                    <h3 className="font-bold text-lg text-orange-200 uppercase tracking-tight">Active Dispatch Message</h3>
                  </div>
                  <div className="p-4 bg-black/40 rounded-xl border border-white/5">
                    <pre className="text-xs text-orange-100/80 whitespace-pre-wrap font-mono leading-relaxed">
                      {selectedAlert.policeAlertMessage ?? 'Establishing secure channel...'}
                    </pre>
                  </div>
                  <div className="flex gap-3">
                    <button className="flex-1 bg-orange-600 hover:bg-orange-500 text-white font-bold py-4 rounded-xl transition-all shadow-lg active:scale-95 flex items-center justify-center gap-2">
                      <Siren size={18} />
                      Override Signals
                    </button>
                    <button className="flex-1 border border-white/10 hover:bg-white/5 py-4 rounded-xl font-bold transition-all flex items-center justify-center gap-2">
                      <Phone size={18} />
                      Contact Unit
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
