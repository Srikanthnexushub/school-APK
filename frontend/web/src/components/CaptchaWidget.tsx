import { useState, useEffect, useCallback } from 'react';
import { RefreshCw, ShieldCheck } from 'lucide-react';
import api from '../lib/api';
import { cn } from '../lib/utils';

interface Props {
  onVerify: (token: string | null) => void;
}

export default function CaptchaWidget({ onVerify }: Props) {
  const [challengeId, setChallengeId] = useState('');
  const [imageDataUri, setImageDataUri] = useState('');
  const [answer, setAnswer] = useState('');
  const [loading, setLoading] = useState(false);
  const [verified, setVerified] = useState(false);

  const fetchChallenge = useCallback(async () => {
    setLoading(true);
    setAnswer('');
    setVerified(false);
    onVerify(null);
    try {
      const res = await api.get('/api/v1/captcha/challenge');
      setChallengeId(res.data.id);
      setImageDataUri(res.data.imageDataUri);
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
        <div
          className="relative rounded-xl overflow-hidden border border-white/10 flex-shrink-0"
          style={{ width: 220, height: 80, background: '#16162b' }}
        >
          {loading ? (
            <div className="flex items-center justify-center w-full h-full">
              <div className="w-6 h-6 border-2 border-white/20 border-t-brand-400 rounded-full animate-spin" />
            </div>
          ) : imageDataUri ? (
            <img
              src={imageDataUri}
              alt="CAPTCHA challenge"
              className="w-full h-full object-cover select-none pointer-events-none"
              draggable={false}
            />
          ) : null}
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
          className={cn(
            'input w-full text-center font-mono text-xl tracking-[0.4em] uppercase pr-10 transition-colors',
            verified && 'border-green-500/60 bg-green-500/5',
            !verified && answer.length > 0 && answer.length < 6 && 'border-white/20'
          )}
        />
        {verified && (
          <ShieldCheck className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-green-400" />
        )}
      </div>

      {answer.length === 6 && !verified && (
        <p className="text-xs text-red-400">Keep typing — 6 characters required</p>
      )}
    </div>
  );
}
