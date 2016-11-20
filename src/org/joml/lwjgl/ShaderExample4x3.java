package org.joml.lwjgl;

import org.joml.Matrix4f;
import org.joml.Matrix4x3f;
import org.joml.Quaternionf;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.system.MemoryUtil.*;

public class ShaderExample4x3 {
    GLFWErrorCallback errorCallback;
    GLFWKeyCallback keyCallback;
    GLFWFramebufferSizeCallback fbCallback;

    long window;
    int width = 300;
    int height = 300;
    Object lock = new Object();
    boolean destroyed;

    Matrix4f projMatrix = new Matrix4f();
    Matrix4x3f viewMatrix = new Matrix4x3f();
    FloatBuffer fb = BufferUtils.createFloatBuffer(16);

    void run() {
        try {
            init();
            loop();

            synchronized (lock) {
                destroyed = true;
                glfwDestroyWindow(window);
            }
            keyCallback.free();
            fbCallback.free();
        } finally {
            glfwTerminate();
            errorCallback.free();
        }
    }

    void init() {
        glfwSetErrorCallback(errorCallback = GLFWErrorCallback.createPrint(System.err));
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 8);

        window = glfwCreateWindow(width, height, "Hello shaders!", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                    glfwSetWindowShouldClose(window, true);
            }
        });
        glfwSetFramebufferSizeCallback(window, fbCallback = new GLFWFramebufferSizeCallback() {
            @Override
            public void invoke(long window, int w, int h) {
                if (w > 0 && h > 0) {
                    width = w;
                    height = h;
                }
            }
        });

        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (vidmode.width() - width) / 2, (vidmode.height() - height) / 2);
        glfwShowWindow(window);

        IntBuffer framebufferSize = BufferUtils.createIntBuffer(2);
        nglfwGetFramebufferSize(window, memAddress(framebufferSize), memAddress(framebufferSize) + 4);
        width = framebufferSize.get(0);
        height = framebufferSize.get(1);
    }

    void renderCube() {
        glBegin(GL_QUADS);
        glVertex3f(  0.5f, -0.5f, -0.5f );
        glVertex3f( -0.5f, -0.5f, -0.5f );
        glVertex3f( -0.5f,  0.5f, -0.5f );
        glVertex3f(  0.5f,  0.5f, -0.5f );
        
        glVertex3f(  0.5f, -0.5f,  0.5f );
        glVertex3f(  0.5f,  0.5f,  0.5f );
        glVertex3f( -0.5f,  0.5f,  0.5f );
        glVertex3f( -0.5f, -0.5f,  0.5f );
        
        glVertex3f(  0.5f, -0.5f, -0.5f );
        glVertex3f(  0.5f,  0.5f, -0.5f );
        glVertex3f(  0.5f,  0.5f,  0.5f );
        glVertex3f(  0.5f, -0.5f,  0.5f );
        
        glVertex3f( -0.5f, -0.5f,  0.5f );
        glVertex3f( -0.5f,  0.5f,  0.5f );
        glVertex3f( -0.5f,  0.5f, -0.5f );
        glVertex3f( -0.5f, -0.5f, -0.5f );
        
        glVertex3f(  0.5f,  0.5f,  0.5f );
        glVertex3f(  0.5f,  0.5f, -0.5f );
        glVertex3f( -0.5f,  0.5f, -0.5f );
        glVertex3f( -0.5f,  0.5f,  0.5f );
        
        glVertex3f(  0.5f, -0.5f, -0.5f );
        glVertex3f(  0.5f, -0.5f,  0.5f );
        glVertex3f( -0.5f, -0.5f,  0.5f );
        glVertex3f( -0.5f, -0.5f, -0.5f );
        glEnd();
    }

    void renderGrid() {
        glBegin(GL_LINES);
        for (int i = -20; i <= 20; i++) {
            glVertex3f(-20.0f, 0.0f, i);
            glVertex3f( 20.0f, 0.0f, i);
            glVertex3f(i, 0.0f, -20.0f);
            glVertex3f(i, 0.0f,  20.0f);
        }
        glEnd();
    }

    void initOpenGLAndRenderInAnotherThread() {
        glfwMakeContextCurrent(window);
        glfwSwapInterval(0);
        GL.createCapabilities();

        glClearColor(0.6f, 0.7f, 0.8f, 1.0f);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

        // Create a simple shader program
        int program = glCreateProgram();
        int vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vs, 
                "uniform mat4   projMatrix;" +
                "uniform mat4x3 viewMatrix;" + 
                "void main(void) {" + 
                "  gl_Position = projMatrix * vec4(viewMatrix * gl_Vertex, 1.0);" + 
                "}");
        glCompileShader(vs);
        glAttachShader(program, vs);
        int fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs,
                "uniform vec3 color;" +
                "void main(void) {" + 
                "  gl_FragColor = vec4(color, 1.0);" + 
                "}");
        glCompileShader(fs);
        glAttachShader(program, fs);
        glLinkProgram(program);
        glUseProgram(program);

        // Obtain uniform location
        int projMatrixLocation = glGetUniformLocation(program, "projMatrix");
        int viewMatrixLocation = glGetUniformLocation(program, "viewMatrix");
        int colorLocation = glGetUniformLocation(program, "color");
        long lastTime = System.nanoTime();

        /* Quaternion to rotate the cube */
        Quaternionf q = new Quaternionf();

        while (!destroyed) {
            long thisTime = System.nanoTime();
            float dt = (thisTime - lastTime) / 1E9f;
            lastTime = thisTime;

            glViewport(0, 0, width, height);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            projMatrix.setPerspective((float) Math.toRadians(60.0f), (float) width / height, 0.01f, 100.0f);
            glUniformMatrix4fv(projMatrixLocation, false, projMatrix.get(fb));
            viewMatrix.setLookAt(0.0f, 4.0f, 10.0f,
                                 0.0f, 0.5f, 0.0f,
                                 0.0f, 1.0f, 0.0f);
            glUniformMatrix4x3fv(viewMatrixLocation, false, viewMatrix.get(fb));
            glUniform3f(colorLocation, 0.3f, 0.3f, 0.3f);
            renderGrid();

            viewMatrix.translate(0.0f, 0.5f, 0.0f)
                      .rotate(q.rotateY((float) Math.toRadians(45) * dt).normalize());
            glUniformMatrix4x3fv(viewMatrixLocation, false, viewMatrix.get(fb));

            // Render solid cube with outlines
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            glUniform3f(colorLocation, 0.6f, 0.7f, 0.8f);
            renderCube();
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            glEnable(GL_POLYGON_OFFSET_LINE);
            glPolygonOffset(-1.f,-1.f);
            glUniform3f(colorLocation, 0.0f, 0.0f, 0.0f);
            renderCube();
            glDisable(GL_POLYGON_OFFSET_LINE);

            synchronized (lock) {
                if (!destroyed) {
                    glfwSwapBuffers(window);
                }
            }
        }
    }

    void loop() {
        /*
         * Spawn a new thread which to make the OpenGL context current in and which does the
         * rendering.
         */
        new Thread(new Runnable() {
            public void run() {
                initOpenGLAndRenderInAnotherThread();
            }
        }).start();

        /* Process window messages in the main thread */
        while (!glfwWindowShouldClose(window)) {
            glfwWaitEvents();
        }
    }

    public static void main(String[] args) {
        new ShaderExample4x3().run();
    }
}