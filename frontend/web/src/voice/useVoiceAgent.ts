/**
 * useVoiceAgent — zero third-party voice API pipeline.
 *
 * STT:  Browser Web Speech API (SpeechRecognition) — free, built-in Chrome/Edge/Android
 * LLM:  Backend WebSocket → OpenRouter (existing)
 * TTS:  Browser Web Speech API (SpeechSynthesis)   — free, fully on-device
 *
 * No Deepgram. No ElevenLabs. No audio upload to backend.
 * Backend only handles LLM routing via text_input WebSocket messages.
 *
 * Barge-in: tap orb while speaking → cancels TTS and starts listening again.
 */
import { useCallback, useEffect, useRef, useState } from 'react';
import { personaForRole, type UserRole, type VoicePersonaConfig } from './voicePersonas';

// ── Types ────────────────────────────────────────────────────────────────────

export type VoiceState =
  | 'idle'       // not connected
  | 'connecting' // WS handshake in progress
  | 'ready'      // connected, waiting for user to start
  | 'listening'  // SpeechRecognition active
  | 'thinking'   // LLM in progress
  | 'speaking'   // SpeechSynthesis playing
  | 'error';     // fatal error

export interface VoiceAgentMessage {
  role: 'user' | 'assistant';
  text: string;
  timestamp: number;
}

export interface UseVoiceAgentReturn {
  state: VoiceState;
  persona: VoicePersonaConfig | null;
  transcript: string;
  messages: VoiceAgentMessage[];
  error: string | null;
  startListening: () => void;
  stopListening: () => void;
  connect: () => void;
  disconnect: () => void;
  sendText: (text: string) => void;
  inputLevel: number;
}

// ── Browser SpeechRecognition type declarations ───────────────────────────────
// Not in lib.dom.d.ts — declared manually for Chrome/Edge/Android support.

interface SpeechRecognitionResultItem {
  readonly transcript: string;
  readonly confidence: number;
}
interface SpeechRecognitionResult {
  readonly isFinal: boolean;
  readonly length: number;
  item(index: number): SpeechRecognitionResultItem;
  [index: number]: SpeechRecognitionResultItem;
}
interface SpeechRecognitionResultList {
  readonly length: number;
  item(index: number): SpeechRecognitionResult;
  [index: number]: SpeechRecognitionResult;
}
interface SpeechRecognitionEvent extends Event {
  readonly resultIndex: number;
  readonly results: SpeechRecognitionResultList;
}
interface SpeechRecognitionErrorEvent extends Event {
  readonly error: string;
  readonly message: string;
}
interface ISpeechRecognition extends EventTarget {
  lang: string;
  continuous: boolean;
  interimResults: boolean;
  maxAlternatives: number;
  onstart: ((ev: Event) => void) | null;
  onresult: ((ev: SpeechRecognitionEvent) => void) | null;
  onerror: ((ev: SpeechRecognitionErrorEvent) => void) | null;
  onend: ((ev: Event) => void) | null;
  start(): void;
  stop(): void;
  abort(): void;
}
type SpeechRecognitionCtor = new () => ISpeechRecognition;

const SpeechRecognitionAPI: SpeechRecognitionCtor | undefined =
  (window as unknown as { SpeechRecognition?: SpeechRecognitionCtor; webkitSpeechRecognition?: SpeechRecognitionCtor })
    .SpeechRecognition ??
  (window as unknown as { webkitSpeechRecognition?: SpeechRecognitionCtor })
    .webkitSpeechRecognition;

// ── WebSocket URL ─────────────────────────────────────────────────────────────

const WS_GATEWAY_URL = (import.meta.env.VITE_API_BASE_URL || `${window.location.protocol}//${window.location.host}`)
  .replace(/^http/, 'ws') + '/api/v1/ai/voice/ws';

// ── Hook ──────────────────────────────────────────────────────────────────────

