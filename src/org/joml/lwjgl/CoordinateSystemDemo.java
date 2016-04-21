package org.joml.lwjgl;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.stb.STBEasyFont.stb_easy_font_print;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

public class CoordinateSystemDemo {
    GLFWErrorCallback errorCallback;
    GLFWKeyCallback keyCallback;
    GLFWFramebufferSizeCallback fbCallback;
    GLFWCursorPosCallback cpCallback;
    GLFWMouseButtonCallback mbCallback;
    GLFWScrollCallback sCallback;

    long window;
    int width = 300;
    int height = 300;

    float minX, minY;
    float maxX, maxY;
    float lastMouseX, lastMouseY, lastMouseNX, lastMouseNY;
    float mouseX, mouseY, mouseNX, mouseNY;
    int[] viewport = new int[4];
    boolean translate;
    boolean rotate;
    Matrix4f projMatrix = new Matrix4f();
    Matrix4f viewMatrix = new Matrix4f();
    Matrix4f viewProjMatrix = new Matrix4f();
    Matrix4f invViewProj = new Matrix4f();
    Matrix4f control = new Matrix4f();
    Matrix4f tmp = new Matrix4f();
    FloatBuffer fb = BufferUtils.createFloatBuffer(16);
    Vector3f v = new Vector3f();
    ByteBuffer charBuffer = BufferUtils.createByteBuffer(32 * 270);

    void run() {
        try {
            init();
            loop();
            glfwDestroyWindow(window);
            keyCallback.free();
            fbCallback.free();
        } finally {
            glfwTerminate();
            errorCallback.free();
        }
    }

    void toWorld(float x, float y) {
        float nx = (float) x / width * 2.0f - 1.0f;
        float ny = (float) (height - y) / height * 2.0f - 1.0f;
        control.transformPosition(v.set(nx, ny, 0.0f));
    }

