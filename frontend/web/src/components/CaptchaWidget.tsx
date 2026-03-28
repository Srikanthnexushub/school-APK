import { useState, useEffect, useCallback } from 'react';
import { RefreshCw, ShieldCheck, AlertTriangle } from 'lucide-react';
import api from '../lib/api';
import { cn } from '../lib/utils';

interface Props {
  onVerify: (token: string | null) => void;
}

// Container background matches exactly what the server renders (rgb(22,22,35) canvas fill).
// Using inline style so it is never overridden by any global light/dark CSS class.
const IMG_BOX_STYLE: React.CSSProperties = {
  width: 220,
  height: 80,
  background: 'rgb(22, 22, 35)',
  flexShrink: 0,
};

export default function CaptchaWidget({ onVerify }: Props) {
  const [challengeId, setChallengeId] = useState('');
  const [imageDataUri, setImageDataUri] = useState('');
  const [answer, setAnswer] = useState('');
  // Start as loading=true so the spinner is visible immediately on mount
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [verified, setVerified] = useState(false);

  const fetchChallenge = useCallback(async () => {
    setLoading(true);
    setAnswer('');
    setVerified(false);
    setError(false);
    onVerify(null);
    try {
      const res = await api.get(`/api/v1/captcha/challenge?t=${Date.now()}`);
      setChallengeId(res.data.id);
      setImageDataUri(res.data.imageDataUri);
    } catch {
      setError(true);
      setImageDataUri('');
    } finally {
      setLoading(false);
    }
  }, [onVerify]);

  useEffect(() => {
    fetchChallenge();
  }, [fetchChallenge]);

  function handleChange(raw: string) {
    const value = raw.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 6);
    setAnswer(value);
    if (value.length === 6) {
      setVerified(true);
      onVerify(`${challengeId}:${value}`);
    } else {
      setVerified(false);
      onVerify(null);
    }
  }

  return (
    <div className="space-y-3">
      <label className="block text-sm font-medium text-white/70">
        Security check — type the characters you see
      </label>

      {/* Image + refresh */}
      <div className="flex items-center gap-3">
        {/* Fixed container — inline style prevents any CSS class override */}
        <div
          className="relative rounded-xl overflow-hidden border border-white/10"
          style={IMG_BOX_STYLE}
        >
          {loading && (
            <div className="absolute inset-0 flex items-center justify-center">
              <div className="w-6 h-6 border-2 border-white/15 border-t-brand-400 rounded-full animate-spin" />
            </div>
          )}

          {!loading && error && (
            <div className="absolute inset-0 flex flex-col items-center justify-center gap-1.5">
              <AlertTriangle className="w-5 h-5 text-amber-400" />
              <span className="text-[11px] text-white/50">Failed to load</span>
            </div>
          )}

          {!loading && !error && imageDataUri && (
            <img
              src={imageDataUri}
              alt="CAPTCHA challenge"
              className="absolute inset-0 w-full h-full object-fill select-none pointer-events-none"
              draggable={false}
            />
          )}
        </div>

        <button
          type="button"
          onClick={fetchChallenge}
          disabled={loading}
          title="New challenge"
          className="p-2.5 rounded-xl border border-white/10 text-white/40 hover:text-white/80 hover:border-white/25 hover:bg-white/5 transition-all disabled:opacity-30"
        >
          <RefreshCw className={cn('w-4 h-4', loading && 'animate-spin')} />
        </button>
      </div>

      {/* Answer input */}
      <div className="relative">
        <input
          type="text"
          value={answer}
          onChange={(e) => handleChange(e.target.value)}
          placeholder="Enter 6 characters"
          maxLength={6}
          autoComplete="off"
          autoCorrect="off"
          autoCapitalize="characters"
          spellCheck={false}
          disabled={error}
          className={cn(
            'input w-full text-center font-mono text-xl tracking-[0.4em] uppercase pr-10 transition-colors',
            verified && 'border-green-500/60 bg-green-500/5',
            error && 'opacity-50 cursor-not-allowed',
          )}
        />
        {verified && (
          <ShieldCheck className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-green-400" />
        )}
      </div>

      {error && (
        <p className="text-xs text-amber-400/80 flex items-center gap-1.5">
          <AlertTriangle className="w-3.5 h-3.5 flex-shrink-0" />
          Could not load CAPTCHA.{' '}
          <button
            type="button"
            onClick={fetchChallenge}
            className="underline underline-offset-2 hover:text-amber-300 transition-colors"
          >
            Try again
          </button>
        </p>
      )}
    </div>
  );
}
