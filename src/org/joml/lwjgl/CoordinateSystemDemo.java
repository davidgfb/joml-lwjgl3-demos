package org.joml.lwjgl;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBEasyFont.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.text.DecimalFormat;

import org.joml.Intersectionf;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.camera.OrthoCameraControl;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.opengl.GL;

public class CoordinateSystemDemo {
    GLFWErrorCallback errorCallback;
    GLFWKeyCallback keyCallback;
    GLFWFramebufferSizeCallback fbCallback;
    GLFWWindowSizeCallback wsCallback;
    GLFWCursorPosCallback cpCallback;
    GLFWMouseButtonCallback mbCallback;
    GLFWScrollCallback sCallback;

    long window;
    int width = 1280;
    int height = 720;
    int fbWidth = 1280;
    int fbHeight = 720;

    float minX, minY;
    float maxX, maxY;
    float mouseX, mouseY;
    int[] viewport = new int[4];
    boolean translate;
    boolean rotate;
    OrthoCameraControl cam = new OrthoCameraControl(4);
    Matrix3x2f tmp = new Matrix3x2f();
    FloatBuffer fb = BufferUtils.createFloatBuffer(16);
    Vector4f rect = new Vector4f();
    Vector3f v = new Vector3f();
    Vector3f v2 = new Vector3f();
    Vector2f p = new Vector2f();
    ByteBuffer charBuffer = BufferUtils.createByteBuffer(32 * 270);
    float textScale = 3.1f;
    float maxTicks = 17.0f;

    DecimalFormat frmt = new DecimalFormat("0.###");

    void run() {
        try {
            init();
            loop();
            glfwDestroyWindow(window);
            keyCallback.free();
            fbCallback.free();
            wsCallback.free();
            cpCallback.free();
            mbCallback.free();
            sCallback.free();
        } finally {
            glfwTerminate();
            errorCallback.free();
        }
    }

