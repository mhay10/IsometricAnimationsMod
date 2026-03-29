package com.isoanimations.util;

import java.nio.ByteBuffer;

public class ExportFrame {
    public ByteBuffer frameData;
    public long timestampMicros;

    public ExportFrame(ByteBuffer frameData, long timestampMicros) {
        this.frameData = frameData;
        this.timestampMicros = timestampMicros;
    }
}
