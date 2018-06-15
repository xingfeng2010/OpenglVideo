package com.test.administrator.openglvideo.anim;

import android.graphics.Matrix;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import com.test.administrator.openglvideo.view.CustomImageView;

public class MyAnim extends Animation implements Animation.AnimationListener {
    // region: Fields and Consts

    private final CustomImageView mImageView;

    private final float[] mStartImageMatrix = new float[9];

    private final float[] mEndImageMatrix = new float[9];

    private final float[] mAnimMatrix = new float[9];
    // endregion

    public MyAnim(CustomImageView cropImageView) {
        mImageView = cropImageView;

        setDuration(300);
        setFillAfter(true);
        setInterpolator(new AccelerateDecelerateInterpolator());
        setAnimationListener(this);
    }

    public void setStartState(Matrix imageMatrix) {
        reset();
        imageMatrix.getValues(mStartImageMatrix);
    }

    public void setEndState(Matrix imageMatrix) {
        imageMatrix.getValues(mEndImageMatrix);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        Log.i("DEBUG_TEST", "applyTransformation interpolatedTime:" + interpolatedTime);
        for (int i = 0; i < mAnimMatrix.length; i++) {
            mAnimMatrix[i] =
                    mStartImageMatrix[i] + (mEndImageMatrix[i] - mStartImageMatrix[i]) * interpolatedTime;
        }
        Matrix m = mImageView.getImageMatrix();
        m.setValues(mAnimMatrix);
        mImageView.setImageMatrix(m);
       // mImageView.checkBorderAndCenterWhenScale(false);

        mImageView.invalidate();
    }

    @Override
    public void onAnimationStart(Animation animation) {}

    @Override
    public void onAnimationEnd(Animation animation) {
        mImageView.clearAnimation();
    }

    @Override
    public void onAnimationRepeat(Animation animation) {}
}
