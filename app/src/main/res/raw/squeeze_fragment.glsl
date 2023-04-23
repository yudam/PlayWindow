precision mediump float;
varying vec2 vTextureCoord;
uniform sampler2D uTexture;
uniform sampler2D uTexture2;

uniform float progress;
uniform vec2 direction;

void main(){
    vec2 uv = vTextureCoord;
    float colorSeparation = 0.0;
    float y = 0.5 + (uv.y-0.5) / (1.0-progress);
    if (y < 0.0 || y > 1.0) {
        gl_FragColor = texture2D(uTexture2, uv);
    } else {
        vec2 fp = vec2(uv.x, y);
        vec2 off = progress * vec2(0.0, colorSeparation);
        vec4 c = texture2D(uTexture, fp);
        vec4 cn = texture2D(uTexture, fp - off);
        vec4 cp = texture2D(uTexture, fp + off);
        gl_FragColor = vec4(cn.r, c.g, cp.b, c.a);
    }
}



