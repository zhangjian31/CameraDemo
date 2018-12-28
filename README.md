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



1、Uniforms and Attributes
    Uniforms 是一个program 中统一分配的，vertext 和fragment中同名的Uniform必须同类型。对应于不经常变化的变量（用于存储只读常量值的变量）。
    Attributes 变化率高的变量。主要用来定义输入的每个点属性。
    Uniforms and Attributes 在shader中通过location 和 name 来对应的。
2、数据类型
    1）三类基本数据类型：float , int , boolean
    2）复合类型：浮点、整型、布尔向量 vec2 , vec3,vec4。vector访问方式有以下两种：
        （1）.操作：数学{x, y, z, w}, 颜色{r, g, b, a}或 纹理坐标{s, t, r, q}，但不能混用，举例如下：

                vec3 myVec3 = vec3(0.0, 1.0, 2.0); // myVec3 = {0.0, 1.0, 2.0}
                vec3 temp;
                temp = myVec3.xyz; // temp = {0.0, 1.0, 2.0}
                temp = myVec3.xxx; // temp = {0.0, 0.0, 0.0}
                temp = myVec3.zyx; // temp = {2.0, 1.0, 0.0}
        （2）[ ]操作：[0]对应x，[1]对应y，[2]对应z，[3]对应w。[ ]中只能为常量或uniform变量，不能为整数量变量（如：i，j，k）。

    3）矩阵：mat2, mat3,mat4 （按列顺序存储）
          mat3 myMat3 = mat3(1.0, 0.0, 0.0,  // 第一列
                                      0.0, 1.0, 0.0,  // 第二列
                                      0.5, 1.0, 1.0); // 第三列
         可用[ ]或.操作符来访问：
         mat4 myMat4 = mat4(1.0);   // Initialize diagonal to 1.0 (identity)
         vec4 col0 = myMat4[0];        // Get col0 vector out of the matrix
         float m1_1 = myMat4[1][1];  // Get element at [1][1] in matrix
         float m2_2 = myMat4[2].z;   // Get element at [2][2] in matrix
    4）常量
         const float zero = 0.0;
         const float pi = 3.14159;
         const vec4 red = vec4(1.0, 0.0, 0.0, 1.0);
         const mat4 identity = mat4(1.0);
    5）结构体： 用基本类型和复合类型构建结构体。

         struct fogStruct
         {
             vec4 color;
             float start;
             float end;
         } fogVar;
         fogVar = fogStruct(vec4(0.0, 1.0, 0.0, 0.0), // color
                                      0.5, // start
                                      2.0); // end
         vec4 color = fogVar.color;
         float start  = fogVar.start;
         float end   = fogVar.end;
    6）数组：类似于C语言，索引从0开始。在创建时不能被初始化，索引只能为常量或uniform变量。

         float floatArray[4];
         vec4 vecArray[2];
    7）操作
         支持的操作有：*,/,+,-,++,--,=,+=, -=, *=, /=,==, !=, <, >, <=, >=,&&,^^,||

矩阵的乘法
C=AB
两个矩阵可乘的条件：矩阵A的列数==矩阵B的行数
矩阵的结果：【矩阵A的行数，矩阵B的列数】
