package com.jeryzhang.bitmap.camerademo.widget;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.jeryzhang.bitmap.camerademo.filter.ScreenFilter;
import com.jeryzhang.bitmap.camerademo.util.CameraHelper;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 控制了与它相关联的 GLSurfaceView 上绘制什么
 */
public class DouYinRender implements GLSurfaceView.Renderer ,SurfaceTexture.OnFrameAvailableListener{
    private DouYinView mView;
    private ScreenFilter mScreenFilter;
    private CameraHelper mCameraHelper;
    private SurfaceTexture mSurfaceTexture;
    private int[] mTextures;
    private float[] mtx = new float[16];

    public DouYinRender(DouYinView douYinView) {
        this.mView = douYinView;
    }

    /**
     * GLThread
     * 画布创建好了
     * @param gl
     * @param config
     */
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //初始化操作
        mCameraHelper = new CameraHelper(Camera.CameraInfo.CAMERA_FACING_BACK);

        //准备好摄像头绘制的画布
        //通过gl创建一个纹理id
        mTextures = new int[1];
        GLES20.glGenTextures(mTextures.length,mTextures,0);
        mSurfaceTexture = new SurfaceTexture(mTextures[0]);

        mSurfaceTexture.setOnFrameAvailableListener(this);

        //必须要glThread中进行初始化
        mScreenFilter = new ScreenFilter(mView.getContext());
    }

    /**
     * GLThread
     * 画布改变了
     * @param gl
     * @param width
     * @param height
     */
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //开启摄像头预览
        mCameraHelper.startPreview(mSurfaceTexture);
        //可以去获取摄像头数据
        mScreenFilter.onReady(width,height);

    }

    /**
     * GLThread
     * 开始画画
     * @param gl
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        //配置屏幕
        //清理理幕
        GLES20.glClearColor(0,0,0,0);
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        //把摄像头的数据输出
        //更新纹理
        mSurfaceTexture.updateTexImage();
        //获得变化矩阵
        mSurfaceTexture.getTransformMatrix(mtx);
        mScreenFilter.onDrawFrame(mTextures[0],mtx);
    }

    /**
     * OnFrameAvailableListener
     * 当有一个可用的帧时调用
     * @param surfaceTexture
     */
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        //因为使用的是按需渲染，所以当有一个可用的帧的时候就去请求渲染一次
        mView.requestRender();
    }
}