export function useVoiceAgent(role: UserRole, jwtToken: string): UseVoiceAgentReturn {
  const [state, setState]         = useState<VoiceState>('idle');
  const [transcript, setTranscript] = useState('');
  const [messages, setMessages]   = useState<VoiceAgentMessage[]>([]);
  const [error, setError]         = useState<string | null>(null);
  const [persona, setPersona]     = useState<VoicePersonaConfig | null>(null);
  const [inputLevel, setInputLevel] = useState(0);

  const wsRef          = useRef<WebSocket | null>(null);
  const messagesRef    = useRef<VoiceAgentMessage[]>([]);
  const recognitionRef = useRef<ISpeechRecognition | null>(null);
  const levelTimerRef  = useRef<ReturnType<typeof setInterval> | null>(null);
  const stateRef       = useRef<VoiceState>('idle');

  // Keep stateRef in sync — lets async callbacks read current state
  useEffect(() => { stateRef.current = state; }, [state]);
  useEffect(() => { messagesRef.current = messages; }, [messages]);

  // ── Connect ───────────────────────────────────────────────────────────────

  const connect = useCallback(() => {
    if (wsRef.current?.readyState === WebSocket.OPEN) return;
    setState('connecting');
    setError(null);

    const url = `${WS_GATEWAY_URL}?token=${encodeURIComponent(jwtToken)}`;
    const ws = new WebSocket(url);
    wsRef.current = ws;

    ws.onopen = () => { /* wait for server "connected" message */ };

    ws.onmessage = (evt) => {
      if (typeof evt.data === 'string') handleServerMessage(evt.data);
      // Binary frames (ElevenLabs MP3) ignored — we use SpeechSynthesis instead
    };

    ws.onerror = () => {
      setError('Connection failed. Check your network.');
      setState('error');
    };

    ws.onclose = () => {
      if (stateRef.current !== 'error') setState('idle');
    };
  }, [jwtToken]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleServerMessage = (raw: string) => {
    let msg: Record<string, string>;
    try { msg = JSON.parse(raw); } catch { return; }

    switch (msg.type) {
      case 'connected':
        setPersona(personaForRole(role));
        setState('ready');
        break;

      case 'transcript':
        // Echo back the transcript that was sent as text_input
        if (msg.text) {
          setTranscript(msg.text);
          setMessages(prev => [...prev, { role: 'user', text: msg.text, timestamp: Date.now() }]);
        }
        setState('thinking');
        break;

      case 'llm_response':
        if (msg.text) {
          setMessages(prev => [...prev, { role: 'assistant', text: msg.text, timestamp: Date.now() }]);
        }
        setState('speaking');
        break;

      case 'tts_done':
        // Backend finished — speak the response via browser SpeechSynthesis
        speakLastResponse();
        break;

      case 'pong':
        break;

      case 'error':
        setError(msg.message || 'Unknown error');
        setState('error');
        setTimeout(() => { if (stateRef.current === 'error') setState('ready'); }, 3000);
        break;
    }
  };

  // ── TTS — browser SpeechSynthesis ────────────────────────────────────────

  const speakLastResponse = useCallback(() => {
    const lastMsg = [...messagesRef.current].reverse().find(m => m.role === 'assistant');
    if (!lastMsg?.text || !window.speechSynthesis) {
      setState('ready');
      return;
    }
    window.speechSynthesis.cancel();
    const utt = new SpeechSynthesisUtterance(lastMsg.text);
    utt.lang  = 'en-IN';
    utt.rate  = 1.0;
    utt.pitch = 1.0;
    // Pick a good voice if available
    const voices = window.speechSynthesis.getVoices();
    const preferred = voices.find(v =>
      v.lang.startsWith('en') && (v.name.includes('Google') || v.name.includes('Natural') || v.name.includes('Samantha'))
    ) ?? voices.find(v => v.lang.startsWith('en'));
    if (preferred) utt.voice = preferred;
    utt.onend    = () => { if (stateRef.current === 'speaking') setState('ready'); };
    utt.onerror  = () => setState('ready');
    window.speechSynthesis.speak(utt);
  }, []);

  const stopSpeaking = () => {
    if (window.speechSynthesis?.speaking) window.speechSynthesis.cancel();
  };

  // ── STT — browser SpeechRecognition ──────────────────────────────────────

  const startListening = useCallback(() => {
    if (stateRef.current !== 'ready' && stateRef.current !== 'speaking') return;
    stopSpeaking(); // barge-in

    if (!SpeechRecognitionAPI) {
      setError('Speech recognition not supported. Use Chrome or Edge, or type below.');
      setState('error');
      setTimeout(() => setState('ready'), 3000);
      return;
    }

    const recognition = new SpeechRecognitionAPI();
    recognition.lang            = 'en-IN';
    recognition.continuous      = false;
    recognition.interimResults  = true;
    recognition.maxAlternatives = 1;
    recognitionRef.current = recognition;

    // Fake level animation while listening
    let level = 0;
    levelTimerRef.current = setInterval(() => {
      level = Math.random() * 0.6 + 0.2;
      setInputLevel(level);
    }, 120);

    recognition.onstart = () => setState('listening');

    recognition.onresult = (event) => {
      let interim = '';
      let final   = '';
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const t = event.results[i][0].transcript;
        if (event.results[i].isFinal) final += t;
        else interim += t;
      }
      // Show interim transcript in UI
      if (interim) setTranscript(interim);
      // On final result — send to backend LLM
      if (final.trim()) {
        setTranscript(final.trim());
        sendTextInternal(final.trim());
      }
    };

    recognition.onerror = (event) => {
      clearLevelTimer();
      setInputLevel(0);
      if (event.error === 'no-speech') {
        // Silently go back to ready — user just didn't speak
        setState('ready');
      } else if (event.error === 'not-allowed' || event.error === 'audio-capture') {
        setError('Microphone permission denied. Allow it in browser/OS settings.');
        setState('error');
      } else if (event.error === 'network') {
        setError('Speech recognition needs internet. Try typing instead.');
        setState('error');
        setTimeout(() => setState('ready'), 3000);
      } else {
        setState('ready');
      }
    };

    recognition.onend = () => {
      clearLevelTimer();
      setInputLevel(0);
      // If still in listening state (no result came), go back to ready
      if (stateRef.current === 'listening') setState('ready');
    };

    recognition.start();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const stopListening = useCallback(() => {
    recognitionRef.current?.stop();
    recognitionRef.current = null;
    clearLevelTimer();
    setInputLevel(0);
    if (stateRef.current === 'listening') setState('ready');
  }, []);

  const clearLevelTimer = () => {
    if (levelTimerRef.current) { clearInterval(levelTimerRef.current); levelTimerRef.current = null; }
  };

  // ── Send text to backend ──────────────────────────────────────────────────

  const sendTextInternal = (text: string) => {
    if (!text.trim() || wsRef.current?.readyState !== WebSocket.OPEN) return;
    setMessages(prev => [...prev, { role: 'user', text: text.trim(), timestamp: Date.now() }]);
    wsRef.current.send(JSON.stringify({ type: 'text_input', text: text.trim() }));
    setState('thinking');
  };

  const sendText = useCallback((text: string) => {
    sendTextInternal(text);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // ── Disconnect ────────────────────────────────────────────────────────────

  const disconnect = useCallback(() => {
    stopSpeaking();
    stopListening();
    wsRef.current?.close();
    wsRef.current = null;
    setState('idle');
  }, [stopListening]);

  useEffect(() => () => disconnect(), [disconnect]);

  return {
    state,
    persona,
    transcript,
    messages,
    error,
    startListening,
    stopListening,
    connect,
    disconnect,
    sendText,
    inputLevel,
  };
}
