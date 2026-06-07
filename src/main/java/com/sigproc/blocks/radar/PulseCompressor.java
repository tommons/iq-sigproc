package com.sigproc.blocks.radar;

import com.sigproc.core.Complex;
import com.sigproc.core.ComplexBuffer;
import com.sigproc.core.SignalBlock;
import com.sigproc.blocks.fft.FFTBlock;
import org.jtransforms.fft.DoubleFFT_1D;

/**
 * @brief Matched-filter pulse compressor implemented via frequency-domain multiplication.
 *
 * The replica is time-reversed and conjugated before the FFT so that the operation is
 * a matched-filter convolution. For a zero-delay point target the output peak appears
 * at index replicaLen - 1 (standard radar convention). Output length is
 * inputLen + replicaLen - 1 (linear convolution, no circular aliasing).
 */
public class PulseCompressor implements SignalBlock<ComplexBuffer, ComplexBuffer> {

    private final ComplexBuffer replica;

    /**
     * @brief Constructs a PulseCompressor with the given reference replica.
     * @param replica The matched-filter replica (e.g. output of ReplicaGenerator).
     */
    public PulseCompressor(ComplexBuffer replica) {
        this.replica = replica;
    }

    /**
     * @brief Applies matched-filter compression to the received pulse.
     * @param input The received pulse ComplexBuffer.
     * @return A ComplexBuffer of length input.size() + replica.size() - 1 with the compressed output.
     */
    @Override
    public ComplexBuffer process(ComplexBuffer input) {
        int outLen = input.size() + replica.size() - 1;

        ComplexBuffer paddedInput = input.zeroPadTo(outLen);

        // Time-reverse and conjugate the replica (matched-filter convolution convention).
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
            int    idx   = 2 * k;
            double inRe  = inBuf[idx],    inIm  = inBuf[idx+1];
            double refRe = refBuf[idx],   refIm = refBuf[idx+1];
            outBuf[idx]   = inRe * refRe - inIm * refIm;
            outBuf[idx+1] = inRe * refIm + inIm * refRe;
        }

        fft.complexInverse(outBuf, true);

        Complex[] result = FFTBlock.fromInterleaved(outBuf);
        return new ComplexBuffer(result, input.sampleRate());
    }
}
