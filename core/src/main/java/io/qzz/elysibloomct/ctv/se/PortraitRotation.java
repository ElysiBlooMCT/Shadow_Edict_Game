package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

/**
 * Giữ game luôn nằm ngang khi màn hình đứng dọc (bản web trên điện thoại khóa xoay).
 *
 * Khi cửa sổ cao hơn rộng: cả game được vẽ vào một khung ngang (FrameBuffer) rồi xoay 90° cho vừa màn hình,
 * đồng thời mọi chạm/chuột được xoay ngược lại. Các màn hình không cần biết gì: trong lúc game chạy,
 * Gdx.graphics và Gdx.input được thay tạm bằng bản "ngang" (kích thước và tọa độ đã đổi chiều).
 * Màn hình nằm ngang thì không làm gì cả.
 *
 * Chỉ bật cho bản web ({@link #enabled} = true trong WebLauncher). Thử trên PC: -Dse.rotate=true.
 */
public final class PortraitRotation {
    public static boolean enabled = Boolean.parseBoolean(System.getProperty("se.rotate", "false"));

    private static Graphics realGraphics;
    private static Input realInput;
    private static RotatedGraphics rotGraphics;
    private static RotatedInput rotInput;
    private static boolean active;
    private static int swaps;

    private static FrameBuffer fbo;
    private static TextureRegion fboRegion;
    private static SpriteBatch batch;

    private PortraitRotation() {}

    /** Màn hình hiện tại có đang bị xoay không. */
    public static boolean isActive() {
        return active && fbo != null;
    }

    /** Gọi trong Main.resize với kích thước thật; trả về true nếu cần xoay. */
    static boolean update(int realWidth, int realHeight) {
        captureReal();
        active = enabled && realHeight > realWidth && realWidth > 0;
        if (active) ensureFbo();
        return active;
    }

    /** Kích thước "ngang" mà các màn hình nhìn thấy. */
    static int landscapeWidth() {
        return realGraphics.getHeight();
    }

    static int landscapeHeight() {
        return realGraphics.getWidth();
    }

    private static void captureReal() {
        if (swaps > 0) return;
        if (Gdx.graphics != rotGraphics) realGraphics = Gdx.graphics;
        if (Gdx.input != rotInput) realInput = Gdx.input;
        if (rotGraphics == null || rotGraphics.real != realGraphics) rotGraphics = new RotatedGraphics(realGraphics);
        if (rotInput == null || rotInput.real != realInput) rotInput = new RotatedInput(realInput);
    }

    /** Đặt Gdx.graphics / Gdx.input thành bản ngang (lồng nhau được). */
    static void begin() {
        if (!active) return;
        if (swaps++ == 0) {
            Gdx.graphics = rotGraphics;
            Gdx.input = rotInput;
        }
    }

    static void end() {
        if (!active || swaps == 0) return;
        if (--swaps == 0) {
            Gdx.graphics = realGraphics;
            Gdx.input = realInput;
        }
    }

    /** Đảm bảo bộ xử lý chạm thật luôn đi qua lớp xoay (kể cả khi màn hình đặt processor lúc chưa xoay). */
    static void guardInputProcessor() {
        if (!active) return;
        InputProcessor cur = realInput.getInputProcessor();
        if (!(cur instanceof RotatingProcessor)) {
            rotInput.target = cur;
            realInput.setInputProcessor(new RotatingProcessor(cur));
        }
    }

    /** Khi trở lại màn hình ngang: trả processor gốc cho input thật. */
    static void releaseInputProcessor() {
        if (realInput == null) return;
        InputProcessor cur = realInput.getInputProcessor();
        if (cur instanceof RotatingProcessor) realInput.setInputProcessor(((RotatingProcessor) cur).target);
    }

    private static void ensureFbo() {
        int w = realGraphics.getBackBufferHeight();
        int h = realGraphics.getBackBufferWidth();
        if (w <= 0 || h <= 0) return;
        if (fbo != null && fbo.getWidth() == w && fbo.getHeight() == h) return;
        if (fbo != null) fbo.dispose();
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
        fbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        fboRegion = new TextureRegion(fbo.getColorBufferTexture());
        fboRegion.flip(false, true);
        if (batch == null) batch = new SpriteBatch();
    }

    /** Bắt đầu vẽ khung ngang. */
    static void beginFrame() {
        fbo.begin();
    }

