package com.sigproc.core;

@FunctionalInterface
public interface SignalBlock<I, O> {
    O process(I input);
}
