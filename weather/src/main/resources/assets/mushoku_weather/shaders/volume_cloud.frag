#version 150

in vec3 vCameraRelativePosition;
out vec4 fragColor;

uniform mat4 uViewProjection;
uniform vec3 uCenter;
uniform vec3 uHalfSize;
uniform vec2 uWind;
uniform vec3 uSunDirection;
uniform vec3 uNoiseOrigin;
uniform int uKind;
uniform int uSteps;
uniform float uIntensity;
uniform float uPhase;
uniform float uFlowPhase;
uniform float uDaylight;
uniform float uTwilight;
uniform float uLightning;
uniform sampler2D uCloudNoise;

float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

// Seamless triplanar detail keeps the ray-marched field volumetric instead of billboard-flat.
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
        // Cumulonimbus: broad flat base, buoyant towers, and a wind-sheared anvil.
        float base = smoothstep(-1.08, -0.75, q.y);
        float body = smoothInside(radial, 0.18, 0.84) * base
                * (1.0 - smoothstep(0.68, 1.0, q.y));
        float tower = exp(-dot(q.xz, q.xz) * 5.6)
                * smoothstep(-0.94, -0.10, q.y)
                * (1.0 - smoothstep(0.66, 1.02, q.y));
        float satellite = exp(-dot(q.xz - vec2(0.24, -0.16), q.xz - vec2(0.24, -0.16)) * 17.0)
                * smoothstep(-0.78, -0.22, q.y)
                * (1.0 - smoothstep(0.35, 0.82, q.y));
        float anvil = smoothInside(abs(across), 0.38, 0.62)
                * smoothInside(abs(along - 0.18), 0.55, 0.72)
                * smoothstep(0.08, 0.44, q.y)
                * (1.0 - smoothstep(0.82, 1.02, q.y));
        return max(body * 0.60, max(tower * 0.88, max(satellite * 0.54, anvil * 0.72)));
    }
    if (uKind == 1 || uKind == 4) {
        // Supercell and hail core: elevated rotating updraft, wide anvil, and a sculpted wall cloud.
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
        // Squall line: long shelf, turbulent gust front, and a rolled leading edge.
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
        // Tropical cyclone: spiral rain bands orbiting a calmer eye.
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
        // Tornadic condensation funnel tapers into the rotating cloud base.
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
        // Dust wall: a ground-hugging front driven by the local wind field.
        float wall = smoothInside(abs(across), 0.20, 0.80)
                * smoothInside(abs(along), 0.24, 0.76)
                * smoothstep(-1.05, -0.76, q.y)
                * (1.0 - smoothstep(0.12, 0.98, q.y));
        float rotor = exp(-abs(q.y + 0.45 + 0.16 * sin(along * 4.0 + phase)) * 5.0)
                * smoothInside(abs(across), 0.42, 0.58);
        return max(wall * 0.88, rotor * 0.60);
    }
    if (uKind == 8) {
        // Fair-weather cumulus: a level condensation base and buoyant, rounded towers.
        float flatBase = smoothstep(-1.08, -0.82, q.y);
        float puffyTop = 1.0 - smoothstep(0.10, 0.76, q.y);
        float slowBillow = 0.94 + 0.06 * sin(dot(q.xz, wind) * 3.2 + phase * 0.12);
        return flatBase * puffyTop * slowBillow;
    }

    // Rain-bearing stratocumulus / nimbostratus: a continuous, deep cloud deck.
    float flatBase = smoothstep(-1.10, -0.76, q.y);
    float softTop = 1.0 - smoothstep(0.62, 1.02, q.y);
    float broadWave = 0.97 + 0.03 * sin(q.x * 1.2 + q.z * 1.8 + phase * 0.1);
    return flatBase * softTop * broadWave;
}

