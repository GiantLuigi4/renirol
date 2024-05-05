#version 450
#extension GL_ARB_separate_shader_objects : enable

layout(location = 0) out vec3 fragColor;

vec2 positions[3] = vec2[](
    vec2(-0.5, -0.5),
    vec2(0.5, 0.5),
    vec2(-0.5, 0.5)
);

vec3 colors[3] = vec3[](
    vec3(1.0, 0.0, 0.0),
    vec3(0.0, 1.0, 0.0),
    vec3(0.0, 0.0, 1.0)
);

vec3 colors1[3] = vec3[](
    vec3(0.0, 1.0, 0.0),
    vec3(1.0, 0.0, 0.0),
    vec3(0.0, 0.0, 1.0)
);

vec2 osets[9] = vec2[](
        vec2(0.0),

        vec2(1.0, 1.0),
        vec2(1.0, -1.0),
        vec2(-1.0, -1.0),
        vec2(-1.0, 1.0),

        vec2(1.0, 0.0),
        vec2(-1.0, 0.0),
        vec2(0.0, 1.0),
        vec2(0.0, -1.0)
);

void main() {
    int div = gl_InstanceIndex / 2;
    float mul = ((div * 2) == gl_InstanceIndex ? 1 : -1);

    gl_Position = vec4((mul * positions[gl_VertexIndex]) / 4.0 + osets[div] / 2.0, 0.0, 1.0);
    int idx = gl_VertexIndex;
    if (mul == -1) fragColor = colors1[idx];
    else fragColor = colors[idx];
}
