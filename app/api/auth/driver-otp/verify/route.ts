import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/mongodb';
import Driver from '@/models/Driver';
import { verifyOtp } from '@/lib/otp-store';

function normalizePhone(phone: string): string {
  return phone.replace(/[^\d+]/g, '').trim();
}

export async function POST(req: NextRequest) {
  try {
    await connectDB();
    const body = await req.json();
    const phone = normalizePhone(body?.phone ?? '');
    const otp = String(body?.otp ?? '').trim();

    if (!phone || phone.length < 10 || otp.length !== 6) {
      return NextResponse.json({ error: 'Phone and valid 6-digit OTP are required.' }, { status: 400 });
    }

    const otpResult = verifyOtp(phone, otp);
    if (!otpResult.ok) {
      return NextResponse.json({ error: otpResult.error }, { status: 401 });
    }

    const driver = await Driver.findOne({ phone }).lean();
    if (!driver) {
      return NextResponse.json({ error: 'Driver not found.' }, { status: 404 });
    }

    return NextResponse.json({
      success: true,
      message: 'OTP verified. Login successful.',
      driver: {
        _id: driver._id,
        fullName: driver.fullName,
        email: driver.email,
        phone: driver.phone,
        vehicleNumber: driver.vehicleNumber,
        licenseNumber: driver.licenseNumber,
      },
    });
  } catch (error) {
    console.error('Driver OTP verify error:', error);
    return NextResponse.json({ error: 'Failed to verify OTP.' }, { status: 500 });
  }
}
