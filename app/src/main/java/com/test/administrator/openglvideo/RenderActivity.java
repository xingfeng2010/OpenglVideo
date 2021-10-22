package com.test.administrator.openglvideo;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import com.test.administrator.openglvideo.render.CameraRender;
import com.test.administrator.openglvideo.render.CustomRerder;
import com.test.administrator.openglvideo.render.EasyVideoRender;
import com.test.administrator.openglvideo.render.EmbossRerder;
import com.test.administrator.openglvideo.render.MapRender;
import com.test.administrator.openglvideo.render.MyLearnRerder;
import com.test.administrator.openglvideo.render.PicFilterRerder;
import com.test.administrator.openglvideo.render.SimpleRerder;
import com.test.administrator.openglvideo.render.VideoRender;

import androidx.appcompat.app.AppCompatActivity;

public class RenderActivity extends AppCompatActivity {

    GLSurfaceView mGLSurfaceView;
    private boolean bRenderSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGLSurfaceView = new GLSurfaceView(this);

       showSurfaceView();

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
        config();
        setContentView(mGLSurfaceView);
    }

    private void config() {
        ActivityManager activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo info = activityManager.getDeviceConfigurationInfo();
        boolean supportES2 = info.reqGlEsVersion >= 0x20000;
        if (supportES2) {
            mGLSurfaceView.setEGLContextClientVersion(2);
            String renderName = this.getIntent().getStringExtra("rendername");
            mGLSurfaceView.setRenderer(generateRender(renderName));
//            mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            bRenderSet = true;
        } else {
            bRenderSet = false;
        }
    }

    private GLSurfaceView.Renderer generateRender(String renderName) {
        if (TextUtils.isEmpty(renderName)) {
            return new VideoRender(mGLSurfaceView, this);
        }

        if (renderName.equalsIgnoreCase("MapRender")) {
            return new MapRender(this);
        } else if (renderName.equalsIgnoreCase("VideoRender")) {
           return new VideoRender(mGLSurfaceView, this);
        } else if (renderName.equalsIgnoreCase("CameraRender")) {
            return new CameraRender(this, mGLSurfaceView);
        } else if (renderName.equalsIgnoreCase("EasyVideoRender")) {
            return new EasyVideoRender(this, mGLSurfaceView);
        } else if (renderName.equalsIgnoreCase("CustomRerder")) {
            return new CustomRerder(this);
        } else if (renderName.equalsIgnoreCase("PicFilterRerder")) {
            return new PicFilterRerder(this);
        } else if (renderName.equalsIgnoreCase("EmbossRerder")) {
            return new EmbossRerder(this);
        } else if (renderName.equalsIgnoreCase("SimpleRerder")) {
            return new SimpleRerder(this);
        } else if (renderName.equalsIgnoreCase("MyLearnRerder")) {
            return new MyLearnRerder(this);
        } else {
            return new VideoRender(mGLSurfaceView, this);
        }
    }
}
