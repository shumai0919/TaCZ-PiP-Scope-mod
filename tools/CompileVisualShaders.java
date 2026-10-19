import java.nio.file.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL20.*;

/** Compile/link in a hidden desktop OpenGL context, without starting Minecraft. */
public class CompileVisualShaders {
    static int compile(int type,String source) {
        int shader=glCreateShader(type);glShaderSource(shader,source);glCompileShader(shader);
        if(glGetShaderi(shader,GL_COMPILE_STATUS)==0)throw new AssertionError(glGetShaderInfoLog(shader));
        return shader;
    }
    public static void main(String[] args) throws Exception {
        if(!GLFW.glfwInit())throw new IllegalStateException("GLFW unavailable");
        long window=0;
        try {
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR,3);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR,2);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE,GLFW.GLFW_OPENGL_CORE_PROFILE);
            window=GLFW.glfwCreateWindow(16,16,"Shader validation",0,0);
            if(window==0)throw new IllegalStateException("Hidden GL context unavailable");
            GLFW.glfwMakeContextCurrent(window);GL.createCapabilities();
            int vertex=compile(GL_VERTEX_SHADER,"#version 150\nin vec3 Position; in vec2 UV0; out vec2 texCoord0; void main(){texCoord0=UV0;gl_Position=vec4(Position,1);}");
            for(String path:args) {
                int fragment=compile(GL_FRAGMENT_SHADER,Files.readString(Path.of(path)));
                int program=glCreateProgram();glAttachShader(program,vertex);glAttachShader(program,fragment);glLinkProgram(program);
                if(glGetProgrami(program,GL_LINK_STATUS)==0)throw new AssertionError(path+": "+glGetProgramInfoLog(program));
                System.out.println("GPU shader compile/link passed: "+path);
                glDeleteProgram(program);glDeleteShader(fragment);
            }
            glDeleteShader(vertex);
        } finally {if(window!=0)GLFW.glfwDestroyWindow(window);GLFW.glfwTerminate();}
    }
}
