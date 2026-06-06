package com.sigproc.blocks.fft;

import com.sigproc.core.Complex;
import com.sigproc.core.ComplexBuffer;
import com.sigproc.core.SignalBlock;
import org.jtransforms.fft.DoubleFFT_1D;

public class FFTBlock implements SignalBlock<ComplexBuffer, ComplexBuffer> {

    @Override
    public ComplexBuffer process(ComplexBuffer input) {
        int n = input.size();
        double[] interleaved = toInterleaved(input.samples());
        new DoubleFFT_1D(n).complexForward(interleaved);
        return new ComplexBuffer(fromInterleaved(interleaved), input.sampleRate());
    }

    public static double[] toInterleaved(Complex[] samples) {
        double[] buf = new double[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            buf[2 * i]     = samples[i].re();
            buf[2 * i + 1] = samples[i].im();
        }
        return buf;
    }

    public static Complex[] fromInterleaved(double[] buf) {
        Complex[] out = new Complex[buf.length / 2];
        for (int i = 0; i < out.length; i++)
            out[i] = new Complex(buf[2 * i], buf[2 * i + 1]);
        return out;
    }
}
