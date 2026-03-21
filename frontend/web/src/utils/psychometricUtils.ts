// src/utils/psychometricUtils.ts
// Centralised psychometric question bank — 5 education levels, 10 questions each.
// Used by student portal (PsychometricPage) and parent portal (ParentPsychometricPage).

export type EducationLevel = 'primary' | 'middle' | 'secondary' | 'higher_secondary' | 'adult';

export interface PsychQuestion {
  id: number;
  trait: 'openness' | 'conscientiousness' | 'extraversion' | 'agreeableness' | 'neuroticism';
  text: string;
}

/** Derive education level from currentClass (null/undefined → adult) */
export function getEducationLevel(currentClass?: number | null): EducationLevel {
  if (!currentClass) return 'adult';
  if (currentClass <= 5)  return 'primary';
  if (currentClass <= 8)  return 'middle';
  if (currentClass <= 10) return 'secondary';
  if (currentClass <= 12) return 'higher_secondary';
  return 'adult';
}

/** Return 10 questions (2 per Big Five trait) adapted to the education level and optional context */
export function getAdaptiveQuestions(
  level: EducationLevel,
  context?: { board?: string; stream?: string }
): PsychQuestion[] {
  const isCBSE     = context?.board   === 'CBSE';
  const isScience  = context?.stream  === 'PCM' || context?.stream === 'PCB';
  const isCommerce = context?.stream  === 'COMMERCE';

  const banks: Record<EducationLevel, PsychQuestion[]> = {
    primary: [
      { id: 0, trait: 'openness',          text: 'I like trying new games or activities I have never done before.' },
      { id: 1, trait: 'openness',          text: 'I enjoy drawing, storytelling, or making things with my hands.' },
      { id: 2, trait: 'conscientiousness', text: 'I finish my homework before going out to play.' },
      { id: 3, trait: 'conscientiousness', text: 'I keep my books and belongings tidy and in their place.' },
      { id: 4, trait: 'extraversion',      text: 'I love playing with friends and talking to new people.' },
      { id: 5, trait: 'extraversion',      text: 'I feel happy when I take part in class activities or school events.' },
      { id: 6, trait: 'agreeableness',     text: 'I share my things with friends and try to be kind to everyone.' },
      { id: 7, trait: 'agreeableness',     text: 'I try not to hurt my friends\' feelings, even when I am upset.' },
      { id: 8, trait: 'neuroticism',       text: 'I feel very sad or worried when I make even a small mistake.' },
      { id: 9, trait: 'neuroticism',       text: 'I get upset easily when things do not go the way I want them to.' },
    ],

    middle: [
      { id: 0, trait: 'openness',          text: 'I enjoy learning about topics that are not part of my school syllabus.' },
      { id: 1, trait: 'openness',          text: 'I like experimenting with new hobbies, clubs, or creative activities.' },
      { id: 2, trait: 'conscientiousness', text: 'I complete my assignments before the deadline without being reminded.' },
      { id: 3, trait: 'conscientiousness', text: 'I keep my study notes organised and review them regularly.' },
      { id: 4, trait: 'extraversion',      text: 'I enjoy participating in group projects, debates, or classroom discussions.' },
      { id: 5, trait: 'extraversion',      text: 'I feel energised and happy after spending time with friends or classmates.' },
      { id: 6, trait: 'agreeableness',     text: 'I try to help my classmates understand topics they find difficult.' },
      { id: 7, trait: 'agreeableness',     text: 'I listen respectfully to others\' opinions even when I disagree.' },
      { id: 8, trait: 'neuroticism',       text: 'A poor test score or small failure makes me feel anxious for a long time.' },
      { id: 9, trait: 'neuroticism',       text: 'I find it hard to concentrate on my studies when I am stressed or worried.' },
    ],

    secondary: [
      { id: 0, trait: 'openness',          text: 'I explore subjects and concepts beyond what is required for my board exams.' },
      { id: 1, trait: 'openness',          text: 'I enjoy thinking about different ways to solve a problem.' },
      { id: 2, trait: 'conscientiousness', text: isCBSE
          ? 'I follow my CBSE study schedule diligently and complete NCERT exercises on time.'
          : 'I complete my study tasks on time and maintain a consistent daily study routine.' },
      { id: 3, trait: 'conscientiousness', text: 'I keep my notes, study materials, and assignments well organised.' },
      { id: 4, trait: 'extraversion',      text: 'I feel energised after participating in group study sessions or class discussions.' },
      { id: 5, trait: 'extraversion',      text: 'I enjoy presenting my ideas in class and taking part in school activities.' },
      { id: 6, trait: 'agreeableness',     text: 'I enjoy helping my classmates understand difficult topics when they struggle.' },
      { id: 7, trait: 'agreeableness',     text: 'I respect others\' perspectives in group projects even when I disagree.' },
      { id: 8, trait: 'neuroticism',       text: 'Board exam pressure significantly affects my focus and overall wellbeing.' },
      { id: 9, trait: 'neuroticism',       text: 'Small setbacks like a poor test score can disrupt my focus for several days.' },
    ],

    higher_secondary: [
      { id: 0, trait: 'openness',          text: isScience
          ? 'I enjoy solving unfamiliar problems and thinking about scientific possibilities beyond my syllabus.'
          : isCommerce
          ? 'I find it exciting to analyse business trends and think creatively about economic challenges.'
          : 'I often think about abstract concepts and enjoy creative expression in my learning.' },
      { id: 1, trait: 'openness',          text: 'I actively seek knowledge and perspectives beyond my chosen stream.' },
      { id: 2, trait: 'conscientiousness', text: isCBSE
          ? 'I follow the CBSE study schedule diligently and complete NCERT exercises on time.'
          : 'I maintain a consistent study plan and meet all deadlines without external prompting.' },
      { id: 3, trait: 'conscientiousness', text: 'I keep my study materials and revision notes well organised and up to date.' },
      { id: 4, trait: 'extraversion',      text: 'I proactively seek mentors, peers, or online communities to deepen my knowledge.' },
      { id: 5, trait: 'extraversion',      text: 'I feel energised by group study sessions, debates, and collaborative learning.' },
      { id: 6, trait: 'agreeableness',     text: 'I enjoy helping classmates understand difficult concepts in my stream.' },
      { id: 7, trait: 'agreeableness',     text: 'I try to understand different perspectives in group projects and respect others\' opinions.' },
      { id: 8, trait: 'neuroticism',       text: 'Competitive pressure from entrance exam preparation significantly affects my focus and wellbeing.' },
      { id: 9, trait: 'neuroticism',       text: 'Small setbacks in my studies — like a poor mock test score — can disrupt my focus for days.' },
    ],

    adult: [
      { id: 0, trait: 'openness',          text: 'I actively seek out new knowledge and skills outside my regular work or responsibilities.' },
      { id: 1, trait: 'openness',          text: 'I enjoy exploring diverse perspectives and engaging with complex, abstract ideas.' },
      { id: 2, trait: 'conscientiousness', text: 'I set clear goals and consistently follow through on plans and commitments.' },
      { id: 3, trait: 'conscientiousness', text: 'I maintain an organised approach to managing my responsibilities and daily tasks.' },
      { id: 4, trait: 'extraversion',      text: 'I feel energised by social interactions, networking, and collaborative activities.' },
      { id: 5, trait: 'extraversion',      text: 'I proactively take initiative in group settings and enjoy leadership opportunities.' },
      { id: 6, trait: 'agreeableness',     text: 'I prioritise understanding others\' viewpoints and maintaining harmony in relationships.' },
      { id: 7, trait: 'agreeableness',     text: 'I readily offer support and guidance to people around me when they need help.' },
      { id: 8, trait: 'neuroticism',       text: 'Professional or life pressures significantly affect my focus and emotional balance.' },
      { id: 9, trait: 'neuroticism',       text: 'I tend to overthink decisions and feel anxious about uncertainties in my life.' },
    ],
  };

  return banks[level];
}

