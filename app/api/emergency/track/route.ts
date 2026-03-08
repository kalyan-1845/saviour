import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/mongodb';
import Driver from '@/models/Driver';
import EmergencyTrip from '@/models/EmergencyTrip';
import User from '@/models/User';

export async function GET(request: NextRequest) {
  try {
    await connectDB();
    const searchParams = new URL(request.url).searchParams;
    const phone = searchParams.get('phone');
    const tripId = searchParams.get('tripId');

    if (!phone && !tripId) {
      return NextResponse.json({ error: 'phone or tripId is required.' }, { status: 400 });
    }

    let trip = null;

    if (tripId) {
      trip = await EmergencyTrip.findById(tripId).lean();
    } else if (phone) {
      const user = await User.findOne({ phone }).lean();
      if (!user) {
        return NextResponse.json({ error: 'No SOS found for this phone.' }, { status: 404 });
      }
      trip = await EmergencyTrip.findOne({
        userId: user._id,
        status: { $in: ['assigned', 'accepted', 'in-progress', 'pending'] },
      })
        .sort({ createdAt: -1 })
        .lean();
    }

    if (!trip) {
      return NextResponse.json({ error: 'No active SOS trip found.' }, { status: 404 });
    }

    const [user, driver] = await Promise.all([
      trip.userId ? User.findById(trip.userId).select('fullName phone').lean() : null,
      trip.driverId ? Driver.findById(trip.driverId).select('fullName phone vehicleNumber currentLocation').lean() : null,
    ]);

    const pickupLat = trip.pickupLocation?.latitude;
    const pickupLng = trip.pickupLocation?.longitude;
    const dropLat = trip.dropoffLocation?.latitude;
    const dropLng = trip.dropoffLocation?.longitude;
    const driverLat = driver?.currentLocation?.coordinates?.[1];
    const driverLng = driver?.currentLocation?.coordinates?.[0];

    return NextResponse.json({
      success: true,
      trip: {
        id: trip._id,
        status: trip.status,
        emergencyType: trip.emergencyType,
        estimatedTime: trip.estimatedTime,
        hospitalName: trip.hospitalName,
        policeStationName: trip.policeStationName,
        user,
        pickupLocation: trip.pickupLocation,
        dropoffLocation: trip.dropoffLocation,
        pickupMapUrl:
          typeof pickupLat === 'number' && typeof pickupLng === 'number'
            ? `https://www.google.com/maps?q=${pickupLat},${pickupLng}`
            : null,
        hospitalMapUrl:
          typeof dropLat === 'number' && typeof dropLng === 'number'
            ? `https://www.google.com/maps?q=${dropLat},${dropLng}`
            : null,
      },
      driver: driver
        ? {
            id: driver._id,
            fullName: driver.fullName,
            phone: driver.phone,
            vehicleNumber: driver.vehicleNumber,
            currentLocation:
              typeof driverLat === 'number' && typeof driverLng === 'number'
                ? {
                    latitude: driverLat,
                    longitude: driverLng,
                    mapUrl: `https://www.google.com/maps?q=${driverLat},${driverLng}`,
                  }
                : null,
          }
        : null,
      message:
        trip.status === 'accepted'
          ? 'Driver reached hospital.'
          : trip.status === 'in-progress'
            ? 'Driver reached your pickup location.'
            : trip.status === 'completed'
              ? 'Trip completed successfully.'
          : trip.status === 'assigned'
            ? 'Driver has been assigned and is on the way.'
            : 'SOS request is being processed.',
    });
  } catch (error) {
    console.error('Failed to track emergency:', error);
    return NextResponse.json({ error: 'Failed to track SOS.' }, { status: 500 });
  }
}
