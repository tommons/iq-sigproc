package com.sigproc.blocks.radar;

import com.sigproc.blocks.fft.FFTBlock;
import com.sigproc.blocks.window.Window;
import com.sigproc.core.Complex;
import com.sigproc.core.ComplexBuffer;
import com.sigproc.core.RangeDopplerMap;
import com.sigproc.core.SignalBlock;
import org.jtransforms.fft.DoubleFFT_1D;

import java.util.List;

/**
 * @brief Stacks pulse-compressed returns into a range-Doppler map via slow-time FFT.
 *
 * For each range bin, applies a slow-time window across pulses then computes a forward
 * FFT along the pulse (Doppler) axis to produce a RangeDopplerMap.
 */
public class DopplerFFTBlock implements SignalBlock<List<ComplexBuffer>, RangeDopplerMap> {

    private final Window window;
    private final double prf;

    /**
     * @brief Constructs a DopplerFFTBlock with the given slow-time window and PRF.
     * @param window The window function applied along the slow-time (pulse) axis.
     * @param prf    Pulse repetition frequency in Hz; stored as the prf field of the output map.
     */
    public DopplerFFTBlock(Window window, double prf) {
        this.window = window;
        this.prf    = prf;
    }

    /**
     * @brief Builds a RangeDopplerMap from a list of range-compressed pulses.
     * @param pulses List of ComplexBuffers, one per pulse; all must have the same length and sample rate.
     * @return A RangeDopplerMap of dimensions [numRangeBins][numPulses].
     * @throws IllegalArgumentException if the pulse list is empty.
     */
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
