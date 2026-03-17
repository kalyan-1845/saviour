'use client';

import { useState } from 'react';
import { motion } from 'framer-motion';
import { ChevronLeft, MapPin, Filter } from 'lucide-react';
import Link from 'next/link';
import { useI18n } from '@/components/shared/LanguageProvider';
import { HospitalCard, GlowButton } from '@/components/shared';
import { useEmergencyStore } from '@/store/useEmergencyStore';

const mockHospitals = [
  {
    id: '1',
    name: 'Apollo Hospitals',
    distance: '2.3 km',
    eta: '4 min',
    beds: 12,
    specialties: ['Cardiac', 'Trauma', 'Neurology'],
    rating: 4.8,
    lat: 28.5597,
    lng: 77.2350,
  },
  {
    id: '2',
    name: 'Max Super Speciality',
    distance: '3.1 km',
    eta: '6 min',
    beds: 8,
    specialties: ['Cardiac', 'ICU'],
    rating: 4.6,
    lat: 28.5812,
    lng: 77.2940,
  },
  {
    id: '3',
    name: 'Fortis Hospital',
    distance: '1.8 km',
    eta: '3 min',
    beds: 15,
    specialties: ['Emergency', 'Burns'],
    rating: 4.7,
    lat: 28.5355,
    lng: 77.1650,
  },
];

interface Hospital {
  id: string;
  name: string;
  distance: string;
  eta: string;
  beds: number;
  specialties: string[];
  rating: number;
  lat: number;
  lng: number;
}

export default function HospitalSelection() {
  const { t } = useI18n();
  const activeTrip = useEmergencyStore((state) => state.activeTrip);
  const updateStoreHospitals = useEmergencyStore((state) => state.setHospitals);
  const [localHospitals, setLocalHospitals] = useState<Hospital[]>(mockHospitals);
  const [filter, setFilter] = useState('');

  const handleSelect = (hospital: Hospital) => {
    updateStoreHospitals([hospital]);
    window.location.href = '/driver/active-route';
  };

  const filteredHospitals = localHospitals.filter(h => 
    h.specialties.some(s => s.toLowerCase().includes(filter.toLowerCase())) ||
    h.name.toLowerCase().includes(filter.toLowerCase())
  );

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="min-h-screen p-6 bg-gradient-to-br from-slate-950 via-slate-900"
    >
      <div className="max-w-4xl mx-auto">
        <div className="mb-8">
          <Link href="/driver/dashboard" className="flex items-center gap-2 text-slate-400 hover:text-white mb-6 inline-block">
            <ChevronLeft size={20} />
            Select Emergency Type
          </Link>
          <div className="flex flex-col md:flex-row gap-4 justify-between items-start md:items-center mb-8">
            <div>
              <h1 className="text-3xl font-black gradient-text">Hospital Selection</h1>
              <p className="text-slate-300">Emergency: <span className="font-semibold text-orange-400">{activeTrip?.type || 'Cardiac'}</span></p>
            </div>
            <div className="flex gap-2">
              <div className="relative">
                <Filter className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 w-4 h-4" />
                <input
                  value={filter}
                  onChange={(e) => setFilter(e.target.value)}
                  placeholder="Filter by specialty"
                  className="pl-10 pr-4 py-2 bg-white/10 border border-white/20 rounded-lg focus:border-orange-400"
                />
              </div>
              <GlowButton size="sm" variant="secondary">
                Sort by Distance
              </GlowButton>
            </div>
          </div>
        </div>
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredHospitals.map((hospital) => (
            <HospitalCard key={hospital.id} hospital={hospital} onSelect={() => {}}>
              <div className="mt-4">
                <GlowButton className="w-full" onClick={() => handleSelect(hospital)}>
                  Navigate ({hospital.eta})
                </GlowButton>
              </div>
            </HospitalCard>
          ))}
        </div>
        {filteredHospitals.length === 0 && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="text-center py-12">
            <MapPin size={48} className="mx-auto text-slate-500 mb-4" />
            <p className="text-slate-400">No hospitals match your filter</p>
          </motion.div>
        )}
      </div>
    </motion.div>
  );
}

