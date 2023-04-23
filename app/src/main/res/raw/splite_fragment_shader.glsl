#extension GL_OES_EGL_image_external : require
precision highp float;
varying vec2 vTextureCoord;
uniform sampler2D sTexture;
uniform sampler2D pip;
uniform float center_x;   // 分割线的中心点x坐标[0,1]
uniform float center_y;   // 分割线的中心点y坐标[0,1]
uniform float gradient;   // 分割线角度的tan值
uniform float border;     // 分割线的宽度(下面对border的计算认为是宽度的一半，具体为啥不清楚)
uniform vec4 vBorderColor;// 分割线的颜色
uniform int isCenter;     // 表示是否 绘制中心画面
uniform int vertical;     // vertical==1表示分割线是90度或者270度，也就是垂直的
uniform int cropLeft;
uniform int cropRight;
void main()
{
    vec2 tempCoord = vTextureCoord;
    float ox = vTextureCoord.x;
    float oy = vTextureCoord.y;
    if (vertical == 1){
        // 根据纹理的x坐标在分割线的左右两端来计算应该绘制那一个纹理
        if (ox < center_x - border){
            if (cropLeft == 1) {
                tempCoord = vec2(vTextureCoord.x * 0.96528, vTextureCoord.y * 0.8298 + 0.092);
            }
            if (isCenter == 1){
                gl_FragColor = texture2D(sTexture, tempCoord + vec2(0.5 - center_x / 2.0, 0.0));
            } else {
                gl_FragColor = texture2D(sTexture, tempCoord);
            }
        } else if (ox >= center_x - border && ox <= center_x + border){
            gl_FragColor = vBorderColor;
        } else {
            if (cropRight == 1) {
                tempCoord = vec2(vTextureCoord.x * 0.96528, vTextureCoord.y * 0.8298 + 0.092);
            }
            if (isCenter == 1){
                gl_FragColor = texture2D(pip, tempCoord - vec2(center_x / 2.0, 0.0));
            } else {
                gl_FragColor = texture2D(pip, tempCoord);
            }
        }
    } else {
        // 计算出来的分割线的left和right边界（不太好理解，具体的以后在研究）
        float fy_left = gradient * (ox - center_x) + center_y + sqrt(pow(gradient * border, 2.0) + pow(border, 2.0));
        float fy_right = gradient * (ox - center_x) + center_y - sqrt(pow(gradient * border, 2.0) + pow(border, 2.0));
        // oy代表纹理y坐标,纹理的坐标范围是[0,1],原点位于左下角
        if (oy > fy_left){
            if (cropRight == 1) {
                tempCoord = vec2(vTextureCoord.x * 0.96528, vTextureCoord.y * 0.8298 + 0.092);
            }
            if (isCenter == 1){
                gl_FragColor = texture2D(sTexture, tempCoord - vec2(center_x / 2.0, 0.0));
            } else {
                gl_FragColor = texture2D(sTexture, tempCoord);
            }
        } else if (oy >= fy_right && oy <= fy_left){
            gl_FragColor = vBorderColor;
        } else {
            if (cropLeft == 1) {
                tempCoord = vec2(vTextureCoord.x * 0.96528, vTextureCoord.y * 0.8298 + 0.092);
            }
            if (isCenter == 1){
                gl_FragColor = texture2D(pip, tempCoord + vec2(0.5 - center_x / 2.0, 0.0));
            } else {
                gl_FragColor = texture2D(pip, tempCoord);
            }
        }
    }
}
