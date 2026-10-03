#version 150

in vec3 Position;
uniform mat4 uViewProjection;
uniform vec3 uCenter;
uniform vec3 uHalfSize;
out vec3 vCameraRelativePosition;

void main() {
    vec3 position = uCenter + Position * uHalfSize;
    vCameraRelativePosition = position;
    gl_Position = uViewProjection * vec4(position, 1.0);
}
