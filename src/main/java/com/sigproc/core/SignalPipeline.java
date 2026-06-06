package com.sigproc.core;

public final class SignalPipeline<T> {

    private final T data;

    private SignalPipeline(T data) {
        this.data = data;
    }

    public static <T> SignalPipeline<T> of(T data) {
        return new SignalPipeline<>(data);
    }

    public <R> SignalPipeline<R> then(SignalBlock<T, R> block) {
        return new SignalPipeline<>(block.process(data));
    }

    public T get() {
        return data;
    }
}
