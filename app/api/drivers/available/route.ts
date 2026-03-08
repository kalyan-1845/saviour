import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/mongodb';
import Driver from '@/models/Driver';

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
    const latitude = Number(searchParams.get('latitude'));
    const longitude = Number(searchParams.get('longitude'));

    const drivers = await Driver.find({
      isAvailable: true,
      'currentLocation.coordinates.0': { $exists: true },
      'currentLocation.coordinates.1': { $exists: true },
    }).lean();

    const availableDrivers = drivers
      .map((driver) => ({
        id: driver._id,
        fullName: driver.fullName,
        phone: driver.phone,
        vehicleNumber: driver.vehicleNumber,
        licenseNumber: driver.licenseNumber,
        isAvailable: driver.isAvailable,
        location: {
          latitude: driver.currentLocation.coordinates[1],
          longitude: driver.currentLocation.coordinates[0],
        },
        distanceKm:
          Number.isFinite(latitude) && Number.isFinite(longitude)
            ? calculateDistanceKm(
                latitude,
                longitude,
                driver.currentLocation.coordinates[1],
                driver.currentLocation.coordinates[0]
              )
            : null,
      }))
      .sort((a, b) => {
        if (a.distanceKm == null) return 0;
        if (b.distanceKm == null) return 0;
        return a.distanceKm - b.distanceKm;
      });

    return NextResponse.json({
      success: true,
      count: availableDrivers.length,
      drivers: availableDrivers,
    });
  } catch (error) {
    console.error('Failed to load available drivers:', error);
    return NextResponse.json({ error: 'Failed to load available drivers.' }, { status: 500 });
  }
}