    /** Kết thúc khung ngang và vẽ nó xoay 90° lên màn hình thật. */
    static void endFrame() {
        fbo.end();
        int bw = realGraphics.getBackBufferWidth();
        int bh = realGraphics.getBackBufferHeight();
        Gdx.gl.glViewport(0, 0, bw, bh);
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        float sw = realGraphics.getWidth();
        float sh = realGraphics.getHeight();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, sw, sh);
        batch.begin();
        batch.disableBlending();
        // Ảnh ngang rộng sh, cao sw; xoay -90° (theo chiều kim đồng hồ) quanh tâm màn hình.
        batch.draw(fboRegion, sw / 2f - sh / 2f, sh / 2f - sw / 2f, sh / 2f, sw / 2f, sh, sw, 1f, 1f, -90f);
        batch.enableBlending();
        batch.end();
    }

    public static void dispose() {
        if (fbo != null) fbo.dispose();
        if (batch != null) batch.dispose();
        fbo = null;
        batch = null;
    }

    // ------------------------------------------------------------------ đổi tọa độ
    // Màn hình dọc rộng W (tọa độ chạm, y tính từ trên xuống). Ảnh ngang được xoay theo chiều kim đồng hồ:
    // điểm (lx, ly) trên ảnh ngang nằm ở (W - ly, lx) trên màn hình -> ngược lại lx = py, ly = W - px.

    static int toLandscapeX(int px, int py) {
        return py;
    }

    static int toLandscapeY(int px, int py) {
        return realGraphics.getWidth() - 1 - px;
    }

    /** Chuyển sự kiện chạm/chuột sang tọa độ ngang rồi giao cho processor của màn hình. */
    static final class RotatingProcessor implements InputProcessor {
        final InputProcessor target;

        RotatingProcessor(InputProcessor target) {
            this.target = target;
        }

        @Override public boolean keyDown(int keycode) { begin(); try { return target != null && target.keyDown(keycode); } finally { end(); } }
        @Override public boolean keyUp(int keycode) { begin(); try { return target != null && target.keyUp(keycode); } finally { end(); } }
        @Override public boolean keyTyped(char c) { begin(); try { return target != null && target.keyTyped(c); } finally { end(); } }
        @Override public boolean touchDown(int x, int y, int p, int b) { begin(); try { return target != null && target.touchDown(toLandscapeX(x, y), toLandscapeY(x, y), p, b); } finally { end(); } }
        @Override public boolean touchUp(int x, int y, int p, int b) { begin(); try { return target != null && target.touchUp(toLandscapeX(x, y), toLandscapeY(x, y), p, b); } finally { end(); } }
        @Override public boolean touchCancelled(int x, int y, int p, int b) { begin(); try { return target != null && target.touchCancelled(toLandscapeX(x, y), toLandscapeY(x, y), p, b); } finally { end(); } }
        @Override public boolean touchDragged(int x, int y, int p) { begin(); try { return target != null && target.touchDragged(toLandscapeX(x, y), toLandscapeY(x, y), p); } finally { end(); } }
        @Override public boolean mouseMoved(int x, int y) { begin(); try { return target != null && target.mouseMoved(toLandscapeX(x, y), toLandscapeY(x, y)); } finally { end(); } }
        @Override public boolean scrolled(float ax, float ay) { begin(); try { return target != null && target.scrolled(ax, ay); } finally { end(); } }
    }

    /** Gdx.graphics phiên bản ngang: đổi chiều rộng/cao, phần còn lại giữ nguyên. */
    static final class RotatedGraphics implements Graphics {
        final Graphics real;

        RotatedGraphics(Graphics real) {
            this.real = real;
        }

        @Override public int getWidth() { return real.getHeight(); }
        @Override public int getHeight() { return real.getWidth(); }
        @Override public int getBackBufferWidth() { return real.getBackBufferHeight(); }
        @Override public int getBackBufferHeight() { return real.getBackBufferWidth(); }
    @Override public float getBackBufferScale() { return real.getBackBufferScale(); }
    @Override public com.badlogic.gdx.Graphics.BufferFormat getBufferFormat() { return real.getBufferFormat(); }
    @Override public float getDeltaTime() { return real.getDeltaTime(); }
    @Override public float getDensity() { return real.getDensity(); }
    @Override public com.badlogic.gdx.Graphics.DisplayMode getDisplayMode() { return real.getDisplayMode(); }
    @Override public com.badlogic.gdx.Graphics.DisplayMode getDisplayMode(com.badlogic.gdx.Graphics.Monitor a0) { return real.getDisplayMode(a0); }
    @Override public com.badlogic.gdx.Graphics.DisplayMode[] getDisplayModes() { return real.getDisplayModes(); }
    @Override public com.badlogic.gdx.Graphics.DisplayMode[] getDisplayModes(com.badlogic.gdx.Graphics.Monitor a0) { return real.getDisplayModes(a0); }
    @Override public long getFrameId() { return real.getFrameId(); }
    @Override public int getFramesPerSecond() { return real.getFramesPerSecond(); }
    @Override public com.badlogic.gdx.graphics.GL20 getGL20() { return real.getGL20(); }
    @Override public com.badlogic.gdx.graphics.GL30 getGL30() { return real.getGL30(); }
    @Override public com.badlogic.gdx.graphics.GL31 getGL31() { return real.getGL31(); }
    @Override public com.badlogic.gdx.graphics.GL32 getGL32() { return real.getGL32(); }
    @Override public com.badlogic.gdx.graphics.glutils.GLVersion getGLVersion() { return real.getGLVersion(); }
    @Override public com.badlogic.gdx.Graphics.Monitor getMonitor() { return real.getMonitor(); }
    @Override public com.badlogic.gdx.Graphics.Monitor[] getMonitors() { return real.getMonitors(); }
    @Override public float getPpcX() { return real.getPpcX(); }
    @Override public float getPpcY() { return real.getPpcY(); }
    @Override public float getPpiX() { return real.getPpiX(); }
    @Override public float getPpiY() { return real.getPpiY(); }
    @Override public com.badlogic.gdx.Graphics.Monitor getPrimaryMonitor() { return real.getPrimaryMonitor(); }
    @Override public float getRawDeltaTime() { return real.getRawDeltaTime(); }
    @Override public int getSafeInsetBottom() { return real.getSafeInsetBottom(); }
    @Override public int getSafeInsetLeft() { return real.getSafeInsetLeft(); }
    @Override public int getSafeInsetRight() { return real.getSafeInsetRight(); }
    @Override public int getSafeInsetTop() { return real.getSafeInsetTop(); }
    @Override public com.badlogic.gdx.Graphics.GraphicsType getType() { return real.getType(); }
    @Override public boolean isContinuousRendering() { return real.isContinuousRendering(); }
    @Override public boolean isFullscreen() { return real.isFullscreen(); }
    @Override public boolean isGL30Available() { return real.isGL30Available(); }
    @Override public boolean isGL31Available() { return real.isGL31Available(); }
    @Override public boolean isGL32Available() { return real.isGL32Available(); }
    @Override public com.badlogic.gdx.graphics.Cursor newCursor(com.badlogic.gdx.graphics.Pixmap a0, int a1, int a2) { return real.newCursor(a0, a1, a2); }
    @Override public void requestRendering() { real.requestRendering(); }
    @Override public void setContinuousRendering(boolean a0) { real.setContinuousRendering(a0); }
    @Override public void setCursor(com.badlogic.gdx.graphics.Cursor a0) { real.setCursor(a0); }
    @Override public void setForegroundFPS(int a0) { real.setForegroundFPS(a0); }
    @Override public boolean setFullscreenMode(com.badlogic.gdx.Graphics.DisplayMode a0) { return real.setFullscreenMode(a0); }
    @Override public void setGL20(com.badlogic.gdx.graphics.GL20 a0) { real.setGL20(a0); }
    @Override public void setGL30(com.badlogic.gdx.graphics.GL30 a0) { real.setGL30(a0); }
    @Override public void setGL31(com.badlogic.gdx.graphics.GL31 a0) { real.setGL31(a0); }
    @Override public void setGL32(com.badlogic.gdx.graphics.GL32 a0) { real.setGL32(a0); }
    @Override public void setResizable(boolean a0) { real.setResizable(a0); }
    @Override public void setSystemCursor(com.badlogic.gdx.graphics.Cursor.SystemCursor a0) { real.setSystemCursor(a0); }
    @Override public void setTitle(java.lang.String a0) { real.setTitle(a0); }
    @Override public void setUndecorated(boolean a0) { real.setUndecorated(a0); }
    @Override public void setVSync(boolean a0) { real.setVSync(a0); }
    @Override public boolean setWindowedMode(int a0, int a1) { return real.setWindowedMode(a0, a1); }
    @Override public boolean supportsDisplayModeChange() { return real.supportsDisplayModeChange(); }
    @Override public boolean supportsExtension(java.lang.String a0) { return real.supportsExtension(a0); }
    }

    /** Gdx.input phiên bản ngang: tọa độ chạm đã xoay, processor của màn hình được bọc lớp xoay. */
    static final class RotatedInput implements Input {
        final Input real;
        InputProcessor target;

        RotatedInput(Input real) {
            this.real = real;
        }

        @Override public int getX() { return toLandscapeX(real.getX(), real.getY()); }
        @Override public int getY() { return toLandscapeY(real.getX(), real.getY()); }
        @Override public int getX(int p) { return toLandscapeX(real.getX(p), real.getY(p)); }
        @Override public int getY(int p) { return toLandscapeY(real.getX(p), real.getY(p)); }
        @Override public int getDeltaX() { return real.getDeltaY(); }
        @Override public int getDeltaY() { return -real.getDeltaX(); }
        @Override public int getDeltaX(int p) { return real.getDeltaY(p); }
        @Override public int getDeltaY(int p) { return -real.getDeltaX(p); }
        @Override public InputProcessor getInputProcessor() { return target; }
        @Override public void setInputProcessor(InputProcessor processor) {
            target = processor;
            real.setInputProcessor(new RotatingProcessor(processor));
        }
    @Override public void closeTextInputField(boolean a0) { real.closeTextInputField(a0); }
    @Override public void closeTextInputField(boolean a0, com.badlogic.gdx.input.NativeInputConfiguration.NativeInputCloseCallback a1) { real.closeTextInputField(a0, a1); }
    @Override public float getAccelerometerX() { return real.getAccelerometerX(); }
    @Override public float getAccelerometerY() { return real.getAccelerometerY(); }
    @Override public float getAccelerometerZ() { return real.getAccelerometerZ(); }
    @Override public float getAzimuth() { return real.getAzimuth(); }
    @Override public long getCurrentEventTime() { return real.getCurrentEventTime(); }
    @Override public float getGyroscopeX() { return real.getGyroscopeX(); }
    @Override public float getGyroscopeY() { return real.getGyroscopeY(); }
    @Override public float getGyroscopeZ() { return real.getGyroscopeZ(); }
    @Override public int getMaxPointers() { return real.getMaxPointers(); }
    @Override public com.badlogic.gdx.Input.Orientation getNativeOrientation() { return real.getNativeOrientation(); }
    @Override public float getPitch() { return real.getPitch(); }
    @Override public float getPressure() { return real.getPressure(); }
    @Override public float getPressure(int a0) { return real.getPressure(a0); }
    @Override public float getRoll() { return real.getRoll(); }
    @Override public int getRotation() { return real.getRotation(); }
    @Override public void getRotationMatrix(float[] a0) { real.getRotationMatrix(a0); }
    @Override public void getTextInput(com.badlogic.gdx.Input.TextInputListener a0, java.lang.String a1, java.lang.String a2, java.lang.String a3) { real.getTextInput(a0, a1, a2, a3); }
    @Override public void getTextInput(com.badlogic.gdx.Input.TextInputListener a0, java.lang.String a1, java.lang.String a2, java.lang.String a3, com.badlogic.gdx.Input.OnscreenKeyboardType a4) { real.getTextInput(a0, a1, a2, a3, a4); }
    @Override public boolean isButtonJustPressed(int a0) { return real.isButtonJustPressed(a0); }
    @Override public boolean isButtonPressed(int a0) { return real.isButtonPressed(a0); }
    @Override public boolean isCatchKey(int a0) { return real.isCatchKey(a0); }
    @Override public boolean isCursorCatched() { return real.isCursorCatched(); }
    @Override public boolean isKeyJustPressed(int a0) { return real.isKeyJustPressed(a0); }
    @Override public boolean isKeyPressed(int a0) { return real.isKeyPressed(a0); }
    @Override public boolean isPeripheralAvailable(com.badlogic.gdx.Input.Peripheral a0) { return real.isPeripheralAvailable(a0); }
    @Override public boolean isTextInputFieldOpened() { return real.isTextInputFieldOpened(); }
    @Override public boolean isTouched() { return real.isTouched(); }
    @Override public boolean isTouched(int a0) { return real.isTouched(a0); }
    @Override public boolean justTouched() { return real.justTouched(); }
    @Override public void openTextInputField(com.badlogic.gdx.input.NativeInputConfiguration a0) { real.openTextInputField(a0); }
    @Override public void setCatchKey(int a0, boolean a1) { real.setCatchKey(a0, a1); }
    @Override public void setCursorCatched(boolean a0) { real.setCursorCatched(a0); }
    @Override public void setCursorPosition(int a0, int a1) { real.setCursorPosition(a0, a1); }
    @Override public void setKeyboardHeightObserver(com.badlogic.gdx.Input.KeyboardHeightObserver a0) { real.setKeyboardHeightObserver(a0); }
    @Override public void setOnscreenKeyboardVisible(boolean a0) { real.setOnscreenKeyboardVisible(a0); }
    @Override public void setOnscreenKeyboardVisible(boolean a0, com.badlogic.gdx.Input.OnscreenKeyboardType a1) { real.setOnscreenKeyboardVisible(a0, a1); }
    @Override public void vibrate(com.badlogic.gdx.Input.VibrationType a0) { real.vibrate(a0); }
    @Override public void vibrate(int a0) { real.vibrate(a0); }
    @Override public void vibrate(int a0, boolean a1) { real.vibrate(a0, a1); }
    @Override public void vibrate(int a0, int a1, boolean a2) { real.vibrate(a0, a1, a2); }
    }
}
