import { NextRequest } from 'next/server';
import { GET as getAssignedTrip } from '@/app/api/driver/assigned/route';

// Compatibility endpoint:
// GET /api/driver/trips?driverId=...|email=...
export async function GET(request: NextRequest) {
  return getAssignedTrip(request);
}
