import { NextRequest } from 'next/server';
import { PATCH as updateHospitalCase } from '@/app/api/hospital/cases/route';

// Compatibility endpoint:
// POST /api/hospital/update { tripId, hospitalCaseStatus }
export async function POST(request: NextRequest) {
  return updateHospitalCase(request);
}
