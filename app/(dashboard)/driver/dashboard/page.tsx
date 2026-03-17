'use client';

import { useState } from 'react';
import Link from 'next/link';
import { motion } from 'framer-motion';
import { ChevronLeft, HeartPulse, Baby, UserMinus, Brain, Flame } from 'lucide-react';
import { useI18n } from '@/components/shared/LanguageProvider';
import { EmergencyCard, GlowButton } from '@/components/shared';
import { useEmergencyStore } from '@/store/useEmergencyStore';

interface Emergency {
  id: string;
  title: string;
  description: string;
  icon: any;
  variant: 'default' | 'highlight' | 'warning';
  protocol: string;
}

const emergencies: Emergency[] = [
  {
    id: 'cardiac',
    title: 'Cardiac Arrest',
    description: 'CPR, Defibrillator, Cardiologist needed',
    icon: <HeartPulse />,
    variant: 'warning',
    protocol: 'Prepare defibrillator, notify cardiologist',
  },
  {
    id: 'pediatric',
    title: 'Pediatric Emergency',
    description: 'Child specialist, pediatric kit',
    icon: <Baby />,
    variant: 'highlight',
    protocol: 'Contact parents, child kit ready',
  },
  {
    id: 'trauma',
    title: 'Trauma/Accident',
    description: 'Trauma team, imaging, surgery prep',
    icon: <UserMinus />,
    variant: 'warning',
    protocol: 'Stabilize patient, trauma bay',
  },
  {
    id: 'stroke',
    title: 'Stroke',
    description: 'Neuro team, CT scan, thrombolysis window',
    icon: <Brain />,
    variant: 'highlight',
    protocol: 'CT scan stat, neuro consult',
  },
  {
    id: 'burn',
    title: 'Burn Injury',
    description: 'Burn specialist, ICU, cooling protocol',
    icon: <Flame />,
    variant: 'highlight',
    protocol: 'Cooling, fluid resuscitation',
  },
];

export default function DriverDashboard() {
  const { t } = useI18n();
  const setActiveTrip = useEmergencyStore((state) => state.setActiveTrip);
  const [selectedEmergency, setSelectedEmergency] = useState<Emergency | null>(null);

  const handleSelect = (emergency: Emergency) => {
    setSelectedEmergency(emergency);
    setActiveTrip({ 
      id: emergency.id,
      emergencyType: emergency.id === 'cardiac' ? 'heart_attack' : 'medical',
      location: { latitude: 17.3850, longitude: 78.4867 }, // Default for transition
      timestamp: Date.now(),
      status: 'assigned' as const 
    });
    window.location.href = '/driver/hospital-selection';
  };

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="min-h-screen p-6 bg-gradient-to-br from-slate-950 via-slate-900"
    >
      <div className="max-w-4xl mx-auto">
        <div className="mb-12">
          <Link href="/driver-login" className="flex items-center gap-2 text-slate-400 hover:text-white mb-6 inline-block">
            <ChevronLeft size={20} />
            Back to Login
          </Link>
          <h1 className="text-4xl font-black mb-4 bg-gradient-to-r from-orange-400 to-red-500 bg-clip-text text-transparent">
            Emergency Dashboard
          </h1>
          <p className="text-xl text-slate-300">Select emergency type (6 options)</p>
        </div>
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {emergencies.map((emergency) => (
            <motion.div
              key={emergency.id}
              whileHover={{ y: -8 }}
              className="group"
            >
              <EmergencyCard
                title={emergency.title}
                description={emergency.description}
                icon={emergency.icon}
                variant={emergency.variant}
                onClick={() => handleSelect(emergency)}
              />
              <div className="mt-4">
                <p className="font-semibold text-slate-300 mb-2 text-sm">{emergency.protocol}</p>
                <GlowButton size="sm" className="w-full" onClick={() => handleSelect(emergency)}>
                  Select {emergency.title}
                </GlowButton>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </motion.div>
  );
}
