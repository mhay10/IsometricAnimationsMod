package com.isoanimations.util;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class BufferPool {
    private static final int poolSize = 250;
    private static final ArrayBlockingQueue<ByteBuffer> pool = new ArrayBlockingQueue<>(poolSize);

    public static void init(int byteSize) {
        pool.clear();
        for (int i = 0; i < poolSize; i++) {
            pool.offer(ByteBuffer.allocateDirect(byteSize));
        }
    }

    public static ByteBuffer tryGetBuffer() {
        return pool.poll();
    }

    public static void returnBuffer(ByteBuffer buffer) {
        buffer.clear();
        pool.offer(buffer);
    }
}
