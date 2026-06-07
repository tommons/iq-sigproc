package com.sigproc.core;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @brief Utility class providing higher-order SignalBlock combinators.
 */
public final class Blocks {

    private Blocks() {}

    /**
     * @brief Lifts a per-element block to operate over a List.
     * @param block A SignalBlock applied to each element individually.
     * @return A SignalBlock that maps block over every element of a List.
     */
    public static <I, O> SignalBlock<List<I>, List<O>> mapEach(SignalBlock<I, O> block) {
        return inputs -> inputs.stream().map(block::process).collect(Collectors.toList());
    }
}
