package com.sigproc.blocks.radar;

import com.sigproc.core.Complex;
import com.sigproc.core.ComplexBuffer;
import com.sigproc.core.Peak;
import com.sigproc.core.RangeDopplerMap;
import com.sigproc.core.SignalBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PeakPickerBlock implements SignalBlock<RangeDopplerMap, List<Peak>> {

    private final int maxPeaks;
    private final double thresholdPower;
    private final int maxDopplerBins;

    public PeakPickerBlock(int maxPeaks) {
        this(maxPeaks, Double.NEGATIVE_INFINITY, Integer.MAX_VALUE);
    }

    public PeakPickerBlock(int maxPeaks, double thresholdDb) {
        this(maxPeaks, thresholdDb, Integer.MAX_VALUE);
    }

    public PeakPickerBlock(int maxPeaks, int maxDopplerBins) {
        this(maxPeaks, Double.NEGATIVE_INFINITY, maxDopplerBins);
    }

    public PeakPickerBlock(int maxPeaks, double thresholdDb, int maxDopplerBins) {
        this.maxPeaks       = maxPeaks;
        this.thresholdPower = Math.pow(10.0, thresholdDb / 10.0);
        this.maxDopplerBins = maxDopplerBins;
    }

    public List<Peak> process(ComplexBuffer buffer) {
        Complex[][] data = new Complex[1][];
        data[0] = buffer.samples().clone();
        return process(new RangeDopplerMap(data, buffer.sampleRate(), buffer.sampleRate()));
    }

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
