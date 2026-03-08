'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { AlertCircle, Search, Shield } from 'lucide-react';
import { useI18n } from '@/components/shared/LanguageProvider';

type PoliceStationOption = {
  _id?: string;
  id?: string;
  name: string;
  phone: string;
  jurisdiction: string;
  zone: string;
  city: string;
  address: string;
};

function getStationId(station: PoliceStationOption): string {
  const raw = station.id ?? station._id ?? '';
  if (typeof raw === 'string') {
    return raw.trim();
  }
  if (raw && typeof raw === 'object' && '$oid' in (raw as Record<string, unknown>)) {
    return String((raw as Record<string, unknown>).$oid ?? '').trim();
  }
  return String(raw).trim();
}

export default function PoliceLoginPage() {
  const { t } = useI18n();
  const [stations, setStations] = useState<PoliceStationOption[]>([]);
  const [stationId, setStationId] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [phone, setPhone] = useState('');
  const [loading, setLoading] = useState(false);
  const [loadingStations, setLoadingStations] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    async function loadStations() {
      setLoadingStations(true);
      try {
        const response = await fetch('/api/police');
        const data = await response.json();
        if (response.ok && data?.policeStations) {
          const normalizedStations: PoliceStationOption[] = data.policeStations.map((station: PoliceStationOption) => ({
            ...station,
            id: getStationId(station),
          }));
          setStations(normalizedStations);
          if (normalizedStations[0]) {
            setStationId(getStationId(normalizedStations[0]));
            setPhone(normalizedStations[0].phone ?? '');
          }
        } else {
          setError('No police stations found. Seed DB and refresh.');
        }
      } catch {
        setError('Failed to load police stations.');
      } finally {
        setLoadingStations(false);
      }
    }
    loadStations();
  }, []);

  const filteredStations = stations.filter((station) => {
    const q = searchTerm.trim().toLowerCase();
    if (!q) return true;
    return (
      station.name.toLowerCase().includes(q) ||
      station.jurisdiction.toLowerCase().includes(q) ||
      station.zone.toLowerCase().includes(q) ||
      station.city.toLowerCase().includes(q) ||
      station.address.toLowerCase().includes(q) ||
      String(station.phone).toLowerCase().includes(q)
    );
  });

  async function handleLogin(event: React.FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError('');

    try {
      const response = await fetch('/api/auth/police-login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ policeStationId: stationId, phone }),
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error || 'Police login failed.');
      }

      localStorage.setItem(
        'currentPoliceStation',
        JSON.stringify({
          ...data.station,
          type: 'police',
        })
      );
      window.location.href = '/police/dashboard';
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Police login failed.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 flex items-center justify-center p-4">
      <div className="w-full max-w-xl bg-white/10 border border-white/20 rounded-2xl p-8 backdrop-blur-md">
        <div className="text-center mb-6">
          <div className="inline-flex w-14 h-14 rounded-full bg-blue-600 items-center justify-center mb-3">
            <Shield className="text-white" />
          </div>
          <h1 className="text-3xl font-black text-white">{t('policeLogin.title')}</h1>
          <p className="text-blue-200">{t('policeLogin.subtitle')}</p>
        </div>

        {error && (
          <div className="mb-4 p-3 bg-red-500/20 border border-red-500 rounded-lg flex gap-2">
            <AlertCircle size={18} className="text-red-300 mt-0.5" />
            <p className="text-red-200 text-sm">{error}</p>
          </div>
        )}

        <form onSubmit={handleLogin} className="space-y-4">
          <div>
            <label className="block text-white text-sm font-semibold mb-2">{t('policeLogin.station')}</label>
            <div className="relative mb-2">
              <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                type="text"
                value={searchTerm}
                onChange={(event) => setSearchTerm(event.target.value)}
                placeholder={t('policeLogin.search')}
                className="w-full pl-10 pr-4 py-3 bg-slate-900 border border-slate-700 rounded-lg text-white"
              />
            </div>
            {loadingStations ? (
              <div className="p-3 text-slate-300 text-sm border border-slate-700 rounded-lg bg-slate-900">
                Loading police stations...
              </div>
            ) : (
              <div className="border border-slate-700 rounded-lg bg-slate-900 overflow-hidden">
                {filteredStations.length === 0 && (
                  <p className="px-3 py-3 text-slate-400 text-sm">No police stations match your search.</p>
                )}
                {filteredStations.length > 0 && (
                  <div className="max-h-56 overflow-y-auto">
                    {filteredStations.map((station) => {
                      const id = getStationId(station);
                      const isSelected = stationId === id;
                      return (
                        <button
                          key={id}
                          type="button"
                          onClick={() => {
                            setStationId(id);
                            setPhone(station.phone ?? '');
                          }}
                          className={`w-full text-left px-3 py-3 border-b border-slate-800 last:border-b-0 transition ${
                            isSelected ? 'bg-blue-700/40 text-blue-100' : 'text-slate-200 hover:bg-slate-800'
                          }`}
                        >
                          <p className="font-semibold">{station.name}</p>
                          <p className="text-xs text-slate-400">
                            {station.jurisdiction} | {station.zone}, {station.city}
                          </p>
                        </button>
                      );
                    })}
                  </div>
                )}
              </div>
            )}
          </div>

          <div>
            <label className="block text-white text-sm font-semibold mb-2">{t('policeLogin.phone')}</label>
            <input
              type="tel"
              value={phone}
              onChange={(event) => setPhone(event.target.value)}
              placeholder="Auto-filled from selected station"
              className="w-full px-4 py-3 bg-slate-900 border border-slate-700 rounded-lg text-white"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-900 rounded-lg font-bold text-white"
          >
            {loading ? 'Signing in...' : t('policeLogin.login')}
          </button>
        </form>

        <div className="text-center mt-5">
          <Link href="/" className="text-blue-300 hover:text-blue-200 text-sm">
            {t('common.backHome')}
          </Link>
        </div>
      </div>
    </div>
  );
}
