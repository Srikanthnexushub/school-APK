/**
 * useVoiceAgent — enterprise WebSocket voice agent hook.
 *
 * Pipeline:
 *   MediaRecorder (webm/opus) → WebSocket (binary frames)
 *   → Backend STT (Deepgram) → LLM → TTS (ElevenLabs)
 *   → WebSocket (binary MP3) → AudioContext playback
 *
 * VAD (Voice Activity Detection) is done via AnalyserNode RMS:
 *   if energy < VAD_THRESHOLD for VAD_SILENCE_MS → auto-send audio_end
 *
 * Barge-in: calling stopListening() mid-TTS cancels playback immediately.
 */
import { useCallback, useEffect, useRef, useState } from 'react';
import { personaForRole, type UserRole, type VoicePersonaConfig } from './voicePersonas';

// ── Types ────────────────────────────────────────────────────────────────────

export type VoiceState =
  | 'idle'       // not connected
  | 'connecting' // WS handshake in progress
  | 'ready'      // connected, waiting for user to start
  | 'listening'  // microphone active, capturing audio
  | 'thinking'   // STT+LLM in progress
  | 'speaking'   // TTS audio playing
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
  /** Volume level 0–1 for waveform visualizer */
  inputLevel: number;
}

// ── Constants ────────────────────────────────────────────────────────────────

const VAD_THRESHOLD   = 0.012;  // RMS below this = silence
const VAD_SILENCE_MS  = 1400;   // ms of silence before auto-sending
const WS_GATEWAY_URL  = (import.meta.env.VITE_API_BASE_URL || 'http://13.126.138.9')
  .replace(/^http/, 'ws') + '/api/v1/ai/voice/ws';

// ── Hook ──────────────────────────────────────────────────────────────────────

