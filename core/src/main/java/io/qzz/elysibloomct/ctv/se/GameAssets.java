package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import java.util.IdentityHashMap;

/**
 * Bộ nhớ đệm texture dùng chung cho toàn game.
 *
 * - Sprite gốc 1254x1254 (≈6.3 MB VRAM/ảnh) được thu nhỏ khi nạp xuống tối đa {@link #SPRITE_MAX}px
 *   (≈1 MB VRAM/ảnh) và dùng filter Linear -> nhẹ hơn ~6 lần và hết răng cưa khi vẽ nhỏ.
 * - Texture được giữ lại giữa các lần chơi (chơi lại / qua ải không phải decode lại PNG).
 * - TextureData tự nạp lại từ file nên vẫn an toàn khi Android mất OpenGL context.
 */
public final class GameAssets {
    /** Kích thước cạnh lớn nhất của sprite nhân vật sau khi nạp (384px = ~2x kích thước hiển thị thật, nhẹ VRAM). */
    public static final int SPRITE_MAX = 384;
    /** Kích thước cạnh lớn nhất của ảnh nền. */
    public static final int BACKGROUND_MAX = 1920;

    /** Danh sách sprite nạp trước ở LoadingScreen để vào trận tức thì. */
    public static final String[] PRELOAD_SPRITES = {
        "trangdung1.png", "trangdung2.png",
        "trangchay1.png", "trangchay2.png", "trangchay3.png",
        "trangnhay.png", "trangroi.png",
        "trangchem1.png", "trangchem2.png", "trangchem3.png",
        "trangsungdung.png", "trangsungban1.png", "trangsungban2.png",
        "trangcungdung.png", "trangcungban1.png", "trangcungban2.png",
        "trangriudung.png", "trangriubo1.png", "trangriubo2.png",
        "songdaodung.png", "songdaochem1.png", "songdaochem2.png",
        "dendung1.png", "dendung2.png", "dendung3.png",
        "denchay1.png", "denchay2.png",
        "dennhay.png", "denroi.png",
        "dendanh1.png", "dendanh2.png", "dendanh3.png", "dendanh4.png",
        "cungdung.png", "cungduong.png", "bancung.png",
        "dencungdung.png", "dencungduong.png", "denbancung.png",
        "denriudung.png", "denriubo1.png", "denriubo2.png",
        "densongdaodung.png", "densongdaochem1.png", "densongdaochem2.png",
        "densungdung.png", "densungban1.png", "densungban2.png",
        "linhdung.png", "linhbuoc.png", "linhdon.png", "linhbidanh.png", "linhchet.png",
        "linhdanh1.png", "linhdanh2.png", "linhnga.png",
        "caubedung.png", "caubedi1.png", "caubedi2.png", "caubedanh.png", "caubebidanh.png",
        "caubenga.png", "caubechet.png",
        "thulinhbuoc1.png", "thulinhbuoc2.png", "thulinhdanh1.png", "thulinhdanh2.png",
        "thulinhgam.png", "thulinhulti.png", "thulinhnga.png", "thulinhchet.png",
        "trieudinhbay1.png", "trieudinhbay2.png", "trieudinhban1.png", "trieudinhban2.png",
        "trieudinhroi.png", "trieudinhchet.png",
        "khongtac1.png", "khongtac2.png", "khongtaclao.png", "khongtacroi.png", "khongtacchet.png"
    };

    /**
     * Bật/tắt thu nhỏ sprite khi nạp. Nếu thiết bị nào gặp lỗi, đặt false để nạp ảnh gốc như bản cũ
     * (tốn VRAM hơn nhưng chắc chắn hiển thị). Khi thu nhỏ lỗi, code cũng tự quay về ảnh gốc.
     */
    public static boolean DOWNSCALE = true;
    private static int loadedCount = 0, fallbackCount = 0, failedCount = 0;

    private static final ObjectMap<String, Texture> CACHE = new ObjectMap<>();

    /** Thông số chuẩn hóa khung hình nhân vật (tỉ lệ thân, vị trí bàn chân, tâm ngang). */
    public static final class SpriteMetric {
        public final float bodyFrac;
        public final float footFrac;
        public final float centerXFrac;

