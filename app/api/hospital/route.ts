import { NextRequest, NextResponse } from 'next/server';

// Mock hospital database
const hospitals = [
  {
    id: '1',
    name: 'Apollo Hospital Delhi',
    latitude: 28.5597,
    longitude: 77.235,
    bedsAvailable: 8,
    specialization: ['Cardiac', 'Trauma', 'Neurology'],
    rating: 4.8,
    contact: '+91-11-XXXX-XXXX',
  },
  {
    id: '2',
    name: 'Max Super Specialty',
    latitude: 28.5812,
    longitude: 77.294,
    bedsAvailable: 5,
    specialization: ['Pediatric', 'Burn', 'Trauma'],
    rating: 4.7,
    contact: '+91-11-XXXX-XXXX',
  },
  {
    id: '3',
    name: 'Delhi Heart & Lung Institute',
    latitude: 28.5693,
    longitude: 77.2109,
    bedsAvailable: 12,
    specialization: ['Cardiac', 'Stroke'],
    rating: 4.9,
    contact: '+91-11-XXXX-XXXX',
  },
];

/**
 * GET /api/hospital
 * Get available hospitals with filters
 */
export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const specialization = searchParams.get('specialization');
    const latitude = searchParams.get('latitude');
    const longitude = searchParams.get('longitude');

    let filtered = hospitals;

    // Filter by specialization
    if (specialization) {
      filtered = filtered.filter((h) =>
        h.specialization.some((s) =>
          s.toLowerCase().includes(specialization.toLowerCase())
        )
      );
    }

    // Sort by distance if coordinates provided
    if (latitude && longitude) {
      const userLat = parseFloat(latitude);
      const userLng = parseFloat(longitude);

      filtered.sort((a, b) => {
        const distA = Math.sqrt(
          Math.pow(a.latitude - userLat, 2) + Math.pow(a.longitude - userLng, 2)
        );
        const distB = Math.sqrt(
          Math.pow(b.latitude - userLat, 2) + Math.pow(b.longitude - userLng, 2)
        );
        return distA - distB;
      });
    }

    return NextResponse.json({ success: true, hospitals: filtered });
  } catch (error) {
    console.error('Hospital fetch error:', error);
    return NextResponse.json(
      { error: 'Failed to fetch hospitals' },
      { status: 500 }
    );
  }
}

/**
 * POST /api/hospital
 * Update hospital bed availability or send pre-arrival alert
 */
export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { hospitalId, action, patientData } = body;

    if (action === 'alert') {
      // Send pre-arrival alert to hospital
      console.log(`Pre-arrival alert sent to hospital ${hospitalId}:`, patientData);
      
      // TODO: Integrate with hospital notification system
      
      return NextResponse.json({
        success: true,
        message: `Hospital ${hospitalId} has been alerted`,
      });
    }

    return NextResponse.json(
      { error: 'Invalid action' },
      { status: 400 }
    );
  } catch (error) {
    console.error('Hospital update error:', error);
    return NextResponse.json(
      { error: 'Failed to update hospital' },
      { status: 500 }
    );
  }
}
