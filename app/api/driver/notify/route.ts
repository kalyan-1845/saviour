import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/mongodb';
import Driver from '@/models/Driver';
import EmergencyTrip from '@/models/EmergencyTrip';
import PoliceStation from '@/models/PoliceStation';

const EARTH_RADIUS_KM = 6371;

function distanceKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
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
    const tripId = String(body?.tripId ?? '').trim();
    const driverEmail = String(body?.driverEmail ?? '').trim().toLowerCase();
    const latitude = Number(body?.location?.latitude);
    const longitude = Number(body?.location?.longitude);
    const etaMinutes = Number(body?.etaMinutes);

    if (!tripId || !driverEmail || !Number.isFinite(latitude) || !Number.isFinite(longitude)) {
      return NextResponse.json(
        { error: 'tripId, driverEmail and valid location are required.' },
        { status: 400 }
      );
    }

    const [driver, trip] = await Promise.all([
      Driver.findOne({ email: driverEmail }),
      EmergencyTrip.findById(tripId).populate('userId', 'fullName phone'),
    ]);

    if (!driver) {
      return NextResponse.json({ error: 'Driver not found.' }, { status: 404 });
    }
    if (!trip) {
      return NextResponse.json({ error: 'Trip not found.' }, { status: 404 });
    }

    driver.currentLocation = {
      type: 'Point',
      coordinates: [longitude, latitude],
    };
    await driver.save();

    if (Number.isFinite(etaMinutes) && etaMinutes > 0) {
      trip.estimatedTime = Math.round(etaMinutes);
      await trip.save();
    }

    const nearbyPolice = await PoliceStation.find({ isEmergencyAvailable: true }).lean();
    const nearestPolice = nearbyPolice
      .map((station) => ({
        station,
        d: distanceKm(latitude, longitude, station.latitude, station.longitude),
      }))
      .sort((a, b) => a.d - b.d)
      .slice(0, 3)
      .map((item) => item.station);

    const destinationLat = trip.dropoffLocation?.latitude;
    const destinationLng = trip.dropoffLocation?.longitude;
    const routeDistanceKm =
      Number.isFinite(destinationLat) && Number.isFinite(destinationLng)
        ? distanceKm(latitude, longitude, Number(destinationLat), Number(destinationLng))
        : Number.isFinite(trip.distance)
          ? Number(trip.distance)
          : 1;

    const congestionScore = Math.min(
      95,
      Math.max(15, Math.round(routeDistanceKm * 6 + Number(trip.estimatedTime ?? 1) * 2))
    );
    const congestionLevel =
      congestionScore >= 75 ? 'severe' : congestionScore >= 50 ? 'moderate' : 'light';
    const estimatedSignals = Math.max(2, Math.round(routeDistanceKm * 3));
    const redSignalRisk = Math.min(estimatedSignals, Math.max(1, Math.round((congestionScore / 100) * estimatedSignals)));
    const estimatedTimeToClearMins = Math.max(2, Math.round(redSignalRisk * 1.5));

    const user = trip.userId as { fullName?: string; phone?: string } | null;
    const emergencyType = String(trip.emergencyType ?? 'medical').toUpperCase();
    const hospitalMessage = [
      'EMERGENCY PRE-ARRIVAL ALERT',
      `Emergency Type: ${emergencyType}`,
      `Driver: ${driver.fullName}`,
      `Vehicle Number: ${driver.vehicleNumber}`,
      `Patient Contact: ${user?.phone ?? 'N/A'}`,
      `ETA: ${trip.estimatedTime ?? 'N/A'} minutes`,
      `Driver Live Location: https://www.google.com/maps?q=${latitude},${longitude}`,
      `Pickup Location: ${
        trip.pickupLocation?.latitude && trip.pickupLocation?.longitude
          ? `https://www.google.com/maps?q=${trip.pickupLocation.latitude},${trip.pickupLocation.longitude}`
          : 'N/A'
      }`,
    ].join('\n');

    const policeMessage = [
      'GREEN CORRIDOR / POLICE ALERT',
      `Emergency Vehicle: Ambulance`,
      `Vehicle Number: ${driver.vehicleNumber}`,
      `Driver: ${driver.fullName} (${driver.phone})`,
      `Emergency Type: ${emergencyType}`,
      `Route Distance: ${routeDistanceKm.toFixed(1)} km`,
      `Current ETA to destination: ${trip.estimatedTime ?? 'N/A'} minutes`,
      `Traffic Congestion: ${congestionLevel.toUpperCase()} (${congestionScore}/100)`,
      `Estimated Traffic Signals on Route: ${estimatedSignals}`,
      `Red Signal Intervention Needed: ${redSignalRisk}`,
      `Estimated Signal Clearance Window: ${estimatedTimeToClearMins} min`,
      `Live Location: https://www.google.com/maps?q=${latitude},${longitude}`,
      'Request: Kindly clear route and support emergency passage.',
    ].join('\n');

    trip.policeAlertMessage = policeMessage;
    trip.notifiedPoliceStationIds = nearestPolice.map((station) => String(station._id));
    trip.policeAlertMeta = {
      routeDistanceKm: Number(routeDistanceKm.toFixed(2)),
      congestionScore,
      congestionLevel,
      estimatedSignals,
      redSignalRisk,
      estimatedTimeToClearMins,
      generatedAt: new Date(),
    };
    await trip.save();

    return NextResponse.json({
      success: true,
      hospital: {
        name: trip.hospitalName ?? 'Assigned Hospital',
        message: hospitalMessage,
      },
      police: {
        stations: nearestPolice.map((station) => ({
          id: String(station._id),
          name: station.name,
          phone: station.phone,
          jurisdiction: station.jurisdiction,
        })),
        message: policeMessage,
        analysis: {
          routeDistanceKm: Number(routeDistanceKm.toFixed(2)),
          congestionScore,
          congestionLevel,
          estimatedSignals,
          redSignalRisk,
          estimatedTimeToClearMins,
        },
      },
      live: {
        driverLocationMap: `https://www.google.com/maps?q=${latitude},${longitude}`,
        etaMinutes: trip.estimatedTime ?? null,
      },
    });
  } catch (error) {
    console.error('Failed to notify hospital/police:', error);
    return NextResponse.json({ error: 'Failed to send notifications.' }, { status: 500 });
  }
}
