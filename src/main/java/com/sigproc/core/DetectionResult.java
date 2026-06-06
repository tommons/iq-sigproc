package com.sigproc.core;

public record DetectionResult(Peak peak, double snrDb) {

    public int rangeIndex()   { return peak.rangeIndex(); }
    public int dopplerIndex() { return peak.dopplerIndex(); }
    public double power()     { return peak.power(); }
}
