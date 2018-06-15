package com.test.administrator.openglvideo;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.test.administrator.openglvideo.render.PicFilterRerder;


public class MainActivity extends AppCompatActivity {

    GLSurfaceView mGLSurfaceView;
    private boolean bRenderSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

       // showSurfaceView();
       setContentView(R.layout.activity_main);
    }

    @Override
    protected void onPause() {
        super.onPause();
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
            mGLSurfaceView.setRenderer(new PicFilterRerder(this));

            bRenderSet = true;
        } else {
            bRenderSet = false;
        }
    }
}
