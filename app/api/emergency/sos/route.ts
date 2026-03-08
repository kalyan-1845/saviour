import { NextRequest, NextResponse } from 'next/server';
import bcrypt from 'bcryptjs';
import connectDB from '@/lib/mongodb';
import Driver from '@/models/Driver';
import EmergencyTrip from '@/models/EmergencyTrip';
import Hospital from '@/models/Hospital';
import PoliceStation from '@/models/PoliceStation';
import User from '@/models/User';

type EmergencyType = 'medical' | 'accident' | 'fire' | 'crime' | 'police' | 'heart_attack';

const EARTH_RADIUS_KM = 6371;
const DEMO_DRIVER_EMAIL = 'govindammajavvadi85@gmail.com';
const DEMO_DRIVER_PASSWORD = 'raj123';
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

async function getOrCreateDemoDriver(latitude: number, longitude: number) {
  let driver = await Driver.findOne({ email: DEMO_DRIVER_EMAIL });
  if (driver) {
    const hasCoords =
      Array.isArray(driver.currentLocation?.coordinates) &&
      driver.currentLocation.coordinates.length === 2 &&
      Number.isFinite(driver.currentLocation.coordinates[0]) &&
      Number.isFinite(driver.currentLocation.coordinates[1]);

    if (!hasCoords) {
      driver.currentLocation = {
        type: 'Point',
        coordinates: [longitude, latitude],
      };
      await driver.save();
    }

    return driver;
  }

  driver = await Driver.create({
    fullName: 'Govindamma',
    email: DEMO_DRIVER_EMAIL,
    phone: '9701700099',
    licenseNumber: `TS-DEMO-${Date.now()}`,
    vehicleNumber: `TS-09-DEMO-${Math.floor(Math.random() * 900 + 100)}`,
    password: DEMO_DRIVER_PASSWORD,
    isAvailable: true,
    currentLocation: {
      type: 'Point',
      coordinates: [longitude, latitude],
    },
  });

  return driver;
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

    let user = await User.findOne({ phone });
    if (!user) {
      const generatedPassword = Math.random().toString(36).slice(-10);
      const hashedPassword = await bcrypt.hash(generatedPassword, 10);
      user = await User.create({
        fullName: `Emergency User ${phone.slice(-4)}`,
        email: `auto_${phone.replace(/[^\d]/g, '')}@sarathi.in`,
        phone,
        password: hashedPassword,
      });
    }

    const hospitals = medicalEmergency
      ? await Hospital.find({
          isEmergencyAvailable: true,
          bedsAvailable: { $gt: 0 },
        }).lean()
      : [];

    const policeStations = !medicalEmergency
      ? await PoliceStation.find({
          isEmergencyAvailable: true,
        }).lean()
      : [];

    const demoDriver = await getOrCreateDemoDriver(latitude, longitude);
    const driverLongitude = demoDriver.currentLocation?.coordinates?.[0] ?? longitude;
    const driverLatitude = demoDriver.currentLocation?.coordinates?.[1] ?? latitude;
    const demoDriverDistanceKm = calculateDistanceKm(latitude, longitude, driverLatitude, driverLongitude);

    const specialtyMatchedHospitals = hospitals.filter((hospital) =>
      matchesSpecialty(emergencyType, hospital.specialties ?? [])
    );
    const candidateHospitals =
      specialtyMatchedHospitals.length > 0 ? specialtyMatchedHospitals : hospitals;

    const nearestHospital =
      candidateHospitals.length > 0
        ? candidateHospitals
            .filter(
              (hospital) =>
                Number.isFinite(hospital.latitude) &&
                Number.isFinite(hospital.longitude)
            )
            .map((hospital) => ({
              ...hospital,
              distanceKm: calculateDistanceKm(latitude, longitude, hospital.latitude, hospital.longitude),
            }))
            .sort((a, b) => a.distanceKm - b.distanceKm)[0]
        : null;

    const nearestPoliceStation =
      policeStations.length > 0
        ? policeStations
            .filter(
              (station) =>
                Number.isFinite(station.latitude) &&
                Number.isFinite(station.longitude)
            )
            .map((station) => ({
              ...station,
              distanceKm: calculateDistanceKm(latitude, longitude, station.latitude, station.longitude),
            }))
            .sort((a, b) => a.distanceKm - b.distanceKm)[0]
        : null;

    const etaMinutes = estimateEtaMinutes(
      medicalEmergency ? nearestHospital?.distanceKm ?? demoDriverDistanceKm : nearestPoliceStation?.distanceKm ?? 1
    );

    const emergencyTrip = await EmergencyTrip.create({
      userId: user._id,
      driverId: demoDriver._id,
      emergencyType,
      pickupLocation: {
        latitude,
        longitude,
      },
      hospitalId: nearestHospital?._id?.toString(),
      hospitalName: nearestHospital?.name,
      policeStationId: nearestPoliceStation?._id?.toString(),
      policeStationName: nearestPoliceStation?.name,
      dropoffLocation:
        medicalEmergency && nearestHospital
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
      status: 'assigned',
      estimatedTime: etaMinutes,
      distance: medicalEmergency
        ? nearestHospital?.distanceKm ?? demoDriverDistanceKm
        : nearestPoliceStation?.distanceKm,
    });

    return NextResponse.json(
      {
        success: true,
        tripId: emergencyTrip._id,
        status: emergencyTrip.status,
        user: {
          id: user._id,
          phone: user.phone,
          fullName: user.fullName,
        },
        driver: {
          id: demoDriver._id,
          fullName: demoDriver.fullName,
          phone: demoDriver.phone,
          vehicleNumber: demoDriver.vehicleNumber,
          distanceKm: Number(demoDriverDistanceKm.toFixed(2)),
        },
        hospital: nearestHospital
          ? {
              id: nearestHospital._id,
              name: nearestHospital.name,
              address: nearestHospital.address,
              phone: nearestHospital.phone,
              specialties: nearestHospital.specialties,
              distanceKm: Number(nearestHospital.distanceKm.toFixed(2)),
              latitude: nearestHospital.latitude,
              longitude: nearestHospital.longitude,
              mapUrl: `https://www.google.com/maps?q=${nearestHospital.latitude},${nearestHospital.longitude}`,
            }
          : null,
        policeStation: nearestPoliceStation
          ? {
              id: nearestPoliceStation._id,
              name: nearestPoliceStation.name,
              address: nearestPoliceStation.address,
              phone: nearestPoliceStation.phone,
              distanceKm: Number(nearestPoliceStation.distanceKm.toFixed(2)),
              latitude: nearestPoliceStation.latitude,
              longitude: nearestPoliceStation.longitude,
              mapUrl: `https://www.google.com/maps?q=${nearestPoliceStation.latitude},${nearestPoliceStation.longitude}`,
            }
          : null,
        etaMinutes,
        location: {
          latitude,
          longitude,
          mapUrl: `https://www.google.com/maps?q=${latitude},${longitude}`,
        },
        autoRegistered: true,
      },
      { status: 201 }
    );
  } catch (error) {
    console.error('SOS processing failed:', error);
    const message = error instanceof Error ? error.message : 'Unknown error';
    return NextResponse.json(
      { error: 'Failed to process SOS request.', details: message },
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
