precision mediump float;

varying vec4 v_Color;
varying vec2 v_Texture;
uniform sampler2D u_TextureUnit;

void main()
{
   vec4 textureColor = texture2D(u_TextureUnit, v_Texture);
   gl_FragColor = textureColor;
  // gl_FragColor = vec4(0,0,1,1);
}