precision mediump float;
varying vec2 vTextureCoord;
uniform sampler2D uTexture1;
uniform sampler2D uTexture2;
uniform float progress;
uniform vec2 direction;
void main(){
    vec2 p = vTextureCoord + progress * sign(direction);
    float m = step(0.0, p.y) * step(p.y, 1.0) * step(0.0, p.x) * step(p.x, 1.0);
    vec4 color = mix(texture2D(uTexture2, vTextureCoord), texture2D(uTexture1, vTextureCoord), m);
    gl_FragColor = vec4(color);
}




