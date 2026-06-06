package com.sigproc.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SignalPipelineTest {

    @Test
    void ofReturnsInitialValue() {
        assertEquals(42, SignalPipeline.of(42).get());
    }

    @Test
    void thenAppliesBlock() {
        int result = SignalPipeline.of(3)
                .then(x -> x * 2)
                .then(x -> x + 1)
                .get();
        assertEquals(7, result);
    }

    @Test
    void mapEachLiftsBlock() {
        List<Integer> result = SignalPipeline.of(List.of(1, 2, 3))
                .then(Blocks.mapEach(x -> x * 10))
                .get();
        assertEquals(List.of(10, 20, 30), result);
    }
}
