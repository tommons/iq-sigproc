package com.sigproc.blocks.fft;

import com.sigproc.blocks.window.Window;
import com.sigproc.core.Complex;
import com.sigproc.core.ComplexBuffer;
import com.sigproc.core.RangeDopplerMap;
import com.sigproc.core.SignalBlock;
import org.jtransforms.fft.DoubleFFT_1D;

import java.util.Arrays;

/**
 * @brief Forward FFT block. Applies an optional window then computes a complex forward FFT.
 *
 * Gain convention: the forward transform is unnormalized (DC amplitude = N for a unit
 * input of length N). The inverse (IFFTBlock) divides by N, so ifft(fft(x)) == x.
 */
public class FFTBlock implements SignalBlock<ComplexBuffer, ComplexBuffer> {

    private final Window window;

    /**
     * @brief Constructs an FFTBlock with a rectangular (no-op) window.
     */
    public FFTBlock() {
        this(Window.RECT);
    }

    /**
     * @brief Constructs an FFTBlock that applies the given window before the FFT.
     * @param window The window function to apply to each input block.
     */
    public FFTBlock(Window window) {
        this.window = window;
    }

    /**
     * @brief Applies the window then computes the forward FFT.
     * @param input The time-domain ComplexBuffer to transform.
     * @return A ComplexBuffer containing the complex spectrum.
     */
    @Override
    public ComplexBuffer process(ComplexBuffer input) {
        int n = input.size();
        double[] interleaved = toInterleaved(input.samples());
        double[] win = window.apply(n);
        for (int i = 0; i < n; i++) {
            interleaved[2 * i]     *= win[i];
            interleaved[2 * i + 1] *= win[i];
        }
        new DoubleFFT_1D(n).complexForward(interleaved);
        return new ComplexBuffer(fromInterleaved(interleaved), input.sampleRate());
    }

    /**
     * @brief Converts an array of Complex samples to a JTransforms-compatible interleaved double array.
     * @param samples Array of complex samples.
     * @return double[] of length 2*N with alternating real/imaginary values.
     */
    public static double[] toInterleaved(Complex[] samples) {
        double[] buf = new double[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            buf[2 * i]     = samples[i].re();
            buf[2 * i + 1] = samples[i].im();
        }
        return buf;
    }

    /**
     * @brief Converts a JTransforms interleaved double array back to an array of Complex samples.
     * @param buf Interleaved double array of length 2*N.
     * @return Array of N Complex samples.
     */
    public static Complex[] fromInterleaved(double[] buf) {
        Complex[] out = new Complex[buf.length / 2];
        for (int i = 0; i < out.length; i++)
            out[i] = new Complex(buf[2 * i], buf[2 * i + 1]);
        return out;
    }

    /**
     * @brief Converts a frequency in Hz to the expected FFT bin index.
     * @param freqHz      Frequency in Hz.
     * @param fftSize     Number of FFT points.
     * @param sampleRateHz Sample rate in Hz.
     * @return round(freqHz * fftSize / sampleRateHz).
     */
    public static int frequencyToBin(double freqHz, int fftSize, double sampleRateHz) {
        return (int) Math.round(freqHz * fftSize / sampleRateHz);
    }

    /**
     * @brief Wraps a 1-D spectrum as a single-row RangeDopplerMap for use with detection blocks.
     *
     * Sets prf = sampleRate so that dopplerToBin(f) == frequencyToBin(f, N, sampleRate).
     *
     * @param spectrum The complex spectrum to wrap.
     * @return A RangeDopplerMap with one range bin and N Doppler bins.
     */
    public static RangeDopplerMap toRangeDopplerMap(ComplexBuffer spectrum) {
        Complex[][] data = new Complex[1][];
        data[0] = Arrays.copyOf(spectrum.samples(), spectrum.size());
        return new RangeDopplerMap(data, spectrum.sampleRate(), spectrum.sampleRate());
    }
}
