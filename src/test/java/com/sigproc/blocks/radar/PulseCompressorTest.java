package com.sigproc.blocks.radar;

import com.sigproc.core.Complex;
import com.sigproc.core.ComplexBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PulseCompressorTest {

    @Test
    void selfCompressionProducesPeak() {
        // compress a chirp against itself — peak should be at index replicaLen-1
        ReplicaGenerator gen = new ReplicaGenerator(10e6, 10e-6, 100e6);
        ComplexBuffer replica = gen.generate();

        ComplexBuffer compressed = new PulseCompressor(replica).process(replica);

        int peakIdx = 0;
        double peakPow = 0;
        for (int i = 0; i < compressed.size(); i++) {
            double p = compressed.samples()[i].magnitudeSq();
            if (p > peakPow) { peakPow = p; peakIdx = i; }
        }

        // peak should be at replicaLen - 1 (centre of linear convolution)
        assertEquals(replica.size() - 1, peakIdx, 2);
    }

    @Test
    void outputLengthIsInputPlusReplicaMinusOne() {
        int inputLen   = 200;
        int replicaLen = 50;
        Complex[] inputSamples   = new Complex[inputLen];
        Complex[] replicaSamples = new Complex[replicaLen];
        for (int i = 0; i < inputLen;   i++) inputSamples[i]   = new Complex(1, 0);
        for (int i = 0; i < replicaLen; i++) replicaSamples[i] = new Complex(1, 0);

        ComplexBuffer replica = new ComplexBuffer(replicaSamples, 1.0);
        ComplexBuffer input   = new ComplexBuffer(inputSamples,   1.0);
        ComplexBuffer out     = new PulseCompressor(replica).process(input);

        assertEquals(inputLen + replicaLen - 1, out.size());
    }
}
