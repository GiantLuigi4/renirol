#version 450
#extension GL_ARB_separate_shader_objects : enable

layout(location = 0) in vec2 uv;
layout(location = 0) out float outColor;

layout(binding = 0) uniform sampler2D texSampler;

void main() {
//    outColor = texture(texSampler, uv);
    outColor = 1.;
}
