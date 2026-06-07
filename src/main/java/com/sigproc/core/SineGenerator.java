package com.sigproc.core;

public class SineGenerator {

    private final double frequency;
    private final int    numSamples;
    private final double sampleRate;

    public SineGenerator(double frequency, int numSamples, double sampleRate) {
        this.frequency  = frequency;
        this.numSamples = numSamples;
        this.sampleRate = sampleRate;
    }

    public ComplexBuffer generateReal() {
        Complex[] samples = new Complex[numSamples];
        for (int i = 0; i < numSamples; i++) {
            double phase = 2 * Math.PI * frequency * i / sampleRate;
            samples[i] = new Complex(Math.sin(phase), 0.0);
        }
        return new ComplexBuffer(samples, sampleRate);
    }

    public ComplexBuffer generate() {
        Complex[] samples = new Complex[numSamples];
        for (int i = 0; i < numSamples; i++) {
            double phase = 2 * Math.PI * frequency * i / sampleRate;
            samples[i] = new Complex(Math.cos(phase), Math.sin(phase));
        }
        return new ComplexBuffer(samples, sampleRate);
    }
}