vec3 cloudTint(float qy, float detail, float cellular, float viewSun) {
    vec3 underside = vec3(0.095, 0.125, 0.165);
    vec3 body = vec3(0.30, 0.38, 0.46);
    vec3 sunlit = vec3(0.80, 0.85, 0.89);
    if (uKind == 6) {
        underside = vec3(0.31, 0.22, 0.105);
        body = vec3(0.63, 0.46, 0.23);
        sunlit = vec3(0.91, 0.72, 0.38);
    } else if (uKind == 4) {
        underside = vec3(0.18, 0.26, 0.35);
        body = vec3(0.47, 0.59, 0.69);
        sunlit = vec3(0.88, 0.94, 0.99);
    } else if (uKind == 7) {
        float stormCover = smoothstep(0.48, 1.0, uIntensity);
        underside = mix(vec3(0.27, 0.34, 0.42), vec3(0.105, 0.14, 0.19), stormCover);
        body = mix(vec3(0.58, 0.67, 0.74), vec3(0.32, 0.40, 0.48), stormCover);
        sunlit = vec3(0.85, 0.90, 0.94);
    } else if (uKind == 8) {
        underside = vec3(0.30, 0.39, 0.49);
        body = vec3(0.67, 0.75, 0.82);
        sunlit = vec3(0.98, 0.97, 0.91);
    }

    float topLight = smoothstep(-0.48, 0.82, qy);
    float billowHighlight = uKind == 8
            ? smoothstep(0.38, 0.88, cellular) * smoothstep(-0.40, 0.34, qy) * 0.20
            : 0.0;
    vec3 color = mix(underside, body, 0.34 + topLight * 0.44);
    color = mix(color, sunlit, clamp(topLight * 0.48 + detail * 0.12 + billowHighlight, 0.0, 0.78));

    float sunElevation = max(uSunDirection.y, 0.0);
    vec3 nightTint = vec3(0.57, 0.68, 0.88);
    vec3 warmSun = vec3(1.12, 0.66, 0.43);
    vec3 daySun = vec3(1.0, 0.96, 0.87);
    vec3 sunColor = mix(warmSun, daySun, smoothstep(0.06, 0.68, sunElevation));
    color *= mix(nightTint, vec3(1.0), clamp(uDaylight, 0.0, 1.0));
    color = mix(color, color * sunColor, clamp(topLight * sunElevation * 0.30, 0.0, 0.30));
    color = mix(color, color * vec3(1.16, 0.77, 0.57), clamp(uTwilight * 0.52, 0.0, 0.52));
    float forwardScatter = pow(max(viewSun, 0.0), 6.0)
            * (sunElevation * uDaylight + uTwilight * 0.12);
    color += sunColor * forwardScatter * 0.14;
    return color;
}

