package com.test.administrator.openglvideo.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

public class VideoTextureView extends TextureView implements TextureView.SurfaceTextureListener {
    Context mContext;
    private MediaPlayer mediaPlayer;
    private String URL = "http://www.androidbook.com/akc/filestorage/android/documentfiles/3389/movie.mp4";
    public VideoTextureView(Context context) {
        this(context, null);
    }

    public VideoTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        this.setSurfaceTextureListener(this);
        init();
    }

    private void init() {
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        Log.i("DEBUG_TEST", "width:" + mp.getVideoWidth() + "  height:" + mp.getVideoHeight());
                        mp.start();
                    }
                });
                Surface surface = new Surface(surfaceTexture);
                mediaPlayer.setSurface(surface);
                surface.release();
                mediaPlayer.setDataSource(URL);
                mediaPlayer.prepareAsync();
            } else {
                mediaPlayer.start();
            }
        } catch (Exception e) {
            Log.i("DEBUG_TEST", "exception:" + e);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
