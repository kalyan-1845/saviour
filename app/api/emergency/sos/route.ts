import { NextRequest, NextResponse } from 'next/server';
import bcrypt from 'bcryptjs';
import connectDB from '@/lib/mongodb';
import { coordinateEmergency } from '@/lib/openclaw';
import Driver from '@/models/Driver';
import EmergencyTrip from '@/models/EmergencyTrip';
import Hospital from '@/models/Hospital';
import PoliceStation from '@/models/PoliceStation';
import User from '@/models/User';

type EmergencyType = 'medical' | 'accident' | 'fire' | 'crime' | 'police' | 'heart_attack';

const EARTH_RADIUS_KM = 6371;
const SPECIALTY_KEYWORDS: Record<string, string[]> = {
  heart_attack: ['cardio', 'cardiac', 'heart'],
  stroke: ['neuro', 'stroke'],
  accident: ['trauma', 'orthopedic', 'emergency'],
  burn: ['burn', 'plastic', 'emergency'],
  pediatric: ['pediatric', 'children', 'emergency'],
  medical: ['emergency', 'general'],
};

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

function estimateEtaMinutes(distanceKm: number): number {
  if (!Number.isFinite(distanceKm) || distanceKm <= 0) return 1;
  const avgSpeedKmph = 35; // city emergency driving baseline
  return Math.max(1, Math.ceil((distanceKm / avgSpeedKmph) * 60));
}

function matchesSpecialty(emergencyType: string, specialties: string[]): boolean {
  const keywords = SPECIALTY_KEYWORDS[emergencyType] ?? SPECIALTY_KEYWORDS.medical;
  if (!specialties?.length) return false;
  const lowerSpecs = specialties.map((s) => s.toLowerCase());
  return keywords.some((keyword) => lowerSpecs.some((spec) => spec.includes(keyword)));
}

function isMedicalEmergency(type: EmergencyType): boolean {
  return type === 'medical' || type === 'heart_attack' || type === 'accident';
}

function sanitizePhone(phone: string): string {
  return phone.replace(/[^\d]/g, '').trim();
}

function validateCoordinates(latitude: unknown, longitude: unknown): latitude is number {
  return (
    typeof latitude === 'number' &&
    typeof longitude === 'number' &&
    latitude >= -90 &&
    latitude <= 90 &&
    longitude >= -180 &&
    longitude <= 180
  );
}

