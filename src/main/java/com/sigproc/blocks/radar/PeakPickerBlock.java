package com.sigproc.blocks.radar;

import com.sigproc.core.Peak;
import com.sigproc.core.RangeDopplerMap;
import com.sigproc.core.SignalBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PeakPickerBlock implements SignalBlock<RangeDopplerMap, List<Peak>> {

    private final int maxPeaks;
    private final double thresholdPower;

    public PeakPickerBlock(int maxPeaks) {
        this(maxPeaks, Double.NEGATIVE_INFINITY);
    }

    // thresholdDb is a power threshold in dB; cells below it are ignored
    public PeakPickerBlock(int maxPeaks, double thresholdDb) {
        this.maxPeaks       = maxPeaks;
        this.thresholdPower = Math.pow(10.0, thresholdDb / 10.0);
    }

    @Override
    public List<Peak> process(RangeDopplerMap map) {
        int R = map.numRangeBins();
        int D = map.numDopplerBins();

        List<Peak> peaks = new ArrayList<>();
        for (int r = 0; r < R; r++) {
            for (int d = 0; d < D; d++) {
                double power = map.magnitudeSq(r, d);
                if (power < thresholdPower) continue;
                if (isLocalMax(map, r, d)) {
                    peaks.add(new Peak(r, d, power));
                }
            }
        }

        peaks.sort(Comparator.comparingDouble(Peak::power).reversed());
        return peaks.size() <= maxPeaks ? peaks : peaks.subList(0, maxPeaks);
    }

    private boolean isLocalMax(RangeDopplerMap map, int r, int d) {
        int R = map.numRangeBins();
        int D = map.numDopplerBins();
        double center = map.magnitudeSq(r, d);
        for (int dr = -1; dr <= 1; dr++) {
            for (int dd = -1; dd <= 1; dd++) {
                if (dr == 0 && dd == 0) continue;
                int nr = r + dr, nd = (d + dd + D) % D;
                if (nr < 0 || nr >= R) continue;
                if (map.magnitudeSq(nr, nd) > center) return false;
            }
        }
        return true;
    }
}
