#version 150
uniform sampler2D Sampler0;
uniform vec2 GunBlurStep;
in vec2 texCoord0;
out vec4 fragColor;
void main() {
    fragColor = texture(Sampler0, texCoord0);
    if (GunBlurStep.x > 0.0 || GunBlurStep.y > 0.0) {
        // The existing layer uses premultiplied-alpha blending. Filter RGBA
        // together, never unpremultiply or discard transparent neighbors.
        vec2 inset=0.5/vec2(textureSize(Sampler0,0));
        vec4 sum=fragColor*0.4;
        for(int x=-1;x<=1;x++) for(int y=-1;y<=1;y++) {
            if(x==0 && y==0) continue;
            sum+=texture(Sampler0,clamp(texCoord0+vec2(x,y)*GunBlurStep,inset,1.0-inset))*0.075;
        }
        fragColor=sum;
    }
    if (fragColor.a <= 0.0) discard;
}
