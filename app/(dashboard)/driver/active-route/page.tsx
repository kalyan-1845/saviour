'use client';

import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { ChevronLeft, Navigation2, Mic, Volume2, MapPin, Clock, Gauge } from "lucide-react";
import Link from "next/link";
import { MapComponent } from "@/components/map/MapComponent";
import { VoiceAssistant } from "@/components/shared/VoiceAssistant";
import { GlowButton } from "@/components/shared/GlowButton";
import { useEmergencyStore } from "@/store/useEmergencyStore";
import { useLocation } from "@/hooks/useLocation";

interface Hospital {
  id: string;
  name: string;
  lat: number;
  lng: number;
  specialties: string[];
}

export default function ActiveRoute() {
  const activeTrip = useEmergencyStore((state) => state.activeTrip);
  const hospitals = useEmergencyStore((state) => state.hospitals);
  const selectedHospital: Hospital = hospitals[0] || { 
    id: "default", 
    name: "Selected Hospital", 
    lat: 28.6139, 
    lng: 77.209, 
    specialties: [] 
  };
  const { location: userLocationHook } = useLocation();
  const [eta, setEta] = useState("4 min");
  const [distance, setDistance] = useState("2.3 km");
  const [speed, setSpeed] = useState(45);

  useEffect(() => {
    const interval = setInterval(() => {
      setEta("3 min 45s");
      setDistance("1.8 km");
      setSpeed(52);
    }, 5000);
    return () => clearInterval(interval);
  }, []);

  const center = { lat: selectedHospital.lat, lng: selectedHospital.lng };

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 relative"
    >
      <div className="absolute top-4 left-4 z-20">
        <Link href="/driver/hospital-selection" className="bg-white/10 backdrop-blur p-3 rounded-full">
          <ChevronLeft size={24} className="text-white" />
        </Link>
      </div>
      <div className="h-screen flex flex-col">
        {/* Map */}
        <div className="flex-1 relative z-10">
          <MapComponent
            center={center}
            zoom={15}
            markers={[{ position: center, title: selectedHospital.name }]}
          />
        </div>
        
        {/* Bottom Sheet */}
        <motion.div
          initial={{ y: 100 }}
          animate={{ y: 0 }}
          className="bg-white/10 backdrop-blur border-t border-white/20 p-6"
        >
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-3 h-3 bg-green-500 rounded-full animate-pulse" />
              <span className="font-bold text-lg bg-gradient-to-r from-orange-400 to-red-500 bg-clip-text text-transparent">
                {selectedHospital.name}
              </span>
            </div>
            <div className="grid grid-cols-2 gap-4 text-center p-4 bg-white/5 rounded-xl">
              <div>
                <MapPin size={20} className="mx-auto mb-1 text-slate-400" />
                <p className="text-2xl font-black">{distance}</p>
                <p className="text-slate-400 text-sm">Distance</p>
              </div>
              <div>
                <Clock size={20} className="mx-auto mb-1 text-slate-400" />
                <p className="text-2xl font-black text-emerald-400">{eta}</p>
                <p className="text-slate-400 text-sm">ETA</p>
              </div>
            </div>
            <div className="flex items-center gap-2 p-3 bg-slate-900/50 rounded-lg">
              <Gauge size={20} />
              <span className="font-bold">
                Current Speed: <span className="text-orange-400">{speed} km/h</span>
              </span>
            </div>
            <div className="flex gap-3 pt-2">
              <GlowButton className="flex-1" variant="secondary">
                <Navigation2 size={20} />
                Recalculate Route
              </GlowButton>
              <VoiceAssistant className="p-3 bg-white/10 rounded-xl flex-shrink-0" />
            </div>
          </div>
        </motion.div>
      </div>
    </motion.div>
  );
}

