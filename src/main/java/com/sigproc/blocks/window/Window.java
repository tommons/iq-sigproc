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
    },

    TAYLOR {
        @Override
        public double[] apply(int n) {
            int nbar = 4;
            double sll = 30.0;

            double R  = Math.pow(10.0, sll / 20.0);
            double A  = Math.log(R + Math.sqrt(R * R - 1.0)) / Math.PI;
            double s2 = (double)(nbar * nbar) / (A * A + (nbar - 0.5) * (nbar - 0.5));

            double[] F = new double[nbar];
            for (int m = 1; m < nbar; m++) {
                double num = 1.0, den = 1.0;
                for (int p = 1; p < nbar; p++) {
                    double z2 = s2 * (A * A + (p - 0.5) * (p - 0.5));
                    num *= (1.0 - (double)(m * m) / z2);
                    if (p != m) den *= (1.0 - (double)(m * m) / (p * p));
                }
                F[m] = ((m & 1) == 1 ? 1.0 : -1.0) * num / den;
            }

            double[] w = new double[n];
            double peak = 0.0;
            for (int i = 0; i < n; i++) {
                w[i] = 1.0;
                for (int m = 1; m < nbar; m++)
                    w[i] += 2.0 * F[m] * Math.cos(2.0 * Math.PI * m * (i - (n - 1) / 2.0) / n);
                if (w[i] > peak) peak = w[i];
            }
            for (int i = 0; i < n; i++) w[i] /= peak;
            return w;
        }
    };

    public abstract double[] apply(int n);
}
