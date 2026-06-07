package com.sigproc.blocks.fft;

import com.sigproc.core.Complex;
import com.sigproc.core.ComplexBuffer;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class InterpFTBlockTest {

    @Test
    void outputLengthAndSampleRate() {
        int n = 64; double sr = 1000.0; int factor = 3;
        Complex[] s = new Complex[n];
        Arrays.fill(s, new Complex(1, 0));
        ComplexBuffer out = new InterpFTBlock(factor).process(new ComplexBuffer(s, sr));
        assertEquals(n * factor, out.size());
        assertEquals(sr * factor, out.sampleRate(), 1e-9);
    }

    @Test
    void toneAmplitudePreserved() {
        int n = 128; double sr = 128.0; double freq = 5.0;
        Complex[] s = new Complex[n];
        for (int i = 0; i < n; i++) {
            double p = 2 * Math.PI * freq * i / sr;
            s[i] = new Complex(Math.cos(p), Math.sin(p));
        }
        ComplexBuffer out = new InterpFTBlock(4).process(new ComplexBuffer(s, sr));
        double peak = 0;
        for (Complex c : out.samples()) peak = Math.max(peak, c.magnitude());
        assertEquals(1.0, peak, 1e-6);
    }

    @Test
    void interpolatedSamplesMatchOriginal() {
        int n = 64; double sr = 64.0; double freq = 3.0; int factor = 2;
        Complex[] s = new Complex[n];
        for (int i = 0; i < n; i++) {
            double p = 2 * Math.PI * freq * i / sr;
            s[i] = new Complex(Math.cos(p), Math.sin(p));
        }
        ComplexBuffer out = new InterpFTBlock(factor).process(new ComplexBuffer(s, sr));
        for (int i = 0; i < n; i++) {
            assertEquals(s[i].re(), out.samples()[i * factor].re(), 1e-9, "re at " + i);
            assertEquals(s[i].im(), out.samples()[i * factor].im(), 1e-9, "im at " + i);
        }
    }

    @Test
    void magnitudeSquaredOfUnitTone() {
        int n = 64; double sampleRate = 64.0;
        Complex[] samples = new Complex[n];
        for (int i = 0; i < n; i++) {
            double phase = 2 * Math.PI * 5.0 * i / sampleRate;
            samples[i] = new Complex(Math.cos(phase), Math.sin(phase));
        }
        double[] magSq = new ComplexBuffer(samples, sampleRate).magnitudeSquared();
        assertEquals(n, magSq.length);
        for (int i = 0; i < n; i++)
            assertEquals(1.0, magSq[i], 1e-12, "at " + i);
    }
}
