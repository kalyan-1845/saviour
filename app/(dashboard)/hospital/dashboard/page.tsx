'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Ambulance, Clock3, MapPinned, Phone, User, ShieldCheck } from 'lucide-react';

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
    mapUrl: string | null;
  } | null;
  pickupMapUrl: string | null;
  hospitalMapUrl: string | null;
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
    setLoading(true);
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
    const interval = setInterval(fetchCases, 7000);
    return () => clearInterval(interval);
  }, [hospital?.id, fetchCases]);

  const activeCases = useMemo(
    () => cases.filter((item) => ['assigned', 'in-progress', 'accepted'].includes(item.status)),
    [cases]
  );

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
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 text-white p-4 md:p-8">
      <div className="max-w-7xl mx-auto">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-8">
          <div>
            <h1 className="text-4xl font-black">Hospital Emergency Desk</h1>
            <p className="text-slate-300">{hospital?.name ?? 'Loading hospital...'}</p>
          </div>
          <button onClick={logout} className="px-4 py-2 bg-red-600 hover:bg-red-700 rounded-lg font-semibold">
            Logout
          </button>
        </div>

        {error && <div className="mb-4 bg-red-900/40 border border-red-700 rounded-lg p-3 text-red-200">{error}</div>}

        <div className="mb-4 text-sm text-slate-300">
          Active cases: <span className="font-bold text-white">{activeCases.length}</span>
        </div>

        {loading && <p className="text-slate-300">Loading hospital cases...</p>}
        {!loading && activeCases.length === 0 && (
          <div className="bg-slate-800/60 border border-slate-700 rounded-xl p-6 text-slate-300">
            No active incoming ambulance cases for this hospital.
          </div>
        )}

        <div className="grid lg:grid-cols-2 gap-5">
          {activeCases.map((item) => (
            <div key={item.id} className="rounded-xl border border-slate-700 bg-slate-800/70 p-5 space-y-3">
              <div className="flex justify-between items-start gap-3">
                <div>
                  <p className="text-xs text-slate-400">Case ID</p>
                  <p className="font-semibold">{item.id}</p>
                </div>
                <div className="text-right">
                  <p className="text-xs text-slate-400">Emergency</p>
                  <p className="font-bold text-amber-300 uppercase">{item.emergencyType}</p>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3 text-sm">
                <div className="bg-slate-900/70 rounded-lg p-3">
                  <p className="text-slate-400">Trip Status</p>
                  <p className="font-semibold">{item.status}</p>
                </div>
                <div className="bg-slate-900/70 rounded-lg p-3">
                  <p className="text-slate-400">Case Prep</p>
                  <p className="font-semibold">{item.hospitalCaseStatus}</p>
                </div>
              </div>

              <div className="text-sm space-y-1">
                <p className="flex items-center gap-2"><Clock3 size={14} /> ETA: {item.etaMinutes ?? 'N/A'} min</p>
                <p className="flex items-center gap-2"><User size={14} /> User: {item.user?.fullName ?? 'N/A'} ({item.user?.phone ?? 'N/A'})</p>
                <p className="flex items-center gap-2"><Ambulance size={14} /> Driver: {item.driver?.fullName ?? 'N/A'} | Vehicle: {item.driver?.vehicleNumber ?? 'N/A'}</p>
                <p className="flex items-center gap-2"><Phone size={14} /> Driver Contact: {item.driver?.phone ?? 'N/A'}</p>
              </div>

              <div className="flex gap-3 flex-wrap">
                {item.driver?.mapUrl && (
                  <a href={item.driver.mapUrl} target="_blank" rel="noreferrer" className="px-3 py-2 bg-blue-600 hover:bg-blue-700 rounded-lg text-sm inline-flex items-center gap-2">
                    <MapPinned size={14} />
                    Live Ambulance
                  </a>
                )}
                {item.pickupMapUrl && (
                  <a href={item.pickupMapUrl} target="_blank" rel="noreferrer" className="px-3 py-2 bg-cyan-700 hover:bg-cyan-800 rounded-lg text-sm">
                    Pickup Location
                  </a>
                )}
                {item.hospitalMapUrl && (
                  <a href={item.hospitalMapUrl} target="_blank" rel="noreferrer" className="px-3 py-2 bg-violet-700 hover:bg-violet-800 rounded-lg text-sm">
                    Hospital Route
                  </a>
                )}
              </div>

              <div className="flex gap-2 flex-wrap pt-2 border-t border-slate-700">
                <button
                  onClick={() => updateCaseStatus(item.id, 'registered')}
                  disabled={updatingCaseId === item.id || item.hospitalCaseStatus !== 'pending'}
                  className="px-3 py-2 bg-emerald-600 hover:bg-emerald-700 disabled:bg-emerald-900 disabled:text-emerald-200 rounded-lg text-sm inline-flex items-center gap-2"
                >
                  <ShieldCheck size={14} />
                  Register Case
                </button>
                <button
                  onClick={() => updateCaseStatus(item.id, 'ready')}
                  disabled={updatingCaseId === item.id || item.hospitalCaseStatus === 'ready'}
                  className="px-3 py-2 bg-amber-600 hover:bg-amber-700 disabled:bg-amber-900 disabled:text-amber-200 rounded-lg text-sm"
                >
                  Mark Team Ready
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
