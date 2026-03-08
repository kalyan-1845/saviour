import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/mongodb';
import Driver from '@/models/Driver';
import { createOtp } from '@/lib/otp-store';

const DEMO_DRIVER_EMAIL = 'govindammajavvadi85@gmail.com';
const DEMO_DRIVER_PASSWORD = 'raj123';
const DEMO_DRIVER_PHONE = '9701700099';

function normalizePhone(phone: string): string {
  return phone.replace(/[^\d+]/g, '').trim();
}

export async function POST(req: NextRequest) {
  try {
    await connectDB();
    const body = await req.json();
    const phone = normalizePhone(body?.phone ?? '');

    if (!phone || phone.length < 10) {
      return NextResponse.json({ error: 'Valid phone number is required.' }, { status: 400 });
    }

    let driver = await Driver.findOne({ phone });
    if (!driver && phone === DEMO_DRIVER_PHONE) {
      driver = await Driver.create({
        fullName: 'Govindamma',
        email: DEMO_DRIVER_EMAIL,
        phone: DEMO_DRIVER_PHONE,
        licenseNumber: `TS-DEMO-OTP-${Date.now()}`,
        vehicleNumber: `TS-09-OTP-${Math.floor(Math.random() * 900 + 100)}`,
        password: DEMO_DRIVER_PASSWORD,
        isAvailable: true,
        currentLocation: {
          type: 'Point',
          coordinates: [78.5006, 17.4426],
        },
      });
    }

    if (!driver) {
      return NextResponse.json({ error: 'Driver not found for this mobile number.' }, { status: 404 });
    }

    const otp = createOtp(phone);
    console.log(`Driver OTP for ${phone}: ${otp}`);

    return NextResponse.json({
      success: true,
      message: 'OTP sent successfully.',
      otp, // Demo only: expose OTP to simplify local testing
    });
  } catch (error) {
    console.error('Driver OTP send error:', error);
    return NextResponse.json({ error: 'Failed to send OTP.' }, { status: 500 });
  }
}
