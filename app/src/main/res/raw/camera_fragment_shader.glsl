#extension GL_OES_EGL_image_external : require

precision mediump float;
varying vec2 v_TextureCoord;
uniform samplerExternalOES u_TextureUnit;

void main() {
    gl_FragColor = texture2D(u_TextureUnit, v_TextureCoord);
}