package com.sigproc.core;

import java.util.Arrays;

public record ComplexBuffer(Complex[] samples, double sampleRate) {

    public int size() {
        return samples.length;
    }

    public ComplexBuffer slice(int from, int to) {
        return new ComplexBuffer(Arrays.copyOfRange(samples, from, to), sampleRate);
    }

    public ComplexBuffer circshift(int shift) {
        int n = samples.length;
        if (n == 0) return this;
        shift = ((shift % n) + n) % n;
        if (shift == 0) return this;
        Complex[] out = new Complex[n];
        System.arraycopy(samples, n - shift, out, 0, shift);
        System.arraycopy(samples, 0, out, shift, n - shift);
        return new ComplexBuffer(out, sampleRate);
    }

    public ComplexBuffer zeroPadTo(int n) {
        if (n <= samples.length) return this;
        Complex[] padded = Arrays.copyOf(samples, n);
        for (int i = samples.length; i < n; i++) padded[i] = Complex.ZERO;
        return new ComplexBuffer(padded, sampleRate);
    }

    public double[] magnitudeSquared() {
        double[] out = new double[samples.length];
        for (int i = 0; i < samples.length; i++)
            out[i] = samples[i].magnitudeSq();
        return out;
    }
}
