package com.sigproc.blocks.radar;

import com.sigproc.blocks.window.Window;
import com.sigproc.core.Complex;
import com.sigproc.core.ComplexBuffer;
import com.sigproc.core.RangeDopplerMap;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DopplerFFTBlockTest {

    @Test
    void stationaryTargetAppearsAtBinZero() {
        int numRangeBins = 32;
        int numPulses    = 64;
        double prf       = 1000.0;
        int targetRange  = 5;

        List<ComplexBuffer> pulses = new ArrayList<>();
        for (int p = 0; p < numPulses; p++) {
            Complex[] samples = new Complex[numRangeBins];
            for (int r = 0; r < numRangeBins; r++)
                samples[r] = r == targetRange ? new Complex(1, 0) : Complex.ZERO;
            pulses.add(new ComplexBuffer(samples, prf));
        }

        RangeDopplerMap rdm = new DopplerFFTBlock(Window.RECT, prf).process(pulses);

        // find Doppler bin with max power at the target range
        int maxDopplerBin = 0;
        double maxPow = 0;
        for (int d = 0; d < rdm.numDopplerBins(); d++) {
            double p = rdm.magnitudeSq(targetRange, d);
            if (p > maxPow) { maxPow = p; maxDopplerBin = d; }
        }
        assertEquals(0, maxDopplerBin);
    }

    @Test
    void movingTargetShiftsDopplerBin() {
        int numRangeBins  = 16;
        int numPulses     = 32;
        double prf        = 1000.0;
        int targetRange   = 3;
        int targetDoppler = 4; // cycles over the CPI

        List<ComplexBuffer> pulses = new ArrayList<>();
        for (int p = 0; p < numPulses; p++) {
            Complex[] samples = new Complex[numRangeBins];
            double phase = 2 * Math.PI * targetDoppler * p / numPulses;
            for (int r = 0; r < numRangeBins; r++)
                samples[r] = r == targetRange
                        ? new Complex(Math.cos(phase), Math.sin(phase))
                        : Complex.ZERO;
            pulses.add(new ComplexBuffer(samples, prf));
        }

        RangeDopplerMap rdm = new DopplerFFTBlock(Window.RECT, prf).process(pulses);

        int maxBin = 0;
        double maxPow = 0;
        for (int d = 0; d < rdm.numDopplerBins(); d++) {
            double p = rdm.magnitudeSq(targetRange, d);
            if (p > maxPow) { maxPow = p; maxBin = d; }
        }
        assertEquals(targetDoppler, maxBin);
    }
}
