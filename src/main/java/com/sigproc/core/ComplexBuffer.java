package com.sigproc.core;

import java.util.Arrays;

public record ComplexBuffer(Complex[] samples, double sampleRate) {

    public int size() {
        return samples.length;
    }

    public ComplexBuffer zeroPadTo(int n) {
        if (n <= samples.length) return this;
        Complex[] padded = Arrays.copyOf(samples, n);
        for (int i = samples.length; i < n; i++) padded[i] = Complex.ZERO;
        return new ComplexBuffer(padded, sampleRate);
    }
}
