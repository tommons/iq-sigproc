package com.sigproc.core;

/**
 * @brief A detected local-maximum cell in a RangeDopplerMap.
 *
 * @param rangeIndex   Range bin where the peak was found.
 * @param dopplerIndex Doppler bin where the peak was found.
 * @param power        Magnitude squared of the peak cell.
 */
public record Peak(int rangeIndex, int dopplerIndex, double power) {
}
