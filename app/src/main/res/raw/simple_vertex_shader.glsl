attribute vec4 aPosition;
attribute vec2 aTextCoord;
uniform mat4 aMvpMatrix;
varying vec2 vTextureCoord;
void main(){
    vTextureCoord = aTextCoord;
    gl_Position = aMvpMatrix * aPosition;
}