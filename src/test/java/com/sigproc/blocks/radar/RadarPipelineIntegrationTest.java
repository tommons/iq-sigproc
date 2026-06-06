package com.sigproc.blocks.radar;

import com.sigproc.blocks.window.Window;
import com.sigproc.core.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RadarPipelineIntegrationTest {

    @Test
    void globalNoiseLevelMatchesMeanPower() {
        // uniform map: every cell has power = amplitude²
        int R = 8, D = 16;
        double amplitude = 3.0;
        Complex[][] data = new Complex[R][D];
        for (int r = 0; r < R; r++)
            for (int d = 0; d < D; d++)
                data[r][d] = new Complex(amplitude, 0);

        RangeDopplerMap map = new RangeDopplerMap(data, 1.0, 1.0);
        NoiseLevel nl = new GlobalNoiseLevelBlock().process(map);

        assertEquals(amplitude * amplitude, nl.power(), 1e-10);
        assertEquals(10.0 * Math.log10(amplitude * amplitude), nl.dB(), 1e-9);
    }

    @Test
    void fullPipelineDetectsPointTarget() {
        double sampleRate  = 100e6;
        double bandwidth   = 10e6;
        double pulseWidth  = 10e-6;
        double prf         = 1000.0;
        int    numPulses   = 32;
        int    targetDoppler = 3;

        ReplicaGenerator rg = new ReplicaGenerator(bandwidth, pulseWidth, sampleRate);
        ComplexBuffer replica = rg.generate();
        int replicaLen = replica.size();

        // build synthetic pulses: replica at start of each pulse, phase-shifted per Doppler bin
        List<ComplexBuffer> rawPulses = new ArrayList<>();
        for (int p = 0; p < numPulses; p++) {
            double phase = 2 * Math.PI * targetDoppler * p / numPulses;
            Complex phaseFactor = new Complex(Math.cos(phase), Math.sin(phase));
            Complex[] samples = new Complex[replicaLen];
            for (int i = 0; i < replicaLen; i++)
                samples[i] = replica.samples()[i].multiply(phaseFactor);
            rawPulses.add(new ComplexBuffer(samples, sampleRate));
        }

        List<ComplexBuffer> compressed = SignalPipeline
                .of(rawPulses)
                .then(Blocks.mapEach(new PulseCompressor(replica)))
                .get();

        RangeDopplerMap rdm = new DopplerFFTBlock(Window.RECT, prf).process(compressed);

        NoiseLevel noiseLevel = new GlobalNoiseLevelBlock().process(rdm);
        assertTrue(noiseLevel.power() >= 0);

        List<Peak> peaks = new PeakPickerBlock(1).process(rdm);
        assertFalse(peaks.isEmpty());

        List<DetectionResult> detections = new SNRBlock(rdm, 2, 8).process(peaks);
        assertFalse(detections.isEmpty());

        double dopplerFreqHz = targetDoppler * prf / numPulses;

        DetectionResult best = detections.get(0);
        assertEquals(rdm.dopplerToBin(dopplerFreqHz), best.dopplerIndex());
        assertEquals(rdm.timeToBin(0.0) + replicaLen - 1, best.rangeIndex());
        assertTrue(best.snrDb() > 0, "SNR should be positive, got " + best.snrDb());
    }
}
