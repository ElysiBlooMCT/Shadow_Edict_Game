package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/**
 * Bộ giao diện dùng chung: font có dấu, màu chủ đạo, nút & khung VUÔNG (cùng kiểu menu chính), overlay.
 * Tài nguyên tạo 1 lần và dùng lại cho mọi màn hình (giải phóng ở Main.dispose()).
 */
public final class UiKit {
    // --- Bảng màu chủ đạo (dark fantasy: than chì + vàng kim) ---
    public static final Color GOLD = new Color(0.90f, 0.74f, 0.36f, 1f);
    public static final Color GOLD_DIM = new Color(0.62f, 0.50f, 0.26f, 1f);
    public static final Color TEXT = new Color(0.94f, 0.92f, 0.88f, 1f);
    public static final Color MUTED = new Color(0.66f, 0.66f, 0.72f, 1f);
    public static final Color PANEL = new Color(0.07f, 0.08f, 0.11f, 0.94f);
    public static final Color PANEL_LIGHT = new Color(0.12f, 0.14f, 0.19f, 0.95f);
    public static final Color GREEN = new Color(0.16f, 0.52f, 0.30f, 1f);
    public static final Color BLUE = new Color(0.16f, 0.38f, 0.62f, 1f);
    public static final Color RED = new Color(0.62f, 0.16f, 0.18f, 1f);
    public static final Color ORANGE = new Color(0.74f, 0.44f, 0.12f, 1f);
    public static final Color PURPLE = new Color(0.40f, 0.24f, 0.58f, 1f);
    public static final Color GRAY = new Color(0.22f, 0.23f, 0.28f, 1f);
    public static final Color SUCCESS = new Color(0.45f, 0.92f, 0.52f, 1f);
    public static final Color WARNING = new Color(1.00f, 0.80f, 0.30f, 1f);
    public static final Color DANGER = new Color(1.00f, 0.42f, 0.40f, 1f);

    /** Màu nhấn khi chọn/rê chuột – trùng với menu chính. */
    public static final Color ACCENT = new Color(1.00f, 0.34f, 0.36f, 1f);

    private static BitmapFont fontSmall, fontNormal, fontLarge, fontTitle, fontSmallBold, fontNormalBold;
    private static Texture whiteTex, haloGlowTex, haloRingTex;
    private static final com.badlogic.gdx.utils.Array<Pixmap> PIXMAPS = new com.badlogic.gdx.utils.Array<>();
    private static TextureRegion whiteRegion, haloGlowRegion, haloRingRegion;

    private UiKit() {}

    // ================== FONT ==================
    public static BitmapFont small() { ensure(); return fontSmall; }
    public static BitmapFont normal() { ensure(); return fontNormal; }
    public static BitmapFont large() { ensure(); return fontLarge; }
    public static BitmapFont title() { ensure(); return fontTitle; }
    /** Bản đậm của small/normal – dùng cho chữ trên nút. */
    public static BitmapFont smallBold() { ensure(); return fontSmallBold; }
    public static BitmapFont normalBold() { ensure(); return fontNormalBold; }

    // Font có dấu tiếng Việt (assets/ui/*.fnt, sinh từ DejaVu Sans Condensed).
    // Thiếu file -> tự quay về font mặc định để game không crash.
    private static BitmapFont fontDisplay, fontBody, fontBodyBold, fontButton, fontInput;
    /** Font 20px có dấu cho ô nhập chữ (TextField không tự scale font được). */
    public static BitmapFont inputFont() { if (fontInput == null) fontInput = loadFont("ui/font_input.fnt", 1.15f); return fontInput; }
    public static BitmapFont buttonFont() { if (fontButton == null) fontButton = loadFont("ui/font_button.fnt", 1.3f); return fontButton; }
    public static BitmapFont display() { if (fontDisplay == null) fontDisplay = loadFont("ui/font_display.fnt", 2.6f); return fontDisplay; }
    public static BitmapFont body() { if (fontBody == null) fontBody = loadFont("ui/font_body.fnt", 1.15f); return fontBody; }
    public static BitmapFont bodyBold() { if (fontBodyBold == null) fontBodyBold = loadFont("ui/font_body_bold.fnt", 1.15f); return fontBodyBold; }

