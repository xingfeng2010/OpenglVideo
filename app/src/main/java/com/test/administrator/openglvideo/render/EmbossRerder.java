package com.test.administrator.openglvideo.render;

import android.content.Context;
import android.media.effect.Effect;
import android.media.effect.EffectContext;
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
import static android.opengl.GLES20.glUniform1i;

public class EmbossRerder implements GLSurfaceView.Renderer {
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

    private static final String A_POSITION = "a_Position";
    private static final String A_TEXTURE_COORD = "a_TextureCoord";
    private static final String U_TEXTURE_UNIT = "u_TextureUnit";

    private int aPositionLocation;
    private int aTextureCoord;
    private int uTextureUnit;

    private FloatBuffer mTextureVertexBuffer;

    private EffectContext mEffectContext;
    private Effect mEffect;

    private int mWidth;
    private int mHeight;
    int[] textureObjectIds = new int[2];

    public EmbossRerder(Context context) {
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
                .readTextFileFromResource(mContext, R.raw.emboss_vertex_shader);
        String fragmentShaderSource = TextResourceReader
                .readTextFileFromResource(mContext, R.raw.emboss_fragment_shader);

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

        uTextureUnit = GLES20.glGetUniformLocation(mProgram, U_TEXTURE_UNIT);

        GLES20.glGenTextures(1, textureObjectIds, 0);
        mTimeTexture = TextureHelper.loadTexture(mContext, R.drawable.timg,textureObjectIds);
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

        //安装一个program object，并把它作为当前rendering state的一部分。
        GLES20.glUseProgram(mProgram);

        //三个成员变量mode,first,count
        //1) mode:指明render原语，如：GL_POINTS, GL_LINE_STRIP, GL_LINE_LOOP, GL_LINES, GL_TRIANGLE_STRIP, GL_TRIANGLE_FAN, GL_TRIANGLES, GL_QUAD_STRIP, GL_QUADS, 和 GL_POLYGON。
        //2) first: 指明Enable数组中起始索引。
        //3) count: 指明被render的原语个数。
        //可以预先使用单独的数据定义vertex、normal和color，然后通过一个简单的glDrawArrays构造一系列原语。当调用 glDrawArrays时，它使用每个enable的数组中的count个连续的元素，来构造一系列几何原语，从第first个元素开始

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);

        // Set the active texture unit to texture unit 0.
        glActiveTexture(GL_TEXTURE0);

        // Bind the texture to this unit.
        glBindTexture(GL_TEXTURE_2D, mTimeTexture);

        // Tell the texture uniform sampler to use this texture in the shader by
        // telling it to read from texture unit 0.
        glUniform1i(uTextureUnit, 0);
    }
}
