import { NextRequest, NextResponse } from 'next/server';
import { analyzeRoute } from '@/lib/groq';

/**
 * POST /api/groq
 * AI Route Analysis using Groq API
 * Analyzes current location, destination, traffic, and provides optimal route
 */
export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { origin, destination, emergencyType, trafficData } = body;

    if (!origin || !destination) {
      return NextResponse.json(
        { error: 'Origin and destination are required' },
        { status: 400 }
      );
    }

    const now = new Date().toISOString();
    const analysis = await analyzeRoute({
      origin,
      destination,
      emergencyType,
      trafficData,
      timestamp: now,
    });

    const trafficLevel =
      typeof trafficData?.level === 'string'
        ? trafficData.level
        : typeof trafficData?.congestion === 'string'
          ? trafficData.congestion
          : 'moderate';

    return NextResponse.json({
      success: true,
      analysis,
      estimatedTime: null,
      distance: null,
      trafficLevel,
      routeAlternatives: 1,
      generatedAt: now,
    });
  } catch (error: unknown) {
    console.error('Route analysis error:', error);
    return NextResponse.json(
      {
        error:
          error instanceof Error ? error.message : 'Failed to process route analysis',
      },
      { status: 500 }
    );
  }
}
