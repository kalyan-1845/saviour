import { NextRequest, NextResponse } from 'next/server';

/**
 * POST /api/traffic
 * Green Corridor Messaging Logic
n * Sends notifications to traffic systems and police for clearing emergency corridors
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

    // Simulate sending green corridor messages to traffic systems
    console.log('Green Corridor Alert:', {
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
      policeStations: [
        { station: 'Central Police Station', status: 'Notified' },
        { station: 'Traffic Control Center', status: 'Notified' },
      ],
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
 * Get current traffic status and heatmap data
 */
export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const lat = searchParams.get('lat');
    const lng = searchParams.get('lng');
    const radius = searchParams.get('radius') || '5';

    // Mock traffic heatmap data
    const trafficZones = [
      {
        area: 'Main Road',
        status: 'heavy',
        congestion: 85,
        avgSpeed: 15,
      },
      {
        area: 'Side Street A',
        status: 'moderate',
        congestion: 50,
        avgSpeed: 35,
      },
      {
        area: 'Highway Exit',
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
