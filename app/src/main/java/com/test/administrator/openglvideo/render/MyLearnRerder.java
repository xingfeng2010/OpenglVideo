package com.test.administrator.openglvideo.render;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.test.administrator.openglvideo.R;
import com.test.administrator.openglvideo.util.ShaderHelper;
import com.test.administrator.openglvideo.util.TextResourceReader;
import com.test.administrator.openglvideo.util.TextureHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glUniform1i;

public class MyLearnRerder implements GLSurfaceView.Renderer {
    float[] tableVerticesWithTriangles = {
            -1.0f, 1.0f, 0.0f,  1.0f, 0.0f, 0.0f,
            1.0f, 1.0f, 0.0f,  0.0f, 1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,  0.0f, 0.0f, 1.0f,
            -1.0f, -1.0f, 0.0f,  0.0f, 0.0f, 1.0f
    };
    private static final int BYTES_PER_FLOAT = 4;
    private static final int POSITION_COMPONENT_COUNT = 3;
    private static final int COLOR_COMPONENT_COUNT = 3;
    private static final int TEXTURE_COMPONENT_COUNT = 2;

    private static final int STRIDE = (POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT) * BYTES_PER_FLOAT;


    private FloatBuffer mVertexBuffer;
    private Context mContext;
    private int mProgram;
    private int mTimeTexture;

    private static final String A_POSITION = "a_Position";
    private static final String A_COLOR = "a_Color";
    private static final String A_TEXTURE = "a_Texture";
    private static final String U_TEXTURE_UNIT = "u_TextureUnit";

    private int aPositionLocation;
    private int aColorLocation;
    private int aTextureLocation;

    private int uTextureUnit;
    private int[] mTextureIds = new int[2];


    public MyLearnRerder(Context context) {
        mContext = context;
        initPositionData();
    }

    private void initPositionData() {
        mVertexBuffer = ByteBuffer.allocateDirect(tableVerticesWithTriangles.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(tableVerticesWithTriangles);

        mVertexBuffer.position(0);
    }

    private void initProgram() {
        String vertexShaderSource = TextResourceReader
                .readTextFileFromResource(mContext, R.raw.mylearn_vertex_shader);
        String fragmentShaderSource = TextResourceReader
                .readTextFileFromResource(mContext, R.raw.mylearn_fragment_shader);

        int vertexShader = ShaderHelper.compileVertexShader(vertexShaderSource);
        int fragmentShader = ShaderHelper.compileFragmentShader(fragmentShaderSource);

        mProgram = ShaderHelper.linkProgram(vertexShader, fragmentShader);

        aPositionLocation = GLES20.glGetAttribLocation(mProgram, A_POSITION);
        GLES20.glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT, GLES20.GL_FLOAT,
                false, STRIDE, mVertexBuffer);
        //Enable由索引index指定的通用顶点属性数组。
        GLES20.glEnableVertexAttribArray(aPositionLocation);

        mVertexBuffer.position(POSITION_COMPONENT_COUNT);
        aColorLocation = GLES20.glGetAttribLocation(mProgram, A_COLOR);
        GLES20.glVertexAttribPointer(aColorLocation, COLOR_COMPONENT_COUNT, GLES20.GL_FLOAT, false,
                STRIDE, mVertexBuffer);
        GLES20.glEnableVertexAttribArray(aColorLocation);

//        mVertexBuffer.position(POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT);
//        aTextureLocation = GLES20.glGetAttribLocation(mProgram, A_TEXTURE);
//        GLES20.glVertexAttribPointer(aTextureLocation, TEXTURE_COMPONENT_COUNT, GLES20.GL_FLOAT, false,
//                STRIDE, mVertexBuffer);
//        glEnableVertexAttribArray(aTextureLocation);
//
//        uTextureUnit = GLES20.glGetUniformLocation(mProgram, U_TEXTURE_UNIT);
//        GLES20.glGenTextures(1, mTextureIds, 0);
//        mTimeTexture = TextureHelper.loadTexture(mContext, R.drawable.lady, mTextureIds);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);

        initProgram();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(mProgram);
        GLES20.glDrawArrays(gl.GL_TRIANGLE_FAN, 0, 4);
//
//        glActiveTexture(GL_TEXTURE0);
//        glBindTexture(GL_TEXTURE_2D, mTimeTexture);
//        glUniform1i(uTextureUnit, 0);
    }
}