    void toWorld(float x, float y) {
        float nx = (float) x / width * 2.0f - 1.0f;
        float ny = (float) (height - y) / height * 2.0f - 1.0f;
        cam.invviewproj().transformPosition(v.set(nx, ny, 0));
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
                cam.onMouseMove((int) x, height - (int) y);
                mouseX = (float) x;
                mouseY = (float) y;
            }
        });
        glfwSetScrollCallback(window, sCallback = new GLFWScrollCallback() {
            public void invoke(long window, double xoffset, double yoffset) {
                float scale;
                if (yoffset > 0.0) {
                    scale = 1.2f;
                } else {
                    scale = 1.0f / 1.2f;
                }
                cam.zoom(scale);
            }
        });
        glfwSetMouseButtonCallback(window, mbCallback = new GLFWMouseButtonCallback() {
            public void invoke(long window, int button, int action, int mods) {
                if (action == GLFW_PRESS)
                    cam.onMouseDown(button);
                else
                    cam.onMouseUp(button);
            }
        });
        glfwSetFramebufferSizeCallback(window, fbCallback = new GLFWFramebufferSizeCallback() {
            public void invoke(long window, int w, int h) {
                if (w > 0 && h > 0) {
                    fbWidth = w;
                    fbHeight = h;
                }
            }
        });
        glfwSetWindowSizeCallback(window, wsCallback = new GLFWWindowSizeCallback() {
            public void invoke(long window, int w, int h) {
                if (w > 0 && h > 0) {
                    width = w;
                    height = h;
                    cam.setSize(w, h);
                }
            }
        });
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (vidmode.width() - width) / 2, (vidmode.height() - height) / 2);
        IntBuffer framebufferSize = BufferUtils.createIntBuffer(2);
        nglfwGetFramebufferSize(window, memAddress(framebufferSize), memAddress(framebufferSize) + 4);
        fbWidth = framebufferSize.get(0);
        fbHeight = framebufferSize.get(1);
        cam.setSize(width, height);
    }

    float stippleOffsetY(int width) {
        cam.invviewproj().unprojectInv(0, 0, 0, viewport, v);
        float x0 = v.x, y0 = v.y;
        cam.invviewproj().unprojectInv(0, width, 0, viewport, v);
        float x1 = v.x, y1 = v.y;
        float len = (float) Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0));
        return y0 % len - len * 0.25f;
    }

    float stippleOffsetX(int width) {
        cam.invviewproj().unprojectInv(0, 0, 0, viewport, v);
        float x0 = v.x, y0 = v.y;
        cam.invviewproj().unprojectInv(width, 0, 0, viewport, v);
        float x1 = v.x, y1 = v.y;
        float len = (float) Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0));
        return x0 % len - len * 0.25f;
    }

    float tick(float range, float subs) {
        return tick(range, subs, false);
    }

    float tick(float range, float subs, boolean sub) {
        float tempStep = range / subs;
        float mag = (float) Math.floor(Math.log10(tempStep));
        float magPow = (float) Math.pow(10.0, mag);
        float magMsd = (int) (tempStep / magPow + 0.5f);
        if (magMsd > 5.0)
            magMsd = sub ? 2.0f : 10.0f;
        else if (magMsd > 2.0f)
            magMsd = sub ? 1.0f : 5.0f;
        else if (magMsd > 1.0f)
            magMsd = sub ? 0.5f : 2.0f;
        else if (magMsd == 1.0f)
            magMsd = sub ? 0.2f : 1.0f;
        return magMsd * magPow;
    }

    float diagonal() {
        cam.invviewproj().transformPosition(v.set(-1, -1, 0));
        float x = v.x, y = v.y;
        cam.invviewproj().transformPosition(v.set(+1, +1, 0));
        float x2 = v.x, y2 = v.y;
        return (float) Math.sqrt((x2 - x) * (x2 - x) + (y2 - y) * (y2 - y));
    }

    float px(int px) {
        cam.invviewproj().unprojectInv(0, 0, 0, viewport, v);
        float x0 = v.x, y0 = v.y;
        cam.invviewproj().unprojectInv(px, 0, 0, viewport, v);
        float x1 = v.x, y1 = v.y;
        return (float) Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0));
    }

    void renderGrid() {
        Vector2f min = new Vector2f();
        Vector2f v0 = new Vector2f();
        Vector2f v1 = new Vector2f();
        cam.viewSpan(min, v0, v1);
        glColor3f(0.6f, 0.6f, 0.6f);
        glLineWidth(2.1f);
        glBegin(GL_LINES);
        float sx = stippleOffsetX(16);
        float sy = stippleOffsetY(16);
        float subticks = tick(diagonal(), maxTicks, true);
        float startX = subticks * (float) Math.floor(minX / subticks);
        float subtickLen = px(5);
        for (float x = startX; x <= maxX; x += subticks) {
            glVertex2f(x, 0);
            glVertex2f(x, +subtickLen);
        }
        float startY = subticks * (float) Math.floor(minY / subticks);
        for (float y = startY; y <= maxY; y += subticks) {
            glVertex2f(-subtickLen, y);
            glVertex2f(0, y);
        }
        glEnd();
        glLineWidth(1.2f);
        glEnable(GL_LINE_STIPPLE);
        glLineStipple(1, (short) 0xFC);
        glBegin(GL_LINES);
        float ticks = tick(diagonal(), maxTicks);
        startX = ticks * (float) Math.floor(minX / ticks);
        for (float x = startX; x <= maxX; x += ticks) {
            glVertex2f(x, minY - sy);
            glVertex2f(x, maxY + sy);
        }
        startY = ticks * (float) Math.floor(minY / ticks);
        for (float y = startY; y <= maxY; y += ticks) {
            glVertex2f(minX - sx, y);
            glVertex2f(maxX + sx, y);
        }
        glEnd();
        glDisable(GL_LINE_STIPPLE);

        // Main axes
        glLineWidth(1.7f);
        glBegin(GL_LINES);
        glColor3f(0.5f, 0.2f, 0.2f);
        glVertex2f(minX, 0);
        glVertex2f(maxX, 0);
        glColor3f(0.2f, 0.5f, 0.2f);
        glVertex2f(0, minY);
        glVertex2f(0, maxY);
        glEnd();
        glLineWidth(1.0f);

        // unit square
        glColor3f(0.2f, 0.4f, 0.6f);
        glLineWidth(1.9f);
        glBegin(GL_LINES);
        for (int i = -1; i <= +1; i++) {
            if (i == 0)
                continue;
            glVertex2f(i, -1);
            glVertex2f(i, +1);
            glVertex2f(-1, i);
            glVertex2f(+1, i);
        }
        glEnd();
        glLineWidth(1.0f);
    }

    boolean snapX(float edge, float x2, float y2, float x3, float y3) {
        cam.invviewproj().transformPosition(v2.set(edge, +1, 0));
        float x0 = v2.x, y0 = v2.y;
        cam.invviewproj().transformPosition(v2.set(edge, -1, 0));
        float x1 = v2.x, y1 = v2.y;
        if (Intersectionf.intersectLineLine(x0, y0, x1, y1, x2, y2, x3, y3, p)) {
            cam.viewproj().transformPosition(v2.set(p.x, p.y, 0));
            return v2.x >= -1.1f && v2.y >= -1.1f && v2.x <= 1.1f && v2.y <= 1.1f;
        }
        return false;
    }

    boolean snapY(float edge, float x2, float y2, float x3, float y3) {
        cam.invviewproj().transformPosition(v2.set(-1, edge, 0));
        float x0 = v2.x, y0 = v2.y;
        cam.invviewproj().transformPosition(v2.set(+1, edge, 0));
        float x1 = v2.x, y1 = v2.y;
        if (Intersectionf.intersectLineLine(x0, y0, x1, y1, x2, y2, x3, y3, p)) {
            cam.viewproj().transformPosition(v2.set(p.x, p.y, 0));
            return v2.x >= -1.1f && v2.y >= -1.1f && v2.x <= 1.1f && v2.y <= 1.1f;
        }
        return false;
    }

    float textWidth(String text) {
        return stb_easy_font_width(text) * textScale / width;
    }

    float textHeight(String text) {
        return stb_easy_font_height(text) * textScale / height;
    }

    void renderTickLabels() {
        glEnableClientState(GL_VERTEX_ARRAY);
        glVertexPointer(2, GL_FLOAT, 16, charBuffer);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        float subticks = tick(diagonal(), maxTicks);
        float startX = subticks * (float) Math.floor(minX / subticks);
        float xoff = 8.0f / width;
        float yoff = 8.0f / height;
        for (float x = startX; x <= maxX; x += subticks) {
            if (Math.abs(x) < 1E-5f)
                continue;
            String text = frmt.format(x);
            float textWidth = textWidth(text);
            float textHeight = textHeight(text);
            cam.viewproj().transformPosition(v.set(x, 0, 0));
            if (v.x < -1 && snapX(-1, x, -1, x, +1)) {
                glColor3f(0.5f, 0.3f, 0.3f);
                v.set(v2);
                v.x += xoff;
            } else if (v.x > +1 && snapX(+1, x, -1, x, +1)) {
                glColor3f(0.5f, 0.3f, 0.3f);
                v.set(v2);
                v.x -= textWidth + xoff;
            } else if (v.y < -1 && snapY(-1, x, -1, x, +1)) {
                glColor3f(0.5f, 0.3f, 0.3f);
                v.set(v2);
                v.y += textHeight * 0.8f;
            } else if (v.y > +1 && snapY(+1, x, -1, x, +1)) {
                glColor3f(0.5f, 0.3f, 0.3f);
                v.set(v2);
                v.y -= yoff;
            } else {
                glColor3f(0.3f, 0.3f, 0.3f);
                v.y -= yoff;
                v.x += xoff;
            }
            glLoadIdentity();
            glTranslatef(v.x, v.y, 0);
            glScalef(textScale / width, -textScale / height, 0.0f);
            int quads = stb_easy_font_print(0, 0, text, null, charBuffer);
            glDrawArrays(GL_QUADS, 0, quads * 4);
        }
        float startY = subticks * (float) Math.floor(minY / subticks);
        for (float y = startY; y <= maxY; y += subticks) {
            if (Math.abs(y) < 1E-5f)
                continue;
            String text = frmt.format(y);
            float textWidth = textWidth(text);
            float textHeight = textHeight(text);
            cam.viewproj().transformPosition(v.set(0, y, 0));
            if (v.y < -1 && snapY(-1, -1, y, +1, y)) {
                glColor3f(0.3f, 0.5f, 0.3f);
                v.set(v2);
                v.y += textHeight * 0.8f;
            } else if (v.y > +1 && snapY(+1, -1, y, +1, y)) {
                glColor3f(0.3f, 0.5f, 0.3f);
                v.set(v2);
                v.y -= yoff;
            } else if (v.x < -1 && snapX(-1, -1, y, +1, y)) {
                glColor3f(0.3f, 0.5f, 0.3f);
                v.set(v2);
                v.x += xoff;
            } else if (v.x > +1 && snapX(+1, -1, y, +1, y)) {
                glColor3f(0.3f, 0.5f, 0.3f);
                v.set(v2);
                v.x -= textWidth + xoff;
            } else {
                v.x += xoff;
                v.y -= yoff;
                glColor3f(0.3f, 0.3f, 0.3f);
            }
            glLoadIdentity();
            glTranslatef(v.x, v.y, 0);
            glScalef(textScale / width, -textScale / height, 0.0f);
            int quads = stb_easy_font_print(0, 0, text, null, charBuffer);
            glDrawArrays(GL_QUADS, 0, quads * 4);
        }
        glDisableClientState(GL_VERTEX_ARRAY);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
    }

    void renderMouseCursorCoordinates() {
        glEnableClientState(GL_VERTEX_ARRAY);
        glVertexPointer(2, GL_FLOAT, 16, charBuffer);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        cam.invviewproj().unprojectInv(mouseX, height - mouseY, 0, viewport, v);
        String str = frmt.format(v.x) + "\n" + frmt.format(v.y);
        float ndcX = (mouseX-viewport[0])/viewport[2]*2.0f-1.0f;
        float ndcY = (viewport[3]-mouseY-viewport[1])/viewport[3]*2.0f-1.0f;
        glTranslatef(ndcX, ndcY, 0);
        int quads = stb_easy_font_print(0, 0, str, null, charBuffer);
        glScalef(textScale / width, -textScale / height, 0.0f);
        glTranslatef(5, -15, 0);
        glColor3f(0.3f, 0.3f, 0.3f);
        glDrawArrays(GL_QUADS, 0, quads * 4);
        glDisableClientState(GL_VERTEX_ARRAY);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
    }

    private void computeVisibleExtents() {
        cam.viewRect(rect);
        minX = rect.x;
        minY = rect.y;
        maxX = rect.z;
        maxY = rect.w;
    }

    void loop() {
        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        glClearColor(0.97f, 0.97f, 0.97f, 1.0f);
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            glViewport(0, 0, fbWidth, fbHeight);
            viewport[2] = fbWidth; viewport[3] = fbHeight;
            glClear(GL_COLOR_BUFFER_BIT);
            computeVisibleExtents();
            glMatrixMode(GL_PROJECTION);
            glLoadMatrixf(cam.viewproj().get(fb));
            renderGrid();
            renderTickLabels();
            //renderMouseCursorCoordinates();
            glfwSwapBuffers(window);
        }
    }

    public static void main(String[] args) {
        new CoordinateSystemDemo().run();
    }
}
