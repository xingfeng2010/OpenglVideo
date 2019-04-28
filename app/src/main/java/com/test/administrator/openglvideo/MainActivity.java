package com.test.administrator.openglvideo;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.test.administrator.openglvideo.render.MapRender;
import com.test.administrator.openglvideo.render.VideoRender;


public class MainActivity extends AppCompatActivity {

    GLSurfaceView mGLSurfaceView;
    private boolean bRenderSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

       //showSurfaceView();
       setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PackageManager.PERMISSION_GRANTED == this.checkSelfPermission(Manifest.permission.CAMERA)) {
            } else {
                this.requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 10);
            }
        }
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
           // mGLSurfaceView.setRenderer(new MapRender(this));
            mGLSurfaceView.setRenderer(new VideoRender(mGLSurfaceView, this));

            bRenderSet = true;
        } else {
            bRenderSet = false;
        }
    }
}
