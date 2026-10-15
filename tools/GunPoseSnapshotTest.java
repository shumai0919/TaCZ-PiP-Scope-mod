import com.shumai.taczpip.client.GunPoseSnapshot;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import org.joml.Quaternionf;
import java.util.List;

public final class GunPoseSnapshotTest {
    private static void check(boolean ok) { if (!ok) throw new AssertionError(); }
    public static void main(String[] args) {
        BedrockPart root = new BedrockPart("root"), child = new BedrockPart("magazine");
        root.children.add(child);
        root.offsetX=.03f;
        root.additionalQuaternion.rotateXYZ(.2f,.3f,.4f);
        Quaternionf aimed = new Quaternionf(root.additionalQuaternion);
        child.offsetY=.75f; child.yRot=.2f; child.xScale=.8f; child.visible=false;
        var captured = new GunPoseSnapshot(List.of(root,child));
        root.offsetX=0; root.additionalQuaternion.identity();
        child.offsetY=0; child.yRot=0; child.xScale=1; child.visible=true;
        Runnable verify = () -> {
            check(root.offsetX==.03f && root.additionalQuaternion.equals(aimed));
            check(child.offsetY==.75f && child.yRot==.2f && child.xScale==.8f && !child.visible);
        };
        captured.run(verify);
        check(root.offsetX==0 && root.additionalQuaternion.equals(new Quaternionf()));
        check(child.offsetY==0 && child.xScale==1 && child.visible);
        root.offsetX=.9f;
        try { captured.run(() -> {verify.run(); throw new IllegalStateException("test");}); }
        catch (IllegalStateException expected) { check(expected.getMessage().equals("test")); }
        check(root.offsetX==.9f && child.offsetY==0 && child.visible);
        captured.run(verify);
        check(root.offsetX==.9f);
        System.out.println("GunPoseSnapshot: actual TaCZ bones; sway/reload, child deduplication, quaternion copy, repeated draw and exception restoration passed");
    }
}
