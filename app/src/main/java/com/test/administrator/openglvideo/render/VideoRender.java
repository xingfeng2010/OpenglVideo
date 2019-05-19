package com.test.administrator.openglvideo.render;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import com.test.administrator.openglvideo.util.EGLHelper;
import com.test.administrator.openglvideo.util.MyTextureHelper;
import com.test.administrator.openglvideo.util.ShaderHelper;
import com.test.administrator.openglvideo.util.TextResourceReader;
import com.test.administrator.openglvideo.util.TextureHelper;
import com.test.administrator.openglvideo.R;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.Semaphore;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.net.sip.SipErrorCode.TIME_OUT;

public class VideoRender implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private static final String mOutputPath = Environment.getExternalStoragePublicDirectory("opengl").getPath();
    /**
     *
     */
    private static float squareSize = 1.0f;
    private static float squareCoords[] = {
            -squareSize, squareSize,   // top left
            -squareSize, -squareSize,   // bottom left
            squareSize, -squareSize,    // bottom right
            squareSize, squareSize}; // top right

    private static short drawOrder[] = {0, 1, 2, 0, 2, 3};

    private int mTexture;
    private int mProgram;
    private Context mContext;

    // Texture to be shown in backgrund
    private FloatBuffer textureBuffer;
    private float textureCoords[] = {
            0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 1.0f, 0.0f, 1.0f};

    private int[] textures = new int[1];

    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;

    private float[] videoTextureTransform;
    private boolean frameAvailable = false;

    int textureParamHandle;
    int textureCoordinateHandle;
    int positionHandle;
    int textureTranformHandle;

    private SurfaceTexture mSurfaceTexture;
    private Surface mDecoderSurface;
    private MediaExtractor extractor;
    public String videoPath = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4";

    private int tracknumb;
    private MediaCodec decoder, encoder;
    private int width, height;
    private int videoWidth, videoHeight;
    private EGLHelper mEglHelper;
    private GLSurfaceView mGLSurfaceView;

    private MediaMuxer mMediaMuxer;
    private int mVideoEncoderTrack;

    Semaphore mSem = new Semaphore(0);

    public VideoRender(GLSurfaceView glSurfaceView, Context context) {
        Log.i("DEBUG_TEST", "mOutputPath:" + mOutputPath);
        mGLSurfaceView = glSurfaceView;
        mContext = context;
        videoTextureTransform = new float[16];
        mEglHelper = new EGLHelper();
        initPoint();
    }

    private void initPoint() {
        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        // Initialize the texture holder
        ByteBuffer bb = ByteBuffer.allocateDirect(squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());

        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);


        ByteBuffer texturebb = ByteBuffer.allocateDirect(textureCoords.length * 4);
        texturebb.order(ByteOrder.nativeOrder());

        textureBuffer = texturebb.asFloatBuffer();
        textureBuffer.put(textureCoords);
        textureBuffer.position(0);
    }

    private int mTimeTexture;

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        initProgram();
        //mTimeTexture = MyTextureHelper.loadTexture(mContext, R.drawable.fengj);
