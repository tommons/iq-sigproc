package com.sigproc.blocks.radar;

import com.sigproc.core.NoiseLevel;
import com.sigproc.core.RangeDopplerMap;
import com.sigproc.core.SignalBlock;

public class GlobalNoiseLevelBlock implements SignalBlock<RangeDopplerMap, NoiseLevel> {

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