/** Compute Big Five trait scores (0–1) from quiz answers */
export function computeTraits(questions: PsychQuestion[], answers: Record<number, number>) {
  const traitAvg = (trait: string) => {
    const qs = questions.filter(q => q.trait === trait);
    const sum = qs.reduce((acc, q) => acc + (answers[q.id] ?? 3), 0);
    return parseFloat((sum / qs.length / 5.0).toFixed(4));
  };
  const openness          = traitAvg('openness');
  const conscientiousness = traitAvg('conscientiousness');
  const extraversion      = traitAvg('extraversion');
  const agreeableness     = traitAvg('agreeableness');
  const neuroticism       = traitAvg('neuroticism');

  const riasecScores: Record<string, number> = {
    R: conscientiousness * 0.5 + (1 - openness) * 0.5,
    I: openness * 0.7 + conscientiousness * 0.3,
    A: openness * 0.8 + extraversion * 0.2,
    S: agreeableness * 0.6 + extraversion * 0.4,
    E: extraversion * 0.5 + (1 - neuroticism) * 0.5,
    C: conscientiousness * 0.7 + (1 - openness) * 0.3,
  };
  const riasecCode = Object.entries(riasecScores)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 3)
    .map(([k]) => k)
    .join('-');

  return { openness, conscientiousness, extraversion, agreeableness, neuroticism, riasecCode };
}
