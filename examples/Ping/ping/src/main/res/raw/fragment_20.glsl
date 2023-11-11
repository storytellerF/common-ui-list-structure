#version 100
#extension GL_OES_EGL_image_external : require

precision mediump float;

uniform samplerExternalOES frame;

varying vec2 texture_coordination;

void main() {
    gl_FragColor = texture2D(frame, texture_coordination);
}