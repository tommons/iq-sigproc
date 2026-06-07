package com.sigproc.blocks.radar;

import com.sigproc.core.Complex;
import com.sigproc.core.ComplexBuffer;
import com.sigproc.core.Peak;
import com.sigproc.core.RangeDopplerMap;
import com.sigproc.core.SignalBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @brief Detects local maxima in a RangeDopplerMap and returns the top-N peaks sorted by power.
 *
 * A cell is a peak if its power exceeds a threshold and it is greater than all 8-connected
 * neighbors. The Doppler search can optionally be restricted to the first maxDopplerBins bins,
 * which is useful for limiting detection to the positive-frequency half of a real-signal spectrum.
 */
public class PeakPickerBlock implements SignalBlock<RangeDopplerMap, List<Peak>> {

    private final int maxPeaks;
    private final double thresholdPower;
    private final int maxDopplerBins;

    /**
     * @brief Constructs a PeakPickerBlock with no threshold and no Doppler limit.
     * @param maxPeaks Maximum number of peaks to return.
     */
    public PeakPickerBlock(int maxPeaks) {
        this(maxPeaks, Double.NEGATIVE_INFINITY, Integer.MAX_VALUE);
    }

    /**
     * @brief Constructs a PeakPickerBlock with a power threshold and no Doppler limit.
     * @param maxPeaks    Maximum number of peaks to return.
     * @param thresholdDb Minimum peak power in dB; cells below this level are ignored.
     */
    public PeakPickerBlock(int maxPeaks, double thresholdDb) {
        this(maxPeaks, thresholdDb, Integer.MAX_VALUE);
    }

    /**
     * @brief Constructs a PeakPickerBlock with no threshold and a Doppler bin limit.
     * @param maxPeaks      Maximum number of peaks to return.
     * @param maxDopplerBins Number of Doppler bins to search; use numDopplerBins/2 for real signals.
     */
    public PeakPickerBlock(int maxPeaks, int maxDopplerBins) {
        this(maxPeaks, Double.NEGATIVE_INFINITY, maxDopplerBins);
    }

    /**
     * @brief Constructs a PeakPickerBlock with a power threshold and a Doppler bin limit.
     * @param maxPeaks       Maximum number of peaks to return.
     * @param thresholdDb    Minimum peak power in dB; cells below this level are ignored.
     * @param maxDopplerBins Number of Doppler bins to search; use numDopplerBins/2 for real signals.
     */
    public PeakPickerBlock(int maxPeaks, double thresholdDb, int maxDopplerBins) {
        this.maxPeaks       = maxPeaks;
        this.thresholdPower = Math.pow(10.0, thresholdDb / 10.0);
        this.maxDopplerBins = maxDopplerBins;
    }

    /**
     * @brief Detects peaks directly from a ComplexBuffer, treating it as a single-row map.
     * @param buffer The spectrum to search; wrapped internally as a 1-row RangeDopplerMap.
     * @return List of up to maxPeaks peaks sorted by descending power.
     */
    public List<Peak> process(ComplexBuffer buffer) {
        Complex[][] data = new Complex[1][];
        data[0] = buffer.samples().clone();
        return process(new RangeDopplerMap(data, buffer.sampleRate(), buffer.sampleRate()));
    }

    /**
     * @brief Detects peaks in a RangeDopplerMap.
     * @param map The range-Doppler map to search.
     * @return List of up to maxPeaks peaks sorted by descending power.
     */
    @Override
    public List<Peak> process(RangeDopplerMap map) {
        int R    = map.numRangeBins();
        int D    = map.numDopplerBins();
        int Deff = Math.min(D, maxDopplerBins);

        List<Peak> peaks = new ArrayList<>();
        for (int r = 0; r < R; r++) {
            for (int d = 0; d < Deff; d++) {
                double power = map.magnitudeSq(r, d);
                if (power < thresholdPower) continue;
                if (isLocalMax(map, r, d, Deff)) {
                    peaks.add(new Peak(r, d, power));
                }
            }
        }

        peaks.sort(Comparator.comparingDouble(Peak::power).reversed());
        return peaks.size() <= maxPeaks ? peaks : peaks.subList(0, maxPeaks);
    }

    /**
     * @brief Tests whether cell (r, d) is strictly greater than all 8-connected neighbors.
     *
     * When Deff < D the Doppler boundary is non-cyclic, preventing wrap-around between
     * the Nyquist bin and DC in half-spectrum mode.
     *
     * @param map  The range-Doppler map.
     * @param r    Range bin index of the candidate cell.
     * @param d    Doppler bin index of the candidate cell.
     * @param Deff Effective number of Doppler bins being searched.
     * @return true if (r, d) is a local maximum.
     */
    private boolean isLocalMax(RangeDopplerMap map, int r, int d, int Deff) {
        int R = map.numRangeBins();
        int D = map.numDopplerBins();
        double center = map.magnitudeSq(r, d);
        for (int dr = -1; dr <= 1; dr++) {
            for (int dd = -1; dd <= 1; dd++) {
                if (dr == 0 && dd == 0) continue;
                int nr = r + dr;
                if (nr < 0 || nr >= R) continue;
                int nd;
                if (Deff >= D) {
                    nd = (d + dd + D) % D;
                } else {
                    nd = d + dd;
                    if (nd < 0 || nd >= Deff) continue;
                }
                if (map.magnitudeSq(nr, nd) > center) return false;
            }
        }
        return true;
    }
}
