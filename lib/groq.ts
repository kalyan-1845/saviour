import Groq from 'groq-sdk';

export type AssistantIntent =
  | 'emergency_sos'
  | 'track_ambulance'
  | 'find_hospital'
  | 'find_police'
  | 'route_guidance'
  | 'general_help';

export type AssistantResponse = {
  reply: string;
  detectedLanguage: string;
  intent: AssistantIntent;
  confidence: number;
  suggestions: string[];
};

let cachedGroq: Groq | null = null;

function getGroqClient(): Groq {
  const apiKey = process.env.GROQ_API_KEY;
  if (!apiKey) {
    throw new Error('GROQ_API_KEY environment variable is not set');
  }
  if (!cachedGroq) {
    cachedGroq = new Groq({ apiKey });
  }
  return cachedGroq;
}

function safeJsonParse<T>(value: string): T | null {
  try {
    return JSON.parse(value) as T;
  } catch {
    return null;
  }
}

export async function analyzeRoute(routeData: unknown): Promise<string> {
  const groqClient = getGroqClient();

  const response = await groqClient.chat.completions.create({
    model: 'llama-3.3-70b-versatile',
    messages: [
      {
        role: 'system',
        content:
          'You are an emergency route analyst. Give a short, practical recommendation with ETA/risks/alternatives.',
      },
      {
        role: 'user',
        content: `Analyze this emergency route and provide the best path: ${JSON.stringify(routeData)}`,
      },
    ],
    temperature: 0.2,
    max_tokens: 700,
  });

  return response.choices[0]?.message?.content ?? 'No route analysis available.';
}

export async function runAssistantMessage(params: {
  message: string;
  preferredLanguage?: string;
  context?: Record<string, unknown>;
}): Promise<AssistantResponse> {
  const groqClient = getGroqClient();
  const { message, preferredLanguage = 'auto', context = {} } = params;

  const schemaHint = {
    reply: 'string',
    detectedLanguage: 'string (BCP-47 when possible)',
    intent:
      'one of emergency_sos|track_ambulance|find_hospital|find_police|route_guidance|general_help',
    confidence: 'number from 0 to 1',
    suggestions: 'string[]',
  };

  const response = await groqClient.chat.completions.create({
    model: 'llama-3.3-70b-versatile',
    temperature: 0.3,
    max_tokens: 900,
    messages: [
      {
        role: 'system',
        content:
          'You are SARATHI, a multilingual emergency assistant. Respond in the user language. Be concise, calm, and actionable. If emergency is likely, advise SOS immediately. Output strict JSON only.',
      },
      {
        role: 'user',
        content: JSON.stringify({
          task: 'Generate assistant response JSON',
          preferredLanguage,
          message,
          context,
          outputSchema: schemaHint,
        }),
      },
    ],
  });

  const raw = response.choices[0]?.message?.content?.trim() ?? '';
  const parsed = safeJsonParse<AssistantResponse>(raw);
  if (!parsed) {
    return {
      reply: raw || 'I am ready. Tell me what you need.',
      detectedLanguage: preferredLanguage === 'auto' ? 'en' : preferredLanguage,
      intent: 'general_help',
      confidence: 0.4,
      suggestions: ['Send SOS', 'Track ambulance', 'Find nearest hospital'],
    };
  }

  return {
    reply: parsed.reply || 'I am ready. Tell me what you need.',
    detectedLanguage:
      parsed.detectedLanguage || (preferredLanguage === 'auto' ? 'en' : preferredLanguage),
    intent: parsed.intent || 'general_help',
    confidence:
      typeof parsed.confidence === 'number' && Number.isFinite(parsed.confidence)
        ? Math.max(0, Math.min(1, parsed.confidence))
        : 0.5,
    suggestions: Array.isArray(parsed.suggestions) ? parsed.suggestions.slice(0, 5) : [],
  };
}
