package com.test.administrator.openglvideo.media;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaMuxerWrapper {
    private MediaMuxer mMediaMuxer;
    private int mStartCount = 0;
    private boolean mIsStarted = false;
    public MediaMuxerWrapper(String path) throws IOException{
        mMediaMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        mIsStarted = false;
    }

    public synchronized void start() {
        if (mStartCount == 0) {
            mStartCount = 1;
        } else if (mStartCount == 1) {
            mMediaMuxer.start();
            mIsStarted = true;
        }
    }

    public void stop() {
    }

    public void release() {
    }

    public int addTrack(MediaFormat newFormat) {
        return mMediaMuxer.addTrack(newFormat);
    }

    public void writeSampleData(int trackIndex, ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo) {
        if (mIsStarted) {
            mMediaMuxer.writeSampleData(trackIndex, encodedData, bufferInfo);
        }
    }
}
