package org.joml.lwjgl;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryUtil.*;

public class ShaderExample {
    GLFWErrorCallback errorCallback;
    GLFWKeyCallback keyCallback;
    GLFWFramebufferSizeCallback fbCallback;

    long window;
    int width;
    int height;

    Matrix4f viewProjMatrix = new Matrix4f();
    FloatBuffer fb = BufferUtils.createFloatBuffer(16);

    void run() {
        try {
            init();
            loop();

            glfwDestroyWindow(window);
            keyCallback.release();
            fbCallback.release();
        } finally {
            glfwTerminate();
            errorCallback.release();
        }
    }

    void init() {
        glfwSetErrorCallback(errorCallback = errorCallbackPrint(System.err));
        if (glfwInit() != GL11.GL_TRUE)
            throw new IllegalStateException("Unable to initialize GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GL_TRUE);

        int WIDTH = 300;
        int HEIGHT = 300;

        window = glfwCreateWindow(WIDTH, HEIGHT, "Hello World!", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                    glfwSetWindowShouldClose(window, GL_TRUE);
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

        ByteBuffer vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (GLFWvidmode.width(vidmode) - WIDTH) / 2, (GLFWvidmode.height(vidmode) - HEIGHT) / 2);

        glfwShowWindow(window);
    }

    void renderCube() {
        glBegin(GL_QUADS);
        glColor3f(1.0f, 1.0f, 0.0f);
        glVertex3f(0.5f, -0.5f, -0.5f);
        glVertex3f(0.5f, 0.5f, -0.5f);
        glVertex3f(-0.5f, 0.5f, -0.5f);
        glVertex3f(-0.5f, -0.5f, -0.5f);
        glColor3f(0.0f, 1.0f, 1.0f);
        glVertex3f(0.5f, -0.5f, 0.5f);
        glVertex3f(0.5f, 0.5f, 0.5f);
        glVertex3f(-0.5f, 0.5f, 0.5f);
        glVertex3f(-0.5f, -0.5f, 0.5f);
        glColor3f(1.0f, 0.0f, 1.0f);
        glVertex3f(0.5f, -0.5f, -0.5f);
        glVertex3f(0.5f, 0.5f, -0.5f);
        glVertex3f(0.5f, 0.5f, 0.5f);
        glVertex3f(0.5f, -0.5f, 0.5f);
        glColor3f(0.0f, 1.0f, 0.0f);
        glVertex3f(-0.5f, -0.5f, 0.5f);
        glVertex3f(-0.5f, 0.5f, 0.5f);
        glVertex3f(-0.5f, 0.5f, -0.5f);
        glVertex3f(-0.5f, -0.5f, -0.5f);
        glColor3f(0.0f, 0.0f, 1.0f);
        glVertex3f(0.5f, 0.5f, 0.5f);
        glVertex3f(0.5f, 0.5f, -0.5f);
        glVertex3f(-0.5f, 0.5f, -0.5f);
        glVertex3f(-0.5f, 0.5f, 0.5f);
        glColor3f(1.0f, 0.0f, 0.0f);
        glVertex3f(0.5f, -0.5f, -0.5f);
        glVertex3f(0.5f, -0.5f, 0.5f);
        glVertex3f(-0.5f, -0.5f, 0.5f);
        glVertex3f(-0.5f, -0.5f, -0.5f);
        glEnd();
    }

    void renderGrid() {
        glBegin(GL_LINES);
        for (int i = -20; i < 20; i++) {
            glVertex3f(-20.0f, 0.0f, i);
            glVertex3f(20.0f, 0.0f, i);
            glVertex3f(i, 0.0f, -20.0f);
            glVertex3f(i, 0.0f, 20.0f);
        }
        glEnd();
    }

    void initOpenGLAndRenderInAnotherThread() {
        glfwMakeContextCurrent(window);
        glfwSwapInterval(0);
        GLContext.createFromCurrent();

        glClearColor(0.6f, 0.7f, 0.8f, 1.0f);
        glEnable(GL_DEPTH_TEST);
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

        // Create a simple shader program
        int program = glCreateProgram();
        int vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vs, 
                "uniform mat4 viewProjMatrix;" + 
                "void main(void) {"
                + "  gl_Position = viewProjMatrix * gl_Vertex;" + 
                "}");
        glCompileShader(vs);
        glAttachShader(program, vs);
        int fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs, 
                "void main(void) {" + 
                "  gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);" + 
                "}");
        glCompileShader(fs);
        glAttachShader(program, fs);
        glLinkProgram(program);
        glUseProgram(program);

        // Obtain uniform location
        int matLocation = glGetUniformLocation(program, "viewProjMatrix");
        long firstTime = System.nanoTime();

        while (glfwWindowShouldClose(window) == GL_FALSE) {
            long thisTime = System.nanoTime();
            float diff = (thisTime - firstTime) / 1E9f;
            float angle = diff * 20.0f;

            glViewport(0, 0, width, height);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Create a view-projection matrix
            viewProjMatrix.setPerspective(45.0f, (float) width / height, 0.01f, 100.0f)
                    .lookAt(0.0f, 2.0f, 3.0f, 0.0f, 0.5f, 0.0f, 0.0f, 1.0f, 0.0f).get(fb);
            // Upload the matrix stored in the FloatBuffer to the
            // shader uniform.
            glUniformMatrix4fv(matLocation, false, fb);
            // Render the grid without rotating
            renderGrid();

            // Build a rotation for the cube
            viewProjMatrix.rotate(angle, 0.0f, 1.0f, 0.0f).translate(0.0f, 0.5f, 0.0f).get(fb);
            // Upload the matrix
            glUniformMatrix4fv(matLocation, false, fb);
            // Render cube
            renderCube();

            glfwSwapBuffers(window);
        }
    }

    void loop() {
        /*
         * Spawn a new thread which to make the OpenGL context current in and which does the
         * rendering.
         */
        Thread t = new Thread(new Runnable() {
            public void run() {
                initOpenGLAndRenderInAnotherThread();
            }
        });
        t.start();

        /* Process window messages in the main thread */
        while (glfwWindowShouldClose(window) == GL_FALSE) {
            glfwPollEvents();
        }
    }

    public static void main(String[] args) {
        new ShaderExample().run();
    }
}