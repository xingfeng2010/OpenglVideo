package com.test.administrator.openglvideo.view;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class VideoSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mSurfaceHolder;
    private MediaPlayer mediaPlayer;
    private Context mContext;

    private String URL = "http://www.androidbook.com/akc/filestorage/android/documentfiles/3389/movie.mp4";
    public VideoSurfaceView(Context context) {
        this(context, null);
    }

    public VideoSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext =context;

        init();
    }

    private void init() {
        mSurfaceHolder = this.getHolder();
        mSurfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        Log.i("DEBUG_TEST","width:" + mp.getVideoWidth() + "  height:"+ mp.getVideoHeight());
                        mp.start();
                    }
                });
                mediaPlayer.setDisplay(mSurfaceHolder);
                mediaPlayer.setDataSource(URL);
                mediaPlayer.prepareAsync();
            } else {
                mediaPlayer.start();
            }
        } catch (Exception e) {
            Log.i("DEBUG_TEST","exception:" + e);
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }
}
