'use client';

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { ChevronLeft, MapPin, Filter, Search } from 'lucide-react';
import Link from 'next/link';
import { HospitalCard, GlowButton } from '@/components/shared';
import { useEmergencyStore } from '@/store/useEmergencyStore';

export default function HospitalSelection() {
  const activeTrip = useEmergencyStore((state) => state.activeTrip);
  const hospitals = useEmergencyStore((state) => state.hospitals);
  const setStoreHospitals = useEmergencyStore((state) => state.setHospitals);
  
  const [filter, setFilter] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchHospitals = async () => {
      try {
        const res = await fetch('/api/hospital');
        const data = await res.json();
        setStoreHospitals(data.hospitals || []);
      } catch (err) {
        console.error('Failed to fetch hospitals:', err);
      } finally {
        setIsLoading(false);
      }
    };
    fetchHospitals();
  }, [setStoreHospitals]);

  const handleSelect = (hospital: any) => {
    setStoreHospitals([hospital]);
    window.location.href = '/driver/active-route';
  };

  const filteredHospitals = hospitals.filter(h => 
    h.name.toLowerCase().includes(filter.toLowerCase()) ||
    (h.specialization && h.specialization.some(s => s.toLowerCase().includes(filter.toLowerCase())))
  );

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="min-h-screen p-6 bg-slate-950"
    >
      <div className="max-w-5xl mx-auto">
        <div className="mb-10">
          <Link href="/driver/dashboard" className="inline-flex items-center gap-2 text-slate-500 hover:text-white transition-colors mb-6 group">
            <ChevronLeft size={20} className="group-hover:-translate-x-1 transition-transform" />
            Back to Dashboard
          </Link>
          
          <div className="flex flex-col md:flex-row gap-6 justify-between items-start md:items-center">
            <div>
              <h1 className="text-4xl font-black text-white tracking-tight mb-2">Hospital Selection</h1>
              <div className="flex items-center gap-3">
                <span className="px-2 py-0.5 rounded bg-red-500/10 border border-red-500/20 text-red-500 text-[10px] font-bold uppercase tracking-wider">
                  Active SOS
                </span>
                <p className="text-slate-400 text-sm">
                  Priority: <span className="text-white font-bold">{activeTrip?.emergencyType || 'General'}</span>
                </p>
              </div>
            </div>

            <div className="relative w-full md:w-80">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" size={18} />
              <input
                value={filter}
                onChange={(e) => setFilter(e.target.value)}
                placeholder="Search hospitals or specialties..."
                className="w-full pl-12 pr-4 py-4 bg-slate-900 border border-white/10 rounded-2xl text-white placeholder:text-slate-600 focus:border-blue-500/50 outline-none transition-all shadow-xl"
              />
            </div>
          </div>
        </div>

        {isLoading ? (
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6 animate-pulse">
            {[1, 2, 3, 4, 5, 6].map(i => (
              <div key={i} className="h-64 bg-slate-900 rounded-3xl border border-white/5"></div>
            ))}
          </div>
        ) : (
          <>
            <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
              {filteredHospitals.map((hospital) => (
                <div key={hospital.id || hospital._id} className="flex flex-col">
                  <HospitalCard 
                    hospital={{
                      ...hospital,
                      distance: hospital.distance ? `${hospital.distance} km` : 'Calculating...',
                      eta: hospital.eta || 'Pending'
                    }} 
                    onSelect={() => handleSelect(hospital)}
                  />
                  <GlowButton 
                    className="mt-4 py-4 rounded-2xl font-black text-sm uppercase tracking-widest shadow-lg shadow-blue-500/10" 
                    onClick={() => handleSelect(hospital)}
                  >
                    Set Destination
                  </GlowButton>
                </div>
              ))}
            </div>

            {filteredHospitals.length === 0 && (
              <div className="text-center py-20 bg-slate-900/50 rounded-[40px] border border-dashed border-white/10">
                <MapPin size={48} className="mx-auto text-slate-600 mb-4" />
                <h3 className="text-white font-bold text-xl mb-1">No hospitals found</h3>
                <p className="text-slate-500">Try adjusting your search or priority</p>
              </div>
            )}
          </>
        )}
      </div>
    </motion.div>
  );
}
