package com.jeryzhang.bitmap.camerademo.widget;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

/**
 * 放置图形的View容器
 * GLSurfaceView只是一种选择，比较适合于全屏绘制图形或者近似全屏绘制
 * 其他可以选择的还有 TextureView和SurfaceView
 */
public class DouYinView extends GLSurfaceView {
    public DouYinView(Context context) {
        this(context, null);
    }

    public DouYinView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //设置EGL版本，如果使用OpenGL ES 2.0，还需要加一句声明：
        setEGLContextClientVersion(2);
        //设置渲染器
        setRenderer(new DouYinRender(this));

        //设置渲染模式:连续渲染、按需渲染
        //默认渲染方式为RENDERMODE_CONTINUOUSLY
        //当设置为RENDERMODE_CONTINUOUSLY时渲染器会不停地渲染场景
        // 当设置为RENDERMODE_WHEN_DIRTY时只有在创建和调用requestRender()时才会刷新。
        //一般设置为RENDERMODE_WHEN_DIRTY方式，这样不会让CPU一直处于高速运转状态，提高手机电池使用时间和软件整体性能。
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }
}
