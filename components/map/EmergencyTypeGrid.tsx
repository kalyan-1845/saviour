'use client';

import React from 'react';
import {
  Heart,
  Brain,
  Flame,
  AlertCircle,
  TrendingUp,
  Ambulance,
} from 'lucide-react';

interface EmergencyType {
  id: string;
  name: string;
  icon: React.ReactNode;
  description: string;
  color: string;
  bgColor: string;
}

interface EmergencyTypeGridProps {
  onSelect: (type: EmergencyType) => void;
  selectedType?: string;
}

export function EmergencyTypeGrid({ onSelect, selectedType }: EmergencyTypeGridProps) {
  const emergencyTypes: EmergencyType[] = [
    {
      id: 'cardiac',
      name: 'Cardiac',
      icon: <Heart size={40} />,
      description: 'Heart attack, Chest pain',
      color: 'text-red-500',
      bgColor: 'bg-red-500/20',
    },
    {
      id: 'stroke',
      name: 'Stroke',
      icon: <Brain size={40} />,
      description: 'Brain-related emergency',
      color: 'text-purple-500',
      bgColor: 'bg-purple-500/20',
    },
    {
      id: 'burn',
      name: 'Burn',
      icon: <Flame size={40} />,
      description: 'Severe burns/injuries',
      color: 'text-orange-500',
      bgColor: 'bg-orange-500/20',
    },
    {
      id: 'trauma',
      name: 'Trauma',
      icon: <AlertCircle size={40} />,
      description: 'Accident/Injury',
      color: 'text-yellow-500',
      bgColor: 'bg-yellow-500/20',
    },
    {
      id: 'pediatric',
      name: 'Pediatric',
      icon: <TrendingUp size={40} />,
      description: 'Child emergency',
      color: 'text-blue-500',
      bgColor: 'bg-blue-500/20',
    },
    {
      id: 'ambulance',
      name: 'General',
      icon: <Ambulance size={40} />,
      description: 'Other emergency',
      color: 'text-green-500',
      bgColor: 'bg-green-500/20',
    },
  ];

  return (
    <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
      {emergencyTypes.map((type) => (
        <button
          key={type.id}
          onClick={() => onSelect(type)}
          className={`card-glow p-6 text-center transition-all duration-300 ${
            selectedType === type.id
              ? 'ring-2 ring-red-500 border-red-500/50 bg-red-500/10'
              : 'hover:scale-105'
          }`}
        >
          <div className={`${type.color} ${type.bgColor} w-20 h-20 rounded-full flex items-center justify-center mx-auto mb-4`}>
            {type.icon}
          </div>
          <h3 className="font-bold text-lg mb-2">{type.name}</h3>
          <p className="text-sm text-slate-400">{type.description}</p>
        </button>
      ))}
    </div>
  );
}
