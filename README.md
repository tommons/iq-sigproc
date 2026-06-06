# iq-sigproc

A composable Java library for radar / SDR signal processing of IQ data. Processing steps are expressed as typed `SignalBlock` instances that can be chained with a fluent `SignalPipeline` builder.

## Requirements

- Java 17+
- Maven 3.x
- JTransforms 3.1 (fetched automatically by Maven)

## Build & test

```bash
mvn test
```

---

## Project layout

```
src/main/java/com/sigproc/
├── core/
│   ├── Complex.java
│   ├── ComplexBuffer.java
│   ├── RangeDopplerMap.java
│   ├── Peak.java
│   ├── DetectionResult.java
│   ├── NoiseLevel.java
│   ├── SignalBlock.java
│   ├── SignalPipeline.java
│   └── Blocks.java
└── blocks/
    ├── fft/
    │   ├── FFTBlock.java
    │   └── IFFTBlock.java
    ├── window/
    │   └── Window.java
    └── radar/
        ├── ReplicaGenerator.java
        ├── PulseCompressor.java
        ├── DopplerFFTBlock.java
        ├── GlobalNoiseLevelBlock.java
        ├── PeakPickerBlock.java
        └── SNRBlock.java
```

---

## Core types

### `Complex`
Immutable value type (`re`, `im`). Arithmetic: `add`, `subtract`, `multiply`, `conjugate`. Magnitude: `magnitude()`, `magnitudeSq()`. Polar construction: `Complex.fromPolar(r, theta)`.

### `ComplexBuffer`
1-D array of `Complex` samples with a `sampleRate` (Hz). `zeroPadTo(int n)` pads with `Complex.ZERO` for use before convolution.

### `RangeDopplerMap`
2-D complex array `data[rangeBin][dopplerBin]` with `sampleRate` and `prf` metadata.

**Bin conversion helpers:**
```java
int r = rdm.timeToBin(timeDelaySeconds);   // round(τ · sampleRate)
int d = rdm.dopplerToBin(dopplerHz);       // round(fd · numDopplerBins / prf)
```

### `Peak` / `DetectionResult`
`Peak` carries `rangeIndex`, `dopplerIndex`, and `power` (magnitude²). `DetectionResult` wraps a `Peak` with an `snrDb` value.

### `NoiseLevel`
Holds a mean `power` (mean |x|²) and a `dB()` accessor (`10·log10(power)`).

### `SignalBlock<I,O>`
Functional interface: `O process(I input)`. Any lambda or method reference qualifies.

### `SignalPipeline<T>`
Fluent chain builder:
```java
Result r = SignalPipeline.of(input)
    .then(blockA)
    .then(blockB)
    .get();
```

### `Blocks`
Lifts a per-element block to operate over a `List`:
```java
SignalBlock<List<ComplexBuffer>, List<ComplexBuffer>> allCompressed =
    Blocks.mapEach(new PulseCompressor(replica));
```

---

## Windows

`Window` enum — each constant implements `double[] apply(int n)`:

| Constant | Description |
|----------|-------------|
| `RECT` | Rectangular (uniform weights, no sidelobe reduction) |
| `HAMMING` | Hamming — moderate sidelobe suppression (~−43 dB) |
| `HANN` | Hann — smooth roll-off, good general purpose |
| `BLACKMAN` | Blackman — high sidelobe suppression (~−74 dB) |
| `TAYLOR` | Taylor (nbar=4, −30 dB sidelobes) — standard radar choice |

Used by `DopplerFFTBlock` for slow-time windowing. Apply manually to a `ComplexBuffer` before passing to `FFTBlock` for fast-time windowing.

---

## Processing blocks

### `FFTBlock` / `IFFTBlock`
Thin wrappers around JTransforms `DoubleFFT_1D`. Handles arbitrary lengths (no power-of-2 requirement).

**Gain convention:** forward FFT is **unnormalized** (a DC signal of amplitude 1 with N samples produces bin 0 with magnitude N). The inverse FFT divides by N, so `ifft(fft(x)) == x`.

