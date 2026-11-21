package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;

public class Main extends Game {
    public String selectedCharacter = "P1";
    public String selectedMode = "SOLO";
    public String playerName = "Chiến Thần";
    public int selectedTeam = 1;
    /** Ải được chọn ở màn chọn nhân vật (chế độ SOLO). */
    public int selectedLevel = 1;
    /** Bản đồ trực tuyến được chọn (1: Thành Cổ, 2: Bến Cảng, 3: Pháo Đài). */
    public int selectedOnlineMap = 1;
    /** true = "Chọn màn chơi (tùy chọn)": chơi tự do, KHÔNG ghi đè file lưu của chiến dịch cốt truyện. */
    public boolean freePlayMode = false;
    // Cầu nối xin quyền Micro trên Android cho Voice Chat
    public java.util.function.BooleanSupplier micPermissionChecker = null;
    public Runnable micPermissionRequester = null;

    @Override
    public void create() {
        // Cho phép duyệt lồng Array của libGDX bằng for-each (an toàn, code chính dùng vòng for chỉ số)
        com.badlogic.gdx.utils.Collections.allocateIterators = true;
        // Android: tự xử lý nút Back (mở menu tạm dừng / quay lại), không để hệ thống đóng ứng dụng ngay
        Gdx.input.setCatchKey(com.badlogic.gdx.Input.Keys.BACK, true);
        try {
            PlayerProfile profile = PlayerProfile.getInstance();
            playerName = profile.playerName;
            profile.applyAudioSettings();
            LanguageManager.init();
            NetworkManager.getInstance().registerUserWithServer();
        } catch (Exception e) {
            Gdx.app.error("Main", "Khoi tao ho so/mang that bai", e);
        }

        try {
            setScreen(new LoadingScreen(this));
        } catch (Throwable t) {
            Gdx.app.error("Main", "Error loading LoadingScreen", t);
            setScreen(new MainMenuScreen(this));
        }
    }

    /**
     * Chuyển màn hình an toàn: màn hình cũ được dispose ở đầu frame KẾ TIẾP.
     * Trước đây các màn hình tự gọi dispose() ngay trong ClickListener/socket callback,
     * tức là hủy Stage khi nó còn đang xử lý sự kiện -> dễ crash và rò rỉ.
     */
    @Override
    public void setScreen(Screen next) {
        final Screen old = getScreen();
        // Đang xoay (màn hình dọc): màn mới phải thấy kích thước ngang ngay khi show()/resize()
        PortraitRotation.begin();
        try {
            super.setScreen(next);
        } finally {
            PortraitRotation.end();
        }
        if (old != null && old != next) {
            Gdx.app.postRunnable(() -> {
                try {
                    old.dispose();
                } catch (Exception e) {
                    Gdx.app.error("Main", "Loi khi giai phong man hinh cu", e);
                }
            });
        }
    }

    /** Màn hình dọc (web trên điện thoại khóa xoay): báo cho màn hình kích thước ngang, xem PortraitRotation. */
    @Override
    public void resize(int width, int height) {
        if (PortraitRotation.update(width, height) && PortraitRotation.isActive()) {
            PortraitRotation.guardInputProcessor();
            PortraitRotation.begin();
            try {
                super.resize(PortraitRotation.landscapeWidth(), PortraitRotation.landscapeHeight());
            } finally {
                PortraitRotation.end();
            }
        } else {
            PortraitRotation.releaseInputProcessor();
            super.resize(width, height);
        }
    }

    @Override
    public void render() {
        if (!PortraitRotation.isActive()) {
            super.render();
            return;
        }
        PortraitRotation.guardInputProcessor();
        PortraitRotation.beginFrame();
        PortraitRotation.begin();
        try {
            super.render();
        } finally {
            PortraitRotation.end();
            PortraitRotation.endFrame();
        }
    }

    @Override
    public void dispose() {
        Screen current = getScreen();
        if (current != null) {
            try { current.hide(); current.dispose(); } catch (Exception ignored) {}
        }
        SoundManager.getInstance().dispose();
        GameAssets.disposeAll();
        UiKit.dispose();
        PortraitRotation.dispose();
    }
}
