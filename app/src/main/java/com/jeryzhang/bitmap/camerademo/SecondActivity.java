package com.jeryzhang.bitmap.camerademo;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class SecondActivity extends AppCompatActivity {

    private GLSurfaceView mGLSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

//        if (!Utils.supportGlEs20(this)) {
//            Toast.makeText(this, "GLES 2.0 not supported!", Toast.LENGTH_LONG).show();
//            finish();
//            return;
//        }

        //渲染的“画布”
        mGLSurfaceView = findViewById(R.id.surface);
        //设置GLES版本
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        //MyRenderer实现我们的渲染逻辑
        //将GLSurfaceView 和 Renderer 连接起来
        mGLSurfaceView.setRenderer(new MyRenderer());
        //RENDERMODE_WHEN_DIRTY 和 RENDERMODE_CONTINUOUSLY
        //前者是懒惰渲染，需要手动调用 glSurfaceView.requestRender() 才会进行更新，而后者则是不停渲染。
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLSurfaceView.onResume();
    }

    private static class MyRenderer implements GLSurfaceView.Renderer {

        private static final String VERTEX_SHADER =
                "attribute vec4 vPosition;\n"
                        + "uniform mat4 uMVPMatrix;\n"
                        + "void main() {\n"
                        + "  gl_Position = uMVPMatrix * vPosition;\n"
                        + "}";
        private static final String FRAGMENT_SHADER =
                "precision mediump float;\n"
                        + "void main() {\n"
                        + "  gl_FragColor = vec4(0.5, 0, 0, 1);\n"
                        + "}";
        private static final float[] VERTEX = {   // in counterclockwise order:
                0, 1, 0,  // top
                -0.5f, -1, 0,  // bottom left
                1, -1, 0,  // bottom right
        };

        private final FloatBuffer mVertexBuffer;

        private int mProgram;
        private int mPositionHandle;
        private int mMatrixHandle;
        private float[] mMVPMatrix = new float[16];

        MyRenderer() {
            mVertexBuffer = ByteBuffer.allocateDirect(VERTEX.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(VERTEX);
            mVertexBuffer.position(0);
        }

        static int loadShader(int type, String shaderCode) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
            return shader;
        }

        /**
         * 常用于进行初始化工作，只会被回调一次
         *
         * @param unused
         * @param config
         */
        @Override
        public void onSurfaceCreated(GL10 unused, EGLConfig config) {
            //OpenGL需要加载GLSL程序，让GPU进行绘制。所以我们需要定义shader代码，并在初始化时（也就是onSurfaceCreated回调中）加载
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

            //创建GLSL程序
            mProgram = GLES20.glCreateProgram();
            //加载shader代码
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
            //attatch shader代码
            GLES20.glAttachShader(mProgram, vertexShader);
            GLES20.glAttachShader(mProgram, fragmentShader);
            //链接GLSL程序
            GLES20.glLinkProgram(mProgram);
            //使用GLSL程序
            GLES20.glUseProgram(mProgram);

            //获取shader代码中的变量索引
            //Java代码需要获取shader代码中定义的变量索引，用于在后面的绘制代码中进行赋值，
            //变量索引在GLSL程序的生命周期内（链接之后和销毁之前），都是固定的，只需要获取一次。
            mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
            mMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");

            //启用vertex
            GLES20.glEnableVertexAttribArray(mPositionHandle);
            //绑定vertex坐标值
            GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false,
                    12, mVertexBuffer);
        }

        /**
         * 每次surface尺寸变化时被回调，注意，第一次得知surface的尺寸时也会回调
         *
         * @param unused
         * @param width
         * @param height
         */
        @Override
        public void onSurfaceChanged(GL10 unused, int width, int height) {
            //设置Screen space的大小
            GLES20.glViewport(0, 0, width, height);

            //计算变换矩阵
            //perspectiveM 就是透视投影,得到投影矩阵mMVPMatrix,用于投影变换
            /**
             * @param m  float array 保存变换矩阵的数组
             * @param offset m中开始保存的下标偏移量
             * @param fovy y 轴的 field of view 值，视角大小
             * @param aspect Screen space 的宽高比
             * @param zNear 视锥体近平面的z轴坐标了
             * @param zFar 视锥体远平面的z轴坐标了
             */
            Matrix.perspectiveM(mMVPMatrix, 0, 45, (float) width / height, 0.1f, 100f);
            //由于历史原因，Matrix.perspectiveM 会让 z 轴方向倒置，所以左乘投影矩阵之后，顶点 z 坐标需要在 -zNear~-zFar 范围内才会可见。
            //前面我们顶点的 z 坐标都是 0，我们可以把它修改为 -0.1f~-100f 之间的值，也可以通过一个位移变换来达到此目的。
            Matrix.translateM(mMVPMatrix, 0, 0f, 0f, -2.5f);
        }


        /**
         * 绘制每一帧的时候回调
         * 绘制操作，绘制的过程其实就是为shader代码变量赋值，并调用绘制命令的过程
         *
         * @param unused
         */
        @Override
        public void onDrawFrame(GL10 unused) {
            //由于顶点坐标已经在onSurfaceCreated绑定过了，所以这里无需进行变量赋值，直接调用绘制指令即可。
            //通过 GLES20.glDrawArrays 或者 GLES20.glDrawElements 开始绘制
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);


            //在绘制的时候向shader传递
            GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mMVPMatrix, 0);

            //glDrawArrays/glDrawElements执行完毕之后，GPU 就在显存中处理好帧数据了，但此时并没有更新到 surface 上，
            //GLSurfaceView 会在调用 renderer.onDrawFrame 之后，调用 eglSwapBuffers，来把显存的帧数据更新到 surface 上的。
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
        }
    }
    /**
     * 投影变换
     * 用于把 View space 的坐标转换为 Clip space 的坐标
     * 使用较多的是正投影和透视投影
     * 透视投影：Matrix.perspectiveM
     * 通常坐标系的变换都是对顶点坐标进行矩阵左乘运算，因此我们需要修改我们的 vertex shader 代码：
     private static final String VERTEX_SHADER =
     "attribute vec4 vPosition;\n"
     + "uniform mat4 uMVPMatrix;\n"
     + "void main() {\n"
     + "  gl_Position = uMVPMatrix * vPosition;\n"
     + "}";
     *
     *
     *
     */
}