**Static helpers:**
```java
// Frequency (Hz) → expected FFT bin index
int bin = FFTBlock.frequencyToBin(freqHz, fftSize, sampleRateHz);

// Wrap a 1-D spectrum as a single-row RangeDopplerMap for downstream detection
// Sets prf = sampleRate so dopplerToBin(f) == frequencyToBin(f, N, sampleRate)
RangeDopplerMap rdm = FFTBlock.toRangeDopplerMap(spectrum);
```

### `ReplicaGenerator`
Generates a baseband LFM chirp:
```
s(t) = exp(jπ · (B/τ) · t²)   for 0 ≤ t < pulseWidth
```
```java
ComplexBuffer replica = new ReplicaGenerator(bandwidth, pulseWidth, sampleRate).generate();
```

### `PulseCompressor`
Matched filter via frequency-domain multiplication. Uses the **matched-filter convolution** convention — the replica is time-reversed and conjugated before the FFT, so the output peak for a zero-delay target is at index `replicaLen − 1`.

Output length = `inputLen + replicaLen − 1` (linear convolution, no circular aliasing).

```java
ComplexBuffer compressed = new PulseCompressor(replica).process(receivedPulse);
```

### `DopplerFFTBlock`
Stacks N compressed pulses into a range × pulse matrix, applies a slow-time window per range bin, and FFTs along the pulse (slow-time) axis:
```java
RangeDopplerMap rdm = new DopplerFFTBlock(Window.TAYLOR, prf).process(compressedPulses);
```

### `GlobalNoiseLevelBlock`
Mean power across all cells: `power = mean(|data[r][d]|²)`.
```java
NoiseLevel nl = new GlobalNoiseLevelBlock().process(rdm);
double noiseFloorDb = nl.dB();
```

### `PeakPickerBlock`
Finds local maxima (8-connected neighborhood) above an optional dB threshold and returns the top-N peaks sorted by power descending.

```java
new PeakPickerBlock(maxPeaks)
new PeakPickerBlock(maxPeaks, thresholdDb)

// Restrict search to first maxDopplerBins — use N/2 for real-signal half-spectrum
new PeakPickerBlock(maxPeaks, maxDopplerBins)
new PeakPickerBlock(maxPeaks, thresholdDb, maxDopplerBins)
```

When `maxDopplerBins < numDopplerBins`, the Doppler neighbor check is **non-cyclic** at the boundary (prevents wrap-around between the Nyquist bin and DC).

### `SNRBlock`
CA-CFAR SNR estimation along the Doppler axis. For each peak, averages power in training cells on either side (skipping guard cells) and computes `snrDb = 10·log10(signalPower / noisePower)`.
```java
List<DetectionResult> detections = new SNRBlock(rdm, guardCells, trainingCells).process(peaks);
```

---

## Example: full radar pipeline

```java
ReplicaGenerator rg     = new ReplicaGenerator(10e6, 10e-6, 100e6);
ComplexBuffer    replica = rg.generate();

RangeDopplerMap rdm = SignalPipeline
    .of(rawPulses)                                            // List<ComplexBuffer>
    .then(Blocks.mapEach(new PulseCompressor(replica)))       // List<ComplexBuffer>
    .then(new DopplerFFTBlock(Window.TAYLOR, 1000.0))         // RangeDopplerMap
    .get();

List<DetectionResult> detections = new SNRBlock(rdm, 2, 8)
    .process(new PeakPickerBlock(5, 10.0).process(rdm));

// Verify a known target
int expectedRange   = rdm.timeToBin(0.0) + replica.size() - 1;  // zero-delay target
int expectedDoppler = rdm.dopplerToBin(targetDopplerHz);
```

## Example: 1-D spectral detection

```java
ComplexBuffer spectrum = new FFTBlock().process(new ComplexBuffer(samples, sampleRate));
RangeDopplerMap rdm    = FFTBlock.toRangeDopplerMap(spectrum);

// For real-valued input: limit to positive frequencies (first N/2 bins)
List<DetectionResult> detections = new SNRBlock(rdm, 2, 8)
    .process(new PeakPickerBlock(3, 10.0, spectrum.size() / 2).process(rdm));

int expectedBin = FFTBlock.frequencyToBin(toneFreqHz, spectrum.size(), sampleRate);
```
