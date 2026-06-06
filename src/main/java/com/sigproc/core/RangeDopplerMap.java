package com.sigproc.core;

public record RangeDopplerMap(Complex[][] data, double sampleRate, double prf) {

    public int numRangeBins() {
        return data.length;
    }

    public int numDopplerBins() {
        return data.length == 0 ? 0 : data[0].length;
    }

    public double magnitudeSq(int r, int d) {
        return data[r][d].magnitudeSq();
    }
}
