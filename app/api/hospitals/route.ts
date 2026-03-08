import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/mongodb';
import Hospital from '@/models/Hospital';

const EARTH_RADIUS_KM = 6371;

function calculateDistanceKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);
  return EARTH_RADIUS_KM * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
}

export async function GET(request: NextRequest) {
  try {
    await connectDB();
    const { searchParams } = new URL(request.url);
    const specialization = searchParams.get('specialization');
    const latitude = Number(searchParams.get('latitude'));
    const longitude = Number(searchParams.get('longitude'));

    const query: Record<string, unknown> = { isEmergencyAvailable: true };
    if (specialization) {
      query.specialties = { $regex: specialization, $options: 'i' };
    }

    const hospitals = await Hospital.find(query).lean();

    const enrichedHospitals = hospitals
      .map((hospital) => ({
        ...hospital,
        distanceKm:
          Number.isFinite(latitude) && Number.isFinite(longitude)
            ? calculateDistanceKm(latitude, longitude, hospital.latitude, hospital.longitude)
            : null,
      }))
      .sort((a, b) => {
        if (a.distanceKm == null) return 0;
        if (b.distanceKm == null) return 0;
        return a.distanceKm - b.distanceKm;
      });

    return NextResponse.json({
      success: true,
      count: enrichedHospitals.length,
      hospitals: enrichedHospitals,
    });
  } catch (error) {
    console.error('Failed to load hospitals:', error);
    return NextResponse.json({ error: 'Failed to load hospitals.' }, { status: 500 });
  }
}
