// Offline question bank — pre-written MCQs grouped by subject › chapter › difficulty
// Used when the Question Bank page is in "Offline" mode (no AI API call required).

export type OfflineDifficulty = 'Low' | 'Medium' | 'High';

export interface OfflineQuestion {
  q: string;
  options: string[];
  answer: string; // "A" | "B" | "C" | "D"
  explanation: string;
  chapter: string;
  subject: string;
  difficulty: OfflineDifficulty;
}

// ─── Physics ─────────────────────────────────────────────────────────────────

const physicsQuestions: OfflineQuestion[] = [
  // Mechanics — Low
  { subject: 'Physics', chapter: 'Mechanics', difficulty: 'Low',
    q: 'A body is in uniform circular motion. The work done by the centripetal force in one complete revolution is:',
    options: ['A) Positive', 'B) Negative', 'C) Zero', 'D) Depends on speed'],
    answer: 'C', explanation: 'Centripetal force is always perpendicular to displacement, so work done = 0.' },
  { subject: 'Physics', chapter: 'Mechanics', difficulty: 'Low',
    q: 'The SI unit of momentum is:',
    options: ['A) kg·m/s²', 'B) kg·m/s', 'C) N·s²', 'D) J/m'],
    answer: 'B', explanation: 'Momentum = mass × velocity → kg × m/s = kg·m/s.' },
  { subject: 'Physics', chapter: 'Mechanics', difficulty: 'Low',
    q: 'Newton\'s third law states that every action has an equal and opposite:',
    options: ['A) Force', 'B) Reaction', 'C) Impulse', 'D) Acceleration'],
    answer: 'B', explanation: 'For every action force there is an equal and opposite reaction force.' },
  { subject: 'Physics', chapter: 'Mechanics', difficulty: 'Low',
    q: 'Which of the following is a scalar quantity?',
    options: ['A) Velocity', 'B) Acceleration', 'C) Speed', 'D) Force'],
    answer: 'C', explanation: 'Speed has magnitude only; velocity, acceleration, and force have both magnitude and direction.' },

  // Mechanics — Medium
  { subject: 'Physics', chapter: 'Mechanics', difficulty: 'Medium',
    q: 'A 2 kg object is moving at 3 m/s. A net force of 4 N acts on it for 2 s. What is the final speed?',
    options: ['A) 5 m/s', 'B) 7 m/s', 'C) 9 m/s', 'D) 11 m/s'],
    answer: 'B', explanation: 'Impulse = F·t = 4×2 = 8 N·s = Δp. Initial p = 6 kg·m/s, final p = 14 kg·m/s → v = 14/2 = 7 m/s.' },
  { subject: 'Physics', chapter: 'Mechanics', difficulty: 'Medium',
    q: 'A projectile is thrown with velocity 20 m/s at 30° to the horizontal. The maximum height reached is (g = 10 m/s²):',
    options: ['A) 5 m', 'B) 10 m', 'C) 15 m', 'D) 20 m'],
    answer: 'A', explanation: 'H = u²sin²θ / 2g = (400 × 0.25) / 20 = 5 m.' },
  { subject: 'Physics', chapter: 'Mechanics', difficulty: 'Medium',
    q: 'The escape velocity from the Earth\'s surface is approximately:',
    options: ['A) 7.9 km/s', 'B) 11.2 km/s', 'C) 3.4 km/s', 'D) 15.6 km/s'],
    answer: 'B', explanation: 'Escape velocity = √(2GM/R) ≈ 11.2 km/s for Earth.' },

  // Mechanics — High
  { subject: 'Physics', chapter: 'Mechanics', difficulty: 'High',
    q: 'A uniform rod of length L and mass M is hinged at one end. The moment of inertia about the hinge is:',
    options: ['A) ML²/2', 'B) ML²/3', 'C) ML²/4', 'D) ML²/12'],
    answer: 'B', explanation: 'For a rod about one end: I = ML²/3 (standard result from integration).' },
  { subject: 'Physics', chapter: 'Mechanics', difficulty: 'High',
    q: 'A particle executes SHM with amplitude A and angular frequency ω. At displacement x = A/2, the kinetic energy is:',
    options: ['A) mω²A²/4', 'B) 3mω²A²/8', 'C) mω²A²/2', 'D) 3mω²A²/4'],
    answer: 'B', explanation: 'KE = ½mω²(A² - x²) = ½mω²(A² - A²/4) = ½mω²·3A²/4 = 3mω²A²/8.' },

  // Thermodynamics — Medium
  { subject: 'Physics', chapter: 'Thermodynamics', difficulty: 'Medium',
    q: 'In an adiabatic process, which quantity remains constant?',
    options: ['A) Temperature', 'B) Pressure', 'C) Heat exchange', 'D) Volume'],
    answer: 'C', explanation: 'Adiabatic process: no heat exchange (Q = 0) between system and surroundings.' },
  { subject: 'Physics', chapter: 'Thermodynamics', difficulty: 'Medium',
    q: 'The efficiency of a Carnot engine operating between 500 K and 300 K is:',
    options: ['A) 20%', 'B) 30%', 'C) 40%', 'D) 60%'],
    answer: 'C', explanation: 'η = 1 - T_cold/T_hot = 1 - 300/500 = 0.4 = 40%.' },
  { subject: 'Physics', chapter: 'Thermodynamics', difficulty: 'Low',
    q: 'The first law of thermodynamics is a statement of conservation of:',
    options: ['A) Mass', 'B) Momentum', 'C) Energy', 'D) Entropy'],
    answer: 'C', explanation: 'ΔU = Q - W is the first law — it expresses conservation of energy.' },

  // Electrostatics — Low
  { subject: 'Physics', chapter: 'Electrostatics', difficulty: 'Low',
    q: 'The electric field inside a uniformly charged hollow sphere is:',
    options: ['A) Maximum at centre', 'B) Non-uniform', 'C) Zero', 'D) Same as outside'],
    answer: 'C', explanation: 'By Gauss\'s law, the enclosed charge inside a conducting sphere is zero, so E = 0 inside.' },
  { subject: 'Physics', chapter: 'Electrostatics', difficulty: 'Medium',
    q: 'Two point charges +q and -q are placed 2 m apart. The electric potential at the midpoint is:',
    options: ['A) kq/m', 'B) -kq/m', 'C) 2kq/m', 'D) Zero'],
    answer: 'D', explanation: 'Potential is scalar. V = kq/1 + k(-q)/1 = 0 at the midpoint.' },
  { subject: 'Physics', chapter: 'Electrostatics', difficulty: 'High',
    q: 'The capacitance of a parallel plate capacitor is doubled when:',
    options: ['A) Distance between plates is doubled', 'B) Area of plates is halved', 'C) A dielectric of K=2 is inserted', 'D) Voltage across it is doubled'],
    answer: 'C', explanation: 'C = Kε₀A/d. Inserting K=2 doubles C. Doubling d halves C; halving A halves C.' },

  // Optics — Low
  { subject: 'Physics', chapter: 'Optics', difficulty: 'Low',
    q: 'The phenomenon responsible for the blue colour of the sky is:',
    options: ['A) Reflection', 'B) Refraction', 'C) Rayleigh scattering', 'D) Total internal reflection'],
    answer: 'C', explanation: 'Shorter (blue) wavelengths are scattered more by atmospheric molecules — Rayleigh scattering.' },
  { subject: 'Physics', chapter: 'Optics', difficulty: 'Medium',
    q: 'A concave mirror has a focal length of 20 cm. An object at 60 cm produces an image at:',
    options: ['A) 30 cm', 'B) 40 cm', 'C) 20 cm', 'D) 15 cm'],
    answer: 'A', explanation: '1/v + 1/u = 1/f → 1/v + 1/(-60) = 1/(-20) → 1/v = -1/20 + 1/60 = -2/60 → v = -30 cm.' },

  // Modern Physics — High
  { subject: 'Physics', chapter: 'Modern Physics', difficulty: 'High',
    q: 'In photoelectric effect, if the frequency of incident light is doubled, the maximum kinetic energy of emitted electrons:',
    options: ['A) Doubles', 'B) More than doubles', 'C) Becomes hf - φ', 'D) Becomes 2hf - φ'],
    answer: 'D', explanation: 'KE_max = hf - φ. At 2f: KE = 2hf - φ, which is more than double of (hf - φ).' },
  { subject: 'Physics', chapter: 'Modern Physics', difficulty: 'Medium',
    q: 'De Broglie wavelength of a particle with momentum p is:',
    options: ['A) h/p', 'B) hp', 'C) h/p²', 'D) p/h'],
    answer: 'A', explanation: 'λ = h/p is the de Broglie relation (h = Planck\'s constant).' },
];

