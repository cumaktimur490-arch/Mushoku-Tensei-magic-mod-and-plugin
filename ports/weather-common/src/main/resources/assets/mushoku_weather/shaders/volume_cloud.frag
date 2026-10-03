#version 150

in vec3 vCameraRelativePosition;
out vec4 fragColor;

uniform mat4 uViewProjection;
uniform vec3 uCenter;
uniform vec3 uHalfSize;
uniform vec2 uWind;
uniform int uKind;
uniform int uSteps;
uniform float uIntensity;
uniform float uPhase;
uniform float uTime;
uniform sampler2D uCloudNoise;

float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

// A seamless 2D detail texture is projected along all three axes so the
// density field keeps its 3D shape instead of looking like a flat billboard.
vec4 sampleCloudNoise(vec3 point) {
    vec4 alongX = texture(uCloudNoise, point.yz);
    vec4 alongY = texture(uCloudNoise, point.zx);
    vec4 alongZ = texture(uCloudNoise, point.xy);
    return (alongX + alongY + alongZ) * 0.3333333;
}

float smoothInside(float value, float edge, float feather) {
    return 1.0 - smoothstep(edge, edge + feather, value);
}

float cloudShape(vec3 q, float phase) {
    float heightBand = smoothstep(-1.08, -0.72, q.y) * (1.0 - smoothstep(0.80, 1.03, q.y));
    float radial = length(q.xz);
    vec2 wind = normalize(uWind + vec2(0.0001, 0.0));
    vec2 crossWind = vec2(-wind.y, wind.x);
    float along = dot(q.xz, wind);
    float across = dot(q.xz, crossWind);

    if (uKind == 0) {
        // Broad cumulonimbus body with a rising convective tower and a wind-sheared anvil.
        float body = smoothInside(radial, 0.22, 0.82) * heightBand;
        float tower = exp(-dot(q.xz, q.xz) * 6.8)
                * smoothstep(-0.92, -0.15, q.y)
                * (1.0 - smoothstep(0.62, 0.98, q.y));
        float anvil = smoothInside(abs(across), 0.48, 0.52)
                * smoothInside(abs(along - 0.22), 0.60, 0.60)
                * smoothstep(0.10, 0.48, q.y)
                * (1.0 - smoothstep(0.82, 1.0, q.y));
        return max(body * 0.56, max(tower * 0.92, anvil * 0.72));
    }
    if (uKind == 1 || uKind == 4) {
        // Rotating supercell / hail core: elevated tower, broad anvil, and a curled wall cloud.
        float body = smoothInside(radial, 0.10, 0.90) * heightBand * 0.64;
        float tower = exp(-across * across * 4.2 - (along + 0.05) * (along + 0.05) * 4.0)
                * smoothstep(-0.90, -0.20, q.y)
                * (1.0 - smoothstep(0.58, 0.96, q.y));
        float anvil = smoothInside(abs(across), 0.36, 0.60)
                * smoothInside(abs(along - 0.25), 0.42, 0.68)
                * smoothstep(0.03, 0.40, q.y)
                * (1.0 - smoothstep(0.84, 1.0, q.y));
        float wallRadius = 0.26 + 0.06 * sin(phase + q.y * 2.2);
        float wall = exp(-abs(radial - wallRadius) * 16.0 - (q.y + 0.45) * (q.y + 0.45) * 12.0);
        float rotation = 0.5 + 0.5 * sin(atan(q.z, q.x) * 2.0 - phase + radial * 7.0);
        return max(body, max(tower * 0.94, max(anvil * 0.78, wall * rotation * 0.72)));
    }
    if (uKind == 2) {
        // Squall line: a long, low, wind-perpendicular shelf cloud and turbulent gust front.
        float front = smoothInside(abs(along + 0.10), 0.08, 0.34);
        float width = smoothInside(abs(across), 0.26, 0.74);
        float shelf = front * width * heightBand;
        float trailing = smoothInside(abs(along + 0.40), 0.12, 0.60)
                * smoothInside(abs(across), 0.58, 0.45)
                * smoothstep(-0.92, -0.46, q.y)
                * (1.0 - smoothstep(0.50, 0.88, q.y));
        float roll = exp(-abs(q.y + 0.18 + 0.10 * sin(across * 7.0 + phase)) * 8.0)
                * front * width;
        return max(shelf * 0.82, max(trailing * 0.58, roll * 0.78));
    }
    if (uKind == 3) {
        // Tropical cyclone: layered spiral bands around a broad eye.
        float angle = atan(q.z, q.x);
        float spiral = 0.5 + 0.5 * sin(radial * 26.0 - angle * 2.3 + phase);
        float bands = smoothstep(0.42, 0.80, spiral)
                * smoothInside(radial, 0.14, 0.86)
                * heightBand;
        float eyeWall = exp(-abs(radial - 0.30 - 0.04 * sin(angle * 2.0 + phase)) * 12.0)
                * smoothstep(-0.95, -0.60, q.y)
                * (1.0 - smoothstep(0.82, 1.0, q.y));
        return max(bands * 0.82, eyeWall * 0.62);
    }
    if (uKind == 5) {
        // Tornadic condensation funnel reaches the ground and tapers into the rotating cloud base.
        float h = clamp(q.y * 0.5 + 0.5, 0.0, 1.0);
        float radius = mix(0.10, 0.48, smoothstep(0.02, 0.88, h));
        vec2 wobble = vec2(sin(h * 8.0 + phase), cos(h * 6.2 + phase * 0.8)) * 0.035;
        float funnelRadius = length(q.xz - wobble);
        float wall = exp(-abs(funnelRadius - radius) * 16.0);
        float core = exp(-funnelRadius * 11.0) * 0.24;
        float cap = smoothstep(-1.02, -0.82, q.y) * (1.0 - smoothstep(0.82, 1.02, q.y));
        return (wall * 0.86 + core) * cap;
    }
    if (uKind == 6) {
        // Dust wall: a turbulent, ground-hugging front driven downwind.
        float wall = smoothInside(abs(across), 0.20, 0.80)
                * smoothInside(abs(along), 0.24, 0.76)
                * smoothstep(-1.05, -0.76, q.y)
                * (1.0 - smoothstep(0.12, 0.98, q.y));
        float rotor = exp(-abs(q.y + 0.45 + 0.16 * sin(along * 4.0 + phase)) * 5.0)
                * smoothInside(abs(across), 0.42, 0.58);
        return max(wall * 0.88, rotor * 0.60);
    }

    // Regional overcast: broad, soft-edged stratiform cloud layers.
    float layer = smoothInside(radial, 0.18, 0.82)
            * smoothstep(-1.06, -0.68, q.y)
            * (1.0 - smoothstep(0.70, 1.02, q.y));
    float undulation = 0.82 + 0.18 * sin(q.x * 4.0 + q.z * 2.8 + phase);
    return layer * undulation;
}

