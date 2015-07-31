package org.joml.lwjgl;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

public class ProjectiveShadowDemo {
    GLFWErrorCallback errorCallback;
    GLFWKeyCallback   keyCallback;
    GLFWFramebufferSizeCallback fbCallback;

    long window;
    int width;
    int height;

    // FloatBuffer for transferring matrices to OpenGL
    FloatBuffer fb = BufferUtils.createFloatBuffer(16);

    void run() {
        try {
            init();
            loop();

            glfwDestroyWindow(window);
            keyCallback.release();
        } finally {
            glfwTerminate();
            errorCallback.release();
        }
    }

    void init() {
        glfwSetErrorCallback(errorCallback = errorCallbackPrint(System.err));
        if (glfwInit() != GL11.GL_TRUE)
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure our window
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GL_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 4);

        int WIDTH = 300;
        int HEIGHT = 300;

        window = glfwCreateWindow(WIDTH, HEIGHT, "Hello World!", NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");

        glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key,
                    int scancode, int action, int mods) {
                if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                    glfwSetWindowShouldClose(window, GL_TRUE);
            }
        });
        glfwSetFramebufferSizeCallback(window,
                fbCallback = new GLFWFramebufferSizeCallback() {
            @Override
            public void invoke(long window, int w, int h) {
                if (w > 0 && h > 0) {
                    width = w;
                    height = h;
                }
            }
        });

        ByteBuffer vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(
            window,
            (GLFWvidmode.width(vidmode) - WIDTH) / 2,
            (GLFWvidmode.height(vidmode) - HEIGHT) / 2
        );

        glfwMakeContextCurrent(window);
        glfwSwapInterval(0);
        glfwShowWindow(window);
    }

    void renderPlane() {
        glBegin(GL_QUADS);
        glColor3f(0.5f, 0.6f, 0.7f);
        glVertex3f(-1.0f, 0.0f, -1.0f);
        glVertex3f( 1.0f, 0.0f, -1.0f);
        glVertex3f( 1.0f, 0.0f,  1.0f);
        glVertex3f(-1.0f, 0.0f,  1.0f);
        glEnd();
    }

    void renderCube(boolean shadow) {
        glBegin(GL_QUADS);
        if (shadow) {
            glColor3f(0.2f, 0.2f, 0.2f);
        } else {
            glColor3f(0.0f, 0.0f, 0.2f);
        }
        glVertex3f(  0.5f, -0.5f, -0.5f );
        glVertex3f(  0.5f,  0.5f, -0.5f );
        glVertex3f( -0.5f,  0.5f, -0.5f );
        glVertex3f( -0.5f, -0.5f, -0.5f );
        if (!shadow) {
            glColor3f(0.0f, 0.0f, 1.0f);
        }
        glVertex3f(  0.5f, -0.5f,  0.5f );
        glVertex3f(  0.5f,  0.5f,  0.5f );
        glVertex3f( -0.5f,  0.5f,  0.5f );
        glVertex3f( -0.5f, -0.5f,  0.5f );
        if (!shadow) {
            glColor3f(1.0f, 0.0f, 0.0f);
        }
        glVertex3f(  0.5f, -0.5f, -0.5f );
        glVertex3f(  0.5f,  0.5f, -0.5f );
        glVertex3f(  0.5f,  0.5f,  0.5f );
        glVertex3f(  0.5f, -0.5f,  0.5f );
        if (!shadow) {
            glColor3f(0.2f, 0.0f, 0.0f);
        }
        glVertex3f( -0.5f, -0.5f,  0.5f );
        glVertex3f( -0.5f,  0.5f,  0.5f );
        glVertex3f( -0.5f,  0.5f, -0.5f );
        glVertex3f( -0.5f, -0.5f, -0.5f );
        if (!shadow) {
            glColor3f(0.0f, 1.0f, 0.0f);
        }
        glVertex3f(  0.5f,  0.5f,  0.5f );
        glVertex3f(  0.5f,  0.5f, -0.5f );
        glVertex3f( -0.5f,  0.5f, -0.5f );
        glVertex3f( -0.5f,  0.5f,  0.5f );
        if (!shadow) {
            glColor3f(0.0f, 0.2f, 0.0f);
        }
        glVertex3f(  0.5f, -0.5f, -0.5f );
        glVertex3f(  0.5f, -0.5f,  0.5f );
        glVertex3f( -0.5f, -0.5f,  0.5f );
        glVertex3f( -0.5f, -0.5f, -0.5f );
        glEnd();
    }

    void loop() {
        GLContext.createFromCurrent();

        glClearColor(0.6f, 0.7f, 0.8f, 1.0f);
        glEnable(GL_DEPTH_TEST);

        long firstTime = System.nanoTime();

        Matrix4f m = new Matrix4f();
        Matrix4f m2 = new Matrix4f();
        Matrix4f planeTransform = new Matrix4f().translate(0.0f, -2.0f, 0.0f).scale(10.0f);
        Vector3f lightPos = new Vector3f();

        while ( glfwWindowShouldClose(window) == GL_FALSE ) {
            long thisTime = System.nanoTime();
            float diff = (thisTime - firstTime) / 1E9f;
            float angle = diff;

            glViewport(0, 0, width, height);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            m.setPerspective((float) Math.toRadians(45.0f), (float)width/height,
                             0.01f, 100.0f).get(fb);
            glMatrixMode(GL_PROJECTION);
            glLoadMatrixf(fb);

            m.setLookAt(1.0f, 6.0f, 12.0f,
                        0.0f, 0.0f, 0.0f,
                        0.0f, 1.0f, 0.0f)
                      .rotateY(angle / 5.0f).get(fb);
            glMatrixMode(GL_MODELVIEW);
            glLoadMatrixf(fb);

            renderCube(false);

            // Render the plane on which to project the shadow
            m.mul(planeTransform, m2).with(m2).get(fb);
            glLoadMatrixf(fb);
            renderPlane();

            // Render projected shadow of the cube
            m2.rotationY(angle).transform(lightPos.set(2, 2, 2));
            m.shadow(lightPos.x, lightPos.y, lightPos.z, 1.0f, 0.0f, 1.0f, 0.0f, 2.0f).get(fb);
            glLoadMatrixf(fb);
            glEnable(GL_POLYGON_OFFSET_FILL);
            // use polygon offset to combat z-fighting between plane and projected shadow
            glPolygonOffset(-1.f,-1.f);
            renderCube(true);
            glDisable(GL_POLYGON_OFFSET_FILL);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    public static void main(String[] args) {
        new ProjectiveShadowDemo().run();
    }
}