'use client';

import { motion } from 'framer-motion';
import Link from 'next/link';
import {
  LayoutGrid,
  MapPin,
  Hospital,
  LogOut,
  Settings,
  Bell,
  User,
} from 'lucide-react';
import { useState } from 'react';

interface SidebarProps {
  userType?: 'driver' | 'admin' | 'hospital';
  userName?: string;
}

export function Sidebar({ userType = 'driver', userName = 'User' }: SidebarProps) {
  const [isCollapsed, setIsCollapsed] = useState(false);

  const driverMenuItems = [
    { icon: LayoutGrid, label: 'Dashboard', href: '/(dashboard)/driver/dashboard' },
    { icon: MapPin, label: 'Active Route', href: '/(dashboard)/driver/dashboard' },
    { icon: Hospital, label: 'History', href: '/(dashboard)/driver/history' },
  ];

  const adminMenuItems = [
    { icon: LayoutGrid, label: 'Overview', href: '/admin/dashboard' },
    { icon: Hospital, label: 'Hospitals', href: '/admin/hospitals' },
    { icon: MapPin, label: 'Traffic Control', href: '/admin/traffic' },
    { icon: Settings, label: 'Settings', href: '/admin/settings' },
  ];

  const menuItems = userType === 'driver' ? driverMenuItems : adminMenuItems;

  return (
    <motion.aside
      animate={{ width: isCollapsed ? 80 : 280 }}
      transition={{ duration: 0.3 }}
      className="hidden md:flex flex-col h-screen bg-gradient-to-b from-slate-900 to-slate-950 border-r border-slate-800 backdrop-blur-md sticky top-0"
    >
      {/* Top Section */}
      <div className="p-6 border-b border-slate-800 flex items-center justify-between">
        {!isCollapsed && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex flex-col">
            <h2 className="font-bold text-sm text-slate-300">SARATHI</h2>
            <p className="text-xs text-slate-500 capitalize">{userType} Portal</p>
          </motion.div>
        )}
        <button
          onClick={() => setIsCollapsed(!isCollapsed)}
          className="p-1 hover:bg-slate-700/50 rounded-lg transition"
        >
          {isCollapsed ? '→' : '←'}
        </button>
      </div>

      {/* Menu Items */}
      <nav className="flex-1 p-4 space-y-2 overflow-y-auto">
        {menuItems.map((item, idx) => (
          <motion.div key={idx} whileHover={{ x: 5 }} transition={{ duration: 0.2 }}>
            <Link href={item.href}>
              <div className="flex items-center gap-3 px-4 py-3 rounded-lg hover:bg-slate-700/50 text-slate-300 hover:text-white transition group cursor-pointer relative overflow-hidden">
                <div className="absolute inset-0 bg-gradient-to-r from-red-500/0 to-transparent opacity-0 group-hover:opacity-10 transition-opacity" />
                <item.icon size={20} className="group-hover:text-red-400 transition relative z-10" />
                {!isCollapsed && <span className="text-sm font-medium relative z-10">{item.label}</span>}
              </div>
            </Link>
          </motion.div>
        ))}
      </nav>

      {/* Bottom Section */}
      <div className="border-t border-slate-800 p-4 space-y-2">
        {!isCollapsed && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="px-4 py-3 rounded-lg bg-slate-800/50 border border-slate-700/50">
            <p className="text-xs text-slate-400 mb-1">Logged in as</p>
            <p className="text-sm font-semibold text-white truncate flex items-center gap-2">
              <User size={16} />
              {userName}
            </p>
          </motion.div>
        )}

        <motion.div whileHover={{ x: 5 }}>
          <button className="w-full flex items-center gap-3 px-4 py-2 rounded-lg hover:bg-red-600/20 text-red-400 hover:text-red-300 transition text-sm font-medium">
            <LogOut size={18} />
            {!isCollapsed && <span>Logout</span>}
          </button>
        </motion.div>
      </div>
    </motion.aside>
  );
}
