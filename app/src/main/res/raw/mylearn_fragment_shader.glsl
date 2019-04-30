precision mediump float;

varying vec4 v_Color;
varying vec2 v_Texture;
uniform sampler2D u_TextureUnit;

uniform vec2 uResolution;

void main()
{
   vec2 position = gl_FragCoord.xy / uResolution;
    float gradient = position.x;
    gl_FragColor = vec4(0., gradient, 0., 1.);
}