package com.sigproc.blocks.radar;

import com.sigproc.core.DetectionResult;
import com.sigproc.core.Peak;
import com.sigproc.core.RangeDopplerMap;
import com.sigproc.core.SignalBlock;

import java.util.List;
import java.util.stream.Collectors;

public class SNRBlock implements SignalBlock<List<Peak>, List<DetectionResult>> {

    private final RangeDopplerMap map;
    private final int guardCells;
    private final int trainingCells;

    public SNRBlock(RangeDopplerMap map, int guardCells, int trainingCells) {
        this.map           = map;
        this.guardCells    = guardCells;
        this.trainingCells = trainingCells;
    }

    @Override
    public List<DetectionResult> process(List<Peak> peaks) {
        return peaks.stream().map(this::detect).collect(Collectors.toList());
    }

    private DetectionResult detect(Peak peak) {
        double noisePower = estimateNoise(peak.rangeIndex(), peak.dopplerIndex());
        double snrDb = noisePower > 0
                ? 10.0 * Math.log10(peak.power() / noisePower)
                : Double.POSITIVE_INFINITY;
        return new DetectionResult(peak, snrDb);
    }

    // CA-CFAR along the Doppler axis
    private double estimateNoise(int r, int d) {
        int D = map.numDopplerBins();
        int halfGuard    = guardCells;
        int halfTraining = trainingCells;

        double sum = 0.0;
        int    count = 0;

        for (int offset = halfGuard + 1; offset <= halfGuard + halfTraining; offset++) {
            int dLeft  = (d - offset + D) % D;
            int dRight = (d + offset) % D;
            sum += map.magnitudeSq(r, dLeft) + map.magnitudeSq(r, dRight);
            count += 2;
        }

        return count > 0 ? sum / count : 0.0;
    }
}