// ─── Chemistry ───────────────────────────────────────────────────────────────

const chemistryQuestions: OfflineQuestion[] = [
  // Organic Chemistry — Low
  { subject: 'Chemistry', chapter: 'Organic Chemistry', difficulty: 'Low',
    q: 'Which functional group is present in alcohols?',
    options: ['A) -COOH', 'B) -OH', 'C) -CHO', 'D) -NH₂'],
    answer: 'B', explanation: '-OH (hydroxyl) is the characteristic functional group of alcohols.' },
  { subject: 'Chemistry', chapter: 'Organic Chemistry', difficulty: 'Low',
    q: 'The IUPAC name of CH₃-CH₂-OH is:',
    options: ['A) Methanol', 'B) Propanol', 'C) Ethanol', 'D) Butanol'],
    answer: 'C', explanation: 'CH₃-CH₂-OH has 2 carbons with an -OH group → Ethanol.' },
  { subject: 'Chemistry', chapter: 'Organic Chemistry', difficulty: 'Medium',
    q: 'Which reaction converts an alkene to an alkane?',
    options: ['A) Halogenation', 'B) Hydrogenation', 'C) Nitration', 'D) Sulfonation'],
    answer: 'B', explanation: 'Hydrogenation (addition of H₂ over Ni/Pd catalyst) saturates the double bond.' },
  { subject: 'Chemistry', chapter: 'Organic Chemistry', difficulty: 'High',
    q: 'The major product of Markovnikov addition of HBr to propene is:',
    options: ['A) 1-bromopropane', 'B) 2-bromopropane', 'C) 1,2-dibromopropane', 'D) Bromocyclopropane'],
    answer: 'B', explanation: 'Markovnikov rule: H adds to the less substituted carbon. H adds to C1, Br to C2 → 2-bromopropane.' },

  // Inorganic Chemistry — Medium
  { subject: 'Chemistry', chapter: 'Inorganic Chemistry', difficulty: 'Medium',
    q: 'Which of the following has the highest ionisation energy?',
    options: ['A) Na', 'B) Mg', 'C) Al', 'D) Si'],
    answer: 'B', explanation: 'Mg has a completely filled 3s² sub-shell making it extra stable → higher IE than Al.' },
  { subject: 'Chemistry', chapter: 'Inorganic Chemistry', difficulty: 'Low',
    q: 'The oxidation state of Mn in KMnO₄ is:',
    options: ['A) +4', 'B) +6', 'C) +7', 'D) +5'],
    answer: 'C', explanation: 'K(+1) + Mn + 4O(-8) = 0 → Mn = +7.' },
  { subject: 'Chemistry', chapter: 'Inorganic Chemistry', difficulty: 'High',
    q: 'The shape of PCl₅ molecule according to VSEPR theory is:',
    options: ['A) Tetrahedral', 'B) Square pyramidal', 'C) Trigonal bipyramidal', 'D) Octahedral'],
    answer: 'C', explanation: 'PCl₅ has 5 bond pairs, no lone pairs → trigonal bipyramidal (VSEPR).' },

  // Physical Chemistry — Medium
  { subject: 'Chemistry', chapter: 'Physical Chemistry', difficulty: 'Medium',
    q: 'For an ideal gas, PV = nRT. If T is doubled at constant V, the pressure:',
    options: ['A) Halves', 'B) Doubles', 'C) Quadruples', 'D) Remains same'],
    answer: 'B', explanation: 'At constant n and V, P ∝ T. Doubling T doubles P (Gay-Lussac\'s law).' },
  { subject: 'Chemistry', chapter: 'Physical Chemistry', difficulty: 'High',
    q: 'The rate constant of a first-order reaction has units:',
    options: ['A) mol L⁻¹ s⁻¹', 'B) L mol⁻¹ s⁻¹', 'C) s⁻¹', 'D) L² mol⁻² s⁻¹'],
    answer: 'C', explanation: 'For first-order: rate = k[A] → k = rate/[A] = (mol L⁻¹ s⁻¹)/(mol L⁻¹) = s⁻¹.' },
  { subject: 'Chemistry', chapter: 'Physical Chemistry', difficulty: 'Low',
    q: 'Raoult\'s law applies to:',
    options: ['A) Ideal solutions', 'B) Non-ideal solutions only', 'C) Gases', 'D) Electrolytes'],
    answer: 'A', explanation: 'Raoult\'s law (P = x·P°) holds strictly for ideal solutions where solute-solvent interactions equal solvent-solvent interactions.' },

  // Electrochemistry — High
  { subject: 'Chemistry', chapter: 'Electrochemistry', difficulty: 'High',
    q: 'The standard electrode potential of the hydrogen electrode under standard conditions is:',
    options: ['A) +1.23 V', 'B) -0.76 V', 'C) 0.00 V', 'D) +0.34 V'],
    answer: 'C', explanation: 'The SHE (standard hydrogen electrode) is defined as 0.00 V — all other potentials are measured relative to it.' },
  { subject: 'Chemistry', chapter: 'Electrochemistry', difficulty: 'Medium',
    q: 'During electrolysis of NaCl solution, at the cathode we get:',
    options: ['A) Cl₂ gas', 'B) Na metal', 'C) H₂ gas', 'D) O₂ gas'],
    answer: 'C', explanation: 'In aqueous NaCl, water is preferentially reduced at cathode: 2H₂O + 2e⁻ → H₂ + 2OH⁻.' },
];

