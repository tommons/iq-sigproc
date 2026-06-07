package com.sigproc.blocks.window;

/**
 * @brief Functional interface for window functions used in spectral analysis and sidelobe control.
 *
 * Standard windows are provided as static constants. The Taylor window is available both
 * as a default constant (nbar=4, sll=30 dB) and via the taylor() factory for custom parameters.
 */
@FunctionalInterface
public interface Window {

    /**
     * @brief Computes the window coefficients for the given length.
     * @param n Number of samples.
     * @return double[] of length n containing the window weights.
     */
    double[] apply(int n);

    /** @brief Rectangular window — uniform weights, no sidelobe reduction. */
    Window RECT = n -> {
        double[] w = new double[n];
        for (int i = 0; i < n; i++) w[i] = 1.0;
        return w;
    };

    /** @brief Hamming window — moderate sidelobe suppression (~−43 dB). */
    Window HAMMING = n -> {
        double[] w = new double[n];
        double c = 2 * Math.PI / (n - 1);
        for (int i = 0; i < n; i++)
            w[i] = 0.54 - 0.46 * Math.cos(c * i);
        return w;
    };

    /** @brief Hann window — smooth roll-off, good general-purpose choice. */
    Window HANN = n -> {
        double[] w = new double[n];
        double c = 2 * Math.PI / (n - 1);
        for (int i = 0; i < n; i++)
            w[i] = 0.5 * (1 - Math.cos(c * i));
        return w;
    };

    /** @brief Blackman window — high sidelobe suppression (~−74 dB). */
    Window BLACKMAN = n -> {
        double[] w = new double[n];
        double c1 = 2 * Math.PI / (n - 1);
        double c2 = 4 * Math.PI / (n - 1);
        for (int i = 0; i < n; i++)
            w[i] = 0.42 - 0.5 * Math.cos(c1 * i) + 0.08 * Math.cos(c2 * i);
        return w;
    };

    /** @brief Taylor window with default parameters (nbar=4, sll=30 dB). */
    Window TAYLOR = taylor(4, 30.0);

    /**
     * @brief Creates a Taylor window with the specified near-sidelobe parameters.
     * @param nbar Number of nearly-constant-level sidelobes adjacent to the main lobe.
     * @param sll  Peak sidelobe level in dB (positive value, e.g. 35 for −35 dB).
     * @return A Window instance that produces Taylor-weighted coefficients normalized to peak = 1.
     */
    static Window taylor(int nbar, double sll) {
        return n -> {
            double R   = Math.pow(10.0, sll / 20.0);
            double A   = Math.log(R + Math.sqrt(R * R - 1.0)) / Math.PI;
            double A2  = A * A;
            double s2  = (double)(nbar * nbar) / (A2 + (nbar - 0.5) * (nbar - 0.5));

            // Precompute p-dependent terms reused across the m loop
            double[] pHalfSq = new double[nbar];
            double[] pSq     = new double[nbar];
            for (int p = 1; p < nbar; p++) {
                double ph = p - 0.5;
                pHalfSq[p] = ph * ph;
                pSq[p]     = (double)(p * p);
            }

            double[] F = new double[nbar];
            for (int m = 1; m < nbar; m++) {
                double num = 1.0, den = 1.0;
                double m2  = (double)(m * m);
                for (int p = 1; p < nbar; p++) {
                    double z2 = s2 * (A2 + pHalfSq[p]);
                    num *= (1.0 - m2 / z2);
                    if (p != m) den *= (1.0 - m2 / pSq[p]);
                }
                F[m] = ((m & 1) == 1 ? 1.0 : -1.0) * num / den;
            }

            // Precompute per-harmonic angular step and 2*F[m] products
            double twoPiOverN = 2.0 * Math.PI / n;
            double[] freq  = new double[nbar];
            double[] twoFm = new double[nbar];
            for (int m = 1; m < nbar; m++) {
                freq[m]  = twoPiOverN * m;
                twoFm[m] = 2.0 * F[m];
            }

            double half = (n - 1) / 2.0;
            double[] w  = new double[n];
            double peak  = 0.0;
            for (int i = 0; i < n; i++) {
                double iCentered = i - half;
                w[i] = 1.0;
                for (int m = 1; m < nbar; m++)
                    w[i] += twoFm[m] * Math.cos(freq[m] * iCentered);
                if (w[i] > peak) peak = w[i];
            }
            for (int i = 0; i < n; i++) w[i] /= peak;
            return w;
        };
    }
}
