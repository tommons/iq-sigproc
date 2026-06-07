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
        for (int i = 0; i < n; i++)
            w[i] = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (n - 1));
        return w;
    };

    /** @brief Hann window — smooth roll-off, good general-purpose choice. */
    Window HANN = n -> {
        double[] w = new double[n];
        for (int i = 0; i < n; i++)
            w[i] = 0.5 * (1 - Math.cos(2 * Math.PI * i / (n - 1)));
        return w;
    };

    /** @brief Blackman window — high sidelobe suppression (~−74 dB). */
    Window BLACKMAN = n -> {
        double[] w = new double[n];
        for (int i = 0; i < n; i++)
            w[i] = 0.42 - 0.5  * Math.cos(2 * Math.PI * i / (n - 1))
                        + 0.08 * Math.cos(4 * Math.PI * i / (n - 1));
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
        };
    }
}
