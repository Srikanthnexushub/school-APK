// src/hooks/useStaffAI.ts
// AI-powered hooks for the Staff portal — bio generation and gap analysis.
// All prompts are constructed dynamically from live data (no hardcoded text blobs).

import { useState } from 'react';
import api from '../lib/api';
import { SUBJECT_OPTIONS } from '../constants/staffConstants';

// ─── Types ────────────────────────────────────────────────────────────────────

interface StaffBioParams {
  firstName: string;
  lastName: string;
  roleType: string;
  designation: string;
  subjects: string;
  yearsOfExperience: number | null;
  qualification: string;
}

interface StaffMemberForAnalysis {
  status: string;
  subjects: string | null;
  roleType: string | null;
}

interface GapAnalysisResult {
  recommendation: string;
  subjectCoverage: Record<string, number>;
  understaffedSubjects: string[];
}

// ─── Bio Generator ────────────────────────────────────────────────────────────

export function useStaffBioGenerator() {
  const [isGenerating, setIsGenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function generateBio(params: StaffBioParams): Promise<string> {
    setIsGenerating(true);
    setError(null);

    const roleLabel   = params.designation || params.roleType;
    const subjectText = params.subjects
      ? `specialising in ${params.subjects}`
      : '';
    const expText = params.yearsOfExperience != null
      ? params.yearsOfExperience === 0
        ? 'as a fresher'
        : `with ${params.yearsOfExperience} year${params.yearsOfExperience === 1 ? '' : 's'} of teaching experience`
      : '';
    const qualText = params.qualification ? `holding ${params.qualification}` : '';

    const contextParts = [roleLabel, subjectText, expText, qualText].filter(Boolean);

    const systemPrompt = [
      'You are a professional HR writer specialising in the education sector.',
      'Generate a concise, third-person professional bio (2–3 sentences, max 80 words).',
      'The bio must highlight expertise and value to students.',
      'Do not use first-person pronouns.',
      'Return only the bio text — no labels, no bullet points, no extra formatting.',
    ].join(' ');

    const userMessage = [
      `Write a professional staff bio for: ${params.firstName} ${params.lastName},`,
      contextParts.join(', ') + '.',
    ].join(' ');

    try {
      const res = await api.post('/api/v1/ai/completions', {
        requesterId: 'staff-bio-generator',
        systemPrompt,
        userMessage,
        maxTokens: 150,
        temperature: 0.65,
      });
      return (res.data?.content ?? '').trim();
    } catch {
      setError('Bio generation failed — please write one manually or try again.');
      return '';
    } finally {
      setIsGenerating(false);
    }
  }

  return { generateBio, isGenerating, error };
}

// ─── Gap Analyzer ─────────────────────────────────────────────────────────────

export function useStaffGapAnalysis() {
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [result, setResult] = useState<GapAnalysisResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function analyzeGaps(staff: StaffMemberForAnalysis[]): Promise<GapAnalysisResult> {
    setIsAnalyzing(true);
    setError(null);

    const active = staff.filter(s => s.status === 'ACTIVE');

    // Build subject coverage map from active staff
    const subjectCoverage: Record<string, number> = {};
    for (const subject of SUBJECT_OPTIONS) {
      subjectCoverage[subject] = active.filter(s =>
        s.subjects?.split(',').map(x => x.trim()).includes(subject)
      ).length;
    }

    const understaffedSubjects = Object.entries(subjectCoverage)
      .filter(([, count]) => count === 0)
      .map(([subject]) => subject);

    const coverageSummary = Object.entries(subjectCoverage)
      .map(([subject, count]) => `${subject}: ${count}`)
      .join(', ');

    const roleSummary = active.reduce<Record<string, number>>((acc, s) => {
      if (s.roleType) acc[s.roleType] = (acc[s.roleType] ?? 0) + 1;
      return acc;
    }, {});
    const roleSummaryText = Object.entries(roleSummary)
      .map(([role, count]) => `${role}: ${count}`)
      .join(', ');

    const systemPrompt = [
      'You are an educational staffing analyst.',
      'Analyse staff coverage data and provide exactly 3 concise hiring recommendations as bullet points (•).',
      'Be specific about subject gaps and role gaps.',
      'Each bullet must be one actionable sentence.',
      'Return only the 3 bullet points — no preamble, no headings.',
    ].join(' ');

    const userMessage = [
      `Active staff count: ${active.length}.`,
      `Subject coverage (staff count per subject): ${coverageSummary}.`,
      `Role distribution: ${roleSummaryText || 'none recorded'}.`,
      `Subjects with zero coverage: ${understaffedSubjects.join(', ') || 'none'}.`,
      'What are the 3 most urgent hiring priorities?',
    ].join(' ');

    try {
      const res = await api.post('/api/v1/ai/completions', {
        requesterId: 'staff-gap-analysis',
        systemPrompt,
        userMessage,
        maxTokens: 200,
        temperature: 0.35,
      });

      const recommendation = (res.data?.content ?? '').trim();
      const analysisResult: GapAnalysisResult = {
        recommendation,
        subjectCoverage,
        understaffedSubjects,
      };
      setResult(analysisResult);
      return analysisResult;
    } catch {
      setError('Gap analysis failed — please try again.');
      const fallback: GapAnalysisResult = {
        recommendation: '',
        subjectCoverage,
        understaffedSubjects,
      };
      setResult(fallback);
      return fallback;
    } finally {
      setIsAnalyzing(false);
    }
  }

  return { analyzeGaps, isAnalyzing, result, error };
}
