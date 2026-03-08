'use client';

import { useState } from 'react';
import { MapPin, Bed, Star, Navigation2, Filter } from 'lucide-react';
import { HospitalCard, GlowButton } from '@/components/shared';

interface Hospital {
  id: string;
  name: string;
  distance: number;
  beds: number;
  specialties: string[];
  rating: number;
  address: string;
}

export default function HospitalSelectionPage() {
  const [selectedHospital, setSelectedHospital] = useState<string | null>(null);
  const [filterSpecialty, setFilterSpecialty] = useState('');

  const hospitals: Hospital[] = [
    {
      id: '1',
      name: 'Apollo Hospital Delhi',
      distance: 2.5,
      beds: 8,
      specialties: ['Cardiac', 'Trauma', 'Neurology'],
      rating: 4.8,
      address: 'Sarita Vihar, Delhi',
    },
    {
      id: '2',
      name: 'Max Super Specialty',
      distance: 3.2,
      beds: 5,
      specialties: ['Pediatric', 'Burn', 'Trauma'],
      rating: 4.7,
      address: 'Patparganj, Delhi',
    },
    {
      id: '3',
      name: 'Delhi Heart & Lung Institute',
      distance: 4.1,
      beds: 12,
      specialties: ['Cardiac', 'Stroke'],
      rating: 4.9,
      address: 'Ansari Nagar, Delhi',
    },
    {
      id: '4',
      name: 'Fortis Hospital',
      distance: 1.8,
      beds: 3,
      specialties: ['Trauma', 'General'],
      rating: 4.6,
      address: 'Sector 62, Noida',
    },
  ];

  const allSpecialties = Array.from(
    new Set(hospitals.flatMap((h) => h.specialties))
  );

  const filteredHospitals = filterSpecialty
    ? hospitals.filter((h) => h.specialties.includes(filterSpecialty))
    : hospitals.sort((a, b) => a.distance - b.distance);

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-blue-900 to-purple-900 p-4">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <div className="text-center mb-8 pt-8">
          <h1 className="text-5xl font-black text-white mb-2">
            Select Hospital
          </h1>
          <p className="text-blue-200 text-xl">
            Nearest hospitals with best match for your condition
          </p>
        </div>

        {/* Filter Section */}
        <div className="mb-8 flex flex-wrap gap-3 items-center justify-center">
          <div className="flex items-center gap-2 text-white">
            <Filter size={20} />
            <span className="font-semibold">Filter by:</span>
          </div>
          <button
            onClick={() => setFilterSpecialty('')}
            className={`px-4 py-2 rounded-lg font-semibold transition ${
              !filterSpecialty
                ? 'bg-blue-600 text-white'
                : 'bg-white/10 text-white/70 hover:bg-white/20'
            }`}
          >
            All
          </button>
          {allSpecialties.map((specialty) => (
            <button
              key={specialty}
              onClick={() => setFilterSpecialty(specialty)}
              className={`px-4 py-2 rounded-lg font-semibold transition ${
                filterSpecialty === specialty
                  ? 'bg-blue-600 text-white'
                  : 'bg-white/10 text-white/70 hover:bg-white/20'
              }`}
            >
              {specialty}
            </button>
          ))}
        </div>

        {/* Hospitals Grid */}
        <div className="grid md:grid-cols-2 gap-6 mb-12">
          {filteredHospitals.map((hospital) => (
            <button
              key={hospital.id}
              onClick={() => setSelectedHospital(hospital.id)}
              className="text-left"
            >
              <HospitalCard
                name={hospital.name}
                distance={hospital.distance}
                beds={hospital.beds}
                specialties={hospital.specialties}
                isSelected={selectedHospital === hospital.id}
                onSelect={() => setSelectedHospital(hospital.id)}
              />
            </button>
          ))}
        </div>

        {/* Selected Hospital Details */}
        {selectedHospital && (
          <div className="bg-gradient-to-r from-blue-600/20 to-purple-600/20 border-2 border-blue-500 rounded-xl p-8 mb-8">
            {(() => {
              const hospital = hospitals.find((h) => h.id === selectedHospital);
              return hospital ? (
                <div>
                  <div className="flex justify-between items-start mb-6">
                    <div>
                      <h2 className="text-3xl font-black text-white mb-2">
                        {hospital.name}
                      </h2>
                      <p className="text-blue-300 flex items-center gap-2">
                        <MapPin size={16} />
                        {hospital.address}
                      </p>
                    </div>
                    <div className="text-right">
                      <div className="flex items-center gap-1 justify-end mb-2">
                        {[...Array(5)].map((_, i) => (
                          <Star
                            key={i}
                            size={18}
                            className={
                              i < Math.floor(hospital.rating)
                                ? 'fill-yellow-400 text-yellow-400'
                                : 'text-white/20'
                            }
                          />
                        ))}
                      </div>
                      <p className="text-yellow-400 font-bold">
                        {hospital.rating} rating
                      </p>
                    </div>
                  </div>

                  <div className="grid md:grid-cols-3 gap-4 mb-6">
                    <div className="bg-white/10 p-4 rounded-lg">
                      <p className="text-white/70 text-sm mb-1">Distance</p>
                      <p className="text-white text-2xl font-bold">
                        {hospital.distance.toFixed(1)} km
                      </p>
                    </div>
                    <div className="bg-white/10 p-4 rounded-lg">
                      <p className="text-white/70 text-sm mb-1">
                        Available Beds
                      </p>
                      <p className="text-white text-2xl font-bold">
                        {hospital.beds}
                      </p>
                    </div>
                    <div className="bg-white/10 p-4 rounded-lg">
                      <p className="text-white/70 text-sm mb-1">ETA</p>
                      <p className="text-white text-2xl font-bold">
                        {Math.ceil(hospital.distance * 3)} mins
                      </p>
                    </div>
                  </div>
                </div>
              ) : null;
            })()}
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex gap-4 justify-center mb-8">
          <button
            onClick={() => setSelectedHospital(null)}
            className="px-8 py-3 bg-white/10 hover:bg-white/20 border-2 border-white/20 text-white font-bold rounded-lg transition"
          >
            Clear Selection
          </button>
          {selectedHospital && (
            <GlowButton
              variant="primary"
              size="lg"
              className="flex items-center gap-2"
            >
              <Navigation2 size={20} />
              Start Navigation
            </GlowButton>
          )}
        </div>
      </div>
    </div>
  );
}
