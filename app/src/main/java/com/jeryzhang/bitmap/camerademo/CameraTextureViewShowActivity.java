package com.jeryzhang.bitmap.camerademo;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;

import java.io.IOException;

/**
 * TextureView和SurfaceView显示Camera数据其实差不多,差别就两点
 * 1. SurfaceView显示需要实现SurfaceHolder.Callback的回调而TextureView通过实现 TextureView.SurfaceTextureListener接口。
 * 2. 当Camera使用SurfaceView预览时通过mCamera.setPreviewDisplay(holder)方法来设置预览视图，
 *    当Camera使用TextureView预览时使用mCamera.setPreviewTexture(mCameraTextureView.getSurfaceTexture())方法来设置
 */
public class CameraTextureViewShowActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {
    private TextureView mCameraTextureView;
    private Camera mCamera;
    private Camera.Parameters mParameters;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_textureview);
        mCameraTextureView = findViewById(R.id.textureview);
        mCameraTextureView.setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        try {
            mCamera = Camera.open(0);
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewTexture(mCameraTextureView.getSurfaceTexture());
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if (success) {
                    mParameters = mCamera.getParameters();
                    mParameters.setPictureFormat(PixelFormat.RGB_888); //图片输出格式
//                    mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);//预览持续发光
                    mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//持续对焦模式
                    mCamera.setParameters(mParameters);
                    mCamera.startPreview();
                    mCamera.cancelAutoFocus();
                }
            }
        });
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    public void onViewClicked(View view) {
        PropertyValuesHolder valuesHolder = PropertyValuesHolder.ofFloat("translationX", 0.0f, 0.0f);
        PropertyValuesHolder valuesHolder1 = PropertyValuesHolder.ofFloat("scaleX", 1.0f, 0.3f, 1.0f);
        PropertyValuesHolder valuesHolder4 = PropertyValuesHolder.ofFloat("scaleY", 1.0f, 0.3f, 1.0f);
        PropertyValuesHolder valuesHolder2 = PropertyValuesHolder.ofFloat("rotationX", 0.0f, 2 * 360.0f, 0.0F);
        PropertyValuesHolder valuesHolder5 = PropertyValuesHolder.ofFloat("rotationY", 0.0f, 2 * 360.0f, 0.0F);
        PropertyValuesHolder valuesHolder3 = PropertyValuesHolder.ofFloat("alpha", 1.0f, 0.0f, 1.0F);
        ObjectAnimator objectAnimator = ObjectAnimator.ofPropertyValuesHolder(mCameraTextureView, valuesHolder1, valuesHolder3, valuesHolder4);
        objectAnimator.setDuration(5000).start();
    }
}
