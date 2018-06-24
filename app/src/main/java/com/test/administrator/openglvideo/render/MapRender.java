package com.test.administrator.openglvideo.render;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.test.administrator.openglvideo.R;
import com.test.administrator.openglvideo.util.ShaderHelper;
import com.test.administrator.openglvideo.util.TextResourceReader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MapRender implements GLSurfaceView.Renderer {
    private static final float[] vertex = new float[]{
            -1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f
    };

    private FloatBuffer vertextBuffer;

    private static final String A_POSITION = "a_Position";
    private int aPositionLocation;

    private int mProgram;
    private Context mContext;



    public MapRender(Context context) {
        mContext = context;
        initData();
    }

    private void initData() {
        vertextBuffer = ByteBuffer.allocateDirect(vertex.length * 4).
                order(ByteOrder.nativeOrder()).
                asFloatBuffer().
                put(vertex);

        vertextBuffer.position(0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        initProgram();
    }

    private void initProgram() {
        mProgram = ShaderHelper.buildProgram(TextResourceReader.readTextFileFromResource(mContext, R.raw.shape_vertex_shader),
                TextResourceReader.readTextFileFromResource(mContext, R.raw.shape_fragment_shader));
        GLES20.glUseProgram(mProgram);

        aPositionLocation = GLES20.glGetAttribLocation(mProgram, A_POSITION);
        GLES20.glVertexAttribPointer(aPositionLocation, 2,
                GLES20.GL_FLOAT, false, 0, vertextBuffer);
        GLES20.glEnableVertexAttribArray(aPositionLocation);
    }

    @Override
    public void onSurfaceChanged (GL10 gl10, int width, int height){
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame (GL10 gl){
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,3);
    }
}
