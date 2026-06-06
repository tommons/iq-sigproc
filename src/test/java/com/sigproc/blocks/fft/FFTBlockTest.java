package com.sigproc.blocks.fft;

import com.sigproc.blocks.radar.PeakPickerBlock;
import com.sigproc.blocks.radar.SNRBlock;
import com.sigproc.blocks.window.Window;
import com.sigproc.core.Complex;
import com.sigproc.core.ComplexBuffer;
import com.sigproc.core.DetectionResult;
import com.sigproc.core.RangeDopplerMap;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        double sampleRate = 256.0;
        double toneFreq   = 10.0;
        int expectedBin   = FFTBlock.frequencyToBin(toneFreq, n, sampleRate);

        Complex[] samples = new Complex[n];
        for (int i = 0; i < n; i++) {
            double phase = 2 * Math.PI * toneFreq * i / sampleRate;
            samples[i] = new Complex(Math.cos(phase), Math.sin(phase));
        }

        ComplexBuffer spectrum = new FFTBlock().process(new ComplexBuffer(samples, sampleRate));

        int maxBin = 0;
        double maxPow = 0;
        for (int k = 0; k < n; k++) {
            double p = spectrum.samples()[k].magnitudeSq();
            if (p > maxPow) { maxPow = p; maxBin = k; }
        }
        assertEquals(expectedBin, maxBin);
    }

    @Test
    void detectToneInSpectrum() {
        int n = 256;
        double sampleRate = 256.0;
        double toneFreq   = 10.0;
        int expectedBin   = FFTBlock.frequencyToBin(toneFreq, n, sampleRate);

        Complex[] samples = new Complex[n];
        for (int i = 0; i < n; i++) {
            double phase = 2 * Math.PI * toneFreq * i / sampleRate;
            samples[i] = new Complex(Math.cos(phase), Math.sin(phase));
        }

        ComplexBuffer spectrum = new FFTBlock().process(new ComplexBuffer(samples, sampleRate));
        RangeDopplerMap rdm    = FFTBlock.toRangeDopplerMap(spectrum);

        List<DetectionResult> detections = new SNRBlock(rdm, 2, 8)
                .process(new PeakPickerBlock(3).process(rdm));

        assertFalse(detections.isEmpty());
        assertEquals(expectedBin, detections.get(0).dopplerIndex());
        assertTrue(detections.get(0).snrDb() > 0);
    }

    @Test
    void realSignalHalfSpectrumDetection() {
        int n = 256;
        double sampleRate = 256.0;
        double toneFreq   = 10.0;
        int expectedBin   = FFTBlock.frequencyToBin(toneFreq, n, sampleRate);

        // real-only signal → double-sided spectrum (peaks at bin 10 AND bin 246)
        Complex[] samples = new Complex[n];
        for (int i = 0; i < n; i++) {
            double phase = 2 * Math.PI * toneFreq * i / sampleRate;
            samples[i] = new Complex(Math.cos(phase), 0.0);
        }

        ComplexBuffer spectrum = new FFTBlock().process(new ComplexBuffer(samples, sampleRate));
        RangeDopplerMap rdm    = FFTBlock.toRangeDopplerMap(spectrum);

        List<DetectionResult> detections = new SNRBlock(rdm, 2, 8)
                .process(new PeakPickerBlock(3, 10.0, n / 2).process(rdm));

        assertEquals(1, detections.size());
        assertEquals(expectedBin, detections.get(0).dopplerIndex());
    }

    @Test
    void windowingPreservesToneBin() {
        int n = 256;
        double sampleRate = 256.0;
        double toneFreq   = 10.0;
        int expectedBin   = FFTBlock.frequencyToBin(toneFreq, n, sampleRate);

        Complex[] samples = new Complex[n];
        for (int i = 0; i < n; i++) {
            double phase = 2 * Math.PI * toneFreq * i / sampleRate;
            samples[i] = new Complex(Math.cos(phase), Math.sin(phase));
        }
        ComplexBuffer buf = new ComplexBuffer(samples, sampleRate);

        ComplexBuffer rectSpectrum    = new FFTBlock().process(buf);
        ComplexBuffer hammingSpectrum = new FFTBlock(Window.HAMMING).process(buf);

        // peak bin is unchanged by windowing
        int maxBin = 0; double maxPow = 0;
        for (int k = 0; k < n; k++) {
            double p = hammingSpectrum.samples()[k].magnitudeSq();
            if (p > maxPow) { maxPow = p; maxBin = k; }
        }
        assertEquals(expectedBin, maxBin);

        // window was actually applied — output differs from RECT
        assertNotEquals(
            rectSpectrum.samples()[expectedBin].re(),
            hammingSpectrum.samples()[expectedBin].re()
        );
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
