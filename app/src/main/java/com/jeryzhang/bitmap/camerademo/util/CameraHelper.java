package com.jeryzhang.bitmap.camerademo.util;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;

public class CameraHelper implements Camera.PreviewCallback {
    private static final String TAG = "CameraHelper";
    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;

    private int mCameraId;
    private Camera mCamera;
    private SurfaceTexture mSurfaceTexture;
    private byte[] buffer;
    private Camera.PreviewCallback mPreviewCallback;

    public CameraHelper(int cameraId) {
        mCameraId = cameraId;
    }

    public void switchCamera() {
        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        } else {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        }
        stopPreview();
        startPreview(mSurfaceTexture);
    }

    public void setPreviewCallback(Camera.PreviewCallback mPreviewCallback) {
        this.mPreviewCallback = mPreviewCallback;
    }

    public int getmCameraId() {
        return mCameraId;
    }


    public void startPreview(SurfaceTexture surfaceTexture) {
        mSurfaceTexture = surfaceTexture;
        try {
            mCamera = Camera.open(mCameraId);
            //设置数据格式
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewFormat(ImageFormat.NV21);
            parameters.setPreviewSize(WIDTH, HEIGHT);
            mCamera.setParameters(parameters);

            //设置数据缓存区
            buffer = new byte[WIDTH * HEIGHT * 3];
            mCamera.addCallbackBuffer(buffer);
            mCamera.setPreviewCallback(this);

            //设置预览画面
            mCamera.setPreviewTexture(mSurfaceTexture);
            mCamera.startPreview();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopPreview() {
        if (mCamera != null) {
            //数据停止回调接口
            mCamera.setPreviewCallback(null);
            //停止预览
            mCamera.stopPreview();
            //释放摄像头
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 1、使用Camera的PreviewCallback预览回调接口
     * onPreviewFrame()获取的数据格式只能是NV21或NV12
     * 一般情况下NV21或NV12需要转成RGB格式然后进行处理，这样太耗时了
     * 2、基于以上缺点，使用SurfaceTexture来获取预览图像。
     * @param data
     * @param camera
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (null != mPreviewCallback) {
            mPreviewCallback.onPreviewFrame(data, camera);
        }
        camera.addCallbackBuffer(buffer);
    }
}
