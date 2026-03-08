package com.edutech.assess.domain.service;

import com.edutech.assess.domain.model.Question;
import java.util.List;

/**
 * Item Response Theory (IRT) 3-Parameter Logistic Model (3PL) theta estimator.
 *
 * The 3PL probability that a student with ability theta answers item i correctly:
 *   P(theta) = c_i + (1 - c_i) * [1 / (1 + exp(-1.7 * a_i * (theta - b_i)))]
 *
 * where:
 *   a_i = discrimination parameter (how well item differentiates ability levels)
 *   b_i = difficulty parameter (ability level at which P = 0.5 for c=0)
 *   c_i = guessing parameter (lower asymptote / pseudo-chance level)
 *
 * Theta is estimated via Maximum Likelihood Estimation (MLE) using Newton-Raphson
 * iteration. Falls back to boundary values when MLE is indeterminate (all correct
 * or all incorrect).
 *
 * Pure domain service: no Spring, no framework dependencies.
 */
public class IrtThetaEstimator {

    private static final double D = 1.7;                  // IRT scaling constant
    private static final double THETA_MIN = -4.0;
    private static final double THETA_MAX = 4.0;
    private static final double CONVERGENCE_THRESHOLD = 0.001;
    private static final int MAX_ITERATIONS = 50;

    // Default IRT parameter values when a question has no calibration data
    private static final double DEFAULT_DISCRIMINATION = 1.0;  // a = 1.0 (mid-range)
    private static final double DEFAULT_DIFFICULTY = 0.0;      // b = 0.0 (mean difficulty)
    private static final double DEFAULT_GUESSING = 0.25;       // c = 0.25 (4-option MCQ)

    /**
     * Estimate student ability theta from a list of responses.
     *
     * @param questions  the questions answered
     * @param responses  true = correct, false = incorrect (parallel to questions list)
     * @return estimated theta in range [-4.0, 4.0]
     */
    public double estimateTheta(List<Question> questions, List<Boolean> responses) {
        if (questions.isEmpty()) {
            return 0.0;
        }
        if (allCorrect(responses)) {
            return THETA_MAX;
        }
        if (allIncorrect(responses)) {
            return THETA_MIN;
        }
        double theta = mlEstimate(questions, responses);
        return Math.max(THETA_MIN, Math.min(THETA_MAX, theta));
    }

    /**
     * Select the next best question for Computerized Adaptive Testing (CAT)
     * using Maximum Fisher Information criterion.
     *
     * Fisher information at theta for item i:
     *   I_i(theta) = D^2 * a_i^2 * (P_i - c_i)^2 / ((1 - c_i)^2 * P_i * Q_i)
     *
     * @param candidateQuestions questions not yet administered
     * @param currentTheta       current theta estimate
     * @return the question with maximum Fisher information at currentTheta
     */
    public Question selectNextQuestion(List<Question> candidateQuestions, double currentTheta) {
        Question best = null;
        double bestInfo = Double.NEGATIVE_INFINITY;
        for (Question q : candidateQuestions) {
            double info = fisherInformation(q, currentTheta);
            if (info > bestInfo) {
                bestInfo = info;
                best = q;
            }
        }
        return best;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private double mlEstimate(List<Question> questions, List<Boolean> responses) {
        double theta = 0.0; // start at population mean
        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            double firstDerivative = 0.0;
            double secondDerivative = 0.0;

            for (int i = 0; i < questions.size(); i++) {
                Question q = questions.get(i);
                boolean correct = responses.get(i);
                double a = discriminationOf(q);
                double b = difficultyOf(q);
                double c = guessingOf(q);

                double p = probability(theta, a, b, c);
                double qProb = 1.0 - p;
                // Weight: how much P depends on the logistic component
                double w = (p - c) / (1.0 - c);
                double u = correct ? 1.0 : 0.0;

                // Avoid division by zero near boundary probabilities
                double pq = p * qProb + 1e-10;
                firstDerivative  += D * a * w * (u - p) / pq;
                secondDerivative -= D * D * a * a * w * w * (p * qProb) / pq;
            }

            if (Math.abs(secondDerivative) < 1e-10) {
                break;
            }
            double delta = firstDerivative / secondDerivative;
            theta -= delta;
            theta = Math.max(THETA_MIN, Math.min(THETA_MAX, theta));
            if (Math.abs(delta) < CONVERGENCE_THRESHOLD) {
                break;
            }
        }
        return theta;
    }

    private double probability(double theta, double a, double b, double c) {
        double exponent = -D * a * (theta - b);
        return c + (1.0 - c) / (1.0 + Math.exp(exponent));
    }

    private double fisherInformation(Question q, double theta) {
        double a = discriminationOf(q);
        double b = difficultyOf(q);
        double c = guessingOf(q);

        double p = probability(theta, a, b, c);
        double qProb = 1.0 - p;
        double numerator = D * D * a * a * Math.pow(p - c, 2);
        double denominator = Math.pow(1.0 - c, 2) * p * qProb + 1e-10;
        return numerator / denominator;
    }

    /** Returns the discrimination parameter, with a safe default when zero. */
    private double discriminationOf(Question q) {
        double a = q.getDiscrimination();
        return (a > 0.0) ? a : DEFAULT_DISCRIMINATION;
    }

    /** Returns the difficulty parameter (b-parameter). */
    private double difficultyOf(Question q) {
        return q.getDifficulty();
    }

    /** Returns the guessing parameter, with a safe default when zero. */
    private double guessingOf(Question q) {
        double c = q.getGuessingParam();
        return (c >= 0.0 && c < 1.0) ? c : DEFAULT_GUESSING;
    }

    private boolean allCorrect(List<Boolean> responses) {
        return responses.stream().allMatch(r -> r);
    }

    private boolean allIncorrect(List<Boolean> responses) {
        return responses.stream().noneMatch(r -> r);
    }
}