export function useVoiceAgent(role: UserRole, jwtToken: string): UseVoiceAgentReturn {
  const [state, setState] = useState<VoiceState>('idle');
  const [transcript, setTranscript] = useState('');
  const [messages, setMessages] = useState<VoiceAgentMessage[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [persona, setPersona] = useState<VoicePersonaConfig | null>(null);
  const [inputLevel, setInputLevel] = useState(0);

  const wsRef        = useRef<WebSocket | null>(null);
  const recorderRef  = useRef<MediaRecorder | null>(null);
  const audioCtxRef  = useRef<AudioContext | null>(null);
  const analyserRef  = useRef<AnalyserNode | null>(null);
  const streamRef    = useRef<MediaStream | null>(null);
  const vadTimerRef  = useRef<ReturnType<typeof setTimeout> | null>(null);
  const rafRef       = useRef<number | null>(null);
  const mp3BufRef    = useRef<Uint8Array[]>([]);
  const sourceNodeRef = useRef<AudioBufferSourceNode | null>(null);

  // ── Connect ───────────────────────────────────────────────────────────────

  const connect = useCallback(() => {
    if (wsRef.current?.readyState === WebSocket.OPEN) return;
    setState('connecting');
    setError(null);

    const url = `${WS_GATEWAY_URL}?token=${encodeURIComponent(jwtToken)}`;
    const ws = new WebSocket(url);
    ws.binaryType = 'arraybuffer';
    wsRef.current = ws;

    ws.onopen = () => {
      // wait for "connected" message from server
    };

    ws.onmessage = async (evt) => {
      if (typeof evt.data === 'string') {
        handleTextMessage(evt.data);
      } else {
        // Binary: MP3 chunk from ElevenLabs TTS
        mp3BufRef.current.push(new Uint8Array(evt.data as ArrayBuffer));
      }
    };

    ws.onerror = () => {
      setError('Connection failed. Check your network.');
      setState('error');
    };

    ws.onclose = () => {
      if (state !== 'error') setState('idle');
    };
  }, [jwtToken]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleTextMessage = (raw: string) => {
    let msg: Record<string, string>;
    try { msg = JSON.parse(raw); } catch { return; }

    switch (msg.type) {
      case 'connected':
        setPersona(personaForRole(role));
        setState('ready');
        break;

      case 'transcript':
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
        mp3BufRef.current = []; // reset buffer for this response
        break;

      case 'tts_done':
        playAccumulatedMp3();
        break;

      case 'pong':
        break;

      case 'error':
        setError(msg.message || 'Unknown error');
        setState('error');
        setTimeout(() => setState('ready'), 3000);
        break;
    }
  };

  // ── Audio playback ────────────────────────────────────────────────────────

  const playAccumulatedMp3 = async () => {
    const chunks = mp3BufRef.current;
    mp3BufRef.current = [];
    if (chunks.length === 0) { setState('ready'); return; }

    const totalLen = chunks.reduce((acc, c) => acc + c.length, 0);
    const merged = new Uint8Array(totalLen);
    let offset = 0;
    for (const c of chunks) { merged.set(c, offset); offset += c.length; }

    try {
      const ctx = getAudioContext();
      const decoded = await ctx.decodeAudioData(merged.buffer);
      const source = ctx.createBufferSource();
      source.buffer = decoded;
      source.connect(ctx.destination);
      sourceNodeRef.current = source;
      source.start(0);
      source.onended = () => {
        sourceNodeRef.current = null;
        setState('ready');
      };
    } catch {
      setState('ready');
    }
  };

  const stopTtsPlayback = () => {
    try { sourceNodeRef.current?.stop(); } catch { /* ignore */ }
    sourceNodeRef.current = null;
    mp3BufRef.current = [];
  };

  // ── Microphone ────────────────────────────────────────────────────────────

  const startListening = useCallback(async () => {
    if (state !== 'ready' && state !== 'listening') return;
    stopTtsPlayback(); // barge-in: stop TTS if speaking

    // getUserMedia requires a secure context (HTTPS or localhost)
    if (!window.isSecureContext || !navigator.mediaDevices?.getUserMedia) {
      setError('Microphone requires HTTPS. Please open the app at https://');
      setState('error');
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: { echoCancellation: true, noiseSuppression: true, sampleRate: 16000 },
      });
      streamRef.current = stream;

      // Analyser for VAD + waveform level
      const ctx = getAudioContext();
      const source = ctx.createMediaStreamSource(stream);
      const analyser = ctx.createAnalyser();
      analyser.fftSize = 512;
      source.connect(analyser);
      analyserRef.current = analyser;

      // MediaRecorder — webm/opus (Deepgram nova-2 accepts this natively)
      const mimeType = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
        ? 'audio/webm;codecs=opus'
        : 'audio/webm';
      const recorder = new MediaRecorder(stream, { mimeType });
      recorderRef.current = recorder;

      recorder.ondataavailable = (e) => {
        if (e.data.size > 0 && wsRef.current?.readyState === WebSocket.OPEN) {
          wsRef.current.send(e.data);
        }
      };
      recorder.start(200); // chunk every 200ms

      setState('listening');
      startVad();
      startLevelMonitor();
    } catch (e: unknown) {
      if (e instanceof DOMException) {
        if (e.name === 'NotAllowedError' || e.name === 'PermissionDeniedError') {
          setError('Microphone permission denied. Allow it in browser/OS settings.');
        } else if (e.name === 'NotFoundError') {
          setError('No microphone found on this device.');
        } else {
          setError(`Microphone error: ${e.name}`);
        }
      } else {
        setError('Microphone access failed. Check browser/OS permissions.');
      }
      setState('error');
    }
  }, [state]);

  const stopListening = useCallback(() => {
    clearVad();
    cancelLevelMonitor();
    setInputLevel(0);

    const recorder = recorderRef.current;
    if (recorder && recorder.state !== 'inactive') {
      recorder.stop();
    }
    recorderRef.current = null;

    streamRef.current?.getTracks().forEach(t => t.stop());
    streamRef.current = null;

    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({ type: 'audio_end' }));
    }
    setState('thinking');
  }, []);

  // ── VAD ───────────────────────────────────────────────────────────────────

  const startVad = () => {
    const check = () => {
      const analyser = analyserRef.current;
      if (!analyser) return;
      const buf = new Float32Array(analyser.fftSize);
      analyser.getFloatTimeDomainData(buf);
      const rms = Math.sqrt(buf.reduce((s, v) => s + v * v, 0) / buf.length);
      if (rms < VAD_THRESHOLD) {
        if (!vadTimerRef.current) {
          vadTimerRef.current = setTimeout(() => {
            if (recorderRef.current?.state === 'recording') stopListening();
          }, VAD_SILENCE_MS);
        }
      } else {
        clearVad();
      }
      rafRef.current = requestAnimationFrame(check);
    };
    rafRef.current = requestAnimationFrame(check);
  };

  const clearVad = () => {
    if (vadTimerRef.current) { clearTimeout(vadTimerRef.current); vadTimerRef.current = null; }
  };

  // ── Input level monitor (for waveform UI) ─────────────────────────────────

  const startLevelMonitor = () => {
    const measure = () => {
      const analyser = analyserRef.current;
      if (!analyser) return;
      const buf = new Float32Array(analyser.fftSize);
      analyser.getFloatTimeDomainData(buf);
      const rms = Math.sqrt(buf.reduce((s, v) => s + v * v, 0) / buf.length);
      setInputLevel(Math.min(1, rms * 10));
      rafRef.current = requestAnimationFrame(measure);
    };
    rafRef.current = requestAnimationFrame(measure);
  };

  const cancelLevelMonitor = () => {
    if (rafRef.current) { cancelAnimationFrame(rafRef.current); rafRef.current = null; }
  };

  // ── Disconnect ────────────────────────────────────────────────────────────

  const disconnect = useCallback(() => {
    stopTtsPlayback();
    clearVad();
    cancelLevelMonitor();
    recorderRef.current?.stop();
    streamRef.current?.getTracks().forEach(t => t.stop());
    wsRef.current?.close();
    wsRef.current = null;
    setState('idle');
  }, []);

  useEffect(() => () => disconnect(), [disconnect]);

  // ── Shared AudioContext ───────────────────────────────────────────────────

  const getAudioContext = (): AudioContext => {
    if (!audioCtxRef.current || audioCtxRef.current.state === 'closed') {
      audioCtxRef.current = new AudioContext();
    }
    if (audioCtxRef.current.state === 'suspended') {
      audioCtxRef.current.resume();
    }
    return audioCtxRef.current;
  };

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
    inputLevel,
  };
}
