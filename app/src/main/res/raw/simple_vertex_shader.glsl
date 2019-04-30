precision mediump float;
attribute vec4 a_Position;
attribute vec4 a_Color;
attribute vec2 a_Texture;

varying vec4 v_Color;
varying vec2 v_Texture;

void main()
{
    v_Color = a_Color;
    gl_Position = a_Position;
    v_Texture = a_Texture;
}