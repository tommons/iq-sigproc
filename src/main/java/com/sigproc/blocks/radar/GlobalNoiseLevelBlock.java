package com.sigproc.blocks.radar;

import com.sigproc.core.NoiseLevel;
import com.sigproc.core.RangeDopplerMap;
import com.sigproc.core.SignalBlock;

/**
 * @brief Estimates the noise floor as the mean power across all cells of a RangeDopplerMap.
 */
public class GlobalNoiseLevelBlock implements SignalBlock<RangeDopplerMap, NoiseLevel> {

    /**
     * @brief Computes the mean |x|² across all range and Doppler bins.
     * @param map The input RangeDopplerMap.
     * @return A NoiseLevel whose power equals mean(|data[r][d]|²) over all cells.
     */
    @Override
    public NoiseLevel process(RangeDopplerMap map) {
        int R = map.numRangeBins();
        int D = map.numDopplerBins();
        double sum = 0.0;
        for (int r = 0; r < R; r++)
            for (int d = 0; d < D; d++)
                sum += map.magnitudeSq(r, d);
        return new NoiseLevel(sum / (R * D));
    }
}
