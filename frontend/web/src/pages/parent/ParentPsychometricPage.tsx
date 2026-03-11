import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import {
  Brain, ChevronRight, Star, TrendingUp, TrendingDown,
  BookOpen, Loader2,
} from 'lucide-react';
import {
  RadarChart, Radar, PolarGrid, PolarAngleAxis, PolarRadiusAxis,
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid,
} from 'recharts';
import { cn } from '../../lib/utils';
import { Avatar } from '../../components/ui/Avatar';
import api from '../../lib/api';

// ─── Types ────────────────────────────────────────────────────────────────────

interface ParentProfileResponse {
  id: string;
  name: string;
}

interface StudentLinkResponse {
  id: string;
  studentId: string;
  studentName: string;
  status: string;
}

interface TraitDimension {
  trait: string;
  score: number;
  percentile: number;
}

interface CareerMapping {
  riasecCode: string;
  score: number;
  careerSuggestions: string[];
}

interface PsychProfile {
  id: string;
  studentId: string;
  sessionType: 'INITIAL' | 'REASSESSMENT';
  status: string;
  traitDimensions: TraitDimension[];
  careerMappings: CareerMapping[];
  learningStyle: string;
  completedAt: string;
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

const BIG_FIVE_LABELS: Record<string, string> = {
  OPENNESS: 'Openness',
  CONSCIENTIOUSNESS: 'Conscientiousness',
  EXTRAVERSION: 'Extraversion',
  AGREEABLENESS: 'Agreeableness',
  NEUROTICISM: 'Neuroticism',
};

const RIASEC_LABELS: Record<string, string> = {
  R: 'Realistic',
  I: 'Investigative',
  A: 'Artistic',
  S: 'Social',
  E: 'Enterprising',
  C: 'Conventional',
};

const RIASEC_COLORS: Record<string, string> = {
  R: '#f87171', I: '#818cf8', A: '#fb923c',
  S: '#34d399', E: '#fbbf24', C: '#60a5fa',
};

const LEARNING_STYLE_ICONS: Record<string, string> = {
  VISUAL: '👁️',
  AUDITORY: '👂',
  KINESTHETIC: '✋',
  'READING-WRITING': '📖',
  READING_WRITING: '📖',
};

function formatDate(iso: string): string {
  if (!iso) return '—';
  try {
    return new Date(iso).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  } catch {
    return iso;
  }
}

// ─── Big Five Radar ────────────────────────────────────────────────────────────

function BigFiveRadar({ traits }: { traits: TraitDimension[] }) {
  const data = traits.map((t) => ({
    subject: BIG_FIVE_LABELS[t.trait?.toUpperCase()] ?? t.trait,
    score: Math.round(t.score * 100),
    fullMark: 100,
  }));

  if (data.length === 0) return null;

  return (
    <div className="card">
      <h3 className="font-semibold text-white text-sm mb-4">Big Five Personality Traits</h3>
      <ResponsiveContainer width="100%" height={260}>
        <RadarChart data={data} margin={{ top: 8, right: 24, bottom: 8, left: 24 }}>
          <PolarGrid stroke="rgba(255,255,255,0.06)" />
          <PolarAngleAxis dataKey="subject" tick={{ fill: 'rgba(255,255,255,0.5)', fontSize: 11 }} />
          <PolarRadiusAxis angle={90} domain={[0, 100]} tick={{ fill: 'rgba(255,255,255,0.2)', fontSize: 9 }} />
          <Radar
            name="Score"
            dataKey="score"
            stroke="#6366f1"
            fill="#6366f1"
            fillOpacity={0.25}
            dot={{ fill: '#6366f1', r: 4 }}
          />
          <Tooltip
            contentStyle={{ background: '#252836', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12, fontSize: 12 }}
            labelStyle={{ color: 'rgba(255,255,255,0.5)' }}
            itemStyle={{ color: '#818cf8' }}
          />
        </RadarChart>
      </ResponsiveContainer>
    </div>
  );
}

// ─── RIASEC Chart ─────────────────────────────────────────────────────────────

function RiasecChart({ mappings }: { mappings: CareerMapping[] }) {
  // Build RIASEC bar data
  const data = Object.keys(RIASEC_LABELS).map((code) => {
    const mapping = mappings.find((m) => m.riasecCode?.toUpperCase().startsWith(code));
    return {
      name: RIASEC_LABELS[code],
      code,
      score: mapping ? Math.round(mapping.score * 100) : 0,
      fill: RIASEC_COLORS[code],
    };
  });

  // Top 3 codes by score
  const top3 = [...data].sort((a, b) => b.score - a.score).slice(0, 3);
  const topCode = top3.map((d) => d.code).join('');

  return (
    <div className="card">
      <div className="flex items-start justify-between mb-4">
        <div>
          <h3 className="font-semibold text-white text-sm">RIASEC Career Code</h3>
          <p className="text-xs text-white/40 mt-0.5">Holland Interest Code Profile</p>
        </div>
        <div className="text-right">
          <div className="text-lg font-bold text-brand-400">{topCode || '—'}</div>
          <div className="text-xs text-white/30">{top3.map((d) => d.name).join(' · ')}</div>
        </div>
      </div>
      <ResponsiveContainer width="100%" height={180}>
        <BarChart data={data} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.04)" vertical={false} />
          <XAxis dataKey="name" tick={{ fill: 'rgba(255,255,255,0.4)', fontSize: 10 }} axisLine={false} tickLine={false} />
          <YAxis domain={[0, 100]} tick={{ fill: 'rgba(255,255,255,0.2)', fontSize: 9 }} axisLine={false} tickLine={false} />
          <Tooltip
            contentStyle={{ background: '#252836', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12, fontSize: 12 }}
            labelStyle={{ color: 'rgba(255,255,255,0.5)' }}
          />
          <Bar dataKey="score" radius={[4, 4, 0, 0]}>
            {data.map((entry) => (
              <rect key={entry.code} fill={entry.fill} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

// ─── Career Suggestions ───────────────────────────────────────────────────────

function CareerSuggestions({ mappings }: { mappings: CareerMapping[] }) {
  const suggestions = mappings
    .flatMap((m) => m.careerSuggestions ?? [])
    .filter(Boolean)
    .slice(0, 5);

  if (suggestions.length === 0) return null;

  return (
    <div className="card">
      <h3 className="font-semibold text-white text-sm mb-4">Top Career Recommendations</h3>
      <div className="space-y-2">
        {suggestions.map((career, i) => (
          <div key={i} className="flex items-center gap-3 p-3 rounded-xl bg-white/[0.03] border border-white/5">
            <div className="w-6 h-6 rounded-full bg-brand-500/20 flex items-center justify-center flex-shrink-0">
              <span className="text-brand-400 text-xs font-bold">{i + 1}</span>
            </div>
            <span className="text-sm text-white/80">{career}</span>
            <ChevronRight className="w-4 h-4 text-white/20 ml-auto flex-shrink-0" />
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── Strengths & Growth ────────────────────────────────────────────────────────

function StrengthsCard({ traits }: { traits: TraitDimension[] }) {
  if (traits.length === 0) return null;

  const sorted = [...traits].sort((a, b) => b.score - a.score);
  const strengths = sorted.slice(0, 3);
  const growth = sorted[sorted.length - 1];

  return (
    <div className="card">
      <h3 className="font-semibold text-white text-sm mb-4">Strengths & Growth Areas</h3>
      <div className="space-y-4">
        <div>
          <div className="flex items-center gap-2 mb-2">
            <TrendingUp className="w-4 h-4 text-emerald-400" />
            <span className="text-xs font-semibold text-emerald-400 uppercase tracking-wider">Top Strengths</span>
          </div>
          <div className="space-y-2">
            {strengths.map((t) => (
              <div key={t.trait} className="flex items-center gap-3">
                <Star className="w-3.5 h-3.5 text-emerald-400 flex-shrink-0" />
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-sm text-white/80">{BIG_FIVE_LABELS[t.trait?.toUpperCase()] ?? t.trait}</span>
                    <span className="text-xs text-white/40">{Math.round(t.score * 100)}%</span>
                  </div>
                  <div className="w-full bg-white/5 rounded-full h-1">
                    <div
                      className="h-1 rounded-full bg-emerald-400 transition-all"
                      style={{ width: `${Math.round(t.score * 100)}%` }}
                    />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {growth && (
          <div>
            <div className="flex items-center gap-2 mb-2">
              <TrendingDown className="w-4 h-4 text-amber-400" />
              <span className="text-xs font-semibold text-amber-400 uppercase tracking-wider">Growth Area</span>
            </div>
            <div className="flex items-center gap-3">
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between mb-1">
                  <span className="text-sm text-white/80">{BIG_FIVE_LABELS[growth.trait?.toUpperCase()] ?? growth.trait}</span>
                  <span className="text-xs text-white/40">{Math.round(growth.score * 100)}%</span>
                </div>
                <div className="w-full bg-white/5 rounded-full h-1">
                  <div
                    className="h-1 rounded-full bg-amber-400 transition-all"
                    style={{ width: `${Math.round(growth.score * 100)}%` }}
                  />
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────

export default function ParentPsychometricPage() {
  const [activeStudentId, setActiveStudentId] = useState<string | null>(null);

  const { data: profile } = useQuery<ParentProfileResponse>({
    queryKey: ['parent-profile'],
    queryFn: () => api.get('/api/v1/parents/me').then((r) => r.data),
  });

  const { data: linkedStudents = [], isLoading: studentsLoading } = useQuery<StudentLinkResponse[]>({
    queryKey: ['linked-students', profile?.id],
    queryFn: () =>
      api.get(`/api/v1/parents/${profile!.id}/students?size=50`).then((r) => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
    enabled: !!profile?.id,
  });

  const resolvedStudentId = activeStudentId ?? linkedStudents[0]?.studentId ?? null;
  const selectedLink = linkedStudents.find((s) => s.studentId === resolvedStudentId) ?? linkedStudents[0] ?? null;

  const { data: psychProfiles = [], isLoading: psychLoading } = useQuery<PsychProfile[]>({
    queryKey: ['psych-profiles-parent', resolvedStudentId],
    queryFn: () =>
      api.get(`/api/v1/psych/profiles?studentId=${resolvedStudentId}`).then((r) => {
        const d = r.data;
        return Array.isArray(d) ? d : (d.content ?? []);
      }),
    enabled: !!resolvedStudentId,
  });

  const latestProfile = psychProfiles[0] ?? null;
  const isLoading = studentsLoading || psychLoading;

  return (
    <div className="p-4 lg:p-8 space-y-6 max-w-5xl mx-auto">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-white">Psychometric Profile</h1>
        <p className="text-white/50 text-sm mt-0.5">
          View your child's personality traits, career aptitude, and learning style.
        </p>
      </div>

      {/* Child selector */}
      {linkedStudents.length > 1 && (
        <div className="flex gap-3 overflow-x-auto pb-1">
          {linkedStudents.map((s) => (
            <button
              key={s.studentId}
              onClick={() => setActiveStudentId(s.studentId)}
              className={cn(
                'flex items-center gap-3 px-4 py-3 rounded-2xl border transition-all flex-shrink-0',
                resolvedStudentId === s.studentId
                  ? 'border-brand-500 bg-brand-500/10'
                  : 'border-white/5 bg-surface-50/40 hover:border-white/10'
              )}
            >
              <Avatar name={s.studentName} size="sm" />
              <div className="text-left">
                <div className="text-sm font-semibold text-white">{s.studentName}</div>
              </div>
            </button>
          ))}
        </div>
      )}

      {/* No children state */}
      {!studentsLoading && linkedStudents.length === 0 && (
        <div className="card text-center py-12">
          <Brain className="w-10 h-10 text-white/20 mx-auto mb-3" />
          <p className="text-white/50 text-sm">No children linked yet.</p>
          <p className="text-white/30 text-xs mt-1">Link a child to view their psychometric profile.</p>
        </div>
      )}

      {/* Loading */}
      {isLoading && resolvedStudentId && (
        <div className="flex items-center justify-center py-16">
          <Loader2 className="w-8 h-8 text-brand-400 animate-spin" />
        </div>
      )}

      {/* No profile state */}
      {!isLoading && selectedLink && !latestProfile && (
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="card text-center py-16"
        >
          <Brain className="w-12 h-12 text-white/15 mx-auto mb-4" />
          <h3 className="text-white/60 font-semibold mb-2">No Psychometric Assessment Yet</h3>
          <p className="text-white/30 text-sm max-w-md mx-auto">
            {selectedLink.studentName} hasn't completed the psychometric assessment yet.
            They can take it from their student portal under the Psychometric section.
          </p>
          <div className="mt-6 p-4 bg-brand-500/5 border border-brand-500/15 rounded-xl max-w-md mx-auto">
            <div className="flex items-start gap-3 text-left">
              <Brain className="w-5 h-5 text-brand-400 flex-shrink-0 mt-0.5" />
              <div>
                <div className="text-sm font-medium text-white mb-1">About the Assessment</div>
                <p className="text-xs text-white/40">
                  The Big Five personality assessment takes about 10 minutes and provides insights
                  into personality traits, career aptitude (RIASEC), and learning style.
                </p>
              </div>
            </div>
          </div>
        </motion.div>
      )}

      {/* Profile data */}
      {!isLoading && latestProfile && (
        <>
          {/* Session info */}
          <div className="flex items-center gap-3 flex-wrap">
            <span className="text-xs text-white/40">
              Assessment completed: <span className="text-white/60">{formatDate(latestProfile.completedAt)}</span>
            </span>
            <span className="text-xs px-2 py-0.5 rounded-full bg-brand-500/15 text-brand-400 font-medium">
              {latestProfile.sessionType}
            </span>
            <span className={cn(
              'text-xs px-2 py-0.5 rounded-full font-medium',
              latestProfile.status === 'COMPLETED' ? 'bg-emerald-500/15 text-emerald-400' : 'bg-amber-500/15 text-amber-400'
            )}>
              {latestProfile.status}
            </span>
          </div>

          {/* Learning style */}
          {latestProfile.learningStyle && (
            <div className="card border border-brand-500/15">
              <div className="flex items-center gap-3">
                <div className="p-2.5 rounded-xl bg-brand-500/15 flex-shrink-0">
                  <BookOpen className="w-5 h-5 text-brand-400" />
                </div>
                <div>
                  <div className="text-xs text-white/40 mb-0.5">Dominant Learning Style</div>
                  <div className="text-lg font-bold text-white flex items-center gap-2">
                    <span>{LEARNING_STYLE_ICONS[latestProfile.learningStyle?.toUpperCase()] ?? '🧠'}</span>
                    <span className="capitalize">{latestProfile.learningStyle.replace('_', '-').toLowerCase()}</span>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Radar + Strengths grid */}
          <div className="grid lg:grid-cols-2 gap-6">
            {latestProfile.traitDimensions?.length > 0 && (
              <BigFiveRadar traits={latestProfile.traitDimensions} />
            )}
            {latestProfile.traitDimensions?.length > 0 && (
              <StrengthsCard traits={latestProfile.traitDimensions} />
            )}
          </div>

          {/* RIASEC + Career */}
          {latestProfile.careerMappings?.length > 0 && (
            <div className="grid lg:grid-cols-2 gap-6">
              <RiasecChart mappings={latestProfile.careerMappings} />
              <CareerSuggestions mappings={latestProfile.careerMappings} />
            </div>
          )}
        </>
      )}
    </div>
  );
}
