package com.test.administrator.openglvideo.render;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.view.Surface;

import com.test.administrator.openglvideo.R;
import com.test.administrator.openglvideo.util.ShaderHelper;
import com.test.administrator.openglvideo.util.TextResourceReader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class EasyVideoRender implements GLSurfaceView.Renderer {
    private static final float[] VERTEX = {
            -1.0f, 1.0f, 0.0f,
            -1.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            1.0f, 1.0f, 0.0f,
    };

    private static final float[] UV_TEX_VERTEX = {   // in clockwise order:
            1.0f, 0.0f,
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
    };

    private static final int BYTES_PER_FLOAT = 4;
    private static final int POSITION_COMPONENT_COUNT = 3;
    private static final int TEXTURE_COMPONENT_COUNT = 0;

    private static final int STRIDE = (POSITION_COMPONENT_COUNT + TEXTURE_COMPONENT_COUNT) * BYTES_PER_FLOAT;

    public String videoPath = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4";
    private FloatBuffer mVertexBuffer;
    private Context mContext;
    private int mProgram;
    private int mTexture;

    private static final String A_POSITION = "a_Position";
    private static final String A_TEXTURE_COORD = "a_TextureCoord";

    private int aPositionLocation;
    private int aTextureCoord;

    private FloatBuffer mTextureVertexBuffer;
    private SurfaceTexture mSurfaceTexture;
    private MediaPlayer mMediaPlayer;
    private GLSurfaceView mGLSurfaceView;

    private SurfaceTexture.OnFrameAvailableListener mOnFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            mGLSurfaceView.requestRender();
        }
    };

    public EasyVideoRender(Context context, GLSurfaceView view) {
        mGLSurfaceView = view;
        mContext = context;
        initPositionData();
        initTextureData();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mSurfaceTexture = new SurfaceTexture(0);
        mSurfaceTexture.setOnFrameAvailableListener(mOnFrameAvailableListener);

        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);

        initProgram();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        openCamera();
    }

    private void openCamera() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mMediaPlayer.start();
                }
            });
            mMediaPlayer.setSurface(new Surface(mSurfaceTexture));
            try {
                mMediaPlayer.setDataSource(videoPath);
                mMediaPlayer.prepareAsync();
            } catch (Exception e) {

            }
        } else {
            mMediaPlayer.start();
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);

        if (mSurfaceTexture != null) {
            mSurfaceTexture.updateTexImage();
        }
    }

    private void initTextureData() {
        mTextureVertexBuffer = ByteBuffer.allocateDirect(UV_TEX_VERTEX.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(UV_TEX_VERTEX);

        mTextureVertexBuffer.position(0);
    }

    private void initPositionData() {
        mVertexBuffer = ByteBuffer.allocateDirect(VERTEX.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(VERTEX);

        mVertexBuffer.position(0);
    }

    private void initProgram() {
        String vertexShaderSource = TextResourceReader
                .readTextFileFromResource(mContext, R.raw.camera_vertex_shader);
        String fragmentShaderSource = TextResourceReader
                .readTextFileFromResource(mContext, R.raw.camera_fragment_shader);

        int vertexShader = ShaderHelper.compileVertexShader(vertexShaderSource);
        int fragmentShader = ShaderHelper.compileFragmentShader(fragmentShaderSource);

        mProgram = ShaderHelper.linkProgram(vertexShader, fragmentShader);

        aPositionLocation = GLES20.glGetAttribLocation(mProgram, A_POSITION);
        GLES20.glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT, GLES20.GL_FLOAT,
                false, STRIDE, mVertexBuffer);
        //Enable由索引index指定的通用顶点属性数组。
        GLES20.glEnableVertexAttribArray(aPositionLocation);

        aTextureCoord = GLES20.glGetAttribLocation(mProgram, A_TEXTURE_COORD);
        GLES20.glVertexAttribPointer(aTextureCoord, 2, GLES20.GL_FLOAT,
                false, 8, mTextureVertexBuffer);
        //Enable由索引index指定的通用顶点属性数组。
        GLES20.glEnableVertexAttribArray(aTextureCoord);
    }
}
