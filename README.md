SurfaceView
继承自View,拥有View的大部分属性，但是由于holder的存在，不能设置透明度。 
优点：可以在一个独立的线程中进行绘制，不会影响主线程，使用双缓冲机制，播放视频时画面更流畅 
缺点：surface的显示不受View属性的控制，不能将其放在ViewGroup中，SurfaceView不能嵌套使用。

GlSurfaceView继承自SurfaceView类，专门用来显示OpenGL渲染的，简单理解可以显示视频，图像及3D场景这些的。

SurfaceTexture和SurfaceView功能类似，区别是，SurfaceTexure可以不显示在界面中。使用OpenGl对图片流进行美化，添加水印，滤镜这些操作的时候我们都是通过SurfaceTexre去处理，
处理完之后再通过GlSurfaceView显示。缺点，可能会导致个别帧的延迟。本身管理着BufferQueue,所以内存消耗会多一点。

TextureView
继承自View，必须在开启硬件加速的设备中使用（保守估计目前百分之九十的Android设备都开启了），TextureView通过setSurfaceTextureListener的回调在子线程中进行更新UI.
优点：支持动画效果。 
缺点：在5.0之前在主线程渲染，在5.0之后在单独线程渲染。


SurfaceView:大量画布更新(游戏绘制)
TextureView:视频播放，相机应用