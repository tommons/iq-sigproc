package com.sigproc.blocks.fft;

import com.sigproc.core.Complex;
import com.sigproc.core.ComplexBuffer;
import com.sigproc.core.SignalBlock;
import org.jtransforms.fft.DoubleFFT_1D;

public class IFFTBlock implements SignalBlock<ComplexBuffer, ComplexBuffer> {

    @Override
    public ComplexBuffer process(ComplexBuffer input) {
        int n = input.size();
        double[] interleaved = FFTBlock.toInterleaved(input.samples());
        new DoubleFFT_1D(n).complexInverse(interleaved, true);
        return new ComplexBuffer(FFTBlock.fromInterleaved(interleaved), input.sampleRate());
    }
}
