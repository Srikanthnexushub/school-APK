import React from 'react';
import { motion } from 'framer-motion';

interface VoiceWaveformProps {
  /** 0–1 input energy for "listening" state */
  level: number;
  /** Whether to animate (speaking/listening) */
  active: boolean;
  accentHex: string;
  bars?: number;
}

/**
 * Animated equalizer bars — grows with input level when listening,
 * plays a sine-wave animation when the AI is speaking.
 */
export function VoiceWaveform({ level, active, accentHex, bars = 7 }: VoiceWaveformProps) {
  return (
    <div className="flex items-center justify-center gap-[3px] h-8">
      {Array.from({ length: bars }, (_, i) => {
        const delay = i * 0.08;
        const baseHeight = active ? 4 + level * 28 : 4;
        const variation = active ? Math.sin((i / bars) * Math.PI) * 12 : 0;

        return (
          <motion.div
            key={i}
            style={{ backgroundColor: accentHex, borderRadius: 2, width: 3 }}
            animate={{
              height: active
                ? [baseHeight + variation, baseHeight + variation * 1.6, baseHeight + variation * 0.4, baseHeight + variation]
                : 4,
              opacity: active ? [0.7, 1, 0.8, 0.7] : 0.3,
            }}
            transition={{
              duration: 0.7,
              delay,
              repeat: Infinity,
              ease: 'easeInOut',
            }}
          />
        );
      })}
    </div>
  );
}
