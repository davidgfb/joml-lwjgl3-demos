package org.joml.lwjgl;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/**
 * Simple arcball camera demo.
 * 
 * @author Kai Burjack
 */
public class ArcBallCameraDemo {
    private static long window;
    private static int width = 800;
    private static int height = 600;
    private static float zoom = 20;
    private static int mouseX, mouseY;
    private static final Vector3f center = new Vector3f();
    private static float pitch = 0.3f, yaw = 0.2f;

    private static void renderCube() {
        glBegin(GL_QUADS);
        glColor3f(0.0f, 0.0f, 0.2f);
        glVertex3f(0.5f, -0.5f, -0.5f);
        glVertex3f(0.5f, 0.5f, -0.5f);
        glVertex3f(-0.5f, 0.5f, -0.5f);
        glVertex3f(-0.5f, -0.5f, -0.5f);
        glColor3f(0.0f, 0.0f, 1.0f);
        glVertex3f(0.5f, -0.5f, 0.5f);
        glVertex3f(0.5f, 0.5f, 0.5f);
        glVertex3f(-0.5f, 0.5f, 0.5f);
        glVertex3f(-0.5f, -0.5f, 0.5f);
        glColor3f(1.0f, 0.0f, 0.0f);
        glVertex3f(0.5f, -0.5f, -0.5f);
        glVertex3f(0.5f, 0.5f, -0.5f);
        glVertex3f(0.5f, 0.5f, 0.5f);
        glVertex3f(0.5f, -0.5f, 0.5f);
        glColor3f(0.2f, 0.0f, 0.0f);
        glVertex3f(-0.5f, -0.5f, 0.5f);
        glVertex3f(-0.5f, 0.5f, 0.5f);
        glVertex3f(-0.5f, 0.5f, -0.5f);
        glVertex3f(-0.5f, -0.5f, -0.5f);
        glColor3f(0.0f, 1.0f, 0.0f);
        glVertex3f(0.5f, 0.5f, 0.5f);
        glVertex3f(0.5f, 0.5f, -0.5f);
        glVertex3f(-0.5f, 0.5f, -0.5f);
        glVertex3f(-0.5f, 0.5f, 0.5f);
        glColor3f(0.0f, 0.2f, 0.0f);
        glVertex3f(0.5f, -0.5f, -0.5f);
        glVertex3f(0.5f, -0.5f, 0.5f);
        glVertex3f(-0.5f, -0.5f, 0.5f);
        glVertex3f(-0.5f, -0.5f, -0.5f);
        glEnd();
    }

    private static void renderGrid() {
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

    public static void main(String[] args) {
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");
        window = glfwCreateWindow(width, height, "Hello ArcBall Camera!", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");
        System.out.println("Drag the mouse while holding the left mouse button to rotate the camera");
        System.out.println("Press ENTER to change the center position");
        System.out.println("Scroll the mouse-wheel to zoom in/out");
        glfwSetKeyCallback(window, (win, k, s, a, m) -> {
            if (k == GLFW_KEY_ESCAPE && a == GLFW_RELEASE)
                glfwSetWindowShouldClose(window, true);
            if (k == GLFW_KEY_ENTER && a == GLFW_PRESS) {
                center.set((float) Math.random() * 20.0f - 10.0f, 0.0f, (float) Math.random() * 20.0f - 10.0f);
            }
        });
        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            if (w > 0 && h > 0) {
                width = w;
                height = h;
            }
        });
        glfwSetCursorPosCallback(window, (win, x, y) -> {
            if (glfwGetMouseButton(win, GLFW_MOUSE_BUTTON_1) == GLFW_PRESS) {
                yaw += ((int) x - mouseX) * 0.01f;
                pitch += ((int) y - mouseY) * 0.01f;
            }
            mouseX = (int) x;
            mouseY = (int) y;
        });
        glfwSetScrollCallback(window, (win, x, y) -> {
            if (y > 0) {
                zoom /= 1.1f;
            } else {
                zoom *= 1.1f;
            }
        });
        IntBuffer framebufferSize = BufferUtils.createIntBuffer(2);
        nglfwGetFramebufferSize(window, memAddress(framebufferSize), memAddress(framebufferSize) + 4);
        width = framebufferSize.get(0);
        height = framebufferSize.get(1);
        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        glClearColor(0.9f, 0.9f, 0.9f, 1.0f);
        glEnable(GL_DEPTH_TEST);
        Matrix4f mat = new Matrix4f();
        FloatBuffer fb = BufferUtils.createFloatBuffer(16);
        while (!glfwWindowShouldClose(window)) {
            glViewport(0, 0, width, height);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glMatrixMode(GL_PROJECTION);
            glLoadMatrixf(
                    mat.setPerspective((float) Math.toRadians(45.0f), (float) width / height, 0.01f, 100.0f)
                       .get(fb));
            glMatrixMode(GL_MODELVIEW);
            // Load arcball camera view matrix into 'mat':
            glLoadMatrixf(
                mat.translation(0, 0, -zoom)
                   .rotateX(pitch)
                   .rotateY(yaw)
                   .translate(-center.x, -center.y, -center.z)
                   .get(fb));
            renderGrid();
            // apply model transformation to 'mat':
            glLoadMatrixf(
                mat.translate(center)
                   .get(fb));
            renderCube();
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }
}