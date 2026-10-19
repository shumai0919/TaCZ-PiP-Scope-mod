public class ScratchOrderTest {
    static int seed(int i) {
        int within=i%12;
        return (i/12)*12+within/4+(within%2)*6+((within%4)/2)*3;
    }
    public static void main(String[] args) {
        boolean[] used=new boolean[48];
        for(int i=0;i<48;i++) {
            int s=seed(i);
            if(s<0||s>=48||used[s]||s/12!=i/12)throw new AssertionError("set preservation");
            used[s]=true;
        }
        for(int block=0;block<4;block++) {
            for(int batch=0;batch<3;batch++) {
                boolean[] sectors=new boolean[4];
                for(int j=0;j<4;j++) sectors[(seed(block*12+batch*4+j)%12)/3]=true;
                for(boolean present:sectors)if(!present)throw new AssertionError("quadrant distribution");
            }
            if(seed(block*12+1)-seed(block*12)!=6)throw new AssertionError("opposite pair");
        }
        int[] expected={0,6,3,9,1,7,4,10,2,8,5,11};
        for(int i=0;i<12;i++)if(seed(i)!=expected[i])throw new AssertionError("prefix order");
        System.out.println("ScratchOrder: opposite pairs, all four quadrants per batch, unique seeds and unchanged full sets passed");
    }
}
