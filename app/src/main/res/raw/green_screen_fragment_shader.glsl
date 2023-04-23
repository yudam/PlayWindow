// 绿幕扣像
precision highp float;
varying vec2 vTextureCoord;
uniform sampler2D sTexture;

uniform vec2 uChromaKey;
uniform vec2 uPixelSize;
uniform float uSmoothness;
uniform float uSimilarity;
uniform float uSpill;

mat4 yuv_mat = mat4(0.182586, 0.614231, 0.062007, 0.062745,
-0.100644, -0.338572, 0.439216, 0.501961,
0.439216, -0.398942, -0.040274, 0.501961,
0.000000, 0.000000, 0.000000, 1.000000);

vec4 SampleTexture(vec2 uv)
{
    return texture2D(sTexture, uv);
}

float saturate(float x)
{
    return clamp(x, 0.0, 1.0);
}

vec3 saturate(vec3 x)
{
    return clamp(x, vec3(0.0, 0.0, 0.0), vec3(1.0, 1.0, 1.0));
}

float GetChromaDist(vec3 rgb)
{
    vec4 yuvx = vec4(rgb.rgb, 1.0) * yuv_mat;
    return distance(uChromaKey, yuvx.yz);
}

float GetBoxFilteredChromaDist(vec3 rgb, vec2 texCoord)
{
    float distVal = GetChromaDist(rgb);
    return distVal;
}

vec4 ProcessChromaKey(vec4 rgba, vec2 uv)
{
    float chromaDist = GetBoxFilteredChromaDist(rgba.rgb, uv);
    float baseMask = chromaDist - uSimilarity;
    float fullMask = pow(saturate(baseMask / uSmoothness), 1.5);
    float spillVal = pow(saturate(baseMask / uSpill), 1.5);
    rgba.a *= fullMask;
    float desat = (rgba.r * 0.2126 + rgba.g * 0.7152 + rgba.b * 0.0722);
    rgba.rgb = saturate(vec3(desat, desat, desat)) * (1.0 - spillVal) + rgba.rgb * spillVal;
    return rgba;
}

void main() {
    vec4 tc = texture2D(sTexture, vTextureCoord);
    gl_FragColor = ProcessChromaKey(tc, vTextureCoord);
}

