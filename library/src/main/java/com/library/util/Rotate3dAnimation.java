package com.library.util;

/**
 * Created by android1 on 2018/1/5.
 */

import android.graphics.Camera;
import android.graphics.Matrix;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;


public class Rotate3dAnimation extends Animation {
    public static final Byte ROTATE_X_AXIS = 0x00;
    public static final Byte ROTATE_Y_AXIS = 0x01;
    public static final Byte ROTATE_Z_AXIS = 0x02;
    private float mFromDegrees;
    private float mToDegrees;
    private float mCenterX;
    private float mCenterY;
    private float mDepthZ;
    private boolean mReverse;
    private Camera mCamera;
    private Byte mRotateAxis;  // 0：X轴  1：Y轴  2：Z轴


    /**
     * 对180度旋转的封装
     */
    public static void rotate3dDegrees180(final View view, final int depthZ, final int duration, final Byte rotateXAxis) {
        Rotate3dAnimation rotate3dAnimation1 = new Rotate3dAnimation(0, 90, view.getWidth() / 2,
                view.getHeight() / 2, depthZ, rotateXAxis, true);
        rotate3dAnimation1.setDuration(duration / 2);
        rotate3dAnimation1.setInterpolator(new LinearInterpolator());
        rotate3dAnimation1.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Rotate3dAnimation rotate3dAnimation2 = new Rotate3dAnimation(270, 360, view.getWidth() / 2,
                        view.getHeight() / 2, depthZ, rotateXAxis, false);
                rotate3dAnimation2.setDuration(duration / 2);
                rotate3dAnimation2.setInterpolator(new LinearInterpolator());
                view.startAnimation(rotate3dAnimation2);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        view.startAnimation(rotate3dAnimation1);
    }


    /**
     * @param fromDegrees 开始角度
     * @param toDegrees   结束角度
     * @param centerX     中心点X
     * @param centerY     中心点Y
     * @param depthZ      Z轴偏移距离，
     * @param rotateAxis  控制翻转方式 分别沿Y轴，X轴，Z轴。选择Z轴时和平面旋转一样
     * @param reverse     ture为由近到远，false为由远到近，偏移距离为depthZ
     */
    public Rotate3dAnimation(float fromDegrees, float toDegrees, float centerX, float centerY, float depthZ, Byte rotateAxis, boolean reverse) {
        mFromDegrees = fromDegrees;
        mToDegrees = toDegrees;
        mCenterX = centerX;
        mCenterY = centerY;
        mDepthZ = depthZ;
        mRotateAxis = rotateAxis;
        mReverse = reverse;
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
        mCamera = new Camera();
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        final float fromDegrees = mFromDegrees;
        float degrees = fromDegrees + ((mToDegrees - fromDegrees) * interpolatedTime);

        final float centerX = mCenterX;
        final float centerY = mCenterY;
        final Camera camera = mCamera;

        final Matrix matrix = t.getMatrix();
        //保存摄像头位置
        camera.save();
        if (mReverse) {
            //z的偏移会越来越大。形成从近到远效果
            camera.translate(0.0f, 0.0f, mDepthZ * interpolatedTime);
        } else {
            //z的偏移会越来越小。形成从远到近效果
            camera.translate(0.0f, 0.0f, mDepthZ * (1.0f - interpolatedTime));
        }
        //加上旋转效果
        if (ROTATE_X_AXIS.equals(mRotateAxis)) {
            camera.rotateX(degrees);
        } else if (ROTATE_Y_AXIS.equals(mRotateAxis)) {
            camera.rotateY(degrees);
        } else {
            camera.rotateZ(degrees);
        }

        //将一系列变换应用到变换矩阵上面
        camera.getMatrix(matrix);
        //camera位置恢复
        camera.restore();

        //以View中心为旋转点
        matrix.preTranslate(-centerX, -centerY);
        matrix.postTranslate(centerX, centerY);
    }
}