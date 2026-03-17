'use client';

import { useMemo, useRef, useState } from 'react';
import { Globe2, Mic, Send, Square, Volume2, VolumeX } from 'lucide-react';
import { useI18n } from './LanguageProvider';

type AssistantApiResponse = {
  success: boolean;
  reply: string;
  detectedLanguage: string;
  intent: string;
  confidence: number;
  suggestions: string[];
  source?: 'groq' | 'fallback';
  warning?: string;
};

type SpeechRecognitionResultEvent = Event & {
  results: {
    [key: number]: {
      [key: number]: { transcript: string };
      isFinal: boolean;
    };
    length: number;
  };
  resultIndex: number;
};

interface SpeechRecognitionInstance extends EventTarget {
  continuous: boolean;
  interimResults: boolean;
  lang: string;
  onstart: ((this: SpeechRecognitionInstance, ev: Event) => unknown) | null;
  onresult: ((this: SpeechRecognitionInstance, ev: SpeechRecognitionResultEvent) => unknown) | null;
  onerror: ((this: SpeechRecognitionInstance, ev: Event) => unknown) | null;
  onend: ((this: SpeechRecognitionInstance, ev: Event) => unknown) | null;
  start: () => void;
  stop: () => void;
}

type SpeechRecognitionConstructor = new () => SpeechRecognitionInstance;

const languagePresets: Array<{ label: string; code: string }> = [
  { label: 'Auto', code: 'auto' },
  { label: 'English', code: 'en-IN' },
  { label: 'Hindi', code: 'hi-IN' },
  { label: 'Telugu', code: 'te-IN' },
  { label: 'Tamil', code: 'ta-IN' },
  { label: 'Marathi', code: 'mr-IN' },
  { label: 'Bengali', code: 'bn-IN' },
  { label: 'Arabic', code: 'ar' },
  { label: 'Spanish', code: 'es' },
  { label: 'French', code: 'fr' },
  { label: 'German', code: 'de' },
  { label: 'Japanese', code: 'ja' },
  { label: 'Chinese', code: 'zh-CN' },
];

function resolveBrowserLanguage(appLanguage: string): string {
  if (appLanguage === 'te') return 'te-IN';
  if (appLanguage === 'hi') return 'hi-IN';
  if (appLanguage === 'mr') return 'mr-IN';
  return 'en-IN';
}

