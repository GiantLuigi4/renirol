#version 450
#extension GL_ARB_separate_shader_objects: enable

layout (location = 0) in vec2 inPosition;

layout (location = 0) out vec2 outUV;


layout (location = 1) in vec2 Offset;
layout (location = 2) in vec4 DrawBounds;
layout (location = 3) in vec4 UVBounds;

void main() {
    // TODO: transform and such
    vec2 vPos = inPosition.xy * DrawBounds.xy + (1 - inPosition.xy) * DrawBounds.zw + (Offset.xy / 70);
    vPos /= 200;
    gl_Position = vec4(((vPos - 1.)), 0, 1);
    outUV = inPosition * UVBounds.zw + ((1 - inPosition) * UVBounds.xy);
}
