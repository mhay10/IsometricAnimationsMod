package com.isoanimations.util;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class BufferPool {
    private static final int POOL_SIZE = 10;
    private static final ArrayBlockingQueue<ByteBuffer> pool = new ArrayBlockingQueue<>(POOL_SIZE);
    private static int currentCapacity = 0;

    public static void init(int byteSize) {
        // Only reallocate if window size changed
        if (byteSize == currentCapacity) {
            return;
        }

        // Clear old buffers and allocate new ones
        pool.clear();
        currentCapacity = byteSize;
        for (int i = 0; i < POOL_SIZE; i++) {
            pool.offer(ByteBuffer.allocateDirect(byteSize));
        }
    }

    public static ByteBuffer getBufferBlocking() {
        try {
            return pool.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public static void returnBuffer(ByteBuffer buffer) {
        if (buffer != null && buffer.capacity() == currentCapacity) {
            buffer.clear();
            pool.offer(buffer);
        }
    }
}
