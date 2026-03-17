import { useState } from 'react';

/**
 * useSpeech Hook
 * Voice Assistant - Text-to-Speech (TTS) functionality
 */
export function useSpeech() {
  const [isSupported] = useState(
    typeof window !== 'undefined' &&
      ('speechSynthesis' in window || 'webkitSpeechSynthesis' in window)
  );
  const [isSpeaking, setIsSpeaking] = useState(false);

  const speak = (
    text: string,
    options?: { rate?: number; pitch?: number; volume?: number; lang?: string }
  ) => {
    if (!isSupported) {
      console.warn('Speech synthesis not supported');
      return;
    }

    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.rate = options?.rate || 1;
    utterance.pitch = options?.pitch || 1;
    utterance.volume = options?.volume || 1;
    if (options?.lang) {
      utterance.lang = options.lang;
    }

    utterance.onstart = () => setIsSpeaking(true);
    utterance.onend = () => setIsSpeaking(false);

    window.speechSynthesis.speak(utterance);
  };

  const stop = () => {
    if (isSupported) {
      window.speechSynthesis.cancel();
      setIsSpeaking(false);
    }
  };

  return { speak, stop, isSpeaking, isSupported };
}
