package com.sigproc.blocks.radar;

import com.sigproc.core.Complex;
import com.sigproc.core.ComplexBuffer;
import com.sigproc.core.SignalBlock;
import com.sigproc.blocks.fft.FFTBlock;
import org.jtransforms.fft.DoubleFFT_1D;

public class PulseCompressor implements SignalBlock<ComplexBuffer, ComplexBuffer> {

    private final ComplexBuffer replica;

    public PulseCompressor(ComplexBuffer replica) {
        this.replica = replica;
    }

    @Override
    public ComplexBuffer process(ComplexBuffer input) {
        int outLen = input.size() + replica.size() - 1;

        ComplexBuffer paddedInput = input.zeroPadTo(outLen);

        // Time-reverse and conjugate the replica so convolution == matched filtering;
        // this places the output peak at index replicaLen-1 (standard radar convention).
        Complex[] replicaSamples = replica.samples();
        Complex[] revConj = new Complex[outLen];
        for (int i = 0; i < replicaSamples.length; i++)
            revConj[i] = replicaSamples[replicaSamples.length - 1 - i].conjugate();
        for (int i = replicaSamples.length; i < outLen; i++)
            revConj[i] = Complex.ZERO;

        DoubleFFT_1D fft = new DoubleFFT_1D(outLen);

        double[] inBuf  = FFTBlock.toInterleaved(paddedInput.samples());
        double[] refBuf = FFTBlock.toInterleaved(revConj);

        fft.complexForward(inBuf);
        fft.complexForward(refBuf);

        double[] outBuf = new double[outLen * 2];
        for (int k = 0; k < outLen; k++) {
            double inRe  = inBuf[2 * k],     inIm  = inBuf[2 * k + 1];
            double refRe = refBuf[2 * k],    refIm = refBuf[2 * k + 1];
            outBuf[2 * k]     = inRe * refRe - inIm * refIm;
            outBuf[2 * k + 1] = inRe * refIm + inIm * refRe;
        }

        fft.complexInverse(outBuf, true);

        Complex[] result = FFTBlock.fromInterleaved(outBuf);
        return new ComplexBuffer(result, input.sampleRate());
    }
}
