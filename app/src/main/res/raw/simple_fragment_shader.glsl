
precision mediump float;
uniform sampler2D uTexture1;
varying vec2 vTextureCoord;

void main(){

    gl_FragColor = texture2D(uTexture1,vTextureCoord);
}