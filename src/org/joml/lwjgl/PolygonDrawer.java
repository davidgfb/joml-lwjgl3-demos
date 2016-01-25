package org.joml.lwjgl;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAddress;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;

import org.joml.PolygonPointIntersection;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

/**
 * This demo showcases the {@link PolygonPointIntersection} algorithm. The outlines of a polygon can be drawn with the mouse and an intersection test is
 * performed on every mouse movement to color the polygon in red if the mouse cursor is inside; or black if not.
 * 
 * @author Kai Burjack
 */
public class PolygonDrawer {
    GLFWErrorCallback errorCallback;
    GLFWKeyCallback keyCallback;
    GLFWFramebufferSizeCallback fbCallback;
    GLFWCursorPosCallback cpCallback;
    GLFWMouseButtonCallback mbCallback;
    PolygonPointIntersection pointIntersection;

    long window;
    int width = 800;
    int height = 600;
    int x, y;
    boolean down;
    float[] verticesXY = new float[1024 * 1024];
    int num = 0;
    boolean inside;

    void run() {
        try {
            init();
            loop();

            glfwDestroyWindow(window);
            keyCallback.release();
            fbCallback.release();
            cpCallback.release();
            mbCallback.release();
        } finally {
            glfwTerminate();
            errorCallback.release();
        }
    }

    void init() {
        glfwSetErrorCallback(errorCallback = GLFWErrorCallback.createPrint(System.err));
        if (glfwInit() != GL11.GL_TRUE)
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure our window
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GL_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 4);

        System.out.println("Draw a polygon with holding the left mouse button down");
        System.out.println("Move the mouse cursor in and out of the polygon");
        System.out.println("Press 'S' to load save the current polygon in file 'poly.gon'");
        System.out.println("Press 'L' to load a previously saved polygon from file 'poly.gon'");

        window = glfwCreateWindow(width, height, "Polygon Demo", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");
        long cursor = glfwCreateStandardCursor(GLFW.GLFW_CROSSHAIR_CURSOR);
        glfwSetCursor(window, cursor);

        glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                    glfwSetWindowShouldClose(window, GL_TRUE);
                if (key == GLFW_KEY_L && action == GLFW_RELEASE) {
                    try {
                        RandomAccessFile rFile = new RandomAccessFile("poly.gon", "r");
                        int size = (int) rFile.length();
                        FileChannel inChannel = rFile.getChannel();
                        ByteBuffer buf_in = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
                        while (buf_in.hasRemaining()) {
                            inChannel.read(buf_in);
                        }
                        buf_in.flip();
                        FloatBuffer fb = buf_in.asFloatBuffer();
                        num = fb.remaining() / 2;
                        fb.get(verticesXY, 0, num * 2);
                        inChannel.close();
                        rFile.close();
                        pointIntersection = new PolygonPointIntersection(verticesXY, num);
                    } catch (IOException e) {
                        // just ignore everything :)
                    }
                } else if (key == GLFW_KEY_S && action == GLFW_RELEASE) {
                    try {
                        RandomAccessFile rFile = new RandomAccessFile("poly.gon", "rw");
                        rFile.setLength(0);
                        FileChannel inChannel = rFile.getChannel();
                        ByteBuffer buf_in = ByteBuffer.allocateDirect(num * 2 * 4).order(ByteOrder.nativeOrder());
                        FloatBuffer fb = buf_in.asFloatBuffer();
                        fb.put(verticesXY, 0, num * 2);
                        while (buf_in.hasRemaining()) {
                            inChannel.write(buf_in);
                        }
                        inChannel.close();
                        rFile.close();
                    } catch (IOException e) {
                        // just ignore everything :)
                    }
                }
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
                x = (int) xpos;
                y = (int) ypos;
                if (down) {
                    verticesXY[2 * num + 0] = x;
                    verticesXY[2 * num + 1] = y;
                    num++;
                } else {
                    if (pointIntersection != null)
                        inside = pointIntersection.pointInPolygon(x, y);
                    else
                        inside = false;
                }
            }
        });
        glfwSetMouseButtonCallback(window, mbCallback = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                if (action == GLFW_PRESS) {
                    down = true;
                    num = 0;
                    inside = false;
                } else if (action == GLFW_RELEASE) {
                    down = false;
                    pointIntersection = new PolygonPointIntersection(verticesXY, num);
                }
            }
        });

        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (vidmode.width() - width) / 2, (vidmode.height() - height) / 2);

        IntBuffer framebufferSize = BufferUtils.createIntBuffer(2);
        nglfwGetFramebufferSize(window, memAddress(framebufferSize), memAddress(framebufferSize) + 4);
        width = framebufferSize.get(0);
        height = framebufferSize.get(1);

        glfwMakeContextCurrent(window);
        glfwSwapInterval(0);
        glfwShowWindow(window);
    }

    void renderPolygon() {
        glBegin(GL_LINE_STRIP);
        if (num > 0) {
            for (int i = 0; i < num; i++) {
                if (i == num - 1 && down)
                    glColor3f(0.8f, 0.8f, 0.8f);
                glVertex2f(verticesXY[2 * i + 0], verticesXY[2 * i + 1]);
            }
            glVertex2f(verticesXY[0], verticesXY[1]);
        }
        glEnd();
    }

    void loop() {
        GL.createCapabilities();

        glClearColor(0.99f, 0.99f, 0.99f, 1.0f);
        glLineWidth(1.8f);

        while (glfwWindowShouldClose(window) == GL_FALSE) {
            glViewport(0, 0, width, height);
            glClear(GL_COLOR_BUFFER_BIT);

            glMatrixMode(GL_PROJECTION);
            glLoadIdentity();
            glOrtho(0, width, height, 0, -1, 11);
            glMatrixMode(GL_MODELVIEW);
            glLoadIdentity();

            if (inside)
                glColor3f(1.0f, 0.3f, 0.3f);
            else
                glColor3f(0.01f, 0.01f, 0.01f);
            renderPolygon();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    public static void main(String[] args) {
        new PolygonDrawer().run();
    }
}