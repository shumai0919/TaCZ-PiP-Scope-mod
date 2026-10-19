#version 150
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform float EdgeRefraction;
uniform float Ghost;
uniform float SurfaceTexture;
uniform float IdleGlass;
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
uniform float Reflection;
uniform float PeripheralBlur;
uniform vec2 LensView;
uniform float Dirt;
uniform float Scratches;
uniform float ScratchDensity;
uniform float ScratchSize;
uniform float SurfacePattern;
in vec2 texCoord0;
out vec4 fragColor;

// Fixed low-frequency surface pattern. No time input, pixel noise, or extra texture.
float surfaceHash(vec2 p) {
    // Shared deterministic seed for smudges and scratches. Split the integer to
    // keep coordinates modest even at the maximum setting; zero is legacy-exact.
    float pattern = floor(clamp(SurfacePattern,0.0,9999.0));
    p += vec2(mod(pattern,100.0),floor(pattern/100.0)) * vec2(7.13,11.71);
    vec3 q = fract(vec3(p.xyx) * 0.1031);
    q += dot(q, q.yzx + 33.33);
    return fract((q.x + q.y) * q.z);
}
float surfaceNoise(vec2 p) {
    vec2 cell = floor(p), f = fract(p);
    f = f*f*(3.0-2.0*f);
    return mix(mix(surfaceHash(cell), surfaceHash(cell+vec2(1,0)), f.x),
               mix(surfaceHash(cell+vec2(0,1)), surfaceHash(cell+vec2(1,1)), f.x), f.y);
}
float scratchPattern(vec2 p, float footprint) {
    float result = 0.0;
    if (ScratchDensity <= 0.0 || ScratchSize <= 0.0) return result;
    float count = 12.0 * clamp(ScratchDensity,0.0,4.0);
    float size = clamp(ScratchSize,0.0001,4.0);
    // Sparse finite segments, not full-screen streaks. Pixel coverage suppresses
    // subpixel shimmer instead of widening distant scratches to opaque one-pixel lines.
    for (int i=0; i<48; i++) {
        if (float(i) >= count) break;
        // Existing marks keep their seeds/positions when density changes.
        float presence = clamp(count-float(i),0.0,1.0);
        // Opposite sectors first: 0,6,3,9,1,7,4,10,2,8,5,11.
        // Reorder complete marks, preserving each full set of twelve and its pattern.
        int within = i % 12;
        int slot = within / 4 + (within % 2) * 6 + ((within % 4) / 2) * 3;
        float seed = float((i / 12) * 12 + slot);
        // Spread marks across the middle and outer lens, leaving the aiming
        // center clean through the independent shape-aware scratch protection.
        float placement = (seed + surfaceHash(vec2(seed,3))) * 6.283185 / 12.0;
        vec2 center = vec2(cos(placement),sin(placement)) * mix(0.30,0.95,surfaceHash(vec2(seed,7)));
        float angle = surfaceHash(vec2(seed,11)) * 6.283185;
        vec2 direction = vec2(cos(angle),sin(angle));
        float halfLength = mix(0.035,0.18,surfaceHash(vec2(seed,17))) * size;
        vec2 v = p-center;
        float along = dot(v,direction);
        float segment = clamp(along,-halfLength,halfLength);
        float transverse = dot(v,vec2(-direction.y,direction.x));
        float curvature = (surfaceHash(vec2(seed,19)) - 0.5) * 0.7;
        float bend = curvature * segment * segment / halfLength;
        float distanceToLine = length(vec2(along-segment,transverse-bend));
        float taper = 1.0-smoothstep(0.65,1.0,abs(segment)/halfLength);
        float width = mix(0.0015,0.004,surfaceHash(vec2(seed,23))) * mix(0.3,1.0,taper) * size;
        float aa = max(footprint,0.0001);
        float coverage = (1.0-smoothstep(max(0.0,width-aa),width+aa,distanceToLine)) * min(1.0,width/aa);
        result = max(result,coverage * mix(0.45,1.0,surfaceHash(vec2(seed,29))) * presence);
    }
    return result;
}

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
        vec3 glass=vec3(0.0);
        if(IdleGlass>0.0) {
            // No scene texture reads: hip-fire must never display a stale magnified frame.
            vec2 p=(texCoord0-LensCenter)/max(LensRadius,vec2(0.00001));
            vec2 h=p-vec2(-0.25,0.55)+clamp(LensView,vec2(-2),vec2(2))*.2;
            float sheen=exp(-dot(h*vec2(.7,1.3),h*vec2(.7,1.3))*2.0);
            glass=IdleGlass*(vec3(.025,.035,.045)+vec3(.22,.32,.40)*sheen);
        }
        fragColor = vec4(glass, 1.0);
    } else {
        if (OpticsEnabled < 0.5 || (Distortion == 0.0 && Chromatic == 0.0 &&
                Vignette == 0.0 && Coating == 0.0 && Haze == 0.0 && Reflection == 0.0 && PeripheralBlur == 0.0 &&
                Dirt == 0.0 && Scratches == 0.0 && EdgeRefraction == 0.0 && Ghost == 0.0)) {
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
            // A hard max is continuous but its gradient jumps on the corner bisectors.
            // Blend competing edges symmetrically; no face-order-dependent smooth-max fold.
            // Subtract the maximum before exp to keep even extreme off-axis values finite.
            const float edgeBlend = 0.035;
            float weights = 0.0;
            for (int i=0; i<16; i++) {
                if (float(i) >= OpticEdgeCount) break;
                weights += exp((dot(OpticEdges[i], delta) - radius) / edgeBlend);
            }
            float roundedRadius = radius + edgeBlend * log(max(weights, 1.0));
            // Preserve zero at the optical center, entirely below the visible effect onset.
            radius = mix(radius, roundedRadius, smoothstep(0.0, 0.25, radius));
        }
        float edge = smoothstep(0.35, 1.0, radius);
        float peripheral = edge * edge;
        // Protect the actual screen-center aiming ray even during an off-axis ADS transition.
        float aimGuard = smoothstep(0.08, 0.25, length((texCoord0 - vec2(0.5)) / LensRadius));
        float warp = Distortion * peripheral * aimGuard;
        float edgeBend=smoothstep(0.75,1.0,radius);
        warp += EdgeRefraction * 0.12 * edgeBend * edgeBend * aimGuard;
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
        // Four taps only where peripheral softening is active. Lens-relative radius
        // remains stable when PiP resolution changes; the reticle is drawn separately.
        float softness = smoothstep(0.45, 1.0, radius) * aimGuard * PeripheralBlur;
        if (softness > 0.0001) {
            vec2 inset = 0.5 / vec2(textureSize(Sampler0, 0));
            vec2 spread = LensRadius * (0.025 * softness);
            vec3 blurred = (
                texture(Sampler0, clamp(uv + vec2(spread.x, 0.0), inset, 1.0-inset)).rgb +
                texture(Sampler0, clamp(uv - vec2(spread.x, 0.0), inset, 1.0-inset)).rgb +
                texture(Sampler0, clamp(uv + vec2(0.0, spread.y), inset, 1.0-inset)).rgb +
                texture(Sampler0, clamp(uv - vec2(0.0, spread.y), inset, 1.0-inset)).rgb) * 0.25;
            color = mix(color, blurred, min(0.8, softness * 2.0));
        }
        // Soft internal falloff, not an additional black ring or a new circular mask.
        float shade = 1.0 - Vignette * (0.65 * edge + 0.35 * smoothstep(0.82, 1.05, radius));
        color *= shade;
        if (Coating > 0.0 || Haze > 0.0 || Reflection > 0.0 || Ghost > 0.0) {
            // Artistic transmission/reflection approximation, NOT a reflected scene.
            // Sample the existing PiP image only; no readback or extra world pass.
            // Fixed lens-relative taps keep the broad veil independent of pixel resolution.
            vec2 inset = 0.5 / vec2(textureSize(Sampler0, 0));
            vec3 lightA=texture(Sampler0, clamp(LensCenter + LensRadius * vec2(-0.55, 0.55), inset, 1.0-inset)).rgb;
            vec3 lightB=texture(Sampler0, clamp(LensCenter + LensRadius * vec2(0.0, 0.7), inset, 1.0-inset)).rgb;
            vec3 lightC=texture(Sampler0, clamp(LensCenter + LensRadius * vec2(0.55, 0.55), inset, 1.0-inset)).rgb;
            vec3 light=(lightA+lightB+lightC)/3.0;
            float brightness = smoothstep(0.12, 0.8, dot(light, vec3(0.2126, 0.7152, 0.0722)));
            vec2 p = delta / LensRadius;
            // Reuse the same optical-frame eye projection as the shadow, not screen position.
            vec2 offset = clamp(LensView, vec2(-2.0), vec2(2.0));
            vec2 highlight = p - vec2(-0.3, 0.65) + offset * 0.35;
            float lobe = exp(-dot(highlight * vec2(0.8, 1.2), highlight * vec2(0.8, 1.2)) * 1.7);
            float protectAim = mix(0.25, 1.0, aimGuard);
            vec3 tint = mix(vec3(0.38, 0.6, 1.0), vec3(0.85, 0.5, 0.7),
                            clamp(p.x * 0.25 + offset.x * 0.2 + offset.y * 0.1 + 0.5, 0.0, 1.0));
            float oblique = smoothstep(0.0, 1.5, length(offset));
            vec3 reflection = tint * (Coating * brightness * lobe * protectAim * (0.65 + 0.35 * oblique));
            // Broad moving glass highlight, not a mirror image or simulated light source.
            vec2 glintPosition = p - vec2(0.35, 0.6) - offset * 0.45;
            float glint = exp(-dot(glintPosition * vec2(0.7, 1.8), glintPosition * vec2(0.7, 1.8)) * 2.5);
            vec3 glass = mix(vec3(0.65, 0.8, 0.9), clamp(light, 0.0, 1.0), 0.5) *
                         (Reflection * brightness * glint * protectAim * (0.35 + 0.65 * oblique));
            reflection = vec3(1.0) - (vec3(1.0)-reflection) * (vec3(1.0)-glass);
            if(Ghost>0.0) {
                vec3 lum=vec3(dot(lightA,vec3(.2126,.7152,.0722)),dot(lightB,vec3(.2126,.7152,.0722)),dot(lightC,vec3(.2126,.7152,.0722)));
                float sum=max(.001,lum.x+lum.y+lum.z);
                vec2 source=vec2(.55*(lum.z-lum.x)/sum,(.55*lum.x+.7*lum.y+.55*lum.z)/sum);
                vec2 g=p+source*.65-offset*.15;
                vec2 g2=p+source*1.1-offset*.1;
                float spots=exp(-dot(g,g)*100.0)*.55+exp(-dot(g2,g2)*180.0)*.3;
                float hot=smoothstep(.65,.98,max(lum.x,max(lum.y,lum.z)));
                vec3 ghost=vec3(.55,.75,.65)*Ghost*hot*spots*aimGuard;
                reflection=vec3(1.0)-(vec3(1.0)-reflection)*(vec3(1.0)-ghost);
            }
            vec3 veil = clamp(light, 0.0, 1.0) *
                        (Haze * brightness * (0.25 + 0.75 * lobe) * protectAim);
            // Bounded screen blend, opaque output. No radius cutoff: even rectangular
            // corners survive. The existing aperture/eyebox stencil alone clips these effects.
            color += (vec3(1.0) - clamp(color, 0.0, 1.0)) *
                     (vec3(1.0) - (vec3(1.0) - reflection) * (vec3(1.0) - veil));
        }
        if (Dirt > 0.0 || Scratches > 0.0) {
            vec2 surfacePosition = delta / LensRadius;
            float protectCenter = aimGuard * smoothstep(0.45,0.85,radius);
            float surfaceDetail=0.0;
            float imageScratches=0.0;
            if(SurfaceTexture>0.5) {
                // Shared pattern rotates/shifts the texture without wrapping a seam across glass.
                float rotation=mod(floor(SurfacePattern),4.0)*1.5707963;
                mat2 turn=mat2(cos(rotation),sin(rotation),-sin(rotation),cos(rotation));
                vec2 shift=vec2(surfaceHash(vec2(3,31)),surfaceHash(vec2(7,37)))-.5;
                vec2 imagePosition=turn*surfacePosition*.48+shift*.03;
                vec2 textureUv=clamp(vec2(.5)+imagePosition,vec2(.001),vec2(.999));
                surfaceDetail=clamp(texture(Sampler1,textureUv).r*4.0,0.0,1.0);
                // One continuous image, never repeated scratch stamps or tiled UVs.
                // Size magnifies the scratch image. Preserve continuous faint detail;
                // density is only for the procedural fallback, strength controls opacity.
                vec2 scratchUv=vec2(.5)+imagePosition/max(ScratchSize,0.001);
                vec2 inside=smoothstep(vec2(0.0),vec2(0.025),scratchUv)*
                            (vec2(1.0)-smoothstep(vec2(0.975),vec2(1.0),scratchUv));
                float detail=texture(Sampler1,clamp(scratchUv,vec2(.001),vec2(.999))).r;
                imageScratches=clamp(detail*5.0,0.0,1.0)*inside.x*inside.y;
                if(ScratchSize<=0.0) imageScratches=0.0;
            }
            if (Dirt > 0.0) {
                float smudge = smoothstep(0.52,0.85,surfaceNoise(surfacePosition*3.0+vec2(5.3,9.1)));
                if(SurfaceTexture>0.5) smudge=surfaceDetail;
                // Transmission loss, not an opaque painted stain or a new mask.
                color *= 1.0 - Dirt * smudge * protectCenter * 0.35;
            }
            if (Scratches > 0.0) {
                float footprint = max(length(dFdx(surfacePosition)),length(dFdy(surfacePosition)));
                float scratchProtection = aimGuard * smoothstep(0.18,0.42,radius);
                float marks;
                if(SurfaceTexture>0.5) marks=imageScratches;
                else marks=scratchPattern(surfacePosition,footprint);
                marks *= Scratches * scratchProtection;
                float illumination = smoothstep(0.05,0.75,dot(clamp(color,0.0,1.0),vec3(0.2126,0.7152,0.0722)));
                color += (vec3(1.0)-clamp(color,0.0,1.0)) * (marks * illumination * 0.9);
            }
        }
        fragColor = vec4(color, 1.0);
    }
}
