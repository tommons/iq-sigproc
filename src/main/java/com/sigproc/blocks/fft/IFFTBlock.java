package com.sigproc.blocks.fft;

import com.sigproc.core.Complex;
import com.sigproc.core.ComplexBuffer;
import com.sigproc.core.SignalBlock;
import org.jtransforms.fft.DoubleFFT_1D;

/**
 * @brief Inverse FFT block. Computes the normalized complex inverse FFT (divides by N).
 *
 * Together with FFTBlock, satisfies ifft(fft(x)) == x to floating-point precision.
 */
public class IFFTBlock implements SignalBlock<ComplexBuffer, ComplexBuffer> {

    /**
     * @brief Computes the normalized inverse FFT of the input spectrum.
     * @param input The frequency-domain ComplexBuffer to transform.
     * @return A ComplexBuffer containing the reconstructed time-domain signal.
     */
    @Override
    public ComplexBuffer process(ComplexBuffer input) {
        int n = input.size();
        double[] interleaved = FFTBlock.toInterleaved(input.samples());
        new DoubleFFT_1D(n).complexInverse(interleaved, true);
        return new ComplexBuffer(FFTBlock.fromInterleaved(interleaved), input.sampleRate());
    }
}