// ─── Mathematics ─────────────────────────────────────────────────────────────

const mathQuestions: OfflineQuestion[] = [
  // Algebra — Low
  { subject: 'Mathematics', chapter: 'Algebra', difficulty: 'Low',
    q: 'If x² - 5x + 6 = 0, the roots are:',
    options: ['A) 2, 3', 'B) 1, 6', 'C) -2, -3', 'D) 2, -3'],
    answer: 'A', explanation: 'Factor: (x-2)(x-3) = 0 → x = 2 or x = 3.' },
  { subject: 'Mathematics', chapter: 'Algebra', difficulty: 'Medium',
    q: 'The sum of an infinite geometric series a + ar + ar² + … (|r| < 1) is:',
    options: ['A) a/(1+r)', 'B) a/(1-r)', 'C) a·r/(1-r)', 'D) a/(r-1)'],
    answer: 'B', explanation: 'S∞ = a/(1-r) for |r| < 1.' },
  { subject: 'Mathematics', chapter: 'Algebra', difficulty: 'High',
    q: 'The number of solutions to |x² - 4| = x is:',
    options: ['A) 0', 'B) 1', 'C) 2', 'D) 3'],
    answer: 'C', explanation: 'Case 1: x²-4 = x → x²-x-4=0 → x=(1±√17)/2 (only positive root valid). Case 2: x²-4 = -x → x²+x-4=0 → one positive root. Total = 2 valid solutions.' },

  // Calculus — Medium
  { subject: 'Mathematics', chapter: 'Calculus', difficulty: 'Medium',
    q: 'The derivative of sin(x²) with respect to x is:',
    options: ['A) cos(x²)', 'B) 2x·cos(x²)', 'C) 2x·sin(x²)', 'D) cos(2x)'],
    answer: 'B', explanation: 'Chain rule: d/dx[sin(x²)] = cos(x²) · 2x.' },
  { subject: 'Mathematics', chapter: 'Calculus', difficulty: 'High',
    q: '∫₀^π sin²(x) dx equals:',
    options: ['A) π/4', 'B) π/2', 'C) π', 'D) 2π'],
    answer: 'B', explanation: 'Using sin²x = (1-cos2x)/2: ∫₀^π (1-cos2x)/2 dx = [x/2 - sin2x/4]₀^π = π/2.' },
  { subject: 'Mathematics', chapter: 'Calculus', difficulty: 'Low',
    q: 'The slope of the tangent to y = x³ at x = 2 is:',
    options: ['A) 6', 'B) 8', 'C) 12', 'D) 4'],
    answer: 'C', explanation: 'dy/dx = 3x². At x=2: slope = 3(4) = 12.' },

  // Trigonometry — Low
  { subject: 'Mathematics', chapter: 'Trigonometry', difficulty: 'Low',
    q: 'The value of sin 30° + cos 60° is:',
    options: ['A) 0', 'B) 1', 'C) √3', 'D) 2'],
    answer: 'B', explanation: 'sin 30° = 0.5, cos 60° = 0.5 → sum = 1.' },
  { subject: 'Mathematics', chapter: 'Trigonometry', difficulty: 'Medium',
    q: 'The general solution of sin θ = 1/2 is:',
    options: ['A) θ = nπ + π/6', 'B) θ = nπ ± π/6', 'C) θ = nπ + (-1)ⁿ π/6', 'D) θ = 2nπ ± π/6'],
    answer: 'C', explanation: 'General solution of sin θ = k is θ = nπ + (-1)ⁿ α where sin α = k. Here α = π/6.' },

  // Coordinate Geometry — Medium
  { subject: 'Mathematics', chapter: 'Coordinate Geometry', difficulty: 'Medium',
    q: 'The distance between points (3, 4) and (-1, 1) is:',
    options: ['A) 3', 'B) 4', 'C) 5', 'D) √17'],
    answer: 'C', explanation: 'd = √[(3-(-1))² + (4-1)²] = √[16+9] = √25 = 5.' },
  { subject: 'Mathematics', chapter: 'Coordinate Geometry', difficulty: 'High',
    q: 'The equation of a circle with centre (2, -3) and radius 5 is:',
    options: ['A) (x-2)² + (y+3)² = 5', 'B) (x-2)² + (y+3)² = 25', 'C) (x+2)² + (y-3)² = 25', 'D) x² + y² = 25'],
    answer: 'B', explanation: '(x-h)² + (y-k)² = r². h=2, k=-3, r=5 → (x-2)² + (y+3)² = 25.' },

  // Probability — Medium
  { subject: 'Mathematics', chapter: 'Probability', difficulty: 'Medium',
    q: 'Two dice are thrown. The probability of getting a sum of 7 is:',
    options: ['A) 1/6', 'B) 7/36', 'C) 1/12', 'D) 5/36'],
    answer: 'A', explanation: 'Favourable outcomes: (1,6),(2,5),(3,4),(4,3),(5,2),(6,1) = 6. Total = 36. P = 6/36 = 1/6.' },
  { subject: 'Mathematics', chapter: 'Statistics', difficulty: 'Low',
    q: 'The mean of 5, 10, 15, 20, 25 is:',
    options: ['A) 12', 'B) 13', 'C) 15', 'D) 17'],
    answer: 'C', explanation: 'Mean = (5+10+15+20+25)/5 = 75/5 = 15.' },
];

