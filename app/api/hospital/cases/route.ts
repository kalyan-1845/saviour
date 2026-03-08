import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/mongodb';
import Driver from '@/models/Driver';
import EmergencyTrip from '@/models/EmergencyTrip';
import User from '@/models/User';

export async function GET(request: NextRequest) {
  try {
    await connectDB();
    const hospitalId = new URL(request.url).searchParams.get('hospitalId');

    if (!hospitalId) {
      return NextResponse.json({ error: 'hospitalId is required.' }, { status: 400 });
    }

    const trips = await EmergencyTrip.find({
      hospitalId,
      status: { $in: ['assigned', 'in-progress', 'accepted', 'completed'] },
    })
      .sort({ createdAt: -1 })
      .limit(50)
      .lean();

    const userIds = trips.map((trip) => trip.userId).filter(Boolean);
    const driverIds = trips.map((trip) => trip.driverId).filter(Boolean);

    const [users, drivers] = await Promise.all([
      User.find({ _id: { $in: userIds } }).select('fullName phone').lean(),
      Driver.find({ _id: { $in: driverIds } }).select('fullName phone vehicleNumber currentLocation').lean(),
    ]);

    const usersById = new Map(users.map((user) => [String(user._id), user]));
    const driversById = new Map(drivers.map((driver) => [String(driver._id), driver]));

    const cases = trips.map((trip) => {
      const user = usersById.get(String(trip.userId));
      const driver = driversById.get(String(trip.driverId));
      const driverLat = driver?.currentLocation?.coordinates?.[1];
      const driverLng = driver?.currentLocation?.coordinates?.[0];
      const pickupLat = trip.pickupLocation?.latitude;
      const pickupLng = trip.pickupLocation?.longitude;
      const dropLat = trip.dropoffLocation?.latitude;
      const dropLng = trip.dropoffLocation?.longitude;

      return {
        id: trip._id,
        emergencyType: trip.emergencyType,
        status: trip.status,
        hospitalCaseStatus: trip.hospitalCaseStatus ?? 'pending',
        etaMinutes: trip.estimatedTime ?? null,
        createdAt: trip.createdAt,
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
              mapUrl:
                typeof driverLat === 'number' && typeof driverLng === 'number'
                  ? `https://www.google.com/maps?q=${driverLat},${driverLng}`
                  : null,
            }
          : null,
        pickupMapUrl:
          typeof pickupLat === 'number' && typeof pickupLng === 'number'
            ? `https://www.google.com/maps?q=${pickupLat},${pickupLng}`
            : null,
        hospitalMapUrl:
          typeof dropLat === 'number' && typeof dropLng === 'number'
            ? `https://www.google.com/maps?q=${dropLat},${dropLng}`
            : null,
      };
    });

    return NextResponse.json({ success: true, cases });
  } catch (error) {
    console.error('Failed to fetch hospital cases:', error);
    return NextResponse.json({ error: 'Failed to fetch hospital cases.' }, { status: 500 });
  }
}

export async function PATCH(request: NextRequest) {
  try {
    await connectDB();
    const body = await request.json();
    const tripId = String(body?.tripId ?? '').trim();
    const hospitalCaseStatus = String(body?.hospitalCaseStatus ?? '').trim();

    if (!tripId || !hospitalCaseStatus) {
      return NextResponse.json({ error: 'tripId and hospitalCaseStatus are required.' }, { status: 400 });
    }

    if (!['pending', 'registered', 'ready'].includes(hospitalCaseStatus)) {
      return NextResponse.json({ error: 'Invalid hospitalCaseStatus.' }, { status: 400 });
    }

    const trip = await EmergencyTrip.findById(tripId);
    if (!trip) {
      return NextResponse.json({ error: 'Trip not found.' }, { status: 404 });
    }

    trip.hospitalCaseStatus = hospitalCaseStatus as 'pending' | 'registered' | 'ready';
    if (hospitalCaseStatus === 'registered') {
      trip.hospitalCaseRegisteredAt = new Date();
    }
    await trip.save();

    return NextResponse.json({
      success: true,
      trip: {
        id: trip._id,
        hospitalCaseStatus: trip.hospitalCaseStatus,
      },
    });
  } catch (error) {
    console.error('Failed to update hospital case status:', error);
    return NextResponse.json({ error: 'Failed to update case status.' }, { status: 500 });
  }
}
