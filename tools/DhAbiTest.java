import com.shumai.taczpip.compat.DhAbi;
import com.shumai.taczpip.compat.DhVersionRules;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public class DhAbiTest {
    public static void main(String[] args) throws Exception {
        if (!DhVersionRules.legacy("2.2.1-a") || DhVersionRules.modern("3.2.9")
                || DhVersionRules.modern("3.3.0-b") || DhVersionRules.modern("garbage")
                || !DhVersionRules.modern("3.3.0") || !DhVersionRules.modern("3.3.3")
                || !DhVersionRules.modern("3.4.0") || !DhVersionRules.modern("4.0.0"))
            throw new AssertionError("Version rules");
        for (String path : args) {
            try (JarFile jar = new JarFile(path)) {
                DhAbi.Classes reader = name -> {
                    var entry = jar.getJarEntry(name.replace('.', '/') + ".class");
                    if (entry == null) throw new ClassNotFoundException(name);
                    ClassNode node = new ClassNode();
                    try (var stream = jar.getInputStream(entry)) { new ClassReader(stream).accept(node, 0); }
                    return node;
                };
                DhAbi.Kind expected = path.contains("2.2.1") ? DhAbi.Kind.LEGACY : DhAbi.Kind.MODERN;
                if (DhAbi.detect(reader) != expected) throw new AssertionError("ABI rejected " + path);
                if (expected == DhAbi.Kind.MODERN) {
                    if (DhAbi.detect(name -> {
                        ClassNode node = reader.read(name);
                        if (name.equals(DhAbi.META)) node.fields.removeIf(f -> f.name.equals("depthTexture"));
                        return node;
                    }) != DhAbi.Kind.NONE) throw new AssertionError("Changed ABI accepted");
                }
                System.out.println("PASS: " + path + " => " + expected);
            }
        }
        if (DhAbi.detect(name -> { throw new ClassNotFoundException(name); }) != DhAbi.Kind.NONE)
            throw new AssertionError("Absent DH");
        System.out.println("PASS: release version rules; absent/changed DH rejected");
    }
}
