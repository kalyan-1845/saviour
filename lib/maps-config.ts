/**
 * Google Maps API Configuration
 * Setup for Maps API with traffic layer support
 */

if (!process.env.NEXT_PUBLIC_GOOGLE_MAPS_KEY) {
  console.warn('NEXT_PUBLIC_GOOGLE_MAPS_KEY environment variable is not set');
}

export const MAPS_CONFIG = {
  apiKey: process.env.NEXT_PUBLIC_GOOGLE_MAPS_KEY,
  defaultCenter: {
    lat: 0,
    lng: 0,
  },
  defaultZoom: 15,
  mapOptions: {
    disableDefaultUI: false,
    zoomControl: true,
    mapTypeControl: true,
    streetViewControl: true,
  },
};

export function loadGoogleMapsScript() {
  if (typeof window !== 'undefined' && !window.google) {
    const script = document.createElement('script');
    script.src = `https://maps.googleapis.com/maps/api/js?key=${MAPS_CONFIG.apiKey}&libraries=places,routes`;
    script.async = true;
    script.defer = true;
    document.head.appendChild(script);
  }
}
