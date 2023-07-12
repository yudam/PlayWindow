precision mediump float;
varying vec2 vTextureCoord;
uniform sampler2D uTexture;
uniform sampler2D uTexture2;

uniform float progress;// 0.0 ~ 1.0
uniform vec2 direction;// vec2(1.0, 0.0)水平; vec2(0.0, 1.0)垂直;

void main() {
    float count = 10.0;
    float smoothness = 0.5;
    vec2 p = vTextureCoord;
    float pr = smoothstep(-smoothness, 0.0, p.x - progress * (1.0 + smoothness));
    float s = step(pr, fract(count * p.x));
    vec4 color = mix(texture2D(uTexture, p), texture2D(uTexture2, p), s);
    gl_FragColor = vec4(color);
}
