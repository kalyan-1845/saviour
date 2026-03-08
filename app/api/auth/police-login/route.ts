import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/mongodb';
import PoliceStation from '@/models/PoliceStation';

export async function POST(request: NextRequest) {
  try {
    await connectDB();
    const body = await request.json();
    const policeStationId = String(body?.policeStationId ?? '').trim();
    const phone = String(body?.phone ?? '').replace(/[^\d]/g, '');

    if (!policeStationId || !phone) {
      return NextResponse.json({ error: 'policeStationId and phone are required.' }, { status: 400 });
    }

    const station = await PoliceStation.findById(policeStationId).lean();
    if (!station) {
      return NextResponse.json({ error: 'Police station not found.' }, { status: 404 });
    }

    const stationPhone = String(station.phone ?? '').replace(/[^\d]/g, '');
    if (!stationPhone || stationPhone.slice(-6) !== phone.slice(-6)) {
      return NextResponse.json({ error: 'Invalid police station phone verification.' }, { status: 401 });
    }

    return NextResponse.json({
      success: true,
      station: {
        id: String(station._id),
        name: station.name,
        phone: station.phone,
        jurisdiction: station.jurisdiction,
        zone: station.zone,
        city: station.city,
      },
    });
  } catch (error) {
    console.error('Police login failed:', error);
    return NextResponse.json({ error: 'Failed to login police station.' }, { status: 500 });
  }
}
