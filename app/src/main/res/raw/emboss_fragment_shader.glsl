varying highp vec2 v_TextureCoord;
uniform sampler2D u_TextureUnit;
const vec2 texSize = vec2(1920, 1080);

void main() {
    vec2 tex = v_TextureCoord;
    vec2 upLeftUV = vec2(tex.x - 1.0 / texSize.x, tex.y - 1.0/texSize.y);
    vec4 textureColor = texture2D(u_TextureUnit, v_TextureCoord);
    vec4 upColor = texture2D(u_TextureUnit, upLeftUV);
    vec4 delColor = textureColor - upColor;
    float h = 0.3 * delColor.x + 0.59*delColor.y + 0.11*delColor.z;
    vec4 bkColor = vec4(0.5, 0.5, 0.5, 1.0);
    gl_FragColor = vec4(h, h, h,0.0) + bkColor;
}