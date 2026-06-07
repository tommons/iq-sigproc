package com.sigproc.blocks.radar;

import com.sigproc.core.DetectionResult;
import com.sigproc.core.Peak;
import com.sigproc.core.RangeDopplerMap;
import com.sigproc.core.SignalBlock;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @brief CA-CFAR SNR estimator. Annotates each Peak with an SNR value derived from
 *        training cells on either side along the Doppler axis.
 */
public class SNRBlock implements SignalBlock<List<Peak>, List<DetectionResult>> {

    private final RangeDopplerMap map;
    private final int guardCells;
    private final int trainingCells;

    /**
     * @brief Constructs an SNRBlock bound to the given map.
     * @param map           The RangeDopplerMap used to read training-cell power values.
     * @param guardCells    Number of cells on each side of the peak to exclude from the noise estimate.
     * @param trainingCells Number of cells on each side (beyond the guard band) used to estimate noise.
     */
    public SNRBlock(RangeDopplerMap map, int guardCells, int trainingCells) {
        this.map           = map;
        this.guardCells    = guardCells;
        this.trainingCells = trainingCells;
    }

    /**
     * @brief Estimates SNR for each peak and returns the corresponding DetectionResults.
     * @param peaks List of peaks to annotate.
     * @return List of DetectionResults in the same order as the input peaks.
     */
    @Override
    public List<DetectionResult> process(List<Peak> peaks) {
        return peaks.stream().map(this::detect).collect(Collectors.toList());
    }

    /**
     * @brief Estimates SNR for a single peak using CA-CFAR along the Doppler axis.
     * @param peak The peak to annotate.
     * @return A DetectionResult containing the peak and its snrDb value.
     */
    private DetectionResult detect(Peak peak) {
        double noisePower = estimateNoise(peak.rangeIndex(), peak.dopplerIndex());
        double snrDb = noisePower > 0
                ? 10.0 * Math.log10(peak.power() / noisePower)
                : Double.POSITIVE_INFINITY;
        return new DetectionResult(peak, snrDb);
    }

    /**
     * @brief Estimates the noise power at cell (r, d) using CA-CFAR training cells.
     * @param r Range bin index of the cell under test.
     * @param d Doppler bin index of the cell under test.
     * @return Mean power of the training cells, or 0.0 if no training cells are available.
     */
    private double estimateNoise(int r, int d) {
        int D = map.numDopplerBins();
        double sum   = 0.0;
        int    count = 0;
        int    limit = guardCells + trainingCells;

        for (int offset = guardCells + 1; offset <= limit; offset++) {
            int dLeft  = (d - offset + D) % D;
            int dRight = (d + offset) % D;
            sum += map.magnitudeSq(r, dLeft) + map.magnitudeSq(r, dRight);
            count += 2;
        }

        return count > 0 ? sum / count : 0.0;
    }
}
