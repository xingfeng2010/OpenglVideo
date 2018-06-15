precision mediump float;
varying vec2 v_TextureCoord;
uniform sampler2D u_TextureUnit;

void main() {
    vec4 textureColor = texture2D(u_TextureUnit, v_TextureCoord);
    float gray = textureColor.r * 0.299 + textureColor.g * 0.587 + textureColor.b * 0.114;
    gl_FragColor = vec4(gray,gray,gray,textureColor.w);
}