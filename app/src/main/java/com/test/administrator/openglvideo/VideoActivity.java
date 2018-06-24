package com.test.administrator.openglvideo;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;

import com.test.administrator.openglvideo.render.MapRender;
import com.test.administrator.openglvideo.render.VideoDecodRender;
import com.test.administrator.openglvideo.render.VideoRender;
import com.test.administrator.openglvideo.util.EGLHelper;

import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;

public class VideoActivity extends AppCompatActivity {
    private static final String TAG = "VideoActivity";
    private GLSurfaceView mGLSurfaceView;
    private boolean bRenderSet = false;

    private static final String mOutputPath = Environment.getExternalStoragePublicDirectory("opengl").getPath();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showSurfaceView();

        Log.i(TAG,"onCreate mOutputPath:" + mOutputPath);
    }

    private void showSurfaceView() {
        mGLSurfaceView = new GLSurfaceView(this);
        config();
        setContentView(mGLSurfaceView);
    }

    private void config() {
        ActivityManager activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo info = activityManager.getDeviceConfigurationInfo();
        boolean supportES2 = info.reqGlEsVersion >= 0x20000;
        if (supportES2) {
            mGLSurfaceView.setEGLContextClientVersion(2);
            mGLSurfaceView.setRenderer(new VideoRender(mGLSurfaceView, this));

            bRenderSet = true;
        } else {
            bRenderSet = false;
        }
    }
}
