import { useEffect, useState } from 'react';

/**
 * useSpeech Hook
 * Voice Assistant - Text-to-Speech (TTS) functionality
 */
export function useSpeech() {
  const [isSupported, setIsSupported] = useState(false);
  const [isSpeaking, setIsSpeaking] = useState(false);

  useEffect(() => {
    const supported =
      typeof window !== 'undefined' &&
      ('speechSynthesis' in window ||
        'webkitSpeechSynthesis' in window);
    setIsSupported(supported);
  }, []);

  const speak = (text: string, options?: { rate?: number; pitch?: number; volume?: number }) => {
    if (!isSupported) {
      console.warn('Speech synthesis not supported');
      return;
    }

    const utterance = new SpeechSynthesisUtterance(text);
    utterance.rate = options?.rate || 1;
    utterance.pitch = options?.pitch || 1;
    utterance.volume = options?.volume || 1;

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
