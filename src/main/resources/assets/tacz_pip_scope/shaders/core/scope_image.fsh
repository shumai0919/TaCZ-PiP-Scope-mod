#version 150
uniform sampler2D Sampler0;
uniform float MaskDebug;
uniform float LensReady;
uniform float PupilLimited;
uniform float PupilRadius;
uniform float PupilEdgeCount;
uniform vec4 PupilEdges[16];
uniform vec2 ViewportSize;
uniform float OpticsEnabled;
uniform vec2 LensCenter;
uniform vec2 LensRadius;
uniform float OpticEdgeCount;
uniform vec2 OpticEdges[16];
uniform float Distortion;
uniform float Chromatic;
uniform float Vignette;
uniform float Coating;
uniform float Haze;
in vec2 texCoord0;
out vec4 fragColor;

void main() {
    if (PupilLimited > 0.5) {
        vec2 pixel = (texCoord0 - vec2(0.5)) * ViewportSize;
        if (PupilRadius <= 0.0) discard;
        if (PupilEdgeCount > 2.5) {
            for (int i=0; i<16; i++) {
                if (float(i) >= PupilEdgeCount) break;
                if (dot(PupilEdges[i].xy, pixel) < PupilEdges[i].z) discard;
            }
        } else if (length(pixel) > PupilRadius) discard;
    }
    // Sky/fog can have zero alpha in a perfectly valid world render.
    // Never discard it or blend the unzoomed main view through it.
    if (MaskDebug > 0.5) {
        fragColor = vec4(0.0, 1.0, 1.0, 1.0);
    } else if (LensReady < 0.5) {
        // No world texture yet (hip-fire / ADS entry): cover the whole optical
        // ocular, without a circle cutout or sampling a stale frame.
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
    } else {
        if (OpticsEnabled < 0.5 || (Distortion == 0.0 && Chromatic == 0.0 &&
                Vignette == 0.0 && Coating == 0.0 && Haze == 0.0)) {
            fragColor = vec4(texture(Sampler0, texCoord0).rgb, 1.0);
            return;
        }
        // Bounds normalize the real projected ocular, not the full-screen aspect ratio.
        // They influence shading only; TaCZ's stencil remains the sole silhouette mask.
        vec2 delta = texCoord0 - LensCenter;
        float radius = length(delta / LensRadius);
        // Shape-normalized distance to the opening, not to the moving eye-box shadow.
        // Shading only: never discard pixels or alter the aperture stencil here.
        if (OpticEdgeCount > 2.5) {
            radius = 0.0;
            for (int i=0; i<16; i++) {
                if (float(i) >= OpticEdgeCount) break;
                radius = max(radius, dot(OpticEdges[i], delta));
            }
        }
        float edge = smoothstep(0.35, 1.0, radius);
        float peripheral = edge * edge;
        // Protect the actual screen-center aiming ray even during an off-axis ADS transition.
        float aimGuard = smoothstep(0.08, 0.25, length((texCoord0 - vec2(0.5)) / LensRadius));
        float warp = Distortion * peripheral * aimGuard;
        vec2 uv = LensCenter + delta * (1.0 - warp);
        vec3 color;
        if (Chromatic > 0.0) {
            // All channels move inward into the unmasked world texture. No outward reads
            // across the lens edge, no extra zoom/overscan render, no stretched border clamp.
            // Preserve the old cap in the old range; allow stronger explicitly selected effects.
            float texelRadius = length(delta * vec2(textureSize(Sampler0, 0)));
            float texelCap = 1.25 * max(1.0, Chromatic / 0.006);
            float ca = min(Chromatic, texelCap / max(texelRadius, 1.0)) * peripheral * aimGuard;
            color.r = texture(Sampler0, LensCenter + delta * (1.0 - warp - ca)).r;
            color.g = texture(Sampler0, uv).g;
            color.b = texture(Sampler0, LensCenter + delta * (1.0 - warp - ca * 0.5)).b;
        } else {
            color = texture(Sampler0, uv).rgb;
        }
        // Soft internal falloff, not an additional black ring or a new circular mask.
        float shade = 1.0 - Vignette * (0.65 * edge + 0.35 * smoothstep(0.82, 1.05, radius));
        color *= shade;
        if (Coating > 0.0 || Haze > 0.0) {
            // Artistic transmission/reflection approximation, NOT a reflected scene.
            // Sample the existing PiP image only; no readback or extra world pass.
            // Fixed lens-relative taps keep the broad veil independent of pixel resolution.
            vec2 inset = 0.5 / vec2(textureSize(Sampler0, 0));
            vec3 light = (
                texture(Sampler0, clamp(LensCenter + LensRadius * vec2(-0.55, 0.55), inset, 1.0-inset)).rgb +
                texture(Sampler0, clamp(LensCenter + LensRadius * vec2(0.0, 0.7), inset, 1.0-inset)).rgb +
                texture(Sampler0, clamp(LensCenter + LensRadius * vec2(0.55, 0.55), inset, 1.0-inset)).rgb) / 3.0;
            float brightness = smoothstep(0.12, 0.8, dot(light, vec3(0.2126, 0.7152, 0.0722)));
            vec2 p = delta / LensRadius;
            // Projected off-axis displacement is only a view-angle proxy.
            vec2 offset = clamp((LensCenter - vec2(0.5)) / LensRadius, vec2(-1.0), vec2(1.0));
            vec2 highlight = p - vec2(-0.3, 0.65) + offset * 0.35;
            float lobe = exp(-dot(highlight * vec2(0.8, 1.2), highlight * vec2(0.8, 1.2)) * 1.7);
            float protectAim = mix(0.25, 1.0, aimGuard);
            vec3 tint = mix(vec3(0.38, 0.6, 1.0), vec3(0.85, 0.5, 0.7),
                            clamp(p.x * 0.25 + 0.5, 0.0, 1.0));
            vec3 reflection = tint * (Coating * brightness * lobe * protectAim);
            vec3 veil = clamp(light, 0.0, 1.0) *
                        (Haze * brightness * (0.25 + 0.75 * lobe) * protectAim);
            // Bounded screen blend, opaque output. No radius cutoff: even rectangular
            // corners survive. The existing aperture/eyebox stencil alone clips these effects.
            color += (vec3(1.0) - clamp(color, 0.0, 1.0)) *
                     (vec3(1.0) - (vec3(1.0) - reflection) * (vec3(1.0) - veil));
        }
        fragColor = vec4(color, 1.0);
    }
}
