package com.sigproc.core;

/**
 * @brief 2-D complex map indexed by [rangeBin][dopplerBin] with sample rate and PRF metadata.
 */
public record RangeDopplerMap(Complex[][] data, double sampleRate, double prf) {

    /**
     * @brief Returns the number of range bins.
     * @return Number of rows in the data array.
     */
    public int numRangeBins() {
        return data.length;
    }

    /**
     * @brief Returns the number of Doppler bins.
     * @return Number of columns in the data array, or 0 if the map is empty.
     */
    public int numDopplerBins() {
        return data.length == 0 ? 0 : data[0].length;
    }

    /**
     * @brief Returns the magnitude squared of the cell at (r, d).
     * @param r Range bin index.
     * @param d Doppler bin index.
     * @return |data[r][d]|².
     */
    public double magnitudeSq(int r, int d) {
        return data[r][d].magnitudeSq();
    }

    /**
     * @brief Converts a time delay to the nearest range bin index.
     * @param timeDelaySeconds Time delay in seconds.
     * @return round(timeDelaySeconds * sampleRate).
     */
    public int timeToBin(double timeDelaySeconds) {
        return (int) Math.round(timeDelaySeconds * sampleRate);
    }

    /**
     * @brief Converts a Doppler frequency to the nearest Doppler bin index.
     * @param dopplerHz Doppler frequency in Hz.
     * @return round(dopplerHz * numDopplerBins / prf).
     */
    public int dopplerToBin(double dopplerHz) {
        return (int) Math.round(dopplerHz * numDopplerBins() / prf);
    }
}
