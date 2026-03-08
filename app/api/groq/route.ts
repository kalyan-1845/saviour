import { NextRequest, NextResponse } from 'next/server';

if (!process.env.GROQ_API_KEY) {
  console.error('GROQ_API_KEY is not set');
}

/**
 * POST /api/groq
 * AI Route Analysis using Groq API
 * Analyzes current location, destination, traffic, and provides optimal route
 */
export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { origin, destination, emergencyType } = body;

    if (!origin || !destination) {
      return NextResponse.json(
        { error: 'Origin and destination are required' },
        { status: 400 }
      );
    }

    // Simulate AI route analysis
    // In production, call Groq API for advanced analysis
    const mockAnalysis = `Optimal route from ${origin} to ${destination}. Emergency: ${emergencyType}. Distance: 8.5km, ETA: 12 minutes. Traffic: Moderate on Main Road. Alternative: Highway exit recommended.`;
    
    return NextResponse.json({
      success: true,
      analysis: mockAnalysis,
      estimatedTime: Math.ceil(Math.random() * 20) + 5,
      distance: (Math.random() * 15).toFixed(1),
      trafficLevel: 'moderate',
      routeAlternatives: 2,
    });
  } catch (error) {
    console.error('Route analysis error:', error);
    return NextResponse.json(
      { error: 'Failed to process route analysis' },
      { status: 500 }
    );
  }
}
