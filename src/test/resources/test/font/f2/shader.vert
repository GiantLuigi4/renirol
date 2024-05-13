#version 450
#extension GL_ARB_separate_shader_objects : enable

layout(location = 0) in vec2 inPosition;

layout(location = 0) out vec2 outUV;

void main() {
    // TODO: transform and such
    gl_Position = vec4(inPosition, 0, 1);
    outUV = inPosition;
}
