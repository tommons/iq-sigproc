package com.sigproc.blocks.radar;

import com.sigproc.core.Complex;
import com.sigproc.core.ComplexBuffer;

public class ReplicaGenerator {

    private final double bandwidth;
    private final double pulseWidth;
    private final double sampleRate;

    public ReplicaGenerator(double bandwidth, double pulseWidth, double sampleRate) {
        this.bandwidth  = bandwidth;
        this.pulseWidth = pulseWidth;
        this.sampleRate = sampleRate;
    }

    // s(t) = exp(j * π * (bandwidth/pulseWidth) * t²)  for 0 ≤ t < pulseWidth
    public ComplexBuffer generate() {
        int n = (int) Math.round(pulseWidth * sampleRate);
        Complex[] samples = new Complex[n];
        double chirpRate = bandwidth / pulseWidth;
        for (int i = 0; i < n; i++) {
            double t = i / sampleRate;
            double phase = Math.PI * chirpRate * t * t;
            samples[i] = new Complex(Math.cos(phase), Math.sin(phase));
        }
        return new ComplexBuffer(samples, sampleRate);
    }
}
