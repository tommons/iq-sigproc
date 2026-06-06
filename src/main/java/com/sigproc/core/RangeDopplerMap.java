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

    public int timeToBin(double timeDelaySeconds) {
        return (int) Math.round(timeDelaySeconds * sampleRate);
    }

    public int dopplerToBin(double dopplerHz) {
        return (int) Math.round(dopplerHz * numDopplerBins() / prf);
    }
}
