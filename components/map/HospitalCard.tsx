'use client';

import React from 'react';
import { MapPin, Users, AlertCircle } from 'lucide-react';

interface Hospital {
  id: string;
  name: string;
  distance: number;
  bedsAvailable: number;
  specialization: string[];
  rating: number;
  eta: number;
}

interface HospitalCardProps {
  hospital: Hospital;
  isSelected?: boolean;
  onSelect: (hospital: Hospital) => void;
}

export function HospitalCard({ hospital, isSelected, onSelect }: HospitalCardProps) {
  return (
    <button
      onClick={() => onSelect(hospital)}
      className={`hospital-card w-full text-left ${isSelected ? 'selected' : ''}`}
    >
      {/* Header */}
      <div className="flex justify-between items-start mb-3">
        <div>
          <h3 className="font-bold text-lg">{hospital.name}</h3>
          <div className="flex items-center gap-1 text-yellow-500 text-sm">
            {'★'.repeat(Math.floor(hospital.rating))}
            <span className="text-slate-400">({hospital.rating})</span>
          </div>
        </div>
        <span className="text-sm font-bold text-red-500 bg-red-500/20 px-2 py-1 rounded">
          {hospital.eta}min
        </span>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 gap-3 mb-3 text-sm">
        <div className="flex items-center gap-2">
          <MapPin size={16} className="text-slate-500" />
          <span>{hospital.distance.toFixed(1)} km away</span>
        </div>
        <div className="flex items-center gap-2">
          <Users size={16} className="text-slate-500" />
          <span>{hospital.bedsAvailable} beds available</span>
        </div>
      </div>

      {/* Specializations */}
      <div className="flex flex-wrap gap-2">
        {hospital.specialization.slice(0, 2).map((spec) => (
          <span
            key={spec}
            className="text-xs bg-red-500/20 text-red-400 px-2 py-1 rounded border border-red-500/30"
          >
            {spec}
          </span>
        ))}
        {hospital.specialization.length > 2 && (
          <span className="text-xs bg-slate-700/50 text-slate-300 px-2 py-1 rounded">
            +{hospital.specialization.length - 2}
          </span>
        )}
      </div>

      {/* Alert */}
      {hospital.bedsAvailable < 3 && (
        <div className="mt-3 flex items-center gap-2 text-yellow-500 text-xs bg-yellow-500/10 p-2 rounded border border-yellow-500/30">
          <AlertCircle size={14} />
          Limited beds available
        </div>
      )}
    </button>
  );
}

interface HospitalGridProps {
  hospitals: Hospital[];
  selectedHospitalId?: string;
  onSelect: (hospital: Hospital) => void;
}

export function HospitalGrid({ hospitals, selectedHospitalId, onSelect }: HospitalGridProps) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      {hospitals.map((hospital) => (
        <HospitalCard
          key={hospital.id}
          hospital={hospital}
          isSelected={selectedHospitalId === hospital.id}
          onSelect={onSelect}
        />
      ))}
    </div>
  );
}
