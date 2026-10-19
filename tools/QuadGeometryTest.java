import com.shumai.taczpip.client.QuadGeometry;
public final class QuadGeometryTest {
    public static void main(String[] args) {
        double[] face={-2,-1,3,2,-1,3,2,1,3,-2,1,3};
        double[] empty={10,20,-30,10,20,-30,10,20,-30,10,20,-30};
        // BedrockCubePerFace.compile emits six quads, including five EMPTY_VERTEX faces.
        double[][] emitted={empty,empty,empty,empty,empty,face};int visible=0;
        for(var q:emitted)if(QuadGeometry.hasArea(q))visible++;
        if(visible!=1||emitted.length==1)throw new AssertionError("single rendered-face gate");
        if(QuadGeometry.hasArea(new double[]{0,0,0,1,1,1,2,2,2,3,3,3}))throw new AssertionError("line face");
        for(int frame=0;frame<100;frame++) {
            double angle=frame*.01;double[] transformed=face.clone();
            for(int i=0;i<12;i+=3) {
                transformed[i]=face[i]*Math.cos(angle)+face[i+2]*Math.sin(angle)+10;
                transformed[i+2]=-face[i]*Math.sin(angle)+face[i+2]*Math.cos(angle)-30;
            }
            if(!QuadGeometry.hasArea(transformed)||QuadGeometry.hasArea(empty))throw new AssertionError("pose stability");
        }
        System.out.println("QuadGeometry: six emitted faces -> one real face; transformed placeholders ignored; real rotated faces retained");
    }
}
