import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/mongodb';
import EmergencyTrip from '@/models/EmergencyTrip';
import Hospital from '@/models/Hospital';

const EMERGENCY_MAP: Record<string, string> = {
  cardiac: 'heart_attack',
  trauma: 'accident',
  general: 'medical',
  pediatric: 'pediatric',
  stroke: 'stroke',
  burn: 'burn',
};

const SPECIALTY_KEYWORDS: Record<string, string[]> = {
  heart_attack: ['cardio', 'cardiac', 'heart'],
  stroke: ['neuro', 'stroke'],
  accident: ['trauma', 'orthopedic', 'emergency'],
  burn: ['burn', 'plastic', 'emergency'],
  pediatric: ['pediatric', 'children', 'emergency'],
  medical: ['emergency', 'general'],
};

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

function estimateEtaMinutes(distanceKm: number): number {
  if (!Number.isFinite(distanceKm) || distanceKm <= 0) return 1;
  const avgSpeedKmph = 35;
  return Math.max(1, Math.ceil((distanceKm / avgSpeedKmph) * 60));
}

function matchesSpecialty(emergencyType: string, specialties: string[]): boolean {
  const keywords = SPECIALTY_KEYWORDS[emergencyType] ?? SPECIALTY_KEYWORDS.medical;
  if (!specialties?.length) return false;
  const lowerSpecs = specialties.map((s) => s.toLowerCase());
  return keywords.some((keyword) => lowerSpecs.some((spec) => spec.includes(keyword)));
}

export async function PATCH(request: NextRequest) {
  try {
    await connectDB();
    const body = await request.json();
    const tripId = String(body?.tripId ?? '').trim();
    const driverEmergencyType = String(body?.emergencyType ?? '').trim();

    if (!tripId || !driverEmergencyType) {
      return NextResponse.json({ error: 'tripId and emergencyType are required.' }, { status: 400 });
    }

    const mappedType = EMERGENCY_MAP[driverEmergencyType] ?? driverEmergencyType;
    const trip = await EmergencyTrip.findById(tripId);
    if (!trip) {
      return NextResponse.json({ error: 'Trip not found.' }, { status: 404 });
    }

    trip.emergencyType = mappedType;

    if (
      Number.isFinite(trip.pickupLocation?.latitude) &&
      Number.isFinite(trip.pickupLocation?.longitude)
    ) {
      const pickupLat = Number(trip.pickupLocation?.latitude);
      const pickupLng = Number(trip.pickupLocation?.longitude);

      const hospitals = await Hospital.find({
        isEmergencyAvailable: true,
        bedsAvailable: { $gt: 0 },
      }).lean();

      const specialtyMatched = hospitals.filter((hospital) =>
        matchesSpecialty(mappedType, hospital.specialties ?? [])
      );
      const candidateHospitals = specialtyMatched.length > 0 ? specialtyMatched : hospitals;

      const nearestHospital = candidateHospitals
        .filter((hospital) => Number.isFinite(hospital.latitude) && Number.isFinite(hospital.longitude))
        .map((hospital) => ({
          ...hospital,
          distanceKm: calculateDistanceKm(pickupLat, pickupLng, hospital.latitude, hospital.longitude),
        }))
        .sort((a, b) => a.distanceKm - b.distanceKm)[0];

      if (nearestHospital) {
        trip.hospitalId = String(nearestHospital._id);
        trip.hospitalName = nearestHospital.name;
        trip.dropoffLocation = {
          latitude: nearestHospital.latitude,
          longitude: nearestHospital.longitude,
          address: nearestHospital.address,
        };
        trip.distance = nearestHospital.distanceKm;
        trip.estimatedTime = estimateEtaMinutes(nearestHospital.distanceKm);
      }
    }

    await trip.save();

    return NextResponse.json({
      success: true,
      trip: {
        id: trip._id,
        emergencyType: trip.emergencyType,
        hospitalId: trip.hospitalId,
        hospitalName: trip.hospitalName,
        estimatedTime: trip.estimatedTime,
      },
    });
  } catch (error) {
    console.error('Failed to update emergency type:', error);
    return NextResponse.json({ error: 'Failed to update emergency type.' }, { status: 500 });
  }
}