// ─── Biology ─────────────────────────────────────────────────────────────────

const biologyQuestions: OfflineQuestion[] = [
  // Cell Biology — Low
  { subject: 'Biology', chapter: 'Cell Biology', difficulty: 'Low',
    q: 'The powerhouse of the cell is the:',
    options: ['A) Nucleus', 'B) Ribosome', 'C) Mitochondria', 'D) Golgi apparatus'],
    answer: 'C', explanation: 'Mitochondria produce ATP via cellular respiration — hence called the powerhouse of the cell.' },
  { subject: 'Biology', chapter: 'Cell Biology', difficulty: 'Medium',
    q: 'Which organelle is responsible for protein synthesis?',
    options: ['A) Mitochondria', 'B) Lysosome', 'C) Ribosome', 'D) Vacuole'],
    answer: 'C', explanation: 'Ribosomes translate mRNA into proteins — they are the sites of protein synthesis.' },
  { subject: 'Biology', chapter: 'Cell Biology', difficulty: 'High',
    q: 'The fluid-mosaic model of the cell membrane was proposed by:',
    options: ['A) Watson & Crick', 'B) Singer & Nicolson', 'C) Davson & Danielli', 'D) Schleiden & Schwann'],
    answer: 'B', explanation: 'Singer and Nicolson (1972) proposed the fluid-mosaic model describing the phospholipid bilayer with embedded proteins.' },

  // Genetics — Medium
  { subject: 'Biology', chapter: 'Genetics', difficulty: 'Medium',
    q: 'In a monohybrid cross between Tt × Tt, the ratio of tall to dwarf plants is:',
    options: ['A) 1:1', 'B) 2:1', 'C) 3:1', 'D) 1:2:1'],
    answer: 'C', explanation: 'Tt × Tt → TT : 2Tt : tt = 3 tall : 1 dwarf.' },
  { subject: 'Biology', chapter: 'Genetics', difficulty: 'Low',
    q: 'DNA replication is:',
    options: ['A) Conservative', 'B) Dispersive', 'C) Semi-conservative', 'D) Non-conservative'],
    answer: 'C', explanation: 'Each new DNA double helix retains one original strand — proved by Meselson-Stahl experiment (semi-conservative).' },
  { subject: 'Biology', chapter: 'Genetics', difficulty: 'High',
    q: 'Which enzyme unwinds the DNA double helix during replication?',
    options: ['A) DNA polymerase', 'B) Helicase', 'C) Ligase', 'D) Primase'],
    answer: 'B', explanation: 'Helicase breaks hydrogen bonds between base pairs and unwinds the helix at the replication fork.' },

  // Human Physiology — Medium
  { subject: 'Biology', chapter: 'Human Physiology', difficulty: 'Medium',
    q: 'The normal pH of human blood is approximately:',
    options: ['A) 6.8', 'B) 7.0', 'C) 7.4', 'D) 8.0'],
    answer: 'C', explanation: 'Normal blood pH is maintained at 7.35–7.45, approximately 7.4.' },
  { subject: 'Biology', chapter: 'Human Physiology', difficulty: 'Low',
    q: 'The largest organ of the human body is:',
    options: ['A) Liver', 'B) Lungs', 'C) Skin', 'D) Brain'],
    answer: 'C', explanation: 'Skin is the largest organ by surface area and weight.' },

  // Ecology — Low
  { subject: 'Biology', chapter: 'Ecology', difficulty: 'Low',
    q: 'The primary producers in an ecosystem are:',
    options: ['A) Herbivores', 'B) Carnivores', 'C) Decomposers', 'D) Green plants'],
    answer: 'D', explanation: 'Green plants (autotrophs) fix solar energy via photosynthesis — they are primary producers.' },
  { subject: 'Biology', chapter: 'Ecology', difficulty: 'Medium',
    q: 'Energy transfer efficiency between successive trophic levels is approximately:',
    options: ['A) 1%', 'B) 5%', 'C) 10%', 'D) 20%'],
    answer: 'C', explanation: '10% of energy is transferred to the next trophic level (Lindeman\'s 10% law).' },
];