export function VoiceAssistant() {
  const { language: appLanguage } = useI18n();
  const [isListening, setIsListening] = useState(false);
  const [transcript, setTranscript] = useState('');
  const [assistantReply, setAssistantReply] = useState('');
  const [isThinking, setIsThinking] = useState(false);
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [speechEnabled, setSpeechEnabled] = useState(true);
  const [selectedLang, setSelectedLang] = useState<string>('auto');
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const recognitionRef = useRef<SpeechRecognitionInstance | null>(null);

  const recognitionCtor = useMemo<SpeechRecognitionConstructor | null>(() => {
    if (typeof window === 'undefined') return null;
    const win = window as Window & {
      SpeechRecognition?: SpeechRecognitionConstructor;
      webkitSpeechRecognition?: SpeechRecognitionConstructor;
    };
    return win.SpeechRecognition ?? win.webkitSpeechRecognition ?? null;
  }, []);

  const activeLanguage = selectedLang === 'auto' ? resolveBrowserLanguage(appLanguage) : selectedLang;

  const askAssistant = async (message: string) => {
    setIsThinking(true);
    try {
      const response = await fetch('/api/assistant', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          message,
          language: selectedLang === 'auto' ? 'auto' : selectedLang,
          role: 'public',
        }),
      });

      const data = (await response.json()) as AssistantApiResponse;
      if (!response.ok || !data.success) {
        throw new Error(data.reply || 'Assistant request failed');
      }

      setAssistantReply(data.reply);
      setSuggestions(data.suggestions ?? []);
      if (speechEnabled) {
        speak(data.reply, data.detectedLanguage || activeLanguage);
      }
    } catch (error) {
      const fallback =
        error instanceof Error
          ? `Assistant temporarily unavailable: ${error.message}`
          : 'Assistant temporarily unavailable.';
      setAssistantReply(fallback);
    } finally {
      setIsThinking(false);
    }
  };

  const speak = (text: string, langCode: string) => {
    if (typeof window === 'undefined' || !('speechSynthesis' in window)) return;

    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = langCode || activeLanguage;
    utterance.rate = 0.95;
    utterance.pitch = 1;
    utterance.onstart = () => setIsSpeaking(true);
    utterance.onend = () => setIsSpeaking(false);
    window.speechSynthesis.speak(utterance);
  };

  const startListening = () => {
    if (!recognitionCtor) {
      setAssistantReply('Speech recognition is not supported in this browser.');
      return;
    }

    const recognition = new recognitionCtor();
    recognition.continuous = false;
    recognition.interimResults = true;
    recognition.lang = activeLanguage;

    recognition.onstart = () => {
      setIsListening(true);
      setTranscript('Listening...');
    };

    recognition.onresult = (event) => {
      let interim = '';
      for (let i = event.resultIndex; i < event.results.length; i += 1) {
        const phrase = event.results[i]?.[0]?.transcript ?? '';
        if (event.results[i]?.isFinal) {
          setTranscript(phrase);
          void askAssistant(phrase);
        } else {
          interim += phrase;
        }
      }
      if (interim) setTranscript(interim);
    };

    recognition.onerror = () => {
      setIsListening(false);
    };

    recognition.onend = () => {
      setIsListening(false);
    };

    recognitionRef.current = recognition;
    recognition.start();
  };

  const stopListening = () => {
    recognitionRef.current?.stop();
    recognitionRef.current = null;
    setIsListening(false);
  };

  return (
    <div className="fixed bottom-6 right-6 z-50 w-[340px] max-w-[calc(100vw-24px)] space-y-3">
      <div className="rounded-2xl border border-blue-500/30 bg-slate-900/90 p-3 text-white shadow-2xl backdrop-blur">
        <div className="mb-2 flex items-center justify-between gap-2">
          <div className="flex items-center gap-2 text-sm font-semibold">
            <Globe2 className="h-4 w-4 text-blue-300" />
            SARATHI AI Assistant
          </div>
          <button
            type="button"
            onClick={() => setSpeechEnabled((prev) => !prev)}
            className="rounded-md border border-white/20 p-1 hover:bg-white/10"
            aria-label="Toggle voice output"
          >
            {speechEnabled ? <Volume2 className="h-4 w-4" /> : <VolumeX className="h-4 w-4" />}
          </button>
        </div>

        <div className="mb-2 flex items-center gap-2">
          <select
            value={selectedLang}
            onChange={(event) => setSelectedLang(event.target.value)}
            className="w-full rounded-md border border-white/20 bg-slate-800 px-2 py-1 text-xs text-white outline-none"
          >
            {languagePresets.map((lang) => (
              <option key={lang.code} value={lang.code}>
                {lang.label}
              </option>
            ))}
          </select>
        </div>

        <div className="min-h-[48px] rounded-lg border border-white/10 bg-slate-800/60 p-2 text-sm">
          {isThinking ? 'Thinking...' : assistantReply || transcript || 'Tap mic and speak in any language.'}
        </div>

        {suggestions.length > 0 && (
          <div className="mt-2 flex flex-wrap gap-1">
            {suggestions.map((item) => (
              <button
                key={item}
                type="button"
                onClick={() => void askAssistant(item)}
                className="rounded-full border border-blue-300/40 px-2 py-1 text-[11px] text-blue-200 hover:bg-blue-500/15"
              >
                {item}
              </button>
            ))}
          </div>
        )}

        <div className="mt-3 flex items-center gap-2">
          <button
            type="button"
            onClick={() => {
              if (isListening) stopListening();
              else startListening();
            }}
            className={`flex h-11 w-11 items-center justify-center rounded-full ${
              isListening ? 'bg-red-600' : 'bg-blue-600'
            }`}
            aria-label="Voice input"
          >
            {isListening ? <Square className="h-5 w-5 text-white" /> : <Mic className="h-5 w-5 text-white" />}
          </button>
          <button
            type="button"
            onClick={() => {
              if (transcript.trim()) void askAssistant(transcript);
            }}
            className="flex h-11 flex-1 items-center justify-center gap-2 rounded-xl border border-white/20 bg-slate-800/80 text-sm hover:bg-slate-700/80"
          >
            <Send className="h-4 w-4" />
            Send
          </button>
        </div>
      </div>

      {isSpeaking && (
        <div className="rounded-lg border border-emerald-400/30 bg-emerald-500/10 px-3 py-2 text-xs text-emerald-200">
          Speaking...
        </div>
      )}
    </div>
  );
}
