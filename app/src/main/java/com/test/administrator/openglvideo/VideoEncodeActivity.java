package com.test.administrator.openglvideo;

import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import com.test.administrator.openglvideo.media.VideoEncoderCore;
import com.test.administrator.openglvideo.util.MiscUtils;

import java.io.File;
import java.nio.ByteBuffer;

public class VideoEncodeActivity extends Activity implements TextureView.SurfaceTextureListener{
    public static final String TAG = "VideoEncodeActivity";
    private TextureView mTextureView;
    private boolean bTextureAvailabe;
    private VideoActivity.FrameCallback mFrameCallback;
    private static final String PATH = Environment.getExternalStorageDirectory().toString() + "/DCIM/Camera";
    private VideoEncoderCore mVideoEncoderCore;

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
            Toast.makeText(this,"Texture not Available", Toast.LENGTH_SHORT).show();
            return;
        }

        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        Surface surface = new Surface(texture);

        try {
            String[] fileds = MiscUtils.getFiles(new File(PATH), "*.mp4");
            String path = new File(PATH, fileds[0]).toString();
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(path);
            int trackIndex = selectTrackIndex(extractor);
            extractor.selectTrack(trackIndex);
            MediaFormat format = extractor.getTrackFormat(trackIndex);
            Log.i(TAG,"start play format :" + format);

            int width = format.getInteger(MediaFormat.KEY_WIDTH);
            int height = format.getInteger(MediaFormat.KEY_HEIGHT);
            adjustAspectRatio(width, height);

            mVideoEncoderCore = new VideoEncoderCore(1280, 720, 1000000, new File(PATH, "test.mp4"));
        } catch (Exception e) {
          Log.i(TAG,"start play exception :" + e);
        }


        PlayTask task = new PlayTask(surface,new File(PATH),mTextureView, mFrameCallback, mVideoEncoderCore);
        new Thread(task, "Movei player").start();
    }

    public static class PlayTask implements Runnable {
        private Surface mSurface;
        private File mSourceFile;
        private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
        private TextureView textureView;
        private String[] mMovieFiles;
        private String path;
        private VideoActivity.FrameCallback mFrameCallback;
        private VideoEncoderCore videoEncoder;

        public PlayTask(Surface surface, File filesDir, TextureView view, VideoActivity.FrameCallback frameCallback, VideoEncoderCore encode) {
            mSurface = encode.getInputSurface();
            mSourceFile = filesDir;
            textureView = view;
            mMovieFiles = MiscUtils.getFiles(mSourceFile, "*.mp4");
            path = new File(filesDir, mMovieFiles[0]).toString();
            mFrameCallback = frameCallback;
            videoEncoder = encode;
        }

        @Override
        public void run() {
            try {
                MediaExtractor extractor = new MediaExtractor();
                extractor.setDataSource(path);
                int trackIndex = selectTrackIndex(extractor);
                if (trackIndex < 0) {
                    Log.i(TAG,"can't find video track!");
                }

                extractor.selectTrack(trackIndex);
                MediaFormat format = extractor.getTrackFormat(trackIndex);
                String mime = format.getString(MediaFormat.KEY_MIME);
                MediaCodec decoder = MediaCodec.createDecoderByType(mime);
                decoder.configure(format, mSurface, null, 0);
                decoder.start();

                // adjustAspectRatio(format.getInteger(MediaFormat.KEY_WIDTH), format.getInteger(MediaFormat.KEY_HEIGHT));

                doExtract(extractor, trackIndex, decoder);
            } catch (Exception e) {
                Log.i(TAG,"startPlay e:" + e);
            }
        }

        private void doExtract(MediaExtractor extractor, int trackIndex, MediaCodec decoder) {
            final int TIMEOUT_USEC = 10000;
            ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
            int inputChunk = 0;
            long firstInputTimeNsec = -1;

            boolean outputDone = false;
            boolean inputDone = false;
            while (!outputDone) {

                if (!inputDone) {
                    int inputbufferIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                    if (inputbufferIndex > 0) {
                        if (firstInputTimeNsec == -1) {
                            firstInputTimeNsec = System.nanoTime();
                        }

                        ByteBuffer inputBuffer = decoderInputBuffers[inputbufferIndex];
                        int chunkSize = extractor.readSampleData(inputBuffer, 0);
                        if (chunkSize < 0) {
                            decoder.queueInputBuffer(inputbufferIndex, 0 ,0 ,0L,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                        } else {
                            long presentationTimeUs = extractor.getSampleTime();
                            decoder.queueInputBuffer(inputbufferIndex, 0 ,chunkSize ,presentationTimeUs,0);
                            inputChunk ++;
                            extractor.advance();
                        }
                    } else {
                        Log.i(TAG,"input buffer is not awawable");
                    }
                }

                if (!outputDone) {
                    int decodeStatus = decoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                    if (decodeStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        Log.i(TAG,"no output awaiable decoder !");
                    } else if (decodeStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        Log.i(TAG,"decoder output buffer changed!");
                    } else if (decodeStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat format = decoder.getOutputFormat();
                        Log.i(TAG,"NEW format info:" + format.toString());
                    } else if (decodeStatus < 0) {
                        throw new RuntimeException(
                                "unexpected result from decoder.dequeueOutputBuffer: " +
                                        decodeStatus);
                    } else {
                        if (firstInputTimeNsec != 0) {
                            // Log the delay from the first buffer of input to the first buffer
                            // of output.
                            long nowNsec = System.nanoTime();
                            Log.d(TAG, "startup lag " + ((nowNsec-firstInputTimeNsec) / 1000000.0) + " ms");
                            firstInputTimeNsec = 0;
                        }

                        Log.d(TAG, "surface decoder given buffer " + decodeStatus +
                                " (size=" + mBufferInfo.size + ")");

                        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true;
                            Log.d(TAG, "output EOS");
                        }

                        boolean doRender = (mBufferInfo.size != 0);

                        if (doRender && mFrameCallback != null) {
                            mFrameCallback.preRender(mBufferInfo.presentationTimeUs);
                        }
                        decoder.releaseOutputBuffer(decodeStatus, doRender);
                        if (doRender && mFrameCallback != null) {
                            mFrameCallback.postRender();
                        }

                        videoEncoder.drainEncoder(outputDone);
                    }
                }
            }
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
    }
}