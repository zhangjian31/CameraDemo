package com.jeryzhang.bitmap.camerademo;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 1、在xml中添加GlSurfaceView
 * 2、创建渲染器类实现GlSurfaceView.Renderer
 * 3、清除画布，并创建一个纹理并绑定到。
 * 4、创建一个用来最后显示的SurfaceTexture来显示处理后的数据。
 * 5、创建Opengl ES程序并添加着色器到该程序中，创建openGl程序的可执行文件，并释放shader资源。
 * 6、打开摄像头，并配置相关属性。设置预览视图，并开启预览。
 * 7、添加程序到ES环境中，并设置及启用各类句柄。
 * 8、在onDrawFrame中进行画布的清理及绘制最新的数据到纹理图形中。
 * 9、设置一个SurfaceTexture.OnFrameAvailableListener的回调来通知GlSurfaceview渲染新的帧数据。
 *
 * 顶点缓冲区VBO使用
 */
public class CameraGlSurfaceShowActivity extends AppCompatActivity implements SurfaceTexture.OnFrameAvailableListener {
    private GLSurfaceView mCameraGlsurfaceView;
    private SurfaceTexture mSurfaceTexture;
    private Camera camera;
    private int cameraId = 1;
    private MyRender mRender;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glsurfaceview);
        mCameraGlsurfaceView = findViewById(R.id.camera_glsurface_view);
        //在setRenderer()方法前调用此方法
        mCameraGlsurfaceView.setEGLContextClientVersion(2);

        mRender = new MyRender();
        mCameraGlsurfaceView.setRenderer(mRender);
        mCameraGlsurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mCameraGlsurfaceView.requestRender();
    }

    private class MyRender implements GLSurfaceView.Renderer {
//        private final String vertexShaderCode = "uniform mat4 textureTransform;\n" +
//                "attribute vec2 inputTextureCoordinate;\n" +
//                "attribute vec4 position;            \n" +//NDK坐标点
//                "varying   vec2 textureCoordinate; \n" +//纹理坐标点变换后输出
//                "\n" +
//                " void main() {\n" +
//                "     gl_Position = position;\n" +
//                "     textureCoordinate = inputTextureCoordinate;\n" +
//                " }";
//
//        private final String fragmentShaderCode = "#extension GL_OES_EGL_image_external : require\n" +
//                "precision mediump float;\n" +
//                "uniform samplerExternalOES videoTex;\n" +
//                "varying vec2 textureCoordinate;\n" +
//                "\n" +
//                "void main() {\n" +
//                "    vec4 tc = texture2D(videoTex, textureCoordinate);\n" +
//                "    float color = tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11;\n" +  //所有视图修改成黑白
//                "    gl_FragColor = vec4(color,color,color,1.0);\n" +
////                "    gl_FragColor = vec4(tc.r,tc.g,tc.b,1.0);\n" +
//                "}\n";

//        private String vertexShaderCode;
//        private String fragmentShaderCode;

        private FloatBuffer mPosBuffer;
        private FloatBuffer mTexBuffer;
        private float[] mPosCoordinate = {-1, -1, -1, 1, 1, -1, 1, 1};
        private float[] mTexCoordinateBackRight = {1, 1, 0, 1, 1, 0, 0, 0};//顺时针转90并沿Y轴翻转  后摄像头正确，前摄像头上下颠倒
        private float[] mTexCoordinateForntRight = {0, 1, 1, 1, 0, 0, 1, 0};//顺时针旋转90  后摄像头上下颠倒了，前摄像头正确

        public int mProgram;
        public boolean mBoolean = false;

        public MyRender() {
            Matrix.setIdentityM(mProjectMatrix, 0);
            Matrix.setIdentityM(mCameraMatrix, 0);
            Matrix.setIdentityM(mMVPMatrix, 0);
            Matrix.setIdentityM(mTempMatrix, 0);
        }

        private int loadShader(int type, String shaderCode) {
            int shader = GLES20.glCreateShader(type);
            // 添加上面编写的着色器代码并编译它
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
            return shader;
        }

        private void creatProgram() {
            //通常做法
            String vertexSource = read(CameraGlSurfaceShowActivity.this, "vertex_texture.glsl");
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
            String fragmentSource = read(CameraGlSurfaceShowActivity.this, "fragment_texture.glsl");
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
//            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
//            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
            // 创建空的OpenGL ES程序
            mProgram = GLES20.glCreateProgram();

            // 添加顶点着色器到程序中
            GLES20.glAttachShader(mProgram, vertexShader);

            // 添加片段着色器到程序中
            GLES20.glAttachShader(mProgram, fragmentShader);

            // 创建OpenGL ES程序可执行文件
            GLES20.glLinkProgram(mProgram);

            // 释放shader资源
            GLES20.glDeleteShader(vertexShader);
            GLES20.glDeleteShader(fragmentShader);
        }

        private FloatBuffer convertToFloatBuffer(float[] buffer) {
            FloatBuffer fb = ByteBuffer.allocateDirect(buffer.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            fb.put(buffer);
            fb.position(0);
            return fb;
        }

        private int uPosHandle;
        private int aTexHandle;
        private int mMVPMatrixHandle;
        private float[] mProjectMatrix = new float[16];
        private float[] mCameraMatrix = new float[16];
        private float[] mMVPMatrix = new float[16];
        private float[] mTempMatrix = new float[16];

        //添加程序到ES环境中
        private void activeProgram() {
            // 将程序添加到OpenGL ES环境
            GLES20.glUseProgram(mProgram);

            mSurfaceTexture.setOnFrameAvailableListener(CameraGlSurfaceShowActivity.this);
            // 获取顶点着色器的位置的句柄
            uPosHandle = GLES20.glGetAttribLocation(mProgram, "position");
            aTexHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
            mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "textureTransform");

            mPosBuffer = convertToFloatBuffer(mPosCoordinate);
            if (cameraId == 0) {
                mTexBuffer = convertToFloatBuffer(mTexCoordinateBackRight);
            } else {
                mTexBuffer = convertToFloatBuffer(mTexCoordinateForntRight);
            }

            GLES20.glVertexAttribPointer(uPosHandle, 2, GLES20.GL_FLOAT, false, 0, mPosBuffer);
            GLES20.glVertexAttribPointer(aTexHandle, 2, GLES20.GL_FLOAT, false, 0, mTexBuffer);

            // 启用顶点位置的句柄
            GLES20.glEnableVertexAttribArray(uPosHandle);
            GLES20.glEnableVertexAttribArray(aTexHandle);
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            mSurfaceTexture = new SurfaceTexture(createOESTextureObject());
            creatProgram();
//            mProgram = ShaderUtils.createProgram(CameraGlSurfaceShowActivity.this, "vertex_texture.glsl", "fragment_texture.glsl");
            camera = Camera.open(cameraId);
            try {
                camera.setPreviewTexture(mSurfaceTexture);
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
            activeProgram();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            Matrix.scaleM(mMVPMatrix, 0, 1, -1, 1);
            float ratio = (float) width / height;
            Matrix.orthoM(mProjectMatrix, 0, -1, 1, -ratio, ratio, 1, 7);// 3和7代表远近视点与眼睛的距离，非坐标点
            Matrix.setLookAtM(mCameraMatrix, 0, 0, 0, 3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);// 3代表眼睛的坐标点
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectMatrix, 0, mCameraMatrix, 0);

        }

        @Override
        public void onDrawFrame(GL10 gl) {
            if (mBoolean) {
                activeProgram();
                mBoolean = false;
            }
            if (mSurfaceTexture != null) {
                //清除深度缓冲与颜色缓冲
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
                //更新纹理
                mSurfaceTexture.updateTexImage();
                GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mPosCoordinate.length / 2);
            }
        }

        public int createOESTextureObject() {
            int[] tex = new int[1];
            //生成一个纹理
            GLES20.glGenTextures(1, tex, 0);
            //将此纹理绑定到外部纹理上
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);
            //设置纹理过滤参数
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
            return tex[0];
        }
    }

    public void onSwitch(View view) {
        cameraId ^= 1;
        if (camera != null) {
            camera.stopPreview();
            camera.release();
        }
        mRender.mBoolean = true;
        camera = Camera.open(cameraId);
        try {
            camera.setPreviewTexture(mSurfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
        camera.startPreview();

    }

    public void onAnimal(View view) {
        PropertyValuesHolder valuesHolder1 = PropertyValuesHolder.ofFloat("scaleX", 1.0f, 0.5f, 1.0f);
        PropertyValuesHolder valuesHolder4 = PropertyValuesHolder.ofFloat("scaleY", 1.0f, 0.5f, 1.0f);
        PropertyValuesHolder valuesHolder5 = PropertyValuesHolder.ofFloat("rotationY", 0.0f, 360.0f, 0.0F);
        ObjectAnimator objectAnimator = ObjectAnimator.ofPropertyValuesHolder(mCameraGlsurfaceView, valuesHolder1, valuesHolder4, valuesHolder5);
        objectAnimator.setDuration(3000).start();
    }


    public static String read(Context context, String fileName) {
        String result = null;
        try {
            InputStream is = context.getResources().getAssets().open("Shader/" + fileName);
            int length = is.available();
            byte[] buffer = new byte[length];
            is.read(buffer);
            result = new String(buffer, "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }
}