float selfShadow(vec3 q, float phase) {
    float opticalDepth = 0.0;
    vec3 stepQ = uSunDirection * (26.0 / uHalfSize);
    for (int index = 1; index <= 4; ++index) {
        float sampleShape = cloudShape(q + stepQ * float(index), phase);
        opticalDepth += max(sampleShape - 0.06, 0.0);
    }
    return exp(-opticalDepth * (uKind == 7 ? 0.34 : 0.46));
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
    float frequency = uKind == 5 ? 0.0048 : (uKind == 6 ? 0.0025 : (uKind == 7 || uKind == 8 ? 0.00055 : 0.00145));
    vec3 noiseScale = vec3(frequency, frequency * 1.72, frequency);
    float viewSun = dot(rayDirection, uSunDirection);

    for (int index = 0; index < 56; ++index) {
        if (index >= steps || accumulatedAlpha > 0.985) {
            break;
        }
        float jitter = hash31(vec3(gl_FragCoord.xy, float(index) + 17.0)) - 0.5;
        float distanceAlongRay = tNear + (float(index) + 0.5 + jitter * 0.48) * stepLength;
        vec3 point = rayDirection * distanceAlongRay;
        vec3 local = point - uCenter;
        vec3 q = local / uHalfSize;
        float phase = uPhase;
        float shape = cloudShape(q, phase);
        float highWisps = uKind == 8
                ? smoothstep(0.39, 0.54, q.y) * (1.0 - smoothstep(0.86, 0.98, q.y))
                : 0.0;
        if (shape > 0.002 || highWisps > 0.002) {
            vec3 flowOffset = vec3(wind.x, 0.08, wind.y) * uFlowPhase;
            vec3 kindOffset = vec3(float(uKind) * 0.173, float(uKind) * 0.317, float(uKind) * 0.239);
            vec3 noisePoint = local * noiseScale + uNoiseOrigin + flowOffset + kindOffset;
            vec4 cloudNoise = sampleCloudNoise(noisePoint);
            float broadNoise = cloudNoise.r;
            float detailNoise = cloudNoise.g * 0.58 + cloudNoise.b * 0.42;
            float cellularNoise = cloudNoise.a;
            float cloudField = cloudNoise.r * 0.56 + cloudNoise.g * 0.29 + cloudNoise.b * 0.15;
            float cloudDensity;
            if (uKind == 8) {
                // Worley cells form detached cumulus; the tighter cap mask lets the tops tower upward.
                float puffField = cellularNoise * 0.58 + cloudNoise.r * 0.26
                        + cloudNoise.g * 0.11 + cloudNoise.b * 0.05;
                float threshold = mix(0.78, 0.54, clamp(uIntensity, 0.0, 1.0));
                float softPuffs = smoothstep(threshold - 0.16, threshold + 0.12, puffField);
                float compactCores = smoothstep(threshold + 0.02, threshold + 0.20, puffField);
                float towerGrowth = smoothstep(-0.88, -0.10, q.y);
                float towerField = mix(softPuffs * 0.76, compactCores, towerGrowth);
                float billow = mix(0.70, 1.10,
                        clamp((cellularNoise * 0.76 + broadNoise * 0.24) * 1.25, 0.0, 1.0));
                cloudDensity = shape * towerField * billow * uIntensity * 1.85;
                float cirrusField = cloudNoise.r * 0.42 + cloudNoise.g * 0.38 + cloudNoise.b * 0.20;
                float cirrusMask = smoothstep(0.59, 0.82, cirrusField);
                float acrossWind = dot(q.xz, vec2(-wind.y, wind.x));
                float stretchedWisps = 0.84 + 0.16 * (0.5 + 0.5 * sin(
                        acrossWind * 24.0 + cloudNoise.r * 5.0 + phase * 0.04));
                cloudDensity += highWisps * cirrusMask * stretchedWisps * uIntensity * 0.30;
            } else if (uKind == 7) {
                float threshold = mix(0.68, 0.36, clamp(uIntensity, 0.0, 1.0));
                float cellMask = smoothstep(threshold - 0.13, threshold + 0.17, cloudField);
                float billow = mix(0.72, 1.0, clamp(cloudField * 1.35, 0.0, 1.0));
                cloudDensity = shape * cellMask * billow * uIntensity * 1.78;
            } else {
                float erosion = (cloudNoise.r - 0.50) * 0.50
                        + (cloudNoise.g - 0.50) * 0.31
                        + (cloudNoise.b - 0.50) * 0.19;
                float fineErosion = (detailNoise - 0.50) * 0.34;
                float cellularBillows = (uKind == 0 || uKind == 1 || uKind == 4)
                        ? (cellularNoise - 0.50) * 0.24 : 0.0;
                float warpedShape = shape + (broadNoise - 0.50) * 0.16 + cellularBillows;
                float coverage = mix(0.70, 0.30, clamp(uIntensity, 0.0, 1.0));
                cloudDensity = clamp((warpedShape + erosion * 0.92 + fineErosion - coverage) * 3.0, 0.0, 1.0)
                        * uIntensity;
            }

            float fogBand = smoothstep(-1.04, -0.88, q.y) * (1.0 - smoothstep(-0.44, -0.12, q.y));
            float stormFogStrength = uKind == 7
                    ? smoothstep(0.45, 0.92, uIntensity) * 0.082
                    : (uKind == 0 || uKind == 1 || uKind == 2 || uKind == 3 || uKind == 4
                            ? uIntensity * 0.035 : 0.0);
            float fogDensity = fogBand * stormFogStrength * (0.72 + cloudNoise.g * 0.56);
            float density = cloudDensity + fogDensity;
            if (density > 0.012) {
                if (!foundDepth && density > 0.035) {
                    firstCloudDepth = distanceAlongRay;
                    foundDepth = true;
                }
                float absorption = (uKind == 6 ? 0.041 : (uKind == 7 ? 0.038 : 0.033))
                        * stepLength * density;
                float sampleAlpha = 1.0 - exp(-absorption);
                float lightTransmittance = 1.0;
                if (uSteps >= 40 && uSunDirection.y > 0.04) {
                    lightTransmittance = selfShadow(q, phase);
                }
                float topLight = smoothstep(-0.48, 0.82, q.y);
                float ambient = mix(0.25, 0.42, clamp(uDaylight, 0.0, 1.0));
                float direct = max(uSunDirection.y, 0.0) * (0.38 + topLight * 0.46)
                        * lightTransmittance;
                float illumination = clamp(ambient + direct, 0.20, 1.22);
                float edgeSignal = uKind == 8 ? cellularNoise : detailNoise;
                float silverEdge = pow(clamp(1.0 - abs(edgeSignal - 0.5) * 2.0, 0.0, 1.0), 3.0)
                        * mix(0.06, 0.18, clamp(uDaylight, 0.0, 1.0))
                        * pow(max(viewSun, 0.0), 3.0);
                vec3 color = cloudTint(q.y, detailNoise, cellularNoise, viewSun) * illumination;
                if (fogDensity > cloudDensity * 0.35) {
                    float fogMix = clamp(fogDensity / max(density, 0.0001) * 0.78, 0.0, 0.78);
                    vec3 fogColor = mix(vec3(0.43, 0.52, 0.62), vec3(0.18, 0.24, 0.33),
                            clamp(uIntensity * 0.86, 0.0, 0.86));
                    color = mix(color, fogColor, fogMix);
                }
                color += vec3(0.80, 0.88, 1.0) * silverEdge;
                if (uLightning > 0.5 && uKind != 5 && uKind != 6 && uKind != 7 && uKind != 8) {
                    float flashWave = sin(uPhase * 41.0);
                    float lightning = pow(max(flashWave, 0.0), 48.0) * clamp(uIntensity, 0.0, 1.0);
                    color += vec3(0.48, 0.58, 0.74) * lightning;
                }
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
