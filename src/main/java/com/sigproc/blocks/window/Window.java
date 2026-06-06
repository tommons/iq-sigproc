package com.sigproc.blocks.window;

public enum Window {

    RECT {
        @Override
        public double[] apply(int n) {
            double[] w = new double[n];
            for (int i = 0; i < n; i++) w[i] = 1.0;
            return w;
        }
    },

    HAMMING {
        @Override
        public double[] apply(int n) {
            double[] w = new double[n];
            for (int i = 0; i < n; i++)
                w[i] = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (n - 1));
            return w;
        }
    },

    HANN {
        @Override
        public double[] apply(int n) {
            double[] w = new double[n];
            for (int i = 0; i < n; i++)
                w[i] = 0.5 * (1 - Math.cos(2 * Math.PI * i / (n - 1)));
            return w;
        }
    },

    BLACKMAN {
        @Override
        public double[] apply(int n) {
            double[] w = new double[n];
            for (int i = 0; i < n; i++)
                w[i] = 0.42 - 0.5 * Math.cos(2 * Math.PI * i / (n - 1))
                             + 0.08 * Math.cos(4 * Math.PI * i / (n - 1));
            return w;
        }
    };

    public abstract double[] apply(int n);
}
