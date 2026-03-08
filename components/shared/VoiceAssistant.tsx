'use client';

import { useState, useEffect } from 'react';
import { Volume2, Square, Mic } from 'lucide-react';

export function VoiceAssistant() {
  const [isListening, setIsListening] = useState(false);
  const [transcript, setTranscript] = useState('');
  const [isSpeaking, setIsSpeaking] = useState(false);

  // Check for Web Speech API support
  const SpeechRecognition = typeof window !== 'undefined' 
    ? (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition 
    : null;

  const handleVoiceInput = () => {
    if (!SpeechRecognition) {
      alert('Speech Recognition not supported in your browser');
      return;
    }

    const recognition = new SpeechRecognition();
    recognition.continuous = false;
    recognition.interimResults = true;
    recognition.lang = 'en-IN';

    recognition.onstart = () => {
      setIsListening(true);
      setTranscript('Listening...');
    };

    recognition.onresult = (event: any) => {
      let interim = '';
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const transcript = event.results[i][0].transcript;
        if (event.results[i].isFinal) {
          setTranscript(transcript);
          processVoiceCommand(transcript);
        } else {
          interim += transcript;
        }
      }
      if (interim) setTranscript(interim);
    };

    recognition.onend = () => {
      setIsListening(false);
    };

    recognition.start();
  };

  const processVoiceCommand = (command: string) => {
    const lowerCommand = command.toLowerCase();

    if (lowerCommand.includes('hospital') || lowerCommand.includes('destination')) {
      speakResponse('Navigating to hospital');
    } else if (lowerCommand.includes('emergency')) {
      speakResponse('Emergency mode activated');
    } else {
      speakResponse('Command not recognized');
    }
  };

  const speakResponse = (text: string) => {
    if ('speechSynthesis' in window) {
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = 'en-IN';
      utterance.rate = 0.9;
      utterance.onstart = () => setIsSpeaking(true);
      utterance.onend = () => setIsSpeaking(false);
      window.speechSynthesis.speak(utterance);
    }
  };

  return (
    <div className="fixed bottom-8 right-8 z-50 space-y-4">
      {/* Transcript */}
      {transcript && (
        <div className="bg-white/10 backdrop-blur-md border-2 border-blue-500 rounded-lg p-4 max-w-xs text-white text-sm">
          {transcript}
        </div>
      )}

      {/* Voice Button */}
      <button
        onClick={handleVoiceInput}
        disabled={isListening}
        className={`w-16 h-16 rounded-full flex items-center justify-center transition-all transform ${
          isListening
            ? 'bg-red-600 scale-110 animate-pulse'
            : isSpeaking
            ? 'bg-blue-600'
            : 'bg-purple-600 hover:scale-110'
        } shadow-lg shadow-purple-600/50`}
      >
        {isListening ? (
          <Square size={24} className="text-white" />
        ) : isSpeaking ? (
          <Volume2 size={24} className="text-white" />
        ) : (
          <Mic size={24} className="text-white" />
        )}
      </button>
    </div>
  );
}
