package com.test.administrator.openglvideo.render;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.test.administrator.openglvideo.R;
import com.test.administrator.openglvideo.util.MyTextureHelper;
import com.test.administrator.openglvideo.util.ShaderHelper;
import com.test.administrator.openglvideo.util.TextResourceReader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class PicFilterRerder implements GLSurfaceView.Renderer {
    private static final float[] VERTEX = {
            -1.0f, 1.0f, 0.0f,
            -1.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            1.0f, 1.0f, 0.0f,
    };

    private static final float[] UV_TEX_VERTEX = {   // in clockwise order:
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
    };

    private static final int BYTES_PER_FLOAT = 4;
    private static final int POSITION_COMPONENT_COUNT = 3;
    private static final int TEXTURE_COMPONENT_COUNT = 0;

    private static final int STRIDE = (POSITION_COMPONENT_COUNT + TEXTURE_COMPONENT_COUNT) * BYTES_PER_FLOAT;


    private FloatBuffer mVertexBuffer;
    private Context mContext;
    private int mProgram;
    private int mTimeTexture;
    private int mTreeTexture;

    private static final String A_POSITION = "a_Position";
    private static final String A_TEXTURE_COORD = "a_TextureCoord";
    private static final String U_TEXTURE_UNIT_1 = "u_TextureUnit1";
    private static final String U_TEXTURE_UNIT_2 = "u_TextureUnit2";


    private int aPositionLocation;
    private int aTextureCoord;
    private int uTextureUnit1;
    private int uTextureUnit2;

    private FloatBuffer mTextureVertexBuffer;

    private int mWidth;
    private int mHeight;

    int[] textTures = new int[2];

    public PicFilterRerder(Context context) {
        mContext = context;
        initPositionData();
        initTextureData();
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
                .readTextFileFromResource(mContext, R.raw.filter_vertex_shader);
        String fragmentShaderSource = TextResourceReader
                .readTextFileFromResource(mContext, R.raw.filter_fragment_shader);

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

        uTextureUnit1 = GLES20.glGetUniformLocation(mProgram, U_TEXTURE_UNIT_1);
        uTextureUnit2 = GLES20.glGetUniformLocation(mProgram, U_TEXTURE_UNIT_2);

        mTimeTexture = MyTextureHelper.loadTexture(mContext,R.drawable.timg);
        mTreeTexture = MyTextureHelper.loadTexture(mContext,R.drawable.tree);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);

        initProgram();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        mWidth = width;
        mHeight = height;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(mProgram);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);

        //激活纹理单元，GL_TEXTURE0代表纹理单元0，GL_TEXTURE1代表纹理单元1，以此类推。OpenGL使用纹理单元来表示被绘制的纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //绑定纹理到这个纹理单元
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTimeTexture);
        //把选定的纹理单元传给片段着色器中的u_TextureUnit，
        GLES20.glUniform1i(uTextureUnit1, 0);

        //激活纹理单元，GL_TEXTURE0代表纹理单元0，GL_TEXTURE1代表纹理单元1，以此类推。OpenGL使用纹理单元来表示被绘制的纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        //绑定纹理到这个纹理单元
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTreeTexture);
        //把选定的纹理单元传给片段着色器中的u_TextureUnit，
        GLES20.glUniform1i(uTextureUnit2, 1);
    }
}
