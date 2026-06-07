package com.sigproc.blocks.window;

/**
 * @brief Enumeration of window functions for spectral analysis and sidelobe control.
 *
 * Each constant implements apply(int n) to produce a length-n weight array normalized
 * so that the peak value is 1.0 (for Taylor) or follows the standard formula otherwise.
 */
public enum Window {

    /** @brief Rectangular window — uniform weights, no sidelobe reduction. */
    RECT {
        /**
         * @brief Returns an all-ones weight array.
         * @param n Number of samples.
         * @return double[] of length n filled with 1.0.
         */
        @Override
        public double[] apply(int n) {
            double[] w = new double[n];
            for (int i = 0; i < n; i++) w[i] = 1.0;
            return w;
        }
    },

    /** @brief Hamming window — moderate sidelobe suppression (~−43 dB). */
    HAMMING {
        /**
         * @brief Returns Hamming window coefficients.
         * @param n Number of samples.
         * @return double[] of length n with Hamming weights.
         */
        @Override
        public double[] apply(int n) {
            double[] w = new double[n];
            for (int i = 0; i < n; i++)
                w[i] = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (n - 1));
            return w;
        }
    },

    /** @brief Hann window — smooth roll-off, good general-purpose choice. */
    HANN {
        /**
         * @brief Returns Hann window coefficients.
         * @param n Number of samples.
         * @return double[] of length n with Hann weights.
         */
        @Override
        public double[] apply(int n) {
            double[] w = new double[n];
            for (int i = 0; i < n; i++)
                w[i] = 0.5 * (1 - Math.cos(2 * Math.PI * i / (n - 1)));
            return w;
        }
    },

    /** @brief Blackman window — high sidelobe suppression (~−74 dB). */
    BLACKMAN {
        /**
         * @brief Returns Blackman window coefficients.
         * @param n Number of samples.
         * @return double[] of length n with Blackman weights.
         */
        @Override
        public double[] apply(int n) {
            double[] w = new double[n];
            for (int i = 0; i < n; i++)
                w[i] = 0.42 - 0.5 * Math.cos(2 * Math.PI * i / (n - 1))
                             + 0.08 * Math.cos(4 * Math.PI * i / (n - 1));
            return w;
        }
    },

    /** @brief Taylor window (nbar=4, −30 dB sidelobes) — standard radar weighting. */
    TAYLOR {
        /**
         * @brief Returns Taylor window coefficients normalized so the peak equals 1.
         * @param n Number of samples.
         * @return double[] of length n with Taylor weights.
         */
        @Override
        public double[] apply(int n) {
            int nbar = 4;
            double sll = 30.0;

            double R  = Math.pow(10.0, sll / 20.0);
            double A  = Math.log(R + Math.sqrt(R * R - 1.0)) / Math.PI;
            double s2 = (double)(nbar * nbar) / (A * A + (nbar - 0.5) * (nbar - 0.5));

            double[] F = new double[nbar];
            for (int m = 1; m < nbar; m++) {
                double num = 1.0, den = 1.0;
                for (int p = 1; p < nbar; p++) {
                    double z2 = s2 * (A * A + (p - 0.5) * (p - 0.5));
                    num *= (1.0 - (double)(m * m) / z2);
                    if (p != m) den *= (1.0 - (double)(m * m) / (p * p));
                }
                F[m] = ((m & 1) == 1 ? 1.0 : -1.0) * num / den;
            }

            double[] w = new double[n];
            double peak = 0.0;
            for (int i = 0; i < n; i++) {
                w[i] = 1.0;
                for (int m = 1; m < nbar; m++)
                    w[i] += 2.0 * F[m] * Math.cos(2.0 * Math.PI * m * (i - (n - 1) / 2.0) / n);
                if (w[i] > peak) peak = w[i];
            }
            for (int i = 0; i < n; i++) w[i] /= peak;
            return w;
        }
    };

    /**
     * @brief Computes the window coefficients for the given length.
     * @param n Number of samples.
     * @return double[] of length n containing the window weights.
     */
    public abstract double[] apply(int n);
}
