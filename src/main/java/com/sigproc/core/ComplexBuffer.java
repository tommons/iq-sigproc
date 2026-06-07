package com.sigproc.core;

import java.util.Arrays;

/**
 * @brief A 1-D array of complex samples paired with a sample rate.
 */
public record ComplexBuffer(Complex[] samples, double sampleRate) {

    /**
     * @brief Creates a ComplexBuffer from a real-valued array, setting imaginary parts to zero.
     * @param real       Array of real sample values.
     * @param sampleRate Sample rate in Hz.
     * @return A new ComplexBuffer with im = 0 for every sample.
     */
    public static ComplexBuffer fromReal(double[] real, double sampleRate) {
        Complex[] samples = new Complex[real.length];
        for (int i = 0; i < real.length; i++)
            samples[i] = new Complex(real[i], 0.0);
        return new ComplexBuffer(samples, sampleRate);
    }

    /**
     * @brief Returns the number of samples in the buffer.
     * @return Length of the samples array.
     */
    public int size() {
        return samples.length;
    }

    /**
     * @brief Returns a new buffer containing samples [from, to), preserving sampleRate.
     * @param from Start index (inclusive).
     * @param to   End index (exclusive).
     * @return A new ComplexBuffer with the specified slice of samples.
     */
    public ComplexBuffer slice(int from, int to) {
        return new ComplexBuffer(Arrays.copyOfRange(samples, from, to), sampleRate);
    }

    /**
     * @brief Circularly shifts samples right by the given number of positions.
     * @param shift Number of positions to shift right; negative values shift left.
     * @return A new ComplexBuffer with the shifted samples.
     */
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

    /**
     * @brief Returns a new buffer zero-padded to length n.
     * @param n Target length.
     * @return A new ComplexBuffer of length n, or this if already long enough.
     */
    public ComplexBuffer zeroPadTo(int n) {
        if (n <= samples.length) return this;
        Complex[] padded = Arrays.copyOf(samples, n);
        for (int i = samples.length; i < n; i++) padded[i] = Complex.ZERO;
        return new ComplexBuffer(padded, sampleRate);
    }

    /**
     * @brief Element-wise complex addition with another buffer of equal length.
     * @param other The buffer to add; must be the same length as this buffer.
     * @return A new ComplexBuffer containing the element-wise sums.
     * @throws IllegalArgumentException if the buffer lengths do not match.
     */
    public ComplexBuffer add(ComplexBuffer other) {
        if (other.samples.length != samples.length)
            throw new IllegalArgumentException("buffer lengths must match");
        Complex[] out = new Complex[samples.length];
        for (int i = 0; i < samples.length; i++)
            out[i] = samples[i].add(other.samples[i]);
        return new ComplexBuffer(out, sampleRate);
    }

    /**
     * @brief Element-wise complex multiplication with another buffer of equal length.
     * @param other The buffer to multiply with; must be the same length as this buffer.
     * @return A new ComplexBuffer containing the element-wise products.
     * @throws IllegalArgumentException if the buffer lengths do not match.
     */
    public ComplexBuffer multiply(ComplexBuffer other) {
        if (other.samples.length != samples.length)
            throw new IllegalArgumentException("buffer lengths must match");
        Complex[] out = new Complex[samples.length];
        for (int i = 0; i < samples.length; i++)
            out[i] = samples[i].multiply(other.samples[i]);
        return new ComplexBuffer(out, sampleRate);
    }

    /**
     * @brief Element-wise addition of a real array to this buffer.
     * @param real Real-valued array to add to each sample's real part; must be the same length as this buffer.
     * @return A new ComplexBuffer where each sample has real[i] added to its real part.
     * @throws IllegalArgumentException if the array length does not match.
     */
    public ComplexBuffer add(double[] real) {
        if (real.length != samples.length)
            throw new IllegalArgumentException("array length must match buffer length");
        Complex[] out = new Complex[samples.length];
        for (int i = 0; i < samples.length; i++)
            out[i] = new Complex(samples[i].re() + real[i], samples[i].im());
        return new ComplexBuffer(out, sampleRate);
    }

    /**
     * @brief Element-wise scaling of this buffer by a real array.
     * @param real Real-valued scale factors; must be the same length as this buffer.
     * @return A new ComplexBuffer where each sample is scaled by real[i].
     * @throws IllegalArgumentException if the array length does not match.
     */
    public ComplexBuffer multiply(double[] real) {
        if (real.length != samples.length)
            throw new IllegalArgumentException("array length must match buffer length");
        Complex[] out = new Complex[samples.length];
        for (int i = 0; i < samples.length; i++)
            out[i] = new Complex(samples[i].re() * real[i], samples[i].im() * real[i]);
        return new ComplexBuffer(out, sampleRate);
    }

    /**
     * @brief Returns the magnitude squared of each sample as a real array.
     * @return double[] where element i = |samples[i]|².
     */
    public double[] magnitudeSquared() {
        double[] out = new double[samples.length];
        for (int i = 0; i < samples.length; i++)
            out[i] = samples[i].magnitudeSq();
        return out;
    }
}
