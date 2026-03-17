import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/mongodb';
import Driver from '@/models/Driver';
import EmergencyTrip from '@/models/EmergencyTrip';

const ALLOWED_STATUS = new Set(['assigned', 'accepted', 'in-progress', 'completed', 'cancelled']);
const STAGE_TO_STATUS: Record<string, string> = {
  reached_pickup: 'in-progress',
  reached_hospital: 'accepted',
  completed: 'completed',
};

const EMERGENCY_MAP: Record<string, string> = {
  cardiac: 'heart_attack',
  trauma: 'accident',
  general: 'medical',
};

function normalizeEmergencyType(type: string | undefined): string | null {
  if (!type) return null;
  const value = type.trim().toLowerCase();
  if (!value) return null;
  return EMERGENCY_MAP[value] ?? value;
}

export async function POST(request: NextRequest) {
  try {
    await connectDB();
    const body = await request.json();

    const driverId = String(body?.driverId ?? '').trim();
    const driverEmail = String(body?.driverEmail ?? '').trim().toLowerCase();
    const tripId = String(body?.tripId ?? '').trim();
    const stage = String(body?.stage ?? '').trim();
    const requestedStatus = String(body?.status ?? '').trim();
    const emergencyType = normalizeEmergencyType(body?.emergencyType);
    const latitude = Number(body?.latitude);
    const longitude = Number(body?.longitude);

    if (!driverId && !driverEmail) {
      return NextResponse.json({ error: 'driverId or driverEmail is required.' }, { status: 400 });
    }

    const driver = driverId
      ? await Driver.findById(driverId)
      : await Driver.findOne({ email: driverEmail });

    if (!driver) {
      return NextResponse.json({ error: 'Driver not found.' }, { status: 404 });
    }

    const hasLocation = Number.isFinite(latitude) && Number.isFinite(longitude);
    if (hasLocation) {
      driver.currentLocation = {
        type: 'Point',
        coordinates: [longitude, latitude],
      };
      await driver.save();
    }

    const trip =
      (tripId ? await EmergencyTrip.findById(tripId) : null) ??
      (await EmergencyTrip.findOne({
        driverId: driver._id,
        status: { $in: ['assigned', 'accepted', 'in-progress', 'pending'] },
      }).sort({ createdAt: -1 }));

    if (trip && emergencyType) {
      trip.emergencyType = emergencyType;
    }

    if (trip) {
      const nextStatus = stage ? STAGE_TO_STATUS[stage] : requestedStatus;
      if (nextStatus) {
        if (!ALLOWED_STATUS.has(nextStatus)) {
          return NextResponse.json({ error: 'Invalid status update.' }, { status: 400 });
        }
        trip.status = nextStatus;
      }

      if (trip.status === 'completed') {
        trip.completedAt = new Date();
        driver.isAvailable = true;
        await driver.save();
      }

      await trip.save();
    }

    return NextResponse.json({
      success: true,
      message: 'Driver update applied.',
      trip: trip
        ? {
            id: String(trip._id),
            status: trip.status,
            emergencyType: trip.emergencyType,
            estimatedTime: trip.estimatedTime ?? null,
            hospitalName: trip.hospitalName ?? null,
          }
        : null,
    });
  } catch (error) {
    console.error('Failed to update driver state:', error);
    return NextResponse.json({ error: 'Failed to update driver state.' }, { status: 500 });
  }
}
