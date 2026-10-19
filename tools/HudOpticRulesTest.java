import com.shumai.taczpip.client.HudOpticRules;
import java.util.List;
public class HudOpticRulesTest {
    record Node(boolean geometry,boolean lit,List<Node> children) {}
    static Node n(boolean geometry,boolean lit,Node... children){return new Node(geometry,lit,List.of(children));}
    static boolean hud(Node root){return HudOpticRules.emissiveOnly(root,Node::children,Node::geometry,Node::lit);}
    public static void main(String[] args){
        if(!hud(n(false,false,n(true,true)))) throw new AssertionError("HUD child");
        if(!hud(n(false,false,n(false,true,n(true,false))))) throw new AssertionError("inherited emission");
        if(hud(n(false,false,n(true,false)))) throw new AssertionError("physical child aperture");
        if(hud(n(false,false,n(true,true),n(true,false)))) throw new AssertionError("mixed aperture");
        if(hud(n(false,false))) throw new AssertionError("empty geometry");
        if(hud(n(true,false))) throw new AssertionError("ordinary aperture");
        System.out.println("HUD structural classification: all six cases passed");
    }
}