        public SpriteMetric(float bodyFrac, float footFrac, float centerXFrac) {
            this.bodyFrac = bodyFrac;
            this.footFrac = footFrac;
            this.centerXFrac = centerXFrac;
        }
    }

    public static final SpriteMetric DEFAULT_METRIC = new SpriteMetric(0.85f, 0.94f, 0.50f);
    private static final ObjectMap<String, SpriteMetric> PATH_METRICS = new ObjectMap<>();
    private static final IdentityHashMap<Texture, SpriteMetric> TEX_METRICS = new IdentityHashMap<>();

    static {
        // P1 - Bạch Sát Thủ
        m("trangdung1.png",    0.925f, 0.952f, 0.49f);
        m("trangdung2.png",    0.928f, 0.952f, 0.49f);
        m("trangchay1.png",    0.885f, 0.915f, 0.50f);
        m("trangchay2.png",    0.890f, 0.918f, 0.50f);
        m("trangchay3.png",    0.915f, 0.948f, 0.48f);
        m("trangnhay.png",     0.850f, 0.897f, 0.50f);
        m("trangroi.png",      0.820f, 0.862f, 0.50f);
        m("trangchem1.png",    0.815f, 0.952f, 0.47f);
        m("trangchem2.png",    0.775f, 0.905f, 0.46f);
        m("trangchem3.png",    0.805f, 0.956f, 0.45f);
        m("songdaodung.png",   0.888f, 0.952f, 0.49f);
        m("songdaochem1.png",  0.760f, 0.927f, 0.48f);
        m("songdaochem2.png",  0.760f, 0.940f, 0.45f);
        m("trangcungdung.png", 0.805f, 0.935f, 0.48f);
        m("trangcungban1.png", 0.790f, 0.921f, 0.47f);
        m("trangcungban2.png", 0.805f, 0.935f, 0.47f);
        m("trangsungdung.png", 0.882f, 0.946f, 0.48f);
        m("trangsungban1.png", 0.810f, 0.936f, 0.45f);
        m("trangsungban2.png", 0.801f, 0.938f, 0.46f);
        m("trangriudung.png",  0.745f, 0.927f, 0.49f);
        m("trangriubo1.png",   0.715f, 0.900f, 0.52f);
        m("trangriubo2.png",   0.735f, 0.926f, 0.47f);

        // P2 - Dạ Sát Thủ (Sát Thủ Đen)
        m("dendung1.png",         0.797f, 0.936f, 0.50f);
        m("dendung2.png",         0.802f, 0.936f, 0.50f);
        m("dendung3.png",         0.793f, 0.936f, 0.50f);
        m("denchay1.png",         0.869f, 0.945f, 0.505f);
        m("denchay2.png",         0.863f, 0.943f, 0.504f);
        m("dennhay.png",          0.785f, 0.964f, 0.515f);
        m("denroi.png",           0.660f, 0.785f, 0.453f);
        m("dendanh1.png",         0.735f, 0.888f, 0.52f);
        m("dendanh2.png",         0.720f, 0.862f, 0.46f);
        m("dendanh3.png",         0.730f, 0.913f, 0.50f);
        m("dendanh4.png",         0.710f, 0.860f, 0.45f);
        m("cungdung.png",         0.834f, 0.936f, 0.470f);
        m("cungduong.png",        0.827f, 0.936f, 0.461f);
        m("bancung.png",          0.839f, 0.936f, 0.459f);
        m("dencungdung.png",      0.834f, 0.936f, 0.470f);
        m("dencungduong.png",     0.827f, 0.936f, 0.461f);
        m("denbancung.png",       0.839f, 0.936f, 0.459f);
        m("denriudung.png",       0.860f, 0.950f, 0.460f);
        m("denriubo1.png",        0.840f, 0.939f, 0.490f);
        m("denriubo2.png",        0.825f, 0.911f, 0.450f);
        m("densongdaodung.png",   0.880f, 0.938f, 0.500f);
        m("densongdaochem1.png",  0.863f, 0.930f, 0.485f);
        m("densongdaochem2.png",  0.871f, 0.930f, 0.485f);
        m("densungdung.png",      0.880f, 0.965f, 0.480f);
        m("densungban1.png",      0.880f, 0.965f, 0.450f);
        m("densungban2.png",      0.880f, 0.965f, 0.440f);

        // Tiểu Vũ (Cậu bé)
        m("caubedung.png",     0.787f, 0.902f, 0.52f);
        m("caubedi1.png",      0.866f, 0.932f, 0.52f);
        m("caubedi2.png",      0.878f, 0.948f, 0.52f);
        m("caubedanh.png",     0.919f, 0.954f, 0.48f);
        m("caubebidanh.png",   0.849f, 0.948f, 0.50f);
        m("caubenga.png",      0.820f, 0.839f, 0.52f);
        m("caubechet.png",     0.820f, 0.606f, 0.50f);

        // Lính canh
        m("linhdung.png",      0.785f, 0.927f, 0.48f);
        m("linhbuoc.png",      0.780f, 0.917f, 0.48f);
        m("linhdon.png",       0.655f, 0.913f, 0.47f);
        m("linhdanh1.png",     0.745f, 0.893f, 0.44f);
        m("linhdanh2.png",     0.700f, 0.868f, 0.40f);
        m("linhbidanh.png",    0.665f, 0.913f, 0.48f);
        m("linhnga.png",       0.760f, 0.853f, 0.48f);
        m("linhchet.png",      0.760f, 0.688f, 0.50f);

        // Thủ Lĩnh (Boss)
        m("thulinhbuoc1.png",  0.832f, 0.936f, 0.56f);
        m("thulinhbuoc2.png",  0.865f, 0.960f, 0.56f);
        m("thulinhdanh1.png",  0.830f, 0.958f, 0.54f);
        m("thulinhdanh2.png",  0.855f, 0.962f, 0.52f);
        m("thulinhgam.png",    0.780f, 0.971f, 0.52f);
        m("thulinhulti.png",   0.780f, 0.956f, 0.50f);
        m("thulinhnga.png",    0.840f, 0.940f, 0.54f);
        m("thulinhchet.png",   0.840f, 0.722f, 0.50f);

        // Quái trên không
        m("trieudinhbay1.png", 0.635f, 0.831f, 0.50f);
        m("trieudinhbay2.png", 0.635f, 0.833f, 0.50f);
        m("trieudinhban1.png", 0.644f, 0.844f, 0.48f);
        m("trieudinhban2.png", 0.660f, 0.839f, 0.48f);
        m("trieudinhroi.png",  0.850f, 0.960f, 0.50f);
        m("trieudinhchet.png", 0.750f, 0.859f, 0.50f);
        m("khongtac1.png",     0.880f, 0.960f, 0.50f);
        m("khongtac2.png",     0.880f, 0.960f, 0.50f);
        m("khongtaclao.png",   0.820f, 0.882f, 0.50f);
        m("khongtacroi.png",   0.780f, 0.765f, 0.50f);
        m("khongtacchet.png",  0.780f, 0.819f, 0.50f);
    }

