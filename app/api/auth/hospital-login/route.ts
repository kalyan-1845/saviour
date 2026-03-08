import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/mongodb';
import Hospital from '@/models/Hospital';

export async function POST(req: NextRequest) {
  try {
    await connectDB();
    const body = await req.json();
    const hospitalId = String(body?.hospitalId ?? '').trim();
    const phone = String(body?.phone ?? '').replace(/[^\d]/g, '');

    if (!hospitalId || !phone) {
      return NextResponse.json({ error: 'hospitalId and phone are required.' }, { status: 400 });
    }

    const hospital = await Hospital.findById(hospitalId).lean();
    if (!hospital) {
      return NextResponse.json({ error: 'Hospital not found.' }, { status: 404 });
    }

    const hospitalPhoneDigits = String(hospital.phone ?? '').replace(/[^\d]/g, '');
    if (!hospitalPhoneDigits || hospitalPhoneDigits.slice(-6) !== phone.slice(-6)) {
      return NextResponse.json({ error: 'Invalid hospital phone verification.' }, { status: 401 });
    }

    return NextResponse.json({
      success: true,
      hospital: {
        id: hospital._id,
        name: hospital.name,
        phone: hospital.phone,
        address: hospital.address,
        city: hospital.city,
        zone: hospital.zone,
      },
    });
  } catch (error) {
    console.error('Hospital login failed:', error);
    return NextResponse.json({ error: 'Failed to login hospital.' }, { status: 500 });
  }
}