//        initTexture();
        try {
            //initExtractor();
            initDecoder();
            //initEncoder();
            startDecode();
        } catch (Exception e) {
            Log.i("DEBUG_TEST", " exception e:" + e);
        }

    }

    private void initEncoder() {
        // get mimetype and format
        MediaFormat format = extractor.getTrackFormat(tracknumb);

         Log.i("DEBUG_TEST", " initEncoder para:" + format.toString());
        String mime = format.getString(MediaFormat.KEY_MIME);
        videoWidth = format.getInteger(MediaFormat.KEY_WIDTH);
        videoHeight = format.getInteger(MediaFormat.KEY_HEIGHT);

        MediaFormat videoFormat = MediaFormat.createVideoFormat(mime, videoWidth, videoHeight);
        //videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, videoHeight * videoWidth * 5);
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 24);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, 8);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        try {
            encoder = MediaCodec.createEncoderByType(mime);
            Log.i("DEBUG_TEST", " initEncoder mime:" + mime);
            Log.i("DEBUG_TEST", " initEncoder 000 e:");
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (Exception e) {
            Log.i("DEBUG_TEST", " initEncoder exception e:" + e);
        }

        //注意这里，创建了一个Surface，这个Surface是编码器的输入，也是OpenGL环境的输出
        Surface outputSurface = encoder.createInputSurface();
        Bundle bundle = new Bundle();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            bundle.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, width * width * 5);
            encoder.setParameters(bundle);
        }

        mEglHelper.setSurface(outputSurface);
        boolean create = mEglHelper.createGLES(videoWidth, videoHeight);
        if (!create) {
            Log.i("DEBUG_TEST", " initEncoder failed !!");
        }
    }

    private void initTexture() {
        // Prepare texture handler
        GLES20.glGenTextures(1, textures, 0);

        mTexture = textures[0];
       // GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTexture);

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        // Link the texture handler to surface texture
        mSurfaceTexture = new SurfaceTexture(mTexture);
        //mSurfaceTexture.setDefaultBufferSize(320, 240);

        // Create decoder surface
        mDecoderSurface = new Surface(mSurfaceTexture);
        mEglHelper.setSurface(mSurfaceTexture);
        boolean create = mEglHelper.createGLES(videoWidth,videoHeight);
        if (!create) {
            Log.i("DEBUG_TEST"," createGLES failed !!");
        }
        mSurfaceTexture.setOnFrameAvailableListener(this);
    }

    private boolean initExtractor() {
        extractor = new MediaExtractor();
        try {
            extractor.setDataSource(videoPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // get video track
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video")) {
                tracknumb = i;
                break;
            }
        }

        if (tracknumb == -1) {
            Log.e("DEBUG_TEST", "Can't find video track!");
            return false;
        }

        Log.e("DEBUG_TEST", "find video track!");
        // set track to extractor
        extractor.selectTrack(tracknumb);

        return true;
    }

    private boolean initDecoder() throws IOException{
        extractor = new MediaExtractor();
        extractor.setDataSource(videoPath);

        int count = extractor.getTrackCount();
        for (int i =0; i < count; i ++) {
           MediaFormat format = extractor.getTrackFormat(i);
           String mime = format.getString(MediaFormat.KEY_MIME);
           if (mime.startsWith("video")) {
               videoWidth = format.getInteger(MediaFormat.KEY_WIDTH);
               videoHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
               decoder = MediaCodec.createDecoderByType(mime);
               extractor.selectTrack(i);

               //initTexture();

               mTexture = mEglHelper.createTextureID();
               mSurfaceTexture = new SurfaceTexture(mTexture);
               mDecoderSurface = new Surface(mSurfaceTexture);
               mSurfaceTexture.setOnFrameAvailableListener(this);

               decoder.configure(format, mDecoderSurface, null, 0);

               mEglHelper.setSurface(mSurfaceTexture);
               mEglHelper.createGLES(videoWidth,videoHeight);

               //decoder.start();
               break;
           }
        }

        return true;
    }


    private void initProgram() {
        //获取顶点着色器文本
        String vertexShaderSource = TextResourceReader
                .readTextFileFromResource(mContext, R.raw.video_vertext_shader);
        //获取片段着色器文本
        String fragmentShaderSource = TextResourceReader
                .readTextFileFromResource(mContext, R.raw.video_fragment_shader);
        //获取program的id
        mProgram = ShaderHelper.buildProgram(vertexShaderSource, fragmentShaderSource);
        GLES20.glUseProgram(mProgram);

        textureParamHandle = GLES20.glGetUniformLocation(mProgram, "texture");
        textureCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "vTexCoordinate");
        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        textureTranformHandle = GLES20.glGetUniformLocation(mProgram, "textureTransform");
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int i, int i1) {
        width = i;
        height = i1;
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        Log.i("lishixing", "onDrawFrame:");
        {
            if (frameAvailable) {
                mSurfaceTexture.updateTexImage();
                mSurfaceTexture.getTransformMatrix(videoTextureTransform);
                frameAvailable = false;
            }

        }
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glViewport(0, 0, width, height);
        this.drawTexture();
        Log.i("lishixing", "videoEncodeStep onDrawFrame:");
        //videoEncodeStep(false);
        mEglHelper.swapBuffers();
    }

    private void drawTexture() {
        // Draw texture

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTexture);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glUniform1i(textureParamHandle, 0);

        GLES20.glEnableVertexAttribArray(textureCoordinateHandle);
        GLES20.glVertexAttribPointer(textureCoordinateHandle, 4, GLES20.GL_FLOAT, false, 0, textureBuffer);

        GLES20.glUniformMatrix4fv(textureTranformHandle, 1, false, videoTextureTransform, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(textureCoordinateHandle);
    }


    public void startDecode() {
        new Thread() {

            @Override
            public void run() {
                decoder.start();
//                encoder.start();
//                mMediaMuxer.start();
                // get buffers
                // start getting buffer
                BufferInfo info = new BufferInfo();
                boolean isEOS = false;
                long startMs = System.currentTimeMillis();

                Log.d("DEBUG_TEST", "BufferInfo: size:" + info.size);


                while (!Thread.interrupted()) {
                    // get input buffer (decoder)
                    if (!isEOS) {
                        isEOS = readDecoderBuffer();
                    }

                    isEOS = checkDecoderBuffer(info, startMs);
                    if (isEOS)
                        break;
                }

                decoder.stop();
                decoder.release();
//                extractor.release();


            }
        }.start();
    }

    private boolean readDecoderBuffer() {
        Log.d("DECODE_TEST", "readDecoderBuffer 00");
        int inIndex = decoder.dequeueInputBuffer(10000);

        Log.d("DECODE_TEST", "readDecoderBuffer 11 inIndex:" + inIndex);

        // index did not get correctly
        if (inIndex < 0)
            return true;

        ByteBuffer buffer = decoder.getInputBuffer(inIndex);
        int sampleSize = extractor.readSampleData(buffer, 0);

        if (sampleSize < 0) {

            // We shouldn't stop the playback at this point, just pass the EOS
            // flag to decoder, we will get it again from the dequeueOutputBuffer
            Log.d("DECODE_TEST", "InputBuffer BUFFER_FLAG_END_OF_STREAM");
            decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);

            return true;

        } else {

            decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor.getSampleTime(), 0);
            extractor.advance();

        }

        return false;
    }

    private boolean checkDecoderBuffer(BufferInfo info, long startMs) {
        // get output buffer, to control the time
        int outIndex = decoder.dequeueOutputBuffer(info, 10000);
        Log.i("DECODE_TEST", "BufferInfo: size:" + info.size + " presentationTimeUs:" + info.presentationTimeUs + " offset:" + info.offset + " flags:" + info.flags);

        switch (outIndex) {
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                Log.d("DECODE_TEST", "INFO_OUTPUT_BUFFERS_CHANGED");
                decoder.getOutputBuffers();
                break;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                Log.d("DECODE_TEST", "New format " + decoder.getOutputFormat());
                break;
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                Log.d("DECODE_TEST", "dequeueOutputBuffer timed out!");
                break;
            default:
                ByteBuffer buffer = decoder.getOutputBuffer(outIndex);
                Log.v("DECODE_TEST", "We can't use this buffer but render it due to the API limit, " + buffer);

                // We use a very simple clock to keep the video FPS, or the video playback will be too fast
                while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        return false;
                    }
                }

                decoder.releaseOutputBuffer(outIndex, true);
                break;
        }

        // All decoded frames have been rendered, we can stop playing now
        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            Log.d("DECODE_TEST", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
            return true;
        }

        return false;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized (this) {
            frameAvailable = true;
            mGLSurfaceView.requestRender();
            Log.i("lishixing", "videoEncodeStep onFrameAvailable:");
        }
    }

    private boolean videoEncodeStep(boolean isEnd) {
        if (isEnd) {
            encoder.signalEndOfInputStream();
        }
        MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
        while (true) {
            int mOutputIndex = encoder.dequeueOutputBuffer(info, 10000);
            Log.i("lishixing", "videoEncodeStep mOutputIndex:" + mOutputIndex);
            Log.i("lishixing", "videoEncodeStep mVideoEncoderBufferInfo:" + info.size);
            if (mOutputIndex >= 0) {
                ByteBuffer buffer = getOutputBuffer(encoder, mOutputIndex);
                if (info.size > 0) {
                    mMediaMuxer.writeSampleData(mVideoEncoderTrack, buffer, info);
                }
                encoder.releaseOutputBuffer(mOutputIndex, false);
            } else if (mOutputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat format = encoder.getOutputFormat();
                mVideoEncoderTrack = mMediaMuxer.addTrack(format);
//                mMediaMuxer.start();
//                synchronized (MUX_LOCK){
//                    MUX_LOCK.notifyAll();
//                }
            } else if (mOutputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break;
            }
        }
        return false;
    }

    private ByteBuffer getOutputBuffer(MediaCodec encoder, int mOutputIndex) {
        ByteBuffer buffer = encoder.getOutputBuffer(mOutputIndex);

        return buffer;
    }
}
