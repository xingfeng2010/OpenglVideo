package com.test.administrator.openglvideo;

import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import com.test.administrator.openglvideo.media.AudioEncoderCore;
import com.test.administrator.openglvideo.media.MediaMuxerWrapper;
import com.test.administrator.openglvideo.media.VideoEncoderCore;
import com.test.administrator.openglvideo.util.MiscUtils;

import java.io.File;
import java.nio.ByteBuffer;

public class VideoEncodeActivity extends Activity implements TextureView.SurfaceTextureListener {
    public static final String TAG = "VideoEncodeActivity";
    private TextureView mTextureView;
    private boolean bTextureAvailabe;
    private VideoActivity.FrameCallback mFrameCallback;
    private static final String PATH = Environment.getExternalStorageDirectory().toString() + "/DCIM/Camera";
    private VideoEncoderCore mVideoEncoderCore;
    private AudioEncoderCore mAudioEncoderCore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.video_layout);
        mTextureView = (TextureView) findViewById(R.id.texture);
        mTextureView.setSurfaceTextureListener(this);
        mFrameCallback = new SpeedControlCallback();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        bTextureAvailabe = true;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private void adjustAspectRatio(int videoWidth, int videoHeight) {
        int viewWidth = mTextureView.getWidth();
        int viewHeight = mTextureView.getHeight();
        double aspectRatio = (double) videoHeight / videoWidth;

        int newWidth, newHeight;
        if (viewHeight > (int) (viewWidth * aspectRatio)) {
            // limited by narrow width; restrict height
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        } else {
            // limited by short height; restrict width
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = viewHeight;
        }
        int xoff = (viewWidth - newWidth) / 2;
        int yoff = (viewHeight - newHeight) / 2;
        Log.v(TAG, "video=" + videoWidth + "x" + videoHeight +
                " view=" + viewWidth + "x" + viewHeight +
                " newView=" + newWidth + "x" + newHeight +
                " off=" + xoff + "," + yoff);

        Matrix txform = new Matrix();
        mTextureView.getTransform(txform);
        txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
        //txform.postRotate(10);          // just for fun
        txform.postTranslate(xoff, yoff);
        mTextureView.setTransform(txform);
    }

    private int selectTrackIndex(MediaExtractor extractor) {
        int index = -1;
        int count = extractor.getTrackCount();
        for (int i = 0; i < count; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                index = i;
                break;
            }
        }

        return index;
    }

    public void startPlay(View view) {
        if (!bTextureAvailabe) {
            Toast.makeText(this, "Texture not Available", Toast.LENGTH_SHORT).show();
            return;
        }

        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        Surface surface = new Surface(texture);
        MediaMuxerWrapper muxer = null;

        try {
            String[] fileds = MiscUtils.getFiles(new File(PATH), "*.mp4");
            String path = new File(PATH, fileds[0]).toString();
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(path);
            int trackIndex = selectTrackIndex(extractor);
            extractor.selectTrack(trackIndex);
            MediaFormat format = extractor.getTrackFormat(trackIndex);
            Log.i(TAG, "start play format :" + format);

            int width = format.getInteger(MediaFormat.KEY_WIDTH);
            int height = format.getInteger(MediaFormat.KEY_HEIGHT);
            adjustAspectRatio(width, height);

            // Create a MediaMuxer.  We can't add the video track and start() the muxer here,
            // because our MediaFormat doesn't have the Magic Goodies.  These can only be
            // obtained from the encoder after it has started processing data.
            //
            // We're not actually interested in multiplexing audio.  We just want to convert
            // the raw H.264 elementary stream we get from MediaCodec into a .mp4 file.
            muxer = new MediaMuxerWrapper(new File(PATH, "test.mp4").toString());
//            muxer = new MediaMuxer(new File(PATH, "test.mp4").toString(),
//                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

//            mVideoEncoderCore = new VideoEncoderCore(1280, 720, 1000000, muxer);
//            mAudioEncoderCore = new AudioEncoderCore(muxer);
        } catch (Exception e) {
            Log.i(TAG, "start play exception :" + e);
        }


        PlayTask task = new PlayTask(surface, new File(PATH), mTextureView, mFrameCallback, muxer);
        new Thread(task, "Movei player").start();
    }

    public static class PlayTask implements Runnable {
        private Surface mVideoSurface;
        private VideoEncoderCore mVideoEncoderCore;
        private AudioEncoderCore mAudioEncoderCore;
        private File mSourceFile;
        private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
        private TextureView textureView;
        private String[] mMovieFiles;
        private String path;
        private VideoActivity.FrameCallback mFrameCallback;
        private MediaExtractor videoExtractor, audioExtractor;
        private MediaCodec videoDecoder, audioDecoder;

        private int videoTrackIndex, audioTrackIndex;

        private MediaMuxerWrapper mMediaMuxer;

        public PlayTask(Surface surface, File filesDir, TextureView view, VideoActivity.FrameCallback frameCallback, MediaMuxerWrapper muxer) {
            mSourceFile = filesDir;
            textureView = view;
            mMovieFiles = MiscUtils.getFiles(mSourceFile, "*.mp4");
            path = new File(filesDir, mMovieFiles[0]).toString();
            mFrameCallback = frameCallback;
            mMediaMuxer = muxer;
        }

        @Override
        public void run() {
            try {
                videoExtractor = new MediaExtractor();
                videoExtractor.setDataSource(path);
                videoTrackIndex = selectTrackIndex(videoExtractor, "video/");
                if (videoTrackIndex < 0) {
                    Log.i(TAG, "can't find video track!");
                }

                videoExtractor.selectTrack(videoTrackIndex);
                MediaFormat videoFromat = videoExtractor.getTrackFormat(videoTrackIndex);
                String videomime = videoFromat.getString(MediaFormat.KEY_MIME);
                mVideoEncoderCore = new VideoEncoderCore(720, 1280, 4000000, mMediaMuxer);
                mVideoSurface = mVideoEncoderCore.getInputSurface();
                videoDecoder = MediaCodec.createDecoderByType(videomime);
                videoDecoder.configure(videoFromat, mVideoSurface, null, 0);
                videoDecoder.start();

                /******************************************************************************************/
                /*************************分割线，上面是video,下面是audio**********************************/
                /*****************************************************************************************/
                audioExtractor = new MediaExtractor();
                audioExtractor.setDataSource(path);
                audioTrackIndex = selectTrackIndex(audioExtractor, "audio/");
                if (audioTrackIndex < 0) {
                    Log.i(TAG, "can't find audio track!");
                }

                audioExtractor.selectTrack(audioTrackIndex);
                MediaFormat audioformat = audioExtractor.getTrackFormat(audioTrackIndex);
                int sample = audioformat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                int channel = audioformat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                String audiomime = audioformat.getString(MediaFormat.KEY_MIME);
                mAudioEncoderCore = new AudioEncoderCore(sample, channel, audiomime, mMediaMuxer);

                audioDecoder = MediaCodec.createDecoderByType(audiomime);
                audioDecoder.configure(audioformat, null, null, 0);
                audioDecoder.start();

                // adjustAspectRatio(format.getInteger(MediaFormat.KEY_WIDTH), format.getInteger(MediaFormat.KEY_HEIGHT));

                boolean outputDone = false;
                while (!outputDone) {
                    doVideoExtract(videoExtractor, videoTrackIndex, videoDecoder);
                    outputDone = doAudioExtract(audioExtractor, audioTrackIndex, audioDecoder);
                }
            } catch (Exception e) {
                Log.i(TAG, "startPlay e:" + e);
            }
        }

        private boolean doVideoExtract(MediaExtractor extractor, int trackIndex, MediaCodec decoder) {
            final int TIMEOUT_USEC = 10000;
            ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
            int inputChunk = 0;
            long firstInputTimeNsec = -1;

            boolean outputDone = false;
            boolean inputDone = false;


            if (!inputDone) {
                int inputbufferIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (inputbufferIndex > 0) {
                    if (firstInputTimeNsec == -1) {
                        firstInputTimeNsec = System.nanoTime();
                    }

                    ByteBuffer inputBuffer = decoderInputBuffers[inputbufferIndex];
                    int chunkSize = extractor.readSampleData(inputBuffer, 0);
                    if (chunkSize < 0) {
                        decoder.queueInputBuffer(inputbufferIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                    } else {
                        long presentationTimeUs = extractor.getSampleTime();
                        decoder.queueInputBuffer(inputbufferIndex, 0, chunkSize, presentationTimeUs, 0);
                        inputChunk++;
                        extractor.advance();
                    }
                } else {
                    Log.i(TAG, "input buffer is not awawable");
                }
            }

            if (!outputDone) {
                int decodeStatus = decoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                if (decodeStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    Log.i(TAG, "no output awaiable decoder !");
                } else if (decodeStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    Log.i(TAG, "decoder output buffer changed!");
                } else if (decodeStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat format = decoder.getOutputFormat();
                    Log.i(TAG, "NEW format info:" + format.toString());
                } else if (decodeStatus < 0) {
                    throw new RuntimeException(
                            "unexpected result from decoder.dequeueOutputBuffer: " +
                                    decodeStatus);
                } else {
                    if (firstInputTimeNsec != 0) {
                        // Log the delay from the first buffer of input to the first buffer
                        // of output.
                        long nowNsec = System.nanoTime();
                        Log.d(TAG, "startup lag " + ((nowNsec - firstInputTimeNsec) / 1000000.0) + " ms");
                        firstInputTimeNsec = 0;
                    }

                    Log.d(TAG, "surface decoder given buffer " + decodeStatus +
                            " (size=" + mBufferInfo.size + ")");

                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true;
                        Log.d(TAG, "output EOS");
                    }

                    boolean doRender = (mBufferInfo.size != 0);

                    decoder.releaseOutputBuffer(decodeStatus, doRender);
                    boolean result = mVideoEncoderCore.drainEncoder(outputDone);
                }
            }

            return outputDone;
        }

        private boolean doAudioExtract(MediaExtractor audioExtractor, int audioTrackIndex, MediaCodec audioDecoder) {
            boolean inputDone = false;
            boolean outputDone = false;
            long firstInputTimeNsec = -1;

            if (!inputDone) {
                int audioInputIndex = audioDecoder.dequeueInputBuffer(10000);
                if (audioInputIndex > 0) {
                    ByteBuffer buffer = audioDecoder.getInputBuffer(audioInputIndex);
                    int chunksize = audioExtractor.readSampleData(buffer, 0);
                    if (chunksize < 0) {
                        audioDecoder.queueInputBuffer(audioInputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                    } else {
                        long presentationTimeUs = audioExtractor.getSampleTime();
                        audioDecoder.queueInputBuffer(audioInputIndex, 0, chunksize, presentationTimeUs, 0);
                        audioExtractor.advance();
                    }
                }
            }

            if (!outputDone) {
                int decodeStatus = audioDecoder.dequeueOutputBuffer(mBufferInfo, 10000);
                if (decodeStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    Log.i(TAG, "no output awaiable decoder !");
                } else if (decodeStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    Log.i(TAG, "decoder output buffer changed!");
                } else if (decodeStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat format = audioDecoder.getOutputFormat();
                    Log.i(TAG, "NEW format info:" + format.toString());
                } else if (decodeStatus < 0) {
                    throw new RuntimeException(
                            "unexpected result from decoder.dequeueOutputBuffer: " +
                                    decodeStatus);
                } else {
                    if (firstInputTimeNsec != 0) {
                        // Log the delay from the first buffer of input to the first buffer
                        // of output.
                        long nowNsec = System.nanoTime();
                        Log.d(TAG, "startup lag " + ((nowNsec - firstInputTimeNsec) / 1000000.0) + " ms");
                        firstInputTimeNsec = 0;
                    }

                    Log.d(TAG, "surface decoder given buffer " + decodeStatus + " (size=" + mBufferInfo.size + ")");

                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true;
                        Log.d(TAG, "output EOS");
                    }

                    ByteBuffer buffer = audioDecoder.getOutputBuffer(decodeStatus);
                    ByteBuffer data = ByteBuffer.allocateDirect(mBufferInfo.size);
                    data.put(buffer);
                    data.flip();
                    // mMediaMuxer.writeSampleData(writevideoTrack,buffer,mBufferInfo);
                    audioDecoder.releaseOutputBuffer(decodeStatus, false);
                    mAudioEncoderCore.encode(data, mBufferInfo.size, audioExtractor.getSampleTime());
                    mAudioEncoderCore.drainEncoder(outputDone);
                }
            }

            return outputDone;
        }

        private int selectTrackIndex(MediaExtractor extractor, String prefix) {
            int index = -1;
            int count = extractor.getTrackCount();
            for (int i = 0; i < count; i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith(prefix)) {
                    index = i;
                    break;
                }
            }

            return index;
        }
    }
}