# Plan: Composable IQ Signal Processing Toolbox (Java/Radar)

## Context
Build a Maven + pure-Java library for radar/SDR signal processing of IQ data.
The user needs a composable pipeline model so processing blocks can be chained
fluently. V1 scope: FFT, pulse compression (with replica generation), Doppler FFT,
peak picking, and SNR computation.

---

## Project layout

```
iq-sigproc/
├── pom.xml
└── src/
    ├── main/java/com/sigproc/
    │   ├── core/
    │   │   ├── Complex.java            value type: re + im, arithmetic ops
    │   │   ├── ComplexBuffer.java      1-D complex array + sampleRate metadata
    │   │   ├── RangeDopplerMap.java    2-D complex array [rangeBins × dopplerBins]
    │   │   ├── Peak.java               range/Doppler bin indices + magnitude
    │   │   ├── DetectionResult.java    Peak + snrDb
    │   │   ├── NoiseLevel.java         power field (mean |x|²) + dB() accessor (10·log10(power))
    │   │   ├── SignalBlock.java        @FunctionalInterface I → O
    │   │   ├── SignalPipeline.java     fluent chain builder
    │   │   └── Blocks.java            static utility: mapEach(block)
    │   └── blocks/
    │       ├── fft/
    │       │   ├── FFTBlock.java       ComplexBuffer → ComplexBuffer (forward, wraps JTransforms)
    │       │   └── IFFTBlock.java      ComplexBuffer → ComplexBuffer (inverse, wraps JTransforms)
    │       ├── window/
    │       │   └── Window.java         enum: RECT, HAMMING, HANN, BLACKMAN + apply()
    │       └── radar/
    │           ├── ReplicaGenerator.java     generates LFM chirp as ComplexBuffer
    │           ├── PulseCompressor.java      matched filter in freq domain
    │           ├── DopplerFFTBlock.java      List<ComplexBuffer> → RangeDopplerMap
    │           ├── GlobalNoiseLevelBlock.java RangeDopplerMap → NoiseLevel
    │           ├── PeakPickerBlock.java      RangeDopplerMap → List<Peak>
    │           └── SNRBlock.java             List<Peak> → List<DetectionResult>
    └── test/java/com/sigproc/
        ├── core/SignalPipelineTest.java
        └── blocks/
            ├── fft/FFTBlockTest.java
            └── radar/
                ├── PulseCompressorTest.java
                ├── DopplerFFTBlockTest.java
                └── RadarPipelineIntegrationTest.java
```

---

## Core types

### `Complex` (record)
```java
public record Complex(double re, double im) {
    public Complex add(Complex o)       { return new Complex(re+o.re, im+o.im); }
    public Complex subtract(Complex o)  { return new Complex(re-o.re, im-o.im); }
    public Complex multiply(Complex o)  { return new Complex(re*o.re - im*o.im, re*o.im + im*o.re); }
    public Complex conjugate()          { return new Complex(re, -im); }
    public double  magnitude()          { return Math.sqrt(re*re + im*im); }
    public double  magnitudeSq()        { return re*re + im*im; }
    public static Complex fromPolar(double r, double theta) { ... }
}
```

### `ComplexBuffer` (record)
```java
public record ComplexBuffer(Complex[] samples, double sampleRate) {
    public int size() { return samples.length; }
    public ComplexBuffer zeroPadTo(int n) { ... }   // convenience for FFT
}
```

### `RangeDopplerMap` (record)
```java
// data[rangeIndex][dopplerIndex]
public record RangeDopplerMap(Complex[][] data, double sampleRate, double prf) {
    public int numRangeBins()   { return data.length; }
    public int numDopplerBins() { return data[0].length; }
    public double magnitude(int r, int d) { return data[r][d].magnitude(); }
}
```

### `SignalBlock` (functional interface)
```java
@FunctionalInterface
public interface SignalBlock<I, O> {
    O process(I input);
}
```

### `SignalPipeline<T>` (fluent builder)
```java
public final class SignalPipeline<T> {
    public static <T> SignalPipeline<T> of(T data) { ... }
    public <R> SignalPipeline<R> then(SignalBlock<T, R> block) { ... }
    public T get() { ... }
}
```

### `Blocks` (utility)
```java
public final class Blocks {
    // Lifts a per-element block to operate over List
    public static <I, O> SignalBlock<List<I>, List<O>> mapEach(SignalBlock<I, O> block) {
        return inputs -> inputs.stream().map(block::process).collect(Collectors.toList());
    }
}
```

---

## Processing blocks

### `FFTBlock` / `IFFTBlock`
Thin wrappers around **JTransforms** `DoubleFFT_1D`. Supports arbitrary input
lengths — no padding required.

Internal flow: `Complex[]` → interleaved `double[]` (re₀, im₀, re₁, im₁, …) →
`DoubleFFT_1D.complexForward` / `complexInverse` → back to `Complex[]`.

