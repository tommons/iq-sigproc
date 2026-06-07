package com.sigproc.core;

/**
 * @brief Generates a sampled sinusoid as a ComplexBuffer.
 */
public class SineGenerator {

    private final double frequency;
    private final int    numSamples;
    private final double sampleRate;

    /**
     * @brief Constructs a SineGenerator with the given parameters.
     * @param frequency   Tone frequency in Hz.
     * @param numSamples  Number of samples to generate.
     * @param sampleRate  Sample rate in Hz.
     */
    public SineGenerator(double frequency, int numSamples, double sampleRate) {
        this.frequency  = frequency;
        this.numSamples = numSamples;
        this.sampleRate = sampleRate;
    }

    /**
     * @brief Generates a complex exponential tone: cos(2πfn/fs) + j*sin(2πfn/fs).
     * @return A ComplexBuffer of length numSamples with unit magnitude at each sample.
     */
    public ComplexBuffer generate() {
        Complex[] samples = new Complex[numSamples];
        for (int i = 0; i < numSamples; i++) {
            double phase = 2 * Math.PI * frequency * i / sampleRate;
            samples[i] = new Complex(Math.cos(phase), Math.sin(phase));
        }
        return new ComplexBuffer(samples, sampleRate);
    }

    /**
     * @brief Generates a real-only sine wave: sin(2πfn/fs), imaginary part = 0.
     * @return A ComplexBuffer of length numSamples with zero imaginary part.
     */
    public ComplexBuffer generateReal() {
        Complex[] samples = new Complex[numSamples];
        for (int i = 0; i < numSamples; i++) {
            double phase = 2 * Math.PI * frequency * i / sampleRate;
            samples[i] = new Complex(Math.sin(phase), 0.0);
        }
        return new ComplexBuffer(samples, sampleRate);
    }
}
