/**
 * Voice persona config — maps each user role to an AI persona with
 * ElevenLabs voice ID, visual theme colours, and UI metadata.
 *
 * Voice IDs use ElevenLabs premade voices (no API lookup required):
 *   NEO  → Rachel  21m00Tcm4TlvDq8ikWAM
 *   SAGE → Daniel  onwK4e9ZLuTAKqWW03F9
 *   ARIA → Elli    MF3mGyEYCl7XYWbV9V6O
 *   APEX → Josh    TxGEqnHWrfWFTfGW9XjX
 *   NEXUS→ George  JBFqnCBsd6RMkjVDRZzb
 */

export type PersonaName = 'NEO' | 'SAGE' | 'ARIA' | 'APEX' | 'NEXUS';

export interface VoicePersonaConfig {
  name: string;
  tagline: string;
  /** Primary neon glow colour (Tailwind arbitrary value) */
  glowColor: string;
  /** CSS gradient for the orb */
  orbGradient: string;
  /** Accent hex for waveform bars */
  accentHex: string;
  /** ElevenLabs voice ID */
  voiceId: string;
}

export const PERSONA_CONFIGS: Record<PersonaName, VoicePersonaConfig> = {
  NEO: {
    name: 'SRI',
    tagline: '',
    glowColor: 'rgba(99,102,241,0.6)',   // indigo
    orbGradient: 'radial-gradient(circle at 35% 35%, #818cf8, #4f46e5, #1e1b4b)',
    accentHex: '#818cf8',
    voiceId: '21m00Tcm4TlvDq8ikWAM',
  },
  SAGE: {
    name: 'SAGE',
    tagline: 'Your teaching intelligence. Elevating every classroom.',
    glowColor: 'rgba(20,184,166,0.6)',   // teal
    orbGradient: 'radial-gradient(circle at 35% 35%, #2dd4bf, #0f766e, #042f2e)',
    accentHex: '#2dd4bf',
    voiceId: 'onwK4e9ZLuTAKqWW03F9',
  },
  ARIA: {
    name: 'ARIA',
    tagline: 'Your family education advisor. Informed and empowered.',
    glowColor: 'rgba(244,114,182,0.6)',  // pink
    orbGradient: 'radial-gradient(circle at 35% 35%, #f9a8d4, #db2777, #500724)',
    accentHex: '#f472b6',
    voiceId: 'MF3mGyEYCl7XYWbV9V6O',
  },
  APEX: {
    name: 'APEX',
    tagline: 'Your center management assistant. Running smarter.',
    glowColor: 'rgba(251,146,60,0.6)',   // orange
    orbGradient: 'radial-gradient(circle at 35% 35%, #fdba74, #ea580c, #431407)',
    accentHex: '#fb923c',
    voiceId: 'TxGEqnHWrfWFTfGW9XjX',
  },
  NEXUS: {
    name: 'NEXUS',
    tagline: 'Your institution intelligence hub. Command-level insights.',
    glowColor: 'rgba(168,85,247,0.6)',   // violet
    orbGradient: 'radial-gradient(circle at 35% 35%, #c084fc, #9333ea, #3b0764)',
    accentHex: '#a855f7',
    voiceId: 'JBFqnCBsd6RMkjVDRZzb',
  },
};

export type UserRole =
  | 'STUDENT'
  | 'TEACHER'
  | 'PARENT'
  | 'CENTER_ADMIN'
  | 'INSTITUTION_ADMIN'
  | 'SUPER_ADMIN';

export function personaForRole(role: UserRole): VoicePersonaConfig {
  switch (role) {
    case 'STUDENT':           return PERSONA_CONFIGS.NEO;
    case 'TEACHER':           return PERSONA_CONFIGS.SAGE;
    case 'PARENT':            return PERSONA_CONFIGS.ARIA;
    case 'CENTER_ADMIN':      return PERSONA_CONFIGS.APEX;
    case 'INSTITUTION_ADMIN':
    case 'SUPER_ADMIN':       return PERSONA_CONFIGS.NEXUS;
    default:                  return PERSONA_CONFIGS.NEO;
  }
}
