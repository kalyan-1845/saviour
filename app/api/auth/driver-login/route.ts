import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/mongodb';
import Driver from '@/models/Driver';
import { signJwt } from '@/lib/jwt';

const DEMO_DRIVER_EMAIL = 'prsnlkalyan@gmail.com';
const DEMO_DRIVER_PASSWORD = 'kalyan1234';

export async function POST(req: NextRequest) {
  try {
    await connectDB();

    const { email, password } = await req.json();

    // Validation
    if (!email || !password) {
      return NextResponse.json(
        { error: 'Please provide email and password' },
        { status: 400 }
      );
    }

    let driver = await Driver.findOne({ email }).select('+password');

    if (!driver && email.toLowerCase() === DEMO_DRIVER_EMAIL && password === DEMO_DRIVER_PASSWORD) {
      driver = await Driver.create({
        fullName: 'Kalyan',
        email: DEMO_DRIVER_EMAIL,
        phone: '9999999999',
        licenseNumber: `TS-DEMO-${Date.now()}`,
        vehicleNumber: `TS-09-DEMO-${Math.floor(Math.random() * 900 + 100)}`,
        password: DEMO_DRIVER_PASSWORD,
        isAvailable: true,
        currentLocation: {
          type: 'Point',
          coordinates: [78.5006, 17.4426],
        },
      });
      driver = await Driver.findById(driver._id).select('+password');
    }

    if (!driver) {
      return NextResponse.json(
        { error: 'Invalid credentials' },
        { status: 401 }
      );
    }

    // Check password
    const isPasswordMatch = await driver.matchPassword(password);

    if (!isPasswordMatch) {
      return NextResponse.json(
        { error: 'Invalid credentials' },
        { status: 401 }
      );
    }

    // Remove password from response
    const driverResponse = driver.toObject();
    delete driverResponse.password;
    const token = signJwt({
      sub: String(driverResponse._id),
      role: 'driver',
      email: driverResponse.email,
    });

    return NextResponse.json(
      {
        success: true,
        message: 'Login successful',
        driver: driverResponse,
        token,
      },
      { status: 200 }
    );
  } catch (error: unknown) {
    console.error('Driver login error:', error);
    const message = error instanceof Error ? error.message : 'Failed to login';
    return NextResponse.json(
      { error: message },
      { status: 500 }
    );
  }
}