```java
// dependency in pom.xml:
// com.github.wendykierp:JTransforms:3.1
DoubleFFT_1D fft = new DoubleFFT_1D(n);
fft.complexForward(interleaved);   // in-place, arbitrary n
fft.complexInverse(interleaved, true); // true = scale by 1/N
```

`ComplexBuffer.zeroPadTo()` is retained as an optional utility (e.g. for
linear-convolution length in pulse compression) but is no longer required by
the FFT blocks themselves.

### `ReplicaGenerator`
Generates a baseband LFM (linear frequency modulated) chirp.
```java
public ReplicaGenerator(double bandwidth, double pulseWidth, double sampleRate)
public ComplexBuffer generate()
// s(t) = exp(j * π * (bandwidth/pulseWidth) * t²)  for 0 ≤ t < pulseWidth
```

### `PulseCompressor` — `SignalBlock<ComplexBuffer, ComplexBuffer>`
Matched filter via frequency-domain multiplication:
1. Zero-pad input and replica to length `inputLen + replicaLen - 1` (linear convolution length — no power-of-2 requirement)
2. FFT both (via JTransforms `DoubleFFT_1D`)
3. Pointwise: `out[k] = input[k] * conj(replica[k])`
4. IFFT → compressed pulse (trim to valid length)

Constructor accepts a pre-computed `ComplexBuffer` replica (or use `ReplicaGenerator`).

### `DopplerFFTBlock` — `SignalBlock<List<ComplexBuffer>, RangeDopplerMap>`
```java
public DopplerFFTBlock(Window slowTimeWindow, double prf)
```
Steps:
1. Stack N compressed pulses into `Complex[numRangeBins][N]`
2. Apply `slowTimeWindow` along slow-time (pulse) axis per range bin
3. FFT each row along slow-time dimension
4. Wrap in `RangeDopplerMap`

### `PeakPickerBlock` — `SignalBlock<RangeDopplerMap, List<Peak>>`
Finds local maxima above an optional dB threshold; returns top-N peaks sorted
by magnitude. Each `Peak` carries `rangeIndex`, `dopplerIndex`, `magnitude`.

```java
public PeakPickerBlock(int maxPeaks)
public PeakPickerBlock(int maxPeaks, double thresholdDb)
```

### `GlobalNoiseLevelBlock` — `SignalBlock<RangeDopplerMap, NoiseLevel>`
Computes mean power across all cells in the map (no sqrt):

```
power = mean( |data[r][d]|² )   over all r, d
      = mean( re² + im² )
```

`NoiseLevel` is a record with a single `double power` field and a `dB()` accessor
(`10 * log10(power)`). No configuration needed — single no-arg constructor.

```java
public GlobalNoiseLevelBlock()
```

### `SNRBlock` — `SignalBlock<List<Peak>, List<DetectionResult>>`
For each peak, estimates noise power from a CFAR-style sliding window on the
magnitude map (configurable guard cells + training cells), then computes
`snrDb = 10 * log10(signalPower / noisePower)`.

```java
public SNRBlock(RangeDopplerMap map, int guardCells, int trainingCells)
```

---

## Example pipeline
```java
ReplicaGenerator rg = new ReplicaGenerator(10e6, 10e-6, 100e6);
ComplexBuffer replica = rg.generate();

List<DetectionResult> detections = SignalPipeline
    .of(rawPulses)                                          // List<ComplexBuffer>
    .then(Blocks.mapEach(new PulseCompressor(replica)))     // List<ComplexBuffer>
    .then(new DopplerFFTBlock(Window.HAMMING, 1000.0))      // RangeDopplerMap
    .then(new PeakPickerBlock(5, 10.0))                     // List<Peak>
    .then(new SNRBlock(rdMap, 2, 8))                        // List<DetectionResult>
    .get();
```

---

## Maven `pom.xml` highlights
- Java 17 (records, sealed types)
- `maven-compiler-plugin` source/target 17
- JUnit 5 (`junit-jupiter`) for tests
- **JTransforms 3.1** (`com.github.wendykierp:JTransforms:3.1`) for FFT — handles arbitrary lengths via mixed-radix / Bluestein

---

## Verification

1. **Unit — FFT round-trip**: `ifft(fft(x)) ≈ x` to floating-point precision.
2. **Unit — tone detection**: single complex tone at bin k → FFT magnitude spike at k.
3. **Unit — pulse compression**: LFM chirp cross-correlated with its own replica → narrow mainlobe, sidelobes below −13 dB.
4. **Unit — Doppler FFT**: N pulses of a stationary target → energy concentrated in bin 0; moving target shifts to correct Doppler bin.
5. **Unit — global noise level**: uniform noise map → `NoiseLevel.power` matches `mean(|x|²)` analytically; `dB()` matches `10·log10(power)` within floating-point tolerance.
6. **Integration — full pipeline**: synthetic scene (point target at known range/Doppler) runs end-to-end; `DetectionResult` reports expected range bin, Doppler bin, and SNR > 0 dB.
7. `mvn test` passes all tests; only external dependency is JTransforms.