    private static void m(String file, float bodyFrac, float footFrac, float centerXFrac) {
        PATH_METRICS.put(file, new SpriteMetric(bodyFrac, footFrac, centerXFrac));
    }

    public static SpriteMetric metricOf(Texture tex) {
        if (tex == null) return DEFAULT_METRIC;
        SpriteMetric sm = TEX_METRICS.get(tex);
        return sm != null ? sm : DEFAULT_METRIC;
    }

    private GameAssets() {}

    /** Sprite nhân vật/quái (thu nhỏ xuống {@link #SPRITE_MAX}). Trả về null nếu file không tồn tại. */
    public static Texture sprite(String path) {
        return load(path, SPRITE_MAX);
    }

    /** Ảnh nền / ảnh UI lớn. Trả về null nếu file không tồn tại. */
    public static Texture background(String path) {
        return load(path, BACKGROUND_MAX);
    }

    public static boolean exists(String path) {
        try {
            return Gdx.files.internal(path).exists();
        } catch (Exception e) {
            return false;
        }
    }

    private static Texture load(String path, int maxSize) {
        String key = path + "@" + maxSize;
        Texture cached = CACHE.get(key);
        if (cached != null) return cached;
        if (!exists(path)) {
            Gdx.app.error("GameAssets", "Khong tim thay file: " + path);
            return null;
        }
        Texture tex = null;
        if (DOWNSCALE) {
            ScaledFileTextureData data = null;
            try {
                data = new ScaledFileTextureData(Gdx.files.internal(path), maxSize);
                tex = new Texture(data);
                loadedCount++;
            } catch (Throwable e) {
                if (data != null) data.disposePendingPixmap();
                Gdx.app.error("GameAssets", "Thu nho that bai, dung anh goc: " + path, e);
                tex = null;
            }
        }
        if (tex == null) {
            // Cách nạp gốc của bản cũ: luôn hoạt động nếu file hợp lệ
            try {
                tex = new Texture(Gdx.files.internal(path));
                fallbackCount++;
            } catch (Throwable e) {
                failedCount++;
                Gdx.app.error("GameAssets", "KHONG NAP DUOC anh: " + path, e);
                return null;
            }
        }
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        tex.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        CACHE.put(key, tex);
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        String baseName = slash >= 0 ? path.substring(slash + 1) : path;
        SpriteMetric sm = PATH_METRICS.get(baseName);
        if (sm != null) TEX_METRICS.put(tex, sm);
        return tex;
    }