export async function POST(request: NextRequest) {
  try {
    await connectDB();

    const body = await request.json();
    const phone = sanitizePhone(body?.phone ?? '');
    const latitude = Number(body?.latitude);
    const longitude = Number(body?.longitude);
    const emergencyType = (body?.emergencyType ?? 'medical') as EmergencyType;

    if (!phone || !validateCoordinates(latitude, longitude)) {
      return NextResponse.json(
        { error: 'Valid phone, latitude, and longitude are required.' },
        { status: 400 }
      );
    }

    const medicalEmergency = isMedicalEmergency(emergencyType);

    // Auto-registration / lookup
    let user = await User.findOne({ phone });
    let autoRegistered = false;
    if (!user) {
      const generatedPassword = Math.random().toString(36).slice(-10);
      const hashedPassword = await bcrypt.hash(generatedPassword, 10);
      user = await User.create({
        fullName: `Citizen ${phone.slice(-4)}`,
        email: `sos_${phone.replace(/[^\d]/g, '')}_${Date.now()}@sarathi.emergency`,
        phone,
        password: hashedPassword,
      });
      autoRegistered = true;
    }

    // 1. Find the nearest available driver within 20km
    const nearestDriver = await Driver.findOne({
      isAvailable: true,
      currentLocation: {
        $nearSphere: {
          $geometry: {
            type: 'Point',
            coordinates: [longitude, latitude],
          },
          $maxDistance: 20000, // 20km in meters
        },
      },
    });

    if (!nearestDriver) {
      return NextResponse.json(
        { error: 'No available drivers found within response radius (20km).' },
        { status: 404 }
      );
    }

    const driverLongitude = nearestDriver.currentLocation?.coordinates?.[0] ?? longitude;
    const driverLatitude = nearestDriver.currentLocation?.coordinates?.[1] ?? latitude;
    const driverDistanceKm = calculateDistanceKm(latitude, longitude, driverLatitude, driverLongitude);

    // 2. Find nearest hospital/police station
    const hospitals = medicalEmergency
      ? await Hospital.find({
          isEmergencyAvailable: true,
          bedsAvailable: { $gt: 0 },
        }).lean()
      : [];

    const policeStations = await PoliceStation.find({
      isEmergencyAvailable: true,
    }).lean();

    const nearestHospital = medicalEmergency && hospitals.length > 0
      ? hospitals
          .map((h) => ({
            ...h,
            distanceKm: calculateDistanceKm(latitude, longitude, h.latitude, h.longitude),
          }))
          .sort((a, b) => a.distanceKm - b.distanceKm)[0]
      : null;

    const nearestPoliceStation = policeStations.length > 0
      ? policeStations
          .map((s) => ({
            ...s,
            distanceKm: calculateDistanceKm(latitude, longitude, s.latitude, s.longitude),
          }))
          .sort((a, b) => a.distanceKm - b.distanceKm)[0]
      : null;

    const etaMinutes = estimateEtaMinutes(
      medicalEmergency ? (nearestHospital?.distanceKm ?? driverDistanceKm) : (nearestPoliceStation?.distanceKm ?? 1)
    );

    // 3. Create the trip
    const emergencyTrip = await EmergencyTrip.create({
      userId: user._id,
      driverId: nearestDriver._id,
      emergencyType,
      pickupLocation: { latitude, longitude },
      hospitalId: nearestHospital?._id?.toString(),
      hospitalName: nearestHospital?.name,
      policeStationId: nearestPoliceStation?._id?.toString(),
      policeStationName: nearestPoliceStation?.name,
      status: 'assigned',
      estimatedTime: etaMinutes,
      distance: medicalEmergency
        ? (nearestHospital?.distanceKm ?? driverDistanceKm)
        : (nearestPoliceStation?.distanceKm ?? 1),
      dropoffLocation: medicalEmergency && nearestHospital
        ? {
            latitude: nearestHospital.latitude,
            longitude: nearestHospital.longitude,
            address: nearestHospital.address,
          }
        : !medicalEmergency && nearestPoliceStation
        ? {
            latitude: nearestPoliceStation.latitude,
            longitude: nearestPoliceStation.longitude,
            address: nearestPoliceStation.address,
          }
        : undefined,
    });

    // Trigger OpenClaw Coordination (Async)
    const tripData = {
      tripId: emergencyTrip._id,
      emergencyType,
      user: { fullName: user.fullName, phone: user.phone },
      driver: { fullName: nearestDriver.fullName, phone: nearestDriver.phone },
      hospital: nearestHospital,
      policeStation: nearestPoliceStation,
      etaMinutes,
    };
    coordinateEmergency(tripData).catch(err => console.error('Agent coordination error:', err));

    return NextResponse.json(
      {
        success: true,
        tripId: emergencyTrip._id,
        status: emergencyTrip.status,
        emergencyType,
        user: {
          id: user._id,
          phone: user.phone,
          fullName: user.fullName,
        },
        driver: {
          id: nearestDriver._id,
          fullName: nearestDriver.fullName,
          phone: nearestDriver.phone,
          vehicleNumber: nearestDriver.vehicleNumber,
          distanceKm: Number(driverDistanceKm.toFixed(2)),
        },
        hospital: nearestHospital ? {
          id: nearestHospital._id,
          name: nearestHospital.name,
          phone: nearestHospital.phone,
          distanceKm: Number(nearestHospital.distanceKm.toFixed(2)),
          latitude: nearestHospital.latitude,
          longitude: nearestHospital.longitude,
        } : null,
        policeStation: nearestPoliceStation ? {
          id: nearestPoliceStation._id,
          name: nearestPoliceStation.name,
          phone: nearestPoliceStation.phone,
          distanceKm: Number(nearestPoliceStation.distanceKm.toFixed(2)),
          latitude: nearestPoliceStation.latitude,
          longitude: nearestPoliceStation.longitude,
        } : null,
        etaMinutes,
        autoRegistered,
      },
      { status: 201 }
    );
  } catch (error) {
    console.error('SOS processing failed:', error);
    return NextResponse.json(
      { error: 'Failed to process SOS request.', details: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    );
  }
}

export async function GET(request: NextRequest) {
  try {
    await connectDB();
    const tripId = new URL(request.url).searchParams.get('tripId');

    if (!tripId) {
      return NextResponse.json({ error: 'tripId is required.' }, { status: 400 });
    }

    const trip = await EmergencyTrip.findById(tripId)
      .populate('userId', 'fullName phone')
      .populate('driverId', 'fullName phone vehicleNumber')
      .lean();

    if (!trip) {
      return NextResponse.json({ error: 'Trip not found.' }, { status: 404 });
    }

    return NextResponse.json({ success: true, trip });
  } catch (error) {
    console.error('Failed to fetch SOS trip:', error);
    return NextResponse.json({ error: 'Failed to fetch SOS trip.' }, { status: 500 });
  }
}
