import {
  RadarChart as RechartsRadarChart,
  PolarGrid,
  PolarAngleAxis,
  Radar,
  ResponsiveContainer,
  Tooltip,
} from 'recharts';

interface SubjectRadarChartProps {
  data: { subject: string; score: number }[];
}

export function SubjectRadarChart({ data }: SubjectRadarChartProps) {
  return (
    <ResponsiveContainer width="100%" height={320}>
      <RechartsRadarChart data={data} margin={{ top: 16, right: 24, bottom: 16, left: 24 }}>
        <PolarGrid stroke="rgba(255,255,255,0.08)" />
        <PolarAngleAxis
          dataKey="subject"
          tick={{ fill: 'rgba(255,255,255,0.5)', fontSize: 12 }}
        />
        <Tooltip
          contentStyle={{
            backgroundColor: '#1a1d27',
            border: '1px solid rgba(255,255,255,0.1)',
            borderRadius: '12px',
            color: '#fff',
            fontSize: '13px',
          }}
          formatter={(value: number) => [`${value}%`, 'Mastery']}
        />
        <Radar
          name="Mastery"
          dataKey="score"
          stroke="#6366f1"
          fill="#6366f1"
          fillOpacity={0.25}
          strokeWidth={2}
          dot={{ r: 3, fill: '#6366f1', strokeWidth: 0 }}
        />
      </RechartsRadarChart>
    </ResponsiveContainer>
  );
}
