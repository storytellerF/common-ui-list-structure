#version 300 es

layout(location = 0) in vec2 in_position;
layout(location = 1) in vec2 in_texture_coordination;

uniform mat4 mvp;

out vec2 texture_coordination;

void main() {
    gl_Position = mvp * vec4(in_position, 1.0f, 1.0f);
    texture_coordination = in_texture_coordination;
}