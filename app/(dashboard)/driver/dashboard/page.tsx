'use client';

import { useEffect, useState } from 'react';
import { AlertTriangle, Heart, Lightbulb, Pill, Zap, Activity, ExternalLink } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { GlowButton } from '@/components/shared';

type AssignedTrip = {
  id: string;
  status: string;
  emergencyType: string;
  hospitalCaseStatus?: 'pending' | 'registered' | 'ready';
  hospitalCaseRegisteredAt?: string | null;
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
  policeStationName?: string;
  mapUrl?: string | null;
  hospitalMapUrl?: string | null;
};

interface Emergency {
  id: string;
  name: string;
  icon: React.ReactNode;
  description: string;
  color: string;
  protocols: string[];
}

const CARD_TO_TRIP_EMERGENCY: Record<string, string> = {
  cardiac: 'heart_attack',
  trauma: 'accident',
  general: 'medical',
  pediatric: 'pediatric',
  stroke: 'stroke',
  burn: 'burn',
};

const TRIP_TO_CARD_EMERGENCY: Record<string, string> = {
  heart_attack: 'cardiac',
  accident: 'trauma',
  medical: 'general',
  pediatric: 'pediatric',
  stroke: 'stroke',
  burn: 'burn',
};

export default function DriverDashboardPage() {
  const router = useRouter();
  const [selectedEmergency, setSelectedEmergency] = useState<string | null>(null);
  const [assignedTrip, setAssignedTrip] = useState<AssignedTrip | null>(null);
  const [loadingTrip, setLoadingTrip] = useState(true);
  const [tripError, setTripError] = useState('');
  const [updating, setUpdating] = useState(false);
  const [updatingEmergency, setUpdatingEmergency] = useState(false);


  const emergencies: Emergency[] = [
    {
      id: 'cardiac',
      name: 'Cardiac Arrest',
      icon: <Heart size={48} className="text-red-400" />,
      description: 'Chest pain, heart attack symptoms',
      color: 'from-red-600 to-red-700',
      protocols: ['CPR Ready', 'Defibrillator', 'Cardiologist Alert'],
    },
    {
      id: 'pediatric',
      name: 'Pediatric Emergency',
      icon: <Lightbulb size={48} className="text-yellow-400" />,
      description: 'Child medical emergency',
      color: 'from-yellow-600 to-yellow-700',
      protocols: ['Pediatric Kit', 'Specialist Alert', 'Parent Contact'],
    },
    {
      id: 'trauma',
      name: 'Trauma/Accident',
      icon: <AlertTriangle size={48} className="text-orange-400" />,
      description: 'Injury from accident or fall',
      color: 'from-orange-600 to-orange-700',
      protocols: ['Trauma Team', 'Imaging Ready', 'Surgery Prep'],
    },
    {
      id: 'stroke',
      name: 'Stroke',
      icon: <Zap size={48} className="text-purple-400" />,
      description: 'Speech difficulty, numbness',
      color: 'from-purple-600 to-purple-700',
      protocols: ['Neuro Team', 'CT Scan', 'Treatment Window'],
    },
    {
      id: 'burn',
      name: 'Burn Injury',
      icon: <Activity size={48} className="text-pink-400" />,
      description: 'Thermal, chemical, or electrical burn',
      color: 'from-pink-600 to-pink-700',
      protocols: ['Burn Specialist', 'ICU Ready', 'Cooling Protocol'],
    },
    {
      id: 'general',
      name: 'General Emergency',
      icon: <Pill size={48} className="text-blue-400" />,
      description: 'Other medical emergency',
      color: 'from-blue-600 to-blue-700',
      protocols: ['General Assessment', 'Flexible Response'],
    },
  ];

  async function fetchAssignedTrip() {
    setTripError('');
    setLoadingTrip(true);
    try {
      const rawUser = localStorage.getItem('currentUser');
      const currentUser = rawUser ? (JSON.parse(rawUser) as { email?: string }) : null;
      if (!currentUser?.email) {
        setAssignedTrip(null);
        return;
      }

      const response = await fetch(`/api/driver/assigned?email=${encodeURIComponent(currentUser.email)}`);
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error || 'Failed to load assignment.');
      }
      setAssignedTrip(data.trip);
    } catch (error) {
      setTripError(error instanceof Error ? error.message : 'Failed to load assignment.');
    } finally {
      setLoadingTrip(false);
    }
  }

  useEffect(() => {
    fetchAssignedTrip();
    const interval = setInterval(fetchAssignedTrip, 5000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (!assignedTrip?.emergencyType) return;
    const mapped = TRIP_TO_CARD_EMERGENCY[assignedTrip.emergencyType] ?? null;
    if (mapped) setSelectedEmergency(mapped);
  }, [assignedTrip?.emergencyType]);

  const handleContinue = () => {
    if (selectedEmergency) {
      router.push(`/driver/active-route?emergency=${selectedEmergency}`);
    }
  };

  const handleStageUpdate = async (stage: 'reached_pickup' | 'reached_hospital' | 'completed') => {
    if (!assignedTrip) return;
    setUpdating(true);
    setTripError('');
    try {
      const rawUser = localStorage.getItem('currentUser');
      const currentUser = rawUser ? (JSON.parse(rawUser) as { email?: string }) : null;
      if (!currentUser?.email) {
        throw new Error('Driver session not found.');
      }

      const response = await fetch('/api/driver/trip-status', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tripId: assignedTrip.id,
          stage,
          driverEmail: currentUser.email,
          driverLocation:
            stage === 'reached_hospital' || stage === 'completed'
              ? assignedTrip.dropoffLocation
              : assignedTrip.pickupLocation,
        }),
      });

      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error || 'Failed to update status.');
      }

      await fetchAssignedTrip();
    } catch (error) {
      setTripError(error instanceof Error ? error.message : 'Failed to update status.');
    } finally {
      setUpdating(false);
    }
  };

  const handleEmergencySelect = async (emergencyId: string) => {
    setSelectedEmergency(emergencyId);
    if (!assignedTrip?.id) return;

    const tripEmergencyType = CARD_TO_TRIP_EMERGENCY[emergencyId] ?? emergencyId;
    setUpdatingEmergency(true);
    setTripError('');
    try {
      const response = await fetch('/api/driver/emergency-type', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tripId: assignedTrip.id,
          emergencyType: tripEmergencyType,
        }),
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error || 'Failed to update emergency type.');
      }
      await fetchAssignedTrip();
    } catch (error) {
      setTripError(error instanceof Error ? error.message : 'Failed to update emergency type.');
    } finally {
      setUpdatingEmergency(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-orange-900 to-red-900 p-4">
      <div className="max-w-6xl mx-auto">
        <div className="text-center mb-8 pt-8">
          <h1 className="text-5xl font-black text-white mb-2">Driver Dashboard</h1>
          <p className="text-orange-200 text-xl">Assigned SOS + Emergency type controls</p>
        </div>

        <div className="mb-10 bg-black/30 border-2 border-orange-500/40 rounded-xl p-6">
          <h2 className="text-2xl font-bold text-white mb-4">Current Assigned SOS</h2>
          {loadingTrip && <p className="text-orange-200">Checking for active assignment...</p>}
          {!loadingTrip && !assignedTrip && (
            <p className="text-orange-200">No active assignment for this driver right now.</p>
          )}
          {!loadingTrip && assignedTrip && (
            <div className="space-y-2 text-white">
              <p>Trip ID: {assignedTrip.id}</p>
              <p>Status: {assignedTrip.status}</p>
              <p>
                Hospital Case: {' '}
                <span
                  className={`font-semibold ${
                    assignedTrip.hospitalCaseStatus === 'ready'
                      ? 'text-emerald-300'
                      : assignedTrip.hospitalCaseStatus === 'registered'
                        ? 'text-cyan-300'
                        : 'text-amber-300'
                  }`}
                >
                  {assignedTrip.hospitalCaseStatus ?? 'pending'}
                </span>
              </p>
              {assignedTrip.estimatedTime != null && <p>ETA to Hospital: {assignedTrip.estimatedTime} min</p>}
              <p>User Phone: {assignedTrip.user?.phone ?? 'N/A'}</p>
              <p>
                Pickup: {assignedTrip.pickupLocation?.latitude?.toFixed(5) ?? 'N/A'},{' '}
                {assignedTrip.pickupLocation?.longitude?.toFixed(5) ?? 'N/A'}
              </p>
              {assignedTrip.hospitalName && <p>Hospital: {assignedTrip.hospitalName}</p>}
              {assignedTrip.policeStationName && <p>Police Station: {assignedTrip.policeStationName}</p>}
              <div className="flex gap-3 pt-3 flex-wrap">
                {assignedTrip.mapUrl && (
                  <a
                    href={assignedTrip.mapUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded-lg font-semibold inline-flex items-center gap-2"
                  >
                    Open Pickup Map
                    <ExternalLink size={16} />
                  </a>
                )}
                {assignedTrip.hospitalMapUrl && (
                  <a
                    href={assignedTrip.hospitalMapUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="px-4 py-2 bg-violet-600 hover:bg-violet-700 rounded-lg font-semibold inline-flex items-center gap-2"
                  >
                    Open Hospital Map
                    <ExternalLink size={16} />
                  </a>
                )}
                <button
                  onClick={() => handleStageUpdate('reached_pickup')}
                  disabled={updating || assignedTrip.status !== 'assigned'}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 disabled:bg-emerald-900 disabled:text-emerald-200 rounded-lg font-semibold"
                >
                  {assignedTrip.status !== 'assigned'
                    ? 'Reached Pickup'
                    : updating
                      ? 'Updating...'
                      : 'Mark Reached Pickup'}
                </button>
                <button
                  onClick={() => handleStageUpdate('reached_hospital')}
                  disabled={updating || assignedTrip.status !== 'in-progress'}
                  className="px-4 py-2 bg-amber-600 hover:bg-amber-700 disabled:bg-amber-900 disabled:text-amber-200 rounded-lg font-semibold"
                >
                  {assignedTrip.status === 'accepted'
                    ? 'Reached Hospital'
                    : updating
                      ? 'Updating...'
                      : 'Mark Reached Hospital'}
                </button>
                <button
                  onClick={() => handleStageUpdate('completed')}
                  disabled={updating || (assignedTrip.status !== 'accepted' && assignedTrip.status !== 'in-progress')}
                  className="px-4 py-2 bg-green-700 hover:bg-green-800 disabled:bg-green-950 disabled:text-green-200 rounded-lg font-semibold"
                >
                  {assignedTrip.status === 'completed'
                    ? 'Work Completed'
                    : updating
                      ? 'Updating...'
                      : 'Complete Work'}
                </button>
              </div>
            </div>
          )}
          {tripError && <p className="text-red-300 mt-3">{tripError}</p>}
        </div>

        <div className="text-center mb-8">
          <h2 className="text-3xl font-black text-white mb-2">Emergency Type</h2>
          <p className="text-orange-200 text-lg">Selecting updates hospital case prep with emergency category</p>
          {updatingEmergency && <p className="text-amber-200 text-sm mt-1">Syncing emergency type...</p>}
        </div>

        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6 mb-12">
          {emergencies.map((emergency) => (
            <button key={emergency.id} onClick={() => handleEmergencySelect(emergency.id)} className="text-left group">
              <div
                className={`p-6 rounded-xl border-2 transition-all duration-300 ${
                  selectedEmergency === emergency.id
                    ? 'border-orange-400 bg-gradient-to-br from-orange-500/20 to-red-500/20 shadow-lg shadow-orange-500/50 scale-105'
                    : 'border-white/20 bg-white/5 hover:border-orange-400 hover:bg-orange-500/10'
                }`}
              >
                <div className="flex items-start justify-between mb-4">
                  <div className="p-3 bg-white/10 rounded-lg">{emergency.icon}</div>
                </div>
                <h3 className="text-xl font-bold text-white mb-2">{emergency.name}</h3>
                <p className="text-white/70 text-sm mb-3">{emergency.description}</p>
                {selectedEmergency === emergency.id && (
                  <div className="space-y-1 pt-3 border-t border-white/20">
                    {emergency.protocols.map((protocol) => (
                      <p key={protocol} className="text-xs text-orange-200">
                        - {protocol}
                      </p>
                    ))}
                  </div>
                )}
              </div>
            </button>
          ))}
        </div>

        <div className="flex gap-4 justify-center">
          <button
            onClick={() => router.back()}
            className="px-8 py-3 bg-white/10 hover:bg-white/20 border-2 border-white/20 text-white font-bold rounded-lg transition"
          >
            Back
          </button>
          <GlowButton
            variant="danger"
            size="lg"
            onClick={handleContinue}
            disabled={!selectedEmergency}
            className={selectedEmergency ? '' : 'opacity-50 cursor-not-allowed'}
          >
            Continue to Route Planning
          </GlowButton>
        </div>
      </div>
    </div>
  );
}
