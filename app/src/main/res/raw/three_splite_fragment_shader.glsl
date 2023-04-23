#extension GL_OES_EGL_image_external : require
precision highp float;
varying vec2 vTextureCoord;
uniform sampler2D sTexture;
uniform sampler2D pip;
uniform sampler2D subC;
uniform float center_x;
uniform float center_y;
uniform vec4 vBorderColor;
uniform float border;
uniform int isCenter;
uniform int vertical;
uniform int cropLeft;
uniform int cropRight;
void main()
{
    vec2 tempCoord = vTextureCoord;
    float ox = vTextureCoord.x;
    float oy = vTextureCoord.y;
    if (vertical == 1){

        if (ox < center_x-1.0/6.0 - border){
            if (isCenter == 1){
                gl_FragColor = texture2D(sTexture, tempCoord + vec2(0.5 - center_x / 3.0, 0.0));
            } else {
                gl_FragColor = texture2D(sTexture, tempCoord);
            }
        } else if (ox>=center_x-1.0/6.0 + border && ox<=center_x+1.0/6.0 - border){
            gl_FragColor = texture2D(pip, tempCoord);
        } else if (ox>=center_x+1.0/6.0 + border){
            if (isCenter == 1){
                gl_FragColor = texture2D(subC, tempCoord - vec2(0.5 - center_x / 3.0, 0.0));
            } else {
                gl_FragColor = texture2D(subC, tempCoord);
            }
        } else {
            gl_FragColor = vBorderColor;
        }
    }
}