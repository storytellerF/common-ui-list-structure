#version 100

attribute vec2 in_position;
attribute vec2 in_texture_coordination;

uniform mat4 mvp;

varying vec2 texture_coordination;

void main() {
    gl_Position = mvp * vec4(in_position, 1.0, 1.0);
    texture_coordination = in_texture_coordination;
}