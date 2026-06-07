package com.sigproc.core;

/**
 * @brief Immutable complex number with double-precision real and imaginary parts.
 */
public record Complex(double re, double im) {

    /** @brief The zero complex number (0 + 0i). */
    public static final Complex ZERO = new Complex(0, 0);

    /**
     * @brief Adds another complex number to this one.
     * @param o The complex number to add.
     * @return A new Complex equal to this + o.
     */
    public Complex add(Complex o) {
        return new Complex(re + o.re, im + o.im);
    }

    /**
     * @brief Subtracts another complex number from this one.
     * @param o The complex number to subtract.
     * @return A new Complex equal to this - o.
     */
    public Complex subtract(Complex o) {
        return new Complex(re - o.re, im - o.im);
    }

    /**
     * @brief Multiplies this complex number by another.
     * @param o The complex number to multiply by.
     * @return A new Complex equal to this * o.
     */
    public Complex multiply(Complex o) {
        return new Complex(re * o.re - im * o.im, re * o.im + im * o.re);
    }

    /**
     * @brief Returns the complex conjugate of this number.
     * @return A new Complex with the imaginary part negated.
     */
    public Complex conjugate() {
        return new Complex(re, -im);
    }

    /**
     * @brief Returns the magnitude (absolute value) of this complex number.
     * @return sqrt(re² + im²).
     */
    public double magnitude() {
        return Math.sqrt(re * re + im * im);
    }

    /**
     * @brief Returns the squared magnitude of this complex number.
     * @return re² + im².
     */
    public double magnitudeSq() {
        return re * re + im * im;
    }

    /**
     * @brief Constructs a complex number from polar form.
     * @param r     The radius (magnitude).
     * @param theta The angle in radians.
     * @return A new Complex equal to r * exp(j * theta).
     */
    public static Complex fromPolar(double r, double theta) {
        return new Complex(r * Math.cos(theta), r * Math.sin(theta));
    }
}
