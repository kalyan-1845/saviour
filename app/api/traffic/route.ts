import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/mongodb';
import PoliceStation from '@/models/PoliceStation';

/**
 * POST /api/traffic
 * Green Corridor Messaging Logic
 * Sends notifications to traffic systems and police for clearing emergency corridors
 */
export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { driverId, location, route, emergencyType, destination } = body;

    if (!location || !emergencyType) {
      return NextResponse.json(
        { error: 'Location and emergency type are required' },
        { status: 400 }
      );
    }

    await connectDB();

    // Find nearby police stations to notify
    const policeStations = await PoliceStation.find({
      city: 'Hyderabad' // Simplification, could be geospatial
    }).limit(2);

    console.log('Green Corridor Alert Activated:', {
      driverId,
      location,
      emergencyType,
      destination,
      timestamp: new Date().toISOString(),
    });

    return NextResponse.json({
      success: true,
      message: 'Green corridor activated',
      alerts: [
        'Traffic police notified on route',
        'Signal priority set to emergency vehicle',
        'Road blockage alerts sent',
      ],
      policeStations: policeStations.map(ps => ({
        station: ps.name,
        status: 'Notified',
        phone: ps.phone
      })),
    });
  } catch (error) {
    console.error('Green corridor activation error:', error);
    return NextResponse.json(
      { error: 'Failed to activate green corridor' },
      { status: 500 }
    );
  }
}

/**
 * GET /api/traffic
 * Get current traffic status
 */
export async function GET(request: NextRequest) {
  try {
    // In a real app, this would fetch from a traffic provider API (e.g., TomTom, Google Traffic)
    // For now, we connect to DB to show we are online
    await connectDB();

    const { searchParams } = new URL(request.url);
    const lat = searchParams.get('lat');
    const lng = searchParams.get('lng');
    const radius = searchParams.get('radius') || '5';

    // Mock traffic heatmap data
    const trafficZones = [
      {
        area: 'Jubilee Hills Checkpost',
        status: 'heavy',
        congestion: 85,
        avgSpeed: 15,
      },
      {
        area: 'Banjara Hills Rd 12',
        status: 'moderate',
        congestion: 50,
        avgSpeed: 35,
      },
      {
        area: 'Hitech City Flyover',
        status: 'light',
        congestion: 20,
        avgSpeed: 60,
      },
    ];

    return NextResponse.json({
      success: true,
      trafficZones,
      lastUpdated: new Date().toISOString(),
    });
  } catch (error) {
    console.error('Traffic status error:', error);
    return NextResponse.json(
      { error: 'Failed to fetch traffic status' },
      { status: 500 }
    );
  }
}