    /** Ghi log tổng kết (xem trong Logcat, lọc tag "GameAssets"). */
    public static void logSummary() {
        Gdx.app.log("GameAssets", "Sprite da nap: thu nho=" + loadedCount + ", anh goc=" + fallbackCount
            + ", loi=" + failedCount + ", DOWNSCALE=" + DOWNSCALE);
    }

    /** Giải phóng toàn bộ (gọi khi thoát game). */
    public static void disposeAll() {
        for (Texture t : CACHE.values()) {
            try { t.dispose(); } catch (Exception ignored) {}
        }
        CACHE.clear();
        TEX_METRICS.clear();
    }

    /**
     * TextureData đọc PNG/JPG rồi thu nhỏ bằng bộ lọc BiLinear.
     * Managed = true: khi mất GL context, libGDX gọi lại prepare() để tự nạp lại.
     */
    private static final class ScaledFileTextureData implements TextureData {
        private final FileHandle file;
        private final int maxSize;
        private Pixmap pixmap;
        private int width, height;
        private boolean prepared;

        ScaledFileTextureData(FileHandle file, int maxSize) {
            this.file = file;
            this.maxSize = maxSize;
            prepare(); // biết trước kích thước
        }

        @Override public TextureDataType getType() { return TextureDataType.Pixmap; }
        @Override public boolean isPrepared() { return prepared; }

        @Override
        public void prepare() {
            if (prepared) return;
            Pixmap src = new Pixmap(file);
            try {
                int sw = src.getWidth(), sh = src.getHeight();
                float scale = Math.min(1f, (float) maxSize / Math.max(sw, sh));
                if (scale >= 0.999f && src.getFormat() == Pixmap.Format.RGBA8888) {
                    pixmap = src;
                } else {
                    int nw = Math.max(1, Math.round(sw * scale));
                    int nh = Math.max(1, Math.round(sh * scale));
                    Pixmap dst = new Pixmap(nw, nh, Pixmap.Format.RGBA8888);
                    try {
                        dst.setBlending(Pixmap.Blending.None);
                        dst.setFilter(Pixmap.Filter.BiLinear);
                        dst.drawPixmap(src, 0, 0, sw, sh, 0, 0, nw, nh);
                        pixmap = dst;
                    } catch (Throwable t) {
                        dst.dispose();
                        throw t;
                    }
                }
                width = pixmap.getWidth();
                height = pixmap.getHeight();
                prepared = true;
            } finally {
                if (pixmap != src) src.dispose();
            }
        }

        void disposePendingPixmap() {
            if (pixmap != null) {
                try { pixmap.dispose(); } catch (Exception ignored) {}
                pixmap = null;
            }
            prepared = false;
        }

        @Override
        public Pixmap consumePixmap() {
            if (!prepared) throw new GdxRuntimeException("TextureData chua prepare");
            prepared = false;
            Pixmap p = pixmap;
            pixmap = null;
            return p;
        }

        @Override public boolean disposePixmap() { return true; }
        @Override public void consumeCustomData(int target) { throw new GdxRuntimeException("Khong ho tro"); }
        @Override public int getWidth() { return width; }
        @Override public int getHeight() { return height; }
        @Override public Pixmap.Format getFormat() { return Pixmap.Format.RGBA8888; }
        @Override public boolean useMipMaps() { return false; }
        @Override public boolean isManaged() { return true; }
    }
}
