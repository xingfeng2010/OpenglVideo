package com.test.administrator.openglvideo;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import com.test.administrator.openglvideo.view.DynamicHeartView;

public class DyanmicHeart extends AppCompatActivity {
    private DynamicHeartView dynamic_view;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dyanmic_heart);

        dynamic_view = (DynamicHeartView) findViewById(R.id.dynamic_view);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dynamic_view.startPathAnim(1000);
//                Animator animator = ViewAnimationUtils.createCircularReveal(dynamic_view, 400, 400, 0, 360);
//                animator.setInterpolator(new DecelerateInterpolator());
//                animator.setDuration(2000);
//                animator.start();
            }
        }, 3000);
    }
}
