import { NextRequest, NextResponse } from 'next/server';
import { runAssistantMessage } from '@/lib/groq';

type AssistantRequestBody = {
  message?: string;
  language?: string;
  role?: 'public' | 'driver' | 'hospital' | 'police';
  context?: Record<string, unknown>;
};

function getFallbackReply(message: string): {
  reply: string;
  intent: string;
  suggestions: string[];
} {
  const normalized = message.toLowerCase();
  if (normalized.includes('sos') || normalized.includes('emergency') || normalized.includes('help')) {
    return {
      reply:
        'Emergency detected. Use SOS now. Share location, keep phone reachable, and follow first-aid steps until response arrives.',
      intent: 'emergency_sos',
      suggestions: ['Send SOS', 'Share live location', 'Call 112'],
    };
  }
  if (normalized.includes('hospital')) {
    return {
      reply: 'I can help locate and contact the nearest emergency hospital.',
      intent: 'find_hospital',
      suggestions: ['Show nearby hospitals', 'Call hospital desk', 'Route to ER'],
    };
  }
  if (normalized.includes('police')) {
    return {
      reply: 'I can alert police and start green-corridor coordination.',
      intent: 'find_police',
      suggestions: ['Notify police', 'Track route alerts', 'Share vehicle position'],
    };
  }
  return {
    reply: 'I am ready to help. Tell me your emergency or destination.',
    intent: 'general_help',
    suggestions: ['Send SOS', 'Track ambulance', 'Find hospital'],
  };
}

export async function POST(request: NextRequest) {
  try {
    const body = (await request.json()) as AssistantRequestBody;
    const message = String(body.message ?? '').trim();
    const language = String(body.language ?? 'auto').trim() || 'auto';
    const role = body.role ?? 'public';

    if (!message) {
      return NextResponse.json({ error: 'message is required' }, { status: 400 });
    }

    try {
      const ai = await runAssistantMessage({
        message,
        preferredLanguage: language,
        context: {
          role,
          ...(body.context ?? {}),
        },
      });

      return NextResponse.json({
        success: true,
        source: 'groq',
        ...ai,
      });
    } catch (error) {
      const fallback = getFallbackReply(message);
      return NextResponse.json({
        success: true,
        source: 'fallback',
        reply: fallback.reply,
        detectedLanguage: language === 'auto' ? 'en' : language,
        intent: fallback.intent,
        confidence: 0.35,
        suggestions: fallback.suggestions,
        warning:
          error instanceof Error ? error.message : 'AI service unavailable, using fallback.',
      });
    }
  } catch (error) {
    return NextResponse.json(
      {
        error: error instanceof Error ? error.message : 'Failed to process assistant request',
      },
      { status: 500 }
    );
  }
}
