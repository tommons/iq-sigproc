package com.sigproc.blocks.radar;

import com.sigproc.blocks.fft.FFTBlock;
import com.sigproc.blocks.window.Window;
import com.sigproc.core.Complex;
import com.sigproc.core.ComplexBuffer;
import com.sigproc.core.RangeDopplerMap;
import com.sigproc.core.SignalBlock;
import org.jtransforms.fft.DoubleFFT_1D;

import java.util.List;

public class DopplerFFTBlock implements SignalBlock<List<ComplexBuffer>, RangeDopplerMap> {

    private final Window window;
    private final double prf;

    public DopplerFFTBlock(Window window, double prf) {
        this.window = window;
        this.prf    = prf;
    }

    @Override
    public RangeDopplerMap process(List<ComplexBuffer> pulses) {
        if (pulses.isEmpty()) throw new IllegalArgumentException("pulse list is empty");

        int numRangeBins   = pulses.get(0).size();
        int numPulses      = pulses.size();
        double sampleRate  = pulses.get(0).sampleRate();

        double[] win = window.apply(numPulses);

        Complex[][] data = new Complex[numRangeBins][numPulses];
        for (int r = 0; r < numRangeBins; r++) {
            double[] interleaved = new double[numPulses * 2];
            for (int p = 0; p < numPulses; p++) {
                Complex s = pulses.get(p).samples()[r];
                interleaved[2 * p]     = s.re() * win[p];
                interleaved[2 * p + 1] = s.im() * win[p];
            }
            new DoubleFFT_1D(numPulses).complexForward(interleaved);
            Complex[] row = FFTBlock.fromInterleaved(interleaved);
            data[r] = row;
        }

        return new RangeDopplerMap(data, sampleRate, prf);
    }
}
