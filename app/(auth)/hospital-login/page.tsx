'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { Building2, AlertCircle, Search } from 'lucide-react';
import { useI18n } from '@/components/shared/LanguageProvider';

type HospitalOption = {
  _id: string;
  name: string;
  phone: string;
  address: string;
  city?: string;
  zone?: string;
};

export default function HospitalLoginPage() {
  const { t } = useI18n();
  const [hospitals, setHospitals] = useState<HospitalOption[]>([]);
  const [hospitalId, setHospitalId] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [phone, setPhone] = useState('');
  const [loading, setLoading] = useState(false);
  const [loadingHospitals, setLoadingHospitals] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    async function loadHospitals() {
      setLoadingHospitals(true);
      try {
        const response = await fetch('/api/hospitals');
        const data = await response.json();
        if (response.ok && data?.hospitals) {
          setHospitals(data.hospitals);
          if (data.hospitals[0]?._id) {
            setHospitalId(data.hospitals[0]._id);
            setPhone(data.hospitals[0].phone ?? '');
          }
        } else {
          setError('No hospitals found. Seed hospital data and refresh.');
        }
      } catch {
        setError('Failed to load hospitals.');
      } finally {
        setLoadingHospitals(false);
      }
    }
    loadHospitals();
  }, []);

  const selectedHospital = hospitals.find((item) => item._id === hospitalId);
  const filteredHospitals = hospitals.filter((item) => {
    const q = searchTerm.trim().toLowerCase();
    if (!q) return true;
    return (
      item.name.toLowerCase().includes(q) ||
      item.address.toLowerCase().includes(q) ||
      String(item.phone).toLowerCase().includes(q) ||
      String(item.city ?? '').toLowerCase().includes(q) ||
      String(item.zone ?? '').toLowerCase().includes(q)
    );
  });

  async function handleLogin(event: React.FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError('');

    try {
      const response = await fetch('/api/auth/hospital-login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ hospitalId, phone }),
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error || 'Hospital login failed.');
      }

      localStorage.setItem(
        'currentHospital',
        JSON.stringify({
          ...data.hospital,
          type: 'hospital',
        })
      );
      window.location.href = '/hospital/dashboard';
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Hospital login failed.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 flex items-center justify-center p-4">
      <div className="w-full max-w-lg bg-white/10 border border-white/20 rounded-2xl p-8 backdrop-blur-md">
        <div className="text-center mb-8">
          <div className="inline-flex w-14 h-14 rounded-full bg-emerald-600 items-center justify-center mb-3">
            <Building2 className="text-white" />
          </div>
          <h1 className="text-3xl font-black text-white">{t('hospitalLogin.title')}</h1>
          <p className="text-emerald-200">{t('hospitalLogin.subtitle')}</p>
        </div>

        {error && (
          <div className="mb-4 p-3 bg-red-500/20 border border-red-500 rounded-lg flex gap-2">
            <AlertCircle size={18} className="text-red-300 mt-0.5" />
            <p className="text-red-200 text-sm">{error}</p>
          </div>
        )}

        <form onSubmit={handleLogin} className="space-y-4">
          <div>
            <label className="block text-white text-sm font-semibold mb-2">{t('hospitalLogin.hospital')}</label>
            <div className="relative mb-2">
              <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                type="text"
                value={searchTerm}
                onChange={(event) => setSearchTerm(event.target.value)}
                placeholder={t('hospitalLogin.search')}
                className="w-full pl-10 pr-4 py-3 bg-slate-900 border border-slate-700 rounded-lg text-white"
              />
            </div>
            <div className="max-h-48 overflow-y-auto border border-slate-700 rounded-lg bg-slate-900">
              {loadingHospitals && <p className="p-3 text-slate-300 text-sm">Loading hospitals...</p>}
              {!loadingHospitals && filteredHospitals.length === 0 && (
                <p className="p-3 text-slate-400 text-sm">No hospitals match your search.</p>
              )}
              {!loadingHospitals &&
                filteredHospitals.map((hospital) => (
                  <button
                    key={hospital._id}
                    type="button"
                    onClick={() => {
                      setHospitalId(hospital._id);
                      setPhone(hospital.phone ?? '');
                    }}
                    className={`w-full text-left px-3 py-2 border-b border-slate-800 last:border-b-0 hover:bg-slate-800 ${
                      hospitalId === hospital._id ? 'bg-emerald-900/40' : ''
                    }`}
                  >
                    <p className="text-white text-sm font-semibold">{hospital.name}</p>
                    <p className="text-slate-400 text-xs">{hospital.address}</p>
                  </button>
                ))}
            </div>
          </div>

          {selectedHospital && (
            <div className="text-xs text-slate-300 bg-slate-900/70 border border-slate-700 rounded-lg p-3">
              <p>{selectedHospital.address}</p>
              <p>Hospital Phone (for verification): {selectedHospital.phone}</p>
            </div>
          )}

          <div>
            <label className="block text-white text-sm font-semibold mb-2">{t('hospitalLogin.phone')}</label>
            <input
              type="tel"
              value={phone}
              onChange={(event) => setPhone(event.target.value)}
              placeholder="Auto-filled from selected hospital"
              className="w-full px-4 py-3 bg-slate-900 border border-slate-700 rounded-lg text-white"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 bg-emerald-600 hover:bg-emerald-700 disabled:bg-emerald-900 rounded-lg font-bold text-white"
          >
            {loading ? 'Signing in...' : t('hospitalLogin.login')}
          </button>
        </form>

        <div className="text-center mt-6">
          <Link href="/" className="text-emerald-300 hover:text-emerald-200 text-sm">
            {t('common.backHome')}
          </Link>
        </div>
      </div>
    </div>
  );
}
