import { create } from 'zustand';

export interface Hospital {
  id: string;
  name: string;
  address?: string;
  phone?: string;
  latitude: number;
  longitude: number;
  distance?: number;
  bedsAvailable?: number;
  specialization?: string[];
}

export interface PoliceStation {
  id: string;
  name: string;
  address?: string;
  phone?: string;
  latitude: number;
  longitude: number;
  distance?: number;
}

export interface EmergencyTrip {
  id: string;
  citizenName?: string;
  phone?: string;
  location: { latitude: number; longitude: number };
  emergencyType: 'medical' | 'accident' | 'fire' | 'crime' | 'police' | 'heart_attack';
  timestamp: number;
  driverId?: string;
  hospital?: Hospital;
  policeStation?: PoliceStation;
  status: 'pending' | 'assigned' | 'accepted' | 'in-progress' | 'completed' | 'cancelled';
  etaMinutes?: number;
  tripId?: string;
}

interface EmergencyStore {
  activeTrip: EmergencyTrip | null;
  hospitals: Hospital[];
  setActiveTrip: (trip: EmergencyTrip | null) => void;
  setHospitals: (hospitals: Hospital[]) => void;
  updateTripStatus: (tripId: string, status: EmergencyTrip['status']) => void;
}

export const useEmergencyStore = create<EmergencyStore>((set) => ({
  activeTrip: null,
  hospitals: [],
  setActiveTrip: (trip) => set({ activeTrip: trip }),
  setHospitals: (hospitals) => set({ hospitals }),
  updateTripStatus: (tripId, status) =>
    set((state) => ({
      activeTrip:
        state.activeTrip && state.activeTrip.id === tripId
          ? { ...state.activeTrip, status }
          : state.activeTrip,
    })),
}));
