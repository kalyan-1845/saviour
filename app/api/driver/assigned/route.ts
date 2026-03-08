import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/mongodb';
import Driver from '@/models/Driver';
import EmergencyTrip from '@/models/EmergencyTrip';

const EARTH_RADIUS_KM = 6371;

function haversineKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
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
    const email = new URL(request.url).searchParams.get('email');

    if (!email) {
      return NextResponse.json({ error: 'Driver email is required.' }, { status: 400 });
    }

    const driver = await Driver.findOne({ email: email.toLowerCase() }).lean();
    if (!driver) {
      return NextResponse.json({ error: 'Driver not found.' }, { status: 404 });
    }

    const trip = await EmergencyTrip.findOne({
      driverId: driver._id,
      status: { $in: ['assigned', 'accepted', 'in-progress'] },
    })
      .sort({ createdAt: -1 })
      .populate('userId', 'fullName phone')
      .lean();

    if (!trip) {
      return NextResponse.json({ success: true, trip: null });
    }

    const pickupLat = trip.pickupLocation?.latitude;
    const pickupLng = trip.pickupLocation?.longitude;
    const dropLat = trip.dropoffLocation?.latitude;
    const dropLng = trip.dropoffLocation?.longitude;
    const driverLng = driver.currentLocation?.coordinates?.[0];
    const driverLat = driver.currentLocation?.coordinates?.[1];

    let estimatedTime = trip.estimatedTime ?? null;
    if (
      typeof driverLat === 'number' &&
      typeof driverLng === 'number' &&
      typeof dropLat === 'number' &&
      typeof dropLng === 'number'
    ) {
      const distanceKm = haversineKm(driverLat, driverLng, dropLat, dropLng);
      const avgSpeedKmph = 35;
      estimatedTime = Math.max(1, Math.ceil((distanceKm / avgSpeedKmph) * 60));
    }

    return NextResponse.json({
      success: true,
      trip: {
        id: trip._id,
        status: trip.status,
        emergencyType: trip.emergencyType,
        hospitalCaseStatus: trip.hospitalCaseStatus ?? 'pending',
        hospitalCaseRegisteredAt: trip.hospitalCaseRegisteredAt ?? null,
        estimatedTime,
        createdAt: trip.createdAt,
        user: trip.userId,
        pickupLocation: trip.pickupLocation,
        dropoffLocation: trip.dropoffLocation,
        hospitalName: trip.hospitalName,
        policeStationName: trip.policeStationName,
        mapUrl:
          typeof pickupLat === 'number' && typeof pickupLng === 'number'
            ? `https://www.google.com/maps?q=${pickupLat},${pickupLng}`
            : null,
        hospitalMapUrl:
          typeof dropLat === 'number' && typeof dropLng === 'number'
            ? `https://www.google.com/maps?q=${dropLat},${dropLng}`
            : null,
      },
    });
  } catch (error) {
    console.error('Failed to fetch assigned trip:', error);
    return NextResponse.json({ error: 'Failed to fetch assigned trip.' }, { status: 500 });
  }
}
