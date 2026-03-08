import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/mongodb';
import Driver from '@/models/Driver';
import EmergencyTrip from '@/models/EmergencyTrip';

const ALLOWED_STATUS = new Set(['accepted', 'in-progress', 'completed', 'cancelled']);
const ALLOWED_STAGE = new Set(['reached_pickup', 'reached_hospital', 'completed']);

export async function POST(request: NextRequest) {
  try {
    await connectDB();
    const body = await request.json();

    const tripId = body?.tripId as string | undefined;
    const status = body?.status as string | undefined;
    const stage = body?.stage as string | undefined;
    const driverEmail = body?.driverEmail as string | undefined;
    const driverLocation = body?.driverLocation as { latitude: number; longitude: number } | undefined;

    if (!tripId || !driverEmail) {
      return NextResponse.json(
        { error: 'tripId and driverEmail are required.' },
        { status: 400 }
      );
    }

    if (!status && !stage) {
      return NextResponse.json({ error: 'status or stage is required.' }, { status: 400 });
    }

    if (status && !ALLOWED_STATUS.has(status)) {
      return NextResponse.json({ error: 'Invalid status update.' }, { status: 400 });
    }

    if (stage && !ALLOWED_STAGE.has(stage)) {
      return NextResponse.json({ error: 'Invalid stage update.' }, { status: 400 });
    }

    const driver = await Driver.findOne({ email: driverEmail.toLowerCase() });
    if (!driver) {
      return NextResponse.json({ error: 'Driver not found.' }, { status: 404 });
    }

    const trip = await EmergencyTrip.findById(tripId);
    if (!trip) {
      return NextResponse.json({ error: 'Trip not found.' }, { status: 404 });
    }

    let nextStatus = status;
    if (stage === 'reached_pickup') nextStatus = 'in-progress';
    if (stage === 'reached_hospital') nextStatus = 'accepted';
    if (stage === 'completed') nextStatus = 'completed';

    if (nextStatus) {
      trip.status = nextStatus;
    }

    if (nextStatus === 'completed') {
      const now = new Date();
      trip.completedAt = now;
      driver.isAvailable = true;

      // Demo behavior: closing work marks all active trips for this driver as completed.
      await EmergencyTrip.updateMany(
        {
          driverId: driver._id,
          status: { $in: ['assigned', 'accepted', 'in-progress'] },
        },
        {
          $set: {
            status: 'completed',
            completedAt: now,
          },
        }
      );
    }

    if (
      driverLocation &&
      Number.isFinite(driverLocation.latitude) &&
      Number.isFinite(driverLocation.longitude)
    ) {
      driver.currentLocation = {
        type: 'Point',
        coordinates: [driverLocation.longitude, driverLocation.latitude],
      };
    }

    await Promise.all([trip.save(), driver.save()]);

    return NextResponse.json({
      success: true,
      trip: {
        id: trip._id,
        status: trip.status,
        stage:
          trip.status === 'accepted'
            ? 'reached_hospital'
            : trip.status === 'in-progress'
              ? 'reached_pickup'
              : trip.status === 'completed'
                ? 'completed'
                : 'assigned',
      },
    });
  } catch (error) {
    console.error('Failed to update trip status:', error);
    return NextResponse.json({ error: 'Failed to update trip status.' }, { status: 500 });
  }
}
