package com.jeryzhang.bitmap.camerademo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 1、绘制矩形
 * 2、绘制图片纹理
 * 3、读取显存的内容
 */
public class FourActivity extends AppCompatActivity {
    private static final int EXTERNAL_STORAGE_OK_CODE = 1;
    private GLSurfaceView mGLSurfaceView;
    private MyRenderer mRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_four);
//        init();
        getPermission();
    }

    private void getPermission() {
        if (Build.VERSION.SDK_INT > 22) {
            if (ContextCompat.checkSelfPermission(FourActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(FourActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(FourActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_OK_CODE);
                Log.i("MainActivity", "requestPermissions");
            } else {
                Log.i("MainActivity", "已经获取了权限");
                init();
            }
        } else {
            Log.i("MainActivity", "这个说明系统版本在6.0之下，不需要动态获取权限。");
            init();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == EXTERNAL_STORAGE_OK_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            init();
        }
    }

    private void init() {
        //渲染的“画布”
        mGLSurfaceView = findViewById(R.id.surface);
        //设置GLES版本
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        //MyRenderer实现我们的渲染逻辑
        //将GLSurfaceView 和 Renderer 连接起来
        mRenderer = new MyRenderer(this);
        mGLSurfaceView.setRenderer(mRenderer);
        //RENDERMODE_WHEN_DIRTY 和 RENDERMODE_CONTINUOUSLY
        //前者是懒惰渲染，需要手动调用 glSurfaceView.requestRender() 才会进行更新，而后者则是不停渲染。
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }


    private static class MyRenderer implements GLSurfaceView.Renderer {

        /**
         * uniform 由外部程序传递给 shader，就像是C语言里面的常量，shader 只能用，不能改；
         * attribute 是只能在 vertex shader 中使用的变量；
         * varying 变量是 vertex 和 fragment shader 之间做数据传递用的。
         */
        private static final String VERTEX_SHADER =
                "uniform mat4 uMVPMatrix;" +
                        "attribute vec4 vPosition;" +
                        "attribute vec2 a_texCoord;" +
                        "varying vec2 v_texCoord;" +
                        "void main() {" +
                        "  gl_Position = uMVPMatrix * vPosition;" +
                        "  v_texCoord = a_texCoord;" +
                        "}";
        private static final String FRAGMENT_SHADER =
                "precision mediump float;" +
                        "varying vec2 v_texCoord;" +
                        "uniform sampler2D s_texture;" +
                        "void main() {" +
                        "  gl_FragColor = texture2D(s_texture, v_texCoord);" +
                        "}";
        private static final float[] VERTEX = {   // in counterclockwise order:
                1, 1, 0,   // top right
                -1, 1, 0,  // top left
                -1, -1, 0, // bottom left
                1, -1, 0,  // bottom right
        };
        private static final short[] VERTEX_INDEX = {0, 1, 2, 0, 2, 3};
        private final ShortBuffer mVertexIndexBuffer;
        private final FloatBuffer mVertexBuffer;

        //指定了截取纹理区域的坐标
        private static final float[] TEX_VERTEX = {   // in clockwise order:
                //完整的区域
//                1, 0,  // bottom right
//                0, 0,  // bottom left
//                0, 1,  // top left
//                1, 1,  // top right

                0.5f, 0,  // bottom right
                0, 0,  // bottom left
                0, 0.5f,  // top left
                0.5f, 0.5f,  // top right
        };
        private final FloatBuffer mTexVertexBuffer;


        private int mProgram;
        private int mPositionHandle;
        private int mTexCoordHandle;
        private int mMatrixHandle;
        private int mTexSamplerHandle;
        private float[] mMVPMatrix = new float[16];
        private int mTexName;
        private Context mContext;
        private int width;
        private int height;

        MyRenderer(Context context) {
            mContext = context;
            mVertexBuffer = ByteBuffer.allocateDirect(VERTEX.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(VERTEX);
            mVertexBuffer.position(0);


            mVertexIndexBuffer = ByteBuffer.allocateDirect(VERTEX_INDEX.length * 2)
                    .order(ByteOrder.nativeOrder())
                    .asShortBuffer()
                    .put(VERTEX_INDEX);
            mVertexIndexBuffer.position(0);

            mTexVertexBuffer = ByteBuffer.allocateDirect(TEX_VERTEX.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(TEX_VERTEX);
            mTexVertexBuffer.position(0);
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
            mTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "a_texCoord");
            mMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
            mTexSamplerHandle = GLES20.glGetUniformLocation(mProgram, "s_texture");

            //启用vertex
            GLES20.glEnableVertexAttribArray(mPositionHandle);
            //绑定vertex坐标值
            GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false,
                    12, mVertexBuffer);


            GLES20.glEnableVertexAttribArray(mTexCoordHandle);
            GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0,
                    mTexVertexBuffer);

            int[] texNames = new int[1];
            //通过 glGenTextures 创建纹理
            GLES20.glGenTextures(1, texNames, 0);
            mTexName = texNames[0];
            Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.aaa);
            //通过 glActiveTexture 激活指定编号的纹理
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            //通过 glBindTexture 将新建的纹理和编号绑定起来
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexName);

            //对图片纹理设置一系列参数
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_REPEAT);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_REPEAT);

            //通过 texImage2D 把图片数据拷贝到纹理中。
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            bitmap.recycle();
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
            this.width = width;
            this.height = height;

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
            Matrix.translateM(mMVPMatrix, 0, 0f, 0f, -5f);
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
            GLES20.glUniform1i(mTexSamplerHandle, 0);

            //glDrawArrays/glDrawElements执行完毕之后，GPU 就在显存中处理好帧数据了，但此时并没有更新到 surface 上，
            //GLSurfaceView 会在调用 renderer.onDrawFrame 之后，调用 eglSwapBuffers，来把显存的帧数据更新到 surface 上的。
//            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
            // 用 glDrawElements 来绘制，mVertexIndexBuffer 指定了顶点绘制顺序
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, VERTEX_INDEX.length, GLES20.GL_UNSIGNED_SHORT, mVertexIndexBuffer);

//            sendImage(width, height);
        }


        static void sendImage(int width, int height) {
            ByteBuffer rgbaBuf = ByteBuffer.allocateDirect(width * height * 4);
            rgbaBuf.position(0);
            long start = System.nanoTime();
            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                    rgbaBuf);
            long end = System.nanoTime();
            Log.d("TryOpenGL", "glReadPixels: " + (end - start));
            saveRgb2Bitmap(rgbaBuf, Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/gl_dump_" + width + "_" + height + ".png", width, height);
        }

        static void saveRgb2Bitmap(Buffer buf, String filename, int width, int height) {
            Log.d("TryOpenGL", "Creating " + filename);
            BufferedOutputStream bos = null;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(filename));
                Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bmp.copyPixelsFromBuffer(buf);
                bmp.compress(Bitmap.CompressFormat.PNG, 90, bos);
                bmp.recycle();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (bos != null) {
                    try {
                        bos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void destroy() {
            GLES20.glDeleteTextures(1, new int[]{mTexName}, 0);
        }
    }

    //避免 activity pause 之后进行不必要的渲染
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //activity 销毁时，我们需要销毁 OpenGL 纹理
        mRenderer.destroy();
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