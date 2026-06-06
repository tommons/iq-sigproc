package com.sigproc.blocks.fft;

import com.sigproc.core.Complex;
import com.sigproc.core.ComplexBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FFTBlockTest {

    private static final double TOL = 1e-9;

    @Test
    void roundTrip() {
        int n = 128;
        Complex[] in = new Complex[n];
        for (int i = 0; i < n; i++) in[i] = new Complex(Math.random(), Math.random());
        ComplexBuffer buf = new ComplexBuffer(in, 1.0);

        ComplexBuffer out = new IFFTBlock().process(new FFTBlock().process(buf));

        for (int i = 0; i < n; i++) {
            assertEquals(in[i].re(), out.samples()[i].re(), TOL, "re at " + i);
            assertEquals(in[i].im(), out.samples()[i].im(), TOL, "im at " + i);
        }
    }

    @Test
    void toneAtKnownBin() {
        int n = 256;
        int targetBin = 10;
        double sampleRate = 1.0;
        Complex[] samples = new Complex[n];
        for (int i = 0; i < n; i++) {
            double phase = 2 * Math.PI * targetBin * i / n;
            samples[i] = new Complex(Math.cos(phase), Math.sin(phase));
        }

        ComplexBuffer spectrum = new FFTBlock().process(new ComplexBuffer(samples, sampleRate));

        // find bin with max power
        int maxBin = 0;
        double maxPow = 0;
        for (int k = 0; k < n; k++) {
            double p = spectrum.samples()[k].magnitudeSq();
            if (p > maxPow) { maxPow = p; maxBin = k; }
        }
        assertEquals(targetBin, maxBin);
    }

    @Test
    void arbitraryLength() {
        // JTransforms handles non-power-of-2 sizes
        int n = 100;
        Complex[] in = new Complex[n];
        for (int i = 0; i < n; i++) in[i] = new Complex(1.0, 0.0);
        ComplexBuffer out = new FFTBlock().process(new ComplexBuffer(in, 1.0));
        assertEquals(n, out.size());
        // DC bin should equal n
        assertEquals(n, out.samples()[0].re(), 1e-6);
    }
}
