package com.test.administrator.openglvideo.media;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.Surface;

import com.test.administrator.openglvideo.VideoEncodeActivity;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioEncoderCore {
    private static final String TAG = VideoEncodeActivity.TAG;
    private static final boolean VERBOSE = false;

    // TODO: these ought to be configurable as well
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 30;               // 30fps
    private static final int IFRAME_INTERVAL = 5;           // 5 seconds between I-frames

    private Surface mInputSurface;
    private MediaMuxerWrapper mMuxer;
    private MediaCodec mEncoder;
    private MediaCodec.BufferInfo mBufferInfo;
    private int mTrackIndex;
    private boolean mMuxerStarted;


    /**
     * Configures encoder and muxer state, and prepares the input Surface.
     */
    public AudioEncoderCore(int sample, int channel, String audiomime, MediaMuxerWrapper mediaMuxer) throws IOException {
        mBufferInfo = new MediaCodec.BufferInfo();

        MediaFormat audioFormat = MediaFormat.createAudioFormat(audiomime, sample, channel);
        audioFormat.setInteger("aac-profile", MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger("channel-mask", 16);
        audioFormat.setInteger("bitrate", 64 * 1024);
        audioFormat.setInteger("channel-count", 2);
        Log.i(TAG, "AudioEncoderCore format");
        mEncoder = MediaCodec.createEncoderByType("audio/mp4a-latm");
        Log.i(TAG, "AudioEncoderCore audioFormat:" + audioFormat.toString());
        mEncoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        Log.i(TAG, "AudioEncoderCore createInputSurface");
//        mInputSurface = mEncoder.createInputSurface();
        mEncoder.start();
        mMuxer = mediaMuxer;

        mTrackIndex = -1;
        mMuxerStarted = false;
    }

    public void encode(ByteBuffer buffer, int length, long presentationTimeUs) {
        Log.i(TAG,"AudioEncoderCore encode buffer:" + buffer + " length:" + length);
        int inutindex = mEncoder.dequeueInputBuffer(1000);
        Log.i(TAG,"AudioEncoderCore encode inutindex:" + inutindex);
        if (inutindex > 0) {
            ByteBuffer inputBuffer = mEncoder.getInputBuffer(inutindex);
            Log.i(TAG,"AudioEncoderCore encode inputBuffer:" + inputBuffer);
            inputBuffer.clear();
            Log.i(TAG,"AudioEncoderCore encode after clear:");
            if (buffer != null) {
                inputBuffer.put(buffer);
            }

            Log.i(TAG,"AudioEncoderCore encode after put:");

            if (length <= 0) {
                mEncoder.queueInputBuffer(inutindex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            } else {
                mEncoder.queueInputBuffer(inutindex, 0, length, presentationTimeUs, 0);
            }
        }
    }

    /**
     * Releases encoder resources.
     */
    public void release() {
        Log.d(TAG, "releasing encoder objects");
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (mMuxer != null) {
            // TODO: stop() throws an exception if you haven't fed it any data.  Keep track
            //       of frames submitted, and don't call stop() if we haven't written anything.
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
    }

    /**
     * Extracts all pending data from the encoder and forwards it to the muxer.
     * <p>
     * If endOfStream is not set, this returns when there is no more data to drain.  If it
     * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     * Calling this with endOfStream set should be done once, right before stopping the muxer.
     * <p>
     * We're just using the muxer to get a .mp4 file (instead of a raw H.264 stream).  We're
     * not recording audio.
     */
    public void drainEncoder(boolean endOfStream) {
        final int TIMEOUT_USEC = 10000;
        Log.d(TAG, "AudioEncoderCore drainEncoder(" + endOfStream + ")");

        if (endOfStream) {
            Log.d(TAG, "AudioEncoderCore sending EOS to encoder");
            mEncoder.signalEndOfInputStream();
        }

        ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
        while (true) {
            int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    break;      // out of while
                } else {
                    Log.d(TAG, "AudioEncoderCore no output available, spinning to await EOS");
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = mEncoder.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (mMuxerStarted) {
                    throw new RuntimeException("format changed twice");
                }
                MediaFormat newFormat = mEncoder.getOutputFormat();
                Log.d(TAG, "AudioEncoderCore encoder output format changed: " + newFormat);

                // now that we have the Magic Goodies, start the muxer
                mTrackIndex = mMuxer.addTrack(newFormat);
                mMuxer.start();
                mMuxerStarted = true;
            } else if (encoderStatus < 0) {
                Log.w(TAG, "AudioEncoderCore unexpected result from encoder.dequeueOutputBuffer: " +
                        encoderStatus);
                // let's ignore it
            } else {
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                            " was null");
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    Log.d(TAG, "AudioEncoderCore ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    if (!mMuxerStarted) {
                        throw new RuntimeException("muxer hasn't started");
                    }

                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                    mMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                    {
                        Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer, ts=" +
                                mBufferInfo.presentationTimeUs);
                    }
                }

                mEncoder.releaseOutputBuffer(encoderStatus, false);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "AudioEncoderCore reached end of stream unexpectedly");
                    } else {
                        Log.d(TAG, "AudioEncoderCore end of stream reached");
                    }
                    break;      // out of while
                }
            }
        }
    }
}