    void init() {
        glfwSetErrorCallback(errorCallback = GLFWErrorCallback.createPrint(System.err));
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");
        // Configure our window
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 4);
        window = glfwCreateWindow(width, height, "Hello coordinate system!", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");
        System.out.println("Drag with the left mouse key to move around");
        System.out.println("Drag with the right mouse key to rotate");
        System.out.println("Use the mouse wheel to zoom in/out");
        glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                    glfwSetWindowShouldClose(window, true);
            }
        });
        glfwSetCursorPosCallback(window, cpCallback = new GLFWCursorPosCallback() {
            public void invoke(long window, double x, double y) {
                mouseX = (float)x;
                mouseY = (float)y;
                mouseNX = (float) x / width * 2.0f - 1.0f;
                mouseNY = (float) (height - y) / height * 2.0f - 1.0f;
                if (translate) {
                    toWorld(mouseX, mouseY);
                    float wx = v.x, wy = v.y;
                    toWorld(lastMouseX, lastMouseY);
                    float wx2 = v.x, wy2 = v.y;
                    float dx = wx - wx2;
                    float dy = wy - wy2;
                    viewMatrix.translate(dx, dy, 0);
                } else if (rotate) {
                    float angle = (float) Math.atan2(mouseNX * lastMouseNY - mouseNY * lastMouseNX, mouseNX * lastMouseNX + mouseNY * lastMouseNY);
                    tmp.rotationZ(-angle).mulAffine(viewMatrix, viewMatrix);
                }
                lastMouseX = mouseX;
                lastMouseY = mouseY;
                lastMouseNX = mouseNX;
                lastMouseNY = mouseNY;
            }
        });
        glfwSetScrollCallback(window, sCallback = new GLFWScrollCallback() {
            public void invoke(long window, double xoffset, double yoffset) {
                float scale = 1.0f;
                if (yoffset > 0.0) {
                    scale = 1.1f;
                } else if (yoffset < 0.0) {
                    scale = 1.0f / 1.1f;
                }
                tmp.scaling(scale).mulAffine(viewMatrix, viewMatrix);
            }
        });
        glfwSetMouseButtonCallback(window, mbCallback = new GLFWMouseButtonCallback() {
            public void invoke(long window, int button, int action, int mods) {
                if (action == GLFW_PRESS && button == GLFW_MOUSE_BUTTON_LEFT) {
                    translate = true;
                    rotate = false;
                } else if (action == GLFW_PRESS && button == GLFW_MOUSE_BUTTON_RIGHT) {
                    translate = false;
                    rotate = true;
                } else if (action == GLFW_RELEASE) {
                    translate = false;
                    rotate = false;
                }
            }
        });
        glfwSetFramebufferSizeCallback(window, fbCallback = new GLFWFramebufferSizeCallback() {
            public void invoke(long window, int w, int h) {
                if (w > 0 && h > 0) {
                    width = w;
                    height = h;
                }
            }
        });
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (vidmode.width() - width) / 2, (vidmode.height() - height) / 2);
    }

    void computeVisibleExtents() {
        minX = Float.POSITIVE_INFINITY;
        minY = Float.POSITIVE_INFINITY;
        maxX = Float.NEGATIVE_INFINITY;
        maxY = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < 4; i++) {
            float x = ((i & 1) << 1) - 1.0f;
            float y = (((i >>> 1) & 1) << 1) - 1.0f;
            invViewProj.transformPosition(v.set(x, y, 0));
            minX = minX < v.x ? minX : v.x;
            minY = minY < v.y ? minY : v.y;
            maxX = maxX > v.x ? maxX : v.x;
            maxY = maxY > v.y ? maxY : v.y;
        }
    }

    float stippleOffsetY(int width) {
    	invViewProj.unprojectInv(v.set(0, 0, 0), viewport, v);
    	float x0 = v.x, y0 = v.y;
    	invViewProj.unprojectInv(v.set(0, width, 0), viewport, v);
    	float x1 = v.x, y1 = v.y;
    	float len = (float) Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0));
    	return y0 % len - len * 0.25f;
    }

    float stippleOffsetX(int width) {
    	invViewProj.unprojectInv(v.set(0, 0, 0), viewport, v);
    	float x0 = v.x, y0 = v.y;
    	invViewProj.unprojectInv(v.set(width, 0, 0), viewport, v);
    	float x1 = v.x, y1 = v.y;
    	float len = (float) Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0));
    	return x0 % len - len * 0.25f;
    }

    float tick(float range, float subs) {
        float tempStep = range / subs;
        float mag = (float) Math.floor(Math.log10(tempStep));
        float magPow = (float) Math.pow(10.0, mag);
        float magMsd = (int) (tempStep / magPow + 0.5f);
        if (magMsd > 5.0)
            magMsd = 10.0f;
        else if (magMsd > 2.0f)
            magMsd = 5.0f;
        else if (magMsd > 1.0f)
            magMsd = 2.0f;
        return magMsd * magPow;
    }

    void renderGrid() {
        glColor3f(0.5f, 0.5f, 0.5f);
        glEnable(GL_LINE_STIPPLE);
        glLineStipple(1, (short) 0x8888);
        glBegin(GL_LINES);
        float subticks = tick(Math.min(maxX - minX, maxY - minY), 12.0f);
        float sx = stippleOffsetX(16);
        float sy = stippleOffsetY(16);
        float startX = subticks * (float) Math.floor(minX / subticks);
        for (float x = startX; x <= maxX; x += subticks) {
            glVertex2f(x, minY - sy);
            glVertex2f(x, maxY + sy);
        }
        float startY = subticks * (float) Math.floor(minY / subticks);
        for (float y = startY; y <= maxY; y += subticks) {
            glVertex2f(minX - sx, y);
            glVertex2f(maxX + sx, y);
        }
        glEnd();
        glDisable(GL_LINE_STIPPLE);

        // unit square
        glColor3f(0.2f, 0.4f, 0.6f);
        glBegin(GL_LINES);
        for (int i = -1; i <= +1; i++) {
            glVertex2f(i, -1);
            glVertex2f(i, +1);
            glVertex2f(-1, i);
            glVertex2f(+1, i);
        }
        glEnd();

        // Render coordinate under mouse pointer
        glEnableClientState(GL_VERTEX_ARRAY);
        glVertexPointer(2, GL_FLOAT, 16, charBuffer);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        DecimalFormat frmt = new DecimalFormat("0.###E0");
        viewProjMatrix.set(projMatrix).mul(viewMatrix).unproject(v.set(mouseX, height - mouseY, 0), viewport, v);
        String str = frmt.format(v.x) + "\n" + frmt.format(v.y);
        float ndcX = (mouseX-viewport[0])/viewport[2]*2.0f-1.0f;
        float ndcY = (viewport[3]-mouseY-viewport[1])/viewport[3]*2.0f-1.0f;
        glLoadIdentity();
        glTranslatef(ndcX, ndcY, 0);
        int quads = stb_easy_font_print(0, 0, str, null, charBuffer);
        glScalef(1.0f / 400.0f, -1.0f / 400.0f, 0.0f);
        glTranslatef(5, -15, 0);
        glColor3f(0.0f, 0.0f, 0.0f);
        glDrawArrays(GL_QUADS, 0, quads * 4);
        glDisableClientState(GL_VERTEX_ARRAY);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
    }

    void loop() {
        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        glClearColor(0.97f, 0.97f, 0.97f, 1.0f);
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            glViewport(0, 0, width, height);
            viewport[2] = width; viewport[3] = height;
            float aspect = (float) width / height;
            glClear(GL_COLOR_BUFFER_BIT);
            projMatrix.identity().ortho2D(-aspect, +aspect, -1, +1);
            invViewProj.set(projMatrix).mulAffine(viewMatrix).invertAffine(invViewProj);
            if (!translate && !rotate)
                control.set(invViewProj);
            computeVisibleExtents();
            glMatrixMode(GL_PROJECTION);
            glLoadMatrixf(projMatrix.get(fb));
            glMatrixMode(GL_MODELVIEW);
            glLoadMatrixf(viewMatrix.get(fb));
            renderGrid();
            glfwSwapBuffers(window);
        }
    }

    public static void main(String[] args) {
        new CoordinateSystemDemo().run();
    }
}