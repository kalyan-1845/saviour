import { NextRequest } from 'next/server';
import { GET as emergencySosGet, POST as emergencySosPost } from '@/app/api/emergency/sos/route';

// Compatibility endpoint required by mobile clients:
// POST /api/sos
export async function POST(request: NextRequest) {
  return emergencySosPost(request);
}

// Optional read-through for trip status by tripId.
export async function GET(request: NextRequest) {
  return emergencySosGet(request);
}
