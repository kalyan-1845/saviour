'use client';

import { Heart, Navigation2, Bed } from 'lucide-react';

interface HospitalCardProps {
  name: string;
  distance: number;
  beds: number;
  specialties: string[];
  onSelect?: () => void;
  isSelected?: boolean;
}

export function HospitalCard({
  name,
  distance,
  beds,
  specialties,
  onSelect,
  isSelected = false,
}: HospitalCardProps) {
  return (
    <div
      onClick={onSelect}
      className={`
        p-4 rounded-lg border-2 transition-all cursor-pointer
        ${
          isSelected
            ? 'border-orange-500 bg-orange-500/20 shadow-lg shadow-orange-500/50'
            : 'border-white/20 bg-white/10 hover:border-orange-400 hover:bg-orange-500/10'
        }
      `}
    >
      <div className="flex justify-between items-start mb-3">
        <div>
          <h3 className="text-lg font-bold text-white">{name}</h3>
          <p className="text-orange-300 text-sm font-semibold">{distance.toFixed(1)} km away</p>
        </div>
        <Heart
          size={20}
          className={isSelected ? 'fill-orange-500 text-orange-500' : 'text-white'}
        />
      </div>

      <div className="grid grid-cols-2 gap-3 mb-3">
        <div className="bg-white/10 p-3 rounded-lg">
          <div className="flex items-center gap-2">
            <Bed size={18} className="text-orange-400" />
            <span className="text-white font-semibold">{beds} Beds</span>
          </div>
        </div>
        <div className="bg-white/10 p-3 rounded-lg flex items-center justify-center">
          <span className="text-green-400 text-sm font-bold">Available</span>
        </div>
      </div>

      <div className="flex flex-wrap gap-2">
        {specialties.map((spec) => (
          <span
            key={spec}
            className="px-2 py-1 bg-blue-600/40 text-blue-200 text-xs rounded-full font-semibold"
          >
            {spec}
          </span>
        ))}
      </div>

      {!isSelected && (
        <button className="mt-3 w-full bg-orange-600 hover:bg-orange-700 text-white font-bold py-2 rounded-lg transition">
          <Navigation2 size={16} className="inline mr-2" />
          Navigate
        </button>
      )}
    </div>
  );
}