vec3 cloudTint(float qy, float detail) {
    vec3 underside = vec3(0.105, 0.135, 0.175);
    vec3 body = vec3(0.31, 0.39, 0.47);
    vec3 sunlit = vec3(0.78, 0.83, 0.86);
    if (uKind == 6) {
        underside = vec3(0.31, 0.22, 0.105);
        body = vec3(0.63, 0.46, 0.23);
        sunlit = vec3(0.91, 0.72, 0.38);
    } else if (uKind == 4) {
        underside = vec3(0.20, 0.28, 0.36);
        body = vec3(0.48, 0.59, 0.68);
        sunlit = vec3(0.86, 0.93, 0.98);
    }
    float topLight = smoothstep(-0.48, 0.86, qy);
    vec3 color = mix(underside, body, 0.48 + topLight * 0.35);
    color = mix(color, sunlit, clamp(topLight * 0.42 + detail * 0.12, 0.0, 0.62));
    return color;
}

void main() {
    vec3 rayDirection = normalize(vCameraRelativePosition);
    vec3 safeDirection = vec3(
            abs(rayDirection.x) < 0.00001 ? (rayDirection.x < 0.0 ? -0.00001 : 0.00001) : rayDirection.x,
            abs(rayDirection.y) < 0.00001 ? (rayDirection.y < 0.0 ? -0.00001 : 0.00001) : rayDirection.y,
            abs(rayDirection.z) < 0.00001 ? (rayDirection.z < 0.0 ? -0.00001 : 0.00001) : rayDirection.z);
    vec3 inverseDirection = 1.0 / safeDirection;
    vec3 t0 = (uCenter - uHalfSize) * inverseDirection;
    vec3 t1 = (uCenter + uHalfSize) * inverseDirection;
    vec3 tSmall = min(t0, t1);
    vec3 tLarge = max(t0, t1);
    float tNear = max(max(tSmall.x, tSmall.y), tSmall.z);
    float tFar = min(min(tLarge.x, tLarge.y), tLarge.z);
    tNear = max(tNear, 0.0);
    if (tFar <= tNear || uIntensity <= 0.005) {
        discard;
    }

    int steps = clamp(uSteps, 12, 56);
    float stepLength = (tFar - tNear) / float(steps);
    float accumulatedAlpha = 0.0;
    vec3 accumulatedColor = vec3(0.0);
    float firstCloudDepth = tNear;
    bool foundDepth = false;
    vec2 wind = normalize(uWind + vec2(0.0001, 0.0));
    float flowTime = uTime * (uKind == 2 ? 0.025 : 0.009);

    for (int index = 0; index < 56; ++index) {
        if (index >= steps || accumulatedAlpha > 0.985) {
            break;
        }
        float jitter = hash31(vec3(gl_FragCoord.xy, float(index) + 17.0)) - 0.5;
        float distanceAlongRay = tNear + (float(index) + 0.5 + jitter * 0.48) * stepLength;
        vec3 point = rayDirection * distanceAlongRay;
        vec3 local = point - uCenter;
        vec3 q = local / uHalfSize;
        float phase = uPhase + uTime * (uKind == 5 ? 1.1 : 0.16);
        float shape = cloudShape(q, phase);
        if (shape > 0.002) {
            vec3 flowOffset = vec3(wind.x, 0.08, wind.y) * flowTime;
            float frequency = uKind == 5 ? 0.0105 : (uKind == 6 ? 0.0095 : 0.0085);
            vec3 noisePoint = (local + flowOffset) * vec3(frequency, frequency * 1.72, frequency)
                    + vec3(uPhase * 0.31, uPhase * 0.17, uPhase * 0.43);
            vec4 cloudNoise = sampleCloudNoise(noisePoint);
            float broadNoise = cloudNoise.r;
            float detailNoise = cloudNoise.g * 0.58 + cloudNoise.b * 0.42;
            float erosion = (cloudNoise.r - 0.50) * 0.50
                    + (cloudNoise.g - 0.50) * 0.31
                    + (cloudNoise.b - 0.50) * 0.19;
            float warpedShape = shape + (broadNoise - 0.50) * 0.12;
            float coverage = mix(0.72, 0.34, clamp(uIntensity, 0.0, 1.0));
            float density = clamp((warpedShape + erosion - coverage) * 2.45, 0.0, 1.0) * uIntensity;
            if (density > 0.012) {
                if (!foundDepth && density > 0.055) {
                    firstCloudDepth = distanceAlongRay;
                    foundDepth = true;
                }
                float absorption = (uKind == 6 ? 0.041 : 0.030) * stepLength * density;
                float sampleAlpha = 1.0 - exp(-absorption);
                float lighting = clamp(0.54 + q.y * 0.18 + broadNoise * 0.17 + cloudNoise.g * 0.08, 0.30, 1.15);
                float silverEdge = pow(clamp(1.0 - abs(detailNoise - 0.5) * 2.0, 0.0, 1.0), 3.0) * 0.10;
                vec3 color = cloudTint(q.y, detailNoise) * (lighting + silverEdge);
                accumulatedColor += (1.0 - accumulatedAlpha) * sampleAlpha * color;
                accumulatedAlpha += (1.0 - accumulatedAlpha) * sampleAlpha;
            }
        }
    }

    if (accumulatedAlpha < 0.018) {
        discard;
    }
    vec4 clip = uViewProjection * vec4(rayDirection * firstCloudDepth, 1.0);
    float depth = clip.z / clip.w * 0.5 + 0.5;
    gl_FragDepth = clamp(depth, 0.0, 1.0);
    fragColor = vec4(accumulatedColor / max(accumulatedAlpha, 0.0001), accumulatedAlpha);
}
