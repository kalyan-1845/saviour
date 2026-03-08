import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/mongodb';
import Driver from '@/models/Driver';
import EmergencyTrip from '@/models/EmergencyTrip';

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

export async function POST(request: NextRequest) {
  try {
    await connectDB();
    const body = await request.json();
    const tripId = body?.tripId as string | undefined;
    const requestedDriverId = body?.driverId as string | undefined;

    if (!tripId) {
      return NextResponse.json({ error: 'tripId is required.' }, { status: 400 });
    }

    const trip = await EmergencyTrip.findById(tripId);
    if (!trip) {
      return NextResponse.json({ error: 'Trip not found.' }, { status: 404 });
    }

    let driver = requestedDriverId ? await Driver.findById(requestedDriverId) : null;

    if (driver && !driver.isAvailable) {
      return NextResponse.json({ error: 'Requested driver is not available.' }, { status: 409 });
    }

    if (!driver) {
      if (!trip.pickupLocation?.latitude || !trip.pickupLocation?.longitude) {
        return NextResponse.json({ error: 'Trip pickup location is missing.' }, { status: 400 });
      }

      const availableDrivers = await Driver.find({
        isAvailable: true,
        'currentLocation.coordinates.0': { $exists: true },
        'currentLocation.coordinates.1': { $exists: true },
      });

      driver =
        availableDrivers
          .map((item) => ({
            driver: item,
            distanceKm: calculateDistanceKm(
              trip.pickupLocation.latitude,
              trip.pickupLocation.longitude,
              item.currentLocation.coordinates[1],
              item.currentLocation.coordinates[0]
            ),
          }))
          .sort((a, b) => a.distanceKm - b.distanceKm)[0]?.driver ?? null;

      if (!driver) {
        return NextResponse.json({ error: 'No available driver found.' }, { status: 404 });
      }
    }

    trip.driverId = driver._id;
    trip.status = 'assigned';
    await trip.save();

    driver.isAvailable = false;
    await driver.save();

    return NextResponse.json({
      success: true,
      message: 'Driver assigned successfully.',
      trip: {
        id: trip._id,
        status: trip.status,
        driverId: trip.driverId,
      },
    });
  } catch (error) {
    console.error('Failed to assign driver:', error);
    return NextResponse.json({ error: 'Failed to assign driver.' }, { status: 500 });
  }
}
