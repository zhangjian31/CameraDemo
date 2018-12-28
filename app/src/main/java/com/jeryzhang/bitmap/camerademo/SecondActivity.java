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

        /**
         * 常用于进行初始化工作，只会被回调一次
         *
         * @param unused
         * @param config
         */
        @Override
        public void onSurfaceCreated(GL10 unused, EGLConfig config) {
            //OpenGL 支持两种颜色模式：一种是RGBA，一种是颜色索引模式。
            //glClearColor设置颜色缓存的清除值
            //glClearColor就是用来设置这个“底色”的，即所谓的背景颜色。glClearColor ( ) 只起到Set 的作用，并不Clear 任何。
            GLES20.glClearColor(0.0f, 1.0f, 0.0f, 0.0f);

            //OpenGL需要加载GLSL程序，让GPU进行绘制。所以我们需要定义shader代码，并在初始化时（也就是onSurfaceCreated回调中）加载

            //创建GLSL程序
            mProgram = GLES20.glCreateProgram();
            //加载shader代码
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
            //把之前编译的着色器附加到程序对象上
            GLES20.glAttachShader(mProgram, vertexShader);
            GLES20.glAttachShader(mProgram, fragmentShader);

            //链接GLSL程序,链接着色器至一个程序的时候，它会把每个着色器的输出链接到下个着色器的输入。
            GLES20.glLinkProgram(mProgram);
            //激活这个程序对象,之后每个着色器调用和渲染调用都会使用这个程序对象（也就是之前写的着色器)了
            GLES20.glUseProgram(mProgram);

            //把着色器对象链接到程序对象以后，记得删除着色器对象，我们不再需要它们了
            GLES20.glDeleteProgram(mProgram);

            //获取shader代码中的变量索引
            //Java代码需要获取shader代码中定义的变量索引，用于在后面的绘制代码中进行赋值，
            //变量索引在GLSL程序的生命周期内（链接之后和销毁之前），都是固定的，只需要获取一次。
            //获取顶点着色器中，指定attribute名的index。以后就可以通过这个index向顶点着色器中传递数据。获取失败则返回-1
            mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
            mMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");

            //启用顶点属性,开始mPositionHandle=0
            GLES20.glEnableVertexAttribArray(mPositionHandle);
            //绑定vertex坐标值,设置顶点属性指针，size每个顶点由几个值组成，stride步幅-一个顶点的步幅3*4（float大小）
            GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 12, mVertexBuffer);
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
            //glClear将缓存清除为预先的设置值,用来清除屏幕颜色，即将屏幕的所有像素点都还原为 “底色 ”。
            //GL_COLOR_BUFFER_BIT 是缓冲标志位，表明需要清除的缓冲是颜色缓冲
            /**
             *
             * 清除颜色缓冲区的作用是防止缓冲区中原有的颜色信息影响本次绘图（注意：即使认为可以直接覆盖原值，也是有可能会有影响），
             * 当绘图区域为整个窗口时，就是通常看到的，颜色缓冲区的清除值就是窗口的背景颜色。所以，这两条清除指令并不是必须的，
             * 比如对于静态画面只需设置一次，比如不需要背景色 / 背景色为白色。
             * 另外，glClear ( ) 比手动涂抹一个背景画布效率高且省力，所以通常使用这种方式。
             *
             *        mask	                说明
             *    GL_COLOR_BUFFER_BIT	指定当前被激活为写操作的颜色缓存
             *    GL_DEPTH_BUFFER_BIT	指定深度缓存
             *    GL_ACCUM_BUFFER_BIT	指定累加缓存
             *    GL_STENCIL_BUFFER_BIT
             */
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);


            //在绘制的时候向shader传递
            GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mMVPMatrix, 0);

            //glDrawArrays/glDrawElements执行完毕之后，GPU 就在显存中处理好帧数据了，但此时并没有更新到 surface 上，
            //GLSurfaceView 会在调用 renderer.onDrawFrame 之后，调用 eglSwapBuffers，来把显存的帧数据更新到 surface 上的。
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
        }

        /**
         * 把着色器源码附加到着色器对象上，然后编译它
         *
         * @param type
         * @param shaderCode
         * @return
         */
        private int loadShader(int type, String shaderCode) {
            //创建一个着色器对象
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
            return shader;
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