// ─── English ─────────────────────────────────────────────────────────────────

const englishQuestions: OfflineQuestion[] = [
  { subject: 'English', chapter: 'Grammar', difficulty: 'Low',
    q: 'Choose the correct form: "She ___ to school every day."',
    options: ['A) go', 'B) goes', 'C) going', 'D) gone'],
    answer: 'B', explanation: 'Third person singular (she) takes "goes" in simple present tense.' },
  { subject: 'English', chapter: 'Grammar', difficulty: 'Medium',
    q: 'Identify the figure of speech in: "The wind whispered through the trees."',
    options: ['A) Simile', 'B) Metaphor', 'C) Personification', 'D) Hyperbole'],
    answer: 'C', explanation: 'Attributing human action "whispered" to wind is personification.' },
  { subject: 'English', chapter: 'Vocabulary', difficulty: 'Low',
    q: 'The antonym of "benevolent" is:',
    options: ['A) Kind', 'B) Generous', 'C) Malevolent', 'D) Indifferent'],
    answer: 'C', explanation: 'Benevolent = well-wishing; malevolent = wishing harm — they are antonyms.' },
  { subject: 'English', chapter: 'Vocabulary', difficulty: 'Medium',
    q: 'The word "ephemeral" means:',
    options: ['A) Lasting forever', 'B) Short-lived', 'C) Very large', 'D) Deeply emotional'],
    answer: 'B', explanation: '"Ephemeral" means lasting for a very short time (from Greek ephemeros = lasting a day).' },
  { subject: 'English', chapter: 'Writing Skills', difficulty: 'Medium',
    q: 'A paragraph\'s central idea is best conveyed in the:',
    options: ['A) Concluding sentence', 'B) Supporting sentence', 'C) Topic sentence', 'D) Transitional sentence'],
    answer: 'C', explanation: 'The topic sentence states the main idea of a paragraph, usually placed first.' },
  { subject: 'English', chapter: 'Reading Comprehension', difficulty: 'Low',
    q: '"Inference" in reading comprehension means:',
    options: ['A) Copy text verbatim', 'B) Understand implied meaning', 'C) Summarise the passage', 'D) Identify the author'],
    answer: 'B', explanation: 'Inference is drawing conclusions based on evidence and reasoning from the text.' },
  { subject: 'English', chapter: 'Literature', difficulty: 'Low',
    q: 'Who wrote "Romeo and Juliet"?',
    options: ['A) Charles Dickens', 'B) John Keats', 'C) William Shakespeare', 'D) Mark Twain'],
    answer: 'C', explanation: 'Romeo and Juliet is a tragedy written by William Shakespeare (c. 1594–1596).' },
];

