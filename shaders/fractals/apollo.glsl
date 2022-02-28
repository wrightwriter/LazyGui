#version 120

uniform sampler2D texture;
uniform vec2 resolution;
uniform float time;
uniform float scaleBase;
uniform float scaleMult;
uniform float range;
uniform int   iterations;
uniform float ampBase;
uniform float ampMult;
uniform float offsetX;
uniform float offsetY;
uniform float radius;
uniform float power;

const float pi = 3.14159;

// pretty gradient by iq

vec3 a = vec3(0.5);
vec3 b = vec3(0.5);
vec3 c = vec3(1);
vec3 d = vec3(0,0.1,0.2);

vec3 palette( in float t, in vec3 a, in vec3 b, in vec3 c, in vec3 d )
{
    return a + b*cos( 6.28318*(c*t+d) );
}

float sdCircle(vec2 uv, float r){
    return length(uv.xy) - r;
}

float sdLine(float x){
    return abs(x);
}

mat2 rotate2D(float angle){
    return mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
}

void main() {
    vec2 uv = (gl_FragCoord.xy - 0.5 * resolution.xy) / resolution.y;
    vec3 p = vec3(uv.xy, 1.);
    float scale = scaleBase;
    float t = time;
//    p.xy *= rotate2D(-length(sin(p*2.)) + time);
    p /= dot(p.xy, p.xy);
    p *= scale;
    p.xy /= dot(p.xy, p.xy);
    p.xy *= rotate2D(length(0.1*sin(p.xy*2. + t)));
    p = fract(p) - 0.5;
    p.xy /= p.z;
    float lineGrid = min(sdLine(p.x), sdLine(p.y));
    float n = smoothstep(radius, 0.0, lineGrid);
    vec3 col = palette(pow(n, power), a,b,c,d);
    gl_FragColor = vec4(col, 1.);
}


























