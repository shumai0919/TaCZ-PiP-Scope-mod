#version 150
uniform vec2 ViewportSize;
uniform float PupilRadius;
uniform float FeatherWidth;
uniform float PupilEdgeCount;
uniform vec4 PupilEdges[16];
uniform float ApertureEdgeCount;
uniform vec4 ApertureEdges[16];
in vec2 texCoord0;
out vec4 fragColor;
void main() {
    float apertureCoverage = 0.0;
    if (ApertureEdgeCount > 2.5) {
        vec2 pixel = (texCoord0 - vec2(0.5)) * ViewportSize;
        float inside = 1e20;
        for (int i=0; i<16; i++) {
            if (float(i) >= ApertureEdgeCount) break;
            inside = min(inside, dot(ApertureEdges[i].xy, pixel) - ApertureEdges[i].z);
        }
        apertureCoverage = 1.0-smoothstep(0.0, 1.5, inside);
    }
    if (PupilRadius <= 0.0 || FeatherWidth <= 0.0) {
        if (apertureCoverage <= 0.0) discard;
        fragColor = vec4(0.0,0.0,0.0,apertureCoverage);
        return;
    }
    if (PupilEdgeCount > 2.5) {
        vec2 pixel = (texCoord0 - vec2(0.5)) * ViewportSize;
        float inside = 1e20;
        for (int i=0; i<16; i++) {
            if (float(i) >= PupilEdgeCount) break;
            inside = min(inside, dot(PupilEdges[i].xy, pixel) - PupilEdges[i].z);
        }
        float coverage = max(apertureCoverage,1.0-smoothstep(0.0, FeatherWidth, inside));
        if (coverage <= 0.0) discard;
        fragColor = vec4(0.0, 0.0, 0.0, coverage);
        return;
    }
    float distanceToPupil = length((texCoord0 - vec2(0.5)) * ViewportSize);
    float start = max(0.0, PupilRadius - FeatherWidth);
    float coverage = max(apertureCoverage,smoothstep(start, PupilRadius, distanceToPupil));
    if (coverage <= 0.0) discard;
    fragColor = vec4(0.0, 0.0, 0.0, coverage);
}