// ─── Social Science ───────────────────────────────────────────────────────────

const socialScienceQuestions: OfflineQuestion[] = [
  { subject: 'Social Science', chapter: 'History', difficulty: 'Low',
    q: 'India gained independence from British rule on:',
    options: ['A) 26 January 1950', 'B) 15 August 1947', 'C) 26 January 1947', 'D) 15 August 1950'],
    answer: 'B', explanation: 'India became independent on 15 August 1947; 26 January 1950 is Republic Day.' },
  { subject: 'Social Science', chapter: 'History', difficulty: 'Medium',
    q: 'The Non-Cooperation Movement was launched by Mahatma Gandhi in:',
    options: ['A) 1915', 'B) 1919', 'C) 1920', 'D) 1930'],
    answer: 'C', explanation: 'Gandhi launched the Non-Cooperation Movement in 1920 in response to the Jallianwala Bagh massacre.' },
  { subject: 'Social Science', chapter: 'Geography', difficulty: 'Low',
    q: 'The Tropic of Cancer passes through which Indian state?',
    options: ['A) Kerala', 'B) Rajasthan', 'C) West Bengal', 'D) Both B and C'],
    answer: 'D', explanation: 'The Tropic of Cancer (23.5°N) passes through Rajasthan, Madhya Pradesh, Jharkhand, West Bengal, and others.' },
  { subject: 'Social Science', chapter: 'Geography', difficulty: 'Medium',
    q: 'The longest river in India is:',
    options: ['A) Brahmaputra', 'B) Godavari', 'C) Ganga', 'D) Indus'],
    answer: 'C', explanation: 'The Ganga is the longest river entirely within India at approximately 2,525 km.' },
  { subject: 'Social Science', chapter: 'Civics', difficulty: 'Low',
    q: 'The Fundamental Rights of Indian citizens are guaranteed by:',
    options: ['A) Part III of the Constitution', 'B) Part IV of the Constitution', 'C) Preamble', 'D) Part V of the Constitution'],
    answer: 'A', explanation: 'Articles 12–35 in Part III of the Indian Constitution enumerate Fundamental Rights.' },
  { subject: 'Social Science', chapter: 'Economics', difficulty: 'Medium',
    q: 'GDP stands for:',
    options: ['A) Gross Domestic Product', 'B) General Development Plan', 'C) Gross Demand for Products', 'D) Government Domestic Policy'],
    answer: 'A', explanation: 'GDP (Gross Domestic Product) is the total monetary value of goods and services produced in a country in a year.' },
  { subject: 'Social Science', chapter: 'Economics', difficulty: 'Low',
    q: 'Which sector employs the majority of India\'s workforce?',
    options: ['A) Secondary (Industry)', 'B) Tertiary (Services)', 'C) Primary (Agriculture)', 'D) Information Technology'],
    answer: 'C', explanation: 'Despite its declining share of GDP, agriculture still employs ~44% of India\'s workforce.' },
];

