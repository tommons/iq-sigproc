package com.sigproc.core;

/**
 * @brief Functional interface representing a single processing step that transforms input to output.
 *
 * @tparam I Input type.
 * @tparam O Output type.
 */
@FunctionalInterface
public interface SignalBlock<I, O> {

    /**
     * @brief Processes the input and returns the transformed output.
     * @param input The input data.
     * @return The processed output.
     */
    O process(I input);
}
