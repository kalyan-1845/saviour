import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/mongodb';
import Hospital from '@/models/Hospital';

/**
 * GET /api/hospital
 * Get available hospitals with filters from database
 */
export async function GET(request: NextRequest) {
  try {
    await connectDB();

    const { searchParams } = new URL(request.url);
    const specialization = searchParams.get('specialization');
    const latitudeParam = searchParams.get('latitude');
    const longitudeParam = searchParams.get('longitude');

    let query: any = {};

    // Filter by specialization
    if (specialization) {
      query.specialties = { $regex: specialization, $options: 'i' };
    }

    let hospitals = await Hospital.find(query);

    // Sort by distance if coordinates provided
    if (latitudeParam && longitudeParam) {
      const userLat = parseFloat(latitudeParam);
      const userLng = parseFloat(longitudeParam);

      hospitals.sort((a, b) => {
        const distA = Math.sqrt(
          Math.pow(a.latitude - userLat, 2) + Math.pow(a.longitude - userLng, 2)
        );
        const distB = Math.sqrt(
          Math.pow(b.latitude - userLat, 2) + Math.pow(b.longitude - userLng, 2)
        );
        return distA - distB;
      });
    }

    return NextResponse.json({ success: true, hospitals });
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

      // TODO: Integrate with hospital notification system (e.g., Twilio/Email)

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
