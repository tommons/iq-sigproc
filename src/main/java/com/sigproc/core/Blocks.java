package com.sigproc.core;

import java.util.List;
import java.util.stream.Collectors;

public final class Blocks {

    private Blocks() {}

    public static <I, O> SignalBlock<List<I>, List<O>> mapEach(SignalBlock<I, O> block) {
        return inputs -> inputs.stream().map(block::process).collect(Collectors.toList());
    }
}
