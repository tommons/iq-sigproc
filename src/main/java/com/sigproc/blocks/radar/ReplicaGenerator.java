package com.sigproc.blocks.radar;

import com.sigproc.core.Complex;
import com.sigproc.core.ComplexBuffer;

/**
 * @brief Generates a baseband LFM (linear frequency modulated) chirp replica.
 *
 * The waveform is: s(t) = exp(j * π * (bandwidth / pulseWidth) * t²) for 0 ≤ t < pulseWidth.
 */
public class ReplicaGenerator {

    private final double bandwidth;
    private final double pulseWidth;
    private final double sampleRate;

    /**
     * @brief Constructs a ReplicaGenerator with the given waveform parameters.
     * @param bandwidth  LFM sweep bandwidth in Hz.
     * @param pulseWidth Pulse duration in seconds.
     * @param sampleRate Sample rate in Hz.
     */
    public ReplicaGenerator(double bandwidth, double pulseWidth, double sampleRate) {
        this.bandwidth  = bandwidth;
        this.pulseWidth = pulseWidth;
        this.sampleRate = sampleRate;
    }

    /**
     * @brief Generates the LFM chirp replica as a ComplexBuffer.
     * @return A ComplexBuffer of length round(pulseWidth * sampleRate) containing the chirp.
     */
    public ComplexBuffer generate() {
        int n = (int) Math.round(pulseWidth * sampleRate);
        Complex[] samples = new Complex[n];
        double chirpRate = bandwidth / pulseWidth;
        for (int i = 0; i < n; i++) {
            double t = i / sampleRate;
            double phase = Math.PI * chirpRate * t * t + (-bandwidth/2) * t;
            samples[i] = new Complex(Math.cos(phase), Math.sin(phase));
        }
        return new ComplexBuffer(samples, sampleRate);
    }
}
