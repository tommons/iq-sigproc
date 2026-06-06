package com.sigproc.blocks.window;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WindowTest {

    @Test
    void taylorWindowLength() {
        int n = 64;
        assertEquals(n, Window.TAYLOR.apply(n).length);
    }

    @Test
    void taylorWindowPeaksAtOne() {
        double[] w = Window.TAYLOR.apply(128);
        double max = 0;
        for (double v : w) if (v > max) max = v;
        assertEquals(1.0, max, 1e-9);
    }

    @Test
    void taylorWindowIsSymmetric() {
        double[] w = Window.TAYLOR.apply(64);
        for (int i = 0; i < w.length / 2; i++)
            assertEquals(w[i], w[w.length - 1 - i], 1e-9, "asymmetry at " + i);
    }
}
