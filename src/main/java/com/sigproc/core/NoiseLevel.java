package com.sigproc.core;

public record NoiseLevel(double power) {

    public double dB() {
        return 10.0 * Math.log10(power);
    }
}