    /** Nạp 1 bản RIÊNG của font (mỗi bản có tỉ lệ riêng, không ảnh hưởng nhau). */
    private static BitmapFont scaled(String path, float scale, float fallbackScale) {
        return loadFont(path, scale, fallbackScale);
    }

    private static BitmapFont loadFont(String path, float fallbackScale) {
        return loadFont(path, 1f, fallbackScale);
    }

    private static BitmapFont loadFont(String path, float loadedScale, float fallbackScale) {
        try {
            com.badlogic.gdx.files.FileHandle fh = com.badlogic.gdx.Gdx.files.internal(path);
            if (fh.exists()) {
                BitmapFont f = new BitmapFont(fh);
                if (loadedScale != 1f) f.getData().setScale(loadedScale);
                f.setUseIntegerPositions(false);
                for (TextureRegion r : f.getRegions()) {
                    r.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                }
                return f;
            }
        } catch (Exception e) {
            com.badlogic.gdx.Gdx.app.error("UiKit", "Khong nap duoc font " + path, e);
        }
        return makeFont(fallbackScale);
    }

    private static BitmapFont makeFont(float scale) {
        BitmapFont f = new BitmapFont();
        f.getData().setScale(scale);
        f.setUseIntegerPositions(false);
        // Font mặc định dùng filter Nearest -> phóng to bị vỡ hạt. Linear cho chữ mượt hơn.
        for (TextureRegion r : f.getRegions()) {
            r.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
        return f;
    }

    // ================== TEXTURE NỀN ==================
    private static Texture managedTexture(Pixmap pm) {
        // managed = true: tự khôi phục khi Android mất GL context (giữ pixmap nhỏ để nạp lại)
        PIXMAPS.add(pm);
        Texture t = new Texture(new PixmapTextureData(pm, null, false, false, true));
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    private static void ensure() {
        if (fontNormal != null) return;
        // Cỡ chữ tương đương font cũ (Arial 15px x 0.95/1.15/1.45/2.4) nhưng CÓ DẤU tiếng Việt
        fontSmall = scaled("ui/font_body.fnt", 0.52f, 0.95f);
        fontNormal = scaled("ui/font_body.fnt", 0.62f, 1.15f);
        fontSmallBold = scaled("ui/font_body_bold.fnt", 0.52f, 0.95f);
        fontNormalBold = scaled("ui/font_body_bold.fnt", 0.62f, 1.15f);
        fontLarge = scaled("ui/font_body_bold.fnt", 0.80f, 1.45f);
        fontTitle = scaled("ui/font_display.fnt", 0.56f, 2.4f);

        Pixmap w = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
        w.setColor(Color.WHITE);
        w.fill();
        whiteTex = managedTexture(w);
        whiteRegion = new TextureRegion(whiteTex, 1, 1, 2, 2);
    }

    // ================== DRAWABLE ==================
    /** Ô màu phẳng (không bo góc). */
    public static Drawable solid(Color c) {
        ensure();
        return new TextureRegionDrawable(whiteRegion).tint(c);
    }

    /** Nền phẳng (giữ tên cũ cho tương thích – nay là góc vuông như menu chính). */
    public static Drawable rounded(Color c) {
        return solid(c);
    }

    /** Khung vuông: nền + viền 1px. */
    public static Drawable framed(Color fill, Color border) {
        ensure();
        return new BoxDrawable(fill, border, null, 0f, false);
    }

    public static Drawable panel() {
        return framed(PANEL, GOLD_DIM);
    }

    public static TextureRegion whiteRegion() {
        ensure();
        return whiteRegion;
    }

    /**
     * Khung vuông kiểu menu chính: nền, viền 1px, vạch nhấn bên trái (tùy chọn), bóng đổ dưới chân (tùy chọn).
     */
    public static final class BoxDrawable extends BaseDrawable {
        private final Color fill = new Color(), border = new Color(), accent = new Color();
        private final boolean hasAccent, shadow;
        private final float accentW;

        BoxDrawable(Color fill, Color border, Color accent, float accentW, boolean shadow) {
            this.fill.set(fill);
            this.border.set(border);
            this.hasAccent = accent != null && accentW > 0f;
            if (hasAccent) this.accent.set(accent);
            this.accentW = accentW;
            this.shadow = shadow;
            setLeftWidth(10); setRightWidth(10); setTopHeight(6); setBottomHeight(6);
            setMinWidth(16); setMinHeight(16);
        }

        @Override
        public void draw(Batch batch, float x, float y, float width, float height) {
            Color old = batch.getColor();
            float or = old.r, og = old.g, ob = old.b, oa = old.a;
            if (shadow) {
                batch.setColor(0f, 0f, 0f, 0.45f * oa);
                batch.draw(whiteRegion, x + 2, y - 4, width, 4);
            }
            batch.setColor(fill.r * or, fill.g * og, fill.b * ob, fill.a * oa);
            batch.draw(whiteRegion, x, y, width, height);
            if (shadow) { // vệt sáng mép trên giống nút menu chính
                batch.setColor(1f, 1f, 1f, 0.07f * oa);
                batch.draw(whiteRegion, x, y + height - 3, width, 2);
            }
            batch.setColor(border.r * or, border.g * og, border.b * ob, border.a * oa);
            batch.draw(whiteRegion, x, y, width, 1);
            batch.draw(whiteRegion, x, y + height - 1, width, 1);
            batch.draw(whiteRegion, x, y, 1, height);
            batch.draw(whiteRegion, x + width - 1, y, 1, height);
            if (hasAccent) {
                batch.setColor(accent.r * or, accent.g * og, accent.b * ob, accent.a * oa);
                batch.draw(whiteRegion, x, y, accentW, height);
            }
            batch.setColor(or, og, ob, oa);
        }
    }

    // ================== WIDGET ==================
    public static Color lighter(Color c, float amt) {
        return new Color(Math.min(1f, c.r + amt), Math.min(1f, c.g + amt), Math.min(1f, c.b + amt), 1f);
    }

    public static Color darker(Color c, float amt) {
        return new Color(Math.max(0f, c.r - amt), Math.max(0f, c.g - amt), Math.max(0f, c.b - amt), 1f);
    }

    /**
     * Nút vuông cùng kiểu menu chính. Màu gốc chỉ nhuộm nhẹ nền tối để vẫn phân biệt được
     * (xanh = bắt đầu/đồng ý, đỏ = thoát/nguy hiểm, xám = phụ); rê chuột/được chọn -> viền & vạch nhấn sáng lên.
     */
    /** Thu nhỏ chữ trên nút cho vừa bề rộng nút (chữ dài như "LUYỆN TẬP MỘT MÌNH (Enter)" không tràn ra ngoài). */
    public static void fitLabel(TextButton b, float buttonWidth) {
        if (b == null) return;
        Label l = b.getLabel();
        l.setFontScale(1f);
        float pw = l.getPrefWidth(), avail = buttonWidth - 20f;
        if (pw > avail && pw > 0f) l.setFontScale(avail / pw);
    }

    public static TextButton.TextButtonStyle buttonStyle(Color base, BitmapFont font) {
        ensure();
        if (font == fontSmall) font = fontSmallBold;     // chữ trên nút luôn đậm
        else if (font == fontNormal) font = fontNormalBold;
        Color dark = new Color(0.07f, 0.08f, 0.11f, 1f);
        Color fillUp = new Color(dark).lerp(base, 0.45f); fillUp.a = 0.90f;
        Color fillOver = new Color(dark).lerp(base, 0.70f); fillOver.a = 0.95f;
        Color hi = lighter(base, 0.35f);
        TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
        s.up = new BoxDrawable(fillUp, new Color(1f, 1f, 1f, 0.22f), null, 0f, true);
        s.over = new BoxDrawable(fillOver, hi, hi, 4f, true);
        s.down = new BoxDrawable(darker(fillOver, 0.06f), hi, hi, 4f, false);
        s.checked = new BoxDrawable(fillOver, hi, hi, 4f, true);
        s.disabled = new BoxDrawable(new Color(0.12f, 0.12f, 0.15f, 0.85f), new Color(1f, 1f, 1f, 0.10f), null, 0f, false);
        s.font = font;
        s.fontColor = TEXT;
        s.overFontColor = Color.WHITE;
        s.downFontColor = Color.WHITE;
        s.checkedFontColor = Color.WHITE;
        s.disabledFontColor = new Color(0.50f, 0.50f, 0.54f, 1f);
        s.pressedOffsetY = -2f;
        return s;
    }

    public static TextButton button(String text, Color base) {
        return new TextButton(text, buttonStyle(base, normal()));
    }

    public static TextButton button(String text, Color base, BitmapFont font) {
        return new TextButton(text, buttonStyle(base, font));
    }

    public static Label label(String text, BitmapFont font, Color color) {
        return new Label(text, new Label.LabelStyle(font, color));
    }

    public static TextField.TextFieldStyle textFieldStyle() {
        TextField.TextFieldStyle s = new TextField.TextFieldStyle();
        s.font = normal();
        s.fontColor = TEXT;
        s.messageFont = normal();
        s.messageFontColor = new Color(0.55f, 0.56f, 0.62f, 1f);
        Drawable bg = framed(new Color(0.05f, 0.06f, 0.09f, 0.95f), GOLD_DIM);
        s.background = bg;
        s.focusedBackground = framed(new Color(0.07f, 0.08f, 0.12f, 0.98f), GOLD);
        s.cursor = solid(GOLD);
        s.cursor.setMinWidth(2f);
        s.selection = solid(new Color(0.85f, 0.70f, 0.30f, 0.35f));
        return s;
    }

    public static ScrollPane.ScrollPaneStyle scrollStyle() {
        ScrollPane.ScrollPaneStyle s = new ScrollPane.ScrollPaneStyle();
        s.background = rounded(new Color(0.04f, 0.05f, 0.08f, 0.80f));
        s.vScrollKnob = rounded(new Color(0.85f, 0.70f, 0.32f, 0.65f));
        s.vScrollKnob.setMinWidth(6f);
        return s;
    }

    /**
     * Lớp phủ tối toàn màn hình CHẶN click xuyên xuống các nút bên dưới
     * (Table mặc định là childrenOnly nên trước đây bấm xuyên được).
     */
    public static Image dimOverlay(float alpha) {
        Image img = new Image(solid(new Color(0f, 0f, 0f, alpha)));
        img.setFillParent(true);
        img.setTouchable(Touchable.enabled);
        img.addListener(new InputListener() {
            @Override public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                event.stop();
                return true;
            }
        });
        return img;
    }

    /** Phát âm thanh click thống nhất cho mọi nút. */
    public static void onClick(com.badlogic.gdx.scenes.scene2d.Actor actor, Runnable action) {
        actor.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (actor instanceof TextButton && ((TextButton) actor).isDisabled()) return;
                SoundManager.getInstance().playSlash();
                action.run();
            }
        });
    }

    private static void ensureHaloTextures() {
        ensure();
        if (haloRingRegion != null && haloGlowRegion != null) return;
        int sz = 256;
        float half = sz * 0.5f;

        Pixmap glowPm = new Pixmap(sz, sz, Pixmap.Format.RGBA8888);
        Pixmap ringPm = new Pixmap(sz, sz, Pixmap.Format.RGBA8888);
        for (int y = 0; y < sz; y++) {
            for (int x = 0; x < sz; x++) {
                float dx = (x + 0.5f - half) / half;
                float dy = (y + 0.5f - half) / half;
                float r = (float) Math.sqrt(dx * dx + dy * dy);
                if (r <= 1.0f) {
                    float ga = (float) Math.pow(1.0f - r, 2.1f) * 0.85f;
                    glowPm.setColor(1f, 1f, 1f, ga);
                    glowPm.drawPixel(x, y);

                    float ang = MathUtils.atan2(dy, dx);
                    float ra = 0f;
                    // Vòng sáng chính phía ngoài + hào quang mờ
                    float dOuter = Math.abs(r - 0.89f);
                    if (dOuter < 0.085f) {
                        ra = Math.max(ra, (float) Math.pow(1.0f - dOuter / 0.085f, 1.8f));
                    }
                    // Vòng cổ ngữ phía trong
                    float dInner = Math.abs(r - 0.76f);
                    if (dInner < 0.035f) {
                        ra = Math.max(ra, (1.0f - dInner / 0.035f) * 0.80f);
                    }
                    // 8 mũi kiếm/hoa văn phát sáng quanh vòng
                    float star8 = (float) Math.pow(Math.max(0f, MathUtils.cos(ang * 8f)), 14f);
                    if (r >= 0.74f && r <= 0.98f) {
                        float radialFade = 1.0f - Math.abs(r - 0.88f) / 0.14f;
                        if (radialFade > 0f) ra = Math.max(ra, star8 * radialFade * 0.95f);
                    }
                    // 24 vạch khắc mảnh giữa 2 vòng
                    float ticks = (float) Math.pow(Math.max(0f, MathUtils.cos(ang * 24f)), 26f);
                    if (r >= 0.78f && r <= 0.86f) {
                        ra = Math.max(ra, ticks * 0.65f);
                    }
                    if (ra > 0.005f) {
                        ringPm.setColor(1f, 1f, 1f, MathUtils.clamp(ra, 0f, 1f));
                        ringPm.drawPixel(x, y);
                    }
                }
            }
        }
        haloGlowTex = managedTexture(glowPm);
        haloRingTex = managedTexture(ringPm);
        haloGlowRegion = new TextureRegion(haloGlowTex);
        haloRingRegion = new TextureRegion(haloRingTex);
    }

    /** Vòng sáng xoay quanh nhân vật đang được chọn ở màn hình chọn tướng. */
    public static final class HeroHalo extends Actor {
        private final Color primaryColor = new Color();
        private float time = 0f;
        private final float scaleMul;

        public HeroHalo(Color primaryColor) {
            this(primaryColor, 1.48f);
        }

        public HeroHalo(Color primaryColor, float scaleMul) {
            this.primaryColor.set(primaryColor);
            this.scaleMul = scaleMul;
            setTouchable(Touchable.disabled);
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            time += delta;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (!isVisible()) return;
            ensureHaloTextures();
            float cx = getX() + getWidth() * 0.5f;
            float cy = getY() + getHeight() * 0.5f;
            float baseSize = Math.max(getWidth(), getHeight()) * scaleMul;
            float pulse = 1.0f + 0.045f * MathUtils.sin(time * 3.8f);

            Color old = batch.getColor();
            float or = old.r, og = old.g, ob = old.b, oa = old.a;
            int srcFunc = batch.getBlendSrcFunc();
            int dstFunc = batch.getBlendDstFunc();
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

            // Hào quang nền tỏa rộng
            float glowSz = baseSize * 1.15f * pulse;
            batch.setColor(primaryColor.r, primaryColor.g, primaryColor.b, 0.52f * parentAlpha);
            batch.draw(haloGlowRegion, cx - glowSz * 0.5f, cy - glowSz * 0.5f, glowSz, glowSz);

            // Vòng pháp trận ngoài xoay thuận
            float outerSz = baseSize * pulse;
            batch.setColor(primaryColor.r, primaryColor.g, primaryColor.b, 0.92f * parentAlpha);
            batch.draw(haloRingRegion, cx - outerSz * 0.5f, cy - outerSz * 0.5f,
                outerSz * 0.5f, outerSz * 0.5f, outerSz, outerSz, 1f, 1f, time * 28f);

            // Vòng hoàng kim trong xoay ngược
            float innerSz = baseSize * 0.90f * (2.0f - pulse);
            batch.setColor(1.0f, 0.86f, 0.42f, 0.82f * parentAlpha);
            batch.draw(haloRingRegion, cx - innerSz * 0.5f, cy - innerSz * 0.5f,
                innerSz * 0.5f, innerSz * 0.5f, innerSz, innerSz, 1f, 1f, -time * 42f);

            // Viền sáng ôm sát khung ảnh chân dung
            float bx = getX() + 2f, by = getY() + 2f, bw = getWidth() - 4f, bh = getHeight() - 4f;
            float borderAlpha = (0.55f + 0.30f * MathUtils.sin(time * 5.0f)) * parentAlpha;
            batch.setColor(1.0f, 0.90f, 0.50f, borderAlpha);
            batch.draw(whiteRegion, bx, by, bw, 3f);
            batch.draw(whiteRegion, bx, by + bh - 3f, bw, 3f);
            batch.draw(whiteRegion, bx, by, 3f, bh);
            batch.draw(whiteRegion, bx + bw - 3f, by, 3f, bh);

            batch.setBlendFunction(srcFunc, dstFunc);
            batch.setColor(or, og, ob, oa);
        }
    }

    private static long modalKeyConsumedFrame = -1L;

    /** Đánh dấu một hộp thoại modal vừa xử lý phím bấm ở frame này (ngăn màn hình nền nhận trùng Esc/Enter). */
    public static void consumeModalKey() {
        if (Gdx.graphics != null) modalKeyConsumedFrame = Gdx.graphics.getFrameId();
    }

    public static boolean wasModalKeyConsumedThisFrame() {
        return Gdx.graphics != null && modalKeyConsumedFrame == Gdx.graphics.getFrameId();
    }

    /** Kiểm tra một modal có đang nằm trên cùng của Stage hay không (tránh modal bên dưới cướp phím Esc/Enter). */
    public static boolean isTopModal(com.badlogic.gdx.scenes.scene2d.Stage stage, com.badlogic.gdx.scenes.scene2d.Actor root) {
        if (stage == null || root == null || root.getStage() != stage) return false;
        com.badlogic.gdx.utils.Array<com.badlogic.gdx.scenes.scene2d.Actor> actors = stage.getActors();
        return actors.size > 0 && actors.peek() == root;
    }

    public static void dispose() {
        if (fontSmall != null) fontSmall.dispose();
        if (fontNormal != null) fontNormal.dispose();
        if (fontLarge != null) fontLarge.dispose();
        if (fontTitle != null) fontTitle.dispose();
        if (fontSmallBold != null) fontSmallBold.dispose();
        if (fontNormalBold != null) fontNormalBold.dispose();
        fontSmallBold = fontNormalBold = null;
        if (whiteTex != null) whiteTex.dispose();
        if (haloGlowTex != null) haloGlowTex.dispose();
        if (haloRingTex != null) haloRingTex.dispose();
        for (Pixmap pm : PIXMAPS) pm.dispose();
        PIXMAPS.clear();
        if (fontDisplay != null) fontDisplay.dispose();
        if (fontBody != null) fontBody.dispose();
        if (fontBodyBold != null) fontBodyBold.dispose();
        if (fontButton != null) fontButton.dispose();
        if (fontInput != null) fontInput.dispose();
        fontSmall = fontNormal = fontLarge = fontTitle = null;
        fontDisplay = fontBody = fontBodyBold = fontButton = fontInput = null;
        whiteTex = haloGlowTex = haloRingTex = null;
        whiteRegion = haloGlowRegion = haloRingRegion = null;
    }
}
