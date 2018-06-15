precision mediump float;

varying vec2 v_TextureCoord;

uniform sampler2D u_TextureUnit1;
uniform sampler2D u_TextureUnit2;

void main() {
    mediump vec4 textureColor = texture2D(u_TextureUnit1, v_TextureCoord);
    mediump vec4 textureColor2 = texture2D(u_TextureUnit2, v_TextureCoord);

    gl_FragColor = textureColor + textureColor2 - vec4(1.0);
}