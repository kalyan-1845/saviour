'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Navigation, AlertCircle } from 'lucide-react';

declare global {
  interface Window {
    google?: {
      maps: {
        Map: new (el: HTMLElement, options: Record<string, unknown>) => unknown;
        Marker: new (options: Record<string, unknown>) => unknown;
        TrafficLayer: new () => { setMap: (map: unknown) => void };
        SymbolPath: { CIRCLE: unknown };
      };
    };
  }
}

interface MarkerInput {
  position: { lat: number; lng: number };
  title: string;
}

interface MapComponentProps {
  center?: { lat: number; lng: number };
  zoom?: number;
  markers?: MarkerInput[];
  onLocationChange?: (location: { lat: number; lng: number }) => void;
}

function loadGoogleMapsScript(apiKey: string): Promise<void> {
  return new Promise((resolve, reject) => {
    if (window.google?.maps) {
      resolve();
      return;
    }

    const existing = document.querySelector(
      'script[data-sarathi-google-maps="true"]'
    ) as HTMLScriptElement | null;

    if (existing) {
      if (window.google?.maps || existing.dataset.loaded === 'true') {
        resolve();
        return;
      }
      existing.addEventListener('load', () => resolve(), { once: true });
      existing.addEventListener('error', () => reject(new Error('Failed to load Google Maps script.')), {
        once: true,
      });

      // Fallback: if load event was already fired before listener attachment, poll briefly.
      let attempts = 0;
      const timer = window.setInterval(() => {
        attempts += 1;
        if (window.google?.maps) {
          window.clearInterval(timer);
          resolve();
        } else if (attempts > 30) {
          window.clearInterval(timer);
          reject(new Error('Google Maps script timed out.'));
        }
      }, 250);
      return;
    }

    const script = document.createElement('script');
    script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}&libraries=places,routes`;
    script.async = true;
    script.defer = true;
    script.dataset.sarathiGoogleMaps = 'true';
    script.onload = () => {
      script.dataset.loaded = 'true';
      resolve();
    };
    script.onerror = () => reject(new Error('Failed to load Google Maps script.'));
    document.head.appendChild(script);
  });
}

export function MapComponent({
  center = { lat: 28.6139, lng: 77.209 },
  zoom = 14,
  markers = [],
  onLocationChange,
}: MapComponentProps) {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<unknown>(null);
  const markersRef = useRef<unknown[]>([]);

  const [userLocation, setUserLocation] = useState<{ lat: number; lng: number } | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const apiKey =
    process.env.NEXT_PUBLIC_GOOGLE_MAPS_KEY ?? process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY ?? '';

  const allMarkers = useMemo(() => {
    const list: MarkerInput[] = [...markers];
    if (userLocation) {
      list.unshift({ position: userLocation, title: 'Your Location' });
    }
    return list;
  }, [markers, userLocation]);

  const renderMap = useCallback(() => {
    if (!mapContainerRef.current || !window.google?.maps) return;

    const mapCenter = userLocation || center;

    if (!mapRef.current) {
      mapRef.current = new window.google.maps.Map(mapContainerRef.current, {
        center: mapCenter,
        zoom,
      });

      const trafficLayer = new window.google.maps.TrafficLayer();
      trafficLayer.setMap(mapRef.current);
    }

    // Recenter map before refreshing markers so live updates are visible.
    (mapRef.current as { setCenter?: (c: { lat: number; lng: number }) => void })?.setCenter?.(mapCenter);

    // Rebuild markers for fresh render.
    markersRef.current = [];
    allMarkers.forEach((marker) => {
      const isUser = marker.title === 'Your Location';
      const googleMarker = new window.google!.maps.Marker({
        position: marker.position,
        map: mapRef.current,
        title: marker.title,
        ...(isUser
          ? {
              icon: {
                path: window.google!.maps.SymbolPath.CIRCLE,
                scale: 9,
                fillColor: '#2563eb',
                fillOpacity: 1,
                strokeColor: '#ffffff',
                strokeWeight: 2,
              },
            }
          : {}),
      });
      markersRef.current.push(googleMarker);
    });

    setIsLoading(false);
  }, [allMarkers, center, userLocation, zoom]);

  useEffect(() => {
    let mounted = true;

    async function init() {
      try {
        if (!apiKey) {
          throw new Error('Google Maps API key missing.');
        }
        await loadGoogleMapsScript(apiKey);
        if (!mounted) return;
        renderMap();
      } catch (initError) {
        setError(initError instanceof Error ? initError.message : 'Map initialization failed.');
        setIsLoading(false);
      }
    }

    init();
    return () => {
      mounted = false;
    };
  }, [apiKey, renderMap]);

  useEffect(() => {
    renderMap();
  }, [renderMap]);

  useEffect(() => {
    if (!navigator.geolocation) return;
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const location = { lat: position.coords.latitude, lng: position.coords.longitude };
        setUserLocation(location);
        onLocationChange?.(location);
      },
      () => {
        // Keep map usable even if location permission is denied.
      },
      {
        enableHighAccuracy: true,
        timeout: 8000,
        maximumAge: 30000,
      }
    );
  }, [onLocationChange]);

  return (
    <div className="relative w-full h-full">
      {isLoading && (
        <div className="absolute inset-0 bg-black/50 flex items-center justify-center z-10 rounded-lg">
          <div className="text-center">
            <div className="animate-spin mb-4">
              <Navigation size={40} className="text-blue-500" />
            </div>
            <p className="text-white font-semibold">Loading Map...</p>
          </div>
        </div>
      )}

      {error && (
        <div className="absolute inset-0 bg-red-900/80 flex items-center justify-center z-10 rounded-lg p-3">
          <div className="text-center">
            <AlertCircle size={40} className="text-white mx-auto mb-2" />
            <p className="text-white font-semibold">{error}</p>
          </div>
        </div>
      )}

      <div ref={mapContainerRef} className="w-full h-full rounded-lg overflow-hidden bg-gray-900" />
    </div>
  );
}
