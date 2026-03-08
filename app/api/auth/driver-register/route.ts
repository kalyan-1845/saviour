import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/mongodb';
import Driver from '@/models/Driver';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_REGEX = /^\d{10}$/;
const VEHICLE_REGEX = /^(AP|TS)[ -]?\d{2}[ -]?[A-Z]{1,2}[ -]?\d{4}$/i;

export async function POST(req: NextRequest) {
  try {
    await connectDB();

    const { fullName, email, phone, licenseNumber, vehicleNumber, password, confirmPassword } = await req.json();

    const normalizedEmail = String(email ?? '').trim().toLowerCase();
    const normalizedPhone = String(phone ?? '').replace(/[^\d]/g, '');
    const normalizedLicense = String(licenseNumber ?? '').trim().toUpperCase();
    const normalizedVehicle = String(vehicleNumber ?? '').trim().toUpperCase().replace(/\s+/g, ' ');
    const normalizedName = String(fullName ?? '').trim();

    // Validation
    if (!normalizedName || !normalizedEmail || !normalizedPhone || !normalizedLicense || !normalizedVehicle || !password) {
      return NextResponse.json(
        { error: 'Please provide all required fields' },
        { status: 400 }
      );
    }

    if (!EMAIL_REGEX.test(normalizedEmail)) {
      return NextResponse.json({ error: 'Please provide a valid email format.' }, { status: 400 });
    }

    if (!PHONE_REGEX.test(normalizedPhone)) {
      return NextResponse.json({ error: 'Phone must be exactly 10 digits.' }, { status: 400 });
    }

    if (!VEHICLE_REGEX.test(normalizedVehicle)) {
      return NextResponse.json(
        { error: 'Vehicle number must be in AP/TS format like TS 09 AB 1234.' },
        { status: 400 }
      );
    }

    if (password !== confirmPassword) {
      return NextResponse.json(
        { error: 'Passwords do not match' },
        { status: 400 }
      );
    }

    // Check if driver already exists
    const existingDriver = await Driver.findOne({
      $or: [
        { email: normalizedEmail },
        { phone: normalizedPhone },
        { licenseNumber: normalizedLicense },
        { vehicleNumber: normalizedVehicle },
      ],
    });

    if (existingDriver) {
      return NextResponse.json(
        { error: 'Driver with this email, license, or vehicle already exists' },
        { status: 400 }
      );
    }

    // Create new driver
    const driver = await Driver.create({
      fullName: normalizedName,
      email: normalizedEmail,
      phone: normalizedPhone,
      licenseNumber: normalizedLicense,
      vehicleNumber: normalizedVehicle,
      password,
    });

    // Remove password from response
    const driverResponse = driver.toObject();
    delete driverResponse.password;

    return NextResponse.json(
      {
        success: true,
        message: 'Driver registered successfully',
        driver: driverResponse,
      },
      { status: 201 }
    );
  } catch (error: any) {
    console.error('Driver registration error:', error);

    if (error.code === 11000) {
      const field = Object.keys(error.keyPattern)[0];
      return NextResponse.json(
        { error: `This ${field} is already registered` },
        { status: 400 }
      );
    }

    return NextResponse.json(
      { error: error.message || 'Failed to register driver' },
      { status: 500 }
    );
  }
}
