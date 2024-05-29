#version 450
#extension GL_ARB_separate_shader_objects: enable

layout (location = 0) in vec2 inUV;

layout (location = 0) out vec4 outColor;

layout (binding = 0) uniform sampler2D texSampler;

float sampleFont(sampler2D tex, vec2 texCoord) {
    // Get the size of the texture
    ivec2 texSize = textureSize(tex, 0);

    // Calculate the texture coordinates for the four nearest texels
    vec2 texCoords = texCoord * vec2(texSize);
    ivec2 texelCoords = ivec2(texCoords);
    vec2 frac = fract(texCoords);

    // Fetch the four nearest texels
    float texel00 = texelFetch(tex, texelCoords, 0).r;
    float texel10 = texelFetch(tex, texelCoords + ivec2(1, 0), 0).r;
    float texel01 = texelFetch(tex, texelCoords + ivec2(0, 1), 0).r;
    float texel11 = texelFetch(tex, texelCoords + ivec2(1, 1), 0).r;

    // Perform bilinear interpolation
    float texel0 = mix(texel00, texel10, frac.x);
    float texel1 = mix(texel01, texel11, frac.x);
    float texel = mix(texel0, texel1, frac.y);

    return texel;
}

//@formatter:off
float kernel[9] = {
    0.111, 0.111, 0.111,
    0.111, 0.111, 0.111,
    0.111, 0.111, 0.111
};
//@formatter:on
vec4 applyKernel(sampler2D tex, vec2 texCoords) {
    vec2 texSize = textureSize(tex, 0);
    vec2 onePixel = vec2(1.0) / texSize;
    vec4 colorSum = vec4(0.0);
    int index = 0;
    float sum = 0;
    for (int i = -1; i <= 1; i++) {
        for (int j = -1; j <= 1; j++, index++) {
            vec2 offset = vec2(float(j), float(i)) * onePixel / 2.;
            colorSum += texture(tex, texCoords + offset) * kernel[index];
            sum += kernel[index];
        }
    }
    colorSum /= sum;
    return colorSum;
//    return texture(tex, texCoords);
}

float px(float distance) {
    float width = 0.1;
    float edge = 0.3;
    float alpha = smoothstep(width, width + edge, distance);
    //    width += 0.15;
    //    alpha = min(alpha, smoothstep(width, width - edge, distance));
    return alpha;
}

void main() {
    vec2 ts = textureSize(texSampler, 0);
    ts = (1. / ts);

    float left = applyKernel(texSampler, inUV.xy - vec2(ts.x / 1., 0)).x;
    float center = applyKernel(texSampler, inUV.xy).x;
    float right = applyKernel(texSampler, inUV.xy + vec2(ts.x / 1., 0)).x;

    ivec3 subpixel = ivec3(0, 1, 2);

    float distance = outColor.r;
    float l = px(left);
    float c = px(center);
    float r = px(right);
//    c = max(l, max(c, r));

    vec4 res = vec4(1);
    res[subpixel.r] = l;
    res[subpixel.g] = c;
    res[subpixel.b] = r;

    res.a = max(res.x, max(res.y, res.z));
//    res.xyz += c * 1;
//    res.xyz /= 2.;

    outColor = res;

    if (res.a == 0) discard;
}