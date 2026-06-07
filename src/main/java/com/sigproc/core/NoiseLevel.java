package com.sigproc.core;

/**
 * @brief Estimated noise level expressed as mean power.
 *
 * @param power Mean |x|² across the estimation region.
 */
public record NoiseLevel(double power) {

    /**
     * @brief Returns the noise level in dB.
     * @return 10 * log10(power).
     */
    public double dB() {
        return 10.0 * Math.log10(power);
    }
}
