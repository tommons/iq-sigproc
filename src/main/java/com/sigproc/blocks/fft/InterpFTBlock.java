package com.sigproc.blocks.fft;

import com.sigproc.core.Complex;
import com.sigproc.core.ComplexBuffer;
import com.sigproc.core.SignalBlock;
import org.jtransforms.fft.DoubleFFT_1D;

/**
 * @brief FFT-based upsampling by an integer factor (interpft).
 *
 * Computes a forward FFT of the input, zero-pads the spectrum in the middle to expand
 * it by the factor, then performs an inverse FFT. The Nyquist bin is split when the
 * input length is even to avoid aliasing. Amplitude is preserved: a unit-magnitude
 * tone in the input produces a unit-magnitude tone in the output.
 */
public class InterpFTBlock implements SignalBlock<ComplexBuffer, ComplexBuffer> {

    private final int factor;

    /**
     * @brief Constructs an InterpFTBlock with the given upsampling factor.
     * @param factor Integer upsampling factor; must be >= 1.
     * @throws IllegalArgumentException if factor < 1.
     */
    public InterpFTBlock(int factor) {
        if (factor < 1) throw new IllegalArgumentException("factor must be >= 1");
        this.factor = factor;
    }

    /**
     * @brief Upsamples the input by the configured factor.
     * @param input The band-limited input signal to upsample.
     * @return A ComplexBuffer of length input.size() * factor at factor * input.sampleRate().
     */
    @Override
    public ComplexBuffer process(ComplexBuffer input) {
        int n    = input.size();
        int nOut = n * factor;

        double[] spectrum = FFTBlock.toInterleaved(input.samples());
        new DoubleFFT_1D(n).complexForward(spectrum);

        double[] padded  = new double[nOut * 2];  // zero-initialised

        int posCount = n / 2;  // DC + positive-freq bins before Nyquist

        // DC and positive frequencies
        System.arraycopy(spectrum, 0, padded, 0, posCount * 2);

        if (n % 2 == 0) {
            // Split Nyquist bin between top of positive and bottom of negative halves
            double nyRe = spectrum[posCount * 2]     / 2.0;
            double nyIm = spectrum[posCount * 2 + 1] / 2.0;
            padded[posCount * 2]              = nyRe;
            padded[posCount * 2 + 1]          = nyIm;
            padded[(nOut - posCount) * 2]     = nyRe;
            padded[(nOut - posCount) * 2 + 1] = nyIm;
            int negStart = posCount + 1;
            int negCount = n - negStart;
            System.arraycopy(spectrum, negStart * 2, padded, (nOut - negCount) * 2, negCount * 2);
        } else {
            // Odd N: no Nyquist bin — pack negative freqs at end
            int negStart = posCount + 1;
            int negCount = n - negStart;
            System.arraycopy(spectrum, negStart * 2, padded, (nOut - negCount) * 2, negCount * 2);
        }

        new DoubleFFT_1D(nOut).complexInverse(padded, true);

        Complex[] result = FFTBlock.fromInterleaved(padded);
        for (int i = 0; i < nOut; i++)
            result[i] = new Complex(result[i].re() * factor, result[i].im() * factor);

        return new ComplexBuffer(result, input.sampleRate() * factor);
    }
}
