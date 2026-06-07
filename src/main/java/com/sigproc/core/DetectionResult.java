package com.sigproc.core;

/**
 * @brief A Peak annotated with an SNR estimate.
 *
 * @param peak  The underlying peak (range/Doppler indices and power).
 * @param snrDb Signal-to-noise ratio in dB.
 */
public record DetectionResult(Peak peak, double snrDb) {

    /**
     * @brief Convenience accessor for the range bin index of the underlying peak.
     * @return peak.rangeIndex().
     */
    public int rangeIndex()   { return peak.rangeIndex(); }

    /**
     * @brief Convenience accessor for the Doppler bin index of the underlying peak.
     * @return peak.dopplerIndex().
     */
    public int dopplerIndex() { return peak.dopplerIndex(); }

    /**
     * @brief Convenience accessor for the power of the underlying peak.
     * @return peak.power().
     */
    public double power()     { return peak.power(); }
}
