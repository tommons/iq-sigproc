package com.sigproc.core;

/**
 * @brief Fluent builder for chaining SignalBlock instances into a processing pipeline.
 *
 * @tparam T The current output type of the pipeline.
 */
public final class SignalPipeline<T> {

    private final T data;

    private SignalPipeline(T data) {
        this.data = data;
    }

    /**
     * @brief Creates a new pipeline with the given initial value.
     * @param data The starting value.
     * @return A new SignalPipeline wrapping data.
     */
    public static <T> SignalPipeline<T> of(T data) {
        return new SignalPipeline<>(data);
    }

    /**
     * @brief Applies a SignalBlock to the current value and returns a new pipeline stage.
     * @param block The block to apply.
     * @return A new SignalPipeline holding the block's output.
     */
    public <R> SignalPipeline<R> then(SignalBlock<T, R> block) {
        return new SignalPipeline<>(block.process(data));
    }

    /**
     * @brief Returns the current value held by the pipeline.
     * @return The processed result.
     */
    public T get() {
        return data;
    }
}
