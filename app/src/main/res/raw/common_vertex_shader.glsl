

attribute vec4 a_Position;
attribute vec4 a_TextureCoord;
uniform mat4 a_mvpMatrix;
varying vec4 vTextureCoord;

void main() {

    vTextureCoord = a_TextureCoord;
    gl_Position = a_mvpMatrix * a_Position;

}
