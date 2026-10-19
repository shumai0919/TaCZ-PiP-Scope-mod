/** CPU reference of surfaceHash; not a GPU pixel equality test. */
public class SurfacePatternTest {
    static float fract(float v){return v-(float)Math.floor(v);}
    static float legacy(float px,float py) {
        float x=fract(px*.1031f),y=fract(py*.1031f),z=fract(px*.1031f);
        float d=x*(y+33.33f)+y*(z+33.33f)+z*(x+33.33f);
        x+=d;y+=d;z+=d;
        return fract((x+y)*z);
    }
    static float hash(float x,float y,int pattern) {
        return legacy(x+(pattern%100)*7.13f,y+(pattern/100)*11.71f);
    }
    public static void main(String[] args) {
        for(int pattern:new int[]{0,1,99,100,1234,9999}) {
            boolean changedDirt=false,changedScratches=false;
            for(int i=0;i<48;i++) {
                float dirt=hash(i-12,9,pattern),scratch=hash(i,7,pattern);
                if(!Float.isFinite(dirt)||dirt<0||dirt>=1||!Float.isFinite(scratch)||scratch<0||scratch>=1)
                    throw new AssertionError("hash range");
                if(dirt!=hash(i-12,9,pattern)||scratch!=hash(i,7,pattern))throw new AssertionError("reproducibility");
                if(pattern==0&&(dirt!=legacy(i-12,9)||scratch!=legacy(i,7)))throw new AssertionError("legacy pattern");
                changedDirt|=dirt!=hash(i-12,9,0);
                changedScratches|=scratch!=hash(i,7,0);
            }
            if(pattern!=0&&(!changedDirt||!changedScratches))throw new AssertionError("shared pattern not applied");
        }
        System.out.println("SurfacePattern: legacy zero, shared dirt/scratch variation, repeatability and bounds passed (CPU reference)");
    }
}
