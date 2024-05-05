#version 450
#extension GL_ARB_separate_shader_objects : enable

layout(location = 0) in vec4 inPosition;
layout(location = 1) in vec4 inColor;
//layout(location = 2) in vec2 inUV;

layout(location = 0) out vec4 fragColor;

void main() {
    gl_Position = inPosition;
//    fragColor = vec4(inUV, 0, 1) * inColor;
    fragColor = inColor;
}