// ─── Master pool & helpers ────────────────────────────────────────────────────

const ALL_QUESTIONS: OfflineQuestion[] = [
  ...physicsQuestions,
  ...chemistryQuestions,
  ...mathQuestions,
  ...biologyQuestions,
  ...englishQuestions,
  ...socialScienceQuestions,
];

function shuffle<T>(arr: T[]): T[] {
  const copy = [...arr];
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy;
}

export function getOfflineQuestions(
  subject: string,
  chapters: string[],
  difficulty: OfflineDifficulty,
  count: number
): OfflineQuestion[] {
  const pool = ALL_QUESTIONS.filter(
    (q) =>
      q.subject === subject &&
      (chapters.length === 0 || chapters.includes(q.chapter)) &&
      q.difficulty === difficulty
  );

  if (pool.length === 0) {
    // Fallback: relax difficulty filter if no exact match
    const relaxed = ALL_QUESTIONS.filter(
      (q) =>
        q.subject === subject &&
        (chapters.length === 0 || chapters.includes(q.chapter))
    );
    return shuffle(relaxed).slice(0, count);
  }

  return shuffle(pool).slice(0, count);
}

export function getAllSubjectsInPool(): string[] {
  return [...new Set(ALL_QUESTIONS.map((q) => q.subject))];
}
