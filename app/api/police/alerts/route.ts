import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/mongodb';
import Driver from '@/models/Driver';
import EmergencyTrip from '@/models/EmergencyTrip';
import User from '@/models/User';
import mongoose from 'mongoose';

export async function GET(request: NextRequest) {
  try {
    await connectDB();
    const stationId = String(new URL(request.url).searchParams.get('stationId') ?? '').trim();

    if (!stationId) {
      return NextResponse.json({ error: 'stationId is required.' }, { status: 400 });
    }

    const stationIdQuery: Record<string, unknown>[] = [{ notifiedPoliceStationIds: stationId }];
    if (mongoose.Types.ObjectId.isValid(stationId)) {
      stationIdQuery.push({ notifiedPoliceStationIds: new mongoose.Types.ObjectId(stationId) });
    }

    const trips = await EmergencyTrip.find({
      $or: stationIdQuery,
    })
      .sort({ updatedAt: -1 })
      .limit(50)
      .lean();

    const userIds = trips
      .map((trip) => String(trip.userId ?? ''))
      .filter((id) => mongoose.Types.ObjectId.isValid(id))
      .map((id) => new mongoose.Types.ObjectId(id));
    const driverIds = trips
      .map((trip) => String(trip.driverId ?? ''))
      .filter((id) => mongoose.Types.ObjectId.isValid(id))
      .map((id) => new mongoose.Types.ObjectId(id));

    const [users, drivers] = await Promise.all([
      userIds.length > 0
        ? User.find({ _id: { $in: userIds } }).select('fullName phone').lean()
        : Promise.resolve([]),
      driverIds.length > 0
        ? Driver.find({ _id: { $in: driverIds } }).select('fullName phone vehicleNumber currentLocation').lean()
        : Promise.resolve([]),
    ]);

    const usersById = new Map(users.map((user) => [String(user._id), user]));
    const driversById = new Map(drivers.map((driver) => [String(driver._id), driver]));

    const alerts = trips.map((trip) => {
      const user = usersById.get(String(trip.userId));
      const driver = driversById.get(String(trip.driverId));
      const driverLat = driver?.currentLocation?.coordinates?.[1];
      const driverLng = driver?.currentLocation?.coordinates?.[0];

      return {
        id: String(trip._id),
        status: trip.status,
        emergencyType: trip.emergencyType,
        etaMinutes: trip.estimatedTime ?? null,
        policeAlertMessage: trip.policeAlertMessage ?? null,
        policeAlertMeta: trip.policeAlertMeta ?? null,
        createdAt: trip.createdAt,
        hospitalName: trip.hospitalName ?? null,
        user: user
          ? {
              fullName: user.fullName,
              phone: user.phone,
            }
          : null,
        driver: driver
          ? {
              fullName: driver.fullName,
              phone: driver.phone,
              vehicleNumber: driver.vehicleNumber,
              liveMapUrl:
                typeof driverLat === 'number' && typeof driverLng === 'number'
                  ? `https://www.google.com/maps?q=${driverLat},${driverLng}`
                  : null,
            }
          : null,
      };
    });

    return NextResponse.json({ success: true, alerts });
  } catch (error) {
    console.error('Failed to fetch police alerts:', error);
    return NextResponse.json({ error: 'Failed to fetch police alerts.' }, { status: 500 });
  }
}
