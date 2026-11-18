package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.math.MathUtils;

import java.util.Random;

/**
 * Hồ sơ người chơi lưu cục bộ (Preferences): danh tính, vàng, tiến trình ải và cài đặt âm thanh.
 */
public class PlayerProfile {
    private static final String PREF_NAME = "AssassinsLegend_Profile";
    /** Tổng số ải cốt truyện (StoryChapterConfig: 3 màn x 3 ải). */
    public static final int MAX_LEVEL = StoryChapterConfig.TOTAL_STAGES;
    private static PlayerProfile instance;

    public String userId;
    public String playerName;
    public int gold;
    public int level;
    public int matches = 0;
    public int wins = 0;
    public String favoriteHero = "Vân Tiêu";

    // Tiến trình chế độ SOLO
    public int maxUnlockedLevel = 1;
    public int bestScore = 0;
    /** Ải cốt truyện chơi gần nhất & nhân vật đã chọn (để "Chơi tiếp"). */
    public int lastStoryLevel = 1;
    public String lastCharacter = "P1";

    // Cài đặt âm thanh & ngôn ngữ
    public boolean musicEnabled = true;
    public boolean soundEnabled = true;
    public float musicVolume = 0.50f;
    public float soundVolume = 0.85f;
    public String language = "vi";

    private PlayerProfile() {
        load();
    }

    public static PlayerProfile getInstance() {
        if (instance == null) {
            instance = new PlayerProfile();
        }
        return instance;
    }

    private Preferences prefs() {
        return Gdx.app.getPreferences(PREF_NAME);
    }

    public void load() {
        try {
            Preferences prefs = prefs();
            userId = prefs.getString("userId", "");
            if (userId.isEmpty()) {
                userId = "AL-" + (1000 + new Random().nextInt(9000));
                prefs.putString("userId", userId);
            }
            playerName = prefs.getString("playerName", "Chiến Thần");
            gold = prefs.getInteger("gold", 500);
            level = prefs.getInteger("level", 1);
            matches = prefs.getInteger("matches", 0);
            wins = prefs.getInteger("wins", 0);
            favoriteHero = prefs.getString("favoriteHero", "Vân Tiêu");
            maxUnlockedLevel = MathUtils.clamp(prefs.getInteger("maxUnlockedLevel", 1), 1, MAX_LEVEL);
            bestScore = prefs.getInteger("bestScore", 0);
            lastStoryLevel = MathUtils.clamp(prefs.getInteger("lastStoryLevel", 1), 1, maxUnlockedLevel);
            lastCharacter = "P2".equals(prefs.getString("lastCharacter", "P1")) ? "P2" : "P1";
            musicEnabled = prefs.getBoolean("musicEnabled", true);
            soundEnabled = prefs.getBoolean("soundEnabled", true);
            musicVolume = MathUtils.clamp(prefs.getFloat("musicVolume", 0.50f), 0f, 1f);
            soundVolume = MathUtils.clamp(prefs.getFloat("soundVolume", 0.85f), 0f, 1f);
            language = prefs.getString("language", "vi");
            prefs.flush();
        } catch (Exception e) {
            if (userId == null || userId.isEmpty()) {
                userId = "AL-" + (1000 + new Random().nextInt(9000));
            }
            if (playerName == null || playerName.isEmpty()) {
                playerName = "Chiến Thần";
            }
            gold = 500;
            level = 1;
        }
    }

    public void save() {
        try {
            Preferences prefs = prefs();
            prefs.putString("userId", userId);
            prefs.putString("playerName", playerName);
            prefs.putInteger("gold", gold);
            prefs.putInteger("level", level);
            prefs.putInteger("matches", matches);
            prefs.putInteger("wins", wins);
            prefs.putString("favoriteHero", favoriteHero);
            prefs.putInteger("maxUnlockedLevel", maxUnlockedLevel);
            prefs.putInteger("bestScore", bestScore);
            prefs.putInteger("lastStoryLevel", lastStoryLevel);
            prefs.putString("lastCharacter", lastCharacter);
            prefs.putBoolean("musicEnabled", musicEnabled);
            prefs.putBoolean("soundEnabled", soundEnabled);
            prefs.putFloat("musicVolume", musicVolume);
            prefs.putFloat("soundVolume", soundVolume);
            prefs.putString("language", language);
            prefs.flush();
        } catch (Exception e) {
            Gdx.app.error("PlayerProfile", "Khong luu duoc ho so", e);
        }
    }

    /** Cộng vàng mang về sau trận và lưu lại. */
    public void addGold(int amount) {
        if (amount <= 0) return;
        gold += amount;
        save();
    }

    /** Ghi nhận hoàn thành ải SOLO: mở khóa ải tiếp theo + cập nhật điểm cao nhất. */
    public void onLevelCleared(int clearedLevel, int score) {
        maxUnlockedLevel = MathUtils.clamp(Math.max(maxUnlockedLevel, clearedLevel + 1), 1, MAX_LEVEL);
        lastStoryLevel = MathUtils.clamp(clearedLevel + 1, 1, MAX_LEVEL);
        if (score > bestScore) bestScore = score;
        save();
    }

    /** Ghi nhận kết quả trận đấu online/custom. */
    public void recordMatchResult(boolean won) {
        matches++;
        if (won) wins++;
        save();
    }

    public String getRankTitle() {
        if (level <= 1) return "Tập Sự";
        if (level <= 4) return "Chiến Binh";
        if (level <= 9) return "Tinh Anh Hư Không";
        if (level <= 14) return "Sát Thủ Ám Ảnh";
        if (level <= 19) return "Bậc Thầy Song Đao";
        return "Huyền Thoại Bất Diệt";
    }

    public String getWinRateText() {
        if (matches <= 0) return "0%";
        int rate = (int) Math.round((double) wins * 100.0 / matches);
        return rate + "%";
    }

    /** Áp dụng cài đặt âm thanh đã lưu cho SoundManager. */
    public void applyAudioSettings() {
        SoundManager sm = SoundManager.getInstance();
        sm.setSoundVolume(soundVolume);
        sm.setSoundEnabled(soundEnabled);
        sm.setMusicVolume(musicVolume);
        sm.setMusicEnabled(musicEnabled);
    }

    public void updateFromServer(String serverUserId, String serverName, int serverGold, int serverLevel) {
        if (serverUserId != null && !serverUserId.isEmpty()) {
            this.userId = serverUserId;
        }
        if (serverName != null && !serverName.isEmpty()) {
            this.playerName = serverName;
        }
        // Server hiện chưa nhận vàng từ client -> giữ giá trị lớn hơn để không mất vàng kiếm được offline
        if (serverGold > this.gold) {
            this.gold = serverGold;
        }
        if (serverLevel > 0) {
            this.level = serverLevel;
        }
        save();
    }
}
