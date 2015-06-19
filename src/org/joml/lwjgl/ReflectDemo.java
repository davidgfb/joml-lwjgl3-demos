package org.joml.lwjgl;

import static org.lwjgl.glfw.Callbacks.errorCallbackPrint;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAddress;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.joml.Matrix4f;
import org.joml.Quaternion;
import org.joml.Vector3f;
import org.joml.camera.ArcBallCamera;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWvidmode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

/**
 * Showcases the use of
 * {@link Matrix4f#reflect(float, float, float, float, float, float)
 * Matrix4f.reflect()} with stencil reflections.
 * <p>
 * This demo also makes use of joml-camera with the {@link ArcBallCamera}.
 * 
 * @author Kai Burjack
 */
public class ReflectDemo {
    GLFWErrorCallback errorCallback;
    GLFWKeyCallback keyCallback;
    GLFWFramebufferSizeCallback fbCallback;
    GLFWCursorPosCallback cpCallback;
    GLFWScrollCallback sCallback;
    GLFWMouseButtonCallback mbCallback;

    long window;
    int width = 800;
    int height = 600;
    int x, y;
    float zoom = 20;
    int mouseX, mouseY;
    boolean down;

    void run() {
        try {
            init();
            loop();

            glfwDestroyWindow(window);
            keyCallback.release();
            fbCallback.release();
            cpCallback.release();
            sCallback.release();
            mbCallback.release();
        } finally {
            glfwTerminate();
            errorCallback.release();
        }
    }

    ArcBallCamera cam = new ArcBallCamera();

    void init() {
        glfwSetErrorCallback(errorCallback = errorCallbackPrint(System.err));
        if (glfwInit() != GL11.GL_TRUE)
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure our window
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GL_TRUE);

        window = glfwCreateWindow(width, height, "Hello ArcBall Camera!", NULL, NULL);
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
        glfwSetCursorPosCallback(window, cpCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                x = (int) xpos - width / 2;
                y = height / 2 - (int) ypos;
            }
        });
        glfwSetMouseButtonCallback(window, mbCallback = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                if (action == GLFW_PRESS) {
                    down = true;
                    mouseX = x;
                    mouseY = y;
                } else if (action == GLFW_RELEASE) {
                    down = false;
                }
            }
        });
        glfwSetScrollCallback(window, sCallback = new GLFWScrollCallback() {
            @Override
            public void invoke(long window, double xoffset, double yoffset) {
                if (yoffset > 0) {
                    zoom /= 1.1f;
                } else {
                    zoom *= 1.1f;
                }
            }
        });

        ByteBuffer vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (GLFWvidmode.width(vidmode) - width) / 2, (GLFWvidmode.height(vidmode) - height) / 2);

        IntBuffer framebufferSize = BufferUtils.createIntBuffer(2);
        nglfwGetFramebufferSize(window, memAddress(framebufferSize), memAddress(framebufferSize) + 4);
        width = framebufferSize.get(0);
        height = framebufferSize.get(1);

        glfwMakeContextCurrent(window);
        glfwSwapInterval(0);
        glfwShowWindow(window);
    }

    void renderMirror() {
        glBegin(GL_QUADS);
        glVertex3f(-0.5f, -0.5f, 0.0f);
        glVertex3f(0.5f, -0.5f, 0.0f);
        glVertex3f(0.5f, 0.5f, 0.0f);
        glVertex3f(-0.5f, 0.5f, 0.0f);
        glEnd();
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
        glColor3f(0.2f, 0.2f, 0.2f);
        for (int i = -20; i <= 20; i++) {
            glVertex3f(-20.0f, 0.0f, i);
            glVertex3f(20.0f, 0.0f, i);
            glVertex3f(i, 0.0f, -20.0f);
            glVertex3f(i, 0.0f, 20.0f);
        }
        glEnd();
    }

    void loop() {
        GLContext.createFromCurrent();

        // Set the clear color
        glClearColor(0.9f, 0.9f, 0.9f, 1.0f);
        // Enable depth testing
        glEnable(GL_DEPTH_TEST);
        glLineWidth(1.4f);

        // Remember the current time.
        long lastTime = System.nanoTime();

        Matrix4f mat = new Matrix4f();
        // FloatBuffer for transferring matrices to OpenGL
        FloatBuffer fb = BufferUtils.createFloatBuffer(16);

        cam.setAlpha(-20.0f);
        cam.setBeta(20.0f);

        /* Build the transformation of the mirror object */
        Matrix4f mirrorMatrix = new Matrix4f();
        Vector3f mirrorPosition = new Vector3f(0.0f, 3.0f, -3.0f);
        Vector3f mirrorNormal = new Vector3f(0.0f, -1.0f, 5.0f);
        mirrorMatrix.translate(mirrorPosition)
                    .rotate(new Quaternion().lookRotate(mirrorNormal.x, mirrorNormal.y, mirrorNormal.z,
                                                        0.0f, 1.0f, 0.0f).invert())
                    .scale(5.0f);

        /* Build the reflection matrix */
        Matrix4f reflectMatrix = new Matrix4f();
        reflectMatrix.reflect(mirrorNormal.x, mirrorNormal.y, mirrorNormal.z, 
                              mirrorPosition.x, mirrorPosition.y, mirrorPosition.z);

        while (glfwWindowShouldClose(window) == GL_FALSE) {
            /* Set input values for the camera */
            if (down) {
                cam.setAlpha(cam.getAlpha() + (x - mouseX) * 0.1f);
                cam.setBeta(cam.getBeta() + (mouseY - y) * 0.1f);
                mouseX = x;
                mouseY = y;
            }
            cam.zoom(zoom);

            /* Compute delta time */
            long thisTime = System.nanoTime();
            float diff = (float) ((thisTime - lastTime) / 1E9);
            lastTime = thisTime;
            /* And let the camera make its update */
            cam.update(diff);

            glViewport(0, 0, width, height);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

            mat.setPerspective(45.0f, (float) width / height, 0.01f, 100.0f).get(fb);
            glMatrixMode(GL_PROJECTION);
            glLoadMatrixf(fb);

            /*
             * Obtain the camera's view matrix and render grid.
             */
            cam.viewMatrix(mat.identity()).get(fb);
            glMatrixMode(GL_MODELVIEW);
            glLoadMatrixf(fb);

            /* Stencil the mirror */
            glPushMatrix();
            mirrorMatrix.get(fb);
            glMultMatrixf(fb);
            glEnable(GL_STENCIL_TEST);
            glColorMask(false, false, false, false);
            glDisable(GL_DEPTH_TEST);
            glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE);
            glStencilFunc(GL_ALWAYS, 1, 1);
            glEnable(GL_CULL_FACE);
            renderMirror();
            glDisable(GL_CULL_FACE);
            glColorMask(true, true, true, true);
            glEnable(GL_DEPTH_TEST);
            glStencilFunc(GL_EQUAL, 1, 1);
            glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
            glPopMatrix();

            /* Render the reflected scene */
            glPushMatrix();
            reflectMatrix.get(fb);
            glMultMatrixf(fb);
            renderGrid();
            renderCube();
            glPopMatrix();
            glDisable(GL_STENCIL_TEST);

            /* Render scene normally */
            renderGrid();
            renderCube();

            /* Render visible mirror geometry with blending */
            glPushMatrix();
            mirrorMatrix.get(fb);
            glMultMatrixf(fb);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glColor4f(1, 1, 1, 0.5f);
            renderMirror();
            glDisable(GL_BLEND);
            glPopMatrix();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    public static void main(String[] args) {
        new ReflectDemo().run();
    }
}