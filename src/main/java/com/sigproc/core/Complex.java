package com.sigproc.core;

public record Complex(double re, double im) {

    public static final Complex ZERO = new Complex(0, 0);

    public Complex add(Complex o) {
        return new Complex(re + o.re, im + o.im);
    }

    public Complex subtract(Complex o) {
        return new Complex(re - o.re, im - o.im);
    }

    public Complex multiply(Complex o) {
        return new Complex(re * o.re - im * o.im, re * o.im + im * o.re);
    }

    public Complex conjugate() {
        return new Complex(re, -im);
    }

    public double magnitude() {
        return Math.sqrt(re * re + im * im);
    }

    public double magnitudeSq() {
        return re * re + im * im;
    }

    public static Complex fromPolar(double r, double theta) {
        return new Complex(r * Math.cos(theta), r * Math.sin(theta));
    }
}
