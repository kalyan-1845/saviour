/**
 * Groq SDK Configuration
 * AI-powered route analysis and response optimization
 */

import Groq from 'groq-sdk';

if (!process.env.GROQ_API_KEY) {
  throw new Error('GROQ_API_KEY environment variable is not set');
}

export const groqClient = new Groq({
  apiKey: process.env.GROQ_API_KEY,
});

export async function analyzeRoute(routeData: unknown) {
  try {
    const response = await groqClient.chat.completions.create({
      model: 'mixtral-8x7b-32768',
      messages: [
        {
          role: 'user',
          content: `Analyze this emergency route and provide the best path: ${JSON.stringify(routeData)}`,
        },
      ],
      max_tokens: 1024,
    });

    return response.choices[0].message.content;
  } catch (error) {
    console.error('Groq API error:', error);
    throw error;
  }
}
