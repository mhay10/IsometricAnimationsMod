package com.isoanimations.util;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class BufferPool {
    private static final int POOL_SIZE = 250;
    private static final ArrayBlockingQueue<ByteBuffer> pool = new ArrayBlockingQueue<>(POOL_SIZE);

    public static void init(int byteSize) {
        pool.clear();
        for (int i = 0; i < POOL_SIZE; i++) {
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
