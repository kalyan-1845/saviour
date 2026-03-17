'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Ambulance, Clock3, MapPinned, Phone, User, ShieldCheck, X } from 'lucide-react';
import { MapComponent } from '@/components/map/MapComponent';

type HospitalCase = {
  id: string;
  emergencyType: string;
  status: string;
  hospitalCaseStatus: 'pending' | 'registered' | 'ready';
  etaMinutes: number | null;
  user: {
    fullName: string;
    phone: string;
  } | null;
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

type HospitalSession = {
  id: string;
  name: string;
  phone: string;
};

export default function HospitalDashboardPage() {
  const [hospital, setHospital] = useState<HospitalSession | null>(null);
  const [cases, setCases] = useState<HospitalCase[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [updatingCaseId, setUpdatingCaseId] = useState('');
  const [selectedCaseId, setSelectedCaseId] = useState<string | null>(null);

  useEffect(() => {
    const raw = localStorage.getItem('currentHospital');
    if (!raw) {
      window.location.href = '/hospital-login';
      return;
    }
    try {
      const parsed = JSON.parse(raw) as HospitalSession;
      setHospital(parsed);
    } catch {
      localStorage.removeItem('currentHospital');
      window.location.href = '/hospital-login';
    }
  }, []);

  const fetchCases = useCallback(async () => {
    if (!hospital?.id) return;
    setError('');
    try {
      const response = await fetch(`/api/hospital/cases?hospitalId=${hospital.id}`);
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error || 'Failed to load cases.');
      }
      setCases(data.cases ?? []);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Failed to load cases.');
    } finally {
      setLoading(false);
    }
  }, [hospital?.id]);

  useEffect(() => {
    if (!hospital?.id) return;
    fetchCases();
    const interval = setInterval(fetchCases, 5000); // Faster polling for real-time
    return () => clearInterval(interval);
  }, [hospital?.id, fetchCases]);

  const activeCases = useMemo(
    () => cases.filter((item) => ['assigned', 'in-progress', 'accepted'].includes(item.status)),
    [cases]
  );

  const selectedCase = useMemo(
    () => cases.find(c => c.id === selectedCaseId),
    [cases, selectedCaseId]
  );

  const markers = useMemo(() => {
    if (!selectedCase) return [];
    const m = [];
    if (selectedCase.pickupLocation) {
      m.push({
        position: { lat: selectedCase.pickupLocation.latitude, lng: selectedCase.pickupLocation.longitude },
        title: 'Pickup Location',
      });
    }
    if (selectedCase.dropoffLocation) {
      m.push({
        position: { lat: selectedCase.dropoffLocation.latitude, lng: selectedCase.dropoffLocation.longitude },
        title: hospital?.name || 'Hospital',
      });
    }
    if (selectedCase.driver?.currentLocation?.coordinates) {
      m.push({
        position: {
          lat: selectedCase.driver.currentLocation.coordinates[1],
          lng: selectedCase.driver.currentLocation.coordinates[0]
        },
        title: `Ambulance: ${selectedCase.driver.fullName}`,
      });
    }
    return m;
  }, [selectedCase, hospital?.name]);

  async function updateCaseStatus(caseId: string, hospitalCaseStatus: 'registered' | 'ready') {
    setUpdatingCaseId(caseId);
    try {
      const response = await fetch('/api/hospital/cases', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tripId: caseId, hospitalCaseStatus }),
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error || 'Failed to update case.');
      }
      await fetchCases();
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Failed to update case.');
    } finally {
      setUpdatingCaseId('');
    }
  }

  function logout() {
    localStorage.removeItem('currentHospital');
    window.location.href = '/hospital-login';
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 text-white p-4 md:p-8">
      <div className="max-w-7xl mx-auto">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-8">
          <div>
            <h1 className="text-4xl font-black gradient-text">Emergency Reception</h1>
            <p className="text-slate-400 font-medium">{hospital?.name ?? 'Loading hospital...'}</p>
          </div>
          <button onClick={logout} className="px-5 py-2.5 bg-red-600/10 hover:bg-red-600 border border-red-500/50 rounded-xl font-bold transition-all active:scale-95">
            Logout Panel
          </button>
        </div>

        {error && <div className="mb-6 bg-red-900/30 border border-red-500/50 rounded-xl p-4 text-red-200 animate-in fade-in slide-in-from-top-2">{error}</div>}

        <div className="grid lg:grid-cols-3 gap-8">
          {/* Left Column: Active Cases List */}
          <div className="lg:col-span-1 space-y-4">
            <div className="flex items-center justify-between px-2">
              <h2 className="text-xl font-bold text-white flex items-center gap-2">
                <Ambulance className="text-emerald-400" size={20} />
                Incoming Cases
              </h2>
              <span className="px-2 py-0.5 bg-emerald-500/20 text-emerald-400 text-xs font-black rounded-lg">
                {activeCases.length} ACTIVE
              </span>
            </div>

            {loading && cases.length === 0 && <p className="text-slate-500 text-center py-10">Syncing with emergency network...</p>}
            {!loading && activeCases.length === 0 && (
              <div className="bg-slate-900/50 border border-slate-800 rounded-2xl p-8 text-center text-slate-500">
                No active incoming ambulance cases.
              </div>
            )}

            <div className="space-y-3 overflow-y-auto max-h-[70vh] pr-1">
              {activeCases.map((item) => (
                <div
                  key={item.id}
                  onClick={() => setSelectedCaseId(item.id)}
                  className={`group rounded-2xl border transition-all cursor-pointer p-5 space-y-3 ${selectedCaseId === item.id
                      ? 'bg-emerald-500/10 border-emerald-500 shadow-lg shadow-emerald-500/10'
                      : 'bg-slate-900/50 border-slate-800 hover:border-slate-700'
                    }`}
                >
                  <div className="flex justify-between items-start">
                    <div>
                      <p className="text-xs text-slate-500 uppercase font-black tracking-widest">Case: {item.id.slice(-6)}</p>
                      <p className="font-bold text-lg text-white group-hover:text-emerald-300 transition-colors uppercase">{item.emergencyType.replace('_', ' ')}</p>
                    </div>
                    <div className="text-right">
                      <p className="text-2xl font-black text-white">{item.etaMinutes ?? '-'}</p>
                      <p className="text-[10px] text-slate-500 uppercase font-bold">MIN ETA</p>
                    </div>
                  </div>

                  <div className="flex items-center gap-3 text-sm text-slate-300">
                    <div className="flex items-center gap-1.5 px-2.5 py-1 bg-white/5 rounded-lg border border-white/10">
                      <User size={14} className="text-slate-400" />
                      <span className="font-medium truncate max-w-[100px]">{item.user?.fullName ?? 'N/A'}</span>
                    </div>
                    <div className={`px-2.5 py-1 rounded-lg border font-bold text-[10px] uppercase tracking-tighter ${item.hospitalCaseStatus === 'ready'
                        ? 'bg-emerald-500/20 border-emerald-500/50 text-emerald-400'
                        : 'bg-amber-500/20 border-amber-500/50 text-amber-400'
                      }`}>
                      {item.hospitalCaseStatus}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Right Column: Case Details & Map */}
          <div className="lg:col-span-2 space-y-6">
            {!selectedCase ? (
              <div className="h-full min-h-[500px] flex flex-col items-center justify-center bg-slate-900/30 border border-slate-800 border-dashed rounded-3xl text-slate-500">
                <MapPinned size={48} className="mb-4 opacity-20" />
                <p className="font-medium">Select a case to view live tracking and manage prep</p>
              </div>
            ) : (
              <div className="space-y-6 animate-in fade-in zoom-in-95 duration-300">
                {/* Map Section */}
                <div className="rounded-3xl border border-slate-800 bg-black/50 overflow-hidden h-[450px] relative shadow-2xl">
                  <MapComponent
                    center={selectedCase.driver?.currentLocation?.coordinates
                      ? { lat: selectedCase.driver.currentLocation.coordinates[1], lng: selectedCase.driver.currentLocation.coordinates[0] }
                      : { lat: 17.3850, lng: 78.4867 }}
                    markers={markers}
                    onLocationChange={() => { }}
                  />
                  <div className="absolute top-5 left-5 right-5 flex justify-between items-start pointer-events-none">
                    <div className="bg-slate-950/80 backdrop-blur-md border border-slate-800 rounded-2xl px-4 py-3 pointer-events-auto flex items-center gap-3 shadow-xl">
                      <div className="w-10 h-10 rounded-xl bg-emerald-600 flex items-center justify-center">
                        <Ambulance size={24} />
                      </div>
                      <div>
                        <p className="text-white font-black text-sm uppercase">{selectedCase.driver?.vehicleNumber ?? 'LOCATING UNIT'}</p>
                        <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">{selectedCase.driver?.fullName ?? 'SEARCHING...'}</p>
                      </div>
                    </div>
                    <button
                      onClick={() => setSelectedCaseId(null)}
                      className="w-10 h-10 bg-slate-950/80 backdrop-blur-md border border-slate-800 rounded-full flex items-center justify-center text-slate-400 hover:text-white pointer-events-auto transition-colors"
                    >
                      <X size={20} />
                    </button>
                  </div>
                </div>

                {/* Patient & Actions Info */}
                <div className="grid md:grid-cols-2 gap-6">
                  <div className="bg-slate-900/50 border border-slate-800 rounded-2xl p-6 space-y-4">
                    <h3 className="text-sm font-black text-slate-500 uppercase tracking-widest flex items-center gap-2">
                      <User size={16} /> Patient Information
                    </h3>
                    <div className="space-y-3">
                      <div className="flex justify-between">
                        <span className="text-slate-400 text-sm">Full Name</span>
                        <span className="font-bold text-white">{selectedCase.user?.fullName ?? 'N/A'}</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-slate-400 text-sm">Contact</span>
                        <span className="font-bold text-white underline underline-offset-4 decoration-slate-700">{selectedCase.user?.phone ?? 'N/A'}</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-slate-400 text-sm">Emergency</span>
                        <span className="font-bold text-red-400 uppercase">{selectedCase.emergencyType.replace('_', ' ')}</span>
                      </div>
                    </div>
                    <div className="flex gap-2 pt-2">
                      <a href={`tel:${selectedCase.user?.phone}`} className="flex-1 bg-white/5 hover:bg-white/10 border border-white/10 py-3 rounded-xl text-center text-sm font-bold flex items-center justify-center gap-2 transition-colors">
                        <Phone size={14} /> Call Patient
                      </a>
                      <a href={`tel:${selectedCase.driver?.phone}`} className="flex-1 bg-white/5 hover:bg-white/10 border border-white/10 py-3 rounded-xl text-center text-sm font-bold flex items-center justify-center gap-2 transition-colors">
                        <Ambulance size={14} /> Driver
                      </a>
                    </div>
                  </div>

                  <div className="bg-slate-900/50 border border-slate-800 rounded-2xl p-6 space-y-4">
                    <h3 className="text-sm font-black text-slate-500 uppercase tracking-widest flex items-center gap-2">
                      <ShieldCheck size={16} /> Hospital Preparation
                    </h3>
                    <div className="space-y-3">
                      <button
                        onClick={() => updateCaseStatus(selectedCase.id, 'registered')}
                        disabled={updatingCaseId === selectedCase.id || selectedCase.hospitalCaseStatus !== 'pending'}
                        className="w-full flex items-center justify-between px-5 py-4 bg-emerald-600 hover:bg-emerald-500 disabled:bg-emerald-950/50 disabled:text-emerald-600/50 disabled:border-emerald-950 border border-emerald-500 rounded-2xl transition-all font-bold"
                      >
                        <span>Register Incoming Case</span>
                        {selectedCase.hospitalCaseStatus === 'registered' || selectedCase.hospitalCaseStatus === 'ready' ? <ShieldCheck size={20} /> : <div className="w-5 h-5 rounded-full border-2 border-emerald-200/30" />}
                      </button>

                      <button
                        onClick={() => updateCaseStatus(selectedCase.id, 'ready')}
                        disabled={updatingCaseId === selectedCase.id || selectedCase.hospitalCaseStatus === 'ready'}
                        className="w-full flex items-center justify-between px-5 py-4 bg-amber-600 hover:bg-amber-500 disabled:bg-amber-950/50 disabled:text-amber-600/50 disabled:border-amber-950 border border-amber-500 rounded-2xl transition-all font-bold"
                      >
                        <span>Mark Medical Team Ready</span>
                        {selectedCase.hospitalCaseStatus === 'ready' ? <ShieldCheck size={20} /> : <Clock3 size={20} className="text-amber-200/40" />}
                      </button>
                    </div>
                    <p className="text-[10px] text-slate-600 text-center font-bold uppercase">Status is synced instantly with driver's navigation app</p>
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
